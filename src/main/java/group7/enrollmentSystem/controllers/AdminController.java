package group7.enrollmentSystem.controllers;

import group7.enrollmentSystem.dtos.CourseDto;
import group7.enrollmentSystem.repos.CourseRepo;
import group7.enrollmentSystem.services.CourseService;
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

    @GetMapping("/courses")
    public String getCourses(Model model) {
        model.addAttribute("courses",courseRepo.findAll());
        model.addAttribute("courseDto",new CourseDto());
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

}
