package group7.enrollmentSystem.controllers;

import group7.enrollmentSystem.services.CourseEnrollmentService;
import group7.enrollmentSystem.services.CourseService;
import group7.enrollmentSystem.services.StudentProgrammeAuditService;
import group7.enrollmentSystem.services.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/student")
@RequiredArgsConstructor
public class StudentApiController {
    private final CourseService courseService;
    private final StudentProgrammeAuditService studentProgrammeAuditService;
    private final StudentService studentService;
    private final CourseEnrollmentService courseEnrollmentService;
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
}
