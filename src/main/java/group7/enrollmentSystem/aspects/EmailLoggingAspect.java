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

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Aspect
@Component
public class EmailLoggingAspect {

    private static final Logger logger = LoggerFactory.getLogger(EmailLoggingAspect.class);
    private static final String LOG_DIRECTORY = "logs";
    private static final String LOG_FILE = LOG_DIRECTORY + "/email-logs-" +
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmmss")) + ".log";
    private static PrintWriter logFileWriter;

    static {
        initializeLogFile();
    }

    private static void initializeLogFile() {
        try {
            // Create logs directory if it doesn't exist
            Files.createDirectories(Paths.get(LOG_DIRECTORY));

            // Initialize log file writer
            logFileWriter = new PrintWriter(new FileWriter(LOG_FILE, true));

            // Register shutdown hook to properly close the writer
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                if (logFileWriter != null) {
                    logToFile("Application shutting down - closing log file");
                    logFileWriter.close();
                }
            }));

            logToFile("========== Application Started ==========");
            logToFile("Logging initialized at: " + LocalDateTime.now());
            logger.info("Log file initialized at: {}", LOG_FILE);
        } catch (IOException e) {
            logger.error("Failed to initialize log file writer", e);
            throw new RuntimeException("Failed to initialize log file writer", e);
        }
    }

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

    @Before("emailNotificationMethods()")
    public void logBeforeEmail(JoinPoint joinPoint) {
        if (shouldSample()) {
            String methodName = joinPoint.getSignature().getName();
            Object[] args = joinPoint.getArgs();
            String recipient = (String) args[0];

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

            String logMessage = String.format("[%s] Preparing to send email - %s",
                    LocalDateTime.now(), context);
            logger.info(logMessage);
            logToFile(logMessage);
            updateStatistics(methodName, "initiated");
        }
    }

    @AfterReturning(pointcut = "emailNotificationMethods()", returning = "result")
    public void logAfterEmailSuccess(JoinPoint joinPoint, Object result) {
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();
        String templateName = getTemplateNameFromArgs(args);

        String logMessage = String.format("[%s] Email dispatch completed - method: %s, template: %s, status: success",
                LocalDateTime.now(), methodName, templateName);

        logger.info(logMessage);
        logToFile(logMessage);
        totalEmailsProcessed.incrementAndGet();
        updateStatistics(methodName, "success");
    }

    @AfterThrowing(pointcut = "emailNotificationMethods()", throwing = "ex")
    public void logAfterEmailFailure(JoinPoint joinPoint, Exception ex) {
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();
        String recipient = args.length > 0 ? (String) args[0] : "unknown";

        String logMessage = String.format("[%s] Email dispatch failed - method: %s, recipient: %s, error: %s",
                LocalDateTime.now(), methodName, maskEmail(recipient), ex.getMessage());

        logger.error(logMessage, ex);
        logToFile(logMessage + "\nStack Trace: " + getStackTrace(ex, 3));
        failedEmails.incrementAndGet();
        updateStatistics(methodName, "failed");
    }

    @Around("emailNotificationMethods()")
    public Object trackEmailExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        StopWatch stopWatch = new StopWatch(methodName);
        String startTime = LocalDateTime.now().toString();

        try {
            stopWatch.start();
            Object result = joinPoint.proceed();
            stopWatch.stop();

            long executionTime = stopWatch.getTotalTimeMillis();
            String logMessage = String.format("[%s] Email executed in %d ms - method: %s",
                    LocalDateTime.now(), executionTime, methodName);

            if (executionTime > executionTimeThreshold) {
                logMessage += String.format(" (EXCEEDED THRESHOLD OF %d ms)", executionTimeThreshold);
                logger.warn(logMessage);
            } else {
                logger.debug(logMessage);
            }

            logToFile(logMessage);
            return result;
        } catch (Exception e) {
            if (stopWatch.isRunning()) {
                stopWatch.stop();
            }
            String errorMessage = String.format("[%s] Email failed after %d ms - method: %s, error: %s",
                    LocalDateTime.now(), stopWatch.getTotalTimeMillis(), methodName, e.getMessage());

            logger.error(errorMessage);
            logToFile(errorMessage);
            throw e;
        }
    }

    @Around("asyncEmailNotifications()")
    public Object monitorAsyncEmails(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        String startTime = LocalDateTime.now().toString();

        String startMessage = String.format("[%s] Starting async email operation: %s",
                startTime, methodName);
        logger.info(startMessage);
        logToFile(startMessage);

        try {
            Object result = joinPoint.proceed();
            String successMessage = String.format("[%s] Async email completed: %s",
                    LocalDateTime.now(), methodName);

            logger.info(successMessage);
            logToFile(successMessage);
            return result;
        } catch (Exception e) {
            String errorMessage = String.format("[%s] Async email failed: %s, error: %s",
                    LocalDateTime.now(), methodName, e.getMessage());

            logger.error(errorMessage, e);
            logToFile(errorMessage + "\nStack Trace: " + getStackTrace(e, 3));
            throw e;
        }
    }

    @Scheduled(fixedRateString = "${email.monitoring.report.interval:1800000}")
    public void reportEmailStatistics() {
        if (!emailStatistics.isEmpty()) {
            StringBuilder report = new StringBuilder();
            report.append(String.format("[%s] Email Statistics Report\n", LocalDateTime.now()));
            report.append(String.format("Total Emails Processed: %d\n", totalEmailsProcessed.get()));
            report.append(String.format("Failed Emails: %d\n", failedEmails.get()));
            report.append(String.format("Success Rate: %.2f%%\n", calculateSuccessRate()));

            report.append("Method Statistics:\n");
            emailStatistics.forEach((method, stats) -> {
                report.append(String.format("  %s - %s\n", method, stats.getSummary()));
            });

            String reportMessage = report.toString();
            logger.info(reportMessage);
            logToFile(reportMessage);
        }
    }

    // Helper Methods
    private static synchronized void logToFile(String message) {
        try {
            if (logFileWriter != null) {
                logFileWriter.println(message);
                logFileWriter.flush();
            }
        } catch (Exception e) {
            logger.error("Failed to write to log file", e);
        }
    }

    private String getTemplateNameFromArgs(Object[] args) {
        return args.length > 2 && args[2] instanceof String ? (String) args[2] : "unknown";
    }

    private String getStackTrace(Exception ex, int depth) {
        StringBuilder sb = new StringBuilder();
        StackTraceElement[] stackTrace = ex.getStackTrace();
        for (int i = 0; i < Math.min(depth, stackTrace.length); i++) {
            sb.append("\n\tat ").append(stackTrace[i]);
        }
        return sb.toString();
    }

    private String maskEmail(String email) {
        if (email == null || email.length() < 5) return "*****";
        int atIndex = email.indexOf('@');
        return atIndex < 3 ? "*****" + email.substring(atIndex)
                : email.substring(0, 2) + "*****" + email.substring(atIndex);
    }

    private boolean shouldSample() {
        return random.nextDouble() <= sampleRate;
    }

    private double calculateSuccessRate() {
        int total = totalEmailsProcessed.get();
        return total > 0 ? (total - failedEmails.get()) * 100.0 / total : 100.0;
    }

    private void updateStatistics(String methodName, String status) {
        emailStatistics.computeIfAbsent(methodName, k -> new EmailStatistics())
                .update(status);
    }

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