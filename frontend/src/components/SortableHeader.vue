<script setup>
import { computed } from 'vue';
import { CaretBottom, CaretTop } from '@element-plus/icons-vue';

const props = defineProps({
    label: {
        type: String,
        required: true
    },
    field: {
        type: String,
        required: true
    },
    sortBy: {
        type: String,
        default: ''
    },
    sortOrder: {
        type: String,
        default: ''
    }
});

defineEmits(['toggle']);

const active = computed(() => props.sortBy === props.field);
const nextOrderLabel = computed(() => {
    if (active.value && props.sortOrder === 'desc') {
        return '升序';
    } else {
        return '降序';
    }
});
const ariaLabel = computed(() => `${props.label}，点击${nextOrderLabel.value}`);
</script>

<template>
  <button type="button" class="sortable-header" :aria-label="ariaLabel" :title="ariaLabel" @click="$emit('toggle', field)">
    <span class="sortable-header-label">{{ label }}</span>
    <span class="sortable-header-icon" aria-hidden="true">
      <el-icon class="sort-caret sort-caret-up" :class="{ active: active && sortOrder === 'asc' }">
        <CaretTop />
      </el-icon>
      <el-icon class="sort-caret sort-caret-down" :class="{ active: active && sortOrder === 'desc' }">
        <CaretBottom />
      </el-icon>
    </span>
  </button>
</template>
