# Mana Zone and the Grand Magic Zones, redesigned (round 1.99.0)

The owner's request: Mana Zone was reachable in the early game and broke progression, so make it strictly a LATE-GAME / END-GAME technique, and redesign the
mechanics and the environmental effects of Mana Zone and Grand Magic Zones to match the anime, with rendering, event handling and ticking efficient enough that
an expanding zone never lags. A second message from the owner then replaced the notes this work started from with canon text; this document follows that text.

## What the canon says, and what was used

**Sources.** The wiki pages (blackclover.fandom.com `Mana_Zone` and `Grand_Magic_Zone`) were **not reachable** from the build sandbox (the fetch failed with
`ENOTFOUND`). The design follows (1) the owner's canon text, which is the primary reference and covers everything below, and (2) the author's memory of the
anime for flavour (named in the lists). Nothing here was checked against the live pages.

From the owner's canon text:

* **Mana Zone**: an advanced technique that lets a mage manipulate the natural mana of the surroundings instead of only their own reserve. Three capabilities:
  (1) omnidirectional offence: bigger size, range and number of spells, and attacks launched from any point of the zone (behind an opponent, round them, a
  spell born inside an enemy attack to destroy it from within); (2) heightened defence and senses: much sharper mana detection and reaction time, with
  Reinforcement Magic (Mana Skin) the user predicts incoming attacks very accurately, and can condense the surrounding mana into highly pressurised protective
  spaces for themselves and allies; (3) mid-air mobility: standing, moving and changing direction in the air on the ambient mana as invisible footholds.
  Famous users: Mereoleona, Yami, Yuno. It lets them beat opponents with bigger reserves.
* **Mana Zone vs Mana Method**: Mana Zone is control and domain; Mana Method (Heart Kingdom) shapes ambient mana into runes and magic circles that instruct the
  spell, and can produce **True Magic** (Shin Maho): the real element, fuelled mostly by the environment's dense mana, hugely stronger, devils highly
  susceptible, needs Stage 3 or higher control, easiest for the natural attributes. The Mana Zone must not make true elements.
* **Grand Magic Zone** (Strong Magic Region): **a place, not a caster ability**. Exceptionally dense, unstable, powerful natural mana; works like a dungeon through
  magical pressure; chaotic weather and terrain that defy normal physics; borders between kingdoms are full of them. Named: Seabed Temple (violent swirling
  currents), Witches' Forest (witches, ruled by the Witch Queen), Ultime-Volcano Mountain Trail (magma, used for mana skin training), Gravito Rock Zone (chaotic
  gravity, massive floating boulders, a dungeon on the largest rock), Elysia (hidden forested coastal village, refuge of the elves).

**From the author's memory of the anime (not from the wiki):** the look of the zone as a sphere of faintly lit air with footholds under the feet; Langris
negating spells inside his zone and Mereoleona's speed (which fed the "foreign projectiles are destroyed from within / slowed" and "the caster moves faster"
mechanics together with the owner's text); the zone as the top of the ladder after mana skin and reinforcement.

**Guessed, said honestly:** every number (the gate, the radii, the costs, the effect strengths); where the five named zones stand in the world (placeholders,
see below); the attunement ritual (the mod has no wiki-given trial, so a time-in-dense-mana trial was built from "Ultime-Volcano is used for mana skin
training"); True Magic as a page (see "True Magic").

## Why it was early

* `GrimoireBook.pages()` appended a universal page `mana_zone` to **every** grimoire, and `usable()` ended with `if (instant(mode)) return true;`, which the
  Mana Zone page satisfied (it was an "instant" page), so it was selectable from the first day with no rank, no mastery and no EP condition. `MasteryPages`
  only gates the family pages, never the appended base pages.
* `ManaLaw.toggleZone` charged 5% of the pool, needed 15%, gave 20% cheaper spells and up to 40% more power, slow falling and speed for 30 to 60 seconds. At the
  first grimoire that was a free, strong buff.
* The Wizard King's Crown of Mana called `forceZone` (kept, see below).

## The late-game gate (`mana/zone/ManaZoneGate`, `data/nusmp/mana_zone.json`)

Pure and unit tested. All six must hold (the Wizard King is open by right of the crown):

| Requirement | Shipped number | Where it comes from | Hard floor (no file or config goes under it) |
|---|---|---|---|
| EP (Tensura existence points) | **2,000,000** | `BalanceLaw.epScale` is x1 at 1k EP and tops out at x11 at 10M (the table the whole mod is balanced on); 2M is x9.3, the upper end of the designed range | 1,000,000 |
| Magic Knight rank | **Senior 1st Class (step 14)**, Grand Magic Knight and the Wizard King count | `KnightRank.step`; auto-promotion stops at step 14 at 10 net stars per class, so 140 net stars of mission merit | step 12 (Senior 3rd Class) |
| Mana skin | own grimoire mastery **100%** (Stage 0), **or** Reinforcement Magic at **60%** | `GrimoireBook.masteryFrac`; Mana Skin is the passive every grimoire has, Reinforcement is the mana skin school | none (data) |
| Boss kills | **15** | quest counter `boss.defeated` (`NusmpEvents.count`) | 5 |
| Dungeon depth | deepest floor **40** on either road | `PlayerQuestData.maxFloor` (the hub's shop tiers stop at floor 25) | 10 |
| Attunement (the ritual) | **1,800 s (30 minutes)** inside a Grand Magic Zone region (not a haven: Elysia teaches nothing) with the mana skin held (half the pool) and strong enough to stand it (harm under 0.5) | `ManaZoneGate.attuneGain`; a naturally dense biome (density 1.8, deep sea, Nether, End) counts a quarter, so two hours; fires the quest event `mana_zone.attuned` | 300 s |

A server may set `manaZone.gateStrictness` in `nusmp-mana-zone-server.toml` (0.5 to 4): it multiplies the EP, boss, depth and attunement numbers (the rank is a
ladder and does not move) and can never take them under the floors. A datapack may override the whole file. The numbers were chosen so that, by the mod's own
scales, a player reaches all six only after the long game: millions of EP, 140 net stars, a fully mastered grimoire, fifteen bosses, forty dungeon floors and half
an hour of pressure in the densest mana. Whether that matches a particular server's pace cannot be known without play data (see "Not verified").

**Where it applies:** `GrimoireBook.usable` asks `ManaZoneManager.gateOpenFor` for the Mana Zone page and for the three pages below, before the instant-page rule, so
they are not selectable, not listed (shown as "?") and not castable until the gate is open; `ManaLaw.toggleZone` and the Wizard King's `forceZone` go through the
manager; the Avatar's cast list denies them (a test asserts it); there is no command that opens a zone (`/multiverse mana zone` only reports the checklist; its
admin forms set the attunement and close a zone). The client learns only whether the gate is open (`GatePayload`), for the menu.

**Old saves.** `nusmp_mana.ZoneUntil` and everything else of the 1.5.0 data stays in the save, untouched and no longer read. A player who had the page early keeps
it in their book but pressing it is refused with a clear message (action bar: "The mana of the land is beyond you. The Mana Zone still needs: EP 12,000 of 2,000,000;
..."; the full checklist goes to the chat at most every ten seconds) until the gate is met.

## The mechanics (`mana/zone/ManaZoneManager`, `ManaZoneActions`, `ManaZoneMath`)

A zone is centred on the caster (their chest), grows from the start tick over 60 ticks (ease-out) to a radius cap that scales with EP:
`cap = 14 + 8 x log10(EP / gate EP)`, at most 40 (config `maxRadius`): 14 at the gate, 22 at 20M, 30 at 200M, 38 at 2B. One zone per caster; it ends on death,
dimension change, mounting or dismounting (the brief's "dismount"), logging out, running out of mana, the 4,800-tick maximum, or the caster pressing the page.

| Canon capability | What it does here | Numbers |
|---|---|---|
| Bigger spells | spell power x(1 + 25% ramping in with the growth + up to 10% with local mana density); spells cost x0.8 | exhausted after a zone: x1.25 |
| Bigger range and number | Mana Strike reaches x1.6 (to 38.4 blocks), is wider (1.8 vs 1.2) and pierces 3 more foes | `range_mult`, `extra_targets` |
| Attacks from any point | **Omni Volley** page (6% of the pool, 8 s): for the six foes nearest the crosshair inside the zone, 3 bolts each start behind them and round them, not at the caster | 6 raw damage per bolt through `GrimoireBook.hurt` (the balance law: EP scaling, the 40% player hit cap); a boss takes half |
| A spell born inside an enemy attack | every scan, foreign projectiles inside the zone are destroyed from within (a burst effect at the projectile) | at most 6 per scan, 0.1% of the pool each; a **boss's** projectile is only slowed (x0.55, at most three times), never removed |
| Sharper senses | the caster's **perception** (client): the 24 nearest living things and foreign projectiles in the zone get a glowing mark drawn **through walls** | scan every 5 ticks |
| Reaction time / foresight | blows struck by an entity are foreseen: -15%, rising to -35% with Reinforcement mastery (Mana Skin); capped at -15% against a boss | `predict_*` |
| Pressurised spaces for self and allies | **Pressure Space** page (8%, 20 s cooldown): a 5-block sphere for 8 s, up to two at once; blows to the caster and to allies inside are cut by 45% (22.5% against a boss) | allies are `Foes.isAlly` (same team, pets, summons) |
| Allies in the zone | allies inside take -15% damage | the total of all of it never goes over -70% |
| Mid-air footholds | NeoForge's creative-flight attribute and `mayfly` are given while the zone is open (hover, move and turn in the air; the pad of light under the feet is the visible foothold); taken back at the end with a few seconds of slow falling | the same mechanism as the mod's other flight powers; no hunger |
| Moves faster | movement speed +20% (a transient attribute modifier, removed at the end) | `speed_bonus` |
| Mana density visible | flow streaks run in from the edge, the lattice and the rune rings breathe, motes rise in the colour of the caster's magic | see "VFX" |

**Costs.** Opening costs 5% of the pool (25% needed). Upkeep per second is `0.15% + 0.02% x radius` of the pool (0.43% a second at 14 blocks, 0.95% at 40); if
it cannot be paid the zone collapses. A collapse sets a cooldown of `600 + 20 x radius` ticks (44 to 70 s; half after a deliberate cancel, a quarter if cancelled
within 5 s) and an **exhaustion state**: Slowness I and Weakness I for 100 ticks + 4 ticks per second held (at most 500), doubled if the mana ran out, and spells
cost x1.25 until it passes. **Nothing in the technique touches hunger**: no hunger effect, no exhaustion added, no food drain (a test scans the sources and the
self-test checks the food bar through a whole zone's life).

**Wizard King.** The Crown of Mana still gives a full Mana Zone for 60 s at no cost and with no gate (radius at least 24); it ends like any other.

### Mana Method / True Magic (optional, an interpretation)

An appended page **True Magic** (`true_magic_rune_array`, 12% of the pool, 30 s): for the NATURAL attributes only (fire, water, wind, earth, lightning, plant);
the caster needs Stage 3 control or better (an Arcane or Saint stage counts) and the same late-game gate. A rune array draws itself in the air above the aimed point
(up to 40 blocks); 24 ticks later it fires the real element in a 5-block radius: true fire, a deluge, a downburst, a fall of stone, a bolt from the sky (visual only:
no fire, no transformed mobs), a surge of thorns, each with a short capped control effect through the balance law's control caps. The environment pays most of the
cost: 20% in ordinary mana, 40% at density 2, 60% at 3 (refunded as magicule); the dense mana also adds up to +35% power. Raw damage 40 (Mana Strike is 7), x1.6 to
devils (anything that is a `DaemonMaster`, in the devil state, a devil player, or an entity type whose id says devil, daemon, demon or zagred), x1.3 and x0.6 for a boss.
It is **not** produced by the Mana Zone. How the canon's rune arrays, Stage 3 and "easiest for natural attributes" map to this page is the author's reading.

## The Grand Magic Zones as places (`mana/zone/GrandZones`, `GrandZoneRegion`, `GrandZoneMath`, `data/<ns>/grand_magic_zones/*.json`)

The caster-ability version of the first draft was never written; what the mod already had stays: `ManaLaw.density` / `isGrandZone` (dense biomes: deep sea,
Nether, mountains, deep dark, End, great height) are kept as "dense mana" and now also take the density of a region.

**Schema** (one file per region, id = file name, datapack-overridable): `name`, `dimension`, `shape` (`sphere` with `centre` and `radius`, or `box` with `min` and
`max`), `theme` (seabed, forest, volcano, gravito, haven, wild), `density` (1 to 3), `recommended_ep`, `effects`, `tint`, optional `tp` and `border`.
Effects: `chaotic_gravity`, `floating_boulders`, `violent_currents`, `magma_heat`, `mana_storm`, `witch_glow`, `calm`, `pressure`.

| Zone | Theme | Density | Recommended EP | Effects |
|---|---|---|---|---|
| Seabed Temple | seabed | 2.6 | 1,500,000 | violent currents, pressure, mana storm |
| Witches' Forest | forest | 2.2 | 800,000 | witch-glow (darkness, confusion for the weak), pressure |
| Ultime-Volcano Mountain Trail | volcano | 2.4 | 1,200,000 | magma heat (fire and heat damage to the weak), pressure, mana storm |
| Gravito Rock Zone | gravito | 2.8 | 2,000,000 | chaotic gravity pulses, floating boulders, pressure, mana storm |
| Elysia | haven | 2.0 | 100,000 | calm (nothing harms; the hurt are healed, mana returns, hostile mobs are weakened) |

**Where they stand.** The five are overworld spheres at far-out **placeholder** positions (radii 120 to 280 around (-3600, 2800), (3000, -3400), (-2500, -4200),
(4600, 2600) and (-4500, -1000)); the biome there is not controlled, so the seabed's currents only act where there is water. Move one by editing its file or with a
datapack. `/multiverse zone tp <id>` puts an operator on the surface above a zone's heart (or at its `tp` point).

**How a zone treats a player** (`GrandZoneMath.exposure`): the player's EP against the zone's recommendation. Under it, `harm` = (1 - EP/recommended)^0.75 x a density
factor, up to 1; over it, `boon` = a third at twice the recommendation, full at eight times. A Mana Zone user resists half the harm. A haven never harms and always
feeds a little. Everything fades in over 24 blocks from the border (no cliff). Weak players (harm from 0.3): slowness, then slowness II and fatigue, a magic
"pressure" pulse of at most 6% of the maximum health every 2 s that can never kill (it leaves one point), the zone's own effect on top; strong players: mana
regeneration up to 0.4% of the pool a second and up to +10% spell power. Creative players feel the weather, not the pressure.

| Effect | What happens | Caps |
|---|---|---|
| chaotic gravity | every 3 to 8 s a pulse lifts (low gravity and slow falling), pushes or slams the player and up to ten living things within 12 blocks | speed change at most 0.7 blocks per tick; a strong player rides it (-60%); a Mana Zone halves it; a slam only happens to players being harmed |
| violent currents | things in water within 14 blocks of a player are swept round the zone's centre axis with a wandering surge | at most 0.18 per application, twice a second |
| magma heat | weak players catch fire (1 to 4 s) and take heat damage every 2 s unless fire resistant | at most 8% of the maximum health a pulse |
| mana storm | visual-only lightning near the player about once in seven seconds; a bolt that finds a harmed player takes at most 10% (6 HP) and never kills | no entity damage from the bolt otherwise, nothing burns, nothing persists |
| witch glow | darkness and sometimes nausea for the weak | short, refreshed |
| floating boulders | client only: massive rocks hung in the air, bobbing; drifting debris | none (drawing) |

Nothing edits the world: no block is placed or broken, no fire is lit, the lightning is visual only.

**The dungeon's hook** (the floor generator is not touched): `GrandZones.bind(key, zoneId, dimension, x0, y0, z0, x1, y1, z1)` gives a box the environment of a
zone template, so a floor or room can be a Gravito floor or a Seabed room; `unbind(key)` releases it. Bindings live in memory (a floor binds itself when it loads).
For hand use there is `/multiverse zone bind <key> <id> <from> <to>` and `unbind <key>`. `/multiverse zone list|info [id]|tp <id> [player]` are operator commands.

## Efficiency (the owner's explicit ask)

| Where | Rule | Budget |
|---|---|---|
| Server tracked state | **one `Zone` entry per caster** in a map; one small `Track` per player that is inside a region (none for a player outside every region); no per-entity state | O(open zones) |
| Idle server | `onServerTick` returns on `ZONES.isEmpty()`; `GrandZones.onPlayerTick` returns when nothing is loaded and nobody tracked | one comparison per tick |
| Radius growth | `radiusAt(now - startTick)`, a function of the start tick; nothing changes per tick | 0 state writes |
| Entity queries | zone: one AABB query for **projectiles only** every 4 ticks, then a squared-distance check; regions: staggered by entity id, once per 10 ticks per player, entity queries only for gravity (AABB 12, every 3 to 8 s), currents (AABB 14), haven (AABB 16, every 2 s) | at most 6 projectiles destroyed per scan, 10 entities per gravity pulse |
| Upkeep | once a second per zone | |
| Network | zone state at start, at end and every 100 ticks (about 60 bytes); region state on enter, leave, a twentieth of change and every 100 ticks (about 80 bytes); gate state on change and once a minute | under 1 byte per tick per zone |
| Client vertex data | the shell is an **instanced template**: one unit sphere per level of detail built once at class load, scaled and moved per frame with no trigonometry; ring tables likewise; static scratch vectors, so no allocation in `ManaZoneLayer` per frame | shell 100 quads at full detail (384 vertices + the outline sprite 4), ground at most 392 vertices, bubble 324, scenery 164 to 188 measured; every instance is under the buffer's cap of 400 and the preview checks it |
| Level of detail | three levels by camera distance (12x8, 8x6, 6x4 shell quads; 32, 20, 12 ring segments; fewer motes and streaks), one level less on Decreased particles, nothing drawn beyond 8 radii + 96 blocks, none on Minimal particles | |
| Perception | one AABB query every 5 ticks, a fixed array of 24, insertion by squared distance; one extra draw call of at most 96 vertices | |
| Respawning | the two zone instances (200 ticks each) are re-spawned one tick before they end, with the elapsed ticks, so nothing restarts and nothing doubles; a zone that ended is marked in a ring of 16 serials and its instances draw nothing from the next frame | |

Measured by the preview (vertices): shell 100 (outside), 384 (inside, full detail), ground 244 at the frames shown, opening pulse 68, collapse 96, bubble 324, scenery
164 to 188. An existing cost outside this lane: `VfxVertexBuffer.quad` allocates one small key per quad; the layers' vertex data is cached but the shared buffer is not
(a note for the performance pass).

## VFX (`vfx/client/layer/ManaZoneLayer`, `ManaZoneGeometry`, `tools/gen_mana_zone_textures.py`)

New shapes (appended): `MANA_ZONE_SHELL`, `MANA_ZONE_GROUND`, `MANA_ZONE_BURST`, `MANA_ZONE_BUBBLE`, `GRAND_ZONE_ENV`. Sixteen generated textures (`mana_zone_shell`, `rim`, `ring`, `floor`,
`pad`, `flow`, `mote`, `ping`, `burst`; `grand_zone_boulder`, `spore`, `haze`, `current`, `bolt`, `vein`, `halo`). The Time Magic files were read as the bar and not touched.

* **Shell**: a lattice of ambient mana on a sphere that brightens towards the silhouette (a Fresnel term per quad), breathing, with a round outline sprite from outside.
* **Ground**: the floor disc, a growth front and a slow outward pulse, two rune rings turning against each other, the hexagonal **foothold** under the feet, motes in the
  colour of the caster's magic (flame gold, water blue, wind mint, ...) and streaks of mana running in from the edge.
* **Opening pulse and collapse finisher**; the **pressure space** (a small shell, a floor ring, motes).
* **Grand Magic Zone scenery** round the viewer: veins of light in the ground, the storm's distant bolts with a glow, then by theme: seabed ribbons and bubbles, witch-glow
  spores and pale patches, heat haze and embers, floating boulders (with a seam of mana light) and gravity ripples, a haven's slow motes. A fog that closes in and a
  thin tint of the screen and of the fog colour come from the synced harm and edge fade. Only the client draws any of it.

Preview: `python3 -B tools/vfx_preview/preview.py mana_zone_shell mana_zone_ground mana_zone_open mana_zone_collapse mana_zone_bubble grand_zone_gravito grand_zone_seabed
grand_zone_forest grand_zone_volcano grand_zone_haven` (scenes in `tools/vfx_preview/scenes.d/mana_zone.json`). The preview rasteriser skips any quad with a corner behind
the camera (no near-plane clipping), so the wide ground veins and the long current ribbons show little in it; the game clips them.

## Tests

JUnit (pure, run in the shim here): `ManaZoneGateTest` (12), `ManaZoneMathTest` (10), `GrandZoneTest` (11), `TrueMagicRulesTest` (4), `ManaZoneGeometryTest` (1),
`ManaZoneWiringTest` (parity of the client constants with the server's, the Avatar list, the old entry points, the appended-only shared files, no hunger in the sources,
every text in the lang fragment, this document).
CI self-tests (prefixes for the smoke test's `SMOKE_EXPECT`, which this round does not edit): **`mana.zone_gate`** (closed for a fresh player in every way it can be
asked, the four pages not usable and last in the book, the Avatar list, the floors) and **`mana.zone_core`** (one tracked entry per caster, the radius from the start
tick and capped, a foreign arrow destroyed and a boss's slowed, a clean end with the food bar untouched, the five regions loaded and found, the harm on a weak player, the
dungeon hook bound and released).

## NEW / REPLACED / KEPT

| | Item |
|---|---|
| NEW | `mana/zone/*`: `ManaZoneGate`, `ManaZoneRules`, `ManaZoneMath`, `ManaZoneData`, `ManaZoneManager`, `ManaZoneActions` (Omni Volley, Pressure Space), `TrueMagic` and `TrueMagicRules`, `ManaZonePlayer`, `ManaZoneNet`, `ManaZoneClientGate`, `ManaZoneConfig`, `ManaZoneCommands`, `ManaZoneBoot`, `ManaZoneSelfTests`, `ManaZoneStyle`; `GrandZones`, `GrandZoneRegion`, `GrandZoneMath`, `GrandZoneCommands` |
| NEW | `data/nusmp/mana_zone.json`, `data/nusmp/grand_magic_zones/*.json` (five zones) |
| NEW | `client/manazone/ManaZoneClient` (state, perception through walls, scenery, fog and screen tint); `vfx/client/layer/ManaZoneLayer`, `ManaZoneGeometry`; 5 appended `VfxShape` entries; 16 textures and their generator; preview scenes |
| NEW | pages appended last to every grimoire: Omni Volley, Pressure Space, True Magic; `/multiverse mana zone`, `/multiverse zone list|info|tp|bind|unbind`; config `nusmp-mana-zone-server.toml`; lang fragment `tools/lang/mana_zone.json` |
| REPLACED | the Mana Zone's mechanics: the 30 to 60 s toggle buff (slow falling, speed, 5% to open, "+20% cheaper, up to +40% stronger") became the late-game technique above; its early availability (the `instant(mode)` rule) is gone; the "Grand Magic Zone" announcement for dense biomes no longer claims a named zone ("The mana here is thick."); `ManaCommands info` says "dense mana" |
| REPLACED (reading) | "a Grand Magic Zone is a place of dense mana" was a biome density test; it is now a data-driven region with effects. The test is kept as `density` |
| KEPT | `ManaLaw.toggleZone`, `forceZone`, `zoneActive` (same signatures, now delegating); the 1.5.0 player data (`ZoneUntil`) untouched; `ManaLaw.density`, `isGrandZone`; the page id `mana_zone` and its position in every book; the Wizard King's Crown of Mana; `LuciusCapabilities.onManaZone`; every other page and mode number; the Time Magic VFX untouched |

## Hooks into shared files (append-only one-liners)

`ManaLaw` (delegation, `density` takes a region's, the per-second hook, Mana Strike reach), `GrimoireBook` (three pages appended, `usable` asks the gate first),
`ManaCommands` (the `zone` subtree after the original commands), `MultiverseCommands` (`GrandZoneCommands.build()`), `NUSMP` (`ManaZoneBoot.init`), `VfxManager`
(`register(new ManaZoneLayer())`), `VfxShape` (five entries appended).

## Not verified

Nothing here could be run in Minecraft (no game, no Gradle). Checked: the pure logic by JUnit; every changed Java file against the API stubs (one deliberate wrong call as a
negative control); the layer and its textures through the headless preview (shape, colour, motion, vertex counts). These could not be verified:

* the wiki pages (not reachable) and therefore every canon detail beyond the owner's text;
* that the gate numbers suit a real server's pace (EP, boss kills, depth and stars depend on how the rest of the mod is played); `gateStrictness` exists for tuning;
* the CI self-tests (they need the real server and a stand-in player; assumed: a `FakePlayer` can take attribute modifiers, abilities and effects);
* that creative-flight abilities sync cleanly on every client mod set, and that giving `mayfly` is accepted by other mods' anti-fly checks;
* in game: the shell, rings, perception marks through walls, fog and screen tint (the fog event is cancelled to apply its values, the usual NeoForge way), the bolts, the
  boulders; how the Mana Zone reads next to the Time Magic effects; frame cost on weak machines (the budgets are counted, not measured);
* the five zone positions (placeholders in unknown terrain), whether the seabed has water at its heart, and how the dungeon owner will call `GrandZones.bind`;
* that every foreign magic projectile of the mod and of Tensura is a `Projectile` (spells that are rays or area ticks are not destroyed by the zone, only slowed ones that are);
* `LivingIncomingDamageEvent` ordering against ManasCore and other mods' reductions (the zone multiplies the amount and floors the total cut at -70%).
