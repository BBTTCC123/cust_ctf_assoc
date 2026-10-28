package cn.edu.cust.assoc.domain;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "points_ledger")
public class PointsLedgerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "request_id", nullable = false)
    private Long requestId;

    @Column(name = "entry_type", nullable = false, length = 16)
    private String entryType;

    @Column(nullable = false)
    private Integer points;

    @Column(name = "vulnerability_id")
    private Long vulnerabilityId;

    // CHAR(7) 必须写 columnDefinition，否则 Hibernate 用 VARCHAR 去比，启动就失败（已踩第三次）
    @Column(name = "period_month", nullable = false, length = 7, columnDefinition = "char(7)")
    private String periodMonth;

    @Column(name = "occurred_on", nullable = false)
    private LocalDate occurredOn;

    @Column(name = "idempotency_key", nullable = false, length = 80)
    private String idempotencyKey;

    @Column(name = "reverses_id")
    private Long reversesId;

    @Column(length = 500)
    private String note;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getRequestId() { return requestId; }
    public void setRequestId(Long requestId) { this.requestId = requestId; }
    public String getEntryType() { return entryType; }
    public void setEntryType(String entryType) { this.entryType = entryType; }
    public Integer getPoints() { return points; }
    public void setPoints(Integer points) { this.points = points; }
    public Long getVulnerabilityId() { return vulnerabilityId; }
    public void setVulnerabilityId(Long vulnerabilityId) { this.vulnerabilityId = vulnerabilityId; }
    public String getPeriodMonth() { return periodMonth; }
    public void setPeriodMonth(String periodMonth) { this.periodMonth = periodMonth; }
    public LocalDate getOccurredOn() { return occurredOn; }
    public void setOccurredOn(LocalDate occurredOn) { this.occurredOn = occurredOn; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public Long getReversesId() { return reversesId; }
    public void setReversesId(Long reversesId) { this.reversesId = reversesId; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}