package group7.enrollmentSystem.dtos.classDtos;

import group7.enrollmentSystem.enums.OnHoldTypes;
import group7.enrollmentSystem.models.OnHoldStatus;
import lombok.Data;

import java.util.List;

@Data
public class StudentHoldViewDto {
    private String studentId;
    private String fullName;
    private String email;
    private boolean hasHold;
    //private OnHoldTypes holdType;
    private List<OnHoldStatus> activeHolds;
    private String holdMessage;

    private boolean canRegisterCourses;
    private boolean canViewCompletedCourses;
    private boolean canViewStudentAudit;
    private boolean canGenerateTranscript;
    private boolean canApplyForGraduation;
}
