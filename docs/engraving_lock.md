# Engraving lock (4.33.0)

Owner request: the engravings on the mod's weapons must not be removable, and they must stay on the weapon they were put on.

## What it does
A stack of one of this mod's items that has been engraved (marker `NUEngraved` from `WeaponEngravings`, or `NUFranchise` from `FranchiseEngravings`) is **locked**:

| Way to lose or move an engraving | Result |
|---|---|
| Grindstone, engraved weapon in either slot | no output (nothing is stripped) |
| Anvil, engraved weapon in the right slot (its enchantments would be merged onto another item or a book) | no output |
| Anything else that strips enchantments (another mod, a command) | the once-a-second pass restores every table engraving of that weapon |

Restoring only raises a missing or lowered engraving to the level of the weapon's own table; it never lowers or removes anything, so the Relic Forge's extra engravings and higher levels are not touched.

## Settings
Server config `engravingLock` (default `true`). `false` gives the old behaviour exactly.

## NEW / REPLACED / KEPT
- NEW: `item/EngravingLock`, `item/EngravingLockSelfTests` (self-test key `engraving.lock`), the setting, `EngravingLockSourceScanTest`.
- CHANGED (appended behaviour only): `WeaponEngravings.engrave` and `FranchiseEngravings.engrave` no longer stop at the marker when the lock is on; they run their table again and report whether they put something back.
- REPLACED: nothing. KEPT: every table, every marker, the once-only first engraving.

## Worst-case cost (rule 11)
No new tick handler, entity, packet or state. The anvil and grindstone events fire only while a player has that screen open and read two stacks' custom data. The restore pass reuses the existing once-a-second inventory pass: per engraved weapon at most its table (about 1 to 5 enchantment lookups); a player inventory is at most 41 stacks.

## Not checked
Nothing was tried at a real anvil or grindstone. Items engraved by other classes (`RelicEngravings`, `DjinnWeaponEngrave`, `ArmorEngravings`) are locked against the anvil and grindstone (they share the `NUEngraved` marker) but do not get the restore pass. Repairing an engraved item at a grindstone is also blocked (the grindstone cannot tell repair from stripping).
