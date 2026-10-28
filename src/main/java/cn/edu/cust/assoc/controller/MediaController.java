package cn.edu.cust.assoc.controller;

import cn.edu.cust.assoc.domain.MediaEntity;
import cn.edu.cust.assoc.repository.UserRepository;
import cn.edu.cust.assoc.service.MediaService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.io.IOException;

@Controller
public class MediaController {

    private final MediaService mediaService;
    private final UserRepository userRepository;

    public MediaController(MediaService mediaService, UserRepository userRepository) {
        this.mediaService = mediaService;
        this.userRepository = userRepository;
    }

    /**
     * 所有文件都从这里出去，没有静态目录直出。
     * 规则：本人或管理员任何时候能看；其他人只能看「已审核 + 公开 + 已进公共区」的。
     */
    @GetMapping("/media/{id}")
    public ResponseEntity<byte[]> get(@PathVariable Long id, Authentication auth) throws IOException {
        MediaEntity media = mediaService.get(id);
        if (!canView(media, auth)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(media.getMime()))
                .header("Cache-Control", media.isPubliclyViewable() ? "public, max-age=86400" : "private, no-store")
                .body(mediaService.content(media));
    }

    private boolean canView(MediaEntity media, Authentication auth) {
        if (media.isPubliclyViewable()) {
            return true;
        }
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        boolean admin = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_SUPER_ADMIN") || a.equals("ROLE_CONTENT_ADMIN"));
        if (admin) {
            return true;
        }
        return userRepository.findByUsername(auth.getName())
                .map(u -> u.getId().equals(media.getOwnerUserId()))
                .orElse(false);
    }
}