# The Spirit of Space (key `spirit_space`)

Owner request (approved design): a rare spatial spirit that looks like a TEAR IN REALITY. It circles a spatial mage as a starfield orb, swells into a celestial dragon-like wisp when it works, marks waypoints and opens short portals, lifts nearby enemies, and cuts projectiles out of the air in the way of Langris. Later the owner added poses, expressions and creature sounds in place of speech. The Spirit of Space is not the basic Tensura spirit: it has its own models, its own renderer and its own sound set.

Nothing here was seen in a game. Every picture is the project's headless rasteriser (section 10); every sound was synthesised and never heard (section 11).

The whole feature is one **server** config switch: `magic.spiritSpace` in `nusmp-spiritspace-server.toml` (default `true`; `false` and the key does nothing, no entity exists, nothing else in the mod changes).

## 1. NEW / REPLACED / KEPT

| | What |
|---|---|
| NEW | the whole `spiritspace` package (rules, caps, waypoints, landing search, mood rules, sound gate, server config, key packet, events, `/spiritspace` commands, self-test), the two prop kinds `SPIRITSPACE_1` (orb, wisp) and `SPIRITSPACE_2` (portal ring), their painter and draw code, the orb's and the wisp head's geo models (3 levels each and a glow-only head model), the starfield layer textures, 35 synthesised sounds (9 ability events, 7 creature cues), the three keys, the lang fragment, previews, tests |
| REPLACED | nothing. Rule 5 check: the mod already had Spatial Magic (`book/SpatialBook`, the gate and rift props `SPATIAL_1`/`SPATIAL_2`, the canon books of Finral and Langris) but no spirit, no waypoint, no portal travel for a player and no projectile erasing; the Spirit of Space is a new thing next to them |
| KEPT | every Spatial Magic page, prop, effect and canon book; the exclusive `ElementalSpiritLordEntity` and the Great Spirits; the Fire casting spirit (`firecast`); the mob voice register (`ModSounds`) and all older `sounds.json` entries; `SpellRuntime.dissolveBolts(level, at, r, keeper)` (a second overload with a cap was added beside it) |

Existing files touched (append-only one-liners, listed so merges stay trivial): `prop/PropKind` (+`SPIRITSPACE_1`, `SPIRITSPACE_2` at the end), `prop/PropRegistry` (+1 line), `client/prop/PropPainterRegistry` (+1 line), `NUSMP` (+2 lines: the boot and the client keys), `book/SpellRuntime` (+ the capped `dissolveBolts` overload), `sounds.json` (+16 `spirit_space.*` entries, written by `tools/gen_spirit_space_sounds.py`; the other entries are untouched), `tools/safe_additive/scan_textures.py` and `SafeAdditiveTextureScanTest` (+ the two texture scopes of the spirit).

## 2. What it does (the server decides everything, all capped)

Linking: the key `.` (rebindable) links or unlinks the spirit. It needs the **Spatial Magic grimoire** (`book_spatial`) unless `magic.requireSpatialBook` is `false`: a spatial mage is a rare thing in Black Clover, so for anyone else the spirit does not answer. One spirit per player, 48 on the server.

Keys: `;` picks the ability (portal, gravity pulse, spatial cut, mark the waypoint), Sneak + `;` picks the waypoint slot, `/` uses it. The client keeps only those two numbers and shows them on the action bar; the packet is 3 bytes (action, ability, slot) and the server re-checks all of it.

When the spirit works, the powered wisp comes out for 4 seconds (one per player, 6 on the server; cooldown 5 s). If the cap is reached the work still happens with the orb.

### 2.1 Waypoints and portals

* Up to 3 named waypoints per player (config 1 to 6). Mark with the key at your feet (you must stand on the ground) or with `/spiritspace set <slot> [name]`; `/spiritspace name`, `clear`, `list`, `go <slot>` also exist. Names are cleaned (letters, digits, space, `_ - '`, at most 20 characters).
* Saved in the player's persistent data (`nusmp_spirit_space`), so they are per player and survive restarts; they are copied to the new body on death. On every load a malformed waypoint or one whose dimension no longer exists is cleared.
* A trip: the waypoint must be set, in the **same dimension** the player stands in, within the portal range (default 96 blocks, config 16 to 160, a 3D distance). A **1.5 second channel** (30 ticks) starts; moving more than 1.5 blocks or taking damage closes the portal (no cost, no cooldown). When it ends the server re-checks, finds a safe landing (below), draws the mana (6 percent of the maximum, floor 40, through `BalanceLaw.cost` and `ManaLaw`), opens the two rings for 2.5 seconds and moves the player. Cooldown 25 seconds (500 ticks, config 300 to 6000).
* Landing (`SpaceLanding`, pure): only in chunks that are **already loaded** (`getChunkNow`, never a load or a generation); the waypoint's own column, then its 8 neighbours and the 16 at distance 2, 4 blocks up and down; stone under the feet, two cells of room, no lava, fire, cactus, magma, berry bush, campfire, powder snow or cobweb in or under the feet or beside them; never over the void. At most 25 columns x 9 heights x 11 reads = 2475 block reads, once per portal.
* **Not built (decided):** server-wide or cross-dimension travel. It can drop a player into unloaded chunks anywhere (lag and grief risk), so there is no `longRange` switch at all (a test asserts the config has none).
* Rings: one pair per player (the entrance in front of the caster, the exit at the landing), 12 pairs on the server (config up to 64). Nothing else leaves the owner: no passengers, no other players.

### 2.2 Gravity

One scan per cast (radius 8, config 2 to 16; at most 64 entities looked at, at most 6 lifted, config 1 to 12): the nearest hostile mobs (`Enemy`, not allied) get Levitation I. The length is 2 seconds through `BalanceLaw.controlTicks` (it cuts bosses to 1 second, a target hit again within 30 seconds to half and a quarter), and the pulse uses the shared 12 second hard-control lock (`BalanceLaw.beginControl`). Cooldown 15 seconds, mana 4 percent. Players are never lifted.

### 2.3 The spatial cut (Langris)

One scan per cast (radius 10, config 2 to 16; at most 64 looked at, at most 12 deleted, config 1 to 32): the nearest foreign projectiles (arrows, fireballs, tridents, thrown potions, vanilla and mod; never the owner's, an ally's or a fishing hook) are removed, then the mod's own spell bolts (`SpellRuntime` bolts of other casters) for what is left of the cap, through the capped `dissolveBolts` overload. Then **one** target in the look direction (reach 24, one ray) takes bounded damage: raw 10 (config up to 60) times the caster's EP scaling, the spell-damage game rule and the mana law's power, never above 400 after every multiplier, then `BalanceLaw.cap` (the players' hit cap and the mob multiplier). It uses `BalanceLaw.cap` and not the "sliver of max health" of `BalanceLaw.damage`: no percent-of-max-health damage, no instant kill, no hunger. A player is hit only when PvP allows it. **It never removes a block** (a test fails if the package names a block-changing call). Cooldown 10 seconds, mana 5 percent.

## 3. Poses, expressions and voice (owner addition)

Decided by the pure `SpaceMoodRules` (the server only wires it): a situation gives a pose, an expression and a voice cue; each situation has its own cooldown; weak situations (idle, low health, a kill) never interrupt a running pose; nothing interrupts a farewell. The result travels in the prop's param (bits 4 to 7 pose, 9 and 10 expression, 12+ the tick it began), so there is no new packet. **No pose or expression adds a quad**: they change scale, squash, brightness, tint, the orbit, the spine's bend and which of the two existing clips plays.

| Situation | Orb pose | Wisp pose | Expression | Voice | Gap |
|---|---|---|---|---|---|
| link | curious (drifts in toward your face, tilts) | | excited | chirp | |
| portal channel | portal pulse (three pulses, the ring swells) | coil | excited | whisper | |
| portal opens | portal pulse | ascend (rises, nose up) | excited | chime | |
| spatial cut | snap (contracts, springs back) | roar (head rears, jaw wide, mane flares) | angry | none (the tear sound) | |
| gravity | squeeze (squashed, the ring contracts) | roar | excited | murmur | |
| took damage | hurt (flicker, tremble) | | angry | warble (distress) | 40 ticks |
| idle | calm; every third cycle curious | | calm | one idle cycle in three (a cycle is 400 ticks) | 400 ticks |
| owner below 30 percent health | curious | | angry (alarm) | warble | 600 ticks |
| owner killed a hostile mob | flare (swells, the ring flares) | | excited | trill | 120 ticks |
| owner died | farewell (winds into itself, goes out in 24 ticks) | farewell | sleepy | sigh | |
| unlink | farewell | farewell | sleepy | sigh | |
| owner still for one minute | sleepy (dim, low, slow; wakes with the next situation) | | sleepy | sigh | |

Expressions: calm blue-violet (strength 0.9), excited cyan (1.25, ring 1.1), angry red-violet (1.2, ring 0.9), sleepy dim (0.5, ring 0.85). The tint multiplies the starfield shells, the halo and the fins; the dragon's eye glow has its own colour per expression (cyan, white-cyan, red-pink, dim violet). Pictures: `docs/previews/spirit_space/spirit_space_poses.png`, `spirit_space_expressions.png`.

### Voice (no words, 10 to 20 variants)

7 cues, 17 variants: whisper x3 (breathy noise through moving bands), chime x3 (glassy partials, three-note arpeggios), murmur x2 (a soft hum), chirp x3 (two quick rising notes, the curious "bip-bweep"), warble x2 (a falling pitch with a wide fast vibrato, distress), trill x2 (alternating notes, happy), sigh x2 (breath and a falling tone). Played by the server, **only to listeners within 32 blocks, at most one cue per listener per 3 seconds** (`SpaceMoodRules.hears`), after the owner's spirit passed the shared `SoundGate` (a gap of 3 seconds per owner and at most 6 sounds per tick for the whole system). The server sends the one small sound packet to each qualifying listener itself, once per mood change.

The nine ability sounds (hum, ping, unlink, tear, swell, open, gravity, wisp, deny; 2 variants each) go through the same gate and use `ServerLevel.playSound`, which reaches only the players within the sound's range. All 35 files are mono Ogg Vorbis, 0.4 to 2.2 seconds, 424 KB together (budget 2 MB), written by `tools/gen_spirit_space_sounds.py` (numpy, ffmpeg; every variant seeded from its name, so a re-run writes the same audio).

## 4. The look

**Orb** (0.58 blocks across, the ring about 0.9): a UV sphere of dark sky (`core.png`) under three shells of starfield (far, mid, near) that scroll at different speeds (UV offsets in the vertices; the textures repeat) and also slide with the viewer's angle, which is the parallax of the face; a ring of 16 cracked-glass shards with 5 orbiting fragments (geo `spirit_space_orb`, drawn additive, animated: the ring spins, the fragments orbit, the ring flares on `cast`); a violet halo behind.

**Wisp** (3.2 blocks long, 0.42 at the thickest): a tube along an undulating spine (a wave runs head to tail, the pose can coil or lift it) drawn as the dark body plus two starfield shells, cosmic fins along the sides and a crest (24 segments each, translucent emissive), and a small horned head (geo `spirit_space_wisp`: skull, muzzle, jaw, fangs, two three-segment horns with an antler prong, two whiskers of three segments, cheek spines; thin cel linework; a constellation on each cheek) with a glow-only model (`_fx`: eyes, throat, horn tips, whisker beads, mane and cheek fins).

**Portal ring**: two bands and two starfield windows, 120 quads, opens and closes like an iris.

Skins are deterministic scripts: deep indigo / black with violet and cyan star points, pale cel linework, glow only through additive passes that go through `SafeAdditive` (`SafeAdditiveSourceScanTest` passes; the scan covers `textures/entity/spirit_space/` and the orb and fx sheets). The scroll is baked into the vertex UVs, so no custom render type was needed (the owner's text asked for one "through the safe helpers": the helpers are used for every additive and emissive pass).

## 5. Levels of detail and the exact quads

Counted from the generated files by `SpiritSpaceAssetsTest` and from the draw code's own loop counts by `SpiritSpaceLodTest`.

| Form | Level | Distance | Sphere or tube | Layers | Fins | Geo (head or ring) | Glow model | Halo | **Quads** |
|---|---|---|---|---|---|---|---|---|---|
| Orb | full | up to 16 | 8 x 12 = 96 | 4 (base, far, mid, near) | | 105 | | 1 | **490** |
| Orb | half | up to 32 | 6 x 10 = 60 | 3 (base, mid, near) | | 60 | | 1 | **241** |
| Orb | low | up to 64 | 4 x 6 = 24 | 2 (base, near) | | 18 | | | **66** |
| Orb | culled | beyond 64 | | | | | | | 0 |
| Wisp | full | up to 32 | 24 x 8 = 192 | 3 (body, far, mid) | 72 | 115 | 37 | | **800** |
| Wisp | half | up to 64 | 16 x 6 = 96 | 2 (body, mid) | 48 | 91 | 37 | | **368** |
| Wisp | low | up to 112 | 8 x 4 = 32 | 1 | | 39 | | | **71** |
| Wisp | culled | beyond 112 | | | | | | | 0 |
| Portal ring | | up to 64 | | | | | | | **120** |

Distances scale with the effect quality (0.5 to 1). One shared **3200 quad budget per frame** for all spirits and rings together: the nearest are drawn at the level their distance asks for, each is stepped down a level until it fits, then culled (`SpiritSpaceLod.fit`); **the viewer's own spirit within the half-detail band is never degraded** (it is charged to the budget so the others give way). Nothing is allocated per frame (static scratch arrays and tables; a source scan fails on `new float[`, `new Vec3`, `Axis.YP.rotation...` in the draw code), the geo bones are moved by clip, motes go through the client particle budget once per game tick.

## 6. Worst-case cost (rule 11)

Server, defaults:

| Thing | Per player | Server-wide | Lifetime |
|---|---|---|---|
| Spirit orb (prop entity) | 1 | 48 | while linked (it is put back if lost) |
| Wisp (prop entity) | 1 | 6 | 4 s, then 5 s cooldown |
| Portal rings (2 prop entities per pair) | 1 pair | 12 pairs | 2.5 s |

At most 48 + 6 + 24 = 78 prop entities. Per tick the upkeep runs every 4th tick and only for linked players or players with a channel or rings: a handful of entity lookups by id and one distance check. Entity scans: only the gravity pulse (once per 15 s per player) and the cut (once per 10 s), each looking at no more than 64 entities and acting on a capped few. Block reads: only the portal landing (once per 25 s per player, at most 2475 reads, loaded chunks only). Packets: 3 bytes client to server per key press, at most one answered per 8 ticks; props sync as ordinary entities (the orb and the wisp are placed from the owner's body on the client, the server nudges every 5 ticks); a pose change is one entity-data update; sounds: at most 6 per tick for the whole system, a voice cue at most once per 3 seconds per listener within 32 blocks. Messages: at most one per 12 ticks per player.

Client per frame: at most 3200 quads shared, plus the viewer's own spirit (at most 800 more): **at most 4000 quads**.

## 7. Server friendliness (rule 9)

All rules are decided on the server; the common package names no client class (`SpiritSpaceSourceScanTest` and `ServerSafetyTest` pass); per-player state is keyed by the player's UUID (`STATES`, the ledgers, the sound gate, the listener memory); gameplay numbers are SERVER config values clamped to hard caps when read; the packet checks its sender (a real `ServerPlayer`), its action, its ability and its slot; cleanup on logout, death (the farewell plays, then the props end), respawn, dimension change and server stop. Self-test `magic.spirit_space` runs in the CI server smoke test.

## 8. Files

Common: `spiritspace/` (`SpiritSpaceRules`, `SpaceMoodRules`, `SpaceLanding`, `Waypoints`, `SpaceLedger`, `SoundGate`, `SpiritSpaceMesh`, `SpiritSpaceLod` are pure; `SpiritSpace`, `SpiritSpaceProps`, `SpiritSpaceNet`, `SpiritSpaceEvents`, `SpiritSpaceCommands`, `SpiritSpaceConfig`, `SpiritSpaceBoot`, `SpiritSpaceSounds`, `SpiritSpaceVoice`, `WaypointStore`, `LoadedProbe`, `SpiritSpaceSelfTests`). Client: `client/prop/SpiritSpaceDraw`, `client/prop/SpiritSpacePropPainter`, `client/spiritspace/SpiritSpaceClient`. Tools: `tools/gen_spirit_space_art.py` (layer textures), `tools/gen_spirit_space_geo.py` (models, clips, sheets), `tools/spirit_space_lib.py` and `tools/spirit_space_paint.py` (kit and painters), `tools/gen_spirit_space_sounds.py`, preview scenes `tools/render_preview/scenes.d/spirit_space.json` with `examples/SpiritSpaceProp.java`. Lang: `tools/lang/spirit_space.json` (not merged into `en_us.json`; texts are read with `translatableWithFallback`).

## 9. Tests

`SpiritSpaceRulesTest` (caps, range, channel, prop word, packet validation, nearest-projectile selection, damage ceiling, ledgers, sound gate, the landing search on a fake world including lava, void, unloaded and the probe bound), `WaypointsTest`, `SpaceMoodRulesTest` (the pose table, durations, cooldowns, the hearing rule, param bits, the bounds of every pose at every tick, expression colours), `SpiritSpaceLodTest` (exact quads, bands, the shared budget, the owner's exemption, the spine's length), `SpiritSpaceAssetsTest` (geo quads against the table, sheets, clips, layer textures, and the sound data: every event has an entry, every entry its mono Ogg files of 0.3 to 2.4 s, no orphan file or entry, 10 to 20 voice variants, every subtitle in the lang fragment, the older entries kept, the size budget), `SpiritSpaceSourceScanTest` (server-only, scans capped and once per cast, no block change, no hunger or percent damage or instant kill, no cross-dimension code, packet validation, config name and no long-range switch, no allocation in the draw code). The existing guards were run too: `SafeAdditiveTextureScanTest`, `SafeAdditiveSourceScanTest`, `FlatPropsSourceScanTest`, `FxSourceScanTest`, `PerfSourceScanTest`, `ServerSafetyTest`, `ConfigFileNamesTest`, `GeoAssetsTest`, `SoundsJsonTest`. Self-test key: `magic.spirit_space`.

## 10. Previews

`python3 tools/render_preview/preview.py spirit_space_orb spirit_space_wisp ...` (34 scenes in `scenes.d/spirit_space.json`). Five rounds of look-and-fix were done (orb and wisp, the fixes, poses, expressions, the tint pass): the first showed paper-flat pale shards, a hard-edged halo, an eel-thin body, white mane lines and square eye glow; the fixes were dim cyan-edged crystals, a smooth halo, a thicker body with larger undulation and a bigger head, violet fins, almond eyes. Pictures in `docs/previews/spirit_space/`.

## 11. Not verified (honest list)

* **Nothing was run in a game or on a server.** The type check was against API stubs (the only errors are the known "is abstract; cannot be instantiated" artefacts) and the JUnit run was in a shim without Minecraft; the self-test and the real classes (entity spawn, teleport, the sound packet, the Neoforge config) are left to CI.
* **No sound was heard.** The files were synthesised and checked for length, format and peak only. Whether the whispers read as whispers, the chirp as curious and the warble as distress is unknown; they may need tuning by ear.
* The look is judged in the preview only. The head is still boxy, the body reads as a thin serpent rather than a dragon from far, the sphere shows its facets (8 x 12) at its edge, the coil pose is an S-curve more than a coil from the side, and the preview has no vanilla lighting, fog or the player model (the "figure" of the player scenes did not draw). The additive repeat of the starfield textures assumes the default `GL_REPEAT` wrap of mod textures.
* The scroll is baked into UVs, so an unusually old GPU path cannot break it, but the parallax with the viewer's angle was only checked numerically.
* Portal feel is untested: the 1.5 second channel, how the exit ring looks when the player arrives, and whether the player should be hidden for a tick. Other mods' teleport restrictions are not consulted. The teleport call is the plain `teleportTo` of the same level.
* Key defaults (`.`, `;`, `/`) are guesses and may clash; the client cycles only the first 3 waypoint slots (the commands reach all 6).
* Balance (mana 4 to 6 percent, cooldowns 10 to 25 seconds, cut damage 10, levitation 2 seconds) is untested.
* The orb's `HIDDEN`/pose changes are one entity-data update per change; the exact network size was not measured.
* Server-wide and cross-dimension travel are deliberately not built.

## 12. Sources (rule 10)

No wiki source for a Black Clover Space spirit exists as far as this round could tell: in Black Clover, spatial magic (Finral, Langris) is a human trait and the four Great Spirits are wind, fire, water and earth, so a Space Spirit is the owner's invention, a terrifying anomaly by design. The lore line (Space is a high-tier fundamental element grouped with Time, and Space spirits are notoriously rare) is the owner's own wording of the Tensura and Soul Land idea, taken from the owner's brief. **The English Tensura wiki (`tensura.fandom.com/wiki/Space_Magic`) and the Japanese Tensura wiki page for the space element could not be fetched in this session** (`getaddrinfo ENOTFOUND tensura.fandom.com`: no network route to fandom.com), so nothing was checked against them. The design is therefore from the owner's brief and the existing Spatial Magic content of the repo (`SpatialBook`, `SpatialProps`, `CanonBook.FINRAL`, `CanonBook.LANGRIS`, `SpellRuntime.dissolveBolts`).
