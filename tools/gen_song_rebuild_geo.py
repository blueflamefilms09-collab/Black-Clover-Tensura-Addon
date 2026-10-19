"""Song Magic (1.93.0 rebuild) geo models: the Concert Regalia armour, the note familiar, the choir spirit and the metronome.

    python3 -B tools/gen_song_rebuild_geo.py        # writes every model, animation and texture, runs tools/geo_check.py

Models (assets/nusmp/geo/entity/song_<name>.geo.json, animations/entity/..., textures/entity/song_<name>.png + _glow.png):
  armour     (Space.PLAYER, one root bone per piece, drawn piece by piece by client/aura/SongArmourKit with the Djinn equip animation)
             helm (circlet, treble-clef crest, two trumpet-bell ear horns, plume), chest (brass breastplate with a lyre of glowing strings),
             back (a staff cape: five glowing staff lines on ivory cloth), tails (conductor's tails), lyre (an upright lyre halo behind the
             back), note_a / note_b / note_c (three eighth notes orbiting the torso), pauldron_r/l (trumpet bells), vambrace_r/l (piano-key
             cuffs), gauntlet_r/l (brass gloves with picks), greave_r/l (shin plates with a bell flare). clip: idle.
  note       the note familiar (the Staccato bolt's body): a glossy eighth note with a glowing core and a ring of grace notes. clips: idle, strike
  choir      the choir spirit: a hooded singer in a flowing robe with a songbook, a halo and a wisp tail; the jaw opens with the song. clips: idle, sing
  metronome  the tempo construct: a brass pyramid with a swinging pendulum and a sliding weight. clip: idle

The look: warm brass with engraved scrolls, ivory piano keys, rose velvet, strings and note glyphs that glow pink (0xFFB4E8, the Song colour).
File frame: y up, feet at the origin, the front on -z (see tools/geo_builder.py).
"""
import math
import os
import sys

sys.dont_write_bytecode = True
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from song_rebuild_kit import Atlas, material, loop, sin_loop, bob_loop, spin_loop, check   # noqa: E402

KEY = "song"
PINK = 0xFFB4E8

material("brass", 0xD9A441, "engrave", glow=PINK, glow_strength=90, spec=0xFFF1B8)
material("brass_dark", 0x8A6422, "plate", glow=PINK, glow_strength=0, dark=0.5)
material("ivory", 0xEFE8D6, "keys", glow=PINK, glow_strength=0, black=(30, 24, 40), dark=0.62)
material("velvet", 0xA33E78, "cloth", glow=PINK, glow_strength=0, dark=0.5)
material("staff", 0xF0E4C8, "brushed", glow=PINK, glow_strength=0, dark=0.7)
material("string", 0xE8A0D0, "emit", glow=0xFFB4E8, glow_strength=255, light=1.0, dark=0.85)
material("glowrose", 0xD870B4, "emit", glow=0xFFA0DC, glow_strength=235, light=1.0, dark=0.85)
material("bellinner", 0x3A1830, "core", glow=0xFF90D8, glow_strength=210, dark=0.6)
material("spirit", 0xE8D8F8, "wisp", glow=0xFFC8F0, glow_strength=170, alpha=210, dark=0.78, light=1.1)
material("spiritrobe", 0xD7C4EE, "cloth", glow=0xFFC8F0, glow_strength=90, alpha=225, dark=0.74, light=1.12)
material("woodbrass", 0x7A4A22, "plate", glow=PINK, glow_strength=0)


def bell(A, bone, base, tilt, length, w0, w1, steps, mat, inner, pivot):
    """A trumpet bell: a stack of cubes widening from w0 to w1 along +y from `base`, the whole thing tipped by `tilt` degrees about z around `pivot`;
    the mouth gets a recessed glowing inner cube."""
    n = 0
    seg = max(1, int(round(length / float(steps))))
    for k in range(steps):
        f = (k / float(max(1, steps - 1))) ** 1.6
        w = int(round(w0 + (w1 - w0) * f))
        o = (base[0] - w / 2.0, base[1] + k * seg, base[2] - w / 2.0)
        A.cube(bone, o, (w, seg, w), mat, rot=(0, 0, tilt) if tilt else None, pivot=pivot if tilt else None)
        n += 1
    w = max(2, int(w1) - 2)
    top = base[1] + steps * seg
    A.cube(bone, (base[0] - w / 2.0, top - 1, base[2] - w / 2.0), (w, 1, w), inner, rot=(0, 0, tilt) if tilt else None, pivot=pivot if tilt else None, inflate=0.05)
    return n + 1


# ===========================================================================================================================
def build_armour():
    A = Atlas(KEY, "armour", 256, 256, seed=11)
    c = A.cube

    # ---- HELM: circlet, clef crest, ear horns, plume
    A.bone("helm", pivot=(0, 28, 0))
    c("helm", (-4.5, 28, -4.5), (9, 3, 2), "brass", inflate=0.15)                      # circlet front
    c("helm", (-4.5, 28, 2.5), (9, 3, 2), "brass", inflate=0.15)                       # back
    c("helm", (-4.5, 28, -2.5), (2, 3, 5), "brass", inflate=0.15)                      # sides
    c("helm", (2.5, 28, -2.5), (2, 3, 5), "brass", inflate=0.15)
    c("helm", (-1, 31, -5), (2, 7, 1), "brass", inflate=0.1)                           # treble clef: stem
    c("helm", (-2, 36, -5), (3, 2, 1), "brass", inflate=0.1)                           #   top curl
    c("helm", (1, 33, -5), (2, 3, 1), "brass", inflate=0.1)                            #   upper bowl
    c("helm", (-3, 31, -5), (2, 3, 1), "brass", inflate=0.1)                           #   lower bowl
    c("helm", (-1.5, 29, -5.4), (3, 3, 1), "glowrose")                                 # a gem on the clef's foot
    for sg in (-1, 1):                                                                  # ear horns: throat, then a bell flaring sideways
        for k, w in enumerate((2, 3, 4, 5)):
            ox = 4.2 + k if sg > 0 else -5.2 - k
            c("helm", (ox, 27.5 - w / 2.0, -w / 2.0), (1, w, w), "brass", share="ear%d" % k)
        c("helm", (7.6 if sg > 0 else -8.6, 26.5, -1.5), (1, 3, 3), "bellinner", inflate=0.02)
    for k in range(2):                                                                  # plume: two flags sweeping back over the crown
        c("helm", (-1, 32 + k * 0.6, 1.5 + k * 1.6), (2, 1, 4), "velvet", rot=(-18 - k * 10, 0, 0), share="plume%d" % k)
        c("helm", (-1.5 + (k - 1) * 0.0, 32.6 + k * 0.6, 1.5 + k * 1.6), (3, 1, 1), "glowrose", rot=(-18 - k * 10, 0, 0), share="plumeg")

    # ---- CHEST: breastplate with a lyre of strings, collar, belt, back plate
    A.bone("chest", pivot=(0, 18, 0))
    c("chest", (-4.6, 14, -3.2), (9, 9, 2), "brass", inflate=0.2)                      # plate
    c("chest", (-4.8, 21, -3.4), (10, 2, 3), "brass_dark", inflate=0.15)  # gorget
    for k in range(5):                                                                  # the lyre's strings (glowing)
        c("chest", (-2 + k, 15, -3.7), (1, 6, 1), "string", share="chs")
    c("chest", (-3, 14, -3.7), (6, 1, 1), "brass", share="lyb")                         # lyre base
    c("chest", (-3, 15, -3.7), (1, 7, 1), "brass", share="lya")                         # lyre arms
    c("chest", (2, 15, -3.7), (1, 7, 1), "brass", share="lya")
    c("chest", (-3, 22, -3.7), (6, 1, 1), "brass", share="lyb")
    c("chest", (-1.5, 17.5, -4.2), (3, 3, 1), "glowrose")                              # a heart gem
    c("chest", (-3.5, 24, -2.5), (7, 2, 5), "brass", inflate=0.1)                       # stand-up collar
    c("chest", (-4.7, 12, -3), (9, 2, 6), "brass_dark", inflate=0.3)   # belt
    c("chest", (-1.5, 12, -3.6), (3, 2, 1), "brass", inflate=0.1)                       # buckle
    c("chest", (-4.6, 14, 1.3), (9, 9, 2), "brass_dark", inflate=0.2)                   # back plate
    A.mcube("chest", "chest", (-6, 22, -2.5), (2, 2, 5), "brass_dark", inflate=0.1)     # shoulder straps

    # ---- BACK: the staff cape
    A.bone("back", pivot=(0, 18, 3))
    c("back", (-4.5, 10, 2.4), (9, 13, 1), "staff", inflate=0.1)
    for k in range(5):
        c("back", (-4.4, 12.5 + k * 2.2, 3.5), (8.8, 1, 1), "string", share="staff%d" % k)
    c("back", (-2.5, 14, 3.6), (3, 2, 1), "glowrose")                                    # a quaver on the staff
    c("back", (1.5, 18, 3.6), (3, 2, 1), "glowrose")

    # ---- TAILS: conductor's tails
    A.bone("tails", pivot=(0, 12, 3))
    for sg in (-1, 1):
        x0 = 0.5 if sg > 0 else -3.5
        c("tails", (x0, 3, 2.2), (3, 9, 1), "velvet", rot=(10, 0, 0), pivot=(0, 12, 3), share="tail")
        c("tails", (x0, 2, 2.4), (3, 1, 1), "brass", rot=(10, 0, 0), pivot=(0, 12, 3), share="tailhem")

    # ---- LYRE halo behind the back (smaller, standing clear of the cape)
    A.bone("lyre", pivot=(0, 26, 7))
    for sg in (-1, 1):
        x0 = 5 if sg > 0 else -6
        c("lyre", (x0, 15, 6.5), (1, 4, 2), "brass", share="lyre0")
        c("lyre", (x0 + sg * 1, 18, 6.5), (1, 4, 2), "brass", share="lyre1")
        c("lyre", (x0 + sg * 1, 22, 6.5), (1, 4, 2), "brass", share="lyre2")
        c("lyre", (x0, 26, 6.5), (1, 4, 2), "brass", share="lyre3")
        c("lyre", (x0 - sg * 1, 30, 6.5), (1, 3, 2), "brass", share="lyre4")
        c("lyre", (x0 - sg * 1, 33, 6.5), (1, 1, 2), "glowrose", share="lyre5")
    c("lyre", (-5, 13, 6.5), (10, 2, 2), "brass")
    c("lyre", (-5, 33, 6.5), (10, 1, 2), "brass")
    for k in range(5):
        c("lyre", (-4 + k * 2, 15, 7.2), (1, 18, 1), "string", share="lyrs")

    # ---- three orbiting notes
    for tag, ang, y in (("a", 0.0, 22), ("b", 120.0, 17), ("c", 240.0, 27)):
        name = "note_" + tag
        A.bone(name, pivot=(0, y, 0))
        r = 11.0
        cx, cz = r * math.sin(math.radians(ang)), r * math.cos(math.radians(ang))
        c(name, (cx - 1.5, y - 1, cz - 1), (3, 2, 2), "glowrose", share="nh")
        c(name, (cx + 1, y, cz - 0.5), (1, 6, 1), "brass", share="ns")
        c(name, (cx + 2, y + 4.5, cz - 0.5), (2, 1, 1), "brass", share="nf")
        c(name, (cx + 3, y + 3.5, cz - 0.5), (1, 1, 1), "brass", share="nf2")

    # ---- PAULDRONS (trumpet bells), VAMBRACES (piano-key cuffs), GAUNTLETS (brass gloves)
    for sg, tag in ((-1, "r"), (1, "l")):
        sx = 6.0 * sg
        A.bone("pauldron_" + tag, pivot=(sx, 22, 0))
        c("pauldron_" + tag, (sx - 3.2 if sg < 0 else sx - 2.8, 22, -3), (6, 3, 6), "brass", inflate=0.2)
        bell(A, "pauldron_" + tag, (sx, 24.8, 0), -sg * 16, 5, 3, 6, 3, "brass", "bellinner", (sx, 24, 0))
        c("pauldron_" + tag, (sx - 1.5 - (0.5 if sg > 0 else 0), 19, -3.6), (3, 4, 1), "brass_dark", inflate=0.1)
        A.bone("vambrace_" + tag, pivot=(sx, 18, 0))
        ox = sx - 2.7
        c("vambrace_" + tag, (ox - 0.1, 14.2, -2.7), (5, 5, 5), "ivory", inflate=0.2)
        c("vambrace_" + tag, (ox - 0.4, 13.2, -3), (6, 1, 6), "brass", inflate=0.1)
        c("vambrace_" + tag, (ox - 0.4, 19, -3), (6, 1, 6), "brass", inflate=0.1)
        c("vambrace_" + tag, (sx + sg * 3.2 - 0.5, 15.5, -0.5), (1, 3, 1), "glowrose")           # tuning peg
        A.bone("gauntlet_" + tag, pivot=(sx, 13, 0))
        c("gauntlet_" + tag, (ox - 0.2, 11.6, -2.7), (5, 2, 5), "brass", inflate=0.25)
        c("gauntlet_" + tag, (ox + 0.3, 12.2, -3.3), (4, 1, 1), "brass_dark")
        for k in range(4):
            c("gauntlet_" + tag, (ox + k * 1.2 + 0.2, 12.4, -3.6), (1, 1, 1), "glowrose", share="pk")
    # ---- GREAVES
    for sg, tag in ((-1, "r"), (1, "l")):
        lx = -2.0 if sg < 0 else 2.0
        A.bone("greave_" + tag, pivot=(lx, 6, 0))
        c("greave_" + tag, (lx - 2.3, 3, -2.9), (4, 7, 1), "brass", inflate=0.3)
        c("greave_" + tag, (lx - 2.6, 9.4, -2.6), (5, 3, 5), "brass_dark", inflate=0.3)          # knee cop
        c("greave_" + tag, (lx - 1, 9.8, -3.6), (2, 2, 1), "glowrose")
        c("greave_" + tag, (lx - 2.8, 1.6, -2.8), (5, 2, 5), "ivory", inflate=0.4)                # ankle cuff with keys
        c("greave_" + tag, (lx - 2.6, 0.2, -3.6), (5, 1, 6), "brass_dark", inflate=0.1)
        c("greave_" + tag, (lx - 2.8, 5, 1.6), (5, 5, 1), "brass_dark", inflate=0.2)             # calf back plate

    # ---- animation: lyre sway, orbiting notes, tails swing
    A.anim.clip("idle", length=6.0, loop=True)
    A.anim.rot("idle", "lyre", sin_loop(6.0, 3.0, ax=2))
    A.anim.rot("idle", "tails", sin_loop(6.0, 6.0, ax=0))
    A.anim.rot("idle", "back", sin_loop(6.0, 1.5, ax=0))
    for tag, ph in (("a", 0.0), ("b", 0.0), ("c", 0.0)):
        A.anim.rot("idle", "note_" + tag, loop(6.0, lambda p: (0, 360.0 * p, 0)))
        A.anim.pos("idle", "note_" + tag, bob_loop(6.0, 1.2, phase={"a": 0.0, "b": 0.33, "c": 0.66}[tag]))
    return A.save()


# ===========================================================================================================================
def build_note():
    A = Atlas(KEY, "note", 128, 128, seed=21)
    c = A.cube
    A.bone("root", pivot=(0, 8, 0))
    A.bone("body", "root", (0, 8, 0))
    A.bone("flag", "body", (2, 20, 0))
    A.bone("rim", "root", (0, 8, 0))
    A.bone("grace_a", "root", (0, 8, 0))
    A.bone("grace_b", "root", (0, 8, 0))
    # the note head: a tilted oval of stacked cubes
    c("body", (-4, 4, -2), (8, 4, 4), "glowrose", inflate=0.1)
    c("body", (-5, 5, -2), (10, 2, 4), "brass", rot=(0, 0, 18), inflate=0.1)
    c("body", (-3, 3, -2), (6, 1, 4), "brass_dark")
    c("body", (-3, 8, -2), (6, 1, 4), "brass")
    c("body", (-2, 5, -2.5), (4, 2, 5), "glowrose")
    c("body", (2.5, 8, -1), (2, 14, 2), "brass")                                      # the stem
    c("body", (2.75, 9, -1.25), (2, 12, 2), "glowrose", inflate=0.12)
    c("flag", (4.5, 19, -1), (3, 2, 2), "brass")
    c("flag", (6.5, 16, -1), (3, 3, 2), "brass")
    c("flag", (6, 13, -1), (2, 3, 2), "glowrose")
    c("rim", (-7, 7, -0.5), (14, 1, 1), "string", rot=(0, 0, 18))                    # a glowing staff line under the note
    c("rim", (-9, 5.5, -0.5), (18, 1, 1), "string", rot=(0, 0, 18))
    c("grace_a", (-9, 14, 0), (2, 2, 2), "glowrose", share="gh")
    c("grace_a", (-7.5, 14, 0), (1, 5, 1), "brass", share="gs")
    c("grace_b", (8, 4, 0), (2, 2, 2), "glowrose", share="gh")
    c("grace_b", (9.5, 4, 0), (1, 5, 1), "brass", share="gs")
    A.anim.clip("idle", length=2.0, loop=True)
    A.anim.pos("idle", "root", bob_loop(2.0, 1.5))
    A.anim.rot("idle", "body", sin_loop(2.0, 8.0, ax=2))
    A.anim.rot("idle", "flag", sin_loop(2.0, 14.0, ax=2, phase=0.25))
    A.anim.rot("idle", "grace_a", loop(2.0, lambda p: (0, 0, 360 * p)))
    A.anim.rot("idle", "grace_b", loop(2.0, lambda p: (0, 0, -360 * p)))
    A.anim.clip("strike", length=0.5, loop=True)
    A.anim.rot("strike", "body", loop(0.5, lambda p: (0, 0, 360 * p)))
    A.anim.rot("strike", "grace_a", loop(0.5, lambda p: (0, 0, 720 * p)))
    A.anim.rot("strike", "grace_b", loop(0.5, lambda p: (0, 0, -720 * p)))
    A.anim.scale("strike", "rim", loop(0.5, lambda p: (1.0 + 0.4 * math.sin(p * 6.28), 1, 1)))
    return A.save()


# ===========================================================================================================================
def build_choir():
    A = Atlas(KEY, "choir", 128, 128, seed=31)
    c = A.cube
    A.bone("root", pivot=(0, 0, 0))
    A.bone("body", "root", (0, 12, 0))
    A.bone("head", "body", (0, 26, 0))
    A.bone("jaw", "head", (0, 27, -2))
    A.bone("arm_r", "body", (-5, 24, 0))
    A.bone("arm_l", "body", (5, 24, 0))
    A.bone("book", "body", (0, 17, -6))
    A.bone("halo", "head", (0, 36, 0))
    A.bone("tail", "root", (0, 10, 0))
    # the robe: a cone of stacked cubes, wide at the hem
    for k, (w, d) in enumerate(((13, 11), (12, 10), (11, 9), (10, 8), (9, 7), (8, 6))):
        c("body", (-w / 2.0, 4 + k * 3.6, -d / 2.0), (w, 4, d), "spiritrobe", inflate=0.05)
    c("body", (-6.6, 4, -5.6), (13, 1, 1), "brass")                                    # gold hem band
    c("body", (-1, 14, -4.6), (2, 8, 1), "glowrose")                                   # a stole down the front
    c("body", (-4.5, 21, -3.2), (9, 3, 6), "brass", inflate=0.1)   # shoulder cape
    # head, hood and an open mouth
    c("head", (-3.5, 26, -3.5), (7, 7, 7), "spirit")
    c("head", (-4.3, 25.4, -4.3), (9, 9, 9), "spiritrobe", inflate=-0.1)    # hood
    c("head", (-1, 29, -4.4), (2, 1, 1), "glowrose")                                   # eyes (closed, singing)
    c("head", (-3, 29, -4.4), (2, 1, 1), "glowrose")
    c("head", (1, 29, -4.4), (2, 1, 1), "glowrose")
    c("jaw", (-1.5, 26.5, -4.2), (3, 2, 1), "bellinner")
    c("halo", (-5, 36, -5), (10, 1, 1), "glowrose", share="hl")
    c("halo", (-5, 36, 4), (10, 1, 1), "glowrose", share="hl")
    c("halo", (-5, 36, -4), (1, 1, 8), "glowrose", share="hv")
    c("halo", (4, 36, -4), (1, 1, 8), "glowrose", share="hv")
    # arms holding the songbook
    for sg, nm in ((-1, "arm_r"), (1, "arm_l")):
        x = 5.0 * sg
        c(nm, (x - 1.5 if sg > 0 else x - 1.5, 18, -3), (3, 7, 3), "spiritrobe", rot=(-50, 0, 0), pivot=(x, 24, 0))
        c(nm, (x - 1, 15, -6.5), (2, 2, 2), "spirit", rot=(-50, 0, 0), pivot=(x, 24, 0))
    c("book", (-4, 14, -8), (8, 6, 1), "brass", rot=(-25, 0, 0))
    c("book", (-3.4, 14.6, -8.6), (6.8, 4.8, 1), "staff", rot=(-25, 0, 0))
    for k in range(3):
        c("book", (-3, 15.2 + k * 1.3, -9.1), (6, 1, 1), "string", rot=(-25, 0, 0), share="bk")
    # the wisp tail under the hem
    for k, (w, h) in enumerate(((7, 4), (5, 4), (3, 3))):
        c("tail", (-w / 2.0, 3 - k * 3.2, -w / 2.0), (w, h, w), "spirit", rot=(0, 0, 6 * (k % 2 * 2 - 1)))
    A.anim.clip("idle", length=3.0, loop=True)
    A.anim.pos("idle", "root", bob_loop(3.0, 1.2))
    A.anim.rot("idle", "head", sin_loop(3.0, 4.0, ax=1))
    A.anim.rot("idle", "jaw", loop(3.0, lambda p: (max(0.0, 14.0 * math.sin(p * 2 * 2 * math.pi)), 0, 0)))
    A.anim.rot("idle", "tail", sin_loop(3.0, 8.0, ax=2))
    A.anim.rot("idle", "halo", loop(3.0, lambda p: (0, 360.0 * p, 0)))
    A.anim.clip("sing", length=1.2, loop=True)
    A.anim.pos("sing", "root", bob_loop(1.2, 2.0))
    A.anim.rot("sing", "head", loop(1.2, lambda p: (-14.0 - 4.0 * math.sin(p * 2 * math.pi), 0, 0)))
    A.anim.rot("sing", "jaw", loop(1.2, lambda p: (24.0 + 10.0 * math.sin(p * 2 * 2 * math.pi), 0, 0)))
    A.anim.rot("sing", "arm_r", sin_loop(1.2, -8.0, ax=0))
    A.anim.rot("sing", "arm_l", sin_loop(1.2, -8.0, ax=0))
    A.anim.rot("sing", "halo", loop(1.2, lambda p: (0, 720.0 * p, 0)))
    A.anim.rot("sing", "tail", sin_loop(1.2, 12.0, ax=2))
    return A.save()


# ===========================================================================================================================
def build_metronome():
    A = Atlas(KEY, "metronome", 128, 128, seed=41)
    c = A.cube
    A.bone("root", pivot=(0, 0, 0))
    A.bone("pendulum", "root", (0, 5, 0))
    A.bone("weight", "pendulum", (0, 16, 0))
    for k, (w, d) in enumerate(((14, 10), (12, 9), (10, 8), (8, 7), (6, 6))):
        c("root", (-w / 2.0, k * 5, -d / 2.0), (w, 5, d), "woodbrass", inflate=0.05)
    c("root", (-8, 0, -6), (16, 2, 12), "brass_dark")
    c("root", (-3, 24, -3), (6, 2, 6), "brass")
    c("root", (-1.5, 26, -1.5), (3, 3, 3), "glowrose")
    for k in range(5):                                                                   # tempo marks on the front
        c("root", (-1, 4 + k * 4, -5.2), (2, 1, 1), "string", share="tm")
    c("pendulum", (-0.5, 5, -1), (1, 25, 1), "brass")
    c("weight", (-2, 14, -2), (4, 3, 4), "brass")
    c("weight", (-1.5, 14.5, -2.5), (3, 2, 1), "glowrose")
    A.anim.clip("idle", length=1.0, loop=True)
    A.anim.rot("idle", "pendulum", loop(1.0, lambda p: (0, 0, 28.0 * math.sin(p * 2 * math.pi)), steps=8))
    A.anim.scale("idle", "root", loop(1.0, lambda p: (1.0, 1.0 + 0.015 * abs(math.sin(p * 2 * math.pi)), 1.0)))
    return A.save()


if __name__ == "__main__":
    ids = [build_armour(), build_note(), build_choir(), build_metronome()]
    check(*ids)
