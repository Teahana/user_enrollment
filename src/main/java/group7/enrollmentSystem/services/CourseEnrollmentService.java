package group7.enrollmentSystem.services;

import group7.enrollmentSystem.dtos.classDtos.CourseEnrollDto;
import group7.enrollmentSystem.dtos.classDtos.CourseEnrollmentDto;
import group7.enrollmentSystem.dtos.classDtos.CoursePrerequisiteDto;
import group7.enrollmentSystem.dtos.classDtos.EnrollmentPageData;
import group7.enrollmentSystem.enums.PrerequisiteType;
import group7.enrollmentSystem.enums.SpecialPrerequisiteType;
import group7.enrollmentSystem.models.*;
import group7.enrollmentSystem.repos.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
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
    private final CourseService courseService;
    private final EnrollmentStateRepo enrollmentStateRepo;

    // Cancel enrollment
    public void cancelEnrollment(Long enrollmentId) {
        CourseEnrollment enrollment = courseEnrollmentRepo.findById(enrollmentId)
                .orElseThrow(() -> new RuntimeException("Enrollment not found"));
        enrollment.setCurrentlyTaking(false);
        enrollment.setCancelled(true); // Set cancelled to true
        courseEnrollmentRepo.save(enrollment);
    }


    // Reactivate enrollment
    public void activateEnrollment(Long enrollmentId) {
        CourseEnrollment enrollment = courseEnrollmentRepo.findById(enrollmentId)
                .orElseThrow(() -> new RuntimeException("Enrollment not found"));
        enrollment.setCurrentlyTaking(true);
        enrollment.setCancelled(false); // Set cancelled to false
        courseEnrollmentRepo.save(enrollment);
    }

    // Fetch enrolled courses only for active enrollments
    public List<CourseEnrollment> getActiveEnrollments(Long studentId) {
        return courseEnrollmentRepo.findByStudentIdAndCurrentlyTakingTrue(studentId);
    }
    //Semester-based
    public List<CourseEnrollment> getActiveEnrollmentsBySemester(Long studentId, int semester) {
        return courseEnrollmentRepo.findByStudentIdAndCurrentlyTakingTrueAndSemesterEnrolled(studentId, semester);
    }


    // Fetch inactive (canceled) enrollments
    public List<CourseEnrollment> getCanceledEnrollments(Long studentId) {
        return courseEnrollmentRepo.findByStudentIdAndCurrentlyTakingFalseAndCancelledTrue(studentId);
    }
    //Semester-based
    public List<CourseEnrollment> getCanceledEnrollmentsBySemester(Long studentId, int semester) {
        return courseEnrollmentRepo.findByStudentIdAndCurrentlyTakingFalseAndCancelledTrueAndSemesterEnrolled(studentId, semester);
    }

    // Fetch completed enrollments
    public List<CourseEnrollment> getCompletedEnrollmentsWithHighestGrade(Long studentId) {
        return courseEnrollmentRepo.findByStudentIdAndCompletedTrue(studentId).stream()
                .collect(Collectors.groupingBy(enrollment -> enrollment.getCourse().getId()))
                .values().stream()
                .map(enrollments -> enrollments.stream()
                        .max(Comparator.comparing(CourseEnrollment::getMark))
                        .orElseThrow(() -> new RuntimeException("No completed enrollment found")))
                .toList();
    }

    // Get available Courses Based on Semester
    public List<CourseProgramme> getAvailableCoursesForSemester(Long studentId, int semester) {

        // Fetch the student's current programme
        Student student = studentRepo.findById(studentId).orElseThrow(() -> new RuntimeException("Student not found"));
        StudentProgramme studentProgramme = studentProgrammeRepo.findByStudentAndCurrentProgrammeTrue(student)
                .orElseThrow(() -> new RuntimeException("No active programme found for the student"));

        // Fetch active, cancelled and completed enrollments
        List<CourseEnrollment> activeEnrollments = getActiveEnrollments(studentId);
        List<CourseEnrollment> cancelledEnrollments = getCanceledEnrollments(studentId);
        List<CourseEnrollment> completedEnrollments = courseEnrollmentRepo.findByStudentIdAndCompletedTrue(studentId);

        // Extract course IDs from active, cancelled and completed enrollments
        List<Long> enrolledIds = activeEnrollments.stream()
                .map(e -> e.getCourse().getId()).toList();

        List<Long> cancelledIds = cancelledEnrollments.stream()
                .map(e -> e.getCourse().getId()).toList();

        List<Long> completedIds = completedEnrollments.stream()
                .map(e -> e.getCourse().getId())
                .toList();

        // Fetch all courses linked to the student's current programme
        List<CourseProgramme> programmeCourses = courseProgrammeRepo.findByProgramme(studentProgramme.getProgramme());

        // Fetch available courses for the semester
        //List<CourseProgramme> courses = courseProgrammeRepo.findBySemester(semester);

        // Filter out courses the student is already enrolled in or has completed or has cancelled
        return programmeCourses.stream()
                .filter(cp -> (semester == 1 && cp.getCourse().isOfferedSem1()) ||
                        (semester == 2 && cp.getCourse().isOfferedSem2()))
                .filter(cp -> !enrolledIds.contains(cp.getCourse().getId()))
                .filter(cp -> !completedIds.contains(cp.getCourse().getId()))
                .filter(cp -> !cancelledIds.contains(cp.getCourse().getId())) // Exclude cancelled courses
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

    public EnrollmentPageData getEnrollmentPageData(Student student, Programme programme, int currentSemester) {
        Long studentId = student.getId();

        // Fetch all course enrollments for this student
        List<CourseEnrollment> allEnrollments = courseEnrollmentRepo.findByStudentId(studentId);

        // Active enrollments for the current semester
        List<CourseEnrollment> active = allEnrollments.stream()
                .filter(e -> e.isCurrentlyTaking() && e.getSemesterEnrolled() == currentSemester)
                .toList();

        // Cancelled enrollments for the current semester
        List<CourseEnrollment> cancelled = allEnrollments.stream()
                .filter(e -> e.isCancelled() && e.getSemesterEnrolled() == currentSemester)
                .toList();

        // Set of ineligible course IDs: already completed, currently taking, or cancelled
        Set<Long> ineligibleCourseIds = allEnrollments.stream()
                .filter(e -> e.isCompleted() || e.isCurrentlyTaking() || e.isCancelled())
                .map(e -> e.getCourse().getId())
                .collect(Collectors.toSet());

        // All courses from student's programme offered in current semester
        List<CourseProgramme> programmeCourses = courseProgrammeRepo.findByProgramme(programme);
        List<Course> eligible = programmeCourses.stream()
                .map(CourseProgramme::getCourse)
                .filter(course -> (currentSemester == 1 && course.isOfferedSem1()) ||
                        (currentSemester == 2 && course.isOfferedSem2()))
                .filter(course -> !ineligibleCourseIds.contains(course.getId()))
                .toList();

        return new EnrollmentPageData(active, cancelled, eligible);
    }


    //For testing purposes
    @Transactional
    public void passStudentByEmailAndYear(String email, short level) {
        Student student = studentRepo.findByEmail(email).orElseThrow(() ->
                new IllegalArgumentException("Student not found with email: " + email));

        Programme programme = studentProgrammeRepo.findStudentCurrentProgramme(student)
                .orElseThrow(() -> new IllegalStateException("Student has no current programme"));


        List<Course> programmeCourses = courseProgrammeRepo.getCoursesByProgramme(programme);
        List<Course> coursesForLevel = programmeCourses.stream()
                .filter(course -> course.getLevel() != null && course.getLevel() == level)
                .toList();
        EnrollmentState state = enrollmentStateRepo.findById(1L).orElseThrow(() -> new RuntimeException("Enrollment state not found"));
        for (Course course : coursesForLevel) {
            CourseEnrollment enrollment = new CourseEnrollment();
            enrollment.setStudent(student);
            enrollment.setCourse(course);
            enrollment.setCompleted(true);
            enrollment.setFailed(false);
            enrollment.setCancelled(false);
            enrollment.setCurrentlyTaking(false);
            enrollment.setDateEnrolled(LocalDate.now());
            enrollment.setSemesterEnrolled(state.isSemesterOne() ? 1 : 2);
            enrollment.setProgramme(programme);
            courseEnrollmentRepo.save(enrollment);
        }
    }

    public List<CourseEnrollment> getAllGradeChangeRequests() {
        return courseEnrollmentRepo.findByRequestGradeChangeTrue();
    }
}
