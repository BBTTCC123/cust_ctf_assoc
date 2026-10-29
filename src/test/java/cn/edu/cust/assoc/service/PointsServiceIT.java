package cn.edu.cust.assoc.service;

import cn.edu.cust.assoc.common.BizException;
import cn.edu.cust.assoc.domain.Periods;
import cn.edu.cust.assoc.domain.PointsAppealEntity;
import cn.edu.cust.assoc.domain.PointsLedgerEntity;
import cn.edu.cust.assoc.domain.PointsRequestEntity;
import cn.edu.cust.assoc.domain.UserEntity;
import cn.edu.cust.assoc.repository.PointsAppealRepository;
import cn.edu.cust.assoc.repository.PointsLedgerRepository;
import cn.edu.cust.assoc.repository.PointsRequestRepository;
import cn.edu.cust.assoc.support.IntegrationTestBase;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 积分审核、单漏洞点封顶、冲抵与申诉的数据库级回归（ISS-003 / ISS-006 / ISS-007 / ISS-021）。
 *
 * 积分算错不会报错，只会让榜单慢慢失真；这里直接查 points_ledger 的分数、
 * 幂等键与 reverses_id，确保「只增不改、冲抵反向、封顶按漏洞点」三条红线都在。
 */
@Transactional
@EnabledIf("cn.edu.cust.assoc.support.TestDatabase#isAvailable")
class PointsServiceIT extends IntegrationTestBase {

    private static final String PROOF = "https://example.com/proof";

    @Autowired
    private PointsService pointsService;

    @Autowired
    private PointsRequestRepository requestRepository;

    @Autowired
    private PointsLedgerRepository ledgerRepository;

    @Autowired
    private PointsAppealRepository appealRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    private UserEntity member;
    private UserEntity otherMember;
    private UserEntity reviewer;

    @BeforeEach
    void setUpUsers() {
        member = createActiveUser("points", "ItPass!2026abc", "USER");
        otherMember = createActiveUser("other", "ItPass!2026abc", "USER");
        reviewer = createActiveUser("reviewer", "ItPass!2026abc", "POINTS_ADMIN");
    }

    @Test
    void bountyCapIsEnforcedPerVulnerability() {
        String externalId = uniqueName("vuln");

        PointsRequestEntity first = submitHighBounty(externalId);
        PointsLedgerEntity firstLedger = pointsService.approve(first.getId(), reviewer.getId());
        assertThat(firstLedger.getEntryType()).isEqualTo("EARN");
        assertThat(firstLedger.getPoints()).isEqualTo(6);
        assertThat(firstLedger.getNote()).isNull();
        assertThat(firstLedger.getIdempotencyKey()).isEqualTo("EARN:" + first.getId());
        assertThat(firstLedger.getPeriodMonth()).isEqualTo(Periods.periodMonth(first.getOccurredOn(), LocalDate.now()));

        // 同一个漏洞点再记一笔 6 分：只允许补到 10 分，多余 2 分不记，并在备注里说明
        PointsRequestEntity second = submitHighBounty(externalId);
        PointsLedgerEntity secondLedger = pointsService.approve(second.getId(), reviewer.getId());
        assertThat(secondLedger.getVulnerabilityId()).isEqualTo(firstLedger.getVulnerabilityId());
        assertThat(secondLedger.getPoints()).isEqualTo(4);
        assertThat(secondLedger.getNote()).contains("封顶 10 分").contains("已入账 6");

        assertThat(ledgerRepository.sumByVulnerability(firstLedger.getVulnerabilityId())).isEqualTo(10);

        // 已处理过的申请单不能再次审核（防重复点击的第二道闸）
        assertThatThrownBy(() -> pointsService.approve(first.getId(), reviewer.getId()))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("已经处理过");
    }

    @Test
    void recordApprovedIsIdempotentAndDatabaseRejectsDuplicateKeys() {
        String key = "IT:EARN:" + uniqueName("idem");
        LocalDate occurredOn = LocalDate.now().minusDays(2);

        PointsLedgerEntity first = pointsService.recordApproved(member.getId(), "COMPETITION", null, 12,
                occurredOn, PROOF, "比赛奖录入", key, reviewer.getId());
        long requestsAfterFirst = requestRepository.count();

        PointsLedgerEntity again = pointsService.recordApproved(member.getId(), "COMPETITION", null, 12,
                occurredOn, PROOF, "重复调用", key, reviewer.getId());

        assertThat(again.getId()).isEqualTo(first.getId());
        assertThat(requestRepository.count()).as("重复幂等键不应再生成申请单").isEqualTo(requestsAfterFirst);
        assertThat(ledgerRepository.findByIdempotencyKey(key)).get().extracting(PointsLedgerEntity::getPoints)
                .isEqualTo(12);

        // 唯一约束是最后一道兜底：绕过 Service 直接插重复键也会被数据库拒绝
        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO points_ledger "
                        + "(user_id, request_id, entry_type, points, period_month, occurred_on, idempotency_key) "
                        + "VALUES (?, ?, 'EARN', ?, ?, ?, ?)",
                member.getId(), first.getRequestId(), 1, Periods.monthKey(LocalDate.now()),
                Date.valueOf(LocalDate.now()), key))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void acceptedAppealCreatesOneReversalAndFreesTheCapAgain() {
        String externalId = uniqueName("appeal");
        PointsRequestEntity request = submitHighBounty(externalId);
        PointsLedgerEntity earn = pointsService.approve(request.getId(), reviewer.getId());
        assertThat(earn.getPoints()).isEqualTo(6);

        // 只能申诉自己的加分流水
        assertThatThrownBy(() -> pointsService.submitAppeal(otherMember.getId(), earn.getId(), "不是我的流水"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("只能对自己的流水");

        PointsAppealEntity appeal = pointsService.submitAppeal(member.getId(), earn.getId(), "分数算错了");
        assertThat(appeal.getState()).isEqualTo("OPEN");
        assertThat(appeal.getUserId()).isEqualTo(member.getId());

        pointsService.acceptAppeal(appeal.getId(), reviewer.getId());
        reloadFromDatabase();

        PointsAppealEntity accepted = appealRepository.findById(appeal.getId()).orElseThrow();
        assertThat(accepted.getState()).isEqualTo("ACCEPTED");
        assertThat(accepted.getHandledBy()).isEqualTo(reviewer.getId());

        List<PointsLedgerEntity> ledger = ledgerRepository.findByUserIdOrderByOccurredOnDesc(member.getId());
        List<PointsLedgerEntity> reversals = ledger.stream()
                .filter(entry -> "REVERSAL".equals(entry.getEntryType()))
                .toList();
        assertThat(reversals).hasSize(1);
        PointsLedgerEntity reversal = reversals.get(0);
        assertThat(reversal.getPoints()).isEqualTo(-6);
        assertThat(reversal.getReversesId()).isEqualTo(earn.getId());
        assertThat(reversal.getIdempotencyKey()).isEqualTo("REVERSAL:" + earn.getId());
        assertThat(reversal.getRequestId()).isEqualTo(earn.getRequestId());

        // 原流水没被改，冲抵后该漏洞点累计回到 0，可以重新按封顶计分
        PointsLedgerEntity origin = ledgerRepository.findById(earn.getId()).orElseThrow();
        assertThat(origin.getEntryType()).isEqualTo("EARN");
        assertThat(origin.getPoints()).isEqualTo(6);
        assertThat(ledgerRepository.sumByVulnerability(earn.getVulnerabilityId())).isZero();

        // 重复处理申诉 / 重复申诉都必须是明确的业务错误
        assertThatThrownBy(() -> pointsService.acceptAppeal(appeal.getId(), reviewer.getId()))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("已经处理过");
        assertThatThrownBy(() -> pointsService.submitAppeal(member.getId(), earn.getId(), "再申诉一次"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("已经被冲抵");
        assertThatThrownBy(() -> pointsService.submitAppeal(member.getId(), reversal.getId(), "冲抵流水不能申诉"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("不是加分流水");
    }

    @Test
    void rejectedAppealKeepsOriginalLedgerAndCannotBeHandledTwice() {
        PointsRequestEntity request = submitHighBounty(uniqueName("reject"));
        PointsLedgerEntity earn = pointsService.approve(request.getId(), reviewer.getId());
        PointsAppealEntity appeal = pointsService.submitAppeal(member.getId(), earn.getId(), "想再核对一下");

        pointsService.rejectAppeal(appeal.getId(), reviewer.getId(), "核对无误");
        reloadFromDatabase();

        PointsAppealEntity rejected = appealRepository.findById(appeal.getId()).orElseThrow();
        assertThat(rejected.getState()).isEqualTo("REJECTED");
        assertThat(rejected.getHandledBy()).isEqualTo(reviewer.getId());
        assertThat(ledgerRepository.sumByVulnerability(earn.getVulnerabilityId())).isEqualTo(6);
        assertThat(ledgerRepository.findByUserIdOrderByOccurredOnDesc(member.getId()))
                .noneMatch(entry -> "REVERSAL".equals(entry.getEntryType()));

        assertThatThrownBy(() -> pointsService.rejectAppeal(appeal.getId(), reviewer.getId(), "再驳回一次"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("已经处理过");
    }

    @Test
    void aLedgerCanOnlyHaveOneOpenAppeal() {
        PointsRequestEntity request = submitHighBounty(uniqueName("dup"));
        PointsLedgerEntity earn = pointsService.approve(request.getId(), reviewer.getId());
        pointsService.submitAppeal(member.getId(), earn.getId(), "第一次申诉");

        assertThatThrownBy(() -> pointsService.submitAppeal(member.getId(), earn.getId(), "重复申诉"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("已经有一条待处理的申诉");
    }

    @Test
    void sameVulnerabilityCannotBeResubmittedWithAnotherSeverity() {
        String externalId = uniqueName("severity");
        submitHighBounty(externalId);

        assertThatThrownBy(() -> pointsService.submitBounty(
                member.getId(), "CRITICAL", "SINGLE", "demo.example.com",
                "EDUSRC", externalId, LocalDate.now().minusDays(1), PROOF, reviewer.getId()))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("不能按另一等级");
    }

    private PointsRequestEntity submitHighBounty(String externalId) {
        return pointsService.submitBounty(member.getId(), "HIGH", "SINGLE", "demo.example.com",
                "EDUSRC", externalId, LocalDate.now().minusDays(1), PROOF, reviewer.getId());
    }

    /**
     * handleOpen 是 @Modifying 批量 UPDATE，Hibernate 一级缓存不会自动同步。
     * 测试事务比真实请求长，清一次上下文才能读到库里的申诉终态（生产里下一次请求自然是新事务）。
     */
    private void reloadFromDatabase() {
        entityManager.flush();
        entityManager.clear();
    }
}
