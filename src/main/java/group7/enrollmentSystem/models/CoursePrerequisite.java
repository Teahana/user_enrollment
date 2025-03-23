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
@Table(name = "course_prerequisite")
public class CoursePrerequisite {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "course_id", nullable = false)
    private Course course; // The main course
    @ManyToOne
    @JoinColumn(name = "prerequisite_id", nullable = false)
    private Course prerequisite; // The prerequisite course
    @ManyToOne
    @JoinColumn(name = "programme_id", nullable = true)
    private Programme programme;
    @Enumerated(EnumType.STRING)
    private PrerequisiteType prerequisiteType;
    @Enumerated(EnumType.STRING)
    private PrerequisiteType operatorToNext;
    private int groupId;
    private boolean isParent;
    private boolean isChild;
    private int childId;
    private int parentId;
}


