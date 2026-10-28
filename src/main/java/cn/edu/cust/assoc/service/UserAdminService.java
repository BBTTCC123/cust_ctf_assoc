package cn.edu.cust.assoc.service;

import cn.edu.cust.assoc.domain.RoleEntity;
import cn.edu.cust.assoc.domain.UserEntity;
import cn.edu.cust.assoc.repository.RoleRepository;
import cn.edu.cust.assoc.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserAdminService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final SessionRevocationService sessions;
    private final AuditService audit;

    public UserAdminService(UserRepository userRepository, RoleRepository roleRepository,
                            SessionRevocationService sessions, AuditService audit) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.sessions = sessions;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<UserEntity> users() {
        return userRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<RoleEntity> roles() {
        return roleRepository.findAll();
    }

    /** 整体替换某人的角色。传空集合 = 收回所有管理权限（还剩普通用户身份）。 */
    @Transactional
    public void setRoles(Long userId, Set<Long> roleIds) {
        UserEntity user = require(userId);
        String before = roleCodes(user);
        Set<RoleEntity> roles = new HashSet<>();
        if (roleIds != null) {
            roles.addAll(roleRepository.findAllById(roleIds));
        }
        user.getRoles().clear();
        user.getRoles().addAll(roles);
        userRepository.save(user);
        // 权限在登录时装进会话：改完必须让旧会话立即失效，否则降权不生效
        sessions.expireAll(userId);
        audit.record("ROLE_CHANGE", "USER", userId, before, roleCodes(user));
    }

    @Transactional
    public void setStatus(Long userId, String status) {
        UserEntity user = require(userId);
        if (!"ACTIVE".equals(status) && !"DISABLED".equals(status)) {
            throw new IllegalArgumentException("未知状态：" + status);
        }
        String before = user.getStatus();
        user.setStatus(status);
        userRepository.save(user);
        if ("DISABLED".equals(status)) {
            sessions.expireAll(userId);
        }
        audit.record("USER_STATUS_CHANGE", "USER", userId, before, status);
    }

    private UserEntity require(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在：" + userId));
    }

    private String roleCodes(UserEntity user) {
        return user.getRoles().stream().map(RoleEntity::getCode).sorted().collect(Collectors.joining(","));
    }
}
