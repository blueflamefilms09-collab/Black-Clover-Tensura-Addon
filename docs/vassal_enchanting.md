# Vassal weapons can be enchanted (4.33.0)

Owner request: "allow vassal weapons to be enchanted".

## What it does
The 17 "Vassal ..." weapons (every hero weapon whose item id starts with `vassal_`) accept enchantments:
- enchanting table (enchantability 15, like iron-tier gear),
- enchanted books on the anvil,
- Sharpness, Smite, Bane of Arthropods, Knockback, Fire Aspect, Looting and Sweeping Edge (through the vanilla item tags `enchantable/weapon`, `sharp_weapon`, `sword`, `fire_aspect`).

Mending and Unbreaking do not apply (the weapons are already unbreakable). The Seven Star and Holy weapons are unchanged.
Tensura engravings are a separate mechanism and are not touched; the engraving lock still applies to the weapon's own engravings.

## Settings
None: the tags are data and the two item methods are fixed. To undo: delete the four files under `data/minecraft/tags/item/enchantable/` and the two overrides in `HeroWeaponItem`.

## NEW / REPLACED / KEPT
NEW: enchantability for 17 items. REPLACED: nothing. KEPT: all attributes, forms, skills.

## Worst-case cost
No tick, packet, entity or state. Vanilla enchanting code only.

## Not checked
Not tried at a real table or anvil.
