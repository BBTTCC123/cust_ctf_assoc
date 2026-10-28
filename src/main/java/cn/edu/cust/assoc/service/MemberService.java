package cn.edu.cust.assoc.service;

import cn.edu.cust.assoc.dto.LeaderboardRow;
import cn.edu.cust.assoc.dto.MemberLedgerRow;
import cn.edu.cust.assoc.dto.MemberPointsView;
import cn.edu.cust.assoc.domain.PointsLedgerEntity;
import cn.edu.cust.assoc.domain.PointsRequestEntity;
import cn.edu.cust.assoc.domain.UserEntity;
import cn.edu.cust.assoc.repository.PointsRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 榜单与成员积分页的数据组装。对外只给昵称，不给登录名/邮箱/学号。 */
@Service
public class MemberService {

    private final PointsService pointsService;
    private final PointsRequestRepository requestRepository;
    private final UserDirectoryService userDirectory;

    public MemberService(PointsService pointsService, PointsRequestRepository requestRepository,
                         UserDirectoryService userDirectory) {
        this.pointsService = pointsService;
        this.requestRepository = requestRepository;
        this.userDirectory = userDirectory;
    }

    @Transactional(readOnly = true)
    public List<LeaderboardRow> ranking(String type, int limit) {
        List<Object[]> raw = pointsService.ranking(type, limit);
        List<Long> ids = raw.stream().map(row -> (Long) row[0]).toList();
        Map<Long, String> names = userDirectory.nicknames(ids);
        List<LeaderboardRow> rows = new ArrayList<>();
        int rank = 1;
        for (Object[] row : raw) {
            Long userId = (Long) row[0];
            long total = ((Number) row[1]).longValue();
            rows.add(new LeaderboardRow(rank++, userId, names.getOrDefault(userId, "成员"), total));
        }
        return rows;
    }

    @Transactional(readOnly = true)
    public List<LeaderboardRow> top(int n) {
        return ranking("ALL", n);
    }

    @Transactional(readOnly = true)
    public MemberPointsView view(Long userId) {
        UserEntity user = userDirectory.require(userId);
        List<PointsLedgerEntity> ledger = pointsService.ledgerOf(userId);

        Map<Long, String> sources = requestRepository
                .findAllById(ledger.stream().map(PointsLedgerEntity::getRequestId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(PointsRequestEntity::getId, PointsRequestEntity::getSourceType));

        List<MemberLedgerRow> rows = new ArrayList<>();
        Map<String, Integer> bySource = new LinkedHashMap<>();
        int total = 0;
        for (PointsLedgerEntity entry : ledger) {
            String source = sources.getOrDefault(entry.getRequestId(), "OTHER");
            rows.add(new MemberLedgerRow(entry.getPeriodMonth(), entry.getOccurredOn(),
                    sourceLabel(source), entry.getEntryType(), entry.getPoints()));
            bySource.merge(sourceLabel(source), entry.getPoints(), Integer::sum);
            total += entry.getPoints();
        }
        return new MemberPointsView(userId, user.getNickname(), total, bySource, rows);
    }

    private String sourceLabel(String sourceType) {
        return switch (sourceType) {
            case "COMPETITION" -> "比赛奖";
            case "BOUNTY" -> "挖洞";
            case "HW" -> "护网";
            default -> "其它";
        };
    }
}
