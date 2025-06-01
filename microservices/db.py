import mysql.connector

def get_db_connection():
    return mysql.connector.connect(
        host="localhost",
        database="enrollment_database",
        user="root",
        password="12345"
    )
