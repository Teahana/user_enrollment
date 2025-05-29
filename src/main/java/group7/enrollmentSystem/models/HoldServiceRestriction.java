package group7.enrollmentSystem.models;

import group7.enrollmentSystem.enums.OnHoldTypes;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class HoldServiceRestriction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(unique = true)
    private OnHoldTypes holdType;

    private boolean blockCourseEnrollment = true;
    private boolean blockViewCompletedCourses = true;
    private boolean blockStudentAudit = true;
    private boolean blockGenerateTranscript = true;
    private boolean blockGraduationApplication = true;
}
