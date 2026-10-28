package cn.edu.cust.assoc.repository;

import cn.edu.cust.assoc.domain.MediaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MediaRepository extends JpaRepository<MediaEntity, Long> {

    List<MediaEntity> findByReviewStateOrderByCreatedAtAsc(String reviewState);

    List<MediaEntity> findByOwnerUserIdOrderByCreatedAtDesc(Long ownerUserId);

    List<MediaEntity> findByReviewStateAndVisibilityOrderByCreatedAtDesc(String reviewState, String visibility);

    List<MediaEntity> findAllByOrderByCreatedAtDesc();

    /** 审核操作先锁行：重复点击时只有一个事务能通过状态检查。 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from MediaEntity m where m.id = :id")
    Optional<MediaEntity> findByIdForUpdate(@Param("id") Long id);
}
