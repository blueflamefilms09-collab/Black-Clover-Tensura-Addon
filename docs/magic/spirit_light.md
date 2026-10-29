# Spirit of Light (key `spirit_light`): a golden lion cub that grows into a many-eyed seraph

Owner-approved design, added on top of Light Magic. **No existing Light Magic page, skill, number or texture was changed.** The spirit is a companion and a short powered form; every number below is capped on the server.

Nothing here was seen in a running game. Every picture is the project's headless rasteriser (`tools/render_preview`), see section 9 and the "Not verified" list at the end.

The whole feature is one **server** config switch: `magic.spiritLight` in `nusmp-spiritlight-server.toml` (default `true`; `false` and nobody can link a spirit).

## 0. NEW, REPLACED, KEPT

| | What |
|---|---|
| **NEW** | Everything of the Spirit of Light: the link, the cub, the seraph, the piercing beam, the Glowing pulse, Swiftness III while linked, the mood rules (poses and faces), the sound set, the spoken lines of the seraph, the key bindings, the config file, the self-test, the previews. Two prop kinds (`SPIRIT_LIGHT_1`, `SPIRIT_LIGHT_2`) appended at the end of `PropKind`. |
| **REPLACED** | Nothing. No Light spirit creature existed in the mod. The only older mention is a relic socket gem called "the Spirit of Light" (`relics/socket/SocketCatalog`, lang `socket.nusmp.spirit_light`): it is an item socket, a different feature, so it is kept as it is and the new spirit is added next to it. |
| **KEPT** | Light Magic (`book/LightBook`), all its pages, the old Light VFX, the other spirits (Salamander, Sylph, Undine, Gnome, Darkness, Space), the voice catalogue (`VoiceCatalog`, 120 lines, 4 roles) and its asset test, the Time Magic VFX (not touched, `FlatPropsSourceScanTest.theTimeMagicFilesAreUntouched` passes). |

## 1. What the player gets

| Part | What it does |
|---|---|
| Link key (`]`) | Links or unlinks the spirit. Only a Light Magic caster can link (`LightSpiritBridge.hasLightMagic`). One spirit per player. |
| Link key with sneak | Raises the **seraph** for 60 s (1200 ticks); cooldown 30 s (600 ticks) after it ends. One powered form per player, 6 on the whole server. |
| Command key (`[`) | The **piercing light beam**. |
| Command key with sneak | The **Glowing pulse**: marks hostile mobs through walls. |
| While linked | **Swiftness III** (amplifier 2), a short ambient effect (60 ticks, refreshed below 40), removed on unlink, logout, death and dimension change. |

Never: hunger or exhaustion, damage as a percent of max health, an instant kill.

### 1.1 Numbers (all in `SpiritLightRules`, all with a hard cap)

| | Cub | Seraph | Hard cap |
|---|---|---|---|
| Beam raw damage (goes through `LightBook.lightHit`, so Light Magic's own scaling, resistances and cooldown rules apply on top) | 3.0 | 4.5 | 5.0 (`BEAM_RAW_CAP`), then the config `beamDamageScale` 0..1 |
| Beam pierce (targets hit) | 2 | 4 | 5 |
| Beam range (blocks) | 22 | 30 | 32 |
| Beam cooldown (ticks) | 30 | 24 | never under 16 |
| Beam candidates looked at | 256 at most | | |
| Pulse radius (blocks) | 12 | 18 | 24 |
| Pulse targets marked | 12 | 20 | 24 |
| Pulse Glowing duration (ticks) | 100 | 140 | 200 |
| Pulse cooldown (ticks) | 400 | 400 | never under 200 |
| Pulse scan | one box search per pulse, at most 512 looked at, at most 32 line-of-sight candidates kept | | |

The beam and the pulse each do one `getEntitiesOfClass`, and only after their own cooldown has passed. Nothing scans per tick. The pulse marks hidden targets (behind walls) first. Config knobs (`nusmp-spiritlight-server.toml`): `magic.spiritLight`, `caps.maxSpirits` (default 48), `caps.maxPowered` (6), `caps.maxEffectProps` (24), `beamDamageScale` (0..1, can only turn damage down).

### 1.2 Server state

* Per player, keyed by UUID: `SpiritLight.State` (cooldowns, power timers, the two prop ids, the mood, the last position).
* Ledgers (`SpiritLightLedger.Counted` / `Timed`, pure): spirits 48, powered 6, effect props 24 (3 per player), each acquire fails when full; a `Timed` slot expires by itself.
* The player tick only visits players in the `LIVE` set, and only every 4th tick.
* Cleanup on logout, death (the killer's spirit cheers, the dead player's spirit grieves, then it is forgotten), respawn, dimension change and server stop: the props are discarded, the ledgers released, Swiftness removed.

## 2. How it looks (the building standard)

Real Bedrock-style geo models, drawn by `client/prop/SpiritLightDraw` through the existing prop painter registry. Deterministic cel-shaded skins (gold and white, thin dark linework) made by `tools/gen_spirit_light_art.py`; the glow is a separate `_fx` model drawn in the additive pass with textures that carry no colour under transparent pixels (the SafeAdditive scan passes).

### 2.1 The cub

About 0.8 blocks tall (model scale 0.25), a golden lion cub: square head with a mane ring of fur chevrons, mane tuft on top, tail with a tuft, a halo that turns, four paws. It floats a little beside the owner's shoulder and bobs.

### 2.2 The seraph

About 2.1 blocks tall (model scale 0.46): a robed body of light, a calm face, **three pairs of wings covered in eyes**, three **gyroscope rings** turning around the body with eyes along them, a floating **prism** above the head. (The look follows the "biblically accurate" angel idea; the face is deliberately serene.)

### 2.3 Poses and faces (owner addition)

| | Poses (clip name = pose name) | Faces |
|---|---|---|
| Cub (8 asked, 10 built) | `idle`, `cast`, `pounce`, `roll`, `stretch`, `lick`, `roar`, `sleep`, `hurt`, `victory` | `happy`, `sleepy`, `fierce`, `surprised` |
| Seraph | `idle`, `cast`, `wings_open`, `halo_spin`, `bless`, `prayer`, `hurt`, `victory` | `serene`, `stern`, `compassionate`, `radiant` |

A face is a set of overlay slabs on the head; the clip scales the slabs of the face it wants to full size and the others to nothing, so a pose and its face always go together.

**Which pose when** is decided on the server by the pure class `LightMoodRules` (so it can be tested without Minecraft). Situations: link, unlink, power up, power down, beam, pulse, hurt (the owner took damage), idle fidget (after 400 ticks standing still, then every 900), idle rest (after 1200 ticks), wake, owner low health (30 percent or less), owner killed something, owner died. Every situation has a cooldown and a priority; a loop pose (idle, sleep, prayer) holds until the next reaction; a one-shot pose ends and falls back to idle. The pose goes in the prop's synced int word (bits 4..7), so the client needs no packet for it.

### 2.4 Sounds

A **new** synthesised set (`tools/gen_spirit_light_sounds.py`, numpy plus ffmpeg libvorbis, 11 events, 24 ogg files, about 362 KB): bright chimes (link, unlink), a choir-like pad (power), the beam zap, the pulse swell, and for the cub **creature sounds only, no words**: purr, mew, small roar, yawn, chirp, whimper (each with variants). They are played by the server to players within 48 blocks only, through `PerfBudgets.effectPacket`, with a per-kind, per-player gap (`SOUND_GAPS`, for example the purr at most once per 20 s).

### 2.5 The seraph's voice

The seraph (and only the seraph) speaks, in a clear, resonant, gentle voice: **35 short lines** are written (`tools/audio/spirit_light_lines.json`, text also in `tools/lang/spirit_light_voice.json`), cues greeting, farewell, rise, settle, beam, pulse, damage, owner low health, owner wins, owner dies and idle. The cub never speaks (creature sounds only).

**State of the audio: all 35 lines have a real recording (3.34.0).** 13 were recorded in the first run, which then ran out of credits; the other 22 (beam 2-5, pulse, damage, low, win, death, idle) were recorded in 3.34.0 with the same voice (`eiNgkRczHJmif3gxSoyz`) and model (`eleven_v4`), one take each. A line without an ogg file is still **not registered as a sound event and is never spoken**, and a cue with no recorded line still plays the synthesised cue sound (`LightMoodRules.fallbackSound`, covered by `LightMoodRulesTest`).

| Cue | Lines written | Lines recorded |
|---|---|---|
| greet | 4 | 4 |
| farewell | 3 | 3 |
| rise | 3 | 3 |
| settle | 2 | 2 |
| beam | 5 | 5 |
| pulse | 4 | 4 |
| damage | 4 | 4 |
| low | 3 | 3 |
| win | 3 | 3 |
| death | 2 | 2 |
| idle | 2 | 2 |

To redo a line: generate it (the `prompt` field of the manifest, voice and model above), save it as `<id>.mp3` in one folder, then run

    python3 -B tools/audio/spirit_light_voice.py <that folder>

It converts each take (mono, 32 kHz, silence trimmed, about -16 LUFS, Ogg Vorbis q3), writes `sounds/spirit_light/voice/<id>.ogg`, registers exactly the lines that have a file in `sounds.json` and in the generated `SpiritLightVoiceLines.java`, and updates the report. `--refresh` only re-registers. Total size of the 35 files: about 942 KB (limit about 3 MB).

How the voice is played: the server picks a line with `LightMoodRules.line` (never the same one twice in a row, per-cue cooldown), and sends a `Say` packet only to players within **32 blocks**, **at most one line per 4 seconds per listener**, through the existing `VoiceLimiter`; the client plays it positionally from the seraph in the voice category and honours the voice on/off and volume settings.

## 3. Level of detail and the quad budget (rule 11)

Exact quads (one quad = one drawn face) of the models, counted by the generator (`tools/gen_spirit_light_geo.py`) and checked by `SpiritLightLodTest` and `SpiritLightAssetsTest`:

| Model | Full (LOD 0) | Half (LOD 1) | Low (LOD 2) | Glow `_fx` (drawn at LOD 0 and 1) | Limit |
|---|---|---|---|---|---|
| Cub | 229 | 183 | 84 | 15 | 600 (lod1 280, lod2 120) |
| Seraph | 215 | 155 | 56 | 90 | 1000 (lod1 450, lod2 120) |

Worst case for one spirit is therefore 244 quads (cub) or 305 quads (seraph) in the close view. A beam is 5 quads (3 ribbons plus 2 sprites, 6 allowed), a pulse is 8.

| LOD | Cub distance | Seraph distance |
|---|---|---|
| 0 full | up to 16 blocks | up to 32 |
| 1 half | up to 36 | up to 64 |
| 2 low | up to 64 | up to 112 |
| culled | beyond | beyond |

* One **shared frame budget of 3200 quads** for all spirits, beams and pulses in a frame (`SpiritLightLod.QuadBudget`). A spirit that does not fit drops one level; one that still does not fit is skipped.
* The **owner's own spirit within 8 blocks is never degraded and never skipped** (`ownClose`), so the player always sees their own companion at full quality.
* No allocation per frame (`SpiritLightSourceScanTest.thePerFrameDrawCodeAllocatesNothingAndBuildsNoRenderTypeOfItsOwn`), no render type of its own, motes spawned at most once per tick.

## 4. Worst-case cost

| What | Bound |
|---|---|
| Spirits on the server | 48 form props, plus at most 6 seraph props (the cub hides while the seraph is up) |
| Effect props (beams, pulses) on the server | 24, 3 per player, each lives 6 (beam) or 16 (pulse) ticks |
| Server work per tick | the `LIVE` players, every 4th tick, no entity scan; one scan per beam (after 24 to 30 ticks) and one per pulse (after 400 ticks) |
| Prop sync | the vanilla entity tracker; the follow nudge is at most every 5 ticks per spirit |
| Key packets from a client | one per press, refused if sooner than 6 ticks after the last (`PACKET_GAP`); validated: action 0..3, sender must be a real server player (no fake players) |
| Say packets (spoken lines) to a listener | at most 1 line per 4 s per listener, range 32 blocks, per-cue cooldown 100 to 2400 ticks, and the project's `VoiceLimiter` |
| Sound packets | `PerfBudgets.effectPacket` per tick, per-kind per-player gap |
| Client quads per frame | 3200 shared, plus the owner's close spirit (at most 305 + 8 + 6) |

## 5. How it was checked

* Pure JUnit (run here with the project's shim, see "Not verified"): `SpiritLightRulesTest` (caps, pierce, pulse pick, prop words, swiftness due), `LightMoodRulesTest` (poses per situation, cooldowns, cue and line choice, the cub never speaks, low health), `SpiritLightLodTest` (LOD choice, budget, own-close), `SpiritLightAssetsTest` (geo, animation clip names, textures, lang, previews, this doc), `SpiritLightSoundsDataTest` (sounds.json, ogg files, sizes), `SpiritLightSourceScanTest` (server-safety, config, packet, no hunger or percent damage).
* Guards run: `GeoAssetsTest`, `ConfigFileNamesTest`, `ServerSafetyTest` (the allow list `docs/server_safe_allow.txt` was not needed), `FxSourceScanTest`, `PerfSourceScanTest`, `FlatPropsSourceScanTest`, `SafeAdditiveTextureScanTest` (the scope lists were extended with the new texture names; `scan_textures.py --fix` leaves nothing to fix).
* In-game self-test **`magic.spirit_light`** (`SpiritLightSelfTests`, run by the CI server smoke test, so on a dedicated server with a stand-in player): prop kinds, sound events, model and animation resources, config default, caps, the mood table, every registered line has its file, the link spawns the cub and counts it, Swiftness III is a short effect and only while linked, a second link is refused, the owner's hurt makes the cub flinch, an unknown line is never spoken, the seraph rises and ends, one beam and one pulse spawn within the caps and cooldowns, a second beam in the same tick is refused, and all of it is gone after `forget` (logout, death and dimension change all call it).

## 6. Files

* Server: `spiritlight/` (`SpiritLightRules`, `SpiritLightLod`, `SpiritLightLedger`, `LightMoodRules`, `SpiritLight`, `SpiritLightEvents`, `SpiritLightProps`, `SpiritLightNet`, `SpiritLightSounds`, `SpiritLightVoice`, `SpiritLightVoiceLines` (generated), `SpiritLightConfig`, `SpiritLightBoot`, `SpiritLightSelfTests`), `book/LightSpiritBridge`.
* Client: `client/spiritlight/SpiritLightClient`, `SpiritLightVoiceClient`; `client/prop/SpiritLightPropPainter`, `SpiritLightDraw`.
* Tools: `tools/spirit_light_lib.py`, `gen_spirit_light_art.py`, `gen_spirit_light_anim.py`, `gen_spirit_light_geo.py`, `gen_spirit_light_sounds.py`, `tools/audio/spirit_light_lines.py|json`, `spirit_light_voice.py`, `sl_sounds_json.py`, `tools/render_preview/gen_spirit_light_scenes.py` and the example `examples/SpiritLightProp.java`.
* Shared files touched, each by an appended one-liner only: `prop/PropKind.java`, `prop/PropRegistry.java`, `client/prop/PropPainterRegistry.java`, `NUSMP.java` (two lines), `assets/nusmp/sounds.json` (one block of new `spirit_light.*` entries after `zagred_word`), `tools/safe_additive/scan_textures.py` and `SafeAdditiveTextureScanTest.java` (scope entries).

## 7. Sources and what could not be fetched

* The **Black Clover wiki has no Light spirit**. The nearest canon is Light Magic itself and the four-spirit lore around Licht and Zenon; nothing says what a Spirit of Light looks like. (Not fetched in this session either: the Fandom wiki could not be reached from the build sandbox, so no wiki page was read for this feature.)
* The design therefore follows the owner's brief and **Tensura lore** (elemental spirits as small companions with a great form; the mod already follows that pattern for the other spirits), plus **Fairy Tail** (Loke / Leo the Lion, Regulus: the lion and the light) for the cub, and **Bleach** (Nijigasumi, the rainbow / light weapon) for the prism.
* The seraph's many-eyed wings and gyroscope rings follow the "ophanim" and "seraphim" imagery of the Biblical angel tradition, used as visual vocabulary only.

## 8. Voice licence and the parallel voice path

* Voice: the ElevenLabs library voice **"Cherine Hani - Warm Storyteller"** (`eiNgkRczHJmif3gxSoyz`), model `eleven_v4`. It is a **library voice, not a clone of a real person**. The lines were generated with the owner's ElevenLabs account. **Licence caveat:** ElevenLabs library voices carry their own terms; the owner must check that the plan in use allows the use in a released mod and note the attribution the terms ask for. This is recorded in `tools/audio/spirit_light_lines.json` and the voice report.
* Why a **parallel voice path** and not the existing voice catalogue: `VoiceCatalog` is hard-coded to 120 lines and 4 roles and `VoiceAssetsTest` counts them, so adding lines there would break that test and another builder's data. The Spirit of Light has its own catalogue (`SpiritLightVoiceLines`), its own payload (`SpiritLightNet.Say`) and its own sound events (`spirit_light.voice.<id>`), and **reuses** the existing `VoiceLimiter`, `VoiceConfig` and `VoiceClientConfig` (so the owner's voice volume and on/off settings apply). This is a merge decision for the owner: the two paths can be joined later.

## 9. Previews

`docs/previews/spirit_light/`: `spirit_light_companion.png` (cub, four views), `spirit_light_powered.png` (seraph, four views), `spirit_light_poses.png` (every pose, three frames each), `spirit_light_faces.png` (all eight faces), `spirit_light_in_game.png` (the props placed beside a stand-in player, the beam and the pulse), `spirit_light_lods.png` (the three levels of each model). Scenes: `tools/render_preview/scenes.d/spirit_light_*.json`; render with `python tools/render_preview/preview.py <scene>`. The cub faces sheet uses a preview-only clip `face_sleepy` (the real sleep pose lies down and hides the face), the seraph uses preview-only `face_<mood>` clips; the game never plays them.

Iteration history: more than five look rounds were made and looked at (shape of the mane, tuft cut-outs and outlines, the face orientation, the wing sign, the eyes on the feathers, the ring size, the LOD degradation, the face framing); the main fixes were the floating tuft outlines, the mane hidden behind the head, the seraph face hidden behind the face wings and the cub camera targets.

## Not verified

* **Nothing was run in the game.** No client, no dedicated server, no Gradle build, no CI in this session: the pure tests and the guard tests ran with the project's JUnit shim over the real sources, and the Minecraft-dependent classes were only type-checked against stubs (four known stub artefacts are reported by that check and are not in the code). `magic.spirit_light` and the full build still have to pass in CI.
* The look in the real renderer (lighting, the additive pass, the shaders of other mods) is not seen; the pictures come from the headless rasteriser.
* The sounds were judged by numbers (length, peak, loudness) and not heard.
* The spoken lines: only 13 of 35 exist as audio (see 2.5); the rest are not registered and not spoken. How the recorded voice sounds in the game was not heard here, and the recordings were judged by length and loudness only.
* Packet handling on a real network (`SpiritLightNet.Key` and `Say`) is only covered by source scans and by the self-test calling the server methods directly.
* The wiki side: no Fandom page could be fetched (see section 7).
* Balance: the beam numbers are small on purpose; they were not play-tested against the real Light Magic scaling.

## Update 3.0.0: controls moved to the spirit control plane

The two Light keys (the right and left bracket, with and without sneak) are gone. The Light grimoire's "Summon Spirit / Release Spirit" page (or `/multiverse spirit summon spirit_light`) links and unlinks the cub; the pages "Light Beam", "Glowing Pulse" and "Seraph Form" (or `/multiverse spirit ability beam|pulse|power`) do what the command key and the sneak keys did; "Spirit Combat Mode" lets it cast light beams with its mana and yours. The voice lines still travel to the clients; only the key packet was removed. Full description, numbers and costs: `docs/magic/spirit_control.md`.
