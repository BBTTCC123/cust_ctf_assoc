package cn.edu.cust.assoc.security;

import cn.edu.cust.assoc.domain.UserEntity;
import cn.edu.cust.assoc.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/** 从登录态解析当前用户。业务代码禁止再写固定用户 1。 */
@Component
public class CurrentUserService {

    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Long id(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalStateException("未登录");
        }
        if (auth.getPrincipal() instanceof AssocUserDetails details) {
            return details.getId();
        }
        return userRepository.findByUsername(auth.getName())
                .map(UserEntity::getId)
                .orElseThrow(() -> new IllegalStateException("登录用户不存在"));
    }

    public String nickname(Authentication auth) {
        if (auth != null && auth.getPrincipal() instanceof AssocUserDetails details) {
            return details.getNickname();
        }
        return userRepository.findByUsername(auth.getName())
                .map(UserEntity::getNickname)
                .orElse(auth.getName());
    }

    public UserEntity require(Authentication auth) {
        Long id = id(auth);
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("登录用户不存在"));
    }
}
