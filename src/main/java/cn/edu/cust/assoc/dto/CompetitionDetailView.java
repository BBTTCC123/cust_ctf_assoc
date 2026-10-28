package cn.edu.cust.assoc.dto;

import java.time.LocalDate;
import java.util.List;

/** 竞赛档案详情。公开页只用昵称、名次和分数，佐证链接不对外。 */
public record CompetitionDetailView(Long id, String name, String organizer, String levelLabel,
                                    String categoryLabel, LocalDate startDate, LocalDate endDate,
                                    Short seasonYear, List<AwardView> individual,
                                    List<TeamAwardView> teams, List<Long> photoIds) {
}
