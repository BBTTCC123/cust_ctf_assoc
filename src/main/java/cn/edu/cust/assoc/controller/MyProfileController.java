package cn.edu.cust.assoc.controller;

import cn.edu.cust.assoc.common.BizException;
import cn.edu.cust.assoc.dto.MemberProfileForm;
import cn.edu.cust.assoc.security.CurrentUserService;
import cn.edu.cust.assoc.service.MemberProfileService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** 成员资料与隐私开关。未上线的战队空间、内部工具、积分兑换不在导航展示。 */
@Controller
@RequestMapping("/me/profile")
public class MyProfileController {

    private final MemberProfileService profileService;
    private final CurrentUserService currentUser;

    public MyProfileController(MemberProfileService profileService, CurrentUserService currentUser) {
        this.profileService = profileService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public String form(Model model, Authentication auth) {
        model.addAttribute("form", profileService.form(currentUser.id(auth)));
        return "me/profile";
    }

    @PostMapping
    public String save(@ModelAttribute("form") MemberProfileForm form, Authentication auth,
                       RedirectAttributes redirect) {
        try {
            profileService.save(currentUser.id(auth), form);
            redirect.addFlashAttribute("message", "资料已保存，公开页按隐私开关展示");
        } catch (BizException e) {
            redirect.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/me/profile";
    }
}
