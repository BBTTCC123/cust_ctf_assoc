package cn.edu.cust.assoc.controller;

import cn.edu.cust.assoc.domain.CompetitionEntity;
import cn.edu.cust.assoc.domain.Competitions;
import cn.edu.cust.assoc.dto.CompetitionForm;
import cn.edu.cust.assoc.service.CompetitionService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/competitions")
public class AdminCompetitionController {

    private final CompetitionService competitionService;

    public AdminCompetitionController(CompetitionService competitionService) {
        this.competitionService = competitionService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("competitions", competitionService.listAll());
        return "admin/competition-list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("form", new CompetitionForm());
        model.addAttribute("editing", false);
        addOptions(model);
        return "admin/competition-form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("form", competitionService.toForm(competitionService.get(id)));
        model.addAttribute("editing", true);
        model.addAttribute("id", id);
        addOptions(model);
        return "admin/competition-form";
    }

    @PostMapping
    public String save(@Valid @ModelAttribute("form") CompetitionForm form,
                       BindingResult binding,
                       @RequestParam(required = false) Long id,
                       Model model,
                       RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            model.addAttribute("editing", id != null);
            model.addAttribute("id", id);
            addOptions(model);
            return "admin/competition-form";
        }
        try {
            if (id == null) {
                competitionService.create(form);
            } else {
                competitionService.update(id, form);
            }
        } catch (IllegalArgumentException e) {
            model.addAttribute("editing", id != null);
            model.addAttribute("id", id);
            model.addAttribute("errorMessage", e.getMessage());
            addOptions(model);
            return "admin/competition-form";
        }
        redirect.addFlashAttribute("message", "已保存");
        return "redirect:/admin/competitions";
    }

    /** 级别和类别的可选项。硬编码在模板里会漂，这里统一从枚举取。 */
    private void addOptions(Model model) {
        model.addAttribute("levels", Competitions.Level.values());
        model.addAttribute("categories", Competitions.Category.values());
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirect) {
        competitionService.delete(id);
        redirect.addFlashAttribute("message", "已删除");
        return "redirect:/admin/competitions";
    }
}