# Boss overhaul, Charter Grimoires, boss bar, worn renderers, ranged relics and the Djinn update (1.25.0)

Written without a compiler; nothing was run in game. Each section is the package's own design note.

---

# Boss overhaul, part 1: Lucius configuration and the Mod Boss Global Buff Engine

Package `com.newuniverse.nusmp.boss`. Everything here applies only to this mod's own bosses (namespace `nusmp` AND a row in
`BossRegistry`): dante, vanica, zenon, lucius, devil_avatar, goliath, charybdis_spawn. The God of Order, the Gatekeeper, Dark
Disciples, Blood Beasts, clones and every vanilla or base-Tensura entity are never touched (guard test: `BossRegistryTest`).

## What was built

| Part | Class | Kind |
|---|---|---|
| Boss list and isolation | `BossRegistry` (pure), `BossEntities` (registry glue) | new |
| Buff numbers per boss, plan/diff (idempotence) | `BossBuffTable` (pure) | new |
| Engine: join/leave/tick, staggered refresh, suspension, aura | `BossBuffEngine`, `BossHooks`, `BossBuffConfig` | new |
| Boss skills (lend/take back, never from players) | `BossSkills` | new |
| Engravings on the boss's blows (Barrier Piercing, Soul Eater) | `BossEngravings` | new |
| Lucius attributes, nullifications, passives, blow riders | `LuciusProfile`, `LuciusNumbers`, `LuciusMitigation`, `LuciusKit` | new |
| Usurper (non-destructive), Dead End Rainbow | `UsurperPolicy`, `DeadEndRainbow` (pure) | new |
| Three cast spells as telegraphed moves | `LuciusSpellTable`, `LuciusSpells`, `SpellMath` | new |

Edits in existing files (additive): `ZogratisBoss` gets a `mitigate(DamageSource, float)` hook (identity by default) called in `hurt`
only when the boss is not exposed and before `PowerCurve.clampDamage`. `LuciusBoss` overrides `mitigate`, `canBeAffected` and
`strike`, iterates `LuciusSpellTable.allSpecs()` for its move list (the original table, its tests and the move indices are
unchanged; the three spells are appended), delegates the `default` branch of `runOf`/`tellOf` to `LuciusSpells`, and clears its
scratch state with the fight.

## The engine (all bosses)

* Join: an entity joining a server level is tracked only if `BossEntities.bossId` names it a boss. First tick: effects and skills
  applied. Then a refresh every 100 ticks per boss, staggered by `(game time + entity id) % 100`. No world scan: the loop is over the
  few live bosses. The one per-tick read is the boss's exposure flag.
* Effects (infinite, ambient, no particles, no icon): `tensura:magic_barrier`, `physical_barrier`, `haki_coat`,
  `instant_regeneration`. Skills lent to the boss itself: `tensura:starved`, `mathematician`, `mortal_fear`.
* Suspension: while the boss is exposed (state RECOVERING after an ultimate), transforming at the Gates, or Lucius is under Julius's
  will, the four effects are removed; they come back the moment it recovers. The skills stay.
* Idempotent: it reads what is on the boss and changes only the difference (`BossBuffTable.missing`/`surplus`); applying twice changes nothing.
* Config (`NUConfig` edit, see the report): `bosses.globalBuffs` (default true; false removes the effects and the lent theme
  skills) and `bosses.buffMultiplier` (0..3, default 1.0; level = trunc(base level x multiplier), 0 drops the effect; also scales Lucius's damage cuts).
* Anti-magic aura (the equivalent of Tensura's Anti-Magic Area for a mob, which cannot be spawned for a mob safely): during a fight,
  every refresh, players within the radius may have an Ultimate skill pushed onto a 2 to 3 second cooldown
  (`Nullification.interfere`: cooldown only, never a removal; users of Anti-Magic are exempt; none while the boss is exposed).
* Engravings: Barrier Piercing and Soul Eater are Tensura enchantments of weapons, which a mob does not carry, so the boss's blows
  carry them: a blow on a player shaves `pierceTicks` off the player's finite Tensura barrier effects and takes `soulPoints` of the
  player's spiritual health (floor 1, regenerates), at most once a second per player.

### Numbers table (levels are 1-based; amplifier = level - 1)

| Boss | Tier | Magic barrier | Physical barrier | Haki coat | Instant regen | Skills | Aura (blocks / jam s / chance) | Pierce ticks | Soul pts |
|---|---|---|---|---|---|---|---|---|---|
| goliath | 1 | - | 1 | - | - | mortal_fear | 8 / 2 / 0.30 | 0 | 0 |
| charybdis_spawn | 1 | 1 | - | - | - | starved | 8 / 2 / 0.30 | 0 | 0 |
| dante | 2 | 1 | 1 | 1 | - | starved, mortal_fear | 10 / 2 / 0.35 | 20 | 1 |
| vanica | 2 | 1 | 1 | 1 | - | starved, mathematician | 10 / 2 / 0.35 | 20 | 1 |
| zenon | 2 | 1 | 1 | 1 | - | mathematician, mortal_fear | 10 / 2 / 0.35 | 20 | 1 |
| devil_avatar | 3 | 2 | 2 | 1 | 1 | starved, mortal_fear | 12 / 3 / 0.40 | 30 | 2 |
| lucius | 4 | 2 | 2 | 2 | 1 | starved, mathematician, mortal_fear | 14 / 3 / 0.45 | 40 | 3 |

Max level the engine ever hands out: 4 (multiplier 3.0). The effect levels are tuned by feel: what each Tensura effect does per
level could not be read here, so they are small, suspended while the boss is exposed and tunable by the multiplier.

## Lucius

Tensura attributes, resolved by registry id at run time, ADD_VALUE modifiers with stable ids, applied once on the first tick and
re-checked every refresh (a modifier already there with the same amount is left alone), skipped silently if the entity lacks the attribute:

| Attribute | Modifier | Why |
|---|---|---|
| `tensura:max_magicule` | +50,000,000 (id `nusmp:lucius_max_magicule`) | "maximized": Tensura clamps to its energy config's maximum, the pool is filled once |
| `tensura:max_aura` | +50,000,000 (`nusmp:lucius_max_aura`) | same |
| `tensura:spiritual_health_regeneration` | +400 (`nusmp:lucius_spiritual_regen`) | extreme (base 2): spirit damage does not last on him |
| `tensura:dodge_invulnerability` | +0 to +8, never past 20 ticks in total (`nusmp:lucius_dodge_invulnerability`) | capped: a dodge never gives more than one second |

Side effect to know about: the balance law scales a boss's hit by its max EP (x1 at 1k EP up to x12). Lucius already had 2M magicules
(x9.3); maxed pools put him at the x12 cap, about +29% before the per-hit cap. The framework's pvp cap (40% of a player's max health
per hit) is unchanged and still binds.

### Immunities, as fair damage cuts

The four nullification skills were not learned as skills: in Tensura they make a body untouchable by their kind and cannot be lifted
for an exposed window, which would leave the boss unkillable. Equivalent behaviour:

* `magic_nullification` -> 45% of every magic hit removed (damage types containing magic, elemental, holy, spirit, darkness, light, lightning, soul, ...).
* `physical_attack_nullification` -> 30% of every hit that has an attacker and is not magic or spatial.
* `spatial_attack_nullification` -> 60% of space/spatial/dimension hits.
* `abnormal_condition_nullification` -> a harmful effect does not land (`canBeAffected`).
* Never cut below 25% of the hit, never more than 75%; the multiplier scales the cut (0 = off).
* Not applied while the boss is exposed, transforming or under Julius's will, not against anti-magic blows (the designed counter),
  not against the Zogratis family, not to damage that bypasses invulnerability. `ZogratisBoss.hurt` runs the cut before `PowerCurve.clampDamage`,
  so the power lines and the gates stay authoritative, and the exposed bonus (x1.25) is untouched.
* Killability check (test): 3600 health against 60-damage hits through the hardest cut class still falls in 150 blows.

### Skills and attack modifiers

* `tensura:absolute_severance`: learned for real (inert for a mob). Effect `tensura:severance_blade`: infinite ambient on him.
* `tensura:anti_skill` and `tensura:tuner` are not learned (the real ones give total skill/magic immunity and fatal-blow survival).
  Their equivalents: anti-skill = the magic cut plus the engine's aura (a short cooldown on a player's Ultimate skills); tuner =
  Instant Regeneration (engine) plus his existing precognition charges.
* Safe Usurper (`UsurperPolicy`): when a blow lands on a player (at most once per 30 s, never while a copy is lent), he lends himself a copy
  of one Tensura skill the player has (resistance, common, extra or unique only; never ultimate, intrinsic, nullifications, the Usurper
  itself, anti-skill, tuner, or one of this mod's grimoire skills) for 20 s and drains 3% of the player's max magicules (they
  regenerate). The player's skill list is never written to; `BossSkills.forget` refuses players and non-bosses. The copy is recorded on the boss and taken back by the refresh.
* Intangibility: his physical blows (not magic) scale their base by the victim's armour so that 60% of it is ignored, at most x2, and
  then go through `ZogratisDamage` like every blow (power tuning, balance law, pvp cap).
* Dead End Rainbow: every landed blow on a player stacks one mark (marks at least 10 ticks apart, so a flurry from one move is one mark,
  window 12 s). The seventh mark rings the player in rainbow circles for 16 ticks, then drops the player's spiritual health to its floor
  (never to 0 and never hit points: this repo's `EnergyBridge.spirit` keeps 1) and adds 5 s of Weakness. The victim is safe from the next one for 30 s.

### Megiddo, True Darkness, Dimension Cutter

Tensura's real spells cannot be capped by the boss damage law, so they are three new moves, appended after the 20 of the table (indices 20 to 22):

| Move | Power | Wind-up | Cooldown | Telegraph | Effect |
|---|---|---|---|---|---|
| Megiddo | 40% | 40 | 520 | a ring under each of up to 6 foes, locked at the start | sun pillars fall on the rings 4 ticks apart; 9 base damage, nobody takes more than 2 |
| Dimension Cutter | 40% | 30 | 300 | the lane drawn with two edge lines, locked at the start | cuts the lane once for 14, and 0.6x a third of a second later |
| True Darkness | 60% | 48 | 780 | a dark ring growing to 18 blocks | every foe in the ring in his line of sight: 3 s blindness, 10 spiritual health, 4 base damage |

All hits go through `strike`, so they are capped by the framework like every other move. None changes blocks.

## Review addendum (R2)

* Tensura's Instant Regeneration effect has a death-stopping hook (`InstantRegenerationEffect.canStopDeath`). The engine therefore takes
  Instant Regeneration off a boss at or under 50% health and puts it back only above 60% (`BossBuffTable.lowHealth`, hysteresis), on top of the
  exposed/transforming/Julius suspension of all four effects. The barriers, the coat and the skills are unchanged.
* The engine walks a copy of its tracked list, and drops a boss whose join another handler vetoed (checked on the refresh tick).
* The engravings skip creative and spectator players.

---

# Charter Grimoires and the first-kill drop lock (package m2, `com.newuniverse.nusmp.charter`)

Seven world-exclusive books, one per boss of this mod: Dante, Vanica, Zenon, Lucius Zogratis, the Devil Avatar, Goliath and the Charybdis Spawn.
Each can be won once per world, by the first player whose kill rolls it; while held it lends that boss's Tensura skills on loan and takes nothing.

## The three laws, where they live
1. **Isolation.** `CharterTable.byEntity(namespace, path)` only matches the registry namespace `nusmp`. Vanilla and base Tensura entities (even with a boss's path) are never bosses. Dark Disciples, clones and Blood Beasts are not bosses either.
2. **Non-destructive.** The only way skills are touched is `MealSkills.grant(player, id, 60, "Charter Grimoire")`, the existing lease system: a skill the player owns is left alone, a lent one is only extended, the lease is written before the skill is, it expires by itself about three seconds after the book is put away, and it survives relog and death without ever becoming permanent. `MirrorPolicy.plan(owned, wanted)` is the pure planner and can only return additions; `tensura:usurper` is on the never-lent list.
3. **Immutable lock.** `ModWorldDropData` (SavedData `nusmp_charter_drops`, Overworld storage) keeps `has_dropped_charter_grimoire_<bossId>` booleans (all seven keys always present) plus who/when. The class has no public clear; the single door is the package-private `adminClear`, reached only by `/multiverse charter reset` (op level 4), which logs a warning.

## The drop (`CharterEvents.onDeath`, game bus, `LivingDeathEvent` at LOWEST)
1. A boss of namespace `nusmp` dies (a death another mod cancelled never reaches the handler).
2. The lock is read; if set, nothing else happens.
3. The killer: the player who dealt the killing blow (a pet or summon counts for its owner), else the last player to hurt the boss within 30 s. A killer in creative or spectator gets nothing and nobody takes their place. No player at all: no drop, lock untouched.
4. The roll (`DropRoll`): base chance 25% for the four Zogratis, 40% for the three floor bosses, times the config `charter.dropChance`, capped at 100%. A miss leaves the lock untouched; the next kill rolls again.
5. On a hit the lock is set in the same call that checks it (all on the server thread: two kills in one tick, first wins), the book is given to the killer (a full inventory: an indestructible, never-despawning item at their feet), the server announces `[Server] <name> is the first to claim the Charter Grimoire of <boss>.`, and every client is synced. The book is a separate item, so bosses that clear their drops do not matter.

## Sync and tooltips
`nusmp:charter_sync` (server to client, optional channel): the whole claimed set (boss id, name, day), at login and on every change. The client keeps it in the static `CharterClaims`; tooltips show `Unclaimed` or `Claimed by <name> on day N`, the grade, the lore and the lent skills.

## Mirroring
Once a second per player (staggered by entity id): if a Charter Grimoire is in the main hand, the off hand or any Curios slot (it is meant for the `charm` slot), the union of the held books' skills is planned and leased for 60 ticks. Several books merge. Skills the player owns for good are never touched. Chat shows the lend line of the lease system once per skill when it first applies.

| Grimoire | Grade | Lent skills (8 or 9 each, cap 12) |
|---|---|---|
| Dante | Legend | gravity domination, gravity manipulation, gravity field, gravity flight, gravity attack resistance, burden, oppressor, strengthen body |
| Vanica | Legend | blood mist, curse, curse bind, darkness, shadow bind, shadow motion, drain, self regeneration |
| Zenon | Legend | spatial manipulation, spatial domination, spatial motion, spatial storage, spatial attack resistance, dimension cutter, instant move, danger sense |
| Lucius | God | anti skill, tuner, absolute severance, megiddo, true darkness, dimension cutter, thought acceleration, spiritual attack resistance, holy attack resistance |
| Devil Avatar | God | hellfire, black flame, flame manipulation, flame domination, flame attack resistance, darkness attack resistance, demon lord haki, summon daemon |
| Goliath | Legend | earth manipulation, earth domination, earth wall, earth spikes, earthshatter kick, giantification, physical attack resistance, steel strength, strengthen body |
| Charybdis Spawn | Legend | water manipulation, water domination, water current control, water jail, water blade, water breathing, water attack resistance, hydraulic propulsion |

Every id is in the Tensura 2.0.1.3 id reference (a unit test checks the whole table against it). Usurper is not lent (law 2).

## Commands
`/multiverse charter status` (everyone), `give <player> <bossId>` (op 2, does not touch the lock), `reset <bossId>` (op 4, the escape hatch for a lost book; logged).

## Config (new file `nusmp-charter-server.toml`)
`charter.enabled` (true), `charter.dropChance` (multiplier, 1.0), `charter.mirror` (true).

## Files
Java in `charter/`: `CharterTable`, `DropRoll`, `KillerPick`, `DropLedger`, `MirrorPolicy` (pure); `ModWorldDropData`, `CharterConfig`, `CharterGrimoireItem`, `CharterItems`, `CharterClaims`, `CharterNet`, `CharterEvents`, `CharterMirror`, `CharterCommand`. Art by `tools/gen_charter_assets.py` (reads the cover colours from `CharterTable.java`): per boss a 32x32 texture, a 3D book model (nine plain model elements: back cover, pages, front cover, spine, two gold bands, four corners, a raised sigil plate, a ribbon) with a distinct cover colour and sigil, and a 16x16 icon plus `_icon` model. Tests: `src/test/java/.../charter/` (27 tests).

## Not verified in game
Nothing ran in the game. The hand and GUI display transforms are the stock generated-item values with a tilted GUI view (judged only from a software preview); the packet, the SavedData round trip, the Curios charm placement and the chat spam of the lease system need a look in a real run.

## Review addendum (R2)

* The book item is built before the lock is set, and the world data is written to disk at once after a claim (`DimensionDataStorage.save()`), so a crash cannot leave a book in an inventory with the lock unset.
* `DropLedger` is synchronized (exactly one of any number of racing claims wins) and `all()` returns a snapshot copy.
* Curios: add the seven `nusmp:charter_grimoire_*` ids to `data/curios/tags/item/charm.json` (and to `tools/relics/gen_curios_tags.py` extras if that script rewrites the file) so they can be worn in the charm slot.

---

## The animated boss bar (MAS phase 5)

The mod's bosses (Dante, Vanica, Zenon, Lucius, the Devil Avatar, Goliath, Charybdis Spawn, the God of Order: everything that extends `ZogratisBoss`) get their own animated boss bar. Every other boss bar in the game (vanilla bosses, other mods, the mod's Zagred / Monarch / Juggernaut bars) is still drawn, and drawn exactly like vanilla: same sprites, same 182x5 size, the name 9 px above, 19 px between bars, stopping at a third of the screen height.

**What the animated bar looks like.** An ornate metal frame (shield-shaped end caps with a gem) around a 182x7 window. The fill is shaded and tinted from the boss's primary colour toward its secondary one; two layers of flames / aura scroll across it in opposite directions (additive); a bright shine travels along the fill; the leading edge of the fill glows; two glow lanes in the frame's top and bottom rim scroll in opposite directions; five phase gems sit under the bar and light up one per phase (0 at 20% power ... 4 once the Gates of the Tree of Qliphoth are open). A boss in rage (the Gates open, or a Triad rage step) burns faster. Lucius is gold and turns deep purple as the Gates open (a little at 60% and 80%, all the way at the Gates); the floor bosses and the God of Order have their own palettes, every other boss uses its devil's colours.

**Reactive motion.**
- Losing more than 4% of the bar within 10 ticks shakes it for 12 ticks (a decaying sinusoid, never more than 4 px), flashes the fill white for the first 4 ticks and displaces up to three horizontal slices of the fill by a few pixels (new ones every tick).
- A phase change blends the colours over 40 ticks and shatters the bar into 12 vertical strips that fly apart, fall and reassemble over 30 ticks.

**Options** (client, `config/nusmp-bossbar-client.toml`, section `bossbar`): `animations` (default true; false = vanilla draws every boss bar and the mod cancels nothing), `shake` (default true; false = no shake / glitch / flash / shatter, the rest stays), `percent` (default true; the health percentage at the right end of the name line).

**How it works.**
- Server (`boss/bar`): `ZogratisBoss.refreshBar()` calls `BossBarSync.update(this, bar)`, which works out the style (tints from `BossBarThemes`, phase from `PowerCurve.stage(power)`, rage flag) and sends `nusmp:boss_bar_style` {bar uuid, boss id, primary, secondary, phase, rage} to the players who can see the vanilla bar. A per-tick check tells late joiners. `ZogratisBoss.remove` calls `BossBarSync.remove(bar)`, which sends `nusmp:boss_bar_clear`. Any other boss with a `ServerBossEvent` can opt in with `BossBarSync.update(entity, bar, bossId, primary, secondary, phase, rage)` and `BossBarSync.remove(bar)`.
- Client (`client/gui/bossbar`): `ModBossBarOverlay` listens to `RenderGuiLayerEvent.Pre` of vanilla's boss layer, reads the client's bar list from `BossHealthOverlay.events` by reflection (found once, cached; there is no access transformer), draws every bar and cancels the vanilla layer. If the list cannot be read the layer is left alone and one warning is logged; if drawing ever throws, the mod stops drawing and vanilla takes over again. `BossBarAnim` is the pure state machine behind the shake, glitch, morph and shatter (unit tested). At most two bars shatter in the same frame.
- Textures: `assets/nusmp/textures/gui/bossbar/` (frame, bg, fill, flame, shine, glow, pips, edge), generated by `tools/gen_bossbar_textures.py` (original pixel art, neutral grey, tinted in game).

**What the health text shows.** The percentage only (the client does not know the boss's health points, only the bar's progress).

**Not covered by the animated bar** (they keep the vanilla look): Julius's own white bar during Lucius's fight, the Zagred / Monarch / Underworld / Juggernaut bars. Each can be added with the two calls above.

---

## Boss relics and their worn models (MAS phase 1 curio attachment, phase 2 worn renderers)

Three new worn relics drawn from the Zogratis fights, each with a bone-tracked worn model. All three are the Order's unique relics (one live copy per world, issued through `UniqueLedger`), appear in the relic loot pool and the creative tab, and can be taken from Dante and Zenon.

| Relic | Slot / grade | While worn and attuned | Abilities |
|---|---|---|---|
| Dante's Gravity Crown (`dante_gravity_crown`) | HEAD, LEGEND, Djinn vessel | gravity attribute -30%, jump strength +20%, safe fall +4, half fall damage, Tensura Gravity Attack Resistance (lent) | Weight of the Crown (3% magicules, 20 s): a gravity well for 4 s, foes within 8 blocks dragged in, slowed (I to III), sunk, crushed once as it closes (Tensura Gravity Field lent); Lift (4%, 30 s): Tensura Gravity Flight lent 7 s plus slow fall and jump boost |
| Zenon's Spatial Ring (`zenon_spatial_ring`) | HANDS, LEGEND, Djinn vessel | Tensura Spatial Attack Resistance (lent) | Rift Step (2%, 5 s): safe blink 10 blocks (then 6, then 3), Spatial Motion lent; Twin Gates (5%, 30 s): two linked gates for 15 s (2.5 blocks ahead and up to 12 blocks away), you and your pets walk through either, 1.5 s re-entry delay, one pair per player, Warp Portal lent |
| Artifact Mimic (`artifact_mimic`) | BELT, UNIQUE | +1 luck | Bite (3%, 8 s): jaws open for 0.6 s (the telegraph), then snap and lunge at the nearest foe in front within 4 blocks, 6 damage (capped for players), slow, 1 HP back; Hoard (2 s): the 27-slot pocket on the belt (`Wave6Pockets`) |

Fairness: every ability has a cooldown and a magicule cost; nothing kills instantly; damage goes through `Fx.damage` (30% of a player's health at most); creative and spectator players are never targets; the well and the gates are bounded (24 foes, 4 travellers per gate per check) and cleaned up on death, logout and server stop.

Drops: Dante can drop the crown and Zenon the ring when a player kills them (`WaveBoss.dropChance`, default 8%); issued through the Order's ledger, so nothing drops while a copy lives.

New stats: none. Everything runs on Tensura magicules (the costs), Tensura skills lent through the lease (`gravity_field`, `gravity_flight`, `gravity_attack_resistance`, `spatial_motion`, `warp_portal`, `spatial_attack_resistance`, all verified in the id reference) and vanilla attributes.

### Worn rendering
`client/relics/worn`: `WornRenderers.register(relicId, WornRenderer)`, `interface WornRenderer { void render(WornCtx c); }`, `record WornCtx(stack, pose, buffers, light, model, player, partialTick, ageInTicks, netHeadYaw, headPitch, firstPerson)`. `RelicWornLayer` calls the registered renderer of a worn relic instead of its generic palette model (HEAD included), after the generic ones, with the pose stack restored and the renderer switched off if it ever throws. This is the Curios `ICurioRenderer` of the MAS prompt: Curios is only read reflectively here, so the same behaviour lives in the layer and the registry.
- Crown: follows `model.head` (pivot, yaw, pitch, crouch), a gold square band with eight spikes and a brow gem, six floating shards orbiting at 2.4 degrees a tick that give back 85% of the head's yaw and 60% of its pitch (they hang and lag), a tilted lens ring; solid, `entityTranslucentEmissive` and additive passes. On Weight of the Crown the shards are pulled in, glow, burst out and settle.
- Ring: follows `model.rightArm` (swing, attack offset, sneak), slim or default arms, silver band outside the sleeve, an emissive rift slit and two gyroscopic additive rings with three fragments; flares on Rift Step and Twin Gates.
- Mimic: a GeoLite model (28 cubes) drawn from `model.body`; idle clip loops on the wearer's age (tongue wobble, breathing, blink); bite and yawn clips play from a cue stamp.
- The bite state comes from a tiny S2C packet (`WaveBossNet.Cue`: entity id, relic, kind) to everyone tracking the player; `WornCues` keeps the start tick per player. Chosen over replicated persistent data because it needs no registered attachment and costs nothing when idle; a late joiner sees the idle belt.
- First person: nothing is drawn (the layer is not even called for the local hand; a third-person camera draws normally).

Asset pipeline spec (bone hierarchies, pivots, keyframe states, Blender export): `docs/asset_pipeline.md`, section "Curio attachment".

---

## Bows, crossbows, oversized weapons and state layers (MAS phases 1 and 2, package m5 "itemfx")

### What a player sees

- **Relic bows and crossbows** draw, load and shoot exactly like vanilla's (a bow's full draw is 20 ticks; a crossbow charges for its vanilla duration, Quick Charge included; arrows and bolts are found and used up as usual, Infinity and creative work, a loaded crossbow keeps its charge and fireworks; the weapon never breaks). In the hand they are 3D: the limbs flex and the string draws back through three stages as you pull, and a loaded crossbow shows its bolt on the rail, to everyone who sees it. A loaded crossbow is held at the ready in third person.
- **The relic's ability** goes on **sneak + use** (the first ability) and on the Use Worn Relic key (any ability), because the use button means "draw". A relic bow that needs attunement cannot be drawn until it is yours: sneak and hold use for five seconds, as for every relic. A crouching archer whose ability is still cooling down simply draws. The tooltip says so.
- **The two crossbows the mod already had** (Bolt Thrower, Crossbow of Swift Death) load themselves, so they do not draw: while ready their glow pulses faster and brighter, and for the moment after a shot (their cooldown) the bolt is gone, the limbs are relaxed and the string hangs straight.
- **Long weapons no longer go through the camera in first person**: every weapon longer than 1.4 blocks (35 at the time of writing; the greatswords, polearms, staves, the big hammers) is drawn smaller in first person with its grip where an ordinary sword's grip is, so its tip stays in view. In third person (and on the ground, in frames) it is as large as before.
- **Genesis and five-leaf glow**: a Genesis-grade relic, or any weapon held by a player whose body is Genesis, glows 1.6 times faster and wider and gets an extra soft emissive pass (or its own `_genesis` mesh if it has one). A Black Clover weapon held by a player who owns a five-leaf (or higher) grimoire glows a violet-black (or uses its `_5leaf` mesh).

### New vs replacement (CLAUDE.md rule 5)

NEW: `RelicBowItem`, `RelicCrossbowItem`, the draw/load mesh states, `RangedItemProperties`, `ItemStateVariants` and the Genesis / five-leaf layers, `RelicHolderSync`, `OversizeGuard`, `tools/relics/ranged_meshes.py`, `tools/relics/crossbow_states.py`, the new mesh files `bolt_thrower_charged`, `bolt_thrower_spent`, `crossbow_of_swift_death_charged`, `crossbow_of_swift_death_spent`, docs/asset_pipeline.md (m5 sections).
REPLACEMENT (owner request "rebuild weapons", display only): the `firstperson_righthand` / `firstperson_lefthand` display blocks of every model json of a weapon longer than 1.4 blocks (listed in `tools/relics/display_presets_manifest.json`); everything else in those jsons is untouched. The old mesh files of the two crossbows are kept unchanged.

### Fairness and safety

Nothing here touches stats, damage, drops or the world: the item classes inherit vanilla's shooting, and the act path goes through the same `RelicEngine.useHeld` as every relic (cost, cooldown, attunement). The holder-flag packet is read only (a boolean and a small integer per player, sent when they change). Variants and states are client-side decisions per draw; no stack or entity is changed and only mod items reach the renderer.

New stats: none (nothing runs on Tensura stats; the Genesis flag is read, not written).

### For the wave authors

`RelicDef.bow()` / `.crossbow()` on a MAIN_HAND def (`RelicDef.RangedKind` also exists). Ship the rest mesh `<id>.txt` plus any of `<id>_pull1`, `_pull2`, `_pull3` (and `_charged` for a crossbow); a stage you do not ship falls back to the nearest lower one. `tools/relics/ranged_meshes.py` makes them from a finished `mesh_lib` mesh whose parts are named `limb*`, `string*`, `arrow*`, `bolt*`. After adding or rebuilding any long weapon run `python tools/relics/display_presets.py --apply`.

### Changelog lines

- Relic bows and crossbows (`.bow()` / `.crossbow()`): vanilla draw and load timing, 3D draw stages, sneak + use for the ability.
- Oversized weapons are drawn smaller in first person so they no longer clip the camera (display presets, manifest, render-time guard).
- Genesis and five-leaf weapon glow layers; loaded relic crossbows are held at the ready.
- Bolt Thrower and Crossbow of Swift Death: ready and just-fired states.

---

## Footwear relics (FEET slot) - 1.25.0

Relics can now be boots, sandals or greaves. `RelicSlot.FEET` ("Feet") is real armour (vanilla boots: `ArmorItem.Type.BOOTS`, right-click puts them on, never breaks), not a Curios slot. Everything the other relics have works on it: grade, attunement (counts toward the 3 attuned relics), passives, acts, flags, resistances, the Djinn vessel, the unique ledger.

- Armour value: a boots def without an explicit `armor(...)` gets 40% of the chest value for its grade (same share as a helmet, at least 1); `armor(defence, toughness)` overrides it.
- Look: `Look.BOOTS` (default for FEET), `Look.SANDALS`, `Look.GREAVES` (appended at the end of the enum). Set it with `.look(Look.SANDALS)`. LEATHER and the cloth looks give soft boots.
- Worn layer: painted at run time from palette (main, trim, glow) + look on the leg box of the armour layer_1 (64x32, leg box at (0,16): sides 4 wide x 12 tall, sole at (8,16)). Vanilla draws boots with layer_1 and both legs, so only the lower rows of the four sides and the sole are painted; the rest is clear. Boots: turned cuff, lacing, toe cap, stone on the outer side. Sandals: ankle strap, criss-cross instep straps, clasp stone, bare shin. Greaves: knee guard with gem, shin plate with ridge and rivets, ankle band, plated toe. A resource pack PNG at `textures/models/armor/relic_<id>_layer_1.png` still wins. No leggings texture or geometry is needed.
- Scanning: `Relics.scan` (hence `Relics.equipped/worn/loadout`, the engine tick and the uniqueness scan) reads the player's FEET armour slot like HEAD and CHEST.
- Curios tags: FEET has no Curios slot; `tools/relics/gen_curios_tags.py` skips it.
- Code rule: never write an exhaustive `switch` over `RelicSlot` or `Look` without a `default` (the repo's switches have one).

Example: `RelicDef.of("boots_of_example", "Boots of Example", Grade.RARE, RelicSlot.FEET).look(Look.GREAVES).palette(0x6A7A9A, 0xD8B040, 0x80E0FF).passive(Passive.effect("minecraft:speed", 0))`.

---

# The Dark Djinn (relics/djinn/dark, server side)

The owner asked for "a Dark Djinn version like the show". In Magi a Dark Djinn is a Djinn whose bond has been fed black rukh (Al-Thamen): the equip turns black and gold, the wings go black, the power is greater and the wielder's nature is eaten by it. This page is the server rules and the items; the black-and-gold look is the client work (client/relics/djinn) and reads one sync bit (section 7).

## 1. New or replacement (CLAUDE.md rule 5)
Everything here is NEW: no dark state, no black-rukh item and no second Djinn state existed. It is layered on the Djinn system through small hooks; a Djinn that is never fed black rukh plays exactly as before (every function takes the plain value and a dark flag and returns the plain value when the flag is off). Nothing was removed.

## 2. Becoming dark (a choice with a price)
* **The drink.** The owner (never a household member) sneaks and uses a Dark Rukh Shard with the bound vessel of their own Djinn in the other hand. Requirements: trust 50 or more, no phase out (dismiss the Djinn first), not inside a running Dungeon collapse, no household members (a dark Djinn cannot be lent: `unshare` them first). One shard is consumed, "The Djinn drinks the black rukh." Refusals say why in the action bar and spend nothing.
* **The unforced fall.** In a Full Equip at 90% assimilation or more, a plain Djinn rolls once a second (10% per minute in total) to darken by itself, but ONLY if the player carries a Dark Rukh Shard in the pack, which is eaten. Never random without a shard; never in a collapse; never twice.
* **The record.** `DjinnState.dark()` / `setDark(boolean)`, a persistent boolean under `"dark"` in the Djinn record. It survives death (the attachment copies on death; dying never releases the Djinn), is removed by `clearBond()` (release, an admin reset, the ledger taking the Djinn back) and by `bind()`/`lend()`, and is never read as true for a household member (`DarkDjinn.active(state)` = dark and owns).

## 3. Cleansing
Holy Water (the Wave 19 relic `holy_water`): sneak and use it with the vessel of your dark Djinn in the other hand: the water is poured, not thrown (the hurl does not happen, one flask is spent), the flag goes, the Maso numbers return at once. Also `/multiverse djinn dark off [player]`, and letting the Djinn go (`release`). With no dark Djinn, or without sneaking, Holy Water does what it always did.

## 4. What the black rukh changes (all numbers in DarkRules, tested)
| Where | Plain | Dark |
|---|---|---|
| Half and Full Equip rider (extra element damage on a blow) | 25% of the blow, cap 6 (the trait's own) | x1.35 of the share, cap x1.5 (6 becomes 9). The 6-point cap on a PLAYER target is unchanged |
| Maso speed | +30% | +40% (+10% more) |
| Maso attack | +5 | +7 |
| Maso element boost (Tensura) | +30% | +50% (Strength: +3 attack becomes +5; Life: +6 health becomes +10) |
| Extreme Magic raw damage | the Djinn's | x1.25, still through the balance law, then Fx.damageHeavy (45% player cap, unchanged) |
| Extreme Magic Magoi price | 60% | 48% (the bar must still be 60% full to begin; a cancelled cast gives back what was paid) |
| Magoi upkeep per second | base | x1.3 |
| Assimilation gain (and the Extreme's extra 2.0) | base | x2 |
| Trust | rises to 100 | cannot RISE above 60 (it may fall; a trust above 60 kept from before the drink is neither raised nor taken). The Extreme Magic's gate of 70 counts 60 as 70 for a dark Djinn |
| Maso backlash severity | by the end | +0.15 when the Maso ends by the bearer's will, a timeout or a burn-out (never for death, logout, an admin or a lost vessel); even a clean early exit leaves a mild mark |
| Elemental calm (the 40-100% cut of the Djinn's own element) | full | halved |
| Skills lent while dark (Tensura, on top of the Djinn's set) | none | Half: `tensura:darkness_attack_resistance`; Full: also `tensura:shadow_motion` and `tensura:mortal_fear` |
| Unease (Full Equip only) | none | every second, within 6 blocks: non-hostile mobs (not the owner's pets) get `tensura:fear` for 3 s (at most 16); allied players (team) get a 2 s ambient Darkness (at most 8). Hostile things are foes, other players, creative and spectator players are untouched. Nothing damages anyone |
| Households | up to 2 | none: `share` and a pending `accept` are refused while dark |

Nothing deletes a skill, an item or the bond. No new pool or meter exists: each changed stat stays on what it already ran on.

## 5. Stats and what Tensura thing each runs on
| Stat | Runs on |
|---|---|
| Magoi upkeep, Extreme price | Tensura magicules (and Aura as the second pool), as before: only the factor changes |
| Trust (capped at 60) | the persistent `ABILITY_MASTERY_GAIN` modifier `nusmp:djinn_trust`, as before: the cap is applied when it is written |
| Assimilation (x2) | the persistent `ABILITY_LEARNING_GAIN` modifier `nusmp:djinn_assimilation` plus the transient element resistance, as before |
| Maso speed/attack/element | the same transient attribute modifiers (ids `djinn_maso_*`), values from DarkRules |
| Dark skills | `MealSkills.grant` leases through `Fx.lendSkills` (5 s, renewed each second), ids from the Tensura id reference |
| Unease | the `tensura:fear` effect, and a vanilla Darkness effect for allies |
| The dark flag itself | a plain saved boolean in the Djinn record (a state, not a stat or a pool) |

## 6. Where shards come from (DjinnSources, DarkDjinn)
* The Blood-Red Commander (Monarch's Vault) and the Devil Avatar (Underworld Gate): on every kill (heard through the same `DungeonCollapseEvent` as the Seal), 15%, to the killer (a drop where it fell if nobody is credited). Apart from the Seal logic, rolled even when the Seal has already been paid.
* Lucius: the same 15% on his death (`LivingDeathEvent`, lowest priority, skipped if the death is cancelled; his fall posts no collapse event).
* A vault chest: `DjinnSources.vaultSeal` returns a Seal at 3% as before; when it returns nothing, a further 2% for a shard in the same slot (no change at the call site).
* The chance is a constant (`DarkRules.BOSS_CHANCE`, `VAULT_CHANCE`); no config entry was added.
* Admin: `/give @s nusmp:dark_rukh_shard`, or the creative tab once the lead adds the entry.

## 7. How the client reads it (the contract with the visuals)
`DjinnNet.Sync.flags` gets bit 3, value 8 (`DjinnNet.FLAG_DARK`); the existing bits are 1 lent, 2 marked, 4 vessel missing (set by the client). The packet layout is unchanged. The server sets bit 8 in the sync of the owner and of every watcher (`DjinnEngine.flags`) whenever the player's Djinn is dark; a household member never carries it. It is sent on every change (the signature includes the flags), on login, respawn and dimension change, and to a player who starts tracking a dark player even if no phase is out. The client reads `(flags & 8) != 0`.

## 8. Commands
`/multiverse djinn dark <on|off> [player]` (ops, no price; refused for a player who owns no Djinn, and `on` is refused while a household exists). `/multiverse djinn status [player]` shows `DARK (the black rukh)`.

## 9. Not done / design notes
* No second Dark-only Extreme Magic, no new shapes: the Extreme keeps its own spell, harder and cheaper.
* The Dark Djinn of the older design note (permanent max-health cost, uncapped assimilation, a sacrifice mechanic) is NOT what was built: the price here is the faster assimilation, the trust cap, the heavier backlash, the upkeep and the unease, all reversible with Holy Water.
* The shard drop chances are not configurable in the file config yet.

---

# Djinn visuals: a look of their own for each Djinn, and the Dark Djinn

Client side only (client/relics/djinn, tools/relics, assets). The server rules of the Dark Djinn are another package; the one contract is the synced `DjinnNet.Sync.flags` bit 3 (value 8) = dark, read by `DjinnClientState.Snap.dark()`.

## New or replacement (CLAUDE.md rule 5)
* The look of each Half Equip and Full Equip: **REPLACEMENT of the look** (a unique set per Djinn is what the game draws by default), but the old shared rig (`djinn/<id>/*.txt`, `<id>_armor*.png`, `<id>_rune.png`, `<id>_bust.png`, `gen_djinn_art.py`, `djinn_parts.py`) is **kept** and is the fallback when a Djinn has no set of its own.
* The Dark Djinn variant (sets, skins, eye crest, thorn halo, rukh feathers, veins, aura, particles, bust, first-person arm, HUD tint): **NEW**.
* New pieces appended to `DjinnMeshes.Piece`: WEAPON, EYE, BACKDROP, MANTLE. The 16x16 Seal icons are untouched.

## How a Djinn is drawn
`DjinnMeshes.setOf(djinn, dark)` picks ONE set: dark -> `d_<id>`, else (and as the dark fallback) `u_<id>`, else the shared rig `<id>`; the first whose `chest.txt` exists (`DjinnLook.pickSet`, tested). A piece a set omits on purpose (Baal has no halo, Sitri no tail) is never filled in from an older set. Each mesh carries the set it came from, so it is drawn with that set's skin. Pieces: helm, horns, ears, crest (the Half Equip brow crest), eye (dark only), chest, belt, mantle, both pauldrons, vambraces, gauntlets, greaves, wings (two segments), tail (own number of segments), halo, backdrop (a piece behind the back), weapon (Half Equip, in the hand), and the old sheath only when the Djinn has no unique set.

Half Equip = main-hand pauldron, vambrace, gauntlet, the brow crest, the Dark Djinn's eye, the weapon (solid when the hand is empty, translucent over the item when it holds one), ambient particles at 35%. Full Equip = everything.

## The twelve looks (measured on the references; see the constants in the Python modules)
| Djinn | Helm / horns | Wings | Weapon (Half Equip) | Back piece | Tail | Halo | Mantle | Particles |
|---|---|---|---|---|---|---|---|---|
| Baal | lightning diadem of three bolts, swept hair blades, crescent horns | storm-raven blade feathers with lightning cores | thunder lance (leaf head, two lightning prongs) | the blood ring (R 12, spikes and bolts, turns) | whip, barbed bolt tip | none | none | sparks off the feathers |
| Amon | circlet with a crown of seven flame tongues, ram horns | flame tongues (glow only) | sun greatsword (50 px, flames along both edges) | arch of flame over the shoulders | flame | ring of flame | white cape and sash | embers rising |
| Vinea | fan-fin tiara with a pearl, fish-fin ears, coral antlers | three water ribbons with droplets and a fan fin | trident staff, pearl in a coral cradle | arch of pearl bubbles with wave crests | fish tail with a fluke | ring of droplets | long water scarves | bubbles and drops |
| Focalor | wind plume of four blades, temple feathers | three huge scimitar blades, a rank of short blades | gale glaive with a crescent, ribbons and wind rings | two crossing cyclone rings, blades riding them | ribbon streamers | ring with four swirl blades | the coiling serpent scarf | gusts and white feathers |
| Zagan | bloom of six petals, tall stag antlers with leaves | leaf wings with midribs and a vine | bloom staff (two stems, thorns, open flower) | thorn arch with four flower buds | vine with leaves and a bud | ring of buds and leaves | ruff of ten leaves | petals and spores |
| Zepar | heavy great helm, brow slab, curled ram horns | crystal shards on thick bones | giant great axe (bearded double bit) | crown of eight crystal shards round a stone disc (turns) | spiked club tail | none | spiked gorget | shards and stone dust |
| Furfur | bell crown, speaker ear-cups, trumpet horns | concentric sound-wave arcs | war horn with sound rings | three concentric rings (pulse) | ribbon with a bell | ring with tuning forks and notes | four ribbons with bells | notes over the head, enchant motes |
| Marchosias | wolf cowl, tall ears, fangs on a brow ridge | ragged ember bat wings with claws | ember claws on a fist-bar | a mane of fire spikes down the spine | fur brush with a flame tip | none | wolf fur ruff | a trail of embers from the feet |
| Sitri | sun crown, three buns, long hair, gold scrolls | radial fan of gold rays | dawn bow (light string, arrow) | the two extra golden arms (ref 61) and a sun disc | none | thin lens halo (ref 61) | long hair | golden motes |
| Bifron | storm mask, forked zig-zag antlers | zig-zag lightning vanes | storm glaive (double ended) | fan of five great bolts | forked blade | ring of eight bolts | none | violet sparks |
| Paimon | crescent moon circlet, a tall white mane, crescent wing-horns | angel wing: coverts and eight primaries | twin moon glaive | full moon disc with a ring and stars (ref 59 tile 9) | white plume | ring with crescents | white fur collar (ref 60) | white feathers and frost |
| Vassago | skull plate with glowing sockets, heavy abyssal horns, tentacles | jellyfish dome of five ribs with hanging tentacles | abyssal scythe | the reaper moon, a huge crescent blade | tentacle with an eye | none | tattered shroud | abyssal drops and ink |

## The Dark Djinn (refs 60, 61, 62)
Same silhouettes, a second palette and extra parts, generated by the same scripts:
* black metal and cloth (obsidian, a trace of the Djinn's colour), gold trim and edges (so black wings with gold edges), the Djinn's saturated colour kept in the crystals and the glow (so each Dark Djinn is told apart),
* a **ring of black thorns edged in gold** over the head, turning (every Dark Djinn has one, even those without a halo),
* the **eye crest** on the brow (ref 62: a black chevron of two swept wings over a gold-ringed eye whose pupil glows in the Djinn's colour), drawn from the Half Equip on,
* **black rukh feathers** hanging from the wings,
* body-art: gold and accent lines (additive) plus **black veins** over the skin (alpha blended): black marks under the eyes, on the temples and along the limbs,
* the Zagred aura tinted a dark red-violet with 22% of the Djinn's own glow (`DjinnLook.darkAura`; pairwise distinct for the twelve), the assembling burst in black-violet dust and ink,
* **black rukh particles** (dark dust streaming backwards off the wings) on top of the Djinn's own,
* the Lesser Summon's bust (black body, gold horns, eye crest, black wings), the first-person arm (the dark vambrace and gauntlet),
* HUD and screen: a black-violet vignette with an extra black edge, a gold frame, "Dark" in the phase label.

## Budget, tests
Shipped sets: 2,900 to 4,700 quad lines in a Full Equip (the tail counted five times); the test caps a set at 5,200. About 18 MB of new meshes and textures (1024 px skins). Pure tests: `DjinnLookTest` (set picking, file names, colour maths, the dark aura pairwise distinct, the per-Djinn tables, particle counts and shares, which piece comes in at which phase; five mutations were tried and each killed by a test), `DjinnUniqueAssetsTest` (format, budget, the dark sets add eye and halo, weapon in item space with the blade along +y, no two Djinn share a silhouette piece, old rig kept, texture sizes, dark skins darker).

## What could not be tested
Nothing was run in the game. The type-check harness (with private stubs for Minecraft, ClientLevel, PlayerRenderer and Resource, which its stub jar prunes) is clean on every file I wrote; the previews are a software rasteriser. Untested in game: the weapon's pose in the hand (it reuses the vanilla item-layer chain and the display transform of a hand-held tool, see the report), the shared sheath's corrected direction, the particle positions behind the back, the alpha-blended veins over the skin, the look of the glow textures under the real additive shader.
