package cn.edu.cust.assoc.controller;

import cn.edu.cust.assoc.common.BizException;
import cn.edu.cust.assoc.security.CurrentUserService;
import cn.edu.cust.assoc.service.AccountService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** 自助改密。首次登录、管理员重置后都会被强制带到这一页。 */
@Controller
public class AccountController {

    private final AccountService accountService;
    private final CurrentUserService currentUser;

    public AccountController(AccountService accountService, CurrentUserService currentUser) {
        this.accountService = accountService;
        this.currentUser = currentUser;
    }

    @GetMapping("/password/change")
    public String form(Model model, Authentication auth) {
        model.addAttribute("username", auth.getName());
        return "password-change";
    }

    @PostMapping("/password/change")
    public String change(@RequestParam String currentPassword,
                         @RequestParam String newPassword,
                         @RequestParam String confirmPassword,
                         Authentication auth, HttpServletRequest request,
                         RedirectAttributes redirect) {
        if (!newPassword.equals(confirmPassword)) {
            redirect.addFlashAttribute("errorMessage", "两次输入的新密码不一致");
            return "redirect:/password/change";
        }
        try {
            accountService.changeOwnPassword(currentUser.id(auth), currentPassword, newPassword);
        } catch (BizException e) {
            redirect.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/password/change";
        }
        // 改完密码让旧会话彻底作废：清上下文、销毁会话，再提示重新登录
        SecurityContextHolder.clearContext();
        var session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        redirect.addFlashAttribute("message", "密码已修改，请用新密码重新登录");
        return "redirect:/login";
    }
}
