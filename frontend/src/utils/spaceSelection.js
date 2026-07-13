export const SPACE_STORAGE_KEY = 'agentnexus.currentSpaceId';

export function activeSpaces(spaces) {
    return Array.isArray(spaces)
        ? spaces.filter((space) => space?.id && String(space.status || '').toUpperCase() === 'ACTIVE')
        : [];
}

export function resolveSpaceSelection(spaces, storedSpaceId) {
    const availableSpaces = activeSpaces(spaces);
    if (!availableSpaces.length) {
        return '';
    }
    return availableSpaces.some((space) => space.id === storedSpaceId)
        ? storedSpaceId
        : availableSpaces[0].id;
}

export function findSelectedSpace(spaces, spaceId) {
    return activeSpaces(spaces).find((space) => space.id === spaceId) || null;
}
