package cn.edu.cust.assoc.repository;

import cn.edu.cust.assoc.domain.MemberProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberProfileRepository extends JpaRepository<MemberProfileEntity, Long> {

    Optional<MemberProfileEntity> findByUserId(Long userId);
}
