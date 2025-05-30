package group7.enrollmentSystem.controllers;

import com.itextpdf.text.DocumentException;
import group7.enrollmentSystem.config.CustomExceptions;
import group7.enrollmentSystem.dtos.appDtos.EnrollCourseRequest;
import group7.enrollmentSystem.dtos.classDtos.*;
import group7.enrollmentSystem.enums.OnHoldTypes;
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

import java.io.IOException;
import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/student")
@RequiredArgsConstructor
public class StudentController {

    private final CourseEnrollmentService courseEnrollmentService;
    private final StudentRepo studentRepo;
    private final StudentProgrammeService studentProgrammeService;
    private final EnrollmentStateRepo enrollmentStateRepo;
    private final CourseRepo courseRepo;
    private final CourseEnrollmentRepo courseEnrollmentRepo;
    private final UserRepo userRepo;
    private final InvoicePdfGeneratorService invoicePdfGeneratorService;
    private final StudentService studentService;
    private final StudentProgrammeAuditService auditService;
    private final StudentHoldService studentHoldService;

    @GetMapping("/enrollment")
    public String enrollment(Model model, Principal principal) {
        EnrollmentState state = enrollmentStateRepo.findById(1L)
                .orElseThrow(() -> new RuntimeException("Enrollment state not found"));

//        if (!state.isOpen()) {
//            model.addAttribute("pageOpen", false);
//            model.addAttribute("restrictionType", "ENROLLMENT_CLOSED");
//            model.addAttribute("message", "The course enrollment period has ended<br>Please contact Student Administrative Services for more info");
//            return "accessDenied";
//        }

        boolean canAccess = checkAccess(principal,
                StudentHoldService.HoldRestrictionType.COURSE_ENROLLMENT);

        model.addAttribute("pageOpen", canAccess);
        if (!canAccess) {
            StudentHoldViewDto holdStatus = studentHoldService.getStudentHoldDetails(principal.getName());
            model.addAttribute("restrictionType", "HOLD_RESTRICTION");
            model.addAttribute("message", holdStatus.getHoldMessage());
            return "accessDenied";
        }

        String email = principal.getName();
        Student student = studentRepo.findByEmail(email).orElseThrow();
        Programme programme = studentProgrammeService.getStudentProgramme(student);

        int semester = state.isSemesterOne() ? 1 : 2;
        List<CourseEnrollmentDto> eligibleCourses = studentService.getEligibleCourses(principal.getName());
        List<CourseEnrollmentDto> enrolledCourses = studentService.getEnrolledCourses(student);

        model.addAttribute("student", student);
        model.addAttribute("programme", programme.getName());
        model.addAttribute("semester", semester);
        model.addAttribute("pageOpen", true);
        model.addAttribute("activeEnrollments", enrolledCourses);
        model.addAttribute("eligibleCourses", eligibleCourses);

        return "enrollment";
    }
    @PostMapping("/payCourse/{id}")
    public String payCourse(@PathVariable(value = "id") Long courseId, RedirectAttributes redirectAttributes, Principal principal) {
        studentService.payCourse(courseId, principal.getName());
        redirectAttributes.addFlashAttribute("successMessage", "Payment successful!");
        return "redirect:/student/enrollment";
    }
    @PostMapping("/completeCourse/{id}")
    public String completeCourse(@PathVariable(value = "id") Long courseId, Model model, Principal principal) {
        studentService.completeCourse(courseId, principal.getName());
        model.addAttribute("successMessage", "Course completed successfully!");
        return "redirect:/student/enrollment";
    }
    @PostMapping("/failCourse/{id}")
    public String failCourse(@PathVariable(value = "id") Long courseId, Model model, Principal principal) {
        studentService.failCourse(courseId, principal.getName());
        model.addAttribute("successMessage", "Course Failed successfully!");
        return "redirect:/student/enrollment";
    }
    @PostMapping("/cancelEnrollment/{id}")
    public String cancelEnrollment(@PathVariable Long id, Principal principal, RedirectAttributes redirectAttributes) {
        try {
            Student student = studentRepo.findByEmail(principal.getName()).orElseThrow();
            studentService.cancelCourse(id,student.getId());
            redirectAttributes.addFlashAttribute("successMessage", "Course enrollment cancelled successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/student/enrollment";
    }
    @PostMapping("/enrollCourses")
    public String enrollCourses(@RequestParam(value = "selectedCourses", required = false) List<String> selectedCourseCodes,
                                Principal principal,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        try{
            Student student = studentRepo.findByEmail(principal.getName())
                    .orElseThrow(() -> new RuntimeException("Student not found"));

            EnrollCourseRequest request = new EnrollCourseRequest(selectedCourseCodes, student.getId());
            studentService.enrollStudent(request);
            redirectAttributes.addFlashAttribute("successMessage", "Courses enrolled successfully!");
            return "redirect:/student/enrollment";
        }
        catch(Exception e){
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/student/enrollment";
        }

    }

    @GetMapping("/completedCourses")
    public String viewCompletedCourses(Model model, Principal principal) {
        boolean canAccess = checkAccess(principal,
                StudentHoldService.HoldRestrictionType.VIEW_COMPLETED_COURSES);

        model.addAttribute("pageOpen", canAccess);
        if (!canAccess) {
            StudentHoldViewDto holdStatus = studentHoldService.getStudentHoldDetails(principal.getName());
            model.addAttribute("restrictionType", "HOLD_RESTRICTION");
            model.addAttribute("message", holdStatus.getHoldMessage());
            return "accessDenied";
        }

        String email = principal.getName();
        Student student = studentRepo.findByEmail(email).orElseThrow(() -> new RuntimeException("Student not found"));

        // Fetch completed enrollments
        List<CourseEnrollment> completedEnrollments = courseEnrollmentService.getCompletedEnrollmentsWithHighestGrade(student.getId());
        model.addAttribute("completedEnrollments", completedEnrollments);
        return "completedCourses";
    }
    @GetMapping("/requestGradeChange/{enrollmentId}")
    public String requestGradeChange(@PathVariable Long enrollmentId, Principal principal, RedirectAttributes redirectAttributes) {
        studentService.requestGradeChange(enrollmentId, principal.getName());
        redirectAttributes.addFlashAttribute("success", "Grades requested successfully!");
        return "redirect:/student/completedCourses";
    }
    @GetMapping("/audit")
    public String loadStudentAuditPage(Model model, Authentication authentication) {
        boolean canAccess = checkAccess(authentication,
                StudentHoldService.HoldRestrictionType.STUDENT_AUDIT);

        model.addAttribute("pageOpen", canAccess);
        if (!canAccess) {
            StudentHoldViewDto holdStatus = studentHoldService.getStudentHoldDetails(authentication.getName());
            model.addAttribute("restrictionType", "HOLD_RESTRICTION");
            model.addAttribute("message", holdStatus.getHoldMessage());
            return "accessDenied";
        }

        String email = authentication.getName();
        User user = userRepo.findByEmail(email).orElse(null);

        if (user instanceof Student student) {
            StudentFullAuditDto auditDto = auditService.getFullAudit(student.getStudentId());
            model.addAttribute("auditData", auditDto);
        }

        return "studentAudit"; // this maps to studentAudit.html in templates folder
    }
    @GetMapping("/invoice/download")
    public ResponseEntity<byte[]> downloadInvoice(Principal principal) throws DocumentException, IOException {
        byte[] pdfBytes = studentService.generateInvoicePdfForStudent(principal.getName());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "invoice.pdf");

        return ResponseEntity.ok().headers(headers).body(pdfBytes);
    }

    private boolean checkAccess(Principal principal, StudentHoldService.HoldRestrictionType restrictionType) {
        try {
            studentHoldService.checkAccess(principal.getName(), restrictionType);
            return true;
        } catch (CustomExceptions.StudentOnHoldException e) {
            return false;
        }
    }
    @GetMapping("/viewHolds")
    public String viewHolds(Authentication authentication, Model model) {
        Student student = studentRepo.findByEmail(authentication.getName())
                .orElseThrow(() -> new CustomExceptions.StudentNotFoundException(authentication.getName()));

        StudentHoldViewDto holdStatus = studentHoldService.getStudentHoldDetails(authentication.getName());
        model.addAttribute("holdStatus", holdStatus);
        return "viewHolds";
    }

}
