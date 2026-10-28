package cn.edu.cust.assoc.domain;

import java.util.Map;

/**
 * 竞赛分值表。V4 会把它迁到 points_rule 表（带版本与生效日），现在先放在代码里，
 * 因为 M2 阶段还没有规则版本化的需求。
 */
public final class CompetitionPoints {

    private CompetitionPoints() {}

    /** 名次：一等 / 二等 / 三等 / 优胜 */
    public enum Rank {
        FIRST("一等"), SECOND("二等"), THIRD("三等"), HONOR("优胜");

        private final String label;
        Rank(String label) { this.label = label; }
        public String getLabel() { return label; }
        public static boolean isValid(String code) {
            for (Rank r : values()) {
                if (r.name().equals(code)) return true;
            }
            return false;
        }
    }

    /** 级别 × 名次 = 分数。校赛 0 分：比赛记录留档，但不计入积分。 */
    private static final Map<String, int[]> TABLE = Map.of(
            "PROVINCE", new int[]{10, 7, 5, 0},
            "NATIONAL", new int[]{15, 13, 11, 0},
            "INTERNATIONAL", new int[]{20, 18, 16, 0},
            "CAMPUS", new int[]{0, 0, 0, 0}
    );

    public static int of(String level, String rank) {
        int[] row = TABLE.get(level);
        if (row == null) {
            throw new IllegalArgumentException("未知的比赛级别：" + level);
        }
        return row[rankIndex(rank)];
    }

    private static int rankIndex(String rank) {
        return switch (rank) {
            case "FIRST" -> 0;
            case "SECOND" -> 1;
            case "THIRD" -> 2;
            case "HONOR" -> 3;
            default -> throw new IllegalArgumentException("未知的名次：" + rank);
        };
    }
}
