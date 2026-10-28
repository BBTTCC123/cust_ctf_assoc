package cn.edu.cust.assoc.controller;

import cn.edu.cust.assoc.domain.PointsAppealEntity;
import cn.edu.cust.assoc.repository.PointsAppealRepository;
import cn.edu.cust.assoc.repository.UserRepository;
import cn.edu.cust.assoc.service.PointsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;

@Controller
@RequestMapping("/admin/appeals")
public class AdminAppealController {

    private final PointsAppealRepository appealRepository;
    private final PointsService pointsService;
    private final UserRepository userRepository;

    public AdminAppealController(PointsAppealRepository appealRepository, PointsService pointsService,
                                 UserRepository userRepository) {
        this.appealRepository = appealRepository;
        this.pointsService = pointsService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("appeals", appealRepository.findAllByOrderByCreatedAtDesc());
        return "admin/appeals";
    }

    @PostMapping("/{id}/accept")
    public String accept(@PathVariable Long id, RedirectAttributes redirect) {
        PointsAppealEntity appeal = appealRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("申诉不存在"));
        if (!"OPEN".equals(appeal.getState())) {
            redirect.addFlashAttribute("errorMessage", "这条申诉已经处理过了");
            return "redirect:/admin/appeals";
        }
        // 申诉成立 = 冲抵原流水。原流水不动，另加一条负数。
        pointsService.reverse(appeal.getLedgerId(), "申诉成立，冲抵原流水 #" + appeal.getLedgerId());
        appeal.setState("ACCEPTED");
        appeal.setHandledAt(LocalDateTime.now());
        appealRepository.save(appeal);
        redirect.addFlashAttribute("message", "已冲抵原流水");
        return "redirect:/admin/appeals";
    }

    @PostMapping("/{id}/reject")
    public String reject(@PathVariable Long id, RedirectAttributes redirect) {
        PointsAppealEntity appeal = appealRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("申诉不存在"));
        appeal.setState("REJECTED");
        appeal.setHandledAt(LocalDateTime.now());
        appealRepository.save(appeal);
        redirect.addFlashAttribute("message", "已驳回申诉");
        return "redirect:/admin/appeals";
    }
}
