package group7.enrollmentSystem.services;

import group7.enrollmentSystem.dtos.classDtos.*;
import group7.enrollmentSystem.enums.PrerequisiteType;
import group7.enrollmentSystem.models.Course;
import group7.enrollmentSystem.models.CoursePrerequisite;
import group7.enrollmentSystem.models.Programme;
import group7.enrollmentSystem.repos.CoursePrerequisiteRepo;
import group7.enrollmentSystem.repos.CourseProgrammeRepo;
import group7.enrollmentSystem.repos.CourseRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
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

            // Fetch Programme Codes
            List<String> programmeCodes = courseProgrammeRepo.findProgrammesByCourseId(course.getId()).stream()
                    .map(Programme::getProgrammeCode)
                    .collect(Collectors.toList());
            dto.setProgrammes(programmeCodes);

            // Fetch Prerequisites
            List<CoursePrerequisite> prerequisites = coursePrerequisiteRepo.findByCourseId(course.getId());
            if (prerequisites.isEmpty()) {
                dto.setPrerequisites(null);
                dto.setHasPreReqs(false);
                return dto;
            }

            // Group prerequisites by groupId
            Map<Integer, List<CoursePrerequisite>> groupedPrereqs = prerequisites.stream()
                    .collect(Collectors.groupingBy(CoursePrerequisite::getGroupId));

            // Identify parent groups
            List<Integer> parentGroups = prerequisites.stream()
                    .filter(cp -> cp.isParent() && !cp.isChild())
                    .sorted(Comparator.comparingInt(CoursePrerequisite::getGroupId))
                    .map(CoursePrerequisite::getGroupId)
                    .distinct()
                    .collect(Collectors.toList());

            // Map parent groups to their subgroups
            // Instead of a List, use a Set
            Map<Integer, Set<Integer>> parentToChildGroupMap = new HashMap<>();
            for (CoursePrerequisite cp : prerequisites) {
                if (cp.isChild()) {
                    parentToChildGroupMap
                            .computeIfAbsent(cp.getParentId(), k -> new HashSet<>())
                            .add(cp.getGroupId());
                }
            }


            // Build prerequisite expression using StringBuilder
            StringBuilder prerequisiteExpression = new StringBuilder();
            for (int i = 0; i < parentGroups.size(); i++) {
                int parentGroupId = parentGroups.get(i);
                String parentGroupExpression = buildGroupExpression(parentGroupId, groupedPrereqs, parentToChildGroupMap);

                prerequisiteExpression.append(parentGroupExpression);

                // Append operatorToNext AFTER the current group, if it's not the last one
                if (i < parentGroups.size() - 1) {
                    PrerequisiteType operatorToNext = groupedPrereqs.get(parentGroupId).get(0).getOperatorToNext();
                    if (operatorToNext != null) {
                        prerequisiteExpression.append(" ").append(operatorToNext).append(" ");
                    }
                }
            }


            // Store the final formatted prerequisite string
            dto.setPrerequisites(prerequisiteExpression.toString());
            dto.setHasPreReqs(true);

            return dto;
        }).collect(Collectors.toList());
    }

    /**
     * Recursively builds the prerequisite expression for a given group.
     * - Uses `prerequisiteType` (AND/OR) within a **single parent group**.
     * - Ensures **subgroups inherit the parent group’s prerequisiteType**.
     * - Does NOT use `operatorToNext` between subgroups.
     */
    private String buildGroupExpression(
            int groupId,
            Map<Integer, List<CoursePrerequisite>> groupedPrereqs,
            Map<Integer, Set<Integer>> parentToChildGroupMap) {

        List<CoursePrerequisite> group = groupedPrereqs.get(groupId);
        if (group == null || group.isEmpty()) return "";

        StringBuilder groupExpression = new StringBuilder();
        PrerequisiteType groupType = group.get(0).getPrerequisiteType(); // AND / OR within the group

        // Build expression for courses in the current group
        for (int i = 0; i < group.size(); i++) {
            CoursePrerequisite cp = group.get(i);
            String courseCode = cp.getPrerequisite().getCourseCode();

            if (i > 0) {
                groupExpression.append(" ").append(groupType).append(" ");
            }
            groupExpression.append(courseCode);
        }

        // If the group has subgroups, append them using the **same** groupType (prerequisiteType)
        if (parentToChildGroupMap.containsKey(groupId)) {
            for (int childGroupId : parentToChildGroupMap.get(groupId)) {
                String childExpression = buildGroupExpression(childGroupId, groupedPrereqs, parentToChildGroupMap);
                if (!childExpression.isEmpty()) {
                    groupExpression.append(" ")
                            .append(groupType)
                            .append(" ")
                            .append(childExpression);
                }
            }
        }
        // Wrap in parentheses if multiple elements exist
        if (group.size() > 1 || parentToChildGroupMap.containsKey(groupId)) {
            return "(" + groupExpression.toString() + ")";
        }
        return groupExpression.toString();
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
    }
    @Transactional
    public void addPrerequisites(FlatCoursePrerequisiteRequest request) {
        Course mainCourse = courseRepo.findById(request.getCourseId())
                .orElseThrow(() -> new IllegalArgumentException("Course not found with ID: " + request.getCourseId()));

        if (!coursePrerequisiteRepo.findByCourse(mainCourse).isEmpty()) {
            throw new IllegalStateException("Course already has prerequisites. Use update instead.");
        }
        List<FlatCoursePrerequisiteDTO> prerequisites = request.getPrerequisites();

        // ✅ Ensure prerequisites are not empty
        if (prerequisites.isEmpty()) {
            throw new IllegalArgumentException("No prerequisites provided.");
        }
        // ✅ Validate the prerequisite groups and relationships
        validatePrerequisiteGroups(prerequisites);

        List<CoursePrerequisite> prerequisitesToSave = new ArrayList<>();

        for (FlatCoursePrerequisiteDTO dto : prerequisites) {
            // ✅ Prevent self-referencing prerequisites
            if (dto.getCourseId().equals(dto.getPrerequisiteId())) {
                throw new IllegalArgumentException("A course cannot be a prerequisite to itself.");
            }

            // ✅ Ensure prerequisite course exists
            Course prerequisiteCourse = courseRepo.findById(dto.getPrerequisiteId())
                    .orElseThrow(() -> new IllegalArgumentException("Prerequisite course not found with ID: " + dto.getPrerequisiteId()));

            CoursePrerequisite prerequisite = new CoursePrerequisite();
            prerequisite.setCourse(mainCourse);
            prerequisite.setPrerequisite(prerequisiteCourse);
            prerequisite.setPrerequisiteType(dto.getPrerequisiteType());
            prerequisite.setOperatorToNext(dto.getOperatorToNext());
            prerequisite.setGroupId(dto.getGroupId());
            prerequisite.setParent(dto.isParent());
            prerequisite.setChild(dto.isChild());
            prerequisite.setChildId(dto.getChildId());

            // ✅ Validate child-parent relationships
            if (dto.isChild()) {
                System.out.println("1 child found");
                System.out.println("childs id: " + dto.getGroupId());
                System.out.println("childs parent: " + dto.getParentId());
                if (dto.getParentId() == 0 || dto.getParentId() == dto.getGroupId()) {
                    throw new IllegalArgumentException("Invalid parentId for child group: " + dto.getGroupId());
                }
                prerequisite.setParentId(dto.getParentId());
            }

            prerequisitesToSave.add(prerequisite);
        }

        // ✅ Save all valid prerequisites
        coursePrerequisiteRepo.saveAll(prerequisitesToSave);
    }
    @Transactional
    public void updatePrerequisites(FlatCoursePrerequisiteRequest request) {
        Course mainCourse = courseRepo.findById(request.getCourseId())
                .orElseThrow(() -> new IllegalArgumentException("Course not found with ID: " + request.getCourseId()));
        coursePrerequisiteRepo.deleteByCourse(mainCourse);
        addPrerequisites(request);
    }
    private void validatePrerequisiteGroups(List<FlatCoursePrerequisiteDTO> prerequisites) {
        // Count how many course entries exist per groupId
        Map<Integer, Long> courseCountPerGroup = prerequisites.stream()
                .collect(Collectors.groupingBy(
                        FlatCoursePrerequisiteDTO::getGroupId,
                        Collectors.counting()
                ));

        // Get all unique groupIds involved (could also collect from DTOs directly)
        Set<Integer> allGroupIds = prerequisites.stream()
                .map(FlatCoursePrerequisiteDTO::getGroupId)
                .collect(Collectors.toSet());

        for (Integer groupId : allGroupIds) {
            if (!courseCountPerGroup.containsKey(groupId)) {
                throw new IllegalArgumentException("Group " + groupId + " has no courses.");
            }
        }
    }
    public FlatCoursePrerequisiteRequest getPrerequisitesForCourse(Long courseId) {
        List<CoursePrerequisite> entities = coursePrerequisiteRepo.findByCourseId(courseId);
        System.out.println("entities count: " + entities.size());

        List<FlatCoursePrerequisiteDTO> dtos = entities.stream().map(cp -> {
            FlatCoursePrerequisiteDTO dto = new FlatCoursePrerequisiteDTO();
            dto.setCourseId(cp.getCourse().getId());
            dto.setPrerequisiteId(cp.getPrerequisite().getId());
            dto.setGroupId(cp.getGroupId());
            dto.setPrerequisiteType(cp.getPrerequisiteType());
            dto.setOperatorToNext(cp.getOperatorToNext());
            dto.setParent(cp.isParent());
            dto.setChild(cp.isChild());
            dto.setChildId(cp.getChildId());
            dto.setParentId(cp.getParentId());
            return dto;
        }).collect(Collectors.toList());

        FlatCoursePrerequisiteRequest response = new FlatCoursePrerequisiteRequest();
        response.setCourseId(courseId);
        response.setPrerequisites(dtos);

        return response;
    }



}
