package cn.edu.cust.assoc.dto;

import java.time.LocalDateTime;
import java.util.List;

/** 战队详情：成员、队长与状态。 */
public record TeamDetail(Long id, String name, String intro, Long leaderId, String leaderName,
                         String status, List<TeamMemberRow> members, LocalDateTime createdAt) {
}
