package cn.edu.cust.assoc.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/** 登录态里带上用户 id、昵称和改密标记，避免每次请求都回查数据库。 */
public class AssocUserDetails implements UserDetails {

    private final Long id;
    private final String username;
    private final String password;
    private final String nickname;
    private final boolean enabled;
    private final boolean mustChangePassword;
    private final List<GrantedAuthority> authorities;

    public AssocUserDetails(Long id, String username, String password, String nickname,
                            boolean enabled, boolean mustChangePassword,
                            List<GrantedAuthority> authorities) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.nickname = nickname;
        this.enabled = enabled;
        this.mustChangePassword = mustChangePassword;
        this.authorities = authorities;
    }

    public Long getId() { return id; }
    public String getNickname() { return nickname; }
    public boolean isMustChangePassword() { return mustChangePassword; }

    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
    @Override public String getPassword() { return password; }
    @Override public String getUsername() { return username; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return enabled; }
}
