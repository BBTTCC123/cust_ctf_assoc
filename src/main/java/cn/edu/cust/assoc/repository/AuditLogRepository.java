package cn.edu.cust.assoc.repository;

import cn.edu.cust.assoc.domain.AuditLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface AuditLogRepository extends JpaRepository<AuditLogEntity, Long> {

    @Query("""
            select a from AuditLogEntity a
            where (:operator is null or a.operatorName like concat('%', :operator, '%'))
              and (:action is null or a.action = :action)
              and (:targetType is null or a.targetType = :targetType)
              and (:from is null or a.createdAt >= :from)
              and (:to is null or a.createdAt < :to)
            order by a.createdAt desc, a.id desc
            """)
    Page<AuditLogEntity> search(@Param("operator") String operator,
                                @Param("action") String action,
                                @Param("targetType") String targetType,
                                @Param("from") LocalDateTime from,
                                @Param("to") LocalDateTime to,
                                Pageable pageable);
}
