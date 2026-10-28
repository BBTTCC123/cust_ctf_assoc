package cn.edu.cust.assoc.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PointCalcTest {

    @Test
    @DisplayName("基础分：严重 8 / 高危 6 / 中危 4 / 低危 2")
    void base() {
        assertThat(PointCalc.base("CRITICAL")).isEqualTo(8);
        assertThat(PointCalc.base("HIGH")).isEqualTo(6);
        assertThat(PointCalc.base("MEDIUM")).isEqualTo(4);
        assertThat(PointCalc.base("LOW")).isEqualTo(2);
    }

    @Test
    @DisplayName("通杀和公益都减半，且不叠乘")
    void scene() {
        assertThat(PointCalc.unitPoints("CRITICAL", "SINGLE")).isEqualTo(8);
        assertThat(PointCalc.unitPoints("CRITICAL", "SWEEP")).isEqualTo(4);
        assertThat(PointCalc.unitPoints("CRITICAL", "CHARITY")).isEqualTo(4);
        assertThat(PointCalc.unitPoints("HIGH", "SWEEP")).isEqualTo(3);
        assertThat(PointCalc.unitPoints("LOW", "CHARITY")).isEqualTo(1);
    }

    @Test
    @DisplayName("同一漏洞点累计封顶 10 分")
    void cap() {
        assertThat(PointCalc.clampToCap(0, 8)).isEqualTo(8);
        assertThat(PointCalc.clampToCap(8, 4)).isEqualTo(2);   // 8 + 4 超了，只补 2
        assertThat(PointCalc.clampToCap(10, 4)).isZero();
        assertThat(PointCalc.clampToCap(9, 4)).isEqualTo(1);
    }

    @Test
    @DisplayName("护网分段：999 不算分，1000 起 3 分，封顶 20")
    void hw() {
        assertThat(PointCalc.hw(999)).isZero();
        assertThat(PointCalc.hw(1000)).isEqualTo(3);
        assertThat(PointCalc.hw(4999)).isEqualTo(3);
        assertThat(PointCalc.hw(7000)).isEqualTo(5);
        assertThat(PointCalc.hw(10000)).isEqualTo(10);
        assertThat(PointCalc.hw(15000)).isEqualTo(12);
        assertThat(PointCalc.hw(200000)).isEqualTo(20);
    }

    @Test
    @DisplayName("未知等级要报错，不能悄悄当 0 分")
    void unknownLevel() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> PointCalc.base("SUPER"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
