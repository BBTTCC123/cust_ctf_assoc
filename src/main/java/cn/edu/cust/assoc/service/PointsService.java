package cn.edu.cust.assoc.service;

import cn.edu.cust.assoc.domain.*;
import cn.edu.cust.assoc.repository.PointsLedgerRepository;
import cn.edu.cust.assoc.repository.PointsRequestRepository;
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

    public PointsService(PointsRequestRepository requestRepository,
                         PointsLedgerRepository ledgerRepository) {
        this.requestRepository = requestRepository;
        this.ledgerRepository = ledgerRepository;
    }

    @Transactional(readOnly = true)
    public List<PointsRequestEntity> pending() {
        return requestRepository.findByStateOrderByCreatedAtAsc("PENDING");
    }

    @Transactional(readOnly = true)
    public List<PointsRequestEntity> all() {
        return requestRepository.findAllByOrderByCreatedAtDesc();
    }

    /** 提交一笔挖洞积分申请：分数由「等级 + 场景」算，不让人手填。 */
    @Transactional
    public PointsRequestEntity submitBounty(Long userId, String level, String scene, String target,
                                            LocalDate occurredOn, String proofUrl, Long submittedBy) {
        if (!PointCalc.Level.isValid(level)) throw new IllegalArgumentException("未知等级：" + level);
        if (!PointCalc.Scene.isValid(scene)) throw new IllegalArgumentException("未知场景：" + scene);
        if (target == null || target.isBlank()) throw new IllegalArgumentException("目标不能为空");
        if (proofUrl == null || proofUrl.isBlank()) throw new IllegalArgumentException("必须填佐证链接");

        PointsRequestEntity r = new PointsRequestEntity();
        r.setUserId(userId);
        r.setSourceType("BOUNTY");
        r.setScene(scene);
        r.setTarget(target.trim());
        r.setOccurredOn(occurredOn);
        r.setPoints(PointCalc.unitPoints(level, scene));
        r.setState("PENDING");
        r.setProofUrl(proofUrl.trim());
        r.setSubmittedBy(submittedBy);
        return requestRepository.save(r);
    }

    /**
     * 审核通过 → 生成流水。幂等键用 requestId，重复点不会重复入账。
     * 同一个漏洞点累计封顶 10 分，超出部分截断并写进备注。
     */
    @Transactional
    public PointsLedgerEntity approve(Long requestId, Long reviewerId) {
        PointsRequestEntity request = requestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("申请单不存在：" + requestId));
        if (!"PENDING".equals(request.getState())) {
            throw new IllegalStateException("这张申请单已经处理过了");
        }

        String key = "EARN:" + request.getId();
        Optional<PointsLedgerEntity> existing = ledgerRepository.findByIdempotencyKey(key);
        if (existing.isPresent()) {
            return existing.get();
        }

        int points = request.getPoints();
        String note = null;
        if ("BOUNTY".equals(request.getSourceType()) && request.getVulnerabilityId() != null) {
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
        return ledgerRepository.save(ledger);
    }

    @Transactional
    public void reject(Long requestId, Long reviewerId, String reason) {
        PointsRequestEntity request = requestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("申请单不存在：" + requestId));
        if (!"PENDING".equals(request.getState())) {
            throw new IllegalStateException("这张申请单已经处理过了");
        }
        request.setState("REJECTED");
        request.setReason(reason);
        request.setReviewedBy(reviewerId);
        request.setReviewedAt(LocalDateTime.now());
        requestRepository.save(request);
    }

    /** 撤销：原流水一个字不动，另加一条负数，并记下冲抵的是哪一笔。 */
    @Transactional
    public PointsLedgerEntity reverse(Long ledgerId, String reason) {
        PointsLedgerEntity origin = ledgerRepository.findById(ledgerId)
                .orElseThrow(() -> new IllegalArgumentException("流水不存在：" + ledgerId));
        if (!"EARN".equals(origin.getEntryType())) {
            throw new IllegalStateException("只有正向流水才能被冲抵");
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
        re.setPeriodMonth(Periods.currentMonthKey(LocalDate.now()));
        re.setIdempotencyKey(key);
        re.setReversesId(origin.getId());
        re.setNote(reason);
        return ledgerRepository.save(re);
    }

    @Transactional(readOnly = true)
    public List<PointsLedgerEntity> ledgerOf(Long userId) {
        return ledgerRepository.findByUserIdOrderByOccurredOnDesc(userId);
    }

    /** 四个榜：总 / 年 / 季 / 月。 */
    @Transactional(readOnly = true)
    public List<Object[]> ranking(String type) {
        LocalDate today = LocalDate.now();
        return switch (type) {
            case "YEAR" -> ledgerRepository.rankByPrefix(Periods.yearPrefix(today));
            case "QUARTER" -> ledgerRepository.rankByMonths(Periods.quarterMonths(today));
            case "MONTH" -> ledgerRepository.rankByMonths(List.of(Periods.currentMonthKey(today)));
            default -> ledgerRepository.rankAll();
        };
    }
}
