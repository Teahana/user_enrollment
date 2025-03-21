package group7.enrollmentSystem.services;

import group7.enrollmentSystem.dtos.classDtos.CourseDto;
import group7.enrollmentSystem.dtos.classDtos.CoursePrerequisiteRequest;
import group7.enrollmentSystem.dtos.classDtos.PrerequisiteGroupDTO;
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
    @Transactional
    public void addPrerequisites(CoursePrerequisiteRequest request) {
        Course course = courseRepo.findById(request.getCourseId())
                .orElseThrow(() -> new RuntimeException("Course not found with id: " + request.getCourseId()));

        // 🧹 CLEANUP existing groups + course prerequisites
        List<CoursePrerequisite> existingLinks = coursePrerequisiteRepo.findByCourse(course);
        Set<PrerequisiteGroup> topLevelGroups = existingLinks.stream()
                .map(CoursePrerequisite::getPrerequisiteGroup)
                .filter(g -> g.getParentGroup() == null)
                .collect(Collectors.toSet());

        coursePrerequisiteRepo.deleteAll(existingLinks);
        prerequisiteGroupRepository.deleteAll(topLevelGroups);

        // ✅ Proceed to save new prerequisite structure
        List<PrerequisiteGroup> savedGroups = new ArrayList<>();
        PrerequisiteGroup prevGroup = null;

        for (int i = 0; i < request.getPrerequisiteGroups().size(); i++) {
            PrerequisiteGroupDTO groupDTO = request.getPrerequisiteGroups().get(i);
            PrerequisiteGroup savedGroup = savePrerequisiteGroup(groupDTO, null);

            if (prevGroup != null) {
                prevGroup.setNextGroup(savedGroup);
                prevGroup.setOperatorToNext(PrerequisiteType.valueOf(groupDTO.getOperatorToNext()));
                prerequisiteGroupRepository.save(prevGroup);
            }

            prevGroup = savedGroup;
            savedGroups.add(savedGroup);
        }

        for (int i = 0; i < savedGroups.size(); i++) {
            saveCoursePrerequisites(course, savedGroups.get(i), request.getPrerequisiteGroups().get(i));
        }
    }



    // ✅ Save Prerequisite Groups recursively
    private PrerequisiteGroup savePrerequisiteGroup(PrerequisiteGroupDTO groupDTO, PrerequisiteGroup parentGroup) {
        PrerequisiteGroup group;
        if (groupDTO.getId() != null) {
            group = prerequisiteGroupRepository.findById(groupDTO.getId())
                    .orElse(new PrerequisiteGroup());
        } else {
            group = new PrerequisiteGroup();
        }
        group.setType(PrerequisiteType.valueOf(groupDTO.getType()));
        group.setParentGroup(parentGroup);
        PrerequisiteGroup savedGroup = prerequisiteGroupRepository.save(group);

        // Recursively save subgroups
        PrerequisiteGroup prevSubGroup = null;
        for (PrerequisiteGroupDTO subGroupDTO : groupDTO.getSubGroups()) {
            PrerequisiteGroup subGroup = savePrerequisiteGroup(subGroupDTO, savedGroup);
            if (prevSubGroup != null) {
                prevSubGroup.setNextGroup(subGroup);
                prevSubGroup.setOperatorToNext(PrerequisiteType.valueOf(subGroupDTO.getOperatorToNext()));
                prerequisiteGroupRepository.save(prevSubGroup);
            }
            prevSubGroup = subGroup;
        }

        return savedGroup;
    }

    // ✅ Save CoursePrerequisites based on groups
    private void saveCoursePrerequisites(Course course, PrerequisiteGroup group, PrerequisiteGroupDTO groupDTO) {
        // Create `CoursePrerequisite` entries from DTO course IDs
        for (Long prerequisiteId : groupDTO.getCourses()) {
            Course prerequisite = courseRepo.findById(prerequisiteId)
                    .orElseThrow(() -> new RuntimeException("Prerequisite Course not found: " + prerequisiteId));

            CoursePrerequisite coursePrerequisite = new CoursePrerequisite();
            coursePrerequisite.setCourse(course);
            coursePrerequisite.setPrerequisite(prerequisite);
            coursePrerequisite.setPrerequisiteGroup(group);
            coursePrerequisiteRepo.save(coursePrerequisite);
        }

        // Recursively handle subgroups
        for (int i = 0; i < groupDTO.getSubGroups().size(); i++) {
            PrerequisiteGroupDTO subGroupDTO = groupDTO.getSubGroups().get(i);
            PrerequisiteGroup subGroup = group.getSubGroups().get(i); // Get the matching saved entity

            saveCoursePrerequisites(course, subGroup, subGroupDTO);
        }
    }

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
}