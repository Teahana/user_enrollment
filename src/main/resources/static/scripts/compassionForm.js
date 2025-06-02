let disableFrontendValidation = false; // starts with validation ON

function applyFrontendValidationState() {
    const inputs = document.querySelectorAll("input, select, textarea");
    inputs.forEach(input => {
        if (disableFrontendValidation) {
            input.removeAttribute("required");
            input.classList.remove("was-validated");
        } else {
            if (input.dataset.originalRequired === "true") {
                input.setAttribute("required", "required");
            }
        }
    });
}

document.addEventListener("DOMContentLoaded", () => {
    // Backup original required attributes
    document.querySelectorAll("input[required], select[required], textarea[required]").forEach(el => {
        el.dataset.originalRequired = "true";
    });

    const toggleBtn = document.getElementById("toggleValidationBtn");
    if (toggleBtn) {
        toggleBtn.addEventListener("click", () => {
            disableFrontendValidation = !disableFrontendValidation;
            applyFrontendValidationState();
            toggleBtn.textContent = disableFrontendValidation
                ? "Enable Frontend Validation"
                : "Disable Frontend Validation";
            alert("Frontend validation is now " + (disableFrontendValidation ? "OFF" : "ON"));
        });

        // Set initial button label on load
        toggleBtn.textContent = disableFrontendValidation
            ? "Enable Frontend Validation"
            : "Disable Frontend Validation";
    }
});


const canvas = document.getElementById("signatureCanvas");
    const ctx = canvas.getContext("2d");
    let drawing = false;

    // Fill background white (needed for accurate comparison)
    ctx.fillStyle = "#fff";
    ctx.fillRect(0, 0, canvas.width, canvas.height);

    canvas.addEventListener("mousedown", () => drawing = true);
    canvas.addEventListener("mouseup", () => drawing = false);
    canvas.addEventListener("mouseleave", () => drawing = false);

    canvas.addEventListener("mousemove", e => {
    if (!drawing) return;
    const rect = canvas.getBoundingClientRect();
    ctx.lineWidth = 2;
    ctx.lineCap = "round";
    ctx.strokeStyle = "#000";
    ctx.lineTo(e.clientX - rect.left, e.clientY - rect.top);
    ctx.stroke();
    ctx.beginPath();
    ctx.moveTo(e.clientX - rect.left, e.clientY - rect.top);
});

    function clearSignature() {
    ctx.fillStyle = "#fff";
    ctx.fillRect(0, 0, canvas.width, canvas.height);
    ctx.beginPath();
}

    // Helper: Check if canvas is blank by comparing pixel data
    function isCanvasBlank(canvas) {
    const blank = document.createElement('canvas');
    blank.width = canvas.width;
    blank.height = canvas.height;
    const blankCtx = blank.getContext("2d");
    blankCtx.fillStyle = "#fff";
    blankCtx.fillRect(0, 0, canvas.width, canvas.height);
    return canvas.toDataURL() === blank.toDataURL();
}

function beforeSubmit() {
    document.getElementById("signatureImage").value = canvas.toDataURL();

    if (disableFrontendValidation) return true;

    const dob = document.getElementById("dob").value;
    const submissionDate = document.getElementById("submissionDate").value;
    const today = new Date().toISOString().split("T")[0];

    if (dob > today) {
        alert("Date of Birth cannot be in the future.");
        return false;
    }

    if (submissionDate < today) {
        alert("Submission date cannot be in the past.");
        return false;
    }

    if (isCanvasBlank(canvas)) {
        alert("Please sign the application before submitting.");
        return false;
    }

    return true;
}


function addExam() {
        const container = document.getElementById("examEntries");
        const original = container.querySelector(".exam-group");
        if (!original) return;

        const clone = original.cloneNode(true);

        // Clear values in the cloned inputs
        clone.querySelectorAll("input").forEach(input => {
            input.value = "";
        });

        container.appendChild(clone);
    }