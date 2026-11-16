"""The table of the 26 devils: per devil the id (the WorldDevil id), the head skin and hair, the eyes, the outfit of the painted skin and the build function of the cube parts (dvchars.py).  DEVILS is read by gen_dv.py."""
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
sys.dont_write_bytecode = True

import bcskin as S  # noqa: E402
import dvchars as C  # noqa: E402
import dvskin as DS  # noqa: E402
from dvchars import (BLACK, eyes, face, line, weg)  # noqa: E402

H = DS.H


def extra_eyes(rows, iris, glow=None):
    """More eyes stacked on the face (the Future Devil): rows of (cy, w, h) painted as small anime eyes."""
    def fn(f):
        for cy, w, h in rows:
            for cx in (2.35, 5.65):
                H.eye(f, cx, cy, w, h, iris, lash="#1c1226", sclera="#e8f4f0", tilt=0.08, glow=glow, lash_w=0.3)
    return fn


def cheek_eyes(color="#ff6a30"):
    """Small round eyes on the cheeks (the Fox Devil's many eyes)."""
    def fn(f):
        for cx, cy in ((0.9, 3.1), (7.1, 3.1), (1.0, 6.5), (7.0, 6.5)):
            f.ellipse(cx, cy, 0.42, 0.42, "#fff6e8")
            f.ellipse(cx, cy, 0.24, 0.24, color, glow=color)
            f.ellipse(cx, cy, 0.1, 0.1, "#10060a")
    return fn


def sewn(color="#2a1a22"):
    """Eyes and mouth sewn shut (the Ghost Devil): two lid lines with stitches and stitches across the mouth."""
    def fn(f):
        for cx in (2.35, 5.65):
            f.polyline([(cx - 1.3, 4.9), (cx, 5.15), (cx + 1.3, 4.9)], color, 0.18)
            for k in range(-2, 3):
                f.polyline([(cx + k * 0.5, 4.55), (cx + k * 0.5, 5.4)], color, 0.07, a=0.8)
        f.polyline([(2.6, 6.9), (5.4, 6.9)], color, 0.16)
        for k in range(7):
            f.polyline([(2.7 + k * 0.4, 6.5), (2.7 + k * 0.4, 7.3)], color, 0.07, a=0.8)
    return fn


def patches(color="#9a9aa4"):
    """Patches of diseased skin (Lucifer's host)."""
    def fn(f):
        for cx, cy, r in ((1.2, 2.6, 0.7), (6.6, 6.2, 0.9), (0.9, 6.6, 0.5)):
            f.ellipse(cx, cy, r, r * 0.8, color, a=0.5, soft=3.0)
    return fn


def chest_hole(color="#3ad8d8"):
    """A dark hollow in the chest with an eye in it (the Future Devil), on the torso's front."""
    def fn(cv):
        f = cv.face("body", "front", False)
        f.ellipse(4.0, 5.4, 1.9, 2.1, "#0a0806")
        H.gem(f, 4.0, 5.4, 1.1, color, setting="#2a1c10", glow=True)
    return fn


def bra(color="#e8b830", gem="#ff6a2a"):
    """A gold top over a bare midriff (Iblis' dancer's attire)."""
    def fn(cv):
        f = cv.face("body", "front", False)
        H.trim_band(f, 1.4, 4.2, color, spec=0.4)
        H.gem(f, 4.0, 2.8, 0.55, gem, setting=color, glow=True)
        H.trim_band(cv.face("body", "back", False), 1.4, 4.2, color, spec=0.3)
    return fn


def contract_mark(color="#d82a9a"):
    """The mark of the Faustian contract on the back of the left hand (Sebastian)."""
    def fn(cv):
        f = cv.face("arm_l", "back", False)
        w = f.w / 2
        f.polyline([(w - 0.9, 10.9), (w, 9.9), (w + 0.9, 10.9), (w - 1.0, 10.1), (w + 1.0, 10.1), (w - 0.9, 10.9)], color, 0.1, a=0.9, glow=color)
    return fn


def dv(id, build, skin, hair, e, outfit, brow=None, mouth=None, marks=(), blush=0.0, slim=False, scale=1.0, body_skin=None, hairline=1.8):
    o = dict(outfit)
    o["skin"] = skin
    if body_skin:
        o["body_skin"] = body_skin
    return dict(id=id, build=build, slim=slim, scale=scale, hair=hair, outfit=o, face=face(skin, hair, e, brow, mouth, marks, blush, hairline))


RED = ("#5a0a10", "#d82a30", "#ffb0a8")
FANG = dict(kind="fang", color="#6a2a30", lip="#8a4a50")
TEETH = dict(kind="teeth_dark", w=2.6)


def lines_face(xs, y0, y1, color, glow, w=0.14):
    return lambda f: S.mark_lines(f, xs=xs, y0=y0, y1=y1, color=color, glow=glow, w=w)


DEVILS = [
    # ---- Black Clover
    dv("liebe", C.liebe, BLACK, "#f4f2f8", eyes(*RED, glow="#ff3a40", tilt=0.18, w=2.6), dict(kind="bare", legs_bare=True, boots=None, accent="#e8e8f0", marks=[weg("#e8e8f0")]),
       brow=dict(color="#d8d4e0", angle=0.8, thick=0.2), mouth=FANG, marks=[line((0.1, 3.6), (7.9, 4.5), color="#e8e8f0", w=0.2, a=0.8), line((0.1, 4.3), (7.9, 5.3), color="#e8e8f0", w=0.2, a=0.6)], scale=0.9),
    dv("lucifero", C.lucifero, ("#bcb0bc", "#d8ccd8", "#efe6ef"), "#ece8f6", eyes(*RED, glow="#ff4a8a", slit=True, dark_sclera="#1a1018"),
       dict(kind="bare", pants="#1c1426", boots="#1c1426", accent="#9a7ad0", marks=[weg("#9a7ad0")]), mouth=TEETH, marks=[S.mark_wrinkles], scale=1.08, hairline=1.6),
    dv("beelzebub", C.beelzebub, BLACK, "#2a2e4a", eyes("#0a1a6a", "#3a4ac8", "#b8c8ff", glow="#5a6aff", slit=True, dark_sclera="#0a0a14"),
       dict(kind="bare", pants="#1c1e2e", boots="#101018", accent="#5a6ae0", marks=[weg("#5a6ae0")]), mouth=TEETH, scale=1.1),
    dv("astaroth", C.astaroth, ("#e8d4b8", "#f6e6cc", "#fff8ea"), "#f4ecd0", eyes("#8a6a10", "#f5c030", "#fff4b0", glow="#f5d76e", slit=True),
       dict(kind="robe", top="#2a2418", trim="#f5d76e", accent="#f5d76e"), brow=dict(color="#c8b878", angle=0.4, thick=0.22), mouth=dict(kind="smirk", color="#8a5a3a", lip="#c08a6a"), scale=1.04),
    dv("megicula", C.megicula, ("#d8ccd8", "#ece0ec", "#fff6fc"), "#e8e0ee", eyes("#6a0a2a", "#d82a6a", "#ffa0c8", glow="#ff4a9a", slit=True, dark_sclera="#2a1424"),
       dict(kind="coat", top="#1c1020", pants="#1c1020", boots="#1c1020", trim="#e8dce8", accent="#d82a6a", gem=False), body_skin=BLACK, mouth=dict(kind="fang", color="#a84a60", lip="#d8847a"),
       marks=[lambda f: S.mark_third_eye(f, cy=1.7, color="#2a1018", glow="#ff3a8a")], slim=True, scale=0.94),
    dv("lucifugus", C.lucifugus, ("#d4c8e4", "#e8dcf4", "#f8f0ff"), "#dcd0f4", eyes("#3a0a7a", "#b06aff", "#e8d0ff", glow="#c890ff", slit=True),
       dict(kind="robe", top="#241a3a", trim="#b06aff", accent="#b06aff", marks=[weg("#b06aff")]), brow=dict(color="#a890d0", angle=0.5, thick=0.2), mouth=dict(kind="smirk", color="#6a4a8a", lip="#a888c0"), scale=1.06),
    # ---- Chainsaw Man
    dv("fox_devil", C.fox, ("#b86a20", "#e08a2a", "#f4b060"), "#e08a2a", eyes("#6a1a00", "#ff6a30", "#ffd0a0", glow="#ff6a30", slit=True, dark_sclera="#2a1008"),
       dict(kind="open", top="#2a1c14", trim="#e08a2a", pants="#2a1c14", boots="#f6f0e8", belt="#f6f0e8", accent="#ff6a30", body_skin=("#b86a20", "#e08a2a", "#f4b060")),
       mouth=dict(kind="grin", color="#6a2a1a", lip="#8a4a30"), marks=[cheek_eyes()]),
    dv("curse_devil", C.curse, ("#c8c0b0", "#e6e0d0", "#fff8e8"), None, eyes("#2a0a4a", "#8a3aff", "#e0c0ff", glow="#c070ff", dark_sclera="#0a0610", w=2.7, h=2.1),
       dict(kind="robe", top="#1c1224", trim="#8a4aba", accent="#c070ff"), mouth=TEETH, marks=[lines_face((0.9, 1.5, 6.5, 7.1), 3.4, 7.8, "#1a1018", "#c070ff")], scale=1.06),
    dv("future_devil", C.future, ("#6a4a30", "#8a6440", "#a88050"), None, eyes("#0a4a4a", "#3ad8d8", "#d0ffff", glow="#3ad8d8", w=2.3, h=1.5, cy=5.4),
       dict(kind="bare", pants="#4a3220", boots="#2a1c10", accent="#3ad8d8", marks=[weg("#3ad8d8"), chest_hole()]), mouth=dict(kind="open", color="#1a0a06", lip="#3a2218", w=3.0, open=1.0),
       marks=[extra_eyes([(2.6, 1.9, 1.1), (3.9, 2.1, 1.2)], ("#0a4a4a", "#3ad8d8", "#d0ffff"), glow="#3ad8d8")], scale=1.08),
    dv("ghost_devil", C.ghost, ("#9a98a8", "#b8b6c4", "#d4d2dc"), "#1a1620", eyes("#2a2a3a", "#5a5a7a", "#9a9ab8", skip=(0, 1)), dict(kind="dress", top="#f4b8d0", trim="#8aa8e8", accent="#8aa8e8", boots=None, sleeve=4, gem=False),
       brow=dict(color="#2a2430", angle=-0.2, thick=0.2), mouth=dict(kind="flat", color="#3a3a4a", lip="#5a5a6a"), marks=[sewn()], slim=True),
    # ---- Black Butler
    dv("sebastian", C.sebastian, ("#d8c8c8", "#ecdcdc", "#fcf0f0"), "#14121a", eyes("#6a1a3a", "#d82a9a", "#ffb0e0", glow="#ff4ab0", slit=True),
       dict(kind="butler", top="#101014", trim="#c8c8d0", pants="#101014", boots="#08080c", belt="#7a7a84", glove="#f2f0f4", accent="#d82a9a", marks=[contract_mark()], gem=False),
       brow=dict(color="#14121a", angle=0.5, thick=0.24), mouth=dict(kind="smirk", color="#7a3a4a", lip="#b06a74"), scale=1.06),
    # ---- Blue Exorcist
    dv("kin_of_iblis", C.iblis, ("#6a3a24", "#8a5233", "#a8704c"), "#ff6a2a", eyes("#8a6a00", "#f0c820", "#fff6a0", glow="#ffd830", slit=True),
       dict(kind="bare", pants="#c8400a", boots=None, accent="#ff6a2a", marks=[bra()], sleeve=0), mouth=dict(kind="smirk", color="#5a1a10", lip="#8a3a2a"),
       marks=[lambda f: S.mark_third_eye(f, cy=1.6, color="#3a1008", glow="#ffd830")], slim=True, scale=0.98),
    dv("kin_of_egyn", C.egyn, ("#8a6a50", "#aa8466", "#c8a080"), "#7ab8f0", eyes("#8a6a10", "#f0c030", "#fff0a0", glow="#ffd860", h=2.3, w=2.8),
       dict(kind="coat", top="#2a6ab8", trim="#a8e0ff", pants="#1c3a78", boots="#e8f4ff", accent="#7ab8f0"), mouth=dict(kind="open", color="#6a2a2a", lip="#a85a5a"), blush=0.3, slim=True, scale=0.92),
    dv("kin_of_amaimon", C.amaimon, ("#c8a890", "#e8c8b0", "#fce6d4"), "#3aa85a", eyes("#4a5a00", "#a8d030", "#f0ff90", glow="#c8ff40", slit=True, dark_sclera="#1a1a10"),
       dict(kind="coat", top="#4a3a28", trim="#8a6a3a", pants="#3a2a1c", boots="#2a1c10", accent="#a8d030"), mouth=dict(kind="grin", color="#6a3a3a", lip="#a86060")),
    dv("kin_of_astaroth", C.kin_astaroth, ("#8a9a6a", "#aab888", "#c8d6a8"), "#5a6a2a", eyes("#6a0a10", "#d82a30", "#ffc0b0", glow="#ff3a30", slit=True),
       dict(kind="coat", top="#3a4a1a", trim="#c8d8a0", pants="#2a3210", boots="#1a2008", accent="#9ad03a"), mouth=FANG, scale=0.96),
    dv("kin_of_lucifer", C.kin_lucifer, ("#e8d0b8", "#f8e4cc", "#fffaf0"), "#f4dc7a", eyes("#8a7010", "#f0d050", "#fffbd0", glow="#fff0a8", h=2.0),
       dict(kind="robe", top="#f6f0e0", trim="#f5d76e", accent="#fff0a8"), mouth=dict(kind="fang", color="#8a5a4a", lip="#c08a7a"), marks=[patches()], scale=1.06),
    # ---- Tensura
    dv("daemon_noir", C.noir, ("#e0c8bc", "#f2dcd0", "#fff0e8"), "#14101a", eyes("#8a6a00", "#f0c030", "#fff0a0", glow="#ffd860", slit=True, dark_sclera="#14080a", pupil="#c81e2d"),
       dict(kind="coat", top="#14101a", trim="#e8b830", pants="#14101a", boots="#08060a", belt="#8a1020", accent="#e8b830"), mouth=dict(kind="smirk", color="#8a4a50", lip="#c08088"), scale=1.06),
    dv("daemon_blanc", C.blanc, ("#e8d8d8", "#f6eaea", "#fffafa"), "#f4f4fc", eyes("#5a0a14", "#c81e2d", "#ff9aa0", glow="#ff4a5a"),
       dict(kind="coat", top="#2a2a34", trim="#c8c8d8", pants="#1c1c24", boots="#14141a", accent="#ff4a5a"), mouth=dict(kind="flat", color="#a85a60", lip="#d88a90"), slim=True, scale=1.05),
    dv("daemon_jaune", C.jaune, ("#e0c0a8", "#f4d8c4", "#fff0e4"), "#f0d030", eyes("#1a3a8a", "#3a78d8", "#b8d8ff"),
       dict(kind="coat", top="#14161c", trim="#f0f0f4", pants="#14161c", boots="#08090c", belt="#c8c8d0", accent="#f0d030", gem=False), mouth=dict(kind="smirk", color="#a85a60", lip="#d8847a"), slim=True, scale=0.98),
    dv("daemon_violet", C.violet, ("#e8d4d8", "#f8e6e8", "#fffafa"), "#9a4ae0", eyes("#8a7000", "#f0d020", "#fff8a0", glow="#ffe840"),
       dict(kind="dress", top="#1c1824", trim="#c890ff", pants="#1c1824", boots="#14101c", accent="#c890ff", sleeve=12), mouth=dict(kind="open", color="#8a3a50", lip="#d8847a"), blush=0.3, slim=True, scale=0.96),
    dv("daemon_rouge", C.rouge, ("#d8b098", "#f0ccb4", "#ffe6d4"), "#c81e2d", eyes("#5a0a10", "#d82030", "#ffa0a8", glow="#ff3a48"),
       dict(kind="open", top="#1a1214", trim="#e8e0d0", pants="#1a1214", boots="#0a0608", belt="#8a0a14", accent="#ff4a58"), brow=dict(angle=0.9, thick=0.3), mouth=dict(kind="smirk", color="#7a3a3a", lip="#a86060"),
       marks=[line((1.5, 5.9), (1.2, 7.1), color="#0e0a0c", w=0.2), line((2.3, 6.0), (2.1, 7.2), color="#0e0a0c", w=0.2), line((6.5, 5.9), (6.8, 7.1), color="#0e0a0c", w=0.2), line((5.7, 6.0), (5.9, 7.2), color="#0e0a0c", w=0.2)],
       scale=1.06),
    dv("daemon_vert", C.vert, ("#e8d4c8", "#f8e6dc", "#fffaf4"), "#3aa85a", eyes("#0a4a1a", "#3aa85a", "#b8f4c8"),
       dict(kind="dress", top="#8a1020", trim="#f2f0f4", boots="#14101c", glove="#3aa85a", accent="#3aa85a", sleeve=5, gem=False), mouth=dict(kind="smirk", color="#a85a60", lip="#d8847a"), blush=0.3, slim=True),
    dv("daemon_bleu", C.bleu, ("#e8d4c8", "#f8e6dc", "#fffaf4"), "#3a6ae8", eyes("#0a1a6a", "#3a6ae8", "#b8d0ff"),
       dict(kind="dress", top="#8a1020", trim="#f2f0f4", boots="#14101c", glove="#3a6ae8", accent="#3a6ae8", sleeve=5, gem=False), mouth=dict(kind="flat", color="#a85a60", lip="#d8847a"), blush=0.35, slim=True, scale=0.94),
    # ---- Dororo, Berserk, Fairy Tail
    dv("hall_demon", C.hall, ("#8a5a3a", "#aa7248", "#c8906a"), "#3a2a22", eyes("#6a3a00", "#f0a020", "#ffe090", glow="#ffb040", slit=True, dark_sclera="#1a100a"),
       dict(kind="open", top="#4a3228", trim="#d8c8a8", pants="#3a2a22", boots="#2a2018", belt="#d8c8a8", accent="#ffb040"), brow=dict(angle=1.0, thick=0.34),
       mouth=dict(kind="fang", color="#4a1a10", lip="#6a3a28", fang="#f0e8d0"), scale=1.1),
    dv("god_hand", C.god_hand, ("#e8dcdc", "#f6eeee", "#ffffff"), "#f4f0f6", eyes("#4a0a14", "#c81e2d", "#ff9aa0", glow="#ff4a58", slit=True),
       dict(kind="armor", top="#14080c", trim="#c8a050", pants="#14080c", boots="#0a0408", accent="#c81e2d"), brow=dict(color="#d8d0d8", angle=0.6, thick=0.22), mouth=dict(kind="smirk", color="#8a4a50", lip="#c08088"), scale=1.08),
    dv("etherious", C.etherious, ("#c8b8dc", "#e0d4f0", "#f4ecff"), "#2a1a4a", eyes("#1a1030", "#6a3cff", "#c8b0ff", glow="#8a5cff", slit=True, dark_sclera="#0a0618"),
       dict(kind="coat", top="#1a1030", trim="#6a3cff", pants="#120a24", boots="#0a0618", accent="#8a5cff", marks=[weg("#8a5cff")]), mouth=dict(kind="smirk", color="#5a3a7a", lip="#8a6aa8"),
       marks=[lines_face((0.6, 1.2, 6.8, 7.4), 3.8, 7.9, "#1a1030", "#8a5cff", 0.12)], scale=1.04),
]

IDS = [d["id"] for d in DEVILS]
