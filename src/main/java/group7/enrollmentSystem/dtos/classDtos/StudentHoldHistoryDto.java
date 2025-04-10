package group7.enrollmentSystem.dtos.classDtos;

import group7.enrollmentSystem.enums.OnHoldTypes;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class StudentHoldHistoryDto {
    private Long studentId;
    private String firstName;
    private String lastName;
    private String email;
    private OnHoldTypes holdType;
    private String action; // "Hold Placed" or "Hold Removed"
    private LocalDateTime timestamp;
    private String actionBy;

    public StudentHoldHistoryDto(Long studentId, String firstName, String lastName, String email,
                                 OnHoldTypes holdType, String action, LocalDateTime timestamp,
                                 String actionBy) {
        this.studentId = studentId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.holdType = holdType;
        this.action = action;
        this.timestamp = timestamp;
        this.actionBy = actionBy;
    }
}