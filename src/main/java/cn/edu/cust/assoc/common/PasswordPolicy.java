package cn.edu.cust.assoc.common;

import java.util.Locale;
import java.util.Set;

/**
 * 口令强度规则。管理员初始化、成员创建、重置、自助改密共用同一个口径。
 */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 12;

    /** 历史默认口令与常见弱口令，一律拒绝。 */
    private static final Set<String> WEAK = Set.of(
            "changeme#2026", "changeit#2026", "password123!", "admin123456!",
            "123456789012", "qwerty123456", "password", "admin");

    private PasswordPolicy() {}

    public static void validate(String raw) {
        if (raw == null || raw.length() < MIN_LENGTH) {
            throw new BizException("密码至少 " + MIN_LENGTH + " 位");
        }
        if (raw.chars().anyMatch(Character::isWhitespace)) {
            throw new BizException("密码不能包含空白字符");
        }
        if (WEAK.contains(raw.toLowerCase(Locale.ROOT))) {
            throw new BizException("这个密码太常见，请换一个");
        }
        int classes = 0;
        if (raw.chars().anyMatch(Character::isUpperCase)) classes++;
        if (raw.chars().anyMatch(Character::isLowerCase)) classes++;
        if (raw.chars().anyMatch(Character::isDigit)) classes++;
        if (raw.chars().anyMatch(c -> !Character.isLetterOrDigit(c))) classes++;
        if (classes < 3) {
            throw new BizException("密码至少包含大写字母、小写字母、数字、符号中的三类");
        }
    }
}
