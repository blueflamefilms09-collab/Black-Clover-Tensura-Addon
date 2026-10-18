# Vassal Staff (`vassal_staff`): the mage's weapon of accelerated casting

Class SEVEN_STAR, style CAST. In the series the Vassal Staff is one of the seven Seven Star Weapons of Raphtalia's world; its special ability is accelerated magic casting and
its power-up method is Magic Power-up Points (wiki summaries); its holder in the story is Aultcray Melromarc XXXII. The owner names no holder for it. Logic
`heroweapon/weapon/staff/StaffLogic`, forms `data/nusmp/hero/forms/vassal_staff.json`.
This page is written in sections; the LOOK section comes first because the art builder (`hart2`) reads it first.

## 1. Look (for the art builder)

Source of the look: the owner's text ("a mage's weapon"), the wiki's skill names (Fenrir Rod, Float Mirror, Magic Prism) and the seed "a long mage staff with the gem in a ring head".
Colours and proportions are DESIGN.

* **Silhouette.** A tall, slender mage's staff, class `staff` of `weapon_classes.json` (apparent length 2.0 to 2.6 blocks, aim for 2.2; grip at 0.40 of the length from the butt).
  Straight, narrow and vertical in feel, with all the visual weight in the head. The head is an OPEN RING (a torus, outer diameter about 0.14 of the length) held on the shaft by
  four small curved prongs, with a faceted gem FLOATING in the middle of the ring, and a small upturned crescent on top of the ring. The ring's plane faces the viewer (the ring's
  axis runs along mesh z) so that first person sees the whole circle with the gem in the middle.
* **Named parts (9 to 13).** `shaft` (a slightly tapering round lathe, 8 or 12 sides), `grip_wrap` (leather bands over the grip zone), `band_low`, `band_mid` (silver rings on the
  shaft), `foot` (a pointed silver ferrule on the butt), `collar` (a silver sleeve under the head), `prongs` (4), `head_ring` (the torus), `crescent`, `gem` (a double pyramid / octahedron,
  radius about 0.05 of the length, floating, not touching the ring), `stars` (seven small raised five-point stars down the front of the shaft: the Seven Star mark; they may be
  texture only on the body skin but must be on the glow skin).
* **Palette.** Shaft lacquered dark indigo `#262A5A`; metal silver-white `#D3DAEA` with brighter edges `#F4F7FF`; trim pale gold `#E7D79A`; grip leather `#3A2C3F`; gem violet-blue
  `#7B8CFF` with highlights `#C7CEFF`. The seven stars `#A9B5FF`.
* **The gem.** The floating faceted gem in the ring. On the glow skin it is the strongest spot; the glow colour in the mesh's `glow` line is `7B8CFF`.
* **Glow skin** (additive): the gem (strong), the seven stars (soft), the ring's inner edge (faint). Nothing else.
* **In hand.** The grip at the mesh origin region, the staff pointing up-forward on the icon's diagonal like the mod's other staffs and spears, the ring facing the wielder
  in first person. Sprite (16 x 16, kept as the inventory icon): a diagonal indigo shaft with a silver ring head and one violet dot inside the ring.
* **Mood.** Scholarly and calm; the glow of the gem is the only loud thing.

## 2. Canon and sources

Each fact is tagged [summary] (read in a web search summary of the fandom wiki; the pages themselves were not opened), [memory] (from what the builder remembers of the series) or [design] (this mod's own choice).

* [summary] The Vassal Staff (Seven Star Staff, the "Staff Spirit of the Vassal Weapon") is one of the eight Vassal Weapons of Raphtalia's world; its special ability is accelerated magic casting. https://shield-hero.fandom.com/wiki/Vassal_Staff
* [summary] Its power-up method is Magic Power-up Points: points gained on level-up are put into magic spells to raise their Rank (1 to 10), which strengthens them and can add effects; the pool is separate from the Gauntlet's Skill Power-up Points; points can be reassigned only after 24 hours. https://shield-hero.fandom.com/wiki/Legendary_Weapons/Power-up_Methods
* [summary] Its holder is Aultcray Melromarc XXXII (he earned it by rank and later was no longer deemed worthy). https://shield-hero.fandom.com/wiki/Aultcray_Melromarc_XXXII and https://shield-hero.fandom.com/wiki/Aultcray_Melromarc_XXXII/Powers_and_Abilities
* [summary] Skills named by the wiki: Fenrir Rod (the user can chant magic with increased speed), Float Mirror (a mirror floats mid-air and can be moved at will), Magic Prism (fires a prism; when a magic attack hits it, it makes several copies of that attack), a magic-reflect skill that puts the enemy under a perpetual bombardment of magical attacks, and Staff 0 (no damage, effective against illegitimate powers; not used here). Naofumi used the staff for a while: he could analyze an opponent's magic quickly, cast his own magic and some combo skills (the origin of Analyze Magic).
* [design] Everything that is not a name above is this mod's design: the bolt, the chain spark, the rune field, the burst and the heal are the mage's ordinary kit for first-person combat; the per-spell Rank system is not copied (a rank table per spell would be a menu, not a weapon), its place is taken by the casting rhythm (spells cast one after another come back sooner) and by marks.
* [design] The colours (indigo shaft, silver, violet-blue gem) are the art brief's.

## 3. How it plays

A caster that keeps a **rhythm**. Every staff spell cast within 4 s of the previous one continues a chain, and with the Quick Chant passive each earlier spell of the chain (up to 3) makes the next cooldown 5 percent shorter. Fenrir Rod is the other half: for 8 s all staff spells wait 35 percent less. The two multiply but never go below 0.55 of the nominal cooldown, and never below the minimum cooldown of the kind of technique (60 ticks for damage, 100 for a utility, 600 for the ultimate), so the staff cannot be hasted into the budget's forbidden zone. A full melee swing with the staff also keeps the chain alive.

Marks are the staff's second system. Analyze Magic marks up to 6 foes; a marked foe takes 15 percent more from the staff's damage spells, so the opening Analyze pays for itself. The Magic Prism multiplies a bolt, the Float Mirror answers archers, Healing Light looks after the party, and the Perpetual Bombardment is the ultimate that finishes a fight in a field.

What the staff keeps in memory (per wielder, in `StaffState`): the rhythm counters, up to 32 marks with end times, one prism and one mirror with end times. All of it ends by itself, is dropped on logout, when the weapon is put away and when the server stops, and nothing is saved to disk.

All damage goes through the shared hit gate (the balance law applies); heals through the shared heal gate; no hard control at all (only slows); no block is ever changed; no entity is spawned; every repeating task (rune pulses, mirror watch, bombardment salvos) is a bounded zone of at most 6 s.

## 4. Forms

Five forms (3 to 5 required), each a different way to play; the requirements are items the gem absorbs. The weapon has no curse series.

| form | name | tier | parents | requirements | mastery | stats | skills (slot: skill) | passives |
|---|---|---|---|---|---|---|---|---|
| `base` | Apprentice Staff | 0 | - | - | 0 | max_health 2 | use: arcane_bolt, sneak: arcane_burst, key1: fenrir_rod | quick_chant |
| `focus_staff` | Amethyst Focus Staff | 1 | base | amethyst_shard x32, lapis_lazuli x32 | 120 | max_health 2, movement_speed 0.02, luck 1 | use: arcane_bolt, sneak: arcane_burst, key1: fenrir_rod, key2: analyze, key3: chain_spark | quick_chant, sharp_focus, star_mark |
| `rune_staff` | Runic Staff | 2 | focus_staff | blaze_rod x16, glowstone_dust x32, tensura:low_quality_magic_crystal x12 | 200 | max_health 4, movement_speed 0.03, luck 1 | use: arcane_bolt, sneak: rune_field, key1: fenrir_rod, key2: magic_prism, key3: healing_light | quick_chant, wide_prism |
| `mirror_staff` | Mirror Staff | 2 | focus_staff | glass x48, ender_pearl x12, quartz x32 | 200 | max_health 2, armor 3, armor_toughness 1, luck 1 | use: arcane_bolt, sneak: arcane_burst, key1: float_mirror, key2: analyze, key3: magic_prism | quick_chant, deep_mirror, star_mark |
| `archmage_staff` | Seven Star Archmage Staff | 3 | rune_staff, mirror_staff | tensura:high_quality_magic_crystal x12, echo_shard x8, dragon_breath x6, amethyst_shard x64 | 320 | max_health 6, movement_speed 0.05, armor 2, luck 2, attack_damage 2 | use: arcane_bolt, sneak: rune_field, key1: fenrir_rod, key2: magic_prism, key3: bombardment | quick_chant, sharp_focus, wide_prism |

Item ids were checked by the real forms validator (the job's forms check script: structure, stat budgets, vanilla item and tag ids against the API dumps); the three `tensura:` crystals are ids that other systems of this mod already use, and the game's own `hero.forms` self-test resolves them against the live registry.

## 5. Techniques

Ten techniques (6 required), one ultimate (the Perpetual Bombardment, only in the top form). Every number lives in `StaffNumbers` and the live skills are built from that table, so the cooldown, the cost and the ultimate flag the dispatcher sees are the table's. `rawMax` counts the strongest value the mechanics can produce (a mark and Sharp Focus together), and a test proves that no technique exceeds the budget even then: ordinary 3 to 12 raw, ultimate at most 32 in all to one foe, at most 12 foes, cooldown at least 60 (damage), 100 (utility), 600 (ultimate), cost 1 to 3 percent (ultimate 5 to 8).

| technique | id | kind | cooldown | cost | raw damage (base..strongest, hits per foe) | foes | heal |
|---|---|---|---|---|---|---|---|
| Arcane Bolt | `staff.arcane_bolt` | ordinary | 80 ticks (4 s) | 1.5% | 6..7.475 x1 | 7 | 0% |
| Chain Spark | `staff.chain_spark` | ordinary | 110 ticks (5.5 s) | 2% | 5..5.75 x1 | 5 | 0% |
| Rune Field | `staff.rune_field` | ordinary | 160 ticks (8 s) | 2.5% | 3.2..3.68 x3 | 12 | 0% |
| Arcane Burst | `staff.arcane_burst` | ordinary | 120 ticks (6 s) | 2% | 8..9.2 x1 | 12 | 0% |
| Magic Prism | `staff.magic_prism` | utility | 240 ticks (12 s) | 2% | none | 1 | 0% |
| Float Mirror | `staff.float_mirror` | utility | 320 ticks (16 s) | 2.5% | 3..3 x4 | 4 | 0% |
| Fenrir Rod | `staff.fenrir_rod` | utility | 360 ticks (18 s) | 3% | none | 1 | 0% |
| Analyze Magic | `staff.analyze` | utility | 200 ticks (10 s) | 1.5% | none | 6 | 0% |
| Healing Light | `staff.healing_light` | utility | 420 ticks (21 s) | 3% | none | 8 | 14.0% |
| Perpetual Bombardment | `staff.bombardment` | ultimate | 900 ticks (45 s) | 6% | 4.5..5.175 x6 | 12 | 0% |

The cooldown shown is the nominal one; the rhythm and Fenrir Rod shorten it (never below the floor of its kind).

### Arcane Bolt (`staff.arcane_bolt`)

* **What.** A hitscan bolt along the look line (24 blocks, 0.45 wide). It stops at a wall and strikes up to 2 foes on its line (3 with Sharp Focus), nearest first, each once. 6 raw, +0.5 with Sharp Focus, +15 percent on a foe carrying one of your marks (7.475 at most). If a Magic Prism of yours hangs within 1.8 blocks of the bolt's path the prism spends a charge and splits the bolt into up to 3 copies (4 with Wide Prism), each at a different foe within 14 blocks of the prism that the prism can see, at 60 percent of the damage. One foe is never hit twice by one cast, so the technique's 7 foes are the 3 pierced plus the 4 copies at most.
* **Numbers.** ordinary; cooldown 80 ticks (4 s); cost 1.5% of the maximum magicules; raw damage 6..7.475 x1; foes 7; heal 0%. Given by: base:use, focus_staff:use, rune_staff:use, mirror_staff:use, archmage_staff:use.
* **Tell.** The cast swing, a rising chime and the violet lightning line itself. There is no wind-up, which is allowed because its strongest value stays at or under 8 raw (a test pins this).
* **Counter.** Break the line of sight (a wall stops the bolt), stay out of 24 blocks, or kill the wielder between casts: it waits 4 s.

### Chain Spark (`staff.chain_spark`)

* **What.** Aim at a foe (22 blocks, 0.9 wide, a wall stops the aim). A spark strikes it for 5 raw (5.75 marked), then leaps up to 4 times, each time to the nearest unstruck foe within 7 blocks of the last, among foes within 18 blocks of the first. Arcs are drawn from the staff to the first foe and from foe to foe. Refused (nothing spent) when there is no foe in the line.
* **Numbers.** ordinary; cooldown 110 ticks (5.5 s); cost 2% of the maximum magicules; raw damage 5..5.75 x1; foes 5; heal 0%. Given by: focus_staff:key3.
* **Tell.** The lightning arcs; instant, 5 raw.
* **Counter.** Spread out: a jump is at most 7 blocks, so a foe standing 8 blocks from the others is skipped; or break the line of sight to the first foe.

### Rune Field (`staff.rune_field`)

* **What.** A rune circle (radius 4) drawn on the ground at the aim point (20 blocks, dropped to the floor). After 1 s it pulses 3 times, 0.6 s apart, each pulse 3.2 raw (3.68 marked) to every foe inside plus Slowness II for 1.5 s. The foes are looked up at each pulse, so a foe that walks in later is hit by the pulses that remain.
* **Numbers.** ordinary; cooldown 160 ticks (8 s); cost 2.5% of the maximum magicules; raw damage 3.2..3.68 x3; foes 12; heal 0%. Given by: rune_staff:sneak, archmage_staff:sneak.
* **Tell.** A dust ring on the ground and a seal circle on it, 1 s before the first pulse; each pulse bursts in a circle flash.
* **Counter.** Walk out of the ring during the first second (or stand outside it: it does not move).

### Arcane Burst (`staff.arcane_burst`)

* **What.** A ring of force 5 blocks round the wielder. 0.3 s after the ring shows, every foe inside takes 8 raw (9.2 marked) and a shove of 0.9 away from the wielder. The ring is centred where the wielder stands when it lands.
* **Numbers.** ordinary; cooldown 120 ticks (6 s); cost 2% of the maximum magicules; raw damage 8..9.2 x1; foes 12; heal 0%. Given by: base:sneak, focus_staff:sneak, mirror_staff:sneak.
* **Tell.** A dust ring on the ground and an expanding gust ring, 6 ticks before the blow.
* **Counter.** Leave the ring in the 0.3 s it shows, or keep out of 5 blocks.

### Magic Prism (`staff.magic_prism`)

* **What.** Hangs a prism where you aim (3 to 18 blocks along the look line, stopped by a wall) for 8 s (12 s with Wide Prism) with 3 charges. An Arcane Bolt whose path passes within 1.8 blocks of it splits (see Arcane Bolt). Only one prism per wielder; a new one replaces the old.
* **Numbers.** utility; cooldown 240 ticks (12 s); cost 2% of the maximum magicules; raw damage none; foes 1; heal 0%. Given by: rune_staff:key2, mirror_staff:key3, archmage_staff:key2.
* **Tell.** An orbiting sigil at the prism and a flash; the prism is a visual, it cannot be hit.
* **Counter.** It is a buff: kill or outrange the wielder; a foe near the prism is the one that gets copies, so stand away from it.

### Float Mirror (`staff.float_mirror`)

* **What.** A mirror floats 1.6 blocks in front of the wielder's eyes for 4 s (6 s with Deep Mirror). Any projectile entity not fired by the wielder or an ally that flies towards the wielder inside the mirror is destroyed, up to 6 (8 with Deep Mirror); the mirror's reach is 2.4 blocks round its centre. Up to 4 of those shots whose shooter is a foe within 40 blocks are sent back as a magic ray for 3 raw each (a shooter can take every returned shot: 3 raw x 4 at most).
* **Numbers.** utility; cooldown 320 ticks (16 s); cost 2.5% of the maximum magicules; raw damage 3..3 x4; foes 4; heal 0%. Given by: mirror_staff:key1.
* **Tell.** A mirror pane in front of the wielder, redrawn every half second; every destroyed shot flashes a ray to the mirror.
* **Counter.** Attack in melee or with something that is not a projectile entity; the mirror only stops what it can see coming, and it runs out after 6 shots or 4 s.

### Fenrir Rod (`staff.fenrir_rod`)

* **What.** Quickens the chant for 8 s: every staff spell the wielder casts waits 35 percent less before it can be cast again (the cooldown is multiplied by 0.65; together with the Quick Chant chain bonus it never goes below 0.55 of the nominal value, and never below the minimum of its kind: 60 ticks for damage, 100 for a utility, 600 for the ultimate). Fenrir Rod itself is not hasted.
* **Numbers.** utility; cooldown 360 ticks (18 s); cost 3% of the maximum magicules; raw damage none; foes 1; heal 0%. Given by: base:key1, focus_staff:key1, rune_staff:key1, archmage_staff:key1.
* **Tell.** A mana charge gathers on the wielder and a wind mantle follows them for the 8 s; an action bar line says it is on.
* **Counter.** It is a buff: burst the wielder down during the 8 s, or wait it out.

### Analyze Magic (`staff.analyze`)

* **What.** Marks up to 6 foes in a cone in front of the wielder (24 blocks, 22 degrees to either side) for 10 s (15 s with Star Mark). A marked foe takes 15 percent more from the staff's own damage spells (the bonus is added to the raw number before the balance law). Refused when the cone holds no foe.
* **Numbers.** utility; cooldown 200 ticks (10 s); cost 1.5% of the maximum magicules; raw damage none; foes 6; heal 0%. Given by: focus_staff:key2, mirror_staff:key2.
* **Tell.** A lock sigil over every marked foe.
* **Counter.** Hide: a mark fades by itself. It does nothing alone.

### Healing Light (`staff.healing_light`)

* **What.** Heals the wielder and up to 7 allies (players of the team, pets, summons) within 7 blocks for 14 percent of their maximum health, through the shared heal gate, and washes away one harmful effect each.
* **Numbers.** utility; cooldown 420 ticks (21 s); cost 3% of the maximum magicules; raw damage none; foes 8; heal 14.0%. Given by: rune_staff:key3.
* **Tell.** A green rune circle at the wielder's feet and a healing stream to every ally.
* **Counter.** None needed: it is a heal. A foe can burst a healed ally down before the next one (21 s).

### Perpetual Bombardment (`staff.bombardment`)

* **What.** The ultimate (the wiki's magic-reflect "perpetual bombardment"). Marks a field of radius 4.5 where you aim (24 blocks). After 1.2 s six salvos fall 0.5 s apart; each salvo strikes every foe inside for 4.5 raw (5.175 marked): at most 27 (31.05 marked) to one foe, 12 foes at most. The field does not move.
* **Numbers.** ultimate; cooldown 900 ticks (45 s); cost 6% of the maximum magicules; raw damage 4.5..5.175 x6; foes 12; heal 0%. Given by: archmage_staff:key3.
* **Tell.** A dust ring and a 6 block column of dust, plus a large divine sigil, for 1.2 s before the first salvo; every salvo drops three bolts of light into the field.
* **Counter.** Leave the field in the 1.2 s warning and stay out for the following 2.5 s; the wielder is free to move while it falls.

## 6. Passives

| passive | id | effect | forms that have it |
|---|---|---|---|
| Quick Chant | `quick_chant` | Spells cast one after another (each within 4 s of the last) wait 5 percent less per earlier spell, up to 15 percent. A full staff swing keeps the chain going. | base, focus_staff, rune_staff, mirror_staff, archmage_staff |
| Sharp Focus | `sharp_focus` | Arcane Bolt deals 0.5 more damage and pierces one more foe. | focus_staff, archmage_staff |
| Wide Prism | `wide_prism` | Magic Prism lasts 4 s longer and splits a bolt into 4. | rune_staff, archmage_staff |
| Deep Mirror | `deep_mirror` | Float Mirror lasts 2 s longer and can destroy 2 more shots. | mirror_staff |
| Star Mark | `star_mark` | Marks from Analyze Magic last 5 s longer. | focus_staff, mirror_staff |

## 7. Engravings (the owner's rule 6: every weapon gets Tensura engravings)

The line for `item/WeaponEngravings.java` (the lead integrates it; it is also in the builder's report):

```java
Map.entry("vassal_staff", List.of(e("magic_weapon", 1), e("swift", 1), e("magic_interference", 1))),
```

* `magic_weapon` 1: the staff is a casting focus and the gem in its ring is a source of magic, so its own blows are magical.
* `swift` 1: the Vassal Staff's signature is accelerated casting; the engraving is that speed on the item.
* `magic_interference` 1: the Magic Prism and the Float Mirror bend other people's magic and shots; interference with enemy magic is what they do.

## 8. Files

* Logic: `heroweapon/weapon/staff/` : `StaffLogic` (skills, hooks, wiring, self-test), `StaffSpells` (the ten bodies), `StaffNumbers` (every number, no game classes), `StaffText` (names, descriptions, passive texts, no game classes), `StaffState` (short-lived memory), `CastRhythm` (the chain and haste arithmetic, no game classes).
* Shared with the other three weapons of this builder (the Gauntlet, the Projectile and the Carriage): the small kit `heroweapon/weapon/kit/`, one line each on why it exists:
  * `Budget`: the design's balance budget (raw damage ranges, foes, cooldown floors, costs, heal cap) as data: a `Spec` per technique and `violations` that judges it; every Numbers class is a table of Specs and every Numbers test calls it.
  * `Foes`: who may be struck and who may be helped (never creative or spectator players, allies, the wielder's summons, pets and bodies, relic summons, a team mate); the area, line, cone and aim queries, nearest first, at most 12.
  * `Rays`: a straight flight stopped by the first solid block, with where it ended, whether a wall ended it and that wall's normal; `Geometry`: the plain arithmetic of segments, cones, turns, fans and chains (tested).
  * `Moves`: a lunge by distance, a search for a spot with room and ground and no lava, a blink to it; never changes a block.
  * `Timed`: the delayed half of a technique and a short repeating field, both re-checking that the wielder is still alive, in the same world and bonded to the same weapon when they run.
  * `Status`: the few mob effects the weapons use (slow, weakness, speed, resistance) and the removal of one harmful effect; `Tally`: the per-cast limits on how many foes and how many hits on one foe; `ExpiryTable`: a capped table of things that end by themselves (marks); `Fmt`: numbers in the texts.
  * `VbSkill`: builds the live skill from a Numbers row (cost, cooldown, ultimate flag are the row's) and catches a body that throws; `WeaponSelfTest`: the common half of the four self-tests; `FormsFileCheck`: the forms-file validator the data tests run.
* Forms: `src/main/resources/data/nusmp/hero/forms/vassal_staff.json`.
* Lang: the builder's fragment (`hvb.lang.json`, keys `hero.skill.staff.*` and `hero.passive.staff.*`).
* Tests: `StaffNumbersTest` (budget, strongest values, haste floors), `CastRhythmTest` (the chain), `StaffFormsTest` (the forms file against the validator, texts present, plain ASCII).

## 9. Tests and the self-test

* JUnit (run with the repo's test shim here and by CI): the whole table against the budget, the strongest values, that hasting never breaks a floor, the rhythm's window and saturation, the forms file (structure, fairness, every skill and every passive used by some form), the texts.
* Self-test `hero.weapon.staff` (registered from `StaffLogic`'s constructor, run by CI's server smoke test): checks the forms file resolves and every skill id of every form is registered, then binds the weapon to a stand-in player at the world spawn, puts the weapon in its hand, and casts every technique once beside a dummy husk, counting any exception as a failure; it checks the live skill's cost, ultimate flag and cooldown range against the table; it removes the dummy, releases the bond and forgets the stand-in's state.

## 10. What is unverified (honest list)

Nothing of this has been run in the game. Type-checked against API stubs: yes. Unit-tested: the arithmetic and the data. Not verified:

* How the effects look and sound (the VFX shapes were chosen from their source comments; colours and sizes are guesses: `LIGHTNING_SPEAR` honours the payload colour, the others, such as `SEAL_BASIC`, `SEAL_ORBIT`, `MIRROR_PANE`, `ELF_CIRCLE`, `DIVINE_SIGIL` and `COMPASS_LOCK`, may use palettes of their own).
* The feel of every number: they satisfy the budget, they have not been played.
* That the dispatcher asks for the cooldown after the cast (the rhythm counts the cast that just happened; if it asks before, the first spell of a chain is 5 percent slower than the text says).
* That the Float Mirror's "projectile" test catches every shot of this mod's bosses (it destroys any `Projectile` entity flying at the wielder that was not fired by the wielder or an ally; a boss that tracks its own projectile entity may be surprised by that).
* The stand-in player of the self-test (a `FakePlayer`) behaving like a real player for swings and ray queries.
