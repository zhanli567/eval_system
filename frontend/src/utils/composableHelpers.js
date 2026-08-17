export function getErrorMessage(error, fallback) {
    return error?.response?.data?.msg
        || error?.response?.data?.message
        || (error instanceof Error && error.message)
        || fallback;
}

export function toggleDescSort(sortBy, sortOrder, field) {
    sortOrder.value = sortBy.value === field && sortOrder.value === 'asc' ? 'desc' : 'asc';
    sortBy.value = field;
}

export function sortParams(sortBy, sortOrder) {
    if (sortBy.value) {
        return { sortBy: sortBy.value, sortOrder: sortOrder.value || 'asc' };
    } else {
        return {};
    }
}

export function movePreviousPageIfLastRow(records, page) {
    if (records.value.length === 1 && page.value > 1) {
        page.value -= 1;
    }
}

export async function runExclusive(busy, action) {
    if (busy.value) {
        return undefined;
    }
    busy.value = true;
    try {
        return await action();
    }
    finally {
        busy.value = false;
    }
}

export async function runExclusiveById(busyIds, id, action) {
    if (!id || busyIds.value.includes(id)) {
        return undefined;
    }
    busyIds.value = [...busyIds.value, id];
    try {
        return await action();
    }
    finally {
        busyIds.value = busyIds.value.filter((item) => item !== id);
    }
}

export function labelFromMap(map, value) {
    return value ? map[value] || value : '-';
}
