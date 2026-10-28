package cn.edu.cust.assoc.repository;

import cn.edu.cust.assoc.domain.RedeemRequestEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RedeemRequestRepository extends JpaRepository<RedeemRequestEntity, Long> {

    List<RedeemRequestEntity> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<RedeemRequestEntity> findAllByOrderByCreatedAtDesc();

    boolean existsByUserIdAndItemIdAndState(Long userId, Long itemId, String state);

    /** 审核/取消先锁行：重复点击只有一个事务能通过状态检查。 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from RedeemRequestEntity r where r.id = :id")
    Optional<RedeemRequestEntity> findByIdForUpdate(@Param("id") Long id);

    @Query("select coalesce(sum(r.costPoints), 0) from RedeemRequestEntity r " +
            "where r.userId = :userId and r.state = 'APPROVED'")
    long sumApprovedCostByUser(@Param("userId") Long userId);
}
