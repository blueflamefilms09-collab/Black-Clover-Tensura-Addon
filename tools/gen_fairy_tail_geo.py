"""Fairy Tail skills: the temporary armour of the ten timed forms (Dragon Force, Lightning Flame Dragon Mode, Fire Dragon King's Mode, Ice Devil's Rage, the Sky Dragon's
Armor, Lightning Body, Iron Dragon Scales, the Shadow Iron Dragon, Water Body, Rain Woman). One GeoLite model per piece (helm, chest, two pauldrons, two vambraces and an
accent: wings, a tail, a bolt halo or a veil), authored in whole-body coordinates (see tools/water_aura_lib.py), plus the body shell texture of each form:

    python3 -B tools/gen_fairy_tail_geo.py            # writes geo/entity/ft_<form>_arm_*.geo.json, animations, textures + _glow maps, textures/aura/ft_<form>_shell*.png
    python3 -B tools/geo_check.py ft_dragon_force_arm_helm ...

The forms and their order are fairytail/FtForms.java (the order is the armour style number); the pieces are listed in client/aura/FairyTailAura.java. Every piece is forged
in and shattered out by EquipAnimation exactly like the Djinn full equip.
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from water_aura_lib import Mat, Piece, Anim, mbox, stepped_spike, sine_rot, spin_y, shell_texture  # noqa: E402


def forms():
    """key -> materials and the look flags (helm: horns | crown | fins | bolts | tiara; accent: wings | tail | halo | veil)."""
    return {
        "dragon_force": dict(
            a=Mat("scales", "B8281F", dark="5E0F0C", light="F0604A", glow="FF7A22", glow_amount=0.7, seed=11),
            b=Mat("plate", "8A1A18", dark="3E0808", light="D8443A", glow="FF6A1E", glow_amount=0.6, seed=12),
            trim=Mat("metal", "E2B23C", dark="6B4609", light="FFEBA3", glow="FFC866", glow_amount=0.55, seam=1, seed=13),
            fx=Mat("flame", "FF7C1E", dark="B3200C", light="FFE276", glow="FF9830", glow_amount=0.95, seed=14, grain=0.04),
            shell=Mat("scales", "7A1812", dark="2E0605", light="E24A2C", glow="FF5A18", glow_amount=0.9, alpha=235, seed=15),
            helm="horns", accent="wings"),
        "lightning_flame": dict(
            a=Mat("plate", "F2E9D2", dark="9B8F76", light="FFFFFF", glow="FFE9A0", glow_amount=0.6, seed=21),
            b=Mat("plate", "C73418", dark="650F08", light="FF7A3A", glow="FF8A2A", glow_amount=0.7, seed=22),
            trim=Mat("bolt", "FFE45A", dark="B8860B", light="FFFFFF", glow="FFF2A0", glow_amount=1.0, seed=23),
            fx=Mat("flame", "FFB02E", dark="E0480F", light="FFF6B0", glow="FFD060", glow_amount=1.0, seed=24, grain=0.04),
            shell=Mat("bolt", "5A2A14", dark="1A0A05", light="FFE26A", glow="FFC24A", glow_amount=1.0, alpha=235, seed=25),
            helm="bolts", accent="wings"),
        "fire_king": dict(
            a=Mat("plate", "2A1214", dark="0C0405", light="6A2A22", glow="FF4A14", glow_amount=0.9, seed=31),
            b=Mat("plate", "7A0F14", dark="2A0305", light="C8302A", glow="FF3A12", glow_amount=0.8, seed=32),
            trim=Mat("metal", "F0C050", dark="7A5008", light="FFF2B0", glow="FFD070", glow_amount=0.8, seam=1, seed=33),
            fx=Mat("flame", "FF5A14", dark="A0150A", light="FFD060", glow="FF8A22", glow_amount=1.0, seed=34, grain=0.04),
            shell=Mat("crystal", "1E0A0A", dark="070202", light="FF7A22", glow="FF5A14", glow_amount=1.0, cells=14, seamonly=True, alpha=240, seed=35),
            helm="crown", accent="wings"),
        "ice_devil": dict(
            a=Mat("crystal", "A8E4F8", dark="3E7FA8", light="F2FCFF", glow="B8F0FF", glow_amount=0.7, seed=41),
            b=Mat("plate", "27506E", dark="0E2438", light="6AA8D0", glow="7AD0FF", glow_amount=0.6, seed=42),
            trim=Mat("crystal", "E6F8FF", dark="7AB4D0", light="FFFFFF", glow="D0F6FF", glow_amount=0.8, seed=43),
            fx=Mat("crystal", "BFEFFF", dark="5AA0C8", light="FFFFFF", glow="C8F4FF", glow_amount=0.9, seed=44),
            shell=Mat("crystal", "3E7FA8", dark="12304A", light="C8F4FF", glow="9FE4FF", glow_amount=0.9, cells=12, seamonly=True, alpha=220, seed=45),
            helm="horns", accent="tail"),
        "sky_armor": dict(
            a=Mat("feather", "E8FFF6", dark="8CC8B4", light="FFFFFF", glow="BFF5E8", glow_amount=0.7, seed=51),
            b=Mat("cloth", "49B9A0", dark="1E6A5C", light="9AF0DC", glow="9AF5E0", glow_amount=0.5, seed=52),
            trim=Mat("metal", "F2E2A0", dark="8A7430", light="FFFFFF", glow="FFF4C0", glow_amount=0.6, seam=1, seed=53),
            fx=Mat("wisp", "D8FFF0", dark="7ACCB0", light="FFFFFF", glow="E0FFF6", glow_amount=0.9, seed=54),
            shell=Mat("wisp", "BFF0E2", dark="3E9C86", light="FFFFFF", glow="BFF5E8", glow_amount=0.8, alpha=150, seed=55),
            helm="fins", accent="wings"),
        "lightning_body": dict(
            a=Mat("plate", "2A2E3A", dark="0A0C12", light="6A7288", glow="FFE45A", glow_amount=0.6, seed=61),
            b=Mat("plate", "F0D030", dark="8A7008", light="FFF6A0", glow="FFF08A", glow_amount=0.8, seed=62),
            trim=Mat("bolt", "FFF2A0", dark="C8A010", light="FFFFFF", glow="FFFFC0", glow_amount=1.0, seed=63),
            fx=Mat("bolt", "FFF6B0", dark="E8C020", light="FFFFFF", glow="FFFFD0", glow_amount=1.0, seed=64),
            shell=Mat("bolt", "3A3A18", dark="0C0C06", light="FFF06A", glow="FFE45A", glow_amount=1.0, alpha=200, seed=65),
            helm="bolts", accent="halo"),
        "iron_scales": dict(
            a=Mat("scales", "8E96A4", dark="3A404C", light="DDE3EC", glow="AEB8C8", glow_amount=0.25, seed=71),
            b=Mat("plate", "5C6472", dark="22262E", light="A8B0BE", glow="9AA4B4", glow_amount=0.2, seed=72),
            trim=Mat("metal", "C8CED8", dark="5A6070", light="FFFFFF", glow="E0E6F0", glow_amount=0.3, seam=1, seed=73),
            fx=Mat("metal", "A8B0BE", dark="4A505C", light="F0F4FA", glow="D0D8E4", glow_amount=0.3, seed=74),
            shell=Mat("scales", "6A7280", dark="262A32", light="C0C8D4", glow="A8B2C2", glow_amount=0.3, alpha=240, seed=75),
            helm="horns", accent="tail"),
        "shadow_iron": dict(
            a=Mat("scales", "3A2A4C", dark="120A1C", light="7A5C9C", glow="B070FF", glow_amount=0.6, seed=81),
            b=Mat("plate", "1C1428", dark="06040A", light="4A3868", glow="9A5CE8", glow_amount=0.55, seed=82),
            trim=Mat("metal", "8A8AA0", dark="30304A", light="D0D0E8", glow="C09CFF", glow_amount=0.5, seam=1, seed=83),
            fx=Mat("wisp", "8A4CD8", dark="3A1C6C", light="E0B8FF", glow="C080FF", glow_amount=1.0, seed=84),
            shell=Mat("wisp", "2A1C3C", dark="08040E", light="9A5CE8", glow="B070FF", glow_amount=0.9, alpha=235, seed=85),
            helm="horns", accent="tail"),
        "water_body": dict(
            a=Mat("water", "5CB0F0", dark="2468B0", light="E0F6FF", glow="9AD8FF", glow_amount=0.6, alpha=190, seed=91),
            b=Mat("water", "3A8CE0", dark="144C98", light="B8E6FF", glow="7AC8FF", glow_amount=0.5, alpha=190, seed=92),
            trim=Mat("crystal", "CFF0FF", dark="5AA0D8", light="FFFFFF", glow="E0F8FF", glow_amount=0.8, alpha=210, seed=93),
            fx=Mat("water", "7CC8FF", dark="2C78C8", light="FFFFFF", glow="C0EAFF", glow_amount=0.8, alpha=180, seed=94),
            shell=Mat("water", "4AA0EC", dark="1C5AA8", light="E0F6FF", glow="8AD0FF", glow_amount=0.7, alpha=170, seed=95),
            helm="tiara", accent="veil"),
        "rain_woman": dict(
            a=Mat("cloth", "3E5A8C", dark="161E3C", light="8AA8D8", glow="6A9CF0", glow_amount=0.35, seed=101),
            b=Mat("cloth", "28406E", dark="0C1226", light="6A88C0", glow="5A88E0", glow_amount=0.3, seed=102),
            trim=Mat("crystal", "BFE0FF", dark="5A90D0", light="FFFFFF", glow="D8EEFF", glow_amount=0.8, seed=103),
            fx=Mat("water", "6AB4F4", dark="2C70C0", light="F0FAFF", glow="B0E0FF", glow_amount=0.8, alpha=180, seed=104),
            shell=Mat("cloth", "34508A", dark="101830", light="8AB0E8", glow="6AA0F0", glow_amount=0.4, alpha=200, seed=105),
            helm="tiara", accent="veil"),
    }


def pid(key):
    return "ft_" + key


def helm(key, F):
    p = Piece(pid(key), "arm_helm", density=3)
    p.box("root", (-4.75, 29.0, -4.75), (9.5, 1.6, 0.8), F["trim"])               # the band round the brow
    p.box("root", (-4.75, 29.0, 3.95), (9.5, 1.6, 0.8), F["trim"])
    p.box("root", (-4.75, 29.0, -3.95), (0.8, 1.6, 7.9), F["trim"])
    p.box("root", (3.95, 29.0, -3.95), (0.8, 1.6, 7.9), F["trim"])
    p.box("root", (-4.5, 31.0, -4.5), (9.0, 1.2, 9.0), F["a"])                    # the cap
    style = F["helm"]
    if style == "horns":
        for sx in (-1, 1):
            stepped_spike(p, "root", (sx * 3.3, 32.0, -0.5), "+y", 5.0, 2.2, F["trim"], steps=3, thick=2.0)
            stepped_spike(p, "root", (sx * 4.3, 31.2, 1.5), "+z", 4.0, 1.8, F["b"], steps=3, thick=1.8)
    elif style == "crown":
        for k in range(-2, 3):
            h = 4.6 - abs(k) * 0.9
            stepped_spike(p, "root", (k * 1.9, 32.0, -3.6), "+y", h, 1.6, F["trim"], steps=3, thick=1.2)
        for sx in (-1, 1):
            stepped_spike(p, "root", (sx * 4.3, 31.0, 0.0), "+x" if sx > 0 else "-x", 2.4, 1.8, F["b"], steps=2, thick=1.6)
    elif style == "fins":
        for sx in (-1, 1):
            p.box("root", (sx * 4.7 - 0.4, 27.0, -1.5), (0.8, 4.4, 5.0), F["a"], rot=(0, 0, sx * 20), pivot=(sx * 4.7, 28.0, 0))
            p.box("root", (sx * 5.6 - 0.4, 28.5, -1.0), (0.8, 3.4, 3.6), F["trim"], rot=(0, 0, sx * 32), pivot=(sx * 5.6, 29.0, 0))
    elif style == "bolts":
        for k in (-2, -1, 0, 1, 2):
            p.box("root", (k * 1.9 - 0.5, 32.2, -4.0 + abs(k) * 0.6), (1.0, 3.6 - abs(k) * 0.5, 1.0), F["trim"], rot=(0, 0, k * 14), pivot=(k * 1.9, 32.2, -3.5))
        for sx in (-1, 1):
            p.box("root", (sx * 4.9 - 0.4, 28.0, -1.0), (0.8, 5.0, 2.0), F["b"])
    else:                                                                          # tiara
        p.box("root", (-1.0, 30.2, -5.0), (2.0, 3.2, 0.8), F["trim"])
        for sx in (-1, 1):
            p.box("root", (sx * 2.4 - 0.7, 30.2, -4.9), (1.4, 2.2, 0.8), F["b"], rot=(0, 0, sx * 20), pivot=(sx * 2.4, 30.2, -4.5))
    p.save()
    return p


def chest(key, F):
    p = Piece(pid(key), "arm_chest", density=3)
    p.box("root", (-4.6, 16.4, -2.75), (9.2, 7.6, 1.1), F["a"])                    # the breastplate
    p.box("root", (-4.45, 14.7, -2.65), (8.9, 1.7, 0.95), F["trim"])               # the lames of the belly
    p.box("root", (-4.3, 12.9, -2.55), (8.6, 1.7, 0.85), F["b"])
    p.box("root", (-1.5, 19.0, -3.2), (3.0, 3.0, 0.6), F["fx"], rot=(0, 0, 45), pivot=(0, 20.5, -3.0))     # the emblem of the form
    p.box("root", (-4.8, 23.4, -2.9), (9.6, 1.9, 5.8), F["trim"])                  # the gorget
    p.box("root", (-4.7, 12.2, 2.05), (9.4, 11.2, 0.95), F["a"])                   # the back plate
    p.box("root", (-0.5, 14.0, 3.0), (1.0, 8.0, 0.5), F["fx"])                     # the vent along the spine
    mbox(p, "root", 0, (-4.95, 13.2, -1.9), (0.9, 8.6, 3.8), F["b"])
    mbox(p, "root", 1, (-4.95, 13.2, -1.9), (0.9, 8.6, 3.8), F["b"])
    p.save()
    return p


def pauldron(key, F, side):
    p = Piece(pid(key), "arm_pauldron_" + ("l" if side else "r"), density=3)
    mbox(p, "root", side, (-9.8, 21.0, -3.6), (5.6, 3.6, 7.2), F["a"])
    mbox(p, "root", side, (-9.2, 24.0, -3.2), (4.6, 1.3, 6.4), F["trim"])
    mbox(p, "root", side, (-10.3, 18.8, -2.9), (1.0, 2.6, 5.8), F["b"])
    bx = -7.0 if side == 0 else 7.0
    for z in (-1.9, 1.9):
        stepped_spike(p, "root", (bx, 25.3, z), "+y", 3.0, 1.8, F["trim"], steps=2, thick=1.4)
    p.save()
    return p


def vambrace(key, F, side):
    p = Piece(pid(key), "arm_vambrace_" + ("l" if side else "r"), density=3)
    mbox(p, "root", side, (-8.55, 14.3, -2.6), (4.9, 4.9, 5.2), F["a"])
    mbox(p, "root", side, (-8.75, 18.2, -2.8), (5.3, 1.0, 5.6), F["trim"])
    mbox(p, "root", side, (-8.5, 11.5, -2.6), (4.9, 2.8, 5.2), F["b"])              # the gauntlet below the elbow guard
    bx = -8.7 if side == 0 else 8.7
    stepped_spike(p, "root", (bx, 16.6, 0.0), "-x" if side == 0 else "+x", 3.0, 2.4, F["trim"], steps=2, thick=0.9)
    p.save()
    return p


def accent(key, F):
    kind = F["accent"]
    p = Piece(pid(key), "arm_accent", density=3)
    a = Anim()
    a.clip("idle", length=1.6, loop=True)
    if kind == "wings":                                                            # both wings in one model, fanned feathers of the form's material
        for side in (0, 1):
            sg = 1.0 if side else -1.0
            P = (sg * 2.4, 21.6, 2.6)
            bone = "w%d" % side
            p.bone(bone, P)
            sine_rot(a, "idle", bone, 1.6, 2, -9.0 * sg, 0.0)
            fan = [(-46, 12.5, 3.4), (-26, 15.0, 3.8), (-8, 16.0, 3.8), (12, 14.0, 3.6), (30, 10.5, 3.2)]
            for i, (rz, L, w) in enumerate(fan):
                b = "f%d_%d" % (side, i)
                p.bone(b, P, parent=bone, rotation=(0, sg * (20 + i * 3), -sg * rz))
                o = (P[0] - L, P[1] - w / 2, P[2]) if side == 0 else (P[0], P[1] - w / 2, P[2])
                p.box(b, o, (L, w, 0.5), F["fx"] if i % 2 else F["a"])
                sine_rot(a, "idle", b, 1.6, 2, 5.0 * sg, 0.22 + i * 0.05)
            p.box(bone, (P[0] - (9.0 if side == 0 else 0.0), P[1] - 0.6, P[2] - 0.2), (9.0, 1.2, 1.0), F["trim"], rot=(0, 0, sg * 36), pivot=P)
    elif kind == "tail":                                                           # a segmented tail ending in a blade
        z, y, parent = 2.4, 11.0, "root"
        for i in range(5):
            b = "t%d" % i
            p.bone(b, (0, y, z), parent=parent)
            w = 2.6 - i * 0.3
            p.box(b, (-w / 2, y - w / 2, z), (w, w, 3.6), F["b"] if i % 2 else F["a"], rot=(14, 0, 0), pivot=(0, y, z))
            sine_rot(a, "idle", b, 1.6, 1, 14.0, phase=-i * 0.11)
            parent = b
            z += 3.4
            y -= 1.4
        p.bone("tip", (0, y, z), parent=parent)
        stepped_spike(p, "tip", (0, y, z), "+z", 5.0, 2.2, F["trim"], steps=3, thick=1.2)
        sine_rot(a, "idle", "tip", 1.6, 1, 18.0, phase=-0.6)
        for sx in (-1, 1):                                                         # spines down the back
            for k in range(3):
                stepped_spike(p, "root", (sx * 1.6, 21.0 - k * 3.5, 3.1), "+z", 2.6, 1.4, F["trim"], steps=2, thick=1.0)
    elif kind == "halo":                                                           # a ring of bolts turning behind the back
        cy, cz = 19.0, 8.0
        p.bone("disc", (0, cy, cz))
        for i in range(8):
            p.box("disc", (-3.0, cy + 7.0, cz), (6.0, 1.2, 0.8), F["trim"], rot=(0, 0, i * 45.0), pivot=(0, cy, cz))
        p.box("disc", (-2.6, cy - 2.6, cz - 0.1), (5.2, 5.2, 0.8), F["b"], rot=(0, 0, 45), pivot=(0, cy, cz))
        a.rot("idle", "disc", {1.6 * i / 8: (0, 0, 360.0 * i / 8) for i in range(9)})
    else:                                                                          # veil: flowing sheets from the shoulders
        for side in (0, 1):
            sg = 1.0 if side else -1.0
            for k in range(3):
                b = "v%d_%d" % (side, k)
                p.bone(b, (sg * (2.0 + k * 1.4), 23.0, 3.0))
                p.box(b, (sg * (2.0 + k * 1.4) - 0.9, 8.0 - k * 1.5, 2.6), (1.8, 15.0 + k * 1.5, 0.6), F["fx"] if k % 2 else F["b"])
                sine_rot(a, "idle", b, 1.6, 0, 7.0, phase=side * 0.3 + k * 0.17)
    p.save(a)
    return p


def build(key, F):
    shell_texture("ft_%s_shell" % key, F["shell"])
    made = [helm(key, F), chest(key, F), accent(key, F)]
    for s in (0, 1):
        made += [pauldron(key, F, s), vambrace(key, F, s)]
    return made


def main():
    total = 0
    for key, F in forms().items():
        for m in build(key, F):
            print("%-34s cubes %3d" % (m.id, m.cube_count()))
            total += m.cube_count()
    print("total cubes", total)


if __name__ == "__main__":
    main()
