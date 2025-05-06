package group7.enrollmentSystem.controllers;

import group7.enrollmentSystem.dtos.appDtos.CourseIdsResponse;
import group7.enrollmentSystem.dtos.appDtos.LoginResponse;
import group7.enrollmentSystem.dtos.classDtos.EmailRequest;
import group7.enrollmentSystem.dtos.classDtos.LoginRequest;
import group7.enrollmentSystem.dtos.serverKtDtos.CourseIdDto;
import group7.enrollmentSystem.dtos.serverKtDtos.CourseIdsDto;
import group7.enrollmentSystem.helpers.EmailService;
import group7.enrollmentSystem.helpers.JwtService;
import group7.enrollmentSystem.models.User;
import group7.enrollmentSystem.services.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiController {

    private final CourseService courseService;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;

    @PostMapping("/send")
    public ResponseEntity<String> sendTestMail(@RequestBody EmailRequest request) {
        Map<String, Object> model = new HashMap<>();
        model.put("subject", request.getSubject());
        model.put("header", request.getHeader());
        model.put("body", request.getBody());

        emailService.sendHtmlMail(request.getTo(), request.getSubject(), "notification", model);

        return ResponseEntity.ok("Email sent to " + request.getTo());
    }

    @Operation(
            summary = "User login",
            description = "Authenticates user credentials and returns a JWT token for session management."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully authenticated",
                    content =@Content(schema =@Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request format"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
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
    }

    @Operation(
            summary = "Generate SVG diagrams in batch",
            description = "Generates Mermaid diagram codes for multiple courses at once."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully generated SVG codes"),
            @ApiResponse(responseCode = "400", description = "Invalid course IDs"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or expired token"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/generateSvgBatch")
    public ResponseEntity<List<CourseIdsResponse>> generateBatchSvg(@RequestBody CourseIdsDto request) {
        List<Long> courseIds = request.getCourseIds();
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

    @Operation(
            summary = "Generate SVG diagram for a course",
            description = "Generates Mermaid diagram code for a single course's prerequisite structure."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully generated SVG code"),
            @ApiResponse(responseCode = "400", description = "Invalid course ID"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or expired token"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/generateSvg")
    public ResponseEntity<String> generateSvg(@RequestBody CourseIdDto request) {
        String code = courseService.getMermaidDiagramForCourse(request.getCourseId());
        if (code == null || code.trim().isEmpty()) {
            code = "graph TD; A[Code missing] --> B[Course ID: " + request.getCourseId() + "]";
        }
        code = code.replace("\n", "; ").replace("\r", "");
        return ResponseEntity.ok(code);
    }
}
