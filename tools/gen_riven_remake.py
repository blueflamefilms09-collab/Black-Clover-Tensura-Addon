"""Writes Riven Remake's model, animation clips and textures (run from the repo root; needs only the Python standard library):
  src/main/resources/assets/nusmp/geo/entity/riven_remake.geo.json
  src/main/resources/assets/nusmp/animations/entity/riven_remake.animation.json
  src/main/resources/assets/nusmp/textures/entity/riven_remake{,_glow,_final,_final_glow}.png
Final Form is a second texture on the same model: the page-wings and the bull-skull crown are transparent in the base texture and painted in the
final one, so the boss never swaps entity. Signs of animation rotations follow client/geo/GeoDraw (check them with the preview if a limb turns the wrong way)."""
import json, math, os, struct, zlib

RES = "src/main/resources/assets/nusmp"
TW, TH = 128, 96

# ------------------------------------------------------------------ model
def cube(origin, size, uv): return {"origin": origin, "size": size, "uv": uv}
BONES = [
    {"name": "root", "pivot": [0, 0, 0]},
    {"name": "body", "parent": "root", "pivot": [0, 12, 0], "cubes": [cube([-4, 12, -2], [8, 12, 4], [0, 16])]},
    {"name": "head", "parent": "body", "pivot": [0, 24, 0], "cubes": [cube([-4, 24, -4], [8, 8, 8], [0, 0]), cube([-4.5, 31, -4.5], [9, 2, 9], [32, 0])]},
    {"name": "crown", "parent": "head", "pivot": [0, 32, 0], "cubes": [cube([-2.5, 32.5, -1.5], [5, 4, 3], [64, 44])]},
    {"name": "arm_r", "parent": "body", "pivot": [-6, 22, 0], "cubes": [cube([-7.5, 10, -1.5], [3, 12, 3], [24, 32])]},
    {"name": "arm_l", "parent": "body", "pivot": [6, 22, 0], "cubes": [cube([4.5, 10, -1.5], [3, 12, 3], [24, 32])]},
    {"name": "leg_r", "parent": "root", "pivot": [-2, 12, 0], "cubes": [cube([-4, 0, -2], [4, 12, 4], [0, 32])]},
    {"name": "leg_l", "parent": "root", "pivot": [2, 12, 0], "cubes": [cube([0, 0, -2], [4, 12, 4], [0, 32])]},
    {"name": "coat", "parent": "body", "pivot": [0, 13, 2], "cubes": [cube([-4, 3, 2], [8, 10, 1], [40, 16])]},
    {"name": "grimoire", "parent": "root", "pivot": [10, 17, 0], "cubes": [cube([8, 14, -2.5], [5, 7, 1], [64, 0])]},
    {"name": "wing_r", "parent": "body", "pivot": [-3, 22, 2.5], "cubes": [cube([-4, 12, 2.5], [1, 14, 10], [64, 16])]},
    {"name": "wing_l", "parent": "body", "pivot": [3, 22, 2.5], "cubes": [cube([3, 12, 2.5], [1, 14, 10], [64, 16])]},
]
GEO = {"format_version": "1.12.0", "minecraft:geometry": [{"description": {"identifier": "geometry.riven_remake", "texture_width": TW, "texture_height": TH,
        "visible_bounds_width": 4, "visible_bounds_height": 4, "visible_bounds_offset": [0, 1, 0]}, "bones": BONES}]}

# ------------------------------------------------------------------ animations
S = math.sin
def z(): return [0, 0, 0]
def clip(length, loop, fn, step=0.05):
    n = max(1, int(round(length / step)))
    bones = {}
    for i in range(n + 1):
        t = min(length, i * step)
        frame = fn(t, t / length if length else 0)
        for b, ch in frame.items():
            for k, v in ch.items():
                bones.setdefault(b, {}).setdefault(k, {})["%.2f" % t] = [round(x, 3) for x in v]
    return {"loop": loop, "animation_length": length, "bones": bones}

def idle(t, p):
    a = t * 2 * math.pi / 4.0
    return {"body": {"rotation": [1.5 * S(a), 2 * S(a / 2), 0]}, "head": {"rotation": [2 * S(a + 1), 6 * S(a / 2), 0]},
            "arm_r": {"rotation": [0, 0, 4 + 2 * S(a)]}, "arm_l": {"rotation": [0, 0, -4 - 2 * S(a)]}, "coat": {"rotation": [3 + 2 * S(a), 0, 0]},
            "grimoire": {"position": [0, 1.2 * S(a * 2), 0], "rotation": [0, 25 * S(a), 0]}}
def walk(t, p):
    a = p * 2 * math.pi
    return {"leg_r": {"rotation": [35 * S(a), 0, 0]}, "leg_l": {"rotation": [-35 * S(a), 0, 0]}, "arm_r": {"rotation": [-25 * S(a), 0, 4]}, "arm_l": {"rotation": [25 * S(a), 0, -4]},
            "body": {"rotation": [3, 4 * S(a), 0]}, "coat": {"rotation": [14 + 8 * S(a * 2), 0, 0]}, "grimoire": {"position": [0, 1 * S(a * 2), -1], "rotation": [0, 30 * S(a), 0]}}
def strafe(t, p):
    f = walk(t, p)
    f["body"] = {"rotation": [3, -18, 0]}
    return f
def talk(t, p):
    return {"head": {"rotation": [6 * S(p * 6 * math.pi), 8 * S(p * 2 * math.pi), 0]}, "arm_r": {"rotation": [-50 * S(p * math.pi), 0, 10]}, "body": {"rotation": [0, 5 * S(p * 2 * math.pi), 0]},
            "grimoire": {"position": [0, 2 * S(p * math.pi), 0]}}
def cast_grimoire(t, p):
    r = S(p * math.pi)
    return {"arm_l": {"rotation": [-85 * r, 0, -10]}, "arm_r": {"rotation": [-40 * r, 0, 10]}, "body": {"rotation": [-6 * r, 0, 0]}, "head": {"rotation": [-8 * r, 0, 0]},
            "grimoire": {"position": [-6 * r, 3 * r, -3 * r], "rotation": [0, 360 * p, 0]}}
def cast_song(t, p):
    r = S(p * math.pi)
    return {"arm_l": {"rotation": [-65 * r, 0, -25 * r]}, "arm_r": {"rotation": [-65 * r, 0, 25 * r]}, "head": {"rotation": [-20 * r, 8 * S(p * 6 * math.pi), 0]},
            "body": {"rotation": [-4 * r, 5 * S(p * 4 * math.pi), 0]}, "coat": {"rotation": [8 * r, 0, 0]}}
def eldritch_blast(t, p):
    r = S(min(p * 2, 1) * math.pi / 2) if p < 0.5 else 1 - (p - 0.5) * 1.2
    return {"arm_r": {"rotation": [-90 * r, 0, 0]}, "body": {"rotation": [8 * max(0, p - 0.4), 0, 0]}, "head": {"rotation": [-4 * r, 0, 0]}}
def shadow_step(t, p):
    r = S(p * math.pi)
    return {"body": {"rotation": [25 * r, 0, 0]}, "root": {"scale": [1 + 0.1 * r, 1 - 0.15 * r, 1 + 0.1 * r]}, "coat": {"rotation": [-20 * r, 0, 0]}}
def manifest_weapon(t, p):
    r = S(p * math.pi)
    return {"arm_l": {"rotation": [-100 * r, 0, -15]}, "arm_r": {"rotation": [-100 * r, 0, 15]}, "grimoire": {"position": [-8 * r, 6 * r, -2 * r], "rotation": [0, 720 * p, 0]}, "head": {"rotation": [-10 * r, 0, 0]}}
def manifest_shield(t, p):
    r = S(p * math.pi)
    return {"arm_l": {"rotation": [-90 * r, 0, 25 * r]}, "arm_r": {"rotation": [-60 * r, 0, -10 * r]}, "body": {"rotation": [-5 * r, -10 * r, 0]}}
def soul_bond(t, p):
    r = S(p * math.pi)
    return {"arm_r": {"rotation": [-80 * r, 0, -10]}, "head": {"rotation": [0, -12 * r, 0]}, "body": {"rotation": [5 * r, -10 * r, 0]}}
def combo(k):
    def f(t, p):
        r = S(p * math.pi)
        if k == 1: return {"arm_r": {"rotation": [-130 + 160 * p, 0, 0]}, "body": {"rotation": [0, -20 + 40 * p, 0]}}
        if k == 2: return {"arm_r": {"rotation": [-60, 90 - 180 * p, 0]}, "body": {"rotation": [0, 30 - 60 * p, 0]}, "leg_r": {"rotation": [-15 * r, 0, 0]}}
        return {"arm_r": {"rotation": [-170 * (1 - p) + 20 * p, 0, 0]}, "arm_l": {"rotation": [-170 * (1 - p), 0, 0]}, "body": {"rotation": [-10 + 25 * p, 0, 0]}}
    return f
def hit(t, p):
    r = S(p * math.pi)
    return {"body": {"rotation": [-15 * r, 0, 0]}, "head": {"rotation": [-12 * r, 0, 0]}, "coat": {"rotation": [20 * r, 0, 0]}}
def stagger(t, p):
    r = S(min(p * 2, 1) * math.pi / 2)
    return {"body": {"rotation": [22 * r + 3 * S(p * 8 * math.pi), 0, 0]}, "head": {"rotation": [25 * r, 0, 0]}, "arm_r": {"rotation": [0, 0, 20 * r]}, "arm_l": {"rotation": [0, 0, -20 * r]}}
def phase2(t, p):
    r = S(p * math.pi)
    return {"arm_r": {"rotation": [0, 0, 70 * r]}, "arm_l": {"rotation": [0, 0, -70 * r]}, "body": {"rotation": [-10 * r, 0, 0]}, "head": {"rotation": [-22 * r, 0, 0]},
            "grimoire": {"position": [0, 6 * r, 0], "rotation": [0, 1080 * p, 0]}, "root": {"position": [0, 1.5 * r, 0]}}
def final_form(t, p):
    r = min(1, p * 2) * (1 if p < 0.85 else (1 - p) / 0.15 * 0.3 + 0.7)
    return {"arm_r": {"rotation": [0, 0, 80 * r]}, "arm_l": {"rotation": [0, 0, -80 * r]}, "body": {"rotation": [-12 * r, 0, 0]}, "head": {"rotation": [-25 * r, 0, 0]},
            "root": {"position": [0, 4 * r, 0]}, "wing_r": {"rotation": [0, 0, 35 * r]}, "wing_l": {"rotation": [0, 0, -35 * r]},
            "grimoire": {"position": [0, 8 * r, 0], "rotation": [0, 1440 * p, 0]}}
def death(t, p):
    r = min(1, p * 1.6)
    return {"body": {"rotation": [-80 * r, 0, 0]}, "head": {"rotation": [15 * r, 0, 0]}, "leg_r": {"rotation": [-10 * r, 0, 0]}, "leg_l": {"rotation": [-10 * r, 0, 0]},
            "arm_r": {"rotation": [0, 0, 40 * r]}, "arm_l": {"rotation": [0, 0, -40 * r]}, "root": {"position": [0, -10 * r, 6 * r]},
            "grimoire": {"position": [0, -8 * p, 0], "rotation": [0, 90 * p, 0]}}

ANIMS = {"idle": clip(4.0, True, idle), "walk": clip(1.0, True, walk), "strafe": clip(1.0, True, strafe), "talk": clip(1.2, False, talk),
         "cast_grimoire": clip(0.8, False, cast_grimoire), "cast_song": clip(1.6, False, cast_song), "eldritch_blast": clip(0.45, False, eldritch_blast),
         "shadow_step": clip(0.35, False, shadow_step), "manifest_weapon": clip(0.6, False, manifest_weapon), "manifest_shield": clip(0.5, False, manifest_shield),
         "soul_bond": clip(0.7, False, soul_bond), "sword_combo_1": clip(0.4, False, combo(1)), "sword_combo_2": clip(0.4, False, combo(2)),
         "sword_combo_3": clip(0.4, False, combo(3)), "hit": clip(0.3, False, hit), "stagger": clip(0.8, False, stagger), "phase2": clip(1.4, False, phase2),
         "final_form": clip(2.2, False, final_form), "death": clip(2.0, "hold_on_last_frame", death)}

# ------------------------------------------------------------------ textures
class Img:
    def __init__(self): self.px = [[(0, 0, 0, 0)] * TW for _ in range(TH)]
    def rect(self, x, y, w, h, c):
        for yy in range(int(y), int(y + h)):
            for xx in range(int(x), int(x + w)):
                if 0 <= xx < TW and 0 <= yy < TH: self.px[yy][xx] = c
    def save(self, path):
        raw = b"".join(b"\x00" + b"".join(bytes(p) for p in row) for row in self.px)
        def chunk(t, d): return struct.pack(">I", len(d)) + t + d + struct.pack(">I", zlib.crc32(t + d) & 0xFFFFFFFF)
        data = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", TW, TH, 8, 6, 0, 0, 0)) + chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b"")
        os.makedirs(os.path.dirname(path), exist_ok=True)
        open(path, "wb").write(data)

COAT, COAT2, SILVER, SKIN, HAIR = (20, 17, 28, 255), (30, 25, 44, 255), (200, 205, 225, 255), (222, 212, 214, 255), (14, 12, 20, 255)
VIOLET, BLUE, WHITE = (150, 110, 255, 255), (90, 160, 255, 255), (235, 230, 255, 255)

def box_uv(sx, sy, sz): return 2 * (sx + sz), sz + sy
def face(u, v, sx, sy, sz, which):                              # (x, y, w, h) of a box face in the unfolded layout
    return {"top": (u + sz, v, sx, sz), "bottom": (u + sz + sx, v, sx, sz), "right": (u, v + sz, sz, sy), "front": (u + sz, v + sz, sx, sy),
            "left": (u + sz + sx, v + sz, sz, sy), "back": (u + 2 * sz + sx, v + sz, sx, sy)}[which]

def paint(final):
    im, gl = Img(), Img()
    def fill(u, v, sx, sy, sz, c):
        w, h = box_uv(sx, sy, sz); im.rect(u, v, w, h, c)
    fill(0, 0, 8, 8, 8, SKIN)                                                    # head
    x, y, w, h = face(0, 0, 8, 8, 8, "top"); im.rect(x, y, w, h, HAIR)
    x, y, w, h = face(0, 0, 8, 8, 8, "back"); im.rect(x, y, w, h, HAIR)
    for side in ("left", "right"): x, y, w, h = face(0, 0, 8, 8, 8, side); im.rect(x, y, w, 3, HAIR); im.rect(x, y + 3, w, h - 3, SKIN)
    x, y, w, h = face(0, 0, 8, 8, 8, "front")
    im.rect(x, y, w, 3, HAIR); im.rect(x + 1, y + 3, 2, 1, HAIR); im.rect(x + 5, y + 3, 1, 1, HAIR)   # messy fringe
    im.rect(x + 1, y + 4, 2, 1, VIOLET); im.rect(x + 5, y + 4, 2, 1, VIOLET)                          # the eyes
    im.rect(x + 3, y + 6, 2, 1, (150, 120, 125, 255))
    gl.rect(x + 1, y + 4, 2, 1, VIOLET if not final else WHITE); gl.rect(x + 5, y + 4, 2, 1, VIOLET if not final else WHITE)
    fill(32, 0, 9, 2, 9, HAIR)                                                   # hair top
    fill(0, 16, 8, 12, 4, COAT)                                                  # body: high-collar coat
    x, y, w, h = face(0, 16, 8, 12, 4, "front")
    im.rect(x + 3, y, 2, 12, COAT2); im.rect(x, y, 8, 2, COAT2)                  # the collar
    for i in range(1, 12, 2): im.rect(x + 1, y + i, 1, 1, SILVER); im.rect(x + 6, y + i, 1, 1, SILVER)   # silver filigree
    im.rect(x + 3, y + 4, 2, 3, WHITE); im.rect(x + 3, y + 5, 1, 1, COAT)       # the Black Bull skull
    gl.rect(x + 3, y + 4, 2, 3, BLUE); gl.rect(x + 2, y + 9, 4, 1, VIOLET)
    fill(0, 32, 4, 12, 4, COAT)                                                  # legs
    fill(24, 32, 3, 12, 3, COAT)                                                 # arms
    x, y, w, h = face(24, 32, 3, 12, 3, "front"); im.rect(x, y + 10, 3, 2, SKIN)
    x, y, w, h = face(24, 32, 3, 12, 3, "right"); gl.rect(x, y + 3, 3, 1, BLUE); gl.rect(x, y + 6, 3, 1, VIOLET)   # lightning up the arm
    fill(40, 16, 8, 10, 1, COAT2)                                                # coat tails
    gl.rect(41, 18, 1, 8, VIOLET)
    fill(64, 0, 5, 7, 1, (40, 28, 70, 255))                                      # the floating grimoire
    x, y, w, h = face(64, 0, 5, 7, 1, "front"); im.rect(x + 1, y + 1, 3, 5, SILVER)
    gl.rect(x + 1, y + 2, 3, 3, VIOLET if not final else WHITE)
    if final:
        fill(64, 16, 1, 14, 10, (220, 225, 255, 235))                           # page-wings
        for k in range(0, 24, 4): im.rect(64, 16 + k, 22, 1, (150, 150, 190, 255))
        gl.rect(66, 18, 18, 20, (60, 50, 120, 255))
        fill(64, 44, 5, 4, 3, WHITE)                                             # the crown of the bull skull
        x, y, w, h = face(64, 44, 5, 4, 3, "front"); im.rect(x + 1, y + 1, 1, 1, HAIR); im.rect(x + 3, y + 1, 1, 1, HAIR)
        gl.rect(x + 1, y + 1, 1, 1, WHITE); gl.rect(x + 3, y + 1, 1, 1, WHITE)
        gl.rect(0, 16, 24, 16, (40, 30, 90, 255))                                # coat lightning, full violet
    return im, gl

if __name__ == "__main__":
    def dump(path, obj):
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "w") as f: json.dump(obj, f, indent=1); f.write("\n")
    dump(RES + "/geo/entity/riven_remake.geo.json", GEO)
    dump(RES + "/animations/entity/riven_remake.animation.json", {"format_version": "1.8.0", "animations": ANIMS})
    for final, name in ((False, "riven_remake"), (True, "riven_remake_final")):
        im, gl = paint(final)
        im.save("%s/textures/entity/%s.png" % (RES, name)); gl.save("%s/textures/entity/%s_glow.png" % (RES, name))
    print("ok")
