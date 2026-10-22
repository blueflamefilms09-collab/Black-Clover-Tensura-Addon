# The Spirit of Darkness (key `spirit_dark`)

Owner request: a Spirit of Darkness for the Dark Magic caster, built to the same bar as the Salamander: a **shadow-wisp companion** (clearly not the basic Tensura spirit) that the owner links with a key, a **powered miniature demon form**, real 3D models with deterministic cel-shaded skins, animations, a new sound set played by the server, bounded mechanics (a little life steal, a short Wither or Blindness mark, a **Shadow Step**), and, added by a later message of the owner, **poses, faces and spoken lines** chosen per situation.

Nothing here was seen in a game. Every picture is the project's headless rasteriser (`docs/previews/spirit_dark/`). See section 12 for what could not be verified.

## 1. Canon and sources (rule 10)

The Black Clover wiki has **no Darkness spirit** (the four Great Spirits of Black Clover are Salamander, Undine, Sylph and the earth spirit); the owner's source is **Tensura lore**: the Great Spirit of Darkness is the progenitor of demons (the primordial demons such as Diablo, and Guy Crimson, come from dark elementals), and the Soul Land inspiration of dark elements (corrosion, shadow manipulation, obscuring the senses).

* English Tensura wiki (`https://tensura.fandom.com/wiki/Great_Spirit_of_Darkness`): **could not be fetched** (`getaddrinfo ENOTFOUND tensura.fandom.com`: no network route to fandom.com).
* Japanese fallback (`https://ja.wikipedia.org/wiki/...` for the Tensura article): **could not be fetched** (`getaddrinfo ENOTFOUND ja.wikipedia.org`).
* A web search returned only fan sites, not a wiki: `myanimethoughts.com` ("Unraveling Tensura's Celestial Tapestry") and `mystiqora.com` ("Seven Primordial Demons in Tensura Explained") both say the Seven Primordial Demons were born from the Great (Holy) Spirit of Darkness and name Diablo as the Primordial Noir (Black). These are fan-written and sometimes inconsistent; they are the only text source used, for that one sentence. The look and the mechanics are the owner's approved design, not wiki text.

## 2. What it is and how it is read

| | The companion (shadow wisp) | The powered form (miniature aristocratic demon) |
|---|---|---|
| Look | a black-violet hooded blob with a mask face (glowing violet eyes and a jagged mouth), two small curled horns, smoke tufts on the crown and cheeks, small ragged bat wings, tiny clawed arms, a hanging cloak of smoke ending in a long chained smoke tail, curls of smoke at the sides and back. About 0.9 block tall (drawn at scale 0.6). Hovers over the owner's **left shoulder** | about 2.3 blocks: swept horns (two pairs), pointed ears, a face with glowing crimson eyes, a black-violet tailcoat with violet lapels, sash and collar points, pauldrons with spikes, lace-edged cravat, boots with claws, a spaded tail with spines, **bat wings** (arm, forearm, three fingers with claws, four membranes with a glowing violet seam), three dark orbs orbiting, a glowing ring of smoke on the ground. Hovers **behind** the owner |
| Called by | the link key | the power key, only while linked; 15 s, then 60 s before it may come again; one per player, 6 on the server |
| Own model, own sounds | yes (`spirit_dark_companion*`, `spirit.dark.*`) | yes (`spirit_dark_powered*`) |

Keys (Controls > Multiverse of Anime, all rebindable): link or unlink `]`, Shadow Step `;`, call or dismiss the demon `'`. The defaults are guesses and may clash with a pack's keys. Only a player who holds **Dark Magic** (the Dark grimoire's skill, `book_dark`) may link; the server config `magic.spiritDark` (default true, `nusmp-spiritdark-server.toml`) switches the whole gameplay off (nothing can be linked, a linked spirit is dismissed at the next upkeep).

## 3. Mechanics (server decides; every number is a constant of `spiritdark/SpiritDarkRules`)

While the companion is linked:

| Rule | Numbers |
|---|---|
| **Life steal** on a landed melee strike (player attack damage type) on a mob or player that is not an ally | 12 % of the damage dealt, at most **1.5 HP per strike** and **3.0 HP per second** (powered form: 2.0 and 4.0); heals the owner with `heal()`, only when hurt. The existing law's heal decay is not applied (it would make a capped steal worthless); the spirit's own caps are stricter than its 50 % cap. The older Dark Cloaked Guard steal (12 %, cap 6) is untouched and stacks with it |
| **Mark**: Wither or Blindness on the struck foe | marks alternate, Wither first: Wither I for 60 ticks (3 s; 80 powered), Blindness for 40 ticks (2 s); **once per foe per 100 ticks (5 s)**; at most 16 foes remembered per player (oldest dropped) |
| **Shadow Step** (key) | a validated teleport of up to **14 blocks** (never below 3), same dimension only, cooldown **180 ticks (9 s)**, 140 while the demon is out, a refused step may be asked again after 20 ticks. The server checks (pure `ShadowStep`, tested on a fake grid): the chunk is **loaded** (`hasChunkAt`, never loads one), the height is inside the world's limits, two free cells for the body, a **sturdy floor** (so never over the void or a lava lake), no fire, lava, magma, cactus, berry bush, wither rose, powder snow or cobweb in the three cells, a clear sight line from where the owner stands (so never through a wall), then the **real hit box** fits (`noCollision`) and the spot is inside the world border. It scores candidates (a fan of 8 directions around the look times 6 distances, 48 columns, each scanning 8 cells) by distance, **prefers dark spots** (light level <= 7: +5 and -0.25 per light level), and checks the sight line of at most the 5 best. The owner may not be riding, asleep, dead or a spectator. Fall distance is reset (the spot has a floor within a block or two) |
| **No** hunger, **no** percent of maximum health damage, **no** instant kill | the spirit never deals damage of its own (a source scan test forbids `hurt(` and any food-data call) |
| Caps | 1 companion and 1 powered form per player; `magic.caps.maxSpirits` 48 and `maxPowered` 6 server wide (server config); all state is per player UUID |
| Cleanup | logout, death, respawn, dimension change, server stop (`forget`/`clearAll`: entities, ledgers, sound gates); the farewell prop plays its dissolve and ends itself (at most 45 ticks) |

### 3.1 Flows

```
key packet (1 byte: 0 link, 1 step, 2 power) -> SpiritDarkNet: sender is a ServerPlayer, action in 0..2
    SpiritDark.onKey: at most one ask per 10 ticks, eligible(p) = switch on and Dark Magic held, else ignore/forget
    link:   SPIRITS.acquire(1 per player, maxSpirits) -> spawn the prop (life 1,200,000 ticks, ends with the link) -> chime + dark burst -> mood LINK
    step:   cooldown, alive, not riding/asleep -> ShadowStep.pick (pure) -> lastLook (border, hit box) -> burst, sound, teleport, burst, sound -> mood STEP
    power:  needs link, cooldown, POWERED.acquire(1, maxPowered) -> prop (300 ticks) -> companion hidden (prop word bit) -> mood POWER_CALL
upkeep (every 4th tick, only players in LIVE): props lost -> release; the demon's last 36 ticks -> farewell; eligibility every 20 ticks; ambient whisper / flutter; idle mood
LivingDamageEvent.Post: a landed melee strike of a linked owner -> life steal, mark; the owner hurt -> mood DAMAGED / LOW_HEALTH
LivingDeathEvent: the owner killed a mob -> mood KILL; the owner died -> mood OWNER_DIED, then forget
GrimoireBook.castPage (one appended line): a Dark or Shadow cast -> mood CAST
```

### 3.2 Ties to the existing Dark and demon content

The gate is the existing Dark book skill (`NUSkills.BOOK_DARK`); the bursts use the existing Dark effect shape (`VfxShape.DARKFX_BURST`) and the Dark particle theme (`magic.DARK`) through their budgets; the cast mood listens to Dark and Shadow casts (Dark Cloaked Dimension Slash, Black Hole, Shadow Embrace ... are untouched). The demon-progenitor lore is the look of the powered form (horns, bat wings, tail, tailcoat). The older exclusive Spirit Lords (`ElementalSpiritLordEntity`), the Tensura-style small spirits (`SpiritLordEntity`, including the Black Devil kind), the dark props (`DARK_1`, `DARK_2`) and every Dark page are kept.

## 4. Models, skins and the exact quads (rule 11)

Real geo models built by `tools/gen_spirit_dark_geo.py` (kit `tools/spirit_dark_geo_lib.py`: free-angle limbs and plates; skins by `tools/spirit_dark_art.py`; poses by `tools/spirit_dark_poses.py`; faces by `tools/spirit_dark_faces.py`). The skins are deterministic and hand tuned, **cel shaded**: flat dark violet and black bands with hard steps, an ink line round every region, a lit rim on the top and left edge, glowing parts (eyes, grin, runes, seams, smoke wisps, the ground ring) **only** in the glow sheets, drawn additive through the safe additive path; the wing membranes are alpha-cut triangles with scalloped edges (no flat white cut-outs). Wings, tail, coat and smoke move by bone rotation only.

Quads are counted from the model files (`SpiritDarkLodTest` reads them, so these numbers cannot drift):

| Form | Level | When (distance) | Body | Face model (every level) | Glow-only model + face glow (levels 0 and 1) | **Quads per frame** |
|---|---|---|---|---|---|---|
| Companion | full | up to 16 blocks | 374 | 1 | 34 + 1 | **410** |
| Companion | level 1 | up to 36 | 286 | 1 | 34 + 1 | **322** |
| Companion | level 2 | up to 64 | 40 | 1 | 0 | **41** |
| Companion | culled | beyond 64 | 0 | 0 | 0 | 0 |
| Powered | full | up to 28 blocks | 637 | 2 | 54 + 2 | **695** |
| Powered | level 1 | up to 64 | 347 | 2 | 54 + 2 | **405** |
| Powered | level 2 | up to 112 | 109 | 2 | 0 | **111** |
| Powered | culled | beyond 112 | 0 | 0 | 0 | 0 |

(The distances are at the best effect quality and are scaled down to half on a weaker setting: `SpiritDarkLod.level`.) The sheets: full 512 x 512 per form, level 1 half, level 2 a quarter, the glow-only sheet half size; the seven face sheets per form are tiny (160 x 48 and 72 x 48, colour and glow). The whole asset set (textures, geo, animations) is about 1.8 MB.

**Shared frame budget**: `SpiritDarkLod.FRAME_QUADS = 3200` quads for all spirits in one frame. The nearest are drawn at the level their distance asks for, then each is stepped down a level until it fits, then culled. **The owner's own spirit within 12 blocks is never degraded and never culled by the budget** (it is spent first, so everybody else gives way); worst case in one frame is the cap plus the owner's own full forms. Nothing is allocated per frame (static specs and scratch arrays; no `LazyBuffers`, the passes are sequential model draws), the smoke particles go through the client particle budget once per game tick, the painter places the form from its owner's interpolated body every frame (the server nudges the entity every 5 ticks only).

## 5. Poses, faces and voice (the owner's later addition)

### 5.1 Poses (`DarkMoodRules.Pose`, clips in `animations/entity/spirit_dark_<form>.animation.json`)

lounging aristocrat idle (companion rest), bow, smug with arms crossed, shadow-step vanish and appear, cast with claws, life-steal feast, hurt recoil, cackle, powered demon battle stance (powered rest), farewell dissolve. The powered form plays `flap` while its owner moves. Picture: `docs/previews/spirit_dark/spirit_dark_poses.png`. A pose runs for its own ticks (table in `Pose`), then the form returns to rest; the end of a one-shot clip is not blended into the rest pose (a small pop).

### 5.2 Faces (`DarkMoodRules.Face`; **no extra quads**)

smug, grin, angry, bored, surprised, sinister, amused. The head's face plates are a small model of their own (`spirit_dark_<form>_face`, the same bones as the body so every clip moves it; the body model leaves out the faces it replaces) and the mood is only a different small sheet (`spirit_dark_<form>_face_<mood>.png` colour, `..._faceglow_<mood>.png` additive). Picture: `spirit_dark_faces.png`.

### 5.3 Which one, when (`DarkMoodRules`, pure, with cooldowns; test `DarkMoodRulesTest`)

| Situation | Pose | Face | Voice group (chance, voice gap) | Event gap |
|---|---|---|---|---|
| link | bow | smug | link (100 %) | 40 |
| unlink | farewell | amused | unlink (100 %) | 40 |
| the owner casts a Dark or Shadow spell | cast | grin | cast (35 %, 600) | 30 |
| Shadow Step | appear | smug | step (40 %, 400) | 20 |
| the owner is hurt | recoil | surprised | hurt (50 %, 400) | 60 |
| a life steal strike | feast | sinister | feast (25 %, 600) | 100 |
| idle, now and then (900 to 1800 ticks) | rotates smug / bow / lounge, faces bored / amused / sinister / smug (the powered form only changes its face) | | idle (60 %, 900) | 900 |
| the owner is at 30 % health or less | battle | angry | low (100 %, 900) | 900 |
| the owner killed something | cackle | amused | kill (40 %, 500) | 200 |
| the owner died | farewell (the prop is removed with the owner, so only the line is heard) | surprised | died (100 %) | 0 |
| the demon is called | appear, then its battle stance | angry | power (100 %) | 100 |
| the demon leaves | farewell | bored | power_end (100 %) | 40 |

A stronger pose is not interrupted by a weaker one (priorities in `Pose`); two lines of one owner are never closer than 60 ticks; a face is worn for 300 ticks and then the form's base face (companion smug, powered sinister) comes back. The pose and face travel in the prop's synced param word (4 + 3 bits and a 20 bit tick stamp), so there is **no new packet** for moods.

### 5.4 Spoken lines (rule 11 limits; `SpiritDarkVoiceCatalog`, `SpiritDarkSounds.say`)

47 short lines of a smooth, mocking, aristocratic voice in 12 groups (the plan with every prompt: `tools/audio/spirit_dark_lines.json`; generator and converter: `tools/audio/spirit_dark_voice.py`). Voice: ElevenLabs library voice **"Oliver - Warm British Narrator"** (id `lo1O1Dz1Tu4FWVEPaxuT`, a library voice, not a clone of anyone; not one of the voices of the older lines: David, Final Boss, Justin, Obsidian), model `eleven_v4`, one take per line, converted to mono 32 kHz Ogg q3 with silence trimmed and loudness normalised. **Licence caveat for the owner:** library voices can carry their own conditions on top of the plan's licence; the commercial-use terms of this voice were **not** checked or cleared and must be checked before a public release.

* The game reads `assets/nusmp/spirit_dark_voice.json` (id to text), written by `spirit_dark_voice.py sync` from the recordings that **exist**. A line without a recording is never registered, picked or played; a group with no spoken line falls back to a synthesised cue (link to the chime, hurt and feast to the siphon, cast and kill to the whisper, ...). Nothing is faked.
* Played by the server to players within **32 blocks** only, one by one: each listener hears **at most one line per 4 seconds** (whoever speaks), the same line not again for 30 seconds, the server starts at most 4 lines per tick; per-event and per-owner gaps in 5.3; `magic.spiritDarkVoice` (server) switches the voice off. The text of a line also goes to the owner's action bar. The events are `spirit.dark.voice.<id>` in `sounds.json` (no subtitle, like the older voice lines), files `sounds/spirit/dark/voice/<id>.ogg`, the sound category `VOICE` (the client's own voice slider).
* Recording status is in section 11.

## 6. Sounds (NEW sound set, synthesised)

`tools/gen_spirit_dark_sounds.py` writes eight cues (19 files, mono 32 kHz, all under 2.4 s, 0.4 MB) from nothing with numpy and ffmpeg, deterministically: `spirit.dark.whisper` (low breathy whispers through moving vowel formants over a sub drone and a dark echo), `chime` (inharmonic bell partials a minor third apart), `flutter` (leathery bat-wing flaps with a tremolo), `step` (the Shadow Step whoosh: a swept noise band falling away and a sub sweep), `arrive` (a rising sweep ending in a soft low thud and a faint chime), `strike` (a thump and a descending ring-modulated siphon), `unlink` (a falling chime and a last breath), `powered` (a deep drone swell with a growl, a bell and whispers). Registered on a register of their own (`SpiritDarkSounds`), entries in `sounds.json` (with `attenuation_distance` per cue and subtitles from `tools/lang/spirit_dark.json`). Played by the **server** to nearby players through `SpiritDarkSoundRules.Gate`: a gap per source and cue (14 to 300 ticks) and a server-wide allowance of 6 sounds per tick. Ambient: a whisper every 18 to 30 s per companion, a flutter every 3.5 to 6.5 s for a powered form, both only for linked owners.

## 7. Worst-case cost (rule 11)

| Thing | Per player | Server wide | Notes |
|---|---|---|---|
| Prop entities | 1 companion + 1 powered (15 s) | 48 + 6 = **54** | nudged every 5 ticks; a farewell prop lives at most 45 ticks after its link ends |
| Server per tick | the upkeep every 4th tick, only for players in `LIVE`; no world scan; one set lookup per player per tick; the damage/death handlers return at once when nobody has a spirit | | |
| Shadow Step | 1 per 9 s (7 s powered): <= 48 candidate columns x 8 cells of block reads and <= 5 sight-line clips, then 1 hit-box check | | never on a tick, never loads a chunk |
| Strike | an O(1) ledger and window update; at most 16 foes remembered per owner | | |
| Packets | client to server: 1 byte per key press, at most 1 per 10 ticks per player. Server to client: the prop's ordinary data (the param word changes only on a mood, which has its own cooldowns: at most about one per 20 ticks per owner), the effect packets through the effect budget, the sounds through the gate (6 per tick), voice lines (<= 4 per tick, one per listener per 4 s) | | nearby only (sounds by range, voice <= 32 blocks, effects through the budget) |
| Client per frame | <= 3200 quads shared (plus the owner's own full forms: 695 + 410 at most), levels and culls as in section 4, no allocation | | |

## 8. NEW / REPLACED / KEPT (rule 5)

| | What |
|---|---|
| NEW | the whole `spiritdark` package (rules, ledger, Shadow Step search and probe, mood director, voice catalogue and gates, config, sounds, net, events, props, self-test), `client/spiritdark/SpiritDarkClient`, `client/prop/SpiritDarkDraw` and `SpiritDarkPropPainter`, the two forms with 3 levels, a glow-only model, a face model and 7 mood sheets each, 12 + 11 clips, the generators and the preview scenes, the 8-cue sound set, the voice pipeline and plan, the lang fragment, tests, this doc |
| REPLACED | nothing |
| KEPT | the Dark book and every page id and number (`DarkBook`, `DarkArts`), the existing Dark lifesteal of Dark Cloaked Guard, `ElementalSpiritLordEntity`, `SpiritLordEntity` (incl. the Black Devil kind), `SpiritSlots`, the Dark props and layers, every older sound entry and the other builders' Spirit work |

Existing files touched (append-only one-liners): `prop/PropKind` (+`SPIRITDARK_1`), `prop/PropRegistry`, `client/prop/PropPainterRegistry`, `NUSMP` (the boot line and the client line), `book/GrimoireBook.castPage` (one line after the cast), `assets/nusmp/sounds.json` (the `spirit.dark.*` block, inserted after `zagred_phase` so appends at the end do not collide), `tools/safe_additive/scan_textures.py` (one scope line).

## 9. Tests

* Pure JUnit (all pass in the shim): `SpiritDarkRulesTest` (caps, windows, ledgers, prop word, anchors, sound gate), `ShadowStepTest` (a fake grid: range, dark preference, headroom, void, hazards, unloaded chunks, walls, slopes, bounded clips), `DarkMoodRulesTest` (the table, cooldowns, priorities, idle rotation, client return to rest), `SpiritDarkLodTest` (the geo files against the table, budgets, culls, owner protection), `SpiritDarkVoiceTest` (plan, recordings, data file, sounds.json, 3 MB, per listener gate), `SpiritDarkAssetsTest` (models, sheets, bones, clips, sounds, lang, previews), `SpiritDarkSourceScanTest` (server friendly, no hunger, no world scan, packets validated, no per-frame allocation, hooks).
* The guards that scan it, all passing in the shim: `ServerSafetyTest`, `ConfigFileNamesTest`, `FlatPropsSourceScanTest`, `PerfSourceScanTest`, `FxSourceScanTest`; the safe additive scan (`tools/safe_additive/scan_textures.py`, scope line added, 0 offending); `tools/geo_assets_check.py --prefix spirit_dark_` (the `GeoAssetsTest` rules, 12 models, 0 problems).
* Self-test `magic.spirit_dark` (`SpiritDarkSelfTests`, in the CI server smoke test): registered events and files, the rules, the Shadow Step search against the real level, a stand-in player linking and being forgotten (entity and ledger).
* Previews: `python3 -B tools/spirit_dark_scenes.py` then `python3 -B tools/render_preview/preview.py sd_powered_game ...`; the sheets: `python3 -B tools/spirit_dark_sheets.py`. Pictures in `docs/previews/spirit_dark/`: `powered_demon.png`, `companion_wisp.png`, `lods.png`, `spirit_dark_poses.png` (+ one per form), `spirit_dark_faces.png`.

## 10. Files

New: `src/main/java/com/newuniverse/nusmp/spiritdark/*` (SpiritDark, SpiritDarkRules, ShadowStep, ShadowProbe, DarkMoodRules, SpiritDarkLod, SpiritDarkConfig, SpiritDarkSounds, SpiritDarkSoundRules, SpiritDarkVoiceCatalog, DarkCapLedger, SpiritDarkProps, SpiritDarkEvents, SpiritDarkNet, SpiritDarkBoot, SpiritDarkSelfTests), `client/spiritdark/SpiritDarkClient`, `client/prop/SpiritDarkDraw`, `client/prop/SpiritDarkPropPainter`; `tools/gen_spirit_dark_geo.py`, `spirit_dark_geo_lib.py`, `spirit_dark_art.py`, `spirit_dark_poses.py`, `spirit_dark_faces.py`, `spirit_dark_scenes.py`, `spirit_dark_sheets.py`, `gen_spirit_dark_sounds.py`, `audio/spirit_dark_voice.py`, `audio/spirit_dark_lines.json`, `audio/spirit_dark_voice_report.json`, `render_preview/examples/SpiritDarkProp.java`, `render_preview/scenes.d/spirit_dark.json`, `lang/spirit_dark.json`; assets `geo/entity/spirit_dark_*`, `animations/entity/spirit_dark_*`, `textures/entity/spirit_dark_*`, `sounds/spirit/dark/**`, `spirit_dark_voice.json`; tests `src/test/java/com/newuniverse/nusmp/spiritdark/*`.

## 11. Voice recording status

STATUS_PLACEHOLDER

## 12. Not verified (honest list)

* **Nothing was run in a game or on a server.** The type check was against API stubs (the only errors are the known stub artefacts "is abstract; cannot be instantiated"; a deliberate wrong call fails as it should), the JUnit run in a shim without Minecraft; tests that need the game classes (`MagicEnumAuditTest`, `GeoAssetsTest` itself, the self-test) did not run in the shim and are left to CI; the `GeoAssetsTest` rules were applied by `tools/geo_assets_check.py`.
* **The look** is judged only in the headless preview, which does not draw vanilla's lighting, fog, particles or the player's own skin. The models are box based (like the Salamander): the head and the body are blocky next to a smooth demon, the cel shading is a texture effect and its linework thins on the lower levels. The poses are judged from stills: the arm crossing of the smug pose, the claws of the cast pose and the cackle's head throw are read from single frames; the end of a one-shot pose pops back to rest. The glow strength in a dark cave and the ground ring on uneven ground are untested.
* **Nothing was listened to.** The eight synthesised cues were checked only by their level, length and spectral centroid; the spoken lines were not heard (wording, accent, pacing and the match to the character are unchecked; the silence trim threshold of -45 dB may clip a breathy start or end).
* The **key defaults** (`]`, `;`, `'`) are a guess. The balance feel (life steal share, mark lengths, step range and cooldown, demon duration) is untested. The powered form's hover place and the companion's size next to a first-person view are untested; the companion at scale 0.6 may feel small or large.
* The prop is placed from the owner's body; a **vehicle or elytra** owner, a very fast fall and a **Shadow Step across a chunk border at the world edge** were not exercised. A teleport mid-chunk-load relies on `hasChunkAt`.
* `book_dark` as the gate means a Dark Magic caster of any canon grimoire may link; the spirit commands of the older systems (`/multiverse spirit_*`) are not extended (no admin command for this spirit).
* The mood hook is one appended line in `GrimoireBook.castPage`; if another builder moves that method the hook must be re-added.
