package group7.enrollmentSystem.models;

import group7.enrollmentSystem.models.Course;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Data
public class CourseEnrollment {
   @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;
    @ManyToOne
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;
    private boolean completed;
    private boolean failed;
    private boolean cancelled;
    private int mark;
    private String grade;
    private boolean paid;
    private LocalDate dateEnrolled;
    private boolean currentlyTaking;
    private int semesterEnrolled;
    private boolean requestGradeChange;
    private LocalDate requestGradeChangeDate;
    private LocalTime requestGradeChangeTime;
    @ManyToOne
    @JoinColumn(name = "programme_id")
    private Programme programme;
}
