let inputJson = null;

const fileInput = document.getElementById("fileInput");
const inputArea = document.getElementById("input");
const outputArea = document.getElementById("output");
const mappingSelect = document.getElementById("mapping");

document
    .getElementById("transformBtn")
    .addEventListener("click", transform);

document
    .getElementById("downloadBtn")
    .addEventListener("click", downloadOutput);

fileInput.addEventListener("change", loadFile);

/**
 * Reads the selected JSON file.
 */
function loadFile(event) {
    const file = event.target.files[0];
    if (!file) return;

    const reader = new FileReader();

    reader.onload = e => {
        try {
            inputJson = JSON.parse(e.target.result);
            inputArea.value = JSON.stringify(inputJson, null, 2);
        } catch (err) {
            alert("Invalid JSON file.");
        }
    };

    reader.readAsText(file);
}

/**
 * Loads the selected mapping file.
 */
async function loadMapping() {
    const response = await fetch(mappingSelect.value);

    if (!response.ok) {
        throw new Error("Could not load mapping.");
    }

    return response.json();
}

/**
 * Parses a path string into segments.
 * e.g. "[].seasons[].number" → ["[]", "seasons", "[]", "number"]
 */
function parsePath(path) {
    return path.split(".").flatMap(part => {
        if (part === "[]") return ["[]"];
        if (part.endsWith("[]")) return [part.slice(0, -2), "[]"];
        return [part];
    });
}

/**
 * Walks the data following the given path, collecting
 * { value, indices } for each reached leaf.
 * indices tracks which array index was chosen at each [] level.
 */
function getValues(data, fromPath) {
    const segments = parsePath(fromPath);
    const results = [];

    function walk(node, segIdx, indices) {
        if (segIdx === segments.length) {
            if (node !== null && node !== undefined) {
                results.push({value: node, indices: [...indices]});
            }
            return;
        }
        const seg = segments[segIdx];
        if (seg === "[]") {
            if (!Array.isArray(node)) return;
            node.forEach((item, i) => walk(item, segIdx + 1, [...indices, i]));
        } else {
            if (node == null || typeof node !== "object") return;
            walk(node[seg], segIdx + 1, indices);
        }
    }

    walk(data, 0, []);
    return results;
}

/**
 * Walks/creates the output structure following the given path,
 * using indices to choose array positions, and assigns value at the leaf.
 */
function setValue(output, toPath, value, indices) {
    const segments = parsePath(toPath);
    let arrIdx = 0;
    let node = output;

    for (let i = 0; i < segments.length - 1; i++) {
        const seg = segments[i];
        const nextSeg = segments[i + 1];
        const nextIsArray = nextSeg === "[]";
        if (seg === "[]") {
            const idx = indices[arrIdx++];
            if (node[idx] === undefined) node[idx] = nextIsArray ? [] : {};
            node = node[idx];
        } else {
            if (node[seg] === undefined) node[seg] = nextIsArray ? [] : {};
            node = node[seg];
        }
    }

    const lastSeg = segments[segments.length - 1];
    if (lastSeg === "[]") {
        node[indices[arrIdx]] = value;
    } else {
        node[lastSeg] = value;
    }
}

/**
 * Sets a constant value at all leaf positions matching the toPath
 * in the output structure, creating nested structures as needed.
 */
function setConstantValueAtPath(output, toPath, value) {
    const segments = parsePath(toPath);

    function walk(node, segIdx) {
        if (segIdx >= segments.length) return;

        const seg = segments[segIdx];
        const isLastSegment = segIdx === segments.length - 1;

        if (seg === "[]") {
            if (!Array.isArray(node)) return;
            if (isLastSegment) return; // [] can't be last segment
            node.forEach(item => walk(item, segIdx + 1));
        } else {
            if (node == null || typeof node !== "object") return;

            if (isLastSegment) {
                node[seg] = value;
            } else {
                const nextSeg = segments[segIdx + 1];
                const shouldBeArray = nextSeg === "[]";
                if (node[seg] === undefined) {
                    node[seg] = shouldBeArray ? [] : {};
                }
                walk(node[seg], segIdx + 1);
            }
        }
    }

    walk(output, 0);
}

/**
 * Transform the JSON using the mapping.
 */
async function transform() {

    if (!inputJson) {
        alert("Please load a JSON file first.");
        return;
    }

    try {

        const mapping = await loadMapping();

        const output = [];

        for (const field of mapping.fields) {

            if (field.value !== undefined) {
                // Constant value — apply to all matching paths in output
                setConstantValueAtPath(output, field.to, field.value);
            } else {
                // Mapped value — copy from input using index trail
                const results = getValues(inputJson, field.from);
                for (const {value, indices} of results) {
                    setValue(output, field.to, value, indices);
                }
            }

        }

        outputArea.value =
            JSON.stringify(output, null, 2);

    } catch (err) {

        console.error(err);
        alert(err.message);

    }

}

/**
 * Downloads the transformed JSON.
 */
function downloadOutput() {

    if (!outputArea.value) {
        alert("Nothing to download.");
        return;
    }

    const blob = new Blob(
        [outputArea.value],
        {type: "application/json"}
    );

    const url = URL.createObjectURL(blob);

    const a = document.createElement("a");

    a.href = url;
    a.download = "output.json";

    a.click();

    URL.revokeObjectURL(url);
}
