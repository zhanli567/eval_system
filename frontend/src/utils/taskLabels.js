import { labelFromMap } from './composableHelpers';

export const TASK_STATUS_OPTIONS = [
    { label: '全部状态', value: '' },
    { label: '待执行', value: 'pending' },
    { label: '进行中', value: 'running' },
    { label: '评测完成', value: 'completed' },
    { label: '评测失败', value: 'failed' }
];

const TASK_STATUS_LABELS = {
    ...Object.fromEntries(TASK_STATUS_OPTIONS.map((item) => [item.value, item.label])),
    annotation_pending: '待标注',
    annotating: '标注中',
    skipped: '跳过'
};
const TAG_TYPE_LABELS = {
    category: '分类',
    boolean: '布尔',
    number: '数字',
    text: '文本'
};

export function statusLabel(value) {
    return labelFromMap(TASK_STATUS_LABELS, value);
}

export function passTagType(value) {
    return value === 'pass' ? 'success' : value === 'fail' ? 'danger' : 'info';
}

export function tagTypeLabel(value) {
    return labelFromMap(TAG_TYPE_LABELS, value);
}
