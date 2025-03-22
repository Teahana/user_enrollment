package group7.enrollmentSystem.repos;

import group7.enrollmentSystem.dtos.classDtos.CourseEnrollmentDto;
import group7.enrollmentSystem.models.CourseEnrollment;
import group7.enrollmentSystem.models.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CourseEnrollmentRepo extends JpaRepository<CourseEnrollment, Long> {

    @Query("SELECT ce.course.id AS courseId, ce.course.courseCode AS courseCode, ce.currentlyTaking AS currentlyTaking " +
            "FROM CourseEnrollment ce WHERE ce.student.id = :studentId")
    List<CourseEnrollmentDto> getCourseEnrollments(@Param("studentId") Long studentId);

    boolean existsByStudentIdAndCourseIdAndCompletedTrue(Long studentId, Long courseId);

    List<CourseEnrollment> findByStudentIdAndCurrentlyTakingTrue(Long studentId);
    List<CourseEnrollment> findByStudentIdAndCurrentlyTakingFalseAndCompletedFalse(Long studentId);
    List<CourseEnrollment> findByStudent(Student student);
    List<CourseEnrollment> findByStudentIdAndCompletedTrue(Long studentId);

    @Query("SELECT ce FROM CourseEnrollment ce WHERE ce.student.id = :studentId")
    List<CourseEnrollment> exampleForGettingByLongId(@Param("studentId") Long id);
}


