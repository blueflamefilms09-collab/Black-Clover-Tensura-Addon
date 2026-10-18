# The Legendary Shield (`nusmp:legendary_shield`, `HeroKind.SHIELD`, style BLOCK, curse series: Wrath)

> Status: complete. Section 1 (the look) is what the art builder reads. Sections 2 to 12 are the research, the design, every form, skill and number, the engravings,
> the files and an honest list of what is not verified. Nothing here has been run in the game.
> Sections 4 to 6 (the forms, the techniques and the passives) are generated from the source and the forms file, so they cannot disagree with the code.

## 1. The look (for the art builder: `hart1`)

Everything here is from the series as far as the wiki search summaries tell, and from memory where marked (M). Nothing was checked against a picture from this machine.

**What it is.** The Small Shield of the series' first episodes: a plain, round hero's shield strapped to the forearm, clean steel with ONE bright colour, a green jewel set
in the middle of the face (the series puts a jewel on the back of the hand of every Legendary Weapon: green for the Shield (M), blue for the Sword (M)). Noble and plain at
the base look: no ornament that is not needed.

**Size class.** A `shield` class: about 1.0 block across (0.9 to 1.2 as the art brief asks), 0.11 thick at the rim, 0.20 thick at the raised centre. Held upright, the
face looks to +z, the handle behind it (the convention of the wave 23 shields, `docs/weapon_scale_orientation.md`).

**Named parts (8 to 14 allowed, this is the intended list).**
| part | shape | notes |
|---|---|---|
| `face` | a shallow dome, 14 to 16 segments, radius 0.50 | the main plate; brushed cold steel; a faint radial grain |
| `rim` | a rolled rim, tube section radius 0.035 | brighter steel; a thin teal line (0x1E6B5E) just inside it |
| `ribs` | 8 low raised ribs from the collar to the rim, bevelled | they give the round plate its hero's-shield silhouette; evenly spaced |
| `inner_ring` | one engraved ring at 55 percent of the radius | a groove, not a part that stands out |
| `collar` | a short round boss (radius 0.14) the gem sits in | silver, a bevel on both edges |
| `gem` | a cabochon (half a flattened sphere), radius 0.085, standing 0.05 proud | THE jewel: emerald green; the only part on the glow skin at full strength |
| `studs` | 8 small domed rivets on the rim between the ribs | catch the light |
| `grip` | a round bar across the back, 0.45 long, set 0.12 behind the face | where the hand holds it |
| `strap` | a forearm band on the back | dark leather, a plain buckle |

**Palette (0xRRGGBB).** Cold steel 0xB8C4D4, dark steel (shade side) 0x3C4654, bright edge 0xE8F0FA, trim teal 0x1E6B5E, leather 0x2A2420. The jewel: emerald 0x2EE6A0 with a darker
core 0x0E8A5E. Glow skin: the jewel, plus a faint line along the inner ring and the rib edges (about 25 percent of the jewel's strength).

**The forms.** There is one base mesh and one curse mesh (the contract has two looks); every ordinary form uses the base look, the Wrath forms use the curse look. The gem colour
is the same for all ordinary forms. (A per-form tint of the gem would be a later extension of the contract, it is not part of this job.)

**Curse look: Wrath.** The SAME footprint (a round 1.0 block shield, the same grip and strap so the hand and the animation fit) made monstrous:
* the rim grows 12 curved spikes, 0.18 to 0.28 long, leaning clockwise like a saw blade; two of them longer than the rest;
* the face is black iron (0x15161A) with deep cracks that run from the collar outwards and branch; the cracks are the glow: burning red-orange (0xC2181B at the edge, 0xFF6A1A in the
  hot centre of a crack), about 70 percent strength;
* the ribs become flame-shaped fins that curl around the collar;
* the jewel turns into a red-black eye: dark red 0x7A0B0B with a bright 0xFF3A1A slit, standing further out (0.08 proud) with a spiked collar;
* the strap gets a chain of three short links hanging from the grip (decor only); the studs become small spikes.
Wrath's colour in the code is 0xC2181B (`Sin.WRATH`).

**Inventory icon (16 x 16).** A round steel disc with a green dot in the middle and a light rim, on a transparent ground; the old plain sprite style of the other shields.

## 2. Canon: what the series says, and where each fact comes from

The wiki pages themselves could not be opened from the build machine (the fandom host is unreachable from it), so every canon fact below comes from the SUMMARIES that a
web search returned for these pages. Those summaries are fan-compiled and sometimes disagree with each other. Provenance marks: **S** = stated in a search summary,
**M** = from my memory of the series and not checked, **D** = my own design for this mod.

Pages the summaries came from:
* https://shield-hero.fandom.com/wiki/Legendary_Shield
* https://shield-hero.fandom.com/wiki/Legendary_Shield_(Web_Novel)
* https://shield-hero.fandom.com/wiki/Naofumi_Iwatani/Powers_and_Abilities
* https://shield-hero.fandom.com/wiki/Legendary_Weapons/Weapon_Effects
* https://shield-hero.fandom.com/wiki/Legendary_Weapons and https://shield-hero.fandom.com/wiki/Legendary_Weapons/Power-up_Methods
* https://shield-hero.fandom.com/wiki/Curse_Series, https://shield-hero.fandom.com/wiki/Curse_Series/Weapon_List, https://shield-hero.fandom.com/wiki/Wrath_Series_(Web_Novel),
  https://shield-hero.fandom.com/wiki/Shield_of_Wrath, https://shield-hero.fandom.com/wiki/The_Shield_of_Rage
* https://shield-hero.fandom.com/wiki/Chimera

(The same searches also returned the Bless Series, Battle of Good and Evil, Ost Horai and Mamoru Shirono pages; they were not used.)

| Fact | Mark | What the mod does with it |
|---|---|---|
| The Chimera Viper Shield has the equip bonus skill Change Shield, Antidote Compounding up, Poison Resistance, the special effect Snake Venom Fang and Hook; Naofumi kept it for a very long time. | S | Form `chimera_viper_shield` with the `venom_fang` passive, Hook, Prison, Chain Shield and Iron Maiden. |
| The Soul Eater Shield is the source of Drain Null (negates any drain effect that hits the user), Undead Control and Wall Escape. | S | Form `soul_eater_shield`: `drain_null`, Undead Control, plus the soul mechanic (Soul Eater passive, Soul Aegis). Wall Escape is NOT built (section 3). |
| The Needle Shield (small) is a counter-attack: after the shield has been hit it shoots out several needles; the Animal Needle Shield and the Bee Needle Shields unlock it. | S | Form `needle_shield`: the `needle_counter` passive answers a blocked blow, Needle Burst fires the stored Counter. |
| Air Strike Shield (literally First Shield) instantly creates a floating shield within about 5 metres that lasts about 15 seconds. | S | Air Strike Shield, with the mod's larger ranges and shorter life (12 blocks, 8 s) because combat here is faster. |
| Second Shield (the wiki's entry is S Float Shield) makes a second shield with the properties of the first. | S | Second Shield, changed on purpose to a ward that follows a friend, because the mod needs a party protection tool more than a second pane. |
| Shield Prison is a cage of shields; Change Shield alters the shields that skills such as Shield Prison and Air Strike Shield summon. | S | Shield Prison (a slowing cage with a pull-back). The alteration of other shields is not built. |
| Chain Shield links the floating shields with chains, trapping enemies in between. | S | Chain Shield: chains bind up to four foes and draw them together. |
| Change Shield (Attack) turns the shield into a striking form to inflict damage (listed as a Wrath-series skill; the Shield of Compassion shares it). | S | Change Shield (Attack) is the Shield's one real attack, so the base form already has it (the owner's text: counter-attacks). |
| Iron Maiden has to be activated by first using Shield Prison to trap an enemy and Change Shield (Attack) to run it through with spikes; sources disagree on whether it costs a lot of magic or all SP. | S | Iron Maiden needs a foe in the wielder's Prison and spends the Counter into the strike; its price is a normal cost plus the Prison's. |
| Blood Sacrifice draws the user's blood out into a bear-trap shape that crushes the enemy; it seriously wounds Naofumi and curses him with a 30 percent stat cut except defence, and can leave him unconscious for a day. | S | Blood Sacrifice: a health price with a floor (it cannot kill), then Weakness and Slowness for 15 s as the stat curse, and corruption. |
| The Self-Burning Curse is a curse-type flame that reduces healing; the first Wrath shield carries it and a strength boost; the cost is physical and psychological. | S | The Wrath forms: the curse engine burns the wielder; the `rage_fury` passive gives the strength (section 7). |
| Progression: Shield of Rage (first); Rage II needs the Zombie Dragon's stone and adds roar and frenzy; Wrath III adds Blood Sacrifice; Wrath IV (from a dragon core of the Demon Dragon, or Emperor, the sources differ) adds Megiddo Burst. | S | Four Wrath forms, `rage_shield`, `rage_shield_2`, `wrath_shield_3`, `wrath_shield_4`, with Rage Bash, Rage Roar, Blood Sacrifice and Megiddo Burst. The requirements are my design (blaze, ghast tears, nether star ...). |
| Megiddo Burst is the next step of the Wrath series; its effects are unknown on the wiki and Naofumi believed it too dangerous to use. | S | Megiddo Burst is my design: a huge, slow, expensive burst whose price is 30 percent of the wielder's health. The danger is the price. |
| The Shield of Compassion suppresses the Shield of Wrath and shares Iron Maiden and Change Shield (Attack). | S | Not built: the Bless Series is out of scope (design.md sections 7 and 18). |
| The Light Metal Shield and the Chimera shields are listed on the Legendary Shield page; no equip bonus for the Light Metal Shield was in the summary. | S | Form `light_metal_shield` (the party's wall) is my design: a very hard guard (`bulwark`), covering and floating shields. |
| The Leaf, Orange Balloon, Egg, Slime and Zombie shields (and the Small Shield as the starting shape) are names I remember from the series; the search could not confirm their equip bonuses ('Inventory Rot Resistance' of the Inter-Dimensional Zombie Shield is listed as unknown). | M | Forms with those names exist; their passives and skills are my design (healing herbs, a soft landing, ward and pets, a gel cushion, rotting endurance). |
| No page mentioned a 'Shooting Star Shield' or a 'Faust Blood' shield; the Sword's first offensive skill is also called Shooting Star Sword. | S (absence) | Shooting Star Shield is my design (a thrown comet of glass, the brief suggested the name). Faust Blood is not built. |
| The Legendary Shield's power-up methods include sharing the power-up methods of other legendary and vassal weapons; weapons can also be grown by copying, absorbing materials, mastery and awakening. | S | Handled by the forms system (absorption into the gem, mastery points), not by this weapon's logic. |

What the searches did NOT find: the Leaf Shield, the equip bonuses of the Small, Orange Balloon, Egg and Slime shields, a Shooting Star Shield, a Faust Blood shield, and any
effect of Megiddo Burst. Those parts are memory (M) or design (D), as marked above.

## 3. How the Shield plays in this mod

The owner's text: absolute defence, party support and counter-attacks; it cannot deal direct damage easily on its own, so it relies on absorbing materials, on specialised
forms (the Wrath series, the Soul Eater Shield) and on allies for offence. The design has four layers, and the offence is the thinnest of them on purpose.

**1. The guard (the use key).** The core's item raises the shield like a vanilla shield. Every blow it blocks reaches `ShieldLogic.onBlocked`, which asks the `GuardMeter`:
* **Resolve** is the damage the guard can still soak: 1.5 times the wielder's maximum health (2.25 times in the Light Metal form, passive `bulwark`). Each blocked blow spends it.
* **Parry.** A block in the first 13 ticks (0.65 s) after the guard went up (the vanilla 5-tick delay plus 8; 17 ticks with `bulwark`) is a parry: it costs no Resolve, it gives
  twice the Counter, and a melee attacker is shoved back. The next parry counts only 20 ticks later, so lowering and raising the guard again and again cannot make every blow a parry.
* **Break.** A blow bigger than the Resolve left goes through for the rest, the guard breaks, the shield is on a 3 s cooldown (60 ticks) and comes back with a quarter of its Resolve.
* **Recovery.** After 1.5 s without a blow the Resolve returns by 10 percent of its capacity per second. The wielder is told once, on the action bar, when it falls under a quarter ('The guard is failing').
* **Counter.** Every point of damage the guard stops also charges the Counter (0 to 100): the damage stopped as a percentage of the wielder's maximum health (a Rage form turns
  more of it into Counter, 1.5 times; a parry doubles it). It holds for 8 s and then fades over about 25 s. The wielder is told at every 25 percent.

So a Shield cannot soak a boss for ever: the player manages the Resolve, times the raise, and decides when to spend the Counter. That is the 'absolute defence' of the owner's text
made into something that can be read and played.

**2. The counters.** The Counter is spent by Change Shield (Attack) (all of it, 6 to 12 raw), Iron Maiden (all of it, 16 to 20 raw) and Needle Burst (half of it, 4 to 10 raw). The
Needle form also answers every blocked melee blow with needles at once, with at most half the blow's final damage (the owner's rule: a reflected answer never exceeds the blow it answers).
To make that exact, the answer asks the balance law what a raw number would come to and scales it to fit under the limit.

**3. The party.** Second Shield (a ward on a friend), Protect (the wielder takes 40 percent of what the friends take), Air Strike Shield (a floating shield that eats projectiles),
Herb Mend (a heal), Soul Aegis (Absorption from eaten souls) and Absolute Defence (a dome). Protect and the ward live in a game event, because the weapon's own hooks only run for the
wielder; the event changes the blow in place and never cancels it. Allies are players the wielder may not harm (team mates, a server with PvP off), tamed pets and the wielder's own pets.

**4. Control.** Shield Prison, Chain Shield, Hook and Undead Control each ask the shared control lock first (`HeroCtx.control()`), take their length from the balance law and never
hold a boss beyond a stagger. They exist to put foes where the party can hit them, not to kill.

**Offence.** The base attack of the form stats is tiny (ordinary forms have 0 to 1 attack damage). The real damage is the Counter: Change Shield (Attack) is at most 12 raw, Iron Maiden at
most 20, Needle Burst at most 10, and each needs the wielder to have taken blows first. The Wrath forms are the one Shield that attacks (attack damage 4 to 12), at the curse's price.

**Where the Soul Eater Shield sits, and why.** The brief allowed a high ordinary form or a Gluttony-flavoured special. It is an ordinary tier-4 form because (1) the series lists it among
the Legendary Shield's normal unlockable shields with Drain Null and Undead Control, not in the curse series (S), (2) the curse engine ties a sin to the weapon's own series (Wrath for the
Shield) and a Gluttony form would need an engine entry the design does not give the Shield, and (3) a player with no curse should be able to reach the strongest ordinary form. Its
flavour is the soul mechanic and a soul-themed requirement (soul sand, wither skulls, daemon essence).

**Not built, on purpose.**
* Wall Escape (an effect of the Soul Eater Shield): it would move the wielder through solid blocks, a way past dungeon walls and the Mega-Dungeon guard, and the job allows no block changes.
* The Shield of Compassion and the Bless Series: out of scope (design.md sections 7 and 18).
* Shooting Star Shield is built (as a thrown comet); the Faust Blood shield and the Nue Shield's scream are not.

## 4. The forms tree

Ten ordinary forms (tiers 0 to 4) and four Wrath forms. Every parent must be learned first. Requirements are common materials at tier 1 and rarer ones above; nothing needs an
item that cannot be obtained in survival. Mastery grants the bonus once the form is finished (a hit is a point, a technique two, a successful block with the form active one). `#` marks an item tag.

| Form (id) | Tier | Parents | Needs (absorbed in the gem) | Mastery | Stats, and the bonus mastery grants | Skills by slot | Passives |
|---|---|---|---|---|---|---|---|
| **Small Shield** (`base`) | 0 | none | none (always learned) | 0 | armor 2 | sneak: Change Shield (Attack)<br>key1: Air Strike Shield<br>key2: Second Shield | none |
| **Leaf Shield** (`leaf_shield`) | 1 | Small Shield | `#minecraft:leaves` x48<br>`minecraft:sweet_berries` x16 | 80 | armor 2.5, max_health 2<br>mastery: max_health +0.5 | sneak: Change Shield (Attack)<br>key1: Herb Mend<br>key2: Air Strike Shield<br>key3: Second Shield | herb_regen |
| **Orange Balloon Shield** (`orange_balloon_shield`) | 1 | Small Shield | `minecraft:slime_ball` x24<br>`minecraft:orange_dye` x16<br>`minecraft:string` x16 | 80 | armor 1.5, movement_speed 0.04, knockback_resistance 0.1<br>mastery: movement_speed +0.01 | sneak: Change Shield (Attack)<br>key1: Shooting Star Shield<br>key2: Air Strike Shield<br>key3: Provoke | balloon_bounce |
| **Slime Shield** (`slime_shield`) | 1 | Small Shield | `minecraft:slime_ball` x48<br>`minecraft:clay_ball` x16 | 80 | armor 2, max_health 4<br>mastery: max_health +1 | sneak: Change Shield (Attack)<br>key1: Chain Shield<br>key2: Provoke<br>key3: Second Shield | gel_absorb |
| **Egg Shield** (`egg_shield`) | 1 | Small Shield | `minecraft:egg` x32<br>`minecraft:feather` x16 | 80 | armor 2, max_health 2<br>mastery: max_health +0.5 | sneak: Protect<br>key1: Second Shield<br>key2: Herb Mend<br>key3: Air Strike Shield | hatchling_guard |
| **Zombie Shield** (`zombie_shield`) | 2 | Slime Shield | `minecraft:rotten_flesh` x64<br>`minecraft:bone` x32 | 140 | armor 3, max_health 6, knockback_resistance 0.15<br>mastery: max_health +1.5, knockback_resistance +0.03 | sneak: Change Shield (Attack)<br>key1: Provoke<br>key2: Hook<br>key3: Protect | rotting_endurance |
| **Chimera Viper Shield** (`chimera_viper_shield`) | 2 | Leaf Shield | `minecraft:spider_eye` x32<br>`minecraft:poisonous_potato` x16<br>`minecraft:fermented_spider_eye` x8 | 150 | armor 3, armor_toughness 1, attack_damage 1<br>mastery: armor +0.5 | sneak: Hook<br>key1: Shield Prison<br>key2: Chain Shield<br>key3: Iron Maiden | venom_fang |
| **Needle Shield** (`needle_shield`) | 2 | Egg Shield | `minecraft:honeycomb` x24<br>`minecraft:iron_ingot` x24 | 140 | armor 3, attack_damage 1<br>mastery: armor +0.5 | sneak: Needle Burst<br>key1: Change Shield (Attack)<br>key2: Shooting Star Shield<br>key3: Provoke | needle_counter |
| **Light Metal Shield** (`light_metal_shield`) | 3 | Zombie Shield, Needle Shield | `tensura:low_magisteel_ingot` x16<br>`minecraft:diamond` x8<br>`minecraft:iron_ingot` x64 | 240 | armor 6, armor_toughness 2, max_health 8, knockback_resistance 0.3<br>mastery: armor +1, armor_toughness +0.5, max_health +2 | sneak: Protect<br>key1: Air Strike Shield<br>key2: Second Shield<br>key3: Change Shield (Attack) | bulwark |
| **Soul Eater Shield** (`soul_eater_shield`) | 4 | Chimera Viper Shield, Light Metal Shield | `minecraft:soul_sand` x64<br>`minecraft:wither_skeleton_skull` x4<br>`tensura:daemon_essence` x8 | 360 | armor 7, armor_toughness 3, max_health 10, knockback_resistance 0.4<br>mastery: armor +1.5, armor_toughness +0.75, max_health +2.5 | sneak: Soul Aegis<br>key1: Undead Control<br>key2: Protect<br>key3: Absolute Defence | soul_eater, drain_null |
| **Shield of Rage** (`rage_shield`)<br>CURSE: wrath, corruption 3/s | 1 | Small Shield | `minecraft:blaze_powder` x24<br>`minecraft:redstone` x32 | 100 | attack_damage 4, armor 3<br>mastery: attack_damage +1 | sneak: Rage Bash<br>key1: Provoke<br>key2: Change Shield (Attack)<br>key3: Second Shield | rage_fury |
| **Shield of Rage II** (`rage_shield_2`)<br>CURSE: wrath, corruption 4/s | 2 | Shield of Rage | `minecraft:rotten_flesh` x64<br>`minecraft:bone` x64<br>`minecraft:ghast_tear` x8 | 180 | attack_damage 7, armor 4, movement_speed 0.04<br>mastery: attack_damage +1.5 | sneak: Rage Bash<br>key1: Rage Roar<br>key2: Change Shield (Attack)<br>key3: Hook | rage_fury |
| **Shield of Wrath III** (`wrath_shield_3`)<br>CURSE: wrath, corruption 6/s | 3 | Shield of Rage II | `minecraft:crying_obsidian` x32<br>`minecraft:ghast_tear` x16<br>`minecraft:fermented_spider_eye` x32 | 260 | attack_damage 10, armor 4, movement_speed 0.06<br>mastery: attack_damage +2 | sneak: Rage Bash<br>key1: Shield Prison<br>key2: Iron Maiden<br>key3: Blood Sacrifice | rage_fury |
| **Shield of Wrath IV** (`wrath_shield_4`)<br>CURSE: wrath, corruption 9/s | 4 | Shield of Wrath III | `minecraft:nether_star` x4<br>`minecraft:dragon_breath` x16<br>`minecraft:wither_skeleton_skull` x8 | 380 | attack_damage 12, armor 5, movement_speed 0.08<br>mastery: attack_damage +2.5 | sneak: Blood Sacrifice<br>key1: Shield Prison<br>key2: Iron Maiden<br>key3: Megiddo Burst | rage_fury |

Reading the tree: the Leaf, Orange Balloon, Slime and Egg shields are the four first steps (healer, mobile defender, tank with control, protector of the party). The Zombie shield (from Slime)
is the tank that holds on, the Chimera Viper shield (from Leaf) the controller with poison, the Needle shield (from Egg) the counter-fighter. The Light Metal shield needs both the Zombie and
the Needle branch and is the wall of the party. The Soul Eater shield needs the Chimera Viper and the Light Metal shield. The Wrath series has its own line from the base shield.

## 5. The techniques

Nineteen techniques: fifteen of the ordinary tree and four of the Wrath series. Any form can list any technique; a form changes how the weapon plays by which ones it puts on its slots
(there is no `use` slot: the use key is the guard). The numbers below are in code (`ShieldNumbers`); raw damage is before the bond factor (1.0 to 2.2) and the balance law.

| Skill | Class | Cooldown | Cost (of max magicules) | Raw per hit (largest) | Hits on one target | Most on one target | Series |
|---|---|---|---|---|---|---|---|
| Change Shield (Attack) (`shield.change_attack`) | damage | 4 s | 1.5% | 12 | 1 | 12 | ordinary |
| Air Strike Shield (`shield.air_strike`) | utility | 7 s | 2% | none | - | none | ordinary |
| Second Shield (`shield.second_shield`) | utility | 10 s | 2% | none | - | none | ordinary |
| Protect (`shield.cover`) | utility | 20 s | 3% | none | - | none | ordinary |
| Provoke (`shield.provoke`) | utility | 8 s | 1.5% | none | - | none | ordinary |
| Herb Mend (`shield.herb_mend`) | utility | 20 s | 3% | none | - | none | ordinary |
| Shield Prison (`shield.prison`) | utility | 15 s | 3% | none | - | none | ordinary |
| Chain Shield (`shield.chain`) | damage | 13 s | 2.5% | 3 | 4 | 12 | ordinary |
| Hook (`shield.hook`) | damage | 7 s | 2% | 4 | 1 | 4 | ordinary |
| Iron Maiden (`shield.iron_maiden`) | heavy | 15 s | 3% | 20 | 1 | 20 | ordinary |
| Needle Burst (`shield.needle_burst`) | damage | 5 s | 2% | 10 | 1 | 10 | ordinary |
| Shooting Star Shield (`shield.shooting_star`) | damage | 6 s | 2% | 9 | 1 | 9 | ordinary |
| Soul Aegis (`shield.soul_aegis`) | utility | 25 s | 3% | none | - | none | ordinary |
| Undead Control (`shield.undead_control`) | utility | 20 s | 3% | none | - | none | ordinary |
| Absolute Defence (`shield.absolute_defence`) | ultimate | 60 s | 7% | none | - | none | ordinary |
| Rage Bash (`shield.rage_bash`) | damage | 4 s | 2% | 10 | 1 | 10 | curse |
| Rage Roar (`shield.rage_roar`) | damage | 15 s | 2% | 4 | 1 | 4 | curse |
| Blood Sacrifice (`shield.blood_sacrifice`) | heavy | 30 s | 3% | 18 | 1 | 18 | curse |
| Megiddo Burst (`shield.megiddo`) | ultimate | 60 s | 8% | 28 | 1 | 28 | curse |

### Change Shield (Attack) (`shield.change_attack`)

* **Class and price:** damage; cooldown 4 s; cost 1.5% of the maximum magicules.
* **What it does:** The shield turns into its striking form for one cleaving blow in a cone in front of the wielder, shoving the foes back (harder with more Counter). All the Counter is spent. Refused, and free, when no foe is in reach.
* **Numbers:** 6 raw with no Counter, 12 raw with a full Counter; cone of 3.6 blocks and 55 degrees to each side; wind-up 6 ticks.
* **Tell:** A cone of green dust on the ground and the sound of the shield changing, 6 ticks (0.3 s) before the blow.
* **How to answer it:** Step out of the cone or raise a guard.
* **Given by:** Small Shield, Leaf Shield, Orange Balloon Shield, Slime Shield, Zombie Shield, Needle Shield, Light Metal Shield, Shield of Rage, Shield of Rage II

### Air Strike Shield (`shield.air_strike`)

* **Class and price:** utility; cooldown 7 s; cost 2% of the maximum magicules.
* **What it does:** A floating shield stands where the wielder looks (three blocks ahead if the wielder looks at a wall right in front). Hostile projectiles that cross it are removed (it takes six, then it shatters); foes that touch it are nudged away. Two can stand at once, a third ends the oldest. It hurts nobody.
* **Numbers:** 3.2 by 3.2 blocks, half a block thick; up to 12 blocks away; 8 s (160 ticks); 6 hits; no damage.
* **Tell:** The shield itself: a pane of light and the sound of a trapdoor closing.
* **How to answer it:** Go round it, or spend six shots on it.
* **Given by:** Small Shield, Leaf Shield, Orange Balloon Shield, Egg Shield, Light Metal Shield

### Second Shield (`shield.second_shield`)

* **Class and price:** utility; cooldown 10 s; cost 2% of the maximum magicules.
* **What it does:** A ward on the friend the wielder looks at (else the nearest friend within 12 blocks, else the wielder) that soaks the next blows that friend takes. A wielder keeps at most three wards; a new ward on the same friend replaces the old one.
* **Numbers:** Bank of 15 percent of the friend's maximum health times a bond factor of 1.0 to 1.6, never above 30 points; 10 s (200 ticks); aim up to 24 blocks.
* **Tell:** A ring of green light circles the warded friend, and a flash shows each soaked blow.
* **How to answer it:** It is a small bank of damage that ends by itself.
* **Given by:** Small Shield, Leaf Shield, Slime Shield, Egg Shield, Light Metal Shield, Shield of Rage

### Protect (`shield.cover`)

* **Class and price:** utility; cooldown 20 s; cost 3% of the maximum magicules.
* **What it does:** Protect. For a while part of every blow that a covered friend takes goes to the wielder instead, and the damage the wielder takes this way also charges the Counter. It is a game-event hook: the blow on the friend is changed in place, never cancelled, and it never loops.
* **Numbers:** 40 percent of each blow; 8 s (160 ticks); the friends within 12 blocks at the cast, up to eight; the wielder must stay within 20 blocks of the friend, be bonded and summoned, and keep above 25 percent of their health (below it the cover gives nothing); a wielder who was just hurt (frames of grace) is not hit by a second redirect.
* **Tell:** A green seal on the ground round the wielder, a line to every covered friend, and a line again at every redirected blow.
* **How to answer it:** Wear the wielder down; the cover ends at a quarter health or when the wielder is more than 20 blocks away.
* **Given by:** Egg Shield, Zombie Shield, Light Metal Shield, Soul Eater Shield

### Provoke (`shield.provoke`)

* **Class and price:** utility; cooldown 8 s; cost 1.5% of the maximum magicules.
* **What it does:** Hostile mobs near the wielder turn on the wielder, who takes less damage while they do. Players and bosses are not affected (a boss's own script decides whom it fights). It is not hard control (a mob can still be killed or lured away).
* **Numbers:** Within 10 blocks, at most 12 mobs; 4 s (80 ticks), re-aimed twice a second; Resistance I (a fifth less damage) on the wielder.
* **Tell:** An ember ring on the ground and a roar.
* **How to answer it:** Kill or out-run the mob; ranged mobs keep shooting the wielder instead of the party, which is the point.
* **Given by:** Orange Balloon Shield, Slime Shield, Zombie Shield, Needle Shield, Shield of Rage

### Herb Mend (`shield.herb_mend`)

* **Class and price:** utility; cooldown 20 s; cost 3% of the maximum magicules.
* **What it does:** The Leaf Shield's herbs: half a second after the cast the wielder and the friends round them are healed and regenerate. It heals friends only.
* **Numbers:** 10 percent of maximum health times the bond factor (never above the law's 25 percent cap) plus Regeneration I for 5 s; radius 5 blocks; wind-up 10 ticks; every heal goes through the balance law.
* **Tell:** A green ring on the ground and herbs falling, half a second before the heal.
* **How to answer it:** None needed: it does no harm.
* **Given by:** Leaf Shield, Egg Shield

### Shield Prison (`shield.prison`)

* **Class and price:** utility; cooldown 15 s; cost 3% of the maximum magicules.
* **What it does:** Needs the shared control lock. A cage of shields shuts round the foe the wielder looks at. The foe is slowed and pulled back whenever it tries to leave the circle; it can still fight from inside it. The foe is marked for the Iron Maiden.
* **Numbers:** Up to 14 blocks away; up to 4 s (80 ticks, shortened by the balance law for a player or a boss); circle of 2.6 blocks; Slowness II; the mark lasts the length plus one second.
* **Tell:** The cage rises round the foe at once. It does no damage.
* **How to answer it:** Kill or distract the wielder, or fight from inside: the cage is slow, not a wall.
* **Given by:** Chimera Viper Shield, Shield of Wrath III, Shield of Wrath IV

### Chain Shield (`shield.chain`)

* **Class and price:** damage; cooldown 13 s; cost 2.5% of the maximum magicules.
* **What it does:** Needs control. Chains bind up to four foes near where the wielder looks. At every pulse each bound foe is cut and drawn towards the middle of the group.
* **Numbers:** 3 raw a pulse, four pulses a second apart (3 s), 12 at most on one foe; the four nearest foes within 6 blocks of the aim point; the pull is at most 0.25 blocks per tick.
* **Tell:** Chains are drawn from the wielder to each foe at the cast, and links between the foes at every pulse.
* **How to answer it:** Kill the wielder or walk out; the pull is gentle.
* **Given by:** Slime Shield, Chimera Viper Shield

### Hook (`shield.hook`)

* **Class and price:** damage; cooldown 7 s; cost 2% of the maximum magicules.
* **What it does:** Needs control. A chain flies to the foe the wielder looks at, hurts it and drags it to the wielder. A boss is only nudged (a quarter of the speed).
* **Numbers:** 4 raw; up to 14 blocks; drag speed up to 1.6 blocks per tick.
* **Tell:** The chain is drawn at the cast. The pull is instant, so the answer is range and cover.
* **How to answer it:** Stay out of the 14-block line or put a wall in it.
* **Given by:** Zombie Shield, Chimera Viper Shield, Shield of Rage II

### Iron Maiden (`shield.iron_maiden`)

* **Class and price:** heavy; cooldown 15 s; cost 3% of the maximum magicules.
* **What it does:** Needs a foe in the wielder's Shield Prison (the one looked at, else the nearest within 16 blocks). After a short wind-up the iron maiden closes on that foe (the cage keeps it near the marked spot) and its mark is released. The Counter is spent into it.
* **Numbers:** 16 raw with no Counter, 20 raw with a full Counter (all of it spent); wind-up 14 ticks.
* **Tell:** A ring and a column of steel dust at the foe's feet for 0.7 s, and the sound of an iron door.
* **How to answer it:** Before it: do not be caged (the Prison needs the control lock and has a 15 s cooldown). After it starts: kill or distract the wielder inside the 0.7 s.
* **Given by:** Chimera Viper Shield, Shield of Wrath III, Shield of Wrath IV

### Needle Burst (`shield.needle_burst`)

* **Class and price:** damage; cooldown 5 s; cost 2% of the maximum magicules.
* **What it does:** The needles of the Needle Shield all at once, to every foe near the wielder, after a short gathering. Needs a foe in reach; if the foes have stepped out by the time the needles fly, nothing is spent.
* **Numbers:** 4 raw with no Counter, 10 raw with a full Counter, to every foe within 4.5 blocks; half of the Counter is spent; wind-up 6 ticks.
* **Tell:** Eight lines of steel dust show the reach for 0.3 s before the needles fly.
* **How to answer it:** Step out of the 4.5 blocks in that time, or stay away from a wielder who has a stored Counter.
* **Given by:** Needle Shield

### Shooting Star Shield (`shield.shooting_star`)

* **Class and price:** damage; cooldown 6 s; cost 2% of the maximum magicules.
* **What it does:** After a short gathering of light at the shield a comet of glass leaves it and strikes the first foe it meets, shoving it. The wielder's aim at the moment of release counts.
* **Numbers:** 9 raw; wind-up 4 ticks; 1.6 blocks per tick for 14 ticks (about 22 blocks); the bolt is 1 block wide.
* **Tell:** Light gathers at the shield for 0.2 s, then a streak of light follows the whole path.
* **How to answer it:** Sidestep after the flash of light, or put something in the way.
* **Given by:** Orange Balloon Shield, Needle Shield

### Soul Aegis (`shield.soul_aegis`)

* **Class and price:** utility; cooldown 25 s; cost 3% of the maximum magicules.
* **What it does:** The Soul Eater Shield spends the souls it has eaten as Absorption for the wielder and the friends round them. A kill within 16 blocks of the wielder gives one soul; at most five are held and they fade 30 s after the last. Refused when there are none.
* **Numbers:** Absorption for 12 s (240 ticks): the wielder gets one level per soul (I to V), friends within 6 blocks about half as many; all souls are spent.
* **Tell:** Souls rise round the wielder.
* **How to answer it:** None needed: it does no harm.
* **Given by:** Soul Eater Shield

### Undead Control (`shield.undead_control`)

* **Class and price:** utility; cooldown 20 s; cost 3% of the maximum magicules.
* **What it does:** Needs control. The undead (not bosses, not tamed) near the wielder lose their target again and again, unless the wielder struck them in the last two seconds. Refused when no undead are near.
* **Numbers:** Within 12 blocks, at most 12 mobs; 6 s (120 ticks), re-applied ten times a second (a mob's own goals look for a target again every few ticks).
* **Tell:** Souls rise and a curse sound plays.
* **How to answer it:** Strike the undead (it answers at once), or use a living attacker.
* **Given by:** Soul Eater Shield

### Absolute Defence (`shield.absolute_defence`)

* **Class and price:** ultimate; cooldown 60 s; cost 7% of the maximum magicules. An ultimate: one per form at most.
* **What it does:** Absolute Defence. A dome of iron stands where the wielder cast it. Friends inside take far less damage (the wielder most), hostile projectiles in it are removed, foes inside are pushed out. The wielder is slowed all the time and deals no damage with it. The dome stays where it is drawn: a wielder who leaves it loses its protection.
* **Numbers:** 5 s (100 ticks), radius 6.5 blocks; Resistance III for friends (60 percent less damage), Resistance IV for the wielder (80 percent less); Slowness III on the wielder. Ultimate: cost 7 percent, cooldown 60 s.
* **Tell:** The dome and a circle on the ground, visible for all 5 s, with a beacon sound.
* **How to answer it:** Wait outside the dome; it is five seconds out of a minute.
* **Given by:** Soul Eater Shield

### Rage Bash (`shield.rage_bash`)

* **Class and price:** damage (curse series); cooldown 4 s; cost 2% of the maximum magicules.
* **What it does:** (Wrath) The Shield of Rage lunges and slams: everything near the point of impact is hurt, set alight and shoved. Needs a foe within about 7 blocks.
* **Numbers:** 10 raw; lunge up to 4 blocks; radius 2.2; 3 s of fire; wind-up 6 ticks.
* **Tell:** A red line along the lunge and an ember ring at the landing, 0.3 s before.
* **How to answer it:** Leave the ring.
* **Given by:** Shield of Rage, Shield of Rage II, Shield of Wrath III

### Rage Roar (`shield.rage_roar`)

* **Class and price:** damage (curse series); cooldown 15 s; cost 2% of the maximum magicules.
* **What it does:** (Wrath) A roar shoves back the foes in a cone and, if the control lock is free, slows them. The wielder is filled with strength.
* **Numbers:** 4 raw; cone of 8 blocks and 60 degrees to each side; Slowness II for up to 3 s (the law decides); Strength I on the wielder for 6 s; wind-up 10 ticks.
* **Tell:** A red cone of dust and the growl of a dragon, 0.5 s before.
* **How to answer it:** Leave the cone.
* **Given by:** Shield of Rage II

### Blood Sacrifice (`shield.blood_sacrifice`)

* **Class and price:** heavy (curse series); cooldown 30 s; cost 3% of the maximum magicules.
* **What it does:** (Wrath) A ring of blood is drawn under the foe the wielder looks at; a second later a trap of blood closes on whatever stands in the ring. The ring stays where it was drawn, so a foe that walks out is missed, and the blood is paid only if the trap catches something.
* **Numbers:** 18 raw to the foe that was looked at, 8 raw to every other foe in the ring (radius 3), a root (Slowness VII) for up to 1.5 s if the lock is free. The wielder pays 25 percent of their maximum health, never going below 30 percent, and is then Weakened and Slowed for 15 s. In a curse form: +80 corruption. Aim up to 12 blocks; wind-up 20 ticks.
* **Tell:** A blood sigil and a red ring on the ground for a second, and a red gathering at the wielder.
* **How to answer it:** Walk out of the ring.
* **Given by:** Shield of Wrath III, Shield of Wrath IV

### Megiddo Burst (`shield.megiddo`)

* **Class and price:** ultimate (curse series); cooldown 60 s; cost 8% of the maximum magicules. An ultimate: one per form at most.
* **What it does:** (Wrath) After a long wind-up the circle on the ground flares and fire burns everything in it. The wielder pays in blood and is left weakened. The burst is centred on the circle drawn where the wielder stood at the cast.
* **Numbers:** 28 raw to everything within 9 blocks of the circle, set alight for 4 s and shoved; wind-up 40 ticks (2 s). The wielder pays 30 percent of their maximum health, never going below 25 percent, and is then Weakened II and Slowed for 15 s. Ultimate: cost 8 percent, cooldown 60 s. In a curse form: +150 corruption.
* **Tell:** A growing red ring, a second ring, fire gathering and a deep charging sound for 2 s.
* **How to answer it:** Leave the circle: nine blocks in two seconds is a sprint.
* **Given by:** Shield of Wrath IV

## 6. The passives

| Passive | id | Forms | What it does |
|---|---|---|---|
| Balloon Landing | `balloon_bounce` | Orange Balloon Shield | The wielder takes no damage from falls. |
| Bulwark | `bulwark` | Light Metal Shield | The guard holds half as much again, and the parry window is wider. |
| Drain Null | `drain_null` | Soul Eater Shield | The wielder cannot be withered or starved by effects. |
| Gel Cushion | `gel_absorb` | Slime Shield | A quarter of every blow is soaked up by the gel, at most 8 percent of the wielder's health for one blow. |
| Hatchling Guard | `hatchling_guard` | Egg Shield | The wielder's pets within 12 blocks regenerate. |
| Herbal Regrowth | `herb_regen` | Leaf Shield | While the fight is quiet (three seconds since the last blow) the Leaf Shield slowly mends its wielder. |
| Needle Counter | `needle_counter` | Needle Shield | A melee blow stopped by the guard is answered with needles: at most half the blow's strength, at most once per attacker every half second. |
| Rage Fury | `rage_fury` | Shield of Rage, Shield of Rage II, Shield of Wrath III, Shield of Wrath IV | Every blow the wielder takes fills them with strength for 4 seconds (stronger if hit again), and the damage the guard soaks becomes half as much Counter again. |
| Rotting Endurance | `rotting_endurance` | Zombie Shield | Once a minute, a blow that would take the wielder under 30 percent of their health is answered with regeneration and a moment of toughness. |
| Soul Eater | `soul_eater` | Soul Eater Shield | Foes the wielder kills within 16 blocks leave souls (up to five) for Soul Aegis. |
| Viper's Fang | `venom_fang` | Chimera Viper Shield | A mob that strikes the guard is poisoned for 4 seconds (not a player, not a boss). |

A passive is an id in the forms file that the logic checks with `c.form.has(id)`; the name and text come from the lang fragment (`hero.passive.shield.<id>`).

## 7. The Wrath series: what it costs the wielder, and why

**What the curse engine does** (design.md section 7, built by the curse builder): while a Wrath form is active the wielder burns and loses health every second but strikes harder, and the
corruption (0 to 1000) rises by the form's `corruptionPerSecond` (3, 4, 6 and 9 for the four forms) and by each technique. At 1000 the weapon takes over for 10 to 20 seconds. The series is
unlocked only by the player's deliberate acceptance after despair, with a permanent price. None of that is in this weapon's files.

**What this weapon adds, and why:**
* **Rage Fury** (all four forms). Each blow the wielder takes gives Strength for 4 s (stronger when hit again inside it) and turns 1.5 times as much of what the guard soaks into Counter. The
  series' first Wrath shield carries a strength boost (S); here being hit feeds the wielder, so a Rage Shield is a bruiser that wants to be hit, but it still guards.
* **Rage Bash and Rage Roar** are the offence the ordinary tree lacks: the cursed Shield is the one Shield that attacks.
* **Blood Sacrifice** (Wrath III and IV) and **Megiddo Burst** (Wrath IV) pay in health: 25 percent (never below 30 percent) and 30 percent (never below 25 percent). The floors mean
  they can never kill the wielder, and a trap that catches nothing costs nothing. Afterwards Weakness and Slowness for 15 s stand in for the series' 'cut all stats by 30 percent except defence' (S).
  In a curse form they also add corruption (+80 and +150). If the same techniques are used from an ordinary form (any form may list them) they add none, because the sin is the form's.
* **Why bounded:** the owner's rule is that nothing leaves the wielder dead by its own price, and the engine's rule is that the curse itself never takes the last heart. Two floors and a
  corruption number are all the weapon needs to keep both.
* **The price in short:** burning and health loss (engine), a 15 s weakness after the two blood techniques (weapon), corruption per second and per technique (engine and weapon), the risk of the Overrun at 1000 (engine).

## 8. Engravings (the owner's rule 6)

The lead adds this line to `item/WeaponEngravings.java` (the file is shared; I did not edit it):

```java
Map.entry("legendary_shield", List.of(e("sturdy", 3), e("magic_protection", 2), e("magic_interference", 1), e("holy_weapon", 1))),   // the wall of the party: hard to break, hard to spell on, one of the four Holy Weapons
```

| Engraving | Level | Why |
|---|---|---|
| `sturdy` | 3 | The Shield is the wall of the party; the engraving that makes a weapon hard to break fits a weapon whose job is to be hit. |
| `magic_protection` | 2 | Absolute defence is also against spells: the Mega-Dungeon's casters, the Wrath fire, the magic of the foes it covers the party from. |
| `magic_interference` | 1 | The Soul Eater Shield's Drain Null and the Shield's whole role are to stop magic from landing; this is the Tensura engraving that interferes with it (the Magebane Shield in the table has it for the same reason). |
| `holy_weapon` | 1 | It is one of the four Holy (Legendary) Weapons. |

A level above an engraving's maximum is capped by the engraving code; an id Tensura lacks is skipped.

## 9. Numbers and the balance law

The budget of design.md section 5, and how this weapon keeps it (every point is a test in `ShieldNumbersTest` unless marked 'code' or 'event'):

* Raw damage per hit is at least 3 and at most 12 for an ordinary technique, 10 to 20 for a heavy one, at most 32 for an ultimate; the most one target takes from one cast is also in class.
  The largest are Megiddo Burst (28, ultimate) and Iron Maiden (20, heavy).
* Cooldown at least 60 ticks for a damage technique, 100 for a utility, 600 for an ultimate; cost 1 to 3 percent of the magicules, an ultimate 5 to 8 percent.
* Every damage goes through `HeroCtx.hit` (the balance law: existence scaling, the PvP cap, the boss multiplier). Every heal goes through `HeroCtx.heal` (Herb Mend: at most the law's quarter).
* Every control (Prison, Chain Shield, Hook, Undead Control, the root of Blood Sacrifice, the slow of Rage Roar) asks `HeroCtx.control()` first and takes its length from
  `BalanceLaw.controlTicks`; a boss is held for a second at most. Provoke is aggro, not hard control, and affects mobs only (no players, no bosses).
* At most 12 targets per technique (`ShieldTargets` cuts the list). No entity is spawned by any technique: the floating shields are logical panels with an effect, the souls are numbers. No
  block is placed or broken. No task runs longer than 20 s (the longest are the 8 s panel and the 8 s cover, which is a book entry).
* The wielder cannot be killed by their own price: Blood Sacrifice and Megiddo Burst leave 30 and 25 percent of the maximum health; Protect never takes the wielder under 25 percent.
* A reflected answer never exceeds half of the blow it answers (the Needle form), and the cover redirects 40 percent of a blow at most.
* The Resolve is bounded (1.5 or 2.25 times the maximum health) and breaks; a parry has a pause; the Counter fades. Without these the Shield would be a free win.
* Hooks never throw: each one catches `RuntimeException` and `LinkageError` and reports once through `SafeCast.fault`.
* State: one `ShieldState` per wielder in a map that is dropped on logout and on `ServerStoppedEvent`; the cover book is bounded (eight covered, three wards per wielder); all of it is a cache,
  nothing is saved, so old worlds load unchanged.

## 10. Files, classes and tests (the why of each)

| File | Why it exists |
|---|---|
| `weapon/shield/ShieldNumbers.java` | Every number of the weapon in one place: a table with a row per technique (cooldown, cost, raw damage, hits) and the constants of the guard, the cover, the panels, the Wrath price. The skills read their cooldown and cost from it and the test walks it against the budget, so a later edit cannot break the balance without a test noticing. |
| `weapon/shield/GuardMeter.java` | The guard as a small machine with no game types: Resolve, parry, break, Counter, recovery. It is the reason 'absolute defence' is a defence a player can read and manage instead of a free win. Tested alone. |
| `weapon/shield/CounterMath.java` | The arithmetic of the Counter: how much a block charges, how a counter attack scales with the charge, how a reflected answer is fitted under the blow it answers (the owner's rule), and how much health a technique may cost. Tested alone. |
| `weapon/shield/CoverBook.java` | The memory of Protect (who is covered by whom) and Second Shield (who carries a ward and how much it can still soak), bounded, with the redirect arithmetic. Tested alone. |
| `weapon/shield/PanelMath.java` | The geometry of a floating shield: where its face is, whether a point is on it, whether a projectile's step crossed it. Tested alone. |
| `weapon/shield/SoulPool.java` | The souls of the Soul Eater form: bounded, expiring, spent all at once. Tested alone. |
| `weapon/shield/HitCap.java` | A per-target cap for one cast, so overlapping pulses cannot pass the technique's budget. Tested alone. |
| `weapon/shield/ShieldState.java` | What one wielder carries from call to call (guard, souls, cages, floating shields, passive timers), one plain object per wielder with bounded tables. |
| `weapon/shield/ShieldTargets.java` | Who the Shield may hurt and help: one place, so every technique asks the same question (never a creative or spectator player, a team mate, a pet, a villager or a peaceful animal). |
| `weapon/shield/ShieldFx.java` | The effects the techniques use, named (`shatter`, `cage`, `pane`, `seal` ...), built from `VfxSpawn` shapes, `Tells` and sounds only, so the techniques read plainly and no vanilla particle is used. |
| `weapon/shield/ShieldRuntime.java` | The memory of the promises that outlive a cast, the one game event where blows to OTHER people are seen (the cover and the ward), and the two scheduling helpers every technique uses. Everything it keeps is cleared when a wielder leaves or the server stops. |
| `weapon/shield/ShieldOps.java` | What each technique does, one static method per technique, top to bottom. |
| `weapon/shield/ShieldSkills.java` | The list of skills with their names and texts; each takes its cooldown and cost from the table. |
| `weapon/shield/ShieldLogic.java` | The hooks: the guard (`onBlocked`), the passives (`onTick`, `onDamaged`), souls (`onKill`), cleanup (`onFormChanged`, `onDismiss`). |
| `weapon/shield/ShieldSelfTest.java` | The in-game self-test `hero.weapon.shield`: table against registered skills, forms file against skills and passives, then a cast of every skill with a stand-in player. |
| `data/nusmp/hero/forms/legendary_shield.json` | The forms: the base shield, ten ordinary forms and the four Wrath forms. |
| `test/.../shield/ShieldNumbersTest, GuardMeterTest, CoverBookTest, PanelMathTest, ShieldSmallPartsTest` | The budget of design.md section 5 as a test, and the pure parts (guard, cover, panel, souls, caps, counter arithmetic). |
| `test/.../shield/FormsFileCheck, ShieldFormsDataTest` | A checker for every rule of design.md section 6 applied to the shipped file, and negative controls proving the checker rejects broken copies. The Sword's test uses the same checker. |

Every Java class opens with a Javadoc whose first paragraph starts with `Why:`. The self-test is registered from the logic's constructor (`SelfTests.register("hero.weapon.shield", ...)`), which runs at start-up.

## 11. For the lead and the other builders

* **Engravings:** the line in section 8.
* **Lang fragment:** `$S/hero/frag/hsa.lang.json` (120 keys for both weapons: name and text of every skill, name and text of every passive). Generated from the source, ASCII only.
* **Forms file:** `src/main/resources/data/nusmp/hero/forms/legendary_shield.json`. The first form is `base`; no form uses the `use` slot (a blocking weapon has none).
* **Constructors:** `HeroBootstrap` must construct `new ShieldLogic()` once at start-up (the existing stub's constructor is replaced by one that hooks the events and registers the self-test).
* **Curse engine:** the weapon calls `HeroCurse.addCorruption(ServerPlayer, int)` from Blood Sacrifice (+80) and Megiddo Burst (+150), only when the active form is a curse form. The sin downsides
  (burning, health loss, stronger blows) are the engine's.
* **Core:** the guard break puts the weapon's item on cooldown for 60 ticks through the player's `ItemCooldowns` and stops the use; the core must let that cooldown stand (not clear it on re-summon).
* **Art:** sizes and parts are in section 1; the Wrath look is the `curse` look of the forms.
* **Config:** none (a weapon builder keeps its numbers in code).

## 12. What is not verified

* **Nothing ran in the game.** What ran: the pure classes' tests (110 JUnit tests for both weapons through a small local shim, the real JUnit runs in CI) and a type-check of the glue against API stubs (the stubs reported only the known artifact 'MobEffectInstance is abstract' in my files, the same as in older files of the repo; a deliberate wrong call proved the checker sees my code).
* **The hook contract was read, not exercised.** I assume `onBlocked(c, source, amount)` is called when the raised guard blocks a blow, with the damage that reached the guard, and that its return is the damage that goes through; that `p.getTicksUsingItem()` is the number of ticks the guard has been up at that moment (the parry window depends on it); and that the core does not also reduce the blow itself.
* **Vanilla shield rules.** I assume the core's item blocks like a vanilla shield (only from the front, with the 5-tick delay) and do not know whether it reacts to an axe disabling shields; the break uses an item cooldown on the stack's item, which should show as the usual shield cooldown.
* **`LivingIncomingDamageEvent` order.** Protect and Second Shield change the blow in the event of the covered friend and hurt the wielder with `hurt(source, amount)`; that second blow goes through the core's hooks like any other blow on the wielder. I did not verify how the core orders its own listener against mine.
* **Numbers are desk numbers.** Nothing was play-tested: the Counter rate, the Resolve multiple (1.5 and 2.25 times the maximum health), the parry window, the size of Absolute Defence's resistance, Megiddo's 28 raw on up to twelve foes, the cover share. The tests pin the budget, not the feel.
* **Effects were never seen.** `ShieldFx` composes `VfxShape` values and `Tells` by reading their code and signatures; the shapes exist (the file compiles against them) but how they look, how long they stay and whether two of them clash is unknown. The art builder and the lead should look at them in game.
* **Tensura requirements.** `tensura:low_magisteel_ingot` and `tensura:daemon_essence` were checked against the owner's id reference table, not against the live registry; the `hero.forms` self-test of CI resolves every id. Tags used: `#minecraft:leaves`.
* **Interpretation of the budget.** design.md section 5 says 'raw base damage per hit: ordinary 3..12'. I read it as: no single hit below 3 raw (a test pins it), and the total one target can take from one cast is also within the class (12 / 20 / 32); the answer of the Needle form to a small blow is the one exception below 3, because the owner's rule caps a reflected answer at the blow it answers.
* **A blocked blow and the balance law.** The Needle answer asks the law what a raw number would come to (`BalanceLaw.damage` as a probe) and scales it to fit the limit. If that method has a side effect I did not see, the probe would repeat it.
* **Frames of grace.** My strikes set `invulnerableTime` to 10 on the target before `c.hit`, so a technique that lands right after a melee blow is not swallowed. I did not verify what `c.hit` does with it internally.
