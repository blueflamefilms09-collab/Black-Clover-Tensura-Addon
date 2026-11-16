# Beast Will canon rework

Every one of the 27 Beast Wills now follows its source: the four TBATE wills follow the TBATE wiki, the others follow their myth or folklore, and the three that have no wiki beast are marked as relatives of a TBATE beast or as design. This page is the record of what each will took from its source, what changed, what it costs a server and what has not been checked.

## How it is switched

- Server config `nusmp-beastwill-server.toml`, group `rules`, key `canonRework` (full path `beastwill.rules.canonRework`), default `true`.
- `true`: the game reads `data/anime_multiverse/beast_will_canon` (the canon wills), the milestone cooldowns and the fiery-only ignition.
- `false`: the game reads `data/anime_multiverse/beast_will` exactly as before (the remade wills), with the old ignition rule and no milestone cooldowns. Nothing in that folder was changed.
- `classicWills = true` still wins over both (it reads `beast_will_classic`).
- Ids, save data, keywords, drops, EP needs (one exception below), affinities, names and tints are unchanged, so old worlds, cores and progress load as they were. Wills the player holds keep their progress; only the abilities and numbers that the form gives change.

## What is new in the rules

| Rule | What it does | Where |
|---|---|---|
| Three new ability kinds | `tail_sweep` (a hit and a push of up to 6 foes round you), `thunder_chain` (a bolt that hops foe to foe, up to 5, each hop keeps 80 %), `devour` (a heavy bite at one foe that heals you 40 % of what it dealt, at most 6 health) | `BeastWillRules.KINDS`, `BeastWillAbilities`, `BeastWillCanon` |
| Rank progression | the first form unlocks at 10 % assimilation for A, 15 % for S, 20 % for SS beasts; the second form asks 50 % / 60 % / 70 % mastery | `BeastWillCanon.acquireUnlock`, `integrateMastery` |
| Mastery milestones | at a quarter, a half, three quarters and full mastery a line tells the holder; from half mastery all ability cooldowns are 5 %, 8 % and 10 % shorter (never below the kind's own minimum) | `BeastWillCanon.milestone`, `cooldownFactor`, `BeastWillManager` |
| Fiery-only ignition | a draconic pulse sets foes alight only for fire wills (affinity with fire, flame, heat, magma, ember, inferno or blaze); earth, wind and stone beasts no longer burn their targets | `BeastWillCanon.ignites` |
| Distinct identities | no two wills share the pair of ability kinds of a form; a will never has two abilities of one kind in a form (they would share one cooldown); no ability id twice | `BeastWillCanon.signatureProblems` |
| Readable text | every will has a `canon_lore` and a `canon_source` line (tooltip of its core) | `tools/lang/beastwill_canon.json` |

Source tags in the table: CANON = the wiki of the series says it; TBATE-KIN = a relative of a TBATE beast that the wiki names; MYTH = a myth or folklore page; DESIGN = the mod's own. Any detail that is not in the fetched text is written as design in the will's notes.

## The 27 wills

### Dragon Will (`dragon_will`, rank SS, CANON)

- Source: TBATE Fandom wiki (English, MediaWiki API): Beast Will, Beast Tamers, Arthur Leywin/Abilities, Sylvia
- Canon fact: Beast Will passes on from an A-class or higher mana beast; Acquire borrows part of the beast's strength, Integrate changes the body into the beast's form (Beast Will page).
- Canon fact: Arthur's Acquire phase is Static Void: he excludes himself from the flow of time, and it never gets easier (Arthur Leywin/Abilities).
- Canon fact: His Integrate phase is Realmheart: white hair, lavender eyes, gold runes, sees all mana and aether, and may use gravity magic only in this form; Down Force is a gravity spell that crushes what it pushes down on.
- Canon fact: A third phase exists (Unleashed Realmheart) and shattered his core when overused (drain is raised for this reason; the third phase itself is not built).
- Note: not built: the third phase (Unleashed Realmheart), forged vs legacy insight
- First form unlocks at 20 % assimilation (was 10 %); second form asks 70 % mastery (was 50 %).
- Changed: integrate drainPct 2.4 (was 2)
- Changed: integrate strainPerSecond 1.5 (was 1.35)

| Form | Slot | Ability | Kind | Status | Numbers |
|---|---|---|---|---|---|
| Acquire | 1 | Static Void (`static_void`) | time_exclusion | KEPT  | cost 12 %, cd 1200 t, strain 14, targets 3 |
| Acquire | 2 | Aether Step (`aether_step`) | shadow_dash | KEPT  | cost 5 %, cd 160 t, strain 6, targets 1 |
| Integrate | 1 | Realmheart Sight (`realmheart_sight`) | dragon_sight | KEPT  | cost 6 %, cd 400 t, strain 10, targets 8 |
| Integrate | 2 | Down Force (`down_force`) | maelstrom | NEW replaces dragon_breath_pulse (draconic_pulse) | cost 14 %, cd 480 t, strain 20, targets 6 |

- Worst case: no entity is spawned; at most 8 foes touched per use; both Integrate abilities used on cooldown at full mastery: 0.10 uses per second, at most 0.10 pictures per second to nearby players (the new kinds send at most 2 per use, the old kinds as before); particles go through the perf budgets.
- Not checked: the look in a game (no model or texture was changed), the balance against other wills, and the canon beyond the pages listed above.

### Shadow Panther Will (`shadow_panther_will`, rank A, CANON)

- Source: TBATE Fandom wiki (English, MediaWiki API): Beast Tamers, Mana Beasts, Beast Will
- Canon fact: Shadow Panther is an AA-class mana beast; Virion holds its will as a forged tamer (Mana Beasts, Beast Tamers).
- Canon fact: Acquire: partially borrows the speed and stealth of the shadow panther. Integrate: a black panther with superhuman strength, speed and stealth, far beyond the first phase.
- Canon fact: Shadow panthers instinctively manipulate wind and sound to erase their presence (Beast Tamers).
- Note: the old Acquire had two dashes and no veil; the veil also gives the quiet steps (BeastWillStealth.quiet)
- First form unlocks at 10 % assimilation (was 10 %); second form asks 50 % mastery (was 50 %).
- Changed: passive: spd x0.03 (was spd x0.02)
- Changed: integrate stats: spd x0.2, step +0.5, jump x0.2, aspd x0.15, atk +4, fall +4 (was spd x0.2, step +0.5, jump x0.2, aspd x0.15, atk +3, fall +4)
- Changed: requireEp 20000 (was 10000), a design value for the AA class

| Form | Slot | Ability | Kind | Status | Numbers |
|---|---|---|---|---|---|
| Acquire | 1 | Hushed Wind (`hushed_wind`) | shadow_veil | NEW replaces afterimage_sidestep (shadow_dash) | cost 5 %, cd 360 t, strain 5, targets 1 |
| Acquire | 2 | Peak Reflex Step (`night_step`) | shadow_dash | KEPT  | cost 3 %, cd 100 t, strain 4, targets 1 |
| Integrate | 1 | Shadow-Merge Windsilence (`windsilence_shroud`) | shadow_veil | KEPT  | cost 12 %, cd 400 t, strain 12, targets 1 |
| Integrate | 2 | Throat Pounce (`throat_pounce`) | ambush | KEPT  | cost 10 %, cd 240 t, strain 14, targets 1 |

- Worst case: no entity is spawned; at most 1 foes touched per use; both Integrate abilities used on cooldown at full mastery: 0.15 uses per second, at most 0.15 pictures per second to nearby players (the new kinds send at most 2 per use, the old kinds as before); particles go through the perf budgets.
- Not checked: the look in a game (no model or texture was changed), the balance against other wills, and the canon beyond the pages listed above.

### Elderwood Guardian Will (`elderwood_guardian_will`, rank S, CANON)

- Source: TBATE Fandom wiki (English, MediaWiki API): Elderwood Guardian, Beast Will
- Canon fact: Humongous S-class mana beast of wood and plant affinity with a beast core and a beast will (Elderwood Guardian).
- Canon fact: Made entirely of entangled vines and branches; an armoured man's top half on a centaur-like lower body of vines.
- Canon fact: Shapes its vines into many forms, such as a giant lance; it has a high-speed regenerative ability; it can use mist to create illusions.
- Canon fact: Tessia's will was corrupted and needed training to master (Beast Will page); the mist illusions are not built.
- Note: not built: mist illusions
- First form unlocks at 15 % assimilation (was 10 %); second form asks 60 % mastery (was 50 %).
- Changed: passive: hp +2 (was none)
- Changed: acquire regen 0.3 (was 0.25)
- Changed: integrate regen 0.7 (was 0.5)

| Form | Slot | Ability | Kind | Status | Numbers |
|---|---|---|---|---|---|
| Acquire | 1 | Emerald Vine Lash (`emerald_vine_lash`) | vine_lash | KEPT  | cost 3 %, cd 100 t, strain 5, targets 2 |
| Acquire | 2 | Grass Burst Snare (`grass_burst_snare`) | restraining_vines | KEPT  | cost 6 %, cd 600 t, strain 8, targets 3 |
| Integrate | 1 | Vine Lance (`vine_lance`) | ambush | NEW replaces aura_twin_blades (ambush) | cost 11 %, cd 240 t, strain 16, targets 1 |
| Integrate | 2 | Heartwood Regrowth (`heartwood_regrowth`) | verdant_mend | NEW replaces corrupted_coil_burst (draconic_pulse) | cost 12 %, cd 480 t, strain 16, targets 1 |

- Worst case: no entity is spawned; at most 3 foes touched per use; both Integrate abilities used on cooldown at full mastery: 0.14 uses per second, at most 0.14 pictures per second to nearby players (the new kinds send at most 2 per use, the old kinds as before); particles go through the perf budgets.
- Not checked: the look in a game (no model or texture was changed), the balance against other wills, and the canon beyond the pages listed above.

### Lion Will (`lion_will`, rank A, CANON)

- Source: TBATE Fandom wiki (English, MediaWiki API): Curtis Glayder/Abilities, Mana Beasts
- Canon fact: World Lion is an A-class mana beast; Curtis formed an equal contract with the lion Grawder (Mana Beasts, Curtis Glayder/Abilities).
- Canon fact: In his first phase his hair becomes larger and bushier like a lion's, spreading to his face; he has little control over it and uses it as a last resort.
- Canon fact: World Howl: mounted on Grawder, mana is built up and fired as a condensed beam that shreds what it meets; it takes a great amount of mana and needs Grawder for full power.
- Note: the beam is built as the existing cone howl; the lion beside you is not built
- First form unlocks at 10 % assimilation (was 10 %); second form asks 50 % mastery (was 50 %).
- Changed: acquire strainPerSecond 0.22 (was 0.14)

| Form | Slot | Ability | Kind | Status | Numbers |
|---|---|---|---|---|---|
| Acquire | 1 | Pride's Poise (`pride_poise`) | mane_surge | KEPT  | cost 5 %, cd 600 t, strain 6, targets 1 |
| Acquire | 2 | Mane Guard (`mane_guard`) | bark_ward | NEW replaces cub_roar (world_howl) | cost 5 %, cd 480 t, strain 6, targets 1 |
| Integrate | 1 | Sovereign's Full Howl (`sovereigns_roar_call`) | world_howl | KEPT  | cost 18 %, cd 600 t, strain 24, targets 8 |
| Integrate | 2 | Royal Paw (`royal_paw`) | draconic_pulse | KEPT  | cost 9 %, cd 140 t, strain 12, targets 4 |

- Worst case: no entity is spawned; at most 8 foes touched per use; both Integrate abilities used on cooldown at full mastery: 0.20 uses per second, at most 0.20 pictures per second to nearby players (the new kinds send at most 2 per use, the old kinds as before); particles go through the perf budgets.
- Not checked: the look in a game (no model or texture was changed), the balance against other wills, and the canon beyond the pages listed above.

### Flame-Serpent Will (`flame_serpent_will`, rank A, TBATE-KIN)

- Source: TBATE Fandom wiki (English, MediaWiki API): Mana Beasts (Hades Serpent)
- Canon fact: Hades Serpent (AA class): a powerful serpent over 100 feet long with blue flames across its spine and matte grey scales (Mana Beasts).
- Canon fact: The Flame-Serpent Elder is the mod's own A-class relative, not a wiki creature (design).
- First form unlocks at 10 % assimilation (was 10 %); second form asks 50 % mastery (was 50 %).

| Form | Slot | Ability | Kind | Status | Numbers |
|---|---|---|---|---|---|
| Acquire | 1 | Pit-Viper Heat Sense (`pit_heat_sense`) | dragon_sight | KEPT  | cost 4 %, cd 300 t, strain 4, targets 6 |
| Acquire | 2 | Cinder Tail Whip (`cinder_tail_whip`) | tail_sweep | NEW replaces heat_haze (shadow_veil) | cost 5 %, cd 260 t, strain 6, targets 3 |
| Integrate | 1 | Magma Spit (`magma_spit`) | venom_spit | KEPT  | cost 10 %, cd 140 t, strain 14, targets 3 |
| Integrate | 2 | Spine-Flame Burst (`spine_flame_burst`) | draconic_pulse | NEW replaces molten_coil (restraining_vines) | cost 12 %, cd 180 t, strain 16, targets 5 |

- Worst case: no entity is spawned; at most 6 foes touched per use; both Integrate abilities used on cooldown at full mastery: 0.28 uses per second, at most 0.28 pictures per second to nearby players (the new kinds send at most 2 per use, the old kinds as before); particles go through the perf budgets.
- Not checked: the look in a game (no model or texture was changed), the balance against other wills, and the canon beyond the pages listed above.

### Ridge-Elder Fang Will (`ridge_wolf_will`, rank A, TBATE-KIN)

- Source: TBATE Fandom wiki (English, MediaWiki API): Mana Beasts (Black-fanged Wolf)
- Canon fact: Black-fanged Wolf (B class): large wolf-like mana beasts with fire-resistant fur, can eat rocks, move in packs, hunt strategically and evaluate prey before pouncing (Mana Beasts).
- Canon fact: The Ridge-Wolf Elder is the mod's own A-class relative (design); fire-resistant fur is not built (no allowed attribute).
- First form unlocks at 10 % assimilation (was 10 %); second form asks 50 % mastery (was 50 %).

| Form | Slot | Ability | Kind | Status | Numbers |
|---|---|---|---|---|---|
| Acquire | 1 | Scent Trail (`scent_trail`) | dragon_sight | KEPT  | cost 3 %, cd 600 t, strain 4, targets 5 |
| Acquire | 2 | Pack Surge (`pack_surge`) | mane_surge | NEW replaces lope_burst (shadow_dash) | cost 5 %, cd 420 t, strain 6, targets 1 |
| Integrate | 1 | Hamstring Lunge (`hamstring_lunge`) | ambush | KEPT  | cost 9 %, cd 200 t, strain 12, targets 1 |
| Integrate | 2 | Pack Call (`pack_call`) | world_howl | KEPT  | cost 11 %, cd 450 t, strain 14, targets 8 |

- Worst case: no entity is spawned; at most 8 foes touched per use; both Integrate abilities used on cooldown at full mastery: 0.16 uses per second, at most 0.16 pictures per second to nearby players (the new kinds send at most 2 per use, the old kinds as before); particles go through the perf budgets.
- Not checked: the look in a game (no model or texture was changed), the balance against other wills, and the canon beyond the pages listed above.

### Wind-Skimmer Raptor Will (`wind_skimmer_will`, rank A, TBATE-KIN)

- Source: TBATE Fandom wiki (English, MediaWiki API): Mana Beasts (Thunder Hawk)
- Canon fact: Thunder Hawk: a mana beast that lifts forest hounds from the ground and kills them in the air; lives in large canopy nests (Mana Beasts).
- Canon fact: The Gale Skimmer Elder is the mod's own A-class relative (design); the chain bolts are the design twist.
- First form unlocks at 10 % assimilation (was 10 %); second form asks 50 % mastery (was 50 %).

| Form | Slot | Ability | Kind | Status | Numbers |
|---|---|---|---|---|---|
| Acquire | 1 | Skim Step (`skim_step`) | shadow_dash | KEPT  | cost 3 %, cd 100 t, strain 4, targets 1 |
| Acquire | 2 | Wingbeat Gust (`wingbeat_gust`) | world_howl | NEW replaces cloud_veil (shadow_veil) | cost 6 %, cd 400 t, strain 6, targets 3 |
| Integrate | 1 | Storm-Hawk Bolt (`storm_hawk_bolt`) | thunder_chain | NEW replaces wingbeat_burst (draconic_pulse) | cost 11 %, cd 240 t, strain 14, targets 4 |
| Integrate | 2 | Cyclone Wake (`cyclone_wake`) | maelstrom | KEPT  | cost 12 %, cd 320 t, strain 14, targets 6 |

- Worst case: no entity is spawned; at most 6 foes touched per use; both Integrate abilities used on cooldown at full mastery: 0.16 uses per second, at most 0.25 pictures per second to nearby players (the new kinds send at most 2 per use, the old kinds as before); particles go through the perf budgets.
- Not checked: the look in a game (no model or texture was changed), the balance against other wills, and the canon beyond the pages listed above.

### Ammit Will (`ammit_will`, rank S, MYTH)

- Source: Wikipedia (English, MediaWiki API): Ammit
- Canon fact: Ancient Egyptian goddess 'Devourer of the Dead' with the forequarters of a lion, the hindquarters of a hippopotamus and the head of a crocodile.
- Canon fact: Waits in the Duat to learn whether she may consume the heart of the deceased.
- First form unlocks at 15 % assimilation (was 10 %); second form asks 60 % mastery (was 50 %).

| Form | Slot | Ability | Kind | Status | Numbers |
|---|---|---|---|---|---|
| Acquire | 1 | Weighing Glance (`weighing_glance`) | dragon_sight | KEPT  | cost 4 %, cd 500 t, strain 6, targets 5 |
| Acquire | 2 | Hippo Hide (`hippo_hide`) | bark_ward | KEPT  | cost 4 %, cd 500 t, strain 5, targets 1 |
| Integrate | 1 | Death Roll Snap (`death_roll_snap`) | ambush | KEPT  | cost 12 %, cd 260 t, strain 22, targets 1 |
| Integrate | 2 | Devour the Heart (`heart_devour`) | devour | NEW replaces heart_feast (verdant_mend) | cost 12 %, cd 320 t, strain 18, targets 1 |

- Worst case: no entity is spawned; at most 5 foes touched per use; both Integrate abilities used on cooldown at full mastery: 0.15 uses per second, at most 0.22 pictures per second to nearby players (the new kinds send at most 2 per use, the old kinds as before); particles go through the perf budgets.
- Not checked: the look in a game (no model or texture was changed), the balance against other wills, and the canon beyond the pages listed above.

### Apophis Will (`apophis_will`, rank SS, MYTH)

- Source: Wikipedia (English, MediaWiki API): Apep
- Canon fact: Usually shown as a giant snake, 'the Lord of Chaos', greatest enemy of the sun god Ra.
- Canon fact: The Coffin Texts imply a magical gaze that overwhelms Ra and his entourage.
- Canon fact: The old 'swallow the sun' time stop is not in the extract and is replaced by a coil sweep (only the dragon and the sphinx keep a time ability).
- First form unlocks at 20 % assimilation (was 10 %); second form asks 70 % mastery (was 50 %).

| Form | Slot | Ability | Kind | Status | Numbers |
|---|---|---|---|---|---|
| Acquire | 1 | Dusk Slither (`dusk_slither`) | shadow_veil | KEPT  | cost 5 %, cd 400 t, strain 5, targets 1 |
| Acquire | 2 | Coil Lash (`coil_lash`) | vine_lash | KEPT  | cost 4 %, cd 120 t, strain 5, targets 2 |
| Integrate | 1 | Mind-Stopping Gaze (`mind_stopping_gaze`) | dread_gaze | KEPT  | cost 10 %, cd 500 t, strain 18, targets 6 |
| Integrate | 2 | Coil of Chaos (`coil_of_chaos`) | tail_sweep | NEW replaces sun_swallowed (time_exclusion) | cost 12 %, cd 180 t, strain 16, targets 6 |

- Worst case: no entity is spawned; at most 6 foes touched per use; both Integrate abilities used on cooldown at full mastery: 0.17 uses per second, at most 0.29 pictures per second to nearby players (the new kinds send at most 2 per use, the old kinds as before); particles go through the perf budgets.
- Not checked: the look in a game (no model or texture was changed), the balance against other wills, and the canon beyond the pages listed above.

### Cerberus Will (`cerberus_will`, rank S, MYTH)

- Source: Wikipedia (English, MediaWiki API): Cerberus
- Canon fact: Multi-headed dog guarding the gates of the underworld, usually three heads, a serpent for a tail, snakes protruding from its body.
- Canon fact: Ovid's Cerberus has a venomous mouth; Euphorion associates it with the creation of the poisonous plant aconite.
- Canon fact: The old hellfire breath is not in the extract and is replaced by venom (design twist kept small).
- First form unlocks at 15 % assimilation (was 10 %); second form asks 60 % mastery (was 50 %).

| Form | Slot | Ability | Kind | Status | Numbers |
|---|---|---|---|---|---|
| Acquire | 1 | Threshold Guard (`threshold_guard`) | bark_ward | KEPT  | cost 6 %, cd 420 t, strain 6, targets 1 |
| Acquire | 2 | Hound Lunge (`hound_lunge`) | shadow_dash | KEPT  | cost 5 %, cd 80 t, strain 6, targets 1 |
| Integrate | 1 | Triple Howl (`triple_howl`) | world_howl | KEPT  | cost 14 %, cd 480 t, strain 24, targets 9 |
| Integrate | 2 | Venom Foam (`foam_venom`) | venom_spit | NEW replaces hellfire_breath (draconic_pulse) | cost 11 %, cd 170 t, strain 16, targets 4 |

- Worst case: no entity is spawned; at most 9 foes touched per use; both Integrate abilities used on cooldown at full mastery: 0.18 uses per second, at most 0.18 pictures per second to nearby players (the new kinds send at most 2 per use, the old kinds as before); particles go through the perf budgets.
- Not checked: the look in a game (no model or texture was changed), the balance against other wills, and the canon beyond the pages listed above.

### Crested Boar Will (`crested_boar_will`, rank A, MYTH)

- Source: Wikipedia (English, MediaWiki API): Erymanthian boar, Calydonian boar hunt
- Canon fact: Erymanthian boar: 'shaggy and wild', 'tameless', 'of vast weight' and 'foaming jaws'; the fourth labour of Heracles, driven into deep snow.
- Canon fact: Calydonian boar: sent by Artemis to ravage the region of Calydon.
- First form unlocks at 10 % assimilation (was 10 %); second form asks 50 % mastery (was 50 %).

| Form | Slot | Ability | Kind | Status | Numbers |
|---|---|---|---|---|---|
| Acquire | 1 | Crest Hide (`crest_hide`) | bark_ward | KEPT  | cost 4 %, cd 500 t, strain 5, targets 1 |
| Acquire | 2 | Rooting Gore (`rooting_gore`) | ambush | NEW replaces rooting_rush (shadow_dash) | cost 6 %, cd 300 t, strain 8, targets 1 |
| Integrate | 1 | Crest Gore (`crest_gore`) | ambush | KEPT  | cost 10 %, cd 260 t, strain 14, targets 1 |
| Integrate | 2 | Tusk Quake (`tusk_quake`) | draconic_pulse | KEPT  | cost 9 %, cd 160 t, strain 12, targets 6 |

- Worst case: no entity is spawned; at most 6 foes touched per use; both Integrate abilities used on cooldown at full mastery: 0.22 uses per second, at most 0.22 pictures per second to nearby players (the new kinds send at most 2 per use, the old kinds as before); particles go through the perf budgets.
- Not checked: the look in a game (no model or texture was changed), the balance against other wills, and the canon beyond the pages listed above.

### Cu Sith Will (`cu_sith_will`, rank A, MYTH)

- Source: Wikipedia (English, MediaWiki API): Cu-sith
- Canon fact: Dark green shaggy coat; capable of hunting silently but occasionally gives three terrifying barks and only three.
- Canon fact: Those who hear it must reach safety by the third bark or be overcome with terror to the point of death; it can take the souls of the living.
- First form unlocks at 10 % assimilation (was 10 %); second form asks 50 % mastery (was 50 %).

| Form | Slot | Ability | Kind | Status | Numbers |
|---|---|---|---|---|---|
| Acquire | 1 | Moor Mist (`moor_mist`) | shadow_veil | KEPT  | cost 6 %, cd 320 t, strain 6, targets 1 |
| Acquire | 2 | Hound's Scent (`hound_scent`) | dragon_sight | NEW replaces padfoot_dash (shadow_dash) | cost 4 %, cd 400 t, strain 5, targets 5 |
| Integrate | 1 | The Third Bark (`third_bark`) | world_howl | KEPT  | cost 14 %, cd 450 t, strain 20, targets 8 |
| Integrate | 2 | Death Omen (`death_omen`) | dread_gaze | KEPT  | cost 10 %, cd 360 t, strain 16, targets 4 |

- Worst case: no entity is spawned; at most 8 foes touched per use; both Integrate abilities used on cooldown at full mastery: 0.11 uses per second, at most 0.11 pictures per second to nearby players (the new kinds send at most 2 per use, the old kinds as before); particles go through the perf budgets.
- Not checked: the look in a game (no model or texture was changed), the balance against other wills, and the canon beyond the pages listed above.

### Fenrir Will (`fenrir_will`, rank S, MYTH)

- Source: Wikipedia (English, MediaWiki API): Fenrir
- Canon fact: Son of Loki, grows rapidly; the gods bound him with Gleipnir and he bit off Tyr's right hand.
- Canon fact: Foretold to kill Odin at Ragnarok and to be killed in turn by Vidar. Binding foes with the chain is a design twist.
- First form unlocks at 15 % assimilation (was 10 %); second form asks 60 % mastery (was 50 %).

| Form | Slot | Ability | Kind | Status | Numbers |
|---|---|---|---|---|---|
| Acquire | 1 | Sun-Chaser's Leap (`soul_light_leap`) | shadow_dash | KEPT  | cost 5 %, cd 80 t, strain 6, targets 1 |
| Acquire | 2 | Wolf Pounce (`wolf_pounce`) | ambush | NEW replaces loyal_surge (mane_surge) | cost 6 %, cd 300 t, strain 8, targets 1 |
| Integrate | 1 | Jaws of Ragnarok (`jaws_of_ragnarok`) | ambush | KEPT  | cost 14 %, cd 260 t, strain 22, targets 1 |
| Integrate | 2 | Gleipnir, the Ribbon That Binds (`gleipnir_snare`) | restraining_vines | KEPT  | cost 16 %, cd 520 t, strain 26, targets 5 |

- Worst case: no entity is spawned; at most 5 foes touched per use; both Integrate abilities used on cooldown at full mastery: 0.13 uses per second, at most 0.13 pictures per second to nearby players (the new kinds send at most 2 per use, the old kinds as before); particles go through the perf budgets.
- Not checked: the look in a game (no model or texture was changed), the balance against other wills, and the canon beyond the pages listed above.

### Hafgufa Will (`hafgufa_will`, rank S, MYTH)

- Source: Wikipedia (English, MediaWiki API): Hafgufa
- Canon fact: Described as a massive fish that looked more like an island than a living thing; its name means sea-reek or sea-steamer.
- Canon fact: It belches out food that attracts fish, then closes its mouth and devours them all at once.
- First form unlocks at 15 % assimilation (was 10 %); second form asks 60 % mastery (was 50 %).

| Form | Slot | Ability | Kind | Status | Numbers |
|---|---|---|---|---|---|
| Acquire | 1 | Barnacle Hide (`barnacle_hide`) | bark_ward | KEPT  | cost 5 %, cd 500 t, strain 5, targets 1 |
| Acquire | 2 | Island Disguise (`island_disguise`) | shadow_veil | NEW replaces lure_glow (dragon_sight) | cost 5 %, cd 400 t, strain 5, targets 1 |
| Integrate | 1 | Sweet Breath (`sweet_breath`) | dread_gaze | KEPT  | cost 10 %, cd 600 t, strain 16, targets 6 |
| Integrate | 2 | Great Swallow (`great_swallow`) | maelstrom | KEPT  | cost 15 %, cd 600 t, strain 20, targets 6 |

- Worst case: no entity is spawned; at most 6 foes touched per use; both Integrate abilities used on cooldown at full mastery: 0.07 uses per second, at most 0.07 pictures per second to nearby players (the new kinds send at most 2 per use, the old kinds as before); particles go through the perf budgets.
- Not checked: the look in a game (no model or texture was changed), the balance against other wills, and the canon beyond the pages listed above.

### Hydra Will (`hydra_will`, rank A, MYTH)

- Source: Wikipedia (English, MediaWiki API): Lernaean Hydra
- Canon fact: Poisonous breath and blood so virulent that even its scent was deadly.
- Canon fact: Later versions: for each head chopped off it regrows two; one head was immortal; Heracles burned the necks with fire.
- First form unlocks at 10 % assimilation (was 10 %); second form asks 50 % mastery (was 50 %).
- Changed: passive: hp +2 (was none)

| Form | Slot | Ability | Kind | Status | Numbers |
|---|---|---|---|---|---|
| Acquire | 1 | Marsh Fang (`marsh_fang`) | venom_spit | KEPT  | cost 3 %, cd 160 t, strain 4, targets 1 |
| Acquire | 2 | Scab-Over (`scab_over`) | verdant_mend | KEPT  | cost 4 %, cd 600 t, strain 4, targets 1 |
| Integrate | 1 | Nine-Head Strike (`nine_head_strike`) | vine_lash | KEPT  | cost 9 %, cd 100 t, strain 12, targets 3 |
| Integrate | 2 | Heads Regrow (`heads_regrow`) | verdant_mend | KEPT  | cost 14 %, cd 500 t, strain 16, targets 1 |

- Worst case: no entity is spawned; at most 3 foes touched per use; both Integrate abilities used on cooldown at full mastery: 0.27 uses per second, at most 0.27 pictures per second to nearby players (the new kinds send at most 2 per use, the old kinds as before); particles go through the perf budgets.
- Not checked: the look in a game (no model or texture was changed), the balance against other wills, and the canon beyond the pages listed above.

### Jormungandr Will (`jormungandr_will`, rank S, MYTH)

- Source: Wikipedia (English, MediaWiki API): Jormungandr
- Canon fact: An unfathomably large sea serpent that grew until it surrounded the earth and grasped its own tail.
- Canon fact: Thor's archenemy; they kill each other at Ragnarok, Thor dying of the serpent's venom after nine paces; it sprays poison over air and water.
- First form unlocks at 15 % assimilation (was 10 %); second form asks 60 % mastery (was 50 %).

| Form | Slot | Ability | Kind | Status | Numbers |
|---|---|---|---|---|---|
| Acquire | 1 | Eel Slip (`eel_slip`) | shadow_dash | KEPT  | cost 4 %, cd 120 t, strain 4, targets 1 |
| Acquire | 2 | Brine Fang (`brine_fang`) | venom_spit | KEPT  | cost 5 %, cd 200 t, strain 6, targets 2 |
| Integrate | 1 | World Coil (`world_coil`) | restraining_vines | KEPT  | cost 14 %, cd 700 t, strain 20, targets 6 |
| Integrate | 2 | The Ocean Rises (`ocean_rises`) | maelstrom | KEPT  | cost 14 %, cd 400 t, strain 18, targets 6 |

- Worst case: no entity is spawned; at most 6 foes touched per use; both Integrate abilities used on cooldown at full mastery: 0.09 uses per second, at most 0.09 pictures per second to nearby players (the new kinds send at most 2 per use, the old kinds as before); particles go through the perf budgets.
- Not checked: the look in a game (no model or texture was changed), the balance against other wills, and the canon beyond the pages listed above.

### Kitsune Will (`kitsune_will`, rank S, MYTH)

- Source: Wikipedia (English, MediaWiki API): Kitsune
- Canon fact: A fox or fox spirit with the supernatural ability to shapeshift or bewitch other life forms; as many as nine tails.
- Canon fact: Reputed to fire off kitsunebi, 'fox-fire'; keeps a luminous jewel in its tail.
- First form unlocks at 15 % assimilation (was 10 %); second form asks 60 % mastery (was 50 %).

| Form | Slot | Ability | Kind | Status | Numbers |
|---|---|---|---|---|---|
| Acquire | 1 | Fox Glamour (`fox_glamour`) | shadow_veil | KEPT  | cost 5 %, cd 320 t, strain 6, targets 1 |
| Acquire | 2 | Bewitching Glance (`bewitching_glance`) | dread_gaze | NEW replaces fox_hop (shadow_dash) | cost 6 %, cd 500 t, strain 8, targets 3 |
| Integrate | 1 | Nine-Tail Mirage (`nine_tail_mirage`) | dread_gaze | KEPT  | cost 14 %, cd 360 t, strain 22, targets 6 |
| Integrate | 2 | Kitsunebi Flare (`kitsunebi_flare`) | draconic_pulse | NEW replaces kitsunebi_ring (maelstrom) | cost 12 %, cd 180 t, strain 16, targets 5 |

- Worst case: no entity is spawned; at most 6 foes touched per use; both Integrate abilities used on cooldown at full mastery: 0.19 uses per second, at most 0.19 pictures per second to nearby players (the new kinds send at most 2 per use, the old kinds as before); particles go through the perf budgets.
- Not checked: the look in a game (no model or texture was changed), the balance against other wills, and the canon beyond the pages listed above.

### Kraken Will (`kraken_will`, rank S, MYTH)

- Source: Wikipedia (English, MediaWiki API): Kraken
- Canon fact: Described as a huge octopus-like creature with a reputation for pulling down ships.
- Canon fact: Its submerging was said to make a whirlpool compared to Norway's Moskstraumen, 'the Maelstrom'. The ink cloud is not in the extract (design).
- First form unlocks at 15 % assimilation (was 10 %); second form asks 60 % mastery (was 50 %).

| Form | Slot | Ability | Kind | Status | Numbers |
|---|---|---|---|---|---|
| Acquire | 1 | Ink Cloud (`ink_cloud`) | shadow_veil | KEPT  | cost 4 %, cd 450 t, strain 4, targets 1 |
| Acquire | 2 | Sucker Grip (`sucker_grip`) | restraining_vines | NEW replaces jet_dash (shadow_dash) | cost 6 %, cd 600 t, strain 8, targets 3 |
| Integrate | 1 | Tentacle Sweep (`tentacle_sweep`) | tail_sweep | NEW replaces tentacle_grip (restraining_vines) | cost 11 %, cd 170 t, strain 15, targets 6 |
| Integrate | 2 | Whirlpool Drag (`whirlpool_drag`) | maelstrom | KEPT  | cost 13 %, cd 500 t, strain 18, targets 5 |

- Worst case: no entity is spawned; at most 6 foes touched per use; both Integrate abilities used on cooldown at full mastery: 0.18 uses per second, at most 0.31 pictures per second to nearby players (the new kinds send at most 2 per use, the old kinds as before); particles go through the perf budgets.
- Not checked: the look in a game (no model or texture was changed), the balance against other wills, and the canon beyond the pages listed above.

### Minotaur Will (`minotaur_will`, rank A, MYTH)

- Source: Wikipedia (English, MediaWiki API): Minotaur
- Canon fact: Head and tail of a bull on the body of a man, also called Asterion or Asterius; dwelt at the centre of the Labyrinth built by Daedalus.
- Canon fact: Killed by Theseus who found his way with Ariadne's thread. The axe quake is design (the labrys is not in the extract).
- First form unlocks at 10 % assimilation (was 10 %); second form asks 50 % mastery (was 50 %).

| Form | Slot | Ability | Kind | Status | Numbers |
|---|---|---|---|---|---|
| Acquire | 1 | Ram Charge (`ram_charge`) | shadow_dash | KEPT  | cost 4 %, cd 80 t, strain 6, targets 1 |
| Acquire | 2 | Maze Sense (`maze_sense`) | dragon_sight | KEPT  | cost 4 %, cd 600 t, strain 5, targets 4 |
| Integrate | 1 | Rage of Asterion (`rage_of_asterion`) | mane_surge | KEPT  | cost 14 %, cd 320 t, strain 24, targets 1 |
| Integrate | 2 | Labrys Quake (`labrys_quake`) | maelstrom | KEPT  | cost 16 %, cd 300 t, strain 26, targets 6 |

- Worst case: no entity is spawned; at most 6 foes touched per use; both Integrate abilities used on cooldown at full mastery: 0.14 uses per second, at most 0.14 pictures per second to nearby players (the new kinds send at most 2 per use, the old kinds as before); particles go through the perf budgets.
- Not checked: the look in a game (no model or texture was changed), the balance against other wills, and the canon beyond the pages listed above.

### Nidhoggr Will (`nidhoggr_will`, rank S, MYTH)

- Source: Wikipedia (English, MediaWiki API): Nidhogg
- Canon fact: A Germanic dragon who gnaws at the roots of the world tree Yggdrasil.
- Canon fact: Its name may refer to chewing the corpses of the inhabitants of Niflheim, the guilty of murder, adultery and oath-breaking ('Malice Biter').
- First form unlocks at 15 % assimilation (was 10 %); second form asks 60 % mastery (was 50 %).

| Form | Slot | Ability | Kind | Status | Numbers |
|---|---|---|---|---|---|
| Acquire | 1 | Rot Bark (`rot_bark`) | bark_ward | KEPT  | cost 5 %, cd 600 t, strain 6, targets 1 |
| Acquire | 2 | Carrion Feast (`carrion_feast`) | verdant_mend | KEPT  | cost 5 %, cd 800 t, strain 5, targets 1 |
| Integrate | 1 | Corpse Gnaw (`corpse_gnaw`) | devour | NEW replaces withering_breath (venom_spit) | cost 12 %, cd 320 t, strain 18, targets 1 |
| Integrate | 2 | Root Gnaw (`root_gnaw`) | restraining_vines | KEPT  | cost 12 %, cd 800 t, strain 16, targets 5 |

- Worst case: no entity is spawned; at most 5 foes touched per use; both Integrate abilities used on cooldown at full mastery: 0.10 uses per second, at most 0.17 pictures per second to nearby players (the new kinds send at most 2 per use, the old kinds as before); particles go through the perf budgets.
- Not checked: the look in a game (no model or texture was changed), the balance against other wills, and the canon beyond the pages listed above.

### Nue Will (`nue_will`, rank A, MYTH)

- Source: Wikipedia (English, MediaWiki API): Nue
- Canon fact: In the Tale of the Heike: head of a monkey, limbs of a tiger, body of a Japanese raccoon dog and the front half of a snake as a tail.
- Canon fact: Shape-shifts into a black cloud that flies; its eerie 'hyoo hyoo' cry; the emperor fell ill under it. The old lightning is not in the extract and is dropped.
- First form unlocks at 10 % assimilation (was 10 %); second form asks 50 % mastery (was 50 %).

| Form | Slot | Ability | Kind | Status | Numbers |
|---|---|---|---|---|---|
| Acquire | 1 | Serpent-Tail Lash (`serpent_tail_lash`) | tail_sweep | NEW replaces tiger_bound (shadow_dash) | cost 5 %, cd 240 t, strain 6, targets 3 |
| Acquire | 2 | Black-Cloud Cloak (`cloud_cloak`) | shadow_veil | KEPT  | cost 4 %, cd 400 t, strain 5, targets 1 |
| Integrate | 1 | Tail-Snake Venom (`tail_snake_venom`) | venom_spit | NEW replaces black_cloud_lightning (draconic_pulse) | cost 10 %, cd 170 t, strain 14, targets 3 |
| Integrate | 2 | Ominous Night-Cry (`night_cry`) | dread_gaze | KEPT  | cost 12 %, cd 360 t, strain 20, targets 5 |

- Worst case: no entity is spawned; at most 5 foes touched per use; both Integrate abilities used on cooldown at full mastery: 0.19 uses per second, at most 0.19 pictures per second to nearby players (the new kinds send at most 2 per use, the old kinds as before); particles go through the perf budgets.
- Not checked: the look in a game (no model or texture was changed), the balance against other wills, and the canon beyond the pages listed above.

### Pegasus Will (`pegasus_will`, rank A, MYTH)

- Source: Wikipedia (English, MediaWiki API): Pegasus
- Canon fact: A winged horse, usually a white stallion, foaled by Medusa; creator of the fountain Hippocrene on Mount Helicon.
- Canon fact: Zeus brought Pegasus to Olympus to carry his thunderbolts.
- First form unlocks at 10 % assimilation (was 10 %); second form asks 50 % mastery (was 50 %).

| Form | Slot | Ability | Kind | Status | Numbers |
|---|---|---|---|---|---|
| Acquire | 1 | Sky Leap (`sky_leap`) | shadow_dash | KEPT  | cost 5 %, cd 80 t, strain 6, targets 1 |
| Acquire | 2 | Star-Mane Gallop (`star_mane_gallop`) | mane_surge | KEPT  | cost 6 %, cd 400 t, strain 6, targets 1 |
| Integrate | 1 | Hippocrene Spring (`hippocrene_spring`) | verdant_mend | KEPT  | cost 16 %, cd 420 t, strain 22, targets 1 |
| Integrate | 2 | Thunderbearer's Bolts (`thunderbearer_bolts`) | thunder_chain | NEW replaces thunderbearer_hoof (draconic_pulse) | cost 12 %, cd 240 t, strain 16, targets 4 |

- Worst case: no entity is spawned; at most 4 foes touched per use; both Integrate abilities used on cooldown at full mastery: 0.15 uses per second, at most 0.24 pictures per second to nearby players (the new kinds send at most 2 per use, the old kinds as before); particles go through the perf budgets.
- Not checked: the look in a game (no model or texture was changed), the balance against other wills, and the canon beyond the pages listed above.

### Scylla and Charybdis Will (`scylla_charybdis_will`, rank A, MYTH)

- Source: Wikipedia (English, MediaWiki API): Scylla, Charybdis
- Canon fact: Scylla: a man-eating monster on one side of a narrow channel, six dog heads in the Suda; sailors who avoid Charybdis pass dangerously close to her.
- Canon fact: Charybdis swallowed a huge amount of water three times a day and belched it out, making whirlpools that drag a ship under.
- First form unlocks at 10 % assimilation (was 10 %); second form asks 50 % mastery (was 50 %).

| Form | Slot | Ability | Kind | Status | Numbers |
|---|---|---|---|---|---|
| Acquire | 1 | Snatch from the Deck (`snatch_from_deck`) | ambush | NEW replaces rock_snatch (shadow_dash) | cost 6 %, cd 300 t, strain 8, targets 1 |
| Acquire | 2 | Six-Neck Lash (`six_neck_lash`) | vine_lash | NEW replaces strait_rock (bark_ward) | cost 4 %, cd 110 t, strain 5, targets 2 |
| Integrate | 1 | Six Heads' Meal (`six_heads_meal`) | devour | NEW replaces six_heads_snap (draconic_pulse) | cost 12 %, cd 320 t, strain 18, targets 1 |
| Integrate | 2 | Charybdis Gulp (`charybdis_gulp`) | maelstrom | KEPT  | cost 13 %, cd 600 t, strain 17, targets 6 |

- Worst case: no entity is spawned; at most 6 foes touched per use; both Integrate abilities used on cooldown at full mastery: 0.11 uses per second, at most 0.18 pictures per second to nearby players (the new kinds send at most 2 per use, the old kinds as before); particles go through the perf budgets.
- Not checked: the look in a game (no model or texture was changed), the balance against other wills, and the canon beyond the pages listed above.

### Sphinx Will (`sphinx_will`, rank S, MYTH)

- Source: Wikipedia (English, MediaWiki API): Sphinx
- Canon fact: Greek sphinx: head of a woman, haunches of a lion, wings of a bird; challenges all she meets to a riddle and kills and eats those who fail (Oedipus).
- Canon fact: Sphinxes are guardians of temple entrances. Stopping time with silence is a design twist kept from the remade will.
- First form unlocks at 15 % assimilation (was 10 %); second form asks 60 % mastery (was 50 %).
- Changed: passive: tough +0.5 (was none)

| Form | Slot | Ability | Kind | Status | Numbers |
|---|---|---|---|---|---|
| Acquire | 1 | Stone Poise (`stone_poise`) | bark_ward | KEPT  | cost 6 %, cd 420 t, strain 6, targets 1 |
| Acquire | 2 | Riddling Stare (`riddling_stare`) | dread_gaze | NEW replaces truth_glance (dragon_sight) | cost 6 %, cd 500 t, strain 8, targets 3 |
| Integrate | 1 | Silence of Stone (`silence_of_stone`) | time_exclusion | KEPT  | cost 18 %, cd 700 t, strain 26, targets 5 |
| Integrate | 2 | Riddle of the Ages (`riddle_of_ages`) | dread_gaze | KEPT  | cost 14 %, cd 360 t, strain 24, targets 5 |

- Worst case: no entity is spawned; at most 5 foes touched per use; both Integrate abilities used on cooldown at full mastery: 0.09 uses per second, at most 0.09 pictures per second to nearby players (the new kinds send at most 2 per use, the old kinds as before); particles go through the perf budgets.
- Not checked: the look in a game (no model or texture was changed), the balance against other wills, and the canon beyond the pages listed above.

### Thunder-Fang Chimera Will (`thunder_fang_chimera_will`, rank S, MYTH)

- Source: Wikipedia (English, MediaWiki API): Chimera (mythology)
- Canon fact: Fire-breathing hybrid of different animal parts: a lion with a goat's head on its back and a snake's head at the tail end; killed by Bellerophon.
- Canon fact: The thunder is a design twist (the myth gives fire).
- First form unlocks at 15 % assimilation (was 10 %); second form asks 60 % mastery (was 50 %).

| Form | Slot | Ability | Kind | Status | Numbers |
|---|---|---|---|---|---|
| Acquire | 1 | Ram's Brace (`ram_brace`) | mane_surge | KEPT  | cost 5 %, cd 400 t, strain 6, targets 1 |
| Acquire | 2 | Serpent-Tail Sting (`serpent_tail_sting`) | venom_spit | NEW replaces static_step (shadow_dash) | cost 4 %, cd 180 t, strain 5, targets 2 |
| Integrate | 1 | Storm-Venom Spit (`storm_venom_spit`) | venom_spit | KEPT  | cost 10 %, cd 160 t, strain 12, targets 3 |
| Integrate | 2 | Thunder-Fang Chain (`thunder_fang_chain`) | thunder_chain | NEW replaces thunder_maelstrom (maelstrom) | cost 12 %, cd 220 t, strain 16, targets 4 |

- Worst case: no entity is spawned; at most 4 foes touched per use; both Integrate abilities used on cooldown at full mastery: 0.24 uses per second, at most 0.34 pictures per second to nearby players (the new kinds send at most 2 per use, the old kinds as before); particles go through the perf budgets.
- Not checked: the look in a game (no model or texture was changed), the balance against other wills, and the canon beyond the pages listed above.

### Typhon Will (`typhon_will`, rank SS, MYTH)

- Source: Wikipedia (English, MediaWiki API): Typhon
- Canon fact: Hesiod: a hundred snake heads that emitted fire and every kind of noise; fire flashed from his eyes; unkempt hair streaming on the wind.
- Canon fact: Nicander: many heads, hands and wings, with huge snake coils from his thighs. Father of destructive storm winds; lost to Zeus's thunderbolts.
- First form unlocks at 20 % assimilation (was 10 %); second form asks 70 % mastery (was 50 %).

| Form | Slot | Ability | Kind | Status | Numbers |
|---|---|---|---|---|---|
| Acquire | 1 | Storm Eye (`storm_eye`) | dragon_sight | KEPT  | cost 4 %, cd 400 t, strain 4, targets 6 |
| Acquire | 2 | Storm-Eye Bolts (`storm_eye_bolts`) | thunder_chain | NEW replaces gale_leap (shadow_dash) | cost 6 %, cd 300 t, strain 8, targets 3 |
| Integrate | 1 | Storm-Fire Breath (`storm_fire_breath`) | draconic_pulse | KEPT  | cost 14 %, cd 160 t, strain 16, targets 6 |
| Integrate | 2 | Typhoon Vortex (`typhoon_vortex`) | maelstrom | KEPT  | cost 16 %, cd 360 t, strain 20, targets 6 |

- Worst case: no entity is spawned; at most 6 foes touched per use; both Integrate abilities used on cooldown at full mastery: 0.20 uses per second, at most 0.20 pictures per second to nearby players (the new kinds send at most 2 per use, the old kinds as before); particles go through the perf budgets.
- Not checked: the look in a game (no model or texture was changed), the balance against other wills, and the canon beyond the pages listed above.

### Yamata no Orochi Will (`yamata_no_orochi_will`, rank SS, MYTH)

- Source: Wikipedia (English, MediaWiki API): Yamata no Orochi
- Canon fact: A legendary eight-headed and eight-tailed serpent; its body spans eight valleys and eight peaks, covered in moss, cypress and cedar, with blood oozing from its belly.
- Canon fact: Susanoo had it drink eightfold-refined sake and killed it; Kusanagi no Tsurugi was found in its tail.
- First form unlocks at 20 % assimilation (was 10 %); second form asks 70 % mastery (was 50 %).

| Form | Slot | Ability | Kind | Status | Numbers |
|---|---|---|---|---|---|
| Acquire | 1 | Eight-Valley Sight (`eight_valley_sight`) | dragon_sight | KEPT  | cost 4 %, cd 400 t, strain 4, targets 6 |
| Acquire | 2 | Eight-Head Lash (`eight_head_lash`) | vine_lash | NEW replaces serpent_dart (shadow_dash) | cost 4 %, cd 110 t, strain 5, targets 2 |
| Integrate | 1 | Eightfold Strike (`eightfold_strike`) | vine_lash | KEPT  | cost 10 %, cd 100 t, strain 12, targets 3 |
| Integrate | 2 | Valley Flood (`valley_flood`) | maelstrom | KEPT  | cost 14 %, cd 600 t, strain 18, targets 6 |

- Worst case: no entity is spawned; at most 6 foes touched per use; both Integrate abilities used on cooldown at full mastery: 0.26 uses per second, at most 0.26 pictures per second to nearby players (the new kinds send at most 2 per use, the old kinds as before); particles go through the perf budgets.
- Not checked: the look in a game (no model or texture was changed), the balance against other wills, and the canon beyond the pages listed above.

## Server cost, all together

- No ability spawns an entity, a lightning bolt or a block; the three new kinds only hit entities that are already there, through the same `around`, `cone`, `hit` and `BalanceLaw` helpers as the old kinds.
- `tail_sweep`: one world query round the caster, at most 6 targets, one picture and one particle burst. `thunder_chain`: one cone query and at most 4 small queries, at most 5 targets, one picture, one burst and short particle lines (4 each). `devour`: one cone query, one target, one picture, one burst, one heal that goes through `BalanceLaw`.
- Cooldowns are per kind and per will progress as before; the milestone factor only shortens them and never below half of the written value (`BeastWillRules.cooldown`) and never below the kind's own minimum in the data.
- The milestone line is one chat line per milestone, rate-limited by `BeastWillFeedback`; the first time it is only recorded. Per-holder state is one small map entry per will, dropped with the holder's run state.
- Loading: 27 small files at data reload, as before.

## Sources and what could not be fetched

- English first (rule 10): TBATE Fandom wiki through its MediaWiki `api.php` (Beast Will, Beast Tamers, Mana Beasts, Elderwood Guardian, Curtis Glayder/Abilities, Arthur Leywin/Abilities, Sylvia, Sylvie Leywin and others) and English Wikipedia through its `api.php` (introductory extracts of the mythology pages named per will).
- The Japanese wiki was not needed: every page above came back with enough text.
- Not used: the Hippocampus page was fetched but no will uses it. No other series wiki was queried for this task, because the 27 wills come only from TBATE, myth and folklore.
- Details that the fetched text does not give are written as design: the kraken's ink, the minotaur's axe quake, the thunder of the chimera, the Gleipnir snare, the sphinx's stilled time, and the three elder beasts (flame serpent, ridge wolf, gale skimmer) that are relatives of TBATE beasts and not wiki creatures.

## Not built, not checked

- Not built: a third phase for the dragon (Unleashed Realmheart), forged vs legacy insight, the Elderwood Guardian's mist illusions, the black-fanged wolf's fire-resistant fur (no allowed attribute), new models or textures (the looks are unchanged), and the separate `BeastSkillRules` skills.
- Not checked in a game: the three new kinds, the milestone line and the new tooltips were written and covered by unit tests, source-scan tests and the CI self-test `beastcanon`, but not seen on a screen. The build was not compiled in the session that wrote it (no Gradle there); CI is the first compile.

## Tests

- `BeastWillCanonTest` (pure rules), `BeastWillCanonDataTest` (the 27 canon files: validator, signatures, progression, caps, lang), `BeastWillCanonWiringTest` (source scan of the loader, config, abilities, manager and boot), `BeastWillRulesTest` (the kind table).
- Self-test prefix `beastcanon` (`BeastCanonSelfTests`): `catalog.load`, `rules.bounds`, `abilities.nospawn`.
- Regenerate everything with `python3 tools/beastwill/canon/build_canon.py`.
