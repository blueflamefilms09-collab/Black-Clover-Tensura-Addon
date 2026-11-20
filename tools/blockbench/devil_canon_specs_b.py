"""Canon specs, part 2: Chainsaw Man (custom bodies) and Blue Exorcist. See devil_canon_specs.py."""
from devil_canon_specs import dots, eyes, brow, mouth, nose, lines, ring_eye, spec  # noqa: F401

# ================================================================ Chainsaw Man (custom bodies)
spec(id="fox_devil", body="fox", source="Chainsaw Man wiki (en), Fox Devil: Appearance. An oversized arctic fox: a giant fox head covered in many giant eyes of concentric circles, a monstrously large arm with oversized claws covered in eyes; orange or light brown fur in the manga, white in the anime (the manga colouring is used), red-orange glowing eyes",
     colors=dict(skin="#e08a2a", hair="#e08a2a", fur="#e08a2a", fur2="#b86a20", white="#f6efe0", claw="#f2e8d0", horn="#f2e8d0", accent="#ff6a30", ink="#2a1208", tooth="#fffaf0", cloth="#e08a2a"), glow="#ff4a20", eye="#ff4a20",
     sway=[("tail1", "rotation", (0, 8, 0), 0.0), ("tail2", "rotation", (0, 10, 0), -0.7), ("tail3", "rotation", (0, 12, 0), -1.4)],
     paint={"face": ring_eye(2.0, 3.6, 1.1, ["white", ("eye", True), "ink", ("eye", True)]) + ring_eye(6.0, 3.6, 1.1, ["white", ("eye", True), "ink", ("eye", True)]) + ring_eye(1.0, 1.8, 0.55, ["white", ("eye", True), "ink"]) + ring_eye(7.0, 1.8, 0.55, ["white", ("eye", True), "ink"])
            + ring_eye(4.0, 1.4, 0.6, ["white", ("eye", True), "ink"]) + ring_eye(0.9, 5.8, 0.5, ["white", ("eye", True), "ink"]) + ring_eye(7.1, 5.8, 0.5, ["white", ("eye", True), "ink"]),
            "eyes_arm|nsew": dots([(0.3, 0.2), (0.7, 0.45), (0.35, 0.7), (0.65, 0.88)], "eye", 2, 2),
            "jaw": lines((((0.4, 0.5), (5.2, 0.5)),))})

spec(id="curse_devil", body="curse", source="Chainsaw Man wiki (en), Curse Devil: Appearance. A large two-headed skeleton with horns; the right skull is human-like, the left has three eye slits and longer mandible-like teeth; clawed hands and a large distended ribcage",
     colors=dict(skin="#c8c0b0", bone="#d8d0c4", hair="#2a1a3a", accent="#c070ff", horn="#5a4a58", cloth="#2a1a3a", claw="#4a3858", tooth="#f4ecdc", ink="#14081c"), glow="#c070ff", eye="#c070ff",
     sway=[("head", "rotation", (2, 5, 1), 0.0), ("head2", "rotation", (2, -5, -1), 1.6), ("tail1", "rotation", (0, 7, 0), 0), ("tail2", "rotation", (0, 9, 0), -0.7), ("tail3", "rotation", (0, 12, 0), -1.4)],
     paint={"face_a": [("r", 1.2, 3.0, 3.2, 5.0, "ink"), ("r", 4.8, 3.0, 6.8, 5.0, "ink"), ("r", 1.8, 3.8, 2.6, 4.4, "eye", True), ("r", 5.4, 3.8, 6.2, 4.4, "eye", True), ("r", 3.4, 5.2, 4.6, 6.2, "ink"), ("l", 2.0, 7.0, 6.0, 7.0, "ink")],
            "face_b": [("r", 0.8, 2.0, 7.2, 2.6, "ink"), ("r", 0.8, 3.4, 7.2, 4.0, "ink"), ("r", 0.8, 4.8, 7.2, 5.4, "ink"), ("r", 1.6, 2.1, 6.4, 2.5, "eye", True), ("r", 1.6, 3.5, 6.4, 3.9, "eye", True), ("r", 1.6, 4.9, 6.4, 5.3, "eye", True), ("l", 1.0, 7.0, 7.0, 7.0, "ink")]})

spec(id="future_devil", body="tree", source="Chainsaw Man wiki (en), Future Devil: Appearance. The lower half is a tree growing from the floor, a trunk and branches up to a humanoid body with outstretched arms and a horned head; vine-like streamers grow from the arms and chest; the face has three pairs of eyes stacked vertically and a wide, always open mouth; a large hole in the chest with an oversized eye inside",
     colors=dict(skin="#8a6440", wood="#7a5638", wood2="#4a3220", leaf="#3a9a6a", accent="#3ad8d8", horn="#4a3220", ink="#120a06", tooth="#e8dcc0", white="#e8f4f0"), glow="#3ad8d8", eye="#3ad8d8",
     sway=[("arm_r", "rotation", (0, 0, 5), 0), ("arm_l", "rotation", (0, 0, -5), 0.5), ("stream_0", "rotation", (5, 0, 0), 0.2), ("stream_1", "rotation", (5, 0, 0), 1.0), ("stream_2", "rotation", (5, 0, 0), 1.8)],
     paint={"face": [("r", 2.0, 1.2, 3.0, 1.8, "ink"), ("r", 5.0, 1.2, 6.0, 1.8, "ink"), ("r", 2.2, 1.2, 2.8, 1.8, "eye", True), ("r", 5.2, 1.2, 5.8, 1.8, "eye", True),
                     ("r", 1.6, 2.6, 3.0, 3.2, "ink"), ("r", 5.0, 2.6, 6.4, 3.2, "ink"), ("r", 1.9, 2.6, 2.7, 3.2, "eye", True), ("r", 5.3, 2.6, 6.1, 3.2, "eye", True),
                     ("r", 1.2, 4.0, 3.0, 4.6, "ink"), ("r", 5.0, 4.0, 6.8, 4.6, "ink"), ("r", 1.6, 4.0, 2.6, 4.6, "eye", True), ("r", 5.4, 4.0, 6.4, 4.6, "eye", True), ("r", 1.8, 5.8, 6.2, 7.8, "ink"), ("r", 2.4, 5.8, 2.9, 6.4, "tooth"), ("r", 5.1, 5.8, 5.6, 6.4, "tooth")],
            "big_eye": [("r", 0.0, 0.0, 4.8, 3.6, "white"), ("r", 1.0, 0.6, 3.8, 3.0, "eye", True), ("r", 2.1, 0.6, 2.7, 3.0, "ink")],
            "chest": lines((((1.0, 1.0), (4.0, 3.0)), ((11.0, 1.0), (8.0, 3.0))), "eye", True)})

spec(id="ghost_devil", body="flower", source="Chainsaw Man wiki (en), Ghost Devil: Appearance. A large devil with a cylindrical body covered in white flowers (pink outside, blue inside in the anime) with several arms instead of legs; grey skin; an old, desiccated head with long dark wavy hair and sunken cheeks; eyes and mouth sewn shut",
     colors=dict(skin="#9a98a8", skin2="#7a788a", hair="#1a1620", flower="#f2a6c8", flower2="#7aa8f0", accent="#8aa8e8", ink="#1a1420", cloth="#2a2630"), glow="#8aa8e8", eye="#8aa8e8",
     sway=[("limb01", "rotation", (4, 0, 3), 0), ("limb11", "rotation", (4, 0, -3), 1.0), ("limb21", "rotation", (4, 0, 2), 2.0), ("limb31", "rotation", (4, 0, -2), 3.0), ("lock_r", "rotation", (2, 0, 2), 0), ("lock_l", "rotation", (2, 0, -2), 1)],
     paint={"stitched_face": [("l", 1.4, 3.6, 3.2, 3.6, "ink"), ("l", 4.8, 3.6, 6.6, 3.6, "ink")] + [("r", x, 3.2, x + 0.25, 4.1, "ink") for x in (1.6, 2.2, 2.8, 5.0, 5.6, 6.2)] + mouth("sewn", 6.2) + lines((((2.0, 5.0), (3.0, 5.6)), ((6.0, 5.0), (5.0, 5.6))), "ink"),
            "heart_*|nsewu": [("r", 0, 0, 9, 9, "flower2", True)], "stitch": [("l", 0.0, 3.0, 8.0, 3.0, "ink")] + [("r", x, 2.4, x + 0.3, 3.6, "ink") for x in (0.8, 2.0, 3.2, 4.4, 5.6, 6.8)]})

# ================================================================ Blue Exorcist (the kin are the mod's own designs in each king's look)
spec(id="kin_of_iblis", source="Blue Exorcist wiki (en), Iblis: Appearance (host and demon form). Dark skin, yellow eyes, a body that exudes flames, a third eye in the forehead early on; the demon form has a serpentine tail of molten rock, flame hair with the ponytail in bulbs, a belly dancer's clothes. The KIN is the mod's own design in that look",
     colors=dict(skin="#8a5233", skin2="#5a3022", hair="#ff6a2a", accent="#ff6a2a", flame="#ff7a30", cloth="#c8481a", cloth2="#7a2a14", horn="#2a1a14", claw="#2a1a14", stone="#6a3a24", tooth="#fff0d0", metal="#ffd060"), glow="#ff8a30", eye="#ffd030",
     dims=dict(hip=11.0, lt=4.0, lx=2.6, pw=9.0, aw=7.8, cw=10.4, chh=6.0, cd=6.0, hs=8.0, at=3.6, ua=7.0, fa=7.0, ah=4.0), legs="serpent", serpent_mat="skin2", serpent_mats=["skin2", "stone", "stone", "skin2", "stone"], ears="pointed", claws=3,
     outfit="belly", belt_gem=False, hair=dict(cap=True, fringe=0, mat="flame", spikes=7, spike_len=5.4, spike_fan=20, back=0, tail=dict(len=3.6, w=3.4, n=3, mat="flame", rx0=38, rx=-6), tuft_mat="flame", tufts=[(-5.4, 6.0, 4.0, 40), (3.6, 6.0, 4.0, -40)]),
     horns=[], wings=[], tail=None, extras=[("third_eye",), ("chest_gem",)],
     sway=[("tail1", "rotation", (0, 6, 0), 0), ("tail2", "rotation", (0, 8, 0), -0.7), ("tail3", "rotation", (0, 10, 0), -1.4), ("tail4", "rotation", (0, 12, 0), -2.1), ("ptail1", "rotation", (4, 0, 3), 0.3), ("ptail2", "rotation", (5, 0, 4), 1.0)],
     paint={"face": eyes(sclera="ink", iris="eye", w=2.0) + brow(slant=0.4) + mouth("grin", 6.2), "chest": lines((((2.0, 1.0), (4.0, 3.0)), ((10.0, 1.0), (8.0, 3.0)), ((3.0, 4.0), (9.0, 4.0))), "eye", True)})

spec(id="kin_of_egyn", source="Blue Exorcist wiki (en), Egyn: Appearance (hosts). Light-blue hair darker toward the tips, a long teardrop-shaped horn at the top of the head, brown skin, gold octopus-like eyes, a small body (about half the height of Lucifer); the legs as tentacles of water are the mod's own design",
     colors=dict(skin="#aa8466", skin2="#7a5a44", hair="#7ab8f0", accent="#7ab8f0", water="#5a9ae0", cloth="#2a5a9a", cloth2="#1a3a6a", horn="#e8f4ff", claw="#e8f4ff", tooth="#f4fbff"), glow="#7ac8ff", eye="#ffd060",
     dims=dict(hip=9.0, lt=3.4, lx=2.2, pw=8.0, aw=7.0, cw=9.4, chh=5.6, cd=5.4, hs=8.8, at=3.0, ua=6.0, fa=6.0, ah=3.6), legs="tentacles", ears="pointed", claws=3, claw_len=2.0, outfit="belly",
     hair=dict(cap=True, fringe=5, back=14, side=10, fringe_len=3.4), horns=[dict(kind="tear", single=True, y=8.2, z=-0.6, s=1.5)], wings=[], tail=None,
     sway=[("leg_r", "rotation", (4, 0, 3), 0), ("leg_l", "rotation", (4, 0, -3), 1.5)],
     paint={"face": eyes(sclera="eye", iris="ink", pupil="ink", w=2.2, h=1.2, slit=False, glow=False) + [("r", 2.0, 4.0, 2.7, 4.5, "eye", True), ("r", 5.3, 4.0, 6.0, 4.5, "eye", True)] + [("l", 2.2, 5.8, 3.2, 6.1, "ink"), ("l", 3.2, 6.1, 4.8, 6.1, "ink"), ("l", 4.8, 6.1, 5.8, 5.8, "ink")]})

spec(id="kin_of_amaimon", source="Blue Exorcist wiki (en), Amaimon: Appearance (hosts). Green hair, a small pointed horn on top of the head, golden-green slit eyes with a red inner ring, long sharp nails; the hulking, rock-plated body of the KIN is the mod's own design",
     colors=dict(skin="#c8a890", skin2="#8a7058", hair="#3aa85a", accent="#a8d030", horn="#8a7458", cloth="#5a4a38", cloth2="#3a2e22", white="#7a6a52", metal="#c8a050", claw="#2a2218", tooth="#f4ecd8"), glow="#a8d030", eye="#a8d030",
     dims=dict(hip=11.5, lt=5.2, lx=3.4, pw=12.4, aw=11.0, cw=16.4, chh=8.0, cd=8.4, hs=8.6, at=5.8, ua=7.6, fa=7.4, ah=5.0, hand=4.0), legs="boot", pants="cloth", ears="pointed", claws=4, claw_len=5.0, claw_mat="claw", outfit="plate", belt_gem=False,
     hair=dict(cap=True, fringe=0, spikes=7, spike_len=5.4, spike_fan=16, spike_back=-8, back=3, side=0), horns=[dict(kind="spike", single=True, y=8.2, z=-1.0, s=1.4)], wings=[], tail=None, extras=[("pauldron_spikes", "horn")],
     paint={"face": eyes(sclera="ink", iris="eye", w=2.0) + [("r", 1.8, 3.8, 2.2, 4.2, "accent"), ("r", 5.8, 3.8, 6.2, 4.2, "accent")] + brow(slant=0.6, w=2.4) + mouth("fangs", 6.3), "chest": lines((((2.0, 3.0), (5.0, 2.0)), ((10.0, 3.0), (7.0, 2.0))), "eye", True)})

spec(id="kin_of_astaroth", source="Blue Exorcist wiki (en), Astaroth: Appearance (hosts). Ram horns, longer sharper black nails, canine-like teeth, red bloodshot eyes, a demonic tail, a longer tongue, demonic mushrooms (one with four eyes) grown over the body; the KIN is the mod's own design in that look",
     colors=dict(skin="#aab888", skin2="#7a8a5a", hair="#5a6a2a", accent="#9ad03a", horn="#3a3a28", cloth="#3a4a2a", cloth2="#2a3a1a", rot="#7a8a3a", claw="#14180e", tooth="#f4f0d8", sclera="#d8c8c0", metal="#9ad03a"), glow="#9ad03a", eye="#e83a3a",
     dims=dict(hip=13.5, lt=3.6, lx=2.5, pw=8.6, aw=7.6, cw=11.0, chh=6.0, cd=5.8, hs=8.0, at=3.4, ua=7.6, fa=7.6, ah=4.4), legs="claw", ears="pointed", claws=3, claw_len=4.0, claw_mat="claw", outfit="bandage", belt_gem=False,
     hair=dict(cap=True, fringe=3, spikes=5, spike_len=3.6, spike_fan=22, back=2, side=4), horns=[dict(kind="ram", x=3.0, y=7.0, s=1.1)], wings=[], tail=dict(len=6.0, w=2.2, tip="spade", n=3, mat="skin"), extras=[("mushrooms",)],
     sway=[("tail1", "rotation", (0, 6, 0), 0)],
     paint={"face": [("r", 0.9, 3.2, 3.1, 4.9, "ink"), ("r", 4.9, 3.2, 7.1, 4.9, "ink")] + eyes(sclera="sclera", iris="eye", w=1.8, slit=False) + [("l", 1.0, 3.4, 2.0, 3.8, "eye"), ("l", 6.0, 3.8, 7.0, 3.4, "eye")] + brow(slant=0.7) + mouth("grin", 6.4)})

spec(id="kin_of_lucifer", source="Blue Exorcist wiki (en), Lucifer: Appearance (host). A tall, fair-skinned man with spiky slicked-back blond hair, a long blond tail, pointed ears, elongated canines, patches of diseased skin; the halo and white-gold coat are the mod's own design for the King of Light",
     colors=dict(skin="#f8e4cc", skin2="#d8bca0", hair="#f4dc7a", accent="#fff0a8", horn="#fff0a8", cloth="#f4efe6", cloth2="#c8a850", white="#fffaf0", metal="#ffe070", claw="#e8d4b0", tooth="#fffaf0", ink="#2a1a10"), glow="#fff0a8", eye="#ffe070",
     dims=dict(hip=15.0, lt=3.6, lx=2.5, pw=8.8, aw=7.8, cw=11.4, chh=6.6, cd=6.0, hs=8.0, at=3.4, ua=8.0, fa=7.8, ah=4.8), legs="boot", pants="cloth", ears="pointed", claws=3, claw_len=2.0, outfit="coat", coat_len=14, belt_gem=False,
     hair=dict(cap=True, fringe=0, spikes=8, spike_len=5.0, spike_fan=18, spike_back=-30, back=3, side=0), horns=[], wings=[], tail=dict(len=7.4, w=2.2, tip="none", n=3, mat="hair"), extras=[("halo", "gem", 8.0), ("chest_gem",)],
     sway=[("tail1", "rotation", (0, 7, 0), 0), ("tail2", "rotation", (0, 9, 0), -0.8)],
     paint={"face": eyes(sclera="white", iris="eye", w=1.8) + brow(slant=0.5) + mouth("fangs", 6.3), "chest": [("r", 2.0, 1.0, 3.6, 2.2, "skin2"), ("r", 8.0, 2.4, 10.0, 3.8, "skin2")], "abs": [("r", 1.0, 1.0, 2.8, 2.6, "skin2"), ("r", 5.6, 1.4, 7.4, 3.0, "skin2")]})
