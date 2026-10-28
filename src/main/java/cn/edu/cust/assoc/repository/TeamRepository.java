package cn.edu.cust.assoc.repository;

import cn.edu.cust.assoc.domain.TeamEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TeamRepository extends JpaRepository<TeamEntity, Long> {

    List<TeamEntity> findByStatusOrderByIdAsc(String status);

    List<TeamEntity> findAllByOrderByIdDesc();

    Optional<TeamEntity> findByName(String name);

    long countByStatus(String status);
}
