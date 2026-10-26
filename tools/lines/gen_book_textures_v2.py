#!/usr/bin/env python3
"""Generate the 3.26.0 Skill Book look ("the painted grimoires"): textures, 3D models, icons and the overrides of skill_book.json.

usage: python3 tools/lines/gen_book_textures_v2.py [--check] [--only franchise[,franchise]] [--no-icons]

writes (deterministic, no clock, no global randomness; new files only, every 1.82.0 model and texture stays):
  assets/nusmp/textures/item/skill_book_v2/<franchise>.png      512 x 256  the cover atlas (front, back, spine, edges, tinted emblem plaque, leather, metal, slab, drop)   x 11
  assets/nusmp/textures/item/skill_book_v2/<franchise>_fx.png   288 x 208  raised relief ornaments and the emissive layer (alpha)                                      x 11
  assets/nusmp/textures/item/skill_book_v2/trim_<rarity>.png    256 x 256  corners, strap, gems, chain, claws, fangs, page edges, inlay                                  x 5
  assets/nusmp/textures/item/skill_book_v2/glow_mythic.png      32 x 256   the animated emissive page edge (8 frames) + glow_mythic.png.mcmeta
  assets/nusmp/textures/item/skill_book_icons/<f>_<r>.png       64 x 64    the painted inventory icons (+ <f>_<r>_tint.png: the layer the client tints)                  x 55
  assets/nusmp/models/item/skill_book_v2.json                    the display block and default textures (the parent)
  assets/nusmp/models/item/skill_book_v2_<f>_<r>.json            the 3D body, 55 models, each at most 140 elements
  assets/nusmp/models/item/skill_book_icon_<f>_<r>.json          the flat icon models, 55
  assets/nusmp/models/item/skill_book_v2st_<f>_<r>.json          the separate_transforms wrappers (body in hand / ground / frames, icon in the inventory), 55
  assets/nusmp/models/item/skill_book_st_generic.json            the generic grey 3D book (legacy look of a franchise without a 1.82.0 cover)
  assets/nusmp/models/item/skill_book.json                       the registered model: old overrides first (lowest priority), the 55 new ones, the legacy ones (nusmp:look = 1) last
--check compares the decoded pixels / parsed json with what is on disk and fails on a difference (compressed bytes may differ between zlib versions, pixels may not).
The old generator (gen_book_textures.py) still writes the 1.82.0 files and, through book_model_v2.skill_book_overrides, the same skill_book.json.

WHY a script: eleven covers, five trims and 55 icons that share one painting method are a few functions and eleven motif drawings; by hand they would drift apart. Geometry: book_model_v2.py;
painting: book_paint_v2.py (helpers), book_parts_v2.py (materials, metal, gems, trims), book_covers_v2.py (covers and motifs); the previewer (preview_book.py) reads the same model json.
"""
import json
import os
import sys

import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import book_model_v2 as BM  # noqa: E402
import book_covers_v2 as BC  # noqa: E402
import book_parts_v2 as BP  # noqa: E402
import gen_book_textures as OLD  # noqa: E402
from lines_lib import FRANCHISES  # noqa: E402

ROOT = BP.ROOT
ASSETS = BP.ASSETS
TEX_DIR = BP.TEX_DIR
ICON_DIR = BP.ICON_DIR
MODEL_DIR = BP.MODEL_DIR
FRANCHISE_ORDER = list(FRANCHISES.keys())


def write_json(path, data, check, problems):
    text = json.dumps(data, indent=None, separators=(",", ":"), ensure_ascii=False) + "\n"
    text = text.replace('"elements":[', '"elements":[\n').replace('},{"name"', '},\n{"name"')
    if check:
        if not os.path.exists(path) or json.load(open(path, encoding="utf-8")) != data:
            problems.append(path)
        return
    os.makedirs(os.path.dirname(path), exist_ok=True)
    if not os.path.exists(path) or open(path, encoding="utf-8").read() != text:
        open(path, "w", encoding="utf-8", newline="\n").write(text)


def write_png(path, arr, check, problems, mode="RGBA"):
    img = Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8), mode)
    if check:
        if not os.path.exists(path):
            problems.append(path)
            return
        old = np.asarray(Image.open(path).convert(mode))
        if old.shape != np.asarray(img).shape or not np.array_equal(old, np.asarray(img)):
            problems.append(path)
        return
    os.makedirs(os.path.dirname(path), exist_ok=True)
    if os.path.exists(path):
        old = np.asarray(Image.open(path).convert(mode))
        if old.shape == np.asarray(img).shape and np.array_equal(old, np.asarray(img)):
            return
    img.save(path, optimize=True)


def write_text(path, text, check, problems):
    if check:
        if not os.path.exists(path) or open(path, encoding="utf-8").read() != text:
            problems.append(path)
        return
    os.makedirs(os.path.dirname(path), exist_ok=True)
    if not os.path.exists(path) or open(path, encoding="utf-8").read() != text:
        open(path, "w", encoding="utf-8", newline="\n").write(text)


def old_top_overrides():
    """The 1.82.0 franchise overrides exactly as gen_book_textures.py lists them."""
    return [{"predicate": {"nusmp:franchise": round((i + 1) / OLD.PROP_STEPS - 0.5 / OLD.PROP_STEPS, 6)}, "model": "nusmp:item/skill_book_st_%s" % f} for i, f in enumerate(OLD.FRANCHISE_ORDER)]


def main():
    args = sys.argv[1:]
    check = "--check" in args
    icons = "--no-icons" not in args
    only = args[args.index("--only") + 1].split(",") if "--only" in args else FRANCHISE_ORDER
    problems = []
    # ---- textures
    for f in only:
        write_png(os.path.join(TEX_DIR, f + ".png"), BC.cover_atlas(f), check, problems)
        write_png(os.path.join(TEX_DIR, f + "_fx.png"), BC.fx_atlas(f), check, problems)
    for r in BM.RARITIES:
        write_png(os.path.join(TEX_DIR, "trim_%s.png" % r), BP.paint_trim(r), check, problems)
    write_png(os.path.join(TEX_DIR, "glow_mythic.png"), BP.paint_glow_mythic(), check, problems)
    write_text(os.path.join(TEX_DIR, "glow_mythic.png.mcmeta"), json.dumps({"animation": {"interpolate": True, "frametime": 4}}, indent=2) + "\n", check, problems)
    # ---- models
    write_json(os.path.join(MODEL_DIR, "skill_book_v2.json"), BM.parent_json(), check, problems)
    for f in only:
        for r in BM.RARITIES:
            write_json(os.path.join(MODEL_DIR, "skill_book_v2_%s_%s.json" % (f, r)), BM.model_json(f, r), check, problems)
            write_json(os.path.join(MODEL_DIR, "skill_book_icon_%s_%s.json" % (f, r)), BM.icon_json(f, r), check, problems)
            write_json(os.path.join(MODEL_DIR, "skill_book_v2st_%s_%s.json" % (f, r)), BM.wrapper_json(f, r), check, problems)
    # ---- icons (rendered from the models with the previewer's rasteriser, then painted over: outline, rim light)
    if icons:
        import book_icons_v2 as BI
        for f in only:
            for r in BM.RARITIES:
                base, tint = BI.icon(f, r)
                write_png(os.path.join(ICON_DIR, "%s_%s.png" % (f, r)), base, check, problems)
                write_png(os.path.join(ICON_DIR, "%s_%s_tint.png" % (f, r)), tint, check, problems)
    # ---- skill_book.json (same composition as gen_book_textures.py)
    top = OLD.separate("nusmp:item/skill_book_3d", "nusmp:item/skill_book_2d")
    top["overrides"] = BM.skill_book_overrides(old_top_overrides(), FRANCHISE_ORDER)
    if only == FRANCHISE_ORDER:
        text_path = os.path.join(MODEL_DIR, "skill_book.json")
        OLD.write_json(text_path, top, check, problems)
        # the generic grey 3D book with the old sprite in the GUI: the legacy look of a franchise that has no 1.82.0 cover (fairy_tail)
        OLD.write_json(os.path.join(MODEL_DIR, "skill_book_st_generic.json"), OLD.separate("nusmp:item/skill_book_3d", "nusmp:item/skill_book_2d"), check, problems)
    if check and problems:
        print("STALE:\n  " + "\n  ".join(problems))
        sys.exit(1)
    print("ok: %d franchises x %d rarities %s" % (len(only), len(BM.RARITIES), "checked" if check else "written"))


if __name__ == "__main__":
    main()
