package cn.edu.cust.assoc.dto;

import java.time.LocalDate;

/** 成员积分流水行（公开部分）。 */
public record MemberLedgerRow(String periodMonth, LocalDate occurredOn, String sourceType,
                              String entryType, int points) {
}
