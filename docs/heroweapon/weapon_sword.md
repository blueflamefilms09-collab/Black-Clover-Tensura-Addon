# The Legendary Sword (`nusmp:legendary_sword`, `HeroKind.SWORD`, style SWING, curse series: Greed and Gluttony)

> Status: complete. Section 1 (the look) is what the art builder reads. Sections 2 to 12 are the research, the design, every form, skill and number, the engravings,
> the files and an honest list of what is not verified. Nothing here has been run in the game.
> Sections 4 to 6 (the forms, the techniques and the passives) are generated from the source and the forms file, so they cannot disagree with the code.

## 1. The look (for the art builder: `hart1`)

Everything here is from the series as far as the wiki search summaries tell, and from memory where marked (M). Nothing was checked against a picture from this machine.

**What it is.** The Small Sword of the series: a straight, double-edged hero's sword, slim and fast, a plain cross guard with ONE jewel set in the middle of it (blue for the
Sword (M); the series puts a jewel on the back of the hand of every Legendary Weapon). Noble and plain at the base look: clean steel and one bright colour.

**Size class.** A `sword` class: 1.5 blocks long overall (1.4 to 1.8 as the art brief asks): blade 1.05, ricasso 0.06, guard 0.28 wide and 0.05 thick, grip 0.2, pommel 0.06.
The tip points up-forward on the icon diagonal and the mesh's -x is forward (the owner's convention, `docs/weapon_scale_orientation.md`).

**Named parts (8 to 14 allowed, this is the intended list).**
| part | shape | notes |
|---|---|---|
| `blade` | a loft of a flattened diamond section, 0.075 wide at the base tapering to a point; a slight distal taper | bright steel, a visible central ridge; the edges bevelled |
| `fuller` | a shallow groove along the middle of the blade for 70 percent of its length | blue-tinged (0x6FA8E8) at the bottom of the groove |
| `edges` | two thin brighter bevel lines | they make the blade read as sharp |
| `ricasso` | a short unsharpened block above the guard | plain steel |
| `guard` | a cross guard, quillons drooping a little towards the blade, end caps | silver with navy inlay lines |
| `gem` | a cabochon (half a flattened sphere), radius 0.05, set in the middle of the guard on both faces | THE jewel: sapphire blue, on the glow skin at full strength |
| `grip` | a cylinder with 5 leather wrap rings | dark navy leather |
| `pommel` | a flattened sphere | silver, a small ring in the centre |

**Palette (0xRRGGBB).** Blade steel 0xDCE6F2, shade 0x8FA0B5, fuller blue 0x6FA8E8, guard silver 0xB9C4D2, navy 0x1F3A66, grip leather 0x1B2748. The jewel: sapphire 0x2E7BE6 with a
darker core 0x123C8A. Glow skin: the jewel at full strength, the fuller at about 30 percent.

**Curse look: Greed and Gluttony.** The SAME footprint (same length, same grip, same guard width so the hand and the animation fit) made monstrous:
* the blade turns black iron (0x15121A) with a gold line along each edge (0xD9A520, the colour of Greed in the code) and the fuller becomes a row of small fangs pointing at the tip,
  the "hungry teeth along the fuller";
* the guard becomes a maw: the two quillons are jaws that curve towards the blade, each with 4 to 6 fangs, so the guard looks like an open mouth around the blade's root;
* the jewel turns into a slit-pupil eye: amber 0xD9A520 with a dark brown slit (Gluttony's colour is 0x8B4A1F), standing further out in the mouth;
* the grip keeps its wrap but the leather turns dark brown with gold thread; the pommel gets a small crown of three spikes;
* glow skin: the gold edge lines, the fangs along the fuller and the eye (about 80 percent).

**Inventory icon (16 x 16).** A diagonal blade with a blue dot on the guard, on a transparent ground, in the style of the other sword sprites.

## 2. Canon: what the series says, and where each fact comes from

The wiki pages themselves could not be opened from the build machine (the fandom host is unreachable from it), so every canon fact below comes from the SUMMARIES that a
web search returned for these pages. Those summaries are fan-compiled and sometimes disagree with each other. Provenance marks: **S** = stated in a search summary,
**M** = from my memory of the series and not checked, **D** = my own design for this mod.

Pages the summaries came from:
* https://shield-hero.fandom.com/wiki/Legendary_Sword and https://shield-hero.fandom.com/wiki/Legendary_Sword_(Web_Novel)
* https://shield-hero.fandom.com/wiki/Ren_Amaki, https://shield-hero.fandom.com/wiki/Ren_Amaki/Powers_and_Abilities, https://shield-hero.fandom.com/wiki/Ren_Amaki_(Web_Novel)
* https://shield-hero.fandom.com/wiki/Curse_Series, https://shield-hero.fandom.com/wiki/Curse_Series/Weapon_List, https://shield-hero.fandom.com/wiki/Greed_Series_(Web_Novel),
  https://shield-hero.fandom.com/wiki/Gluttony_Series_(Web_Novel)
* https://shield-hero.fandom.com/wiki/Legendary_Weapons and https://shield-hero.fandom.com/wiki/Legendary_Weapons/Weapon_Effects
* https://shield-hero.fandom.com/wiki/Vassal_Katana, https://shield-hero.fandom.com/wiki/Spirit_Tortoise, https://shield-hero.fandom.com/wiki/Items/Weapons, https://shield-hero.fandom.com/wiki/Flash

| Fact | Mark | What the mod does with it |
|---|---|---|
| Hundred Swords: the blade is raised and splits into hundreds of smaller swords that rain down on the enemies (the name 'Red Hand Sword' is a mistranslation). | S | Hundred Swords: four waves on a circle where the wielder points. |
| Lightning Hundred Swords is the combined version that rains lightning-covered blades; the Lightning Sword is described as a combination of Liberation Magic Enchantment and the compound magic Thunder God. | S | Lightning Hundred Swords is the ultimate of the tree; the top form is called Thunder God Sword (my name, from the compound magic). |
| Flashing Sword makes the blade flash and temporarily blinds the target; the Flash Sword is the form with it. | S | Flash Sword form and the Flashing Sword technique; a mob is blinded, a player only lit up and slowed. |
| Gravity Sword increases gravity around the target to restrict its movement; it comes with the Spirit Tortoise Sword (obtained by weapon copy, with a Sharpness Bonus). | S | Spirit Tortoise Sword with the `gravity_edge` passive and the Gravity Sword technique. |
| The Spirit Tortoise Carapace Sword is unlocked by absorbing parts of the butchered Spirit Tortoise guardian beast and gives the Float Sword skill. | S | Spirit Tortoise Carapace Sword with `carapace_guard` and the Float Sword technique; requirements are shells and carapaces. |
| Phoenix Gale Blade charges the sword with red energy and unleashes a maelstrom of flames, from which a bird of fiery energy charges at the target; the Phoenix Sword has it as its equip bonus. | S | Phoenix Sword and the Phoenix Gale Blade technique (a storm and a bird). |
| Eagle Blade allows a defence-ignoring attack. | S | Eagle Blade: an armour-piercing lunge (the point ignores 60 percent of armour, capped by the budget). |
| Meteor Sword charges the blade with magic energy and releases a burst of stars; the Sword's first offensive skill is also called Shooting Star Sword. | S | Meteor Sword form and technique: a ring of falling stars; the form's passive calls a star on every fifth blow. |
| The Heavenly Wind King Katana was made by Ren with Filo's feathers; it gives charge reduction, an agility bonus on level-up, wind magic and Wind Cutter Whistle (the Vassal Katana page calls the skill Wind Wing Cutter). | S | Heavenly Wind King Katana form with the `wind_walker` passive and the Wind Cutter Whistle technique (three crescents). |
| The web novel page lists Meteor Sword, Transfer Sword and Assassin Sword as skills. | S | Assassin Sword form and technique (a blink behind the foe). Transfer Sword is not built. |
| The Legendary Sword grows by Mastery (level 100), by converting mastery into energy and by Awakening a form with that energy. | S | Handled by the forms system (mastery points, absorption), not by this weapon's logic. |
| The Sword of Greed line: the second sword gives Assassinating Sword and Guillotine; the third adds Maneater and Gold Rebellion; the combined Sword of Greed and Gluttony III adds Strong Decline, an earth-splitting attack and very high automatic self-repair (the wiki says Eclair's provocation of Ren unlocked it). | S | Greed forms `sword_of_greed`, `sword_of_greed_3`; Gluttony forms `sword_of_gluttony`, `sword_of_greed_gluttony_3`; Guillotine, Gold Rebellion, Maneater, Strong Decline as techniques. The self-repair is the Maneater and Maw heals. |
| Guillotine drops a blade onto the target and steals SP once it breaches the defence; Strong Decline splits the ground beneath the target and sends fangs and flies at it; Gold Rebellion's effects are unknown. | S | Guillotine steals magicules (the mod's SP); Strong Decline is a split line with fangs; Gold Rebellion (a ring that gilds foes) is my design. |
| The downsides: using the Gluttony sword made Ren unable to gain EXP and dropped his level; after Guillotine and Strong Decline he cannot collect EXP or hold anything worth money; the Greed line makes him violent and willing to steal gear, the Gluttony line murderous in pursuit of EXP. | S | The sin downsides are the curse engine's (design.md section 7: Greed pulls drops in and makes techniques cost more, Gluttony drains hunger and lets kills heal). The weapon adds corruption on Guillotine (+40), Strong Decline (+30) and Insatiable Maw (+100). |
| No page in the search results mentioned a 'Blitz Strike'. | S (absence) | Blitz Strike is the brief's name (memory); it is the base form's technique of the use key. Tempest Dance, Afterimage Step, Skyfall Cleave and Insatiable Maw are my design. |
| Hundred Sword as a form name, the Small Sword as the starting shape. | M | Forms `base` and `hundred_sword` (tier 4, from the Phoenix Sword and the Wind King Katana). |

## 3. How the Sword plays in this mod

The owner's text: high-speed physical damage and agility, heavy offensive strikes, later curse series variations. It is the fastest melee weapon of the set, and the design is a rhythm:
keep hitting and the weapon gets faster and then pays you back; stop and it forgets.

**1. The Tempo (the combo).** Every melee blow on a foe within 2 s (40 ticks) of the last one adds 1 to the Tempo (to 10); a longer gap starts again at 1; a kill adds 2 and never finishes
it alone. From Tempo 3 the wielder has Speed I and from 7 Speed II (one blow earlier with the Wind Walker passive); the speed is refreshed every 5 ticks while the Tempo stands. The wielder
is told at 3, 7 and 10.

**2. The Flow.** When the Tempo is 10 and the blow is a full swing (the attack bar was full, so clicking fast does nothing), the blow earns a Flow strike of 6 raw and the Tempo is spent.

**3. The ghost.** Blitz Strike, Afterimage Step and Assassin Sword arm a ghost for 3 s: the next blow earns a ghost strike of 4 raw. So the techniques open the rhythm and the blows close it.

**4. One bonus per blow.** A blow earns at most ONE bonus strike, the largest candidate among the Flow (6), the ghost (4), the form's passive strike (3 to 4) and a gilded foe (3); the
losers keep their charge for the next blow. Every bonus is at most 6 raw, goes through the balance law, and cannot start another bonus (the weapon's own strikes are marked, so they are not
taken for melee blows). That rule is the one thing that must never break, and it is a pure class with tests (`HitLedger`).

**5. Passives that change the rhythm:** the opening strike of the Flash Sword, the stab in the back of the Assassin Sword, the star on every fifth blow of the Meteor Sword, the blades on
every tenth blow of the Hundred Sword, the arc of the Thunder God Sword, the drag of the Spirit Tortoise Sword, the fire of the Phoenix Sword, the carapace that halves a heavy blow.

**6. Greed and Gluttony.** Greed steals: each blow takes 0.3 percent of the foe's maximum magicules (at most every half second) and the wielder keeps at most 1 percent of their own
maximum; Guillotine takes 4 percent (keeping 2); Gold Rebellion gilds foes so the next blow on them earns a bonus. Gluttony feeds: Fang Maw adds 3 raw against a foe under 30 percent of its
health, Maneater heals 4 percent of the wielder's health per foe bitten, Insatiable Maw 6 percent per wave that bites. All heals go through the balance law (a quarter at most).
The cost in hunger, drops and technique prices is the curse engine's.

**Readable by design.** Every technique above 8 raw shows its tell before it lands (a line, a ring, a cone, a column) and has a way to be answered; the techniques are in section 5.

**Not built, on purpose.** Transfer Sword (a teleport the wielder cannot aim; the Afterimage Step and the Assassin Sword's blink are the mod's versions); the Bless and Compassion lines;
the gaining-no-experience downside of the Gluttony sword (the restrictions system owns experience).

## 4. The forms tree

Ten ordinary forms (tiers 0 to 5) and four curse forms (two Greed, two Gluttony). The combined Sword of Greed and Gluttony III takes Gluttony as its sin (a form has one) and keeps Greed's
Plunder, so it has both passives and the techniques of both. Every parent must be learned first. A form's tier is its depth in the tree, 1 + the highest tier among its parents (the rule of the
forms validator), so a form that is meant to be tier 3 hangs under a tier-2 form. `#` marks an item tag.

| Form (id) | Tier | Parents | Needs (absorbed in the gem) | Mastery | Stats, and the bonus mastery grants | Skills by slot | Passives |
|---|---|---|---|---|---|---|---|
| **Small Sword** (`base`) | 0 | none | none (always learned) | 0 | attack_speed 0.2 | use: Blitz Strike<br>sneak: Tempest Dance<br>key1: Afterimage Step | none |
| **Flash Sword** (`flash_sword`) | 1 | Small Sword | `minecraft:glowstone_dust` x32<br>`minecraft:quartz` x24 | 80 | attack_damage 1, attack_speed 0.3<br>mastery: attack_speed +0.05 | use: Blitz Strike<br>sneak: Flashing Sword<br>key1: Afterimage Step<br>key2: Tempest Dance | quick_draw |
| **Heavenly Wind King Katana** (`wind_king_katana`) | 1 | Small Sword | `minecraft:feather` x64<br>`minecraft:string` x32 | 80 | attack_damage 1, attack_speed 0.4, movement_speed 0.04<br>mastery: movement_speed +0.01 | use: Blitz Strike<br>sneak: Wind Cutter Whistle<br>key1: Afterimage Step<br>key2: Tempest Dance | wind_walker |
| **Meteor Sword** (`meteor_sword`) | 2 | Flash Sword | `minecraft:amethyst_shard` x32<br>`minecraft:iron_ingot` x32<br>`minecraft:gold_ingot` x16 | 140 | attack_damage 3, attack_speed 0.2<br>mastery: attack_damage +0.5 | use: Blitz Strike<br>sneak: Meteor Sword<br>key1: Skyfall Cleave<br>key2: Wind Cutter Whistle | star_shower |
| **Assassin Sword** (`assassin_sword`) | 2 | Flash Sword | `minecraft:ender_pearl` x16<br>`minecraft:ink_sac` x16<br>`minecraft:spider_eye` x16 | 140 | attack_damage 2, attack_speed 0.5, luck 1<br>mastery: attack_speed +0.1 | use: Assassin Sword<br>sneak: Blitz Strike<br>key1: Afterimage Step<br>key2: Flashing Sword | shadow_blade |
| **Spirit Tortoise Sword** (`spirit_tortoise_sword`) | 2 | Heavenly Wind King Katana | `tensura:armorsaurus_scale` x16<br>`minecraft:prismarine_shard` x32 | 150 | attack_damage 3, armor 2, knockback_resistance 0.1<br>mastery: armor +0.5 | use: Blitz Strike<br>sneak: Gravity Sword<br>key1: Tempest Dance<br>key2: Eagle Blade | gravity_edge |
| **Spirit Tortoise Carapace Sword** (`tortoise_carapace_sword`) | 3 | Spirit Tortoise Sword | `tensura:armorsaurus_shell` x12<br>`tensura:giant_ant_carapace` x24<br>`minecraft:iron_ingot` x32 | 240 | attack_damage 4, armor 4, armor_toughness 1<br>mastery: armor +1 | use: Blitz Strike<br>sneak: Float Sword<br>key1: Gravity Sword<br>key2: Tempest Dance<br>key3: Skyfall Cleave | carapace_guard |
| **Phoenix Sword** (`phoenix_sword`) | 3 | Meteor Sword | `minecraft:blaze_rod` x24<br>`minecraft:magma_cream` x24<br>`minecraft:ghast_tear` x6 | 250 | attack_damage 6, attack_speed 0.3<br>mastery: attack_damage +1 | use: Blitz Strike<br>sneak: Phoenix Gale Blade<br>key1: Eagle Blade<br>key2: Skyfall Cleave<br>key3: Wind Cutter Whistle | ember_edge |
| **Hundred Sword** (`hundred_sword`) | 4 | Phoenix Sword, Heavenly Wind King Katana | `minecraft:iron_ingot` x64<br>`minecraft:diamond` x12<br>`tensura:mithril_ingot` x8 | 330 | attack_damage 8, attack_speed 0.4<br>mastery: attack_damage +1.5 | use: Blitz Strike<br>sneak: Hundred Swords<br>key1: Meteor Sword<br>key2: Eagle Blade<br>key3: Wind Cutter Whistle | hundred_edge |
| **Thunder God Sword** (`thunder_god_sword`) | 5 | Hundred Sword, Spirit Tortoise Carapace Sword | `minecraft:nether_star` x4<br>`tensura:high_quality_magic_crystal` x8<br>`minecraft:lightning_rod` x32 | 400 | attack_damage 12, attack_speed 0.6<br>mastery: attack_damage +2, attack_speed +0.1 | use: Blitz Strike<br>sneak: Lightning Hundred Swords<br>key1: Hundred Swords<br>key2: Gravity Sword<br>key3: Eagle Blade | thunder_edge |
| **Sword of Greed** (`sword_of_greed`)<br>CURSE: greed, corruption 3/s | 1 | Small Sword | `minecraft:gold_ingot` x32<br>`minecraft:emerald` x16 | 120 | attack_damage 5, attack_speed 0.3<br>mastery: attack_damage +1 | use: Blitz Strike<br>sneak: Guillotine<br>key1: Afterimage Step<br>key2: Tempest Dance | plunder |
| **Sword of Greed III** (`sword_of_greed_3`)<br>CURSE: greed, corruption 5/s | 3 | Sword of Greed, Assassin Sword | `minecraft:gold_block` x16<br>`minecraft:diamond` x16<br>`tensura:gold_coin` x32 | 270 | attack_damage 9, attack_speed 0.4<br>mastery: attack_damage +2 | use: Blitz Strike<br>sneak: Guillotine<br>key1: Gold Rebellion<br>key2: Assassin Sword<br>key3: Afterimage Step | plunder |
| **Sword of Gluttony** (`sword_of_gluttony`)<br>CURSE: gluttony, corruption 4/s | 2 | Heavenly Wind King Katana | `minecraft:cooked_beef` x48<br>`minecraft:bread` x32<br>`minecraft:golden_apple` x4 | 150 | attack_damage 6, attack_speed 0.2, max_health 4<br>mastery: attack_damage +1, max_health +1 | use: Blitz Strike<br>sneak: Maneater<br>key1: Tempest Dance<br>key2: Skyfall Cleave | fang_maw |
| **Sword of Greed and Gluttony III** (`sword_of_greed_gluttony_3`)<br>CURSE: gluttony, corruption 8/s | 4 | Sword of Greed III, Sword of Gluttony | `minecraft:enchanted_golden_apple` x4<br>`minecraft:nether_star` x4<br>`minecraft:wither_skeleton_skull` x8 | 390 | attack_damage 13, attack_speed 0.5, max_health 6<br>mastery: attack_damage +2.5, max_health +1.5 | use: Blitz Strike<br>sneak: Maneater<br>key1: Strong Decline<br>key2: Guillotine<br>key3: Insatiable Maw | plunder, fang_maw |

Reading the tree: the Flash Sword and the Heavenly Wind King Katana are the two first steps. The Flash line goes Flash Sword, Meteor Sword (stars), Phoenix Sword (fire), Hundred Sword, and the
Assassin Sword also comes from the Flash Sword. The Katana line goes Heavenly Wind King Katana, Spirit Tortoise Sword (gravity), Spirit Tortoise Carapace Sword (floating swords). The Hundred Sword
also needs the Wind King Katana (its Wind Cutter), and the Thunder God Sword (the top, tier 5) needs the Hundred Sword and the Carapace Sword (its Gravity Sword), so the top of the tree asks for both
lines. On the curse side the Sword of Greed starts at the base sword and the Sword of Gluttony at the Wind King Katana (so neither series needs the other one learned); Sword of Greed III needs the
Sword of Greed and the Assassin Sword (canon: the second Greed sword brings Assassinating Sword and Guillotine); the combined sword needs Greed III and the Sword of Gluttony.

## 5. The techniques

Nineteen techniques: fourteen of the ordinary tree and five of the Greed and Gluttony series. Any form can list any technique; the base form puts Blitz Strike on the `use` slot (the swing
weapon's right click) and the others on `sneak` and the keys. The numbers below are in code (`SwordNumbers`); raw damage is before the bond factor (1.0 to 2.2) and the balance law.

| Skill | Class | Cooldown | Cost (of max magicules) | Raw per hit (largest) | Hits on one target | Most on one target | Series |
|---|---|---|---|---|---|---|---|
| Blitz Strike (`sword.blitz`) | damage | 3.5 s | 1.5% | 8 | 1 | 8 | ordinary |
| Tempest Dance (`sword.tempest`) | damage | 4.5 s | 2% | 5 | 2 | 10 | ordinary |
| Afterimage Step (`sword.afterimage`) | utility | 5 s | 1.5% | none | - | none | ordinary |
| Flashing Sword (`sword.flashing`) | utility | 5 s | 1.5% | 4 | 1 | 4 | ordinary |
| Wind Cutter Whistle (`sword.wind_cutter`) | damage | 4.5 s | 1.5% | 4 | 3 | 12 | ordinary |
| Meteor Sword (`sword.meteor`) | damage | 8 s | 2.5% | 3 | 4 | 12 | ordinary |
| Assassin Sword (`sword.assassin`) | damage | 8 s | 2% | 12 | 1 | 12 | ordinary |
| Eagle Blade (`sword.eagle_blade`) | heavy | 10 s | 2.5% | 16 | 1 | 16 | ordinary |
| Gravity Sword (`sword.gravity`) | damage | 14 s | 2.5% | 3 | 3 | 9 | ordinary |
| Skyfall Cleave (`sword.cleave`) | heavy | 13 s | 3% | 16 | 1 | 16 | ordinary |
| Phoenix Gale Blade (`sword.phoenix_gale`) | heavy | 15 s | 3% | 9 | 2 | 18 | ordinary |
| Float Sword (`sword.float_blades`) | damage | 20 s | 3% | 3 | 4 | 12 | ordinary |
| Hundred Swords (`sword.hundred_swords`) | heavy | 13 s | 3% | 4 | 4 | 16 | ordinary |
| Lightning Hundred Swords (`sword.lightning_hundred`) | ultimate | 45 s | 7% | 3.6 | 8 | 28.8 | ordinary |
| Guillotine (`sword.guillotine`) | heavy | 18 s | 3% | 16 | 1 | 16 | curse |
| Gold Rebellion (`sword.gold_rebellion`) | damage | 15 s | 2.5% | 10 | 1 | 10 | curse |
| Maneater (`sword.maneater`) | heavy | 10 s | 2.5% | 14 | 1 | 14 | curse |
| Strong Decline (`sword.strong_decline`) | heavy | 16 s | 3% | 16 | 1 | 16 | curse |
| Insatiable Maw (`sword.insatiable_maw`) | ultimate | 60 s | 8% | 9.6 | 3 | 28.8 | curse |

### Blitz Strike (`sword.blitz`)

* **Class and price:** damage; cooldown 3.5 s; cost 1.5% of the maximum magicules.
* **What it does:** Blitz Strike, the technique of the use key. A dash along where the wielder faces: it stops at a wall and cuts everything within 1.2 blocks of the path. The next blow within 3 s leaves a ghost strike (4 raw).
* **Numbers:** 8 raw to each foe on the path (at most 12 foes); dash up to 7 blocks; wind-up 3 ticks.
* **Tell:** A blue line on the ground along the path, 3 ticks before, and a rushing sound.
* **How to answer it:** Step off the line; it is short and straight.
* **Given by:** Small Sword, Flash Sword, Heavenly Wind King Katana, Meteor Sword, Assassin Sword, Spirit Tortoise Sword, Spirit Tortoise Carapace Sword, Phoenix Sword, Hundred Sword, Thunder God Sword, Sword of Greed, Sword of Greed III, Sword of Gluttony, Sword of Greed and Gluttony III

### Tempest Dance (`sword.tempest`)

* **Class and price:** damage; cooldown 4.5 s; cost 2% of the maximum magicules.
* **What it does:** Tempest Dance: two sweeps of the blade round the wielder, shoving the foes back. Refused, and free, when no foe is in reach. The sweeps follow the wielder's body.
* **Numbers:** 5 raw a sweep, 10 at most on one foe; radius 4.5 blocks; sweeps at 6 and 12 ticks.
* **Tell:** A ring of blue dust marks the reach, and a sweep sounds before each sweep.
* **How to answer it:** Leave the ring.
* **Given by:** Small Sword, Flash Sword, Heavenly Wind King Katana, Spirit Tortoise Sword, Spirit Tortoise Carapace Sword, Sword of Greed, Sword of Gluttony

### Afterimage Step (`sword.afterimage`)

* **Class and price:** utility; cooldown 5 s; cost 1.5% of the maximum magicules.
* **What it does:** Afterimage Step: a step the way the wielder was moving (backwards when standing still) that leaves a double behind. The wielder is faster and a little tougher for a moment and the next blow leaves a ghost strike. Refused when there is no room.
* **Numbers:** Step of 5 blocks; Speed II for 3 s; Resistance I for 1 s; no damage; the ghost stays armed for 3 s.
* **Tell:** The double stays where the wielder was for a moment.
* **How to answer it:** It deals no damage itself; the blow after it does.
* **Given by:** Small Sword, Flash Sword, Heavenly Wind King Katana, Assassin Sword, Sword of Greed, Sword of Greed III

### Flashing Sword (`sword.flashing`)

* **Class and price:** utility; cooldown 5 s; cost 1.5% of the maximum magicules.
* **What it does:** Flashing Sword: the blade flashes in front of the wielder. Foes in the cone are hurt and lit up; a mob is blinded, a player is only slowed; the wielder runs faster.
* **Numbers:** 4 raw; cone of 7 blocks and 70 degrees to each side; Glowing 3 s; Blindness 2 s on mobs, Slowness I 1 s on players; Speed I for 3 s on the wielder.
* **Tell:** A flare of white light in front of the wielder.
* **How to answer it:** A player is not blinded; a mob loses sight for 2 s.
* **Given by:** Flash Sword, Assassin Sword

### Wind Cutter Whistle (`sword.wind_cutter`)

* **Class and price:** damage; cooldown 4.5 s; cost 1.5% of the maximum magicules.
* **What it does:** Wind Cutter Whistle: three whistling crescents of wind fly out in a narrow fan and pass through everything they meet.
* **Numbers:** 4 raw a crescent, 12 at most on one foe; 12 degrees between neighbouring crescents; 1.5 blocks per tick for 8 ticks (12 blocks); 0.9 blocks wide.
* **Tell:** The crescents are drawn from the cast, with a whistle.
* **How to answer it:** Step aside; the fan is narrow.
* **Given by:** Heavenly Wind King Katana, Meteor Sword, Phoenix Sword, Hundred Sword

### Meteor Sword (`sword.meteor`)

* **Class and price:** damage; cooldown 8 s; cost 2.5% of the maximum magicules.
* **What it does:** Meteor Sword: a ring of four stars falls where the wielder points, one after another, hurting the foes near each.
* **Numbers:** 3 raw a star, 12 at most on one foe; aim up to 16 blocks; ring radius 1.6, star radius 1.8; the first star after 12 ticks, then one every 3 ticks.
* **Tell:** A ring and a column of dust where the stars will land, and a chime.
* **How to answer it:** Stay out of the ring.
* **Given by:** Meteor Sword, Hundred Sword

### Assassin Sword (`sword.assassin`)

* **Class and price:** damage; cooldown 8 s; cost 2% of the maximum magicules.
* **What it does:** Assassin Sword: the wielder blinks behind the foe they look at (else the nearest foe within 12 blocks) and stabs it; a foe that was not facing the wielder is stabbed deeper. If the foe has fled far or teleported away in the quarter second between, the blink fizzles and the wielder stays.
* **Numbers:** 8 raw, 12 raw from behind; wind-up 5 ticks; the blink fizzles when the foe is more than 16 blocks away.
* **Tell:** An afterimage at the wielder and a line to the target, 0.25 s before the blink.
* **How to answer it:** Face the wielder (the extra 4 raw is lost) or get far away.
* **Given by:** Assassin Sword, Sword of Greed III

### Eagle Blade (`sword.eagle_blade`)

* **Class and price:** heavy; cooldown 10 s; cost 2.5% of the maximum magicules.
* **What it does:** Eagle Blade: after a glint the wielder lunges at the first foe in the line ahead with a point that goes through armour. Refused, and free, when no foe is in the line.
* **Numbers:** 10 raw, raised by what 60 percent of the foe's armour would have stopped, never above 16; lunge up to 5 blocks; the line is 6.5 blocks long and 1 block wide; wind-up 8 ticks.
* **Tell:** A line of thunder-coloured dust along the lunge and a glint.
* **How to answer it:** Step out of the line.
* **Given by:** Spirit Tortoise Sword, Phoenix Sword, Hundred Sword, Thunder God Sword

### Gravity Sword (`sword.gravity`)

* **Class and price:** damage; cooldown 14 s; cost 2.5% of the maximum magicules.
* **What it does:** Gravity Sword: needs control. The weight round the foe the wielder looks at grows for three pulses: everything within 3.5 blocks of it is slowed, pulled to the ground and cut. The circle stays where it was cast.
* **Numbers:** 3 raw a pulse, 9 at most on one foe; pulses at once, after 1 s and after 2 s; Slowness III for up to the law's length (a boss one second at most); aim up to 14 blocks.
* **Tell:** A well of dark light opens on the spot at once and a heavy sound pulses every second.
* **How to answer it:** Walk out of the 3.5-block circle.
* **Given by:** Spirit Tortoise Sword, Spirit Tortoise Carapace Sword, Thunder God Sword

### Skyfall Cleave (`sword.cleave`)

* **Class and price:** heavy; cooldown 13 s; cost 3% of the maximum magicules.
* **What it does:** Skyfall Cleave: after a crouch the wielder leaps to a ring ahead and slams the blade down, throwing the foes up and back. The leap goes to the ring that was drawn, even if the wielder moved in between. Refused, and free, when no foe is in reach.
* **Numbers:** 16 raw to everything within 3.5 blocks of the landing; leap up to 5 blocks (6.5 if the wielder moved); wind-up 12 ticks; reach for the check about 8.5 blocks.
* **Tell:** A ring and a column of sky-blue dust where the wielder will land.
* **How to answer it:** Leave the ring.
* **Given by:** Meteor Sword, Spirit Tortoise Carapace Sword, Phoenix Sword, Sword of Gluttony

### Phoenix Gale Blade (`sword.phoenix_gale`)

* **Class and price:** heavy; cooldown 15 s; cost 3% of the maximum magicules.
* **What it does:** Phoenix Gale Blade: the sword gathers red energy, then a storm of flame burns the cone in front of the wielder and a bird of fire flies on along the line. Both set the foe alight.
* **Numbers:** 9 raw from the storm and 9 from the bird, 18 at most on one foe; cone of 7 blocks and 55 degrees to each side; the bird flies 1.4 blocks per tick for 11 ticks (about 15 blocks) and is 1.2 blocks wide; fire for 4 s; wind-up 18 ticks.
* **Tell:** Red energy gathers at the blade and a flame cone is marked on the ground for 0.9 s.
* **How to answer it:** Leave the cone and the line of the bird.
* **Given by:** Phoenix Sword

### Float Sword (`sword.float_blades`)

* **Class and price:** damage; cooldown 20 s; cost 3% of the maximum magicules.
* **What it does:** Float Sword: four swords circle the wielder for a while. Once a second each of them cuts a different foe near the wielder, and together they remove a few hostile projectiles that come close.
* **Numbers:** 3 raw a cut, 12 at most on one foe; 8 s (160 ticks); radius 3.5 blocks; stops 3 projectiles within 3 blocks. A newer cast or a form change ends the old swords.
* **Tell:** The swords themselves orbit the wielder, visible all the time.
* **How to answer it:** Stay out of 3.5 blocks; the cuts are small.
* **Given by:** Spirit Tortoise Carapace Sword

### Hundred Swords (`sword.hundred_swords`)

* **Class and price:** heavy; cooldown 13 s; cost 3% of the maximum magicules.
* **What it does:** Hundred Swords: the blade splits into a hundred and they fall in four waves on the circle where the wielder points.
* **Numbers:** 4 raw a wave, 16 at most on one foe; aim up to 14 blocks; radius 5 blocks; the first wave after 10 ticks, then one every 10 ticks.
* **Tell:** A ring of dust on the ground and light gathering over it, half a second before.
* **How to answer it:** Leave the ring.
* **Given by:** Hundred Sword, Thunder God Sword

### Lightning Hundred Swords (`sword.lightning_hundred`)

* **Class and price:** ultimate; cooldown 45 s; cost 7% of the maximum magicules. An ultimate: one per form at most.
* **What it does:** Lightning Hundred Swords (the ultimate of the tree): after a long gathering, eight waves of lightning blades fall over a wide ring where the wielder points.
* **Numbers:** 3.6 raw a wave, 28.8 at most on one foe; aim up to 16 blocks; radius 7 blocks; gathering 18 ticks, then a wave every 5 ticks. Ultimate: cost 7 percent, cooldown 45 s.
* **Tell:** A great ring of thunder-coloured dust, a circle of light, light gathering above and a deep charging sound.
* **How to answer it:** Leave the ring during the gathering.
* **Given by:** Thunder God Sword

### Guillotine (`sword.guillotine`)

* **Class and price:** heavy (curse series); cooldown 18 s; cost 3% of the maximum magicules.
* **What it does:** Guillotine (Greed): a column of gold dust marks the spot where the foe looked at stands; a moment later a great blade falls there on everything on the spot and takes a share of the magicules of what it cuts.
* **Numbers:** 16 raw; radius 1.8 around the marked spot; takes 4 percent of the foe's maximum magicules and the wielder keeps up to 2 percent of their own maximum; wind-up 16 ticks; aim up to 12 blocks. In a curse form: +40 corruption.
* **Tell:** A column and a ring of gold dust on the spot and a falling blade, 0.8 s before.
* **How to answer it:** Move off the spot: a foe that moves away is missed.
* **Given by:** Sword of Greed, Sword of Greed III, Sword of Greed and Gluttony III

### Gold Rebellion (`sword.gold_rebellion`)

* **Class and price:** damage (curse series); cooldown 15 s; cost 2.5% of the maximum magicules.
* **What it does:** Gold Rebellion (Greed): a ring of golden blades cuts every foe near the wielder, shoves it back and gilds it; the wielder's next blow on a gilded foe takes more. Refused, and free, when no foe is in reach.
* **Numbers:** 10 raw to each foe within 5 blocks; gilded for 6 s (120 ticks); the next blow on a gilded foe earns a bonus strike of 3 raw (once); wind-up 6 ticks.
* **Tell:** A ring of gold dust round the wielder, 0.3 s before.
* **How to answer it:** Leave the ring.
* **Given by:** Sword of Greed III

### Maneater (`sword.maneater`)

* **Class and price:** heavy (curse series); cooldown 10 s; cost 2.5% of the maximum magicules.
* **What it does:** Maneater (Gluttony): a maw closes on up to three foes in front of the wielder and feeds them to it. Refused, and free, when no foe is in the cone.
* **Numbers:** 14 raw to each of up to three foes; cone of 4.5 blocks and 50 degrees to each side; heals 4 percent of the maximum health per foe bitten, through the balance law; wind-up 8 ticks.
* **Tell:** The cone is marked in brown dust and a bite sounds, 0.4 s before.
* **How to answer it:** Leave the cone.
* **Given by:** Sword of Gluttony, Sword of Greed and Gluttony III

### Strong Decline (`sword.strong_decline`)

* **Class and price:** heavy (curse series); cooldown 16 s; cost 3% of the maximum magicules.
* **What it does:** Strong Decline (Greed and Gluttony): the ground splits in a line in front of the wielder and fangs rise out of it. No block is changed: the split is only drawn.
* **Numbers:** 16 raw to every foe in the strip (10 blocks long, 2.4 wide); Slowness II for 2 s; wind-up 14 ticks. In a curse form: +30 corruption.
* **Tell:** A brown line along the strip and a ring at its end, 0.7 s before, and a shriek.
* **How to answer it:** Step off the line.
* **Given by:** Sword of Greed and Gluttony III

### Insatiable Maw (`sword.insatiable_maw`)

* **Class and price:** ultimate (curse series); cooldown 60 s; cost 8% of the maximum magicules. An ultimate: one per form at most.
* **What it does:** Insatiable Maw (the ultimate of the series): after a long wind-up three great maws close, one after another, over the ring drawn where the wielder stood at the cast; every wave that bites feeds the wielder.
* **Numbers:** 9.6 raw a wave, 28.8 at most on one foe; radius 7 blocks; the first wave after 30 ticks, then one every 10 ticks; heals 6 percent of the maximum health per wave that bites, through the balance law. Ultimate: cost 8 percent, cooldown 60 s. In a curse form: +100 corruption, once.
* **Tell:** A banquet field, a brown ring and gold light gathering for 1.5 s, with a deep charging sound.
* **How to answer it:** Leave the ring.
* **Given by:** Sword of Greed and Gluttony III

## 6. The passives

| Passive | id | Forms | What it does |
|---|---|---|---|
| Carapace Guard | `carapace_guard` | Spirit Tortoise Carapace Sword | Once every 15 seconds the carapace halves a blow that would take a fifth of the wielder's health. |
| Ember Edge | `ember_edge` | Phoenix Sword | Blows set the foe alight, and the wielder resists fire. |
| Fang Maw | `fang_maw` | Sword of Gluttony, Sword of Greed and Gluttony III | Blows on a foe under 30 percent of its health bite for 3 more damage. |
| Gravity Edge | `gravity_edge` | Spirit Tortoise Sword | Blows slow the foe (not a boss) for a second and a half. |
| Hundred Edge | `hundred_edge` | Hundred Sword | Every tenth blow rains blades on the foe: 3 damage to everything near it. |
| Plunder | `plunder` | Sword of Greed, Sword of Greed III, Sword of Greed and Gluttony III | Blows take a sliver of the foe's magicules for the wielder. |
| Quick Draw | `quick_draw` | Flash Sword | The first blow after a pause of three seconds is an opening strike with 3 extra damage. |
| Shadow Blade | `shadow_blade` | Assassin Sword | A blow from the back half of a foe adds 3 damage. |
| Star Shower | `star_shower` | Meteor Sword | Every fifth blow calls a star down on the foe: 4 damage to everything near it. |
| Thunder Edge | `thunder_edge` | Thunder God Sword | A blow arcs lightning to a second foe within 5 blocks for 3 damage, at most once a second. |
| Wind Walker | `wind_walker` | Heavenly Wind King Katana | The speed that the Tempo earns comes one blow earlier, and falls hurt half as much. |

A passive is an id in the forms file that the logic checks with `c.form.has(id)`; the name and text come from the lang fragment (`hero.passive.sword.<id>`).

## 7. The curse series: Greed and Gluttony, what it costs the wielder, and why

**What the curse engine does** (design.md section 7, built by the curse builder): corruption (0 to 1000) rises by the form's `corruptionPerSecond` (Greed 3 and 5, Gluttony 4 and 8) and by
each technique; Greed makes the wielder unable to stop collecting (drops are pulled in and every technique costs more); Gluttony drains hunger fast and lets kills heal. At 1000 the weapon takes
over for 10 to 20 seconds. The series is unlocked only by the player's deliberate acceptance after despair (in the series, Ren's came on discovering a betrayal (M)), with a permanent price.

**What this weapon adds, and why:**
* The techniques of the two sins, which are what the curse is FOR: Guillotine, Gold Rebellion, Maneater, Strong Decline and Insatiable Maw. Four of the five are heavy or ultimate; Gold Rebellion, the lightest, is 10 raw.
* **Extra corruption on the three that the series ties to its worst cost** (Guillotine +40, Strong Decline +30, Insatiable Maw +100), only when the active form is a curse form: the canon says that after
  Guillotine and Strong Decline Ren cannot gain experience and cannot hold what is worth money (S). Gold Rebellion and Maneater add none; they are the cheaper members of the series.
* **Plunder and Fang Maw** are the passives of the sins: Greed takes from what it cuts, Gluttony bites deeper into what is hurt.
* **The heals** of Gluttony are small and capped by the law, so the Sword cannot out-heal a boss.
* **Why bounded:** a stolen sliver of magicules is a percent of a maximum, never the foe's whole store; every strike is within the budget; the price is paid in corruption, which the engine already
  bounds with the Overrun.

## 8. Engravings (the owner's rule 6)

The lead adds this line to `item/WeaponEngravings.java` (the file is shared; I did not edit it):

```java
Map.entry("legendary_sword", List.of(e("swift", 2), e("severance", 2), e("barrier_piercing", 1), e("holy_weapon", 1))),   // the fast blade: quick, cuts clean, goes through wards, one of the four Holy Weapons
```

| Engraving | Level | Why |
|---|---|---|
| `swift` | 2 | The Sword is the fast weapon of the set: attack speed, dashes, afterimages. |
| `severance` | 2 | A blade that cuts clean is the Sword's whole offence (Tempest, Cleave, Guillotine, the Hundred Swords). |
| `barrier_piercing` | 1 | Eagle Blade is the series' defence-ignoring attack (S): its point goes through armour and wards. |
| `holy_weapon` | 1 | It is one of the four Holy (Legendary) Weapons. |

A level above an engraving's maximum is capped by the engraving code; an id Tensura lacks is skipped.

## 9. Numbers and the balance law

The budget of design.md section 5, and how this weapon keeps it (every point is a test in `SwordNumbersTest` unless marked 'code'):

* Raw damage per hit is at least 3 and at most 12 for an ordinary technique, 10 to 20 for a heavy one, at most 32 for an ultimate; the most one target takes from one cast is also in class
  (every multi-hit technique has a per-target cap, `HitCap`). The largest are Lightning Hundred Swords and Insatiable Maw (28.8, ultimates) and the 16-raw heavy techniques.
* Bonus strikes of the Tempo and the passives are 3 to 6 raw, one per blow (`HitLedger`).
* Cooldown at least 60 ticks for a damage technique, 100 for a utility, 600 for an ultimate; cost 1 to 3 percent of the magicules, an ultimate 5 to 8 percent.
* Every damage goes through `HeroCtx.hit`; every heal through `HeroCtx.heal`; Gravity Sword asks `HeroCtx.control()` first and takes its length from `BalanceLaw.controlTicks`. Flashing Sword
  blinds mobs for 2 s and only slows players for 1 s (it is not hard control).
* At most 12 targets per technique. No entity is spawned (the crescents and the bird are bolts of `SpellRuntime`, at most three at once; the floating swords are an effect). No block is
  placed or broken. No task runs longer than 20 s (the longest is the 8 s Float Sword).
* The weapon moves the wielder with `teleportTo` after a collision check; a move that cannot be made is not made.
* Hooks never throw: each one catches `RuntimeException` and `LinkageError` and reports once through `SafeCast.fault`.
* State: one `SwordState` per wielder in a map dropped on logout and on `ServerStoppedEvent`; the mark table is bounded (24 marks); all of it is a cache, nothing is saved.

## 10. Files, classes and tests (the why of each)

| File | Why it exists |
|---|---|
| `weapon/sword/SwordNumbers.java` | Every number of the weapon in one place: a table with a row per technique and the constants of the Tempo, the bonus strikes, the passives and the techniques. The skills read their cooldown and cost from it and the test walks it against the budget, so one careless edit (a cooldown of 20, an extra pulse) cannot pass. |
| `weapon/sword/HitLedger.java` | The rules of the wielder's blows with no game types: the Tempo, the Flow strike, the ghost of an afterimage and the passives' bonus strikes, with the one rule that must never break (one bonus per blow, never above the cap). Tested alone. |
| `weapon/sword/SwordGeo.java` | The arithmetic of the Sword's shapes (fan, ring, scatter, strip, 'behind', dash length, yaw). A shape that is a little wrong is a technique that visibly misses. Tested alone. |
| `weapon/sword/MarkBook.java` | Which foes carry the gilding of Gold Rebellion and until when; bounded, consumed once. Tested alone. |
| `weapon/sword/HitCap.java` | A per-target cap for one cast, so overlapping pulses cannot pass the technique's budget. Tested alone. |
| `weapon/sword/SwordState.java` | What one wielder carries from call to call (the ledger, the marks, the timers of the passives and of the floating swords). |
| `weapon/sword/SwordTargets.java` | Who the Sword may hurt: one place, so every technique and every passive asks the same question. |
| `weapon/sword/SwordFx.java` | The effects the techniques use, named (`crescent`, `streak`, `star`, `maw`, `fissure` ...), built from `VfxSpawn` shapes, `Tells` and sounds only; no vanilla particle. |
| `weapon/sword/SwordRuntime.java` | The per-wielder state cache (dropped on logout and server stop) and the two scheduling helpers every technique uses. |
| `weapon/sword/SwordOps.java` | What each technique does, one static method per technique, top to bottom; also the dash and the one flag that keeps the weapon's own strikes from being taken for melee blows. |
| `weapon/sword/SwordSkills.java` | The list of skills with their names and texts; each takes its cooldown and cost from the table. |
| `weapon/sword/SwordLogic.java` | The hooks: Tempo speed (`onTick`), the blow (`onHit`), the passives that act on damage taken (`onDamaged`), kills, cleanup. |
| `weapon/sword/SwordSelfTest.java` | The in-game self-test `hero.weapon.sword`: table against registered skills, forms file against skills and passives, then a cast of every skill and a run of the hooks with a stand-in player. |
| `data/nusmp/hero/forms/legendary_sword.json` | The forms: the base sword, ten ordinary forms and four curse forms (two Greed, two Gluttony). |
| `test/.../sword/SwordNumbersTest, HitLedgerTest, SwordGeoTest` | The budget of design.md section 5 as a test, and the pure parts (ledger, geometry, marks, caps). |
| `test/.../sword/SwordFormsDataTest` | The rules of design.md section 6 applied to the shipped file with the shared checker, and negative controls. |

Every Java class opens with a Javadoc whose first paragraph starts with `Why:`. The self-test is registered from the logic's constructor (`SelfTests.register("hero.weapon.sword", ...)`), which runs at start-up.

## 11. For the lead and the other builders

* **Engravings:** the line in section 8.
* **Lang fragment:** `$S/hero/frag/hsa.lang.json` (120 keys for both weapons). Generated from the source, ASCII only.
* **Forms file:** `src/main/resources/data/nusmp/hero/forms/legendary_sword.json`. The first form is `base`, and its `use` slot is Blitz Strike.
* **Constructors:** `HeroBootstrap` must construct `new SwordLogic()` once at start-up (the constructor hooks the events and registers the self-test).
* **Curse engine:** the weapon calls `HeroCurse.addCorruption(ServerPlayer, int)` from Guillotine (+40), Strong Decline (+30) and Insatiable Maw (+100), only when the active form is a curse form.
  The sin downsides are the engine's.
* **Core:** `onHit(c, target, dealt, fullSwing)` must be called once per melee blow of the wielder after the damage; the Flow depends on `fullSwing`.
* **Art:** sizes and parts are in section 1; the Greed and Gluttony look is the `curse` look of the forms.
* **Config:** none (a weapon builder keeps its numbers in code).

## 12. What is not verified

* **Nothing ran in the game.** What ran: the pure classes' tests (110 JUnit tests for both weapons through a small local shim, the real JUnit runs in CI) and a type-check of the glue against API stubs (only the known artifact 'MobEffectInstance is abstract' was reported in my files; a deliberate wrong call proved the checker sees my code).
* **`onHit` contract.** I assume `onHit(c, target, dealt, fullSwing)` is called once per melee blow of the wielder on a living target, after the blow's damage, with `fullSwing` true when the attack bar was full. The Flow strike depends on that flag. If the core calls it for other damage (a thorns hit, a bonus of another mod) the Tempo would count it.
* **Re-entrancy.** My strikes go through `c.hit`, which raises damage events; I mark them with a flag so the weapon's own `onHit` ignores them. If the core's dispatcher calls `onHit` from a different place than the damage event of the wielder's attack (for example from a delayed task), the flag would not cover it. The flag is a plain static boolean used on the server thread only.
* **The dash.** Blitz, Afterimage, Eagle Blade, Cleave and Assassin move the wielder with `teleportTo` after a collision check; I did not verify how that interacts with anti-cheat of other mods, with vehicles, or with a wielder in water. A failed move is simply not made.
* **Servers see a player's velocity badly,** so the Afterimage direction is read from the change of position since the last tick (`getX() - xo`), not from `getDeltaMovement()`.
* **Tensura storage.** Plunder, Guillotine and Insatiable Maw's steal use `TensuraStorages.getExistenceFrom`, `getMagicule`, `setMagicule`, `markDirty` and `EnergyHelper.getMaxMagicule`, modelled on `EnergyBridge.drain`. A foe without magicules gives nothing.
* **Numbers are desk numbers.** Nothing was play-tested: the Tempo window (2 s), the speed levels, the size of the bonus strikes, the 16 raw heavy hits, 28.8 raw ultimates on up to twelve foes. The tests pin the budget, not the feel.
* **Effects were never seen.** `SwordFx` composes `VfxShape` values and `Tells` by reading their code and signatures; the shapes exist but how they look, how long they stay and whether two of them clash is unknown.
* **Tensura requirements.** `tensura:armorsaurus_scale`, `armorsaurus_shell`, `giant_ant_carapace`, `mithril_ingot`, `high_quality_magic_crystal` and `gold_coin` were checked against the owner's id reference table, not against the live registry; the `hero.forms` self-test of CI resolves every id. `minecraft:enchanted_golden_apple` is only found in loot in 1.21, never crafted (it is a rare requirement on purpose).
* **Interpretation of the budget.** design.md section 5 says 'raw base damage per hit: ordinary 3..12'. I read it as: no single hit below 3 raw (a test pins it), and the total one target can take from one cast is also within the class (12 / 20 / 32).
* **Frames of grace.** My strikes set `invulnerableTime` to 10 on the target before `c.hit`, so the many small blows of a barrage each land. I did not verify what `c.hit` does with it internally.
