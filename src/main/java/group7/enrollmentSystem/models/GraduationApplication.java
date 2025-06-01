package group7.enrollmentSystem.models;

import group7.enrollmentSystem.enums.ApplicationStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
public class GraduationApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "student_id", referencedColumnName = "studentId", nullable = false)
    private Student student;

    private LocalDate dateOfBirth;

    @ManyToOne
    @JoinColumn(name = "programme_id", nullable = false)
    private Programme programme;

    private String major1;
    private String major2;
    private String minor;

    private String ceremonyPreference;
    private String otherCampus;

    private Boolean willAttend;

    private String studentSignatureFilePath;
    private LocalDate signatureDate;

    private LocalDateTime submittedAt = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    private ApplicationStatus status;
}
