package group7.enrollmentSystem.services;


import group7.enrollmentSystem.dtos.formDtos.CompassionateFormDTO;
import group7.enrollmentSystem.dtos.formDtos.GraduationFormDTO;
import group7.enrollmentSystem.enums.ApplicationStatus;
import group7.enrollmentSystem.helpers.EmailService;
import group7.enrollmentSystem.helpers.FileUploads;
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
import java.util.HashMap;
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
    private final FileUploads fileUploads;

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
        emailService.sendHtmlMailAsync(
                "adriandougjonajitino@gmail.com",
                "New Graduation Application Submitted",
                "graduation-email",
                Map.of(
                        "fullName", student.getFirstName() + " " + student.getLastName(),
                        "programme", programme.getName(),
                        "studentId", student.getStudentId()
                )
        );

        // Send confirmation to student
        emailService.sendHtmlMailAsync(
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
        String signaturePath = fileUploads.saveSignature(form.getStudentSignature());
        app.setStudentSignatureFilePath(signaturePath);
        app.setSignatureDate(form.getSignatureDate());
        app.setSubmittedAt(LocalDateTime.now());
        app.setStatus(ApplicationStatus.PENDING);

        graduationApplicationRepository.save(app);

        // === Notify Admin ===
        Map<String, Object> adminModel = new HashMap<>();
        adminModel.put("fullName", student.getFirstName() + " " + student.getLastName());
        adminModel.put("programme", programme.getName());
        adminModel.put("studentId", student.getStudentId());
        adminModel.put(("dateSubmitted"), LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")));

        emailService.notifyAdminNewApplication("adriandougjonajitino@gmail.com", adminModel);

        // === Notify Student ===
        Map<String, Object> studentModel = new HashMap<>();
        studentModel.put("fullName", student.getFirstName() + " " + student.getLastName());
        studentModel.put("programme", programme.getName());
        studentModel.put("studentId", student.getStudentId());

        emailService.notifyStudentApplicationSubmission(email, studentModel);
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

        // Notify student
        Student student = app.getStudent();
        emailService.notifyStudentApplicationStatusUpdate(
                student.getEmail(),
                student.getFirstName() + " " + student.getLastName(),
                student.getStudentId(),
                "Graduation",
                status.name()
        );
    }


    public void submitApplication(String email, CompassionateFormDTO form) {
        Student student = studentRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        if (!compassionateRepo.findByStudent_StudentId(student.getStudentId()).isEmpty()) {
            throw new RuntimeException("You have already submitted a compassionate application.");
        }

        CompassionateApplication app = new CompassionateApplication();
        app.setStudent(student);

        String joinedType = String.join(", ", form.getApplicationType());
        app.setApplicationType(joinedType);
        app.setReason(form.getReason());
        String signaturePath = fileUploads.saveSignature(form.getStudentSignature());
        app.setStudentSignatureFilePath(signaturePath);
        app.setSubmissionDate(form.getSubmissionDate());
        app.setCampus(form.getCampus());
        app.setSemesterYear(form.getSemesterYear());

        // 🔽 Save files
        List<String> savedPaths = fileUploads.saveFiles(form.getDocuments());
        app.setDocumentPaths(savedPaths);

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

        compassionateRepo.save(app);

        Map<String, Object> adminModel = Map.of(
                "fullName", student.getFirstName() + " " + student.getLastName(),
                "studentId", student.getStudentId(),
                "applicationType", joinedType,
                "submittedAt", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm"))
        );

        Map<String, Object> studentModel = Map.of(
                "fullName", student.getFirstName() + " " + student.getLastName(),
                "studentId", student.getStudentId()
        );

        try {
            emailService.notifyAdminNewApplication("adriandougjonajitino@gmail.com", adminModel);
            emailService.notifyStudentApplicationSubmission(email, studentModel);
        } catch (Exception e) {
            System.out.println("[ERROR] Email sending failed: " + e.getMessage());
        }
    }


    public List<CompassionateApplication> getAllCompassionateApplications() {
        return compassionateRepo.findAll();
    }

    public void updateOtherApplicationStatus(Long applicationId, ApplicationStatus status) {
        CompassionateApplication app = compassionateRepo.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Compassionate application not found"));
        app.setStatus(status);
        compassionateRepo.save(app);

        // Notify student
        Student student = app.getStudent();
        emailService.notifyStudentApplicationStatusUpdate(
                student.getEmail(),
                student.getFirstName() + " " + student.getLastName(),
                student.getStudentId(),
                "Compassionate",
                status.name()
        );
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

    public void deleteGraduationApplication(Long applicationId) {
        GraduationApplication app = graduationApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Graduation application not found"));
        graduationApplicationRepository.delete(app);
    }
    public void deleteCompassionateApplication(Long applicationId) {
        CompassionateApplication app = compassionateRepo.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Compassionate application not found"));
        compassionateRepo.delete(app);
    }

    public boolean hasGraduationApp(String studentId) {
        return graduationApplicationRepository.findByStudent_StudentId(studentId).isPresent();
    }

    public boolean hasCompassionateApp(String studentId) {
        return !compassionateRepo.findByStudent_StudentId(studentId).isEmpty();
    }

}
