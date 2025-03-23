package group7.enrollmentSystem.services;

import group7.enrollmentSystem.dtos.classDtos.*;
import group7.enrollmentSystem.enums.PrerequisiteType;
import group7.enrollmentSystem.models.Course;
import group7.enrollmentSystem.models.CoursePrerequisite;
import group7.enrollmentSystem.models.Programme;
import group7.enrollmentSystem.repos.CoursePrerequisiteRepo;
import group7.enrollmentSystem.repos.CourseProgrammeRepo;
import group7.enrollmentSystem.repos.CourseRepo;
import group7.enrollmentSystem.repos.ProgrammeRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepo courseRepo;
    private final CoursePrerequisiteRepo coursePrerequisiteRepo;
    private final CourseProgrammeRepo courseProgrammeRepo;
    private final ProgrammeRepo programmeRepo;

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
        PrerequisiteType groupType = group.get(0).getPrerequisiteType();

        // Build expression for current group's courses
        for (int i = 0; i < group.size(); i++) {
            CoursePrerequisite cp = group.get(i);
            String courseCode = cp.getPrerequisite().getCourseCode();
            String programmeCode = cp.getProgramme() != null
                    ? cp.getProgramme().getProgrammeCode()
                    : "All";

            if (i > 0) {
                groupExpression.append(" ").append(groupType).append(" ");
            }

            // No space between courseCode and programmeCode
            groupExpression.append(courseCode).append("(").append(programmeCode).append(")");
        }

        // Add subgroups
        if (parentToChildGroupMap.containsKey(groupId)) {
            for (int childGroupId : parentToChildGroupMap.get(groupId)) {
                String childExpression = buildGroupExpression(childGroupId, groupedPrereqs, parentToChildGroupMap);
                if (!childExpression.isEmpty()) {
                    groupExpression.append(" ").append(groupType).append(" ").append(childExpression);
                }
            }
        }

        // Wrap in parentheses if more than one element
        if (group.size() > 1 || parentToChildGroupMap.containsKey(groupId)) {
            return "(" + groupExpression + ")";
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
                .orElseThrow(() -> new IllegalArgumentException(
                        "Course not found with ID: " + request.getCourseId()));

        // Check if course already has prereqs
        if (!coursePrerequisiteRepo.findByCourse(mainCourse).isEmpty()) {
            throw new IllegalStateException(
                    "Course already has prerequisites. Use update instead.");
        }

        List<FlatCoursePrerequisiteDTO> prerequisites = request.getPrerequisites();
        if (prerequisites.isEmpty()) {
            throw new IllegalArgumentException("No prerequisites provided.");
        }
        // Validate groups if needed
        validatePrerequisiteGroups(prerequisites);

        List<CoursePrerequisite> prerequisitesToSave = new ArrayList<>();

        for (FlatCoursePrerequisiteDTO dto : prerequisites) {
            if (dto.getCourseId().equals(dto.getPrerequisiteId())) {
                throw new IllegalArgumentException(
                        "A course cannot be a prerequisite to itself.");
            }

            Course prerequisiteCourse = courseRepo.findById(dto.getPrerequisiteId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Prerequisite course not found: " + dto.getPrerequisiteId()));

            // *** Handle the programmeId possibly being null:
            Programme programme = null;
            if (dto.getProgrammeId() != null) {
                programme = programmeRepo.findById(dto.getProgrammeId())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Programme not found with ID: " + dto.getProgrammeId()));
            }

            CoursePrerequisite prerequisite = new CoursePrerequisite();
            prerequisite.setCourse(mainCourse);
            prerequisite.setPrerequisite(prerequisiteCourse);
            prerequisite.setProgramme(programme);  // will be null if "All"
            prerequisite.setPrerequisiteType(dto.getPrerequisiteType());
            prerequisite.setOperatorToNext(dto.getOperatorToNext());
            prerequisite.setGroupId(dto.getGroupId());
            prerequisite.setParent(dto.isParent());
            prerequisite.setChild(dto.isChild());
            prerequisite.setChildId(dto.getChildId());

            if (dto.isChild()) {
                if (dto.getParentId() == 0 || dto.getParentId() == dto.getGroupId()) {
                    throw new IllegalArgumentException("Invalid parentId for child group: "
                            + dto.getGroupId());
                }
                prerequisite.setParentId(dto.getParentId());
            }

            prerequisitesToSave.add(prerequisite);
        }

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

        List<FlatCoursePrerequisiteDTO> dtos = entities.stream()
                .map(cp -> {
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

                    // If programme is null => means "All"
                    Programme prog = cp.getProgramme();
                    dto.setProgrammeId(prog == null ? null : prog.getId());

                    return dto;
                })
                .collect(Collectors.toList());

        FlatCoursePrerequisiteRequest response = new FlatCoursePrerequisiteRequest();
        response.setCourseId(courseId);
        response.setPrerequisites(dtos);

        return response;
    }



    public GraphicalPrerequisiteNode buildPrerequisiteTree(Long courseId) {
        // 1) Load the main course
        Course mainCourse = courseRepo.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found"));

        // 2) Create the top-level node
        GraphicalPrerequisiteNode root = new GraphicalPrerequisiteNode();
        root.setCourseId(mainCourse.getId());
        root.setCourseCode(mainCourse.getCourseCode());
        root.setLevel(mainCourse.getLevel());
        // The operator for the “root” might be null or “AND” by default
        root.setOperator(null);

        // 3) Fetch all CoursePrerequisite rows for this course
        List<CoursePrerequisite> prereqs = coursePrerequisiteRepo.findByCourseId(courseId);
        if (prereqs.isEmpty()) {
            return root; // no prerequisites
        }

        // 4) Convert to subgroups: see your existing grouping logic
        // For example, group by groupId:
        Map<Integer, List<CoursePrerequisite>> byGroupId = prereqs.stream()
                .collect(Collectors.groupingBy(CoursePrerequisite::getGroupId));

        // Identify “parent groups” vs “child groups” if needed
        // or just build them all into sub-nodes:
        // For demonstration, we'll create 1 child node per unique group ID
        List<GraphicalPrerequisiteNode> children = new ArrayList<>();
        for (Integer groupId : byGroupId.keySet()) {
            List<CoursePrerequisite> groupList = byGroupId.get(groupId);
            // Suppose we assume they all share the same PrerequisiteType
            PrerequisiteType groupType = groupList.get(0).getPrerequisiteType();

            GraphicalPrerequisiteNode groupNode = new GraphicalPrerequisiteNode();
            // You might treat the "group node" as an operator node, or
            // you might treat each row as a child. Up to you.
            groupNode.setOperator(groupType);
            // If you want operatorToNext as well:
            groupNode.setOperatorToNext(groupList.get(0).getOperatorToNext());

            // For each row in this group, add a child for the actual course
            for (CoursePrerequisite cp : groupList) {
                Course prereqCourse = cp.getPrerequisite(); // the “prerequisite” course
                GraphicalPrerequisiteNode childNode = new GraphicalPrerequisiteNode(
                        prereqCourse.getId(),
                        prereqCourse.getCourseCode(),
                        prereqCourse.getLevel(),
                        cp.getPrerequisiteType(),
                        cp.getOperatorToNext(),
                        new ArrayList<>()
                );
                groupNode.getChildren().add(childNode);

                // If you want to go multiple levels deep (the child’s prerequisites, etc.),
                // you could RECURSE here: childNode.setChildren( ... buildPrerequisiteTree(...) ... )
                // But be mindful to avoid cycles in your data.
            }

            children.add(groupNode);
        }

        root.setChildren(children);

        return root;
    }
    public String getMermaidDiagramForCourse(Long courseId) {
        Optional<Course> courseOpt = courseRepo.findById(courseId);
        if (courseOpt.isEmpty()) {
            return "%% Error: Course not found";
        }

        Course course = courseOpt.get();
        List<CoursePrerequisite> prerequisites = coursePrerequisiteRepo.findByCourseId(courseId);

        if (prerequisites.isEmpty()) {
            return "%% No prerequisites found for this course";
        }

        // Group prerequisites
        Map<Integer, List<CoursePrerequisite>> groupedPrereqs = prerequisites.stream()
                .collect(Collectors.groupingBy(CoursePrerequisite::getGroupId));

        List<Integer> parentGroups = prerequisites.stream()
                .filter(cp -> cp.isParent() && !cp.isChild())
                .sorted(Comparator.comparingInt(CoursePrerequisite::getGroupId))
                .map(CoursePrerequisite::getGroupId)
                .distinct()
                .collect(Collectors.toList());

        Map<Integer, Set<Integer>> parentToChildGroupMap = new HashMap<>();
        for (CoursePrerequisite cp : prerequisites) {
            if (cp.isChild()) {
                parentToChildGroupMap
                        .computeIfAbsent(cp.getParentId(), k -> new HashSet<>())
                        .add(cp.getGroupId());
            }
        }

        StringBuilder prerequisiteExpression = new StringBuilder();
        for (int i = 0; i < parentGroups.size(); i++) {
            int parentGroupId = parentGroups.get(i);
            String expr = buildGroupExpression(parentGroupId, groupedPrereqs, parentToChildGroupMap);
            prerequisiteExpression.append(expr);

            if (i < parentGroups.size() - 1) {
                PrerequisiteType op = groupedPrereqs.get(parentGroupId).get(0).getOperatorToNext();
                if (op != null) {
                    prerequisiteExpression.append(" ").append(op).append(" ");
                }
            }
        }

        return convertToMermaid(course.getCourseCode(), prerequisiteExpression.toString());
    }
    public String convertToMermaid(String courseCode, String expression) {
        StringBuilder sb = new StringBuilder("graph TD\n");
        AtomicInteger nodeId = new AtomicInteger(0);
        List<String> nodes = new ArrayList<>();
        List<String> edges = new ArrayList<>();

        // Generate unique node
        Function<String, String> getNode = label -> {
            String id = "N" + nodeId.getAndIncrement();
            String safeLabel = label.replace("\"", "\\\"");
            nodes.add(id + "[\"" + safeLabel + "\"]");
            return id;
        };

        // Fully wrapped check
        Function<String, Boolean> isFullyWrapped = str -> {
            str = str.trim();
            if (!str.startsWith("(") || !str.endsWith(")")) return false;
            int depth = 0;
            for (int i = 0; i < str.length(); i++) {
                char c = str.charAt(i);
                if (c == '(') depth++;
                if (c == ')') depth--;
                if (depth == 0 && i < str.length() - 1) return false;
            }
            return depth == 0;
        };

        // Recursive parser
        Function<String, String> parse = new Function<>() {
            @Override
            public String apply(String expr) {
                expr = expr.trim();
                if (isFullyWrapped.apply(expr)) {
                    expr = expr.substring(1, expr.length() - 1).trim();
                }

                List<String> parts = splitByTopLevel(expr, "OR");
                String operator = "OR";

                if (parts.size() == 1) {
                    parts = splitByTopLevel(expr, "AND");
                    operator = "AND";
                }

                if (parts.size() == 1) {
                    return getNode.apply(parts.get(0));
                }

                String opNode = getNode.apply(operator);
                for (String part : parts) {
                    String childId = this.apply(part);
                    edges.add(opNode + " --> " + childId);
                }
                return opNode;
            }
        };

        // Root
        String root = getNode.apply(courseCode + " (Main Course)");
        String body = parse.apply(expression);
        edges.add(root + " --> " + body);

        nodes.forEach(line -> sb.append(line).append("\n"));
        edges.forEach(line -> sb.append(line).append("\n"));


        return sb.toString();
    }
    private List<String> splitByTopLevel(String expr, String operator) {
        List<String> parts = new ArrayList<>();
        int depth = 0;
        StringBuilder current = new StringBuilder();

        for (int i = 0; i < expr.length(); i++) {
            char c = expr.charAt(i);
            if (c == '(') depth++;
            else if (c == ')') depth--;

            if (depth == 0 && expr.startsWith(" " + operator + " ", i)) {
                parts.add(current.toString().trim());
                current.setLength(0);
                i += operator.length() + 1;
            } else {
                current.append(c);
            }
        }

        if (current.length() > 0) {
            parts.add(current.toString().trim());
        }

        return parts;
    }

    @Transactional
    public void deletePrerequisites(Course course) {
        coursePrerequisiteRepo.deleteByCourse(course);
    }
    //Temporary
    //Used to fetch prerequisiteCourseCodes for Prerequisite Column in selectCourses Page
    public List<String> getPrerequisiteCodesByCourseId(Long courseId) {
        return coursePrerequisiteRepo.findPrerequisitesByCourseId(courseId).stream()
                .map(Course::getCourseCode)
                .collect(Collectors.toList());
    }
}