package group7.enrollmentSystem.controllers;

import group7.enrollmentSystem.config.CustomExceptions;
import group7.enrollmentSystem.models.Student;
import group7.enrollmentSystem.models.User;
import group7.enrollmentSystem.repos.StudentRepo;
import group7.enrollmentSystem.repos.UserRepo;
import group7.enrollmentSystem.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.Optional;


@Controller
@RequiredArgsConstructor
public class MainController {
    private final UserRepo userRepo;
    private final StudentRepo studentRepo;
    private final UserService userService;

    @GetMapping("/home")
    public String home(Authentication authentication, Model model) {
        Student student = studentRepo.findByEmail(authentication.getName())
                .orElseThrow(() -> new CustomExceptions.StudentNotFoundException(authentication.getName()));

        // Add hold message if exists
        student.getActiveHold().ifPresent(hold -> {
            model.addAttribute("holdMessage", "Your Account is on Hold.");
        });

        model.addAttribute("user", student);
        return "home";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }
    @GetMapping("/test")
    public String test() {
        throw new RuntimeException("Test exception");
    }



}
