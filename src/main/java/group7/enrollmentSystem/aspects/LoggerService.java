package group7.enrollmentSystem.aspects;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;

@Aspect
@Component
public class LoggerService {

    private static final Logger logger = LoggerFactory.getLogger(LoggerService.class);

    // Pointcut for all methods in services and controllers
    @Pointcut("within(@org.springframework.stereotype.Service *) || within(@org.springframework.stereotype.Controller *) || within(@org.springframework.web.bind.annotation.RestController *)")
    public void loggableBeans() {}

    @Around("loggableBeans()")
    public Object logExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().toShortString();
        Object[] args = joinPoint.getArgs();
        LocalDateTime start = LocalDateTime.now();

        log(">>> " + methodName + " called at " + start);
        log("    Arguments: " + Arrays.toString(args));

        try {
            Object result = joinPoint.proceed();
            log("<<< " + methodName + " returned: " + result);
            return result;
        } catch (Throwable ex) {
            log("!!! Exception in " + methodName + ": " + ex.getClass().getSimpleName() + " - " + ex.getMessage());
            Arrays.stream(ex.getStackTrace()).limit(5).forEach(ste -> log("    at " + ste));
            throw ex;
        } finally {
            log("    Completed at " + LocalDateTime.now() + " [" + methodName + "]");
            log("---------------------------------------------------");
        }
    }

    private void log(String message) {
        logger.info(message); // logs to generalSystemLogs.txt if SLF4J is set to file
    }
}
