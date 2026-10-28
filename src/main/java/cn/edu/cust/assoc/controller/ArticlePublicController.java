package cn.edu.cust.assoc.controller;

import cn.edu.cust.assoc.domain.ArticleEntity;
import cn.edu.cust.assoc.service.ArticleService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class ArticlePublicController {

    private final ArticleService articleService;

    public ArticlePublicController(ArticleService articleService) {
        this.articleService = articleService;
    }

    @GetMapping("/articles")
    public String list(Model model, Authentication auth) {
        boolean loggedIn = auth != null && auth.isAuthenticated()
                && !"anonymousUser".equals(auth.getName());
        model.addAttribute("articles", loggedIn ? articleService.memberList() : articleService.publicList());
        model.addAttribute("loggedIn", loggedIn);
        return "articles";
    }

    @GetMapping("/articles/{id}")
    public String detail(@PathVariable Long id, Model model, Authentication auth) {
        ArticleEntity article = articleService.get(id);
        boolean loggedIn = auth != null && auth.isAuthenticated()
                && !"anonymousUser".equals(auth.getName());
        if (!"PUBLISHED".equals(article.getState())) {
            model.addAttribute("message", "这篇文章还没发布");
            return "article-missing";
        }
        if (!"PUBLIC".equals(article.getVisibility()) && !loggedIn) {
            model.addAttribute("message", "这篇文章只对站内成员开放，请先登录");
            return "article-missing";
        }
        model.addAttribute("article", article);
        return "article";
    }
}
