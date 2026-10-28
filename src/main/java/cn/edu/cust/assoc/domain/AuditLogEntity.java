package cn.edu.cust.assoc.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/** 后台操作审计日志。只增不改，由 AuditService 统一写入。 */
@Entity
@Table(name = "audit_log")
public class AuditLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "operator_id")
    private Long operatorId;

    @Column(name = "operator_name", length = 64)
    private String operatorName;

    @Column(nullable = false, length = 64)
    private String action;

    @Column(name = "target_type", length = 32)
    private String targetType;

    @Column(name = "target_id", length = 64)
    private String targetId;

    @Column(name = "detail_before", length = 1000)
    private String detailBefore;

    @Column(name = "detail_after", length = 1000)
    private String detailAfter;

    @Column(length = 64)
    private String ip;

    @Column(name = "request_id", length = 64)
    private String requestId;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public Long getOperatorId() { return operatorId; }
    public void setOperatorId(Long operatorId) { this.operatorId = operatorId; }
    public String getOperatorName() { return operatorName; }
    public void setOperatorName(String operatorName) { this.operatorName = operatorName; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }
    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }
    public String getDetailBefore() { return detailBefore; }
    public void setDetailBefore(String detailBefore) { this.detailBefore = detailBefore; }
    public String getDetailAfter() { return detailAfter; }
    public void setDetailAfter(String detailAfter) { this.detailAfter = detailAfter; }
    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
