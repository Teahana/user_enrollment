package group7.enrollmentSystem.repos;

import group7.enrollmentSystem.models.PrerequisiteGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PrerequisiteGroupRepository extends JpaRepository<PrerequisiteGroup, Long> {
}
