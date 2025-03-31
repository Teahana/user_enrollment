package group7.enrollmentSystem.services;

import group7.enrollmentSystem.dtos.classDtos.CourseEnrollmentDto;
import group7.enrollmentSystem.enums.PrerequisiteType;
import group7.enrollmentSystem.enums.SpecialPrerequisiteType;
import group7.enrollmentSystem.models.*;
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
    private final EnrollmentStateRepo enrollmentStateRepo;

    public List<CourseEnrollmentDto> getEligibleCourses(String email) {
        Student student = studentRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Student not found with email: " + email));

        Programme programme = studentProgrammeRepo.findStudentCurrentProgramme(student)
                .orElseThrow(() -> new RuntimeException("Programme not found for student with email: " + email));

        EnrollmentState enrollmentState = enrollmentStateRepo.findById(1L).orElseThrow();
        boolean isSemesterOne = enrollmentState.isSemesterOne();

        List<Long> courseIdsForSem = isSemesterOne
                ? courseProgrammeRepo.getCourseIdsByProgrammeAndSemester1(programme)
                : courseProgrammeRepo.getCourseIdsByProgrammeAndSemester2(programme);
        List<Long> courseIdsForProgramme = courseProgrammeRepo.getCourseIdsByProgramme(programme);

        List<Long> completedCourseIds = courseEnrollmentRepo.getCompletedCourseIdsByStudent(student);
        List<Long> appliedCourseIds = courseEnrollmentRepo.getAppliedCourseIdsByStudent(student);

        List<CourseEnrollmentDto> eligibleDtos = new ArrayList<>();

        for (Long courseId : courseIdsForSem) {
            if (appliedCourseIds.contains(courseId)) continue;
            if (completedCourseIds.contains(courseId)) continue;

            if (isEligibleForCourse(courseId, completedCourseIds, programme, courseIdsForProgramme)) {
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

    private boolean isEligibleForCourse(Long courseId, List<Long> completedCourseIds, Programme studentProgramme, List<Long> courseIdsForProgramme) {
        List<CoursePrerequisite> cps = coursePrerequisiteRepo.findByCourseId(courseId);
        if (cps.isEmpty()) return true;

        Set<Integer> parentGroupIds = cps.stream()
                .filter(cp -> cp.isParent() && !cp.isChild())
                .map(CoursePrerequisite::getGroupId)
                .collect(Collectors.toSet());

        Map<Integer, Boolean> groupCache = new HashMap<>();
        List<Boolean> parentGroupResults = new ArrayList<>();

        for (Integer groupId : parentGroupIds) {
            boolean groupValid = evaluateGroup(groupId, cps, completedCourseIds, groupCache, studentProgramme, courseIdsForProgramme);
            parentGroupResults.add(groupValid);
        }

        return combineWithOperatorToNext(parentGroupIds, cps, parentGroupResults);
    }

    private boolean evaluateGroup(
            int groupId,
            List<CoursePrerequisite> allPrereqs,
            List<Long> completedCourseIds,
            Map<Integer, Boolean> groupCache,
            Programme studentProgramme,
            List<Long> courseIdsForProgramme
    ) {
        if (groupCache.containsKey(groupId)) {
            return groupCache.get(groupId);
        }

        List<CoursePrerequisite> groupEntries = allPrereqs.stream()
                .filter(cp -> cp.getGroupId() == groupId)
                .toList();

        if (groupEntries.isEmpty()) return true;

        PrerequisiteType type = groupEntries.getFirst().getPrerequisiteType();
        List<Boolean> conditions = new ArrayList<>();

        for (CoursePrerequisite cp : groupEntries) {
            // ✅ Handle special prerequisites
            if (cp.isSpecial()) {
                if (cp.getSpecialType() == SpecialPrerequisiteType.ADMISSION_PROGRAMME) {
                    if (cp.getProgramme() != null && cp.getProgramme().getId().equals(studentProgramme.getId())) {
                        conditions.add(true); // student is admitted to required programme
                    } else {
                        conditions.add(false); // not admitted to this programme
                    }
                } else if (cp.getSpecialType() == SpecialPrerequisiteType.COMPLETION_LEVEL_PERCENT) {
                    int level = cp.getTargetLevel();
                    double requiredPercent = cp.getPercentageValue();

                    List<Long> levelCourseIds = courseIdsForProgramme.stream()
                            .map(id -> courseRepo.findById(id).orElse(null))
                            .filter(Objects::nonNull)
                            .filter(c -> c.getLevel() == level)
                            .map(Course::getId)
                            .toList();

                    long total = levelCourseIds.size();
                    long completed = levelCourseIds.stream().filter(completedCourseIds::contains).count();

                    boolean met = total > 0 && ((double) completed / total) >= requiredPercent;
                    conditions.add(met);
                }

                continue; // skip normal logic if special handled
            }

            // Skip if this prerequisite is program-specific and doesn't match
            if (cp.getProgramme() != null && !cp.getProgramme().getId().equals(studentProgramme.getId())) {
                continue;
            }

            if (cp.getPrerequisite() != null) {
                boolean passed = completedCourseIds.contains(cp.getPrerequisite().getId());
                conditions.add(passed);
            }

            if (cp.getChildId() != 0) {
                boolean childValid = groupCache.containsKey(cp.getChildId())
                        ? groupCache.get(cp.getChildId())
                        : evaluateGroup(cp.getChildId(), allPrereqs, completedCourseIds, groupCache, studentProgramme, courseIdsForProgramme);
                groupCache.putIfAbsent(cp.getChildId(), childValid);
                conditions.add(childValid);
            }
        }

        if (conditions.isEmpty()) {
            groupCache.put(groupId, true);
            return true;
        }

        boolean finalResult = type == PrerequisiteType.AND
                ? conditions.stream().allMatch(Boolean::booleanValue)
                : conditions.stream().anyMatch(Boolean::booleanValue);

        groupCache.put(groupId, finalResult);
        return finalResult;
    }

    private boolean combineWithOperatorToNext(Set<Integer> groupIds, List<CoursePrerequisite> allCps, List<Boolean> groupResults) {
        List<Integer> sortedGroupIds = new ArrayList<>(groupIds);
        Collections.sort(sortedGroupIds);

        if (groupResults.isEmpty()) return false;

        boolean result = groupResults.get(0);

        for (int i = 1; i < groupResults.size(); i++) {
            int finalI = i;

            PrerequisiteType operator = allCps.stream()
                    .filter(cp -> cp.getGroupId() == sortedGroupIds.get(finalI - 1) && cp.isParent())
                    .findFirst()
                    .map(CoursePrerequisite::getOperatorToNext)
                    .orElse(PrerequisiteType.AND);

            boolean nextResult = groupResults.get(i);
            result = switch (operator) {
                case AND -> result && nextResult;
                case OR -> result || nextResult;
            };
        }

        return result;
    }
}

