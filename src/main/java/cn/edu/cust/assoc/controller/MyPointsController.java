package cn.edu.cust.assoc.controller;

import cn.edu.cust.assoc.dto.OwnLedgerRow;
import cn.edu.cust.assoc.domain.PointsLedgerEntity;
import cn.edu.cust.assoc.security.CurrentUserService;
import cn.edu.cust.assoc.service.PointsService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class MyPointsController {

    private final PointsService pointsService;
    private final CurrentUserService currentUser;

    public MyPointsController(PointsService pointsService, CurrentUserService currentUser) {
        this.pointsService = pointsService;
        this.currentUser = currentUser;
    }

    @GetMapping("/me")
    public String mine(Model model, Authentication auth) {
        Long userId = currentUser.id(auth);
        List<PointsLedgerEntity> ledger = pointsService.ledgerOf(userId);
        int total = ledger.stream().mapToInt(PointsLedgerEntity::getPoints).sum();
        List<OwnLedgerRow> rows = ledger.stream()
                .map(l -> new OwnLedgerRow(l.getId(), l.getPeriodMonth(), l.getOccurredOn(),
                        l.getEntryType(), l.getPoints(), l.getNote()))
                .toList();
        model.addAttribute("ledger", rows);
        model.addAttribute("total", total);
        model.addAttribute("memberId", userId);
        return "me";
    }

    /** 申诉：只能对自己的加分流水发起，校验在 PointsService 里统一做。 */
    @PostMapping("/me/appeal")
    public String appeal(@RequestParam Long ledgerId, @RequestParam String reason,
                         Authentication auth, RedirectAttributes redirect) {
        try {
            pointsService.submitAppeal(currentUser.id(auth), ledgerId, reason);
            redirect.addFlashAttribute("message", "申诉已提交，等积分管理员处理");
        } catch (RuntimeException e) {
            redirect.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/me";
    }
}
