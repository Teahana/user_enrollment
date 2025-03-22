package group7.enrollmentSystem.repos;

import group7.enrollmentSystem.enums.EnrollmentStatus;
import group7.enrollmentSystem.models.EnrollmentState;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnrollmentStatusRepo extends JpaRepository<EnrollmentState, Long> {
}
