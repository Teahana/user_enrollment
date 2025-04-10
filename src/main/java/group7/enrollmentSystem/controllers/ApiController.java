package group7.enrollmentSystem.controllers;

import group7.enrollmentSystem.config.CustomExceptions;
import group7.enrollmentSystem.dtos.appDtos.CourseIdsResponse;
import group7.enrollmentSystem.dtos.appDtos.LoginResponse;
import group7.enrollmentSystem.dtos.classDtos.LoginRequest;
import group7.enrollmentSystem.helpers.JwtService;
import group7.enrollmentSystem.models.User;
import group7.enrollmentSystem.services.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpHeaders;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiController {

    private final CourseService courseService;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
            User user = (User) auth.getPrincipal();
            String token = jwtService.generateToken(user, 3600); // 1 hour

            String userType = user.getRoles().contains("ROLE_ADMIN") ? "admin" : "student";

            LoginResponse response = new LoginResponse(
                    user.getId(),
                    userType,
                    token
            );
            return ResponseEntity.ok(response);
        } catch (AuthenticationException e) {
            throw new CustomExceptions.UserNotFoundException(request.getEmail());
        }
    }

    @PostMapping("/generateSvgBatch")
    public ResponseEntity<?> generateBatchSvg(@RequestBody Map<String, List<Long>> request) {
        List<Long> courseIds = request.get("courseIds");
        List<CourseIdsResponse> response = new ArrayList<>();
        for(Long id : courseIds) {
            String code = courseService.getMermaidDiagramForCourse(id);
            if (code == null || code.trim().isEmpty()) {
                code = "graph TD; A[Code missing] --> B[Course ID: " + id + "]";
            }
            code = code.replace("\n", "; ").replace("\r", "");
            response.add(new CourseIdsResponse(id, code));
        }
        return ResponseEntity.ok(response);
    }
    @PostMapping("/generateSvg")
    public ResponseEntity<?> generateSvg(@RequestBody Map<String, String> request) {
        String code = courseService.getMermaidDiagramForCourse(Long.parseLong(request.get("courseId")));
        if (code == null || code.trim().isEmpty()) {
            code = "graph TD; A[Code missing] --> B[Course ID: " + request.get("courseId") + "]";
        }
        code = code.replace("\n", "; ").replace("\r", "");
        return ResponseEntity.ok(code);
    }

}
