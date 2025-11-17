package com.xddcodec.fs.aspectj;


import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;

/**
 * 请求日志AOP
 *
 * @author Yann
 * @date 2025/10/10 14:45
 */
@Profile("dev")
@Slf4j
@Aspect
@Component
public class RequestLoggingAspect {

    /**
     * 定义切点
     */
    @Pointcut("within(@org.springframework.web.bind.annotation.RestController *)")
    public void controllerLog() {}

    /**
     * 方法执行前日志
     */
    @Before("controllerLog()")
    public void logBefore(JoinPoint joinPoint) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return;
        }
        HttpServletRequest request = attributes.getRequest();

        log.info("【Free-Fs】 -> 请求地址: {}, 请求方式: {}, IP: {}",
                request.getRequestURL().toString(),
                request.getMethod(),
                request.getRemoteAddr());
        log.info("【Free-Fs】 -> 请求执行方法: {}.{}",
                joinPoint.getSignature().getDeclaringTypeName(),
                joinPoint.getSignature().getName());
        log.info("【Free-Fs】 -> 请求参数: {}", Arrays.toString(joinPoint.getArgs()));
    }

    /**
     * 在方法成功返回后打印响应信息
     */
    @AfterReturning(pointcut = "controllerLog()", returning = "result")
    public void logAfterReturning(Object result) {
        log.info("【Free-Fs】 -> 响应结果: {}", result);
    }

    /**
     * 环绕通知，可以统计方法执行耗时
     */
    @Around("controllerLog()")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        Object result = joinPoint.proceed(); // 执行目标方法
        long timeTaken = System.currentTimeMillis() - startTime;
        log.info("【Free-Fs】-> 执行耗时： {}.{} | {}{}ms{}",
                joinPoint.getSignature().getDeclaringTypeName(),
                joinPoint.getSignature().getName(),
                "\u001B[33m",
                timeTaken,
                "\u001B[0m");
        return result;
    }
}
