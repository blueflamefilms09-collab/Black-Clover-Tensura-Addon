"""Measures the look of a relic from the files the game reads (no game needed): the proxy numbers of the visual upgrade.

A relic is seen in two views: "hand" (what client/relics/hand/HandForms draws: the hand catalog's signature models, else the models it wears) and "worn"
(what client/relics/worn/lib/WornLibrary draws on the body). Per view the tool reads the model files and records

    pieces, quads, vertices   the geometry of the models of the view
    passes                    render passes the models use: solid (1), emissive glow (if a quad is emissive or has a glow map), rim shell (if a quad is shell),
                              nebula/starfield (STAR quads), additive halo/fx (HALO quads)
    painted_texels            baseline: the atlas cells the quads' UVs touch x 32 x 32 (the shared atlas is 4 x 8 cells of 32 x 32 tinted by the palette);
                              upgrade: the opaque texels of the item's own skin PNG
    groups, group_kinds       idle animation groups (bob spin swing sway pulse wave orbit) and how many different kinds
    own_layers                the item's own ambient effect layers (upgrade only: the entries of its fx json)
    generic_layers            the grade layers every worn relic of that grade gets from the shared code (WornFxPlan at the default 'medium' quality, RelicFxClient,
                              RelicGradeFx), recomputed here from the same numbers

The visual score V is a weighted sum of the ratios of the five components (documented in docs/visual_upgrade/charms_genesis_worn.md):

    V = 0.35 mesh + 0.20 texture + 0.15 passes + 0.20 ambient + 0.10 animation          each component = (sum over the two views of the after value) / max(sum over the two views of the before value, 1)

so V_before is 1.00 by definition and V_after is the weighted mean ratio. The ratios are proxies: they say how much more there is, not how good it looks.
"""
import json
import os
import re
import struct
import zlib

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", "..", ".."))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp")
HAND_DIR = os.path.join(ASSETS, "relic_hand")
WORN_DIR = os.path.join(ASSETS, "relic_worn")
VU_DIR = os.path.join(ASSETS, "visual_upgrade")
VU_MODELS = os.path.join(VU_DIR, "models")
VU_FX = os.path.join(VU_DIR, "fx")
TEX_VU = os.path.join(ASSETS, "textures", "visual_upgrade")

EMIT, SHELL, HOT, GLOWMAP, STAR, HALO = 1, 2, 4, 8, 16, 32
WEIGHTS = {"mesh": 0.35, "texture": 0.20, "passes": 0.15, "ambient": 0.20, "animation": 0.10}

# the look of each slot's relic when the worn catalog has no `relic` line (mirrors Look + WornCatalog.defaultLook)
DEFAULT_LOOK = {"HEAD": "CIRCLET", "BACK": "CLOAK", "HANDS": "GAUNTLETS", "CHARM": "CHARM", "NECK": "AMULET"}
LOOK_OF = {"the_space_stone_or_the_tesseract": "ORB", "the_mind_stone": "ORB", "the_power_stone": "ORB", "the_time_stone_or_the_eye_of_agamotto": "ORB",
           "the_reality_stone": "ORB", "the_soul_stone": "ORB", "deck_of_many_things": "CHARM", "warrens_key": "CHARM", "rose_petal": "CHARM",
           "the_infinity_gauntlet": "GAUNTLETS"}


def lines_of(path):
    with open(path, encoding="utf8") as fh:
        return fh.read().splitlines()


class Catalogs:
    """The hand catalog and the worn catalog as the game parses them (Java: HandCatalog, WornCatalog), plus the visual upgrade overrides when asked."""

    def __init__(self, upgrade=False):
        self.sig, self.relic, self.look = {}, {}, {}
        for ln in lines_of(os.path.join(HAND_DIR, "catalog.txt")):
            v = ln.split()
            if len(v) >= 3 and v[0] == "sig":
                self.sig[v[1]] = v[2:]
        for ln in lines_of(os.path.join(WORN_DIR, "catalog.txt")):
            v = ln.split()
            if len(v) >= 4 and v[0] == "look":
                self.look[(v[1].upper(), v[2].upper())] = v[3:]
            elif len(v) >= 3 and v[0] == "relic":
                self.relic[v[1]] = v[2:]
        self.over_hand, self.over_worn = {}, {}
        if upgrade:
            p = os.path.join(VU_DIR, "overrides.txt")
            if os.path.exists(p):
                for ln in lines_of(p):
                    ln = ln.split("#")[0].split("|")[0].split()
                    if len(ln) >= 3 and ln[0] == "hand":
                        self.over_hand[ln[1]] = ln[2:]
                    elif len(ln) >= 3 and ln[0] == "worn":
                        self.over_worn[ln[1]] = ln[2:]

    def worn_pieces(self, rid, slot):
        if rid in self.over_worn:
            return self.over_worn[rid]
        if rid in self.relic:
            return self.relic[rid]
        look = LOOK_OF.get(rid, DEFAULT_LOOK.get(slot, ""))
        p = self.look.get((slot, look))
        if p is None:
            p = self.look.get((slot, DEFAULT_LOOK.get(slot, "")))
        return p or []

    def hand_pieces(self, rid, slot):
        if rid in self.over_hand:
            return self.over_hand[rid]
        if rid in self.sig:
            return self.sig[rid]
        return self.worn_pieces(rid, slot)


def piece_name(p):
    return p.split("@")[0].rstrip("~")


def model_path(name):
    if name.startswith("vu/"):
        return os.path.join(VU_MODELS, name[3:] + ".txt")
    for d in (HAND_DIR, WORN_DIR):
        p = os.path.join(d, name + ".txt")
        if os.path.exists(p):
            return p
    return None


def parse_model(name):
    path = model_path(name)
    if path is None:
        return None
    quads, groups, cells = 0, [], set()
    flags = {"emit": 0, "shell": 0, "glowmap": 0, "star": 0, "halo": 0}
    head = {}
    used_slots = set()
    for ln in lines_of(path):
        v = ln.split()
        if not v or v[0].startswith("#"):
            continue
        if v[0] == "grp":
            groups.append(v[3])
        elif v[0] in ("skin", "glowtex", "startex", "lod"):
            head[v[0]] = v[1]
        elif v[0] == "q":
            quads += 1
            fl = int(v[3])
            for key, bit in (("emit", EMIT), ("shell", SHELL), ("glowmap", GLOWMAP), ("star", STAR), ("halo", HALO)):
                if fl & bit:
                    flags[key] += 1
            nums = [float(x) for x in v[4:]]
            u = sum(nums[3 + 8 * k] for k in range(4)) / 4
            w = sum(nums[4 + 8 * k] for k in range(4)) / 4
            if "skin" not in head:
                col, row = min(3, max(0, int(u * 4))), min(7, max(0, int(w * 8)))
                cells.add(row * 4 + col)
    return {"name": name, "quads": quads, "vertices": quads * 4, "groups": groups, "flags": flags, "cells": cells, "head": head}


def png_opaque_texels(path):
    """Opaque (alpha > 0) texel count of a PNG, and (w, h). Uses PIL when present."""
    from PIL import Image
    import numpy as np
    im = Image.open(path).convert("RGBA")
    a = np.asarray(im)
    return int((a[..., 3] > 0).sum()), im.size


def png_lit_texels(path):
    """Texels of a glow map that carry light (max channel > 8)."""
    from PIL import Image
    import numpy as np
    a = np.asarray(Image.open(path).convert("RGBA"))
    return int(((a[..., :3].max(axis=2) > 8) & (a[..., 3] > 0)).sum())


def measure_view(pieces, own_layers=0):
    """The numbers of one view: the models of `pieces` (names with optional @anchor and ~)."""
    quads = 0
    groups, kinds = 0, set()
    flags = {"emit": 0, "shell": 0, "glowmap": 0, "star": 0, "halo": 0}
    cells = set()
    skin_texels, glow_texels = 0, 0
    skins = set()
    n = 0
    for p in pieces:
        if piece_name(p) == "none":
            continue
        m = parse_model(piece_name(p))
        if m is None:
            continue
        n += 1
        quads += m["quads"]
        groups += len(m["groups"])
        kinds.update(m["groups"])
        for k in flags:
            flags[k] += m["flags"][k]
        cells |= m["cells"]
        sk = m["head"].get("skin")
        if sk and sk not in skins:
            skins.add(sk)
            sp = os.path.join(ASSETS, "textures", sk)
            if os.path.exists(sp):
                skin_texels += png_opaque_texels(sp)[0]
            gp = m["head"].get("glowtex")
            if gp and os.path.exists(os.path.join(ASSETS, "textures", gp)):
                glow_texels += png_lit_texels(os.path.join(ASSETS, "textures", gp))
    passes = 1 + (1 if (flags["emit"] or flags["glowmap"]) else 0) + (1 if flags["shell"] else 0) + (1 if flags["star"] else 0) + (1 if flags["halo"] else 0)
    painted = skin_texels if skins else len(cells) * 32 * 32
    return {"pieces": n, "quads": quads, "vertices": quads * 4, "passes": passes, "painted_texels": painted, "glow_texels": glow_texels,
            "emissive_quads": flags["emit"] + flags["glowmap"], "shell_quads": flags["shell"], "star_quads": flags["star"], "halo_quads": flags["halo"],
            "groups": groups, "group_kinds": len(kinds), "own_layers": own_layers, "skin_pngs": sorted(skins)}


def generic_layers(grade, view):
    """The ambient layers every relic of this grade already gets from the shared code at the default 'medium' quality.
    worn: WornFxPlan(tier) rim shell, shimmer, orbiting motes, rune ring (tier 5+), the halo band (tier 7 at medium), ambient particles (RelicFxClient),
    the halo of motes over the head (tier 6+), the wearer's shimmer (RelicGradeFx, GOD+). hand: none (a held relic has only the dropped-item column, which is not a held view)."""
    if view != "worn":
        return 0
    tier = {"GOD": 6, "GENESIS": 7}[grade]
    n = 4 + 1 + 1 + 1                     # rim, shimmer, motes, rune ring; particles; head halo motes; wearer shimmer
    if tier >= 7:
        n += 1                            # the flowing halo band
    return n


def load_fx(rid):
    p = os.path.join(VU_FX, rid + ".json")
    if not os.path.exists(p):
        return None
    with open(p, encoding="utf8") as fh:
        return json.load(fh)


def measure_item(item, cats, upgrade):
    rid, _name, grade, slot = item[0], item[1], item[2], item[3]
    out = {"id": rid, "grade": grade, "slot": slot, "kind": {"CHARM": "charm", "HANDS": "hands", "HEAD": "head", "BACK": "back"}[slot]}
    fx = load_fx(rid) if upgrade else None
    for view, pieces in (("hand", cats.hand_pieces(rid, slot)), ("worn", cats.worn_pieces(rid, slot))):
        own = 0
        if fx:
            own = sum(1 for l in fx.get("layers", []) if view in l.get("views", ["hand", "worn"]))
        m = measure_view(pieces, own)
        m["pieces_named"] = [piece_name(p) for p in pieces]
        m["generic_layers"] = generic_layers(grade, view)
        m["ambient_layers"] = m["own_layers"] + m["generic_layers"]
        out[view] = m
    out["acts"] = None
    return out


def ratios(before, after):
    """Component ratios and V for one item (sum over the two views of after / max(before, 1)), V_before = 1."""
    comp = {}
    key = {"mesh": "quads", "texture": "painted_texels", "passes": "passes", "ambient": "ambient_layers", "animation": "groups"}
    for c, k in key.items():
        num = sum(after[v][k] for v in ("hand", "worn"))
        den = sum(max(before[v][k], 0) for v in ("hand", "worn"))
        comp[c] = num / max(den, 1)
    v = sum(WEIGHTS[c] * comp[c] for c in WEIGHTS)
    return comp, v
