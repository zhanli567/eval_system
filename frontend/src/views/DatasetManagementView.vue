<script setup>
import { Delete, Plus, Refresh, Search } from '@element-plus/icons-vue';
import { useDatasetList } from '../modules/dataset/composables/useDatasetList';
const { datasetLoading, datasets, datasetTotal, datasetPage, datasetSize, datasetKeyword, sortBy, sortOrder, createVisible, draggedFieldIndex, dragOverFieldIndex, createForm, loadDatasets, searchDatasets, changeDatasetSize, toggleSort, openDataset, openCreateDialog, submitCreate, removeDataset, addField, removeField, startFieldDrag, enterFieldDrag, dropField, endFieldDrag, formatTime } = useDatasetList();
</script>

<template>
  <section class="dataset-panel fill-workspace">
    <div class="panel-toolbar table-toolbar">
      <el-input
        v-model="datasetKeyword"
        clearable
        placeholder="请输入评测集名称"
        class="search-input"
        @keyup.enter="searchDatasets"
        @clear="searchDatasets"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
      <el-button class="search-icon-button" :icon="Search" title="搜索" aria-label="搜索" @click="searchDatasets" />
      <div class="table-toolbar-actions">
        <el-button class="toolbar-icon-button" :icon="Refresh" title="刷新" aria-label="刷新" @click="loadDatasets" />
        <el-button type="primary" :icon="Plus" @click="openCreateDialog">创建评测集</el-button>
      </div>
    </div>

    <el-table
      v-loading="datasetLoading"
      :data="datasets"
      row-key="id"
      border
      height="100%"
      highlight-current-row
      tooltip-effect="light"
      class="dataset-table"
    >
      <el-table-column prop="name" label="评测集名称" width="240" fixed="left" :resizable="false">
        <template #default="{ row }">
          <OverflowTooltip
            :content="row.name"
            class="linkish"
            role="button"
            tabindex="0"
            @click="openDataset(row)"
            @keyup.enter="openDataset(row)"
            @keyup.space.prevent="openDataset(row)"
          />
        </template>
      </el-table-column>
      <el-table-column prop="publishedVersionCount" label="版本数量" min-width="120" :resizable="false" align="center">
        <template #default="{ row }">
          <OverflowTooltip :content="row.publishedVersionCount" />
        </template>
      </el-table-column>
      <el-table-column prop="latestItemCount" label="数据量" min-width="110" :resizable="false" align="center">
        <template #default="{ row }">
          <OverflowTooltip :content="row.latestItemCount" />
        </template>
      </el-table-column>
      <el-table-column prop="description" label="描述" min-width="280" :resizable="false">
        <template #default="{ row }">
          <ResourceDescriptionCell
            resource-type="dataset"
            :resource-id="row.id"
            :description="row.description"
            @updated="() => loadDatasets()"
          />
        </template>
      </el-table-column>
      <el-table-column prop="createdByName" label="创建人" min-width="140" :resizable="false" align="center">
        <template #default="{ row }">
          <OverflowTooltip :content="row.createdByName || '-'" />
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
          <OverflowTooltip :content="formatTime(row.createdDate)" />
        </template>
      </el-table-column>
      <el-table-column prop="lastUpdatedByName" label="更新人" min-width="140" :resizable="false" align="center">
        <template #default="{ row }">
          <OverflowTooltip :content="row.lastUpdatedByName || '-'" />
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
          <OverflowTooltip :content="formatTime(row.lastUpdatedDate)" />
        </template>
      </el-table-column>
      <el-table-column column-key="actions" label="操作" width="140" fixed="right" :resizable="false" align="center">
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
        :page-sizes="[5, 10, 20]"
        layout="total, sizes, prev, pager, next, jumper"
        :total="datasetTotal"
        @size-change="changeDatasetSize"
        @current-change="loadDatasets"
      />
    </div>
  </section>

  <el-dialog v-model="createVisible" title="创建评测集" class="dataset-create-dialog fixed-dialog" style="--fixed-dialog-width: min(760px, 86vw); --fixed-dialog-height: min(640px, 86vh)" :close-on-click-modal="true">
    <el-form label-position="top">
      <el-form-item>
        <template #label>评测集名称 <span class="required-mark">*</span></template>
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
          <el-select v-model="field.fieldType" clearable placeholder="类型">
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
