package cn.edu.cust.assoc.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 积分规则行。 */
public record PointsRuleRow(Long id, String direction, String itemKey, Integer version,
                            Integer points, LocalDate effectiveFrom, LocalDateTime publishedAt) {
}
