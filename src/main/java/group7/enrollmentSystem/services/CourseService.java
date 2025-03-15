package group7.enrollmentSystem.services;

import group7.enrollmentSystem.models.Course;
import group7.enrollmentSystem.repos.CourseRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepo courseRepo;

    // Create a new course
    public void saveCourse(String courseCode, String title, String description, Short creditPoints, Short level, Boolean offeredSem1, Boolean offeredSem2) {
        Course course = new Course();
        course.setCourseCode(courseCode);
        course.setTitle(title);
        course.setDescription(description);
        course.setCreditPoints(creditPoints);
        course.setLevel(level);
        course.setOfferedSem1(offeredSem1);
        course.setOfferedSem2(offeredSem2);
        courseRepo.save(course);
    }

    // Get all courses recordss
    public List<Course> getAllCourses() {
        return courseRepo.findAll();
    }

    // Get a single course record
    public Optional<Course> getCourseByCode(String courseCode) {
        return courseRepo.findByCourseCode(courseCode);
    }

    // Update a course
    public void updateCourse(String courseCode, String title, String description, Short creditPoints, Short level, Boolean offeredSem1, Boolean offeredSem2) {
        Optional<Course> optionalCourse = courseRepo.findByCourseCode(courseCode);
        if (optionalCourse.isPresent()) {
            Course course = optionalCourse.get();
            course.setTitle(title);
            course.setDescription(description);
            course.setCreditPoints(creditPoints);
            course.setLevel(level);
            course.setOfferedSem1(offeredSem1);
            course.setOfferedSem2(offeredSem2);
            courseRepo.save(course);
        } else {
            throw new RuntimeException("Course not found with code: " + courseCode);
        }
    }

    // Delete a course record
    public void deleteCourse(String courseCode) {
        Optional<Course> optionalCourse = courseRepo.findByCourseCode(courseCode);
        if (optionalCourse.isPresent()) {
            courseRepo.delete(optionalCourse.get());
        } else {
            throw new RuntimeException("Course not found with code: " + courseCode);
        }
    }
}
