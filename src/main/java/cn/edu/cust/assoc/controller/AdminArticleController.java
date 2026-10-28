package cn.edu.cust.assoc.controller;

import cn.edu.cust.assoc.domain.ArticleEntity;
import cn.edu.cust.assoc.dto.ArticleAdminRow;
import cn.edu.cust.assoc.security.CurrentUserService;
import cn.edu.cust.assoc.service.ArticleService;
import cn.edu.cust.assoc.service.MediaLinkService;
import cn.edu.cust.assoc.service.UserDirectoryService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/articles")
public class AdminArticleController {

    private final ArticleService articleService;
    private final UserDirectoryService userDirectory;
    private final MediaLinkService mediaLinkService;
    private final CurrentUserService currentUser;

    public AdminArticleController(ArticleService articleService, UserDirectoryService userDirectory,
                                  MediaLinkService mediaLinkService, CurrentUserService currentUser) {
        this.articleService = articleService;
        this.userDirectory = userDirectory;
        this.mediaLinkService = mediaLinkService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public String list(Model model) {
        List<ArticleEntity> pending = articleService.pending();
        List<ArticleEntity> published = articleService.published();
        List<Long> authorIds = new ArrayList<>();
        pending.forEach(a -> authorIds.add(a.getAuthorId()));
        published.forEach(a -> authorIds.add(a.getAuthorId()));
        Map<Long, String> names = userDirectory.nicknames(authorIds);

        model.addAttribute("pending", rows(pending, names));
        model.addAttribute("published", rows(published, names));
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
                         @RequestParam(required = false) String tags,
                         @RequestParam(required = false) Long coverMediaId,
                         Authentication auth,
                         RedirectAttributes redirect) {
        try {
            ArticleEntity article = articleService.create(currentUser.id(auth), title, summary,
                    contentMd, involvesVuln, visibility, parseTags(tags), true);
            if (coverMediaId != null) {
                mediaLinkService.setSingle(MediaLinkService.ARTICLE_COVER, article.getId(), coverMediaId);
            }
            redirect.addFlashAttribute("message", "已提交，等待审核");
            return "redirect:/admin/articles";
        } catch (RuntimeException e) {
            redirect.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/articles/new";
        }
    }

    @PostMapping("/{id}/publish")
    public String publish(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            articleService.publish(id, null);
            redirect.addFlashAttribute("message", "已发布");
        } catch (RuntimeException e) {
            redirect.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/articles";
    }

    @PostMapping("/{id}/reject")
    public String reject(@PathVariable Long id, @RequestParam(required = false) String reason,
                         RedirectAttributes redirect) {
        try {
            articleService.reject(id, reason);
            redirect.addFlashAttribute("message", "已驳回");
        } catch (RuntimeException e) {
            redirect.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/articles";
    }

    @PostMapping("/{id}/offline")
    public String offline(@PathVariable Long id, @RequestParam(required = false) String reason,
                          RedirectAttributes redirect) {
        try {
            articleService.offline(id, reason);
            redirect.addFlashAttribute("message", "已下架");
        } catch (RuntimeException e) {
            redirect.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/articles";
    }

    private List<ArticleAdminRow> rows(List<ArticleEntity> articles, Map<Long, String> names) {
        return articles.stream()
                .map(a -> new ArticleAdminRow(a.getId(), a.getTitle(),
                        names.getOrDefault(a.getAuthorId(), "成员"), a.getVisibility(),
                        Boolean.TRUE.equals(a.getInvolvesVuln()), a.getState(), a.getRejectReason(),
                        a.getCreatedAt()))
                .toList();
    }

    private List<String> parseTags(String tags) {
        if (tags == null || tags.isBlank()) {
            return List.of();
        }
        return Arrays.stream(tags.split("[,，]"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
