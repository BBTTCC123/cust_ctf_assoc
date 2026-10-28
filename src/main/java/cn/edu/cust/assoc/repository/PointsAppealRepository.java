package cn.edu.cust.assoc.repository;

import cn.edu.cust.assoc.domain.PointsAppealEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PointsAppealRepository extends JpaRepository<PointsAppealEntity, Long> {

    List<PointsAppealEntity> findByStateOrderByCreatedAtAsc(String state);

    List<PointsAppealEntity> findAllByOrderByCreatedAtDesc();
}
