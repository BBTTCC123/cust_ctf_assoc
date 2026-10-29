package cn.edu.cust.assoc.security;

import cn.edu.cust.assoc.domain.UserEntity;
import cn.edu.cust.assoc.support.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 四类后台角色的 URL 授权矩阵（ISS-002）。
 *
 * 矩阵来自 SecurityConfig：账号域只给超管，积分域给超管+积分管理员，
 * 内容域给超管+内容管理员，宣传域给超管+宣传管理员；普通成员只能进 /me。
 * 这些错配不会当场报错，只会在越权时静默放行，所以必须逐条回归。
 */
@Transactional
@EnabledIf("cn.edu.cust.assoc.support.TestDatabase#isAvailable")
class PermissionMatrixIT extends IntegrationTestBase {

    private static final String PASSWORD = "ItPass!2026abc";

    private static final List<String> ALL_ADMIN_PAGES = List.of(
            "/admin",
            "/admin/users",
            "/admin/audit",
            "/admin/teams",
            "/admin/points",
            "/admin/rewards",
            "/admin/appeals",
            "/admin/competitions",
            "/admin/articles",
            "/admin/media",
            "/admin/comments",
            "/admin/sensitive-words",
            "/admin/public");

    private static final List<String> PUBLIC_PAGES = List.of(
            "/", "/about", "/competitions", "/points", "/articles",
            "/honors", "/announcements", "/join", "/register", "/login");

    private UserEntity member;
    private UserEntity contentAdmin;
    private UserEntity pointsAdmin;
    private UserEntity prAdmin;
    private UserEntity superAdmin;

    @BeforeEach
    void setUpAccounts() {
        member = createActiveUser("member", PASSWORD, "USER");
        contentAdmin = createActiveUser("content", PASSWORD, "CONTENT_ADMIN");
        pointsAdmin = createActiveUser("points", PASSWORD, "POINTS_ADMIN");
        prAdmin = createActiveUser("pr", PASSWORD, "PR_ADMIN");
        superAdmin = createActiveUser("super", PASSWORD, "SUPER_ADMIN");
    }

    @Test
    void anonymousCanReadPublicPagesButIsSentToLoginEverywhereElse() throws Exception {
        for (String url : PUBLIC_PAGES) {
            mockMvc.perform(get(url))
                    .andExpect(status().isOk());
        }
        mockMvc.perform(get("/members/{id}", member.getId()))
                .andExpect(status().isOk());

        for (String url : ALL_ADMIN_PAGES) {
            mockMvc.perform(get(url))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/login"));
        }
        for (String url : List.of("/me", "/me/articles", "/me/profile", "/me/favorites")) {
            mockMvc.perform(get(url))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/login"));
        }
    }

    @Test
    void normalMemberOnlyReachesOwnPages() throws Exception {
        MockHttpSession session = login(member.getUsername(), PASSWORD);

        for (String url : ALL_ADMIN_PAGES) {
            getAs(session, url).andExpect(status().isForbidden());
        }
        for (String url : List.of("/me", "/me/articles", "/me/profile", "/me/favorites", "/password/change")) {
            getAs(session, url).andExpect(status().isOk());
        }
    }

    @Test
    void contentAdminOwnsContentDomainOnly() throws Exception {
        MockHttpSession session = login(contentAdmin.getUsername(), PASSWORD);

        for (String url : List.of("/admin", "/admin/articles", "/admin/articles/new",
                "/admin/media", "/admin/comments", "/admin/sensitive-words")) {
            getAs(session, url).andExpect(status().isOk());
        }
        for (String url : List.of("/admin/users", "/admin/audit", "/admin/teams", "/admin/points",
                "/admin/rewards", "/admin/appeals", "/admin/competitions", "/admin/public")) {
            getAs(session, url).andExpect(status().isForbidden());
        }
    }

    @Test
    void pointsAdminOwnsPointsDomainOnly() throws Exception {
        MockHttpSession session = login(pointsAdmin.getUsername(), PASSWORD);

        for (String url : List.of("/admin", "/admin/points", "/admin/rewards", "/admin/appeals",
                "/admin/competitions", "/admin/competitions/new")) {
            getAs(session, url).andExpect(status().isOk());
        }
        for (String url : List.of("/admin/users", "/admin/audit", "/admin/teams", "/admin/articles",
                "/admin/media", "/admin/comments", "/admin/sensitive-words", "/admin/public")) {
            getAs(session, url).andExpect(status().isForbidden());
        }
    }

    @Test
    void prAdminOwnsPublicDomainOnly() throws Exception {
        MockHttpSession session = login(prAdmin.getUsername(), PASSWORD);

        for (String url : List.of("/admin", "/admin/public")) {
            getAs(session, url).andExpect(status().isOk());
        }
        for (String url : List.of("/admin/users", "/admin/audit", "/admin/teams", "/admin/points",
                "/admin/rewards", "/admin/appeals", "/admin/competitions", "/admin/articles",
                "/admin/media", "/admin/comments", "/admin/sensitive-words")) {
            getAs(session, url).andExpect(status().isForbidden());
        }
    }

    @Test
    void superAdminReachesEveryAdminPage() throws Exception {
        MockHttpSession session = login(superAdmin.getUsername(), PASSWORD);

        for (String url : ALL_ADMIN_PAGES) {
            getAs(session, url).andExpect(status().isOk());
        }
        for (String url : List.of("/admin/articles/new", "/admin/competitions/new")) {
            getAs(session, url).andExpect(status().isOk());
        }
    }
}
