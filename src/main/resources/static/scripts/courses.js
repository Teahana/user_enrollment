// ============================================================================
// Global variables
// ============================================================================
let allCourses = [];          // All available courses from the API
let selectedCourseId;         // The Course ID for which we’re adding prerequisites
let topLevelGroups = [];      // Array of top-level groups
let groupCounter = 1;         // Unique group ID generator
let currentGroupId = null;    // The group we're currently adding a course to

// ============================================================================
// On page load, fetch courses, attach event listeners
// ============================================================================
document.addEventListener("DOMContentLoaded", function() {
    // Fetch all courses
    fetch("/api/admin/getAllCourses")
        .then(response => response.json())
        .then(data => {
            allCourses = data;
        })
        .catch(err => console.error("Error fetching courses:", err));

    // Attach event listeners to "Add Prereq" buttons
    document.querySelectorAll(".addPrereqBtn").forEach(button => {
        button.addEventListener("click", handleAddPrerequisites);
    });

    // Optional: attach event listener for main search input
    let searchInput = document.getElementById("searchInput");
    if (searchInput) {
        searchInput.addEventListener("input", filterCourses);
    }
});

// ============================================================================
// Open the "Add Prerequisites" modal for a given course
// ============================================================================
function handleAddPrerequisites(event) {
    let button = event.currentTarget;
    selectedCourseId = button.getAttribute("data-course-id");
    document.getElementById("selectedCourseId").value = selectedCourseId;
    document.getElementById("selectedCourseName").textContent = button.getAttribute("data-course-code");

    // Clear existing content before re-rendering
    document.getElementById("prerequisiteGroupsContainer").innerHTML = "";

    topLevelGroups = [];
    groupCounter = 1;
    currentGroupId = null;

    addNewPrerequisiteGroup();
    buildExpressionPreview();
}

// ============================================================================
// Add a new top-level group
// parentGroupId is null => top-level, else it is a subgroup
// ============================================================================
function addNewPrerequisiteGroup(parentGroupId = null) {
    let groupId = generateGroupId();

    let newGroup = {
        id: groupId,
        type: "AND",
        courses: [],
        subGroups: [],
        operatorToNext: "AND"  // 🆕 Ensure subgroups also have an operator
    };

    if (!parentGroupId) {
        topLevelGroups.push(newGroup);
        renderTopLevelGroups();
    } else {
        let parent = findGroupById(parentGroupId, topLevelGroups);
        if (parent) {
            parent.subGroups.push(newGroup);
            renderSubgroups(parentGroupId);
        }
    }

    buildExpressionPreview();
}


// ============================================================================
// Remove a group (excluding the last top-level group)
// ============================================================================
function removeGroup(groupId) {
    let isTopLevel = topLevelGroups.some(g => g.id === groupId);

    if (isTopLevel) {
        if (topLevelGroups.length === 1) {
            alert("You must have at least one prerequisite group.");
            return;
        }
        topLevelGroups = topLevelGroups.filter(g => g.id !== groupId);
    } else {
        removeGroupRecursive(groupId, topLevelGroups);
    }

    renumberGroups(); // 🆕 Renumber all groups after deletion
    renderTopLevelGroups();
    buildExpressionPreview();
}



function removeGroupRecursive(groupId, groupArr) {
    for (let i = 0; i < groupArr.length; i++) {
        let group = groupArr[i];

        let index = group.subGroups.findIndex(sub => sub.id === groupId);
        if (index !== -1) {
            group.subGroups.splice(index, 1);
            renumberGroups(); // 🆕 Renumber after subgroup deletion
            return;
        }

        // Recursive call for deeper nested groups
        removeGroupRecursive(groupId, group.subGroups);
    }
}

function renumberGroups() {
    let newCounter = 1;

    function traverseAndRenumber(groups) {
        groups.forEach(group => {
            group.id = newCounter++; // Assign new ID sequentially
            traverseAndRenumber(group.subGroups);
        });
    }

    traverseAndRenumber(topLevelGroups);
    groupCounter = newCounter; // 🆕 Update groupCounter to reflect actual groups
}



// Create a subgroup inside an existing group
function addSubGroup(parentGroupId) {
    addNewPrerequisiteGroup(parentGroupId);
}

// ============================================================================
// Render ALL top-level groups in the container, including the operator dropdown
// that links each group to the next one
// ============================================================================
function renderTopLevelGroups() {
    let container = document.getElementById("prerequisiteGroupsContainer");
    container.innerHTML = "";

    topLevelGroups.forEach((groupObj, index) => {
        let groupBox = document.createElement("div");
        groupBox.classList.add("group-box", "mb-3");
        groupBox.dataset.groupId = groupObj.id;

        groupBox.innerHTML = `
          <div class="d-flex justify-content-between align-items-center mb-2">
            <span><strong>Group ${groupObj.id}</strong></span>
            <div>
                <select class="form-select form-select-sm d-inline-block" style="width:150px;"
                        onchange="updateGroupType(${groupObj.id}, this.value)">
                    <option value="AND" ${groupObj.type === "AND" ? "selected" : ""}>ALL (AND)</option>
                    <option value="OR" ${groupObj.type === "OR" ? "selected" : ""}>ANY (OR)</option>
                </select>
                <button type="button" class="btn btn-sm btn-outline-danger" onclick="removeGroup(${groupObj.id})">✖</button>
            </div>
          </div>
          
          <div class="mb-2" id="group-${groupObj.id}-courses"></div>

          <div class="mb-2">
            <button type="button" class="btn btn-sm btn-outline-secondary" onclick="openSelectCourseModal(${groupObj.id})">+ Add Course</button>
            <button type="button" class="btn btn-sm btn-outline-warning" onclick="addNewPrerequisiteGroup(${groupObj.id})">+ Add Subgroup</button>
          </div>

          <div class="subgroups-container" id="subgroups-container-${groupObj.id}"></div>
        `;

        container.appendChild(groupBox);
        renderGroupCourses(groupObj.id);
        renderSubgroups(groupObj.id);

        // 🆕 Add AND/OR selection between top-level groups
        if (index < topLevelGroups.length - 1) {
            let connectorDiv = document.createElement("div");
            connectorDiv.classList.add("text-center", "my-2");
            connectorDiv.innerHTML = `
                <select class="form-select form-select-sm d-inline-block" style="width: 80px;"
                        onchange="updateOperatorToNext(${groupObj.id}, this.value)">
                    <option value="AND" ${groupObj.operatorToNext === "AND" ? "selected" : ""}>AND</option>
                    <option value="OR" ${groupObj.operatorToNext === "OR" ? "selected" : ""}>OR</option>
                </select>
                <span class="text-muted"> (connecting Group ${groupObj.id} to the next group)</span>
            `;
            container.appendChild(connectorDiv);
        }
    });
}


// Rerender just the subgroups for a parent group
function renderSubgroups(parentGroupId) {
    let parent = findGroupById(parentGroupId, topLevelGroups);
    if (!parent) return;

    let container = document.getElementById(`subgroups-container-${parentGroupId}`);
    container.innerHTML = "";

    parent.subGroups.forEach((sub, index) => {
        let subDiv = document.createElement("div");
        subDiv.classList.add("group-box", "ms-4", "mb-2");
        subDiv.dataset.groupId = sub.id;

        subDiv.innerHTML = `
          <div class="d-flex justify-content-between align-items-center mb-2">
            <span><strong>Group ${sub.id}</strong></span>
            <div>
                <select class="form-select form-select-sm d-inline-block" style="width:150px;"
                        onchange="updateGroupType(${sub.id}, this.value)">
                    <option value="AND" ${sub.type === "AND" ? "selected" : ""}>ALL (AND)</option>
                    <option value="OR" ${sub.type === "OR" ? "selected" : ""}>ANY (OR)</option>
                </select>
                <button type="button" class="btn btn-sm btn-outline-danger" onclick="removeGroup(${sub.id})">✖</button>
            </div>
          </div>

          <div class="mb-2" id="group-${sub.id}-courses"></div>

          <div class="mb-2">
            <button type="button" class="btn btn-sm btn-outline-secondary" onclick="openSelectCourseModal(${sub.id})">+ Add Course</button>
            <button type="button" class="btn btn-sm btn-outline-warning" onclick="addNewPrerequisiteGroup(${sub.id})">+ Add Subgroup</button>
          </div>

          <div class="subgroups-container" id="subgroups-container-${sub.id}"></div>
        `;

        container.appendChild(subDiv);
        renderGroupCourses(sub.id);
        renderSubgroups(sub.id);

        // 🆕 Add AND/OR selection between subgroups
        if (index < parent.subGroups.length - 1) {
            let subConnectorDiv = document.createElement("div");
            subConnectorDiv.classList.add("text-center", "my-2");
            subConnectorDiv.innerHTML = `
                <select class="form-select form-select-sm d-inline-block" style="width: 80px;"
                        onchange="updateSubGroupOperator(${sub.id}, this.value)">
                    <option value="AND" ${sub.operatorToNext === "AND" ? "selected" : ""}>AND</option>
                    <option value="OR" ${sub.operatorToNext === "OR" ? "selected" : ""}>OR</option>
                </select>
                <span class="text-muted"> (connecting Group ${sub.id} to the next subgroup)</span>
            `;
            container.appendChild(subConnectorDiv);
        }
    });
}



// ============================================================================
// Update the group's internal type ("AND" or "OR")
function updateGroupType(groupId, newType) {
    let grp = findGroupById(groupId, topLevelGroups);
    if (grp) {
        grp.type = newType;
        buildExpressionPreview();
    }
}

// Update how this group connects to the next top-level group
function updateOperatorToNext(groupId, newOp) {
    let grp = findGroupById(groupId, topLevelGroups);
    if (grp) {
        if (grp === topLevelGroups[topLevelGroups.length - 1]) {
            // If this is the last group, do NOT assign an operatorToNext
            delete grp.operatorToNext;
        } else {
            grp.operatorToNext = newOp;
        }
        buildExpressionPreview();
    }
}

// Update how this subgroup connects to the next subgroup
function updateSubGroupOperator(groupId, newOp) {
    let parent = findParentGroup(groupId, topLevelGroups);
    if (parent) {
        let subgroup = parent.subGroups.find(g => g.id === groupId);
        if (subgroup) {
            let isLastSubgroup = parent.subGroups[parent.subGroups.length - 1] === subgroup;

            if (isLastSubgroup) {
                delete subgroup.operatorToNext; // Remove operator if last subgroup
            } else {
                subgroup.operatorToNext = newOp;
            }

            buildExpressionPreview();
        }
    }
}




// ============================================================================
// Generate a new group ID
function generateGroupId() {
    return groupCounter++;
}

// ============================================================================
// Find group by ID recursively
function findGroupById(groupId, groupArr) {
    for (let g of groupArr) {
        if (g.id === groupId) return g;
        let subResult = findGroupById(groupId, g.subGroups);
        if (subResult) return subResult;
    }
    return null;
}

// ============================================================================
//  OPEN "Select Course" modal for the given group
// ============================================================================
function openSelectCourseModal(groupId) {
    currentGroupId = groupId;

    // Wait until courses are fully loaded
    if (allCourses.length === 0) {
        console.warn("Courses are not yet loaded. Please try again.");
        return;
    }

    populateSelectCourseList(); // Populate course list
    document.getElementById("selectCourseSearch").value = ""; // Reset search

    let modal = new bootstrap.Modal(document.getElementById("selectCourseModal"), {});
    modal.show();
}


// Populate the select course list in the modal
function populateSelectCourseList() {
    let listDiv = document.getElementById("selectCourseList");
    if (!listDiv) {
        console.error("Error: selectCourseList element not found.");
        return;
    }
    listDiv.innerHTML = ""; // Clear existing list

    // ✅ Exclude the course being edited + already selected courses
    let excludedIds = [Number(selectedCourseId)];
    let group = findGroupById(currentGroupId, topLevelGroups);
    if (group) {
        excludedIds.push(...group.courses);
    }

    // ✅ Ensure `course.title` is assigned properly
    let filteredCourses = allCourses
        .filter(course => !excludedIds.includes(course.id))
        .map(course => ({
            id: course.id,
            courseCode: course.courseCode,
            title: course.title || "(No Title)" // 🛠 Ensure title is not undefined
        }))
        .sort((a, b) => a.courseCode.localeCompare(b.courseCode));

    // ✅ Render the full list when modal opens
    filteredCourses.forEach(course => {
        let row = document.createElement("div");
        row.classList.add("d-flex", "justify-content-between", "align-items-center", "mb-2", "course-item");

        row.innerHTML = `
            <span class="course-code">${course.courseCode} - ${course.title}</span>
            <button type="button" class="btn btn-sm btn-primary">Add</button>
        `;

        row.querySelector("button").onclick = () => {
            selectCourse(course.id);
        };

        listDiv.appendChild(row);
    });
}


function filterSelectCourseList() {
    let searchInput = document.getElementById("selectCourseSearch");
    let searchTerm = searchInput.value.toLowerCase();
    let listDiv = document.getElementById("selectCourseList");
    if (!listDiv) {
        console.error("Error: selectCourseList element not found.");
        return;
    }

    // ✅ Exclude the currently selected course and already added courses
    let excludedIds = [Number(selectedCourseId)];
    let group = findGroupById(currentGroupId, topLevelGroups);
    if (group) {
        excludedIds.push(...group.courses);
    }

    // ✅ Filter courses dynamically (show exact matches first)
    let filteredCourses = allCourses
        .filter(course => !excludedIds.includes(course.id) &&
            course.courseCode.toLowerCase().includes(searchTerm))
        .sort((a, b) => {
            if (a.courseCode.toLowerCase().startsWith(searchTerm)) return -1;
            if (b.courseCode.toLowerCase().startsWith(searchTerm)) return 1;
            return a.courseCode.localeCompare(b.courseCode);
        });

    // ✅ Clear the existing list
    listDiv.innerHTML = "";

    // ✅ Re-populate with only the matching courses
    filteredCourses.forEach(course => {
        let row = document.createElement("div");
        row.classList.add("d-flex", "justify-content-between", "align-items-center", "mb-2", "course-item");

        row.innerHTML = `
            <span class="course-code">${course.courseCode} - ${course.title}</span>
            <button type="button" class="btn btn-sm btn-primary">Add</button>
        `;

        row.querySelector("button").onclick = () => {
            selectCourse(course.id);
        };

        listDiv.appendChild(row);
    });

}





// When user clicks "Add" in the course modal
function selectCourse(courseId) {
    let group = findGroupById(currentGroupId, topLevelGroups);
    if (!group) return;

    group.courses.push(courseId);
    renderGroupCourses(currentGroupId);

    // close modal
    let modalEl = document.getElementById("selectCourseModal");
    let modal = bootstrap.Modal.getInstance(modalEl);
    modal.hide();

    buildExpressionPreview();
}

// ============================================================================
// Render the "chips" for a group's courses
function renderGroupCourses(groupId) {
    let group = findGroupById(groupId, topLevelGroups);
    if (!group) return;

    let listDiv = document.getElementById(`group-${groupId}-courses`);
    if (!listDiv) return;

    listDiv.innerHTML = "";

    group.courses.forEach(cid => {
        let courseObj = allCourses.find(c => c.id === cid);
        if (!courseObj) return;

        let chip = document.createElement("div");
        chip.classList.add("chip");
        chip.dataset.courseId = cid;
        chip.innerHTML = `
            ${courseObj.courseCode}
            <span class="remove-chip" onclick="removeCourseFromGroup(${groupId}, ${cid})">
               &times;
            </span>
        `;
        listDiv.appendChild(chip);
    });
}

// Remove a course from group
function removeCourseFromGroup(groupId, courseId) {
    let group = findGroupById(groupId, topLevelGroups);
    if (!group) return;

    let idx = group.courses.indexOf(courseId);
    if (idx >= 0) group.courses.splice(idx, 1);

    renderGroupCourses(groupId);
    buildExpressionPreview();
}

// ============================================================================
// Build and display the entire "Expression Preview"
// ============================================================================
function buildExpressionPreview() {
    let previewDiv = document.getElementById("expressionPreview");
    if (!previewDiv) return;

    if (topLevelGroups.length === 0) {
        previewDiv.textContent = "(No prerequisites)";
        return;
    }

    let exprParts = [];
    for (let i = 0; i < topLevelGroups.length; i++) {
        let g = topLevelGroups[i];
        let groupStr = buildGroupExpression(g);
        if (i < topLevelGroups.length - 1) {
            groupStr += " " + g.operatorToNext + " ";
        }
        exprParts.push(groupStr);
    }

    previewDiv.textContent = exprParts.join("");
}

// Recursively build a string for a group
function buildGroupExpression(groupObj) {
    // 1) Build expression for courses
    let courseCodes = groupObj.courses.map(cid => {
        let cObj = allCourses.find(x => x.id === cid);
        return cObj ? cObj.courseCode : "???";
    });

    // 2) Build expressions for subgroups
    let subExprs = groupObj.subGroups.map(sub => buildGroupExpression(sub));

    // Combine them all with the group's "type" (AND/OR)
    // If group.type === "AND", we join with " AND ", else " OR "
    let joiner = (groupObj.type === "AND") ? " AND " : " OR ";

    // Combine courses + subExprs in a single array of parts
    let allParts = [...courseCodes, ...subExprs];
    if (allParts.length === 0) {
        // no courses, no subgroups
        return "( )";
    }
    if (allParts.length === 1) {
        // only one element, no need to join
        return allParts[0];
    }

    // multiple parts => wrap in parentheses
    return "(" + allParts.join(joiner) + ")";
}

function cleanEmptyGroups(groups) {
    return groups
        .filter(group => group.courses.length > 0 || group.subGroups.length > 0) // Keep only non-empty groups
        .map(group => ({
            ...group,
            subGroups: cleanEmptyGroups(group.subGroups) // Recursively clean subgroups
        }));
}




// ============================================================================
// Filter the main course list in your table (optional)
// ============================================================================
function filterCourses() {
    let searchTerm = this.value.toLowerCase();
    let tableRows = document.querySelectorAll("#courseTableBody tr");

    tableRows.forEach(row => {
        let courseCodeCell = row.querySelector(".courseCode");
        if (!courseCodeCell) return;
        let courseCode = courseCodeCell.textContent.toLowerCase();
        row.style.display = courseCode.includes(searchTerm) ? "" : "none";
    });
}

// ============================================================================
// Show success/error messages
// ============================================================================
function showMessage(divId, message, type) {
    let div = document.getElementById(divId);
    div.innerText = message;
    div.style.display = "block";
    div.classList.remove("alert-success", "alert-danger");

    if (type === "success") {
        div.classList.add("alert-success");
    } else {
        div.classList.add("alert-danger");
    }

    setTimeout(() => {
        div.style.display = "none";
        div.classList.remove("alert-success", "alert-danger");
    }, 2000);
}
function flattenGroups(courseId, groups, isChild = false, parentId = null) {
    let flatList = [];

    for (let i = 0; i < groups.length; i++) {
        let group = groups[i];
        const currentGroupId = group.id;
        const childGroupId = group.subGroups.length > 0 ? group.subGroups[0].id : 0;
        const isTopLevelParent = !isChild;
        const isFinalSubGroup = isChild && group.subGroups.length === 0;

        group.courses.forEach(prerequisiteId => {
            flatList.push({
                courseId: Number(courseId),
                prerequisiteId: prerequisiteId,
                groupId: currentGroupId,
                prerequisiteType: group.type,
                operatorToNext: isTopLevelParent ? group.operatorToNext : null,
                parent: !isChild || group.subGroups.length > 0,
                child: isChild,
                childId: childGroupId || 0
            });
        });

        // Handle subgroups recursively
        if (group.subGroups.length > 0) {
            flatList.push(...flattenGroups(courseId, group.subGroups, true, currentGroupId));
        }
    }

    return flatList;
}
function submitPrerequisiteForm(event) {
    event.preventDefault(); // Prevent default form submission

    let courseId = document.getElementById("selectedCourseId").value;

    if (!courseId || topLevelGroups.length === 0) {
        alert("Please add at least one prerequisite group.");
        return;
    }

    // Clean empty groups before sending
    let cleanedGroups = cleanEmptyGroups(topLevelGroups);

    let flattenedPrerequisites = flattenGroups(courseId, cleanedGroups);

    let requestData = {
        courseId: Number(courseId),
        prerequisites: flattenedPrerequisites
    };

    // ✅ First, send the request to API
    fetch("/api/admin/addPreReqs", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(requestData)
    })
        .then(response => response.json().then(data => ({ status: response.status, body: data })))
        .then(({ status, body }) => {
            document.getElementById("successStatus").value = (status === 200) ? "true" : "false";
            document.getElementById("responseMessage").value = body.message ||
                (status === 200 ? "Prerequisites added successfully" : "An unknown error occurred");

            // ✅ Now, submit the form normally
            document.getElementById("addPrerequisiteForm").submit();
        })
        .catch(err => {
            document.getElementById("successStatus").value = "false";
            document.getElementById("responseMessage").value = "Network error: " + err.message;

            // ✅ Submit form with error message
            document.getElementById("addPrerequisiteForm").submit();
        });
}


