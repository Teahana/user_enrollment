package group7.enrollmentSystem.models;

import group7.enrollmentSystem.enums.OnHoldTypes;
import jakarta.persistence.*;
import lombok.Data;

import javax.annotation.processing.Generated;

@Entity
@Data
public class OnHoldStatus {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING)
    private OnHoldTypes onHoldType;
    private boolean onHold;
}
