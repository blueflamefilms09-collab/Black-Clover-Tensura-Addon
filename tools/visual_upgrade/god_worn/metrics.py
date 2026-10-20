"""Measures a worn relic's visuals (before and after the 1.102.0 upgrade) and scores them: the proxies the owner's "5x better looking" is made measurable with.

Everything is counted from the files the game reads, not guessed:
  quads / vertices   the model files of the pieces the relic wears (assets/nusmp/relic_worn/<model>.txt; a mirrored copy on the other arm or leg counts again, as it is drawn again)
  pieces             how many model files are drawn (pairs count twice)
  glow passes        the draw passes beyond the lit solid pass that THIS relic's models take part in: the emissive model pass, the glow-map pass (after), the rim and
                     the shimmer pass (when the models have shell quads). The procedural motes / rune ring / halo of the grade plan are the same for every God relic before
                     and after, so they are listed (`shared_grade_plan`) but not counted, for either state
  skin               the texture size the relic samples and the PAINTED TEXELS it uses: the opaque texels of the atlas cells its quads sample (old: 32 x 32 cells of
                     the shared 128 x 256 atlas; new: 64 x 64 cells of its own skin), plus the painted armour layer (64 x 32) a chest relic keeps; glow-map texels are
                     counted separately (`glow_map_texels`: non-black texels of the emissive map)
  ambient layers     the effect layers THIS relic brings while it is merely worn: every emissive model group that moves on its own (orbit, spin, pulse, bob), a
                     particle theme the catalog names for it (`fx <id> <theme>`), and, after, every layer of its data table (assets/nusmp/visual_upgrade/<id>.json).
                     The grade plan (motes, rune ring, halo band, the generic ambient particles, the head particle halo) is shared by every God relic and unchanged: it is
                     listed under `shared_grade_plan` and not counted, for either state
  animation groups   the animation groups of the pieces (swing, sway, wave, bob, spin, orbit, pulse)
  act vfx            acts that have a visual of their own (before: none, the acts only apply their effects; after: the acts of the data table)
Score: V = 0.35 mesh + 0.20 texture + 0.15 glow + 0.20 ambient + 0.10 animation, each term the count over a fixed reference (REFS), so V = 1 means "about one of everything
the God brief asks for". The weights and references are choices of this slice, written down here and in docs/visual_upgrade/god_worn.md; the ratio V_after / V_before is the
number reported, never a claim about how it looks.
"""
import json
import os

import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", "..", ".."))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp")
WORN = os.path.join(ASSETS, "relic_worn")
HAND = os.path.join(ASSETS, "relic_hand")
UPG = os.path.join(ASSETS, "visual_upgrade")
OLD_ATLAS = os.path.join(ASSETS, "textures", "relic_worn", "atlas.png")

WEIGHTS = {"mesh": 0.35, "texture": 0.20, "glow": 0.15, "ambient": 0.20, "animation": 0.10}
# the references are the God floors of the brief (900 quads for a worn piece set, >= 3 glow passes, >= 3 ambient layers, >= 3 animated groups) and four times the median
# painted texel count of the old models (5120 x 4 = 20480): a score of 1.0 per term means "meets the floor"
REFS = {"mesh": 900.0, "texture": 20480.0, "glow": 3.0, "ambient": 3.0, "animation": 3.0}

EMIT, SHELL, HOT, LIT, ADD = 1, 2, 4, 64, 128      # the bits of WornMesh.java (8, 16, 32 are the Charms / Genesis flags)
MOVING = ("orbit", "spin", "pulse", "bob")

# painted armour layer of the two chest relics (64 x 32 ARGB, opaque pixels), counted by running ArmorPainter.paint for the def (tools/visual_upgrade/god_worn/README in
# docs/visual_upgrade/god_worn.md says how): the head and feet relics have a 3D model, so their armour box is left clear (RelicTextures: blank)
ARMOUR_PAINTED = {"berserker_armor_shard": 576, "orsteds_dragon_god_armor": 576}
ARMOUR_GLOW_PAINTED = {"berserker_armor_shard": 25, "orsteds_dragon_god_armor": 25}


def read_catalog(path=None):
    path = path or os.path.join(WORN, "catalog.txt")
    relics, looks = {}, {}
    with open(path, encoding="utf8") as fh:
        for line in fh:
            v = line.split()
            if not v or v[0].startswith("#"):
                continue
            if v[0] == "relic":
                relics[v[1]] = v[2:]
            elif v[0] == "look":
                looks[(v[1], v[2])] = v[3:]
    return relics, looks


def piece_name(p):
    return p.split("@")[0].rstrip("~")


def model_path(name):
    for d in (WORN, HAND):
        p = os.path.join(d, name + ".txt")
        if os.path.exists(p):
            return p
    raise FileNotFoundError(name)


def model_stats(name, atlas_cell=32, atlas_w=128, atlas_h=256):
    """Counts of one model file: quads, emissive / lit / shell quads, animation group kinds, the atlas cells it samples."""
    quads = emit = lit = shell = 0
    groups, emit_groups = {}, set()
    cells = set()
    skin = None
    with open(model_path(name), encoding="utf8") as fh:
        for line in fh:
            v = line.split()
            if not v or v[0].startswith("#"):
                continue
            if v[0] == "skin":
                skin = v[1]
            elif v[0] == "grp":
                groups[int(v[1])] = v[3]
            elif v[0] == "q":
                g, flags = int(v[1]), int(v[3])
                quads += 1
                if flags & (EMIT | ADD):
                    emit += 1
                if flags & LIT:
                    lit += 1
                if flags & SHELL:
                    shell += 1
                if flags & (EMIT | LIT | ADD) and g:
                    emit_groups.add(g)
                u = sum(float(v[4 + k * 8 + 3]) for k in range(4)) / 4.0
                w = sum(float(v[4 + k * 8 + 4]) for k in range(4)) / 4.0
                cells.add((int(u * atlas_w) // atlas_cell, int(w * atlas_h) // atlas_cell))
    return {"quads": quads, "emit": emit, "lit": lit, "shell": shell, "groups": groups, "emit_groups": emit_groups, "cells": cells, "skin": skin}


def old_atlas_texels(cells):
    a = np.array(Image.open(OLD_ATLAS).convert("RGBA"))
    n = 0
    for cx, cy in cells:
        n += int((a[cy * 32:(cy + 1) * 32, cx * 32:(cx + 1) * 32, 3] > 0).sum())
    return n


def plan_layers(slot):
    """The grade plan of a GOD relic at the HIGH client quality (WornFxPlan.of(6, HIGH, near)): motes, rune ring, halo; plus the ambient particles; a head relic adds the particle halo.
    Shared by every God relic, before and after: reported, not scored."""
    layers = ["plan:motes(5)", "plan:rune_ring", "plan:halo_band", "particles:ambient_theme"]
    if slot == "HEAD":
        layers.append("particles:head_halo")
    return layers


def catalog_themes(path=None):
    path = path or os.path.join(WORN, "catalog.txt")
    out = {}
    with open(path, encoding="utf8") as fh:
        for line in fh:
            v = line.split()
            if len(v) >= 3 and v[0] == "fx":
                out[v[1]] = v[2]
    return out


def score(m):
    t = {
        "mesh": m["quads"] / REFS["mesh"],
        "texture": m["painted_texels"] / REFS["texture"],
        "glow": m["glow_passes"] / REFS["glow"],
        "ambient": m["ambient_layers"] / REFS["ambient"],
        "animation": m["animation_groups"] / REFS["animation"],
    }
    v = sum(WEIGHTS[k] * t[k] for k in WEIGHTS)
    return round(v, 4), {k: round(x, 4) for k, x in t.items()}


def measure_old(rid, slot, look, catalog=None):
    """The state before the upgrade: the relic's own `relic` entry of catalog.txt (the last one written), else the models of its look."""
    relics, looks = catalog or read_catalog()
    pieces = relics.get(rid) or looks.get((slot, look)) or looks[(slot, {"HEAD": "CIRCLET", "CHEST": "PLATE", "NECK": "AMULET", "BACK": "CLOAK", "BELT": "BELT", "HANDS": "GAUNTLETS", "FEET": "BOOTS"}[slot])]
    quads = emit = shell = 0
    group_kinds, ambient = [], []
    theme = catalog_themes().get(rid)
    if theme:
        ambient.append("particles:theme_" + theme)
    cells = set()
    for p in pieces:
        s = model_stats(piece_name(p))
        quads += s["quads"]
        emit += s["emit"]
        shell += s["shell"]
        group_kinds += [s["groups"][g] for g in sorted(s["groups"])]
        ambient += ["model:%s:%s" % (piece_name(p), s["groups"][g]) for g in sorted(s["emit_groups"]) if s["groups"][g] in MOVING]
        cells |= s["cells"]
    passes = (["emissive_model"] if emit else []) + (["shell_rim", "shimmer"] if shell else [])
    texels = old_atlas_texels(cells) + ARMOUR_PAINTED.get(rid, 0)
    m = {
        "pieces": len(pieces), "piece_files": [piece_name(p) for p in pieces],
        "quads": quads, "vertices": quads * 4,
        "glow_passes": len(passes), "glow_pass_names": passes,
        "skin": {"texture": "textures/relic_worn/atlas.png (shared)", "size": [128, 256], "texels_total": 128 * 256, "cells_used": len(cells)},
        "painted_texels": texels, "armour_layer_texels": ARMOUR_PAINTED.get(rid, 0),
        "glow_map_texels": ARMOUR_GLOW_PAINTED.get(rid, 0),
        "ambient_layers": len(ambient), "ambient_layer_names": ambient, "shared_grade_plan": plan_layers(slot),
        "animation_groups": len(group_kinds), "animation_kinds": sorted(set(group_kinds)),
        "act_vfx": 0, "swing_trail": False,
    }
    m["V"], m["V_terms"] = score(m)
    return m


def load_json(path):
    with open(path, encoding="utf8") as fh:
        return json.load(fh)
