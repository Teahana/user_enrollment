package group7.enrollmentSystem.dtos;

import group7.enrollmentSystem.models.Course;
import lombok.Data;

import java.util.List;

@Data
public class CourseDto {
    private String courseCode;
    private String title;
    private String description;
    private Short creditPoints;
    private Short level;
    private Boolean offeredSem1;
    private Boolean offeredSem2;
    private List<Course> prerequisites;
}
