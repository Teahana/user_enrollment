package group7.enrollmentSystem.dtos.appDtos

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
    val selectedCourses: List<String>
)