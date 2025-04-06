package group7.enrollmentSystem.controllers;

import group7.enrollmentSystem.dtos.classDtos.*;
import group7.enrollmentSystem.models.Course;
import group7.enrollmentSystem.models.EnrollmentState;
import group7.enrollmentSystem.models.Programme;
import group7.enrollmentSystem.models.User;
import group7.enrollmentSystem.repos.CourseRepo;
import group7.enrollmentSystem.repos.EnrollmentStateRepo;
import group7.enrollmentSystem.repos.ProgrammeRepo;
import group7.enrollmentSystem.repos.UserRepo;
import group7.enrollmentSystem.services.CourseProgrammeService;
import group7.enrollmentSystem.services.CourseService;
import group7.enrollmentSystem.services.ProgrammeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {
    private final CourseRepo courseRepo;
    private final CourseService courseService;
    private final EnrollmentStateRepo enrollmentStateRepo;

    private final CourseProgrammeService courseProgrammeService;
    private final ProgrammeRepo programmeRepo;
    private final ProgrammeService programmeService;
    private final UserRepo userRepo;

    @GetMapping("/dashboard")
    public String getAdminPage(Model model, Authentication authentication) {
        String email = authentication.getName();
        User user = userRepo.findByEmail(email).orElse(null);
        model.addAttribute("user", user);

        // Fetch the enrollment state
        EnrollmentState enrollmentState = enrollmentStateRepo.findById(1L).orElseThrow(() -> new RuntimeException("Enrollment state not found"));
        model.addAttribute("enrollmentState", enrollmentState);

        return "admin";
    }

    @GetMapping("/courses")
    public String getCourses(Model model) {
        List<CourseDto> courseDtos = courseService.getAllCoursesWithProgrammesAndPrereqs();
        courseDtos.sort(Comparator.comparing(CourseDto::getLevel));
        model.addAttribute("courses", courseDtos);
        return "courses";
    }
    @PostMapping("/confirmPreReqAdd")
    public String confirmPreReqAdd(
            @RequestParam("successStatus") String successStatus,
            @RequestParam("responseMessage") String responseMessage,
            RedirectAttributes redirectAttributes) {

        if ("true".equals(successStatus)) {
            redirectAttributes.addFlashAttribute("message", responseMessage);
        } else {
            redirectAttributes.addFlashAttribute("error", responseMessage);
        }

        return "redirect:/admin/courses";
    }
    @PostMapping("/confirmPreReqEdit")
    public String confirmPreReqEdit(
            @RequestParam("successStatus") String successStatus,
            @RequestParam("responseMessage") String responseMessage,
            RedirectAttributes redirectAttributes) {

        if ("true".equals(successStatus)) {
            redirectAttributes.addFlashAttribute("message", responseMessage);
        } else {
            redirectAttributes.addFlashAttribute("error", responseMessage);
        }

        return "redirect:/admin/courses";
    }
    @GetMapping("/deletePreReqs/{courseId}")
    public String deletePreReqs(@PathVariable("courseId") Long courseId, RedirectAttributes redirectAttributes) {
        try {
            Course course = courseRepo.findById(courseId).orElse(null);
            System.out.println("Course: "+course);
            courseService.deletePrerequisites(course);
            redirectAttributes.addFlashAttribute("message", "Prerequisites deleted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to delete prerequisites: " + e.getMessage());
        }
        return "redirect:/admin/courses";
    }
    @PostMapping("/addCourse")
    public String addCourse(@ModelAttribute("courseDto") CourseDto courseDto, RedirectAttributes redirectAttributes) {
        try{
            courseService.addCourse(courseDto);
            redirectAttributes.addFlashAttribute("message","Course added");
            return "redirect:/admin/courses";
        }
        catch(Exception e){
            redirectAttributes.addFlashAttribute("error",e.getMessage());
            return "redirect:/admin/courses";
        }
    }

    @PostMapping("/updateCourse")
    public String updateCourse(@ModelAttribute CourseDto dto, RedirectAttributes redirectAttributes) {
        try {
            courseService.updateCourse(dto);
            redirectAttributes.addFlashAttribute("message", "Course updated successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/courses";
    }

    // Display all programmes
    @GetMapping("/programmes")
    public String getProgrammes(Model model) {
        List<Programme> programmes = programmeService.getAllProgrammes();
        if (programmes == null) {
            return "redirect:/admin/dashboard";
        }
        model.addAttribute("programmes", programmes);
        model.addAttribute("programmeDto", new ProgrammeDto());
        return "programmes";
    }

    // Add a new programme
    @PostMapping("/addProgramme")
    public String addProgramme(@ModelAttribute("programmeDto") ProgrammeDto programmeDto, RedirectAttributes redirectAttributes) {
        try {
            programmeService.addProgramme(programmeDto);
            redirectAttributes.addFlashAttribute("message", "Programme added");
            return "redirect:/admin/programmes";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/programmes";
        }
    }

    @PostMapping("/deleteProgramme")
    public String deleteProgramme (@ModelAttribute ProgrammeDto dto, RedirectAttributes redirectAttributes)
    {

        try {
            String programmeCode = dto.getProgrammeCode();
            programmeService.deleteProgramme(programmeCode);
            redirectAttributes.addFlashAttribute("message", "Programme Deleted successfully.");
            return "redirect:/admin/programmes";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/programmes";
        }
    }

    @PostMapping("/updateProgramme")
    public String updateProgramme(@ModelAttribute ProgrammeDto dto, RedirectAttributes redirectAttributes) {

        try
        {
        String programmeCode = dto.getProgrammeCode();
        String name = dto.getName();
        String faculty = dto.getFaculty();

        programmeService.updateProgramme(programmeCode, name, faculty);
        return "redirect:/admin/programmes";
    } catch (Exception e) {
        redirectAttributes.addFlashAttribute("error", e.getMessage());
        return "redirect:/admin/programmes";
    }
    }

    // Link a course to a programme
    @PostMapping("/linkCourseToProgramme")
    public String linkCourseToProgramme(
            @RequestParam String courseCode,
            @RequestParam String programmeCode,
            RedirectAttributes redirectAttributes) {
        try {
            courseProgrammeService.linkCourseToProgramme(courseCode, programmeCode);
            redirectAttributes.addFlashAttribute("message", "Course linked to Programme");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/programmes";
    }

    //---------Control for Admin to turn off/on students' access to enrollment page-----------------
    @PostMapping("/toggleEnrollment")
    public String toggleEnrollment(RedirectAttributes redirectAttributes) {
        EnrollmentState enrollmentState = enrollmentStateRepo.findById(1L).orElseThrow(() -> new RuntimeException("Enrollment state not found"));
        enrollmentState.setOpen(!enrollmentState.isOpen());
        enrollmentStateRepo.save(enrollmentState);

        String message = enrollmentState.isOpen() ? "Student Course Enrollment is now open." : "Student Course Enrollment is now closed.";
        redirectAttributes.addFlashAttribute("message", message);

        return "redirect:/admin/dashboard";
    }
    @PostMapping("/toggleSemester")
    public String toggleSemester(RedirectAttributes redirectAttributes) {
        EnrollmentState enrollmentState = enrollmentStateRepo.findById(1L)
                .orElseThrow(() -> new RuntimeException("Enrollment state not found"));

        enrollmentState.setSemesterOne(!enrollmentState.isSemesterOne());
        enrollmentStateRepo.save(enrollmentState);

        String message = enrollmentState.isSemesterOne()
                ? "Switched to Semester 1."
                : "Switched to Semester 2.";
        redirectAttributes.addFlashAttribute("message1", message);  // Note: 'message1' used in your HTML

        return "redirect:/admin/dashboard";
    }

}
