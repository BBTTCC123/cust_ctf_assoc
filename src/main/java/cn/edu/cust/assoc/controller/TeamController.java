package cn.edu.cust.assoc.controller;

import cn.edu.cust.assoc.domain.TeamEntity;
import cn.edu.cust.assoc.dto.TeamDetail;
import cn.edu.cust.assoc.dto.TeamForm;
import cn.edu.cust.assoc.security.CurrentUserService;
import cn.edu.cust.assoc.service.TeamService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/teams")
public class TeamController {

    private final TeamService teamService;
    private final CurrentUserService currentUser;

    public TeamController(TeamService teamService, CurrentUserService currentUser) {
        this.teamService = teamService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public String list(Model model, Authentication auth) {
        Long myId = currentUser.id(auth);
        model.addAttribute("teams", teamService.listActive());
        model.addAttribute("myTeam", teamService.myTeam(myId).orElse(null));
        return "teams";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model, Authentication auth) {
        Long myId = currentUser.id(auth);
        TeamDetail detail = teamService.detail(id);
        model.addAttribute("team", detail);
        model.addAttribute("myId", myId);
        model.addAttribute("isLeader", detail.leaderId().equals(myId)
                && TeamEntity.ACTIVE.equals(detail.status()));
        model.addAttribute("isMember", detail.members().stream()
                .anyMatch(member -> member.userId().equals(myId)));
        return "team";
    }

    @PostMapping
    public String create(@Valid TeamForm form, BindingResult binding, Authentication auth,
                         RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            return flashError(redirect, binding.getAllErrors().get(0).getDefaultMessage(), "/teams");
        }
        try {
            TeamEntity team = teamService.create(currentUser.id(auth), form.getName(), form.getIntro());
            redirect.addFlashAttribute("message", "战队创建成功，你就是队长");
            return "redirect:/teams/" + team.getId();
        } catch (RuntimeException e) {
            return flashError(redirect, e.getMessage(), "/teams");
        }
    }

    @PostMapping("/{id}/members")
    public String addMember(@PathVariable Long id, @RequestParam String username,
                            Authentication auth, RedirectAttributes redirect) {
        try {
            teamService.addMember(currentUser.id(auth), id, username);
            redirect.addFlashAttribute("message", "成员已加入战队");
        } catch (RuntimeException e) {
            return flashError(redirect, e.getMessage(), "/teams/" + id);
        }
        return "redirect:/teams/" + id;
    }

    @PostMapping("/{id}/members/{userId}/remove")
    public String removeMember(@PathVariable Long id, @PathVariable Long userId,
                               Authentication auth, RedirectAttributes redirect) {
        try {
            teamService.removeMember(currentUser.id(auth), id, userId);
            redirect.addFlashAttribute("message", "成员已移出战队");
        } catch (RuntimeException e) {
            return flashError(redirect, e.getMessage(), "/teams/" + id);
        }
        return "redirect:/teams/" + id;
    }

    @PostMapping("/{id}/leave")
    public String leave(@PathVariable Long id, Authentication auth, RedirectAttributes redirect) {
        try {
            teamService.leave(currentUser.id(auth), id);
            redirect.addFlashAttribute("message", "已退出战队");
            return "redirect:/teams";
        } catch (RuntimeException e) {
            return flashError(redirect, e.getMessage(), "/teams/" + id);
        }
    }

    @PostMapping("/{id}/leader")
    public String transferLeader(@PathVariable Long id, @RequestParam Long userId,
                                 Authentication auth, RedirectAttributes redirect) {
        try {
            teamService.transferLeader(currentUser.id(auth), id, userId);
            redirect.addFlashAttribute("message", "队长已转让");
        } catch (RuntimeException e) {
            return flashError(redirect, e.getMessage(), "/teams/" + id);
        }
        return "redirect:/teams/" + id;
    }

    @PostMapping("/{id}/intro")
    public String updateIntro(@PathVariable Long id, @RequestParam(required = false) String intro,
                              Authentication auth, RedirectAttributes redirect) {
        try {
            teamService.updateIntro(currentUser.id(auth), id, intro);
            redirect.addFlashAttribute("message", "战队简介已更新");
        } catch (RuntimeException e) {
            return flashError(redirect, e.getMessage(), "/teams/" + id);
        }
        return "redirect:/teams/" + id;
    }

    @PostMapping("/{id}/disband")
    public String disband(@PathVariable Long id, Authentication auth, RedirectAttributes redirect) {
        try {
            teamService.disband(currentUser.id(auth), id, false);
            redirect.addFlashAttribute("message", "战队已解散");
            return "redirect:/teams";
        } catch (RuntimeException e) {
            return flashError(redirect, e.getMessage(), "/teams/" + id);
        }
    }

    private String flashError(RedirectAttributes redirect, String message, String path) {
        redirect.addFlashAttribute("errorMessage", message);
        return "redirect:" + path;
    }
}
