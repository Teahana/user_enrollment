package group7.enrollmentSystem.models;

import jakarta.persistence.*;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;

@Entity
@Data
public class Major {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String majorCode;
    @OneToMany(mappedBy = "major")
    private Set<StudentMajor> students = new HashSet<>();
    @ManyToMany(mappedBy = "majors")
    private Set<Course> courses = new HashSet<>();
    @ManyToMany
    private Set<Course> electives = new HashSet<>();
}
