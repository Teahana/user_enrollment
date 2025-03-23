package group7.enrollmentSystem.dtos.classDtos;

import group7.enrollmentSystem.models.Course;
import group7.enrollmentSystem.models.CourseEnrollment;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class EnrollmentPageData {
    private List<CourseEnrollment> activeEnrollments;
    private List<CourseEnrollment> canceledEnrollments;
    private List<Course> eligibleCourses;
}

