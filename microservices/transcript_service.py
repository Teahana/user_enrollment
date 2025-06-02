import os
import json
from typing import List
from collections import defaultdict
from io import BytesIO
from fpdf import FPDF
from db import get_db_connection
from models import CourseEnrollment


class TranscriptPDF(FPDF):
    def generate(self, student_meta, history, passed, failed, gpa_info) -> bytes:
        self.add_page()

        # Header
        self.set_font("Arial", "B", 14)
        self.cell(0, 10, "ACADEMIC TRANSCRIPT", ln=True, align="C")
        self.ln(5)

        self.set_font("Arial", "", 11)
        self.cell(0, 10, f"Student ID: {student_meta['student_id']}", ln=True)
        self.cell(0, 10, f"Name: {student_meta['first_name']} {student_meta['last_name']}", ln=True)
        self.cell(0, 10, f"Programme: {student_meta['programme']}", ln=True)
        self.ln(10)

        self.add_table("History", history)
        self.add_table("Passed Courses", passed)
        self.add_table("Failed Courses", failed)

        self.ln(5)
        self.set_font("Arial", "B", 11)
        self.cell(0, 10, f"Calculated GPA: {gpa_info['gpa']:.2f}", ln=True)
        self.set_font("Arial", "", 10)
        self.cell(0, 10, f"Total Units Completed (History): {gpa_info['completed']}", ln=True)
        self.cell(0, 10, f"Total Units Passed: {gpa_info['passed']}", ln=True)
        self.cell(0, 10, f"Total Units Failed: {gpa_info['failed']}", ln=True)

        return self.output(dest='S').encode('latin1')

    def add_table(self, title, enrollments):
        if not enrollments:
            return
        self.set_font("Arial", "B", 12)
        self.cell(0, 10, title, ln=True)
        self.set_font("Arial", "B", 10)
        self.cell(40, 10, "Course Code", 1)
        self.cell(80, 10, "Course Title", 1)
        self.cell(30, 10, "Grade", 1)
        self.cell(30, 10, "Mark", 1)
        self.ln()

        self.set_font("Arial", "", 10)
        for e in enrollments:
            self.cell(40, 10, e.course_code, 1)
            self.cell(80, 10, e.course_title, 1)
            self.cell(30, 10, str(e.grade) if e.grade is not None else "", 1)
            self.cell(30, 10, str(e.mark), 1)
            self.ln()
        self.ln(5)


class TranscriptService:
    def load_grade_points(self):
        base_dir = os.path.dirname(os.path.abspath(__file__))
        path = os.path.normpath(os.path.join(base_dir, "../src/main/resources/configs/grades.json"))
        with open(path, "r") as f:
            data = json.load(f)
        return {k: v["gpa"] for k, v in data.items()}

    def fetch_student_metadata(self, student_id: int):
        query = """
        SELECT s.student_id, u.first_name, u.last_name, p.name AS programme
        FROM student s
        JOIN users u ON s.id = u.id
        JOIN course_enrollment ce ON ce.student_id = s.id
        JOIN programme p ON ce.programme_id = p.id
        WHERE s.id = %s
        LIMIT 1
        """
        conn = get_db_connection()
        cursor = conn.cursor(dictionary=True)
        cursor.execute(query, (student_id,))
        row = cursor.fetchone()
        cursor.close()
        conn.close()
        return row

    def fetch_enrollments(self, student_id: int) -> List[CourseEnrollment]:
        query = """
        SELECT 
            ce.id, ce.student_id, ce.completed, ce.failed, ce.cancelled,
            ce.mark, ce.grade, ce.paid, ce.date_enrolled, ce.currently_taking,
            ce.semester_enrolled, ce.request_grade_change, ce.request_grade_change_date,
            ce.request_grade_change_time, ce.programme_id,
            c.title AS course_title, c.course_code AS course_code, c.level AS course_level
        FROM course_enrollment ce
        JOIN course c ON ce.course_id = c.id
        WHERE ce.completed = TRUE AND ce.student_id = %s
        """
        conn = get_db_connection()
        cursor = conn.cursor(dictionary=True)
        cursor.execute(query, (student_id,))
        rows = cursor.fetchall()
        cursor.close()
        conn.close()

        return [CourseEnrollment(**row) for row in rows]


    def filter_highest_attempts(self, enrollments: List[CourseEnrollment], grade_map):
        grouped = defaultdict(list)
        for e in enrollments:
            grouped[e.course_code].append(e)

        highest = []
        for attempts in grouped.values():
            passed = [a for a in attempts if grade_map.get(a.grade, 0.0) > 0 and not a.failed]
            selected = max(passed, key=lambda x: x.mark) if passed else max(attempts, key=lambda x: x.mark)
            highest.append(selected)
        return highest

    def calculate_gpa(self, enrollments: List[CourseEnrollment], grade_map):
        total = count = passed = failed = 0
        for e in enrollments:
            gpa = grade_map.get(e.grade, 0.0)
            total += gpa
            count += 1
            if gpa > 0:
                passed += 1
            else:
                failed += 1
        return {
            "gpa": total / count if count else 0.0,
            "completed": count,
            "passed": passed,
            "failed": failed
        }

    def generate_transcript(self, student_id: int) -> bytes:
        student_meta = self.fetch_student_metadata(student_id)
        all_enrollments = self.fetch_enrollments(student_id)
        grade_map = self.load_grade_points()
        history = sorted(self.filter_highest_attempts(all_enrollments, grade_map), key=lambda x: x.course_level)
        passed = [e for e in history if e.completed and not e.failed]
        failed = [e for e in all_enrollments if e.failed]
        gpa_info = self.calculate_gpa(history, grade_map)
        return TranscriptPDF().generate(student_meta, history, passed, failed, gpa_info)
