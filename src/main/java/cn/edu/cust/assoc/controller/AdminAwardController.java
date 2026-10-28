package cn.edu.cust.assoc.controller;

import cn.edu.cust.assoc.domain.AwardShareEntity;
import cn.edu.cust.assoc.domain.CompetitionPoints;
import cn.edu.cust.assoc.domain.MediaEntity;
import cn.edu.cust.assoc.security.CurrentUserService;
import cn.edu.cust.assoc.service.AwardService;
import cn.edu.cust.assoc.service.CompetitionService;
import cn.edu.cust.assoc.service.MediaLinkService;
import cn.edu.cust.assoc.service.UserDirectoryService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/competitions/{competitionId}/awards")
public class AdminAwardController {

    private final AwardService awardService;
    private final CompetitionService competitionService;
    private final UserDirectoryService userDirectory;
    private final MediaLinkService mediaLinkService;
    private final CurrentUserService currentUser;

    public AdminAwardController(AwardService awardService, CompetitionService competitionService,
                                UserDirectoryService userDirectory, MediaLinkService mediaLinkService,
                                CurrentUserService currentUser) {
        this.awardService = awardService;
        this.competitionService = competitionService;
        this.userDirectory = userDirectory;
        this.mediaLinkService = mediaLinkService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public String list(@PathVariable Long competitionId, Model model) {
        model.addAttribute("competition", competitionService.get(competitionId));
        model.addAttribute("individual", awardService.listIndividual(competitionId));
        model.addAttribute("teams", awardService.listTeam(competitionId));
        var users = userDirectory.all();
        model.addAttribute("users", users);
        model.addAttribute("ranks", CompetitionPoints.Rank.values());

        Map<Long, String> names = new HashMap<>();
        users.forEach(u -> names.put(u.getId(), u.getNickname()));
        model.addAttribute("userNames", names);

        Map<Long, List<AwardShareEntity>> sharesByTeam = new HashMap<>();
        awardService.listTeam(competitionId)
                .forEach(ta -> sharesByTeam.put(ta.getId(), awardService.sharesOf(ta.getId())));
        model.addAttribute("sharesByTeam", sharesByTeam);

        List<MediaEntity> photos = mediaLinkService.mediaOf(MediaLinkService.COMPETITION_PHOTO, competitionId);
        model.addAttribute("photos", photos);
        return "admin/awards";
    }

    @PostMapping("/individual")
    public String addIndividual(@PathVariable Long competitionId,
                                @RequestParam Long userId,
                                @RequestParam String rankLevel,
                                @RequestParam(required = false) String proofUrl,
                                Authentication auth,
                                RedirectAttributes redirect) {
        try {
            awardService.createIndividual(competitionId, userId, rankLevel, proofUrl,
                    currentUser.id(auth));
            redirect.addFlashAttribute("message", "个人奖已记录，积分已同步入账");
        } catch (RuntimeException e) {
            redirect.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/competitions/" + competitionId + "/awards";
    }

    @PostMapping("/team")
    public String addTeam(@PathVariable Long competitionId,
                          @RequestParam String rankLevel,
                          @RequestParam(required = false) String proofUrl,
                          @RequestParam(required = false) Long user1, @RequestParam(required = false) Integer points1,
                          @RequestParam(required = false) Long user2, @RequestParam(required = false) Integer points2,
                          @RequestParam(required = false) Long user3, @RequestParam(required = false) Integer points3,
                          @RequestParam(required = false) Long user4, @RequestParam(required = false) Integer points4,
                          Authentication auth,
                          RedirectAttributes redirect) {
        try {
            Map<Long, Integer> shares = new LinkedHashMap<>();
            putShare(shares, user1, points1);
            putShare(shares, user2, points2);
            putShare(shares, user3, points3);
            putShare(shares, user4, points4);
            awardService.createTeam(competitionId, rankLevel, proofUrl, currentUser.id(auth), shares);
            redirect.addFlashAttribute("message", "团队奖已记录，成员积分已同步入账");
        } catch (RuntimeException e) {
            redirect.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/competitions/" + competitionId + "/awards";
    }

    @PostMapping("/photos")
    public String attachPhoto(@PathVariable Long competitionId, @RequestParam Long mediaId,
                              RedirectAttributes redirect) {
        try {
            mediaLinkService.attach(MediaLinkService.COMPETITION_PHOTO, competitionId, mediaId);
            redirect.addFlashAttribute("message", "照片已关联");
        } catch (RuntimeException e) {
            redirect.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/competitions/" + competitionId + "/awards";
    }

    @PostMapping("/photos/detach")
    public String detachPhoto(@PathVariable Long competitionId, @RequestParam Long mediaId,
                              RedirectAttributes redirect) {
        mediaLinkService.detachMedia(MediaLinkService.COMPETITION_PHOTO, competitionId, mediaId);
        redirect.addFlashAttribute("message", "已解除关联");
        return "redirect:/admin/competitions/" + competitionId + "/awards";
    }

    /** 同一成员填两次会被 Map 静默合并，这里显式报错。 */
    private void putShare(Map<Long, Integer> shares, Long userId, Integer points) {
        if (userId == null || points == null) {
            return;
        }
        if (shares.putIfAbsent(userId, points) != null) {
            throw new IllegalArgumentException("同一个成员不能填两次");
        }
    }
}
