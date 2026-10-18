# Vassal Axe (`nusmp:vassal_axe`, `HeroKind.AXE`, class Seven Star, style SWING)

Builder `hva`. Code: `heroweapon/weapon/axe/**` (+ the shared helpers of the four striking weapons in `heroweapon/weapon/strike/**`), forms file
`data/nusmp/hero/forms/vassal_axe.json`, lang fragment `hva.lang.json`. This page is written in the order the art builder needs it: the look first.

## 1. Look (for the art builder `hart2`)

Canon, from search summaries of the fandom wiki: the Vassal Axe (Seven Star Axe) is a vassal of the Legendary Sword (with the Vassal Gauntlet); in the web novel
it passed from Filo to Midori (Green, a Filolial, whose name means green); its canon forms are the **Small Axe**, the **Pipe Axe**, the **Animal Needle Axe** (the
last two marked as uncertain by the wiki) and a **Curse Axe**; its canon skills include Great Quake, Great Tornado, Power Break, Mountain Break, Crescent Break,
Tornado Axe, Bedrock Breaker, Shine Axe Burst, Accelerator Smash and (curse) Guillotine. The wiki gives no picture. The model below is DESIGN.

* **Silhouette.** A broad single-bit battle axe: a heavy steel head with a long, slightly curved cutting edge (-x) that drops into a pronounced beard (the lower
  corner hooks down), a thick squared poll on the back (+x) with a short back spike, on a straight haft. A woodcutter's silhouette made noble: the blade is
  the largest shape and the edge is the first thing you see.
* **Size.** Class `battleaxe` of `assets/nusmp/weapon_classes.json` (1.6 to 2.0 blocks apparent; aim for 1.8), head at least 0.75 tall (aim 0.85) and 0.5 wide at
  the edge. The class `greataxe` (2.4 to 3.0) is too big for a weapon swung one-handed at speed.
* **Parts (8 to 14 named parts).** `butt_cap`, `grip_wrap`, `haft`, `haft_ring_low`, `haft_ring_high`, `socket_collar`, `head_blade` (the crescent-bearded blade),
  `edge_bevel` (the bright cutting edge as its own strip), `poll_block`, `back_spike`, `gem_bezel`, `gem`, `cheek_inlay` (an etched channel across the blade's cheek).
* **Palette.** Blade: steel `#9AA3B2`, edge bevel `#D5DCE8`, cheek inlay `#3F8F66` (a dark green line). Poll and spike: darker steel `#6E7686`. Collar, rings, bezel and
  butt cap: dark iron `#2F3238` with gold `#B8893A` trim. Haft: pale ash `#9C7A4E`, grip wrap black-brown leather `#2E2018`.
* **The gem.** One emerald-green cabochon (about 0.12 across) in an eight-sided bezel set into the blade's cheek near the socket (the eye of the axe). Green is the
  wielder's colour in the series (Midori means green). Glow colour `#35E07C`.
* **Glow skin.** The gem, a thin green line along the cheek inlay, and a hairline of pale light along the edge bevel (alpha under 30 percent). The seven-star
  mark (seven tiny dots) on the poll block. Nothing else glows.
* **Orientation.** Mesh -x is forward: the edge, the beard and the hooked corner face -x; the poll and the back spike face +x. The head hangs at the tip (high y).
  A correct axe looks like a woodcutter's: haft forward, blade hanging, edge down in third person and away from you in first person.
* **Icon.** Keep the 16 x 16 sprite as the inventory icon (an axe head with a green dot).
* **Curse form.** The Axe has one small curse form (Curse Axe). The vassal weapons have no curse mesh: the curse form keeps this look.

## 2. Canon: what is from a summary, what is from memory, what is design

The fandom wiki could not be opened from the build machine (a fetch of its pages fails), so every canon claim below comes from SEARCH SUMMARIES of the pages named, run on
2026-10-18. Tags: **S** = a search summary of that page, **M** = my memory of the series, **D** = my design for Minecraft. Nothing here was read on the page itself.

Pages (all on https://shield-hero.fandom.com/wiki/): `Vassal_Axe`, `Vassal_Weapons`, `Vassal_Weapons/Web_Novel`, `Heroes`, `Heroes_(Web_Novel)`, `Green_(Web_Novel)`,
`Takt_Alsaholn_Faubrey/Powers_and_Abilities`, `Curse_Series`, `Curse_Series/Weapon_List`.

| fact | tag | what the mod does with it |
|---|---|---|
| The Vassal Axe (Seven Star Axe) comes from the Sword World and is a vassal of the Legendary Sword, with the Vassal Gauntlet; the wiki does not say what its role in that trio is | S | the Axe is the cleaving, heavy-cutting weapon (the Sword's partner) |
| In the web novel it was first given to Filo, who held both axe and claw and gave the axe to Green (Midori, "the Axe Hero"); Takt collected it, and after he fell it was recaptured by the vanguards of the waves; Mald used it with the curse series and no side effects. Takt was not shown using any axe skill | S | not used |
| Power-up method: **muscle enhancement** ("body strengthening"), hypothesised to be a physical enhancement; the details are unknown | S | **Brawn**, a build-up that strengthens the body (section 6): an invention on a thin canon |
| Skills in the wiki's table: Great Quake, Great Tornado, Jason Murder, Power Break, Shine Axe Burst, Mountain Break, Crescent Break, Tornado Axe, Bedrock Breaker, Accelerator Smash (listed under Green, details to be added); curse-series Cursed Penetration Attack and Guillotine. Most have no unlocking form and, except Jason Murder, no description | S | seven of these names are techniques here (Crescent Break, Power Break, Tornado Axe, Mountain Break, Accelerator Smash, Shine Axe Burst, Guillotine) and the Cursed Penetration; what each DOES is design |
| Not built: Great Quake and Great Tornado (upgrades of the Quake and Tornado names; the Hammer owns the quake), Jason Murder (summons a hockey mask and swings wildly; it needs the Gauntlet's power-up to reach its full form, and it would be a second ultimate), Bedrock Breaker (it would break blocks, which no technique may do) | S + D | |
| Forms in the wiki: Small Axe, Pipe Axe (marked uncertain; equip bonus Chain Bind), Animal Needle Axe (uncertain; Chain Needle), and the **Curse Axe** (Curse Series; equip bonus the Cursed Penetration Attack and Guillotine; special effect Automatic Self-repair) | S | Small Axe and Pipe Axe keep their names; Chain Bind and Chain Needle are not built; Timber Axe and Shine Axe are design; the Curse Axe is the one curse form (section 7) |
| The Curse Axe's penetration attack creates a wound on the opponent even if they block; its Guillotine steals SP upon breaching defence; Automatic Self-repair is listed (rated Very High on the Sword of Greed and Gluttony III) but its page is empty | S | the wound that follows the Cursed Penetration (section 4); the SP steal is not built (the mod has no SP); the self-repair is small by the healing law (section 7) |
| The Guillotine appears in the Greed and Gluttony section of the curse list, in a parenthetical next to the Sword of Greed II and the Cursed Axe. The Cursed Axe has no section of its own | S, from one search | the sin of the Curse Axe is **greed** (section 7) |
| Executes on wounded foes, bounded; a throwing axe; the forms from wood and iron | D | the owner's brief |

## 3. The forms tree

Five forms: a line `base` -> `timber_axe` -> `pipe_axe` -> `shine_axe` (4 ordinary forms; the brief asks for 3 to 5) and one small curse form `curse_axe` (section 7). Small Axe and
Pipe Axe are canon names; the Timber Axe (logs, flint: the woodcutter) and the Shine Axe (the light of the Shine Axe Burst) are design. The brief asks for forms from wood and iron:
logs for tier 1, iron for tier 2. Every form has the passive `muscle_enhancement` (Brawn).

The table is generated from `data/nusmp/hero/forms/vassal_axe.json`:

| form (id) | tier | parents | requirements | mastery | stats | ability bonus | skills (slot: id) | passives |
|---|---|---|---|---|---|---|---|---|
| Small Axe (`base`) | 0 | - | none | 0 | movement_speed +0.01 | - | use: `axe.crescent_break`, sneak: `axe.power_break`, key1: `axe.accelerator_smash` | `muscle_enhancement` |
| Timber Axe (`timber_axe`) | 1 | `base` | 48 x `#minecraft:logs`, 16 x `minecraft:flint` | 100 | attack_damage +1.5, movement_speed +0.02 | movement_speed +0.005 | use: `axe.crescent_break`, sneak: `axe.power_break`, key1: `axe.accelerator_smash`, key2: `axe.tornado_axe` | `muscle_enhancement`, `cleaving` |
| Pipe Axe (`pipe_axe`) | 2 | `timber_axe` | 56 x `minecraft:iron_ingot`, 6 x `minecraft:chain` | 180 | attack_damage +3, attack_speed -0.1, armor +2, knockback_resistance +0.2 | armor +0.5, knockback_resistance +0.05 | use: `axe.crescent_break`, sneak: `axe.mountain_break`, key1: `axe.axe_throw`, key2: `axe.power_break`, key3: `axe.guillotine` | `muscle_enhancement`, `cleaving`, `headsman` |
| Shine Axe (`shine_axe`) | 3 | `pipe_axe` | 48 x `minecraft:emerald`, 24 x `minecraft:diamond`, 8 x `minecraft:gold_block` | 300 | attack_damage +4.5, armor +3, knockback_resistance +0.3, max_health +4, movement_speed +0.03 | attack_damage +1, max_health +1 | use: `axe.crescent_break`, sneak: `axe.mountain_break`, key1: `axe.guillotine`, key2: `axe.tornado_axe`, key3: `axe.shine_axe_burst` | `muscle_enhancement`, `cleaving`, `headsman`, `radiance` |
| Curse Axe (`curse_axe`) - CURSE, sin greed, 5 corruption/s | 1 | `base` | none (the curse system teaches it) | 150 | attack_damage +5, movement_speed +0.03, armor -2 | - | use: `axe.crescent_break`, sneak: `axe.cursed_penetration`, key1: `axe.guillotine`, key2: `axe.mountain_break`, key3: `axe.tornado_axe` | `muscle_enhancement`, `headsman`, `self_repair` |

Fairness of the climb: tier 1 is 64 common items (any logs and flint), tier 2 is 62 iron ingots and chains, tier 3 is 48 emeralds, 24 diamonds and 8 gold blocks (a real task). The
log requirement is a TAG (`#minecraft:logs`), the forms system resolves tags as well; every vanilla id and tag was checked against the item and tag lists of Minecraft; the CI
self-test `hero.forms` resolves them again and fails on a wrong id.

## 4. The techniques

Nine techniques (eight for the tree, one for the curse form), one ultimate. The budget rows (the pure table `AxeNumbers`, judged by `Budget` in `AxeNumbersTest`; "raw" is the
highest a hit can reach before Brawn, the bond-level factor 1.0 to 2.2 and the balance law; the cooldown is the BASE cooldown, see section 11):

| skill id | kind | raw per hit | hits | cooldown | cost (of max magicules) | targets |
|---|---|---|---|---|---|---|
| `axe.crescent_break` | damage | 8 | 1 | 70 ticks (3.5 s) | 1.5 % | 12 |
| `axe.power_break` | heavy | 10 | 1 | 110 ticks (5.5 s) | 2 % | 1 |
| `axe.tornado_axe` | damage | 3.5 | 4 | 140 ticks (7 s) | 2.5 % | 12 |
| `axe.mountain_break` | heavy | 12 | 1 | 120 ticks (6 s) | 2.5 % | 12 |
| `axe.accelerator_smash` | damage | 9 | 2 | 120 ticks (6 s) | 2 % | 12 |
| `axe.axe_throw` | damage | 7 | 2 | 100 ticks (5 s) | 2 % | 4 |
| `axe.guillotine` | heavy | 19.8 | 1 | 110 ticks (5.5 s) | 2 % | 1 |
| `axe.shine_axe_burst` | ultimate | 31.6 | 1 | 900 ticks (45 s) | 7 % | 12 |
| `axe.cursed_penetration` | heavy | 11 | 2 | 120 ticks (6 s) | 3 % | 1 |

Brawn adds up to 16 percent to a technique's raw damage, but EVERY hit is clamped to the cap of its kind (12 ordinary, 20 heavy, 32 ultimate) in the same arithmetic
(`ExecuteMath.raw`), so the budget holds whatever has been stacked; the Guillotine row is its best hit, the execution (9 x 2.2 = 19.8). The heavy blows show a tell and land a few
ticks later (`Strike.later` re-checks that the wielder is alive, in the same world and still bonded); a cast that finds no target returns false and costs nothing. Each technique
that lands adds one Brawn (section 6).

* **Crescent Break** (`use` of every form; canon).
  *What:* sweeps the axe in a half circle in front of the wielder: 4.6 blocks out, 90 degrees either side of the facing. Every foe inside is cut and thrown back.
  *Numbers:* raw 8 times Brawn, up to 12 foes, shove 0.5; the blow lands 6 ticks after the cast.
  *Tell:* a green arc drawn on the ground at the cast, showing the half circle; a sweep sound; a wind effect when it lands.
  *Counter:* be outside the arc (behind the wielder or past 4.6 blocks) before the 6 ticks end.
  *Cost / cooldown:* 1.5 %, 70 ticks.
* **Power Break** (`sneak` of the Small and Timber Axes, `key2` of the Pipe Axe; canon).
  *What:* a short overhead chop on the foe you face (within 4.4 blocks, 40 degrees, line of sight) that breaks its strength. A player who is blocking has the shield knocked down FIRST (the shield goes on cooldown), so the chop itself is not swallowed by it.
  *Numbers:* raw 10 times Brawn, single target; 5 seconds of Weakness II and 3 seconds of Slowness II; it lands 7 ticks after the cast if the foe is within 5.9 blocks; the wielder is slowed (Slowness II) for those 7 ticks.
  *Tell:* a ring on the foe's feet and a gathering of green dust at its middle; an anvil sound.
  *Counter:* get more than 5.9 blocks away from the wielder within 7 ticks (0.35 s).
  *Cost / cooldown:* 2 %, 110 ticks.
* **Tornado Axe** (`key2` of the Timber and Shine Axes, `key3` of the Curse Axe; canon).
  *What:* spins with the axe held out: a green tornado grows round the wielder and four times in a row everything within 3.8 blocks is cut and shoved, the last time hardest. The wielder is slowed (Slowness II) while it turns.
  *Numbers:* raw 3.5 times Brawn per pulse, 4 pulses at about 5, 11, 17 and 23 ticks after the cast (a 4-tick start, then a pulse every 6 ticks, 24 ticks in all), up to 12 foes, shove 0.3 (0.9 on the last). The hits are rapid hits (against a player they fall inside the hurt timer, section 11).
  *Tell:* the tornado itself, from the cast, a sweep sound at every pulse.
  *Counter:* step out of the 3.8-block radius; the spin lasts 1.2 s and the wielder cannot chase.
  *Cost / cooldown:* 2.5 %, 140 ticks.
* **Mountain Break** (`sneak` of the Pipe and Shine Axes, `key2` of the Curse Axe; canon).
  *What:* brings the axe down and cuts a line 12 blocks long straight ahead (it stops at a wall; needs 3 blocks of room, else "There is no room for the cut" and no cost). Everything along the line at chest height, on the ground or in the air, is cut and shoved.
  *Numbers:* raw 12 times Brawn, up to 12 foes, within 1.1 blocks (plus half the foe's width) of the line, shove 0.5 and a small lift; lands 10 ticks after the cast; the wielder is slowed (Slowness III) for those 10 ticks.
  *Tell:* a dotted crack along the whole line for 14 ticks, a green line of dust, an anvil sound; a bright crack runs out when it lands.
  *Counter:* leave the 2.2-block-wide line, up or sideways.
  *Cost / cooldown:* 2.5 %, 120 ticks.
* **Accelerator Smash** (`key1` of the Small and Timber Axes; canon, web-novel name).
  *What:* charges 6 blocks forward with the shoulder, hitting what is in the way, then smashes the ground where it stops.
  *Numbers:* the shoulder charge is raw 5 times Brawn on the foes in the path (1.3 blocks either side, up to 12), 2 ticks after the cast, shove 0.6; the smash is raw 9 times Brawn on everything within 3 blocks of where the wielder stopped, 7 ticks after the cast, shove 0.7 and lift 0.25.
  *Tell:* a ring of the smash's radius is drawn at the end of the charge at the cast, and the charge itself is a gale streak; a smash sound and a ring when it lands.
  *Counter:* leave the line, and then the ring, in the 0.35 seconds the charge takes.
  *Cost / cooldown:* 2 %, 120 ticks.
* **Axe Throw** (`key1` of the Pipe Axe; the owner's throwing axe).
  *What:* hurls the axe 14 blocks along the look; it cuts up to 4 foes on the way out, turns at the end and cuts again on the way back to the wielder's hand. No entity is spawned: each foe in the line is scheduled to be cut when the axe reaches it (1.3 blocks a tick), and a foe that has stepped out of the line by then (more than about 1.6 blocks plus half its width) is missed.
  *Numbers:* raw 7 times Brawn out, 5 times Brawn back; Brawn gains one once per throw.
  *Tell:* the slash wave flying out and back, a sweep sound at each throw; none above 8 raw.
  *Counter:* sidestep the line; the axe takes up to 11 ticks to reach the far end.
  *Cost / cooldown:* 2 %, 100 ticks.
* **Guillotine** (`key3` of the Pipe Axe, `key1` of the Shine and Curse Axes; canon, from the curse list).
  *What:* a falling cut on the foe you face (within 4.6 blocks, 40 degrees, line of sight), with a red column over it as the warning. The execution is a bonus ON THE HIT: a foe at or under a quarter of its health (a player at or under a fifth) takes 2.2 times the damage (a player 1.4 times), never a boss. The result is clamped to the heavy cap, so the balance law and the caps still apply; whether the foe then dies is up to the arithmetic of the hit. Eligibility is judged again at the moment of the blow.
  *Numbers:* raw 9 times Brawn (19.8 executing, at most 20 with Brawn), single target; it lands 8 ticks after the cast if the foe is within 6.1 blocks; "Executed." is said on an execution.
  *Tell:* a red column and a ring over the foe, an anvil sound.
  *Counter:* leave 6.1 blocks in 8 ticks, or stay above the threshold.
  *Cost / cooldown:* 2 %, 110 ticks.
* **Shine Axe Burst** (`key3` of the Shine Axe; canon, the ultimate).
  *What:* needs 5 Brawn or more ("Shine Axe Burst needs 5 Brawn; you have n", no cost) and spends all of it. The axe blazes: a pillar of green light rises over the wielder, who is held still (Slowness IV) for 16 ticks, then everything within 8 blocks is cut.
  *Numbers:* raw 14 plus 2.2 for each Brawn spent (25 at five, 31.6 at eight; the ceiling of the ultimate is 32), up to 12 foes, shove 1.0 and lift 0.35. The Brawn is spent at the cast, so a foe that leaves in time costs the wielder the stacks.
  *Tell:* a pillar of divine light in its telegraph voice for 20 ticks, a ring of the full radius and a gathering of dust on the wielder, a bell; when it lands a second pillar, a flare, a ring and thunder.
  *Counter:* leave the 8-block ring in the 0.8 seconds it gives, or the 16 ticks of the wind-up while the wielder cannot move.
  *Cost / cooldown:* 7 %, 900 ticks (45 s).
* **Cursed Penetration** (`sneak` of the Curse Axe only; canon, the curse form's technique).
  *What:* the Curse Axe drives through the foe you face (within 4.8 blocks). If the foe is a player who is blocking, the shield is knocked down first. A heavy cut lands 8 ticks after the cast (if the foe is within 6.3 blocks), and 12 ticks after that the wound opens: a second cut.
  *Numbers:* raw 11 times Brawn, then raw 5 times Brawn for the wound (no shove), single target; 15 corruption are paid at the cast. Whether a shield that is raised again after the first cut would stop the wound is the vanilla shield logic (section 12); the canon text says the wound comes even if the opponent blocks, and here a foe that was blocking at the first cut has its shield on cooldown for the second.
  *Tell:* a gold ring and a gathering of gold dust (the colour of greed) on the foe for 8 ticks, an anvil sound; an impact and a bite sound when it lands.
  *Counter:* leave the reach in the 8 ticks.
  *Cost / cooldown:* 3 %, 120 ticks, plus the corruption.

## 5. The passives

| id | form that gives it | what it does |
|---|---|---|
| `muscle_enhancement` | every form | the canon power-up as **Brawn** (section 6) |
| `cleaving` | Timber, Pipe and Shine Axes | a full swing (attack bar at 90 percent or more) also cuts up to 3 foes within 2.2 blocks of the foe struck, raw 3 each |
| `headsman` | Pipe, Shine and Curse Axes | a full swing on a foe an execution would take (a mob at a quarter of its health, a player at a fifth, never a boss) cuts once more, raw 3.5 |
| `radiance` | Shine Axe | Brawn fades at half the speed (a stack every 4 seconds instead of every 2), and sparks of green light show round the wielder from 6 Brawn on |
| `self_repair` | Curse Axe | every 40 ticks the wielder mends 4 percent of their maximum health through `HeroCtx.heal`; the healing law cuts a heal to 40 percent when the target was healed in the last 20 seconds, so in a steady state it is 1.6 percent every 2 seconds, about 0.8 percent of the maximum health a second |

## 6. Brawn (the canon power-up)

Brawn is a stack of up to 8 that the wielder earns by fighting with the axe, and spends on the Shine Axe Burst. It is two numbers in the wielder's own saved data (`nusmp_ax_bn`
the count, `nusmp_ax_bt` the tick of the last gain), read LAZILY from the clock, so the fading needs no timer and nothing stays behind.

* **Gains:** +1 for a full swing that hits, +1 for each technique that lands (once per cast), +2 for a kill with the weapon summoned.
* **Held** for 120 ticks (6 s) after the last gain, then **fades** one stack every 40 ticks (every 80 with Radiance).
* **What each stack gives:** melee damage +3 percent, technique raw damage +2 percent (always clamped to the cap of the technique's kind), knockback resistance +0.04, speed +1 percent. The first, third and fourth are attribute modifiers that the weapon's tick refreshes every 5 ticks with a 40-tick timer (the mod's `TimedModifiers`), so putting the axe away, dying or leaving removes them within two seconds.
* **The sink:** Shine Axe Burst needs 5 and spends all (14 + 2.2 per stack).
* The wielder is told when the fifth stack arrives ("Brawn 5: Shine Axe Burst is ready.") and at the top ("Brawn at its peak.").

## 7. The curse series: one small form, and why

The brief allows one small curse form for a Seven Star weapon if a reason is given. The Axe has the **Curse Axe** because the canon curse list names it, with the Cursed Penetration
Attack, the Guillotine and Automatic Self-repair (S), and because the Guillotine is the execution the owner asked of the Axe ("executes on wounded foes (bounded)").

* **How it is reached:** no requirements; the curse system teaches it when the wielder embraces the series (despair). Mastery need 150. `FormsCheck` for the Axe is built with `curseAllowed = true`.
* **The sin: greed.** From one search summary: the Guillotine is listed in the Greed and Gluttony section next to the Sword of Greed II and the Cursed Axe (section 2). It is the best canon basis I found, not a certainty; the forms rules let a Seven Star curse form take any sin, and the owner may change it by editing the file.
* **What it costs the wielder:** 5 corruption a second while the form is worn (1000 is an Overrun; the curse builder's current default is 10 to 20 seconds of the weapon holding its wielder); armour -2; every technique cast in the form adds the curse system's own corruption (6 by default, three times that for an ultimate) through the dispatcher; and the Cursed Penetration adds 15 of its own at the cast. Greed, in the curse system, also makes techniques cost more (its own rule, not mine).
* **What it gives:** attack damage +5 (the strongest blow in the set), the Headsman, the self-repair, the Cursed Penetration and the Guillotine on its keys. It has no Cleaving and no Radiance.
* **Kept small:** one form, one extra technique, no tree under it, no curse look (the look is the base look).

## 8. Engravings (the owner's rule 6, design.md section 16)

The exact line for `item/WeaponEngravings.java` (the lead integrates it):

```java
Map.entry("vassal_axe", List.of(e("severance", 2), e("crushing", 2), e("sturdy", 1))),   // cleaving arcs, heavy chops that break strength and guard, a body hardened by Brawn
```

* `severance` 2: its manner is the wide, clean cut (Crescent Break, Mountain Break, the Guillotine); the table gives it to the other cleavers (the Severing Greatsword, the Butcher Cleaver).
* `crushing` 2: Power Break and the Cursed Penetration break a foe's strength and a raised guard; the table gives `crushing` to the Doom Hammer and the Thundercleave for the same reason.
* `sturdy` 1: muscle enhancement, a body that takes more and is harder to shove (the Brawn gives knockback resistance).

## 9. The code: the WHY of each class

All in `src/main/java/com/newuniverse/nusmp/heroweapon/weapon/`. The package `strike/` is shared by the four striking weapons (its files and their WHY are in the table of
`weapon_hammer.md`, section 9): `SkillSpec`, `Budget`, `FormsCheck`, `Strike`, `StrikeFx`, `StrikeSkill`, `StrikeSelfTest`.

| file (`axe/`) | why it exists |
|---|---|
| `AxeNumbers` | every number of the weapon in one pure table, judged by the budget in a unit test |
| `BrawnMath` | the arithmetic of Brawn (cap, hold, fade, what a stack gives, the Shine Axe's damage), pure, lazily read from the clock |
| `ExecuteMath` | the bounded execution as arithmetic: a bonus on the hit, clamped to the cap, never on a boss, pure |
| `AxeBrawn` | the game side of Brawn: two numbers on the wielder, gains, the spend, and the timed attribute modifiers |
| `AxeSkills` | the nine techniques |
| `AxeLogic` | lists the skills and the hooks: Brawn and Self-repair on the tick, a full swing's Cleaving and Headsman, Brawn on a kill; registers the self-test |

## 10. Config keys, commands, quest hooks, lang, saved data

* **Config keys:** none (the numbers are in `AxeNumbers`). The curse system's keys govern the corruption.
* **Commands:** none of its own.
* **Quest hooks:** none of its own (the dispatcher fires `hero.skill`).
* **Lang:** `hero.skill.axe.<id>.name` / `.desc` for the nine techniques and `hero.passive.axe.<id>` / `.desc` for the five passives, in the lang fragment `hva.lang.json` (the lead merges it into `en_us.json`). Messages to the wielder are plain strings.
* **Saved data:** on the wielder `nusmp_ax_bn`, `nusmp_ax_bt` (Brawn); the attribute modifier ids `nusmp:hero_axe_brawn_melee`, `nusmp:hero_axe_brawn_knock`, `nusmp:hero_axe_brawn_speed` (transient).

## 11. Tests, the self-test, judgement calls

Pure JUnit 5 tests, all passing in the local shim: `axe/AxeNumbersTest` (9: every row inside the budget, the check can fail, six techniques and one ultimate, the ultimate row is
exactly the best burst, a stacked hit never passes the cap, every blow with a tell has a delay, the tornado's four pulses, the curse form's corruption and wound, the self-repair is
small), `axe/BrawnMathTest` (cap, hold, fade, what a stack gives), `axe/ExecuteMathTest` (the thresholds, never a boss, clamped); shared: `strike/BudgetTest`,
`strike/FormsCheckTest`, `strike/StrikeFormsTest` (the shipped Axe file is checked against `AxeNumbers`). The mutation check is described in `weapon_hammer.md`, section 11. The forms
file also passes the forms builder's own validator (5 forms, 0 errors, 0 warnings).

**Self-test `hero.weapon.axe`** (registered from `AxeLogic`'s constructor; NOT run): the shipped file through `FormsCheck`, the loaded forms, every skill registered with the right
flags and texts, then a stand-in player bonded to the weapon casts every skill of every form beside a dummy zombie without a mind, calls every hook, releases the bond.

Judgement calls:

* **Cooldowns are base cooldowns.** The dispatcher shortens a cooldown by the bond level (to 0.75 at level 100) and floors it at the config's 20 ticks; the budget floors hold for the rows, not after that scaling. The dispatcher's 40-tick lock between any two techniques bounds the spam. A decision for the lead if the owner wants the floors to hold after the scaling.
* **Corruption is counted twice on purpose** in the Curse Axe (the curse system's per-technique gain plus the 15 of the Cursed Penetration).
* **Rapid hits against players:** the Tornado's pulses, the Accelerator Smash's charge, the Cleaving and Headsman cuts keep a PLAYER's hurt timer, so a flurry cannot strip a player faster than the balance law's cap allows; against a mob every hit lands.
* **The execution is bounded three ways:** only under a quarter of the health (a fifth for a player), never a boss, and clamped to the heavy cap 20 like any hit.
* **Power Break and the Cursed Penetration knock a shield down before they hit** (the vanilla way an axe beats a shield, but with a tell and a cooldown).
* **The Shine Axe Burst spends Brawn at the cast.** A foe that leaves the ring in time costs the wielder their stacks.

## 12. What is NOT verified (honest list)

Nothing of the weapon has been run in the game. The code was type-checked against API stubs of Minecraft, NeoForge and Tensura (0 errors in my files; about 1,175 stub artefacts in
other files are ignored); the pure logic is covered by the unit tests above; the forms file by the forms builder's validator. Not verified:

* **Everything visual:** `WIND_EMPEROR`, `WIND_TORNADO`, `WIND_GALE`, `SLASH_WAVE`, `DARK_IAI`, `LIGHT_FLARE`, the divine pillar and the stone telegraphs as the Axe uses them, whether each honours the green payload `#35E07C`, and whether the dust arc of Crescent Break reads as a half circle.
* **The self-test** `hero.weapon.axe` (never run).
* **`Player.disableShield()`** is public in the API dump I read; the type-check passes, but it has not run. Whether `LivingEntity.hurt` returns true for a hit that a raised shield swallows (so the order matters: the shield is knocked down BEFORE the hit for that reason) is vanilla behaviour I did not test; and whether a player who raises a shield again between the first cut and the wound of the Cursed Penetration blocks the wound is the vanilla shield logic.
* **The attribute modifiers of Brawn** use `TimedModifiers` (the mod's helper, already used for armour shredding); that a refresh every 5 ticks with a 40-tick timer never leaves a gap is read from its source, not run. That the dispatcher calls `onTick` every 5 ticks while the weapon is summoned is the contract's statement (design.md section 5).
* **Kills of the technique itself:** the `onKill` hook is told of a kill by any means (the core's source), and the Axe gives 2 Brawn only while the weapon is summoned; I read this from the core builder's unfinished source.
* **Feel and balance:** the numbers are judged on paper by the budget; the speed Brawn builds at, the size of the arcs and the time to kill are not tried.
* **The Curse Axe's sin** (greed) rests on one search summary (section 7).
