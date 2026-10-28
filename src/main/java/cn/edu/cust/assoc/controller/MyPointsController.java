package cn.edu.cust.assoc.controller;

import cn.edu.cust.assoc.domain.PointsAppealEntity;
import cn.edu.cust.assoc.domain.PointsLedgerEntity;
import cn.edu.cust.assoc.repository.PointsAppealRepository;
import cn.edu.cust.assoc.repository.UserRepository;
import cn.edu.cust.assoc.service.PointsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.security.Principal;
import java.util.List;

@Controller
public class MyPointsController {

    private final PointsService pointsService;
    private final UserRepository userRepository;
    private final PointsAppealRepository appealRepository;

    public MyPointsController(PointsService pointsService, UserRepository userRepository,
                              PointsAppealRepository appealRepository) {
        this.pointsService = pointsService;
        this.userRepository = userRepository;
        this.appealRepository = appealRepository;
    }

    @GetMapping("/me")
    public String mine(Model model, Principal principal) {
        Long userId = currentUserId(principal);
        List<PointsLedgerEntity> ledger = pointsService.ledgerOf(userId);
        int total = ledger.stream().mapToInt(PointsLedgerEntity::getPoints).sum();
        model.addAttribute("ledger", ledger);
        model.addAttribute("total", total);
        model.addAttribute("userId", userId);
        return "me";
    }

    @PostMapping("/me/appeal")
    public String appeal(@RequestParam Long ledgerId, @RequestParam String reason,
                         Principal principal, RedirectAttributes redirect) {
        if (reason == null || reason.isBlank()) {
            redirect.addFlashAttribute("errorMessage", "申诉要写理由");
            return "redirect:/me";
        }
        PointsAppealEntity appeal = new PointsAppealEntity();
        appeal.setLedgerId(ledgerId);
        appeal.setUserId(currentUserId(principal));
        appeal.setReason(reason.trim());
        appeal.setState("OPEN");
        appealRepository.save(appeal);
        redirect.addFlashAttribute("message", "申诉已提交，等积分管理员处理");
        return "redirect:/me";
    }

    private Long currentUserId(Principal principal) {
        return userRepository.findByUsername(principal.getName())
                .map(u -> u.getId())
                .orElseThrow(() -> new IllegalStateException("登录用户不存在"));
    }
}
