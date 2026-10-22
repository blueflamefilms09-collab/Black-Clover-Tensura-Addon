# The Gnome: the Earth Spirit of Black Clover as a stone golem (key `gnome`)

Owner request: "make the Spirit lord. The Earth Spirit in Black Clover is Gnome (Tsuchi no Seirei), and its host is Feramina (also spelled Feremena), one of the Shining Generals of the Diamond Kingdom ... Appears in battle as a massive earth golem ... try to make it as something in black clover as possible since there is no image I could find."

There is no reference picture. The look is built from the description and from the art language of the series and of the mod's other Black Clover pieces (flat cel tones, dark thin ink lines, pen hatching on the shaded sides, heavy shapes, a glow in the spirit's own colour, magic circles). Nothing here was seen in a game: every picture is the project's headless rasteriser (section 12).

The Gnome is also a **character** (section 7): nine poses, six faces and a voice (a deep, slow, gravelly spoken line, grunts, a chuckle, a snore) that follow what happens to its master.

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
| A NEW sound set of its own (11 cues, 29 files: the 7 of the first build and 4 non-verbal voice cues) | NEW | `gnome.*` events; the basic Tensura spirit sounds are not used |
| Server switch, caps, per-player state, the Gnome key, the emitter of the earth spells, cleanup, self-test `magic.gnome` | NEW | `GnomeConfig`, `Gnome`, `GnomeNet`, `GnomeEvents`, `GnomeSelfTests` |
| A fifth voice role `gnome` in the voice system (`VoiceCatalog`: role, id helper, the list of recorded lines; the two voice tests count them) | NEW (appended after the 120 lines of the other roles; nothing of them moves) | `audio/voice/VoiceCatalog`, `VoiceAssetsTest`, `VoiceLogicTest`, `tools/audio/voice_lines.*` |
| Poses, faces and voice (section 7): 6 new clips, 6 face plates per form, 4 non-verbal sound cues, `GnomeMoodRules`, the spoken lines of the voice system | NEW | `gnome/GnomeMoodRules`, `geo/gnome/*`, `textures/entity/gnome_faces*.png`, `sounds/voice/gnome/*`, `tools/audio/gnome_voice.py` |
| The two eye slabs of the first glow-only model (the eyes were part of `gnome_*_fx`) | REPLACED (inside this build, never released) | the eyes are now the face plate, so they can change; `gnome_*_fx` lost the slab (58 to 57 and 94 to 93 quads); the soft eye halo stayed |
| The prop word of the first build (variant, a PUNCH bit, the stamp) | REPLACED (same build) | variant, hidden bit, 4 pose bits, 3 face bits, a 16 bit stamp: `GnomeRules.word`. The clip functions take the pose now (`fullClip(life, max, pose, since)`) |
| The nine pages of the Earth book | KEPT | same ids, order, costs, cooldowns, numbers; `Gnome.wrap` only adds the emitter and the slam while a Gnome is linked (`magic.gnome = false` or no link: byte-for-byte the old cast) |
| The exclusive Earth spirit entity (`ElementalSpiritLordEntity`, kind GNOME), its model `spirit_gnome`, its renderer, the Spirit Slots, the Spirit Dive Earth look, the Gnome Plate armour page | KEPT | untouched. The Gnome of this doc is a separate, cheaper prop (like the Salamander prop of the fire casting styles); it borrows the Spirit Lord's job (follows its master, emits the spells) but not its AI |
| The old look of the earth spirit | KEPT | it is the old entity body; `magic.gnome = false` leaves it as the only earth spirit |
| Canon host Feremena's grimoire | NOT BUILT | the repo has no `CanonBook` for her (`CanonBook` has Mars for the Diamond Kingdom's Mineral Magic). The Gnome answers any mage who holds the Earth grimoire. `GnomeRules.isHostCanon` already knows the ids `feremena` / `feramina` for the day a canon book is added; nothing uses it yet |

## 3. The look

### 3.1 Design (from the description, in the series' art language)

A **massive earth golem**: a stone giant of layered rock plates and boulders. Heavy proportions as in the series' big constructs: huge boulder shoulders, thick arms ending in huge rocky fists (bigger than the forearm), a SMALL head sunk forward between the shoulders under a heavy brow, a V-shaped body (narrow pelvis, wide chest), stacked-boulder legs on slabs that stay planted with a clear gap between them.

| Part | What it is |
|---|---|
| Face | a stone mask: heavy brow ridge, a nose ridge, a mouth seam with square stone teeth, a small gem on the forehead (the host's mage stone); the **eyes are a face plate with six expressions** (section 7.2): calm, amused, angry, sleepy, surprised, stern |
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

Counted from the model files by `GnomeLodTest` (so these numbers cannot drift from the files). A level costs its body model + the glow-only model + 3 glow quads + the face plate (1 colour quad and 1 glow quad: `GnomeLod.FACE_QUADS`) (levels 0 and 1).

| Form | Level | When (best effect quality) | Body quads | Glow-only quads | Glow + face quads | **Per frame** |
|---|---|---|---|---|---|---|
| Companion | 0 full | up to 12 blocks | 519 | 57 | 3 + 2 | **581** |
| Companion | 1 half | up to 28 | 326 | 30 | 3 + 2 | **361** |
| Companion | 2 low | up to 64 | 74 | 0 | 0 | **74** |
| Companion | culled | beyond 64 | | | | 0 |
| Giant | 0 full | up to 32 blocks | 751 | 93 | 3 + 2 | **849** |
| Giant | 1 half | up to 64 | 398 | 34 | 3 + 2 | **437** |
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
| `sink` | 20 ticks | the giant's last second: the arms sag, the whole body goes down into the ground; the companion when it is unlinked or its master dies | |
| `slam` | 22 ticks | a mid page (cost tier 12): one fist drives down onto the ground | tick 10 |
| `shrug` | 22 ticks | a hit lands on the master: the shoulders heave up, the head rocks, the chest jerks back and settles (the shoulders move by position, the only limb bones that do) | |
| `guard` | 3 s loop | the master is nearly dead: both arms crossed over the chest, held until the server clears it (100 ticks) | |
| `pound` | 36 ticks | the master killed something: a fist beats the chest twice | |
| `doze` | 4 s loop | the master has stood still for 30 s: the head sinks, the arms hang, a slow breath | |
| `kneel` | 40 ticks | the companion at the moment it is linked: it kneels in the earth and rises | |

The clock is the server's: the clip lengths and contact ticks are constants of `GnomeRules`, and `GnomeAssetsTest` reads the animation files and fails if a length or a contact keyframe moves. The client picks the clip from the synced prop word (variant, hidden bit, the POSE the server asked for, the FACE, the tick stamp of that event) and the entity's life; it never decides a rule. The companion's animation positions are scaled with the model (a bob of 1 block on the giant is 0.165 blocks on the companion). **Dust and pebbles** are custom particles only (`particle.fx.Fx`, theme `magic.EARTH`), sent by the painter through the client budgets: rubble while the giant rises (3 every second tick), a burst and a ring at each fist landing, pebbles at the companion's feet.

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
             else: the pose of the tier (punch, slam), the face and the stamp are written to the prop word (section 7); the contact effect and sound on the contact tick
upkeep (every 4th tick, only for players in LIVE): lost props are dropped (ledger slots returned), the link ends when the book or the switch is gone,
          while the giant stands the master gets Resistance (refreshed every second, the old Earth spirit's aura, only while the giant stands), the master's stillness is watched (20 s: an idle remark, 30 s: it dozes, a step wakes it), a held guard ends after 5 s. (The first build's idle rumble, the cue `gnome.idle`, stays registered and in the files; the idle remark of section 7 replaced it in the upkeep.)
forget (logout, respawn, dimension change, server stop): the props are discarded, STATES / LIVE / both ledgers / the sound gate / the line gate are cleared for the player
the master dies: the Gnome mourns for 3 seconds (section 7), then the props are discarded; everything else of the player is cleared at once
hit / kill of the master (LivingDamageEvent.Post, LivingDeathEvent): Gnome.onOwnerHurt / onOwnerKill, only for players in LIVE
```

* The giant: **one per player and 6 server-wide** (`maxFull`), 120 ticks (6 s: rise 20, hold 80, sink 20), cooldown **600 ticks** (`fullCooldownTicks`, 100 to 24000). The companion is hidden while it stands. **48 companions server-wide** (`maxSpirits`).
* The emitter reaches the Boulder Shot (its projectile starts at the fist) through one added call in `book/TensuraShots.java` (chained with the fire casting call). The ground pages (Earth Spikes, Mud Wall, Divided Mother Earth, Witch Hunter Claws, Rampaging Mother Earth, Swallowing Ground) start where they always did, along the caster's line; the giant stands in that line and slams as they erupt.
* All state is per player UUID; nothing is static "current player" (a source scan checks it). The packet is validated on the server (real player, known action, gap, Earth grimoire, switch).
* The spirit's Spirit Dive and its Earth Slam (old entity) are untouched.

## 6. Sound (`tools/gen_gnome_sounds.py`, `gnome/GnomeSound*`, `assets/nusmp/sounds/gnome/*.ogg`)

A new distinct set, **not** the Tensura spirit sounds and not the mob families: deep rumble, stone grinding and crashing, a low resonant earth tone, all written sample by sample by numpy (no recording, deterministic per file name, mono 32 kHz, encoded by ffmpeg Ogg Vorbis, peaks under 0.97 decoded, 366 KB in all, 29 files). The four voice cues (grunt, chuckle, snore, huff) use a fifth engine, GRUFF: a glottal source (irregular impulses at 38 to 80 Hz, softened to low pulses) through vowel formants, plus breath and grit; it is synthesised like the rest and is **not** a recording of anyone. The 7 first cues are unchanged. Engines: RUMBLE (low-passed brown noise with a swell, a drifting sub-bass sine, a beating pair a few Hz apart), GRIND (band-passed noise gated by an irregular stick-slip pulse train, faint stone squeal partials), CRASH (a noise burst, a shower of inharmonic stone clacks, a sub thud sweeping 95 to 34 Hz), TONE (a fundamental at 55 to 82 Hz with stone partials 1, 1.51, 2.76, 4.16 that ring and beat).

| Event | Files | Seconds | Volume / range | What | Cooldown per owner |
|---|---|---|---|---|---|
| `gnome.link` | 3 | 1.5 to 1.7 | 0.9 / 16 | the tone swells, grinding, a settling crack | 1 s |
| `gnome.unlink` | 2 | 1.4 to 1.5 | 0.8 / 16 | a descending tone, grinding fading | 1 s |
| `gnome.idle` | 3 | 1.8 to 2.2 | 0.55 / 16 | a quiet grind and a faint tone | 8 s |
| `gnome.cast` | 3 | 0.95 to 1.15 | 1.0 / 16 | a short grind, a crash, a tone | 0.3 s |
| `gnome.rise` | 3 | 2.8 to 2.9 | 2.0 / 32 | deep rumble swell, grinding heave, a crash, a rising tone | 2 s |
| `gnome.slam` | 3 | 1.8 to 2.0 | 2.2 / 35 | crash, thud, a ringing resonant tone | 0.3 s |
| `gnome.sink` | 2 | 2.1 to 2.2 | 1.6 / 25 | rumble, grinding, a falling tone | 2 s |
| `gnome.grunt` | 4 | 0.5 to 0.8 | 0.8 / 16 | a deep effort grunt: an irregular 60 to 78 Hz "vocal fry" pulse train falling in pitch, shaped by low vowel formants, a little breath and stone grit | 1.5 s |
| `gnome.chuckle` | 2 | 1.0 to 1.3 | 0.7 / 16 | three or four "huh" beats at 70 to 80 Hz, each a short formant-shaped pulse with breath | 6 s |
| `gnome.snore` | 2 | 2.3 to 2.4 | 0.5 / 16 | a rattling breath in (a 24 to 32 Hz flutter on filtered noise), then a low fry-voiced breath out | 10 s |
| `gnome.huff` | 2 | 0.55 to 0.65 | 0.7 / 16 | a breath through stone: shaped noise, a low thump, a faint grind | 3 s |

Wiring: its own `DeferredRegister<SoundEvent>` (`GnomeSounds`, registered by `GnomeBoot`); 11 entries appended at the end of `sounds.json` (the generator rewrites only its own block), subtitle lines in `tools/lang/gnome.json`. **Server driven, nearby only, budgeted**: the server plays a cue with `level.playSound` (sent to the players inside the cue's range only: 16 blocks, or 16 times the volume above 1), through `GnomeSoundRules.Gate`, the pattern of `audio.voice.VoiceLimiter`: a cooldown per cue per owner, at most 3 sounds a second per owner, at most 12 a second on the whole server; state per owner UUID, dropped at logout. Nothing is played by the client on its own.

## 7. Moods: poses, faces and voice (`gnome/GnomeMoodRules.java`, `Gnome.react`)

The owner's addition: the Gnome has different **poses**, **faces** and **voice lines**, and they follow what happens to its master. Which pose, face, grunt and spoken line go with each situation is a table of pure data (`GnomeMoodRules`, no Minecraft class, unit tested by `GnomeMoodRulesTest`); the server applies a reaction (`Gnome.react`); the clients only draw what the synced prop word says.

### 7.1 The table

| Situation | When (server) | Pose (clip) | Face | Cue | Spoken group (chance) | Cooldown per owner |
|---|---|---|---|---|---|---|
| LINK | the key links the Gnome | `kneel` (it kneels in the earth and rises) | calm | | greet (100%) | 1 s |
| UNLINK | the key again | `sink` | calm | | farewell (100%) | 1 s |
| CAST_SMALL | a starter page (Earth Spikes, cost tier 8) | `punch` | stern | `grunt` (40%) | cast (8%) | 0.3 s |
| CAST_MID | a mid page (cost tier 12: Mud Wall, Boulder Shot, Witch Hunter Claws, Clay Guardian, Gnome Plate) | `slam` | stern | `grunt` (60%) | cast (18%) | 0.3 s |
| CAST_BIG | a zone or signature page (tier 20 and up: it calls the giant, or the giant that stands plays it) | `cast`, both fists (the giant keeps its rise) | angry | `grunt` (100%) | cast_big (60%) | 0.3 s |
| HURT | a hit of 1 or more lands on the master, who stays above 30% health | `shrug` | stern | `huff` (80%) | hurt (35%) | 5 s |
| SURPRISE | a single hit of 8 or more | `shrug` | surprised | `huff` (100%) | surprised (70%) | 8 s |
| LOW_HEALTH | the master is at or under 30% health after a hit | `guard`, arms crossed, held 5 s | stern (always at least) | | lowhp (80%) | 20 s |
| KILL | the master kills something | `pound` | amused | `chuckle` (60%) | victory (55%) | 8 s |
| OWNER_DEATH | the master dies | `doze` (it mourns), then sinks 3 s later | sleepy | | death (100%) | none |
| IDLE | the master has stood still for 20 s and nothing happened for 20 s | `idle` | amused | `chuckle` | idle (100%) | 2 min |
| SLEEP | still for 30 s | `doze` | sleepy | `snore` (100%) | doze (60%) | 30 s |
| WAKE | the master moves while it dozes | `stomp` | surprised | `huff` | wake (100%) | 5 s |

Thresholds are constants of `GnomeMoodRules` (`LOW_HEALTH_FRACTION` 0.30, `SURPRISE_DAMAGE` 8, `MIN_DAMAGE` 1, `DOZE_AFTER_TICKS` 600, `IDLE_REMARK_AFTER_TICKS` 400, `GUARD_TICKS` 100); each situation has its own cooldown per owner (`GnomeMoodRules.Cooldowns`, indexed by situation). Stillness is a position compared with a 0.15 block tolerance (looking round counts as still). The pose is a one-shot clip that plays from the tick stamp of the event, or a loop (`guard`, `doze`) that holds until the server sets another pose. The giant plays `slam`, `punch`, `cast`, `shrug`, `guard` and `pound`; it never kneels or dozes (it stands for 6 seconds only).

### 7.2 The faces (`geo/gnome/`, `textures/entity/gnome_faces.png` + `gnome_faces_fx.png`)

The Gnome is a golem, so its face is a **plate**: one quad on the front of the head per expression and per form, painted on a 480 x 280 sheet (colour) with its own glow sheet (additive, the safe additive rule: nothing under a transparent pixel; `gnome_faces_fx` joined the scan). Six expressions (`GnomeRules.FACE_*`): **calm** (two slanted amber bars), **amused** (two upward arcs), **angry** (two sharp slanted blades), **sleepy** (two thin dim lines, the halo behind them cut to a quarter), **surprised** (two round white-hot discs), **stern** (flat heavy bars under the brow). The plate rides the head bone, so it moves with every clip. It costs 2 quads per frame on the two nearer levels (`GnomeLod.FACE_QUADS`), nothing on level 2. The soft amber eye halo is scaled by the face (`EYE_GLOW`: angry 1.3, sleepy 0.25).

The face shown is the one the last event set while it lasts (`GnomeRules.FACE_TICKS` = 100), calm after; a doze is always sleepy, a guard always at least stern. Picture: `docs/previews/gnome/gnome_faces.png` (all six, both forms).

### 7.3 The poses

Nine poses in the picture `docs/previews/gnome/gnome_poses.png` (companion: kneel, slam, guard, stomp, shrug, pound, doze, cast, idle; giant: slam, guard, shrug, pound), each with the face its situation gives it. The new clips (section 4) are `slam`, `shrug`, `guard`, `pound`, `doze`, `kneel`; `cast`, `stomp`, `punch`, `sink` and `idle` were there and are reused. The prop word (`GnomeRules.word`) carries variant (4 bits), hidden (1), pose (4), face (3) and a 16 bit tick stamp, so one synced int carries the whole state: the clients draw from it and decide nothing.

### 7.4 The voice

**Spoken lines.** Voice: ElevenLabs **library voice "Garrison - Rugged and Stoic"** (voice id `xHxp1c5pQOzhWjBqV78M`, male, deep and rough), model `eleven_v4` with audio tags (`[deep, slow, gravelly]` and so on), **one generation per line**, taken from the library: **no voice of any real person was cloned**. It is a different voice from the four of the voice system (System, boss, NPC, Djinn). **Licence: not verified.** A library voice is used under the generating account's ElevenLabs plan and the terms of that plan; whether the plan allows commercial use of the output, and whether the voice's own listing has a restriction, was not checked here; the owner should confirm it before the mod is distributed commercially. The lines are converted by `tools/audio/voice_convert.py`'s own function (mono, 32 kHz, trimmed, loudness-normalised, Ogg Vorbis quality 3) and registered by `tools/audio/gnome_voice.py` as the fifth role `gnome` of the voice system (`VoiceCatalog`), events `voice.gnome_<line>`, no subtitle (a voice line has none; the game's own message carries the text).

The script plans **21 lines in 12 groups** (the situation's group is picked, never the line just said); **15 are recorded**:

| Line id | Text | Group | Recorded |
|---|---|---|---|
| `gnome_greet_1` | Gnome rises. The earth is with you. | greet | yes |
| `gnome_greet_2` | You called. I came. | greet | yes |
| `gnome_farewell_1` | The earth remembers you. Gnome sleeps. | farewell | yes |
| `gnome_farewell_2` | Call again, and I will stand. | farewell | yes |
| `gnome_cast_1` | Stone, answer. | cast | yes |
| `gnome_cast_2` | Earth, rise. | cast | yes |
| `gnome_cast_3` | Hold. Break. | cast | yes |
| `gnome_cast_big_1` | Mother Earth, awake! | cast_big | yes |
| `gnome_cast_big_2` | Let the ground take them! | cast_big | yes |
| `gnome_hurt_1` | Stone does not bleed. | hurt | yes |
| `gnome_hurt_2` | Is that all? | hurt | yes |
| `gnome_hurt_3` | Hm. A scratch. | hurt | yes |
| `gnome_surprised_1` | You are hurt! Careful. | surprised | yes |
| `gnome_lowhp_1` | Gnome stands firm. Behind me. | lowhp | yes |
| `gnome_lowhp_2` | You will not fall. Not while I stand. | lowhp | **no** (no credits left) |
| `gnome_victory_1` | The earth is pleased. | victory | **no** (no credits left) |
| `gnome_victory_2` | Another one crumbles. | victory | **no** (no credits left) |
| `gnome_death_1` | Rest now. The earth will hold you. | death | **no** (no credits left) |
| `gnome_doze_1` | Gnome... waits... in the stone. | doze | **no** (no credits left) |
| `gnome_wake_1` | Hm? We move. | wake | yes |
| `gnome_idle_1` | Still. As the mountain is still. | idle | **no** (no credits left) |

**Six lines are not recorded**, because the ElevenLabs account ran out of credits during the run (it had 10000 in all and 33 left; the credits are shared with other work, one credit per character of the prompt, tags included: the 15 recorded lines cost about 680). Until they exist, the situations SLEEP, IDLE, KILL and OWNER_DEATH make their grunts, chuckles and snores but say nothing, and the second line of the low-health group is absent. **To finish: top up the credits, generate each missing line (the text of each is in `tools/audio/gnome_voice.py`, `python3 tools/audio/gnome_voice.py --missing` prints id and prompt, model `eleven_v4`, voice `xHxp1c5pQOzhWjBqV78M`), save the take as `<raw_dir>/gnome_<line>.mp3`, then run the one command `python3 tools/audio/gnome_voice.py <raw_dir>`**: it converts the takes, registers them in `voice_lines.json`, `voice_report.json`, `sounds.json` and `VoiceCatalog.GNOME_LINES`, and the game uses them (the pools in `GnomeMoodRules` already name all 21 lines; a line without a recording is simply not in the pool). Sizes: the 15 files are 328 KB; with the six missing about 450 KB.

**Non-verbal voice.** `gnome.grunt` (4), `gnome.chuckle` (2), `gnome.snore` (2), `gnome.huff` (2): synthesised (section 6), played by the sound gate like the other cues, with their own subtitles ("The Gnome grunts", ...). They are the Gnome's sounds when it has no line.

### 7.5 Rules of the voice (server driven, nearby only, budgeted)

* Everything is decided on the **server** (`Gnome.react`): the client never chooses a pose, a face or a line.
* A spoken line goes to the players **within 32 blocks of the Gnome** (`LINE_RADIUS`, spectators skipped) through the voice system (`audio.voice.Voice.say`, its own tiny `Say` packet, its own limiter and its `voice.maxPerSecond` server option), **at most one line every 4 seconds to one listener** however many Gnomes speak (`GnomeMoodRules.LineGate`, state keyed by the listener's UUID, dropped at logout), and the same line is not said to the same listener again within 30 seconds. The voice is **not positional** (the voice system plays a line to the listener, not at a point in the world): the 32 block radius decides who hears it, not the distance inside it.
* Each situation has its own cooldown (table above); the cues go through `GnomeSoundRules.Gate` (3 a second per owner, 12 a second server-wide).
* Per-player state (the cooldowns, the stillness tracker, the last line, the doze flag) is in the player's `State`, keyed by UUID, dropped by `forget` (logout, respawn, dimension change, server stop). The master's death keeps the props alive for 3 seconds as a farewell (the prop stays where it stands, the state and ledgers are dropped at once) and then discards them.
* Events listened to (all cheap, all gated by the `LIVE` set, so a player without a Gnome costs one set lookup): `LivingDamageEvent.Post` (the master was hit), `LivingDeathEvent` (the master died, or the master killed something).


## 8. Worst-case cost (rule 11)

Server, per server (defaults):

| Thing | Per caster | Server-wide cap | Lifetime | Notes |
|---|---|---|---|---|
| Companion (prop entity) | 1 | 48 | while linked | one position nudge every 5 ticks (4 a second); an orphan or a lost owner ends it |
| Giant (prop entity) | 1 | 6 | 120 ticks, then 600 ticks cooldown | the companion is hidden meanwhile |
| Per-player state | one small record | players with a Gnome | until logout / death / dimension | dropped by `forget` |

Worst case in the world from this feature: 48 + 6 = **54 prop entities** (the moods add no entity) (a caster has at most one of each; both caps are config values). Packets: client to server 1 byte per key press, at most 2 a second per caster (answered at most once per 10 ticks); to nearby clients the props' own entity packets (a position every 5 ticks per companion, a param word per cast), at most 3 effect packets per big cast and 1 per small cast through `VfxSpawn`'s per-tick budget, sounds as above (at most 12 a second server-wide, each only to the players in range). Moods (section 7.5): a hit, a kill or a death costs one `LIVE` lookup for anybody without a Gnome; for a master with a Gnome out a reaction is one prop word write, at most one cue through the gate and at most one spoken line per listener within 32 blocks. Server per tick: the upkeep runs every 4th tick and only for players in `LIVE` (an empty set costs one `isEmpty`), it does a few hash lookups and no world or entity scan, no chunk load; one scheduled task per cast. `PerfSourceScanTest`'s pins, the new `GnomeSourceScanTest` and `ServerSafetyTest` hold the rules.

Client, per frame: at most **3200 quads shared** by all Gnomes (section 3.3), plus the owner's own close companion (never dropped: 581 quads at most); nothing beyond 64 / 112 blocks; custom particles at most 3 per tick per giant while it rises, then a burst and a ring per landing, 5 pebbles per companion cast, all through the particle budgets. Textures: one 1024 x 1024 sheet (4 MB) + its half and quarter copies + the glow sheet, loaded once for both forms (the companion reads the giant's files).

## 9. Files

New: `gnome/` (`Gnome`, `GnomeMoodRules`, `GnomeRules`, `GnomeLod`, `GnomeLedger`, `GnomeConfig`, `GnomeNet`, `GnomeProps`, `GnomeEvents`, `GnomeBoot`, `GnomeSounds`, `GnomeSoundRules`, `GnomeSelfTests`), `client/prop/GnomeDraw`, `client/prop/GnomePropPainter`, `client/gnome/GnomeClient`; `tools/gnome_geo_lib.py` (the geo kit, a copy of the Salamander's so neither edits the other), `tools/gen_gnome_art.py`, `tools/gen_gnome_geo.py` (`--count` prints the quads), `tools/gen_gnome_sounds.py`, `tools/audio/gnome_voice.py` (the script, converter and registration of the spoken lines), `tools/gnome_scenes.py`, `tools/gnome_preview_sheets.py`, `tools/render_preview/examples/GnomeProp.java`, `tools/render_preview/scenes.d/gnome.json`; 10 geo files + 12 face plate geo files (`geo/gnome/`), 2 animation files (13 clips each), 12 textures (`gnome_{full,companion}` + `_lod1`, `_lod2`, `_fx`, `_fx_lod1`; `gnome_faces`, `gnome_faces_fx`), 29 `.ogg` in `sounds/gnome/` and 15 in `sounds/voice/gnome/`; `tools/lang/gnome.json`; tests `gnome/GnomeRulesTest`, `GnomeMoodRulesTest`, `GnomeVoiceDataTest`, `GnomeLodTest`, `GnomeSoundsTest`, `GnomeAssetsTest`, `GnomeSourceScanTest`; `docs/previews/gnome/*.png`.

Edited (additions, append-only one-liners): `prop/PropKind` (+`GNOME_1` at the end), `prop/PropRegistry` (+1 line), `client/prop/PropPainterRegistry` (+1 line), `NUSMP` (+2 lines: `GnomeBoot.init`, `GnomeClient.init`), `book/EarthBook` (the page list goes through `Gnome.wrap(...)`: the pages themselves are not touched), `book/TensuraShots` (the start point of the shot goes through `Gnome.origin(...)`), `assets/nusmp/sounds.json` (11 `gnome.*` entries and the `voice.gnome_*` entries appended), `tools/safe_additive/scan_textures.py` and `SafeAdditiveTextureScanTest` (+1 scope entry each, `gnome_faces_fx` added to the Gnome's own entry), `audio/voice/VoiceCatalog` (+ the role `gnome`, `gnomeId`, the list `GNOME_LINES` of recorded lines; the other roles are untouched), `VoiceAssetsTest` and `VoiceLogicTest` (the counts add `GNOME_LINES.size()`, the roles list gets a fifth entry), `tools/audio/voice_lines.py` (the manifest takes the Gnome's lines from `gnome_voice.py`), `voice_lines.json` and `voice_report.json` (the gnome entries appended).

## 10. Configuration, key, self-test

* `nusmp-gnome-server.toml`: `magic.gnome` (true), `caps.maxSpirits` (48), `caps.maxFull` (6), `caps.fullCooldownTicks` (600). Unique file name (`ConfigFileNamesTest`). Nothing of the Gnome changes only looks, so there is no client config.
* Key "Gnome (Earth Spirit)", default right bracket, rebindable, Controls > Multiverse of Anime.
* Self-test `magic.gnome` (registered, runs in the CI server smoke test): the prop kind, the 11 sound events and their 29 files, the voice lines and their files, the `sounds.json` entries, the 8 models; the nine Earth page ids in order and the cost tiers; the prop word with pose and face, the clips, every situation of the mood table (a pose that is a clip, a face, a cue and a line group that exist), the ledger, the sound gate and the emitter bound; on a stand-in player the companion and the giant spawn, are counted, are capped to one each, and are all gone (entity and ledger) after `forget`; a reaction writes its pose into the prop word and does not react again inside its cooldown.

## 11. Checks that were run

* Type-check against the API stubs: 0 real errors in the new and edited files (the known stub artefacts only); a deliberate wrong call fails as it should.
* JUnit in the shim (pure): the Gnome's seven test classes, **56 tests, all green** (`GnomeRulesTest`, `GnomeMoodRulesTest` (table, thresholds, cooldowns, stillness, line pools, the per-listener gate), `GnomeVoiceDataTest` (script, manifest, catalogue, `sounds.json`, files, sizes), `GnomeLodTest`, `GnomeSoundsTest`, `GnomeAssetsTest` (13 clips, the 12 face plates and the two face sheets), `GnomeSourceScanTest` (adds the mood and voice rules)); plus green after the change: `ServerSafetyTest`, `ConfigFileNamesTest` (3), `PerfSourceScanTest`, `FxSourceScanTest`, `FlatPropsSourceScanTest`, `GeoAssetsTest` (47 together), `SafeAdditiveTextureScanTest` (3), `VoiceAssetsTest` (5). `VoiceLogicTest` (assertions changed for the fifth role) could **not** be run in the shim (it needs game classes the shim does not have); it is a count and a role list, checked by reading, and CI runs it. One failure that is **not** from this change: `ScaleCastingScanTest.theSharedHooksAreThere` asserts that `SCALE_BALANCE` is the last `PropKind`, which the fire casting merge already broke (`FIRECAST_1/2` come after it); a stale pin, left alone.
* `python3 -B tools/safe_additive/scan_textures.py`: 0 offending textures. `tools/gen_gnome_sounds.py --check`: 29 files, 0.5 to 2.9 s, peaks under 0.97.
* Previews with the real render classes (`python3 tools/render_preview/preview.py gnome_full gnome_companion_solo gnome_scale ...`, 57 scenes, 0 errors in the last full run), pictures in `docs/previews/gnome/`: `gnome_poses.png` (nine poses of the companion, four of the giant), `gnome_faces.png` (the six faces of both forms), `gnome_views.png` (both forms, four views each), `gnome_scale.png` (the player, the companion and the giant in one line, and the giant from the front), `gnome_closeups.png`, `gnome_clips.png` (every clip of both forms frame by frame), `gnome_lod.png` (the three levels). Six rounds of render, look and fix: a blob of cubes, then proportions (a V-shaped body, a gap between the arms and the torso, legs apart), then palette and contrast (grey-brown stone with ochre swaps, the face and the circle), then crystals and layers, then the pen hatching, the forehead gem, the clips and the glow.

## 12. What could not be verified (honest list)

* **Nothing was run in a game or on a server.** The pictures come from the headless rasteriser, which does not draw vanilla's lighting, fog, particles or the player's own animations, and knows no first-person view. The three soft glow quads (eyes, circle) did not show in the preview at all (they are counted in the vertices but too faint or hidden); they were not seen drawn.
* **The lore** (section 1): no wiki page was fetched; the name "Gnome" and the "massive earth golem" look are the owner's, not confirmed by anything reachable.
* **The look against the series:** there is no picture, so "looks like Black Clover" is a judgement from the repo's other Black Clover renders and the series' art language (cel flat tones, ink lines, hatching, glow, magic circle). The silhouette is still blockier than a drawn golem (the models are cubes; plates are rotated to break the box, but it is a box-built giant). The crystals read as capped pillars more than gems.
* **The spoken lines were never heard**, and neither were the synthesised grunts, chuckle, snore and huff: the lines are an ElevenLabs library voice I could not play, checked only by duration, size and level; whether the voice sounds like a stone golem, and whether the licence of the voice allows it in a released mod, is the owner's call (section 7.4).
* **Six of the 21 planned lines have no recording** (the account ran out of credits): `lowhp_2`, `victory_1`, `victory_2`, `death_1`, `doze_1`, `idle_1`. The situations still react with pose, face and cue; they just do not speak. One command finishes it (section 7.4).
* **The spoken lines are not positional**: the voice system plays a line to the listener, so inside the 32 blocks there is no fading with distance and no direction.
* **The poses**: `shrug` and `doze` are small movements and read weakly in a still picture (the shoulders heave, the head sinks); `kneel` was judged from stills only. The face plate sits a hair in front of the head: it was not checked for flicker against the head's own face in a real depth buffer.
* **The sounds were never listened to.** They were checked by numbers (duration, peak, level) and spectrograms (a low band, an irregular pulse train for the grinding, a decaying burst for the crash), not by ear; every recipe is a few lines of parameters in the generator and can be retuned.
* The key's default (right bracket) is a guess and may clash with a pack's keys; it is rebindable.
* Balance feel (the giant's cooldown of 30 s, 6 s of Resistance while it stands) is untested. The giant stands on the ground level of its caster (no search for the ground: on a slope it can float or sink a little).
* The ground pages are not moved to the fist (they start where they always did); only the Boulder Shot starts from it. A ledge or a wall in front of the caster can leave the giant standing inside the block it rose in.
* The rise sound plays at tick 0 and the slam sound at tick 14: the spells' own effects start at once, so the first earth effect comes about 0.7 s before the giant's fists land.
* The Gnome link needs the Earth grimoire skill, not a canon grimoire; the host Feremena has no grimoire in the mod (section 2).
* The companion's walk clip does not follow the real stride length of its master; it only plays while the master moves.

## Update 3.0.0: controls moved to the spirit control plane

The Gnome key (the right bracket) is gone. Link and unlink the Gnome with the Earth grimoire's "Summon Spirit / Release Spirit" page, or `/multiverse spirit summon gnome` / `release`; the giant is the page "Gnome Giant" or `/multiverse spirit ability giant`; "Spirit Combat Mode" lets it cast stone shots with its mana and yours. Only the key was removed. Full description, numbers and costs: `docs/magic/spirit_control.md`.
