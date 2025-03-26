package group7.enrollmentSystem.controllers;

import group7.enrollmentSystem.dtos.classDtos.LoginRequest;
import group7.enrollmentSystem.helpers.JwtService;
import group7.enrollmentSystem.services.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;


@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiController {

    private final UserService userService;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

//    @PostMapping("/login")
//    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
//        Authentication auth = authenticationManager.authenticate(
//                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
//        );
//        UserDetails user = userService.loadUserByUsername(request.getEmail());
//        String token = jwtService.generateToken(user.getUsername(), 3600,null); // 1 hour
//        return ResponseEntity.ok(Map.of("token", token));
//    }




}
