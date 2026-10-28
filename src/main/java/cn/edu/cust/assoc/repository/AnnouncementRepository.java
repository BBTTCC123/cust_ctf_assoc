package cn.edu.cust.assoc.repository;

import cn.edu.cust.assoc.domain.AnnouncementEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnnouncementRepository extends JpaRepository<AnnouncementEntity, Long> {
    List<AnnouncementEntity> findAllByOrderByPinnedDescPublishedAtDesc();
}
