package group7.enrollmentSystem.helpers;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import group7.enrollmentSystem.dtos.classDtos.CourseEnrollmentDto;
import group7.enrollmentSystem.dtos.classDtos.InvoiceDto;
import group7.enrollmentSystem.dtos.classDtos.StudentFullAuditDto;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.tools.DocumentationTool;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
@Service
public class ProgrammeAuditPdfGeneratorService {

    public byte[] generateAuditPdf(StudentFullAuditDto studentFullAuditDto) throws DocumentException, IOException {
        Document document = new Document();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, outputStream);
        document.open();

        Font titleFont = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD);
        Font headerFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
        Font normalFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL);

        // Add USP Logo
        ClassPathResource imageResource = new ClassPathResource("static/images/usp_logo.png");
        Image logo = Image.getInstance(imageResource.getURL());
        logo.scaleToFit(110, 110); //Scale the image
        //logo.setAlignment(Element.ALIGN_CENTER); // Center the image
        document.add(logo);

        // Add a space after the image
        document.add(new Paragraph("\n"));

        // Audit document Header
        document.add(new Paragraph("STUDENT Audit", titleFont));
        document.add(new Paragraph("\n"));
        document.add(new Paragraph("Student ID: " + studentFullAuditDto.getStudentId(), normalFont));
        document.add(new Paragraph("Student Name: " + studentFullAuditDto.getStudentName(), normalFont));
        document.add(new Paragraph("Programme: " + studentFullAuditDto.getProgrammeName(), normalFont));
        document.add(new Paragraph("\n"));

        // Table for Course Enrollments
        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10f);
        table.setWidths(new float[]{3, 2, 2});

        // Table Headers
        String[] headers = {"Description", "Courses", "Reg Status"};
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
            cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
            table.addCell(cell);
        }

        // Course Data
        InvoiceDto invoiceDto = new InvoiceDto();
        for (CourseEnrollmentDto course : invoiceDto.getEnrolledCourses()) {
            table.addCell(new PdfPCell(new Phrase(course.getTitle(), normalFont)));
            table.addCell(new PdfPCell(new Phrase(course.getCourseCode(), normalFont)));
            //table.addCell(new PdfPCell(new Phrase("Face to Face", normalFont)));
            table.addCell(new PdfPCell(new Phrase("**Registered**", normalFont)));
            table.addCell(new PdfPCell(new Phrase("$" + course.getCost(), normalFont)));
            table.addCell(new PdfPCell(new Phrase("$0.00", normalFont))); // No credit column for now
        }

        document.add(table);

        document.close();
        return outputStream.toByteArray();
    }
}