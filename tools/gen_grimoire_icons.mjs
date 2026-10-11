import { deflateSync } from "node:zlib";
import { existsSync, mkdirSync, writeFileSync } from "node:fs";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const root = resolve(dirname(fileURLToPath(import.meta.url)), "..");
const output = join(root, "src", "main", "resources", "assets", "nusmp", "textures", "skill", "grimoire");
const entries = [
  ["air", "#9fe8ff", "wind"], ["hair", "#ff6e9a", "strand"], ["memory", "#bca8ff", "orbit"],
  ["mineral", "#78a8c8", "facet"], ["modification", "#ff9b58", "geometry"], ["mucus", "#72d6b4", "fluid"],
  ["mud", "#8a593d", "stone"], ["nail", "#c7b9a6", "spike"], ["permeation", "#65d7e8", "portal"],
  ["poison_plant", "#9fcb3b", "vine"], ["red_ochre", "#d64c37", "stone"], ["rock", "#777c86", "stone"],
  ["sandstone", "#d5a76c", "facet"], ["scale", "#63bdb5", "scale"], ["shakudo", "#6e4946", "shield"],
  ["skin", "#e1a995", "shield"], ["smoke", "#82788f", "wind"], ["snow", "#e7f7ff", "snow"],
  ["song", "#ffb4e8", "sound"], ["soul_corpse", "#6c7899", "soul"], ["soul", "#72d9cc", "soul"],
  ["sound", "#70c8f0", "sound"], ["spike", "#9ba8b9", "spike"], ["switching", "#ffd260", "geometry"],
  ["tongue", "#e66c70", "strand"], ["tree", "#477d39", "vine"], ["stone", "#8f8c86", "stone"],
  ["vine", "#6db743", "vine"], ["vortex", "#59b8c9", "vortex"], ["wing", "#e6e2f4", "wing"],
];

const W = 96;
const S = W * W;
const pixels = new Uint8Array(S * 4);
const rgba = (hex, alpha = 255) => {
  const n = Number.parseInt(hex.slice(1), 16);
  return [(n >> 16) & 255, (n >> 8) & 255, n & 255, alpha];
};
function blend(x, y, color) {
  if (x < 0 || y < 0 || x >= W || y >= W) return;
  const i = (y * W + x) * 4, a = color[3] / 255, inv = 1 - a;
  pixels[i] = Math.round(color[0] * a + pixels[i] * inv);
  pixels[i + 1] = Math.round(color[1] * a + pixels[i + 1] * inv);
  pixels[i + 2] = Math.round(color[2] * a + pixels[i + 2] * inv);
  pixels[i + 3] = Math.min(255, Math.round(color[3] + pixels[i + 3] * inv));
}
function disk(x, y, r, color) {
  const rr = r * r;
  for (let py = Math.floor(y - r); py <= Math.ceil(y + r); py++)
    for (let px = Math.floor(x - r); px <= Math.ceil(x + r); px++)
      if ((px - x) ** 2 + (py - y) ** 2 <= rr) blend(px, py, color);
}
function line(a, b, width, color) {
  const dx = b[0] - a[0], dy = b[1] - a[1], len = Math.max(1, Math.hypot(dx, dy));
  const steps = Math.ceil(len * 1.5);
  for (let i = 0; i <= steps; i++) {
    const t = i / steps;
    disk(a[0] + dx * t, a[1] + dy * t, width / 2, color);
  }
}
function path(points, width, color) {
  for (let i = 1; i < points.length; i++) line(points[i - 1], points[i], width, color);
}
function ring(cx, cy, r, width, color, start = 0, end = Math.PI * 2) {
  const points = [];
  const n = Math.max(32, Math.ceil(r * (end - start)));
  for (let i = 0; i <= n; i++) {
    const a = start + (end - start) * i / n;
    points.push([cx + Math.cos(a) * r, cy + Math.sin(a) * r]);
  }
  path(points, width, color);
}
function polygon(points, color) {
  const minY = Math.max(0, Math.floor(Math.min(...points.map(p => p[1]))));
  const maxY = Math.min(W - 1, Math.ceil(Math.max(...points.map(p => p[1]))));
  for (let y = minY; y <= maxY; y++) {
    const xs = [];
    for (let i = 0; i < points.length; i++) {
      const [x1, y1] = points[i], [x2, y2] = points[(i + 1) % points.length];
      if ((y1 <= y && y2 > y) || (y2 <= y && y1 > y)) xs.push(x1 + (y - y1) * (x2 - x1) / (y2 - y1));
    }
    xs.sort((a, b) => a - b);
    for (let i = 0; i + 1 < xs.length; i += 2)
      for (let x = Math.ceil(xs[i]); x <= Math.floor(xs[i + 1]); x++) blend(x, y, color);
  }
}
function star(cx, cy, outer, inner, points, color, rotation = -Math.PI / 2) {
  const p = [];
  for (let i = 0; i < points * 2; i++) {
    const r = i % 2 ? inner : outer, a = rotation + i * Math.PI / points;
    p.push([cx + Math.cos(a) * r, cy + Math.sin(a) * r]);
  }
  polygon(p, color);
}
function motif(type, seed, glow, white) {
  const cx = 48, cy = 47, g = rgba(glow), pale = rgba(white), dark = rgba("#160d24");
  const shift = (seed % 5) - 2;
  const curve = (phase, amp = 8) => {
    const p = [];
    for (let i = 0; i <= 32; i++) {
      const t = i / 32;
      p.push([cx + (t - 0.5) * 32, cy + Math.sin(t * Math.PI * 2 + phase) * amp]);
    }
    return p;
  };
  if (type === "wind" || type === "vortex") {
    for (let k = 0; k < 3; k++) {
      const p = [];
      for (let i = 0; i <= 36; i++) {
        const t = i / 36, a = t * Math.PI * (2.2 + k * 0.35) + k * 2.1;
        const r = 2 + t * (18 + k * 2);
        p.push([cx + Math.cos(a) * r, cy + Math.sin(a) * r * 0.76]);
      }
      path(p, k === 1 ? 4 : 2.6, k === 1 ? pale : g);
    }
  } else if (type === "strand" || type === "vine") {
    const main = [];
    for (let i = 0; i <= 30; i++) {
      const t = i / 30;
      main.push([cx + shift + Math.sin(t * Math.PI * 2) * (4 + t * 5), cy + 21 - t * 42]);
    }
    path(main, 5, g);
    for (const side of [-1, 1]) {
      const y = cy + side * 7;
      path([[cx, y], [cx + side * 9, y - 8], [cx + side * 14, y - 14]], 2.7, pale);
      star(cx + side * 14, y - 14, 3, 1.5, 4, g);
    }
  } else if (type === "facet" || type === "geometry") {
    const radius = type === "facet" ? 19 : 17;
    const p = Array.from({ length: 6 }, (_, i) => {
      const a = -Math.PI / 2 + i * Math.PI / 3 + shift * 0.025;
      return [cx + Math.cos(a) * radius, cy + Math.sin(a) * radius];
    });
    path([...p, p[0]], 3.2, pale);
    path([[cx, cy], p[0], p[2], [cx, cy], p[3], p[5], [cx, cy]], 2, g);
    ring(cx, cy, 7 + seed % 4, 2, g);
  } else if (type === "fluid") {
    ring(cx, cy, 12, 3, g);
    for (let i = 0; i < 5; i++) {
      const a = -Math.PI / 2 + i * Math.PI * 2 / 5 + shift * 0.08;
      const x = cx + Math.cos(a) * (16 + (i % 2) * 4), y = cy + Math.sin(a) * (16 + (i % 2) * 4);
      disk(x, y, 3 + (i % 2), i % 2 ? pale : g);
    }
    disk(cx, cy, 6, pale);
  } else if (type === "stone" || type === "shield") {
    const p = [[cx - 15, cy - 13], [cx - 5, cy - 20], [cx + 14, cy - 15], [cx + 19, cy + 2],
      [cx + 10, cy + 17], [cx - 9, cy + 19], [cx - 18, cy + 5]];
    polygon(p, dark);
    path([...p, p[0]], 3.2, g);
    path([[cx - 8, cy + 7], [cx - 4, cy - 7], [cx + 1, cy + 3], [cx + 7, cy - 11], [cx + 10, cy + 5]], 3.2, pale);
    if (type === "shield") path([[cx - 14, cy - 8], [cx, cy - 16], [cx + 14, cy - 8]], 2.3, g);
  } else if (type === "spike") {
    for (let i = -1; i <= 1; i++) {
      const x = cx + i * 12;
      polygon([[x - 5, cy + 14], [x, cy - 18 - Math.abs(i) * 2], [x + 5, cy + 14]], i ? g : pale);
      line([x, cy - 12], [x, cy + 12], 1.3, dark);
    }
  } else if (type === "scale") {
    for (let row = 0; row < 4; row++) for (let col = 0; col < 3; col++) {
      const x = cx - 13 + col * 13 + (row % 2) * 6, y = cy - 13 + row * 9;
      ring(x, y + 3, 5.2, 2.2, (row + col) % 2 ? pale : g, Math.PI, Math.PI * 2);
    }
  } else if (type === "snow") {
    for (let i = 0; i < 6; i++) {
      const a = i * Math.PI / 3;
      const end = [cx + Math.cos(a) * 21, cy + Math.sin(a) * 21];
      line([cx, cy], end, 3, pale);
      for (const t of [0.58, 0.78]) {
        const x = cx + Math.cos(a) * 21 * t, y = cy + Math.sin(a) * 21 * t;
        for (const s of [-1, 1]) line([x, y], [x + Math.cos(a + s * 0.75) * 6, y + Math.sin(a + s * 0.75) * 6], 2, g);
      }
    }
  } else if (type === "sound") {
    for (let i = 0; i < 4; i++) {
      const r = 7 + i * 5 + (seed % 3);
      ring(cx, cy, r, i === 1 ? 3 : 2, i % 2 ? g : pale, -1.25, 1.25);
      ring(cx, cy, r, 2, g, Math.PI - 1.25, Math.PI + 1.25);
    }
    path(curve(seed * 0.4, 12), 3, pale);
  } else if (type === "soul") {
    ring(cx, cy, 13, 2.5, g);
    const heart = [];
    for (let i = 0; i <= 40; i++) {
      const t = i / 40 * Math.PI * 2, x = 16 * Math.sin(t) ** 3, y = -(13 * Math.cos(t) - 5 * Math.cos(2 * t) - 2 * Math.cos(3 * t) - Math.cos(4 * t));
      heart.push([cx + x * 0.74, cy + y * 0.74]);
    }
    path(heart, 3, pale);
    for (let i = 0; i < 3; i++) star(cx + (i - 1) * 15, cy - 20, 2.5, 1.1, 4, g);
  } else if (type === "portal") {
    ring(cx, cy, 17, 3, g);
    ring(cx, cy, 10, 2.5, pale, -Math.PI / 2 + shift * 0.2, Math.PI * 1.35 + shift * 0.2);
    path([[cx - 4, cy + 11], [cx - 4, cy - 9], [cx + 4, cy - 9], [cx + 4, cy + 11]], 2.4, g);
    line([cx, cy - 5], [cx, cy + 7], 2, pale);
  } else if (type === "wing") {
    for (const side of [-1, 1]) {
      const edge = [[cx, cy + 13], [cx + side * 8, cy + 7], [cx + side * 17, cy - 7], [cx + side * 20, cy - 18]];
      path(edge, 3.2, pale);
      for (let i = 0; i < 4; i++) {
        const y = cy + 10 - i * 7;
        path([[cx + side * 2, y], [cx + side * (10 + i * 2), y - 5], [cx + side * (15 + i), y - 12]], 2.5, g);
      }
    }
  } else {
    path(curve(seed * 0.3), 3.2, g);
  }
}
function makeIcon(entry, index) {
  const [, hex, shape] = entry, glow = rgba(hex), pale = rgba("#f2ead5");
  pixels.fill(0);
  const cx = 47.5, cy = 47.5;
  for (let y = 0; y < W; y++) for (let x = 0; x < W; x++) {
    const r = Math.hypot(x - cx, y - cy);
    if (r < 43) {
      const t = Math.max(0, 1 - r / 46), i = (y * W + x) * 4;
      pixels[i] = Math.round(8 + t * (glow[0] * 0.22));
      pixels[i + 1] = Math.round(11 + t * (glow[1] * 0.17));
      pixels[i + 2] = Math.round(25 + t * (glow[2] * 0.25));
      pixels[i + 3] = 255;
    }
  }
  ring(cx, cy, 42, 3, rgba("#b98c47"));
  ring(cx, cy, 38, 1.4, rgba("#f4d997"));
  ring(cx, cy, 32, 1.2, rgba(hex, 155));
  for (let i = 0; i < 12; i++) {
    const a = i * Math.PI / 6, r0 = i % 3 === 0 ? 34 : 36, r1 = 38;
    line([cx + Math.cos(a) * r0, cy + Math.sin(a) * r0], [cx + Math.cos(a) * r1, cy + Math.sin(a) * r1],
      i % 3 === 0 ? 2.2 : 1.2, rgba(i % 3 === 0 ? "#f4d997" : hex, 200));
  }
  motif(shape, index, hex, "#f2ead5");
  for (let i = 0; i < 4; i++) {
    const a = Math.PI / 4 + i * Math.PI / 2;
    star(cx + Math.cos(a) * 28, cy + Math.sin(a) * 28, 2.3, 1, 4, rgba(hex));
  }
  const out = new Uint8Array(32 * 32 * 4);
  for (let y = 0; y < 32; y++) for (let x = 0; x < 32; x++) {
    for (let c = 0; c < 4; c++) {
      let sum = 0;
      for (let sy = 0; sy < 3; sy++) for (let sx = 0; sx < 3; sx++) sum += pixels[((y * 3 + sy) * W + x * 3 + sx) * 4 + c];
      out[(y * 32 + x) * 4 + c] = Math.round(sum / 9);
    }
  }
  return out;
}
const crcTable = new Uint32Array(256);
for (let n = 0; n < 256; n++) {
  let c = n;
  for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1;
  crcTable[n] = c >>> 0;
}
function crc32(buffer) {
  let c = 0xffffffff;
  for (const value of buffer) c = crcTable[(c ^ value) & 255] ^ (c >>> 8);
  return (c ^ 0xffffffff) >>> 0;
}
function chunk(type, data) {
  const name = Buffer.from(type), out = Buffer.alloc(data.length + 12);
  out.writeUInt32BE(data.length, 0); name.copy(out, 4); data.copy(out, 8);
  out.writeUInt32BE(crc32(Buffer.concat([name, data])), data.length + 8);
  return out;
}
function png(rgbaPixels) {
  const raw = Buffer.alloc((32 * 4 + 1) * 32);
  for (let y = 0; y < 32; y++) Buffer.from(rgbaPixels.buffer, rgbaPixels.byteOffset + y * 32 * 4, 32 * 4).copy(raw, y * (32 * 4 + 1) + 1);
  const header = Buffer.alloc(13);
  header.writeUInt32BE(32, 0); header.writeUInt32BE(32, 4);
  header[8] = 8; header[9] = 6;
  return Buffer.concat([Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]), chunk("IHDR", header), chunk("IDAT", deflateSync(raw)), chunk("IEND", Buffer.alloc(0))]);
}

mkdirSync(output, { recursive: true });
const force = process.argv.includes("--force");
const requested = new Set(process.argv.slice(2).filter(arg => !arg.startsWith("--")));
for (const [index, entry] of entries.entries()) {
  const [id] = entry;
  if (requested.size && !requested.has(id)) continue;
  const path = join(output, `${id}.png`);
  if (!force && existsSync(path)) { console.log(`kept existing icon: ${id}`); continue; }
  writeFileSync(path, png(makeIcon(entry, index)));
  console.log(`generated grimoire skill icon: ${id}`);
}
