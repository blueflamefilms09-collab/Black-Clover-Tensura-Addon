# Hero and vassal weapons in the enchanting menus (4.100.0)

Owner request: "fix players not being able to enchant the hero and vassal weapons with regular enchantment tables and the skills from Tensura that allow you to enchant them."

## Why it did not work (found in the code)
1. **The loan emptied the menu.** A hero weapon is only a projection of the bond. `HeroLoan.scanMenu` removed every hero weapon from every menu slot that is not the player's own inventory, so the enchanting table, the anvil and Tensura's Researcher screen (the Researcher, Great Sage and the other enchanting skills open `ResearcherEnchantingMenu`) lost the weapon within four ticks.
2. **The table refuses engraved items.** Vanilla `ItemStack.isEnchantable()` says no to a stack that already carries an enchantment, and every hero weapon carries its Tensura engravings from the moment it is made.
3. **A rebuilt projection forgot.** Put away and summoned again (or put back by the menu in another slot), the projection is made fresh, so its enchantments were gone.
4. **The four Legendary Weapons were never enchantable** (4.33.0 only did the 17 Seven Star and Vassal Weapons).

## What it does now
- The player's own projection may sit in the vanilla enchanting table, the anvil and the Researcher screen. While it does, the loan makes no second projection. Another player's weapon, or a bare stack, is still removed.
- `HeroWeaponEnchantableMixin` lets the table accept a hero weapon that already has engravings (the item's own `isEnchantable` decides).
- `HeroEnchants` keeps the enchantments of the live projection in the bond (`extra("enchants")`) whenever the loan would rebuild or end it; the next projection is made with them. Engravings are only ever raised (`WeaponEngravings` unchanged); the engraving lock is unchanged.
- The Legendary Sword and Spear join the melee tags, the Shield the `weapon` tag, the Bow gets `enchantable/bow` (data/minecraft/tags/item/enchantable). Tensura's engravings follow, because its tag `handheld_enchantable` includes those vanilla tags.
- A logout, a death, a dismiss or a release empties the menu of the weapon (nothing is dropped; hero item entities are cancelled as before).

## Settings (server, `nusmp-heroweapon-server.toml`, `[heroweapon]`)
- `enchantMenus` (default true): menus accept the weapon, the table accepts engraved stacks, enchantments are kept. Off = the old behaviour exactly (weapon removed from every menu).
- `enchantHoly` (default true): the Legendary Weapons are enchantable too. Off = only the 17 as in 4.33.0. The tag files list the Legendary Weapons either way; this setting is what the table and the menus obey.

## NEW / REPLACED / KEPT
- NEW: `HeroEnchants`, `HeroWeaponEnchantableMixin`, the two settings, `HeroEnchantMenusTest`, a block in the `hero.bond` self-test, `enchantable/bow.json`.
- REPLACED: `VassalEnchantTagsTest` (now also checks the Legendary ids). The 4.33.0 tags are kept and extended, not removed.
- KEPT: all forms, skills, engravings, the engraving lock, the loan rules for everything that is not an enchanting menu.

## Worst-case cost (rule 11)
No tick handler, entity, packet or new state beyond one small tag in the bond. While a player has such a menu open with the weapon in it: a few slot reads and one comparison per four-tick beat; one encode only when the enchantments changed; one decode per beat to compare. Nothing runs for players without the weapon in a menu. The mixin is one instanceof per `isEnchantable` call.

## Not checked
Not tried at a real table, anvil or Researcher screen by a person (the `hero.bond` self-test drives a real `EnchantmentMenu` on a stand-in player). Tensura's own screens were not opened. Enchantments applied by an operator command while the weapon is out are kept at the next put-away, not before.
