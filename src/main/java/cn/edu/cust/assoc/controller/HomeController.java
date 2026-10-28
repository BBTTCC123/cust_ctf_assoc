package cn.edu.cust.assoc.controller;

import cn.edu.cust.assoc.service.PointsService;
import cn.edu.cust.assoc.service.PublicContentService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class HomeController {

    private final PublicContentService publicContent;
    private final PointsService pointsService;

    public HomeController(PublicContentService publicContent, PointsService pointsService) {
        this.publicContent = publicContent;
        this.pointsService = pointsService;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("greeting", "网络攻防协会");
        model.addAttribute("announcements", publicContent.announcements().stream().limit(3).toList());
        model.addAttribute("honors", publicContent.honors().stream().limit(3).toList());
        List<Object[]> top = pointsService.ranking("ALL").stream().limit(3).toList();
        model.addAttribute("topPoints", top);
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
