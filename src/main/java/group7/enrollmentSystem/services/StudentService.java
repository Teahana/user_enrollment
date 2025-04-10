package group7.enrollmentSystem.services;

import com.itextpdf.text.DocumentException;
import group7.enrollmentSystem.dtos.appDtos.EnrollCourseRequest;
import group7.enrollmentSystem.dtos.classDtos.CourseEnrollmentDto;
import group7.enrollmentSystem.dtos.classDtos.InvoiceDto;
import group7.enrollmentSystem.enums.PrerequisiteType;
import group7.enrollmentSystem.enums.SpecialPrerequisiteType;
import group7.enrollmentSystem.helpers.InvoicePdfGeneratorService;
import group7.enrollmentSystem.models.*;
import group7.enrollmentSystem.repos.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDate;
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
    private final StudentProgrammeService studentProgrammeService;
    private final CourseEnrollmentService courseEnrollmentService;
    private final InvoicePdfGeneratorService invoicePdfGeneratorService;

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
            // Handle special prerequisites
            if (cp.isSpecial()) {
                if (cp.getSpecialType() == SpecialPrerequisiteType.ADMISSION_PROGRAMME) {
                    if (cp.getProgramme() != null && cp.getProgramme().getId().equals(studentProgramme.getId())) {
                        conditions.add(true); // student is admitted to required programme
                    } else {
                        conditions.add(false); // not admitted to this programme
                    }
                } else if (cp.getSpecialType() == SpecialPrerequisiteType.COMPLETION_LEVEL_PERCENT) {
                    short level = cp.getTargetLevel();
                    double requiredPercent = cp.getPercentageValue();

                    List<Long> levelCourseIds = courseIdsForProgramme.stream()
                            .map(id -> courseRepo.findById(id).orElse(null))
                            .filter(Objects::nonNull)
                            .filter(c -> c.getLevel() == level)
                            .map(Course::getId)
                            .toList();

                    long total = levelCourseIds.size();
                    long completed = levelCourseIds.stream().filter(completedCourseIds::contains).count();
                    System.out.println("Total: " + total + ", Completed: " + completed + ", Required Percent: " + requiredPercent);
                    boolean met = total > 0 && ((double) completed / total) >= requiredPercent;
                    conditions.add(met);
                }
                continue;// skip normal logic if special handled
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

    public void enrollStudent(EnrollCourseRequest request) {
        Optional<Student> optionalStudent = studentRepo.findById(request.getUserId());
        if(!optionalStudent.isPresent()) {
            throw new RuntimeException("Student not found with ID: " + request.getUserId());
        }
        Student student = optionalStudent.get();
        Programme programme = studentProgrammeRepo.findStudentCurrentProgramme(student)
                .orElseThrow(() -> new RuntimeException("Programme not found for student with ID: " + student.getId()));
        int currentlyApplied = studentRepo.getCurrentlyAppliedByStudent(student);
        if(request.getSelectedCourses() == null || request.getSelectedCourses().isEmpty()) {
            throw new RuntimeException("No courses selected for enrollment.");
        }
        System.out.println("Currently applied courses: " + currentlyApplied);
        System.out.println("Selected courses: " + request.getSelectedCourses().size());
        if(currentlyApplied + request.getSelectedCourses().size() > 4){
            throw new RuntimeException("You have reached the maximum number of courses you can apply for (4).");
        }
        List<String> inEligibleCourseCodes = new ArrayList<>();
        boolean isValid = validateEnrollmentRequest(student,programme,request.getSelectedCourses(),inEligibleCourseCodes);

        if(isValid) {
            int semester = enrollmentStateRepo.isSemesterOne() ? 1 : 2;
            for (String courseCode : request.getSelectedCourses()) {
                Course course = courseRepo.findByCourseCode(courseCode)
                        .orElseThrow(() -> new RuntimeException("Course not found with code: " + courseCode));
                CourseEnrollment enrollment = new CourseEnrollment();
                enrollment.setStudent(student);
                enrollment.setCourse(course);
                enrollment.setCurrentlyTaking(true);
                enrollment.setDateEnrolled(LocalDate.now());
                enrollment.setProgramme(programme);
                enrollment.setSemesterEnrolled(semester);
                courseEnrollmentRepo.save(enrollment);
            }
        } else {
            String inEligibleCourseCode = inEligibleCourseCodes.getFirst();
            throw new RuntimeException("You are not eligible to enroll in " + inEligibleCourseCode + ". Please check the prerequisites.");
        }

    }

    private boolean validateEnrollmentRequest(Student student,Programme programme, List<String> selectedCourses, List<String> inEligibleCourseCodes) {
        // Retrieve courses by course codes.
        List<Course> courses = courseRepo.findByCourseCodeIn(selectedCourses);

        // Check if all selected courses were found.
        if (courses.size() != selectedCourses.size()) {
            throw new RuntimeException("One or more selected courses could not be found.");
        }

        // Get all course IDs for the student's programme.
        List<Long> courseIdsForProgramme = courseProgrammeRepo.getCourseIdsByProgramme(programme);

        // Get the student's completed courses.
        List<Long> completedCourseIds = courseEnrollmentRepo.getCompletedCourseIdsByStudent(student);

        // Check prerequisites for each selected course.
        for (Course course : courses) {
            if (!isEligibleForCourse(course.getId(), completedCourseIds, programme, courseIdsForProgramme)) {
                inEligibleCourseCodes.add(course.getCourseCode());
                return false;
            }
        }
        return true;
    }

    public Student getStudentByEmail(String email) {
        return studentRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Student not found with email: " + email));
    }
    public byte[] generateInvoicePdfForStudent(String email) throws DocumentException, IOException {
        Student student = studentRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        List<CourseEnrollmentDto> enrolledCourses = courseEnrollmentService.getActiveEnrollments(student.getId())
                .stream()
                .map(ce -> new CourseEnrollmentDto(
                        ce.getCourse().getId(),
                        ce.getCourse().getCourseCode(),
                        ce.getCourse().getTitle(),
                        ce.getCourse().getCost()))
                .collect(Collectors.toList());

        double totalDue = enrolledCourses.stream()
                .mapToDouble(CourseEnrollmentDto::getCost)
                .sum();

        InvoiceDto invoiceDto = new InvoiceDto();
        invoiceDto.setStudentName(student.getFirstName() + " " + student.getLastName());
        invoiceDto.setStudentId(student.getStudentId());

        Optional<StudentProgramme> currentProgramme = studentProgrammeService.getCurrentProgramme(student);
        if (currentProgramme.isPresent()) {
            invoiceDto.setProgramme(currentProgramme.get().getProgramme().getName());
        } else {
            throw new RuntimeException("No current programme found for the student");
        }

        invoiceDto.setEnrolledCourses(enrolledCourses);
        invoiceDto.setTotalDue(totalDue);

        return invoicePdfGeneratorService.generateInvoicePdf(invoiceDto);
    }


    public List<CourseEnrollmentDto> getEnrolledCourses(Student student) {
        List<CourseEnrollment> courseEnrollments = courseEnrollmentRepo.findByStudentAndCurrentlyTakingTrue(student);
        return courseEnrollments.stream()
                .map(ce -> new CourseEnrollmentDto(
                        ce.getCourse().getId(),
                        ce.getCourse().getCourseCode(),
                        ce.getCourse().getTitle(),
                        ce.getCourse().getCost()))
                .collect(Collectors.toList());

    }
    @Transactional
    public void cancelCourse(long courseId, long userId) {
        Course course = courseRepo.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found with ID: " + courseId));
        Student student = studentRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("Student not found with ID: " + userId));
        CourseEnrollment courseEnrollment = courseEnrollmentRepo.findByStudentAndCourseAndCurrentlyTakingTrue(student, course);

        if (courseEnrollment == null) {
            throw new RuntimeException("No ongoing course enrollment found for cancellation.");
        }

        courseEnrollment.setCancelled(true);
        courseEnrollment.setCurrentlyTaking(false);
        courseEnrollmentRepo.save(courseEnrollment);
    }

    public void passEnrolledCourses(long userId, List<String> selectedCourses) {
        Student student = studentRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("Student not found with ID: " + userId));
        List<Course> courses = courseRepo.findByCourseCodeIn(selectedCourses);
        if(courses.size() != selectedCourses.size()) {
            throw new RuntimeException("One or more selected courses could not be found.");
        }
        List<CourseEnrollment> courseEnrollments = courseEnrollmentRepo.findByStudentAndCourseInAndCurrentlyTakingTrue(student, courses);
        if(courseEnrollments.size() != selectedCourses.size()) {
            throw new RuntimeException("One or more selected courses are not currently enrolled.");
        }
        for (CourseEnrollment enrollment : courseEnrollments) {
            if (selectedCourses.contains(enrollment.getCourse().getCourseCode())) {
                enrollment.setCompleted(true);
                enrollment.setCurrentlyTaking(false);
                enrollment.setCancelled(false);
                courseEnrollmentRepo.save(enrollment);
            }
        }
    }
    public void failEnrolledCourses(long userId, List<String> selectedCourses) {
        Student student = studentRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("Student not found with ID: " + userId));
        List<Course> courses = courseRepo.findByCourseCodeIn(selectedCourses);
        if(courses.size() != selectedCourses.size()) {
            throw new RuntimeException("One or more selected courses could not be found.");
        }
        List<CourseEnrollment> courseEnrollments = courseEnrollmentRepo.findByStudentAndCourseInAndCurrentlyTakingTrue(student, courses);
        if(courseEnrollments.size() != selectedCourses.size()) {
            throw new RuntimeException("One or more selected courses are not currently enrolled.");
        }
        for (CourseEnrollment enrollment : courseEnrollments) {
            if (selectedCourses.contains(enrollment.getCourse().getCourseCode())) {
                enrollment.setCompleted(false);
                enrollment.setFailed(true);
                enrollment.setCurrentlyTaking(false);
                enrollment.setCancelled(false);
                courseEnrollmentRepo.save(enrollment);
            }
        }
    }
}

