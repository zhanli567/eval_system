const DESCRIPTION_FIELD = 'description';
const SUPPORTED_FIELDS = new Set([DESCRIPTION_FIELD]);

export function buildResourceFieldPatch(fields = {}) {
    const rawFields = fields || {};
    const normalized = {};
    for (const [field, value] of Object.entries(rawFields)) {
        if (!SUPPORTED_FIELDS.has(field)) {
            throw new Error(`暂不支持修改字段：${field}`);
        }
        normalized[field] = normalizeDescription(value);
    }
    if (!Object.keys(normalized).length) {
        throw new Error('请提供需要修改的字段');
    }
    return { fields: normalized };
}

function normalizeDescription(value) {
    if (value === null || value === undefined) {
        return '';
    }
    if (typeof value !== 'string') {
        throw new Error('描述必须是字符串');
    }
    const normalized = value.trim();
    if (normalized.length > 200) {
        throw new Error('描述不能超过200个字符');
    }
    return normalized;
}
