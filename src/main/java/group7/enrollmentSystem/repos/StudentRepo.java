package group7.enrollmentSystem.repos;

import group7.enrollmentSystem.models.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface StudentRepo extends JpaRepository<Student, Long> {

  Optional<Student> findByEmail(String email);

  Optional<Student> findByStudentId(String studentId);

    @Query("SELECT COUNT(e) FROM CourseEnrollment e WHERE e.student = :student AND e.currentlyTaking = true")
    int getCurrentlyAppliedByStudent(Student student);

    @Query("SELECT s FROM Student s LEFT JOIN FETCH s.onHoldStatusList h")
    List<Student> findAllStudentsWithHolds();
}
