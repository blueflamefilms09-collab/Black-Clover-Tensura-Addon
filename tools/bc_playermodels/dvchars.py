"""The 26 devils of the Devil Contract roster (WorldDevil.java) as player-model looks: per devil an outfit and face spec (dvskin / bcskin) and a build function (dvlib / bcparts*).  DEVILS is read by gen_dv.py.

Sources (rule 10), English wikis first, fetched with curl through the fandom MediaWiki API (action=parse, the page's wikitext; the pages themselves are blocked, the API is not), the Appearance /
Description section of each page, treated as untrusted data:
  Black Clover wiki    Liebe, Lucifero, Beelzebub, Megicula, Lucifugus (Astaroth has no Appearance section: only 'highest-ranking devil, Time Magic')
  Chainsaw Man wiki    Fox Devil, Curse Devil, Future Devil, Ghost Devil
  Kuroshitsuji wiki    Sebastian Michaelis
  Blue Exorcist wiki   Iblis, Egyn, Amaimon, Astaroth, Lucifer (the Demon Kings' host looks)
  Tensura wiki         Noir, Blanc, Jaune, Violet, Rouge, Vert, Bleu (the seven Primordials' looks; the roster's devils are 'daemons of their line' and wear the line's colours and look)
  Berserk wiki         God Hand, Femto; Fairy Tail wiki: Etherious, Mard Geer
Not fetched: the Dororo wiki (no Hall of Hell page under the guessed names) and every Japanese wiki (the English ones answered, so the fallback was not needed): the Dororo demon is from memory of the
series (an oni-like demon of the Hall of Hell). Astaroth (Black Clover) has no canon look in the wiki, so its design is this mod's (a golden, time-themed devil). Kin devils are this mod's too.
"""
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
sys.dont_write_bytecode = True

import bclib as L  # noqa: E402,F401
import bcparts as P  # noqa: E402
import bcparts2 as Q  # noqa: E402
import bcparts3 as R3  # noqa: E402
import bcskin as S  # noqa: E402
import dvlib as D  # noqa: E402

PALE = ("#d4bcc4", "#ead4d4", "#fbeeea")
FAIR = ("#d8a88c", "#f0caae", "#ffe4d0")
ASH = ("#b4a8b6", "#d0c4d0", "#ece2ea")
GREY = ("#8a808c", "#a89ea8", "#c8c0c8")
BLACK = ("#141218", "#26222c", "#403a48")
DARK = ("#5a3a2c", "#7a5240", "#9a6e58")


def eyes(d, m, l, glow=None, slit=False, **kw):
    e = dict(iris=(d, m, l), cy=4.95, h=1.95, tilt=0.14, lash_w=0.42)
    if glow:
        e["glow"] = glow
    if slit:
        e["slit"] = True
        e["pupil"] = "#10060a"
    e.update(kw)
    return e


def face(skin, hair, e, brow=None, mouth=None, marks=(), blush=0.0, hairline=1.8):
    f = dict(skin=skin, hair=hair, hairline=hairline, eyes=e, brow=brow or dict(angle=0.6, thick=0.26), mouth=mouth or dict(kind="flat", color="#7a3a3a", lip="#a86060"), marks=list(marks))
    if blush:
        f["blush"] = blush
    return f


def line(*pts, color="#e8e8f0", w=0.16, a=0.9, glow=None):
    return lambda f: f.polyline(list(pts), color, w, a=a, glow=glow)


def weg(color, w=0.13):
    """Glowing lines down both arms and across the chest (the devil's markings in its colour), as an outfit mark."""
    def fn(cv):
        for part in ("arm_r", "arm_l"):
            for nm in ("front", "back"):
                f = cv.face(part, nm, False)
                f.polyline([(f.w * 0.5, 0.3), (f.w * 0.25, 3.5), (f.w * 0.7, 6.6), (f.w * 0.4, 10.5)], color, w, a=0.9, glow=color)
        f = cv.face("body", "front", False)
        f.polyline([(1.2, 0.4), (2.4, 3.0), (4.0, 4.0), (5.6, 3.0), (6.8, 0.4)], color, w, a=0.9, glow=color)
    return fn


def wings(b, c, v, e, span=24.0, y=21.5, **kw):
    R3.wing(b, c, v, e, span=span, y=y, **kw)


# ================================================================================================ Black Clover
def liebe(b):
    D.hair(b, "messy", "#f4f2f8", seed=1, spikes=5, length=4.4, lean=10.0, fan=34.0)
    D.horn_set(b, "#1a1a20", "#a8a4b4", "bull")
    wings(b, "#17151c", "#4a4658", "#a8a4b8", span=17.0, fingers=3)
    P.tail(b, "#17151c", "#e8e4f0", segs=7, length=17.0, spade="trident", y=13.4, z=2.2, w0=1.4, w1=0.5, tip_size=4.5)
    P.bracers(b, False, y_top=19.0, rings=1, gold="#f2f0f8", tassels=False, sides=("l",))


def lucifero(b):
    D.hair(b, "long", "#ece8f6", seed=51, tail=14.0, front=4.0, part=0.3, side=12.0, tiers=3)
    D.horn_set(b, "#17101f", "#8a6ab0", "four")
    P.elf_ears(b, "#c8bcc8", length=3.2)
    wings(b, "#1a1228", "#5a1e8a", "#9a7ad0", span=30.0, y=22.0)
    wings(b, "#16102a", "#4a2a7a", "#8a6ac0", span=22.0, y=18.0, pair=2, z=2.6)
    P.skirt(b, "#1c1426", "#8a5ab0", y_top=13.6, length=13.0, layers_=2, seed=58, radius=4.9, panels=8)
    P.collar(b, "#1c1426", "#8a5ab0")


def beelzebub(b):
    D.hair(b, "wavy", "#2a2e4a", seed=61, tail=3.0, front=3.0, side=7.0)
    D.horn_set(b, "#101014", "#6a78e0", "small")
    P.horns(b, "#101014", "#6a78e0", base=(3.4, 31.4, -0.6), segs=4, length=2.4, w0=2.4, w1=0.8, curl=8.0, out=62.0, seed=62)
    P.elf_ears(b, "#2a2832", length=3.8)
    wings(b, "#101018", "#2a3a8a", "#5a6ae0", span=26.0, y=22.0)
    P.mantle(b, "#3a3c50", "#1c1e2e", y=22.0, thick=2.8, width=17.0, depth=7.0, drop=3.0, seed=63)
    Q.pauldrons(b, "#1c1e2e", "#0e0e18", spikes=2, spike_len=3.6)
    P.bracers(b, False, y_top=18.0, rings=2, gold="#2a3a8a", gem="#6a78ff", tassels=False)


def astaroth(b):
    D.hair(b, "long", "#f4ecd0", seed=71, tail=11.0, front=3.8, part=-0.4, side=11.0, tiers=2)
    D.horn_set(b, "#c8a030", "#fff4c0", "ram")
    wings(b, "#2a2418", "#b89430", "#f5d76e", span=26.0, y=22.0)
    P.halo(b, y=37.2, r=7.0, color="#f5d76e", tilt=-4.0, spin=-14.0, rings=2)
    P.cape(b, "#2a2418", "#6a5a20", length=20.0, y_top=23.4, width=9.0, tiers=3, trim="#f5d76e", seed=72, hem="zigzag")
    P.mantle(b, "#f4ecd0", "#c8b878", y=22.0, thick=2.2, width=16.0, depth=6.6, drop=2.4, seed=73)
    P.bracers(b, False, y_top=18.5, rings=3, gold="#f5d76e", gem="#fff4c0", tassel="#f5d76e")
    P.tail(b, "#2a2418", "#f5d76e", segs=7, length=22.0, spade="arrow", y=13.4, z=2.2, w0=1.8, w1=0.6, tip_size=5.0)


def megicula(b):
    D.hair(b, "wavy", "#e8e0ee", seed=81, tail=12.0, front=4.0, side=12.0)
    P.horns(b, "#1a1218", "#d8c8d8", base=(3.4, 31.4, -0.6), segs=5, length=2.2, w0=2.4, w1=0.8, curl=30.0, out=72.0, seed=82)
    P.horns(b, "#1a1218", "#d8c8d8", base=(1.4, 32.4, -3.2), segs=3, length=2.6, w0=1.5, w1=0.5, curl=-4.0, out=14.0, seed=83)
    P.elf_ears(b, "#e0d4dc", length=3.6)
    R3.wing(b, "#1c1020", "#8a2a5a", "#d85a9a", span=22.0, y=21.5, eyes=True)
    P.tail(b, "#1c1020", "#d82a6a", segs=8, length=22.0, spade="heart", y=13.4, z=2.2, w0=1.6, w1=0.5, tip_size=5.5)
    P.skirt(b, "#e8dce8", "#8a2a5a", y_top=13.4, length=9.0, layers_=2, seed=84, radius=4.5, panels=8, sway=4.0)
    P.collar(b, "#1c1020", "#e8dce8", high=2.4)


def lucifugus(b):
    D.hair(b, "long", "#dcd0f4", seed=91, tail=12.0, front=3.6, part=0.5, side=10.0)
    D.horn_set(b, "#2a1a3a", "#d8c0ff", "ox")
    P.elf_ears(b, "#d4c8e4", length=4.4)
    wings(b, "#1c1230", "#6a3aba", "#b06aff", span=26.0, y=22.0)
    P.tail(b, "#241a3a", "#b06aff", segs=9, length=26.0, spade="arrow", y=13.4, z=2.2, w0=1.8, w1=0.6, tip_size=5.0, curl=-22.0)
    P.halo(b, y=36.4, r=6.4, color="#b06aff", tilt=-6.0, spin=16.0, rings=2)
    P.cape(b, "#241a3a", "#12081e", length=22.0, y_top=23.4, width=9.0, tiers=3, trim="#b06aff", seed=92, hem="zigzag")
    P.bracers(b, False, y_top=19.0, rings=2, gold="#3a2a58", gem="#b06aff", tassel="#b06aff")
    P.collar(b, "#241a3a", "#b06aff")


# ================================================================================================ Chainsaw Man
def fox(b):
    D.hair(b, "messy", "#e08a2a", seed=101, spikes=5, length=4.6, lean=10.0, fan=30.0, tip="#fff0d8")
    D.ears_up(b, "#e08a2a", "#fff0d8", x=2.5, y=32.2, length=7.0)
    P.mantle(b, "#f6f0e8", "#d8cdb8", y=21.9, thick=2.6, width=15.0, depth=6.6, drop=2.8, seed=102)
    P.tail(b, "#e08a2a", "#fff6e8", segs=6, length=22.0, spade="spade", y=13.4, z=2.2, w0=3.8, w1=2.2, tip_size=7.0, curl=-14.0, curl_step=-4.0)
    P.tail(b, "#d07a20", "#fff6e8", segs=5, length=15.0, spade=None, y=13.6, z=2.0, w0=3.2, w1=1.6, curl=-30.0, curl_step=-8.0, seed=17)
    P.bracers(b, False, y_top=19.0, rings=2, gold="#f6f0e8", gem="#ff4a30", tassels=False)
    Q.pauldrons(b, "#f6f0e8", "#d8cdb8", spikes=2, spike_len=2.8)


def curse(b):
    D.hair(b, "bald", "#000000")
    D.horn_set(b, "#d8d0c0", "#fff8e8", "curved")
    Q.ribcage(b, "#d8d0c0", "#6a6058", rows=4)
    Q.pauldrons(b, "#d8d0c0", "#6a6058", spikes=3, spike_len=3.6)
    D.skull(b, "#e6e0d0", "#1a1018", pos=(7.2, 24.4, 0.4), size=4.8)
    P.cape(b, "#1c1224", "#0a0612", length=22.0, y_top=23.4, width=9.0, tiers=3, trim=None, seed=111, hem="ragged")
    P.collar(b, "#1c1224", "#8a4aba", high=2.6)
    P.bracers(b, False, y_top=19.0, rings=2, gold="#d8d0c0", gem="#c070ff", tassels=False)


def future(b):
    D.hair(b, "bald", "#000000")
    P.horns(b, "#3a2a1c", "#7ae0c8", base=(2.8, 31.6, -1.0), segs=4, length=2.4, w0=2.0, w1=0.7, curl=-12.0, out=24.0, seed=121)
    P.horns(b, "#3a2a1c", "#7ae0c8", base=(3.6, 31.0, -0.6), segs=4, length=2.2, w0=1.8, w1=0.6, curl=10.0, out=58.0, seed=122)
    P.horns(b, "#3a2a1c", "#7ae0c8", base=(1.6, 32.2, -0.4), segs=3, length=2.0, w0=1.5, w1=0.5, curl=-6.0, out=14.0, seed=123)
    D.chest_eye(b, "#3ad8d8", y=20.5, size=3.0)
    D.vines(b, "#2a8a5a", "#a8f0c0", n=3, length=12.0)
    P.skirt(b, "#4a3220", "#2a8a5a", y_top=13.4, length=10.0, layers_=2, seed=124, radius=4.7, panels=8, sway=2.0)
    Q.back_spikes(b, "#4a3220", n=5, y=22.0, length=9.0, tip="#7ae0c8")
    P.collar(b, "#2a1c10", "#3ad8d8", high=2.4)


def ghost(b):
    D.hair(b, "wavy", "#1a1620", seed=131, tail=13.0, front=4.2, side=12.0)
    P.mantle(b, "#f4b8d0", "#8aa8e8", y=21.8, thick=2.8, width=16.6, depth=6.9, drop=3.2, seed=132)
    P.skirt(b, "#f4b8d0", "#8aa8e8", y_top=13.4, length=10.0, layers_=2, seed=133, radius=4.9, panels=8, sway=2.4)
    Q.back_spikes(b, "#9aa8c8", n=6, y=21.0, length=10.0, spread=3.6, tip="#e8f0ff", lean=34.0)
    P.bracers(b, False, y_top=19.0, rings=2, gold="#b8c0d8", gem="#8aa8e8", tassel="#f4b8d0")


# ================================================================================================ Black Butler, Blue Exorcist
def sebastian(b):
    D.hair(b, "swept", "#14121a", seed=141, front=4.4, part=0.8, side=9.0)
    R3.wing(b, "#0e0c14", "#7a3cc8", "#d82a9a", span=26.0, y=22.0, eyes=True)
    P.cape(b, "#101014", "#1a1a22", length=14.0, y_top=14.0, width=7.0, tiers=2, trim=None, seed=142, hem="zigzag", z=2.3)
    P.collar(b, "#f2f0f4", "#c8c8d0", high=1.8)
    P.bracers(b, False, y_top=18.4, rings=1, gold="#c8c8d0", gem="#d82a9a", tassels=False, sides=("r",))


def iblis(b):
    D.flame_hair(b, "#ff6a2a", "#ffd24a", seed=151, n=7, length=7.0)
    D.horn_set(b, "#3a1a10", "#ffb050", "small")
    P.bracers(b, True, y_top=19.4, rings=3, gold="#e8b830", gem="#ff6a2a", tassel="#ff8a3a")
    P.circlet(b, "#e8b830", "#ff4a2a", y=31.2)
    P.skirt(b, "#ff7a3a", "#e8b830", y_top=13.4, length=11.0, layers_=2, seed=152, radius=4.6, panels=8, sway=3.4)
    P.tail(b, "#7a2a10", "#ff8a3a", segs=7, length=20.0, spade="arrow", y=13.4, z=2.2, w0=1.8, w1=0.6, tip_size=5.5)


def egyn(b):
    D.hair(b, "bob", "#7ab8f0", seed=161, front=3.8, streak="#2a5ab8")
    P.horns(b, "#2a6ab8", "#a8e0ff", base=(0.0, 32.3, -0.6), segs=4, length=2.2, w0=2.2, w1=0.8, curl=-2.0, out=0.0, mirrored=False, seed=162)
    P.cape(b, "#2a6ab8", "#7ab8f0", length=14.0, y_top=23.4, width=8.0, tiers=2, trim="#a8e0ff", seed=163, hem="zigzag")
    P.skirt(b, "#7ab8f0", "#a8e0ff", y_top=13.2, length=9.0, layers_=2, seed=164, radius=4.5, panels=8, sway=3.6)
    P.tail(b, "#2a6ab8", "#a8e0ff", segs=6, length=14.0, spade="spade", y=13.4, z=2.2, w0=1.8, w1=0.6, tip_size=5.5)
    P.collar(b, "#2a6ab8", "#a8e0ff", high=2.0)


def amaimon(b):
    D.hair(b, "spiky", "#3aa85a", seed=171, spikes=5, length=4.6, lean=12.0, fan=30.0, tip="#1a5a2a")
    P.horns(b, "#3a5a2a", "#c8e0a0", base=(0.0, 32.4, -0.6), segs=3, length=2.4, w0=1.8, w1=0.5, curl=0.0, out=0.0, mirrored=False, seed=172)
    Q.pauldrons(b, "#8a6a3a", "#4a3a20", spikes=3, spike_len=3.2)
    P.leg_bands(b, False, gold="#8a6a3a", y=3.0, rings=2)
    P.tail(b, "#2a6a3a", "#c8e0a0", segs=7, length=19.0, spade="spade", y=13.4, z=2.2, w0=1.8, w1=0.6, tip_size=5.0)
    P.bracers(b, False, y_top=19.0, rings=2, gold="#8a6a3a", gem="#c8e0a0", tassels=False)


def kin_astaroth(b):
    D.hair(b, "messy", "#5a6a2a", seed=181, spikes=4, length=3.6, lean=14.0, fan=26.0, tip="#c8d8a0")
    D.horn_set(b, "#3a3a20", "#d8d8b0", "ram")
    D.mushroom(b, "body", x=-6.0, y=23.6, z=0.2, r=2.6, cap="#6a2a7a", eyes=True)
    D.mushroom(b, "body", x=5.4, y=24.0, z=-0.4, r=2.0, cap="#5a7a2a")
    D.mushroom(b, "body", x=0.6, y=22.0, z=2.9, r=2.2, cap="#7a2a3a", eyes=True)
    P.cape(b, "#3a4a1a", "#1a2208", length=18.0, y_top=23.4, width=8.6, tiers=3, trim=None, seed=182, hem="ragged")
    P.tail(b, "#3a4a1a", "#c8d8a0", segs=7, length=18.0, spade="arrow", y=13.4, z=2.2, w0=1.8, w1=0.6, tip_size=5.0)


def kin_lucifer(b):
    D.hair(b, "spiky", "#f4dc7a", seed=191, spikes=6, length=5.2, lean=22.0, fan=24.0, tip="#fff8d0")
    P.elf_ears(b, "#f0d4b8", length=3.2)
    R3.crown(b, "#f5d76e", "#fff0a8", spikes=5, y=32.45, tall=3.0, width=9.0, point_w=1.6)
    P.halo(b, y=37.0, r=7.0, color="#fff0a8", tilt=-5.0, spin=12.0, rings=2)
    R3.wing(b, "#f6f0d0", "#d8b84a", "#fff4b0", span=24.0, y=22.0)
    P.cape(b, "#f6f0e0", "#d8c070", length=22.0, y_top=23.4, width=9.0, tiers=3, trim="#f5d76e", seed=192, hem="zigzag")
    P.tail(b, "#f4dc7a", "#fff8d0", segs=7, length=22.0, spade="arrow", y=13.4, z=2.2, w0=1.8, w1=0.6, tip_size=5.0)
    P.bracers(b, False, y_top=19.0, rings=2, gold="#f5d76e", gem="#fff0a8", tassel="#fff0a8")


# ================================================================================================ Tensura daemons
def daemon_wings(b, c, v, e, span=24.0, pairs=1):
    for i in range(pairs):
        R3.wing(b, c, v, e, span=span * (1.0 - 0.18 * i), y=22.0 - 3.5 * i, pair=i + 1, z=2.8 + 0.4 * i, layers_=2)


def noir(b):
    D.hair(b, "swept", "#14101a", seed=201, front=4.4, part=0.6, side=9.0, streak="#c81e2d")
    P.fringe(b, "#e8b830", n=2, span=2.4, y_top=31.7, length=4.8, seed=202, layer="hair_fine", rot=6.0, part=2.4)
    D.horn_set(b, "#14101a", "#e8b830", "curved")
    daemon_wings(b, "#14101a", "#8a1020", "#e8b830", span=26.0)
    P.cape(b, "#14101a", "#8a1020", length=20.0, y_top=23.4, width=8.8, tiers=3, trim="#e8b830", seed=203, hem="zigzag")
    P.mantle(b, "#2a2430", "#14101a", y=22.2, thick=2.0, width=15.4, depth=6.4, drop=2.0, seed=204)
    P.bracers(b, False, y_top=19.0, rings=2, gold="#e8b830", gem="#c81e2d", tassels=False)


def blanc(b):
    D.hair(b, "long", "#f4f4fc", seed=211, tail=15.0, front=4.0, part=0.2, side=12.0)
    D.horn_set(b, "#e8e8f4", "#ffffff", "curved")
    daemon_wings(b, "#d8d8e8", "#8a8aa8", "#ffffff", span=24.0)
    P.mantle(b, "#2a2a34", "#14141a", y=22.2, thick=1.8, width=15.2, depth=6.4, drop=1.8, seed=212)
    P.bracers(b, True, y_top=19.4, rings=1, gold="#c8c8d8", tassels=False)
    Q.belts_x(b, "#14141a", "#c8c8d8")


def jaune(b):
    D.hair(b, "bob", "#f0d030", seed=221, front=3.8)
    D.horn_set(b, "#2a2410", "#f0d030", "small")
    daemon_wings(b, "#2a2410", "#a88a10", "#f0d030", span=20.0)
    P.mantle(b, "#14161c", "#08090c", y=21.9, thick=2.6, width=16.4, depth=6.8, drop=2.6, seed=222)
    P.circlet(b, "#c8c8d0", "#f0d030", y=29.6, width=9.0, gem_size=1.0)
    P.bracers(b, True, y_top=19.4, rings=1, gold="#c8c8d0", tassels=False)


def violet(b):
    D.hair(b, "bob", "#9a4ae0", seed=231, front=3.8)
    D.ponytail_side(b, "#9a4ae0", seed=232)
    D.horn_set(b, "#2a1a3a", "#c890ff", "curved")
    for i in range(3):
        R3.wing(b, "#1c1230", "#6a2aba", "#c890ff", span=26.0 - 4.0 * i, y=22.0 - 2.6 * i, pair=i + 1, z=2.8 + 0.35 * i, layers_=2, sweep=34.0 + 14.0 * i, rise=14.0 - 4.0 * i)
    P.skirt(b, "#1c1824", "#c890ff", y_top=13.4, length=8.0, layers_=2, seed=233, radius=4.6, panels=8)
    P.bracers(b, True, y_top=21.0, rings=1, gold="#c890ff", tassels=False, sides=("l",))
    P.collar(b, "#1c1824", "#c890ff", high=2.0)


def rouge(b):
    D.hair(b, "wavy", "#c81e2d", seed=241, tail=16.0, front=4.0, side=13.0)
    D.horn_set(b, "#14080a", "#ff4a58", "four")
    daemon_wings(b, "#14080a", "#8a0a14", "#ff4a58", span=28.0, pairs=2)
    P.mantle(b, "#1a1214", "#0a0608", y=22.0, thick=1.6, width=16.6, depth=6.8, drop=1.6, seed=242)
    P.bracers(b, False, y_top=19.4, rings=2, gold="#e8e0d0", gem="#e8e0d0", tassels=False)
    Q.belts_x(b, "#8a0a14", "#e8e0d0")
    P.tail(b, "#14080a", "#c81e2d", segs=8, length=24.0, spade="arrow", y=13.4, z=2.2, w0=1.9, w1=0.6, tip_size=6.0)


def vert(b):
    D.hair(b, "wavy", "#3aa85a", seed=251, tail=11.0, front=4.0, side=11.0)
    D.horn_set(b, "#1a2a1c", "#a8f0c0", "small")
    daemon_wings(b, "#14281c", "#2a7a42", "#a8f0c0", span=20.0)
    P.skirt(b, "#8a1020", "#f2f0f4", y_top=13.4, length=8.0, layers_=2, seed=252, radius=4.6, panels=8)
    P.circlet(b, "#f2f0f4", "#3aa85a", y=31.0, width=9.0, gem_size=1.0)
    P.bracers(b, True, y_top=19.4, rings=2, gold="#3aa85a", tassels=False)


def bleu(b):
    D.hair(b, "bob", "#3a6ae8", seed=261, front=4.0)
    D.horn_set(b, "#101a3a", "#a8c8ff", "small")
    daemon_wings(b, "#101a3a", "#2a4ab0", "#a8c8ff", span=20.0)
    P.skirt(b, "#8a1020", "#f2f0f4", y_top=13.4, length=8.0, layers_=2, seed=262, radius=4.6, panels=8)
    P.circlet(b, "#f2f0f4", "#3a6ae8", y=31.0, width=9.0, gem_size=1.0)
    P.bracers(b, True, y_top=19.4, rings=2, gold="#3a6ae8", tassels=False)


# ================================================================================================ Dororo, Berserk, Fairy Tail
def hall(b):
    D.hair(b, "messy", "#3a2a22", seed=271, spikes=6, length=5.4, lean=8.0, fan=34.0)
    D.horn_set(b, "#d8c8a8", "#fff4d8", "bull")
    P.cape(b, "#4a3228", "#1c1410", length=16.0, y_top=23.4, width=8.8, tiers=3, trim=None, seed=272, hem="ragged")
    P.mantle(b, "#5a4636", "#2a2018", y=22.0, thick=2.4, width=16.2, depth=6.7, drop=2.4, seed=273)
    Q.pauldrons(b, "#d8c8a8", "#6a5a40", spikes=2, spike_len=3.2)
    Q.belts_x(b, "#3a2a22", "#d8c8a8")
    P.leg_bands(b, False, gold="#d8c8a8", y=3.2, rings=2)
    D.skull(b, "#e6e0d0", "#1a1018", pos=(-1.0, 12.4, -2.9), size=3.2, lean=0.0)


def god_hand(b):
    D.hair(b, "swept", "#f4f0f6", seed=281, front=4.2, part=0.5, side=9.0)
    D.horn_set(b, "#2a0a10", "#c8a050", "curved")
    R3.crown(b, "#c8a050", "#c81e2d", spikes=5, y=32.45, tall=3.4, width=9.0, point_w=1.7)
    daemon_wings(b, "#14080c", "#8a0a14", "#e8d8c0", span=30.0, pairs=2)
    P.cape(b, "#14080c", "#8a0a14", length=22.0, y_top=23.4, width=9.0, tiers=3, trim="#c8a050", seed=282, hem="zigzag")
    Q.pauldrons(b, "#2a0a10", "#0e0408", spikes=3, spike_len=4.2)
    P.collar(b, "#14080c", "#c8a050")
    P.bracers(b, False, y_top=19.0, rings=2, gold="#c8a050", gem="#c81e2d", tassels=False)


def etherious(b):
    D.hair(b, "wavy", "#2a1a4a", seed=291, tail=9.0, front=4.4, side=10.0)
    D.ponytail_side(b, "#2a1a4a", seed=292, x=2.6, y=31.4, length=15.0)
    D.horn_set(b, "#1a1030", "#b090ff", "curved")
    R3.wing(b, "#1a1030", "#6a3cff", "#b090ff", span=24.0, y=22.0)
    P.tail(b, "#1a1030", "#6a3cff", segs=8, length=24.0, spade="arrow", y=13.4, z=2.2, w0=1.8, w1=0.6, tip_size=5.5)
    P.cape(b, "#1a1030", "#0a0618", length=18.0, y_top=23.4, width=8.8, tiers=3, trim="#6a3cff", seed=293, hem="ragged")
    P.collar(b, "#1a1030", "#6a3cff")
    P.bracers(b, False, y_top=19.0, rings=2, gold="#3a2a6a", gem="#b090ff", tassels=False)


