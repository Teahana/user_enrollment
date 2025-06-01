package group7.enrollmentSystem.controllers;

import group7.enrollmentSystem.dtos.formDtos.GraduationFormDTO;
import group7.enrollmentSystem.helpers.EmailService;
import group7.enrollmentSystem.services.FormsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.security.Principal;

@RestController
@RequestMapping("/external")
public class ExternalFormController {

    private final EmailService emailService;
    private final RestTemplate restTemplate;
    private final FormsService graduationApplicationService;

    public ExternalFormController(RestTemplate restTemplate, EmailService emailService, FormsService graduationApplicationService) {
        this.restTemplate = restTemplate;
        this.emailService = emailService;
        this.graduationApplicationService = graduationApplicationService;
    }

    @GetMapping("/graduation/form")
    public ResponseEntity<String> fetchGraduationForm() {
        String url = "http://localhost:3000/forms/graduation";
        String formHtml = restTemplate.getForObject(url, String.class);
        return ResponseEntity.ok(formHtml);
    }

    @PostMapping("/graduation_submit")
    public ResponseEntity<String> submitGraduationForm(Principal principal, @RequestBody GraduationFormDTO form) {
        String email = principal.getName();
        graduationApplicationService.processGraduationForm(email);
        return ResponseEntity.ok("Application submitted and emails sent.");
    }


}
