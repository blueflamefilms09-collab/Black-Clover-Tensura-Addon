"""Requip: The Knight (Erza Scarlet): the temporary armour sets of the fourteen armours. One GeoLite model per piece, authored in WHOLE-BODY coordinates (feet at y = 0, shoulders at
y = 24, the right arm at x = -5, see tools/water_aura_lib.py), anchored on the model part it was built for by client/aura/ErzaAura.java (through WaterAuraRig) and forged in / shattered out by
client/aura/EquipAnimation exactly like the Djinn full equip. The pieces of each armour, their anchors, delays and fly-in sides are the table requip/ArmourSets.java; this script must define
exactly those pieces (the unit test ArmourAssetsTest compares the files with the table):

    python3 -B tools/gen_erza_requip_geo.py                    # all fourteen armours (about 160 models, textures and glow maps)
    python3 -B tools/gen_erza_requip_geo.py heart_kreuz        # one armour
    python3 -B tools/geo_check.py erza_heart_kreuz_arm_helm ...

Files: assets/nusmp/geo/entity/erza_<armour>_arm_<piece>.geo.json (+ animations/entity/*.animation.json for the animated pieces, textures/entity/*.png and *_glow.png).
"""
import math
import os
import sys

sys.dont_write_bytecode = True
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from water_aura_lib import Mat, Piece, Anim, mbox, stepped_spike, sine_rot, spin_y, feather_wing, tail_chain  # noqa: E402


def M(kind, base, dark, light, glow, amount, seed, **kw):
    return Mat(kind, base, dark=dark, light=light, glow=glow, glow_amount=amount, seed=seed, **kw)


# ====================================================================================================================== palettes: main plate, second plate, trim metal, hot (the glowing stuff), glass (wings and rings), cloth
PALETTES = {
    "heart_kreuz": dict(main=M("plate", "B8203E", "560A1E", "F06A84", "FF6A8C", 0.5, 101, inlay=1), alt=M("plate", "CCD0DC", "6A7088", "FFFFFF", "FF8AA0", 0.3, 102),
                        trim=M("metal", "E0B040", "6A4A0A", "FFEEA0", "FFD070", 0.5, 103, seam=1), hot=M("plate", "FF6A8C", "B02050", "FFC0D0", "FF8AA8", 1.0, 104),
                        glass=M("crystal", "E85A78", "7A1030", "FFC0D0", "FF7A9A", 0.9, 105, alpha=200, cells=6), cloth=M("cloth", "7A1228", "30060E", "C04060", "FF6A8C", 0.4, 106, trim=1)),
    "heavens_wheel": dict(main=M("plate", "D8DEEC", "7A86A8", "FFFFFF", "7AB8FF", 0.6, 111, inlay=1), alt=M("plate", "2E3E78", "10183C", "6A80D0", "7AB8FF", 0.5, 112),
                          trim=M("metal", "E8C860", "7A5A12", "FFF4B8", "FFE080", 0.5, 113, seam=1), hot=M("metal", "E8F0FF", "8A9ABE", "FFFFFF", "A0D0FF", 1.0, 114, seam=1),
                          glass=M("crystal", "BCD8FF", "5A78C0", "FFFFFF", "A0D0FF", 0.9, 115, alpha=190, cells=7), cloth=M("cloth", "DCE6FA", "7A8CB8", "FFFFFF", "A0D0FF", 0.5, 116, trim=1)),
    "flame_empress": dict(main=M("plate", "A31E27", "4C0B12", "E65440", "FF7A22", 0.8, 121, inlay=1), alt=M("plate", "8E1620", "3E080E", "D8443A", "FF6A1E", 0.6, 122),
                          trim=M("metal", "D9A730", "6B4609", "FFEBA3", "FFC866", 0.55, 123, seam=1), hot=M("flame", "FF7C1E", "B3200C", "FFE276", "FF9830", 0.95, 124, grain=0.04),
                          glass=M("flame", "FF9A30", "D0300C", "FFE680", "FFB040", 1.0, 125, grain=0.04), cloth=M("cloth", "7A1018", "300408", "D04030", "FF7A22", 0.5, 126, trim=1)),
    "lightning_empress": dict(main=M("bolt", "EFE6B8", "9A8A4A", "FFFFFF", "FFF080", 1.0, 131), alt=M("plate", "E8D890", "8A7424", "FFFBD0", "FFF080", 0.6, 132, inlay=1),
                              trim=M("metal", "E8C050", "7A5A10", "FFF4B0", "FFE070", 0.55, 133, seam=1), hot=M("flame", "FFE870", "D09A10", "FFFFF0", "FFF490", 1.0, 134, grain=0.03),
                              glass=M("crystal", "FFF2A0", "C8A020", "FFFFFF", "FFF490", 0.95, 135, alpha=190, cells=6), cloth=M("cloth", "F4EBC0", "9A8844", "FFFFFF", "FFF080", 0.6, 136, trim=1)),
    "sea_empress": dict(main=M("scales", "1E8CA8", "08384A", "6AD8EC", "80F0FF", 0.5, 141, cells=4), alt=M("plate", "E8F2F2", "7A9AA4", "FFFFFF", "80F0FF", 0.4, 142),
                        trim=M("metal", "F0E8D8", "8A8470", "FFFFFF", "FFB0C0", 0.4, 143, seam=1), hot=M("water", "6AD8F0", "1A6C90", "E8FCFF", "90F4FF", 1.0, 144),
                        glass=M("water", "4ABCE0", "1470A0", "D8FAFF", "80F0FF", 0.9, 145, alpha=190, ragged=1), cloth=M("cloth", "1A6C88", "083040", "5ABCD8", "80F0FF", 0.5, 146, trim=1)),
    "black_wing": dict(main=M("plate", "1E1830", "08060E", "52468A", "A060FF", 0.7, 151, inlay=1), alt=M("plate", "2E2650", "100C20", "6A5CB0", "A060FF", 0.5, 152),
                       trim=M("metal", "9A90C8", "403868", "E8E0FF", "B488FF", 0.5, 153, seam=1), hot=M("wisp", "3A2A5C", "0A0614", "9A70E8", "B070FF", 1.0, 154),
                       glass=M("feather", "241C3C", "0A0614", "7A5CC8", "A870FF", 0.8, 155), cloth=M("cloth", "1A1430", "06040C", "48388A", "A060FF", 0.5, 156, trim=1)),
    "clear_heart": dict(main=M("cloth", "ECF0FA", "9AA4C0", "FFFFFF", "C8E0FF", 0.5, 161, trim=1), alt=M("cloth", "C8D4EE", "7884A8", "F4F8FF", "C8E0FF", 0.4, 162, trim=1),
                        trim=M("metal", "E8D8A0", "8A7A44", "FFFBE0", "FFF0B0", 0.5, 163, seam=1), hot=M("wisp", "E0ECFF", "8A9CC8", "FFFFFF", "D8ECFF", 1.0, 164),
                        glass=M("cloth", "F4F8FF", "A8B4D4", "FFFFFF", "D8ECFF", 0.6, 165, ragged=1), cloth=M("cloth", "F0F4FC", "9AA6C6", "FFFFFF", "D8ECFF", 0.5, 166, trim=1)),
    "nakagami": dict(main=M("plate", "F0F2FA", "8A94B4", "FFFFFF", "60C0FF", 0.7, 171, inlay=1), alt=M("plate", "DCE2F2", "7A86A8", "FFFFFF", "60C0FF", 0.5, 172),
                     trim=M("metal", "E4BE5C", "7A5A14", "FFF2B0", "FFE080", 0.5, 173, seam=1), hot=M("bolt", "BCE4FF", "3E88D0", "FFFFFF", "70C8FF", 1.0, 174),
                     glass=M("crystal", "C8E8FF", "5A9AD8", "FFFFFF", "70C8FF", 0.95, 175, alpha=190, cells=6), cloth=M("cloth", "F4F6FC", "9AA4C4", "FFFFFF", "70C8FF", 0.6, 176, trim=1)),
    "grand_chariot": dict(main=M("plate", "F4E4A8", "9A8040", "FFFBE0", "FFD060", 0.7, 181, inlay=1), alt=M("plate", "E0C870", "7A6420", "FFF4C0", "FFD060", 0.5, 182),
                          trim=M("metal", "F0C850", "806010", "FFF6B8", "FFE070", 0.6, 183, seam=1), hot=M("metal", "FFF0B8", "A08A40", "FFFFFF", "FFE890", 1.0, 184, seam=1),
                          glass=M("crystal", "FFEAA0", "C09A30", "FFFFFF", "FFE070", 0.95, 185, alpha=190, cells=7), cloth=M("cloth", "F8EEC8", "A08C50", "FFFFFF", "FFD060", 0.6, 186, trim=1)),
    "adamantine": dict(main=M("metal", "586A64", "1E2A26", "9AB4AA", "7AFFC0", 0.4, 191, seam=1), alt=M("plate", "46564F", "182420", "86A096", "7AFFC0", 0.35, 192),
                       trim=M("metal", "8E9E98", "3A4642", "DCEAE4", "A0FFD0", 0.4, 193, seam=1), hot=M("plate", "3C4A44", "10181A", "7A968C", "7AFFC0", 0.9, 194, inlay=1),
                       glass=M("crystal", "9AD8C0", "3A7A62", "F0FFF8", "9AFFD0", 0.8, 195, alpha=200, cells=5), cloth=M("cloth", "3A4A44", "10181A", "6A8A7E", "7AFFC0", 0.3, 196, trim=1)),
    "purgatory": dict(main=M("plate", "241A1E", "0A0608", "5A4048", "FF3A1A", 0.9, 201, inlay=1), alt=M("plate", "30222A", "0E080A", "6A4A54", "FF3A1A", 0.7, 202),
                      trim=M("metal", "7A6A72", "2E262A", "C8B8C0", "FF6A3A", 0.5, 203, seam=1), hot=M("flame", "FF4A1A", "A01808", "FFB050", "FF6A2A", 1.0, 204, grain=0.04),
                      glass=M("flame", "FF5A1E", "B01C08", "FFC070", "FF7A30", 1.0, 205, grain=0.04), cloth=M("cloth", "30121A", "0E0408", "7A2A34", "FF3A1A", 0.7, 206, trim=1)),
    "flight_armor": dict(main=M("plate", "8CB8E8", "36588E", "D8ECFF", "C0F0FF", 0.6, 211, inlay=1), alt=M("plate", "E8F0FA", "8A9EBE", "FFFFFF", "C0F0FF", 0.4, 212),
                         trim=M("metal", "C8D8EC", "586A88", "FFFFFF", "E0F8FF", 0.5, 213, seam=1), hot=M("bolt", "C8F0FF", "4A9ADC", "FFFFFF", "C0F0FF", 1.0, 214),
                         glass=M("crystal", "A8D8FF", "4A8AD0", "FFFFFF", "C0F0FF", 0.95, 215, alpha=185, cells=6), cloth=M("cloth", "BCD4F0", "5A7AA8", "F0F8FF", "C0F0FF", 0.5, 216, trim=1)),
    "wind_god": dict(main=M("plate", "E8F4EC", "7AA48C", "FFFFFF", "70F0B0", 0.6, 221, inlay=1), alt=M("plate", "58C898", "1A6848", "A8F4D0", "70F0B0", 0.6, 222),
                     trim=M("metal", "D8F0E4", "6A9A84", "FFFFFF", "A0FFD0", 0.5, 223, seam=1), hot=M("wisp", "A8F0D0", "2A8860", "F0FFF8", "90FFC8", 1.0, 224),
                     glass=M("feather", "A0F0C8", "2A9068", "F4FFF8", "90FFC8", 0.9, 225), cloth=M("cloth", "C8F0DC", "5AA888", "F4FFF8", "90FFC8", 0.6, 226, trim=1)),
    "fairy_armor": dict(main=M("plate", "F2B8E4", "9A4C88", "FFE8F8", "FFE0A0", 0.6, 231, inlay=1), alt=M("plate", "FFF0D8", "A8885C", "FFFFFF", "FFE0A0", 0.4, 232),
                        trim=M("metal", "F0C860", "8A6414", "FFF6B8", "FFE48A", 0.6, 233, seam=1), hot=M("metal", "FFE8A0", "B8903C", "FFFFFF", "FFF0B0", 1.0, 234, seam=1),
                        glass=M("crystal", "FFC8F0", "C070B0", "FFFFFF", "FFE8B0", 0.95, 235, alpha=180, cells=6), cloth=M("cloth", "FADCF2", "B07AA4", "FFFFFF", "FFE0A0", 0.6, 236, trim=1)),
}

# ====================================================================================================================== the look of each armour: style names per piece kind
SPECS = {
    "heart_kreuz": dict(helm="crown", chest="breast", emblem="heart", pauldron="round", vambrace="plain", gauntlet="plain", greave="plain", skirt="tassets", extras=("skirt", "gauntlet")),
    "heavens_wheel": dict(helm="winged", chest="breast", emblem="cross", pauldron="wing", vambrace="finned", gauntlet="plain", greave="winged", skirt="tassets",
                          extras=("skirt", "wing:blade", "halo:blades", "backdrop:wheel")),
    "flame_empress": dict(helm="crest", chest="breast", emblem="sun", pauldron="spiked", vambrace="finned", gauntlet="claw", greave="spiked", skirt="tassets",
                          extras=("skirt", "gauntlet", "halo:flame", "tail:flame")),
    "lightning_empress": dict(helm="winged", chest="breast", emblem="bolt", pauldron="spiked", vambrace="finned", gauntlet="plain", greave="winged", skirt="short",
                              extras=("skirt", "gauntlet", "halo:runes", "wing:bolt")),
    "sea_empress": dict(helm="crown", chest="breast", emblem="wave", pauldron="round", vambrace="finned", gauntlet="plain", greave="winged", skirt="tassets",
                        extras=("skirt", "gauntlet", "halo:coral", "wing:fin")),
    "black_wing": dict(helm="horned", chest="breast", emblem="feather", pauldron="wing", vambrace="finned", gauntlet="claw", greave="spiked", skirt="short",
                       extras=("skirt", "gauntlet", "wing:feather")),
    "clear_heart": dict(helm="ribbon", chest="cloth", emblem="none", pauldron="none", vambrace="cloth", gauntlet="none", greave="boot", skirt="cloth",
                        extras=("skirt",)),
    "nakagami": dict(helm="visor", chest="breast", emblem="rune", pauldron="tiered", vambrace="ring", gauntlet="plain", greave="plain", skirt="tassets",
                     extras=("skirt", "gauntlet", "halo:runes", "cape:cloth")),
    "grand_chariot": dict(helm="winged", chest="breast", emblem="cross", pauldron="wing", vambrace="finned", gauntlet="plain", greave="winged", skirt="tassets",
                          extras=("skirt", "wing:blade", "halo:blades", "backdrop:chariot")),
    "adamantine": dict(helm="visor", chest="heavy", emblem="cross", pauldron="tiered", vambrace="ring", gauntlet="round", greave="plain", skirt="tassets",
                       extras=("skirt", "gauntlet")),
    "purgatory": dict(helm="horned", chest="heavy", emblem="sun", pauldron="spiked", vambrace="finned", gauntlet="claw", greave="spiked", skirt="short",
                      extras=("skirt", "gauntlet", "backdrop:spikes")),
    "flight_armor": dict(helm="visor", chest="breast", emblem="none", pauldron="round", vambrace="plain", gauntlet="plain", greave="winged", skirt="short",
                         extras=("skirt", "gauntlet", "wing:jet")),
    "wind_god": dict(helm="winged", chest="breast", emblem="feather", pauldron="wing", vambrace="finned", gauntlet="plain", greave="winged", skirt="short",
                     extras=("skirt", "gauntlet", "wing:feather", "halo:ring", "tail:ribbon")),
    "fairy_armor": dict(helm="crown", chest="breast", emblem="heart", pauldron="round", vambrace="plain", gauntlet="plain", greave="plain", skirt="tassets",
                        extras=("skirt", "gauntlet", "wing:fairy", "halo:petals")),
}


class Ctx:
    """The armour being built: its key, palette and style."""

    def __init__(self, aid):
        self.id, self.key, self.pal, self.spec = aid, "erza_" + aid, PALETTES[aid], SPECS[aid]

    def piece(self, name, density=3):
        return Piece(self.key, "arm_" + name, density=density)


# ====================================================================================================================== standard pieces
def helm(c):
    P, s = c.pal, c.spec["helm"]
    p = c.piece("helm")
    if s == "ribbon":                                                   # Clear Heart: a ribbon round the brow with two trailing ends
        p.box("root", (-4.7, 29.4, -4.7), (9.4, 1.4, 9.4), P["main"])
        p.box("root", (-0.8, 29.2, -5.0), (1.6, 1.6, 0.6), P["trim"], rot=(0, 0, 45), pivot=(0, 30.0, -4.8))
        p.box("root", (-1.6, 24.0, 4.1), (1.4, 5.8, 0.7), P["main"], rot=(8, 0, 6), pivot=(-1.0, 29.8, 4.4))
        p.box("root", (0.4, 23.0, 4.3), (1.4, 6.8, 0.7), P["alt"], rot=(10, 0, -6), pivot=(1.0, 29.8, 4.4))
        p.save()
        return p
    # the brow band every armour wears (above the eyes: the face stays free)
    p.box("root", (-4.75, 29.2, -4.75), (9.5, 1.8, 0.8), P["trim"])
    p.box("root", (-4.75, 29.2, 3.95), (9.5, 1.8, 0.8), P["trim"])
    p.box("root", (-4.75, 29.2, -3.95), (0.8, 1.8, 7.9), P["trim"])
    p.box("root", (3.95, 29.2, -3.95), (0.8, 1.8, 7.9), P["trim"])
    p.box("root", (-4.5, 31.0, -4.5), (9.0, 1.3, 9.0), P["main"])
    for sx in (0, 1):
        mbox(p, "root", sx, (-5.05, 24.4, -2.8), (1.0, 4.6, 6.0), P["main"])      # cheek guards (the face is open)
    if s == "crest":
        p.box("root", (-0.7, 32.2, -4.9), (1.4, 1.4, 9.8), P["trim"])
        for sx in (-1, 1):
            p.box("root", (sx * 3.3 - 0.9, 32.2, 0.2), (1.8, 1.9, 1.2), P["main"])
    elif s == "horned":
        for sx in (-1, 1):
            stepped_spike(p, "root", (sx * 4.2, 31.2, 0.0), "+y", 5.0, 1.8, P["trim"], steps=3, thick=1.6)
            stepped_spike(p, "root", (sx * 4.9, 30.4, 0.0), "+x" if sx > 0 else "-x", 2.4, 1.4, P["trim"], steps=2, thick=1.2)
    elif s == "winged":
        for sx in (-1, 1):
            p.box("root", (sx * 5.0 - 0.4, 28.6, -2.0), (0.8, 5.2, 4.2), P["alt"], rot=(0, 0, -sx * 26), pivot=(sx * 5.0, 29.0, -1.0))
            p.box("root", (sx * 5.6 - 0.35, 31.2, -1.6), (0.7, 4.0, 3.2), P["main"], rot=(0, 0, -sx * 44), pivot=(sx * 5.4, 31.0, -1.0))
        p.box("root", (-0.8, 30.0, -5.0), (1.6, 1.6, 0.8), P["hot"], rot=(0, 0, 45), pivot=(0, 31.0, -4.8))
    elif s == "visor":
        p.box("root", (-4.8, 29.4, -5.3), (9.6, 1.9, 1.0), P["alt"])
        p.box("root", (-0.5, 32.2, -4.6), (1.0, 2.2, 9.2), P["trim"])
        p.box("root", (-4.9, 26.6, -4.2), (0.9, 2.8, 2.4), P["alt"])
        p.box("root", (4.0, 26.6, -4.2), (0.9, 2.8, 2.4), P["alt"])
    elif s == "crown":
        for i, dx in enumerate((-3.4, -1.7, 0.0, 1.7, 3.4)):
            stepped_spike(p, "root", (dx, 32.3, -3.6), "+y", 3.6 - 0.8 * abs(dx) / 3.4, 1.4, P["trim"], steps=2, thick=1.0)
        p.box("root", (-0.7, 30.4, -5.0), (1.4, 1.4, 0.7), P["hot"], rot=(0, 0, 45), pivot=(0, 31.0, -4.8))
    p.save()
    return p


def chest(c):
    P, st, em = c.pal, c.spec["chest"], c.spec["emblem"]
    p = c.piece("chest")
    if st == "cloth":                                                   # a wrapped top: a band, shoulder straps, a knot
        p.box("root", (-4.5, 17.6, -2.7), (9.0, 4.2, 5.4), P["main"])
        p.box("root", (-4.4, 21.4, -2.6), (2.0, 3.0, 5.2), P["alt"])
        p.box("root", (2.4, 21.4, -2.6), (2.0, 3.0, 5.2), P["alt"])
        p.box("root", (-4.6, 16.6, -2.8), (9.2, 1.0, 5.6), P["trim"])
        p.box("root", (-1.0, 18.6, -3.1), (2.0, 2.0, 0.6), P["trim"], rot=(0, 0, 45), pivot=(0, 19.6, -2.9))
        p.box("root", (-4.5, 12.6, -2.6), (9.0, 4.0, 5.2), P["alt"])
        p.save()
        return p
    heavy = st == "heavy"
    th = 1.5 if heavy else 1.1
    p.box("root", (-4.6, 16.4, -2.75 - (0.3 if heavy else 0)), (9.2, 7.6, th), P["main"])                 # breastplate
    p.box("root", (-4.45, 14.7, -2.65), (8.9, 1.7, 0.95), P["trim"])                                     # the lames of the belly
    p.box("root", (-4.3, 12.9, -2.55), (8.6, 1.7, 0.85), P["alt"])
    p.box("root", (-4.8, 23.4, -2.9), (9.6, 1.9, 5.8), P["trim"])                                         # the gorget
    p.box("root", (-4.7, 12.2, 2.05), (9.4, 11.2, 0.95 + (0.4 if heavy else 0)), P["main"])               # back plate
    p.box("root", (-0.5, 14.0, 3.0), (1.0, 8.0, 0.5), P["hot"])                                          # the vent / seam down the back
    for sx in (0, 1):
        mbox(p, "root", sx, (-4.95, 13.2, -1.9), (0.9, 8.6, 3.8), P["alt"])                              # side guards
    ey, ez = 20.4, -3.15                                                                                  # the emblem on the breastplate
    if em in ("sun", "rune"):
        p.box("root", (-1.5, ey - 1.5, ez - 0.05), (3.0, 3.0, 0.6), P["trim"], rot=(0, 0, 45), pivot=(0, ey, ez))
        p.box("root", (-0.7, ey - 0.7, ez - 0.4), (1.4, 1.4, 0.5), P["hot"], rot=(0, 0, 45), pivot=(0, ey, ez))
    elif em == "heart":
        p.box("root", (-1.3, ey - 1.6, ez - 0.05), (2.6, 2.6, 0.6), P["hot"], rot=(0, 0, 45), pivot=(0, ey - 0.3, ez))
        p.box("root", (-2.3, ey + 0.1, ez - 0.05), (1.9, 1.9, 0.6), P["hot"], rot=(0, 0, 45), pivot=(-1.3, ey + 0.8, ez))
        p.box("root", (0.4, ey + 0.1, ez - 0.05), (1.9, 1.9, 0.6), P["hot"], rot=(0, 0, 45), pivot=(1.3, ey + 0.8, ez))
    elif em == "cross":
        p.box("root", (-0.6, ey - 2.2, ez - 0.05), (1.2, 4.4, 0.6), P["trim"])
        p.box("root", (-2.2, ey - 0.6, ez - 0.05), (4.4, 1.2, 0.6), P["trim"])
        p.box("root", (-0.5, ey - 0.5, ez - 0.4), (1.0, 1.0, 0.5), P["hot"])
    elif em == "bolt":
        p.box("root", (-1.6, ey, ez - 0.05), (1.8, 2.4, 0.6), P["hot"], rot=(0, 0, 22), pivot=(-0.7, ey + 1.2, ez))
        p.box("root", (-0.2, ey - 2.2, ez - 0.05), (1.8, 2.4, 0.6), P["hot"], rot=(0, 0, 22), pivot=(0.7, ey - 1.0, ez))
    elif em == "wave":
        for k in range(3):
            p.box("root", (-2.6 + k * 0.3, ey + 1.2 - k * 1.3, ez - 0.05), (5.2 - k * 0.6, 0.8, 0.6), P["hot"])
    elif em == "feather":
        p.box("root", (-0.5, ey - 2.0, ez - 0.05), (1.0, 4.6, 0.6), P["trim"], rot=(0, 0, 18), pivot=(0, ey, ez))
        p.box("root", (-1.6, ey - 0.4, ez - 0.05), (1.4, 2.6, 0.6), P["hot"], rot=(0, 0, 38), pivot=(-0.9, ey + 0.4, ez))
        p.box("root", (0.2, ey - 0.4, ez - 0.05), (1.4, 2.6, 0.6), P["hot"], rot=(0, 0, -38), pivot=(0.9, ey + 0.4, ez))
    if heavy:                                                                                             # rivets along the plate edges
        for sx in (-1, 1):
            for yy in (17.2, 19.2, 21.2):
                p.box("root", (sx * 3.9 - 0.4, yy, -3.15), (0.8, 0.8, 0.5), P["trim"])
    p.save()
    return p


def skirt(c):
    P, st = c.pal, c.spec["skirt"]
    p = c.piece("skirt", density=2)
    a = Anim()
    a.clip("idle", length=2.0, loop=True)
    p.box("root", (-4.7, 11.8, -2.65), (9.4, 1.4, 5.3), P["trim"])                                       # the belt
    if st == "cloth":                                                                                     # a flowing cloth skirt
        for i, cx in enumerate((-3.0, 0.0, 3.0)):
            n = "f%d" % i
            p.bone(n, (cx, 11.8, -3.0))
            p.box(n, (cx - 1.6, 5.6, -3.5), (3.2, 6.2, 0.8), P["cloth"] if i % 2 == 0 else P["main"])
            sine_rot(a, "idle", n, 2.0, 0, 7.0, phase=i * 0.2)
        p.bone("b0", (0, 11.8, 2.9))
        p.box("b0", (-3.6, 5.8, 2.6), (7.2, 6.0, 0.8), P["main"])
        sine_rot(a, "idle", "b0", 2.0, 0, -7.0, phase=0.4)
        p.save(a)
        return p
    drop = 3.3 if st == "tassets" else 2.2
    for i, cx in enumerate((-2.9, 0.0, 2.9)):
        n = "f%d" % i
        p.bone(n, (cx, 11.8, -3.0))
        p.box(n, (cx - 1.4, 11.8 - drop - 0.2, -3.4), (2.8, drop, 0.9), P["main"] if i % 2 == 0 else P["alt"])
        sine_rot(a, "idle", n, 2.0, 0, 5.0, phase=i * 0.2)
    p.bone("b0", (0, 11.8, 2.9))
    p.box("b0", (-3.2, 11.8 - drop - 0.2, 2.55), (6.4, drop + 0.1, 0.9), P["alt"])
    sine_rot(a, "idle", "b0", 2.0, 0, -5.0, phase=0.4)
    for sx in (-1, 1):
        p.box("root", (sx * 4.2 - 0.5, 11.8 - drop - 0.1, -1.8), (1.0, drop, 3.6), P["main"])
    p.save(a)
    return p


def pauldron(c, side):
    P, st = c.pal, c.spec["pauldron"]
    n = "pauldron_" + ("l" if side else "r")
    p = c.piece(n)
    if st == "round":                                                   # a domed pauldron: three stacked plates
        mbox(p, "root", side, (-10.2, 21.0, -3.9), (6.4, 1.5, 7.8), P["main"])
        mbox(p, "root", side, (-9.6, 22.4, -3.3), (5.2, 1.4, 6.6), P["alt"])
        mbox(p, "root", side, (-9.0, 23.7, -2.7), (4.0, 1.3, 5.4), P["main"])
        mbox(p, "root", side, (-10.4, 20.6, -3.5), (1.0, 2.4, 7.0), P["trim"])
    elif st == "tiered":                                                # heavy tiered lames hanging off the shoulder
        for k in range(4):
            mbox(p, "root", side, (-10.8 + k * 0.35, 22.8 - k * 1.6, -3.9 + k * 0.1), (6.6 - k * 0.5, 1.7, 7.8 - k * 0.2), P["main"] if k % 2 == 0 else P["alt"])
        mbox(p, "root", side, (-10.0, 24.4, -3.3), (5.2, 1.2, 6.6), P["trim"])
        mbox(p, "root", side, (-11.0, 21.6, -0.6), (0.8, 3.4, 1.2), P["hot"])
    elif st == "wing":                                                  # a swept row of feather-plates
        for k in range(4):
            mbox(p, "root", side, (-10.0 + k * 0.4, 20.4 + k * 1.1, -3.2), (5.4 - k * 0.7, 1.3, 6.4), P["main"] if k % 2 == 0 else P["trim"], rot=(0, 0, 8 - k * 4), pivot=(-7.5, 22.5, 0.0))
        mbox(p, "root", side, (-9.4, 25.0, -2.5), (4.2, 1.0, 5.0), P["alt"])
    else:                                                               # "spiked": a plate with fangs and horns
        mbox(p, "root", side, (-9.8, 21.0, -3.6), (5.6, 3.6, 7.2), P["main"])
        mbox(p, "root", side, (-9.2, 24.0, -3.2), (4.6, 1.3, 6.4), P["trim"])
        mbox(p, "root", side, (-10.3, 18.8, -2.9), (1.0, 2.6, 5.8), P["alt"])
        for z in (-1.9, 1.9):
            bx = -7.0 if side == 0 else 7.0
            stepped_spike(p, "root", (bx, 25.3, z), "+y", 3.2, 1.8, P["trim"], steps=2, thick=1.4)
    p.save()
    return p


def vambrace(c, side):
    P, st = c.pal, c.spec["vambrace"]
    n = "vambrace_" + ("l" if side else "r")
    p = c.piece(n)
    if st == "cloth":                                                   # a cloth sleeve band
        mbox(p, "root", side, (-8.5, 14.6, -2.6), (4.9, 4.6, 5.2), P["main"])
        mbox(p, "root", side, (-8.7, 18.0, -2.8), (5.3, 0.9, 5.6), P["trim"])
        mbox(p, "root", side, (-8.7, 14.4, -2.8), (5.3, 0.8, 5.6), P["trim"])
        p.save()
        return p
    mbox(p, "root", side, (-8.55, 14.3, -2.6), (4.9, 4.9, 5.2), P["main"])
    mbox(p, "root", side, (-8.75, 18.2, -2.8), (5.3, 1.0, 5.6), P["trim"])
    bx = -8.7 if side == 0 else 8.7
    if st == "finned":
        stepped_spike(p, "root", (bx, 16.6, 0.0), "-x" if side == 0 else "+x", 3.4, 2.4, P["trim"], steps=2, thick=0.9)
    elif st == "ring":
        mbox(p, "root", side, (-8.75, 15.4, -2.8), (5.3, 0.9, 5.6), P["trim"])
        mbox(p, "root", side, (-8.75, 16.9, -2.8), (5.3, 0.9, 5.6), P["alt"])
    p.save()
    return p


def gauntlet(c, side):
    P, st = c.pal, c.spec["gauntlet"]
    n = "gauntlet_" + ("l" if side else "r")
    p = c.piece(n)
    mbox(p, "root", side, (-8.5, 11.5, -2.6), (4.9, 2.8, 5.2), P["alt"])
    mbox(p, "root", side, (-8.65, 11.4, -3.0), (5.2, 1.0, 1.0), P["trim"])
    if st == "claw":
        for k in range(4):
            cx = (-8.0 + k * 1.05) if side == 0 else (6.95 - k * 1.05)
            p.box("root", (cx, 8.2, -2.4 + 0.25 * (k % 2)), (0.95, 3.4, 0.8), P["trim"] if k % 2 else P["main"])
    elif st == "round":
        mbox(p, "root", side, (-8.8, 8.6, -2.9), (5.4, 3.4, 5.8), P["main"])
        mbox(p, "root", side, (-8.9, 11.0, -3.0), (5.6, 0.8, 6.0), P["trim"])
    else:
        mbox(p, "root", side, (-8.55, 8.8, -2.7), (4.9, 3.0, 5.4), P["main"])
        mbox(p, "root", side, (-8.4, 9.6, -3.1), (4.6, 1.2, 0.7), P["trim"])
    p.save()
    return p


def greave(c, side):
    P, st = c.pal, c.spec["greave"]
    n = "greave_" + ("l" if side else "r")
    p = c.piece(n)
    if st == "boot":                                                    # cloth boots with a trim cuff
        mbox(p, "root", side, (-4.5, 1.5, -2.55), (4.9, 4.0, 5.1), P["main"])
        mbox(p, "root", side, (-4.55, 0.0, -3.0), (5.1, 1.6, 6.0), P["alt"])
        mbox(p, "root", side, (-4.6, 5.3, -2.75), (5.2, 0.8, 5.5), P["trim"])
        p.save()
        return p
    mbox(p, "root", side, (-4.5, 6.4, -2.55), (4.9, 4.8, 5.1), P["main"])                 # thigh guard
    mbox(p, "root", side, (-4.4, 5.2, -3.1), (4.7, 2.2, 1.1), P["trim"])                  # knee
    sx = -2.0 if side == 0 else 2.0
    if st == "spiked":
        stepped_spike(p, "root", (sx, 6.3, -3.1), "-z", 3.0, 2.0, P["trim"], steps=2, thick=1.6)
    mbox(p, "root", side, (-4.45, 1.5, -2.55), (4.9, 3.6, 5.1), P["alt"])                 # shin
    mbox(p, "root", side, (-4.55, 0.0, -3.0), (5.1, 1.6, 6.0), P["main"])                 # boot
    mbox(p, "root", side, (-4.6, 1.3, -2.75), (5.2, 0.8, 5.5), P["trim"])                 # cuff
    if st == "winged":                                                  # small wings at the ankle
        wx = -4.9 if side == 0 else 4.9
        for k in range(3):
            p.box("root", (wx - (1.8 if side == 0 else 0.0) , 2.0 + k * 0.9, -1.0 + k * 0.5), (1.8, 0.6, 2.8 - k * 0.5), P["trim"], rot=(0, 0, (-1 if side == 0 else 1) * (10 + k * 14)), pivot=(wx, 2.4 + k * 0.9, -0.5))
    p.save()
    return p


# ====================================================================================================================== extras
def wing(c, side, style):
    P = c.pal
    n = "wing_" + ("l" if side else "r")
    p = c.piece(n)
    a = Anim()
    a.clip("idle", length=1.2, loop=True)
    pos = (2.4, 21.6, 2.6)
    if style == "jet":                                                  # two stubby jet fins with a glowing vent
        sg = 1.0 if side else -1.0
        p.bone("w", (sg * 3.0, 20.0, 3.0))
        p.box("w", (sg * 3.0 - (3.6 if side == 0 else 0.0), 16.0, 3.0), (3.6, 9.0, 1.2), P["main"], rot=(0, 0, sg * -14), pivot=(sg * 3.0, 20.0, 3.0))
        p.box("w", (sg * 6.2 - 1.2, 15.0, 2.6), (2.4, 4.0, 2.4), P["alt"], rot=(0, 0, sg * -14), pivot=(sg * 3.0, 20.0, 3.0))
        p.box("w", (sg * 6.2 - 0.8, 14.2, 3.0), (1.6, 1.2, 1.6), P["hot"], rot=(0, 0, sg * -14), pivot=(sg * 3.0, 20.0, 3.0))
        sine_rot(a, "idle", "w", 1.2, 2, 3.0 * sg, 0.0)
        p.save(a)
        return p
    fan = {"feather": [(-46, 14.5, 3.8), (-26, 17.0, 4.2), (-8, 18.0, 4.2), (12, 16.0, 4.0), (30, 12.5, 3.6)],
           "blade": [(-46, 15.0, 2.0), (-28, 19.0, 2.4), (-10, 21.0, 2.4), (8, 19.0, 2.4), (26, 15.0, 2.0), (42, 11.0, 1.8)],
           "bolt": [(-40, 13.0, 1.8), (-18, 16.0, 2.0), (4, 16.5, 2.0), (26, 13.0, 1.8)],
           "fin": [(-36, 9.0, 4.6), (-14, 11.0, 5.0), (8, 10.0, 4.8), (28, 8.0, 4.2)],
           "fairy": [(-34, 13.0, 5.4), (-6, 15.0, 5.8), (22, 11.0, 4.8)]}[style]
    mat = P["glass"] if style in ("fairy", "fin", "bolt", "feather") else P["hot"]
    arm = P["trim"] if style != "fairy" else None
    feather_wing(p, a, side, pos, fan, mat, arm_mat=arm, length=1.4 if style != "fin" else 1.0, amp=10.0 if style != "blade" else 5.0, yaw=20.0)
    p.save(a)
    return p


def halo(c, style):
    P = c.pal
    p = c.piece("halo")
    a = Anim()
    a.clip("idle", length=8.0, loop=True)
    p.bone("spin", (0, 34.5, 0))
    n = 8
    for i in range(n):
        p.box("spin", (-1.5, 34.0, -7.4), (3.0, 0.9, 2.2), P["trim"], rot=(0, i * 360.0 / n, 0), pivot=(0, 34.5, 0))
    if style == "blades":                                               # a ring of small swords pointing up
        for i in range(n):
            p.box("spin", (-0.5, 34.9, -7.2), (1.0, 4.6, 0.5), P["hot"], rot=(0, i * 360.0 / n, 0), pivot=(0, 34.5, 0))
    elif style == "runes":                                              # four floating rune plates standing on the ring
        for i in range(4):
            p.box("spin", (-1.3, 35.0, -7.5), (2.6, 3.0, 0.5), P["hot"], rot=(0, i * 90.0, 0), pivot=(0, 34.5, 0))
    elif style == "flame":
        for i in range(n):
            p.box("spin", (-0.9, 34.9, -7.3), (1.8, 2.8, 0.8), P["hot"], rot=(0, i * 360.0 / n + 22.5, 0), pivot=(0, 34.5, 0))
    elif style == "coral":
        for i in range(n):
            p.box("spin", (-0.7, 34.9, -7.3), (1.4, 3.2, 0.8), P["main"], rot=(0, i * 360.0 / n + 22.5, 0), pivot=(0, 34.5, 0))
            p.box("spin", (-0.5, 37.6, -7.2), (1.0, 1.4, 0.6), P["hot"], rot=(0, i * 360.0 / n + 22.5, 0), pivot=(0, 34.5, 0))
    elif style == "petals":
        for i in range(n):
            p.box("spin", (-1.0, 34.9, -7.5), (2.0, 2.2, 0.5), P["glass"], rot=(0, i * 360.0 / n + 22.5, 0), pivot=(0, 34.5, 0))
    spin_y(a, "idle", "spin", 8.0, turns=1, steps=8)
    p.save(a)
    return p


def backdrop(c, style):
    P = c.pal
    p = c.piece("backdrop")
    a = Anim()
    cy, cz = 19.0, 9.0
    if style in ("wheel", "chariot"):                                   # the wheel of swords turning behind the back
        a.clip("idle", length=24.0, loop=True)
        n = 12 if style == "wheel" else 16
        p.bone("disc", (0, cy, cz))
        for i in range(n):
            p.box("disc", (-0.9, cy + 4.5, cz), (1.8, 11.0 if style == "wheel" else 13.0, 0.7), P["hot"], rot=(0, 0, i * 360.0 / n), pivot=(0, cy, cz))        # the blades
            p.box("disc", (-1.4, cy + 4.0, cz - 0.05), (2.8, 1.0, 0.9), P["trim"], rot=(0, 0, i * 360.0 / n), pivot=(0, cy, cz))                               # their guards
        p.box("disc", (-3.2, cy - 3.2, cz - 0.1), (6.4, 6.4, 0.8), P["main"], rot=(0, 0, 45), pivot=(0, cy, cz))
        p.box("disc", (-1.6, cy - 1.6, cz - 0.4), (3.2, 3.2, 0.5), P["trim"], rot=(0, 0, 45), pivot=(0, cy, cz))
        a.rot("idle", "disc", {24.0 * i / 8: (0, 0, 360.0 * i / 8) for i in range(9)})
        if style == "chariot":                                          # a second, smaller wheel turning the other way and the yoke between them
            p.bone("disc2", (0, cy, cz + 2.0))
            for i in range(8):
                p.box("disc2", (-0.7, cy + 3.0, cz + 2.0), (1.4, 7.0, 0.6), P["trim"], rot=(0, 0, i * 45.0), pivot=(0, cy, cz + 2.0))
            a.rot("idle", "disc2", {24.0 * i / 8: (0, 0, -360.0 * i / 8) for i in range(9)})
            p.box("root", (-9.0, cy - 0.6, cz - 0.4), (18.0, 1.2, 1.0), P["main"])
    else:                                                               # "spikes": the spines of Purgatory
        a.clip("idle", length=4.0, loop=True)
        p.bone("spines", (0, cy, cz))
        for k, (dx, dy, L) in enumerate(((0, 8.0, 7.0), (-4.0, 6.0, 6.0), (4.0, 6.0, 6.0), (-6.5, 2.0, 5.0), (6.5, 2.0, 5.0), (0, -4.0, 5.0))):
            sgx = -1 if dx < 0 else 1
            stepped_spike(p, "spines", (dx, cy + dy, cz), "+y" if dy > 0 else "-y", L, 2.4, P["main"] if k % 2 == 0 else P["trim"], steps=3, thick=1.4)
            p.box("spines", (dx - 0.8, cy + dy - 0.4, cz - 0.2), (1.6, 0.8, 1.8), P["hot"])
        sine_rot(a, "idle", "spines", 4.0, 2, 3.0)
    p.save(a)
    return p


def tail(c, style):
    P = c.pal
    p = c.piece("tail")
    a = Anim()
    a.clip("idle", length=1.8, loop=True)
    if style == "ribbon":                                               # two long cloth ribbons streaming behind
        for k, sx in enumerate((-1.2, 1.2)):
            parent = "root"
            y, z = 11.0, 2.6
            for i in range(4):
                b = "r%d_%d" % (k, i)
                p.bone(b, (sx, y, z), parent=parent)
                p.box(b, (sx - 0.9, y - 0.4, z), (1.8, 0.8, 4.0), P["glass"] if i % 2 else P["cloth"], rot=(10, 0, 0), pivot=(sx, y, z))
                sine_rot(a, "idle", b, 1.8, 1, 16.0, phase=-i * 0.12 + k * 0.3)
                parent = b
                z += 3.8
                y -= 1.2
        p.save(a)
        return p
    mats = [P["alt"], P["main"]]                                         # "flame": a tail of plates ending in a tuft of flame
    y, z, parent = tail_chain(p, a, mats, 4, 3.6, (11.0, 2.4), 2.4, taper=0.3, droop=1.5, tilt=14.0, wave=14.0, length=1.8)
    p.bone("tuft", (0, y, z), parent=parent)
    for k, ang in enumerate((-28, 28)):
        p.box("tuft", (-1.1, y - 3.6, z), (2.2, 4.4, 1.4), P["hot"], rot=(0, ang, 0), pivot=(0, y, z))
    sine_rot(a, "idle", "tuft", 1.8, 1, 18.0, phase=-0.6)
    p.save(a)
    return p


def cape(c, style):
    P = c.pal
    p = c.piece("cape")
    a = Anim()
    a.clip("idle", length=2.4, loop=True)
    for i, cx in enumerate((-3.6, -1.2, 1.2, 3.6)):                     # four hanging panels with a gold hem
        b = "p%d" % i
        p.bone(b, (cx, 23.0, 2.9))
        p.box(b, (cx - 1.2, 3.2, 2.9), (2.4, 19.8, 0.7), P["cloth"] if i % 2 == 0 else P["alt"])
        p.box(b, (cx - 1.25, 3.0, 2.85), (2.5, 0.8, 0.9), P["trim"])
        sine_rot(a, "idle", b, 2.4, 0, 4.0, phase=i * 0.18)
    p.box("root", (-5.2, 22.4, 2.5), (10.4, 1.2, 1.2), P["trim"])
    p.save(a)
    return p


# ====================================================================================================================== one armour
def build(aid):
    c = Ctx(aid)
    made = [helm(c), chest(c)]
    if c.spec["pauldron"] != "none":
        made += [pauldron(c, 0), pauldron(c, 1)]
    made += [vambrace(c, 0), vambrace(c, 1)]
    made += [greave(c, 0), greave(c, 1)]
    for ex in c.spec["extras"]:
        name, _, style = ex.partition(":")
        if name == "skirt":
            made.append(skirt(c))
        elif name == "gauntlet":
            made += [gauntlet(c, 0), gauntlet(c, 1)]
        elif name == "wing":
            made += [wing(c, 0, style), wing(c, 1, style)]
        elif name == "halo":
            made.append(halo(c, style))
        elif name == "backdrop":
            made.append(backdrop(c, style))
        elif name == "tail":
            made.append(tail(c, style))
        elif name == "cape":
            made.append(cape(c, style))
        else:
            raise ValueError(ex)
    return made


def main(only=None):
    total = 0
    for aid in SPECS:
        if only and aid not in only:
            continue
        made = build(aid)
        n = sum(m.cube_count() for m in made)
        total += n
        print("%-20s pieces %2d cubes %4d" % (aid, len(made), n))
    print("total cubes", total)


if __name__ == "__main__":
    # 1.99.0: the armours were remade at the Djinn's quality by tools/gen_erza_armour.py, which writes the SAME file names; this 1.96.0 generator (boxes, 128 to 256 px textures, 7 to 15 pieces
    # an armour) would overwrite the remade models with the old look, so it only runs when asked to with --legacy (it is kept as the record of the first version, never deleted).
    if "--legacy" not in sys.argv:
        print("tools/gen_erza_requip_geo.py is the 1.96.0 generator, superseded by tools/gen_erza_armour.py (1.99.0, the Djinn-quality remake). Pass --legacy to rewrite the old models anyway.")
        sys.exit(0)
    main([a for a in sys.argv[1:] if a != "--legacy"])
