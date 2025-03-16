package group7.enrollmentSystem.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    @Column(unique = true)
    private String courseCode;
    private String description;
    private Short creditPoints;
    private Short level;
    private double cost;
    private boolean offeredSem1;
    private boolean offeredSem2;

    public Course(String courseCode, String title, String description, short creditPoints, short level, boolean offeredSem1, boolean offeredSem2) {
        this.courseCode = courseCode;
        this.title = title;
        this.description = description;
        this.creditPoints = creditPoints;
        this.level = level;
        this.offeredSem1 = offeredSem1;
        this.offeredSem2 = offeredSem2;
    }
}
