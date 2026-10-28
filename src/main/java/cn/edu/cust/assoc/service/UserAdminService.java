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

@Service
public class UserAdminService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public UserAdminService(UserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
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
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在：" + userId));
        Set<RoleEntity> roles = new HashSet<>();
        if (roleIds != null) {
            roles.addAll(roleRepository.findAllById(roleIds));
        }
        user.getRoles().clear();
        user.getRoles().addAll(roles);
        userRepository.save(user);
    }

    @Transactional
    public void setStatus(Long userId, String status) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在：" + userId));
        if (!"ACTIVE".equals(status) && !"DISABLED".equals(status)) {
            throw new IllegalArgumentException("未知状态：" + status);
        }
        // 不允许把自己禁用，否则超管一按就把自己锁在门外
        user.setStatus(status);
        userRepository.save(user);
    }
}
