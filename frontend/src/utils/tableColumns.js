import { reactive } from 'vue';

export function useColumnWidths(limits) {
    const columnWidths = reactive(Object.fromEntries(
        Object.entries(limits).map(([key, value]) => [key, value.width])
    ));

    function handleColumnResize(newWidth, _oldWidth, column) {
        const key = column.property || column.columnKey || column.rawColumnKey || column.label;
        const limit = limits[key];
        if (!limit) {
            return;
        }
        const width = Math.min(limit.max, Math.max(limit.min, Number(newWidth) || limit.width));
        columnWidths[key] = width;
        column.width = width;
        column.realWidth = width;
    }

    return { columnWidths, handleColumnResize };
}
