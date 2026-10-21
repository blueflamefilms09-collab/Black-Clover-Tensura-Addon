# Themed dungeon music (1.107.0)

A client-side music engine for the Mega-Dungeon and the Hub: a loop that fits the theme of the floor you are on, a fight loop that takes over when something hunts you, a boss
loop per road, and their own loops for safe rooms, the Hub and rift pockets, all cross-faded. There are no OST files in the project, so the 25 loops are **synthesised** by
`tools/audio/gen_music.py` (original compositions, deterministic) and can be replaced by real music file for file (see "Replacing the music").

## NEW / REPLACED / KEPT

| | What |
|---|---|
| NEW | `audio/music/*` (pure catalogue, rules, cross-fade director, sound registry, state packet, server evaluation, self-test) |
| NEW | `client/music/*` (the engine, the sound instance, the client state, the client config `nusmp-music-client.toml`) |
| NEW | 25 sound events `nusmp:music.<...>` and their `sounds.json` entries (appended after the mob voices) |
| NEW | 25 loops `assets/nusmp/sounds/music/*.ogg` (7,209,097 bytes = 6.87 MiB in total), the generator `tools/audio/gen_music.py` |
| NEW | lang fragment `tools/lang/themed_music.json` (the track titles of the optional "now playing" line) |
| REPLACED | nothing. The vanilla music is only held back while a themed loop plays, and only when the player leaves `stopVanilla` on |
| KEPT | `ModSounds` (the mob voices), `NUSounds`, every older `sounds.json` entry, the theme ambience (`ThemeAmbient`) |

Shared files touched (append-only one-liners): `NUSMP.java` (3 lines: `ThemedMusic.init`, `ThemedMusic.register` + `MusicSelfTests.register`, `ThemedMusicEngine.init`),
`sounds.json` (25 entries at the end, nothing else changed: the diff is additions only).

## How it works

```
server (once a second per player)            client (every tick)
 sector + theme + road ──┐                    MusicClientState (last state, 20 s fresh)
 safe room? rift pocket? │  MusicNet.State    │
 boss within 40 blocks?  ├──────────────────► ThemedMusicEngine ── MusicDirector (pure: fades, cooldown, hold)
 monsters hunting you?   │  (layer, theme,    │                       │ voices (id, track, level)
 MusicRules.decide ──────┘   road; on change  │                       ▼
                             + every 5 s)     └─► ThemedMusicInstance per voice (MUSIC category, looping, relative)
```

* **Layers** (`MusicLayer`): `EXPLORE` (default), `BATTLE` (at least one dungeon monster within 28 blocks has you as its target), `BOSS` (a boss, by `BossIdentity`, within
  40 blocks), `SAFE` (a safe room or rest anchor), `RIFT` (inside a rift pocket), `HUB` (the Sanctuary dimension). Order of the rules: hub, boss, rift, safe, fight, explore
  (`MusicRules`, tested). A spectator hears nothing.
* **Track choice** (`MusicCatalog`): explore and battle use the family of the floor's theme; boss uses the road (Holy `boss_holy`, Abyssal `boss_abyssal`); safe, hub and rift
  have one loop each. The 27 Region Themes map onto ten families (below). A theme that is not in the table (a datapack theme) gets a family by a stable hash of its id, so it
  always sounds the same; a position in no sector uses the `stone` family.
* **Cross-fade** (`MusicDirector`): the first track starts at once and fades in (4 s by default). Going UP in priority (explore to battle to boss) needs the new want to hold for
  half a second, ignores the cooldown and fades in at double speed. Going to the same or a lower priority (the fight is over, the floor changed family) needs the want to hold
  for 5 s without a break and the last switch to be at least `cooldownSeconds` old (12 s). The curve is equal-power (sine). At most three voices exist at once. Leaving the
  dungeon, switching the option off or logging out fades (or, on logout, cuts) everything.
* **Vanilla music**: while a voice exists, `SelectMusicEvent` answers "no music" and a vanilla track that was already playing is stopped; when the themed music ends,
  vanilla returns by itself. `stopVanilla = false` keeps vanilla playing as well.
* **Volume**: the loops play in the MUSIC category, so the vanilla Music slider (and the Master slider) scale them with no code of ours; at Music = 0 nothing is started (no
  streaming of a file nobody can hear). `themedVolume` (0 to 1, default 0.8) is a second, mod-only multiplier.
* **Config** `nusmp-music-client.toml`, section `[music]`: `themed` (master switch, default true), `themedVolume`, `fadeSeconds` (0.5 to 20, default 4), `cooldownSeconds`
  (0 to 120, default 12), `stopVanilla` (true), `showTitle` (false: shows the track title above the hotbar when it starts).
* **Cost**: the server does one sector lookup, one safe-room lookup and one entity scan of a 80 x 40 x 80 box per player per second, only inside the dungeon, and sends a packet
  only on change or every 5 s. The client does a few comparisons per tick and keeps at most three streamed loops open. No allocation per tick beyond the map iterator.
* **No hunger or exhaustion** is touched anywhere.

## The ten families and the 25 tracks

Each family has its own tonic, scale, tempo, pad, lead, texture and drum kit; the battle loop of a family keeps the key and the **same theme (motif)** as its explore loop at a
faster tempo with drums, a bass ostinato and chord stabs, so a fight growing out of a quiet floor stays in the same musical world.

| Family | Region Themes | Character | Key, scale | Explore bpm | Battle bpm |
|---|---|---|---|---|---|
| stone | gravity, dwarven_forgehold | heavy, low drones, horn, taiko | D dorian | 70 | 112 |
| storm | storm, sky_citadel | wind, plucked arpeggios, driving drums | E mixolydian | 88 | 140 |
| blood | blood, vampire_castle, cursed_archive | slow strings, heartbeat, cello | A phrygian dominant | 58 | 100 |
| flame | flame, infernal_foundry | embers, brass, anvil | E phrygian | 84 | 132 |
| frost | frost, astral, crystal_geode | glass pads, bells, high wind | F# lydian | 56 | 96 |
| verdant | verdant, feywild_grove, fungal_underdark | warm pads, kalimba, birdsong, hand drums | G mixolydian | 80 | 118 |
| spatial | spatial, shadow, spider_warrens | whole-tone shimmer, glass bells, whooshes | C# whole tone | 50 | 90 |
| holy | radiant, holy_church, celestial_court, mountain_monastery | choir pads, bells, timpani | D ionian | 76 | 120 |
| deep | abyss, sunken_temple, drowned_crypt | dark pads, sonar pings, drips, deep toms | B aeolian | 54 | 92 |
| crypt | necropolis, haunted_manor | organ, celesta, low wind, wood hits | E harmonic minor | 62 | 104 |
| (boss_holy) | Holy road boss | choir, brass, string ostinato, timpani, cymbal swells | D lydian | 128 | |
| (boss_abyssal) | Abyssal road boss | low brass, "oh" choir, heavy toms, string ostinato | C phrygian dominant | 104 | |
| (safe) | safe rooms, rest anchors | music box, warm pad, soft bell | F pentatonic | 60 | |
| (hub) | the Hub | plucks, flute, shaker, hand drums | C ionian | 92 | |
| (rift) | rift pockets | whole-tone glass, long swells, deep toms | C# whole tone | 48 | |

`python3 -B tools/audio/gen_music.py --list` prints the exact keys, bars and lengths. Every loop is 60 to 90 s (71 to 86 s as built), mono, 22,050 Hz Ogg Vorbis (quality 2),
about 160 to 400 KB each, 7,209,097 bytes in total (budget: 25 MB).

### How a loop is made seamless
Every note is added into a circular buffer (a note that runs past the end continues at the start), every slow movement (wind, swells, drones) repeats a whole number of times
per loop, and the reverb is a circular convolution, so the last sample runs straight into the first. The generator prints, per track, the step across the seam next to the
99th-percentile step inside the track (the seam is always the smaller). The engine loops the whole file (`looping = true`, streamed).

## Why (the design decisions)

* **Layers, not one track per floor.** Music that only changes when the floor does cannot follow a fight; music that changes with every hit flaps. So the server names the
  situation (a layer), and the client's director adds the hysteresis: up quickly, down slowly.
* **The server decides, the client plays.** Who is hunting the player and which room this is are server facts; sending a layer number once a second costs almost nothing and
  keeps the client free of dungeon data. The client double-checks the dimension and treats a state older than 20 s as "no music".
* **Ten families, not 27 themes.** A family gives a place its own key, scale, tempo and instruments; 27 pairs of loops would be 3 times the files for little more variety. The
  table is data in one class and a datapack theme still gets music.
* **MUSIC category and a relative, unattenuated instance.** The music slider, the master slider and the "pause" behaviour of the game then work with no special cases.
* **The pure director.** The fade rules are the part that can go wrong without an error, so they are a state machine with no Minecraft class, covered by tests.
* **Synthesised and deterministic.** There is no OST; the generator is reproducible and the loops are original, so nothing is copied from anywhere.

## Replacing the music

The tracks are plain files, so real music needs **no code change**:

1. Make a resource pack (or edit the mod's assets) with `assets/nusmp/sounds/music/<track>.ogg` using the same names as below. Mono or stereo both work; mono is smaller.
   A track is looped as a whole, so cut the file so that its end runs into its start (or accept a short gap).
2. To use several files for one situation, or a different file name, ship your own `assets/nusmp/sounds.json` entry for the event (the entries of one event from several packs
   are combined, so add `"replace": true` to yours to make it the only one). Keep `"stream": true` for anything longer than a few seconds. A looping instance picks one of the listed files each time it starts.
3. Event names (`nusmp:<event>`): `music.<family>.explore`, `music.<family>.battle` for the ten families (stone, storm, blood, flame, frost, verdant, spatial, holy, deep,
   crypt), `music.boss.holy`, `music.boss.abyssal`, `music.safe`, `music.hub`, `music.rift`. File names are `<family>_explore`, `<family>_battle`, `boss_holy`, `boss_abyssal`,
   `safe`, `hub`, `rift`.
4. To change which theme uses which family, edit the table in `audio/music/MusicCatalog.java` (`THEME_FAMILY`); to change the pieces themselves, edit the table at the top of
   `tools/audio/gen_music.py` (it mirrors the same ids) and run `python3 -B tools/audio/gen_music.py` (needs numpy and ffmpeg with libvorbis; takes about 8 minutes). The test
   `MusicAssetsTest` and the self-test `dungeon.music` fail if the Java table, `sounds.json` and the files disagree.

## Tests

* JUnit: `MusicCatalogTest` (every Region Theme has a family, names, track choice, unknown themes, wire codes), `MusicDirectorTest` (fade in, escalation, lingering, flicker,
  boss, cooldown, silence, equal power, voice cap, reset, timing), `MusicRulesTest` (the order of the layer rules), `MusicAssetsTest` (each track has an entry with one streamed
  file, mono, 60 to 90 s, no orphans, 25 MB budget, older entries kept, lang titles).
* Self-test `dungeon.music` (CI server smoke test): the 25 events are registered and are the ones `MusicSounds` hands out; each `sounds.json` entry names an Ogg on the class
  path; the size budget; every Region Theme maps to an existing family; the layer rules; a simulated cross-fade ends with one voice; the packet survives its codec.

## What could NOT be verified (honest list)

Nothing can be run in a game here, and nobody has listened to the loops.

* **How the music sounds.** The loops were checked only by numbers (length, level, peak, the seam, spectrograms). Whether the melodies are pleasant, whether the families are
  distinguishable by ear, the loudness against the vanilla music and against the mob voices, and whether the battle loops feel like a fight are unknown. They are procedural
  placeholders in the best sense: playable, original, and meant to be replaced by real OST.
* **Seamless looping in the game.** The signal is continuous across the loop point, but the Vorbis encoder adds a few milliseconds of priming; if the game's decoder does not
  trim it there could be a tiny tick at the loop point. Not heard.
* **`SelectMusicEvent` with a null music.** The event is documented to accept "no music"; if a NeoForge build treats it differently, the per-tick `stopPlaying()` on the vanilla
  music manager still silences vanilla (at worst a vanilla track starts for a tick).
* **The sound engine re-reading the volume of a tickable instance every tick**, the start-silent behaviour, and the re-start after a sound reload (F3+S): written to the
  documented behaviour of the sound engine, not run.
* **Server cost on a crowded floor.** One entity scan per player per second is the same order as the existing theme ambience; not measured.
* **What counts as "a boss" and "hunting".** The boss test is `BossIdentity.isBoss` (the mod's bosses and the `nusmp:boss` tag); a Tensura or vanilla boss does not trigger the
  boss loop. Hunters are monsters tagged as dungeon monsters (or `Enemy`) that have the player as their target; a mob without a target AI never counts.
* **Rift pockets**: detected by their coordinates (`RiftRules.slotAt`); pockets built by other means would sound as the floor they are in.
* **The dedicated server** needs no client class (the packet handler names the client class inside a lambda, like the floor card); this was written to that rule, not run.
