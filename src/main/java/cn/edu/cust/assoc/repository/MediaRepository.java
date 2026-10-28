package cn.edu.cust.assoc.repository;

import cn.edu.cust.assoc.domain.MediaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MediaRepository extends JpaRepository<MediaEntity, Long> {

    List<MediaEntity> findByReviewStateOrderByCreatedAtAsc(String reviewState);

    List<MediaEntity> findByOwnerUserIdOrderByCreatedAtDesc(Long ownerUserId);
}
