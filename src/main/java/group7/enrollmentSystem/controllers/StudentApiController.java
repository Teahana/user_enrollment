package group7.enrollmentSystem.controllers;

import group7.enrollmentSystem.config.CustomErrorHandler;
import group7.enrollmentSystem.config.GlobalApiExceptionHandler;
import group7.enrollmentSystem.dtos.appDtos.LoginResponse;
import group7.enrollmentSystem.dtos.appDtos.StudentDto;
import group7.enrollmentSystem.helpers.JwtService;
import group7.enrollmentSystem.models.*;
import group7.enrollmentSystem.repos.UserRepo;
import group7.enrollmentSystem.dtos.appDtos.EnrollCourseRequest;
import group7.enrollmentSystem.repos.CourseEnrollmentRepo;
import group7.enrollmentSystem.repos.CourseRepo;
import group7.enrollmentSystem.repos.EnrollmentStateRepo;
import group7.enrollmentSystem.repos.StudentRepo;
import group7.enrollmentSystem.services.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.ArrayList;
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
    private final CourseRepo courseRepo;
    private final CourseEnrollmentRepo courseEnrollmentRepo;
    private final EnrollmentStateRepo enrollmentStatusRepo;


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
                -> new CustomErrorHandler.StudentNotFoundException(email));
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
    @PostMapping("/getEligibleCourses")
    public ResponseEntity<?> getEligibleCourses(@RequestBody Map<String, String> request) {
        return ResponseEntity.ok(studentService.getEligibleCourses(request.get("email")));
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
        Student student = studentRepo.findByEmail(email)
                .orElseThrow(() -> new CustomErrorHandler.StudentNotFoundException(email));

        try {
            return ResponseEntity.ok(studentProgrammeAuditService.getFullAudit(student.getStudentId()));
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "Failed to retrieve programme audit.");
            error.put("error", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }



    /*
    *________________________________________________________________________________________________*
    * STUDENT ENROLLMENT COURSES API's
    ________________________________________________________________________________________________*/


    @PostMapping("/enrollCourses")
    public ResponseEntity<?> enrollCourses(@RequestBody EnrollCourseRequest request, Authentication auth) {
        if (request.getSelectedCourses() == null || request.getSelectedCourses().isEmpty()) {
            return ResponseEntity.badRequest().body("No courses selected.");
        }

        String email = auth.getName();
        Student student = studentRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        EnrollmentState enrollmentState = enrollmentStatusRepo.findById(1L)
                .orElseThrow(() -> new RuntimeException("Enrollment state not found"));
        int semester = enrollmentState.isSemesterOne() ? 1 : 2;

        int currentCount = courseEnrollmentRepo
                .countByStudentAndSemesterEnrolledAndCurrentlyTakingIsTrue(student, semester);

        int totalIfAdded = currentCount + request.getSelectedCourses().size();
        if (totalIfAdded > 4) {
            return ResponseEntity.badRequest().body(
                    "You cannot enroll in more than 4 courses this semester. " +
                            "Already taking: " + currentCount + ", tried to add: " + request.getSelectedCourses().size()
            );
        }

        List<CourseEnrollment> enrollments = new ArrayList<>();
        for (String courseCode : request.getSelectedCourses()) {
            Course course = courseRepo.findByCourseCode(courseCode)
                    .orElseThrow(() -> new RuntimeException("Course not found: " + courseCode));

            CourseEnrollment e = new CourseEnrollment();
            e.setStudent(student);
            e.setCourse(course);
            e.setSemesterEnrolled(semester);
            e.setCurrentlyTaking(true);
            e.setCompleted(false);

            enrollments.add(e);
        }

        courseEnrollmentRepo.saveAll(enrollments);
        return ResponseEntity.ok("Enrolled in " + enrollments.size() + " course(s).");
    }



}
