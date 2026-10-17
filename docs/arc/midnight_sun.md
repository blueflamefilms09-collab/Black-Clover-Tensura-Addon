# The Eye of the Midnight Sun bosses (Black Clover, saga 1: Fana, Patolli, Rades Spirito, Rhya)

Why this page exists: four bosses of the first saga (`ArcSaga.ELF_REINCARNATION`, tier 2) were built together on one small base class, one config and one
Third Eye bond, and what they share is written once here. Each boss has its own page: [fana](fana.md), [patolli](patolli.md), [rades_spirito](rades_spirito.md),
[rhya](rhya.md). Nothing here has been run in a game: every number is a first guess (see "What is NOT verified").

## The four, in story order

| id | who | road and floors | stats (health / attack / speed / armour) | the signature mechanics |
|----|-----|-----------------|-------------------------------------------|-------------------------|
| `rades_spirito` | the Soul Corpse mage of the Royal Capital | Abyssal 30-55 | 1400 / 11 / 0.27 / 8 | the ward of his corpses, the Soul Abductor, the retreat through a gate |
| `fana` | Hatred of the Third Eye, Salamander's summoner | Abyssal 35-60 | 1500 / 12 / 0.30 / 6 | the fireball you can strike back, the Phoenix Feathers Robe, the Evil Heart |
| `rhya` | the Disloyal of the Third Eye, Copy Magic | Abyssal 40-65 | 1450 / 12 / 0.34 / 5 | Copy Magic (the memory), the Invisible Scout, the open Third Eye |
| `patolli` | leader of the Eye (Licht, the elf in William's face), Light Magic | Holy 50-70 | 1900 / 14 / 0.36 / 5 | Overreach, the Light Shaft you can turn back, Heaven's Verdict you can break |

Prerequisites (the story gate, unknown ids are skipped by the roster): Rades after `heath_grice`, `lotus_whomalt`; Fana after `mars`, `vetto`; Rhya after
`vetto`, `fana`; Patolli after `fana`, `rhya`, `rades_spirito`. Both roads are covered by alignment: three Abyssal bosses (the evil road), Patolli Holy.
Zagred (another builder) may list `patolli` as a prerequisite.

## The Third Eye: the decision

Canon (see the sources below): the Third Eye is a trio, Vetto (Despair), Fana (Hatred) and Rhya (the Disloyal), the three strongest of the Eye of the Midnight
Sun, directly under the leader. Each is an elf soul in a human body, marked by a hidden third eye that the forbidden reincarnation magic gave them. Patolli
and Rades Spirito are NOT part of the Third Eye (Patolli is the leader above them, Rades is a member the leader later sacrificed).

So the Third Eye is a multi-body encounter in the story, and the build follows it without a new entity type:

* **Three bosses, one bond.** Vetto belongs to the first builder of saga 1, Fana and Rhya to this group. The roster, the story chain, the Charter Grimoire,
  the boss bar and the floor picker all want ONE boss per record, so a three-in-one entity would have broken every one of them. Instead
  `ThirdEyeBond` (hooked into `ArcBoss.ON_DEFEATED`, installed by the static block of `FanaBoss` and `RhyaBoss`) makes them fight as one: when any of the trio
  is beaten, every other member of the trio that is still standing within 128 blocks takes one rage step (+10% power, the same step the Dark Triad takes, never
  past 80%). A member the roster does not know (Vetto before the first builder's work is merged) simply never takes part. The bond is configurable
  (`thirdEyeBond`).
* **Two phases per member.** Each member of the trio has the Gates (100% power) as "the Third Eye opens": Fana's third eye burns and the Evil Heart wakes,
  Rhya's third eye opens and he holds two stolen magics at once. The model of each has a second form (`form2`) that shows the open eye.
* **`ThirdEyeBond.summonTrio(level, centre, force)`** raises Vetto, Fana and Rhya on a ring of five blocks round a point through the normal summoning rules
  (`ArcSpawner.summon`). Nothing calls it yet: it is there for an arena, a quest or a command (no command file was touched).

## What the four share (`arc/midnight`)

| file | what it is |
|------|------------|
| `MidnightBoss` (glue, abstract, extends `ArcBoss`) | rewrites the framework's devil-voiced lines (the Tree of Qliphoth, "the devil's power") into the character's own, a bar title that names the magic, the OPEN window (see below), safe hooks (`onIncoming`, `guard`, `tickMidnight`, `fightBegins`, `fightEnds`, `frozen`) that never let an exception out of the server tick, helpers for marks, blinks and moves |
| `MidnightFx` (glue) | geometry of the shapes on the world (ring, lane, cone; the SAME `MidnightMath` functions the telegraph uses), blows through the guard (a raised shield halves a blow, anti-magic cuts a spell to 55%), the shared control rule (`BalanceLaw.beginControl`), the detection of what lets a player turn an attack back |
| `MidnightBolt` (glue) | a point projectile that sweeps its segment for foes each tick and can be turned back by a reflector (Patolli's swords, Fana's fireball, Rhya's bolts, Rades's bullets) |
| `ThirdEyeBond`, `RadesThralls` (glue) | the bond (above); the vanilla zombies of Rades (tag, no drops, no experience, never loaded back from disk) |
| `MidnightMath`, `MidnightSpec`, `MidnightText`, `MidnightConfig` (pure) | geometry and fairness laws; the table row and its law checker; text helpers; the config |
| `StaggerClock`, `BreakWatch`, `DamageWindow`, `ReflectRules`, `EyeBondRules` (pure) | the OPEN window, the "take X% of his health before it ends" watch, a bounded damage log, what turning an attack back does, who is in the trio |
| `<Name>Table`, `<Name>Math` (pure) | the move numbers and the counts and radii of each boss |
| `Overreach`, `PhoenixRobe`, `EvilHeart`, `SoulLedger`, `ImitationMemory` (pure) | the signature mechanics, one class each |

**The OPEN window.** Several counters end in "the boss is open": for a short time (45-100 ticks, by what earned it) it starts no move, does half its plain blow
and takes 25% more damage (`StaggerClock.OPEN_BONUS`). A lockout between two windows (default 160 ticks) means a party cannot hold a boss open for ever. The
window is earned by a skill (Patolli: three strikes found only air, a turned-back Light Shaft, a broken Verdict; Fana: a struck-back fireball, a broken or
spent Evil Heart; Rades: nothing, his answer is the ward and the retreat; Rhya: none, his answer is to change what you throw).

**Fairness rules every table obeys** (`MidnightSpec.problems`, tested for all four tables): a wind-up of at least 16 ticks on paper (the framework shortens
it to 75% at the Gates, so 12 at least), an ultimate of at least 40 (30 at the Gates), a recovery after every move (at least 40 and so exposed after an
ultimate), a cooldown of at least 60 (900 for an ultimate), power gates only at the five steps (20, 40, 60, 80, 100), and every mark must be leavable at a walk
in the time it is shown (`MidnightMath.escapeReach`, 0.216 blocks a tick). The telegraph and the strike use the same geometry functions, and an aim that
follows the target locks a few ticks before the strike (the lines turn red).

## Config (`MidnightConfig`, file `nusmp-midnight-server.toml`, section `[midnight]`)

| key | default | what it does |
|-----|---------|--------------|
| `damageScale` | 1.0 (0.1 to 3.0) | multiplier on the damage of every move of the four bosses; the first dial to turn after a first play |
| `thirdEyeBond` | true | the rage step on the Third Eye members when one of them falls |
| `controlEffects` | true | allows the crowd-control effects of the four (roots, blindness); they still obey the balance law's shared cooldown and caps |
| `reflectBackfire` | true | Patolli's Light Shaft and Fana's fireball can be turned back by Full Reflection, a mirror array or (fireball only) an anti-magic swing |
| `thrallLimit` | 6 (0 to 12) | most risen corpses of Rades alive at once (his power step may allow fewer); 0 = he raises none |
| `staggerLockoutTicks` | 160 (0 to 2400) | ticks between two OPEN windows of one boss |
| `evilHeartTicks` | 150 (60 to 600) | Fana's Evil Heart: ticks from the sphere's birth to its detonation |
| `evilHeartBreakPercent` | 6.0 (1 to 40) | percent of Fana's maximum health that must be taken during the Evil Heart to break it |
| `verdictBreakPercent` | 5.0 (1 to 40) | percent of Patolli's maximum health that must be taken during Heaven's Verdict to break it |

Every key is a plain `define` / `defineInRange` read through `MidnightConfig.get(...)`, which answers the default while the spec is not loaded yet (so a boss
spawned during loading never throws).

## LEAD INTEGRATION (exact lines)

1. `ArcCharacters.bootstrap()` (story order; each line also loads the class, which registers the entity type):
   ```java
   ArcRoster.register(com.newuniverse.nusmp.arc.midnight.RadesBoss.CHARACTER);
   ArcRoster.register(com.newuniverse.nusmp.arc.midnight.FanaBoss.CHARACTER);
   ArcRoster.register(com.newuniverse.nusmp.arc.midnight.RhyaBoss.CHARACTER);
   ArcRoster.register(com.newuniverse.nusmp.arc.midnight.PatolliBoss.CHARACTER);
   ```
2. `NUSMP.java`, next to the other server configs:
   ```java
   modContainer.registerConfig(ModConfig.Type.SERVER, com.newuniverse.nusmp.arc.midnight.MidnightConfig.SPEC, "nusmp-midnight-server.toml");
   ```
3. Language: merge the fragment `arc/frag/s1b.lang.json` of the scratchpad (four `entity.nusmp.<id>` names) into `assets/nusmp/lang/en_us.json`.
4. Charter Grimoires: run `python3 tools/gen_arc_grimoire_assets.py` once the four lines of step 1 are in.
5. Renderers: nothing to do (`ArcClient` registers one for every boss of `ArcEntities`). The models, animations and textures are generated and committed
   (`python3 -B tools/gen_arc_characters.py --check` passes).
6. Optional: a command or an arena may call `ThirdEyeBond.summonTrio(level, centre, force)`.
7. New themes that other builders add (`infernal_foundry` for Fana, `drowned_crypt` for Rades) are only hints: an id the mega-dungeon does not know never matches.

## Rades Spirito and the existing Soul Corpse caster

The repository already has a player-facing Soul Corpse Magic book and three named corpses (`corpse` package: Jimmy, Alfredo, Carl, with the Rades-only pages
in `book/CharacterSpells`). That is the player's caster and it was NOT touched. The boss is a separate entity with its own corpses (vanilla zombies); the two
only share the lore. A note for whoever owns the caster: in the story Jimmy is the gunner of the cursed bullets and Alfred (the caster calls him Alfredo) the
corpse wrapped in lightning, while `NamedCorpses` gives Jimmy the brawler role and Alfredo the bruiser role. The boss uses the story's roles.

## What was tested, and what was not

* **Tested (pure, JUnit; `arc.midnight` has 70 cases, all passing with the local shim)**: the geometry edges (lane ends, ring rim, cone corner, behind a body),
  the fairness laws on all four tables, the counts that rise with power, every mark leavable in its warning time, the OPEN window clock, the break watch, the
  damage window, reflection rules, the Third Eye rules, the text rewrite, and each signature mechanic (Overreach, the Phoenix Robe's elements, the Evil Heart
  countdown and falloff, the ward and the ledger and the retreat, the imitation memory). One test was mutation-checked on purpose (the resonance of Rhya's
  memory forced to 1.0: `RhyaTest.whatHeKnowsHurtsHimLess` fails, then the file was restored).
* **Type-checked** against the API stubs of the build (all glue and boss classes): clean except the two known stub artefacts (`MobEffectInstance` shown as
  abstract, `deserializeNBT` on an entity subclass). A deliberate wrong call was checked to be reported.
* **Built for real**: the four `ArcCharacter` expressions were evaluated in a small program (tier 2, health band, 6 to 9 Charter skills, every skill id
  found in the Tensura id list, three lines each).
* **Looked at**: in the last pass, the front view of both forms of all four, a sheet of all eight faces (first and second form of each), the idle, cast and death
  clips of all four, the attack clip of Fana, Patolli and Rhya, the cast_big clip of Fana and Rades, and the back view of Fana. Rendered and saved but not examined
  one by one in the last pass: the back views of the other three, Rades's attack, the cast_big clips of Patolli and Rhya (40 frames in all in the scratchpad
  `previews/s1b`: front, back, face, idle, attack, cast, cast_big, death for the first form, front and face of the second).
* **NOT done**: no game was started. No move was seen working; no number was balanced; no client rendered a model.

## What is NOT verified (honest list)

1. Nothing ran in a game. Balance (the base damage of every move, cooldowns, the 70% resonance, the 42% ward cap, the 6% and 5% break shares) is a first guess; use
   `damageScale` first.
2. How the grimoire hit is stamped on the target (`CopyMemory`, keys `nusmp_copy_book` / `nusmp_copy_at`) is read in the same tick as the hit, as the code
   of `GrimoireBook.hurtAs` suggests; if a grimoire page ever hurt before stamping, Rhya would learn one hit late.
3. The detection of "this player can turn an attack back" uses the persistent key `nusmp_full_reflect`, `MirrorWorks.mirrorsLeft` and `Player.swinging` with a
   held anti-magic item; how the owners of those systems set them was read from the code, not played.
4. Rhya's Invisible Scout uses the vanilla invisibility effect. If the arc boss renderer ignores the effect the boss stays visible and the move is still fair
   (its arrival ring is always drawn); if it honours it the boss vanishes for the last half second of the wind-up (up to three seconds if the move is cut short, the effect then ends by itself).
5. Rades's zombies: the drop and experience cancelling and the "never loaded back from disk" rule use the NeoForge events as documented; not exercised.
6. The sounds and effect shapes (`VfxShape` names) were chosen by name from the code; none was heard or seen in a game.
7. Theme ids `infernal_foundry` and `drowned_crypt` belong to another builder's merge; until then they are inert.
8. The art kit: the idle clip `guard` needs a prop in the right hand to read well (the arm rule complains with a left-hand-only prop), and `cast_jab` needs a
   hand prop; Fana's spec works around both. The kit was not edited.

## Weapons

None of the four gives a weapon (they are not members of the Seven Deadly Sins). Nothing was built for the owner's weapon rules; the Charter Grimoire is the
only drop and it comes from the shared charter tool.

## Sources (canon; found by web search, the pages themselves could not be fetched from the build machine)

Facts were taken from the summaries the search returned, and where sources disagree the page says so. Main pages:
https://blackclover.fandom.com/wiki/Eye_of_the_Midnight_Sun , https://blackclover.fandom.com/wiki/Patolli , https://blackclover.fandom.com/wiki/Fana_(elf) ,
https://blackclover.fandom.com/wiki/Rhya , https://blackclover.fandom.com/wiki/Rades_Spirito , https://blackclover.fandom.com/wiki/Vetto ,
https://blackclover.fandom.com/wiki/Julius_Novachrono_vs._Patolli , https://blackclover.fandom.com/wiki/Asta_vs._Rades_Spirito ,
https://blackclover.fandom.com/wiki/Asta_and_Mars_vs._Fana ,
https://blackclover.fandom.com/wiki/Asta,_Vanessa_Enoteca,_Finral_Roulacase,_and_Noelle_Silva_vs._Fana ,
https://blackclover.fandom.com/wiki/Mereoleona_Vermillion_vs._Rhya , https://blackclover.fandom.com/wiki/Category:Light_Magic_Spells ,
https://blackclover.fandom.com/wiki/Soul_Abductor , https://tvtropes.org/pmwiki/pmwiki.php/Characters/BlackCloverEyeOfTheMidnightSun ,
https://comicon.com/2018/06/17/5-point-discussions-black-clover-36-three-eyes/ , https://comicon.com/2018/11/27/5-point-discussions-black-clover-59-flames-of-hatred/ ,
https://villains.fandom.com/wiki/Patolli , https://villains.fandom.com/wiki/Rades_Spirito , https://villains.fandom.com/wiki/Rhya ,
https://baike.baidu.com/en/item/Fana/3449269 , https://baike.baidu.com/en/item/Patolli/3271402 ,
https://www.soapcentral.com/anime/black-clover-everything-you-need-to-know-about-the-eye-of-the-midnight-sun ,
https://otakuen.com/blogs/article/power-of-rhya-raia-black-clover-copy-paster-of-magic ,
https://otakuen.com/blogs/article/the-power-of-patolli-patri-in-the-black-clover-anime ,
https://animehunch.com/black-clover-the-reincarnation-of-the-elves-patolli-explained/ ,
https://www.sportskeeda.com/anime/all-eye-midnight-sun-members-black-clover-ranked-based-strength ,
https://animenewsnetwork.com/review/black-clover/episode-88/.148101 .
