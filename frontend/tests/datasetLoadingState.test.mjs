import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const currentDir = dirname(fileURLToPath(import.meta.url));
const detailComposable = readFileSync(resolve(currentDir, '../src/modules/dataset/composables/useDatasetDetail.js'), 'utf8');
const detailTemplate = readFileSync(resolve(currentDir, '../src/views/DatasetDetailView.vue'), 'utf8');
const listComposable = readFileSync(resolve(currentDir, '../src/modules/dataset/composables/useDatasetList.js'), 'utf8');
const listTemplate = readFileSync(resolve(currentDir, '../src/views/DatasetManagementView.vue'), 'utf8');

for (const stateName of ['fieldSaving', 'rowSaving', 'excelImporting', 'excelCovering', 'publishing', 'versionOperatingIds', 'rowOperatingIds']) {
  assert.ok(detailComposable.includes(`${stateName}: ref(`), `dataset detail should expose ${stateName}`);
}

for (const helperCall of ['runExclusive(ctx.state.fieldSaving', 'runExclusive(ctx.state.rowSaving', 'runExclusive(ctx.state.publishing', 'runExclusiveById(ctx.state.versionOperatingIds', 'runExclusiveById(ctx.state.rowOperatingIds', 'runExclusive(loading']) {
  assert.ok(detailComposable.includes(helperCall), `dataset detail should guard ${helperCall}`);
}

assert.ok(detailComposable.includes('submitExcel(event, ctx.state.excelImporting'), 'Excel import should pass its loading state into submitExcel');
assert.ok(detailComposable.includes('submitExcel(event, ctx.state.excelCovering'), 'Excel cover should pass its loading state into submitExcel');

assert.ok(detailTemplate.includes(':loading="excelImporting || excelCovering"'), 'dataset import dropdown should show Excel loading');
assert.ok(detailTemplate.includes(':loading="fieldSaving"'), 'field save button should show saving');
assert.ok(detailTemplate.includes(':loading="rowSaving"'), 'row save button should show saving');
assert.ok(detailTemplate.includes(':loading="publishing"'), 'publish button should show loading');
assert.ok(detailTemplate.includes(':disabled="detailOperating"'), 'detail page should disable actions while busy');

function hiddenFileInput(refName) {
  const pattern = new RegExp(`<input\\s+[^>]*ref="${refName}"[^>]*>`, 'm');
  const match = detailTemplate.match(pattern);
  assert.ok(match, `should render hidden file input ${refName}`);
  return match[0];
}

for (const refName of ['excelInput', 'coverExcelInput']) {
  const input = hiddenFileInput(refName);
  assert.ok(input.includes('@change='), `${refName} should handle file selection changes`);
  assert.equal(input.includes('disabled'), false, `${refName} must stay programmatically clickable`);
}

for (const stateName of ['creating', 'deletingIds']) {
  assert.ok(listComposable.includes(`const ${stateName} = ref(`), `dataset list should expose ${stateName}`);
}

assert.ok(listComposable.includes('runExclusive(ctx.creating'), 'dataset creation should be guarded');
assert.ok(listComposable.includes('runExclusiveById(ctx.deletingIds'), 'dataset deletion should be guarded by row id');
assert.ok(listTemplate.includes(':loading="creating"'), 'create button should show loading');
assert.ok(listTemplate.includes(':loading="isDeletingDataset(row.id)"'), 'delete button should show per-row loading');
