package cn.edu.cust.assoc.controller;

import cn.edu.cust.assoc.domain.AnnouncementEntity;
import cn.edu.cust.assoc.domain.RecruitApplicationEntity;
import cn.edu.cust.assoc.security.CurrentUserService;
import cn.edu.cust.assoc.service.MediaLinkService;
import cn.edu.cust.assoc.service.PublicContentService;
import cn.edu.cust.assoc.service.UserDirectoryService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/public")
public class AdminPublicController {

    private final PublicContentService service;
    private final MediaLinkService mediaLinkService;
    private final UserDirectoryService userDirectory;
    private final CurrentUserService currentUser;

    public AdminPublicController(PublicContentService service, MediaLinkService mediaLinkService,
                                 UserDirectoryService userDirectory, CurrentUserService currentUser) {
        this.service = service;
        this.mediaLinkService = mediaLinkService;
        this.userDirectory = userDirectory;
        this.currentUser = currentUser;
    }

    @GetMapping
    public String index(Model model) {
        model.addAttribute("announcements", service.allAnnouncements());
        model.addAttribute("honors", service.allHonors());
        List<RecruitApplicationEntity> recruits = service.recruitList();
        model.addAttribute("recruits", recruits);

        List<Long> handlers = new ArrayList<>();
        recruits.forEach(r -> {
            if (r.getHandledBy() != null) handlers.add(r.getHandledBy());
        });
        Map<Long, String> handlerNames = userDirectory.nicknames(handlers);
        model.addAttribute("handlerNames", handlerNames);
        return "admin/public";
    }

    @PostMapping("/announcement")
    public String addAnnouncement(@RequestParam String title, @RequestParam String contentMd,
                                  @RequestParam(defaultValue = "false") boolean pinned,
                                  @RequestParam(required = false) Long attachmentMediaId,
                                  Authentication auth, RedirectAttributes redirect) {
        try {
            AnnouncementEntity announcement = service.publishAnnouncement(
                    currentUser.id(auth), title, contentMd, pinned, attachmentMediaId);
            redirect.addFlashAttribute("message", "公告已发布");
        } catch (RuntimeException e) {
            redirect.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/public";
    }

    @PostMapping("/announcement/{id}/takedown")
    public String takedownAnnouncement(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            service.takedownAnnouncement(id);
            mediaLinkService.clearOwner(MediaLinkService.ANNOUNCEMENT_ATTACHMENT, id);
            redirect.addFlashAttribute("message", "公告已下架");
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
        try {
            service.addHonor(title, year, description, sortOrder);
            redirect.addFlashAttribute("message", "荣誉已添加");
        } catch (RuntimeException e) {
            redirect.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/public";
    }

    @PostMapping("/honor/{id}/takedown")
    public String takedownHonor(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            service.takedownHonor(id);
            redirect.addFlashAttribute("message", "荣誉已下架");
        } catch (RuntimeException e) {
            redirect.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/public";
    }

    @PostMapping("/recruit/{id}/state")
    public String recruitState(@PathVariable Long id, @RequestParam String targetState,
                               @RequestParam(required = false) String note,
                               Authentication auth, RedirectAttributes redirect) {
        try {
            service.transitionRecruit(id, targetState, note, currentUser.id(auth));
            redirect.addFlashAttribute("message", "报名状态已更新");
        } catch (RuntimeException e) {
            redirect.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/public";
    }
}
