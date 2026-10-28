package cn.edu.cust.assoc.repository;

import cn.edu.cust.assoc.domain.AwardShareEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AwardShareRepository extends JpaRepository<AwardShareEntity, Long> {

    List<AwardShareEntity> findByTeamAwardId(Long teamAwardId);
}
