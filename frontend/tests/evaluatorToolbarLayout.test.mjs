import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const currentDir = dirname(fileURLToPath(import.meta.url));
const styles = readFileSync(resolve(currentDir, '../src/styles.css'), 'utf8');

function cssRule(selector) {
  const pattern = new RegExp(`${selector.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}\\s*\\{([^}]*)\\}`, 'm');
  const match = styles.match(pattern);
  assert.ok(match, `missing CSS rule ${selector}`);
  return match[1];
}

const headRule = cssRule('.evaluator-management-head');
assert.ok(headRule.includes('flex-wrap: nowrap;'), 'evaluator header should keep the tabs and toolbar on one row');
assert.ok(headRule.includes('overflow-x: auto;'), 'evaluator header should scroll horizontally instead of squeezing controls when width is extremely tight');

const toolbarRule = cssRule('.panel-toolbar.evaluator-head-toolbar');
assert.ok(toolbarRule.includes('flex-wrap: nowrap;'), 'evaluator toolbar controls should stay on one row');

const searchRule = cssRule('.evaluator-head-toolbar .search-input');
assert.ok(searchRule.includes('min-width: 200px;'), 'evaluator search input should keep enough width for its placeholder');
assert.ok(searchRule.includes('flex: 0 0 clamp(200px, 22vw, 300px);'), 'evaluator search input should not shrink below its placeholder-safe width');
assert.ok(searchRule.includes('max-width: 300px;'), 'evaluator search input should not grow beyond the intended desktop width');
