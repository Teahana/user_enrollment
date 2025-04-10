package group7.enrollmentSystem.services;

import group7.enrollmentSystem.dtos.classDtos.CourseEnrollDto;
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
    public List<CourseEnrollment> getCompletedEnrollments(Long studentId) {
        return courseEnrollmentRepo.findByStudentIdAndCompletedTrue(studentId);
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


}


//    public List<CourseEnrollDto> getEligibleCoursesForEnrollment(Student student, int semester) {
//        // ---------------------------------------------------------
//        // 1) Identify the student's programme
//        // ---------------------------------------------------------
//        Optional<StudentProgramme> data = studentProgrammeRepo.findByStudentAndCurrentProgrammeTrue(student);
//        if (data.isEmpty()) {
//            throw new IllegalArgumentException("Cannot find student programme, or student has no active programme");
//        }
//        Programme studentProgramme = data.get().getProgramme();
//
//        // ---------------------------------------------------------
//        // 2) Fetch all candidate courses for that programme which are offered
//        // ---------------------------------------------------------
//        List<Course> programmeCourses = courseProgrammeRepo.findByProgramme(studentProgramme)
//                .stream()
//                .map(CourseProgramme::getCourse)
//                .distinct()
//                .collect(Collectors.toList());
//
//        List<Course> offeredThisSemester = programmeCourses.stream()
//                .filter(c -> isOfferedInSemester(c, semester))
//                .collect(Collectors.toList());
//
//        // ---------------------------------------------------------
//        // 3) Filter out courses student already completed or is currently taking
//        // ---------------------------------------------------------
//        Set<Long> ineligibleCourseIds = courseEnrollmentRepo
//                .findByStudentId(student.getId())
//                .stream()
//                .filter(ce -> ce.isCompleted() || ce.isCurrentlyTaking() || ce.isCancelled())
//                .map(ce -> ce.getCourse().getId())
//                .collect(Collectors.toSet());
//
//        List<Course> notYetEnrolledOrCompleted = offeredThisSemester.stream()
//                .filter(c -> !ineligibleCourseIds.contains(c.getId()))
//                .toList();
//
//
//        // ---------------------------------------------------------
//        // 4) Check if student has reached the 4-course limit
//        // ---------------------------------------------------------
//        int currentlyTakingInThisSemester = courseEnrollmentRepo
//                .countByStudentAndSemesterEnrolledAndCurrentlyTakingIsTrue(student, semester);
//
//        if (currentlyTakingInThisSemester >= 4) {
//            return Collections.emptyList();
//        }
//
//        // ---------------------------------------------------------
//        // 5) Filter by checking prerequisites (the *advanced* logic)
//        // ---------------------------------------------------------
//        List<Course> eligibleCourses = new ArrayList<>();
//        for (Course course : notYetEnrolledOrCompleted) {
//            if (meetsPrerequisites(student, studentProgramme, course)) {
//                eligibleCourses.add(course);
//            }
//        }
//
//        // (Optionally limit based on how many slots remain, e.g. 4 - currentlyTakingInThisSemester)
//
//        // ---------------------------------------------------------
//        // 6) Convert to DTOs
//        // ---------------------------------------------------------
//        return eligibleCourses.stream()
//                .map(c -> {
//                    CourseEnrollDto dto = new CourseEnrollDto();
//                    dto.setCourseId(c.getId());
//                    dto.setCourseTitle(c.getTitle());
//                    dto.setCourseCode(c.getCourseCode());
//                    dto.setDescription(c.getDescription());
//
//                    // Now fetch the prerequisites using the service
//                    List<String> codes = courseService.getPrerequisiteCodesByCourseId(c.getId());
//                    dto.setPrerequisiteCodes(codes);
//
//                    return dto;
//                })
//                .collect(Collectors.toList());
//    }

// Example helper
//    private boolean isOfferedInSemester(Course course, int semester) {
//        if (semester == 1) {
//            return course.isOfferedSem1();
//        } else if (semester == 2) {
//            return course.isOfferedSem2();
//        }
//        return false; // default if you only have 2 semesters
//    }
//    private boolean meetsPrerequisites(Student student,
//                                       Programme programme,
//                                       Course course)
//    {
//        // 1) Load all CoursePrerequisite rows for this course
//        List<CoursePrerequisite> all = coursePrerequisiteRepo.findByCourse(course);
//        if (all.isEmpty()) {
//            // No prerequisites => trivially true
//            return true;
//        }
//
//        // 2) Build an in-memory structure: groupId -> prerequisites
//        Map<Integer, List<CoursePrerequisite>> byGroupId = all.stream()
//                .collect(Collectors.groupingBy(CoursePrerequisite::getGroupId));
//
//        // Now you need to decide which groupId(s) are top-level.
//        // Often you look for rows with parentId == 0 or something similar.
//        // If your data always has exactly one "top-level" group with, say, groupId = 1,
//        // you can just do this:
//
//        // For a truly flexible approach, find all groupIds that have no "parent" referencing them:
//        //   Set<Integer> allGroupIds = byGroupId.keySet();
//        //   Set<Integer> childGroupIds = all.stream()
//        //                                   .map(CoursePrerequisite::getChildId)
//        //                                   .filter(cid -> cid != 0)
//        //                                   .collect(Collectors.toSet());
//        //   // top-level groups are allGroupIds - childGroupIds
//        //   ...
//
//        // But for a simple scenario, we might assume the top-level group is groupId=1:
//        int topLevelGroupId = 1;
//
//        // 3) Evaluate the top-level group
//        return evaluateGroup(student, programme, byGroupId, topLevelGroupId);
//    }

/**
 * Evaluate all prerequisites in a given group and combine them using AND or OR.
 * If the group is empty, we might treat that as "true" (no constraints).
 */
//    private boolean evaluateGroup(Student s,
//                                  Programme p,
//                                  Map<Integer, List<CoursePrerequisite>> byGroupId,
//                                  int groupId)
//    {
//        List<CoursePrerequisite> groupItems = byGroupId.getOrDefault(groupId, List.of());
//        if (groupItems.isEmpty()) {
//            // No items => no constraints => passes
//            return true;
//        }
//
//        // We have to decide how the group’s items are combined.
//        // If your schema says "each row has prerequisiteType=AND or OR,"
//        // you might pick the first row as “the group’s operator.”
//        // Or each row might have an operatorToNext you must interpret more carefully.
//
//        // For simplicity, assume "groupOperator" is found in the first row:
//        PrerequisiteType groupOperator = groupItems.get(0).getPrerequisiteType();
//        // (You need to confirm that all items in the group share that same "prerequisiteType"
//        // or that each row’s operatorToNext is consistent.)
//
//        // Evaluate each row in the group
//        List<Boolean> results = new ArrayList<>();
//        for (CoursePrerequisite cp : groupItems) {
//            boolean pass = evaluateSinglePrerequisite(s, p, cp, byGroupId);
//            results.add(pass);
//        }
//
//        // Combine them according to the operator (AND/OR).
//        if (groupOperator == PrerequisiteType.AND) {
//            return results.stream().allMatch(Boolean::booleanValue);
//        } else {
//            // Fallback => OR
//            return results.stream().anyMatch(Boolean::booleanValue);
//        }
//    }
//
//    /**
//     * Evaluate one CoursePrerequisite record:
//     * - Check if the student's programme matches if `programmeId` is set.
//     * - If special => handle the special logic.
//     * - Otherwise => check if the student completed the `prerequisite` course.
//     * - If `isParent` is true => evaluate the child group and combine the results
//     *   with `rowPass` using `operatorToNext`.
//     */
//    private boolean evaluateSinglePrerequisite(Student s,
//                                               Programme p,
//                                               CoursePrerequisite cp,
//                                               Map<Integer, List<CoursePrerequisite>> byGroupId)
//    {
//        // 1) Programme match (if cp.getProgramme() != null)
//        //    If it doesn't match, this row fails.
//        if (cp.getProgramme() != null
//                && !cp.getProgramme().getId().equals(p.getId()))
//        {
//            return false;
//        }
//
//        // 2) Evaluate base condition
//        boolean rowPass;
//        if (cp.isSpecial()) {
//            // e.g. ADMISSION_PROGRAMME or COMPLETION_LEVEL_PERCENT
//            rowPass = checkSpecialPrerequisite(s, p, cp);
//        } else {
//            // Normal course-based prerequisite
//            if (cp.getPrerequisite() == null) {
//                // Possibly "no actual course"? Then pass or fail as desired.
//                rowPass = true;
//            } else {
//                // Must have completed that specific prerequisite course
//                rowPass = courseEnrollmentRepo
//                        .existsByStudentAndCourseAndCompletedIsTrue(s, cp.getPrerequisite());
//            }
//        }
//
//        // 3) If this row is a parent, evaluate the child group and combine results
//        if (cp.isParent()) {
//            int childGroupId = cp.getChildId();
//            boolean childPass = evaluateGroup(s, p, byGroupId, childGroupId);
//
//            if (cp.getOperatorToNext() == PrerequisiteType.AND) {
//                return rowPass && childPass;
//            } else {
//                // OR scenario
//                return rowPass || childPass;
//            }
//        }
//
//        // If no child group, rowPass is final
//        return rowPass;
//    }

/**
 * Evaluate "special" prerequisites such as
 *  - ADMISSION_PROGRAMME
 *  - COMPLETION_LEVEL_PERCENT
 */
//    private boolean checkSpecialPrerequisite(Student s, Programme p, CoursePrerequisite cp) {
//        if (cp.getSpecialType() == SpecialPrerequisiteType.ADMISSION_PROGRAMME) {
//            // Must be in the programme specified by cp (if it’s not null)
//            // If you truly have multiple rows for "BNS" vs "BSE" in the same group,
//            // you'd typically rely on the OR operator in that group.
//            // So if the student fails this row but passes the next,
//            // they'd satisfy the group.
//            return (cp.getProgramme() != null
//                    && cp.getProgramme().getId().equals(p.getId()));
//        }
//        else if (cp.getSpecialType() == SpecialPrerequisiteType.COMPLETION_LEVEL_PERCENT) {
//            short targetLevel = cp.getTargetLevel();
//            double requiredPct = cp.getPercentageValue();
//            // 1) Find all courses in the student's programme at that level
//            List<Course> sameLevelCourses = courseProgrammeRepo
//                    .findByProgramme(p)
//                    .stream()
//                    .map(CourseProgramme::getCourse)
//                    .filter(c -> c.getLevel() == targetLevel)
//                    .collect(Collectors.toList());
//            if (sameLevelCourses.isEmpty()) {
//                // If no courses at that level => can't meet requirement
//                return false;
//            }
//            // 2) Count how many are completed
//            long completedCount = courseEnrollmentRepo
//                    .findByStudentAndCourseInAndCompletedIsTrue(s, sameLevelCourses)
//                    .size();
//
//            double pctCompleted = (completedCount * 100.0) / sameLevelCourses.size();
//            return (pctCompleted >= requiredPct);
//        }
//
//        // If no recognized specialType, or it's something else => either pass or fail by design
//        return true;
//    }