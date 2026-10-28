package cn.edu.cust.assoc.config;

import cn.edu.cust.assoc.domain.RoleEntity;
import cn.edu.cust.assoc.domain.UserEntity;
import cn.edu.cust.assoc.repository.RoleRepository;
import cn.edu.cust.assoc.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 首次启动时确保存在一个超管账号。密码从环境变量 ADMIN_PASSWORD 读。
 */
@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    private static final String DEFAULT_ADMIN_PASSWORD = "ChangeMe#2026";

    @Bean
    public ApplicationRunner initSuperAdmin(UserRepository userRepository,
                                            RoleRepository roleRepository,
                                            PasswordEncoder encoder) {
        return args -> {
            String username = System.getenv().getOrDefault("ADMIN_USERNAME", "Yorkchain");
            if (userRepository.existsByUsername(username)) {
                return;
            }
            String raw = System.getenv().getOrDefault("ADMIN_PASSWORD", DEFAULT_ADMIN_PASSWORD);
            if (DEFAULT_ADMIN_PASSWORD.equals(raw)) {
                log.warn("未设置 ADMIN_PASSWORD，超管使用默认密码，登录后请立刻修改");
            }
            // 角色由 Flyway 种子数据写入，这里必须从库里取，不能 new 一个（否则会插重复 code）
            RoleEntity superAdmin = roleRepository.findByCode("SUPER_ADMIN")
                    .orElseThrow(() -> new IllegalStateException("缺少 SUPER_ADMIN 角色，请检查 V1 迁移"));

            UserEntity admin = new UserEntity();
            admin.setUsername(username);
            admin.setNickname("Yorkchain");
            admin.setPasswordHash(encoder.encode(raw));
            admin.setStatus("ACTIVE");
            admin.getRoles().add(superAdmin);
            userRepository.save(admin);
            log.info("已创建超管账号 {}", username);
        };
    }
}
