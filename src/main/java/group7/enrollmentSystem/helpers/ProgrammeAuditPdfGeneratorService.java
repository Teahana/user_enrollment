package group7.enrollmentSystem.helpers;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import group7.enrollmentSystem.dtos.classDtos.CourseAuditDto;
import group7.enrollmentSystem.dtos.classDtos.StudentFullAuditDto;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProgrammeAuditPdfGeneratorService {

    public byte[] generateAuditPdf(StudentFullAuditDto auditDto) throws DocumentException, IOException {
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

        // Title
        document.add(new Paragraph("USP Student Audit", titleFont));
        document.add(new Paragraph("\n"));

        // Student Info
        document.add(new Paragraph("Student ID: " + auditDto.getStudentId(), normalFont));
        document.add(new Paragraph("Student Name: " + auditDto.getStudentName(), normalFont));
        document.add(new Paragraph("Programme: " + auditDto.getProgrammeName(), normalFont));
        document.add(new Paragraph("Status: " + auditDto.getStatus(), normalFont));
        document.add(new Paragraph("\n"));

        // Audit Sections
        addAuditSection(document, "Completed:", filterCoursesByStatus(auditDto.getProgrammeCourses(), "Completed"), headerFont, normalFont);
        addAuditSection(document, "Registered:", filterCoursesByStatus(auditDto.getProgrammeCourses(), "Registered"), headerFont, normalFont);
        addAuditSection(document, "Unregistered Courses:", filterCoursesByStatus(auditDto.getProgrammeCourses(), "Unregistered"), headerFont, normalFont);

        document.close();
        return out.toByteArray();
    }
    private void addAuditSection(Document document, String sectionTitle, List<CourseAuditDto> courses, Font headerFont, Font normalFont) throws DocumentException {
        document.add(new Paragraph(sectionTitle, headerFont));

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setSpacingBefore(5f);
        table.setWidths(new float[]{2, 4, 2, 2});

        String[] headers = {"Course Code", "Course Name", "Course Level", "Course Status"};
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
            cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
            table.addCell(cell);
        }

        // Sort by course level so grouped together
        courses.sort((a, b) -> Integer.compare(a.getLevel(), b.getLevel()));

        int currentLevel = -1;
        BaseColor[] levelColors = {
                new BaseColor(230, 240, 255), // Light blue
                new BaseColor(240, 255, 230), // Light green
                new BaseColor(255, 245, 230), // Light orange
                new BaseColor(255, 230, 245)  // Light pink
        };

        int colorIndex = 0;
        BaseColor rowColor = BaseColor.WHITE;

        for (CourseAuditDto course : courses) {
            if (course.getLevel() != currentLevel) {
                currentLevel = course.getLevel();
                rowColor = levelColors[colorIndex % levelColors.length];
                colorIndex++;
            }

            table.addCell(createColoredCell(course.getCourseCode(), normalFont, rowColor));
            table.addCell(createColoredCell(course.getTitle(), normalFont, rowColor));
            table.addCell(createColoredCell(String.valueOf(course.getLevel()), normalFont, rowColor));

            String status;
            if (course.isCompleted()) {
                status = "Completed";
            } else if (course.isEnrolled()) {
                status = "Registered";
            } else {
                status = "Unregistered";
            }

            table.addCell(createColoredCell(status, normalFont, rowColor));
        }

        if (courses.isEmpty()) {
            PdfPCell empty = new PdfPCell(new Phrase("No courses in this category", normalFont));
            empty.setColspan(4);
            table.addCell(empty);
        }

        document.add(table);
        document.add(new Paragraph("\n"));
    }

    private PdfPCell createColoredCell(String text, Font font, BaseColor backgroundColor) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(backgroundColor);
        return cell;
    }


    private List<CourseAuditDto> filterCoursesByStatus(List<CourseAuditDto> allCourses, String status) {
        return allCourses.stream()
                .filter(c -> {
                    if ("Completed".equalsIgnoreCase(status)) return c.isCompleted();
                    if ("Registered".equalsIgnoreCase(status)) return !c.isCompleted() && c.isEnrolled();
                    if ("Unregistered".equalsIgnoreCase(status)) return !c.isCompleted() && !c.isEnrolled();
                    return false;
                })
                .collect(Collectors.toList());
    }
}
