package cn.edu.cust.assoc.dto;

import java.time.LocalDate;

/** 我的积分流水行（本人可见备注）。 */
public record OwnLedgerRow(Long id, String periodMonth, LocalDate occurredOn,
                           String entryType, int points, String note) {
}
