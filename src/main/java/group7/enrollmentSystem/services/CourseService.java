package group7.enrollmentSystem.services;

import group7.enrollmentSystem.dtos.classDtos.CourseDto;
import group7.enrollmentSystem.models.Course;
import group7.enrollmentSystem.models.CoursePrerequisite;
import group7.enrollmentSystem.models.Programme;
import group7.enrollmentSystem.repos.CoursePrerequisiteRepo;
import group7.enrollmentSystem.repos.CourseProgrammeRepo;
import group7.enrollmentSystem.repos.CourseRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepo courseRepo;
    private final CoursePrerequisiteRepo coursePrerequisiteRepo;
    private final CourseProgrammeRepo courseProgrammeRepo;

    public List<CourseDto> getAllCoursesWithProgrammesAndPrereqs() {
        List<Course> allCourses = courseRepo.findAll();

        return allCourses.stream().map(course -> {
            CourseDto dto = new CourseDto();
            dto.setId(course.getId());
            dto.setCourseCode(course.getCourseCode());
            dto.setTitle(course.getTitle());
            dto.setDescription(course.getDescription());
            dto.setCreditPoints(course.getCreditPoints());
            dto.setLevel(course.getLevel());
            dto.setOfferedSem1(course.isOfferedSem1());
            dto.setOfferedSem2(course.isOfferedSem2());

            // Fetch programme codes using CourseProgrammeRepo
            List<String> programmeCodes = courseProgrammeRepo.findProgrammesByCourseId(course.getId()).stream()
                    .map(Programme::getProgrammeCode) // or .getName() if needed
                    .collect(Collectors.toList());
            dto.setProgrammes(programmeCodes);

            // Fetch prerequisite course codes using CoursePrerequisiteRepo
            List<String> prereqCodes = coursePrerequisiteRepo.findPrerequisitesByCourseId(course.getId()).stream()
                    .map(Course::getCourseCode)
                    .collect(Collectors.toList());
            dto.setPrerequisites(prereqCodes);

            return dto;
        }).collect(Collectors.toList());
    }
    // Create a new course
    public void saveCourse(String courseCode, String title, String description, double creditPoints, Short level, Boolean offeredSem1, Boolean offeredSem2) {
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
    public void updateCourse(String courseCode, String title, String description, double creditPoints, Short level, Boolean offeredSem1, Boolean offeredSem2) {
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

    public void addCourse(CourseDto courseDto) {
        // Create and save the main course
        Course course = new Course();
        course.setCourseCode(courseDto.getCourseCode());
        course.setTitle(courseDto.getTitle());
        course.setDescription(courseDto.getDescription());
        course.setCreditPoints(courseDto.getCreditPoints());
        course.setLevel(courseDto.getLevel());
        course.setOfferedSem1(courseDto.isOfferedSem1());
        course.setOfferedSem2(courseDto.isOfferedSem2());
        courseRepo.save(course); // Save the new course first

        // Process prerequisites
        List<String> courseReqs = courseDto.getPrerequisites(); // Get prerequisite course codes
        if (courseReqs == null || courseReqs.isEmpty()) {
            return; // No prerequisites, just return
        }

        // Find all prerequisite courses by their course codes
        List<Course> prerequisites = courseRepo.findByCourseCodeIn(courseReqs);

        // Create and save CoursePrerequisite entities
        for (Course prereq : prerequisites) {
            CoursePrerequisite coursePrerequisite = new CoursePrerequisite();
            coursePrerequisite.setCourse(course);       // The new course
            coursePrerequisite.setPrerequisite(prereq); // Each prerequisite course
            coursePrerequisiteRepo.save(coursePrerequisite);
        }
    }

    public void addPrerequisites(Long courseId, List<Long> prereqIds) {
        Course course = courseRepo.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        List<Course> prerequisites = courseRepo.findAllById(prereqIds);

        List<CoursePrerequisite> newPrereqs = prerequisites.stream()
                .map(prereq -> new CoursePrerequisite(null, course, prereq))
                .collect(Collectors.toList());

        coursePrerequisiteRepo.saveAll(newPrereqs);
    }


}
//    public void addPrerequisites(Long courseId, List<String> prerequisiteCodes) {
//        Course course = courseRepo.findById(courseId)
//                .orElseThrow(() -> new IllegalArgumentException("Course not found"));
//
//        List<Course> prerequisiteCourses = courseRepo.findByCourseCodeIn(prerequisiteCodes);
//        List<CoursePrerequisite> coursePrerequisites = new ArrayList<>();
//
//        for (Course prereq : prerequisiteCourses) {
//            CoursePrerequisite cp = new CoursePrerequisite();
//            cp.setCourse(course);
//            cp.setPrerequisite(prereq);
//            coursePrerequisites.add(cp);
//        }
//
//        coursePrerequisiteRepo.saveAll(coursePrerequisites);
//    }