package com.company.mts.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Aspect
@Component
public class LoggingAspect {

    private static final Logger logger = LoggerFactory.getLogger(LoggingAspect.class);

    /**
     * Pointcut for all methods in service layer
     */
    @Pointcut("execution(* com.company.mts.service..*(..))")
    public void serviceLayer() {
    }

    /**
     * Pointcut for all methods in controller layer
     */
    @Pointcut("execution(* com.company.mts.controller..*(..))")
    public void controllerLayer() {
    }

    /**
     * Pointcut for all repository methods
     */
    @Pointcut("execution(* com.company.mts.repository..*(..))")
    public void repositoryLayer() {
    }

    /**
     * Before advice for service methods
     */
    @Before("serviceLayer()")
    public void logBeforeServiceMethod(JoinPoint joinPoint) {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        Object[] args = joinPoint.getArgs();

        logger.info("==> SERVICE CALL: {}.{}() with arguments: {}",
                className, methodName, Arrays.toString(args));
    }

    /**
     * After returning advice for service methods
     */
    @AfterReturning(pointcut = "serviceLayer()", returning = "result")
    public void logAfterServiceMethod(JoinPoint joinPoint, Object result) {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();

        logger.info("<== SERVICE RETURN: {}.{}() returned: {}",
                className, methodName, result != null ? result.getClass().getSimpleName() : "null");
    }

    /**
     * After throwing advice for service methods
     */
    @AfterThrowing(pointcut = "serviceLayer()", throwing = "exception")
    public void logAfterServiceException(JoinPoint joinPoint, Throwable exception) {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();

        logger.error("<!> SERVICE EXCEPTION in {}.{}(): {} - {}",
                className, methodName, exception.getClass().getSimpleName(), exception.getMessage());
    }

    /**
     * Around advice for controller methods to measure execution time
     */
    @Around("controllerLayer()")
    public Object logAroundControllerMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();

        long startTime = System.currentTimeMillis();

        logger.info(">>> CONTROLLER REQUEST: {}.{}()", className, methodName);

        Object result;
        try {
            result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - startTime;

            logger.info("<<< CONTROLLER RESPONSE: {}.{}() completed in {}ms",
                    className, methodName, executionTime);

            return result;
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;

            logger.error("<<< CONTROLLER ERROR: {}.{}() failed after {}ms - {}: {}",
                    className, methodName, executionTime,
                    e.getClass().getSimpleName(), e.getMessage());

            throw e;
        }
    }

    /**
     * Before advice for repository methods (only for custom queries)
     */
    @Before("repositoryLayer() && !execution(* org.springframework.data.repository.CrudRepository+.*(..))")
    public void logBeforeRepositoryMethod(JoinPoint joinPoint) {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();

        logger.debug("--- REPOSITORY QUERY: {}.{}()", className, methodName);
    }

    /**
     * Around advice for transaction methods to track performance
     */
    @Around("execution(* com.company.mts.service.TransactionService.executeIdempotentTransfer(..))")
    public Object logTransferExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();

        logger.info("$$$ TRANSFER INITIATED: From Account={}, To Account={}, Amount={}, IdempotencyKey={}",
                args[0], args[1], args[2], args[3]);

        long startTime = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - startTime;

            logger.info("$$$ TRANSFER SUCCESS: Completed in {}ms, IdempotencyKey={}",
                    executionTime, args[3]);

            return result;
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;

            logger.error("$$$ TRANSFER FAILED: After {}ms, IdempotencyKey={}, Error: {}",
                    executionTime, args[3], e.getMessage());

            throw e;
        }
    }
}