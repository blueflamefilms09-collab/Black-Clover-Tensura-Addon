# The Black Bulls and their friends (builder NPC-A, the arcs job)

Fourteen friendly characters of the saga `ALLIES`: **Yami Sukehiro, Asta, Noelle Silva, Finral Roulacase, Magna Swing, Luck Voltia, Charmy Pappitson, Vanessa
Enoteca, Gordon Agrippa, Zora Ideale, Gauche Adlai, Grey, Secre Swallowtail, Nacht Faust.** They stand in the Sanctuary Hub (not on the floors), talk, point
the player at the next arc and teach a little of the mod's own systems. Nobody can fight them. They are all the one shared entity `nusmp:arc_npc`; what makes
each of them is a record (Java), a dialogue file (JSON) and a model (a spec of the art kit). Yami's model was made by the art-kit builder; the other thirteen
models are new. Everything here is NEW (nothing existed for these characters, nothing was replaced, nothing was removed); the old Black Clover content (their
grimoire books, the Dark book's Equinox, Asta's swords, Charmy's sheep ...) is untouched and is what the dialogue talks about.

Spelling: the owner's brief writes "Enotecia"; the wiki and the series write **Enoteca**, so the id is `vanessa_enoteca` and the name is "Vanessa Enoteca". Change
the record's name only (the id is saved in worlds and in the dialogue file name) if the owner insists on the other spelling.

## 1. Files and why

| File | Why it exists |
|---|---|
| `arc/npc/BlackBullsNpcs.java` | One pure class holding the fourteen `ArcCharacter.npc(...)` records, their order and the arrival gates. One place, one `registerAll()` call for the lead, and a pure class means the order, the gates and the colours are unit tested without a game. |
| `data/nusmp/arc/dialogue/<id>.json` x14 | What each character says (443 lines in all). A datapack can replace any file by shipping the same path. Keys `arc.dialogue.<id>.<n>` come from the repo's tool. |
| `tools/arc/char_<id>.py` x13 | The model of each character as a spec of the art kit (Yami's file already existed). |
| `assets/nusmp/{geo,animations}/entity/arc_<id>.*.json`, `textures/entity/arc_<id>.png`, `arc_<id>_glow.png` x13 | The generated output of those specs (`python3 -B tools/gen_arc_characters.py`; `--check` proves the files are what the specs make). |
| `tools/render_preview/scenes.d/arc_<id>.json` x13 | The preview scenes the generator writes next to every model. |
| `tools/arc/manifest.json` | Edited by the generator: 13 entries added (size, hitbox, stride, clips, bones). The four older entries are unchanged. Regenerate it after merging, never hand-merge it. |
| `src/test/.../arc/npc/BlackBullsNpcsTest.java`, `BlackBullsDialogueTest.java` | 7 + 16 tests (section 6). |
| `$S/arc/frag/npca.lang.json` (outside the repo) | The 443 language entries of the dialogue lines, for the lead to merge into `en_us.json`. |

No new entity, item, block, renderer, config, command, network packet, quest hook or sound. The renderer, the spawn egg, the `/multiverse arc` commands, the Hub
post and the story credit all come from the roster record (docs/arc/core.md section 9).

## 2. The fourteen

Order = the order they gather in the Hub = the order of the roster lines (the Hub gives each NPC its post by roster order, docs/arc/world.md section 6).
`magic` is an id of `MagicType` (a test reads `MagicType.java` and checks every id exists); `devil` is only informational for an NPC.

| id | Name, title | magic | devil | arrives | colours (main / accent) | build, height |
|---|---|---|---|---|---|---|
| `yami_sukehiro` | Yami Sukehiro, Captain of the Black Bulls | dark | - | start | `B89AD0` / `E6C98A` | broad, 2.06 (art-kit builder) |
| `asta` | Asta, Anti-Magic Swordsman | anti_magic | Liebe | start | `C9CED8` / `78C28C` | average, 1.88 |
| `noelle_silva` | Noelle Silva, Water Mage of House Silva | water | - | start | `9CC8F0` / `DDE8F6` | slim, 1.80 |
| `finral_roulacase` | Finral Roulacase, Spatial Mage | spatial | - | start | `78B898` / `E0C878` | slim, 1.90 |
| `magna_swing` | Magna Swing, Fire Mage | flame | - | after `revchi_salik` | `F0703A` / `FFB070` | average, 1.96 |
| `luck_voltia` | Luck Voltia, Lightning Mage | lightning | - | after `revchi_salik` | `FFE35A` / `BFE8FF` | slim, 1.74 |
| `charmy_pappitson` | Charmy Pappitson, Cotton and Food Mage | cotton | - | after `revchi_salik` | `F4EAD0` / `F0B070` | small, 1.52 |
| `vanessa_enoteca` | Vanessa Enoteca, Witch of the Red Thread | thread | - | after `heath_grice` | `E890B8` / `FF7A90` | average, 1.97 |
| `gordon_agrippa` | Gordon Agrippa, Poison Mage | poison | - | after `heath_grice` | `9CC070` / `D8E8B0` | lanky, 2.08 |
| `zora_ideale` | Zora Ideale, Ash Mage | ash | - | after `heath_grice` | `E06A58` / `B8B8C8` | slim, 1.94 |
| `gauche_adlai` | Gauche Adlai, Mirror Mage | mirror | - | after `lotus_whomalt` | `9A8CE0` / `D6DCF8` | slim, 1.96 |
| `grey` | Grey, Transmutation Mage | transmutation | - | after `lotus_whomalt` | `9AB6D8` / `E0ECF8` | slim, 1.74 |
| `secre_swallowtail` | Secre Swallowtail, Keeper of Seals | sealing | - | after `vetto` | `58C8F0` / `B8F0FF` | slim, 1.72 |
| `nacht_faust` | Nacht Faust, Devil Host | shadow | Walgner | after `vetto` | `8E8EB0` / `C8C8E8` | slim, 2.04 |

* Tier stays the default and the stats are 0 on purpose: `/multiverse arc info` should show a friend, not a fighter, and the roster self-test flags an NPC with
  boss-only data (floors, or prerequisites together with a Charter), which these do not have.
* The colours are lifted so the speaker name, the name tag and the spawn egg read on a dark chat (a test demands brightness of at least 0.40).
* Why the colours are what they are: each is the colour of the character's magic or hair (violet for Yami's Dark Magic, ash grey and green for Asta, water
  blue for Noelle, the cyan of the mod's own Seal Magic effects for Secre ...). The javadoc of each constant says which.

## 3. How they arrive in the Hub

The four who explain the dungeon (Yami, Asta, Noelle, Finral) have no prerequisites and are there from the first day. The others follow the story, so the Hub
fills as the player advances, in four waves: **Magna, Luck, Charmy** after Revchi Salik; **Vanessa, Gordon, Zora** after Heath Grice; **Gauche, Grey** after
Lotus Whomalt; **Secre, Nacht** after Vetto (3 / 3 / 2 / 2 behind the first 4; a test pins the shape).

The consumer is ARC-WORLD's `ArcHubNpcs` (read in `arcworld-wt`): a character with prerequisites is placed when at least one online player has them all
defeated (`ArcStory.unlocked`), or always when its config `arc.hubNpcsAlwaysThere` is on. Four consequences the lead must know:

1. **The gate ids must exist in the final roster.** They are the ids S1A uses (`revchi_salik`, `heath_grice`, `lotus_whomalt`, `vetto`; checked in `s1a-wt`
   on the day of writing). The story rules skip a prerequisite that is not in the roster (so a missing one only means the friends stand there from the start),
   BUT the self-test `arc.roster` FAILS for it ("follows 'x', who is not in the roster"), and the smoke test expects that test to pass. If S1A renames a boss,
   change the four constants `GATE_1..GATE_4` in `BlackBullsNpcs` (one line each).
2. The Hub has 47 posts (docs/arc/world.md). They are given by roster order and a character without a post is only reported. These fourteen take 14 of them,
   NPC-B and DM add theirs; the posts of friends that have not arrived yet stay empty.
3. The dialogue is the same whether a friend arrives early (operator command, `hubNpcsAlwaysThere`) or late; the lines that react to a boss only fire after
   that boss is defeated.
4. `ArcRoster.register` throws for a different record under an existing id. Nobody else registers these ids (grepped in every worktree on the day of writing;
   the only other `npc("asta", ...)` is a fixture in `ArcRosterTest`, which clears the roster first).

## 4. What they say

### 4.1 How the files are built (and why)

Every file has 4 or 5 greetings, 22 to 26 hints and 3 farewells (69 / 332 / 42; 443 lines). The engine facts that shaped them (docs/arc/core.md section 3):

* **Greeting** is said at the first meeting only. Plain greetings carry `first`. A greeting that also notices something (`minStage` 3, or the gate boss
  already defeated) has a second condition, so it has the higher priority and wins when it applies. A newcomer at any stage or saga always has at least four
  greetings to hear (a test walks every stage and saga).
* **Hint** is the usual conversation afterwards. The book picks only among the lines of the highest priority that fit, and a line's default priority is its
  number of conditions. Left alone, a stage-conditioned line (1 condition) would beat every unconditional chat line (0) for ever and the chat would never be
  heard. So every hint carries `priority` 1 and a `weight` instead (guidance by stage or saga 3, a reaction to a boss that fell 2, a role line 2, chat and survival
  tips 1): they mix, the newcomer hears the way forward most often, and nothing is buried. The line said last time is not repeated (a test draws 400 lines and
  checks no back-to-back repeat and at least ten different lines).
* **Guidance** (7 to 10 lines per file): one line per saga (`elf_reincarnation`, `spade_raid`, `wizard_king`, `seven_deadly_sins`, `danmachi`; the early saga has a
  second one for `minStage` 4 that points deeper), plus a veteran line at `minStage` 20. Stage = the number of bosses the player has defeated; the saga is the
  earliest one with a boss still standing, so the lines move on by themselves as the story is played.
* **Reactions** (3 to 6 per file) are conditioned on `defeated` of a real story id (`revchi_salik`, `heath_grice`, `lotus_whomalt`, `mars`, `vetto`, `zagred`,
  `dante`, `vanica`, `zenon`, `lucius`) and can never be heard before that boss is down (a test).
* **Farewell** 3 lines, unconditional (spoken when the conversation ends).
* **Voice**: the file has no `voice` colour, so the speaker name takes the record's main colour (section 2).
* Every line is plain ASCII, 12 to 260 characters, keyed, with no repeated text across the fourteen files.

### 4.2 Why there are no quest or shop lines

`TalkPlan` offers a quest as soon as a `quest_offer` line exists and then waits for something to complete the quest stage; no system in the mod does that for these
characters yet, so a `quest_offer` would leave the stage at "offered" for ever and the player would hear it once and never be thanked. The same goes for
`shop`. No file under `data/nusmp/arc/npcs/` exists in the core (no role data), so the small roles are carried by hint lines that name real pages and places (a
test checks that the names exist in the source tree). When a quest or shop system exists it adds `quest_offer`, `quest_done` and `shop` lines to these files;
nothing here has to change.

### 4.3 The small roles and what each line was checked against

Everything a character says about the mod is checked against the source on the day of writing (the file in the right-hand column). The numbers are not covered by
a test (section 6): if a page is rebalanced its line goes stale.

| Character | What they teach | Checked against |
|---|---|---|
| Yami | Directional Lock (clear the first floor of one road and the other shuts), a guardian every tenth floor, a Charter Grimoire a boss may drop (once per world per boss, a miss rolls again) that you borrow by holding or wearing it, the Dark book's Equinox (locked to his own grimoire), the Crucible (three relics, one pick, a catastrophe), the Chronicle's lectern, gloom, raw meat | `LockPolicy`/`AlignmentRules`, `docs/labyrinth/mega_v2.md`, `docs/boss_overhaul.md`, `CharacterSpells`, `DarkBook`, `docs/labyrinth/mega.md`, ARC-WORLD `docs/arc/world.md`, `DungeonFood` |
| Asta | Anti-Magic: the Demon-Slayer Sword from the start, Anti-Magic Power gathering from magic that hits you, the mastery that wakes Demon-Dweller, Demon-Destroyer, Demon-Slasher and turns Black Asta into a held form; grimoires dropping | `AntiMagicBook` |
| Noelle | Sea Dragon's Roar (homing water dragon), Valkyrie Dress (30 s, a minute to recover), Sea Dragon's Nest (a dome: spells swallowed, foes slowed, allies healed) | `WaterBook` |
| Finral | Spatial hint: Fallen Angel Gate (two doors, 20 s, 45 s to rest), Sacred Mana Domination (nearest projectile within 8 blocks, else pulls the target 4 blocks, never both); floors are per team; the stairs "still forming"; the way back to the plaza is never locked | `SpatialBook`, `docs/labyrinth/mega_v2.md`, `AlignmentRules` |
| Magna | Exploding Fireball is the first Fire page; Leo Rugiens and Calidos Brachium open only for the Vermillions' grimoires; party floors | `FireBook`, `CharacterSpells` |
| Luck | Thunder Fiend and Black Lightning Battle Fiend (both open only in his own grimoire), Chain Lightning (3 targets, 5 blocks), wandering bosses, noise and light | `LightningBook`, `WikiSpells`, `CharacterSpells`, `docs/labyrinth/mega.md` |
| Charmy | Sheep Cook, Sleeping Sheep Strike, Sheep Bondage, Cotton Cloud (you and up to four friends), Glutton's Banquet; her grimoire holds both halves; raw monster meat (cook or purify it), magic stones | `CottonBook`, `WikiSpells`, `DungeonFood` |
| Vanessa | Thread / curses: Red Thread (about 3 s on an ordinary enemy, 1 s on a boss, half as long on a repeat within 30 s), Rouge (the thread cat: 10 minutes, watches you, your party, pets and up to six named friends, rewrites a killing or heavy blow for mana, double against a boss, 8 s without a second boss save), the pages that open only in her grimoire, curses as threads, rest only in Landings, no passive healing | `ThreadBook`, `ThreadSpells`, `RougeFateEvents`, `FateRules`, `ThreadConfig`, `BalanceLaw`, `docs/labyrinth/mega.md` |
| Gordon | Poison pages (Aufwachen Dachs, Violett Schirm 8 s, Basilisk's Breath), Curse-Worker's Neighbor (only his grimoire), the Crucible's three catastrophes, milk, raw meat, gloom | `ElementBooks`, `WikiSpells`, `CharacterSpells`, `docs/labyrinth/mega.md` |
| Zora | Traps: the pitfall tile that looks like the floor, the snuffer glyph (torches die, no new light nearby), the four-colour sigil puzzle with its lock-out; Ash pages (Ash Bullets, Ash Cloud, Ash Absorbing Formation 8 s, Revelation of the Cowardly: next three strikers, locked to him); wandering bosses | `labyrinth/trap/*`, `AshBook`, `WikiSpells`, `CharacterSpells` |
| Gauche | Mirror: Reflect Refrain (4 s, returns 2 to 4 shots, silences the shooter), Real Double (a tenth of your magicules each, takes 50% more damage, +20% to your blows each up to three), Full Reflection (8 s), Mirror Array (a mirror shatters in place of a heavy blow), weak to gel and dark; the System window can be moved | `MirrorBook`, `MirrorWorks`, `MirrorDoubleEntity`, `docs/mirror_spec.md`, `docs/labyrinth/mega_v2.md` |
| Grey | Transmutation: Magic Convert (2 s circle, enemy spells become mana), Iron Spikes, Quagmire (sand for 5 s), Grand Transmutation; mimics; rest in Landings | `WikiBooks`, `WikiSpells`, `RelicLoot` |
| Secre | Sealing: Seal (5 s on a mage's grimoire, 1 s on a boss), Branching Array (looked-at locked chests within 8 blocks), Eternal Prison (6 s, 1.5 s on a boss), Wound Sealing (30% of health), Orbital Bind (30 s of magic resistance); the four open only in her grimoire; Gatekeeper doors need the Guardian's Mana Core; the Crucible | `SealingBook`, `SecreSeals`, `docs/labyrinth/mega.md` |
| Nacht | Devils: Liebe, Zagred, Megicula, Lucifero, Beelzebub, Astaroth; his own four (Slotos horse, Gimodelo dog, Plumede cat, Walgner rooster) as Unite Modes for 30 s; Shadow Travel needs shadow; Devil Union is for five-leaf grimoires; noise and light | `Devil`, `UniteKind`, `UnitePages`, `ShadowTravel`, `GrimoireBook` |

### 4.4 The floors the lines name

A test lists every floor a line names and demands exactly this set, so a floor claim cannot creep in unnoticed.

| Said | Means | Source |
|---|---|---|
| the first floor | the Directional Lock binds a player to a road when they clear floor +1 or -1 | `AlignmentRules`, `docs/labyrinth/mega_v2.md` |
| every tenth floor | a guardian on the stairs, then the Crucible | `docs/labyrinth/mega_v2.md` section 1 |
| past the thirtieth floor | the Eye of the Midnight Sun's elite (S1B: floors 30 to 70) | brief `$S/arc/s1b.md` |
| below the fortieth floor | Zagred (ZAG: Abyssal road, floors 40 to 80) | brief `$S/arc/zag.md` |

If S1B or ZAG change their floor ranges the lines go stale (the test cannot see their code). The story gate for a boss is the story, not the floor, so a wrong
floor in a hint misleads but never blocks.

### 4.5 Canon voice (what each one sounds like)

Yami: gruff, deadpan, "Tch", paperwork, never praises straight. Asta: loud, earnest, exclamation marks, training. Noelle: noble tsundere, "Hmph", embarrassed
kindness. Finral: cowardly, flirtatious, taxi jokes, Finesse. Magna: delinquent bravado, friendly rivalry with Luck. Luck: gleeful fight-lover. Charmy: speaks of
herself as "Charmy", always food. Vanessa: drowsy, flirty, "sweetie", a drink in hand, thread metaphors. Gordon: mumbling, trailing dots, kind and gloomy. Zora:
rude, hates nobles, traps and pranks. Gauche: cold, sharp, Marie. Grey: stammering, "Sorry!", turns into others to be brave. Secre: formal, terse, locks and doors,
a bird's tidiness. Nacht: blunt, "Don't lose.", devils.

## 5. Models (phase B)

All thirteen are built from `tools/arc/char_<id>.py` with the art kit (docs/arc/art_kit.md), previewed, looked at in front, 3/4, back, side, face (front and 3/4),
idle, walk, cast (the talking clip), hurt and death, and fixed where ugly. `python3 -B tools/gen_arc_characters.py --check` reports "86 files, 0 differ, 0
problems" (the kit's arm-rule and face-transparency checks included).

| id | Look (from the series and wiki summaries; clothes are partly from memory) | What glows |
|---|---|---|
| asta | Spiky ash-grey hair, green eyes, a grin, a black headband, a black sleeveless top, the black Black Bull cloak, the huge black greatsword stowed diagonally on the back (model prop only, length 0.62) | nothing: anti-magic is black |
| noelle_silva | Silver hair in twin tails with navy ribbons, blue eyes, a pout and a blush, a navy dress with white sleeves and collar, a cravat, knee boots | the cloak's bull, water blue |
| finral_roulacase | Short light-brown hair, green eyes, a shy smile, green earrings, a cream shirt, brown trousers | earrings and the cloak's bull, green |
| magna_swing | A black mohawk with a grey streak, goggles with blue lenses, a navy coat over a white shirt, fingerless gloves, the broom (a custom prop registered by the spec, length 0.55) stowed across the back with the straw above the floor | the cloak's bull and the broom's tip, ember orange |
| luck_voltia | Short golden hair, wide light-blue eyes, a wide grin, an aqua tunic, teal trousers, gold fingerless gloves | the cloak's bull, lightning yellow |
| charmy_pappitson | The one small build (1.52): black hair, green eyes, a blush, a cream tunic, brown shorts, a satchel and a strap, a cloak too big for her | the cloak's bull, biscuit |
| vanessa_enoteca | Long wavy pink hair, half-closed violet eyes, a dark dress, earrings, a red wrap on the forearm and a thread that sways (built in `extra()`) | the thread and the cloak's bull, red |
| gordon_agrippa | Tall and thin (lanky 2.08), pale, no eyebrows, a large nose, a frown, dark circles, slicked dark hair, a grey knee coat, a hooded cloak with the hood back | the cloak's bull, poison green |
| zora_ideale | Messy red undercut, blue eyes, a smirk, black straps round the lower face (two rings and cheek straps, built in `extra()`), a stud above the brow, ear cuffs, a dark sleeveless top, wraps on the hands, a neck of belts | the cloak's bull, ash white |
| gauche_adlai | Messy black hair, a cold face, a monocle on the left eye with a violet glow (his mirror eye), the uniform, a mirror orb floating at his side | the monocle, the orb and the cloak's bull, violet |
| grey | Short light-blue bob, big blue nervous eyes, a blush, a cream shirt, pink trousers, flat shoes, a short cloak without a hood | the cloak's bull, pale blue |
| secre_swallowtail | A black bob, narrow tired eyes with bags, a ragged black dress, a grey strap, a grimoire floating at her side | the grimoire and the cloak's bull, cyan |
| nacht_faust | Pale, pale blue eyes, a cold face, black hair in a high ponytail, a dark high-collared coat, four violet gems on a cord at the chest (his four devils, built in `extra()`) | the four gems, violet |

### 5.1 What looking at the previews changed

* Magna's broom sank 0.43 blocks into the floor (the manifest's bounds showed a minimum height of -0.43): its origin was moved 9 units up the shaft and it was
  shortened, so the straw now ends above the floor (minimum 0.0) and the shaft stays under the top of the mohawk. Asta's sword went from length 0.75 to 0.62 (a
  model that was 3.24 blocks tall is now 2.96; still the tallest of the thirteen, it is his sign).
* Charmy's and Zora's belts lost `pouches` (the kit puts a pouch BEHIND the torso that sticks out of a short cloak's back by 0.05 to 0.3 units), and Charmy's
  cloak emblem went from size 8 to size 4: on a short cloak the emblem straddled the three cloak tiers and was cut in pieces.
* A hood laid back cut a wedge out of Grey's bob (removed); Zora's half mask hid his eyes (removed, the straps are built by hand); Charmy's bun, Luck's shaggy
  hair and Gordon's slick hair read as hats (changed to `short`, `short`, `side_part`); baggy and skirt legs read as dresses or bare legs (changed to `trousers`
  for Luck, Grey and Zora, `dress` tops for Noelle and Vanessa); Nacht's kit necklace has a hard-coded red glow (replaced by a `choker` and four violet gems
  built by hand); Vanessa's first thread looked like a sword (thinner, a cord and a cuff); Finral's green streak was removed.
* The kit's checks forced: no `cast_jab` attack on a character without a held prop (a `TypeError` in the build; `punch` is used for Gauche and Secre), no `guard`
  idle together with a `punch` attack (an arm-rule problem; Luck uses `relaxed`, Magna `proud`), no short ragged cloak on a small or short build ("cape_low face
  fully transparent": Charmy `ragged: False`, Vanessa length 0.5, Grey length 0.6).

### 5.2 Notes for the next author of a spec

`gen_arc_characters.py` treats an unknown flag as "build everything" (`--help` rebuilt all seventeen models in 35 seconds, harmless because the build is
deterministic). A `float` grimoire ignores `length`. `neck: necklace` always glows red. A short cloak's emblem must fit the top tier (about 45% of the cloak): size
4 at y 0.5 on a small build. Preview with `RENDER_PREVIEW_BUILD=<own folder> python3 -B tools/gen_arc_characters.py --preview <id> front back face idle walk cast hurt death`.

### 5.3 Preview files

`$S/previews/npca/<id>_<scene>.png` for the thirteen ids and the scenes front, back, face, idle, walk, cast, hurt, death (104 files), and four contact sheets of
each kind: `final_fronts_1..4.png`, `final_backs_1..4.png`, `final_faces_1..4.png`, `final_clips_1..4.png` (idle, two walk frames, a cast frame, the last death
frame), all in `$S/previews/npca/`. `$S` is the scratchpad of the job, not the repo.

## 6. Tests

* `BlackBullsNpcsTest` (7): the fourteen in order and no id twice; every record is an NPC without boss data (the shape the `arc.roster` self-test insists on); every
  `magic` id is a real `MagicType` id (read from the source file); colours at least 0.40 bright; the arrival groups 4 / 3 / 3 / 2 / 2 (the first four, then four waves) and the gate
  lookup against a fake roster; `registerAll()` twice leaves one copy of each; a boss of the same id is refused, not overwritten.
* `BlackBullsDialogueTest` (16): every file parses clean; keyed, ASCII, 12 to 260 characters; no quest or shop lines; a newcomer is greeted at every stage and saga;
  hints always have lines to pick; every saga has guidance and the early saga two steps; farewells; the gate boss is answered in the greeting; conditions use only
  real story ids and sagas; every line can be said in some situation; every hint has priority 1 and weight 1 to 3; 400 draws never repeat back to back and use at
  least ten lines; a reaction is only heard after that boss fell; the page and place names exist in dialogue AND in the source tree; the floors named are exactly
  the four of section 4.4; no repeated text, nothing forbidden.
* `DialogueFilesTest` (existing, 3): the key numbering. All 26 pass in the local JUnit shim (`$S/mega/jt.sh`); CI runs the real JUnit. Mutation check, run on the
  final files (each change reverted byte for byte afterwards, baseline 26 passed again): a typo in a gate id, Nacht following the wrong gate, Yami no longer
  saying "Equinox", one hint at priority 0, Grey naming "the second floor", and a reaction that lost its `defeated` condition each made the matching tests fail.
* Type check: `tc_wt.sh` on the three Java files: 0 errors. `python3 tools/gen_arc_dialogue_lang.py --check` against `en_us.json` with the fragment merged: passes.

## 7. Canon sources

The wiki pages cannot be fetched from the build machine, so every canon fact below came from search-result summaries, not from a page read in full. The pages the
summaries pointed at (read them before trusting a detail):
https://blackclover.fandom.com/wiki/Asta , https://en.wikipedia.org/wiki/Asta_(Black_Clover) , https://blackclover.fandom.com/wiki/Yami_Sukehiro ,
https://en.wikipedia.org/wiki/Yami_Sukehiro , https://blackclover.fandom.com/wiki/Noelle_Silva , https://blackclover.fandom.com/wiki/Noelle_Silva_vs._Vetto ,
https://blackclover.fandom.com/wiki/Temple_Battle_Royale , https://blackclover.fandom.com/wiki/Underwater_Temple_Arc ,
https://blackclover.fandom.com/wiki/Magna_Swing_vs._Dante_Zogratis , https://blackclover.fandom.com/wiki/Gauche_Adlai ,
https://blackclover.fandom.com/wiki/Large_Reflect_Refrain , https://blackclover.fandom.com/wiki/Secre_Swallowtail ,
https://www.sportskeeda.com/anime/who-secre-black-clover-explained , https://myanimelist.net/character/166026 ,
https://www.sportskeeda.com/anime/what-happened-morgen-black-clover-explained ,
https://comicbook.com/anime/news/black-clover-nacht-brother-morgen-death-what-happened-explained-spoilers/ , https://blackclover.fandom.com/wiki/Heath_Graice
(the wiki's spelling; the mod and the brief use Grice) , https://www.sportskeeda.com/anime/black-clover-every-dead-character-returned-paladin ,
https://blackclover.fandom.com/wiki/Jonna_Agrippa , https://vsbattles.fandom.com/wiki/Gordon_Agrippa , https://vsbattles.fandom.com/wiki/Vanessa_Enoteca ,
https://vsbattles.fandom.com/wiki/Charmy_Pappitson , https://vsbattles.fandom.com/wiki/Nacht_Faust , https://en.wikipedia.org/wiki/List_of_Black_Clover_characters ,
https://tvtropes.org/pmwiki/pmwiki.php/Characters/BlackCloverLaterBlackBulls .

Facts that shaped lines, with how sure they are:

* Noelle's Sea Dragon's Roar blew off Vetto's arm and he regenerated it; Asta's anti-magic sword cut the spell; Yami arrived later in the Seabed Temple fight and
  said he would handle Vetto (summary of the wiki's fight pages). Who struck the last blow is NOT confirmed by a primary page; Yami's "I made the last cut" is
  his boast.
* Magna and Zora went into the Spade Kingdom together; Magna fought Dante and won (summary of the wiki's fight page). Magna's broom being called "Crazy Cyclone"
  comes from one search summary only.
* Heath Grice took his own life when the Black Bulls caught him and was revived years later as one of Lucius's paladins (one news article's summary).
* Secre was Prince Lemiel's servant, sealed him into a statue and spent about five centuries as the bird Nero (several summaries; the anime and the manga
  differ on the name Lemiel / Lumiere).
* Nacht: pale blue eyes, long black hair in a high ponytail, four relics of his four devils (Gimodelo, Slotos, Plumede, Walgner) on a simple string necklace (one summary
  of the wiki, which is where the model's gems come from). His brother Morgen died stopping a forbidden devil-binding ritual (two summaries; the devil's name differs
  between them, so no line names it).
* Gordon's family are curse mages, his father a healer (one summary). Gauche's left eye is a small mirror and Marie is his sister (several).
* Vanessa's feelings for the captain are fan interpretation, so the lines only tease. Finral, Magna and Luck lines about their brothers, houses and rivalries come
  from summaries and memory.

## 8. For the lead (integration)

1. `arc/ArcCharacters.java`, in `bootstrap()`: `com.newuniverse.nusmp.arc.npc.BlackBullsNpcs.registerAll();` (after the boss lines; the order of the NPC lines
   fixes the Hub posts, so keep this block together).
2. Lang: merge `$S/arc/frag/npca.lang.json` (443 keys `arc.dialogue.<id>.<n>`) into `assets/nusmp/lang/en_us.json`, then run
   `python3 tools/gen_arc_dialogue_lang.py --check src/main/resources/assets/nusmp/lang/en_us.json` (it must print nothing and exit 0).
3. `tools/arc/manifest.json`: every builder's branch edits it; after the merge regenerate it with `python3 -B tools/gen_arc_characters.py` and confirm with
   `--check`. Do not hand-merge it.
4. Nothing else: no config class, no command, no quest key, no creative-tab entry (the spawn egg comes from the record), no renderer registration, no mixin.
5. Release notes: fourteen friends in the Hub (names above), arriving as the early story is cleared; thirteen new models.

## 9. Unverified (honest list)

* Nothing ran in the game. The records, the files and the tests are checked by type check, JUnit in the shim, the kit's own `--check` and the preview renderer.
* **The look in game.** The previews are the headless renderer's picture; the real light, the glow layer on a lit entity and the in-game renderer's pose are not seen. The
  clothing details of every model are partly from memory and wiki summaries (each spec's docstring lists what it is unsure about).
* **The size of one model.** Asta's model reaches 2.96 blocks with the sword (Magna's 2.43, Gauche's 2.35, the other ten 2.2 or less; Yami's older model 2.57), while the NPC entity's own box is 0.6 by
  1.95 and the NPC renderer culls with a fixed growth (1.4 sideways, 1.0 up) over it, so a sword tip could pop out when only it is on screen; the name tag floats over the
  entity's box, not the model's head. Gordon (2.08) and Yami (2.06) are taller than the box too.
* **The numbers in the hints** (section 4.3) were read from the source on the day of writing; the tests prove the named pages exist, not the numbers.
* **Rouge** is described in her default (entity) mode; with `thread.rougeEntity` off she is the old cat that undoes one death a day, and the lines would be wrong.
* **Mimics** (Grey's chests with teeth) are the entities of other mods that this mod only recognises (`RelicLoot`); without those mods there are none.
* **The Chronicle line** (Yami) assumes ARC-WORLD's Promenade and lectern exist (`arc.chronicle` on).
* **Hub placement and arrival** are ARC-WORLD's code, read but not run; the gate ids are S1A's today.
* **Canon**: section 7.
