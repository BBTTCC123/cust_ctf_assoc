package cn.edu.cust.assoc.controller;

import cn.edu.cust.assoc.dto.MediaRow;
import cn.edu.cust.assoc.security.CurrentUserService;
import cn.edu.cust.assoc.service.MediaAdminViewService;
import cn.edu.cust.assoc.service.MediaService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.List;

@Controller
@RequestMapping("/admin/media")
public class AdminMediaController {

    private final MediaService mediaService;
    private final MediaAdminViewService viewService;
    private final CurrentUserService currentUser;

    public AdminMediaController(MediaService mediaService, MediaAdminViewService viewService,
                                CurrentUserService currentUser) {
        this.mediaService = mediaService;
        this.viewService = viewService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public String list(Model model) {
        List<MediaRow> pending = viewService.pending();
        model.addAttribute("pending", pending);
        model.addAttribute("all", viewService.all());
        return "admin/media";
    }

    @PostMapping("/upload")
    public String upload(@RequestParam("file") MultipartFile file,
                         Authentication auth, RedirectAttributes redirect) {
        try {
            var media = mediaService.upload(currentUser.id(auth), file);
            redirect.addFlashAttribute("message", "已上传，等待审核（id=" + media.getId() + "）");
        } catch (IOException e) {
            redirect.addFlashAttribute("errorMessage", "文件保存失败，请稍后重试");
        } catch (RuntimeException e) {
            redirect.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/media";
    }

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable Long id,
                          @RequestParam(defaultValue = "MEMBER") String visibility,
                          RedirectAttributes redirect) {
        try {
            mediaService.approve(id, visibility);
            redirect.addFlashAttribute("message", "已通过审核（可见范围：" + visibility + "）");
        } catch (RuntimeException e) {
            redirect.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/media";
    }

    @PostMapping("/{id}/reject")
    public String reject(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            mediaService.reject(id);
            redirect.addFlashAttribute("message", "已驳回");
        } catch (RuntimeException e) {
            redirect.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/media";
    }

    @PostMapping("/{id}/revoke")
    public String revoke(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            mediaService.revoke(id);
            redirect.addFlashAttribute("message", "已下架");
        } catch (RuntimeException e) {
            redirect.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/media";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            mediaService.delete(id);
            redirect.addFlashAttribute("message", "已删除");
        } catch (RuntimeException e) {
            redirect.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/media";
    }
}
