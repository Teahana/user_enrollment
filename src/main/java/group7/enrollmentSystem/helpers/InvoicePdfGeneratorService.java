package group7.enrollmentSystem.helpers;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import group7.enrollmentSystem.dtos.classDtos.InvoiceDto;
import group7.enrollmentSystem.dtos.classDtos.CourseEnrollmentDto;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Random;

@Service
public class InvoicePdfGeneratorService {

    public byte[] generateInvoicePdf(InvoiceDto invoiceDto) throws DocumentException, IOException {
        Document document = new Document();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);
        document.open();

        Font titleFont = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD);
        Font headerFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
        Font normalFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL);
        Font fineheaderFont = new Font(Font.FontFamily.HELVETICA, 8, Font.BOLD);
        Font finePrint = new Font(Font.FontFamily.HELVETICA, 8, Font.NORMAL);

        // Add USP Logo
        ClassPathResource imageResource = new ClassPathResource("static/images/usp_logo.png");
        Image logo = Image.getInstance(imageResource.getURL());
        logo.scaleToFit(110, 110); //Scale the image
        //logo.setAlignment(Element.ALIGN_CENTER); // Center the image
        document.add(logo);

        // Add a space after the image
        document.add(new Paragraph("\n"));

        // Generate a random invoice number
        String invoiceNumber = generateInvoiceNumber();

        // Invoice Header
        document.add(new Paragraph("STUDENT INVOICE / STATEMENT", titleFont));
        document.add(new Paragraph("\n"));
        document.add(new Paragraph("Date Printed: " + java.time.LocalDate.now(), normalFont));
        document.add(new Paragraph("Invoice No: " + invoiceNumber, normalFont));
        document.add(new Paragraph("Student ID: " + invoiceDto.getStudentId(), normalFont));
        document.add(new Paragraph("Student Name: " + invoiceDto.getStudentName(), normalFont));
        document.add(new Paragraph("Programme: " + invoiceDto.getProgramme(), normalFont));
        document.add(new Paragraph("\n"));

        // Table for Course Enrollments
        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10f);
        table.setWidths(new float[]{3, 2, 2, 2, 2});

        // Table Headers
        String[] headers = {"Description", "Courses", "Reg Status", "Charges", "Credit"};
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
            cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
            table.addCell(cell);
        }

        // Course Data
        for (CourseEnrollmentDto course : invoiceDto.getEnrolledCourses()) {
            table.addCell(new PdfPCell(new Phrase(course.getTitle(), normalFont)));
            table.addCell(new PdfPCell(new Phrase(course.getCourseCode(), normalFont)));
            //table.addCell(new PdfPCell(new Phrase("Face to Face", normalFont)));
            table.addCell(new PdfPCell(new Phrase("**Registered**", normalFont)));
            table.addCell(new PdfPCell(new Phrase("$" + course.getCost(), normalFont)));
            table.addCell(new PdfPCell(new Phrase("$0.00", normalFont))); // No credit column for now
        }

        document.add(table);

        // Total Due Section
        document.add(new Paragraph("\nTOTAL DUE: $" + invoiceDto.getTotalDue(), titleFont));
        document.add(new Paragraph("\n"));
        document.add(new Paragraph("Terms and Conditions", fineheaderFont));
        Paragraph paragraph = new Paragraph();
        paragraph.add(new Chunk("1. Payment Deadline: ", fineheaderFont));
        paragraph.add(new Chunk("all fees and payment arrangements must be completed by the fee deadline specified in this invoice/statement.", finePrint));
        document.add(paragraph);

        paragraph = new Paragraph();
        paragraph.add(new Chunk("2. Account Deactivation: ", fineheaderFont));
        paragraph.add(new Chunk("all fees and payment arrangements must be completed by the fee deadline specified in this invoice/statement.", finePrint));
        document.add(paragraph);

        paragraph = new Paragraph();
        paragraph.add(new Chunk("2. Account Deactivation: ", fineheaderFont));
        paragraph.add(new Chunk("Accounts in default after the fee deadline will be deactivated/placed on HOLD for 1 week, after which the\n" +
                " accounts will be deregistered (registrations cancelled).", finePrint));
        document.add(paragraph);

        paragraph = new Paragraph();
        paragraph.add(new Chunk("3. Release of Holds: ", fineheaderFont));
        paragraph.add(new Chunk("Account HOLDS will be released upon payment of fees and applicable late payment penalties, provided deregistration\n" +
                " has not yet been processed. Please allow 3 working days for payment update and account re-activation.", finePrint));
        document.add(paragraph);

        paragraph = new Paragraph();
        paragraph.add(new Chunk("4. Deregistration Process: ", fineheaderFont));
        paragraph.add(new Chunk("Deregistration will be processed for all deactivated/HOLD accounts within 2 weeks after the fee deadline. Students\n" +
                " are liable for accommodation and other ancillary fees after deregistration.", finePrint));
        document.add(paragraph);

                document.close();
        return out.toByteArray();
    }

    // Method to generate a random invoice number
    private String generateInvoiceNumber() {
        Random random = new Random();
        // Generate a random 7-digit number
        int randomNumber = random.nextInt(9000000) + 1000000; // Ensures 7 digits
        return "U" + randomNumber; // Prepend "U" to the random number
    }
}