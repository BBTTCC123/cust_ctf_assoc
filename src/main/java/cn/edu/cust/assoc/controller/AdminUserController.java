package cn.edu.cust.assoc.controller;

import cn.edu.cust.assoc.domain.RoleEntity;
import cn.edu.cust.assoc.domain.UserEntity;
import cn.edu.cust.assoc.service.UserAdminService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/users")
public class AdminUserController {

    private final UserAdminService userAdminService;

    public AdminUserController(UserAdminService userAdminService) {
        this.userAdminService = userAdminService;
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
        return "admin/users";
    }

    @PostMapping("/{id}/roles")
    public String setRoles(@PathVariable Long id,
                           @RequestParam(required = false) List<Long> roleIds,
                           RedirectAttributes redirect) {
        userAdminService.setRoles(id, roleIds == null ? Set.of() : Set.copyOf(roleIds));
        redirect.addFlashAttribute("message", "角色已更新");
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
        userAdminService.setStatus(id, status);
        redirect.addFlashAttribute("message", "状态已更新");
        return "redirect:/admin/users";
    }
}
