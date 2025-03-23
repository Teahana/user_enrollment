package group7.enrollmentSystem.controllers;

import group7.enrollmentSystem.dtos.classDtos.InvoiceDto;
import group7.enrollmentSystem.dtos.classDtos.CourseEnrollmentDto;
import group7.enrollmentSystem.models.Student;
import group7.enrollmentSystem.models.StudentProgramme;
import group7.enrollmentSystem.repos.StudentRepo;
import group7.enrollmentSystem.services.CourseEnrollmentService;
import group7.enrollmentSystem.services.InvoicePdfGeneratorService;
import group7.enrollmentSystem.services.StudentProgrammeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/student")
@RequiredArgsConstructor
public class InvoiceApiController {

    private final CourseEnrollmentService courseEnrollmentService;
    private final StudentProgrammeService studentProgrammeService;
    private final InvoicePdfGeneratorService invoicePdfGeneratorService;
    private final StudentRepo studentRepo;

    @GetMapping("/invoice/download")
    public ResponseEntity<byte[]> downloadInvoice(Principal principal) throws Exception {
        String email = principal.getName();
        Student student = studentRepo.findByEmail(email).orElseThrow(() -> new RuntimeException("Student not found"));

        List<CourseEnrollmentDto> enrolledCourses = courseEnrollmentService.getActiveEnrollments(student.getId())
                .stream()
                .map(ce -> new CourseEnrollmentDto(
                        ce.getCourse().getId(),
                        ce.getCourse().getCourseCode(),
                        ce.getCourse().getTitle(),
                        ce.getCourse().getCost()))
                .collect(Collectors.toList());

        double totalDue = enrolledCourses.stream().mapToDouble(CourseEnrollmentDto::getCost).sum();

        InvoiceDto invoiceDto = new InvoiceDto();
        invoiceDto.setStudentName(student.getFirstName() + " " + student.getLastName());
        invoiceDto.setStudentId(student.getStudentId());
        Optional<StudentProgramme> currentProgramme = studentProgrammeService.getCurrentProgramme(student);
        if (currentProgramme.isPresent()) {
            invoiceDto.setProgramme(currentProgramme.get().getProgramme().getName());
        } else {
            throw new RuntimeException("No current programme found for the student");
        }
        invoiceDto.setEnrolledCourses(enrolledCourses);
        invoiceDto.setTotalDue(totalDue);

        byte[] pdfBytes = invoicePdfGeneratorService.generateInvoicePdf(invoiceDto);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "invoice.pdf");

        return ResponseEntity.ok().headers(headers).body(pdfBytes);
    }
}