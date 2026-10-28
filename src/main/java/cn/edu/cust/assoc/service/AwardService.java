package cn.edu.cust.assoc.service;

import cn.edu.cust.assoc.domain.*;
import cn.edu.cust.assoc.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class AwardService {

    private final AwardRepository awardRepository;
    private final TeamAwardRepository teamAwardRepository;
    private final AwardShareRepository shareRepository;
    private final CompetitionRepository competitionRepository;

    public AwardService(AwardRepository awardRepository,
                        TeamAwardRepository teamAwardRepository,
                        AwardShareRepository shareRepository,
                        CompetitionRepository competitionRepository) {
        this.awardRepository = awardRepository;
        this.teamAwardRepository = teamAwardRepository;
        this.shareRepository = shareRepository;
        this.competitionRepository = competitionRepository;
    }

    @Transactional(readOnly = true)
    public List<AwardEntity> listIndividual(Long competitionId) {
        return awardRepository.findByCompetitionIdOrderByIdDesc(competitionId);
    }

    @Transactional(readOnly = true)
    public List<TeamAwardEntity> listTeam(Long competitionId) {
        return teamAwardRepository.findByCompetitionIdOrderByIdDesc(competitionId);
    }

    @Transactional(readOnly = true)
    public List<AwardShareEntity> sharesOf(Long teamAwardId) {
        return shareRepository.findByTeamAwardId(teamAwardId);
    }

    /** 个人奖：分数由「级别 + 名次」算出来，入库即固定，后面调规则不影响历史。 */
    @Transactional
    public AwardEntity createIndividual(Long competitionId, Long userId, String rank, String proofUrl) {
        CompetitionEntity competition = requireCompetition(competitionId);
        if (!CompetitionPoints.Rank.isValid(rank)) {
            throw new IllegalArgumentException("未知的名次：" + rank);
        }
        int points = CompetitionPoints.of(competition.getLevel(), rank);

        AwardEntity award = new AwardEntity();
        award.setCompetitionId(competitionId);
        award.setUserId(userId);
        award.setRankLevel(rank);
        award.setBasePoints(points);
        award.setProofUrl(proofUrl);
        return awardRepository.save(award);
    }

    /** 团队奖：先校验合计等于总分，再一起写库（同事务，要么全成要么全不成）。 */
    @Transactional
    public TeamAwardEntity createTeam(Long competitionId, String rank, String proofUrl,
                                      Long createdBy, Map<Long, Integer> shares) {
        CompetitionEntity competition = requireCompetition(competitionId);
        if (!CompetitionPoints.Rank.isValid(rank)) {
            throw new IllegalArgumentException("未知的名次：" + rank);
        }
        int base = CompetitionPoints.of(competition.getLevel(), rank);
        TeamShareRules.validate(base, shares);

        TeamAwardEntity teamAward = new TeamAwardEntity();
        teamAward.setCompetitionId(competitionId);
        teamAward.setRankLevel(rank);
        teamAward.setBasePoints(base);
        teamAward.setProofUrl(proofUrl);
        teamAward.setCreatedBy(createdBy);
        TeamAwardEntity saved = teamAwardRepository.save(teamAward);

        shares.forEach((userId, points) -> {
            AwardShareEntity share = new AwardShareEntity();
            share.setTeamAwardId(saved.getId());
            share.setUserId(userId);
            share.setPoints(points);
            shareRepository.save(share);
        });
        return saved;
    }

    private CompetitionEntity requireCompetition(Long competitionId) {
        return competitionRepository.findById(competitionId)
                .orElseThrow(() -> new IllegalArgumentException("比赛不存在：" + competitionId));
    }
}
