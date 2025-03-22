package group7.enrollmentSystem.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import group7.enrollmentSystem.dtos.classDtos.*;
import group7.enrollmentSystem.models.Course;
import group7.enrollmentSystem.models.User;
import group7.enrollmentSystem.repos.CourseRepo;
import group7.enrollmentSystem.repos.ProgrammeRepo;
import group7.enrollmentSystem.repos.UserRepo;
import group7.enrollmentSystem.services.CourseProgrammeService;
import group7.enrollmentSystem.services.CourseService;
import group7.enrollmentSystem.services.ProgrammeService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {
    private final CourseRepo courseRepo;
    private final CourseService courseService;
    private final CourseRepo coursePrerequisiteRepo;

    private final CourseProgrammeService courseProgrammeService;
    private final ProgrammeRepo programmeRepo;
    private final ProgrammeService programmeService;
    private final UserRepo userRepo;

    @GetMapping("/dashboard")
    public String getAdminPage(Model model, Authentication authentication) {
        String email = authentication.getName();
        User user = userRepo.findByEmail(email).orElse(null);
        model.addAttribute("user", user);
        return "admin";
    }

    @GetMapping("/courses")
    public String getCourses(Model model) {
        List<CourseDto> courseDtos = courseService.getAllCoursesWithProgrammesAndPrereqs();
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
//    @PostMapping("/addPreReqs")
//    public String addPrerequisites(
//            @ModelAttribute CoursePrerequisiteRequest request,
//            RedirectAttributes redirectAttributes) {
//
//        try {
//
//            // Call service function
//            courseService.addPrerequisites(request);
//
//            // Redirect with success message
//            redirectAttributes.addFlashAttribute("message", "Prerequisites added successfully.");
//            return "redirect:/admin/courses"; // Redirect back to courses page
//        } catch (Exception e) {
//            redirectAttributes.addFlashAttribute("error", e.getMessage());
//            return "redirect:/admin/courses"; // Redirect back with error
//        }
//    }



    // Display all programmes
    @GetMapping("/programmes")
    public String getProgrammes(Model model) {
        model.addAttribute("programmes", programmeRepo.findAll());
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
}
//    @PostMapping("/addPreReqs")
//    public String addPrerequisites(@ModelAttribute AddCourseReq requestData, RedirectAttributes redirectAttributes) {
//        try {
//            courseService.addPrerequisites(requestData.getCourseId(), requestData.getPrerequisites());
//            redirectAttributes.addFlashAttribute("message", "Prerequisites added successfully!");
//        } catch (DataIntegrityViolationException e) {
//            redirectAttributes.addFlashAttribute("error", "Duplicate prerequisite detected. This prerequisite is already assigned to the course.");
//        } catch (Exception e) {
//            redirectAttributes.addFlashAttribute("error", "Failed to add prerequisites: " + e.getMessage());
//        }
//        return "redirect:/admin/courses";
//    }