package group7.enrollmentSystem.services;

import group7.enrollmentSystem.models.Course;
import group7.enrollmentSystem.models.CourseProgramme;
import group7.enrollmentSystem.models.CourseEnrollment;
import group7.enrollmentSystem.models.Student;
import group7.enrollmentSystem.models.StudentProgramme;
import group7.enrollmentSystem.repos.CourseProgrammeRepo;
import group7.enrollmentSystem.repos.CourseRepo;
import group7.enrollmentSystem.repos.CourseEnrollmentRepo;
import group7.enrollmentSystem.repos.StudentProgrammeRepo;
import group7.enrollmentSystem.repos.StudentRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseEnrollmentService {

    private final CourseProgrammeRepo courseProgrammeRepo;
    private final StudentRepo studentRepo;
    private final CourseEnrollmentRepo courseEnrollmentRepo;
    private final StudentProgrammeRepo studentProgrammeRepo;
    private final CourseRepo courseRepo;

    // Cancel enrollment
    public void cancelEnrollment(Long enrollmentId) {
        CourseEnrollment enrollment = courseEnrollmentRepo.findById(enrollmentId)
                .orElseThrow(() -> new RuntimeException("Enrollment not found"));
        enrollment.setCurrentlyTaking(false);
        courseEnrollmentRepo.save(enrollment);
    }

    // Reactivate enrollment
    public void activateEnrollment(Long enrollmentId) {
        CourseEnrollment enrollment = courseEnrollmentRepo.findById(enrollmentId)
                .orElseThrow(() -> new RuntimeException("Enrollment not found"));
        enrollment.setCurrentlyTaking(true);
        courseEnrollmentRepo.save(enrollment);
    }

    // Fetch enrolled courses only for active enrollments
    public List<CourseEnrollment> getActiveEnrollments(Long studentId) {
        return courseEnrollmentRepo.findByStudentIdAndCurrentlyTakingTrue(studentId);
    }

    // Fetch inactive (canceled) enrollments
    public List<CourseEnrollment> getCanceledEnrollments(Long studentId) {
        return courseEnrollmentRepo.findByStudentIdAndCurrentlyTakingFalse(studentId);
    }

    // Get available Courses Based on Semester
    public List<CourseProgramme> getAvailableCoursesForSemester(Long studentId, int semester) {
        List<CourseEnrollment> activeEnrollments = getActiveEnrollments(studentId);

        List<Long> enrolledIds = activeEnrollments.stream()
                .map(e -> e.getCourse().getId()).collect(Collectors.toList());

        List<CourseProgramme> allCourses = courseProgrammeRepo.findAll();

        return allCourses.stream()
                .filter(cp -> (semester == 1 && cp.getCourse().isOfferedSem1()) ||
                        (semester == 2 && cp.getCourse().isOfferedSem2()))
                .filter(cp -> !enrolledIds.contains(cp.getCourse().getId()))
                .collect(Collectors.toList());
    }

    // Handles the enrollment
    public void enrollStudentInCourses(Long studentId, List<Long> courseIds, int semester) {
        Student student = studentRepo.findById(studentId).orElseThrow();
        List<Course> courses = courseRepo.findAllById(courseIds);

        courses.stream()
                .filter(c -> (semester == 1 && c.isOfferedSem1()) ||
                        (semester == 2 && c.isOfferedSem2()))
                .forEach(c -> {
                    CourseEnrollment enrollment = new CourseEnrollment();
                    enrollment.setStudent(student);
                    enrollment.setCourse(c);
                    enrollment.setCurrentlyTaking(true);
                    enrollment.setDateEnrolled(LocalDate.now());
                    courseEnrollmentRepo.save(enrollment);
                });
    }

    // Get available courses excluding courses already actively enrolled
    public List<Course> getAvailableCoursesForEnrollment(Long studentId) {
        Student student = studentRepo.findById(studentId).orElseThrow();
        // Find the student's current programme
        StudentProgramme studentProgramme = studentProgrammeRepo
                .findByStudentAndCurrentProgrammeTrue(student)
                .orElseThrow(() -> new RuntimeException("Student's current programme not found"));
        //Get all courses linked to this programme
        List<CourseProgramme> programmeCourses = courseProgrammeRepo
                .findByProgramme(studentProgramme.getProgramme());

        //Extract the list of courses
        List<Course> coursesInProgramme = programmeCourses.stream()
                .map(CourseProgramme::getCourse)
                .collect(Collectors.toList());
        //Get the courses the student is already enrolled in
        List<CourseEnrollment> enrolledCourses = courseEnrollmentRepo.findByStudent(student);
        List<Course> currentlyEnrolledCourses = enrolledCourses.stream()
                .map(CourseEnrollment::getCourse)
                .collect(Collectors.toList());
        //Filter out courses the student is already enrolled in
        return coursesInProgramme.stream()
                .filter(course -> !currentlyEnrolledCourses.contains(course))
                .collect(Collectors.toList());
    }

    public List<CourseEnrollment> getActiveEnrollmentsBySemester(Long studentId, int semester) {
        return getActiveEnrollments(studentId).stream()
                .filter(e -> semester == 1 ? e.getCourse().isOfferedSem1() : e.getCourse().isOfferedSem2())
                .collect(Collectors.toList());
    }

    public List<CourseEnrollment> getCanceledEnrollmentsBySemester(Long studentId, int semester) {
        return getCanceledEnrollments(studentId).stream()
                .filter(e -> semester == 1 ? e.getCourse().isOfferedSem1() : e.getCourse().isOfferedSem2())
                .collect(Collectors.toList());
    }

}
