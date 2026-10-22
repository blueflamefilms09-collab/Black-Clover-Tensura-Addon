"""
Headless preview of the Scale casting model (scalecast/ScaleMesh.java, the real in-game geometry) and of what the real rules (scalecast/ScaleCastRules.java) do to it.

    python3 -B tools/scalecast/preview_scale.py

Compiles ScaleMesh and ScaleCastRules with tools/scalecast/ScaleMeshDump.java (plain javac, no Minecraft needed), dumps the quads and a table of the rules (tilt, suppression), and rasterises
scenes with the z-buffer rasteriser of tools/compass/preview_compass3d.py: nearest-texel textures, cutout alpha, item-style lighting, glows additive. Output: docs/previews/scale_casting/*.png
  scale_views.png    the Scale at rest: front, three quarter, side, back
  scale_weighing.png the beam for a heavier foe, equal weight, a lighter foe (the angles and the pan loads come from the real rules)
  scale_levels.png   the three levels of detail with their quad counts
  scale_caster.png   beside a caster, three views
  scale_aura.png     the suppression aura: ring on the ground, foes pressed by the amount the real curve gives them
This is a re-implementation of the renderer (client/scalecast/ScaleRender), not the game: lighting and blending are close, not identical.
"""
import math
import os
import subprocess
import sys

sys.dont_write_bytecode = True
import numpy as np
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
sys.path.insert(0, os.path.join(ROOT, "tools", "compass"))
import preview_compass3d as pc          # noqa: E402  (the rasteriser: Canvas, Prim, card, look, matrices, stand-in figures)

SRC = os.path.join(ROOT, "src", "main", "java", "com", "newuniverse", "nusmp", "scalecast")
JAVA = [os.path.join(SRC, "ScaleMesh.java"), os.path.join(SRC, "ScaleCastRules.java"), os.path.join(HERE, "ScaleMeshDump.java")]
BUILD = os.path.join(ROOT, "build", "scale_preview")
OUT = os.path.join(ROOT, "docs", "previews", "scale_casting")
pc.TEX = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp", "textures")
pc._cache.clear()
W, H = pc.W, pc.H

HOOK_X, HOOK_Y, CHAIN_LEN = 0.93, -0.10, 0.60
TEXN = {0: "entity/scalecast/gold", 1: "entity/scalecast/enamel", 2: "entity/scalecast/chain", 3: "entity/scalecast/gem", 4: "entity/scalecast/glow_runes"}
TEAL, EMBER = (0.50, 0.92, 0.88), (1.0, 0.45, 0.25)
RULES = {}
QUADS = {}


def build_dump():
    os.makedirs(BUILD, exist_ok=True)
    env = {**os.environ, "JAVA_TOOL_OPTIONS": ""}
    subprocess.run(["javac", "-nowarn", "-d", BUILD] + JAVA, check=True, capture_output=True, env=env)
    out = subprocess.run(["java", "-cp", BUILD, "ScaleMeshDump"], check=True, capture_output=True, text=True, env=env).stdout
    parts = {}
    for line in out.splitlines():
        v = line.split()
        if v[0] == "RULE":
            RULES[round(float(v[1]), 2)] = [float(x) for x in v[2:]]
            continue
        if v[0] == "QUADS":
            QUADS[int(v[1])] = int(v[2])
            continue
        name, layer = v[0], int(v[1])
        a = np.array([float(x) for x in v[2:]], np.float32)
        parts.setdefault(name, []).append((layer, a[:20].reshape(4, 5), a[20:23]))
    return parts


def rule(decades):
    """tilt, strength, damage reduction, magic reduction, boss reduction for a difference in decades (foe minus caster), from the table the Java wrote."""
    d = max(-3.0, min(3.0, decades))
    lo = math.floor(d * 4) / 4
    hi = min(3.0, lo + 0.25)
    a, b = RULES[round(lo, 2)], RULES[round(hi, 2)]
    k = 0 if hi == lo else (d - lo) / (hi - lo)
    return [x * (1 - k) + y * k for x, y in zip(a, b)]


def prims(parts, name, m, mode="solid", tint=(1, 1, 1), alpha=1.0, tex=None):
    by_layer = {}
    for layer, q, n in parts[name]:
        qq = np.concatenate([pc.xf(m, q[:, :3]), q[:, 3:]], axis=1)
        nn = m[:3, :3] @ n
        nn = nn / max(1e-9, np.linalg.norm(nn))
        by_layer.setdefault(layer, []).append((qq, nn))
    return [pc.Prim(qs, tex or TEXN[layer], mode, tint, alpha) for layer, qs in by_layer.items()]


def sfx(lod):
    return "" if lod == 0 else str(lod)


def scale_prims(parts, m, angle, load_c=0.5, load_f=0.5, lod=0, t=0.0, eye=None, cam=None, halo=True, bob=0.0):
    """The whole Scale at model matrix m (beam half length 1): the beam turned by 'angle' degrees about z, level pans hanging from its hooks, the orbs of weight, the glows."""
    ps = []
    rz = pc.RZ(angle)
    ps += prims(parts, "beam" + sfx(lod), m @ rz)
    ps += prims(parts, "column" + sfx(lod), m)
    if lod <= 1:
        ps += prims(parts, "pointer", m @ rz)
        ps += prims(parts, "gem" + sfx(lod), m, tint=(0.6, 1.1, 1.05))
    a = math.radians(angle)
    for side, load, tint in ((-1, load_c, TEAL), (1, load_f, EMBER)):
        hx = side * HOOK_X * math.cos(a) - HOOK_Y * math.sin(a)
        hy = side * HOOK_X * math.sin(a) + HOOK_Y * math.cos(a)
        pm = m @ pc.T(hx, hy - CHAIN_LEN + bob * side, 0)
        ps += prims(parts, "pan" + sfx(lod), pm)
        ps += prims(parts, "enamel" + sfx(lod), pm)
        ps += prims(parts, "chain" + sfx(lod), pm)
        if lod <= 1:
            rho = 0.075 + 0.115 * load
            ps += prims(parts, "orb" + sfx(lod), pm @ pc.T(0, -0.125 + rho * 0.85, 0) @ pc.S(rho), tint=tint)
    if lod == 0:
        ps += prims(parts, "beam", m @ rz @ pc.T(0, 0, 0.003), mode="add", alpha=0.8 + 0.1 * math.sin(t * 0.2), tex="entity/scalecast/glow_runes")
    # additive cards: the halo behind, soft glows in the pans and at the jewel
    if cam is not None and lod <= 1:
        right, up = cam
        scale = float(np.linalg.norm(m[:3, 0]))
        z = m[:3, :3] @ np.array([0, 0, 1], np.float32)
        z = z / max(1e-9, np.linalg.norm(z))
        side = 1.0 if eye is None or float((np.array(eye, np.float32) - m[:3, 3]) @ z) >= 0 else -1.0
        if halo:
            ps.append(pc.card(m[:3, 3] - z * 0.25 * scale * side, right * 1.7 * scale, up * 1.7 * scale, "entity/scalecast/glow_halo", (1, 1, 1), 0.42 + 0.08 * math.sin(t * 0.15)))
        gem_at = m[:3, :3] @ np.array([0, 0, 0.2], np.float32) + m[:3, 3]
        ps.append(pc.card(gem_at, right * 0.22 * scale, up * 0.22 * scale, "particle/glow", TEAL, 0.9))
        for side_, load, tint in ((-1, load_c, TEAL), (1, load_f, EMBER)):
            hx = side_ * HOOK_X * math.cos(a) - HOOK_Y * math.sin(a)
            hy = side_ * HOOK_X * math.sin(a) + HOOK_Y * math.cos(a)
            c = (m @ np.array([hx, hy - CHAIN_LEN + 0.03, 0, 1], np.float32))[:3]
            r = (0.30 + 0.3 * load) * scale
            ps.append(pc.card(c, right * r, up * r, "particle/glow", tint, 0.55 * load + 0.2))
    return ps


def ground(size=3.0, colour=(0.26, 0.32, 0.24), cz=0.0):
    return pc.box_prim((0, -0.05, cz), (size, 0.05, size), colour)


def ring_card(center, radius, tint, alpha, spin=0.0):
    c, s = math.cos(spin) * radius, math.sin(spin) * radius
    return pc.card(center, (c, 0, s), (-s, 0, c), "entity/scalecast/glow_ring", tint, alpha)


def ang_for(cq, fq):
    d = (fq - cq) / 20.0
    return d, -rule(d)[0]            # a heavier foe turns the beam clockwise (its pan, at +x, goes down)


def views(parts):
    shots = [("front", 0, 4), ("three-quarter", 38, 14), ("side", 85, 6), ("back", 180, 8)]
    tiles = []
    for name, yaw, pitch in shots:
        c = pc.Canvas(W, H, ((0.10, 0.13, 0.14), (0.22, 0.25, 0.26)))
        a, p = math.radians(yaw), math.radians(pitch)
        eye = (4.2 * math.sin(a) * math.cos(p), -0.2 + 4.2 * math.sin(p), 4.2 * math.cos(a) * math.cos(p))
        cam = pc.look(eye, (0, -0.35, 0), 36)
        ps = scale_prims(parts, np.eye(4, dtype=np.float32), 0.0, 0.5, 0.5, t=8, eye=eye, cam=(cam[2], cam[3]))
        c.draw(ps, cam)
        tiles.append(pc.label(c.finish(), name + " (at rest)"))
    sheet = Image.new("RGB", (W * 2, H * 2))
    for i, t in enumerate(tiles):
        sheet.paste(t, ((i % 2) * W, (i // 2) * H))
    return sheet


def weighing(parts):
    cases = [("foe heavier: 5M vs 80M", 5e6, 8e7), ("equal weight: 5M vs 5M", 5e6, 5e6), ("foe lighter: 5M vs 40K", 5e6, 4e4)]
    tiles = []
    for name, wc, wf in cases:
        cq, fq = int(round(math.log10(wc) * 20)), int(round(math.log10(wf) * 20))
        d, ang = ang_for(cq, fq)
        lc, lf = max(0.15, min(1.0, cq / max(cq, fq))), max(0.15, min(1.0, fq / max(cq, fq)))
        c = pc.Canvas(W, H, ((0.10, 0.13, 0.14), (0.22, 0.25, 0.26)))
        eye = (0.6, 0.2, 4.6)
        cam = pc.look(eye, (0, -0.4, 0), 38)
        ps = scale_prims(parts, np.eye(4, dtype=np.float32), ang, lc, lf, t=8, eye=eye, cam=(cam[2], cam[3]))
        c.draw(ps, cam)
        img = pc.label(c.finish(), name)
        dr = ImageDraw.Draw(img)
        r = rule(d)
        dr.text((6, H - 30), "beam %+.1f deg   foe suppressed %d%% (damage), %d%% (spell power)" % (ang, round(r[2] * 100), round(r[3] * 100)), fill=(255, 235, 170))
        dr.text((6, H - 16), "You %s    Foe %s" % (fmt(wc), fmt(wf)), fill=(160, 240, 230))
        tiles.append(img)
    sheet = Image.new("RGB", (W * 3, H))
    for i, t in enumerate(tiles):
        sheet.paste(t, (i * W, 0))
    return sheet


def fmt(w):
    for u, k in (("T", 1e12), ("B", 1e9), ("M", 1e6), ("K", 1e3)):
        if w >= k:
            v = w / k
            return ("%.1f%s" % (v, u)) if v < 10 else ("%d%s" % (round(v), u))
    return str(int(w))


def levels(parts):
    tiles = []
    for lod, name in ((0, "FULL"), (1, "MEDIUM"), (2, "LOW")):
        c = pc.Canvas(W, H, ((0.10, 0.13, 0.14), (0.22, 0.25, 0.26)))
        eye = (1.6, 0.3, 4.2)
        cam = pc.look(eye, (0, -0.4, 0), 38)
        ps = scale_prims(parts, np.eye(4, dtype=np.float32), -11.0, 0.45, 0.8, lod=lod, t=8, eye=eye, cam=(cam[2], cam[3]))
        c.draw(ps, cam)
        tiles.append(pc.label(c.finish(), "%s: %d quads" % (name, QUADS[lod])))
    sheet = Image.new("RGB", (W * 3, H))
    for i, t in enumerate(tiles):
        sheet.paste(t, (i * W, 0))
    return sheet


def figure_with_scale(parts, ang, lc, lf, t, eye, cam, extra=()):
    """The caster (facing +z) with the Scale at the left shoulder, front toward the caster's face."""
    left = np.array([1, 0, 0], np.float32)
    pos = np.array([0, 1.45, 0], np.float32) + left * 1.15 + np.array([0, 0, 0.2], np.float32)
    m = pc.T(*pos) @ pc.RY(-80.0) @ pc.S(0.55)
    return pc.caster_prims(0.0) + scale_prims(parts, m, ang, lc, lf, t=t, eye=eye, cam=(cam[2], cam[3])) + list(extra)


def caster(parts):
    cq, fq = 100, 112
    d, ang = ang_for(cq, fq)
    left = np.array([1, 0, 0], np.float32)
    pos = np.array([0, 1.45, 0], np.float32) + left * 1.15 + np.array([0, 0, 0.2], np.float32)
    m = pc.T(*pos) @ pc.RY(-80.0) @ pc.S(0.55)
    tiles = []
    shots = [("the caster turns to look at it", (0.0, 1.62, -0.05), (1.15, 1.35, 0.3), 62), ("from the front", (2.6, 1.9, 4.6), (0.6, 1.3, 0.0), 36), ("from behind", (2.2, 1.9, -4.6), (0.6, 1.3, 0.0), 36)]
    for name, eye, tgt, fov in shots:
        c = pc.Canvas(W, H, ((0.16, 0.2, 0.26), (0.34, 0.42, 0.34)))
        cam = pc.look(eye, tgt, fov)
        body = pc.caster_prims(0.0) if name != shots[0][0] else []
        ps = [ground()] + body + scale_prims(parts, m, ang, 0.7, 1.0, t=10, eye=eye, cam=(cam[2], cam[3]))
        c.draw(ps, cam)
        tiles.append(pc.label(c.finish(), name))
    sheet = Image.new("RGB", (W * 3, H))
    for i, t in enumerate(tiles):
        sheet.paste(t, (i * W, 0))
    return sheet


def aura(parts):
    """An aerial view: the caster with the Scale, the ring of the aura (its reach) on the ground, four foes with the pressed amounts the real curve gives them."""
    foes = [("zombie 40K", 4e4, (-5.0, 0, 6.0)), ("brute 2M", 2e6, (2.5, 0, 7.5)), ("peer 8M", 8e6, (6.0, 0, 3.5)), ("boss 300M", 3e8, (-2.0, 0, 11.0))]
    wc = 8e6
    cq = int(round(math.log10(wc) * 20))
    c = pc.Canvas(W * 2, H * 2, ((0.10, 0.13, 0.18), (0.26, 0.34, 0.30)))
    eye = (-12.0, 15.0, -9.0)
    cam = pc.look(eye, (0.0, 0.0, 5.0), 52)
    ps = [ground(16, cz=3.0)] + pc.caster_prims(0.0)
    ps.append(ring_card((0, 0.02, 0), 12.0, (1, 1, 1), 0.55, 0.2))
    for name, w, at in foes:
        fq = int(round(math.log10(w) * 20))
        d = (fq - cq) / 20.0
        boss = w >= 1e8
        r = rule(d)
        red = r[4] if boss else r[2]
        ps += pc.foe_prims(at)
        if red > 0:
            ps.append(ring_card((at[0], 0.03, at[2]), 1.6, (1, 0.75, 0.5), min(1.0, 0.45 + 2.0 * red), 0.5))
    d, ang = ang_for(cq, int(round(math.log10(8e6 * 3) * 20)))
    left = np.array([1, 0, 0], np.float32)
    pos = np.array([0, 1.45, 0], np.float32) + left * 1.15 + np.array([0, 0, 0.2], np.float32)
    m = pc.T(*pos) @ pc.RY(-80.0) @ pc.S(0.55 * 2.2)                        # drawn larger than life so it reads from the air
    ps += scale_prims(parts, m, ang, 0.8, 1.0, t=12, eye=eye, cam=(cam[2], cam[3]))
    c.draw(ps, cam)
    img = pc.label(c.finish(), "aura: the big ring is its reach (12 blocks); a ring under a foe means it is pressed (the Scale is drawn 2x larger here)")
    dr = ImageDraw.Draw(img)
    dr.text((6, 22), "caster weight %s" % fmt(wc), fill=(160, 240, 230))
    for name, w, at in foes:
        fq = int(round(math.log10(w) * 20))
        d = (fq - cq) / 20.0
        boss = w >= 1e8
        r = rule(d)
        p = pc.project(np.array([at[0], 2.4, at[2]], np.float32)[None, :], cam, W * 2 * pc.SS, H * 2 * pc.SS)[0]
        x, y = p[0] / pc.SS, p[1] / pc.SS
        dmg = round((r[4] if boss else r[2]) * 100)
        mag = round((min(0.30, r[4] * 0.85) if boss else r[3]) * 100)
        dr.rectangle([x - 62, y - 14, x + 62, y + 12], fill=(0, 0, 0))
        dr.text((x - 58, y - 12), "%s  %+.1f dec" % (name, d), fill=(160, 240, 230))
        dr.text((x - 58, y), "dmg -%d%%  magic -%d%%%s" % (dmg, mag, " (boss)" if boss else ""), fill=(255, 235, 170))
    return img


def main():
    pc._flat_tex()
    parts = build_dump()
    print({k: len(v) for k, v in parts.items()}, "quads", QUADS)
    os.makedirs(OUT, exist_ok=True)
    for name, fn in (("scale_views", views), ("scale_weighing", weighing), ("scale_levels", levels), ("scale_caster", caster), ("scale_aura", aura)):
        img = fn(parts)
        img.save(os.path.join(OUT, name + ".png"), optimize=True)
        print("wrote", os.path.join(OUT, name + ".png"))


if __name__ == "__main__":
    sys.exit(main())
