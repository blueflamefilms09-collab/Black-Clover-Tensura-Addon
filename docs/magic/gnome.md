# The Gnome: the Earth Spirit of Black Clover as a stone golem (key `gnome`)

Owner request: "make the Spirit lord. The Earth Spirit in Black Clover is Gnome (Tsuchi no Seirei), and its host is Feramina (also spelled Feremena), one of the Shining Generals of the Diamond Kingdom ... Appears in battle as a massive earth golem ... try to make it as something in black clover as possible since there is no image I could find."

There is no reference picture. The look is built from the description and from the art language of the series and of the mod's other Black Clover pieces (flat cel tones, dark thin ink lines, pen hatching on the shaded sides, heavy shapes, a glow in the spirit's own colour, magic circles). Nothing here was seen in a game: every picture is the project's headless rasteriser (section 11).

One **server** switch decides it all: `magic.gnome` in `nusmp-gnome-server.toml` (default `true`; `false` = no Gnome link, every earth page is cast the old way, and the old Earth spirit body is all there is).

## 1. The lore, and where each fact came from (rule 10)

English wiki first, Japanese as the fallback. **No wiki page could be fetched in this session**: `blackclover.fandom.com/wiki/Gnome`, `.../wiki/Feremena`, `ja.wikipedia.org` and `sportskeeda.com` all failed with `getaddrinfo ENOTFOUND` (no route to those hosts). What was reachable is a web-search tool that returns short summaries of those pages, so every fact below is second hand, from search summaries, not from the pages themselves.

| Fact | Source reached | Verdict against the owner's text |
|---|---|---|
| The Earth Spirit's host is **Feremena**, one of the Diamond Kingdom's **Eight** Shining Generals | English wiki page "Feremena" (search summary), Sportskeeda "Black Clover manga finally reveals the Earth Spirit and its host" and "Who is Feremena?" (search summaries) | matches. The English wiki spells the name Feremena; "Feramina" is the owner's spelling (accepted as the other spelling) |
| She is introduced in chapter 382, in the war against Lucius's Guardian Angels, and is the last of the four great spirits to be shown | English wiki "Chapter 382" and the Sportskeeda chapter article (search summaries) | matches "introduced during the final war arc" |
| Moris Libardirt modified her body in the Diamond Kingdom's experiments and counted her his greatest success; dark skin, light hair braided on the right side, a small mage stone on the forehead and a larger one on the sternum, large scars on cheeks and abdomen | English wiki "Feremena" (search summary) | extra detail; the Gnome carries a forehead gem and a gem in the chest circle as the nod to her stones |
| The spirit's **name** "Gnome" / ノーム (Tsuchi no Seirei) | not found in any source that could be reached (the Japanese search for ノーム フェレメナ confirmed Feremena as host and said nothing of ノーム) | **not confirmed.** The sources call it "the Earth Spirit". The name may be fanon (the owner cited `bcfanon.fandom.com/wiki/Gnome`, a FAN wiki, not canon, not fetched: it is treated as fanon) or simply the mod's own name (the mod has called its earth spirit "Gnome" since 0.102) |
| The spirit's **look** ("a massive earth golem") | no text found describes it; a search summary says only that the wiki has an image "Earth Spirit.png" and that Sportskeeda calls it "an elemental entity of immense power", with the spirit seen behind her in a chapter 382 panel | **unverified.** Built as asked anyway |
| A Sportskeeda headline names the host "Patri" | search summary of "Black Clover chapter 382 spoilers: the Earth Spirit host is revealed as Patri" | contradicts its own body text and the wiki; treated as an error |

Plainly for the owner: canon agrees with the host, the rank, the arc and the chapter; it does **not** (in what could be reached) confirm the name "Gnome" or the "massive earth golem" look. Both are built as asked.

## 2. NEW / REPLACED / KEPT (rule 5)

| Part | Status | What |
|---|---|---|
| The Gnome as a prop in two forms (the stone COMPANION beside its master, the FULL giant for the big pages): models, sheet, glow-only models, animations, painter, three levels of detail each | NEW | `gnome/**`, `client/prop/Gnome*`, `tools/gen_gnome_*.py` |
| A NEW sound set of its own (7 cues, 19 files) | NEW | `gnome.*` events; the basic Tensura spirit sounds are not used |
| Server switch, caps, per-player state, the Gnome key, the emitter of the earth spells, cleanup, self-test `magic.gnome` | NEW | `GnomeConfig`, `Gnome`, `GnomeNet`, `GnomeEvents`, `GnomeSelfTests` |
| The nine pages of the Earth book | KEPT | same ids, order, costs, cooldowns, numbers; `Gnome.wrap` only adds the emitter and the slam while a Gnome is linked (`magic.gnome = false` or no link: byte-for-byte the old cast) |
| The exclusive Earth spirit entity (`ElementalSpiritLordEntity`, kind GNOME), its model `spirit_gnome`, its renderer, the Spirit Slots, the Spirit Dive Earth look, the Gnome Plate armour page | KEPT | untouched. The Gnome of this doc is a separate, cheaper prop (like the Salamander prop of the fire casting styles); it borrows the Spirit Lord's job (follows its master, emits the spells) but not its AI |
| The old look of the earth spirit | KEPT | it is the old entity body; `magic.gnome = false` leaves it as the only earth spirit |
| Canon host Feremena's grimoire | NOT BUILT | the repo has no `CanonBook` for her (`CanonBook` has Mars for the Diamond Kingdom's Mineral Magic). The Gnome answers any mage who holds the Earth grimoire. `GnomeRules.isHostCanon` already knows the ids `feremena` / `feramina` for the day a canon book is added; nothing uses it yet |

## 3. The look

### 3.1 Design (from the description, in the series' art language)

A **massive earth golem**: a stone giant of layered rock plates and boulders. Heavy proportions as in the series' big constructs: huge boulder shoulders, thick arms ending in huge rocky fists (bigger than the forearm), a SMALL head sunk forward between the shoulders under a heavy brow, a V-shaped body (narrow pelvis, wide chest), stacked-boulder legs on slabs that stay planted with a clear gap between them.

| Part | What it is |
|---|---|
| Face | a stone mask: heavy brow ridge, two narrow slanted eyes that glow amber with a white-hot core, a nose ridge, a mouth seam with square stone teeth, a small gem on the forehead (the host's mage stone) |
| Chest | a carved MAGIC CIRCLE: two rings, 24 rune ticks, a four-pointed star, a diamond inside it (the Diamond Kingdom's suit) and a clover of four dots; it glows amber, the centre brightest |
| Veins | amber crystal veins that branch like lightning over the fists, the arms, the back and the knees (glow-only plates) |
| Crystals | amber and white-blue "diamond" shards on the head (a crown), the shoulders, the back and the elbows (the nod to the Diamond Kingdom) |
| Moss | flat olive patches on the top faces, hanging moss strands on the shoulders |
| Rune cuffs, belt | bands of square-cut glyphs on both forearms and the belt, glowing |
| Floating stones | six small boulders that bob and turn round the body; a ring of rubble heaves up round the giant's feet |
| Layers | rib plates, pectoral stones, a back plate, a gorget round the head, shin guards, skirt plates, small boulders on the shoulders |

Two forms, one design: the **FULL form** is authored at about 7.5 blocks and drawn at 0.82: **about 6.2 blocks** with its crown. The **COMPANION** is the same design with a head 1.38 times bigger, authored at 0.165 of the size: **about 1.5 blocks**. The pieces that are too small to read at that size are left out of the companion (the loose moss, the small crystals, the thumb stones, the far floating stones).

### 3.2 The skin (`tools/gen_gnome_art.py`, deterministic, seeded per face)

Hand-tuned painters on one 1024 x 1024 sheet (15 regions), in the CEL look: **flat tones only** (five stone tones shade / dark / base / light / highlight, a flat ochre swap on a part of the facets), a **dark ink line** along every facet border and every crack, a **shaded side and a lit rim on every facet**, **pen hatching** (parallel ink strokes) on the shaded facets, flat olive moss with an ink edge, amber veins, the face and the circle painted by hand. No gradients, no photographic noise (a test counts the colours: `GnomeAssetsTest.theSheetIsPaintedInTheCelLookFlatTonesAndInk`).

The glow is **only** a glow-only model with its own half-size glow sheet drawn through the additive pass (the Salamander's way), with **nothing under a transparent pixel** (`tools/safe_additive/scan_textures.py`, scope line added; the safe additive texture test). `RenderType.eyes` is never used. On top, three soft camera-facing amber glow quads (eyes, circle) on the two nearer levels.

### 3.3 Geometry on the silhouette, texture elsewhere; the exact quads

Counted from the model files by `GnomeLodTest` (so these numbers cannot drift from the files). A level costs its body model + the glow-only model + 3 glow quads (levels 0 and 1).

| Form | Level | When (best effect quality) | Body quads | Glow-only quads | Glow quads | **Per frame** |
|---|---|---|---|---|---|---|
| Companion | 0 full | up to 12 blocks | 519 | 58 | 3 | **580** |
| Companion | 1 half | up to 28 | 326 | 31 | 3 | **360** |
| Companion | 2 low | up to 64 | 74 | 0 | 0 | **74** |
| Companion | culled | beyond 64 | | | | 0 |
| Giant | 0 full | up to 32 blocks | 751 | 94 | 3 | **848** |
| Giant | 1 half | up to 64 | 398 | 35 | 3 | **436** |
| Giant | 2 low | up to 112 | 74 | 0 | 0 | **74** |
| Giant | culled | beyond 112 | | | | 0 |

(Brief: about 800 / 1000 at full detail, about 350 / 450 at level 1, at most 140 at level 2, culling 64 / 112: all met.) Levels 1 and 2 read half and quarter size copies of the sheet. A face that is not seen is given no uv and costs nothing; bones are the same in every model, so one animation file drives them all.

**One shared frame budget** `GnomeLod.FRAME_QUADS = 3200` quads for all Gnomes in a frame: the nearest are drawn at the level their distance asks for, each is stepped down a level until it fits, then culled; **the owner's own close companion (within its first two bands) is counted but never dropped**. The distance bands shrink with the effect quality (0.5 to 1). Nothing is allocated per frame (static scratch, static specs, primitive math).

## 4. Animations (`animations/entity/gnome_full|companion.animation.json`, `tools/gen_gnome_geo.py`)

Limbs move by bone rotation only; the body and the ground by position; the stones by position and rotation. Convention checked in the preview: +x swings the part below the pivot forward.

| Clip | Length | Played when | Contact |
|---|---|---|---|
| `idle` | 4 s loop | standing: slow heavy breath, the head turns a little, the arms sway, the stones drift | |
| `walk` | 1.6 s loop (1.2 s companion) | the companion while its master moves: two heavy steps, the body drops onto the planted foot | |
| `rise` | 20 ticks | the giant's first second: the stone bursts out of the ground (the root climbs from 108 units below), both arms up over the head, then both fists slam down | fists land on tick 14 (`FULL_SLAM_TICK`) |
| `cast` | 24 ticks | a big page (cost tier 20 and up): both fists up, then slam the ground | tick 12 (`CAST_CONTACT_TICK`) |
| `punch` | 20 ticks | a page below the big tiers: the right fist winds back and drives forward, the spell leaves it | tick 10 |
| `stomp` | 18 ticks | the giant, once at life tick 62 while it stands: a foot lifts and slams, the body drops with it | tick 10 |
| `sink` | 20 ticks | the giant's last second: the arms sag, the whole body goes down into the ground | |

The clock is the server's: the clip lengths and contact ticks are constants of `GnomeRules`, and `GnomeAssetsTest` reads the animation files and fails if a length or a contact keyframe moves. The client picks the clip from the synced prop word (variant, a PUNCH bit, the tick stamp of the last cast) and the entity's life; it never decides a rule. **Dust and pebbles** are custom particles only (`particle.fx.Fx`, theme `magic.EARTH`), sent by the painter through the client budgets: rubble while the giant rises (3 every second tick), a burst and a ring at each fist landing, pebbles at the companion's feet.

## 5. The role: a Spirit Lord variant of EARTH (`gnome/Gnome.java`)

```
key (client sends Key(0), 10 tick gap, 1 byte) -> Gnome.toggle(player)
    already linked: unlink (the prop is discarded, the slot given back, the "unlink" sound)
    else: needs magic.gnome on AND the Earth grimoire skill; SPIRITS.acquire(player, 1, maxSpirits 48);
          the companion prop is spawned beside the player; state.spirit = its uuid; the player joins LIVE
cast of ANY of the nine Earth pages (Gnome.wrap):
    no state / not linked / magic.gnome off: the page's own cast, unchanged
    linked:  pick the emitter: the giant's fist if it stands, else (cost tier >= 20, cooldown over, FULLS has room) the place the giant will rise, else the companion's fist
             run the page's own cast with the emitter set (the spell's start moves to the fist: GnomeRules.emit, clamped to 7 blocks from the eye)
             if the page returned false nothing else happens (no giant, no cooldown)
             big page: summonFull: the giant prop 4.8 blocks ahead on the ground, 120 ticks, the earth sigil under it, the "rise" sound, the slam contact on tick 14
             else: the stamp of the cast (+ PUNCH below the big tiers) is written to the prop word; the contact effect and sound on the contact tick
upkeep (every 4th tick, only for players in LIVE): lost props are dropped (ledger slots returned), the link ends when the book or the switch is gone,
          while the giant stands the master gets Resistance (refreshed every second, the old Earth spirit's aura, only while the giant stands), an idle rumble every 20 to 40 s
forget (logout, death, respawn, dimension change, server stop): the props are discarded, STATES / LIVE / both ledgers / the sound gate are cleared for the player
```

* The giant: **one per player and 6 server-wide** (`maxFull`), 120 ticks (6 s: rise 20, hold 80, sink 20), cooldown **600 ticks** (`fullCooldownTicks`, 100 to 24000). The companion is hidden while it stands. **48 companions server-wide** (`maxSpirits`).
* The emitter reaches the Boulder Shot (its projectile starts at the fist) through one added call in `book/TensuraShots.java` (chained with the fire casting call). The ground pages (Earth Spikes, Mud Wall, Divided Mother Earth, Witch Hunter Claws, Rampaging Mother Earth, Swallowing Ground) start where they always did, along the caster's line; the giant stands in that line and slams as they erupt.
* All state is per player UUID; nothing is static "current player" (a source scan checks it). The packet is validated on the server (real player, known action, gap, Earth grimoire, switch).
* The spirit's Spirit Dive and its Earth Slam (old entity) are untouched.

## 6. Sound (`tools/gen_gnome_sounds.py`, `gnome/GnomeSound*`, `assets/nusmp/sounds/gnome/*.ogg`)

A new distinct set, **not** the Tensura spirit sounds and not the mob families: deep rumble, stone grinding and crashing, a low resonant earth tone, all written sample by sample by numpy (no recording, deterministic per file name, mono 32 kHz, encoded by ffmpeg Ogg Vorbis, peaks under 0.97 decoded, 324 KB in all). Engines: RUMBLE (low-passed brown noise with a swell, a drifting sub-bass sine, a beating pair a few Hz apart), GRIND (band-passed noise gated by an irregular stick-slip pulse train, faint stone squeal partials), CRASH (a noise burst, a shower of inharmonic stone clacks, a sub thud sweeping 95 to 34 Hz), TONE (a fundamental at 55 to 82 Hz with stone partials 1, 1.51, 2.76, 4.16 that ring and beat).

| Event | Files | Seconds | Volume / range | What | Cooldown per owner |
|---|---|---|---|---|---|
| `gnome.link` | 3 | 1.5 to 1.7 | 0.9 / 16 | the tone swells, grinding, a settling crack | 1 s |
| `gnome.unlink` | 2 | 1.4 to 1.5 | 0.8 / 16 | a descending tone, grinding fading | 1 s |
| `gnome.idle` | 3 | 1.8 to 2.2 | 0.55 / 16 | a quiet grind and a faint tone | 8 s |
| `gnome.cast` | 3 | 0.95 to 1.15 | 1.0 / 16 | a short grind, a crash, a tone | 0.3 s |
| `gnome.rise` | 3 | 2.8 to 2.9 | 2.0 / 32 | deep rumble swell, grinding heave, a crash, a rising tone | 2 s |
| `gnome.slam` | 3 | 1.8 to 2.0 | 2.2 / 35 | crash, thud, a ringing resonant tone | 0.3 s |
| `gnome.sink` | 2 | 2.1 to 2.2 | 1.6 / 25 | rumble, grinding, a falling tone | 2 s |

Wiring: its own `DeferredRegister<SoundEvent>` (`GnomeSounds`, registered by `GnomeBoot`); 7 entries appended at the end of `sounds.json` (the generator rewrites only its own block), subtitle lines in `tools/lang/gnome.json`. **Server driven, nearby only, budgeted**: the server plays a cue with `level.playSound` (sent to the players inside the cue's range only: 16 blocks, or 16 times the volume above 1), through `GnomeSoundRules.Gate`, the pattern of `audio.voice.VoiceLimiter`: a cooldown per cue per owner, at most 3 sounds a second per owner, at most 12 a second on the whole server; state per owner UUID, dropped at logout. Nothing is played by the client on its own.

## 7. Worst-case cost (rule 11)

Server, per server (defaults):

| Thing | Per caster | Server-wide cap | Lifetime | Notes |
|---|---|---|---|---|
| Companion (prop entity) | 1 | 48 | while linked | one position nudge every 5 ticks (4 a second); an orphan or a lost owner ends it |
| Giant (prop entity) | 1 | 6 | 120 ticks, then 600 ticks cooldown | the companion is hidden meanwhile |
| Per-player state | one small record | players with a Gnome | until logout / death / dimension | dropped by `forget` |

Worst case in the world from this feature: 48 + 6 = **54 prop entities** (a caster has at most one of each; both caps are config values). Packets: client to server 1 byte per key press, at most 2 a second per caster (answered at most once per 10 ticks); to nearby clients the props' own entity packets (a position every 5 ticks per companion, a param word per cast), at most 3 effect packets per big cast and 1 per small cast through `VfxSpawn`'s per-tick budget, sounds as above (at most 12 a second server-wide, each only to the players in range). Server per tick: the upkeep runs every 4th tick and only for players in `LIVE` (an empty set costs one `isEmpty`), it does a few hash lookups and no world or entity scan, no chunk load; one scheduled task per cast. `PerfSourceScanTest`'s pins, the new `GnomeSourceScanTest` and `ServerSafetyTest` hold the rules.

Client, per frame: at most **3200 quads shared** by all Gnomes (section 3.3), plus the owner's own close companion (never dropped: 580 quads at most); nothing beyond 64 / 112 blocks; custom particles at most 3 per tick per giant while it rises, then a burst and a ring per landing, 5 pebbles per companion cast, all through the particle budgets. Textures: one 1024 x 1024 sheet (4 MB) + its half and quarter copies + the glow sheet, loaded once for both forms (the companion reads the giant's files).

## 8. Files

New: `gnome/` (`Gnome`, `GnomeRules`, `GnomeLod`, `GnomeLedger`, `GnomeConfig`, `GnomeNet`, `GnomeProps`, `GnomeEvents`, `GnomeBoot`, `GnomeSounds`, `GnomeSoundRules`, `GnomeSelfTests`), `client/prop/GnomeDraw`, `client/prop/GnomePropPainter`, `client/gnome/GnomeClient`; `tools/gnome_geo_lib.py` (the geo kit, a copy of the Salamander's so neither edits the other), `tools/gen_gnome_art.py`, `tools/gen_gnome_geo.py` (`--count` prints the quads), `tools/gen_gnome_sounds.py`, `tools/gnome_scenes.py`, `tools/gnome_preview_sheets.py`, `tools/render_preview/examples/GnomeProp.java`, `tools/render_preview/scenes.d/gnome.json`; 10 geo files, 2 animation files, 10 textures (`gnome_{full,companion}` + `_lod1`, `_lod2`, `_fx`, `_fx_lod1`), 19 `.ogg`; `tools/lang/gnome.json`; tests `gnome/GnomeRulesTest`, `GnomeLodTest`, `GnomeSoundsTest`, `GnomeAssetsTest`, `GnomeSourceScanTest`; `docs/previews/gnome/*.png`.

Edited (additions, append-only one-liners): `prop/PropKind` (+`GNOME_1` at the end), `prop/PropRegistry` (+1 line), `client/prop/PropPainterRegistry` (+1 line), `NUSMP` (+2 lines: `GnomeBoot.init`, `GnomeClient.init`), `book/EarthBook` (the page list goes through `Gnome.wrap(...)`: the pages themselves are not touched), `book/TensuraShots` (the start point of the shot goes through `Gnome.origin(...)`), `assets/nusmp/sounds.json` (7 entries appended), `tools/safe_additive/scan_textures.py` and `SafeAdditiveTextureScanTest` (+1 scope entry each).

## 9. Configuration, key, self-test

* `nusmp-gnome-server.toml`: `magic.gnome` (true), `caps.maxSpirits` (48), `caps.maxFull` (6), `caps.fullCooldownTicks` (600). Unique file name (`ConfigFileNamesTest`). Nothing of the Gnome changes only looks, so there is no client config.
* Key "Gnome (Earth Spirit)", default right bracket, rebindable, Controls > Multiverse of Anime.
* Self-test `magic.gnome` (registered, runs in the CI server smoke test): the prop kind, the 7 sound events and their 19 files, the `sounds.json` entries, the 8 models; the nine Earth page ids in order and the cost tiers; the prop word, the clips, the ledger, the sound gate and the emitter bound; on a stand-in player the companion and the giant spawn, are counted, are capped to one each, and are all gone (entity and ledger) after `forget`.

## 10. Checks that were run

* Type-check against the API stubs: 0 real errors in the new and edited files (the known stub artefacts only); a deliberate wrong call fails as it should.
* JUnit in the shim (pure): `GnomeRulesTest` 9, `GnomeLodTest` 5, `GnomeSoundsTest` 6, `GnomeAssetsTest` 8, `GnomeSourceScanTest` 8 (36 in all, all green); plus, unchanged and green: `ServerSafetyTest`, `ConfigFileNamesTest`, `PerfSourceScanTest`, `FxSourceScanTest`, `FlatPropsSourceScanTest`, `SafeAdditiveTextureScanTest`, `GeoAssetsTest`, `ScreenBlurGuardTest`, `SoundsJsonTest`, `MobSoundSetTest`, `MusicAssetsTest`, `VoiceAssetsTest`, `FireCastRulesTest`, `FireCastLodTest`, `FireCastAssetsTest`, `FireCastSourceScanTest`, `SpiritLooksTest`, `WaterRebuildTest`. One failure that is **not** from this change: `ScaleCastingScanTest.theSharedHooksAreThere` asserts that `SCALE_BALANCE` is the last `PropKind`, which the fire casting merge already broke (`FIRECAST_1/2` come after it); a stale pin, left alone.
* `python3 -B tools/safe_additive/scan_textures.py`: 0 offending textures. `tools/gen_gnome_sounds.py --check`: 19 files, 0.95 to 2.9 s, peaks under 0.97.
* Previews with the real render classes (`python3 tools/render_preview/preview.py gnome_full gnome_companion_solo gnome_scale ...`, 26 scenes, 0 errors), pictures in `docs/previews/gnome/`: `gnome_views.png` (both forms, four views each), `gnome_scale.png` (the player, the companion and the giant in one line, and the giant from the front), `gnome_closeups.png`, `gnome_clips.png` (every clip of both forms frame by frame), `gnome_lod.png` (the three levels). Six rounds of render, look and fix: a blob of cubes, then proportions (a V-shaped body, a gap between the arms and the torso, legs apart), then palette and contrast (grey-brown stone with ochre swaps, the face and the circle), then crystals and layers, then the pen hatching, the forehead gem, the clips and the glow.

## 11. What could not be verified (honest list)

* **Nothing was run in a game or on a server.** The pictures come from the headless rasteriser, which does not draw vanilla's lighting, fog, particles or the player's own animations, and knows no first-person view. The three soft glow quads (eyes, circle) did not show in the preview at all (they are counted in the vertices but too faint or hidden); they were not seen drawn.
* **The lore** (section 1): no wiki page was fetched; the name "Gnome" and the "massive earth golem" look are the owner's, not confirmed by anything reachable.
* **The look against the series:** there is no picture, so "looks like Black Clover" is a judgement from the repo's other Black Clover renders and the series' art language (cel flat tones, ink lines, hatching, glow, magic circle). The silhouette is still blockier than a drawn golem (the models are cubes; plates are rotated to break the box, but it is a box-built giant). The crystals read as capped pillars more than gems.
* **The sounds were never listened to.** They were checked by numbers (duration, peak, level) and spectrograms (a low band, an irregular pulse train for the grinding, a decaying burst for the crash), not by ear; every recipe is a few lines of parameters in the generator and can be retuned.
* The key's default (right bracket) is a guess and may clash with a pack's keys; it is rebindable.
* Balance feel (the giant's cooldown of 30 s, 6 s of Resistance while it stands) is untested. The giant stands on the ground level of its caster (no search for the ground: on a slope it can float or sink a little).
* The ground pages are not moved to the fist (they start where they always did); only the Boulder Shot starts from it. A ledge or a wall in front of the caster can leave the giant standing inside the block it rose in.
* The rise sound plays at tick 0 and the slam sound at tick 14: the spells' own effects start at once, so the first earth effect comes about 0.7 s before the giant's fists land.
* The Gnome link needs the Earth grimoire skill, not a canon grimoire; the host Feremena has no grimoire in the mod (section 2).
* The companion's walk clip does not follow the real stride length of its master; it only plays while the master moves.
