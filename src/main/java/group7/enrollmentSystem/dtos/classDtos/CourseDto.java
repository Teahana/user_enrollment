package group7.enrollmentSystem.dtos.classDtos;

import lombok.Data;

import java.util.List;

@Data
public class CourseDto {
    private Long id;
    private String courseCode;
    private String title;
    private String description;
    private double creditPoints;
    private Short level;
    private boolean offeredSem1;
    private boolean offeredSem2;
    private List<String> prerequisites;
    private List<String> programmes;
}
