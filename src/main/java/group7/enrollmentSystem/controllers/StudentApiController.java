package group7.enrollmentSystem.controllers;

import group7.enrollmentSystem.dtos.appDtos.EnrollCourseRequest;
import group7.enrollmentSystem.dtos.classDtos.CourseEnrollDto;
import group7.enrollmentSystem.models.Course;
import group7.enrollmentSystem.models.CourseEnrollment;
import group7.enrollmentSystem.models.EnrollmentState;
import group7.enrollmentSystem.models.Student;
import group7.enrollmentSystem.repos.CourseEnrollmentRepo;
import group7.enrollmentSystem.repos.CourseRepo;
import group7.enrollmentSystem.repos.EnrollmentStatusRepo;
import group7.enrollmentSystem.repos.StudentRepo;
import group7.enrollmentSystem.services.CourseEnrollmentService;
import group7.enrollmentSystem.services.CourseService;
import group7.enrollmentSystem.services.StudentProgrammeAuditService;
import group7.enrollmentSystem.services.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

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
    private final CourseEnrollmentService courseEnrollmentService;
    private final StudentRepo studentRepo;
    private final CourseRepo courseRepo;
    private final CourseEnrollmentRepo courseEnrollmentRepo;
    private final EnrollmentStatusRepo enrollmentStatusRepo;
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

    @GetMapping("/audit/{studentId}")
    public ResponseEntity<?> getStudentAudit(@PathVariable String studentId) {
        try {
            return ResponseEntity.ok(studentProgrammeAuditService.getFullAudit(studentId));
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "Failed to retrieve programme audit.");
            error.put("error", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    /*
    *________________________________________________________________________________________________*
    * STUDENT API CONTROLLER FOR APP (NOT SURE WHERE TO PUT THIS YET)
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


    @GetMapping("/eligibleCourses")
    public ResponseEntity<List<CourseEnrollDto>> getEligibleCourses(Authentication auth) {
        String email = auth.getName();
        Student student = studentRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        EnrollmentState state = enrollmentStatusRepo.findById(1L)
                .orElseThrow(() -> new RuntimeException("Enrollment state not found"));
        int semester = state.isSemesterOne() ? 1 : 2;

        List<CourseEnrollDto> eligibleCourses =
                courseEnrollmentService.getEligibleCoursesForEnrollment(student, semester);

        return ResponseEntity.ok(eligibleCourses);
    }


}
