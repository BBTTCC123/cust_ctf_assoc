package cn.edu.cust.assoc.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "award_share")
public class AwardShareEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "team_award_id", nullable = false)
    private Long teamAwardId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Integer points;

    public Long getId() { return id; }
    public Long getTeamAwardId() { return teamAwardId; }
    public void setTeamAwardId(Long teamAwardId) { this.teamAwardId = teamAwardId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Integer getPoints() { return points; }
    public void setPoints(Integer points) { this.points = points; }
}
