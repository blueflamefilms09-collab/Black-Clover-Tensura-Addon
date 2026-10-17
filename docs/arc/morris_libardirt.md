# Morris Libardirt (`morris_libardirt`)

Boss of the Spade Kingdom Raid (`ArcSaga.SPADE_RAID`), tier 4. First of the pair Morris + Lucifero (`docs/arc/lucifero.md`); the story unlocks him after the Dark Triad
(`dante`, `vanica`, `zenon`). The name is spelled "Moris" on the fan wikis and "Morris" on many other sites; the brief and this mod say Morris, the id is `morris_libardirt`.

He is the fight against a MAN WHO EDITS THINGS and who treats the fighters as specimens: he re-forms his own body against whatever has been hurting him, rewrites the first
spell thrown at him, ties a thread to the strongest of you and reels it in to dissect it, and closes a cage of tendrils round the party. When the Gates open the devil in him
surfaces: his glow turns violet, the gravity he borrowed from the contract comes in, and The Second Gate opens.

Everything in this file was written without being able to run the game. What was type-checked, tested and looked at, and what was not, is in section 11 and 13.

## 1. What is new, what is kept

| Part | New / replacement / kept |
| --- | --- |
| The boss class, its numbers, its pure math, its tests | NEW (the mod had no Morris; the Diamond Kingdom scholar existed only as a name in the story) |
| Dante's `DanteMoves.gravityField` and `stompWave`, `Devil.LUCIFERO`, the violet | KEPT and reused (Borrowed Weight is Dante's gravity field in Morris's numbers; the Second Gate's waves are Dante's stomp wave) |
| Modification Magic's orange and rune rings (`VfxShape.WIKI_MAGIC_*` with 0xFFFF9B58, `ALCHEMY_CIRCLE`, `DEVIL_CIRCLE`) | KEPT and reused (the mod already draws Modification Magic in this colour) |
| Three mob effects (`flattened_air`, `kneeling`, `specimen`) | NEW (shared with Lucifero: `SpadeEffects`) |
| Model `arc_morris_libardirt` + `arc_morris_libardirt_form2` | NEW (art kit, `tools/arc/char_morris_libardirt.py`) |

## 2. Canon (who he is, how the story fight went) and where it came from

HONEST NOTE ON SOURCES: the fetch tool could not resolve any host in the session (`getaddrinfo ENOTFOUND`), so NO page below was opened. Every fact comes from the summaries
a web-search tool returned for these pages; fan wikis and news sites, and the tool flagged some of them as low quality. Nothing is quoted; it is facts, in my words.

* A magic scholar of the Diamond Kingdom who for years controlled the king from behind the throne; thrown out by Mars, he went over to the Spade Kingdom's Dark Triad and
  worked as its scientist: experiments, among others to make a new generation of Eight Shining Generals (TV Tropes, via the search summary), and the Qliphoth Advent Ritual.
  He copied knowledge out of Lolopechka and used it to find a way to speed the ritual up (Animanga Infinity, via the summary). A pragmatist who follows his research, not a loyalty.
* Magic: Modification Magic, which changes the structure of physical and magical things. As a devil host it reaches intangible things too (knowledge, dreams) and living bodies:
  he can take a creature apart and put it together again. Gravity Magic came to him only through Lucifero's contract. As a Paladin much later he also has an artificial Earth Magic
  (his spell March of Holy Fools raises soldiers out of the ground).
* The fight with the Black Bulls at the Spade castle (chapter 312 on the wiki): he rewrote the ritual so that only he could activate it; as a host he can change intangible things, which
  made him a threat to Dorothy's Dream Magic; he wanted to dissect the "Arcane Stage" mages he had heard of from Dante and sent clawed tendrils to tear the base apart; his magic
  cancelled Vanessa's Fate Manipulation; the tendrils got past Rouge's protection because they act on intangible things; Grey and Gauche turned the tendrils into flowers; he then used the
  borrowed Gravity Magic to pin the base; Asta struck him in the face with the Demon-Dweller Sword; Henry's Raging Black Bull smashed into the chamber and destroyed a devil he had fused.
  Lucifero then forcibly took the last of his magic and life to open the second gate. (One fan breakdown argues that he never used the gravity to its fullest.)
* Look: a tall, slim, pale man with short spiky blond hair, round black tinted glasses over eyes that cannot see (he was born blind). As a host: a dark mark on the forehead grows, the irises change
  colour, the pupils become vertical slits, he grows Lucifero's four horns and wings, and his arms and hands turn black. Later, as a Paladin: a similar shirt and trousers of thicker darker cloth
  and a long light-coloured coat; Lucius rebuilt his body and cured the blindness. Mereoleona beat the Paladin (chapter 371).
* Conflicts between sources, not resolved: which devil he hosts (the wiki says Lucifero for the Spade fight and Asmodeus for Judgment Day; TV Tropes says Lucifero for both); whether the Spade connection is direct.

URLs (search-result pages, not opened): `https://blackclover.fandom.com/wiki/Moris_Libardirt`, `https://blackclover.fandom.com/wiki/Black_Bulls_vs._Moris_Libardirt`,
`https://blackclover.fandom.com/wiki/Chapter_312`, `https://villains.fandom.com/wiki/Moris_Libardirt`, `https://tvtropes.org/pmwiki/pmwiki.php/Characters/BlackCloverDiamondKingdom`,
`https://www.soapcentral.com/anime/who-morris-black-clover-identity-explained`, `https://www.animangainfinity.com/characters/moris-libardirt`,
`https://fictionoverlord.wordpress.com/2021/11/09/the-black-bulls-vs-devil-possessed-morris-black-clover-chapter-312-breakdown/`,
`https://www.sportskeeda.com/anime/black-clover-chapter-371-mereoleona-s-latest-awakening-sees-defeat-paladin-moris`.

## 3. The fight in one paragraph

A scholar does not brawl. He keeps seven to fourteen blocks away, throws needles, edits his own body against the kind of hit that has been hurting him, and wants one specimen: the
strongest of you. A party that fights him with one kind of attack finds him tuned against it (it does a third less) and open to the others (15% more); a party that switches kind, or brings
anti-magic (which he cannot edit), wins the trade. The thread he ties to the specimen is a team puzzle: somebody has to stand on it, or the line must be broken, or he must be hit hard enough to snap
it. From 60% power the gravity he borrowed from the contract comes in, as a WEDGE (not Dante's circles) and as a heavy field; at 60% he closes the Dissection Theatre, a cage of tendrils that closes round
the party from 16 to 5 blocks and has three DOORS; at the Gates (25% health) Lucifero surfaces through him (his glow turns violet and his body is drawn 10% bigger) and the last ultimate, The Second Gate,
rolls five waves of weight out of him. His test subjects (the framework's Dark Disciples, with the look the framework gives them) fill the room.

Power curve (framework): 20% at the start, 40% under 85% health, 60% under 65%, 80% under 45%, the Gates (100%) under 25%. A move's "power" below is the lowest power at which it is allowed.

## 4. The rules only he has

### 4.1 Structural Tuning (Re-Structure) and Spell Rewrite
* WHY: Modification Magic changes structure; his magic cancelled Vanessa's Fate Manipulation and went through Rouge's protection; the Black Bulls beat it with kinds of force he could not edit at once
  (flowers from Grey and Gauche, Asta's anti-magic, Henry's brute force). As a fight rule that is a TRADE, never a free wall: the same boring plan fails, a varied party gains.
* The ledger (`StructuralTuning`): every blow that lands on him is noted by kind (MELEE, RANGED, MAGIC) with a half-life of 100 ticks, so "recent" needs no list. Anti-magic and damage that nobody dealt are never noted and never tuned.
* Re-Structure (power 40, wind-up 24, cooldown 520): he tunes against the kind that leads the ledger (the recent total must be at least 2% of his maximum health, else the kind that struck last, else melee) for 240 ticks (12 s).
  The tuned kind is cut by `tuningCut` x a stage factor (0.85 / 1.0 / 1.15 / 1.3 at 40 / 60 / 80 / the Gates), never above 40%: with the default 0.30 that is 25.5%, 30%, 34.5%, 39%. Every OTHER tunable kind finds a hole: x1.15.
  So the party that does not change loses about a third; the party that rotates gains 15%. Anti-magic is not a kind: x1.0 always. While he is exposed after an ultimate the framework skips the cut.
* The counter: switch kind (melee <-> bow <-> spell), bring anti-magic, or BURST him: the cast has a window of its wind-up; damage of 5% of his maximum health inside it INTERRUPTS the Re-Structure ("Re-Structure is interrupted!").
* Spell Rewrite (power 20, wind-up 18, cooldown 380): for 4 s the FIRST arrow or spell (ranged or magic, from a living shooter) that touches him is cancelled and comes back at its shooter as 2 + stage needles (5 base each, a foe is hit at most twice).
  Melee and anti-magic go through untouched. The counter is to bait it with one cheap arrow or spell and throw the good ones after; it is announced when it goes up, and when it fades unused.
* Config: `structuralTuning` (true) turns the whole tuning off; `tuningCut` (0.30, range 0.05-0.40).

### 4.2 The Specimen thread (Specimen Mark)
* WHY: he wanted to dissect the Arcane Stage mages and sent tendrils to tear the base apart; what stopped his tendrils was the team (the flowers, the protection). As a rule it has to be BREAKABLE in
  several ways that each ask something of the party, or it is only damage. `Tether` is the whole rule as a pure state machine fed once per tick.
* Specimen Mark (power 20, wind-up 26, cooldown 360, range 4-30): he picks the foe with the most existence points within 30 blocks ("the strongest of you"; the chat names them during the wind-up). The thread lives
  160 ticks (8 s). While it holds the specimen is dragged towards him (a pull that grows from 0.03 to 0.13 blocks a tick, always under a walker's 0.216: a specimen can always walk away), drained 2.5 base once a second, and slowed 10% by the effect Specimen.
  HE STANDS STILL while the thread is out (the framework's positioning goal would otherwise back him off as the specimen is drawn in, and no thread could ever reach its specimen).
* How it ends (checked in this order each tick, so the answer never depends on the order the glue reads facts):
  1. ANTI-MAGIC: the specimen holds anti-magic, or an anti-magic blow landed on him this tick: the thread dissolves.
  2. BURST: he took 6% of his maximum health during the thread: it snaps, and he is staggered 40 ticks (x1.15 damage).
  3. CUT: a third foe stood ON the thread for 6 ticks in a row (within 1.15 blocks of the line, not within 2 blocks of either end, thread at least 4.5 blocks long): it is cut, he is staggered 40 ticks.
  4. LOST SIGHT: something solid between them for 30 ticks in a row.
  5. TOO FAR: more than 28 blocks apart.
  6. REACHED: the specimen is within 2.4 blocks of him: the DISSECTION (28 base as a spell, anti-magic cuts it to 40%, Weakness for 6 s), and he steps away.
  7. TIMEOUT after 160 ticks.
* The party sees a line of orange dust from him to the specimen with a rune ring under the specimen and a chat line naming them, an action-bar line for the specimen, and the effect icon.

### 4.3 Lucifero surfaces (from 60%, and the Gates)
* Gravity Pin (power 60) and Borrowed Weight (power 80): the gravity of the contract. Distinct from Dante's: a WEDGE that stays where it was cast, and Dante's field on a mark. Anti-magic halves the Pin's grip.
* Dissection Theatre (ultimate, power 60): the cage with DOORS (4.4). The Second Gate (ultimate, the Gates): five waves of weight that must be jumped (4.5).
* At the Gates: his glow and chat colour turn violet (`styleColor`, `styleAccent`, `chatColor`), the boss bar reads "Lucifero surfaces", the body is drawn 10% bigger (the host form with horns, wings and black arms), a field of
  violet and a gravity aura mark the change, and two more waves of test subjects step out at +120 and +400 ticks.

## 5. The moves (11). Numbers are BASE damage before the framework's damage law

The framework law turns a base number into the real hit: x the power's multiplier (0.60 at 20% up to 1.60 at the Gates) x `damageScale` (1.1 x the config factor) x a factor for the victim's existence points, then the Balance Law
(a sliver of the victim's maximum health, at most 40% of a player's health per hit). His spells (needles, shock, tendrils, thread, cage) hit as MAGIC (they ignore armour; anti-magic cuts them to 40%). Wind-ups shrink with power
(x0.95 at 60%, x0.85 at 80%, x0.75 at the Gates), never below 8 ticks (30 for an ultimate). The tell runs every third tick of the wind-up.

| # | Id / label | Power | Wind-up | Cooldown | Range | Weight | Base dmg | What it does | What the party sees | Counter |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `needles` Dissection Needles | 20 | 18 | 90 | 5-30 | 6 | 5.5 each | A fan of 3 needles (5 at 60%, 7 at 80%, 9 at the Gates), thrown two ticks apart, each re-aimed as it leaves; 1.4 blocks a tick for 26 ticks, stops at the first wall, hits the first foe it touches; a foe takes at most three of a volley. A shield does not stop it. | An orange line along each needle's path for the whole wind-up. | Step out of the lines; break line of sight; anti-magic 60% off. |
| 2 | `structure_shock` Structure Shock | 20 | 14 | 110 | 0-5.5 | 6 | 13 | A blast of rewritten air all round him, radius 4.6: a spell's hit and a shove outwards (knockback 1.5); six ticks later he steps 8-13 blocks away. | A ring growing to 4.6 blocks, a warning ring every six ticks, pale light gathering. | Leave the ring in the wind-up, or be ready to follow him. |
| 3 | `tendrils` Clawed Tendrils | 20 | 24 | 200 | 3-20 | 5 (crowd) | 9 each | Up to four lines locked at the start of the wind-up (the target, then the nearest foes within 20), each 18 blocks long and 1.3 half-wide. The nearest foe on each line takes a spell's hit and, when the Balance Law allows ONE hard drag for the whole fan, is dragged towards him at half a block a tick for up to 10 ticks. Anti-magic cuts it (40% of the hit, no drag). A foe takes at most one tendril. | A lane of orange light and a red line along each tendril for the whole wind-up. | Step out of your line (it is locked, it does not follow); anti-magic. |
| 4 | `specimen` Specimen Mark | 20 | 26 | 360 | 4-30 | 3 | 2.5 / s, 28 | See 4.2. | A ring and a column on the specimen, a pale line from him, the chat names the specimen. | Stand on the thread (an ally), break line of sight, burst him for 6%, anti-magic, or walk away. |
| 5 | `spell_rewrite` Spell Rewrite | 20 | 18 | 380 | 0-48 | 2 | 5 per needle | See 4.1. | A rotating rune ring round him for the wind-up and the 4 s it stands. | Bait with one cheap arrow or spell, then throw the good ones. |
| 6 | `test_subjects` Test Subjects | 20 | 30 | 700 | 0-48 | 2 | none | Calls 2 test subjects (3 at the Gates), up to the framework's cap at his power (1 / 2 / 3 / 4 / 6). | A rune burst and a ring. | Kill them first or ignore them: they are the framework's Disciples. |
| 7 | `restructure` Re-Structure | 40 | 24 | 520 | 0-48 | 2 | none | See 4.1. | An alchemy circle under him for the whole wind-up, a closing ring, the chat names the 5% burst. | Burst him for 5% of his health in the wind-up, or switch kind afterwards. |
| 8 | `pin_down` Gravity Pin | 60 | 26 | 240 | 3-18 | 4 (crowd) | 5 per second | A wedge of 38 degrees half-angle and 16 blocks, locked at the start of the wind-up, that stays 50 ticks (70 at the Gates): slows (up to Slowness IV), presses down, crushes once a second (strength 1.8 at 60%, 2.2 at 80%, 2.6 at the Gates). Anti-magic halves the grip. | Two edge lines at +-38 degrees, a ring at half reach, violet light gathering. | Leave the wedge sideways. |
| 9 | `borrowed_weight` Borrowed Weight | 80 | 28 | 300 | 4-32 | 3 (crowd) | field | Dante's gravity field on the target's spot: radius 8 + stage, 100 ticks (120 at the Gates), strength 1.6 (2.2 at the Gates). | A well on the mark, a warning ring, a ring closing in, a column. | Leave the ring in the wind-up. |
| 10 | `dissection_theatre` Dissection Theatre (ULTIMATE) | 60 | 60 | 1500 | 0-40 | 5 | 12 wall, 30 squeeze | See 4.4. Exposed afterwards (x1.25 for 3 s). | A ring of 16 blocks with three gaps (the doors) in red dust, pale spokes at the doors, a rune field, his order in chat and on the action bar. | Leave through a door before it shuts. |
| 11 | `second_gate` The Second Gate (ULTIMATE) | 100 (Gates) | 66 | 1500 | 0-40 | 5 | 14 per wave | See 4.5. Exposed afterwards. | A devil circle under him and five rings at 7, 11, 15, 19 and 23 blocks. | Jump each wave. |

### 4.4 The Dissection Theatre (the cage and its doors)
* The centre is the middle of the party (within 26 blocks of him), on the floor. Over 120 ticks (6 s) a ring of tendrils closes from 16 to 5 blocks (`CageMath.radius`) with THREE DOORS (`CageMath.doors`): each door is 40 degrees wide
  (a gap of 10.9 blocks at radius 16, 3.4 at radius 5), spread evenly round the ring, and each moved (by at most 50 degrees) to a direction that has standing floor beyond it (floor 19 blocks out, tested in 12 directions) whenever there is one, so a door opens onto somewhere to stand.
* The wall hurts only from inside it: whoever touches it (within 1.25 blocks of the radius) and is not in a door takes 12 base (once every 15 ticks) and is thrown inwards. Whoever stands in a door, or outside, is never touched.
  When the ring has closed, everything within 6.5 blocks of the middle takes the squeeze (30 base, Slowness III for 2 s). The counter is to leave through a door, which is always possible on foot (the ring closes slower than a walk).

### 4.5 The Second Gate
* Five waves, 14 ticks apart, of Dante's stomp wave in Morris's numbers: reaching 7, 11, 15, 19, 23 blocks, 14 base each, travelling 1.1 blocks a tick and hurting each foe once unless the feet are 0.75 blocks off the floor (a jump) or a shield takes it.
  Plus two more test subjects. Lucifero's Flattened Air (no jumping) is never used in this fight, so the jump counter always exists here.

## 6. Numbers

| What | Value |
| --- | --- |
| Health / attack / speed / armor / toughness | 3000 / 14 / 0.30 / 14 / 8 (tier 4 band 3000-3800; the lower end, so Lucifero and Lucius stand above him) |
| Plain melee | 70% of the attack attribute |
| `damageScale` | 1.1 x `morrisDamage` (default 1.0) |
| Hybrid body (Gates) | drawn 1.1 x |
| Hitbox | 0.6 wide, 2.0 tall (the model's height from the manifest) |
| Preferred range | 7 to 14 blocks |
| The damage budget | He is a `ZogratisBoss`: by default 6% of max health per second, 9% in the final phase |
| Floors | 70 to 90 on either road; themes `cursed_archive`, `infernal_foundry`, `shadow`, `gravity`, `astral` (the allocation is ARC-WORLD's) |
| Prerequisites | `dante`, `vanica`, `zenon` |
| Charter Grimoire (30% chance) | `molecular_manipulation`, `analytical_appraisal`, `mana_manipulation`, `chant_annulment`, `thought_acceleration`, `gravity_field`, `gravity_manipulation`, `earth_manipulation` (each id checked against the Tensura id reference by `SpadeRosterTest`) |
| Colours | orange 0xFF9B58 (the colour of Modification Magic in the mod's books), accent 0xFFD9B0; violet 0x5B2A8C / 0xB58CFF once Lucifero surfaces |

## 7. How he keeps the shared rules

* No Unique/Ultimate Tensura skill abuse: none of his moves calls a Tensura skill. The gravity is Dante's own `gravityField` and the Tensura gravity DAMAGE TYPES.
* No free healing: nothing heals him but the framework's slow regeneration by power, as for every boss. Re-Structure only reduces damage (never above 40%), and the framework's step guard and damage budget still apply to what is left.
* Crowd control only through `BalanceLaw.beginControl` (the shared 12 s lock) and `BalanceLaw.controlTicks`: the tendril drag. The thread's pull is slow by design (never beats walking) and does not need the lock.
* Damage only through `bossDamage` / `strike`; no block is ever changed.
* Effects: `Tells`, `ZogratisVfx`, `VfxSpawn` (the mod's Modification Magic rune rings) and the dust of `Tells`; one dust trail on the needles. Sounds are vanilla `SoundEvents`.
* Every timed step is wrapped (`SpadeFx.safe`): a failure is logged once and stops that zone.

## 8. Config, effects, language, icons

* `nusmp-spade-server.toml`, section `[spade]` (`SpadeConfig`, shared): `structuralTuning` (true), `tuningCut` (0.30, 0.05-0.40), `morrisDamage` (1.0, 0.25-3.0); Lucifero's: `flattenedAir`, `kneelSeconds`, `bowGraceTicks`, `luciferoDamage`.
* Effect `nusmp:specimen` (speed -10%, harmful, orange). Language and icons as in `docs/arc/lucifero.md` section 8.

* Trying it (operators, nothing here has been tried): `/multiverse arc summon morris_libardirt` at your feet, `/multiverse arc info morris_libardirt` for the record and its problems. He opens at power 20 and steps up as his health falls (40 under 85%, 60 under 65%, 80 under 45%, the Gates under 25%): Re-Structure shows from 40%, the Pin and the Theatre from 60%, Borrowed Weight from 80%, the Second Gate at the Gates; `morrisDamage` 0.25 makes it survivable while testing and `structuralTuning` false shows the fight without the tuning.

## 9. Model

* `tools/arc/char_morris_libardirt.py`: a slim 2.0-block man, pale skin, closed painted eyes behind round black glasses (the kit draws the frames, a dark lens is added in `extra`), spiky blond hair, a calf-long pale coat with an orange rune on the back, a belt of notebooks,
  an orange cravat, an orb of rewritten structure on his right palm. Form 2 (the Gates): the host: slit violet eyes, a sigil on the forehead, black sleeves and gloves, horns and a small pair of wings, no glasses.
* `ArcLook` (from `tools/arc/manifest.json`, the bigger form for the culling box and shadow): `new ArcLook("morris_libardirt", 2.0f, 0.88f, 1.63f, 0.68f, 1.47f, 0.97f, true)`. Copy them again whenever the spec changes.
* Previews (saved by the builder in its scratch folder `previews/s2/`; rebuild them with `RENDER_PREVIEW_BUILD=<dir> python3 -B tools/gen_arc_characters.py --preview morris_libardirt front back face idle attack cast_big transform death`, and for the second form `python3 -B tools/render_preview/preview.py arc_morris_libardirt_form2_front arc_morris_libardirt_form2_back arc_morris_libardirt_form2_face`): `arc_morris_libardirt_{front,back,face,idle,attack,cast_big,transform,death}.png`, `arc_morris_libardirt_form2_{front,back,face}.png`, and the gallery of the chosen hair style `arc_zz_gallery_hair_spiky_back_g.png`.
* Added for the game, not from the source: the orange rune, the orange trim, the notebooks, the orb, the coat as his scholar's coat (the wiki gives a long light coat to the Paladin outfit). Unverified: the exact coat colour; the glasses as seen from the front.

## 10. The weapon he would give (NOT built; rule 7 of the brief)

A short blade of a scholar, "Libardirt's Scalpel" (a dagger-class 3D model in hand, orange-white glow, a rune ring turning on the blade): `severance` 2 (it cuts along structure), `barrier_piercing` 1,
`magic_interference` 1, `enervation` 1 (the weakness his dissection leaves). Ids the mod already uses in `WeaponEngravings`. Nothing was built.

## 11. Tests, and what was looked at

* Pure classes with JUnit tests (the whole spade package, 85 tests, all pass in the local shim; CI runs the real JUnit): `StructuralTuning` (the cut/hole trade, the ceiling, rotating beats staying), `DamageKind`, `BurstWatch` (windows, thresholds, expiry), `Tether` (every ending, the order they win in, the timings pinned to the documented numbers, the geometry of "on the thread", a pull that never beats walking), `CageMath` (a ring that shrinks slower than a walk, always three doors spread round it and moved to open floor when some lies within 50 degrees, a foe in a door is never touched, a door is wide enough to walk through even at the end), `MorrisTable` (a well-formed table, order pinned, a whole fight played through the framework's scheduler at each power and range), `SpadeRoster`.
* Mutation checks (see `docs/arc/lucifero.md` section 11 for the method; 26 single-line mutations over the package, 11 on Morris's classes and 15 on Lucifero's): the thread's pull ceiling 0.13 to 0.30 (caught by 1 test), the thread's end order with burst before anti-magic (1), the thread's cut time 6 to 1 ticks (SURVIVED at first because the test read the constant it was mutating; a pinning test was added and kills it) and its sight time 30 to 5 (2), the tuning's cut and hole swapped (3), the hole 1.15 removed (1), the tuning's duration 240 to 24 (1), the stage slope of the cut (1), the cage's doors never opening (1), the cage starting 40 wide so that it closes faster than a walk (1), the number of doors 3 to 1 (SURVIVED at first for the same reason; pinned and killed now).
* Type-checked with the stub compiler against the real API dumps (the 17 remaining errors are the known stub artifacts; no real error).
* Looked at: the model previews listed above, rendered by the repo's preview renderer. NOT run: anything in the game; no move seen with its telegraph, no number felt; the hitbox and the culling box were not tried; the icons only as enlarged PNGs.

## 12. For the lead

`ArcRoster.register(MorrisBoss.CHARACTER);` BEFORE Lucifero's line (Lucifero's prerequisites name Morris). See the integration lines at the end of the s2 report and `docs/arc/lucifero.md` section 12.

## 13. Unverified, uncertain, and design choices that are not canon

* All the canon above is from search summaries (no page was opened); the spelling Moris/Morris is not settled by them; which devil he hosts is disputed between the sources.
* The Specimen thread, Structural Tuning, Spell Rewrite, the Dissection Theatre and The Second Gate are GAME rules chosen to express the fight (tendrils that tear the base apart and were stopped by teamwork; a magic that edits structure and was stopped by anti-magic and mixed force; the second gate opening through him). The story has none of them as such. "The strongest of you" as the specimen follows "he wanted to dissect the strongest mages".
* The gravity moves follow the story's order of events (he turned to Gravity Magic after the tendrils failed) but their numbers are the mod's.
* ARENA SIZE: the Dissection Theatre's ring starts 16 blocks wide, the Second Gate's last wave reaches 23, the Gravity Pin 16: they assume the framework's own arena (radius 56) or at least 24 blocks of open floor round him. In a smaller room the cage's doors are moved to whatever open floor there is, but may open onto nothing; ARC-WORLD should give him a roomy arena.
* The coat and the dark lens are design choices (see 9). The Paladin form (Earth Magic, March of Holy Fools) is not used.
* Not run in the game: damage, cooldowns, whether he standing still during the thread is enough of an opening, whether the 12 s tuning is too long, whether the cage's doors are always readable on an arena with irregular walls (the doors are snapped to open floor, but the cage is a ring of dust, not a block structure).
* The dust particle used by the telegraphs and by the needle trails is the vanilla dust type (what `Tells` itself uses); no other vanilla particle type is used.
