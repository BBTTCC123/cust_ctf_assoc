package cn.edu.cust.assoc.repository;

import cn.edu.cust.assoc.domain.RewardItemEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RewardItemRepository extends JpaRepository<RewardItemEntity, Long> {

    List<RewardItemEntity> findAllByOrderBySortOrderAscIdAsc();

    List<RewardItemEntity> findByEnabledTrueOrderBySortOrderAscIdAsc();

    Optional<RewardItemEntity> findByName(String name);

    /** 审核时锁资源行，库存扣减与状态流转在同一个事务里。 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from RewardItemEntity r where r.id = :id")
    Optional<RewardItemEntity> findByIdForUpdate(@Param("id") Long id);

    /** 原子扣库存：库存为 0 时更新 0 行，并发下不会扣成负数。 */
    @Modifying
    @Query("update RewardItemEntity r set r.stock = r.stock - 1 where r.id = :id and r.stock > 0")
    int takeOneStock(@Param("id") Long id);
}
