package cn.edu.cust.assoc.repository;

import cn.edu.cust.assoc.domain.AwardEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AwardRepository extends JpaRepository<AwardEntity, Long> {

    List<AwardEntity> findByCompetitionIdOrderByIdDesc(Long competitionId);

    List<AwardEntity> findByUserIdOrderByIdDesc(Long userId);
}
