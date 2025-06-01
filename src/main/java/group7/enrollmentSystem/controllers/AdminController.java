package group7.enrollmentSystem.controllers;

import group7.enrollmentSystem.dtos.classDtos.*;
import group7.enrollmentSystem.dtos.serverKtDtos.ProgrammesAndCoursesDto;
import group7.enrollmentSystem.enums.ApplicationStatus;
import group7.enrollmentSystem.models.*;
import group7.enrollmentSystem.repos.*;
import group7.enrollmentSystem.services.CourseEnrollmentService;
import group7.enrollmentSystem.services.CourseProgrammeService;
import group7.enrollmentSystem.services.CourseService;
import group7.enrollmentSystem.services.FormsService;
import group7.enrollmentSystem.services.ProgrammeService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;


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
    private final CourseProgrammeRepo courseProgrammeRepo;
    private final CourseEnrollmentService courseEnrollmentService;
    private final FormsService formsService;

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

    @GetMapping("/holds")
    public String showHoldManagementPage(Model model) {
        return "studentHolds";
    }

    @GetMapping("/holdRestrictions")
    public String showHoldRestrictionsPage(Model model) {
        return "holdRestrictions";
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
            throw new RuntimeException("Programmes not found");
        }
        List<ProgrammesAndCoursesDto> data = new ArrayList<>();
        for(Programme programme : programmes) {
            List<Course> course = courseProgrammeRepo.findAllByProgramme(programme);
            ProgrammesAndCoursesDto dto = new ProgrammesAndCoursesDto(programme, course);
            data.add(dto);
        }
        model.addAttribute("programmes", data);
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
        redirectAttributes.addFlashAttribute("message", "Programme updated successfully.");

        return "redirect:/admin/programmes";
        }
        catch (Exception e) {
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
    @GetMapping("/gradeChangeRequests")
    public String gradeChangeRequests(Model model) {
        List<CourseEnrollment> ces = courseEnrollmentService.getAllGradeChangeRequests();
        model.addAttribute("requests", ces);
        return "gradeChangeRequests";
    }



    @GetMapping("/applications")
    public String viewAllApplications(Model model) {
        List<GraduationApplication> graduationApps = formsService.getAllApplications();
        List<CompassionateApplication> compassionateApps = formsService.getAllCompassionateApplications();

        model.addAttribute("graduationApps", graduationApps);
        model.addAttribute("compassionateApps", compassionateApps);
        return "admin_applications";
    }



    @PostMapping("/updateApplicationStatus")
    public String updateApplicationStatus(@RequestParam Long applicationId,
                                          @RequestParam ApplicationStatus status) {
        formsService.updateApplicationStatus(applicationId, status);
        return "redirect:/admin/applications";
    }
    @PostMapping("/updateOtherApplicationStatus")
    public String updateOtherApplicationStatus(@RequestParam Long applicationId,
                                          @RequestParam ApplicationStatus status) {
        formsService.updateOtherApplicationStatus(applicationId, status);
        return "redirect:/admin/applications";
    }
    @PostMapping("/deleteGraduationApplication")
    public String deleteGraduationApplication(@RequestParam Long applicationId, RedirectAttributes redirectAttributes) {
        try {
            formsService.deleteGraduationApplication(applicationId);
            redirectAttributes.addFlashAttribute("message", "Application deleted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to delete application: " + e.getMessage());
        }
        return "redirect:/admin/applications";
    }
    @PostMapping("/deleteCompassionateApplication")
    public String deleteCompassionateApplication(@RequestParam Long applicationId, RedirectAttributes redirectAttributes) {
        try {
            formsService.deleteCompassionateApplication(applicationId);
            redirectAttributes.addFlashAttribute("message", "Application deleted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to delete application: " + e.getMessage());
        }
        return "redirect:/admin/applications";
    }
    @GetMapping("/applications/export/graduation")
    public void exportGraduationCsv(HttpServletResponse response) throws IOException {
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=graduation_applications.csv");

        PrintWriter writer = response.getWriter();
        formsService.exportGraduationCsv(writer);
    }

    @GetMapping("/applications/export/compassionate")
    public void exportCompassionateCsv(HttpServletResponse response) throws IOException {
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=other_applications.csv");

        PrintWriter writer = response.getWriter();
        formsService.exportCompassionateCsv(writer);
    }


}
