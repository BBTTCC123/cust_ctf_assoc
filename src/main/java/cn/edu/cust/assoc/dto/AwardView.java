package cn.edu.cust.assoc.dto;

/** 公开竞赛档案中的个人奖。 */
public record AwardView(Long id, String nickname, String rankLabel, int points) {
}
