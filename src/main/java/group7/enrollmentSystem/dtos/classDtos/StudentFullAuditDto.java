package group7.enrollmentSystem.dtos.classDtos;

import group7.enrollmentSystem.enums.ProgrammeStatus;
import lombok.Data;

import java.util.List;

@Data
public class StudentFullAuditDto {
    private String studentId;
    private String studentName;
    private String programmeName;
    private String status;
    private List<CourseAuditDto> programmeCourses;

    public StudentFullAuditDto(String studentId, String studentName, String programmeName, String status, List<CourseAuditDto> programmeCourses) {
        this.studentId = studentId;
        this.studentName = studentName;
        this.programmeName = programmeName;
        this.status = status;
        this.programmeCourses = programmeCourses;
    }
}
