"""Canon specs, part 3: Tensura (the Primordials' looks), Dororo, Berserk, Fairy Tail. See devil_canon_specs.py. 'polish' says what the polish pass (docs/devil_trial_model.md) changed on that devil."""
from devil_canon_specs import dots, eyes, brow, mouth, nose, lines, ring_eye, bars, hbars, spec  # noqa: F401

# ================================================================ Tensura: "a daemon of <colour>'s line" wears the look of that Primordial (wiki: Primordial, Diablo, Testarossa, Carrera, Ultima, Guy Crimson, Misery, Rain)
spec(id="daemon_noir", source="Tensura wiki (en), Diablo (Primordial Noir): Appearance. Black hair with red and gold streaks, golden eyes with red pupils and black sclera, princely garments that became a butler outfit with a red chain; daemon horns and claws, wings he unfolds",
     polish="gold eyes with the red pupil now readable (fringe stopped above them), thick curved daemon horns, the red chain and gold buttons painted across the coat, gold epaulets with panel lines, fanned wings, rim light on the black coat",
     colors=dict(skin="#f2dcd0", hair="#14101a", accent="#e8b830", horn="#14101a", cloth="#1a1220", cloth2="#7a1420", membrane="#241628", claw="#14101a", tooth="#fff4ec", metal="#e8b830", white="#f4f0ee", boot="#0e0a12", sclera="#14101a", ink="#0a0610"), glow="#ff3a30", eye="#e8b830",
     dims=dict(hip=16.0, lt=3.5, lx=2.5, pw=8.6, aw=7.6, cw=11.2, chh=6.4, cd=5.8, hs=8.0, at=3.3, ua=8.2, fa=8.0, ah=5.0), legs="boot", pants="cloth", ears="pointed", claws=3, claw_len=2.6, claw_mat="claw", outfit="prince", belt_gem=True,
     detail=dict(plates=["accent", "cloth2"]),
     hair=dict(cap=True, fringe=5, back=4, side=6, fringe_len=3.4, spikes=3, spike_len=3.0, spike_fan=14), horns=[dict(kind="curve", x=2.6, y=7.6, s=0.8, mat="horn")], wings=[dict(kind="bat", span=1.35, drop=13, y=-0.5, mem="membrane", rot=(0, 14, -4))], extras=[("chest_gem",)],
     paint={"face": eyes(sclera="ink", iris="eye", pupil="eye", w=2.4, iw=1.7) + [("r", 3.5, 3.9, 4.0, 5.1, "#ff3a30", True)] + brow(slant=0.6) + mouth("flat", 6.4),
            "jacket": [("r", 0.0, 0.0, 12.0, 0.8, "cloth2")] + [("r", 5.6, 0.0, 6.4, 5.8, "cloth2")] + [("r", 4.4, 2.0, 5.0, 2.6, "metal"), ("r", 7.0, 2.0, 7.6, 2.6, "metal"), ("r", 4.4, 3.8, 5.0, 4.4, "metal"), ("r", 7.0, 3.8, 7.6, 4.4, "metal")] + lines((((2.0, 1.0), (4.4, 2.3)), ((10.0, 1.0), (7.6, 2.3))), "cloth2")})

spec(id="daemon_blanc", source="Tensura wiki (en), Testarossa (Primordial Blanc): Appearance. An alluring woman with waist-long white hair and crimson eyes, a black military uniform with baggy trousers (tallest of the three Primordials Diablo recruited); daemon horns and wings",
     polish="crimson eyes bigger and visible under a short fringe, curved horns, a crimson armband and gold-white piping on the uniform front, longer white hair with strand lines, fanned wings, rim light on the black uniform",
     colors=dict(skin="#f6eaea", hair="#f4f4fc", accent="#ff4a5a", horn="#2a2630", cloth="#1a1a22", cloth2="#34343e", membrane="#2e2a3a", claw="#2a2630", tooth="#ffffff", metal="#c8c8d4", white="#f4f4fc", boot="#0e0e14", sclera="#f6eaea", ink="#0c0c12"), glow="#ff4a5a", eye="#e8283a",
     dims=dict(hip=15.0, lt=3.3, lx=2.4, pw=8.0, aw=7.0, cw=10.2, chh=6.0, cd=5.4, hs=7.8, at=3.0, ua=7.8, fa=7.6, ah=4.6), legs="boot", pants="cloth", ears="pointed", claws=3, claw_len=2.0, claw_mat="claw", outfit="military", belt_gem=False,
     detail=dict(plates=["cloth2"]),
     hair=dict(cap=True, fringe=5, back=24, side=17, fringe_len=3.4), horns=[dict(kind="curve", x=2.8, y=7.4, s=0.7)], wings=[dict(kind="bat", span=1.2, drop=12, y=-0.5, rot=(0, 14, -4))],
     paint={"face": eyes(sclera="white", iris="eye", w=2.2, iw=1.6, slit=False) + brow(slant=0.5) + mouth("flat", 6.4), "chest": [("r", 5.0, 1.2, 7.0, 2.0, "metal")],
            "jacket": [("r", 5.7, 0.0, 6.3, 6.0, "white")] + [("r", 4.2, 2.0, 4.8, 2.6, "metal"), ("r", 7.2, 2.0, 7.8, 2.6, "metal"), ("r", 4.2, 3.8, 4.8, 4.4, "metal"), ("r", 7.2, 3.8, 7.8, 4.4, "metal")] + hbars((0.5,), 0.0, 12.0, "white", 0.5),
            "sleeve_up_*|nsew": [("r", 0.0, 0.0, 4.0, 1.0, "accent")]})

spec(id="daemon_jaune", source="Tensura wiki (en), Carrera (Primordial Jaune): Appearance. A young woman with shoulder-length blonde hair and blue eyes, a black snow coat with a skull pin over a white shirt and tie, the air of a delinquent; daemon horns and wings",
     polish="blue eyes readable, the skull pin moved onto the coat where it can be seen, white shirt and a tie painted on the front, button line, a shorter spiky blonde fringe, small curved horns, fanned wings",
     colors=dict(skin="#f4d8c4", hair="#f0d030", accent="#f0d030", horn="#2a2220", cloth="#1a1a20", cloth2="#f2f2f4", membrane="#3a3418", claw="#2a2220", tooth="#ffffff", metal="#e8e8ec", white="#f2f2f4", boot="#0e0e12", sclera="#f4d8c4", ink="#0c0c10"), glow="#f0d030", eye="#3a8ae8",
     dims=dict(hip=13.5, lt=3.4, lx=2.4, pw=8.2, aw=7.2, cw=10.4, chh=6.0, cd=5.6, hs=8.2, at=3.2, ua=7.0, fa=7.0, ah=4.2), legs="boot", pants="cloth", ears="pointed", claws=3, claw_len=1.8, claw_mat="claw", outfit="coat", coat_len=9, belt_gem=False,
     detail=dict(plates=["cloth2"]),
     hair=dict(cap=True, fringe=5, back=8, side=9, fringe_len=3.2, spikes=3, spike_len=2.6, spike_fan=12), horns=[dict(kind="spike", x=2.6, y=7.8, s=1.2, mat="horn")], wings=[dict(kind="bat", span=1.0, drop=10, y=-0.5, rot=(0, 14, -4))],
     paint={"face": eyes(sclera="white", iris="eye", w=2.2, iw=1.6, slit=False) + brow(slant=0.9) + mouth("grin", 6.3),
            "jacket": [("r", 4.4, 0.0, 7.6, 5.6, "white"), ("r", 5.7, 0.2, 6.3, 4.6, "ink"), ("r", 5.5, 4.4, 6.5, 5.0, "ink")] + [("r", 8.4, 1.2, 9.6, 2.4, "white"), ("r", 8.7, 1.5, 9.0, 1.8, "ink"), ("r", 9.1, 1.5, 9.4, 1.8, "ink"), ("r", 8.8, 2.0, 9.2, 2.2, "ink")] + bars((3.9, 8.1), 0.0, 6.0, "ink", 0.4)})

spec(id="daemon_violet", source="Tensura wiki (en), Ultima (Primordial Violet): Appearance. A cute girl with middle-length purple hair in a side ponytail, purple nails, yellow eyes, a black skirted military uniform with an armband; twelve bat-like wings on her back",
     polish="twelve wing blades are now wider bat-wing fingers (they read as needles), bigger yellow eyes, purple trim and an armband on the uniform, a fuller side ponytail, rim light",
     colors=dict(skin="#f8e6e8", hair="#9a4ae0", accent="#c890ff", horn="#2a1a38", cloth="#1a1424", cloth2="#9a4ae0", membrane="#5a2a8a", claw="#9a4ae0", tooth="#ffffff", metal="#c890ff", white="#f8f0fa", boot="#0e0a14", sclera="#f8e6e8", ink="#0c0814"), glow="#c890ff", eye="#f0d030",
     dims=dict(hip=12.5, lt=3.2, lx=2.3, pw=7.8, aw=6.8, cw=9.6, chh=5.6, cd=5.2, hs=8.2, at=2.9, ua=6.6, fa=6.6, ah=4.0), legs="boot", ears="pointed", claws=3, claw_len=1.8, claw_mat="claw", outfit="military", belt_gem=False, detail=dict(plates=["cloth2"]),
     hair=dict(cap=True, fringe=5, back=3, side=6, fringe_len=3.0, tail=dict(x=3.8, y=6.0, len=4.6, w=3.4, n=3, rx0=20, rx=8, rz=10)), horns=[dict(kind="spike", x=2.6, y=7.8, s=1.0, mat="horn")],
     wings=[dict(kind="blades", n=6, span=1.0, bw=2.2, mem="membrane", arm="accent")], extras=[("pauldron_spikes", "accent")],
     paint={"face": eyes(sclera="white", iris="eye", w=2.3, iw=1.7, slit=False) + brow(slant=0.4) + mouth("grin", 6.3), "chest": [("r", 4.2, 0.4, 7.8, 1.2, "accent")],
            "jacket": [("r", 4.6, 0.0, 7.4, 0.8, "cloth2")] + bars((5.6, 6.4), 0.8, 5.4, "cloth2", 0.4)})

spec(id="daemon_rouge", source="Tensura wiki (en), Guy Crimson (Primordial Rouge): Appearance. A handsome man with wavy waist-length rouge hair, blood-red eyes with gold and silver stars and two black stripes under each, a dark open jacket over a muscular chest with layered tooth and fang necklaces, a red sash and loose dark trousers; daemon horns, wings and claws",
     polish="blood-red eyes with the gold star and stripes now visible, swept-back horns, a stronger muscle read on the chest and belly, longer wavy hair, fanned wings, jacket folds and rim light",
     colors=dict(skin="#f0ccb4", hair="#c81e2d", accent="#ff4a58", horn="#14101a", cloth="#1c1620", cloth2="#a01824", membrane="#2a1218", claw="#14101a", tooth="#f4ecd8", metal="#d8d0c0", boot="#0e0a10", sclera="#f0ccb4", ink="#14080c"), glow="#ff4a58", eye="#ff4a58",
     dims=dict(hip=15.0, lt=3.8, lx=2.6, pw=9.0, aw=8.0, cw=13.0, chh=7.0, cd=6.6, hs=8.0, at=4.6, ua=8.0, fa=7.8, ah=5.0), legs="boot", pants="cloth", ears="pointed", claws=3, claw_len=2.8, claw_mat="claw", outfit="open_jacket", belt_gem=False,
     hair=dict(cap=True, fringe=5, back=26, side=17, back_w=1.2, fringe_len=3.4), horns=[dict(kind="sweep", x=2.8, y=7.4, s=1.1, mat="horn")], wings=[dict(kind="bat", span=1.35, drop=13, y=-0.5, mem="membrane", rot=(0, 14, -4))],
     paint={"face": eyes(sclera="white", iris="eye", w=2.2, iw=1.6, slit=False) + [("p", 1.8, 4.2, "#ffe070", True), ("p", 6.2, 4.2, "#ffe070", True), ("l", 1.4, 5.2, 2.4, 6.0, "ink"), ("l", 5.6, 6.0, 6.6, 5.2, "ink"), ("l", 1.8, 5.2, 2.8, 6.0, "ink"), ("l", 5.2, 6.0, 6.2, 5.2, "ink")] + brow(slant=0.5) + mouth("flat", 6.4),
            "chest": lines((((5.0, 0.6), (5.0, 5.8)), ((7.0, 0.6), (7.0, 5.8))), "skin") + lines((((0.8, 3.0), (4.8, 3.4)), ((11.2, 3.0), (7.2, 3.4)), ((1.2, 4.8), (4.6, 5.0)), ((10.8, 4.8), (7.4, 5.0))), "skin2"), "abs": lines((((0.8, 1.4), (7.6, 1.4)), ((4.2, 0.2), (4.2, 4.0)), ((0.8, 2.8), (7.6, 2.8))), "ink")})

spec(id="daemon_vert", source="Tensura wiki (en), Misery (Primordial Vert): Appearance. A strikingly beautiful woman with long wavy green hair past the shoulders, green eyes, a deep-red maid dress and long elbow-length fingerless green gloves; daemon horns and wings",
     polish="green eyes bigger and visible, curved horns, a white apron with a trim line and a green bow painted on the dress, cloth folds in the red dress, long wavy hair, fanned wings",
     colors=dict(skin="#f8e6dc", hair="#3aa85a", accent="#3aa85a", horn="#1c2a20", cloth="#8a1a24", cloth2="#5a0e18", glove="#3aa85a", membrane="#1c3a28", claw="#1c2a20", tooth="#ffffff", metal="#c8d8c8", white="#f6f4f0", boot="#1a0a0e", sclera="#f8e6dc", ink="#0c1a10"), glow="#3aa85a", eye="#3ad870",
     dims=dict(hip=14.0, lt=3.2, lx=2.3, pw=8.0, aw=7.0, cw=10.0, chh=5.8, cd=5.4, hs=8.0, at=3.0, ua=7.4, fa=7.2, ah=4.4), legs="boot", ears="pointed", claws=3, claw_len=1.6, claw_mat="claw", outfit="maid", belt_gem=False,
     hair=dict(cap=True, fringe=5, back=14, side=11, fringe_len=3.2), horns=[dict(kind="curve", x=2.8, y=7.6, s=0.6)], wings=[dict(kind="bat", span=1.0, drop=10, y=-0.5, rot=(0, 14, -4))],
     paint={"face": eyes(sclera="white", iris="eye", w=2.2, iw=1.6, slit=False) + brow(slant=0.3) + mouth("flat", 6.4),
            "jacket": [("r", 4.6, 0.0, 8.0, 5.4, "white"), ("r", 4.6, 0.0, 5.0, 5.4, "cloth2"), ("r", 7.6, 0.0, 8.0, 5.4, "cloth2"), ("r", 5.4, 0.2, 7.2, 1.0, "accent")],
            "apron": hbars((0.5,), 0.0, 6.0, "cloth2", 0.5) + bars((0.3, 5.7), 0.0, 9.0, "cloth2", 0.5)})

spec(id="daemon_bleu", source="Tensura wiki (en), Rain (Primordial Bleu): Appearance. A cute young woman with medium-length blue hair and blue eyes, a deep-red maid dress and long elbow-length fingerless blue gloves; daemon horns and wings",
     polish="blue eyes bigger and visible, upright horns, a white apron with a trim line and a blue bow on the dress, cloth folds in the red dress, fanned wings",
     colors=dict(skin="#f8e6dc", hair="#3a6ae8", accent="#3a6ae8", horn="#1c2238", cloth="#8a1a24", cloth2="#5a0e18", glove="#3a6ae8", membrane="#1c2a58", claw="#1c2238", tooth="#ffffff", metal="#c8d0e8", white="#f6f4f0", boot="#1a0a0e", sclera="#f8e6dc", ink="#0c1020"), glow="#3a8aff", eye="#3a8aff",
     dims=dict(hip=12.5, lt=3.1, lx=2.2, pw=7.8, aw=6.8, cw=9.6, chh=5.6, cd=5.2, hs=8.4, at=2.9, ua=6.6, fa=6.6, ah=4.0), legs="boot", ears="pointed", claws=3, claw_len=1.6, claw_mat="claw", outfit="maid", belt_gem=False,
     hair=dict(cap=True, fringe=5, back=8, side=8, fringe_len=3.2), horns=[dict(kind="up", x=2.8, y=7.8, s=1.1, mat="horn")], wings=[dict(kind="bat", span=0.95, drop=9.5, y=-0.5, rot=(0, 14, -4))],
     paint={"face": eyes(sclera="white", iris="eye", w=2.3, iw=1.7, slit=False) + brow(slant=0.2) + mouth("grin", 6.3),
            "jacket": [("r", 4.6, 0.0, 8.0, 5.0, "white"), ("r", 4.6, 0.0, 5.0, 5.0, "cloth2"), ("r", 7.6, 0.0, 8.0, 5.0, "cloth2"), ("r", 5.4, 0.2, 7.2, 1.0, "accent")],
            "apron": hbars((0.5,), 0.0, 6.0, "cloth2", 0.5) + bars((0.3, 5.7), 0.0, 9.0, "cloth2", 0.5)})

# ================================================================ Dororo
spec(id="hall_demon", body="statue", source="Dororo wiki (en), 48 Demons / Hall of Hell / Twelfth Demon (Asura): the Hall of Hell is a temple of statues of the 48 demons; in the 2019 anime the Twelfth Demon's statue has three heads and six arms, resembling Brahma; its true form is a blob under the castle (not drawn). The figure is that statue come to life",
     polish="a round (octagon) flame halo instead of a flat square, carved block lines and chips on the stone, brighter eyes in the three faces, gold beads and bangles kept, glowing cracks on the chest",
     colors=dict(skin="#8a7a68", stone="#8a7a68", stone2="#6a5a4a", accent="#ffb040", flame="#ffa030", cloth="#6a2a22", cloth2="#3e1812", horn="#c8b898", tooth="#e8dcc0", ink="#1a1008", metal="#ffb040"), glow="#ffb040", eye="#ffb040",
     detail=dict(plates=["stone", "stone2"], folds=["cloth", "cloth2"]),
     sway=[("head_r", "rotation", (0, 4, 0), 0.0), ("head_l", "rotation", (0, -4, 0), 1.5), ("arm2_r", "rotation", (4, 0, 3), 0.5), ("arm2_l", "rotation", (4, 0, -3), 1.0), ("arm3_r", "rotation", (5, 0, 3), 1.2), ("arm3_l", "rotation", (5, 0, -3), 1.7)],
     paint={"face": eyes(y=3.6, sclera="ink", iris="eye", w=2.4, iw=1.8, slit=False) + brow(slant=0.8, w=2.4) + mouth("fangs", 6.3), "chest": lines((((2.0, 1.0), (5.0, 3.0)), ((10.0, 1.0), (7.0, 3.0)), ((3.0, 4.6), (9.0, 4.6))), "eye", True)})

# ================================================================ Berserk
spec(id="god_hand", source="Berserk wiki (en), God Hand and Femto/Griffith pages: the page fetched is the lead and infobox only (silver hair, blue eyes, 'Wings of Darkness'); the look is the Femto form from memory of the manga (a pale lord in white and gold, a horned crest, huge dark and white wings, a crimson cloak and the Beherit gem); the other four members are not drawn",
     polish="a crimson cloak behind the wings (the source look named it but it was missing), the Beherit painted on the breastplate (a dark red egg with an eye and a mouth that glow), white-gold armour with panel lines and rivets, gold crest horns with a base ring, red glowing eyes bigger and readable, fanned feather wings",
     colors=dict(skin="#f6eeee", hair="#f4f0f6", accent="#c81e2d", horn="#e8d070", cloth="#8a0a14", cloth2="#2a1218", white="#f4f0ee", metal="#e8c850", membrane="#f4f0f6", claw="#e8d070", tooth="#fffafa", sclera="#14080c", ink="#14080c"), glow="#ff2a3a", eye="#ff2a3a",
     dims=dict(hip=15.5, lt=3.5, lx=2.5, pw=8.8, aw=7.8, cw=11.6, chh=6.6, cd=6.0, hs=8.0, at=3.4, ua=8.2, fa=8.0, ah=4.8), legs="boot", pants="white", ears="pointed", claws=3, claw_len=1.8, claw_mat="claw", outfit="plate", belt_gem=True,
     detail=dict(plates=["white", "metal"], folds=["cloth", "cloth2", "boot"]),
     hair=dict(cap=True, fringe=5, back=20, side=11, fringe_len=3.4), horns=[dict(kind="sweep", x=3.2, y=7.4, s=1.0, mat="horn")], wings=[dict(kind="feather", span=1.5, rows=3, n=7, drop=14, mem="cloth2", mem2="membrane", y=-0.5, rot=(0, 14, -4)), dict(kind="feather", pfx="wing2", span=1.1, rows=2, n=4, drop=10, mem="membrane", mem2="cloth2", y=-4.0, rot=(0, 26, 20))],
     extras=[("crown", "metal", 5), ("cloak", "cloth", "cloth2", 0.9)], tail=None,
     sway=[("wing2_r", "rotation", (0, 4, -6), 0.8), ("wing2_l", "rotation", (0, -4, 6), 0.8)],
     paint={"face": eyes(sclera="ink", iris="eye", w=2.3, iw=1.7, slit=False) + brow(slant=0.5) + mouth("flat", 6.4),
            "jacket": [("r", 4.6, 0.8, 8.2, 5.4, "ink"), ("r", 5.0, 0.4, 7.8, 5.8, "cloth"), ("r", 5.4, 0.2, 7.4, 6.0, "cloth"), ("r", 5.0, 1.8, 6.0, 2.4, "eye", True), ("r", 6.8, 1.8, 7.8, 2.4, "eye", True), ("r", 5.4, 3.6, 7.4, 4.0, "ink"), ("r", 5.8, 3.8, 7.0, 4.0, "eye", True)] + hbars((0.4,), 0.0, 12.8, "metal", 0.6)})

# ================================================================ Fairy Tail
spec(id="etherious", source="Fairy Tail wiki (en), Etherious and Mard Geer: Appearance. Mard Geer is tall and lean with a young man's look, long wavy dark purple hair mostly tied in a very large ponytail, messy bangs hiding dark slanted eyes, a smirk; Etherious are demons made from Zeref's books with curse marks and a transformed Etherious Form (the horns and the marks are the mod's design; the form itself was not described on the pages fetched)",
     polish="glowing curse marks across the face, chest and coat, bright violet slanted eyes readable under the bangs, lighter violet trim and panel seams so the dark robe no longer blends into one block, swept horns, rim light and folds",
     colors=dict(skin="#e0d4f0", hair="#2a1a4a", accent="#8a5cff", horn="#1a1226", cloth="#241638", cloth2="#5a3a9a", claw="#1a1226", tooth="#fff8ff", metal="#8a5cff", sclera="#14081c", ink="#0e0618"), glow="#8a5cff", eye="#b080ff",
     dims=dict(hip=15.5, lt=3.3, lx=2.4, pw=8.2, aw=7.2, cw=10.6, chh=6.4, cd=5.6, hs=8.0, at=3.1, ua=8.2, fa=8.2, ah=4.8), legs="boot", pants="cloth", ears="pointed", claws=3, claw_len=3.0, claw_mat="claw", outfit="robe", belt_gem=False,
     detail=dict(plates=["cloth2"], folds=["cloth", "cloth2", "boot"]),
     hair=dict(cap=True, fringe=5, back=6, side=9, fringe_len=3.8, spikes=2, spike_len=3.0, spike_fan=14, tail=dict(len=6.0, w=4.2, n=4, mat="hair", rx0=34, rx=6, y=6.0)), horns=[dict(kind="sweep", x=3.0, y=7.0, s=0.95, mat="horn")], wings=[], tail=None, extras=[("pauldron_spikes", "accent")],
     sway=[("ptail1", "rotation", (3, 0, 3), 0.0), ("ptail2", "rotation", (4, 0, 4), 0.8), ("ptail3", "rotation", (5, 0, 5), 1.6)],
     paint={"face": eyes(y=4.0, sclera="ink", iris="eye", w=2.4, h=1.0, iw=1.7) + brow(slant=0.9, w=2.4) + mouth("grin", 6.3) + lines((((1.0, 2.0), (1.8, 3.4)), ((7.0, 2.0), (6.2, 3.4)), ((1.2, 5.4), (2.0, 6.6)), ((6.8, 5.4), (6.0, 6.6))), "eye", True),
            "chest": lines((((2.0, 1.0), (5.0, 3.0)), ((10.0, 1.0), (7.0, 3.0))), "eye", True),
            "jacket": lines((((1.4, 0.4), (3.4, 2.6)), ((3.4, 2.6), (2.4, 5.0)), ((11.2, 0.4), (9.2, 2.6)), ((9.2, 2.6), (10.2, 5.0)), ((5.8, 1.0), (6.8, 1.0)), ((6.3, 1.0), (6.3, 4.6))), "eye", True) + bars((0.7, 11.9), 0.0, 6.0, "cloth2", 0.6),
            "skirt|nsew": lines((((2.0, 1.0), (4.0, 6.0)), ((10.0, 1.0), (8.0, 6.0))), "eye", True) + bars((6.0,), 0.0, 14.0, "cloth2", 0.5)})
