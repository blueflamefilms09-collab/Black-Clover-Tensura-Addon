"""Generates the Glass Magic VFX textures (deterministic, no fonts, no external images).

    python3 -B tools/gen_glass_textures.py

Output: src/main/resources/assets/nusmp/textures/particle/glass_*.png (only glass_* files are written).

Look reference (docs/attributes/art_reference/pack_glass.webp, Verre Detection): a faceted, almost clear crystal solid whose planes
are barely tinted, each plane catching a little light; razor-thin bright edge lines; a soft white bloom hugging the silhouette and
broad pale light shafts behind it. So: clear facets (low alpha, a few brighter ones), white edges, white bloom, long soft shafts.
All sprites are white / grey with alpha, tinted by the vertex colour.
"""
import math
import os

import numpy as np
from PIL import Image, ImageChops, ImageDraw, ImageFilter

OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "nusmp", "textures", "particle")
SS = 4


def save(im, name):
    os.makedirs(OUT, exist_ok=True)
    im.save(os.path.join(OUT, name + ".png"), optimize=True)
    print("wrote", name, im.size)


def blur(im, r):
    return im.filter(ImageFilter.GaussianBlur(r))


def to_rgba(lum, alpha):
    l = np.clip(np.asarray(lum, dtype=np.float32), 0, 255)
    a = np.clip(np.asarray(alpha, dtype=np.float32), 0, 255)
    return Image.fromarray(np.dstack([l, l, l, a]).astype(np.uint8), "RGBA")


def finish(lum_img, alpha_img, w, h):
    lum = lum_img.resize((w, h), Image.LANCZOS)
    al = alpha_img.resize((w, h), Image.LANCZOS)
    return to_rgba(np.asarray(lum), np.asarray(al))


def shade_polygon(pts, rng, w, h, base=70, spread=60):
    """Draw a polygon as triangle facets (fan from a jittered inner point) with varied alpha; returns (alpha L image, edge L image)."""
    A = Image.new("L", (w, h), 0)
    E = Image.new("L", (w, h), 0)
    da, de = ImageDraw.Draw(A), ImageDraw.Draw(E)
    cx = sum(p[0] for p in pts) / len(pts) + rng.uniform(-0.08, 0.08) * w
    cy = sum(p[1] for p in pts) / len(pts) + rng.uniform(-0.08, 0.08) * h
    n = len(pts)
    for i in range(n):
        a, b = pts[i], pts[(i + 1) % n]
        v = int(np.clip(base + rng.uniform(-spread, spread) + (40 if rng.random() < 0.25 else 0), 18, 200))
        da.polygon([a, b, (cx, cy)], fill=v)
        de.line([a, b], fill=255, width=SS * 2)
        de.line([(cx, cy), a], fill=int(rng.uniform(90, 200)), width=SS)
    return A, E


def glint_line(draw, p0, p1, w, v):
    draw.line([p0, p1], fill=v, width=w)


def make_facet(size=256, seed=11):
    """Big faceted clear crystal: pale planes, white edges, tight bloom (the 'Verre' solid of the reference)."""
    rng = np.random.default_rng(seed)
    W = size * SS
    c = W / 2
    pts = []
    n = 9
    for i in range(n):
        a = math.tau * i / n + rng.uniform(-0.18, 0.18)
        r = W * rng.uniform(0.30, 0.40)
        pts.append((c + math.cos(a) * r, c + math.sin(a) * r * 0.95))
    A, E = shade_polygon(pts, rng, W, W, 62, 50)
    sil = Image.new("L", (W, W), 0)
    ImageDraw.Draw(sil).polygon(pts, fill=255)
    # inner secondary facet outline (a second crystal plane seen through the first)
    inner = [(c + (x - c) * 0.55 + rng.uniform(-18, 18), c + (y - c) * 0.55 + rng.uniform(-18, 18)) for x, y in pts[::2]]
    A2, E2 = shade_polygon(inner, rng, W, W, 40, 30)
    A = ImageChops.add(A, A2)
    E = ImageChops.add(E, ImageChops.multiply(E2, Image.new("L", (W, W), 150)))
    bloom = blur(sil, W * 0.035)
    bloom2 = blur(sil, W * 0.012)
    al = np.asarray(A, np.float32) * 0.55 + np.asarray(E, np.float32) * 0.95 + np.asarray(bloom, np.float32) * 0.5 + np.asarray(bloom2, np.float32) * 0.35
    lum = 215 + np.asarray(E, np.float32) * 0.15 + np.asarray(bloom2, np.float32) * 0.1
    # specular streak across the upper-left planes
    S = Image.new("L", (W, W), 0)
    ds = ImageDraw.Draw(S)
    glint_line(ds, (c - W * 0.22, c - W * 0.05), (c + W * 0.06, c - W * 0.3), SS * 7, 140)
    glint_line(ds, (c - W * 0.12, c + W * 0.02), (c + W * 0.14, c - W * 0.2), SS * 3, 200)
    al += np.asarray(blur(S, SS * 2), np.float32)
    # edge fade so nothing touches the border
    yy, xx = np.mgrid[0:W, 0:W].astype(np.float32)
    d = np.hypot(xx - c, yy - c) / (W / 2)
    al *= np.clip((1 - d) * 5, 0, 1)
    return to_rgba(np.asarray(Image.fromarray(np.clip(lum, 0, 255).astype(np.uint8)).resize((size, size), Image.LANCZOS)),
                   np.asarray(Image.fromarray(np.clip(al, 0, 255).astype(np.uint8)).resize((size, size), Image.LANCZOS)))


def make_shard(w=128, h=256, seed=21):
    """Long needle shard pointing up: bright centre ridge, two shaded flanks, white edge lines, glow."""
    rng = np.random.default_rng(seed)
    W, H = w * SS, h * SS
    cx = W / 2
    tip = (cx + W * 0.03, H * 0.04)
    base_l, base_r = (cx - W * 0.26, H * 0.82), (cx + W * 0.24, H * 0.86)
    mid_l, mid_r = (cx - W * 0.20, H * 0.5), (cx + W * 0.17, H * 0.47)
    foot = (cx + W * 0.02, H * 0.97)
    ridge = (cx - W * 0.02, H * 0.55)
    A = Image.new("L", (W, H), 0)
    E = Image.new("L", (W, H), 0)
    da, de = ImageDraw.Draw(A), ImageDraw.Draw(E)
    outline = [tip, mid_r, base_r, foot, base_l, mid_l]
    sil = Image.new("L", (W, H), 0)
    ImageDraw.Draw(sil).polygon(outline, fill=255)
    da.polygon([tip, mid_l, ridge], fill=70)
    da.polygon([tip, ridge, mid_r], fill=125)
    da.polygon([mid_l, base_l, foot, ridge], fill=55)
    da.polygon([ridge, foot, base_r, mid_r], fill=100)
    da.polygon([mid_l, ridge, (cx - W * 0.1, H * 0.7)], fill=95)
    for a, b in zip(outline, outline[1:] + outline[:1]):
        de.line([a, b], fill=255, width=SS * 2)
    for p in (mid_l, mid_r, base_l, base_r, foot):
        de.line([ridge, p], fill=170, width=SS)
    de.line([tip, ridge], fill=235, width=SS * 2)
    bloom = blur(sil, W * 0.05)
    al = (np.asarray(A, np.float32) * 0.85 + np.asarray(E, np.float32) * 0.95 + np.asarray(bloom, np.float32) * 0.45)
    lum = np.full((H, W), 225, np.float32) + np.asarray(E, np.float32) * 0.1
    yy, xx = np.mgrid[0:H, 0:W].astype(np.float32)
    al *= np.clip((0.5 - np.abs(xx / W - 0.5)) * 9, 0, 1) * np.clip(yy / (H * 0.03), 0, 1) * np.clip((H - yy) / (H * 0.03), 0, 1)
    return to_rgba(np.asarray(Image.fromarray(np.clip(lum, 0, 255).astype(np.uint8)).resize((w, h), Image.LANCZOS)),
                   np.asarray(Image.fromarray(np.clip(al, 0, 255).astype(np.uint8)).resize((w, h), Image.LANCZOS)))


def make_pane(size=256, seed=31):
    """A square window pane: bright frame line, diagonal reflection streaks, corner glints, faint dust."""
    rng = np.random.default_rng(seed)
    W = size * SS
    A = Image.new("L", (W, W), 0)
    d = ImageDraw.Draw(A)
    m = W * 0.08
    d.rectangle([m, m, W - m, W - m], fill=40)
    for off, wid, v in ((-0.25, 0.10, 90), (0.0, 0.05, 130), (0.12, 0.16, 70), (0.3, 0.03, 160)):
        x0 = W * (0.5 + off)
        d.polygon([(x0 - W * wid, W - m), (x0 + W * wid, W - m), (x0 + W * wid + W * 0.5, m), (x0 - W * wid + W * 0.5, m)], fill=v)
    mask = Image.new("L", (W, W), 0)
    ImageDraw.Draw(mask).rectangle([m, m, W - m, W - m], fill=255)
    A = ImageChops.multiply(A, mask)
    E = Image.new("L", (W, W), 0)
    de = ImageDraw.Draw(E)
    de.rectangle([m, m, W - m, W - m], outline=255, width=SS * 3)
    de.rectangle([m + SS * 9, m + SS * 9, W - m - SS * 9, W - m - SS * 9], outline=130, width=SS)
    for cx, cy in ((m, m), (W - m, m), (m, W - m), (W - m, W - m)):
        de.line([(cx - W * 0.07, cy), (cx + W * 0.07, cy)], fill=230, width=SS * 2)
        de.line([(cx, cy - W * 0.07), (cx, cy + W * 0.07)], fill=230, width=SS * 2)
    bloom = blur(mask, W * 0.025)
    al = np.asarray(A, np.float32) * 0.85 + np.asarray(E, np.float32) + np.asarray(bloom, np.float32) * 0.35
    lum = np.full((W, W), 228, np.float32)
    return finish(Image.fromarray(lum.astype(np.uint8)), Image.fromarray(np.clip(al, 0, 255).astype(np.uint8)), size, size)


def make_crack(size=256, seed=41):
    """Radial fracture web: branching hairline cracks from the centre, bright core, a few chipped wedges."""
    rng = np.random.default_rng(seed)
    W = size * SS
    c = W / 2
    E = Image.new("L", (W, W), 0)
    de = ImageDraw.Draw(E)
    chips = Image.new("L", (W, W), 0)
    dc = ImageDraw.Draw(chips)

    def branch(x, y, ang, length, width, depth):
        steps = 6
        px, py = x, y
        for i in range(steps):
            ang += rng.uniform(-0.35, 0.35)
            seg = length / steps
            nx, ny = px + math.cos(ang) * seg, py + math.sin(ang) * seg
            de.line([(px, py), (nx, ny)], fill=int(255 * (1 - 0.5 * i / steps)), width=max(1, int(width)))
            if depth < 3 and rng.random() < 0.45:
                branch(nx, ny, ang + rng.choice([-1, 1]) * rng.uniform(0.5, 1.0), length * 0.45, width * 0.65, depth + 1)
            px, py = nx, ny

    n = 11
    for i in range(n):
        a = math.tau * i / n + rng.uniform(-0.2, 0.2)
        branch(c, c, a, W * rng.uniform(0.28, 0.46), SS * 2.4, 0)
        if i % 3 == 0:
            a2 = a + 0.18
            dc.polygon([(c, c), (c + math.cos(a) * W * 0.2, c + math.sin(a) * W * 0.2), (c + math.cos(a2) * W * 0.17, c + math.sin(a2) * W * 0.17)], fill=70)
    yy, xx = np.mgrid[0:W, 0:W].astype(np.float32)
    d = np.hypot(xx - c, yy - c) / (W / 2)
    core = np.exp(-(d * 5.5) ** 2) * 255
    Ea = np.asarray(blur(E, SS * 0.6), np.float32)
    al = Ea + np.asarray(blur(E, SS * 5), np.float32) * 0.8 + np.asarray(chips, np.float32) + core
    al *= np.clip((1 - d) * 4, 0, 1)
    return finish(Image.fromarray(np.full((W, W), 230, np.uint8)), Image.fromarray(np.clip(al, 0, 255).astype(np.uint8)), size, size)


def make_shaft(w=64, h=256, seed=51):
    """A tall soft light shaft: bright base fading up, soft side falloff, faint vertical banding."""
    rng = np.random.default_rng(seed)
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    u = xx / (w - 1) * 2 - 1
    v = yy / (h - 1)  # 0 = top of texture
    side = np.clip(1 - np.abs(u) ** 1.6, 0, 1) ** 1.5
    vert = np.clip(v * 1.15, 0, 1) ** 1.3 * np.clip((1 - v) * 14, 0, 1)
    band = 0.78 + 0.22 * np.sin(u * 7 + rng.uniform(0, 6)) * np.sin(u * 3.1 + 1.3)
    al = side * vert * band * 215
    core = np.exp(-(u * 3.2) ** 2) * vert * 60
    return to_rgba(np.full((h, w), 235), al + core)


def make_glint(size=128, seed=61):
    """Four-point refractive star with a long vertical / horizontal flare and a small cross-flare, tight core."""
    yy, xx = np.mgrid[0:size, 0:size].astype(np.float32)
    u = (xx - size / 2 + 0.5) / (size / 2)
    v = (yy - size / 2 + 0.5) / (size / 2)
    r = np.hypot(u, v) + 1e-4
    core = np.exp(-(r * 6.5) ** 2)
    halo = np.exp(-(r * 2.2) ** 2) * 0.35
    flare = np.exp(-(np.abs(u) * 26)) * np.exp(-(np.abs(v) * 1.9) ** 2) + np.exp(-(np.abs(v) * 26)) * np.exp(-(np.abs(u) * 1.9) ** 2)
    d1, d2 = (u + v) / 1.414, (u - v) / 1.414
    diag = (np.exp(-np.abs(d1) * 30) * np.exp(-(d2 * 3.2) ** 2) + np.exp(-np.abs(d2) * 30) * np.exp(-(d1 * 3.2) ** 2)) * 0.45
    al = (core + halo + flare * 0.9 + diag) * 255
    al *= np.clip((1 - r) * 3.5, 0, 1)
    return to_rgba(np.full((size, size), 245), al)


def make_ring(size=256, seed=71):
    """Faceted ground ring: two thin polygonal rings (hexagonal), radial ticks, small diamonds on the vertices, soft inner glow."""
    rng = np.random.default_rng(seed)
    W = size * SS
    c = W / 2
    E = Image.new("L", (W, W), 0)
    de = ImageDraw.Draw(E)
    F = Image.new("L", (W, W), 0)
    df = ImageDraw.Draw(F)

    def poly(r, n, rot):
        return [(c + math.cos(rot + math.tau * i / n) * r, c + math.sin(rot + math.tau * i / n) * r) for i in range(n)]

    outer = poly(W * 0.47, 12, 0.0)
    mid = poly(W * 0.41, 12, math.tau / 24)
    inner = poly(W * 0.34, 12, 0.0)
    for pts, wd, v in ((outer, 3, 255), (inner, 2, 200)):
        for i in range(len(pts)):
            de.line([pts[i], pts[(i + 1) % len(pts)]], fill=v, width=SS * wd)
    # band between outer and inner: alternating shaded triangles (facets)
    for i in range(12):
        a, b = outer[i], outer[(i + 1) % 12]
        m = mid[i]
        i0, i1 = inner[i], inner[(i + 1) % 12]
        df.polygon([a, b, m], fill=int(rng.uniform(40, 120)))
        df.polygon([a, m, i0], fill=int(rng.uniform(20, 80)))
        df.polygon([b, m, i1], fill=int(rng.uniform(20, 80)))
        df.polygon([m, i0, i1], fill=int(rng.uniform(10, 50)))
        de.line([a, m], fill=150, width=SS)
        de.line([m, i0], fill=110, width=SS)
        de.line([m, i1], fill=110, width=SS)
        # vertex diamond
        x, y = a
        s = W * 0.018
        de.polygon([(x, y - s), (x + s, y), (x, y + s), (x - s, y)], fill=255)
    yy, xx = np.mgrid[0:W, 0:W].astype(np.float32)
    d = np.hypot(xx - c, yy - c) / (W / 2)
    inner_glow = np.exp(-((d - 0.36) * 9) ** 2) * 80
    al = np.asarray(F, np.float32) * 0.9 + np.asarray(E, np.float32) + np.asarray(blur(E, SS * 3), np.float32) * 0.7 + inner_glow
    al *= np.clip((1 - d) * 12, 0, 1)
    return finish(Image.fromarray(np.full((W, W), 232, np.uint8)), Image.fromarray(np.clip(al, 0, 255).astype(np.uint8)), size, size)


def make_dust(size=256, seed=81):
    """A scatter sheet of tiny glass splinters (triangles and slivers with bright edges) and fine glitter."""
    rng = np.random.default_rng(seed)
    W = size * SS
    A = Image.new("L", (W, W), 0)
    da = ImageDraw.Draw(A)
    c = W / 2
    for i in range(46):
        a = rng.uniform(0, math.tau)
        r = W * (0.06 + 0.40 * rng.random() ** 0.8)
        x, y = c + math.cos(a) * r, c + math.sin(a) * r
        s = W * rng.uniform(0.012, 0.05)
        ang = rng.uniform(0, math.tau)
        k = rng.integers(0, 3)
        if k == 0:
            pts = [(x + math.cos(ang + j * 2.1) * s * rng.uniform(0.6, 1.3), y + math.sin(ang + j * 2.1) * s * rng.uniform(0.6, 1.3)) for j in range(3)]
        elif k == 1:
            pts = [(x + math.cos(ang) * s * 2.2, y + math.sin(ang) * s * 2.2), (x + math.cos(ang + 1.7) * s * 0.35, y + math.sin(ang + 1.7) * s * 0.35),
                   (x - math.cos(ang) * s * 1.2, y - math.sin(ang) * s * 1.2), (x + math.cos(ang - 1.7) * s * 0.35, y + math.sin(ang - 1.7) * s * 0.35)]
        else:
            pts = [(x + math.cos(ang + j * 1.57) * s, y + math.sin(ang + j * 1.57) * s) for j in range(4)]
        da.polygon(pts, fill=int(rng.uniform(110, 190)))
        da.line(pts + [pts[0]], fill=255, width=SS)
    for i in range(70):
        x, y = rng.uniform(0.08, 0.92, 2) * W
        da.ellipse([x - SS * 1.5, y - SS * 1.5, x + SS * 1.5, y + SS * 1.5], fill=255)
    al = np.asarray(A, np.float32) + np.asarray(blur(A, SS * 3), np.float32) * 0.9
    yy, xx = np.mgrid[0:W, 0:W].astype(np.float32)
    d = np.hypot(xx - c, yy - c) / (W / 2)
    al *= np.clip((1 - d) * 6, 0, 1)
    return finish(Image.fromarray(np.full((W, W), 240, np.uint8)), Image.fromarray(np.clip(al, 0, 255).astype(np.uint8)), size, size)


def make_caustic(size=256, seed=91):
    """Ground caustic: interlocking bright cell lines (voronoi edges) like light through a pane, soft falloff."""
    rng = np.random.default_rng(seed)
    pts = rng.random((26, 2)) * size
    yy, xx = np.mgrid[0:size, 0:size].astype(np.float32)
    ds = []
    for px, py in pts:
        for ox in (-size, 0, size):
            for oy in (-size, 0, size):
                ds.append(np.hypot(xx - px - ox, yy - py - oy))
    ds = np.sort(np.stack(ds), axis=0)
    edge = ds[1] - ds[0]
    line = np.exp(-(edge / 2.4) ** 2)
    cell = np.clip(1 - ds[0] / 40, 0, 1) * 0.28
    c = size / 2
    d = np.hypot(xx - c, yy - c) / c
    fall = np.clip((1 - d) * 3.5, 0, 1)
    al = (line * 230 + cell * 255) * fall
    return to_rgba(np.full((size, size), 235), al)


def make_prism(size=256, seed=101):
    """A tall faceted prism / crystal spire, clear with white edges (used as pillars and wall panes)."""
    rng = np.random.default_rng(seed)
    W = size * SS
    A = Image.new("L", (W, W), 0)
    E = Image.new("L", (W, W), 0)
    da, de = ImageDraw.Draw(A), ImageDraw.Draw(E)
    cx = W / 2
    top = (cx, W * 0.04)
    ll, lr = (cx - W * 0.16, W * 0.22), (cx + W * 0.16, W * 0.2)
    bl, br = (cx - W * 0.16, W * 0.96), (cx + W * 0.16, W * 0.96)
    mid = (cx + W * 0.02, W * 0.26)
    sil = Image.new("L", (W, W), 0)
    ImageDraw.Draw(sil).polygon([top, lr, br, bl, ll], fill=255)
    da.polygon([top, ll, mid], fill=110)
    da.polygon([top, mid, lr], fill=70)
    da.polygon([ll, mid, (cx - W * 0.03, W * 0.96), bl], fill=45)
    da.polygon([mid, lr, br, (cx - W * 0.03, W * 0.96)], fill=85)
    for a, b in ((top, ll), (top, lr), (ll, bl), (lr, br), (bl, br), (top, mid), (ll, mid), (mid, lr), (mid, (cx - W * 0.03, W * 0.96))):
        de.line([a, b], fill=255 if a == top or b == top else 190, width=SS * 2)
    sh = Image.new("L", (W, W), 0)
    ds = ImageDraw.Draw(sh)
    ds.polygon([(cx - W * 0.10, W * 0.3), (cx - W * 0.06, W * 0.3), (cx - W * 0.06, W * 0.85), (cx - W * 0.10, W * 0.85)], fill=130)
    bloom = blur(sil, W * 0.03)
    al = np.asarray(A, np.float32) * 0.9 + np.asarray(E, np.float32) + np.asarray(bloom, np.float32) * 0.5 + np.asarray(blur(sh, SS * 2), np.float32)
    return finish(Image.fromarray(np.full((W, W), 228, np.uint8)), Image.fromarray(np.clip(al, 0, 255).astype(np.uint8)), size, size)


def main():
    save(make_facet(), "glass_facet")
    save(make_shard(), "glass_shard")
    save(make_pane(), "glass_pane")
    save(make_crack(), "glass_crack")
    save(make_shaft(), "glass_shaft")
    save(make_glint(), "glass_glint")
    save(make_ring(), "glass_ring")
    save(make_dust(), "glass_dust")
    save(make_caustic(), "glass_caustic")
    save(make_prism(), "glass_prism")


if __name__ == "__main__":
    main()
