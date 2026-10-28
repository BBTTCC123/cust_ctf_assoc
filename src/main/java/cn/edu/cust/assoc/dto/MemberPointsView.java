package cn.edu.cust.assoc.dto;

import java.util.List;
import java.util.Map;

/** 成员公开积分页：昵称、分数构成、公开资料与能力概览；不含佐证和内部备注。 */
public record MemberPointsView(Long userId, String nickname, int total,
                               Map<String, Integer> bySource, List<MemberLedgerRow> rows,
                               ProfileView profile, List<AbilityRow> ability) {
}
