# The Legendary Bow (`legendary_bow`, 1.62.0)

Why this page exists: it is the one place that says what the Legendary Bow is in canon, how it plays here, what every number is, what it costs the wielder
and what the art builder should draw. The binding contract of the whole job is `docs/heroweapon/design.md`; this page only adds the Bow's own decisions.
Section 1 (the look) was written first so the art builder could start; everything after it was written when the code was done.

Reading the numbers: every damage figure on this page is **raw** damage, before the level factor of the bond (1.0 at level 0 to 2.2 at level 100) and before
the balance law (existence points, the damage game rule, the player and boss caps). Distances are blocks, delays are ticks (20 a second). A cooldown here is
the number before the bond level shortens it (down to three quarters at level 100). Nothing on this page was ever run in a game; section 13 lists exactly what
was and was not checked.

## 1. The look, for the art builder (written first)

The Legendary Bow is the weapon of the Bow Hero, Itsuki Kawasumi: the long-range weapon of the four Holy Weapons. It must read as a noble, elegant
longbow at the base look, and as the same bow made proud and monstrous at the curse look. It makes its own arrows of light, so it has no arrow rest
for a real arrow and no quiver; the nocked arrow is a thing of light.

**Size class.** `bow` of `assets/nusmp/weapon_classes.json` (1.3 to 1.6 blocks). Aim for 1.6 blocks tall (the top of the range). The grip is the
class grip (0.5, the middle of the riser). The bow is held upright, exactly as the wave 23 bows (`divine_mercy`, `the_dragons_spirit`, `embrasure`
are the nearest examples); the string side faces the archer, the limbs curve forward (mesh -x).

**Base look (named parts, 10 to 14).**

| part | what | size (blocks) | colour / material |
|---|---|---|---|
| `riser` | the central grip block: a slim, slightly bulged handle, swelling into two small shoulders where the limbs begin | 0.34 tall, 0.08 deep | pale ivory ash wood, lacquered |
| `grip_wrap` | leather wrap on the middle of the riser (where the hand holds) | 0.16 tall | deep brown `#5A3B24` |
| `upper_limb`, `lower_limb` | the two long limbs, each a smooth recurve: they sweep away from the archer (forward, -x), then the last quarter curls back toward the archer, tapering from 0.06 to 0.03 thick | 0.65 long each | white-gold: ivory `#F2ECDD` with a gold edge strip `#E0B440` |
| `limb_cap_upper`, `limb_cap_lower` | small gold tip caps with a string notch | 0.07 long | gold |
| `limb_inlay_upper`, `limb_inlay_lower` | a thin line of gold inlay down each limb | 0.5 long | gold, carries a faint glow |
| `wing_left`, `wing_right` | two small leaf-shaped gold fins rising from the riser shoulders along the limbs, like the folded wings of a bird (the series' bows are drawn with winged ornaments; from memory) | 0.22 long | gold, shade dark in the grooves |
| `gem` | THE GEM: a yellow cabochon set into the front (-x) face of the riser, above the grip, framed by a thin gold bezel | 0.07 across | yellow `#F6C61B`, glow `#FFE066` |
| `string` | the bowstring: a single thin straight line from tip to tip on the archer's side (a very thin quad) | 1.5 long, 0.008 wide | pale cream `#FFF3C2`; on the glow skin a soft yellow-white line |
| `arrow_rest` | a tiny notch above the grip where the arrow of light sits | 0.04 | gold |

Silhouette: a tall, slender, gently double-curved bow with a small winged ornament at the riser and a bright jewel. The limbs are the long clean
lines; the gem is the one bright point. Nothing hangs below the lower limb.

**Draw states (a stretch goal; tell the lead whether you made them).** `legendary_bow_pull1`, `_pull2`, `_pull3` as the relic bows have them
(`tools/relics/ranged_meshes.py`): the string pulled back in three steps with a short arrow of light nocked from the gem outwards. If the renderer
for hero weapons does not use the stages yet, they are simply not drawn; nothing in the logic depends on them. The rest mesh must read well alone.

**Glow skin (additive).** The gem (strongest, `#FFE066`, a soft halo), the string (soft yellow-white), the limb inlays (faint), the fins' edges (very
faint). Everything else black on the glow skin.

**Curse look (Pride), `legendary_bow_curse`.** Same height (1.6), same grip and riser position. The sin colour is `Sin.PRIDE` `#7A3FB5`.

| part | change |
|---|---|
| `upper_limb`, `lower_limb` | the ivory turns black lacquer `#1E1626`; the gold edge strip becomes violet-edged iron `#7A3FB5`; the tips curl into hooked talons (a short, sharp hook at each tip) |
| `crown` (new) | a crown-like crest rising from the top of the riser between the limb roots: five tall pointed spikes in a fan (the middle one tallest, 0.18), black iron with violet edges |
| `wing_left`, `wing_right` | the fins grow into larger, hooked wings with serrated edges (twice the length) |
| `string` | thicker (0.015), violet-white energy line; on the glow skin a strong violet glow |
| `gem` | the gem turns royal purple `#9B59FF` with a gold bezel ring, with a ring of tiny spikes around it (the "crown" echo), the halo larger |
| `grip_wrap` | black with a violet stitch |

Glow skin of the curse look: the gem, the string (strong), the crown's spike tips, the talons' tips, the limb edge strips (faint).

**Forms and looks.** The engine only distinguishes `base` and `curse` looks; ordinary forms do not change the mesh.

**Effect colours the game uses (not a mesh matter):** yellow-gold `0xFFFFD84A`, white-gold `0xFFFFE8A0`; the Pride forms use `0xFF7A3FB5`.

## 2. Canon, and what is design

Every canon claim below comes from **search summaries of the fandom wiki** (the pages themselves could not be opened from the build machine: the fetch
fails with "host not found"). They are fan-compiled and disagree between the light novel and the web novel. **[S]** = from a search summary, **[M]** = from
memory of the series (unchecked), **[D]** = this mod's design, not canon.

| fact | status | source |
|---|---|---|
| The Legendary Bow is the Cardinal weapon of Itsuki Kawasumi, the Bow Hero. A special yellow jewel sits in the middle of the weapon and stays through its transformations. | [S] | https://shield-hero.fandom.com/wiki/Legendary_Bow and https://shield-hero.fandom.com/wiki/Legendary_Bow_(Web_Novel) |
| The light novel's form table starts at the **Small Bow**. | [S] | https://shield-hero.fandom.com/wiki/Legendary_Bow |
| **Meteor Bow** (also Shooting Star Bow): fires a shot that leaves a trail of stars behind the arrow. In the anime Itsuki's "Meteor Shot" deals no damage. | [S] | https://shield-hero.fandom.com/wiki/Itsuki_Kawasumi/Powers_and_Abilities |
| **Arrow Rain**: shoots into the air so that arrows fall on the target. **Arrow Squall**: an arrow that splits into a barrage. **Spread Strafing** / **Spread Arrow**: several arrows in rapid fire. | [S] | same |
| **Eagle Piercing Shot**: a single defence-ignoring arrow shaped like an eagle. **Shadow Bind**: paralyses the target when the arrow lands in its shadow. **Bind Arrow**: stitches the target to the ground (or pins it to the wall behind). **Bird Hunting**: the arrow splits into several shafts that all strike the target at once. | [S] | https://shield-hero.fandom.com/wiki/Legendary_Bow |
| The web novel's form list adds **Earth Pinning Shot**, **Second Arrow**, **Frozen Rain** and **Arrow Shower** (several with only placeholder text). | [S] | https://shield-hero.fandom.com/wiki/Legendary_Bow_(Web_Novel) |
| Itsuki's ESPer ability **Accuracy** makes hitting things much easier and lets him snipe from afar. | [S] | https://shield-hero.fandom.com/wiki/ESPer |
| "**Hundred Arrows**" and a damaging "Meteor Shot" were NOT found in the results. Neither name is used for a technique here. | [S] | search results |
| The **Pride Series**: the **Bow of Pride** gives the skills **Law Fanatic** (a sinister aura condenses round the user into a suit of armour that boosts stats) and **Bull of Phalaris** (a huge brass bull opens and closes round the target and roasts it with flames in its belly) and the special effect **Brainwashing** (anyone hit by its arrows falls under Itsuki's control). Web novel only: **Autocracy Justice** (countless orbs of light leave his body, take the shape of lions and rain down), at the cost of losing the sense of self and being unable to refuse orders. Itsuki unlocks the series after Mald, Malty and their group instigate him. | [S] | https://shield-hero.fandom.com/wiki/Pride_Series_(Web_Novel) and https://shield-hero.fandom.com/wiki/Curse_Series/Weapon_List |
| The bow is drawn with winged ornaments at the riser. | [M] | (used for the look only; unverified) |

**Where the Bow departs from canon (all [D]).** (1) The bow makes its own arrows of light from the wielder's magicules and needs none in the pack (the owner's
text; a real arrow is never used, so no tipped-arrow or inventory case exists). (2) Brainwashing is a bounded version: Pride's Command turns the first monster
an armed arrow hits against its own side (never a boss or a player, shared control lock). (3) The form names `bind_arrow`, `bird_hunting`, `meteor_bow`,
`arrow_shower`, `frozen_rain` are the wiki's; `hunters_bow`, `scouts_bow`, `eagle_piercer` and `sagittarius` are inventions of this mod; `bow_of_pride_1` to
`_3` follow the wiki's Bow of Pride and Bow of Pride III. (4) Nothing here changes a block, spawns an entity or loads a chunk.

## 3. How it plays

The Bow is the **range and utility** weapon of the four; its damage per second is kept low on purpose (section 7), and it has no `use` slot (the use key is the draw).

* **The draw.** `onRelease(c, chargeTicks)` is the shot. The share of a full draw follows the vanilla bow's curve (`(c squared + 2c) / 3` with `c` = ticks / 20; 20 ticks is
  full, 16 with Quickdraw). A share under 10 percent is a slip of the string and shoots nothing; two shots cannot leave the string within 4 ticks.
* **The arrow of light.** It costs 0.4 percent of the maximum magicules at a full draw (less for less) and is a line that stops at the first wall: it flies 16 blocks
  at the weakest draw and 48 at a full one (Farsight: 30 percent farther, never past 64), is 0.45 wide (Pride: 1.0) and deals `7 x share x (1 + 0.04 x the
  form's attack stat)` raw, times 1.15 under Law Fanatic and 1.2 with Pride Arrows, never more than 12. It strikes the first foe in its way (the first two
  with Piercing Arrows, the second for 70 percent); Eagle Eye adds 25 percent beyond 24 blocks.
* **The techniques on the sneak and hero keys.** Six of them ARM a change to the NEXT drawn shot for 10 seconds (Eagle Piercing Shot, Bind Arrow, Marking
  Arrow, Burst Arrow, Seeker Arrow, Second Arrow; Pride's Command is the seventh). A change asks for a minimum draw (sniper 90 percent, Bind and Burst and
  Second 50, Seeker and Command 40, Marking 30); a weaker draw looses a plain arrow and leaves the arming for a fuller one. Their cost is paid when they are armed.
  The rest act at once: the rains and the meteor (artillery), the Squall and Bird Hunting (multi-shot), the Backstep Shot (a retreat), Law Fanatic and the Bull of
  Phalaris (Pride).
* **Mastery.** A foe hit by an arrow counts as a hit of the weapon: one mastery point for the form and one point of weapon experience (the core's rules may refuse the
  experience while two Holy wielders stand close).

## 4. The forms tree

Ten ordinary forms (tiers 0 to 5) and the three Pride forms. A form is learned when all its parents are learned and the gem has absorbed the listed
materials (absorbed items are never consumed by unlocking). `mastery` is the points (a hit, two for a technique) that finish the form and grant its permanent bonus.
The stats apply while the weapon is summoned; for a bow the attack stat is draw strength (4 percent more on the plain arrow per point).

| form (`id`) | tier | parents | the gem must absorb | mastery | stats while summoned | permanent bonus | techniques by slot | passives |
|---|---|---|---|---|---|---|---|---|
| **Small Bow** (`base`) | 0 | none | nothing (always learned) | 0 | none | none | sneak: Marking Arrow<br>key1: Backstep Shot | none |
| **Hunter's Bow** (`hunters_bow`) | 1 | Small Bow | 20 string, 20 feather, 16 bone | 60 | attack_damage +1, movement_speed +0.02 | movement_speed +0.005 | sneak: Bind Arrow<br>key1: Marking Arrow<br>key2: Backstep Shot | Quickdraw |
| **Scout's Bow** (`scouts_bow`) | 1 | Small Bow | 24 leather, 12 rabbit hide, 16 copper ingot | 60 | attack_damage +0.5, movement_speed +0.03, luck +0.5 | luck +0.25 | sneak: Eagle Piercing Shot<br>key1: Marking Arrow<br>key2: Backstep Shot | Farsight |
| **Bind Arrow** (`bind_arrow`) | 2 | Hunter's Bow | 24 cobweb, 32 slime ball, 24 spider eye | 100 | attack_damage +2, movement_speed +0.03 | attack_damage +0.5 | sneak: Bind Arrow<br>key1: Arrow Squall<br>key2: Marking Arrow<br>key3: Backstep Shot | Quickdraw, Piercing Arrows |
| **Bird Hunting** (`bird_hunting`) | 2 | Scout's Bow | 16 phantom membrane, 48 feather, 6 rabbit foot | 100 | attack_damage +2, movement_speed +0.04, luck +1 | luck +0.25 | sneak: Eagle Piercing Shot<br>key1: Bird Hunting<br>key2: Seeker Arrow<br>key3: Backstep Shot | Farsight, Piercing Arrows |
| **Arrow Shower** (`arrow_shower`) | 3 | Bind Arrow | 64 gunpowder, 64 arrow, 24 Tensura magic stone | 160 | attack_damage +3, armor +1 | armor +0.5 | sneak: Second Arrow<br>key1: Arrow Rain<br>key2: Arrow Squall<br>key3: Burst Arrow | Quickdraw, Piercing Arrows |
| **Meteor Bow** (`meteor_bow`) | 3 | Bird Hunting | 64 glowstone dust, 16 ender pearl, 32 amethyst shard | 160 | attack_damage +3.5, luck +1, movement_speed +0.03 | luck +0.25 | sneak: Eagle Piercing Shot<br>key1: Meteor Bow<br>key2: Seeker Arrow<br>key3: Burst Arrow | Farsight, Quickdraw |
| **Frozen Rain** (`frozen_rain`) | 4 | Arrow Shower | 32 blue ice, 32 prismarine crystals, 8 Tensura medium quality magic crystal | 240 | attack_damage +4.5, armor +2, knockback_resistance +0.1 | armor +0.5 | sneak: Second Arrow<br>key1: Frozen Rain<br>key2: Arrow Rain<br>key3: Bind Arrow | Quickdraw, Piercing Arrows |
| **Eagle Piercer** (`eagle_piercer`) | 4 | Meteor Bow | 24 diamond, 8 netherite scrap, 8 Tensura high quality magic crystal | 240 | attack_damage +5, movement_speed +0.04, luck +1.5 | luck +0.5 | sneak: Eagle Piercing Shot<br>key1: Meteor Bow<br>key2: Bird Hunting<br>key3: Seeker Arrow | Farsight, Quickdraw, Eagle Eye |
| **Sagittarius** (`sagittarius`) | 5 | Frozen Rain, Eagle Piercer | 4 nether star, 16 dragon breath, 4 Tensura adamantite ingot | 400 | attack_damage +8, movement_speed +0.06, armor +2, max_health +4, luck +2 | max_health +2, luck +0.5 | sneak: Second Arrow<br>key1: Meteor Bow<br>key2: Arrow Rain<br>key3: Starfall Volley | Farsight, Quickdraw, Piercing Arrows, Eagle Eye |
| **Bow of Pride I** (`bow_of_pride_1`)<br>curse, pride, +2 corruption a second | 1 | Small Bow | 40 gold ingot, 24 amethyst shard | 80 | attack_damage +6, movement_speed +0.02 | none (a curse form gives no permanent bonus) | sneak: Law Fanatic<br>key1: Bull of Phalaris<br>key2: Eagle Piercing Shot<br>key3: Backstep Shot | Pride Arrows |
| **Bow of Pride II** (`bow_of_pride_2`)<br>curse, pride, +3 corruption a second | 2 | Bow of Pride I | 8 shulker shell, 8 gold block | 120 | attack_damage +8, armor +1 | none (a curse form gives no permanent bonus) | sneak: Law Fanatic<br>key1: Bull of Phalaris<br>key2: Pride's Command<br>key3: Arrow Rain | Pride Arrows, Piercing Arrows |
| **Bow of Pride III** (`bow_of_pride_3`)<br>curse, pride, +5 corruption a second | 3 | Bow of Pride II | 4 totem of undying, 16 shulker shell, 32 gold block | 160 | attack_damage +10, armor +2, movement_speed +0.04 | none (a curse form gives no permanent bonus) | sneak: Law Fanatic<br>key1: Bull of Phalaris<br>key2: Pride's Command<br>key3: Autocracy Justice | Pride Arrows, Piercing Arrows, Farsight |

```
base
 +- hunters_bow --- bind_arrow --- arrow_shower --- frozen_rain ----+
 |                                                                  +-- sagittarius
 +- scouts_bow ---- bird_hunting -- meteor_bow ----- eagle_piercer --+
 |
 +- bow_of_pride_1 --- bow_of_pride_2 --- bow_of_pride_3     (the Pride series)
```

## 5. The techniques

The numbers come from `BowNumbers.java` (the same constants build the descriptions) and are pinned by `BowNumbersTest` against the owner's budget (see the
Spear page for the budget). The Bow has 17 techniques: 13 for the tree (the Starfall Volley is its ultimate) and 4 for the Pride series (Autocracy Justice is its ultimate). The "most raw
damage to one foe" column adds every hit one cast can land on the same foe.

**Hurt immunity.** The game ignores a second blow on the same creature that is no stronger than the last one for 10 ticks after it. So the arrows of one volley that hit the
same instant (the seven fan arrows) are added up and struck once per foe (`Hits`), and the waves of a rain and the second arrow are spaced at least 10 ticks apart
(`SkillRow.HURT_IMMUNITY`, pinned by `BowNumbersTest`); otherwise only the strongest of them would count.

| technique (`id`) | kind | most raw damage to one foe | most foes | cooldown | cost (of max magicules) |
|---|---|---|---|---|---|
| **Eagle Piercing Shot** (`bow.piercing_shot`) | damage, heavy | 14 | 6 | 140 ticks (7 s) | 2 percent |
| **Bind Arrow** (`bow.net_arrow`) | damage | 5 | 1 | 160 ticks (8 s) | 2 percent |
| **Marking Arrow** (`bow.mark_arrow`) | damage | 3 | 1 | 140 ticks (7 s) | 1.5 percent |
| **Burst Arrow** (`bow.explosive_arrow`) | damage, heavy | 10 | 8 | 120 ticks (6 s) | 2.5 percent |
| **Seeker Arrow** (`bow.tracking_shot`) | damage | 8 | 1 | 120 ticks (6 s) | 2 percent |
| **Second Arrow** (`bow.second_arrow`) | damage | 12 | 2 | 100 ticks (5 s) | 1 percent |
| **Arrow Rain** (`bow.arrow_rain`) | damage | 12 | 12 | 160 ticks (8 s) | 2.5 percent |
| **Arrow Squall** (`bow.arrow_squall`) | damage | 11.9 | 12 | 80 ticks (4 s) | 1.5 percent |
| **Bird Hunting** (`bow.bird_hunting`) | damage | 11 | 1 | 100 ticks (5 s) | 1.5 percent |
| **Backstep Shot** (`bow.retreat_shot`) | utility | 5 | 1 | 100 ticks (5 s) | 1 percent |
| **Meteor Bow** (`bow.meteor_bow`) | damage, heavy | 16 | 12 | 200 ticks (10 s) | 3 percent |
| **Frozen Rain** (`bow.frozen_rain`) | damage | 10 | 12 | 180 ticks (9 s) | 2.5 percent |
| **Starfall Volley** (`bow.starfall_volley`) | ultimate | 26 | 12 | 900 ticks (45 s) | 6 percent |
| **Law Fanatic** (`bow.law_fanatic`) | utility | none | none | 300 ticks (15 s) | 2 percent |
| **Bull of Phalaris** (`bow.bull_of_phalaris`) | damage, heavy | 10 | 1 | 220 ticks (11 s) | 2.5 percent |
| **Pride's Command** (`bow.pride_command`) | damage | 4 | 1 | 240 ticks (12 s) | 2.5 percent |
| **Autocracy Justice** (`bow.autocracy_justice`) | ultimate | 28 | 12 | 900 ticks (45 s) | 6 percent |

### What each technique does, how it shows itself, and how to answer it

* **Eagle Piercing Shot** (`bow.piercing_shot`; 140 ticks, 2 percent). Arms a sniper arrow for 10 seconds. The next draw of 90 percent or more looses an arrow that runs up to 64 blocks, 0.5 wide, through up to 6 foes for 14 each, stopped by the first wall.
  * *Tell:* While it is armed everyone sees a thin red line from the bow along the lane it would take (renewed every 5 ticks), and the action bar tells the wielder; the shot itself is a blade of light.
  * *Answer:* Leave the line or get behind cover as soon as the red line shows; a full draw takes 18 ticks (14 with Quickdraw).
* **Bind Arrow** (`bow.net_arrow`; 160 ticks, 2 percent). Arms a net. The next draw of 50 percent or more hits the first foe for 5 and pins it for up to 1.5 seconds (Slowness VII), through the shared control lock (so once in 12 seconds); a boss is only staggered; at most 3 seconds on a player, 6 on a mob.
  * *Tell:* The arrow streak and a web effect on the foe.
  * *Answer:* Dodge the arrow; the lock makes a second net useless for 12 seconds.
* **Marking Arrow** (`bow.mark_arrow`; 140 ticks, 1.5 percent). Arms a mark. The next draw of 30 percent or more hits the first foe for 3, makes it glow through walls and applies Tensura's Fragility (resistance shredding) for 8 seconds, so the wielder and their friends hurt it more.
  * *Tell:* A compass-lock effect on the foe and a lodestone sound.
  * *Answer:* Break line of sight; it only marks.
* **Burst Arrow** (`bow.explosive_arrow`; 120 ticks, 2.5 percent). Arms a burst. The next draw of 50 percent or more flies up to 40 blocks, sticks in the first foe or wall and bursts 10 ticks later: everything within 3.5 blocks takes 10 (up to 8 foes) and is pushed back. No block is broken.
  * *Tell:* A ring on the ground at the impact for the whole fuse, a flare, a rocket sound.
  * *Answer:* Leave the ring within half a second.
* **Seeker Arrow** (`bow.tracking_shot`; 120 ticks, 2 percent). Arms a seeker. The next draw of 40 percent or more looses a bolt that bends towards the foe nearest the crosshair (within 35 degrees and 40 blocks): speed 1.5, radius 0.6, 40 ticks of life, 8 on the first creature it touches, stopped by walls.
  * *Tell:* A needle of light that follows the bolt all the way.
  * *Answer:* Break line of sight or put something between; the bolt stops at the first creature (friendly or not) it meets.
* **Second Arrow** (`bow.second_arrow`; 100 ticks, 1 percent). Arms a second arrow. The next draw of 50 percent or more looses a plain arrow, and 10 ticks later a second at 60 percent of the first's power (10 ticks is the game's own hurt immunity: sooner, the second blow on the same foe would do nothing); together never more than 12 on one foe.
  * *Tell:* Two visible shots.
  * *Answer:* As for any arrow.
* **Arrow Rain** (`bow.arrow_rain`; 160 ticks, 2.5 percent). Artillery. Shoots into the sky; after 10 ticks arrows fall within 5 blocks of the point aimed at (up to 40 blocks): 6 waves 10 ticks apart, 2 each (12 in all).
  * *Tell:* A ring on the ground from the cast to the end; arrows of light fall in the ring every wave.
  * *Answer:* Leave the ring.
* **Arrow Squall** (`bow.arrow_squall`; 80 ticks, 1.5 percent). Multi-shot. After 6 ticks, 7 arrows in a 40 degree fan up to 20 blocks, each striking the first foe on its line for 1.7 (11.9 at most on one foe).
  * *Tell:* The seven lines are drawn at the cast, then the arrows.
  * *Answer:* Step out of the fan, or stand where only one line reaches you.
* **Bird Hunting** (`bow.bird_hunting`; 100 ticks, 1.5 percent). Multi-shot. Five shafts lock onto the foe under the crosshair (within 36 blocks, 25 degrees) and strike together 10 ticks later for 11 in all.
  * *Tell:* Five streaks converge on the foe, one a tick.
  * *Answer:* Break line of sight in the 10 ticks; the strike checks it again.
* **Backstep Shot** (`bow.retreat_shot`; 100 ticks, 1 percent). Utility. Springs up to 6 blocks backwards to safe ground (refused with no room or while riding) and looses a quick arrow at the nearest foe in front (70 degrees, 20 blocks) for 5.
  * *Tell:* A streak along the spring and one to the foe.
  * *Answer:* A move; the arrow is small.
* **Meteor Bow** (`bow.meteor_bow`; 200 ticks, 3 percent). Artillery. A star falls after 24 ticks within 4 blocks of the point aimed at (up to 48 blocks): 16 to everyone inside, pushed back.
  * *Tell:* Three rings on the ground (cast, middle, just before) and a star streak from the sky.
  * *Answer:* Leave the ring in the 1.2 seconds.
* **Frozen Rain** (`bow.frozen_rain`; 180 ticks, 2.5 percent). Artillery. 5 waves 10 ticks apart within 6 blocks of the point aimed at (up to 36 blocks): 2 each and Slowness II for 1.5 seconds.
  * *Tell:* A ring and an ice effect on the ground.
  * *Answer:* Leave the ring; the slow is short.
* **Starfall Volley** (`bow.starfall_volley`; 900 ticks, 6 percent). The ultimate. After 20 ticks, 8 waves 10 ticks apart within 8 blocks of the point aimed at (up to 40 blocks): 3.25 each (26 in all).
  * *Tell:* A large magic circle and ring on the ground, 10 streaks falling every wave.
  * *Answer:* Leave the ring; the circle is 16 blocks across and is drawn a full second before the first wave.
* **Law Fanatic** (`bow.law_fanatic`; 300 ticks, 2 percent). Pride. Resistance I and Speed I for 10 seconds, and plain arrows 15 percent harder.
  * *Tell:* A shadow effect that follows the wielder.
  * *Answer:* A defensive buff, not an attack; it lasts 10 seconds and the cooldown is 15 seconds.
* **Bull of Phalaris** (`bow.bull_of_phalaris`; 220 ticks, 2.5 percent). Pride. The foe under the crosshair (within 28 blocks) is held for up to 1.5 seconds (the shared control lock; a boss is staggered) and takes 10 after 10 ticks.
  * *Tell:* A brass bull effect and a ring at the foe's feet for 2.5 seconds, an anvil sound.
  * *Answer:* Leave the ring in the half second before it closes, or break line of sight.
* **Pride's Command** (`bow.pride_command`; 240 ticks, 2.5 percent). Pride. Arms a command. The next draw of 40 percent or more hits the first monster for 4 and turns it and up to 3 monsters near it (10 blocks) against each other, for as long as their minds allow (the shared control lock). Never a boss or a player.
  * *Tell:* Threads of light between the monsters.
  * *Answer:* It needs monsters near its victim; the lock stops a second command for 12 seconds.
* **Autocracy Justice** (`bow.autocracy_justice`; 900 ticks, 6 percent). Pride, ultimate. A lion of light tears at everything within 7 blocks of the point aimed at (up to 36 blocks): after 20 ticks, 4 waves 10 ticks apart, 7 each (28 in all). Adds 30 corruption.
  * *Tell:* A magic circle and a ring on the ground, a lion of fire and light from the bow to the ground, a full second before the first wave.
  * *Answer:* Leave the ring in the first second.

## 6. Passives

A form lists its passives; the logic checks them with `HeroForm.has`. Names and texts are in the lang fragment (`hero.passive.bow.<id>`).

* **Quickdraw** (`quickdraw`): The string draws a quarter faster: full power after 16 ticks instead of 20.
* **Farsight** (`farsight`): Plain arrows fly 30 percent farther, never past 64 blocks.
* **Piercing Arrows** (`piercing_arrows`): A plain arrow goes through the first foe and strikes a second one for 70 percent of its power.
* **Eagle Eye** (`eagle_eye`): Plain arrows hit foes farther than 24 blocks 25 percent harder, never above the ordinary limit.
* **Pride Arrows** (`pride_arrows`): Your arrows grow huge: 20 percent harder and as wide as a block.

## 7. Balance: range and utility, not the Sword's damage

The owner asked to keep the Bow's damage per second below the Sword's. The plain arrow at its very strongest does 12 raw damage and takes a full draw (20 ticks, 16 with
Quickdraw) plus the 4 tick lock between two shots: at most 10 raw damage a second, and 5.8 for an ordinary full draw of 7. `BowNumbersTest` pins those two ceilings, and
`BowFormsFileTest` pins that the strongest form's draw strength cannot push a plain arrow past 12. The techniques that do more are all on cooldowns of 80 ticks or
more, the heavy ones on 120 to 220, the ultimates on 900. Whether this is below the Sword's damage per second depends on the Sword builder's numbers, which this
builder has not seen: the lead should compare the two pages. Everything the Bow does at range can be answered by cover, by leaving a line or a ring, or by distance.

## 8. The Pride series (the curse forms)

**Canon.** Pride is Itsuki's series: Law Fanatic, Bull of Phalaris and the brainwashing special effect; the web novel adds Autocracy Justice, at the price of losing the
sense of self [S].

**What the series does here.** Three forms (`bow_of_pride_1` to `_3`), each a step above the one before in attack stat, corruption a second and techniques. The
WEAPON's side is in this mod's code (`BowDirect`, `BowRain`, the Pride Arrows in `BowShots`); the engine's side (despair, the unlock, the price, the corruption bar,
the overrun, "the wielder cannot take help" and "the sight narrows") belongs to the curse builder and is not duplicated here.

| form | corruption a second | what is new |
|---|---|---|
| Bow of Pride I | 2 | Pride Arrows (20 percent harder and a block wide), Law Fanatic, Bull of Phalaris, the sniper arrow and the backstep |
| Bow of Pride II | 3 | Pride's Command, Arrow Rain, Piercing Arrows |
| Bow of Pride III | 5 | Autocracy Justice (the ultimate), Farsight |

* **Huge arrows.** Pride Arrows make the plain arrow 20 percent harder (still never above 12 raw) and 1.0 wide, so it hits what a narrow arrow misses.
* **Why it costs.** Autocracy Justice adds 30 corruption on top of the form's rate, and every Pride form pays its corruption each second. The forms give no permanent bonus.
  Pride's Command and the Bull of Phalaris are hard control and share the 12 second lock; neither moves or commands a boss.
* **The arrows hit harder than the tree's** (attack stat 6 to 10 against 5 to 8): that is the temptation of the series; its price is the corruption and the curse engine's own.

## 9. Engravings (CLAUDE.md rule 6)

The line for `item/WeaponEngravings.java` (shared file, so the lead adds it; the item id is the forms file's name):

```java
Map.entry("legendary_bow", List.of(e("holy_weapon", 2), e("barrier_piercing", 2), e("swift", 2), e("elemental_boost", 1))), // Itsuki's Cardinal weapon: holy, arrows of light that go through wards, a quick draw, frost and fire in its rains
```

* `holy_weapon` 2: the Legendary Bow is one of the four Holy (Cardinal) Weapons.
* `barrier_piercing` 2: the sniper arrow and the arrows of light go through wards (Eagle Piercing Shot is the defence-ignoring arrow of canon).
* `swift` 2: the quick draw, the retreat, the multi-shot.
* `elemental_boost` 1: the Frozen Rain and the Meteor Bow carry ice and fire; one level because the bow's own damage is not elemental.

All four ids were checked against the Tensura registry dump (`tensura_ids/clean.json`: enchantments `holy_weapon`, `barrier_piercing`, `swift`, `elemental_boost`).

## 10. Effects used (the mod's own system only)

`VfxShape`: `STAR_MAGIC` (the arrows and rains), `LIGHT_BLADE` (the sniper arrow), `LIGHT_FLARE` (hits), `THREAD_LINE` (tells: the fan, the laser sight, the commands),
`COMPASS_NEEDLE` (the seeker), `COMPASS_LOCK` (the mark), `THREAD_WEB` (the net), `FIRE_BURST` (the burst, the meteor), `ICE_FX2` (Frozen Rain), `MAGIC_CIRCLE`
(Starfall Volley, Autocracy Justice), `FIRE_LION` (Autocracy Justice), `BRONZE_FX2` and `FIRE_PILLAR` (the Bull of Phalaris), `SHADOW_UNITE` (Law Fanatic). `Tells.ring`
draws every ground ring. Sounds are `SoundEvents`. No vanilla particle is used, and no technique sets anything on fire (fire would be damage outside the balance law).

Colours: gem yellow-gold `0xFFFFD84A`, white-gold light `0xFFFFE8A0`, the sniper's laser sight `0xFFFF5A4A`; the Pride forms turn every effect `0xFF7A3FB5`.

## 11. Files, tests, how to check

* `weapon/bow/` : `BowLogic` (the hooks; its constructor registers the self-test), `BowSkills` (joins a row to a body; the six arming bodies are one), `BowNumbers`,
  `BowIds`, `DrawMath`, `ArmedMode`, `Volley` (pure), `BowShots` (the release of the string and the armed arrows), `BowRain` (the rains and the meteor), `BowDirect`
  (the squall, bird hunting, the retreat, Law Fanatic, the bull), `BowState` (the `bow` compound of the bond), `BowFx`, `BowSelfTest`.
* `weapon/pierce/` (shared with the Spear): see the Spear page.
* `src/main/resources/data/nusmp/hero/forms/legendary_bow.json` : 10 ordinary forms and 3 Pride forms.
* Tests (`src/test/java/.../heroweapon/weapon/`): `bow/BowNumbersTest` (the budget, the table against the id list, the second arrow never above the ordinary budget, the
  ceiling on the plain arrow's damage per second), `bow/DrawMathTest` (the curve, a tap, range, cost, damage), `bow/VolleyTest`, `bow/ArmedModeTest`, `bow/BowFormsFileTest`, and the
  shared `pierce/` tests. CI runs them with JUnit; the builder ran them with the repo's small JUnit shim and mutation-checked one (the Starfall waves put back to 8 ticks apart fail
  the hurt-immunity test).
* In game: the smoke test runs `hero.weapon.bow` (the paper checks, then a stand-in player with the bow in hand: a tap shoots nothing, a full draw hurts a husk eight
  blocks ahead, every armed arrow is loosed, a weak draw does not spend a sniper arming, and every technique is cast once).

## 12. For the others

* **Lead.** Shared files that need the Bow's lines: `WeaponEngravings` (section 9), `en_us.json` (the lang fragment `hero/frag/hsb.lang.json`: `hero.skill.bow.<id>.name` and
  `.desc` for the 17 techniques, `hero.passive.bow.<id>` and `.desc` for the 5 passives; no `hero.msg.*` key is used, the action-bar lines are plain English strings
  like the older weapons'). The self-test is `hero.weapon.bow`. No quest key and no config key: the weapon builders keep their numbers in code.
* **Core and dispatcher.** `onRelease(c, chargeTicks)` must be called with the number of ticks the string was held, for the DRAW style, once per release; the Bow
  returns true (handled) for every release while the weapon is summoned, including a tap, so the core should not shoot a real arrow after it. The Bow awards its own
  mastery point (`FormMastery.hit`) and one point of weapon experience (`HeroXp.gain`) for a hit by an arrow: if the core also does that for ranged hits, one of the two
  should go. Costs and cooldowns of the techniques are the dispatcher's; the arrow's own cost (0.4 percent at a full draw) is paid by the Bow through `HeroCtx.pay`.
  The Bow keeps one small compound in the bond: `bow` (the armed change, the last shot, Law Fanatic's end; absent until used).
* **Forms and curse builders.** The forms file follows design.md section 6 (checked by `FormsFileCheck`); no form lists the `use` slot, which is the draw. The curse
  engine's part of Pride (the despair, the unlock, the price, the corruption bar, the overrun, "the wielder cannot take help", "the sight narrows") is not in this weapon;
  the weapon only adds corruption through `HeroCurse.addCorruption` (Autocracy Justice 30) and its own techniques.
* **Art builder (`hart1`).** Section 1 is yours, including the optional draw stages. Nothing in the logic depends on the mesh.
* **Anyone reusing `weapon/pierce`.** See the Spear page.

## 13. What is not verified

Nothing of the Bow has been run in a game. Honestly:

* **Checked:** the code type-checks against the API stub dump (the only errors left are the stub's own "MobEffectInstance is abstract", which the same stubs give in about
  fifty older files of the mod); the pure logic and the data file pass their JUnit tests under the repo's small JUnit shim; every `tensura:` item id of the forms file and
  the four engraving ids and the Fragility effect id were looked up in the Tensura registry dump.
* **Not checked, and why it matters:**
  * That the self-test `hero.weapon.bow` passes: it has never run (no game here). It needs the husk eight blocks ahead to be found by the line of the shot.
  * What any effect looks like: the `VfxShape` values were chosen from their names and comments, never rendered. The colours and sizes are guesses.
  * That the core calls `onRelease` as described above (ticks held, once per release) and does not also shoot a real arrow.
  * `Mob.setTarget` as the way Pride's Command turns monsters on each other: the monsters' own target goals may take the target back at once.
  * Whether Tensura's Fragility "takes more damage from everyone" (the repo's own docs call it "resistance shredding"); the mark also uses Glowing.
  * Slowness VII as a root (the target can still jump), and Slowness II as "slowed", not as hard control. If the owner counts a slow as control, the places are
    `BowRain` (Frozen Rain) and `BowShots` / `BowDirect` (the roots, which already go through the control lock).
  * The seeker's bolt stops at the first living creature it meets, a friendly one included (it then hurts nobody); the owner may prefer it to pass through.
  * That the foes it spares really are spared: animals, villagers and armour stands are not struck unless they fight the wielder; tamed pets are skipped when they
    implement `OwnableEntity`; Tensura's own summons are protected only if they do (or if `isAlliedTo` says so). Not checked against Tensura's classes.
  * Safe places for the Backstep Shot: `Moves.standable` is deliberately strict and may refuse where a player could stand.
  * Chunks: the lines of the techniques (`Rays`, `Foes.sees`, the mod's own `aim` and bolts) use `Level.clip`, which can load a chunk when a line ends beyond the loaded
    area; the sniper arrow reaches 64 blocks and the Meteor Bow aims 48, within a few chunks of the wielder, so it only matters with a very small view distance or at the world's edge, as for the mod's older bolts.
    Everything that MOVES the wielder (`Moves`) asks `hasChunkAt` first and refuses an unloaded place; nothing here ever force-loads a chunk on purpose.
  * Balance: the numbers obey the owner's budget (tested) and the plain arrow's damage per second is capped by a test of the Bow's own table (10 raw a second at
    most); whether that is below the Sword's is for the lead to compare with the Sword page, and the feel is untested.
* **Canon:** all of section 2 is from search summaries of fan-wiki pages that could not be opened. "Hundred Arrows" and a damaging "Meteor Shot" were not found and are
  not used. The forms `hunters_bow`, `scouts_bow`, `eagle_piercer`, `sagittarius` and the Pride I and II names are this mod's inventions. The winged ornaments of the
  look are from memory.
