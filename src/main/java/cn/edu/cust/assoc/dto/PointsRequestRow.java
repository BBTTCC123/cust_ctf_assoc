package cn.edu.cust.assoc.dto;

import java.time.LocalDate;

/** 后台积分申请行。 */
public record PointsRequestRow(Long id, String userName, String target, String level, String scene,
                               Integer points, LocalDate occurredOn, String proofUrl, boolean proofSafe,
                               String submittedBy, String sourceType, String state, long duplicateTargets) {
}
