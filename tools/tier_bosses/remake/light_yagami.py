"""Light Yagami (Death Note): a slim, poised young man with side-swept auburn-brown hair, calm sharp amber eyes (they flare warm and glow), a faint knowing smile, a dark navy school blazer with silver
buttons and notched lapels over a white shirt and a charcoal tie, dark trousers, polished black shoes. Kept from the earlier build (the owner's 3.36 motif and LightYagamiDataTest): the small teal
notebook in the breast pocket and the thin teal page-edge line on the left cuff are drawn by gen_tier_looks.light_post, called at the end. Look from memory (no wiki could be fetched in the session)."""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import lib_d as L  # noqa: E402

hx = L.hx
FR = L.FR


def _post():
    """gen_tier_looks.light_post (the notebook pocket and the teal cuff); gen_tier_looks may be __main__ already, so import it by file name."""
    try:
        import gen_tier_looks as G
        return G.light_post
    except Exception:                                   # pragma: no cover
        import importlib.util
        path = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "gen_tier_looks.py")
        spec_ = importlib.util.spec_from_file_location("gen_tier_looks_for_light", path)
        mod = importlib.util.module_from_spec(spec_)
        spec_.loader.exec_module(mod)
        return mod.light_post


def paint(spec, cid, masks):
    sk = L.Sk(slim=True)
    S = L.skin_ramp("#f4d4b8")
    hair = hx("#7e4c2e")
    HT = L.ramp(hair, 5)
    N = L.lin("#0a0f1c", "#3a4a72", 6, 0.9)      # navy blazer (kept dark: the test wants a dark suit)
    W = L.lin("#9aa0bc", "#fdfbf4", 6, 0.85)     # white shirt
    K = L.lin("#07080e", "#2e3342", 6)           # trousers, shoes, tie
    SV = L.ramp("#c8ccd6", 6)                    # silver buttons
    lg = L.lg_from([("abcdef", N), ("ghijkl", W), ("ABCDEF", K), ("mnopqr", SV), ("01234", S)])
    L.paint_base_skin(sk, S)

    # ------------------------------------------------------------ head: side-swept auburn hair (3D), sharp calm amber eyes that glow, a faint smile
    L.head_sides(sk, S, HT, hair_side_rows=4, hair_back_rows=7, ear=True, sideburn=1)
    sk.fn("head", "front", False, lambda x, y, w, h: S[3] if y >= 2 else HT[2])
    for y in range(2, 8):
        sk.put("head", "front", False, 0, y, S[2]); sk.put("head", "front", False, 7, y, S[2])
    brow = HT[1]
    for ox, ix in ((1, 2), (6, 5)):                                   # thin brows, the inner end one row lower (a composed, slightly severe look)
        sk.put("head", "front", False, ox, 3, brow); sk.put("head", "front", False, ix, 3, HT[0])
    ir = (hx("#4a2208"), hx("#b4601c"), hx("#f2a448"))                # amber eyes
    lash = hx("#2a160e")
    for ox, ix in ((1, 2), (6, 5)):
        sk.put("head", "front", False, ox, 4, lash); sk.put("head", "front", False, ix, 4, lash)              # the sharp upper lid
        sk.put("head", "front", False, ox, 5, ir[0]); sk.put("head", "front", False, ix, 5, ir[1], glow=True)
        sk.put("head", "front", False, ox, 6, ir[1], glow=True); sk.put("head", "front", False, ix, 6, ir[2], glow=True)
    sk.put("head", "front", False, 2, 5, hx("#fff6e0")); sk.put("head", "front", False, 5, 5, hx("#fff6e0"))        # catch-lights
    sk.put("head", "front", False, 0, 4, L.mix(S[2], lash, 0.3)); sk.put("head", "front", False, 7, 4, L.mix(S[2], lash, 0.3))
    sk.put("head", "front", False, 3, 5, S[4]); sk.put("head", "front", False, 4, 5, S[3])
    sk.put("head", "front", False, 3, 6, S[2]); sk.put("head", "front", False, 4, 6, S[2])
    for x, c in ((2, S[2]), (3, hx("#b0685e")), (4, hx("#b0685e")), (5, hx("#9a5850"))):      # a thin, composed smile, one corner up
        sk.put("head", "front", False, x, 7, c)
    sk.put("head", "front", False, 5, 6, L.mix(S[2], hx("#9a5850"), 0.4))
    sk.put("head", "front", False, 1, 7, S[1]); sk.put("head", "front", False, 6, 7, S[1])               # the jaw line

    # ------------------------------------------------------------ body: a navy blazer, white shirt and charcoal tie; silver buttons
    for ov in (False, True):
        L.panel(sk, "body", "front", ov, N, [2, 4, 4, 3, 3, 3, 2, 1], 0, 12)
        L.panel(sk, "body", "back", ov, N, [1, 2, 3, 3, 3, 3, 2, 1], 0, 12)
        for f in ("west", "east"):
            L.panel(sk, "body", f, ov, N, [2, 3, 3, 1] if f == "west" else [1, 3, 3, 2], 0, 12)
        sk.fn("body", "top", ov, lambda x, y, w, h: N[4])
    front = [
        "ckllllkc",     # 0 shirt collar, the points lie on the blazer
        "dklABlkd",     # 1 tie knot between the shirt
        "ddkABkdd",     # 2 the shirt V narrows
        "dcjABjcd",     # 3
        "ddeABedd",     # lapel edges
        "dceABecd",
        "dcdACdcd",     # 6 the tie ends
        "ddcdoddc",     # 7 button
        "cdcddddc",
        "dcdodddc",     # 9 lower button
        "cdcddcdc",
        "bcbcbcbc",     # 11 hem
    ]
    for ov in (True, False):
        sk.art("body", "front", ov, front, lg)
    # the notched lapels: a lighter edge following the V, silver buttons in a column
    for ov in (True,):
        for (x, y) in ((2, 2), (5, 2), (2, 3), (5, 3)):
            sk.put("body", "front", ov, x, y, N[4])
        sk.put("body", "front", ov, 3, 7, SV[4]); sk.put("body", "front", ov, 3, 9, SV[3])
        sk.put("body", "front", ov, 1, 1, N[4]); sk.put("body", "front", ov, 6, 1, N[4])
        sk.put("body", "front", ov, 3, 2, K[3]); sk.put("body", "front", ov, 3, 4, K[3])
    back = [
        "bcdddddb", "bddeeddb", "bdddeddb", "cdddddcb", "cddddddb", "cdddddcb", "cdddddcb", "cdddddcb", "bcdddcbb", "bcddddcb", "bccdcdcb", "bcbcbcbc",
    ]
    back = [r[:8] for r in back]
    for ov in (True, False):
        sk.art("body", "back", ov, back, lg)
    side = ["bcdb", "cdcb", "ddcb", "ddcb", "cddb", "ddcb", "cdcb", "ddcb", "cddb", "dcdb", "cdcb", "bcbb"]
    for f in ("west", "east"):
        for ov in (True, False):
            sk.art("body", f, ov, side if f == "west" else L.flip(side), lg)

    # ------------------------------------------------------------ arms (slim): navy sleeves, a shirt cuff and silver button; the cuff line is painted by light_post
    sleeve = [
        "cde",
        "dde",
        "cdd",
        "ddc",
        "cdc",
        "dcb",
        "cdb",
        "dcb",
        "cdb",      # row 8: blazer cuff
        "bcb",      # row 9: (the teal page-edge line of light_post lands here on the left cuff)
        "hkh",      # row 10: the white shirt cuff
    ]
    for part in ("arm_r", "arm_l"):
        prof = [1, 3, 3] if part == "arm_r" else [3, 3, 1]
        for ov in (False, True):
            L.panel(sk, part, "front", ov, N, prof, 0, 11)
            L.panel(sk, part, "back", ov, N, [1, 3, 2], 0, 11)
            L.panel(sk, part, "west", ov, N, [2, 3, 3, 1], 0, 11)
            L.panel(sk, part, "east", ov, N, [1, 3, 3, 2], 0, 11)
        for f in ("front", "back"):
            sk.art(part, f, True, sleeve, lg)
        for f in ("west", "east"):
            sk.art(part, f, True, [r + "b" for r in sleeve], lg)
        sk.fn(part, "top", True, lambda x, y, w, h: N[4])
        for f in ("front", "back"):
            for x in range(3):
                sk.put(part, f, False, x, 10, W[4] if x != 1 else W[5])
        for f in ("west", "east"):
            for x in range(4):
                sk.put(part, f, False, x, 10, W[3])
        L.hands(sk, part, S, rows=(11,))
        sk.put(part, "front", True, 1, 8, SV[4])
    sk.put("arm_r", "front", True, 1, 8, SV[4])

    # ------------------------------------------------------------ legs: dark trousers with a crease, polished black shoes
    for part in ("leg_r", "leg_l"):
        prof = [1, 3, 3, 2] if part == "leg_r" else [2, 3, 3, 1]
        for ov in (False, True):
            L.panel(sk, part, "front", ov, K, [x + 1 for x in prof], 0, 9, yadd=lambda y: 1 if y < 2 else 0)
            for f in ("back", "west", "east"):
                L.panel(sk, part, f, ov, K, [1, 2, 3, 1], 0, 9)
        for f in FR:
            for x in range(4):
                sk.put(part, f, True, x, 9, K[3] if x in (1, 2) else K[2])           # shoes
                sk.put(part, f, True, x, 10, K[4] if x in (1, 2) else K[2])
                sk.put(part, f, True, x, 11, K[1] if x in (1, 2) else K[0])
        sk.put(part, "front", True, 1, 10, K[5]); sk.put(part, "front", True, 2, 9, K[5]); sk.put(part, "front", True, 1, 8, K[4])
        for y in range(0, 8):
            sk.put(part, "front", True, 1 if part == "leg_r" else 2, y, K[4])         # the trouser crease

    _post()(sk.cv, spec, 0, sk.glow)         # the notebook pocket and the teal cuff line (3.36.0), drawn last so they stay
    img, glow = sk.finish()
    return img, glow, "anime_swept", hair, True
