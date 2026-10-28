package cn.edu.cust.assoc.repository;

import cn.edu.cust.assoc.domain.AnnouncementEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AnnouncementRepository extends JpaRepository<AnnouncementEntity, Long> {

    List<AnnouncementEntity> findByDeletedAtIsNullOrderByPinnedDescPublishedAtDesc();

    List<AnnouncementEntity> findAllByOrderByPinnedDescPublishedAtDesc();

    Optional<AnnouncementEntity> findByIdAndDeletedAtIsNull(Long id);
}
