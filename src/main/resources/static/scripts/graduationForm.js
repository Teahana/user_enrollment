const canvas = document.getElementById("signature-pad");
const signatureInput = document.getElementById("signatureImage");
const ctx = canvas.getContext("2d");
let drawing = false;

function startDraw(e) {
    drawing = true;
    ctx.beginPath();
    ctx.moveTo(e.offsetX, e.offsetY);
}

function draw(e) {
    if (!drawing) return;
    ctx.lineTo(e.offsetX, e.offsetY);
    ctx.stroke();
}

function stopDraw() {
    drawing = false;
}

function clearSignature() {
    ctx.clearRect(0, 0, canvas.width, canvas.height);
    signatureInput.value = ""; // Clear hidden input too
}

function isCanvasEmpty(canvas) {
    const blank = document.createElement('canvas');
    blank.width = canvas.width;
    blank.height = canvas.height;
    return canvas.toDataURL() === blank.toDataURL();
}

function convertToBase64() {
    if (isCanvasEmpty(canvas)) {
        alert("Please provide your signature before submitting.");
        return false;
    }
    const dataURL = canvas.toDataURL();
    signatureInput.value = dataURL;
    return true;
}

// Drawing events
canvas.addEventListener("mousedown", startDraw);
canvas.addEventListener("mousemove", draw);
canvas.addEventListener("mouseup", stopDraw);
canvas.addEventListener("mouseleave", stopDraw);

// Prevent form submission if no signature
document.getElementById("graduationForm").addEventListener("submit", function(e) {
    if (!convertToBase64()) {
        e.preventDefault();
    }
});