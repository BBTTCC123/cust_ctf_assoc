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
    @DisplayName("每月 1、2 日上个月仍开放，3 日才关账")
    void closeDayBoundary() {
        LocalDate nov1 = LocalDate.of(2026, 11, 1);
        LocalDate nov2 = LocalDate.of(2026, 11, 2);
        LocalDate nov3 = LocalDate.of(2026, 11, 3);

        assertThat(Periods.openMonth(nov1)).isEqualTo("2026-10");
        assertThat(Periods.openMonth(nov2)).isEqualTo("2026-10");
        assertThat(Periods.openMonth(nov3)).isEqualTo("2026-11");

        // 11 月 1、2 日补录 10 月 31 日的成绩，仍归 10 月
        assertThat(Periods.periodMonth(LocalDate.of(2026, 10, 31), nov1)).isEqualTo("2026-10");
        assertThat(Periods.periodMonth(LocalDate.of(2026, 10, 31), nov2)).isEqualTo("2026-10");
        assertThat(Periods.isClosed("2026-10", nov2)).isFalse();
        // 11 月 3 日起，10 月关账，补录记 11 月
        assertThat(Periods.periodMonth(LocalDate.of(2026, 10, 31), nov3)).isEqualTo("2026-11");
        assertThat(Periods.isClosed("2026-10", nov3)).isTrue();
    }

    @Test
    @DisplayName("跨年边界：1 月 1、2 日关的是去年 12 月的账")
    void yearBoundary() {
        LocalDate jan1 = LocalDate.of(2027, 1, 1);
        LocalDate jan2 = LocalDate.of(2027, 1, 2);
        LocalDate jan3 = LocalDate.of(2027, 1, 3);

        assertThat(Periods.openMonth(jan1)).isEqualTo("2026-12");
        assertThat(Periods.periodMonth(LocalDate.of(2026, 12, 31), jan2)).isEqualTo("2026-12");
        assertThat(Periods.periodMonth(LocalDate.of(2026, 12, 31), jan3)).isEqualTo("2027-01");
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
