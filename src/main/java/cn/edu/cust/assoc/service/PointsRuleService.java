package cn.edu.cust.assoc.service;

import cn.edu.cust.assoc.common.BizException;
import cn.edu.cust.assoc.domain.CompetitionPoints;
import cn.edu.cust.assoc.domain.PointCalc;
import cn.edu.cust.assoc.domain.PointsRuleEntity;
import cn.edu.cust.assoc.repository.PointsRuleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 积分规则版本。
 *
 * 计分时按 occurredOn 选择「已发布且生效日不晚于发生日」的最高版本；
 * 已经入账的流水不会被新规则重算 —— 历史流水只增不改。
 */
@Service
public class PointsRuleService {

    public static final String DIR_COMPETITION = "COMPETITION";
    public static final String DIR_BOUNTY = "BOUNTY";
    public static final String DIR_HW = "HW";

    private final PointsRuleRepository repository;
    private final AuditService audit;

    public PointsRuleService(PointsRuleRepository repository, AuditService audit) {
        this.repository = repository;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<PointsRuleEntity> all() {
        return repository.findAllByOrderByDirectionAscItemKeyAscVersionDesc();
    }

    /** 发布新版本：版本号自动递增，发布即生效（生效日可以设在未来）。 */
    @Transactional
    public PointsRuleEntity publish(String direction, String itemKey, int points, LocalDate effectiveFrom) {
        if (!DIR_COMPETITION.equals(direction) && !DIR_BOUNTY.equals(direction) && !DIR_HW.equals(direction)) {
            throw new BizException("未知的积分方向：" + direction);
        }
        if (itemKey == null || itemKey.isBlank()) {
            throw new BizException("规则项不能为空");
        }
        if (points < 0) {
            throw new BizException("分值不能为负");
        }
        if (effectiveFrom == null) {
            throw new BizException("生效日期不能为空");
        }
        int version = repository.findTopByDirectionAndItemKeyOrderByVersionDesc(direction, itemKey.trim())
                .map(r -> r.getVersion() + 1)
                .orElse(1);
        PointsRuleEntity rule = new PointsRuleEntity();
        rule.setDirection(direction);
        rule.setItemKey(itemKey.trim());
        rule.setVersion(version);
        rule.setPoints(points);
        rule.setEffectiveFrom(effectiveFrom);
        rule.setPublishedAt(LocalDateTime.now());
        PointsRuleEntity saved = repository.save(rule);
        audit.record("POINTS_RULE_PUBLISH", "POINTS_RULE", saved.getId(), null,
                direction + "/" + saved.getItemKey() + " v" + version + " = " + points + "，生效 " + effectiveFrom);
        return saved;
    }

    /** 挖洞基础分：先查已发布规则，找不到再用代码里的默认表。 */
    @Transactional(readOnly = true)
    public int bountyBase(String level, LocalDate occurredOn) {
        int fallback = PointCalc.base(level);
        return resolve(DIR_BOUNTY, level, occurredOn).orElse(fallback);
    }

    /** 竞赛奖分值：itemKey = 级别_名次，如 PROVINCE_FIRST。 */
    @Transactional(readOnly = true)
    public int competitionPoints(String level, String rank, LocalDate occurredOn) {
        int fallback = CompetitionPoints.of(level, rank);
        return resolve(DIR_COMPETITION, level + "_" + rank, occurredOn).orElse(fallback);
    }

    /** 护网折算：分段参数同样从规则表取。 */
    @Transactional(readOnly = true)
    public int hwPoints(int score, LocalDate occurredOn) {
        int base1000 = resolve(DIR_HW, "BASE_1000", occurredOn).orElse(PointCalc.HW_BASE_1000);
        int base5000 = resolve(DIR_HW, "HW_5000", occurredOn).orElse(PointCalc.HW_BASE_5000);
        int base10000 = resolve(DIR_HW, "HW_10000", occurredOn).orElse(PointCalc.HW_BASE_10000);
        int step = resolve(DIR_HW, "HW_STEP_5000", occurredOn).orElse(PointCalc.HW_STEP);
        return PointCalc.hw(score, base1000, base5000, base10000, step);
    }

    private Optional<Integer> resolve(String direction, String itemKey, LocalDate occurredOn) {
        if (occurredOn == null) {
            return Optional.empty();
        }
        return repository
                .findTopByDirectionAndItemKeyAndPublishedAtIsNotNullAndEffectiveFromLessThanEqualOrderByVersionDesc(
                        direction, itemKey, occurredOn)
                .map(PointsRuleEntity::getPoints);
    }
}
