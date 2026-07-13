import { ref } from 'vue';

export const currentSpaceId = ref('');

export function activeSpaces(spaces) {
    return Array.isArray(spaces)
        ? spaces.filter((space) => space?.id && String(space.status || '').toUpperCase() === 'ACTIVE')
        : [];
}

export function resolveSpaceSelection(spaces, selectedSpaceId) {
    const availableSpaces = activeSpaces(spaces);
    if (!availableSpaces.length) {
        return '';
    } else {
        return availableSpaces.some((space) => space.id === selectedSpaceId)
            ? selectedSpaceId
            : availableSpaces[0].id;
    }
}

export function findSelectedSpace(spaces, spaceId) {
    return activeSpaces(spaces).find((space) => space.id === spaceId) || null;
}
