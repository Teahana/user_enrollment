package group7.enrollmentSystem.models;

import group7.enrollmentSystem.enums.ApplicationStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
public class CompassionateApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Student student;

    private String applicationType;
    private String reason;

    @ElementCollection
    @CollectionTable(name = "missed_exam_entries", joinColumns = @JoinColumn(name = "application_id"))
    private List<MissedExamEntry> examEntries = new ArrayList<>();

    @Lob
    private String studentSignature;

    private LocalDate submissionDate;

    private String documentPaths; // Optional for future file handling

    @Enumerated(EnumType.STRING)
    private ApplicationStatus status;

    @Embeddable
    @Data
    public static class MissedExamEntry {
        private String courseCode;
        private LocalDate examDate;
        private LocalTime examTime;
    }
}



