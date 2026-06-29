<script setup>
import { useDatasetList } from '../modules/dataset/composables/useDatasetList';
const { datasetLoading, datasets, datasetTotal, datasetPage, datasetSize, datasetKeyword, createVisible, draggedFieldIndex, dragOverFieldIndex, createForm, loadDatasets, openDataset, openCreateDialog, submitCreate, removeDataset, addField, removeField, startFieldDrag, enterFieldDrag, dropField, endFieldDrag, formatTime } = useDatasetList();
</script>

<template>
  <header class="topbar">
    <div>
      <p class="eyebrow">应用评测</p>
      <h1>评测集管理</h1>
    </div>
    <div class="top-actions">
      <el-button :icon="Refresh" @click="loadDatasets">刷新</el-button>
      <el-button type="primary" :icon="Plus" @click="openCreateDialog">创建评测集</el-button>
    </div>
  </header>

  <section class="dataset-panel">
    <div class="panel-toolbar">
      <el-input
        v-model="datasetKeyword"
        clearable
        placeholder="请输入评测集名称"
        class="search-input"
        @keyup.enter="loadDatasets"
        @clear="loadDatasets"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
      <el-button @click="loadDatasets">搜索</el-button>
    </div>

    <el-table
      v-loading="datasetLoading"
      :data="datasets"
      row-key="id"
      highlight-current-row
      tooltip-effect="light"
      class="dataset-table"
    >
      <el-table-column prop="name" label="评测集名称" min-width="220">
        <template #default="{ row }">
          <button class="table-link" type="button" @click.stop="openDataset(row)">{{ row.name }}</button>
        </template>
      </el-table-column>
      <el-table-column prop="publishedVersionCount" label="版本数量" width="110" />
      <el-table-column prop="latestItemCount" label="数据量" width="110" />
      <el-table-column prop="description" label="描述" min-width="260" show-overflow-tooltip>
        <template #default="{ row }">{{ row.description || '暂无描述' }}</template>
      </el-table-column>
      <el-table-column label="创建时间" width="190">
        <template #default="{ row }">{{ formatTime(row.createdDate) }}</template>
      </el-table-column>
      <el-table-column label="更新时间" width="190">
        <template #default="{ row }">{{ formatTime(row.lastUpdatedDate) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click.stop="openDataset(row)">详情</el-button>
          <el-button link type="danger" @click.stop="removeDataset(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pager-row">
      <el-pagination
        v-model:current-page="datasetPage"
        v-model:page-size="datasetSize"
        layout="total, prev, pager, next"
        :total="datasetTotal"
        @current-change="loadDatasets"
      />
    </div>
  </section>

  <el-dialog v-model="createVisible" title="创建评测集" width="760px" class="dataset-create-dialog">
    <el-form label-position="top">
      <el-form-item label="评测集名称">
        <el-input v-model="createForm.name" maxlength="50" show-word-limit />
      </el-form-item>
      <el-form-item label="描述">
        <el-input v-model="createForm.description" type="textarea" maxlength="200" show-word-limit />
      </el-form-item>
      <div class="dialog-subtitle">
        <span>表结构</span>
        <el-button link type="primary" :icon="Plus" @click="addField(createForm.fields)">添加列</el-button>
      </div>
      <div class="field-editor-list">
        <div
          v-for="(field, index) in createForm.fields"
          :key="index"
          class="field-editor"
          :class="{ 'is-dragging': draggedFieldIndex === index, 'is-drop-target': dragOverFieldIndex === index }"
          @dragenter.prevent="enterFieldDrag(index)"
          @dragover.prevent
          @drop="dropField(createForm.fields, index)"
        >
          <button
            class="drag-handle"
            type="button"
            draggable="true"
            aria-label="拖动调整列顺序"
            @dragstart="startFieldDrag(index)"
            @dragend="endFieldDrag"
          >
            <span class="drag-grip" aria-hidden="true">
              <span></span>
              <span></span>
              <span></span>
            </span>
          </button>
          <el-input v-model="field.fieldName" placeholder="列名" />
          <el-select v-model="field.fieldType" placeholder="类型">
            <el-option label="文本" value="string" />
            <el-option label="数字" value="number" />
            <el-option label="布尔" value="boolean" />
          </el-select>
          <el-checkbox v-model="field.required">必填</el-checkbox>
          <el-input v-model="field.description" placeholder="描述" />
          <el-button :icon="Delete" circle @click="removeField(createForm.fields, index)" />
        </div>
      </div>
    </el-form>
    <template #footer>
      <el-button @click="createVisible = false">取消</el-button>
      <el-button type="primary" @click="submitCreate">创建</el-button>
    </template>
  </el-dialog>
</template>
