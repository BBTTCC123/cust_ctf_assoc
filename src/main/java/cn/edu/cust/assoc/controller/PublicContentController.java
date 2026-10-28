package cn.edu.cust.assoc.controller;

import cn.edu.cust.assoc.common.BizException;
import cn.edu.cust.assoc.common.RequestIds;
import cn.edu.cust.assoc.service.MediaLinkService;
import cn.edu.cust.assoc.service.PublicContentService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Controller
public class PublicContentController {

    private static final Logger log = LoggerFactory.getLogger(PublicContentController.class);
    private static final int LIMIT = 5;
    private static final long WINDOW_MS = 3600_000L;

    private final PublicContentService service;
    private final MediaLinkService mediaLinkService;

    /** 进程内限流：同一真实 IP 与同一学号各限 5 次/小时。多实例部署需换 Redis。 */
    private final Map<String, long[]> ipAttempts = new ConcurrentHashMap<>();
    private final Map<String, long[]> studentAttempts = new ConcurrentHashMap<>();

    public PublicContentController(PublicContentService service, MediaLinkService mediaLinkService) {
        this.service = service;
        this.mediaLinkService = mediaLinkService;
    }

    @GetMapping("/announcements")
    public String announcements(Model model) {
        model.addAttribute("items", service.announcements());
        return "announcements";
    }

    @GetMapping("/announcements/{id}")
    public String announcement(@PathVariable Long id, Model model) {
        model.addAttribute("a", service.announcement(id));
        model.addAttribute("attachments", mediaLinkService
                .mediaOf(MediaLinkService.ANNOUNCEMENT_ATTACHMENT, id).stream()
                .map(m -> new cn.edu.cust.assoc.dto.AttachmentRow(
                        m.getId(), m.getOriginalName(), m.getMime()))
                .toList());
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
        if (overLimit(ipAttempts, ip) || overLimit(studentAttempts, studentNo)) {
            redirect.addFlashAttribute("errorMessage", "提交太频繁，过一会儿再试");
            return "redirect:/join";
        }
        try {
            service.recruit(name, studentNo, college, major, contact, message,
                    (short) LocalDate.now().getYear());
            record(ipAttempts, ip);
            record(studentAttempts, studentNo);
            redirect.addFlashAttribute("message", "报名已提交，我们会联系你");
        } catch (BizException e) {
            redirect.addFlashAttribute("errorMessage", e.getMessage());
        } catch (RuntimeException e) {
            // 数据库异常只记日志：页面不暴露表名、字段和约束
            String requestId = RequestIds.getOrCreate(request);
            log.error("招新报名失败 requestId={}", requestId, e);
            redirect.addFlashAttribute("errorMessage", "提交失败，请稍后再试（错误编号 " + requestId + "）");
        }
        return "redirect:/join";
    }

    private boolean overLimit(Map<String, long[]> map, String key) {
        if (key == null || key.isBlank()) return false;
        long now = System.currentTimeMillis();
        if (map.size() > 10_000) {
            map.entrySet().removeIf(e -> now - e.getValue()[0] > WINDOW_MS);
        }
        long[] slot = map.compute(key, (k, old) ->
                (old == null || now - old[0] > WINDOW_MS) ? new long[]{now, 0} : old);
        synchronized (slot) {
            return slot[1] >= LIMIT;
        }
    }

    private void record(Map<String, long[]> map, String key) {
        if (key == null || key.isBlank()) return;
        long[] slot = map.get(key);
        if (slot == null) return;
        synchronized (slot) {
            slot[1]++;
        }
    }
}
