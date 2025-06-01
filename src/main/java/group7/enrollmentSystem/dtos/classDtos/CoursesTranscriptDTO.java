package group7.enrollmentSystem.dtos.classDtos;

import lombok.Data;
import java.util.List;

@Data
public class CoursesTranscriptDTO {
    private String studentId;
    private String studentName;
    private String programme;
    private List<CourseTranscriptRow> transcriptRows;
    private double gpa;

    @Data
    public static class CourseTranscriptRow {
        private String courseCode;
        private String title;
        private String grade;
        private int mark;
        private boolean failed;
    }
}
