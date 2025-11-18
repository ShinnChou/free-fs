package com.xddcodec.fs.system.aspect;

import com.xddcodec.fs.framework.common.exception.BusinessException;
import com.xddcodec.fs.framework.common.utils.Ip2RegionUtils;
import com.xddcodec.fs.framework.common.utils.IpUtils;
import com.xddcodec.fs.log.domain.event.CreateLoginLogEvent;
import com.xddcodec.fs.system.domain.vo.LoginUserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 登录日志切面
 *
 * @Author: xddcodec
 * @Date: 2025/9/25 14:35
 */
@Slf4j
@Aspect
@Component
@Order(1)
@RequiredArgsConstructor
public class LoginLogAspect {
    private final ApplicationEventPublisher eventPublisher;

    @Pointcut("@annotation(com.xddcodec.fs.log.annotation.LoginLog)")
    public void loginLogPointcut() {
    }

    @Around("loginLogPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取登录相关信息
        String ip = IpUtils.getIpAddr();
        String address = Ip2RegionUtils.search(ip);
        String browser = IpUtils.getBrowser();
        String os = IpUtils.getOs();
        String userAgent = IpUtils.getUserAgent();
        // 获取用户名
        String username = extractUsername(joinPoint);

        long startTime = System.currentTimeMillis();

        try {
            // 执行目标方法
            Object result = joinPoint.proceed();

            // 登录成功，从返回值中提取用户信息
            if (result instanceof LoginUserVO loginUserVO) {
                String userId = loginUserVO.getId() != null ? loginUserVO.getId() : null;
                String actualUsername = loginUserVO.getUsername() != null ? loginUserVO.getUsername() : username;

                // 发布登录成功事件
                eventPublisher.publishEvent(
                        CreateLoginLogEvent.success(this, userId, actualUsername, ip, address, browser, os, userAgent)
                );

                long endTime = System.currentTimeMillis();
                log.info("登录成功: 用户[{}], IP[{}], 耗时[{}ms]", actualUsername, ip, endTime - startTime);
            }

            return result;

        } catch (Exception e) {
            // 系统异常
            publishFailureEvent(username, ip, address, browser, os, userAgent, e.getMessage());

            long endTime = System.currentTimeMillis();
            log.error("登录异常: 用户[{}], IP[{}], 耗时[{}ms]",
                    username, ip, endTime - startTime, e);
            //重新抛出去让统一异常处理器处理
            throw new BusinessException(e.getMessage());
        }
    }

    /**
     * 从方法参数中提取用户名
     */
    private String extractUsername(ProceedingJoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args != null && args.length > 0) {
            Object firstArg = args[0];
            if (firstArg != null) {
                try {
                    Method getUsernameMethod = firstArg.getClass().getMethod("getUsername");
                    Object username = getUsernameMethod.invoke(firstArg);
                    return username != null ? username.toString() : "unknown";
                } catch (Exception e) {
                    log.debug("无法从参数中提取用户名", e);
                }
            }
        }
        return "unknown";
    }

    /**
     * 发布登录失败事件
     */
    private void publishFailureEvent(String username, String ip, String address,
                                     String browser, String os, String userAgent, String message) {
        eventPublisher.publishEvent(
                CreateLoginLogEvent.failure(this, username, ip, address, browser, os, message, userAgent)
        );
    }
}