package cn.edu.cust.assoc.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordPolicyTest {

    @Test
    @DisplayName("强密码通过")
    void strong() {
        assertThatCode(() -> PasswordPolicy.validate("Zx9!Qw8@Ty2#Lm5")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("空、过短、缺字符类别、常见弱口令都拒绝")
    void weak() {
        assertThatThrownBy(() -> PasswordPolicy.validate(null)).isInstanceOf(BizException.class);
        assertThatThrownBy(() -> PasswordPolicy.validate("Short1!")).isInstanceOf(BizException.class);
        assertThatThrownBy(() -> PasswordPolicy.validate("aaaaaaaaaaaaaa")).isInstanceOf(BizException.class);
        assertThatThrownBy(() -> PasswordPolicy.validate("ChangeMe#2026")).isInstanceOf(BizException.class);
        assertThatThrownBy(() -> PasswordPolicy.validate("has space 1234!A")).isInstanceOf(BizException.class);
    }
}
