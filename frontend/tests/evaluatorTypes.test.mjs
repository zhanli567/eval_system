import assert from 'node:assert/strict';
import {
  canCreateEvaluatorType,
  defaultParamsForEvaluatorType,
  evaluatorTypeLabel,
  isJiuwenMetricType,
  requiresJudgeModel
} from '../src/utils/evaluatorTypes.js';

assert.equal(evaluatorTypeLabel('exact_match'), '精确匹配');
assert.equal(evaluatorTypeLabel('llm'), '模型裁判');
assert.equal(evaluatorTypeLabel('code'), '代码评估器');

assert.equal(isJiuwenMetricType('exact_match'), true);
assert.equal(isJiuwenMetricType('llm'), true);
assert.equal(isJiuwenMetricType('code'), false);

assert.equal(requiresJudgeModel('exact_match'), false);
assert.equal(requiresJudgeModel('llm'), true);

assert.equal(canCreateEvaluatorType('exact_match'), true);
assert.equal(canCreateEvaluatorType('llm'), true);
assert.equal(canCreateEvaluatorType('code'), false);

assert.deepEqual(
  defaultParamsForEvaluatorType('exact_match').map((param) => param.paramName),
  ['expected', 'actual']
);
