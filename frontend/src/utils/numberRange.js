export const NUMBER_VALUE_MIN = -100000;
export const NUMBER_VALUE_MAX = 100000;
export const NUMBER_VALUE_RANGE_TEXT = '-100000到100000';

export function isNumberValueMissing(value) {
    if (typeof value === 'string' && !value.trim()) {
        return true;
    }
    return value === null || value === undefined || Number.isNaN(Number(value));
}

export function isNumberValueOutOfRange(value) {
    if (isNumberValueMissing(value)) {
        return false;
    }
    const numberValue = Number(value);
    return numberValue < NUMBER_VALUE_MIN || numberValue > NUMBER_VALUE_MAX;
}

export function hasNumberValueOutOfRange(values) {
    return values.some((value) => isNumberValueOutOfRange(value));
}
