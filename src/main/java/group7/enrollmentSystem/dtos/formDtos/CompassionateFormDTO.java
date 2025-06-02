package group7.enrollmentSystem.dtos.formDtos;


import group7.enrollmentSystem.models.CompassionateApplication;
import lombok.Data;
import lombok.ToString;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@ToString
public class CompassionateFormDTO {
    // Application options
    private List<String> applicationType;
    private String reason;
    private MultipartFile[] documents;
    private String studentSignature;
    private LocalDate submissionDate;

    // Personal Details (read-only in form)
    private String lastName;
    private String firstName;
    private LocalDate dateOfBirth;
    private String studentId;
    private String email;
    private String telephone;
    private String address;
    private String campus;
    private String semesterYear;

    //Parallel lists for missed exams (binds directly from the form)
    private List<String> courseCode;
    private List<LocalDate> examDate;
    private List<LocalTime> examTime;
}

