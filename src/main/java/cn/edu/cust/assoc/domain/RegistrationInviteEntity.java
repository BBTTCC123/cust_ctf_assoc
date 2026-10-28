package cn.edu.cust.assoc.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/** 注册邀请码。表在 V1 就已建好，V8 起接入业务。 */
@Entity
@Table(name = "registration_invite")
public class RegistrationInviteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String code;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "used_by")
    private Long usedBy;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "max_uses", nullable = false)
    private Integer maxUses;

    @Column(name = "used_count", nullable = false)
    private Integer usedCount;

    @Column(nullable = false, length = 16)
    private String status;

    @Column(length = 200)
    private String note;

    public Long getId() { return id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public Long getUsedBy() { return usedBy; }
    public void setUsedBy(Long usedBy) { this.usedBy = usedBy; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public Integer getMaxUses() { return maxUses; }
    public void setMaxUses(Integer maxUses) { this.maxUses = maxUses; }
    public Integer getUsedCount() { return usedCount; }
    public void setUsedCount(Integer usedCount) { this.usedCount = usedCount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
