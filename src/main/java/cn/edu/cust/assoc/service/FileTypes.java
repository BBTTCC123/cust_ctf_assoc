package cn.edu.cust.assoc.service;

import java.util.Map;

/**
 * 按文件头判断真实类型。浏览器给的 Content-Type 和扩展名都能伪造，只有魔数不能。
 */
public final class FileTypes {

    private FileTypes() {}

    public record Detected(String ext, String mime) {}

    private static final Map<String, Detected> BY_EXT = Map.of(
            "jpg", new Detected("jpg", "image/jpeg"),
            "jpeg", new Detected("jpg", "image/jpeg"),
            "png", new Detected("png", "image/png"),
            "pdf", new Detected("pdf", "application/pdf")
    );

    /** 支持的类型，按扩展名白名单。 */
    public static Detected allowed(String ext) {
        return BY_EXT.get(ext == null ? "" : ext.toLowerCase());
    }

    /** 文件头是否与声明的扩展名一致。 */
    public static boolean matchesMagic(byte[] head, String ext) {
        if (head == null || head.length < 4) return false;
        String e = ext == null ? "" : ext.toLowerCase();
        return switch (e) {
            case "jpg", "jpeg" -> (head[0] & 0xFF) == 0xFF && (head[1] & 0xFF) == 0xD8 && (head[2] & 0xFF) == 0xFF;
            case "png" -> (head[0] & 0xFF) == 0x89 && head[1] == 'P' && head[2] == 'N' && head[3] == 'G';
            case "pdf" -> head[0] == '%' && head[1] == 'P' && head[2] == 'D' && head[3] == 'F';
            default -> false;
        };
    }
}
