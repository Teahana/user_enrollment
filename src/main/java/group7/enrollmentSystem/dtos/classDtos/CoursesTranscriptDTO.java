package group7.enrollmentSystem.dtos.classDtos;

import lombok.Data;
import java.util.List;

import group7.enrollmentSystem.models.CourseEnrollment;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CoursesTranscriptDTO {

    private String studentId;
    private String studentName;
    private String programme;
    private double gpa;

    private List<CourseTranscriptRow> completedCourses = new ArrayList<>();
    private List<CourseTranscriptRow> passedCourses = new ArrayList<>();
    private List<CourseTranscriptRow> failedCourses = new ArrayList<>();

    @Data
    public static class CourseTranscriptRow {
        private String courseCode;
        private String title;
        private String grade;
        private int mark;
        private boolean failed;
    }

    /**
     * Static builder to extract data from completed CourseEnrollments.
     */
    public static CoursesTranscriptDTO fromCourseEnrollments(
            String studentId,
            String studentName,
            String programme,
            List<CourseEnrollment> completedEnrollments
    ) {
        CoursesTranscriptDTO dto = new CoursesTranscriptDTO();
        dto.setStudentId(studentId);
        dto.setStudentName(studentName);
        dto.setProgramme(programme);

        double totalMarks = 0;
        int gradedCourses = 0;

        for (CourseEnrollment enrollment : completedEnrollments) {
            CourseTranscriptRow row = new CourseTranscriptRow();
            row.setCourseCode(enrollment.getCourse().getCourseCode());
            row.setTitle(enrollment.getCourse().getTitle());
            row.setGrade(enrollment.getGrade());
            row.setMark(enrollment.getMark());
            row.setFailed(enrollment.isFailed());

            dto.getCompletedCourses().add(row);

            if (enrollment.isFailed()) {
                dto.getFailedCourses().add(row);
            } else {
                dto.getPassedCourses().add(row);
            }

            if (enrollment.getGrade() != null && !enrollment.getGrade().equalsIgnoreCase("N/A")) {
                totalMarks += enrollment.getMark();
                gradedCourses++;
            }
        }

        // GPA as average mark divided by 25 (assuming A+ = 100, F = 0, GPA 4.0 scale)
        dto.setGpa(gradedCourses > 0 ? (totalMarks / gradedCourses) / 25.0 : 0.0);

        return dto;
    }
}
