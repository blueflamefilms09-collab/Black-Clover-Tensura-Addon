"""Zetsu (Naruto): the Akatsuki spy, half white and half black. Hand-painted: the face split down the middle (pale left, black right) with big yellow glowing eyes, the green Venus-flytrap clamp
round the head (anime_hood, spiked leaf edges), the black Akatsuki cloak with red-edged white-outlined clouds and a high red-lined collar, one white hand and one black hand, dark wrapped sandals.
Look from memory (no wiki could be fetched)."""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import lib_ga as L  # noqa: E402

hx = L.hx
FR = ("front", "back", "west", "east")


def paint(spec, cid, masks):
    sk = L.Sk(slim=False)
    WH = [hx(c) for c in ("#8e8a86", "#b4b0aa", "#d2cfc8", "#e9e6df", "#f6f4ee", "#ffffff")]       # the white half
    BK = [hx(c) for c in ("#07070a", "#101016", "#1a1a22", "#272731", "#363644", "#4c4c5e")]        # the black half and the cloak
    GR = L.ramp("#3fae38")
    RD = [hx(c) for c in ("#5a0a10", "#860f18", "#b01820", "#d42a2c", "#ee4a40", "#ff7a66")]
    YL = [hx("#a07800"), hx("#d4a800"), hx("#ffd800"), hx("#fff060"), hx("#fffbb0")]
    L.paint_base_skin(sk, WH[1:])
    hair = hx("#6ed03a")
    HT = L.ramp(hair, 5)

    # ------------------------------------------------------------ head: the flytrap clamp (hood), the split face
    for face_ in ("west", "east", "back", "top"):
        sk.fn("head", face_, False, lambda x, y, w, h: GR[4] if y < 2 else (GR[3] if y < 5 else GR[2]))
    sk.fn("head", "front", False, lambda x, y, w, h: None)
    # front: rows 0-1 leaf, then the face. left half (viewer's left) white, right half black
    def half(x, y, w, h):
        base = WH if x < 4 else BK
        return base[3] if y < 5 else base[2]
    sk.fn("head", "front", False, half)
    for x in range(8):
        sk.put("head", "front", False, x, 0, GR[4]); sk.put("head", "front", False, x, 1, GR[3])
    for y in range(2, 8):                              # the two leaf walls at the sides, with teeth
        for x, c in ((0, GR[3]), (7, GR[2])):
            sk.put("head", "front", False, x, y, c if y % 2 == 0 else GR[1])
    sk.put("head", "front", False, 0, 2, GR[5]); sk.put("head", "front", False, 7, 3, GR[4]); sk.put("head", "front", False, 0, 5, GR[5]); sk.put("head", "front", False, 7, 6, GR[4])
    # eyes: big yellow glowing eyes with a black slit pupil, one in each half of the face (set in from the clamp so they show)
    for xs, tone in (((2, 3), WH), ((5, 6), BK)):
        for x in xs:
            sk.put("head", "front", False, x, 3, BK[1] if tone is WH else BK[3])          # the heavy lid
            sk.put("head", "front", False, x, 4, YL[3], True)
            sk.put("head", "front", False, x, 5, YL[2], True)
        sk.put("head", "front", False, xs[0], 4, YL[4], True)
    sk.put("head", "front", False, 3, 4, BK[0]); sk.put("head", "front", False, 3, 5, BK[0])           # the pupil slits
    sk.put("head", "front", False, 5, 4, BK[0]); sk.put("head", "front", False, 5, 5, BK[0])
    sk.put("head", "front", False, 4, 5, WH[2]); sk.put("head", "front", False, 4, 6, BK[3])            # the nose, on the split
    for x, c in ((2, WH[2]), (3, BK[0]), (4, BK[0]), (5, BK[0]), (6, BK[1])):                          # a wide sinister grin
        sk.put("head", "front", False, x, 7, c)
    sk.put("head", "front", False, 3, 7, YL[3]); sk.put("head", "front", False, 5, 7, YL[3])             # a glint of teeth
    # the leaf clamp seen on the hat layer: the spiked rim round the face
    for x in range(8):
        sk.put("head", "front", True, x, 1, GR[5] if x % 2 == 0 else GR[3])
    for face_ in ("west", "east"):
        for x in range(8):
            sk.put("head", face_, True, x, 6 if x % 2 == 0 else 5, GR[2])
            sk.put("head", face_, True, x, 7, GR[1]) if x % 2 == 0 else None

    # ------------------------------------------------------------ the Akatsuki cloak: black, white-outlined red clouds, a high red-lined collar
    cloak = BK
    for part in ("body", "arm_r", "arm_l"):
        for ov in (False, True):
            sk.cloth(part, ov, cloak, y1=11 if part != "body" else None, folds=(3, 4))
    cloud_f = [
        "KKKKKKKK",
        "KKKKKKKK",
        "KKKKKKKK",
        "KKKKKKKK",
        "KKKKKKKK",
        "KKKKKKKK",
        "KKKKKKKK",
        "KKKKKKKK",
        "KKKKKKKK",
        "KKKKKKKK",
        "KKKKKKKK",
        "KKKKKKKK",
    ]
    lg = {"K": None, "w": WH[5], "W": WH[4], "r": RD[3], "R": RD[2], "q": RD[1], "p": RD[4], "k": BK[3], "j": BK[2], "i": BK[4], "z": BK[1], "y": YL[3]}
    # collar: the cloak's standing collar, red lining showing on the inside rim, black outside, a white seam
    collar_f = [
        "qRrrrrRq",
        "zjkijkjz",
        "zjkkkkjz",
        "jkijkikj",
    ]
    for ov in (True,):
        sk.art("body", "front", ov, collar_f, lg)
        sk.art("body", "back", ov, ["qRrrrrRq", "zjkijkjz", "zjkkkkjz"], lg)
        for face_ in ("west", "east"):
            sk.art("body", face_, ov, ["qRrq", "zjkz", "zjkz"], lg)
        sk.fn("body", "top", ov, lambda x, y, w, h: RD[2] if (x in (0, w - 1) or y in (0, h - 1)) else RD[1])
    # the front zipper line and the cloud pattern: each cloud = a 4x3 shape with a white outline
    cloud = [
        ".ww.",
        "wrrw",
        "wRRrw",
    ]
    def cloud_art(part, face, ov, x, y, flip=False):
        shapes = [
            "..ww...",
            ".wrrw..",
            "wrRRrww",
            "wRrrRRw",
            ".wwwwww",
        ]
        if flip:
            shapes = [r[::-1] for r in shapes]
        sk.art(part, face, ov, shapes, lg, ox=x, oy=y)
    for ov in (True, False):                       # cloth folds: light ridges down the cloak
        for y in range(3, 7):
            sk.put("body", "front", ov, 1, y, BK[4]); sk.put("body", "front", ov, 6, y, BK[4])
    # zipper / seam down the chest
    for y in range(3, 12):
        for ov in (True, False):
            sk.put("body", "front", ov, 3, y, BK[5] if y % 2 else BK[3])
            sk.put("body", "front", ov, 4, y, BK[1])
    # clouds on the cloak: lower body, both sleeves, back
    for ov in (True, False):
        cloud_art("body", "front", ov, 1, 7)
        cloud_art("body", "back", ov, 0, 3)
        cloud_art("body", "back", ov, 1, 8, True)
        for part in ("arm_r", "arm_l"):
            for face_ in ("front", "back"):
                sk.art(part, face_, ov, [".wW.", "wrRw", "wRrw", ".ww."], lg, oy=4)
            for face_ in ("west", "east"):
                sk.art(part, face_, ov, [".ww.", "wrRw", "wRRw", ".ww."], lg, oy=3)
    # sleeve cuffs: wide hanging cuffs with a red lining line, hands bare (the left hand white, the right hand black)
    for part, hand in (("arm_r", WH), ("arm_l", BK)):
        for face_ in FR:
            for x in range(4):
                for ov in (True, False):
                    sk.put(part, face_, ov, x, 9, RD[3] if x % 2 == 0 else RD[2])
                    sk.put(part, face_, ov, x, 10, BK[3])
                    if ov:
                        sk.cv.put(sk.rect(part, face_, True)[0] + x, sk.rect(part, face_, True)[1] + 11, (0, 0, 0, 0))
                        sk.put(part, face_, False, x, 11, hand[3] if face_ == "front" else hand[2])
        sk.put(part, "front", False, 1, 11, hand[4])
    # shoulders: a lighter cloth ridge (the cloak is cut in broad shoulders)
    for part in ("arm_r", "arm_l"):
        for ov in (True,):
            for face_ in ("front", "back", "west", "east"):
                for x in range(4):
                    sk.put(part, face_, ov, x, 0, BK[5]); sk.put(part, face_, ov, x, 1, BK[4])
            sk.fn(part, "top", ov, lambda x, y, w, h: BK[5] if (x + y) % 2 == 0 else BK[4])

    # ------------------------------------------------------------ legs: black trousers wrapped at the shin, dark sandals with white tape
    for part in ("leg_r", "leg_l"):
        for ov in (False, True):
            sk.cloth(part, ov, BK, y0=0, y1=12, folds=(1,))
        for face_ in FR:
            for x in range(4):
                sk.put(part, face_, True, x, 8, WH[3] if x % 2 == 0 else WH[2])           # white tape wraps round the shin
                sk.put(part, face_, True, x, 9, BK[3])
                sk.put(part, face_, True, x, 10, WH[2] if x % 2 == 0 else WH[3])
                sk.put(part, face_, True, x, 11, BK[2] if x % 2 else BK[3])
        sk.put(part, "front", True, 0, 11, BK[5]); sk.put(part, "front", True, 3, 11, BK[5])

    img, glow = sk.finish()
    return img, glow, "anime_hood", hair, False
