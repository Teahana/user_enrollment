package group7.enrollmentSystem.dtos.formDtos;


import group7.enrollmentSystem.models.CompassionateApplication;
import lombok.Data;
import lombok.ToString;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import jakarta.validation.constraints.*;
import org.springframework.web.multipart.MultipartFile;

@Data
@ToString
public class CompassionateFormDTO {

    // At least one option must be selected
    @NotEmpty(message = "Please select at least one application type")
    private List<String> applicationType;

    @NotBlank(message = "Reason is required")
    private String reason;

    private MultipartFile[] documents;

    @NotBlank(message = "Signature is required")
    private String studentSignature;

    @NotNull(message = "Submission date is required")
    private LocalDate submissionDate;



    @NotNull(message = "Date of birth is required")
    private LocalDate dateOfBirth;

    @NotBlank(message = "Student ID is required")
    private String studentId;

    @NotBlank(message = "Email is required")
    //@Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "Phone number is required")
    private String telephone;

    @NotBlank(message = "Address is required")
    private String address;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotBlank(message = "First name is required")
    private String firstName;


    @NotBlank(message = "Campus is required")
    private String campus;

    @NotBlank(message = "Semester/Year is required")
    private String semesterYear;

    // Missed exams — each list should have at least one valid entry
    @NotEmpty(message = "At least one course code is required")
    private List<@NotBlank(message = "Course code cannot be blank") String> courseCode;

    @NotEmpty(message = "At least one exam date is required")
    private List<@NotNull(message = "Exam date is required") LocalDate> examDate;

    @NotEmpty(message = "At least one exam time is required")
    private List<@NotNull(message = "Exam time is required") LocalTime> examTime;
}


