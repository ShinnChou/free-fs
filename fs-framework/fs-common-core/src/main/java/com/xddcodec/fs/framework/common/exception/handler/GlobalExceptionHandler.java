package com.xddcodec.fs.framework.common.exception.handler;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import com.xddcodec.fs.framework.common.domain.Result;
import com.xddcodec.fs.framework.common.exception.BusinessException;
import com.xddcodec.fs.framework.common.exception.ErrorCode;
import com.xddcodec.fs.framework.common.exception.StorageConfigException;
import com.xddcodec.fs.framework.common.exception.StorageOperationException;
import com.xddcodec.fs.framework.common.utils.ErrorMessageUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.yaml.snakeyaml.constructor.DuplicateKeyException;

/**
 * 全局异常处理
 *
 * @Author: xddcode
 * @Date: 2023/6/29 17:27
 */
@Slf4j
@RestControllerAdvice
@ResponseBody
public class GlobalExceptionHandler {

    /**
     * 权限码异常
     */
    @ExceptionHandler(NotPermissionException.class)
    public Result<?> handleNotPermissionException(NotPermissionException e, HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        log.error("请求地址'{}',权限码校验失败'{}'", requestURI, e.getMessage());
        return Result.error(ErrorCode.FORBIDDEN.getCode(), ErrorCode.FORBIDDEN.getMsg(), null);
    }

    /**
     * 角色权限异常
     */
//    @ExceptionHandler(NotRoleException.class)
//    public Result<?> handleNotRoleException(NotRoleException e, HttpServletRequest request) {
//        String requestURI = request.getRequestURI();
//        log.error("请求地址'{}',角色权限校验失败'{}'", requestURI, e.getMessage());
//        return Result.forbidden("没有访问权限，请联系管理员授权");
//    }

    /**
     * 认证失败
     */
    @ExceptionHandler(NotLoginException.class)
    public Result<?> handlerNotLoginException(NotLoginException nle) {
        ErrorCode errorCode = switch (nle.getType()) {
            case NotLoginException.NOT_TOKEN -> ErrorCode.NOT_TOKEN;
            case NotLoginException.INVALID_TOKEN -> ErrorCode.INVALID_TOKEN;
            case NotLoginException.TOKEN_TIMEOUT -> ErrorCode.EXPIRED_TOKEN;
            case NotLoginException.BE_REPLACED -> ErrorCode.REPLACED_TOKEN;
            case NotLoginException.KICK_OUT -> ErrorCode.KICK_OUT_TOKEN;
            case NotLoginException.NO_PREFIX -> ErrorCode.NO_PREFIX_MESSAGE_TOKEN;
            default -> ErrorCode.UNAUTHORIZED;
        };
        log.error("Token exception [{}]: {}", errorCode.getCode(), nle.getMessage(), nle);
        return Result.error(errorCode.getCode(), errorCode.getMsg(), null);
    }

    /**
     * 主键或UNIQUE索引，数据重复异常
     */
    @ExceptionHandler(DuplicateKeyException.class)
    public Result<Void> handleDuplicateKeyException(DuplicateKeyException e, HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        log.error("请求地址'{}',数据库中已存在记录'{}'", requestURI, e.getMessage());
        return Result.error("数据库中已存在该记录，请联系管理员确认");
    }

    /**
     * IllegalArgumentException 异常处理返回json
     */
    @ExceptionHandler({IllegalArgumentException.class})
    public Result<?> badRequestException(IllegalArgumentException e) {
        return defHandler(e.getMessage(), e);
    }

    /**
     * BusinessException 业务异常处理
     * 返回状态码:500
     */
    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusinessException(BusinessException e) {
        return defHandler(e.getMessage(), e);
    }


    /**
     * StorageOperationException 存储异常处理
     *
     * @param e
     * @return
     */
    @ExceptionHandler(StorageOperationException.class)
    public Result<?> handleStorageOperationException(StorageOperationException e) {
        String userFriendlyMessage = ErrorMessageUtils.extractUserFriendlyMessage(e);
        log.error("存储操作异常: {}", e.getMessage(), e);
        return Result.error(userFriendlyMessage);
    }

    /**
     * StorageConfigException 存储配置异常处理
     * 返回状态码:500
     */
    @ExceptionHandler(StorageConfigException.class)
    public Result<?> handleStorageConfigException(StorageConfigException e) {
        String userFriendlyMessage = ErrorMessageUtils.extractUserFriendlyMessage(e);
        log.error("存储配置异常: {}", e.getMessage(), e);
        return Result.error(userFriendlyMessage);
    }

    /**
     * HttpRequestMethodNotSupportedException 异常处理返回json
     */
    @ExceptionHandler({HttpRequestMethodNotSupportedException.class})
    public Result<?> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        return defHandler("不支持当前请求方法", e);
    }


    /**
     * 文件上传相关异常（包括大小超限）
     */
    @ExceptionHandler({
            MaxUploadSizeExceededException.class,
            org.springframework.web.multipart.MultipartException.class
    })
    public Result<?> handleFileUploadException(Exception e) {
        log.error("文件上传异常: {}", e.getClass().getName(), e);
        // 其他文件上传异常
        return Result.error(HttpStatus.BAD_REQUEST.value(),
                "文件上传失败: " + e.getMessage(),
                null);
    }

    /**
     * Bean 校验异常 Validate
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        log.error(message, e);
        return Result.error(HttpStatus.BAD_REQUEST.value(), message, null);
    }

    /**
     * 方法参数校验异常 Validate
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public Result<?> handleValidationException(ConstraintViolationException e) {
        String message = e.getConstraintViolations().iterator().next().getMessage();
        log.error(message, e);
        return Result.error(HttpStatus.BAD_REQUEST.value(), message, null);
    }

    /**
     * 方法参数校验异常 Validate
     */
    @ExceptionHandler(BindException.class)
    public Result<?> handleBindException(BindException e) {
        BindingResult bindingResult = e.getBindingResult();
        String message = bindingResult.getAllErrors().iterator().next().getDefaultMessage();
        log.error(message, e);
        return Result.error(HttpStatus.BAD_REQUEST.value(), message, null);
    }

    /**
     * 处理客户端断开连接异常
     * 这种异常通常发生在用户刷新页面、快速切换文件、网络中断等场景
     * 不需要记录为错误日志
     */
    @ExceptionHandler({
            org.springframework.web.context.request.async.AsyncRequestNotUsableException.class,
            java.io.IOException.class
    })
    public Result<?> handleClientAbortException(Exception e) {
        if (isClientAbortException(e)) {
            log.debug("客户端断开连接: {}", e.getMessage());
            // 返回空结果，避免二次异常
            return null;
        }
        // 不是客户端断开异常，按正常异常处理
        return defHandler("系统异常，请联系管理员！", e);
    }

    /**
     * 处理所有不可知的异常
     */
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        return defHandler("系统异常，请联系管理员！", e);
    }

    /**
     * 统一返回
     */
    private Result<?> defHandler(String msg, Exception e) {
        log.error(msg, e);
        return Result.error(msg);
    }

    /**
     * 判断是否为客户端断开连接异常
     */
    private boolean isClientAbortException(Throwable e) {
        if (e == null) {
            return false;
        }

        String className = e.getClass().getName();
        String message = e.getMessage();

        // 检查异常类型
        if (className.contains("ClientAbortException") 
                || className.contains("AsyncRequestNotUsableException")
                || className.contains("EOFException")
                || className.contains("SocketException")) {
            return true;
        }

        // 检查异常消息
        if (message != null) {
            String lowerMessage = message.toLowerCase();
            if (lowerMessage.contains("broken pipe")
                    || lowerMessage.contains("connection reset")
                    || lowerMessage.contains("connection abort")
                    || lowerMessage.contains("stream closed")
                    || lowerMessage.contains("client abort")
                    || lowerMessage.contains("outputstream failed")) {
                return true;
            }
        }

        // 递归检查 cause
        return isClientAbortException(e.getCause());
    }
}
