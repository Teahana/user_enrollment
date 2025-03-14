package group7.enrollmentSystem.repos;

import group7.enrollmentSystem.models.Student;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentRepo extends JpaRepository<Student, Long> {

}
