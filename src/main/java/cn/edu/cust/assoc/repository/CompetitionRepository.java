package cn.edu.cust.assoc.repository;

import cn.edu.cust.assoc.domain.CompetitionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CompetitionRepository extends JpaRepository<CompetitionEntity, Long> {

    List<CompetitionEntity> findAllByOrderByStartDateDesc();

    List<CompetitionEntity> findBySeasonYearOrderByStartDateDesc(Short seasonYear);

    boolean existsByNameAndSeasonYear(String name, Short seasonYear);
}
