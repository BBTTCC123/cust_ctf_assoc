package cn.edu.cust.assoc.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class PeriodsTest {

    private final LocalDate today = LocalDate.of(2026, 10, 28);

    @Test
    @DisplayName("当月发生的，就记当月")
    void sameMonth() {
        assertThat(Periods.periodMonth(LocalDate.of(2026, 10, 2), today)).isEqualTo("2026-10");
        assertThat(Periods.isBackfill(LocalDate.of(2026, 10, 2), today)).isFalse();
    }

    @Test
    @DisplayName("往月的，关账后记当期")
    void closedMonth() {
        assertThat(Periods.periodMonth(LocalDate.of(2026, 5, 10), today)).isEqualTo("2026-10");
        assertThat(Periods.isBackfill(LocalDate.of(2026, 5, 10), today)).isTrue();
    }

    @Test
    @DisplayName("季度月份是三个月")
    void quarter() {
        assertThat(Periods.quarterMonths(LocalDate.of(2026, 10, 1)))
                .containsExactly("2026-10", "2026-11", "2026-12");
        assertThat(Periods.quarterMonths(LocalDate.of(2026, 2, 1)))
                .containsExactly("2026-01", "2026-02", "2026-03");
    }
}
