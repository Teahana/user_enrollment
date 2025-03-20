package group7.enrollmentSystem.repos;

import group7.enrollmentSystem.models.Student;
import group7.enrollmentSystem.models.StudentProgramme;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentProgrammeRepo extends JpaRepository<StudentProgramme, Long> {
    Optional<StudentProgramme> findByStudentAndCurrentProgrammeTrue(Student student);
    List<StudentProgramme> findByStudent_StudentId(String studentId);
}
