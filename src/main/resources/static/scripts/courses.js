// Global variables
let allCourses = []; // Store all courses for filtering
let selectedCourseId;
let selectedPrereqCodes = []; // Store already selected prerequisites
let selectedPrereqIds = new Set(); // Track selected prerequisite IDs

document.addEventListener("DOMContentLoaded", function () {
    // Fetch all courses when the page loads
    fetch("/api/admin/getAllCourses")
        .then(response => response.json())
        .then(data => {
            allCourses = data; // Store the courses globally
            console.log("All courses:", allCourses);
        });

    // Attach event listeners
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
        form.addEventListener("submit", submitAddPrerequisiteForm);
    }
});

function handleAddPrerequisites(event) {
    let button = event.currentTarget;
    selectedCourseId = button.getAttribute("data-course-id");
    let courseCode = button.getAttribute("data-course-code");

    document.getElementById("selectedCourseId").value = selectedCourseId;
    document.getElementById("selectedCourseName").innerText = courseCode;

    // Get already selected prerequisites for this course from the table
    let row = button.closest("tr");
    selectedPrereqCodes = Array.from(row.querySelectorAll("td:nth-child(9) span"))
        .map(span => span.textContent.trim()) // Extract text content
        .filter(Boolean);

    console.log("Already selected prerequisites:", selectedPrereqCodes);

    // Filter courses: exclude selected course and already selected prerequisites
    let availablePrereqs = allCourses.filter(course =>
        course.id !== Number(selectedCourseId) && !selectedPrereqCodes.includes(course.courseCode)
    );

    selectedPrereqIds.clear(); // Reset selected prerequisites tracking
    populatePrerequisiteCheckboxes(availablePrereqs);
}

/**
 * Populates the prerequisite checkboxes inside the modal.
 */
function populatePrerequisiteCheckboxes(courses) {
    let checkboxesDiv = document.getElementById("prerequisitesCheckboxes");
    checkboxesDiv.innerHTML = ""; // Clear previous checkboxes

    courses.forEach(course => {
        let checkbox = document.createElement("input");
        checkbox.type = "checkbox";
        checkbox.value = course.id;
        checkbox.name = "prerequisites";
        checkbox.checked = selectedPrereqIds.has(course.id);

        checkbox.addEventListener("change", function () {
            if (this.checked) {
                selectedPrereqIds.add(course.id);
            } else {
                selectedPrereqIds.delete(course.id);
            }
        });

        let label = document.createElement("label");
        label.textContent = ` ${course.courseCode}`;
        label.prepend(checkbox);

        let div = document.createElement("div");
        div.appendChild(label);
        checkboxesDiv.appendChild(div);
    });
}

/**
 * Prepares the form for submission.
 */
function submitAddPrerequisiteForm() {
    let selectedPrereqs = Array.from(document.querySelectorAll("#prerequisitesCheckboxes input:checked"))
        .map(checkbox => Number(checkbox.value));

    if (selectedPrereqs.length === 0) {
        showMessage("errorDiv", "No prerequisites selected.", "error");
        return false; // Prevent submission if no prerequisites are selected
    }

    // Create hidden input fields for each prerequisite
    let form = document.getElementById("addPrerequisiteForm");
    let hiddenInputsContainer = document.getElementById("prerequisitesInputsContainer");
    hiddenInputsContainer.innerHTML = ""; // Clear old inputs

    selectedPrereqs.forEach(id => {
        let input = document.createElement("input");
        input.type = "hidden";
        input.name = "prerequisites";
        input.value = id;
        hiddenInputsContainer.appendChild(input);
    });

    return true; // Allow normal form submission
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

    // Convert the original course list into an array that maintains selected courses at the top
    let sortedCourses = allCourses
        .filter(course =>
            course.id !== Number(selectedCourseId) &&
            (!searchTerm || course.courseCode.toLowerCase().includes(searchTerm))
        )
        .sort((a, b) => {
            // Keep selected prerequisites at the top
            let aSelected = selectedPrereqIds.has(a.id);
            let bSelected = selectedPrereqIds.has(b.id);
            return bSelected - aSelected; // True (1) moves up, False (0) moves down
        });

    // Repopulate the checkboxes
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
