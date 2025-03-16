package group7.enrollmentSystem.controllers;

import group7.enrollmentSystem.dtos.CourseDto;
import group7.enrollmentSystem.models.Student;
import group7.enrollmentSystem.models.User;
import group7.enrollmentSystem.repos.CourseRepo;
import group7.enrollmentSystem.repos.StudentRepo;
import group7.enrollmentSystem.repos.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class MainController {
    private final UserRepo userRepo;
    private final StudentRepo studentRepo;
    private final CourseRepo courseRepo;
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
    public String showStudentForm() {
        return "student-form";
    }
    @PostMapping("/register")
    public String registerStudent(@RequestParam("studentId") String studentId,
            @RequestParam("firstName") String firstName,
             @RequestParam("lastName") String lastName, RedirectAttributes redirectAttributes) {
        try {
            System.out.println("Student ID: " + studentId);
            System.out.println("First Name: " + firstName);
            System.out.println("Last Name: " + lastName);
            redirectAttributes.addFlashAttribute("successMessage","Saved student " + firstName + " " + lastName);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/register";
    }
}
