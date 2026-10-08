// Generate per-attribute rune overlays with Node's standard library only.
// Run: node tools/gen_magic_runes.mjs
import crypto from "node:crypto";
import fs from "node:fs";
import path from "node:path";
import zlib from "node:zlib";

const root = path.resolve(import.meta.dirname, "..");
const enumPath = path.join(root, "src/main/java/com/newuniverse/nusmp/blackclover/MagicType.java");
const outDir = path.join(root, "src/main/resources/assets/nusmp/textures/particle/magic_runes");
const size = 64, scale = 4, hi = size * scale;
const colors = {
  FLAME: [255, 116, 42], WATER: [104, 205, 255], WIND: [156, 255, 199], EARTH: [183, 227, 115],
  LIGHT: [255, 230, 116], DARKNESS: [203, 127, 255], SPACE: [135, 154, 255], TIME: [97, 239, 218],
  BATTLE: [255, 151, 103], FANTASY: [255, 158, 222], EMPTY: [255, 82, 116],
};

function crc32(bytes) {
  let crc = 0xffffffff;
  for (const byte of bytes) {
    crc ^= byte;
    for (let bit = 0; bit < 8; bit++) crc = (crc >>> 1) ^ (0xedb88320 & -(crc & 1));
  }
  return (crc ^ 0xffffffff) >>> 0;
}

function chunk(type, data) {
  const name = Buffer.from(type, "ascii");
  const body = Buffer.concat([name, data]);
  const head = Buffer.alloc(4), tail = Buffer.alloc(4);
  head.writeUInt32BE(data.length);
  tail.writeUInt32BE(crc32(body));
  return Buffer.concat([head, body, tail]);
}

function line(pixels, x0, y0, x1, y1, color, alpha = 255) {
  let dx = Math.abs(x1 - x0), sx = x0 < x1 ? 1 : -1;
  let dy = -Math.abs(y1 - y0), sy = y0 < y1 ? 1 : -1, err = dx + dy;
  while (true) {
    for (let oy = -1; oy <= 1; oy++) for (let ox = -1; ox <= 1; ox++) {
      const x = x0 + ox, y = y0 + oy;
      if (x < 0 || y < 0 || x >= hi || y >= hi) continue;
      const i = (y * hi + x) * 4;
      pixels[i] = color[0]; pixels[i + 1] = color[1]; pixels[i + 2] = color[2];
      pixels[i + 3] = Math.max(pixels[i + 3], alpha);
    }
    if (x0 === x1 && y0 === y1) break;
    const e2 = 2 * err;
    if (e2 >= dy) { err += dy; x0 += sx; }
    if (e2 <= dx) { err += dx; y0 += sy; }
  }
}

function makePng(name, soul) {
  const seed = crypto.createHash("sha256").update(name).digest();
  const color = colors[soul], pixels = new Uint8Array(hi * hi * 4);
  const cx = hi / 2, cy = hi / 2, r = 25 * scale;
  const polygon = [];
  const corners = 3 + seed[0] % 6, phase = seed.readUInt16BE(1) / 65535 * Math.PI * 2;
  for (let j = 0; j < corners; j++) {
    const a = phase + Math.PI * 2 * j / corners;
    const radius = r * (j % 2 ? 0.43 : 0.73);
    polygon.push([Math.round(cx + Math.cos(a) * radius), Math.round(cy + Math.sin(a) * radius)]);
  }
  for (let j = 0; j < polygon.length; j++) line(pixels, ...polygon[j], ...polygon[(j + 1) % polygon.length], color, 240);
  for (let j = 0; j < 18; j++) {
    const a = phase + Math.PI * 2 * j / 18;
    const r0 = r * 0.85, r1 = r * (seed[j + 5] & 1 ? 0.97 : 0.92);
    line(pixels, Math.round(cx + Math.cos(a) * r0), Math.round(cy + Math.sin(a) * r0),
      Math.round(cx + Math.cos(a) * r1), Math.round(cy + Math.sin(a) * r1), color, 220);
    if (j % 3 === seed[4] % 3) {
      const a2 = a + 0.08;
      line(pixels, Math.round(cx + Math.cos(a) * r1), Math.round(cy + Math.sin(a) * r1),
        Math.round(cx + Math.cos(a2) * r * 0.78), Math.round(cy + Math.sin(a2) * r * 0.78), color, 190);
    }
  }
  // The high-resolution line art is reduced with area averaging for crisp transparent edges.
  const rgba = Buffer.alloc(size * size * 4);
  for (let y = 0; y < size; y++) for (let x = 0; x < size; x++) {
    let alpha = 0, red = 0, green = 0, blue = 0;
    for (let oy = 0; oy < scale; oy++) for (let ox = 0; ox < scale; ox++) {
      const i = ((y * scale + oy) * hi + x * scale + ox) * 4, a = pixels[i + 3];
      alpha += a; red += pixels[i] * a; green += pixels[i + 1] * a; blue += pixels[i + 2] * a;
    }
    const o = (y * size + x) * 4, n = scale * scale;
    rgba[o] = alpha ? Math.round(red / alpha) : 255;
    rgba[o + 1] = alpha ? Math.round(green / alpha) : 255;
    rgba[o + 2] = alpha ? Math.round(blue / alpha) : 255;
    rgba[o + 3] = Math.round(alpha / n);
  }
  const scanlines = Buffer.alloc((size * 4 + 1) * size);
  for (let y = 0; y < size; y++) rgba.copy(scanlines, y * (size * 4 + 1) + 1, y * size * 4, (y + 1) * size * 4);
  const header = Buffer.alloc(13);
  header.writeUInt32BE(size, 0); header.writeUInt32BE(size, 4);
  header[8] = 8; header[9] = 6;
  return Buffer.concat([
    Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]),
    chunk("IHDR", header), chunk("IDAT", zlib.deflateSync(scanlines)), chunk("IEND", Buffer.alloc(0)),
  ]);
}

const source = fs.readFileSync(enumPath, "utf8");
const matches = [...source.matchAll(/^\s*([A-Z][A-Z0-9_]*)\("[^"]*",\s*"[^"]*",\s*"([A-Z]+)"/gm)];
fs.mkdirSync(outDir, { recursive: true });
for (const [, name, soul] of matches) {
  fs.writeFileSync(path.join(outDir, `${name.toLowerCase()}.png`), makePng(name, soul));
}
console.log(`wrote ${matches.length} magic rune textures to ${path.relative(root, outDir)}`);
