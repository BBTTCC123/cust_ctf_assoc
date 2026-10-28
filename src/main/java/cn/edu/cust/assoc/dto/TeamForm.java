package cn.edu.cust.assoc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 创建/修改战队的表单。 */
public class TeamForm {

    @NotBlank(message = "战队名不能为空")
    @Size(min = 2, max = 50, message = "战队名长度需在 2 到 50 个字符之间")
    private String name;

    @Size(max = 500, message = "战队简介不能超过 500 个字符")
    private String intro;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getIntro() { return intro; }
    public void setIntro(String intro) { this.intro = intro; }
}
