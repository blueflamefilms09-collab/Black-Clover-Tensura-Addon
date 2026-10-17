# Zagred, the Word Soul Devil: the remade boss (1.60.0, builder `zag`)

Status: **written and type-checked against API stubs, NOT run in the game.** Every claim below that says "does" describes the code; none of it has been seen working
in a world. Section 12 lists what is unverified.

The owner's request: "remake Zagred over since he sucks, but leave his magic." So this is a new boss under the SAME entity id (`nusmp:zagred`), a new model from the art
kit, and a new fight on the shared boss framework, while the whole magic of Kotodama (`book/KotodamaBook*`, `book/KotodamaWords`, the counters, the skills players use)
is untouched. The old boss stays in the tree and still loads, as `nusmp:zagred_legacy` (rule: never delete).

## 1. Canon used (and what it shaped)

Facts come from the fan wikis, read through search summaries only (the sandbox has no DNS, so the pages themselves could not be opened; nothing here is copied text):

* https://blackclover.fandom.com/wiki/Zagred : the Word Soul (Kotodama) devil, who founded the Eye of the Midnight Sun and destroyed the elves; a tall lean devil with grey
  skin, red irises, horns, bat wings, clawed hands and an arrow-tipped tail; his heart is the weak spot, and he moves it out of the way of a fatal blow; when his body is
  destroyed the heart rebuilds it.
* https://blackclover.fandom.com/wiki/Humans_and_Elves_vs._Zagred and https://blackclover.fandom.com/wiki/Battle_of_the_Shadow_Palace : he was beaten in the Shadow Palace
  by Asta's anti-magic, with the dark dimension slash of Yami and Yuno's wind spirit.
* https://villains.fandom.com/wiki/Zagred : the Word Soul as a power that rewrites reality by speech, and that anti-magic reads nothing of.

| Canon | In the fight |
|---|---|
| Magic is speech: what he says becomes law | Every Word is a command shown on the players' screens (title packets) with its one-line counter underneath |
| Anti-magic is his one answer | Anti-magic cuts a Word being spoken, reaches through both grains of the ward, is never reduced by a closed heart, and spares a player from the mind-words of a verdict |
| The heart is the weak spot and it rebuilds the body | Acts I to IV he is shielded by a ward; at the Gates (act V) the second health bar is the rebuilt body and the heart is the thing to hit (section 5) |
| Bat wings, horns, tail, grey skin, red eyes | The devil form of the model (section 9) |
| The elves' Kotodama is his magic | His Words are his own use of the names the players' Kotodama book also has; the fight never calls the players' spells |

## 2. The shape of the fight

The five acts are the framework's power steps (`PowerCurve`), so the boss bar, the damage budget, the second health bar and the quest keys all behave as for the other bosses.

| Act | Power | Health | Shell | Words and moves that come in | Daemons |
|---|---|---|---|---|---|
| I the Reader | 20% | 100 to 85% | ward, 3 layers | Inked Claw, Halt, Fall, Trident, Redact, Borrowed Art | none |
| II the Speaker | 40% | 85 to 65% | fresh ward, 3 layers | Swords, Seal, Shatter, Banish, Call the Pages | up to 3 pages (scaled by party) |
| III the Judge | 60% (mana unbound) | 65 to 45% | fresh ward, 3 layers | Reverse, Verdict; from here he turns on whoever hurts him most | 4 pages, 1 turret |
| IV the Author | 80% | 45 to 25% | fresh ward, 4 layers | Overwrite (the ultimate) | 6 pages, 2 turrets, 1 Arch daemon (the anchor) |
| V the Heart | 100% (the Gates) | second bar | no ward; the heart | the claw becomes a leap, redact writes two sentences | 8 pages, 3 turrets, 2 Arch daemons |

The Gates are the framework's: a 60-tick invulnerable transformation to the devil form (`gatesForm` = hybrid, `formScale` 1.15) and the second health bar (`PhaseRefill`). It is the
only healing in the fight and it is the documented one (`boss/rules`): it is also the canon "the heart rebuilds the body".

## 3. The Words and their answers

Every Word has a telegraph on the floor or in the air, a title on every screen near him for the whole wind-up, and at least one answer that always works. No wind-up is
shorter than 16 ticks (12 at the Gates, after the framework's 0.75 factor); the ultimate is over three seconds. The counters are the lines under the title.

| Word | Min act | Wind-up | Cooldown | What happens | Answers | Control law |
|---|---|---|---|---|---|---|
| Inked Claw | I | 16 | 70 | a lane of ink bars (5 blocks for the man, who never swings at someone beyond 6.5; 6.5 for the devil, who leaps up to 9 blocks to where it is drawn) | leave the lane; at the Gates keep off the landing ring | none |
| HALT | I | 24 | 300 | a ring closes in; those in the band between 3.5 and 9 (13 at the Gates) blocks are bound | leave the ring, or stand close to him; anti-magic holds the line | `beginControl`, bind capped by `controlTicks` |
| FALL | I | 22 | 320 | fliers are thrown to the floor; a crushing wave runs out in 8 to 12 rings | jump as the wave passes | none (a pull, then 20 ticks of Tensura burden) |
| TRIDENT | I | 20 | 150 | a lance along a 30-block lane written on the floor | leave the line, or raise a shield | none |
| REDACT | I | 28 | 220 | a sentence of 5 to 9 letters on the floor detonates in reading order | step between the letters; anti-magic swung on a letter cuts the rest | none |
| Borrowed Art | I | 18 | 260 | he casts one of the Tensura skills he learned (dark, death, curse, hell, shadow) | the spell's own counter | none |
| SWORDS | II | 26 | 260 | 8 to 14 swords fall on marked spots | raise a shield, or leave the marks | none |
| SEAL | II | 24 | 360 | a chain at the strongest caster; the first body on the line takes it, a wall breaks it | break his line of sight, put a body between, hold anti-magic | `beginControl`, a skill jam through `controlTicks` |
| SHATTER | II | 16 | 380 | a dome for 50 ticks that erases every projectile that is not his and dissolves spell bolts | hold your fire | none |
| BANISH | II | 26 | 420 | a shove that clears 8 blocks around him; picked when four or more foes stand within 7 | do not crowd him | none |
| Call the Pages | II | 30 | 700 | the Grimoire Daemons (pages) tear loose | kill them; the Arch daemon is an anchor | none |
| REVERSE | III | 18 | 520 | for 80 ticks half of every blow turns back on the one who struck (never lethal, capped by `BalanceLaw.cap`, at most once per half second each); he takes the other half; anti-magic passes whole | hold your blows | none |
| VERDICT | III | 24 | 560 | one to three players are named, each with a word of their own | each answers the word over their head (section 6) | `beginControl` for the failures that bind |
| OVERWRITE | IV | 72 (ultimate) | 1500 | a rule is written over the arena for 12 s, held by five stones | break three of the five stones; then he is staggered | none; one of six rules |

He chooses by reading the party (`ZagredAi`): a volley of projectiles gets Shatter, a burst of damage gets Reverse, four foes within seven blocks get Banish, a mage gets Seal,
a flier gets Fall, a knot of ranged fighters gets Halt, and never the same Word twice in a row if anything else is on offer. A move that binds or silences is only picked while
the control law (`BalanceLaw.beginControl`, one hard control per twelve seconds) allows it.

Between two Words he waits at least 5 seconds in act I, then 4.5, 4 and 3.5 (act IV and the Gates), so one title is on the screen at a time and the closed heart has its quiet
(`ZagredMath.wordGap`; the framework's cooldowns shrink to 42% at the Gates, which would otherwise put a new Word up every second or two). In the quiet he claws, casts a
Borrowed Art or calls the pages. Played through the real table, weights and gap (`ZagredPaceTest`, thirty simulated minutes per run, a steady target, no answers
triggered), he speaks 8.8, 9.6, 11.1, 12.0 and 12.1 Words a minute in acts I to V: one every five to seven seconds.

## 4. The ward (acts I to IV)

Three or four layers, each a share (4.5%) of his maximum health, each of one grain: arcane (spells) or kinetic (swords, arrows, fists). The grains alternate; the innermost is
arcane; the outermost breaks first. A layer takes full damage from its own grain and 40% from the other; anti-magic takes 150% from either. When the last layer falls he is
**UNWRITTEN** for three seconds: he cannot speak or move, the Word he was speaking falls apart (not the ultimate), and he takes 150%. Then he writes the ward again, one layer
short for twenty seconds. Every eleven seconds (six with an Arch daemon anchoring him) the weakest layer takes back a third, so a party below that damage rate never breaks it,
and nothing refreshes while he channels the ultimate. An Arch daemon falling, or the third mark of the Last Word, breaks the outermost layer at once. The action bar shows the
layers, their grain and what is left of them once a second.

The global buff engine adds its own rows to every tier-2 boss (`ArcBossProfile`: a magic and a physical barrier, an anti-magic aura, Mortal Fear); they are the owner's system, not part of this fight, and they stack with the ward.

Why: the owner's "break the shield, burst the window". It replaces the old physical-attack immunity, which made melee builds useless: a melee party now pays 2.5 times on the
arcane layers and nothing extra on the kinetic ones, instead of being locked out.

## 5. The heart (act V)

After the Gates he has no ward. The heart is closed: a blow takes 0.45. It opens (1.25) for 3.5 seconds after every Word he finishes (2 seconds with an Arch daemon anchoring him),
for 5 seconds after a Word is cut, after an Arch daemon falls, after three stones break, and as the transformation ends. Anti-magic always takes 1.0. The action bar says
HEART CLOSED or HEART OPEN with the seconds left. The rhythm is "he speaks, you strike", and the more Words the party cuts and anchors it kills, the longer the heart stays open.

Played the same way, the heart is open 38% of the time while Arch daemons anchor him and 67% once they are dead (the average blow is then worth about 0.8 and 1.0 of a plain blow).

Neither number matters to a party that already hits harder than the per-second budget (`BossBudget`, 6% of his health per second): the budget is the real ceiling. The heart
decides the fight for the parties below it.

## 6. Cutting a Word, and why he cannot be stun-locked

* An anti-magic blow while a Word (not the ultimate) is being spoken **cuts** it: he is silenced for 3 seconds, and for 5 seconds after that the next cut does nothing
  (`ZagredComposure`). Two cuts never chain; each buys the party one clear window and no more. Quest key `zagred.cut`.
* Hard control on the party: HALT, SEAL and COWER only land through `BalanceLaw.beginControl` (one per twelve seconds), and the durations go through `controlTicks` (capped, and
  halved on a repeat).
* Hard control on him: the framework refuses paralysis, stun, petrification, sleep, fear, freezing, restraint and stasis (`ZogratisBoss.canBeAffected`).
* Unwritten and the stones' stagger are the only times he stands helpless, and each is earned by the party.

## 7. The Verdict (multi-target)

Up to two players in act III and up to three in act IV and at the Gates are named at once (never more than there are fighters), each with a DIFFERENT word, shown to that player
alone in big letters with their answer under it, for 3.5 seconds in act III, 3 in act IV and 2.5 at the Gates. The server judges the player's last half second (not one tick, so lag does not punish):

| Word | Answer | If missed |
|---|---|---|
| HALT | keep moving (1.2 blocks in half a second) | a hit, and a short bind if the control law allows |
| FALL | jump | thrown to the floor and hit |
| SEAL | break his line of sight | a hit and a skill jam if the control law allows |
| SWORDS | raise a shield | a plain blow a shield would have stopped |
| COWER | look straight at him | a hit, fear and weakness if the control law allows |

Anti-magic spares a player from HALT, SEAL and COWER (the words of the mind). A verdict answered by everyone it named earns a line from him and the quest key `zagred.verdict`.

## 8. Overwrite (the ultimate) and the rules

Five stones (Grimoire Daemons of the stone tier) appear on a ring round him while he speaks. If three are broken before the sentence ends it falls apart: no rule, he is
staggered for two seconds (takes 125%, stands still), and the heart opens. If not, one of six rules is written over the arena for 12 seconds, announced in big letters, and the
stones stay: breaking three still ends it early. Rules: no fire, no ice, no lightning (a blow of that element does not land and the one who struck pays a bruise and a cut of
the spirit), no flying, no standing still, no healing (healing is undone as it happens). Whoever breaks a movement rule pays after a full second, never at once. A rule is never
the same twice in a row. The ultimate leaves him exposed for three seconds (the framework's 1.25).

## 9. The model

`tools/arc/char_zagred.py`, built by the art kit (`python3 -B tools/gen_arc_characters.py zagred`; `--check` reports 0 differences, 0 problems).

* Human form: a tall lean man (build `lanky`, 2.3 blocks), ashen skin, narrow red eyes, a smirk, a rune on the brow, spiked black hair, a long black coat with a violet trim and
  runes, glowing glyph tattoos on the chest, upper arms and forearms, a floating grimoire. Clips: idle `proud`, attack `punch`, cast `magic`, plus the kit's walk, run,
  hurt, death, transform and `spawn`.
* Devil form (`form2`, worn from the middle of the transformation): grey skin, slit red eyes, fangs, cracks, bull horns, bat wings (span 1.3), a devil tail, the otherworldly
  trident on the back, and the glow turned from violet to red.
* `ArcLook("zagred", 2.31, 1.05, 1.95, 1.07, 2.05, 2.27, true)`: the manifest's numbers, the bigger of the two forms for shadow, growXz and growUp. Hitbox 0.55 x 2.31, times
  1.15 in the devil form.
* The old cubiform model (`client/ZagredModel`) stays registered for the legacy boss.
* Kit bug found: `outfit.pattern_color` is not converted by the kit; a hex string or None crashes in `outfit._pattern`, so the spec passes an (r, g, b) tuple. The attack style
  `cast_jab` needs a main-hand prop, so he uses `punch` and carries the trident on his back.
* Previews: `python3 -B tools/render_preview/preview.py arc_zagred_<scene>` and `arc_zagred_form2_<scene>` (scenes: front, back, face, idle, walk, run, attack, cast, cast_big, hurt,
  transform, death, spawn). The Charter Grimoire's art comes from `python3 tools/gen_arc_grimoire_assets.py` (violet cover, red sigil; the model and texture files are in the tree).

## 10. What the files are for

Package `com.newuniverse.nusmp.arc.zagred`. Everything with a pure `Zagred*` name and no game type is unit tested; the glue is not.

| File | Why |
|---|---|
| `ZagredBoss` | the entity: the character record, the moves from the table, the damage rules (rule, cut, Reverse, ward, heart), the 1 Hz upkeep, the hooks, the daemon-master role |
| `ZagredDecrees` | what each Word shows (telegraph) and does (decree), one pair per move, with the same numbers |
| `ZagredScreen` | the Word on every screen: vanilla title packets, every line with a language key and an English fallback |
| `ZagredMark` | tracks one named player over a verdict and turns the last half second into the facts the pure rule judges |
| `ZagredTable` | the 14 moves' numbers, readable without Minecraft; the tests play it through the framework's scheduler |
| `ZagredAi` | the situation to a weight per move (what he answers with what) |
| `ZagredWard` | the layers, grains, the unwritten window, the refresh, save and load |
| `ZagredComposure` | the cut and its lockout: no stun-lock |
| `ZagredHeart` | the closed and open heart, the windows |
| `ZagredWord`, `ZagredAnswers`, `ZagredVerdict` | the Words, the answers and the check of them, the verdict's deal |
| `ZagredRule` | the six rules and the test of a movement rule |
| `ZagredAttention` | his short memory: burst of damage, casters, who hurts him most |
| `ZagredGeometry`, `ZagredMath`, `ZagredLines` | the shapes, every number, his lines |

Edits to existing files (each forced by the remake): `entity/NUEntities` (the legacy type moves to `zagred_legacy`), `entity/ZagredBossEntity` (implements `DaemonMaster`; the
command entry point delegates to the remade boss; `summonLegacy` keeps the old body), `entity/GrimoireDaemonEntity` (asks `DaemonMaster` instead of the legacy class),
`entity/DaemonMaster` (new interface), `item/MagicWeaponItem` (one line: the Last Word's third mark breaks a layer of the remade ward).

## 11. Config, commands, quests, loot

* Config: nothing new. `zagredBossEnabled` (server config) and the game rule `nusmpZagredBoss` still switch `/multiverse boss zagred`; `kotodamaBossReward` still decides the
  Kotodama reward; `arc.enabled` and the floor settings govern his use as a floor boss (Abyssal road, floors 40 to 80, themes cursed_archive, shadow, abyss).
* The command: `ZagredBossEntity.summon` delegates to `ZagredBoss.summon`, so the unchanged line in `MultiverseCommands` summons the remade boss, with his arena at the spot.
* Quest keys: `boss.phase` with arg `zagred:2` to `zagred:5` (the act that starts; the old boss reported 2 to 4, and the existing advancement uses `zagred:2`), `boss.defeated`
  with arg `zagred` (fired by the generic death hook), NEW `zagred.cut` (arg: the Word cut, or `redact` for a letter) and NEW `zagred.verdict` (arg: the Word answered).
* Charter Grimoire (first kill, 30%): `chant_annulment`, `sound_domination`, `voice_cannon`, `magic_jamming`, `thought_acceleration`, `multilayer_barrier`,
  `spiritual_attack_resistance`, `law_manipulation` (all exist in the Tensura id reference; none is a lent-forbidden skill).
* Loot, unchanged: the Heart of Words and one of the Last Word (Barrier Piercing II, Severance I engravings), the Shroud of Margins, the Circlet of Quickened Thought
  (`BossRelics.give`), and the Kotodama reward (`KotodamaWords.reward`) for every fighter.

### Reused, changed, dropped (against the old boss, `docs/zagred_boss_gdd.md`)

| Old | Now |
|---|---|
| Barrier layers with reflex tokens | REUSED as the ward (grains, anchors, the Arch daemon's anchor), without the tokens |
| Redact (sentence of letters, cut by anti-magic) | REUSED (telegraph, letters, the cut), with the party's answer written on the screen |
| Overwrite (rule + five stones, three cut it) | REUSED, six rules, the stagger, the one-second grace |
| Grimoire Daemons (pages, turrets, Arch, stones) | REUSED through `DaemonMaster` |
| Glyph effects, sounds, the trident, Borrowed Art (Tensura spells) | REUSED |
| Physical-attack immunity | DROPPED: it locked melee out; the ward's grains replace it |
| Thought-acceleration dodge, adaptation to the party's element | DROPPED: unreadable, nothing to answer |
| Self-regeneration and the Word HEAL | DROPPED: no healing in combat (`boss/rules`); the Gates' second bar is the documented one |
| The Underworld flood and Devour (block changes) | DROPPED: the Dungeon Guard allows no block change at all; the new boss changes no block |
| The 4 phases at 75/50/25% | REPLACED by the five acts on the framework's power curve |

## 12. Honest list: tested, looked at, NOT verified

Tested (pure logic, 102 tests in `src/test/java/.../arc/zagred`, run with the local shim; CI runs the real JUnit): the ward's arithmetic, the cut and its lockout, the heart's
windows, the answers and the verdict's deal, the geometry, the numbers, the rules, the move table played through the framework's scheduler (what he does at each act, no
distance leaves him idle, the ultimate is rare), the AI's weights, his memory, and the pace of the whole (`ZagredPaceTest`: the real table, weights, speech gap and heart windows together). Three mutations were caught (ward cross-grain, the composure lockout, the AI's control law), and
one in the memory class.

Type-checked against the API stubs: every Java file of the package and the edited legacy files; only the known stub artifacts remain. A deliberate wrong call was caught as a
negative control.

Looked at: the model in both forms, every clip of the preview scenes (front, back, face, idle, walk, run, attack, cast, cast_big, hurt, transform, death, spawn), and the grimoire's contact sheet.

**Not run, not seen:** nothing of the glue has run in a game: the title packets, every telegraph, the ward rings, the heart cue, the daemons under the new master, the
Shatter dome, the verdict judging, the rule enforcement, the command, the reload of a saved fight. Unverified assumptions:

* Damage bases, the ward share (4.5%), the heart windows and every cooldown are starting values; they were chosen against the framework's numbers, not tuned in play.
* `AntiMagic.isMagic` and the damage-type names ("fire", "ice", "lightning") decide the ward's grain and the rule's element; Tensura's own damage-type names were not listed, so a
  type with an unexpected name counts as kinetic and as no element.
* A cut Word's ring and ink bars (sent as one-shot effects) stay for up to a second after the Word is gone.
* Spell bolts are not entities, so Shatter erases them but the AI does not count them as incoming shots.
* The `setTarget` lock (five seconds after a threat swap) assumes the framework's goals only call `setTarget`; a goal that sets the target by another way would bypass it.
* An old world's saved `nusmp:zagred` (the old class) loads as the new boss with the old saved attributes (its health and maximum), power 20%, and no fight; this was not tried.
* The Tensura effect ids `paralysis`, `silence`, `burden` and `fear` are used as elsewhere in the mod; an id that does not exist is ignored.
* `ArcBossRenderer` plays the clips by name (`idle`, `walk`, `run`, `attack`, `cast`, `cast_big`, `hurt`, `transform`, `death`, `spawn`); the kit guarantees them, the renderer was not run.
* The manifest `tools/arc/manifest.json` is shared: the lead re-runs `python3 -B tools/gen_arc_characters.py` after merging every character.
