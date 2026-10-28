package cn.edu.cust.assoc.dto;

/** 敏感词管理行。 */
public record SensitiveWordRow(Long id, String word, String level, boolean enabled) {
}
