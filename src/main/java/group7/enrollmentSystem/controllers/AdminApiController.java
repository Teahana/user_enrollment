package group7.enrollmentSystem.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import group7.enrollmentSystem.dtos.appDtos.LoginResponse;
import group7.enrollmentSystem.dtos.classDtos.CoursePrerequisiteRequest;
import group7.enrollmentSystem.dtos.classDtos.FlatCoursePrerequisiteDTO;
import group7.enrollmentSystem.dtos.classDtos.FlatCoursePrerequisiteRequest;
import group7.enrollmentSystem.dtos.classDtos.GraphicalPrerequisiteNode;
import group7.enrollmentSystem.dtos.interfaceDtos.CourseIdAndCode;
import group7.enrollmentSystem.dtos.interfaceDtos.ProgrammeIdAndCode;
import group7.enrollmentSystem.enums.SpecialPrerequisiteType;
import group7.enrollmentSystem.helpers.JwtService;
import group7.enrollmentSystem.models.Course;
import group7.enrollmentSystem.models.User;
import group7.enrollmentSystem.repos.CourseRepo;
import group7.enrollmentSystem.repos.ProgrammeRepo;
import group7.enrollmentSystem.repos.UserRepo;
import group7.enrollmentSystem.services.CourseProgrammeService;
import group7.enrollmentSystem.services.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminApiController {

    //private final CoursePrerequisiteRepo coursePrerequisiteRepo;
    private final CourseRepo courseRepo;
    private final CourseProgrammeService courseProgrammeService;
    private final CourseService courseService;
    private final ProgrammeRepo programmeRepo;
    private final UserRepo userRepo;
    private final JwtService jwtService;

    @PostMapping("/someEndpoint")
    public ResponseEntity<?> someEndpoint() {
        someFunction();
        return ResponseEntity.ok(Map.of("message", "Success"));
    }
    @PostMapping("/tokenLogin")
    public ResponseEntity<?> tokenLogin(Authentication authentication) {
        System.out.println("Token login request received.");
        User user = userRepo.findByEmail(authentication.getName()).orElseThrow();
        String token = jwtService.generateToken(user, 3600);
        String userType = user.getRoles().contains("ROLE_ADMIN") ? "admin" : "student";
        LoginResponse response = new LoginResponse(
                user.getId(),
                userType,
                token
        );
        return ResponseEntity.ok(response);
    }
    private void someFunction() {
        throw new RuntimeException("SOME FUCKING EXCEPTION");
    }
    @GetMapping("/getSpecialTypes")
    public ResponseEntity<?> getSpecialTypes() {
        return ResponseEntity.ok(Map.of("specialTypes", Arrays.toString(SpecialPrerequisiteType.values())));
    }
    @GetMapping("/getAllCourses")
    public ResponseEntity<?> getAllCourses() {
        List<CourseIdAndCode> courses = courseRepo.findAllBy();
        return ResponseEntity.ok(courses);
    }
    @GetMapping("/getAllProgrammes")
    public ResponseEntity<?> getAllProgrammes() {
        List<ProgrammeIdAndCode> programmes = programmeRepo.findAllBy();
        return ResponseEntity.ok(programmes);
    }
    @PostMapping("/addPreReqs")
    public ResponseEntity<?> addPrerequisites(@RequestBody FlatCoursePrerequisiteRequest request) {
        try{
            System.out.println("Request: "+request);
            courseService.addPrerequisites(request);
            return ResponseEntity.ok(Map.of("message", "Prerequisites added successfully"));
        }
        catch (Exception e){
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
    @PostMapping("/updatePreReqs")
    public ResponseEntity<?> updatePrerequisites(@RequestBody FlatCoursePrerequisiteRequest request) {
        try{
            courseService.updatePrerequisites(request);
            return ResponseEntity.ok(Map.of("message", "Prerequisites updated successfully"));
        }
        catch (Exception e){
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
    @PostMapping("/getPreReqs")
    public ResponseEntity<?> getPreReqs(@RequestBody Map<String,Long> request) {
        try{
            FlatCoursePrerequisiteRequest prerequisites = courseService.getPrerequisitesForCourse(request.get("courseId"));
            return ResponseEntity.ok(Map.of("prerequisites", prerequisites));
        }
        catch (Exception e){
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    @PostMapping("/getPreReqTree")
    public ResponseEntity<?> getPrereqTree(@RequestBody Map<String, Long> request) {
        try {
            Long courseId = request.get("courseId");
            GraphicalPrerequisiteNode root = courseService.buildPrerequisiteTree(courseId);
            return ResponseEntity.ok(root);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/getCoursesExcept/{id}")
    public ResponseEntity<?> getCoursePrerequisites(@PathVariable Long id) {
        List<Course> courses = courseRepo.findByIdNot(id);
        List<String> courseCodes = courses.stream().map(Course::getCourseCode).toList();
        return ResponseEntity.ok(Map.of("courseCodes", courseCodes));
    }

    // Fetch courses for programmess
    @GetMapping("/getCoursesNotLinkedToProgramme")
    public ResponseEntity<List<Course>> getCoursesNotLinkedToProgramme(@RequestParam String programmeCode) {
        List<Course> courses = courseProgrammeService.getCoursesNotLinkedToProgramme(programmeCode);
        return ResponseEntity.ok(courses);
    }

    // This is to remove a course from a programme useddd by admin
    @DeleteMapping("/removeCourseFromProgramme")
    public ResponseEntity<Void> removeCourseFromProgramme(
            @RequestParam String courseCode,
            @RequestParam String programmeCode) {
        courseProgrammeService.removeCourseFromProgramme(courseCode, programmeCode);
        return ResponseEntity.ok().build();
    }
}
//    @PostMapping("/addPreReqs")
//    public ResponseEntity<?> addPrerequisites(@RequestBody AddCourseReq requestData) {
//        try {
//            courseService.addPrerequisites(requestData.getCourseId(), requestData.getPrerequisites());
//            return ResponseEntity.ok(Map.of("message", "Successfully added prerequisites"));
//        } catch (Exception e) {
//            System.out.println("Exception: "+ e.getMessage());
//            return ResponseEntity.badRequest().body("Failed to add prerequisites");
//        }
//    }