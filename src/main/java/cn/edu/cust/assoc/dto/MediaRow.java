package cn.edu.cust.assoc.dto;

/** 后台媒体列表行。 */
public record MediaRow(Long id, String originalName, Long bytes, String ownerName, String zone,
                       String visibility, String reviewState, String mime) {
}
