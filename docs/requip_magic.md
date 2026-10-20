# Requip Magic: the Equip Magic itself (Fairy Tail)

Round 1.99.0, builder "requip-abilities". Owner request: "also the Requip Magic should have custom ability based on Fairy Tail." Requip: The Knight (`nusmp:erza_requip`, `docs/erza_requip.md`) gave the
Ultimate skill its 14 armours. This adds the Equip Magic itself as Fairy Tail shows it, to the **same skill**, as modes appended after Dismiss. Nothing was removed and no id moved.

## NEW / REPLACED / KEPT

| Part | Status | Notes |
|---|---|---|
| Modes 0 to 13 (the 14 armours) and mode 14 (Dismiss) | KEPT | same indices, ids, names, costs, attacks, animation |
| The Djinn equip animation for armour swaps | KEPT | reused: by Partial Armour (the aura is re-forged) and by the loadout flourish |
| `RequipRules.MODES` (15) and `DISMISS_MODE` | KEPT | the skill now answers `RequipMagicRules.MODES` (28) |
| **Armoury** (the pocket space, window, key) | NEW | attachment `nusmp:requip_armoury`, menu `nusmp:requip_armoury`, key "Requip Armoury" |
| **Loadouts** (6 saved sets, one-press swap) | NEW | mode 16, tab Loadouts |
| **Requip: Blade**, **Requip: Barrage**, **Requip: Strike** | NEW | modes 17, 18, 19 |
| **Partial Armour** (an Erza armour drawn and worn with chosen pieces) | NEW | mode 20, tab Partial; an additive field in the aura style |
| **Seven finishers** (Circle Sword, Blumenblatt, Titania's Rampage, Dance of the Swords, Sea of Swords, Moonlight Cross, Dimension Cut) | NEW | modes 21 to 27 |
| Passives: Instant-Change, Pocket Capacity, Weapon Affinity, Requip Reflex | NEW | rules of mastery, see below |
| Server config `nusmp-requip-server.toml` | NEW | reflex on/off and pause, boss share of a volley, loadout flourish |
| In-game self-test `skill.requip_magic` | NEW | `skill/RequipMagicSelfTests` |
| `PlayerAuraPayload` style field | CHANGED (compatible) | a byte became a VarInt so the mask fits; values under 128 are the same single byte on the wire |
| Armour geo models, textures, the six weapons, the generators | KEPT, untouched | no new weapon was added, so `WeaponEngravings` needed no entry |
| Time Magic VFX | KEPT, untouched | |

REPLACED: nothing. Everything here is new next to what existed (CLAUDE.md rule 5: no existing feature covers a pocket space, saved loadouts, weapon summoning or piece-wise armour).

## The modes (appended after Dismiss; the mode index is saved, so the order is pinned by `RequipMagicRulesTest`)

| Mode | Id | Opens at (mastery / max EP) | Cost of max Aura (else Magicule) | Own pause | What it does |
|---|---|---|---|---|---|
| 15 Armoury | `erza.armoury` | start / 0 | none | none | opens the window; the key does the same |
| 16 Loadouts | `erza.loadouts` | 10% / 0 | 1.5% | 1 s (4 ticks with Instant-Change) | skill key: the next saved loadout; sneak + key: the previous one |
| 17 Requip: Blade | `erza.blade` | 5% / 0 | 0.5% | 0.5 s | key: summon the selected Armoury weapon into the main hand (the hand item goes into the cell the weapon left); key again: put it away; sneak + key: select the next weapon of the Armoury |
| 18 Requip: Barrage | `erza.barrage` | 20% / 50,000 | 6% | 12 s | 8 to 24 spectral copies of the held (else the selected) weapon |
| 19 Requip: Strike | `erza.strike` | 15% / 0 | 3% | 5 s | throws the held weapon along the crosshair and recalls it; key again: call it home now |
| 20 Partial Armour | `erza.partial` | 30% / 0 | the armour's re-equip share times the share kept | 1 s | key: switch Partial Armour on or off, or apply the setting to the worn armour; sneak + key: the next preset |
| 21 Circle Sword | `erza.circle_sword` | 25% / 50,000 | 8% | 14 s | a ring of 12 swords closes on the foe or spot you look at |
| 22 Blumenblatt | `erza.blumenblatt` | 35% / 75,000 | 10% | 17 s | a flower of swords opens round you: three waves, reach 9 |
| 23 Titania's Rampage | `erza.titanias_rampage` | 45% / 100,000 | 9% | 15 s | a lunge, then six slashes through up to five foes, the last one heavier |
| 24 Dance of the Swords | `erza.dance_of_swords` | 55% / 150,000 | 10% | 20 s | 16 swords turn round you for 3 seconds, twelve pulses |
| 25 Sea of Swords | `erza.sea_of_swords` | 70% / 250,000 | 14% | 30 s | a field of swords falls on a radius of 11 for 4 seconds, five pulses |
| 26 Moonlight Cross | `erza.moonlight_cross` | 60% / 200,000 | 9% | 13 s | two crescents cross in front of you, then a burst at the cross |
| 27 Dimension Cut | `erza.dimension_cut` | 50% / 100,000 | 7% | 8 s | a short slash (6 blocks) through the pocket that ignores 30% of armour |

Operators and creative players are never locked. A locked mode says what it wants. All costs go through `EnergyBridge.aura` (a share of max Aura, else Magicule; the finishers and the weapon attacks have
the same floor as the armours' attacks), all cooldowns through `DMUtil.cooldown`. **No hunger or exhaustion anywhere** (`NoHungerDrainTest`).

## 1. The Armoury (the pocket space)

* **Where the items live.** A per-player data attachment (`RequipArmoury.Data`), saved with the player, copied on death (`copyOnDeath`), unaffected by dimension changes and by anything that clears or
  replaces the vanilla inventory (another mod's inventory wipe, a grave mod, creative clearing). The server owns it: the window is a container menu, every cell is read and written on the server.
* **Cells.** A 9 by 6 grid (54). What a player may use is the *capacity* (`ArmouryRules.capacity`):

  | Part | Rule |
  |---|---|
  | mastery | 18 cells at the start, one more per 5.6% of mastery, 36 at full mastery |
  | Pocket Capacity (passive) | +6 cells from 35% mastery, +12 from 60%, +18 from 85% |
  | max EP holds it | 18 cells below 25,000 EP, 27 from 25,000, 36 from 50,000, 45 from 100,000, 54 from 250,000 |
  | result | the smaller of (mastery + passive) and the EP limit, never below 18, never above 54; operators and creative players have 54 |

  A cell past the capacity (mastery or EP fell, a config changed) **keeps its stack and still gives it back**; it only takes nothing new. Nothing is ever stranded or deleted.
* **Store and take.** Shift-click on the Pocket tab stores an inventory stack and takes an Armoury stack (vanilla `moveItemStackTo` over the cells below the capacity). Normal click, drag, double-click work
  as in a chest. The armour's own requipped weapon (flagged `ErzaRequip`) is refused: it belongs to its armour.
* **Never duplicates, never loses.** A stack is moved, not copied. The pure rules `ArmouryRules.insert/store/retrieve` keep the total (checked by 200 random runs in `ArmouryRulesTest`), and the in-game
  test checks the same through the real menu.
* **Unique and soulbound items.** Any item, with any components (custom name, enchantments, custom data of relics and soulbound items), is stored whole; two stacks merge only when item and components are
  equal (`ItemStack.isSameItemSameComponents`). The saved form uses the vanilla item codec; one stack that cannot be read (an item of a mod that was removed) is dropped like vanilla drops it from an inventory,
  instead of taking the whole Armoury with it.
* **The window** (`client/requip/RequipArmouryScreen`, theme `docs/gui_theme.md`): anime look (glass panel, a gold circle turning behind the grid, glowing slot wells) and the classic flat look. Three tabs:
  Pocket, Loadouts, Partial. Dimmed cells are past the capacity; a gold bracket marks the selected weapon, a cyan bracket the cell that Blade has displaced an item into.
* **Opened by** the Armoury mode (skill key) or the key "Requip Armoury" (Controls > Multiverse of Anime, unbound by default). The server checks that the player knows the skill.
* **GUI size.** Registered in `GuiScaleRegistry` as `requip_armoury` (design size = the container's own image size, `-1, -1`), so the 1.95.0 GUI Size screen lists it and the screen scales with the
  global or its own value; `GuiScaleTest` passes with it.

## 2. Loadouts (Instant Requip)

* Six loadouts. A loadout is a **template**: for each of 14 worn slots (head, chest, legs, feet, offhand, hotbar 1 to 9) the item that slot should hold. It is never a store of items, so a loadout can
  never hand out a copy; the items stay in the inventory or the Armoury until a swap moves them. *Save* (Loadouts tab) records what is worn now; *Clear* empties one.
* **A swap** (`LoadoutRules.plan`, pure, tested on copies): for every slot with a template, if the slot already holds a match it is left alone; otherwise the best match (exact stack first, then the same item
  with the same name, enchantments and custom data, so a damaged weapon still counts) is taken from the Armoury, then the rest of the inventory, then another worn slot that is not needed where it is;
  the stack that was in the slot **goes back to the Armoury** (the cell the new one left, else a free cell). A slot with an empty template is not touched. A missing item is skipped and counted
  ("2 pieces changed, 1 missing"); if nothing at all is left the swap is refused. **If the old set does not fit in the Armoury nothing at all changes** and the player is told. The plan is made on copies and
  written back only when it applies; a total check (before = after) guards every swap. Nothing goes to the ground.
* The cost is checked with a dry run first, so a refused swap costs nothing. Cooldown 1 s; **Instant-Change** (mastery 75% or operator) cuts it to 4 ticks.
* **Look.** A turning magic circle of enchant glyphs and light at the feet, a flash, the equip sound; and, when no Erza armour is on, the Djinn equip animation: a few pieces of Heart Kreuz (helm,
  pauldrons, gauntlets, greaves) forge in round the user and shatter out (`RequipMagicConfig.loadoutFlourish`, on by default). Vanilla armour slots swap instantly on the server.
* **Requip Reflex.** The star button on a loadout row marks it as the reflex loadout. At 40% mastery, when the user is hit, the skill swaps to it by itself (no cost, no cooldown) at most once per 20 s
  (config `reflexSeconds`, `reflexEnabled`). If no loadout is marked, the Adamantine Armor is put on instead when it is open to the user and the cost can be paid (interpretation of "swap to the
  Adamantine loadout slot": the Adamantine Armor is a render-layer armour, not an inventory set, so the marked loadout is the inventory half and Adamantine the fallback).

## 3. Weapon Requip, Barrage, Strike

* **Blade** is a *swap* between the main hand and the armoury cell the weapon was in, so it can never run out of room or copy anything. Put away swaps back; if the displaced item was moved meanwhile, the
  weapon goes to a free cell instead (and if there is none, nothing moves). A weapon is any non-stackable item whose own attack damage (player's 1 included) is at least 3
  (`RequipWeapons.isWeapon`). **Weapon Affinity** (from 20% mastery): melee hits with the summoned weapon do up to +40% (`BarrageMath.affinity`).
* **Barrage** (Fairy Tail's sword rain, as in Circle Sword / Dance of the Swords style attacks). The swords appear round the caster in a ring (one ring up to 12 swords, two rings above), hang for 14
  ticks (a few appear at a time, the ring turns), and leave one after another (2 ticks apart, 1 tick for more than 12 swords; the whole volley is over inside about 3 seconds) for the foes under the crosshair
  (the foe you look at first, then foes within 8 blocks of the crosshair point, swords shared round-robin; with no foe they spread round the point).
  * Count: 8, plus up to 10 from mastery, plus 2 each at 50,000, 200,000 and 1,000,000 max EP: 8 to 24.
  * Damage per sword (raw, before the balance law scales it with the caster's EP and mastery): the base of the weapon's **grade** read from its attack damage against the grade table
    (`GradeDamage.range`: Normal 20-30 ... Genesis 260-350; a weapon below Normal, such as a vanilla sword, is "grade -1"):

    | Weapon damage | Below Normal | Normal | Special | Rare | Unique | Legend | God | Genesis |
    |---|---|---|---|---|---|---|---|---|
    | base per sword | 3.0 | 4.0 | 5.5 | 7.0 | 8.5 | 10.0 | 12.0 | 14.0 |

    times up to 1.15 for a weapon high in its grade, times Weapon Affinity (1.0 to 1.4), times a share for big volleys (1 up to 12 swords, 0.71 at 24: 24 swords hit about 1.4 times as hard as 12 in
    total). `BalanceLaw.damage` then applies `epScale`, mastery, the mob multiplier and the player hit cap.
  * Pierce: a sword passes through 1 creature, 2 from 50% mastery, 3 from 90%.
  * **Boss cap.** `BalanceLaw.isBoss`: one cast (Barrage, Strike or any finisher) may take at most **10% of a boss's max health** in total (config `bossVolleyShare`). Everything else has no cap beyond the
    balance law's.
  * Targets are `SkillTargets` foes: allies, party, tamed animals, summons, creative players are never selected nor hit along the path.
  * Look: the mod's own blade shape (`VfxShape.LIGHT_BLADE`) flown from where each sword hangs to where it lands (the client animates it, the server resolves the hit when it lands), a magic circle and a
    ring on the floor, glints at the hanging swords.
* **Strike.** The held weapon leaves the hand into the *limbo* (saved with the player), flies the crosshair line (14 blocks, up to 24 with mastery) hitting every foe on it, and comes back hitting
  again at 80%. It returns to the slot it left (else the hand, else a free inventory slot, else the Armoury); it is never dropped on the ground and, because the limbo is saved, it survives a relog, a
  crash and a death: every second a safety net returns a weapon that is out with no throw running. Pressing Strike again recalls it at once.

## 4. Partial Armour

* Eleven piece **families**: helm (with halo and ribbon), gorget, chest (with backplate), belt (with skirt), pauldrons, vambraces, gauntlets, cuisses, greaves, sabatons, extras (wings, tail, cape,
  backdrop). A mask is an integer over them. Presets (the mode cycles them, the window has a button): whole armour, **gauntlets and greaves**, helm and chest, arms, legs, plate without ornaments,
  wings and chest; the window's eleven switches make any other mask.
* The Erza armour never enters an armour slot. The mask rides in the **aura style** next to the armour's ordinal (ordinal in the low byte, mask above it; a whole set is the plain ordinal exactly as
  before), so it is synced with the aura lease and a joining client gets it. `client/aura/ErzaAura` draws `PartialArmourMask.subset(armour, mask)` through the same `WaterAuraRig` (same forge-in,
  shatter-out, the pieces keep their own anchors and delays); a changed mask is a changed style, so the aura forges again.
* **What a partial armour gives.** The share of the whole armour's value that the kept pieces are (`PartialArmourMask.fraction`; the chest and back are worth the most, the sabatons the least): its attribute
  modifiers, its resistances and its upkeep are that share; the equip cost is the usual cost times that share. The special attacks and the guard stance need at least 25% of the armour on. A winged armour
  flies only with its wings (extras) on. Example: Adamantine with only gauntlets and greaves keeps 17% of its stats.
* Stored in the Armoury data (`mask`, `partialOn`); the worn mask is recorded on the skill instance when the armour goes on.

## 5. Finishers (all with the existing shapes, a few particles, no camera work)

Damage per hit is the table value times Weapon Affinity, scaled by the balance law; the boss cap above applies per cast. The caster keeps full control during the animation.

| Finisher | Name status | Script |
|---|---|---|
| Circle Sword | Heaven's Wheel technique name | t0 circle at the spot, 12 swords appear on a ring of radius 5 at height 3; t12 they close on the centre (blade shapes); t18 six hits of 9 raw on every foe within 4.6, flare |
| Blumenblatt | Heaven's Wheel technique name | t0 circle; a five-petal rose curve of 20 blades from the caster out to reach 9; waves at t6, t10, t14 (14 raw each, knock-back) |
| Titania's Rampage | the owner's name ("Titania" is Erza's epithet) | a lunge towards the first foe, then six slashes (every 3 ticks) chaining through up to five foes, the sixth at 1.6 times |
| Dance of the Swords | the owner's name | 16 swords turn round the caster for 60 ticks, twelve pulses of 6 raw within 3.6 (+0.4) blocks |
| Sea of Swords | descriptive | circle of radius 11 at the spot; 60 swords fall over 80 ticks; five pulses of 16 raw |
| Moonlight Cross | descriptive | two crescents (blade shapes) cross 7 blocks ahead; each 24 raw along its line, then a burst of 24 raw at the cross |
| Dimension Cut | not canon; the owner's idea | within 6 blocks: a rift, a dark seam, a cut of `BarrageMath.cutRaw` (3.5 swords' worth of the held weapon) that ignores 30% of armour (`EnergyBridge.armourBypass`, +0.1 with mastery, **capped at 40%**, and the factor at x3), an echo at half |

## Canon notes (what is Fairy Tail and what is interpretation)

* In Fairy Tail, **Requip (Equip) Magic** lets the user store weapons, armour and items in a pocket dimension and summon them, or swap them, instantly. The Armoury, Loadouts and Blade are that.
* **Erza Scarlet** ("Titania", the Queen of the Fairies) changes between many armours and swords in a moment, and uses a ring or rain of swords as the Heaven's Wheel Armor. **Circle Sword** and
  **Blumenblatt** are names of Heaven's Wheel Armor techniques. I used them for finishers that match what they are (a ring of swords; a flower of swords).
* The other names (Titania's Rampage, Dance of the Swords, Sea of Swords, Moonlight Cross, Dimension Cut, and the mode names Blade, Barrage, Strike, Partial Armour, Armoury, Loadouts) are the owner's
  words or descriptive; I did not claim them as canon techniques. If the owner knows the original names they are one string each in `RequipMagicRules.Mode` and `tools/lang/requip_magic.json`.
* Interpretations to be aware of: loadouts as templates (so the pocket cannot duplicate), the capacity by mastery and EP, the boss cap, the Requip Reflex using a marked loadout (Adamantine as the
  fallback), the finisher shapes, and the numbers of everything.

## Files

| Area | Files |
|---|---|
| Pure rules (no Minecraft classes) | `requip/ArmouryRules`, `LoadoutRules`, `BarrageMath`, `PartialArmourMask`, `RequipMagicRules` |
| Game side | `requip/RequipArmoury` (attachment, codec, menu, key packet), `RequipLoadouts`, `RequipWeapons`, `RequipMagicFx`, `RequipMagicConfig`, `RequipMagic` (init) |
| Skill side | `skill/ErzaRequipMagic` (modes, passives, hooks), `RequipScripts` (the animation clock and the damage rule), `RequipBarrage` (Barrage, Strike), `RequipFinishers`, `RequipMagicSelfTests` |
| Client | `client/requip/RequipArmouryScreen`, `RequipArmouryClient` (screen and key); `client/aura/ErzaAura` draws a subset |
| Lang | `tools/lang/requip_magic.json` (a flat fragment, not yet merged into `en_us.json`) |
| Tests | `ArmouryRulesTest`, `LoadoutRulesTest`, `BarrageMathTest`, `PartialArmourMaskTest`, `RequipMagicRulesTest` (`src/test/java/.../requip`), the in-game self-test `skill.requip_magic`; `GuiScaleTest` still passes |

### Edits to existing files (all additions)

`skill/ErzaRequipSkill` (modes appended, dispatch to `ErzaRequipMagic`, partial mask kept on the instance, reflex and affinity hooks), `skill/ErzaRequipFx` (aura and modifier overloads with a mask or
fraction), `requip/RequipRules` (`modifiers` and `afterResist` overloads with a fraction), `requip/RequipSystem` (one line: `RequipMagic.init`), `skill/ErzaSelfTests` (the mode count now follows
`RequipMagicRules.MODES`), `client/aura/ErzaAura` (subset sets), `aura/PlayerAuraPayload` (style as VarInt), `client/ui/scale/GuiScaleRegistry` (one `screen(...)` line).

## What could not be verified (nothing visual or in-game can be run here)

* No Gradle build and no game: the new code was type-checked against API stubs (only the known "X is abstract; cannot be instantiated" artefacts remain) and the pure rules are covered by JUnit in the
  shim (45 tests). The in-game self-test `skill.requip_magic` has not run.
* How the window looks and lays out (buttons, text widths, the hidden cells on the other tabs), whether the GUI Size scaling lines up with it, and the feel of every animation.
* The swords of the Barrage and the finishers are the mod's blade shape and particles, not a 3D copy of the held weapon. A real model of the weapon flying would need a client entity renderer.
* Whether the loadout flourish (a few Heart Kreuz pieces forging and shattering over vanilla armour) looks good; it can be switched off in the config.
* The balance of every number (damage, costs, cooldowns) was set from the existing armours' attacks and the grade table, and not played.
* The skill's lang description text still says "fourteen armours"; the description key is in `tools/lang/erza_requip.json` (another lane), so it was left alone.
