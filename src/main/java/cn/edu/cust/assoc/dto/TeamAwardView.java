package cn.edu.cust.assoc.dto;

import java.util.List;

/** 公开竞赛档案中的团队奖。 */
public record TeamAwardView(Long id, String rankLabel, int basePoints, List<ShareView> shares) {
}
