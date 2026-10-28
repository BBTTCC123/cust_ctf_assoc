package cn.edu.cust.assoc.repository;

import cn.edu.cust.assoc.domain.FavoriteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FavoriteRepository extends JpaRepository<FavoriteEntity, Long> {

    boolean existsByUserIdAndArticleId(Long userId, Long articleId);

    void deleteByUserIdAndArticleId(Long userId, Long articleId);

    List<FavoriteEntity> findByUserIdOrderByCreatedAtDesc(Long userId);
}
