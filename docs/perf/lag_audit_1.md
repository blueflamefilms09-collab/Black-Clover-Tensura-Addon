# Lag audit 1: what shipped in 1.111.0 and the 3D Compass, checked against rule 11

Owner request: "also make sure none of these new updates and any following cause lag in a public server." Rule 11 of `CLAUDE.md`: every change must stay cheap with many players online
(server tick, network, client draw). This audit read the new code of 1.111.0 (franchise engravings, the 356 new relics, the armour failsafe, the weapon grip, the datapack enchantments and
their tooltip line) and of the 3D Compass rebuild (`compass3d/**`, `client/compass/**`, `client/prop/CompassPropPainter`), measured it the only way possible here, fixed what was unbounded or
wasteful and pinned the fixes with tests. **Behaviour is unchanged** (add the new, keep the old): same results, same saves, same packets; at close range the compass looks exactly as before.

Nothing can be run here (no game, no profiler, no Gradle). Every figure is a **static cost model**: counts read from the source, quad counts taken from the real mesh builder (a small program
that calls `CompassMesh`), allocations counted by reading. Where something is a guess it is on the list in section 9. The probe sections added by this audit turn the models into
measurements on the owner's machine: `/multiverse perf on`, play, `/multiverse perf report` (server) and `/nusmp perf on|report` (client).

## 1. NEW / REPLACED / KEPT

| | What |
|---|---|
| **NEW** | `compass3d/CompassLod` (pure: levels by distance, behind-the-camera cull, the per-frame quad budget and its cost tables), `client/compass/CompassFrame` (once-per-frame camera and budget roll, one answer per prop per frame), `perf/LagAudit1SelfTests` (CI self-test `perf.lag_audit_1`), `CompassLodTest`, 9 new tests and 4 probe pins in `PerfSourceScanTest` |
| **NEW** | `PerfBudgets.COMPASS_QUADS` (the client frame budget), `CompassRules.GLOBAL_*` / `globalCap` / `shouldSetWord` / `dashStart` / `dashEnd`, `CompassMesh.bodyLayer` / `body(Sink, lod)` / `needle(Sink, lod)` / `glass(Sink, lod)` / `arrow(Sink, lod)` and the cost helpers, `CuriosLink.equippedCached` / `forget` / `clearCaches`, `FranchiseEngravings.clearCaches`, `ArmorVisible.resolve` (plain-argument form), `UnifiedTooltipRules.style(Category, ...)` |
| **NEW** | probe sections `s.franchise.hit`, `s.relic.w101`, `s.relic.w102`, `c.compass.paint` (the existing `s.franchise.tick` is kept) |
| **REPLACED** (same result, cheaper) | the per-hit list of active franchise effects (kept per player now), the per-call cooldown key string, the once-a-second Curios read (shared), the per-slot franchise lookup and tag copy (cached / no copy), the franchise tooltip line (built once), the unified tooltip style (kept per item), the per-pass sinks and per-frame vectors of the compass renderer, the per-dash arrays of the route, the 400-entry "clear everything" of the relic look table (evicts only stale looks), the per-look arrays of `Relics.scan`, the per-ask `Facts` record and per-check location strings of the armour failsafe, the `HashSet` of the Millennium set count, the `Optional` of `Wave20Util.heldId` |
| **ADDED to** (a new rule beside the old behaviour) | the compass painter (level of detail, cull, frame budget), `CompassKit` (server-wide caps, a send throttle for the aim-only change of the compass word, clean-up on log out and server stop), `regenAround` (one scan per pulse, a target cap, no scan with nobody else in the dimension) |
| **KEPT** | every feature, relic, engraving (all 69), spell, enum constant, registered id, data component and packet; the full-detail compass within 18 blocks (the same 1090 quads as before); the Time Magic VFX (not touched); the weapon grip (reviewed, nothing to change) |

## 2. The worst case, before and after

Scenarios: a server with 100 players, all wearing engraved gear and armour relics (server rows); 20 casters of Compass Magic in view of one client (client rows).
"Before" is the 1.111.0 / compass-rebuild code, "after" is this change.

### 2.1 Server

| Area | Worst case before | Worst case after | Why |
|---|---|---|---|
| Franchise, once a second per player | 1 reflective Curios read (+ combined handler, list) and 3 to 5 map or list allocations per player: **100 reads and about 400 allocations per second** | one Curios read shared with the sheathed-weapon pass (**100 reads per second where the two passes made 200**), **0 allocations** for a player with nothing engraved | `CuriosLink.equippedCached` (5-tick age), lazy maps in `readWorn` / `hasHandEngraving`, the worn map keeps its identity while nothing changes |
| Franchise, every hit | `actives()` rebuilt a `LinkedHashMap`, an `ArrayList`, the `Active` records and a key string per `key()` call, up to 3 times per hit: about **15 to 25 allocations per call, up to 2 100 calls per second** with 100 players fighting | **0 allocations** for a bearer whose hands and worn set did not change (the list is kept in the state), 2 component reads for everyone else | kept list keyed on the identity of the three `ItemEnchantments`, key built once in the record |
| Franchise, hits of mobs on mobs | tag test, then a `getEntity` instanceof | one instanceof pair, then out | reorder in `onIncoming` |
| Inventory pass (franchise, weapon and armour engravings), per player per second | 41 slots x 3 passes: a registry key lookup, up to 18 `startsWith` for an unlisted item, and **a full copy of the custom-data tag for every stack that already carries the marker** (916 items are mapped to a franchise: 20 such items = 20 tag copies per second per player, **2 000 per second at 100 players**) | **0 tag copies**, one `ConcurrentHashMap.get` per slot and pass | `FranchiseEngravings.BY_ITEM`, `CustomData.contains(FLAG)`, `ArmorEngravings.FOR_ITEM` |
| Ally aura (Guild Bond, Hearty Party) | up to 2 `getEntitiesOfClass(ServerPlayer)` scans per pulse (one per engraving), every 2 s per wearer, no cap on players healed: at most **100 scans per second** with 100 wearers | at most **1 scan per pulse** (50 per second), no scan when the dimension has one player, at most **16 players** refreshed per pulse | `regenAround(p, r, dur)` called once from `tick` |
| Wave 101 / 102 damage listeners (see every hit of every mob) | a closure per hit, then the checks | two `instanceof` and out for every hit no player is part of; for a player who holds none of the weapons, one map lookup | early exits before `SafeCast.tick` |
| Wave 101 once a second per player | a closure, `HashSet` allocation for the set count, 2 flag look-ups: 100 closures and sets per second | one cached loadout look and out for a player with no working relic | `Wave101Events.onPlayerTick0`, `Wave101Set.count` |
| Relic look table (`Relics.cached`) | cleared **whole** at 400 entries (100 players plus any mob that was asked): every player rescans on the same tick (Curios read, lists), again and again | only stale looks are evicted, cleared whole only above 1 024 | `Relics.evict` |
| The new relics (waves 101 to 105; 111 of them are armour pieces) | no listener, no tick of their own (data: attribute passives, acts built from `Wave31Acts` shapes, run on use under a cooldown). Cost scales with what a player **wears**, not with the catalog | unchanged | verified by reading `Wave103/104/105` (only `defs`, `hooks` (a self-test) and `run`) |
| Compass server work per tick | per compass prop: one `getEntity`, one view vector, **a data-sync packet to every watcher on every change of aim (up to 20 per second per compass)** | aim-only changes are written at most every 3rd tick (**at most 7 per second**); mode and fade changes still at once | `CompassRules.shouldSetWord` |
| Compass props on the whole server | 43 per caster (1 + 24 needles + 10 seals + 6 turn discs + 2 routes) with no server-wide limit | server-wide caps **96 needles, 48 seals, 24 turn discs, 12 routes** (+ one compass per caster); a spell that finds the cap full still works, its model is just missing that time | `CompassKit.spawn` / `globalLive` |
| Compass bookkeeping | per-caster tables only emptied by a self-test: a player who left kept a list of finished props (and the level they were in) for the life of the server | emptied on log out and on server stop | `CompassKit.register` |

Network (packets per second): the needle props are entities that move every tick (`updateInterval(1)`), one position packet per tick per watcher. A volley of 24 needles lives about 1.5 s;
at the new server-wide cap of 96 needles that is at most **1 920 move packets per second to one watcher** for those 1.5 s (before: 24 per caster with no overall limit). The aim word of the
compass dropped from at most 20 to at most 7 data packets per second per compass. Nothing else of this audit changes a packet.

### 2.2 Client

| Area | Worst case before | Worst case after | Why |
|---|---|---|---|
| One whole compass | **1 090 quads** (798 solid: rim 384 + dial 160 + rose 32 + needle 94 + glass 128; 286 glow passes; ~6 for the aura card, soft glow, glint) and **11 render-type switches** (each one an ended batch and a draw call) | 1 090 quads / 11 switches within 18 blocks (unchanged), **320 quads / 7 switches** from 18 to 36 blocks, **20 quads / 3 switches** from 36 to 56 blocks, nothing beyond 56 or behind the camera (distances for effect quality HIGH; x0.75 on MEDIUM, x0.5 on LOW) | `CompassLod`, `CompassMesh` level 1 (a third of the segments, only the dial lines glow, no glass) and level 2 (dial disc + ring card) |
| One arrow-needle | 64-quad model + 64-quad additive twin + streak + glow card = **131 quads** (24 per caster = 3 144) | 131 within 24 blocks, **19** up to 40 (model without tail ring, no twin), **3** up to 64 (streak only), nothing beyond | `CompassMesh.arrow(sink, lod)` |
| One seal disc (a small compass without glass) | **962 quads** (10 per caster = 9 620) | same tables as the compass: 962 / 320 / 20 | `CompassFrame.small` |
| One route of 10 dashes | 222 quads | 222 / 68 / 2 | `CompassLod.routeCost`, every second dash and no twins at level 1 |
| **Twenty casters in view** | the whole compass, 24 needles, 10 seals, 6 turn discs and 2 routes of each: about **21 000 quads per caster, 420 000 per frame** | one shared budget per frame: **4 000 quads on HIGH, 1 400 on MEDIUM, 600 on LOW** (a tenth of the effect quality's vertex budget). What does not fit is drawn one level lower; a compass within 6 blocks of the camera is never dropped (at worst its cheapest level) | `CompassFrame` / `PerfBudgets.COMPASS_QUADS` |
| Allocation per compass per frame | 3 to 4 sinks, ~6 `Vec3` for the needle goal and the route, a `float[2]` per dash (up to 14 per route) | **0** (two reused sinks, plain doubles, `CompassRules.dashStart/dashEnd`) | pinned by `PerfSourceScanTest` |
| Compass walks over the mesh | each of the 3 body layers walked all 576 body quads and dropped two thirds | each layer walks only its own quads (`CompassMesh.bodyLayer`) | |
| Armour failsafe, per armour relic per wearer per frame | a `Facts` record, `ArmorSlotPlan.of` verdict, map look-ups; and every ten seconds **about 250 `ResourceLocation`s built from strings** by `RelicTextures.verify` | no `Facts` record (plain-argument `resolve`), the locations are built once at registration | `ArmorVisible.resolve(plan, int, boolean...)`, `OWN_LAYERS` map |
| Tooltip line, per frame while a stack is hovered | franchise line: a linear table search and two components; unified tooltip: the class chain walked, names joined and lower-cased, an item stack made, three strings built, twice per frame | franchise line and unified style **kept** (one entry per item at most, rebuilt when the language changes) | `FranchiseEngravings.LINES`, `UnifiedTooltipItems.Kept` |

Client memory: the armour layers add 3 dynamic textures (64x32) per armour relic, 333 for the 111 new ones, about 8 KB each = about **2.7 MB** of VRAM and as much RAM for the new ones (this audit
did not change it; the registration runs once per resource load).

## 3. Per area: what was found, what changed

### 3.1 Franchise engravings (`franchise/**`, `item/*Engravings`, `item/CuriosLink`)
* **Found.** (a) `actives()` rebuilt its map and list on every hit, `Active.key()` concatenated a string on every call. (b) the once-a-second `readWorn` and `hasHandEngraving` allocated maps for
  players with nothing engraved, and `CuriosLink.equipped` (reflective, builds a combined handler) was called three times a second per player by three passes. (c) the inventory pass asked,
  for every stack, a registry key, up to 18 prefix tests and **copied the custom-data tag** to read one boolean (franchise, weapon and armour passes). (d) the ally aura could scan twice per
  pulse and healed an unbounded number of players. (e) the tooltip line searched the franchise table and built two components per frame.
* **Fixed.** Per-player kept list (identity of the three enchantment components), key built once, lazy maps, `equippedCached(5)` shared by the franchise and sheathed-weapon passes,
  `FranchiseEngravings.BY_ITEM`, `CustomData.contains`, `ArmorEngravings.FOR_ITEM`, one aura scan per pulse with a 16-target cap and an early exit for a lone player, kept tooltip lines.
  The reflection stays cached as before (`getInventory` / `getEquipped` are looked up once; a failure pauses reads for 10 s).
* **Already fine.** The handlers leave at once when no player is part of a hit; per-player state is keyed by UUID and dropped on logout and server stop; every proc has a cooldown; `Fx.send` goes through
  the particle budgets; the mana trickle runs every 5 s per engraving.

### 3.2 The 356 new relics (`relics/wave/Wave101..105`)
* **Found.** Waves 103, 104, 105 (75 + 120 + 121 relics) have **no handler of their own**; the passives are attribute modifiers and resistances applied by the existing engine, the acts are shapes of
  `Wave31Acts` run on use under cooldowns with clamped radii (`Fx.MAX_RADIUS`, `MAX_FOES`). Waves 101 and 102 add three listeners: a damage listener each (they see every hit of every mob) and one
  player tick (once a second, staggered by entity id). `Relics.locatedList` / `loadout` are cached per entity for `relicScanInterval` ticks (10 by default; verified in `Relics.cached`).
  The Ring's needles scan 24 blocks once a second, but only for its wearer (the Millennium Items are one of a kind through the Order's ledger, as the code comments say; not verified here).
* **Fixed.** Early exits before any closure, a `FLAGS` table instead of `"w101_" + id` per swing, no `HashSet` or `Optional` per question, nothing for players without a working relic,
  the relic look table no longer thrashes at 400 entries, `Relics.scan` makes no arrays.
* **Left as is, bounded.** `RelicLoot.candidates` walks the catalog (now about 1 450 defs) once per loot roll (a chest, a mimic, a drop): not per tick.

### 3.3 Armour failsafe and worn rendering (`client/relics/**`)
* **Found.** The failsafe runs per armour relic per wearer per frame (it is what makes an armour relic never invisible). Its extra cost over 1.110.0 was a record per ask and, in `RelicTextures.verify`
  (every 10 s), a location built from strings for every own layer. `ArmorTrace`, `WornTrace` and `WornLibDraw`'s record are small mutable records, no allocation after the first frame, bounded maps (cleared at 256 / 512 entries).
* **Fixed.** Plain-argument `ArmorVisible.resolve`, cached layer locations. The deep pixel check (`hasVisiblePixel`, early exit at the first visible pixel) happens once per registration, not every 10 s.
* **Residual (not changed here).** `RelicWornLayer.render0` still allocates a plan and entry lists per wearer per frame (older code); the cost grows with the wearers in view, so a crowd of
  players in relic armour is the part of the client most worth measuring (`c.relic.worn_layer`).

### 3.4 Weapon grip (`visual/grip`, `client/WeaponGrip`, `WeaponRenderer`, `VuRenderer`)
* **Found, nothing to fix.** The grip is worked out once per loaded mesh (`WeaponGrip.resolve` / `forVu`, on load) and the per-context shift is kept in `GripState.cache` (four arrays), so a frame costs
  `WeaponGrip.index` (a switch) and one array read. `displays` is cached per id, the table is read once per reload. The first-person view-limit bisection (24 steps) runs once per mesh and context.
  A source pin now keeps it that way (`theWeaponGripCostsOneArrayReadPerFrame`).

### 3.5 The 3D Compass (`compass3d/**`, `client/compass/**`, `client/prop/CompassPropPainter`)
* **Found.** The mesh is baked once (good), but every compass drew all of it every frame: 1 090 quads and 11 buffer switches at any distance, including behind the camera (`MagicPropRenderer.shouldRender`
  is always true for props), and each of the 10 seal discs of a caster is a whole second compass. 20 casters with their volleys were about 420 000 quads per frame. Server side: no server-wide cap, a
  data-sync packet per change of aim, tables never emptied on logout.
* **Fixed.** Levels of detail (full / a third of the segments / dial disc and ring card / nothing), the cull behind the camera, a per-frame quad budget shared by compasses, arrow-needles, seal discs,
  turn discs and routes (nearest-first is not possible because the entity renderer draws in its own order, so what does not fit is drawn one level lower; a compass within 6 blocks always draws),
  one answer per prop per frame (a shader pack's second pass costs the budget once), a fallback to distance-only levels if the frame event is not seen for half a second, server-wide caps, the aim
  throttle, clean-up on logout. A true display-list or vertex-buffer cache is **not** possible on this renderer path (`MultiBufferSource` batches are rebuilt every frame in 1.21.1); the levels of detail
  and the budget are what bound the cost instead.
* **Left as is.** `CompassBook.willfulCompass` scans `Projectile` entities in a 7-block box every tick for its 10 s (one query per tick per active caster, cooldown-gated, older than the rebuild).

### 3.6 The 69 datapack enchantments and the tooltip line
* The 69 `nusmp:fr_*` enchantments are data only (`"effects": {}`, no components): **no runtime cost**. They add 69 small entries to the enchantment registry that is synced once at login. The cost
  is in the code that reads them (3.1). The tooltip line is now built once per franchise; the unified tooltip's style once per item and rarity.

## 4. Tests and self-test

| Check | What it pins |
|---|---|
| CI self-test **`perf.lag_audit_1`** (pure, runs on the dedicated server) | compass quads fall with the level, levels only get coarser with distance, a crowd of 40 compasses never passes the frame cap on any quality, a compass beside the camera is never dropped, the server-wide caps are above the per-player ones, the aim throttle decides right, the franchise item cache answers exactly what `FranchiseMap` answers for up to 400 real items and hands out the same tooltip line again, plus two micro timings (reported in the result, ceiling 20 us per call) |
| `CompassLodTest` (JUnit, 9 tests) | the bands and levels per quality, the behind-the-camera test, the lower meshes are sane quads and a fraction of the full one, level 0 is the old model, the budget over 20 casters, the keep radius, the roll on a new frame, route dashes, caps and throttle, `dashStart/dashEnd` equal the old `dash` |
| `PerfSourceScanTest` (9 new tests) | no unthrottled entity scan in a tick or frame method of `franchise/`, `compass3d/`, `client/compass/`, `Wave101..105`; no tag copy, no key concatenation, kept lists, shared Curios read, one aura scan; early exits before closures in the wave listeners; the relic table evicts and allocates no arrays; no record or string in the armour failsafe; the grip cache; the compass painter asks for a level and allocates no vectors, the renderer and frame helper allocate nothing; the server caps, throttle and clean-up; probe sections present |

Run here: `CompassLodTest`, `CompassMeshTest`, `CompassRulesTest`, `CompassAssetsTest`, `PerfSourceScanTest` (29), `FranchiseDataTest`, `FranchiseRulesTest`, `PerfBudgetTest`, `VfxQualityTest`,
`PerfMathTest`, `ArmorVisibleTest`, `Wave101RulesTest`, `ServerSafetyTest` all pass in the JUnit shim; the changed files type-check against the API stubs (the only error is the known stub artefact
"MobEffectInstance is abstract"); a deliberate wrong call is rejected (negative control), and a deliberate `new float[3]` in `CompassRender.compass` makes the scan test fail (negative control).

## 5. Top risks found (most serious first)

1. **Compass draw cost on the client** (about 420 000 quads per frame with 20 casters before). Fixed by levels, cull and the frame budget.
2. **Seal discs are whole compasses**, ten per caster (962 quads each): the single biggest term of the above. Same fix.
3. **Per-second inventory passes copying the custom-data tag** of every engraved stack, three passes, 41 slots, every player. Fixed.
4. **The relic look table cleared whole at 400 entries**: on a server with 100+ players every player rescans on the same tick, repeatedly. Fixed.
5. **Moving needle props with no server-wide cap**: one position packet per tick per watcher per needle. Capped at 96 on the server; the remaining worst case (1 920 packets per second to a watcher for
   about 1.5 s) is not reducible without changing the prop entity.

Also found and fixed, smaller: per-hit allocation of the franchise active list, an unbounded ally aura, aim-word packets, props kept after logout. Found and left: Willful Compass's per-tick projectile
scan, the plan allocations of `RelicWornLayer.render0` (3.3), the full-catalog walk per loot roll.

## 6. How to measure it on a live server

* Server: `/multiverse perf on`, play for a few minutes with many players, `/multiverse perf report`. The sections of this audit: `s.franchise.tick`, `s.franchise.hit`, `s.relic.w101`, `s.relic.w102`,
  `s.curios.passives`, `s.engravings.weapon`, `s.engravings.armor`, `s.relic.scan`; budgets: `effect-packets`.
* Client: `/nusmp perf on`, look at a crowd, `/nusmp perf report`. `c.compass.paint` is the compass painter, `c.relic.worn_layer` the worn layer; the budget `compass-quads` shows asked / granted / dropped.
* Settings that already follow the audit: `vfxQuality` (high / medium / low) sets the compass distances (x1, x0.75, x0.5) and the frame cap (4 000 / 1 400 / 600 quads); `relicScanInterval` the look lifetime;
  `entityScanInterval` the scans that follow the config.

## 7. Rule 9 (server-friendly) check

All new work on the server is keyed by player UUID or by the prop, decided on the server and sent as before; `CompassFrame`, `CompassRender` and `CompassPropPainter` are client-only and are not
touched from common code (`ServerSafetyTest` passes, the allow-list is unchanged); the compass settings that change looks stay a CLIENT config; nothing new is a packet, so no new validation is
needed; the new server caps are constants in the pure `CompassRules` (a SERVER config knob for them is a possible follow-up).

## 8. Files

New: `compass3d/CompassLod.java`, `client/compass/CompassFrame.java`, `perf/LagAudit1SelfTests.java`, `src/test/.../compass3d/CompassLodTest.java`, this document.
Changed (additions and replacements as in section 1): `franchise/FranchiseEvents`, `franchise/FranchiseEngravings`, `item/CuriosLink`, `item/WeaponEngravings` and `item/ArmorEngravings` (one line each: the
marker test), `relics/Relics`, `relics/wave/Wave101Events`, `Wave101Set`, `Wave102Events`, `Wave20Util`, `compass3d/CompassKit`, `CompassMesh`, `CompassRules`, `client/compass/CompassRender`,
`client/prop/CompassPropPainter`, `client/relics/RelicTextures`, `RelicWornLayer`, `worn/ArmorVisible`, `tooltip/UnifiedTooltipItems`, `UnifiedTooltipRules`, `prop/CompassProps` (one line),
`perf/PerfBudgets` (one budget), `perf/PerfSelfTests` (one line), `PerfSourceScanTest` (9 tests and 4 probe pins).

## 9. What could not be verified

* **Nothing was run in a game and no profiler was used**; all costs are counted from the source and from the real mesh builder, not timed. The real gain of an allocation fix (a few thousand small objects
  per second) may be small next to the quad and packet terms; the quad and tag-copy terms are the ones that matter.
* The **look** of the levels (18 / 36 / 56 blocks, the ring card standing for the rim at level 2) was not seen; the distances are a judgement and are constants in `CompassLod`.
* `RenderLevelStageEvent.Stage.AFTER_SKY` is assumed to fire once per frame before entities are drawn (the mod's other effects rely on the same event family); with a renderer replacement that does not send it,
  the budget switches itself off after half a second (distance levels remain) and the first frames use distance only.
* `CustomData.contains(String)` and `Camera.getLookVector()` were checked against the API stubs only; the full Gradle build was not run.
* The kept franchise list assumes `ItemEnchantments` is replaced, not changed in place, when an item is enchanted (true for vanilla data components); if a mod mutated it in place the list would be stale until the
  next once-a-second refresh of the worn set or a change of hand item.
* The one-of-a-kind guarantee of the Millennium Items is taken from the code comments (the Order's ledger), not tested.
* The needle packet figure (1 920 per second per watcher at the cap) is a count, not a capture; the real number depends on tracking range and how many casters really volley together.
* `UnifiedTooltipRulesTest`, the Wave 103 / 104 / 105 data tests and the in-game self-tests (`perf.lag_audit_1`, `item.franchise_engravings`, `magic.compass_3d`) need Minecraft classes or a running server and were
  not run here; the CI smoke test runs them.
* Memory of the new caches (one entry per item, one per franchise, a table of at most 512 Curios reads and 1 024 relic looks) is bounded by construction, not measured.
