package group7.enrollmentSystem.repos;

import group7.enrollmentSystem.dtos.interfaceDtos.CourseIdAndCode;
import group7.enrollmentSystem.models.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;


public interface CourseRepo extends JpaRepository<Course, Long> {
    Optional<Course> findByCourseCode(String courseCode);
    List<Course> findByIdNot(Long id);
    List<CourseIdAndCode> findAllBy();
    List<Course> findByCourseCodeIn(List<String> courseReqs);
}
