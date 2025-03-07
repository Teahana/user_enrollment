package group7.enrollmentSystem.controllers;

import group7.enrollmentSystem.dtos.StudentDto;
import group7.enrollmentSystem.models.User;
import group7.enrollmentSystem.repos.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class MainController {
    private final UserRepo userRepo;
    @GetMapping("/home")
    public String home() {
        return "home";
    }
    @GetMapping("/login")
    public String login() {
        return "login";
    }
    @GetMapping("/admin")
    public String admin(Model model) {
        User user = userRepo.findByEmail("adrian@gmail.com").get();
        model.addAttribute("message","Hello world");
        model.addAttribute("message1","Bye world");
        model.addAttribute("user",user);
        return "admin";
    }
    @GetMapping("/register")
    public String showStudentForm(Model model) {
        model.addAttribute("studentDto", new StudentDto());
        return "student-form";
    }
    @PostMapping("/register")
    public String registerStudent(@ModelAttribute StudentDto studentDto, RedirectAttributes redirectAttributes) {
        try {
            System.out.println("Saving student: " + studentDto);

            redirectAttributes.addFlashAttribute("successMessage","Saved student: " + studentDto);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/register";
    }
}
