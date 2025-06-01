package group7.enrollmentSystem.helpers;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import group7.enrollmentSystem.dtos.classDtos.CoursesTranscriptDTO;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.List;

@Service
public class CoursesTranscriptPdfGeneratorService {

    public byte[] generateTranscriptPdf(CoursesTranscriptDTO dto) throws DocumentException {
        Document document = new Document();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);
        document.open();

        Font titleFont = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD);
        Font headerFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
        Font normalFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL);

        document.add(new Paragraph("ACADEMIC TRANSCRIPT", titleFont));
        document.add(new Paragraph("Student ID: " + dto.getStudentId(), normalFont));
        document.add(new Paragraph("Name: " + dto.getStudentName(), normalFont));
        document.add(new Paragraph("Programme: " + dto.getProgramme(), normalFont));
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

        for (CoursesTranscriptDTO.CourseTranscriptRow row : dto.getTranscriptRows()) {
            table.addCell(new Phrase(row.getCourseCode(), normalFont));
            table.addCell(new Phrase(row.getTitle(), normalFont));
            table.addCell(new Phrase(row.getGrade(), normalFont));
            table.addCell(new Phrase(String.valueOf(row.getMark()), normalFont));
        }

        document.add(table);
        document.add(new Paragraph("\nGPA: " + String.format("%.2f", dto.getGpa()), headerFont));
        document.close();

        return out.toByteArray();
    }
}
