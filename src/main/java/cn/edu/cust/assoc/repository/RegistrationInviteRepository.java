package cn.edu.cust.assoc.repository;

import cn.edu.cust.assoc.domain.RegistrationInviteEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RegistrationInviteRepository extends JpaRepository<RegistrationInviteEntity, Long> {

    Optional<RegistrationInviteEntity> findByCode(String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from RegistrationInviteEntity i where i.code = :code")
    Optional<RegistrationInviteEntity> findByCodeForUpdate(@Param("code") String code);

    List<RegistrationInviteEntity> findAllByOrderByExpiresAtDesc();

    boolean existsByCode(String code);
}
