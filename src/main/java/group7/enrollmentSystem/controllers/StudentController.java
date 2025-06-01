package group7.enrollmentSystem.controllers;

import com.itextpdf.text.DocumentException;
import group7.enrollmentSystem.config.CustomExceptions;
import group7.enrollmentSystem.dtos.appDtos.EnrollCourseRequest;
import group7.enrollmentSystem.dtos.classDtos.*;
import group7.enrollmentSystem.dtos.formDtos.CompassionateFormDTO;
import group7.enrollmentSystem.dtos.formDtos.GraduationFormDTO;
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
    private final GraduationApplicationRepo applicationRepo;
    private final FormsService formsService;

    @GetMapping("/enrollment")
    public String enrollment(Model model, Principal principal) {
        EnrollmentState state = enrollmentStateRepo.findById(1L)
                .orElseThrow(() -> new RuntimeException("Enrollment state not found"));

        if (!state.isOpen()) {
            model.addAttribute("pageOpen", false);
            model.addAttribute("restrictionType", "ENROLLMENT_CLOSED");
            model.addAttribute("message", "The course enrollment period has ended<br>Please contact Student Administrative Services for more info");
            return "accessDenied";
        }

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

    @GetMapping("/applicationHistory")
    public String viewApplicationHistory(Model model, Authentication auth) {
        String email = auth.getName();

        // Get Graduation application (only one allowed)
        GraduationApplication gradApp = formsService.getGraduationApplication(email);
        model.addAttribute("graduationApp", gradApp);

        // Get Compassionate/Special/Aegrotat applications (can be multiple)
        List<CompassionateApplication> compassionateApps = formsService.getCompassionateApplications(email);
        model.addAttribute("compassionateApps", compassionateApps);

        return "application_history"; // Adjust view name if needed
    }



    @GetMapping("/graduationApplication")
    public String graduationApplicationForm(Model model, Authentication authentication) {
        boolean canAccess = checkAccess(authentication,
                StudentHoldService.HoldRestrictionType.FORMS_APPLICATION);

        model.addAttribute("pageOpen", canAccess);
        if (!canAccess) {
            StudentHoldViewDto holdStatus = studentHoldService.getStudentHoldDetails(authentication.getName());
            model.addAttribute("restrictionType", "HOLD_RESTRICTION");
            model.addAttribute("message", holdStatus.getHoldMessage());
            return "accessDenied";
        }

        String email = authentication.getName();
        Student student = studentRepo.findByEmail(email)
                .orElseThrow(() -> new CustomExceptions.StudentNotFoundException(email));

        Programme programme = studentProgrammeService.getStudentProgramme(student);
        if (programme == null) {
            throw new RuntimeException("No current programme found for student.");
        }

        GraduationFormDTO form = new GraduationFormDTO();
        form.setProgramme(programme.getProgrammeCode());

        model.addAttribute("student", student);
        model.addAttribute("programme", programme);
        model.addAttribute("graduationForm", form);

        return "forms/graduationForm";
    }


    @PostMapping("/graduation_submit")
    public String submitGraduationForm(@ModelAttribute GraduationFormDTO form,
                                       Authentication authentication,
                                       Model model) {
        String email = authentication.getName();
        try {
            formsService.submitGraduationApplication(email, form);
            model.addAttribute("successMessage", "Application submitted successfully.");
        } catch (RuntimeException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "forms/graduationForm";
        }

        return "redirect:/student/applicationHistory";
    }


    @GetMapping("/compassionateApplication")
    public String compassionateApplicationForm(Model model, Authentication authentication) {
        boolean canAccess = checkAccess(authentication,
                StudentHoldService.HoldRestrictionType.FORMS_APPLICATION);

        model.addAttribute("pageOpen", canAccess);
        if (!canAccess) {
            StudentHoldViewDto holdStatus = studentHoldService.getStudentHoldDetails(authentication.getName());
            model.addAttribute("restrictionType", "HOLD_RESTRICTION");
            model.addAttribute("message", holdStatus.getHoldMessage());
            return "accessDenied";
        }

        String email = authentication.getName();
        Student student = studentRepo.findByEmail(email)
                .orElseThrow(() -> new CustomExceptions.StudentNotFoundException(email));

        // Attach student info and empty form object
        model.addAttribute("student", student);
        model.addAttribute("form", new CompassionateFormDTO());

        return "forms/compassionateForm";
    }
    @PostMapping("/compassionate/submit")
    public String submitCompassionateForm(@ModelAttribute("form") CompassionateFormDTO form,
                                          Authentication authentication,
                                          RedirectAttributes redirectAttributes) {
        String email = authentication.getName();
        try {
            formsService.submitApplication(email, form);
            redirectAttributes.addFlashAttribute("successMessage", "Your application was submitted successfully.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/student/compassionateApplication";
        }

        return "redirect:/student/applicationHistory";
    }




}
