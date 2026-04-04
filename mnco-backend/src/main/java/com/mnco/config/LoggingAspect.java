package com.mnco.config;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

/**
 * AOP aspect for automatic performance and error logging on use case services.
 * Logs method entry, exit, duration, and exceptions for all application layer services.
 */
@Slf4j
@Aspect
@Component
public class LoggingAspect {

    @Pointcut("within(com.mnco.application.usecases..*)")
    public void applicationLayer() {}

    @Around("applicationLayer()")
    public Object logUseCaseExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().toShortString();
        long startTime = System.currentTimeMillis();

        log.debug("→ USE CASE: {}", methodName);

        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;
            log.debug("← USE CASE: {} completed in {}ms", methodName, duration);
            return result;
        } catch (Exception ex) {
            long duration = System.currentTimeMillis() - startTime;
            log.warn("✗ USE CASE: {} failed after {}ms — {}: {}",
                    methodName, duration, ex.getClass().getSimpleName(), ex.getMessage());
            throw ex;
        }
    }
}
