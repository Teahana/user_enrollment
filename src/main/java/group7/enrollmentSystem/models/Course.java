package group7.enrollmentSystem.models;

import jakarta.persistence.*;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;

@Entity
@Data
public class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    private String courseCode;
    private String description;
    private Short creditPoints;
    private Short level;
    private boolean offeredSem1;
    private boolean offeredSem2;

    @ManyToMany
    @JoinTable(
            name = "course_programmes",
            joinColumns = @JoinColumn(name = "course_id"),
            inverseJoinColumns = @JoinColumn(name = "programme_id")
    )
    private Set<Programme> programmes = new HashSet<>();

    @ManyToMany
    @JoinTable(
            name = "course_majors",
            joinColumns = @JoinColumn(name = "course_id"),
            inverseJoinColumns = @JoinColumn(name = "major_id")
    )
    private Set<Major> majors = new HashSet<>();

    @ManyToMany
    @JoinTable(
            name = "course_prerequisites",
            joinColumns = @JoinColumn(name = "course_id"),
            inverseJoinColumns = @JoinColumn(name = "prerequisite_id")
    )
    private Set<Course> prerequisites = new HashSet<>();

    @ManyToMany
    @JoinTable(
            name = "course_corequisites",
            joinColumns = @JoinColumn(name = "course_id"),
            inverseJoinColumns = @JoinColumn(name = "corequisite_id")
    )
    private Set<Course> corequisites = new HashSet<>();

    @ManyToMany
    @JoinTable(
            name = "course_elective_majors",
            joinColumns = @JoinColumn(name = "course_id"),
            inverseJoinColumns = @JoinColumn(name = "major_id")
    )
    private Set<Major> electiveMajors = new HashSet<>();
}
