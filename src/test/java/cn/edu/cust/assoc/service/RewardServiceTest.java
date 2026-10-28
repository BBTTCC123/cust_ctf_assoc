package cn.edu.cust.assoc.service;

import cn.edu.cust.assoc.common.BizException;
import cn.edu.cust.assoc.domain.RedeemRequestEntity;
import cn.edu.cust.assoc.domain.RewardItemEntity;
import cn.edu.cust.assoc.repository.PointsLedgerRepository;
import cn.edu.cust.assoc.repository.RedeemRequestRepository;
import cn.edu.cust.assoc.repository.RewardItemRepository;
import cn.edu.cust.assoc.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RewardServiceTest {

    private final RewardItemRepository itemRepository = mock(RewardItemRepository.class);
    private final RedeemRequestRepository requestRepository = mock(RedeemRequestRepository.class);
    private final PointsLedgerRepository ledgerRepository = mock(PointsLedgerRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final UserDirectoryService userDirectory = mock(UserDirectoryService.class);
    private final AuditService audit = mock(AuditService.class);
    private final RewardService service = new RewardService(itemRepository, requestRepository,
            ledgerRepository, userRepository, userDirectory, audit);

    @Test
    @DisplayName("可用积分不足时不能申请兑换")
    void applyRejectsInsufficientBalance() {
        when(itemRepository.findById(5L)).thenReturn(Optional.of(item(true, 10, 20)));
        when(requestRepository.existsByUserIdAndItemIdAndState(1L, 5L, "PENDING")).thenReturn(false);
        when(ledgerRepository.sumByUser(1L)).thenReturn(5L);
        when(requestRepository.sumApprovedCostByUser(1L)).thenReturn(0L);

        assertThatThrownBy(() -> service.apply(1L, 5L, null))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("积分不足");
        verify(requestRepository, never()).save(any(RedeemRequestEntity.class));
    }

    @Test
    @DisplayName("余额 = 流水合计 - 已通过兑换成本")
    void balanceSubtractsApprovedCost() {
        when(ledgerRepository.sumByUser(1L)).thenReturn(30L);
        when(requestRepository.sumApprovedCostByUser(1L)).thenReturn(12L);

        assertThat(service.balance(1L)).isEqualTo(18L);
    }

    @Test
    @DisplayName("审核通过时原子扣库存并写审核人")
    void approveTakesStockAndMarksApproved() {
        RedeemRequestEntity request = pendingRequest(7L, 1L, 5L, 8);
        when(requestRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(request));
        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(new cn.edu.cust.assoc.domain.UserEntity()));
        when(ledgerRepository.sumByUser(1L)).thenReturn(20L);
        when(requestRepository.sumApprovedCostByUser(1L)).thenReturn(0L);
        when(itemRepository.takeOneStock(5L)).thenReturn(1);

        service.approve(7L, 99L);

        assertThat(request.getState()).isEqualTo("APPROVED");
        assertThat(request.getReviewedBy()).isEqualTo(99L);
        verify(requestRepository).save(request);
        verify(audit).record(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("库存为 0 时审核失败，申请保持待审")
    void approveRejectsEmptyStock() {
        RedeemRequestEntity request = pendingRequest(7L, 1L, 5L, 8);
        when(requestRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(request));
        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(new cn.edu.cust.assoc.domain.UserEntity()));
        when(ledgerRepository.sumByUser(1L)).thenReturn(20L);
        when(requestRepository.sumApprovedCostByUser(1L)).thenReturn(0L);
        when(itemRepository.takeOneStock(5L)).thenReturn(0);

        assertThatThrownBy(() -> service.approve(7L, 99L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("库存不足");
        assertThat(request.getState()).isEqualTo("PENDING");
        verify(requestRepository, never()).save(request);
    }

    @Test
    @DisplayName("重复审核被拒绝，不会二次扣库存")
    void approveRejectsProcessedRequest() {
        RedeemRequestEntity request = pendingRequest(7L, 1L, 5L, 8);
        request.setState("APPROVED");
        when(requestRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> service.approve(7L, 99L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("已经处理过");
        verify(itemRepository, never()).takeOneStock(any());
    }

    @Test
    @DisplayName("只能取消自己的待审申请")
    void cancelRejectsOtherUsersRequest() {
        RedeemRequestEntity request = pendingRequest(7L, 2L, 5L, 8);
        when(requestRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> service.cancel(1L, 7L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("只能取消自己的申请");
        assertThat(request.getState()).isEqualTo("PENDING");
    }

    private RewardItemEntity item(boolean enabled, int stock, int cost) {
        RewardItemEntity item = new RewardItemEntity();
        item.setName("定制贴纸");
        item.setCostPoints(cost);
        item.setStock(stock);
        item.setEnabled(enabled);
        item.setSortOrder(0);
        return item;
    }

    private RedeemRequestEntity pendingRequest(Long id, Long userId, Long itemId, int cost) {
        RedeemRequestEntity request = new RedeemRequestEntity();
        request.setUserId(userId);
        request.setItemId(itemId);
        request.setItemName("定制贴纸");
        request.setCostPoints(cost);
        request.setState("PENDING");
        return request;
    }
}
