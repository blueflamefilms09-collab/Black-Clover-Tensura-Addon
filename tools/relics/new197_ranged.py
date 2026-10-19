"""Bows and the crossbow of 1.97.0: the geometry helpers of wave23_models (limbs, string, arrow) and the finish step with the draw states.

A bow is built upright (limbs along y, string at x = 0, belly toward +x, parts limb* / string* / arrow* move in the draw states); the rest mesh has no arrow, the
three `_pull` states have the arrow riding the nock. tools/relics/bow_vanilla.py then lays every bow of class "bow" on vanilla's diagonal (place angle 225), the size
and the display of vanilla's Bow (BowDisplayGuardTest). A crossbow is built along y (stock) with the prod across x (see wave10_m_needler).
"""
import math
import os
import tempfile

import numpy as np

from new197_lib import *            # noqa: F401,F403
from new197_lib import ml, w, new_mesh, new_atlas, lay_and_paint, stub_icon, render_icon, write_models, MESH_DIR, ICON_DIR, PREVIEW_DIR
import ranged_meshes as rm
import wave23_models as wm


def copy_without(m, prefixes):
    """A copy of the mesh without the parts whose names start with a prefix (the rest view: no arrow, no bolt)."""
    out = ml.Mesh(m.name)
    out.place_cfg, out.glow_cfg = m.place_cfg, m.glow_cfg
    for p in m.parts:
        if not any(p.name.startswith(x) for x in prefixes):
            out.add(wm.rm._copy_part(p))
    return out


def finish_bow(item_id, m, atlas, half, belly, flex, max_draw, glow=("FFFFFF", 0.14, 0.45, 1.0)):
    """Places the bow (upright, fit), writes the rest mesh and the draw states, the skins, the sprite (as vanilla's bow lies, place angle 225) and the item models."""
    tmp = os.path.join(tempfile.gettempdir(), "n197_stub_%d.png" % os.getpid())
    stub_icon(tmp)
    m.place_on_sprite(tmp, fit=True)
    m.set_glow(*glow)
    rig = rm.Rig(tip_a=(0.0, half, 0.0), tip_b=(0.0, -half, 0.0), draw_dir=(-1, 0, 0), max_draw=max_draw, flex=flex, grip=(belly, 0.0, 0.0), limb_axis=(0, 1, 0), limb_span=half)
    paths = wm.emit_bow(m, rig, item_id, atlas)
    atlas.save(item_id)
    rest = copy_without(m, ("arrow",))
    rest.finish()
    render_icon(rest, atlas, os.path.join(ICON_DIR, item_id + ".png"), 225.0, widen=False)
    write_models(item_id)
    print(f"{item_id}: {len(m.parts)} parts, states {sorted(paths)}, atlas {atlas.size}px")
    if PREVIEW_DIR:
        os.makedirs(PREVIEW_DIR, exist_ok=True)
        ml.preview(rest, None, os.path.join(PREVIEW_DIR, item_id + ".png"), atlas=atlas, normals="vertex")
    return paths


def finish_crossbow(item_id, m, atlas, rig, glow=("FFFFFF", 0.16, 0.4, 1.0), real_m=1.1):
    """Places the crossbow on the sprite diagonal, writes the rest mesh, the loading states and the cocked state, the skins, the sprite and the item models."""
    tmp = os.path.join(tempfile.gettempdir(), "n197_stub_%d.png" % os.getpid())
    stub_icon(tmp)
    m.place_on_sprite(tmp, real_m=real_m)
    m.set_glow(*glow)
    paths = rm.emit_states(m, rig, "crossbow", item_id, atlas=atlas, normals="vertex")
    atlas.save(item_id)
    rest = copy_without(m, ("bolt",))
    rest.finish()
    render_icon(rest, atlas, os.path.join(ICON_DIR, item_id + ".png"), m.place_cfg[2], widen=True)
    write_models(item_id)
    print(f"{item_id}: {len(m.parts)} parts, states {sorted(paths)}, atlas {atlas.size}px")
    if PREVIEW_DIR:
        os.makedirs(PREVIEW_DIR, exist_ok=True)
        ml.preview(rest, None, os.path.join(PREVIEW_DIR, item_id + ".png"), atlas=atlas, normals="vertex")
    return paths
