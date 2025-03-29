package group7.enrollmentSystem.services;

import group7.enrollmentSystem.dtos.classDtos.CourseEnrollmentDto;
import group7.enrollmentSystem.enums.PrerequisiteType;
import group7.enrollmentSystem.models.Course;
import group7.enrollmentSystem.models.CoursePrerequisite;
import group7.enrollmentSystem.models.Programme;
import group7.enrollmentSystem.models.Student;
import group7.enrollmentSystem.repos.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentService {
    private final CourseRepo courseRepo;
    private final StudentRepo studentRepo;
    private final CoursePrerequisiteRepo coursePrerequisiteRepo;
    private final CourseEnrollmentRepo courseEnrollmentRepo;
    private final StudentProgrammeRepo studentProgrammeRepo;
    private final CourseProgrammeRepo courseProgrammeRepo;

    /**
     * Returns a list of eligible courses a student can enroll in based on prerequisites.
     * @param email The student's email.
     * @return List of CourseEnrollmentDto representing eligible courses.
     */
    public List<CourseEnrollmentDto> getEligibleCourses(String email) {
        // Fetch student by email
        Student student = studentRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Student not found with email: " + email));

        // Fetch the student's current programme
        Programme programme = studentProgrammeRepo.findStudentCurrentProgramme(student)
                .orElseThrow(() -> new RuntimeException("Programme not found for student with email: " + email));

        // Fetch all course IDs under this programme
        List<Long> courseIds = courseProgrammeRepo.getCourseIdsByProgramme(programme);

        // Get list of course IDs the student has already completed
        List<Long> completedCourseIds = courseEnrollmentRepo.getCompletedCourseIdsByStudent(student);

        // Get list of course IDs the student has already applied for (to avoid showing again)
        List<Long> appliedCourseIds = courseEnrollmentRepo.getAppliedCourseIdsByStudent(student);

        List<CourseEnrollmentDto> eligibleDtos = new ArrayList<>();

        // Check eligibility for each course
        for (Long courseId : courseIds) {
            if (appliedCourseIds.contains(courseId)) continue; // Skip already applied courses
            if(completedCourseIds.contains(courseId)) continue; // Skip already completed courses
            if (isEligibleForCourse(courseId, completedCourseIds)) {
                // If eligible, fetch course details and add to result list
                Course course = courseRepo.findById(courseId).orElseThrow();
                eligibleDtos.add(new CourseEnrollmentDto(
                        course.getId(),
                        course.getCourseCode(),
                        course.getTitle(),
                        course.getCost()
                ));
            }
        }

        return eligibleDtos;
    }

    /**
     * Checks if a student is eligible for a specific course based on its prerequisites.
     */
    private boolean isEligibleForCourse(Long courseId, List<Long> completedCourseIds) {
        List<CoursePrerequisite> cps = coursePrerequisiteRepo.findByCourseId(courseId);

        // If no prerequisites, course is doesnt have a preReq and is eligible
        if (cps.isEmpty()) return true;

        // Find all top-level parent group IDs (i.e., groups that are not children of other groups)
        // Put it into a SET because data is flattened in backend/db so each parentGroup will be repeated with same groupIds.
        Set<Integer> parentGroupIds = cps.stream()
                .filter(cp -> cp.isParent() && !cp.isChild())
                .map(CoursePrerequisite::getGroupId)
                .collect(Collectors.toSet());

        // Cache to avoid recomputing the same group multiple times (e.g., same child used in multiple places)
        Map<Integer, Boolean> groupCache = new HashMap<>();

        // Evaluate all top-level parent groups
        List<Boolean> parentGroupResults = new ArrayList<>();
        for (Integer groupId : parentGroupIds) {
            boolean groupValid = evaluateGroup(groupId, cps, completedCourseIds, groupCache);
            parentGroupResults.add(groupValid);
        }

        // Combine the results of top-level groups using their operatorToNext field (AND/OR)
        return combineWithOperatorToNext(parentGroupIds, cps, parentGroupResults);
    }

    /**
     * Recursively evaluates a group (including nested subgroups) based on AND/OR logic.
     * Uses a cache to prevent redundant subgroup evaluation.
     */
    private boolean evaluateGroup(int groupId, List<CoursePrerequisite> allPrereqs, List<Long> completedCourseIds, Map<Integer, Boolean> groupCache) {
        // Return cached result if this group was already evaluated
        if (groupCache.containsKey(groupId)) {
            return groupCache.get(groupId);
        }

        List<CoursePrerequisite> groupEntries = allPrereqs.stream()
                .filter(cp -> cp.getGroupId() == groupId)
                .toList();

        if (groupEntries.isEmpty()) return true;

        PrerequisiteType type = groupEntries.getFirst().getPrerequisiteType(); // AND/OR for the group
        List<Boolean> conditions = new ArrayList<>();

        for (CoursePrerequisite cp : groupEntries) {
            if (cp.isSpecial()) {
                continue; // Skip special logic for now
            }

            // Check course prerequisite if present
            if (cp.getPrerequisite() != null) {
                boolean passed = completedCourseIds.contains(cp.getPrerequisite().getId());
                conditions.add(passed);
            }

            // Evaluate subgroup if present
            if (cp.getChildId() != 0) {
                // Use cached result if available
                boolean childValid;
                if (groupCache.containsKey(cp.getChildId())) {
                    childValid = groupCache.get(cp.getChildId());
                } else {
                    childValid = evaluateGroup(cp.getChildId(), allPrereqs, completedCourseIds, groupCache);
                    groupCache.put(cp.getChildId(), childValid); // Cache result
                }
                conditions.add(childValid);
            }
        }
        // If no valid conditions collected SOMEHOW!?!? idk just good to check IG
        if (conditions.isEmpty()) {
            groupCache.put(groupId, false);
            return false;
        }

        // Combine the results based on group logic (AND/OR)
        boolean finalResult = type == PrerequisiteType.AND
                ? conditions.stream().allMatch(Boolean::booleanValue)
                : conditions.stream().anyMatch(Boolean::booleanValue);

        // Cache and return
        groupCache.put(groupId, finalResult);
        return finalResult;
    }



    /**
     * Combines results from multiple parent groups using their `operatorToNext` field.
     * Only applies between top-level parent groups (not subgroups).
     */
    private boolean combineWithOperatorToNext(Set<Integer> groupIds, List<CoursePrerequisite> allCps, List<Boolean> groupResults) {
        List<Integer> sortedGroupIds = new ArrayList<>(groupIds);
        Collections.sort(sortedGroupIds); // Ensure consistent processing order

        if (groupResults.isEmpty()) return false;

        boolean result = groupResults.get(0);

        for (int i = 1; i < groupResults.size(); i++) {
            int finalI = i;

            // Get the operatorToNext from the previous group
            PrerequisiteType operator = allCps.stream()
                    .filter(cp -> cp.getGroupId() == sortedGroupIds.get(finalI - 1) && cp.isParent())
                    .findFirst()
                    .map(CoursePrerequisite::getOperatorToNext)
                    .orElse(PrerequisiteType.AND); // Default fallback

            boolean nextResult = groupResults.get(i);

            // Combine based on AND/OR between group results
            result = switch (operator) {
                case AND -> result && nextResult;
                case OR -> result || nextResult;
            };
        }

        return result;
    }
}




