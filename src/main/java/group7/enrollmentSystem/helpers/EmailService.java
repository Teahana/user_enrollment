package group7.enrollmentSystem.helpers;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    public void sendHtmlMail(String to, String subject, String templateName, Map<String, Object> model) {
        try {
            Context context = new Context();
            context.setVariables(model);
            String htmlContent = templateEngine.process(templateName, context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true); // true = is HTML

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send email", e);
        }
    }
    @Async
    public void notifyStudentGradeChangeRequest(String studentEmail, Map<String, Object> studentModel) {
        String subject = "Grade Change Request Notification";
        String templateName = "notification";
        sendHtmlMail(studentEmail, subject, templateName, studentModel);
    }
    @Async
    public void notifyAdminGradeChangeRequest(String mail, Map<String, Object> adminModel) {
        String subject = "Grade Change Request Notification";
        String templateName = "notification";
        sendHtmlMail(mail, subject, templateName, adminModel);
    }

    @Async
    public void notifyAdminNewApplication(String mail, Map<String, Object> adminModel) {
        String subject = "New Application Notification";
        String templateName = "email_admin";
        sendHtmlMail(mail, subject, templateName, adminModel);
    }
    @Async
    public void notifyStudentApplicationSubmission(String mail, Map<String, Object> studentModel) {
        String subject = "New Application Notification";
        String templateName = "email_generic";
        sendHtmlMail(mail, subject, templateName, studentModel);
    }

}

