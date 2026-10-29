package cn.edu.cust.assoc.service;

import cn.edu.cust.assoc.common.BizException;
import cn.edu.cust.assoc.domain.RoleEntity;
import cn.edu.cust.assoc.domain.UserEntity;
import cn.edu.cust.assoc.repository.RoleRepository;
import cn.edu.cust.assoc.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAdminServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private SessionRevocationService sessions;
    @Mock private AuditService audit;

    private UserAdminService service;
    private RoleEntity superRole;
    private UserEntity onlySuperAdmin;

    @BeforeEach
    void setUp() {
        service = new UserAdminService(userRepository, roleRepository, sessions, audit);
        superRole = new RoleEntity();
        superRole.setCode("SUPER_ADMIN");

        onlySuperAdmin = new UserEntity();
        onlySuperAdmin.setId(42L);
        onlySuperAdmin.setUsername("only_super");
        onlySuperAdmin.setStatus("ACTIVE");
        onlySuperAdmin.getRoles().add(superRole);

        when(roleRepository.findByCodeForUpdate("SUPER_ADMIN")).thenReturn(Optional.of(superRole));
        when(userRepository.findById(42L)).thenReturn(Optional.of(onlySuperAdmin));
        when(userRepository.countActiveSuperAdmins()).thenReturn(1L);
    }

    @Test
    @DisplayName("不能移除最后一个启用超管的 SUPER_ADMIN 角色")
    void cannotDemoteLastSuperAdmin() {
        assertThatThrownBy(() -> service.setRoles(42L, Set.of()))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("最后一个");
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("不能禁用最后一个启用超管")
    void cannotDisableLastSuperAdmin() {
        assertThatThrownBy(() -> service.setStatus(42L, "DISABLED"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("最后一个");
        verify(userRepository, never()).save(any());
    }
}
