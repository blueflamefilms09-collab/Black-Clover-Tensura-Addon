"""Composite preview PNG per Legendary Spear form: the whole weapon (profile, three-quarter) over close-ups of the head and the socket,
drawn with the skin that ships in the mod.  python3 tools/blender/forms_legendary_spear_previews.py [form ids...]
Writes docs/heroweapon/form_previews/legendary_spear_<form>.png"""
import os
import sys
import tempfile

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
sys.path.insert(0, os.path.join(ROOT, "tools", "relics"))
import preview_mesh as pm                       # noqa: E402
from PIL import Image                           # noqa: E402

FORMS = ["hunters_spear", "filolial_spear", "chaos_spear", "meteor_spear", "portal_spear", "triaina", "bruenack", "clockwork_spear", "dragon_minute_hand"]
OUT = os.path.join(ROOT, "docs", "heroweapon", "form_previews")


def compose(form):
    mid = "legendary_spear__" + form
    tmp = tempfile.mkdtemp(prefix="lspv_")
    panels = []
    for tag, views, focus in (("full", ("profile", "three_quarter"), None), ("head", ("profile", "three_quarter"), (235.0, 345.0)), ("socket", ("profile", "three_quarter"), (205.0, 285.0))):
        out = os.path.join(tmp, tag + ".png")
        pm.render_preview(mid, None, out, views=views, normals="vertex", focus=focus)
        panels.append(Image.open(out).convert("RGB"))
    w = max(p.width for p in panels)
    sheet = Image.new("RGB", (w, sum(p.height for p in panels)), (52, 58, 72))
    y = 0
    for p in panels:
        sheet.paste(p, ((w - p.width) // 2, y))
        y += p.height
    os.makedirs(OUT, exist_ok=True)
    path = os.path.join(OUT, "legendary_spear_%s.png" % form)
    sheet.save(path, optimize=True)
    for f in os.listdir(tmp):
        os.remove(os.path.join(tmp, f))
    os.rmdir(tmp)
    return path


if __name__ == "__main__":
    for f in (sys.argv[1:] or FORMS):
        print(compose(f))
