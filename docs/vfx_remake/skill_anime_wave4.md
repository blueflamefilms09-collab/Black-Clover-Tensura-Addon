# Skill effects in their anime look, wave 4

Wave 3 (`skill_anime.md`) gave the Tensura-style Unique / Ultimate skills their anime look and left a list for later. This wave takes that list, the 27 Beast Will beasts,
the 21 hero / vessel weapons and the five weakest grimoire attributes. Nine hundred separate effects would be nine hundred scripts, so the work is **style-driven**:
a handful of drawers where the packet's power picks the style, the family, the element or the motif.

## What will be built, how it is read, what is not included

- **Built**: four new `VfxShape` values and one layer. `ANIME2_SKILL` draws fifteen skill signatures, `ANIME2_BEAST` draws the move family of a Beast Will move in the element
  of the beast, `ANIME2_HERO` draws the motif of a hero weapon technique in the weapon's colour, `ANIME2_GRIMOIRE` draws the cast and the impact of five grimoires.
- **Read**: the server (`SkillAnimeFx2`) sends ONE packet per cast through `VfxSpawn`; the layer (`SkillAnime2Layer` and its drawers) reads the power as below.
- **Not included**: a separate, hand-made effect for every one of the ~150 hero techniques and the ~100 Beast Will modes (the motif / family carries them, the weapon /
  beast carries the colour and the form); the remaining Ultimates that already had a wave 3 shape; new sounds; any change to the Time Magic VFX.

## What was built

- **`vfx/VfxShape.java`**: appended after `SKILL_SIGIL` in a commented block: `ANIME2_SKILL, ANIME2_BEAST, ANIME2_HERO, ANIME2_GRIMOIRE`. Nothing moved.
- **`vfx/client/layer/SkillAnime2Layer.java`** (registered last in `VfxManager`) dispatches to four drawers: `SkillAnime2More` (the skills), `SkillAnime2Beast`,
  `SkillAnime2Hero`, `SkillAnime2Grim`. They use the wave 3 kit (`SkillAnimeLayer`'s statics: life, ramp, hoop, zigzag, flash, textures CORE ... SWIRL) and fifteen new
  textures.
- **`vfx/client/layer/SkillAnime2Vfx.java`**: the pure half (texture list, the decoders, the tables, the classic switch).
- **`tools/gen_skill_anime2_vfx_textures.py`**: 15 textures, `textures/particle/skillanime2_*.png` (leaf, droplet, rock, bubble, feather, rune, claw, card, eye, crescent,
  hex, stitch, note, puff, facet). Deterministic, white / grey so the vertex colour tints them, colour bled into the clear texels (no dark squares).
- **`skill/SkillAnimeFx2.java`**: the server helper and the `skill.vfx2` self-test; it is on the smoke list right after `skill.vfx`.
- **`vfx/client/SkillAnimeVfxConfig.java`**: a second key `skills.classicVfx2` in the same client file.
- **Preview**: `tools/vfx_preview/scenes.d/skill_anime_wave4.json` (59 scenes), contact sheets `docs/vfx_remake/previews/skill_anime2_{skill,beast,hero,grim}_sheet.png`.
  `tools/vfx_preview/preview.py` now also copies the wave 3 layer when a wave 4 layer is previewed (it draws with the wave 3 kit).

### How the power is read

| Shape | Power | From -> to |
|---|---|---|
| `ANIME2_SKILL` | `style + 1` (+ `extra / 100`, extra up to 49, used by the Visionary's meteor for its radius) | from = the feet (hip for the kick, sky for the meteor); the distance is the radius (Shishou: `1 + mode`; Vimana: the flight; kick: the foot's reach) |
| `ANIME2_BEAST` | `family * 20 + element` (+ 0.5 for the mastery finisher) | line / rush / volley: chest -> aim; burst / seal / aura: centre -> centre + radius |
| `ANIME2_HERO` | `motif * 20 + variant * 2 + ultimate` | chest -> where the technique is aimed (10 blocks) |
| `ANIME2_GRIMOIRE` | `attribute * 20 + phase * 8 + scale` (scale 0..3, phase 0 cast / 1 impact) | cast: the caster (follows); impact: the point |

Styles / families / elements / motifs: see `SkillAnime2Vfx` (client) and `SkillAnimeFx2` (server). `SkillAnime2VfxAssetTest` pins the two tables against each other and
round-trips every encoding.

## Sources (rule 10)

The English wikis and the Japanese wikis could not be reached from this session (the network proxy refuses them), so **nothing below is wiki-verified**; the looks come from
general knowledge of Fairy Tail, Black Clover, Tensura, DanMachi, Fire Force, Medaka Box, Shield Hero and Bleach. Not fetched at all: every fandom page and ja.wikipedia. The
canon points most worth a check are marked "check" in the table.

## Per skill (part 1: the wave 3 leftovers, `ANIME2_SKILL`)

| Skill | Look | Wired in | Old effect |
|---|---|---|---|
| Liaris Freese | crimson seal, two helices of light climbing the body, petals, a column of light | `LiarisFreeseSkill.onKill` | kept |
| Hati | fire, lightning and magic motes streak INTO the body | `HatiSkill.onTakenDamage` | kept |
| Hawk Eye | an eye opens over the head, a scanning ring sweeps to the radius, feathers drift | `HawkEyeSkill.pulse` (drawn once per 3 s) | none before |
| Spirit Card | a card spins overhead, green rings and motes heal outwards | `SpiritCardSkill.pulse` (once per 3 s) | kept |
| Lord of Summons / Summoner | a card flips open on a rune circle, a pillar of light lifts the summons in; gold / violet (wraith) / orange (legion) | `SummonerSkill.castCard` (covers the Lord of Summons) | kept |
| Book Maker | the wave 3 giant screw on each pinned target (at most 4 per press) | `BookMakerSkill.pin` | kept |
| Shishou | analysis rings, an eye, data lines, one pip per mode | `ShishouGrantService.press` CAST | none before |
| Gate of Babylon: Armour / Vimana / King / Elixir | golden plates lock on / ripple gates and wings trail the flight / sunburst and gates round the radius / fountain of golden droplets | `GateOfBabylonSkill` | kept |
| Devil's Footprints kick | a flaming crescent sweeps up through the cone | `DevilsFootprintsSkill.kick` (also Adolla Burst's kick) | kept |
| The Almighty | a golden eye above, rays of every future, black smoke | `TheAlmightySkill.onPressed` | kept |
| The Visionary | meteor (from the sky), a black sphere of imagination with rising debris, Perfect Body's rings and crown | `TheVisionarySkill.onPressed` | kept |

## Per group (parts 2 to 4)

- **Beast Will (27 beasts, `BeastMoves.cast`)**: the primitive of the move picks the family (BEAM / DISC: a line, RUSH: a dash and pounce, BLAST / DETONATE: a roar of rings and
  rising pieces, BARRAGE: a volley, STOP / SEAL / PSYCHIC: a seal circle with pillars, GUARD / VEIL / SCAN / CAPSULE: an aura round the caster); the beast picks one of twelve
  elements (fire flames, water droplets, ice shards and snowflakes, lightning forks, earth rocks, wind feathers, dark ink smoke, light twinkles, poison bubbles, nature leaves,
  spirit wisps, void runes). The element of each beast is a table in `SkillAnimeFx2.beastElement` (my reading of each beast, check). The mastery finisher is the same family
  bigger with a rune halo; the old finisher shape stays.
- **Hero / vessel weapons (21, `HeroUse.cast`)**: slash (sword, axe, claw, fan, scythe, katana), thrust (spear, harpoon, bow, projectile), shock (hammer, gauntlet, carriage), ward
  (shield), cast (staff, book, instrument), thread (whip, chain, sewing set), shard (mirror); the weapon's `HeroColors` colour tints it; an ultimate technique is the same motif
  bigger with a rune halo behind the wielder. It is sent once after a technique really happened (after the cost, the cooldown and the lock).
- **Grimoires (Cotton, Mirror, Plant, Sealing, Thread)**: `castCircle` / `impact` of each book also send the attribute's own look (wool clouds, flying facets and a hex mirror,
  leaves and vines, a rune seal with bands and chains, stitched strands). The old circle and burst are still sent first.

## The classic setting

`skills.classicVfx2` in `config/nusmp-skill-anime-vfx-client.toml` (a CLIENT setting, default `false`, looks only). `true`: `SkillAnime2Layer` draws nothing and only the old
effects are seen. The wave 3 key `skills.classicVfx` is unchanged.

## Cost (rule 11)

Measured with the headless preview (`tools/vfx_preview/preview.py`, four frames per scene; the engine's `hasBudget` and 400-vertex activation cap stay in force, and every
drawer uses `ctx.seg` for its LOD):

| Group | Worst vertices measured | Quads |
|---|---|---|
| Skills | 248 (Visionary's void) | ~62 |
| Beast | 276 (a void finisher burst) | ~69 |
| Hero | 252 (harpoon / bow) | ~63 |
| Grimoire | 124 (sealing / thread cast) | ~31 |

All under the 400-vertex cap. No per-frame allocation beyond the small vectors the wave 3 kit already makes; the textures are static finals; no `Minecraft.getInstance()`.

Server side, per player (`SkillAnimeFx2.Throttle`, a bounded table: at most 512 players, then it is cleared and refills with the active ones, each entry is two longs):

| Sender | Worst case |
|---|---|
| Hawk Eye, Spirit Card pulses | 1 packet per 60 ticks |
| Liaris, Hati, Shishou | 1 per 20 ticks |
| Beast move | 1 per 6 ticks (in practice one per cooldown) |
| Hero technique | 1 per 4 ticks (in practice one per cooldown and global lock) |
| Grimoire cast / impact | 2 per 6 ticks / 3 per 6 ticks |
| Book Maker screws | 4 per 10 ticks |
| Every other skill | 1 per cast (the cast has its own cooldown) |

Each packet goes through `VfxSpawn` (the per-tick effect budget, players within 96 blocks only), is never sent per target or per tick, spawns no entity, loads no chunk and scans no
world. No effect lasts longer than 200 ticks. Nothing is kept for a logged-out player beyond the bounded throttle table.

## Multiplayer (rule 9)

All decisions stay on the server; the client only draws. The layer never reads the world or the local player; the effects follow instances by entity id as the other layers do.
The new setting is a CLIENT config. Every new packet is the existing VFX packet (its validation is unchanged).

## NEW / REPLACED / KEPT

- **NEW**: `ANIME2_SKILL`, `ANIME2_BEAST`, `ANIME2_HERO`, `ANIME2_GRIMOIRE`; `SkillAnime2Layer` and its four drawers; `SkillAnime2Vfx`; 15 textures and their generator;
  `SkillAnimeFx2` and the `skill.vfx2` self-test; `skills.classicVfx2`; 59 preview scenes and four contact sheets; `SkillAnime2VfxAssetTest`.
- **REPLACED**: nothing was removed. (The wave 3 pin "SKILL_SIGIL is the last shape" in `SkillAnimeVfxAssetTest` and `GreedVfxAssetTest` was loosened to "SKILL_SIGIL closes the
  nine skill shapes", so a later family can be appended; the check that nothing is inserted before it stays.)
- **KEPT**: every old effect of every wired skill, the Beast Will finisher shape, the grimoires' circles and bursts, the wave 3 shapes and their calls, the Time Magic VFX.

## What was left

- The remaining Ultimates that had only a wave 3 shape or an old generic one are not touched again; the Almighty and the Visionary (which had only sprays) are covered here.
- Per-technique effects for the hero weapons and per-mode effects for the beasts (each is its motif / family in the weapon's or beast's colour).
- Grimoire attributes other than the five.
- Sounds.

## Caveats (not seen in a game)

- Nothing here was run in Minecraft or with Gradle. The layer classes were compiled and drawn with the headless preview (real classes against stubs); the server class
  (`SkillAnimeFx2`), the wiring and the tests were read twice but **not compiled in this session**, only the pure decoders were compiled and round-trip checked with `javac`.
  Expect CI to be the first real compile of `SkillAnimeFx2`, the wiring edits and `SkillAnime2VfxAssetTest`.
- The preview is not a Minecraft renderer (no lighting, no depth against blocks); the hero effects read small in the preview because the camera is far away.
- Canon sources could not be fetched (see Sources); the beast element table and the hero weapon motifs are my reading.
- The Visionary's meteor radius rides as the style's extra, clamped to 49.
