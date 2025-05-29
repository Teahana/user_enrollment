package group7.enrollmentSystem.dtos.classDtos;

import group7.enrollmentSystem.enums.OnHoldTypes;
import lombok.Data;
import java.util.List;

@Data
public class StudentHoldDto {
    private Long studentId;
    private String email;
    private String firstName;
    private String lastName;
    private String studentNumber;
    private boolean onHold;
    private List<OnHoldTypes> activeHolds;

    public StudentHoldDto(Long studentId, String email, String firstName,
                          String lastName, String studentNumber,
                          boolean onHold, List<OnHoldTypes> activeHolds) {
        this.studentId = studentId;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.studentNumber = studentNumber;
        this.onHold = onHold;
        this.activeHolds = activeHolds;
    }
}