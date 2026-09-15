import assert from 'node:assert/strict';
import { createHash } from 'node:crypto';
import { mkdtemp, readFile, rm } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import path from 'node:path';
import test from 'node:test';

import {
  APP_COLORS,
  PNG_SIZES,
  buildSvg,
  renderPng,
  writeAssets,
} from '../scripts/icon-assets.mjs';

function pngDimensions(buffer) {
  assert.equal(buffer.toString('ascii', 1, 4), 'PNG');
  return {
    width: buffer.readUInt32BE(16),
    height: buffer.readUInt32BE(20),
  };
}

test('builds a neutral editable SVG without unsafe or external content', () => {
  const svg = buildSvg({ variant: 'master' });

  assert.match(svg, /viewBox="0 0 100 100"/);
  assert.match(svg, /fill-rule="evenodd"/);
  assert.doesNotMatch(svg, /<script|<image|<text|href=/i);
  assert.doesNotMatch(svg, /#[0-9a-f]{3,8}/i);
});

test('builds the approved signal-red app icon colors', () => {
  const svg = buildSvg({ variant: 'app' });

  assert.match(svg, new RegExp(APP_COLORS.background, 'i'));
  assert.match(svg, new RegExp(APP_COLORS.mark, 'i'));
  assert.match(svg, new RegExp(APP_COLORS.recordPoint, 'i'));
});

test('renders valid square PNGs at every declared size', () => {
  for (const size of PNG_SIZES) {
    const png = renderPng(size);
    assert.deepEqual(pngDimensions(png), { width: size, height: size });
    assert.ok(png.length > 100, `${size}px export should not be blank`);
  }
});

test('writes the complete delivery inventory', async () => {
  const outputDir = await mkdtemp(path.join(tmpdir(), 'watermark-icon-'));
  try {
    await writeAssets(outputDir);
    const expected = [
      'logo-mark.svg',
      'logo-mark-reversed.svg',
      'app-icon.svg',
      ...PNG_SIZES.map((size) => `app-icon-${size}.png`),
      'manifest.json',
    ];
    for (const filename of expected) {
      const content = await readFile(path.join(outputDir, filename));
      assert.ok(content.length > 0, `${filename} should contain data`);
    }

    const manifest = JSON.parse(await readFile(path.join(outputDir, 'manifest.json'), 'utf8'));
    assert.equal(manifest.source_status, 'provisional');
    assert.equal(manifest.profile, 'neutral/android-unverified');
    for (const exported of manifest.exports) {
      const content = await readFile(path.join(outputDir, exported.filename));
      const digest = createHash('sha256').update(content).digest('hex');
      assert.equal(exported.sha256, digest, `${exported.filename} digest should match`);
    }
  } finally {
    await rm(outputDir, { recursive: true, force: true });
  }
});
