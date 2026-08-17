import assert from 'node:assert/strict';
import { runExclusive, runExclusiveById } from '../src/utils/composableHelpers.js';

function ref(value) {
  return { value };
}

{
  const busy = ref(false);
  let started = 0;
  let release;

  const first = runExclusive(busy, async () => {
    started += 1;
    await new Promise((resolve) => {
      release = resolve;
    });
    return 'first';
  });
  const second = runExclusive(busy, async () => {
    started += 1;
    return 'second';
  });

  assert.equal(started, 1);
  assert.equal(busy.value, true);
  assert.equal(await second, undefined);
  release();
  assert.equal(await first, 'first');
  assert.equal(busy.value, false);
}

{
  const busyIds = ref([]);
  let started = 0;
  let release;

  const first = runExclusiveById(busyIds, 'row-1', async () => {
    started += 1;
    await new Promise((resolve) => {
      release = resolve;
    });
    return 'deleted';
  });
  const duplicate = runExclusiveById(busyIds, 'row-1', async () => {
    started += 1;
    return 'duplicate';
  });
  const another = runExclusiveById(busyIds, 'row-2', async () => {
    started += 1;
    return 'another';
  });

  assert.deepEqual(busyIds.value, ['row-1', 'row-2']);
  assert.equal(started, 2);
  assert.equal(await duplicate, undefined);
  assert.equal(await another, 'another');
  assert.deepEqual(busyIds.value, ['row-1']);
  release();
  assert.equal(await first, 'deleted');
  assert.deepEqual(busyIds.value, []);
}
