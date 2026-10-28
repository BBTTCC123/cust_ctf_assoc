package cn.edu.cust.assoc.controller;

import cn.edu.cust.assoc.security.CurrentUserService;
import cn.edu.cust.assoc.service.TeamService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/teams")
public class AdminTeamController {

    private final TeamService teamService;
    private final CurrentUserService currentUser;

    public AdminTeamController(TeamService teamService, CurrentUserService currentUser) {
        this.teamService = teamService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public String index(Model model) {
        model.addAttribute("teams", teamService.adminList());
        return "admin/teams";
    }

    @PostMapping("/{id}/disband")
    public String disband(@PathVariable Long id, Authentication auth, RedirectAttributes redirect) {
        try {
            teamService.disband(currentUser.id(auth), id, true);
            redirect.addFlashAttribute("message", "战队已由管理员解散");
        } catch (RuntimeException e) {
            redirect.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/teams";
    }
}
