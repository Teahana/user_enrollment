package group7.enrollmentSystem.models;

import group7.enrollmentSystem.enums.OnHoldTypes;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Data
public class StudentHoldHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long studentId;

    @Enumerated(EnumType.STRING)
    private OnHoldTypes holdType;

    private boolean holdPlaced;

    @CreationTimestamp
    private LocalDateTime timestamp;

    public static StudentHoldHistory create(Long studentId, OnHoldTypes holdType, boolean holdPlaced) {
        StudentHoldHistory history = new StudentHoldHistory();
        history.setStudentId(studentId);
        history.setHoldType(holdType);
        history.setHoldPlaced(holdPlaced);
        return history;
    }
}