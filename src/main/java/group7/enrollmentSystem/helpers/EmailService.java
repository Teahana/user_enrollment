package group7.enrollmentSystem.helpers;

import group7.enrollmentSystem.config.CustomExceptions;
import group7.enrollmentSystem.enums.OnHoldTypes;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.HashMap;
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
    public void sendHtmlMailAsync(String to, String subject, String templateName, Map<String, Object> model) {
        sendHtmlMail(to, subject, templateName, model);
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

    @Async
    public void notifyStudentHoldAdded(String studentEmail, String studentName, OnHoldTypes holdType) {
        String subject = "Account Hold Notification";
        String holdMessage = CustomExceptions.StudentOnHoldException.getHoldMessage(holdType);

        Map<String, Object> model = new HashMap<>();
        model.put("subject", subject);
        model.put("header", "Hold Placed");
        model.put("body", "<p>Dear " + studentName + ",</p>" +
                "<p>" + holdMessage + "</p>" +
                "<p>Contact the administration for any questions.</p>");

        sendHtmlMail(studentEmail, subject, "notification", model);
    }

    @Async
    public void notifyStudentHoldRemoved(String studentEmail, String studentName, OnHoldTypes holdType) {
        String subject = "Account Hold Notification";
        String holdTypeName = holdType.toString().toLowerCase().replace("_", " ");

        Map<String, Object> model = new HashMap<>();
        model.put("subject", subject);
        model.put("header", "Hold Removed");
        model.put("body", "<p>Dear " + studentName + ",</p>" +
                "<p>The <strong>" + holdTypeName + " </strong>hold has been removed from your account.</p>" +
                "<p>You can now access all available services restricted by this hold.</p>");

        sendHtmlMail(studentEmail, subject, "notification", model);
    }

    @Async
    public void notifyAdminHoldChange(String adminEmail, String studentName, String studentEmail, OnHoldTypes holdType, boolean added) {
        String action = added ? "added" : "removed";
        String subject = "Student Hold " + action.substring(0, 1).toUpperCase() + action.substring(1);
        String holdTypeName = holdType.toString().toLowerCase().replace("_", " ");

        Map<String, Object> model = new HashMap<>();
        model.put("subject", subject);
        model.put("header", "Hold " + action + " for Student");
        model.put("body", "<p>A <strong>" + holdTypeName + " </strong>hold has been " + action + " for :</p>" +
                "<p><strong>Student Name:</strong> " + studentName + "</p>" +
                "<p><strong>Student Email:</strong> " + studentEmail + "</p>");

        sendHtmlMail(adminEmail, subject, "notification", model);
    }

    @Async
    public void notifyStudentApplicationStatusUpdate(String studentEmail, String fullName, String studentId, String applicationType, String status) {
        Map<String, Object> model = new HashMap<>();
        model.put("fullName", fullName);
        model.put("studentId", studentId);
        model.put("applicationType", applicationType);
        model.put("status", status);

        sendHtmlMail(studentEmail, "Your Application Status Update", "status_report", model);
    }

    @Async
    public void notifyAdminApplicationStatusChange(String adminEmail, String fullName, String studentId, String applicationType, String status) {
        Map<String, Object> model = new HashMap<>();
        model.put("fullName", fullName);
        model.put("studentId", studentId);
        model.put("applicationType", applicationType);
        model.put("status", status);

        sendHtmlMail(adminEmail, "Student Application Status Updated", "status_report", model);
    }

}

