package cn.edu.cust.assoc.repository;

import cn.edu.cust.assoc.domain.RecruitApplicationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecruitApplicationRepository extends JpaRepository<RecruitApplicationEntity, Long> {
    List<RecruitApplicationEntity> findAllByOrderByCreatedAtDesc();
    boolean existsByStudentNoAndSeasonYear(String studentNo, Short seasonYear);
}
