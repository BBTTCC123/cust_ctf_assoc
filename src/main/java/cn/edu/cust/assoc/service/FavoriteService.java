package cn.edu.cust.assoc.service;

import cn.edu.cust.assoc.domain.ArticleEntity;
import cn.edu.cust.assoc.domain.FavoriteEntity;
import cn.edu.cust.assoc.repository.ArticleRepository;
import cn.edu.cust.assoc.repository.FavoriteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/** 文章收藏。 */
@Service
public class FavoriteService {

    private final FavoriteRepository repository;
    private final ArticleRepository articleRepository;

    public FavoriteService(FavoriteRepository repository, ArticleRepository articleRepository) {
        this.repository = repository;
        this.articleRepository = articleRepository;
    }

    /** 返回操作后是否处于收藏状态。 */
    @Transactional
    public boolean toggle(Long userId, Long articleId) {
        if (repository.existsByUserIdAndArticleId(userId, articleId)) {
            repository.deleteByUserIdAndArticleId(userId, articleId);
            return false;
        }
        FavoriteEntity favorite = new FavoriteEntity();
        favorite.setUserId(userId);
        favorite.setArticleId(articleId);
        repository.save(favorite);
        return true;
    }

    @Transactional(readOnly = true)
    public boolean isFavorited(Long userId, Long articleId) {
        return repository.existsByUserIdAndArticleId(userId, articleId);
    }

    @Transactional(readOnly = true)
    public List<Long> articleIdsOf(Long userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(FavoriteEntity::getArticleId)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ArticleEntity> articlesOf(Long userId) {
        List<Long> ids = articleIdsOf(userId);
        if (ids.isEmpty()) {
            return List.of();
        }
        List<ArticleEntity> result = new ArrayList<>();
        articleRepository.findAllById(ids).forEach(article -> {
            if ("PUBLISHED".equals(article.getState())) {
                result.add(article);
            }
        });
        result.sort((a, b) -> Integer.compare(ids.indexOf(a.getId()), ids.indexOf(b.getId())));
        return result;
    }
}
