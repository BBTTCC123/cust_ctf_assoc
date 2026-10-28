package cn.edu.cust.assoc.repository;

import cn.edu.cust.assoc.domain.TeamAwardEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TeamAwardRepository extends JpaRepository<TeamAwardEntity, Long> {

    List<TeamAwardEntity> findByCompetitionIdOrderByIdDesc(Long competitionId);
}
