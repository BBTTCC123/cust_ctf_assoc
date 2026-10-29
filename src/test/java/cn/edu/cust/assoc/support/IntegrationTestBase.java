package cn.edu.cust.assoc.support;

import cn.edu.cust.assoc.domain.RoleEntity;
import cn.edu.cust.assoc.domain.UserEntity;
import cn.edu.cust.assoc.repository.RoleRepository;
import cn.edu.cust.assoc.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 集成测试基类：真实 MySQL + 完整 Spring 上下文 + MockMvc。
 *
 * 继承它的类按 *IT 命名，由 maven-failsafe-plugin 在 verify 阶段执行；
 * 每个具体测试类用 {@code @EnabledIf("...TestDatabase#isAvailable")} 标注，
 * 没有 Docker 且没有配置外部库时整类跳过，不影响 mvn test 的单元测试。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("it")
@Import(TestContainerConfig.class)
public abstract class IntegrationTestBase {

    /** 每次 JVM 启动一个短跑标记，保证外部库多次运行时用户名不撞。 */
    private static final String RUN_ID = Long.toString(System.currentTimeMillis(), 36);
    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected RoleRepository roleRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @DynamicPropertySource
    static void dataSourceProperties(DynamicPropertyRegistry registry) {
        if (TestDatabase.isExternal()) {
            TestDatabase.registerExternalDataSource(registry);
        }
    }

    protected static String uniqueName(String label) {
        String clean = label == null ? "x" : label.replaceAll("[^A-Za-z0-9_]", "");
        return "it" + RUN_ID + "_" + clean + "_" + SEQ.incrementAndGet();
    }

    /** 造一个可登录的账号；角色为空时默认 USER。 */
    protected UserEntity createUser(String label, boolean mustChangePassword,
                                    String rawPassword, String... roleCodes) {
        UserEntity user = new UserEntity();
        user.setUsername(uniqueName(label));
        user.setNickname("测试" + label + SEQ.get());
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setStatus("ACTIVE");
        user.setMustChangePassword(mustChangePassword);
        if (roleCodes.length == 0) {
            user.getRoles().add(role("USER"));
        } else {
            for (String code : roleCodes) {
                user.getRoles().add(role(code));
            }
        }
        return userRepository.saveAndFlush(user);
    }

    protected UserEntity createActiveUser(String label, String rawPassword, String... roleCodes) {
        return createUser(label, false, rawPassword, roleCodes);
    }

    protected RoleEntity role(String code) {
        return roleRepository.findByCode(code)
                .orElseThrow(() -> new IllegalStateException("V1 种子缺少角色 " + code));
    }

    /** 表单登录并返回已认证会话；账号被禁用或密码错误会直接让断言失败。 */
    protected MockHttpSession login(String username, String rawPassword) throws Exception {
        MvcResult result = mockMvc.perform(post("/login").with(csrf())
                        .param("username", username)
                        .param("password", rawPassword))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    protected ResultActions getAs(MockHttpSession session, String url) throws Exception {
        return mockMvc.perform(get(url).session(session));
    }
}
