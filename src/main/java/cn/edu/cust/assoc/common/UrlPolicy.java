package cn.edu.cust.assoc.common;

import java.util.Locale;

/**
 * 链接协议白名单。Markdown 渲染、模板里的外链都走这一套判断。
 *
 * 允许：http、https、mailto（仅链接）、站内相对链接。
 * 拒绝：javascript、data、vbscript 等任何其它协议，以及协议相对地址（//host）。
 */
public final class UrlPolicy {

    private UrlPolicy() {}

    public static String sanitizeLink(String raw) {
        return sanitize(raw, true);
    }

    public static String sanitizeImage(String raw) {
        String safe = sanitize(raw, false);
        // 页面只允许 HTTPS 外链图片；HTTP 图片在 HTTPS 站点会被浏览器拦截。
        return safe.toLowerCase(Locale.ROOT).startsWith("http://") ? "" : safe;
    }

    public static boolean isSafeLink(String raw) {
        return !sanitizeLink(raw).isEmpty();
    }

    public static boolean isSafeImage(String raw) {
        return !sanitizeImage(raw).isEmpty();
    }

    private static String sanitize(String raw, boolean allowMailto) {
        if (raw == null) {
            return "";
        }
        String url = raw.strip();
        if (url.isEmpty()) {
            return "";
        }
        // 浏览器会忽略协议里的控制字符和零宽字符，判断前必须先剥掉
        String probe = stripInvisible(url);
        String lower = probe.toLowerCase(Locale.ROOT);
        if (lower.startsWith("http://") || lower.startsWith("https://")) {
            return url;
        }
        if (allowMailto && lower.startsWith("mailto:")) {
            return url;
        }
        if (probe.startsWith("//")) {
            return "";
        }
        // 冒号出现在斜杠、问号、井号之前，就视为带协议，只认上面的白名单
        int colon = probe.indexOf(':');
        int pathStart = firstIndexOf(probe, '/', '?', '#');
        if (colon >= 0 && (pathStart < 0 || colon < pathStart)) {
            return "";
        }
        return url;
    }

    private static String stripInvisible(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c <= 0x20 || c == 0x7F) {
                continue;
            }
            if ((c >= '​' && c <= '‏') || c == '﻿' || c == ' ') {
                continue;
            }
            sb.append(c);
        }
        return sb.toString();
    }

    private static int firstIndexOf(String s, char... targets) {
        int best = -1;
        for (char t : targets) {
            int idx = s.indexOf(t);
            if (idx >= 0 && (best < 0 || idx < best)) {
                best = idx;
            }
        }
        return best;
    }
}
