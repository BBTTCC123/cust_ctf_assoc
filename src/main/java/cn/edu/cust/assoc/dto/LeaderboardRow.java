package cn.edu.cust.assoc.dto;

/** 榜单行：只暴露昵称和分数，不暴露登录名、邮箱、学号或数据库 ID 的展示文本。 */
public record LeaderboardRow(int rank, Long userId, String nickname, long total) {
}
