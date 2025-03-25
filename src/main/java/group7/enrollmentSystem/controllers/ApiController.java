package group7.enrollmentSystem.controllers;

import group7.enrollmentSystem.dtos.classDtos.CourseDto;
import group7.enrollmentSystem.dtos.classDtos.StudentFullAuditDto;
import group7.enrollmentSystem.dtos.classDtos.ProgrammeDto;
import group7.enrollmentSystem.dtos.classDtos.CourseEnrollmentDto;
import group7.enrollmentSystem.models.*;
import group7.enrollmentSystem.repos.CourseEnrollmentRepo;
import group7.enrollmentSystem.repos.StudentRepo;
import group7.enrollmentSystem.services.*;
//import group7.enrollmentSystem.services.StudentProgrammeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiController {

    private final UserService userService;
    private final CourseService courseService;
    private final ProgrammeService programmeService;
    private final CourseProgrammeService courseProgrammeService;
    private final StudentProgrammeAuditService studentProgrammeAuditService;

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
}
