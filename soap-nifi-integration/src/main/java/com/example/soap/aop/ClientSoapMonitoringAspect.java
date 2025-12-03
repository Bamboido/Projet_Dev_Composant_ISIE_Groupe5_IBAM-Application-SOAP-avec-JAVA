package com.example.soap.aop;

import com.example.soap.annotation.MonitorSoapMethod;
import com.example.soap.metrics.SoapMetricsService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class ClientSoapMonitoringAspect {  // ← Changer le nom de la classe ici
    
    private final SoapMetricsService metricsService;
    
    public ClientSoapMonitoringAspect(SoapMetricsService metricsService) {  // ← Et ici
        this.metricsService = metricsService;
    }
    
    @Around("@annotation(monitorSoapMethod)")
    public Object monitorSoapMethod(ProceedingJoinPoint joinPoint, MonitorSoapMethod monitorSoapMethod) throws Throwable {
        String methodName = monitorSoapMethod.value().isEmpty() 
            ? getMethodName(joinPoint) 
            : monitorSoapMethod.value();
        
        long startTime = System.currentTimeMillis();
        boolean success = false;
        
        try {
            metricsService.incrementRequest(methodName);
            Object result = joinPoint.proceed();
            success = true;
            return result;
        } catch (Exception e) {
            metricsService.incrementError(methodName, e.getClass().getSimpleName());
            throw e;
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            metricsService.recordRequest(methodName, startTime, success);
        }
    }
    
    private String getMethodName(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        return signature.getDeclaringType().getSimpleName() + "." + signature.getName();
    }
}