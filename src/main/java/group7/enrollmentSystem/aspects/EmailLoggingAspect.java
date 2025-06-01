package group7.enrollmentSystem.aspects;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Aspect
@Component
public class EmailLoggingAspect {

    private static final Logger logger = LoggerFactory.getLogger(EmailLoggingAspect.class);
    private final Map<String, EmailStatistics> emailStatistics = new ConcurrentHashMap<>();
    private final AtomicInteger totalEmailsProcessed = new AtomicInteger(0);
    private final AtomicInteger failedEmails = new AtomicInteger(0);

    @Value("${email.monitoring.threshold.ms:5000}")
    private long executionTimeThreshold;

    @Value("${email.monitoring.sample.rate:0.1}")
    private double sampleRate;

    private final Random random = new Random();

    // Pointcut for all email notification methods
    @Pointcut("execution(* group7.enrollmentSystem.helpers.EmailService.notify*(..))")
    public void emailNotificationMethods() {}

    // Pointcut for async email methods
    @Pointcut("@annotation(org.springframework.scheduling.annotation.Async)")
    public void asyncMethods() {}

    // Composite pointcut
    @Pointcut("emailNotificationMethods() && asyncMethods()")
    public void asyncEmailNotifications() {}

    // Log before email is sent
    @Before("emailNotificationMethods()")
    public void logBeforeEmail(JoinPoint joinPoint) {
        if (shouldSample()) {
            String methodName = joinPoint.getSignature().getName();
            Object[] args = joinPoint.getArgs();
            String recipient = (String) args[0]; // First argument is always email

            Map<String, Object> context = new HashMap<>();
            context.put("method", methodName);
            context.put("recipient", maskEmail(recipient));
            context.put("thread", Thread.currentThread().getName());
            context.put("timestamp", System.currentTimeMillis());

            if (args.length > 1 && args[1] instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> model = (Map<String, Object>) args[1];
                context.put("templateDataKeys", model.keySet());
            }

            logger.info("Preparing to send email with context: {}", context);
            updateStatistics(methodName, "initiated");
        }
    }

    // Log after successful email
    @AfterReturning(pointcut = "emailNotificationMethods()", returning = "result")
    public void logAfterEmailSuccess(JoinPoint joinPoint, Object result) {
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();
        String templateName = getTemplateNameFromArgs(args);

        Map<String, Object> successContext = new HashMap<>();
        successContext.put("method", methodName);
        successContext.put("template", templateName);
        successContext.put("status", "success");

        logger.info("Email dispatch completed: {}", successContext);
        totalEmailsProcessed.incrementAndGet();
        updateStatistics(methodName, "success");
    }

    // email failure logging
    @AfterThrowing(pointcut = "emailNotificationMethods()", throwing = "ex")
    public void logAfterEmailFailure(JoinPoint joinPoint, Exception ex) {
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();
        String recipient = args.length > 0 ? (String) args[0] : "unknown";

        Map<String, Object> failureContext = new HashMap<>();
        failureContext.put("method", methodName);
        failureContext.put("recipient", maskEmail(recipient));
        failureContext.put("exception", ex.getClass().getSimpleName());
        failureContext.put("rootCause", getRootCause(ex).getMessage());
        failureContext.put("stackTrace", Arrays.stream(ex.getStackTrace())
                .limit(3)
                .map(StackTraceElement::toString)
                .toArray());

        logger.error("Email dispatch failed with context: {}", failureContext, ex);
        failedEmails.incrementAndGet();
        updateStatistics(methodName, "failed");
    }

    // execution time tracking
    @Around("emailNotificationMethods()")
    public Object trackEmailExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        StopWatch stopWatch = new StopWatch(methodName);

        try {
            stopWatch.start();
            Object result = joinPoint.proceed();
            stopWatch.stop();

            long executionTime = stopWatch.getTotalTimeMillis();
            if (executionTime > executionTimeThreshold) {
                logger.warn("Email notification took {} ms (exceeded threshold of {} ms) for method {}",
                        executionTime, executionTimeThreshold, methodName);
            }

            logger.debug("Email notification executed in {} ms", executionTime);
            return result;
        } catch (Exception e) {
            if (stopWatch.isRunning()) {
                stopWatch.stop();
            }
            logger.error("Email notification failed after {} ms", stopWatch.getTotalTimeMillis());
            throw e;
        }
    }

    // Monitor async email operations (without correlation ID)
    @Around("asyncEmailNotifications()")
    public Object monitorAsyncEmails(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();

        logger.info("Starting async email operation [{}]", methodName);

        try {
            Object result = joinPoint.proceed();
            logger.info("Async email operation [{}] submitted successfully", methodName);
            return result;
        } catch (Exception e) {
            logger.error("Async email operation [{}] failed during submission", methodName, e);
            throw e;
        }
    }

    // Scheduled method to report email statistics
    @Scheduled(fixedRateString = "${email.monitoring.report.interval:60000}")
    public void reportEmailStatistics() {
        if (!emailStatistics.isEmpty()) {
            Map<String, Object> report = new LinkedHashMap<>();
            report.put("totalEmailsProcessed", totalEmailsProcessed.get());
            report.put("failedEmails", failedEmails.get());
            report.put("successRate", calculateSuccessRate());

            Map<String, Object> methodStats = new HashMap<>();
            emailStatistics.forEach((method, stats) -> {
                methodStats.put(method, stats.getSummary());
            });
            report.put("methodStatistics", methodStats);

            logger.info("Email Statistics Report:\n{}", formatReport(report));
        }
    }

    // Helper methods
    //----------------------------------//
    private String getTemplateNameFromArgs(Object[] args) {
        if (args.length > 2 && args[2] instanceof String) {
            return (String) args[2];
        }
        return "unknown";
    }

    private Throwable getRootCause(Throwable throwable) {
        Throwable rootCause = throwable;
        while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
            rootCause = rootCause.getCause();
        }
        return rootCause;
    }

    private String maskEmail(String email) {
        if (email == null || email.length() < 5) return "*****";
        int atIndex = email.indexOf('@');
        if (atIndex < 3) return "*****" + email.substring(atIndex);
        return email.substring(0, 2) + "*****" + email.substring(atIndex);
    }

    private boolean shouldSample() {
        return random.nextDouble() <= sampleRate;
    }

    private double calculateSuccessRate() {
        int total = totalEmailsProcessed.get();
        return total > 0 ? (total - failedEmails.get()) * 100.0 / total : 100.0;
    }

    private String formatReport(Map<String, Object> report) {
        StringBuilder sb = new StringBuilder();
        report.forEach((key, value) -> sb.append(String.format("%-25s: %s%n", key, value)));
        return sb.toString();
    }

    private void updateStatistics(String methodName, String status) {
        emailStatistics.computeIfAbsent(methodName, k -> new EmailStatistics())
                .update(status);
    }

    // Nested class for statistics tracking
    private static class EmailStatistics {
        private final AtomicInteger initiated = new AtomicInteger();
        private final AtomicInteger success = new AtomicInteger();
        private final AtomicInteger failed = new AtomicInteger();

        public void update(String status) {
            switch (status) {
                case "initiated" -> initiated.incrementAndGet();
                case "success" -> success.incrementAndGet();
                case "failed" -> failed.incrementAndGet();
            }
        }

        public Map<String, Object> getSummary() {
            Map<String, Object> summary = new HashMap<>();
            summary.put("initiated", initiated.get());
            summary.put("success", success.get());
            summary.put("failed", failed.get());
            summary.put("successRate", initiated.get() > 0 ?
                    String.format("%.2f%%", success.get() * 100.0 / initiated.get()) : "N/A");
            return summary;
        }
    }
}