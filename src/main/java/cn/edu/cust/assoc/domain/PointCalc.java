package cn.edu.cust.assoc.domain;

/**
 * 积分计算。全部是纯函数，方便直接测。
 *
 * 挖洞：基础分 = 10 - 2 × 等级序号（严重 1 / 高危 2 / 中危 3 / 低危 4）→ 8/6/4/2
 * 场景系数：单洞 1，通杀 / 公益 各 1/2（两者不叠乘，只减一次）
 * 封顶：同一个漏洞点累计不超过 10 分，超出部分不记。
 *
 * 护网（唯一业务口径）：
 *   1000 分以下        0 分
 *   1000 ~ 4999        3 分
 *   5000 ~ 9999        5 分起，每多 1000 加 1 分（5000=5 … 9999=9）
 *   10000 起           10 分起，每多 5000 加 2 分（15000=12 …），封顶 20
 *
 * 这里的常量是数据库 points_rule 没有对应规则时使用的兜底值；
 * 实际计分优先读 points_rule 的已发布版本（见 PointsRuleService）。
 */
public final class PointCalc {

    private PointCalc() {}

    public static final int VULN_CAP = 10;

    public static final int HW_BASE_1000 = 3;
    public static final int HW_BASE_5000 = 5;
    public static final int HW_BASE_10000 = 10;
    public static final int HW_STEP = 2;
    public static final int HW_CAP = 20;

    public enum Level {
        CRITICAL(1, "严重"), HIGH(2, "高危"), MEDIUM(3, "中危"), LOW(4, "低危");

        private final int rank;
        private final String label;
        Level(int rank, String label) { this.rank = rank; this.label = label; }
        public String getLabel() { return label; }

        public static boolean isValid(String code) {
            for (Level l : values()) if (l.name().equals(code)) return true;
            return false;
        }

        public static int rankOf(String code) {
            for (Level l : values()) if (l.name().equals(code)) return l.rank;
            throw new IllegalArgumentException("未知的漏洞等级：" + code);
        }
    }

    public enum Scene {
        SINGLE("单洞"), SWEEP("通杀"), CHARITY("公益");

        private final String label;
        Scene(String label) { this.label = label; }
        public String getLabel() { return label; }

        public static boolean isValid(String code) {
            for (Scene s : values()) if (s.name().equals(code)) return true;
            return false;
        }
    }

    /** 基础分：8 / 6 / 4 / 2 */
    public static int base(String level) {
        return 10 - 2 * Level.rankOf(level);
    }

    /** 场景后的单价：通杀和公益减半；两者同时出现也只减一次。 */
    public static int unitPoints(String level, String scene) {
        int base = base(level);
        boolean halved = "SWEEP".equals(scene) || "CHARITY".equals(scene);
        // 基础分都是偶数，减半不会出现小数
        return halved ? base / 2 : base;
    }

    /**
     * 同一个漏洞点累计封顶 10 分：
     * 已经入账 existingSum，这次想记 want，实际能记多少。
     */
    public static int clampToCap(int existingSum, int want) {
        if (want <= 0) return 0;
        int remaining = VULN_CAP - existingSum;
        if (remaining <= 0) return 0;
        return Math.min(want, remaining);
    }

    /** 护网折算：默认规则。 */
    public static int hw(int score) {
        return hw(score, HW_BASE_1000, HW_BASE_5000, HW_BASE_10000, HW_STEP);
    }

    /** 护网折算：规则表传入的分段参数。 */
    public static int hw(int score, int base1000, int base5000, int base10000, int step) {
        if (score < 1000) return 0;
        if (score < 5000) return base1000;
        if (score < 10000) return base5000 + (score - 5000) / 1000;
        return Math.min(HW_CAP, base10000 + step * ((score - 10000) / 5000));
    }
}
