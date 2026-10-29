package cn.edu.cust.assoc.repository;

import cn.edu.cust.assoc.domain.UserEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByUsername(String username);

    boolean existsByUsername(String username);

    Optional<UserEntity> findByNickname(String nickname);

    @Query("""
            select count(distinct u.id)
            from UserEntity u join u.roles r
            where r.code = 'SUPER_ADMIN' and u.status = 'ACTIVE'
            """)
    long countActiveSuperAdmins();

    /** 兑换审核锁用户行：同一成员的多笔兑换串行结算，余额不会被并发突破。 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from UserEntity u where u.id = :id")
    Optional<UserEntity> findByIdForUpdate(@Param("id") Long id);
}
