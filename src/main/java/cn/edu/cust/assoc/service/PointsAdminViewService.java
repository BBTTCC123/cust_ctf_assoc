package cn.edu.cust.assoc.service;

import cn.edu.cust.assoc.common.UrlPolicy;
import cn.edu.cust.assoc.domain.PointCalc;
import cn.edu.cust.assoc.domain.PointsAppealEntity;
import cn.edu.cust.assoc.domain.PointsRequestEntity;
import cn.edu.cust.assoc.dto.AppealRow;
import cn.edu.cust.assoc.dto.PointsRequestRow;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 后台积分申请/申诉视图。分数、等级、场景都翻成中文标签。 */
@Service
public class PointsAdminViewService {

    private final PointsService pointsService;
    private final UserDirectoryService userDirectory;

    public PointsAdminViewService(PointsService pointsService, UserDirectoryService userDirectory) {
        this.pointsService = pointsService;
        this.userDirectory = userDirectory;
    }

    @Transactional(readOnly = true)
    public List<PointsRequestRow> pendingRows() {
        return rows(pointsService.pending());
    }

    @Transactional(readOnly = true)
    public List<PointsRequestRow> allRows() {
        return rows(pointsService.all());
    }

    @Transactional(readOnly = true)
    public List<AppealRow> appeals() {
        List<PointsAppealEntity> appeals = pointsService.appeals();
        List<Long> userIds = new ArrayList<>();
        appeals.forEach(a -> {
            userIds.add(a.getUserId());
            if (a.getHandledBy() != null) {
                userIds.add(a.getHandledBy());
            }
        });
        Map<Long, String> names = userDirectory.nicknames(userIds);
        return appeals.stream()
                .map(a -> new AppealRow(a.getId(), a.getLedgerId(),
                        names.getOrDefault(a.getUserId(), "成员"), a.getReason(), a.getState(),
                        a.getHandledBy() == null ? null : names.getOrDefault(a.getHandledBy(), "管理员"),
                        a.getCreatedAt(), a.getHandledAt()))
                .toList();
    }

    private List<PointsRequestRow> rows(List<PointsRequestEntity> requests) {
        List<Long> userIds = new ArrayList<>();
        requests.forEach(r -> {
            userIds.add(r.getUserId());
            if (r.getSubmittedBy() != null) {
                userIds.add(r.getSubmittedBy());
            }
        });
        Map<Long, String> names = userDirectory.nicknames(userIds);

        // 同一目标还有多少待审申请：审核人据此判断是否重复提交
        Map<String, Long> pendingTargets = new HashMap<>();
        requests.stream()
                .filter(r -> "PENDING".equals(r.getState()) && r.getTarget() != null)
                .forEach(r -> pendingTargets.merge(r.getTarget(), 1L, Long::sum));

        return requests.stream()
                .map(r -> new PointsRequestRow(
                        r.getId(),
                        names.getOrDefault(r.getUserId(), "成员"),
                        r.getTarget(),
                        labelOfLevel(r.getLevel()),
                        labelOfScene(r.getScene()),
                        r.getPoints(),
                        r.getOccurredOn(),
                        r.getProofUrl(),
                        UrlPolicy.isSafeLink(r.getProofUrl()),
                        names.getOrDefault(r.getSubmittedBy(), "成员"),
                        r.getSourceType(),
                        r.getState(),
                        r.getTarget() == null ? 0 : pendingTargets.getOrDefault(r.getTarget(), 0L)))
                .toList();
    }

    private String labelOfLevel(String level) {
        if (level == null) return null;
        for (PointCalc.Level value : PointCalc.Level.values()) {
            if (value.name().equals(level)) return value.getLabel();
        }
        return level;
    }

    private String labelOfScene(String scene) {
        if (scene == null) return null;
        for (PointCalc.Scene value : PointCalc.Scene.values()) {
            if (value.name().equals(scene)) return value.getLabel();
        }
        return scene;
    }
}
