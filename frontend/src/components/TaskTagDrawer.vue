<script setup>
import { computed } from 'vue';
import { Plus, Refresh, Search } from '@element-plus/icons-vue';
import { tagTypeLabel } from '../utils/taskLabels';

const props = defineProps({
    modelValue: { type: Boolean, default: false },
    title: { type: String, default: '标签配置' },
    tags: { type: Array, default: () => [] },
    selectedTagIds: { type: Array, default: () => [] },
    tagTypeOptions: { type: Array, default: () => [] },
    loading: { type: Boolean, default: false },
    keyword: { type: String, default: '' },
    tagType: { type: String, default: '' },
    selectedMode: { type: String, default: 'remove' }
});
const emit = defineEmits([
    'update:modelValue',
    'update:keyword',
    'update:tagType',
    'refresh',
    'create',
    'add',
    'remove'
]);

const visible = computed({
    get: () => props.modelValue,
    set: (value) => emit('update:modelValue', value)
});
const searchKeyword = computed({
    get: () => props.keyword,
    set: (value) => emit('update:keyword', value)
});
const selectedType = computed({
    get: () => props.tagType,
    set: (value) => emit('update:tagType', value)
});
const selectedIds = computed(() => new Set(props.selectedTagIds));
const filteredTags = computed(() => {
    const keyword = searchKeyword.value.trim().toLowerCase();
    return props.tags.filter((tag) => {
        const matchesType = !selectedType.value || tag.tagType === selectedType.value;
        const text = `${tag.tagName || ''} ${tag.description || ''}`.toLowerCase();
        const matchesKeyword = !keyword || text.includes(keyword);
        return matchesType && matchesKeyword;
    });
});

function isSelected(tag) {
    return selectedIds.value.has(tag.id);
}

function selectedButtonText() {
    return props.selectedMode === 'disabled' ? '已添加' : '移除';
}

function handleSelectedClick(tag) {
    if (props.selectedMode === 'disabled') {
        return;
    }
    emit('remove', tag);
}
</script>

<template>
  <el-drawer v-model="visible" :title="title" direction="rtl" size="520px" class="task-tag-drawer">
    <div class="task-tag-drawer-toolbar">
      <el-select v-model="selectedType" clearable placeholder="全部类型" class="task-tag-type-select">
        <el-option v-for="type in tagTypeOptions" :key="type.value" :label="type.label" :value="type.value" />
      </el-select>
      <el-input
        v-model="searchKeyword"
        clearable
        placeholder="请输入标签名称"
        maxlength="20"
        show-word-limit
        class="task-tag-search"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
      <el-button class="toolbar-icon-button" :icon="Refresh" title="刷新" aria-label="刷新" @click="emit('refresh')" />
      <el-button :icon="Plus" @click="emit('create')">创建标签</el-button>
    </div>

    <div v-loading="loading" class="task-tag-drawer-list">
      <article v-for="tag in filteredTags" :key="tag.id" class="task-tag-drawer-card">
        <div class="task-tag-drawer-card-main">
          <div class="tag-title-row">
            <strong>{{ tag.tagName }}</strong>
            <span class="tag-type-text">{{ tagTypeLabel(tag.tagType) }}</span>
          </div>
          <p>{{ tag.description || '暂无描述' }}</p>
        </div>
        <el-button
          v-if="isSelected(tag)"
          plain
          :type="selectedMode === 'disabled' ? 'info' : 'danger'"
          :disabled="selectedMode === 'disabled'"
          @click="handleSelectedClick(tag)"
        >
          {{ selectedButtonText() }}
        </el-button>
        <el-button v-else plain type="primary" @click="emit('add', tag)">添加</el-button>
      </article>
      <el-empty v-if="!filteredTags.length" description="暂无匹配标签" :image-size="80" />
    </div>
  </el-drawer>
</template>
