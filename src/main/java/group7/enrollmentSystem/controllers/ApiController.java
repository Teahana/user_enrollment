package group7.enrollmentSystem.controllers;

import group7.enrollmentSystem.config.CustomErrorHandler;
import group7.enrollmentSystem.dtos.appDtos.LoginResponse;
import group7.enrollmentSystem.dtos.classDtos.LoginRequest;
import group7.enrollmentSystem.helpers.JwtService;
import group7.enrollmentSystem.models.User;
import group7.enrollmentSystem.repos.StudentRepo;
import group7.enrollmentSystem.repos.UserRepo;
import group7.enrollmentSystem.services.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;



@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiController {

    private final UserService userService;
    private final UserRepo userRepo;
    private final StudentService studentService;
    private final StudentRepo studentRepo;
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
            throw new CustomErrorHandler.UserNotFoundException(request.getEmail());
        }
    }


}
