# Better Combat support (1.38.0)

Why this exists: [Better Combat](https://modrinth.com/mod/better-combat) replaces the vanilla "click, one hitbox in front of you" swing with combo
swings, arc hitboxes and dual wielding. It is data driven: a weapon is known to it only if a JSON file exists for its registry id. Until 1.38.0
the mod shipped 12 hand-made files; the other ~100 melee weapons ignored the combat mod. Now **every melee weapon ships a profile**, and the
classes that have a recognisable fighting style (greatsword, katana, rapier, axes, whips, twin blades) ship **their own combos and animations**.

Better Combat stays an optional dependency (`neoforge.mods.toml` lists it `type="optional"`). Without it none of these files is read: data under
`data/<ns>/weapon_attributes` and `assets/<ns>/player_animations` is plain resource data that nothing else loads, so nothing changes and nothing can
fail. Nothing in this feature is Java, and nothing is registered.

## How it works

1. `tools/bettercombat/inventory.py` builds the list of melee weapons **from the sources**, so the list cannot rot:
   - the `weapons` table of `assets/nusmp/weapon_classes.json` (and the `tools/relics` copy must agree),
   - `NUItems` (`weapon(...)`, `MagicToolSword`, `DemonSlayerSwordItem`, `OtherworldTridentItem`),
   - every `RelicDef.of(...)` block that calls `.melee(...)`, and the `<id>_left` half of every `.pair(...)`,
   - the 24 Djinn weapons (`djinn_weapon_<name>` and `_dark`, kind from `DjinnWeaponRules`).
   It writes `tools/bettercombat/manifest.json`: id -> weapon class -> chosen profile, plus the excluded ids and why.
2. `tools/bettercombat/profiles.py` holds the per-class numbers (parent, `range_bonus`, `two_handed`, combo) and the hand-written profiles.
3. `tools/bettercombat/gen_attributes.py` writes `data/nusmp/weapon_attributes/<id>.json` for every manifest weapon. Deterministic, idempotent.
   `--check` exits 1 when a weapon has no file or a file differs; `--list` prints the weapons per class.
4. `tools/bettercombat/gen_animations.py` writes `assets/nusmp/player_animations/<name>.json` from small keyframe tables (angles in degrees in the
   table, radians in the file). `--check` and `--list` as above.
5. `WeaponAttributesTest` (JUnit, pure, hand-made JSON reader) runs in Gradle: every melee weapon (manifest, class table, relic defs) has a file; every
   file parses, `parent` starts with `bettercombat:`, `range_bonus` is within -1..3, attack numbers are sane; every `nusmp:<x>` animation has its file;
   every animation has strictly ascending ticks and `beginTick <= endTick <= stopTick`.

### Adding a weapon (the "per-game fix for new ones")

A new melee weapon fails the build until it has a file. The failing message says so; the fix is always:

```
python tools/bettercombat/inventory.py        # refresh the manifest (only needed when the weapon is new)
python tools/bettercombat/gen_attributes.py   # write the file(s)
```

and commit them. Give the weapon a class in `weapon_classes.json` first (that is also what sizes its 3D model). A new weapon *class* needs an entry in
`profiles.py` `CLASS_PROFILES`. A weapon that should differ from its class gets a name in `SPECIAL_PROFILES` (and a line in
`inventory.profile_name`). A new animation is one table in `gen_animations.py`.

## Schema subset used (from memory, not loadable here)

Weapon file (`data/nusmp/weapon_attributes/<id>.json`):

```json
{ "parent": "bettercombat:claymore",
  "attributes": {
    "range_bonus": 1.0,
    "two_handed": true,
    "attacks": [ { "hitbox": "HORIZONTAL_PLANE", "damage_multiplier": 1.0, "angle": 160, "upswing": 0.55,
                   "animation": "nusmp:greatsword_sweep" } ] } }
```

- `parent`: only Better Combat's built-in names are used (sword, claymore, dagger, rapier, katana, axe, double_axe, mace, hammer, scythe, spear,
  glaive, staff, trident, fist, wand; `pike` and `halberd` are known but unused). When unsure the generator falls back to sword/claymore/axe/spear/staff.
- `range_bonus`: matches the weapon's real in-hand size (the 1.30-1.34 size classes). `two_handed` is written only for two-handed classes.
- `attacks` is written **only** where we ship our own combo; otherwise the parent's combo (and its animations) is inherited. A child's `attacks` replaces the
  parent's list.
- Not written because their value spaces (animation / sound ids) are not certain offline: `pose`, `off_hand_pose`, `swing_sound`, `category`, `attack_range`.
  Better Combat dual wields two one-handed weapons by itself, the twin pairs only need a combo that looks right in either hand.
- Extra keys are not used. The existing 12 files keep their `parent` and `range_bonus` exactly (rule: keep the old, add the new).

Animation file (`assets/nusmp/player_animations/<name>.json`, the Player Animator "emote" JSON that Better Combat 1.21 loads from `player_animations`):

```json
{ "format_version": "1.0.0", "name": "x", "author": "Multiverse of Anime",
  "emote": { "isLoop": false, "returnTick": 0, "beginTick": 0, "endTick": 12, "stopTick": 16, "degrees": false,
    "moves": [ { "tick": 0, "easing": "EASEOUTSINE", "turn": 0,
                 "rightArm": { "pitch": -1.0, "yaw": 0.0, "roll": 0.0 }, "torso": { ... } } ] } }
```

Parts used: `head, torso, rightArm, leftArm, rightLeg, leftLeg`; per part `x y z pitch yaw roll` (offsets in pixels, angles in radians because `degrees` is
false). Easings used: LINEAR, EASEINSINE/OUTSINE/INOUTSINE, EASEINQUAD/OUTQUAD, EASEINCUBIC/OUTCUBIC and their INOUT forms. Pitch negative swings a limb
forward/up, yaw positive swings it to the body's left (Minecraft model rotations); these signs are the most likely thing to need a flip once seen in game.
Each animation is a standalone file: if one fails to parse Better Combat logs it and skips it, nothing else is affected.

## Profile table (class -> parent, range, hands, combo)

`range_bonus` follows the in-hand size; `two` is `two_handed`. "parent's combo" = no `attacks` written, the parent's animations are kept.
The count is the number of weapons of the class in the manifest (116 in all; Djinn weapons count under their kind's class).

| class | weapons | parent | range_bonus | two | combo (animation names are `nusmp:`) |
|---|---|---|---|---|---|
| dagger | 7 | dagger | -0.25 | no | rapier_lunge(x0.8) > rapier_lunge(x0.8) > katana_rise(x1.1) |
| shortsword | 14 | sword | 0.00 | no | parent's combo |
| longsword | 13 | sword | +0.25 | no | parent's combo |
| sabre | 4 | sword | +0.10 | no | parent's combo |
| rapier | 5 | rapier | +0.25 | no | rapier_lunge(x0.75) > rapier_lunge(x0.75) > rapier_lunge(x1.25) |
| katana | 5 | katana | +0.35 | no | katana_draw > katana_rise > katana_thrust(x1.15) |
| dadao | 1 | katana | +0.60 | no | katana_draw > katana_rise > greatsword_slam(x1.3) |
| greatsword | 21 | claymore | +1.00 (buster_sword, thundercleave, stegosaurus_greatsword, soul_shatter_blade +1.25; giant_jawblade +1.5) | yes | greatsword_sweep > greatsword_sweep_back > greatsword_slam(x1.35, wider arc) |
| handaxe | 5 | axe | 0.00 | no | axe_chop > axe_chop |
| cleaver | 1 | axe | -0.10 | no | axe_chop > axe_chop |
| battleaxe | 5 | axe | +0.75 | yes | axe_chop > axe_chop > axe_chop(x1.25) |
| greataxe | 4 | double_axe | +1.25 | yes | axe_chop > greatsword_sweep > greatsword_slam(x1.35) |
| crescent | 1 | scythe | +0.75 | yes | parent's combo |
| scythe | 2 | scythe | +1.00 | yes | parent's combo |
| hammer | 1 | hammer | +0.50 | yes | greatsword_sweep > greatsword_slam(x1.35) |
| club | 3 | mace | +0.25 | no | parent's combo |
| spear | 3 | spear | +1.00 | no | parent's combo |
| trident | 3 | trident | +1.00 | no | parent's combo |
| glaive | 7 | glaive | +1.25 | yes | parent's combo |
| staff | 5 | staff | +0.75 | yes | parent's combo |
| wand | 1 | wand | 0.00 | no | parent's combo |
| whip | 1 | sword | +1.50 | no | whip_lash > whip_lash (forward box) |
| coilwhip | 2 | sword | +1.25 | no | whip_lash > whip_lash (forward box) |
| fist | 2 | fist | 0.00 | no | parent's combo (Marchosias' claws, both lights) |

Decisions that were open in the request:
- **Battleaxes are two-handed, handaxes and cleavers one-handed** (head width 0.75+ blocks against 0.5, and the battleaxes are the legendary ones).
- **Spears and tridents are one-handed**, glaives, staves and scythes two-handed.
- **No custom animation exists for spear, trident, glaive, staff, wand, sabre, club, scythe, shortsword, longsword**: they keep the parent's animations
  (stab / slash presets Better Combat already ships), the profile only fixes their reach and hands.
- **Dagger** borrows the rapier lunge for its stabs and the katana rise for its short upswing finisher (no dagger-only animation was authored).

### Named weapons (hand-written profiles)

| weapon | parent | range | hands | combo |
|---|---|---|---|---|
| `demon_slayer_sword` (Genesis Demon-Slayer) | sword (kept) | +0.75 (kept) | one-handed (kept) | sweep (170) > sweep_back (170) > slam (x1.35, 150 degrees) |
| `elsdocia`, `elsdocia_legacy` (alias, model file only) | claymore | +1.5 | two-handed | sweep > sweep_back > wide sweep (200, x1.1) > slam (x1.5, 160) |
| `whirling_blades`, `_left` | sword | +0.1 | one-handed | twin_cross > twin_cross (x1.1) |
| `twin_hook_swords`, `_left`; `twin_swords_of_deflection`, `_left` | sword | 0.0 | one-handed | twin_cross > twin_cross (x1.1) |
| `djinn_weapon_<name>` and `_dark` (11 Djinn, 22 items) | by kind | class range minus the weapon's own reach modifier | by class | the class combo with the last attack x1.5 |

The Genesis keeps the owner's one-handed `sword` parent and 0.75 range, and only gains the combo, because the old file is the Black Clover original
(its range and hands are the owner's choice; rule 2). The Djinn weapons already carry their reach as a vanilla `minecraft:entity_interaction_range`
modifier (`DjinnWeaponItem.props`); Better Combat's `range_bonus` is added on top of the player's interaction range, so only the part of the class size
that the modifier does not cover is added (a lance with +1.0 reach gets 0.0). Sitri's bow (`djinn_weapon_sitri`, `_dark`) is ranged and excluded.

### The 12 files that existed before

Parent and `range_bonus` unchanged: demon_destroyer_sword (sword, 0.5), demon_dweller_sword (sword, 0.25), demon_slasher_katana (sword, 0.25),
demon_slayer_sword (sword, 0.75), licht_destroyer_sword (sword, 0.5), licht_dweller_sword (sword, 0.25), magic_tool_spear (spear), magic_tool_sword (sword),
miasma_infused_katana (katana), rimeheart_runeblade (sword), severing_greatsword (claymore), spell_forged_rapier (rapier). The ones the owner gave a one-handed
`sword` parent (the demon and Licht swords, Rimeheart; README 0.43 says the Rimeheart is one-handed so its off-hand bonus works with dual wielding) get **no**
`two_handed` and **no** custom combo. The Rimeheart had no range and gains 0.5 (it is a greatsword-sized model, like the Demon-Destroyer). The ones whose parent
matches their class (severing_greatsword claymore, miasma katana, rapier, the two magic tools) gain the class range and combo.

## The animations

All ten are one- or two-arm poses with torso twist and a little leg shift; each is wind-up, strike, follow-through, recover. Ticks are the file's own
(Better Combat stretches them over the weapon's attack time; the strike lands at the `upswing` fraction of the attack).

| name | used by | what it shows |
|---|---|---|
| `greatsword_slam` | greatsword finisher, greataxe, hammer, dadao | both arms up behind the head, torso leans back, then folds forward on the strike, front leg steps in |
| `greatsword_sweep` | greatsword, greataxe, hammer | wide horizontal cut right to left, shoulders and head turn through the swing |
| `greatsword_sweep_back` | greatsword combo 2 | the mirror, left to right, so the combo flows back the way it came |
| `katana_draw` | katana, dadao | low crouched draw from the hip, blade cut across and up to the left |
| `katana_rise` | katana, dadao, dagger finisher | starts low by the knee, climbs through the target, torso leans back |
| `katana_thrust` | katana finisher | pull to the hip, step in, drive the point straight out |
| `rapier_lunge` | rapier (3 thrusts, the third x1.25), dagger stabs | one-handed retract, deep step, point out; off hand swings back for balance |
| `axe_chop` | all axes | one-handed overhead chop, torso folds with the blow |
| `twin_cross` | the six twin-pair items | both arms start wide and scissor across each other through the target |
| `whip_lash` | whips | arm cocked back and up, fast forward snap, torso follows |

Authoring reminders: the pair halves use the same `twin_cross` for both hands (Better Combat mirrors an off-hand attack); `greatsword_sweep` and
`_back` exist as two files because a per-attack mirror flag is not something this schema subset relies on.

## Combat compatibility (the Java side, audited, no change needed)

Better Combat does not call `Item.use` and does not run the vanilla client attack. Its client sends an attack packet and the server then calls the vanilla
`Player.attack(target)` for each target in the hitbox (for an off-hand attack in dual wielding it temporarily swaps the hands). This description of Better Combat's
server handler is from memory, not from its source. So everything keyed to the
vanilla attack still runs. What was read:

- **Attribute modifiers are the real ones.** `MagicWeaponItem` (a `SwordItem`) uses `SwordItem.createAttributes` (attack damage, attack speed);
  `RelicWeaponItem` (a `SwordItem`) builds `ATTACK_DAMAGE` / `ATTACK_SPEED` modifiers from the def with vanilla's `base_attack_damage` / `base_attack_speed` ids;
  `DjinnWeaponItem` (a plain `Item`) adds damage, speed and `ENTITY_INTERACTION_RANGE` modifiers. Better Combat reads these, so combo damage and speed follow the card numbers.
- **On-hit effects ride the vanilla hurt pipeline.** `MagicWeaponItem.hurtEnemy` / `DemonSlayerSwordItem.hurtEnemy` (called by `Player.attack`), the relic blow rules,
  `PairHooks.onDamagePost` (a `LivingDamageEvent.Post` on a `player_attack` damage source) and the Djinn Half-Equip special (`DjinnMagic.onOutgoing`) are all
  triggered by the damage that `Player.attack` deals, not by a click handler. There is no weapon logic keyed to `PlayerInteractEvent.LeftClickEmpty/LeftClickBlock` or
  to `swing`. `AttackEntityEvent` users (`TimeStop`, `Wave6Events` Slime Form) cancel a Better Combat swing too, because the server `Player.attack` fires that event.
- **The 0.52 full-swing bonus.** `WeaponArts.swing` / `DemonSlayerSwordItem.onLeftClickEntity` store `getAttackStrengthScale(0.5f)` in `Item.onLeftClickEntity`, which
  NeoForge's attack hook calls inside the server `Player.attack` just before the damage, and `fullSwing` consumes it in `hurtEnemy` (>= 0.9). Both run for a Better
  Combat hit. Better Combat is built to keep `getAttackStrengthScale` meaningful for its own cooldown, but **that was not verified here** (not in game, the mod is not
  on disk); if a swing can land with the scale always at 1.0 the bonus would fire on every hit.
- **Open question, not changed: twin pairs under Better Combat dual wielding.** `PairHooks` adds its own off-hand blow after a main-hand hit (a fraction of the damage, own
  cooldown `PairRules.nextAllowed`, guarded by `striking`). Better Combat's dual wielding also swings the off-hand weapon. With both mods a pair can therefore hit
  with the off hand twice per cycle (once ours, once Better Combat's), the first limited by our cooldown. Nothing was changed because the right answer (skip our blow
  when Better Combat is present, or lower its fraction) needs a look in game; it is the first thing to check.
- Relic passives that add `entity_interaction_range` while the relic is held (the Wave 2, 8, 9, 10 and 12 weapons) stack with `range_bonus`: the profile does not subtract them.
- **Sweep / AOE specials** that assume the vanilla sweep were left alone (the class code does not rely on `sweepAttack`).

No Java file was touched for combat. `CompatCatalog` text (the Compatibility Codex line for Better Combat) was refreshed from "12 weapons inherit its attack
presets" to "Every melee weapon of the mod ships a profile and custom animations".

## Not verified (honest list)

- **Nothing was run in game.** Better Combat is not available offline, so neither the weapon files nor the animations were loaded by it.
- **The schema is from memory** of Better Combat 2.x and Player Animator. The keys follow the request's description (`parent`, `attributes.range_bonus`,
  `two_handed`, `attacks[].hitbox/damage_multiplier/angle/upswing/animation`; animation `emote.moves[]`). If one is wrong, Better Combat logs the file and skips it;
  the weapon then keeps the vanilla swing, nothing crashes.
- Animation sign conventions (yaw left/right, pitch up/down), the `turn` field (always 0 here) and the exact effect of `upswing` per hitbox type are untested;
  `angle` on a `FORWARD_BOX` attack is a small width hint that may be ignored.
- The Genesis and Elsdocia numbers are a first pass; the Djinn range subtraction assumes `range_bonus` adds to the interaction range.
- `elsdocia_legacy` is only a model id (no registered item), kept as an alias so a stack carrying the id still works.
- Better Combat also draws the weapon trail and plays swing sounds from `swing_sound`; none is set, so those are the parent's defaults or silent.
