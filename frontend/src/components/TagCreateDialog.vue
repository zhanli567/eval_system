<script setup>
import { computed, reactive, ref, watch } from 'vue';
import { Delete, Plus } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { tagApi } from '../api/tag';
import { NUMBER_VALUE_MAX, NUMBER_VALUE_MIN, NUMBER_VALUE_RANGE_TEXT, hasNumberValueOutOfRange, isNumberValueMissing } from '../utils/numberRange';

const tagTypeOptions = [
    { label: '分类', value: 'category' },
    { label: '布尔值', value: 'boolean' },
    { label: '数字', value: 'number' },
    { label: '文本', value: 'text' }
];
const booleanOptions = [
    { optionName: 'True', optionGroup: 'pass' },
    { optionName: 'False', optionGroup: 'fail' }
];

const props = defineProps({
    modelValue: { type: Boolean, default: false }
});
const emit = defineEmits(['update:modelValue', 'created']);
const saving = ref(false);
const form = reactive(createDefaultForm());
const visible = computed({
    get: () => props.modelValue,
    set: (value) => emit('update:modelValue', value)
});

watch(visible, (value) => {
    if (value) {
        resetForm();
    }
});

function createDefaultForm() {
    return {
        tagName: '',
        description: '',
        tagType: 'category',
        minValue: undefined,
        maxValue: undefined,
        passThreshold: undefined,
        passOptions: [''],
        failOptions: ['']
    };
}

function resetForm() {
    Object.assign(form, createDefaultForm());
}

function cleanOptions(options) {
    return options.map((item) => item.trim()).filter(Boolean);
}

function validateForm() {
    if (!form.tagName.trim()) {
        return '请输入标签名称';
    } else if (form.description.trim().length > 200) {
        return '描述不能超过200个字符';
    } else if (form.tagType === 'category') {
        return validateCategoryOptions();
    } else if (form.tagType === 'number') {
        return validateNumberConfig();
    } else {
        return '';
    }
}

function validateCategoryOptions() {
    if (!cleanOptions(form.passOptions).length || !cleanOptions(form.failOptions).length) {
        return '分类标签请至少配置一个Pass选项和一个Fail选项';
    } else {
        return '';
    }
}

function validateNumberConfig() {
    const values = [form.minValue, form.maxValue, form.passThreshold];
    if (values.some((value) => isNumberValueMissing(value))) {
        return '请维护评分范围和通过阈值';
    } else if (hasNumberValueOutOfRange(values)) {
        return `评分范围和通过阈值必须在${NUMBER_VALUE_RANGE_TEXT}之间`;
    } else if (form.minValue >= form.maxValue) {
        return '评分最大值必须大于最小值';
    } else if (form.passThreshold < form.minValue || form.passThreshold > form.maxValue) {
        return '通过阈值必须介于评分范围内';
    } else {
        return '';
    }
}

function buildPayload() {
    return {
        tagName: form.tagName.trim(),
        description: form.description.trim(),
        tagType: form.tagType,
        minValue: form.tagType === 'number' ? form.minValue : undefined,
        maxValue: form.tagType === 'number' ? form.maxValue : undefined,
        passThreshold: form.tagType === 'number' ? form.passThreshold : undefined,
        options: form.tagType === 'category' ? buildCategoryOptions() : []
    };
}

function buildCategoryOptions() {
    return [
        ...cleanOptions(form.passOptions).map((optionName) => ({ optionName, optionGroup: 'pass' })),
        ...cleanOptions(form.failOptions).map((optionName) => ({ optionName, optionGroup: 'fail' }))
    ];
}

function addCategoryOption(group) {
    const target = group === 'pass' ? form.passOptions : form.failOptions;
    if (target.length >= 5) {
        ElMessage.warning('Pass和Fail选项每组最多支持5个');
        return;
    }
    target.push('');
}

function removeCategoryOption(group, index) {
    const target = group === 'pass' ? form.passOptions : form.failOptions;
    if (target.length === 1) {
        target[0] = '';
    } else {
        target.splice(index, 1);
    }
}

async function submitTag() {
    const errorMessage = validateForm();
    if (errorMessage) {
        ElMessage.error(errorMessage);
        return;
    }
    saving.value = true;
    try {
        const created = await tagApi.createTag(buildPayload());
        ElMessage.success('标签已创建');
        visible.value = false;
        emit('created', created);
    } finally {
        saving.value = false;
    }
}
</script>

<template>
  <el-dialog
    v-model="visible"
    title="创建标签"
    class="tag-dialog task-tag-create-dialog fixed-dialog"
    style="--fixed-dialog-width: min(680px, 86vw); --fixed-dialog-height: min(640px, 86vh)"
    :close-on-click-modal="true"
  >
    <el-form label-position="top" class="tag-form">
      <el-form-item>
        <template #label>标签名称 <span class="required-mark">*</span></template>
        <el-input v-model="form.tagName" maxlength="20" show-word-limit placeholder="请输入标签名称" />
      </el-form-item>
      <el-form-item label="描述">
        <el-input
          v-model="form.description"
          type="textarea"
          maxlength="200"
          show-word-limit
          :autosize="{ minRows: 4, maxRows: 6 }"
          placeholder="请输入描述"
        />
      </el-form-item>
      <el-form-item>
        <template #label>类型 <span class="required-mark">*</span></template>
        <el-select v-model="form.tagType" clearable class="wide-control">
          <el-option v-for="item in tagTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>

      <div v-if="form.tagType === 'category'" class="tag-config-grid">
        <section class="option-group-card pass">
          <div class="option-group-head">
            <strong>Pass <span class="required-mark">*</span></strong>
            <el-button link type="primary" :icon="Plus" @click="addCategoryOption('pass')">添加标签</el-button>
          </div>
          <div class="option-list">
            <div v-for="(option, index) in form.passOptions" :key="`pass-${index}`" class="option-editor">
              <el-input
                :model-value="option"
                maxlength="20"
                show-word-limit
                placeholder="请输入标签"
                @update:model-value="form.passOptions[index] = $event"
              />
              <el-button :icon="Delete" circle @click="removeCategoryOption('pass', index)" />
            </div>
          </div>
        </section>
        <section class="option-group-card fail">
          <div class="option-group-head">
            <strong>Fail <span class="required-mark">*</span></strong>
            <el-button link type="primary" :icon="Plus" @click="addCategoryOption('fail')">添加标签</el-button>
          </div>
          <div class="option-list">
            <div v-for="(option, index) in form.failOptions" :key="`fail-${index}`" class="option-editor">
              <el-input
                :model-value="option"
                maxlength="20"
                show-word-limit
                placeholder="请输入标签"
                @update:model-value="form.failOptions[index] = $event"
              />
              <el-button :icon="Delete" circle @click="removeCategoryOption('fail', index)" />
            </div>
          </div>
        </section>
      </div>

      <div v-else-if="form.tagType === 'boolean'" class="boolean-config">
        <div v-for="option in booleanOptions" :key="option.optionName" class="boolean-row">
          <span>{{ option.optionName }}</span>
          <el-tag :type="option.optionGroup === 'pass' ? 'success' : 'danger'" effect="plain">
            {{ option.optionGroup === 'pass' ? 'Pass' : 'Fail' }}
          </el-tag>
        </div>
      </div>

      <div v-else-if="form.tagType === 'number'" class="number-config">
        <el-form-item>
          <template #label>评分范围 <span class="required-mark">*</span></template>
          <div class="range-row">
            <el-input-number v-model="form.minValue" :min="NUMBER_VALUE_MIN" :max="NUMBER_VALUE_MAX" :precision="0" controls-position="right" placeholder="最小值" />
            <span>-</span>
            <el-input-number v-model="form.maxValue" :min="NUMBER_VALUE_MIN" :max="NUMBER_VALUE_MAX" :precision="0" controls-position="right" placeholder="最大值" />
          </div>
        </el-form-item>
        <el-form-item>
          <template #label>
            通过阈值 <span class="required-mark">*</span>
            <span class="label-tip">大于等于该阈值为Pass</span>
          </template>
          <el-input-number
            v-model="form.passThreshold"
            :min="NUMBER_VALUE_MIN"
            :max="NUMBER_VALUE_MAX"
            :precision="0"
            controls-position="right"
            class="wide-control"
            placeholder="请输入阈值"
          />
        </el-form-item>
      </div>

      <div v-else class="text-config">
        <span class="meta">文本标签无需额外配置，评测人将在评测任务中填写文字评价。</span>
      </div>
    </el-form>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="submitTag">确定</el-button>
    </template>
  </el-dialog>
</template>
