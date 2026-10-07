"""
Generates the textures the render preview needs (run it again only to change them; the PNGs are committed):

    python tools/render_preview/gen_assets.py

assets/textures/preview/skin_steve.png, skin_alex.png   the stand-in player (wide / slim arms), a blue-shirt Steve-like skin with a face,
                                                         a red headband in the hat layer and a belt in the jacket layer
assets/textures/prop/preview_*.png                       textures of the prop primitives example (checker with holes, glass, glow)
assets/textures/aura/preview_*.png                       textures of the aura example, laid out like a 64x64 player skin
assets/minecraft/textures/misc/white.png                 vanilla's white square (some painters use it)
assets/textures/entity/preview_*.png (+ geo/ json)       the geo test models: face-labelled cube, bone / animation test, see gen_geo_assets()

Textures are looked up in src/main/resources/assets/nusmp first, then here (assets/<namespace>/<path>, and assets/<path> for nusmp).
"""
import math
import os
import sys

sys.dont_write_bytecode = True

from PIL import Image, ImageDraw, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "assets")


def save(img, rel):
    path = os.path.join(OUT, rel)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path)


# ---------------------------------------------------------------------------------------------------- the player skin layout (1.21.1)
def box_faces(u0, v0, w, h, d):
    """The six face rectangles (x0, y0, x1, y1) of ModelPart.Cube's unfolded box at texOffs (u0, v0)."""
    return {
        "right": (u0, v0 + d, u0 + d, v0 + d + h),                    # model -x, the wearer's right
        "front": (u0 + d, v0 + d, u0 + d + w, v0 + d + h),            # model -z
        "left": (u0 + d + w, v0 + d, u0 + 2 * d + w, v0 + d + h),
        "back": (u0 + 2 * d + w, v0 + d, u0 + 2 * d + 2 * w, v0 + d + h),
        "top": (u0 + d, v0, u0 + d + w, v0 + d),
        "bottom": (u0 + d + w, v0, u0 + d + 2 * w, v0 + d),
    }


def skin_parts(slim):
    aw = 3 if slim else 4
    return {
        "head": (0, 0, 8, 8, 8), "hat": (32, 0, 8, 8, 8),
        "body": (16, 16, 8, 12, 4), "jacket": (16, 32, 8, 12, 4),
        "right_arm": (40, 16, aw, 12, 4), "right_sleeve": (40, 32, aw, 12, 4),
        "left_arm": (32, 48, aw, 12, 4), "left_sleeve": (48, 48, aw, 12, 4),
        "right_leg": (0, 16, 4, 12, 4), "right_pants": (0, 32, 4, 12, 4),
        "left_leg": (16, 48, 4, 12, 4), "left_pants": (0, 48, 4, 12, 4),
    }


SKIN = (205, 150, 112, 255)
SKIN_DARK = (176, 120, 88, 255)
HAIR = (74, 50, 30, 255)
SHIRT = (52, 112, 196, 255)
SHIRT_DARK = (38, 86, 160, 255)
PANTS = (58, 58, 120, 255)
SHOE = (96, 96, 100, 255)


def fill(d, rect, color):
    x0, y0, x1, y1 = rect
    d.rectangle((x0, y0, x1 - 1, y1 - 1), fill=color)


def px(d, x, y, color):
    d.point((x, y), fill=color)


def make_skin(slim):
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    parts = skin_parts(slim)
    # head: skin, hair on top, back and the upper side / front rows
    hf = box_faces(*parts["head"])
    for name, rect in hf.items():
        fill(d, rect, SKIN)
    fill(d, hf["top"], HAIR)
    fill(d, hf["back"], HAIR)
    x0, y0, x1, y1 = hf["front"]
    fill(d, (x0, y0, x1, y0 + 2), HAIR)
    for x in (x0, x0 + 1, x1 - 2, x1 - 1):                              # sideburns
        fill(d, (x, y0 + 2, x + 1, y0 + 3), HAIR)
    px(d, x0 + 1, y0 + 4, (255, 255, 255, 255)); px(d, x0 + 2, y0 + 4, (84, 70, 168, 255))                      # eyes
    px(d, x1 - 2, y0 + 4, (255, 255, 255, 255)); px(d, x1 - 3, y0 + 4, (84, 70, 168, 255))
    fill(d, (x0 + 3, y0 + 5, x0 + 5, y0 + 6), SKIN_DARK)                                                          # nose
    fill(d, (x0 + 2, y0 + 6, x0 + 6, y0 + 7), (150, 82, 70, 255))                                               # mouth
    for name in ("right", "left"):
        sx0, sy0, sx1, sy1 = hf[name]
        fill(d, (sx0, sy0, sx1, sy0 + 3), HAIR)
        fill(d, (sx1 - 3 if name == "right" else sx0, sy0 + 3, sx1 if name == "right" else sx0 + 3, sy0 + 5), HAIR)
    # body: shirt with a darker collar band and a chest pocket on the front
    bf = box_faces(*parts["body"])
    for name, rect in bf.items():
        fill(d, rect, SHIRT)
    fill(d, bf["bottom"], PANTS)
    fx0, fy0, fx1, fy1 = bf["front"]
    fill(d, (fx0 + 2, fy0, fx1 - 2, fy0 + 1), SHIRT_DARK)
    fill(d, (fx0 + 5, fy0 + 3, fx0 + 7, fy0 + 5), SHIRT_DARK)
    bx0, by0, bx1, by1 = bf["back"]
    fill(d, (bx0 + 2, by0, bx1 - 2, by0 + 1), SHIRT_DARK)
    # arms: sleeve (shirt) for the top 4 rows, bare below
    for arm in ("right_arm", "left_arm"):
        af = box_faces(*parts[arm])
        for name, rect in af.items():
            fill(d, rect, SKIN)
        for name in ("right", "front", "left", "back"):
            x0, y0, x1, y1 = af[name]
            fill(d, (x0, y0, x1, y0 + 4), SHIRT)
            fill(d, (x0, y0 + 4, x1, y0 + 5), SHIRT_DARK)
        fill(d, af["top"], SHIRT)
    # legs: trousers, shoes for the last 2 rows
    for leg in ("right_leg", "left_leg"):
        lf = box_faces(*parts[leg])
        for name, rect in lf.items():
            fill(d, rect, PANTS)
        for name in ("right", "front", "left", "back"):
            x0, y0, x1, y1 = lf[name]
            fill(d, (x0, y1 - 2, x1, y1), SHOE)
        fill(d, lf["bottom"], SHOE)
    # outer layers: a red headband (hat) and a belt with a buckle (jacket); the rest stays transparent
    hat = box_faces(*parts["hat"])
    for name in ("right", "front", "left", "back"):
        x0, y0, x1, y1 = hat[name]
        fill(d, (x0, y0 + 2, x1, y0 + 3), (210, 40, 40, 255))
    jf = box_faces(*parts["jacket"])
    for name in ("right", "front", "left", "back"):
        x0, y0, x1, y1 = jf[name]
        fill(d, (x0, y1 - 3, x1, y1 - 1), (70, 44, 24, 255))
    x0, y0, x1, y1 = jf["front"]
    fill(d, (x0 + 3, y1 - 3, x0 + 5, y1 - 1), (240, 200, 60, 255))
    return img


# ---------------------------------------------------------------------------------------------------- prop primitive textures
def make_checker():
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    for j in range(8):
        for i in range(8):
            d.rectangle((i * 8, j * 8, i * 8 + 7, j * 8 + 7), fill=(226, 170, 70, 255) if (i + j) % 2 == 0 else (150, 96, 36, 255))
    for cx, cy in ((16, 16), (48, 16), (16, 48), (48, 48)):                                   # holes: the cutout test
        d.ellipse((cx - 6, cy - 6, cx + 6, cy + 6), fill=(0, 0, 0, 0))
    d.rectangle((0, 0, 5, 5), fill=(230, 40, 40, 255))                                        # corners: red top-left, green top-right,
    d.rectangle((58, 0, 63, 5), fill=(40, 200, 60, 255))                                      # blue bottom-left, white bottom-right
    d.rectangle((0, 58, 5, 63), fill=(50, 90, 235, 255))
    d.rectangle((58, 58, 63, 63), fill=(255, 255, 255, 255))
    d.polygon([(32, 20), (40, 34), (35, 34), (35, 46), (29, 46), (29, 34), (24, 34)], fill=(30, 30, 40, 255))   # an arrow: up = v = 0
    return img


def make_glass():
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    px_ = img.load()
    for y in range(32):
        for x in range(32):
            e = min(x, y, 31 - x, 31 - y) / 15.5
            a = 0.62 - 0.40 * e
            if abs((x + y) - 24) < 3:
                a = max(a, 0.8)
            px_[x, y] = (210, 235, 255, int(255 * max(0.0, min(1.0, a))))
    return img


def make_glow():
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    px_ = img.load()
    for y in range(64):
        for x in range(64):
            r = math.hypot(x - 31.5, y - 31.5) / 31.5
            a = max(0.0, 1.0 - r) ** 1.6
            px_[x, y] = (255, 255, 255, int(255 * a))
    return img


# ---------------------------------------------------------------------------------------------------- aura example textures (player skin layout)
PART_TINT = {"head": (232, 190, 70), "body": (214, 140, 60), "right_arm": (190, 110, 50), "left_arm": (190, 110, 50),
             "right_leg": (150, 86, 40), "left_leg": (150, 86, 40)}


def make_aura_overlay():
    """Plated armour: dark seams, lighter plates, a hole in the middle of each face (alpha 0, so the skin shows through)."""
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    parts = skin_parts(False)
    for name, tint in PART_TINT.items():
        for face, (x0, y0, x1, y1) in box_faces(*parts[name]).items():
            w, h = x1 - x0, y1 - y0
            fill(d, (x0, y0, x1, y1), tint + (255,))
            fill(d, (x0, y0, x1, y0 + 1), tuple(int(c * 0.45) for c in tint) + (255,))
            fill(d, (x0, y1 - 1, x1, y1), tuple(int(c * 0.45) for c in tint) + (255,))
            fill(d, (x0, y0, x0 + 1, y1), tuple(int(c * 0.45) for c in tint) + (255,))
            fill(d, (x1 - 1, y0, x1, y1), tuple(int(c * 0.45) for c in tint) + (255,))
            if w >= 4 and h >= 4:
                hx0, hy0 = x0 + w // 2 - (1 if w > 4 else 0), y0 + h // 2 - 1
                fill(d, (hx0, hy0, hx0 + (3 if w > 4 else 2), hy0 + 2), (0, 0, 0, 0))
    # the face of the helmet: a visor slit on the head's front
    hx0, hy0, hx1, hy1 = box_faces(*parts["head"])["front"]
    fill(d, (hx0 + 1, hy0 + 3, hx1 - 1, hy0 + 5), (0, 0, 0, 0))
    return img


def make_aura_glass():
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    parts = skin_parts(False)
    for name in PART_TINT:
        for face, (x0, y0, x1, y1) in box_faces(*parts[name]).items():
            fill(d, (x0, y0, x1, y1), (150, 215, 255, 120))
            d.rectangle((x0, y0, x1 - 1, y1 - 1), outline=(220, 245, 255, 210))
    return img


def make_aura_glow():
    """A rim: bright on the border of every face, empty in the middle (the additive halo outlines the body)."""
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    parts = skin_parts(False)
    for name in PART_TINT:
        for face, (x0, y0, x1, y1) in box_faces(*parts[name]).items():
            fill(d, (x0, y0, x1, y1), (255, 255, 255, 26))
            d.rectangle((x0, y0, x1 - 1, y1 - 1), outline=(255, 255, 255, 190))
    return img


def main():
    save(make_skin(False), "textures/preview/skin_steve.png")
    save(make_skin(True), "textures/preview/skin_alex.png")
    save(make_checker(), "textures/prop/preview_checker.png")
    save(make_glass(), "textures/prop/preview_glass.png")
    save(make_glow(), "textures/prop/preview_glow.png")
    save(make_aura_overlay(), "textures/aura/preview_overlay.png")
    save(make_aura_glass(), "textures/aura/preview_aura_glass.png")
    save(make_aura_glow(), "textures/aura/preview_aura_glow.png")
    save(Image.new("RGBA", (16, 16), (255, 255, 255, 255)), "minecraft/textures/misc/white.png")
    try:
        import gen_geo_assets                      # noqa: F401  (the geo test models, added with the geo scenes)
        gen_geo_assets.main(save)
    except ImportError:
        pass
    print("assets written to", OUT)


if __name__ == "__main__":
    sys.path.insert(0, HERE)
    main()
