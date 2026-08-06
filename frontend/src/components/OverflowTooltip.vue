<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { TABLE_OVERFLOW_TOOLTIP_CLASS } from '../utils/tableOverflowTooltip';

defineOptions({
    inheritAttrs: false
});

const props = defineProps({
    content: {
        type: [String, Number],
        default: ''
    },
    fallback: {
        type: String,
        default: '-'
    },
    placement: {
        type: String,
        default: 'top'
    },
    tag: {
        type: String,
        default: 'span'
    }
});

const textRef = ref();
const isOverflow = ref(false);
let resizeObserver;

const displayText = computed(() => {
    const value = props.content === undefined || props.content === null ? '' : String(props.content);
    return value ? value : props.fallback;
});

function updateOverflowState() {
    const el = resolveTextElement();
    if (el) {
        isOverflow.value = el.scrollWidth > el.clientWidth || el.scrollHeight > el.clientHeight;
    } else {
        isOverflow.value = false;
    }
}

function resolveTextElement() {
    const el = textRef.value?.$el || textRef.value;
    return isDomElement(el) ? el : null;
}

function isDomElement(el) {
    return typeof Element !== 'undefined' && el instanceof Element;
}

function observeTextElement() {
    if (!window.ResizeObserver) {
        return;
    }
    resizeObserver = new ResizeObserver(updateOverflowState);
    const el = resolveTextElement();
    if (el) {
        resizeObserver.observe(el);
    }
}

onMounted(() => {
    nextTick(() => {
        updateOverflowState();
        observeTextElement();
    });
});

onBeforeUnmount(() => {
    if (resizeObserver) {
        resizeObserver.disconnect();
    }
});

watch(displayText, () => {
    nextTick(updateOverflowState);
});
</script>

<template>
  <el-tooltip
    :content="displayText"
    :placement="placement"
    effect="light"
    :popper-class="TABLE_OVERFLOW_TOOLTIP_CLASS"
    :disabled="!isOverflow"
  >
    <component :is="tag" ref="textRef" v-bind="$attrs" class="overflow-tooltip-text">
      <slot>{{ displayText }}</slot>
    </component>
  </el-tooltip>
</template>
