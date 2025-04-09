package group7.enrollmentSystem.dtos.appDtos

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

data class StudentDto(
    val studentId: String,
    val studentName: String,
    val email: String,
    val programmeName: String,
    val address: String,
    val phoneNumber: String,
)

data class StudentAuditDto(
    val studentId: String,
    val studentName: String,
    val programmeName: String,
    val status: String,
    val programmeCourses: List<CourseAuditDto>
)

data class CourseAuditDto(
    val id: Long,
    val courseTitle: String,
    val courseCode: String,
    val enrolled: Boolean,
    val completed: Boolean,
    val level: Int

)

data class EnrollCourseRequest(
    val selectedCourses: List<String>?,
    val userId: Long
)
