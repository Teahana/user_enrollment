package group7.enrollmentSystem.models;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
public class EnrollmentState {
    @Id
    private Long id;
    private boolean open;
    private boolean semesterOne;
}
