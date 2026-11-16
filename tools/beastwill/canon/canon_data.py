"""The per-will canon table of the Beast Will canon rework (docs/beastwill_canon_rework.md).

One entry per will: where the creature comes from, the facts used, the abilities of both forms (K = kept from the remade will, N = new), and the few stat and rate changes.
Everything that is not in the source text is marked as design. build_canon.py turns this table into data/anime_multiverse/beast_will_canon/*.json, the lang fragment and the doc.
"""

TBATE = "TBATE Fandom wiki (English, MediaWiki API)"
WP = "Wikipedia (English, MediaWiki API)"


def K(aid, **ov):
    """An ability kept from the remade will (same id, same kind, same numbers unless overridden)."""
    return {"keep": True, "id": aid, "ov": ov}


def N(aid, kind, name, effect, **ov):
    """A new ability: numbers come from the kind's default table, overridden by ov."""
    return {"keep": False, "id": aid, "kind": kind, "name": name, "effect": effect, "ov": ov}


WILLS = {
    # ------------------------------------------------------------------ the four TBATE wills
    "dragon_will": dict(
        tag="CANON", src=TBATE + ": Beast Will, Beast Tamers, Arthur Leywin/Abilities, Sylvia",
        short="TBATE wiki: Beast Tamers, Arthur Leywin/Abilities",
        lore="Sylvia's dragon will. The first stage is Static Void, which takes the holder out of the flow of time. The second is Realmheart: the eyes see mana and aether, and gravity crushes foes down. It drains mana hard and is not meant to be held for long.",
        facts=["Beast Will passes on from an A-class or higher mana beast; Acquire borrows part of the beast's strength, Integrate changes the body into the beast's form (Beast Will page).",
               "Arthur's Acquire phase is Static Void: he excludes himself from the flow of time, and it never gets easier (Arthur Leywin/Abilities).",
               "His Integrate phase is Realmheart: white hair, lavender eyes, gold runes, sees all mana and aether, and may use gravity magic only in this form; Down Force is a gravity spell that crushes what it pushes down on.",
               "A third phase exists (Unleashed Realmheart) and shattered his core when overused (drain is raised for this reason; the third phase itself is not built)."],
        A=[K("static_void"), K("aether_step")],
        I=[K("realmheart_sight"), N("down_force", "maelstrom", "Down Force", "Realmheart gravity: foes close to you are pressed down and drawn in; bosses resist.", costPct=14, cooldownTicks=480, strain=20, radius=9, power=5, bossFactor=0.35)],
        PH_I=dict(drainPct=2.4, strainPerSecond=1.5),
        notes=["not built: the third phase (Unleashed Realmheart), forged vs legacy insight"],
    ),
    "shadow_panther_will": dict(
        tag="CANON", src=TBATE + ": Beast Tamers, Mana Beasts, Beast Will",
        short="TBATE wiki: Beast Tamers, Mana Beasts",
        lore="The will of a shadow panther (AA class), as Virion holds it. The first stage borrows its speed and stealth, the second makes the holder a black panther of great strength, speed and silence. Wind and sound are bent to erase every trace of the holder.",
        facts=["Shadow Panther is an AA-class mana beast; Virion holds its will as a forged tamer (Mana Beasts, Beast Tamers).",
               "Acquire: partially borrows the speed and stealth of the shadow panther. Integrate: a black panther with superhuman strength, speed and stealth, far beyond the first phase.",
               "Shadow panthers instinctively manipulate wind and sound to erase their presence (Beast Tamers)."],
        A=[N("hushed_wind", "shadow_veil", "Hushed Wind", "Wind and sound bend round you: you turn unseen for a short while and your steps make no noise. Striking or being hurt ends it."), K("night_step")],
        I=[K("windsilence_shroud"), K("throat_pounce")],
        passive=[("spd", "mult", 0.03)], I_stats=[("atk", "add", 4.0)], req_ep=20000,
        notes=["the old Acquire had two dashes and no veil; the veil also gives the quiet steps (BeastWillStealth.quiet)"],
    ),
    "elderwood_guardian_will": dict(
        tag="CANON", src=TBATE + ": Elderwood Guardian, Beast Will",
        short="TBATE wiki: Elderwood Guardian, Beast Will",
        lore="The will of an S-class Elderwood Guardian, a giant of vine and branch with an armoured man's body on a horse of vines. It lashes, snares and thrusts great lances of vine, and mends itself fast. Tessia's was corrupted and had to be trained before it could be held.",
        facts=["Humongous S-class mana beast of wood and plant affinity with a beast core and a beast will (Elderwood Guardian).",
               "Made entirely of entangled vines and branches; an armoured man's top half on a centaur-like lower body of vines.",
               "Shapes its vines into many forms, such as a giant lance; it has a high-speed regenerative ability; it can use mist to create illusions.",
               "Tessia's will was corrupted and needed training to master (Beast Will page); the mist illusions are not built."],
        A=[K("emerald_vine_lash"), K("grass_burst_snare")],
        I=[N("vine_lance", "ambush", "Vine Lance", "A giant lance of vines is thrust through one foe in front of you.", costPct=11, cooldownTicks=240, strain=16, range=10, power=5, bossFactor=0.5),
           N("heartwood_regrowth", "verdant_mend", "Heartwood Regrowth", "Swift regrowth: the vines knit your wounds over a few seconds.", costPct=12, cooldownTicks=480, strain=16, durationTicks=180, power=3, bossFactor=1.0)],
        PH_A=dict(regen=0.3), PH_I=dict(regen=0.7), passive=[("hp", "add", 2.0)],
        notes=["not built: mist illusions"],
    ),
    "lion_will": dict(
        tag="CANON", src=TBATE + ": Curtis Glayder/Abilities, Mana Beasts",
        short="TBATE wiki: Curtis Glayder/Abilities, Mana Beasts",
        lore="The will of a World Lion (A class), as Curtis holds it. In the first stage the holder grows a lion's mane and barely controls it. The second carries the World Howl, a huge mana blast that costs a great deal and is best loosed with the lion beside you.",
        facts=["World Lion is an A-class mana beast; Curtis formed an equal contract with the lion Grawder (Mana Beasts, Curtis Glayder/Abilities).",
               "In his first phase his hair becomes larger and bushier like a lion's, spreading to his face; he has little control over it and uses it as a last resort.",
               "World Howl: mounted on Grawder, mana is built up and fired as a condensed beam that shreds what it meets; it takes a great amount of mana and needs Grawder for full power."],
        A=[K("pride_poise"), N("mane_guard", "bark_ward", "Mane Guard", "The thick mane takes the blow: you take less damage for a few seconds.", costPct=5, cooldownTicks=480, strain=6, durationTicks=100, power=2, bossFactor=1.0)],
        I=[K("sovereigns_roar_call", costPct=18, strain=24, cooldownTicks=600), K("royal_paw")],
        PH_A=dict(strainPerSecond=0.22),
        notes=["the beam is built as the existing cone howl; the lion beside you is not built"],
    ),
    # ------------------------------------------------------------------ TBATE-kin wills (the beast is not in the wiki, its relatives are)
    "flame_serpent_will": dict(
        tag="TBATE-KIN", src=TBATE + ": Mana Beasts (Hades Serpent)",
        short="TBATE wiki: Mana Beasts (Hades Serpent)",
        lore="An A-class elder of the line of the Hades Serpent, the long serpent with blue flames along its spine. It senses living heat, whips with a hot tail, spits molten venom and flares fire along its spine.",
        facts=["Hades Serpent (AA class): a powerful serpent over 100 feet long with blue flames across its spine and matte grey scales (Mana Beasts).",
               "The Flame-Serpent Elder is the mod's own A-class relative, not a wiki creature (design)."],
        A=[K("pit_heat_sense"), N("cinder_tail_whip", "tail_sweep", "Cinder Tail Whip", "A hot tail whip hurls back the foes around you.")],
        I=[K("magma_spit"), N("spine_flame_burst", "draconic_pulse", "Spine-Flame Burst", "Fire flares along the spine in a shockwave that sets foes alight.", costPct=12, cooldownTicks=180, strain=16, range=8, radius=6, durationTicks=70, power=5, maxTargets=5, bossFactor=0.5)],
    ),
    "ridge_wolf_will": dict(
        tag="TBATE-KIN", src=TBATE + ": Mana Beasts (Black-fanged Wolf)",
        short="TBATE wiki: Mana Beasts (Black-fanged Wolf)",
        lore="The elder of a pack of black-fanged wolves: it weighs its prey before it pounces, hunts as a pack and never loses a trail. Its fur does not mind fire (not modelled).",
        facts=["Black-fanged Wolf (B class): large wolf-like mana beasts with fire-resistant fur, can eat rocks, move in packs, hunt strategically and evaluate prey before pouncing (Mana Beasts).",
               "The Ridge-Wolf Elder is the mod's own A-class relative (design); fire-resistant fur is not built (no allowed attribute)."],
        A=[K("scent_trail"), N("pack_surge", "mane_surge", "Pack Surge", "The pack's drive runs in you: a short burst of steadiness and bite.", costPct=5, cooldownTicks=420, strain=6, durationTicks=100, power=1.5, bossFactor=0.0)],
        I=[K("hamstring_lunge"), K("pack_call")],
    ),
    "wind_skimmer_will": dict(
        tag="TBATE-KIN", src=TBATE + ": Mana Beasts (Thunder Hawk)",
        short="TBATE wiki: Mana Beasts (Thunder Hawk)",
        lore="An elder raptor of the hawk line: it skims the top of the gale, buffets with its wings and, like a thunder hawk, takes prey from the ground into the sky. Storm bolts jump between its foes.",
        facts=["Thunder Hawk: a mana beast that lifts forest hounds from the ground and kills them in the air; lives in large canopy nests (Mana Beasts).",
               "The Gale Skimmer Elder is the mod's own A-class relative (design); the chain bolts are the design twist."],
        A=[K("skim_step"), N("wingbeat_gust", "world_howl", "Wingbeat Gust", "A beat of the wings buffets the foes in front of you back.", costPct=6, cooldownTicks=400, strain=6, range=8, radius=6, durationTicks=30, power=1, maxTargets=3, bossFactor=0.1)],
        I=[N("storm_hawk_bolt", "thunder_chain", "Storm-Hawk Bolt", "A bolt leaps from foe to foe, a little weaker at each hop.", costPct=11, cooldownTicks=240, strain=14, range=12, radius=5, power=4.5, maxTargets=4, bossFactor=0.5), K("cyclone_wake")],
    ),
    # ------------------------------------------------------------------ the mythology wills
    "ammit_will": dict(
        tag="MYTH", src=WP + ": Ammit", short="Wikipedia: Ammit",
        lore="Ammit, the Devourer of the Dead: a crocodile's head, a lion's front and a hippopotamus's hind. She waits by the scales to eat the heart of the unjust. The will weighs foes with a glance, hardens like hide and ends with a bite at the heart that mends the holder.",
        facts=["Ancient Egyptian goddess 'Devourer of the Dead' with the forequarters of a lion, the hindquarters of a hippopotamus and the head of a crocodile.",
               "Waits in the Duat to learn whether she may consume the heart of the deceased."],
        A=[K("weighing_glance"), K("hippo_hide")],
        I=[K("death_roll_snap"), N("heart_devour", "devour", "Devour the Heart", "A bite at one foe's heart: heavy damage, and you mend a share of what you dealt.", costPct=12, cooldownTicks=320, strain=18, range=5, power=6, bossFactor=0.4)],
    ),
    "apophis_will": dict(
        tag="MYTH", src=WP + ": Apep", short="Wikipedia: Apep (Apophis)",
        lore="Apophis, the serpent of chaos and enemy of Ra. The Coffin Texts give it a gaze that overwhelms Ra and his company. The will slips through dusk, lashes, stops minds with its gaze and sweeps whole crowds aside with its coil.",
        facts=["Usually shown as a giant snake, 'the Lord of Chaos', greatest enemy of the sun god Ra.",
               "The Coffin Texts imply a magical gaze that overwhelms Ra and his entourage.",
               "The old 'swallow the sun' time stop is not in the extract and is replaced by a coil sweep (only the dragon and the sphinx keep a time ability)."],
        A=[K("dusk_slither"), K("coil_lash")],
        I=[K("mind_stopping_gaze"), N("coil_of_chaos", "tail_sweep", "Coil of Chaos", "A sweep of the great coil flings back everything around you.", costPct=12, cooldownTicks=180, strain=16, radius=6, power=5, maxTargets=6, bossFactor=0.4)],
    ),
    "cerberus_will": dict(
        tag="MYTH", src=WP + ": Cerberus", short="Wikipedia: Cerberus",
        lore="Cerberus, the three-headed hound of Hades, with a snake for a tail and snakes about its neck. Ovid gives it a venomous mouth. The will guards a threshold, lunges, howls with three throats and sprays venom.",
        facts=["Multi-headed dog guarding the gates of the underworld, usually three heads, a serpent for a tail, snakes protruding from its body.",
               "Ovid's Cerberus has a venomous mouth; Euphorion associates it with the creation of the poisonous plant aconite.",
               "The old hellfire breath is not in the extract and is replaced by venom (design twist kept small)."],
        A=[K("threshold_guard"), K("hound_lunge")],
        I=[K("triple_howl"), N("foam_venom", "venom_spit", "Venom Foam", "Venom foams from the hound's mouths in a cone: poison for several seconds.", costPct=11, cooldownTicks=170, strain=16, range=11, radius=6, durationTicks=100, power=5, maxTargets=4, bossFactor=0.5)],
    ),
    "crested_boar_will": dict(
        tag="MYTH", src=WP + ": Erymanthian boar, Calydonian boar hunt", short="Wikipedia: Erymanthian boar, Calydonian boar",
        lore="A tusker like the Erymanthian boar, shaggy, wild and of vast weight with foaming jaws, and like the Calydonian boar that was sent to ravage the fields. It does not dodge: it walks through the blow, roots itself, gores and shakes the ground.",
        facts=["Erymanthian boar: 'shaggy and wild', 'tameless', 'of vast weight' and 'foaming jaws'; the fourth labour of Heracles, driven into deep snow.",
               "Calydonian boar: sent by Artemis to ravage the region of Calydon."],
        A=[K("crest_hide"), N("rooting_gore", "ambush", "Rooting Gore", "You plant your feet and gore one foe in front of you.", costPct=6, cooldownTicks=300, strain=8, range=6, power=3, bossFactor=0.5)],
        I=[K("crest_gore"), K("tusk_quake")],
    ),
    "cu_sith_will": dict(
        tag="MYTH", src=WP + ": Cu-sith", short="Wikipedia: Cu-sith",
        lore="The green hound of the Highlands hunts in silence and gives only three barks; whoever has not reached shelter by the third is overcome with terror. The will walks in moor mist, scents living prey, barks the third bark and marks a death omen.",
        facts=["Dark green shaggy coat; capable of hunting silently but occasionally gives three terrifying barks and only three.",
               "Those who hear it must reach safety by the third bark or be overcome with terror to the point of death; it can take the souls of the living."],
        A=[K("moor_mist"), N("hound_scent", "dragon_sight", "Hound's Scent", "You smell out living prey nearby: they glow for a while.", costPct=4, cooldownTicks=400, strain=5, range=16, radius=14, durationTicks=120, power=1, maxTargets=5, bossFactor=1.0)],
        I=[K("third_bark"), K("death_omen")],
    ),
    "fenrir_will": dict(
        tag="MYTH", src=WP + ": Fenrir", short="Wikipedia: Fenrir",
        lore="Fenrir grew so fast that the gods bound him with the chain Gleipnir, and he bit off Tyr's hand. At Ragnarok he kills Odin. The will leaps, pounces, snaps its jaws and binds foes the way the chain bound him.",
        facts=["Son of Loki, grows rapidly; the gods bound him with Gleipnir and he bit off Tyr's right hand.",
               "Foretold to kill Odin at Ragnarok and to be killed in turn by Vidar. Binding foes with the chain is a design twist."],
        A=[K("soul_light_leap"), N("wolf_pounce", "ambush", "Wolf Pounce", "A pounce on one foe in front of you.", costPct=6, cooldownTicks=300, strain=8, range=6, power=3, bossFactor=0.5)],
        I=[K("jaws_of_ragnarok"), K("gleipnir_snare")],
    ),
    "hafgufa_will": dict(
        tag="MYTH", src=WP + ": Hafgufa", short="Wikipedia: Hafgufa",
        lore="A sea creature so vast it looks like an island. It belches a bait that draws fish in, then shuts its mouth on all of them at once. The will sinks into the sea like an island, breathes its sweet bait and swallows what it drew in.",
        facts=["Described as a massive fish that looked more like an island than a living thing; its name means sea-reek or sea-steamer.",
               "It belches out food that attracts fish, then closes its mouth and devours them all at once."],
        A=[K("barnacle_hide"), N("island_disguise", "shadow_veil", "Island Disguise", "You sink low and look like part of the sea: unseen for a short while.", costPct=5, cooldownTicks=400, strain=5, durationTicks=100, power=2, bossFactor=0.5)],
        I=[K("sweet_breath"), K("great_swallow")],
    ),
    "hydra_will": dict(
        tag="MYTH", src=WP + ": Lernaean Hydra", short="Wikipedia: Lernaean Hydra",
        lore="The Lernaean Hydra: poisonous breath and blood that kills by scent, and heads that grow back, two for each one lost. The will spits marsh venom, scabs wounds shut, strikes with many heads and regrows.",
        facts=["Poisonous breath and blood so virulent that even its scent was deadly.",
               "Later versions: for each head chopped off it regrows two; one head was immortal; Heracles burned the necks with fire."],
        A=[K("marsh_fang"), K("scab_over")],
        I=[K("nine_head_strike"), K("heads_regrow")],
        passive=[("hp", "add", 2.0)],
    ),
    "jormungandr_will": dict(
        tag="MYTH", src=WP + ": Jormungandr", short="Wikipedia: Jormungandr",
        lore="The Midgard Serpent circles the earth and grasps its own tail; Thor's great enemy, it kills him with its venom at Ragnarok. The will slips like an eel, bites with brine venom, coils the world round foes and lets the ocean rise.",
        facts=["An unfathomably large sea serpent that grew until it surrounded the earth and grasped its own tail.",
               "Thor's archenemy; they kill each other at Ragnarok, Thor dying of the serpent's venom after nine paces; it sprays poison over air and water."],
        A=[K("eel_slip"), K("brine_fang")],
        I=[K("world_coil"), K("ocean_rises")],
    ),
    "kitsune_will": dict(
        tag="MYTH", src=WP + ": Kitsune", short="Wikipedia: Kitsune",
        lore="A fox spirit that shapeshifts and bewitches, with up to nine tails and kitsunebi, fox-fire. The will glamours the eye, bewilders with a glance, shows nine tails of mirage and flings fox-fire.",
        facts=["A fox or fox spirit with the supernatural ability to shapeshift or bewitch other life forms; as many as nine tails.",
               "Reputed to fire off kitsunebi, 'fox-fire'; keeps a luminous jewel in its tail."],
        A=[K("fox_glamour"), N("bewitching_glance", "dread_gaze", "Bewitching Glance", "A fox's glance bewilders hunters: foes lose their aim.", costPct=6, cooldownTicks=500, strain=8, range=12, radius=8, durationTicks=100, power=1.5, maxTargets=3, bossFactor=0.2)],
        I=[K("nine_tail_mirage"), N("kitsunebi_flare", "draconic_pulse", "Kitsunebi Flare", "Fox-fire flares in a shockwave round the target.", costPct=12, cooldownTicks=180, strain=16, range=8, radius=6, durationTicks=70, power=4.5, maxTargets=5, bossFactor=0.5)],
    ),
    "kraken_will": dict(
        tag="MYTH", src=WP + ": Kraken", short="Wikipedia: Kraken",
        lore="The kraken: a vast sea creature said to pull down ships, whose sinking makes a whirlpool like the Maelstrom. The will clouds the water, grips with suckers, sweeps with tentacles and drags foes into a whirlpool.",
        facts=["Described as a huge octopus-like creature with a reputation for pulling down ships.",
               "Its submerging was said to make a whirlpool compared to Norway's Moskstraumen, 'the Maelstrom'. The ink cloud is not in the extract (design)."],
        A=[K("ink_cloud"), N("sucker_grip", "restraining_vines", "Sucker Grip", "Tentacles with suckers hold the foes in front of you in place.", costPct=6, cooldownTicks=600, strain=8, range=10, radius=5, durationTicks=60, power=2, maxTargets=3, bossFactor=0.25)],
        I=[N("tentacle_sweep", "tail_sweep", "Tentacle Sweep", "A sweep of tentacles hurls back everything round you.", costPct=11, cooldownTicks=170, strain=15, radius=6, power=4.5, maxTargets=6, bossFactor=0.5), K("whirlpool_drag")],
    ),
    "minotaur_will": dict(
        tag="MYTH", src=WP + ": Minotaur", short="Wikipedia: Minotaur",
        lore="Asterion, the bull-headed man of King Minos's Labyrinth, killed by Theseus with the help of a golden thread. The will senses the maze, charges like a ram, rages and shakes the maze's floor.",
        facts=["Head and tail of a bull on the body of a man, also called Asterion or Asterius; dwelt at the centre of the Labyrinth built by Daedalus.",
               "Killed by Theseus who found his way with Ariadne's thread. The axe quake is design (the labrys is not in the extract)."],
        A=[K("ram_charge"), K("maze_sense")],
        I=[K("rage_of_asterion"), K("labrys_quake")],
    ),
    "nidhoggr_will": dict(
        tag="MYTH", src=WP + ": Nidhogg", short="Wikipedia: Nidhogg",
        lore="Nidhogg gnaws the roots of Yggdrasil and chews the corpses of oath-breakers in Niflheim. The will rots bark-hard, feeds on carrion, devours a foe to mend itself and gnaws roots to bind.",
        facts=["A Germanic dragon who gnaws at the roots of the world tree Yggdrasil.",
               "Its name may refer to chewing the corpses of the inhabitants of Niflheim, the guilty of murder, adultery and oath-breaking ('Malice Biter')."],
        A=[K("rot_bark"), K("carrion_feast")],
        I=[N("corpse_gnaw", "devour", "Corpse Gnaw", "Gnaw one foe hard and mend with what you take.", costPct=12, cooldownTicks=320, strain=18, range=5, power=6, bossFactor=0.4), K("root_gnaw")],
    ),
    "nue_will": dict(
        tag="MYTH", src=WP + ": Nue", short="Wikipedia: Nue",
        lore="The Nue of the Heike: a monkey's head, a tiger's limbs, a tanuki's body and a snake for a tail. It flies as a black cloud and cries 'hyoo hyoo' at night, and the emperor fell ill. The will cloaks in cloud, lashes with the snake tail, bites with its venom and cries a dread.",
        facts=["In the Tale of the Heike: head of a monkey, limbs of a tiger, body of a Japanese raccoon dog and the front half of a snake as a tail.",
               "Shape-shifts into a black cloud that flies; its eerie 'hyoo hyoo' cry; the emperor fell ill under it. The old lightning is not in the extract and is dropped."],
        A=[N("serpent_tail_lash", "tail_sweep", "Serpent-Tail Lash", "The snake tail lashes round you and hurls foes back.", costPct=5, cooldownTicks=240, strain=6, radius=3, power=2, maxTargets=3, bossFactor=0.5), K("cloud_cloak")],
        I=[N("tail_snake_venom", "venom_spit", "Tail-Snake Venom", "The snake tail spits venom at what is in front of you.", costPct=10, cooldownTicks=170, strain=14, range=11, radius=5, durationTicks=100, power=5, maxTargets=3, bossFactor=0.5), K("night_cry")],
    ),
    "pegasus_will": dict(
        tag="MYTH", src=WP + ": Pegasus", short="Wikipedia: Pegasus",
        lore="Pegasus, the white winged stallion. His hoof made the fountain Hippocrene on Mount Helicon, and Zeus had him carry his thunderbolts. The will leaps through the sky, gallops with a star mane, heals at the spring and throws a chain of bolts.",
        facts=["A winged horse, usually a white stallion, foaled by Medusa; creator of the fountain Hippocrene on Mount Helicon.",
               "Zeus brought Pegasus to Olympus to carry his thunderbolts."],
        A=[K("sky_leap"), K("star_mane_gallop")],
        I=[K("hippocrene_spring"), N("thunderbearer_bolts", "thunder_chain", "Thunderbearer's Bolts", "A bolt leaps from foe to foe, a little weaker at each hop.", costPct=12, cooldownTicks=240, strain=16, range=12, radius=5, power=5, maxTargets=4, bossFactor=0.5)],
    ),
    "scylla_charybdis_will": dict(
        tag="MYTH", src=WP + ": Scylla, Charybdis", short="Wikipedia: Scylla, Charybdis",
        lore="Scylla snatches sailors with her dog heads from one side of a narrow strait; across it Charybdis swallows the sea three times a day and belches it back as whirlpools. The will snatches from the deck, lashes with six necks, devours and swallows the sea.",
        facts=["Scylla: a man-eating monster on one side of a narrow channel, six dog heads in the Suda; sailors who avoid Charybdis pass dangerously close to her.",
               "Charybdis swallowed a huge amount of water three times a day and belched it out, making whirlpools that drag a ship under."],
        A=[N("snatch_from_deck", "ambush", "Snatch from the Deck", "A head snatches one foe in front of you.", costPct=6, cooldownTicks=300, strain=8, range=6, power=3, bossFactor=0.5), N("six_neck_lash", "vine_lash", "Six-Neck Lash", "Long necks lash at the nearest foes.", costPct=4, cooldownTicks=110, strain=5, range=7, radius=5, durationTicks=40, power=2.2, maxTargets=2, bossFactor=0.4)],
        I=[N("six_heads_meal", "devour", "Six Heads' Meal", "The heads fall on one foe: heavy damage, and you mend a share.", costPct=12, cooldownTicks=320, strain=18, range=5, power=6, bossFactor=0.4), K("charybdis_gulp")],
    ),
    "sphinx_will": dict(
        tag="MYTH", src=WP + ": Sphinx", short="Wikipedia: Sphinx",
        lore="A guardian with a woman's head, a lion's body and a bird's wings, who sets a riddle and kills and eats those who fail. The will stands stone-still, stares with a riddle, stills the moment and asks the riddle of the ages.",
        facts=["Greek sphinx: head of a woman, haunches of a lion, wings of a bird; challenges all she meets to a riddle and kills and eats those who fail (Oedipus).",
               "Sphinxes are guardians of temple entrances. Stopping time with silence is a design twist kept from the remade will."],
        A=[K("stone_poise"), N("riddling_stare", "dread_gaze", "Riddling Stare", "A riddle in her eyes: foes who meet them lose their aim.", costPct=6, cooldownTicks=500, strain=8, range=12, radius=8, durationTicks=100, power=1.5, maxTargets=3, bossFactor=0.2)],
        I=[K("silence_of_stone"), K("riddle_of_ages")],
        passive=[("tough", "add", 0.5)],
    ),
    "thunder_fang_chimera_will": dict(
        tag="MYTH", src=WP + ": Chimera (mythology)", short="Wikipedia: Chimera",
        lore="The chimera is a fire-breathing hybrid of lion, goat and serpent. This will is the mod's own storm variant: a ram's brace, a venomous tail and thunder that leaps from fang to fang.",
        facts=["Fire-breathing hybrid of different animal parts: a lion with a goat's head on its back and a snake's head at the tail end; killed by Bellerophon.",
               "The thunder is a design twist (the myth gives fire)."],
        A=[K("ram_brace"), N("serpent_tail_sting", "venom_spit", "Serpent-Tail Sting", "The tail's snake spits venom at what is in front of you.", costPct=4, cooldownTicks=180, strain=5, range=7, radius=3, durationTicks=70, power=1.5, maxTargets=2, bossFactor=0.5)],
        I=[K("storm_venom_spit"), N("thunder_fang_chain", "thunder_chain", "Thunder-Fang Chain", "A bolt leaps from foe to foe, a little weaker at each hop.", costPct=12, cooldownTicks=220, strain=16, range=12, radius=5, power=5, maxTargets=4, bossFactor=0.5)],
    ),
    "typhon_will": dict(
        tag="MYTH", src=WP + ": Typhon", short="Wikipedia: Typhon",
        lore="Typhon, the deadliest monster: a hundred snake heads that give off fire and every kind of noise, fire flashing from his eyes, winged, with snake coils for thighs; father of the storm winds. The will sees with storm-eyes, throws their bolts, breathes storm-fire and raises a typhoon.",
        facts=["Hesiod: a hundred snake heads that emitted fire and every kind of noise; fire flashed from his eyes; unkempt hair streaming on the wind.",
               "Nicander: many heads, hands and wings, with huge snake coils from his thighs. Father of destructive storm winds; lost to Zeus's thunderbolts."],
        A=[K("storm_eye"), N("storm_eye_bolts", "thunder_chain", "Storm-Eye Bolts", "Bolts flash from the eyes: a chain that leaps from foe to foe.", costPct=6, cooldownTicks=300, strain=8, range=10, radius=4, power=2, maxTargets=3, bossFactor=0.5)],
        I=[K("storm_fire_breath"), K("typhoon_vortex")],
    ),
    "yamata_no_orochi_will": dict(
        tag="MYTH", src=WP + ": Yamata no Orochi", short="Wikipedia: Yamata no Orochi",
        lore="The eight-headed, eight-tailed serpent that spans eight valleys and eight peaks, mossed over and bloody-bellied. Susanoo made it drink eightfold sake and found the sword Kusanagi in its tail. The will sees the eight valleys, lashes with eight heads and floods the valley.",
        facts=["A legendary eight-headed and eight-tailed serpent; its body spans eight valleys and eight peaks, covered in moss, cypress and cedar, with blood oozing from its belly.",
               "Susanoo had it drink eightfold-refined sake and killed it; Kusanagi no Tsurugi was found in its tail."],
        A=[K("eight_valley_sight"), N("eight_head_lash", "vine_lash", "Eight-Head Lash", "Heads lash at the nearest foes.", costPct=4, cooldownTicks=110, strain=5, range=7, radius=5, durationTicks=40, power=2.2, maxTargets=2, bossFactor=0.4)],
        I=[K("eightfold_strike"), K("valley_flood")],
    ),
}

# the first form's unlock, the second form's mastery; see BeastWillCanon.acquireUnlock and integrateMastery
ACQUIRE_UNLOCK = {"A": 0.10, "S": 0.15, "SS": 0.20}
INTEGRATE_MASTERY = {"A": 0.5, "S": 0.6, "SS": 0.7}

# defaults of a new ability by (kind, phase): cost, cooldown, strain, range, radius, duration, power, targets, boss
DEFAULTS = {
    ("dragon_sight", 1): (4, 400, 5, 16, 14, 120, 1, 5, 1.0),
    ("bark_ward", 1): (5, 460, 5, 0, 0, 100, 2, 1, 1.0),
    ("shadow_dash", 1): (4, 120, 5, 8, 0, 0, 1, 1, 0.0),
    ("shadow_veil", 1): (5, 360, 5, 0, 0, 100, 2, 1, 0.5),
    ("mane_surge", 1): (6, 400, 6, 0, 0, 100, 1.5, 1, 0.0),
    ("vine_lash", 1): (4, 110, 5, 7, 5, 40, 2.2, 2, 0.4),
    ("restraining_vines", 1): (6, 600, 8, 10, 5, 60, 2, 3, 0.25),
    ("verdant_mend", 1): (4, 700, 4, 0, 0, 110, 1, 1, 1.0),
    ("venom_spit", 1): (4, 180, 5, 7, 3, 70, 1.5, 2, 0.5),
    ("world_howl", 1): (6, 400, 6, 8, 6, 30, 1, 3, 0.1),
    ("time_exclusion", 1): (12, 1200, 14, 0, 6, 40, 1, 3, 0.0),
    ("tail_sweep", 1): (5, 260, 6, 0, 3, 0, 2, 3, 0.5),
    ("thunder_chain", 1): (6, 300, 8, 10, 4, 0, 2, 3, 0.5),
    ("devour", 1): (6, 400, 8, 4, 0, 0, 3, 1, 0.6),
    ("ambush", 1): (6, 300, 8, 6, 0, 0, 3, 1, 0.5),
    ("draconic_pulse", 1): (6, 160, 8, 6, 4, 40, 2, 3, 0.6),
    ("dread_gaze", 1): (6, 500, 8, 12, 8, 100, 1.5, 3, 0.2),
    ("maelstrom", 1): (8, 500, 10, 0, 6, 60, 2, 3, 0.4),
    ("ambush", 2): (12, 260, 18, 8, 0, 0, 6, 1, 0.5),
    ("bark_ward", 2): (10, 420, 12, 0, 0, 160, 4, 1, 0.5),
    ("draconic_pulse", 2): (12, 180, 16, 9, 7, 70, 5, 5, 0.5),
    ("dragon_sight", 2): (6, 400, 10, 24, 24, 160, 2, 8, 1.0),
    ("dread_gaze", 2): (10, 450, 16, 14, 12, 120, 3, 5, 0.3),
    ("maelstrom", 2): (15, 450, 20, 0, 10, 100, 5, 6, 0.4),
    ("mane_surge", 2): (14, 320, 24, 0, 0, 160, 6, 1, 0.0),
    ("restraining_vines", 2): (14, 520, 20, 12, 8, 100, 3, 5, 0.25),
    ("shadow_dash", 2): (8, 100, 10, 10, 0, 0, 3, 1, 0.0),
    ("shadow_veil", 2): (12, 400, 12, 0, 0, 120, 4, 1, 1.0),
    ("time_exclusion", 2): (18, 900, 26, 6, 9, 80, 3, 5, 0.0),
    ("venom_spit", 2): (10, 170, 14, 11, 6, 100, 5, 4, 0.5),
    ("verdant_mend", 2): (14, 500, 18, 0, 0, 170, 3, 1, 0.5),
    ("vine_lash", 2): (9, 100, 12, 9, 8, 60, 4, 3, 0.5),
    ("world_howl", 2): (14, 480, 20, 12, 12, 60, 5, 8, 0.25),
    ("tail_sweep", 2): (10, 160, 14, 0, 5, 0, 4, 5, 0.5),
    ("thunder_chain", 2): (12, 220, 16, 12, 5, 0, 5, 4, 0.5),
    ("devour", 2): (12, 320, 18, 5, 0, 0, 6, 1, 0.4),
}

FIELDS = ("costPct", "cooldownTicks", "strain", "range", "radius", "durationTicks", "power", "maxTargets", "bossFactor")

# lang of the milestones (the second, third and fourth are the cooldown steps of BeastWillCanon.cooldownFactor)
MILESTONE_TEXT = {
    1: "a quarter mastered: the will answers more readily.",
    2: "half mastered: its abilities recharge 5 % faster.",
    3: "three quarters mastered: its abilities recharge 8 % faster.",
    4: "fully mastered: its abilities recharge 10 % faster.",
}
