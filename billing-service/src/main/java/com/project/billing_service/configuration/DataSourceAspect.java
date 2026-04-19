package com.project.billing_service.configuration;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Order(1)
public class DataSourceAspect {

    @Around("execution(* com.project.billing_service.query.*Service.*(..))")
    public Object setReadContext(ProceedingJoinPoint joinPoint) throws Throwable {
        DataSourceContextHolder.set(DbType.REPLICA);
        try {
            return joinPoint.proceed();
        } finally {
            DataSourceContextHolder.clear();
        }
    }

    @Around("execution(* com.project.billing_service.command.*Service.*(..))")
    public Object setWriteContext(ProceedingJoinPoint joinPoint) throws Throwable {
        DataSourceContextHolder.set(DbType.MASTER);
        try {
            return joinPoint.proceed();
        } finally {
            DataSourceContextHolder.clear();
        }
    }
}