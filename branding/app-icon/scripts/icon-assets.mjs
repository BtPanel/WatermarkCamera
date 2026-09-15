import { deflateSync } from 'node:zlib';
import { createHash } from 'node:crypto';
import { mkdir, writeFile } from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

export const APP_COLORS = Object.freeze({
  background: '#E43D36',
  mark: '#FFFFFF',
  recordPoint: '#FFC247',
});

export const PNG_SIZES = Object.freeze([16, 32, 48, 64, 72, 96, 144, 192, 512, 1024]);

const MARK_PATH = 'M18 12h64a6 6 0 0 1 6 6v52L64 88H18a6 6 0 0 1-6-6V18a6 6 0 0 1 6-6Zm8 14v48h33l15-11V26H26Zm24 9a15 15 0 1 1 0 30 15 15 0 0 1 0-30Zm0 10a5 5 0 1 0 0 10 5 5 0 0 0 0-10Z';

function svgDocument(content, viewBox = '0 0 100 100') {
  return [
    '<?xml version="1.0" encoding="UTF-8"?>',
    `<svg xmlns="http://www.w3.org/2000/svg" viewBox="${viewBox}" role="img" aria-label="Watermark Camera mark">`,
    content,
    '</svg>',
    '',
  ].join('\n');
}

export function buildSvg({ variant }) {
  if (variant === 'master') {
    return svgDocument(`<path fill="currentColor" fill-rule="evenodd" d="${MARK_PATH}"/>`);
  }
  if (variant === 'reversed') {
    return svgDocument(`<path fill="#FFFFFF" fill-rule="evenodd" d="${MARK_PATH}"/>`);
  }
  if (variant === 'app') {
    return svgDocument([
      `<rect width="100" height="100" rx="22" fill="${APP_COLORS.background}"/>`,
      `<path fill="${APP_COLORS.mark}" fill-rule="evenodd" d="${MARK_PATH}"/>`,
      `<circle cx="50" cy="50" r="5" fill="${APP_COLORS.recordPoint}"/>`,
    ].join('\n'));
  }
  throw new Error(`Unknown SVG variant: ${variant}`);
}

function parseHex(hex) {
  return [
    Number.parseInt(hex.slice(1, 3), 16),
    Number.parseInt(hex.slice(3, 5), 16),
    Number.parseInt(hex.slice(5, 7), 16),
    255,
  ];
}

function roundedRectContains(x, y, left, top, right, bottom, radius) {
  if (x < left || x > right || y < top || y > bottom) return false;
  const cx = Math.min(Math.max(x, left + radius), right - radius);
  const cy = Math.min(Math.max(y, top + radius), bottom - radius);
  return (x - cx) ** 2 + (y - cy) ** 2 <= radius ** 2;
}

function pointInPolygon(x, y, points) {
  let inside = false;
  for (let i = 0, j = points.length - 1; i < points.length; j = i++) {
    const [xi, yi] = points[i];
    const [xj, yj] = points[j];
    const crosses = (yi > y) !== (yj > y)
      && x < ((xj - xi) * (y - yi)) / (yj - yi) + xi;
    if (crosses) inside = !inside;
  }
  return inside;
}

function arcPoints(cx, cy, radius, startDegrees, endDegrees, steps = 6) {
  const points = [];
  for (let i = 1; i <= steps; i += 1) {
    const angle = (startDegrees + ((endDegrees - startDegrees) * i) / steps) * Math.PI / 180;
    points.push([cx + Math.cos(angle) * radius, cy + Math.sin(angle) * radius]);
  }
  return points;
}

const OUTER_MARK = [
  [18, 12], [82, 12],
  ...arcPoints(82, 18, 6, -90, 0),
  [88, 70], [64, 88], [18, 88],
  ...arcPoints(18, 82, 6, 90, 180),
  [12, 18],
  ...arcPoints(18, 18, 6, 180, 270),
];
const INNER_CUT = [[26, 26], [26, 74], [59, 74], [74, 63], [74, 26]];

function markContains(x, y) {
  const inFrame = pointInPolygon(x, y, OUTER_MARK) && !pointInPolygon(x, y, INNER_CUT);
  const distanceSquared = (x - 50) ** 2 + (y - 50) ** 2;
  const inRing = distanceSquared <= 15 ** 2 && distanceSquared >= 5 ** 2;
  return inFrame || inRing;
}

function rasterize(size) {
  const supersample = size <= 64 ? 4 : 2;
  const highSize = size * supersample;
  const high = new Uint8Array(highSize * highSize * 4);
  const red = parseHex(APP_COLORS.background);
  const white = parseHex(APP_COLORS.mark);
  const amber = parseHex(APP_COLORS.recordPoint);

  for (let py = 0; py < highSize; py += 1) {
    for (let px = 0; px < highSize; px += 1) {
      const x = ((px + 0.5) / highSize) * 100;
      const y = ((py + 0.5) / highSize) * 100;
      let color = [0, 0, 0, 0];
      if (roundedRectContains(x, y, 0, 0, 100, 100, 22)) color = red;
      if (markContains(x, y)) color = white;
      if ((x - 50) ** 2 + (y - 50) ** 2 <= 5 ** 2) color = amber;
      high.set(color, (py * highSize + px) * 4);
    }
  }

  const pixels = Buffer.alloc(size * size * 4);
  const samples = supersample ** 2;
  for (let y = 0; y < size; y += 1) {
    for (let x = 0; x < size; x += 1) {
      const totals = [0, 0, 0, 0];
      for (let sy = 0; sy < supersample; sy += 1) {
        for (let sx = 0; sx < supersample; sx += 1) {
          const source = (((y * supersample + sy) * highSize) + x * supersample + sx) * 4;
          for (let channel = 0; channel < 4; channel += 1) totals[channel] += high[source + channel];
        }
      }
      const target = (y * size + x) * 4;
      for (let channel = 0; channel < 4; channel += 1) {
        pixels[target + channel] = Math.round(totals[channel] / samples);
      }
    }
  }
  return pixels;
}

function crc32(buffer) {
  let crc = 0xffffffff;
  for (const byte of buffer) {
    crc ^= byte;
    for (let bit = 0; bit < 8; bit += 1) {
      crc = (crc >>> 1) ^ (0xedb88320 & -(crc & 1));
    }
  }
  return (crc ^ 0xffffffff) >>> 0;
}

function pngChunk(type, data) {
  const typeBuffer = Buffer.from(type, 'ascii');
  const chunk = Buffer.alloc(12 + data.length);
  chunk.writeUInt32BE(data.length, 0);
  typeBuffer.copy(chunk, 4);
  data.copy(chunk, 8);
  chunk.writeUInt32BE(crc32(Buffer.concat([typeBuffer, data])), 8 + data.length);
  return chunk;
}

export function renderPng(size) {
  if (!Number.isInteger(size) || size < 1) throw new Error('PNG size must be a positive integer');
  const pixels = rasterize(size);
  const stride = size * 4;
  const scanlines = Buffer.alloc((stride + 1) * size);
  for (let y = 0; y < size; y += 1) {
    const target = y * (stride + 1);
    scanlines[target] = 0;
    pixels.copy(scanlines, target + 1, y * stride, (y + 1) * stride);
  }
  const header = Buffer.alloc(13);
  header.writeUInt32BE(size, 0);
  header.writeUInt32BE(size, 4);
  header[8] = 8;
  header[9] = 6;
  return Buffer.concat([
    Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]),
    pngChunk('IHDR', header),
    pngChunk('IDAT', deflateSync(scanlines, { level: 9 })),
    pngChunk('IEND', Buffer.alloc(0)),
  ]);
}

export async function writeAssets(outputDir) {
  await mkdir(outputDir, { recursive: true });
  const assets = [
    { filename: 'logo-mark.svg', role: 'editable neutral master', format: 'svg', appearance: 'monochrome', background: 'transparent', content: buildSvg({ variant: 'master' }) },
    { filename: 'logo-mark-reversed.svg', role: 'reversed mark', format: 'svg', appearance: 'reversed', background: 'transparent', content: buildSvg({ variant: 'reversed' }) },
    { filename: 'app-icon.svg', role: 'full-color app icon', format: 'svg', appearance: 'signal-red', background: APP_COLORS.background, content: buildSvg({ variant: 'app' }) },
    ...PNG_SIZES.map((size) => ({
      filename: `app-icon-${size}.png`,
      role: size <= 64 ? 'small-size QA and raster export' : 'raster app icon',
      format: 'png',
      dimensions: { width: size, height: size },
      appearance: 'signal-red',
      background: 'transparent outside rounded preview container',
      content: renderPng(size),
    })),
  ];
  await Promise.all(assets.map(({ filename, content }) => writeFile(path.join(outputDir, filename), content)));

  const digest = (content) => createHash('sha256').update(content).digest('hex');
  const master = assets[0];
  const manifest = {
    manifest_version: '1.0',
    source_master: {
      filename: master.filename,
      variant: 'B2 coordinate-cut',
      sha256: digest(master.content),
    },
    source_status: 'provisional',
    decision_date: '2026-09-09',
    profile: 'neutral/android-unverified',
    exports: assets.map(({ content, ...asset }) => ({
      ...asset,
      sha256: digest(content),
      source_master: master.filename,
    })),
    color: {
      color_space: 'sRGB',
      background: APP_COLORS.background,
      mark: APP_COLORS.mark,
      record_point: APP_COLORS.recordPoint,
      monochrome: 'currentColor',
    },
    tool_context: 'Node.js geometry and PNG exporter; no external assets or fonts',
    exceptions: [{
      profile: 'Android',
      owner: 'product team',
      consequence: 'Exports are not claimed to satisfy a current Android adaptive-icon profile.',
      next_verification: 'Place the neutral mark into the project adaptive-icon template and verify current launcher masks.',
    }],
  };
  await writeFile(path.join(outputDir, 'manifest.json'), `${JSON.stringify(manifest, null, 2)}\n`);
}

if (process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  const defaultOutput = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..', 'exports');
  await writeAssets(process.argv[2] ? path.resolve(process.argv[2]) : defaultOutput);
}
