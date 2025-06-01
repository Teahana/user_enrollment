package group7.enrollmentSystem.services;

import com.itextpdf.text.DocumentException;
import group7.enrollmentSystem.config.CustomExceptions;
import group7.enrollmentSystem.dtos.appDtos.EnrollCourseRequest;
import group7.enrollmentSystem.dtos.classDtos.CourseEnrollmentDto;
import group7.enrollmentSystem.dtos.classDtos.CoursesTranscriptDTO;
import group7.enrollmentSystem.dtos.classDtos.InvoiceDto;
import group7.enrollmentSystem.enums.PrerequisiteType;
import group7.enrollmentSystem.enums.SpecialPrerequisiteType;
import group7.enrollmentSystem.helpers.*;
import group7.enrollmentSystem.models.*;
import group7.enrollmentSystem.repos.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalTime;
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
    private final GradeService gradeService;
    private final Random random = new Random();
    private final StudentHoldService studentHoldService;
    private final EmailService emailService;

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
                        course.getCost(),
                        false
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
        EnrollmentState state = enrollmentStateRepo.findById(1L)
                .orElseThrow(() -> new RuntimeException("Enrollment state not found"));
        if (!state.isOpen()) {
            throw new RuntimeException("The course enrollment period is closed. Please contact Student Administrative Services for more info.");
        }
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
        //Check for any holds
        int holdSize = StudentHoldService.HoldRestrictionType.values().length;
        for(int i = 0; i < holdSize; i++) {
            if(studentHoldService.hasRestriction(student.getEmail(),
                    StudentHoldService.HoldRestrictionType.values()[i])){
                throw new RuntimeException("You cannot enroll in courses due to a hold on your account: " +
                        StudentHoldService.HoldRestrictionType.values()[i]);
            }
        }
        Map<String, Object> response = validateEnrollmentRequest(student,programme,request.getSelectedCourses());

        if((boolean)response.get("isEligible")) {
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
                enrollment.setPaid(false);
                courseEnrollmentRepo.save(enrollment);
            }
        } else {
            throw new RuntimeException((String)response.get("message"));
        }
    }

    private Map<String, Object> validateEnrollmentRequest(Student student,Programme programme, List<String> selectedCourses) {
        Map<String, Object> response = new HashMap<>();
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
        // Check if course is offered in current sem
        for (Course course : courses) {
            if(enrollmentStateRepo.isSemesterOne()) {
                if(!course.isOfferedSem1()){
                    response.put("isEligible", false);
                    response.put("message","You are not eligible to enroll in " + course.getCourseCode() + ". It is not offered in Semester 1.");
                    return response;
                }
            }
            else{
                if(!course.isOfferedSem2()){
                    response.put("isEligible", false);
                    response.put("message","You are not eligible to enroll in " + course.getCourseCode() + ". It is not offered in Semester 2.");
                    return response;
                }
            }
        }
        // Check prerequisites for each selected course.
        for (Course course : courses) {
            if (!isEligibleForCourse(course.getId(), completedCourseIds, programme, courseIdsForProgramme)) {
                response.put("isEligible", false);
                response.put("message","You are not eligible to enroll in " + course.getCourseCode() + ". Please check the prerequisites.");
                return response;
            }
        }
        response.put("isEligible", true);
        return response;
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
                        ce.getCourse().getCost(),
                        ce.isPaid()))
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
                        ce.getCourse().getCost(),
                        ce.isPaid()))
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
                int passMark = generatePassMark();
                String grade = gradeService.getGrade(passMark);
                enrollment.setGrade(grade);
                enrollment.setMark(passMark);
                enrollment.setCompleted(true);
                enrollment.setCurrentlyTaking(false);
                enrollment.setCancelled(false);
                enrollment.setFailed(false);
                courseEnrollmentRepo.save(enrollment);
            }
        }
    }
    private int generatePassMark(){
        int lowestPassingMark = gradeService.getLowestPassingMark();
        return this.random.nextInt(lowestPassingMark,101);//101 because the upperbound is exclusive
    }
    private int generateFailMark(){
        int lowestPassingMark = gradeService.getLowestPassingMark();
        return this.random.nextInt(0,lowestPassingMark+1);//101 because the upperbound is exclusive
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

    public void saveProfilePicture(String email, MultipartFile file) throws IOException {
        Student student = studentRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();

        // This will resolve to: <project-root>/uploadedFiles/
        Path uploadPath = Paths.get("uploadedFiles/");
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        Path filePath = uploadPath.resolve(filename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // Store relative path for use in frontend
        student.setPfpFilePath(filename);
        studentRepo.save(student);
    }
    public Resource getProfilePicture(Long userId) throws IOException {
        Student student = studentRepo.findById(userId)
                .orElseThrow(() -> new CustomExceptions.StudentNotFoundException("Student with ID " + userId + " not found"));

        String filename = student.getPfpFilePath();
        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException("No profile picture uploaded for student");
        }

        Path filePath = Paths.get("uploadedFiles").resolve(filename);
        if (!Files.exists(filePath)) {
            throw new FileNotFoundException("Profile picture file not found on server");
        }

        return new UrlResource(filePath.toUri());
    }

    public void payCourse(Long courseId, String studentEmail) {
        Course c = courseRepo.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found with ID: " + courseId));
        Student s = studentRepo.findByEmail(studentEmail)
                .orElseThrow(() -> new RuntimeException("Student not found with email: " + studentEmail));
        CourseEnrollment ce = courseEnrollmentRepo.findByStudentAndCourseAndCurrentlyTakingTrue(s,c);
        ce.setPaid(true);
        courseEnrollmentRepo.save(ce);
    }

    public void completeCourse(Long courseId, String name) {
        Course c = courseRepo.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found with ID: " + courseId));
        Student s = studentRepo.findByEmail(name)
                .orElseThrow(() -> new RuntimeException("Student not found with email: " + name));
        CourseEnrollment ce = courseEnrollmentRepo.findByStudentAndCourseAndCurrentlyTakingTrue(s,c);
        int mark = generatePassMark();
        String grade = gradeService.getGrade(mark);
        ce.setCompleted(true);
        ce.setFailed(false);
        ce.setCurrentlyTaking(false);
        ce.setCancelled(false);
        ce.setGrade(grade);
        ce.setMark(mark);
        courseEnrollmentRepo.save(ce);
    }
    public void failCourse(Long courseId, String name) {
        Course c = courseRepo.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found with ID: " + courseId));
        Student s = studentRepo.findByEmail(name)
                .orElseThrow(() -> new RuntimeException("Student not found with email: " + name));
        CourseEnrollment ce = courseEnrollmentRepo.findByStudentAndCourseAndCurrentlyTakingTrue(s,c);
        int mark = generateFailMark();
        String grade = gradeService.getGrade(mark);
        ce.setCompleted(true);
        ce.setFailed(true);
        ce.setCurrentlyTaking(false);
        ce.setCancelled(false);
        ce.setGrade(grade);
        ce.setMark(mark);
        courseEnrollmentRepo.save(ce);
    }

    public void requestGradeChange(Long enrollmentId, String studentEmail) {
        CourseEnrollment ce = courseEnrollmentRepo.findById(enrollmentId).orElseThrow(
                () -> new RuntimeException("Enrollment not found with ID: " + enrollmentId));
        if(ce.isRequestGradeChange()){
            throw new RuntimeException("Enrollment with ID " + enrollmentId + " is already requested.");
        }
        ce.setRequestGradeChange(true);
        ce.setRequestGradeChangeDate(LocalDate.now());
        ce.setRequestGradeChangeTime(LocalTime.now());
        courseEnrollmentRepo.save(ce);
        Map<String, Object> studentModel = new HashMap<>();
        studentModel.put("subject", "Grade Change Request");
        studentModel.put("header", "Grade Change");
        studentModel.put("body", "You have requested a grade change for course: " + ce.getCourse().getTitle()
                + " (" + ce.getCourse().getCourseCode() + ").");
        emailService.notifyStudentGradeChangeRequest(studentEmail, studentModel);
        Map<String, Object> adminModel = new HashMap<>();
        adminModel.put("subject", "Grade Change Request");
        adminModel.put("header", "Grade Change");
        // Include HTML for the link
        adminModel.put("body", "Student " + studentEmail + " has requested a grade change for course: "
                + ce.getCourse().getTitle() + " (" + ce.getCourse().getCourseCode() + ").<br/>"
                + "You can view all grade change requests <a href='http://localhost/admin/gradeChangeRequests'>here</a>.");

        emailService.notifyAdminGradeChangeRequest("adriandougjonajitino@gmail.com",adminModel);
    }

    @Autowired
    private CoursesTranscriptPdfGeneratorService coursesTranscriptPdfGeneratorService;

    public byte[] generateCoursesTranscriptPdfForStudent(String email) throws DocumentException, IOException {
        Student student = studentRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        List<CourseEnrollment> completedCourses = courseEnrollmentRepo.findByStudent(student)
                .stream()
                .filter(CourseEnrollment::isCompleted)
                .toList();

        List<CoursesTranscriptDTO.CourseTranscriptRow> rows = new ArrayList<>();
        double totalMarks = 0;
        int count = 0;

        for (CourseEnrollment ce : completedCourses) {
            CoursesTranscriptDTO.CourseTranscriptRow row = new CoursesTranscriptDTO.CourseTranscriptRow();
            row.setCourseCode(ce.getCourse().getCourseCode());
            row.setTitle(ce.getCourse().getTitle());
            row.setGrade(ce.getGrade());
            row.setMark(ce.getMark());
            row.setFailed(ce.isFailed());
            rows.add(row);

            if (!ce.isFailed()) {
                totalMarks += ce.getMark();
                count++;
            }
        }

        double gpa = count > 0 ? (totalMarks / count) / 25.0 : 0.0; // e.g., scale: 100 = 4.0 GPA

        CoursesTranscriptDTO dto = new CoursesTranscriptDTO();
        dto.setStudentId(student.getStudentId());
        dto.setStudentName(student.getFirstName() + " " + student.getLastName());

        studentProgrammeService.getCurrentProgramme(student)
                .ifPresentOrElse(
                        sp -> dto.setProgramme(sp.getProgramme().getName()),
                        () -> { throw new RuntimeException("No current programme found for the student"); }
                );

        dto.setCompletedCourses(rows);
        dto.setGpa(gpa);

        // Separate rows
        List<CoursesTranscriptDTO.CourseTranscriptRow> passed = rows.stream()
                .filter(r -> !r.getGrade().equalsIgnoreCase("F") && !r.getGrade().equalsIgnoreCase("E"))
                .toList();

        List<CoursesTranscriptDTO.CourseTranscriptRow> failed = rows.stream()
                .filter(r -> r.getGrade().equalsIgnoreCase("F") || r.getGrade().equalsIgnoreCase("E"))
                .toList();

        dto.setCompletedCourses(rows); // All completed
        dto.setPassedCourses(passed);  // Subset: passed
        dto.setFailedCourses(failed);  // Subset: failed


        return coursesTranscriptPdfGeneratorService.generateTranscriptPdf(dto);
    }

}

