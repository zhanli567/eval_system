import assert from 'node:assert/strict';
import { buildResourceFieldPatch } from '../src/utils/resourceFields.js';

assert.deepEqual(
  buildResourceFieldPatch({ description: '  新描述  ' }),
  { fields: { description: '新描述' } }
);

assert.deepEqual(
  buildResourceFieldPatch({ description: null }),
  { fields: { description: '' } }
);

assert.throws(
  () => buildResourceFieldPatch({ name: '新名称' }),
  /暂不支持修改字段：name/
);

assert.throws(
  () => buildResourceFieldPatch({ description: 'a'.repeat(201) }),
  /描述不能超过200个字符/
);
