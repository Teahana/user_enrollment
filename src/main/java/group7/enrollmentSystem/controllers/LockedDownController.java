package group7.enrollmentSystem.controllers;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
public class LockedDownController {
    @PostMapping("hello")
    public String hello() {
        return "Hello, World!";
    }
}
