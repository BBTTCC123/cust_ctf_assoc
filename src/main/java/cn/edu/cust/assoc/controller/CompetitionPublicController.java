package cn.edu.cust.assoc.controller;

import cn.edu.cust.assoc.service.CompetitionService;
import cn.edu.cust.assoc.service.CompetitionViewService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class CompetitionPublicController {

    private final CompetitionService competitionService;
    private final CompetitionViewService viewService;

    public CompetitionPublicController(CompetitionService competitionService,
                                       CompetitionViewService viewService) {
        this.competitionService = competitionService;
        this.viewService = viewService;
    }

    @GetMapping("/competitions")
    public String list(@RequestParam(required = false) Short season, Model model) {
        model.addAttribute("competitions", viewService.summaries(season));
        model.addAttribute("seasons", competitionService.seasons());
        model.addAttribute("season", season);
        return "competitions";
    }

    @GetMapping("/competitions/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("competition", viewService.detail(id));
        return "competition-detail";
    }
}
