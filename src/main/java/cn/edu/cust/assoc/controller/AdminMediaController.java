package cn.edu.cust.assoc.controller;

import cn.edu.cust.assoc.domain.MediaEntity;
import cn.edu.cust.assoc.repository.MediaRepository;
import cn.edu.cust.assoc.service.MediaService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;

@Controller
@RequestMapping("/admin/media")
public class AdminMediaController {

    private final MediaService mediaService;
    private final MediaRepository mediaRepository;

    public AdminMediaController(MediaService mediaService, MediaRepository mediaRepository) {
        this.mediaService = mediaService;
        this.mediaRepository = mediaRepository;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("pending", mediaRepository.findByReviewStateOrderByCreatedAtAsc("PENDING"));
        model.addAttribute("all", mediaRepository.findAll());
        return "admin/media";
    }

    @PostMapping("/upload")
    public String upload(@RequestParam("file") MultipartFile file,
                         Authentication auth,
                         RedirectAttributes redirect) {
        try {
            MediaEntity media = mediaService.upload(currentUserId(auth), file);
            redirect.addFlashAttribute("message", "已上传，等待审核（id=" + media.getId() + "）");
        } catch (IOException | IllegalArgumentException e) {
            redirect.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/media";
    }

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable Long id, RedirectAttributes redirect) {
        mediaService.approve(id);
        redirect.addFlashAttribute("message", "已通过并转入公共区");
        return "redirect:/admin/media";
    }

    @PostMapping("/{id}/reject")
    public String reject(@PathVariable Long id, RedirectAttributes redirect) {
        mediaService.reject(id);
        redirect.addFlashAttribute("message", "已驳回");
        return "redirect:/admin/media";
    }

    private Long currentUserId(Authentication auth) {
        // 1.0 阶段只有超管能进后台，这里先取 1；接入完整用户体系后从 principal 取
        return 1L;
    }
}
