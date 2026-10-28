package cn.edu.cust.assoc.dto;

import java.time.LocalDateTime;

/** 审计日志行。 */
public record AuditRow(Long id, String operatorName, String action, String targetType,
                       String targetId, String detailBefore, String detailAfter,
                       String ip, String requestId, LocalDateTime createdAt) {
}
