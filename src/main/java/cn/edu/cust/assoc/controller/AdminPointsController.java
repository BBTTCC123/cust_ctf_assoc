package cn.edu.cust.assoc.controller;

import cn.edu.cust.assoc.domain.PointCalc;
import cn.edu.cust.assoc.domain.PointsRuleEntity;
import cn.edu.cust.assoc.dto.PointsRuleRow;
import cn.edu.cust.assoc.security.CurrentUserService;
import cn.edu.cust.assoc.service.PointsAdminViewService;
import cn.edu.cust.assoc.service.PointsRuleService;
import cn.edu.cust.assoc.service.PointsService;
import cn.edu.cust.assoc.service.UserDirectoryService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/admin/points")
public class AdminPointsController {

    private final PointsService pointsService;
    private final PointsAdminViewService viewService;
    private final PointsRuleService ruleService;
    private final UserDirectoryService userDirectory;
    private final CurrentUserService currentUser;

    public AdminPointsController(PointsService pointsService, PointsAdminViewService viewService,
                                 PointsRuleService ruleService, UserDirectoryService userDirectory,
                                 CurrentUserService currentUser) {
        this.pointsService = pointsService;
        this.viewService = viewService;
        this.ruleService = ruleService;
        this.userDirectory = userDirectory;
        this.currentUser = currentUser;
    }

    @GetMapping
    public String index(Model model) {
        model.addAttribute("pending", viewService.pendingRows());
        model.addAttribute("all", viewService.allRows());
        model.addAttribute("users", userDirectory.all());
        model.addAttribute("levels", PointCalc.Level.values());
        model.addAttribute("scenes", PointCalc.Scene.values());
        List<PointsRuleRow> rules = ruleService.all().stream()
                .map(this::ruleRow)
                .toList();
        model.addAttribute("rules", rules);
        return "admin/points";
    }

    @PostMapping("/bounty")
    public String submitBounty(@RequestParam Long userId,
                               @RequestParam String level,
                               @RequestParam String scene,
                               @RequestParam String target,
                               @RequestParam String platform,
                               @RequestParam String externalId,
                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate occurredOn,
                               @RequestParam String proofUrl,
                               Authentication auth,
                               RedirectAttributes redirect) {
        try {
            pointsService.submitBounty(userId, level, scene, target, platform, externalId,
                    occurredOn, proofUrl, currentUser.id(auth));
            redirect.addFlashAttribute("message", "已提交，等待审核");
        } catch (RuntimeException e) {
            redirect.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/points";
    }

    @PostMapping("/hw")
    public String submitHw(@RequestParam Long userId,
                           @RequestParam int score,
                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate occurredOn,
                           @RequestParam String proofUrl,
                           Authentication auth,
                           RedirectAttributes redirect) {
        try {
            pointsService.submitHw(userId, score, occurredOn, proofUrl, currentUser.id(auth));
            redirect.addFlashAttribute("message", "护网成绩已提交，等待审核");
        } catch (RuntimeException e) {
            redirect.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/points";
    }

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable Long id, Authentication auth, RedirectAttributes redirect) {
        try {
            pointsService.approve(id, currentUser.id(auth));
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
            pointsService.reject(id, currentUser.id(auth), reason);
            redirect.addFlashAttribute("message", "已驳回");
        } catch (RuntimeException e) {
            redirect.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/points";
    }

    @PostMapping("/rules")
    public String publishRule(@RequestParam String direction,
                              @RequestParam String itemKey,
                              @RequestParam int points,
                              @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate effectiveFrom,
                              RedirectAttributes redirect) {
        try {
            ruleService.publish(direction, itemKey, points, effectiveFrom);
            redirect.addFlashAttribute("message", "新规则版本已发布，只影响之后发生的申请");
        } catch (RuntimeException e) {
            redirect.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/points";
    }

    private PointsRuleRow ruleRow(PointsRuleEntity rule) {
        return new PointsRuleRow(rule.getId(), rule.getDirection(), rule.getItemKey(), rule.getVersion(),
                rule.getPoints(), rule.getEffectiveFrom(), rule.getPublishedAt());
    }
}
