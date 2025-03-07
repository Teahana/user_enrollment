package group7.enrollmentSystem.models;

import jakarta.persistence.*;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;

@Entity
@Data
@Table(
        name = "students",
        indexes = {
                @Index(name = "idx_student_id", columnList = "studentId", unique = true)
        }
)
public class Student {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private String studentId;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private String address;
    @OneToMany(mappedBy = "student")
    private Set<StudentProgramme> programmes = new HashSet<>();
    @OneToMany(mappedBy = "student")
    private Set<StudentMajor> majors = new HashSet<>();
    @OneToMany(mappedBy = "student")
    private Set<CourseEnrollment> courseEnrollments = new HashSet<>();
}
