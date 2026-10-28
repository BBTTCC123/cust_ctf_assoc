package cn.edu.cust.assoc.repository;

import cn.edu.cust.assoc.domain.PointsAppealEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PointsAppealRepository extends JpaRepository<PointsAppealEntity, Long> {

    List<PointsAppealEntity> findByStateOrderByCreatedAtAsc(String state);

    List<PointsAppealEntity> findAllByOrderByCreatedAtDesc();

    boolean existsByLedgerIdAndState(Long ledgerId, String state);

    /**
     * 原子状态流转：只有 OPEN 才能被处理。
     * 返回 0 说明已经被别人处理过（或状态不对），调用方必须报错回滚。
     */
    @Modifying
    @Query("""
            update PointsAppealEntity a
            set a.state = :state, a.handledBy = :handledBy, a.handledAt = :handledAt
            where a.id = :id and a.state = 'OPEN'
            """)
    int handleOpen(@Param("id") Long id,
                   @Param("state") String state,
                   @Param("handledBy") Long handledBy,
                   @Param("handledAt") LocalDateTime handledAt);
}
