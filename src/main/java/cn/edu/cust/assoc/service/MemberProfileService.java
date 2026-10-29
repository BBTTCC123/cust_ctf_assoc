package cn.edu.cust.assoc.service;

import cn.edu.cust.assoc.common.BizException;
import cn.edu.cust.assoc.common.UrlPolicy;
import cn.edu.cust.assoc.domain.MemberProfileEntity;
import cn.edu.cust.assoc.dto.MemberProfileForm;
import cn.edu.cust.assoc.dto.ProfileView;
import cn.edu.cust.assoc.repository.MemberProfileRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 成员资料与隐私开关。
 * 公开页只返回开关允许的字段；学号、手机、邮箱默认不公开。
 */
@Service
public class MemberProfileService {

    private final MemberProfileRepository repository;
    private final AuditService audit;

    public MemberProfileService(MemberProfileRepository repository, AuditService audit) {
        this.repository = repository;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public MemberProfileForm form(Long userId) {
        MemberProfileForm form = new MemberProfileForm();
        repository.findByUserId(userId).ifPresent(entity -> {
            form.setRealName(entity.getRealName());
            form.setStudentNo(entity.getStudentNo());
            form.setCollege(entity.getCollege());
            form.setMajor(entity.getMajor());
            form.setGrade(entity.getGrade());
            form.setPhone(entity.getPhone());
            form.setEmail(entity.getEmail());
            form.setBlog(entity.getBlog());
            form.setShowRealName(Boolean.TRUE.equals(entity.getShowRealName()));
            form.setShowStudentNo(Boolean.TRUE.equals(entity.getShowStudentNo()));
            form.setShowPhone(Boolean.TRUE.equals(entity.getShowPhone()));
            form.setShowEmail(Boolean.TRUE.equals(entity.getShowEmail()));
            form.setShowBlog(Boolean.TRUE.equals(entity.getShowBlog()));
        });
        return form;
    }

    @Transactional
    public void save(Long userId, MemberProfileForm form) {
        MemberProfileEntity entity = repository.findByUserId(userId).orElseGet(() -> {
            MemberProfileEntity created = new MemberProfileEntity();
            created.setUserId(userId);
            return created;
        });
        String before = entity.getId() == null ? null : entity.getRealName();

        entity.setRealName(require(form.getRealName(), "真实姓名", 64));
        entity.setStudentNo(optionalStudentNo(form.getStudentNo()));
        entity.setCollege(require(form.getCollege(), "学院", 128));
        entity.setMajor(require(form.getMajor(), "专业", 128));
        entity.setGrade(require(form.getGrade(), "年级", 32));
        entity.setPhone(optional(form.getPhone(), 32));
        entity.setEmail(optionalEmail(form.getEmail()));
        entity.setBlog(optionalBlog(form.getBlog()));
        entity.setShowRealName(form.isShowRealName());
        entity.setShowStudentNo(form.isShowStudentNo());
        entity.setShowPhone(form.isShowPhone());
        entity.setShowEmail(form.isShowEmail());
        entity.setShowBlog(form.isShowBlog());

        try {
            repository.saveAndFlush(entity);
        } catch (DataIntegrityViolationException e) {
            throw new BizException("这个学号已经被其他成员使用");
        }
        audit.record("PROFILE_UPDATE", "MEMBER_PROFILE", userId, before, entity.getRealName());
    }

    @Transactional(readOnly = true)
    public Optional<ProfileView> publicView(Long userId, boolean authenticatedViewer) {
        if (!authenticatedViewer) {
            return Optional.empty();
        }
        return repository.findByUserId(userId).map(e -> new ProfileView(
                Boolean.TRUE.equals(e.getShowRealName()) ? e.getRealName() : null,
                Boolean.TRUE.equals(e.getShowStudentNo()) ? e.getStudentNo() : null,
                e.getCollege(), e.getMajor(), e.getGrade(),
                Boolean.TRUE.equals(e.getShowPhone()) ? e.getPhone() : null,
                Boolean.TRUE.equals(e.getShowEmail()) ? e.getEmail() : null,
                Boolean.TRUE.equals(e.getShowBlog()) ? e.getBlog() : null));
    }

    private String require(String value, String label, int max) {
        String text = value == null ? "" : value.trim();
        if (text.isEmpty()) {
            throw new BizException(label + "不能为空");
        }
        if (text.length() > max) {
            throw new BizException(label + "不能超过 " + max + " 个字符");
        }
        return text;
    }

    private String optional(String value, int max) {
        if (value == null || value.isBlank()) return null;
        String text = value.trim();
        if (text.length() > max) {
            throw new BizException("填写内容过长");
        }
        return text;
    }

    private String optionalStudentNo(String value) {
        if (value == null || value.isBlank()) return null;
        String text = value.trim();
        if (!text.matches("\\d{6,20}")) {
            throw new BizException("学号必须是 6 到 20 位数字");
        }
        return text;
    }

    private String optionalEmail(String value) {
        String text = optional(value, 254);
        if (text != null && (!text.contains("@") || text.contains(" "))) {
            throw new BizException("邮箱格式不对");
        }
        return text;
    }

    private String optionalBlog(String value) {
        String text = optional(value, 2048);
        if (text != null && !UrlPolicy.isSafeLink(text)) {
            throw new BizException("博客链接只允许 http/https");
        }
        return text;
    }
}
