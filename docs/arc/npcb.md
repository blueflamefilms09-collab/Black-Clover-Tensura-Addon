# The other Magic Knights and the court (builder NPC-B, 1.60.0)

Sixteen friendly characters of Black Clover for the Sanctuary Hub: the Golden Dawn (Yuno, William Vangeance, Mimosa Vermillion, Klaus Lunettes), the Crimson Lion
(Fuegoleon, Mereoleona, Leopold Vermillion), the Silver Eagle (Nozel Silva, Solid Silva), three more captains (Charlotte Roselei of the Blue Rose, Jack the Ripper of
the Green Mantis, Dorothy Unsworth of the Coral Peacock), the two siblings of the Seabed Temple (Kahono, Kiato; the mod's exclusive casters of the Song and Butoh
books) and the court (Julius Novachrono, Marx Francois). They are NPCs of the shared `nusmp:arc_npc` entity (`docs/arc/core.md`): a record, a dialogue file and a
model. This document is the WHY of every file, the canon the lines rest on (with the URLs), the numbers, and an honest list of what is unverified.

**State.** Written without a game and without a compiler for the whole project. What was run: the pure JUnit tests of this builder (in the local shim, one
mutation check by hand), the type-check of the Java classes against the API stubs, the language-key tool. **Phase A only**: the records, the dialogue and the
language file are done; the 3D models wait for the art kit (section 10).

## 1. New, replacement, kept (rule 5)

| Part | Status |
|---|---|
| The sixteen records (`arc/knights/*Cast.java`) | NEW. Nothing like them existed: the roster is empty before the character builders merge. |
| The sixteen dialogue files `data/nusmp/arc/dialogue/<id>.json` | NEW. `_fallback.json` (ARC-CORE) is untouched and still serves any character without a file. |
| Language keys `arc.dialogue.<id>.<n>` (354) | NEW, in `$S/arc/frag/npcb.lang.json` for the lead to merge. |
| Tests `KnightsCastTest`, `KnightsDialogueTest` | NEW. |
| The canon grimoires of the same people (`grimoire/CanonBook`) and the exclusive casters Kahono and Kiato (`docs/exclusive/kh.md`, `kt.md`, `book/CharacterSpells`) | KEPT, not touched. The records only borrow their colours and the dialogue only points at their pages. |
| Models (`tools/arc/char_<id>.py`) | PENDING (Phase B, art kit not merged into this tree yet). |

No old model, texture, effect, shape, page or skill was touched. Nothing was removed. No weapon is built or handed out (the owner's rule 6/7 are about weapons;
none of these characters gives one), so there are no engravings to list.

## 2. The cast at a glance

Order = `KnightsCast.ALL` = the order the Sanctuary meets them (the court and the juniors from the first day, the rest as their gate opens).

| id | Name, epithet | Squad / place | Magic (`MagicType` id) | Name colour / accent | Appears after | What they teach |
|---|---|---|---|---|---|---|
| `julius_novachrono` | Julius Novachrono, the Wizard King | the court | time | E8DCC0 / D8B050 | nobody | the story as a whole, the Chronicle, the Lucius arena (recipe, companions, his own will, how precognition fails) |
| `marx_francois` | Marx Francois, Aide to the Wizard King | the court | memory | 8AA0D0 / D8E0F0 | nobody | the rules of the dungeon: parties, the System, Guardian floors, stairs, wanderers, the Charter, the arena logistics |
| `yuno` | Yuno, the Wind Prodigy | Golden Dawn | wind | 7FD6A0 / E8D890 | nobody | the spirit bond and Spirit Dive, mana strain |
| `mimosa_vermillion` | Mimosa Vermillion, the Golden Dawn's Healer | Golden Dawn | plant | E0D468 / 8CC86A | nobody | resting in Safe Zones and Landings, food, curses |
| `klaus_lunettes` | Klaus Lunettes, the Golden Dawn's Strategist | Golden Dawn | steel | 9AB0C0 / D8E0E8 | nobody | reading telegraphs, positions, noise and light, how to beat precognition |
| `leopold_vermillion` | Leopold Vermillion, the Crimson Lion's Youngest Flame | Crimson Lion | flame | F08040 / D8A848 | nobody | encouragement for the first fights, warming up |
| `fuegoleon_vermillion` | Fuegoleon Vermillion, Captain of the Crimson Lion | Crimson Lion | flame | E0502C / E8B850 | `revchi_salik` | the fire spirit, telegraphs, fighting as a party |
| `charlotte_roselei` | Charlotte Roselei, Captain of the Blue Rose | Blue Rose | briar | 4A90C8 / C8D8E8 | `revchi_salik` | traps, noise and light, protecting the weakest member |
| `nozel_silva` | Nozel Silva, Captain of the Silver Eagle | Silver Eagle | mercury | C8CCD8 / 8890A8 | `heath_grice` | defence, telegraphs, fighting Lucius as a unit |
| `solid_silva` | Solid Silva, the Silver Eagle's Second Son | Silver Eagle | water | 6AA0E0 / C8D8F0 | `heath_grice` | mana economy, pulling back to return stronger |
| `jack_the_ripper` | Jack the Ripper, Captain of the Green Mantis | Green Mantis | slash | 3CB074 / C8E0C8 | `heath_grice` | cutting through barriers, how to beat precognition (four cuts in a second) |
| `dorothy_unsworth` | Dorothy Unsworth, Captain of the Coral Peacock | Coral Peacock | dream | E88AA0 / F0D8E0 | `lotus_whomalt` | proper rest, calm |
| `william_vangeance` | William Vangeance, Captain of the Golden Dawn | Golden Dawn | world_tree | E0D0A0 / C8A050 | `lotus_whomalt` | trust and second chances, the Eye of the Midnight Sun's grief, Julius in the Lucius fight |
| `mereoleona_vermillion` | Mereoleona Vermillion, the Crimson Lioness | Crimson Lion | flame | E8602A / E8B850 | `mars` | Mana Zone, Mana Strike, training |
| `kahono` | Kahono, Priestess of the Seabed Temple | Seabed Temple | song | C890D8 / E8D8F0 | `vetto` | the Song pages (voice or the Sing key; the four modes) |
| `kiato` | Kiato, Dancer of the Seabed Temple | Seabed Temple | butoh | 78D0E8 / B8E6FF | `vetto` | stepping out of blows, turning missiles, a boss's rhythm |

Colours: the cover and trim of the character's own canon grimoire (`grimoire/CanonBook`) where it has one, lightened when the cover is too dark to read as a
name in chat (the name is drawn in this colour, bold, on a dark chat; `KnightsCastTest.coloursAreReadableInChatAndDistinctFromEachOther` keeps every colour
bright enough and all sixteen different). `voice` in each dialogue file is the same colour (`theVoiceIsTheRecordsColour`).

## 3. Corrections to the brief (read this, lead)

The brief grouped Jack the Ripper with Kahono and Kiato in "Purple Orca" and put Dorothy in the "Green Mantis". The series, and the mod's own `CanonBook`, say:

* **Jack the Ripper** is the captain of the **Green Mantis** (Slash Magic). The Purple Orca's captain is Kaiser Granvorka (who has a canon grimoire in the mod and is not in this brief).
* **Dorothy Unsworth** is the captain of the **Coral Peacock** (Dream Magic, vice-captain Kirsch Vermillion).
* **Kahono and Kiato are not Magic Knights**: they are the brother and sister of the **Seabed Temple** (the fifth arc), a priestess whose Song Magic heals and her older brother, a
  dancer-warrior whose Butoh Magic is a sword-dance. Both have dreamed of the surface; the Sanctuary is the first open sky they have. The titles follow the series.
* Silver Eagle / Blue Rose: Nozel and Solid are the Silver Eagle, Charlotte is the Blue Rose; they are in different files here for that reason.

`KnightsCastTest.theSquadsFollowTheSeriesNotTheOwnersFirstGuess` pins this. The owner may prefer the brief's grouping; it is a one-word change per record, and
nothing else depends on the squad name.

## 4. Files and the WHY of each

| File | What and why |
|---|---|
| `arc/knights/KnightsGates.java` | The five champion ids the Hub gates wait for, spelled once. Why: a typo in a prerequisite would make a character wait for nobody for ever; a rename of a champion would be one edit. |
| `arc/knights/GoldenDawnCast.java`, `CrimsonLionCast.java`, `SilverEagleCast.java`, `CaptainsCast.java`, `SeabedTempleCast.java`, `CourtCast.java` | The `ArcCharacter.npc(...)` records, grouped by squad. Why grouped: a record is eight lines of data; a file per character would be sixteen files of boilerplate, while one squad shares a palette, a gate rule and a reason to exist. No game types, so they are unit-testable. |
| `arc/knights/KnightsCast.java` | `ALL` (the Sanctuary's order) and `registerAll()`. Why: one line for the lead in `ArcCharacters.bootstrap()`, and one answer to "who is in this cast" for the Hub, the tests and this document. |
| `data/nusmp/arc/dialogue/<id>.json` x16 | What each character says (section 5). Plain data, a datapack can replace a file. |
| `src/test/.../arc/knights/KnightsCastTest.java`, `KnightsDialogueTest.java` | The checks of section 7. |
| `$S/arc/frag/npcb.lang.json` | 354 language entries, made by `tools/gen_arc_dialogue_lang.py` from exactly these sixteen files. |

Stats on the records: `health 0, attack 0, speed 0.25, armor 0` (an NPC has no boss stats; `ArcNpcEntity` uses its own attributes), `tier` left at 1 (only bosses read it),
`saga ALLIES`, `source "Black Clover"`, no floors, no Charter Grimoire.

### The gates (when the Hub places each one)

Ten of the sixteen wait for one of the five opening champions of the first saga (ids fixed by the Saga 1 builders' briefs: `revchi_salik`, `heath_grice`, `lotus_whomalt`,
`mars`, `vetto`); six (the court and the four juniors) stand in the plaza from the first day. Why: the Sanctuary grows with the delver, and the people who were hurt by
a champion arrive when it falls (Kahono and Kiato after Vetto). If an id is renamed or a builder never merges, the story rules treat the prerequisite as satisfied
(`StoryRules`: "an id that is not in the roster is skipped"), so the character simply stands in the Hub from the start; nothing can lock them out.

## 5. How the dialogue works, and why it is written this way

The engine (`docs/arc/core.md` section 3): the first meeting greets; afterwards a right-click gives a **hint**; the farewell is spoken when the conversation ends.
Among the lines whose conditions hold, the highest `priority` wins and the choice among those is random by `weight`, skipping the line said last time.

* **All hints share priority 1.** With the engine's default (priority = number of conditions) the single most specific line would be said at EVERY click for as long as
  its conditions hold. Giving the hints one priority makes them mix. `KnightsDialogueTest.hintsShareOnePriorityOtherwiseTheMostSpecificLineWouldBeSaidForEver` pins it.
* **Weights bias the mix toward the story.** Lines about the player's situation have weight 4, character lines weight 1, so between two and three clicks in five (more for the court,
  which has several lines per saga) are about where the player is, and the rest are the character talking. Situations: `maxStage 0` (beaten nothing), `saga elf_reincarnation` with `minStage 1`, `spade_raid`, `wizard_king`,
  `seven_deadly_sins` and `danmachi` (the saga is "the earliest saga with a champion left", so each band lasts until the player has cleared it), and `defeated` reactions to
  Heath Grice, Vetto, Zagred and Lucius (ids that exist for certain or are fixed by a brief).
* **Never silent.** Every character has at least three hints at every point of the story (generic lines fill the gaps) and a hint written for each of the five sagas
  (`aCharacterIsNeverSilentAndNeverRepeatsAtOnce`, `everyCharacterPointsAtTheStoryOfEachSagaAndAtTheFirstSteps`).
* **Greetings.** Six per character: three for a stranger, one for a stranger who already has a name in the dungeon (`first` + `minStage`), two for a friend seen again. The
  returning greetings are fallbacks only (the engine says a hint to a friend; a greeting is reached when no hint fits). The self-test `arc.dialogue` asks for a greeting at
  stage 0 of the first saga: every file has one with no other condition.
* **No quest and no shop lines, on purpose.** `TalkPlan` offers a quest the first time a book has a `quest_offer` line and moves the stage to "offered"; nothing in the tree
  ever moves it on to "done" except the operator command `/multiverse arc story <player> quest <npc> <stage>` (`ArcStory.setQuestStage` is for "whatever hands out the quest", and nothing does),
  so such a line would promise a task nobody can finish.
  A shop line is never chosen at all. `nobodyPromisesAQuestOrAShopThatNothingCanDeliver` fails the day somebody adds one, as a reminder to add the system first.
  The brief's "role" of a character is therefore its **hint topics** (the column "What they teach" above), each checked against the mod (section 6).
* **Placeholders.** `{player}` (about one line in four), `{saga}` in two lines (Julius's greeting for a veteran stranger, Marx's "the records place you in: {saga}").
* **Plain ASCII, short, no repeats, no quotes.** Chat-friendly (200 characters at most), no `%` (a language template doubles it), no double quote.
* **Original wording.** No line is a quotation from the series; they are written to fit each character's manner. Catchphrases are not used.

### How to change a line

Edit the JSON, then `python3 tools/gen_arc_dialogue_lang.py --write-keys` (numbers new lines), regenerate the fragment from the sixteen files (`--dir <folder with only
these files> --out npcb.lang.json`), run the two tests. The authoring script I used (a compact Python spec per character) is not part of the repository; the JSON is the
source of truth.

## 6. What the lines say about the mod, and where each claim was checked

| Said by | Claim | Checked in |
|---|---|---|
| Julius, Marx, Yuno, Klaus, ... | the Chronicle in the plaza lists the stories and which fights are open | `docs/arc/core.md` (story gate), `$S/arc/arcworld.md` (the Chronicle is BUILT BY ARC-WORLD, not in this tree) |
| Julius | the Hourglass of the Final Hour is crafted from gold, a nether star, amethyst, a clock, diamonds and an ender pearl; Lucius can only be called to his arena | `data/nusmp/recipe/zogratis_hourglass.json`, `docs/lucius_arena.md` |
| Julius, Marx | those within eight blocks travel with the holder (seven companions at most); the arena is closed to latecomers; leaving mid-fight takes ten seconds | `docs/lucius_arena.md` (Hourglass section) |
| Julius, William | at 75%, 55%, 35% and 12% Julius's will surfaces for a few seconds | `entity/zogratis/LuciusBoss.java`, `JuliusWill.java` |
| Julius, Klaus, Jack, Yuno, Nozel, Mereoleona | precognition fails against two attackers, a very fast attacker (four blows in a second) and anti-magic | `docs/lucius_arc.md` (Precognition table) |
| Marx, Klaus, Fuegoleon, Nozel | every harmful attack is telegraphed | `docs/boss_rules.md`, `LuciusChains.valid` (12 ticks minimum) |
| Marx | a team shares one chain of floors; the System's bonuses sleep outside the dungeon; every tenth floor has a Guardian, a Gatekeeper door and a vault; "the stairs are still forming" | `docs/labyrinth/mega_v2.md` sections 1 to 3, `travel/StairTravel.java` (the message) |
| Marx | a Charter Grimoire falls to the first defeat of a champion, once per world, by chance; held, it lends its skills | `docs/boss_overhaul.md` (Charter section) |
| Mimosa, Dorothy, Mereoleona, Jack, Marx | only Landings and Safe Zones restore you; nothing mends easily elsewhere | `docs/labyrinth/mega_safe.md` |
| Marx, Klaus, Charlotte | wandering champions; noise and light draw attention | `docs/labyrinth/mega.md` (systems 3) |
| Yuno, Fuegoleon | a spirit lends power by trust; Spirit Dive has a strain | `book/SpiritBond.java`, `book/GrimoireBook.java` |
| Mereoleona | the Mana Zone and Mana Strike pages exist | `book/GrimoireBook.java` (`STRIKE_ID`, `ZONE_ID`) |
| Kahono | Song magic is sung with the voice (Simple Voice Chat) or the Sing key; Healing Hymn, Lullaby, Barrier Chorus, Requiem; the Crescendo page answers only to Kahono's own grimoire | `docs/exclusive/kh.md`, `book/CharacterSpells.java` |
| Kiato | dodging, shrinking, turning missiles; anyone who carries the Butoh book dances by itself (the passive) | `docs/exclusive/kt.md`, `docs/exclusive/fx3.md` section 1 |
| Marx | some pages answer only to the grimoire of one particular person | `book/CharacterSpells.java` |
| Julius | Chrono Stasis holds a target for a few seconds at most | `book/TimeBook.java` (`BalanceLaw.controlTicks(.., 60)`) |
| Everybody (late bands) | the Chronicle opens pages from other worlds | `docs/labyrinth/mega_v2.md` section 6 (the Seven Deadly Sins, DanMachi sagas) |

## 7. Tests

Run: `cd <worktree> && WT=<worktree> KEY=npcb bash $S/mega/jt.sh com.newuniverse.nusmp.arc.knights.KnightsCastTest com.newuniverse.nusmp.arc.knights.KnightsDialogueTest`
(the dialogue test reads `src/main/resources/...` relative to the worktree, like `DialogueFilesTest`). 19 tests, all pass in the shim; `DialogueFilesTest` (ARC-CORE) passes with
the sixteen new files in the folder.

* `KnightsCastTest` (8): sixteen distinct NPC records of the first saga's friends; no boss data; the gates name only the five openers (ten gated); the court and the juniors wait
  for nobody; the siblings and the Silvas arrive together; the squads follow the series; colours readable and distinct; `registerAll` twice is harmless and a missing champion is skipped.
* `KnightsDialogueTest` (11): every book parses with no problem and has 18 lines at least; the files belong to their records; greetings (four at least, three for strangers, two for
  friends, one for a veteran stranger; a newcomer is greeted, a friend gets a hint); never silent at nine points of the story; a hint for each saga and for a fresh start; hints share
  one priority; no quest or shop line; conditions name only known champions and sagas; lines short, ASCII, unique; voice = record colour; every line has its key in the generator's numbering.
* Mutation check by hand: Kiato's gate changed to `mars` (caught by `theSeabedSiblingsArriveTogetherAfterVetto`) and Yuno's `spade_raid` hint changed to `spade_raidx` (caught by two
  dialogue tests); both restored.
* The Java classes type-check with `tc_wt.sh` (0 errors in the closure). They use only `ArcCharacter.npc(...)`, `ArcRoster.register` and `java.util.List`.

## 8. Lead integration

* `arc/ArcCharacters.bootstrap()`: `com.newuniverse.nusmp.arc.knights.KnightsCast.registerAll();` (or the sixteen lines `ArcRoster.register(<Squad>Cast.<NAME>);`, constants listed in section 2 by id).
  The order of the roster is the order of registration; the first NPC registered is the one the `arc.spawn` self-test places.
* Language: merge `$S/arc/frag/npcb.lang.json` (354 keys `arc.dialogue.<id>.<n>`); afterwards `python3 tools/gen_arc_dialogue_lang.py --check <en_us.json>` must exit 0.
* No registration, no config key, no command, no creative-tab item, no client registration, no quest-hook key of its own (the Hub and the talk code fire CORE's `arc.met` and `arc.talk`
  with these ids). `/multiverse arc spawn_npc <id>` places any of them; the spawn egg carries the id.
* ARC-WORLD (Hub posts): suggested layout, not binding: Julius and Marx beside the Chronicle; Yuno, Mimosa, Klaus and Leopold on the plaza; the captains along the gate road. The gates are in the
  records (`prerequisites`); nothing else is needed from the Hub.

## 9. Canon used, per character (and what was left out)

How the research was done: the page-fetch tool could not resolve any host in this session, so **no page was opened**; the facts come from the summaries the search tool returned for the
queries below, cross-checked against the mod's own `CanonBook` and `CharacterSpells` and against my memory of the series. Search results from content-farm mirrors were ignored
where they disagreed with a wiki or a database.

| Character | Facts used | Left out on purpose |
|---|---|---|
| Yuno | Golden Dawn knight; Wind Magic; the wind spirit Sylph (Spirit of Zephyr); calm, composed, competitive rival of Asta, both aim for Wizard King | his later power (Star Magic) and his past |
| William Vangeance | captain of the Golden Dawn; World Tree Magic; composed and courteous; wore a mask; shared his body with the elf spirit Patolli and betrayed his Wizard King, a debt he now answers | everything after the betrayal (spoilers); the exact scenes |
| Mimosa Vermillion | Golden Dawn; Plant Magic and healing; kind, earnest, once proud of rank; chose a squad other than her family's | her relations to Asta beyond one remark |
| Klaus Lunettes | Golden Dawn; Steel Magic (walls and lances); a noble who was arrogant and grew; careful strategist | his fate in later arcs |
| Fuegoleon Vermillion | captain of the Crimson Lion; Flame Magic with the fire spirit Salamander; calm, just; ambushed during the Royal Capital assault, lost an arm, came back stronger | who struck him |
| Mereoleona Vermillion | eldest daughter of House Vermillion; temporary captain of the Crimson Lion and of the Royal Knights; Flame Magic and Mana Zone; blunt, fearless, a trainer | her squad history beyond that |
| Leopold Vermillion | youngest brother of Fuegoleon; Crimson Lion; Flame Magic; brash, declares Asta his rival | |
| Nozel Silva | captain of the Silver Eagle; Mercury Magic (from his mother's Steel and the family's Water); cold on the surface, protective of his sister | the family curse and his mother |
| Solid Silva | second son of the Silva family; Water Magic; Silver Eagle; arrogant, bullied his sister, later apologised | |
| Charlotte Roselei | captain of the Blue Rose; Briar Magic; the "cool beauty"; abrupt with men; a crush on the Black Bulls' captain | her curse |
| Jack the Ripper | captain of the Green Mantis; Slash Magic (blades that adapt to what they cut, even Spatial Magic); grinning, battle-hungry, loyal, a commoner who recruits commoners; bickers with the Black Bulls' captain | |
| Dorothy Unsworth | captain of the Coral Peacock; Dream Magic (the Glamour World); nearly always asleep, aware anyway; bright when awake, fond of cute things and macarons; vice-captain Kirsch Vermillion | her origin in the Witches' Forest |
| Kahono | priestess of the Seabed Temple; a water mage whose singing heals; dreams of being an idol and of the surface; Kiato's younger sister | |
| Kiato | dancer-warrior of the Seabed Temple, grandson of the High Priest; Butoh Magic as a sword-dance; older brother of Kahono; shares the dream of the surface | what Vetto did to him |
| Julius Novachrono | the 28th Wizard King; Time Magic; the coverless grimoire; playful, humble, curious about all magic, judges nobody by rank; the soul of Lucius resides in his body (the mod: "Two Souls", `LuciusBoss`) | how he died and returned |
| Marx Francois | the Wizard King's aide; Memory Magic; serious, loyal, put upon by his employer's unpredictability | his fate |

Searches (the sources the summaries pointed at; not opened): Wikipedia, List of Black Clover characters (https://en.wikipedia.org/wiki/List_of_Black_Clover_characters);
Black Clover Wiki: https://blackclover.fandom.com/wiki/Crimson_Lion, .../Kiato_and_Kahono_vs._Vetto, .../Underwater_Temple_Arc, .../Mereoleona_Vermillion_vs._Vetto, .../Sylph, .../Chapter_192;
MyAnimeList character pages: https://myanimelist.net/character/153802 (Jack), /157690 (Dorothy), /157331 (Marx), /153801 (Fuegoleon), /153965 (Klaus), /156373 (Solid), /161825 (Kiato);
https://sportskeeda.com/anime/black-clover-magic-knight-squad-captains-ranked-according-power; https://www.sportskeeda.com/anime/black-clover-chapter-373-acier-s-defeat-unites-noelle-siblings-nozel-achieves-a-new-form;
https://sportskeeda.com/anime/why-vangeance-betray-julius-black-clover-explained; https://sportskeeda.com/anime/how-julius-become-lucius-black-clover-explained;
https://www.soapcentral.com/anime/everything-know-about-kiato-black-clover; https://www.soapcentral.com/anime/black-clover-who-dorothy-unsworth; https://ww4.soapcentral.com/anime/who-jack-ripper-black-clover;
https://deltiasgaming.com/?p=116540 (who is Licht); https://animated-character-database.fandom.com/wiki/William_Vangeance; https://animenewsnetwork.com/review/black-clover/episode-69/.143126 (Charlotte);
https://anibase.net/en/character/lYwo0/Solid-Silva, https://anibase.net/en/character/4dZp0/Marx-Francois, https://anibase.net/en/character/1vdpa/Kiato.

## 10. Phase B: the models (pending)

The art kit (`docs/arc/art_kit.md`, `tools/gen_arc_kit.py`, `tools/arc/`) has not been merged into this tree, so no `tools/arc/char_<id>.py` exists yet; until it does, `ArcNpcRenderer`
shows the default model. The look notes below are what the sixteen specs will be written from. They are from the search summaries above and from memory: **unverified**, marked `?`
where I am guessing. The kit's spec style (read from the ARC-ART worktree while it was in progress, not merged): `build`, `height`, `skin`, `face`, `hair`, `outfit`, `cape`,
`accessories`, `props`, `clips`.

| Character | Build / height | Hair, face | Outfit and props | Glow |
|---|---|---|---|---|
| Yuno | slim, 1.80 | short messy black hair, a strand between the eyes; calm look; eyes dark green? | Golden Dawn mantle in tan-gold with a feathery blue collar over a black shirt; the four-leaf grimoire at the hip | green wind (7FD6A0) |
| William | tall, slim | short silver-white hair; purple eyes; a helmet-mask over the upper face | Golden Dawn captain's mantle, gold with blue fur trim, a cape | pale gold (C8A050) |
| Mimosa | slim, 1.62 | long wavy golden hair with flower clips; green eyes? | Golden Dawn mantle over a cream blouse, white short trousers and a brown corset; a grimoire at the hip | soft yellow-green |
| Klaus | tall, slender, 1.76 | short light-blue hair swept back with an angular fringe on his left; dark rectangular glasses with a diamond bridge; gold hoop earrings; fuchsia eyes | double-breasted high-collared shirt with gold trim under the mantle; a steel lance | steel blue |
| Fuegoleon | tall, broad | short red hair swept back? | Crimson Lion mantle in crimson with a gold lion emblem | flame orange |
| Mereoleona | tall, muscular | long wavy vermilion hair, blue eyes, a sharp canine | long red cape, sleeveless top? | hotter flame |
| Leopold | wiry, shorter | short spiky red hair, a sharp canine, eager face | Crimson Lion mantle | flame |
| Nozel | tall, slim, 1.77 | shoulder-length white-silver hair, blue eyes | Silver Eagle mantle in silver-white, an eagle emblem | silver shimmer |
| Solid | 1.69, neat | light silver-blue hair? blue eyes | Silver Eagle mantle, fine noble clothes | water blue |
| Charlotte | slim | blonde hair, blue eyes? | Blue Rose mantle in blue, silver half-armour, a rose emblem; thorny briar vines | blue |
| Jack | very tall 1.97, lean | shaggy dark hair, a wide grin | Green Mantis mantle in green; mana blades along the forearms | green |
| Dorothy | slim, 1.60 | short lilac hair, half-closed eyes | pointed witch hat with flowers; Coral Peacock cloak in coral pink with a peacock emblem | pink dream motes |
| Kahono | slim, 1.58 | sea-blue hair? (invented) | Seabed Temple priestess robe in white and aqua with shells | lilac music notes (C890D8) |
| Kiato | 1.78, lean | dark hair? (invented) | bare arms with wrapped sleeves, a sash, a curved dance-sword | aqua (78D0E8) |
| Julius | tall, 1.85 | light hair? | Wizard King's white and gold cloak; the coverless page-drum grimoire (the mod's existing Julius look, `GrimoireFloatClient`) | white-gold (E8DCC0) |
| Marx | 1.67 | a neat mushroom cut | formal deep-blue aide's robes with silver trim; a ledger | periwinkle (8AA0D0) |

Previews of the models will go to `$S/previews/npcb/` when the kit arrives.

## 11. Unverified (honest list)

* Nothing ran in the game: no line has been seen in a chat, no NPC placed, no gate evaluated by the Hub.
* The canon facts of section 9 are from search summaries, not from opened pages. Where sources disagreed (Mereoleona's squads, several looks) the doubtful part is not used. Two lines rest on a single
  low-quality source and are flavour, not fact: Mimosa joined the Golden Dawn "on her own so nobody could say it was her name", and William's remark about masks.
* The brief's squad grouping contradicted the series and the mod's own canon grimoires; the series was followed (section 3).
* The lines refer to things other builders create or define: the Chronicle (ARC-WORLD), teams sharing floors (PARTY), the Saga 1 ids `revchi_salik`, `heath_grice`, `lotus_whomalt`, `mars`,
  `vetto` (their briefs), the roster id `lucius` (ARC-WORLD's `ExistingBosses`), the word "champion". If one of them is renamed, grep the dialogue files; a wrong `defeated` id only makes a
  reaction line unreachable, a wrong saga id is caught by the tests.
* No floor numbers appear in any line (the floors of the champions are decided by their builders); the Saga 1 hint says "the first guardian floors", the Spade hint names no floor.
* Whether the Hub reads `prerequisites` of an NPC the way the ARC-WORLD brief describes (a post appears when they are satisfied for any online player) is assumed.
* The `{saga}` placeholder inserts the saga's title; "The Seven Deadly Sins" and "DanMachi" read well only in the two lines that quote or colon it (Julius, Marx).
* The real JUnit (CI) has not run these tests; the local shim has. The language keys have not been merged into `en_us.json`; `--check` after the merge is the proof.
