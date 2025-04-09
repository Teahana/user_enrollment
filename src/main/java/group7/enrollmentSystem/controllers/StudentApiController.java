package group7.enrollmentSystem.controllers;

import com.itextpdf.text.DocumentException;
import group7.enrollmentSystem.config.CustomExceptions;
import group7.enrollmentSystem.dtos.appDtos.*;
import group7.enrollmentSystem.dtos.classDtos.StudentFullAuditDto;
import group7.enrollmentSystem.helpers.JwtService;
import group7.enrollmentSystem.helpers.ProgrammeAuditPdfGeneratorService;
import group7.enrollmentSystem.models.*;
import group7.enrollmentSystem.repos.UserRepo;
import group7.enrollmentSystem.repos.StudentRepo;
import group7.enrollmentSystem.services.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/student")
@RequiredArgsConstructor
public class StudentApiController {
    private final CourseService courseService;
    private final StudentProgrammeAuditService studentProgrammeAuditService;
    private final StudentService studentService;
    private final StudentProgrammeService studentProgrammeService;
    private final CourseEnrollmentService courseEnrollmentService;
    private final JwtService jwtService;
    private final UserRepo userRepo;
    @PostMapping("/testing")
    public ResponseEntity<?> testToken() {
        System.out.println("test received");
        return ResponseEntity.ok(Map.of("message","Token is valid and not expired."));
    }
    @PostMapping("/tokenLogin")
    public ResponseEntity<?> tokenLogin(Authentication authentication) {
        System.out.println("Token login request received.");
        User user = userRepo.findByEmail(authentication.getName()).orElseThrow();
        String token = jwtService.generateToken(user, 3600);
        String userType = user.getRoles().contains("ROLE_ADMIN") ? "admin" : "student";
        LoginResponse response = new LoginResponse(
                user.getId(),
                userType,
                token
        );
        return ResponseEntity.ok(response);
    }

    private final StudentRepo studentRepo;

    /**
     * Retrieves the details of the currently logged-in student.
     * <p>
     * This API fetches the student information from the database using
     * their email (from the authentication object), and returns it
     * as a {@link StudentDto}.
     *
     * @param auth the authentication object containing the student's email.
     * @return a {@link ResponseEntity} containing the student's details as a {@link StudentDto}.
     * @throws RuntimeException if the student is not found.
     */
    @PostMapping("/getStudentDetails")
    public ResponseEntity<StudentDto> getStudentDetails(Authentication auth) {
        String email = auth.getName();
        Student student = studentRepo.findByEmail(email).orElseThrow(()
                -> new CustomExceptions.StudentNotFoundException(email));
        Programme programme = studentProgrammeService.getStudentProgramme(student);
        if (student == null) {
            return ResponseEntity.notFound().build();
        }
        String fullName = student.getFirstName() + " " + student.getLastName();
        StudentDto dto = new StudentDto(
                student.getStudentId(),
                fullName,
                student.getEmail(),
                programme.getName(),
                student.getAddress(),
                student.getPhoneNumber()
        );

        return ResponseEntity.ok(dto);
    }

    @PostMapping("/getMermaidCode")
    public ResponseEntity<String> getMermaidCode(@RequestBody Map<String, Long> request) {
        return ResponseEntity.ok(courseService.getMermaidDiagramForCourse(request.get("courseId")));
    }
    @PostMapping("/givePass")
    public ResponseEntity<?> givePass(@RequestBody Map<String, Object> request) {
        try {
            String email = (String) request.get("email");
            Integer levelInt = (Integer) request.get("level");
            short level = levelInt.shortValue();
            courseEnrollmentService.passStudentByEmailAndYear(email, level);
            return ResponseEntity.ok("Pass given successfully.");
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "Failed to give pass.");
            error.put("error", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Retrieves the full audit of the currently logged-in student's programme.
     * <p>
     * This API fetches the audit information from the database using
     * the student's ID (from the authentication object), and returns it
     * as a JSON response.
     *
     * @param auth the authentication object containing the student's email.
     * @return a {@link ResponseEntity} containing the student's programme audit.
     */
    @PostMapping("/audit")
    public ResponseEntity<?> getStudentAudit(Authentication auth) {
        String email = auth.getName();
        Student student = studentService.getStudentByEmail(email);
        studentProgrammeAuditService.getFullAudit(student.getStudentId());
        if (student == null) {
            return ResponseEntity.notFound().build();
        }
        StudentFullAuditDto auditDto = studentProgrammeAuditService.getFullAudit(student.getStudentId());
        return ResponseEntity.ok(auditDto);

    }

    @PostMapping("/audit/download")
    public ResponseEntity<byte[]> downloadStudentAudit(Authentication authentication) throws Exception {
        String email = authentication.getName();
        Student student = studentRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        // Build the audit DTO from service
        StudentFullAuditDto auditDto = studentProgrammeAuditService.getFullAudit(student.getStudentId());

        // Generate PDF
        byte[] pdfBytes = programmeAuditPdfGeneratorService.generateAuditPdf(auditDto);

        // Return PDF as a downloadable response
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "student_audit.pdf");

        return ResponseEntity.ok().headers(headers).body(pdfBytes);
    }

    /*
    *________________________________________________________________________________________________*
    * STUDENT ENROLLMENT COURSES API's
    ________________________________________________________________________________________________*/


    @PostMapping("/enrollCourses")
    public ResponseEntity<?> enrollCourses(@RequestBody EnrollCourseRequest request) {
        studentService.enrollStudent(request);
        return ResponseEntity.ok(Map.of("message", "Enrolled successfully"));
    }
    @PostMapping("/getEligibleCourses")
    public ResponseEntity<?> getEligibleCourses(@RequestBody Map<String, String> request) {
        return ResponseEntity.ok(studentService.getEligibleCourses(request.get("email")));
    }
    @PostMapping("/invoice/download")
    public ResponseEntity<byte[]> downloadInvoice(@RequestBody Map<String,Long> request) throws DocumentException, IOException {
        Student student = studentRepo.findById(request.get("userId")).orElseThrow();
        byte[] pdfBytes = studentService.generateInvoicePdfForStudent(student.getEmail());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "invoice.pdf");
        return ResponseEntity.ok().headers(headers).body(pdfBytes);
    }

}
