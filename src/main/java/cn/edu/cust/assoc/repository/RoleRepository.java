package cn.edu.cust.assoc.repository;

import cn.edu.cust.assoc.domain.RoleEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<RoleEntity, Long> {

    Optional<RoleEntity> findByCode(String code);

    /** 串行化超管角色变更，避免两个并发请求同时移除最后一个超管。 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from RoleEntity r where r.code = :code")
    Optional<RoleEntity> findByCodeForUpdate(@Param("code") String code);
}
