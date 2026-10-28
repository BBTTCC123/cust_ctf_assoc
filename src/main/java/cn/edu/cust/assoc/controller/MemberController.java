package cn.edu.cust.assoc.controller;

import cn.edu.cust.assoc.service.MemberService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class MemberController {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    /** 公开成员积分页：只展示昵称、每笔来源和分数；佐证与内部备注不公开。 */
    @GetMapping("/members/{id}")
    public String points(@PathVariable Long id, Model model) {
        model.addAttribute("member", memberService.view(id));
        return "member";
    }
}
