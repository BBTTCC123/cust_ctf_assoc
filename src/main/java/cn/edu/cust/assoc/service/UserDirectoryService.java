package cn.edu.cust.assoc.service;

import cn.edu.cust.assoc.common.BizException;
import cn.edu.cust.assoc.domain.UserEntity;
import cn.edu.cust.assoc.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** 用户目录：昵称批量查询、成员列表。只对外提供展示需要的字段。 */
@Service
public class UserDirectoryService {

    private final UserRepository userRepository;

    public UserDirectoryService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Map<Long, String> nicknames(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> result = new HashMap<>();
        for (UserEntity user : userRepository.findAllById(userIds)) {
            result.put(user.getId(), user.getNickname());
        }
        return result;
    }

    @Transactional(readOnly = true)
    public String nickname(Long userId) {
        return userRepository.findById(userId).map(UserEntity::getNickname).orElse("成员");
    }

    @Transactional(readOnly = true)
    public List<UserEntity> all() {
        return userRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<UserEntity> find(Long userId) {
        return userRepository.findById(userId);
    }

    @Transactional(readOnly = true)
    public Optional<UserEntity> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Transactional(readOnly = true)
    public UserEntity require(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BizException("成员不存在"));
    }
}
