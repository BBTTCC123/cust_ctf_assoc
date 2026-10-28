package cn.edu.cust.assoc.controller;

import cn.edu.cust.assoc.service.PublicContentService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Controller
public class PublicContentController {

    private final PublicContentService service;

    /** 简单限流：同一 IP 一小时最多提交 5 次。进程内计数，重启即清零。 */
    private final Map<String, long[]> attempts = new ConcurrentHashMap<>();
    private static final int LIMIT = 5;

    public PublicContentController(PublicContentService service) {
        this.service = service;
    }

    @GetMapping("/announcements")
    public String announcements(Model model) {
        model.addAttribute("items", service.announcements());
        return "announcements";
    }

    @GetMapping("/announcements/{id}")
    public String announcement(@PathVariable Long id, Model model) {
        model.addAttribute("a", service.announcement(id));
        return "announcement";
    }

    @GetMapping("/honors")
    public String honors(Model model) {
        model.addAttribute("items", service.honors());
        return "honors";
    }

    @GetMapping("/join")
    public String joinForm() {
        return "join";
    }

    @PostMapping("/join")
    public String join(@RequestParam String name, @RequestParam String studentNo,
                       @RequestParam String college, @RequestParam String major,
                       @RequestParam String contact,
                       @RequestParam(required = false) String message,
                       HttpServletRequest request, RedirectAttributes redirect) {
        String ip = request.getRemoteAddr();
        long now = System.currentTimeMillis();
        long[] slot = attempts.computeIfAbsent(ip, k -> new long[]{now, 0});
        if (now - slot[0] > 3600_000L) {
            slot[0] = now;
            slot[1] = 0;
        }
        if (slot[1] >= LIMIT) {
            redirect.addFlashAttribute("errorMessage", "提交太频繁，过一会儿再试");
            return "redirect:/join";
        }
        try {
            service.recruit(name, studentNo, college, major, contact, message,
                    (short) LocalDate.now().getYear());
            slot[1]++;
            redirect.addFlashAttribute("message", "报名已提交，我们会联系你");
        } catch (RuntimeException e) {
            redirect.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/join";
    }
}
