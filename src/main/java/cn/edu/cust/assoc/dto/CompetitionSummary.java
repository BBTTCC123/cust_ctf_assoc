package cn.edu.cust.assoc.dto;

import java.time.LocalDate;

/** 竞赛档案列表行。 */
public record CompetitionSummary(Long id, String name, String organizer, String levelLabel,
                                 String categoryLabel, LocalDate startDate, LocalDate endDate,
                                 Short seasonYear) {
}
