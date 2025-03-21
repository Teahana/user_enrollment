package group7.enrollmentSystem.repos;

import group7.enrollmentSystem.models.Course;
import group7.enrollmentSystem.models.CoursePrerequisite;
import group7.enrollmentSystem.dtos.interfaceDtos.CoursePrerequisiteDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CoursePrerequisiteRepo extends JpaRepository<CoursePrerequisite, Long> {
    @Query("SELECT cp.prerequisite FROM CoursePrerequisite cp WHERE cp.course.id = :courseId")
    List<Course> findPrerequisitesByCourseId(@Param("courseId") Long courseId);

    @Query("SELECT cp.prerequisite.id as prerequisiteId, cp.prerequisite.courseCode as prerequisiteCode " +
            "FROM CoursePrerequisite cp WHERE cp.course.id = :courseId")
    List<CoursePrerequisiteDto> findPrerequisitesByCourseIdForEnrollment(Long courseId);

    List<CoursePrerequisite> findByCourse(Course course);
}


