package cn.edu.cust.assoc.controller;

import cn.edu.cust.assoc.service.MemberService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LeaderboardController {

    private final MemberService memberService;

    public LeaderboardController(MemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping("/points")
    public String board(@RequestParam(defaultValue = "ALL") String type, Model model) {
        model.addAttribute("rows", memberService.ranking(type, 300));
        model.addAttribute("type", type);
        return "points";
    }
}
