package group7.enrollmentSystem.repos;

import group7.enrollmentSystem.models.CompassionateApplication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface CompassionateApplicationRepo extends JpaRepository<CompassionateApplication, Long> {
    List<CompassionateApplication> findByStudent_StudentId(String studentId);
}
