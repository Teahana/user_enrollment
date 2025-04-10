package group7.enrollmentSystem.dtos.serverKtDtos

import group7.enrollmentSystem.dtos.classDtos.FlatCoursePrerequisiteRequest
import group7.enrollmentSystem.models.Course
import group7.enrollmentSystem.models.Programme

class KtDtos {
}
data class ProgrammesAndCoursesDto(
    val programmes: Programme,
    val courses: List<Course>)

data class CourseIdDto(
    val courseId: Long
)
data class CourseIdsDto(
    val courseIds: List<Long>
)
data class MessageDto(
    val message: String
)
data class CourseCodesDto(
    val courseCodes: List<String>
)
data class PrerequisitesDto(
    val prerequisites: FlatCoursePrerequisiteRequest
)
data class EmailDto(
    val email: String
)
data class UserIdDto(
    val userId: Long
)