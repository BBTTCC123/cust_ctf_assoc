package cn.edu.cust.assoc.repository;

import cn.edu.cust.assoc.domain.RecruitApplicationEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RecruitApplicationRepository extends JpaRepository<RecruitApplicationEntity, Long> {

    List<RecruitApplicationEntity> findAllByOrderByCreatedAtDesc();

    boolean existsByStudentNoAndSeasonYear(String studentNo, Short seasonYear);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from RecruitApplicationEntity r where r.id = :id")
    Optional<RecruitApplicationEntity> findByIdForUpdate(@Param("id") Long id);
}
