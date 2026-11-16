# The Unique / Ultimate skills in their anime look, wave 3

The owner asked for every skill's effects to look like the anime it comes from instead of particle sprays. Wave 1 did the Saiyan Ki techniques
(`ki_dragonball.md`), wave 2 the Black Sword Greed and the devil hits (`greed_devils.md`). This wave covers the Tensura-style Unique / Ultimate
skills in `skill/` (the `NUSkills` ids) whose look was the `SkillFx` facade (`Fx.burst` / `spray` / `aura`, a dotted particle beam) or a generic shape.

## What was built

- **`vfx/client/layer/SkillAnimeLayer.java`**: one layer with 9 new shapes, appended at the end of `VfxShape` after `DEVIL_HIT`:
  `SKILL_GATE, SKILL_FIREBOLT, SKILL_ARGONAUT, SKILL_FROST, SKILL_HOLY_FIRE, SKILL_ALLURE, SKILL_SCREW, SKILL_WINGS, SKILL_SIGIL`. It is registered last in
  `VfxManager`. `SKILL_SIGIL` is drawn by its helper **`SkillAnimeSigils.java`** (eight styles for the smaller skills).
- **`tools/gen_skill_anime_vfx_textures.py`**: makes 20 textures, `textures/particle/skillanime_*.png`. Deterministic, no fonts, white or grey so the vertex
  colour tints them. Generic pieces (core, glow, flash, streak, bolt, ring, flame: the Greed recipes, so the family stands alone) and the signature pieces:
  ripple (the Gate of Babylon), chain (Enkidu), bell (Argonaut), snowflake circle, ice shard, mist, rose petal, halo, sun rays, hearth sigil, sparkle,
  wing spike (Black Wings), swirl band (Ea).
- **Solid weapons and screws**: the Gate's swords / axes and All Fiction's screws are the shaded low-poly solids of `SolidParts` / `SolidModels`, never a
  flat cut-out (the owner removed those, `docs/visual/flat_props.md`). The screw is a new model in the layer (head, shaft, thread ridge, tip, about 13 quads).
- **`vfx/client/layer/SkillAnimeVfx.java`**: the pure half (texture list, mode decoding, sigil styles, the classic switch).
- **`skill/SkillAnimeFx.java`**: the server helper. Every new effect goes out through it, as one `VfxSpawn` packet. It also holds the `skill.vfx` self-test.
- **`vfx/client/SkillAnimeVfxConfig.java`**: the client setting (below).
- **Preview scenes**: `tools/vfx_preview/scenes.d/skill_anime.json` (25 scenes). Renders: `docs/vfx_remake/previews/skill_*.png`, contact sheet
  `docs/vfx_remake/previews/skill_anime_sheet.png`.

### How the power is read

`SKILL_GATE`, `SKILL_ARGONAUT` and `SKILL_HOLY_FIRE` pack a mode into the power in steps of 20 (`mode = power / 20`, the rest is the mode's own number, kept
under 19.5 so it never spills into the next mode). `SKILL_SIGIL`'s power is the style + 1 and its radius is the from -> to distance.
`SkillAnimeVfxAssetTest` pins the server encoding against the client decoding.

### The look

The Time Magic layer is the quality standard. Its techniques were copied, not its code: life ramps, eased opening (ease-out-back), hoops (ribbons on a
circle), seeded randomness so every client sees the same picture. Colours that must stay saturated on a bright sky (gold gates, fire glow, chains, ink
wings) are drawn with the alpha blend, light on top with the additive blend. The Time Magic files were not touched.

## Sources (rule 10)

The English wikis (`danmachi.fandom.com`, `typemoon.fandom.com`) and the Japanese fallback (`ja.wikipedia.org`) could not be reached from this session: the
network proxy refused the connection (403) and the fetch tool could not resolve the host. So every reference below comes from general knowledge of the
anime and **none of it is wiki-verified**. The least certain entries are marked. To have them checked, allow the fandom hosts and `ja.wikipedia.org`
in the session network settings.

## Per skill

| Skill (file) | Anime reference (unverified) | What is drawn now | Packets per cast |
|---|---|---|---|
| Gate of Babylon: Gate (`GateOfBabylonSkill.gate`) | Gilgamesh (Fate): golden ripples open in the air and weapons shoot out | `SKILL_GATE` mode 0, 1 gate: a gold ripple opens behind the caster, a solid gold sword leaves it and flies into the target, a flash | 1 |
| Gate of Babylon: Volley | the barrage of many gates | `SKILL_GATE` mode 0, gates = beams drawn + 2 (at most 8): ripples open one after another, two solid weapons and golden blades of light fly | 1 (was up to 6 beams, kept) |
| Gate of Babylon: Rain | weapons raining from gates in the sky | `SKILL_GATE` mode 1: gates open over the area facing down, each fires twice, a great solid sword falls in the middle and stands in the ground, a gold ring | 1 |
| Gate of Babylon: Chains | Enkidu, the Chains of Heaven | `SKILL_GATE` mode 2: four gates round the target, golden chains shoot from them and coil round it | 1 |
| Gate of Babylon: Gae | a spear fired from the Gate (look unverified) | `SKILL_GATE` mode 0, 1 gate along the line | 1 |
| Gate of Babylon: Ea | Enuma Elish, Ea's spiral of red wind | `SKILL_GATE` mode 3: a gate a step ahead, a crimson spiral (swirl hoops round an ink core) drills to the end, camera shake | 1 |
| Firebolt (`FireboltSkill`) | Bell Cranel's chantless Firebolt: an instant bolt of fire-lightning | `SKILL_FIREBOLT`: a jagged orange bolt with a white-hot core, re-rolled every 2 ticks, two forks, flame tongues along it, a flame burst and ring at the impact | 1 |
| Adolla Burst: Light Speed (`AdollaBurstSkill`) | Shinra's Adolla flames (Fire Force) | `SKILL_FIREBOLT` in the blue Adolla colour along the blink | 1 |
| Adolla Burst: Burst | the Adolla Burst flame explosion | `SKILL_SIGIL` FIRE_FORCE, blue: flame jets from the soles, a ring of ink-edged flames bursts to the radius | 1 |
| Devil's Footprints: Dash (`DevilsFootprintsSkill`) | Shinra's flaming feet ("devil's footprints") | `SKILL_SIGIL` FIRE_FORCE, orange, radius 2.5 | 1 |
| Argonaut: the charge (`ArgonautSkill`) | Bell's Argonaut: white light gathering at the hand with the chime of a bell | `SKILL_ARGONAUT` mode 0 (follows): motes spiral into the right hand, a bell of light rings there, a chime ring | 1 per charge step (at most the charge cap) |
| Argonaut: the release | the charged strike | `SKILL_ARGONAUT` mode 1: a great white crescent sweeps the cone, sparkles, a flash | 1 |
| Hero's Argonaut: the beam (`HerosArgonautSkill`) | Argonaut-charged Firebolt / the hero's light (look partly unverified) | `SKILL_ARGONAUT` mode 2: a white-gold beam with bell rings running along it, camera shake | 1 |
| Wynn Fimbulvetr (`WynnFimbulvetrSkill`) | Riveria's ice magic: everything frozen in a white blizzard | `SKILL_FROST`: a snowflake frost circle, ice spikes erupt one after another, two blizzard bands spin, mist and snow | 1 (the zone's pulses send none) |
| Hestia (`HestiaSkill`) | Hestia, goddess of the hearth: sacred fire (look unverified) | `SKILL_HOLY_FIRE` mode 0: a hearth sigil, a pillar of golden flame, a ring of small flames, embers, a column of light | 1 |
| Rea Laevateinn (`ReaLaevateinnSkill`) | Riveria's fire magic: pillars of flame across the field | `SKILL_HOLY_FIRE` mode 1 (follows the caster like the waves): flame pillars erupt round the caster one after another inside a ring | 1 for all waves (the per-wave `FIRE_PILLAR` shapes are kept) |
| Freya (`FreyaSkill`) | Freya's silver allure, her Charm | `SKILL_ALLURE`: a silver glow, silver waves, rose petals spiralling out, twinkles, a charm mark over her head | 1 |
| All Fiction: Fiction (`AllFictionSkill.erase`) | Kumagawa's giant screws (Medaka Box) | `SKILL_SCREW` from -> to: a giant solid screw is driven into the target and stays | 1 |
| All Fiction: every other mode | "It's All Fiction", screws everywhere | `SKILL_SCREW` from = to: five screws slam down round the caster, a dark violet ring | 1 |
| ~~Faith / Goddess of Faith~~ (skills removed, `docs/faith_removed.md`) | (mod's own) | `SKILL_SIGIL` FAITH / GODDESS: the styles and their layer stay, nothing sends them any more | none |
| ~~Black Wings~~ (skill removed, `docs/black_wings_removed.md`) | Accelerator's black wings | `SKILL_WINGS`: the shape and its layer stay, nothing sends it any more | none |
| Hephaestus (`HephaestusSkill`) | Hephaestus, the smith goddess (look unverified) | `SKILL_SIGIL` FORGE: a fountain of hot sparks, two hammer-strike rings, a heat ring | 1 |
| Uchide no Kozuchi (`UchideNoKozuchiSkill`) | Haruhime's level boost: a golden glow (look unverified) | `SKILL_SIGIL` MALLET: three gold rings climb the body, sparkles rain, a star over the head | 1 |
| Unbreakable / Immortal Will | (mod's own) | `SKILL_SIGIL` WILL: golden flames burst up, a shockwave to the radius | 1 (Immortal: 2 on a refused death, rally 1) |
| Hell Walker / Hell Conqueror | (mod's own; hellfire) | `SKILL_SIGIL` HELL: a seal cracked into the ground, ink hellfire with red hearts, embers | 1 on switch-on; Hell Conqueror 1 per hellfire pulse (a second apart, where `FIRE_WILD` already went) |
| Futsunomitama (`FutsunomitamaSkill`) | Mikoto's gravity magic (look unverified) | `SKILL_SIGIL` GRAVITY: a violet seal, pressure rings fall and crush down, dark streaks fall | 1 |

### Skills left for a later wave

- Not drawn yet (old look kept): Loki (already a `SPATIAL_RIFT` shape), Arcs Ray (already `MIRROR_RAY` + `LIGHT_FLARE`), Airiel (already the wind shapes),
  Liaris Freese, Hati, Hawk Eye, Spirit Card, Summoner / Lord of Summons (card summons), Book Maker, The Almighty / The Visionary (already shaped),
  Shishou, the Gate's Armour / Vimana / King's Aura / Elixir modes, Devil's Footprints' kick.
- **Six Eyes, Limitless (Hollow Purple), Gold Experience (Requiem), Vector Manipulation (as a skill) and Instant Death** were removed by the owner in 3.44.0
  (their ids are inert stubs, `LegacyIds`), so there is nothing to draw. Black Wings (3.44.0 kept it as a grant-only skill) was removed later too, see `docs/black_wings_removed.md`.
- Dragon Ball, the Veldanava skills and Black Sword Greed were already done.

## The classic setting

`config/nusmp-skill-anime-vfx-client.toml`, `skills.classicVfx`, CLIENT (looks only), default `false`. `true` makes the nine `SKILL_*` shapes draw nothing, so
the player sees exactly the 3.98.0 look. The server always sends both the new shapes and the old effects; nothing on the server was removed. The value is
mirrored into `SkillAnimeVfx` when the file loads or reloads. `ConfigFileNamesTest` sees the new literal file name.

## Cost (rule 11)

**Server**

- No entity, no block change, no new query, no map, no per-player state. One `static boolean` (the self-test registration).
- Each cast adds 1 effect packet (about 60 bytes) through `VfxSpawn.send`: the per-tick effect budget, players within 96 blocks only. Never per target.
- The only repeating senders: the Argonaut chime (one per charge step, bounded by the charge cap), and Hell Conqueror's hellfire (one per hellfire pulse, at most one a second). Worst case per player: 1 packet a second.

**Client**

- Every effect is one activation under the engine's 400-vertex (100-quad) cap; the layer keeps the default cap, every ring / hoop / beam loop and solid
  model checks `hasBudget` (a solid is drawn whole or not at all), every loop is a constant or a `ctx.seg` (distance and particle-setting LOD).
- The highest vertex count the preview measured at full detail:

  | Shape | Vertices (quads) |
  |---|---|
  | SKILL_GATE | 340 (85, Rain); Volley of 8 316, Enkidu 264, Ea 236 |
  | SKILL_FIREBOLT | 240 (60) |
  | SKILL_ARGONAUT | 304 (76, Hero's beam); chime 148, strike 132 |
  | SKILL_FROST | 316 (79) |
  | SKILL_HOLY_FIRE | 204 (51, Hestia); Rea Laevateinn 156 |
  | SKILL_ALLURE | 216 (54) |
  | SKILL_SCREW | 352 (88, the ring of five screws); one screw 100 |
  | SKILL_WINGS | 96 (24) |
  | SKILL_SIGIL | 212 (53, GRAVITY); HELL / FIRE_FORCE 184, the rest under 150 |

- Textures are static final fields (no `ResourceLocation` built per frame; `PerfSourceScanTest` lists both files). Nothing is kept between frames, so nothing
  needs cleaning on logout, death or dimension change. A followed effect whose creature is gone draws nothing. Solids beyond 40 blocks are not built.

## NEW / REPLACED / KEPT

**NEW**: `SkillAnimeLayer`, `SkillAnimeSigils`, `SkillAnimeVfx`, `SkillAnimeVfxConfig`, `SkillAnimeFx`; the 9 shapes; the 20 `skillanime_*` textures and their
generator; the screw solid; the preview scenes and PNGs; the client setting `skills.classicVfx`; the self-test `skill.vfx` (in `SMOKE_EXPECT` right after
`devil.vfx`); the test `SkillAnimeVfxAssetTest`; `PerfSourceScanTest` lists the two layer files; `GreedVfxAssetTest`'s "last shape" pin moved to `SKILL_SIGIL`.

**REPLACED**: nothing was removed. The new shapes are a new look on top of the old ones (rule 5: these skills had no anime look, so it is new, not a swap).

**KEPT**: every `SkillFx` spray / ring / beam / shape call in the 21 wired skills (now the accent, and what the classic setting shows); every old shape,
texture and layer; no cost or damage code was touched; the Time Magic files.

## Caveats

- None of this has been seen in the game. It was rendered only by the headless preview (no world lighting, no depth against blocks).
- Gradle could not be run here. The layer and its helpers were compiled by the preview against stubs; the server helper, the wiring, the config and the test were
  checked only by reading them (and by Python mirrors of the test's texture and string checks).
- The wings and the chime use the followed player's body yaw; while the head turns away from the body they can sit a little off.
