# The relic format (what every relic in the mod was built from)

Two layers: the **card** (what you give me or another AI to describe a relic) and the **definition** (the Java the mod builds it from). Give the card; the definition is what I write from it.

## 1. The card (copy, fill in, send one per relic, or a list)

```
NAME:        Ring of Sparks
ID:          ring_of_sparks                (optional; I make it from the name: lowercase, underscores)
RARITY:      Rare                          (Normal / Special / Rare / Unique / Legend / God / Genesis; D&D: common, uncommon, rare, very rare, legendary)
SLOT:        HANDS                         (MAIN_HAND, HEAD, CHEST, FEET, NECK, BACK, BELT, HANDS, CHARM, CONSUMABLE)
ATTUNEMENT:  yes / no
TYPE:        gauntlets                     (sword, rapier, staff, wand, hammer, axe, bow, crossbow, shield, tome, plate, robe, leather, coat, cloak, cape, mantle, hood, crown, circlet, hat, gloves, gauntlets, belt, amulet, pendant, locket, charm, potion, orb, boots, sandals, greaves)
LOOKS:       what it looks like (colours, materials, shapes); an image helps; "3D" if it should be a 3D model in hand
PASSIVES:    what it does just by being worn/held (stats, resistances, effects, a skill it grants)
ABILITIES:   each: name, how it is triggered (use / sneak+use / the worn-relic key), cost (% of magicules), cooldown (or uses per day / rest), duration, what it does, save/DC if any
MELEE:       damage / speed (weapons only)     ARMOUR: defence / toughness (armour only)
ENGRAVINGS:  Tensura engravings that fit it (severance, barrier_piercing, holy_weapon, magic_interference, ...)  (weapons)
FULL TEXT:   the card's original rules text, word for word (I translate tabletop rules: days = Minecraft days, rounds = 6 s, 5 ft = 1 block)
NOTES:       pairs (left/right), curses, bonds, set bonuses, anything unusual
```

## 2. The definition (what I write from the card)

```java
out.accept(RelicDef.of("ring_of_sparks", "Ring of Sparks", Grade.RARE, RelicSlot.HANDS)
    .attune()                                   // requires attunement
    .model3d()                                  // a real 3D model in hand (weapons)
    .look(Look.GAUNTLETS)                       // worn silhouette family (Look enum)
    .palette(0x6A5ACD, 0xFFD700, 0x80E0FF)      // main, trim, glow colours
    .desc("Lightning answers your hands.")      // short tooltip text
    .passive(Passive.attr("minecraft:generic.movement_speed", 0.05, Operation.ADD_MULTIPLIED_BASE))
    .passive(Passive.resist("minecraft:lightning_bolt", 0.5))        // damage type / tag, fraction
    .passive(Passive.effect("minecraft:night_vision", 0))             // standing effect
    .passive(Passive.skill("tensura:magic_sense"))                    // grants a skill while worn
    .passive(Passive.flag("w9_sparks"))                               // a flag my event code reads
    .act(Act.of("arc", "Arc", Wave9Acts::arc)                         // key, display name, body
        .cooldown(400).mana(5))                                        // ticks, % of magicules
    .act(Act.of("storm", "Storm", Wave9Acts::storm).needsSneak().usesPerDay(1))
    .melee(6.0, 1.3)                            // damage, speed (weapons)
    .armor(4, 1.0)                              // defence, toughness (armour)
    .engrave("severance", "magic_weapon")       // Tensura engravings (weapons)
    .bow() / .crossbow()                        // ranged kinds
    .pair("Left Glove"));                       // right half of a pair; the left is made for you
```
Act options: `.cooldown(ticks)`, `.mana(percent)`, `.epPercent(p)`, `.usesPerDay(n)`, `.needsSneak()`, `.hold(ticks)`, `.toggle()`.
First act runs on use, the second on sneak + use, the rest on the worn-relic key.

## 3. What a relic gets automatically
Item, creative tab entry, tooltip, Curios slot tag (rerun `python3 tools/relics/gen_curios_tags.py` after a wave), worn model (rerun `python3 tools/relics/gen_worn_all.py`), loot weight by grade, Tensura EP lift by grade, Djinn vessel (Legend and above), 3D in-hand model for weapons (built in `tools/gen_weapon_models.py`, size class in `tools/relics/weapon_classes.json`), Better Combat profile, Tensura engravings.

## 4. Grades
Normal 1 · Special 2 · Rare 3 · Unique 4 · Legend 5 · God 6 · Genesis 7 (God and Genesis are set by hand). See `docs/relics_of_many_worlds.md` section 1.

Examples to copy from: `src/main/java/com/newuniverse/nusmp/relics/wave/Wave20.java` (weapons), `Wave2*.java` (charms, boots, cloaks), `Wave30*.java` (heads, hands, necklaces, consumables).
