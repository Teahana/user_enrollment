package group7.enrollmentSystem.repos;

import group7.enrollmentSystem.models.EnrollmentState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface EnrollmentStateRepo extends JpaRepository<EnrollmentState, Long> {
    @Query("SELECT e.semesterOne FROM EnrollmentState e WHERE e.id = 1")
    boolean isSemesterOne();
}
