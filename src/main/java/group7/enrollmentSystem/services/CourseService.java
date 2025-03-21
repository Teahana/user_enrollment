package group7.enrollmentSystem.services;

import group7.enrollmentSystem.dtos.classDtos.*;
import group7.enrollmentSystem.enums.PrerequisiteType;
import group7.enrollmentSystem.models.Course;
import group7.enrollmentSystem.models.CoursePrerequisite;
import group7.enrollmentSystem.models.PrerequisiteGroup;
import group7.enrollmentSystem.models.Programme;
import group7.enrollmentSystem.repos.CoursePrerequisiteRepo;
import group7.enrollmentSystem.repos.CourseProgrammeRepo;
import group7.enrollmentSystem.repos.CourseRepo;
import group7.enrollmentSystem.repos.PrerequisiteGroupRepository;
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
    private final PrerequisiteGroupRepository prerequisiteGroupRepository;


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

            // ✅ Fetch programme codes using CourseProgrammeRepo
            List<String> programmeCodes = courseProgrammeRepo.findProgrammesByCourseId(course.getId()).stream()
                    .map(Programme::getProgrammeCode)
                    .collect(Collectors.toList());
            dto.setProgrammes(programmeCodes);

            // ✅ Fetch prerequisites
            List<CoursePrerequisite> prerequisites = coursePrerequisiteRepo.findByCourseId(course.getId());

            // ✅ Step 1: Group by groupId
            Map<Integer, List<CoursePrerequisite>> groupedPrereqs = prerequisites.stream()
                    .collect(Collectors.groupingBy(CoursePrerequisite::getGroupId));

            // ✅ Step 2: Identify parent groups & maintain ordering
            List<Integer> parentGroups = prerequisites.stream()
                    .filter(CoursePrerequisite::isParent)
                    .sorted(Comparator.comparingInt(CoursePrerequisite::getGroupId))
                    .map(CoursePrerequisite::getGroupId)
                    .distinct()
                    .collect(Collectors.toList());

            List<String> formattedPrereqs = new ArrayList<>();

            // ✅ Step 3: Build prerequisite expressions per group
            for (int i = 0; i < parentGroups.size(); i++) {
                int parentGroupId = parentGroups.get(i);
                List<CoursePrerequisite> group = groupedPrereqs.get(parentGroupId);
                if (group == null) continue;

                StringBuilder groupString = new StringBuilder();

                for (int j = 0; j < group.size(); j++) {
                    CoursePrerequisite cp = group.get(j);
                    String courseCode = cp.getPrerequisite().getCourseCode();

                    if (j > 0) {
                        groupString.append(" ").append(cp.getOperatorToNext()).append(" ");
                    }

                    groupString.append(courseCode);
                }

                // Wrap in parentheses if multiple conditions exist
                if (group.size() > 1) {
                    formattedPrereqs.add("(" + groupString.toString() + ")");
                } else {
                    formattedPrereqs.add(groupString.toString());
                }

                // ✅ Step 4: Add `operatorToNext` between parent groups
                if (i < parentGroups.size() - 1) {
                    CoursePrerequisite lastInGroup = group.get(group.size() - 1);
                    if (lastInGroup.getOperatorToNext() != null) {
                        formattedPrereqs.add(lastInGroup.getOperatorToNext().toString());
                    }
                }
            }

            // ✅ Step 5: Convert to a single string expression
            dto.setPrerequisites(List.of(String.join(" ", formattedPrereqs)));
            dto.setHasPreReqs(!prerequisites.isEmpty());
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

    @Transactional
    public void addPrerequisites(FlatCoursePrerequisiteRequest request) {
        Course mainCourse = courseRepo.findById(request.getCourseId())
                .orElseThrow(() -> new IllegalArgumentException("Course not found with ID: " + request.getCourseId()));

        List<CoursePrerequisite> prerequisitesToSave = new ArrayList<>();

        for (FlatCoursePrerequisiteDTO dto : request.getPrerequisites()) {
            // Skip if course is same as prerequisite
            if (dto.getCourseId().equals(dto.getPrerequisiteId())) {
                throw new IllegalArgumentException("A course cannot be a prerequisite to itself.");
            }

            // Fetch the referenced prerequisite course
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

            prerequisitesToSave.add(prerequisite);
        }

        coursePrerequisiteRepo.saveAll(prerequisitesToSave);
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
            return dto;
        }).collect(Collectors.toList());

        FlatCoursePrerequisiteRequest response = new FlatCoursePrerequisiteRequest();
        response.setCourseId(courseId);
        response.setPrerequisites(dtos);

        return response;
    }



}
