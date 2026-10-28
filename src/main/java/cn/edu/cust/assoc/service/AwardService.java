package cn.edu.cust.assoc.service;

import cn.edu.cust.assoc.common.BizException;
import cn.edu.cust.assoc.domain.*;
import cn.edu.cust.assoc.repository.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class AwardService {

    private final AwardRepository awardRepository;
    private final TeamAwardRepository teamAwardRepository;
    private final AwardShareRepository shareRepository;
    private final CompetitionRepository competitionRepository;
    private final PointsRuleService ruleService;
    private final PointsService pointsService;
    private final AuditService audit;

    public AwardService(AwardRepository awardRepository,
                        TeamAwardRepository teamAwardRepository,
                        AwardShareRepository shareRepository,
                        CompetitionRepository competitionRepository,
                        PointsRuleService ruleService,
                        PointsService pointsService,
                        AuditService audit) {
        this.awardRepository = awardRepository;
        this.teamAwardRepository = teamAwardRepository;
        this.shareRepository = shareRepository;
        this.competitionRepository = competitionRepository;
        this.ruleService = ruleService;
        this.pointsService = pointsService;
        this.audit = audit;
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

    /**
     * 个人奖：分数由「级别 + 名次」按比赛结束日当天生效的规则算，
     * 入库即固定；同时写一条已成立的积分申请和流水，榜单和奖项从此出自同一套数据。
     */
    @Transactional
    public AwardEntity createIndividual(Long competitionId, Long userId, String rank,
                                        String proofUrl, Long operatorId) {
        CompetitionEntity competition = requireCompetition(competitionId);
        if (!CompetitionPoints.Rank.isValid(rank)) {
            throw new BizException("未知的名次：" + rank);
        }
        if (awardRepository.existsByCompetitionIdAndUserIdAndRankLevel(competitionId, userId, rank)) {
            throw new BizException("这名成员在这个比赛、这个名次已经记过奖了");
        }
        int points = ruleService.competitionPoints(competition.getLevel(), rank, competition.getEndDate());

        AwardEntity award = new AwardEntity();
        award.setCompetitionId(competitionId);
        award.setUserId(userId);
        award.setRankLevel(rank);
        award.setBasePoints(points);
        award.setProofUrl(proofUrl);
        AwardEntity saved;
        try {
            saved = awardRepository.saveAndFlush(award);
        } catch (DataIntegrityViolationException e) {
            throw new BizException("这名成员在这个比赛、这个名次已经记过奖了");
        }

        pointsService.recordApproved(userId, "COMPETITION", competitionId, points,
                competition.getEndDate(), proofUrl,
                "比赛奖：" + competition.getName() + "（" + rank + "）",
                "AWARD:" + saved.getId(), operatorId);
        audit.record("AWARD_CREATE", "AWARD", saved.getId(), null,
                "比赛 " + competitionId + "，用户 " + userId + "，" + rank + "，积分 " + points);
        return saved;
    }

    /**
     * 团队奖：先校验合计等于总分、成员不重复，再一起写库（同事务）。
     * 每个成员的份额各生成一条积分申请和流水。
     */
    @Transactional
    public TeamAwardEntity createTeam(Long competitionId, String rank, String proofUrl,
                                      Long createdBy, Map<Long, Integer> shares) {
        CompetitionEntity competition = requireCompetition(competitionId);
        if (!CompetitionPoints.Rank.isValid(rank)) {
            throw new BizException("未知的名次：" + rank);
        }
        if (teamAwardRepository.existsByCompetitionIdAndRankLevel(competitionId, rank)) {
            throw new BizException("这个比赛的这个名次已经有团队奖了，不能重复登记");
        }
        TeamShareRules.validateNoDuplicate(new ArrayList<>(shares.keySet()));
        int base = ruleService.competitionPoints(competition.getLevel(), rank, competition.getEndDate());
        TeamShareRules.validate(base, shares);

        TeamAwardEntity teamAward = new TeamAwardEntity();
        teamAward.setCompetitionId(competitionId);
        teamAward.setRankLevel(rank);
        teamAward.setBasePoints(base);
        teamAward.setProofUrl(proofUrl);
        teamAward.setCreatedBy(createdBy);
        TeamAwardEntity saved;
        try {
            saved = teamAwardRepository.saveAndFlush(teamAward);
        } catch (DataIntegrityViolationException e) {
            throw new BizException("这个比赛的这个名次已经有团队奖了，不能重复登记");
        }

        shares.forEach((userId, points) -> {
            AwardShareEntity share = new AwardShareEntity();
            share.setTeamAwardId(saved.getId());
            share.setUserId(userId);
            share.setPoints(points);
            share = shareRepository.saveAndFlush(share);
            pointsService.recordApproved(userId, "COMPETITION", competitionId, points,
                    competition.getEndDate(), proofUrl,
                    "团队奖：" + competition.getName() + "（" + rank + "）",
                    "TEAMSHARE:" + share.getId(), createdBy);
        });
        audit.record("TEAM_AWARD_CREATE", "TEAM_AWARD", saved.getId(), null,
                "比赛 " + competitionId + "，" + rank + "，总分 " + base + "，成员 " + shares.size() + " 人");
        return saved;
    }

    private CompetitionEntity requireCompetition(Long competitionId) {
        return competitionRepository.findById(competitionId)
                .orElseThrow(() -> new BizException("比赛不存在：" + competitionId));
    }
}
