let inputJson = null;
let outputJson = null;

const fileInput = document.getElementById("fileInput");
const inputArea = document.getElementById("input");
const outputArea = document.getElementById("output");
const mappingSelect = document.getElementById("mapping");
const languageSelect = document.getElementById("language");

document
    .getElementById("transformBtn")
    .addEventListener("click", transform);

document
    .getElementById("downloadBtn")
    .addEventListener("click", downloadOutput);

fileInput.addEventListener("change", loadFile);

const MAPPING_PATHS = {
    "shows": "mappings/tv-time-out-shows.json",
    "lists": "mappings/tv-time-out-lists.json",
};

/**
 * Returns a truncated preview of a JSON string for display purposes.
 */
const JSON_PREVIEW_LIMIT = 20_000;

function jsonPreview(json) {
    const full = JSON.stringify(json, null, 2);
    if (full.length <= JSON_PREVIEW_LIMIT) return full;
    return full.slice(0, JSON_PREVIEW_LIMIT) + `\n\n... preview truncated at ${JSON_PREVIEW_LIMIT} characters (${full.length} total)`;
}

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
            // Only display a preview to keep the page performant
            inputArea.value = jsonPreview(inputJson);
        } catch (err) {
            alert("Invalid JSON file.");
        }
    };

    reader.readAsText(file);
}

/**
 * Loads the selected mapping file and returns it as JSON.
 *
 * Replaces language code placeholder value with selected language code.
 */
async function loadMapping() {
    const mappingPath = MAPPING_PATHS[mappingSelect.value];
    if (!mappingPath) {
        throw new Error("Unknown mapping selected.");
    }
    const response = await fetch(mappingPath);

    if (!response.ok) {
        throw new Error("Could not load mapping.");
    }

    const mappingText = await response.text();

    // Replace placeholder with selected language code
    const placeholder = "PLACEHOLDER_LANGUAGE";
    const languageCode = languageSelect.value;
    const replacedText = mappingText.replace(placeholder, languageCode);

    return JSON.parse(replacedText);
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
 * Recursively removes null and undefined elements from arrays.
 * Object properties are left untouched.
 */
function removeNullElements(value) {
    if (Array.isArray(value)) {
        return value
            .filter(item => item !== null && item !== undefined)
            .map(removeNullElements);
    }
    if (value !== null && typeof value === "object") {
        return Object.fromEntries(
            Object.entries(value).map(([k, v]) => [k, removeNullElements(v)])
        );
    }
    return value;
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

        // As the mapping is based on indexes, if an array element has no mapped
        // value (for example, for list items that are movies) a null element
        // would be part of the output JSON. Remove those null/undefined items.
        const sanitizedOutput = removeNullElements(output);

        outputJson = sanitizedOutput;
        // Only display a preview to keep the page performant
        outputArea.value = jsonPreview(sanitizedOutput);

    } catch (err) {

        console.error(err);
        alert(err.message);

    }

}

/**
 * Returns a filename like "shows-2026-07-06-14-30.json"
 * based on the selected mapping ID and the current date/time.
 */
function buildOutputFilename() {
    const mappingId = mappingSelect.value;
    const now = new Date();
    const pad = n => String(n).padStart(2, "0");
    const year = now.getFullYear();
    const month = pad(now.getMonth() + 1);
    const day = pad(now.getDate());
    const hours = pad(now.getHours());
    const minutes = pad(now.getMinutes());
    const seconds = pad(now.getSeconds());
    return `${mappingId}-${year}-${month}-${day}-${hours}${minutes}${seconds}.json`;
}

/**
 * Downloads the transformed JSON.
 */
function downloadOutput() {

    if (!outputJson) {
        alert("Nothing to download.");
        return;
    }

    const blob = new Blob(
        [JSON.stringify(outputJson, null, 2)],
        {type: "application/json"}
    );

    const url = URL.createObjectURL(blob);

    const a = document.createElement("a");

    a.href = url;
    a.download = buildOutputFilename();

    a.click();

    URL.revokeObjectURL(url);
}
