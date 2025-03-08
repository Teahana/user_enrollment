package group7.enrollmentSystem.models;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class Major {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String majorCode;
}
