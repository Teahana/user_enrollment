package group7.enrollmentSystem.controllers;

import group7.enrollmentSystem.dtos.classDtos.CourseEnrollmentDto;
import group7.enrollmentSystem.dtos.classDtos.EnrollmentPageData;
import group7.enrollmentSystem.dtos.classDtos.InvoiceDto;
import group7.enrollmentSystem.dtos.classDtos.StudentFullAuditDto;
import group7.enrollmentSystem.helpers.ProgrammeAuditPdfGeneratorService;
import group7.enrollmentSystem.models.*;
import group7.enrollmentSystem.repos.*;
import group7.enrollmentSystem.services.*;
import group7.enrollmentSystem.helpers.InvoicePdfGeneratorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/student")
@RequiredArgsConstructor
public class StudentController {

    private final CourseEnrollmentService courseEnrollmentService;
    private final StudentRepo studentRepo;
    private final CourseProgrammeRepo courseProgrammeRepo;
    private final StudentProgrammeService studentProgrammeService;
    private final CourseService courseService;
    private final EnrollmentStateRepo enrollmentStateRepo;
    private final CourseRepo courseRepo;
    private final CourseEnrollmentRepo courseEnrollmentRepo;
    private final UserRepo userRepo;
    private final InvoicePdfGeneratorService invoicePdfGeneratorService;
    private final StudentService studentService;
    private final ProgrammeAuditPdfGeneratorService programmeAuditPdfGeneratorService;
    private final StudentProgrammeAuditService studentProgrammeAuditService;


    @GetMapping("/enrollment")
    public String enrollment(Model model, Principal principal) {
        EnrollmentState state = enrollmentStateRepo.findById(1L)
                .orElseThrow(() -> new RuntimeException("Enrollment state not found"));

        if (!state.isOpen()) {
            model.addAttribute("pageOpen", false);
            model.addAttribute("message", "The course enrollment period has ended<br>Please contact Student Administrative Services for more info");
            return "enrollment";
        }

        String email = principal.getName();
        Student student = studentRepo.findByEmail(email).orElseThrow();
        Programme programme = studentProgrammeService.getStudentProgramme(student);

        int semester = state.isSemesterOne() ? 1 : 2;
        EnrollmentPageData data = courseEnrollmentService.getEnrollmentPageData(student, programme, semester);

        model.addAttribute("student", student);
        model.addAttribute("programme", programme.getName());
        model.addAttribute("semester", semester);
        model.addAttribute("pageOpen", true);
        model.addAttribute("activeEnrollments", data.getActiveEnrollments());
        model.addAttribute("canceledEnrollments", data.getCanceledEnrollments());
        model.addAttribute("eligibleCourses", data.getEligibleCourses()); // optional for selectCourses view

        return "enrollment";
    }

    @GetMapping("/selectCourses")
    public String selectCourses(Model model, Principal principal) {
        // Fetch current student
        String email = principal.getName();
        Student student = studentRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        // Determine current semester from enrollment state
        EnrollmentState enrollmentState = enrollmentStateRepo.findById(1L)
                .orElseThrow(() -> new RuntimeException("Enrollment state not found"));
        int semester = enrollmentState.isSemesterOne() ? 1 : 2;

        // Fetch eligible courses
       // List<CourseEnrollDto> eligibleCourses = courseEnrollmentService.getEligibleCoursesForEnrollment(student, semester);
        List<CourseEnrollmentDto> eligibleCourses = studentService.getEligibleCourses(email);

        // Pass to view
        model.addAttribute("courses", eligibleCourses);
        model.addAttribute("semester", semester);

        return "courseEnroll";
    }




    @PostMapping("/cancelEnrollment/{id}")
    public String cancelEnrollment(@PathVariable Long id,
                                   RedirectAttributes redirectAttributes)
    {
        // Just call your service to handle cancellation
        courseEnrollmentService.cancelEnrollment(id);

        redirectAttributes.addFlashAttribute("success", "Enrollment cancelled successfully.");
        // Redirect to /student/enrollment (no semester in path)
        return "redirect:/student/enrollment";
    }


    @PostMapping("/activateEnrollment/{id}")
    public String activateEnrollment(@PathVariable Long id,
                                     Principal principal,
                                     RedirectAttributes redirectAttributes)
    {
        // 1) Identify the student
        String email = principal.getName();
        Student student = studentRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        // 2) Retrieve the enrollment record
        CourseEnrollment enrollment = courseEnrollmentRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Enrollment record not found"));

        // 3) If you still want to limit 4 courses per semester, figure out the semester.
        //    Option A: The enrollment itself might have a getSemesterEnrolled() property:
        int semester = enrollment.getSemesterEnrolled();

        //    Option B: If you store "current semester" in an EnrollmentState table:
        //    EnrollmentState enrollmentState = enrollmentStatusRepo.findById(1L)
        //        .orElseThrow(...);
        //    int semester = enrollmentState.isSemesterOne() ? 1 : 2;

        // 4) Count how many courses are already active in that semester
        int activeEnrollmentsCount =
                courseEnrollmentService.getActiveEnrollmentsBySemester(student.getId(), semester)
                        .size();

        if (activeEnrollmentsCount >= 4) {
            redirectAttributes.addFlashAttribute("error",
                    "You already have four active courses this semester.");
            return "redirect:/student/enrollment";
        }
        // 5) Otherwise, activate the enrollment
        courseEnrollmentService.activateEnrollment(id);
        redirectAttributes.addFlashAttribute("success", "Enrollment activated successfully.");

        // 6) Redirect to a single page that lists all enrollments for the user
        return "redirect:/student/enrollment";
    }


    @PostMapping("/enrollCourses")
    public String enrollCourses(@RequestParam(value = "selectedCourses", required = false) List<Long> selectedCourseIds,
                                Principal principal,
                                Model model,
                                RedirectAttributes redirectAttributes) {

        // 1) If no courses were selected, handle gracefully
        if (selectedCourseIds == null || selectedCourseIds.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "No courses were selected.");
            return "redirect:/student/selectCourses";
        }
        // 2) Get the student
        String email = principal.getName();
        Student student = studentRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        // 3) Determine current semester from your enrollment state
        EnrollmentState enrollmentState = enrollmentStateRepo.findById(1L)
                .orElseThrow(() -> new RuntimeException("Enrollment state not found"));
        int semester = enrollmentState.isSemesterOne() ? 1 : 2;

        // 4) Count how many courses the student is already taking this semester
        int currentlyTakingCount = courseEnrollmentRepo
                .countByStudentAndSemesterEnrolledAndCurrentlyTakingIsTrue(student, semester);

        // 5) Check if adding these new courses would exceed the maximum (4 total)
        int totalCoursesIfAdded = currentlyTakingCount + selectedCourseIds.size();
        if (totalCoursesIfAdded > 4) {
            redirectAttributes.addFlashAttribute("error",
                    "You cannot enroll in more than 4 courses this semester. "
                            + "You are already taking " + currentlyTakingCount
                            + " course(s) and tried to add " + selectedCourseIds.size() + " more.");
            return "redirect:/student/selectCourses";
        }
        // 6) Perform the enrollment logic (saving to database)
        //    - For each selected course, create a CourseEnrollment entity, set
        //      'currentlyTaking = true', 'semesterEnrolled = semester', etc.
        //    - Example:
        List<CourseEnrollment> newEnrollments = new ArrayList<>();
        for (Long courseId : selectedCourseIds) {
            Course course = courseRepo.findById(courseId)
                    .orElseThrow(() -> new RuntimeException("Course not found (ID: " + courseId + ")"));

            // Create the enrollment record (or delegate to a service method)
            CourseEnrollment enrollment = new CourseEnrollment();
            enrollment.setStudent(student);
            enrollment.setCourse(course);
            enrollment.setSemesterEnrolled(semester);
            enrollment.setCurrentlyTaking(true);
            enrollment.setCompleted(false); // presumably false if you’re just enrolling

            newEnrollments.add(enrollment);
        }
        // 7) Save all new enrollments
        courseEnrollmentRepo.saveAll(newEnrollments);
        // 8) Provide a success message
        redirectAttributes.addFlashAttribute("success",
                "Enrollment successful! You have enrolled in " + newEnrollments.size() + " course(s).");
        // 9) Redirect back to the course selection or some summary page
        return "redirect:/student/selectCourses";
    }


    @GetMapping("/completedCourses")
    public String viewCompletedCourses(Model model, Principal principal) {
        String email = principal.getName();
        Student student = studentRepo.findByEmail(email).orElseThrow(() -> new RuntimeException("Student not found"));
        // Fetch completed enrollments
        List<CourseEnrollment> completedEnrollments = courseEnrollmentService.getCompletedEnrollments(student.getId());
        model.addAttribute("completedEnrollments", completedEnrollments);
        return "completedCourses";
    }
    @GetMapping("/audit")
    public String loadStudentAuditPage(Model model, Authentication authentication) {
        String email = authentication.getName();
        User user = userRepo.findByEmail(email).orElse(null);

        if (user instanceof Student student) {
            model.addAttribute("studentId", student.getStudentId());
            model.addAttribute("studentName", student.getFirstName() + " " + student.getLastName());
        }

        return "studentAudit";
    }

    @GetMapping("/invoice/download")
    public ResponseEntity<byte[]> downloadInvoice(Principal principal) throws Exception {
        String email = principal.getName();
        Student student = studentRepo.findByEmail(email).orElseThrow(() -> new RuntimeException("Student not found"));

        List<CourseEnrollmentDto> enrolledCourses = courseEnrollmentService.getActiveEnrollments(student.getId())
                .stream()
                .map(ce -> new CourseEnrollmentDto(
                        ce.getCourse().getId(),
                        ce.getCourse().getCourseCode(),
                        ce.getCourse().getTitle(),
                        ce.getCourse().getCost()))
                .collect(Collectors.toList());

        double totalDue = enrolledCourses.stream().mapToDouble(CourseEnrollmentDto::getCost).sum();

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

        byte[] pdfBytes = invoicePdfGeneratorService.generateInvoicePdf(invoiceDto);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "invoice.pdf");

        return ResponseEntity.ok().headers(headers).body(pdfBytes);
    }

    @GetMapping("/audit/download")
    public ResponseEntity<byte[]> downloadStudentAudit(Principal principal) throws Exception {
        String email = principal.getName();
        Student student = studentRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        // Build the audit DTO
        StudentFullAuditDto auditDto = studentProgrammeAuditService.getFullAudit(student.getStudentId());

        // Generate PDF
        byte[] pdfBytes = programmeAuditPdfGeneratorService.generateAuditPdf(auditDto);

        // Return PDF as response
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "student_audit.pdf");

        return ResponseEntity.ok().headers(headers).body(pdfBytes);
    }

}