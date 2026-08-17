import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const currentDir = dirname(fileURLToPath(import.meta.url));
const composable = readFileSync(resolve(currentDir, '../src/modules/tag/composables/useTagManagement.js'), 'utf8');
const template = readFileSync(resolve(currentDir, '../src/views/TagManagementView.vue'), 'utf8');

assert.ok(composable.includes('runExclusive(ctx.saving'), 'tag create/edit submit should be guarded');
assert.ok(composable.includes('runExclusiveById(ctx.openingIds'), 'tag detail/edit loading should be guarded by row id');
assert.ok(composable.includes('runExclusiveById(ctx.deletingIds'), 'tag deletion should be guarded by row id');
assert.ok(composable.includes('const openingIds = ref([])'), 'tag list should track opening row ids');
assert.ok(composable.includes('const deletingIds = ref([])'), 'tag list should track deleting row ids');
assert.ok(composable.includes('function isOpeningTag(tagId)'), 'tag list should expose row opening state');
assert.ok(composable.includes('function isDeletingTag(tagId)'), 'tag list should expose row deleting state');

assert.ok(template.includes(':loading="isOpeningTag(row.id)"'), 'detail/edit buttons should show row loading');
assert.ok(template.includes(':loading="isDeletingTag(row.id)"'), 'delete button should show row loading');
assert.ok(template.includes(':close-on-click-modal="!saving"'), 'tag dialog should not close by mask while saving');
assert.ok(template.includes(':disabled="saving"'), 'tag dialog cancel should be disabled while saving');
assert.ok(template.includes(':loading="saving"'), 'tag dialog submit should show saving');
