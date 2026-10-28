package cn.edu.cust.assoc.config;

import cn.edu.cust.assoc.common.BizException;
import cn.edu.cust.assoc.common.PasswordPolicy;
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
 * 首次启动时确保存在一个超管账号。密码只从环境变量 ADMIN_PASSWORD 读。
 *
 * 没有密码、用默认口令或弱口令一律启动失败 —— 宁可不启动，也不让一个已知密码的
 * 超管账号暴露在公网上。新超管首次登录会被强制改密。
 */
@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    public ApplicationRunner initSuperAdmin(UserRepository userRepository,
                                            RoleRepository roleRepository,
                                            PasswordEncoder encoder) {
        return args -> {
            String username = System.getenv().getOrDefault("ADMIN_USERNAME", "Yorkchain");
            if (userRepository.existsByUsername(username)) {
                return;
            }
            String raw = System.getenv("ADMIN_PASSWORD");
            if (raw == null || raw.isBlank()) {
                throw new IllegalStateException(
                        "首次启动必须设置 ADMIN_PASSWORD（至少 12 位，含大小写/数字/符号中的三类）");
            }
            try {
                PasswordPolicy.validate(raw);
            } catch (BizException e) {
                throw new IllegalStateException("ADMIN_PASSWORD 不符合安全要求：" + e.getMessage(), e);
            }
            // 角色由 Flyway 种子数据写入，这里必须从库里取，不能 new 一个（否则会插重复 code）
            RoleEntity superAdmin = roleRepository.findByCode("SUPER_ADMIN")
                    .orElseThrow(() -> new IllegalStateException("缺少 SUPER_ADMIN 角色，请检查 V1 迁移"));

            UserEntity admin = new UserEntity();
            admin.setUsername(username);
            admin.setNickname(username);
            admin.setPasswordHash(encoder.encode(raw));
            admin.setStatus("ACTIVE");
            admin.setMustChangePassword(true);
            admin.getRoles().add(superAdmin);
            userRepository.save(admin);
            log.info("已创建超管账号 {}，首次登录必须修改密码", username);
        };
    }
}
