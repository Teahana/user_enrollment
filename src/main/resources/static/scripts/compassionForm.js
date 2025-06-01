    function addExam()
    {
        const container = document.getElementById('examEntries');
        const newExam = container.firstElementChild.cloneNode(true);
        newExam.querySelectorAll("input").forEach(input => input.value = "");
        container.appendChild(newExam);
    }

    document.querySelector("form").addEventListener("submit", function (e)
    {
        let valid = true;
        const form = this;

        const dob = document.getElementById("dob");
        const dobError = document.getElementById("dobError");
        dobError.textContent = "";
        dob.classList.remove("scroll-highlight");

        const submissionDate = document.getElementById("submissionDate");
        const submissionDateError = document.getElementById("submissionDateError");
        submissionDateError.textContent = "";
        submissionDate.classList.remove("scroll-highlight");

        const today = new Date().toISOString().split("T")[0];

        if (dob.value && dob.value > today)
        {
            dobError.textContent = "Date of Birth cannot be in the future.";
            dob.classList.add("scroll-highlight");
            dob.scrollIntoView({ behavior: "smooth", block: "center" });
            valid = false;
        }

        if (submissionDate.value && submissionDate.value < today)
        {
            submissionDateError.textContent = "Signing date cannot be in the past.";
            submissionDate.classList.add("scroll-highlight");
            submissionDate.scrollIntoView({ behavior: "smooth", block: "center" });
            valid = false;
        }

        const firstInvalid = form.querySelector(":invalid");
        if (firstInvalid && valid === false)
        {
            e.preventDefault();
            return;
        }

        if (!valid) e.preventDefault();

        // mark all required labels
        document.querySelectorAll("label").forEach(label => {
        const fieldId = label.getAttribute("for");
        const input = document.getElementById(fieldId);
        if (input && input.required && !label.innerHTML.includes("*"))
        {
            label.innerHTML += ' <span class="required">*</span>';
        }
    });
});
