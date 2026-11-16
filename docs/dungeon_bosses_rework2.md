# Dungeon-boss rework 2: the Labyrinth Guardians, a sound set, particle themes

Companion of `docs/dungeon_bosses_audit.md` and `docs/dungeon_bosses_rework.md` (wave 1: Floor Warden, Direwolf, Basilisk, Living Armor). Status: **written and previewed, NOT compiled and NOT run in a game**
(the session had no Gradle and no Minecraft; CI compiles). Nothing is committed, the version is not bumped, the changelog and `en_us.json` are not edited (the hand-over does those, see section 9).

## 1. What was built, how to read it, what is not included

### Part 1: the Labyrinth Guardians (and the Decennial bosses)

* One move book for the whole `LabyrinthGuardianEntity` family (nine sector Guardians and the 200 Decennial bosses; they are the same class), numbers in `dungeon/rework/GuardianRules.java`, the
  game-side brain in `GuardianBrain.java`. Damage is written as a SHARE of the Guardian's attack damage, so it scales with whatever the sector, floor and Decennial tier already give it.
  * **Ground Slam**: a ring grows on the floor for 1.3 s, then a hit and the kit's rider (radius 5.5 times the size factor, up to 1.6).
  * **Lance**: a line 22 long and 3.2 wide is drawn toward a player for 1.2 s; it tracks until the last 0.4 s, then locks, so you step sideways, not run.
  * **Rain**: up to three marks (radius 2.4), one per player, follow their player for 1 s and then lock for the rest of the 1.8 s; a step out of a mark is enough.
  * **Wave** (phase 2, below 50 % health): three rings roll out to 16 blocks, a jump clears them, one hit per ring per player.
  * **Verdict** (phase 3, below 25 % health, the signature ultimate, 4 s warning, a 3 s "winded" rest afterwards): the kit decides the form. SAFE ZONE (blood, spatial, frost, verdant, storm, astral): two lit
    sigils of radius 3 appear, stand in one (a 20 % graze inside, full hit outside). KEEP OUT (gravity, flame, shadow): everyone inside a ring of radius 12 takes it, get out.
  * Phases at 50 % and 25 % (a chat line, a ring and burst of the kit's particles, a roar or a gong, the bar colour of the existing Guardian code, cooldowns 15 % and 30 % shorter, never under 1.5 s).
* **Nine kits**, one per sector trait (`GuardianRules.KITS`): gravity (pull and knock), blood (weaken and wither), spatial (launch), flame (burn), frost (slow), verdant (poison), storm (knock and
  launch), shadow (weaken and wither, dark particles), astral (launch and weaken). Each has its own telegraph particle theme, its own wind-up cue, hit cue and accent cue, and its own Verdict line.
* **A colourway of the Warden suit per trait** (`guardian_<trait>` rows in `looks_dungeon.json`, skins and extras textures made by `tools/dungeon_bosses/gen_guardian_colourways.py`): a Guardian that has no
  abyssal look wears its sector's colour instead of the same violet and gold. Guardians that already have an abyssal look (the 22 profile looks) keep it.
* The Decennial bosses: the layer that already gives each its phases, lair, adds and signature is untouched. They get their sector trait's kit plus a tier factor (never over 1.25) on the Verdict only, and
  the Verdict pauses while a Decennial shield or "cannot strike" window is on. **They share nine kits; they do not each have a unique move set.** Writing 200 distinct sets was not attempted.
* Damage: every hit goes through `DungeonStrikes.strike`, that is `ZogratisDamage.hit`, the boss damage law and the 40 % player hit cap. Control (slows) goes through `DungeonStrikes.slow`, that is
  `BalanceLaw.beginControl` / `controlTicks` (one control per caster per 12 s). The existing boss DPS budget stays wired as before.
* Cleanup: the brain's state is per entity (cooldowns, marks, sigils, the wave hit map); `reset()` on death; it holds UUIDs, not players, and a mark whose player logged out or changed dimension is dropped
  the next tick; nothing is spawned, so there is nothing to remove on despawn.

### Part 2: sounds

* 16 new cues synthesised from nothing (numpy, written to Ogg Vorbis with ffmpeg by `tools/gen_dungeon_boss_sounds.py`; the engines are the Gnome set's): **rise** (a telegraph whoosh), **slam**, **chain**,
  **roar**, **howl**, **hiss**, **gong** (phase change), **charge** (an ultimate gathering), **blast**, **toll** (bell), **clang**, **spit**, **shimmer**, **crackle**, **step** (a vanishing step), **growl**.
  41 files, 2 to 3 variants each (random per play), mono, 0.3 to 2.35 s, each under 20 KB, 523 KB in all (the limit asked for was 200 KB per file).
* Registered as `anime_multiverse:dboss.<cue>` in `dungeon/rework2/DungeonBossSounds.java` (one `DeferredRegister`, a loop over the catalogue `DungeonBossSoundRules`), entries appended at the end of
  `sounds.json` with a subtitle each (`subtitles.anime_multiverse.dboss.<cue>`).
* Wired in: the **wave-1 brains** (all 28 vanilla `playSound` calls of `WardenBrain` and `BeastBrain` now go through `DungeonStrikes.cue`, which plays the new cue, or the old vanilla sound if the sounds
  setting is off), the **Guardian brain**, and the **Foundry mobs' telegraph goal** (a rising whoosh at the start of a wind-up of a second or more, a slam after an armoured mob's strike).
* Playback is server side, `ServerLevel.playSound(null, ...)`, so the game sends it only to players inside the cue's range (16 blocks, or 16 times the volume above 1: a slam carries 29, the blast 38).
* Mapping used: Warden slam = slam, Chain Lash = chain, Shadow Step = step, Seal Pulse = rise, Key Toll = charge then two tolls then blast, phase = gong, enrage = roar; Direwolf growl (pounce) and howl (Pack Howl), Basilisk rise (gaze), spit (Venom Spit)
  and hiss (tail sweep), Living Armor clang (bash), chain (rally) and slam.

### Part 3: dungeon NPCs and Foundry mobs

* Only the Foundry telegraph voice above. **Not redone**: the 25 mob families already have their own voices, and the hub NPC logic was explicitly out of scope. No new NPC behaviour, no new models.

### Part 4: particle themes

* 13 new themes appended to `particle_themes.json` through `tools/particles/gen_themes.py` (`OTHER`): `dungeon.boss.warden`, `.direwolf`, `.basilisk`, `.armor` and `dungeon.boss.guardian.<trait>` for the nine
  traits (each 1 to 4 existing particles, every particle under 1.05 blocks wide, a colour, a dark set for blood and shadow). The wave-1 brains now pass their theme to every ring, burst, flash and line.
  With `dungeonBosses.themes` off, or if the id is missing, everything falls back to the old seal theme, so the old look is one switch away.

### What is not included

* **No new 3D models.** No Blender (`which blender` finds nothing in the session), so rule 14 could not be followed for new meshes: the visual identity is the nine colourways of the existing Warden meshes
  (re-painted skin, extras texture and glow, same geometry and animations) plus the abyssal looks that profile bosses already had. The Direwolf, Basilisk and Living Armor looks stay as wave 1 left them.
* No Slime, Goliath or Charybdis changes, no new Decennial move sets, no new music, no mounted or flying Guardian moves.

## 2. NEW / REPLACED / KEPT (rule 5)

| boss / part | NEW | REPLACED | KEPT |
|---|---|---|---|
| Labyrinth Guardian (9 sector bosses) | Slam, Lance, Rain, Wave, Verdict; 9 kits; kit theme, cues and chat lines; colourways | nothing removed: the old Ground Slam and Shadow Step still run when `guardians` is off | traits, scaling, D&D layer, budget hooks, drops, abyssal looks, name bars |
| Decennial bosses (200) | tier factor on the Verdict, the Verdict pausing during shield windows | nothing | profiles, phases, lairs, adds, signatures, loot |
| Floor Warden, Direwolf, Basilisk, Living Armor | their own theme per boss; the new sound cues | the bare vanilla sounds (still the fallback when `sounds` is off) | all wave-1 moves, numbers and looks |
| Foundry mobs | a wind-up whoosh and an armoured-strike slam | nothing | every ability, every voice, every model |
| Sounds | 16 cues, 41 files, 16 events, 16 subtitles | nothing | every existing sound |
| Particles | 13 themes | nothing | all 308 earlier themes |
| Entities | hooks `customMoves` / `customMovesTick` / `customCasting` on `FloorWardenEntity`, a synced theme-kit int on the Guardian | nothing | the old chooser stays, behind the setting |

## 3. Settings (`boss_rules` config, section `dungeonBosses`, all default true, all server config)

| setting | what it does when off |
|---|---|
| `dungeonBosses.rework` | the master switch (wave 1): everything below is off too |
| `dungeonBosses.guardians` | Guardians fight as before (old Ground Slam and Shadow Step), and wear the old suit |
| `dungeonBosses.sounds` | no `dboss.*` cue plays; the old vanilla sounds play instead; the Foundry telegraph is silent again |
| `dungeonBosses.themes` | every telegraph uses the old seal theme |

## 4. Worst-case cost (rule 11)

* **Entities spawned**: 0. No new entity, no projectile, no area-effect cloud. The brain draws and strikes only.
* **Server work per tick** per Guardian in a fight: one bounded player query (`DungeonStrikes.players`, at most `MAX_VICTIMS` = 8 players, within the move's radius), only while a move is active; marks hold at
  most 3 UUIDs; the Wave hit map is at most 8 players. No block scan, no chunk load, no `getEntitiesOfClass` in the brain. Idle ticks: a cooldown check.
* **Damage** (units of the Guardian's attack damage, per second, if every move fired as often as its cooldown allows, which one body and a 1 s pause make impossible): phase 1 at most 0.34, phase 2 at most
  0.56, phase 3 at most 0.76 (tier 5). Every hit also goes through the 40 % player hit cap and the boss DPS budget. Self-test and unit test hold the ceiling at 1.0.
* **Control**: slows only, through the control law (one per caster per 12 s, capped durations). No stuns.
* **Particles**: only through the Fx facade and its budgets (distance, per-player, global). My estimate before the budgets: a Guardian telegraph is at most 2 to 3 rings of 24 to 40 points plus a burst of
  10 to 20 per move, a move every 1.6 s at the fastest, so at most about 350 particles a second per Guardian before the budgets cut it, sent to players near it only. Each particle is 1.05 blocks or less.
* **Network**: particle packets only (Fx batches them); the chat line to nearby players on a phase change or a Verdict (at most two per fight per phase); the synced theme-kit int changes at most when the
  sector changes (written every 100 ticks only if different). Sounds: the game's own sound packet, at most 4 per second per boss (`DungeonBossSoundRules.Limiter`, plus a cooldown per cue), to players in range only.
* **Sound memory**: 523 KB of ogg files in the jar, loaded by the client when first played; no stream, no server memory.
* **Client draw**: a colourway is a different texture of the same mesh: no extra quads, no extra draw calls. The 18 new textures (9 skins and 9 extras, each with a glow map) are 64x64 or smaller.
* **Cleanup**: logout, death, dimension change and despawn drop every mark and sigil (UUID lookups that find no player are ignored); `die()` calls `reset()`; the limiter and the brain die with the entity.

## 5. Sound list

| cue | variants | volume | range (blocks) | cooldown (ticks) | used by |
|---|---|---|---|---|---|
| rise | 3 | 1.2 | 19 | 8 | wind-ups: Warden Seal Pulse, Basilisk gaze, Guardian gravity, Foundry telegraph |
| slam | 3 | 1.8 | 29 | 6 | Warden slam, Living Armor slam, Guardian hits, Foundry armoured strike |
| chain | 3 | 1.3 | 21 | 10 | Warden Chain Lash, Living Armor, Guardian shadow |
| roar | 3 | 2.0 | 32 | 40 | Warden enrage, Guardian phase 2 |
| howl | 2 | 1.8 | 29 | 100 | Direwolf Pack Howl |
| hiss | 3 | 1.2 | 19 | 20 | Basilisk tail sweep and gaze, Guardian verdant |
| gong | 2 | 2.2 | 35 | 60 | phase changes, Guardian phase 3 |
| charge | 2 | 1.8 | 29 | 60 | Warden Key Toll gathering |
| blast | 2 | 2.4 | 38 | 60 | Key Toll, Guardian flame and storm hits |
| toll | 2 | 2.0 | 32 | 15 | the two bells of the Key Toll, Guardian astral |
| clang | 3 | 1.2 | 19 | 6 | Living Armor, Guardian spatial and frost |
| spit | 2 | 1.0 | 16 | 10 | Basilisk Venom Spit, Guardian verdant |
| shimmer | 3 | 1.2 | 19 | 8 | Guardian spatial, frost and astral wind-ups |
| crackle | 3 | 1.2 | 19 | 8 | Guardian flame and storm |
| step | 2 | 1.3 | 21 | 20 | Warden Shadow Step, Guardian shadow |
| growl | 3 | 1.0 | 16 | 20 | Direwolf pounce and landing, Guardian blood wind-up |

Sound quality was judged only from spectrograms and levels (peak at or under 0.92, no clipping), never by ear. Treat the first listen as the real test; every cue is a table row in
`tools/gen_dungeon_boss_sounds.py` (a seed and a few parameters), so tuning one is a one-line change and a re-run.

## 6. Previews (`docs/dungeon_bosses_previews2/`, built by `tools/dungeon_bosses/make_previews2.py`)

* `guardian_colourways.png`: **REAL RENDER** with the repo's software renderer of the player-model kit (the same meshes, skins, glow maps and extras the game uses; light and colour differ from the game's):
  the original Floor Warden and the nine colourways, front and back, each labelled with its Verdict form.
* `guardian_moves.png`: **SCHEMATIC, not a render**: the five moves as top-down shapes with the numbers from `GuardianRules` (each panel has its own scale; the Lance is drawn at half length).
* `sound_spectrograms.png`: **REAL DATA** from the synthesis: one variant of each cue as a log-frequency spectrogram (not a recording of the game).
* Wave-1's sheets in `docs/dungeon_bosses_previews/` are unchanged. No turnaround of any new model exists, because there is no new model.

## 7. Tests

* `DungeonBossSoundsTest`: catalogue = generator table = sounds.json = files (mono Ogg, 0.3 to 2.45 s, under 200 KB each) = subtitles in the lang fragment; every registered sound has its file and entry;
  no orphan file or entry; range rule; the limiter's cooldown and rate.
* `GuardianRulesTest`: book, kits (nine, distinct, real cues), damage shares, tier and size factors, worst-case ceiling, geometry and Verdict shares.
* `DungeonBoss2SourceScanTest`: no client class, no static mutable state, no spawn, no bare `hurt`, no world query in the new code; settings exist and default to on; the Guardian keeps the old chooser;
  themes, skins and `looks_dungeon.json` rows exist; every lang key the brain builds exists; the self-test is registered and in the smoke list before `scaling`.
* `DungeonBossSourceScanTest` (wave 1): its smoke-list assertion is now order-independent (the position `dboss,scaling` no longer holds once `dboss2` sits between them).
* Server self-test `dboss2` (smoke list, before `scaling`): every cue is a registered event, nine kits resolve to their own themes and real cues, a Guardian of each of the nine themes syncs the colourway the
  setting says, the book's cooldowns, the damage ceiling and the limiter.

## 8. Caveats and what was not verified

* **Nothing was compiled and nothing was run.** Every Java file was written against the APIs as read in the repo; expect a few compile fixes from CI.
* No fight, sound, particle or colourway was seen or heard in a game. The previews are the software renderer, not the game.
* Sound design is by numbers and spectrogram only; the roar and the growl were the weakest in the plots (noise-heavy) and were softened once.
* The colourways re-paint the existing Warden suit. They will look like recolours because they are: no new silhouette, no Blender model (rule 14 not met for models).
* The Verdict's "get out of the ring" form for gravity, flame and shadow has no ground marker except the telegraph ring; if players miss it, a persistent decal is the next step.
* Decennial bosses share the nine kits.
* Boss voice lines, hub NPCs and the 25 mob families' voices were left alone.
* Wiki rule 10: no canon lookups were needed (these are original dungeon bosses); nothing was fetched.

## 9. For the lead

1. Merge `tools/lang/dungeon_bosses2.json` into `en_us.json` with the usual lang merge (27 keys: 16 subtitles, 11 chat lines).
2. `sounds.json` already has the appended `dboss.*` block; no further merge needed.
3. Bump the version, write the changelog, and run the full test sweep and the smoke job (`dboss2` is in the list before `scaling`).
4. Regenerate if you change anything: `python3 -B tools/gen_dungeon_boss_sounds.py`, `python3 -B tools/dungeon_bosses/gen_guardian_colourways.py`, `python3 -B tools/particles/gen_themes.py`.
