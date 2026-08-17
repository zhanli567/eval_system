import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const currentDir = dirname(fileURLToPath(import.meta.url));
const managementComposable = readFileSync(resolve(currentDir, '../src/modules/evaluator/composables/useEvaluatorManagement.js'), 'utf8');
const managementTemplate = readFileSync(resolve(currentDir, '../src/views/EvaluatorManagementView.vue'), 'utf8');
const editorComposable = readFileSync(resolve(currentDir, '../src/modules/evaluator/composables/useEvaluatorEditor.js'), 'utf8');
const editorTemplate = readFileSync(resolve(currentDir, '../src/views/EvaluatorEditorView.vue'), 'utf8');

assert.ok(managementComposable.includes('runExclusiveById(ctx.openingPresetIds'), 'preset detail loading should be guarded by preset id');
assert.ok(managementComposable.includes('runExclusiveById(ctx.deletingEvaluatorIds'), 'evaluator deletion should be guarded by evaluator id');
assert.ok(managementComposable.includes('function isOpeningPreset(presetId)'), 'management should expose preset row opening state');
assert.ok(managementComposable.includes('function isDeletingEvaluator(evaluatorId)'), 'management should expose evaluator row deletion state');
assert.ok(managementTemplate.includes(':loading="isOpeningPreset(preset.id)"'), 'preset detail button should show loading');
assert.ok(managementTemplate.includes(':loading="isDeletingEvaluator(row.id)"'), 'custom evaluator delete button should show loading');

for (const stateName of ['deletingVersionIds', 'usingPresetIds']) {
  assert.ok(editorComposable.includes(`const ${stateName} = ref([])`), `editor should track ${stateName}`);
}

for (const helperCall of ['runExclusive(ctx.saving', 'runExclusive(ctx.publishing', 'runExclusive(ctx.trialLoading', 'runExclusiveById(ctx.deletingVersionIds', 'runExclusiveById(ctx.usingPresetIds']) {
  assert.ok(editorComposable.includes(helperCall), `editor should guard ${helperCall}`);
}

assert.ok(editorComposable.includes('function isDeletingVersion(versionId)'), 'editor should expose deleting version state');
assert.ok(editorComposable.includes('function isUsingPreset(presetId)'), 'editor should expose preset apply state');
assert.ok(editorTemplate.includes(':loading="isDeletingVersion(activeVersion.id)"'), 'delete version button should show loading');
assert.ok(editorTemplate.includes(':loading="isUsingPreset(preset.id)"'), 'use preset button should show loading');
assert.ok(editorTemplate.includes(':disabled="saving || publishing'), 'save and publish buttons should be disabled while operating');
assert.ok(editorTemplate.includes(':disabled="trialLoading"'), 'trial run should be disabled while running');
