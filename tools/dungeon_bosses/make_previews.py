"""Why: the owner asked to SEE every dungeon boss and creature. This script renders the REAL art-kit geo models (tools/render_preview's headless GeoDraw rasteriser, the same class the game draws
with) from four sides, plus two move poses, and lays them out as one sheet per creature and one overview sheet:

    python3 -B tools/dungeon_bosses/make_previews.py            # render + compose into docs/dungeon_bosses_previews/
    python3 -B tools/dungeon_bosses/make_previews.py --compose  # only compose (the renders in $DBOSS_PREVIEW_BUILD are reused)

Needs JDK 21, numpy, Pillow (see tools/render_preview/README.md). The renders go to $DBOSS_PREVIEW_BUILD (default build/dboss_preview).
HONEST LABELS: a sheet says "art-kit geo" when the picture is the art-kit model (the Warden's default in-game look is the player-model kit, shown by docs/previews/humanoids/floor_warden.png, and the
Goliath, Minotaur and Lucifero in game are Blender meshes, shown by the Blender renders in docs/bosses/previews), and "pose" strips are the model's own attack / cast clips, not the game's particles.
"""
import json
import os
import subprocess
import sys

sys.dont_write_bytecode = True

from PIL import Image, ImageDraw, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
OUT = os.path.join(ROOT, "docs", "dungeon_bosses_previews")
BUILD = os.path.abspath(os.environ.get("DBOSS_PREVIEW_BUILD") or os.path.join(ROOT, "build", "dboss_preview"))
SCENES_D = os.path.join(ROOT, "tools", "render_preview", "scenes.d")

# key -> (art-kit scene prefix, title, what the sheet says about it, the moves it now has)
CREATURES = {
    "floor_warden": ("arc_floor_warden", "Floor Warden", "NEW LOOK + NEW MOVES",
                     ["Hammer Slam: ring on the floor, step out", "Chain Lash: chains tie 3 players, then drag", "Shadow Step: floor mark, cleave on landing",
                      "Seal Pulse (phase 2): 3 rings, jump them", "Key Toll (phase 3): stand in a sigil", "Enrage after 150 s"]),
    "direwolf": ("arc_dungeon_direwolf", "Direwolf", "NEW MOVES, look unchanged", ["Pounce: crouch ring, then a leap", "Pack Howl: quickens up to 6 wolves"]),
    "basilisk": ("arc_basilisk", "Basilisk", "NEW MOVES, look unchanged",
                 ["Petrifying Gaze: only bites if you look at it", "Venom Spit: poison bolt along a line", "Tail Sweep: ring around it"]),
    "living_armor": ("arc_living_armor", "Living Armor", "NEW MOVES, look unchanged", ["Shield Bash: shove", "Rally Rattle (half health): hardens nearby armours"]),
    "slime": ("arc_dungeon_slime", "Dungeon Slime", "UNCHANGED (rated, not reworked)", ["Splits and grazes; no signature move (tier 1 food, by design)"]),
    "goliath": ("arc_goliath", "Goliath (art-kit fallback)", "UNCHANGED here (waves 1-3 reworked it)", ["In game it is the Blender mesh: see docs/bosses/previews/goliath_blender.png"]),
    "charybdis": ("arc_charybdis_spawn", "Charybdis Spawn (art-kit fallback)", "UNCHANGED here (wave 1 moves)", ["Wave-1 moves; look in game is the kit model"]),
}
SHEET_KEYS = ("floor_warden", "direwolf", "basilisk", "living_armor")

BG, FG, DIM, ACC = (22, 20, 30), (235, 232, 245), (150, 146, 170), (196, 150, 255)


def font(size, bold=False):
    for p in ("/usr/share/fonts/truetype/dejavu/DejaVuSans%s.ttf" % ("-Bold" if bold else ""), "/usr/share/fonts/dejavu/DejaVuSans%s.ttf" % ("-Bold" if bold else "")):
        if os.path.isfile(p):
            return ImageFont.truetype(p, size)
    return ImageFont.load_default()


def base_scene(prefix):
    with open(os.path.join(SCENES_D, prefix + ".json"), encoding="utf-8") as f:
        data = json.load(f)
    return data[prefix + "_front"]


def build_scenes():
    scenes = {}
    for key, (prefix, *_rest) in CREATURES.items():
        s = base_scene(prefix)
        v0 = s["views"][0]
        views = []
        for name, yaw, pitch in (("front", 0, 4), ("3/4", 35, 10), ("side", 90, 4), ("back", 180, 5)):
            views.append({"name": name, "yaw": yaw, "pitch": pitch, "target": v0["target"], "dist": v0["dist"]})
        t = dict(s)
        t["views"] = views
        t["tile"] = [300, 440]
        t["ages"] = [0]
        scenes["dboss_%s_turn" % key] = t
    return scenes


def render():
    os.makedirs(BUILD, exist_ok=True)
    scenes_file = os.path.join(BUILD, "dboss_scenes.json")
    with open(scenes_file, "w", encoding="utf-8") as f:
        json.dump(build_scenes(), f)
    names = []
    for key, (prefix, *_r) in CREATURES.items():
        names += ["dboss_%s_turn" % key, prefix + "_attack", prefix + "_cast_big"]
    env = dict(os.environ, RENDER_PREVIEW_BUILD=BUILD)
    cmd = [sys.executable, "-B", os.path.join(ROOT, "tools", "render_preview", "preview.py"), "--scenes", scenes_file] + names
    r = subprocess.run(cmd, env=env, cwd=ROOT)
    if r.returncode != 0:
        sys.exit("the render preview failed")


def tiles(path, cols, rows, tw, th):
    """The tiles of a preview sheet: 6 px frame, a 36 px title, 6 px between tiles (one row per view, one column per age)."""
    im = Image.open(path).convert("RGB")
    out = []
    for r in range(rows):
        row = []
        for c in range(cols):
            x, y = 6 + c * (tw + 6), 36 + r * (th + 6)
            row.append(im.crop((x, y, x + tw, y + th)))
        out.append(row)
    return out


def sheet(key):
    prefix, title, status, moves = CREATURES[key]
    turn = tiles(os.path.join(BUILD, "dboss_%s_turn.png" % key), 1, 4, 300, 440)
    # poses: the 3/4 row of the attack clip and the cast_big clip (three frames of each)
    strips = []
    for clip in ("attack", "cast_big"):
        p = os.path.join(BUILD, "%s_%s.png" % (prefix, clip))
        if not os.path.isfile(p):
            continue
        t = tiles(p, 5, 2, 340, 480)
        strips.append((clip, [t[0][1], t[0][2], t[0][3], t[0][4]]))
    W = 4 * 300 + 5 * 12
    pose_h = 300
    H = 70 + 440 + 12 + (30 + pose_h) * len(strips) + 30 + 18 * (len(moves) + 1) + 20
    img = Image.new("RGB", (W, H), BG)
    d = ImageDraw.Draw(img)
    d.text((12, 8), title, fill=FG, font=font(26, True))
    d.text((12, 42), status + "   |   art-kit geo, real GeoDraw render (see make_previews.py)", fill=ACC, font=font(14))
    for i, row in enumerate(turn):
        img.paste(row[0], (12 + i * 312, 70))
    y = 70 + 440 + 12
    for clip, frames in strips:
        d.text((12, y + 4), "model clip: " + clip + " (frames)", fill=DIM, font=font(13))
        y += 24
        for i, fr in enumerate(frames):
            fr = fr.resize((int(fr.width * pose_h / fr.height), pose_h))
            img.paste(fr, (12 + i * 312, y))
            d.rectangle((12 + i * 312, y, 12 + i * 312 + fr.width, y + pose_h), outline=(60, 56, 80))
        y += pose_h + 6
    d.text((12, y + 4), "Moves", fill=ACC, font=font(15, True))
    y += 26
    for m in moves:
        d.text((24, y), "- " + m, fill=FG, font=font(14))
        y += 18
    img.crop((0, 0, W, y + 14)).save(os.path.join(OUT, "%s_sheet.png" % key), optimize=True)


def overview():
    keys = list(CREATURES)
    cell_w, cell_h = 300, 440
    cols = 4
    kit_rows = (len(keys) + cols - 1) // cols
    blender = [("docs/bosses/previews/goliath_blender.png", "Goliath (Blender mesh, in game)", "Blender render, wave 2, unchanged here"),
               ("docs/bosses/previews/minotaur_blender.png", "Minotaur (Blender mesh, in game)", "Blender render, wave 2, unchanged here"),
               ("docs/bosses/previews/lucifero_v2_blender.png", "Lucifero (Blender mesh, in game)", "Blender render, wave 2, unchanged here")]
    W = cols * (cell_w + 12) + 12
    warden_img = os.path.join(ROOT, "docs", "previews", "humanoids", "floor_warden.png")
    strip = Image.open(warden_img).convert("RGB") if os.path.isfile(warden_img) else None
    strip_h = 0
    if strip is not None:
        strip_h = int(strip.height * (W - 24) / strip.width) + 60
    H = 60 + kit_rows * (cell_h + 52) + (cell_h + 52) + strip_h + 12
    img = Image.new("RGB", (W, H), BG)
    d = ImageDraw.Draw(img)
    d.text((12, 10), "Dungeon bosses and creatures: overview", fill=FG, font=font(26, True))
    d.text((12, 40), "row 1-2: art-kit renders (real GeoDraw, front view); row 3: Blender renders of the mesh bosses; bottom: the Warden's in-game player-model look", fill=DIM, font=font(13))
    items = []
    for key in keys:
        t = tiles(os.path.join(BUILD, "dboss_%s_turn.png" % key), 1, 4, 300, 440)
        items.append((t[0][0], CREATURES[key][1], CREATURES[key][2]))
    for rel, title, status in blender:
        p = os.path.join(ROOT, rel)
        if os.path.isfile(p):
            im = Image.open(p).convert("RGB")
            im.thumbnail((cell_w, cell_h))
            items.append((im, title, status))
    row_of = lambda n: n // cols if n < len(keys) else kit_rows + (n - len(keys)) // cols
    for n, (im, title, status) in enumerate(items):
        col = n % cols if n < len(keys) else (n - len(keys)) % cols
        if n >= len(keys) and kit_rows * cols > len(keys):
            pass
        x, y = 12 + col * (cell_w + 12), 60 + row_of(n) * (cell_h + 52)
        img.paste(im, (x, y))
        d.text((x, y + cell_h + 4), title, fill=FG, font=font(14, True))
        d.text((x, y + cell_h + 24), status, fill=ACC, font=font(12))
    if strip is not None:
        y = 60 + (kit_rows + 1) * (cell_h + 52)
        d.text((12, y), "Floor Warden, in-game look (player-model kit, software preview: front, 3/4, back)", fill=FG, font=font(14, True))
        img.paste(strip.resize((W - 24, strip_h - 60)), (12, y + 24))
    img.save(os.path.join(OUT, "overview.png"), optimize=True)


def move_diagram():
    """A top-down SCHEMATIC of the Floor Warden's five telegraphs, drawn from the numbers of DungeonReworkRules (radii in blocks). Not a render of the game's particles: the Fx
    particle themes are sent by the server and the offline VFX preview cannot show them."""
    panels = [("Hammer Slam", "ring r4.5 grows, step out"), ("Chain Lash", "3 players tied, then dragged in"), ("Shadow Step", "mark r1.8, cleave r3.2 on landing"),
              ("Seal Pulse (ph 2)", "3 rings out to 16: jump them"), ("Key Toll (ph 3)", "stand in a lit sigil r3 at 7")]
    pw, ph, scale = 300, 330, 7.0
    img = Image.new("RGB", (len(panels) * (pw + 8) + 8, ph + 60), BG)
    d = ImageDraw.Draw(img)
    d.text((10, 6), "Floor Warden telegraphs (top-down schematic, 1 block = %d px; not a particle render)" % scale, fill=FG, font=font(14, True))
    for i, (name, note) in enumerate(panels):
        ox, oy = 8 + i * (pw + 8), 34
        d.rectangle((ox, oy, ox + pw, oy + ph), fill=(30, 28, 42), outline=(70, 64, 96))
        cx, cy = ox + pw // 2, oy + ph // 2 - 10
        def circ(r, col, w=2, x=cx, y=cy):
            d.ellipse((x - r * scale, y - r * scale, x + r * scale, y + r * scale), outline=col, width=w)
        if i == 0:
            circ(4.5, ACC, 3)
            circ(2.5, (120, 90, 170), 1)
        elif i == 1:
            for ang in (-0.9, 0.2, 1.3):
                import math
                x, y = cx + math.cos(ang) * 17 * scale * 0.9, cy + math.sin(ang) * 17 * scale * 0.9
                d.line((cx, cy, x, y), fill=ACC, width=2)
                d.ellipse((x - 5, y - 5, x + 5, y + 5), fill=(255, 220, 120))
        elif i == 2:
            circ(1.8, ACC, 3, cx + 60, cy - 40)
            circ(3.2, (120, 90, 170), 1, cx + 60, cy - 40)
            d.line((cx, cy, cx + 60, cy - 40), fill=(90, 80, 120), width=1)
        elif i == 3:
            for r, col in ((4, ACC), (9, (160, 120, 220)), (14, (110, 85, 160))):
                circ(r, col, 2)
        else:
            import math
            for sx in (7, -7):
                circ(3.0, (140, 255, 170), 3, cx + sx * scale, cy)
            circ(22 if False else 14, (110, 85, 160), 1)
        d.ellipse((cx - 6, cy - 6, cx + 6, cy + 6), fill=(255, 255, 255))
        d.text((ox + 8, oy + 6), name, fill=FG, font=font(14, True))
        d.text((ox + 8, oy + ph - 22), note, fill=DIM, font=font(12))
    img.save(os.path.join(OUT, "floor_warden_moves.png"), optimize=True)


def main(argv):
    os.makedirs(OUT, exist_ok=True)
    if "--compose" not in argv:
        render()
    for key in CREATURES:
        sheet(key)
    overview()
    move_diagram()
    print("wrote", OUT)


if __name__ == "__main__":
    main(sys.argv[1:])
