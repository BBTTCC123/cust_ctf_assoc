package cn.edu.cust.assoc.common;

/**
 * 业务异常：消息可以直接展示给用户，不含 SQL、表名等内部信息。
 * 技术异常不要包装成它，交给全局异常处理器记日志并返回通用错误页。
 */
public class BizException extends RuntimeException {

    private final String code;

    public BizException(String message) {
        this("BUSINESS_ERROR", message);
    }

    public BizException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
