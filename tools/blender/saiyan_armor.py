"""The Saiyan alloy armour set (craftable armour, docs/saiyan_armor.md), MADE IN BLENDER: six worn models, modelled, unwrapped and baked through the hero-weapon pipeline (bl_lib.py).

Pieces (each is one model of the worn-model library, assets/anime_multiverse/relic_worn/<id>.txt, with its own baked skin textures/entity/saiyan_alloy/<id>.png + <id>_glow.png):
    saiyan_alloy_chest      body   cuirass, pectoral plates, belly plate, lame, side guards, back plate, gorget (the chest item)
    saiyan_alloy_pauldron   rarm   the stepped shoulder pad (the chest item wears it twice, the left one mirrored by the catalog)
    saiyan_alloy_vambrace   rarm   the gloved forearm guard (same)
    saiyan_alloy_skirt      body   belt, buckle, hung front guards, split back guards, hip pads (the leggings item)
    saiyan_alloy_greave     rleg   thigh lame, shin plate, boot (the boots item, mirrored for the left leg)
    saiyan_alloy_scouter    head   the left-eye scouter (the helmet item)

Authoring frame = tools/relics/gen_worn_lib.py: Y UP, front is -Z, the player's right is -X, 1 unit = 1 model pixel = 1/16 block, relative to the pivot of the anchor part
(body: origin at the neck line, torso x -4..4, y -12..0, z -2..2; rarm: origin at the shoulder, arm x -3..1, y -10..2; rleg: origin at the hip, leg x -2..2, y -12..0; head: origin at
the neck, head y 0..8). The writer flips y into the game's layer space exactly as gen_worn_lib.write_model does.

What Blender does: the low mesh (a few hundred quads in all) is plain shapes; the surface (brushed white plate, hammered gold, cloth, steel, the lit seams and vents, the crest) is a real
shader baked with Cycles into the skin; the lit parts bake into the glow skin, drawn additive by the library's glow-map pass.

Run (Blender as a module, see docs/heroweapon/blender_pipeline.md):
    bvenv/bin/python -B tools/blender/saiyan_armor.py [--only chest,skirt] [--trial DIR] [--quick]
    --trial DIR writes into DIR instead of the mod's assets; --quick bakes at a quarter of the size (a look, not a release).
"""
import math
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import bpy  # noqa: E402,I001
from mathutils import Vector  # noqa: E402
import numpy as np  # noqa: E402
from PIL import Image  # noqa: E402

import bl_lib as bl  # noqa: E402

ml = bl.ml
ASSETS = bl.ASSETS
WORN_DIR = os.path.join(ASSETS, "relic_worn")
SKIN_DIR = os.path.join(ASSETS, "textures", "entity", "saiyan_alloy")
SKIN_REL = "entity/saiyan_alloy/"
GROW = 8 | 4        # quad flags of a quad that has lit texels: GLOWMAP (redrawn additive with the glow map) | HOT (pulses)


# ================================================================ materials
def materials():
    m = {}
    m["white"] = bl.mat("sa_white", bl.rgb(233, 236, 244), metallic=0.35, rough=0.30, bump="brushed", bscale=0.7, bstr=0.10, bevel=0.22)
    m["gold"] = bl.mat("sa_gold", bl.rgb(228, 182, 56), metallic=1.0, rough=0.27, bump="hammer", bscale=0.9, bstr=0.14, bevel=0.15,
                       groove=bl.rgb(120, 80, 14), gamount=0.8)
    m["navy"] = bl.mat("sa_navy", bl.rgb(34, 52, 128), metallic=0.30, rough=0.36, bump="brushed", bscale=0.8, bstr=0.10, bevel=0.2)
    m["suit"] = bl.mat("sa_suit", bl.rgb(34, 40, 66), rough=0.82, bump="leather", bscale=1.4, bstr=0.16, bevel=0.1, groove=bl.rgb(12, 14, 26), gamount=0.6)
    m["steel"] = bl.mat("sa_steel", bl.rgb(126, 136, 156), metallic=1.0, rough=0.34, bump="brushed", bscale=0.8, bstr=0.12, bevel=0.14)
    m["vent"] = bl.mat("sa_vent", bl.rgb(190, 238, 255), rough=0.3, emit=bl.rgb(150, 225, 255), strength=5.0, bevel=0.0)
    m["lens"] = bl.mat("sa_lens", bl.rgb(60, 255, 150), rough=0.25, emit=bl.rgb(50, 255, 140), strength=7.0, bevel=0.0)
    return m


# ================================================================ shapes
def box(name, lo, hi, material, part=None, detail=False):
    """A plain box from corner to corner (6 quads): the surface detail is the shader's and the bevel node's, not the mesh's."""
    (x0, y0, z0), (x1, y1, z1) = lo, hi
    v = [(x0, y0, z0), (x1, y0, z0), (x1, y1, z0), (x0, y1, z0), (x0, y0, z1), (x1, y0, z1), (x1, y1, z1), (x0, y1, z1)]
    f = [(0, 3, 2, 1), (4, 5, 6, 7), (0, 1, 5, 4), (2, 3, 7, 6), (1, 2, 6, 5), (0, 4, 7, 3)]
    return bl.obj(name, v, f, material, part or name, detail)


def shape(name, rings, material, part=None, r=0.5, caps=(True, True), detail=False):
    """A solid skinned through rounded-rectangle rings [(y, half width, half depth, centre x, centre z)] (8 points a ring)."""
    rs = []
    for (y, w, t, ox, oz) in rings:
        rr = max(0.02, min(r, w * 0.9, t * 0.9))
        rs.append(bl.ring(y, bl.rrect(w, t, rr, 1), 1.0, 1.0, ox, oz))
    return bl.loft(name, rs, material, caps, part or name, detail=detail)


def slab(name, y0, y1, w, t, ox, oz, material, r=0.5, part=None, detail=False):
    """A prism of constant section between two heights."""
    return shape(name, [(y1, w, t, ox, oz), (y0, w, t, ox, oz)], material, part, r, detail=detail)


# ================================================================ the pieces
def chest(M):
    slab("suit", -12.3, -0.2, 4.15, 2.15, 0, 0, M["suit"], r=0.9)                                       # the undersuit over the torso
    for s in (-1, 1):                                                                                  # the two pectoral plates, tilted out at the top
        shape("pec%d" % s, [(-1.7, 2.20, 0.55, s * 2.35, -2.35), (-3.2, 2.30, 0.80, s * 2.35, -2.52), (-5.2, 2.20, 0.75, s * 2.35, -2.50), (-6.3, 2.05, 0.55, s * 2.35, -2.35)],
              M["white"], "pec", r=0.7)
    slab("belly", -9.0, -6.5, 4.40, 0.55, 0, -2.52, M["white"], r=0.4)                                 # the belly plate
    slab("lame", -12.0, -9.1, 4.30, 0.50, 0, -2.45, M["navy"], r=0.4)                                  # the navy lower lame
    slab("band", -6.9, -6.0, 4.62, 2.50, 0, 0, M["gold"], r=0.8)                                       # the gold line that wraps the torso between plate and lame
    slab("rim", -1.2, -0.3, 4.70, 2.55, 0, 0, M["gold"], r=0.9)                                        # the gold rim along the top of the chest
    for s in (-1, 1):                                                                                  # side guards
        slab("side%d" % s, -9.6, -2.4, 0.42, 1.85, s * 4.45, 0, M["white"], r=0.2, part="side")
    slab("back", -11.4, -1.6, 4.35, 0.50, 0, 2.58, M["white"], r=0.4)                                  # the back plate
    slab("backlame", -11.8, -10.0, 4.20, 0.45, 0, 2.52, M["navy"], r=0.4)                              # the back lame
    slab("spine", -10.6, -2.2, 0.32, 0.30, 0, 3.10, M["gold"], r=0.1)                                  # the gold spine band
    slab("gorget", -1.7, -0.4, 3.75, 2.55, 0, 0, M["white"], r=0.9)                                    # the collar standing on the shoulders ...
    slab("lip", -0.45, 0.15, 3.80, 2.60, 0, 0, M["gold"], r=0.9)                                       # ... and its gold lip
    # the baked-only detail (no mesh in the game): the lit seams, the cleft, the crest, the engraved lines
    box("cleft", (-0.18, -6.0, -3.07), (0.18, -1.9, -2.88), M["vent"], detail=True)
    for i, (w, y) in enumerate(((0.6, -3.3), (1.2, -4.1), (0.6, -4.9))):                                  # the crest: a stepped diamond, three rows
        box("crest%d" % i, (-w, y - 0.4, -3.05), (w, y + 0.4, -2.84), M["gold"], detail=True)
    for s in (-1, 1):
        box("pecseam%d" % s, (s * 2.35 - 1.7, -6.25, -2.95), (s * 2.35 + 1.7, -6.1, -2.83), M["vent"], detail=True)
        box("pecline%d" % s, (s * 0.35 + (-0.08), -3.0, -2.97), (s * 0.35 + 0.08, -1.9, -2.85), M["gold"], detail=True)
    box("bellyseam", (-4.0, -7.7, -3.04), (4.0, -7.55, -2.98), M["gold"], detail=True)
    box("spinevent", (-0.14, -9.6, 3.22), (0.14, -3.0, 3.34), M["vent"], detail=True)
    for s in (-1, 1):
        box("sideslot%d" % s, (s * 4.68 - 0.05, -7.4, -0.6), (s * 4.68 + 0.05, -3.6, 0.6), M["vent"], detail=True)


def skirt(M):
    slab("belt", -13.1, -11.5, 4.55, 2.55, 0, 0, M["navy"], r=0.9)                                     # the belt
    box("buckle", (-1.15, -13.0, -3.0), (1.15, -11.6, -2.45), M["gold"])                                # the buckle
    for s in (-1, 1):                                                                                  # front guards hung from the belt
        slab("front%d" % s, -17.2, -13.0, 1.95, 0.5, s * 2.3, -2.95, M["gold"], r=0.3, part="front")
        slab("fronthem%d" % s, -17.7, -17.0, 1.95, 0.55, s * 2.3, -3.0, M["white"], r=0.3, part="hem")
        slab("back%d" % s, -17.0, -13.0, 1.95, 0.5, s * 2.9, 2.95, M["gold"], r=0.3, part="backguard")      # the back guards: two halves, the tail-slot notch between them
        slab("backhem%d" % s, -17.5, -16.8, 1.95, 0.55, s * 2.9, 3.0, M["white"], r=0.3, part="hem")
        slab("post%d" % s, -17.0, -13.0, 0.2, 0.5, s * 0.8, 2.9, M["steel"], r=0.08, part="post")
        slab("hip%d" % s, -16.8, -13.2, 0.45, 1.8, s * 5.0, 0, M["white"], r=0.2, part="hip")            # the hip pads
    box("stud", (-0.45, -12.6, -3.18), (0.45, -11.9, -2.98), M["vent"], detail=True)                    # the lit stud on the buckle
    for s in (-1, 1):
        box("frontseam%d" % s, (s * 2.3 - 1.4, -15.4, -3.5), (s * 2.3 + 1.4, -15.28, -3.42), M["white"], detail=True)
        box("backseam%d" % s, (s * 2.9 - 1.4, -15.2, 3.42), (s * 2.9 + 1.4, -15.08, 3.5), M["white"], detail=True)
        box("hipvent%d" % s, (s * 5.26 - 0.04, -15.5, -0.5), (s * 5.26 + 0.04, -14.2, 0.5), M["vent"], detail=True)


def pauldron(M):
    # a right-arm pad: x_rel = x_world + 5 (the pad reaches out to -5.4 and hangs 5 px forward and back of the arm)
    slab("sleeve", -4.5, 2.25, 2.25, 2.25, -1.0, 0, M["suit"], r=0.9, part="sleeve")                    # the sleeve over the upper arm
    slab("under", -1.4, -0.3, 2.60, 3.2, -2.3, 0, M["navy"], r=0.5, part="under")                       # the navy underside
    steps = [(-0.3, 0.8, 3.0, 3.9), (0.8, 1.9, 2.6, 3.5), (1.9, 2.9, 2.0, 3.0), (2.9, 3.7, 1.2, 2.5)]    # four steps, widest first
    for i, (y0, y1, w, t) in enumerate(steps):
        slab("step%d" % i, y0, y1, w, t, -2.2, 0, M["white"], r=0.55, part="step")
    slab("edge", -0.3, 0.8, 0.25, 3.95, -5.35, 0, M["gold"], r=0.1, part="edge")                        # the gold rim: outer edge ...
    slab("front", -0.3, 0.8, 3.0, 0.25, -2.2, -3.95, M["gold"], r=0.1, part="edge")                     # ... and front edge
    slab("ridge", 3.55, 4.15, 0.40, 2.4, -2.2, 0, M["gold"], r=0.1, part="ridge")                       # the raised ridge
    box("seam", (-2.4, 2.9, -2.65), (-2.0, 3.0, 2.65), M["vent"], detail=True)                          # its lit seam
    for k in range(5):                                                                                 # engraved lines on the second step
        box("eng%d" % k, (-4.9 + k * 0.5, 0.82, -3.2), (-4.85 + k * 0.5, 0.9, 3.2), M["gold"], detail=True)


def vambrace(M):
    slab("sleeve", -10.0, -3.6, 2.25, 2.25, -1.0, 0, M["suit"], r=0.9, part="sleeve")                   # the sleeve over forearm and hand
    slab("glove", -10.4, -7.0, 2.50, 2.50, -1.0, 0, M["white"], r=0.9, part="glove")                    # the glove
    slab("band1", -7.0, -6.1, 2.65, 2.65, -1.0, 0, M["gold"], r=0.9, part="band")                       # first gold band at the wrist
    slab("guard", -6.1, -4.1, 2.55, 2.55, -1.0, 0, M["white"], r=0.9, part="guard")                     # the forearm guard
    slab("band2", -4.1, -3.5, 2.70, 2.70, -1.0, 0, M["gold"], r=0.9, part="band")                       # second gold band
    box("vent", (-3.9, -5.7, -0.8), (-3.7, -4.5, 0.8), M["vent"], detail=True)                          # the lit vent on the outer side
    for k in range(3):
        box("knuckle%d" % k, (-2.3 + k * 1.2, -9.7, -3.6), (-1.5 + k * 1.2, -9.5, -3.45), M["steel"], detail=True)


def greave(M):
    slab("leg", -10.4, 0.0, 2.0, 2.0, -0.1, 0, M["suit"], r=0.8, part="leg")                            # the leg of the undersuit
    slab("thigh", -4.1, -1.3, 2.55, 2.55, -0.1, 0, M["navy"], r=0.9, part="thigh")                      # the thigh lame
    slab("trim", -1.3, -0.8, 2.65, 2.65, -0.1, 0, M["gold"], r=0.9, part="trim")                        # its gold trim
    slab("shin", -6.7, -3.3, 2.45, 0.46, -0.1, -2.5, M["white"], r=0.25, part="shin")                   # the shin plate
    slab("band", -7.9, -6.8, 2.70, 2.70, -0.1, 0, M["gold"], r=0.9, part="band")                        # the gold band at the boot top
    slab("boot", -12.0, -7.9, 2.55, 2.60, -0.1, 0, M["white"], r=0.9, part="boot")                      # the boot
    slab("toe", -12.0, -10.0, 2.40, 0.65, -0.1, -3.25, M["white"], r=0.3, part="toe")                   # the toe cap
    box("heelvent", (-1.1, -11.4, 2.58), (0.9, -10.9, 2.7), M["vent"], detail=True)                     # the lit heel vent
    box("shinseam", (-1.9, -5.0, -3.0), (1.7, -4.9, -2.9), M["vent"], detail=True)
    box("toeline", (-2.3, -10.05, -3.9), (2.1, -9.95, -3.8), M["gold"], detail=True)


def scouter(M):
    # the left eye is +x; the head box is x -4..4, y 0..8, z -4..4, the front is -z
    box("ear", (4.2, 1.4, -1.4), (5.9, 4.6, 1.4), M["steel"])                                           # the ear piece
    box("boom", (4.2, 3.2, -4.8), (5.2, 4.0, -1.2), M["steel"])                                         # the boom along the cheek
    box("outer", (3.5, 1.2, -5.7), (4.5, 4.9, -4.2), M["steel"])                                        # the frame round the left eye: outer bar
    box("inner", (0.4, 1.2, -5.7), (1.2, 4.9, -4.2), M["steel"])                                        # inner bar
    box("topbar", (0.4, 4.9, -5.7), (4.5, 5.6, -4.2), M["steel"])                                       # top bar
    box("botbar", (0.4, 0.6, -5.7), (4.5, 1.2, -4.2), M["steel"])                                       # bottom bar
    box("lens", (1.2, 1.2, -5.2), (3.5, 4.9, -4.55), M["lens"])                                         # the glowing lens
    box("antenna", (4.9, 4.6, 0.2), (5.4, 6.6, 0.8), M["steel"])                                        # antenna
    box("tip", (4.8, 6.6, 0.1), (5.5, 7.3, 0.9), M["vent"])                                             # its lit tip
    box("scan", (1.3, 3.0, -5.45), (3.4, 3.12, -5.2), M["vent"], detail=True)                           # a scan line across the lens
    box("rivet1", (3.6, 4.3, -5.9), (3.9, 4.6, -5.6), M["gold"], detail=True)
    box("rivet2", (0.5, 4.3, -5.9), (0.8, 4.6, -5.6), M["gold"], detail=True)


# id: (anchor, builder, skin size, bake size, quad budget)
PIECES = {
    "saiyan_alloy_chest": ("body", chest, 512, 1024, 700),
    "saiyan_alloy_pauldron": ("rarm", pauldron, 256, 512, 350),
    "saiyan_alloy_vambrace": ("rarm", vambrace, 256, 512, 250),
    "saiyan_alloy_skirt": ("body", skirt, 512, 1024, 500),
    "saiyan_alloy_greave": ("rleg", greave, 256, 512, 350),
    "saiyan_alloy_scouter": ("head", scouter, 256, 512, 250),
}


# ================================================================ the worn model file
def write_worn(mesh, atlas, item_id, anchor, out_dir, skin_dir):
    """The mod's worn-model file (tools/relics/gen_worn_lib.write_model's record: y flipped into layer space, the winding reversed), with the skin and glow records and a GLOWMAP flag on the quads that touch lit texels."""
    S = atlas.img.shape[0]
    glow_a = atlas.glow[..., 3]
    recs, lit = [], 0
    for p in mesh.parts:
        for f, idx in enumerate(p.F):
            uvs = p.UV[f]
            u = np.array([c[0] for c in uvs]) / ml.TS
            v = np.array([c[1] for c in uvs]) / ml.TS
            x0, x1 = int(max(0, math.floor(u.min() * S))), int(min(S, math.ceil(u.max() * S) + 1))
            y0, y1 = int(max(0, math.floor(v.min() * S))), int(min(S, math.ceil(v.max() * S) + 1))
            flags = GROW if x1 > x0 and y1 > y0 and glow_a[y0:y1, x0:x1].max() > 40 else 0
            lit += 1 if flags else 0
            rec = []
            for k in range(4):
                P, N = p.V[idx[k]], p.N[f][k]
                rec.append((float(P[0]), -float(P[1]), float(P[2]), float(uvs[k][0]) / ml.TS, float(uvs[k][1]) / ml.TS, float(N[0]), -float(N[1]), float(N[2])))
            recs.append((flags, [rec[0], rec[3], rec[2], rec[1]]))
    lines = ["# worn model %s: %d quads (%d lit), %d vertices; made in Blender by tools/blender/saiyan_armor.py (bl_lib.py), do not edit" % (item_id, len(recs), lit, 4 * len(recs)),
             "model " + item_id, "anchor " + anchor, "skin %s%s.png" % (SKIN_REL, item_id), "glowtex %s%s_glow.png" % (SKIN_REL, item_id)]
    for flags, rec in recs:
        lines.append("q 0 0 %d %s" % (flags, " ".join("%.3f %.3f %.3f %.4f %.4f %.3f %.3f %.3f" % r for r in rec)))
    os.makedirs(out_dir, exist_ok=True)
    with open(os.path.join(out_dir, item_id + ".txt"), "w", newline="\n") as fh:
        fh.write("\n".join(lines) + "\n")
    os.makedirs(skin_dir, exist_ok=True)
    Image.fromarray(np.clip(atlas.img, 0, 255).astype(np.uint8), "RGBA").save(os.path.join(skin_dir, item_id + ".png"), optimize=True)
    Image.fromarray(np.clip(atlas.glow, 0, 255).astype(np.uint8), "RGBA").save(os.path.join(skin_dir, item_id + "_glow.png"), optimize=True)
    return len(recs), lit


def main():
    only = None
    trial = None
    quick = "--quick" in sys.argv
    if "--only" in sys.argv:
        only = {("saiyan_alloy_" + s.strip().replace("saiyan_alloy_", "")) for s in sys.argv[sys.argv.index("--only") + 1].split(",")}
    if "--trial" in sys.argv:
        trial = os.path.abspath(sys.argv[sys.argv.index("--trial") + 1])
    for item_id, (anchor, build, skin, bake, budget) in PIECES.items():
        if only and item_id not in only:
            continue
        bl.reset(samples=12 if quick else 24)
        build(materials())
        div = 4 if quick else 1
        mesh, atlas, stats = bl.finish(item_id, write=False, skin=skin // div, bake=bake // div, budget=budget)
        out_dir = trial or WORN_DIR
        skin_dir = os.path.join(trial, "skins") if trial else SKIN_DIR
        n, lit = write_worn(mesh, atlas, item_id, anchor, out_dir, skin_dir)
        print("WROTE %s: %d quads, %d lit" % (item_id, n, lit))


if __name__ == "__main__":
    main()
