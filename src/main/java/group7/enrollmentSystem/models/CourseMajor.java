package group7.enrollmentSystem.models;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class CourseMajor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne
    @JoinColumn(name = "major_id", nullable = false)
    private Major major;
}
