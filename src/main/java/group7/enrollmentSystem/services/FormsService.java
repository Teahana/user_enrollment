package group7.enrollmentSystem.services;


import group7.enrollmentSystem.dtos.formDtos.CompassionateFormDTO;
import group7.enrollmentSystem.dtos.formDtos.GraduationFormDTO;
import group7.enrollmentSystem.enums.ApplicationStatus;
import group7.enrollmentSystem.helpers.EmailService;
import group7.enrollmentSystem.models.CompassionateApplication;
import group7.enrollmentSystem.models.GraduationApplication;
import group7.enrollmentSystem.models.Programme;
import group7.enrollmentSystem.models.Student;
import group7.enrollmentSystem.repos.CompassionateApplicationRepo;
import group7.enrollmentSystem.repos.GraduationApplicationRepo;
import group7.enrollmentSystem.repos.ProgrammeRepo;
import group7.enrollmentSystem.repos.StudentRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@Service
@RequiredArgsConstructor
public class FormsService {

    private final StudentRepo studentRepo;
    private final ProgrammeRepo programmeRepository;
    private final GraduationApplicationRepo graduationApplicationRepository;
    private final CompassionateApplicationRepo compassionateRepo;
    private final EmailService emailService;

    public void processGraduationForm(String email) {
        GraduationFormDTO form = new GraduationFormDTO();
        Student student = studentRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        Programme programme = programmeRepository.findByProgrammeCode(form.getProgramme())
                .orElseThrow(() -> new RuntimeException("Programme not found"));


        // Save application
        GraduationApplication app = new GraduationApplication();
        app.setStudent(student);
        app.setProgramme(programme);
        app.setSubmittedAt(LocalDateTime.now());
        graduationApplicationRepository.save(app);

        // Send email to admin
        emailService.sendHtmlMail(
                "admin@gmail.com",
                "New Graduation Application Submitted",
                "graduation-email",
                Map.of(
                        "fullName", student.getFirstName() + " " + student.getLastName(),
                        "programme", programme.getName(),
                        "studentId", student.getStudentId()
                )
        );

        // Send confirmation to student
        emailService.sendHtmlMail(
                student.getEmail(),
                "Your Graduation Application Confirmation",
                "graduation-email",
                Map.of(
                        "fullName", student.getFirstName() + " " + student.getLastName(),
                        "programme", programme.getName(),
                        "studentId", student.getStudentId()
                )
        );
    }

    public void submitGraduationApplication(String email, GraduationFormDTO form) {
        Student student = studentRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        if (graduationApplicationRepository.findByStudent_StudentId(student.getStudentId()).isPresent()) {
            throw new RuntimeException("You have already submitted a graduation application.");
        }

        Programme programme = programmeRepository.findByProgrammeCode(form.getProgramme())
                .orElseThrow(() -> new RuntimeException("Programme not found"));

        GraduationApplication app = new GraduationApplication();
        app.setStudent(student);
        app.setProgramme(programme);
        app.setMajor1(form.getMajor1());
        app.setMajor2(form.getMajor2());
        app.setMinor(form.getMinor());
        app.setCeremonyPreference(form.getCeremonyPreference());
        app.setOtherCampus(form.getOtherCampus());
        app.setWillAttend(form.getWillAttend());
        app.setStudentSignature(form.getStudentSignature());
        app.setSignatureDate(form.getSignatureDate());
        app.setSubmittedAt(LocalDateTime.now());
        app.setStatus(ApplicationStatus.PENDING);

        graduationApplicationRepository.save(app);

        // Send email to admin
        emailService.sendHtmlMail(
                "doiglas.m.habu@gmail.com",
                "New Graduation Application Submitted",
                "graduation-email", // your HTML template
                Map.of(
                        "fullName", student.getFirstName() + " " + student.getLastName(),
                        "programme", programme.getName(),
                        "studentId", student.getStudentId()
                )
        );

        // Send confirmation to student
        emailService.sendHtmlMail(
                "22john3na@gmail.com",
                "Your Graduation Application Has Been Received",
                "graduation-email", // reuse same template or create separate one
                Map.of(
                        "fullName", student.getFirstName() + " " + student.getLastName(),
                        "programme", programme.getName(),
                        "studentId", student.getStudentId()
                )
        );
    }

    public GraduationApplication getGraduationApplication(String email) {
        Student student = studentRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Student not found"));
        return graduationApplicationRepository.findByStudent_StudentId(student.getStudentId()).orElse(null);
    }

    public List<CompassionateApplication> getCompassionateApplications(String email) {
        Student student = studentRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Student not found"));
        return compassionateRepo.findByStudent_StudentId(student.getStudentId());
    }

    public List<GraduationApplication> getPendingApplications() {
        return graduationApplicationRepository.findByStatus(ApplicationStatus.PENDING);
    }

    public List<GraduationApplication> getApplicationsByProgramme(String programmeCode) {
        return graduationApplicationRepository.findByProgramme_ProgrammeCode(programmeCode);
    }

    public List<GraduationApplication> getApplicationsByStatus(ApplicationStatus status) {
        return graduationApplicationRepository.findByStatus(status);
    }
    public List<GraduationApplication> getAllApplications() {
        return graduationApplicationRepository.findAll();
    }

    public void updateApplicationStatus(Long applicationId, ApplicationStatus status) {
        GraduationApplication app = graduationApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found"));
        app.setStatus(status);
        graduationApplicationRepository.save(app);
    }

    public void submitApplication(String email, CompassionateFormDTO form) {
        System.out.println("[DEBUG] Starting compassionate form submission for: " + email);

        Student student = studentRepo.findByEmail(email)
                .orElseThrow(() -> {
                    System.out.println("[ERROR] Student not found with email: " + email);
                    return new RuntimeException("Student not found");
                });

        System.out.println("[DEBUG] Student found: " + student.getFirstName() + " " + student.getLastName());

        if (!compassionateRepo.findByStudent_StudentId(student.getStudentId()).isEmpty()) {
            System.out.println("[ERROR] Compassionate application already exists for studentId: " + student.getStudentId());
            throw new RuntimeException("You have already submitted a compassionate application.");
        }

        CompassionateApplication app = new CompassionateApplication();
        app.setStudent(student);

        String joinedType = String.join(", ", form.getApplicationType());
        app.setApplicationType(joinedType);
        app.setReason(form.getReason());
        app.setStudentSignature(form.getStudentSignature());
        app.setSubmissionDate(form.getSubmissionDate());

        System.out.println("[DEBUG] Form fields set. Now converting exam entries...");

        List<CompassionateApplication.MissedExamEntry> examEntries = new ArrayList<>();
        for (int i = 0; i < form.getCourseCode().size(); i++) {
            CompassionateApplication.MissedExamEntry entry = new CompassionateApplication.MissedExamEntry();
            entry.setCourseCode(form.getCourseCode().get(i));
            entry.setExamDate(form.getExamDate().get(i));
            entry.setExamTime(form.getExamTime().get(i));
            examEntries.add(entry);
        }
        app.setExamEntries(examEntries);
        app.setStatus(ApplicationStatus.PENDING);

        System.out.println("[DEBUG] Saving compassionate application to database...");
        compassionateRepo.save(app);
        System.out.println("[DEBUG] Application saved successfully!");

        // Dynamic subject line
        String subject = "New " + joinedType + " Application Submitted";
        String studentSubject = "Your " + joinedType + " Application Has Been Received";

        try {
            System.out.println("[DEBUG] Sending email to Admin...");
            emailService.sendHtmlMail(
                    "adriandougjonajitino@gmail.com",
                    subject,
                    "email_admin",
                    Map.of(
                            "fullName", student.getFirstName() + " " + student.getLastName(),
                            "studentId", student.getStudentId(),
                            "applicationType", joinedType,
                            "submittedAt", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm"))
                    )
            );
            System.out.println("[DEBUG] Admin notified.");

            System.out.println("[DEBUG] Sending email to Student...");
            emailService.sendHtmlMail(
                    student.getEmail(), // or use a test email here
                    studentSubject,
                    "email",
                    Map.of(
                            "fullName", student.getFirstName() + " " + student.getLastName(),
                            "studentId", student.getStudentId()
                    )
            );
            System.out.println("[DEBUG] Student notified.");
        } catch (Exception e) {
            System.out.println("[ERROR] Email sending failed: " + e.getMessage());
        }

        System.out.println("[DEBUG] Submission process complete.");
    }


    public List<CompassionateApplication> getAllCompassionateApplications() {
        return compassionateRepo.findAll();
    }

    public void updateOtherApplicationStatus(Long applicationId, ApplicationStatus status) {
        CompassionateApplication app = compassionateRepo.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Compassionate application not found"));
        app.setStatus(status);
        compassionateRepo.save(app);
    }

    public void exportGraduationCsv(PrintWriter writer) throws IOException {
        List<GraduationApplication> apps = graduationApplicationRepository.findAll();
        writer.println("Student ID,Name,Programme,Submitted At,Status\n");
        for (GraduationApplication app : apps) {
            writer.printf(String.format("%s,%s %s,%s,%s,%s\n",
                    app.getStudent().getStudentId(),
                    app.getStudent().getFirstName(),
                    app.getStudent().getLastName(),
                    app.getProgramme().getName(),
                    app.getSubmittedAt(),
                    app.getStatus()
            ));
        }
    }

    public void exportCompassionateCsv(PrintWriter writer) throws IOException {
        List<CompassionateApplication> apps = compassionateRepo.findAll();
        writer.println("Student ID,Name,Application Type,Reason,Submitted At,Status\n");
        for (CompassionateApplication app : apps) {
            writer.printf(String.format("%s,%s %s,%s,%s,%s,%s\n",
                    app.getStudent().getStudentId(),
                    app.getStudent().getFirstName(),
                    app.getStudent().getLastName(),
                    app.getApplicationType(),
                    app.getReason(),
                    app.getSubmissionDate(),
                    app.getStatus()
            ));
        }
    }

}
