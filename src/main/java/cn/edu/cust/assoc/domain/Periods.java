package cn.edu.cust.assoc.domain;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * 归属月与关账规则。
 *
 * 每月 3 日关上一个月的账：1 日、2 日时上个月仍在开放期，补录的成绩记上月；
 * 3 日起才关闭上月，之后补录的记当期。跨年同样适用（1 月 1、2 日关的是去年 12 月的账）。
 * 这条规则会牺牲一点「分数落在正确月份」的纯度，换来「已公布的榜不会自己变」。
 */
public final class Periods {

    /** 每月几号关上一个月的账。 */
    public static final int CLOSE_DAY = 3;

    private Periods() {}

    public static String monthKey(LocalDate date) {
        return YearMonth.from(date).toString();
    }

    public static String currentMonthKey(LocalDate today) {
        return monthKey(today);
    }

    /** 当前仍开放计账的月份：3 日前是上个月，3 日起是本月。 */
    public static String openMonth(LocalDate today) {
        return today.getDayOfMonth() < CLOSE_DAY
                ? monthKey(today.minusMonths(1))
                : monthKey(today);
    }

    /** 某个月是否已经关账：早于当前开放月才视为已关。 */
    public static boolean isClosed(String monthKey, LocalDate today) {
        return monthKey.compareTo(openMonth(today)) < 0;
    }

    /** 入账时真正归属的月份。 */
    public static String periodMonth(LocalDate occurredOn, LocalDate today) {
        String occurred = monthKey(occurredOn);
        return isClosed(occurred, today) ? openMonth(today) : occurred;
    }

    /** 是否发生了「补录」（发生月已关，被记到当期）。 */
    public static boolean isBackfill(LocalDate occurredOn, LocalDate today) {
        return isClosed(monthKey(occurredOn), today);
    }

    /** 季度对应的三个月。 */
    public static List<String> quarterMonths(LocalDate today) {
        int q = (today.getMonthValue() - 1) / 3;
        List<String> months = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            months.add(YearMonth.of(today.getYear(), q * 3 + i).toString());
        }
        return months;
    }

    public static String yearPrefix(LocalDate today) {
        return today.getYear() + "-";
    }
}
