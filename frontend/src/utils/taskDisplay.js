const APP_OUTPUT_KEYS = ['text', 'content', 'answer', 'error', 'rawText', 'reasoning', 'debug'];
export function formatAppOutput(value) {
    const text = pickAppOutputText(value || '');
    return cleanupDisplayText(text);
}
export function formatEvaluatorReason(value) {
    return cleanupDisplayText(value || '');
}
export function compactText(value, maxLength = 140) {
    const text = cleanupDisplayText(value || '').replace(/\s+/g, ' ');
    return text.length > maxLength ? `${text.slice(0, maxLength)}...` : text;
}
function pickAppOutputText(value) {
    const trimmed = value.trim();
    if (!trimmed)
        return '';
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
    if (!value.includes('\n'))
        return value;
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
    if (lines.length < 5)
        return false;
    const averageLength = lines.reduce((sum, line) => sum + line.length, 0) / lines.length;
    const shortLineRatio = lines.filter((line) => line.length <= 8).length / lines.length;
    return averageLength <= 10 || shortLineRatio >= 0.75;
}
