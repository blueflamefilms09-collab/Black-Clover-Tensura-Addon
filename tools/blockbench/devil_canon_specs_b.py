"""Canon specs, part 2: Chainsaw Man (custom bodies) and Blue Exorcist. See devil_canon_specs.py. 'polish' says what the polish pass (docs/devil_trial_model.md) changed on that devil."""
from devil_canon_specs import dots, eyes, brow, mouth, nose, lines, ring_eye, bars, hbars, spec  # noqa: F401

# ================================================================ Chainsaw Man (custom bodies)
spec(id="fox_devil", body="fox", source="Chainsaw Man wiki (en), Fox Devil: Appearance. An oversized arctic fox: a giant fox head covered in many giant eyes of concentric circles, a monstrously large arm with oversized claws covered in eyes; orange or light brown fur in the manga, white in the anime (the manga colouring is used), red-orange glowing eyes",
     polish="bigger upright ears with dark insides and pale rims, a white fur ruff round the neck, fur spines down the back, a fuller bushy tail, longer claws on the giant arm, ring-eyes also on the chest, fur grain and rim light on the second texture pass",
     colors=dict(skin="#e08a2a", hair="#e08a2a", fur="#e08a2a", fur2="#b86a20", white="#f6efe0", claw="#f2e8d0", horn="#f2e8d0", accent="#ff6a30", ink="#2a1208", tooth="#fffaf0", cloth="#e08a2a"), glow="#ff4a20", eye="#ff4a20",
     sway=[("tail1", "rotation", (0, 8, 0), 0.0), ("tail2", "rotation", (0, 10, 0), -0.7), ("tail3", "rotation", (0, 12, 0), -1.4)],
     paint={"face": ring_eye(2.0, 3.6, 1.1, ["white", ("eye", True), "ink", ("eye", True)]) + ring_eye(6.0, 3.6, 1.1, ["white", ("eye", True), "ink", ("eye", True)]) + ring_eye(1.0, 1.8, 0.55, ["white", ("eye", True), "ink"]) + ring_eye(7.0, 1.8, 0.55, ["white", ("eye", True), "ink"])
            + ring_eye(4.0, 1.4, 0.6, ["white", ("eye", True), "ink"]) + ring_eye(0.9, 5.8, 0.5, ["white", ("eye", True), "ink"]) + ring_eye(7.1, 5.8, 0.5, ["white", ("eye", True), "ink"]),
            "chest": ring_eye(2.0, 3.2, 0.9, ["white", ("eye", True), "ink"]) + ring_eye(10.0, 3.2, 0.9, ["white", ("eye", True), "ink"]) + ring_eye(6.0, 1.6, 0.6, ["white", ("eye", True), "ink"]),
            "eyes_arm|nsew": dots([(0.3, 0.2), (0.7, 0.45), (0.35, 0.7), (0.65, 0.88)], "eye", 2, 2),
            "jaw": lines((((0.4, 0.5), (5.2, 0.5)),))})

spec(id="curse_devil", body="curse", source="Chainsaw Man wiki (en), Curse Devil: Appearance. A large two-headed skeleton with horns; the right skull is human-like, the left has three eye slits and longer mandible-like teeth; clawed hands and a large distended ribcage",
     polish="larger skulls (the face was a small mask on a big body), dark rotten lining behind the ribs so the bones read against it, shoulder spikes and a spine of thorns, sharper contrast and bone grain on the second texture pass",
     colors=dict(skin="#c8c0b0", bone="#d8d0c4", hair="#2a1a3a", accent="#c070ff", horn="#5a4a58", cloth="#2a1a3a", claw="#4a3858", tooth="#f4ecdc", ink="#14081c"), glow="#c070ff", eye="#c070ff",
     sway=[("head", "rotation", (2, 5, 1), 0.0), ("head2", "rotation", (2, -5, -1), 1.6), ("tail1", "rotation", (0, 7, 0), 0), ("tail2", "rotation", (0, 9, 0), -0.7), ("tail3", "rotation", (0, 12, 0), -1.4)],
     paint={"face_a": [("r", 1.0, 3.0, 3.2, 5.2, "ink"), ("r", 4.8, 3.0, 7.0, 5.2, "ink"), ("r", 1.6, 3.7, 2.8, 4.6, "eye", True), ("r", 5.2, 3.7, 6.4, 4.6, "eye", True), ("r", 3.4, 5.4, 4.6, 6.4, "ink"), ("l", 1.8, 7.2, 6.2, 7.2, "ink"), ("l", 2.6, 6.6, 2.6, 7.6, "ink"), ("l", 3.8, 6.6, 3.8, 7.6, "ink"), ("l", 5.0, 6.6, 5.0, 7.6, "ink")],
            "face_b": [("r", 0.6, 2.0, 7.4, 2.8, "ink"), ("r", 0.6, 3.4, 7.4, 4.2, "ink"), ("r", 0.6, 4.8, 7.4, 5.6, "ink"), ("r", 1.4, 2.1, 6.6, 2.7, "eye", True), ("r", 1.4, 3.5, 6.6, 4.1, "eye", True), ("r", 1.4, 4.9, 6.6, 5.5, "eye", True), ("l", 0.8, 7.2, 7.2, 7.2, "ink")]})

spec(id="future_devil", body="tree", source="Chainsaw Man wiki (en), Future Devil: Appearance. The lower half is a tree growing from the floor, a trunk and branches up to a humanoid body with outstretched arms and a horned head; vine-like streamers grow from the arms and chest; the face has three pairs of eyes stacked vertically and a wide, always open mouth; a large hole in the chest with an oversized eye inside",
     polish="leaf clusters on the horns, branches and forearms and moss on the shoulders so it reads as a living tree, a flared root base, brighter cyan eyes and bark grain with rim light on the second texture pass",
     colors=dict(skin="#8a6440", wood="#7a5638", wood2="#4a3220", leaf="#3a9a6a", accent="#3ad8d8", horn="#4a3220", ink="#120a06", tooth="#e8dcc0", white="#e8f4f0"), glow="#3ad8d8", eye="#3ad8d8",
     sway=[("arm_r", "rotation", (0, 0, 5), 0), ("arm_l", "rotation", (0, 0, -5), 0.5), ("stream_0", "rotation", (5, 0, 0), 0.2), ("stream_1", "rotation", (5, 0, 0), 1.0), ("stream_2", "rotation", (5, 0, 0), 1.8)],
     paint={"face": [("r", 1.8, 1.0, 3.2, 1.9, "ink"), ("r", 4.8, 1.0, 6.2, 1.9, "ink"), ("r", 2.1, 1.0, 2.9, 1.9, "eye", True), ("r", 5.1, 1.0, 5.9, 1.9, "eye", True),
                     ("r", 1.4, 2.5, 3.2, 3.3, "ink"), ("r", 4.8, 2.5, 6.6, 3.3, "ink"), ("r", 1.8, 2.5, 2.8, 3.3, "eye", True), ("r", 5.2, 2.5, 6.2, 3.3, "eye", True),
                     ("r", 1.0, 3.9, 3.2, 4.7, "ink"), ("r", 4.8, 3.9, 7.0, 4.7, "ink"), ("r", 1.5, 3.9, 2.7, 4.7, "eye", True), ("r", 5.3, 3.9, 6.5, 4.7, "eye", True), ("r", 1.8, 5.8, 6.2, 7.8, "ink"), ("r", 2.4, 5.8, 2.9, 6.4, "tooth"), ("r", 5.1, 5.8, 5.6, 6.4, "tooth")],
            "big_eye": [("r", 0.0, 0.0, 4.8, 3.6, "white"), ("r", 1.0, 0.6, 3.8, 3.0, "eye", True), ("r", 2.1, 0.6, 2.7, 3.0, "ink")],
            "chest": lines((((1.0, 1.0), (4.0, 3.0)), ((11.0, 1.0), (8.0, 3.0))), "eye", True)})

spec(id="ghost_devil", body="flower", source="Chainsaw Man wiki (en), Ghost Devil: Appearance. A large devil with a cylindrical body covered in white flowers (pink outside, blue inside in the anime) with several arms instead of legs; grey skin; an old, desiccated head with long dark wavy hair and sunken cheeks; eyes and mouth sewn shut",
     polish="every flower is now a six-petal star on one centre (it was an off-centre cross that read as pink squares), long wavy locks fall in front of the shoulders, softer grey skin with blotches; this is the one model above 882 quads (the flowers cost 32 cubes) and stays inside +25 percent of its old budget",
     colors=dict(skin="#9a98a8", skin2="#7a788a", hair="#1a1620", flower="#f2a6c8", flower2="#7aa8f0", accent="#8aa8e8", ink="#1a1420", cloth="#2a2630"), glow="#8aa8e8", eye="#8aa8e8",
     sway=[("limb01", "rotation", (4, 0, 3), 0), ("limb11", "rotation", (4, 0, -3), 1.0), ("limb21", "rotation", (4, 0, 2), 2.0), ("limb31", "rotation", (4, 0, -2), 3.0), ("lock_r", "rotation", (2, 0, 2), 0), ("lock_l", "rotation", (2, 0, -2), 1)],
     paint={"stitched_face": [("l", 1.4, 3.6, 3.2, 3.6, "ink"), ("l", 4.8, 3.6, 6.6, 3.6, "ink")] + [("r", x, 3.2, x + 0.25, 4.1, "ink") for x in (1.6, 2.2, 2.8, 5.0, 5.6, 6.2)] + mouth("sewn", 6.2) + lines((((2.0, 5.0), (3.0, 5.6)), ((6.0, 5.0), (5.0, 5.6))), "ink") + lines((((0.6, 2.6), (1.4, 3.2)), ((7.4, 2.6), (6.6, 3.2))), "skin2"),
            "heart_*|nsewu": [("r", 0, 0, 9, 9, "flower2", True)], "stitch": [("l", 0.0, 3.0, 8.0, 3.0, "ink")] + [("r", x, 2.4, x + 0.3, 3.6, "ink") for x in (0.8, 2.0, 3.2, 4.4, 5.6, 6.8)]})

# ================================================================ Blue Exorcist (the kin are the mod's own designs in each king's look)
spec(id="kin_of_iblis", source="Blue Exorcist wiki (en), Iblis: Appearance (host and demon form). Dark skin, yellow eyes, a body that exudes flames, a third eye in the forehead early on; the demon form has a serpentine tail of molten rock, flame hair with the ponytail in bulbs, a belly dancer's clothes. The KIN is the mod's own design in that look",
     polish="glowing lava cracks along the molten serpent body, gold arm rings and a gold collar, rock scales on the serpent, bigger yellow eyes under the flame hair, the flame hair stands taller",
     colors=dict(skin="#8a5233", skin2="#5a3022", hair="#ff6a2a", accent="#ff6a2a", flame="#ff7a30", cloth="#c8481a", cloth2="#7a2a14", horn="#2a1a14", claw="#2a1a14", stone="#6a3a24", tooth="#fff0d0", metal="#ffd060", ink="#1c0e08"), glow="#ff8a30", eye="#ffd030",
     dims=dict(hip=11.0, lt=4.0, lx=2.6, pw=9.0, aw=7.8, cw=10.4, chh=6.0, cd=6.0, hs=8.0, at=3.6, ua=7.0, fa=7.0, ah=4.0), legs="serpent", serpent_mat="skin2", serpent_mats=["skin2", "stone", "stone", "skin2", "stone"], ears="pointed", claws=3,
     outfit="belly", belt_gem=False, detail=dict(scales=["skin2", "stone"]), hair=dict(cap=True, fringe=0, mat="flame", spikes=7, spike_len=6.2, spike_fan=12, back=0, tail=dict(len=3.6, w=3.4, n=3, mat="flame", rx0=38, rx=-6), tuft_mat="flame", tufts=[(-5.4, 6.0, 4.0, 40), (3.6, 6.0, 4.0, -40)]),
     horns=[], wings=[], tail=None, extras=[("third_eye",), ("chest_gem",)],
     sway=[("tail1", "rotation", (0, 6, 0), 0), ("tail2", "rotation", (0, 8, 0), -0.7), ("tail3", "rotation", (0, 10, 0), -1.4), ("tail4", "rotation", (0, 12, 0), -2.1), ("ptail1", "rotation", (4, 0, 3), 0.3), ("ptail2", "rotation", (5, 0, 4), 1.0)],
     paint={"face": eyes(sclera="ink", iris="eye", w=2.4, iw=1.8) + brow(slant=0.4) + mouth("grin", 6.2), "chest": lines((((2.0, 1.0), (4.0, 3.0)), ((10.0, 1.0), (8.0, 3.0)), ((3.0, 4.0), (9.0, 4.0))), "eye", True),
            "tail*_c|nsew": [("t", 0.5, 0.3, 1, 3, "flame", True), ("t", 0.3, 0.7, 3, 1, "flame", True), ("t", 0.75, 0.55, 2, 1, "flame", True)], "upper_*|nsew": hbars((1.2,), 0.0, 4.0, "metal", 0.9), "fore_*|nsew": hbars((5.2,), 0.0, 4.0, "metal", 0.9)})

spec(id="kin_of_egyn", source="Blue Exorcist wiki (en), Egyn: Appearance (hosts). Light-blue hair darker toward the tips, a long teardrop-shaped horn at the top of the head, brown skin, gold octopus-like eyes, a small body (about half the height of Lucifer); the legs as tentacles of water are the mod's own design",
     polish="a taller teardrop horn with a base ring, bigger gold octopus eyes with a horizontal pupil, gold bracelets and a gold waist band, shimmering water tentacles with ripple lines, hair rim light",
     colors=dict(skin="#aa8466", skin2="#7a5a44", hair="#7ab8f0", accent="#7ab8f0", water="#5a9ae0", cloth="#2a5a9a", cloth2="#1a3a6a", horn="#e8f4ff", claw="#e8f4ff", tooth="#f4fbff", metal="#ffd060", ink="#1a1218"), glow="#7ac8ff", eye="#ffd060",
     dims=dict(hip=9.0, lt=3.4, lx=2.2, pw=8.0, aw=7.0, cw=9.4, chh=5.6, cd=5.4, hs=8.8, at=3.0, ua=6.0, fa=6.0, ah=3.6), legs="tentacles", ears="pointed", claws=3, claw_len=2.0, outfit="belly", detail=dict(plates=["cloth2"]),
     hair=dict(cap=True, fringe=5, back=14, side=10, fringe_len=3.4), horns=[dict(kind="tear", single=True, y=8.2, z=-0.6, s=1.8)], wings=[], tail=None,
     sway=[("leg_r", "rotation", (4, 0, 3), 0), ("leg_l", "rotation", (4, 0, -3), 1.5)],
     paint={"face": eyes(sclera="eye", iris="ink", pupil="ink", w=2.6, h=1.4, slit=False, glow=False, lid=True) + [("r", 1.9, 4.2, 2.7, 4.8, "eye", True), ("r", 5.3, 4.2, 6.1, 4.8, "eye", True), ("r", 1.0, 4.4, 3.0, 4.8, "ink"), ("r", 5.0, 4.4, 7.0, 4.8, "ink")] + [("l", 2.2, 5.8, 3.2, 6.1, "ink"), ("l", 3.2, 6.1, 4.8, 6.1, "ink"), ("l", 4.8, 6.1, 5.8, 5.8, "ink")],
            "fore_*|nsew": hbars((4.8,), 0.0, 3.2, "metal", 0.9), "hip_scarf": hbars((0.4,), 0.0, 10.0, "metal", 0.8), "leg_*_c|nsew": hbars((1.4, 3.4), 0.0, 4.0, "ink", 0.35)})

spec(id="kin_of_amaimon", source="Blue Exorcist wiki (en), Amaimon: Appearance (hosts). Green hair, a small pointed horn on top of the head, golden-green slit eyes with a red inner ring, long sharp nails; the hulking, rock-plated body of the KIN is the mod's own design",
     polish="rock plates with panel lines and rivets on the chest and hips, a row of rock spines down the back, bigger eyes with the red inner ring, rim light and stone grain, green hair spikes fanned less so they read as hair",
     colors=dict(skin="#c8a890", skin2="#8a7058", hair="#3aa85a", accent="#a8d030", horn="#8a7458", cloth="#5a4a38", cloth2="#3a2e22", white="#7a6a52", metal="#c8a050", claw="#2a2218", tooth="#f4ecd8", ink="#1a140c"), glow="#a8d030", eye="#a8d030",
     dims=dict(hip=11.5, lt=5.2, lx=3.4, pw=12.4, aw=11.0, cw=16.4, chh=8.0, cd=8.4, hs=8.6, at=5.8, ua=7.6, fa=7.4, ah=5.0, hand=4.0), legs="boot", pants="cloth", ears="pointed", claws=4, claw_len=5.0, claw_mat="claw", outfit="plate", belt_gem=False,
     detail=dict(plates=["white", "cloth"], folds=["boot"]),
     hair=dict(cap=True, fringe=0, spikes=7, spike_len=5.4, spike_fan=11, spike_back=-8, back=3, side=0), horns=[dict(kind="spike", single=True, y=8.2, z=-1.0, s=1.4)], wings=[], tail=None, extras=[("pauldron_spikes", "horn"), ("back_spikes", "horn", 4)],
     paint={"face": eyes(sclera="ink", iris="eye", w=2.4, iw=1.7) + [("r", 1.6, 3.7, 2.2, 4.3, "accent"), ("r", 5.8, 3.7, 6.4, 4.3, "accent")] + brow(slant=0.6, w=2.4) + mouth("fangs", 6.3), "chest": lines((((2.0, 3.0), (5.0, 2.0)), ((10.0, 3.0), (7.0, 2.0))), "eye", True),
            "jacket": lines((((2.0, 2.0), (5.0, 4.4)), ((10.6, 2.0), (7.6, 4.4))), "ink") + [("r", 5.9, 0.6, 6.7, 5.0, "ink")]})

spec(id="kin_of_astaroth", source="Blue Exorcist wiki (en), Astaroth: Appearance (hosts). Ram horns, longer sharper black nails, canine-like teeth, red bloodshot eyes, a demonic tail, a longer tongue, demonic mushrooms (one with four eyes) grown over the body; the KIN is the mod's own design in that look",
     polish="a long tongue hanging from the mouth, bigger bloodshot eyes with red veins, mottled rotten skin on the second texture pass, thicker ram horns, bandaged forearms with stitch lines",
     colors=dict(skin="#aab888", skin2="#7a8a5a", hair="#5a6a2a", accent="#b04a4a", horn="#3a3a28", cloth="#3a4a2a", cloth2="#2a3a1a", rot="#7a8a3a", claw="#14180e", tooth="#f4f0d8", sclera="#d8c8c0", metal="#9ad03a", ink="#101408"), glow="#9ad03a", eye="#e83a3a",
     dims=dict(hip=13.5, lt=3.6, lx=2.5, pw=8.6, aw=7.6, cw=11.0, chh=6.0, cd=5.8, hs=8.0, at=3.4, ua=7.6, fa=7.6, ah=4.4), legs="claw", ears="pointed", claws=3, claw_len=4.0, claw_mat="claw", outfit="bandage", belt_gem=False,
     detail=dict(folds=["cloth"]),
     hair=dict(cap=True, fringe=3, spikes=5, spike_len=3.6, spike_fan=10, back=2, side=4), horns=[dict(kind="ram", x=3.0, y=7.0, s=1.2)], wings=[], tail=dict(len=6.0, w=2.2, tip="spade", n=3, mat="skin"), extras=[("mushrooms",), ("tongue", "accent")],
     sway=[("tail1", "rotation", (0, 6, 0), 0)],
     paint={"face": [("r", 0.8, 3.0, 3.2, 5.1, "ink"), ("r", 4.8, 3.0, 7.2, 5.1, "ink")] + eyes(sclera="sclera", iris="eye", w=2.2, slit=False, lid=False) + [("l", 0.9, 3.2, 2.0, 3.8, "eye"), ("l", 7.1, 3.2, 6.0, 3.8, "eye"), ("l", 2.4, 4.0, 2.9, 4.8, "eye"), ("l", 5.6, 4.0, 5.1, 4.8, "eye")] + brow(slant=0.7) + mouth("grin", 6.4),
            "wrap_*|nsew": hbars((1.0, 2.6, 4.2), 0.0, 4.0, "ink", 0.35)})

spec(id="kin_of_lucifer", source="Blue Exorcist wiki (en), Lucifer: Appearance (host). A tall, fair-skinned man with spiky slicked-back blond hair, a long blond tail, pointed ears, elongated canines, patches of diseased skin; the halo and white-gold coat are the mod's own design for the King of Light",
     polish="a round gold halo (an octagon) instead of a square, gold embroidery down the coat front, cuffs and hem, panel seams in the long coat, bigger eyes, hair spikes swept back, cloth folds",
     colors=dict(skin="#f8e4cc", skin2="#d8bca0", hair="#f4dc7a", accent="#fff0a8", horn="#fff0a8", cloth="#f4efe6", cloth2="#c8a850", white="#fffaf0", metal="#ffe070", claw="#e8d4b0", tooth="#fffaf0", ink="#2a1a10"), glow="#fff0a8", eye="#ffd040",
     dims=dict(hip=15.0, lt=3.6, lx=2.5, pw=8.8, aw=7.8, cw=11.4, chh=6.6, cd=6.0, hs=8.0, at=3.4, ua=8.0, fa=7.8, ah=4.8), legs="boot", pants="cloth", ears="pointed", claws=3, claw_len=2.0, outfit="coat", coat_len=14, belt_gem=False,
     detail=dict(plates=["cloth2"]),
     hair=dict(cap=True, fringe=0, spikes=8, spike_len=5.0, spike_fan=9, spike_back=-30, back=3, side=0), horns=[], wings=[], tail=dict(len=7.4, w=2.2, tip="none", n=3, mat="hair"), extras=[("halo", "gem", 8.0), ("chest_gem",)],
     sway=[("tail1", "rotation", (0, 7, 0), 0), ("tail2", "rotation", (0, 9, 0), -0.8)],
     paint={"face": eyes(sclera="white", iris="eye", w=2.2) + brow(slant=0.5) + mouth("fangs", 6.3) + [("r", 6.0, 5.0, 7.0, 6.0, "skin2")], "chest": [("r", 2.0, 1.0, 3.6, 2.2, "skin2"), ("r", 8.0, 2.4, 10.0, 3.8, "skin2")], "abs": [("r", 1.0, 1.0, 2.8, 2.6, "skin2"), ("r", 5.6, 1.4, 7.4, 3.0, "skin2")],
            "jacket": bars((1.2, 11.6), 0.0, 5.8, "metal", 0.6) + [("r", 5.6, 0.0, 6.6, 5.8, "metal")] + hbars((5.4,), 0.0, 12.0, "metal", 0.6),
            "skirt|nsew": [("r", 3.6, 0.0, 4.0, 15.0, "metal"), ("r", 6.4, 0.0, 6.8, 15.0, "metal")] + hbars((13.6,), 0.0, 10.0, "metal", 0.8)})
