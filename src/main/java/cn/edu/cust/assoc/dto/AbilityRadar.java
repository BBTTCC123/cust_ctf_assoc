package cn.edu.cust.assoc.dto;

import cn.edu.cust.assoc.domain.RadarGeometry;
import cn.edu.cust.assoc.domain.RadarLabel;
import cn.edu.cust.assoc.domain.RadarLine;

import java.util.List;

/** 能力雷达：六个竞赛类别，个人奖与团队奖份额两条折线。 */
public record AbilityRadar(List<RadarAxis> axes, List<String> rings, List<RadarLine> spokes,
                           List<RadarLabel> labels, String personalPolygon, String teamPolygon,
                           int max, boolean hasData, String viewBox) {

    public static AbilityRadar of(List<RadarAxis> axes) {
        int max = 1;
        for (RadarAxis axis : axes) {
            max = Math.max(max, axis.total());
        }
        List<Integer> personal = axes.stream().map(RadarAxis::personal).toList();
        List<Integer> team = axes.stream().map(RadarAxis::team).toList();
        boolean hasData = axes.stream().anyMatch(a -> a.total() > 0);
        return new AbilityRadar(axes, RadarGeometry.rings(axes.size()),
                RadarGeometry.spokes(axes.size()),
                RadarGeometry.labels(axes.stream().map(RadarAxis::label).toList()),
                RadarGeometry.polygon(personal, max), RadarGeometry.polygon(team, max),
                max, hasData, RadarGeometry.VIEW_BOX);
    }
}
