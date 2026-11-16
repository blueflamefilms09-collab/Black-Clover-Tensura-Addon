# Shinra Kusakabe rework (Hell Walker, Devil's Footprints, Adolla Burst, Hell Conqueror)

Request: "Hell Walker, which is based off of Fire Force, make it into like the main character, like a skill."
Hell Walker (`hell_walker`) is now the Shinra Kusakabe skill. Server setting `shinraRework` (default true) restores the old behaviour.

## What Shinra does in Fire Force, and where each fact came from

Research status (canon pass, v4.5.0 work): the English Fire Force Wiki (fire-force.fandom.com, wikitext through its public MediaWiki api.php) WAS read this time: Shinra_Kusakabe, Rapid, Rapid-Man_Kick, Hellfire, Hysterical_Strength, Adolla_Burst, Adolla_Link, Adolla_Grace, Fourth_Generation, Ignition_Ability, Shinrabanshoman, Knight_Kick (Arthur's, not Shinra's). The Japanese wiki was not needed (Rule 10: English first); the Japanese names in the table are the ones the English pages print. The first rework (v4.4.0) was written without any wiki access; its guesses are listed in "What changed versus the guess".

| Fact | Source page | Used as |
|---|---|---|
| Third Generation; ignites, controls and uses flames from his feet; leaves ash footprints named "Devil's Footprints" (Akuma no Ashiato); resistant to outside flames (pyrokinetics are immune to being burned but still feel heat) | Shinra_Kusakabe (Pyrokinesis), Ignition_Ability | Fire immunity passive, Ignition Dash. The Devil's Footprints are the TRAIL, not an attack. |
| Foot ignition kicks sever limbs or destroy an Infernal's core; an explosive contact kick lifts a car; he changes the path of a kick mid-air for flurries of kicks; flight, gliding on the floor, chained jumps | Shinra_Kusakabe (Pyrokinesis) | Ignition Dash, Devil's Kick (names are the mod's; the wiki lists no named kick other than the Rapid-Man Kick) |
| Rapid (Rapido), Supplementary, short range: the Tora Hishigi seal condenses the flames into one jet, "an exponential boost in speed", flies farther and faster "using up less of his fire"; "kinetic force = mass x speed", so kicks get stronger. Taught by Benimaru; "tenfold speed while reducing the amount of power he uses" | Rapid, Shinra_Kusakabe (Immense Speed, Hand Signs) | New wheel move Rapid |
| Rapid-Man Kick, Offensive, short range: "While using Rapid, Shinra attacks an opponent with his foot", strong enough to destroy an Infernal's core. Nothing else (no damage, cost, range) | Rapid-Man_Kick | Wheel move kept; stronger and cheaper while Rapid burns |
| Corna (hand seal): the flames become explosive, "a lot more force". Hellfire (Jigoku no Honoo), Offensive, short range, parent Corna: darkened flames from his feet in the shape of a demon's head, jaws trap and burn what is inside, then "a massive explosion"; Charon cannot absorb them | Hellfire, Shinra_Kusakabe (Hand Signs) | New wheel move Hellfire |
| Hysterical Strength (Bakajikara, "Superfire"): a Third Generation uses 100 % of the Ignition Ability instead of the usual 30 %; reached through the "Pressure of Death"; flames turn BLUE (higher temperature) and take a controlled CLAW shape; one kick knocked Captain Burns back; later Shinra learned to use it permanently | Hysterical_Strength, Shinra_Kusakabe (Hysterical Strength) | Passive under Hell Mode, blue flame colour |
| Ignition Limit: oxygen fuels the flames; overuse = Overheat (out of breath, burning himself, dehydration), a Third Generation who overuses it starts to char (Tephrosis) | Ignition_Ability (Weaknesses) | Drawback after Adolla Grace |
| Adolla Burst: the original flame from Adolla, held by a few Third Generations; gives a tremendous strength boost; its holders can be linked | Adolla_Burst | Ultimate "Adolla Burst" (the skill); the wiki gives no blast attack |
| Adolla Link: a connection to Adolla or to someone connected to it; with an Adolla Burst it is mild (no scar) and can last minutes; it can be opened on purpose or by strong emotion; senses the emotions and words of others, shares memories, shows the black burning landscape | Adolla_Link | Wheel move Adolla Link (sense), involuntary link under a quarter of health |
| Adolla Grace: a Third Generation's ability enhanced by a Link with an otherworldly being (or with someone already graced), making them Fourth Generation; needs an Adolla Burst; the being can revoke it at any time and, if weak, it lasts as little as one second | Adolla_Grace, Fourth_Generation | Ultimate move Adolla Grace, only through an open Link, then the Ignition Limit |
| Fourth Generation (Shinra): accelerating past the speed of light; the body splits into particles, they go a few seconds back in time and re-form; defeated a Demon Infernal in about a second | Fourth_Generation, Shinra_Kusakabe (Adolla Burst) | Light Speed (the old blink) hits harder under Grace |
| The devil's grin is Shinra's nervous laughter (teeth showing when nervous, stressed or angry), a trauma response that made people call him a devil | Shinra_Kusakabe (Personality, Background) | A TRAIT, not a technique: only the dark mist at low health (look); no stats |
| Shinrabanshoman: soul resonance with his mother and brother, creation and reality warping | Shinrabanshoman | Not implemented (end-of-story form; the wiki gives no numbers) |

NOT given by the wiki (so every number below is design, never canon): any damage, cost, range or cooldown of any move; the Rapid-Man Kick beyond "kick while Rapid, kills an Infernal core"; how long Hysterical Strength or Grace last in seconds; a "Hell Mode" or toggle; an Adolla Burst blast; anything about "Devil's Grin" as a move. Not fetched / not found: the Japanese wiki (not needed), the anime's exact colours (the pages say only "blue" and "dark"; the orange of normal ignition and the cyan of Light Speed are the mod's existing look).

## What changed versus the guess (the canon pass)

| v4.4.0 guess | Canon pass (setting `shinraCanon`, default true) | Why |
|---|---|---|
| Devil's Grin = Speed I under 35 % health | The grin is only a look (the dark mist under half health that Hell Mode already drew). The speed and strength come from Hysterical Strength, which needs a MASTERED skill (it was learned late in the story), triggers under a third of health (Pressure of Death), and at the Ultimate is always on while Hell Mode burns ("learned to use it permanently") | Hysterical_Strength, Shinra_Kusakabe; the grin is a trait |
| Adolla Link = Speed II, Strength I, moves recharge 40 % faster under a quarter of health | Adolla Link is a wheel move that senses hostile minds (they glow, players excluded); under a quarter of health strong emotion opens it by itself (short, small glow); the speed, strength and 40 % guesses are gone | Adolla_Link: senses minds, opened on purpose or by emotion |
| Rapid-Man Kick = a plain short dash kick | Kept as it was without Rapid; while the new Rapid move burns it costs half, reaches 1.3x and hits 1.5x ("less fire", "kinetic force = mass x speed") | Rapid, Rapid-Man_Kick |
| No Rapid, no Corna, no Grace | Rapid, Hellfire, Adolla Grace added | Rapid, Hellfire, Adolla_Grace |
| Adolla Burst Ultimate = dash, kick, blast | Same moves, plus the Shinra wheel and Adolla Grace; Light Speed hits 1.5x under Grace; every move recharges at half time under Grace; the Ignition Limit (Weakness II, Slowness, Hunger II, 6 s) follows | Adolla_Grace, Fourth_Generation, Ignition_Ability |
| "Devil's Footprints" described as the skill's attack | Described as the flame-step trail of the Ignition Dash | Shinra_Kusakabe |
| Colours: orange everywhere | Orange ignition; BLUE flames (0xFF4AA8FF) on the Rapid-Man Kick, Rapid, Devil's Kick and the Hell Mode pulse while Hysterical Strength burns; Rapid is a narrow single jet (width 0.5, not the usual spread); Hellfire is dark red-black with a hell sigil | Hysterical_Strength (blue, claw), Rapid (one jet), Hellfire (darkened flames) |

Not done for lack of any engine support or source: the claw SHAPE of the blue flames (no claw flame shape exists in the effect set; only the colour is used), ash footprints on the ground, Shinra's trajectory control by toes, the Hellfire demon-head model (a flame trail plus an explosion is used), Charon's absorption, Adolla Link between two players (sharing memories, piggy-backing on another's Grace), and the Link scar (Stigma).

### Wheel and unlock order (canon on)

The story's order is Ignition -> Adolla Link (first at the Asakusa/Vulcan arcs) -> Rapid and Rapid-Man Kick -> Hellfire (Corna) -> Grace (Netherworld/Chinese Peninsula arcs, from Shō or the Woman in Black) -> Hysterical Strength (Stigma arc; permanent later) (Shinra_Kusakabe, Plot). The mod has three tiers, so it is a mapping, marked design:

| Tier | Wheel (appended positions never move) | Passives under Hell Mode |
|---|---|---|
| Unique | 0 Ignition Dash, 1 Devil's Kick, 2 Rapid-Man Kick, 3 Rapid, 4 Hellfire (needs mastered), 5 Adolla Link | none (damage curve only) until mastered; mastered: Hysterical Strength under 35 % health: Speed I, Strength I |
| Adolla Burst (Ultimate) | 0 Light Speed, 1 Devil's Kick, 2 Adolla Burst, 3 Rapid-Man Kick, 4 Rapid, 5 Hellfire, 6 Adolla Link, 7 Adolla Grace (needs an open Link) | Hysterical Strength always on: Speed II, Strength II; the old hellfire aura and berserk under low health |

Locked moves are skipped when scrolling and refuse when pressed ("not learned yet").

### Numbers (all design; ratios from the wiki where it gives one)

- Hysterical Strength ratio 30 % -> 100 % = x3.3 is the wiki's; the damage curve (Hell Mode) was already x2.1 to x3.5 at low health and stays; this is why it is not stacked a second time.
- Rapid: Speed II / III / IV for 5 / 7 / 10 s (amp 1 / 2 / 3; the wiki's "tenfold" is not reproducible with potion speed, design), cost 150 (Ultimate 400), cooldown 15 / 13 / 10 s. While it burns: Ignition Dash and Rapid-Man Kick cost x0.5, dash velocity x1.35, kick reach x1.3 and damage x1.5.
- Hellfire: 7 / 8 / 10 blocks (the Unique starts at mastered), jaws 18 / 26 / 36 damage along a 2.2-wide line, then a blast of radius 3.5 (4.5 Ultimate) for 75 % of that, burn 5 s (8 s), cost 450 (900), cooldown 9 / 8 / 5 s.
- Adolla Link: radius 16 / 24 / 32, at most 16 marked, glow 8 s, the Link stays open 30 s (10 s when involuntary), cost 250 (400), cooldown 20 s.
- Adolla Grace: Speed IV, Strength III, Resistance II for 8 s (12 s for a mastered Ultimate when the class split is on; the wiki says only "short, as little as a second"), cost 1200, cooldown 2 min, then the Ignition Limit for 6 s.

## NEW / REPLACED / KEPT (canon pass)

- NEW: moves Rapid, Hellfire, Adolla Link, Adolla Grace; setting `shinraCanon`; `ShinraRules` canon section; `SkillAnimeFx2.devilKick(p, reach, argb)` overload; `tools/lang/shinra_canon.json`; tests; self-test checks.
- REPLACED (owner asked for canon accuracy; all behind `shinraCanon`, false restores v4.4.0 exactly): the Devil's Grin speed buff, the low-health Adolla Link buffs and the 40 % recharge; the descriptions of Hell Walker and Adolla Burst.
- KEPT: every id, every old wheel position (0..3), Hell Conqueror (registered, inherits the Unique wheel at Ultimate tier), the damage curve, the evolution table, `shinraRework`.

## Canon pass: server and cost

- Server only; state is three timestamps in the skill instance (`shinraRapidUntil`, `shinraLinkUntil`, `shinraGraceUntil`), no static maps, nothing to clear on logout, death or dimension change (a Grace that ended while offline pays no Overheat: the due window is 5 s).
- Worst case per press: Rapid one potion effect; Hellfire two entity queries and at most 2 x 24 hits (12 each by `maxTargets`, and the target helper caps a query at 24), one trail, one blast, one sigil; Adolla Link one query and at most 16 glow effects; Grace three potion effects. Cooldowns of 5 s or more (Hellfire 5-9 s, Link 20 s, Rapid 10-15 s, Grace 120 s), so at most 0.2 casts per second per move per player.
- Per second under Hell Mode: one pulse (as before), one blue effect shape at most, and when under a quarter of health one extra query (radius <= 32, 16 marks). Per tick: one long read for the Grace end (`onTick`).
- Packets: effect shapes go through `SkillFx` (nearby players, existing perf budgets); no new packet types. Quads: one extra shape per cast, none per frame.
- Glow marks skip players (no wall-hack in PvP).

## Not verified (canon pass)

Nothing run or seen in a game (no Gradle here, CI compiles). Unverified: that `onTick` of `ToggleAuraSkill` subclasses runs while the skill is not toggled (it does in the base code, which checks the toggle itself); that ManasCore shows eight wheel entries cleanly; how the blue colour reads on `FIRE_BURST` and `ANIME2_SKILL` (colour is passed, shapes unchanged). Lang: the coordinator must merge `tools/lang/shinra_canon.json` (it overrides the two description keys that `shinra_rework.json` also sets, so the existing lines of `en_us.json` must be edited by hand).

## What it is now (v4.4.0 rework; the canon pass above adds to it and, with `shinraCanon` on, replaces its guessed passives)

`hell_walker`, shown as "Shinra Kusakabe" (Unique). One ON/OFF switch for the passives and a wheel of actives (the same pattern as `BeastSkill`).

| Part | What | Origin |
|---|---|---|
| Passive, always on | Your own flames cannot burn you (all fire damage is cancelled) | KEPT from Devil's Footprints |
| Wheel 1 | Ignition Dash (rocket along your sight line, no fall damage from it) | KEPT, folded from Devil's Footprints |
| Wheel 2 | Devil's Kick (burning kick, ignite and knockback) | KEPT, folded from Devil's Footprints |
| Wheel 3 | **Rapid-Man Kick**: a short ignition dash (7 / 8 / 10 blocks) that kicks and ignites every foe on its line, 9 / 13 / 18 damage, 200 / 500 magicules, cooldown 4 s (Ultimate 2.5 s) | NEW |
| Toggle (Hell Mode, 60 magicules/s) | The lower your health the harder you hit (x1 to x2.1 Unique, x2.6 mastered, x3.5 Ultimate), Ultimate heals from blows under half health | KEPT (old Hell Walker, unchanged formula) |
| Toggle, under 35 % health | **Devil's Grin**: Speed I | NEW |
| Toggle, under 25 % health, mastered or Ultimate | **Adolla Link**: Speed II, Strength I (Ultimate II), every move recharges 40 % faster | NEW |

Ultimate `adolla_burst` ("Adolla Burst"): now the Ultimate of Hell Walker (evolution Hell Walker -> Adolla Burst while the setting is on). Wheel: Light Speed (the dash, a blink up to 32 blocks), Devil's Kick (8 blocks), Adolla Burst (the 9-block blast), Rapid-Man Kick. The first three keep their old positions 0, 1, 2 (saved cooldowns stay right). It also has Hell Mode and the grin/link, and the old Hell Conqueror hellfire aura (foes within 4.5 blocks burn each second under half health) and berserk under a third of health are now part of its Adolla Link.

## NEW / REPLACED / KEPT

- NEW: Rapid-Man Kick, Devil's Grin, Adolla Link, `ShinraRules`, `ShinraMigration`, `ShinraSelfTests`, setting `shinraRework`, `tools/lang/shinra_rework.json`, `ShinraRulesTest`.
- REPLACED (owner asked for the rework): Hell Walker's name/description (now Shinra Kusakabe) and role (toggle only -> wheel + toggle); its evolution target (Hell Conqueror -> Adolla Burst, only with the setting on); Adolla Burst's base class (Devil's Footprints -> Hell Walker) and its description.
- KEPT: all four ids, icons and registry entries; Devil's Footprints' dash/kick code (now static helpers used by Hell Walker); the old Hell Walker damage curve; Adolla Burst's three old moves, numbers and positions; Hell Conqueror's class and behaviour; the `m.put(DEVILS_FOOTPRINTS, ADOLLA_BURST)` and `m.put(HELL_WALKER, HELL_CONQUEROR)` lines of the evolution table (the Hell Walker one is overridden by an appended line while the setting is on).

## Decisions

- **Devil's Footprints** is folded in. Not an inert stub: it stays registered and fully working (so nothing breaks if it is given by hand), and `ShinraMigration` (called from the 5-second evolution check) moves a holder to Hell Walker with the same mastery and tag, or just removes it if the player already has Hell Walker or Adolla Burst. `LegacyMigration` was not used (it only forgets inert `LegacySkill` stubs, it cannot grant a replacement).
- **Hell Conqueror** stays registered and keeps working for the players who have it (class KING, hellfire aura, berserk), but nothing evolves into it any more. It also inherits Hell Walker's wheel and fire immunity as an Ultimate-tier Shinra. New players reach Adolla Burst instead. It can be turned into a forwarding stub later if the owner wants it gone.
- Setting `shinraRework=false`: Hell Walker is only the toggle (no wheel, no fire immunity, no grin/link), no folding, Hell Walker evolves into Hell Conqueror, Adolla Burst has its old three-move wheel and no toggle.

## Server and cost

- All rules run on the server; client code is not touched. No per-player static state: cooldowns live in the skill instance, effects expire by themselves (3 s), so nothing needs clearing at logout, death or dimension change.
- Damage goes through `SkillDamage.hit` -> `BalanceLaw.damage` (EP scaling, PvP hit cap, class cap for Adolla Burst). The Hell Mode multiplier also applies to these hits, as it applied to every blow before.
- Worst case, per press: one entity query along a line (Rapid-Man Kick), at most `min(SkillTargets.MAX_FOES, 12)` hits, one velocity packet for the caster, one effect shape and one spray (the existing perf-budgeted `SkillFx` / `SkillAnimeFx2.devilKick`). Cooldown of at least 2.5 s, so at most 0.4 casts per second per player.
- Per second while Hell Mode is on: one pay, up to two potion effects (refreshed, no new entities), one small particle spray; Adolla Burst adds one radius-4.5 entity query and a bounded number of hits.
- Migration: one skill lookup per player every 5 seconds, nothing else.

## Not verified

Nothing was run or seen in a game (no Gradle here, CI is the compiler). Unverified: that ManasCore sends the skill key to `onPressed` while the same skill is toggleable (copied from the working `BeastSkill` pattern); the exact anime effect of the Rapid-Man Kick; wiki facts (see above). Lang: `tools/lang/shinra_rework.json` overrides the existing `hell_walker`, `hell_walker.description` and `adolla_burst.description` keys, which the merge script never replaces, so the coordinator must edit those three lines of `en_us.json` by hand. `SkillAssetsTest.theEvolutionTableIsUnchanged` was changed from 7 to 8 `m.put` lines.
