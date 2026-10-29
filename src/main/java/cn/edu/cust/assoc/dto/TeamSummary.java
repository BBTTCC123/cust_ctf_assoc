package cn.edu.cust.assoc.dto;

/** 战队列表项。 */
public record TeamSummary(Long id, String name, String intro, String leaderName,
                          Long memberCount, String status) {
}
