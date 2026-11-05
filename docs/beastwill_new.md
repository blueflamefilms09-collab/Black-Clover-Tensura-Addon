# The 18 later Beast Wills

Status: data, rules, look and pictures are written and checked offline (the real validator, 35 pure tests, texture safety). **Nothing has been seen in a game and gradle was not run.** See "Not verified" at the end.

Nine wills existed (Dragon, Shadow Panther, Elderwood Guardian, Lion, Thunder-Fang Chimera, Ridge-Wolf, Crested Boar, Wind-Skimmer, Flame-Serpent). The Thunder-Fang Chimera was NOT duplicated. This change adds 18 more next to them (`docs/beastwill.md` explains the system; `docs/beastwill_vfx.md` the spirit look).

## NEW / REPLACED / KEPT

- NEW: 18 will files `data/nusmp/beast_will/<id>.json`; 3 ability kinds (`venom_spit`, `maelstrom`, `dread_gaze`); 18 look kinds (10..27) with 36 spirit textures `particle/bwspirit_<stem>_a/_b.png`; `BeastWillPictures` (the picture table); `tools/lang/beastwill_new.json`; `BeastWillNewTest`; this doc.
- REPLACED: nothing. (Compare to rule 5: no existing will, kind, spirit or texture covers any of these 18.)
- KEPT, only appended to: the nine wills and their tints, kinds 1..9, the twelve ability kinds, the nine spirits (their PNGs are byte-for-byte untouched: the generator has `--new-only`), the old painter look for unknown wills.

## The wills

Rank = lowest beast rank that can give the core (EP: C 1,000, B 5,000, A 20,000, S 100,000). "Cost %, cooldown" are per ability. Balance: B wills sit with Flame-Serpent, A wills with Lion/Chimera, the three S wills between those and the Dragon, the three legendary wills are above the Dragon in what they ask and below it in how often they drop (see below).

| Will | Rank | Affinity | Absorb EP | Core chance | Acquire abilities | Integrate abilities |
|---|---|---|---|---|---|---|
| Fenrir Will | A | Soul | 26000 | 1% | soul_ward (bark_ward, 5%, 25s), spirit_leap (shadow_dash, 4%, 7s) | garm_rush (shadow_dash, 7%, 8s), soul_seal (restraining_vines, 11%, 40s) |
| Jormungandr Will | S | Ocean | 60000 | 0.6% | venom_tide (venom_spit, 4%, 9s), scale_ward (bark_ward, 5%, 30s) | world_coil (restraining_vines, 13%, 45s), sea_deluge (maelstrom, 14%, 50s) |
| Cerberus Will | A | Underworld | 22000 | 1% | guard_stare (dread_gaze, 5%, 30s), hound_lunge (shadow_dash, 3%, 6s) | triple_howl (world_howl, 12%, 45s), hellfire_breath (draconic_pulse, 9%, 8s) |
| Minotaur Will | B | Labyrinth | 9000 | 1% | horn_gore (vine_lash, 3%, 6s), bull_hide (bark_ward, 4%, 30s) | labyrinth_charge (shadow_dash, 6%, 8s), maze_snare (restraining_vines, 9%, 35s) |
| Hydra Will | B | Venom | 10000 | 1% | venom_fang (venom_spit, 3%, 8s), head_regrowth (verdant_mend, 6%, 40s) | many_heads (vine_lash, 8%, 7s), hydra_breath (venom_spit, 9%, 15s) |
| Cu Sith Will | B | Spectre | 9000 | 1% | ghost_veil (shadow_veil, 2%, 20s), moor_dash (shadow_dash, 3%, 6s) | death_pounce (ambush, 7%, 15s), harbinger_cry (dread_gaze, 9%, 35s) |
| Nidhoggr Will | S | Decay | 55000 | 0.6% | rot_spit (venom_spit, 4%, 9s), blight_ward (bark_ward, 5%, 30s) | withering_breath (venom_spit, 11%, 11s), root_gnaw (restraining_vines, 12%, 45s) |
| Hafgufa Will | S | Deep Sea | 52000 | 0.6% | barnacle_hide (bark_ward, 5%, 25s), deep_sight (dragon_sight, 4%, 30s) | great_swallow (maelstrom, 14%, 50s), island_back (bark_ward, 10%, 45s) |
| Kraken Will | A | Abyss | 24000 | 1% | tentacle_lash (vine_lash, 3%, 5s), ink_veil (shadow_veil, 3%, 25s) | tentacle_grip (restraining_vines, 10%, 35s), whirlpool_drag (maelstrom, 10%, 35s) |
| Typhon Will | S legendary | Storm Fire | 100000 | 0.2% | storm_roar (world_howl, 6%, 35s), serpent_bite (venom_spit, 5%, 8s) | storm_flame_breath (draconic_pulse, 9%, 7s), titan_roar (world_howl, 16%, 70s) |
| Scylla and Charybdis Will | A | Strait | 28000 | 1% | rock_heads (vine_lash, 3%, 5s), strait_ward (bark_ward, 4%, 30s) | scylla_heads (venom_spit, 9%, 10s), charybdis_pull (maelstrom, 12%, 45s) |
| Sphinx Will | A | Judgement | 26000 | 1% | riddle_gaze (dread_gaze, 5%, 30s), stone_poise (bark_ward, 4%, 30s) | truth_sight (dragon_sight, 6%, 30s), silence_of_stone (time_exclusion, 16%, 75s) |
| Pegasus Will | A | Light | 20000 | 1% | sky_leap (shadow_dash, 3%, 6s), radiant_mend (verdant_mend, 6%, 40s) | star_gallop (shadow_dash, 6%, 7s), divine_mend (verdant_mend, 10%, 45s) |
| Yamata no Orochi Will | S legendary | Flood | 80000 | 0.3% | two_fangs (venom_spit, 5%, 8s), river_scale (bark_ward, 5%, 30s) | eight_heads (venom_spit, 10%, 7s), blood_flood (maelstrom, 16%, 65s) |
| Kitsune Will | A | Illusion | 24000 | 1% | fox_glamour (shadow_veil, 2%, 20s), foxfire (draconic_pulse, 4%, 8s) | fox_step (shadow_dash, 5%, 6s), tails_of_dream (dread_gaze, 12%, 45s) |
| Nue Will | A | Dread | 28000 | 1% | ominous_cry (dread_gaze, 5%, 30s), night_mist (shadow_veil, 3%, 25s) | black_lightning (draconic_pulse, 9%, 8s), snake_tail (venom_spit, 7%, 12s) |
| Ammit Will | A | Judgement | 30000 | 1% | weighing_gaze (dread_gaze, 5%, 30s), heart_feast (verdant_mend, 7%, 45s) | crocodile_snap (ambush, 8%, 15s), devouring_judgement (restraining_vines, 12%, 40s) |
| Apophis Will | S legendary | Chaos | 90000 | 0.3% | mesmerizing_gaze (dread_gaze, 6%, 30s), chaos_spit (venom_spit, 5%, 8s) | chaos_coil (maelstrom, 12%, 40s), endless_night (time_exclusion, 20%, 90s) |

Legendary (Typhon, Yamata no Orochi, Apophis): drop chance 0.2 to 0.3 % (Dragon 0.4 %), assimilation 3.5 to 4 h (Dragon 3 h), day cap 12 % (Dragon 15 %), core cooldown 21 days (Dragon 14), integrate drain 1.55 to 1.6 %/s (Dragon 1.4). The validator already forces a legendary will to a day cap of 25 % or less and a drop chance of 1 % or less.

### Who they are and where the lore comes from

- **Fenrir Will** (`fenrir_will`, tint `#D9D4FF`): God of War (Ragnarok) story: Fenrir, Atreus' pet wolf whose soul is later sealed into Garm (godofwar.fandom.com and svg.com search snippets; move details not verified). Core keywords: fenrir, direwolf.
- **Jormungandr Will** (`jormungandr_will`, tint `#1E9AA8`): Norse myth: the Midgard Serpent that rings the world (ocean, venom). Core keywords: jormungandr, tempest_serpent.
- **Cerberus Will** (`cerberus_will`, tint `#9A3FD0`): Greek myth: three-headed hound guarding Hades. Core keywords: cerberus, hellhound.
- **Minotaur Will** (`minotaur_will`, tint `#B5482B`): Greek myth: the bull-headed man of the Labyrinth. Core keywords: minotaur, bull.
- **Hydra Will** (`hydra_will`, tint `#8FB82A`): Greek myth: the Lernaean Hydra, regrowing heads, venom, swamp. Core keywords: hydra.
- **Cu Sith Will** (`cu_sith_will`, tint `#7CFF4B`): Gaelic folklore: the green spectral hound, harbinger of death. Core keywords: cu_sith, cusith.
- **Nidhoggr Will** (`nidhoggr_will`, tint `#B8A82A`): Norse myth: the dragon gnawing Yggdrasil's roots. Core keywords: nidhogg.
- **Hafgufa Will** (`hafgufa_will`, tint `#2E62D0`): Norse and medieval bestiary: the island-sized sea monster that swallows ships. Core keywords: hafgufa.
- **Kraken Will** (`kraken_will`, tint `#0E7A66`): Scandinavian sea legend: the giant cephalopod dragging ships down. Core keywords: kraken.
- **Typhon Will** (`typhon_will`, tint `#E03CA0`): Greek myth: the deadliest monster, a hundred snake heads, storm and fire. Core keywords: typhon.
- **Scylla and Charybdis Will** (`scylla_charybdis_will`, tint `#7C93B0`): Greek myth (Odyssey): the many-headed rock monster and the whirlpool of the strait. Core keywords: scylla, charybdis.
- **Sphinx Will** (`sphinx_will`, tint `#E6D08A`): Greek and Egyptian myth: lion, bird and woman; the riddle. Core keywords: sphinx.
- **Pegasus Will** (`pegasus_will`, tint `#FFB8D8`): Greek myth: the divine winged stallion. Core keywords: pegasus.
- **Yamata no Orochi Will** (`yamata_no_orochi_will`, tint `#D81E3C`): Japanese myth (Kojiki): eight heads, eight tails, red eyes. Core keywords: orochi.
- **Kitsune Will** (`kitsune_will`, tint `#FF9A6A`): Japanese folklore: the multi-tailed fox, illusion and shapeshifting. Core keywords: kitsune, nine_tail.
- **Nue Will** (`nue_will`, tint `#56606E`): Japanese folklore (Heike): monkey head, tanuki body, tiger legs, snake tail; ominous cry. Core keywords: nue.
- **Ammit Will** (`ammit_will`, tint `#B0708A`): Egyptian myth: devourer of the dead, crocodile, lion and hippopotamus, eater of unworthy hearts. Core keywords: ammit.
- **Apophis Will** (`apophis_will`, tint `#6E2CF0`): Egyptian myth: the serpent of chaos and darkness that swallows the sun. Core keywords: apophis, apep.

Lore sources: the Fenrir entry comes from the game's story as found by web search (godofwar.fandom.com and svg.com snippets, English); **its move details are not verified and none were invented**: the will is built only from "pale soul-light, a soul sealed into Garm, the soul carried by Atreus' knife, loyal pet that protects its host", so its abilities are generic kinds (a ward, a rush, a soul seal) with names that fit. The other 17 follow the myths and folklore named above from general knowledge; no wiki page was fetched for them (rule 10: English wiki first, Japanese as fallback; **nothing was fetched at all for these 17**, so nobody should read them as canon of a specific game).

Fenrir is the God of War Fenrir, not the sun-devouring chain wolf. The will is called "Fenrir Will" (beast "Fenrir (God of War)") like every other will name; the lang key can be shortened to just "Fenrir" if the owner prefers.

## How a will's numbers are built

Data only, through the existing validator (`BeastWillSchema` / `BeastWillDef.problem()`): every file was parsed by the real validator offline and passes. Passive plus phase stats stay under the attribute caps at full strength without the runtime cap having to cut them (pinned by `BeastWillNewTest`). Creatures: `keywords` match Tensura-namespace entity paths (for example `tensura:pegasus`, `tensura:direwolf`, `tensura:tempest_serpent`); the other beasts have no Tensura entity, so they are given by the offer tag or `/beastwill` as before. Pegasus has no real flight (no creative flight is granted on purpose): it gets speed, jump height and a long safe fall.

## The three new ability kinds (appended, same caps style)

| Kind | What it does | Caps (duration ticks / radius / targets / min cooldown) |
|---|---|---|
| `venom_spit` | A cone in front of the caster: damage plus Poison (Wither from power 6) for the file's duration on at most `maxTargets` creatures. | 120 / 10 / 6 / 100 |
| `maelstrom` | Pulls foes inside the radius toward the caster, small damage, Slowness for a moment; a player target needs the shared hard-control cooldown (`BalanceLaw.beginControl`). | 100 / 10 / 6 / 240 |
| `dread_gaze` | Weakness and Slowness I on foes in the radius, and creatures that were hunting the caster lose their target. No hard control. | 160 / 16 / 6 / 300 |

All three go through the existing rules: `SongTargets.isFoe`, the team check, `mayInteract`, the `abilitiesHitPlayers` setting, `bossFactor` on damage and time, `BalanceLaw` for control, and the server's `maxTargetsCap`. No block, no entity, no chunk load (`BeastWillSourceScanTest` still passes). The other abilities reuse the twelve old kinds: wards, dashes, veils, ambush, lash, mend, restraining vines, howl, pulse, sight and the (bounded, never pausing) exclusion for Sphinx's "Silence of Stone" and Apophis' "Endless Night".

## Look

- **Style packing**: the aura style int keeps phase in bits 24..25 and the tint in 0..23. The look kind field was bits 26..29 (4 bits, 15 kinds); it is now bits 26..30 (5 bits, 31 kinds, 1..27 used, 0 unknown). Nothing else moved, bit 31 stays free, and an old style (kind 0, or any kind 1..9) decodes exactly as before (pinned in `BeastWillLookTest`). A kind the table does not list still reads as unknown and keeps the old look.
- **Core tint**: `BeastCoreLook` has the 18 new colours (equal to the file tints, all different from the nine and from each other; the closest pair is 20 on the CIE Lab scale, which is also the closest pair of the old nine).
- **Spirit art** (`tools/beastwill/gen_spirit_textures.py --new-only`, procedural, fixed seeds, white glow maps tinted by the painter): body `_a` and accent `_b` per will. Fenrir: a giant spectral wolf with runes cut into its flank and soul wisps leaving its back; the accent is a ring of Elder Futhark runes with soul wisps (the Nordic marks of the brief). Heads that multiply: Cerberus has all three heads painted into its body (and hellfire as the accent); Hydra, Typhon, Scylla and Charybdis and Yamata no Orochi paint 2, 3, 3 and 3 heads into the body and in the full phase-2 detail add 3, 3, 3 and 5 swaying extra head sprites (their `_b` is ONE head on a neck, drawn several times): Hydra 5 heads, Typhon 6 (plus the fringe of small snake heads in the body art), Scylla 6, Orochi 8 heads and 8 tails (the tails are in the body art).
- **Painter** (`client/aura/BeastWillAura`): the same structure; one accent colour and one speck mode per new kind (embers, ribbons, dust, falling leaves, shards, bolts).
- **Ability pictures** (`BeastWillPictures`): one table; each will gets a travelling shape and a burst shape out of the existing `VfxShape` list: Fenrir mist + soul vortex, Jormungandr water dragon + water nest, Cerberus dark crescent + crossed dark cuts, Minotaur earth rampage + shockwave, Hydra poison breath + water burst, Cu Sith dark iai + corpse raise, Nidhoggr dark thrust + nihility zone, Hafgufa gravity well, Kraken water javelin + cradle, Typhon fire spear + lightning pillar, Scylla earth spikes + tornado, Sphinx divine ray + pillar, Pegasus light blade + flare, Orochi blood arcs + sigil, Kitsune flame trail + dream dome, Nue space tear + crack, Ammit maw, Apophis dimension slash + black hole. No Time shape, no new shape.

## Worst-case cost (rule 11)

- **Per ability use** (server): at most 2 `VfxSpawn` packets (travel + burst, same as before; `BeastWillLookTest` still pins the sender count) and at most 12 `Fx` particles (a 10-particle tint puff plus at most 2). Energy, a cooldown of 100 to 1,800 ticks and strain bound the rate, so one player sends at most 2 packets per 5 s even on the fastest ability. Targets: at most 6 per ability (`maxTargets` capped by kind and by the server's cap), each at most a few effect applications; no per-tick work, nothing scheduled, nothing to clean up on logout, death or dimension change (no new state, no new map).
- **Per player in a form** (server): unchanged, one upkeep per second; the new wills add nothing per tick.
- **Network**: the aura style is the same one int (the field is 1 bit wider); no new packet.
- **Client draw**, per player per frame: unchanged cap, at most 22 quads in full detail (6 players a frame), at most 8 in light detail, nothing beyond 48 blocks, nothing from the owner's first-person camera. The four multi-head wills draw 3 to 5 head quads in place of the accent and eight specks, so their worst case is 18 quads. No allocation per frame (`BeastWillLookTest` pins `new` count and the speck loop).
- **Memory**: 36 new 256 x 256 textures (about 1.6 MB on disk, 36 x 256 KB of GPU memory when all are loaded; they load on first sight of that will).
- **Data**: 18 small files read once at data load.

## Tests

Changed: `BeastWillLookTest` (27 files, kind field 5 bits, a kind-31 unknown, old styles decode the same, the head cap), `BeastWillRulesTest` (15 kinds), `BeastCoreLookTest` (at least 27 files), `BeastWillSchemaTest` (the folder holds the 27 expected files; the 18 ids in `LATER`), `BeastWillSelfTests` (CI smoke: `beastwill.catalog.load` loads and validates all 18, `beastwill.look.kinds` needs a look kind and a picture for each). Added: `BeastWillNewTest` (validator, tint and kind uniqueness, ranks, legendary rules, Fenrir theme, caps, lang names, pictures, no Time shape). Run offline with a small JUnit stand-in: 35 tests pass (`BeastWillNewTest`, `BeastWillLookTest`, `BeastCoreLookTest`, `BeastWillRulesTest`, `BeastWillSchemaTest`) and `BeastWillSourceScanTest` 8 pass.

## Not verified

Gradle did not run, so `BeastWillAbilities`, `BeastWillAura`, `BeastWillSelfTests` and `BeastWillStateTest` were only syntax-parsed (no compile against Minecraft), and the full test sweep was not run; the shapes' argument conventions (which point is `from`/`to`, what `power` means) were read from the enum comments and not seen on screen; none of the spirits, pictures or the three new kinds was seen in a game; head sway placement, texture look in game and the balance numbers are unplaytested. The lang fragment `tools/lang/beastwill_new.json` is NOT merged into `en_us.json` (the coordinator merges it; until then names fall back to the data file name, which is the same text, and abilities show their raw id). The creative tab was not changed (it still lists four cores; `/beastwill core` gives any will).
