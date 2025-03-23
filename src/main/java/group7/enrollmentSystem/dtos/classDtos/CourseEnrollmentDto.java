package group7.enrollmentSystem.dtos.classDtos;

import lombok.Data;

@Data
public class CourseEnrollmentDto {
    private Long courseId;
    private String courseCode;
    private String title;
    private double cost;

    public CourseEnrollmentDto(Long courseId, String courseCode, String title, double cost) {
        this.courseId = courseId;
        this.courseCode = courseCode;
        this.title = title;
        this.cost = cost;
    }
}