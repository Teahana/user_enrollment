package group7.enrollmentSystem.controllers;

import group7.enrollmentSystem.models.*;
import group7.enrollmentSystem.repos.CourseProgrammeRepo;
import group7.enrollmentSystem.repos.EnrollmentStatusRepo;
import group7.enrollmentSystem.repos.StudentRepo;
import group7.enrollmentSystem.services.CourseEnrollmentService;
import group7.enrollmentSystem.services.CourseService;
import group7.enrollmentSystem.services.StudentProgrammeService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/student")
@RequiredArgsConstructor
public class StudentController {

    private final CourseEnrollmentService courseEnrollmentService;
    private final StudentRepo studentRepo;
    private final CourseProgrammeRepo courseProgrammeRepo;
    private final StudentProgrammeService studentProgrammeService;
    private final CourseService courseService;
    private final EnrollmentStatusRepo enrollmentStatusRepo;


    @GetMapping("/enrollment/{semester}")
    public String enrollment(@PathVariable("semester") int semester, Model model, Principal principal) {
        EnrollmentState enrollmentState = enrollmentStatusRepo.findById(1L).orElseThrow(() -> new RuntimeException("Enrollment state not found"));
        if(enrollmentState.isOpen()){
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
            model.addAttribute("pageOpen",true);
        }
        else{
            model.addAttribute("pageOpen",false);
            model.addAttribute("message","The course enrollment period has ended<br>Please contact Student Administrative Services for more info");
        }
        return "enrollment";
    }

    @PostMapping("/cancelEnrollment/{id}/{semester}")
    public String cancelEnrollment(@PathVariable Long id, @PathVariable int semester, RedirectAttributes redirectAttributes) {
        courseEnrollmentService.cancelEnrollment(id);
        redirectAttributes.addFlashAttribute("success", "Enrollment cancelled successfully.");
        return "redirect:/student/enrollment/" + semester;
    }

    @PostMapping("/activateEnrollment/{id}/{semester}")
    public String activateEnrollment(@PathVariable Long id, @PathVariable int semester,  Principal principal, RedirectAttributes redirectAttributes) {

        String email = principal.getName();
        Student student = studentRepo.findByEmail(email).orElseThrow(() -> new RuntimeException("Student not found"));

        int activeEnrollmentsCount = courseEnrollmentService.getActiveEnrollmentsBySemester(student.getId(), semester).size();

        if (activeEnrollmentsCount >= 4) {
            redirectAttributes.addFlashAttribute("error", "You cannot activate this enrollment because you already have four active courses for this semester.");
            return "redirect:/student/enrollment/" + semester;
        }

        courseEnrollmentService.activateEnrollment(id);
        redirectAttributes.addFlashAttribute("success", "Enrollment activated successfully.");
        return "redirect:/student/enrollment/" + semester;
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
            return "redirect:/student/selectCourses/" + semester;
        }

        // Check active enrollments
        int activeEnrollmentsCount = courseEnrollmentService.getActiveEnrollmentsBySemester(student.getId(), semester).size();

        if (activeEnrollmentsCount + selectedCourseIds.size() > 4) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "You cannot enroll in more than four courses per semester. " +
                            "Currently enrolled: " + activeEnrollmentsCount
            );
            return "redirect:/student/selectCourses/" + semester;
        }

        try {
            courseEnrollmentService.enrollStudentInCourses(student.getId(), selectedCourseIds, semester);
            redirectAttributes.addFlashAttribute("success", "Courses have been enrolled successfully for Semester " + semester);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/student/selectCourses/" + semester;
        }
         return "redirect:/student/enrollment/" + semester;
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