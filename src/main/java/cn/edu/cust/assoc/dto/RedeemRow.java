package cn.edu.cust.assoc.dto;

import java.time.LocalDateTime;

/** 兑换申请行（前台我的申请 / 后台审核共用）。 */
public record RedeemRow(Long id, Long userId, String userName, String itemName, int costPoints,
                        String state, String note, String reviewNote,
                        LocalDateTime createdAt, LocalDateTime reviewedAt) {
}
