package cn.edu.cust.assoc.dto;

import cn.edu.cust.assoc.domain.RadarLabel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AbilityRadarTest {

    @Test
    @DisplayName("六个类别始终都在，最大值为个人/团队中的最高次数")
    void sixAxesWithMax() {
        AbilityRadar radar = AbilityRadar.of(List.of(
                new RadarAxis("Web", 2, 1),
                new RadarAxis("Pwn", 0, 0),
                new RadarAxis("Reverse", 0, 0),
                new RadarAxis("Crypto", 0, 0),
                new RadarAxis("Misc", 0, 0),
                new RadarAxis("应急响应", 0, 0)));

        assertThat(radar.axes()).hasSize(6);
        assertThat(radar.max()).isEqualTo(3);
        assertThat(radar.hasData()).isTrue();
        assertThat(radar.rings()).hasSize(4);
        assertThat(radar.spokes()).hasSize(6);
        assertThat(radar.labels()).hasSize(6);
        // 顶部轴（Web）个人 2/3、团队 1/3，最大轴落在半径上
        assertThat(radar.personalPolygon()).startsWith("180.0,80.0");
        assertThat(radar.teamPolygon()).startsWith("180.0,115.0");
    }

    @Test
    @DisplayName("没有任何获奖时不算有数据，但仍保留六个轴")
    void emptyRadar() {
        AbilityRadar radar = AbilityRadar.of(List.of(
                new RadarAxis("Web", 0, 0), new RadarAxis("Pwn", 0, 0),
                new RadarAxis("Reverse", 0, 0), new RadarAxis("Crypto", 0, 0),
                new RadarAxis("Misc", 0, 0), new RadarAxis("应急响应", 0, 0)));

        assertThat(radar.hasData()).isFalse();
        assertThat(radar.axes()).hasSize(6);
        assertThat(radar.max()).isEqualTo(1);
        assertThat(radar.personalPolygon()).isEqualTo("180.0,150.0 180.0,150.0 180.0,150.0 180.0,150.0 180.0,150.0 180.0,150.0");
    }

    @Test
    @DisplayName("顶部标签居中、右侧左对齐、左侧右对齐，避免压住图形")
    void labelAnchors() {
        List<RadarLabel> labels = AbilityRadar.of(List.of(
                new RadarAxis("Web", 1, 0), new RadarAxis("Pwn", 0, 0),
                new RadarAxis("Reverse", 0, 0), new RadarAxis("Crypto", 0, 0),
                new RadarAxis("Misc", 0, 0), new RadarAxis("应急响应", 0, 0))).labels();

        assertThat(labels.get(0).anchor()).isEqualTo("middle");
        assertThat(labels.get(1).anchor()).isEqualTo("start");
        assertThat(labels.get(5).anchor()).isEqualTo("end");
    }
}
