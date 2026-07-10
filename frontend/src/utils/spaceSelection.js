import { ref } from 'vue';

export const currentSpaceId = ref('');

export function getCurrentSpaceId() {
    return currentSpaceId.value;
}

export function setCurrentSpaceId(spaceId) {
    currentSpaceId.value = spaceId || '';
}

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
