"""Shakudo Magic (1.93.0 rebuild): the geo models of the temporary armour and of the two constructs.

    python3 -B tools/gen_shakudo_geo.py            # writes the models, animations, textures and glow maps
    python3 -B tools/geo_check.py shakudo_armor shakudo_golem shakudo_bulwark

shakudo_armor    the temporary armour as pieces on the humanoid frame, one root bone per piece (the painter draws them one by one, so they
                 can forge in and shatter out like the Djinn full equip): helm (kabuto with a gilt crescent crest and neck lames),
                 chest (do with lamellar bands and a gilt crest disc), waist (hanging tassets), pauldron_r/_l (three layered sode plates),
                 vambrace_r/_l (kote), gauntlet_r/_l, greave_r/_l (cuisse, gilt knee, shin and boot), back (a gilt ring with a glowing rune
                 behind the head, on a pole, with two sash tails). Material: black-purple patinated shakudo with fine gold inlay (the
                 inlay is the glow map). Clips: idle (the ring turns, the sash tails sway), move (faster sway).
shakudo_golem    a 2.3 block alloy golem (the construct the book summons): barrel chest with a glowing gilt core, kabuto head with one
                 eye slit, heavy sode shoulders, big fists. Clips: idle, walk, strike (an overhead double fist), crumble (the end).
shakudo_bulwark  a standing slab of shakudo in a gilt frame, a glowing seal on its face. Clips: idle, rise.
"""
import math
import os
import sys

sys.dont_write_bytecode = True
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from shakudo_kit import Kit, Anim, shakudo, gilt, emissive, sway, bob, rgb          # noqa: E402

SH = shakudo()
SH2 = shakudo(dark=0x1B1520, mid=0x2E2338)
GD = gilt()
RUNE = emissive(0xFFD070, base=0x2A1E10)


def armor():
    k = Kit("shakudo", "armor", 256, 256, seed=14)
    # ------------------------------------------------------------------ helm: kabuto, open face
    k.bone("helm", pivot=(0, 24, 0))
    k.box("helm", (-4, 27, -4), (8, 5, 8), SH, inflate=0.7)                         # the bowl
    k.box("helm", (-4, 31.8, -3), (8, 1, 6), SH2, inflate=0.1)                      # the ridge cap
    k.box("helm", (-5.5, 26.2, -5.6), (11, 1, 3), GD)                               # the brow band (gilt)
    k.box("helm", (-1, 27.2, -5.9), (2, 2, 1), emissive(0xFFD070, base=0x3A2810))   # the brow gem
    k.pair("helm", "helm", (-6.2, 22.4, -3.6), (1, 5, 7), SH, inflate=0.2)           # the cheek guards (fukigaeshi)
    for i, (y, w) in enumerate([(25.5, 10), (23.6, 12), (21.7, 12)]):               # the neck lames (shikoro), stepping out and down
        k.box("helm", (-w // 2, y, 3.0 + 0.5 * i), (w, 2, 2), SH if i != 1 else SH2, inflate=0.15)
    k.pair("helm", "helm", (-4.2, 32.5, -5.2), (1, 7, 1), GD, rotation=(0, 0, 32), pivot=(-3, 32.5, -5))   # the crescent crest, two horns
    k.box("helm", (-1.2, 27, -6.0), (2, 4, 1), GD)
    # ------------------------------------------------------------------ chest: do
    k.bone("chest", pivot=(0, 24, 0))
    k.box("chest", (-4, 12.5, -2), (8, 11, 4), SH, inflate=0.8)                     # the cuirass
    for y in (15, 18, 21):                                                          # lamellar bands
        k.box("chest", (-4.5, y, -3.0), (9, 1, 1), GD, inflate=0.0)
    k.box("chest", (-1.5, 17.5, -3.3), (3, 3, 1), emissive(0xFFD070, base=0x2A1E10))  # the crest disc
    k.box("chest", (-1.5, 17.5, -3.1), (3, 3, 1), GD, inflate=-0.3)
    k.box("chest", (-4, 23, -2), (8, 1, 4), GD, inflate=0.9)                         # the collar rim
    k.box("chest", (-1.5, 17.5, 2.9), (3, 3, 1), GD)                                  # the back crest mount
    # ------------------------------------------------------------------ waist: tassets
    k.bone("waist", pivot=(0, 13, 0))
    k.box("waist", (-4, 11.2, -2), (8, 2, 4), GD, inflate=0.9)                       # the belt
    for i in range(-2, 2):                                                           # front and back tassets
        k.box("waist", (i * 2 + 0.0, 8.0, -3.5), (2, 4, 1), SH if i % 2 == 0 else SH2, rotation=(-6, 0, 0), pivot=(0, 12, -3))
        k.box("waist", (i * 2 + 0.0, 8.0, 2.5), (2, 4, 1), SH if i % 2 == 0 else SH2, rotation=(6, 0, 0), pivot=(0, 12, 3))
    k.pair("waist", "waist", (-5.6, 8.5, -1.5), (1, 4, 3), SH, rotation=(0, 0, 8), pivot=(-5, 12, 0))
    # ------------------------------------------------------------------ pauldrons: three layered sode plates on each shoulder
    r, l = k.pair_bones("pauldron", pivot=(-5, 22, 0))
    k.pair(r, l, (-9.6, 21.6, -3.6), (6, 2, 7), SH, rotation=(0, 0, -8), pivot=(-5, 22, 0))
    k.pair(r, l, (-10.3, 19.4, -3.4), (6, 2, 7), SH2, rotation=(0, 0, -14), pivot=(-5, 22, 0))
    k.pair(r, l, (-11.0, 17.2, -3.2), (6, 2, 6), SH, rotation=(0, 0, -20), pivot=(-5, 22, 0))
    k.pair(r, l, (-9.9, 21.4, -3.8), (6, 1, 1), GD, rotation=(0, 0, -8), pivot=(-5, 22, 0))
    k.pair(r, l, (-9.0, 22.8, -0.5), (3, 1, 1), GD, rotation=(0, 0, -8), pivot=(-5, 22, 0))
    # ------------------------------------------------------------------ vambraces (kote)
    r, l = k.pair_bones("vambrace", pivot=(-5, 22, 0))
    k.pair(r, l, (-8, 13.2, -2), (4, 6, 4), SH, inflate=0.75)
    k.pair(r, l, (-8, 17.6, -2), (4, 1, 4), GD, inflate=0.95)
    k.pair(r, l, (-8, 13.0, -2), (4, 1, 4), GD, inflate=0.95)
    k.pair(r, l, (-8.6, 14.4, -3.2), (5, 3, 1), SH2, inflate=0.0)
    # ------------------------------------------------------------------ gauntlets
    r, l = k.pair_bones("gauntlet", pivot=(-5, 22, 0))
    k.pair(r, l, (-8, 11.6, -2), (4, 3, 4), SH2, inflate=0.8)
    k.pair(r, l, (-8, 11.2, -3.2), (4, 2, 1), GD, inflate=0.0)
    for i in range(4):
        k.pair(r, l, (-7.9 + i * 0.97, 10.4, -3.0), (1, 2, 1), SH, inflate=0.0)
    k.pair(r, l, (-5.8, 12.4, -3.2), (1, 2, 1), GD, inflate=0.0)
    # ------------------------------------------------------------------ greaves: cuisse, gilt knee, shin, boot
    r, l = k.pair_bones("greave", pivot=(-1.9, 12, 0))
    k.pair(r, l, (-4, 6.2, -2), (4, 5, 4), SH, inflate=0.7)
    k.pair(r, l, (-3.2, 5.0, -3.2), (3, 3, 1), GD, inflate=0.0)
    k.pair(r, l, (-4, 0.9, -2), (4, 5, 4), SH2, inflate=0.6)
    k.pair(r, l, (-4, 3.4, -2), (4, 1, 4), GD, inflate=0.78)
    k.pair(r, l, (-4, 0, -2), (4, 2, 4), SH, inflate=0.55)
    k.pair(r, l, (-4, 0, -4), (4, 1, 2), GD, inflate=0.0)
    k.pair(r, l, (-4.2, 6.6, -2), (1, 4, 4), SH2, inflate=0.0)
    # ------------------------------------------------------------------ back: a gilt ring with a glowing rune on a pole, two sash tails
    k.bone("back", pivot=(0, 22, 3))
    k.box("back", (-0.5, 8, 3.2), (1, 20, 1), SH2)                                   # the pole (sashimono)
    k.box("back", (-2.5, 21, 2.6), (5, 2, 2), GD)                                    # the pole clasp
    k.bone("ring", parent="back", pivot=(0, 29, 4.5))
    n = 14
    for i in range(n):
        a = 2 * math.pi * i / n
        cx, cy = 9.0 * math.cos(a), 29 + 9.0 * math.sin(a)
        k.box("ring", (cx - 2, cy - 0.5, 4.0), (4, 1, 1), GD if i % 2 == 0 else SH, rotation=(0, 0, math.degrees(a) + 90), pivot=(cx, cy, 4.5))
    k.box("ring", (-2, 27, 4.0), (4, 4, 1), GD)
    k.box("ring", (-1, 28, 3.6), (2, 2, 1), RUNE)
    for i in range(4):                                                               # four spokes
        a = math.pi / 4 + i * math.pi / 2
        cx, cy = 4.5 * math.cos(a), 29 + 4.5 * math.sin(a)
        k.box("ring", (cx - 3, cy - 0.5, 4.2), (6, 1, 1), SH2, rotation=(0, 0, math.degrees(a)), pivot=(cx, cy, 4.7))
    k.bone("tail_a", parent="back", pivot=(-1.5, 13, 2.6))
    k.box("tail_a", (-3, 4, 2.6), (3, 9, 1), SH)
    k.box("tail_a", (-3, 3, 2.6), (3, 1, 1), GD)
    k.bone("tail_b", parent="back", pivot=(1.5, 13, 2.6))
    k.box("tail_b", (0, 4, 2.6), (3, 9, 1), SH)
    k.box("tail_b", (0, 3, 2.6), (3, 1, 1), GD)
    a = Anim()
    a.clip("idle", length=12.0, loop=True)
    a.rot("idle", "ring", {0.0: (0, 0, 0), 6.0: (0, 0, 180), 12.0: (0, 0, 360)})
    sway(a, "idle", "tail_a", 0, 7, 3.0, 0.0)
    sway(a, "idle", "tail_b", 0, 7, 3.0, 1.6)
    a.clip("move", length=12.0, loop=True)
    a.rot("move", "ring", {0.0: (0, 0, 0), 6.0: (0, 0, 360), 12.0: (0, 0, 720)})
    sway(a, "move", "tail_a", 0, 22, 1.5, 0.0)
    sway(a, "move", "tail_b", 0, 22, 1.5, 1.6)
    k.save(a)


def golem():
    k = Kit("shakudo", "golem", 256, 256, seed=21)
    k.bone("root", pivot=(0, 0, 0))
    k.bone("hips", parent="root", pivot=(0, 22, 0))
    k.box("hips", (-7, 18, -4), (14, 6, 8), SH2)
    k.box("hips", (-8, 21.5, -4.6), (16, 1, 9), GD)
    k.bone("torso", parent="hips", pivot=(0, 24, 0))
    k.box("torso", (-10, 24, -6), (20, 14, 12), SH)                                  # barrel chest
    for y in (27, 31, 35):
        k.box("torso", (-10.5, y, -6.6), (21, 1, 13), GD, inflate=0.0)
    k.box("torso", (-3, 29, -6.9), (6, 6, 1), GD)                                    # the core mount
    k.box("torso", (-2, 30, -7.3), (4, 4, 1), emissive(0xFFD070, base=0x3A2810))     # the core
    k.box("torso", (-8, 37, -5), (16, 2, 10), SH2)                                   # the collar
    k.bone("head", parent="torso", pivot=(0, 40, 0))
    k.box("head", (-4, 38, -4), (8, 7, 8), SH)
    k.box("head", (-4.5, 44, -4.5), (9, 3, 9), SH2)                                  # helm crown
    k.box("head", (-5, 43, -5), (10, 1, 2), GD)
    k.box("head", (-3, 40.5, -4.6), (6, 1, 1), emissive(0xFFB040, base=0x2A1810))   # the eye slit
    k.box("head", (-1, 47, -3), (2, 5, 6), GD)                                       # the crest fin
    for s_name, sgn in (("r", -1), ("l", 1)):
        ox = 10 if sgn > 0 else -10
        k.bone("arm_" + s_name, parent="torso", pivot=(ox + (2 if sgn > 0 else -2), 37, 0))
        bx = 9 if sgn > 0 else -15
        k.box("arm_" + s_name, (bx, 37, -3.6), (6, 3, 7), SH2, rotation=(0, 0, -sgn * 10), pivot=(ox + sgn * 2, 38, 0))      # the shoulder plates
        k.box("arm_" + s_name, (bx - sgn * 0.5, 34.5, -3.2), (6, 3, 6), SH, rotation=(0, 0, -sgn * 16), pivot=(ox + sgn * 2, 38, 0))
        ux = 10 if sgn > 0 else -16
        k.box("arm_" + s_name, (ux, 22, -3), (6, 14, 6), SH)                          # the upper arm
        k.box("arm_" + s_name, (ux - 0.5, 29, -3.5), (7, 1, 7), GD)
        k.bone("fore_" + s_name, parent="arm_" + s_name, pivot=(ox + sgn * 3, 22, 0))
        k.box("fore_" + s_name, (ux - 0.5, 9, -3.6), (7, 13, 7), SH2)               # the forearm
        k.box("fore_" + s_name, (ux - 0.5, 8, -3.6), (7, 1, 7), GD)
        k.box("fore_" + s_name, (ux - 1, 1, -4.5), (8, 7, 9), SH)                    # the fist
        k.box("fore_" + s_name, (ux - 1, 4, -5), (8, 1, 1), GD)
    for s_name, sgn in (("r", -1), ("l", 1)):
        px = 3 if sgn > 0 else -3
        k.bone("leg_" + s_name, parent="hips", pivot=(px, 18, 0))
        bx = 0 if sgn > 0 else -7
        k.box("leg_" + s_name, (bx, 8, -3.5), (7, 10, 7), SH)
        k.box("leg_" + s_name, (bx, 11, -4), (7, 2, 1), GD)
        k.bone("shin_" + s_name, parent="leg_" + s_name, pivot=(px, 8, 0))
        k.box("shin_" + s_name, (bx - 0.5, 1.5, -3.8), (8, 7, 8), SH2)
        k.box("shin_" + s_name, (bx - 1, 0, -5.5), (9, 2, 11), SH)
        k.box("shin_" + s_name, (bx - 1, 1.5, -5.6), (9, 1, 1), GD)
    k.bone("sash", parent="hips", pivot=(0, 19, 4))
    k.box("sash", (-2, 8, 4), (4, 11, 1), SH)
    k.box("sash", (-2, 7, 4), (4, 1, 1), GD)
    a = Anim()
    a.clip("idle", length=3.0, loop=True)
    bob(a, "idle", "torso", 0.6, 3.0)
    sway(a, "idle", "arm_r", 0, 3, 3.0, 0.0)
    sway(a, "idle", "arm_l", 0, 3, 3.0, math.pi)
    sway(a, "idle", "sash", 0, 6, 3.0, 0.5)
    a.clip("walk", length=1.0, loop=True)
    sway(a, "walk", "leg_r", 0, 32, 1.0, 0.0)
    sway(a, "walk", "leg_l", 0, 32, 1.0, math.pi)
    sway(a, "walk", "shin_r", 0, 14, 1.0, math.pi / 2 + 0.6)
    sway(a, "walk", "shin_l", 0, 14, 1.0, math.pi * 1.5 + 0.6)
    sway(a, "walk", "arm_r", 0, 26, 1.0, math.pi)
    sway(a, "walk", "arm_l", 0, 26, 1.0, 0.0)
    sway(a, "walk", "torso", 1, 5, 1.0, 0.0)
    bob(a, "walk", "hips", 0.9, 0.5)
    a.clip("strike", length=0.9, loop=False)
    a.rot("strike", "arm_r", {0.0: (0, 0, 0), 0.3: (-150, 0, 0), 0.45: (-160, 0, 0), 0.6: (20, 0, 0), 0.9: (0, 0, 0)})
    a.rot("strike", "arm_l", {0.0: (0, 0, 0), 0.3: (-150, 0, 0), 0.45: (-160, 0, 0), 0.6: (20, 0, 0), 0.9: (0, 0, 0)})
    a.rot("strike", "fore_r", {0.0: (0, 0, 0), 0.3: (-25, 0, 0), 0.6: (-40, 0, 0), 0.9: (0, 0, 0)})
    a.rot("strike", "fore_l", {0.0: (0, 0, 0), 0.3: (-25, 0, 0), 0.6: (-40, 0, 0), 0.9: (0, 0, 0)})
    a.rot("strike", "torso", {0.0: (0, 0, 0), 0.3: (-14, 0, 0), 0.45: (-18, 0, 0), 0.6: (22, 0, 0), 0.9: (0, 0, 0)})
    a.clip("crumble", length=1.0, loop=False)
    a.rot("crumble", "head", {0.0: (0, 0, 0), 0.6: (35, 0, 12), 1.0: (60, 0, 20)})
    a.rot("crumble", "arm_r", {0.0: (0, 0, 0), 1.0: (0, 0, -40)})
    a.rot("crumble", "arm_l", {0.0: (0, 0, 0), 1.0: (0, 0, 40)})
    a.rot("crumble", "torso", {0.0: (0, 0, 0), 1.0: (14, 0, 0)})
    a.pos("crumble", "hips", {0.0: (0, 0, 0), 1.0: (0, -6, 0)})
    k.save(a)


def bulwark():
    k = Kit("shakudo", "bulwark", 128, 128, seed=33)
    k.bone("root", pivot=(0, 0, 0))
    k.bone("slab", parent="root", pivot=(0, 0, 0))
    k.box("slab", (-12, 2, -2), (24, 40, 4), SH)
    k.box("slab", (-14, 0, -3), (28, 3, 6), GD)                                      # the foot
    k.box("slab", (-14, 42, -3), (28, 3, 6), GD)                                     # the crown rail
    k.box("slab", (-13, 3, -2.6), (2, 40, 1), GD)
    k.box("slab", (11, 3, -2.6), (2, 40, 1), GD)
    k.bone("seal", parent="root", pivot=(0, 24, -3))
    k.box("seal", (-5, 19, -2.9), (10, 10, 1), GD)
    k.box("seal", (-3, 21, -3.4), (6, 6, 1), emissive(0xFFD070, base=0x2A1E10))
    k.box("seal", (-1, 29.5, -3.2), (2, 5, 1), GD)
    k.box("seal", (-1, 13.5, -3.2), (2, 5, 1), GD)
    a = Anim()
    a.clip("idle", length=3.0, loop=True)
    a.scale("idle", "seal", {0.0: (1, 1, 1), 1.5: (1.12, 1.12, 1), 3.0: (1, 1, 1)})
    a.rot("idle", "seal", {0.0: (0, 0, 0), 1.5: (0, 0, 8), 3.0: (0, 0, 0)})
    a.clip("rise", length=0.6, loop=False)
    a.pos("rise", "slab", {0.0: (0, -44, 0), 0.4: (0, 2, 0), 0.6: (0, 0, 0)})
    a.pos("rise", "seal", {0.0: (0, -44, 0), 0.4: (0, 2, 0), 0.6: (0, 0, 0)})
    k.save(a)


if __name__ == "__main__":
    armor()
    golem()
    bulwark()
