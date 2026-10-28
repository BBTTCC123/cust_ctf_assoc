package cn.edu.cust.assoc.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("greeting", "网络攻防协会");
        return "index";
    }

    @GetMapping("/about")
    public String about() {
        return "about";
    }
}
