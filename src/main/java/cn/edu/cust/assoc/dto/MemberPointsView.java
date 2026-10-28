package cn.edu.cust.assoc.dto;

import java.util.List;
import java.util.Map;

/** 成员公开积分页：只有昵称、总分明细和来源汇总，不含佐证与内部备注。 */
public record MemberPointsView(Long userId, String nickname, int total,
                               Map<String, Integer> bySource, List<MemberLedgerRow> rows) {
}
