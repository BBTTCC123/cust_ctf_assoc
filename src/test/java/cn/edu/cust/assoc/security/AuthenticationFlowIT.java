package cn.edu.cust.assoc.security;

import cn.edu.cust.assoc.domain.RegistrationInviteEntity;
import cn.edu.cust.assoc.domain.UserEntity;
import cn.edu.cust.assoc.repository.RegistrationInviteRepository;
import cn.edu.cust.assoc.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 登录、首次登录强制改密、邀请码注册的关键路径（ISS-004 / ISS-005）。
 *
 * 这些流程一旦回归，表现是「账号进不去」或「绕过改密」，比接口报错更隐蔽，
 * 所以用 MockMvc 走完整的表单登录、会话与重定向链路。
 */
@Transactional
@EnabledIf("cn.edu.cust.assoc.support.TestDatabase#isAvailable")
class AuthenticationFlowIT extends IntegrationTestBase {

    private static final String PASSWORD = "ItPass!2026abc";

    @Autowired
    private RegistrationInviteRepository inviteRepository;

    @Test
    void activeAccountCanLoginAndWrongPasswordIsRejected() throws Exception {
        UserEntity user = createActiveUser("login", PASSWORD);

        MockHttpSession session = login(user.getUsername(), PASSWORD);
        getAs(session, "/me").andExpect(status().isOk());

        mockMvc.perform(post("/login").with(csrf())
                        .param("username", user.getUsername())
                        .param("password", "WrongPass!2026"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void disabledAccountCannotLogin() throws Exception {
        UserEntity user = createActiveUser("disabled", PASSWORD);
        user.setStatus("DISABLED");
        userRepository.saveAndFlush(user);

        mockMvc.perform(post("/login").with(csrf())
                        .param("username", user.getUsername())
                        .param("password", PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void firstLoginIsTrappedOnPasswordChangeUntilNewPasswordWorks() throws Exception {
        String oldPassword = "OldPass!2026abc";
        String newPassword = "NewPass!2026abc";
        UserEntity user = createUser("force", true, oldPassword);
        MockHttpSession session = login(user.getUsername(), oldPassword);

        // 未改密前除改密页外全部被拦
        getAs(session, "/").andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/password/change"));
        getAs(session, "/me").andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/password/change"));
        getAs(session, "/password/change").andExpect(status().isOk());

        // 当前密码错误：留在改密页并提示
        mockMvc.perform(post("/password/change").session(session).with(csrf())
                        .param("currentPassword", "NotMyPass!2026")
                        .param("newPassword", newPassword)
                        .param("confirmPassword", newPassword))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/password/change"))
                .andExpect(flash().attribute("errorMessage", "当前密码不正确"));

        // 新密码太弱：同样被口令策略拒绝
        mockMvc.perform(post("/password/change").session(session).with(csrf())
                        .param("currentPassword", oldPassword)
                        .param("newPassword", "short")
                        .param("confirmPassword", "short"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/password/change"))
                .andExpect(flash().attribute("errorMessage", "密码至少 12 位"));

        // 改密成功：会话作废，回到登录页
        mockMvc.perform(post("/password/change").session(session).with(csrf())
                        .param("currentPassword", oldPassword)
                        .param("newPassword", newPassword)
                        .param("confirmPassword", newPassword))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attribute("message", "密码已修改，请用新密码重新登录"));

        UserEntity reloaded = userRepository.findById(user.getId()).orElseThrow();
        assertThat(reloaded.getMustChangePassword()).isFalse();
        assertThat(passwordEncoder.matches(newPassword, reloaded.getPasswordHash())).isTrue();

        mockMvc.perform(post("/login").with(csrf())
                        .param("username", user.getUsername())
                        .param("password", oldPassword))
                .andExpect(redirectedUrl("/login?error"));

        MockHttpSession freshSession = login(user.getUsername(), newPassword);
        getAs(freshSession, "/").andExpect(status().isOk());
    }

    @Test
    void inviteRegistrationCreatesMemberAndConsumesTheCode() throws Exception {
        UserEntity admin = createActiveUser("inviteadmin", PASSWORD, "SUPER_ADMIN");
        RegistrationInviteEntity invite = invite(admin.getId(), LocalDateTime.now().plusDays(1));
        String username = uniqueName("reg");
        String password = "RegPass!2026abc";

        mockMvc.perform(post("/register").with(csrf())
                        .param("inviteCode", invite.getCode())
                        .param("username", username)
                        .param("nickname", "邀请注册测试")
                        .param("password", password)
                        .param("confirmPassword", password))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attribute("message", "注册成功，请登录"));

        UserEntity registered = userRepository.findByUsername(username).orElseThrow();
        assertThat(registered.getRoles()).extracting("code").containsExactly("USER");
        assertThat(registered.getMustChangePassword()).isFalse();

        RegistrationInviteEntity used = inviteRepository.findById(invite.getId()).orElseThrow();
        assertThat(used.getUsedCount()).isEqualTo(1);
        assertThat(used.getStatus()).isEqualTo("USED");
        assertThat(used.getUsedBy()).isEqualTo(registered.getId());

        // 新账号可以立即登录，且不再被强制改密
        MockHttpSession session = login(username, password);
        getAs(session, "/me").andExpect(status().isOk());
    }

    @Test
    void unknownExpiredOrMismatchedRegistrationIsRejected() throws Exception {
        UserEntity admin = createActiveUser("inviteadmin2", PASSWORD, "SUPER_ADMIN");

        mockMvc.perform(post("/register").with(csrf())
                        .param("inviteCode", "NOT-A-REAL-CODE")
                        .param("username", uniqueName("badcode"))
                        .param("nickname", "无效邀请码")
                        .param("password", PASSWORD)
                        .param("confirmPassword", PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/register"))
                .andExpect(flash().attribute("errorMessage", "邀请码无效"));

        RegistrationInviteEntity expired = invite(admin.getId(), LocalDateTime.now().minusMinutes(1));
        mockMvc.perform(post("/register").with(csrf())
                        .param("inviteCode", expired.getCode())
                        .param("username", uniqueName("expired"))
                        .param("nickname", "过期邀请码")
                        .param("password", PASSWORD)
                        .param("confirmPassword", PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/register"))
                .andExpect(flash().attribute("errorMessage", "邀请码已过期"));

        RegistrationInviteEntity valid = invite(admin.getId(), LocalDateTime.now().plusDays(1));
        mockMvc.perform(post("/register").with(csrf())
                        .param("inviteCode", valid.getCode())
                        .param("username", uniqueName("mismatch"))
                        .param("nickname", "两次密码不一致")
                        .param("password", PASSWORD)
                        .param("confirmPassword", PASSWORD + "x"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/register"))
                .andExpect(flash().attribute("errorMessage", "两次输入的密码不一致"));

        // 失败流程不能消耗邀请码
        assertThat(inviteRepository.findById(valid.getId()).orElseThrow().getUsedCount()).isZero();
    }

    private RegistrationInviteEntity invite(Long createdBy, LocalDateTime expiresAt) {
        RegistrationInviteEntity invite = new RegistrationInviteEntity();
        invite.setCode(uniqueName("inv").toUpperCase(Locale.ROOT));
        invite.setCreatedBy(createdBy);
        invite.setExpiresAt(expiresAt);
        invite.setMaxUses(1);
        invite.setUsedCount(0);
        invite.setStatus("ACTIVE");
        return inviteRepository.saveAndFlush(invite);
    }
}
