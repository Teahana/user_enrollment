package group7.enrollmentSystem.models;

import group7.enrollmentSystem.enums.PrerequisiteType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "course_prerequisite",
        uniqueConstraints = {@UniqueConstraint(columnNames = {"course_id", "prerequisite_id", "group_id"})})
public class CoursePrerequisite {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne
    @JoinColumn(name = "prerequisite_id", nullable = false)
    private Course prerequisite;

    @Enumerated(EnumType.STRING)
    private PrerequisiteType prerequisiteType; // AND / OR

    @Column(nullable = false)
    private int groupId; // Groups related AND/OR conditions
}

