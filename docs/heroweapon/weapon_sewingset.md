# Vassal Sewing Set (`vassal_sewing_set`): the needle and thread of S'yne's world

Builder `hvd`. Logic: `heroweapon/weapon/sewingset/**`, forms: `data/nusmp/hero/forms/vassal_sewing_set.json`. Style CAST, class Vassal Weapon.
Written in two passes: the LOOK section (1) first so the art builder could start, then the rest.

## 1. The look (for the art builder `hart4`)

**Decision: a long NEEDLE with a THREAD SPOOL for a grip, and a small pair of scissors hanging from the spool.** Why: the wiki summaries give the set a PURPLE gem and list its
skills as thread work (Bind Wire, Spider Wire, Skill Seal), needle work (Needle Shot, Jumping Needle) and one cut (Scissor Cut). A needle held spool-first is a one-hand focus
that reads as a weapon (a long thin point), carries the thread as a drawn, flowing named part, and the scissors charm covers the Scissor Cut without turning the whole
weapon into a pair of shears (shears would hold the thread poorly and read as a garden tool). (State of knowledge: the purple gem and the skills are from a summary; the shape is design.)

* **What it is.** A steel needle about 1.1 blocks long with a large oval eye near its blunt end, set into a wooden thread spool (the grip, about 0.3 blocks tall, with two
  disc flanges), so the weapon is about 1.5 blocks in all (size class `rapier` 1.0 to 1.3 or a thin `wand`: the art builder decides by the real-life size rule). A silk
  thread is wound on the spool, runs out through the needle's eye and trails in one loose S-curve in the air (a very thin sweep of 6 to 10 segments, it must not be fragile).
  Small embroidery scissors hang from the spool's side on a short ring and cord.
* **Named parts** (8 to 13): `needle` (a long thin loft, a round point), `eye` (a torus or a flat oval hole in the shank), `spool_core`, `flange_top`, `flange_bottom`,
  `thread_wound` (a wrapped cylinder), `thread_trail` (the sweep), `scissors` (two small blades and two ring handles: keep to 60 quads), `cord` (the ring and cord), `gem`.
* **The gem.** In the CENTRE of the top flange (the end cap of the spool, where the needle enters): a purple cabochon, about 0.13 blocks, in a silver bezel. It is the glow.
* **Palette**: needle polished steel `#D5DCE3` (shadow `#8F9AA6`); spool dark violet lacquered wood `#4B2A6B` with silver rims `#BFC6CF`; thread crimson silk `#C03A78` fading to
  violet `#7A3FB5` along the trail; scissors steel `#C9D0D8` with brass ring handles `#C8A24A`; cord `#6E5B3A`; gem `#A64DFF`.
* **Glow skin** (additive): the gem (`#D7A8FF`, full), the trailing thread (a faint violet `#B77CF0`, brighter toward its free end), and a hair-thin line down the needle (`#E8D8FF`).
* **Mood.** Quiet, precise, a little sad (the series says the set is slowly losing power because its world was destroyed): fine details, no heavy decoration.
* **No curse look** (this weapon has no Curse Series).
* **Icon (16 x 16 sprite).** A silver diagonal needle with a purple spool at the lower left and a violet pixel curl of thread near the upper end.

## 2. Canon: what the series says, and what is design

Source types: **S** = a search summary of the series' fan wiki (the pages themselves could not be opened from the build machine), **M** = memory, **D** = design. Nothing
here was checked against the light novels or the anime.

| Fact | Source |
|---|---|
| The Vassal Sewing Set is a weapon of S'yne's world; S'yne Lokk is its current wielder and R'yne, her teacher, held it first. Its purple gem absorbs materials and enables transformations. | S: https://shield-hero.fandom.com/wiki/Vassal_Sewing_Set, https://shield-hero.fandom.com/wiki/S'yne_Lokk, https://shield-hero.fandom.com/wiki/R'yne |
| Its home world was destroyed and the weapon is slowly losing power; teleporting several people at once is risky because it is weakened. | S |
| Thread skills: Bind Wire (thread that immobilises a target and stops it recalling skill names), Fire Paralysis Wire (the thread set alight), Spider Wire (restricts movement), Skill Seal (thread round the throat stops skills), Behind Wire (binds enemies to their backs), Sword Wire: Spider's Poison Web (effect unknown). | S |
| Needle skills: Needle Shot fires needles at targets; Jumping Needle teleports the user to a spot or a person where a special needle was planted (S'yne reached Naofumi this way in volume 11). | S |
| Spider Net traps a target in a spider-shaped net of thread; Scissor Cut is a powerful slash (strong enough to cut one of Tulina's nine tails); Ariadne's Thread auto-maps explored areas and can teleport selected targets out of a labyrinth. | S: https://shield-hero.fandom.com/wiki/S'yne_Lokk/Powers_and_Abilities |
| Sewing Set 0: no damage, cooldown or SP cost, effective against illegitimate powers. The unlock conditions of the skills are marked unknown ("?") on the wiki. | S: https://shield-hero.fandom.com/wiki/0_Series |
| The needle-and-spool look, the stitch marks and beacons, the planted needle, the Stitch Mend, the Stitched Barrier, every number, the form names "Silk Thread Set" and "Spider Wire Set" as tiers. | D |
| The form name "Ariadne's Thread" for the top form. | S (the skill's name, reused as a form name) |

**Not built from canon:** Fire Paralysis Wire, Behind Wire and Sword Wire (variants of the binding that would duplicate it), Skill Seal (Bind Wire already seals skills),
the Sewing Set 0 (the Musical Instrument and the Harpoon carry the 0-series idea in this build).

## 3. How it plays: needles that stay where they were put

1. **The volley.** `sewing_set.needle_shot` fires three needles in a very narrow fan. Each foe struck takes 3 per needle and carries a **stitch mark** for 8 seconds (12 with Fine Stitch).
   With nothing struck, the middle needle goes into the **ally** you look at (a **beacon** for 20 seconds) or, if it hits a wall, **plants itself** in the ground (20 seconds).
   The marks table holds eight creatures at most; a new one pushes the oldest out.
2. **What the marks do.** A stitch mark makes the **Scissor Cut** 30 percent deeper (and is used up by it). A beacon is a place to go: the **Jumping Needle** jumps to the nearest
   beaconed ally or to the planted needle (48 blocks), the **Stitch Mend** reaches beaconed allies even 30 blocks away, and **Ariadne's Thread** draws them to the wielder.
   A faint thread is drawn from the wielder to each beacon while the set is out.
3. **Holds.** Bind Wire (one foe, seals its skills) and Spider Net (three foes, held almost fast) sit behind the shared control lock; the Stitched Barrier is a seam across the ground
   that catches up to six hostile shots and slows and pricks the foes that touch it.

## 4. The forms tree

Items to absorb are listed per form; absorbed items are kept, never consumed (design.md section 6). All forms use the base look; there is no curse series.

| form (id) | tier | parents | needs | mastery | stats | use / sneak / key1 / key2 / key3 | passives |
|---|---|---|---|---|---|---|---|
| Sewing Set (`base`) | 0 | - | nothing (always learned) | 0 | movement_speed 0.01, luck 0.5 | `sewing_set.needle_shot` / - / `sewing_set.stitch_mend` / `sewing_set.bind_wire` / - | - |
| Silk Thread Set (`silk_thread_set`) | 1 | base | #minecraft:wool x32, minecraft:string x48, minecraft:shears x4 | 80 | movement_speed 0.02, luck 1 | `sewing_set.needle_shot` / `sewing_set.stitch_mend` / `sewing_set.bind_wire` / `sewing_set.jumping_needle` / - | fine_stitch |
| Spider Wire Set (`spider_wire_set`) | 2 | silk_thread_set | minecraft:cobweb x24, minecraft:spider_eye x32, minecraft:fermented_spider_eye x8 | 160 | movement_speed 0.03, armor 1, luck 1 | `sewing_set.needle_shot` / `sewing_set.scissor_cut` / `sewing_set.bind_wire` / `sewing_set.spider_net` / `sewing_set.stitched_barrier` | fine_stitch |
| Ariadne's Thread (`ariadnes_thread`) | 3 | spider_wire_set | minecraft:amethyst_shard x48, minecraft:ender_pearl x16, minecraft:compass x8, tensura:high_quality_magic_crystal x6 | 280 | movement_speed 0.05, max_health 4, armor 1, luck 1.5 | `sewing_set.needle_shot` / `sewing_set.stitch_mend` / `sewing_set.jumping_needle` / `sewing_set.scissor_cut` / `sewing_set.ariadnes_thread` | fine_stitch |

Notes on the tree: the Silk Thread Set (wool, string, shears) gives Fine Stitch and the Jumping Needle; the Spider Wire Set (webs and spider eyes) is the offensive branch (Scissor Cut,
Spider Net, Stitched Barrier); Ariadne's Thread (amethyst, ender pearls, compasses, a high-quality magic crystal) is the top form and carries the ultimate. The tree is a line.

## 5. The techniques

### 5.1 The budget (design.md section 5), straight from `SewingNumbers`

Raw damage is before the bond-level factor (1.0 to 2.2) and the balance law. "Most on one target" counts every hit of the technique.

| technique | kind | raw per hit | most on one target | targets | cooldown | cost | heal |
|---|---|---|---|---|---|---|---|
| `sewing_set.needle_shot` | ordinary | 9 | 9 | 3 | 60 ticks (3 s) | 1% | - |
| `sewing_set.bind_wire` | utility | 3 | 3 | 1 | 240 ticks (12 s) | 2% | - |
| `sewing_set.jumping_needle` | utility | 0 | 0 | 0 | 140 ticks (7 s) | 2% | - |
| `sewing_set.stitch_mend` | utility | 0 | 0 | 0 | 200 ticks (10 s) | 2% | 8% |
| `sewing_set.scissor_cut` | heavy | 13 | 13 | 5 | 140 ticks (7 s) | 2% | - |
| `sewing_set.stitched_barrier` | utility | 3 | 3 | 8 | 180 ticks (9 s) | 2% | - |
| `sewing_set.spider_net` | utility | 4 | 4 | 3 | 260 ticks (13 s) | 2.5% | - |
| `sewing_set.ariadnes_thread` | ultimate | 0 | 0 | 4 | 700 ticks (35 s) | 6% | - |

`SewingNumbersTest` hands exactly these rows to `Budget.violations`. The Scissor Cut's raw is 13 because it is 10 with 30 percent more on a marked foe.

### 5.2 Technique by technique

**`sewing_set.needle_shot` Needle Shot** (ordinary, 3 s, 1 percent)
* What: three needles, 4 degrees apart, 20 blocks; 3 for each needle that strikes (9 on one foe at most), up to three foes, each marked. See section 3 for the two fall-backs.
  Refused (and free) when the needles find neither a foe, an ally nor a wall.
* Tell: three thin violet lines from the hand.
* Counter: sidestep (the fan is narrow); the mark is only a set-up for the Scissor Cut.

**`sewing_set.bind_wire` Bind Wire** (utility, 12 s, 2 percent)
* What: the foe in the crosshair (16 blocks) takes 3 and is held fast (Slowness X, the law's control time, 3 s base) and its skills are sealed for that time: a player's grimoire through the
  mod's existing `nusmp_sealed_until` mark, a creature through Tensura's silence effect where the game has it. The foe is stitch-marked. Shared control lock.
* Tell: a thread line from the wielder and a cocoon of thread round the foe for the length of the hold.
* Counter: cure slowness; stay beyond 16 blocks; the lock means it cannot be chained with the weapon's other holds.

**`sewing_set.jumping_needle` Jumping Needle** (utility, 7 s, 2 percent)
* What: jump to the nearest beaconed ally within 48 blocks, or else to the planted needle (a safe spot beside it; the needle is then used up). With neither, it plants a needle where the
  wielder stands (cast again to jump back). Refused (and free) when the planted needle is more than 48 blocks away or there is no safe place beside the target.
* Tell: a thread line between the two spots, a burst of violet dust at the landing and an enderman-like sound.
* Counter: it is movement, not an attack.

**`sewing_set.stitch_mend` Stitch Mend** (utility, 10 s, 2 percent, heals 8 percent)
* What: up to four allies at most: the allies within 6 blocks of the wielder (the wielder included) and the beaconed allies within 30 blocks. Each mends 8 percent of their maximum health
  (`HeroCtx.heal`, so the law's caps apply) and sheds one harmful effect. Refused (and free) when nobody is hurt or afflicted.
* Tell: a mending thread from the wielder to each healed ally.
* Counter: none; it is support.

**`sewing_set.scissor_cut` Scissor Cut** (heavy, 7 s, 2 percent)
* What: the spot you look at (a foe within 10 blocks, else 3.5 blocks ahead) is the centre. After 8 ticks, every foe then within 3 blocks of it (five at most) takes 10, or 13 if it carries a
  stitch mark (the mark is used up). It also snips the wielder free of slowness. Refused (and free) when no foe stands there at the cast.
* Tell: two long crossed lines drawn over the spot for 0.4 seconds, and the sound of shears.
* Counter: step out of the crossing before it closes (the blow lands on whoever stands in it when it closes, not on whoever stood there at the start).

**`sewing_set.stitched_barrier` Stitched Barrier** (utility, 9 s, 2 percent)
* What: a seam of stitches 9 blocks wide, 3.5 blocks ahead of the wielder, across the ground for 120 ticks. Up to six hostile projectile entities that fly into it are caught and removed; a foe
  within 1 block of the seam is slowed (Slowness II for 2 seconds, refreshed) and pricked once for 3 (up to eight foes).
* Tell: three rows of stitches drawn across the ground for the whole six seconds.
* Counter: walk round it (it is a line, not a wall; it stops no one's movement by itself); instant spells and melee pass.

**`sewing_set.spider_net` Spider Net** (utility, 13 s, 2.5 percent)
* What: the foe in the crosshair (14 blocks) and up to two more within 2.8 blocks of it are held almost fast (Slowness V for the law's control time, 4 s base), take 4 and are stitch-marked.
  Shared control lock.
* Tell: a web on the ground under the target for the length of the hold, and a thread line from the hand.
* Counter: cure slowness; do not clump.

**`sewing_set.ariadnes_thread` Ariadne's Thread** (ultimate, 35 s, 6 percent)
* What: no damage. After 30 ticks of gathering, up to four allies within 40 blocks (the beaconed ones first, then party members and the wielder's own pets or summons within 40 blocks; ones already within 4 blocks of the
  wielder are left) are drawn to a safe spot beside the wielder, and every foe within 24 blocks (twelve at most) glows for 8 seconds. Refused (and free) when there is nobody to draw and no foe to light.
* Tell: a ring at the wielder's feet and a thread running to each ally to be drawn, for 1.5 seconds before they arrive.
* Counter: nothing to answer (no damage); a foe that glows is easier to find, which is the point. Allies move only as far as a safe spot the wielder could stand on, in the same dimension.

## 6. The passive

* **`fine_stitch`** (Silk Thread Set and every form after it): a stitch mark in a foe lasts 4 seconds longer (12 instead of 8), on every technique that leaves one.

## 7. Curse series

None. The brief gives curse series only to the Mirror (Envy) and the Chain (Sloth).

## 8. How it differs from what the mod already has

The mod already has **Thread Magic** (`book/ThreadBook`, `book/ThreadSpells`, `thread/**`): the red thread (hard bind), Rouge (the red thread of fate: a set of protected people chosen by name), Arachne's
Web, Dancing Doll, Thread Magic: Mending, and Vanessa's pages (Arresting Dark Weave, Dancing Doll: Recall, Thread Trap Generation with a trap entity, Reinforcement and Mana Skin). The
Sewing Set shares no code with it (only the VFX shapes `THREAD_*` and the helper `thread.SafeSpot` for finding a place to stand). The concepts overlap because canon names overlap; what is different:

| Thread Magic (grimoire) | Vassal Sewing Set (this weapon) |
|---|---|
| thread binds and protects people the caster names (Rouge: fate, a protected set) | a needle marks whoever it strikes, and the mark EXPIRES (8 or 20 seconds); nobody is protected by name |
| Red Thread: one hard hold; Arachne's Web: a web zone | Bind Wire: one hold that also seals skills; Spider Net: three foes held almost fast; both behind the same shared lock |
| Mending: a heal spell | Stitch Mend: 8 percent, four people, and it reaches the beaconed ones from afar |
| Thread Trap: an entity that waits | the Stitched Barrier: a line of stitches that catches shots for six seconds, no entity |
| Reinforcement / Mana Skin: a buff | none |
| none | Scissor Cut (marks make it deeper), Jumping Needle (teleport to a beacon), Ariadne's Thread (draws allies in, lights foes up) |

## 9. Engravings (the owner's rule 6) and why

```java
Map.entry("vassal_sewing_set", List.of(e("severance", 1), e("restoration", 1), e("magic_interference", 1))), // a needle that cuts clean, mends, and seals
```

* `severance` 1: the Scissor Cut (canon: it cut one of Tulina's nine tails) and the needle's fine point.
* `restoration` 1: the Stitch Mend and the sewing idea of closing wounds.
* `magic_interference` 1: Bind Wire stops its target from using skills.

## 10. Code map: what each file is for

Config keys: none (a weapon keeps its numbers in code, in a pure table with a test). Commands: none. Quest keys: none. Lang: the keys `hero.skill.sewing_set.<name>.name` and `.desc` for each skill and `hero.passive.sewing_set.<id>` (and `.desc`) for each passive are in the fragment `hvd.lang.json`, which the lead merges into `en_us.json`.

All under `heroweapon/weapon/sewingset/` (every class has a "Why:" Javadoc).

| File | Why it exists | Tests |
|---|---|---|
| `SewingSetLogic` | the `HeroLogic`: lists the eight skills from the rows, draws the threads to the beacons in `onTick`, registers the self-test `hero.weapon.sewingset` | self-test, `SewingFormsTest` (3) |
| `SewingSkills` | the bodies of the techniques and the beacon threads | self-test |
| `SewingNumbers` | every figure and the small rules (Scissor Cut damage, volley damage, mark length) as pure code | `SewingNumbersTest` (8) |
| `NeedleBoard` | the marks (foe and ally), the planted needle, their expiry and the draw beat, as pure code | `NeedleBoardTest` (8) |
| `vassal_sewing_set.json` | the forms file | `SewingFormsTest` runs the shared checker on the shipped file |

## 11. Server rules, checked

* Damage: all through `HeroCtx.hit`. Heals: only `HeroCtx.heal` (Stitch Mend 8 percent). Mana: the dispatcher pays; nothing extra is paid inside.
* Control: Bind Wire and Spider Net take `HeroCtx.control()` before holding anyone. The Stitched Barrier's slowing (Slowness II, 2 seconds) is a soft slow, not control.
* **Moving other creatures.** Ariadne's Thread teleports up to four allies (`Foes.isCompanion`: the wielder's party and team, and their own pets and summons; the beaconed ones are allies the wielder chose by looking at them).
  A stranger who merely may not be harmed is never drawn unless the wielder put a needle in them. It never moves a foe.
* No entity is spawned. No block is changed. Longest task: the Stitched Barrier's 6 seconds.
* Targets: from `Foes`; at most 12 (the glowing foes), 8 in any damaging technique. The marks table is bounded (8) and expires.
* State: `PlayerState` for the needles (cleared on logout and server stop). Effects: `VfxSpawn` (`THREAD_LINE`, `THREAD_STRINGS`, `THREAD_COCOON`, `THREAD_WEB`, `THREAD_TRAP_LINE`, `SEAL_WOUND`, `SLASH_WAVE`),
  `Tells` and sounds. No vanilla particles.

## 12. Unverified, assumed, and to be checked by the lead

* **Nothing has run in the game**: type-checked and unit-tested only; the self-test `hero.weapon.sewingset` has not run.
* Tensura's silence effect is looked up by the id `tensura:silence`; if the id is different the creature is only slowed, not silenced (a player is sealed through the mod's own mark either way).
* Ariadne's Thread moves party members and pets without asking. If the owner wants consent, the cast should be limited to the beaconed allies; this is a one-line change in `SewingSkills.ariadnesThread`.
* `Moves.landingNear` and `thread.SafeSpot` choose the landing spots; whether they handle every cave and water case was not tested.
* The Stitched Barrier removes projectile entities with `discard()`; a modded projectile with unusual lifecycle code might not like that. Instant (hitscan) spells pass through.
* Item ids: every vanilla id the forms use was checked as a field of `net.minecraft.world.item.Items` in the API dump (the registry name is the field in lower case), the tag `minecraft:wool` is a vanilla item tag, and `tensura:high_quality_magic_crystal` is used elsewhere in this repository and is a field of `TensuraMaterialItems` in the Tensura dump. None of this was seen in the running game; CI's `hero.forms` and this weapon's self-test resolve them against the real registry.
* The figures are first guesses and will need playtesting.

## 13. Sources

* https://shield-hero.fandom.com/wiki/Vassal_Sewing_Set (summary)
* https://shield-hero.fandom.com/wiki/S'yne_Lokk/Powers_and_Abilities, https://shield-hero.fandom.com/wiki/R'yne, https://shield-hero.fandom.com/wiki/0_Series (summaries)
* https://shield-hero.fandom.com/wiki/Legendary_Weapons/Power-up_Methods (named in search results; the power-up method of the Sewing Set is unknown there)
* In the repository: `book/ThreadBook.java`, `book/ThreadSpells.java`, `thread/**` (compared, `thread.SafeSpot` reused), `docs/heroweapon/design.md`, `item/WeaponEngravings.java`.
