"""Filmstrips of the Ultimate Magic charge animations (docs/magic/ultimate_animations.md), without the game.

    python3 tools/ultimate_anim_preview.py        ->  docs/previews/ultimate/anim_<kingdom>.png (6 frames of start, 6 of loop, 4 of release) + anim_kingdoms.png (overview)

A block-figure stand-in of the vanilla player is posed from the REAL keyframes in assets/nusmp/animations/player/cast.animation.json with the same rules as
client/CastAnimClient.pose: the ARMS are replaced by the clip, the LEGS add to the standing pose, the body and head are not driven; rotations use the model's
own order (Z * Y * X, degrees), linear interpolation between keys, loops wrap. No lighting, no skin, no robe, no blending between clips (each frame shows its clip at full
weight): it shows the SHAPE of the poses, not how the game looks. Needs numpy-free Pillow only.
"""
import json
import math
import os

from PIL import Image, ImageDraw, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.normpath(os.path.join(HERE, ".."))
FILE = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp", "animations", "player", "cast.animation.json")
OUT = os.path.join(ROOT, "docs", "previews", "ultimate")
KINGDOMS = {"clover": ((60, 140, 70), "Clover: defiant, arms thrown wide"), "diamond": ((70, 110, 190), "Diamond: disciplined, hands joined and slowly lifted"),
            "spade": ((95, 60, 130), "Spade: cold, arms crossed then thrust like blades"), "heart": ((200, 80, 110), "Heart: calm, one hand on the heart, the other opens")}
SKIN = (232, 190, 160)


# ---------------------------------------------------------------- the clips (the same sampling as client/geo/GeoAnim)
def load():
    with open(FILE) as f:
        return json.load(f)["animations"]


def sample_track(track, t):
    if not track:
        return (0.0, 0.0, 0.0)
    keys = sorted((float(k), v) for k, v in track.items())
    if len(keys) == 1 or t <= keys[0][0]:
        return tuple(keys[0][1])
    if t >= keys[-1][0]:
        return tuple(keys[-1][1])
    i = 0
    while i + 2 < len(keys) and t >= keys[i + 1][0]:
        i += 1
    (t0, a), (t1, b) = keys[i], keys[i + 1]
    k = 0.0 if t1 - t0 < 1e-6 else (t - t0) / (t1 - t0)
    return tuple(a[j] + (b[j] - a[j]) * k for j in range(3))


def pose_at(clip, seconds):
    length = clip["animation_length"]
    t = seconds % length if clip.get("loop", True) else min(seconds, length)
    out = {}
    for bone in ("left_arm", "right_arm", "left_leg", "right_leg"):
        b = clip["bones"].get(bone)
        out[bone] = sample_track(b.get("rotation") if b else None, t)
    return out


# ---------------------------------------------------------------- the stand-in
def rot(v, x, y, z):
    """Rotates v by Rz * Ry * Rx (degrees): the model part order of the game."""
    x, y, z = (math.radians(a) for a in (x, y, z))
    vx, vy, vz = v
    vy, vz = vy * math.cos(x) - vz * math.sin(x), vy * math.sin(x) + vz * math.cos(x)
    vx, vz = vx * math.cos(y) + vz * math.sin(y), -vx * math.sin(y) + vz * math.cos(y)
    vx, vy = vx * math.cos(z) - vy * math.sin(z), vx * math.sin(z) + vy * math.cos(z)
    return (vx, vy, vz)


def box(mn, mx):
    x0, y0, z0 = mn
    x1, y1, z1 = mx
    c = [(x0, y0, z0), (x1, y0, z0), (x1, y1, z0), (x0, y1, z0), (x0, y0, z1), (x1, y0, z1), (x1, y1, z1), (x0, y1, z1)]
    faces = [((0, 1, 2, 3), (0, 0, -1)), ((5, 4, 7, 6), (0, 0, 1)), ((4, 0, 3, 7), (-1, 0, 0)), ((1, 5, 6, 2), (1, 0, 0)), ((4, 5, 1, 0), (0, -1, 0)), ((3, 2, 6, 7), (0, 1, 0))]
    return c, faces


def parts(pose, cloth):
    """[(corners, faces, colour, pivot, (rx, ry, rz))] in model space (pixels, y down, the player looks toward -z); coordinates are absolute (the pivot is where the part turns)."""
    out = []
    out.append((*box((-4, -8, -4), (4, 0, 4)), SKIN, (0, 0, 0), (0, 0, 0)))                                  # head
    out.append((*box((-4, 0, -2), (4, 12, 2)), cloth, (0, 0, 0), (0, 0, 0)))                                 # body
    for side, sx in (("left", 1), ("right", -1)):
        ax, ay, az = pose[side + "_arm"]
        piv = (5 * sx, 2, 0)
        lo, hi = (4, 8) if sx == 1 else (-8, -4)
        out.append((*box((lo, 0, -2), (hi, 8, 2)), cloth, piv, (ax, ay, az)))                               # sleeve
        out.append((*box((lo, 8, -2), (hi, 12, 2)), SKIN, piv, (ax, ay, az)))                               # hand
        lx, ly, lz = pose[side + "_leg"]
        l0, l1 = (0, 4) if sx == 1 else (-4, 0)
        out.append((*box((l0, 12, -2), (l1, 24, 2)), tuple(int(c * 0.55) for c in cloth), (1.9 * sx, 12, 0), (lx, ly, lz)))   # leg
    return out


def draw_figure(d, pose, cloth, ox, oy, scale, yaw=28.0):
    polys = []
    cy, sy = math.cos(math.radians(yaw)), math.sin(math.radians(yaw))
    for corners, faces, colour, piv, (rx, ry, rz) in parts(pose, cloth):
        pts = []
        for c in corners:
            v = (c[0] - piv[0], c[1] - piv[1], c[2] - piv[2])
            v = rot(v, rx, ry, rz)
            v = (v[0] + piv[0], v[1] + piv[1], v[2] + piv[2])
            v = (v[0] * cy + v[2] * sy, v[1], -v[0] * sy + v[2] * cy)           # the 3/4 view
            pts.append(v)
        for idx, n in faces:
            nv = rot(n, rx, ry, rz)
            nv = (nv[0] * cy + nv[2] * sy, nv[1], -nv[0] * sy + nv[2] * cy)
            if nv[2] > 0.05:                                                       # faces away from the viewer (the viewer is at -z)
                continue
            depth = sum(pts[i][2] for i in idx) / 4.0
            shade = 0.62 + 0.38 * max(0.0, -nv[2] * 0.6 - nv[1] * 0.5 + nv[0] * -0.35)
            base = colour
            if colour == SKIN and piv == (0, 0, 0) and n != (0, 0, -1):             # the head: hair on every face but the front
                base = (70, 48, 36)
            col = tuple(min(255, int(c * shade)) for c in base)
            polys.append((depth, [(ox + pts[i][0] * scale, oy + pts[i][1] * scale) for i in idx], col))
    for depth, poly, col in sorted(polys, key=lambda p: -p[0]):
        d.polygon(poly, fill=col, outline=(20, 20, 28))


def font(size):
    try:
        return ImageFont.truetype("DejaVuSans.ttf", size)
    except OSError:
        return ImageFont.load_default()


def cell(pose, cloth, w, h, label):
    """Two views side by side: front 3/4 and from the player's left side (so arms pointing forward can be read)."""
    img = Image.new("RGB", (w, h), (26, 28, 40))
    d = ImageDraw.Draw(img)
    scale = h / 46.0
    oy = h * 0.27
    for i, yaw in enumerate((22.0, 88.0)):
        ox = w * (0.31 + 0.40 * i)
        d.ellipse((ox - 9 * scale, oy + 24 * scale - 3, ox + 9 * scale, oy + 24 * scale + 3), fill=(18, 18, 26))
        draw_figure(d, pose, cloth, ox, oy, scale, yaw)
    d.text((5, 3), label, fill=(210, 214, 230), font=font(11))
    return img


def frames_for(clips, kingdom):
    s, l, r = clips["ult_%s_start" % kingdom], clips["ult_%s_loop" % kingdom], clips["ult_%s_release" % kingdom]
    out = []
    for i in range(6):
        t = i * 0.1
        out.append(("start", "start  %d ticks" % round(t * 20), pose_at(s, t)))
    for i in range(6):
        t = i * l["animation_length"] / 6.0
        out.append(("loop", "loop  %.1f s" % t, pose_at(l, t)))
    for t in (0.0, 0.15, 0.25, 0.6):
        out.append(("release", "release  %d ticks" % round(t * 20), pose_at(r, t)))
    return out


def strip(clips, kingdom):
    cloth, title = KINGDOMS[kingdom]
    cw, ch = 300, 250
    fr = frames_for(clips, kingdom)
    img = Image.new("RGB", (cw * 6, 34 + ch * 3), (14, 15, 22))
    d = ImageDraw.Draw(img)
    d.text((10, 8), "Ultimate Magic charge animation, %s (stand-in figure posed from cast.animation.json; arms and legs only, as in the game)" % title, fill=(235, 238, 250), font=font(14))
    row = {"start": 0, "loop": 1, "release": 2}
    col = {"start": 0, "loop": 0, "release": 0}
    for phase, label, pose in fr:
        c = cell(pose, cloth, cw, ch, label)
        img.paste(c, (col[phase] * cw, 34 + row[phase] * ch))
        col[phase] += 1
    return img


def overview(clips):
    cw, ch = 300, 250
    names = list(KINGDOMS) + ["default"]
    img = Image.new("RGB", (cw * 3 + 170, 30 + ch * len(names)), (14, 15, 22))
    d = ImageDraw.Draw(img)
    d.text((10, 7), "Ultimate Magic wind-up poses by kingdom: wind-up pose (start), hold (loop), release (contact). Default = the old Mana Zone loop and Signature release.", fill=(235, 238, 250), font=font(13))
    for r, k in enumerate(names):
        y = 30 + r * ch
        if k == "default":
            cloth = (110, 110, 120)
            picks = [("", pose_at(clips["cast_mana_zone"], 0.0)), ("", pose_at(clips["cast_mana_zone"], 0.6)), ("", pose_at(clips["cast_signature"], 0.5))]
            title = "default / no kingdom"
        else:
            cloth, title = KINGDOMS[k]
            picks = [("", pose_at(clips["ult_%s_start" % k], 0.5)), ("", pose_at(clips["ult_%s_loop" % k], clips["ult_%s_loop" % k]["animation_length"] * 0.4)), ("", pose_at(clips["ult_%s_release" % k], 0.2))]
        d.text((8, y + 8), title.replace(": ", ":\n", 1), fill=(220, 224, 240), font=font(12))
        for c, (_, pose) in enumerate(picks):
            img.paste(cell(pose, cloth, cw, ch, ("start", "loop", "release")[c]), (170 + c * cw, y))
    return img


def main():
    clips = load()
    os.makedirs(OUT, exist_ok=True)
    for k in KINGDOMS:
        strip(clips, k).save(os.path.join(OUT, "anim_%s.png" % k))
    overview(clips).save(os.path.join(OUT, "anim_kingdoms.png"))
    print("wrote", len(KINGDOMS) + 1, "images to", OUT)


if __name__ == "__main__":
    main()
