package cn.edu.cust.assoc.dto;

/** 公开资料（按隐私开关过滤，未开放的字段为 null）。 */
public record ProfileView(String realName, String studentNo, String college, String major,
                          String grade, String phone, String email, String blog) {
}
