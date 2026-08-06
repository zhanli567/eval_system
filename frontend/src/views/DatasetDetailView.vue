<script setup>
import { computed } from 'vue';
import { useRoute } from 'vue-router';
import { ArrowDown, Delete, Plus, Refresh, Search } from '@element-plus/icons-vue';
import { useDatasetDetail } from '../modules/dataset/composables/useDatasetDetail';
const route = useRoute();
const datasetId = computed(() => String(route.params.datasetId ?? ''));
const { detailLoading, datasetTitle, versions, activeVersionId, tablePage, tableSize, searchFieldId, searchKeyword, fieldVisible, rowVisible, rowEditingId, excelInput, coverExcelInput, draggedFieldIndex, dragOverFieldIndex, fieldForm, rowForm, activeVersion, isDraft, tableRows, tableTotal, fields, dataTableKey, loadDataset, selectVersion, loadDetail, changeTableSize, backToList, addField, removeField, startFieldDrag, enterFieldDrag, dropField, endFieldDrag, openFieldDialog, submitFields, openRowDialog, submitRow, removeRow, handleAddDataCommand, importExcel, coverExcel, publishDraft, removeVersion, coverDraft, formatTime } = useDatasetDetail(datasetId);
</script>

<template>
  <header class="topbar detail-topbar">
    <nav class="page-breadcrumb" aria-label="页面路径">
      <button type="button" class="page-breadcrumb-link" @click="backToList">评测集</button>
      <span class="page-breadcrumb-separator">/</span>
      <OverflowTooltip :content="datasetTitle" tag="span" class="page-breadcrumb-current" />
    </nav>
  </header>

  <section v-if="versions.length" class="detail-panel standalone-detail-panel fill-workspace">
    <aside class="version-rail dataset-version-rail">
      <div class="rail-title">
        <span>评测集版本</span>
        <strong>{{ versions.filter((version) => !version.draft).length }}</strong>
      </div>
      <button
        v-for="version in versions"
        :key="version.id"
        class="version-item"
        :class="{ active: activeVersionId === version.id }"
        @click="selectVersion(version.id)"
      >
        <span class="version-item-title">{{ version.versionName }}</span>
        <span class="version-item-meta">
          <small>{{ version.createdByName || '-' }}</small>
          <small>{{ formatTime(version.createdDate) }}</small>
        </span>
      </button>
    </aside>

    <div class="version-content" v-loading="detailLoading">
      <div class="version-head">
        <div>
          <h2>{{ activeVersion?.versionName || '-' }}</h2>
          <span class="meta">数据量 {{ activeVersion?.itemCount ?? 0 }} · {{ isDraft ? '草稿可编辑' : '发布版本只读' }}</span>
        </div>
        <div class="version-actions">
          <template v-if="isDraft">
            <el-button @click="openFieldDialog">编辑表头</el-button>
            <el-dropdown trigger="hover" @command="handleAddDataCommand">
              <el-button type="primary">
                添加数据
                <el-icon class="el-icon--right"><ArrowDown /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="single">单条新增</el-dropdown-item>
                  <el-dropdown-item command="import">批量导入</el-dropdown-item>
                  <el-dropdown-item command="cover">全量覆盖</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
            <input ref="excelInput" class="hidden-file" type="file" accept=".xlsx,.xls" @change="importExcel" />
            <input ref="coverExcelInput" class="hidden-file" type="file" accept=".xlsx,.xls" @change="coverExcel" />
            <el-button type="success" @click="publishDraft">发布</el-button>
          </template>
          <template v-else-if="activeVersion">
            <el-button @click="coverDraft(activeVersion)">覆盖当前草稿</el-button>
            <el-button type="danger" plain @click="removeVersion(activeVersion)">删除版本</el-button>
          </template>
        </div>
      </div>

      <div class="panel-toolbar">
        <el-select v-model="searchFieldId" clearable placeholder="全部" class="field-select">
          <el-option v-for="field in fields" :key="field.id" :label="field.fieldName" :value="field.id" />
        </el-select>
        <el-input v-model="searchKeyword" clearable placeholder="请输入关键词" class="search-input" @keyup.enter="loadDetail" />
        <el-button class="search-icon-button" :icon="Search" title="筛选" aria-label="筛选" @click="loadDetail" />
        <div class="table-toolbar-actions">
          <el-button class="toolbar-icon-button" :icon="Refresh" title="刷新" aria-label="刷新" @click="loadDataset" />
        </div>
      </div>

      <el-table :key="dataTableKey" :data="tableRows" row-key="id" border height="100%" tooltip-effect="light" class="data-table">
        <el-table-column type="index" label="序号" width="90" fixed="left" :resizable="false" align="center" />
        <el-table-column
          v-for="field in fields"
          :key="`${field.id}:${field.fieldName}:${field.required}:${field.displayOrder}`"
          :label="field.fieldName"
          min-width="220"
          :resizable="false"
        >
          <template #header>
            <span>{{ field.fieldName }}</span>
            <span v-if="field.required" class="required-mark">*</span>
          </template>
          <template #default="{ row }">
            <OverflowTooltip :content="row.values[field.id || ''] || '-'" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="130" fixed="right" :resizable="false" align="center">
          <template #default="{ row }">
            <el-button link type="primary" :disabled="!isDraft" @click="openRowDialog(row)">编辑</el-button>
            <el-button link type="danger" :disabled="!isDraft" @click="removeRow(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager-row">
        <el-pagination
          v-model:current-page="tablePage"
          v-model:page-size="tableSize"
          :page-sizes="[5, 10, 20]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="tableTotal"
          @size-change="changeTableSize"
          @current-change="loadDetail"
        />
      </div>
    </div>
  </section>

  <el-empty v-else v-loading="detailLoading" description="暂无版本数据" />

  <el-dialog v-model="fieldVisible" title="编辑表头" class="dataset-field-dialog fixed-dialog" style="--fixed-dialog-width: min(860px, 86vw); --fixed-dialog-height: min(640px, 86vh)" :close-on-click-modal="true">
    <div class="dialog-subtitle">
      <span>草稿表结构</span>
      <el-button link type="primary" :icon="Plus" @click="addField(fieldForm)">添加列</el-button>
    </div>
    <div class="field-editor-list">
      <div
        v-for="(field, index) in fieldForm"
        :key="field.id || index"
        class="field-editor"
        :class="{ 'is-dragging': draggedFieldIndex === index, 'is-drop-target': dragOverFieldIndex === index }"
        @dragenter.prevent="enterFieldDrag(index)"
        @dragover.prevent
        @drop="dropField(fieldForm, index)"
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
        <el-select v-model="field.fieldType" clearable placeholder="类型" :disabled="Boolean(field.id)">
          <el-option label="文本" value="string" />
          <el-option label="数字" value="number" />
          <el-option label="布尔" value="boolean" />
        </el-select>
        <el-checkbox v-model="field.required" :disabled="Boolean(field.id)">必填</el-checkbox>
        <el-input v-model="field.description" placeholder="描述" />
        <el-button :icon="Delete" circle @click="removeField(fieldForm, index)" />
      </div>
    </div>
    <template #footer>
      <el-button @click="fieldVisible = false">取消</el-button>
      <el-button type="primary" @click="submitFields">保存</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="rowVisible" :title="rowEditingId ? '编辑数据' : '新增数据'" class="dataset-row-dialog fixed-dialog" style="--fixed-dialog-width: min(780px, 86vw); --fixed-dialog-height: min(620px, 86vh)" :close-on-click-modal="true">
    <el-form label-position="top">
      <el-form-item v-for="field in fields" :key="field.id">
        <template #label>
          {{ field.fieldName }} <span v-if="field.required" class="required-mark">*</span>
        </template>
        <el-input
          v-if="field.fieldType === 'string'"
          v-model="rowForm[field.id || '']"
          type="textarea"
          :autosize="{ minRows: 2, maxRows: 5 }"
        />
        <el-input
          v-else-if="field.fieldType === 'number'"
          v-model="rowForm[field.id || '']"
          type="number"
          placeholder="请输入数字"
        />
        <el-select
          v-else-if="field.fieldType === 'boolean'"
          v-model="rowForm[field.id || '']"
          clearable
          placeholder="请选择布尔值"
        >
          <el-option label="true" value="true" />
          <el-option label="false" value="false" />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="rowVisible = false">取消</el-button>
      <el-button type="primary" @click="submitRow">保存</el-button>
    </template>
  </el-dialog>
</template>
