"""crossbow_states: the worked example of tools/relics/ranged_meshes.py on a BAKED mesh: the state meshes of the two crossbows the mod already had, the
Bolt Thrower and the Crossbow of Swift Death (assets/nusmp/weapon_meshes/bolt_thrower.txt, crossbow_of_swift_death.txt, built by tools/gen_weapon_models.py).

Those two are self-loading (item/BoltArts: a right-click looses a bolt, no ammunition, then the item cooldown runs), so their states are not a draw:

    <id>.txt          the old mesh, UNCHANGED: limbs drawn, string at the latch, bolt on the rail. Still used where nobody holds the item (ground, frames).
    <id>_charged.txt  READY (no cooldown running): the old mesh with the weapon's glow charged up (faster, brighter pulse).   [new file]
    <id>_spent.txt    JUST FIRED (cooldown running): the bolt is gone, the limbs have relaxed forward and the string is slack: a straight line tip to tip.   [new file]

client/relics/item/ItemStateVariants picks them (CHARGED when ready, SPENT while Player.getCooldowns().isOnCooldown), so the crossbow visibly "reloads".
Nothing here changes an existing file. The old mesh is the full-draw state (draw 1.0), so the relaxed state is the same displacement maths run backwards
(ranged_meshes.displace_limb(rig, P, f_from=1, f_to=0)).

How the old mesh's parts are found (it carries no part names; quads that share a vertex form one piece, then pieces are told apart by their bounding box):
  prod (limbs)     two flat 45-cm faces at z = +-1 whose silhouette is cut from the texture (alpha) -> cut into strips and sheared; plus the 40/72-quad outline between them
  string           two flat faces at z ~ 2.4 / 2.75 with the V cut from the texture, plus their outline -> removed and rebuilt as a straight 6-quad strip
  bolt             the pieces inside the rail's box (x +-2.6, y 9..38.6, z 2.2..3.6) that are not the rail guides or the latch
Run:  python tools/relics/crossbow_states.py [--preview out.png]     (idempotent; rewrites the four state files)
"""
import os
import sys

import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import ranged_meshes as rm  # noqa: E402

SPECS = {
    # span / back / bow: the _xb_prod arguments of tools/gen_weapon_models.py (S["span"], S["back"], the default bow exponent 1.7)
    "bolt_thrower": dict(span=19.5, back=6.0, bow=1.7, latch_y=8.5, string_xext=30.0, string_z=(2.1, 2.8), string_w=1.0,
                         prod_xext=38.0, prod_y=(10.0, 28.5),
                         bolt_box=((-2.6, 8.9, 1.9), (2.6, 35.6, 3.4)),
                         glow_charged=("78E6FF", 0.20, 0.70, 1.0), glow_spent=("78E6FF", 0.07, 0.20, 0.55)),
    "crossbow_of_swift_death": dict(span=22.5, back=5.5, bow=1.7, latch_y=8.0, string_xext=30.0, string_z=(2.4, 3.1), string_w=1.0,
                                    prod_xext=40.0, prod_y=(10.0, 28.5),
                                    bolt_box=((-2.6, 9.0, 2.2), (2.6, 38.6, 3.6)),
                                    glow_charged=("FF3A54", 0.16, 0.60, 1.0), glow_spent=("FF3A54", 0.06, 0.18, 0.5)),
}


def classify(spec):
    """roles function for ranged_meshes.baked_states."""
    def roles(quads):
        out = {}
        for comp in rm.components(quads):
            lo, hi = rm.bbox_of(quads, comp)
            xext = hi[0] - lo[0]
            if xext > spec["string_xext"] and lo[2] >= spec["string_z"][0] and hi[2] <= spec["string_z"][1]:
                for i in comp:
                    out[i] = "string"
            elif xext > spec["prod_xext"] and max(abs(lo[2]), abs(hi[2])) <= 1.06 and lo[1] >= spec["prod_y"][0] and hi[1] <= spec["prod_y"][1]:
                for i in comp:
                    out[i] = "limbface" if len(comp) <= 2 else "limb"
            else:
                b0, b1 = spec["bolt_box"]
                if all(lo[k] >= b0[k] - 1e-6 and hi[k] <= b1[k] + 1e-6 for k in range(3)) and xext < 5.5:
                    for i in comp:
                        out[i] = "bolt"
        return out
    return roles


def rig_for(spec, quads, roles):
    """The Rig of the drawn mesh: tips measured on the string, the flex of the limbs from the generator's numbers."""
    idx = [i for i, r in roles.items() if r == "string" and np.ptp(quads[i]["P"][:, 0]) < 10.0]      # the outline strip (the two big faces would blur the ends)
    pts = np.concatenate([quads[i]["P"] for i in idx])
    xmax = float(np.abs(pts[:, 0]).max())
    tip_y = float(pts[np.abs(pts[:, 0]) > xmax - 1.0][:, 1].mean())                       # the string stroke's end, centre of its width
    apex_y = float(pts[np.abs(pts[:, 0]) < 2.0][:, 1].mean())
    z = float(pts[:, 2].mean())
    tip_x = spec["span"] - 0.6                                                              # _xb_core: tip_x = span - 0.6
    drawn_a, drawn_b = np.array([-tip_x, tip_y, z]), np.array([tip_x, tip_y, z])
    rig_flex = spec["back"]
    # the limb weights are about the grip at the middle of the stock, the limbs run along x; tips at rest are `back` further forward (+y)
    return rm.Rig(tip_a=tuple(drawn_a + (0, rig_flex, 0)), tip_b=tuple(drawn_b + (0, rig_flex, 0)), draw_dir=(0.0, -1.0, 0.0),
                  max_draw=tip_y - apex_y, flex=rig_flex, grip=(0.0, tip_y, z), limb_axis=(1.0, 0.0, 0.0), limb_span=spec["span"],
                  flex_power=spec["bow"]), drawn_a, drawn_b, (apex_y, z)


def straight_string(rig, drawn_a, drawn_b, z, width, thick, tails):
    """A clean 6-quad strip from the relaxed tip a to the relaxed tip b (the limbs' own displacement decides where the tips are), every vertex on
    one texel of the old string's skin."""
    a = rm.displace_limb(rig, drawn_a, 1.0, 0.0)[0]
    b = rm.displace_limb(rig, drawn_b, 1.0, 0.0)[0]
    hw, ht = width / 2, thick / 2
    # corners of the strip's two ends: (y, z) = (-hw,-ht) (+hw,-ht) (+hw,+ht) (-hw,+ht) around the tip centre line
    ring = lambda p: [p + np.array([0, -hw, -ht]), p + np.array([0, hw, -ht]), p + np.array([0, hw, ht]), p + np.array([0, -hw, ht])]
    ra, rb = ring(a), ring(b)
    quads = []

    def quad(P, n):
        return {"layer": 0, "P": np.array(P), "tails": [list(tails)] * 4, "kind": "q", "normal": np.array(n, float)}
    # counter-clockwise seen from outside: the long faces (along x) and the two end caps
    quads.append(quad([ra[0], rb[0], rb[1], ra[1]], (0, 0, -1)))        # back (-z)
    quads.append(quad([ra[3], ra[2], rb[2], rb[3]], (0, 0, 1)))         # front (+z)
    quads.append(quad([ra[1], rb[1], rb[2], ra[2]], (0, 1, 0)))         # top (+y)
    quads.append(quad([ra[0], ra[3], rb[3], rb[0]], (0, -1, 0)))        # bottom (-y)
    quads.append(quad([ra[0], ra[1], ra[2], ra[3]], (-1, 0, 0)))        # end a
    quads.append(quad([rb[0], rb[3], rb[2], rb[1]], (1, 0, 0)))         # end b
    for q in quads:                                                      # make every winding agree with its normal
        if float(rm.quad_normal(q["P"]) @ q["normal"]) < 0:
            q["P"] = q["P"][::-1]
    return quads


def build(item_id, out_dir=rm.MESH_DIR):
    spec = SPECS[item_id]
    src = os.path.join(rm.MESH_DIR, item_id + ".txt")
    header, quads = rm.read_txt(src)
    roles_fn = classify(spec)
    roles = roles_fn(quads)
    counts = {r: sum(1 for v in roles.values() if v == r) for r in ("string", "limb", "limbface", "bolt")}
    if not (counts["string"] and counts["limb"] and counts["limbface"] == 4 and counts["bolt"]):
        raise RuntimeError("%s: the old mesh is not what this script was written for: %s" % (item_id, counts))
    rig, drawn_a, drawn_b, (apex_y, z) = rig_for(spec, quads, roles)
    string_idx = [i for i, r in roles.items() if r == "string"]
    # a texel of the old string's skin: a vertex of one of the quads of its outline
    tails = quads[[i for i in string_idx if quads[i]["P"][:, 0].max() - quads[i]["P"][:, 0].min() < 10][0]]["tails"][0]
    thick = max(quads[i]["P"][:, 2].max() for i in string_idx) - min(quads[i]["P"][:, 2].min() for i in string_idx)
    string_z = (max(quads[i]["P"][:, 2].max() for i in string_idx) + min(quads[i]["P"][:, 2].min() for i in string_idx)) / 2

    def rebuilt_string(f_to, old):
        return straight_string(rig, drawn_a, drawn_b, string_z, spec["string_w"], thick, tails) if f_to <= 0.0 else None

    charged = rm.baked_states(src, out_dir, item_id, rig, lambda q: {}, [("_charged", 1.0, True)], f_from=1.0, glow=spec["glow_charged"])
    spent = rm.baked_states(src, out_dir, item_id, rig, roles_fn, [("_spent", 0.0, False)], f_from=1.0, glow=spec["glow_spent"], strips=24, string_strip=rebuilt_string)
    info = dict(counts=counts, rig=rig, tip_drawn=drawn_a.round(2).tolist(), tip_rest=np.array(rig.tip_a).round(2).tolist(), apex_y=round(apex_y, 2),
                max_draw=round(rig.max_draw, 2))
    return {**charged, **spent}, info


def main(argv):
    out_png = argv[argv.index("--preview") + 1] if "--preview" in argv else None
    for item_id in SPECS:
        paths, info = build(item_id)
        print(item_id, {k: v for k, v in info.items() if k != "rig"})
        for suffix, p in paths.items():
            print("  wrote", os.path.relpath(p, rm.ROOT), sum(1 for _ in open(p)) - 3, "quads")
        if out_png:
            d = os.path.dirname(out_png)
            all_paths = {"(old)": os.path.join(rm.MESH_DIR, item_id + ".txt"), **paths}
            print("  preview:", rm.preview_states(item_id, all_paths, os.path.join(d, item_id + "_states.png"), views=("profile", "three_quarter", "edge")))


if __name__ == "__main__":
    main(sys.argv[1:])
