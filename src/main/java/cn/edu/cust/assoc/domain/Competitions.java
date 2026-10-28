package cn.edu.cust.assoc.domain;

import java.util.Arrays;

public final class Competitions {

    private Competitions() {}

    /** 竞赛级别。校赛不计分，但要留档。 */
    public enum Level {
        PROVINCE("省级"), NATIONAL("国赛"), INTERNATIONAL("国际赛"), CAMPUS("校级");

        private final String label;
        Level(String label) { this.label = label; }
        public String getLabel() { return label; }
        public static boolean isValid(String code) {
            return Arrays.stream(values()).anyMatch(v -> v.name().equals(code));
        }
    }

    /** 竞赛类别，能力图的六个维度就用这个。 */
    public enum Category {
        WEB("Web"), PWN("Pwn"), REVERSE("Reverse"), CRYPTO("Crypto"), MISC("Misc"), INCIDENT("应急响应");

        private final String label;
        Category(String label) { this.label = label; }
        public String getLabel() { return label; }
        public static boolean isValid(String code) {
            return Arrays.stream(values()).anyMatch(v -> v.name().equals(code));
        }
    }
}
