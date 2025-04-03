package group7.enrollmentSystem.controllers;

import group7.enrollmentSystem.config.CustomExceptions;
import group7.enrollmentSystem.dtos.appDtos.LoginResponse;
import group7.enrollmentSystem.dtos.classDtos.LoginRequest;
import group7.enrollmentSystem.helpers.JwtService;
import group7.enrollmentSystem.helpers.NodeMicroserviceClient;
import group7.enrollmentSystem.models.User;
import group7.enrollmentSystem.repos.StudentRepo;
import group7.enrollmentSystem.repos.UserRepo;
import group7.enrollmentSystem.services.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;

import java.nio.charset.StandardCharsets;
import java.util.Map;


@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiController {

    private final CourseService courseService;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final NodeMicroserviceClient nodeClient;

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
    @PostMapping(value = "/generate", produces = "image/svg+xml")
    public ResponseEntity<byte[]> generateSvg(@RequestBody Map<String, Long> payload) {
        byte[] svgBytes = nodeClient.generateSvg(payload.get("courseId"));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf("image/svg+xml"));
        return new ResponseEntity<>(svgBytes, headers, HttpStatus.OK);
    }

    @GetMapping("/ping")
    public ResponseEntity<String> pingNodeService() {
        String pingResult = nodeClient.pingNodeService();
        return ResponseEntity.ok(pingResult);
    }

}
