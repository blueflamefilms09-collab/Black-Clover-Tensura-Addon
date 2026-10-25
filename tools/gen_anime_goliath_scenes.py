"""Preview scenes, the renderer call and the contact sheets of the anime Goliath (see gen_anime_goliath.py)."""
import json
import os
import shutil
import subprocess
import sys

sys.dont_write_bytecode = True
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)

REPO = os.path.abspath(os.path.join(HERE, ".."))
SCENES_OUT = os.path.join(HERE, "render_preview", "scenes.d", "anime_goliath.json")
SCRATCH = os.environ.get("AG_SCRATCH", "/tmp/claude-0/-home-user-Black-Clover-Tensura-Addon/2c8ee7c0-aec7-5d43-b651-54cec528bb71/scratchpad")
BUILD = os.environ.get("RENDER_PREVIEW_BUILD", os.path.join(SCRATCH, "rp_anime_goliath"))
PREVIEW_OUT = os.path.join(SCRATCH, "previews_anime_goliath")

NEW, OLD, OLD2 = "anime_goliath", "goliath_body", "arc_goliath"
TY, DIST = 3.4, 16.5                       # the look-at height and the camera distance of the full-body views (6.4 block model)
CLIPS = (("idle", 4.0, 4), ("walk", 2.0, 4), ("slam", 2.2, 6), ("stomp", 3.0, 6), ("roar", 2.4, 6), ("rise", 2.4, 6), ("hurt", 0.6, 4), ("death", 3.0, 6), ("transform", 3.4, 6), ("attack", 1.2, 5))


def view(name, yaw, pitch, target, dist):
    return {"name": name, "yaw": yaw, "pitch": pitch, "target": list(target), "dist": dist}


def scene(model, anim, ages, views, tile=(400, 560), texture=None, bg=None):
    s = {"kind": "geo", "model": model, "space": "PROP", "layer": "CUTOUT", "argb": "FFFFFFFF", "glow": "FFFFFFFF", "ages": list(ages), "tile": list(tile),
         "floor": True, "figure": False, "expect": [], "anim": anim, "views": views}
    if bg:
        s["bg"] = bg
    return s


def old_model(name):
    """The existing models are addressed by file (the stone Goliath of the floors) or by key_name (the arc kit's)."""
    return name


def scenes_for():
    sc = {}
    p = NEW + "_"
    sc[p + "front"] = scene(NEW, "idle", [0], [view("front", 0, 4, (0, TY, 0), DIST), view("front 3/4", 35, 8, (0, TY, 0), DIST)])
    sc[p + "back"] = scene(NEW, "idle", [0], [view("back", 180, 5, (0, TY, 0), DIST), view("side", 90, 4, (0, TY, 0), DIST)])
    sc[p + "q"] = scene(NEW, "idle", [0], [view("3/4 left", 38, 7, (0, TY, 0), DIST), view("3/4 right", -38, 7, (0, TY, 0), DIST)])
    sc[p + "head"] = scene(NEW, "idle", [0], [view("head", 0, 6, (0, 5.6, -0.9), 7.6), view("head 3/4", 34, 8, (0, 5.6, -0.9), 7.6)], (420, 420))
    sc[p + "chest"] = scene(NEW, "idle", [0], [view("chest", 0, 2, (0, 4.2, -0.5), 5.4), view("fist 3/4", 32, 10, (2.3, 1.2, -0.5), 4.4)], (420, 420))
    sc[p + "dark"] = scene(NEW, "idle", [0], [view("front dark", 0, 4, (0, TY, 0), DIST), view("3/4 dark", 35, 8, (0, TY, 0), DIST)], bg="dark")
    far = {"name": "far", "yaw": 20, "pitch": 3, "target": [0, TY, 0], "dist": DIST * 2.0}
    sc[p + "far"] = scene(NEW, "idle", [0], [far], (300, 300))
    for clip, secs, n in CLIPS:
        ages = [round(secs * 20.0 * k / max(1, n - 1 if clip not in ("idle", "walk") else n), 1) for k in range(n)]
        wide = 1.15 if clip in ("slam", "rise", "transform", "death", "roar") else 1.0
        sc["%s%s" % (p, clip)] = scene(NEW, clip, ages, [view("3/4", 32, 6, (0, TY + (0.2 if wide > 1.1 else 0.0), 0), DIST * wide)], (330, 470))
        if clip in ("slam", "stomp", "walk"):
            sc["%s%s_side" % (p, clip)] = scene(NEW, clip, ages, [view("side", 90, 5, (0, TY, 0), DIST * wide)], (330, 470))
    q = "anime_goliath_old_"
    sc[q + "front"] = scene(OLD, "idle", [0], [view("front", 0, 4, (0, TY, 0), DIST), view("front 3/4", 35, 8, (0, TY, 0), DIST)])
    sc[q + "arc"] = scene(OLD2, "idle", [0], [view("front", 0, 4, (0, TY, 0), DIST), view("front 3/4", 35, 8, (0, TY, 0), DIST)])
    sc[q + "head"] = scene(OLD, "idle", [0], [view("head", 0, 6, (0, 5.6, -0.9), 7.6), view("head 3/4", 34, 8, (0, 5.6, -0.9), 7.6)], (420, 420))
    sc[q + "slam"] = scene(OLD, "slam", [0, 8, 16, 24, 32, 40], [view("3/4", 32, 6, (0, TY + 0.2, 0), DIST * 1.15)], (330, 470))
    sc[q + "walk"] = scene(OLD, "walk", [0, 10, 20, 30], [view("3/4", 32, 6, (0, TY, 0), DIST)], (330, 470))
    return sc


def write_scenes(path=SCENES_OUT):
    with open(path, "w") as f:
        json.dump(scenes_for(), f, indent=1)


def render(names=None):
    sc = scenes_for()
    names = list(names or sc)
    env = dict(os.environ)
    env["RENDER_PREVIEW_BUILD"] = BUILD
    os.makedirs(PREVIEW_OUT, exist_ok=True)
    os.makedirs(BUILD, exist_ok=True)
    write_scenes()
    r = subprocess.run([sys.executable, "-B", os.path.join(HERE, "anime_goliath_render.py")] + names, cwd=REPO, env=env, capture_output=True, text=True)
    for l in (r.stdout + r.stderr).splitlines():
        if ("WARNING" in l or "ERROR" in l or "rror" in l or "scenes:" in l or " v  " in l or "verts" in l) and "Picked up" not in l:
            print(l)
    for n in names:
        src = os.path.join(BUILD, n + ".png")
        if os.path.isfile(src):
            shutil.copy(src, os.path.join(PREVIEW_OUT, n + ".png"))
    return r.returncode


def main(argv):
    names = [a for a in argv if not a.startswith("-")]
    if "--sheets" in argv:
        from gen_anime_goliath_sheets import make_sheets
        return make_sheets(names)
    names = [n if n.startswith("anime_goliath") else "anime_goliath_" + n for n in names]
    return render(names or None)
