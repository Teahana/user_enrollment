package group7.enrollmentSystem.repos;

import group7.enrollmentSystem.models.Course;
import group7.enrollmentSystem.models.CourseProgramme;
import group7.enrollmentSystem.models.Programme;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseProgrammeRepo extends JpaRepository<CourseProgramme, Long> {
    List<CourseProgramme> findByProgrammeId(Long programme_id);
    @Query("SELECT cp.programme FROM CourseProgramme cp WHERE cp.course.id = :courseId")
    List<Programme> findProgrammesByCourseId(@Param("courseId") Long courseId);
    // Fetch all courses linked to a specific programme
    @Query("SELECT cp.course FROM CourseProgramme cp WHERE cp.programme.programmeCode = :programmeCode")
    List<Course> findCoursesByProgrammeCode(@Param("programmeCode") String programmeCode);

    // Check if a course is already linked to a programme
    boolean existsByCourseAndProgramme(Course course, Programme programme);

    // Find a CourseProgramme record by course and programme
    Optional<CourseProgramme> findByCourseAndProgramme(Course course, Programme programme);

    List<CourseProgramme> findByProgramme(Programme programme);
    @Query("SELECT cp.course FROM CourseProgramme cp WHERE cp.programme = :currentProgramme")
    List<Course> findAllByProgramme(Programme currentProgramme);
}
