package group7.enrollmentSystem.repos;

import group7.enrollmentSystem.models.CoursePrerequisite;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CoursePrerequisiteRepo extends JpaRepository<CoursePrerequisite, Long> {
}
