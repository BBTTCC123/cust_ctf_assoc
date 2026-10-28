package cn.edu.cust.assoc.dto;

import java.time.LocalDateTime;

/** 战队成员行；leader 用于在页面上标出队长。 */
public record TeamMemberRow(Long userId, String nickname, LocalDateTime joinedAt, boolean leader) {
}
