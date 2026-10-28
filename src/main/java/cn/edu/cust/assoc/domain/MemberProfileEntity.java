package cn.edu.cust.assoc.domain;

import jakarta.persistence.*;

/** 成员实名资料与可见性开关。表在 V1 已建，这里接入业务。 */
@Entity
@Table(name = "member_profile")
public class MemberProfileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "real_name", nullable = false, length = 64)
    private String realName;

    @Column(name = "student_no", length = 32)
    private String studentNo;

    @Column(nullable = false, length = 128)
    private String college;

    @Column(nullable = false, length = 128)
    private String major;

    @Column(nullable = false, length = 32)
    private String grade;

    @Column(length = 32)
    private String phone;

    @Column(length = 254)
    private String email;

    @Column(length = 2048)
    private String blog;

    @Column(name = "show_real_name", nullable = false, columnDefinition = "TINYINT(1)")
    private Boolean showRealName;

    @Column(name = "show_student_no", nullable = false, columnDefinition = "TINYINT(1)")
    private Boolean showStudentNo;

    @Column(name = "show_phone", nullable = false, columnDefinition = "TINYINT(1)")
    private Boolean showPhone;

    @Column(name = "show_email", nullable = false, columnDefinition = "TINYINT(1)")
    private Boolean showEmail;

    @Column(name = "show_blog", nullable = false, columnDefinition = "TINYINT(1)")
    private Boolean showBlog;

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
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
    public Boolean getShowRealName() { return showRealName; }
    public void setShowRealName(Boolean showRealName) { this.showRealName = showRealName; }
    public Boolean getShowStudentNo() { return showStudentNo; }
    public void setShowStudentNo(Boolean showStudentNo) { this.showStudentNo = showStudentNo; }
    public Boolean getShowPhone() { return showPhone; }
    public void setShowPhone(Boolean showPhone) { this.showPhone = showPhone; }
    public Boolean getShowEmail() { return showEmail; }
    public void setShowEmail(Boolean showEmail) { this.showEmail = showEmail; }
    public Boolean getShowBlog() { return showBlog; }
    public void setShowBlog(Boolean showBlog) { this.showBlog = showBlog; }
}
