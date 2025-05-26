document.addEventListener('DOMContentLoaded', function() {
    fetchHoldRestrictions();
    setupEditModal();
    initializeDefaultRestrictions();
});

function fetchHoldRestrictions() {
    showLoading();
    fetch('/api/admin/hold-restrictions')
        .then(response => {
            if (!response.ok) {
                throw new Error('Network response was not ok');
            }
            return response.json();
        })
        .then(data => {
            renderHoldRestrictions(data);
            hideLoading();
        })
        .catch(error => {
            console.error('Error fetching hold restrictions:', error);
            showError(error);
            hideLoading();
        });
}

function renderHoldRestrictions(restrictions) {
    const tableBody = document.querySelector('#holdRestrictionsTable tbody');
    tableBody.innerHTML = '';

    restrictions.forEach(restriction => {
        const row = document.createElement('tr');
        row.innerHTML = `
            <td class="fw-bold">${formatHoldType(restriction.holdType)}</td>
            <td><input type="checkbox" class="form-check-input" ${restriction.blockCourseEnrollment ? 'checked' : ''} disabled></td>
            <td><input type="checkbox" class="form-check-input" ${restriction.blockViewCompletedCourses ? 'checked' : ''} disabled></td>
            <td><input type="checkbox" class="form-check-input" ${restriction.blockStudentAudit ? 'checked' : ''} disabled></td>
            <td><input type="checkbox" class="form-check-input" ${restriction.blockGenerateTranscript ? 'checked' : ''} disabled></td>
            <td><input type="checkbox" class="form-check-input" ${restriction.blockGraduationApplication ? 'checked' : ''} disabled></td>
            <td>
                <button class="btn btn-sm btn-primary edit-btn"
                        data-hold-type="${restriction.holdType}">
                    <i class="bi bi-pencil-square"></i> Edit
                </button>
            </td>
        `;
        tableBody.appendChild(row);
    });

    // Add event listeners to edit buttons
    document.querySelectorAll('.edit-btn').forEach(btn => {
        btn.addEventListener('click', function() {
            const holdType = this.getAttribute('data-hold-type');
            openEditModal(holdType);
        });
    });
}

function formatHoldType(holdType) {
    return holdType.toLowerCase()
        .split('_')
        .map(word => word.charAt(0).toUpperCase() + word.slice(1))
        .join(' ');
}

function openEditModal(holdType) {
    showLoading();
    fetch(`/api/admin/hold-restrictions/${holdType}`)
        .then(response => {
            if (!response.ok) {
                throw new Error('Hold type not found');
            }
            return response.json();
        })
        .then(data => {
            populateEditForm(data);
            hideLoading();
        })
        .catch(error => {
            console.error('Error:', error);
            populateEditForm({
                holdType: holdType,
                blockCourseEnrollment: true,
                blockViewCompletedCourses: true,
                blockStudentAudit: true,
                blockGenerateTranscript: true,
                blockGraduationApplication: true
            });
            hideLoading();
        });
}

function populateEditForm(restriction) {
    document.getElementById('editHoldType').value = restriction.holdType;
    document.getElementById('blockCourseEnrollment').checked = restriction.blockCourseEnrollment;
    document.getElementById('blockViewCompletedCourses').checked = restriction.blockViewCompletedCourses;
    document.getElementById('blockStudentAudit').checked = restriction.blockStudentAudit;
    document.getElementById('blockGenerateTranscript').checked = restriction.blockGenerateTranscript;
    document.getElementById('blockGraduationApplication').checked = restriction.blockGraduationApplication;

    const modal = new bootstrap.Modal(document.getElementById('editRestrictionModal'));
    modal.show();
}

function setupEditModal() {
    const form = document.getElementById('editRestrictionForm');
    form.addEventListener('submit', function(e) {
        e.preventDefault();
        saveRestrictionChanges();
    });
}

function saveRestrictionChanges() {
    const holdType = document.getElementById('editHoldType').value;
    const restrictionData = {
        holdType: holdType,
        blockCourseEnrollment: document.getElementById('blockCourseEnrollment').checked,
        blockViewCompletedCourses: document.getElementById('blockViewCompletedCourses').checked,
        blockStudentAudit: document.getElementById('blockStudentAudit').checked,
        blockGenerateTranscript: document.getElementById('blockGenerateTranscript').checked,
        blockGraduationApplication: document.getElementById('blockGraduationApplication').checked
    };

    showLoading();
    fetch(`/api/admin/hold-restrictions/${holdType}`, {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify(restrictionData)
    })
    .then(response => {
        if (!response.ok) {
            return response.text().then(text => { throw new Error(text) });
        }
        return response.json();
    })
    .then(data => {
        fetchHoldRestrictions();
        const modal = bootstrap.Modal.getInstance(document.getElementById('editRestrictionModal'));
        modal.hide();
        showSuccess('Restrictions updated successfully');
    })
    .catch(error => {
        console.error('Error:', error);
        showError(error.message || 'Failed to update restrictions');
    })
    .finally(() => {
        hideLoading();
    });
}

function initializeDefaultRestrictions() {
    fetch('/api/admin/hold-restrictions')
        .then(response => response.json())
        .then(restrictions => {
            if (restrictions.length === 0) {
                createDefaultRestrictions();
            }
        });
}

function createDefaultRestrictions() {
    const holdTypes = ['UNPAID_FEES', 'UNPAID_REGISTRATION', 'DISCIPLINARY_ISSUES', 'UNSATISFACTORY_ACADEMIC_PROGRESS'];

    holdTypes.forEach(holdType => {
        fetch('/api/admin/hold-restrictions/' + holdType, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                holdType: holdType,
                blockCourseEnrollment: true,
                blockViewCompletedCourses: true,
                blockStudentAudit: true,
                blockGenerateTranscript: true,
                blockGraduationApplication: true
            })
        });
    });
}

function showLoading() {
    document.getElementById('messageArea').innerHTML = `
        <div class="d-flex align-items-center">
            <div class="spinner-border spinner-border-sm me-2" role="status">
                <span class="visually-hidden">Loading...</span>
            </div>
            <span>Loading...</span>
        </div>
    `;
    document.getElementById('messageArea').className = 'alert alert-info d-block';
}

function hideLoading() {
    document.getElementById('messageArea').className = 'alert d-none';
}

function showError(message) {
    const alertDiv = document.getElementById('messageArea');
    alertDiv.innerHTML = `
        <div class="d-flex justify-content-between align-items-center">
            <span><strong>Error!</strong> ${message}</span>
            <button type="button" class="btn-close" onclick="hideError()"></button>
        </div>
    `;
    alertDiv.className = 'alert alert-danger d-block';
}

function hideError() {
    document.getElementById('messageArea').className = 'alert d-none';
}

function showSuccess(message) {
    const alertDiv = document.getElementById('messageArea');
    alertDiv.innerHTML = `
        <div class="d-flex justify-content-between align-items-center">
            <span><strong>Success!</strong> ${message}</span>
            <button type="button" class="btn-close" onclick="hideSuccess()"></button>
        </div>
    `;
    alertDiv.className = 'alert alert-success d-block';
    setTimeout(hideSuccess, 5000);
}

function hideSuccess() {
    document.getElementById('messageArea').className = 'alert d-none';
}