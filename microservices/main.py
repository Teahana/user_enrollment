from flask import Flask, send_file
from flask_cors import CORS
from io import BytesIO

from jwt_service import require_jwt, get_student_id_from_request
from transcript_service import TranscriptService
import check_requirements

app = Flask(__name__)
#CORS(app)  # Enable CORS for all routes
CORS(app, origins=["http://localhost"])

service = TranscriptService()

@app.route("/completedCourses/download", methods=["POST"])
@require_jwt
def download_pdf():
    try:
        student_id = get_student_id_from_request()
        print("student id:", student_id)
    except Exception as e:
        return {"error": str(e)}, 400

    pdf_bytes = service.generate_transcript(student_id)
    return send_file(
        BytesIO(pdf_bytes),
        download_name="transcript.pdf",
        as_attachment=True,
        mimetype="application/pdf"
    )

if __name__ == "__main__":
    app.run(port=5001)
