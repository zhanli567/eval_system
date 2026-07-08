<script setup>
import { CircleCheck, CircleClose, Clock, Loading, Plus, Refresh, Search, Sort } from '@element-plus/icons-vue';
import { useTaskManagement } from '../modules/task/composables/useTaskManagement';
const { loading, tasks, total, page, size, keyword, status, sortBy, sortOrder, columnWidths, statusOptions, loadTasks, searchTasks, changeSize, openCreate, openDetail, startTask, isStartingTask, removeTask, canStartTask, canDeleteTask, toggleSort, handleColumnResize, statusLabel, formatTime } = useTaskManagement();
const statusIcons = {
    pending: Clock,
    running: Loading,
    completed: CircleCheck,
    failed: CircleClose
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
function formatAppBinding(base) {
    if (base.appType !== 'agent' || !base.appId)
        return '-';
    return [base.appId || '-', base.appVersionId || '-', base.appAgentAlias || '-'].join(' / ');
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
    if (!items?.length)
        return '-';
    return items.map((item) => picker(item) || '-').join('、');
}
</script>

<template>
  <header class="topbar">
    <div>
      <h1>评测任务</h1>
    </div>
    <div class="top-actions">
      <el-button :icon="Refresh" @click="loadTasks">刷新</el-button>
      <el-button type="primary" :icon="Plus" @click="openCreate">创建评测任务</el-button>
    </div>
  </header>

  <section class="task-panel">
    <div class="panel-toolbar task-toolbar">
      <el-select v-model="status" clearable class="field-select" @change="searchTasks">
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
      <el-button @click="searchTasks">搜索</el-button>
      <div class="task-sort-actions">
        <el-button :class="{ active: sortBy === 'lastUpdatedDate' }" :icon="Sort" @click="toggleSort('lastUpdatedDate')">
          更新时间 {{ sortBy === 'lastUpdatedDate' ? (sortOrder === 'desc' ? '降序' : '升序') : '' }}
        </el-button>
        <el-button :class="{ active: sortBy === 'createdDate' }" :icon="Sort" @click="toggleSort('createdDate')">
          创建时间 {{ sortBy === 'createdDate' ? (sortOrder === 'desc' ? '降序' : '升序') : '' }}
        </el-button>
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
      @header-dragend="handleColumnResize"
    >
      <el-table-column prop="status" label="评测状态" :width="columnWidths.status" min-width="76" align="center">
        <template #default="{ row }">
          <el-tooltip :content="statusLabel(row.base.status)" placement="top">
            <el-icon class="task-status-icon" :class="statusIconClass(row.base.status)">
              <component :is="statusIcon(row.base.status)" />
            </el-icon>
          </el-tooltip>
        </template>
      </el-table-column>
      <el-table-column prop="taskName" label="任务名称" :width="columnWidths.taskName" min-width="160" show-overflow-tooltip>
        <template #default="{ row }">
          <button class="table-link" type="button" @click="openDetail(row)">{{ row.base.taskName }}</button>
        </template>
      </el-table-column>
      <el-table-column prop="datasetName" label="评测集名称" :width="columnWidths.datasetName" min-width="160" show-overflow-tooltip>
        <template #default="{ row }">
          {{ formatNameVersion(row.base.datasetName, row.base.datasetVersionName) }}
        </template>
      </el-table-column>
      <el-table-column column-key="app" label="应用" :width="columnWidths.app" min-width="180" show-overflow-tooltip>
        <template #default="{ row }">
          {{ formatAppBinding(row.base) }}
        </template>
      </el-table-column>
      <el-table-column prop="description" label="描述" :width="columnWidths.description" min-width="180" show-overflow-tooltip>
        <template #default="{ row }">{{ row.base.description || '暂无描述' }}</template>
      </el-table-column>
      <el-table-column column-key="evaluators" label="评估器" :width="columnWidths.evaluators" min-width="160" show-overflow-tooltip>
        <template #default="{ row }">
          {{ formatEvaluatorList(row.evaluators) }}
        </template>
      </el-table-column>
      <el-table-column column-key="tags" label="标签" :width="columnWidths.tags" min-width="140" show-overflow-tooltip>
        <template #default="{ row }">
          {{ formatTagList(row.tags) }}
        </template>
      </el-table-column>
      <el-table-column prop="createdByName" label="创建人" :width="columnWidths.createdByName" min-width="100" show-overflow-tooltip>
        <template #default="{ row }">{{ row.base.createdByName || '-' }}</template>
      </el-table-column>
      <el-table-column prop="createdDate" label="创建时间" :width="columnWidths.createdDate" min-width="160">
        <template #default="{ row }">{{ formatTime(row.base.createdDate) }}</template>
      </el-table-column>
      <el-table-column column-key="actions" label="操作" :width="columnWidths.actions" min-width="160" fixed="right" :resizable="false">
        <template #default="{ row }">
          <el-button link type="primary" @click.stop="openDetail(row)">详情</el-button>
          <el-button
            v-if="canStartTask(row)"
            link
            type="primary"
            :loading="isStartingTask(row.base.id)"
            :disabled="isStartingTask(row.base.id)"
            @click.stop="startTask(row)"
          >
            开始
          </el-button>
          <el-button v-if="canDeleteTask(row)" link type="danger" @click.stop="removeTask(row)">删除</el-button>
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
