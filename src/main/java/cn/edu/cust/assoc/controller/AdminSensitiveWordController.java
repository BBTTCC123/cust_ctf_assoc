package cn.edu.cust.assoc.controller;

import cn.edu.cust.assoc.dto.SensitiveWordRow;
import cn.edu.cust.assoc.service.SensitiveWordService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/sensitive-words")
public class AdminSensitiveWordController {

    private final SensitiveWordService sensitiveWordService;

    public AdminSensitiveWordController(SensitiveWordService sensitiveWordService) {
        this.sensitiveWordService = sensitiveWordService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("words", sensitiveWordService.all().stream()
                .map(w -> new SensitiveWordRow(w.getId(), w.getWord(), w.getLevel(),
                        Boolean.TRUE.equals(w.getEnabled())))
                .toList());
        return "admin/sensitive-words";
    }

    @PostMapping
    public String add(@RequestParam String word, @RequestParam String level,
                      RedirectAttributes redirect) {
        try {
            sensitiveWordService.add(word, level);
            redirect.addFlashAttribute("message", "已添加，立即生效");
        } catch (RuntimeException e) {
            redirect.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/sensitive-words";
    }

    @PostMapping("/{id}/update")
    public String update(@PathVariable Long id, @RequestParam String word,
                         @RequestParam String level,
                         @RequestParam(defaultValue = "false") boolean enabled,
                         RedirectAttributes redirect) {
        try {
            sensitiveWordService.update(id, word, level, enabled);
            redirect.addFlashAttribute("message", "已保存，立即生效");
        } catch (RuntimeException e) {
            redirect.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/sensitive-words";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            sensitiveWordService.delete(id);
            redirect.addFlashAttribute("message", "已删除");
        } catch (RuntimeException e) {
            redirect.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/sensitive-words";
    }

    @PostMapping("/import")
    public String importWords(@RequestParam String text, RedirectAttributes redirect) {
        try {
            redirect.addFlashAttribute("message", sensitiveWordService.importLines(text));
        } catch (RuntimeException e) {
            redirect.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/sensitive-words";
    }
}
