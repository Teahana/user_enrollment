package group7.enrollmentSystem.repos;

import group7.enrollmentSystem.dtos.interfaceDtos.ProgrammeIdAndCode;
import group7.enrollmentSystem.models.Programme;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProgrammeRepo extends JpaRepository<Programme, Long> {
    Optional<Programme> findByProgrammeCode(String programmeCode);

    List<ProgrammeIdAndCode> findAllBy();
}
