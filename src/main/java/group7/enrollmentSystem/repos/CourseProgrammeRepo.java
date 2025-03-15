package group7.enrollmentSystem.repos;

import group7.enrollmentSystem.models.CourseProgramme;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseProgrammeRepo extends JpaRepository<CourseProgramme, Long> {
    List<CourseProgramme> findByProgrammeId(Long programme_id);
}
