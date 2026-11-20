"""Driver for the vassal form builders: run / main / contact sheet (on top of forms_vassal_common.py, which holds the scene and bake helpers).

    from forms_vassal_run import main
    main(sys.argv[1:], BUILDERS, "vassal_katana", "katana", MANIFEST)
BUILDERS = {key: (builder fn, mesh id, form id)}
"""
import os
import sys
import time

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import bpy  # noqa: E402
import forms_vassal_common as C  # noqa: E402
import forms_kit as K  # noqa: E402
from forms_kit import V  # noqa: E402


def wb_preview(path, views=(("front", 0, 0), ("tq", -35, 18), ("side", -90, 0)), size=(420, 760)):
    """Fast Workbench stills of the scene as it is now (flat material colours, studio light): the geometry check before the long Cycles bake."""
    import math
    import numpy as np
    from PIL import Image
    sc = bpy.context.scene
    old = (sc.render.engine, sc.camera, sc.render.resolution_x, sc.render.resolution_y)
    for m in bpy.data.materials:
        try:
            for n in m.node_tree.nodes:
                if n.bl_idname == "ShaderNodeRGB":
                    m.diffuse_color = tuple(n.outputs[0].default_value)
                    break
        except Exception:
            pass
    sc.render.engine = "BLENDER_WORKBENCH"
    sh = sc.display.shading
    sh.light = "STUDIO"
    sh.color_type = "MATERIAL"
    sh.show_cavity = True
    sc.render.resolution_x, sc.render.resolution_y = size
    sc.render.film_transparent = False
    obs = [o for o in sc.collection.objects if o.type == "MESH"]
    pts = np.array([tuple(o.matrix_world @ v.co) for o in obs for v in o.data.vertices])
    lo, hi = pts.min(axis=0), pts.max(axis=0)
    ctr = V(((lo + hi) / 2).tolist())
    scale = max((hi[1] - lo[1]) * size[0] / size[1], hi[0] - lo[0], hi[2] - lo[2]) * 1.1
    tiles = []
    for nm, yaw, pit in views:
        cd = bpy.data.cameras.new("wbcam")
        cd.type = "ORTHO"
        cd.ortho_scale = scale
        cam = bpy.data.objects.new("wbcam", cd)
        sc.collection.objects.link(cam)
        y, p = math.radians(yaw), math.radians(pit)
        pos = ctr + V((math.sin(y) * math.cos(p), math.sin(p), math.cos(y) * math.cos(p))) * 400
        cam.matrix_world = K.frame(pos, pos - ctr, (0, 1, 0))
        sc.camera = cam
        fp = path[:-4] + "_%s.png" % nm
        sc.render.filepath = fp
        bpy.ops.render.render(write_still=True)
        tiles.append(Image.open(fp).convert("RGB"))
        os.remove(fp)
        bpy.data.objects.remove(cam)
    sheet = Image.new("RGB", (size[0] * len(tiles), size[1]))
    for i, t in enumerate(tiles):
        sheet.paste(t, (i * size[0], 0))
    os.makedirs(os.path.dirname(os.path.abspath(path)), exist_ok=True)
    sheet.save(path)
    sc.render.engine, sc.camera, sc.render.resolution_x, sc.render.resolution_y = old


def run(builders, base, klass, manifest, only, trial, beauty, quick, samples, wb=None):
    C.BASE[0] = base
    for key in only:
        fn, mesh_id, form = builders[key]
        t0 = time.time()
        q = fn({"samples": samples})
        print("   parts:", K.quad_table(16))
        print("[%s] built geometry: ~%d body quads in %.1f s" % (mesh_id, q, time.time() - t0))
        if wb:
            wb_preview(os.path.join(wb, key + ".png"))
            print("[wb] wrote", os.path.join(wb, key + ".png"))
        if beauty:
            os.makedirs(beauty, exist_ok=True)
            for view in ("three_quarter", "front"):
                K.beauty(os.path.join(beauty, "%s_%s.png" % (key, view)), view=view, samples=max(8, samples // 2), size=(560, 900))
        if not quick:
            C.finish(mesh_id, trial=trial)
            if not trial:                                           # each finished form is written at once, so an interrupted run loses nothing
                K.write_manifest(manifest, {mesh_id: {"weapon": base, "form": form, "class": klass}})


def main(argv, builders, base, klass, manifest):
    import argparse
    ap = argparse.ArgumentParser()
    ap.add_argument("--only", default=",".join(builders))
    ap.add_argument("--trial")
    ap.add_argument("--beauty")
    ap.add_argument("--quick", action="store_true")
    ap.add_argument("--wb", help="DIR: fast Workbench stills (front / three-quarter / side) of the geometry, no bake")
    ap.add_argument("--sheet", action="store_true", help="only write the contact sheet docs/heroweapon/form_previews/<family>.png from the baked files")
    ap.add_argument("--samples", type=int, default=24)
    ap.add_argument("--threads", type=int, default=2)
    ap.add_argument("--bake", type=int, default=2048)
    a = ap.parse_args(argv)
    C.THREADS[0] = max(1, min(2, a.threads))
    C.BAKE_PX[0] = a.bake
    if a.sheet:
        fam = os.path.basename(manifest)[len("forms_manifest_"):-len(".json")]
        out = os.path.join(K.ROOT, "docs", "heroweapon", "form_previews", fam + ".png")
        print("wrote", contact_sheet(base, [v[1] for v in builders.values()], out, trial=a.trial))
        return
    run(builders, base, klass, manifest, a.only.split(","), a.trial, a.beauty, a.quick or bool(a.wb), a.samples, a.wb)


def contact_sheet(base, mesh_ids, out_png, trial=None, views=("profile", "three_quarter"), cols=2):
    """docs/heroweapon/form_previews/<family>.png: the base weapon first, then every form, drawn by the repo's software preview (tools/relics/preview_mesh.py)."""
    from PIL import Image, ImageDraw
    sys.path.insert(0, os.path.join(K.ROOT, "tools", "relics"))
    import preview_mesh as pm
    real = pm.ASSETS
    cells = []
    tmp = os.path.join(os.environ.get("TMPDIR", "/tmp"), "forms_sheet_tmp")
    os.makedirs(tmp, exist_ok=True)
    for i, mid in enumerate([base] + list(mesh_ids)):
        pm.ASSETS = real if (i == 0 or not trial) else trial
        p = os.path.join(tmp, "c%d.png" % i)
        pm.render_preview(mid, None, p, views=views, panel=(300, 560), ss=2, normals="vertex", label=False)
        im = Image.open(p).convert("RGB")
        d = ImageDraw.Draw(im)
        d.text((8, 6), mid + ("  (base)" if i == 0 else ""), fill=(255, 255, 255))
        cells.append(im)
    pm.ASSETS = real
    w, h = cells[0].size
    rows = (len(cells) + cols - 1) // cols
    sheet = Image.new("RGB", (w * cols, h * rows), (60, 64, 78))
    for i, c in enumerate(cells):
        sheet.paste(c, ((i % cols) * w, (i // cols) * h))
    os.makedirs(os.path.dirname(os.path.abspath(out_png)), exist_ok=True)
    sheet.save(out_png, optimize=True)
    return out_png
