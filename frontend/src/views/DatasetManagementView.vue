<script setup lang="ts">
import { ArrowDown, Delete, Plus, Refresh, Search } from '@element-plus/icons-vue'
import { useDatasetManagement } from '../modules/dataset/composables/useDatasetManagement'

const {
  datasetLoading,
  detailLoading,
  datasets,
  datasetTotal,
  datasetPage,
  datasetSize,
  datasetKeyword,
  selectedDataset,
  versions,
  activeVersionId,
  tablePage,
  tableSize,
  searchFieldId,
  searchKeyword,
  createVisible,
  fieldVisible,
  rowVisible,
  rowEditingId,
  excelInput,
  coverExcelInput,
  createForm,
  fieldForm,
  rowForm,
  activeVersion,
  isDraft,
  tableRows,
  tableTotal,
  fields,
  dataTableKey,
  loadDatasets,
  selectDataset,
  selectVersion,
  loadDetail,
  openCreateDialog,
  submitCreate,
  removeDataset,
  addField,
  removeField,
  startFieldDrag,
  dropField,
  endFieldDrag,
  openFieldDialog,
  submitFields,
  openRowDialog,
  submitRow,
  removeRow,
  handleAddDataCommand,
  importExcel,
  coverExcel,
  publishDraft,
  removeVersion,
  coverDraft,
  formatTime
} = useDatasetManagement()
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
      class="dataset-table"
      @row-click="selectDataset"
    >
      <el-table-column prop="name" label="评测集名称" min-width="220">
        <template #default="{ row }">
          <span class="linkish">{{ row.name }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="publishedVersionCount" label="版本数量" width="110" />
      <el-table-column prop="latestItemCount" label="数据量" width="110" />
      <el-table-column prop="description" label="描述" min-width="260">
        <template #default="{ row }">{{ row.description || '暂无描述' }}</template>
      </el-table-column>
      <el-table-column label="创建时间" width="190">
        <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="更新时间" width="190">
        <template #default="{ row }">{{ formatTime(row.updatedAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
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

  <section v-if="selectedDataset" class="detail-panel">
    <aside class="version-rail">
      <div class="rail-title">
        <span>评测集版本</span>
        <strong>{{ versions.length }}</strong>
      </div>
      <button
        v-for="version in versions"
        :key="version.id"
        class="version-item"
        :class="{ active: activeVersionId === version.id }"
        @click="selectVersion(version.id)"
      >
        <span>{{ version.versionName }}</span>
        <small>{{ version.itemCount }} 条</small>
      </button>
    </aside>

    <div class="version-content" v-loading="detailLoading">
      <div class="version-head">
        <div>
          <p class="eyebrow">{{ selectedDataset.name }}</p>
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
        <el-select v-model="searchFieldId" clearable placeholder="选择搜索列" class="field-select">
          <el-option v-for="field in fields" :key="field.id" :label="field.fieldName" :value="field.id" />
        </el-select>
        <el-input v-model="searchKeyword" clearable placeholder="请输入关键词" class="search-input" @keyup.enter="loadDetail" />
        <el-button @click="loadDetail">筛选</el-button>
      </div>

      <el-table :key="dataTableKey" :data="tableRows" row-key="id" border class="data-table">
        <el-table-column label="序号" width="90">
          <template #default="{ row }"># {{ row.rowNo }}</template>
        </el-table-column>
        <el-table-column
          v-for="field in fields"
          :key="`${field.id}:${field.fieldName}:${field.required}:${field.displayOrder}`"
          :label="field.fieldName"
          min-width="220"
          show-overflow-tooltip
        >
          <template #header>
            <span>{{ field.fieldName }}</span>
            <el-tag v-if="field.required" size="small" type="danger" effect="plain">必填</el-tag>
          </template>
          <template #default="{ row }">
            {{ row.values[field.id || ''] || '-' }}
          </template>
        </el-table-column>
        <el-table-column v-if="isDraft" label="操作" width="130" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openRowDialog(row)">编辑</el-button>
            <el-button link type="danger" @click="removeRow(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager-row">
        <el-pagination
          v-model:current-page="tablePage"
          v-model:page-size="tableSize"
          layout="total, prev, pager, next"
          :total="tableTotal"
          @current-change="loadDetail"
        />
      </div>
    </div>
  </section>

  <el-empty v-else description="暂无评测集，创建一个开始演示" />

  <el-dialog v-model="createVisible" title="创建评测集" width="760px">
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
            拖动
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

  <el-dialog v-model="fieldVisible" title="编辑表头" width="780px">
    <div class="dialog-subtitle">
      <span>草稿表结构</span>
      <el-button link type="primary" :icon="Plus" @click="addField(fieldForm)">添加列</el-button>
    </div>
    <div class="field-editor-list">
      <div
        v-for="(field, index) in fieldForm"
        :key="field.id || index"
        class="field-editor"
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
          拖动
        </button>
        <el-input v-model="field.fieldName" placeholder="列名" />
        <el-select v-model="field.fieldType" placeholder="类型">
          <el-option label="文本" value="string" />
          <el-option label="数字" value="number" />
          <el-option label="布尔" value="boolean" />
        </el-select>
        <el-checkbox v-model="field.required">必填</el-checkbox>
        <el-input v-model="field.description" placeholder="描述" />
        <el-button :icon="Delete" circle @click="removeField(fieldForm, index)" />
      </div>
    </div>
    <template #footer>
      <el-button @click="fieldVisible = false">取消</el-button>
      <el-button type="primary" @click="submitFields">保存</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="rowVisible" :title="rowEditingId ? '编辑数据' : '新增数据'" width="720px">
    <el-form label-position="top">
      <el-form-item v-for="field in fields" :key="field.id" :label="field.fieldName">
        <el-input v-model="rowForm[field.id || '']" type="textarea" :autosize="{ minRows: 2, maxRows: 5 }" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="rowVisible = false">取消</el-button>
      <el-button type="primary" @click="submitRow">保存</el-button>
    </template>
  </el-dialog>
</template>
