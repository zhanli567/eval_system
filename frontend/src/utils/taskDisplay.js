const APP_OUTPUT_KEYS = [
    'text',
    'content',
    'answer',
    'error',
    'rawText',
    'reasoning',
    'debug',
    'toolCall',
    'toolResponse',
    'skillTrigger',
    'references',
    'genUi'
];
export function formatAppOutput(value) {
    const text = pickAppOutputText(value || '');
    return cleanupDisplayText(text);
}
export function formatEvaluatorReason(value) {
    return cleanupDisplayText(value || '');
}
export function formatAgentOutputValue(value) {
    const text = String(value ?? '').trim();
    if (!text) {
        return '';
    }
    return formatJsonText(text) || formatJsonLines(cleanupDisplayText(text)) || cleanupDisplayText(text);
}
export function compactText(value, maxLength = 140) {
    const text = cleanupDisplayText(value || '').replace(/\s+/g, ' ');
    return text.length > maxLength ? `${text.slice(0, maxLength)}...` : text;
}
function pickAppOutputText(value) {
    const trimmed = value.trim();
    if (!trimmed) {
        return '';
    }
    return looksLikeJsonObject(trimmed) ? pickJsonOutputText(trimmed) || trimmed : trimmed;
}
function looksLikeJsonObject(value) {
    return value.startsWith('{') && value.endsWith('}');
}
function pickJsonOutputText(value) {
    try {
        const parsed = JSON.parse(value);
        return APP_OUTPUT_KEYS.map((key) => parsed[key]).find((item) => typeof item === 'string' && item.trim());
    }
    catch {
        return '';
    }
}
function formatJsonText(value) {
    const trimmed = value.trim();
    if (!looksLikeJson(trimmed)) {
        return '';
    }
    try {
        return JSON.stringify(normalizeJsonValue(JSON.parse(trimmed)), null, 2);
    }
    catch {
        return '';
    }
}
function formatJsonLines(value) {
    const lines = value
        .split('\n')
        .map((line) => line.trim())
        .filter(Boolean);
    if (lines.length < 2) {
        return '';
    }
    const formatted = lines.map((line) => formatJsonText(line));
    return formatted.every(Boolean) ? formatted.join('\n\n') : '';
}
function looksLikeJson(value) {
    return (value.startsWith('{') && value.endsWith('}')) || (value.startsWith('[') && value.endsWith(']'));
}
function normalizeJsonValue(value) {
    if (Array.isArray(value)) {
        return value.map((item) => normalizeJsonValue(item));
    }
    if (value && typeof value === 'object') {
        return Object.fromEntries(
            Object.entries(value).map(([key, item]) => [key, normalizeJsonValue(item)])
        );
    }
    if (typeof value === 'string') {
        return formatNestedJsonString(value);
    }
    return value;
}
function formatNestedJsonString(value) {
    const trimmed = value.trim();
    if (!looksLikeJson(trimmed)) {
        return value;
    }
    try {
        return normalizeJsonValue(JSON.parse(trimmed));
    }
    catch {
        return value;
    }
}
function cleanupDisplayText(value) {
    const normalized = value
        .replace(/\\r\\n/g, '\n')
        .replace(/\\n/g, '\n')
        .replace(/\\r/g, '\n')
        .replace(/\*\*([^*]+)\*\*/g, '$1')
        .replace(/__([^_]+)__/g, '$1')
        .replace(/[ \t]+\n/g, '\n')
        .replace(/\n{3,}/g, '\n\n')
        .trim();
    return mergeFragmentedLines(normalized);
}
function mergeFragmentedLines(value) {
    if (!value.includes('\n')) {
        return value;
    }
    return value
        .split(/\n{2,}/)
        .map((block) => {
        const rawLines = block
            .split('\n')
            .filter((line) => line.trim());
        const lines = rawLines.map((line) => line.trim());
        if (shouldMergeLines(lines)) {
            return rawLines.join('');
        }
        return block;
    })
        .join('\n\n');
}
function shouldMergeLines(lines) {
    if (lines.length < 5) {
        return false;
    }
    const averageLength = lines.reduce((sum, line) => sum + line.length, 0) / lines.length;
    const shortLineRatio = lines.filter((line) => line.length <= 8).length / lines.length;
    return averageLength <= 10 || shortLineRatio >= 0.75;
}
