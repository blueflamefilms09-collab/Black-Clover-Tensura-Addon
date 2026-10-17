"""Why: the manual lists every name of the vocabulary (hair styles, eyes, outfits, props ...), and nobody picks a hair style from a word. `--gallery <set>` builds one small
model per name, renders each with the preview harness and lays them out in one contact sheet with the names printed on it, so a character author can LOOK at the whole list.
The models are temporary: they are written under tools/render_preview/assets/nusmp (a folder the preview harness reads, next to the mod's own assets) and removed afterwards,
nothing of them stays in the mod.

    python3 -B tools/gen_arc_characters.py --gallery hair            # sets: hair eyes tops legs builds props accessories extras all
    python3 -B tools/gen_arc_characters.py --gallery props --out docs/arc/preview
"""
import copy
import json
import os
import shutil
import subprocess
import sys

from . import scenes as SC
from . import build as B
from . import face as F
from . import hair as Hr
from . import outfit as O
from . import props as Pr
from . import spec as SP
from .rig import BUILDS

SETS = ("hair", "eyes", "tops", "legs", "builds", "props", "accessories", "extras")
BASE = {"name": "Gallery", "build": "average", "skin": "light", "face": {"expression": "calm", "eye_color": "#4a6aa8"}, "hair": {"style": "short", "color": "#7a4a24"},
        "outfit": {"top": {"kind": "shirt", "color": "#4a5a7a", "trim": "#c8b48c"}, "legs": {"kind": "trousers", "color": "#2c2c38"}, "boots": {"kind": "boots", "color": "#2a2220"}}}


def _merge(a, b):
    a = copy.deepcopy(a)
    for k, v in b.items():
        a[k] = _merge(a[k], v) if isinstance(v, dict) and isinstance(a.get(k), dict) else v
    return a


def entries(name):
    """{label: (spec overrides, view)}: one model per name of the vocabulary set."""
    out = {}
    if name == "hair":
        for st in Hr.HAIR_STYLES:
            out[st] = ({"hair": {"style": st}}, "head")
    elif name == "eyes":
        for e in F.EYE_STYLES:
            out[e] = ({"face": {"eyes": e, "brows": "thin", "mouth": "neutral"}}, "face")
        for b in F.BROW_STYLES:
            out["brows " + b] = ({"face": {"eyes": "round", "brows": b, "mouth": "neutral"}}, "face")
        for m in F.MOUTH_STYLES:
            out["mouth " + m] = ({"face": {"eyes": "round", "brows": "thin", "mouth": m}}, "face")
        for mk in F.MARKS:
            out["mark " + mk] = ({"face": {"eyes": "round", "brows": "thin", "mouth": "neutral", "marks": [mk]}}, "face")
    elif name == "tops":
        for k in O.TOP_KINDS:
            out[k] = ({"outfit": {"top": {"kind": k, "color": "#6a3a4a", "trim": "#e8d8b0", "collar": "stand"}, "belt": {}}}, "body")
        for k in O.CAPE_KINDS:
            out["cape " + k] = ({"cape": {"kind": k, "color": "#7a2430", "emblem": {"kind": "star", "size": 7}}}, "back")
        for st in ("plate", "chain", "leather", "bone", "scale"):
            out["armor " + st] = ({"armor": {"pieces": list(O.ARMOR_PIECES), "style": st, "color": "#8c98a8", "trim": "#a08040", "emblem": "cross"}}, "body")
        for k in O.EMBLEMS:
            if k != "none":
                out["emblem " + k] = ({"outfit": {"top": {"kind": "tabard", "color": "#2c3c6a", "trim": "#e8d8b0", "emblem": {"kind": k, "where": "chest", "size": 8, "y": 2}}}}, "body")
    elif name == "legs":
        for k in O.LEG_KINDS:
            out["legs " + k] = ({"outfit": {"legs": {"kind": k, "stripe": "#c8a040"}, "top": {"kind": "tunic", "length": "hip"}}}, "body")
        for k in O.BOOT_KINDS:
            out["boots " + k] = ({"outfit": {"boots": {"kind": k, "height": "knee", "cuff": "#c8a040"}}}, "body")
        for k in O.GLOVE_KINDS:
            out["gloves " + k] = ({"outfit": {"gloves": {"kind": k, "color": "#6a3a2a"}}}, "body")
        for k in O.NECK_KINDS:
            out["neck " + k] = ({"outfit": {"neck": {"kind": k, "color": "#8a2430"}}}, "head")
    elif name == "builds":
        for b in BUILDS:
            out[b] = ({"build": b, "outfit": {"top": {"kind": "coat", "color": "#3a4a6a", "trim": "#e8d8b0", "length": "knee"}, "belt": {}}}, "body")
    elif name == "props":
        for k in Pr.PROP_KINDS:
            slot = "left" if Pr.PROPS[k].hand == "l" else "right"
            out[k] = ({"props": {slot: {"kind": k, "glow": "#8ad8ff" if k in ("staff", "orb", "wand", "grimoire", "hammer") else None}}}, "body")
        out["back: greatsword"] = ({"props": {"back": {"kind": "greatsword"}}}, "back")
        out["float: grimoire"] = ({"props": {"float": {"kind": "grimoire", "glow": "#ffaa66"}}}, "body")
    elif name == "accessories":
        for k in SP.ACCESSORY_KINDS:
            out[k] = ({"accessories": [{"kind": k, "glow": "#ffcc66" if k in ("crown", "circlet") else None}]}, "head" if k not in ("backpack", "satchel", "quiver", "pocket_watch", "tassel", "chain", "shoulder_fur") else "body")
    elif name == "extras":
        for k in SP.WING_KINDS:
            out["wings " + k] = ({"wings": {"kind": k}}, "back")
        for k in SP.TAIL_KINDS:
            out["tail " + k] = ({"tail": {"kind": k}}, "back")
        for k in SP.HORN_KINDS:
            out["horns " + k] = ({"horns": {"kind": k}}, "head")
        for k in SP.EAR_KINDS:
            out["ears " + k] = ({"ears": {"kind": k}}, "head")
        for k in SP.HALO_KINDS:
            out["halo " + k] = ({"halo": {"kind": k}}, "back")
        for k in SP.MASK_KINDS:
            out["mask " + k] = ({"face": {"mask": {"kind": k, "eye_glow": "#ff4040"}}}, "head")
    return out


def _view(name, yaw, pitch, target, dist):
    return {"name": name, "yaw": yaw, "pitch": pitch, "target": [round(t, 3) for t in target], "dist": round(dist, 3)}


def run(which, out_dir, repo, preview_py):
    from PIL import Image, ImageDraw
    import geo_builder
    sets = SETS if which == "all" else (which,)
    for w in sets:
        if w not in SETS:
            print("unknown gallery set %r; use one of: %s, all" % (w, ", ".join(SETS)))
            return 1
    root = os.path.join(repo, "tools", "render_preview", "assets", "nusmp")
    saved_root = geo_builder.ROOT
    made = []
    code = 0
    os.environ["ARC_FAST"] = "1"
    try:
        geo_builder.ROOT = root
        for w in sets:
            ents = entries(w)
            scn, labels = {}, {}
            for label, (over, mode) in ents.items():
                slug = "".join(ch if ch.isalnum() else "_" for ch in label.lower()).strip("_")
                mid = "zz_gallery_%s_%s" % (w, slug)
                raw = _merge(BASE, over)
                raw["id"] = mid
                R = B.build_character(raw, file_id=mid)
                img, glow = R.finish()
                img, glow, hh = R.cropped(img, glow)
                R.m.tex_h = hh
                R.m.save(R.A)
                R.m.save_texture(img, glow)
                made.append(mid)
                d = R.d
                lo, hi = SC.bounds(R)
                H = max(hi[1], d.top) / 16.0
                if mode == "head":
                    ty = (d.head_y + 5) / 16.0
                    vs = [_view("3/4", 35, 8, [0, ty, 0], 1.9), _view("back", 150, 8, [0, ty, 0], 1.9)]
                    tile = (300, 330)
                elif mode == "face":
                    ty = (d.head_y + 4) / 16.0
                    vs = [_view("front", 0, 2, [0, ty, 0], 1.45)]
                    tile = (300, 300)
                elif mode == "back":
                    ty = H * 0.55
                    vs = [_view("back", 160, 8, [0, ty, 0], H * 2.1 + 0.8), _view("3/4", 35, 8, [0, ty, 0], H * 2.1 + 0.8)]
                    tile = (300, 380)
                else:
                    ty = H * 0.5
                    vs = [_view("front", 0, 5, [0, ty, 0], H * 2.1), _view("3/4", 40, 8, [0, ty, 0], H * 2.1)]
                    tile = (300, 380)
                sname = "arc_%s_g" % mid
                scn[sname] = SC.scene("arc_" + mid, "idle", [0], vs, tile)
                labels[sname] = label
            scene_file = os.path.join(out_dir, "gallery_%s_scenes.json" % w)
            os.makedirs(out_dir, exist_ok=True)
            with open(scene_file, "w") as f:
                json.dump(scn, f)
            env = dict(os.environ)
            env.setdefault("RENDER_PREVIEW_BUILD", os.path.join(out_dir, "build"))
            r = subprocess.run([sys.executable, "-B", preview_py, "--scenes", scene_file] + list(scn), cwd=repo, env=env, capture_output=True, text=True)
            for line in (r.stdout + r.stderr).splitlines():
                if "ERROR" in line or "scenes:" in line:
                    print(line)
            tiles = []
            for sname, label in labels.items():
                p = os.path.join(env["RENDER_PREVIEW_BUILD"], sname + ".png")
                if os.path.isfile(p):
                    im = Image.open(p).convert("RGB")
                    im = im.resize((int(im.size[0] * 0.62), int(im.size[1] * 0.62)), Image.LANCZOS)
                    dr = ImageDraw.Draw(im)
                    dr.rectangle((0, 0, im.size[0], 12), fill=(20, 20, 28))
                    dr.text((3, 0), label, fill=(255, 235, 170))
                    tiles.append(im)
            if not tiles:
                continue
            cols = 8 if w in ("hair", "accessories", "extras", "eyes") else 6
            tw, th = max(t.size[0] for t in tiles), max(t.size[1] for t in tiles)
            sheet = Image.new("RGB", (cols * tw, ((len(tiles) + cols - 1) // cols) * th), (40, 40, 48))
            for k, t in enumerate(tiles):
                sheet.paste(t, ((k % cols) * tw, (k // cols) * th))
            outp = os.path.join(out_dir, "arc_gallery_%s.png" % w)
            sheet.save(outp)
            os.remove(scene_file)
            print("wrote", os.path.relpath(outp, repo) if outp.startswith(repo) else outp, sheet.size, len(tiles), "models")
    except Exception as e:
        import traceback
        traceback.print_exc()
        code = 1
    finally:
        os.environ.pop("ARC_FAST", None)
        geo_builder.ROOT = saved_root
        for mid in made:
            for rel in ("geo/entity/arc_%s.geo.json" % mid, "animations/entity/arc_%s.animation.json" % mid, "textures/entity/arc_%s.png" % mid, "textures/entity/arc_%s_glow.png" % mid):
                p = os.path.join(root, rel)
                if os.path.isfile(p):
                    os.remove(p)
        for dp, dn, fn in os.walk(os.path.join(repo, "tools", "render_preview", "assets", "nusmp"), topdown=False):
            if not os.listdir(dp):
                os.rmdir(dp)
    return code
