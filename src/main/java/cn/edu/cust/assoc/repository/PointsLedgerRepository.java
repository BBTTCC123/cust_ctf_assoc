package cn.edu.cust.assoc.repository;

import cn.edu.cust.assoc.domain.PointsLedgerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PointsLedgerRepository extends JpaRepository<PointsLedgerEntity, Long> {

    boolean existsByIdempotencyKey(String key);

    Optional<PointsLedgerEntity> findByIdempotencyKey(String key);

    List<PointsLedgerEntity> findByUserIdOrderByOccurredOnDesc(Long userId);

    @Query("select coalesce(sum(l.points), 0) from PointsLedgerEntity l where l.vulnerabilityId = :vulnId")
    int sumByVulnerability(@Param("vulnId") Long vulnId);

    @Query("select l.userId, sum(l.points) from PointsLedgerEntity l group by l.userId order by sum(l.points) desc")
    List<Object[]> rankAll();

    @Query("select l.userId, sum(l.points) from PointsLedgerEntity l where l.periodMonth in :months group by l.userId order by sum(l.points) desc")
    List<Object[]> rankByMonths(@Param("months") Collection<String> months);

    @Query("select l.userId, sum(l.points) from PointsLedgerEntity l where l.periodMonth like concat(:prefix, '%') group by l.userId order by sum(l.points) desc")
    List<Object[]> rankByPrefix(@Param("prefix") String prefix);
}
