package group7.enrollmentSystem.dtos.classDtos;

import group7.enrollmentSystem.enums.OnHoldTypes;
import lombok.Data;

@Data
public class HoldRestrictionDto {
    private Long id;
    private OnHoldTypes holdType;
    private boolean blockCourseEnrollment;
    private boolean blockViewCompletedCourses;
    private boolean blockStudentAudit;
    private boolean blockGenerateTranscript;
    private boolean blockGraduationApplication;
}
