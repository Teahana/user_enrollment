package group7.enrollmentSystem.repos;

import group7.enrollmentSystem.dtos.classDtos.StudentHoldDto;
import group7.enrollmentSystem.models.Student;
import group7.enrollmentSystem.models.StudentHoldHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.security.core.userdetails.User;

import java.util.List;
import java.util.Optional;

public interface StudentRepo extends JpaRepository<Student, Long> {

   Optional<Student> findByEmail(String email);

    Optional<Student> findByStudentId(String studentId);

    @Query("SELECT COUNT(e) FROM CourseEnrollment e WHERE e.student = :student AND e.applied = true")
    int getCurrentlyAppliedByStudent(Student student);
<<<<<<< Updated upstream
=======

    @Query("SELECT new group7.enrollmentSystem.dtos.classDtos.StudentHoldDto(" +
            "s.id, s.email, s.firstName, s.lastName, s.studentId, " +
            "CASE WHEN (EXISTS (SELECT 1 FROM s.onHoldStatusList h WHERE h.onHold = true)) THEN true ELSE false END, " +
            "(SELECT h.onHoldType FROM s.onHoldStatusList h WHERE h.onHold = true)) " +
            "FROM Student s")
    List<StudentHoldDto> findAllStudentsWithHoldStatus();
>>>>>>> Stashed changes
}
