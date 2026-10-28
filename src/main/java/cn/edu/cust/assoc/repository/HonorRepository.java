package cn.edu.cust.assoc.repository;

import cn.edu.cust.assoc.domain.HonorEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HonorRepository extends JpaRepository<HonorEntity, Long> {
    List<HonorEntity> findAllByOrderByYearDescSortOrderAsc();
}
