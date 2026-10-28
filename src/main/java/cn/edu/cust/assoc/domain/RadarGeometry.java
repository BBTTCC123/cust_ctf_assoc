package cn.edu.cust.assoc.domain;

/**
 * 雷达图几何计算。抽成纯函数是为了能脱离模板做单元测试：
 * 六边形网格、轴线与标签位置都在这里算好，模板只负责画。
 */
public final class RadarGeometry {

    public static final int CENTER_X = 180;
    public static final int CENTER_Y = 150;
    public static final int RADIUS = 105;
    public static final int LABEL_RADIUS = 128;
    public static final String VIEW_BOX = "0 0 360 300";

    private RadarGeometry() {}

    /** 按轴顺序（从正上方开始顺时针）把一组数值折算成 SVG polygon 的 points 属性。 */
    public static String polygon(java.util.List<Integer> values, int max) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        StringBuilder points = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            double ratio = Math.max(0, Math.min(1, values.get(i) / (double) max));
            double angle = angleOf(i, values.size());
            if (i > 0) points.append(' ');
            points.append(fmt(CENTER_X + RADIUS * ratio * Math.cos(angle)))
                    .append(',')
                    .append(fmt(CENTER_Y + RADIUS * ratio * Math.sin(angle)));
        }
        return points.toString();
    }

    /** 25% / 50% / 75% / 100% 四层网格。 */
    public static java.util.List<String> rings(int axisCount) {
        java.util.List<String> rings = new java.util.ArrayList<>();
        for (int percent : new int[]{25, 50, 75, 100}) {
            java.util.List<Integer> values = new java.util.ArrayList<>();
            for (int i = 0; i < axisCount; i++) {
                values.add(percent);
            }
            rings.add(polygon(values, 100));
        }
        return rings;
    }

    /** 中心到各顶点的轴线。 */
    public static java.util.List<RadarLine> spokes(int axisCount) {
        java.util.List<RadarLine> spokes = new java.util.ArrayList<>();
        for (int i = 0; i < axisCount; i++) {
            double angle = angleOf(i, axisCount);
            spokes.add(new RadarLine(
                    fmt(CENTER_X), fmt(CENTER_Y),
                    fmt(CENTER_X + RADIUS * Math.cos(angle)),
                    fmt(CENTER_Y + RADIUS * Math.sin(angle))));
        }
        return spokes;
    }

    public static java.util.List<RadarLabel> labels(java.util.List<String> names) {
        java.util.List<RadarLabel> labels = new java.util.ArrayList<>();
        for (int i = 0; i < names.size(); i++) {
            double angle = angleOf(i, names.size());
            double x = CENTER_X + LABEL_RADIUS * Math.cos(angle);
            double y = CENTER_Y + LABEL_RADIUS * Math.sin(angle);
            String anchor = x > CENTER_X + 8 ? "start" : (x < CENTER_X - 8 ? "end" : "middle");
            labels.add(new RadarLabel(names.get(i), fmt(x), fmt(y + 4), anchor));
        }
        return labels;
    }

    private static double angleOf(int index, int count) {
        return -Math.PI / 2 + 2 * Math.PI * index / count;
    }

    private static String fmt(double value) {
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }
}
