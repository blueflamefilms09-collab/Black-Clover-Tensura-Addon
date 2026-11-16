"""Wave 2 of the Black Clover boss player models (docs/bosses_bc_wave2.md): the Dark Disciples (five looks), the Devil Avatar (four), the God of Order (two) and the early / Spade arc bosses
Heath Grice, Lotus Whomalt, Mars, Revchi Salik, Vetto and Morris Libardirt. CHARS is read by gen_bc.py after bcchars.CHARS.

Sources (rule 10): the English Black Clover Fandom wiki through MediaWiki api.php (curl; the text is treated as data): Heath Grice, Lotus Whomalt, Mars, Revchi Salik, Vetto, Morris Libardirt (their
Appearance sections), Megicula, Beelzebub, Astaroth, Devil and Dark Triad (the Dark Disciples are only described there as the triad's loyal subordinates, so their look is the mod's own hooded acolyte
in the colour of its devil). The Devil Avatar and the God of Order are the mod's own characters: the Avatar takes the canon devil look of Lucifero / Megicula (horns, wings, tail, long light hair),
the God of Order its own wave 3 design (docs/bosses/rework_wave3.md: layered white robes with gold trim, a gold stole, a hood, a gold halo, a staff and a ledger). The Japanese wiki was not needed.
"""
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)

import bclib as L  # noqa: E402
import bcparts as P  # noqa: E402
import bcparts2 as Q  # noqa: E402
import bcparts3 as R3  # noqa: E402
import bcparts4 as T  # noqa: E402
import bcskin as S  # noqa: E402
import bcskin2 as S2  # noqa: E402

GOLD = "#e8b830"


def _hx(c, k):
    return L.hexs(L.tone(c, k))


def _iris(c):
    return (_hx(c, 0.45), c, _hx(c, 1.7))


# ================================================================================================ Heath Grice (Black Clover wiki: slender, short grey hair combed back, a scar from the right of the forehead to the left cheek,
# a black jumpsuit, a light blue shirt with a short hooded cape with round ornaments, belts and a grimoire pouch; purified form: wings, halo, lines on the neck and face, light clothes)
def heath(b):
    hair = "#a4a8b8"
    P.hair_shell(b, hair, seed=101, front_low=31.2, side_low=28.2, back_low=26.0)
    P.hair_tiers(b, hair, n=2, y0=33.2, w0=8.6, shrink=1.8, h=1.1, seed=102, step_back=0.5)
    P.hair_spikes(b, hair, n=7, y=34.1, spread=3.0, length=4.0, w=2.6, seed=103, rows=2, back_lean=40.0, fan=14.0, tall=0.9)
    P.fringe(b, hair, n=6, span=7.6, y_top=31.6, length=1.6, seed=104, rot=6.0)
    T.cloak_collar(b, "#9cc4e8", trim="#2a3a5a", y=21.7, width=14.2, thick=1.7, drop=2.8, back_drop=6.0, button="#c8d0e0", roll=True, seed=105)
    Q.belts_x(b, "#14101a", "#c0c8d8")
    Q.pouch(b, "#1a1a24", "#a0a8b8")


def heath_purified(b):
    hair = "#f4f6fc"
    P.hair_shell(b, hair, seed=106, front_low=31.2, side_low=28.2, back_low=26.0)
    P.hair_tiers(b, hair, n=2, y0=33.2, w0=8.6, shrink=1.8, h=1.1, seed=107, step_back=0.5)
    P.hair_spikes(b, hair, n=7, y=34.1, spread=3.0, length=4.4, w=2.6, seed=108, rows=2, back_lean=36.0, fan=16.0, tall=0.95)
    P.fringe(b, hair, n=6, span=7.6, y_top=31.6, length=1.8, seed=109, rot=6.0)
    R3.wing(b, "#eef2fb", "#b8c8e8", "#fff0b0", span=25.0, y=21.6, sway=3.5, aspect=0.8)
    R3.wing(b, "#e6ecf8", "#a8b8e0", "#ffe890", span=18.0, y=18.5, pair=2, z=2.6, sway=3.0)
    P.halo(b, y=36.8, r=6.8, color="#fff2b0", tilt=-6.0, spin=14.0, rings=2)
    T.cloak_collar(b, "#f2f4fa", trim=GOLD, y=21.7, width=14.2, thick=1.7, drop=2.8, back_drop=6.0, button=GOLD, roll=True, seed=110)
    Q.belts_x(b, "#d8dcea", GOLD)
    T.chest_gem(b, "#8ad8ff", y=17.6)


def face_heath(purified=False):
    eyes = dict(iris=("#1a3a6a", "#3a78c8", "#b8e0ff"), cy=4.95, h=1.8, w=2.6, tilt=0.24, lash_w=0.4)
    marks = [lambda f: S2.mark_scar(f)]
    if purified:
        eyes = dict(iris=("#8a6a10", "#f0c840", "#fff4b0"), cy=4.95, h=1.8, w=2.6, tilt=0.24, glow="#ffe890", lash_w=0.4)
        marks = [lambda f: S2.mark_scar(f, color="#c8a0a0"), lambda f: S.mark_lines(f, xs=(1.2, 2.2, 5.8, 6.8), y0=0.2, y1=5.6, color="#d8b850", glow="#ffe890", w=0.12)]
    return dict(skin=("#d2c2c2", "#eadcd8", "#f8eeea"), hair="#a4a8b8" if not purified else "#f4f6fc", hairline=1.7, eyes=eyes, brow=dict(angle=0.5, thick=0.24),
                mouth=dict(kind="flat", color="#8a5a5a", lip="#b88080"), marks=marks)


# ================================================================================================ Lotus Whomalt (tall slim, medium black hair, moustache and goatee, a headpiece with three
# diamonds on the right of the forehead, a light tunic, a black three-way belt, a two-tone coat with diamond lapels, boots with fur collars)
def lotus(b):
    hair = "#2a2420"
    P.hair_shell(b, hair, seed=111, front_low=30.8, side_low=27.0, back_low=25.0)
    P.hair_tiers(b, hair, n=2, y0=33.2, w0=8.8, shrink=1.8, h=1.2, seed=112)
    P.hair_spikes(b, hair, n=6, y=34.2, spread=3.0, length=3.4, w=3.0, seed=113, rows=2, back_lean=16.0, fan=30.0)
    P.fringe(b, hair, n=7, span=8.0, y_top=31.6, length=3.4, seed=114, rot=12.0)
    P.side_locks(b, hair, y_top=29.0, length=6.0, count=2, seed=115)
    P.long_hair(b, hair, y_top=31.4, y_split=24.5, tail=3.0, tiers=1, count=6, width=8.6, seed=116, sway=1.4)
    T.headband(b, "#3a3050", "#d8d0ec", y=30.4, diamonds=3, side=-1)
    T.lapels(b, "#14101a", "#d8d0ec")
    T.fur_boots(b, "#4a4048")
    Q.belts_x(b, "#14101a", "#9a90b0", buckle=False)


def face_lotus():
    return dict(skin=("#d4b8a8", "#ecd0bc", "#fbe6d6"), hair="#2a2420", hairline=1.8,
                eyes=dict(iris=("#1c1210", "#4a3428", "#a88060"), cy=4.95, h=1.75, w=2.6, tilt=0.14, lash_w=0.4), brow=dict(angle=0.35, thick=0.26),
                mouth=dict(kind="flat", color="#7a4a4a", lip="#a87070"), marks=[lambda f: S.mark_moustache(f, color="#241c1c")])


# ================================================================================================ Mars (slim pale young man, short light purple spiky hair with a lock on the forehead, pink eyes
# and no eyebrows, three turquoise mage stones in a line on the forehead and one on the chest, a black fur coat with crystals)
def mars(b, form2=False):
    hair = "#fff0d8" if form2 else "#c8b8ec"
    gem = "#ffd060" if form2 else "#40e0d0"
    P.hair_shell(b, hair, seed=121, front_low=30.6, side_low=28.4, back_low=25.4)
    P.hair_tiers(b, hair, n=2, y0=33.3, w0=8.8, shrink=1.7, h=1.2, seed=122)
    P.hair_spikes(b, hair, n=8, y=34.2, spread=3.4, length=5.6, w=2.8, seed=123, rows=2, back_lean=12.0, fan=44.0)
    P.fringe(b, hair, n=3, span=3.0, y_top=31.6, length=4.8, seed=124, rot=8.0, part=0.0)
    T.forehead_stones(b, gem)
    P.mantle(b, "#26222e", "#121018", y=22.0, thick=2.4, width=16.4, depth=6.8, drop=3.2, seed=126)
    P.cape(b, "#1a1620", "#0c0a10", length=19.0, y_top=23.2, width=9.2, tiers=3, trim=None, fur="#26222e", seed=127, hem="ragged")
    T.studs(b, [(-6.4, 24.2, 0.0), (6.4, 24.2, 0.0), (-3.6, 23.6, 2.8), (3.6, 23.6, 2.8), (0.0, 22.6, 3.2)], gem)
    T.chest_gem(b, gem, y=17.4)


def face_mars(form2=False):
    eyes = dict(iris=("#8a1a5a", "#e060b0", "#ffd0ec"), cy=4.95, h=2.15, w=2.6, tilt=0.10, glow="#ff8ad0", lash_w=0.4)
    gem, gl = ("#ffd060", "#ffe8a0") if form2 else ("#40e0d0", "#80fff0")
    if form2:
        eyes = dict(iris=("#8a6a10", "#f0c840", "#fff4b0"), cy=4.95, h=2.15, w=2.6, tilt=0.10, glow="#ffe890", lash_w=0.4)
    return dict(skin=("#dccaca", "#f2e2de", "#fff4f0"), hair="#c8b8ec" if not form2 else "#fff0d8", hairline=1.8, eyes=eyes, brow=dict(skip=(0, 1)),
                mouth=dict(kind="smirk", color="#9a5a62", lip="#d08888"), marks=[lambda f: S2.mark_stones(f, color=gem, glow=gl)])


# ================================================================================================ Revchi Salik (tall lean, messy wavy long black hair, a burn on the left side of the face, a black
# suit with two belts for his chains, a dark red cloak buttoned at the right shoulder)
def revchi(b):
    hair = "#1c181a"
    P.hair_shell(b, hair, seed=131, front_low=30.4, side_low=26.4, back_low=22.8)
    P.hair_tiers(b, hair, n=2, y0=33.2, w0=8.8, shrink=1.8, h=1.2, seed=132)
    P.hair_spikes(b, hair, n=7, y=34.2, spread=3.2, length=4.4, w=3.0, seed=133, rows=2, back_lean=6.0, fan=56.0)
    P.fringe(b, hair, n=8, span=8.4, y_top=31.6, length=4.8, seed=134, rot=22.0)
    P.side_locks(b, hair, y_top=29.4, length=8.0, count=2, seed=135)
    P.long_hair(b, hair, y_top=31.4, y_split=20.5, tail=7.0, tiers=2, count=6, width=8.8, seed=136, sway=2.2)
    T.cloak_collar(b, "#6a1a24", y=21.7, width=14.2, thick=1.7, drop=2.8, back_drop=2.0, button="#c8a030", button_side=-1, seed=137)
    Q.belts_x(b, "#1a1214", "#9a9aa8")


def face_revchi():
    return dict(skin=("#c8a898", "#e4c4b0", "#f8dcc8"), hair="#1c181a", hairline=1.7,
                eyes=dict(iris=("#1a1618", "#5a4a4a", "#b09890"), cy=4.95, h=1.85, w=2.6, tilt=0.16, lash_w=0.42), brow=dict(angle=0.6, thick=0.28),
                mouth=dict(kind="flat", color="#7a4a4a", lip="#a86a6a"), marks=[lambda f: S2.mark_burn(f)])


# ================================================================================================ Vetto (tall muscular, long blond hair and beard, sharp teeth, a red tattoo across the nose
# and under the eyes; in his demonic magic the hair turns white, the ears point and a third eye opens)
def vetto(b):
    hair = "#d8b050"
    P.hair_shell(b, hair, seed=141, front_low=30.6, side_low=24.0, back_low=21.0, thick=1.2)
    P.hair_tiers(b, hair, n=3, y0=33.3, w0=9.6, shrink=1.5, h=1.4, seed=142)
    P.hair_spikes(b, hair, n=9, y=34.4, spread=4.2, length=6.2, w=3.2, seed=143, rows=2, back_lean=6.0, fan=58.0, tall=1.1)
    P.fringe(b, hair, n=7, span=8.6, y_top=31.6, length=3.2, seed=144, rot=14.0)
    P.side_locks(b, hair, y_top=29.0, length=9.0, count=2, seed=145)
    P.long_hair(b, hair, y_top=31.4, y_split=20.0, tail=12.0, tiers=3, count=8, width=9.6, seed=146, sway=2.6)
    T.beard(b, hair)


def vetto_demonic(b):
    hair = "#f2f0ea"
    P.hair_shell(b, hair, seed=147, front_low=30.6, side_low=24.0, back_low=21.0, thick=1.2)
    P.hair_tiers(b, hair, n=3, y0=33.3, w0=9.6, shrink=1.5, h=1.4, seed=148)
    P.hair_spikes(b, hair, n=9, y=34.4, spread=4.2, length=6.4, w=3.2, seed=149, rows=2, back_lean=6.0, fan=60.0, tall=1.1)
    P.fringe(b, hair, n=7, span=8.6, y_top=31.6, length=3.2, seed=150, rot=14.0)
    P.side_locks(b, hair, y_top=29.0, length=9.0, count=2, seed=151)
    P.long_hair(b, hair, y_top=31.4, y_split=20.0, tail=12.0, tiers=3, count=8, width=9.6, seed=152, sway=2.6)
    T.beard(b, "#d8d0c0")
    P.elf_ears(b, "#c89a70", length=3.4)
    P.horns(b, "#2a1a14", "#e0c8a0", base=(2.8, 32.0, -1.0), segs=4, length=2.6, w0=2.2, w1=0.7, curl=14.0, out=26.0)
    P.tail(b, "#d9622b", "#1a1010", segs=7, length=18.0, spade=None, y=13.4, z=2.2, w0=2.2, w1=0.7, sway=6.0, curl=-32.0, curl_step=-8.0)


def face_vetto(demonic=False):
    eyes = dict(iris=("#3a2410", "#8a5a28", "#e0b070"), cy=4.95, h=2.0, w=2.6, tilt=0.14, lash_w=0.44)
    marks = [lambda f: S2.mark_tattoo(f)]
    if demonic:
        eyes = dict(iris=("#6a0a0a", "#e03020", "#ffb090"), cy=4.95, h=2.0, w=2.6, tilt=0.14, glow="#ff4a30", slit=True, lash_w=0.44, dark_sclera="#1a1018")
        marks = [lambda f: S2.mark_tattoo(f, glow="#ff4a30"), lambda f: S.mark_third_eye(f, cx=4.0, cy=1.5, color="#2a1018", glow="#ff3a4a")]
    return dict(skin=("#c8946c", "#e2b088", "#f8ccA4"), hair="#d8b050" if not demonic else "#f2f0ea", hairline=1.7, eyes=eyes, brow=dict(angle=0.7, thick=0.3),
                mouth=dict(kind="fang", color="#7a3a3a", lip="#a86060", w=2.4), marks=marks)


# ================================================================================================ Morris Libardirt (tall slim pale blond with round black tinted glasses over blind eyes; with Lucifero's
# power: a dark mark on the forehead with lines up into the hair and down over the eyes, slit irises; purified: two pairs of horns, two pairs of wings, a halo, a long coat with a magenta lining)
def morris(b):
    hair = "#f4c840"
    P.hair_shell(b, hair, seed=171, front_low=30.8, side_low=28.2, back_low=25.4)
    P.hair_tiers(b, hair, n=2, y0=33.2, w0=8.6, shrink=1.5, h=1.2, seed=172)
    P.hair_spikes(b, hair, n=6, y=34.1, spread=3.0, length=3.6, w=2.8, seed=173, rows=2, back_lean=10.0, fan=30.0)
    P.fringe(b, hair, n=8, span=8.4, y_top=31.6, length=3.6, seed=174, rot=14.0)
    T.glasses(b, "#14101a", "#241a20", r=1.9)


def morris_purified(b):
    hair = "#f4c840"
    P.hair_shell(b, hair, seed=171, front_low=30.8, side_low=28.2, back_low=25.4)
    P.hair_tiers(b, hair, n=2, y0=33.2, w0=8.6, shrink=1.5, h=1.2, seed=172)
    P.hair_spikes(b, hair, n=6, y=34.1, spread=3.0, length=3.6, w=2.8, seed=173, rows=2, back_lean=10.0, fan=30.0)
    P.fringe(b, hair, n=8, span=8.4, y_top=31.6, length=3.6, seed=174, rot=14.0)
    T.glasses(b, "#2a1a3a", "#3a1a4a", r=1.9, alpha=0.8)
    P.horns(b, "#2a1a3a", "#e060b0", base=(2.0, 32.2, -1.2), segs=3, length=2.6, w0=1.6, w1=0.5, curl=2.0, out=10.0)
    P.horns(b, "#2a1a3a", "#e060b0", base=(3.4, 31.4, 0.0), segs=5, length=3.2, w0=2.6, w1=0.8, curl=-14.0, out=70.0)
    R3.wing(b, "#1c1226", "#8a3a7a", "#e060b0", span=26.0, y=21.8, sway=4.0)
    R3.wing(b, "#16102a", "#6a2a6a", "#c84a9a", span=19.0, y=18.0, pair=2, z=2.6, sway=3.5)
    P.cape(b, "#ece4d8", "#c8287a", length=26.0, y_top=23.4, width=9.4, tiers=3, trim=None, seed=175, hem="ragged")
    P.halo(b, y=36.8, r=6.8, color="#ffc8f0", tilt=-6.0, spin=14.0, rings=2)


def face_morris(purified=False):
    eyes = dict(iris=("#58606a", "#a8b0b8", "#e8ecf0"), cy=4.95, h=1.95, w=2.5, tilt=0.06, lash_w=0.34)
    marks = []
    mouth = dict(kind="flat", color="#8a5a5a", lip="#b88080")
    if purified:
        eyes = dict(iris=("#6a3a00", "#f0a020", "#ffe890"), cy=4.95, h=1.95, w=2.5, tilt=0.06, glow="#ffc040", slit=True, lash_w=0.34)
        marks = [lambda f: S.mark_lines(f, xs=(1.2, 2.2, 5.8, 6.8), y0=0.2, y1=5.6, color="#c84a9a", glow="#ff70c8", w=0.13)]
        mouth = dict(kind="smirk", color="#8a5a5a", lip="#b88080")
    return dict(skin=("#dcc6c0", "#f2e0d8", "#fff2ec"), hair="#f4c840", hairline=1.8, eyes=eyes, brow=dict(angle=0.2, thick=0.22), mouth=mouth, marks=marks)


# ================================================================================================ the Dark Disciples (the Dark Triad's loyal subordinates; the mod's hooded acolyte in the colour of its devil)
DEVIL_TONES = {
    "dark_disciple": ("#34323e", "#c8c0e0"),
    "dark_disciple_lucifero": ("#2a2250", "#c8a0ff"),
    "dark_disciple_megicula": ("#4a1824", "#ff6a7a"),
    "dark_disciple_beelzebub": ("#1c2a5a", "#8ab0ff"),
    "dark_disciple_astaroth": ("#4a3a14", "#ffe080"),
}


def disciple(cloth, accent):
    def build(b):
        T.hood(b, cloth, trim=accent, seed=40)
        T.cloak_collar(b, _hx(cloth, 0.9), trim=accent, y=21.9, width=16.2, depth=6.8, thick=1.7, drop=2.6, back_drop=2.0, seed=71)
        T.chest_gem(b, accent, y=17.8)
        P.bracers(b, False, y_top=17.6, rings=2, gold=_hx(cloth, 0.8), gem=accent, tassel=accent, tassels=False)
    return build


def face_disciple(accent):
    return dict(skin=("#c8c0b8", "#e8e0d4", "#fbf6ee"), hair=None, hairline=0.6,
                eyes=dict(iris=_iris(accent), cy=4.95, h=1.9, w=2.6, tilt=0.2, glow=accent, slit=True, lash_w=0.4, dark_sclera="#14101c"), brow=dict(angle=0.8, thick=0.28, color="#14101c"),
                mouth=dict(kind="flat", color="#6a5a5a", lip="#8a7a7a"), marks=[lambda f: S2.mark_mask_lines(f, glow=accent)])


# ================================================================================================ the God of Order (the mod's judge: layered white robes with gold trim, a gold stole, a hood, a closed
# pale face under a gold halo, a staff and a ledger). Its Blender body is kept; this look is the selectable player-model one.
def order(b, form2=False):
    white = "#f4f0e6"
    T.hood(b, white, trim=GOLD, seed=150, peak=2.4)
    T.cloak_collar(b, "#f8f4ea", trim=GOLD, y=21.8, width=16.6, depth=7.2, thick=2.6, drop=4.0, back_drop=3.0, button=GOLD, seed=151)
    T.stole(b, GOLD, "#fff2b0", length=19.0)
    T.robe(b, white, GOLD, y_top=13.6, length=12.2, rings=1, panels=12, radius=4.5)
    P.bracers(b, False, y_top=18.6, rings=3, gold=GOLD, gem="#fff2b0", tassel="#fff2b0")
    T.chest_gem(b, "#ffd870", y=18.2)
    if form2:
        P.halo(b, y=38.4, r=5.6, color="#ffe890", tilt=32.0, spin=-20.0, rings=2)
        T.studs(b, [(-3.2, 22.6, -2.8), (3.2, 22.6, -2.8)], "#ffd870", size=1.0)


def face_order(form2=False):
    return dict(skin=("#d8d0c8", "#eee8e0", "#fbf8f2"), hair=None, hairline=0.5, eyes=dict(skip=(0, 1)), brow=dict(skip=(0, 1)),
                mouth=dict(kind="flat", color="#a88a7a", lip="#c8aa9a", w=1.4),
                marks=[lambda f: S2.mark_closed_eyes(f, glow="#ffd870" if form2 else None), lambda f: S2.mark_forehead_diamond(f)])


# ================================================================================================ the Devil Avatar (the Underworld Gate's floor boss: the devil look of Lucifero / Megicula; wiki Megicula: ram-like horns and thin upward
# horns, long pointed ears, long wavy light hair that turns to mist, three slit eyes in dark patches, a black body below a pale head, bat wings covered in eyes, a long tail with a heart tip, tentacles)
def avatar(megicula, form2):
    def build(b):
        if megicula:
            hair, horn, hornt, wing, tailc, tailt, halo = "#e8e0f0", "#1e1218", "#d0b8c8", ("#2a1020", "#8a2a4a", "#ff6a8a"), "#241018", "#d8283a", "#ffb0b8"
        else:
            hair, horn, hornt, wing, tailc, tailt, halo = "#e6e0f4", "#17101f", "#8a6ab0", ("#1a1228", "#6a4a9a", "#9a7ad0"), "#14101c", "#8a5ab0", "#d8b8ff"
        P.hair_shell(b, hair, seed=160, front_low=30.6, side_low=25.0, back_low=22.0)
        P.hair_tiers(b, hair, n=2, y0=33.3, w0=9.0, shrink=1.8, h=1.3, seed=161)
        P.fringe(b, hair, n=8, span=8.4, y_top=31.6, length=4.0, seed=162, rot=14.0, part=0.2)
        P.long_hair(b, hair, y_top=31.4, y_split=19.0, tail=14.0 if form2 else 11.0, tiers=3 if form2 else 2, count=7, width=9.0, seed=163, sway=3.2)
        P.horns(b, horn, hornt, base=(3.1, 31.8, -0.6), segs=6, length=3.2 if form2 else 2.9, w0=2.8 if form2 else 2.5, w1=0.8, curl=15.0, out=40.0)
        P.horns(b, horn, hornt, base=(1.0, 32.4, -3.6), segs=3, length=2.6, w0=1.2, w1=0.4, curl=2.0, out=8.0)
        if form2:
            P.horns(b, horn, hornt, base=(4.0, 28.8, -0.6), segs=5, length=2.6, w0=2.2, w1=0.7, curl=12.0, out=62.0)
        P.elf_ears(b, "#b8a8b8" if not megicula else "#d4b4b8", length=3.2)
        R3.wing(b, wing[0], wing[1], wing[2], span=30.0 if form2 else 25.0, y=22.0, eyes=megicula, sway=3.5)
        if form2:
            R3.wing(b, wing[0], wing[1], wing[2], span=22.0, y=18.0, pair=2, z=2.6, eyes=megicula, sway=3.0)
        P.tail(b, tailc, tailt, segs=8, length=22.0 if form2 else 19.0, spade="heart" if megicula else "arrow", y=13.4, z=2.2, w0=2.0, w1=0.6, tip_size=6.0, curl=-34.0, curl_step=-9.0)
        P.collar(b, "#14101c" if not megicula else "#1e1218", wing[2], width=9.6)
        if megicula:
            T.tentacles(b, "#d8ccd8", "#2a1a28", n=2 if form2 else 1, y=20.5, length=11.0)
        P.halo(b, y=37.0 if form2 else 36.4, r=7.8 if form2 else 6.8, color=halo, tilt=-6.0, spin=16.0, rings=2)
        if form2:
            T.chest_gem(b, wing[2], y=17.6, size=2.2)
    return build


def face_avatar(megicula, form2):
    if megicula:
        eyes = dict(iris=("#6a0a2a", "#e02a50", "#ffc0d0"), cy=4.95, h=1.95, tilt=0.16, glow="#ff4a8a", slit=True, pupil="#10060a", lash_w=0.42, dark_sclera="#1a1018")
        marks = [lambda f: S2.mark_eye_patches(f), lambda f: S2.mark_forehead_eye(f)]
        skin = ("#d4b4b8", "#ead0d0", "#fbe8e8")
    else:
        eyes = dict(iris=("#3a1a8a", "#8a5af0", "#e0d0ff"), cy=4.95, h=1.95, tilt=0.16, glow="#b08aff", slit=True, pupil="#10060a", lash_w=0.42, dark_sclera="#1a1018")
        marks = [lambda f: S.mark_spade_forehead(f, color="#8a5ab0", glow="#c8a0ff")] if not form2 else [lambda f: S.mark_third_eye(f, glow="#c8a0ff")]
        skin = ("#5a4a70", "#7a6a90", "#a898c0")
    return dict(skin=skin, hair="#e8e0f0", hairline=1.6, eyes=eyes, brow=dict(angle=1.0, thick=0.26, color="#14101c"), mouth=dict(kind="fang", color="#2a1a28", lip="#4a2a3a", w=2.4), marks=marks)


# ================================================================================================ the list
CHARS = [
    dict(id="dark_disciple", face=face_disciple(DEVIL_TONES["dark_disciple"][1]), build=disciple(*DEVIL_TONES["dark_disciple"])),
    dict(id="dark_disciple_lucifero", face=face_disciple(DEVIL_TONES["dark_disciple_lucifero"][1]), build=disciple(*DEVIL_TONES["dark_disciple_lucifero"])),
    dict(id="dark_disciple_megicula", face=face_disciple(DEVIL_TONES["dark_disciple_megicula"][1]), build=disciple(*DEVIL_TONES["dark_disciple_megicula"])),
    dict(id="dark_disciple_beelzebub", face=face_disciple(DEVIL_TONES["dark_disciple_beelzebub"][1]), build=disciple(*DEVIL_TONES["dark_disciple_beelzebub"])),
    dict(id="dark_disciple_astaroth", face=face_disciple(DEVIL_TONES["dark_disciple_astaroth"][1]), build=disciple(*DEVIL_TONES["dark_disciple_astaroth"])),
    dict(id="devil_avatar", face=face_avatar(False, False), build=avatar(False, False), keep_extras=False),
    dict(id="devil_avatar_form2", face=face_avatar(False, True), build=avatar(False, True), keep_extras=False),
    dict(id="devil_avatar_megicula", face=face_avatar(True, False), build=avatar(True, False), keep_extras=False),
    dict(id="devil_avatar_megicula_form2", face=face_avatar(True, True), build=avatar(True, True), keep_extras=False),
    dict(id="god_of_order", face=face_order(), build=order),
    dict(id="god_of_order_form2", face=face_order(True), build=lambda b: order(b, True)),
    dict(id="heath_grice", face=face_heath(), build=heath),
    dict(id="heath_grice_form2", face=face_heath(True), build=heath_purified, keep_bones=["watch", "prop_float"]),
    dict(id="lotus_whomalt", face=face_lotus(), build=lotus),
    dict(id="mars", face=face_mars(), build=mars),
    dict(id="mars_form2", face=face_mars(True), build=lambda b: mars(b, True)),
    dict(id="revchi_salik", face=face_revchi(), build=revchi),
    dict(id="vetto", face=face_vetto(), build=vetto),
    dict(id="vetto_form2", face=face_vetto(True), build=vetto_demonic, keep_extras=False),
    dict(id="morris_libardirt", face=face_morris(), build=morris),
    dict(id="morris_libardirt_form2", face=face_morris(True), build=morris_purified, keep_bones=["prop_r"]),
]
