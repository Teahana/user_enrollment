package group7.enrollmentSystem.dtos.appDtos

import group7.enrollmentSystem.dtos.classDtos.CourseEnrollmentDto
import group7.enrollmentSystem.models.CourseEnrollment

data class StudentDto(
    val studentId: String,
    val studentName: String,
    val email: String,
    val programmeName: String,
    val address: String,
    val phoneNumber: String,
)

data class StudentFullAuditDto(
    val studentId: String,
    val studentName: String,
    val programmeName: String,
    val status: String,
    val programmeCourses: List<CourseAuditDto>
)

data class CourseAuditDto(
    val courseId: Long,
    val courseTitle: String,
    val courseCode: String,
    val isCurrentlyEnrolled: Boolean,
    val level: Int,
    val isCompleted: Boolean
)

data class EnrollCourseRequest(
    val selectedCourses: List<String>?,
    val userId: Long
)

data class InvoiceDto(
    val studentName: String,
    val studentId: String,
    val programme: String,
    val enrolledCourses: List<CourseEnrollmentDto>,
    val totalDue: Double
)