package group7.enrollmentSystem.services;

import group7.enrollmentSystem.dtos.classDtos.CoursePrerequisiteDto;
import group7.enrollmentSystem.enums.PrerequisiteType;
import group7.enrollmentSystem.models.Course;
import group7.enrollmentSystem.models.CourseProgramme;
import group7.enrollmentSystem.models.CourseEnrollment;
import group7.enrollmentSystem.models.Student;
import group7.enrollmentSystem.models.StudentProgramme;
import group7.enrollmentSystem.repos.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseEnrollmentService {

    private final CourseProgrammeRepo courseProgrammeRepo;
    private final StudentRepo studentRepo;
    private final CourseEnrollmentRepo courseEnrollmentRepo;
    private final StudentProgrammeRepo studentProgrammeRepo;
    private final CourseRepo courseRepo;
    private final CoursePrerequisiteRepo coursePrerequisiteRepo;

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
        return courseEnrollmentRepo.findByStudentIdAndCurrentlyTakingFalseAndCompletedFalse(studentId);
    }

    public List<CourseEnrollment> getCompletedEnrollments(Long studentId) {
        return courseEnrollmentRepo.findByStudentIdAndCompletedTrue(studentId);
    }

    // Get available Courses Based on Semester
    public List<CourseProgramme> getAvailableCoursesForSemester(Long studentId, int semester) {

        // Fetch the student's current programme
        Student student = studentRepo.findById(studentId).orElseThrow(() -> new RuntimeException("Student not found"));
        StudentProgramme studentProgramme = studentProgrammeRepo.findByStudentAndCurrentProgrammeTrue(student)
                .orElseThrow(() -> new RuntimeException("No active programme found for the student"));

        // Fetch active and completed enrollments
        List<CourseEnrollment> activeEnrollments = getActiveEnrollments(studentId);
        List<CourseEnrollment> completedEnrollments = courseEnrollmentRepo.findByStudentIdAndCompletedTrue(studentId);

        // Extract course IDs from active and completed enrollments
        List<Long> enrolledIds = activeEnrollments.stream()
                .map(e -> e.getCourse().getId()).toList();
        List<Long> completedIds = completedEnrollments.stream()
                .map(e -> e.getCourse().getId())
                .toList();

        // Fetch all courses linked to the student's current programme
        List<CourseProgramme> programmeCourses = courseProgrammeRepo.findByProgramme(studentProgramme.getProgramme());

        // Fetch available courses for the semester
        //List<CourseProgramme> courses = courseProgrammeRepo.findBySemester(semester);

        // Filter out courses the student is already enrolled in or has completed
        return programmeCourses.stream()
                .filter(cp -> (semester == 1 && cp.getCourse().isOfferedSem1()) ||
                        (semester == 2 && cp.getCourse().isOfferedSem2()))
                .filter(cp -> !enrolledIds.contains(cp.getCourse().getId()))
                .filter(cp -> !completedIds.contains(cp.getCourse().getId()))
                .collect(Collectors.toList());
    }

    // Handles the enrollment
    public void enrollStudentInCourses(Long studentId, List<Long> courseIds, int semester) {
        Student student = studentRepo.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));

        // Fetch all courses in a single query
        List<Course> courses = courseRepo.findAllById(courseIds);
        if (courses.size() != courseIds.size()) {
            throw new IllegalArgumentException("One or more courses not found");
        }

        // Create a map of courseId -> Course for quick lookup
        Map<Long, Course> courseMap = courses.stream()
                .collect(Collectors.toMap(Course::getId, Function.identity()));

        // Fetch all prerequisites for the selected courses in one query
        List<CoursePrerequisiteDto> prerequisites = coursePrerequisiteRepo.findPrerequisitesByCourseIds(courseIds);

        // Group prerequisites by courseId and then by groupId
        Map<Long, Map<Integer, List<CoursePrerequisiteDto>>> groupedPrereqs = prerequisites.stream()
                .collect(Collectors.groupingBy(
                        CoursePrerequisiteDto::getCourseId,
                        Collectors.groupingBy(CoursePrerequisiteDto::getGroupId)
                ));

        for (Long courseId : courseIds) {
            Course course = courseMap.get(courseId);
            if (course == null) {
                throw new IllegalArgumentException("Course not found with ID: " + courseId);
            }

            // Get prerequisites for the current course
            Map<Integer, List<CoursePrerequisiteDto>> coursePrerequisites = groupedPrereqs.get(courseId);

            // If there are no prerequisites, consider them satisfied
            boolean allPrerequisitesSatisfied = true;
            if (coursePrerequisites != null) {
                // Check if all prerequisite groups are satisfied
                allPrerequisitesSatisfied = coursePrerequisites.values().stream()
                        .allMatch(group -> evaluatePrerequisiteGroup(studentId, group));
            }

            if (!allPrerequisitesSatisfied) {
                throw new IllegalArgumentException("Cannot enroll - Prerequisites not completed for course: " + course.getCourseCode());
            }

            // If prerequisites are satisfied and the course is offered in the selected semester, enroll the student
            if ((semester == 1 && course.isOfferedSem1()) || (semester == 2 && course.isOfferedSem2())) {
                CourseEnrollment enrollment = new CourseEnrollment();
                enrollment.setStudent(student);
                enrollment.setCourse(course);
                enrollment.setCurrentlyTaking(true);
                enrollment.setDateEnrolled(LocalDate.now());
                enrollment.setSemesterEnrolled(semester);
                courseEnrollmentRepo.save(enrollment);
            }
        }
    }

    private boolean evaluatePrerequisiteGroup(Long studentId, List<CoursePrerequisiteDto> group) {
        if (group.isEmpty()) {
            return true; // No prerequisites in this group
        }

        PrerequisiteType groupType = group.getFirst().getPrerequisiteType();

        if (groupType == PrerequisiteType.AND) {
            // For AND, all prerequisites in the group must be completed
            return group.stream().allMatch(prereq ->
                    courseEnrollmentRepo.existsByStudentIdAndCourseIdAndCompletedTrue(studentId, prereq.getPrerequisiteId()));
        } else if (groupType == PrerequisiteType.OR) {
            // For OR, at least one prerequisite in the group must be completed
            return group.stream().anyMatch(prereq ->
                    courseEnrollmentRepo.existsByStudentIdAndCourseIdAndCompletedTrue(studentId, prereq.getPrerequisiteId()));
        } else {
            throw new IllegalArgumentException("Invalid prerequisite type: " + groupType);
        }
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