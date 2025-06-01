const canvas = document.getElementById('signature-pad');
const ctx = canvas.getContext('2d');
let drawing = false;

canvas.addEventListener('mousedown', () => drawing = true);
canvas.addEventListener('mouseup', () => {
    drawing = false;
    saveSignature();
});
canvas.addEventListener('mouseleave', () => drawing = false);
canvas.addEventListener('mousemove', draw);

function draw(e) {
    if (!drawing) return;
    const rect = canvas.getBoundingClientRect();
    ctx.lineWidth = 2;
    ctx.lineCap = 'round';
    ctx.strokeStyle = '#000';
    ctx.lineTo(e.clientX - rect.left, e.clientY - rect.top);
    ctx.stroke();
    ctx.beginPath();
    ctx.moveTo(e.clientX - rect.left, e.clientY - rect.top);
}

function saveSignature() {
    const dataURL = canvas.toDataURL();
    document.getElementById('signatureImage').value = dataURL;
}

function clearSignature() {
    ctx.clearRect(0, 0, canvas.width, canvas.height);
    ctx.beginPath();
    document.getElementById('signatureImage').value = "";
}

document.getElementById('graduationForm').addEventListener('submit', function(e) {
    const dob = document.getElementById('dob');
    const dobError = document.getElementById('dobError');
    dobError.textContent = "";
    dob.classList.remove("scroll-highlight");

    const signatureDate = document.getElementById('signatureDate');
    const signatureDateError = document.getElementById('signatureDateError');
    signatureDateError.textContent = "";
    signatureDate.classList.remove("scroll-highlight");

    const today = new Date().toISOString().split("T")[0];
    let valid = true;

    if (dob.value && dob.value > today) {
        dobError.textContent = "Date of Birth cannot be in the future.";
        dob.classList.add("scroll-highlight");
        valid = false;
    }

    if (signatureDate.value && signatureDate.value < today) {
        signatureDateError.textContent = "Signing date cannot be before today.";
        signatureDate.classList.add("scroll-highlight");
        valid = false;
    }

    if (!valid) e.preventDefault();
});
