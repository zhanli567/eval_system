<script setup>
import { CircleCheck, CircleClose, Clock, CopyDocument, Loading, Plus, Refresh, Search } from '@element-plus/icons-vue';
import { useTaskManagement } from '../modules/task/composables/useTaskManagement';
const { loading, tasks, total, page, size, keyword, status, sortBy, sortOrder, statusOptions, loadTasks, searchTasks, changeSize, openCreate, openDetail, copyTask, startTask, stopTask, isStartingTask, isStoppingTask, removeTask, canStartTask, canStopTask, canDeleteTask, toggleSort, formatAppBinding, statusLabel, formatTime } = useTaskManagement();
const statusIcons = {
    pending: Clock,
    running: Loading,
    completed: CircleCheck,
    failed: CircleClose,
    stopped: CircleClose
};
function statusIcon(value) {
    return statusIcons[value] || Clock;
}
function statusIconClass(value) {
    return `is-${value || 'pending'}`;
}
function formatNameVersion(name, version) {
    return `${name || '-'} / ${version || '-'}`;
}
function formatEvaluatorList(evaluators) {
    return formatNameList(evaluators, (item) => {
        const name = item.evaluatorName || item.versionName || '-';
        const version = item.versionName || '-';
        const passRate = item.passRate === undefined || item.passRate === null ? '-' : `${item.passRate}%`;
        return `${name} / ${version} / 通过率 ${passRate}`;
    });
}
function formatTagList(tags) {
    return formatNameList(tags, (item) => item.tagName);
}
function formatNameList(items, picker) {
    if (!items?.length) {
        return '-';
    } else {
        return items.map((item) => picker(item) || '-').join('、');
    }
}
</script>

<template>
  <section class="task-panel">
    <div class="panel-toolbar task-toolbar">
      <el-select v-model="status" clearable placeholder="全部状态" class="field-select" @change="searchTasks">
        <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
      <el-input
        v-model="keyword"
        clearable
        maxlength="50"
        show-word-limit
        placeholder="请输入任务名称"
        class="search-input"
        @keyup.enter="searchTasks"
        @clear="searchTasks"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
      <el-button class="search-icon-button" :icon="Search" title="搜索" aria-label="搜索" @click="searchTasks" />
      <div class="table-toolbar-actions">
        <el-button class="toolbar-icon-button" :icon="Refresh" title="刷新" aria-label="刷新" @click="loadTasks" />
        <el-button type="primary" :icon="Plus" @click="openCreate">创建评测任务</el-button>
      </div>
    </div>

    <el-table
      v-loading="loading"
      :data="tasks"
      row-key="base.id"
      border
      height="100%"
      tooltip-effect="light"
      class="task-table"
    >
      <el-table-column prop="status" label="评测状态" width="90" fixed="left" :resizable="false" align="center">
        <template #default="{ row }">
          <el-tooltip :content="statusLabel(row.base.status)" placement="top" effect="light">
            <el-icon class="task-status-icon" :class="statusIconClass(row.base.status)">
              <component :is="statusIcon(row.base.status)" />
            </el-icon>
          </el-tooltip>
        </template>
      </el-table-column>
      <el-table-column prop="taskName" label="任务名称" min-width="220" :resizable="false">
        <template #default="{ row }">
          <OverflowTooltip
            :content="row.base.taskName"
            class="linkish"
            role="button"
            tabindex="0"
            @click="openDetail(row)"
            @keyup.enter="openDetail(row)"
            @keyup.space.prevent="openDetail(row)"
          />
        </template>
      </el-table-column>
      <el-table-column prop="datasetName" label="评测集名称" min-width="210" :resizable="false">
        <template #default="{ row }">
          <OverflowTooltip :content="formatNameVersion(row.base.datasetName, row.base.datasetVersionName)" />
        </template>
      </el-table-column>
      <el-table-column column-key="app" label="应用" min-width="260" :resizable="false">
        <template #default="{ row }">
          <OverflowTooltip :content="formatAppBinding(row.base)" />
        </template>
      </el-table-column>
      <el-table-column prop="description" label="描述" min-width="260" :resizable="false">
        <template #default="{ row }">
          <OverflowTooltip :content="row.base.description || '暂无描述'" />
        </template>
      </el-table-column>
      <el-table-column column-key="evaluators" label="评估器" min-width="220" :resizable="false">
        <template #default="{ row }">
          <OverflowTooltip :content="formatEvaluatorList(row.evaluators)" />
        </template>
      </el-table-column>
      <el-table-column column-key="tags" label="标签" min-width="190" :resizable="false" align="center">
        <template #default="{ row }">
          <OverflowTooltip :content="formatTagList(row.tags)" />
        </template>
      </el-table-column>
      <el-table-column prop="createdByName" label="创建人" min-width="140" :resizable="false" align="center">
        <template #default="{ row }">
          <OverflowTooltip :content="row.base.createdByName || '-'" />
        </template>
      </el-table-column>
      <el-table-column prop="createdDate" min-width="190" :resizable="false" align="center">
        <template #header>
          <SortableHeader
            label="创建时间"
            field="createdDate"
            :sort-by="sortBy"
            :sort-order="sortOrder"
            @toggle="toggleSort"
          />
        </template>
        <template #default="{ row }">
          <OverflowTooltip :content="formatTime(row.base.createdDate)" />
        </template>
      </el-table-column>
      <el-table-column prop="lastUpdatedDate" min-width="190" :resizable="false" align="center">
        <template #header>
          <SortableHeader
            label="更新时间"
            field="lastUpdatedDate"
            :sort-by="sortBy"
            :sort-order="sortOrder"
            @toggle="toggleSort"
          />
        </template>
        <template #default="{ row }">
          <OverflowTooltip :content="formatTime(row.base.lastUpdatedDate)" />
        </template>
      </el-table-column>
      <el-table-column column-key="actions" label="操作" width="260" fixed="right" :resizable="false" align="center">
        <template #default="{ row }">
          <el-button link type="primary" @click.stop="openDetail(row)">详情</el-button>
          <el-button link type="primary" :icon="CopyDocument" @click.stop="copyTask(row)">复制</el-button>
          <el-button
            link
            type="primary"
            :loading="isStartingTask(row.base.id)"
            :disabled="!canStartTask(row) || isStartingTask(row.base.id)"
            @click.stop="startTask(row)"
          >
            开始
          </el-button>
          <el-button
            link
            type="danger"
            :loading="isStoppingTask(row.base.id)"
            :disabled="!canStopTask(row) || isStoppingTask(row.base.id)"
            @click.stop="stopTask(row)"
          >
            停止
          </el-button>
          <el-button link type="danger" :disabled="!canDeleteTask(row)" @click.stop="removeTask(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pager-row">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="size"
        :page-sizes="[5, 10, 20]"
        layout="total, sizes, prev, pager, next, jumper"
        :total="total"
        @size-change="changeSize"
        @current-change="loadTasks"
      />
    </div>
  </section>
</template>
