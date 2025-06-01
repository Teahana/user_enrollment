package group7.enrollmentSystem.repos;

import group7.enrollmentSystem.enums.ApplicationStatus;
import group7.enrollmentSystem.models.GraduationApplication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GraduationApplicationRepo extends JpaRepository<GraduationApplication, Long> {
    Optional<GraduationApplication> findByStudent_StudentId(String studentId);
    List<GraduationApplication> findByStatus(ApplicationStatus status);
    List<GraduationApplication> findByProgramme_ProgrammeCode(String programmeCode);
}
