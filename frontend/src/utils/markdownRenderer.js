function escapeHtml(value) {
    return value
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

function renderInlineMarkdown(value) {
    return value
        .replace(/`([^`]+)`/g, '<code>$1</code>')
        .replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>');
}

function closeList(output, listType) {
    if (listType) {
        output.push(`</${listType}>`);
    } else {
        output.push('');
    }
}

function openList(output, currentType, nextType) {
    if (currentType === nextType) {
        return currentType;
    } else {
        closeList(output, currentType);
        output.push(`<${nextType}>`);
        return nextType;
    }
}

function renderMarkdownLine(line, output, listType) {
    const trimmed = line.trim();
    const unorderedMatch = trimmed.match(/^[-*]\s+(.+)$/);
    const orderedMatch = trimmed.match(/^\d+\.\s+(.+)$/);
    const headingMatch = trimmed.match(/^(#{1,4})\s+(.+)$/);
    let nextListType = listType;
    if (!trimmed) {
        closeList(output, nextListType);
        nextListType = '';
    } else if (unorderedMatch) {
        nextListType = openList(output, nextListType, 'ul');
        output.push(`<li>${renderInlineMarkdown(unorderedMatch[1])}</li>`);
    } else if (orderedMatch) {
        nextListType = openList(output, nextListType, 'ol');
        output.push(`<li>${renderInlineMarkdown(orderedMatch[1])}</li>`);
    } else if (headingMatch) {
        closeList(output, nextListType);
        nextListType = '';
        const level = Math.min(headingMatch[1].length + 2, 5);
        output.push(`<h${level}>${renderInlineMarkdown(headingMatch[2])}</h${level}>`);
    } else {
        closeList(output, nextListType);
        nextListType = '';
        output.push(`<p>${renderInlineMarkdown(trimmed)}</p>`);
    }
    return nextListType;
}

export function renderSafeMarkdown(value) {
    const rawText = value === undefined || value === null ? '' : String(value);
    const escapedText = escapeHtml(rawText);
    const output = [];
    let listType = '';
    escapedText.replace(/\r\n/g, '\n').split('\n').forEach((line) => {
        listType = renderMarkdownLine(line, output, listType);
    });
    closeList(output, listType);
    return output.filter(Boolean).join('');
}
