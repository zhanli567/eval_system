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
const httpSource = readFileSync('src/api/http.js', 'utf8');
const integrationSource = readFileSync('src/api/integration.js', 'utf8');
assert.match(httpSource, /Aurora\.service\.network\.get/);
assert.match(httpSource, /'x-space-id'/);
assert.doesNotMatch(httpSource, /const\s+\w+\s*=\s*Aurora\.service\.network/);
assert.doesNotMatch(httpSource, /startsWith/);
assert.match(integrationSource, /\/integration\/spaces/);

console.log('space selection checks passed');
