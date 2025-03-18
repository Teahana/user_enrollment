package group7.enrollmentSystem.controllers;

import group7.enrollmentSystem.dtos.CourseDto;
import group7.enrollmentSystem.dtos.ProgrammeDto;
import group7.enrollmentSystem.repos.CourseRepo;
import group7.enrollmentSystem.repos.ProgrammeRepo;
import group7.enrollmentSystem.services.CourseProgrammeService;
import group7.enrollmentSystem.services.CourseService;
import group7.enrollmentSystem.services.ProgrammeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

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


    @GetMapping("/courses")
    public String getCourses(Model model) {
        List<CourseDto> courseDtos = courseService.getAllCoursesWithProgrammesAndPrereqs();
        model.addAttribute("courses", courseDtos);
        return "courses";
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
    @PostMapping("/addPrerequisite")
    public String addPrerequisite(@RequestParam Long courseId,
                                  @RequestParam List<String> prerequisites,
                                  RedirectAttributes redirectAttributes) {
        try {
            courseService.addPrerequisites(courseId, prerequisites);
            redirectAttributes.addFlashAttribute("message", "Prerequisites added successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/courses";
    }

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
