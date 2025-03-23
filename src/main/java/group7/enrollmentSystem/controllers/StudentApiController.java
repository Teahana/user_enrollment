package group7.enrollmentSystem.controllers;

import group7.enrollmentSystem.services.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/student")
@RequiredArgsConstructor
public class StudentApiController {
    private final CourseService courseService;
    @PostMapping("/getMermaidCode")
    public ResponseEntity<?> getMermaidCode(@RequestBody Map<String, Long> request) {
        return ResponseEntity.ok(Map.of("mermaidCode",courseService.getMermaidDiagramForCourse(request.get("courseId"))));
    }
}
