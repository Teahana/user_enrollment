package group7.enrollmentSystem.controllers;

import group7.enrollmentSystem.dtos.classDtos.CourseDto;
import group7.enrollmentSystem.dtos.classDtos.ProgrammeDto;
import group7.enrollmentSystem.models.*;
import group7.enrollmentSystem.services.CoursePrerequisiteService;
import group7.enrollmentSystem.services.CourseProgrammeService;
import group7.enrollmentSystem.services.CourseService;
import group7.enrollmentSystem.services.ProgrammeService;
//import group7.enrollmentSystem.services.StudentProgrammeService;
import group7.enrollmentSystem.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiController {

    private final UserService userService;

    private final CourseService courseService;
    private final ProgrammeService programmeService;

    private final CourseProgrammeService courseProgrammeService;
   // private final StudentProgrammeService studentProgrammeService;
    private final CoursePrerequisiteService coursePrerequisiteService;


    /*@PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody HashMap<String, String> data) {
        String email = data.get("email");
        String password = data.get("password");
        userService.save(email, password);
        HashMap<String, String> response = new HashMap<>();
        response.put("message", "User registered successfully");
        return ResponseEntity.ok(response);
    }*/

   @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody HashMap<String, String> data) {
        try {
            String email = data.get("email");
            String password = data.get("password");
            String firstName = data.get("firstName");
            String lastName = data.get("lastName");
            String role = data.get("role");

            // Extract student-specific data if the registration is for a student
            if ("ROLE_STUDENT".equalsIgnoreCase(role)) {
                Student student = new Student();
                student.setStudentId(data.get("studentId"));
                student.setFirstName(data.get("firstName"));
                student.setLastName(data.get("lastName"));
                student.setPhoneNumber(data.get("phoneNumber"));
                student.setAddress(data.get("address"));

                userService.registerUser(email, password, firstName, lastName, role, student);
            } else if ("ROLE_ADMIN".equalsIgnoreCase(role)) {
                userService.registerUser(email, password, firstName, lastName, role, null);
            } else {
                throw new IllegalArgumentException("Invalid role provided");
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Registration successful.");

            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch(Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }

//--------------------------------------------------------------------------------------------
    //**Programme CRUD**//

    @PostMapping("/addCourse")
    public ResponseEntity<?> courseRegister(@RequestBody CourseDto courseDto) {
        courseService.saveCourse(
                courseDto.getTitle(),
                courseDto.getCourseCode(),
                courseDto.getDescription(),
                courseDto.getCreditPoints(),
                courseDto.getLevel(),
                courseDto.isOfferedSem1(),
                courseDto.isOfferedSem2()
        );

        return ResponseEntity.ok().body(new HashMap<>() {{
            put("message", "Course Added");
        }});
    }

    @GetMapping("/courses")
    public ResponseEntity<List<Course>> getAllCourses() {
        return ResponseEntity.ok(courseService.getAllCourses());
    }

    @GetMapping("/courses/{courseCode}")
    public ResponseEntity<Course> getCourseByCode(@PathVariable String courseCode) {
        Optional<Course> course = courseService.getCourseByCode(courseCode);
        return course.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/courses/{courseCode}")
    public ResponseEntity<?> updateCourse(@PathVariable String courseCode, @RequestBody CourseDto courseDto) {
        courseService.updateCourse(
                courseCode,
                courseDto.getTitle(),
                courseDto.getDescription(),
                courseDto.getCreditPoints(),
                courseDto.getLevel(),
                courseDto.isOfferedSem1(),
                courseDto.isOfferedSem2()
        );
        return ResponseEntity.ok().body(new HashMap<>() {{
            put("message", "Course updated");
        }});
    }

    @DeleteMapping("/courses/{courseCode}")
    public ResponseEntity<?> deleteCourse(@PathVariable String courseCode) {
        courseService.deleteCourse(courseCode);
        return ResponseEntity.ok().body(new HashMap<>() {{
            put("message", "Course deleted");
        }});
    }

//--------------------------------------------------------------------------------------------
    //**Programme CRUD**//

    @PostMapping("/addProgramme")
    public ResponseEntity<?> progRegister(@RequestBody ProgrammeDto programmeDto) {
        programmeService.saveProgramme(
                programmeDto.getProgrammeCode(),
                programmeDto.getName(),
                programmeDto.getFaculty()
        );
        return ResponseEntity.ok().body(new HashMap<>() {{
            put("message", "Programme Added");
        }});
    }

    @GetMapping("/programmes")
    public ResponseEntity<List<Programme>> getAllProgrammes() {
        return ResponseEntity.ok(programmeService.getAllProgrammes());
    }

    @GetMapping("/programmes/{programmeCode}")
    public ResponseEntity<Programme> getProgrammeByCode(@PathVariable String programmeCode) {
        Optional<Programme> programme = programmeService.getProgrammeByCode(programmeCode);
        return programme.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/programmes/{programmeCode}")
    public ResponseEntity<?> updateProgramme(@PathVariable String programmeCode, @RequestBody ProgrammeDto programmeDto) {
        programmeService.updateProgramme(
                programmeCode,
                programmeDto.getName(),
                programmeDto.getFaculty()
        );
        return ResponseEntity.ok().body(new HashMap<>() {{
            put("message", "Programme updated");
        }});
    }

    @DeleteMapping("/programmes/{programmeCode}")
    public ResponseEntity<?> deleteProgramme(@PathVariable String programmeCode) {
        programmeService.deleteProgramme(programmeCode);
        return ResponseEntity.ok().body(new HashMap<>() {{
            put("message", "Programme deleted");
        }});
    }

//--------------------------------------------------------------------------------------------
    //**CourseProgram CRUD**//
    @PostMapping("/courseProgramme")
    public ResponseEntity<?> saveCourseProgramme(@RequestBody HashMap<String, String> data) {
        String courseCode = data.get("courseCode");
        String programmeCode = data.get("programmeCode");
        courseProgrammeService.saveCourseProgramme(courseCode, programmeCode);
        return ResponseEntity.ok().body(new HashMap<>() {{
            put("message", "CourseProgramme added");
        }});
    }

    @GetMapping("/courseProgrammes")
    public ResponseEntity<List<CourseProgramme>> getAllCourseProgrammes() {
        return ResponseEntity.ok(courseProgrammeService.getAllCourseProgrammes());
    }

    @GetMapping("/courseProgrammes/{id}")
    public ResponseEntity<CourseProgramme> getCourseProgrammeById(@PathVariable Long id) {
        Optional<CourseProgramme> courseProgramme = courseProgrammeService.getCourseProgrammeById(id);
        return courseProgramme.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/courseProgrammes/{id}")
    public ResponseEntity<?> updateCourseProgramme(@PathVariable Long id, @RequestBody HashMap<String, Long> data) {
        Long courseId = data.get("courseId");
        Long programmeId = data.get("programmeId");
        courseProgrammeService.updateCourseProgramme(id, courseId, programmeId);
        return ResponseEntity.ok().body(new HashMap<>() {{
            put("message", "CourseProgramme updated");
        }});
    }

    @DeleteMapping("/courseProgrammes/{id}")
    public ResponseEntity<?> deleteCourseProgramme(@PathVariable Long id) {
        courseProgrammeService.deleteCourseProgramme(id);
        return ResponseEntity.ok().body(new HashMap<>() {{
            put("message", "CourseProgramme deleted");
        }});
    }
//--------------------------------------------------------------------------------------------
    //**CoursePrerequisite CRUD**//
    @PostMapping("/coursePrerequisite")
    public ResponseEntity<?> saveCoursePrerequisite(@RequestBody HashMap<String, String> data) {
        String courseCode = data.get("courseCode");
        String prerequisiteCode = data.get("prerequisiteCode");
        coursePrerequisiteService.saveCoursePrerequisite(courseCode, prerequisiteCode);
        return ResponseEntity.ok().body(new HashMap<>() {{
            put("message", "CoursePrerequisite added");
        }});
    }

    @GetMapping("/coursePrerequisites")
    public ResponseEntity<List<CoursePrerequisite>> getAllCoursePrerequisites() {
        return ResponseEntity.ok(coursePrerequisiteService.getAllCoursePrerequisites());
    }

    @GetMapping("/coursePrerequisites/{id}")
    public ResponseEntity<CoursePrerequisite> getCoursePrerequisiteById(@PathVariable Long id) {
        Optional<CoursePrerequisite> coursePrerequisite = coursePrerequisiteService.getCoursePrerequisiteById(id);
        return coursePrerequisite.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/coursePrerequisites/{id}")
    public ResponseEntity<?> updateCoursePrerequisite(@PathVariable Long id, @RequestBody HashMap<String, Long> data) {
        Long courseId = data.get("courseId");
        Long prerequisiteId = data.get("prerequisiteId");
        coursePrerequisiteService.updateCoursePrerequisite(id, courseId, prerequisiteId);
        return ResponseEntity.ok().body(new HashMap<>() {{
            put("message", "CoursePrerequisite updated");
        }});
    }

    @DeleteMapping("/coursePrerequisites/{id}")
    public ResponseEntity<?> deleteCoursePrerequisite(@PathVariable Long id) {
        coursePrerequisiteService.deleteCoursePrerequisite(id);
        return ResponseEntity.ok().body(new HashMap<>() {{
            put("message", "CoursePrerequisite deleted");
        }});
    }
}
