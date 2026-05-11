package com.nieqi.nieqicodefather.aop;

import com.nieqi.nieqicodefather.annotation.AuthCheck;
import com.nieqi.nieqicodefather.constant.UserConstant;
import com.nieqi.nieqicodefather.exception.BusinessException;
import com.nieqi.nieqicodefather.exception.ErrorCode;
import com.nieqi.nieqicodefather.model.entity.User;
import com.nieqi.nieqicodefather.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

/**
 * 统一权限校验切面。
 * 仅当方法或类上标注了 {@link AuthCheck} 时执行校验。
 */
@Aspect
@Component
public class AuthCheckAspect {

    @Resource
    private UserService userService;

    @Around("execution(* com.nieqi.nieqicodefather.controller..*(..))")
    public Object checkAuth(ProceedingJoinPoint joinPoint) throws Throwable {
        AuthCheck authCheck = getAuthCheck(joinPoint);
        if (authCheck == null) {
            return joinPoint.proceed();
        }

        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "请先登录");
        }
        HttpServletRequest request = attributes.getRequest();
        User loginUser = userService.getLoginUser(request);
        String mustRole = authCheck.mustRole();
        if (mustRole != null && !mustRole.isBlank() && !mustRole.equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限访问");
        }
        return joinPoint.proceed();
    }

    private AuthCheck getAuthCheck(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        AuthCheck methodAuthCheck = method.getAnnotation(AuthCheck.class);
        if (methodAuthCheck != null) {
            return methodAuthCheck;
        }
        return method.getDeclaringClass().getAnnotation(AuthCheck.class);
    }
}

