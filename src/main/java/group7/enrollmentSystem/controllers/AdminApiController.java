package group7.enrollmentSystem.controllers;

import group7.enrollmentSystem.config.CustomExceptions;
import group7.enrollmentSystem.dtos.appDtos.LoginResponse;
import group7.enrollmentSystem.dtos.classDtos.*;
import group7.enrollmentSystem.dtos.interfaceDtos.CourseIdAndCode;
import group7.enrollmentSystem.dtos.interfaceDtos.ProgrammeIdAndCode;
import group7.enrollmentSystem.dtos.serverKtDtos.CourseCodesDto;
import group7.enrollmentSystem.dtos.serverKtDtos.CourseIdDto;
import group7.enrollmentSystem.dtos.serverKtDtos.MessageDto;
import group7.enrollmentSystem.dtos.serverKtDtos.PrerequisitesDto;
import group7.enrollmentSystem.enums.OnHoldTypes;
import group7.enrollmentSystem.enums.SpecialPrerequisiteType;
import group7.enrollmentSystem.helpers.JwtService;
import group7.enrollmentSystem.models.*;
import group7.enrollmentSystem.repos.*;
import group7.enrollmentSystem.services.CourseProgrammeService;
import group7.enrollmentSystem.services.CourseService;
import group7.enrollmentSystem.services.StudentHoldService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminApiController {

    private final CourseRepo courseRepo;
    private final CourseProgrammeService courseProgrammeService;
    private final CourseService courseService;
    private final ProgrammeRepo programmeRepo;
    private final UserRepo userRepo;
    private final JwtService jwtService;
    private final StudentRepo studentRepo;
    private final StudentHoldService studentHoldService;

    private final HoldServiceRestrictionRepo restrictionRepo;

    @Operation(
            summary = "Token-based admin login",
            description = "Generates a new JWT token for already authenticated admin users."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully authenticated and returned new token",
                    content =@Content(schema =@Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or expired token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User doesn't have admin permissions"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/tokenLogin")
    public ResponseEntity<LoginResponse> tokenLogin(Authentication authentication) {
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

//------------Courses and Preqs Section-------------------------------------------------------------------

    @Operation(
            summary = "Get all courses with details",
            description = "Retrieves all courses with their programme associations and prerequisites, sorted by level."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully retrieved all courses"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or expired token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User doesn't have admin permissions"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/getCourses")
    public ResponseEntity< List<CourseDto>> getCourses() {
        List<CourseDto> courseDtos = courseService.getAllCoursesWithProgrammesAndPrereqs();
        courseDtos.sort(Comparator.comparing(CourseDto::getLevel));
        return ResponseEntity.ok(courseDtos);
    }

    @Operation(
            summary = "Get special prerequisite types",
            description = "Returns all available special prerequisite types."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully retrieved special types"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or expired token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User doesn't have admin permissions"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/getSpecialTypes")
    public ResponseEntity<?> getSpecialTypes() {
        return ResponseEntity.ok(Map.of("specialTypes", Arrays.toString(SpecialPrerequisiteType.values())));
    }

    @Operation(
            summary = "Get all course IDs and codes",
            description = "Returns minimal course information (ID and code) for all courses."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully retrieved all courses"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or expired token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User doesn't have admin permissions"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/getAllCourses")
    public ResponseEntity<List<CourseIdAndCode>> getAllCourses() {
        List<CourseIdAndCode> courses = courseRepo.findAllBy();
        return ResponseEntity.ok(courses);
    }

    @Operation(
            summary = "Get all programme IDs and codes",
            description = "Returns minimal programme information (ID and code) for all programmes."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully retrieved all programmes"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or expired token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User doesn't have admin permissions"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/getAllProgrammes")
    public ResponseEntity<List<ProgrammeIdAndCode>> getAllProgrammes() {
        List<ProgrammeIdAndCode> programmes = programmeRepo.findAllBy();
        return ResponseEntity.ok(programmes);
    }

    @Operation(
            summary = "Add course prerequisites",
            description = "Adds new prerequisite relationships for a course."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully added prerequisites"),
            @ApiResponse(responseCode = "400", description = "Invalid prerequisite data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or expired token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User doesn't have admin permissions"),
            @ApiResponse(responseCode = "404", description = "Course not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/addPreReqs")
    public ResponseEntity<MessageDto> addPrerequisites(@RequestBody FlatCoursePrerequisiteRequest request) {
         System.out.println("Request: "+request);
        courseService.addPrerequisites(request);
        return ResponseEntity.ok(new MessageDto("Prerequisites added successfully"));
    }

    @Operation(
            summary = "Update course prerequisites",
            description = "Updates existing prerequisite relationships for a course."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully updated prerequisites"),
            @ApiResponse(responseCode = "400", description = "Invalid prerequisite data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or expired token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User doesn't have admin permissions"),
            @ApiResponse(responseCode = "404", description = "Course not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/updatePreReqs")
    public ResponseEntity<MessageDto> updatePrerequisites(@RequestBody FlatCoursePrerequisiteRequest request) {
        courseService.updatePrerequisites(request);
        return ResponseEntity.ok(new MessageDto("Prerequisites updated successfully"));
    }

    @Operation(
            summary = "Get course prerequisites",
            description = "Retrieves all prerequisite relationships for a specific course."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully retrieved prerequisites"),
            @ApiResponse(responseCode = "400", description = "Invalid course ID"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or expired token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User doesn't have admin permissions"),
            @ApiResponse(responseCode = "404", description = "Course not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/getPreReqs")
    public ResponseEntity<?> getPreReqs(@RequestBody CourseIdDto request) {
        FlatCoursePrerequisiteRequest prerequisites = courseService.getPrerequisitesForCourse(request.getCourseId());
        return ResponseEntity.ok(new PrerequisitesDto(prerequisites));
    }

    @Operation(
            summary = "Get prerequisite tree",
            description = "Returns a hierarchical tree structure of all prerequisites for a course."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully retrieved prerequisite tree"),
            @ApiResponse(responseCode = "400", description = "Invalid course ID"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or expired token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User doesn't have admin permissions"),
            @ApiResponse(responseCode = "404", description = "Course not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/getPreReqTree")
    public ResponseEntity<GraphicalPrerequisiteNode> getPrereqTree(@RequestBody CourseIdDto request) {
        Long courseId = request.getCourseId();
        GraphicalPrerequisiteNode root = courseService.buildPrerequisiteTree(courseId);
        return ResponseEntity.ok(root);
    }

    @Operation(
            summary = "Get courses except specified",
            description = "Returns all courses except the one with the given ID."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully retrieved courses"),
            @ApiResponse(responseCode = "400", description = "Invalid course ID"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or expired token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User doesn't have admin permissions"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/getCoursesExcept/{id}")
    public ResponseEntity<CourseCodesDto> getCoursePrerequisites(@PathVariable Long id) {
        List<Course> courses = courseRepo.findByIdNot(id);
        List<String> courseCodes = courses.stream().map(Course::getCourseCode).toList();
        return ResponseEntity.ok(new CourseCodesDto(courseCodes));
    }

    @Operation(
            summary = "Get unlinked courses",
            description = "Returns courses not currently linked to a specific programme."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully retrieved unlinked courses"),
            @ApiResponse(responseCode = "400", description = "Invalid programme code"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or expired token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User doesn't have admin permissions"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/getCoursesNotLinkedToProgramme")
    public ResponseEntity<List<Course>> getCoursesNotLinkedToProgramme(@RequestParam String programmeCode) {
        List<Course> courses = courseProgrammeService.getCoursesNotLinkedToProgramme(programmeCode);
        return ResponseEntity.ok(courses);
    }

    @Operation(
            summary = "Remove course from programme",
            description = "Removes a course from a programme's curriculum."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully removed course from programme"),
            @ApiResponse(responseCode = "400", description = "Invalid course or programme code"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or expired token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User doesn't have admin permissions"),
            @ApiResponse(responseCode = "404", description = "Course or programme not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @DeleteMapping("/removeCourseFromProgramme")
    public ResponseEntity<Void> removeCourseFromProgramme(
            @RequestParam String courseCode,
            @RequestParam String programmeCode) {
        courseProgrammeService.removeCourseFromProgramme(courseCode, programmeCode);
        return ResponseEntity.ok().build();
    }

    //------------Student Holds Section-------------------------------------------------------------------
    @Operation(
            summary = "Get all student holds",
            description = "Returns a list of all students with their current hold status."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully retrieved student holds"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or expired token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User doesn't have admin permissions"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/holds")
    public ResponseEntity<List<StudentHoldDto>> getAllStudentHolds() {
        try {
            List<StudentHoldDto> holds = studentHoldService.getAllStudentsWithHoldStatus();
            return ResponseEntity.ok(holds);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/holds/{studentId}")
    public ResponseEntity<StudentHoldDto> getStudentHolds(@PathVariable Long studentId) {
        return ResponseEntity.ok(studentHoldService.getStudentHolds(studentId));
    }

    @Operation(
            summary = "Place student hold",
            description = "Places a hold on a student account with specified hold type."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully placed hold"),
            @ApiResponse(responseCode = "400", description = "Invalid student ID or hold type"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or expired token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User doesn't have admin permissions"),
            @ApiResponse(responseCode = "404", description = "Student not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/holds/placeHold")
    public ResponseEntity<MessageDto> placeHold(@RequestBody Map<String, String> request, Authentication authentication) {
        Long studentId = Long.parseLong(request.get("studentId"));
        OnHoldTypes holdType = OnHoldTypes.valueOf(request.get("holdType"));
        String actionBy = authentication.getName();
        return studentHoldService.placeHold(studentId, holdType, actionBy);
    }


    @Operation(
            summary = "Remove student hold",
            description = "Removes any active hold from a student account."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully removed hold"),
            @ApiResponse(responseCode = "400", description = "Invalid student ID"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or expired token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User doesn't have admin permissions"),
            @ApiResponse(responseCode = "404", description = "Student not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/holds/removeHold")
    public ResponseEntity<MessageDto> removeHold(@RequestBody Map<String, String> request, Authentication authentication) {
        Long studentId = Long.parseLong(request.get("studentId"));
        OnHoldTypes holdType = OnHoldTypes.valueOf(request.get("holdType"));
        String actionBy = authentication.getName();
        return studentHoldService.removeHold(studentId, holdType, actionBy);
    }

    @GetMapping("/holds/types")
    public ResponseEntity<List<OnHoldTypes>> getHoldTypes() {
        return studentHoldService.getHoldTypes();
    }

    @Operation(
            summary = "Get all hold history",
            description = "Returns complete history of all hold placements and removals."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully retrieved hold history"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or expired token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User doesn't have admin permissions"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/holds/history")
    public ResponseEntity<List<StudentHoldHistoryDto>> getAllHoldHistory() {
        return ResponseEntity.ok(studentHoldService.getAllHoldHistory());
    }

    @Operation(
            summary = "Get (Filter) hold history by student",
            description = "Returns hold history for a specific student."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully retrieved student hold history"),
            @ApiResponse(responseCode = "400", description = "Invalid student ID"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or expired token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User doesn't have admin permissions"),
            @ApiResponse(responseCode = "404", description = "Student not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/holds/history/{studentId}")
    public ResponseEntity<List<StudentHoldHistoryDto>> getHoldHistoryByStudent(@PathVariable Long studentId) {
        return ResponseEntity.ok(studentHoldService.getHoldHistoryByStudent(studentId));
    }

    @Operation(summary = "Get all hold restrictions", description = "Retrieves all hold service restrictions")
    @GetMapping("/hold-restrictions")
    public ResponseEntity<List<HoldRestrictionDto>> getAllHoldRestrictions() {
        List<HoldRestrictionDto> restrictions = restrictionRepo.findAll().stream()
                .map(studentHoldService::convertToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(restrictions);
    }

    @Operation(summary = "Get hold restriction by type", description = "Retrieves service restrictions for a specific hold type")
    @GetMapping("/hold-restrictions/{holdType}")
    public ResponseEntity<HoldRestrictionDto> getHoldRestriction(@PathVariable OnHoldTypes holdType) {
        HoldServiceRestriction restriction = restrictionRepo.findByHoldType(holdType);
        if (restriction == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(studentHoldService.convertToDto(restriction));
    }

    @Operation(summary = "Update hold restriction", description = "Updates service restrictions for a hold type")
    @PutMapping("/hold-restrictions/{holdType}")
    public ResponseEntity<HoldRestrictionDto> updateHoldRestriction(
            @PathVariable OnHoldTypes holdType,
            @RequestBody HoldRestrictionDto dto) {

        HoldServiceRestriction restriction = restrictionRepo.findByHoldType(holdType);
        if (restriction == null) {
            restriction = new HoldServiceRestriction();
            restriction.setHoldType(holdType);
        }

        restriction.setBlockCourseEnrollment(dto.isBlockCourseEnrollment());
        restriction.setBlockViewCompletedCourses(dto.isBlockViewCompletedCourses());
        restriction.setBlockStudentAudit(dto.isBlockStudentAudit());
        restriction.setBlockGenerateTranscript(dto.isBlockGenerateTranscript());
        restriction.setBlockForms(dto.isBlockGraduationApplication());

        restriction = restrictionRepo.save(restriction);
        return ResponseEntity.ok(studentHoldService.convertToDto(restriction));
    }

    @Operation(summary = "Check service access for student")
    @GetMapping("/check-service-access/{studentId}")
    public ResponseEntity<Map<String, Boolean>> checkServiceAccess(
            @PathVariable Long studentId,
            @RequestParam StudentHoldService.HoldRestrictionType service) {
        Student student = studentRepo.findById(studentId)
                .orElseThrow(() -> new CustomExceptions.StudentNotFoundException("Student not found"));

        boolean hasAccess = !studentHoldService.hasRestriction(student.getEmail(), service);
        return ResponseEntity.ok(Map.of("hasAccess", hasAccess));
    }
}
