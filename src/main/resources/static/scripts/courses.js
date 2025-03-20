// Global variables
let allCourses = []; // Store all courses for filtering
let selectedCourseId;
let prerequisiteGroups = []; // Stores groups [{id, type, courses: [], relationToNextGroup: null}]

document.addEventListener("DOMContentLoaded", function () {
    fetch("/api/admin/getAllCourses")
        .then(response => response.json())
        .then(data => {
            allCourses = data;
            console.log("All courses:", allCourses);
        });

    document.querySelectorAll(".addPrereqBtn").forEach(button => {
        button.addEventListener("click", handleAddPrerequisites);
    });

    let searchInput = document.getElementById("searchInput");
    if (searchInput) {
        searchInput.addEventListener("input", filterCourses);
    }

    let searchPrereqInput = document.getElementById("searchPrereqInput");
    if (searchPrereqInput) {
        searchPrereqInput.addEventListener("input", filterPrerequisites);
    }

    let form = document.getElementById("addPrerequisiteForm");
    if (form) {
        form.addEventListener("submit", submitPrerequisiteForm);
    }
});

/**
 * Handles opening the Add Prerequisites modal
 */
function handleAddPrerequisites(event) {
    let button = event.currentTarget;
    selectedCourseId = button.getAttribute("data-course-id");
    document.getElementById("selectedCourseId").value = selectedCourseId;
    document.getElementById("prerequisiteGroupsContainer").innerHTML = "";
    prerequisiteGroups = [];
    addNewPrerequisiteGroup();
}

/**
 * Adds a new prerequisite group (AND/OR)
 */
function addNewPrerequisiteGroup() {
    let groupId = prerequisiteGroups.length + 1;
    let prevGroupId = groupId - 1;

    prerequisiteGroups.push({ id: groupId, type: "AND", courses: [], relationToNextGroup: null });

    let container = document.getElementById("prerequisiteGroupsContainer");

    let groupDiv = document.createElement("div");
    groupDiv.classList.add("mb-3", "p-2", "border", "rounded");
    groupDiv.innerHTML = `
        <label class="form-label">Group ${groupId}</label>
        <select class="form-select mb-2" onchange="updatePrerequisiteType(${groupId}, this.value)">
            <option value="AND">ALL courses in this group must be passed</option>
            <option value="OR">ANY one of these courses must be passed</option>
        </select>
        <div id="group-${groupId}-courses"></div>
        <button type="button" class="btn btn-sm btn-outline-secondary mt-2" onclick="addCourseToGroup(${groupId})">+ Add Course</button>
    `;

    container.appendChild(groupDiv);

    if (prevGroupId > 0) {
        let relationDiv = document.createElement("div");
        relationDiv.classList.add("text-center", "my-2");
        relationDiv.innerHTML = `
            <select class="form-select d-inline w-auto" onchange="updateGroupRelation(${prevGroupId}, this.value)">
                <option value="AND">AND</option>
                <option value="OR">OR</option>
            </select>
        `;
        container.insertBefore(relationDiv, groupDiv);
    }
}

/**
 * Updates the prerequisite type (AND/OR) for a group
 */
function updatePrerequisiteType(groupId, type) {
    let group = prerequisiteGroups.find(g => g.id === groupId);
    if (group) group.type = type;
}

/**
 * Updates the relation between groups (AND/OR)
 */
function updateGroupRelation(groupId, relation) {
    let group = prerequisiteGroups.find(g => g.id === groupId);
    if (group) group.relationToNextGroup = relation;
}

/**
 * Adds a course to a specific prerequisite group
 */
function addCourseToGroup(groupId) {
    let group = prerequisiteGroups.find(g => g.id === groupId);
    if (!group) return;

    let availableCourses = allCourses.filter(course =>
        course.id !== Number(selectedCourseId) && !group.courses.includes(course.id)
    );

    if (availableCourses.length === 0) return alert("No more courses to add!");

    let select = document.createElement("select");
    select.classList.add("form-select", "mb-2");
    select.innerHTML = availableCourses.map(course =>
        `<option value="${course.id}">${course.courseCode}</option>`
    ).join("");

    select.addEventListener("change", function () {
        let selectedCourseId = Number(this.value);
        if (!group.courses.includes(selectedCourseId)) {
            group.courses.push(selectedCourseId);
        }
    });

    document.getElementById(`group-${groupId}-courses`).appendChild(select);
}

/**
 * Filters the course list based on user input.
 */
function filterCourses() {
    let searchTerm = this.value.toLowerCase();
    let tableRows = document.querySelectorAll("#courseTableBody tr");

    tableRows.forEach(row => {
        let courseCodeCell = row.querySelector(".courseCode");
        let courseCode = courseCodeCell.textContent.toLowerCase();

        row.style.display = courseCode.includes(searchTerm) ? "" : "none";
    });
}

/**
 * Filters the prerequisite selection inside the modal.
 */
function filterPrerequisites() {
    let searchTerm = this.value.toLowerCase();
    let checkboxesDiv = document.getElementById("prerequisitesCheckboxes");

    let sortedCourses = allCourses
        .filter(course =>
            course.id !== Number(selectedCourseId) &&
            (!searchTerm || course.courseCode.toLowerCase().includes(searchTerm))
        )
        .sort((a, b) => {
            let aSelected = prerequisiteGroups.some(group => group.courses.includes(a.id));
            let bSelected = prerequisiteGroups.some(group => group.courses.includes(b.id));
            return bSelected - aSelected;
        });

    populatePrerequisiteCheckboxes(sortedCourses);
}

/**
 * Shows a message in a specified div.
 */
function showMessage(divId, message, type) {
    let div = document.getElementById(divId);
    div.innerText = message;
    div.style.display = "block";
    div.classList.add(type === "success" ? "alert-success" : "alert-danger");

    setTimeout(() => {
        div.style.display = "none";
        div.classList.remove("alert-success", "alert-danger");
    }, 2000);
}

/**
 * Handles form submission and adds prerequisite groups data
 */
function submitPrerequisiteForm(event) {
    let hiddenInput = document.createElement("input");
    hiddenInput.type = "hidden";
    hiddenInput.name = "prerequisites";
    hiddenInput.value = JSON.stringify(prerequisiteGroups);
    event.target.appendChild(hiddenInput);
}

// // Global variables
// let allCourses = []; // Store all courses for filtering
// let selectedCourseId;
// let prerequisiteGroups = []; // Stores groups [{id, type, courses: []}]
//
// document.addEventListener("DOMContentLoaded", function () {
//     // Fetch all courses when the page loads
//     fetch("/api/admin/getAllCourses")
//         .then(response => response.json())
//         .then(data => {
//             allCourses = data; // Store the courses globally
//             console.log("All courses:", allCourses);
//         });
//
//     // Attach event listeners
//     document.querySelectorAll(".addPrereqBtn").forEach(button => {
//         button.addEventListener("click", handleAddPrerequisites);
//     });
//
//     let searchInput = document.getElementById("searchInput");
//     if (searchInput) {
//         searchInput.addEventListener("input", filterCourses);
//     }
//
//     let searchPrereqInput = document.getElementById("searchPrereqInput");
//     if (searchPrereqInput) {
//         searchPrereqInput.addEventListener("input", filterPrerequisites);
//     }
//
//     let form = document.getElementById("addPrerequisiteForm");
//     if (form) {
//         form.addEventListener("submit", submitPrerequisiteForm);
//     }
// });
//
// /**
//  * Handles opening the Add Prerequisites modal
//  */
// function handleAddPrerequisites(event) {
//     let button = event.currentTarget;
//     selectedCourseId = button.getAttribute("data-course-id");
//     let courseCode = button.getAttribute("data-course-code");
//
//     document.getElementById("selectedCourseId").value = selectedCourseId;
//     document.getElementById("selectedCourseName").innerText = courseCode;
//
//     prerequisiteGroups = []; // Reset groups
//     document.getElementById("prerequisiteGroupsContainer").innerHTML = ""; // Clear UI
//     addNewPrerequisiteGroup(); // Start with one default group
// }
//
// /**
//  * Adds a new prerequisite group (AND/OR)
//  */
// function addNewPrerequisiteGroup() {
//     let groupId = prerequisiteGroups.length + 1;
//     prerequisiteGroups.push({ id: groupId, type: "AND", courses: [] });
//
//     let container = document.getElementById("prerequisiteGroupsContainer");
//
//     let groupDiv = document.createElement("div");
//     groupDiv.classList.add("mb-3", "p-2", "border", "rounded");
//     groupDiv.innerHTML = `
//         <label class="form-label">Group ${groupId} - Required Courses</label>
//         <select class="form-select mb-2" onchange="updatePrerequisiteType(${groupId}, this.value)">
//             <option value="AND">ALL courses in this group must be passed</option>
//             <option value="OR">ANY one of these courses must be passed</option>
//         </select>
//         <div id="group-${groupId}-courses"></div>
//         <button type="button" class="btn btn-sm btn-outline-secondary mt-2" onclick="addCourseToGroup(${groupId})">+ Add Course</button>
//     `;
//
//     container.appendChild(groupDiv);
// }
//
// /**
//  * Updates the prerequisite type (AND/OR) for a group
//  */
// function updatePrerequisiteType(groupId, type) {
//     let group = prerequisiteGroups.find(g => g.id === groupId);
//     if (group) {
//         group.type = type;
//     }
// }
//
// /**
//  * Adds a course to a specific prerequisite group
//  */
// function addCourseToGroup(groupId) {
//     let group = prerequisiteGroups.find(g => g.id === groupId);
//     if (!group) return;
//
//     let availableCourses = allCourses.filter(course =>
//         course.id !== Number(selectedCourseId) &&
//         !group.courses.includes(course.id) // Prevent duplicates
//     );
//
//     if (availableCourses.length === 0) return alert("No more courses to add!");
//
//     let select = document.createElement("select");
//     select.classList.add("form-select", "mb-2");
//     select.innerHTML = availableCourses.map(course =>
//         `<option value="${course.id}">${course.courseCode}</option>`
//     ).join("");
//
//     select.addEventListener("change", function () {
//         let selectedCourseId = Number(this.value);
//         if (!group.courses.includes(selectedCourseId)) {
//             group.courses.push(selectedCourseId);
//         }
//     });
//
//     document.getElementById(`group-${groupId}-courses`).appendChild(select);
// }
//
// /**
//  * Handles form submission and adds prerequisite groups data
//  */
// function submitPrerequisiteForm(event) {
//     let hiddenInput = document.createElement("input");
//     hiddenInput.type = "hidden";
//     hiddenInput.name = "prerequisites";
//     hiddenInput.value = JSON.stringify(prerequisiteGroups);
//
//     event.target.appendChild(hiddenInput);
// }
//
// /**
//  * Filters the course list based on user input.
//  */
// function filterCourses() {
//     let searchTerm = this.value.toLowerCase();
//     let tableRows = document.querySelectorAll("#courseTableBody tr");
//
//     tableRows.forEach(row => {
//         let courseCodeCell = row.querySelector(".courseCode");
//         let courseCode = courseCodeCell.textContent.toLowerCase();
//
//         row.style.display = courseCode.includes(searchTerm) ? "" : "none";
//     });
// }
//
// /**
//  * Filters the prerequisite selection inside the modal.
//  */
// function filterPrerequisites() {
//     let searchTerm = this.value.toLowerCase();
//     let checkboxesDiv = document.getElementById("prerequisitesCheckboxes");
//
//     let sortedCourses = allCourses
//         .filter(course =>
//             course.id !== Number(selectedCourseId) &&
//             (!searchTerm || course.courseCode.toLowerCase().includes(searchTerm))
//         )
//         .sort((a, b) => {
//             let aSelected = prerequisiteGroups.some(group => group.courses.includes(a.id));
//             let bSelected = prerequisiteGroups.some(group => group.courses.includes(b.id));
//             return bSelected - aSelected; // Selected prerequisites move up
//         });
//
//     populatePrerequisiteCheckboxes(sortedCourses);
// }
//
// /**
//  * Shows a message in a specified div.
//  */
// function showMessage(divId, message, type) {
//     let div = document.getElementById(divId);
//     div.innerText = message;
//     div.style.display = "block";
//     div.classList.add(type === "success" ? "alert-success" : "alert-danger");
//
//     setTimeout(() => {
//         div.style.display = "none";
//         div.classList.remove("alert-success", "alert-danger");
//     }, 2000);
// }
//
