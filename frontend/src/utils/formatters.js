export function formatDateTime(value) {
    if (!value) {
        return '-';
    }
    const date = toDate(value);
    if (!date) {
        return String(value);
    }
    const pad = (item) => String(item).padStart(2, '0');
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}-${pad(date.getMinutes())}-${pad(date.getSeconds())}`;
}

function toDate(value) {
    if (Array.isArray(value)) {
        const [year, month, day, hour = 0, minute = 0, second = 0] = value;
        return new Date(year, month - 1, day, hour, minute, second);
    }
    if (typeof value === 'number' || /^\d+$/.test(String(value))) {
        return new Date(Number(value));
    }
    const parsed = new Date(value);
    return Number.isNaN(parsed.getTime()) ? null : parsed;
}
