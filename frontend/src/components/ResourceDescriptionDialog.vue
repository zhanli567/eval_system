<script setup>
import { computed, ref, watch } from 'vue';

const props = defineProps({
    modelValue: { type: Boolean, default: false },
    title: { type: String, default: '编辑描述' },
    nameLabel: { type: String, default: '名称' },
    name: { type: String, default: '' },
    description: { type: String, default: '' },
    saving: { type: Boolean, default: false }
});

const emit = defineEmits(['update:modelValue', 'save']);
const localDescription = ref('');
const visible = computed({
    get: () => props.modelValue,
    set: (value) => emit('update:modelValue', value)
});

watch(
    () => props.modelValue,
    (value) => {
        if (value) {
            localDescription.value = props.description || '';
        }
    }
);

watch(
    () => props.description,
    (value) => {
        if (props.modelValue) {
            localDescription.value = value || '';
        }
    }
);

function submit() {
    if (props.saving) {
        return;
    }
    emit('save', localDescription.value);
}
</script>

<template>
  <el-dialog
    v-model="visible"
    :title="title"
    class="resource-description-dialog fixed-dialog"
    style="--fixed-dialog-width: min(460px, 86vw); --fixed-dialog-height: min(430px, 86vh)"
    :close-on-click-modal="!saving"
  >
    <el-form label-position="top" class="resource-description-form">
      <el-form-item :label="nameLabel">
        <el-input :model-value="name" disabled />
      </el-form-item>
      <el-form-item label="描述">
        <el-input
          v-model="localDescription"
          type="textarea"
          maxlength="200"
          show-word-limit
          placeholder="请输入描述"
          :autosize="{ minRows: 5, maxRows: 7 }"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button :disabled="saving" @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="submit">确定</el-button>
    </template>
  </el-dialog>
</template>
