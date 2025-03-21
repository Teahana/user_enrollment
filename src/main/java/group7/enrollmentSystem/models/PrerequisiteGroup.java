package group7.enrollmentSystem.models;

import group7.enrollmentSystem.enums.PrerequisiteType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "prerequisite_group")
public class PrerequisiteGroup {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PrerequisiteType type; // AND / OR

    @ManyToOne
    @JoinColumn(name = "parent_group_id")
    private PrerequisiteGroup parentGroup; // Self-referencing for nesting

    @ManyToOne
    @JoinColumn(name = "next_group_id")
    private PrerequisiteGroup nextGroup; // For linking groups at the same level

    @Enumerated(EnumType.STRING)
    private PrerequisiteType operatorToNext; // Operator (AND / OR) for linking groups
    @OneToMany(mappedBy = "parentGroup", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PrerequisiteGroup> subGroups = new ArrayList<>();

    @OneToMany(mappedBy = "prerequisiteGroup", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CoursePrerequisite> prerequisites = new ArrayList<>();
}
