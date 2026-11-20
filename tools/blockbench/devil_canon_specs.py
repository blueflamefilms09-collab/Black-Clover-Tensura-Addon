"""The 26 canon devil specs (docs/devil_trial_model.md, 'Canon looks'). One dict per devil, in roster order (devil/WorldDevil). Each says how the devil is built (body, dims, outfit, hair, horns, wings,
tail ...), its palette and glow, and the marks painted on its face and body; 'source' says where the design came from (rule 10: English wiki first, Japanese as fallback; what is the mod's own design is said so).
"""


# ---------------------------------------------------------------- mark helpers (coordinates: the head front is 8 x 8, the chest 12 x 6.4, the abdomen 8.4 x 4.2)
def eyes(y=3.9, sep=1.9, w=2.2, h=1.2, sclera="sclera", iris="eye", pupil="ink", slit=True, glow=True, iw=1.5, lid=True):
    """Polish pass: larger eyes (2.2 x 1.2 of the 8 x 8 face, the iris 1.5 wide and glowing), an upper lid line and a shadow under the eye, so the face reads from a few blocks away."""
    ops = []
    for s in (-1, 1):
        cx = 4 + s * sep
        if lid:                                                            # the shadow under the eye is drawn first so the iris keeps its last row
            ops.append(("r", cx - w / 2, y + h, cx + w / 2, y + h + 0.25, "skin2"))
            ops.append(("r", cx - w / 2 - 0.2, y - 0.4, cx + w / 2 + 0.2, y - 0.1, "ink"))
        ops.append(("r", cx - w / 2, y, cx + w / 2, y + h, sclera))
        ops.append(("r", cx - iw / 2, y, cx + iw / 2, y + h, iris, glow))
        if slit:
            ops.append(("r", cx - 0.12, y, cx + 0.12, y + h, pupil))
    return ops


def brow(y=3.1, sep=1.9, w=2.0, slant=0.5, col="ink"):
    ops = []
    for s in (-1, 1):
        cx = 4 + s * sep
        ops.append(("l", cx - s * w / 2, y + slant, cx + s * w / 2, y - slant * 0.2, col))
        ops.append(("l", cx - s * w / 2, y + slant - 0.3, cx + s * w / 2, y - slant * 0.2 - 0.3, col))
    return ops


def mouth(kind="fangs", y=6.2, col="ink"):
    if kind == "fangs":
        return [("l", 2.4, y, 5.6, y, col), ("r", 2.7, y, 3.2, y + 0.8, "tooth"), ("r", 4.8, y, 5.3, y + 0.8, "tooth")]
    if kind == "grin":
        return [("l", 2.0, y - 0.4, 3.0, y, col), ("l", 3.0, y, 5.0, y, col), ("l", 5.0, y, 6.0, y - 0.4, col), ("r", 3.2, y, 3.6, y + 0.6, "tooth"), ("r", 4.4, y, 4.8, y + 0.6, "tooth")]
    if kind == "flat":
        return [("l", 2.8, y, 5.2, y, col)]
    if kind == "sewn":
        return [("l", 2.2, y, 5.8, y, col)] + [("r", x, y - 0.5, x + 0.25, y + 0.5, col) for x in (2.4, 3.2, 4.0, 4.8, 5.4)]
    if kind == "open":
        return [("r", 2.6, y - 0.4, 5.4, y + 1.2, col), ("r", 2.8, y - 0.4, 3.2, y + 0.3, "tooth"), ("r", 4.8, y - 0.4, 5.2, y + 0.3, "tooth")]
    return []


def nose(y=5.0):
    return [("r", 3.2, y, 3.5, y + 0.4, "ink"), ("r", 4.5, y, 4.8, y + 0.4, "ink")]


def lines(pts, col="ink", gl=False):
    return [("l", a[0], a[1], b[0], b[1], col, gl) for a, b in pts]


def ring_eye(cx, cy, r, cols):
    """Concentric squares of an eye: cols from the outside in; the one flagged glow is listed as (col, True)."""
    ops = []
    for i, c in enumerate(cols):
        g = False
        if isinstance(c, tuple):
            c, g = c
        rr = r * (1 - i / float(len(cols)))
        ops.append(("r", cx - rr, cy - rr, cx + rr, cy + rr, c, g))
    return ops


def dots(pts, col="eye", w=2, h=2, gl=True, ring="white"):
    """Eyes scattered on a face given as fractional points: a ring of white and a glowing centre."""
    ops = []
    for fx, fy in pts:
        ops.append(("t", fx, fy, w + 2, h + 2, ring, False))
        ops.append(("t", fx, fy, w, h, col, gl))
        ops.append(("t", fx, fy, 1, h, "ink", False))
    return ops


def bars(xs, y0, y1, col="ink", w=0.5, gl=False):
    """Vertical bars (tribal stripes, teeth, panel seams): x positions, from y0 to y1."""
    return [("r", x - w / 2, y0, x + w / 2, y1, col, gl) for x in xs]


def hbars(ys, x0, x1, col="ink", h=0.5, gl=False):
    """Horizontal bars: y positions, from x0 to x1."""
    return [("r", x0, y - h / 2, x1, y + h / 2, col, gl) for y in ys]


SPECS = []


def spec(**kw):
    kw.setdefault("body", "humanoid")
    SPECS.append(kw)
    return kw


# ================================================================ Black Clover
spec(id="liebe", source="Black Clover wiki (en), Liebe: Appearance. Messy white hair, short, body coated in black with a line up the chest, two lines across the red eyes, two bat-like wings, two bull-like horns, long thin tail with a trident tip, pointed teeth, clawed fingers and toes",
     polish="white demon body with black tribal markings (chest, belly, arms, face) instead of an all-black suit; black scaled legs; thick curved bull horns; bigger messy white hair; longer thicker trident tail; raised wings with a fanned membrane; larger red eyes",
     colors=dict(skin="#ece8f4", skin2="#17151d", hair="#f6f4fa", accent="#e8e8f0", horn="#2a262e", cloth="#17151d", membrane="#2a2530", claw="#2a262e", tooth="#f2f0f6", metal="#e0e0ea"), glow="#e84050", eye="#ff2a3e",
     dims=dict(hip=8.5, lt=3.8, lx=2.3, pw=8.4, aw=7.4, cw=10.0, chh=5.4, cd=5.8, hs=9.0, at=3.6, ua=5.2, fa=5.2, ah=3.4),
     torso_mat="skin", arm_mat="skin", leg_mat="skin2", pelvis_mat="skin2", belt_mat="skin2", belt_gem=False, hand_mat="skin", neck_mat="skin", ears="pointed", legs="claw", chin_spike=False,
     detail=dict(scales=["skin2"]),
     hair=dict(cap=True, fringe=3, spikes=9, spike_len=5.6, spike_fan=9, back=6, side=2),
     horns=[dict(kind="bull", x=3.0, y=7.8, s=0.85)], wings=[dict(kind="bat", span=0.9, drop=9, y=-1.0, mem="membrane", rot=(0, 14, -4))], tail=dict(len=6.0, w=1.9, tip="trident", mat="skin2"),
     paint={"face": eyes(sclera="sclera", iris="eye") + [("l", 0.8, 3.4, 3.4, 4.9, "ink"), ("l", 4.6, 4.9, 7.2, 3.4, "ink"), ("l", 0.8, 4.4, 3.4, 5.9, "ink"), ("l", 4.6, 5.9, 7.2, 4.4, "ink"), ("r", 3.7, 0.6, 4.3, 2.2, "ink"), ("l", 1.4, 5.4, 2.0, 7.0, "ink"), ("l", 6.6, 5.4, 6.0, 7.0, "ink")] + mouth("fangs"),
            "chest": [("r", 5.6, 0.0, 6.4, 6.4, "ink")] + lines((((0.8, 0.8), (5.2, 3.4)), ((11.2, 0.8), (6.8, 3.4)))) + lines((((1.8, 3.0), (5.2, 5.4)), ((10.2, 3.0), (6.8, 5.4)))) + [("r", 2.2, 4.4, 2.8, 5.0, "ink"), ("r", 9.2, 4.4, 9.8, 5.0, "ink")],
            "abs": [("r", 3.8, 0.0, 4.6, 4.2, "ink")] + hbars((2.4,), 1.0, 7.4, "ink", 0.5),
            "upper_*|nsew": hbars((3.2,), 0.0, 4.0, "ink", 0.8),
            "fore_*|nsew": [("r", 0.0, 3.4, 4.0, 5.2, "ink")]})

spec(id="lucifero", source="Black Clover wiki (en), Lucifero: Appearance. Tall, four bat-like wings, four curved upward horns (temples and sides), long light hair, pointed teeth and ears, dark sclera red irises slit pupils, thin bony hands with dark nails, long dark skirt, wrinkled twisted skin",
     polish="the three-horned look (a tall forehead horn and two large C-curved horns), dark armour with gold trim over the long skirt (breastplate, pauldrons with spikes, vambraces), a gold octagon halo and gold halo marks on forehead, cheeks and chest, curved horns that no longer end level, four fanned wings, red eyes",
     colors=dict(skin="#cfc4d8", skin2="#8a7a8a", hair="#f0ecf8", accent="#e8c058", horn="#2e2438", cloth="#1c1424", cloth2="#2e2038", membrane="#3a2a4a", claw="#1c1424", tooth="#f0eaf4", metal="#e8c058", white="#34264a", ink="#120c18"), glow="#ffd060", eye="#ff2a40",
     dims=dict(hip=15.0, lt=3.4, lx=2.4, pw=8.4, aw=7.4, cw=10.8, chh=6.6, cd=5.8, hs=8.0, at=3.2, ua=8.2, fa=8.0, ah=4.8), legs="boot", ears="long", claws=3, claw_len=1.6, claw_mat="claw", belt_gem=True,
     outfit=["plate", "long_skirt"], detail=dict(plates=["white"], folds=["cloth", "cloth2", "boot"]), hair=dict(cap=True, fringe=4, back=22, side=15, fringe_len=3.0),
     horns=[dict(kind="curve", x=3.2, y=7.6, s=0.95), dict(kind="up", single=True, y=8.2, z=-1.2, s=1.9)],
     wings=[dict(kind="bat", span=1.45, drop=14, y=0.0, rot=(0, 14, -4)), dict(kind="bat", pfx="wing2", span=1.1, drop=11, y=-4.0, rot=(0, 24, 20))], extras=[("halo", "metal", 8.5), ("pauldron_spikes", "metal")],
     sway=[("wing2_r", "rotation", (0, 4, -6), 0.8), ("wing2_l", "rotation", (0, -4, 6), 0.8)],
     paint={"face": eyes(sclera="ink", iris="eye") + brow(slant=0.9) + mouth("fangs", 6.4) + lines((((1.2, 4.9), (2.2, 5.1)), ((5.8, 5.1), (6.8, 4.9)))) + [("r", 3.6, 1.2, 4.4, 2.6, "metal", True), ("l", 3.2, 2.0, 4.0, 2.7, "metal"), ("l", 4.8, 2.0, 4.0, 2.7, "metal"), ("r", 1.0, 5.6, 1.5, 6.2, "metal", True), ("r", 6.5, 5.6, 7.0, 6.2, "metal", True)],
            "chest": lines((((1.0, 2.0), (4.0, 2.4)), ((8.0, 2.4), (11.0, 2.0)))),
            "jacket": [("r", 5.4, 0.0, 6.8, 6.0, "metal")] + [("r", 4.6, 1.2, 8.0, 1.8, "metal", True), ("r", 4.6, 3.6, 8.0, 4.2, "metal", True), ("r", 4.6, 1.2, 5.2, 4.2, "metal", True), ("r", 7.4, 1.2, 8.0, 4.2, "metal", True)],
            "skirt|nsew": [("r", 5.7, 0.0, 6.3, 11.0, "metal")]})

spec(id="beelzebub", source="Black Clover wiki (en), Beelzebub: Appearance. Large black figure, pointy ears, two wings, four horns (two small upright, two larger curving out and up), hair to the neck splitting into strands, fur on chest and shoulders",
     polish="two pairs of veined fly wings (Lord of the Flies) instead of bat curtains, insect mandibles, a lighter blue-grey face with big glowing eyes so the head reads, a glowing maw painted on the belly (gluttony), thick curved horns, rim-lit dark body",
     colors=dict(skin="#252843", skin2="#12131f", head="#3c4470", hair="#2a2e4a", accent="#5a6ae0", horn="#c8c4d4", cloth="#1c1e30", fur="#2c3050", fur2="#1a1c30", membrane="#8a9ad8", membrane2="#5a6ab0", claw="#c8c4d4", tooth="#e8e6f0", metal="#5a6ae0", ink="#0c0d18"), glow="#5a7aff", eye="#8aa8ff",
     dims=dict(hip=12.5, lt=5.0, lx=3.2, pw=12.0, aw=10.6, cw=16.0, chh=7.6, cd=8.0, hs=9.0, at=5.6, ua=8.0, fa=7.6, ah=5.0, hand=3.6), legs="hoof", ears="pointed", claws=3, claw_len=3.4, head_mat="head",
     outfit="fur", hair=dict(cap=True, fringe=5, back=9, side=9, fringe_len=3.4), detail=dict(folds=["fur2"]),
     horns=[dict(kind="up", x=2.0, y=8.4, s=1.1), dict(kind="bull", x=3.8, y=6.6, s=0.95)],
     wings=[dict(kind="insect", len=21.0, wid=8.4, span=1.0, mem="membrane", mem2="membrane2", y=0.0, rot1=(0, 12, -52), rot2=(0, 18, -10))], tail=None, extras=[("chest_gem",), ("pauldron_spikes", "horn"), ("mandibles",)],
     sway=[("wing_low_r", "rotation", (0, 5, -8), 1.0), ("wing_low_l", "rotation", (0, -5, 8), 1.0)],
     paint={"face": eyes(sclera="ink", iris="eye", w=2.6, iw=1.8, h=1.3) + brow(slant=0.7, w=2.6) + mouth("fangs", 6.3) + lines((((0.8, 2.6), (1.8, 3.4)), ((7.2, 2.6), (6.2, 3.4))), "eye", True),
            "chest": lines((((3.0, 1.5), (5.0, 3.0)), ((9.0, 1.5), (7.0, 3.0))), "eye", True),
            "abs": [("r", 1.0, 0.4, 7.4, 3.8, "ink"), ("r", 1.6, 1.9, 6.8, 2.3, "eye", True)] + bars((1.6, 2.6, 3.6, 4.6, 5.6, 6.6), 0.8, 1.7, "tooth", 0.6) + bars((1.6, 2.6, 3.6, 4.6, 5.6, 6.6), 2.6, 3.5, "tooth", 0.6)})

spec(id="astaroth", source="NOT on the English wiki (Astaroth has no Appearance section: only 'highest-ranking devil, Time Magic') and the Japanese wiki could not be fetched: this look is the mod's own design (golden, time-themed): cream hair, a clock ring behind the head, gold feathered wings, an hourglass gem",
     polish="formal tailored robe (high collar, gold trim lines, panel seams) with white gloves, gold cuffs and red eyes (the brief asked for the butler-formal look); a round gold clock halo (octagon with hands) instead of a square; seven-feather wings; an hourglass and clock emblem on the chest; gold marks on the cheeks",
     colors=dict(skin="#efdcc4", skin2="#c8b090", hair="#f8f0d6", accent="#f5d76e", horn="#f0e2a8", cloth="#e8dcc0", cloth2="#b88a28", membrane="#f5d76e", claw="#d8b860", tooth="#fff8ea", metal="#f5d76e", white="#fff8ea", glove="#fffaf0", ink="#2a1a08"), glow="#ffd64a", eye="#ff4030",
     dims=dict(hip=15.0, lt=3.8, lx=2.5, pw=8.8, aw=7.8, cw=11.4, chh=6.6, cd=6.0, hs=8.0, at=3.6, ua=8.0, fa=7.6, ah=4.8), legs="boot", ears="pointed", claws=3, claw_len=2.0, outfit="robe", hand_mat="glove", hair=dict(cap=True, fringe=3, back=12, side=9, mat="hair"),
     detail=dict(plates=["cloth2"], folds=["cloth", "cloth2", "boot", "glove"]),
     horns=[dict(kind="curve", x=2.8, y=7.8, s=0.62), dict(kind="sweep", x=3.6, y=5.4, z=-0.4, s=0.8)],
     wings=[dict(kind="feather", span=1.25, rows=3, n=7, drop=12, mem="membrane", mem2="white", y=-0.5, rot=(0, 14, -2))], tail=None, extras=[("halo", "gem", 9.0, True), ("chest_gem",)],
     paint={"face": eyes(sclera="white", iris="eye", slit=False, glow="eye") + brow(slant=0.3) + mouth("flat", 6.4) + [("l", 0.9, 4.8, 1.6, 6.0, "metal"), ("l", 7.1, 4.8, 6.4, 6.0, "metal")],
            "chest": [("r", 5.6, 1.4, 6.4, 2.0, "eye", True), ("r", 5.2, 2.0, 6.8, 2.6, "eye", True), ("r", 5.6, 2.6, 6.4, 3.4, "eye", True)] + lines((((5.0, 1.0), (7.0, 1.0)), ((5.0, 3.6), (7.0, 3.6))), "eye", True),
            "jacket": bars((1.4, 11.2), 0.0, 6.0, "metal") + hbars((0.6,), 0.0, 12.6, "metal", 0.6) + [("r", 4.8, 0.0, 7.8, 0.8, "cloth2"), ("r", 5.0, 1.0, 7.6, 1.4, "ink"), ("r", 5.0, 3.6, 7.6, 4.0, "ink"), ("l", 5.0, 1.4, 7.6, 3.6, "metal"), ("l", 7.6, 1.4, 5.0, 3.6, "metal")],
            "jacket_low": bars((2.0, 7.0), 0.0, 4.6, "metal"),
            "sleeve_lo_*|nsew": [("r", 0.0, 4.6, 6.0, 5.6, "metal")]})

spec(id="megicula", source="Black Clover wiki (en), Megicula: Appearance. Petite, pale from the neck up and black below, a black star pattern at the neck, three eyes with dark patches, big curved ram horns and thin upward forehead horns, long pointy ears, long wavy light hair turning to mist, light tentacles all over, bat wings covered in eyes, long thin tail with a heart tip",
     polish="taller and sharper (the brief: tall, spiked, pale with dark armour): longer limbs, black spiked armour (breastplate, spiked pauldrons, vambraces, back spines) with pink trim, pale face and hands, mist hair with upward spikes, ram horns kept thick, three bigger eyes, eye-covered wings with a fanned membrane, the heart tail kept",
     colors=dict(skin="#efe8f4", skin2="#14101c", hair="#ece4f2", accent="#e0307a", horn="#3a2a40", cloth="#14101c", membrane="#2a1c34", claw="#3a2a40", tooth="#f6f0fa", mist="#ece4f2", metal="#e0307a", white="#241a30", ink="#0c0812"), glow="#ff3a8a", eye="#ff3a8a",
     dims=dict(hip=13.0, lt=3.4, lx=2.2, pw=8.2, aw=7.2, cw=10.0, chh=6.0, cd=5.6, hs=8.6, at=3.2, ua=7.6, fa=7.4, ah=4.4), torso_mat="skin2", arm_mat="skin2", leg_mat="skin2", pelvis_mat="skin2", belt_mat="skin2", belt_gem=False, neck_mat="skin", hand_mat="skin", head_mat="skin",
     legs="claw", ears="long", claws=3, claw_len=2.4, outfit="plate", detail=dict(plates=["white"], scales=["skin2"]),
     hair=dict(cap=True, fringe=5, back=20, side=13, fringe_len=3.4, mat="mist", spikes=5, spike_len=4.0, spike_fan=20),
     horns=[dict(kind="ram", x=3.0, y=7.6, s=1.2, mat="horn"), dict(kind="up", x=1.4, y=8.2, z=-2.4, s=1.1)],
     wings=[dict(kind="bat", span=1.15, drop=12, y=-1.0, mem="membrane", rot=(0, 14, -4))], tail=dict(len=6.4, w=1.6, tip="heart", mat="skin2"), extras=[("third_eye",), ("tentacles", 3, "mist", "ink"), ("pauldron_spikes", "horn"), ("back_spikes", "horn", 4)],
     paint={"face": [("r", 0.8, 3.0, 3.2, 5.2, "ink"), ("r", 4.8, 3.0, 7.2, 5.2, "ink")] + eyes(y=3.8, w=2.0, sclera="white", iris="eye", iw=1.4, lid=False) + brow(slant=0.4) + [("r", 3.5, 1.7, 4.5, 2.6, "eye", True)] + mouth("flat", 6.4),
            "jacket": [("r", 4.6, 0.0, 8.0, 0.8, "metal"), ("r", 5.4, 0.8, 7.2, 3.8, "metal"), ("r", 5.8, 1.2, 6.8, 3.4, "ink"), ("r", 6.0, 1.8, 6.6, 2.6, "eye", True)] + hbars((5.2,), 0.0, 11.0, "metal", 0.6),
            "wing_mem_*|ns": dots([(0.5, 0.25), (0.5, 0.6)], "eye", 2, 2)})

spec(id="lucifugus", source="Black Clover wiki (en), Lucifugus: Appearance. Tall and slender, long pointed ears, two large ox-like horns, a pair of wings, a long prehensile tail; the palette (lavender, violet) follows the mod's Kotodama devil",
     polish="large ox horns that curve up (the old ones leaned out like antennae), longer ears, violet robe with a gem clasp, violet trim lines and folds, raised fanned wings, a longer tail ending in a leaf blade, bigger eyes",
     colors=dict(skin="#ece0f6", skin2="#c8b8e0", hair="#dcd0f4", accent="#b06aff", horn="#3a2a58", cloth="#2a1e44", cloth2="#5a3a9a", membrane="#4a3a78", claw="#3a2a58", tooth="#fffaff", metal="#c9a6ff", ink="#160e26"), glow="#c08aff", eye="#c08aff",
     dims=dict(hip=15.5, lt=3.2, lx=2.3, pw=8.2, aw=7.2, cw=10.2, chh=6.6, cd=5.4, hs=7.6, at=3.0, ua=8.6, fa=8.4, ah=5.0), legs="hoof", ears="long", claws=3, claw_len=3.0, outfit="robe", hair=dict(cap=True, fringe=3, back=6, side=7),
     detail=dict(plates=["cloth2"]),
     horns=[dict(kind="bull", x=3.0, y=7.2, s=1.15)], wings=[dict(kind="bat", span=1.2, drop=12.5, y=-0.5, rot=(0, 14, -4))], tail=dict(len=8.0, w=2.2, tip="blade", n=3, mat="skin"), extras=[("chest_gem",)],
     paint={"face": eyes(sclera="ink", iris="eye") + brow(slant=0.5) + mouth("flat", 6.4), "chest": lines((((4.0, 1.0), (6.0, 3.0)), ((8.0, 1.0), (6.0, 3.0))), "eye", True),
            "jacket": bars((1.6, 11.0), 0.0, 6.0, "accent") + hbars((0.5,), 0.0, 12.6, "accent", 0.6) + [("r", 5.0, 0.6, 7.8, 1.2, "accent")],
            "skirt|nsew": bars((1.2, 3.6, 6.0, 8.4, 10.8), 0.0, 14.0, "cloth2", 0.5)})

# ================================================================ Black Butler
spec(id="sebastian", source="Kuroshitsuji wiki (en), Sebastian Michaelis: Appearance. Tall, handsome adult, black hair, reddish-brown eyes (glowing fuchsia, slit pupils in the demon form), pale skin, black nails, tailcoat, vest, tie with the crest, white gloves, a chained pocket watch, high-heeled boots; demon form with claws, longer teeth, dark wings with many eyes",
     polish="red slit eyes now readable (the fringe stopped hiding them, eyes larger), tailcoat with lapel lines, buttons and cuffs painted on, silver watch chain, crisp white gloves and shirt front with the crest on the tie, wings with more eyes and a fanned membrane, cloth folds and rim light on the black coat",
     colors=dict(skin="#f0e2e0", hair="#1a1822", accent="#d8204a", horn="#1a1620", cloth="#16141c", cloth2="#54505e", membrane="#241a2c", claw="#16141c", tooth="#fff8f8", metal="#d8d8e4", white="#f6f4f8", boot="#0e0c12", ink="#0a080e"), glow="#ff3a70", eye="#ff2850",
     dims=dict(hip=16.0, lt=3.4, lx=2.4, pw=8.6, aw=7.6, cw=11.0, chh=6.4, cd=5.6, hs=8.0, at=3.2, ua=8.0, fa=8.0, ah=5.0), legs="heel", pants="cloth", ears="pointed", claws=3, claw_len=1.5, claw_mat="claw", outfit="butler", belt_gem=False, belt_mat="cloth",
     detail=dict(folds=["cloth", "cloth2", "boot", "white"]),
     hair=dict(cap=True, fringe=5, back=3, side=5, fringe_len=3.2), horns=[], wings=[dict(kind="feather", span=1.2, rows=3, n=6, drop=12, mem="membrane", mem2="cloth2", y=-0.5, rot=(0, 14, -2))],
     paint={"face": eyes(sclera="white", iris="eye", w=2.8, iw=2.2, h=1.4, glow="eye") + brow(slant=0.6) + mouth("flat", 6.3), "wing_f_1_*|ns": dots([(0.5, 0.55)], "eye", 1, 2, ring="ink"), "wing_f_0_*|ns": dots([(0.5, 0.5)], "eye", 1, 2, ring="ink"),
            "jacket": [("l", 5.6, 0.0, 3.2, 5.2, "cloth2"), ("l", 7.4, 0.0, 9.8, 5.2, "cloth2"), ("r", 6.2, 4.4, 6.8, 5.0, "metal"), ("r", 6.2, 3.0, 6.8, 3.6, "metal")],
            "sleeve_lo_*|nsew": [("r", 0.0, 4.6, 4.2, 5.4, "white"), ("r", 1.8, 4.7, 2.4, 5.3, "metal")]})
