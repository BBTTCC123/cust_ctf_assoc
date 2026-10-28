package cn.edu.cust.assoc.controller;

import cn.edu.cust.assoc.domain.CommentEntity;
import cn.edu.cust.assoc.dto.PendingCommentRow;
import cn.edu.cust.assoc.security.CurrentUserService;
import cn.edu.cust.assoc.service.ArticleService;
import cn.edu.cust.assoc.service.CommentService;
import cn.edu.cust.assoc.service.UserDirectoryService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/comments")
public class AdminCommentController {

    private final CommentService commentService;
    private final ArticleService articleService;
    private final UserDirectoryService userDirectory;
    private final CurrentUserService currentUser;

    public AdminCommentController(CommentService commentService, ArticleService articleService,
                                  UserDirectoryService userDirectory, CurrentUserService currentUser) {
        this.commentService = commentService;
        this.articleService = articleService;
        this.userDirectory = userDirectory;
        this.currentUser = currentUser;
    }

    @GetMapping
    public String list(Model model) {
        List<CommentEntity> pending = commentService.pending();
        Map<Long, String> names = userDirectory.nicknames(
                pending.stream().map(CommentEntity::getUserId).toList());
        Map<Long, String> titles = articleService.titlesOf(
                pending.stream().map(CommentEntity::getArticleId).distinct().toList());
        List<PendingCommentRow> rows = pending.stream()
                .map(c -> new PendingCommentRow(c.getId(), c.getArticleId(),
                        titles.getOrDefault(c.getArticleId(), "文章 #" + c.getArticleId()),
                        names.getOrDefault(c.getUserId(), "成员"), c.getContent(), c.getCreatedAt()))
                .toList();
        model.addAttribute("comments", rows);
        return "admin/comments";
    }

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable Long id, Authentication auth, RedirectAttributes redirect) {
        try {
            commentService.approve(id, currentUser.id(auth));
            redirect.addFlashAttribute("message", "评论已通过");
        } catch (RuntimeException e) {
            redirect.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/comments";
    }

    @PostMapping("/{id}/reject")
    public String reject(@PathVariable Long id, Authentication auth, RedirectAttributes redirect) {
        try {
            commentService.reject(id, currentUser.id(auth));
            redirect.addFlashAttribute("message", "评论已驳回");
        } catch (RuntimeException e) {
            redirect.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/comments";
    }
}
