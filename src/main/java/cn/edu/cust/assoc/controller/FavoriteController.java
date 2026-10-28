package cn.edu.cust.assoc.controller;

import cn.edu.cust.assoc.dto.FavoriteRow;
import cn.edu.cust.assoc.security.CurrentUserService;
import cn.edu.cust.assoc.service.FavoriteService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class FavoriteController {

    private final FavoriteService favoriteService;
    private final CurrentUserService currentUser;

    public FavoriteController(FavoriteService favoriteService, CurrentUserService currentUser) {
        this.favoriteService = favoriteService;
        this.currentUser = currentUser;
    }

    @GetMapping("/me/favorites")
    public String list(Model model, Authentication auth) {
        List<FavoriteRow> rows = favoriteService.articlesOf(currentUser.id(auth)).stream()
                .map(a -> new FavoriteRow(a.getId(), a.getTitle(), a.getVisibility()))
                .toList();
        model.addAttribute("favorites", rows);
        return "me/favorites";
    }
}
