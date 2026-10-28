package cn.edu.cust.assoc.dto;

import java.util.List;

/** 文章编辑表单视图。 */
public record ArticleEditView(Long id, String title, String summary, String contentMd,
                              boolean involvesVuln, String visibility, String state,
                              String rejectReason, Long coverMediaId, List<String> tags) {
}
