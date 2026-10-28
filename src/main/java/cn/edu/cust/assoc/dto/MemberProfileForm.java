package cn.edu.cust.assoc.dto;

/** 成员资料编辑表单。 */
public class MemberProfileForm {

    private String realName;
    private String studentNo;
    private String college;
    private String major;
    private String grade;
    private String phone;
    private String email;
    private String blog;
    private boolean showRealName;
    private boolean showStudentNo;
    private boolean showPhone;
    private boolean showEmail;
    private boolean showBlog;

    public String getRealName() { return realName; }
    public void setRealName(String realName) { this.realName = realName; }
    public String getStudentNo() { return studentNo; }
    public void setStudentNo(String studentNo) { this.studentNo = studentNo; }
    public String getCollege() { return college; }
    public void setCollege(String college) { this.college = college; }
    public String getMajor() { return major; }
    public void setMajor(String major) { this.major = major; }
    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getBlog() { return blog; }
    public void setBlog(String blog) { this.blog = blog; }
    public boolean isShowRealName() { return showRealName; }
    public void setShowRealName(boolean showRealName) { this.showRealName = showRealName; }
    public boolean isShowStudentNo() { return showStudentNo; }
    public void setShowStudentNo(boolean showStudentNo) { this.showStudentNo = showStudentNo; }
    public boolean isShowPhone() { return showPhone; }
    public void setShowPhone(boolean showPhone) { this.showPhone = showPhone; }
    public boolean isShowEmail() { return showEmail; }
    public void setShowEmail(boolean showEmail) { this.showEmail = showEmail; }
    public boolean isShowBlog() { return showBlog; }
    public void setShowBlog(boolean showBlog) { this.showBlog = showBlog; }
}
