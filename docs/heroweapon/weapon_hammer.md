# Vassal Hammer (`nusmp:vassal_hammer`, `HeroKind.HAMMER`, class Seven Star, style SWING)

Builder `hva`. Code: `heroweapon/weapon/hammer/**` (+ the shared helpers of the four striking weapons in `heroweapon/weapon/strike/**`), forms file
`data/nusmp/hero/forms/vassal_hammer.json`, lang fragment `hva.lang.json`. This page is written in the order the art builder needs it: the look first.

## 1. Look (for the art builder `hart2`)

Canon, from search summaries of the fandom wiki (the page itself could not be opened from the build machine): the Vassal Hammer is a Seven Star Weapon of
Raphtalia's world, a vassal of the Legendary Shield; its wielder is meant to support the Shield Hero, forge equipment and keep him from straying, because a
shield is weak against impact. The wiki lists only one form, the **Small Hammer**, with no picture described. So the model below is DESIGN, not canon.

* **Silhouette.** A big two-handed war hammer that reads at arm's length: a square block head on a straight haft, slightly flared at the faces, a short
  collar under the head and a flat pommel cap. Nothing curved, nothing thin: it is the heaviest-looking of the four striking weapons.
* **Size.** Class `hammer` of `assets/nusmp/weapon_classes.json` (1.3 to 1.8 blocks apparent length; aim for 1.6). The head is about 0.55 long across
  the swing and 0.34 square on the face (the class asks for a head of at least 0.5); the haft carries the rest. Grip share 0.25 of the length from the butt.
* **Parts (8 to 14 named parts).** `pommel_cap`, `grip_wrap` (leather, spiral), `haft`, `haft_band_low`, `haft_band_high`, `neck_collar`, `head_core`,
  `face_plate_front` (the striking face, -x), `face_plate_back` (the blunt peen, +x), `head_rim_band_a`, `head_rim_band_b`, `gem_bezel`, `gem`, four `rivets`.
* **Palette (base look only; the Seven Star weapons have no curse look).** Head: forged dark iron `#4A4F5C`, bevels and face plates `#7A8090`. Bands, collar,
  bezel and pommel cap: worn brass `#B8893A`. Haft: dark walnut `#4B3220`; grip wrap: black-brown leather `#2E2018`. Rivets `#C9CED8`.
* **The gem (the series' central jewel).** One large amber cabochon, about 0.13 across, in an eight-sided brass bezel at the centre of the head's flat side
  plate, so it shows from the side in the hand and in the icon; it reads as a forge-fire coal. Glow colour `#FF9A2E`.
* **Glow skin.** The gem, and only faintly (alpha under 40 percent) a thin line along the rim of each face plate and the engraved seven-star mark
  (seven small dots in the pattern of the Plough, on the striking face) in the same amber. Nothing else glows.
* **Orientation.** Mesh -x is forward: the striking face is -x, the peen +x; the head sits at the tip (high y), the pommel at the butt. Tip up and forward on
  the icon diagonal, as every baked weapon (see `docs/weapon_scale_orientation.md`).
* **Icon.** Keep the 16 x 16 sprite as the inventory icon (a hammer head with an amber dot), as the owner asks (rule 7 of the owner's list).
* **Do not.** No curse look (the forms file has no curse form; the Hammer carries none). No spikes on the face (this is the blunt, armour-breaking weapon).

## 2. Canon: what is from a summary, what is from memory, what is design

The fandom wiki could not be opened from the build machine (a fetch of its pages fails), so every canon claim below comes from SEARCH SUMMARIES of the pages named, run on
2026-10-18. Tags: **S** = a search summary of that page, **M** = my memory of the series, **D** = my design for Minecraft. Nothing here was read on the page itself.

Pages (all on https://shield-hero.fandom.com/wiki/): `Vassal_Hammer`, `Vassal_Weapons`, `Vassal_Weapons/Web_Novel`, `Heroes`, `Heroes_(Web_Novel)`,
`Raphtalia/Powers_and_Abilities`, `Raph-chan_II`, `Shildina/Powers_and_Abilities`, `Natalia`, `Takt_Alsaholn_Faubrey/Powers_and_Abilities`, `Curse_Series`,
`Curse_Series/Weapon_List`, `Legendary_Weapons/Power-up_Methods`.

| fact | tag | what the mod does with it |
|---|---|---|
| The Vassal Hammer is a Seven Star Weapon of Raphtalia's world, a vassal of the Legendary Shield (with the Vassal Claw) | S | the class is Seven Star, style SWING |
| Its wielder is meant to support and guide the Shield Hero, forge equipment and give advice, and to keep him from straying, because a shield is weak against impact | S | the whole shape of the weapon: a blunt, armour-breaking weapon plus one support technique (Forge Aegis) |
| The Vassal Claw page says the Shield is strong against slashing and weak against impact | S | the Hammer is "the impact weapon": poise, break, armour crack |
| Power-up method: **Weapon Form Synthesis**. It copies the most characteristic special effect of one form onto another; the source form's enchantment and rarity reset; a weapon has a limited number of uses as synthesis material; the 0, curse and bless series cannot be fused | S | the passive `synthesis` and the table of signature passives ([HammerSynthesis](#9-the-code-the-why-of-each-class)); and the reason the Hammer has no curse form (section 7) |
| The only form the wiki lists is the **Small Hammer**; no skills and no equip bonuses are listed; Takt held it but was not shown using any skill | S | the base form is called Small Hammer; EVERY other form and EVERY technique is design |
| The current holder is given differently by page: Raphtalia (web novel), Shildina (Heroes), Natalia (light novel vol. 22) | S, conflicting | not used |
| The names of the seven techniques, the poise rule and the forms Stone, Anvil and Synthesis Hammer | D | the owner's brief: heavy blunt, slams, shockwaves, stagger, armour breaking, slow and readable; forms from ores and stone |

## 3. The forms tree

Four forms in one line: `base` -> `stone_hammer` -> `anvil_hammer` -> `synthesis_hammer`. The brief asks for 3 to 5 ordinary forms for a Seven Star weapon and no curse series
unless argued (section 7). The names Stone, Anvil and Synthesis are design: the first two name the material the head is made from (ores and stone, as the brief says), the
third is the canon power-up. Mastery needed is 100, 180 and 300 (the forms system accepts 40 to 400). Every form keeps `use` on the Overhead Smash, so the hammer always
feels like the hammer; a higher form adds slots and swaps the other two.

The table is generated from `data/nusmp/hero/forms/vassal_hammer.json` (stat keys are the forms system's closed set; `abilityBonus` is the extra per bond level):

| form (id) | tier | parents | requirements | mastery | stats | ability bonus | skills (slot: id) | passives |
|---|---|---|---|---|---|---|---|---|
| Small Hammer (`base`) | 0 | - | none | 0 | armor +1, knockback_resistance +0.1 | - | use: `hammer.overhead_smash`, sneak: `hammer.shockwave`, key1: `hammer.armor_crack` | `heavy_blows` |
| Stone Hammer (`stone_hammer`) | 1 | `base` | 48 x `minecraft:cobblestone`, 16 x `minecraft:coal` | 100 | attack_damage +1.5, armor +1, knockback_resistance +0.2 | knockback_resistance +0.05 | use: `hammer.overhead_smash`, sneak: `hammer.shockwave`, key1: `hammer.armor_crack`, key2: `hammer.quake_stomp` | `heavy_blows`, `rubble` |
| Anvil Hammer (`anvil_hammer`) | 2 | `stone_hammer` | 64 x `minecraft:iron_ingot`, 24 x `minecraft:gold_ingot`, 8 x `minecraft:blaze_rod` | 180 | attack_damage +2, armor +3, armor_toughness +1, knockback_resistance +0.35, max_health +4 | armor +0.5, max_health +1 | use: `hammer.overhead_smash`, sneak: `hammer.skyward_smash`, key1: `hammer.armor_crack`, key2: `hammer.quake_stomp`, key3: `hammer.forge_aegis` | `heavy_blows`, `anvil_stance` |
| Synthesis Hammer (`synthesis_hammer`) | 3 | `anvil_hammer` | 32 x `minecraft:diamond`, 48 x `minecraft:obsidian`, 8 x `minecraft:netherite_scrap` | 300 | attack_damage +4, attack_speed -0.2, armor +4, armor_toughness +2, knockback_resistance +0.5, max_health +6 | attack_damage +1, armor +1, max_health +1.5 | use: `hammer.overhead_smash`, sneak: `hammer.shockwave`, key1: `hammer.skyward_smash`, key2: `hammer.quake_stomp`, key3: `hammer.titan_fall` | `heavy_blows`, `synthesis`, `crushing_weight` |

Fairness of the climb: tier 1 is 64 common items (cobblestone and coal), tier 2 is 96 items of iron, gold and blaze rods (the Nether is needed), tier 3 is 32 diamonds, 48
obsidian and 8 netherite scrap (a real task). Every item id was checked against the vanilla item and tag lists (`minecraft:` only); the forms system resolves them again at
start-up and the CI self-test `hero.forms` fails on a wrong id.

## 4. The techniques

Seven techniques, one ultimate. The budget rows (the pure table `HammerNumbers`, judged by `Budget` in `HammerNumbersTest`; "raw" is before the 1.0 to 2.2 level factor of the
bond and before the balance law; the cooldown is the BASE cooldown, see section 12):

| skill id | kind | raw per hit | hits | cooldown | cost (of max magicules) | targets |
|---|---|---|---|---|---|---|
| `hammer.overhead_smash` | heavy | 11 | 1 | 80 ticks (4 s) | 1.5 % | 12 |
| `hammer.shockwave` | damage | 7 | 1 | 100 ticks (5 s) | 2 % | 12 |
| `hammer.armor_crack` | heavy | 10 | 1 | 120 ticks (6 s) | 1.5 % | 1 |
| `hammer.quake_stomp` | damage | 9 | 1 | 140 ticks (7 s) | 2.5 % | 12 |
| `hammer.skyward_smash` | damage | 8 | 1 | 90 ticks (4.5 s) | 1.5 % | 1 |
| `hammer.forge_aegis` | utility | 0 | 0 | 300 ticks (15 s) | 3 % | 10 |
| `hammer.titan_fall` | ultimate | 24 | 1 | 900 ticks (45 s) | 6.5 % | 12 |

How every heavy blow works: at the cast the technique draws its TELL (a closing ring or a dotted line on the ground, from the boss move effects of the mod) and the blow lands
a few ticks later ([Strike](#9-the-code-the-why-of-each-class) `later`, which re-checks that the wielder is alive, in the same world and still bonded). The wielder is slowed while
a heavy blow winds up (the weapon is heavy). The ground blows (Overhead Smash, Shockwave, Quake Stomp) only touch foes that stand on something or swim; a foe in the air
passes over them. All damage goes through `HeroCtx.hit` (the balance law). A cast that finds no target ("No foe within reach") returns false, so the dispatcher gives the cost back.

* **Overhead Smash** (`use` of every form).
  *What:* raises the hammer and brings it down on the ground 2.8 blocks ahead (pulled back from a wall); everything on the ground within 3.4 blocks of the spot is hit.
  *Numbers:* raw 11 per hit, up to 12 foes, shove 0.55 and a small lift, 42 poise.
  *Tell:* a dim ring closes in on the landing spot for 16 ticks and a base ring is drawn; an anvil sound; the wielder is slowed (Slowness III) for the 12 ticks of the wind-up; the blow lands 12 ticks (0.6 s) after the cast.
  *Counter:* step out of the ring or jump (a foe off the ground is not hit), or raise a shield.
  *Cost / cooldown:* 1.5 %, 80 ticks.
* **Shockwave** (`sneak` of the base, Stone and Synthesis forms).
  *What:* strikes the ground and sends a crack running up to 11 blocks ahead along the ground (stopped by a wall; needs at least 2.5 blocks of room, else "There is no room for a shockwave" and no cost). Everything on the ground within 1.2 blocks of the line is hit.
  *Numbers:* raw 7, up to 12 foes, shove 0.5, 24 poise.
  *Tell:* a dotted crack along the whole path for 12 ticks; the blow lands 8 ticks after the cast and a bright crack runs out.
  *Counter:* step sideways out of the 2.4-block-wide line, or jump.
  *Cost / cooldown:* 2 %, 100 ticks.
* **Armor Crack** (`key1` of the Small, Stone and Anvil Hammers).
  *What:* a short, hard blow on the foe you face (within 4.2 blocks, 40 degrees of the look, line of sight needed). If the foe is still within 5.7 blocks and alive 7 ticks later the hammer lands.
  *Numbers:* raw 10, single target, 36 poise, then the foe's armour is cut by 45 percent and its toughness by 30 percent for 160 ticks (8 s). It is the mod's timed attribute modifier, so it removes itself; re-applying restarts the timer, it does not stack.
  *Tell:* a ring on the foe's feet and a gathering of amber dust at its middle for 7 ticks, an anvil sound.
  *Counter:* get more than 5.7 blocks from the wielder in the 7 ticks (0.35 s) before the hammer falls, or raise a shield; the short tell is paid for by the single target.
  *Cost / cooldown:* 1.5 %, 120 ticks.
* **Quake Stomp** (`key2` from the Stone Hammer up).
  *What:* plants the hammer and stamps: the ground heaves in a ring of 5.2 blocks round the wielder; everything on the ground inside is hurt and thrown outward.
  *Numbers:* raw 9, up to 12 foes, shove 0.9 and lift 0.3, 30 poise. The wielder is slowed (Slowness IV) for the 14 ticks of the wind-up.
  *Tell:* a closing ring of the full radius at the wielder's feet for 18 ticks, anvil sound.
  *Counter:* jump, or run out of the ring in the 0.7 s it gives.
  *Cost / cooldown:* 2.5 %, 140 ticks.
* **Skyward Smash** (`sneak` of the Anvil Hammer, `key1` of the Synthesis Hammer).
  *What:* an upward blow on the foe you face (within 3.8 blocks, 45 degrees) that throws it into the air, out of reach of the ground blows, so a friend can strike it or the wielder can break off. A boss is only nudged (lift 0.25 instead of 0.95).
  *Numbers:* raw 8, single target, 26 poise, lift 0.95, a tiny shove of 0.15; it lands 5 ticks after the cast if the foe is within 5.3 blocks.
  *Tell:* a ring at the foe's feet and an anvil sound; the shortest tell of the weapon (5 ticks), paid for by the low damage.
  *Counter:* step out of reach in the quarter second, or get out of the ring; being in the air afterwards, the foe cannot be hit by Overhead Smash, Shockwave or Quake Stomp.
  *Cost / cooldown:* 1.5 %, 90 ticks.
* **Forge Aegis** (`key3` of the Anvil Hammer; support, no damage).
  *What:* the wielder and the allies within 7 blocks (at most 10; the mod's usual rule for friends: the wielder, team members, other players without a team, and the tamed animals of those) are tempered for 8 seconds: Resistance I and Absorption I (two hearts of absorption). It heals nothing.
  *Numbers:* 160 ticks. Cost 3 %, cooldown 300 ticks (15 s).
  *Tell:* a ring on the ground, a magic circle on the wielder, anvil and smithing sounds. It is not an attack, so it has no counter; the cooldown is the limit.
* **Titan's Fall** (`key3` of the Synthesis Hammer; the ultimate).
  *What:* needs solid ground to leap from (else "Titan's Fall needs solid ground to leap from", no cost). The wielder leaps (0.35 forward, 1.05 up) and comes down like a mountain; no fall damage. When the wielder lands (at the latest 38 ticks after the leap) everything within 7 blocks, on the ground or not, is struck.
  *Numbers:* raw 24, up to 12 foes, shove 1.2 and lift 0.5, 160 poise: more than the gauge of any ordinary foe (non-bosses have at most 80), so it breaks them; a boss is broken only if its gauge is 160 or less (a boss of up to 640 health), and then for one second.
  *Tell:* an amber column at take-off, then a ring of the landing radius drawn on the ground under the wielder every 6 ticks while in the air (the ring follows the wielder down, so it shows where the fall will land).
  *Counter:* leave the 7-block ring while the wielder is in the air (roughly 1.5 s, estimated from the leap speeds). It does not need ground under the foe, so a foe in the air within about 4.6 blocks above or below is hit too; the counter is distance.
  *Cost / cooldown:* 6.5 %, 900 ticks (45 s).

## 5. The passives

| id | form that gives it | what it does |
|---|---|---|
| `heavy_blows` | every form | every melee blow adds poise to the foe: 3, or 8 on a full swing (attack bar at 90 percent or more) |
| `rubble` | Stone Hammer and the forms that keep its signature (see `synthesis`) | a full swing throws stone chips at up to 3 foes within 2.2 blocks of the foe struck: raw 2.5 each and 4 poise each |
| `anvil_stance` | Anvil Hammer (and by `synthesis`) | while the wielder sneaks with the hammer summoned and in hand, a blow from another creature (falls, fire and the void are not blunted) lands at 82 percent |
| `crushing_weight` | Synthesis Hammer | every third full swing cuts the armour of the foe struck by 30 percent for 100 ticks (5 s) |
| `synthesis` | Synthesis Hammer | the canon power-up: this form keeps the signature passive of every form the wielder has learned (base: Heavy Blows, Stone: Rubble, Anvil: Anvil Stance, Synthesis: Crushing Weight), not only its own. A lower form keeps only its own |

The melee blows are the ones the item reports (`HeroLogic.onHit`); a technique's own damage does not come that way, and `Strike.inHit` additionally guards against a blow that
would re-enter the hook.

## 6. Poise and break (the weapon's own rule)

Every blow of the hammer builds POISE damage on the foe, an unseen gauge that is as big as the foe is tough. When it fills, the foe BREAKS: staggered for a moment and easier to
hurt, and then it cannot be broken again for a while, so the wielder gets a window and the foe a window to recover, not a stun that could be chained forever. The gauge is four
numbers in the foe's own saved data (`nusmp_hv_pz`, `nusmp_hv_pt`, `nusmp_hv_pb`, `nusmp_hv_pl`) and is read lazily from the clock: no timer, nothing left in a table when the foe
unloads or dies, and a lock that lies further ahead than any lock can (a copied entity from another clock) counts as over.

* **Size of the gauge:** an ordinary foe 60 percent of its maximum health, from 12 to 80 points (a player, 20 health, has 12: one heavy blow breaks a player); a boss (the balance law's test: the boss tag or 300 health) 25 percent of its health, from 100 to 300.
* **Recovery:** the gauge is held for 60 ticks after a blow, then drains over 100 ticks.
* **A break of an ordinary foe:** 50 ticks (2.5 s) of Slowness IV, 100 ticks of Weakness I and 60 ticks of Tensura's Fragility; and, if the wielder passes the shared control lock (`HeroCtx.control`, 12 s), a root for the time the balance law allows (`BalanceLaw.controlTicks` of 30: 1.5 s, less if the foe was controlled in the last 30 s) with Slowness VII and Tensura's Movement Interference. Then the foe is unbreakable for 200 ticks (10 s).
* **A break of a boss:** only 20 ticks (1 s) of Slowness II, and 400 ticks of lockout. The Hammer never roots a boss.
* **No ordinary technique breaks even the weakest boss in one blow** (the highest is 42 poise against a gauge of at least 100); that is pinned by a test.

## 7. The curse series: none

The brief says a Seven Star weapon has no curse series unless it is argued. The Hammer has none: the summaries of the Curse Series pages list no cursed hammer, and the canon
power-up of the Hammer says plainly that curse forms cannot be fused (section 2), so a curse form would contradict the weapon's own rule. The forms file has no curse form and
`FormsCheck` for the Hammer is built with `curseAllowed = false`.

## 8. Engravings (the owner's rule 6, design.md section 16)

The exact line for `item/WeaponEngravings.java` (the lead integrates it):

```java
Map.entry("vassal_hammer", List.of(e("crushing", 3), e("sturdy", 2), e("enervation", 1))),   // a heavy blunt head that breaks armour and poise, forge-hardened, and saps what it breaks
```

* `crushing` 3: the Hammer is the impact weapon, built to break armour and poise (Armor Crack, Crushing Weight, the break); the table gives `crushing` 3 to the Doom Hammer for the same reason.
* `sturdy` 2: a forged head and a stance made for holding (Anvil Stance, Forge Aegis, the armour and toughness in its stats); the table gives `sturdy` to the hard-wearing weapons (the Doom Hammer, the Magebane Shield).
* `enervation` 1: a broken foe is weakened (Weakness, Fragility); the table gives `enervation` to the weapons that sap (the Club of Rot, the Butcher Cleaver).

## 9. The code: the WHY of each class

All in `src/main/java/com/newuniverse/nusmp/heroweapon/weapon/`. The four striking weapons share the package `strike/` (written once so that the picking of targets, the delayed
blow and the budget are the same four times); only the Hammer's own files are listed in the second table.

| file (`strike/`, shared by the Hammer, Claw, Axe and Whip) | why it exists |
|---|---|
| `SkillSpec` | the owner's budget for one technique (raw per hit, hits, cooldown, cost, targets) as one small immutable record, so the numbers live in a pure table a test can judge |
| `Budget` | design.md section 5 as code: raw 3 to 12 / 10 to 20 / at most 32 per hit, at most 12 targets, cooldown floors 60 / 100 / 600, costs 1 to 3 percent (5 to 8 for an ultimate); checked at the weapon's best multiplier, so the budget holds at its best, not only on paper |
| `FormsCheck` | the structural rules of a forms file plus what only the weapon knows (every skill id and passive is real, every one is used by some form, at most one ultimate per form), pure and Gson-only, so a weapon's test and self-test need nothing from the forms system |
| `Strike` | the server-side chores every technique shares and can get wrong: foes (never creative or spectator players, allies, pets or summons), the hit (through the balance law, hurt timer reset so a technique lands on top of a melee blow), the delayed blow that re-checks the wielder, movement without fall damage, shoves and lifts |
| `StrikeFx` | the effect doors of the mod under short names: the stone telegraph and strike (`BossMoveVfx`), the gravity impact (`ZogratisVfx`), dust on the ground (`Tells`), and a sound; never vanilla particles |
| `StrikeSkill` | a `HeroSkill` built from a `SkillSpec`, a name, a text and a body, with an optional cooldown factor that can never take the cooldown under the floor of its kind |
| `StrikeSelfTest` | the CI self-test `hero.weapon.<pkg>`, one implementation for the four weapons (section 11) |

| file (`hammer/`) | why it exists |
|---|---|
| `HammerNumbers` | every number of the weapon in one pure table, judged by the budget in a unit test |
| `PoiseMath` | the arithmetic of the gauge (size, hold, drain, break, lockout), pure, so it can be tested without a server |
| `HammerPoise` | the game side of poise: reads and writes the four numbers on the foe, carries out a break (slow, weaken, fragility, root through the control lock) and cracks armour |
| `HammerSynthesis` | the canon power-up as a table: the signature passive of each form and the rule of what a form keeps |
| `HammerSkills` | the seven techniques |
| `HammerLogic` | lists the skills and the hooks: Heavy Blows, Rubble and Crushing Weight on a melee blow, Anvil Stance on damage taken; registers the self-test |

## 10. Config keys, commands, quest hooks, lang, saved data

* **Config keys:** none. A weapon builder keeps its numbers in code (`HammerNumbers`, a pure table with a test); the dispatcher's own keys (the global lock between two techniques, the minimum cooldown, the level scaling of the cooldown) belong to the core.
* **Commands:** none of its own. The `/multiverse hero ...` commands of the core and the forms system handle every weapon.
* **Quest hooks:** none of its own. The dispatcher fires `hero.skill` with the skill id after every cast.
* **Lang:** `hero.skill.hammer.<id>.name` and `.desc` for the seven techniques and `hero.passive.hammer.<id>` and `.desc` for the five passives, in the lang fragment `hva.lang.json` (the lead merges it into `en_us.json`) (shared by the four weapons; 108 keys in all). The forms carry their own English names and descriptions. Messages to the wielder are plain strings in the code (no `hero.msg.*` keys).
* **Saved data:** on the foe `nusmp_hv_pz`, `nusmp_hv_pt`, `nusmp_hv_pb`, `nusmp_hv_pl` (the gauge); on the wielder `nusmp_hv_sw` (the count of full swings for Crushing Weight, harmless if lost); the attribute modifier ids `nusmp:hero_hammer_crack` and `nusmp:hero_hammer_crack_t` (transient, never saved).

## 11. Tests and the self-test

Pure JUnit 5 tests (no Minecraft import), all passing in the local shim:

* `hammer/PoiseMathTest` (8): gauge size, hold and drain, blows add up, a break and its lockout, a boss breaks briefly and rarely, a backwards clock is safe.
* `hammer/HammerNumbersTest` (7): every row inside the budget, the budget can fail on the Hammer, every heavy blow has a tell of at least 4 ticks, the heavy techniques break an ordinary foe in one blow but never a boss, the passive set matches the synthesis table, the stance still lets most of a blow through.
* `hammer/HammerSynthesisTest` (4): a plain form keeps only its own effect, the synthesis form keeps every learned one, a never-learned form lends nothing, unknown ids are safe.
* shared: `strike/BudgetTest` (6), `strike/FormsCheckTest` (8, with a negative control for every rule), `strike/StrikeFormsTest` (6: the four shipped forms files are checked against each weapon's numbers, hold the forms the docs list, and only the Claw and the Axe carry a curse form).

Mutation check (done once for the whole set, on 2026-10-18): in `FormsCheck` the lower bound of the ordinary forms (`ordinary < known.minOrdinary()`) was replaced by `ordinary < 0`; `FormsCheckTest.anUnreachableSkillAndATooSmallTreeAreReported` failed ("one form is below the minimum of two: expected true"); the original was put back (a `diff` against the copy shows no difference) and the tests of `FormsCheckTest` and `StrikeFormsTest` pass again.

The forms file also passes the forms builder's own validator (`FormsSchema`, run locally over the shipped file): 4 forms accepted, 0 errors, 0 warnings.

**Self-test `hero.weapon.hammer`** (registered from `HammerLogic`'s constructor, run by the CI server smoke test; it has NOT been run): it reads the shipped forms file from the jar and checks it
with `FormsCheck`; checks that the forms the game loaded name only skills the weapon has and passives it implements; checks every skill is registered with the right ultimate flag,
a name, a text and a sane cooldown and cost; then binds a stand-in player to the weapon, puts the summoned weapon in its hand, and casts EVERY skill under EVERY form that lists it beside a fresh dummy zombie
that does not think, then calls every hook of the logic once, all inside try/catch; it releases the bond and removes every dummy at the end.

## 12. Judgement calls and balance notes (for the lead and the owner)

* **Cooldowns are base cooldowns.** The core's dispatcher shortens a cooldown by the bond level (down to 0.75 at level 100) and floors it at the config's 20 ticks; the budget floors (60, 100, 600) hold for the rows, not after that scaling. At level 100 the 80-tick Overhead Smash runs 60 ticks, and the 900-tick ultimate 675. The dispatcher's 40-tick global lock between two techniques is the real limit on spamming. If the owner means the floors to hold after the scaling, that is a change in `HeroConfig.minCooldownTicks` / `DispatchRules`, not in the weapons.
* **The tell is drawn by the technique, not by the dispatcher's wind-up.** `HeroSkill.windUpTicks` makes the dispatcher wait and draw its own generic charge effect on the wielder; the Hammer wants a ring where the blow will land, so it casts at once, draws its tell and lands the blow itself a few ticks later (the cost is paid, and the cooldown starts, at the cast).
* **Armor Crack can be kept up.** The armour cut lasts 8 s and the cooldown is 6 s, so on one target it can stay on; it is a single, dodgeable blow with a visible tell, and the cut does not stack.
* **Players are broken by one heavy blow** (their gauge is the minimum, 12). The harm of a break is bounded: Slowness IV for 2.5 s, Weakness, Fragility, a root of at most 3 s only if the shared 12 s control lock is free, then 10 s in which they cannot be broken again.
* **Forge Aegis tempers strangers.** It uses the mod's friend rule (`SongTargets.allies`), which counts every teamless player as a friend, as the song and Time-book effects do. Absorption I is a vanilla effect (4 points), not a heal through `HeroCtx.heal`.
* **Ground blows ignore the airborne.** That is the counter to Overhead Smash, Shockwave and Quake Stomp, and Skyward Smash sends a foe there on purpose.

## 13. What is NOT verified (honest list)

Nothing of the weapon has been run in the game. The code was type-checked against API stubs of Minecraft, NeoForge and Tensura (0 errors in my files; about 1,175 stub artefacts in
other files are ignored); the pure logic is covered by the unit tests above; the forms file by the forms builder's validator. Not verified:

* **Everything visual.** Which `VfxShape` draws what, whether every shape honours the colour payload (amber `#FF9A2E`), how the stone telegraph looks next to the dust rings, and whether the tells are as readable as I believe.
* **The self-test** `hero.weapon.hammer` (never run), including the stand-in player helper `SelfTests.player` and reading the forms file from the jar with `getResourceAsStream`.
* **The Tensura effect ids** `tensura:fragility` and `tensura:movement_interference`: if an id does not exist the effect is skipped silently (the mod's `Fx.addEffect`), the break then only slows and weakens. What Fragility does exactly is Tensura's.
* **Feel and balance.** The numbers are judged on paper by the budget; the time to kill, the strength at bond level 100 against the existence scaling of the balance law, and the size of the poise gauges against real Tensura monsters are not tried.
* **`HeroCtx.hit` on a blocking player:** the shield may absorb a ground blow like any attack (the hit goes through `hurt` with a player-attack source).
* **Whether the Hammer's techniques and the dispatcher agree on `cast` returning false** (a cast that finds no foe returns false so the cost is refunded) is read from the dispatcher's source in the core builder's worktree, which was still being written.
* **A boss that is not a boss by the balance law's test** (neither the boss tag nor 300 health) is treated as an ordinary foe and can be rooted for the time the law allows.
