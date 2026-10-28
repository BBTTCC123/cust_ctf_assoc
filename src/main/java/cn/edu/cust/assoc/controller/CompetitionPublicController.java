package cn.edu.cust.assoc.controller;

import cn.edu.cust.assoc.service.CompetitionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class CompetitionPublicController {

    private final CompetitionService competitionService;

    public CompetitionPublicController(CompetitionService competitionService) {
        this.competitionService = competitionService;
    }

    @GetMapping("/competitions")
    public String list(Model model) {
        model.addAttribute("competitions", competitionService.listAll());
        return "competitions";
    }
}
