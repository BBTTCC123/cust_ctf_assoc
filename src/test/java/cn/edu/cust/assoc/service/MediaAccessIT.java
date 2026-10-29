package cn.edu.cust.assoc.service;

import cn.edu.cust.assoc.common.BizException;
import cn.edu.cust.assoc.domain.MediaEntity;
import cn.edu.cust.assoc.domain.UserEntity;
import cn.edu.cust.assoc.repository.MediaRepository;
import cn.edu.cust.assoc.support.IntegrationTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 媒体两阶段审核与三档可见性（ISS-016 / ISS-020 / ISS-027）。
 *
 * 红线：未审核的文件不进公共区；审核顺序必须是「复制到公共区 → 数据库提交 → 清理私有区」。
 * 可见性矩阵：PUBLIC 所有人、MEMBER 登录成员、OWNER 仅上传人，管理员始终可看。
 *
 * 这个类故意不加 @Transactional：审核提交后要观察私有区副本是否被清理，
 * 外层测试事务会让 afterCommit 永远不触发。数据由 @AfterEach 手工清掉。
 */
@EnabledIf("cn.edu.cust.assoc.support.TestDatabase#isAvailable")
class MediaAccessIT extends IntegrationTestBase {

    private static final String PASSWORD = "ItPass!2026abc";

    @Autowired
    private MediaService mediaService;

    @Autowired
    private MediaRepository mediaRepository;

    @Autowired
    private LocalStorageService storage;

    private UserEntity owner;
    private UserEntity otherMember;
    private UserEntity contentAdmin;

    private MockHttpSession ownerSession;
    private MockHttpSession otherSession;
    private MockHttpSession adminSession;

    private final List<Long> mediaIds = new ArrayList<>();
    private final List<Long> userIds = new ArrayList<>();

    @BeforeEach
    void setUpAccounts() throws Exception {
        owner = createActiveUser("mediaowner", PASSWORD, "USER");
        otherMember = createActiveUser("mediaother", PASSWORD, "USER");
        contentAdmin = createActiveUser("mediacontent", PASSWORD, "CONTENT_ADMIN");
        userIds.add(owner.getId());
        userIds.add(otherMember.getId());
        userIds.add(contentAdmin.getId());

        ownerSession = login(owner.getUsername(), PASSWORD);
        otherSession = login(otherMember.getUsername(), PASSWORD);
        adminSession = login(contentAdmin.getUsername(), PASSWORD);
    }

    @AfterEach
    void cleanUp() {
        for (Long id : mediaIds) {
            mediaRepository.findById(id).ifPresent(media -> {
                deleteQuietly("public", media.getStoredKey());
                deleteQuietly("public", media.getThumbKey());
                deleteQuietly("private", media.getStoredKey());
                deleteQuietly("private", media.getThumbKey());
                mediaRepository.delete(media);
            });
        }
        mediaIds.clear();
        userRepository.deleteAllById(userIds);
        userIds.clear();
    }

    @Test
    void pendingMediaStaysPrivateAndOnlyOwnerOrAdminCanReadIt() throws Exception {
        MediaEntity media = upload(owner);

        assertThat(media.getZone()).isEqualTo("PRIVATE");
        assertThat(media.getReviewState()).isEqualTo("PENDING");
        assertThat(storage.read("private", media.getStoredKey())).isNotEmpty();

        mockMvc.perform(get("/media/{id}", media.getId()))
                .andExpect(status().isForbidden());
        getAs(otherSession, "/media/" + media.getId()).andExpect(status().isForbidden());
        getAs(ownerSession, "/media/" + media.getId()).andExpect(status().isOk());
        getAs(adminSession, "/media/" + media.getId()).andExpect(status().isOk());
    }

    @Test
    void approvingMovesFilesToPublicZoneAndAppliesVisibilityMatrix() throws Exception {
        MediaEntity media = upload(owner);
        String storedKey = media.getStoredKey();
        String thumbKey = media.getThumbKey();
        assertThat(thumbKey).isNotNull();

        mediaService.approve(media.getId(), "PUBLIC");

        MediaEntity approved = mediaRepository.findById(media.getId()).orElseThrow();
        assertThat(approved.getReviewState()).isEqualTo("APPROVED");
        assertThat(approved.getVisibility()).isEqualTo("PUBLIC");
        assertThat(approved.getZone()).isEqualTo("PUBLIC");

        // 两阶段：公共区可读，私有区副本在事务提交后被清理
        assertThat(storage.read("public", storedKey)).isNotEmpty();
        assertThat(storage.read("public", thumbKey)).isNotEmpty();
        assertThatThrownBy(() -> storage.read("private", storedKey))
                .isInstanceOf(NoSuchFileException.class);
        assertThatThrownBy(() -> storage.read("private", thumbKey))
                .isInstanceOf(NoSuchFileException.class);

        mockMvc.perform(get("/media/{id}", media.getId()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/png"))
                .andExpect(header().string("Cache-Control", containsString("public")));
        getAs(otherSession, "/media/" + media.getId()).andExpect(status().isOk());

        // 重复审核只调整可见范围，不再搬文件：改成 MEMBER 后匿名不可见、成员可见
        mediaService.approve(media.getId(), "MEMBER");
        mockMvc.perform(get("/media/{id}", media.getId()))
                .andExpect(status().isForbidden());
        getAs(otherSession, "/media/" + media.getId())
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("private")));

        // OWNER：除上传人和管理员外，其他成员也不可见
        mediaService.approve(media.getId(), "OWNER");
        getAs(otherSession, "/media/" + media.getId()).andExpect(status().isForbidden());
        getAs(ownerSession, "/media/" + media.getId()).andExpect(status().isOk());
        getAs(adminSession, "/media/" + media.getId()).andExpect(status().isOk());
    }

    @Test
    void rejectedMediaCannotBeApprovedAndRemainsOwnerOnly() throws Exception {
        MediaEntity media = upload(owner);
        mediaService.reject(media.getId());

        MediaEntity rejected = mediaRepository.findById(media.getId()).orElseThrow();
        assertThat(rejected.getReviewState()).isEqualTo("REJECTED");
        assertThat(rejected.getZone()).isEqualTo("PRIVATE");

        getAs(ownerSession, "/media/" + media.getId()).andExpect(status().isOk());
        getAs(otherSession, "/media/" + media.getId()).andExpect(status().isForbidden());
        mockMvc.perform(get("/media/{id}", media.getId())).andExpect(status().isForbidden());

        assertThatThrownBy(() -> mediaService.approve(media.getId(), "PUBLIC"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("只有待审文件");
    }

    @Test
    void revokedMediaIsNoLongerPublicButOwnerAndAdminCanStillTraceIt() throws Exception {
        MediaEntity media = upload(owner);
        mediaService.approve(media.getId(), "PUBLIC");
        mediaService.revoke(media.getId());

        MediaEntity revoked = mediaRepository.findById(media.getId()).orElseThrow();
        assertThat(revoked.getReviewState()).isEqualTo("REVOKED");
        assertThat(revoked.getZone()).isEqualTo("PUBLIC");

        mockMvc.perform(get("/media/{id}", media.getId())).andExpect(status().isForbidden());
        getAs(otherSession, "/media/" + media.getId()).andExpect(status().isForbidden());
        getAs(ownerSession, "/media/" + media.getId()).andExpect(status().isOk());
        getAs(adminSession, "/media/" + media.getId()).andExpect(status().isOk());

        assertThatThrownBy(() -> mediaService.revoke(media.getId()))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("只有已通过的文件");
    }

    @Test
    void approveRejectsUnknownVisibility() throws Exception {
        MediaEntity media = upload(owner);

        assertThatThrownBy(() -> mediaService.approve(media.getId(), "EVERYONE"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("未知的可见范围");
        assertThat(mediaRepository.findById(media.getId()).orElseThrow().getReviewState()).isEqualTo("PENDING");
    }

    private MediaEntity upload(UserEntity uploader) throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "it-media.png", "image/png", pngBytes());
        MediaEntity media = mediaService.upload(uploader.getId(), file);
        mediaIds.add(media.getId());
        return media;
    }

    private byte[] pngBytes() throws IOException {
        BufferedImage image = new BufferedImage(40, 30, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.BLUE);
        graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        graphics.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }

    private void deleteQuietly(String zone, String key) {
        if (key == null) {
            return;
        }
        try {
            storage.delete(zone, key);
        } catch (IOException ignored) {
            // 清理失败只留下孤儿文件，不影响断言
        }
    }
}
