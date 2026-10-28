package cn.edu.cust.assoc.service;

import cn.edu.cust.assoc.domain.MemberProfileEntity;
import cn.edu.cust.assoc.dto.ProfileView;
import cn.edu.cust.assoc.repository.MemberProfileRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MemberProfileServiceTest {

    private final MemberProfileRepository repository = mock(MemberProfileRepository.class);
    private final MemberProfileService service = new MemberProfileService(repository, mock(AuditService.class));

    @Test
    @DisplayName("默认不公开真实姓名、学号、手机、邮箱和博客")
    void privateByDefault() {
        MemberProfileEntity entity = profile(false, false, false, false, false);
        when(repository.findByUserId(1L)).thenReturn(Optional.of(entity));

        ProfileView view = service.publicView(1L).orElseThrow();

        assertThat(view.realName()).isNull();
        assertThat(view.studentNo()).isNull();
        assertThat(view.phone()).isNull();
        assertThat(view.email()).isNull();
        assertThat(view.blog()).isNull();
        // 学院/专业/年级没有开关，始终展示
        assertThat(view.college()).isEqualTo("计算机学院");
        assertThat(view.major()).isEqualTo("信息安全");
        assertThat(view.grade()).isEqualTo("2024 级");
    }

    @Test
    @DisplayName("打开开关的字段才会出现在公开页")
    void showEnabledFields() {
        MemberProfileEntity entity = profile(true, false, true, false, true);
        when(repository.findByUserId(2L)).thenReturn(Optional.of(entity));

        ProfileView view = service.publicView(2L).orElseThrow();

        assertThat(view.realName()).isEqualTo("张三");
        assertThat(view.studentNo()).isNull();
        assertThat(view.phone()).isEqualTo("13800000000");
        assertThat(view.email()).isNull();
        assertThat(view.blog()).isEqualTo("https://example.com");
    }

    private MemberProfileEntity profile(boolean showRealName, boolean showStudentNo,
                                        boolean showPhone, boolean showEmail, boolean showBlog) {
        MemberProfileEntity entity = new MemberProfileEntity();
        entity.setUserId(1L);
        entity.setRealName("张三");
        entity.setStudentNo("250511106");
        entity.setCollege("计算机学院");
        entity.setMajor("信息安全");
        entity.setGrade("2024 级");
        entity.setPhone("13800000000");
        entity.setEmail("test@example.com");
        entity.setBlog("https://example.com");
        entity.setShowRealName(showRealName);
        entity.setShowStudentNo(showStudentNo);
        entity.setShowPhone(showPhone);
        entity.setShowEmail(showEmail);
        entity.setShowBlog(showBlog);
        return entity;
    }
}
