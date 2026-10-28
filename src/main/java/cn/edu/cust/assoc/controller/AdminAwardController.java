package cn.edu.cust.assoc.controller;

import cn.edu.cust.assoc.domain.CompetitionPoints;
import cn.edu.cust.assoc.repository.UserRepository;
import cn.edu.cust.assoc.service.AwardService;
import cn.edu.cust.assoc.service.CompetitionService;
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
    private final UserRepository userRepository;

    public AdminAwardController(AwardService awardService,
                                CompetitionService competitionService,
                                UserRepository userRepository) {
        this.awardService = awardService;
        this.competitionService = competitionService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public String list(@PathVariable Long competitionId, Model model) {
        model.addAttribute("competition", competitionService.get(competitionId));
        model.addAttribute("individual", awardService.listIndividual(competitionId));
        model.addAttribute("teams", awardService.listTeam(competitionId));
        var users = userRepository.findAll();
        model.addAttribute("users", users);
        model.addAttribute("ranks", CompetitionPoints.Rank.values());

        Map<Long, String> names = new HashMap<>();
        users.forEach(u -> names.put(u.getId(), u.getNickname()));
        model.addAttribute("userNames", names);

        Map<Long, List<cn.edu.cust.assoc.domain.AwardShareEntity>> sharesByTeam = new HashMap<>();
        awardService.listTeam(competitionId)
                .forEach(ta -> sharesByTeam.put(ta.getId(), awardService.sharesOf(ta.getId())));
        model.addAttribute("sharesByTeam", sharesByTeam);
        return "admin/awards";
    }

    @PostMapping("/individual")
    public String addIndividual(@PathVariable Long competitionId,
                                @RequestParam Long userId,
                                @RequestParam String rankLevel,
                                @RequestParam(required = false) String proofUrl,
                                RedirectAttributes redirect) {
        try {
            awardService.createIndividual(competitionId, userId, rankLevel, proofUrl);
            redirect.addFlashAttribute("message", "个人奖已记录");
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
                          RedirectAttributes redirect) {
        Map<Long, Integer> shares = new LinkedHashMap<>();
        putIfPresent(shares, user1, points1);
        putIfPresent(shares, user2, points2);
        putIfPresent(shares, user3, points3);
        putIfPresent(shares, user4, points4);
        try {
            awardService.createTeam(competitionId, rankLevel, proofUrl, 1L, shares);
            redirect.addFlashAttribute("message", "团队奖已记录");
        } catch (RuntimeException e) {
            redirect.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/competitions/" + competitionId + "/awards";
    }

    private void putIfPresent(Map<Long, Integer> shares, Long userId, Integer points) {
        if (userId != null && points != null) {
            shares.put(userId, points);
        }
    }
}