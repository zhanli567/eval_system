import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { resolveSpaceSelection } from '../src/utils/spaceSelection.js';

const spaces = [
  { id: 'space-1', name: 'Space One', status: 'ACTIVE' },
  { id: 'space-2', name: 'Space Two', status: 'ACTIVE' }
];

assert.equal(resolveSpaceSelection(spaces, 'space-2'), 'space-2');
assert.equal(resolveSpaceSelection(spaces, 'missing'), 'space-1');
assert.equal(resolveSpaceSelection([], 'space-2'), '');
assert.match(readFileSync('src/api/http.js', 'utf8'), /withCredentials:\s*true/);

console.log('space selection checks passed');
