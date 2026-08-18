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

const formRule = cssRule('.annotation-tag-form');
assert.ok(formRule.includes('display: flex;'), 'annotation tag form should use vertical flex layout');
assert.ok(formRule.includes('flex-direction: column;'), 'annotation tag form should stack tags vertically');
assert.ok(formRule.includes('overflow-y: auto;'), 'annotation tag form should scroll when tags overflow');
assert.ok(formRule.includes('overscroll-behavior: contain;'), 'annotation tag form should keep wheel scrolling inside the tag list');

const editorRule = cssRule('.annotation-tag-editor');
assert.ok(editorRule.includes('flex: 0 0 auto;'), 'each annotation tag editor should keep its content height and not shrink');
assert.ok(editorRule.includes('min-height: max-content;'), 'each annotation tag editor should reserve at least its content height');
