package group7.enrollmentSystem.dtos.classDtos;

import lombok.Data;

import java.util.List;

@Data
public class CourseEnrollDto {
    private Long courseId;
    private String courseTitle;
    private String courseCode;
    private String description;
    private List<String> prerequisiteCodes;
}
