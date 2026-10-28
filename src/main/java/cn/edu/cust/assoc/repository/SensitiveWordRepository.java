package cn.edu.cust.assoc.repository;

import cn.edu.cust.assoc.domain.SensitiveWordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SensitiveWordRepository extends JpaRepository<SensitiveWordEntity, Long> {

    List<SensitiveWordEntity> findByEnabledTrue();
}
