# Ultimate Magic (system 3 of the owner's master spec)

"Each magical attribute gets ONE Ultimate: a long wind-up that holds the caster in place, all of the caster's mana, a massive area hit with heavy debuffs, and cinematic but cheap effects."
Audit (new vs replacement) and the unbuilt proposals for every other attribute: `docs/magic/ultimate_audit.md`. Previews (software render, the real effect layers): `docs/previews/ultimate/`.

## What was built

A data-driven framework (`ultimate/UltimateDef` + `UltimateRegistry`: id, book, name, element, origin, wind-up, radius, damage, debuffs, fire / freeze / push / lift, three effect shapes with their powers) and the eight flagship Ultimates:

| Book | Page | Name | Origin |
|---|---|---|---|
| `book_fire` | `ultimate_fire` | Ultimate Flame Magic: Crimson Sun Descent | original |
| `book_water` | `ultimate_water` | Ultimate Water Magic: Sea God's Deluge | original |
| `book_wind` | `ultimate_wind` | Ultimate Wind Magic: Heavenfall Tempest | original |
| `book_earth` | `ultimate_earth` | Ultimate Earth Magic: World-Splitting Titan Quake | original |
| `book_lightning` | `ultimate_lightning` | Ultimate Lightning Magic: Thunder Sovereign's Verdict | original |
| `book_light` | `ultimate_light` | Ultimate Light Magic: Dawn of Judgment | original |
| `book_dark` | `ultimate_dark` | Ultimate Dark Magic: Black Moon Eclipse | original |
| `book_spatial` | `ultimate_spatial` | Ultimate Spatial Magic: World's Edge Collapse | original |

(Why original: the show's named Ultimate Magics, Excelitius Leonum / Lightning Battle Fiend / Apocalypra Astrauza, already exist as character pages and are kept; see the audit.)

## How a player meets it

1. The page is the LAST family page of its book (`UltimatePages.append` wraps outside the 15-spells hook, so no existing mode, id, unlock bit or mastery moves). It is hidden in a grimoire bound to a character whose set does not list it; the plain and basic grimoires show it.
2. It opens at the end of the mastery bar and only for a player who passes the gate (`UltimateGate`, asked through `GrimoireBook.pageBlock`): **full mastery bar, Magic Knight step 14 (Senior 1st Class) and 1,000,000 EP** (Wizard King: rank and EP waived, mastery still needed). The cast checks the gate again.
3. Hold the page and release as for any page: at max magicule (99 percent) a wind-up starts (default 120 to 200 ticks by element, scaled by `windupScale`, always 100 to 400). The caster is held in place (movement speed and jump strength transient modifiers), the existing looping charge clip plays, a circle follows the caster, the ground picture of the element is re-sent every 40 ticks, nearby players (radius + 48 blocks, at most 32) get an action-bar warning so they can run or interrupt.
4. At the end: ALL current magicule is taken, the cooldown (default 15 minutes) starts, and three pulses at +0, +10, +20 ticks (60 / 25 / 15 percent of the damage) hit up to 24 foes each inside the radius; the first pulse also applies the debuffs, fire, push / pull and lift. Then the mana law's exhaustion applies (slowness and weakness, a blackout that does not kill by default).

## Cancel rules (decision: nothing is spent before the release, so nothing is refunded)

| Reason | Effect |
|---|---|
| caster dies / logs out / changes dimension | cast ends, nothing spent, short lockout (10 s; none after a server stop or an admin end) |
| damage during the wind-up reaches 35 percent of max health (`interruptFraction`, 10 to 100 percent) | cast ends |
| sealed (Spell Seal, grimoire seal) | cast ends |
| reserve drops under 90 percent of max (another spell was cast) | cast ends |
| sneak held 15 ticks after the first second (`sneakCancel`) | the caster lets go |
| `ultimate.enabled` set to false | cast ends; the pages are no longer appended |
| server stopping | all casts dropped |

After the release the entry only finishes its pulses; a caster who died or left in that time loses the remaining pulses (the magicule is gone).

## Settings (SERVER config `nusmp-ultimate-server.toml`, section `[ultimate]`)

`enabled` (true; false = the books exactly as before), `maxSimultaneous` (2, 1 to 8, global), `cooldownTicks` (18000, never under 6000), `windupScale`, `radiusScale` (0.25 to 2), `damageScale` (0 to 2), `interruptFraction` (0.35), `sneakCancel` (true), `exhaustAfter` (true), `hitPlayers` (true; they still must be foes: PvP setting and teams), `gateStrictness` (EP x 0.5 to 4, floors 500,000 EP and step 12). Every value is clamped again by `UltimateRules` when read.

## Worst-case cost (rule 11)

* Idle: every handler returns on `SESSIONS.isEmpty()`; no per-tick work, no allocation.
* Per active cast per tick: O(1) checks; a particle emission every 10 ticks (2 `PerfParticles` packets, 14 + 8 particles, through the per-tick budget); lock re-check every 20 ticks.
* Server totals: at most `maxSimultaneous` casts (default 2, hard max 8); each cast at most 16 effect packets (13 by the rule for a 400 tick wind-up: circle, 6 ground pictures, release, burst, 2 echoes), every one through `VfxSpawn`'s nearby-players rule and per-tick packet budget; at most 3 area queries (one per pulse), at most 96 candidate entities looked at and 24 hit per pulse (72 hits per cast); no entity spawned, no block changed, no chunk loaded, no new packet type, no new client class.
* Client draw cost is that of the reused shapes (the previews report 12 to 384 of the 400-vertex budget per effect; the largest are the Wind tornado at 384 and the Earth sigil at 368).
* Per-player state: the persisted player tag `nusmp_ultimate` (cooldown, lockout, counter); the running cast is a `Map<UUID, Session>` on the server thread, removed on every end (death, logout, dimension, damage, seal, sneak, low mana, disabled, shutdown, release finished).

## Files

New: `ultimate/` (`UltimateRules`, `UltimateGate`, `UltimateDef`, `UltimateRegistry` pure; `UltimateConfig`, `UltimatePlayer`, `UltimatePages`, `UltimateManager`, `UltimateBoot`, `UltimateSelfTests`), `tools/lang/ultimate.json` + `tools/lang/merge_ultimate.py`, `tools/vfx_preview/scenes.d/ultimate.json`, `docs/previews/ultimate/`, `src/test/resources/armour_policy/ultimate_new_pages.json`, tests in `src/test/java/.../ultimate/`.
Shared files (append / one-line): `book/GrimoireBook.java` (the wrap in `familyAll`, two cache flags, the first line of `pageBlock`), `book/pages15/Pages15SelfTests.java` (`first` leaves the Ultimate out), `NUSMP.java` (one boot line).

## Tests

Pure: `UltimateRulesTest` (cost, caps, cancel rules, damage clamps, cooldown floor, packet and target bounds), `UltimateGateTest`, `UltimateRegistryTest` (table, shapes, no Time shape, vanilla effects, id clashes), `UltimatePagesTest` (the hook order, the gate order, the new-pages file, the lang fragment), `UltimateSourceScanTest` (no client class, no packet, no static player, no spawn / block / chunk, damage only via the balance law, targets only via `Foes.isFoe`, budgeted particles, capped packets, early returns, magicule and cooldown touched in one place each, SERVER config). Armour policy: an Ultimate never equips an armour (its ids are in `ultimate_new_pages.json`, not in `armour_policy.json`; the pages15 files are not used because `ArmourPolicyTest` ties them to batches 1 to 8 and to page sources under `book/`).
In game (CI server smoke, key prefix `magic.ultimate`): `magic.ultimate.rules` (each Ultimate is the last family page of its real book with handler, lang text, real shapes and potion effects; closed for a fresh player in 6 ways; worst-case packets) and `magic.ultimate.cast` (stand-in players: one entry per caster, the global cap, the hold on and off, every cancel reason, the release takes all magicule, cooldown, at most 24 foes hit, creative player / caster / far mob untouched, debuffs applied, clean end).

## Not verified

Nothing was played: no cast in a real client, no check of how the grimoire UI shows the page or the key flow feels, no multiplayer run, no look at the effects inside the game (only the software-render previews of the real layers, which have no world, lighting or depth). The charge pose is the Mana Zone's clip (no new Blockbench animation). The Light and Space ground pictures are the Zogratis layers (boss look).
