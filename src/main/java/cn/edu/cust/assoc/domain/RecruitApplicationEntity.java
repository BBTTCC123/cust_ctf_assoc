package cn.edu.cust.assoc.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "recruit_application")
public class RecruitApplicationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 64)
    private String name;
    @Column(name = "student_no", nullable = false, length = 32)
    private String studentNo;
    @Column(nullable = false, length = 128)
    private String college;
    @Column(nullable = false, length = 128)
    private String major;
    @Column(nullable = false, length = 128)
    private String contact;
    @Column(length = 1000)
    private String message;
    @Column(name = "season_year", nullable = false)
    private Short seasonYear;
    @Column(nullable = false, length = 16)
    private String state;
    @Column(length = 500)
    private String note;
    @Column(name = "handled_by")
    private Long handledBy;
    @Column(name = "handled_at")
    private LocalDateTime handledAt;
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getStudentNo() { return studentNo; }
    public void setStudentNo(String studentNo) { this.studentNo = studentNo; }
    public String getCollege() { return college; }
    public void setCollege(String college) { this.college = college; }
    public String getMajor() { return major; }
    public void setMajor(String major) { this.major = major; }
    public String getContact() { return contact; }
    public void setContact(String contact) { this.contact = contact; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Short getSeasonYear() { return seasonYear; }
    public void setSeasonYear(Short seasonYear) { this.seasonYear = seasonYear; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public Long getHandledBy() { return handledBy; }
    public void setHandledBy(Long handledBy) { this.handledBy = handledBy; }
    public LocalDateTime getHandledAt() { return handledAt; }
    public void setHandledAt(LocalDateTime handledAt) { this.handledAt = handledAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
