package cn.edu.cust.assoc.controller;

import cn.edu.cust.assoc.domain.RoleEntity;
import cn.edu.cust.assoc.domain.UserEntity;
import cn.edu.cust.assoc.service.AccountService;
import cn.edu.cust.assoc.service.UserAdminService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/users")
public class AdminUserController {

    private final UserAdminService userAdminService;
    private final AccountService accountService;

    public AdminUserController(UserAdminService userAdminService, AccountService accountService) {
        this.userAdminService = userAdminService;
        this.accountService = accountService;
    }

    @GetMapping
    public String list(Model model, Authentication auth) {
        List<UserEntity> users = userAdminService.users();
        Map<Long, Set<Long>> userRoleIds = new HashMap<>();
        for (UserEntity u : users) {
            userRoleIds.put(u.getId(), u.getRoles().stream().map(RoleEntity::getId).collect(Collectors.toSet()));
        }
        model.addAttribute("users", users);
        model.addAttribute("roles", userAdminService.roles());
        model.addAttribute("userRoleIds", userRoleIds);
        model.addAttribute("me", auth.getName());
        model.addAttribute("invites", accountService.invites());
        return "admin/users";
    }

    @PostMapping
    public String createMember(@RequestParam String username, @RequestParam String nickname,
                               @RequestParam String password, RedirectAttributes redirect) {
        try {
            UserEntity user = accountService.createMember(username, nickname, password);
            redirect.addFlashAttribute("message", "成员 " + user.getUsername() + " 已创建，首次登录必须改密");
        } catch (RuntimeException e) {
            redirect.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/reset-password")
    public String resetPassword(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            String temp = accountService.resetPassword(id);
            redirect.addFlashAttribute("message", "一次性临时密码：" + temp + "（只显示这一次，请立即转告本人）");
        } catch (RuntimeException e) {
            redirect.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/invites")
    public String createInvite(@RequestParam(defaultValue = "1") int maxUses,
                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expiresOn,
                               @RequestParam(required = false) String note,
                               RedirectAttributes redirect) {
        try {
            var invite = accountService.createInvite(maxUses, expiresOn.atTime(LocalTime.MAX), note);
            redirect.addFlashAttribute("message", "邀请码已生成：" + invite.getCode());
        } catch (RuntimeException e) {
            redirect.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/invites/{id}/revoke")
    public String revokeInvite(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            accountService.revokeInvite(id);
            redirect.addFlashAttribute("message", "邀请码已停用");
        } catch (RuntimeException e) {
            redirect.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/roles")
    public String setRoles(@PathVariable Long id,
                           @RequestParam(required = false) List<Long> roleIds,
                           RedirectAttributes redirect) {
        try {
            userAdminService.setRoles(id, roleIds == null ? Set.of() : Set.copyOf(roleIds));
            redirect.addFlashAttribute("message", "角色已更新，目标用户已下线");
        } catch (RuntimeException e) {
            redirect.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/status")
    public String setStatus(@PathVariable Long id, @RequestParam String status,
                            Authentication auth, RedirectAttributes redirect) {
        String target = userAdminService.users().stream()
                .filter(u -> u.getId().equals(id)).map(UserEntity::getUsername).findFirst().orElse("");
        if (target.equals(auth.getName()) && "DISABLED".equals(status)) {
            redirect.addFlashAttribute("errorMessage", "不能禁用自己的账号");
            return "redirect:/admin/users";
        }
        try {
            userAdminService.setStatus(id, status);
            redirect.addFlashAttribute("message", "状态已更新");
        } catch (RuntimeException e) {
            redirect.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/users";
    }
}
