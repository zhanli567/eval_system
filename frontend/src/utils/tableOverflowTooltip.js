export const TABLE_OVERFLOW_TOOLTIP_CLASS = 'table-overflow-tooltip';

const RESIZED_CLASS = 'table-overflow-tooltip--resized';
const RESIZING_CLASS = 'table-overflow-tooltip--resizing';
const TOP_RESIZING_CLASS = 'table-overflow-tooltip--resizing-top';
const BOTTOM_RESIZING_CLASS = 'table-overflow-tooltip--resizing-bottom';
const RESIZE_ZONE_SIZE = 18;
const MIN_WIDTH = 260;
const MIN_HEIGHT = 120;
const VIEWPORT_MARGIN = 24;

export const tableOverflowTooltipOptions = {
    effect: 'light',
    enterable: true,
    hideAfter: 1200,
    popperClass: TABLE_OVERFLOW_TOOLTIP_CLASS
};

export function installTableOverflowTooltipResize() {
    if (typeof document === 'undefined') {
        return;
    }
    if (window.__agentNexusTableOverflowTooltipResizeInstalled) {
        return;
    }
    window.__agentNexusTableOverflowTooltipResizeInstalled = true;
    document.addEventListener('pointerdown', handleResizeStart, true);
}

function handleResizeStart(event) {
    const target = event.target instanceof Element ? event.target : null;
    const tooltip = target?.closest(`.${TABLE_OVERFLOW_TOOLTIP_CLASS}`);
    if (!tooltip || !isInResizeZone(event, tooltip)) {
        return;
    }
    const state = createResizeState(event, tooltip);
    const handleMove = (moveEvent) => handleResizeMove(moveEvent, state);
    const handleEnd = () => finishResize(tooltip, handleMove, handleEnd);
    event.preventDefault();
    event.stopPropagation();
    tooltip.classList.add(RESIZED_CLASS, RESIZING_CLASS);
    document.body.classList.add(RESIZING_CLASS, resolveBodyResizeClass(state));
    document.addEventListener('pointermove', handleMove);
    document.addEventListener('pointerup', handleEnd, { once: true });
    document.addEventListener('pointercancel', handleEnd, { once: true });
}

function isInResizeZone(event, tooltip) {
    const rect = tooltip.getBoundingClientRect();
    const placement = tooltip.getAttribute('data-popper-placement') || '';
    const isTop = placement.startsWith('top');
    const isBottom = placement.startsWith('bottom') || !isTop;
    const inRightEdge = event.clientX >= rect.right - RESIZE_ZONE_SIZE;
    const inTopEdge = event.clientY <= rect.top + RESIZE_ZONE_SIZE;
    const inBottomEdge = event.clientY >= rect.bottom - RESIZE_ZONE_SIZE;
    if (isTop) {
        return inRightEdge && inTopEdge;
    }
    if (isBottom) {
        return inRightEdge && inBottomEdge;
    }
    return false;
}

function createResizeState(event, tooltip) {
    const rect = tooltip.getBoundingClientRect();
    const placement = tooltip.getAttribute('data-popper-placement') || '';
    const translate = readTranslate(tooltip);
    return {
        tooltip,
        isTopPlacement: placement.startsWith('top'),
        startX: event.clientX,
        startY: event.clientY,
        startWidth: rect.width,
        startHeight: rect.height,
        startTranslateX: translate.x,
        startTranslateY: translate.y,
        maxWidth: Math.max(MIN_WIDTH, document.documentElement.clientWidth - rect.left - VIEWPORT_MARGIN),
        maxHeight: resolveMaxHeight(rect, placement)
    };
}

function handleResizeMove(event, state) {
    event.preventDefault();
    const nextWidth = clamp(state.startWidth + event.clientX - state.startX, MIN_WIDTH, state.maxWidth);
    const nextHeight = clamp(
        state.startHeight + resolveHeightDelta(event, state),
        MIN_HEIGHT,
        state.maxHeight
    );
    state.tooltip.style.width = `${Math.round(nextWidth)}px`;
    state.tooltip.style.height = `${Math.round(nextHeight)}px`;
    if (state.isTopPlacement) {
        const heightDelta = nextHeight - state.startHeight;
        state.tooltip.style.transform = `translate(${state.startTranslateX}px, ${state.startTranslateY - heightDelta}px)`;
    }
}

function finishResize(tooltip, handleMove, handleEnd) {
    tooltip.classList.remove(RESIZING_CLASS);
    document.body.classList.remove(RESIZING_CLASS, TOP_RESIZING_CLASS, BOTTOM_RESIZING_CLASS);
    document.removeEventListener('pointermove', handleMove);
    document.removeEventListener('pointerup', handleEnd);
    document.removeEventListener('pointercancel', handleEnd);
}

function resolveBodyResizeClass(state) {
    if (state.isTopPlacement) {
        return TOP_RESIZING_CLASS;
    }
    return BOTTOM_RESIZING_CLASS;
}

function resolveHeightDelta(event, state) {
    if (state.isTopPlacement) {
        return state.startY - event.clientY;
    }
    return event.clientY - state.startY;
}

function resolveMaxHeight(rect, placement) {
    const viewportHeight = document.documentElement.clientHeight;
    if ((placement || '').startsWith('top')) {
        return Math.max(MIN_HEIGHT, rect.bottom - VIEWPORT_MARGIN);
    }
    return Math.max(MIN_HEIGHT, viewportHeight - rect.top - VIEWPORT_MARGIN);
}

function readTranslate(element) {
    const transform = element.style.transform || window.getComputedStyle(element).transform;
    if (!transform || transform === 'none') {
        return { x: 0, y: 0 };
    }
    const matrix = new DOMMatrixReadOnly(transform);
    return { x: matrix.m41, y: matrix.m42 };
}

function clamp(value, min, max) {
    return Math.min(Math.max(value, min), max);
}
