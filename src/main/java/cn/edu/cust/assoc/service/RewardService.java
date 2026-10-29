package cn.edu.cust.assoc.service;

import cn.edu.cust.assoc.common.BizException;
import cn.edu.cust.assoc.domain.RedeemRequestEntity;
import cn.edu.cust.assoc.domain.RewardItemEntity;
import cn.edu.cust.assoc.dto.RedeemRow;
import cn.edu.cust.assoc.dto.RewardItemForm;
import cn.edu.cust.assoc.dto.RewardItemRow;
import cn.edu.cust.assoc.dto.RewardView;
import cn.edu.cust.assoc.repository.PointsLedgerRepository;
import cn.edu.cust.assoc.repository.RedeemRequestRepository;
import cn.edu.cust.assoc.repository.RewardItemRepository;
import cn.edu.cust.assoc.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 积分兑换：资源由后台配置；申请不扣分，审核通过才占用余额与库存。
 * 积分流水保持只增不改 —— 兑换成本单独记在 redeem_request，可用余额 = 流水合计 - 已通过成本。
 */
@Service
public class RewardService {

    private static final String PENDING = "PENDING";
    private static final String APPROVED = "APPROVED";
    private static final String REJECTED = "REJECTED";
    private static final String CANCELLED = "CANCELLED";

    private final RewardItemRepository itemRepository;
    private final RedeemRequestRepository requestRepository;
    private final PointsLedgerRepository ledgerRepository;
    private final UserRepository userRepository;
    private final UserDirectoryService userDirectory;
    private final AuditService audit;

    public RewardService(RewardItemRepository itemRepository,
                         RedeemRequestRepository requestRepository,
                         PointsLedgerRepository ledgerRepository,
                         UserRepository userRepository, UserDirectoryService userDirectory,
                         AuditService audit) {
        this.itemRepository = itemRepository;
        this.requestRepository = requestRepository;
        this.ledgerRepository = ledgerRepository;
        this.userRepository = userRepository;
        this.userDirectory = userDirectory;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public long balance(Long userId) {
        return ledgerRepository.sumByUser(userId) - requestRepository.sumApprovedCostByUser(userId);
    }

    @Transactional(readOnly = true)
    public RewardView catalog(Long userId) {
        List<RewardItemRow> items = itemRepository.findByEnabledTrueOrderBySortOrderAscIdAsc()
                .stream().map(this::itemRow).toList();
        List<RedeemRequestEntity> mine = requestRepository.findByUserIdOrderByCreatedAtDesc(userId);
        String nickname = userDirectory.nickname(userId);
        List<RedeemRow> rows = mine.stream().map(request -> row(request, nickname)).toList();
        List<Long> pendingItemIds = mine.stream()
                .filter(request -> PENDING.equals(request.getState()))
                .map(RedeemRequestEntity::getItemId).distinct().toList();
        return new RewardView(balance(userId), items, rows, pendingItemIds);
    }

    @Transactional
    public void apply(Long userId, Long itemId, String note) {
        String cleanNote = optional(note, 500);
        RewardItemEntity item = itemRepository.findById(itemId)
                .orElseThrow(() -> new BizException("兑换资源不存在"));
        if (!Boolean.TRUE.equals(item.getEnabled())) {
            throw new BizException("这件资源已下架");
        }
        if (item.getStock() <= 0) {
            throw new BizException("这件资源已兑完");
        }
        if (requestRepository.existsByUserIdAndItemIdAndState(userId, itemId, PENDING)) {
            throw new BizException("这件资源已经有一条待审核的申请");
        }
        if (balance(userId) < item.getCostPoints()) {
            throw new BizException("可用积分不足");
        }
        RedeemRequestEntity request = new RedeemRequestEntity();
        request.setUserId(userId);
        request.setItemId(item.getId());
        request.setItemName(item.getName());
        request.setCostPoints(item.getCostPoints());
        request.setState(PENDING);
        request.setNote(cleanNote);
        try {
            request = requestRepository.saveAndFlush(request);
        } catch (DataIntegrityViolationException e) {
            throw new BizException("这件资源已经有一条待审核的申请");
        }
        audit.record("REDEEM_APPLY", "REDEEM_REQUEST", request.getId(), null,
                item.getName() + " " + item.getCostPoints() + " 分");
    }

    @Transactional
    public void cancel(Long userId, Long requestId) {
        RedeemRequestEntity request = requestRepository.findByIdForUpdate(requestId)
                .orElseThrow(() -> new BizException("兑换申请不存在"));
        if (!request.getUserId().equals(userId)) {
            throw new BizException("只能取消自己的申请");
        }
        if (!PENDING.equals(request.getState())) {
            throw new BizException("这条申请已经处理过了");
        }
        request.setState(CANCELLED);
        requestRepository.save(request);
        audit.record("REDEEM_CANCEL", "REDEEM_REQUEST", requestId, PENDING, CANCELLED);
    }

    @Transactional(readOnly = true)
    public List<RewardItemRow> allItems() {
        return itemRepository.findAllByOrderBySortOrderAscIdAsc().stream().map(this::itemRow).toList();
    }

    @Transactional(readOnly = true)
    public List<RedeemRow> allRequests() {
        List<RedeemRequestEntity> requests = requestRepository.findAllByOrderByCreatedAtDesc();
        Map<Long, String> names = userDirectory.nicknames(
                requests.stream().map(RedeemRequestEntity::getUserId).distinct().toList());
        return requests.stream()
                .map(request -> row(request, names.getOrDefault(request.getUserId(), "成员")))
                .toList();
    }

    @Transactional
    public RewardItemEntity createItem(RewardItemForm form) {
        validate(form);
        String name = form.getName().trim();
        if (itemRepository.findByName(name).isPresent()) {
            throw new BizException("同名资源已存在");
        }
        RewardItemEntity item = new RewardItemEntity();
        copy(form, item);
        try {
            item = itemRepository.saveAndFlush(item);
        } catch (DataIntegrityViolationException e) {
            throw new BizException("同名资源已存在");
        }
        audit.record("REWARD_ITEM_CREATE", "REWARD_ITEM", item.getId(), null, name);
        return item;
    }

    @Transactional
    public void updateItem(Long itemId, RewardItemForm form) {
        validate(form);
        RewardItemEntity item = itemRepository.findById(itemId)
                .orElseThrow(() -> new BizException("兑换资源不存在"));
        String name = form.getName().trim();
        itemRepository.findByName(name)
                .filter(other -> !other.getId().equals(itemId))
                .ifPresent(other -> {
                    throw new BizException("同名资源已存在");
                });
        String before = item.getName() + " " + item.getCostPoints() + " 分 / 库存 " + item.getStock();
        copy(form, item);
        itemRepository.save(item);
        audit.record("REWARD_ITEM_UPDATE", "REWARD_ITEM", itemId, before,
                name + " " + form.getCostPoints() + " 分 / 库存 " + form.getStock());
    }

    @Transactional
    public void toggleItem(Long itemId) {
        RewardItemEntity item = itemRepository.findById(itemId)
                .orElseThrow(() -> new BizException("兑换资源不存在"));
        boolean before = Boolean.TRUE.equals(item.getEnabled());
        item.setEnabled(!before);
        itemRepository.save(item);
        audit.record("REWARD_ITEM_TOGGLE", "REWARD_ITEM", itemId,
                before ? "上架" : "下架", before ? "下架" : "上架");
    }

    @Transactional
    public void approve(Long requestId, Long reviewerId) {
        RedeemRequestEntity request = requestRepository.findByIdForUpdate(requestId)
                .orElseThrow(() -> new BizException("兑换申请不存在"));
        if (!PENDING.equals(request.getState())) {
            throw new BizException("这条申请已经处理过了");
        }
        if (reviewerId.equals(request.getUserId())) {
            throw new BizException("不能审核自己的兑换申请");
        }
        // 锁用户行：同一成员的并发审核串行结算，可用余额不会被突破
        userRepository.findByIdForUpdate(request.getUserId())
                .orElseThrow(() -> new BizException("申请成员不存在"));
        if (balance(request.getUserId()) < request.getCostPoints()) {
            throw new BizException("该成员可用积分不足，无法通过");
        }
        if (itemRepository.takeOneStock(request.getItemId()) == 0) {
            throw new BizException("资源库存不足，无法通过");
        }
        request.setState(APPROVED);
        request.setReviewedBy(reviewerId);
        request.setReviewedAt(LocalDateTime.now());
        requestRepository.save(request);
        audit.record("REDEEM_APPROVE", "REDEEM_REQUEST", requestId, PENDING,
                APPROVED + " " + request.getCostPoints() + " 分");
    }

    @Transactional
    public void reject(Long requestId, Long reviewerId, String note) {
        RedeemRequestEntity request = requestRepository.findByIdForUpdate(requestId)
                .orElseThrow(() -> new BizException("兑换申请不存在"));
        if (!PENDING.equals(request.getState())) {
            throw new BizException("这条申请已经处理过了");
        }
        String cleanNote = optional(note, 500);
        request.setState(REJECTED);
        request.setReviewNote(cleanNote);
        request.setReviewedBy(reviewerId);
        request.setReviewedAt(LocalDateTime.now());
        requestRepository.save(request);
        audit.record("REDEEM_REJECT", "REDEEM_REQUEST", requestId, PENDING,
                cleanNote == null ? REJECTED : REJECTED + " " + cleanNote);
    }

    private void validate(RewardItemForm form) {
        String name = form.getName() == null ? "" : form.getName().trim();
        if (name.isEmpty() || name.length() > 100) {
            throw new BizException("资源名称长度需在 1 到 100 个字符之间");
        }
        if (form.getCostPoints() <= 0) {
            throw new BizException("所需积分必须大于 0");
        }
        if (form.getStock() < 0) {
            throw new BizException("库存不能为负");
        }
        if (form.getDescription() != null && form.getDescription().length() > 500) {
            throw new BizException("资源说明不能超过 500 个字符");
        }
    }

    private void copy(RewardItemForm form, RewardItemEntity item) {
        item.setName(form.getName().trim());
        item.setDescription(optional(form.getDescription(), 500));
        item.setCostPoints(form.getCostPoints());
        item.setStock(form.getStock());
        item.setSortOrder(form.getSortOrder());
        item.setEnabled(form.isEnabled());
    }

    private RewardItemRow itemRow(RewardItemEntity item) {
        return new RewardItemRow(item.getId(), item.getName(), item.getDescription(),
                item.getCostPoints(), item.getStock(), Boolean.TRUE.equals(item.getEnabled()),
                item.getSortOrder());
    }

    private RedeemRow row(RedeemRequestEntity request, String userName) {
        return new RedeemRow(request.getId(), request.getUserId(), userName, request.getItemName(),
                request.getCostPoints(), request.getState(), request.getNote(),
                request.getReviewNote(), request.getCreatedAt(), request.getReviewedAt());
    }

    private String optional(String value, int max) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String text = value.trim();
        if (text.length() > max) {
            throw new BizException("填写内容不能超过 " + max + " 个字符");
        }
        return text;
    }
}
