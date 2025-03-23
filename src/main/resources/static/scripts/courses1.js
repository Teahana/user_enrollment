document.addEventListener("DOMContentLoaded", function () {
    if (typeof mermaid === "undefined") {
        console.error("Mermaid is not loaded.");
        return;
    } else {
        console.log("Mermaid is loaded.");
    }

    document.querySelectorAll(".visualizeBtn").forEach(button => {
        button.addEventListener("click", () => {
            const courseCode = button.getAttribute("data-course-code");
            const expression = button.getAttribute("data-expression");

            const mermaidStr = convertToMermaidTree(courseCode, expression);
            console.log("Generated Mermaid diagram:");
            console.log(mermaidStr);

            // Render the Mermaid diagram into SVG
            mermaid.render("generatedDiagram", mermaidStr).then(({ svg }) => {
                const container = document.getElementById("mermaidContainer");
                container.innerHTML = svg;

                const modal = new bootstrap.Modal(document.getElementById("visualizeModal"));
                modal.show();
            }).catch(err => {
                console.error("Mermaid render error:", err);
            });
        });
    });
    let selectedCourseId = null;

    document.querySelectorAll(".removePrereqBtn").forEach(button => {
        button.addEventListener("click", () => {
            selectedCourseId = button.getAttribute("data-course-id");
            const courseCode = button.getAttribute("data-course-code");
            document.getElementById("deleteCourseName").textContent = courseCode;

            const modal = new bootstrap.Modal(document.getElementById("confirmDeleteModal"));
            modal.show();
        });
    });

    document.getElementById("confirmDeleteBtn").addEventListener("click", () => {
        if (!selectedCourseId) return;
        window.location.href = `/admin/deletePreReqs/${selectedCourseId}`;
    });
});

/**
 * Converts logical expression string into a Mermaid diagram
 */
function convertToMermaidTree(courseCode, expression) {
    let nodeId = 0;
    const nodes = [];
    const edges = [];

    function getNode(label) {
        const id = "N" + (nodeId++);
        const safeLabel = label.replace(/"/g, '\\"');
        nodes.push(`${id}["${safeLabel}"]`);
        return id;
    }

    function isFullyWrapped(str) {
        str = str.trim();
        if (!str.startsWith("(") || !str.endsWith(")")) return false;
        let depth = 0;
        for (let i = 0; i < str.length; i++) {
            if (str[i] === "(") depth++;
            else if (str[i] === ")") depth--;
            if (depth === 0 && i < str.length - 1) return false;
        }
        return depth === 0;
    }

    function splitByTopLevelOperator(expr, operator) {
        let parts = [];
        let current = "";
        let depth = 0;
        let braceDepth = 0;

        for (let i = 0; i < expr.length; i++) {
            const c = expr[i];
            if (c === "(") depth++;
            if (c === ")") depth--;
            if (c === "{") braceDepth++;
            if (c === "}") braceDepth--;

            if (
                depth === 0 &&
                braceDepth === 0 &&
                expr.slice(i, i + operator.length + 2) === ` ${operator} `
            ) {
                parts.push(current.trim());
                current = "";
                i += operator.length + 1;
            } else {
                current += c;
            }
        }

        if (current.trim()) parts.push(current.trim());
        return parts;
    }

    function parse(expr) {
        expr = expr.trim();
        if (isFullyWrapped(expr)) {
            expr = expr.slice(1, -1).trim();
        }

        // Try OR first
        let parts = splitByTopLevelOperator(expr, "OR");
        let operator = "OR";

        if (parts.length === 1) {
            parts = splitByTopLevelOperator(expr, "AND");
            operator = "AND";
        }

        if (parts.length === 1) {
            return getNode(parts[0]);
        }

        const opNode = getNode(operator);
        for (const part of parts) {
            const childId = parse(part);
            edges.push(`${opNode} --> ${childId}`);
        }

        return opNode;
    }

    const root = getNode(`${courseCode} (Main Course)`);
    const body = parse(expression);
    edges.push(`${root} --> ${body}`);

    return `graph TD\n${[...nodes, ...edges].join("\n")}`;
}

