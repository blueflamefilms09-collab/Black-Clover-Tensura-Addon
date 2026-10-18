#!/usr/bin/env python3
"""Draws the Hub's textures (docs/elite/hub.md): the two NPC skins (villager model layout, 64x64) and the Dungeon Map's inventory icon (16x16).

    python tools/elite/gen_hub_textures.py            # writes src/main/resources/assets/nusmp/textures/{entity/hub,item}/*.png

Why a generator and not hand-painted files: the villager model's UV layout is a fixed 64x64 sheet of box nets, and painting two skins pixel by pixel in an editor is how a sleeve ends up on the
wrong face. This paints each box net region by name (head, body, robe, arms, legs, nose) from a small palette, so the skins can be recoloured by changing a dict. The hat and hat rim regions
stay transparent: the villager model hides them.
"""
import os
import sys

from PIL import Image

ROOT = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
OUT = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp", "textures")


def box(img, u, v, w, h, d, faces):
    """Fills the six faces of a Minecraft box net at texture offset (u, v) of a w x h x d box. faces: dict of face name -> colour or callable(x, y, w, h) -> colour."""
    regions = {
        "top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d),
        "right": (u, v + d, d, h), "front": (u + d, v + d, w, h), "left": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h),
    }
    for name, (x0, y0, rw, rh) in regions.items():
        f = faces.get(name, faces.get("all"))
        if f is None:
            continue
        for y in range(rh):
            for x in range(rw):
                c = f(x, y, rw, rh) if callable(f) else f
                if c is not None:
                    img.putpixel((x0 + x, y0 + y), c)


def rgba(h, a=255):
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


def skin(palette):
    """The villager sheet: head, nose, body, robe, arms, legs."""
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    S, S2, HAIR, EYE, ROBE, ROBE2, TRIM, SLEEVE, LEG, BELT, BEARD = (rgba(palette[k]) for k in
        ("skin", "skin_shade", "hair", "eye", "robe", "robe_shade", "trim", "sleeve", "leg", "belt", "beard"))

    def face(x, y, w, h):                                   # front of the head, 8 x 10
        if y <= 1:
            return HAIR
        if y == 3 and x in (1, 2, 5, 6):                    # brows
            return HAIR
        if y == 4 and x in (1, 2, 5, 6):                    # eyes (white, pupil)
            return rgba("#F4F4F4") if x in (1, 6) else EYE
        if palette.get("moustache") and y == 7 and 1 <= x <= 6 and x not in (3, 4):
            return BEARD
        if palette.get("long_beard") and y >= 7:
            return BEARD
        if y == 9:
            return S2
        return S

    def side(x, y, w, h):
        return HAIR if y <= 2 else S

    def back(x, y, w, h):
        return HAIR if y <= 6 else S2

    box(img, 0, 0, 8, 10, 8, {"front": face, "right": side, "left": side, "back": back, "top": HAIR, "bottom": S2})
    box(img, 24, 0, 2, 4, 2, {"all": S2})                                       # nose

    def tunic(x, y, w, h):                                                      # the body under the robe: trim at the neck, belt at the waist
        if y == 0:
            return TRIM
        if y == 8 or y == 9:
            return BELT
        return ROBE if (x + y) % 5 else ROBE2

    box(img, 16, 20, 8, 12, 6, {"front": tunic, "back": ROBE, "right": ROBE, "left": ROBE, "top": TRIM, "bottom": ROBE2})

    def robe(x, y, w, h):                                                       # the long robe over it, 8 x 18
        if y >= h - 2:
            return TRIM
        if x in (0, w - 1):
            return ROBE2
        return ROBE if (x * 3 + y) % 7 else ROBE2

    box(img, 0, 38, 8, 18, 6, {"front": robe, "back": robe, "right": robe, "left": robe, "top": None, "bottom": ROBE2})

    def arm(x, y, w, h):
        return SLEEVE if y < h - 2 else S

    box(img, 44, 22, 4, 8, 4, {"all": arm, "top": SLEEVE, "bottom": S})         # the two upright arms share one net
    box(img, 40, 38, 8, 4, 4, {"all": SLEEVE, "front": lambda x, y, w, h: SLEEVE if y < 3 else S2})   # the crossed arms
    box(img, 0, 22, 4, 12, 4, {"all": LEG, "bottom": rgba("#2A2118")})          # legs
    return img


QUEST_GIVER = {"skin": "#C8946A", "skin_shade": "#A8754F", "hair": "#E8E4DC", "eye": "#2B3F7A", "robe": "#2F4B8C", "robe_shade": "#243A6E", "trim": "#E0B040",
               "sleeve": "#2F4B8C", "leg": "#3A2F27", "belt": "#E0B040", "beard": "#E8E4DC", "long_beard": True}
MERCHANT = {"skin": "#D6A07A", "skin_shade": "#B8825C", "hair": "#4A2E1A", "eye": "#2E5A2E", "robe": "#4F7A3A", "robe_shade": "#3E6330", "trim": "#C08A4A",
            "sleeve": "#E8DCC0", "leg": "#4A3828", "belt": "#7A4E2A", "beard": "#4A2E1A", "moustache": True}


def map_icon():
    """A parchment map: cream sheet, curled corners, a little maze of ink walls, a red X."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    paper, edge, ink, red = rgba("#E9D9A6"), rgba("#B89B5C"), rgba("#4A3A22"), rgba("#B02A2A")
    for y in range(2, 14):
        for x in range(2, 14):
            img.putpixel((x, y), paper)
    for i in range(2, 14):
        for (x, y) in ((i, 2), (i, 13), (2, i), (13, i)):
            img.putpixel((x, y), edge)
    for (x, y) in ((2, 2), (13, 2), (2, 13), (13, 13)):
        img.putpixel((x, y), (0, 0, 0, 0))
    walls = [(4, 4), (5, 4), (6, 4), (8, 4), (9, 4), (10, 4), (4, 5), (4, 6), (6, 6), (7, 6), (8, 6), (10, 5), (10, 6), (10, 7), (4, 8), (5, 8), (6, 8), (8, 8), (9, 8), (8, 9), (8, 10),
             (6, 10), (6, 11), (10, 10), (11, 10), (4, 11), (11, 6)]
    for (x, y) in walls:
        img.putpixel((x, y), ink)
    for (x, y) in ((11, 11), (12, 12), (12, 11), (11, 12)):
        img.putpixel((x, y), red)
    return img


def main():
    os.makedirs(os.path.join(OUT, "entity", "hub"), exist_ok=True)
    os.makedirs(os.path.join(OUT, "item"), exist_ok=True)
    skin(QUEST_GIVER).save(os.path.join(OUT, "entity", "hub", "quest_giver_npc.png"))
    skin(MERCHANT).save(os.path.join(OUT, "entity", "hub", "merchant_npc.png"))
    map_icon().save(os.path.join(OUT, "item", "dungeon_map.png"))
    print("wrote", os.path.join(OUT, "entity", "hub"), "and item/dungeon_map.png")
    return 0


if __name__ == "__main__":
    sys.exit(main())
