package cn.edu.cust.assoc.repository;

import cn.edu.cust.assoc.domain.PointsRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PointsRuleRepository extends JpaRepository<PointsRuleEntity, Long> {

    /** 某天生效的最新已发布规则。历史流水不受新规则影响靠的就是这个查询。 */
    Optional<PointsRuleEntity> findTopByDirectionAndItemKeyAndPublishedAtIsNotNullAndEffectiveFromLessThanEqualOrderByVersionDesc(
            String direction, String itemKey, LocalDate date);

    List<PointsRuleEntity> findAllByOrderByDirectionAscItemKeyAscVersionDesc();

    Optional<PointsRuleEntity> findTopByDirectionAndItemKeyOrderByVersionDesc(String direction, String itemKey);
}
