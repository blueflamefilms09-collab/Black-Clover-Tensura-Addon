# Vassal Claw (`nusmp:vassal_claw`, `HeroKind.CLAW`, class Seven Star, style SWING)

Builder `hva`. Code: `heroweapon/weapon/claw/**` (+ the shared helpers of the four striking weapons in `heroweapon/weapon/strike/**`), forms file
`data/nusmp/hero/forms/vassal_claw.json`, lang fragment `hva.lang.json`. This page is written in the order the art builder needs it: the look first.

## 1. Look (for the art builder `hart2`)

Canon, from search summaries of the fandom wiki: the Vassal Claw (Seven Star Claw) belongs to Raphtalia's world, is a vassal of the Legendary Shield, and is the
slashing counterpart of the Vassal Hammer (a shield is strong against slashes and weak against impact). Its canon forms are the **Small Claw**, the **Rope Claw**
and the **Dragon Head Claws**, plus a **Cursed Claw** in the curse list; its canon wielder is Filo (Filolia). The wiki gives no picture. The model below is DESIGN.

* **Silhouette.** A hand-claw: a gauntlet glove whose knuckle bar carries **three long curved blades** that fan slightly outward and sweep forward and up. The
  blades are the weapon; the glove is the handle. It must read as a fist full of claws from the front and as a strip of three curved blades from the side.
  It is the lightest and smallest of the four striking weapons.
* **Size.** No existing class fits a hand-claw: use `handitem` (0.5 to 0.9 blocks) or add a class `claw` (min 0.5, max 0.9, head_min 0.0, grip 0.2) to the class
  table fragment `tools/heroweapon/classes_hart2.json`; aim for 0.75 blocks from the wrist strap to the blade tips (the blades about 0.5 of that).
* **Parts (8 to 14 named parts).** `wrist_strap`, `cuff` (leather, flared), `back_plate` (steel over the back of the hand), `palm_guard`, `knuckle_bar`,
  `gem_bezel`, `gem`, `blade_left`, `blade_mid` (longest), `blade_right`, `blade_spur_left`, `blade_spur_right` (small hooks behind the blades), `finger_strap`.
* **Palette.** Leather: black-brown `#3A2A22`, strap stitching `#6B4A33`. Steel plates `#8E96A8` with bevels `#B9C2D6`. Blades: bright steel `#C9D2E3` with a thin
  cold edge highlight `#8FB4FF` along the cutting side; the blade roots darker `#5A6070`. Bezel: worn gold `#B8893A`.
* **The gem.** One amber-orange cabochon (about 0.1 across) in a small gold bezel on the back plate, centred over the knuckles. Glow colour `#FFA030`
  (the same warm orange the Beast effects of the mod use, so the claw marks it leaves match its gem).
* **Glow skin.** The gem, a hairline of amber along the cutting edge of each blade (alpha under 35 percent), and the seven-star mark (seven tiny dots) on the back plate.
* **Orientation.** Mesh -x is forward: the blades point -x and curve up (+y) at their tips, away from the wearer's hand; the glove is at the butt (low y). Tip up and
  forward on the icon diagonal. Blades curve toward the edge side (-x), like a katana, not toward the spine.
* **Icon.** Keep the 16 x 16 sprite as the inventory icon (three claws with an amber dot).
* **Curse form.** The Claw has one small curse form (Cursed Claw). The vassal weapons have no curse mesh: the curse form keeps this look; only the corruption HUD differs.

## 2. Canon: what is from a summary, what is from memory, what is design

The fandom wiki could not be opened from the build machine (a fetch of its pages fails), so every canon claim below comes from SEARCH SUMMARIES of the pages named, run on
2026-10-18. Tags: **S** = a search summary of that page, **M** = my memory of the series, **D** = my design for Minecraft. Nothing here was read on the page itself.

Pages (all on https://shield-hero.fandom.com/wiki/): `Vassal_Claw`, `Vassal_Weapons`, `Vassal_Weapons/Web_Novel`, `Heroes`, `Heroes_(Web_Novel)`, `Filolia`,
`Takt_Alsaholn_Faubrey/Powers_and_Abilities`, `Curse_Series`, `Curse_Series/Weapon_List`, `Vassal_Hammer` (for the shared rule of unlocking forms).

| fact | tag | what the mod does with it |
|---|---|---|
| The Vassal Claw (Seven Star Claw) is one of the eight vassal weapons, from the Shield World, a vassal of the Legendary Shield with the Vassal Hammer. The Shield is strong against the Claw's slashing and weak against the Hammer's impact | S | the Claw is "the slashing weapon", the light counterpart of the Hammer |
| Wielders by source: Filolia (earlier timeline), Takt (a false hero, who was stripped of all seven stolen vassal weapons), Filo holds the claw and the axe in the web novel; the current holder is unknown | S | not used |
| Three skills are named: **Wahnsinn Claw** (fires a light attack that inflicts a heavy curse on those hit; the Takt page also calls it Madness Claw), **Air Strike Slash** (a horizontal claw slash) and **Second Slash** (a follow-up to the Air Strike Slash, a second powerful slash) | S | all three are techniques here, under their canon names |
| Forms: **Small Claw** (no skill or effect listed), **Rope Claw** (equip bonus Air Strike Slash, special effect "Rope"), **Dragon Head Claws** (unlocked by combining several claws through the hammer's power-up method; no skill listed), and a **Cursed Claw** in the Curse Series list (equip bonus Wahnsinn Claw). The wiki does not say which form unlocks Second Slash | S | the base form is the Small Claw; Rope Claws carry the passive `rope_hook`; the Dragon Head Claws are the top form; the Cursed Claw is the one curse form (section 7) |
| Power-up method: **Skill Mastery**: the more a skill is used, the more its levels and abilities improve | S | the passive `skill_mastery`: five levels per technique (section 5) |
| Forms give equip bonuses and special effects; after enough time with a form a user can keep its bonuses; special effects stay tied to their form | S | the forms file: each form lists its techniques and passives |
| The Curse Series is tied to the seven deadly sins and unlocked by an extreme psychological stimulus. The summaries do NOT say which sin the Cursed Claw belongs to; Takt used the Wahnsinn Claw without the usual side effects | S | the sin of the Cursed Claw is **wrath**, a DESIGN choice (section 7) |
| All other technique names (Hemorrhage, Pounce, Shredding Rush, Predator's Instinct, Dragon Rend), the wounds that bleed, the forms Bone Claws, Rope Claws' homing, the numbers | D | the owner's brief: fast clawing strikes, bleeding, leaps, shredding; the lightest of the four; forms from bones and hides |

## 3. The forms tree

Five forms: a line `base` -> `bone_claws` -> `rope_claws` -> `dragon_head_claws` (4 ordinary forms; the brief asks for 3 to 5) and one small curse form `cursed_claw` (section 7).
Names follow the canon list where it has one (Small Claw, Rope Claw, Dragon Head Claws, Cursed Claw); the Bone Claws are design (bones and hides, as the brief says). A form has
five slots (`use`, `sneak`, `key1`, `key2`, `key3`); the Dragon Head Claws trade the Pounce and the Shredding Rush for Predator's Instinct and the ultimate, so a player who wants
the leaps keeps the lower forms (and `rope_hook` therefore sits only on the Rope Claws, where the Pounce is).

The table is generated from `data/nusmp/hero/forms/vassal_claw.json`:

| form (id) | tier | parents | requirements | mastery | stats | ability bonus | skills (slot: id) | passives |
|---|---|---|---|---|---|---|---|---|
| Small Claw (`base`) | 0 | - | none | 0 | movement_speed +0.02 | - | use: `claw.air_strike_slash`, sneak: `claw.second_slash`, key1: `claw.hemorrhage` | `rending_blows`, `skill_mastery` |
| Bone Claws (`bone_claws`) | 1 | `base` | 40 x `minecraft:bone`, 24 x `minecraft:leather` | 100 | attack_damage +1, attack_speed +0.3, movement_speed +0.03 | movement_speed +0.01 | use: `claw.air_strike_slash`, sneak: `claw.second_slash`, key1: `claw.hemorrhage`, key2: `claw.pounce` | `rending_blows`, `skill_mastery`, `scent_of_blood` |
| Rope Claws (`rope_claws`) | 2 | `bone_claws` | 48 x `minecraft:string`, 16 x `minecraft:phantom_membrane` | 180 | attack_damage +1.5, attack_speed +0.4, movement_speed +0.05, reach +0.5 | attack_speed +0.1, movement_speed +0.01 | use: `claw.air_strike_slash`, sneak: `claw.second_slash`, key1: `claw.hemorrhage`, key2: `claw.pounce`, key3: `claw.shredding_rush` | `rending_blows`, `skill_mastery`, `scent_of_blood`, `rope_hook` |
| Dragon Head Claws (`dragon_head_claws`) | 3 | `rope_claws` | 4 x `minecraft:dragon_breath`, 16 x `minecraft:diamond`, 32 x `minecraft:phantom_membrane` | 300 | attack_damage +3, attack_speed +0.5, movement_speed +0.07, reach +0.5, max_health +4 | attack_damage +0.75, movement_speed +0.015, max_health +1 | use: `claw.air_strike_slash`, sneak: `claw.second_slash`, key1: `claw.hemorrhage`, key2: `claw.predator_instinct`, key3: `claw.dragon_rend` | `rending_blows`, `skill_mastery`, `scent_of_blood`, `dragon_fangs` |
| Cursed Claw (`cursed_claw`) - CURSE, sin wrath, 6 corruption/s | 1 | `base` | none (the curse system teaches it) | 150 | attack_damage +4, attack_speed +0.4, movement_speed +0.04, armor -2 | - | use: `claw.air_strike_slash`, sneak: `claw.wahnsinn_claw`, key1: `claw.hemorrhage`, key2: `claw.pounce`, key3: `claw.shredding_rush` | `rending_blows`, `skill_mastery`, `madness` |

Fairness of the climb: tier 1 is 64 common items (bones and leather), tier 2 is 64 items of string and phantom membrane (the membrane needs the night sky), tier 3 is 4 dragon's
breath, 16 diamonds and 32 phantom membranes (the dragon's breath needs the End: a real task). Every item id was checked against the vanilla item list; the forms system resolves them
again at start-up and the CI self-test `hero.forms` fails on a wrong id.

## 4. The techniques

Eight techniques (seven for the tree, one for the curse form), one ultimate. The budget rows (the pure table `ClawNumbers`, judged by `Budget` in `ClawNumbersTest` AT THE TOP
MASTERY, because a technique grows 30 percent harder with use; "raw" is before the bond-level factor 1.0 to 2.2 and before the balance law; the cooldown is the BASE cooldown, see section 11):

| skill id | kind | raw per hit | hits | cooldown | cost (of max magicules) | targets |
|---|---|---|---|---|---|---|
| `claw.air_strike_slash` | damage | 6 | 1 | 60 ticks (3 s) | 1 % | 3 |
| `claw.second_slash` | damage | 7 | 2 | 70 ticks (3.5 s) | 1.5 % | 6 |
| `claw.hemorrhage` | heavy | 15 | 1 | 100 ticks (5 s) | 2 % | 1 |
| `claw.pounce` | damage | 6 | 1 | 100 ticks (5 s) | 1.5 % | 8 |
| `claw.shredding_rush` | damage | 3 | 4 | 140 ticks (7 s) | 2 % | 1 |
| `claw.predator_instinct` | utility | 0 | 0 | 260 ticks (13 s) | 2 % | 12 |
| `claw.dragon_rend` | ultimate | 7 | 3 | 900 ticks (45 s) | 6.5 % | 8 |
| `claw.wahnsinn_claw` | heavy | 10 | 1 | 160 ticks (8 s) | 3 % | 6 |

The Claw is the fast weapon, so only the heavy blows (Hemorrhage, Wahnsinn Claw) and the ultimate show a tell; the others are a visible streak or crescent that the foe can
read as it comes, and none of them does more than 8 raw in one hit. A cast that finds no target returns false, so the dispatcher gives the cost back. "Opens a wound" means the
bleed of section 6.

* **Air Strike Slash** (`use` of every tree form; the canon skill).
  *What:* slashes the air and sends a crescent flying along the look, 12 blocks at most (it stops at a wall), at 1.5 blocks a tick. It cuts up to 3 foes in its line (0.9 blocks either side) and opens 1 wound on each.
  *Numbers:* raw 6 times the mastery factor. Each foe is hit when the crescent arrives (distance / 1.5 ticks later, 8 ticks at the far end) and is MISSED if by then it has stepped more than about 1.6 blocks (plus half its width) from the line. A landed cut also arms the combo of the Second Slash for 100 ticks.
  *Tell:* the crescent itself, a wind slash shape flying out in amber, and a sweep sound.
  *Counter:* sidestep; the crescent takes time to cross the distance.
  *Cost / cooldown:* 1 %, 60 ticks.
* **Second Slash** (`sneak` of the tree forms; the canon follow-up).
  *What:* two crossing cuts, 2 and 5 ticks after the cast, in a fan of 110 degrees in front of the wielder, 3.4 blocks long; each cut finds its foes anew (up to 6) and opens a wound on each, so 2 wounds per foe per cast.
  *Combo:* if it is used within 100 ticks (5 s) of an Air Strike Slash that landed, it is the follow-up: it lunges toward the nearest foe (when that is more than 2.6 blocks away), reaches 5 blocks and hits 40 percent harder. The combo is spent by the cast. (100 ticks is deliberate: the dispatcher's lock between two techniques is 40 ticks, a shorter window could never be used.)
  *Numbers:* raw 5 per cut (7 with the combo) times the mastery factor, 2 hits; against a player the second cut falls inside the hurt timer and adds little (section 11).
  *Tell:* none beyond the swing (7 raw at most).
  *Counter:* stay out of the 3.4 blocks, or step back as the lunge comes (the lunge only closes distance, it adds no hit).
  *Cost / cooldown:* 1.5 %, 70 ticks. With no foe in the fan it refuses and costs nothing.
* **Hemorrhage** (`key1` of the tree forms; the payoff for wounds).
  *What:* tears open every wound on the foe you face (within 4.6 blocks, 40 degrees, line of sight) at once. It needs a bleeding foe ("The foe is not bleeding", no cost).
  *Numbers:* raw `min(15, 2 + 2.6 x wounds)` times the mastery factor (4.6 on one wound, 15 on five or more; at the best mastery 19.5, inside the heavy cap of 20), single target, and the wounds close.
  *Tell:* blood gathers at the foe's middle for the 5 ticks before the burst, a bite sound; the burst only lands if the foe is still within 6.1 blocks.
  *Counter:* get out of reach in the quarter second, or do not be wounded.
  *Cost / cooldown:* 2 %, 100 ticks.
* **Pounce** (`key2` of the Bone and Rope Claws and of the Cursed Claw).
  *What:* leaps far forward (0.9 blocks a tick forward and 0.5 up: about 7 blocks, an estimate from the speeds), lands with the claws out and cuts the foes at the landing; no fall damage. The landing is detected every tick, from the 3rd tick on, and forced at the 15th.
  *Rope Hook (Rope Claws):* the leap homes in on the foe you face, up to 12 blocks away and within 25 degrees of the look; it aims at that foe and its speed is chosen to land next to it (between 0.5 and 1.4).
  *Numbers:* raw 6 times the mastery factor on up to 8 foes within 2.8 blocks of the landing, 2 wounds each, shove 0.3.
  *Tell:* a beast streak along the leap and a growl; the landing is not drawn on the ground (6 raw only).
  *Counter:* leave the landing point, which is where the streak ends.
  *Cost / cooldown:* 1.5 %, 100 ticks.
* **Shredding Rush** (`key3` of the Rope Claws and of the Cursed Claw).
  *What:* rushes the foe you face (within 6.5 blocks, 45 degrees) and rakes it four times in a flurry: lunge to within about 2 blocks (when farther than 2.6), then cuts 5, 8, 11 and 14 ticks after the cast, each only if the foe is alive and within 4.6 blocks ("If it gets away from you the flurry stops").
  *Numbers:* raw 3 per cut times the mastery factor, 4 cuts, a wound each, and each cut strips armour: 7, 14, 21, 28 percent (one modifier that each cut replaces) for 100 ticks (5 s) after the last cut.
  *Tell:* a beast streak at the lunge and the rising pitch of the cuts; the flurry is slow enough to see.
  *Counter:* move out of 4.6 blocks while it runs; a shield blocks single cuts.
  *Cost / cooldown:* 2 %, 140 ticks.
* **Predator's Instinct** (`key2` of the Dragon Head Claws; utility, no damage).
  *What:* the beast in the gem wakes for 8 seconds (160 ticks): the wielder gains Speed II and Haste II, and every foe within 20 blocks (at most 12) that has an open wound glows, so it can be found through walls and in the dark.
  *Numbers:* cost 2 %, cooldown 260 ticks (13 s). Not a mastery technique (it deals no damage).
  *Tell:* an aura ring on the wielder for 3 seconds and a growl. It is not an attack; the glow also shows bleeding PLAYERS through walls, as any glowing does.
* **Dragon Rend** (`key3` of the Dragon Head Claws; the ultimate).
  *What:* needs foes in a 12-block line in front of the wielder when it starts ("Nothing in the path of the Dragon Claws", no cost). A dotted line shows the path for 10 ticks while the wielder is held still (Slowness IV for 6 ticks); then the wielder tears through the line in one run (a dash of up to 1.5 blocks a tick) and rakes the foes found in the line 2, 6 and 10 ticks later.
  *Numbers:* raw 7 per pass times the mastery factor (9.1 at the best), 3 passes on up to 8 foes in a path 3.6 blocks wide, a wound at each pass. A foe that has left the line when the run starts is missed.
  *Tell:* the dotted crack along the whole line, a gathering of amber dust on the wielder and a low growl, 6 ticks before the run.
  *Counter:* leave the line in the 0.3 seconds it shows (the lightest weapon has the shortest warning of the four ultimates).
  *Cost / cooldown:* 6.5 %, 900 ticks (45 s).
* **Wahnsinn Claw** (`sneak` of the Cursed Claw only; the canon skill of the curse form).
  *What:* fires a beam of crimson light 14 blocks long (it stops at a wall) after a short warning. Foes still in the line (1.3 blocks either side, up to 6) are hurt, cursed with Tensura's Curse for 8 seconds and cut with 3 wounds. It eats at the wielder's mind: 25 corruption are paid at the cast, whether or not it hits.
  *Numbers:* raw 10 times the mastery factor (13 at the best), 8 ticks between the warning and the beam.
  *Tell:* the crimson ray in its telegraph voice for 10 ticks along the whole line, an eye-launch sound; the beam answers with a bright ray and a sonic boom.
  *Counter:* leave the line, or put a wall in it.
  *Cost / cooldown:* 3 %, 160 ticks, plus the corruption.

## 5. The passives

| id | form that gives it | what it does |
|---|---|---|
| `rending_blows` | every form | every melee blow opens a wound on the foe (2 on a full swing, i.e. an attack bar at 90 percent or more; 1 more with Madness) |
| `skill_mastery` | every form | the canon power-up: each damaging technique counts its uses and has five levels at 6, 16, 32, 56 and 90 uses; each level gives 6 percent more damage and 4 percent less cooldown (level 5: x1.3 damage, x0.8 cooldown). The cooldown can never fall under the floor of its kind. The uses are kept in the bond (`bond.extra("claw")`, one number per technique), a level-up says "<Technique> mastery <n>" |
| `scent_of_blood` | Bone, Rope and Dragon Head Claws | a melee blow on a foe with 3 or more open wounds cuts once more for 2.5 raw |
| `rope_hook` | Rope Claws | the Pounce homes in on the foe you face (see Pounce) |
| `dragon_fangs` | Dragon Head Claws | wounds stack to 7 instead of 5 and stay open 8 seconds instead of 6 |
| `madness` | Cursed Claw | every melee blow opens one wound more; it is also the mark of the form whose Wahnsinn Claw costs corruption |

## 6. Wounds (the bleed)

A wound is a count and a closing time kept on the foe's own saved data (`nusmp_cl_bn`, `nusmp_cl_bu`, `nusmp_cl_bc`), so nothing is left in a table when the foe unloads or dies.

* Up to 5 wounds (7 with Dragon Fangs); each new wound renews the closing time of all of them to 120 ticks (6 s; 160 ticks, 8 s, with Dragon Fangs). A stored closing time further ahead than any wound can last (a copied entity) counts as closed.
* The damage is a chain of delayed pulses, one every 20 ticks: raw 0.9 per open wound (0.9 on one, 4.5 on five, 6.3 on seven), a hit through the balance law without the shove of a blow, in the name of the wielder. Armour reduces it (the law has one damage kind), which is why Shredding Rush strips armour.
* The chain starts when the first wound opens, and ends when the foe dies, the wounds close, the wielder is gone, or after 380 ticks (19 s), whichever is first: the owner's rule is no repeating task beyond 20 seconds. A foe that is still bleeding when the chain ends needs a new wound to start another.
* Hemorrhage spends all the wounds of the foe it hits.
* The pulses are a status, not a row of the budget: each is small (0.9 to 6.3 raw; under the 3 raw floor of a technique while there are three wounds or fewer, and never near the cap of 12).

## 7. The curse series: one small form, and why

The brief allows one small curse form for a Seven Star weapon if a reason is given. The Claw has the **Cursed Claw** because the canon curse list names it, with the Wahnsinn Claw as
its equip bonus (S), and the Wahnsinn Claw is one of the three skills the wiki gives the Claw; without the form the weapon would lose a named skill.

* **How it is reached:** it has no requirements and is not taught by materials: the curse system teaches it when the wielder embraces the series (despair), like every curse form. Its mastery need is 150. `FormsCheck` for the Claw is built with `curseAllowed = true` (the Hammer's and the Whip's are not).
* **The sin: wrath.** The summaries do not give the Cursed Claw a sin. Wrath is my choice (Wahnsinn is German for madness, the claw's curse is a rage that cuts at friend and foe alike); the forms rules let a Seven Star curse form take any sin. It is a design choice the owner may change by editing the file.
* **What it costs the wielder:** 6 corruption a second while the form is worn (the curse system's engine, by the defaults of the curse builder's worktree: 1000 is an Overrun, the weapon holding its wielder for 10 to 20 seconds); armour -2; every technique cast in the form adds the curse system's own corruption (6 by default, three times that for an ultimate) through the dispatcher; and the Wahnsinn Claw adds 25 of its own on top at the cast. The extra 25 is deliberate (the form's signature costs a piece of the mind) and can be removed by the owner by setting the curse system's scale to 0, which scales every gain.
* **What it gives:** attack damage +4, attack speed +0.4, speed +0.04, the passive `madness` (one more wound at every blow), and the beam.
* **Kept small:** one form, one extra technique, no tree under it, no curse look (the look is the base look).

## 8. Engravings (the owner's rule 6, design.md section 16)

The exact line for `item/WeaponEngravings.java` (the lead integrates it):

```java
Map.entry("vassal_claw", List.of(e("swift", 2), e("severance", 2), e("energy_steal", 1))),   // the lightest, fastest weapon; its cuts open wounds; the gem feeds on what bleeds
```

* `swift` 2: the Claw is the lightest and fastest of the four striking weapons (attack speed up to +0.5 in the tree).
* `severance` 2: clean cutting is its whole manner, and its wounds are what the other techniques cash in; the same engraving the table gives the other cutting weapons.
* `energy_steal` 1: the beast in the gem drinks from what it wounds; the table gives it to the Bloodbound Katana for the same reason.

## 9. The code: the WHY of each class

All in `src/main/java/com/newuniverse/nusmp/heroweapon/weapon/`. The package `strike/` is shared by the four striking weapons (its files and their WHY are in the table of
`weapon_hammer.md`, section 9): `SkillSpec`, `Budget`, `FormsCheck`, `Strike`, `StrikeFx`, `StrikeSkill`, `StrikeSelfTest`.

| file (`claw/`) | why it exists |
|---|---|
| `ClawNumbers` | every number of the weapon in one pure table, judged by the budget at the top mastery in a unit test |
| `BleedMath` | the arithmetic of wounds (stack, renew, pulse size, burst, chain limit), pure |
| `ClawMastery` | the canon power-up as arithmetic: uses to level, damage and cooldown factors, pure |
| `ClawBleed` | the game side of wounds: state on the foe, the chain of delayed pulses in the wielder's name |
| `ClawSkills` | the eight techniques, with the mastery wrapper that counts uses and shortens cooldowns |
| `ClawLogic` | lists the skills and the melee hook (a wound at every blow, Scent of Blood); registers the self-test |

## 10. Config keys, commands, quest hooks, lang, saved data

* **Config keys:** none (the numbers are in `ClawNumbers`). The curse system's keys (`perTechnique`, `ultimateFactor`, `scale`) govern the corruption.
* **Commands:** none of its own.
* **Quest hooks:** none of its own (the dispatcher fires `hero.skill`).
* **Lang:** `hero.skill.claw.<id>.name` / `.desc` for the eight techniques and `hero.passive.claw.<id>` / `.desc` for the six passives, in the lang fragment `hva.lang.json` (the lead merges it into `en_us.json`). Messages to the wielder are plain strings.
* **Saved data:** on the foe `nusmp_cl_bn`, `nusmp_cl_bu`, `nusmp_cl_bc` (wounds); on the wielder `nusmp_cl_combo` (the tick of the last landed Air Strike Slash); in the bond `extra("claw")` (uses per technique); the attribute modifier id `nusmp:hero_claw_shred` (transient).

## 11. Tests, the self-test, judgement calls

Pure JUnit 5 tests, all passing in the local shim: `claw/ClawNumbersTest` (8: every row inside the budget at the best mastery, the check can fail, six techniques and one ultimate,
the ultimate warns before it runs, the combo arithmetic and the 100-tick window, the Hemorrhage cap, a bleed never outlives 20 s, the passive set), `claw/BleedMathTest`
(stacking, renewal, a closed wound is none, the pulse, the burst, the chain limit, a backwards clock), `claw/ClawMasteryTest` (the climb is monotone and bounded, a level-up is exactly a
threshold crossing); shared: `strike/BudgetTest`, `strike/FormsCheckTest`, `strike/StrikeFormsTest` (the shipped Claw file is checked against `ClawNumbers`). The mutation check (a rule
of `FormsCheck` weakened, the test failed, the original restored) is described in `weapon_hammer.md`, section 11. The forms file also passes the forms builder's own validator (5 forms, 0 errors, 0 warnings).

**Self-test `hero.weapon.claw`** (registered from `ClawLogic`'s constructor; NOT run): the shipped file through `FormsCheck`, the loaded forms, every skill registered with the right
flags and texts, then a stand-in player bonded to the weapon casts every skill of every form beside a dummy zombie without a mind, calls every hook, releases the bond.

Judgement calls:

* **Cooldowns are base cooldowns.** The dispatcher shortens a cooldown by the bond level (to 0.75 at level 100) and floors it at the config's 20 ticks; the mastery factor comes on top (x0.8), but `StrikeSkill` never takes it under the floor of the technique's kind (60 / 100 / 600) before the dispatcher's scaling. At bond level 100 and mastery 5 the Air Strike Slash can run every 36 ticks; the dispatcher's 40-tick lock between any two techniques is what bounds the spam. A decision for the lead if the owner wants the floors to hold after the scaling.
* **Corruption is counted twice on purpose** in the Cursed Claw (the curse system's per-technique gain plus the 25 of the Wahnsinn Claw); see section 7.
* **Rapid hits against players.** The hits of a flurry (Second Slash's second cut, the Shredding Rush, the Dragon Rend passes) keep a PLAYER's hurt timer, so a flurry cannot strip a player faster than the balance law's cap allows; against a mob the timer is reset and every cut lands. A wound is opened only when its hit landed.
* **Predator's Instinct makes bleeding players glow.** Vanilla glowing shows through walls.
* **The Dragon Head Claws drop the Pounce and the Shredding Rush** (five slots); the lower forms keep them.

## 12. What is NOT verified (honest list)

Nothing of the weapon has been run in the game. The code was type-checked against API stubs of Minecraft, NeoForge and Tensura (0 errors in my files; about 1,175 stub artefacts in
other files are ignored); the pure logic is covered by the unit tests above; the forms file by the forms builder's validator. Not verified:

* **Everything visual:** `WIND_SLASH`, `BEAST_FX1/2/3`, the blood effects (`BLOOD_DOWNPOUR`) and the crimson `DIVINE_RAY` as the Claw uses them, whether each honours the colour payload, and how the dust of a wound pulse reads.
* **The self-test** `hero.weapon.claw` (never run).
* **The Tensura effect id** `tensura:curse` (if it does not exist the curse of the beam is skipped silently; the wounds and the damage remain).
* **`bond.extra("claw")`:** it is the core's saved area; that the uses survive a relog, a death and a release as I assume is read from the core builder's unfinished source, not tested.
* **The curse system's own accounting.** `HeroCurse.addCorruption` and the dispatcher's `HeroCurse.onTechnique` come from the curse builder's worktree (still being written); the stub in my tree ignores both.
* **Feel:** the speed of the crescent, the width of the fan, the lock-on cone of Rope Hook, the 5-wound ceiling and the numbers of the pulse have never been tried against a real mob; the balance is judged on paper.
* **Foes that are not "foes":** the target filter is the relics' `Fx.foesAround` (24 nearest, never creative or spectator players, allies, pets or summons); a foe the filter rejects can never bleed.
