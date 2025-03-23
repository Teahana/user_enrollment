package group7.enrollmentSystem.controllers;

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
    public String home() {
        return "home";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    /*@GetMapping("/admin")
    public String admin(Model model) {
        User user = userRepo.findByEmail("adrian@gmail.com").get();
        model.addAttribute("message", "Hello world");
        model.addAttribute("message1", "Bye world");
        model.addAttribute("user", user);
        return "admin";
    }*/

    @GetMapping("/register")
    public String showStudentForm() {
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            @RequestParam("role") String role,
            @RequestParam(value = "studentId", required = false) String studentId,
            @RequestParam(value = "firstName", required = false) String firstName,
            @RequestParam(value = "lastName", required = false) String lastName,
            @RequestParam(value = "phoneNumber", required = false) String phoneNumber,
            @RequestParam(value = "address", required = false) String address,
            RedirectAttributes redirectAttributes) {

        try {
            if (role.equalsIgnoreCase("ROLE_STUDENT")) {

                Student student = new Student();
                student.setStudentId(studentId);
                student.setFirstName(firstName);
                student.setLastName(lastName);
                student.setEmail(email);
                student.setPhoneNumber(phoneNumber);
                student.setAddress(address);

                // Register the user as a student
                userService.registerUser(email, password, firstName, lastName, role, student);
            } else if (role.equalsIgnoreCase("ROLE_ADMIN")) {
                // Register the user as an admin
                userService.registerUser(email, password, firstName, lastName, role, null);
            } else {
                throw new IllegalArgumentException("Invalid role selected");
            }

            redirectAttributes.addFlashAttribute("successMessage", "User registered successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/register";
    }

    //student view progAud
    @GetMapping("/student/audit")
    public String loadStudentAuditPage(Model model, Authentication authentication) {
        String email = authentication.getName();
        User user = userRepo.findByEmail(email).orElse(null);

        if (user instanceof Student student) {
            model.addAttribute("studentId", student.getStudentId());
            model.addAttribute("studentName", student.getFirstName() + " " + student.getLastName());
        }

        return "studentAudit";
    }


    @GetMapping("student/dashboard")
    public String getAdminPage(Model model, Authentication authentication) {
        String email = authentication.getName();
        User user = userRepo.findByEmail(email).orElse(null);
        if (user != null) {
            String studentName = user.getFirstName() + " " + user.getLastName();
            model.addAttribute("studentName", studentName);
            model.addAttribute("user", user);
        }
        return "studentDashboard";
    }


  //global method yet to be used if needed
    private String getStudentId(Authentication auth) {
        Object principal = auth.getPrincipal();
        if (principal instanceof Student student) {
            return student.getStudentId();
        }
        return "fallbackId"; // or redirect logic if you want
    }

}
