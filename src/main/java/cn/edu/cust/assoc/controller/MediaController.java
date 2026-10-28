package cn.edu.cust.assoc.controller;

import cn.edu.cust.assoc.domain.MediaEntity;
import cn.edu.cust.assoc.service.MediaService;
import cn.edu.cust.assoc.service.UserDirectoryService;
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
    private final UserDirectoryService userDirectory;

    public MediaController(MediaService mediaService, UserDirectoryService userDirectory) {
        this.mediaService = mediaService;
        this.userDirectory = userDirectory;
    }

    /**
     * 所有文件都从这里出去，没有静态目录直出。
     * 访问矩阵：
     *   APPROVED + PUBLIC -> 所有人
     *   APPROVED + MEMBER -> 登录成员
     *   APPROVED + OWNER  -> 上传人本人（管理员始终可看）
     *   非 APPROVED       -> 只有上传人本人和管理员
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
        boolean authenticated = auth != null && auth.isAuthenticated()
                && !"anonymousUser".equals(auth.getName());
        boolean admin = authenticated && auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_SUPER_ADMIN") || a.equals("ROLE_CONTENT_ADMIN"));

        if (admin) {
            return true;
        }
        if (!"APPROVED".equals(media.getReviewState())) {
            // 未通过审核：只有上传人自己能看
            return authenticated && isOwner(media, auth);
        }
        return switch (media.getVisibility()) {
            case "PUBLIC" -> true;
            case "MEMBER" -> authenticated;
            case "OWNER" -> authenticated && isOwner(media, auth);
            default -> false;
        };
    }

    private boolean isOwner(MediaEntity media, Authentication auth) {
        return userDirectory.findByUsername(auth.getName())
                .map(u -> u.getId().equals(media.getOwnerUserId()))
                .orElse(false);
    }
}
