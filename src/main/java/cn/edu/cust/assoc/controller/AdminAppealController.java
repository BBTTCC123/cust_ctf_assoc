package cn.edu.cust.assoc.controller;

import cn.edu.cust.assoc.security.CurrentUserService;
import cn.edu.cust.assoc.service.PointsAdminViewService;
import cn.edu.cust.assoc.service.PointsService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/appeals")
public class AdminAppealController {

    private final PointsService pointsService;
    private final PointsAdminViewService viewService;
    private final CurrentUserService currentUser;

    public AdminAppealController(PointsService pointsService, PointsAdminViewService viewService,
                                 CurrentUserService currentUser) {
        this.pointsService = pointsService;
        this.viewService = viewService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("appeals", viewService.appeals());
        return "admin/appeals";
    }

    @PostMapping("/{id}/accept")
    public String accept(@PathVariable Long id, Authentication auth, RedirectAttributes redirect) {
        try {
            pointsService.acceptAppeal(id, currentUser.id(auth));
            redirect.addFlashAttribute("message", "申诉成立，已冲抵原流水");
        } catch (RuntimeException e) {
            redirect.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/appeals";
    }

    @PostMapping("/{id}/reject")
    public String reject(@PathVariable Long id, @RequestParam(required = false) String note,
                         Authentication auth, RedirectAttributes redirect) {
        try {
            pointsService.rejectAppeal(id, currentUser.id(auth), note);
            redirect.addFlashAttribute("message", "已驳回申诉");
        } catch (RuntimeException e) {
            redirect.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/appeals";
    }
}
