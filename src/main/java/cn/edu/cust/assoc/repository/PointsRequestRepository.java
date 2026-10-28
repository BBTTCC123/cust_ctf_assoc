package cn.edu.cust.assoc.repository;

import cn.edu.cust.assoc.domain.PointsRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PointsRequestRepository extends JpaRepository<PointsRequestEntity, Long> {

    List<PointsRequestEntity> findByStateOrderByCreatedAtAsc(String state);

    List<PointsRequestEntity> findAllByOrderByCreatedAtDesc();

    List<PointsRequestEntity> findByUserIdOrderByCreatedAtDesc(Long userId);
}
