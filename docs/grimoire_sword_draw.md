# Grimoire Sword Draw (0.52)

When Anti-Magic or Sword Magic summons a weapon, the player draws it out of the grimoire: the book opens at the hip, the main arm
reaches into its pages, grips the hilt and pulls the sword up. 24 ticks (1.2 s). The weapon lands in the main hand at **tick 12**,
the tick the hilt leaves the pages.

## What it replaces (new vs replacement)
- **Replacement:** the old "the sword just appears in your inventory" for Sword Magic's *Demon-Dweller Sword* / *Demon-Destroyer Sword*
  pages and for the Anti-Magic awakenings (the Demon-Slayer when the skill is learned; Demon-Dweller, Demon-Destroyer and
  Demon-Slasher on mastery or awakening of the Lord). Same items, same binding, same 60 s for the Sword Magic swords.
- **New:** the pose, the first-person hand motion, the particles and sounds.
- Not changed: how swords are earned, who owns them, `MagicWeaponItem.bound / summoned`.

## 1. Keyframe timeline

Angles are degrees in the player model's own rotations (x negative lifts an arm forward and up; y turns it across the body; z swings
it out from the body). Between two keyframes the motion uses the ease of the *later* row. The pose is blended in over 3 ticks and out
over ticks 20 to 24, so it never pops.

**Phases:** 0-4 activation (0.0-0.2 s), 4-10 reach (0.2-0.5 s), 10-18 extraction (0.5-0.9 s), 18-24 equip and stance (0.9-1.2 s).

| Tick | Time | Main (right) arm x / y / z | Ease | Off (left) arm x / y / z | Body x / y / z | Head offset x / y / z | What happens |
|---|---|---|---|---|---|---|---|
| 0 | 0.00 s | 0 / 0 / 0 | in-out quad | 0 / 0 / 0 | 0 / 0 / 0 | 0 / 0 / 0 | grimoire opens and glows at the hip, page sound |
| 4 | 0.20 s | -15 / 0 / 14 | in-out quad | -10 / 0 / -12 | (6: 10 / 8 / 3) | 22 / 14 / 0 | arm leaves the side; the eyes drop to the book |
| 6 | 0.30 s | | | | 10 / 8 / 3 | | torso bends toward the hip; more glow |
| 8 | 0.40 s | -40 / -6 / 20 | in-out quad | | | | the hand reaches down into the pages |
| 10 | 0.50 s | | | -30 / 10 / -18 | | | off hand braces at the chest |
| 12 | 0.60 s | -48 / -8 / 18 | in-out quad | | 14 / 10 / 4 | 20 / 12 / 0 | **the hilt is gripped; the weapon is placed in the hand**, burst VFX and the draw sound |
| 14 | 0.70 s | -70 / -6 / 12 | out cubic | | | | the pull begins |
| 16 | 0.80 s | | | | 4 / 0 / 0 | | torso straightens |
| 18 | 0.90 s | -125 / -8 / 6 | out cubic | -45 / 20 / -10 | | -4 / 0 / 0 | the blade is lifted clear of the book; the gaze follows it up |
| 20 | 1.00 s | | | | -5 / -6 / -2 | | a small lean back with the weight of the sword; blend-out begins |
| 22 | 1.10 s | -95 / 6 / 8 | in-out quad | | | | the sword swings down toward guard |
| 24 | 1.20 s | -70 / 8 / 8 | in-out quad | -25 / 25 / -8 | 0 / 0 / 0 | 0 / 0 / 0 | combat stance; the grimoire closes or idles; the pose is gone |

The head offset is **added** to the player's own head rotation (so the head keeps tracking); arms and body blend toward the keyframes.

**First person** (`RenderHandEvent`, main hand only): `down = easeInOut(t/8) - easeOutCubic((t-10)/10)`. The hand moves
`(0.12, -0.65, 0.10) x down`, pitched `28 degrees x down`, rolled `-14 degrees x down`: it sinks to the book over 8 ticks and rises
with the sword from tick 10 to 20.

## 2. VFX and sound sync

| Tick | Anti-Magic | Sword Magic |
|---|---|---|
| 0 | server: dark `KOTO_SHATTER` at the hip, low page-turn. client: black and crimson dust wisps rise from the book | server: pale gold `KOTO_SHATTER`, high page-turn. client: end-rod light and enchant runes rise |
| 6 | server: deep crimson glow at the hip | server: pale blue glow |
| 12 (hilt leaves) | server: crimson `KOTO_SHATTER` burst, **anvil + low wither crack** (heavy metal). client: ring of black smoke and an explosion puff | server: white burst, **sweep + amethyst chime** (blade-draw). client: ring of end-rod sparks |
| 14-24 | the weapon's own effects take over | the weapon's own effects take over |

The dimensional void (`rendertype_demon_void`) is on the Demon-Slayer's blade itself (WeaponRenderer / DemonSlayerRenderer), which is in
the hand from tick 12 on; the draw does not render a separate void portal at the book.

## 3. Implementation

| Part | File |
|---|---|
| server clock, hand-over, VFX and sounds | `anim/SwordDraw.java` (`draw(player, style, items...)`) |
| packet to everyone who sees the player | `anim/SwordDrawPayload.java` (registered in `NUSMP`) |
| keyframes, pose, first-person hand, particles | `client/SwordDrawClient.java` |
| applies the pose after vanilla's `PlayerModel.setupAnim` | `mixin/client/PlayerModelMixin.java` (optional, `require = 0`) |
| call sites | `CanonSpells.summonSword`, `AntiMagicBook.awaken / onLearnSkill`, `AntiMagicLordSkill.onLearnSkill / onSkillMastered` |

```java
// server: anywhere a weapon is summoned
SwordDraw.draw(player, SwordDraw.ANTI_MAGIC, MagicWeaponItem.bound(NUItems.DEMON_SLAYER.get(), player));
SwordDraw.draw(player, SwordDraw.SWORD_MAGIC, MagicWeaponItem.summoned(NUItems.LICHT_DWELLER.get(), player, 1200));
```
The first item goes into the main hand if it is empty at tick 12, everything else (and a full hand) into the inventory.

No animation library is needed: the mod already ships mixins, so the pose is applied directly to the model's arm, body and head parts
(and copied to the sleeves, jacket and hat so the outer layer follows).

## 4. Not verified
Everything here was written without running the game. Check in game: that the arm sits right at the book (the numbers above are the
first guess to tune), that the first-person hand is not clipped by the screen edge, and that the mixin applied (if the game log says
the PlayerModel injection was skipped, the pose is missing but nothing else breaks).
