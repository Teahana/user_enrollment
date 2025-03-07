package group7.enrollmentSystem.models;

import group7.enrollmentSystem.enums.EnrollmentStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Data
public class UniversityEnrollment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private String address;
    private LocalDate dateOfApplication;

    @Enumerated(EnumType.STRING)
    private EnrollmentStatus status; // "PENDING", "APPROVED", "REJECTED"

    @ElementCollection
    private Set<String> filePaths = new HashSet<>(); // Stores file paths only
}

