import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const currentDir = dirname(fileURLToPath(import.meta.url));
const managementComposable = readFileSync(resolve(currentDir, '../src/modules/task/composables/useTaskManagement.js'), 'utf8');
const managementTemplate = readFileSync(resolve(currentDir, '../src/views/TaskManagementView.vue'), 'utf8');
const createComposable = readFileSync(resolve(currentDir, '../src/modules/task/composables/useTaskCreate.js'), 'utf8');
const createTemplate = readFileSync(resolve(currentDir, '../src/views/TaskCreateView.vue'), 'utf8');
const detailComposable = readFileSync(resolve(currentDir, '../src/modules/task/composables/useTaskDetail.js'), 'utf8');
const detailTemplate = readFileSync(resolve(currentDir, '../src/views/TaskDetailView.vue'), 'utf8');
const annotationComposable = readFileSync(resolve(currentDir, '../src/modules/task/composables/useTaskAnnotation.js'), 'utf8');
const annotationTemplate = readFileSync(resolve(currentDir, '../src/views/TaskAnnotationView.vue'), 'utf8');
const tagCreateDialog = readFileSync(resolve(currentDir, '../src/components/TagCreateDialog.vue'), 'utf8');
const resourceDescriptionDialog = readFileSync(resolve(currentDir, '../src/components/ResourceDescriptionDialog.vue'), 'utf8');
const resourceDescriptionComposable = readFileSync(resolve(currentDir, '../src/composables/useResourceDescriptionEdit.js'), 'utf8');

assert.ok(managementComposable.includes('runExclusiveById(ctx.deletingTaskIds'), 'task deletion should be guarded by task id');
assert.ok(managementComposable.includes('function isDeletingTask(taskId)'), 'task list should expose deleting state');
assert.ok(managementComposable.includes('if (isStartingTask(row.base.id))'), 'task start should ignore duplicate clicks');
assert.ok(managementComposable.includes('if (isStoppingTask(row.base.id))'), 'task stop should ignore duplicate clicks');
assert.ok(managementTemplate.includes(':loading="isDeletingTask(row.base.id)"'), 'task delete button should show loading');

assert.ok(createComposable.includes('runExclusive(ctx.state.saving'), 'task creation should be guarded by saving state');
assert.ok(createTemplate.includes(':disabled="saving || !canSubmit"'), 'task create button should be disabled while saving or invalid');
assert.ok(createTemplate.includes(':disabled="saving"'), 'task create cancel should be disabled while saving');

assert.ok(detailComposable.includes('runExclusive(ctx.stopping'), 'task detail stop should be guarded by stopping state');
assert.ok(detailTemplate.includes(':loading="stopping"'), 'task detail stop button should show loading');
assert.ok(detailTemplate.includes(':disabled="stopping"'), 'task detail stop button should be disabled while stopping');

assert.ok(annotationComposable.includes('runExclusive(ctx.saving'), 'task annotation save should be guarded by saving state');
assert.ok(annotationComposable.includes('targetItemId && !ctx.saving.value'), 'task annotation navigation should be blocked while saving');
assert.ok(annotationTemplate.includes(':disabled="saving || !!loadError || !item"'), 'task annotation save button should be disabled while saving');
assert.ok(annotationTemplate.includes(':disabled="!previousItemId || saving"'), 'previous annotation button should be disabled while saving');
assert.ok(annotationTemplate.includes(':disabled="!nextItemId || saving"'), 'next annotation button should be disabled while saving');

assert.ok(tagCreateDialog.includes('runExclusive(saving'), 'task tag creation dialog should guard submit');
assert.ok(tagCreateDialog.includes(':close-on-click-modal="!saving"'), 'task tag creation dialog should not close by mask while saving');
assert.ok(tagCreateDialog.includes(':disabled="saving"'), 'task tag creation cancel should be disabled while saving');

assert.ok(resourceDescriptionComposable.includes('runExclusive(saving'), 'resource description save should be guarded');
assert.ok(resourceDescriptionDialog.includes('if (props.saving)'), 'resource description dialog should ignore duplicate submits while saving');
assert.ok(resourceDescriptionDialog.includes(':close-on-click-modal="!saving"'), 'resource description dialog should not close by mask while saving');
assert.ok(resourceDescriptionDialog.includes(':disabled="saving"'), 'resource description cancel should be disabled while saving');
