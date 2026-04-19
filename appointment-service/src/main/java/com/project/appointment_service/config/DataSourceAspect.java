package com.project.appointment_service.config;

import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Aspect
@Component
public class DataSourceAspect {

    @Before("@annotation(transactional) && execution(* com.project.appointment_service..*.*(..))")
    public void setDataSourceContext(Transactional transactional) {
        if (transactional.readOnly()) {
            DataSourceContextHolder.setDbType(DbType.READ);
        } else {
            DataSourceContextHolder.setDbType(DbType.WRITE);
        }
    }

    @After("@annotation(transactional) && execution(* com.project.appointment_service..*.*(..))")
    public void clearDataSourceContext(Transactional transactional) {
        DataSourceContextHolder.clear();
    }
}
