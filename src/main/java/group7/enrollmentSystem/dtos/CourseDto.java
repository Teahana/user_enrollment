package group7.enrollmentSystem.dtos;

import lombok.Data;

@Data
public class CourseDto {
    private String courseCode;
    private String title;
    private String description;
    private Short creditPoints;
    private Short level;
    private Boolean offeredSem1;
    private Boolean offeredSem2;
}
