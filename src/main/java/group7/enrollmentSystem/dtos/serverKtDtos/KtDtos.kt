package group7.enrollmentSystem.dtos.serverKtDtos

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import group7.enrollmentSystem.dtos.classDtos.FlatCoursePrerequisiteRequest
import group7.enrollmentSystem.models.Course
import group7.enrollmentSystem.models.Programme

class KtDtos {
}
data class ProgrammesAndCoursesDto @JsonCreator constructor(
    @JsonProperty("programmes") val programmes: Programme,
    @JsonProperty("courses") val courses: List<Course>)


data class CourseIdsDto @JsonCreator constructor(
    @JsonProperty("courseIds") val courseIds: List<Long>
)

data class CourseCodesDto @JsonCreator constructor(
    @JsonProperty("courseCodes") val courseCodes: List<String>
)
data class PrerequisitesDto @JsonCreator constructor(
    @JsonProperty("prerequisites") val prerequisites: FlatCoursePrerequisiteRequest
)
data class EmailDto @JsonCreator constructor(
    @JsonProperty("email") val email: String
)

data class CourseIdDto @JsonCreator constructor(
    @JsonProperty("courseId") val courseId: Long
)

data class MessageDto @JsonCreator constructor(
    @JsonProperty("message") val message: String
)

data class UserIdDto @JsonCreator constructor(
    @JsonProperty("userId") val userId: Long
)
data class CancelCourseRequest @JsonCreator constructor(
    @JsonProperty("courseId") val courseId: Long,
    @JsonProperty("userId") val userId: Long
)
