package group7.enrollmentSystem.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serializable;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Entity
@Data
@PrimaryKeyJoinColumn(name = "id")
@AttributeOverrides({
        @AttributeOverride(name = "email", column = @Column(name = "email", nullable = false, unique = true)),
        @AttributeOverride(name = "firstName", column = @Column(name = "first_name")),
        @AttributeOverride(name = "lastName", column = @Column(name = "last_name"))
})
@NoArgsConstructor
public class Student extends User implements UserDetails {
    @Column(nullable = false, unique = true)
    private String studentId;
    private String phoneNumber;
    private String address;

    public Student(String studentId, String firstName, String lastName, String address, String phoneNumber) {
        this.studentId = studentId;
        this.setFirstName(firstName);
        this.setLastName(lastName);
        this.address = address;
        this.phoneNumber = phoneNumber;
        this.getRoles().add("ROLE_STUDENT");
    }
    @OneToMany(mappedBy = "student")
    private List<CourseEnrollment> enrollments;

    @OneToMany(mappedBy = "student")
    private List<StudentProgramme> studentProgrammes;

}
