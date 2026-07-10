export function getErrorMessage(error, fallback) {
    if (error instanceof Error && error.message) {
        return error.message;
    }
    return error?.response?.data?.msg || error?.response?.data?.message || fallback;
}

export function toggleDescSort(sortBy, sortOrder, field) {
    sortOrder.value = sortBy.value === field && sortOrder.value === 'desc' ? 'asc' : 'desc';
    sortBy.value = field;
}

export function movePreviousPageIfLastRow(records, page) {
    if (records.value.length === 1 && page.value > 1) {
        page.value -= 1;
    }
}

export function labelFromMap(map, value) {
    return value ? map[value] || value : '-';
}
