package group7.enrollmentSystem.dtos.serverKtDtos

import group7.enrollmentSystem.models.Course
import group7.enrollmentSystem.models.Programme

class KtDtos {
}
data class ProgrammesAndCoursesDto(
    val programmes: Programme,
    val courses: List<Course>)