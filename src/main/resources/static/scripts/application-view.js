document.addEventListener("DOMContentLoaded", () => {
    const filePaths = window.applicationFilePaths || [];

    const list = document.getElementById("documentList");
    if (list && filePaths.length > 0) {
        fetch("/api/admin/fileMeta", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(filePaths)
        })
            .then(response => response.json())
            .then(files => {
                files.forEach(file => {
                    const li = document.createElement("li");
                    li.innerHTML = `
            <strong>${file.name}</strong> (${file.mimeType})`;
                    list.appendChild(li);
                });
            })
            .catch(err => {
                console.error("Failed to fetch file metadata", err);
            });
    }

    const downloadBtn = document.getElementById("downloadAllBtn");
    if (downloadBtn) {
        downloadBtn.addEventListener("click", () => {
            fetch("/api/admin/getFiles", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ filePaths: filePaths })
            })
                .then(res => res.blob())
                .then(blob => {
                    const zipUrl = URL.createObjectURL(blob);
                    const a = document.createElement("a");
                    a.href = zipUrl;
                    a.download = "supporting_documents.zip";
                    document.body.appendChild(a);
                    a.click();
                    document.body.removeChild(a);
                    URL.revokeObjectURL(zipUrl);
                })
                .catch(err => {
                    console.error("Download failed", err);
                });
        });
    }

    // Signature Rendering
    const signaturePath = window.signaturePath || "";
    if (signaturePath) {
        fetch("/api/admin/getFile", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ fileName: signaturePath })
        })
            .then(res => res.blob())
            .then(blob => {
                const img = new Image();
                img.onload = function () {
                    const canvas = document.getElementById("signatureCanvas");
                    if (canvas) {
                        canvas.width = img.width;
                        canvas.height = img.height;
                        const ctx = canvas.getContext("2d");
                        ctx.drawImage(img, 0, 0);
                    }
                };
                img.src = URL.createObjectURL(blob);
            })
            .catch(err => {
                console.error("Failed to load signature image", err);
            });
    }
});
