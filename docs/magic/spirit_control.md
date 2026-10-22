# Spirit control plane (3.0.0): summon, release, abilities and combat mode without keybinds

Code: `src/main/java/com/newuniverse/nusmp/spiritcontrol/**`, one small handle class in each spirit package, a few hooks in `book/GrimoireBook`.
Config: `nusmp-spirit-control-server.toml` (SERVER). Self-tests: `magic.spirit_control`, `magic.spirit_combat`. Lang: `tools/lang/spirit_control.json`.

## What the owner asked

"Add a way to release/summon the Spirit Lord from my mod just how a Djinn user can: spirit users should be able to summon the spirit from their GRIMOIRE or with a COMMAND, NOT a keybind
(I already have too many keys). They should also be able to toggle on a COMBAT MODE for their spirits that allows the spirit to cast spells using BOTH its mana and the user's mana/magicules."

## How it is read (player's view)

### 1. From the grimoire (the pages)

Six pages are appended at the very end of every grimoire (after the Mana Zone's pages, so no page id or mode number moved). They are hidden (a `?` page the page switch skips) unless a spirit of the
book's magic answers you:

| Page | Id | What it does |
| --- | --- | --- |
| Summon Spirit / Release Spirit | `spirit_call` | a toggle that acts on the key press (like Summon Grimoire): calls the spirit, or sends it away when it is out |
| Spirit Combat Mode: ON / OFF | `spirit_combat` | the combat mode toggle, also on the press |
| the spirit's abilities, 1 to 4 | `spirit_ability_1` .. `spirit_ability_4` | ordinary pages: hold and release to cast, the grimoire must be summoned; named after the abilities of the spirit that answers |

Which spirit sits in which book (the package's own rule decides who answers, exactly as its old key did):

| Book | Spirit | Answers when | Abilities (pages 1..) |
| --- | --- | --- | --- |
| Fire | Salamander | the bound canon grimoire is Fuegoleon's | Salamander Dragon (the big form) |
| Wind | Sylph | Yuno's | Sylph Powered Form |
| Water | Undine | Noelle's | Undine Full Form |
| Earth | Gnome | the player holds the Earth grimoire skill | Gnome Giant |
| Light | Spirit of Light | Light Magic | Light Beam, Glowing Pulse, Seraph Form |
| Dark | Spirit of Darkness | Dark Magic | Shadow Step, Demon Form |
| Spatial | Spirit of Space | Spatial Magic (the config may relax it) | Spirit Portal, Gravity Pulse, Spatial Cut, Mark Waypoint |
| Fire, Water, Wind, Earth, Anti-Magic | Spirit Lord (the orb and the true body) | the Spirit Lord skill or a spirit bond; the book is the one of the bond's element | none (it fights; Spirit Dive and Spirit Burst are still their own pages) |

Waypoint slot of the portal and waypoint pages: the slot you last chose. Cast either page while SNEAKING and it does not cast but picks the next slot (the old cycle key); the command with a slot also sets it.
The Spirit Lord skill (for mages without a grimoire) has a third mode, "Spirit Combat Mode", next to Spirit Burst and Call Spirit; its Call Spirit mode is unchanged.

Page price: the toggles are free. An ability page asks the price of the spirit's ability: 6 percent of your maximum magicule for the forms, 3 to 5 for the Light and Darkness abilities; the Space spirit
asks nothing on the page because its own code already charges its own mana and cooldown. A refused ability gives the price back. The spirit's own cooldowns are untouched.

### 2. From the command (op-free, your own spirit only)

`/multiverse spirit ...` and the short `/spirit ...` (the same tree; `bond`, `incarnate` and `info` of the old tree are unchanged):

* `summon [spirit]`, `release [spirit]`: call or send away (with no name: the first spirit that answers you and fits; names are suggested)
* `status`: every spirit that answers you, whether it is out, its abilities, its mana pool, and whether combat mode is on
* `combat on|off`
* `ability <name> [slot]`: one of the spirit's abilities (names are suggested); `slot` is 1-based for the portal and the waypoint

Every ask goes through `SpiritControl`, the same code the pages call: the switch, the spirit's own rule, an ability the spirit lists, a slot in range, and one ask per `control.askGapTicks` (10) ticks per player.

### 3. Combat mode

Switch it on (page or command). While it is on and a spirit is out, every `combat.decisionTicks` (10) ticks the server makes one decision for you:

1. find the target: your last mob target or attacker if it is still good, otherwise (one search, only while you are in a fight or once every 40 ticks when idle) the nearest hostile mob in range;
2. pick the strongest spell of the spirit's small list that is off cooldown, in range, and affordable in BOTH pools;
3. pay: the spirit's own mana (`SpiritMana`, a pool that grows with the bond and refills lazily) AND a share of your magicules (`combat.userManaShare`, half and half by default; one unit is
   `combat.unitUserPercent` percent of your maximum, at least `combat.userFloor`, times the mana law's cost multiplier);
4. cast: damage through the balance law, credited to you, capped by `combat.maxDamagePerCast`; power follows the bond and both pools (0.6 to 1.6 times).

If either pool cannot pay, the spirit stops and says so on the action bar (at most once in five seconds). The spirit never spends your last `combat.userReservePercent` (10) percent of magicules.
Targets: hostile mobs and mobs that hunt you; a player only where the server already counts them your enemy (PvP allowed, not your FTB team); never yourself, your pets, summons, allies or teammates
(the existing `Foes` helper decides). A Spirit Lord with combat mode on stops its free casts and casts through this code instead (so it pays); with combat mode off it casts for nothing as always.
Combat mode ends at logout, death, respawn and dimension change, like the spirits' own links; the pools survive a death or a change of world (no refill by dying), and go at logout.

| Spirit | Combat spells (damage / mana units / cooldown ticks / range) |
| --- | --- |
| Salamander | Flame Bolt (7 / 8 / 30 / 20, ignites), Fire Ball (12 / 16 / 80 / 18, ignites) |
| Sylph | Wind Blade (7 / 8 / 30 / 20), Gale Gust (10 / 14 / 70 / 16, throws up) |
| Undine | Water Sphere (7 / 8 / 30 / 20), Tidal Splash (11 / 15 / 80 / 16, slows) |
| Gnome | Stone Shot (8 / 9 / 35 / 18), Earth Spikes (12 / 16 / 90 / 14, slows) |
| Spirit of Light | Light Beam (8 / 9 / 30 / 22), Glow Mark (5 / 10 / 70 / 20, glowing) |
| Spirit of Darkness | Shadow Mark (5 / 10 / 60 / 20, weakness), Shadow Lance (9 / 11 / 35 / 20) |
| Spirit of Space | Rift Cut (8 / 10 / 40 / 20, slows), Gravity Tug (10 / 14 / 80 / 16, throws up) |
| Spirit Lord | the list of its element; the Anti-Magic lord: Null Slash (8 / 9 / 40 / 18) |

Spirit pool: `spiritManaBase` (60) + 0.6 per trust point (so 60 to 120), regeneration 2 + 0.05 per trust point per second (2 to 7). Bond = the Spirit Bond's trust when there is one, else 50.

## Config (`nusmp-spirit-control-server.toml`)

`magic.spiritControl` (true): off hides the pages, refuses the commands and stops combat mode; the spirits themselves keep working. **The old keybindings are gone for good and do not return with this switch.**
`spirits.*`: a switch per spirit (all true). `control.askGapTicks` 10. `combat.*`: `enabled`, `userManaShare` 0.5, `decisionTicks` 10 (5 to 100), `minCastGapTicks` 30, `maxCastsPerSecondServer` 24,
`maxDecisionsPerTick` 8, `idleScanTicks` 40, `fightWindowTicks` 200, `targetCap` 12, `rangeScale` 1, `damageScale` 1, `maxDamagePerCast` 60, `unitUserPercent` 0.25, `userFloor` 15,
`userReservePercent` 10, `spiritManaBase` 60, `spiritManaPerTrust` 0.6, `spiritRegenBase` 2, `spiritRegenPerTrust` 0.05. Every number has hard bounds in the spec.

## How it is built

* `SpiritControl` (facade) + `SpiritRegistry` (pure): each spirit package registers ONE `SpiritHandle` with an append-only line in its Boot (`SalamanderSpirit`, `SylphSpirit`, `UndineSpirit`, `GnomeSpirit`,
  `SpiritLightControl`, `SpiritDarkControl`, `SpiritSpaceControl`, all package-private, all calling the package's own link / unlink / form / ability methods); the Spirit Lord's handle is `LordSpirit`.
* `SpiritControlPages` + `PageGate` (pure): the six pages and who sees them; page names come from a small `SpiritUi` tag the server writes into the book's skill instance once a second and after every action
  (only when it changed), so clients name the pages without a packet of their own.
* `SpiritCombat` + `CombatRules`, `ManaSplit`, `SpiritMana`, `RateWindow`, `TickBudget`, `PlayerGap` (all pure): the combat mode and its throttles.
* `SpiritControlCommand`: the verbs, under `/multiverse spirit` (one append-only line in `MultiverseCommands`) and the `/spirit` alias (registered by the plane's own event).

## Cost, worst case (rules 9 and 11)

Everything is decided on the server; there is NO packet from the client for any of it (the key packets are gone) and no new packet type; page names ride on the skill instance's existing sync.

* Idle server (nobody in combat mode): the tick handler is one `isEmpty()` check per player per tick.
* One player in combat mode: 2 decisions per second; each does at most ONE entity search (box of the longest spell range, at most `targetCap` 12 candidates looked at), none when the last target is still good,
  and when idle at most one per 40 ticks; at most 1 cast per 30 ticks (0.67 casts per second); one line-of-sight ray per cast; no chunk or block is read or loaded.
* Whole server: at most 8 decisions per tick (160 per second, so at most 160 searches per second even in a crowd), at most 24 casts per second. Each cast sends one effect packet through the existing
  effect budget (160 per tick) to players within 96 blocks and one level sound to players in earshot: at most 48 packets originated per second on the whole server, each only to nearby players.
* Damage per cast capped at 60 after all scaling; per-hit caps of the balance law apply on top (players, bosses).
* Pages and commands: at most 2 asks per second per player, each running the spirit's own existing code (its own caps and ledgers: one spirit per player, the server-wide spirit caps).
* The once-a-second page-name sync does one eligibility check per spirit of the book's magic per grimoire (books of magics with no spirit return at once); it writes only on a change.
* Memory: per player in combat mode three longs and a small map of at most eight pool entries; the ask-gap map is held under 512 entries; everything goes at logout (pools) / death, respawn, dimension change (mode).
* Client draw cost: none added.

## NEW / REPLACED / KEPT

| | |
| --- | --- |
| NEW | the control plane (`spiritcontrol/**`), the six grimoire pages, the commands `/multiverse spirit summon|release|status|combat|ability` and `/spirit ...`, combat mode with the spirit mana pool and the two-pool cost, the Spirit Lord skill's Combat Mode (mode 2), the Spirit Lord's handle, `nusmp-spirit-control-server.toml`, the self-tests, the lang fragment |
| REPLACED (owner asked) | the seven spirit keybindings and their client key handling and key packets: Sylph key, Undine key, Gnome key, Salamander key, Spirit of Light (link, command), Spirit of Darkness (link, step, power), Spirit of Space (link, cycle, use). Deleted: `client/{sylph,undine,gnome,firecast,spiritdark,spiritspace}/*Client`, the six `*Net` key packets, the key record of `SpiritLightNet`, the `RegisterKeyMappingsEvent` hooks, the six `NUSMP` client init lines. The `key.nusmp.*` lang entries stay in `en_us.json` (harmless) |
| KEPT | every server-side action (`toggle`, `link`, `unlink`, `onKey`, the forms, the abilities), the spirits' looks, sounds, caps, ledgers and moods, the Spirit Lord entities and their free casts (while combat mode is off), the Spirit Lord skill's two modes, Spirit Dive and Spirit Burst, `/spiritspace` (waypoint names), `/multiverse spirit bond|incarnate|info`, the admin `spirit_body`, `spirit_lord`, `spirit_release` commands, all page ids and mode numbers of every book |

Shared files touched (additions only, except the key deletions): `GrimoireBook` (7 hooks), `MultiverseCommands` (1 line), `NUSMP` (1 line added, 6 key lines removed), the seven spirit Boots (1 line each, the
payload line removed), `SpiritLordSkill` (mode 2), `SpiritLordEntity` and `ElementalSpiritLordEntity` (1 guard line each), `Salamander.summonDragon` and `SylphCasting.summonPowered` (visibility only),
`SpiritLightNet` (key packet removed), `ManaZoneSelfTests` (the Mana Zone's three pages must now be the three right before the spirit pages instead of the last three).

## Tests

JUnit (pure): `SpiritRegistryTest`, `ManaSplitTest`, `SpiritManaTest`, `CombatRulesTest`, `SpiritThrottleTest`, `PageGateTest`, `SpiritCatalogTest`, `SpiritControlSourceScanTest`. Updated because they asserted the old keys:
`SylphSourceScanTest`, `FireCastSourceScanTest`, `SpiritLightSourceScanTest`, `SpiritDarkSourceScanTest`, `GnomeSourceScanTest`, `UndineSourceScanTest`, `SpiritSpaceSourceScanTest` (the key packet and key class
assertions now assert they are gone and that the handle asks the same rule). CI self-tests: `magic.spirit_control`, `magic.spirit_combat`.

## Not verified (nothing was run in a game)

* The page flow end to end: holding and releasing an ability page, the toggles on the press, the page names appearing in the client from the `SpiritUi` tag, the hidden `?` pages being skipped when switching.
* That the grimoire's cost check (`EnergyHelper.isOutOfEnergy`) takes the price before the cast, as the refund on a refused ability assumes (the Mana Zone's True Magic page makes the same assumption).
* The look and sound of a combat cast (existing effect shapes and vanilla sounds are used; no spirit-specific sound per spell), and the wait between the spirit's own animation and the damage (the damage is instant).
* How the FTB teammate and PvP rules behave in a real party (the existing `Foes` helper is trusted).
* The stand-in player of the self-tests has no Tensura data, so the owner's pool payment is covered by the pure tests, not by the self-test; the spirit's pool payment is covered by both.
* The full test sweep (Gradle is not available offline): only the pure tests and the source scans listed above were run, with a stub-compiled type check; see the report for the exact list.

Decisions left to the owner: whether the canon-book gating (Fuegoleon for the Salamander, Yuno for the Sylph, Noelle for Undine) should also be offered with a plain spirit contract; the combat spell numbers; the page prices of the abilities.
