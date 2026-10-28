package cn.edu.cust.assoc.controller;

import cn.edu.cust.assoc.repository.UserRepository;
import cn.edu.cust.assoc.service.PublicContentService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/admin/public")
public class AdminPublicController {

    private final PublicContentService service;
    private final UserRepository userRepository;

    public AdminPublicController(PublicContentService service, UserRepository userRepository) {
        this.service = service;
        this.userRepository = userRepository;
    }

    @GetMapping
    public String index(Model model) {
        model.addAttribute("announcements", service.announcements());
        model.addAttribute("recruits", service.recruitList());
        return "admin/public";
    }

    @PostMapping("/announcement")
    public String addAnnouncement(@RequestParam String title, @RequestParam String contentMd,
                                  @RequestParam(defaultValue = "false") boolean pinned,
                                  Principal principal, RedirectAttributes redirect) {
        try {
            Long authorId = userRepository.findByUsername(principal.getName()).map(u -> u.getId()).orElseThrow();
            service.publishAnnouncement(authorId, title, contentMd, pinned);
            redirect.addFlashAttribute("message", "公告已发布");
        } catch (RuntimeException e) {
            redirect.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/public";
    }

    @PostMapping("/honor")
    public String addHonor(@RequestParam String title, @RequestParam Short year,
                           @RequestParam(required = false) String description,
                           @RequestParam(required = false) Integer sortOrder,
                           RedirectAttributes redirect) {
        service.addHonor(title, year, description, sortOrder);
        redirect.addFlashAttribute("message", "荣誉已添加");
        return "redirect:/admin/public";
    }
}
