"""Draws the five Berserk weapon skill icons (32x32, rounded corners, gold ring frame like the other Ultimates): Lord of Slothful Weight (a drooping axe), Lord of Green-Eyed Envy (a slit eye), Lord of Empty Vanity
(a hand mirror), Lord of the Twin Rings (two linked rings), Seifort's Holy Judgement (a radiant cross-sword). Original art, drawn from scratch with Pillow at 4x and shrunk.
Run: python tools/gen_sin_skill_icons.py -> src/main/resources/assets/anime_multiverse/textures/skill/<id>.png (ids = sinweapons.SinSkillRules.skillId)."""
import math
import os
from PIL import Image, ImageDraw

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
OUT = os.path.join(ROOT, "src", "main", "resources", "assets", "anime_multiverse", "textures", "skill")
S, K = 32, 4
W = S * K
GOLD = (255, 236, 150, 255)
GOLD_DARK = (160, 112, 40, 255)


def bg(rgb):
    """Dark radial background tinted with the weapon's colour (lit centre, black edge)."""
    im = Image.new("RGBA", (W, W))
    px = im.load()
    c = (W - 1) / 2.0
    for y in range(W):
        for x in range(W):
            d = min(1.0, math.hypot(x - c, y - c) / (W * 0.62))
            f = (1.0 - d) ** 1.4
            px[x, y] = (int(10 + (rgb[0] * 0.38) * f), int(8 + (rgb[1] * 0.38) * f), int(14 + (rgb[2] * 0.38) * f), 255)
    return im


def u(v):
    return v * K


def sloth(d, rgb):
    d.line([(u(10), u(27)), (u(21), u(7))], fill=(70, 56, 90, 255), width=u(2))                # haft
    d.polygon([(u(16), u(7)), (u(26), u(6)), (u(27), u(16)), (u(20), u(14)), (u(17), u(11))], fill=(60, 50, 80, 255))   # head
    d.polygon([(u(20), u(7)), (u(26), u(6)), (u(27), u(16)), (u(24), u(13))], fill=rgb + (255,))
    for i in range(3):                                                                            # drips: the weight drags down
        d.line([(u(22 + i * 2), u(17)), (u(22 + i * 2), u(21 + i))], fill=rgb + (255,), width=u(1))


def envy(d, rgb):
    d.polygon([(u(4), u(16)), (u(10), u(10)), (u(22), u(10)), (u(28), u(16)), (u(22), u(22)), (u(10), u(22))], fill=(14, 40, 20, 255), outline=rgb + (255,))
    d.ellipse([u(11), u(11), u(21), u(21)], fill=rgb + (255,))
    d.ellipse([u(14.5), u(10.5), u(17.5), u(21.5)], fill=(4, 10, 6, 255))                        # the slit pupil
    d.ellipse([u(12), u(12.5), u(14), u(14.5)], fill=(235, 255, 235, 255))


def vanity(d, rgb):
    d.line([(u(12), u(19)), (u(7), u(27))], fill=(220, 190, 80, 255), width=u(2))                # the handle
    d.ellipse([u(9), u(4), u(25), u(20)], fill=(230, 160, 220, 255), outline=(255, 236, 150, 255), width=u(1))
    d.ellipse([u(11), u(6), u(23), u(18)], fill=rgb + (255,))
    d.line([(u(13), u(15)), (u(19), u(8))], fill=(255, 255, 255, 230), width=u(1))               # the glint: it admires itself
    d.line([(u(14), u(17)), (u(21), u(10))], fill=(255, 230, 250, 160), width=u(1))


def chakrams(d, rgb):
    for cx in (12.5, 19.5):
        d.ellipse([u(cx - 7), u(16 - 7), u(cx + 7), u(16 + 7)], outline=rgb + (255,), width=u(2))
        d.ellipse([u(cx - 4.5), u(16 - 4.5), u(cx + 4.5), u(16 + 4.5)], outline=(250, 230, 220, 255), width=u(1))
    for a in range(0, 360, 60):                                                                   # the blades on the rings
        r = math.radians(a)
        d.line([(u(12.5 + 7 * math.cos(r)), u(16 + 7 * math.sin(r))), (u(12.5 + 9.5 * math.cos(r)), u(16 + 9.5 * math.sin(r)))], fill=(250, 230, 220, 255), width=u(1))


def holy(d, rgb):
    for a in range(0, 360, 30):                                                                   # rays
        r = math.radians(a)
        d.line([(u(16 + 6 * math.cos(r)), u(15 + 6 * math.sin(r))), (u(16 + 12 * math.cos(r)), u(15 + 12 * math.sin(r)))], fill=(rgb[0], rgb[1], rgb[2], 150), width=u(1))
    d.polygon([(u(14.5), u(5)), (u(17.5), u(5)), (u(17.5), u(25)), (u(16), u(28)), (u(14.5), u(25))], fill=(250, 244, 230, 255))   # the blade
    d.rectangle([u(9), u(11), u(23), u(13.5)], fill=rgb + (255,))                                # the cross-guard
    d.rectangle([u(15.4), u(5), u(16.6), u(24)], fill=(255, 255, 255, 255))


DRAW = {
    "lord_of_slothful_weight": ((0x7A, 0x3F, 0xD1), sloth),
    "lord_of_green_eyed_envy": ((0x2F, 0xBF, 0x5A), envy),
    "lord_of_empty_vanity": ((0xD6, 0x4F, 0xB0), vanity),
    "lord_of_the_twin_rings": ((0xE2, 0x3B, 0x4B), chakrams),
    "seifort_holy_judgement": ((0xE8, 0xB9, 0x8A), holy),
}


def icon(rgb, draw):
    big = bg(rgb)
    d = ImageDraw.Draw(big, "RGBA")
    draw(d, rgb)
    im = big.resize((S, S), Image.LANCZOS)
    d2 = ImageDraw.Draw(im)
    d2.rectangle([0, 0, S - 1, S - 1], outline=GOLD)                                              # the Ultimate frame: gold ring
    d2.rectangle([1, 1, S - 2, S - 2], outline=GOLD_DARK)
    px = im.load()
    for x, y in ((0, 0), (S - 1, 0), (0, S - 1), (S - 1, S - 1), (1, 0), (0, 1), (S - 2, 0), (S - 1, 1), (0, S - 2), (1, S - 1), (S - 1, S - 2), (S - 2, S - 1)):
        px[x, y] = (0, 0, 0, 0)                                                                    # rounded corners, like Tensura's frames
    return im


if __name__ == "__main__":
    for name, (rgb, draw) in DRAW.items():
        path = os.path.join(OUT, name + ".png")
        icon(rgb, draw).save(path)
        print("wrote", path)
