package cn.edu.cust.assoc.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "team")
public class TeamEntity {

    public static final String ACTIVE = "ACTIVE";
    public static final String DISBANDED = "DISBANDED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "leader_id", nullable = false)
    private Long leaderId;

    @Column(columnDefinition = "TEXT")
    private String intro;

    @Column(nullable = false, length = 16)
    private String status;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public boolean isActive() {
        return ACTIVE.equals(status);
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Long getLeaderId() { return leaderId; }
    public void setLeaderId(Long leaderId) { this.leaderId = leaderId; }
    public String getIntro() { return intro; }
    public void setIntro(String intro) { this.intro = intro; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
