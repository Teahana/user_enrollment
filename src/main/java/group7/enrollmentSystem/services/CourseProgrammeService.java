package group7.enrollmentSystem.services;

import group7.enrollmentSystem.models.Course;
import group7.enrollmentSystem.models.CourseProgramme;
import group7.enrollmentSystem.models.Programme;
import group7.enrollmentSystem.repos.CourseProgrammeRepo;
import group7.enrollmentSystem.repos.CourseRepo;
import group7.enrollmentSystem.repos.ProgrammeRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CourseProgrammeService {

    private final CourseProgrammeRepo courseProgrammeRepo;
    private final CourseRepo courseRepo;
    private final ProgrammeRepo programmeRepo;

    // Create a new CourseProgramme record
    public void saveCourseProgramme(String courseCode, String programmeCode) {
        Optional<Course> course = courseRepo.findByCourseCode(courseCode);
        Optional<Programme> programme = programmeRepo.findByProgrammeCode(programmeCode);

        if (course.isPresent() && programme.isPresent()) {
            CourseProgramme courseProgramme = new CourseProgramme();
            courseProgramme.setCourse(course.get());
            courseProgramme.setProgramme(programme.get());
            courseProgrammeRepo.save(courseProgramme);
        } else {
            throw new RuntimeException("Course or Programme nottt found");
        }
    }

    public List<CourseProgramme> getAllCourseProgrammes() {
        return courseProgrammeRepo.findAll();
    }

    public Optional<CourseProgramme> getCourseProgrammeById(Long id) {
        return courseProgrammeRepo.findById(id);
    }

    // Update a CourseProgramme record
    public void updateCourseProgramme(Long id, Long courseId, Long programmeId) {
        Optional<CourseProgramme> optionalCourseProgramme = courseProgrammeRepo.findById(id);
        Optional<Course> course = courseRepo.findById(courseId);
        Optional<Programme> programme = programmeRepo.findById(programmeId);

        if (optionalCourseProgramme.isPresent() && course.isPresent() && programme.isPresent()) {
            CourseProgramme courseProgramme = optionalCourseProgramme.get();
            courseProgramme.setCourse(course.get());
            courseProgramme.setProgramme(programme.get());
            courseProgrammeRepo.save(courseProgramme);
        } else {
            throw new RuntimeException("CourseProgramme, Course, or Programme not found");
        }
    }

    // Delete a CourseProgramme record
    public void deleteCourseProgramme(Long id) {
        courseProgrammeRepo.deleteById(id);
    }
}
