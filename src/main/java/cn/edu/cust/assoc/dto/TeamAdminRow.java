package cn.edu.cust.assoc.dto;

import java.time.LocalDateTime;

/** 后台战队管理行。 */
public record TeamAdminRow(Long id, String name, String leaderName, long memberCount,
                           String status, LocalDateTime createdAt) {
}
