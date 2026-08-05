export const TABLE_OVERFLOW_TOOLTIP_CLASS = 'table-overflow-tooltip';

const RESIZED_CLASS = 'table-overflow-tooltip--resized';
const RESIZING_CLASS = 'table-overflow-tooltip--resizing';
const TOP_RESIZING_CLASS = 'table-overflow-tooltip--resizing-top';
const BOTTOM_RESIZING_CLASS = 'table-overflow-tooltip--resizing-bottom';
const RESIZE_SHIELD_CLASS = 'table-overflow-tooltip-resize-shield';
const RESIZE_ZONE_SIZE = 18;
const MIN_WIDTH = 260;
const MIN_HEIGHT = 120;
const VIEWPORT_MARGIN = 24;

export const tableOverflowTooltipOptions = {
    effect: 'light',
    enterable: true,
    hideAfter: 0,
    persistent: true,
    appendTo: 'body',
    zIndex: 4200,
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
    const handleEnd = (endEvent) => finishResize(state, handleMove, handleEnd, endEvent);
    event.preventDefault();
    event.stopPropagation();
    tooltip.classList.add(RESIZED_CLASS, RESIZING_CLASS);
    document.body.classList.add(RESIZING_CLASS, resolveBodyResizeClass(state));
    lockTooltipHover(tooltip);
    tryCapturePointer(tooltip, event.pointerId);
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
    freezeTooltipPosition(tooltip, rect);
    const isTopPlacement = placement.startsWith('top');
    return {
        tooltip,
        isTopPlacement,
        pointerId: event.pointerId,
        shield: createResizeShield(isTopPlacement),
        startX: event.clientX,
        startY: event.clientY,
        startLeft: rect.left,
        startTop: rect.top,
        startBottom: rect.bottom,
        startWidth: rect.width,
        startHeight: rect.height,
        maxWidth: Math.max(MIN_WIDTH, document.documentElement.clientWidth - rect.left - VIEWPORT_MARGIN),
        maxHeight: resolveMaxHeight(rect, placement)
    };
}

function handleResizeMove(event, state) {
    event.preventDefault();
    event.stopPropagation();
    const nextWidth = clamp(state.startWidth + event.clientX - state.startX, MIN_WIDTH, state.maxWidth);
    const nextHeight = clamp(
        state.startHeight + resolveHeightDelta(event, state),
        MIN_HEIGHT,
        state.maxHeight
    );
    setLockedStyle(state.tooltip, 'left', `${Math.round(state.startLeft)}px`);
    setLockedStyle(state.tooltip, 'width', `${Math.round(nextWidth)}px`);
    setLockedStyle(state.tooltip, 'height', `${Math.round(nextHeight)}px`);
    if (state.isTopPlacement) {
        setLockedStyle(state.tooltip, 'top', `${Math.round(state.startBottom - nextHeight)}px`);
    } else {
        setLockedStyle(state.tooltip, 'top', `${Math.round(state.startTop)}px`);
    }
    setLockedStyle(state.tooltip, 'transform', 'none');
}

function finishResize(state, handleMove, handleEnd, event) {
    state.tooltip.classList.remove(RESIZING_CLASS);
    document.body.classList.remove(RESIZING_CLASS, TOP_RESIZING_CLASS, BOTTOM_RESIZING_CLASS);
    document.removeEventListener('pointermove', handleMove);
    document.removeEventListener('pointerup', handleEnd);
    document.removeEventListener('pointercancel', handleEnd);
    releasePointer(state.tooltip, state.pointerId);
    unlockTooltipHover(state.tooltip);
    state.shield?.remove();
    if (event && !isPointInsideTooltip(event, state.tooltip)) {
        bindTooltipCleanup(state.tooltip);
        triggerTooltipMouseLeave(state.tooltip, event);
    } else {
        bindTooltipCleanup(state.tooltip);
    }
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

function freezeTooltipPosition(tooltip, rect) {
    setLockedStyle(tooltip, 'position', 'fixed');
    setLockedStyle(tooltip, 'inset', 'auto');
    setLockedStyle(tooltip, 'left', `${Math.round(rect.left)}px`);
    setLockedStyle(tooltip, 'top', `${Math.round(rect.top)}px`);
    setLockedStyle(tooltip, 'right', 'auto');
    setLockedStyle(tooltip, 'bottom', 'auto');
    setLockedStyle(tooltip, 'transform', 'none');
}

function createResizeShield(isTopPlacement) {
    const shield = document.createElement('div');
    const directionClass = isTopPlacement ? `${RESIZE_SHIELD_CLASS}--top` : `${RESIZE_SHIELD_CLASS}--bottom`;
    shield.className = `${RESIZE_SHIELD_CLASS} ${directionClass}`;
    document.body.appendChild(shield);
    return shield;
}

function lockTooltipHover(tooltip) {
    tooltip.addEventListener('mouseleave', stopHoverSwitch, true);
    tooltip.addEventListener('pointerleave', stopHoverSwitch, true);
    document.addEventListener('mouseover', stopHoverSwitch, true);
    document.addEventListener('pointerover', stopHoverSwitch, true);
}

function unlockTooltipHover(tooltip) {
    tooltip.removeEventListener('mouseleave', stopHoverSwitch, true);
    tooltip.removeEventListener('pointerleave', stopHoverSwitch, true);
    document.removeEventListener('mouseover', stopHoverSwitch, true);
    document.removeEventListener('pointerover', stopHoverSwitch, true);
}

function stopHoverSwitch(event) {
    if (document.body.classList.contains(RESIZING_CLASS)) {
        event.stopImmediatePropagation();
    }
}

function tryCapturePointer(tooltip, pointerId) {
    try {
        tooltip.setPointerCapture?.(pointerId);
    } catch {
        return;
    }
}

function releasePointer(tooltip, pointerId) {
    try {
        tooltip.releasePointerCapture?.(pointerId);
    } catch {
        return;
    }
}

function isPointInsideTooltip(event, tooltip) {
    const rect = tooltip.getBoundingClientRect();
    return event.clientX >= rect.left && event.clientX <= rect.right && event.clientY >= rect.top && event.clientY <= rect.bottom;
}

function triggerTooltipMouseLeave(tooltip, event) {
    tooltip.dispatchEvent(new MouseEvent('mouseleave', {
        view: window,
        bubbles: false,
        cancelable: true,
        clientX: event.clientX,
        clientY: event.clientY
    }));
}

function bindTooltipCleanup(tooltip) {
    tooltip.addEventListener('mouseleave', cleanupTooltipLock, { once: true });
}

function cleanupTooltipLock(event) {
    const tooltip = event.currentTarget;
    window.setTimeout(() => {
        tooltip.classList.remove(RESIZED_CLASS);
        const lockedProperties = ['position', 'inset', 'left', 'top', 'right', 'bottom', 'transform', 'width', 'height'];
        lockedProperties.forEach((name) => {
            tooltip.style.removeProperty(name);
        });
    }, 0);
}

function setLockedStyle(element, name, value) {
    element.style.setProperty(name, value, 'important');
}

function clamp(value, min, max) {
    return Math.min(Math.max(value, min), max);
}
