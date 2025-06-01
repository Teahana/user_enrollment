from dataclasses import dataclass
from datetime import date, time

@dataclass
class CourseEnrollment:
    id: int
    student_id: int
    course_code: str
    completed: bool
    failed: bool
    cancelled: bool
    mark: int
    grade: str
    paid: bool
    date_enrolled: date
    currently_taking: bool
    semester_enrolled: int
    request_grade_change: bool
    request_grade_change_date: date
    request_grade_change_time: time
    programme_id: int
    course_title: str
    course_level: int

