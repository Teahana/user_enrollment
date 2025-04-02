package group7.enrollmentSystem.models;

import java.util.List;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Programme {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private String programmeCode;
    private String name;
    private String faculty;
    public Programme(String programmeCode, String name, String faculty) {
        this.programmeCode = programmeCode;
        this.name = name;
        this.faculty = faculty;
    }

}

