package cn.edu.cust.assoc.service;

import cn.edu.cust.assoc.domain.AwardEntity;
import cn.edu.cust.assoc.domain.AwardShareEntity;
import cn.edu.cust.assoc.domain.CompetitionEntity;
import cn.edu.cust.assoc.domain.Competitions;
import cn.edu.cust.assoc.domain.PointsLedgerEntity;
import cn.edu.cust.assoc.domain.PointsRequestEntity;
import cn.edu.cust.assoc.domain.TeamAwardEntity;
import cn.edu.cust.assoc.domain.TeamEntity;
import cn.edu.cust.assoc.domain.UserEntity;
import cn.edu.cust.assoc.dto.AbilityRadar;
import cn.edu.cust.assoc.dto.LeaderboardRow;
import cn.edu.cust.assoc.dto.MemberLedgerRow;
import cn.edu.cust.assoc.dto.MemberPointsView;
import cn.edu.cust.assoc.dto.RadarAxis;
import cn.edu.cust.assoc.repository.AwardRepository;
import cn.edu.cust.assoc.repository.AwardShareRepository;
import cn.edu.cust.assoc.repository.CompetitionRepository;
import cn.edu.cust.assoc.repository.PointsRequestRepository;
import cn.edu.cust.assoc.repository.TeamAwardRepository;
import cn.edu.cust.assoc.repository.TeamMemberRepository;
import cn.edu.cust.assoc.repository.TeamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** 榜单与成员积分页的数据组装。对外只给昵称和隐私开关允许的字段。 */
@Service
public class MemberService {

    private final PointsService pointsService;
    private final PointsRequestRepository requestRepository;
    private final UserDirectoryService userDirectory;
    private final MemberProfileService profileService;
    private final AwardRepository awardRepository;
    private final AwardShareRepository awardShareRepository;
    private final TeamAwardRepository teamAwardRepository;
    private final CompetitionRepository competitionRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamRepository teamRepository;

    public MemberService(PointsService pointsService, PointsRequestRepository requestRepository,
                         UserDirectoryService userDirectory, MemberProfileService profileService,
                         AwardRepository awardRepository, AwardShareRepository awardShareRepository,
                         TeamAwardRepository teamAwardRepository,
                         CompetitionRepository competitionRepository,
                         TeamMemberRepository teamMemberRepository,
                         TeamRepository teamRepository) {
        this.pointsService = pointsService;
        this.requestRepository = requestRepository;
        this.userDirectory = userDirectory;
        this.profileService = profileService;
        this.awardRepository = awardRepository;
        this.awardShareRepository = awardShareRepository;
        this.teamAwardRepository = teamAwardRepository;
        this.competitionRepository = competitionRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.teamRepository = teamRepository;
    }

    @Transactional(readOnly = true)
    public List<LeaderboardRow> ranking(String type, int limit) {
        List<Object[]> raw = pointsService.ranking(type, limit);
        List<Long> ids = raw.stream().map(row -> (Long) row[0]).toList();
        Map<Long, String> names = userDirectory.nicknames(ids);
        List<LeaderboardRow> rows = new ArrayList<>();
        int rank = 1;
        for (Object[] row : raw) {
            Long userId = (Long) row[0];
            long total = ((Number) row[1]).longValue();
            rows.add(new LeaderboardRow(rank++, userId, names.getOrDefault(userId, "成员"), total));
        }
        return rows;
    }

    @Transactional(readOnly = true)
    public List<LeaderboardRow> top(int n) {
        return ranking("ALL", n);
    }

    @Transactional(readOnly = true)
    public MemberPointsView view(Long userId, Long viewerId, boolean privilegedViewer) {
        UserEntity user = userDirectory.require(userId);
        List<PointsLedgerEntity> ledger = pointsService.ledgerOf(userId);

        Map<Long, String> sources = requestRepository
                .findAllById(ledger.stream().map(PointsLedgerEntity::getRequestId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(PointsRequestEntity::getId, PointsRequestEntity::getSourceType));

        boolean detailsVisible = viewerId != null
                && (viewerId.equals(userId) || privilegedViewer || isLeaderOf(userId, viewerId));
        List<MemberLedgerRow> rows = new ArrayList<>();
        Map<String, Integer> bySource = new LinkedHashMap<>();
        int total = 0;
        for (PointsLedgerEntity entry : ledger) {
            String source = sources.getOrDefault(entry.getRequestId(), "OTHER");
            rows.add(new MemberLedgerRow(entry.getPeriodMonth(), entry.getOccurredOn(),
                    sourceLabel(source), entry.getEntryType(), entry.getPoints()));
            bySource.merge(sourceLabel(source), entry.getPoints(), Integer::sum);
            total += entry.getPoints();
        }
        return new MemberPointsView(userId, user.getNickname(), total, bySource,
                detailsVisible ? rows : List.of(),
                viewerId == null ? null : profileService.publicView(userId, true).orElse(null),
                radarOf(userId), detailsVisible);
    }

    private boolean isLeaderOf(Long memberId, Long viewerId) {
        return teamMemberRepository.findByUserIdAndLeftAtIsNull(memberId)
                .flatMap(member -> teamRepository.findById(member.getTeamId()))
                .filter(TeamEntity::isActive)
                .map(team -> team.getLeaderId().equals(viewerId))
                .orElse(false);
    }

    /** 六维能力雷达：个人获奖 + 团队奖份额，都按竞赛类别聚合。 */
    private AbilityRadar radarOf(Long userId) {
        List<AwardEntity> awards = awardRepository.findByUserIdOrderByIdDesc(userId);
        List<AwardShareEntity> shares = awardShareRepository.findByUserId(userId);
        Map<Long, TeamAwardEntity> teamAwards = teamAwardRepository
                .findAllById(shares.stream().map(AwardShareEntity::getTeamAwardId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(TeamAwardEntity::getId, award -> award));

        Set<Long> competitionIds = new HashSet<>();
        awards.forEach(award -> competitionIds.add(award.getCompetitionId()));
        teamAwards.values().forEach(award -> competitionIds.add(award.getCompetitionId()));
        Map<Long, CompetitionEntity> competitions = competitionRepository
                .findAllById(competitionIds).stream()
                .collect(Collectors.toMap(CompetitionEntity::getId, competition -> competition));

        Map<String, Integer> personal = new HashMap<>();
        Map<String, Integer> team = new HashMap<>();
        for (AwardEntity award : awards) {
            CompetitionEntity competition = competitions.get(award.getCompetitionId());
            if (competition != null) {
                personal.merge(competition.getCategory(), 1, Integer::sum);
            }
        }
        for (AwardShareEntity share : shares) {
            TeamAwardEntity teamAward = teamAwards.get(share.getTeamAwardId());
            CompetitionEntity competition = teamAward == null ? null
                    : competitions.get(teamAward.getCompetitionId());
            if (competition != null) {
                team.merge(competition.getCategory(), 1, Integer::sum);
            }
        }
        List<RadarAxis> axes = Arrays.stream(Competitions.Category.values())
                .map(category -> new RadarAxis(category.getLabel(),
                        personal.getOrDefault(category.name(), 0),
                        team.getOrDefault(category.name(), 0)))
                .toList();
        return AbilityRadar.of(axes);
    }

    private String sourceLabel(String sourceType) {
        return switch (sourceType) {
            case "COMPETITION" -> "比赛奖";
            case "BOUNTY" -> "挖洞";
            case "HW" -> "护网";
            default -> "其它";
        };
    }
}
