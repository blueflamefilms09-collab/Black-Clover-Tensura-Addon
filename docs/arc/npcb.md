# The other Magic Knights and the court (builder NPC-B, 1.60.0)

Sixteen friendly characters of Black Clover for the Sanctuary Hub: the Golden Dawn (Yuno, William Vangeance, Mimosa Vermillion, Klaus Lunettes), the Crimson Lion
(Fuegoleon, Mereoleona, Leopold Vermillion), the Silver Eagle (Nozel Silva, Solid Silva), three more captains (Charlotte Roselei of the Blue Rose, Jack the Ripper of
the Green Mantis, Dorothy Unsworth of the Coral Peacock), the two siblings of the Seabed Temple (Kahono, Kiato; the mod's exclusive casters of the Song and Butoh
books) and the court (Julius Novachrono, Marx Francois). They are NPCs of the shared `nusmp:arc_npc` entity (`docs/arc/core.md`): a record, a dialogue file and a
model. This document is the WHY of every file, the canon the lines rest on (with the URLs), the numbers, and an honest list of what is unverified.

**State.** Written without a game and without a compiler for the whole project. What was run: the pure JUnit tests of this builder (in the local shim, one
mutation check by hand), the type-check of the Java classes against the API stubs, the language-key tool, and for the models the art kit's own build of all sixteen and its
rebuild-and-compare (`python3 -B tools/gen_arc_characters.py --check`: 101 files, 0 differ, 0 problems) plus the preview harness (looked at, section 10). **Both phases are
done**: the records, the dialogue, the language file and the sixteen models.

## 1. New, replacement, kept (rule 5)

| Part | Status |
|---|---|
| The sixteen records (`arc/knights/*Cast.java`) | NEW. Nothing like them existed: the roster is empty before the character builders merge. |
| The sixteen dialogue files `data/nusmp/arc/dialogue/<id>.json` | NEW. `_fallback.json` (ARC-CORE) is untouched and still serves any character without a file. |
| Language keys `arc.dialogue.<id>.<n>` (354) | NEW, in `$S/arc/frag/npcb.lang.json` for the lead to merge. |
| Tests `KnightsCastTest`, `KnightsDialogueTest` | NEW. |
| The canon grimoires of the same people (`grimoire/CanonBook`) and the exclusive casters Kahono and Kiato (`docs/exclusive/kh.md`, `kt.md`, `book/CharacterSpells`) | KEPT, not touched. The records only borrow their colours and the dialogue only points at their pages. |
| The sixteen models: `tools/arc/char_<id>.py` and what the art kit generates from them (geo, animations, texture, glow texture, preview scenes) | NEW. `tools/arc/char_npc_default.py` (the fallback) and the two reference characters are untouched. |

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
| `tools/arc/char_<id>.py` x16 | The look of each character as a spec of the art kit (`docs/arc/art_kit.md`); every file starts with a `Why:` docstring that says where the look comes from and what was added for the game. Jack (forearm blades) and Julius (the page drum) have an `extra(kit, rig)`. |
| `src/main/resources/assets/nusmp/geo/entity/arc_<id>.geo.json`, `animations/entity/arc_<id>.animation.json`, `textures/entity/arc_<id>.png` and `_glow.png` (x16) | Generated by `python3 -B tools/gen_arc_characters.py <ids>`; deterministic, checked by `--check`. The model key is the roster id, which is how `ArcNpcRenderer` finds it. |
| `tools/render_preview/scenes.d/arc_<id>.json` (x16) | The preview scenes the driver writes (`python3 -B tools/render_preview/preview.py arc_<id>_front ...`). |
| `tools/arc/manifest.json` | The driver's table of every model; this tree's copy has the sixteen new rows. It will conflict with the other builders' copies at the merge: regenerate it with `python3 -B tools/gen_arc_characters.py` after the last merge and run `--check`. |

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
* **Weights bias the mix toward the story.** Lines about the player's situation have weight 4, character lines weight 1, so about a quarter to two thirds of the clicks are about where the player is
  (measured with the real `DialogueBook.pick`, 3000 draws per case, six characters at five points of the story: 22% for a character with one line per saga up to 70% for Julius in the
  Lucius band, which has five), and the rest are the character talking. In all 90 000 draws no line was said twice in a row. Situations: `maxStage 0` (beaten nothing), `saga elf_reincarnation` with `minStage 1`, `spade_raid`, `wizard_king`,
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
* Models: nothing to register (`ArcClient` registers the NPC renderer, which draws `arc_<roster id>`). After the last builder is merged: `python3 -B tools/gen_arc_characters.py` (rebuilds the
  manifest; expect a textual conflict in `tools/arc/manifest.json` before that), then `python3 -B tools/gen_arc_characters.py --check` must say `0 differ, 0 problems`. The NPC models need no
  `ArcLook` numbers (only bosses use them); the hitbox of the NPC entity is its own (`ArcNpcEntity`), the manifest's `recommended_hitbox_blocks` is only a hint.
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

## 10. The models (Phase B)

Each character is one spec file of the art kit (`tools/arc/char_<id>.py`); `python3 -B tools/gen_arc_characters.py <ids>` turns it into the geo model, the clips (`idle walk run attack cast cast_big
hurt transform death`), the texture and the glow texture. For an NPC the renderer plays `idle`, `walk`, the **`cast` clip as the talking clip**, `hurt` and `death`. The looks are from the search
summaries of section 9 and from memory, with the kit's vocabulary (a rose is the sun emblem, a helmet-mask is a visor): **unverified against the series**; what is invented is said in each file's
docstring. Twelve of the sixteen carry the same small device so a delver can read the cast at a glance: **a floating grimoire** at the right side in the colours of the character's canon book (`grimoire/CanonBook`),
glowing in the character's colour (the other four carry what is theirs: Jack his forearm blades, Kiato his sheathed sword, Marx his ledger, Julius his page drum), and the Golden Dawn four share the **gold mantle with blue fur shoulders**.

| Character | Build, height | Hair, face | Clothes, cape, extras | Props, glow |
|---|---|---|---|---|
| Yuno | slim 2.00 | black, long bangs; calm, green eyes | black shirt, tan-gold mantle (sun emblem), blue-white fur shoulders | floating grimoire, wind green |
| William | slim 2.03 | silver-white side part; a gold visor mask with a violet slit (his purple eyes) | cream captain's coat with gold trim, gold cape lined blue with the World Tree, blue fur | floating grimoire, gold |
| Mimosa | slim 1.84 | long wavy gold, pink bow; shy, blush, green eyes | cream tunic, white shorts, brown corset belt and boots, gold mantle, blue fur | floating grimoire, plant green |
| Klaus | slim 2.00 | light-blue side part; thick dark glasses, gold earrings, fuchsia eyes | navy double-breasted uniform with gold trim, steel bracers, gold mantle, blue fur | floating grimoire, steel blue |
| Fuegoleon | average 2.09 | slicked deep red; calm, amber eyes | dark-red uniform with gold buttons, crimson mantle with a gold flame | floating grimoire, flame |
| Mereoleona | broad 2.00 (narrowed 6%) | a lioness mane of vermilion hair; a toothy grin, blue eyes | sleeveless dark-red top, wrapped fists, a long red cape with a flame | floating grimoire, flame |
| Leopold | average 1.88 | spiky red; a wide grin | red tunic, fingerless gloves, crimson mantle with a flame | floating grimoire, orange flame |
| Nozel | slim 2.00 | long white-silver; cold, blue eyes | silver-white coat, white cape lined navy with the eagle's wings | floating grimoire, mercury blue |
| Solid | slim 1.91 | silver-blue side part; a smirk | navy uniform with silver trim, silver mantle with the eagle | floating grimoire, water blue |
| Charlotte | slim 1.91 | long blonde; cold, lipstick, arched brows | blue uniform, silver breastplate (a flower), pauldrons and bracers, a blue cape | floating grimoire, blue |
| Jack | lanky 2.19 | shaggy dark green; a sinister grin | green tunic with torn sleeves, a tattered green cloak with a crest; **extra: six glowing mana blades along the forearms** | green; no grimoire (the blades are his magic) |
| Dorothy | small 1.62 | lilac bob; eyes closed (asleep), a smile, blush | a witch hat, white-and-pink dress, a coral cloak with the peacock's fan | floating grimoire, pink |
| Kahono | small 1.69 | sea-blue twin tails, lilac ribbons; cheerful, blush | white-and-aqua temple dress, obi, shell pendant, earrings | floating grimoire, lilac |
| Kiato | average 1.94 | short dark blue, aqua headband; calm | teal vest on a bare chest, wrapped forearms, white hakama, sash | a curved sword sheathed at the hip, aqua edge glow |
| Julius | average 2.03 | pale gold curtains; a broad smile | white floor-length robe, gold sash, white mantle with the gold clover; **extra: the coverless page drum of his Time grimoire**, floating and turning | gold bands on the drum |
| Marx | average 1.88 | dark bowl cut; stern | navy aide's coat with silver trim, a short cape with the clover | a ledger (an open book) in the left hand; his talking clip writes in the air (`book`) |

Numbers from `tools/arc/manifest.json` (all sixteen: no problem listed; 923 cubes in all, none above 89 cubes or 500 faces per pass, the kit's own advice is 250 cubes):

| id | height | cubes | faces | texture | hitbox hint |
|---|---|---|---|---|---|
| `julius_novachrono` | 2.03 | 45 | 237 | 512x64 | 0.62 x 2.03 |
| `marx_francois` | 1.88 | 53 | 275 | 512x64 | 0.62 x 1.88 |
| `yuno` | 2.00 | 37 | 195 | 512x64 | 0.55 x 2.00 |
| `mimosa_vermillion` | 1.84 | 55 | 273 | 512x64 | 0.55 x 1.84 |
| `klaus_lunettes` | 2.00 | 64 | 322 | 512x64 | 0.55 x 2.00 |
| `leopold_vermillion` | 1.88 | 89 | 500 | 512x64 | 0.62 x 1.88 |
| `fuegoleon_vermillion` | 2.09 | 76 | 420 | 512x64 | 0.62 x 2.09 |
| `charlotte_roselei` | 1.91 | 54 | 287 | 512x64 | 0.55 x 1.91 |
| `nozel_silva` | 2.00 | 55 | 285 | 512x64 | 0.55 x 2.00 |
| `solid_silva` | 1.91 | 47 | 242 | 512x64 | 0.55 x 1.91 |
| `jack_the_ripper` | 2.19 | 66 | 301 | 512x64 | 0.55 x 2.19 |
| `dorothy_unsworth` | 1.62 | 52 | 276 | 512x64 | 0.50 x 1.62 |
| `william_vangeance` | 2.03 | 54 | 277 | 512x64 | 0.55 x 2.03 |
| `mereoleona_vermillion` | 2.00 | 76 | 396 | 512x128 | 0.73 x 2.00 |
| `kahono` | 1.69 | 43 | 239 | 512x64 | 0.50 x 1.69 |
| `kiato` | 1.94 | 57 | 330 | 512x64 | 0.62 x 1.94 |

### What the kit could not draw, and what stands in its place

* **A rose** (Blue Rose): no emblem of the kit is one; the **sun** emblem (a ring with rays) stands for it on Charlotte's breastplate and cape.
* **William's helmet-mask** covers the upper face: the kit's `half` mask is the lower half, so the **visor** (an eye band) in gold with a violet glow in its slit is used (it reads as his purple eyes).
* **Julius's coverless page drum**: not a kit prop. `extra()` builds it (two blocks of pages, one turned 45 degrees, two gold bands, turning slowly on its own bone).
* **Jack's mana blades**: `extra()` lays three glowing blades along each forearm outside, reaching past the fist, the way the kit's Heath Grice frost is built; they ride on the forearm bones.
* **Klaus's lance**: a held spear is twice his height and shrank him in every picture (the preview frames the longest part), so he wears steel bracers and has the floating grimoire instead.
* **Mimosa's flower** is hidden inside her hair by the kit's flower accessory, so a pink bow (`hair.ribbon`) is used.
* Two rules of the kit that every character builder will meet (found by `--check`, both listed here so nobody loses an hour): the **`guard` idle breaks the kit's own arm rules** (a clip starts 60 degrees
  away from the idle pose) for a character with no held prop (Mereoleona and Leopold use `proud`); and the **weapon attack styles** (`slash overhead thrust smash stab sweep`) need a **held, drawn**
  prop (Jack has none and Kiato's sword is sheathed, so both use `punch`, which is also what an NPC never really plays).

### Previews (looked at, one by one)

`$S/previews/npcb/` (`$S` is the scratchpad of the job): per character `<id>_front.png` (front and 3/4), `<id>_back.png` (back and side), `<id>_face.png`, `<id>_idle.png` (four frames), `<id>_cast.png`
(the talking clip, four frames), and `jack_the_ripper_attack.png`, `kiato_attack.png`; the contact sheets `overview_front.png`, `overview_back.png`, `overview_face.png` (all sixteen) and
`gallery/arc_gallery_hair.png` (the kit's 32 hair styles, used to choose). What was looked at and fixed: every front, face and cast; the idle of every character; back views of the cloaks (the eagle
emblem was silver on silver and is now blue-grey); walk, hurt and death of Julius, Dorothy and Marx; the attack of Jack and Kiato; William's mask and hair (a silver crown of clumps was replaced by the side
part and the gold visor), Klaus's lance, Mimosa's hidden flower, Jack's blades (small claws, then long blades), Kahono's red pendant (now aqua), Marx's ledger (smaller), Fuegoleon's hair (spiky, then slick).

## 11. Unverified (honest list)

* Nothing ran in the game: no line has been seen in a chat, no NPC placed, no gate evaluated by the Hub, no model drawn by the real renderer. The previews come from the kit's software rasteriser of the
  game's own model code (`GeoDraw`), not from the game: light, colours and the glow layer differ in the game.
* The canon facts of section 9 are from search summaries, not from opened pages. Where sources disagreed (Mereoleona's squads, several looks) the doubtful part is not used. Two lines rest on a single
  low-quality source and are flavour, not fact: Mimosa joined the Golden Dawn "on her own so nobody could say it was her name", and William's remark about masks.
* The looks are partly from memory and partly invented (hair and outfit colours of Fuegoleon, Leopold, Solid, Kahono, Kiato, Julius, Marx and the casual clothes of everybody); the series' own
  colours of the squads' mantles are not verified (gold with blue fur for the Golden Dawn is from a figure description and a wiki line that disagreed).
* The brief's squad grouping contradicted the series and the mod's own canon grimoires; the series was followed (section 3).
* The lines refer to things other builders create or define: the Chronicle (ARC-WORLD), teams sharing floors (PARTY), the Saga 1 ids `revchi_salik`, `heath_grice`, `lotus_whomalt`, `mars`,
  `vetto` (their briefs), the roster id `lucius` (ARC-WORLD's `ExistingBosses`), the word "champion". If one of them is renamed, grep the dialogue files; a wrong `defeated` id only makes a
  reaction line unreachable, a wrong saga id is caught by the tests.
* No floor numbers appear in any line (the floors of the champions are decided by their builders); the Saga 1 hint says "the first guardian floors", the Spade hint names no floor.
* Whether the Hub reads `prerequisites` of an NPC the way the ARC-WORLD brief describes (a post appears when they are satisfied for any online player) is assumed.
* The `{saga}` placeholder inserts the saga's title; "The Seven Deadly Sins" and "DanMachi" read well only in the two lines that quote or colon it (Julius, Marx).
* The real JUnit (CI) has not run these tests; the local shim has. The language keys have not been merged into `en_us.json`; `--check` after the merge is the proof.
* The floating grimoire of twelve characters and Julius's page drum are drawn beside the character in every clip (an orbit of a few pixels): how it looks next to a wall or inside a doorway of the plaza is not known.
