<script setup>
import { ref } from 'vue';
import { EditPen } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { resourceFieldApi } from '../api/resourceFields';
import { getErrorMessage } from '../utils/composableHelpers';
import OverflowTooltip from './OverflowTooltip.vue';

const props = defineProps({
    resourceType: {
        type: String,
        required: true
    },
    resourceId: {
        type: String,
        required: true
    },
    description: {
        type: String,
        default: ''
    }
});

const emit = defineEmits(['updated']);

const dialogVisible = ref(false);
const draftDescription = ref('');
const saving = ref(false);

function openDialog() {
    draftDescription.value = props.description || '';
    dialogVisible.value = true;
}

async function submitDescription() {
    saving.value = true;
    try {
        const result = await resourceFieldApi.updateFields(props.resourceType, props.resourceId, {
            description: draftDescription.value
        });
        const description = result?.fields?.description ?? draftDescription.value.trim();
        dialogVisible.value = false;
        ElMessage.success('描述已更新');
        emit('updated', description);
    } catch (error) {
        ElMessage.error(getErrorMessage(error, '更新描述失败'));
    } finally {
        saving.value = false;
    }
}
</script>

<template>
  <div class="resource-description-cell">
    <OverflowTooltip :content="description || '暂无描述'" class="resource-description-text" />
    <el-tooltip content="修改描述" placement="top" effect="light">
      <el-button
        class="resource-description-edit"
        :icon="EditPen"
        text
        circle
        title="修改描述"
        aria-label="修改描述"
        @click.stop="openDialog"
      />
    </el-tooltip>
  </div>

  <el-dialog
    v-model="dialogVisible"
    title="修改描述"
    class="description-edit-dialog fixed-dialog"
    style="--fixed-dialog-width: min(520px, 86vw); --fixed-dialog-height: min(360px, 70vh)"
    :close-on-click-modal="true"
  >
    <el-form label-position="top">
      <el-form-item label="描述">
        <el-input
          v-model="draftDescription"
          type="textarea"
          maxlength="200"
          show-word-limit
          :autosize="{ minRows: 5, maxRows: 8 }"
          placeholder="请输入描述"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="submitDescription">确认</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.resource-description-cell {
  display: flex;
  align-items: center;
  min-width: 0;
  gap: 14px;
}

.resource-description-text {
  flex: 1 1 auto;
  min-width: 0;
}

.resource-description-edit {
  flex: 0 0 auto;
}
</style>
