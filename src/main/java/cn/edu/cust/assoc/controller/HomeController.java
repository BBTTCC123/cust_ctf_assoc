package cn.edu.cust.assoc.controller;

import cn.edu.cust.assoc.service.MemberService;
import cn.edu.cust.assoc.service.PublicContentService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final PublicContentService publicContent;
    private final MemberService memberService;

    public HomeController(PublicContentService publicContent, MemberService memberService) {
        this.publicContent = publicContent;
        this.memberService = memberService;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("greeting", "网络攻防协会");
        model.addAttribute("announcements", publicContent.announcements().stream().limit(3).toList());
        model.addAttribute("honors", publicContent.honors().stream().limit(3).toList());
        model.addAttribute("topPoints", memberService.top(3));
        return "index";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/about")
    public String about() {
        return "about";
    }
}
