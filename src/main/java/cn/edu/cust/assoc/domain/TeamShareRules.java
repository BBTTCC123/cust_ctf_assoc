package cn.edu.cust.assoc.domain;

import java.util.List;
import java.util.Map;

/**
 * 团队奖分配的纯规则：分给成员的合计必须等于这个奖的总分，一分不能多、一分不能少。
 * 单独抽出来是为了能脱离数据库做单元测试。
 */
public final class TeamShareRules {

    private TeamShareRules() {}

    public static void validate(int basePoints, Map<Long, Integer> shares) {
        if (shares == null || shares.isEmpty()) {
            throw new IllegalArgumentException("至少要分给一个成员");
        }
        int sum = 0;
        for (Map.Entry<Long, Integer> e : shares.entrySet()) {
            Integer p = e.getValue();
            if (p == null || p < 0) {
                throw new IllegalArgumentException("成员分数不能为负");
            }
            if (p > basePoints) {
                throw new IllegalArgumentException("单个成员拿到的分不能超过这个奖的总分");
            }
            sum += p;
        }
        if (sum != basePoints) {
            throw new IllegalArgumentException("分给成员的合计是 " + sum + "，必须等于奖项总分 " + basePoints);
        }
    }

    /** 用于校验前的自查：重复成员会被 Map 天然合并，这里给调用方一个显式检查入口。 */
    public static void validateNoDuplicate(List<Long> userIds) {
        if (userIds.size() != userIds.stream().distinct().count()) {
            throw new IllegalArgumentException("同一个成员不能出现两次");
        }
    }
}
