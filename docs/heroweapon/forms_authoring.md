# Writing a forms file that passes (for the weapon builders)

Your weapon's forms live in ONE file: `src/main/resources/data/nusmp/hero/forms/<itemId>.json` (for example `legendary_shield.json`, `vassal_sewing_set.json`; the file name is the
weapon's item id, see `HeroKind`). The same validator (`forms/FormsSchema`) runs on it in JUnit (`FormsDataTest`, which reads every shipped file), in the game at every data
reload (a bad form is skipped and logged, a bad file leaves the weapon with its base form only) and in the game's `hero.forms` self-test (which also resolves every item, tag
and skill in the real registries). Anything it reports as an error or a warning fails the build, so fix every line.

## How to check your file before you hand it over

1. `bash $S/hero/forms_check.sh /home/user/<your worktree>/src/main/resources/data/nusmp/hero/forms/<itemId>.json` (`S` is the scratchpad of the job). It runs the real validator,
   compares your skill ids with the string literals in `heroweapon/weapon/<pkg>/`, checks vanilla item and item-tag ids against the API dumps and `nusmp:` ids by text search,
   and prints the list as the Weapon Book will show it. Exit code 0 means clean. (Tensura ids and tags outside vanilla cannot be checked here: the game test does it.) The helper compiles the validator from the forms
   builder's worktree (`/home/user/hforms-wt`, or the folder in `HFORMS_SRC`), so it always has the latest rules.
2. `WT=<worktree> KEY=<key> bash $S/mega/jt.sh com.newuniverse.nusmp.heroweapon.forms.FormsDataTest` from the worktree folder (the test reads `src/main/resources/...` relative to it).
3. In the game (CI does this): `/multiverse hero form report` lists what the loader found; `/multiverse mega selftest hero.forms` checks the registries.

## The file

A short example (a real Holy Weapon file has 8 to 10 ordinary forms, so this one would draw the count warning; it shows the shapes):

```json
{
  "weapon": "legendary_shield",
  "forms": [
    { "id": "base", "name": "Small Shield", "desc": "The shape every Legendary Shield starts in.", "tier": 0, "mastery": 0,
      "stats": { "armor": 2.0 }, "skills": { "sneak": "shield.air_strike", "key1": "shield.prison" }, "passives": ["counter_ready"] },
    { "id": "leaf_shield", "name": "Leaf Shield", "desc": "Woven from what grows.", "tier": 1, "parents": ["base"],
      "requires": [ { "tag": "minecraft:leaves", "count": 32 }, { "item": "minecraft:glow_berries", "count": 8 } ], "mastery": 120,
      "stats": { "armor": 3.0, "movement_speed": 0.04 }, "abilityBonus": { "armor": 0.5 },
      "skills": { "sneak": "shield.air_strike", "key2": "shield.leaf_cover" } },
    { "id": "wrath_1", "name": "Shield of Wrath", "desc": "Anger given a face.", "tier": 1, "parents": ["base"], "mastery": 120,
      "curse": true, "sin": "wrath", "corruptionPerSecond": 3, "look": "curse", "stats": { "attack_damage": 4.0 } }
  ]
}
```

Keys left out take these defaults: `parents` and `requires` empty, `stats`, `abilityBonus`, `skills` empty, `passives` empty, `curse` false, `sin` none, `corruptionPerSecond` 0,
`look` "base" (for a curse form of a Holy Weapon "curse"); the base form may also leave out `tier` and `mastery` (both 0). Every other form must say its `tier`, `mastery`, `name` and
`desc`. Unknown keys are ignored, as the design says, so you may keep your own remarks (`_comment`, `comment`, `note`) or extra keys your weapon's code reads; only a key that
looks like a slip is a WARNING with a "did you mean" hint (`"require"`, `"requirements"`, `"skill"`, `"abilitybonus"` ...), because it would otherwise silently give a form no
requirements or no techniques. A key written twice in one object is a WARNING too (JSON keeps only the last).

## The checklist (every line is enforced)

**The file**
1. `"weapon"` is present and equals the item id (a copied file is caught). A missing key is a warning.
2. The FIRST form is `base`: tier 0, no parents, no requirements, `mastery` 0, not a curse form. Without a valid base form the whole file is rejected. The base form is never
   finished, so it can have no `abilityBonus` (nothing would ever give it): one written there is dropped with a WARNING, and you move it to a form that has mastery.
3. At most 40 forms. Ordinary (non-curse) forms INCLUDING the base: Holy Weapons 8 to 10, Seven Star and Vassal weapons 3 to 5 (a warning outside). At most 8 curse forms. A Holy
   Weapon needs at least one curse form for each sin it carries (Shield: wrath; Sword: greed and gluttony; Spear: lust; Bow: pride), or the curse system has nothing to unlock.

**Every form**
4. `id`: lower-case snake_case, 2 to 32 characters, starts with a letter, unique in the file.
5. `name` (at most 40 characters) and `desc` (at most 500) are required text.
6. `tier` 0 to 5, and it is the DEPTH: `tier = 1 + the highest tier among the parents` (the validator tells you the number it expects).
7. `parents`: 1 to 4 ids of other forms of the file (all must be learned first), no loops, no self. Everything hangs off `base`. A form whose parent was rejected is rejected too.
8. `mastery`: 40 to 400 points to finish the form (a point per hit, two per technique, one per block). Finishing pays the permanent `abilityBonus` once.
9. `requires`: one to four entries `{ "item": "ns:id", "count": n }` or `{ "tag": "ns:tag", "count": n }` (exactly one of the two), no repeats. Only a CURSE form may have none (its
   series is what opens it). `count` 4 to 64; above 64 (up to 256) only for a TOP form: tier 3 or deeper and nothing builds on it. Items are `minecraft:`, `tensura:` or `nusmp:`;
   tags are `minecraft:`, `c:` or `nusmp:`. Items that cannot be had in survival (barrier, bedrock, command blocks, spawners, spawn eggs ...) are refused. Every id must exist in
   the real registry (the game test resolves them; a tag must exist and hold at least one item). Tier 1 asks for common things, tier 2 for rarer, the top forms for rare ones.
10. `stats` and `abilityBonus` use only the keys `attack_damage`, `attack_speed`, `armor`, `armor_toughness`, `max_health`, `movement_speed` (a share: 0.05 is plus five percent),
    `knockback_resistance`, `reach`, `luck`. Budget of `stats` (also as a negative number down to the same size, except attack speed): armor 8, armor_toughness 4, max_health 12,
    attack_damage 14, attack_speed -3 to +1, movement_speed 0.2, knockback_resistance 0.6, reach 2, luck 3. `abilityBonus` may use a quarter of each (attack_speed -0.75 to +0.25).
    `attack_damage` and `attack_speed` of the active form go on the weapon item (main hand only); the others on the wielder; an `abilityBonus` is always on the wielder.
11. `skills`: slot words `use`, `sneak`, `key1`, `key2`, `key3` (lower case), at most one id each. A Shield (BLOCK style) and a Bow (DRAW style) have no `use` slot: their right
    click is the guard and the draw. Every id starts with the weapon's short name (`shield.`, `sewing_set.` ...) and must be registered by the weapon's logic (checked in the game and by
    the helper), and a form has at most one ultimate (checked in the game).
12. `passives`: up to 8 ids, lower-case snake_case, no repeats. Give each a name in your lang fragment as `hero.passive.<kind>.<id>` (and `.desc`), where `<kind>` is the enum name
    in lower case (`shield`, `sewing_set`); the Weapon Book shows the name, or the id in words when the key is missing.
13. Curse forms (`"curse": true`): `sin` is required, one of `wrath pride lust envy greed sloth gluttony` (lower case). A Holy Weapon keeps its canon sins (the validator lists them);
    a Seven Star or Vassal weapon may use any. `look`: "curse" for a Holy Weapon (it has a cursed mesh), "base" for the others (they have none); an ordinary form is always "base".
    `corruptionPerSecond` 0 to 100 (the curse builder's doc says what value a form of a series should have), and 0 on an ordinary form; `sin` on an ordinary form is an error.

## What the validator says, and what happens in the game

* `error: form 'x': ...`: the form is skipped, and so is every form listing it as a parent (the message says so). Fix the line.
* `error:` about the file (not an object, the first form is not `base`, the base form has errors, another weapon's file): the weapon keeps only its synthetic base form.
* `warning:` (counts, canon sins without a curse form, unknown or doubled keys, a missing `"weapon"`, an `abilityBonus` on the base form): the file is still used, but `FormsDataTest` and
  `hero.forms` fail, so it must be fixed before the merge.

## Advice that is not enforced

* A form should change how the weapon plays, not only its numbers: give it a different technique in at least one slot.
* The base form has the weapon's first two or three techniques; each tier adds or swaps one. The curse forms are the strongest and dearest (high `corruptionPerSecond`).
* Progression must be fair: tier 1 about 20 to 60 items of common material, the top form a real task. Prefer vanilla ids and tags the player already collects.
* The same item may appear in several forms (counts accumulate over all meals, and a stack of the item is taken only as far as the forms still to be learned can use it).
