package group7.enrollmentSystem.controllers;

import group7.enrollmentSystem.dtos.formDtos.GraduationFormDTO;
import group7.enrollmentSystem.helpers.EmailService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@RestController
@RequestMapping("/external")
public class ExternalFormController {

    private final EmailService emailService;
    private final RestTemplate restTemplate;

    public ExternalFormController(RestTemplate restTemplate, EmailService emailService) {
        this.restTemplate = restTemplate;
        this.emailService = emailService;
    }

    @GetMapping("/graduation/form")
    public ResponseEntity<String> fetchGraduationForm() {
        String url = "http://localhost:3000/forms/graduation";
        String formHtml = restTemplate.getForObject(url, String.class);
        return ResponseEntity.ok(formHtml);
    }

    @PostMapping("/graduation-submit-test")
    public ResponseEntity<String> submitGraduationForm(@RequestBody GraduationFormDTO form) {
        System.out.println("Received POST /graduation-submit-test for: " + form.getEmail());

        // Simulated eligibility check
        if (!form.getStudentId().isEmpty() && form.getProgramme().equalsIgnoreCase("BSE")) {

            emailService.sendHtmlMail(
                    form.getEmail(),
                    "USP Graduation Application",
                    "graduation-email", // Thymeleaf template in resources/templates
                    Map.of(
                            "studentId", form.getStudentId(),
                            "fullName", form.getFullName(),
                            "programme", form.getProgramme(),
                            "email", form.getEmail()
                    )
            );

            System.out.println("Email sent to " + form.getEmail());
            return ResponseEntity.ok("Form received. Email sent!");
        } else {
            System.out.println("Not eligible");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Not eligible or incomplete.");
        }
    }

    // Optional debug route (keep for testing)
    @PostMapping("/debug")
    public ResponseEntity<String> debugTest(@RequestBody Map<String, Object> body) {
        System.out.println("Debug POST hit: " + body);
        return ResponseEntity.ok("Debug received");
    }


}
