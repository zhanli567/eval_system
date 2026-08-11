import { reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { getErrorMessage } from '../utils/composableHelpers';

export function useResourceDescriptionEdit(options) {
    const dialogVisible = ref(false);
    const saving = ref(false);
    const form = reactive({ id: '', name: '', description: '' });

    function openDialog(row) {
        form.id = options.getId(row);
        form.name = options.getName(row);
        form.description = options.getDescription(row) || '';
        dialogVisible.value = true;
    }

    async function submitDescription(description) {
        const normalized = description == null ? '' : description.trim();
        if (normalized.length > 200) {
            ElMessage.warning('描述不能超过200个字符');
            return;
        }
        saving.value = true;
        try {
            await options.update(form.id, normalized);
            form.description = normalized;
            ElMessage.success('描述已更新');
            dialogVisible.value = false;
            if (options.reload) {
                await options.reload();
            } else {
                return;
            }
        } catch (error) {
            ElMessage.error(getErrorMessage(error, '描述更新失败'));
        } finally {
            saving.value = false;
        }
    }

    return {
        descriptionDialogVisible: dialogVisible,
        descriptionSaving: saving,
        descriptionForm: form,
        openDescriptionDialog: openDialog,
        submitDescription
    };
}
