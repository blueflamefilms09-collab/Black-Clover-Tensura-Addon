"""Shared pieces of the Hell Mode summon models (docs/summoner_creatures.md): cube/bone helpers on top of gen_devil_trial, a painter with material kinds (fur, plate, stripe, shell, ring, mirror, glow, scale,
cloth, porcelain), a clip builder that only keeps the bones a model has, and the exporter (geo, animation, sheet, glow mask, Blockbench source).

A script fallback, said plainly (rule 15): Blockbench and Blender cannot run in the cloud session. The sources are written by script into the same free-model .bbmodel format and the geo files the game reads;
nothing here was made in an app. Everything is deterministic.
"""
import io
import json
import math
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
BB = os.path.dirname(HERE)
ROOT = os.path.abspath(os.path.join(BB, "..", ".."))
sys.path.insert(0, BB)
import gen_devil_trial as T  # noqa: E402
import numpy as np  # noqa: E402
from PIL import Image  # noqa: E402

ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "anime_multiverse")
GEO_DIR = os.path.join(ASSETS, "geo", "entity")
ANIM_DIR = os.path.join(ASSETS, "animations", "entity")
TEX_DIR = os.path.join(ASSETS, "textures", "entity")
BB_DIR = HERE

BONES, CUBES = T.BONES, T.CUBES
bone, cube, pair = T.bone, T.cube, T.pair
hexrgb, mix, tones = T.hexrgb, T.mix, T.tones


def reset():
    del BONES[:]
    del CUBES[:]


def C(b, name, x, y, z, w, h, d, mat, rot=None, pv=None, side=-1, paint=None):
    """A cube by its centre in x (x < 0 is the entity's right) and its low corner in y and z; w h d are the sizes."""
    cube(b, name, (x - w / 2.0, y, z), (w, h, d), mat, rot=rot, pv=pv, side=side, paint=paint)


def Cx(b, name, x0, y, z, w, h, d, mat, rot=None, pv=None, side=-1, paint=None):
    """A cube by its low x corner (right-side convention)."""
    cube(b, name, (x0, y, z), (w, h, d), mat, rot=rot, pv=pv, side=side, paint=paint)


def has(name):
    return any(b["name"] == name for b in BONES)


# ---------------------------------------------------------------- painting
def kind_of(entry):
    return entry[1] if isinstance(entry, (tuple, list)) and len(entry) > 1 else "flat"


def color_of(entry):
    return entry[0] if isinstance(entry, (tuple, list)) else entry


def paint_face(entry, face, fw, fh, seed):
    kind = kind_of(entry)
    tn = tones(hexrgb(color_of(entry)))
    img = T.shade_face(tn, face, fw, fh, seed)
    glow = np.zeros((fh, fw), np.float32)
    rs = T.rng_for(*seed, "k")
    lo, mid, hi = (np.array(t, np.float32) for t in tn)
    if kind == "fur":
        for x in range(fw):
            img[:, x] *= 0.9 + 0.16 * rs.rand()
        img[::4, :] *= 1.06
    elif kind == "plate":
        if fw > 3 and fh > 3:
            img[1, 1:-1] *= 1.18
            img[1:-1, 1] *= 1.1
            img[-2, 1:-1] *= 0.8
        if fw > 7 and fh > 7:
            for yy in (2, fh - 3):
                for xx in (2, fw - 3):
                    img[yy, xx] *= 1.3
            img[fh // 2, 3:-3] *= 0.86
    elif kind == "stripe":
        other = np.array(hexrgb(entry[2]), np.float32)
        band = max(2, int(round(fh / 6.0))) if face not in ("up", "down") else max(2, int(round(fw / 3.0)))
        for i in range(0, fh if face not in ("up", "down") else fw, band * 2):
            if face not in ("up", "down"):
                img[i:i + band, :] = img[i:i + band, :] * 0.25 + other * 0.75 * (img[i:i + band, :].mean() / max(1.0, mid.mean()))
            else:
                img[:, i:i + band] = img[:, i:i + band] * 0.25 + other * 0.75
    elif kind == "shell":
        gl = np.array(hexrgb(entry[2]), np.float32)
        step = max(3, fw // 3)
        for xx in range(0, fw, step):
            img[:, xx] *= 0.7
        for yy in range(0, fh, step):
            img[yy, :] *= 0.7
        mask = np.zeros((fh, fw), bool)
        mask[1::step, 1:-1] = True
        mask[1:-1, 1::step] = True
        if face in ("up",):
            img[mask] = img[mask] * 0.35 + gl * 0.65
            glow[mask] = 255
    elif kind == "ring":
        gl = np.array(hexrgb(entry[2]), np.float32) if len(entry) > 2 else hi
        cx, cy = (fw - 1) / 2.0, (fh - 1) / 2.0
        for yy in range(fh):
            for xx in range(fw):
                r = max(abs(xx - cx) / max(1.0, fw / 2.0), abs(yy - cy) / max(1.0, fh / 2.0))
                if face in ("north", "south") and (abs(r - 0.35) < 0.07 or abs(r - 0.72) < 0.07):
                    img[yy, xx] = img[yy, xx] * 0.5 + gl * 0.5
                elif r < 0.14 and face in ("north", "south"):
                    img[yy, xx] = img[yy, xx] * 0.3 + gl * 0.7
    elif kind == "mirror":
        gl = np.array(hexrgb(entry[2]), np.float32) if len(entry) > 2 else hi
        for yy in range(fh):
            t = ((yy / max(1.0, fh - 1.0)) + 0.0)
            img[yy, :] = img[yy, :] * (0.7 + 0.5 * (1 - t)) + gl * 0.25 * (1 - t)
        if face == "north":
            for i in range(min(fw, fh)):
                x = fw - 1 - i
                if 0 <= x < fw and i < fh and i % 5 < 2:
                    img[i, x] = img[i, x] * 0.4 + 255 * 0.6
            glow[:, :] = 70
    elif kind == "glow":
        gl = np.array(hexrgb(color_of(entry)), np.float32)
        img = img * 0.35 + gl * 0.65
        glow[:, :] = 255
    elif kind == "scale":
        for yy in range(0, fh, 3):
            off = (yy // 3) % 2 * 2
            img[yy, :] *= 0.8
            img[yy:yy + 3, off::4] *= 0.9
    elif kind == "cloth":
        img[0::2, :] *= 0.93
        img[:, 0::4] *= 0.95
    elif kind == "porcelain":
        img *= 1.0
        img[0, :] *= 0.97
    return np.clip(img, 0, 255), glow


def draw_eyes(img, glow, fw, fh, kind, pal):
    """Eyes and a mouth on a head's front face. kinds: slit, round, doll, dot, grin."""
    ec = np.array(hexrgb(pal["eye"]), np.float32)
    if kind == "slit":
        for sx in (0.22, 0.64):
            x0 = int(fw * sx)
            w = max(2, fw // 4)
            y = int(fh * 0.38)
            img[y:y + 1 + fh // 8, x0:x0 + w] = ec
            glow[y:y + 1 + fh // 8, x0:x0 + w] = 255
    elif kind in ("round", "doll"):
        for sx in (0.2, 0.6):
            x0, w, h = int(fw * sx), max(2, int(fw * 0.2)), max(2, int(fh * (0.3 if kind == "doll" else 0.24)))
            y = int(fh * 0.38)
            img[y:y + h, x0:x0 + w] = (255, 255, 255) if kind == "doll" else (20, 16, 18)
            ix = x0 + w // 4
            img[y + h // 4:y + h, ix:ix + max(1, w // 2)] = ec
            if kind == "doll":
                glow[y + h // 4:y + h, ix:ix + max(1, w // 2)] = 60
    elif kind == "dot":
        for sx in (0.22, 0.66):
            x0 = int(fw * sx)
            img[int(fh * 0.4):int(fh * 0.4) + 2, x0:x0 + 2] = (15, 12, 14)
    if pal.get("mouth") and fh > 5:
        mc = np.array(hexrgb(pal["mouth"]), np.float32)
        y = int(fh * 0.72)
        img[y:y + 1, int(fw * 0.3):int(fw * 0.7)] = mc


def paint_sheet(pal, k, pos):
    S = T.SHEET
    base = np.zeros((S, S, 4), np.float32)
    glow = np.zeros((S, S, 4), np.float32)
    gl_rgb = np.array(hexrgb(pal.get("glow", "#ffffff")), np.float32)
    for i, c in enumerate(CUBES):
        entry = pal["m"][c["mat"]]
        for face, (u, v, fw, fh) in T.face_rects(c, k, pos[i]).items():
            img, g = paint_face(entry, face, fw, fh, (c["name"], face))
            if c["paint"] and face == "north":
                draw_eyes(img, g, fw, fh, c["paint"], pal)
            rgba = np.concatenate([img, np.full((fh, fw, 1), 255, np.float32)], axis=2)
            base[v:v + fh, u:u + fw] = rgba
            gcol = np.zeros((fh, fw, 4), np.float32)
            src = np.array(hexrgb(color_of(entry)), np.float32) if kind_of(entry) == "glow" else gl_rgb
            if c["paint"] and face == "north":
                src = np.array(hexrgb(pal["eye"]), np.float32)
            gcol[..., :3] = src
            gcol[..., 3] = g
            glow[v:v + fh, u:u + fw] = gcol
    return base, glow


# ---------------------------------------------------------------- clips
def tl(rows):
    return {("%.2f" % t): [float(a), float(b), float(c)] for t, (a, b, c) in rows}


def wave(fn, length, step=0.125):
    return T.keys(fn, length, step)


class Clips:
    """Builds the four clips (idle, walk, attack, ability) for the bones present. Add motions with the helpers, call build()."""

    def __init__(self):
        self.t = {n: {} for n in ("idle", "walk", "attack", "ability")}
        self.len = {"idle": 3.2, "walk": 1.0, "attack": 0.6, "ability": 1.6}

    def rot(self, clip, b, fn=None, rows=None):
        if not has(b):
            return
        tr = self.t[clip].setdefault(b, {})
        tr["rotation"] = wave(fn, self.len[clip]) if rows is None else tl(rows)

    def pos(self, clip, b, fn=None, rows=None):
        if not has(b):
            return
        tr = self.t[clip].setdefault(b, {})
        tr["position"] = wave(fn, self.len[clip]) if rows is None else tl(rows)

    def build(self, ident):
        loop = {"idle": True, "walk": True, "attack": False, "ability": False}
        return {"animation.%s.%s" % (ident, n): {"loop": loop[n], "animation_length": self.len[n], "bones": self.t[n]} for n in self.t}


def S(period, ph=0.0):
    return lambda t: math.sin(2 * math.pi * t / period + ph)


def export(ident, pal, clips, top, width=4.0, write=True):
    k, pos = T.fit_density()
    base, glow = paint_sheet(pal, k, pos)
    geo = T.geo_doc(k, pos, ident)
    g = geo["minecraft:geometry"][0]["description"]
    g["visible_bounds_width"] = max(3.0, width)
    g["visible_bounds_height"] = max(3.0, top / 16.0 + 1.0)
    g["visible_bounds_offset"] = [0, round(top / 32.0, 2), 0]
    anims = clips.build(ident)
    if write:
        for d in (GEO_DIR, ANIM_DIR, TEX_DIR):
            os.makedirs(d, exist_ok=True)
        T.save_png(base, os.path.join(TEX_DIR, ident + ".png"))
        T.save_png(glow, os.path.join(TEX_DIR, ident + "_glowmask.png"))
        json.dump(geo, open(os.path.join(GEO_DIR, ident + ".geo.json"), "w"), indent=1)
        json.dump({"format_version": "1.8.0", "animations": anims}, open(os.path.join(ANIM_DIR, ident + ".animation.json"), "w"), indent=1)
        img = Image.fromarray((np.clip(base, 0, 255) + 0.5).astype(np.uint8), "RGBA")
        json.dump(T.bb_doc(k, pos, img, anims, name=ident), open(os.path.join(BB_DIR, ident[len("summon_"):] + ".bbmodel"), "w"), indent=1)
    stats = dict(id=ident[len("summon_"):], bones=len(BONES), cubes=len(CUBES), quads=len(CUBES) * 6, density=k, top_px=round(top, 1), height_blocks=round(top / 16.0, 2),
                 lit_texels=int((glow[..., 3] > 0).sum()))
    return stats, (base, glow), g, geo["minecraft:geometry"][0], anims
