package group7.enrollmentSystem.controllers;

import com.itextpdf.text.DocumentException;
import group7.enrollmentSystem.dtos.appDtos.EnrollCourseRequest;
import group7.enrollmentSystem.dtos.classDtos.CourseEnrollmentDto;
import group7.enrollmentSystem.dtos.classDtos.EnrollmentPageData;
import group7.enrollmentSystem.dtos.classDtos.InvoiceDto;
import group7.enrollmentSystem.dtos.classDtos.StudentFullAuditDto;
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
    private final StudentProgrammeService studentProgrammeService;
    private final EnrollmentStateRepo enrollmentStateRepo;
    private final CourseRepo courseRepo;
    private final CourseEnrollmentRepo courseEnrollmentRepo;
    private final UserRepo userRepo;
    private final InvoicePdfGeneratorService invoicePdfGeneratorService;
    private final StudentService studentService;
    private final StudentProgrammeAuditService auditService;

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

}