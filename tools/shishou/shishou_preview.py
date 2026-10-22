"""Previews of Shishou without the game (numpy + PIL only): writes docs/previews/shishou/*.png.

    python3 -B tools/shishou/shishou_preview.py [name ...]      names: compare, closeup, states, dormant, hand, icon, hud   (none = all)

What is drawn: the REAL mesh file the game draws (assets/nusmp/weapon_meshes/shishou.txt) and its skins, through the software rasteriser of tools/relics/preview_mesh.py (the same rules as the game's
renderer: counter-clockwise quads, back faces culled, cutout alpha, the glow layer added). The four animation states use the SAME pose maths as client/shishou/ShishouRenderer (the clip's rotation and
position of the bone 'sword', then the entity's yaw and pitch, then the blade laid along +z), sampled from the real animation file; the sky, the ground and the HUD panel are stand-ins (the HUD is drawn
with the same rows as client/shishou/ShishouHud). Not a game screenshot: the lighting is the rasteriser's.
"""
import json
import math
import os
import subprocess
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
sys.path.insert(0, os.path.join(ROOT, "tools", "relics"))
import preview_mesh as pm                                                   # noqa: E402

ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp")
OUT = os.path.join(ROOT, "docs", "previews", "shishou")
REF = os.path.join(HERE, "shishou_reference.webp")
ITEM = "shishou"


def ref_turned(path):
    """The reference turned half a turn (tip up), on white, saved as a temp png for the comparison."""
    im = Image.open(REF).convert("RGBA").crop((20, 100, 440, 1515)).rotate(180)
    bg = Image.new("RGBA", im.size, (255, 255, 255, 0))
    bg.alpha_composite(im)
    bg.save(path)
    return path


def load():
    d = pm.parse_game_txt(os.path.join(ASSETS, "weapon_meshes", ITEM + ".txt"))
    td = os.path.join(ASSETS, "textures", "entity", "weapon")
    skin, gl = os.path.join(td, ITEM + ".png"), os.path.join(td, ITEM + "_glow.png")
    quads = [(l, P, UV, N, (.7, .7, .7)) for (l, P, UV, N) in d["quads"]]
    return d, quads, skin, (gl if os.path.exists(gl) else None)


def compare():
    os.makedirs(OUT, exist_ok=True)
    tmp = ref_turned(os.path.join(OUT, "_ref_turned.png"))
    pm.render_preview(ITEM, tmp, os.path.join(OUT, "sword_vs_reference.png"), views=("profile", "three_quarter", "edge"), panel=(300, 640), normals="vertex")
    os.remove(tmp)


def closeup():
    """The guard and the head, close: the front, the three-quarter and the edge."""
    os.makedirs(OUT, exist_ok=True)
    d, q, skin, gl = load()
    ys = [p[1] for (_, P, _, _, _) in q for p in P]
    ymin, ymax = min(ys), max(ys)
    span = ymax - ymin
    # the guard is about 0.20 .. 0.33 of the way from the pommel to the tip
    pm.render_preview(ITEM, None, os.path.join(OUT, "closeup_guard_head.png"), views=("profile", "three_quarter", "edge"), panel=(420, 420), normals="vertex", focus=(ymin + span * 0.18, ymin + span * 0.40))


def state_pose(clip_name, t, yaw=0.0, pitch=0.0):
    """The 4x4 of the renderer for a clip at time t seconds: yaw, then the clip's position (px/16), the clip yaw, pitch + the clip's x, the clip's roll, then the blade laid along +z."""
    anim = json.load(open(os.path.join(ASSETS, "animations", "entity", "shishou_sword.animation.json")))["animations"]["animation.shishou." + clip_name]
    bone = anim["bones"]["sword"]
    length = anim["animation_length"]
    loop = anim.get("loop", True) is True
    tt = (t % length) if loop else min(t, length)

    def samp(track, default):
        if track is None:
            return np.array(default, float)
        ks = sorted((float(k), np.array(v, float)) for k, v in track.items())
        if tt <= ks[0][0]:
            return ks[0][1]
        for (t0, v0), (t1, v1) in zip(ks, ks[1:]):
            if t0 <= tt <= t1:
                return v0 + (v1 - v0) * ((tt - t0) / max(t1 - t0, 1e-6))
        return ks[-1][1]
    rot = samp(bone.get("rotation"), [0, 0, 0])
    pos = samp(bone.get("position"), [0, 0, 0])

    def Rx(a):
        c, s = math.cos(math.radians(a)), math.sin(math.radians(a))
        return np.array([[1, 0, 0], [0, c, -s], [0, s, c]])

    def Ry(a):
        c, s = math.cos(math.radians(a)), math.sin(math.radians(a))
        return np.array([[c, 0, s], [0, 1, 0], [-s, 0, c]])

    def Rz(a):
        c, s = math.cos(math.radians(a)), math.sin(math.radians(a))
        return np.array([[c, -s, 0], [s, c, 0], [0, 0, 1]])
    R = Ry(-yaw) @ Ry(rot[1]) @ Rx(pitch + rot[0]) @ Rz(rot[2]) @ Rx(90)
    return R, Ry(-yaw) @ (pos / 16.0)


def states():
    """The four states of the body: idle (two phases of the bob), aggro, attack (three moments), dormant: the blade seen from the side, +z to the right."""
    os.makedirs(OUT, exist_ok=True)
    d, q, skin, gl = load()
    ox, oy, ang, sc, ymin = d["place"]
    ys = [p[1] for (_, P, _, _, _) in q for p in P]
    mid = (min(ys) + max(ys)) / 2
    panels = []
    spec = [("idle", 0.0, "idle: bob up, blade 15 deg down", 90, 0), ("idle", 2.0, "idle: bob down", 90, 0), ("aggro", 0.05, "aggro: horizontal, jitter", 90, 0), ("attack", 0.2, "attack: pull back", 90, 0),
            ("attack", 0.3, "attack: thrust", 90, 0), ("attack", 0.4, "attack: slash (roll)", 125, 25), ("aggro", 0.05, "aggro, three-quarter", 125, 25), ("dormant", 0.5, "dormant: tip into the ground", 0, 0)]
    for clip, t, label, yw, pt in spec:
        R, off = state_pose(clip, t)
        qq = []
        for (l, P, UV, N, tn) in q:
            Pm = (P - [0, mid, 0]) * sc
            qq.append((l, Pm @ R.T + off, UV, N @ R.T, tn))
        img = pm.render_view(qq, yw, pt, (260, 220), 84.0 if clip != "dormant" else 80.0, (0.0, 0.0), skin, gl, d["glow"], ss=2, cull=True, vertex_normals=True)
        im = Image.fromarray((np.clip(img, 0, 1) * 255).astype(np.uint8))
        dr = ImageDraw.Draw(im)
        dr.text((6, 4), label, fill=(255, 255, 255))
        if yw == 90:
            dr.line([(0, 110), (260, 110)], fill=(255, 255, 255, 60), width=1)                  # the horizon of the entity
        panels.append(im)
    sheet = Image.new("RGB", (260 * 4, 220 * 2), (30, 34, 44))
    for i, im in enumerate(panels):
        sheet.paste(im, ((i % 4) * 260, (i // 4) * 220))
    sheet.save(os.path.join(OUT, "animation_states.png"))


def dormant():
    """The dormant sword standing in the ground (the tip 0.55 blocks into a grass block): the front and the side. The block is drawn over the buried part (a stand-in for the world)."""
    os.makedirs(OUT, exist_ok=True)
    d, q, skin, gl = load()
    ox, oy, ang, sc, ymin = d["place"]
    ys = [p[1] for (_, P, _, _, _) in q for p in P]
    mid = (min(ys) + max(ys)) / 2
    R, off = state_pose("dormant", 0.5)
    k, PW, PH = 90.0, 300, 380
    sheet = Image.new("RGB", (PW * 2, PH), (30, 34, 44))
    for i, yaw in enumerate((0, 90)):
        qq = []
        for (l, P, UV, N, tn) in q:
            Pm = (P - [0, mid, 0]) * sc
            qq.append((l, Pm @ R.T + off, UV, N @ R.T, tn))
        # the posed sword's lowest point is its tip: world y = 0 is the entity's feet = the tip; the ground top is 0.55 above it
        low = min((P_[:, 1].min() for (_, P_, _, _, _) in qq))
        img = pm.render_view(qq, yaw, 0, (PW, PH), k, (0.0, 0.9), skin, gl, d["glow"], ss=2, cull=True, vertex_normals=True)
        im = Image.fromarray((np.clip(img, 0, 1) * 255).astype(np.uint8))
        dr = ImageDraw.Draw(im)
        ground_y = int(PH / 2 - (low + 0.55 - 0.9) * k)
        dr.rectangle([0, ground_y, PW, PH], fill=(104, 78, 52))
        dr.rectangle([0, ground_y, PW, ground_y + 10], fill=(92, 140, 58))
        dr.text((8, 6), "dormant: front" if yaw == 0 else "dormant: side", fill=(255, 255, 255))
        sheet.paste(im, (i * PW, 0))
    sheet.save(os.path.join(OUT, "dormant_in_ground.png"))


def icon():
    os.makedirs(OUT, exist_ok=True)
    ic = Image.open(os.path.join(ASSETS, "textures", "item", ITEM + ".png")).convert("RGBA")
    big = ic.resize((ic.width * 8, ic.height * 8), Image.NEAREST)
    ref = Image.open(REF).convert("RGBA").crop((20, 100, 440, 1515))
    ref = ref.resize((int(ref.width * 256 / ref.height), 256), Image.LANCZOS)
    sheet = Image.new("RGBA", (big.width + ref.width + 48, max(big.height, ref.height) + 16), (84, 89, 107, 255))
    sheet.alpha_composite(ref, (12, 8))
    sheet.alpha_composite(big, (ref.width + 36, 8))
    sheet.convert("RGB").save(os.path.join(OUT, "inventory_icon.png"))


def hand():
    """The mesh placed and lit like the game's item preview (tools/item_preview/preview_weapons.py): the icon, front, three-quarter, edge, back."""
    os.makedirs(OUT, exist_ok=True)
    subprocess.run([sys.executable, "-B", os.path.join(ROOT, "tools", "item_preview", "preview_weapons.py"), ITEM], check=True, cwd=ROOT)
    src = os.path.join(ROOT, "build", "item_preview", "weapons.png")
    if os.path.exists(src):
        Image.open(src).convert("RGB").save(os.path.join(OUT, "in_hand_weapon_preview.png"))


def hud():
    """The HUD panel and an Appraisal readout, drawn with the rows of ShishouHud on a stand-in game view (the panel look is a stand-in for the mod's UiPanel)."""
    os.makedirs(OUT, exist_ok=True)
    W, S = 156, 3
    rows = [("Shishou", "Lv 37"), None, None, ("Mana 612 / 704", ""), ("Black Lightning (黒雷 Kurokaminari)", "35%"), ("Barrier  Sense", ""), None,
            ("Blaze", ""), ("Health 20 / 20", ""), ("Level ~34   Mana ~19", ""), ("Weak to: ice", ""), ("Skill: Fire Magic", "")]
    H = 128
    im = Image.new("RGB", (W * S + 40, H * S + 40), (46, 64, 52))
    d = ImageDraw.Draw(im)
    for y in range(0, im.size[1], 24):
        d.line([(0, y), (im.size[0], y)], fill=(54, 74, 60))
    x0, y0 = 20, 20
    d.rectangle([x0, y0, x0 + W * S, y0 + H * S], fill=(8, 13, 27), outline=(92, 216, 255), width=2)
    try:
        f = ImageFont.truetype("DejaVuSans.ttf", 10 * S // 2 + 4)
    except OSError:
        f = ImageFont.load_default()
    y = y0 + 4 * S
    d.text((x0 + 6 * S, y), "Shishou", fill=(230, 242, 255), font=f)
    d.text((x0 + W * S - 40 * S, y), "Lv 37", fill=(255, 215, 106), font=f)
    y += 12 * S
    d.rectangle([x0 + 4 * S, y, x0 + (W - 4) * S, y + 5 * S], fill=(20, 28, 48))
    d.rectangle([x0 + 4 * S, y, x0 + int(4 * S + (W - 8) * S * 0.44), y + 5 * S], fill=(255, 215, 106))
    y += 7 * S
    d.rectangle([x0 + 4 * S, y, x0 + (W - 4) * S, y + 5 * S], fill=(20, 28, 48))
    d.rectangle([x0 + 4 * S, y, x0 + int(4 * S + (W - 8) * S * 0.87), y + 5 * S], fill=(92, 216, 255))
    y += 6 * S
    d.text((x0 + 5 * S, y), "Mana 612 / 704", fill=(143, 163, 191), font=f)
    y += 11 * S
    d.text((x0 + 5 * S, y), "Black Lightning (Kurokaminari)", fill=(230, 242, 255), font=f)
    y += 11 * S
    d.text((x0 + 5 * S, y), "Barrier  Sense", fill=(124, 240, 176), font=f)
    y += 12 * S
    d.line([(x0 + 4 * S, y), (x0 + (W - 4) * S, y)], fill=(80, 90, 110))
    y += 4 * S
    for i, t in enumerate(["Blaze", "Health 20 / 20", "Level ~34   Mana ~19", "Weak to: ice", "Skill: Fire Magic"]):
        d.text((x0 + 5 * S, y), t, fill=(232, 196, 104) if i == 0 else (230, 242, 255), font=f)
        y += 11 * S
    im.save(os.path.join(OUT, "hud_readout.png"))


def main(names=None):
    names = names or ["compare", "closeup", "states", "dormant", "icon", "hud", "hand"]
    for n in names:
        globals()[n]()
        print("preview:", n)


if __name__ == "__main__":
    main(sys.argv[1:])
