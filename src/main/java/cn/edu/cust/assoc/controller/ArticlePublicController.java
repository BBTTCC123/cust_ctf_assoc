package cn.edu.cust.assoc.controller;

import cn.edu.cust.assoc.dto.ArticleCard;
import cn.edu.cust.assoc.dto.ArticleDetail;
import cn.edu.cust.assoc.service.ArticleService;
import cn.edu.cust.assoc.service.ArticleViewService;
import cn.edu.cust.assoc.service.CommentService;
import cn.edu.cust.assoc.service.FavoriteService;
import cn.edu.cust.assoc.security.CurrentUserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ArticlePublicController {

    private final ArticleService articleService;
    private final ArticleViewService articleViewService;
    private final CommentService commentService;
    private final FavoriteService favoriteService;
    private final CurrentUserService currentUser;

    public ArticlePublicController(ArticleService articleService, ArticleViewService articleViewService,
                                   CommentService commentService, FavoriteService favoriteService,
                                   CurrentUserService currentUser) {
        this.articleService = articleService;
        this.articleViewService = articleViewService;
        this.commentService = commentService;
        this.favoriteService = favoriteService;
        this.currentUser = currentUser;
    }

    @GetMapping("/articles")
    public String list(@RequestParam(required = false) String q,
                       @RequestParam(required = false) String tag,
                       @RequestParam(defaultValue = "0") int page,
                       Model model, Authentication auth) {
        boolean loggedIn = isLoggedIn(auth);
        Pageable pageable = PageRequest.of(Math.max(0, page), 10,
                Sort.by(Sort.Direction.DESC, "publishedAt"));
        Page<ArticleCard> cards = articleViewService.cards(articleService.search(loggedIn, q, tag, pageable));
        model.addAttribute("articles", cards);
        model.addAttribute("tags", articleService.allTags());
        model.addAttribute("q", q);
        model.addAttribute("tag", tag);
        model.addAttribute("loggedIn", loggedIn);
        return "articles";
    }

    @GetMapping("/articles/{id}")
    public String detail(@PathVariable Long id, Model model, Authentication auth) {
        boolean loggedIn = isLoggedIn(auth);
        Long viewerId = loggedIn ? currentUser.id(auth) : null;
        ArticleDetail article = articleViewService.detail(id, viewerId);
        if (!"PUBLISHED".equals(article.state())) {
            model.addAttribute("message", "这篇文章还没发布");
            return "article-missing";
        }
        if (!"PUBLIC".equals(article.visibility()) && !loggedIn) {
            model.addAttribute("message", "这篇文章只对站内成员开放，请先登录");
            return "article-missing";
        }
        model.addAttribute("article", article);
        model.addAttribute("loggedIn", loggedIn);
        return "article";
    }

    @PostMapping("/articles/{id}/comments")
    public String comment(@PathVariable Long id, @RequestParam String content,
                          Authentication auth, RedirectAttributes redirect) {
        try {
            commentService.add(id, currentUser.id(auth), content);
            redirect.addFlashAttribute("message", "评论已提交，审核通过后展示");
        } catch (RuntimeException e) {
            redirect.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/articles/" + id;
    }

    @PostMapping("/articles/{id}/favorite")
    public String favorite(@PathVariable Long id, Authentication auth, RedirectAttributes redirect) {
        boolean favorited = favoriteService.toggle(currentUser.id(auth), id);
        redirect.addFlashAttribute("message", favorited ? "已收藏" : "已取消收藏");
        return "redirect:/articles/" + id;
    }

    private boolean isLoggedIn(Authentication auth) {
        return auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName());
    }
}
