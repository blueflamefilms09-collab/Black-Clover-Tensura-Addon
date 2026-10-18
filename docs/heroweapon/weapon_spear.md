# The Legendary Spear (`legendary_spear`, 1.62.0)

Why this page exists: it is the one place that says what the Legendary Spear is in canon, how it plays here, what every number is, what it costs the wielder
and what the art builder should draw. The binding contract of the whole job is `docs/heroweapon/design.md`; this page only adds the Spear's own decisions.
Section 1 (the look) was written first so the art builder could start; everything after it was written when the code was done.

Reading the numbers: every damage figure on this page is **raw** damage, before the level factor of the bond (1.0 at level 0 to 2.2 at level 100) and before
the balance law (existence points, the damage game rule, the player and boss caps). Distances are blocks, delays are ticks (20 a second). A cooldown here is
the number before the bond level shortens it (down to three quarters at level 100). Nothing on this page was ever run in a game; section 13 lists exactly what
was and was not checked.

## 1. The look, for the art builder (written first)

The Legendary Spear is the weapon of the Spear Hero, Motoyasu Kitamura. It is the longest melee weapon of the four Holy Weapons and the fastest to
move with. It must read as noble, plain and strong at the base look, and as the same weapon made monstrous at the curse look.

**Size class.** `spear` of `assets/nusmp/weapon_classes.json` (2.8 to 3.6 blocks). Aim for 3.3 blocks of apparent length, butt to tip. The grip
(hand position) is the class grip, 0.33 of the length from the butt. Held like every other long weapon (mesh -x forward, tip up-forward on the icon
diagonal, see `docs/weapon_scale_orientation.md`).

**Base look (named parts, 10 to 13).**

| part | what | size (blocks) | colour / material |
|---|---|---|---|
| `butt_cap` | a short gold ferrule with a small ball finial at the butt | 0.18 long, 0.09 wide | gold |
| `shaft` | the long straight round shaft, very slightly thicker at the grip | 2.5 long, 0.07 wide | pale ivory ash wood (lacquered), fine grain |
| `grip_wrap` | leather wrap over the grip zone (0.33 of the length from the butt), cross-hatched binding | 0.45 long | deep crimson leather |
| `ring_1`, `ring_2`, `ring_3` | three thin gold binding rings on the shaft (at 0.58, 0.72 and 0.86 of the shaft) | 0.03 tall, 0.1 wide | gold |
| `collar` | a gold collar where the shaft meets the socket, with a short flare | 0.14 long | gold, engraved lines |
| `socket` | the head's socket: a gold cup that holds the blade, wider than the shaft | 0.2 long, 0.12 wide | gold, shaded dark in the grooves |
| `gem` | THE GEM: a red cabochon set into the front (-x) face of the socket, the series' central jewel | 0.07 across | red `#E3342F`, glow `#FF5C4D` |
| `blade` | the head: a long leaf-shaped, double-edged blade with a raised central ridge (a diamond cross-section), straight edges that swell into a leaf and meet in a sharp point | 0.6 long, 0.15 at its widest | bright steel `#DCE6F2`, highlight `#FFFFFF`, shade `#8FA0B8` |
| `blade_ridge` | a thin inlaid line down the middle of the blade | 0.5 long | gold inlay (it carries a faint glow) |
| `feather` | one small white feather hanging from the collar on a short cord (canon: the spin-off spear carries a feather of Fitoria as a keepsake) | 0.22 long | white `#F4F1EA`, a hint of pale blue |

Silhouette: a clean spear; nothing sticks out sideways except the leaf of the blade and the little feather. The blade is the brightest part, the
socket and rings the warm part, the shaft the quiet part. Nothing hangs below the grip.

**Glow skin (additive).** The gem is the strongest glow (`#FF5C4D`, a soft halo painted around it on the skin). A very faint warm line along the
`blade_ridge` and a faint edge on the three rings. Everything else black on the glow skin.

**Curse look (Lust), `legendary_spear_curse`.** Same length (3.3), same grip position, same socket position, so the hand and the animation fit.
The sin colour is `Sin.LUST` `#D94A8C`.

| part | change |
|---|---|
| `blade` | the leaf becomes a barbed, serrated spade: the edges grow backward-pointing barbs (three a side), the point splits into a short fork, the steel turns blackened iron `#2A1B2B` with a magenta edge `#FF4FA3` |
| `shaft` | wrapped in black thorned vines: a spiral of thick dark cords with 8 to 10 small thorns (thin cones, 0.05 long) sticking out; the ivory wood shows only in places |
| `chains` | two short black iron chains (6 to 8 small links each) hang from the collar and swing below the grip height by at most 0.3 |
| `butt_cap` | the ball finial becomes a thorned rosebud: three layered petals, magenta-black |
| `gem` | the gem turns magenta-pink `#FF3F9E`, cracked (a bright crack line across it on the glow skin), the halo larger |
| `feather` | blackened, torn (or left out) |

Glow skin of the curse look: the gem, the barbed edges of the blade (magenta), the thorn tips (faint), and the chain links (very faint).

**Forms and looks.** The engine only distinguishes `base` and `curse` looks; ordinary forms do not change the mesh. The form's identity is shown in
the Weapon Book and the tooltip.

**Effect colours the game uses (not a mesh matter):** red-gold `0xFFFF5C4D`, white-gold `0xFFFFE8A0`, silver `0xFFE4E8F0`; the Lust forms use `0xFFD94A8C`.

## 2. Canon, and what is design

Every canon claim below comes from **search summaries of the fandom wiki** (the pages themselves could not be opened from the build machine: the fetch
fails with "host not found"). They are fan-compiled, they disagree between the light novel and the web novel, and they are marked here: **[S]** = from a
search summary, **[M]** = from memory of the series (unchecked), **[D]** = this mod's design, not canon.

| fact | status | source |
|---|---|---|
| The Legendary Spear is the Cardinal weapon of Motoyasu Kitamura, the Spear Hero. | [S] | https://shield-hero.fandom.com/wiki/Legendary_Spear |
| A red gem sits in the middle of the weapon and stays through every transformation; it is the matrix that lets the weapon change. | [S] | https://shield-hero.fandom.com/wiki/Legendary_Spear_(Web_Novel) |
| The light novel's form table starts at the **Small Spear** (equip bonus Attack 3, Defense 3); the web novel calls the base "Legendary Spear". | [S] | https://shield-hero.fandom.com/wiki/Legendary_Spear |
| Skills and special effects the wiki lists for the spear: **Chaos Thrust** (also Chaos Stab / Chaos Spear: the spear splits into several spears that fly at the target), **Meteor Spear** (also Shooting Star Spear / Meteor Thrust), **Portal Spear** (teleports the user and the party to saved places). | [S] | https://shield-hero.fandom.com/wiki/Motoyasu_Kitamura/Powers_and_Abilities |
| **Triaina** shoots a beam of energy and is the precursor of **Brionac** (a beam of blazing white light). The form that follows Triaina is listed as **Bruenack**. Brionac was first used by Biscas T. Balmus with a replica and later unlocked by Motoyasu (the Water Dragon Harpoon copied from Sadeena's harpoon unlocked it). One source says Triaina is a move of Sadeena's copied spear. | [S] | https://shield-hero.fandom.com/wiki/Legendary_Spear_(Web_Novel) |
| The spear has forms that can be thrown like a javelin. | [S] | https://shield-hero.fandom.com/wiki/Legendary_Spear |
| The combo **Fire Storm Meteor Spear** (Meteor Spear with companions' fire and wind magic, fired at the Pope in volume 4). Not used here. | [S] | https://shield-hero.fandom.com/wiki/Motoyasu_Kitamura/Powers_and_Abilities |
| The power-up methods listed for the spear (smelting, spirit enchantment, status enchantment). Not used: the owner's absorption mechanic replaces them. | [S] | https://shield-hero.fandom.com/wiki/Legendary_Weapons/Power-up_Methods |
| **Time Reversal** is the unique property of the spear in the spin-off *The Reprise of the Spear Hero*: the spear (the Minute Hand of the Dragon / Dragon Minute Hand Spear, with a feather of Fitoria hung on it as a keepsake) sends Motoyasu back to the day the heroes were summoned whenever one of the four Legendary Heroes dies; he keeps his power and only vague memories. | [S] | https://shield-hero.fandom.com/wiki/Time_Reversal and https://shield-hero.fandom.com/wiki/The_Reprise_of_the_Spear_Hero |
| The **Lust Series**: the **Lust Spear IV** appears in web novel chapter 209 (after Motoyasu loses the filolial cart race to Naofumi with Filo as the prize). Its special effect **Temptation** is an area allurement that pulls targets towards the user, can be resisted, and blocks teleportation; the weapon raises the power of those under lust, mainly his own. The wiki documents **no cost** of the curse. The "women are pigs" habit probably comes from his collapse after the companions abandoned him, not from the curse. | [S] | https://shield-hero.fandom.com/wiki/Lust_Series_(Web_Novel) and https://shield-hero.fandom.com/wiki/Curse_Series/Weapon_List |
| The Envy Spear IV (Ressentiment) also exists. Not built: Envy belongs to a vassal weapon in this mod (design.md section 1). | [S] | same |
| The wiki could not confirm a spear skill called "Hundred" or "Fighting Spirit". Neither is used. | [S] | search results |

**Where the Spear departs from canon (all [D]).** (1) The canon Time Reversal rewinds the whole world to the summoning when a hero dies, with no limit;
here it is the owner's bounded mechanic: only the wielder, at most eight seconds back, a safe place, a quarter of the health at most, and once in two minutes (90 seconds at the top bond level)
(section 7). (2) The canon tree is a list of special effects; here every form is reached by feeding the gem the right materials (design.md section 6), and
the form names `hunters_spear`, `filolial_spear`, `clockwork_spear` and the Lust I to III steps are inventions of this mod. The names `chaos_spear`,
`meteor_spear`, `portal_spear`, `triaina`, `bruenack`, `dragon_minute_hand` and `lust_spear_4` come from the wiki's lists. (3) The canon weapon cannot be
used with other weapons; here the Holy rejection rules of the core do that. (4) Nothing here changes a block, spawns an entity or loads a chunk.

## 3. How it plays

The Spear is the **reach and mobility** weapon of the four. Its techniques are straight lines, rings and dashes; none of them follows its target for more
than a moment, and every one of them shows what it is about to do before it does it.

* **Reach.** The forms add up to 1.6 blocks of reach at the top (the `reach` stat, blocks of added interaction range; the budget allows 2.0), and the Longshaft
  passive makes the thrust, the Meteor Spear and Triaina a quarter longer. The right-click technique (`use`) is the **Piercing Thrust**, a line of 8 blocks.
* **Mobility.** Five different moves: the Dragoon Lunge (a dash through a line), Pole Vault (a leap), Portal Spear (a short blink to where you aim), Filo Rush
  (a sprint that tramples) and Hooked Pierce (haul a foe in, or yourself to a boss). Every move that puts the wielder somewhere new lands only on a safe
  place (section 7 and `Moves`); the Lunge, the Portal, Rapture and Time Reversal are refused while riding.
* **Time.** The top ordinary form, the Dragon Minute Hand Spear, can turn time back for its wielder alone: Time Reversal (section 7). The Clockwork Spear below
  it sweeps a Minute Hand round the wielder like the hand of a clock.
* **Lust.** The curse series (section 8) trades control for power: blows grow, the aim snaps to prey, foes are drawn in, and every technique adds corruption.

**Slots.** `use` (right click), `sneak` (sneak + right click) and the three hero keys. Every Spear form has the Piercing Thrust on `use`, so the weapon always
has a basic reach attack; the forms change the other four.

## 4. The forms tree

Ten ordinary forms (tiers 0 to 5) and the four Lust forms. A form is learned when all its parents are learned and the gem has absorbed the listed
materials (absorbed items are never consumed by unlocking). `mastery` is the points (a blow, two for a technique) that finish the form and grant its permanent bonus.
The stats apply while the weapon is summoned. Only existing item ids are asked for: the vanilla ids are checked by the forms test against the repo's list of vanilla items, and the four Tensura items (magic stone, the medium and high quality magic crystals, hihiirokane ingot) were looked up in the Tensura registry dump and are used elsewhere in the repo.

| form (`id`) | tier | parents | the gem must absorb | mastery | stats while summoned | permanent bonus | techniques by slot | passives |
|---|---|---|---|---|---|---|---|---|
| **Small Spear** (`base`) | 0 | none | nothing (always learned) | 0 | none | none | use: Piercing Thrust<br>sneak: Pole Vault<br>key1: Dragoon Lunge | none |
| **Hunter's Spear** (`hunters_spear`) | 1 | Small Spear | 20 bone, 20 string, 12 flint | 60 | attack_damage +1, reach +0.3 | reach +0.1 | use: Piercing Thrust<br>sneak: Pole Vault<br>key1: Dragoon Lunge<br>key2: Javelin Throw | Longshaft |
| **Filolial Spear** (`filolial_spear`) | 1 | Small Spear | 40 feather, 12 egg | 60 | attack_damage +0.5, movement_speed +0.04 | movement_speed +0.01 | use: Piercing Thrust<br>sneak: Pole Vault<br>key1: Dragoon Lunge<br>key2: Filo Rush | Featherlight |
| **Chaos Spear** (`chaos_spear`) | 2 | Hunter's Spear | 16 ender pearl, 32 amethyst shard | 100 | attack_damage +2, reach +0.6 | attack_damage +0.5 | use: Piercing Thrust<br>sneak: Hooked Pierce<br>key1: Chaos Spear<br>key2: Javelin Throw<br>key3: Dragoon Lunge | Longshaft, Victor's Vigor |
| **Meteor Spear** (`meteor_spear`) | 2 | Filolial Spear | 16 blaze rod, 48 glowstone dust | 100 | attack_damage +2, reach +0.6, movement_speed +0.05 | movement_speed +0.01 | use: Piercing Thrust<br>sneak: Windmill Sweep<br>key1: Meteor Spear<br>key2: Filo Rush<br>key3: Pole Vault | Featherlight, Victor's Vigor |
| **Portal Spear** (`portal_spear`) | 3 | Chaos Spear | 32 ender pearl, 32 obsidian, 24 Tensura magic stone | 160 | attack_damage +3, reach +0.9, movement_speed +0.04 | max_health +1 | use: Piercing Thrust<br>sneak: Portal Spear<br>key1: Chaos Spear<br>key2: Hooked Pierce<br>key3: Dragoon Lunge | Longshaft, Portal Echo |
| **Triaina** (`triaina`) | 3 | Meteor Spear | 64 quartz, 12 diamond, 8 Tensura medium quality magic crystal | 160 | attack_damage +3.5, reach +0.9, armor +1 | armor +0.5 | use: Piercing Thrust<br>sneak: Counter Thrust<br>key1: Meteor Spear<br>key2: Triaina<br>key3: Windmill Sweep | Longshaft, Victor's Vigor |
| **Bruenack** (`bruenack`) | 4 | Triaina | 8 Tensura high quality magic crystal, 32 diamond, 8 netherite scrap | 240 | attack_damage +5, reach +1.2, armor +2, armor_toughness +1 | attack_damage +1 | use: Piercing Thrust<br>sneak: Counter Thrust<br>key1: Triaina<br>key2: Meteor Spear<br>key3: Brionac | Longshaft, Victor's Vigor |
| **Clockwork Spear** (`clockwork_spear`) | 4 | Portal Spear | 8 clock, 64 gold ingot, 64 redstone | 240 | attack_damage +4, reach +1.2, movement_speed +0.06 | movement_speed +0.015 | use: Piercing Thrust<br>sneak: Portal Spear<br>key1: Minute Hand<br>key2: Hooked Pierce<br>key3: Windmill Sweep | Portal Echo, Memory of the Hours |
| **Dragon Minute Hand Spear** (`dragon_minute_hand`) | 5 | Bruenack, Clockwork Spear | 16 echo shard, 16 dragon breath, 4 Tensura hihiirokane ingot | 400 | attack_damage +7, reach +1.6, movement_speed +0.08, max_health +4, armor +2 | max_health +2, attack_damage +1.5 | use: Piercing Thrust<br>sneak: Counter Thrust<br>key1: Minute Hand<br>key2: Portal Spear<br>key3: Time Reversal | Longshaft, Portal Echo, Memory of the Hours, Victor's Vigor, Featherlight |
| **Spear of Lust I** (`lust_spear_1`)<br>curse, lust, +2 corruption a second | 1 | Small Spear | 32 nether wart, 16 magma cream | 80 | attack_damage +5, reach +0.6, movement_speed +0.05 | none (a curse form gives no permanent bonus) | use: Piercing Thrust<br>sneak: Temptation<br>key1: Dragoon Lunge<br>key2: Windmill Sweep | Fervor, Ravenous Aim |
| **Spear of Lust II** (`lust_spear_2`)<br>curse, lust, +3 corruption a second | 2 | Spear of Lust I | 8 ghast tear, 16 crying obsidian | 120 | attack_damage +7, reach +0.9, movement_speed +0.07 | none (a curse form gives no permanent bonus) | use: Piercing Thrust<br>sneak: Temptation<br>key1: Rapture<br>key2: Windmill Sweep<br>key3: Dragoon Lunge | Fervor, Ravenous Aim, Victor's Vigor |
| **Spear of Lust III** (`lust_spear_3`)<br>curse, lust, +4 corruption a second | 3 | Spear of Lust II | 4 wither skeleton skull, 32 crying obsidian | 160 | attack_damage +9, reach +1.2, movement_speed +0.09, attack_speed +0.2 | none (a curse form gives no permanent bonus) | use: Piercing Thrust<br>sneak: Temptation<br>key1: Rapture<br>key2: Thorn Chains<br>key3: Meteor Spear | Fervor, Ravenous Aim, Victor's Vigor, Longshaft |
| **Lust Spear IV** (`lust_spear_4`)<br>curse, lust, +6 corruption a second | 4 | Spear of Lust III | 16 Tensura high quality magic crystal, 24 ghast tear, 48 crying obsidian | 200 | attack_damage +11, reach +1.5, movement_speed +0.12, attack_speed +0.3 | none (a curse form gives no permanent bonus) | use: Piercing Thrust<br>sneak: Temptation<br>key1: Rapture<br>key2: Thorn Chains<br>key3: Lust's Embrace | Fervor, Ravenous Aim, Victor's Vigor, Longshaft |

```
base
 +- hunters_spear --- chaos_spear --- portal_spear --- clockwork_spear --+
 |                                                                      +-- dragon_minute_hand
 +- filolial_spear -- meteor_spear -- triaina -------- bruenack ---------+
 |
 +- lust_spear_1 --- lust_spear_2 --- lust_spear_3 --- lust_spear_4     (the Lust series)
```

## 5. The techniques

The numbers come from `SpearNumbers.java` (the same constants build the descriptions, so a text cannot disagree with the code) and are pinned by
`SpearNumbersTest` against the owner's budget: raw damage 3 to 12 for an ordinary technique, 10 to 20 for a heavy one, at most 32 for an ultimate; at most 12
foes; a cooldown of at least 60 ticks for a damage technique, 100 for a utility one and 600 for an ultimate as declared (the dispatcher shortens every cooldown by up to a
quarter at bond level 100; the test also pins that an ultimate still waits 600 ticks then, and Time Reversal 1800); a cost of 1 to 3 percent of the maximum magicules, 5 to 8 for an ultimate. The "most raw damage to one foe" column adds every hit one cast can
land on the same foe (five lances, the lane and the burst, every pulse of the chains).

**Hurt immunity.** The game ignores a second blow on the same creature that is no stronger than the last one for 10 ticks after it. So the pieces of one cast that hit the
same instant (the five lances, the lane and the burst, the seven fan arrows) are added up and struck once per foe (`Hits`), and waves, pulses and a second arrow are
spaced at least 10 ticks apart (`SkillRow.HURT_IMMUNITY`, pinned by the numbers tests); otherwise only the strongest of them would count.

| technique (`id`) | kind | most raw damage to one foe | most foes | cooldown | cost (of max magicules) |
|---|---|---|---|---|---|
| **Piercing Thrust** (`spear.piercing_thrust`) | damage | 6 | 6 | 60 ticks (3 s) | 1 percent |
| **Dragoon Lunge** (`spear.lunge`) | damage | 6 | 6 | 80 ticks (4 s) | 1 percent |
| **Pole Vault** (`spear.vault`) | utility | none | none | 100 ticks (5 s) | 1 percent |
| **Javelin Throw** (`spear.javelin`) | damage | 9 | 4 | 100 ticks (5 s) | 1.5 percent |
| **Windmill Sweep** (`spear.whirlwind`) | damage | 8 | 12 | 100 ticks (5 s) | 1.5 percent |
| **Counter Thrust** (`spear.riposte`) | damage | 7 | 1 | 140 ticks (7 s) | 1.5 percent |
| **Chaos Spear** (`spear.chaos_spear`) | damage | 12 | 5 | 90 ticks (4.5 s) | 1.5 percent |
| **Hooked Pierce** (`spear.hooked_pierce`) | damage | 5 | 1 | 160 ticks (8 s) | 2 percent |
| **Meteor Spear** (`spear.meteor_spear`) | damage, heavy | 18 | 8 | 140 ticks (7 s) | 2.5 percent |
| **Portal Spear** (`spear.portal_spear`) | utility | 4 | 12 | 160 ticks (8 s) | 2 percent |
| **Filo Rush** (`spear.filo_rush`) | utility | 5 | 6 | 200 ticks (10 s) | 2 percent |
| **Triaina** (`spear.triaina`) | damage, heavy | 14 | 10 | 160 ticks (8 s) | 2.5 percent |
| **Minute Hand** (`spear.minute_hand`) | damage, heavy | 16 | 12 | 240 ticks (12 s) | 3 percent |
| **Brionac** (`spear.brionac`) | ultimate | 26 | 12 | 900 ticks (45 s) | 6 percent |
| **Time Reversal** (`spear.time_reversal`) | ultimate | none | none | 2400 ticks (120 s) | 6 percent |
| **Temptation** (`spear.temptation`) | damage | 6 | 8 | 200 ticks (10 s) | 2.5 percent |
| **Rapture** (`spear.rapture`) | damage, heavy | 10 | 1 | 140 ticks (7 s) | 2.5 percent |
| **Thorn Chains** (`spear.thorn_chains`) | damage | 12 | 12 | 180 ticks (9 s) | 2 percent |
| **Lust's Embrace** (`spear.lusts_embrace`) | ultimate | 28 | 12 | 900 ticks (45 s) | 6 percent |

### What each technique does, how it shows itself, and how to answer it

* **Piercing Thrust** (`spear.piercing_thrust`; 60 ticks, 1 percent). A straight line from the eye, 8 blocks (10 with Longshaft), 0.9 wide; every foe in it, up to 6, is struck once 3 ticks after the cast. The line is fixed at the cast.
  * *Tell:* A thin light line from the spear's tip along the whole lane for 7 ticks, then a streak of star light and a flare on each foe struck.
  * *Answer:* Step out of the line in the 3 ticks; a wall ends the line; it hits only foes the wielder may strike.
* **Dragoon Lunge** (`spear.lunge`; 80 ticks, 1 percent). Dash up to 9 blocks along the aim to a safe landing, striking every foe in the line (up to 6, 1.6 wide) before moving. Refused with no room, or while riding.
  * *Tell:* A blade of light along the dash. It is instant, and under the 8 raw damage that asks for a wind-up.
  * *Answer:* Be off the line; the dash stops at walls and never ends over a gap.
* **Pole Vault** (`spear.vault`; 100 ticks, 1 percent). A leap of about 7 blocks forward and 2 to 3 up (launch speed 0.8 forward, 0.62 up) with Slow Falling for 3 seconds. No damage.
  * *Tell:* A ring of wind at the feet.
  * *Answer:* A utility move; nothing to answer.
* **Javelin Throw** (`spear.javelin`; 100 ticks, 1.5 percent). The spear is thrown after a 6 tick wind-up: a bolt of 0.7 radius at 2.2 blocks a tick for 14 ticks (about 30 blocks) that pierces up to 4 foes.
  * *Tell:* A glint at the hand and a thin line along the whole path during the wind-up (the path is fixed at the cast), then a needle of light flying down it.
  * *Answer:* Leave the line in the 6 ticks; any wall stops it.
* **Windmill Sweep** (`spear.whirlwind`; 100 ticks, 1.5 percent). A ring of wind round the wielder, radius 4.8, struck 4 ticks after the cast on whoever is inside then; foes are thrown outward.
  * *Tell:* A ring drawn on the ground and a ring of wind at the cast.
  * *Answer:* Step out of the ring (it is at the wielder's feet) or be farther than 4.8 blocks.
* **Counter Thrust** (`spear.riposte`; 140 ticks, 1.5 percent). A counter stance for 1.2 seconds. The next blow from a creature is cut by 65 percent and, if the attacker is a foe within 7 blocks, answered with a thrust of 7 raw; projectiles are only halved. Falls, fire and the void pass untouched. The stance is spent by the first blow.
  * *Tell:* A flare and a ring of wind on the wielder's chest for the length of the stance, an anvil sound when it catches a blow.
  * *Answer:* Bait it with a weak blow, or use a projectile; the window is short and one blow only.
* **Chaos Spear** (`spear.chaos_spear`; 90 ticks, 1.5 percent). Five lances at the nearest foes the wielder can see within 14 blocks (one foe takes all five): 2.4 each. They land 10 ticks after the cast, and only on a foe still in sight then.
  * *Tell:* Five needles of light fly from the spear to their targets, one a tick.
  * *Answer:* Break line of sight in the 10 ticks (a pillar, a wall), or be farther than 14 blocks at the cast.
* **Hooked Pierce** (`spear.hooked_pierce`; 160 ticks, 2 percent). The foe under the crosshair within 12 blocks is struck for 5 and hauled to arm's length. A boss is not moved: the wielder is placed beside it instead. Hard control, so it goes through the shared 12 second control lock.
  * *Tell:* A thread of light from the spear to the foe for the 6 ticks before the strike.
  * *Answer:* Break line of sight in the 6 ticks; the lock means it cannot be chained.
* **Meteor Spear** (`spear.meteor_spear`; 140 ticks, 2.5 percent). After a 10 tick glow the spear streaks 16 blocks (20 with Longshaft): up to 8 foes in the 1.4 wide lane take 14, and the point where it ends bursts for 4 more within 2.5 blocks (18 at most on one foe).
  * *Tell:* A line along the lane and a ring on the ground where it will end, during the 10 ticks; a blade of light and a star streak when it fires.
  * *Answer:* Leave the lane and the ring in the 10 ticks; a wall shortens the lane.
* **Portal Spear** (`spear.portal_spear`; 160 ticks, 2 percent). A portal opens at the point aimed at (up to 24 blocks) and the wielder steps through 6 ticks later onto solid ground within 3 blocks of it; foes within 2.8 blocks of the exit take 4. Refused when there is no footing; refused while riding; Portal Echo adds Speed II for 3 seconds.
  * *Tell:* A ring-gate at the wielder and another at the exit for 18 ticks.
  * *Answer:* A move, not an attack; the 4 raw at the exit is a nudge.
* **Filo Rush** (`spear.filo_rush`; 200 ticks, 2 percent). Speed III for 2.5 seconds; every 3 ticks the foes within 1.9 blocks (up to 6 in all) take 5 once and are knocked aside.
  * *Tell:* A trail of wind along the run.
  * *Answer:* Step aside; each foe is struck once.
* **Triaina** (`spear.triaina`; 160 ticks, 2.5 percent). A beam of energy: after a 12 tick wind-up it runs 18 blocks (22.5 with Longshaft), 1.4 wide, striking up to 10 foes for 14.
  * *Tell:* A line from the spear along the beam during the 12 ticks; it follows the wielder's aim and is renewed every 4 ticks, so the last line seen is the beam.
  * *Answer:* Leave the line before the beam fires; a wall stops it.
* **Minute Hand** (`spear.minute_hand`; 240 ticks, 3 percent). After 8 ticks a hand of light 9 blocks long sweeps a full turn round the wielder in 30 ticks (15 steps of 2 ticks, 24 degrees each, 14 degrees of width on either side). Each foe it passes is struck once for 16. The hand needs line of sight from the wielder's chest.
  * *Tell:* The ring on the ground at the cast, the starting line of the hand for 10 ticks, then the hand drawn at every step.
  * *Answer:* Break line of sight, or move with the hand; it moves in one direction, so a foe can run round with it.
* **Brionac** (`spear.brionac`; 900 ticks, 6 percent). The grand beam: a ring gathers at the wielder's feet for 24 ticks while a line shows the beam, then a beam 26 blocks long and 2.6 wide strikes up to 12 foes for 26 and drives them back.
  * *Tell:* A magic circle under the wielder, a line that follows the aim (renewed every 4 ticks) and a growing flare at the spear.
  * *Answer:* Leave the line in the 1.2 seconds; a wall stops it.
* **Time Reversal** (`spear.time_reversal`; 2400 ticks, 6 percent). The wielder returns to where they stood 3 to 8 seconds ago, regains up to a quarter of their maximum health, and loses the harmful effects gained since. Section 7.
  * *Tell:* A clock dial turns back at the place left and at the place reached, for 2 seconds.
  * *Answer:* It is not an attack. Its cost is the 2400 tick cooldown (still 1800 at level 100) and 6 percent of the magicules.
* **Temptation** (`spear.temptation`; 200 ticks, 2.5 percent). Lust. After 10 ticks the foes within 9 blocks (up to 8) are drawn towards the wielder for 1.2 seconds; those who arrive take 6 at the end. A boss is only slowed. Hard control: the shared lock.
  * *Tell:* A ring on the ground and a magic circle at the wielder for the 10 ticks, threads of light from the foes to the wielder while they are drawn.
  * *Answer:* Leave the ring in the 10 ticks (the pull is slow: 0.3 blocks a tick).
* **Rapture** (`spear.rapture`; 140 ticks, 2.5 percent). Lust. A thread fixes on the nearest foe in an 80 degree cone within 10 blocks; 6 ticks later the wielder lunges beside it (if there is still footing), strikes for 10 and drinks 5 percent of their maximum health through the balance law's heal. Adds 12 corruption.
  * *Tell:* A magenta thread from the spear to the prey for the 6 ticks and a flare on the prey.
  * *Answer:* Leave the cone, break the line, or get more than 13 blocks away in the 6 ticks: the lunge then does nothing.
* **Thorn Chains** (`spear.thorn_chains`; 180 ticks, 2 percent). Lust. Chains and thorns burst from the ground within 4 blocks of the point aimed at (up to 14 blocks): after 10 ticks, 5 pulses 10 ticks apart, each 2.4 to every foe inside (12 in all) and a short slow.
  * *Tell:* A ring on the ground and a chain effect for the whole time.
  * *Answer:* Step out of the ring; the slow is only Slowness II for 1.25 seconds.
* **Lust's Embrace** (`spear.lusts_embrace`; 900 ticks, 6 percent). Lust, ultimate. After 16 ticks everything within 10 blocks (up to 12 foes) is dragged to the wielder over 20 ticks, then a burst of 28 strikes everything within 4 blocks. A boss is struck but not moved. Adds 40 corruption. Hard control: the shared lock.
  * *Tell:* A ring on the ground, a large magic circle at the wielder for about 2 seconds, threads from every foe being drawn.
  * *Answer:* Be out of the 10 blocks at the cast; the pull is 0.55 blocks a tick, so a fast foe or a wall can leave it.

## 6. Passives

A form lists its passives; the logic checks them with `HeroForm.has`. Names and texts are in the lang fragment (`hero.passive.spear.<id>`).

* **Memory of the Hours** (`time_memory`): While the spear is in your hand it writes down where you stand, how healthy you are and which ills you carry, once a second, for the last 8 seconds. Time Reversal reads it back. The memory is dropped when the spear is put away, at logout and at death.
* **Featherlight** (`featherlight`): You take no damage from falls while the spear is in your hand.
* **Longshaft** (`longshaft`): The Piercing Thrust, the Meteor Spear and Triaina run 25 percent longer.
* **Victor's Vigor** (`kill_vigor`): A kill mends 2 percent of your maximum health.
* **Portal Echo** (`portal_echo`): Stepping out of a Portal Spear leaves a rush of speed (Speed II) for 3 seconds.
* **Fervor** (`lust_fervor`): Each blow of the spear adds 6 percent to your melee damage, up to 5 times (30 percent); the stacks last 6 seconds after the last blow.
* **Ravenous Aim** (`ravenous_aim`): Your thrusts, throws, lunge and beams turn towards the nearest foe in sight within 9 blocks and 75 degrees of where you look. Restraint is gone: you cannot aim away from prey.

## 7. Time Reversal, in full

**What the owner asked for.** A real, readable, bounded mechanic: an ultimate of a high form with a cooldown of at least 1800 ticks that rewinds the WIELDER to
where and how they were up to eight seconds ago: the place (only if safe: air above a solid floor, not inside blocks, same dimension; else the nearest safe
place within 3 blocks; else the skill is refused), the health (up to a quarter of the maximum above the current, through the balance law's heal) and the
debuffs gained in the window. It never rewinds the world or other players. A per-player ring buffer of one snapshot a second, filled while the weapon is
summoned and the form has the passive `time_memory`.

**How it is built.**

* `RewindBuffer` (pure, tested): a ring of 10 snapshots. A snapshot is the game time, x, y, z, health, the dimension, whether the wielder burned and the ids of
  the harmful effects they carried. One is written when 20 ticks have passed since the newest, so the buffer always holds the last 8 or 9 seconds. If the game
  clock goes backwards (a reloaded world, `/time set`) the buffer empties itself instead of mixing two timelines.
* `SpearTime.remember` (every 5 ticks while summoned, only with the passive `time_memory`): writes a snapshot when one is due, never while the wielder is dead,
  a spectator or riding. The memory is in server memory only, per player; it is dropped when the spear is put away, at logout, at respawn and when the server
  stops. A rewind across a relog or a death would be a bug, not a feature.
* `RewindPlanner` (pure, tested): picks the **oldest** snapshot that is at least 60 ticks (3 s) and at most 160 ticks (8 s) old, in the wielder's present
  dimension, whose block can be stood in; if that block cannot, the nearest block that can within 3 blocks of it (`SafeSpot.nearest`, which prefers the smaller
  vertical difference on a tie, so the answer never depends on the order the world is asked in); a moment in mid-air (a fall) is skipped in favour of a later one.
  When no snapshot qualifies the technique is refused with a reason ("not run long enough" or "no safe footing") and costs nothing: the dispatcher starts the
  cooldown and takes the magicules only when a cast returns true.
* `Moves.standable` answers the world's side of "safe": the chunk is loaded (nothing here ever loads one), something to stand on within half a block down or 0.6
  up (the collision shape of the block under the feet or of their own cell, so slabs, carpets, snow layers and soul sand count), room for the whole body
  (`noCollision`), no liquid, and none of fire, campfires, lava, magma, cactus, sweet berry bushes, wither roses, powder snow, cobwebs, end or nether portals in
  the feet, the head or under the feet; inside the world's height.
* `SpearTime.rewind`: teleports the wielder to the exact x and z of the moment (or the centre of the fallback block) at the real standing height, clears the
  fall distance and the speed; heals `min(health then - health now, 25 percent of the maximum)` through `HeroCtx.heal` (never takes health away); removes
  every harmful effect that was not on the wielder in that snapshot; puts out fire the wielder did not have then and clears powder-snow freezing; empties the
  buffer (so the next rewind needs 3 seconds of new memory); fires the quest event `hero.rewind`.
* It refuses while the wielder rides anything. It touches nothing else: no block, no entity, no other player, no item, no cooldown, no experience.

**Why 3 seconds at least.** A younger moment would make the skill a one-second blink and nothing could be called "back in time"; the window of 3 to 8 seconds
is long enough to undo a mistake (a fall into a trap, a lost trade of blows) and short enough that it cannot erase a whole fight.

**Abuse considered.** Rewinding out of lava or the void: the old place must itself be safe, and a fall in progress is skipped. Rewinding after death: the buffer
is emptied at respawn. Rewinding between dimensions: refused (the planner compares the dimension ids). Healing: a quarter of the maximum at most, once in
2400 ticks (1800 at level 100). Stacking with the control lock: it applies no control. Using it on the spot to dodge a boss attack: it needs 3 seconds of
memory and moves the wielder to where they stood at least 3 seconds ago, which may be right in front of the boss again.

**What is tested.** `RewindBufferTest` (order, overwrite when full, a clock that goes back empties it, `due`), `RewindPlannerTest` (the oldest safe moment,
the age edges, another dimension, the fallback within 3 blocks and not 4, a mid-air moment skipped, nothing safe refuses, negative coordinates, the heal
arithmetic, the "has a past" question), `SafeSpotTest` (the nearest spot, ties, the radius, the order of asking), `SpearNumbersTest` (cooldown at least 1800 also at level 100, window 160, step 20,
capacity, heal cap 0.25, radius 3). The in-game self-test `hero.weapon.spear` prepares a past with a safe place in it, moves the stand-in player five blocks away and requires the
cast to bring them back.

## 8. The Lust series (the curse forms)

**Canon.** Lust is Motoyasu's series; its weapon draws people towards the wielder (Temptation) and strengthens those who are under lust, mostly himself. The
wiki documents no downside [S]. The owner's text asks for: the wielder drawn to the nearest foe (bounded), blows that grow, restraint lost.

**What the series does here.** Four forms (`lust_spear_1` to `lust_spear_4`), each a step above the one before in attack stat, reach, speed, corruption a second
and techniques. The WEAPON's side of the curse is in this mod's code (`SpearLust`, `SpearAim`); the engine's side (despair, the unlock, the price, the corruption
bar, the overrun, anything it does to the wielder's body each second) belongs to the curse builder and is not duplicated here.

| form | corruption a second | what is new |
|---|---|---|
| Spear of Lust I | 2 | Fervor, Ravenous Aim, Temptation (sneak) |
| Spear of Lust II | 3 | Rapture, Victor's Vigor |
| Spear of Lust III | 4 | Thorn Chains, the Meteor Spear in the Lust colours, Longshaft |
| Lust Spear IV | 6 | Lust's Embrace (the ultimate); attack speed +0.3 |

* **Blows grow (Fervor).** Every melee blow of the spear adds one stack of 6 percent melee damage, up to 5 (30 percent); the stacks last 6 seconds after the last
  blow. It is one transient attribute modifier (`nusmp:spear_fervor`, never saved), replaced only when the number of stacks changes, removed when the form
  changes, the weapon is put away or the time runs out.
* **Restraint is lost (Ravenous Aim).** The direction of the thrust, the throw, the lunge, the meteor and the beams snaps to the nearest foe the wielder can see
  within 9 blocks and 75 degrees of their look. The wielder cannot aim away from prey. It never reaches through a wall or across a room.
* **Drawn to the nearest foe.** Rapture runs the nearest foe down. The slow drift of the wielder's own steps towards foes (if the owner wants it) is the curse
  engine's job (design.md section 7 lists "Lust: the wielder is drawn to the nearest foe" as the downside by sin); this weapon adds no per-second steering.
* **Why it costs.** Rapture adds 12 corruption and Lust's Embrace 40 on top of the form's own rate, so the strongest techniques push the wielder towards the
  Overrun (1000). The forms give no permanent bonus. A hard-control technique (Temptation, Lust's Embrace) shares the 12 second control lock with everything else,
  never moves a boss, and pulls at most 8 or 12 foes.

## 9. Engravings (CLAUDE.md rule 6)

The lines for `item/WeaponEngravings.java` (shared file, so the lead adds them; the item id is the forms file's name):

```java
Map.entry("legendary_spear", List.of(e("holy_weapon", 2), e("barrier_piercing", 3), e("swift", 2), e("restoration", 1))), // Motoyasu's Cardinal weapon: holy, a lance that goes through wards, quick, and it mends (Time Reversal)
```

* `holy_weapon` 2: the Legendary Spear is one of the four Holy (Cardinal) Weapons; the engraving is the Tensura one for exactly that.
* `barrier_piercing` 3: a lance and its beams (Triaina, Brionac) go through wards; the same level as the Genesis blade, the mod's other "goes through magic" weapon.
* `swift` 2: the weapon is the fast one of the four (the Lunge, the Vault, Filo Rush, the Portal).
* `restoration` 1: Time Reversal mends its wielder; a single level because the mend is capped at a quarter.

All four ids were checked against the Tensura registry dump (`tensura_ids/clean.json`: enchantments `holy_weapon`, `barrier_piercing`, `swift`, `restoration`).

## 10. Effects used (the mod's own system only)

`VfxShape`: `THREAD_LINE` (every tell), `STAR_MAGIC` (the strike streak), `LIGHT_BLADE` (the lunge, the meteor, the beams), `LIGHT_FLARE` (hits), `COMPASS_NEEDLE`
(the javelin, the chaos lances), `WIND_RING`, `WIND_SLASH`, `WIND_GALE` (the vault, the sweep, the rush), `SPACE_RINGGATE` (the portal), `TIME_ACCEL` (the Minute
Hand), `TIME_REWIND` (Time Reversal), `MAGIC_CIRCLE`, `MAGIC_CIRCLE_EXPLOSION` (Brionac, Temptation, Embrace), `CHAIN_FX2` (Thorn Chains), `FIRE_BURST`
(Brionac's hits, visual only). `Tells.ring` draws every ground ring. Sounds are `SoundEvents`. No vanilla particle is used, and no technique sets anything on fire:
fire would be damage outside the balance law.

Colours: gem red `0xFFFF5C4D`, white-gold light `0xFFFFE8A0`, silver `0xFFE4E8F0`; the Lust forms turn every effect `0xFFD94A8C`. Time Magic's own files were not touched
(only existing shapes are reused).

## 11. Files, tests, how to check

* `weapon/spear/` : `SpearLogic` (the hooks; its constructor registers the self-test and the memory cleaning), `SpearSkills` (joins a row to a body), `SpearNumbers`
  and `SpearIds` (pure), `SpearStrikes` (thrust, javelin, chaos, meteor, Triaina, Brionac), `SpearMotion` (lunge, vault, portal, rush, hook), `SpearGuard` (sweep,
  counter stance, Minute Hand), `SpearTime` (memory and reversal), `RewindBuffer` and `RewindPlanner` (pure), `SpearLust` (the Lust techniques and Fervor),
  `SpearAim`, `SpearState` (the `spear` compound of the bond), `SpearFx`, `SpearSelfTest`.
* `weapon/pierce/` (shared with the Bow; named for the thing both do): `Geo`, `SafeSpot`, `SkillRow` (pure), `Foes` (who a technique may strike), `Hits` (one
  blow per foe with the sum of a cast's pieces, because vanilla hurt immunity would otherwise keep only the strongest), `Rays`, `Moves`, `Tasks`.
* `src/main/resources/data/nusmp/hero/forms/legendary_spear.json` : 10 ordinary forms and 4 Lust forms.
* Tests (`src/test/java/.../heroweapon/weapon/`): `pierce/GeoTest`, `pierce/SafeSpotTest`, `pierce/FormsFileCheck` (the checker of design.md section 6),
  `spear/RewindBufferTest`, `spear/RewindPlannerTest`, `spear/SpearNumbersTest`, `spear/SpearFormsFileTest`. CI runs them with JUnit; the builder ran them with the
  repo's small JUnit shim and mutation-checked two of them (a lowered Time Reversal cooldown, a window edge moved by one tick: both fail the intended test).
* In game: the smoke test runs `hero.weapon.spear` (the paper checks, then a stand-in player with the weapon in hand casts every technique beside a husk and the
  time reversal is proven on a prepared past).

## 12. For the others

* **Lead.** Shared files that need the Spear's lines: `WeaponEngravings` (section 9), `en_us.json` (the lang fragment `hero/frag/hsb.lang.json`: `hero.skill.spear.<id>.name`
  and `.desc` for the 19 techniques, `hero.passive.spear.<id>` and `.desc` for the 7 passives; no `hero.msg.*` key is used, the action-bar lines are plain
  English strings like the older weapons'). The self-test is `hero.weapon.spear`. The quest key `hero.rewind` is fired once per successful Time Reversal
  (`NusmpEvents.fire(player, "hero.rewind")`; it does nothing for a stand-in player). No config key: the weapon builders keep their numbers in code.
* **Core and dispatcher.** Costs and cooldowns are the dispatcher's; a technique that cannot work returns false before anything happens (no target, no footing, riding,
  the control lock taken). The hooks used: `onTick` (the memory and the Fervor timeout), `onHit` (Fervor), `onDamaged` (the counter stance, Featherlight; it runs when the
  weapon is not summoned too, and Featherlight then does nothing because it asks for the weapon in hand), `onKill` (Victor's Vigor), `onFormChanged` and `onDismiss`
  (Fervor off, the memory dropped). The Spear keeps one small compound in the bond: `spear` (the stance and the Fervor stacks, absent until used).
* **Forms and curse builders.** The forms file follows design.md section 6 (checked by `FormsFileCheck`). The curse engine's part of Lust (the despair, the unlock, the
  price, the corruption bar, the overrun and any per-second effect on the wielder's body) is not in this weapon; the weapon only adds corruption through
  `HeroCurse.addCorruption` (Rapture 12, Lust's Embrace 40) and its own techniques.
* **Art builder (`hart1`).** Section 1 is yours. Nothing in the logic depends on the mesh; the forms only distinguish the `base` and the `curse` look.
* **Anyone reusing `weapon/pierce`.** `Foes`, `Hits`, `Moves`, `Tasks`, `Geo`, `SafeSpot` and `SkillRow` are public on purpose; keep their tests green.

## 13. What is not verified

Nothing of the Spear has been run in a game. Honestly:

* **Checked:** the code type-checks against the API stub dump (the only errors left are the stub's own "MobEffectInstance is abstract", which the same stubs give in about
  fifty older files of the mod); the pure logic and the data file pass their JUnit tests under the repo's small JUnit shim; two tests were mutation-checked (a lowered Time
  Reversal cooldown and a window edge moved by one tick both fail the intended test); every `tensura:` item id of the forms file and the four engraving ids were looked up in
  the Tensura registry dump.
* **Not checked, and why it matters:**
  * That the self-test `hero.weapon.spear` passes: it has never run (no game here).
  * What any effect looks like: the `VfxShape` values were chosen from their names and comments, never rendered. The colours and sizes are guesses.
  * `ServerPlayer.teleportTo(x, y, z)` as the way to move the wielder (the repo's older spells do the same) and that the rotation is kept; the fall distance and the
    speed are reset by hand.
  * The distances of the Pole Vault (about 7 blocks forward and 2.5 up) and of the hook are physics estimates from the launch speed with vanilla gravity and drag, not
    measured; the launch uses `setDeltaMovement` and `hurtMarked` as the older book spells do.
  * The timing of `SpellRuntime.later` and `.zone` was read from its source (a zone created inside a delayed task starts on the next tick; `Tasks.pulses` adds one tick so
    the last pulse is not lost); it was not observed.
  * That the foes it spares really are spared: animals, villagers and armour stands are not struck unless they fight the wielder; tamed pets are skipped when they
    implement `OwnableEntity`; Tensura's own summons are protected only if they do (or if `isAlliedTo` says so). Not checked against Tensura's classes.
  * Slowness II as "slowed", and the soft slow of Thorn Chains: not hard control by this page's reading of the rules. If the owner counts a slow as control, the
    places are `SpearLust` (Temptation's boss slow, the Chains' slow) and `BowRain` (Frozen Rain).
  * Safe places: `Moves.standable` is deliberately strict (a trapdoor, a fence gate or a leaf carpet may be refused where a player could stand); it will refuse
    rather than risk a bad landing. It does not know of any Mega-Dungeon no-teleport rule; if the dungeon has one, the dispatcher is the place to veto.
  * Chunks: the lines of the techniques (`Rays`, `Foes.sees`, the mod's own `aim` and bolts) use `Level.clip`, which can load a chunk when a line ends beyond the loaded
    area; the Brionac beam reaches 26 blocks and the Portal Spear aims 24, within a few chunks of the wielder, so it only matters with a very small view distance or at the world's edge, as for the mod's older bolts.
    Everything that MOVES the wielder (`Moves`) asks `hasChunkAt` first and refuses an unloaded place; nothing here ever force-loads a chunk on purpose.
  * Balance: the numbers obey the owner's budget (tested) but have never been felt in play.
* **Canon:** all of section 2 is from search summaries of fan-wiki pages that could not be opened; the spellings (Bruenack against Brionac, Chaos Thrust against Chaos
  Spear) differ between sources. The Lust forms I to III and the forms `hunters_spear`, `filolial_spear`, `clockwork_spear` are this mod's inventions. Time Reversal is
  the owner's bounded design, not the canon loop.
