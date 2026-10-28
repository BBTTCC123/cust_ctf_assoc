package cn.edu.cust.assoc.service;

import cn.edu.cust.assoc.common.BizException;
import cn.edu.cust.assoc.domain.*;
import cn.edu.cust.assoc.dto.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 竞赛档案前台视图：把实体和奖项翻成只含公开字段的 DTO。 */
@Service
public class CompetitionViewService {

    private final CompetitionService competitionService;
    private final AwardService awardService;
    private final UserDirectoryService userDirectory;
    private final MediaLinkService mediaLinkService;

    public CompetitionViewService(CompetitionService competitionService, AwardService awardService,
                                  UserDirectoryService userDirectory, MediaLinkService mediaLinkService) {
        this.competitionService = competitionService;
        this.awardService = awardService;
        this.userDirectory = userDirectory;
        this.mediaLinkService = mediaLinkService;
    }

    @Transactional(readOnly = true)
    public List<CompetitionSummary> summaries(Short seasonYear) {
        return competitionService.listBySeason(seasonYear).stream()
                .map(c -> new CompetitionSummary(c.getId(), c.getName(), c.getOrganizer(),
                        levelLabel(c.getLevel()), categoryLabel(c.getCategory()),
                        c.getStartDate(), c.getEndDate(), c.getSeasonYear()))
                .toList();
    }

    @Transactional(readOnly = true)
    public CompetitionDetailView detail(Long competitionId) {
        CompetitionEntity competition = competitionService.get(competitionId);
        List<AwardEntity> individual = awardService.listIndividual(competitionId);
        List<TeamAwardEntity> teams = awardService.listTeam(competitionId);

        List<Long> userIds = new ArrayList<>();
        individual.forEach(a -> userIds.add(a.getUserId()));
        Map<Long, List<AwardShareEntity>> sharesByTeam = new HashMap<>();
        for (TeamAwardEntity team : teams) {
            List<AwardShareEntity> shares = awardService.sharesOf(team.getId());
            sharesByTeam.put(team.getId(), shares);
            shares.forEach(s -> userIds.add(s.getUserId()));
        }
        Map<Long, String> names = userDirectory.nicknames(userIds);

        List<AwardView> awardViews = individual.stream()
                .map(a -> new AwardView(a.getId(), names.getOrDefault(a.getUserId(), "成员"),
                        rankLabel(a.getRankLevel()), a.getBasePoints()))
                .toList();

        List<TeamAwardView> teamViews = teams.stream()
                .map(team -> new TeamAwardView(team.getId(), rankLabel(team.getRankLevel()),
                        team.getBasePoints(),
                        sharesByTeam.get(team.getId()).stream()
                                .map(s -> new ShareView(names.getOrDefault(s.getUserId(), "成员"), s.getPoints()))
                                .toList()))
                .toList();

        List<MediaEntity> photos = mediaLinkService.mediaOf(MediaLinkService.COMPETITION_PHOTO, competitionId);

        return new CompetitionDetailView(competition.getId(), competition.getName(),
                competition.getOrganizer(), levelLabel(competition.getLevel()),
                categoryLabel(competition.getCategory()), competition.getStartDate(),
                competition.getEndDate(), competition.getSeasonYear(), awardViews, teamViews,
                photos.stream().map(MediaEntity::getId).toList());
    }

    private String levelLabel(String level) {
        for (Competitions.Level value : Competitions.Level.values()) {
            if (value.name().equals(level)) return value.getLabel();
        }
        return level;
    }

    private String categoryLabel(String category) {
        for (Competitions.Category value : Competitions.Category.values()) {
            if (value.name().equals(category)) return value.getLabel();
        }
        return category;
    }

    private String rankLabel(String rank) {
        for (CompetitionPoints.Rank value : CompetitionPoints.Rank.values()) {
            if (value.name().equals(rank)) return value.getLabel();
        }
        return rank;
    }
}
