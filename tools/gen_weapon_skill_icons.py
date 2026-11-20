"""Draws the 12 weapon skill icons (docs/weapon_skills.md): 32x32, a dark radial background tinted with the item's own colour, the item's 16x16 sprite doubled with nearest-neighbour (so the icon is the
item the skill speaks for, as the weapon rule asks), and the gold ring frame with rounded corners of the other Ultimates. The ten Vassal weapons use their item sprite, the Djinn Equip skill a Djinn Seal, the Beast Will
skill the Beast Will core.
Run: python tools/gen_weapon_skill_icons.py -> src/main/resources/assets/anime_multiverse/textures/skill/ws_<id>.png (ids = skill.rules.WeaponSkillRules)."""
import math
import os
from PIL import Image, ImageDraw

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
TEX = os.path.join(ROOT, "src", "main", "resources", "assets", "anime_multiverse", "textures")
S = 32
GOLD = (255, 236, 150, 255)
GOLD_DARK = (160, 112, 40, 255)

# the ten Vassal Weapons (skill.rules.WeaponSkillRules.heroKinds: HeroClass.VASSAL) + the Djinn and the Beast Will
SPRITES = {
    "ws_vassal_carriage": "vassal_carriage", "ws_vassal_fan": "vassal_fan", "ws_vassal_scythe": "vassal_scythe", "ws_vassal_katana": "vassal_katana",
    "ws_vassal_book": "vassal_book", "ws_vassal_mirror": "vassal_mirror", "ws_vassal_instrument": "vassal_instrument", "ws_vassal_harpoon": "vassal_harpoon",
    "ws_vassal_sewing_set": "vassal_sewing_set", "ws_vassal_chain": "vassal_chain",
    "ws_djinn": "djinn_seal_baal", "ws_beast_will": "beast_will_core",
}


def tint(sprite):
    """The average colour of the sprite's opaque pixels, pushed to a saturated mid tone."""
    px = [p for p in sprite.getdata() if p[3] > 200]
    if not px:
        return (120, 120, 160)
    r = sum(p[0] for p in px) / len(px)
    g = sum(p[1] for p in px) / len(px)
    b = sum(p[2] for p in px) / len(px)
    m = max(r, g, b, 1)
    return (int(r / m * 255), int(g / m * 255), int(b / m * 255))


def background(rgb):
    im = Image.new("RGBA", (S, S))
    px = im.load()
    c = (S - 1) / 2.0
    for y in range(S):
        for x in range(S):
            d = min(1.0, math.hypot(x - c, y - c) / (S * 0.62))
            f = (1.0 - d) ** 1.4
            px[x, y] = (int(10 + rgb[0] * 0.38 * f), int(8 + rgb[1] * 0.38 * f), int(14 + rgb[2] * 0.38 * f), 255)
    return im


def draw(skill_id, sprite_name):
    sprite = Image.open(os.path.join(TEX, "item", sprite_name + ".png")).convert("RGBA")
    im = background(tint(sprite))
    big = sprite.resize((S, S), Image.NEAREST)
    # a one pixel dark outline under the sprite so it reads on the glow
    shadow = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    a = big.split()[3]
    for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        sh = Image.new("RGBA", (S, S), (6, 4, 10, 255))
        shadow.paste(sh, (dx, dy), a)
    im.alpha_composite(shadow)
    im.alpha_composite(big)
    # the gold ring frame with rounded corners
    ring = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(ring)
    d.rounded_rectangle([0, 0, S - 1, S - 1], radius=5, outline=GOLD, width=1)
    d.rounded_rectangle([1, 1, S - 2, S - 2], radius=4, outline=GOLD_DARK, width=1)
    im.alpha_composite(ring)
    mask = Image.new("L", (S, S), 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, S - 1, S - 1], radius=5, fill=255)
    out = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    out.paste(im, (0, 0), mask)
    return out


if __name__ == "__main__":
    for sid, sprite in SPRITES.items():
        draw(sid, sprite).save(os.path.join(TEX, "skill", sid + ".png"))
        print("wrote", sid)
