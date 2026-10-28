package cn.edu.cust.assoc.service;

import cn.edu.cust.assoc.common.BizException;
import cn.edu.cust.assoc.common.PasswordPolicy;
import cn.edu.cust.assoc.domain.RegistrationInviteEntity;
import cn.edu.cust.assoc.domain.RoleEntity;
import cn.edu.cust.assoc.domain.UserEntity;
import cn.edu.cust.assoc.repository.RegistrationInviteRepository;
import cn.edu.cust.assoc.repository.RoleRepository;
import cn.edu.cust.assoc.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 账号闭环：超管创建成员、邀请码注册、自助改密、超管重置。
 *
 * 规则：
 * - 超管创建 / 重置后的账号必须首次登录改密；
 * - 邀请码一次性使用（可配置次数），过期、停用、用完都不行；
 * - 重置密码只展示一次，库里只存 BCrypt 哈希。
 */
@Service
public class AccountService {

    private static final Pattern USERNAME = Pattern.compile("^[A-Za-z0-9_]{4,32}$");
    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RegistrationInviteRepository inviteRepository;
    private final PasswordEncoder passwordEncoder;
    private final SessionRevocationService sessions;
    private final AuditService audit;

    public AccountService(UserRepository userRepository,
                          RoleRepository roleRepository,
                          RegistrationInviteRepository inviteRepository,
                          PasswordEncoder passwordEncoder,
                          SessionRevocationService sessions,
                          AuditService audit) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.inviteRepository = inviteRepository;
        this.passwordEncoder = passwordEncoder;
        this.sessions = sessions;
        this.audit = audit;
    }

    @Transactional
    public UserEntity createMember(String username, String nickname, String rawPassword) {
        String name = validateUsername(username);
        String displayName = validateNickname(nickname);
        PasswordPolicy.validate(rawPassword);
        if (userRepository.existsByUsername(name)) {
            throw new BizException("登录名已被占用");
        }
        UserEntity user = new UserEntity();
        user.setUsername(name);
        user.setNickname(displayName);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setStatus("ACTIVE");
        user.setMustChangePassword(true);
        user.getRoles().add(role("USER"));
        UserEntity saved = saveWithUniqueMessage(user);
        audit.record("USER_CREATE", "USER", saved.getId(), null, name + " / " + displayName);
        return saved;
    }

    /** 返回一次性临时密码，调用方只展示一次。 */
    @Transactional
    public String resetPassword(Long userId) {
        UserEntity user = requireUser(userId);
        String temp = randomPassword();
        user.setPasswordHash(passwordEncoder.encode(temp));
        user.setMustChangePassword(true);
        userRepository.save(user);
        sessions.expireAll(userId);
        audit.record("USER_RESET_PASSWORD", "USER", userId, user.getUsername(), "已下发一次性临时密码");
        return temp;
    }

    @Transactional
    public void changeOwnPassword(Long userId, String currentRaw, String newRaw) {
        UserEntity user = requireUser(userId);
        if (currentRaw == null || !passwordEncoder.matches(currentRaw, user.getPasswordHash())) {
            throw new BizException("当前密码不正确");
        }
        PasswordPolicy.validate(newRaw);
        if (passwordEncoder.matches(newRaw, user.getPasswordHash())) {
            throw new BizException("新密码不能和当前密码相同");
        }
        user.setPasswordHash(passwordEncoder.encode(newRaw));
        user.setMustChangePassword(false);
        userRepository.save(user);
        audit.record("PASSWORD_CHANGE", "USER", userId, user.getUsername(), "本人修改密码");
    }

    @Transactional
    public UserEntity register(String inviteCode, String username, String nickname, String rawPassword) {
        if (inviteCode == null || inviteCode.isBlank()) {
            throw new BizException("必须填写邀请码");
        }
        String code = inviteCode.trim().toUpperCase();
        RegistrationInviteEntity invite = inviteRepository.findByCodeForUpdate(code)
                .orElseThrow(() -> new BizException("邀请码无效"));
        if (!"ACTIVE".equals(invite.getStatus())) {
            throw new BizException("邀请码已停用或已用完");
        }
        if (invite.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BizException("邀请码已过期");
        }
        if (invite.getUsedCount() >= invite.getMaxUses()) {
            throw new BizException("邀请码已用完");
        }
        String name = validateUsername(username);
        String displayName = validateNickname(nickname);
        PasswordPolicy.validate(rawPassword);
        if (userRepository.existsByUsername(name)) {
            throw new BizException("登录名已被占用");
        }

        UserEntity user = new UserEntity();
        user.setUsername(name);
        user.setNickname(displayName);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setStatus("ACTIVE");
        user.setMustChangePassword(false);
        user.getRoles().add(role("USER"));
        UserEntity saved = saveWithUniqueMessage(user);

        invite.setUsedCount(invite.getUsedCount() + 1);
        invite.setUsedBy(saved.getId());
        if (invite.getUsedCount() >= invite.getMaxUses()) {
            invite.setStatus("USED");
        }
        inviteRepository.save(invite);
        audit.record("USER_REGISTER", "USER", saved.getId(), null, "邀请码 " + code);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<RegistrationInviteEntity> invites() {
        return inviteRepository.findAllByOrderByExpiresAtDesc();
    }

    @Transactional
    public RegistrationInviteEntity createInvite(int maxUses, LocalDateTime expiresAt, String note) {
        if (maxUses < 1 || maxUses > 100) {
            throw new BizException("邀请码可用次数必须在 1 到 100 之间");
        }
        if (expiresAt == null || expiresAt.isBefore(LocalDateTime.now())) {
            throw new BizException("有效期必须晚于现在");
        }
        RegistrationInviteEntity invite = new RegistrationInviteEntity();
        invite.setCode(uniqueCode());
        invite.setCreatedBy(auditOperatorId());
        invite.setMaxUses(maxUses);
        invite.setUsedCount(0);
        invite.setStatus("ACTIVE");
        invite.setExpiresAt(expiresAt);
        invite.setNote(note == null || note.isBlank() ? null : note.trim());
        RegistrationInviteEntity saved = inviteRepository.save(invite);
        audit.record("INVITE_CREATE", "INVITE", saved.getId(), null,
                saved.getCode() + "，可用 " + maxUses + " 次");
        return saved;
    }

    @Transactional
    public void revokeInvite(Long inviteId) {
        RegistrationInviteEntity invite = inviteRepository.findById(inviteId)
                .orElseThrow(() -> new BizException("邀请码不存在"));
        if (!"ACTIVE".equals(invite.getStatus())) {
            throw new BizException("邀请码已经是停用状态");
        }
        invite.setStatus("REVOKED");
        inviteRepository.save(invite);
        audit.record("INVITE_REVOKE", "INVITE", inviteId, invite.getCode(), "已停用");
    }

    private UserEntity requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BizException("用户不存在"));
    }

    private RoleEntity role(String code) {
        return roleRepository.findByCode(code)
                .orElseThrow(() -> new IllegalStateException("缺少角色 " + code + "，请检查 V1 迁移"));
    }

    private String validateUsername(String username) {
        String name = username == null ? "" : username.trim();
        if (!USERNAME.matcher(name).matches()) {
            throw new BizException("登录名只能使用字母、数字和下划线，长度 4 到 32 位");
        }
        return name;
    }

    private String validateNickname(String nickname) {
        String displayName = nickname == null ? "" : nickname.trim();
        if (displayName.isEmpty() || displayName.length() > 64) {
            throw new BizException("昵称不能为空且不超过 64 个字符");
        }
        return displayName;
    }

    private UserEntity saveWithUniqueMessage(UserEntity user) {
        try {
            return userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            throw new BizException("登录名已被占用");
        }
    }

    private String uniqueCode() {
        for (int attempt = 0; attempt < 12; attempt++) {
            String code = generate(CODE_ALPHABET, 12);
            if (!inviteRepository.existsByCode(code)) {
                return code;
            }
        }
        throw new IllegalStateException("邀请码生成失败，请重试");
    }

    /** 临时密码：至少包含大小写、数字、符号四类，长度 16。 */
    private String randomPassword() {
        String upper = "ABCDEFGHJKLMNPQRSTUVWXYZ";
        String lower = "abcdefghijkmnopqrstuvwxyz";
        String digits = "23456789";
        String symbols = "!@#$%^&*";
        StringBuilder sb = new StringBuilder();
        sb.append(pick(upper)).append(pick(lower)).append(pick(digits)).append(pick(symbols));
        String all = upper + lower + digits + symbols;
        while (sb.length() < 16) {
            sb.append(pick(all));
        }
        char[] chars = sb.toString().toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int j = RANDOM.nextInt(i + 1);
            char tmp = chars[i];
            chars[i] = chars[j];
            chars[j] = tmp;
        }
        return new String(chars);
    }

    private char pick(String alphabet) {
        return alphabet.charAt(RANDOM.nextInt(alphabet.length()));
    }

    private String generate(String alphabet, int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(pick(alphabet));
        }
        return sb.toString();
    }

    private Long auditOperatorId() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof cn.edu.cust.assoc.security.AssocUserDetails d) {
            return d.getId();
        }
        return null;
    }
}
