package group7.enrollmentSystem.dtos.formDtos;

import group7.enrollmentSystem.models.Programme;
import lombok.Data;

import java.time.LocalDate;
import java.util.Date;

@Data
public class GraduationFormDTO {
    private String programme;
    private LocalDate dateOfBirth;
    private String major1;
    private String major2;
    private String minor;

    private String ceremonyPreference;
    private String otherCampus;

    private Boolean willAttend;

    private String studentSignature;

    private LocalDate signatureDate;

}
