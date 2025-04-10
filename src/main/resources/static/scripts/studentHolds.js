// Initialize when DOM is loaded
document.addEventListener('DOMContentLoaded', function() {
    // Fetch and display students
    fetchStudents();

    // Handle modal display
    const placeHoldModal = document.getElementById('placeHoldModal');
    if (placeHoldModal) {
        placeHoldModal.addEventListener('show.bs.modal', function(event) {
            const button = event.relatedTarget;
            const studentId = button.getAttribute('data-student-id');
            const studentName = button.getAttribute('data-student-name');

            document.getElementById('modalStudentId').value = studentId;
            document.getElementById('studentName').value = studentName;
        });
    }

    // Handle place hold form submission
    document.getElementById('placeHoldForm').addEventListener('submit', function(e) {
        e.preventDefault();
        placeHold();
    });

    //Handle the holds history
    document.getElementById('showHistoryBtn').addEventListener('click', function() {
        showAllHoldHistory();
        populateStudentFilter();
    });

    document.getElementById('closeHistoryBtn').addEventListener('click', function() {
        document.getElementById('holdHistorySection').style.display = 'none';
        document.getElementById('studentsTable').style.display = 'table';
        document.getElementById('showHistoryBtn').style.display = 'block';
    });
});

// Fetch students from API
function fetchStudents() {
    fetch('/api/admin/holds')
        .then(response => {
            if (!response.ok) {
                throw new Error('Network response was not ok');
            }
            return response.json();
        })
        .then(data => {
            if (!Array.isArray(data)) {
                // If it's an error object
                if (data.error) {
                    throw new Error(data.error);
                }
                // If it's a single object, wrap it in an array
                if (typeof data === 'object') {
                    data = [data];
                } else {
                    throw new Error('Unexpected response format');
                }
            }

            const tbody = document.querySelector('#studentsTable tbody');
            tbody.innerHTML = '';

            data.forEach(student => {
                const row = document.createElement('tr');
                row.innerHTML = `
                    <td>${student.studentNumber}</td>
                    <td>${student.firstName} ${student.lastName}</td>
                    <td>${student.email}</td>
                    <td>
                        <span class="badge ${student.onHold ? 'bg-danger' : 'bg-success'}">
                            ${student.onHold ? 'Yes' : 'No'}
                        </span>
                    </td>
                    <td>${student.holdType || 'N/A'}</td>
                    <td>
                        ${student.onHold ?
                            `<button onclick="removeHold('${student.studentId}')" class="btn btn-success btn-sm">Remove Hold</button>` :
                            `<button class="btn btn-warning btn-sm"
                                data-bs-toggle="modal"
                                data-bs-target="#placeHoldModal"
                                data-student-id="${student.studentId}"
                                data-student-name="${student.firstName} ${student.lastName}">
                                Place Hold
                            </button>`}
                    </td>
                `;
                tbody.appendChild(row);
            });
        })
        .catch(error => {
            console.error('Error:', error);
            showMessage('Error fetching students: ' + error.message, 'danger');
        });
}

// Place a hold on a student
function placeHold() {
    const studentId = document.getElementById('modalStudentId').value;
    const holdType = document.getElementById('holdType').value;

    fetch('/api/admin/holds/place', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify({
            studentId: studentId,
            holdType: holdType
        })
    })
    .then(response => response.json())
    .then(data => {
        if (data.error) {
            throw new Error(data.error);
        }
        showMessage(data.message, 'success');
        document.getElementById('placeHoldModal').querySelector('.btn-close').click();
        fetchStudents();
    })
    .catch(error => showMessage(error.message, 'danger'));
}

// Remove a hold from a student
function removeHold(studentId) {
    if (!confirm('Are you sure you want to remove this hold?')) return;

    fetch('/api/admin/holds/remove', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify({
            studentId: studentId
        })
    })
    .then(response => response.json())
    .then(data => {
        if (data.error) {
            throw new Error(data.error);
        }
        showMessage(data.message, 'success');
        fetchStudents();
    })
    .catch(error => showMessage(error.message, 'danger'));
}

// Show a message to the user
function showMessage(message, type) {
    const messageArea = document.getElementById('messageArea');
    messageArea.textContent = message;
    messageArea.className = `alert alert-${type} d-block`;
    setTimeout(() => {
        messageArea.className = 'alert d-none';
    }, 5000);
}

function showAllHoldHistory() {
    fetch('/api/admin/holds/history')
        .then(response => {
            if (!response.ok) {
                throw new Error('Network response was not ok');
            }
            return response.json();
        })
        .then(history => {
            if (!Array.isArray(history)) {
                console.error('Expected array but got:', history);
                throw new Error('Unexpected response format - expected array');
            }
            renderHistoryTable(history);
            document.getElementById('holdHistorySection').style.display = 'block';
            document.getElementById('studentsTable').style.display = 'none';
            document.getElementById('showHistoryBtn').style.display = 'none';
        })
        .catch(error => {
            console.error('Error fetching history:', error);
            showMessage('Error loading hold history: ' + error.message, 'danger');
        });
}

function populateStudentFilter() {
    fetch('/api/admin/holds/history/filter')
        .then(response => response.json())
        .then(students => {
            const filter = document.getElementById('studentFilter');

            // Clear existing options
            filter.innerHTML = '';

            const defaultOption = document.createElement('option');
            defaultOption.value = '';
            defaultOption.textContent = 'All Students';
            filter.appendChild(defaultOption);

            // Track unique students using a Set
            const uniqueStudents = new Set();

            students.forEach(student => {
                const studentKey = `${student.studentId}-${student.firstName}-${student.lastName}`;
                if (!uniqueStudents.has(studentKey)) {
                    uniqueStudents.add(studentKey);

                    const option = document.createElement('option');
                    option.value = student.studentId;
                    option.textContent = `${student.firstName} ${student.lastName} (${student.studentNumber})`;
                    filter.appendChild(option);
                }
            });

            filter.addEventListener('change', function() {
                const studentId = this.value;
                if (studentId) {
                    fetch(`/api/admin/holds/history/${studentId}`)
                        .then(response => response.json())
                        .then(history => renderHistoryTable(history))
                        .catch(error => console.error('Error:', error));
                } else {
                    showAllHoldHistory();
                }
            });
        })
        .catch(error => {
            console.error('Error:', error);
            filter.innerHTML = '<option value="">Error loading students</option>';
        });
}

function renderHistoryTable(history) {
    const historyTable = document.querySelector('#holdHistoryTable tbody');
    historyTable.innerHTML = '';

    if (!history || !history.forEach) {
        console.error('History is not iterable:', history);
        showMessage('Error: History data is not in the expected format', 'danger');
        return;
    }

    history.forEach(record => {
        const row = document.createElement('tr');
        row.innerHTML = `
            <td>${new Date(record.timestamp).toLocaleString()}</td>
            <td>${record.studentId || 'N/A'}</td>
            <td>${record.firstName} ${record.lastName}</td>
            <td>${record.email}</td>
            <td>${record.action}</td>
            <td>${record.holdType}</td>
        `;
        historyTable.appendChild(row);
    });
}