package com.april.studentmanagementproject.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Aspect
@Component
public class LoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(LoggingAspect.class);

    @Pointcut("within(com.april.studentmanagementproject.controller..*) || within(com.april.studentmanagementproject.service..*)")
    public void applicationLayer() {
    }

    // Runs before matched methods and logs input arguments.
    @Before("applicationLayer()")
    public void logBeforeMethod(JoinPoint jp) {
        log.info("[BEFORE]: Entering {} with args {}", jp.getSignature().toShortString(), Arrays.toString(jp.getArgs()));
    }

    // Runs after matched methods, whether they succeed or fail.
    @After("applicationLayer()")
    public void logAfterMethod(JoinPoint jp) {
        log.info("[AFTER]: Exiting {}", jp.getSignature().toShortString());
    }

    // Wraps matched methods to measure execution time.
    @Around("applicationLayer()")
    public Object logMethodExecution(ProceedingJoinPoint jp) throws Throwable {
        long startTime = System.currentTimeMillis();
        String methodName = jp.getSignature().toShortString();

        try {
            Object result = jp.proceed();
            long duration = System.currentTimeMillis() - startTime;
            log.info("[AROUND]: Completed {} in {} ms", methodName, duration);
            return result;
        } catch (Throwable ex) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[AROUND]: Failed {} after {} ms: {}", methodName, duration, ex.getMessage());
            throw ex;
        }
    }
}
