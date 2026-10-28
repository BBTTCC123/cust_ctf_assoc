package cn.edu.cust.assoc.repository;

import cn.edu.cust.assoc.domain.CommentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<CommentEntity, Long> {

    List<CommentEntity> findByArticleIdAndStateOrderByCreatedAtAsc(Long articleId, String state);

    List<CommentEntity> findByStateOrderByCreatedAtAsc(String state);

    List<CommentEntity> findByArticleIdOrderByCreatedAtDesc(Long articleId);
}
