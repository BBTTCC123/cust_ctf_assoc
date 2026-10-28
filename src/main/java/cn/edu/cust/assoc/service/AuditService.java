package cn.edu.cust.assoc.service;

import cn.edu.cust.assoc.common.RequestIds;
import cn.edu.cust.assoc.domain.AuditLogEntity;
import cn.edu.cust.assoc.domain.UserEntity;
import cn.edu.cust.assoc.repository.AuditLogRepository;
import cn.edu.cust.assoc.repository.UserRepository;
import cn.edu.cust.assoc.security.AssocUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

/**
 * 审计日志。所有敏感后台操作都从这里留痕：操作人、动作、对象、前后值、IP、requestId。
 * 只提供写入和查询，不提供修改/删除。
 */
@Service
public class AuditService {

    private static final int MAX_DETAIL = 1000;

    private final AuditLogRepository repository;
    private final UserRepository userRepository;

    public AuditService(AuditLogRepository repository, UserRepository userRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void record(String action, String targetType, Object targetId, String before, String after) {
        AuditLogEntity log = new AuditLogEntity();
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId == null ? null : String.valueOf(targetId));
        log.setDetailBefore(trim(before));
        log.setDetailAfter(trim(after));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AssocUserDetails details) {
            log.setOperatorId(details.getId());
            log.setOperatorName(details.getUsername());
        } else if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            log.setOperatorName(auth.getName());
            userRepository.findByUsername(auth.getName()).ifPresent(u -> log.setOperatorId(u.getId()));
        }

        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes servletAttrs) {
            HttpServletRequest request = servletAttrs.getRequest();
            log.setIp(request.getRemoteAddr());
            log.setRequestId(RequestIds.getOrCreate(request));
        }
        repository.save(log);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogEntity> search(String operator, String action, String targetType,
                                       LocalDateTime from, LocalDateTime to, Pageable pageable) {
        return repository.search(blankToNull(operator), blankToNull(action), blankToNull(targetType),
                from, to, pageable);
    }

    private String trim(String value) {
        if (value == null) return null;
        return value.length() <= MAX_DETAIL ? value : value.substring(0, MAX_DETAIL);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
