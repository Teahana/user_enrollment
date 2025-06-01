package group7.enrollmentSystem.helpers;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import group7.enrollmentSystem.dtos.classDtos.CoursesTranscriptDTO;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CoursesTranscriptPdfGeneratorService {

    private final GradeService gradeService;

    public byte[] generateTranscriptPdf(CoursesTranscriptDTO dto) throws DocumentException, IOException {
        Document document = new Document();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);
        document.open();

        Font titleFont = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD);
        Font headerFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
        Font normalFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL);

        // Add USP Logo
        ClassPathResource imageResource = new ClassPathResource("static/images/usp_logo.png");
        Image logo = Image.getInstance(imageResource.getURL());
        logo.scaleToFit(110, 110);
        document.add(logo);
        document.add(new Paragraph("\n"));

        // Header Info
        document.add(new Paragraph("ACADEMIC TRANSCRIPT", titleFont));
        document.add(new Paragraph("Student ID: " + dto.getStudentId(), normalFont));
        document.add(new Paragraph("Name: " + dto.getStudentName(), normalFont));
        document.add(new Paragraph("Programme: " + dto.getProgramme(), normalFont));
        document.add(new Paragraph("\n"));

        // Prepare filtered completed list (show only highest fail or highest pass per course)
        Map<String, CoursesTranscriptDTO.CourseTranscriptRow> finalCompleted = new HashMap<>();

        for (CoursesTranscriptDTO.CourseTranscriptRow row : dto.getCompletedCourses()) {
            String code = row.getCourseCode();
            double gpaPoint = gradeService.getGradePoint(row.getGrade());

            if (!finalCompleted.containsKey(code)) {
                finalCompleted.put(code, row);
            } else {
                CoursesTranscriptDTO.CourseTranscriptRow existing = finalCompleted.get(code);
                double existingGpa = gradeService.getGradePoint(existing.getGrade());

                // Prefer pass over fail, or if same type, prefer higher mark
                if (gpaPoint > 0 && existingGpa == 0) {
                    finalCompleted.put(code, row); // Replace fail with pass
                } else if ((gpaPoint == 0 && existingGpa == 0) || (gpaPoint > 0 && existingGpa > 0)) {
                    if (row.getMark() > existing.getMark()) {
                        finalCompleted.put(code, row); // Same type, higher mark wins
                    }
                }
            }
        }

        // Convert to list and pass to section
        List<CoursesTranscriptDTO.CourseTranscriptRow> completedFiltered = new ArrayList<>(finalCompleted.values());
        addCourseTableSection(document, "History", completedFiltered, headerFont, normalFont);
        addCourseTableSection(document, "Passed Courses", dto.getPassedCourses(), headerFont, normalFont);
        addCourseTableSection(document, "Failed Courses", dto.getFailedCourses(), headerFont, normalFont);

        // GPA Calculation (based on completed courses)
        double totalGpa = 0;
        int count = 0;

        Map<String, CoursesTranscriptDTO.CourseTranscriptRow> highestGradeMap = new HashMap<>();

        for (CoursesTranscriptDTO.CourseTranscriptRow row : dto.getCompletedCourses()) {
            String courseCode = row.getCourseCode();
            if (!highestGradeMap.containsKey(courseCode) || row.getMark() > highestGradeMap.get(courseCode).getMark()) {
                highestGradeMap.put(courseCode, row);
            }
        }

        for (CoursesTranscriptDTO.CourseTranscriptRow row : highestGradeMap.values()) {
                double gpaPoint = gradeService.getGradePoint(row.getGrade());
                totalGpa += gpaPoint;
                count++;
        }
        double gpa = count > 0 ? totalGpa / count : 0.0;

        // Determine which course attempts are failed (grade = 0 GPA) AND not selected for GPA
        List<CoursesTranscriptDTO.CourseTranscriptRow> failedOnlyAttempts = new ArrayList<>();
        for (CoursesTranscriptDTO.CourseTranscriptRow row : dto.getCompletedCourses()) {
            double gpaPoint = gradeService.getGradePoint(row.getGrade());
            if (gpaPoint == 0.0) {
                // Always add failed attempt if it's NOT the best attempt used for GPA
                CoursesTranscriptDTO.CourseTranscriptRow best = highestGradeMap.get(row.getCourseCode());
                if (best == null || !Objects.equals(best.getMark(), row.getMark())) {
                    failedOnlyAttempts.add(row);
                }
            }
        }


        document.add(new Paragraph(String.format("Calculated GPA: %.2f", gpa), headerFont));
        document.add(new Paragraph("Total Units Completed: " + dto.getCompletedCourses().size(), normalFont));
        document.add(new Paragraph("Total Units Passed: " + dto.getPassedCourses().size(), normalFont));
        document.add(new Paragraph("Total Units Failed: " + failedOnlyAttempts.size(), normalFont));

        document.close();
        return out.toByteArray();
    }

    private void addCourseTableSection(Document document, String title,
                                       List<CoursesTranscriptDTO.CourseTranscriptRow> rows,
                                       Font headerFont, Font normalFont) throws DocumentException {
        if (rows == null || rows.isEmpty()) return;

        document.add(new Paragraph(title, headerFont));
        document.add(new Paragraph("\n"));

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{2, 5, 2, 2});

        String[] headers = {"Course Code", "Course Title", "Grade", "Mark"};
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
            cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
            table.addCell(cell);
        }

        for (CoursesTranscriptDTO.CourseTranscriptRow row : rows) {
            table.addCell(new Phrase(row.getCourseCode(), normalFont));
            table.addCell(new Phrase(row.getTitle(), normalFont));
            table.addCell(new Phrase(row.getGrade(), normalFont));
            table.addCell(new Phrase(String.valueOf(row.getMark()), normalFont));
        }

        document.add(table);
        document.add(new Paragraph("\n"));
    }
}
