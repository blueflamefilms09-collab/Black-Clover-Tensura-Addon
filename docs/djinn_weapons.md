# Djinn weapons (1.34.0): the Half and Full Equip give a real, temporary weapon

Until 1.33.0 the Djinn Equip drew its weapon only as a render layer (`DjinnArmorLayer`, the `weapon` piece of the armour set). From 1.34.0 a Half Equip
(and the Full Equip that follows it) puts a **real item** in the bearer's main hand: one weapon per Djinn, 24 items in all (the twelve Djinn, each in a
light and a dark look), with its own melee numbers, Tensura engravings and **two abilities**. The Extreme Magic (phase 4) is unchanged. This page
supersedes the older sentence "the Djinn weapon is only visual" in `docs/relics_of_many_worlds.md` and `docs/boss_overhaul.md`.

(1.93.0: thirty-six Djinn, so 72 weapon items; the loan overlays a weapon vessel instead of moving it; see section 5 and `docs/djinn_roster_36.md`.)

Code: `relics/djinn/weapon/**` (new), `relics/djinn/DjinnWeaponBridge.java` (new, the package-private helpers the weapons may use), small hooks in
`DjinnEngine`, `DjinnEvents`, `DjinnItems`, `DjinnState`, `DjinnComponents`, `DjinnCommand`; client: `client/relics/djinn/DjinnWeaponClient.java` (new) and
a three-line edit of `DjinnArmorLayer`. Art: `tools/relics/djinn_weapons.py`.

## 1. The items

`djinn_weapon_<name>` and `djinn_weapon_<name>_dark` (24 ids, `Djinn` enum order, light then dark), all one class, `DjinnWeaponItem`. The dark twin is a
separate item id (the state-variant system is not used): `DjinnWeaponService.apply` hands the bearer the one that matches the Djinn's colour *now*, and
swaps it in place when the Djinn turns dark or is cleansed (`DarkDjinn` already calls `DjinnEngine.refresh`).

Melee: attack damage and attack speed are the table's (below, dark x1.1 damage), the weapon adds entity reach, never breaks, glints. The Djinn's Half
Equip special (chain lightning, melt, freeze, push, life steal, crush, deafen, brand, smite, paralyse, slice, drown: `Djinn.Trait`) is unchanged and
rides on every blow of a bearer in Half Equip or higher: with the real weapon in hand that **is** the weapon's on-hit special.

Engravings (CLAUDE.md rule 6): Tensura's engraving enchantments, from `DjinnWeaponRules.engravings`, applied once when a stack is made by
`DjinnWeaponEngrave` (the same method as `item/WeaponEngravings`; only ids from the Tensura id reference are used, a test checks them). A dark weapon
also carries Ruination.

| Djinn | kind | damage | speed | reach | act 1, right-click (Magoi, cooldown, power, range) | act 2, sneak + right-click, Full Equip only |
|---|---|---|---|---|---|---|
| Baal | lance | 9.0 | 1.4 | +1.0 | thunder_throw (3.0%, 6.0s, 9, 20 b) | judgement_step (5.0%, 13.0s, 11, 8 b) |
| Amon | greatsword | 12.0 | 1.0 | +0.5 | sun_cleave (3.0%, 5.5s, 11, 6 b) | ember_wall (5.0%, 14.0s, 8, 7 b) |
| Vinea | trident | 9.0 | 1.5 | +0.8 | tide_spear (2.5%, 5.0s, 8, 11 b) | frost_surge (5.0%, 13.0s, 9, 7 b) |
| Focalor | glaive | 8.0 | 1.6 | +1.0 | cyclone_sweep (3.0%, 5.5s, 8, 4.5 b) | updraft_leap (4.0%, 11.0s, 6, 5 b) |
| Zagan | staff | 6.0 | 1.6 | +0.5 | bloom_heal (4.0%, 9.0s, 6, 9 b) | thorn_snare (4.0%, 12.0s, 7, 12 b) |
| Zepar | axe | 13.0 | 0.9 | +0.3 | titan_slam (4.0%, 8.0s, 12, 5.5 b) | rock_breaker (4.0%, 12.0s, 12, 4.5 b) |
| Furfur | horn | 7.0 | 1.2 | +0.3 | resonance_wave (3.0%, 6.0s, 7, 9 b) | dissonance (4.5%, 13.0s, 6, 6.5 b) |
| Marchosias | claws | 7.5 | 2.4 | +0.0 | ember_rush (3.0%, 6.0s, 8, 8 b) | brand (3.5%, 10.0s, 5, 14 b) |
| Sitri | bow | 6.0 | 1.8 | +0.5 | dawn_arrow (2.5%, 3.0s, 10, 32 b) | radiant_burst (5.0%, 13.0s, 8, 6.5 b) |
| Bifron | glaive | 9.0 | 1.5 | +1.0 | chain_lightning (3.0%, 6.0s, 8, 12 b) | storm_pillars (5.0%, 13.0s, 10, 14 b) |
| Paimon | glaive | 9.0 | 1.5 | +1.0 | crescent_wind (3.0%, 5.5s, 8, 10 b) | heavenly_dash (4.0%, 11.0s, 9, 9 b) |
| Vassago | scythe | 10.0 | 1.1 | +0.8 | abyssal_reap (3.5%, 7.0s, 9, 5.5 b) | drowning_pull (4.5%, 13.0s, 6, 9 b) |

Cost is a share of the **maximum Magoi** (Tensura magicules), multiplied by the same drain factor the phases pay (`DjinnPhaseRules.drainFactor`: the
multi-bond price, assimilation strain) through `Magoi.pay(..., DjinnPhaseRules.FLOOR)`, so an act never drives the pool below the floor; creative players
pay nothing. **Power** is a raw damage (or a heal for Zagan's Bloom Heal and the allies' share of Radiant Burst) that goes through `Fx.damageHeavy`: the
balance law and the 45% cap on players. In the Full Equip power is x1.35 and cooldowns x0.8; a dark weapon is x1.2 power and x1.25 Magoi. Sitri's Dawn Arrow
is a hitscan (it stops at blocks, hits the first two foes in its line, twice as hard on the undead): Sitri's weapon is an item with a ranged act, not a bow.
Details of each act are in the lang texts (`djinn_weapon.act.<key>.desc`) and the tooltip.

Every act: only foes the Djinn spells already strike (`Fx.foesAround`: hostile mobs, mobs that target the bearer, players the bearer may harm; never creative
or spectator players, allies, tamed pets or summons; at most 24, at most 12 hit per act), no block is changed, particles are bounded (the `DjinnFx` shapes
clamp their counts), a right-click is answered at most every 8 ticks, and nothing runs while an Extreme Magic charges. Storm Pillars keeps at most six
pending strikes per bearer (they fall 24 ticks later, the marks are drawn at once). Brand's mark is a table of at most 64 entries (`WeaponMarks`).

## 2. The loan: how the weapon gets in the hand and out again

`WeaponLoan` is a pure state machine (property-tested with 4000 random event sequences: no real item is ever lost or duplicated, at most one weapon
exists); `DjinnWeaponService` is its glue over the player's inventory.

* **Summon** (the engine enters phase 2): the weapon goes in the **hotbar slot that was selected** (the home slot). What stood there is displaced: to the first
  free slot of the 36 main slots (remembered), else onto the **stash list**. State (home slot, the displaced item and where it went, the stash) is saved in the
  bearer's Djinn record under `wloan` (`DjinnState.loan()`; `clearBond()` never touches it), so a crash or a relog cannot lose it.
* **Half to Full**: the same weapon stays; the Full Equip opens the second act and strengthens both. A Full Equip that ends into a Half Equip keeps it.
* **Enforcement**: every fourth tick (`DjinnWeaponEvents.onPlayerTick`, 42 slot checks) the loan is made whole: a weapon that is not in the home slot (moved, put
  in the offhand, an armour slot, the cursor, dropped) is wiped and made again in the home slot; an item put in the home slot is displaced like the original was; a
  weapon of the wrong look is replaced in place. The player may scroll away: the weapon waits in its slot. `ItemTossEvent` is cancelled for it, an `ItemEntity`
  holding one never joins the world, `LivingDropsEvent` strips it, and the item itself dissolves (`inventoryTick`) in anyone's inventory but its owner's, or once the
  Equip is over. A weapon carried into a chest stays inert (it dissolves when anyone takes it out) and is made again at once.
* **End** (phase below 2, a lost vessel, depletion, the Djinn taken back, logout, server stop, death, an admin): `DjinnEngine.stepDown` / `shutdown` call
  `DjinnWeaponService.release`: every weapon is wiped, the original item returns to the home slot (from the stash, else from where it was moved if it is still there),
  the rest of the stash fills free slots, and **only what finds no room at all** is dropped at the feet as an item that cannot burn, despawn or be picked up for a second.
  On death the Equip ends before the drops, so the original item is simply part of the normal drops (or stays under keepInventory); `LivingDropsEvent` is a second
  guard. On login and respawn `DjinnEngine.restore` releases again (a crash leaves a weapon and a stash behind; this cleans both).
* **Creative / spectator**: no item swap (`apply` releases instead), the old render-only look stays. **Extreme Magic**: unchanged; the weapon is quiet while it charges.
* **Admin weapons**: `/multiverse djinn weapon give [player [djinn [dark]]]` gives a weapon outside the loan (`nusmp:djinn_temp` with `admin: true`): it needs no Djinn
  Equip, is never taken back, can be dropped, acts as a Full Equip's. A bare stack (the creative tab) is adopted as an admin weapon by whoever holds it.
  `/multiverse djinn weapon clear [player]` removes every Djinn weapon and ends the loan. Both need permission level 2.
* The component is **`nusmp:djinn_temp`** (a compound: `owner` uuid, `djinn` name, `expires` game time or 0, `admin`), registered in `DjinnComponents` (already on the mod bus).

## 3. The art

The meshes are **derived from the Djinn armour set's own weapon pieces** (`assets/nusmp/djinn/u_<id>/weapon.txt` for the light look, `d_<id>/weapon.txt` for the dark
one): the same geometry, painted skin and glow, converted by `tools/relics/djinn_weapons.py` into WeaponRenderer's baked format (`qn` quads with smoothed normals,
the weapon method's layers, `orient manual`). Each is scaled to its target apparent length under the size rule (long weapons 2.2 to 3.6 blocks): Baal's lance 3.3,
Amon's greatsword 2.8, Vinea's trident 3.2, Focalor's glaive 3.4, Zagan's staff 2.8, Zepar's axe 2.7, Furfur's horn 2.3, Marchosias' claws 1.7 (a short weapon),
Sitri's bow 2.4, Bifron's glaive 3.4, Paimon's twin-moon glaive 3.2, Vassago's scythe 2.8. The edge or head of an asymmetric weapon is at -x (the orientation
convention: the horn and the scythe were mirrored), the hand holds the weapon exactly where the armour layer held it (the mesh origin), and the winding of every quad
was checked against its normals.

Per weapon: `weapon_meshes/<id>.txt`, `textures/entity/weapon/<id>.png` and `_glow.png` (the armour skin with everything the quads do not use cleared, 30 to 90 KB),
`models/item/<id>.json` (builtin/entity; first-person scale from `display_presets` with the grip at the mesh origin, marked `"nusmp_display_preset": "manual"` so
`display_presets.py --apply` leaves it alone) and `<id>_icon.json`, and the 16 x 16 inventory icon `textures/item/<id>.png`, rendered from the mesh and its skin.
`audit_weapon_scale.py` and `apply_weapon_fixes.py` skip the `djinn_weapon_` prefix; `python tools/relics/djinn_weapons.py --check` is the CI check that the files are
what the tool writes. The renderer is `WeaponRenderer` itself (the weapons are not in its fixed list: `DjinnWeaponClient` registers the same renderer for the 24 items
and their icon models).

When the real weapon is in the main hand, `DjinnArmorLayer` does **not** draw its own weapon piece (or the shared rig's sheath): no duplicate. A creative bearer still sees it.

## 4. Tests

`relics/djinn/weapon/WeaponLoanTest` (8 cases: the loan, stash, overflow, strays, round trip, and the 4000-sequence property), `DjinnWeaponRulesTest` (every row, the
Full / dark numbers, the cooldown table, the ids, every engraving id is a Tensura one), `WeaponGeoTest` (cone, beam distance, wall, crescents, chain), `ActClockTest`
(cooldowns, a clock that went backwards, the mark table and its cap), `DjinnWeaponAssetsTest` (the 24 meshes, skins, icons, models, the size band, the lang table).
Nothing was run in the game.

## 5. 1.93.0: the weapon overlays a weapon vessel, thirty-six weapons, better acts

This section changes the text above where it says so; nothing above was removed.

**The bug and its fix.** A vessel can be a weapon. The vessel is found only while it is held (a hand), worn (armour) or in a Curios slot (`VesselAccess.findBound`, from the
core's `Relics.equipped`). The loan (section 2) displaced whatever stood in the selected hotbar slot to the first free slot, so a weapon that was also the Djinn's vessel
left the hand, the engine's once-a-second check found no vessel, and the Equip ended ("Your vessel left you") the moment it began; the Djinn weapon "moved the weapon over
to a new place" instead of covering it. Now `WeaponLoan.summon` / `ensure` ask `Ops.overlays(item)`: the bearer's own bound vessel (`VesselOverlay.isVesselOf`: the
`djinn_bound` and `djinn_serial` components of this Djinn and this bearer) is not displaced. It is kept whole in the loan record (`wloan.overlay`, saved with `saveOptional`, so
enchantments and every data component survive), in no inventory slot, the Djinn weapon takes its slot, and `VesselAccess.findBound` counts the overlaid vessel as on the body
(`VesselOverlay.read`). The vessel returns to the home slot unchanged when the Equip ends, on death (before the drops), logout, the login after a crash, a dimension change or
a switch to creative, and a scroll of the hotbar changes nothing (the weapon stays in its slot). A vessel put into the home slot during the Equip is overlaid too. Any other item
in the home slot is displaced exactly as before (section 2). If a record loses its home slot but still holds a vessel, the vessel is given back to the first free slot
(`WeaponLoan.release`), and a new Equip over such a stale overlay displaces it instead of deleting it. While overlaid, the vessel's own relic powers do not work (the Djinn
weapon is in the hand, as before); the Djinn's Equip does.

**Thirty-six weapons.** `Djinn` now has 36 rows, so there are 72 weapon items (`djinn_weapon_<id>` and `djinn_weapon_<id>_dark`) and 72 acts. The twenty-four new rows and
their acts are in `docs/djinn_roster_36.md`; their acts are data (`ActLibrary`: beam, cone, nova, dash, wall, pillars, chain, heal, pull, shield) run by
`DjinnWeaponActs.generic`, so they follow every rule of section 1 (paid in Magoi, cooldowns, foes the Djinn spells strike, the balance law, bounded particles). Engravings
for the new weapons are in `DjinnWeaponRules.engravings`. Item models and meshes of the new ids come from the asset tools; an item with no model draws as the missing-model
cube and nothing else is affected.

**The buff of the twelve.** Melee damage +1; act power x1.15, cooldown x0.9, reach x1.1 (table in `docs/djinn_roster_36.md`); the acts' effects last longer (Thunder Throw jumps one
more link, Judgement Step leaves you quick, the fire acts burn longer, Frost Surge freezes harder, Brand marks for +30% (+15% on players), Chain Lightning has five links,
Abyssal Reap and Radiant Burst heal more). The test bounds this widened are named in `DjinnWeaponRulesTest`: melee damage 15, power 15, reach 35, shortest cooldown 50 ticks.
