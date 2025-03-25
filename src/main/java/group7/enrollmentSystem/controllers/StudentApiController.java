package group7.enrollmentSystem.controllers;

import group7.enrollmentSystem.services.CourseService;
import group7.enrollmentSystem.services.StudentProgrammeAuditService;
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
    @PostMapping("/getMermaidCode")
    public ResponseEntity<String> getMermaidCode(@RequestBody Map<String, Long> request) {
        return ResponseEntity.ok(courseService.getMermaidDiagramForCourse(request.get("courseId")));
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
