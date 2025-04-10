package group7.enrollmentSystem.controllers;

import com.itextpdf.text.DocumentException;
import group7.enrollmentSystem.config.CustomExceptions;
import group7.enrollmentSystem.dtos.appDtos.*;
import group7.enrollmentSystem.dtos.classDtos.CourseEnrollmentDto;
import group7.enrollmentSystem.dtos.classDtos.StudentFullAuditDto;
import group7.enrollmentSystem.dtos.serverKtDtos.CancelCourseRequest;
import group7.enrollmentSystem.dtos.serverKtDtos.EmailDto;
import group7.enrollmentSystem.dtos.serverKtDtos.MessageDto;
import group7.enrollmentSystem.dtos.serverKtDtos.UserIdDto;
import group7.enrollmentSystem.helpers.JwtService;
import group7.enrollmentSystem.helpers.ProgrammeAuditPdfGeneratorService;
import group7.enrollmentSystem.models.*;
import group7.enrollmentSystem.repos.UserRepo;
import group7.enrollmentSystem.repos.StudentRepo;
import group7.enrollmentSystem.services.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
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
    private final ProgrammeAuditPdfGeneratorService programmeAuditPdfGeneratorService;
    private final StudentRepo studentRepo;

    /**
     * Authenticates the user using a token and returns a new session token if valid.
     *
     * @param authentication the authentication object with user credentials.
     * @return a {@link ResponseEntity} containing {@link LoginResponse} with user details and token.
     */
    @Operation(
            summary = "Token-based login",
            description = "Authenticates the user using an existing valid JWT and returns a new session token."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully authenticated and returned new token",
                    content =@Content(schema =@Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or expired token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User doesn't have required permissions"),
            @ApiResponse(responseCode = "423", description = "Student account is on hold"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/tokenLogin")
    public ResponseEntity<LoginResponse> tokenLogin(Authentication authentication) {
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
    @Operation(
            summary = "Get student details",
            description = "Fetches details of the currently authenticated student including name, email, and programme info."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully retrieved student details"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or expired token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User doesn't have required permissions"),
            @ApiResponse(responseCode = "404", description = "Student not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
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
    /**
     * Generates and returns Mermaid diagram code for a given course.
     *
     * @param request a map containing the "courseId".
     * @return a {@link ResponseEntity} with Mermaid diagram code as a String.
     */
    @Operation(
            summary = "Generate Mermaid diagram for a course",
            description = "Returns a Mermaid diagram string representing the prerequisite structure for a given course ID."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully generated Mermaid diagram code"),
            @ApiResponse(responseCode = "400", description = "Invalid course ID format"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or expired token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User doesn't have required permissions"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/getMermaidCode")
    public ResponseEntity<String> getMermaidCode(@RequestBody Map<String, Long> request) {
        return ResponseEntity.ok(courseService.getMermaidDiagramForCourse(request.get("courseId")));
    }
    /**
     * Gives a pass to a student for a specific academic level.
     *
     * @param request a map containing "email" and "level".
     * @return a success message or an error response.
     */
    @Operation(
            summary = "Give pass for a level",
            description = "Assigns a pass to a student for a specific academic level, identified by email and level."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully gave pass to student"),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or expired token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User doesn't have required permissions"),
            @ApiResponse(responseCode = "404", description = "Student not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/givePass")
    public ResponseEntity<String> givePass(@RequestBody Map<String, Object> request) {
            String email = (String) request.get("email");
            Integer levelInt = (Integer) request.get("level");
            short level = levelInt.shortValue();
            courseEnrollmentService.passStudentByEmailAndYear(email, level);
            return ResponseEntity.ok("Pass given successfully.");
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
    @Operation(
            summary = "Get student programme audit",
            description = "Retrieves the full programme audit for the currently logged-in student, including completed and enrolled courses."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully retrieved programme audit"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or expired token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User doesn't have required permissions"),
            @ApiResponse(responseCode = "404", description = "Student not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/audit")
    public ResponseEntity<StudentFullAuditDto> getStudentAudit(Authentication auth) {
        String email = auth.getName();
        Student student = studentService.getStudentByEmail(email);
        studentProgrammeAuditService.getFullAudit(student.getStudentId());
        if (student == null) {
            return ResponseEntity.notFound().build();
        }
        StudentFullAuditDto auditDto = studentProgrammeAuditService.getFullAudit(student.getStudentId());
        return ResponseEntity.ok(auditDto);

    }

    /**
     * Downloads the student's programme audit as a PDF.
     *
     * @param authentication the authentication object containing the student's email.
     * @return a PDF file containing the audit report.
     * @throws Exception if PDF generation fails.
     */
    @Operation(
            summary = "Download audit report (PDF)",
            description = "Generates and downloads the full programme audit for the logged-in student as a PDF document."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully generated PDF audit"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or expired token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User doesn't have required permissions"),
            @ApiResponse(responseCode = "404", description = "Student not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error while generating PDF")
    })
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
    /**
     * Downloads the student's invoice as a PDF document.
     *
     * @param request a map containing "userId" of the student.
     * @return a PDF invoice for the student.
     * @throws DocumentException if there's an error generating the PDF.
     * @throws IOException if file IO fails.
     */
    @Operation(
            summary = "Download student invoice (PDF)",
            description = "Downloads the fee invoice for a given student (by userId) as a PDF document."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully generated PDF invoice"),
            @ApiResponse(responseCode = "400", description = "Invalid user ID format"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or expired token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User doesn't have required permissions"),
            @ApiResponse(responseCode = "404", description = "Student not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error while generating PDF")
    })
    @PostMapping("/invoice/download")
    public ResponseEntity<byte[]> downloadInvoice(@RequestBody UserIdDto request) throws DocumentException, IOException {
        Student student = studentRepo.findById(request.getUserId()).orElseThrow();
        byte[] pdfBytes = studentService.generateInvoicePdfForStudent(student.getEmail());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "invoice.pdf");
        return ResponseEntity.ok().headers(headers).body(pdfBytes);
    }

    /**
     * Enrolls the student in selected courses.
     *
     * @param request an {@link EnrollCourseRequest} with selected course IDs and user ID.
     * @return a confirmation message on success.
     */
    @Operation(
            summary = "Enroll in courses",
            description = "Enrolls the student in a list of selected courses using course codes and their user ID."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully enrolled in courses"),
            @ApiResponse(responseCode = "400", description = "Invalid course codes or user ID"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or expired token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User doesn't have required permissions"),
            @ApiResponse(responseCode = "404", description = "Student or courses not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/enrollCourses")
    public ResponseEntity<MessageDto> enrollCourses(@RequestBody EnrollCourseRequest request) {
        studentService.enrollStudent(request);
        return ResponseEntity.ok(new MessageDto("Courses enrolled successfully"));
    }

    @Operation(
            summary = "Cancel a course enrollment",
            description = "Cancels a student's enrollment in a specific course using course ID and user ID."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully cancelled course enrollment"),
            @ApiResponse(responseCode = "400", description = "Invalid course ID or user ID"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or expired token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User doesn't have required permissions"),
            @ApiResponse(responseCode = "404", description = "Student or course not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/cancelCourse")
    public ResponseEntity<MessageDto> cancelCourse(@RequestBody CancelCourseRequest request) {
        studentService.cancelCourse(request.getCourseId(), request.getUserId());
        return ResponseEntity.ok(new MessageDto("Course cancelled successfully"));
    }
    /**
     * Retrieves a list of eligible courses for a student based on their email.
     *
     * This endpoint checks which courses the student is eligible to enroll in
     * and returns a list of course details including course ID, code, title, and cost.
     *
     * @param request a map containing the student's "email".
     * @return a {@link ResponseEntity} containing a list of {@link CourseEnrollmentDto} objects.
     */
    @Operation(
            summary = "Get eligible courses",
            description = "Returns a list of courses a student is eligible to enroll in, based on completed and enrolled courses."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully retrieved eligible courses"),
            @ApiResponse(responseCode = "400", description = "Invalid email format"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or expired token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User doesn't have required permissions"),
            @ApiResponse(responseCode = "404", description = "Student not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/getEligibleCourses")
    public ResponseEntity<List<CourseEnrollmentDto>> getEligibleCourses(@RequestBody UserIdDto request) {
        Student student = studentRepo.findById(request.getUserId()).orElseThrow();
        return ResponseEntity.ok(studentService.getEligibleCourses(student.getEmail()));
    }

    @Operation(
            summary = "Get enrolled courses",
            description = "Returns a list of courses the student is currently enrolled in."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully retrieved enrolled courses"),
            @ApiResponse(responseCode = "400", description = "Invalid user ID"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or expired token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User doesn't have required permissions"),
            @ApiResponse(responseCode = "404", description = "Student not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/getEnrolledCourses")
    public ResponseEntity<List<CourseEnrollmentDto>> getEnrolledCourses(@RequestBody UserIdDto request) {
        Student student = studentRepo.findById(request.getUserId()).orElseThrow();
        return ResponseEntity.ok(studentService.getEnrolledCourses(student));
    }
    @PostMapping("/passEnrolledCourses")
    public ResponseEntity<MessageDto> passEnrolledCourses(@RequestBody EnrollCourseRequest request){
        studentService.passEnrolledCourses(request.getUserId(), request.getSelectedCourses());
        return ResponseEntity.ok(new MessageDto("Courses passed successfully"));
    }
    @PostMapping("/failEnrolledCourses")
    public ResponseEntity<MessageDto> failEnrolledCourses(@RequestBody EnrollCourseRequest request){
        studentService.failEnrolledCourses(request.getUserId(), request.getSelectedCourses());
        return ResponseEntity.ok(new MessageDto("Courses failed successfully"));
    }
}
