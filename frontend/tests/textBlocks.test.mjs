import assert from 'node:assert/strict';
import { normalizePromptForEditor } from '../src/utils/textBlocks.js';

const rawPrompt = `
    第一段评估说明
      - 子项也不应该保留文本块缩进

    <查询>
    \${query}
    </查询>
`;

assert.equal(
  normalizePromptForEditor(rawPrompt),
  '第一段评估说明\n- 子项也不应该保留文本块缩进\n\n<查询>\n${query}\n</查询>'
);
