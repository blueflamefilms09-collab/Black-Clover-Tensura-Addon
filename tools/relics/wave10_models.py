"""Relics of Many Worlds, second card set, wave 10: the ten hand-held weapons as 3D models, built with the owner's model method (tools/relics/mesh_lib.py).

    python3 -B tools/relics/wave10_models.py [id ...]       (no ids = all ten; idempotent, deterministic)
    W10_PREVIEW=<dir> python3 -B tools/relics/wave10_models.py krakeblade      (also renders the preview beside the card)

Run tools/relics/wave10_icons.py first: every mesh is placed on its 16 x 16 sprite (the sprite stays the inventory icon).
Per id it writes assets/nusmp/weapon_meshes/<id>.txt (smoothed per-vertex "qn" normals), textures/entity/weapon/<id>.png and <id>_glow.png
(one 512 px skin each) and, for the Needler (a crossbow), its states needler_pull1..3 and needler_charged (ranged_meshes.emit_states).
Budget of this set: 8 to 14 named parts and at most 2800 quads per weapon.

The method, in order, for every weapon (each module wave10_m_<name>.py follows it):
  0 target decided: the card's art and the printed text.
  1 measure the reference: pixels along one known length -> centimetres; every proportion is a NAMED CONSTANT with a comment saying where it came from.
  2 geometry from real cross-sections (loft / sweep / lathe), never a box or a four-sided frustum for a long form.
  3 the small raised parts are the detail: each is its own named Part with its own constants.
  4 normals smoothed by angle per part, UVs projected on the dominant axis (mesh_lib.Atlas.layout), one skin per weapon.
  5 painted material plus a second additive glow skin.
  6 rendered beside the reference (W10_PREVIEW) and corrected: silhouette, then sections and bevels, then the raised parts, then the texture.
"""
import importlib
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
os.environ["W1_PREVIEW"] = os.environ.get("W10_PREVIEW", "")             # wave1_models' finish() / reference() read this one
import wave1_models as w                                                # noqa: E402
import mesh_lib as ml                                                   # noqa: E402

CARDS = os.environ.get("RELICS2", "/tmp/claude-0/-home-user-Black-Clover-Tensura-Addon/2c8ee7c0-aec7-5d43-b651-54cec528bb71/scratchpad/relics2")
w.ZIP = CARDS                                                           # the cards of this set live under scratchpad/relics2/p2, p3

MODELS = ["krakeblade", "unicorn_dagger", "rune_splitter", "junglethorn_whip", "needler", "sidras_swearing_sword", "stegosaurus_greatsword",
          "jade_xiphos", "returning_hand_axe", "dead_blade"]
MODULES = {i: "wave10_m_" + i for i in MODELS}


def card(rel_path, box, out_name):
    """The card art crop for the preview (None when the images are not around)."""
    return w.reference(rel_path, box, out_name)


def main(only=None):
    for item_id in MODELS:
        if only and item_id not in only:
            continue
        mod = importlib.import_module(MODULES[item_id])
        mod.build()


if __name__ == "__main__":
    main(sys.argv[1:])


# ================================================================ helpers shared by the ten models
def wrapped_grip(m, y0, y1, r, base, wrap_base, n_wraps, material="leather", wrap_material="leather", name="grip", swell=1.05, sides=10):
    """A grip (a round loft with a slow swell) and, as its own part, binding rings along it (flat bands, no end caps: few quads)."""
    import mesh_lib as ml
    gp = w.add(m, name, material, base=base, uv_mode="loft", smooth=50)
    ml.loft(gp, "y", (0, 0, 0), [(y0, 0.94, 0.94), (y0 + (y1 - y0) * 0.3, 1.0, 1.0), ((y0 + y1) / 2, swell, swell), (y1 - (y1 - y0) * 0.25, 1.0, 1.0), (y1, 0.96, 0.96)], ml.circle(r, sides))
    gw = w.add(m, name + "_wraps", wrap_material, base=wrap_base, wear=0.3, smooth=60)
    for i in range(n_wraps):
        ml.collar(gw, y0 + 0.6 + (y1 - y0 - 1.8) * i / max(1, n_wraps - 1), r * 1.08, 0.6, lip=0.16, sides=sides, caps=(False, False))
    return gp, gw


def tube(m, name, material, pts, radius, taper=0.8, sides=6, base=None, glow=False, smooth=70, end_point=True):
    """A tapering round tube along a polyline (a tentacle, a vine, a claw): radius at the start, shrinking by `taper` to a point."""
    import mesh_lib as ml
    p = w.add(m, name, material, base=base, glow=glow, smooth=smooth, uv_mode="loft")

    def sc(t):
        r = radius * (1.0 - taper * t)
        return (r, r) if (t < 0.97 or not end_point) else (0.0, 0.0)
    ml.sweep(p, pts, ml.circle(1.0, sides), scales=sc)
    return p


def glow_all(atlas, part, rgb, strength=0.5, core=0.3):
    w.paint_part(atlas, part, lambda c: w.set_glow(c, rgb, strength, core=core))
