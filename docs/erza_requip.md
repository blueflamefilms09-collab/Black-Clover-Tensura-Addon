# Requip: The Knight (Erza Scarlet)

A standalone Tensura **Ultimate** skill (`nusmp:erza_requip`, registered in `skill/NUSkills`, class `skill/ErzaRequipSkill`) with one mode per armour and a last mode, Dismiss. It does not depend on the skill
tree or on any grimoire: it is taught by its own book, **Titania's Requip** (`nusmp:titanias_requip`).

## Getting it

| Way | How |
|---|---|
| The book | Use Titania's Requip. It teaches the skill if the reader has **100,000 max EP**, or **25,000 max EP and one mastered Unique skill** (`RequipRules.eligible`). The book is used up only after the skill was learned. |
| Loot | A pool is added to the vanilla ancient city, end city, stronghold library, bastion, mansion, dungeon and trial vault chests and to this mod's own dungeon vault, library ruins, grimoire tower, hidden cache and labyrinth pedestal chests (`RequipSystem.LOOT`, 3 to 10 percent per opening). |
| Admin | `/requip book <player>` gives the book, `/requip teach <player>` teaches the skill without the EP gate, `/requip set <player> <armour>` puts an armour on without cost, `/requip dismiss <player>` takes it off (permission level 2). |
| Creative tab | The book and the six weapons are in the mod's creative tab. |

## Using it

* Switch modes with the mode key like every other multi-mode skill: modes 0 to 13 are the armours in the table below, mode 14 is **Dismiss**.
* Press the skill key in an armour's mode while another armour (or none) is worn: the armour is **put on** (cost: a share of max Aura, else Magicule; 60 percent of it when swapping from another armour; a 3 second cooldown on that mode).
* Press it again in the mode of the armour you wear: its first special attack; **sneak and press**: its second. Each attack has its own cooldown and its own cost (`EnergyBridge.aura`: a share of max Aura, else Magicule, so it scales with the wearer's EP). Damage goes through `BalanceLaw` (EP scaling, mastery, the PvP cap).
* The armour costs a share of the Aura every second to keep; when the power runs dry it fades. Purgatory also takes health every second (never to death).
* Armours past the first few unlock with mastery of the skill (every cast adds mastery). Operators and creative players are never locked.
* Winged armours fly: **sprint in the air** to thrust along your look (the thrust builds up to a cap), **sneak in the air** to glide (Heaven's Wheel, Black Wing, Grand Chariot, Fairy) or to hover (Flight Armor, Wind God). Fall damage is cancelled while they fly.
* The Sea Empress gives water breathing, dolphin's grace and night vision in water and hits 40 percent harder with its attacks there. Nakagami hits casters harder (up to +60 percent by the target's max EP), jams the ultimate skills of what it hits and strips its buffs. Adamantine throws 25 percent of a melee hit back at the attacker (70 percent in Reflect Stance).

## The equip animation: the Djinn Full Equip

Every swap and the dismiss uses `client/aura/EquipAnimation`, the animation the Djinn full equip is built on. The armour is **a render-layer aura set** (`Aura.ERZA`, painter `client/aura/ErzaAura`, the set drawn by `client/aura/WaterAuraRig`):
never in an armour slot, every piece its own geo model anchored on the model part it was built for, so it follows crouching, swimming and swinging.

* **Putting on:** the pieces forge in staggered by their delay (0 to 12 ticks), each flying in from its own side while it fades up and grows from 85 to 100 percent over 8 ticks, with a flash of light (a white flash particle, a rising ring of the armour's colours, a ring of light on the ground that opens over 16 ticks) and the armour's theme particles.
* **Swapping:** the old armour shatters out over the last 18 ticks of its aura (`RequipRules.SHATTER_WINDOW` = `EquipAnimation.SHATTER_WINDOW`), then the new one forges in (`RequipRules.SWAP_GAP` ticks later, from the skill's tick), so a swap is one second of animation.
* **Dismissing:** the aura is cut to the shatter window: the pieces drift apart, fade and swell, and are gone.
* The server keeps the aura alive with a refresh every 400 ticks (a refresh of the same style keeps the pieces assembled).
* Held weapons come with the armour as real items drawn as 3D models by `client/WeaponRenderer` (see below) and are removed with it.

## Armours

Percentages are the share of the incoming damage class taken off (at most 90 percent with the guard stance). "Speed" and "Atk speed" are multiplied-base attribute modifiers; the others are added values; mastery strengthens attack, armor and toughness by up to 25 percent.
`Equip` and `Upkeep` are shares of the max Aura (else Magicule).

| # | Armour | Weapon | Speed | Attack | Armor | Toughness | Atk speed | Knockback res. | Resists | Flight | Equip | Upkeep/s | Unlocks at |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| 0 | Heart Kreuz Armor | - | +10% | +2.0 | +6.0 | +2.0 | +5% | 10% | physical 15%, fire 10%, lightning 10%, projectile 15%, magic 15% | - | 3.0% | 0.20% | start |
| 1 | Heaven's Wheel Armor | - | +20% | +3.0 | +4.0 | +1.0 | +10% | - | physical 10%, projectile 25% | glide 0.9 | 4.5% | 0.30% | start |
| 2 | Flame Empress Armor | flame_empress_blade | +5% | +4.0 | +8.0 | +3.0 | - | 20% | physical 10%, fire 80% | - | 4.0% | 0.30% | 5% mastery |
| 3 | Lightning Empress Armor | lightning_empress_halberd | +30% | +3.0 | +5.0 | +2.0 | +15% | - | physical 5%, lightning 80% | - | 4.0% | 0.30% | 10% mastery |
| 4 | Sea Empress Armor | sea_empress_trident | +10% | +3.0 | +7.0 | +2.0 | +5% | 10% | physical 5%, fire 30%, water 90% | - | 4.0% | 0.30% | 10% mastery |
| 5 | Black Wing Armor | black_wing_blade | +35% | +4.0 | +4.0 | +1.0 | +10% | - | projectile 10% | glide 1.2 | 4.5% | 0.35% | 20% mastery |
| 6 | Clear Heart Clothing | - | +45% | +2.0 | +1.0 | - | +45% | - | - | - | 2.5% | 0.20% | start |
| 7 | Nakagami Armor | nakagami_edge | +5% | +5.0 | +10.0 | +4.0 | - | 30% | fire 30%, lightning 30%, magic 60% | - | 12.0% | 1.20% | 25% mastery |
| 8 | Heaven's Wheel Armor: Grand Chariot | - | +25% | +4.0 | +5.0 | +2.0 | +10% | - | physical 15%, fire 5%, lightning 5%, projectile 35%, magic 5% | glide 1.0 | 8.0% | 0.50% | 40% mastery |
| 9 | Adamantine Armor | - | -10% | +3.0 | +16.0 | +8.0 | -10% | 100% | physical 50%, fire 20%, projectile 50%, magic 20%, explosion 50% | - | 5.0% | 0.30% | 10% mastery |
| 10 | Purgatory Armor | purgatory_mace | -5% | +9.0 | +6.0 | +3.0 | -5% | 50% | physical 20%, fire 40% | - | 8.0% | 0.40% + 1.0 HP | 40% mastery |
| 11 | Flight Armor | - | +30% | +2.0 | +3.0 | - | +10% | - | - | hover 1.5 | 3.5% | 0.30% | start |
| 12 | Wind God Armor | - | +40% | +3.0 | +3.0 | +1.0 | +20% | - | magic 10% | hover 1.8 | 5.0% | 0.40% | 25% mastery |
| 13 | Fairy Armor | - | +20% | +3.0 | +7.0 | +3.0 | +10% | - | magic 35% | glide 1.0 | 6.0% | 0.40% | 25% mastery |

| Armour | Attack | Key | Kind | Element | Base damage | Reach | Count | Cost | Cooldown | Self-damage |
|---|---|---|---|---|---|---|---|---|---|---|
| Heart Kreuz Armor | Kreuz Cross | skill key | CONE | none | 14 | 5.5 | - | 3.0% | 2.5 s | - |
|  | Kreuz Ward | sneak + skill key | GUARD | none | - | - | - | 4.0% | 12.0 s | - |
| Heaven's Wheel Armor | Sword Rain | skill key | VOLLEY | none | 9 | 28.0 | 10 | 5.0% | 8.0 s | - |
|  | Telekinesis | sneak + skill key | LIFT | none | 8 | 24.0 | - | 4.0% | 6.0 s | - |
| Flame Empress Armor | Empress Flame Slash | skill key | LINE | fire | 18 | 14.0 | - | 5.0% | 5.0 s | - |
|  | Inferno Burst | sneak + skill key | BURST | fire | 14 | 7.0 | - | 7.0% | 11.0 s | - |
| Lightning Empress Armor | Thunder Dash | skill key | DASH | lightning | 14 | 10.0 | - | 5.0% | 4.0 s | - |
|  | Thunderstorm | sneak + skill key | VOLLEY | lightning | 12 | 18.0 | 6 | 7.0% | 11.0 s | - |
| Sea Empress Armor | Tidal Wave | skill key | LINE | water | 15 | 16.0 | - | 5.0% | 5.5 s | - |
|  | Whirlpool | sneak + skill key | PULL | water | 10 | 8.0 | - | 6.0% | 10.0 s | - |
| Black Wing Armor | Gale Slash | skill key | LINE | wind | 16 | 16.0 | - | 5.0% | 5.0 s | - |
|  | Dark Dive | sneak + skill key | DASH | dark | 18 | 12.0 | - | 6.0% | 8.0 s | - |
| Clear Heart Clothing | Flurry | skill key | FLURRY | none | 5 | 6.0 | 6 | 4.0% | 4.0 s | - |
|  | Phantom Step | sneak + skill key | DASH | none | 6 | 8.0 | - | 3.0% | 3.5 s | - |
| Nakagami Armor | Null Cut | skill key | NULLIFY | anti_magic | 20 | 6.0 | - | 15.0% | 10.0 s | - |
|  | Anti-Magic Field | sneak + skill key | FIELD | anti_magic | 8 | 10.0 | - | 20.0% | 30.0 s | - |
| Heaven's Wheel Armor: Grand Chariot | Grand Sword Rain | skill key | VOLLEY | none | 10 | 34.0 | 24 | 12.0% | 15.0 s | - |
|  | Chariot Charge | sneak + skill key | DASH | none | 22 | 16.0 | - | 6.0% | 6.0 s | - |
| Adamantine Armor | Adamant Smash | skill key | BURST | none | 16 | 5.0 | - | 5.0% | 7.0 s | - |
|  | Reflect Stance | sneak + skill key | GUARD | none | - | - | - | 6.0% | 15.0 s | - |
| Purgatory Armor | Purgatory Smash | skill key | BURST | fire | 40 | 7.0 | - | 10.0% | 13.0 s | 15% of health |
|  | Hellfire Cataclysm | sneak + skill key | BURST | fire | 80 | 12.0 | - | 25.0% | 45.0 s | 30% of health |
| Flight Armor | Aerial Boost | skill key | DASH | wind | 8 | 14.0 | - | 4.0% | 4.0 s | - |
|  | Wing Blast | sneak + skill key | CONE | wind | 12 | 8.0 | - | 4.0% | 4.5 s | - |
| Wind God Armor | Cyclone Slash | skill key | BURST | wind | 15 | 7.0 | - | 6.0% | 7.5 s | - |
|  | Gale Rush | sneak + skill key | DASH | wind | 20 | 18.0 | - | 6.0% | 6.5 s | - |
| Fairy Armor | Fairy Radiance | skill key | HEAL | light | 10 | 10.0 | - | 8.0% | 15.0 s | - |
|  | Fairy Burst | sneak + skill key | BURST | light | 20 | 9.0 | - | 10.0% | 12.0 s | - |

## Pieces: anchor part, delay, fly-in side

The direction is the side the piece flies in from, in model pixels in the anchor part's frame (the Djinn's own numbers are about 2 to 14); y is down in that frame, so "above" is the head's own. The table is
`requip/ArmourSets.java`; the models are `assets/nusmp/geo/entity/erza_<armour>_arm_<piece>.geo.json` with a texture, a `_glow` map and, for animated pieces, an animation clip. Since 1.99.0 (the
Djinn-quality remake, `docs/erza_armour_remake.md`) every armour is a **full set of eighteen pieces** (belt, gorget, two gauntlets, two vambraces, two sabatons, two greaves, two cuisses, skirt, helm, two
pauldrons, chest, backplate) plus its own signature pieces, and the forging order follows the weight of a piece: the light underlayers first (belt 0, gorget 1), then the hands and feet, the legs, the
helm and skirt, the pauldrons, the cuirass and the back, the wings and the halo, the backdrop last (delay 12); `ArmourSets.weightOf` is the rule, `ErzaArmourPiecesTest` pins it.

**Heart Kreuz Armor** (`erza_heart_kreuz`)

| Piece | Anchor part | Delay (ticks) | Weight | Flies in from (x,y,z px) | Notes |
|---|---|---|---|---|---|
| belt | body | 0 | 1 | below (0,8,-8) | detail (24 blocks) |
| gorget | body | 1 | 1 | above (0,-10,-4) | detail (24 blocks) |
| gauntlet_r | arm_r | 2 | 2 | right (-10,8,-6) | detail (24 blocks) |
| gauntlet_l | arm_l | 2 | 2 | left (10,8,-6) | detail (24 blocks) |
| vambrace_r | arm_r | 3 | 2 | right (-12,2,-6) |  |
| vambrace_l | arm_l | 3 | 2 | left (12,2,-6) |  |
| sabaton_r | leg_r | 3 | 2 | below (-3,14,-8) |  |
| sabaton_l | leg_l | 4 | 2 | below (3,14,-8) |  |
| greave_r | leg_r | 5 | 3 | below (-3,14,-6) |  |
| greave_l | leg_l | 6 | 3 | below (3,14,-6) |  |
| cuisse_r | leg_r | 5 | 3 | below (-4,12,-6) |  |
| cuisse_l | leg_l | 6 | 3 | below (4,12,-6) |  |
| skirt | body | 6 | 3 | below (0,10,-6) | detail (24 blocks), animated |
| helm | head | 6 | 3 | above (0,-16,0) | hidden in first person |
| pauldron_r | arm_r | 7 | 4 | right (-14,-4,0) |  |
| pauldron_l | arm_l | 7 | 4 | left (14,-4,0) |  |
| chest | body | 8 | 5 | front (0,0,-14) |  |
| backplate | body | 9 | 5 | behind (0,0,12) |  |
| ribbon | head | 1 | 1 | above (0,-14,4) | detail (24 blocks), hidden in first person, animated |

**Heaven's Wheel Armor** (`erza_heavens_wheel`)

| Piece | Anchor part | Delay (ticks) | Weight | Flies in from (x,y,z px) | Notes |
|---|---|---|---|---|---|
| belt | body | 0 | 1 | below (0,8,-8) | detail (24 blocks) |
| gorget | body | 1 | 1 | above (0,-10,-4) | detail (24 blocks) |
| gauntlet_r | arm_r | 2 | 2 | right (-10,8,-6) | detail (24 blocks) |
| gauntlet_l | arm_l | 2 | 2 | left (10,8,-6) | detail (24 blocks) |
| vambrace_r | arm_r | 3 | 2 | right (-12,2,-6) |  |
| vambrace_l | arm_l | 3 | 2 | left (12,2,-6) |  |
| sabaton_r | leg_r | 3 | 2 | below (-3,14,-8) |  |
| sabaton_l | leg_l | 4 | 2 | below (3,14,-8) |  |
| greave_r | leg_r | 5 | 3 | below (-3,14,-6) |  |
| greave_l | leg_l | 6 | 3 | below (3,14,-6) |  |
| cuisse_r | leg_r | 5 | 3 | below (-4,12,-6) |  |
| cuisse_l | leg_l | 6 | 3 | below (4,12,-6) |  |
| skirt | body | 6 | 3 | below (0,10,-6) | detail (24 blocks), animated |
| helm | head | 6 | 3 | above (0,-16,0) | hidden in first person |
| pauldron_r | arm_r | 7 | 4 | right (-14,-4,0) |  |
| pauldron_l | arm_l | 7 | 4 | left (14,-4,0) |  |
| chest | body | 8 | 5 | front (0,0,-14) |  |
| backplate | body | 9 | 5 | behind (0,0,12) |  |
| wing_r | body | 10 | 6 | behind (-10,-4,12) | animated, flaps faster in the air |
| wing_l | body | 10 | 6 | behind (10,-4,12) | animated, flaps faster in the air |
| halo | head | 10 | 6 | above (0,-16,0) | translucent, detail (24 blocks), hidden in first person, animated |
| backdrop | body | 12 | 7 | behind (0,0,12) | translucent, detail (24 blocks), animated |

**Flame Empress Armor** (`erza_flame_empress`)

| Piece | Anchor part | Delay (ticks) | Weight | Flies in from (x,y,z px) | Notes |
|---|---|---|---|---|---|
| belt | body | 0 | 1 | below (0,8,-8) | detail (24 blocks) |
| gorget | body | 1 | 1 | above (0,-10,-4) | detail (24 blocks) |
| gauntlet_r | arm_r | 2 | 2 | right (-10,8,-6) | detail (24 blocks) |
| gauntlet_l | arm_l | 2 | 2 | left (10,8,-6) | detail (24 blocks) |
| vambrace_r | arm_r | 3 | 2 | right (-12,2,-6) |  |
| vambrace_l | arm_l | 3 | 2 | left (12,2,-6) |  |
| sabaton_r | leg_r | 3 | 2 | below (-3,14,-8) |  |
| sabaton_l | leg_l | 4 | 2 | below (3,14,-8) |  |
| greave_r | leg_r | 5 | 3 | below (-3,14,-6) |  |
| greave_l | leg_l | 6 | 3 | below (3,14,-6) |  |
| cuisse_r | leg_r | 5 | 3 | below (-4,12,-6) |  |
| cuisse_l | leg_l | 6 | 3 | below (4,12,-6) |  |
| skirt | body | 6 | 3 | below (0,10,-6) | detail (24 blocks), animated |
| helm | head | 6 | 3 | above (0,-16,0) | hidden in first person |
| pauldron_r | arm_r | 7 | 4 | right (-14,-4,0) |  |
| pauldron_l | arm_l | 7 | 4 | left (14,-4,0) |  |
| chest | body | 8 | 5 | front (0,0,-14) |  |
| backplate | body | 9 | 5 | behind (0,0,12) |  |
| tail | body | 9 | 5 | behind (0,6,14) | detail (24 blocks), animated |
| halo | head | 10 | 6 | above (0,-16,0) | translucent, detail (24 blocks), hidden in first person, animated |

**Lightning Empress Armor** (`erza_lightning_empress`)

| Piece | Anchor part | Delay (ticks) | Weight | Flies in from (x,y,z px) | Notes |
|---|---|---|---|---|---|
| belt | body | 0 | 1 | below (0,8,-8) | detail (24 blocks) |
| gorget | body | 1 | 1 | above (0,-10,-4) | detail (24 blocks) |
| gauntlet_r | arm_r | 2 | 2 | right (-10,8,-6) | detail (24 blocks) |
| gauntlet_l | arm_l | 2 | 2 | left (10,8,-6) | detail (24 blocks) |
| vambrace_r | arm_r | 3 | 2 | right (-12,2,-6) |  |
| vambrace_l | arm_l | 3 | 2 | left (12,2,-6) |  |
| sabaton_r | leg_r | 3 | 2 | below (-3,14,-8) |  |
| sabaton_l | leg_l | 4 | 2 | below (3,14,-8) |  |
| greave_r | leg_r | 5 | 3 | below (-3,14,-6) |  |
| greave_l | leg_l | 6 | 3 | below (3,14,-6) |  |
| cuisse_r | leg_r | 5 | 3 | below (-4,12,-6) |  |
| cuisse_l | leg_l | 6 | 3 | below (4,12,-6) |  |
| skirt | body | 6 | 3 | below (0,10,-6) | detail (24 blocks), animated |
| helm | head | 6 | 3 | above (0,-16,0) | hidden in first person |
| pauldron_r | arm_r | 7 | 4 | right (-14,-4,0) |  |
| pauldron_l | arm_l | 7 | 4 | left (14,-4,0) |  |
| chest | body | 8 | 5 | front (0,0,-14) |  |
| backplate | body | 9 | 5 | behind (0,0,12) |  |
| wing_r | body | 10 | 6 | behind (-10,-4,12) | translucent, animated, flaps faster in the air |
| wing_l | body | 10 | 6 | behind (10,-4,12) | translucent, animated, flaps faster in the air |
| halo | head | 10 | 6 | above (0,-16,0) | translucent, detail (24 blocks), hidden in first person, animated |

**Sea Empress Armor** (`erza_sea_empress`)

| Piece | Anchor part | Delay (ticks) | Weight | Flies in from (x,y,z px) | Notes |
|---|---|---|---|---|---|
| belt | body | 0 | 1 | below (0,8,-8) | detail (24 blocks) |
| gorget | body | 1 | 1 | above (0,-10,-4) | detail (24 blocks) |
| gauntlet_r | arm_r | 2 | 2 | right (-10,8,-6) | detail (24 blocks) |
| gauntlet_l | arm_l | 2 | 2 | left (10,8,-6) | detail (24 blocks) |
| vambrace_r | arm_r | 3 | 2 | right (-12,2,-6) |  |
| vambrace_l | arm_l | 3 | 2 | left (12,2,-6) |  |
| sabaton_r | leg_r | 3 | 2 | below (-3,14,-8) |  |
| sabaton_l | leg_l | 4 | 2 | below (3,14,-8) |  |
| greave_r | leg_r | 5 | 3 | below (-3,14,-6) |  |
| greave_l | leg_l | 6 | 3 | below (3,14,-6) |  |
| cuisse_r | leg_r | 5 | 3 | below (-4,12,-6) |  |
| cuisse_l | leg_l | 6 | 3 | below (4,12,-6) |  |
| skirt | body | 6 | 3 | below (0,10,-6) | detail (24 blocks), animated |
| helm | head | 6 | 3 | above (0,-16,0) | hidden in first person |
| pauldron_r | arm_r | 7 | 4 | right (-14,-4,0) |  |
| pauldron_l | arm_l | 7 | 4 | left (14,-4,0) |  |
| chest | body | 8 | 5 | front (0,0,-14) |  |
| backplate | body | 9 | 5 | behind (0,0,12) |  |
| wing_r | body | 10 | 6 | behind (-10,-4,12) | translucent, animated, flaps faster in the air |
| wing_l | body | 10 | 6 | behind (10,-4,12) | translucent, animated, flaps faster in the air |
| halo | head | 10 | 6 | above (0,-16,0) | translucent, detail (24 blocks), hidden in first person, animated |

**Black Wing Armor** (`erza_black_wing`)

| Piece | Anchor part | Delay (ticks) | Weight | Flies in from (x,y,z px) | Notes |
|---|---|---|---|---|---|
| belt | body | 0 | 1 | below (0,8,-8) | detail (24 blocks) |
| gorget | body | 1 | 1 | above (0,-10,-4) | detail (24 blocks) |
| gauntlet_r | arm_r | 2 | 2 | right (-10,8,-6) | detail (24 blocks) |
| gauntlet_l | arm_l | 2 | 2 | left (10,8,-6) | detail (24 blocks) |
| vambrace_r | arm_r | 3 | 2 | right (-12,2,-6) |  |
| vambrace_l | arm_l | 3 | 2 | left (12,2,-6) |  |
| sabaton_r | leg_r | 3 | 2 | below (-3,14,-8) |  |
| sabaton_l | leg_l | 4 | 2 | below (3,14,-8) |  |
| greave_r | leg_r | 5 | 3 | below (-3,14,-6) |  |
| greave_l | leg_l | 6 | 3 | below (3,14,-6) |  |
| cuisse_r | leg_r | 5 | 3 | below (-4,12,-6) |  |
| cuisse_l | leg_l | 6 | 3 | below (4,12,-6) |  |
| skirt | body | 6 | 3 | below (0,10,-6) | detail (24 blocks), animated |
| helm | head | 6 | 3 | above (0,-16,0) | hidden in first person |
| pauldron_r | arm_r | 7 | 4 | right (-14,-4,0) |  |
| pauldron_l | arm_l | 7 | 4 | left (14,-4,0) |  |
| chest | body | 8 | 5 | front (0,0,-14) |  |
| backplate | body | 9 | 5 | behind (0,0,12) |  |
| wing_r | body | 10 | 6 | behind (-10,-4,12) | animated, flaps faster in the air |
| wing_l | body | 10 | 6 | behind (10,-4,12) | animated, flaps faster in the air |

**Clear Heart Clothing** (`erza_clear_heart`)

| Piece | Anchor part | Delay (ticks) | Weight | Flies in from (x,y,z px) | Notes |
|---|---|---|---|---|---|
| belt | body | 0 | 1 | below (0,8,-8) | detail (24 blocks) |
| gorget | body | 1 | 1 | above (0,-10,-4) | detail (24 blocks) |
| gauntlet_r | arm_r | 2 | 2 | right (-10,8,-6) | detail (24 blocks) |
| gauntlet_l | arm_l | 2 | 2 | left (10,8,-6) | detail (24 blocks) |
| vambrace_r | arm_r | 3 | 2 | right (-12,2,-6) |  |
| vambrace_l | arm_l | 3 | 2 | left (12,2,-6) |  |
| sabaton_r | leg_r | 3 | 2 | below (-3,14,-8) |  |
| sabaton_l | leg_l | 4 | 2 | below (3,14,-8) |  |
| greave_r | leg_r | 5 | 3 | below (-3,14,-6) |  |
| greave_l | leg_l | 6 | 3 | below (3,14,-6) |  |
| cuisse_r | leg_r | 5 | 3 | below (-4,12,-6) |  |
| cuisse_l | leg_l | 6 | 3 | below (4,12,-6) |  |
| skirt | body | 6 | 3 | below (0,10,-6) | detail (24 blocks), animated |
| helm | head | 6 | 3 | above (0,-16,0) | hidden in first person |
| pauldron_r | arm_r | 7 | 4 | right (-14,-4,0) |  |
| pauldron_l | arm_l | 7 | 4 | left (14,-4,0) |  |
| chest | body | 8 | 5 | front (0,0,-14) |  |
| backplate | body | 9 | 5 | behind (0,0,12) |  |
| ribbon | head | 1 | 1 | above (0,-14,4) | detail (24 blocks), hidden in first person, animated |
| tail | body | 9 | 5 | behind (0,6,14) | detail (24 blocks), animated |

**Nakagami Armor** (`erza_nakagami`)

| Piece | Anchor part | Delay (ticks) | Weight | Flies in from (x,y,z px) | Notes |
|---|---|---|---|---|---|
| belt | body | 0 | 1 | below (0,8,-8) | detail (24 blocks) |
| gorget | body | 1 | 1 | above (0,-10,-4) | detail (24 blocks) |
| gauntlet_r | arm_r | 2 | 2 | right (-10,8,-6) | detail (24 blocks) |
| gauntlet_l | arm_l | 2 | 2 | left (10,8,-6) | detail (24 blocks) |
| vambrace_r | arm_r | 3 | 2 | right (-12,2,-6) |  |
| vambrace_l | arm_l | 3 | 2 | left (12,2,-6) |  |
| sabaton_r | leg_r | 3 | 2 | below (-3,14,-8) |  |
| sabaton_l | leg_l | 4 | 2 | below (3,14,-8) |  |
| greave_r | leg_r | 5 | 3 | below (-3,14,-6) |  |
| greave_l | leg_l | 6 | 3 | below (3,14,-6) |  |
| cuisse_r | leg_r | 5 | 3 | below (-4,12,-6) |  |
| cuisse_l | leg_l | 6 | 3 | below (4,12,-6) |  |
| skirt | body | 6 | 3 | below (0,10,-6) | detail (24 blocks), animated |
| helm | head | 6 | 3 | above (0,-16,0) | hidden in first person |
| pauldron_r | arm_r | 7 | 4 | right (-14,-4,0) |  |
| pauldron_l | arm_l | 7 | 4 | left (14,-4,0) |  |
| chest | body | 8 | 5 | front (0,0,-14) |  |
| backplate | body | 9 | 5 | behind (0,0,12) |  |
| cape | body | 9 | 5 | behind (0,-4,12) | detail (24 blocks), animated |
| halo | head | 10 | 6 | above (0,-16,0) | translucent, detail (24 blocks), hidden in first person, animated |

**Heaven's Wheel Armor: Grand Chariot** (`erza_grand_chariot`)

| Piece | Anchor part | Delay (ticks) | Weight | Flies in from (x,y,z px) | Notes |
|---|---|---|---|---|---|
| belt | body | 0 | 1 | below (0,8,-8) | detail (24 blocks) |
| gorget | body | 1 | 1 | above (0,-10,-4) | detail (24 blocks) |
| gauntlet_r | arm_r | 2 | 2 | right (-10,8,-6) | detail (24 blocks) |
| gauntlet_l | arm_l | 2 | 2 | left (10,8,-6) | detail (24 blocks) |
| vambrace_r | arm_r | 3 | 2 | right (-12,2,-6) |  |
| vambrace_l | arm_l | 3 | 2 | left (12,2,-6) |  |
| sabaton_r | leg_r | 3 | 2 | below (-3,14,-8) |  |
| sabaton_l | leg_l | 4 | 2 | below (3,14,-8) |  |
| greave_r | leg_r | 5 | 3 | below (-3,14,-6) |  |
| greave_l | leg_l | 6 | 3 | below (3,14,-6) |  |
| cuisse_r | leg_r | 5 | 3 | below (-4,12,-6) |  |
| cuisse_l | leg_l | 6 | 3 | below (4,12,-6) |  |
| skirt | body | 6 | 3 | below (0,10,-6) | detail (24 blocks), animated |
| helm | head | 6 | 3 | above (0,-16,0) | hidden in first person |
| pauldron_r | arm_r | 7 | 4 | right (-14,-4,0) |  |
| pauldron_l | arm_l | 7 | 4 | left (14,-4,0) |  |
| chest | body | 8 | 5 | front (0,0,-14) |  |
| backplate | body | 9 | 5 | behind (0,0,12) |  |
| wing_r | body | 10 | 6 | behind (-10,-4,12) | animated, flaps faster in the air |
| wing_l | body | 10 | 6 | behind (10,-4,12) | animated, flaps faster in the air |
| halo | head | 10 | 6 | above (0,-16,0) | translucent, detail (24 blocks), hidden in first person, animated |
| backdrop | body | 12 | 7 | behind (0,0,12) | translucent, detail (24 blocks), animated |

**Adamantine Armor** (`erza_adamantine`)

| Piece | Anchor part | Delay (ticks) | Weight | Flies in from (x,y,z px) | Notes |
|---|---|---|---|---|---|
| belt | body | 0 | 1 | below (0,8,-8) | detail (24 blocks) |
| gorget | body | 1 | 1 | above (0,-10,-4) | detail (24 blocks) |
| gauntlet_r | arm_r | 2 | 2 | right (-10,8,-6) | detail (24 blocks) |
| gauntlet_l | arm_l | 2 | 2 | left (10,8,-6) | detail (24 blocks) |
| vambrace_r | arm_r | 3 | 2 | right (-12,2,-6) |  |
| vambrace_l | arm_l | 3 | 2 | left (12,2,-6) |  |
| sabaton_r | leg_r | 3 | 2 | below (-3,14,-8) |  |
| sabaton_l | leg_l | 4 | 2 | below (3,14,-8) |  |
| greave_r | leg_r | 5 | 3 | below (-3,14,-6) |  |
| greave_l | leg_l | 6 | 3 | below (3,14,-6) |  |
| cuisse_r | leg_r | 5 | 3 | below (-4,12,-6) |  |
| cuisse_l | leg_l | 6 | 3 | below (4,12,-6) |  |
| skirt | body | 6 | 3 | below (0,10,-6) | detail (24 blocks), animated |
| helm | head | 6 | 3 | above (0,-16,0) | hidden in first person |
| pauldron_r | arm_r | 7 | 4 | right (-14,-4,0) |  |
| pauldron_l | arm_l | 7 | 4 | left (14,-4,0) |  |
| chest | body | 8 | 5 | front (0,0,-14) |  |
| backplate | body | 9 | 5 | behind (0,0,12) |  |
| cape | body | 9 | 5 | behind (0,-4,12) | detail (24 blocks), animated |

**Purgatory Armor** (`erza_purgatory`)

| Piece | Anchor part | Delay (ticks) | Weight | Flies in from (x,y,z px) | Notes |
|---|---|---|---|---|---|
| belt | body | 0 | 1 | below (0,8,-8) | detail (24 blocks) |
| gorget | body | 1 | 1 | above (0,-10,-4) | detail (24 blocks) |
| gauntlet_r | arm_r | 2 | 2 | right (-10,8,-6) | detail (24 blocks) |
| gauntlet_l | arm_l | 2 | 2 | left (10,8,-6) | detail (24 blocks) |
| vambrace_r | arm_r | 3 | 2 | right (-12,2,-6) |  |
| vambrace_l | arm_l | 3 | 2 | left (12,2,-6) |  |
| sabaton_r | leg_r | 3 | 2 | below (-3,14,-8) |  |
| sabaton_l | leg_l | 4 | 2 | below (3,14,-8) |  |
| greave_r | leg_r | 5 | 3 | below (-3,14,-6) |  |
| greave_l | leg_l | 6 | 3 | below (3,14,-6) |  |
| cuisse_r | leg_r | 5 | 3 | below (-4,12,-6) |  |
| cuisse_l | leg_l | 6 | 3 | below (4,12,-6) |  |
| skirt | body | 6 | 3 | below (0,10,-6) | detail (24 blocks), animated |
| helm | head | 6 | 3 | above (0,-16,0) | hidden in first person |
| pauldron_r | arm_r | 7 | 4 | right (-14,-4,0) |  |
| pauldron_l | arm_l | 7 | 4 | left (14,-4,0) |  |
| chest | body | 8 | 5 | front (0,0,-14) |  |
| backplate | body | 9 | 5 | behind (0,0,12) |  |
| cape | body | 9 | 5 | behind (0,-4,12) | detail (24 blocks), animated |
| backdrop | body | 12 | 7 | behind (0,0,12) | translucent, detail (24 blocks), animated |

**Flight Armor** (`erza_flight_armor`)

| Piece | Anchor part | Delay (ticks) | Weight | Flies in from (x,y,z px) | Notes |
|---|---|---|---|---|---|
| belt | body | 0 | 1 | below (0,8,-8) | detail (24 blocks) |
| gorget | body | 1 | 1 | above (0,-10,-4) | detail (24 blocks) |
| gauntlet_r | arm_r | 2 | 2 | right (-10,8,-6) | detail (24 blocks) |
| gauntlet_l | arm_l | 2 | 2 | left (10,8,-6) | detail (24 blocks) |
| vambrace_r | arm_r | 3 | 2 | right (-12,2,-6) |  |
| vambrace_l | arm_l | 3 | 2 | left (12,2,-6) |  |
| sabaton_r | leg_r | 3 | 2 | below (-3,14,-8) |  |
| sabaton_l | leg_l | 4 | 2 | below (3,14,-8) |  |
| greave_r | leg_r | 5 | 3 | below (-3,14,-6) |  |
| greave_l | leg_l | 6 | 3 | below (3,14,-6) |  |
| cuisse_r | leg_r | 5 | 3 | below (-4,12,-6) |  |
| cuisse_l | leg_l | 6 | 3 | below (4,12,-6) |  |
| skirt | body | 6 | 3 | below (0,10,-6) | detail (24 blocks), animated |
| helm | head | 6 | 3 | above (0,-16,0) | hidden in first person |
| pauldron_r | arm_r | 7 | 4 | right (-14,-4,0) |  |
| pauldron_l | arm_l | 7 | 4 | left (14,-4,0) |  |
| chest | body | 8 | 5 | front (0,0,-14) |  |
| backplate | body | 9 | 5 | behind (0,0,12) |  |
| wing_r | body | 10 | 6 | behind (-10,-4,12) | animated, flaps faster in the air |
| wing_l | body | 10 | 6 | behind (10,-4,12) | animated, flaps faster in the air |
| ribbon | head | 1 | 1 | above (0,-14,4) | detail (24 blocks), hidden in first person, animated |

**Wind God Armor** (`erza_wind_god`)

| Piece | Anchor part | Delay (ticks) | Weight | Flies in from (x,y,z px) | Notes |
|---|---|---|---|---|---|
| belt | body | 0 | 1 | below (0,8,-8) | detail (24 blocks) |
| gorget | body | 1 | 1 | above (0,-10,-4) | detail (24 blocks) |
| gauntlet_r | arm_r | 2 | 2 | right (-10,8,-6) | detail (24 blocks) |
| gauntlet_l | arm_l | 2 | 2 | left (10,8,-6) | detail (24 blocks) |
| vambrace_r | arm_r | 3 | 2 | right (-12,2,-6) |  |
| vambrace_l | arm_l | 3 | 2 | left (12,2,-6) |  |
| sabaton_r | leg_r | 3 | 2 | below (-3,14,-8) |  |
| sabaton_l | leg_l | 4 | 2 | below (3,14,-8) |  |
| greave_r | leg_r | 5 | 3 | below (-3,14,-6) |  |
| greave_l | leg_l | 6 | 3 | below (3,14,-6) |  |
| cuisse_r | leg_r | 5 | 3 | below (-4,12,-6) |  |
| cuisse_l | leg_l | 6 | 3 | below (4,12,-6) |  |
| skirt | body | 6 | 3 | below (0,10,-6) | detail (24 blocks), animated |
| helm | head | 6 | 3 | above (0,-16,0) | hidden in first person |
| pauldron_r | arm_r | 7 | 4 | right (-14,-4,0) |  |
| pauldron_l | arm_l | 7 | 4 | left (14,-4,0) |  |
| chest | body | 8 | 5 | front (0,0,-14) |  |
| backplate | body | 9 | 5 | behind (0,0,12) |  |
| wing_r | body | 10 | 6 | behind (-10,-4,12) | animated, flaps faster in the air |
| wing_l | body | 10 | 6 | behind (10,-4,12) | animated, flaps faster in the air |
| halo | head | 10 | 6 | above (0,-16,0) | translucent, detail (24 blocks), hidden in first person, animated |
| tail | body | 9 | 5 | behind (0,6,14) | detail (24 blocks), animated |

**Fairy Armor** (`erza_fairy_armor`)

| Piece | Anchor part | Delay (ticks) | Weight | Flies in from (x,y,z px) | Notes |
|---|---|---|---|---|---|
| belt | body | 0 | 1 | below (0,8,-8) | detail (24 blocks) |
| gorget | body | 1 | 1 | above (0,-10,-4) | detail (24 blocks) |
| gauntlet_r | arm_r | 2 | 2 | right (-10,8,-6) | detail (24 blocks) |
| gauntlet_l | arm_l | 2 | 2 | left (10,8,-6) | detail (24 blocks) |
| vambrace_r | arm_r | 3 | 2 | right (-12,2,-6) |  |
| vambrace_l | arm_l | 3 | 2 | left (12,2,-6) |  |
| sabaton_r | leg_r | 3 | 2 | below (-3,14,-8) |  |
| sabaton_l | leg_l | 4 | 2 | below (3,14,-8) |  |
| greave_r | leg_r | 5 | 3 | below (-3,14,-6) |  |
| greave_l | leg_l | 6 | 3 | below (3,14,-6) |  |
| cuisse_r | leg_r | 5 | 3 | below (-4,12,-6) |  |
| cuisse_l | leg_l | 6 | 3 | below (4,12,-6) |  |
| skirt | body | 6 | 3 | below (0,10,-6) | detail (24 blocks), animated |
| helm | head | 6 | 3 | above (0,-16,0) | hidden in first person |
| pauldron_r | arm_r | 7 | 4 | right (-14,-4,0) |  |
| pauldron_l | arm_l | 7 | 4 | left (14,-4,0) |  |
| chest | body | 8 | 5 | front (0,0,-14) |  |
| backplate | body | 9 | 5 | behind (0,0,12) |  |
| wing_r | body | 10 | 6 | behind (-10,-4,12) | translucent, animated, flaps faster in the air |
| wing_l | body | 10 | 6 | behind (10,-4,12) | translucent, animated, flaps faster in the air |
| halo | head | 10 | 6 | above (0,-16,0) | translucent, detail (24 blocks), hidden in first person, animated |


## Weapons and engravings

Six armours put a weapon in the hand (a real item flagged `ErzaRequip`, unbreakable, removed with the armour). Each is a pixel-built 3D model (`tools/gen_erza_weapons.py`, drawn by `client/WeaponRenderer`, previewed with `python tools/item_preview/preview_weapons.py <id>`), keeps a flat sprite as its inventory icon, and carries Tensura engravings (`item/WeaponEngravings.java`, applied once to the stack):

| Weapon | Armour | Engravings |
|---|---|---|
| `flame_empress_blade` | Flame Empress | elemental boost 2, severance 1, barrier piercing 1 |
| `lightning_empress_halberd` | Lightning Empress | elemental boost 2, swift 2, crushing 1 |
| `sea_empress_trident` | Sea Empress | elemental boost 2, barrier piercing 1, magic weapon 1 |
| `black_wing_blade` | Black Wing | swift 2, severance 2, enervation 1 |
| `nakagami_edge` | Nakagami | anti-magic 1, barrier piercing 3, magic interference 2, magicule absorption 1 |
| `purgatory_mace` | Purgatory | crushing 3, sturdy 2, elemental boost 1, severance 1 |

## Files

| Area | Files |
|---|---|
| Table and rules (no Minecraft classes) | `requip/Armour.java`, `requip/ArmourSets.java`, `requip/RequipRules.java` |
| Items, loot, command | `requip/RequipItems.java`, `requip/TitaniasRequipItem.java`, `requip/RequipWeaponItem.java`, `requip/RequipSystem.java` |
| Skill, effects, attacks, self-test | `skill/ErzaRequipSkill.java`, `skill/ErzaRequipFx.java`, `skill/ErzaRequipAttacks.java`, `skill/ErzaSelfTests.java` |
| Client | `client/aura/ErzaAura.java` |
| Assets | `tools/gen_erza_armour.py` (the 1.99.0 Djinn-quality remake: 288 piece models, see `docs/erza_armour_remake.md`; the 1.96.0 `tools/gen_erza_requip_geo.py` is kept behind `--legacy`), `tools/gen_erza_weapons.py` (six weapons, the book, the skill icon), the preview scenes `tools/render_preview/scenes.d/erza_requip.json` and `erza_armour.json` |
| Lang | `tools/lang/erza_requip.json` (a flat fragment, not yet merged into `en_us.json`) |
| Tests | `ArmourTableTest`, `RequipAnimationTest`, `ArmourAssetsTest` (`src/test/java/.../requip`), `RequipEngravingsTest` (`.../item`), the in-game self-test `skill.erza_requip` |

Preview without the game: `python3 -B tools/render_preview/preview.py erza_piece_view erza_heart_kreuz erza_forge_and_shatter ...` (the real `ErzaAura` painter and the real equip animation, headless); `python3 -B tools/item_preview/preview_weapons.py flame_empress_blade lightning_empress_halberd sea_empress_trident black_wing_blade nakagami_edge purgatory_mace`.

## Checks

* JUnit: the table's order is pinned (append only; the saved skill stores the worn armour as an ordinal), every number is in a fair range, the named trade-offs hold (Flame Empress resists fire, Clear Heart is the fastest and thinnest, Adamantine the wall, Nakagami the most expensive, Purgatory hurts its wearer and hits hardest, the Grand Chariot rains more swords), every piece's delay is 0 to 12 ticks and it flies in from its own side, the shatter window equals `EquipAnimation.SHATTER_WINDOW`, the modifier map, resistances, reflection and costs are exact, and the files on disk are exactly the table's models.
* In game, `skill.erza_requip` (run by the CI server smoke test): the skill is a registered Ultimate with fifteen mode ids; every armour's attribute modifiers go on a stand-in player and come off leaving the base values; real damage sources are reduced as the table says; every piece model, texture and glow map is in the jar, every weapon has its mesh, skin, models and icon; the weapons come out engraved; the command path teaches the skill once.
* Not checked without a person in the game: how the pieces look on a real skin in motion, the feel of the attacks, the sounds, flight, balance.
