package cn.edu.cust.assoc.dto;

/** 兑换资源列表项。 */
public record RewardItemRow(Long id, String name, String description, int costPoints,
                            int stock, boolean enabled, int sortOrder) {
}
