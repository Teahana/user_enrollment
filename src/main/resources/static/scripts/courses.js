/* Updated prerequisites.js */

// ============================================================================
// Global variables
// ============================================================================
let allCourses = [];     // from API
let allProgrammes = [];  // from API
let specialTypes = []; // We'll store the enum names from the server
let hasFetchedSpecialTypes = false; // We only fetch once
let selectedCourseId;    // The main course for which we add/edit prereqs
let topLevelGroups = []; // Array of top-level groups
let groupCounter = 1;
let currentGroupId = null; // The group to which we are adding a new course
let isEditMode = false;  // false => Add mode, true => Edit mode

document.addEventListener("DOMContentLoaded", function () {
    // 1) Fetch courses
    fetch("/api/admin/getAllCourses")
        .then(res => res.json())
        .then(data => {
            allCourses = data;
        })
        .catch(err => console.error("Error fetching courses:", err));

    // 2) Fetch programmes
    fetch("/api/admin/getAllProgrammes")
        .then(res => res.json())
        .then(data => {
            allProgrammes = data;
            populateProgrammeDropdown();
            populateProgrammeCheckboxes();
        })
        .catch(err => console.error("Error fetching programmes:", err));

    fetch("/api/admin/getSpecialTypes")
        .then(res => res.json())
        .then(data => {
            // According to your endpoint, you may get something like:
            // { specialTypes: " [ADMISSION_PROGRAMME, COMPLETION_LEVEL_PERCENT]" }
            // So we need to parse that string carefully.
            if (data.specialTypes) {
                // e.g. " [ADMISSION_PROGRAMME, COMPLETION_LEVEL_PERCENT]"
                // remove brackets and spaces:
                let clean = data.specialTypes.replace(/[\[\]\s]/g, "");
                // => "ADMISSION_PROGRAMME,COMPLETION_LEVEL_PERCENT"
                specialTypes = clean.split(",");
                hasFetchedSpecialTypes = true;
            }
        })
        .catch(err => console.error("Error fetching special types:", err));
    // 3) Add/Edit button listeners
    document.querySelectorAll(".addPrereqBtn").forEach(btn => {
        btn.addEventListener("click", handleAddPrerequisites);
    });
    document.querySelectorAll(".editPrereqBtn").forEach(btn => {
        btn.addEventListener("click", handleEditPrerequisites);
    });

    // 4) Search filter
    const searchInput = document.getElementById("searchInput");
    if (searchInput) {
        searchInput.addEventListener("input", filterCourses);
    }

    // 5) Modal cleanup for ADD
    document.getElementById("addPrerequisiteModal").addEventListener("hidden.bs.modal", () => {
        document.getElementById("prerequisiteGroupsContainer").innerHTML = "";
        document.getElementById("expressionPreview").textContent = "";

        // BONUS TIP: Reset global state
        selectedCourseId = null;
        topLevelGroups = [];
        groupCounter = 1;
        currentGroupId = null;

        document.querySelectorAll(".modal-backdrop").forEach(el => el.remove());
        if (!document.querySelector(".modal.show")) {
            document.body.classList.remove("modal-open");
            document.body.style = "";
        }
    });

    // 6) Modal cleanup for EDIT
    document.getElementById("editPrerequisiteModal").addEventListener("hidden.bs.modal", () => {
        document.getElementById("editPrerequisiteGroupsContainer").innerHTML = "";
        document.getElementById("editExpressionPreview").textContent = "";

        // BONUS TIP: Reset global state
        selectedCourseId = null;
        topLevelGroups = [];
        groupCounter = 1;
        currentGroupId = null;

        document.querySelectorAll(".modal-backdrop").forEach(el => el.remove());
        if (!document.querySelector(".modal.show")) {
            document.body.classList.remove("modal-open");
            document.body.style = "";
        }
    });
    document.querySelectorAll(".editCourseBtn").forEach(btn => {
        btn.addEventListener("click", function () {
            const id = btn.getAttribute("data-course-id");
            const code = btn.getAttribute("data-course-code");
            const title = btn.getAttribute("data-title");
            const desc = btn.getAttribute("data-description");
            const credit = btn.getAttribute("data-credit");
            const level = btn.getAttribute("data-level");
            const sem1 = btn.getAttribute("data-sem1") === "true";
            const sem2 = btn.getAttribute("data-sem2") === "true";
            const programmes = btn.getAttribute("data-programmes").split(',').map(p => p.trim());

            document.getElementById("editCourseId").value = id;
            document.getElementById("editCourseCode").value = code;
            document.getElementById("editTitle").value = title;
            document.getElementById("editDescription").value = desc;
            document.getElementById("editCredit").value = credit;
            document.getElementById("editLevel").value = level;
            document.getElementById("editSem1").checked = sem1;
            document.getElementById("editSem2").checked = sem2;

            // Programmes
            const container = document.getElementById("editProgrammeCheckboxContainer");
            container.innerHTML = "";
            allProgrammes.forEach(prog => {
                const div = document.createElement("div");
                div.classList.add("form-check");

                const isChecked = programmes.includes(prog.programmeCode);
                div.innerHTML = `
                    <input class="form-check-input" type="checkbox"
                           name="programmeIds" value="${prog.id}"
                           id="editProgCheck${prog.id}"
                           ${isChecked ? "checked" : ""}>
                    <label class="form-check-label" for="editProgCheck${prog.id}">
                        ${prog.programmeCode} - ${prog.name}
                    </label>
                `;
                container.appendChild(div);
            });
        });
    });
});


/******************************************************************************
 *  handleAddPrerequisites(event)
 *    - user clicked "Add Prerequisites" for a course without prereqs
 ******************************************************************************/
function handleAddPrerequisites(evt) {
    let btn = evt.currentTarget;
    let courseId = btn.getAttribute("data-course-id");
    let courseCode = btn.getAttribute("data-course-code");

    selectedCourseId = courseId;  // store globally
    document.getElementById("selectedCourseId").value = courseId;
    document.getElementById("selectedCourseName").textContent = courseCode;

    isEditMode = false;

    // Reset topLevelGroups, etc.
    topLevelGroups = [];
    groupCounter = 1;
    currentGroupId = null;

    // Clear the container
    let container = document.getElementById("prerequisiteGroupsContainer");
    container.innerHTML = "";

    // Start with 1 empty group if you want:
    addNewPrerequisiteGroup();

    buildExpressionPreview(false);

    // show the "addPrerequisiteModal"
    let modal = new bootstrap.Modal(document.getElementById("addPrerequisiteModal"), {});
    modal.show();
}

/******************************************************************************
 *  handleEditPrerequisites(event)
 *    - user clicked "Edit Prerequisites" for a course with existing prereqs
 ******************************************************************************/
function handleEditPrerequisites(evt) {
    let btn = evt.currentTarget;
    let courseId = btn.getAttribute("data-course-id");
    let courseCode = btn.getAttribute("data-course-code");

    document.getElementById("editSelectedCourseId").value = courseId;
    document.getElementById("editSelectedCourseName").textContent = courseCode;

    selectedCourseId = courseId;
    isEditMode = true;

    // Reset
    document.getElementById("editPrerequisiteGroupsContainer").innerHTML = "";
    topLevelGroups = [];
    groupCounter = 1;
    currentGroupId = null;

    // fetch existing prereqs
    fetch("/api/admin/getPreReqs", {
        method: "POST",
        headers: {"Content-Type":"application/json"},
        body: JSON.stringify({ courseId: Number(courseId) })
    })
        .then(res=>res.json())
        .then(data => {
            if (data.prerequisites && data.prerequisites.prerequisites.length > 0) {
                let nested = convertFlatPrerequisitesToNested(data.prerequisites.prerequisites);
                topLevelGroups = convertToProgrammeStyle(nested);
                setGroupCounterToMaxId(topLevelGroups);
                renderTopLevelGroups(true);
                buildExpressionPreview(true);
            } else {
                // Reset if there are no prerequisites
                topLevelGroups = [];
                groupCounter = 1;
                currentGroupId = null;
                document.getElementById("editPrerequisiteGroupsContainer").innerHTML = "";
                buildExpressionPreview(true);
            }

            // Show modal regardless
            let modal = new bootstrap.Modal(document.getElementById("editPrerequisiteModal"), {});
            modal.show();
        })
        .catch(err => console.error("Error fetching prereqs:", err));
}

/******************************************************************************
 *  convertFlatPrerequisitesToNested(...)
 *    - your existing logic that returns {id, type, courses[], subGroups[], ...}
 ******************************************************************************/
function convertFlatPrerequisitesToNested(flat) {
    let groupMap = {};
    let parentMap = {};
    let topLevel = [];

    flat.forEach(pr => {
        if (!groupMap[pr.groupId]) {
            groupMap[pr.groupId] = {
                id: pr.groupId,
                type: pr.prerequisiteType || "AND",
                prereqItems: [],
                subGroups: [],
                operatorToNext: pr.operatorToNext || null
            };
        }

        // Detect if this is a special item
        if (pr.special && specialTypes.includes(pr.specialType)) {
            if (pr.specialType === "ADMISSION_PROGRAMME") {
                // Try to group same ADMISSION_PROGRAMME items with same groupId and targetLevel
                let existingSpecial = groupMap[pr.groupId].prereqItems.find(it =>
                    it.special &&
                    it.specialType === "ADMISSION_PROGRAMME"
                );

                if (existingSpecial) {
                    // Append to existing programmeIds array
                    if (!existingSpecial.programmeIds.includes(pr.programmeId)) {
                        existingSpecial.programmeIds.push(pr.programmeId);
                    }
                } else {
                    groupMap[pr.groupId].prereqItems.push({
                        special: true,
                        specialType: pr.specialType,
                        programmeIds: pr.programmeId ? [pr.programmeId] : []
                    });
                }
            }
            else if (pr.specialType === "COMPLETION_LEVEL_PERCENT") {
                groupMap[pr.groupId].prereqItems.push({
                    special: true,
                    specialType: pr.specialType,
                    targetLevel: pr.targetLevel,
                    percentageValue: pr.percentageValue
                });
            }
        } else {
            // Normal item
            groupMap[pr.groupId].prereqItems.push({
                courseId: pr.prerequisiteId,
                programmeId: pr.programmeId || null
            });
        }

        if (pr.child && pr.parentId && pr.parentId !== 0) {
            parentMap[pr.groupId] = pr.parentId;
        }
    });

    Object.keys(parentMap).forEach(childId => {
        let pId = parentMap[childId];
        if (groupMap[pId] && groupMap[childId]) {
            groupMap[pId].subGroups.push(groupMap[childId]);
        }
    });

    Object.values(groupMap).forEach(g => {
        if (!parentMap[g.id]) {
            topLevel.push(g);
        }
    });

    return topLevel;
}


/******************************************************************************
 *  convertToProgrammeStyle(nestedGroups)
 *    We want each group to have "prereqItems" = [{courseId, programmeId}, ...]
 *    For now, we set programmeId=null for all existing items (since old records
 *    didn't store programme).
 ******************************************************************************/
function convertToProgrammeStyle(groups) {
    return groups.map(g => {
        // Convert subGroups recursively
        let subG = convertToProgrammeStyle(g.subGroups || []);

        // Just pass through prereqItems as-is
        let items = g.prereqItems || [];

        return {
            id: g.id,
            type: g.type,
            prereqItems: items,
            subGroups: subG,
            operatorToNext: g.operatorToNext || null
        };
    });
}


/******************************************************************************
 *  setGroupCounterToMaxId
 ******************************************************************************/
function setGroupCounterToMaxId(groups) {
    let maxId = 0;
    function traverse(arr) {
        arr.forEach(g => {
            if (g.id>maxId) maxId=g.id;
            traverse(g.subGroups);
        });
    }
    traverse(groups);
    groupCounter = maxId+1;
}

/******************************************************************************
 *  addNewPrerequisiteGroup(parentId)
 ******************************************************************************/
function addNewPrerequisiteGroup(parentId=null) {
    let gid = groupCounter++;
    let newGroup = {
        id: gid,
        type:"AND",
        prereqItems: [],
        subGroups: [],
        operatorToNext:"AND"
    };

    if (!parentId) {
        topLevelGroups.push(newGroup);
        renderTopLevelGroups(isEditMode);
    } else {
        let p = findGroupById(parentId, topLevelGroups);
        if (p) {
            p.subGroups.push(newGroup);
            renderSubgroups(parentId);
        }
    }
    buildExpressionPreview(isEditMode);
}

/******************************************************************************
 *  renderTopLevelGroups(editMode)
 ******************************************************************************/
function renderTopLevelGroups(editMode=false) {
    let container = editMode
        ? document.getElementById("editPrerequisiteGroupsContainer")
        : document.getElementById("prerequisiteGroupsContainer");

    container.innerHTML = "";

    topLevelGroups.forEach((g, idx) => {
        let box = document.createElement("div");
        box.classList.add("group-box","mb-3");
        box.dataset.groupId = g.id;

        box.innerHTML = `
          <div class="d-flex justify-content-between align-items-center mb-2">
            <span><strong>Group ${g.id}</strong></span>
            <div>
                <select class="form-select form-select-sm d-inline-block"
                        style="width:150px;"
                        onchange="updateGroupType(${g.id}, this.value)">
                    <option value="AND" ${g.type==="AND"?"selected":""}>ALL (AND)</option>
                    <option value="OR"  ${g.type==="OR" ?"selected":""}>ANY (OR)</option>
                </select>
                <button type="button" class="btn btn-sm btn-outline-danger"
                        onclick="removeGroup(${g.id})">&times;</button>
            </div>
          </div>

          <div class="mb-2" id="group-${g.id}-courses"></div>
          <div class="mb-2">
            <button type="button" class="btn btn-sm btn-outline-secondary"
                    onclick="openSelectCourseModal(${g.id})">
              + Add Course
            </button>
            <button type="button" class="btn btn-sm btn-outline-warning"
                    onclick="addNewPrerequisiteGroup(${g.id})">
              + Add Subgroup
            </button>
          </div>
          <div class="subgroups-container" id="subgroups-container-${g.id}"></div>
        `;
        container.appendChild(box);

        renderGroupCourses(g.id);
        renderSubgroups(g.id);

        // If there's a next group, show AND/OR connector
        if (idx<topLevelGroups.length-1) {
            let connector = document.createElement("div");
            connector.classList.add("text-center","my-2");
            connector.innerHTML = `
                <select class="form-select form-select-sm d-inline-block"
                        style="width:80px;"
                        onchange="updateOperatorToNext(${g.id}, this.value)">
                    <option value="AND" ${g.operatorToNext==="AND"?"selected":""}>AND</option>
                    <option value="OR"  ${g.operatorToNext==="OR" ?"selected":""}>OR</option>
                </select>
                <span class="text-muted"> (between Group ${g.id} and next)</span>
            `;
            container.appendChild(connector);
        }
    });
    buildExpressionPreview(editMode);
}

/******************************************************************************
 *  renderSubgroups(parentId)
 ******************************************************************************/
function renderSubgroups(parentId, visited=new Set()) {
    if (visited.has(parentId)) return;
    visited.add(parentId);

    let parent = findGroupById(parentId, topLevelGroups);
    if (!parent) return;

    let container = document.getElementById(`subgroups-container-${parentId}`);
    container.innerHTML = "";

    parent.subGroups.forEach((sub, idx) => {
        let box = document.createElement("div");
        box.classList.add("group-box","ms-4","mb-2");
        box.dataset.groupId = sub.id;

        box.innerHTML = `
          <div class="d-flex justify-content-between align-items-center mb-2">
            <span><strong>Group ${sub.id}</strong></span>
            <div>
                <select class="form-select form-select-sm d-inline-block"
                        style="width:150px;"
                        onchange="updateGroupType(${sub.id}, this.value)">
                    <option value="AND" ${sub.type==="AND"?"selected":""}>ALL (AND)</option>
                    <option value="OR"  ${sub.type==="OR" ?"selected":""}>ANY (OR)</option>
                </select>
                <button type="button" class="btn btn-sm btn-outline-danger"
                        onclick="removeGroup(${sub.id})">✖</button>
            </div>
          </div>
          <div class="mb-2" id="group-${sub.id}-courses"></div>
          <div class="mb-2">
            <button type="button" class="btn btn-sm btn-outline-secondary"
                    onclick="openSelectCourseModal(${sub.id})">
              + Add Course
            </button>
            <button type="button" class="btn btn-sm btn-outline-warning"
                    onclick="addNewPrerequisiteGroup(${sub.id})">
              + Add Subgroup
            </button>
          </div>
          <div class="subgroups-container" id="subgroups-container-${sub.id}"></div>
        `;
        container.appendChild(box);

        // render items
        renderGroupCourses(sub.id);
        renderSubgroups(sub.id, visited);

        // if there's a next sub, show connector
        if (idx<parent.subGroups.length-1) {
            let conn = document.createElement("div");
            conn.classList.add("text-center","my-2");
            conn.innerHTML=`
              <select class="form-select form-select-sm d-inline-block"
                      style="width:80px;"
                      onchange="updateSubGroupOperator(${sub.id}, this.value)">
                <option value="AND" ${sub.operatorToNext==="AND"?"selected":""}>AND</option>
                <option value="OR"  ${sub.operatorToNext==="OR" ?"selected":""}>OR</option>
              </select>
              <span class="text-muted"> (connecting Group ${sub.id} to the next)</span>
            `;
            container.appendChild(conn);
        }
    });
}

/******************************************************************************
 *  data structures
 *    groupObj => { id, type, prereqItems: [], subGroups:[], operatorToNext?... }
 *    each item => { courseId, programmeId }
 ******************************************************************************/

/******************************************************************************
 *  openSelectCourseModal(groupId)
 *    - we do not hide the parent modal, just open child on top
 *    - after closing the child, we forcibly remove extra backdrops if any
 ******************************************************************************/
function openSelectCourseModal(groupId) {
    currentGroupId = groupId;
    // reset search
    document.getElementById("selectCourseSearch").value="";
    populateSelectCourseList();
    // show the child modal
    let childModal = new bootstrap.Modal(
        document.getElementById("selectCourseModal"), {}
    );
    childModal.show();
}

/******************************************************************************
 *  populateSelectCourseList
 ******************************************************************************/
function populateSelectCourseList() {
    let listDiv = document.getElementById("selectCourseList");
    if (!listDiv) return;
    listDiv.innerHTML="";

    // find the group
    let group = findGroupById(currentGroupId, topLevelGroups);
    if (!group) return;

    // we store items in group.prereqItems => but we haven't replaced old code yet
    // So let's do it properly:
    let excluded = new Set(
        group.prereqItems ? group.prereqItems.map(it=>it.courseId) : group.courses
    );
    // also exclude the main course ID
    if(selectedCourseId) excluded.add(Number(selectedCourseId));

    // build a sorted list
    let filtered = allCourses
        .filter(c => !excluded.has(c.id))
        .sort((a,b)=>a.courseCode.localeCompare(b.courseCode));

    filtered.forEach(c => {
        let row = document.createElement("div");
        row.classList.add("d-flex","justify-content-between","align-items-center","mb-2");

        row.innerHTML=`
          <span>${c.courseCode} - ${c.title||"(No Title)"}</span>
          <button type="button" class="btn btn-sm btn-primary">Add</button>
        `;

        row.querySelector("button").onclick = () => {
            selectCourse(c.id);
        };

        listDiv.appendChild(row);
    });
}

/******************************************************************************
 *  filterSelectCourseList
 ******************************************************************************/
function filterSelectCourseList(inputEl) {
    let searchTerm = inputEl.value.toLowerCase();
    let listDiv = document.getElementById("selectCourseList");
    if (!listDiv) return;

    let group = findGroupById(currentGroupId, topLevelGroups);
    if (!group) return;

    let excluded = new Set(
        group.prereqItems ? group.prereqItems.map(it=>it.courseId) : group.courses
    );
    if(selectedCourseId) excluded.add(Number(selectedCourseId));

    let filtered = allCourses
        .filter(c=>!excluded.has(c.id) &&
            c.courseCode.toLowerCase().includes(searchTerm)
        )
        .sort((a,b)=>a.courseCode.localeCompare(b.courseCode));

    listDiv.innerHTML="";
    filtered.forEach(c => {
        let row = document.createElement("div");
        row.classList.add("d-flex","justify-content-between","align-items-center","mb-2");
        row.innerHTML=`
          <span>${c.courseCode} - ${c.title||"(No Title)"}</span>
          <button class="btn btn-sm btn-primary">Add</button>
        `;
        row.querySelector("button").onclick=()=>selectCourse(c.id);
        listDiv.appendChild(row);
    });
}

/******************************************************************************
 *  populateProgrammeDropdown
 *    - fill the #selectProgrammeDropdown in the child modal
 ******************************************************************************/
function populateProgrammeDropdown() {
    let progSelect = document.getElementById("selectProgrammeDropdown");
    if(!progSelect) return;
    progSelect.innerHTML = `<option value="">(Any programmes)</option>`;
    allProgrammes.forEach(p => {
        let opt = document.createElement("option");
        opt.value = p.id;
        opt.textContent = `${p.programmeCode} - ${p.name}`;
        progSelect.appendChild(opt);
    });
}

/******************************************************************************
 *  selectCourse(courseId)
 *    - user clicked "Add" in the child modal
 *    - we store {courseId, programmeId} in group.prereqItems
 ******************************************************************************/
function selectCourse(courseId) {
    let group = findGroupById(currentGroupId, topLevelGroups);
    if (!group) return;

    // check if we have group.prereqItems or group.courses
    if(!group.prereqItems) {
        // convert the existing group.courses => prereqItems
        group.prereqItems = (group.courses||[]).map(cid=>({ courseId: cid, programmeId:null }));
        delete group.courses;
    }

    // read the chosen programme
    let progSelect = document.getElementById("selectProgrammeDropdown");
    let selectedProgId = progSelect && progSelect.value ? Number(progSelect.value) : null;

    // push
    group.prereqItems.push({
        courseId:courseId,
        programmeId:selectedProgId
    });

    // re-render
    renderGroupCourses(currentGroupId);

    // close the child modal
    let modalEl = document.getElementById("selectCourseModal");
    let childModal = bootstrap.Modal.getInstance(modalEl);
    if (childModal) {
        childModal.hide();
        // forcibly remove leftover backdrop if more than 1
        removeExtraBackdrops();
    }

    buildExpressionPreview(isEditMode);
}

/******************************************************************************
 *  removeExtraBackdrops()
 *    - if there's more than 1 .modal-backdrop, remove the last
 ******************************************************************************/
function removeExtraBackdrops() {
    let backdrops = document.querySelectorAll(".modal-backdrop");
    if (backdrops.length>1) {
        // remove the last one
        backdrops[ backdrops.length -1 ].remove();
    }
}

/******************************************************************************
 *  renderGroupCourses(groupId) => "chips"
 *    - if group has prereqItems => use that
 *    - else fallback to group.courses
 ******************************************************************************/
function renderGroupCourses(groupId) {
    let group = findGroupById(groupId, topLevelGroups);
    if (!group) {
        console.warn("No group found for id", groupId);
        return;
    }

    let container = document.getElementById(`group-${groupId}-courses`);
    if (!container) {
        console.warn("No container found for group", groupId);
        return;
    }

    container.innerHTML = "";

    if (group.prereqItems) {
        group.prereqItems.forEach(item => {
            if (item.special) {
                // === Special item ===
                let chipLabel = "";
                if (item.specialType === "COMPLETION_LEVEL_PERCENT") {
                    let lvl = item.targetLevel || 0;
                    let pct = (item.percentageValue || 0) * 100; // convert back to 75, etc.
                    chipLabel = `${pct}% of ${lvl}-level courses`;
                }
                else if (item.specialType === "ADMISSION_PROGRAMME") {
                    chipLabel = "Admission into: ";
                    let progNames = [];
                    if (item.programmeIds && item.programmeIds.length > 0) {
                        for (let pid of item.programmeIds) {
                            let pObj = allProgrammes.find(x => x.id === pid);
                            if (pObj) progNames.push(pObj.programmeCode);
                        }
                        chipLabel += progNames.join(", ");
                    } else {
                        // If no programmes selected, fallback text
                        chipLabel += "(no programmes?)";
                    }
                }
                else {
                    chipLabel = item.specialType; // fallback
                }

                let chip = document.createElement("div");
                chip.classList.add("chip");
                chip.innerHTML = `
                    ${chipLabel}
                    <span class="remove-chip"
                        onclick="removePrereqItem(${groupId}, null, null, '${item.specialType}', ${item.targetLevel || 0}, ${item.percentageValue || 0}, '${(item.programmeIds||[]).join(",")}')">
                        &times;
                    </span>
                `;
                container.appendChild(chip);
            }
            else {
                // === Normal item (course) ===
                let cObj = allCourses.find(c => c.id === item.courseId);
                if (!cObj) {
                    console.warn("No course found for ID", item.courseId);
                    return;
                }
                let label = cObj.courseCode;
                if (item.programmeId) {
                    let pObj = allProgrammes.find(p => p.id === item.programmeId);
                    label += pObj ? ` (${pObj.programmeCode})` : " (??)";
                } else {
                    label += " (Any)";
                }

                let chip = document.createElement("div");
                chip.classList.add("chip");
                chip.innerHTML = `
                  ${label}
                  <span class="remove-chip"
                        onclick="removePrereqItem(${groupId}, ${item.courseId}, ${item.programmeId || 0})">
                    &times;
                  </span>
                `;
                container.appendChild(chip);
            }
        });
    }
}



/******************************************************************************
 *  removePrereqItem
 ******************************************************************************/
function removePrereqItem(groupId, courseId, programmeId,
                          specialType=null, targetLevel=0, percentageValue=0, programmeIdsCsv="") {
    let group = findGroupById(groupId, topLevelGroups);
    if (!group || !group.prereqItems) return;

    if (!specialType) {
        // Normal course-based removal
        group.prereqItems = group.prereqItems.filter(
            it => !(it.courseId === courseId && (it.programmeId || 0) === (programmeId || 0))
        );
    } else {
        // Special item removal
        let progIds = programmeIdsCsv
            ? programmeIdsCsv.split(",").map(x => parseInt(x.trim())).filter(Boolean)
            : [];
        group.prereqItems = group.prereqItems.filter(it => {
            if (!it.special) return true; // keep normal items

            let sameProgrammes = JSON.stringify((it.programmeIds || []).sort()) ===
                JSON.stringify(progIds.sort());
            return !(
                it.specialType === specialType &&
                (it.targetLevel || 0) === parseInt(targetLevel) &&
                (it.percentageValue || 0) === parseFloat(percentageValue) &&
                sameProgrammes
            );
        });
    }

    renderGroupCourses(groupId);
    buildExpressionPreview(isEditMode);
}


/******************************************************************************
 *  buildGroupExpression(...) => includes programme info
 *    - if group.prereqItems => use them, else fallback to group.courses
 ******************************************************************************/
function buildGroupExpression(g) {
    // 1) Build an array of item labels
    let items = [];
    if (g.prereqItems) {
        items = g.prereqItems.map(it => {
            if (it.special) {
                // ---------- SPECIAL ITEM ----------
                if (it.specialType === "COMPLETION_LEVEL_PERCENT") {
                    const lvl = it.targetLevel || 0;
                    const pct = (it.percentageValue || 0) * 100; // e.g. 0.75 -> 75
                    return `{${pct}% of ${lvl}-level courses}`;
                }
                else if (it.specialType === "ADMISSION_PROGRAMME") {
                    // build a list of programme codes, e.g. ["BSE", "BNS"]
                    const pCodes = (it.programmeIds || []).map(pid => {
                        let pObj = allProgrammes.find(px => px.id === pid);
                        return pObj ? pObj.programmeCode : "??";
                    });

                    if (pCodes.length > 1) {
                        // e.g. "{Admission into (BSE OR BNS)}"
                        return `{Admission into (${pCodes.join(" OR ")})}`;
                    } else if (pCodes.length === 1) {
                        // e.g. "{Admission into BSE}"
                        return `{Admission into ${pCodes[0]}}`;
                    } else {
                        // e.g. "{Admission into (none)}"
                        return `{Admission into (none)}`;
                    }
                }
                else {
                    // fallback if new specialType is added
                    return `{${it.specialType}}`;
                }
            } else {
                // ---------- NORMAL ITEM (course) ----------
                let cObj = allCourses.find(cx => cx.id === it.courseId);
                let label = cObj ? cObj.courseCode : "???";
                // If there's a programmeId, e.g. for major-specific prereq
                if (it.programmeId) {
                    let p = allProgrammes.find(px => px.id === it.programmeId);
                    label += p ? `(${p.programmeCode})` : "(??)";
                } else {
                    label += "(Any)";
                }
                return label;
            }
        });
    } else {
        // fallback if using older group.courses
        items = (g.courses || []).map(cid => {
            let c = allCourses.find(cx => cx.id === cid);
            return c ? c.courseCode + "(Any)" : "???(Any)";
        });
    }

    // 2) Recursively build sub-group expressions
    let subExprs = g.subGroups.map(s => buildGroupExpression(s));

    // 3) Combine all items and sub-groups with AND/OR
    let joiner = (g.type === "AND") ? " AND " : " OR ";
    let allParts = [...items, ...subExprs];

    // If the group only has 0 or 1 items, don't add parentheses
    if (allParts.length <= 1) {
        return allParts.join("");
    } else {
        // If multiple items, wrap them in parentheses
        return `(${allParts.join(joiner)})`;
    }
}




/******************************************************************************
 *  buildExpressionPreview(editMode)
 ******************************************************************************/
function buildExpressionPreview(editMode=false) {
    let el = editMode
        ? document.getElementById("editExpressionPreview")
        : document.getElementById("expressionPreview");
    if(!el) return;

    if(topLevelGroups.length===0) {
        el.textContent="(No prerequisites)";
        return;
    }

    let parts=[];
    for (let i=0;i<topLevelGroups.length;i++){
        let e = buildGroupExpression(topLevelGroups[i]);
        if(i<topLevelGroups.length-1 && topLevelGroups[i].operatorToNext){
            e+=" "+topLevelGroups[i].operatorToNext+" ";
        }
        parts.push(e);
    }
    el.textContent=parts.join("");
}

/******************************************************************************
 *  updateGroupType, updateOperatorToNext, updateSubGroupOperator
 *    - same as your old code
 ******************************************************************************/
function updateGroupType(groupId, newType){
    let g=findGroupById(groupId, topLevelGroups);
    if(g) {
        g.type=newType;
        buildExpressionPreview(isEditMode);
    }
}
function updateOperatorToNext(groupId, newOp){
    let idx=topLevelGroups.findIndex(x=>x.id===groupId);
    if(idx>=0 && idx<topLevelGroups.length-1){
        topLevelGroups[idx].operatorToNext=newOp;
        buildExpressionPreview(isEditMode);
    }
}
function updateSubGroupOperator(groupId, newOp){
    let parent=findParentGroup(groupId, topLevelGroups);
    if(!parent) return;
    let sub=parent.subGroups.find(s=>s.id===groupId);
    if(sub){
        let isLast= parent.subGroups[parent.subGroups.length-1]===sub;
        if(isLast){
            delete sub.operatorToNext;
        } else {
            sub.operatorToNext=newOp;
        }
        buildExpressionPreview(isEditMode);
    }
}

/******************************************************************************
 *  findGroupById  => same as old
 ******************************************************************************/
function findGroupById(groupId,arr){
    for(let g of arr){
        if(g.id===groupId)return g;
        let s=findGroupById(groupId,g.subGroups);
        if(s)return s;
    }
    return null;
}
function findParentGroup(childId,arr){
    for(let g of arr){
        if(g.subGroups.some(s=>s.id===childId)){
            return g;
        }
        let s=findParentGroup(childId, g.subGroups);
        if(s)return s;
    }
    return null;
}

/******************************************************************************
 * removeGroup => same as old, but we re-render
 ******************************************************************************/
function removeGroup(id){
    // check top-level
    let isTop=topLevelGroups.some(g=>g.id===id);
    if(isTop){
        if(topLevelGroups.length===1){
            alert("You must keep at least 1 group");
            return;
        }
        topLevelGroups=topLevelGroups.filter(g=>g.id!==id);
    } else {
        removeGroupRecursive(id,topLevelGroups);
    }
    renumberGroups();
    renderTopLevelGroups(isEditMode);
    buildExpressionPreview(isEditMode);
}
function removeGroupRecursive(id, arr){
    for(let i=0;i<arr.length;i++){
        let idx=arr[i].subGroups.findIndex(sg=>sg.id===id);
        if(idx>=0){
            arr[i].subGroups.splice(idx,1);
            return;
        }
        removeGroupRecursive(id,arr[i].subGroups);
    }
}

/******************************************************************************
 * flattenGroups => we store {courseId, programmeId} => see flattenPrereqItems
 ******************************************************************************/
function flattenGroups(courseId, groups, parentId = 0) {
    let list = [];
    for (let g of groups) {
        let currentId = g.id;
        let isChild = parentId !== 0;

        // If g.prereqItems => flatten them
        let items = g.prereqItems
            ? g.prereqItems
            : (g.courses || []).map(cid => ({ courseId: cid, programmeId: null }));

        for (let it of items) {

            // Common fields for all items
            const baseFields = {
                courseId: Number(courseId),
                groupId: currentId,
                prerequisiteType: g.type,                // AND/OR
                operatorToNext: isChild ? null : g.operatorToNext,
                parent: !isChild,
                child: isChild,
                parentId: parentId,
                childId: (g.subGroups.length > 0) ? g.subGroups[0].id : 0,

                // "special" might be true or false
                special: !!it.special,
                specialType: it.specialType || null,
                targetLevel: it.targetLevel || null,
                percentageValue: it.percentageValue || null
            };

            if (it.special) {
                // ===== SPECIAL ITEM =====
                if (it.specialType === "ADMISSION_PROGRAMME") {
                    // If user selected multiple programmes in one special item,
                    // we split it into multiple DTOs (one per programme).
                    if (Array.isArray(it.programmeIds) && it.programmeIds.length > 0) {
                        it.programmeIds.forEach(pid => {
                            list.push({
                                ...baseFields,
                                // "programmeId" becomes the single ID for this DTO
                                programmeId: pid,
                            });
                        });
                    } else {
                        // if no programmeIds, we still push one item with programmeId=null
                        list.push({
                            ...baseFields,
                            programmeId: null
                        });
                    }
                }
                else if (it.specialType === "COMPLETION_LEVEL_PERCENT") {
                    // Only one item needed
                    list.push({
                        ...baseFields,
                        programmeId: null  // not applicable
                    });
                }
                else {
                    // Fallback for other special types
                    list.push({
                        ...baseFields,
                        programmeId: null
                    });
                }
            }
            else {
                // ===== NORMAL ITEM =====
                list.push({
                    ...baseFields,
                    prerequisiteId: it.courseId,
                    programmeId: it.programmeId || null
                });
            }
        }

        // Recurse down subGroups
        if (g.subGroups.length > 0) {
            list.push(...flattenGroups(courseId, g.subGroups, currentId));
        }
    }
    return list;
}



/******************************************************************************
 * cleanEmptyGroups, validateGroups => same as old
 ******************************************************************************/
function cleanEmptyGroups(groups){
    return groups
        .filter(g=>
            (g.prereqItems && g.prereqItems.length>0) ||
            (g.courses && g.courses.length>0) ||
            g.subGroups.length>0
        )
        .map(g=>({
            ...g,
            subGroups: cleanEmptyGroups(g.subGroups)
        }));
}
function validateGroups(groups){
    for(let g of groups){
        let hasItems= (g.prereqItems && g.prereqItems.length>0) ||
            (g.courses && g.courses.length>0);
        if(!hasItems && g.subGroups.length===0)return false;
        if(!validateGroups(g.subGroups))return false;
    }
    return true;
}

/******************************************************************************
 * renumberGroups => from old
 ******************************************************************************/
function renumberGroups(){
    let counter=1;
    function traverse(arr){
        arr.forEach(g=>{
            g.id=counter++;
            traverse(g.subGroups);
        });
    }
    traverse(topLevelGroups);
    groupCounter=counter;
}

/******************************************************************************
 * submitPrerequisiteForm(event)
 ******************************************************************************/
function submitPrerequisiteForm(evt){
    evt.preventDefault();

    let courseId=document.getElementById("selectedCourseId").value;
    if(!courseId||topLevelGroups.length===0){
        alert("Please add at least one group.");
        return;
    }
    if(!validateGroups(topLevelGroups)){
        alert("Some groups empty. Please fix!");
        return;
    }
    let cleaned=cleanEmptyGroups(topLevelGroups);
    let flattened=flattenGroups(courseId, cleaned);

    let requestData={
        courseId:Number(courseId),
        prerequisites:flattened
    };
    console.log(JSON.stringify(requestData,null,2))
    fetch("/api/admin/addPreReqs", {
        method:"POST",
        headers:{"Content-Type":"application/json"},
        body:JSON.stringify(requestData)
    })
        .then(async(resp)=>{
            let data;
            try{ data=await resp.json();} catch(e){ data={message:"Unexpected"}; }
            if(!resp.ok){
                throw new Error(data.message||"Error adding prereqs");
            }
            // success => fill hidden fields => submit form
            document.getElementById("successStatus").value="true";
            document.getElementById("responseMessage").value=data.message||"Success";
            document.getElementById("addPrerequisiteForm").submit();
        })
        .catch(err=>{
            document.getElementById("successStatus").value="false";
            document.getElementById("responseMessage").value="Error: "+err.message;
            document.getElementById("addPrerequisiteForm").submit();
        });
}

/******************************************************************************
 * submitEditPrerequisiteForm(event)
 ******************************************************************************/
function submitEditPrerequisiteForm(evt){
    evt.preventDefault();
    let courseId=document.getElementById("editSelectedCourseId").value;
    if(!courseId||topLevelGroups.length===0){
        alert("Please add at least one group.");
        return;
    }
    if(!validateGroups(topLevelGroups)){
        alert("Some groups empty. Please fix!");
        return;
    }
    let cleaned=cleanEmptyGroups(topLevelGroups);
    let flattened=flattenGroups(courseId, cleaned);

    let requestData={
        courseId:Number(courseId),
        prerequisites:flattened
    };

    fetch("/api/admin/updatePreReqs", {
        method:"POST",
        headers:{"Content-Type":"application/json"},
        body:JSON.stringify(requestData)
    })
        .then(async(resp)=>{
            let data;
            try{ data=await resp.json(); } catch(e){ data={message:"Unexpected"}; }
            if(!resp.ok){
                throw new Error(data.message||"Error updating prereqs");
            }
            document.getElementById("editSuccessStatus").value="true";
            document.getElementById("editResponseMessage").value=data.message||"Updated";
            document.getElementById("editPrerequisiteForm").submit();
        })
        .catch(err=>{
            document.getElementById("editSuccessStatus").value="false";
            document.getElementById("editResponseMessage").value="Error: "+err.message;
            document.getElementById("editPrerequisiteForm").submit();
        });
}

/******************************************************************************
 *  filterCourses => optional table filter
 ******************************************************************************/
function filterCourses(){
    let term=this.value.toLowerCase();
    let rows=document.querySelectorAll("#courseTableBody tr");
    rows.forEach(r=>{
        let codeCell=r.querySelector(".courseCode");
        if(!codeCell)return;
        let code=codeCell.textContent.toLowerCase();
        r.style.display=(code.includes(term))?"":"none";
    });
}
function populateProgrammeCheckboxes() {
    const container = document.getElementById("programmeCheckboxContainer");
    if (!container) return;

    container.innerHTML = ""; // Clear any old checkboxes

    allProgrammes.forEach(prog => {
        const div = document.createElement("div");
        div.classList.add("form-check");

        div.innerHTML = `
            <input class="form-check-input" type="checkbox" 
                   name="programmeIds" value="${prog.id}" 
                   id="progCheck${prog.id}">
            <label class="form-check-label" for="progCheck${prog.id}">
                ${prog.programmeCode} - ${prog.name}
            </label>
        `;

        container.appendChild(div);
    });
}
