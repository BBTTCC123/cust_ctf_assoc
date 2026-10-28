package cn.edu.cust.assoc.repository;

import cn.edu.cust.assoc.domain.PointsRequestEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PointsRequestRepository extends JpaRepository<PointsRequestEntity, Long> {

    List<PointsRequestEntity> findByStateOrderByCreatedAtAsc(String state);

    List<PointsRequestEntity> findAllByOrderByCreatedAtDesc();

    List<PointsRequestEntity> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from PointsRequestEntity r where r.id = :id")
    Optional<PointsRequestEntity> findByIdForUpdate(@Param("id") Long id);
}
