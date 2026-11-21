# Infinity Stones, Phase B (carriers, the reworked Snap, Loki's Scepter, Mind Stone leftovers, the Vormir altar)

Status: built and compiled, pure tests green. Nothing here has been seen in a running game (see "Honest caveats"). Version not bumped, nothing committed, per the lead's instruction.

## What it is, in plain words

* **The Infinity Gauntlet and the new Nano Gauntlet are carriers.** Right-click "Socket a Stone" and the next Infinity Stone in your pack that fits an empty socket is set into the gauntlet (up to six, one per kind). The stone leaves your pack only after the gauntlet is read back with it inside. "Unsocket the Stones" gives them all back. Every socketed stone's acts appear on the gauntlet (the Mind Stone has four: Psionic Lance, Psionic Reach, Telekinesis, Mind Link; the others two).
* **The synergy circuit** (Soul, Mind, Power, Space, Time, Reality, back to Soul): a stone socketed next to the one before it in the circle acts 25% stronger and recovers 15% faster.
* **Socket surge**: socketing stings. It takes a share of health (10% of the base, scaled down the stronger you are above the 1 billion EP gate, twice as much through the Nano Gauntlet, never above 30%) and never kills (it always leaves a health point).
* **Combos**: Mend (Soul + Time + Reality): heals up to 12 allies for 40% and cleanses them. Rift (Space + Power): strikes up to 8 foes within 24 blocks.
* **The Snap** (all six socketed, hold 5 seconds, costs 8% existence points): choose a mode with "Choose the Snap": Restore (heal and cleanse allies), Purge (cleanse the area), Grant (a short boon), Erase (off by default). Never touches a player, never an instant kill, never world-wide: at most 40 targets within 48 blocks. A boss only ever loses a tenth of its health, other hostile mobs lose at most half (weak ones under 120 health are dusted), named, owned and relic-owned mobs are spared. The wearer pays a toll (weakness and mining fatigue); the Nano Gauntlet also leaves a "burned arm" weakness for an hour. Once per N game days (7 by default). A stun, bind, paralysis or petrification on the bearer blocks every use.
* **Loki's Scepter** (God grade, staff, melee 8 damage, engraved magic weapon, severance 2, magic interference): holds the Mind Stone only. Socketed, it gives the Mind Stone's four acts plus Scepter Blast (16 damage bolt, 24 blocks) and Scepter Barrier (absorbs 8 damage for 10 seconds). No mind control anywhere (owner decision): nothing sets a target, owner or tame state.
* **Mind Stone leftovers**: Telekinesis lifts one non-player, non-boss mob for 3 seconds and then throws it (the second press cannot land because of the act cooldown, so it auto-throws). Mind Link shares the beneficial effects of the wearer with up to 4 allies for 30 seconds (no chaining: only effects with more than 5 seconds left are shared). Mastery bonus: with the Mind Stone socketed, +1 mastery point on one learned, unfinished Tensura skill every 80 seconds of play. Astral scouting is not built.
* **Vormir altar** (block): right-click shows two clickable lines, "Offer your best skill" and "Offer a named pet", which run the existing Soul Stone trial command. Nothing is consumed before success (Phase A rule). The gate of 1 billion base EP still applies.

Everything keeps the Phase A rules: gate by base EP, one canonical stone per server (a socketed stone stays on the ledger because the Order scanner reads the container), nothing consumed before success.

## Settings (server config `nusmp-stones-server.toml`, section `infinityStonesB`)

| key | default | what it does |
|---|---|---|
| carriers | true | gauntlet sockets; false = the Wave 30 gauntlet exactly as before |
| socketSurge | true | the sting when socketing |
| synergyCircuit | true | +25% effect and -15% cooldown |
| combos | true | Mend and Rift |
| snap | true | the Snap at all |
| snapErase | **false** | the Erase mode (removes mobs) |
| snapToll | true | weakness and fatigue after a Snap |
| snapCooldownDays | 7 | game days between Snaps (at least one) |
| scepter | true | Loki's Scepter acts |
| telekinesis | true | lift and throw |
| maxLifts | 24 | server-wide cap of mobs held at once |
| mindLink | true | Mind Link |
| maxLinks | 24 | server-wide cap of open links |
| masteryBonus | true | the mastery points |
| altar | true | the altar block answers |
| altarRequired | **false** | the Soul Stone offer works only within six blocks of an altar |

## Worst-case cost (Rule 11)

* Per second, per player with a carrier: one mask read (staggered by player, once a second, cached) and one rewind sample when Time is socketed.
* Per tick: only the open lifts and links (global caps 24 + 24; each lift moves one entity, each link pulses every 40 ticks to at most 4 allies, at most 6 effects per pulse). No world scan, no chunk load.
* Snap: at most 40 mobs once per 7 game days per player, one 48 block box query, one server broadcast.
* Altar click: 1 second spam guard, a 13x13x13 block check only when the altar is required, only on the command.
* Packets: none new except the vanilla ones; Fx effects use the existing perf budgets and go to nearby players only.
* Client: the scepter mesh is 832 quads (790 in the main layer, 42 glow), one atlas of 512 px; the altar is a plain cube. The Nano Gauntlet has a sprite and uses the existing hand look.
* Cleanup: lifts, links, mask caches and mastery timers are dropped on logout, death, dimension change and server stop.

## NEW, REPLACED, KEPT

* NEW: Nano Gauntlet, Loki's Scepter (+ Better Combat file, mesh, icons), Vormir Altar (block, item, loot table, textures), the carrier code (`stones/Carrier*`, `SnapActs`, `MindActs`, `ScepterActs`, `VormirAltarBlock`), Telekinesis and Mind Link acts, the settings section, lang fragment `tools/lang/infinity_b.json`.
* REPLACED: the Phase A Snap of the Wave 30 gauntlet is untouched; the new Snap is a carrier act on the gauntlet next to it, behind `snap` and `carriers`. Nothing removed.
* KEPT: every Phase A and Wave 30 act, setting and rule. Old carried stones still count through the gauntlet (Wave30Kit also reads socketed stones).

## Not built

Iron Man Mark LXXXV and Ultron armour, Vision, Death, Ego, astral scouting, stone sources in the world, generating the altar in a structure (creative or operators place it), a global once-per-real-day limit on the Snap, and an interrupt of the Snap hold other than at fire time.

## Honest caveats

* Never run in a game: only javac, the pure tests and the asset checks ran. The CI server smoke test (`stones.carrier.rules|defs|data|state|snap|altar`, under the existing `stones` prefix) is the first real run.
* The scepter mesh was made by the numpy mesh script (`tools/relics/infinity_b_scepter.py`), not Blender (Blender cannot run in this session). It is a rigid staff, so it needs no swing rig (`build_swing.py --check` is green). The Nano Gauntlet is a sprite plus the existing hand look; the altar is a flat-texture cube. No Blockbench model was made.
* Sprites come from a PIL script (`tools/relics/infinity_b_icons.py`).
* No wiki was fetched; names and effects follow the lead's brief.
* The mastery bonus is time based, an approximation of "mastery goes up faster".
* Pre-existing red tests seen in the wider sweep, not caused by this work: RankingsTest (5), UpgradeParticleNamesTest (`mote`), CodexEntriesTest (2).

## Owner decisions

Erase default stays off (it removes mobs). `altarRequired` stays off so the trial works as in Phase A. The scepter needs the 1 billion EP gate for its stone acts. Telekinesis auto-throws after 3 seconds.
