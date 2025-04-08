package group7.enrollmentSystem.repos;

import group7.enrollmentSystem.dtos.classDtos.StudentHoldHistoryDto;

import group7.enrollmentSystem.models.StudentHoldHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface StudentHoldHistoryRepo extends JpaRepository<StudentHoldHistory, Long>  {
    @Query("SELECT new group7.enrollmentSystem.dtos.classDtos.StudentHoldHistoryDto(" +
            "h.studentId, " +
            "s.firstName, " +
            "s.lastName, " +
            "s.email, " +
            "h.holdType, " +
            "CASE WHEN h.holdPlaced = true THEN 'Hold Placed' ELSE 'Hold Removed' END, " +
            "h.timestamp) " +
            "FROM StudentHoldHistory h " +
            "JOIN Student s ON h.studentId = s.id " +
            "ORDER BY h.timestamp DESC")
    List<StudentHoldHistoryDto> findAllHoldsHistory();

    @Query("SELECT new group7.enrollmentSystem.dtos.classDtos.StudentHoldHistoryDto(" +
            "h.studentId, " +
            "s.firstName, " +
            "s.lastName, " +
            "s.email, " +
            "h.holdType, " +
            "CASE WHEN h.holdPlaced = true THEN 'Hold Placed' ELSE 'Hold Removed' END, " +
            "h.timestamp) " +
            "FROM StudentHoldHistory h " +
            "JOIN Student s ON h.studentId = s.id " +
            "WHERE h.studentId = :studentId " +
            "ORDER BY h.timestamp DESC")
    List<StudentHoldHistoryDto> findHistoryByStudentId(Long studentId);
}
