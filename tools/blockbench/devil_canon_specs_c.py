"""Canon specs, part 3: Tensura (the Primordials' looks), Dororo, Berserk, Fairy Tail. See devil_canon_specs.py."""
from devil_canon_specs import dots, eyes, brow, mouth, nose, lines, ring_eye, spec  # noqa: F401

# ================================================================ Tensura: "a daemon of <colour>'s line" wears the look of that Primordial (wiki: Primordial, Diablo, Testarossa, Carrera, Ultima, Guy Crimson, Misery, Rain)
spec(id="daemon_noir", source="Tensura wiki (en), Diablo (Primordial Noir): Appearance. Black hair with red and gold streaks, golden eyes with red pupils and black sclera, princely garments that became a butler outfit with a red chain; daemon horns and claws, wings he unfolds",
     colors=dict(skin="#f2dcd0", hair="#14101a", accent="#e8b830", horn="#14101a", cloth="#1a1220", cloth2="#7a1420", membrane="#241628", claw="#14101a", tooth="#fff4ec", metal="#e8b830", white="#f4f0ee", boot="#0e0a12", sclera="#14101a"), glow="#ff3a30", eye="#e8b830",
     dims=dict(hip=16.0, lt=3.5, lx=2.5, pw=8.6, aw=7.6, cw=11.2, chh=6.4, cd=5.8, hs=8.0, at=3.3, ua=8.2, fa=8.0, ah=5.0), legs="boot", pants="cloth", ears="pointed", claws=3, claw_len=2.6, claw_mat="claw", outfit="prince", belt_gem=True,
     hair=dict(cap=True, fringe=5, back=4, side=6, fringe_len=3.4, spikes=3, spike_len=3.0), horns=[dict(kind="curve", x=2.6, y=7.6, s=0.95, mat="horn")], wings=[dict(kind="bat", span=1.35, drop=13, y=-0.5, mem="membrane")], extras=[("chest_gem",)],
     paint={"face": eyes(sclera="ink", iris="eye", pupil="eye", w=2.0, h=1.0) + [("r", 3.5, 3.9, 4.0, 4.9, "#ff3a30", True)] + brow(slant=0.6) + mouth("flat", 6.4)})

spec(id="daemon_blanc", source="Tensura wiki (en), Testarossa (Primordial Blanc): Appearance. An alluring woman with waist-long white hair and crimson eyes, a black military uniform with baggy trousers (tallest of the three Primordials Diablo recruited); daemon horns and wings",
     colors=dict(skin="#f6eaea", hair="#f4f4fc", accent="#ff4a5a", horn="#2a2630", cloth="#1a1a22", cloth2="#34343e", membrane="#2e2a3a", claw="#2a2630", tooth="#ffffff", metal="#c8c8d4", white="#f4f4fc", boot="#0e0e14", sclera="#f6eaea"), glow="#ff4a5a", eye="#e8283a",
     dims=dict(hip=15.0, lt=3.3, lx=2.4, pw=8.0, aw=7.0, cw=10.2, chh=6.0, cd=5.4, hs=7.8, at=3.0, ua=7.8, fa=7.6, ah=4.6), legs="boot", pants="cloth", ears="pointed", claws=3, claw_len=2.0, claw_mat="claw", outfit="military", belt_gem=False,
     hair=dict(cap=True, fringe=5, back=24, side=17, fringe_len=3.4), horns=[dict(kind="curve", x=2.8, y=7.4, s=0.9)], wings=[dict(kind="bat", span=1.2, drop=12, y=-0.5)],
     paint={"face": eyes(sclera="white", iris="eye", w=1.8, slit=False) + brow(slant=0.5) + mouth("flat", 6.4), "chest": [("r", 5.0, 1.2, 7.0, 2.0, "metal")]})

spec(id="daemon_jaune", source="Tensura wiki (en), Carrera (Primordial Jaune): Appearance. A young woman with shoulder-length blonde hair and blue eyes, a black snow coat with a skull pin over a white shirt and tie, the air of a delinquent; daemon horns and wings",
     colors=dict(skin="#f4d8c4", hair="#f0d030", accent="#f0d030", horn="#2a2220", cloth="#1a1a20", cloth2="#f2f2f4", membrane="#3a3418", claw="#2a2220", tooth="#ffffff", metal="#e8e8ec", white="#f2f2f4", boot="#0e0e12", sclera="#f4d8c4"), glow="#f0d030", eye="#3a8ae8",
     dims=dict(hip=13.5, lt=3.4, lx=2.4, pw=8.2, aw=7.2, cw=10.4, chh=6.0, cd=5.6, hs=8.2, at=3.2, ua=7.0, fa=7.0, ah=4.2), legs="boot", pants="cloth", ears="pointed", claws=3, claw_len=1.8, claw_mat="claw", outfit="coat", coat_len=9, belt_gem=False,
     hair=dict(cap=True, fringe=5, back=8, side=9, fringe_len=3.2, spikes=3, spike_len=2.6), horns=[dict(kind="spike", x=2.6, y=7.8, s=1.2, mat="horn")], wings=[dict(kind="bat", span=1.0, drop=10, y=-0.5)],
     paint={"face": eyes(sclera="white", iris="eye", w=1.8, slit=False) + brow(slant=0.9) + mouth("grin", 6.3), "chest": [("r", 5.4, 1.4, 6.6, 2.6, "white"), ("r", 5.6, 1.6, 5.9, 1.9, "ink"), ("r", 6.1, 1.6, 6.4, 1.9, "ink"), ("r", 5.7, 2.2, 6.3, 2.4, "ink")]})

spec(id="daemon_violet", source="Tensura wiki (en), Ultima (Primordial Violet): Appearance. A cute girl with middle-length purple hair in a side ponytail, purple nails, yellow eyes, a black skirted military uniform with an armband; twelve bat-like wings on her back",
     colors=dict(skin="#f8e6e8", hair="#9a4ae0", accent="#c890ff", horn="#2a1a38", cloth="#1a1424", cloth2="#9a4ae0", membrane="#5a2a8a", claw="#9a4ae0", tooth="#ffffff", metal="#c890ff", white="#f8f0fa", boot="#0e0a14", sclera="#f8e6e8"), glow="#c890ff", eye="#f0d030",
     dims=dict(hip=12.5, lt=3.2, lx=2.3, pw=7.8, aw=6.8, cw=9.6, chh=5.6, cd=5.2, hs=8.2, at=2.9, ua=6.6, fa=6.6, ah=4.0), legs="boot", ears="pointed", claws=3, claw_len=1.8, claw_mat="claw", outfit="military", belt_gem=False,
     hair=dict(cap=True, fringe=5, back=3, side=6, fringe_len=3.0, tail=dict(x=3.8, y=6.0, len=4.6, w=3.0, n=3, rx0=20, rx=8, rz=10)), horns=[dict(kind="spike", x=2.6, y=7.8, s=1.0, mat="horn")],
     wings=[dict(kind="blades", n=6, span=1.0, mem="membrane", arm="accent")], extras=[("pauldron_spikes", "accent")],
     paint={"face": eyes(sclera="white", iris="eye", w=1.9, slit=False) + brow(slant=0.4) + mouth("grin", 6.3), "chest": [("r", 4.2, 0.4, 7.8, 1.2, "accent")]})

spec(id="daemon_rouge", source="Tensura wiki (en), Guy Crimson (Primordial Rouge): Appearance. A handsome man with wavy waist-length rouge hair, blood-red eyes with gold and silver stars and two black stripes under each, a dark open jacket over a muscular chest with layered tooth and fang necklaces, a red sash and loose dark trousers; daemon horns, wings and claws",
     colors=dict(skin="#f0ccb4", hair="#c81e2d", accent="#ff4a58", horn="#14101a", cloth="#1c1620", cloth2="#a01824", membrane="#2a1218", claw="#14101a", tooth="#f4ecd8", metal="#d8d0c0", boot="#0e0a10", sclera="#f0ccb4"), glow="#ff4a58", eye="#ff4a58",
     dims=dict(hip=15.0, lt=3.8, lx=2.6, pw=9.0, aw=8.0, cw=13.0, chh=7.0, cd=6.6, hs=8.0, at=4.6, ua=8.0, fa=7.8, ah=5.0), legs="boot", pants="cloth", ears="pointed", claws=3, claw_len=2.8, claw_mat="claw", outfit="open_jacket", belt_gem=False,
     hair=dict(cap=True, fringe=5, back=26, side=17, back_w=1.2, fringe_len=3.4), horns=[dict(kind="sweep", x=2.8, y=7.4, s=1.1, mat="horn")], wings=[dict(kind="bat", span=1.35, drop=13, y=-0.5, mem="membrane")],
     paint={"face": eyes(sclera="white", iris="eye", w=1.8, slit=False) + [("p", 1.8, 4.0, "#ffe070", True), ("p", 6.2, 4.0, "#ffe070", True), ("l", 1.4, 5.0, 2.4, 5.8, "ink"), ("l", 5.6, 5.8, 6.6, 5.0, "ink"), ("l", 1.8, 5.0, 2.8, 5.8, "ink"), ("l", 5.2, 5.8, 6.2, 5.0, "ink")] + brow(slant=0.5) + mouth("flat", 6.4),
            "chest": lines((((5.0, 0.6), (5.0, 5.8)), ((7.0, 0.6), (7.0, 5.8))), "skin"), "abs": lines((((0.8, 1.4), (7.6, 1.4)), ((4.2, 0.2), (4.2, 4.0)), ((0.8, 2.8), (7.6, 2.8))), "ink")})

spec(id="daemon_vert", source="Tensura wiki (en), Misery (Primordial Vert): Appearance. A strikingly beautiful woman with long wavy green hair past the shoulders, green eyes, a deep-red maid dress and long elbow-length fingerless green gloves; daemon horns and wings",
     colors=dict(skin="#f8e6dc", hair="#3aa85a", accent="#3aa85a", horn="#1c2a20", cloth="#8a1a24", cloth2="#5a0e18", glove="#3aa85a", membrane="#1c3a28", claw="#1c2a20", tooth="#ffffff", metal="#c8d8c8", white="#f6f4f0", boot="#1a0a0e", sclera="#f8e6dc"), glow="#3aa85a", eye="#3ad870",
     dims=dict(hip=14.0, lt=3.2, lx=2.3, pw=8.0, aw=7.0, cw=10.0, chh=5.8, cd=5.4, hs=8.0, at=3.0, ua=7.4, fa=7.2, ah=4.4), legs="boot", ears="pointed", claws=3, claw_len=1.6, claw_mat="claw", outfit="maid", belt_gem=False,
     hair=dict(cap=True, fringe=5, back=14, side=11, fringe_len=3.2), horns=[dict(kind="curve", x=2.8, y=7.6, s=0.8)], wings=[dict(kind="bat", span=1.0, drop=10, y=-0.5)],
     paint={"face": eyes(sclera="white", iris="eye", w=1.8, slit=False) + brow(slant=0.3) + mouth("flat", 6.4)})

spec(id="daemon_bleu", source="Tensura wiki (en), Rain (Primordial Bleu): Appearance. A cute young woman with medium-length blue hair and blue eyes, a deep-red maid dress and long elbow-length fingerless blue gloves; daemon horns and wings",
     colors=dict(skin="#f8e6dc", hair="#3a6ae8", accent="#3a6ae8", horn="#1c2238", cloth="#8a1a24", cloth2="#5a0e18", glove="#3a6ae8", membrane="#1c2a58", claw="#1c2238", tooth="#ffffff", metal="#c8d0e8", white="#f6f4f0", boot="#1a0a0e", sclera="#f8e6dc"), glow="#3a8aff", eye="#3a8aff",
     dims=dict(hip=12.5, lt=3.1, lx=2.2, pw=7.8, aw=6.8, cw=9.6, chh=5.6, cd=5.2, hs=8.4, at=2.9, ua=6.6, fa=6.6, ah=4.0), legs="boot", ears="pointed", claws=3, claw_len=1.6, claw_mat="claw", outfit="maid", belt_gem=False,
     hair=dict(cap=True, fringe=5, back=8, side=8, fringe_len=3.2), horns=[dict(kind="up", x=2.8, y=7.8, s=1.0, mat="horn")], wings=[dict(kind="bat", span=0.95, drop=9.5, y=-0.5)],
     paint={"face": eyes(sclera="white", iris="eye", w=1.9, slit=False) + brow(slant=0.2) + mouth("grin", 6.3)})

# ================================================================ Dororo
spec(id="hall_demon", body="statue", source="Dororo wiki (en), 48 Demons / Hall of Hell / Twelfth Demon (Asura): the Hall of Hell is a temple of statues of the 48 demons; in the 2019 anime the Twelfth Demon's statue has three heads and six arms, resembling Brahma; its true form is a blob under the castle (not drawn). The figure is that statue come to life",
     colors=dict(skin="#8a7a68", stone="#8a7a68", stone2="#6a5a4a", accent="#ffb040", flame="#ffa030", cloth="#6a2a22", cloth2="#3e1812", horn="#c8b898", tooth="#e8dcc0", ink="#1a1008", metal="#ffb040"), glow="#ffb040", eye="#ffb040",
     sway=[("head_r", "rotation", (0, 4, 0), 0.0), ("head_l", "rotation", (0, -4, 0), 1.5), ("arm2_r", "rotation", (4, 0, 3), 0.5), ("arm2_l", "rotation", (4, 0, -3), 1.0), ("arm3_r", "rotation", (5, 0, 3), 1.2), ("arm3_l", "rotation", (5, 0, -3), 1.7)],
     paint={"face": eyes(y=3.6, sclera="ink", iris="eye", w=2.0, slit=False) + brow(slant=0.8, w=2.4) + mouth("fangs", 6.3), "chest": lines((((2.0, 1.0), (5.0, 3.0)), ((10.0, 1.0), (7.0, 3.0)), ((3.0, 4.6), (9.0, 4.6))), "eye", True)})

# ================================================================ Berserk
spec(id="god_hand", source="Berserk wiki (en), God Hand and Femto/Griffith pages: the page fetched is the lead and infobox only (silver hair, blue eyes, 'Wings of Darkness'); the look is the Femto form from memory of the manga (a pale lord in white and gold, a horned crest, huge dark and white wings, a crimson cloak and the Beherit gem); the other four members are not drawn",
     colors=dict(skin="#f6eeee", hair="#f4f0f6", accent="#c81e2d", horn="#e8d070", cloth="#8a0a14", cloth2="#2a1218", white="#f4f0ee", metal="#e8c850", membrane="#f4f0f6", claw="#e8d070", tooth="#fffafa", sclera="#14080c"), glow="#ff2a3a", eye="#ff2a3a",
     dims=dict(hip=15.5, lt=3.5, lx=2.5, pw=8.8, aw=7.8, cw=11.6, chh=6.6, cd=6.0, hs=8.0, at=3.4, ua=8.2, fa=8.0, ah=4.8), legs="boot", pants="white", ears="pointed", claws=3, claw_len=1.8, claw_mat="claw", outfit="plate", belt_gem=True,
     hair=dict(cap=True, fringe=5, back=20, side=11, fringe_len=3.4), horns=[dict(kind="sweep", x=3.2, y=7.4, s=1.0, mat="horn")], wings=[dict(kind="feather", span=1.5, rows=3, n=7, drop=14, mem="cloth2", mem2="membrane", y=-0.5), dict(kind="feather", pfx="wing2", span=1.1, rows=2, n=5, drop=10, mem="membrane", mem2="cloth2", y=-4.0, rot=(0, 26, 22))],
     extras=[("crown", "metal", 5), ("chest_gem",)], tail=None,
     paint={"face": eyes(sclera="ink", iris="eye", w=1.9, slit=False) + brow(slant=0.5) + mouth("flat", 6.4)})

# ================================================================ Fairy Tail
spec(id="etherious", source="Fairy Tail wiki (en), Etherious and Mard Geer: Appearance. Mard Geer is tall and lean with a young man's look, long wavy dark purple hair mostly tied in a very large ponytail, messy bangs hiding dark slanted eyes, a smirk; Etherious are demons made from Zeref's books with curse marks and a transformed Etherious Form (the horns and the marks are the mod's design; the form itself was not described on the pages fetched)",
     colors=dict(skin="#e0d4f0", hair="#2a1a4a", accent="#8a5cff", horn="#1a1226", cloth="#241638", cloth2="#4a2a7a", claw="#1a1226", tooth="#fff8ff", metal="#8a5cff", sclera="#14081c"), glow="#8a5cff", eye="#a070ff",
     dims=dict(hip=15.5, lt=3.3, lx=2.4, pw=8.2, aw=7.2, cw=10.6, chh=6.4, cd=5.6, hs=8.0, at=3.1, ua=8.2, fa=8.2, ah=4.8), legs="boot", pants="cloth", ears="pointed", claws=3, claw_len=3.0, claw_mat="claw", outfit="robe", belt_gem=False,
     hair=dict(cap=True, fringe=5, back=6, side=9, fringe_len=3.8, spikes=2, spike_len=3.0, tail=dict(len=6.0, w=4.2, n=4, mat="hair", rx0=34, rx=6, y=6.0)), horns=[dict(kind="sweep", x=3.0, y=7.0, s=0.9, mat="horn")], wings=[], tail=None, extras=[("pauldron_spikes", "accent")],
     sway=[("ptail1", "rotation", (3, 0, 3), 0.0), ("ptail2", "rotation", (4, 0, 4), 0.8), ("ptail3", "rotation", (5, 0, 5), 1.6)],
     paint={"face": eyes(y=4.0, sclera="ink", iris="eye", w=2.0, h=0.8) + brow(slant=0.9, w=2.4) + mouth("grin", 6.3) + lines((((1.0, 2.0), (1.8, 3.4)), ((7.0, 2.0), (6.2, 3.4))), "eye", True),
            "chest": lines((((2.0, 1.0), (5.0, 3.0)), ((10.0, 1.0), (7.0, 3.0))), "eye", True)})
