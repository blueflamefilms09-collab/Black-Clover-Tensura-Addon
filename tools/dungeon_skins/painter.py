"""Why: one function paints a dungeon person: the foundry-style row is turned into the art kit's sheet (`foundry_kit.build_spec`, normalised by the kit), the player-model kit's painter
(`tools/playermodel/gen_skins.paint_character`: skin tone, face, garments on both layers, armour plates, accessories, glow) paints the 64x64 skin, and the wear recipe of the family
(`recipes.py`, `wear.py`) weathers it. It also holds what every generator of the pipeline needs to turn that into a `looks.json` entry: the head appendages (ears, tusks, beards ...) as extras
with their tints, and the numbers of the entry.
"""
import common as CM
import foundry_kit as FK
import gen_hair
import gen_skins as GS
import parts as PT
import wear as W
from arc_kit import color as C
from arc_kit import spec as S

_masks = None


def hair_masks():
    global _masks
    if _masks is None:
        gen_hair.build()
        _masks = {sid: getattr(st, "mask", None) for sid, st in gen_hair.STYLES.items()}
    return _masks


def normalized(row):
    """The art kit's normalised sheet of a roster-format row."""
    return S.normalize(FK.build_spec(row), row["id"])


def make_post(recipe):
    def post(cv, spec, seed, glow):
        cv.tones = [tuple(int(v) for v in t[:3]) for t in C.skin_palette(spec["skin"])]
        cv.spec = spec
        steps = list(recipe)
        if (spec.get("outfit") or {}).get("bandages") and not any(n == "bandages" for n, _ in steps):
            steps.insert(0, ("bandages", {}))                      # a sheet that wraps its mob in bandages gets them painted
        return W.apply(cv, glow, steps, seed)
    return post


def paint(row, name, recipe):
    """(spec, skin image, glow image or None, hair style, hair rgb, slim) of a row, weathered by `recipe`."""
    spec = normalized(row)
    img, glow, hair_style, hair_rgb, slim = GS.paint_character(spec, name, hair_masks(), post=make_post(recipe) if recipe else None)
    return spec, img, glow, hair_style, hair_rgb, slim


def tint_of(source, spec, hair_rgb):
    """The ARGB string of the tint a head part wears: 'skin' (the skin's light tone), 'bone', 'hair', 'gold'."""
    if source == "skin":
        t = C.skin_palette(spec["skin"])[2]
        rgb = tuple(int(v) for v in t[:3])
    elif source == "hair":
        rgb = tuple(hair_rgb)
    elif source == "gold":
        rgb = (232, 190, 80)
    else:
        rgb = (236, 230, 208)
    # parts are painted in mid greys (about 150 of 255), so the tint is lifted to keep the colour true
    rgb = tuple(min(255, int(v * 1.5)) for v in rgb)
    return "#FF%02X%02X%02X" % rgb


def part_extras(row, spec, hair_rgb):
    """The extras entries of the head parts a row asks for (tokens `ears`, `bigears`, `tusks`, `beard`, `snout`, `crown3d`)."""
    out = []
    for part, src in PT.parts_for(row["tokens"], row.get("faction", "")):
        out.append({"model": PT.model_name(part), "texture": PT.model_name(part), "glow": False, "tint": tint_of(src, spec, hair_rgb)})
    return out
