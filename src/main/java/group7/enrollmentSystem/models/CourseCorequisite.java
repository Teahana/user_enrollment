package group7.enrollmentSystem.models;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class CourseCorequisite {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne
    @JoinColumn(name = "corequisite_id", nullable = false)
    private Course corequisite;
}
