package cn.edu.cust.assoc.controller;

import cn.edu.cust.assoc.common.BizException;
import cn.edu.cust.assoc.dto.ArticleEditView;
import cn.edu.cust.assoc.dto.MyArticleRow;
import cn.edu.cust.assoc.domain.ArticleEntity;
import cn.edu.cust.assoc.security.CurrentUserService;
import cn.edu.cust.assoc.service.ArticleService;
import cn.edu.cust.assoc.service.MediaLinkService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.List;

/** 成员投稿：草稿、编辑、提交审核、查看驳回原因。 */
@Controller
@RequestMapping("/me/articles")
public class MyArticleController {

    private final ArticleService articleService;
    private final MediaLinkService mediaLinkService;
    private final CurrentUserService currentUser;

    public MyArticleController(ArticleService articleService, MediaLinkService mediaLinkService,
                               CurrentUserService currentUser) {
        this.articleService = articleService;
        this.mediaLinkService = mediaLinkService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public String list(Model model, Authentication auth) {
        Long userId = currentUser.id(auth);
        List<MyArticleRow> rows = articleService.mine(userId).stream()
                .map(a -> new MyArticleRow(a.getId(), a.getTitle(), a.getState(), a.getVisibility(),
                        Boolean.TRUE.equals(a.getInvolvesVuln()), a.getRejectReason(), a.getUpdatedAt()))
                .toList();
        model.addAttribute("articles", rows);
        return "me/articles";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("editing", false);
        model.addAttribute("formAction", "/me/articles");
        return "me/article-form";
    }

    @PostMapping
    public String create(@RequestParam String title,
                         @RequestParam(required = false) String summary,
                         @RequestParam String contentMd,
                         @RequestParam(defaultValue = "false") boolean involvesVuln,
                         @RequestParam(defaultValue = "INTERNAL") String visibility,
                         @RequestParam(required = false) String tags,
                         @RequestParam(required = false) Long coverMediaId,
                         @RequestParam(defaultValue = "false") boolean submit,
                         Authentication auth, RedirectAttributes redirect) {
        try {
            ArticleEntity article = articleService.create(currentUser.id(auth), title, summary,
                    contentMd, involvesVuln, visibility, parseTags(tags), coverMediaId, submit);
            redirect.addFlashAttribute("message", submit ? "已提交审核" : "草稿已保存");
            return submit ? "redirect:/me/articles" : "redirect:/me/articles/" + article.getId() + "/edit";
        } catch (BizException e) {
            redirect.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/me/articles/new";
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model, Authentication auth) {
        ArticleEntity article = articleService.getOwn(currentUser.id(auth), id);
        ArticleEditView view = new ArticleEditView(article.getId(), article.getTitle(),
                article.getSummary(), article.getContentMd(), Boolean.TRUE.equals(article.getInvolvesVuln()),
                article.getVisibility(), article.getState(), article.getRejectReason(),
                mediaLinkService.singleOf(MediaLinkService.ARTICLE_COVER, id) == null ? null
                        : mediaLinkService.singleOf(MediaLinkService.ARTICLE_COVER, id).getId(),
                articleService.tagsOf(id).stream().map(t -> t.getTag()).toList());
        model.addAttribute("editing", true);
        model.addAttribute("formAction", "/me/articles/" + id);
        model.addAttribute("article", view);
        return "me/article-form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @RequestParam String title,
                         @RequestParam(required = false) String summary,
                         @RequestParam String contentMd,
                         @RequestParam(defaultValue = "false") boolean involvesVuln,
                         @RequestParam(defaultValue = "INTERNAL") String visibility,
                         @RequestParam(required = false) String tags,
                         @RequestParam(required = false) Long coverMediaId,
                         Authentication auth, RedirectAttributes redirect) {
        try {
            articleService.updateOwn(currentUser.id(auth), id, title, summary, contentMd,
                    involvesVuln, visibility, parseTags(tags), coverMediaId);
            redirect.addFlashAttribute("message", "已保存为草稿");
            return "redirect:/me/articles";
        } catch (BizException e) {
            redirect.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/me/articles/" + id + "/edit";
        }
    }

    @PostMapping("/{id}/submit")
    public String submit(@PathVariable Long id, Authentication auth, RedirectAttributes redirect) {
        try {
            articleService.submitOwn(currentUser.id(auth), id);
            redirect.addFlashAttribute("message", "已提交审核");
        } catch (BizException e) {
            redirect.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/me/articles";
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
