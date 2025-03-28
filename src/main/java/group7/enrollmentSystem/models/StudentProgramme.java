package group7.enrollmentSystem.models;

import group7.enrollmentSystem.enums.ProgrammeStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Entity
@Data
public class StudentProgramme {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;
    @ManyToOne
    @JoinColumn(name = "programme_id", nullable = false)
    private Programme programme;
    private boolean currentProgramme;

    //added timestamp tracking and status tracking
    private LocalDate dateEnrolled;
    private LocalDate dateCompleted;
    @Enumerated(EnumType.STRING)
    private ProgrammeStatus status; //ENROLLED, COMPLETED, DROPPED
}
