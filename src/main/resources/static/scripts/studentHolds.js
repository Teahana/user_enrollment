// Initialize when DOM is loaded
document.addEventListener('DOMContentLoaded', function() {
    // Fetch and display students
    fetchStudents();

    // Setup event listeners
    setupEventListeners();
});

function setupEventListeners() {
    // Add Hold Modal
    const addHoldModal = document.getElementById('addHoldModal');
    if (addHoldModal) {
        addHoldModal.addEventListener('show.bs.modal', async function(event) {
            const button = event.relatedTarget;
            const studentId = button.getAttribute('data-student-id');
            const studentName = button.getAttribute('data-student-name');

            document.getElementById('addHoldStudentId').value = studentId;
            document.getElementById('addHoldStudentName').value = studentName;

            // Fetch current holds to populate dropdown with only available hold types
            try {
                const response = await fetch(`/api/admin/holds/${studentId}`);
                if (!response.ok) throw new Error('Failed to fetch student holds');
                const student = await response.json();

                const holdTypeSelect = document.getElementById('addHoldType');
                holdTypeSelect.innerHTML = '<option value="">Select a hold type</option>';

                // Get all possible hold types from the enum
                const allHoldTypes = Object.values(OnHoldTypes);

                // Filter out holds the student already has
                const availableHolds = allHoldTypes.filter(hold =>
                    !student.activeHolds.includes(hold)
                );

                if (availableHolds.length === 0) {
                    const option = document.createElement('option');
                    option.value = '';
                    option.textContent = 'No available hold types to add';
                    option.disabled = true;
                    holdTypeSelect.appendChild(option);
                    return;
                }

                availableHolds.forEach(hold => {
                    const option = document.createElement('option');
                    option.value = hold;
                    option.textContent = hold;
                    holdTypeSelect.appendChild(option);
                });
            } catch (error) {
                console.error('Error fetching holds:', error);
                showMessage('Error loading hold types: ' + error.message, 'danger');
            }
        });
    }

    // Remove Hold Modal
    const removeHoldModal = document.getElementById('removeHoldModal');
    if (removeHoldModal) {
        removeHoldModal.addEventListener('show.bs.modal', async function(event) {
            const button = event.relatedTarget;
            const studentId = button.getAttribute('data-student-id');
            const studentName = button.getAttribute('data-student-name');

            document.getElementById('removeHoldStudentId').value = studentId;
            document.getElementById('removeHoldStudentName').value = studentName;

            // Fetch current holds for this student
            try {
                const response = await fetch(`/api/admin/holds/${studentId}`);
                if (!response.ok) throw new Error('Failed to fetch student holds');
                const student = await response.json();

                const holdToRemoveSelect = document.getElementById('holdToRemove');
                holdToRemoveSelect.innerHTML = '<option value="">Select a hold to remove</option>';

                if (student.activeHolds.length === 0) {
                    const option = document.createElement('option');
                    option.value = '';
                    option.textContent = 'No active holds';
                    option.disabled = true;
                    holdToRemoveSelect.appendChild(option);
                    return;
                }

                student.activeHolds.forEach(hold => {
                    const option = document.createElement('option');
                    option.value = hold;
                    option.textContent = hold;
                    holdToRemoveSelect.appendChild(option);
                });
            } catch (error) {
                console.error('Error fetching holds:', error);
                showMessage('Error loading current holds: ' + error.message, 'danger');
            }
        });
    }

    // Add Hold Form Submission
    document.getElementById('addHoldForm')?.addEventListener('submit', function(e) {
        e.preventDefault();
        addHold();
    });

    // Remove Hold Form Submission
    document.getElementById('removeHoldForm')?.addEventListener('submit', function(e) {
        e.preventDefault();
        const studentId = document.getElementById('removeHoldStudentId').value;
        const holdType = document.getElementById('holdToRemove').value;
        removeHold(studentId, holdType);
    });

    // Show History Button
    document.getElementById('showHistoryBtn')?.addEventListener('click', function() {
        showAllHoldHistory();
        populateStudentFilter();
    });

    // Close History Button
    document.getElementById('closeHistoryBtn')?.addEventListener('click', function() {
        document.getElementById('holdHistorySection').style.display = 'none';
        document.getElementById('studentsTable').style.display = 'table';
        document.getElementById('showHistoryBtn').style.display = 'block';
    });
}

// Add a hold to a student
function addHold() {
    const studentId = document.getElementById('addHoldStudentId').value;
    const holdType = document.getElementById('addHoldType').value;

    if (!holdType) {
        showMessage('Please select a hold type', 'danger');
        return;
    }

    fetch('/api/admin/holds/placeHold', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify({
            studentId: studentId,
            holdType: holdType
        })
    })
    .then(response => {
        if (!response.ok) {
            return response.text().then(text => { throw new Error(text) });
        }
        return response.json();
    })
    .then(data => {
        showMessage(data.message || 'Hold added successfully', 'success');
        // Close the modal
        bootstrap.Modal.getInstance(document.getElementById('addHoldModal')).hide();
        // Refresh the student list
        fetchStudents();
    })
    .catch(error => {
        console.error('Error:', error);
        showMessage(error.message || 'Failed to add hold', 'danger');
    });
}

// Remove a hold from a student
function removeHold(studentId, holdType) {
    if (!holdType) {
        showMessage('Please select a hold to remove', 'danger');
        return;
    }

    if (!confirm(`Are you sure you want to remove the ${holdType} hold?`)) return;

    fetch('/api/admin/holds/removeHold', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify({
            studentId: studentId,
            holdType: holdType
        })
    })
    .then(response => {
        if (!response.ok) {
            return response.text().then(text => { throw new Error(text) });
        }
        return response.json();
    })
    .then(data => {
        showMessage(data.message || 'Hold removed successfully', 'success');
        // Close the modal
        bootstrap.Modal.getInstance(document.getElementById('removeHoldModal')).hide();
        // Refresh the student list
        fetchStudents();
    })
    .catch(error => {
        console.error('Error:', error);
        showMessage(error.message || 'Failed to remove hold', 'danger');
    });
}

// Fetch students from API (including those with and without holds)
function fetchStudents() {
    fetch('/api/admin/holds')
        .then(response => {
            if (!response.ok) {
                throw new Error('Network response was not ok');
            }
            return response.json();
        })
        .then(data => {
            const tbody = document.querySelector('#studentsTable tbody');
            if (!tbody) {
                console.error('Table body not found');
                return;
            }
            tbody.innerHTML = '';

            data.forEach(student => {
                const row = document.createElement('tr');
                const holdsList = document.createElement('ul');

                // Create list items for each active hold
                student.activeHolds?.forEach(hold => {
                    const li = document.createElement('li');
                    li.textContent = hold;
                    holdsList.appendChild(li);
                });

                row.innerHTML = `
                    <td>${student.studentNumber}</td>
                    <td>${student.firstName} ${student.lastName}</td>
                    <td>${student.email}</td>
                    <td>${student.activeHolds?.length > 0 ? holdsList.innerHTML : 'None'}</td>
                    <td>
                        <div class="btn-group" role="group">
                            <button class="btn btn-primary btn-sm me-2"
                                data-bs-toggle="modal"
                                data-bs-target="#addHoldModal"
                                data-student-id="${student.studentId}"
                                data-student-name="${student.firstName} ${student.lastName}">
                                Add Hold
                            </button>
                            <button class="btn btn-danger btn-sm"
                                data-bs-toggle="modal"
                                data-bs-target="#removeHoldModal"
                                data-student-id="${student.studentId}"
                                data-student-name="${student.firstName} ${student.lastName}"
                                ${student.activeHolds?.length === 0 ? 'disabled' : ''}>
                                Remove Hold
                            </button>
                        </div>
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

// Show all hold history
function showAllHoldHistory() {
    fetch('/api/admin/holds/history')
        .then(response => {
            if (!response.ok) {
                throw new Error('Network response was not ok');
            }
            return response.json();
        })
        .then(history => {
            renderHistoryTable(history);
            document.getElementById('holdHistorySection').style.display = 'block';
            document.getElementById('studentsTable').style.display = 'none';
            document.getElementById('showHistoryBtn').style.display = 'none';
        })
        .catch(error => {
            console.error('Error:', error);
            showMessage('Error loading hold history: ' + error.message, 'danger');
        });
}

// Render history table
function renderHistoryTable(history) {
    const historyTable = document.querySelector('#holdHistoryTable tbody');
    if (!historyTable) return;
    
    historyTable.innerHTML = '';

    history.forEach(record => {
        const row = document.createElement('tr');
        row.innerHTML = `
            <td>${new Date(record.timestamp).toLocaleString() || 'N/A'}</td>
            <td>${record.studentId || 'N/A'}</td>
            <td>${record.firstName} ${record.lastName}</td>
            <td>${record.action}</td>
            <td>${record.holdType || 'N/A'}</td>
            <td>${record.actionBy || 'System'}</td>
        `;
        historyTable.appendChild(row);
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

// Show a message to the user
function showMessage(message, type) {
    const messageArea = document.getElementById('messageArea');
    if (!messageArea) return;
    
    messageArea.textContent = message;
    messageArea.className = `alert alert-${type} d-block`;
    setTimeout(() => {
        messageArea.className = 'alert d-none';
    }, 5000);
}
