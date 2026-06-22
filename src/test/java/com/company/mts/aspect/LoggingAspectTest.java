package com.company.mts.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoggingAspectTest {

    @Mock
    private JoinPoint joinPoint;

    @Mock
    private ProceedingJoinPoint proceedingJoinPoint;

    @Mock
    private Signature signature;

    @InjectMocks
    private LoggingAspect loggingAspect;

    @BeforeEach
    void setUp() {
        // Pointcuts do nothing but we call them for coverage
        loggingAspect.serviceLayer();
        loggingAspect.controllerLayer();
        loggingAspect.repositoryLayer();
    }

    @Test
    void logBeforeServiceMethod_Success() {
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("testMethod");
        when(joinPoint.getTarget()).thenReturn(new Object());
        when(joinPoint.getArgs()).thenReturn(new Object[]{"arg1"});

        assertDoesNotThrow(() -> loggingAspect.logBeforeServiceMethod(joinPoint));
    }

    @Test
    void logAfterServiceMethod_Success() {
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("testMethod");
        when(joinPoint.getTarget()).thenReturn(new Object());

        // Result not null
        assertDoesNotThrow(() -> loggingAspect.logAfterServiceMethod(joinPoint, "resultString"));
        // Result null
        assertDoesNotThrow(() -> loggingAspect.logAfterServiceMethod(joinPoint, null));
    }

    @Test
    void logAfterServiceException_Success() {
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("testMethod");
        when(joinPoint.getTarget()).thenReturn(new Object());

        assertDoesNotThrow(() -> loggingAspect.logAfterServiceException(joinPoint, new RuntimeException("err")));
    }

    @Test
    void logAroundControllerMethod_Success() throws Throwable {
        when(proceedingJoinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("testCtrlMethod");
        when(proceedingJoinPoint.getTarget()).thenReturn(new Object());
        when(proceedingJoinPoint.proceed()).thenReturn("ctrlResult");

        Object result = loggingAspect.logAroundControllerMethod(proceedingJoinPoint);
        assertEquals("ctrlResult", result);
    }

    @Test
    void logAroundControllerMethod_Exception() throws Throwable {
        when(proceedingJoinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("testCtrlMethod");
        when(proceedingJoinPoint.getTarget()).thenReturn(new Object());
        when(proceedingJoinPoint.proceed()).thenThrow(new RuntimeException("CtrlException"));

        assertThrows(RuntimeException.class, () -> loggingAspect.logAroundControllerMethod(proceedingJoinPoint));
    }

    @Test
    void logBeforeRepositoryMethod_Success() {
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("findByName");
        when(joinPoint.getTarget()).thenReturn(new Object());

        assertDoesNotThrow(() -> loggingAspect.logBeforeRepositoryMethod(joinPoint));
    }

    @Test
    void logTransferExecution_Success() throws Throwable {
        Object[] args = {1L, 2L, new java.math.BigDecimal("100"), "KEY-123"};
        when(proceedingJoinPoint.getArgs()).thenReturn(args);
        when(proceedingJoinPoint.proceed()).thenReturn("txResult");

        Object result = loggingAspect.logTransferExecution(proceedingJoinPoint);
        assertEquals("txResult", result);
    }

    @Test
    void logTransferExecution_Exception() throws Throwable {
        Object[] args = {1L, 2L, new java.math.BigDecimal("100"), "KEY-123"};
        when(proceedingJoinPoint.getArgs()).thenReturn(args);
        when(proceedingJoinPoint.proceed()).thenThrow(new RuntimeException("TransferException"));

        assertThrows(RuntimeException.class, () -> loggingAspect.logTransferExecution(proceedingJoinPoint));
    }
}
