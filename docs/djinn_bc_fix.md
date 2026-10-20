# Djinn binding with Better Combat (release 1.101.0)

Owner report: "fix not being able to add djinns to weapons with better combat". With Better Combat installed a Djinn Seal could not be bound to a weapon vessel.
This page is what was found (read from the real Better Combat 2.4.0 source, `zsoltmolnarrr/bettercombat`, MC 1.21.1), what was changed, and what could not be run.

## How a Djinn is bound to a weapon

There is no altar and no anvil. The gesture is `DjinnSealItem.use`: a **Djinn Seal in one hand and the vessel (a Legend-grade relic flagged `vessel()`, attuned to you) in the other hand**,
then right-click (`DjinnBinding.attempt`, the price in `BindingRules`: full Magoi bar, a quarter of the EP, three crystals, a trial). The vessel is judged by its **item** through the
relic catalog (`Relics.defOf` -> `RelicDef.isVessel()` and `grade().djinnCapable()`) plus two components of ours (`djinn_bound`, `relic_attuned`); no other component is read. The bearer finds
the vessel again with `VesselAccess.findBound` (a hand, armour or a Curios slot, or the weapon the Djinn weapon overlays).

## The real defects (with file:line)

**D1. The Seal read "the other hand" through the game, and Better Combat makes the game lie about the off hand.**
Better Combat `PlayerEntityMixin.getEquippedStack_Pre` (net/bettercombat/mixin/player/PlayerEntityMixin.java:128-153) is a head inject on `getEquippedStack` (Mojang `Player.getItemBySlot`):
when the slot is OFFHAND and the main-hand stack **or** the off-hand stack has a weapon profile with `two_handed`, it returns EMPTY. `LivingEntity.getItemInHand(OFF_HAND)`, `getOffhandItem()`
and the client's `startUseItem` all read through that method. The raw inventory (`Inventory.offhand.get(0)`) is not touched. `two_handed` is resolved through the parent preset
(`WeaponAttributesHelper.override`: `b.two_handed() != null ? b : a`), so every weapon whose preset is two-handed counts even when our file writes no `two_handed`
(katana, spear, claymore, double_axe, heavy_axe, glaive, halberd, hammer, scythe, staff, anchor, ...). Of the 46 relic items that are flagged `vessel()` and have a Better Combat file, 23 are two-handed
(`DjinnBetterCombatScanTest` computes this from the sources: Noon Cleaver, Calamity Scythe, the Black Halberd, Blackrazor, Staff of the Magi, Gungnir, Kusanagi, Dragon Slayer, the hammers, ...).
So with a Seal in the main hand and such a weapon in the off hand, `DjinnSealItem.use` got EMPTY from `player.getItemInHand(OFF_HAND)` and answered the vague
"Hold a Metal Vessel in your other hand" (old `DjinnSealItem.java:39`). With the Seal in the **off** hand and a two-handed weapon in the main hand the Seal is never used at all:
the client skips an empty hand and the server reads the same lie, so no `use` call reaches the Seal. Without Better Combat the two hands are always visible, which is why only the mod broke it and only for some weapons.
The same read was in `DarkRukhShardItem.java:28`, `DarkDjinn.java:140` (Holy Water on the dark vessel), `UnbindingSealItem.java:31` and `DjinnCommand.java:197` (the admin `grant ... bind`).

**D2. The same lie hid a vessel held in the off hand from the Djinn engine.**
`VesselAccess.equipped` (old `VesselAccess.java:59`, `:88`) and `Relics.scan` (`Relics.java:194`) use `getOffhandItem()`. When a two-handed weapon is in the **main** hand, a vessel in the off hand
(a shield, a held relic) is "not on the body": `DjinnEngine` (once a second, `DjinnEngine.java:154`) found no vessel and ended the Equip ("Your vessel left you"). The Djinn weapons themselves are
two-handed under Better Combat when their kind is a spear/glaive/staff/greatsword/axe/scythe, so a Half Equip put a two-handed weapon in the main hand and a vessel in the off hand vanished at that moment.
`VesselAccess.equipped` now also reads the raw inventory. (`Relics.scan` is the relic core, other relics in the off hand are Better Combat's design and were not touched.)

**D3. A weapon with a Better Combat profile eats the right-click for a short time.**
`MinecraftClientInject.pre_doItemUse` (MinecraftClientInject.java:126-135) cancels `doItemUse` while a swing is winding up or the attack cooldown is below `1 - upswingRate`, and `startUpswing`
sets the vanilla `itemUseCooldown` to the attack cooldown (:252). Right after a swing, a right-click of the Seal is dropped. This is transient, but it is one more reason the ritual must not depend only on a right-click.

**D4. Thirteen Djinn weapon kinds had no Better Combat file (the other direction).**
`tools/bettercombat/inventory.py` printed `PROBLEM: unknown djinn weapon kind: ...` for the 1.93.0 roster kinds (claymore, harpoon, daggers, rapier, anchor, fans, pincers, sabre, spear, cleaver, halberd,
longsword, mace) and its row regex `Djinn\.([A-Z]+)` skipped `GLASYA_LABOLAS`. Those 30 weapon items (15 Djinn, light and dark) swung with the vanilla attack. Nothing broke, but they were not Better Combat weapons like the other 38.

**Checked and fine (not causes).**
* Better Combat does not add or change a data component on the stack: `WeaponRegistry.getAttributes` reads `bettercombat:preset_id` only if something put it there, else the item id. Nothing compares stacks with components in the binding.
* `Relics.defOf` is by item (`RelicBearer` or the adopted-item map), so it cannot depend on Better Combat's data path.
* Our `weapon_attributes` files use `bettercombat:<preset>` parents, no `nusmp:` namespace, and no weapon id appears twice.
* The overlay (`VesselOverlay`, `WeaponLoan`) keys on the `djinn_bound`/`djinn_serial` components and the item, not on a stack equality that Better Combat could change; the Djinn weapon never rewrites its own components while held, so Better Combat's "stack changed, cancel the swing" check (`MinecraftClientInject.cancelSwingIfNeeded`) does not trigger.
* The Djinn weapons' `use` is main-hand only and already returns pass in the off hand; Better Combat's left-click swing does not call `use`.

## What changed

* **The other hand is read from the raw inventory** (`VesselAccess.rawMain/rawOff/rawIn/rawOtherThan`) in the Seal, the Dark Rukh Shard, Holy Water, the Unbinding Seal, the admin grant, and in `VesselAccess.equipped`.
* **Every gesture binds** (`BindGesture.Gesture`):
  1. the Seal in the main hand, the vessel in the off hand (works now behind a two-handed weapon);
  2. the Seal in the off hand, the vessel in the main hand (works unless the main weapon is two-handed: Better Combat then hides the off hand from the game, nothing of ours can see the click; the three below cover it);
  3. **inventory click**: in any inventory screen, the Seal on the cursor and a right-click on the relic in your own inventory (or the vessel on the cursor and a right-click on the Seal): `DjinnSealItem.overrideStackedOnOther` / `overrideOtherStackedOnMe`. Only relics are taken over; every other click stays vanilla. A vessel in a chest is refused with a reason;
  4. **`/multiverse djinn bind [main|off|slot <0-40>] [djinn]`** and the short alias **`/djinn bind ...`** (the whole `djinn` tree is registered at the root too). With no argument it finds the Seal (hand, then pack) and the first legal vessel (hands, then the inventory). Slots 0-35 are the pack, 36-39 armour, 40 the off hand;
  5. **the Djinn key (J)** while no Djinn is bound and a Seal is carried starts the same ritual.
  Gestures 3-5 have server switches in `serverconfig/nusmp-djinn-bind-server.toml` (`commandBind`, `guiBind`), **default ON**; right-clicking the Seal never depends on a switch. All of them run the same ritual and pay the same price (full bar, a quarter of the EP, three crystals, the trial).
* **Refusals say why**, on the action bar and in chat (`BindGesture.Why`, `message.nusmp.djinn_bind.*`): no Seal, no vessel found, not a relic, not a Metal Vessel, below Legend grade, already holds a Djinn, not attuned to you, not in your inventory, this way is switched off.
  The old Seal-specific messages (already owned, bond limits, bar not full, crystals) are unchanged.
* **The vessel is judged by item + the ritual's two components only** (`BindGesture.look`, `VesselAccess.look`); extra components (Better Combat's preset id, attribute modifiers, other mods' data) change nothing.
* **Better Combat files for the 1.93.0 Djinn kinds** (`profiles.py` `DJINN_KIND_CLASS.update(...)`, regex fixed, `manifest.json` regenerated, 30 files written): claymore->greatsword, harpoon->trident, daggers/fans->dagger, rapier, anchor->hammer, pincers->fist,
  sabre, spear, cleaver, halberd->glaive, longsword, mace->club. Sitri's and Barbatos' bows stay excluded (ranged).
* `DjinnEngine.sync` returns when the player has no connection (a self-test stand-in), nothing changes for real players.
* The Seal's tooltip names the no-hand ways.

### What was deliberately not changed
The `two_handed` profiles are **kept** (rule 2, and they are what makes a greatsword swing with both arms). The fix is that the ritual no longer depends on the off hand, not that vessels lost their second hand.
A Seal used from the off hand behind a two-handed main weapon remains impossible (Better Combat's design: the off hand is empty for the game); the inventory click, the command and the key replace that gesture.

## NEW / REPLACED / KEPT

| | what |
|---|---|
| NEW | `BindGesture`, `BcHands` (pure), `DjinnBindConfig` + `nusmp-djinn-bind-server.toml`, `DjinnBindSelfTest` (`djinn.bc_bind`), `/multiverse djinn bind` and `/djinn ...`, the inventory-click and key gestures, 30 Djinn weapon Better Combat files, `tools/lang/djinn_bc_fix.json`, `BindGestureTest`, `DjinnBetterCombatScanTest` |
| REPLACED | the way "the other hand" is read in `DjinnSealItem`, `DarkRukhShardItem`, `DarkDjinn` (Holy Water), `UnbindingSealItem`, `DjinnCommand` (grant), `VesselAccess.otherHand/equipped`; the vague `NO_VESSEL` refusal (now the exact reason) |
| KEPT | the price and trial (`BindingRules`), `DjinnBinding.attempt(p, seal, vessel)` (still there, delegates), every `two_handed` profile, every existing weapon file, the manifest entries, the enum order of `Refusal` (nothing removed), the overlay and loan |

## Tests

* `BindGestureTest` (11): the failing path modelled (`BcHands`: the old read returns EMPTY for each two-handed vessel in the off hand, the raw read finds it), extra components on the vessel change nothing (200 junk components), every refusal reason, every gesture accepts a legal vessel, the switches.
* `DjinnBetterCombatScanTest` (6): the vessel weapons that are two-handed (from the sources and the preset list), the binding code reads no `getOffhandItem()` / `getItemInHand(OFF_HAND)`, the five gestures are wired and the switches default on, every melee Djinn weapon has a Better Combat file, the lang fragment matches the fallback texts. Mutation check: putting `player.getItemInHand(OFF_HAND)` back in `DjinnSealItem` fails it.
* Self-test `djinn.bc_bind` (server smoke test): a stand-in whose off hand answers EMPTY while a two-handed weapon is held (Better Combat's rule) binds a Seal to a Legend weapon vessel with a Better Combat file through each of the six paths (Seal main, Seal off, inventory click, command by slot, command off hand, key), checks the stamp, `findBound`, the Djinn weapon overlay and its return, then undoes everything; an iron sword is refused.

## Could not be verified

* No Better Combat jar and no game here: the behaviour above is read from Better Combat's source (2.4.0, the version the repo's extractor used), not observed. The stand-in in the self-test models the rule, it is not the mod.
* The self-test and the Minecraft-facing code (`DjinnBinding.fromInventory`, `overrideStackedOnOther`, the command tree, the key path) were type-checked against the API stubs only; the JUnit shim cannot load Minecraft classes. `djinn.bc_bind` skips itself (and says so) when the stand-in cannot answer the bond or stage lookups.
* Whether `overrideStackedOnOther` fires on every client menu (the creative inventory tab handles clicks on its own and was not covered) was not seen.
* A hand-held Seal in the off hand behind a two-handed weapon cannot be fixed from our side, see above.
* `tools/bettercombat/gen_attributes.py --check` still prints the older `PROBLEM: tools/relics/weapon_classes.json and assets/nusmp/weapon_classes.json disagree`: the two copies of the class table differ (not touched here), so `--check` exits 1 even though every weapon file is in sync.
* The new Djinn weapon files use Better Combat's presets with the same range rule as the older ones; how those 30 weapons feel in game was not seen.
