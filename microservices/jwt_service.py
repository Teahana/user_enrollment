import base64
import jwt
from flask import request, jsonify, g
from functools import wraps
from db import get_db_connection

# Must match Spring Boot secret
SECRET_B64 = "AWjoPHjE9PYBWKO50U5lc9XNqYkuQas+Nf6fGadw9tU="
SECRET_BYTES = base64.b64decode(SECRET_B64)
print("FLASK JWT KEY (base-64url):",
      base64.urlsafe_b64encode(SECRET_BYTES).rstrip(b"=").decode())

def decode_jwt(token):
    return jwt.decode(token, SECRET_BYTES, algorithms=["HS256"])

def require_jwt(view_func):
    @wraps(view_func)
    def wrapper(*args, **kwargs):
        auth_header = request.headers.get("Authorization")
        print("Auth header received:", auth_header)  # Debug

        if not auth_header or not auth_header.startswith("Bearer "):
            return jsonify({"error": "Missing or invalid Authorization header"}), 401

        token = auth_header.split(" ")[1]
        print("Token extracted:", token)  # Debug

        try:
            decoded = decode_jwt(token)
            g.user = decoded  # ✅ Request-scope assignment
            print("Decoded JWT:", decoded)  # Debug
        except jwt.ExpiredSignatureError:
            return jsonify({"error": "Token expired"}), 401
        except jwt.InvalidTokenError as e:
            print("Invalid token error:", str(e))  # Debug
            return jsonify({"error": "Invalid token"}), 401

        return view_func(*args, **kwargs)
    return wrapper

def get_student_id_from_request():
    if not hasattr(g, "user") or "sub" not in g.user:
        raise ValueError("JWT does not contain a subject (user identifier)")

    email = g.user["sub"]
    return fetch_student_id_by_email(email)

def fetch_student_id_by_email(email: str) -> int:
    query = "SELECT s.id FROM student s JOIN users u ON s.id = u.id WHERE u.email = %s LIMIT 1"
    conn = get_db_connection()
    cursor = conn.cursor()
    cursor.execute(query, (email,))
    result = cursor.fetchone()
    cursor.close()
    conn.close()

    if not result:
        raise ValueError(f"No student found with email {email}")

    return result[0]
