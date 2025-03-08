package group7.enrollmentSystem.models;

import jakarta.persistence.*;
import lombok.Data;

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
}
