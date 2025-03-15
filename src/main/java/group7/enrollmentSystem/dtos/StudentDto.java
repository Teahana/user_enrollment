package group7.enrollmentSystem.dtos;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@ToString
public class StudentDto {
    private String studentId;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private String address;
}
