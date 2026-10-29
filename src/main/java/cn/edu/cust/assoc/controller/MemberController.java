package cn.edu.cust.assoc.controller;

import cn.edu.cust.assoc.security.CurrentUserService;
import cn.edu.cust.assoc.service.MemberService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class MemberController {

    private final MemberService memberService;
    private final CurrentUserService currentUser;

    public MemberController(MemberService memberService, CurrentUserService currentUser) {
        this.memberService = memberService;
        this.currentUser = currentUser;
    }

    /** 公开成员积分页：只展示昵称、每笔来源和分数；佐证与内部备注不公开。 */
    @GetMapping("/members/{id}")
    public String points(@PathVariable Long id, Model model, Authentication auth) {
        boolean loggedIn = auth != null && auth.isAuthenticated()
                && !"anonymousUser".equals(auth.getName());
        Long viewerId = loggedIn ? currentUser.id(auth) : null;
        boolean privileged = loggedIn && auth.getAuthorities().stream()
                .anyMatch(a -> switch (a.getAuthority()) {
                    case "ROLE_SUPER_ADMIN", "ROLE_POINTS_ADMIN", "ROLE_TEACHER" -> true;
                    default -> false;
                });
        model.addAttribute("member", memberService.view(id, viewerId, privileged));
        return "member";
    }
}
