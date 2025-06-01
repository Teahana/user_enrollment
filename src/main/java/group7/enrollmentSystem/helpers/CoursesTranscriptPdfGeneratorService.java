package group7.enrollmentSystem.helpers;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import group7.enrollmentSystem.dtos.classDtos.CoursesTranscriptDTO;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
public class CoursesTranscriptPdfGeneratorService {

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
        document.add(new Paragraph("GPA: " + String.format("%.2f", dto.getGpa()), normalFont));
        document.add(new Paragraph("\n"));

        // Completed Courses Table
        addCourseTableSection(document, "Completed Courses", dto.getCompletedCourses(), headerFont, normalFont);

        // Passed Courses Table
        addCourseTableSection(document, "Passed Courses", dto.getPassedCourses(), headerFont, normalFont);

        // Failed Courses Table
        addCourseTableSection(document, "Failed Courses", dto.getFailedCourses(), headerFont, normalFont);

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
