export const EVALUATOR_TYPE_EXACT_MATCH = 'exact_match';
export const EVALUATOR_TYPE_LLM = 'llm';
export const EVALUATOR_TYPE_CODE = 'code';

const TYPE_LABELS = {
    [EVALUATOR_TYPE_EXACT_MATCH]: '精确匹配',
    [EVALUATOR_TYPE_LLM]: '模型裁判',
    [EVALUATOR_TYPE_CODE]: '代码评估器'
};

export function evaluatorTypeLabel(type) {
    return TYPE_LABELS[type] || type || '-';
}

export function isJiuwenMetricType(type) {
    return type === EVALUATOR_TYPE_EXACT_MATCH || type === EVALUATOR_TYPE_LLM;
}

export function requiresJudgeModel(type) {
    return type === EVALUATOR_TYPE_LLM;
}

export function canCreateEvaluatorType(type) {
    return isJiuwenMetricType(type);
}

export function defaultParamsForEvaluatorType(type) {
    if (type === EVALUATOR_TYPE_EXACT_MATCH) {
        return [
            { paramName: 'expected', dataType: 'string', defaultValue: '', required: true, description: '期望结果' },
            { paramName: 'actual', dataType: 'string', defaultValue: '', required: true, description: '实际结果' }
        ];
    }
    return [
        { paramName: 'expected', dataType: 'string', defaultValue: '', required: true, description: '期望结果' },
        { paramName: 'actual', dataType: 'string', defaultValue: '', required: true, description: '实际结果' }
    ];
}
