package cn.edu.cust.assoc.controller;

import cn.edu.cust.assoc.common.BizException;
import cn.edu.cust.assoc.security.CurrentUserService;
import cn.edu.cust.assoc.service.RewardService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class RewardController {

    private final RewardService rewardService;
    private final CurrentUserService currentUser;

    public RewardController(RewardService rewardService, CurrentUserService currentUser) {
        this.rewardService = rewardService;
        this.currentUser = currentUser;
    }

    @GetMapping("/rewards")
    public String catalog(Model model, Authentication auth) {
        model.addAttribute("view", rewardService.catalog(currentUser.id(auth)));
        return "rewards";
    }

    @PostMapping("/rewards/{itemId}/apply")
    public String apply(@PathVariable Long itemId, @RequestParam(required = false) String note,
                        Authentication auth, RedirectAttributes redirect) {
        try {
            rewardService.apply(currentUser.id(auth), itemId, note);
            redirect.addFlashAttribute("message", "兑换申请已提交，等积分管理员审核");
        } catch (BizException e) {
            redirect.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/rewards";
    }

    @PostMapping("/me/redemptions/{id}/cancel")
    public String cancel(@PathVariable Long id, Authentication auth, RedirectAttributes redirect) {
        try {
            rewardService.cancel(currentUser.id(auth), id);
            redirect.addFlashAttribute("message", "兑换申请已取消");
        } catch (BizException e) {
            redirect.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/rewards";
    }
}
