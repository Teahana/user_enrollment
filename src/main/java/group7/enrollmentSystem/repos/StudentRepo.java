package group7.enrollmentSystem.repos;

import group7.enrollmentSystem.models.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StudentRepo extends JpaRepository<Student, Long> {

   Optional<Student> findByEmail(String email);

    Optional<Student> findByStudentId(String studentId);
}
