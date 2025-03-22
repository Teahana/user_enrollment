package group7.enrollmentSystem.dtos.classDtos;

import lombok.Data;

import java.util.List;

@Data
public class InvoiceDto {
    private String studentName;
    private String studentId;
    private String programme;
    private List<CourseEnrollmentDto> enrolledCourses;
    private double totalDue;
}