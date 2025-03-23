document.addEventListener("DOMContentLoaded", function () {
    // Load courses for the dropdown when the modal is opened
    document.querySelectorAll(".linkCourseBtn").forEach(button => {
        button.addEventListener("click", function () {
            let programmeCode = this.getAttribute("data-programme-code");
            document.getElementById("selectedProgrammeCode").value = programmeCode;

            // Fetch courses NOT linked to the selected programme
            fetch(`/api/admin/getCoursesNotLinkedToProgramme?programmeCode=${programmeCode}`)
                .then(response => response.json())
                .then(data => {
                    let dropdown = document.getElementById("coursesDropdown");
                    dropdown.innerHTML = "";
                    data.forEach(course => {
                        let option = document.createElement("option");
                        option.value = course.courseCode;
                        option.textContent = course.courseCode + " - " + course.title;
                        dropdown.appendChild(option);
                    });
                });
        });
    });

    // Handle removing a course from a programme
    document.querySelectorAll(".removeCourseBtn").forEach(button => {
        button.addEventListener("click", function () {
            let courseCode = this.getAttribute("data-course-code");
            let programmeCode = this.getAttribute("data-programme-code");

            fetch(`/api/admin/removeCourseFromProgramme?courseCode=${courseCode}&programmeCode=${programmeCode}`, {
                method: "DELETE"
            }).then(response => {
                if (response.ok) {
                    location.reload(); // Refresh the page to reflect changes
                } else {
                    alert("Failed to remove course.");
                }
            });
        });
    });
});