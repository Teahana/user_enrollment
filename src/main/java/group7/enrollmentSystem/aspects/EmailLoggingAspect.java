package group7.enrollmentSystem.aspects;

import jakarta.mail.MessagingException;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Aspect
@Component
public class EmailLoggingAspect {

    @Value("${email.monitoring.threshold.ms:5000}")
    private long executionTimeThreshold;

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter FILE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final AtomicInteger emailCounter = new AtomicInteger(0);
    private final ThreadLocal<Deque<Integer>> emailIdStack = ThreadLocal.withInitial(ArrayDeque::new);
    private final ThreadLocal<Long> startTime = ThreadLocal.withInitial(System::currentTimeMillis);
    private final String logFileName;

    public EmailLoggingAspect() {
        this.logFileName = "logs/email_logs_" + LocalDateTime.now().format(FILE_DATE_FORMAT) + ".log";
        createLogsDirectory();
    }

    @Pointcut("execution(* group7.enrollmentSystem.helpers.EmailService.*(..))")
    public void emailSendingMethods() {}

    @Before("emailSendingMethods()")
    public void logBeforeEmailSending(JoinPoint joinPoint) {
        int currentEmailId = emailCounter.incrementAndGet();
        emailIdStack.get().push(currentEmailId);
        startTime.set(System.currentTimeMillis());

        String methodName = joinPoint.getSignature().getName();
        String recipient = getRecipient(joinPoint.getArgs());
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);

        String logMessage = String.format("[%s] SENDING EMAIL [E%d] - Recipient(s): %s | Method: %s",
                timestamp, currentEmailId, recipient, methodName);

        logToFile(logMessage);
    }

    @AfterReturning("emailSendingMethods()")
    public void logAfterEmailSuccess(JoinPoint joinPoint) {
        int currentEmailId = emailIdStack.get().peek();
        String methodName = joinPoint.getSignature().getName();
        String recipient = getRecipient(joinPoint.getArgs());
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);

        long duration = System.currentTimeMillis() - startTime.get();

        // Log email details if available
        if (hasEmailDetails(joinPoint.getArgs())) {
            logEmailDetails(currentEmailId, timestamp, joinPoint.getArgs());
        }

        String successLog = String.format("[%s] EMAIL [E%d] SENT SUCCESSFULLY - Recipient(s): %s | Method: %s",
                timestamp, currentEmailId, recipient, methodName);
        logToFile(successLog);

        String timeLog = String.format("[%s] EMAIL [E%d] PROCESSING TIME - Recipient(s): %s | Method: %s | Time: %d ms%s",
                timestamp, currentEmailId, recipient, methodName, duration,
                duration > executionTimeThreshold ? " (WARNING: Exceeded threshold of " + executionTimeThreshold + " ms)" : "");
        logToFile(timeLog);

        emailIdStack.get().pop();
    }

    @AfterThrowing(pointcut = "emailSendingMethods()", throwing = "ex")
    public void logAfterEmailFailure(JoinPoint joinPoint, Throwable ex) {
        int currentEmailId = emailIdStack.get().pop();
        String methodName = joinPoint.getSignature().getName();
        String recipient = getRecipient(joinPoint.getArgs());
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);

        long duration = System.currentTimeMillis() - startTime.get();

        String errorLog = String.format("[%s] EMAIL [E%d] SEND FAILED - Recipient(s): %s | Method: %s | Error: %s",
                timestamp, currentEmailId, recipient, methodName, ex.getMessage());
        logToFile(errorLog);

        String timeLog = String.format("[%s] EMAIL [E%d] PROCESSING TIME - Recipient(s): %s | Method: %s | Time: %d ms",
                timestamp, currentEmailId, recipient, methodName, duration);
        logToFile(timeLog);
    }

    private boolean hasEmailDetails(Object[] args) {
        return args.length >= 4 && args[3] instanceof Map;
    }

    private String getRecipient(Object[] args) {
        return args.length > 0 ? args[0].toString() : "Unknown";
    }

    private void logEmailDetails(int emailId, String timestamp, Object[] args) {
        try {
            StringBuilder details = new StringBuilder();
            details.append(String.format("EMAIL [E%d] DETAILS:\n", emailId));

            String to = args.length > 0 ? args[0].toString() : "Unknown";
            String subject = args.length > 1 ? args[1].toString() : "Unknown";
            String templateName = args.length > 2 ? args[2].toString() : "Unknown";
            @SuppressWarnings("unchecked")
            Map<String, Object> model = args.length > 3 ? (Map<String, Object>) args[3] : null;

            details.append(String.format("  Time: %s\n", timestamp));
            details.append(String.format("  Recipient: %s\n", to));
            details.append(String.format("  Subject: %s\n", subject));
            details.append(String.format("  Template: %s\n", templateName));

            if (model != null) {
                details.append("  Model Content:\n");
                model.forEach((key, value) ->
                        details.append(String.format("    %s: %s\n", key, value)));
            }

            logToFile(details.toString());
        } catch (Exception e) {
            System.err.println("Failed to log email details: " + e.getMessage());
        }
    }

    private void createLogsDirectory() {
        try {
            Files.createDirectories(Paths.get("logs"));
        } catch (IOException e) {
            System.err.println("Failed to create logs directory: " + e.getMessage());
        }
    }

    private void logToFile(String content) {
        // Log to console
        System.out.println(content);

        // Log to file
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFileName, true))) {
            writer.write(content);
            writer.newLine();
            writer.newLine();
        } catch (IOException e) {
            System.err.println("Failed to write to log file: " + e.getMessage());
        }
    }
}