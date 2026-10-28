package cn.edu.cust.assoc.controller;

import cn.edu.cust.assoc.repository.UserRepository;
import cn.edu.cust.assoc.service.ArticleService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/admin/articles")
public class AdminArticleController {

    private final ArticleService articleService;
    private final UserRepository userRepository;

    public AdminArticleController(ArticleService articleService, UserRepository userRepository) {
        this.articleService = articleService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("pending", articleService.pending());
        return "admin/articles";
    }

    @GetMapping("/new")
    public String form() {
        return "admin/article-form";
    }

    @PostMapping
    public String create(@RequestParam String title,
                         @RequestParam(required = false) String summary,
                         @RequestParam String contentMd,
                         @RequestParam(defaultValue = "false") boolean involvesVuln,
                         @RequestParam(defaultValue = "INTERNAL") String visibility,
                         Principal principal,
                         RedirectAttributes redirect) {
        try {
            Long authorId = userRepository.findByUsername(principal.getName())
                    .map(u -> u.getId()).orElseThrow();
            articleService.save(authorId, title, summary, contentMd, involvesVuln, visibility);
            redirect.addFlashAttribute("message", "已提交，等待审核");
        } catch (RuntimeException e) {
            redirect.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/articles/new";
        }
        return "redirect:/admin/articles";
    }

    @PostMapping("/{id}/publish")
    public String publish(@PathVariable Long id, RedirectAttributes redirect) {
        articleService.publish(id, null);
        redirect.addFlashAttribute("message", "已发布");
        return "redirect:/admin/articles";
    }

    @PostMapping("/{id}/reject")
    public String reject(@PathVariable Long id, @RequestParam(required = false) String reason,
                         RedirectAttributes redirect) {
        articleService.reject(id, reason);
        redirect.addFlashAttribute("message", "已驳回");
        return "redirect:/admin/articles";
    }
}
