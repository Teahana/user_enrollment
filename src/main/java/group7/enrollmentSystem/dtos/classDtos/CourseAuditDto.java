package group7.enrollmentSystem.dtos.classDtos;

import lombok.Data;

@Data
public class CourseAuditDto {
    private Long id;
    private String title;
    private String courseCode;
    private boolean enrolled;
    private boolean completed;
    private Short level;

    public CourseAuditDto(Long id,String title, String courseCode, boolean enrolled, Short level, boolean completed) {
        this.title = title;
        this.courseCode = courseCode;
        this.enrolled = enrolled;
        this.level = level;
        this.completed = completed;
        this.id = id;
    }
}
