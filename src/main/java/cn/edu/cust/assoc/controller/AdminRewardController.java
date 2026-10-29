package cn.edu.cust.assoc.controller;

import cn.edu.cust.assoc.dto.RewardItemForm;
import cn.edu.cust.assoc.security.CurrentUserService;
import cn.edu.cust.assoc.service.RewardService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/rewards")
public class AdminRewardController {

    private final RewardService rewardService;
    private final CurrentUserService currentUser;

    public AdminRewardController(RewardService rewardService, CurrentUserService currentUser) {
        this.rewardService = rewardService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public String index(Model model) {
        model.addAttribute("items", rewardService.allItems());
        model.addAttribute("requests", rewardService.allRequests());
        return "admin/rewards";
    }

    @PostMapping("/items")
    public String createItem(@Valid RewardItemForm form, BindingResult binding,
                             RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            return flashError(redirect, binding.getAllErrors().get(0).getDefaultMessage());
        }
        try {
            rewardService.createItem(form);
            redirect.addFlashAttribute("message", "资源已创建");
        } catch (RuntimeException e) {
            return flashError(redirect, e.getMessage());
        }
        return "redirect:/admin/rewards";
    }

    @PostMapping("/items/{id}")
    public String updateItem(@PathVariable Long id, @Valid RewardItemForm form, BindingResult binding,
                             RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            return flashError(redirect, binding.getAllErrors().get(0).getDefaultMessage());
        }
        try {
            rewardService.updateItem(id, form);
            redirect.addFlashAttribute("message", "资源已保存");
        } catch (RuntimeException e) {
            return flashError(redirect, e.getMessage());
        }
        return "redirect:/admin/rewards";
    }

    @PostMapping("/items/{id}/toggle")
    public String toggleItem(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            rewardService.toggleItem(id);
            redirect.addFlashAttribute("message", "上架状态已切换");
        } catch (RuntimeException e) {
            return flashError(redirect, e.getMessage());
        }
        return "redirect:/admin/rewards";
    }

    @PostMapping("/requests/{id}/approve")
    public String approve(@PathVariable Long id, Authentication auth, RedirectAttributes redirect) {
        try {
            rewardService.approve(id, currentUser.id(auth));
            redirect.addFlashAttribute("message", "兑换已通过，余额与库存已扣减");
        } catch (RuntimeException e) {
            return flashError(redirect, e.getMessage());
        }
        return "redirect:/admin/rewards";
    }

    @PostMapping("/requests/{id}/reject")
    public String reject(@PathVariable Long id, @RequestParam(required = false) String note,
                         Authentication auth, RedirectAttributes redirect) {
        try {
            rewardService.reject(id, currentUser.id(auth), note);
            redirect.addFlashAttribute("message", "兑换已驳回");
        } catch (RuntimeException e) {
            return flashError(redirect, e.getMessage());
        }
        return "redirect:/admin/rewards";
    }

    private String flashError(RedirectAttributes redirect, String message) {
        redirect.addFlashAttribute("errorMessage", message);
        return "redirect:/admin/rewards";
    }
}
