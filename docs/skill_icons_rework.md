# Skill icon rework (v3): 121 repainted icons, 56 animated Ultimates

Study of the reference mods and of what was wrong: `docs/skill_icons_study.md`. Generator: `python tools/skills/gen_icons_v3.py` (Pillow + numpy, deterministic: two runs give byte-identical files). Preview sheets: `docs/skill_icons/`.

## What is built

* **Painter** (`tools/skills/icon3_lib.py`): every icon is painted at 128 px and brought down to 32 px, so the symbol is bevel-shaded (light from the top left, warm lights, cool shadows, a specular glint), has a dark outline, a thin dark rim between overlapping parts, a coloured rim glow, a soft aura, a drop shadow, and sits on a dark vignetted background with a lit centre. The **frame is drawn pixel-exact afterwards and says the class** (Tensura's convention): **Unique = cyan**, **Extra = gold / orange**, **Ultimate = royal gold with a violet inner line and four accent-colour jewels**. Corner pixels are transparent like Tensura's own icons.
* **Symbols** (`icon3_root.py`, `icon3_beasts.py`, `icon3_db.py`): one hand-built symbol per skill, in the skill's source's look (table below). A Beast Will / Dragon Ball pair shares its symbol: the Unique is the plain picture in the cyan frame, the Ultimate is the same emblem in the gold frame, and animated.
* **Animation** (Ultimates only, 56 icons): an **8-frame loop of 6 ticks per frame (2.4 s)**: the aura pulses, a glint sweeps once over the symbol, two sparkles orbit it, a shimmer circles the frame. Every effect is periodic and the glint leaves the canvas at both ends, so the loop is seamless (checked by `SkillIconsV3Test`). Uniques and Extras stay still (kept cheap; the owner said "if you want to").
* **Playing the loop**: skill icons are blitted by path, so vanilla `.png.mcmeta` animation cannot play them (it only works for atlas textures). `client/skillicons/SkillIconSwap` registers the existing `AnimatedIconTexture` (a dynamic texture that shows one 32 x 32 frame of the strip) **under the skill's own icon path**, exactly as `AnimatedIcons` does for the five Veldanava Ultimates, so no skill class and no ManasCore code changes. The `.mcmeta` next to each strip carries the frame time (read by the helper).
* **Settings (CLIENT, `config/nusmp-skillicons-client.toml`)**: `skillIcons.newStyle` (default **true**; **false = the icons of 4.4.0**, drawn from `textures/skill/classic/v440/`) and `skillIcons.animate` (default **true**; false = the still frame, nothing ticks). Looks only, so client configs; the server never reads them.
* **Data**: `assets/anime_multiverse/skill_icons.json` (written by the generator) lists the 121 styled ids and the 56 animated ones; the client helper, `SkillIconSelfTests` and `SkillIconsV3Test` all read it (`skill/icons/SkillIconIndex`, pure and Minecraft-free, validates every value).

## NEW / REPLACED / KEPT

| | What |
|---|---|
| **REPLACED** (same path, same id, same 32 x 32) | 121 icons `textures/skill/<id>.png`: **37 root skills** (14 Ultimates, 20 Uniques, 3 Extras), **54 Beast Will** (`bw_<key>`, `bw_<key>_unique`), **30 Dragon Ball** (`db_<id>`, `db_<id>_unique`). The old picture of each is kept (rule 2) in `textures/skill/classic/v440/<id>.png` and shown when `skillIcons.newStyle = false`. |
| **NEW** | 56 animated strips `textures/skill/anim/<id>_anim.png` (+ `.mcmeta`), `skill_icons.json`, `client/skillicons/{SkillIconSwap,SkillIconOptions}`, `skill/icons/{SkillIconIndex,SkillIconSelfTests}`, the generator and its modules, `SkillIconsV3Test`, the self-tests `skillicons.index` and `skillicons.bounds` (smoke list, before `scaling`), the setting `skillIcons.newStyle`/`animate`, this doc, the study and the sheets. |
| **KEPT** | the five Veldanava Ultimate icons and their animation (`AnimatedIcons`, untouched), the 107 `grimoire/` icons and the `icons/` layers (reviewed fine in `docs/skill_icons_v2.md`), the seven legacy icons of removed skills (`black_wings`, `gold_experience(_requiem)`, `instant_death`, `limitless`, `six_eyes`, `vector_manipulation`), `classic/` (the 3.86 originals), `hd/`, every skill id and registry order, every skill class (the Summoner / All Fiction / Faith-god reworks of other agents are not touched; their icon files were repainted like all others and can be repainted again by editing the one symbol function). |

## Motif per skill (the owner asked for the anime's own look)

Colours and symbols follow the series each skill comes from, drawn inside the house frame. **Honest limit: no wiki could be consulted in this session (rule 10), so every motif is from memory of the series and the skill's own description; entries marked (?) are the least certain.**

### Ultimates, root (animated)
| Id | Source | Motif and palette |
|---|---|---|
| `lord_of_summons` | Tensura-style Summoner line | crowned hexagram summoning circle, violet and gold, cyan core |
| `heros_argonaut` | DanMachi, Bell Cranel's Argonaut | the golden bell with white-gold light rays and sparkles |
| `hell_conqueror` | Fire Force (Shinra) | crowned skull in a pillar of orange flame, red eyes |
| `immortal_will` | (?) Fire Force / DanMachi spirit | phoenix: flame body, orange wings, halo |
| `adolla_burst` | Fire Force, Shinra's Adolla Burst | white boot inside an orange star burst, blue-white flame below |
| `all_fiction` | Book Maker line (?) | open violet book with loose pages flying out, white-violet glow |
| `the_almighty` | Tensura | crimson slit eye in a golden sun-ring (sunburst rays) |
| `the_visionary` | Tensura | cyan eye on a large white four-point star |
| `nen` | Hunter x Hunter | the glass of water with a floating leaf (water divination) and blue-white aura flames |
| `gate_of_babylon` | Fate (Gilgamesh) | golden ripple gate with swords and spears of gold and steel emerging, hexagram core |
| `ki` | Dragon Ball | golden-flame ki aura round a white-blue energy orb |
| `lord_of_gluttonous_greed` | Tensura / Fullmetal-style sins (?) | fanged maw swallowing a falling gold coin, dark crimson |
| `shishou` | the owner's Genesis blade | winged-guard longsword with glowing cyan runes and light blades |

### Uniques, root
`summoner` (summoning circle, three spirit orbs), `argonaut` (the plain silver-gold bell, DanMachi), `hell_walker` (flame boot, Fire Force), `unbreakable_will` (steel shield with a gold rim and a flame), `devils_footprints` (flaming footprint, Fire Force), `book_maker` (open book and quill), `airiel` (sword in wind swirls, teal; Aiz's Airiel, DanMachi), `liaris_freese` (three rising gold chevrons and a star: its rapid growth, DanMachi), `hati` (silver wolf with a gold crescent moon, Norse), `adaptability` (hexagon with two chasing arrows round an eye of light), `hestia` (hearth flame in a bronze brazier, DanMachi), `freya` (pink charm heart with silver wings, DanMachi), `hephaestus` (anvil and hammer with sparks, DanMachi), `loki` (green trickster mask with gold horns, grin), `wynn_fimbulvetr` (ice snowflake, Riveria's frost storm, DanMachi), `rea_laevateinn` (white-hot sword between two columns of flame, Lefiya, DanMachi), `arcs_ray` (gold bow and a beam of white light) (?), `futsunomitama` (katana under a pale moon, Mikoto's gravity magic, DanMachi), `uchide_no_kozuchi` (the golden magic mallet with coins and stars, Haruhime, DanMachi).

### Extras, root
`firebolt` (Bell's golden lightning bolt in red flame, DanMachi), `hawk_eye` (yellow raptor eye in a sight ring with feather lines), `spirit_card` (violet card with a ghost).

### Beast Will (27 beasts, each a Unique and an Ultimate; mythology, no anime source)
`ammit` Egyptian crocodile jaws, gold ring; `apophis` void-purple coil round a dark orb; `cerberus` three wolf heads, orange eyes; `crested_boar` boar head, tusks, mane crest; `cu_sith` green fae hound; `dragon` red dragon, gold horns and fangs; `elderwood_guardian` treant with glowing eyes; `fenrir` icy wolf, God of War blue soul-light (the owner's brief); `flame_serpent` flame-coloured serpent and fire; `hafgufa` island-backed whale in waves; `hydra` five serpent heads; `jormungandr` ouroboros ring; `kitsune` fox head with five tails and foxfire; `kraken` blue octopus; `lion` maned lion; `minotaur` bull head, nose ring, big horns; `nidhoggr` dark dragon under the roots of the world tree; `nue` storm cloud, lightning and a serpent tail; `pegasus` white horse with a wing; `ridge_wolf` grey wolf, dorsal crest; `scylla_charybdis` whirlpool and two tentacle heads; `shadow_panther` black-violet panther, green eyes, moon; `sphinx` striped gold-blue nemes with a uraeus; `thunder_fang_chimera` lion head with lightning horns and fangs; `typhon` storm-blue horned giant with serpent flanks and lightning; `wind_skimmer` swept-wing bird in wind rings; `yamata_no_orochi` eight serpent heads with red eyes and a sake ring.

### Dragon Ball (15 characters, Super-era look)
`db_goku` silver Ultra Instinct spiky hair and orange gi; `db_vegeta` black flame hair, blue Saiyan armour with white pauldrons; `db_gohan` Beast: long white hair, red eyes, purple suit; `db_piccolo` green Namekian, antennae, white cape and turban; `db_krillin` bald with the six incense dots, orange gi, a destructo-disc ring; `db_android18` blond bob, blue eyes, infinity rings; `db_android17` black hair, orange scarf, green barrier hexagon; `db_goten` kid with Goku's spikes, orange gi; `db_trunks` lavender hair, Capsule Corp patch; `db_tien` bald, the third eye glowing; `db_yamcha` scar, long hair, orange wolf-claw slashes; `db_chiaotzu` pale face with red cheeks and the topknot, psychic aura; `db_roshi` sunglasses, white beard, the turtle shell; `db_future_trunks` lavender hair, the sword behind the shoulder, black jacket; `db_bulma` the Dragon Radar with seven dragon-ball blips.

## Worst-case cost (rule 11)

* **Server: none.** No packet, no tick work, no entity, no world access. The server only reads the jar's index in the self-test.
* **Client, new style, stills (65 icons, all Uniques and Extras):** plain resource-pack textures: no code runs, 121 x about 2 KB of VRAM-resident 32 x 32 RGBA.
* **Client, animated (56 Ultimates):** one `AnimatedIconTexture` each. Per tick: an integer division per icon (56 per tick); a frame change (every 6 ticks, 8 frames) copies and uploads one 32 x 32 RGBA square (4 KiB): at most **56 x 3.33 = 187 uploads per second = 750 KiB per second**, never allocating. Nothing is drawn extra: one blit per icon, as before. `skillIcons.animate = false` removes all of it. The `skillicons.bounds` self-test fails the build above 400 uploads per second.
* **The once-a-second check** touches at most 256 ids (121 shipped): a map lookup each, no allocation; a resource reload resets the paths and the next check puts the textures back.
* **Memory:** 56 strips of 32 x 256 RGBA = 7 KiB each held for the animation (about 400 KiB), freed when the setting is switched off or on a reload.
* **Files:** 121 icons + 56 strips + 121 classic copies, about 2 KB each (under 0.6 MB in the jar).

## What could not be seen in a game

* Nothing was run in Minecraft or with Gradle in this session (CI is the compiler). The Java was re-read for compile errors; the pictures were judged on 3x to 8x contact sheets only, not in the skill list, the hotbar, hover cards or with a GUI scale other than 1.
* That ManasCore's lists draw the registered dynamic texture is inferred from the Veldanava Ultimates, which already rely on it (3.17.0); it was not re-verified.
* Sheets: `docs/skill_icons/{root,beast,dragonball}_{before,after}.png` (labelled, 3x) and `docs/skill_icons/animated_frames_1..4.png` (each row = one Ultimate, its 8 frames).
* Weakest results at 32 px (worth the owner's eye): Hafgufa (reads as a whale-submarine), Jormungandr's head, the Gohan / Goku / Vegeta / Trunks hair (spikes are soft), Yamata no Orochi (eight heads merge), Nue, Arcs Ray. The Dragon Ball faces are emblem-style busts, not model sheets. Every motif was drawn from memory of the series (no wiki), see the table; `arcs_ray`, `immortal_will`, `all_fiction`, `lord_of_gluttonous_greed` are the least sure.
* Not done: Grimoire (107) and `icons/` layers (already reviewed in v2), the seven legacy icons, the Veldanava five; no HD 128 px copies of the new icons (the card screen of Veldanava uses its own).

## Tests

`SkillIconsV3Test`: every root icon is repainted or a known legacy one (121 = 37 + 54 + 30); each is 32 x 32 with a transparent corner and an opaque centre, differs from its kept `v440` copy; the frame colour matches the class; exactly the Ultimates have a strip of 8 frames 32 px wide with a valid `.mcmeta` whose frames differ and whose loop is seamless; the index matches the manifest and bounds the upload rate; bad indexes parse to empty. Self-tests `skillicons.index` (every styled id is a registered skill and ships its picture, classic copy, strip and mcmeta) and `skillicons.bounds`.
