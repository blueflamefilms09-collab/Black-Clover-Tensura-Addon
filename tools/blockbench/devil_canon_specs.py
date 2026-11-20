"""The 26 canon devil specs (docs/devil_trial_model.md, 'Canon looks'). One dict per devil, in roster order (devil/WorldDevil). Each says how the devil is built (body, dims, outfit, hair, horns, wings,
tail ...), its palette and glow, and the marks painted on its face and body; 'source' says where the design came from (rule 10: English wiki first, Japanese as fallback; what is the mod's own design is said so).
"""


# ---------------------------------------------------------------- mark helpers (coordinates: the head front is 8 x 8, the chest 12 x 6.4, the abdomen 8.4 x 4.2)
def eyes(y=3.9, sep=1.9, w=1.8, h=1.0, sclera="sclera", iris="eye", pupil="ink", slit=True, glow=True, iw=1.0):
    ops = []
    for s in (-1, 1):
        cx = 4 + s * sep
        ops.append(("r", cx - w / 2, y, cx + w / 2, y + h, sclera))
        ops.append(("r", cx - iw / 2, y, cx + iw / 2, y + h, iris, glow))
        if slit:
            ops.append(("r", cx - 0.25, y, cx + 0.25, y + h, pupil))
    return ops


def brow(y=3.1, sep=1.9, w=2.0, slant=0.5, col="ink"):
    ops = []
    for s in (-1, 1):
        cx = 4 + s * sep
        ops.append(("l", cx - s * w / 2, y + slant, cx + s * w / 2, y - slant * 0.2, col))
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


SPECS = []


def spec(**kw):
    kw.setdefault("body", "humanoid")
    SPECS.append(kw)
    return kw


# ================================================================ Black Clover
spec(id="liebe", source="Black Clover wiki (en), Liebe: Appearance. Messy white hair, short, body coated in black with a line up the chest, two lines across the red eyes, two bat-like wings, two bull-like horns, long thin tail with a trident tip, pointed teeth, clawed fingers and toes",
     colors=dict(skin="#d8d4e2", skin2="#17151d", hair="#f4f2f8", accent="#e8e8f0", horn="#2a262e", cloth="#17151d", membrane="#2a2530", claw="#2a262e", tooth="#f2f0f6", metal="#e0e0ea"), glow="#e84050", eye="#e8283a",
     dims=dict(hip=8.5, lt=3.8, lx=2.3, pw=8.4, aw=7.4, cw=10.0, chh=5.4, cd=5.8, hs=9.0, at=3.6, ua=5.2, fa=5.2, ah=3.4),
     torso_mat="skin2", arm_mat="skin2", leg_mat="skin2", pelvis_mat="skin2", belt_mat="skin2", belt_gem=False, hand_mat="skin", neck_mat="skin", ears="pointed", legs="claw", chin_spike=False,
     hair=dict(cap=True, fringe=3, spikes=7, spike_len=4.6, spike_fan=26, back=5, side=0),
     horns=[dict(kind="bull", x=3.0, y=7.8, s=1.15)], wings=[dict(kind="bat", span=0.85, drop=8.5, y=-1.0, mem="membrane")], tail=dict(len=5.4, w=1.7, tip="trident", mat="skin2"),
     paint={"face": eyes(sclera="sclera", iris="eye") + [("l", 1.0, 3.6, 3.2, 4.8, "ink"), ("l", 4.8, 4.8, 7.0, 3.6, "ink"), ("l", 1.0, 4.6, 3.2, 5.6, "ink"), ("l", 4.8, 5.6, 7.0, 4.6, "ink")] + mouth("fangs"),
            "chest": [("r", 5.7, 0.0, 6.3, 6.4, "white")]})

spec(id="lucifero", source="Black Clover wiki (en), Lucifero: Appearance. Tall, four bat-like wings, four curved upward horns (temples and sides), long light hair, pointed teeth and ears, dark sclera red irises slit pupils, thin bony hands with dark nails, long dark skirt, wrinkled twisted skin",
     colors=dict(skin="#bcb0bc", skin2="#8a7a8a", hair="#ece8f6", accent="#9a7ad0", horn="#2e2438", cloth="#1c1424", cloth2="#2e2038", membrane="#3a2a4a", claw="#1c1424", tooth="#f0eaf4", metal="#9a7ad0"), glow="#b070ff", eye="#e83050",
     dims=dict(hip=15.0, lt=3.4, lx=2.4, pw=8.4, aw=7.4, cw=10.8, chh=6.6, cd=5.8, hs=8.0, at=3.2, ua=8.2, fa=8.0, ah=4.8), legs="boot", ears="long", claws=3, claw_len=1.6, claw_mat="claw", belt_gem=True,
     outfit="long_skirt", hair=dict(cap=True, fringe=4, back=22, side=15, fringe_len=3.0),
     horns=[dict(kind="curve", x=3.2, y=7.6, s=1.1), dict(kind="curve", x=4.4, y=4.6, z=-0.4, s=1.0)],
     wings=[dict(kind="bat", span=1.45, drop=14, y=0.0), dict(kind="bat", pfx="wing2", span=1.1, drop=11, y=-4.0, rot=(0, 24, 26))],
     paint={"face": eyes(sclera="ink", iris="eye") + brow(slant=0.9) + mouth("fangs", 6.4) + lines((((1.2, 4.9), (2.2, 5.1)), ((5.8, 5.1), (6.8, 4.9)), ((3.0, 2.0), (5.0, 2.0)), ((2.6, 2.4), (5.4, 2.4)))),
            "chest": lines((((1.0, 2.0), (4.0, 2.4)), ((8.0, 2.4), (11.0, 2.0)), ((2.0, 4.0), (5.0, 4.2)), ((7.0, 4.2), (10.0, 4.0)))),
            "abs": lines((((1.0, 1.2), (3.5, 1.4)), ((5.0, 1.4), (7.4, 1.2)), ((1.5, 2.8), (4.0, 3.0)), ((4.6, 3.0), (7.0, 2.8))))})

spec(id="beelzebub", source="Black Clover wiki (en), Beelzebub: Appearance. Large black figure, pointy ears, two wings, four horns (two small upright, two larger curving out and up), hair to the neck splitting into strands, fur on chest and shoulders",
     colors=dict(skin="#1c1e30", skin2="#12131f", hair="#2a2e4a", accent="#5a6ae0", horn="#c8c4d4", cloth="#1c1e30", fur="#2c3050", fur2="#1a1c30", membrane="#262a48", claw="#c8c4d4", tooth="#e8e6f0", metal="#5a6ae0"), glow="#5a7aff", eye="#6a8aff",
     dims=dict(hip=12.5, lt=5.0, lx=3.2, pw=12.0, aw=10.6, cw=16.0, chh=7.6, cd=8.0, hs=9.0, at=5.6, ua=8.0, fa=7.6, ah=5.0, hand=3.6), legs="hoof", ears="pointed", claws=3, claw_len=3.4,
     outfit="fur", hair=dict(cap=True, fringe=5, back=9, side=9, fringe_len=3.4),
     horns=[dict(kind="up", x=2.0, y=8.4, s=1.1), dict(kind="bull", x=3.8, y=6.6, s=1.45)],
     wings=[dict(kind="bat", span=1.35, drop=13, y=-1.0, rot=(0, 16, 12))], tail=None, extras=[("chest_gem",), ("pauldron_spikes", "horn")],
     paint={"face": eyes(sclera="ink", iris="eye", w=2.0) + brow(slant=0.7, w=2.4) + mouth("fangs", 6.3),
            "chest": lines((((3.0, 1.5), (5.0, 3.0)), ((9.0, 1.5), (7.0, 3.0))), "eye", True)})

spec(id="astaroth", source="NOT on the English wiki (Astaroth has no Appearance section: only 'highest-ranking devil, Time Magic') and the Japanese wiki could not be fetched: this look is the mod's own design (golden, time-themed): cream hair, a clock ring behind the head, gold feathered wings, an hourglass gem",
     colors=dict(skin="#e8d4b8", skin2="#c8b090", hair="#f4ecd0", accent="#f5d76e", horn="#f0e2a8", cloth="#e8dcc0", cloth2="#b88a28", membrane="#f5d76e", claw="#d8b860", tooth="#fff8ea", metal="#f5d76e", white="#fff8ea"), glow="#ffd64a", eye="#ffd64a",
     dims=dict(hip=15.0, lt=3.8, lx=2.5, pw=8.8, aw=7.8, cw=11.4, chh=6.6, cd=6.0, hs=8.0, at=3.6, ua=8.0, fa=7.6, ah=4.8), legs="boot", ears="pointed", claws=3, claw_len=2.0, outfit="robe", hair=dict(cap=True, fringe=3, back=12, side=9, mat="hair"),
     horns=[dict(kind="curve", x=2.8, y=7.8, s=0.95), dict(kind="sweep", x=3.6, y=5.4, z=-0.4, s=0.8)],
     wings=[dict(kind="feather", span=1.25, rows=3, n=6, drop=12, mem="membrane", mem2="white", y=-0.5)], tail=None, extras=[("halo", "gem", 9.0), ("chest_gem",)],
     paint={"face": eyes(sclera="white", iris="eye", slit=False) + brow(slant=0.3) + mouth("flat", 6.4),
            "chest": [("r", 5.6, 1.4, 6.4, 2.0, "eye", True), ("r", 5.2, 2.0, 6.8, 2.6, "eye", True), ("r", 5.6, 2.6, 6.4, 3.4, "eye", True)] + lines((((5.0, 1.0), (7.0, 1.0)), ((5.0, 3.6), (7.0, 3.6))), "eye", True)})

spec(id="megicula", source="Black Clover wiki (en), Megicula: Appearance. Petite, pale from the neck up and black below, a black star pattern at the neck, three eyes with dark patches, big curved ram horns and thin upward forehead horns, long pointy ears, long wavy light hair turning to mist, light tentacles all over, bat wings covered in eyes, long thin tail with a heart tip",
     colors=dict(skin="#e8e0ee", skin2="#14101c", hair="#e8e0ee", accent="#d82a6a", horn="#3a2a40", cloth="#14101c", membrane="#2a1c34", claw="#3a2a40", tooth="#f6f0fa", mist="#ece4f2", metal="#d82a6a"), glow="#ff3a8a", eye="#ff3a8a",
     dims=dict(hip=10.0, lt=3.4, lx=2.2, pw=8.2, aw=7.2, cw=9.6, chh=5.8, cd=5.4, hs=8.6, at=3.2, ua=6.2, fa=6.2, ah=3.8), torso_mat="skin2", arm_mat="skin2", leg_mat="skin2", pelvis_mat="skin2", belt_mat="skin2", belt_gem=False, neck_mat="skin", hand_mat="skin2",
     legs="claw", ears="long", claws=3, claw_len=2.0,
     hair=dict(cap=True, fringe=5, back=20, side=13, fringe_len=3.4, mat="mist"),
     horns=[dict(kind="ram", x=3.0, y=7.6, s=1.2, mat="horn"), dict(kind="up", x=1.4, y=8.2, z=-2.4, s=0.9)],
     wings=[dict(kind="bat", span=1.1, drop=11, y=-1.0, mem="membrane")], tail=dict(len=6.4, w=1.6, tip="heart", mat="skin2"), extras=[("third_eye",), ("tentacles", 3, "mist", "ink")],
     paint={"face": [("r", 0.9, 3.2, 3.1, 5.0, "ink"), ("r", 4.9, 3.2, 7.1, 5.0, "ink")] + eyes(y=3.8, w=1.6, sclera="white", iris="eye") + brow(slant=0.4) + [("r", 3.6, 1.8, 4.4, 2.5, "eye", True)] + mouth("flat", 6.4),
            "chest": [("l", 1.0, 0.0, 11.0, 0.0, "white"), ("l", 3.0, 0.0, 6.0, 2.4, "white"), ("l", 6.0, 2.4, 9.0, 0.0, "white"), ("l", 4.0, 1.4, 8.0, 1.4, "white")],
            "wing_mem_*|ns": dots([(0.5, 0.25), (0.5, 0.6)], "eye", 2, 2)})

spec(id="lucifugus", source="Black Clover wiki (en), Lucifugus: Appearance. Tall and slender, long pointed ears, two large ox-like horns, a pair of wings, a long prehensile tail; the palette (lavender, violet) follows the mod's Kotodama devil",
     colors=dict(skin="#e8dcf4", skin2="#c8b8e0", hair="#dcd0f4", accent="#b06aff", horn="#3a2a58", cloth="#2a1e44", cloth2="#5a3a9a", membrane="#4a3a78", claw="#3a2a58", tooth="#fffaff", metal="#b06aff"), glow="#c08aff", eye="#c08aff",
     dims=dict(hip=15.5, lt=3.2, lx=2.3, pw=8.2, aw=7.2, cw=10.2, chh=6.6, cd=5.4, hs=7.6, at=3.0, ua=8.6, fa=8.4, ah=5.0), legs="hoof", ears="long", claws=3, claw_len=3.0, outfit="robe", hair=dict(cap=True, fringe=3, back=6, side=7),
     horns=[dict(kind="bull", x=3.0, y=7.2, s=1.6)], wings=[dict(kind="bat", span=1.2, drop=12.5, y=-0.5)], tail=dict(len=8.0, w=2.2, tip="none", n=3, mat="skin"), extras=[("chest_gem",)],
     paint={"face": eyes(sclera="ink", iris="eye", w=2.0) + brow(slant=0.5) + mouth("flat", 6.4), "chest": lines((((4.0, 1.0), (6.0, 3.0)), ((8.0, 1.0), (6.0, 3.0))), "eye", True)})

# ================================================================ Black Butler
spec(id="sebastian", source="Kuroshitsuji wiki (en), Sebastian Michaelis: Appearance. Tall, handsome adult, black hair, reddish-brown eyes (glowing fuchsia, slit pupils in the demon form), pale skin, black nails, tailcoat, vest, tie with the crest, white gloves, a chained pocket watch, high-heeled boots; demon form with claws, longer teeth, dark wings with many eyes",
     colors=dict(skin="#ecdcdc", hair="#14121a", accent="#d82a9a", horn="#1a1620", cloth="#14121a", cloth2="#54505e", membrane="#1c1622", claw="#14121a", tooth="#fff8f8", metal="#c8c8d4", white="#f4f2f6", boot="#0e0c12"), glow="#ff3ab0", eye="#ff3ab0",
     dims=dict(hip=16.0, lt=3.4, lx=2.4, pw=8.6, aw=7.6, cw=11.0, chh=6.4, cd=5.6, hs=8.0, at=3.2, ua=8.0, fa=8.0, ah=5.0), legs="heel", pants="cloth", ears="pointed", claws=3, claw_len=1.5, claw_mat="claw", outfit="butler", belt_gem=False, belt_mat="cloth",
     hair=dict(cap=True, fringe=5, back=3, side=5, fringe_len=3.2), horns=[], wings=[dict(kind="feather", span=1.2, rows=3, n=6, drop=12, mem="membrane", mem2="cloth2", y=-0.5)],
     paint={"face": eyes(sclera="white", iris="eye", w=2.4, iw=1.8, h=1.2) + brow(slant=0.6) + mouth("flat", 6.3), "wing_f_1_*|ns": dots([(0.5, 0.55)], "eye", 1, 2, ring="ink")})
