package cn.edu.cust.assoc.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TeamShareRulesTest {

    private Map<Long, Integer> shares(int... pairs) {
        Map<Long, Integer> map = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put((long) pairs[i], pairs[i + 1]);
        }
        return map;
    }

    @Test
    @DisplayName("合计等于总分时通过")
    void sumEqualsBase() {
        TeamShareRules.validate(15, shares(1, 5, 2, 5, 3, 5));
    }

    @Test
    @DisplayName("合计少于总分时拒绝")
    void sumTooSmall() {
        assertThatThrownBy(() -> TeamShareRules.validate(15, shares(1, 5, 2, 5)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("必须等于奖项总分");
    }

    @Test
    @DisplayName("合计超过总分时拒绝")
    void sumTooLarge() {
        assertThatThrownBy(() -> TeamShareRules.validate(15, shares(1, 10, 2, 10)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("单个成员超额时拒绝，哪怕合计刚好")
    void singleOverBase() {
        assertThatThrownBy(() -> TeamShareRules.validate(10, shares(1, 11, 2, -1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("空名单拒绝")
    void empty() {
        assertThatThrownBy(() -> TeamShareRules.validate(10, Map.of()))
                .hasMessageContaining("至少要分给一个成员");
    }

    @Test
    @DisplayName("重复成员显式拒绝")
    void duplicateMember() {
        assertThatThrownBy(() -> TeamShareRules.validateNoDuplicate(java.util.List.of(1L, 2L, 1L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("同一个成员不能出现两次");
        org.assertj.core.api.Assertions.assertThatCode(
                        () -> TeamShareRules.validateNoDuplicate(java.util.List.of(1L, 2L, 3L)))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("竞赛分值表：省级/国赛/国际/校级")
    void competitionPointsTable() {
        assertThat(CompetitionPoints.of("PROVINCE", "FIRST")).isEqualTo(10);
        assertThat(CompetitionPoints.of("NATIONAL", "SECOND")).isEqualTo(13);
        assertThat(CompetitionPoints.of("INTERNATIONAL", "FIRST")).isEqualTo(20);
        assertThat(CompetitionPoints.of("CAMPUS", "FIRST")).isZero();
    }
}
