package cn.edu.cust.assoc.common;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全局异常出口：
 * - 业务异常（BizException / 参数错误）显示明确中文提示，不暴露内部细节；
 * - 未预期的异常只记日志，页面只给一个 correlation id。
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String notFound(NoResourceFoundException e) {
        return "error/404";
    }

    @ExceptionHandler(BizException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String business(BizException e, HttpServletRequest request, Model model) {
        model.addAttribute("message", e.getMessage());
        model.addAttribute("requestId", RequestIds.getOrCreate(request));
        return "error/business";
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String uploadTooLarge(MaxUploadSizeExceededException e, HttpServletRequest request, Model model) {
        model.addAttribute("message", "文件超过 10MB 上限，请压缩后再上传");
        model.addAttribute("requestId", RequestIds.getOrCreate(request));
        return "error/business";
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String unexpected(Exception e, HttpServletRequest request, Model model) {
        String requestId = RequestIds.getOrCreate(request);
        log.error("未处理异常 requestId={} uri={}", requestId, request.getRequestURI(), e);
        model.addAttribute("requestId", requestId);
        return "error/500";
    }
}
