"""3.86.1 skill icons, redrawn: lit, bevelled relief emblems on richer backgrounds in ornate frames (replaces the flat silhouettes).

    python tools/icon_forge.py [--only substring] [--sheet out.png]

Writes, for every grimoire magic (active / buff / ultimate) and every Beast Will skill (Unique / Ultimate):
  * the live 32x32 icon at its old path (what ManasCore's lists draw), reduced from the rich 256 px render;
  * a 128x128 HD copy under textures/skill/hd/<same path> (kept for screens that draw larger).
The originals stay under textures/skill/classic/<same path> (written once, never overwritten). Deterministic.
Emblem shapes come from gen_skill_icons.glyph_mask and gen_bw_skill_icons.draw_emblem; each shape is turned into a height field (three blurs),
lit from the top-left (diffuse + specular), engraved with contour lines, rim-lit in the magic colour, dropped a soft shadow, then framed with gems.
"""
import hashlib, math, os, sys
import numpy as np
from PIL import Image, ImageDraw, ImageFilter

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import gen_skill_icons as G
import gen_bw_skill_icons as B

S, U = G.S, G.U
TEX, OUT = G.TEX, G.OUT
LIGHT = np.array([-0.45, -0.55, 0.70], np.float32); LIGHT /= np.linalg.norm(LIGHT)
HALF = LIGHT + np.array([0, 0, 1], np.float32); HALF /= np.linalg.norm(HALF)


def relief(a, glow, body, dark=False, scale=1.0, glow_strength=1.0, tint_mask=None):
    """RGBA float canvas for a float mask a (0..1, SxS): drop shadow, halo, dark outline, bevelled lit body, contour engraving, rim light."""
    if scale != 1.0:
        sz = int(S * scale)
        m2 = Image.fromarray((np.clip(a, 0, 1) * 255).astype(np.uint8)).resize((sz, sz), Image.LANCZOS)
        full = Image.new("L", (S, S), 0); full.paste(m2, ((S - sz) // 2, (S - sz) // 2))
        a = np.asarray(full).astype(np.float32) / 255
    solid = (a > 0.3).astype(np.float32)
    facet = ((a > 0.25) & (a < 0.85)).astype(np.float32)
    sm = G.blur(solid, 1.0 * U)
    h = (G.blur(solid, 1.4 * U) + G.blur(solid, 3.2 * U) + G.blur(solid, 6.5 * U)) / 3.0 * solid
    gy, gx = np.gradient(h)
    k = 26.0
    n = np.dstack([-gx * k * U, -gy * k * U, np.ones_like(h)])
    n /= np.linalg.norm(n, axis=2, keepdims=True)
    diff = np.clip((n * LIGHT).sum(2), 0, 1)
    spec = np.clip((n * HALF).sum(2), 0, 1) ** 26
    g, b = np.array(G.rgb(glow), np.float32) / 255, np.array(G.rgb(body), np.float32) / 255
    yy = np.mgrid[0:S, 0:S][0].astype(np.float32) / S
    t = np.clip((yy - 0.2) / 0.6, 0, 1)[..., None]
    top = np.clip(b * 1.1 + 0.10, 0, 1) if not dark else np.array([0.20, 0.15, 0.18], np.float32)
    bot = (b * 0.50 + g * 0.30) if not dark else np.array([0.04, 0.03, 0.05], np.float32)
    base = top * (1 - t) + bot * t
    if tint_mask is not None:
        base = base * (1 - tint_mask[..., None]) + np.clip(g * 0.6 + 0.4, 0, 1) * tint_mask[..., None]
    col = base * (0.30 + 0.95 * diff[..., None])
    col = col + spec[..., None] * (0.40 if not dark else 0.30) * (0.5 * b + 0.5)
    iso = (np.abs(((h * 7.0) % 1.0) - 0.5) < 0.07) & (h > 0.06)                      # engraved contour lines
    col = np.where(iso[..., None], col * 0.80, col)
    col = col * (1 - facet[..., None] * 0.30) + g * 0.35 * facet[..., None]
    fres = (1 - n[..., 2]) ** 1.6 * solid
    col = col + g * np.clip(fres, 0, 1)[..., None] * 1.1
    col = col * (0.72 + 0.28 * np.clip(h * 3.0, 0, 1)[..., None])
    outline = np.clip(np.maximum(G.blur(solid, 0.9 * U) * 3.0, solid), 0, 1)
    halo = np.clip(G.blur(solid, 2.8 * U) * 2.2, 0, 1)
    sh = np.zeros_like(solid); dy, dx = int(1.4 * U), int(1.0 * U)
    sh[dy:, dx:] = solid[:-dy, :-dx]
    shadow = np.clip(G.blur(sh, 1.6 * U) * 1.4, 0, 1) * (1 - solid)
    line = np.array([0.03, 0.02, 0.04], np.float32) if not dark else g * 0.95
    out = np.zeros((S, S, 4), np.float32)
    out[..., :3] = g
    out[..., 3] = halo * 0.85 * glow_strength
    out[..., :3] = out[..., :3] * (1 - shadow[..., None] * 0.9) + 0.0 * shadow[..., None]
    out[..., 3] = np.maximum(out[..., 3], shadow * 0.55)
    ol = outline * (1 - solid)
    out[..., :3] = out[..., :3] * (1 - ol[..., None]) + line * ol[..., None]
    out[..., 3] = np.maximum(out[..., 3], ol)
    col = np.clip(col, 0, 1)
    out[..., :3] = out[..., :3] * (1 - solid[..., None]) + col * solid[..., None]
    out[..., 3] = np.maximum(out[..., 3], solid)
    return out


def gem(dst, cx, cy, r, colour):
    """A small faceted gem: dark socket, coloured body, white glint."""
    im = Image.new("L", (S, S), 0)
    ImageDraw.Draw(im).ellipse([cx - r - 0.5 * U, cy - r - 0.5 * U, cx + r + 0.5 * U, cy + r + 0.5 * U], fill=255)
    m = np.asarray(im).astype(np.float32) / 255
    dst[..., :3] = dst[..., :3] * (1 - m[..., None]) + np.array([0.05, 0.04, 0.05], np.float32) * m[..., None]
    im = Image.new("L", (S, S), 0)
    ImageDraw.Draw(im).ellipse([cx - r, cy - r, cx + r, cy + r], fill=255)
    m = np.asarray(im).astype(np.float32) / 255
    c = np.array(G.rgb(colour), np.float32) / 255
    yy, xx = np.mgrid[0:S, 0:S].astype(np.float32)
    shade = np.clip(1.15 - ((xx - cx) * 0.5 + (yy - cy) * 0.8) / (r * 1.6), 0.45, 1.25)
    dst[..., :3] = dst[..., :3] * (1 - m[..., None]) + np.clip(c * shade[..., None], 0, 1) * m[..., None]
    im = Image.new("L", (S, S), 0)
    ImageDraw.Draw(im).ellipse([cx - r * 0.55, cy - r * 0.6, cx - r * 0.1, cy - r * 0.15], fill=255)
    gl = np.asarray(im.filter(ImageFilter.GaussianBlur(0.3 * U))).astype(np.float32) / 255
    dst[..., :3] = np.clip(dst[..., :3] + gl[..., None] * 0.9, 0, 1)
    G.add_glow(dst, np.clip(G.blur(m, 1.2 * U), 0, 1) * (1 - m), colour, 0.35)


def frame2(img, metal, glow, corner, gem_colour=None):
    G.frame(img, metal, rim_glow=glow if corner == "spike" else None, corner=corner)
    gc = gem_colour if gem_colour is not None else glow
    o, mid = 5.2 * U, S / 2
    for (x, y) in ((o, o), (S - o, o), (o, S - o), (S - o, S - o)):
        gem(img, x, y, 1.9 * U, gc)
    for (x, y) in ((mid, 1.5 * U), (mid, S - 1.5 * U), (1.5 * U, mid), (S - 1.5 * U, mid)):
        gem(img, x, y, 0.9 * U, 0xFFF0C0 if corner != "spike" else gc)
    return img


def finish(img, hd_path, live_path, rel):
    """Save the 128 px HD copy and the 32 px live icon; keep the original once under classic/."""
    arr = (np.clip(img, 0, 1) * 255).astype(np.uint8)
    full = Image.fromarray(arr, "RGBA")
    hd = full.resize((128, 128), Image.LANCZOS)
    a = np.asarray(hd).copy(); a[..., 3] = np.where(a[..., 3] > 128, 255, 0)
    os.makedirs(os.path.dirname(hd_path), exist_ok=True)
    Image.fromarray(a, "RGBA").save(hd_path)
    live = G.reduce(img)
    classic = os.path.join(TEX, "classic", rel)
    if not os.path.exists(classic) and os.path.exists(live_path):
        os.makedirs(os.path.dirname(classic), exist_ok=True)
        Image.open(live_path).save(classic)
    live.save(live_path)
    return live


def magic_variants(name, seed):
    _, glow, body, _, _ = G.MAGIC[name]
    dark = name in ("anti_magic", "forbidden")
    a = G.to_arr(G.glyph_mask(name))
    # active
    img = G.bg_aura(glow if not dark else 0x5A0A2A)
    G.streaks(img, glow, seed + 1); G.particles(img, glow, 22, seed + 2, size=(0.4, 1.0))
    G.over(img, relief(a, glow, body, dark, 0.92))
    act = frame2(img, G.GOLD, glow, "clover", 0xE23B4B)
    # buff
    img = G.bg_ancient(glow)
    G.chains(img, (112, 96, 80))
    G.magic_circle(img, glow if name != "creation" else 0xE8C050, 11.8, ticks=24, width=0.9, rot=seed * 0.1)
    G.magic_circle(img, glow, 8.4, ticks=12, width=0.6, strength=0.6, rot=-seed * 0.2)
    G.over(img, relief(a, glow, body, dark, 0.64, 0.8))
    G.particles(img, glow, 10, seed + 3, size=(0.4, 0.9), area=0.7)
    buf = frame2(img, G.SILVER, glow, "rune", 0x5AC8FF)
    # ultimate
    img = G.bg_void(glow)
    G.magic_circle(img, glow, 12.2, star=5, ticks=30, width=0.7, strength=0.9, rot=seed * 0.13)
    G.magic_circle(img, glow, 7.6, star=6 if name != "forbidden" else 5, ticks=18, width=0.6, strength=0.7, rot=-seed * 0.21)
    G.particles(img, glow, 30, seed + 4, size=(0.3, 1.0), area=0.95)
    G.over(img, relief(a, glow, body, dark, 0.86, 1.4))
    ult = frame2(img, G.IRON, glow, "spike", glow)
    return act, buf, ult


def beast_icon(tint, emblem, ult, idx):
    lay = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    B.draw_emblem(ImageDraw.Draw(lay), emblem, tint, (255, 0, 0, 255), (0, 255, 0, 255), (0, 0, 255, 255))
    p = np.asarray(lay).astype(np.float32) / 255
    a = np.clip(p[..., 3], 0, 1)
    eye = (p[..., 1] > 0.6) & (p[..., 0] < 0.4) & (a > 0.5)
    rimm = (p[..., 2] > 0.6) & (p[..., 0] < 0.4) & (a > 0.5)
    glow = (tint[0] << 16) | (tint[1] << 8) | tint[2]
    body = (B.lt(tint, 0.06)[0] << 16) | (B.lt(tint, 0.06)[1] << 8) | B.lt(tint, 0.06)[2]
    g = np.array(tint, np.float32) / 255
    yy = np.mgrid[0:S, 0:S][0].astype(np.float32) / S
    n = G.fbm(S, S, 64, 40 + idx)[..., None]
    bg = (g * 0.18 + np.array([0.02, 0.02, 0.05], np.float32)) * (1 - G.R[..., None] * 0.5) + g * np.exp(-(G.R / 0.55) ** 2)[..., None] * (0.34 if ult else 0.22) + (n - 0.5) * 0.07
    img = np.dstack([np.clip(bg, 0, 1), np.ones((S, S))]).astype(np.float32)
    if ult:
        rays = (0.5 + 0.5 * np.cos(G.ANG * 14)) ** 7 * np.clip(1.05 - G.R, 0, 1)
        G.add_glow(img, rays, glow, 0.5)
        G.magic_circle(img, glow, 12.0, star=5, ticks=28, width=0.7, strength=0.8, rot=idx * 0.17)
    else:
        G.magic_circle(img, glow, 11.6, ticks=20, width=0.6, strength=0.55, rot=idx * 0.11)
    G.particles(img, glow, 16 if ult else 8, idx + 5, size=(0.3, 0.9), area=0.9)
    tint_mask = (rimm.astype(np.float32))
    G.over(img, relief(a, glow, body, False, 0.88, 1.2 if ult else 0.8, tint_mask))
    ex = G.blur(eye.astype(np.float32), 0.6 * U)
    G.add_glow(img, np.clip(ex * 1.5, 0, 1), 0xFFFFFF, 1.0)
    G.add_glow(img, np.clip(G.blur(eye.astype(np.float32), 2.4 * U) * 2, 0, 1), glow, 0.8)
    return frame2(img, G.GOLD if ult else G.SILVER, glow, "clover" if ult else "rune", 0xFFD24A if ult else 0x9FD0FF)


def main():
    only = sys.argv[sys.argv.index("--only") + 1] if "--only" in sys.argv else None
    made = []
    for i, name in enumerate(G.MAGIC):
        if only and only not in name:
            continue
        act, buf, ult = magic_variants(name, i * 7 + 1)
        if name == "anti_magic":
            act = G.variant_active_anti_magic(i * 7 + 1)
        for im, rel in ((act, "grimoire/%s.png" % name), (buf, "icons/%s_buff.png" % name), (ult, "icons/%s_ultimate.png" % name)):
            made.append(finish(im, os.path.join(TEX, "hd", rel), os.path.join(TEX, rel), rel))
    seen = {}
    for idx, (key, (tint, emblem)) in enumerate(B.BEASTS.items()):
        for u, nm in ((False, "bw_%s_unique" % key), (True, "bw_%s" % key)):
            if only and only not in nm:
                continue
            im = beast_icon(tint, emblem, u, idx * 2 + (1 if u else 0) + 1)
            rel = nm + ".png"
            icon = finish(im, os.path.join(TEX, "hd", rel), os.path.join(TEX, rel), rel)
            h = hashlib.md5(icon.tobytes()).hexdigest()
            assert h not in seen, nm + " identical to " + seen[h]
            seen[h] = nm
            made.append(icon)
    print("wrote", len(made), "icons (32 px live + 128 px hd)")
    if "--sheet" in sys.argv:
        Z, cols = 6, 12
        rows = (len(made) + cols - 1) // cols
        sh = Image.new("RGBA", (cols * 32 * Z, rows * 32 * Z), (36, 36, 40, 255))
        for i, im in enumerate(made):
            sh.alpha_composite(im.convert("RGBA").resize((32 * Z, 32 * Z), Image.NEAREST), ((i % cols) * 32 * Z, (i // cols) * 32 * Z))
        sh.save(sys.argv[sys.argv.index("--sheet") + 1])


if __name__ == "__main__":
    main()
