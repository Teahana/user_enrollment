package group7.enrollmentSystem.repos;

import group7.enrollmentSystem.models.CourseEnrollment;
import group7.enrollmentSystem.models.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseEnrollmentRepo extends JpaRepository<CourseEnrollment, Long> {
    List<CourseEnrollment> findByStudentAndCurrentlyTakingTrue(Student student);
    List<CourseEnrollment> findByStudentAndCurrentlyTakingFalse(Student student);
    List<CourseEnrollment> findByStudent(Student student);
    
    @Query("SELECT ce FROM CourseEnrollment ce WHERE ce.student.id = :studentId")
    List<CourseEnrollment> exampleForGettingByLongId(@Param("studentId") Long id);
}


