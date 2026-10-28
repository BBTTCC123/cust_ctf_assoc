package cn.edu.cust.assoc.service;

import cn.edu.cust.assoc.common.BizException;
import cn.edu.cust.assoc.domain.*;
import cn.edu.cust.assoc.repository.PointsAppealRepository;
import cn.edu.cust.assoc.repository.PointsLedgerRepository;
import cn.edu.cust.assoc.repository.PointsRequestRepository;
import cn.edu.cust.assoc.repository.VulnerabilityRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class PointsService {

    private final PointsRequestRepository requestRepository;
    private final PointsLedgerRepository ledgerRepository;
    private final PointsAppealRepository appealRepository;
    private final VulnerabilityRepository vulnerabilityRepository;
    private final PointsRuleService ruleService;
    private final AuditService audit;
    private final LocalDate backfillStart;

    public PointsService(PointsRequestRepository requestRepository,
                         PointsLedgerRepository ledgerRepository,
                         PointsAppealRepository appealRepository,
                         VulnerabilityRepository vulnerabilityRepository,
                         PointsRuleService ruleService,
                         AuditService audit,
                         @Value("${app.points.backfill-start:2015-01-01}") String backfillStart) {
        this.requestRepository = requestRepository;
        this.ledgerRepository = ledgerRepository;
        this.appealRepository = appealRepository;
        this.vulnerabilityRepository = vulnerabilityRepository;
        this.ruleService = ruleService;
        this.audit = audit;
        this.backfillStart = LocalDate.parse(backfillStart);
    }

    @Transactional(readOnly = true)
    public List<PointsRequestEntity> pending() {
        return requestRepository.findByStateOrderByCreatedAtAsc("PENDING");
    }

    @Transactional(readOnly = true)
    public List<PointsRequestEntity> all() {
        return requestRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<PointsAppealEntity> appeals() {
        return appealRepository.findAllByOrderByCreatedAtDesc();
    }

    /** 提交一笔挖洞积分申请：分数由「等级 + 场景」算，不让人手填；漏洞点必须落到 vulnerability 表。 */
    @Transactional
    public PointsRequestEntity submitBounty(Long userId, String level, String scene, String target,
                                            String platform, String externalId, LocalDate occurredOn,
                                            String proofUrl, Long submittedBy) {
        if (!PointCalc.Level.isValid(level)) throw new BizException("未知等级：" + level);
        if (!PointCalc.Scene.isValid(scene)) throw new BizException("未知场景：" + scene);
        if (target == null || target.isBlank()) throw new BizException("目标不能为空");
        if (platform == null || platform.isBlank()) throw new BizException("漏洞平台不能为空");
        if (externalId == null || externalId.isBlank()) throw new BizException("漏洞编号不能为空");
        if (proofUrl == null || proofUrl.isBlank()) throw new BizException("必须填佐证链接");
        validateOccurredOn(occurredOn);

        VulnerabilityEntity vulnerability = findOrCreateVulnerability(
                platform.trim(), externalId.trim(), level, userId, occurredOn);

        int base = ruleService.bountyBase(level, occurredOn);
        int points = ("SWEEP".equals(scene) || "CHARITY".equals(scene)) ? Math.max(1, base / 2) : base;

        PointsRequestEntity r = new PointsRequestEntity();
        r.setUserId(userId);
        r.setSourceType("BOUNTY");
        r.setVulnerabilityId(vulnerability.getId());
        r.setScene(scene);
        r.setLevel(level);
        r.setTarget(target.trim());
        r.setOccurredOn(occurredOn);
        r.setPoints(points);
        r.setState("PENDING");
        r.setProofUrl(proofUrl.trim());
        r.setSubmittedBy(submittedBy);
        return requestRepository.save(r);
    }

    /** 登记护网成绩：分数按规则表折算后进入同一条申请/审核链路。 */
    @Transactional
    public PointsRequestEntity submitHw(Long userId, int score, LocalDate occurredOn,
                                        String proofUrl, Long submittedBy) {
        if (score < 0 || score > 10_000_000) throw new BizException("护网得分不在合理范围");
        if (proofUrl == null || proofUrl.isBlank()) throw new BizException("必须填佐证链接");
        validateOccurredOn(occurredOn);

        int points = ruleService.hwPoints(score, occurredOn);
        PointsRequestEntity r = new PointsRequestEntity();
        r.setUserId(userId);
        r.setSourceType("HW");
        r.setTarget("护网 " + score + " 分");
        r.setOccurredOn(occurredOn);
        r.setPoints(points);
        r.setState("PENDING");
        r.setProofUrl(proofUrl.trim());
        r.setSubmittedBy(submittedBy);
        return requestRepository.save(r);
    }

    /**
     * 审核通过 → 生成流水。幂等键用 requestId，重复点不会重复入账。
     * 同一个漏洞点累计封顶 10 分：审核时锁住漏洞行，并发审核也突破不了。
     */
    @Transactional
    public PointsLedgerEntity approve(Long requestId, Long reviewerId) {
        PointsRequestEntity request = requestRepository.findByIdForUpdate(requestId)
                .orElseThrow(() -> new BizException("申请单不存在：" + requestId));
        if (!"PENDING".equals(request.getState())) {
            throw new BizException("这张申请单已经处理过了");
        }

        String key = "EARN:" + request.getId();
        Optional<PointsLedgerEntity> existing = ledgerRepository.findByIdempotencyKey(key);
        if (existing.isPresent()) {
            return existing.get();
        }

        int points = request.getPoints();
        String note = null;
        if ("BOUNTY".equals(request.getSourceType()) && request.getVulnerabilityId() != null) {
            // 锁住漏洞点：同一漏洞点的多笔申请同时审核时，谁先拿到锁谁先记账
            vulnerabilityRepository.findByIdForUpdate(request.getVulnerabilityId())
                    .orElseThrow(() -> new BizException("漏洞点不存在"));
            int already = ledgerRepository.sumByVulnerability(request.getVulnerabilityId());
            int allowed = PointCalc.clampToCap(already, points);
            if (allowed < points) {
                note = "按单漏洞点封顶 " + PointCalc.VULN_CAP + " 分截断（已入账 " + already + "）";
            }
            points = allowed;
        }

        LocalDate today = LocalDate.now();
        PointsLedgerEntity ledger = new PointsLedgerEntity();
        ledger.setUserId(request.getUserId());
        ledger.setRequestId(request.getId());
        ledger.setEntryType("EARN");
        ledger.setPoints(points);
        ledger.setVulnerabilityId(request.getVulnerabilityId());
        ledger.setOccurredOn(request.getOccurredOn());
        ledger.setPeriodMonth(Periods.periodMonth(request.getOccurredOn(), today));
        ledger.setIdempotencyKey(key);
        ledger.setNote(note);

        request.setState("APPROVED");
        request.setReviewedBy(reviewerId);
        request.setReviewedAt(LocalDateTime.now());
        requestRepository.save(request);
        PointsLedgerEntity saved = ledgerRepository.save(ledger);
        audit.record("POINTS_APPROVE", "POINTS_REQUEST", requestId, "PENDING",
                "APPROVED " + points + " 分");
        return saved;
    }

    /**
     * 比赛奖等「录入即成立」的来源也走同一张 request + ledger：
     * request 直接以 APPROVED 落库，ledger 用调用方给的幂等键兜底。
     */
    @Transactional
    public PointsLedgerEntity recordApproved(Long userId, String sourceType, Long competitionId, int points,
                                             LocalDate occurredOn, String proofUrl, String note,
                                             String idempotencyKey, Long operatorId) {
        Optional<PointsLedgerEntity> existing = ledgerRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return existing.get();
        }
        validateOccurredOn(occurredOn);

        PointsRequestEntity request = new PointsRequestEntity();
        request.setUserId(userId);
        request.setSourceType(sourceType);
        request.setCompetitionId(competitionId);
        request.setOccurredOn(occurredOn);
        request.setPoints(points);
        request.setState("APPROVED");
        request.setProofUrl(proofUrl);
        request.setSubmittedBy(operatorId);
        request.setReviewedBy(operatorId);
        request.setReviewedAt(LocalDateTime.now());
        request = requestRepository.save(request);

        PointsLedgerEntity ledger = new PointsLedgerEntity();
        ledger.setUserId(userId);
        ledger.setRequestId(request.getId());
        ledger.setEntryType("EARN");
        ledger.setPoints(points);
        ledger.setOccurredOn(occurredOn);
        ledger.setPeriodMonth(Periods.periodMonth(occurredOn, LocalDate.now()));
        ledger.setIdempotencyKey(idempotencyKey);
        ledger.setNote(note);
        return ledgerRepository.save(ledger);
    }

    @Transactional
    public void reject(Long requestId, Long reviewerId, String reason) {
        PointsRequestEntity request = requestRepository.findByIdForUpdate(requestId)
                .orElseThrow(() -> new BizException("申请单不存在：" + requestId));
        if (!"PENDING".equals(request.getState())) {
            throw new BizException("这张申请单已经处理过了");
        }
        request.setState("REJECTED");
        request.setReason(reason);
        request.setReviewedBy(reviewerId);
        request.setReviewedAt(LocalDateTime.now());
        requestRepository.save(request);
        audit.record("POINTS_REJECT", "POINTS_REQUEST", requestId, "PENDING", "REJECTED " + reason);
    }

    /** 撤销：原流水一个字不动，另加一条负数，并记下冲抵的是哪一笔。 */
    @Transactional
    public PointsLedgerEntity reverse(Long ledgerId, String reason) {
        PointsLedgerEntity origin = ledgerRepository.findById(ledgerId)
                .orElseThrow(() -> new BizException("流水不存在：" + ledgerId));
        if (!"EARN".equals(origin.getEntryType())) {
            throw new BizException("只有正向流水才能被冲抵");
        }
        String key = "REVERSAL:" + ledgerId;
        Optional<PointsLedgerEntity> existing = ledgerRepository.findByIdempotencyKey(key);
        if (existing.isPresent()) {
            return existing.get();
        }
        PointsLedgerEntity re = new PointsLedgerEntity();
        re.setUserId(origin.getUserId());
        re.setRequestId(origin.getRequestId());
        re.setEntryType("REVERSAL");
        re.setPoints(-origin.getPoints());
        re.setVulnerabilityId(origin.getVulnerabilityId());
        re.setOccurredOn(origin.getOccurredOn());
        re.setPeriodMonth(Periods.openMonth(LocalDate.now()));
        re.setIdempotencyKey(key);
        re.setReversesId(origin.getId());
        re.setNote(reason);
        PointsLedgerEntity saved = ledgerRepository.save(re);
        audit.record("POINTS_REVERSE", "POINTS_LEDGER", ledgerId,
                "原流水 " + origin.getPoints() + " 分", "已生成冲抵流水 " + saved.getId());
        return saved;
    }

    /** 用户申诉自己的流水：必须本人、必须是加分流水、未被冲抵、没有待处理申诉。 */
    @Transactional
    public PointsAppealEntity submitAppeal(Long userId, Long ledgerId, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new BizException("申诉要写理由");
        }
        if (reason.length() > 500) {
            throw new BizException("申诉理由不能超过 500 字");
        }
        PointsLedgerEntity ledger = ledgerRepository.findById(ledgerId)
                .orElseThrow(() -> new BizException("流水不存在"));
        if (!ledger.getUserId().equals(userId)) {
            throw new BizException("只能对自己的流水发起申诉");
        }
        if (!"EARN".equals(ledger.getEntryType())) {
            throw new BizException("这笔流水不是加分流水，不能申诉");
        }
        if (ledgerRepository.existsByReversesId(ledgerId)) {
            throw new BizException("这笔流水已经被冲抵，无需再申诉");
        }
        if (appealRepository.existsByLedgerIdAndState(ledgerId, "OPEN")) {
            throw new BizException("这笔流水已经有一条待处理的申诉");
        }
        PointsAppealEntity appeal = new PointsAppealEntity();
        appeal.setLedgerId(ledgerId);
        appeal.setUserId(userId);
        appeal.setReason(reason.trim());
        appeal.setState("OPEN");
        PointsAppealEntity saved = appealRepository.save(appeal);
        audit.record("APPEAL_SUBMIT", "APPEAL", saved.getId(), null, "流水 " + ledgerId);
        return saved;
    }

    /** 申诉成立：原子流转 OPEN -> ACCEPTED，写处理人，并冲抵原流水。 */
    @Transactional
    public void acceptAppeal(Long appealId, Long reviewerId) {
        int updated = appealRepository.handleOpen(appealId, "ACCEPTED", reviewerId, LocalDateTime.now());
        if (updated == 0) {
            throw new BizException("这条申诉已经处理过了");
        }
        PointsAppealEntity appeal = appealRepository.findById(appealId)
                .orElseThrow(() -> new BizException("申诉不存在"));
        reverse(appeal.getLedgerId(), "申诉成立，冲抵原流水 #" + appeal.getLedgerId());
        audit.record("APPEAL_ACCEPT", "APPEAL", appealId, "OPEN", "ACCEPTED");
    }

    /** 申诉驳回：同样只能处理 OPEN，写处理人。 */
    @Transactional
    public void rejectAppeal(Long appealId, Long reviewerId, String note) {
        int updated = appealRepository.handleOpen(appealId, "REJECTED", reviewerId, LocalDateTime.now());
        if (updated == 0) {
            throw new BizException("这条申诉已经处理过了");
        }
        audit.record("APPEAL_REJECT", "APPEAL", appealId, "OPEN",
                note == null ? "REJECTED" : "REJECTED " + note);
    }

    @Transactional(readOnly = true)
    public List<PointsLedgerEntity> ledgerOf(Long userId) {
        return ledgerRepository.findByUserIdOrderByOccurredOnDesc(userId);
    }

    /** 四个榜：总 / 年 / 季 / 月。列表必须限行，不能把全表拉进内存。 */
    @Transactional(readOnly = true)
    public List<Object[]> ranking(String type, int limit) {
        Pageable page = PageRequest.of(0, Math.max(1, Math.min(limit, 500)));
        LocalDate today = LocalDate.now();
        return switch (type) {
            case "YEAR" -> ledgerRepository.rankByPrefix(Periods.yearPrefix(today), page);
            case "QUARTER" -> ledgerRepository.rankByMonths(Periods.quarterMonths(today), page);
            case "MONTH" -> ledgerRepository.rankByMonths(List.of(Periods.currentMonthKey(today)), page);
            default -> ledgerRepository.rankAll(page);
        };
    }

    private void validateOccurredOn(LocalDate occurredOn) {
        if (occurredOn == null) {
            throw new BizException("发生日期不能为空");
        }
        LocalDate today = LocalDate.now();
        if (occurredOn.isAfter(today)) {
            throw new BizException("发生日期不能晚于今天");
        }
        if (occurredOn.isBefore(backfillStart)) {
            throw new BizException("发生日期早于允许补录的最早日期（" + backfillStart + "）");
        }
    }

    private VulnerabilityEntity findOrCreateVulnerability(String platform, String externalId, String level,
                                                          Long reporterId, LocalDate firstSeenOn) {
        Optional<VulnerabilityEntity> existing = vulnerabilityRepository
                .findByPlatformAndExternalId(platform, externalId);
        if (existing.isPresent()) {
            return existing.get();
        }
        VulnerabilityEntity vulnerability = new VulnerabilityEntity();
        vulnerability.setPlatform(platform);
        vulnerability.setExternalId(externalId);
        vulnerability.setLevel(level);
        vulnerability.setReporterUserId(reporterId);
        vulnerability.setFirstSeenOn(firstSeenOn);
        try {
            return vulnerabilityRepository.saveAndFlush(vulnerability);
        } catch (DataIntegrityViolationException e) {
            // 并发下另一个请求先建了同一个漏洞点：回读即可
            return vulnerabilityRepository.findByPlatformAndExternalId(platform, externalId)
                    .orElseThrow(() -> e);
        }
    }
}
