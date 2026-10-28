package cn.edu.cust.assoc.controller;

import cn.edu.cust.assoc.common.BizException;
import cn.edu.cust.assoc.service.AccountService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** 邀请码注册。账号审批流程由超管发码控制。 */
@Controller
public class RegisterController {

    private final AccountService accountService;

    public RegisterController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/register")
    public String form(@RequestParam(required = false) String code, Model model) {
        model.addAttribute("code", code);
        return "register";
    }

    @PostMapping("/register")
    public String register(@RequestParam String inviteCode,
                           @RequestParam String username,
                           @RequestParam String nickname,
                           @RequestParam String password,
                           @RequestParam String confirmPassword,
                           RedirectAttributes redirect) {
        if (!password.equals(confirmPassword)) {
            redirect.addFlashAttribute("errorMessage", "两次输入的密码不一致");
            redirect.addFlashAttribute("code", inviteCode);
            return "redirect:/register";
        }
        try {
            accountService.register(inviteCode, username, nickname, password);
        } catch (BizException e) {
            redirect.addFlashAttribute("errorMessage", e.getMessage());
            redirect.addFlashAttribute("code", inviteCode);
            return "redirect:/register";
        }
        redirect.addFlashAttribute("message", "注册成功，请登录");
        return "redirect:/login";
    }
}
