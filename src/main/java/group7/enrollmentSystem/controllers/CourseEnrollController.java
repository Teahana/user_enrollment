package group7.enrollmentSystem.controllers;

import group7.enrollmentSystem.models.CourseProgramme;
import group7.enrollmentSystem.models.CourseEnrollment;
import group7.enrollmentSystem.models.Student;
import group7.enrollmentSystem.models.StudentProgramme;
import group7.enrollmentSystem.repos.CourseProgrammeRepo;
import group7.enrollmentSystem.repos.StudentRepo;
import group7.enrollmentSystem.services.CourseEnrollmentService;
import group7.enrollmentSystem.services.CourseService;
import group7.enrollmentSystem.services.StudentProgrammeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/courseEnroll")
@RequiredArgsConstructor
public class CourseEnrollController {

    private final CourseEnrollmentService courseEnrollmentService;
    private final StudentRepo studentRepo;
    private final CourseProgrammeRepo courseProgrammeRepo;
    private final StudentProgrammeService studentProgrammeService;
    private final CourseService courseService;

    @GetMapping("/enrollment/{semester}")
    public String enrollment(@PathVariable("semester") int semester, Model model, Principal principal) {
        String email = principal.getName();
        Student student = studentRepo.findByEmail(email).orElseThrow(() -> new RuntimeException("Student not found"));
        StudentProgramme programme = studentProgrammeService.getAllStudentProgrammes().stream()
                .filter(StudentProgramme::isCurrentProgramme)
                .filter(sp -> sp.getStudent().equals(student))
                .findFirst().orElseThrow(() -> new RuntimeException("No active programme found."));

        List<CourseEnrollment> activeEnrollments = courseEnrollmentService.getActiveEnrollmentsBySemester(student.getId(), semester);
        List<CourseEnrollment> canceledEnrollments = courseEnrollmentService.getCanceledEnrollmentsBySemester(student.getId(), semester);

        model.addAttribute("student", student);
        model.addAttribute("programme", programme.getProgramme());
        model.addAttribute("activeEnrollments", activeEnrollments);
        model.addAttribute("canceledEnrollments", canceledEnrollments);
        model.addAttribute("semester", semester);

        return "enrollment";
    }

    @PostMapping("/cancelEnrollment/{id}/{semester}")
    public String cancelEnrollment(@PathVariable Long id, @PathVariable int semester, RedirectAttributes redirectAttributes) {
        courseEnrollmentService.cancelEnrollment(id);
        redirectAttributes.addFlashAttribute("success", "Enrollment cancelled successfully.");
        return "redirect:/courseEnroll/enrollment/" + semester;
    }

    @PostMapping("/activateEnrollment/{id}/{semester}")
    public String activateEnrollment(@PathVariable Long id, @PathVariable int semester,  Principal principal, RedirectAttributes redirectAttributes) {

        String email = principal.getName();
        Student student = studentRepo.findByEmail(email).orElseThrow(() -> new RuntimeException("Student not found"));

        int activeEnrollmentsCount = courseEnrollmentService.getActiveEnrollmentsBySemester(student.getId(), semester).size();

        if (activeEnrollmentsCount >= 4) {
            redirectAttributes.addFlashAttribute("error", "You cannot activate this enrollment because you already have four active courses for this semester.");
            return "redirect:/courseEnroll/enrollment/" + semester;
        }

        courseEnrollmentService.activateEnrollment(id);
        redirectAttributes.addFlashAttribute("success", "Enrollment activated successfully.");
        return "redirect:/courseEnroll/enrollment/" + semester;
    }

    @GetMapping("/selectCourses/{semester}")
    public String selectCourses(@PathVariable("semester") int semester, Model model, Principal principal) {
        String email = principal.getName();
        Student student = studentRepo.findByEmail(email).orElseThrow();

        // Fetch available courses for the semester based on the student's current programme
        List<CourseProgramme> courses = courseEnrollmentService.getAvailableCoursesForSemester(student.getId(), semester);

        model.addAttribute("courses", courses);
        model.addAttribute("courseService", courseService);
        model.addAttribute("semester", semester);
        return "courseEnroll";
    }

    @PostMapping("/enrollCourses/{semester}")
    public String enrollCourses(@PathVariable("semester") int semester,
                                @RequestParam(value = "selectedCourses", required = false) List<Long> selectedCourseIds,
                                Principal principal,
                                RedirectAttributes redirectAttributes) {

        String email = principal.getName();
        Student student = studentRepo.findByEmail(email).orElseThrow(() -> new RuntimeException("Student not found"));

        // Check if no courses are selected
        if (selectedCourseIds == null || selectedCourseIds.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Please select a course");
            return "redirect:/courseEnroll/selectCourses/" + semester;
        }

        // Check active enrollments
        int activeEnrollmentsCount = courseEnrollmentService.getActiveEnrollmentsBySemester(student.getId(), semester).size();

        if (activeEnrollmentsCount + selectedCourseIds.size() > 4) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "You cannot enroll in more than four courses per semester. " +
                            "Currently enrolled: " + activeEnrollmentsCount
            );
            return "redirect:/courseEnroll/selectCourses/" + semester;
        }

        try {
            courseEnrollmentService.enrollStudentInCourses(student.getId(), selectedCourseIds, semester);
            redirectAttributes.addFlashAttribute("success", "Courses have been enrolled successfully for Semester " + semester);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/courseEnroll/selectCourses/" + semester;
        }
         return "redirect:/courseEnroll/enrollment/" + semester;
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
}