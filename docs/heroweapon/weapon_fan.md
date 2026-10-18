# Vassal Fan (`vassal_fan`): Glass' war fan

Class VASSAL, style SWING, canon holder Glass (the Fan Hero). Logic `heroweapon/weapon/fan/FanLogic`, forms `data/nusmp/hero/forms/vassal_fan.json`.
This page is written in sections; the LOOK section comes first because the art builder (`hart3`) reads it first.

## 1. Look (for the art builder)

Source of the look: the owner's text ("Vassal Fan (Glass)"), the series' fan hero as remembered (a heavy black-iron war fan, a tessen) and the design below. The wiki
pages could not give a picture; every colour here is MEMORY or DESIGN, not read from a picture.

* **Silhouette.** An OPENED war fan held by a short handle, the ribs fanned through about 150 degrees like a half circle, the middle rib pointing up-forward on the
  icon's diagonal. The face of the fan is a thin vertical plane (thickness about 0.03 block) so the first-person view sees the broad face. The two outer ribs are
  sharpened steel blades (Glass cuts with the fan's edge); the top arc of the fan is lightly serrated. Size: the radius of the open fan about 0.8 block (a large
  fan, class `handitem` with a largest dimension of 0.9 to 1.1; add a `fan` class to `weapon_classes.json` if the art builder prefers a range of its own).
* **Named parts (8 to 12).** `pivot` (the rivet and its collar, where the gem sits), `handle` (short, 0.22 block, wrapped), `tassel` (a red cord with two beads that
  hangs from the handle end), `rib_outer_l` and `rib_outer_r` (steel blades), `ribs` (nine jet-black lacquered iron ribs fanned between them), `face` (the pale
  silver-blue paper between the ribs, with a plum branch embroidered in silver), `rim` (a thin silver band along the top arc), `teeth` (small steel serrations on the
  top arc), `gem`.
* **Palette.** Glass herself wears a jet-black kimono with silver embroidery (a wiki search summary), so the fan follows her: jet-black iron `#15151B`, steel
  `#9AA3AD`, face `#D9E4EC` (pale silver-blue, a little translucent look in the texture), silver trim `#C5CED8`, tassel red `#B3202A`, plum petals white-pink `#F7DDE6`
  on the face, gem spirit cyan `#5CE1FF`. (Which of these the series really paints on the Vassal Fan is MEMORY or DESIGN: no picture could be read.)
* **The gem.** A round cabochon (radius about 0.05 block) in the pivot collar on the face side, spirit cyan; it is the centre the ribs radiate from, so it reads at once.
* **Glow skin** (additive): the gem (strong), a soft ring around the collar, the plum branch on the face (faint cyan), the silver rim (very faint). Nothing else glows.
* **In hand.** The grip at the mesh origin; mesh -x is the forward edge (the fan opens toward up-forward, its blade ribs on the -x side). The face is turned so the
  wielder sees the broad side in first person. Sprite (16 x 16, kept as the inventory icon): an opened dark fan with a pale face and a cyan dot at the pivot.
* **Mood.** Elegant and light; no spikes, no skulls. When the form is the Demon Dragon Fan the art builder may add nothing: forms share one look (`look: base`).

## 2. Canon facts and where they come from

Only search summaries of the fandom wiki could be read (the pages themselves cannot be opened from the build machine). "Basis" says which part of a claim is a
SUMMARY (read), MEMORY (what I remember of the series, not confirmed) or DESIGN (mine).

| fact | page | basis |
|---|---|---|
| The Vassal Fan is one of the eight Vassal Weapons of Glass' World (fan, scythe, boat, katana, book, mirror, musical instrument, harpoon); the owner lists seven of them (no boat). | https://shield-hero.fandom.com/wiki/Vassal_Weapons , https://shield-hero.fandom.com/wiki/Glass'_World | summary |
| Its hero is the Fan Hero; the current one is Glass, chosen over the other disciples of a martial arts school. | https://shield-hero.fandom.com/wiki/Vassal_Fan | summary |
| Its power-up method is absorbing the magic that leaks out of foes as they attack, and the magic residue left in the air when they fall, to power up the weapon and its forms. | https://shield-hero.fandom.com/wiki/Legendary_Weapons/Power-up_Methods , the Vassal Fan page | summary |
| Forms: Demon Dragon Fan, White Tiger Fan, Keystone Fan, Wave Sealing Sword (an upgrade of the Keystone Fan). | the Vassal Fan page | summary |
| Skills: Reverse Snow Moon Flower (a red tornado of flower petals; the White Tiger Fan unlocked it), Tortoise Shell Cracker (a piercing thrust that can ignore defence), Moon Break, Flower Wind, Instant, Combination Formation, Sword Dance: Mizuchi; Phantasm, a second fan, Mist Veil, Lawless Counter and Snow Plum (a buff; the Wave Sealing Sword unlocked these two), Wind Dance Melody (evasion). | the Vassal Fan page, https://shield-hero.fandom.com/wiki/Glass/Powers_and_Abilities | summary |
| Glass is a spirit person with a jet-black kimono with silver embroidery and a bond with Sildina. | https://shield-hero.fandom.com/wiki/Glass | summary |
| A Vassal Weapon's holder may be a native or a summoned person and does not suffer the Holy Weapons' experience penalty; vassals can freely cross worlds. | https://shield-hero.fandom.com/wiki/Vassal_Weapons | summary |
| The Vassal Fan is a black iron war fan; the colours of the Vassal Fan itself. | - | MEMORY, unconfirmed |
| The base form is called "Beginner's Fan" (the wiki names Beginner's Scythe and Beginner's Katana); there are only four fan forms named in the summary, the fifth form of this file (the base) is named by analogy. | - | DESIGN |
| NOT built: Phantasm, the second fan, Lawless Counter (the Katana has the counter), Instant, Combination Formation and Sword Dance: Mizuchi. | - | DESIGN |

## 3. How it plays

The Fan is the graceful, mobile one of the four, a crowd tool and an escape artist. **Flower Wind** (right click) is its bread and butter: a wide arc of cutting wind that
nudges foes back. Every sweep that cuts two foes or more, every kill and every fourth full swing gathers a stack of **magic residue** (up to six; the series' ambient mana
absorption). **Moon Break** throws a crescent down a line. The **Repelling Gust** and the **Gale Pull** push and pull the crowd (the pull is hard control and takes the shared
12 second lock; the gust also turns arrows back on archers). **Wind Dance** is a gliding step, and holding sneak while you fall (Featherfall) glides you down. The
**Mist Veil** makes mobs lose you for a moment, the **Snow Plum** blesses your friends, the **Tortoise Shell Cracker** is the one thrust that grows with the foe's armour,
and when the fan holds residue, **Reverse Snow Moon Flower** burns all of it in a red tornado of petals. The four forms are four ways to use the same nine techniques:
a quick skirmisher (White Tiger), a steady controller (Keystone), the one with the ultimate (Demon Dragon) and the sealing sword that reaches farther (Wave Sealing).

## 4. The forms

| form | tier | parents | requires (absorbed) | mastery | stats while active | skills (slot: technique) | passives |
|---|---|---|---|---|---|---|---|
| **Beginner's Fan** (`base`) | 0 | - | nothing | - | attack_damage 3, attack_speed -1.2, movement_speed 0.02 | use: Flower Wind; sneak: Wind Dance; key1: Repelling Gust; key2: Mist Veil | `featherfall`, `residue` |
| **White Tiger Fan** (`white_tiger_fan`) | 1 | `base` | 32 x feather; 24 x iron_ingot; 16 x white_wool | 100 | attack_damage 4, attack_speed -0.8, movement_speed 0.05 | use: Flower Wind; sneak: Moon Break; key1: Wind Dance; key2: Tortoise Shell Cracker; key3: Repelling Gust | `featherfall`, `residue`, `tiger_pounce` |
| **Keystone Fan** (`keystone_fan`) | 1 | `base` | 32 x stone_bricks; 24 x copper_ingot; 12 x amethyst_shard | 100 | attack_damage 3, attack_speed -1.4, armor 2, knockback_resistance 0.15 | use: Flower Wind; sneak: Gale Pull; key1: Repelling Gust; key2: Mist Veil; key3: Snow Plum | `featherfall`, `residue`, `keystone_ward` |
| **Demon Dragon Fan** (`demon_dragon_fan`) | 2 | `white_tiger_fan` | 24 x phantom_membrane; 16 x blaze_rod; 12 x breeze_rod; 8 x tensura:low_magisteel_ingot | 220 | attack_damage 6, attack_speed -1, movement_speed 0.06, max_health 4 | use: Flower Wind; sneak: Moon Break; key1: Tortoise Shell Cracker; key2: Wind Dance; key3: Reverse Snow Moon Flower | `featherfall`, `residue`, `tiger_pounce`, `dragon_surge` |
| **Wave Sealing Sword** (`wave_sealing_sword`) | 2 | `keystone_fan` | 48 x prismarine_shard; 16 x prismarine_crystals; 32 x amethyst_shard; 8 x tensura:low_magisteel_ingot | 220 | attack_damage 5, attack_speed -1.2, armor 3, armor_toughness 1, knockback_resistance 0.25 | use: Flower Wind; sneak: Gale Pull; key1: Snow Plum; key2: Mist Veil; key3: Repelling Gust | `featherfall`, `residue`, `keystone_ward`, `sealing_tide` |

The tree is two branches of two: the White Tiger Fan toward the Demon Dragon Fan (offence and the ultimate) and the Keystone Fan toward the Wave Sealing Sword (control and
support). Tier 1 asks for common things (feathers, iron, wool; stone bricks, copper, amethyst), tier 2 for rarer (phantom membranes, blaze and breeze rods, prismarine, a few
Tensura low-magisteel ingots). The gem takes only items some form of the Fan asks for; absorbed items accumulate and are never consumed by learning a form. Every stat is
inside the budget of design.md section 6 and every ability bonus inside a quarter of it (the test `FanFormsTest` checks both).

## 5. The techniques

| technique (`id`) | kind | rest | cost | raw damage | foes | works for |
|---|---|---|---|---|---|---|
| **Flower Wind** (`fan.flower_wind`) | ordinary | 3 s (60 ticks) | 1 % | 7 | 8 | instant |
| **Moon Break** (`fan.moon_break`) | ordinary | 4 s (80 ticks) | 1.5 % | 8 | 6 | instant |
| **Repelling Gust** (`fan.repel_gust`) | utility | 6 s (120 ticks) | 1.5 % | none | 12 | instant |
| **Gale Pull** (`fan.gale_pull`) | utility | 8 s (160 ticks) | 1.5 % | none | 8 | 0.2 s |
| **Wind Dance** (`fan.wind_dance`) | utility | 5 s (100 ticks) | 1.5 % | 4 | 4 | 4 s |
| **Mist Veil** (`fan.mist_veil`) | utility | 10 s (200 ticks) | 1.5 % | none | - | 4 s |
| **Snow Plum** (`fan.snow_plum`) | utility | 10 s (200 ticks) | 2 % | none | 12 | 8 s |
| **Tortoise Shell Cracker** (`fan.tortoise_shell_cracker`) | heavy | 7 s (140 ticks) | 2.5 % | 18 | 1 | 0.5 s |
| **Reverse Snow Moon Flower** (`fan.reverse_snow_moon_flower`) | ultimate | 35 s (700 ticks) | 6 % | 5 x 6 = 30 | 12 | 4 s |

Rests are before the level scaling (the dispatcher shortens them by up to a quarter); damage is raw, before the level factor (1.0 to 2.2) and the balance law; a technique
that finds nothing to act on refuses and costs nothing.

### Flower Wind (`fan.flower_wind`)

A wide sweep of cutting wind and petals: up to 8 foes within 5.5 blocks in front take 5 damage and are nudged back, and a sweep that cuts two or more gathers magic residue. Counter: stay out of the arc.

* **Numbers:** ordinary technique, rests 3 s, costs 1 % of the maximum magicules, raw damage 7 per foe at most.
* **Tell:** None needed (5 raw, 7 with Dragon Surge, under the 8 raw that asks for one): the arm swings and a crescent of wind opens ahead.
* **Answer:** Stay out of the 110 degree arc, or beyond its reach of 5.5 blocks (7 with Dragon Surge).
* **Effects:** `WIND_SLASH` (the crescent), `CHERRY_BLOSSOM_FX3` (a spray of snow petals opening a little ahead of the wielder), the sweep sound, an arm swing.
* **Why:** It is the use-key technique of every form, the bread and butter, and the one that feeds the residue: a sweep that cuts two foes or more adds a stack.

### Moon Break (`fan.moon_break`)

Flings a crescent of wind 14 blocks forward that cuts up to 6 foes in its path for 8 damage. Counter: step aside; walls stop it.

* **Numbers:** ordinary technique, rests 4 s, costs 1.5 % of the maximum magicules, raw damage 8 per foe at most.
* **Tell:** The crescent leaving the hand and its sound; 8 raw is the ordinary ceiling that needs no wind-up.
* **Answer:** Sidestep the line (0.9 blocks to each side of it), or put a wall in the way: the crescent stops at the first block.
* **Effects:** `WIND_EMPEROR` (the big layered crescent flying from the hand to the end of the line), two sounds.
* **Why:** Canon Moon Break; gives the Fan a way to hit something at range that is not a thrust, and the line pierces up to six foes.

### Repelling Gust (`fan.repel_gust`)

A ring of wind pushes every foe within 5 blocks away and turns arrows back on their archers. It deals no damage. Heavy foes barely move.

* **Numbers:** utility technique, rests 6 s, costs 1.5 % of the maximum magicules, no damage.
* **Tell:** None (no damage): the ring of wind is the effect.
* **Answer:** Nothing to dodge; heavy foes (knockback resistance) move less, and a foe that is pushed can simply come back.
* **Effects:** `WIND_RING` (rings of wind rippling out), the wing-flap sound. Arrows and other projectiles within the ring that are not the wielder's or an ally's are turned back (`Projectile.deflect(REVERSE)`), which also makes them the wielder's.
* **Why:** The 'gust that pushes'. It is also the Fan's answer to archers. It is a nudge, not hard control, so it does not take the 12 second lock.

### Gale Pull (`fan.gale_pull`)

Draws up to 8 foes in front of you toward a point just ahead (heavy foes less). It is hard control, so it shares the 12 second control rest, and bosses are not pulled. Counter: stay beyond 9 blocks.

* **Numbers:** utility technique, rests 8 s, costs 1.5 % of the maximum magicules, no damage.
* **Tell:** The whirl of wind at the point ahead and the wind lanes from the foes to it.
* **Answer:** Stay beyond 9 blocks (10.5 with Sealing Tide) or out of the 100 degree cone; a foe with knockback resistance is drawn less; bosses are not drawn at all.
* **Effects:** `WIND_TORNADO` (small, at the point), `WIND_GALE` lanes from up to three foes, a low flap.
* **Why:** The 'gust that pulls'. Pulling is hard control, so it asks `HeroCtx.control()` (the shared 12 second lock) only once it knows it will work, and says how long the lock still holds when it is refused.

### Wind Dance (`fan.wind_dance`)

A gliding dash of about 6 blocks that brushes aside foes in the way for 4 damage; slow falling follows for 4 seconds.

* **Numbers:** utility technique, rests 5 s, costs 1.5 % of the maximum magicules, raw damage 4 per foe at most.
* **Tell:** None (it is a step): the wind mantle forms round the wielder.
* **Answer:** Nothing: foes brushed on the way take 4 and are nudged aside; the wielder can be hit during and after the step like any other time.
* **Effects:** `WIND_ZEPHYR` following the wielder (the wings and mantle of gale), `WIND_SLASH` along the path, the elytra sound.
* **Why:** Canon Wind Dance Melody (evasion). The 'gliding movement' of the brief: a dash of about 6 blocks and four seconds of slow falling, and the Featherfall glide takes over if the wielder keeps sneaking.

### Mist Veil (`fan.mist_veil`)

Wraps you in mist for 4 seconds: mobs hunting you lose their target and you turn invisible. Attacking ends the veil. Foes right beside you still notice you.

* **Numbers:** utility technique, rests 10 s, costs 1.5 % of the maximum magicules, no damage.
* **Tell:** The mist round the wielder (`MIST_MAGIC`).
* **Answer:** Foes right beside the wielder still notice them (invisibility cuts how far mobs see); players see the mist.
* **Effects:** `MIST_MAGIC` following the wielder, a spell sound; invisibility (no particles) for 4 seconds.
* **Why:** Canon Mist Veil, a utility: mobs that hunt the wielder drop their target once, and attacking (a technique or a real swing) ends the veil, so it is an escape or an opening, not a cloak.

### Snow Plum (`fan.snow_plum`)

Plum blossoms bless you and your friends within 8 blocks: speed for 8 seconds and a small heal (never more than 25 percent of their health).

* **Numbers:** utility technique, rests 10 s, costs 2 % of the maximum magicules, no damage.
* **Tell:** The white plum blossoms opening on the ground over a radius of 8.
* **Answer:** -
* **Effects:** `CHERRY_BLOSSOM_FX2` (a ground seal of snow petals), `SEAL_WOUND` streams from the wielder to up to four friends, a chime.
* **Why:** Canon Snow Plum (a buff): speed for 8 seconds and a small heal through `HeroCtx.heal` (2 plus 4 percent of their maximum health, never more than a quarter). Friends are the wielder and what the game counts as allied (team mates, the wielder's pets).

### Tortoise Shell Cracker (`fan.tortoise_shell_cracker`)

The wind coils at the fan tip, then a thrust cracks the foe on your crosshair for 10 damage plus half its armour (up to 8 more). Counter: leave its line of sight or raise a shield.

* **Numbers:** heavy technique, rests 7 s, costs 2.5 % of the maximum magicules, raw damage 18 per foe at most.
* **Tell:** A thin line of dust from the fan to the mark and the wind coiling at the fan tip for half a second (10 ticks) before the thrust.
* **Answer:** Break the line of sight, or get farther than 12 blocks from the wielder (the mark is chosen within 9), during the half second; or raise a shield (the thrust is an ordinary hit and a shield blocks it).
* **Effects:** `STAR_MAGIC` coiling at the tip, a dust line (`Tells.line`), then `WIND_GALE` to the foe and a `STAR_MAGIC` glint on it, the critical-hit sound.
* **Why:** Canon Tortoise Shell Cracker, a piercing thrust that can ignore defence: here the damage rises with the foe's armour (10 plus half an armour point, at most 8 more, so 18 at most, inside the heavy limit of 20) so armour does not stop it.

### Reverse Snow Moon Flower (`fan.reverse_snow_moon_flower`)

Ultimate. After a gathering of red petals a tornado of petals stands where you aim for 3 seconds and cuts foes inside it 6 times; every residue stack burns for more damage. Counter: leave the red circle.

* **Numbers:** ultimate technique, rests 35 s, costs 6 % of the maximum magicules, raw damage 30 per foe at most.
* **Tell:** A red dust ring of 5 blocks on the ground and a red ground seal of petals for a full second (20 ticks) where the tornado will stand.
* **Answer:** Leave the red circle; the tornado draws foes in only gently (0.25 blocks per tick at most).
* **Effects:** `CHERRY_BLOSSOM_FX2` (the red seal), then `WIND_TORNADO` tinted red for the 3 seconds it stands, `CHERRY_BLOSSOM_FX3` bursts at the start and the end.
* **Why:** Canon Reverse Snow Moon Flower, 'a red tornado of flower petals', and the Fan's loop: it spends ALL the residue the fan has gathered when it rises (3.5 raw per pulse and 0.25 more for each stack, six pulses, 30 at most).

## 6. Passives

| passive | what it does | forms |
|---|---|---|
| **Featherfall** (`featherfall`) | The fan's wind cushions you: falls hurt 60 percent less, and holding sneak while you fall lets you glide down slowly. | all five |
| **Magic Residue** (`residue`) | The fan drinks the magic that leaks from foes: a kill, a sweep that cuts two or more, and every 4 full swings each add a stack (up to 6) that the petal tornado burns. Putting the fan away loses them. | all five |
| **Dragon Surge** (`dragon_surge`) | At 6 residue the sweep of Flower Wind reaches 1.5 blocks farther and cuts for 2 more. | Demon Dragon Fan |
| **Sealing Tide** (`sealing_tide`) | The gust and the pull of this sealing fan reach 1.5 blocks farther. | Wave Sealing Sword |
| **Tiger Pounce** (`tiger_pounce`) | Wind Dance carries you a quarter faster. | White Tiger Fan, Demon Dragon Fan |
| **Keystone Ward** (`keystone_ward`) | While the Mist Veil holds, projectiles hurt you 40 percent less. | Keystone Fan, Wave Sealing Sword |

## 7. Curse series

None. The brief gives the Fan no series, and the seven sins are taken: the Holy Weapons have the canon ones, the Mirror (Envy) and the Chain (Sloth) the other two
(design.md section 2). The forms file therefore has no `curse` form and only the base look.

## 8. Engravings (for `item/WeaponEngravings`, lead integration)

```java
Map.entry("vassal_fan", List.of(e("swift", 2), e("elemental_boost", 1), e("magicule_absorption", 1))),   // Glass' fan
```

* `swift` 2: the Fan is the quick, light weapon (wind, the glide, a step that covers six blocks).
* `elemental_boost` 1: its techniques are wind; the sweeps and the tornado are elemental play.
* `magicule_absorption` 1: the canon power-up method is absorbing the magic that leaks from foes (the residue).

## 9. Numbers and the budget

`FanNumbers` holds every number once. Its table is checked by `FanNumbersTest` against `Budget` (design.md section 5): an ordinary technique at most 12 raw, a heavy one at
most 20, the ultimate at most 32; a damage technique rests at least 60 ticks, a utility at least 100, the ultimate at least 600; a cost of 1 to 3 percent, the ultimate
5 to 8; at most 12 foes; nothing works for more than 20 seconds. The edges the test pins: Flower Wind with Dragon Surge is 7, the Tortoise Shell Cracker against any armour is 18
at most, the petal tornado with six stacks is exactly 30 (six pulses of 5), the Snow Plum heal never passes a quarter of a friend's health at any health, and the glide needs
sneaking in the air and either a fall of at least 0.25 blocks per tick or 3 blocks fallen to begin with, and then holds for as long as sneak is held (a glide that has slowed the fall is
renewed because it is already gliding, not because it is still falling fast), and not while flying, swimming, climbing or riding. The texts (`FanText`) are built from the same constants, so a description cannot say 5 while the code does 6.

## 10. Code map (the WHY of each file)

| file | why |
|---|---|
| `weapon/fan/FanLogic` | the part the shared machinery calls: lists the skills, the Featherfall fall and glide, the Keystone Ward, ending the veil on an attack, residue from swings and kills, the self-test `hero.weapon.fan`, forgetting a wielder |
| `weapon/fan/FanSkills` | the nine techniques, one small method each |
| `weapon/fan/FanNumbers` | every number, the budget rows, the pure rules (armour bonus, residue damage, plum heal, glide) |
| `weapon/fan/FanText` | every name and description, built from the numbers; writes the lang entries |
| `weapon/fan/ResidueLedger` | the pure counter of magic residue |
| `weapon/fan/FanState` | the per-wielder memory (residue, veil, glide), bounded, cleared on logout, when the fan is put away and on server stop |
| `data/nusmp/hero/forms/vassal_fan.json` | the five forms |
| `weapon/glassworld/*` | the kit shared with the Scythe, the Katana and the Book (see `docs/heroweapon/weapon_glassworld.md`) |

Tests: `FanNumbersTest`, `ResidueLedgerTest`, `FanTextTest`, `FanFormsTest` (and the kit's tests). Self-test in the game: `hero.weapon.fan` (binds a stand-in player, casts every
skill of every form beside two dummy zombies, calls every hook, none may throw); run it with the existing `/multiverse mega selftest hero.weapon.fan` (permission 2). Config keys: none
(the numbers are code). Commands: none of its own. Language: `hero.skill.<id>.name|desc` for the nine skills and `hero.passive.fan.<id>` (+ `.desc`) for the six passives, in `frag/hvc.lang.json`.

## 11. What is unverified

* **Nothing here was run in a game.** The type checker (javac against the API stubs) and the pure unit tests are all that ran; every statement about how a technique
  feels (distances of a dash, the look of an effect, the timing of a tell) is a calculation or a reading of the effect layers' comments, not something seen.
* **The dispatcher and the hooks are the core builder's.** The skills assume what the contract says: a refused cast (returns false) costs nothing and starts no rest,
  `onHit` follows a melee blow of the wielder, `onDamaged` runs before the wielder takes the damage and its return value replaces the amount, `onTick` runs about every
  five ticks while the weapon is summoned. The kit's guard `GlassKit.striking()` keeps a weapon from answering its own techniques' damage if the core's `onHit` also fires
  for them.
* **Effects.** Shapes, colours, durations and sizes were chosen from the javadoc of the effect layers (`vfx/client/layer/*`); the layers were not run. A shape whose
  `power` means something else in play than its comment says would look off, not break.
* **Forms.** Every item id was checked against the vanilla item list and the Tensura id reference, and the three Tensura ids used are the ones other code of the repo
  already uses; the forms system's own self-test `hero.forms` resolves them against the real registries in the game. The counts are a first balance, not tuned.
* **Stats.** `attack_damage` and `attack_speed` are read as the forms builder reads them (modifiers on the item); if it reads them as absolute values the weapons
  would hit far too softly or fast, and the numbers here would need one pass.
* **Damage immunity.** Every technique strike first cuts the foe's vanilla damage immunity (`invulnerableTime`) to ten ticks (`GlassKit.strike`, rule `Immunity`, tested), so a blow lands in full instead of being swallowed by the hit before it, as the older weapons of this mod do. No technique of the Fan is a quick series (the petal tornado pulses ten ticks apart), so the exception for players is not used here. Whether another mod changes the immunity of its entities was not checked.
* **The dash.** Wind Dance sets a velocity (0.95 blocks per tick horizontally, 1.19 with Tiger Pounce, plus a small lift on the ground); the distance it really covers
  depends on friction and was not measured. The glide renews slow falling every five ticks while you hold sneak, a rate that assumes `onTick` runs every five ticks. It reads the wielder's fall speed from `getDeltaMovement()`, which for a player on a server is only the server's own model of the fall and may be stale; the distance fallen (`fallDistance`, 3 blocks, which a server does track) starts the glide as well, and once it holds it needs neither.
* **Deflected arrows** keep their own vanilla damage (they are not hits through the balance law); they become the wielder's, so they can hurt the archer.
* **Canon:** the colours and shape of the weapon, and which of the series' skills each form owns, are from summaries and memory only.
