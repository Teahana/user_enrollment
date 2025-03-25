package group7.enrollmentSystem.services;

import group7.enrollmentSystem.models.Course;
import group7.enrollmentSystem.models.CoursePrerequisite;
import group7.enrollmentSystem.repos.CoursePrerequisiteRepo;
import group7.enrollmentSystem.repos.CourseRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CoursePrerequisiteService {

    private final CoursePrerequisiteRepo coursePrerequisiteRepo;
    private final CourseRepo courseRepo;

    // Create a new CoursePrerequisite record
    public void saveCoursePrerequisite(String courseCode, String prerequisiteCode) {
        Optional<Course> course = courseRepo.findByCourseCode(courseCode);
        Optional<Course> prerequisite = courseRepo.findByCourseCode(prerequisiteCode);

        if (course.isPresent() && prerequisite.isPresent()) {
            CoursePrerequisite coursePrerequisite = new CoursePrerequisite();
            coursePrerequisite.setCourse(course.get());
            coursePrerequisite.setPrerequisite(prerequisite.get());
            coursePrerequisiteRepo.save(coursePrerequisite);
        } else {
            throw new RuntimeException("Course or Prerequisite not found");
        }
    }

    // Get all CoursePrerequisite records
    public List<CoursePrerequisite> getAllCoursePrerequisites() {
        return coursePrerequisiteRepo.findAll();
    }

    // Get 1 CoursePrerequisite record
    public Optional<CoursePrerequisite> getCoursePrerequisiteById(Long id) {
        return coursePrerequisiteRepo.findById(id);
    }

    // Delete a CoursePrerequisite record
    public void deleteCoursePrerequisite(Long id) {
        coursePrerequisiteRepo.deleteById(id);
    }
}
