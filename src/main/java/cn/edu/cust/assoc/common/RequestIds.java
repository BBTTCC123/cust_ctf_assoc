package cn.edu.cust.assoc.common;

import jakarta.servlet.http.HttpServletRequest;

import java.util.UUID;

/** 每个请求一个 correlation id，错误页和日志用它对齐。 */
public final class RequestIds {

    private static final String ATTR = "app.requestId";

    private RequestIds() {}

    public static String getOrCreate(HttpServletRequest request) {
        Object existing = request.getAttribute(ATTR);
        if (existing instanceof String s && !s.isBlank()) {
            return s;
        }
        String id = UUID.randomUUID().toString().substring(0, 8);
        request.setAttribute(ATTR, id);
        return id;
    }
}
