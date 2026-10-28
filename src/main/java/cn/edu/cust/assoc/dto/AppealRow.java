package cn.edu.cust.assoc.dto;

import java.time.LocalDateTime;

/** 后台申诉行。 */
public record AppealRow(Long id, Long ledgerId, String userName, String reason, String state,
                        String handleByName, LocalDateTime createdAt, LocalDateTime handledAt) {
}
