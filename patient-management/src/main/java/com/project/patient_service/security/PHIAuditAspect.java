package com.project.patient_service.security;

import com.project.patient_service.model.AuditLog;
import com.project.patient_service.repository.AuditLogRepository;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class PHIAuditAspect {

    private final AuditLogRepository auditLogRepository;

    public PHIAuditAspect(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Pointcut("execution(* com.project.patient_service.service.MedicalRecordService.get*(..))")
    public void medicalRecordAccess() {}

    @AfterReturning(pointcut = "medicalRecordAccess()")
    public void auditMedicalRecordAccess(JoinPoint joinPoint) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = (auth != null) ? auth.getName() : "SYSTEM";
        
        Object[] args = joinPoint.getArgs();
        String resourceId = (args.length > 0) ? args[0].toString() : "UNKNOWN";

        AuditLog log = new AuditLog(
            username,
            "ACCESS_PHI",
            resourceId,
            "MedicalRecord",
            "Method: " + joinPoint.getSignature().getName()
        );
        
        auditLogRepository.save(log);
    }
}
