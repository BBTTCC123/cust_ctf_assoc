package cn.edu.cust.assoc.controller;

import cn.edu.cust.assoc.domain.PointCalc;
import cn.edu.cust.assoc.repository.UserRepository;
import cn.edu.cust.assoc.service.PointsService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/admin/points")
public class AdminPointsController {

    private final PointsService pointsService;
    private final UserRepository userRepository;

    public AdminPointsController(PointsService pointsService, UserRepository userRepository) {
        this.pointsService = pointsService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public String index(Model model) {
        model.addAttribute("pending", pointsService.pending());
        model.addAttribute("all", pointsService.all());
        model.addAttribute("levels", PointCalc.Level.values());
        model.addAttribute("scenes", PointCalc.Scene.values());
        Map<Long, String> names = new HashMap<>();
        userRepository.findAll().forEach(u -> names.put(u.getId(), u.getNickname()));
        model.addAttribute("userNames", names);
        return "admin/points";
    }

    @PostMapping("/bounty")
    public String submitBounty(@RequestParam Long userId,
                               @RequestParam String level,
                               @RequestParam String scene,
                               @RequestParam String target,
                               @RequestParam String occurredOn,
                               @RequestParam String proofUrl,
                               Authentication auth,
                               RedirectAttributes redirect) {
        try {
            pointsService.submitBounty(userId, level, scene, target, LocalDate.parse(occurredOn),
                    proofUrl, currentUserId(auth));
            redirect.addFlashAttribute("message", "已提交，等待审核");
        } catch (RuntimeException e) {
            redirect.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/points";
    }

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable Long id, Authentication auth, RedirectAttributes redirect) {
        try {
            pointsService.approve(id, currentUserId(auth));
            redirect.addFlashAttribute("message", "已入账");
        } catch (RuntimeException e) {
            redirect.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/points";
    }

    @PostMapping("/{id}/reject")
    public String reject(@PathVariable Long id, @RequestParam(required = false) String reason,
                         Authentication auth, RedirectAttributes redirect) {
        try {
            pointsService.reject(id, currentUserId(auth), reason);
            redirect.addFlashAttribute("message", "已驳回");
        } catch (RuntimeException e) {
            redirect.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/points";
    }

    private Long currentUserId(Authentication auth) {
        return userRepository.findByUsername(auth.getName()).map(u -> u.getId()).orElse(1L);
    }
}
