function showSpecialConditionForm() {
    // Hide the normal course list
    document.getElementById("selectCourseListContainer").style.display = "none";
    // Show the special condition form
    document.getElementById("specialConditionForm").style.display = "block";

    // Populate the specialTypeSelect
    populateSpecialTypeSelect();

    // **Now** populate checkboxes for programmes
    populateAdmissionProgrammeCheckboxes();
}


function hideSpecialConditionForm() {
    // Show the normal course list
    document.getElementById("selectCourseListContainer").style.display = "block";
    // Hide the special condition form
    document.getElementById("specialConditionForm").style.display = "none";
}

// Fill the dropdown for special types
function populateSpecialTypeSelect() {
    let sel = document.getElementById("specialTypeSelect");
    if (!sel) return;
    sel.innerHTML = ""; // clear
    specialTypes.forEach(st => {
        let opt = document.createElement("option");
        opt.value = st; // e.g. "ADMISSION_PROGRAMME"
        opt.textContent = st.replaceAll("_", " ");
        sel.appendChild(opt);
    });
    // Trigger an initial onSpecialTypeChange() to update fields
    onSpecialTypeChange();
}
/**
 * Called when the user changes specialType dropdown
 * We show/hide the relevant inputs
 */
function onSpecialTypeChange() {
    let st = document.getElementById("specialTypeSelect").value;
    let completionFields = document.getElementById("completionLevelFields");
    let admissionFields = document.getElementById("admissionProgrammeFields");
    if (!completionFields || !admissionFields) return;

    if (st === "COMPLETION_LEVEL_PERCENT") {
        completionFields.style.display = "block";
        admissionFields.style.display = "none";
    } else if (st === "ADMISSION_PROGRAMME") {
        completionFields.style.display = "none";
        admissionFields.style.display = "block";
    } else {
        // Fallback if additional enum types are ever added
        completionFields.style.display = "none";
        admissionFields.style.display = "none";
    }
}
function addSpecialCondition() {
    let group = findGroupById(currentGroupId, topLevelGroups);
    if (!group) return;

    if (!group.prereqItems) {
        group.prereqItems = [];
    }

    let st = document.getElementById("specialTypeSelect").value;

    let newItem = {
        special: true,
        specialType: st
    };

    if (st === "COMPLETION_LEVEL_PERCENT") {
        let lvl = document.getElementById("targetLevelInput").value;
        let pct = document.getElementById("percentageValueInput").value;
        newItem.targetLevel = parseInt(lvl) || 0;
        newItem.percentageValue = (parseFloat(pct) || 0) / 100.0;
    }
    else if (st === "ADMISSION_PROGRAMME") {
        // Instead of reading from a <select>, gather the checked checkboxes:
        let checkboxes = document.querySelectorAll("#admissionProgrammeCheckboxes input[type='checkbox']:checked");
        let selectedProgIds = [...checkboxes].map(cb => Number(cb.value));
        newItem.programmeIds = selectedProgIds;
    }

    group.prereqItems.push(newItem);

    // Hide the modal or revert to the course list, then re-render
    let modalEl = document.getElementById("selectCourseModal");
    let childModal = bootstrap.Modal.getInstance(modalEl);
    if (childModal) {
        childModal.hide();
        removeExtraBackdrops();
    }

    renderGroupCourses(group.id);
    buildExpressionPreview(isEditMode);
}

function populateAdmissionProgrammeCheckboxes() {
    let container = document.getElementById("admissionProgrammeCheckboxes");
    if (!container) return;

    container.innerHTML = ""; // Clear anything that might exist

    // allProgrammes = fetched from the server. For each programme, create a checkbox + label
    allProgrammes.forEach(p => {
        let wrapper = document.createElement("div");
        wrapper.classList.add("form-check", "mb-1");

        // The checkbox itself
        let checkbox = document.createElement("input");
        checkbox.type = "checkbox";
        checkbox.classList.add("form-check-input");
        checkbox.id = `progChk_${p.id}`;
        checkbox.value = p.id;

        // The label
        let label = document.createElement("label");
        label.classList.add("form-check-label");
        label.setAttribute("for", `progChk_${p.id}`);
        label.textContent = `${p.programmeCode} - ${p.name}`;

        wrapper.appendChild(checkbox);
        wrapper.appendChild(label);
        container.appendChild(wrapper);
    });
}
function openEditModal(button) {
    const courseId         = button.getAttribute('data-course-id');
    const courseCode       = button.getAttribute('data-course-code');
    const title            = button.getAttribute('data-title');
    const description      = button.getAttribute('data-description');
    const creditPoints     = button.getAttribute('data-creditpoints');
    const level            = button.getAttribute('data-level');
    const offeredSem1      = (button.getAttribute('data-offeredsem1') === 'true');
    const offeredSem2      = (button.getAttribute('data-offeredsem2') === 'true');

    // Fix: Get programme codes (like "BSE BNS")
    let currentProgrammeCodes = [];
    const rawProgs = button.getAttribute('data-programmes');
    if (rawProgs) {
        currentProgrammeCodes = rawProgs.trim().split(/\s+/);
    }

    document.getElementById('editCourseId').value      = courseId;
    document.getElementById('editCourseCode').value    = courseCode;
    document.getElementById('editTitle').value         = title;
    document.getElementById('editDescription').value   = description;
    document.getElementById('editCreditPoints').value  = creditPoints;
    document.getElementById('editLevel').value         = level;
    document.getElementById('editOfferedSem1').checked = offeredSem1;
    document.getElementById('editOfferedSem2').checked = offeredSem2;

    // Programmes checkboxes
    const container = document.getElementById('editProgrammeList');
    container.innerHTML = '';

    allProgrammes.forEach(prog => {
        const isChecked = currentProgrammeCodes.includes(prog.programmeCode);
        const chkId = 'editProgChk_' + prog.id;
        const div = document.createElement('div');
        div.className = 'form-check';

        div.innerHTML = `
            <input type="checkbox" class="form-check-input"
                   name="programmeIds"
                   value="${prog.id}"
                   id="${chkId}" ${isChecked ? 'checked' : ''}>
            <label class="form-check-label" for="${chkId}">
                ${prog.programmeCode} - ${prog.name}
            </label>
        `;
        container.appendChild(div);
    });

    const editModal = new bootstrap.Modal(document.getElementById('editCourseModal'));
    editModal.show();
}
