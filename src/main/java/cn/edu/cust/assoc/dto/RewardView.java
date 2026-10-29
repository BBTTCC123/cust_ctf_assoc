package cn.edu.cust.assoc.dto;

import java.util.List;

/** 前台兑换页：可用余额、上架资源与我的申请。 */
public record RewardView(long balance, List<RewardItemRow> items,
                         List<RedeemRow> myRequests, List<Long> pendingItemIds) {
}
