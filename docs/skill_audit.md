# Skill audit: every skill id this mod registers, what works and what does not

Research only. No Java was changed, nothing is committed, the version is not bumped. Written on 2026-11-16 against `HEAD` of this worktree (1,881 commits).

## 0. How to read this (honest scope)

* **Judged by reading, not by playing.** No game was started and nothing was built. A status is my reading of the skill class, the registry, the lang file, the icon folder, the tests and the docs. "WORKING" means "the class reads complete and something tests or names it", not "seen working in a game".
* **Statuses.** WORKING = reads complete, has a way to be used. PARTIAL = it runs when given, but something around it is missing (no way to get it in survival, no display name, outside the class system, no test). NOT FUNCTIONAL = registered but switched off or empty. STALE = reads complete but has not been touched for 1,000+ commits and no dedicated test or self-test names it.
* **Evidence flags I looked for:** empty `use()` body, TODO / stub words, a missing `anime_multiverse.skill.<id>` lang key, no icon file named by the id, no `data/` reference, no self-test or unit test naming the class, no way to obtain it in survival (no reference to its constant outside `NUSkills` and its own file), class never referenced.
* **The date skew.** 47 of the 128 explicit skill classes (and every file named in the pay conversion) were last edited on **2026-11-16** by one mass change (the pay conversion to `NUSkill.pay`, `docs/ultimate_classes.md`), so "last touched" says little for them. For those the useful age signal is **commits in the file history** (a small number means rarely worked on). STALE is only given where the file is also old in commits since.
* **Ids counted.** 347 live skill ids = 128 explicit registrations in `skill/NUSkills.java` (77 grimoire books incl. Forbidden Magic, the Unique and Ultimate skills, Veldanava five, Shishou, Adaptability, Greed) + 30 wiki-attribute books + 105 legacy `grimoire_<type>` skills (one per `MagicType`) + 30 Dragon Ball ids + 54 Beast Will ids. Plus **361 inert legacy stubs** and **64 retired merged-magic stubs** (section 5), which are not live.
* **Not enumerated one by one:** the non-ManasSkill ability families (hero weapons, djinn, wave relics, saiyan, greed forms, devil forms, beast will abilities, avatar forge, songs): they are abilities of items and systems, not registry skills; they are summarised in section 4.

Totals for the 128 explicit ids: **70 WORKING, 33 PARTIAL, 25 STALE, 0 NOT FUNCTIONAL** (plus the 30 Dragon Ball ids, which are NOT FUNCTIONAL by default, section 4).

## 1. The worst skills

1. **The Danmachi and God-line Uniques with no way to get them in survival** (Firebolt, Argonaut, Liaris Freese, Airiel, Hestia, Freya, Hephaestus, Loki, Faith, Wynn Fimbulvetr, Rea Laevateinn, Arcs Ray, Futsunomitama, Uchide no Kozuchi, Hell Walker, Hawk Eye, Spirit Card, Hati, Summoner, Unbreakable Will, Devil's Footprints, Book Maker). The only code that references most of them is the registry and the evolution table. Docs say "obtain: `/tensura edit <player> ability grant anime_multiverse:<id>`". Oldest: **Loki** (last touched 2026-10-18, 1,355 commits ago, 3 commits in its history, no self-test).
2. **`wizard_king`, `spirit_lord`, `anti_magic_spirit_lord`**: real, working skills with a survival path (the crown, the non-grimoire path, mastering Anti-Magic), but **no skill name or description in `en_us.json`**, so the skill list shows a raw key; `wizard_king` also reuses the storm grimoire icon.
3. **The five Veldanava Ultimates** (Sandalphon, Israfil, Jibril, Solomon, Hercules): 250 to 350 lines each, with 10 modes, but **outside the class system** (no `UltimateClass`, own cost and mastery) and given **only by `/multiverse ultimate give`** since the boss and card screen were removed in 3.41.0 (`docs/veldanava_skills.md`). Sandalphon, Jibril and Solomon have not been touched for 300+ commits.
4. **The 30 Dragon Ball skill ids** (15 Unique + 15 Ultimate, 3.56.0): switched off by default by `dragonBallLegacy = false`, taken from players at login. They exist only so old worlds load.
5. **The 25 small grimoire books unchanged since 2026-10-07 (Mercury 2026-10-20)** (Eye, Eyeball, Body, Curse, Chain, Fungus, Food, Crystal, Copper, Iron, Black Oil, Gel, Glass, Bubble, Ice Wedge, Demon x5, Ice, Mercury, Dice, Game, and others): 33 to 80 lines each, 1,000 to 1,880 commits behind HEAD, no dedicated test, only generic list tests name them. They read complete (the Eye book has twelve pages with animations), so they are STALE, not broken.

Also noted: `grimoire_magic` and its 105 `grimoire_<type>` siblings are the legacy path the master guide wants to retire; they still pay flat (`LegacyPay`), outside the class cost.

## 2. Live skills, by status then age (oldest first)

Sorted by status (NOT FUNCTIONAL, PARTIAL, STALE, WORKING), then by last-touched date. "Since" is the number of commits made after the last change to that file; the history count is the number of commits that ever touched it.

### 2.1 PARTIAL (33)

| id | class (file) | what it does | evidence | last touched / commits since / commits in history |
|---|---|---|---|---|
| `loki` | skill/LokiSkill.java | Unique. The Trickster. Vanish from sight and blink forward; monsters that were hunting you lose track of you. | no way to obtain it in survival found: its only other reference is in package avatar (the evolution table or a race check); docs say admin grant (/tensura edit ... ability grant); no self-test or unit test names its class | 2026-10-18 / 1355 / 3 |
| `sandalphon` | entity/veldanava/skills/SandalphonSkill.java | Sandalphon, Lord of Judgment: the first of the five Veldanava ultimate skills (the numbers are in {@link JudgmentRules#SANDALPHON}, the passive rules in {@link JudgmentEvents}). | Ultimate but not an NUSkill: no UltimateClass, own cost and mastery; given only by /multiverse ultimate give (the boss and card screen were removed in 3.41.0) | 2026-10-26 / 318 / 3 |
| `jibril` | entity/veldanava/skills/JibrilSkill.java | Jibril, Lord of Rigor: the third of the five Veldanava ultimate skills (the numbers are in {@link JibrilRules}, the passive rules in {@link JibrilEvents}). Ten modes: four toggle passives (Divine | Ultimate but not an NUSkill: no UltimateClass, own cost and mastery; given only by /multiverse ultimate give (the boss and card screen were removed in 3.41.0) | 2026-10-26 / 311 / 2 |
| `solomon` | entity/veldanava/skills/SolomonSkill.java | Solomon, Lord of Sorcery: the fourth of the five Veldanava ultimate skills (the numbers are in {@link SolomonRules}, the passive rules in {@link SolomonEvents} and {@link SolomonHooks}). Ten modes: fo | Ultimate but not an NUSkill: no UltimateClass, own cost and mastery; given only by /multiverse ultimate give (the boss and card screen were removed in 3.41.0) | 2026-10-26 / 307 / 2 |
| `hercules` | entity/veldanava/skills/HerculesSkill.java | Hercules, Lord of Martial Prowess: the fifth and last of the Veldanava ultimate skills (the numbers are in {@link HerculesRules}, the passive rules in {@link HerculesEvents}). Ten modes: four toggle p | Ultimate but not an NUSkill: no UltimateClass, own cost and mastery; given only by /multiverse ultimate give (the boss and card screen were removed in 3.41.0) | 2026-10-31 / 170 / 3 |
| `anti_magic_spirit_lord` | antimagic/AntiMagicLordSkill.java | Anti-Magic Spirit Lord (Unique). Legacy skill: the Lord is a state of the Anti-Magic grimoire now (book.AntiMagicBook) and this skill is | no skill name / description key in en_us.json (heuristic grep of the id); no self-test or unit test names its class | 2026-11-15 / 78 / 3 |
| `wizard_king` | multiverse/WizardKingSkill.java | The Wizard King's own skill (Unique; only the holder of the crown has it, see {@link WizardKing}). Five abilities, cycle with the | no skill name / description key in en_us.json (heuristic grep of the id); reuses the storm grimoire icon; no self-test or unit test names its class | 2026-11-15 / 78 / 2 |
| `spirit_lord` | multiverse/SpiritLordSkill.java | Spirit Lord (Unique): the path for mages without a grimoire. Granted by the Spirit Lord Skill item (or /multiverse spirit bond); | no skill name / description key in en_us.json (heuristic grep of the id) | 2026-11-15 / 78 / 7 |
| `israfil` | entity/veldanava/skills/IsrafilSkill.java | Israfil, Lord of Trials: the second of the five Veldanava ultimate skills (the numbers are in {@link IsrafilRules}, the passive rules in {@link IsrafilEvents}). Ten modes: four toggle | Ultimate but not an NUSkill: no UltimateClass, own cost and mastery; given only by /multiverse ultimate give (the boss and card screen were removed in 3.41.0) | 2026-11-15 / 78 / 4 |
| `arcs_ray` | skill/ArcsRaySkill.java | Chanted magic (1 line). A piercing beam of light strikes everything in a line, and burns the undead even harder. | no way to obtain it in survival found: its only other reference is in package skill (the evolution table or a race check); docs say admin grant (/tensura edit ... ability grant); no self-test or unit test names its class | 2026-11-16 / 47 / 4 |
| `airiel` | skill/AirielSkill.java | Unique. Tempest: wrap yourself in wind for speed, power and protection from arrows, or ride it in a sudden dash that recovers faster while the wind blows. | no way to obtain it in survival found: no code reference outside the registry and its own file; docs say admin grant (/tensura edit ... ability grant) | 2026-11-16 / 42 / 4 |
| `nen` | skill/NenSkill.java | Ultimate. Hunter x Hunter's life energy in seventeen modes: Ten, Zetsu, Ren, Gyo, En, Ken, Ko, Shu, In, Ryu, the six Hatsu types (Enhancement, Transmutation, Emission, Conjuration, Manipulation, Speci | no way to obtain it in survival found: no code reference outside the registry and its own file; docs say admin grant (/tensura edit ... ability grant) | 2026-11-16 / 42 / 7 |
| `argonaut` | skill/ArgonautSkill.java | Unique. The wish to become a hero. Hold to charge with the chime of bells (up to five), release to strike everything in front of you. | no way to obtain it in survival found: its only other reference is in package skill (the evolution table or a race check); docs say admin grant (/tensura edit ... ability grant) | 2026-11-16 / 37 / 6 |
| `freya` | skill/FreyaSkill.java | Unique. Goddess of Beauty. Your Charm makes nearby monsters forget you and leaves your enemies dazed. | no way to obtain it in survival found: its only other reference is in package avatar (the evolution table or a race check); docs say admin grant (/tensura edit ... ability grant) | 2026-11-16 / 37 / 4 |
| `hephaestus` | skill/HephaestusSkill.java | Unique. Goddess of the Forge. Restore the weapon in your hand, and your off-hand and armour once mastered. | no way to obtain it in survival found: its only other reference is in package avatar (the evolution table or a race check); docs say admin grant (/tensura edit ... ability grant) | 2026-11-16 / 37 / 4 |
| `futsunomitama` | skill/FutsunomitamaSkill.java | Chanted magic (2 lines). A gravity field at the spot you look at crushes foes down, pulls them in and pins them. | no way to obtain it in survival found: its only other reference is in package skill (the evolution table or a race check); docs say admin grant (/tensura edit ... ability grant) | 2026-11-16 / 37 / 6 |
| `uchide_no_kozuchi` | skill/UchideNoKozuchiSkill.java | Chanted magic (3 lines). Level Boost: greatly raise the power of the ally you look at, or your own, for a time. | no way to obtain it in survival found: its only other reference is in package skill (the evolution table or a race check); docs say admin grant (/tensura edit ... ability grant) | 2026-11-16 / 37 / 4 |
| `hell_walker` | skill/HellWalkerSkill.java | Unique. Toggle. The closer you are to death, the harder you hit: at ten percent health your blows deal about double. | no way to obtain it in survival found: its only other reference is in package skill (the evolution table or a race check); docs say admin grant (/tensura edit ... ability grant) | 2026-11-16 / 37 / 4 |
| `unbreakable_will` | skill/UnbreakableWillSkill.java | Unique. Passive. Once in a long while, survive a killing blow and stand back up. Press to see whether your will is ready. | no way to obtain it in survival found: its only other reference is in package skill (the evolution table or a race check); docs say admin grant (/tensura edit ... ability grant) | 2026-11-16 / 37 / 4 |
| `heros_argonaut` | skill/HerosArgonautSkill.java | Ultimate. Evolved from Argonaut. Charge up to ten bells and strike with triple power, or release the charge as a piercing Hero's Beam. | no way to obtain it in survival found: its only other reference is in package skill (the evolution table or a race check); docs say admin grant (/tensura edit ... ability grant) | 2026-11-16 / 37 / 5 |
| `hell_conqueror` | skill/HellConquerorSkill.java | Ultimate. Toggle. Evolved from Hell Walker. Near death your blows become devastating and heal you, and a hellfire aura burns the foes around you. | no way to obtain it in survival found: its only other reference is in package skill (the evolution table or a race check); docs say admin grant (/tensura edit ... ability grant) | 2026-11-16 / 37 / 5 |
| `immortal_will` | skill/ImmortalWillSkill.java | Ultimate. Evolved from Unbreakable Will. Rise from a killing blow at full health, far more often, with a shockwave; Rally heals and cleanses you on demand. | no way to obtain it in survival found: its only other reference is in package skill (the evolution table or a race check); docs say admin grant (/tensura edit ... ability grant) | 2026-11-16 / 37 / 5 |
| `adolla_burst` | skill/AdollaBurstSkill.java | Ultimate. Evolved from Devil's Footprints. Move at the speed of light striking everything on the way, kick with far more force, and unleash the Adolla Burst around you. | no way to obtain it in survival found: its only other reference is in package skill (the evolution table or a race check); docs say admin grant (/tensura edit ... ability grant) | 2026-11-16 / 37 / 5 |
| `all_fiction` | skill/AllFictionSkill.java | Ultimate. Make injuries, enemies and even your own death never have happened. | no way to obtain it in survival found: its only other reference is in package skill (the evolution table or a race check); docs say admin grant (/tensura edit ... ability grant) | 2026-11-16 / 37 / 6 |
| `black_wings` | skill/BlackWingsSkill.java | Ultimate. Toggle. Total reflection of melee and projectiles, no fall damage, and wings that let you glide in the air while you sneak. | no way to obtain it in survival found: no code reference outside the registry and its own file; docs say admin grant (/tensura edit ... ability grant) | 2026-11-16 / 37 / 4 |
| `grimoire_magic` | skill/GrimoireMagicSkill.java | Grimoire Magic (Black Clover). Granted with a grimoire; its magic type, leaf count and devil | legacy skill, extends Tensura Skill not NUSkill, pays flat; the guide plans to retire it | 2026-11-16 / 34 / 8 |
| `liaris_freese` | skill/LiarisFreeseSkill.java | Unique. Passive. While your feelings remain, so does your growth: your kills can raise the mastery of all your skills. | no way to obtain it in survival found: its only other reference is in package skilltree (the evolution table or a race check); docs say admin grant (/tensura edit ... ability grant) | 2026-11-16 / 30 / 3 |
| `spirit_card` | skill/SpiritCardSkill.java | Toggle. A healing aura for you and everyone near you; once mastered it also cleanses harmful effects. | no way to obtain it in survival found: no code reference outside the registry and its own file; docs say admin grant (/tensura edit ... ability grant) | 2026-11-16 / 30 / 4 |
| `hati` | skill/HatiSkill.java | Unique. Passive. Fire, lightning and magic hitting you are partly absorbed and turned into stacking Strength. | no way to obtain it in survival found: no code reference outside the registry and its own file; docs say admin grant (/tensura edit ... ability grant) | 2026-11-16 / 30 / 5 |
| `devils_footprints` | skill/DevilsFootprintsSkill.java | Unique. Flames burst from your feet: dash through the air or deliver a burning kick. Your own fire cannot hurt you. | no way to obtain it in survival found: its only other reference is in package skill (the evolution table or a race check); docs say admin grant (/tensura edit ... ability grant) | 2026-11-16 / 30 / 7 |
| `the_almighty` | skill/TheAlmightySkill.java | Ultimate. See every future and choose the one where you are never harmed. | no way to obtain it in survival found: no code reference outside the registry and its own file; docs say admin grant (/tensura edit ... ability grant) | 2026-11-16 / 30 / 5 |
| `the_visionary` | skill/TheVisionarySkill.java | Ultimate. Whatever you imagine becomes reality: a meteor, a void, a perfect body. | no way to obtain it in survival found: no code reference outside the registry and its own file; docs say admin grant (/tensura edit ... ability grant) | 2026-11-16 / 30 / 6 |
| `gate_of_babylon` | skill/GateOfBabylonSkill.java | Ultimate. The King of Heroes' vault from Fate in ten modes: Gate, Volley, Rain, Chains of Heaven, Golden Armour, Vimana, King's Aura, Gae Bolg, Ea and Elixir. | no way to obtain it in survival found: no code reference outside the registry and its own file; docs say admin grant (/tensura edit ... ability grant) | 2026-11-16 / 30 / 6 |

### 2.2 STALE (25)

| id | class (file) | what it does | evidence | last touched / commits since / commits in history |
|---|---|---|---|---|
| `book_dice` | book/DiceBook.java | Grimoire book (dice), about 4 pages declared in DiceBook.java.  | class unchanged for 1880 commits since 2026-10-07 (1 commit(s) in its history); only generic list tests name it | 2026-10-07 / 1880 / 1 |
| `book_game` | book/GameBook.java | Grimoire book (game), about 6 pages declared in GameBook.java.  | class unchanged for 1880 commits since 2026-10-07 (1 commit(s) in its history); only generic list tests name it | 2026-10-07 / 1880 / 1 |
| `book_demon_beast` | book/DemonBeastBook.java | Grimoire book (demon_beast), about 12 pages declared in DemonBeastBook.java.  | class unchanged for 1852 commits since 2026-10-07 (2 commit(s) in its history); only generic list tests name it | 2026-10-07 / 1852 / 2 |
| `book_curse` | book/CurseBook.java | Grimoire book (curse), about 12 pages declared in CurseBook.java.  | class unchanged for 1852 commits since 2026-10-07 (2 commit(s) in its history); only generic list tests name it | 2026-10-07 / 1852 / 2 |
| `book_curse_warding` | book/CurseWardingBook.java | Grimoire book (curse_warding), about 12 pages declared in CurseWardingBook.java.  | class unchanged for 1852 commits since 2026-10-07 (2 commit(s) in its history); only generic list tests name it | 2026-10-07 / 1852 / 2 |
| `book_demon_fire` | book/DemonFireBook.java | Grimoire book (demon_fire), about 12 pages declared in DemonFireBook.java.  | class unchanged for 1852 commits since 2026-10-07 (2 commit(s) in its history); only generic list tests name it | 2026-10-07 / 1852 / 2 |
| `book_demon_ice` | book/DemonIceBook.java | Grimoire book (demon_ice), about 12 pages declared in DemonIceBook.java.  | class unchanged for 1852 commits since 2026-10-07 (2 commit(s) in its history); only generic list tests name it | 2026-10-07 / 1852 / 2 |
| `book_demon_light` | book/DemonLightBook.java | Grimoire book (demon_light), about 12 pages declared in DemonLightBook.java.  | class unchanged for 1852 commits since 2026-10-07 (2 commit(s) in its history); only generic list tests name it | 2026-10-07 / 1852 / 2 |
| `book_demon_water` | book/DemonWaterBook.java | Grimoire book (demon_water), about 10 pages declared in DemonWaterBook.java.  | 1 TODO/stub word(s) in the class; class unchanged for 1852 commits since 2026-10-07 (2 commit(s) in its history); only generic list tests name it | 2026-10-07 / 1852 / 2 |
| `book_chain` | book/ChainBook.java | Grimoire book (chain), about 12 pages declared in ChainBook.java.  | class unchanged for 1852 commits since 2026-10-07 (2 commit(s) in its history); only generic list tests name it | 2026-10-07 / 1852 / 2 |
| `book_crystal` | book/CrystalBook.java | Grimoire book (crystal), about 12 pages declared in CrystalBook.java.  | 1 TODO/stub word(s) in the class; class unchanged for 1852 commits since 2026-10-07 (2 commit(s) in its history); only generic list tests name it | 2026-10-07 / 1852 / 2 |
| `book_gel` | book/GelBook.java | Grimoire book (gel), about 12 pages declared in GelBook.java.  | class unchanged for 1852 commits since 2026-10-07 (2 commit(s) in its history); only generic list tests name it | 2026-10-07 / 1852 / 2 |
| `book_glass` | book/GlassBook.java | Grimoire book (glass), about 12 pages declared in GlassBook.java.  | class unchanged for 1852 commits since 2026-10-07 (2 commit(s) in its history); only generic list tests name it | 2026-10-07 / 1852 / 2 |
| `book_bubble` | book/BubbleBook.java | Grimoire book (bubble), about 12 pages declared in BubbleBook.java.  | class unchanged for 1852 commits since 2026-10-07 (2 commit(s) in its history); only generic list tests name it | 2026-10-07 / 1852 / 2 |
| `book_ice_wedge` | book/IceWedgeBook.java | Grimoire book (ice_wedge), about 12 pages declared in IceWedgeBook.java.  | class unchanged for 1852 commits since 2026-10-07 (2 commit(s) in its history); only generic list tests name it | 2026-10-07 / 1852 / 2 |
| `book_body` | book/BodyBook.java | Grimoire book (body), about 11 pages declared in BodyBook.java.  | class unchanged for 1849 commits since 2026-10-07 (2 commit(s) in its history); only generic list tests name it | 2026-10-07 / 1849 / 2 |
| `book_eye` | book/EyeBook.java | Grimoire book (eye), about 12 pages declared in EyeBook.java.  | class unchanged for 1849 commits since 2026-10-07 (2 commit(s) in its history); only generic list tests name it | 2026-10-07 / 1849 / 2 |
| `book_eyeball` | book/EyeballBook.java | Grimoire book (eyeball), about 12 pages declared in EyeballBook.java.  | class unchanged for 1849 commits since 2026-10-07 (2 commit(s) in its history); only generic list tests name it | 2026-10-07 / 1849 / 2 |
| `book_fungus` | book/FungusBook.java | Grimoire book (fungus), about 12 pages declared in FungusBook.java.  | class unchanged for 1849 commits since 2026-10-07 (2 commit(s) in its history); only generic list tests name it | 2026-10-07 / 1849 / 2 |
| `book_food` | book/FoodBook.java | Grimoire book (food), about 12 pages declared in FoodBook.java.  | class unchanged for 1849 commits since 2026-10-07 (2 commit(s) in its history); only generic list tests name it | 2026-10-07 / 1849 / 2 |
| `book_copper` | book/CopperBook.java | Grimoire book (copper), about 12 pages declared in CopperBook.java.  | class unchanged for 1849 commits since 2026-10-07 (2 commit(s) in its history); only generic list tests name it | 2026-10-07 / 1849 / 2 |
| `book_black_oil` | book/BlackOilBook.java | Grimoire book (black_oil), about 12 pages declared in BlackOilBook.java.  | class unchanged for 1849 commits since 2026-10-07 (2 commit(s) in its history); only generic list tests name it | 2026-10-07 / 1849 / 2 |
| `book_iron` | book/IronBook.java | Grimoire book (iron), about 12 pages declared in IronBook.java.  | class unchanged for 1847 commits since 2026-10-07 (2 commit(s) in its history); only generic list tests name it | 2026-10-07 / 1847 / 2 |
| `book_ice` | book/IceBook.java | Grimoire book (ice), about 5 pages declared in IceBook.java.  | class unchanged for 1846 commits since 2026-10-07 (3 commit(s) in its history); only generic list tests name it | 2026-10-07 / 1846 / 3 |
| `book_mercury` | book/MercuryBook.java | Grimoire book (mercury), about 4 pages declared in MercuryBook.java.  | class unchanged for 1022 commits since 2026-10-20 (3 commit(s) in its history); only generic list tests name it | 2026-10-20 / 1022 / 3 |

### 2.3 WORKING (70)

| id | class (file) | what it does | evidence | last touched / commits since / commits in history |
|---|---|---|---|---|
| `book_painting` | book/PaintingBook.java | Grimoire book (painting), about 0 pages declared in PaintingBook.java.  | reads complete; covered by a unit test | 2026-10-07 / 1880 / 1 |
| `book_kotodama` | book/KotodamaBook.java | Grimoire book (kotodama), about 0 pages declared in KotodamaBook.java.  | reads complete; covered by a unit test | 2026-10-07 / 1880 / 1 |
| `book_barrier` | book/BarrierBook.java | Grimoire book (barrier), about 12 pages declared in BarrierBook.java.  | reads complete; covered by a unit test | 2026-10-07 / 1852 / 2 |
| `book_cherry_blossom` | book/CherryBlossomBook.java | Grimoire book (cherry_blossom), about 12 pages declared in CherryBlossomBook.java.  | reads complete; covered by a unit test | 2026-10-07 / 1852 / 2 |
| `book_briar` | book/BriarBook.java | Grimoire book (briar), about 11 pages declared in BriarBook.java.  | reads complete; covered by a unit test | 2026-10-07 / 1849 / 2 |
| `book_key` | book/KeyBook.java | Grimoire book (key), about 14 pages declared in KeyBook.java.  | reads complete; covered by a unit test | 2026-10-07 / 1840 / 3 |
| `book_corundum` | book/CorundumBook.java | Grimoire book (corundum), about 13 pages declared in CorundumBook.java.  | reads complete; covered by a unit test | 2026-10-07 / 1840 / 3 |
| `book_time` | book/TimeBook.java | Grimoire book (time), about 12 pages declared in TimeBook.java.  | reads complete; covered by a unit test | 2026-10-08 / 1838 / 2 |
| `book_legion` | book/LegionBook.java | Grimoire book (legion), about 12 pages declared in LegionBook.java.  | reads complete; covered by a unit test | 2026-10-08 / 1830 / 5 |
| `book_bronze` | book/BronzeBook.java | Grimoire book (bronze), about 14 pages declared in BronzeBook.java.  | reads complete; covered by a unit test | 2026-10-11 / 1839 / 3 |
| `book_butoh` | book/ButohBook.java | Grimoire book (butoh), about 12 pages declared in ButohBook.java.  | reads complete; covered by a unit test | 2026-10-16 / 1658 / 4 |
| `book_gravity` | book/GravityBook.java | Grimoire book (gravity), about 5 pages declared in GravityBook.java.  | reads complete; covered by a unit test | 2026-10-18 / 1141 / 2 |
| `book_ash` | book/AshBook.java | Grimoire book (ash), about 0 pages declared in AshBook.java.  | reads complete; covered by a self-test | 2026-10-19 / 1137 / 2 |
| `book_transmutation` | book/WikiBooks.java | Grimoire book (transmutation), about 0 pages declared in WikiBooks.java.  | reads complete; covered by a self-test | 2026-10-19 / 1093 / 14 |
| `book_recombination` | book/WikiBooks.java | Grimoire book (recombination), about 0 pages declared in WikiBooks.java.  | reads complete; covered by a self-test | 2026-10-19 / 1093 / 14 |
| `book_slash` | book/SlashBook.java | Grimoire book (slash), about 4 pages declared in SlashBook.java.  | reads complete; covered by a unit test | 2026-10-20 / 1022 / 4 |
| `book_compass` | book/CompassBook.java | Grimoire book (compass), about 4 pages declared in CompassBook.java.  | reads complete; covered by a unit test | 2026-10-22 / 795 / 2 |
| `book_earth` | book/EarthBook.java | Grimoire book (earth), about 9 pages declared in EarthBook.java.  | reads complete; covered by a self-test | 2026-10-22 / 757 / 3 |
| `book_wind` | book/WindBook.java | Grimoire book (wind), about 6 pages declared in WindBook.java.  | reads complete; covered by a self-test | 2026-10-22 / 679 / 3 |
| `book_dark` | book/DarkBook.java | Grimoire book (dark), about 12 pages declared in DarkBook.java.  | reads complete; covered by a unit test | 2026-10-22 / 679 / 6 |
| `book_light` | book/LightBook.java | Grimoire book (light), about 10 pages declared in LightBook.java.  | reads complete; covered by a unit test | 2026-10-22 / 672 / 3 |
| `book_lightning` | book/LightningBook.java | Grimoire book (lightning), about 11 pages declared in LightningBook.java.  | reads complete; covered by a unit test | 2026-10-22 / 672 / 5 |
| `book_steel` | book/SteelBook.java | Grimoire book (steel), about 5 pages declared in SteelBook.java.  | reads complete; covered by a unit test | 2026-10-22 / 672 / 2 |
| `book_sword` | book/ElementBooks.java | Grimoire book (sword), about 0 pages declared in ElementBooks.java.  | reads complete; covered by a unit test | 2026-10-22 / 672 / 4 |
| `book_explosion` | book/ElementBooks.java | Grimoire book (explosion), about 0 pages declared in ElementBooks.java.  | reads complete; covered by a unit test | 2026-10-22 / 672 / 4 |
| `book_magma` | book/ElementBooks.java | Grimoire book (magma), about 0 pages declared in ElementBooks.java.  | reads complete; covered by a unit test | 2026-10-22 / 672 / 4 |
| `book_mist` | book/ElementBooks.java | Grimoire book (mist), about 0 pages declared in ElementBooks.java.  | reads complete; covered by a unit test | 2026-10-22 / 672 / 4 |
| `book_star` | book/ElementBooks.java | Grimoire book (star), about 0 pages declared in ElementBooks.java.  | reads complete; covered by a unit test | 2026-10-22 / 672 / 4 |
| `book_storm` | book/ElementBooks.java | Grimoire book (storm), about 0 pages declared in ElementBooks.java.  | reads complete; covered by a unit test | 2026-10-22 / 672 / 4 |
| `book_sand` | book/ElementBooks.java | Grimoire book (sand), about 0 pages declared in ElementBooks.java.  | reads complete; covered by a unit test | 2026-10-22 / 672 / 4 |
| `book_shadow` | book/ElementBooks.java | Grimoire book (shadow), about 0 pages declared in ElementBooks.java.  | reads complete; covered by a unit test | 2026-10-22 / 672 / 4 |
| `book_poison` | book/ElementBooks.java | Grimoire book (poison), about 0 pages declared in ElementBooks.java.  | reads complete; covered by a unit test | 2026-10-22 / 672 / 4 |
| `book_reinforcement` | book/ElementBooks.java | Grimoire book (reinforcement), about 0 pages declared in ElementBooks.java.  | reads complete; covered by a unit test | 2026-10-22 / 672 / 4 |
| `book_beast` | book/ElementBooks.java | Grimoire book (beast), about 0 pages declared in ElementBooks.java.  | reads complete; covered by a unit test | 2026-10-22 / 672 / 4 |
| `book_bone` | book/ElementBooks.java | Grimoire book (bone), about 0 pages declared in ElementBooks.java.  | reads complete; covered by a unit test | 2026-10-22 / 672 / 4 |
| `book_blood` | book/ElementBooks.java | Grimoire book (blood), about 0 pages declared in ElementBooks.java.  | reads complete; covered by a unit test | 2026-10-22 / 672 / 4 |
| `book_creation` | book/ElementBooks.java | Grimoire book (creation), about 0 pages declared in ElementBooks.java.  | reads complete; covered by a unit test | 2026-10-22 / 672 / 4 |
| `book_copy` | book/ElementBooks.java | Grimoire book (copy), about 0 pages declared in ElementBooks.java.  | reads complete; covered by a unit test | 2026-10-22 / 672 / 4 |
| `book_illusion` | book/ElementBooks.java | Grimoire book (illusion), about 0 pages declared in ElementBooks.java.  | reads complete; covered by a unit test | 2026-10-22 / 672 / 4 |
| `book_dream` | book/ElementBooks.java | Grimoire book (dream), about 0 pages declared in ElementBooks.java.  | reads complete; covered by a unit test | 2026-10-22 / 672 / 4 |
| `book_anti_magic` | book/ElementBooks.java | Grimoire book (anti_magic), about 0 pages declared in ElementBooks.java.  | reads complete; covered by a unit test | 2026-10-22 / 672 / 4 |
| `book_water` | book/WaterBook.java | Grimoire book (water), about 13 pages declared in WaterBook.java.  | reads complete; covered by a self-test | 2026-10-22 / 667 / 7 |
| `book_fire` | book/FireBook.java | Grimoire book (fire), about 11 pages declared in FireBook.java.  | reads complete; covered by a self-test | 2026-10-22 / 649 / 8 |
| `book_spatial` | book/SpatialBook.java | Grimoire book (spatial), about 10 pages declared in SpatialBook.java.  | reads complete; covered by a unit test | 2026-10-22 / 649 / 6 |
| `adaptability` | adaptability/AdaptabilitySkill.java | Unique. Toggle. A skill that evolves through punishment: every kind of damage you survive is learned, so the next hit of that kind hurts less; Severance is adapted to completely, resistances are learn | reads complete; covered by a unit test | 2026-11-15 / 78 / 2 |
| `book_world_tree` | book/WorldTreeBook.java | Grimoire book (world_tree), about 4 pages declared in WorldTreeBook.java.  | reads complete; covered by a unit test | 2026-11-15 / 78 / 3 |
| `forbidden_magic` | book/ForbiddenBook.java | Grimoire book (forbidden_magic), about 8 pages declared in ForbiddenBook.java.  | reads complete; covered by a unit test | 2026-11-15 / 78 / 4 |
| `lord_of_summons` | skill/LordOfSummonsSkill.java | Ultimate. Evolved from Summoner. Twice the summons and far stronger cards: Beast Card calls three wolves, Stone Card is a colossus, Legion calls a pack of five and the Wraith Card a swarm of flying sp | reads complete; covered by a unit test | 2026-11-16 / 42 / 3 |
| `ki` | skill/KiSkill.java | Ultimate. Dragon Ball's ki in ten modes: Ki Blast, Kamehameha, Instant Transmission, Solar Flare, Afterimage, Super Saiyan, Spirit Bomb, Kaioken, Ultra Instinct and Ki Sense. | reads complete; covered by a self-test | 2026-11-16 / 42 / 4 |
| `shishou` | skill/ShishouUltimateSkill.java | Ultimate. The talking sword's abilities as one skill: switch modes (Telekinesis, Sensory, Defence, Offence, Storage, Magic, Counsel) with the mode key, press the skill key to use the mode, sneak and p | reads complete; covered by a self-test | 2026-11-16 / 42 / 2 |
| `lord_of_gluttonous_greed` | skill/LordOfGluttonousGreedSkill.java | Ultimate. The skill of the Black Sword Greed: devour what you kill (stats, satiation, skills into the Gluttony Vault). Modes: Secret Technique (pays a share of the devoured stats for the technique of  | reads complete; covered by a unit test | 2026-11-16 / 42 / 2 |
| `faith` | skill/FaithSkill.java | Unique. Pray to gather Faith, then spend it to bestow a skill upon a player you look at. Faith is kept when this skill evolves. | reads complete; covered by a unit test | 2026-11-16 / 37 / 5 |
| `firebolt` | skill/FireboltSkill.java | Instant magic. A bolt of lightning-fire fired the moment you press, striking what you look at and scorching everything near it. Mastery doubles the damage. | reads complete; covered by a unit test | 2026-11-16 / 37 / 7 |
| `hestia` | skill/HestiaSkill.java | Unique. Goddess of the Hearth. Heal and bless yourself and every ally around you; once mastered it also cleanses and wards off fire. | reads complete; covered by a unit test | 2026-11-16 / 37 / 4 |
| `wynn_fimbulvetr` | skill/WynnFimbulvetrSkill.java | Chanted magic (3 lines). A blizzard falls on the spot you look at: it freezes everything in it and keeps chilling whatever stays. | reads complete; covered by a unit test | 2026-11-16 / 37 / 6 |
| `rea_laevateinn` | skill/ReaLaevateinnSkill.java | Chanted magic (3 lines). Wave after wave of flame pillars erupt around you and burn every foe that comes close. | reads complete; covered by a unit test | 2026-11-16 / 37 / 5 |
| `goddess_of_faith` | skill/GoddessOfFaithSkill.java | Ultimate. Evolved from Faith. Bestow for half the Faith and half the wait, pray for triple Faith, grant allies a Blessing, and work a Miracle that heals and protects them. | reads complete; covered by a unit test | 2026-11-16 / 37 / 4 |
| `page_chrono_stasis` | skill/TimePages.java | LEGACY: superseded by book.TimeBook (real Tensura magic). Kept registered so old saves load; players are migrated automatically. | time page; icon file not found by id (heuristic), referenced from blackclover; taught by the Time book | 2026-11-16 / 34 / 3 |
| `page_chrono_stasis_grigora` | skill/TimePages.java | LEGACY: superseded by book.TimeBook (real Tensura magic). Kept registered so old saves load; players are migrated automatically. | time page; icon file not found by id (heuristic), referenced from blackclover; taught by the Time book | 2026-11-16 / 34 / 3 |
| `page_time_acceleration` | skill/TimePages.java | LEGACY: superseded by book.TimeBook (real Tensura magic). Kept registered so old saves load; players are migrated automatically. | time page; icon file not found by id (heuristic), referenced from blackclover; taught by the Time book | 2026-11-16 / 34 / 3 |
| `page_time_reversal` | skill/TimePages.java | LEGACY: superseded by book.TimeBook (real Tensura magic). Kept registered so old saves load; players are migrated automatically. | time page; icon file not found by id (heuristic), referenced from blackclover; taught by the Time book | 2026-11-16 / 34 / 3 |
| `page_stolen_time` | skill/TimePages.java | LEGACY: superseded by book.TimeBook (real Tensura magic). Kept registered so old saves load; players are migrated automatically. | time page; icon file not found by id (heuristic), referenced from blackclover; taught by the Time book | 2026-11-16 / 34 / 3 |
| `summoner` | skill/SummonerSkill.java | Unique. Call summon cards: a Beast Card wolf or a Stone Card golem fight beside you, Recall dismisses them. Mastery raises how many can answer. | reads complete; covered by a self-test | 2026-11-16 / 30 / 4 |
| `hawk_eye` | skill/HawkEyeSkill.java | Toggle. Bird Card sight: night vision, and every hostile creature around you glows, even through walls. | reads complete; covered by a unit test | 2026-11-16 / 30 / 3 |
| `book_maker` | skill/BookMakerSkill.java | Grimoire book (maker), about 0 pages declared in BookMakerSkill.java.  | reads complete; covered by a unit test | 2026-11-16 / 30 / 7 |
| `book_mirror` | book/MirrorBook.java | Grimoire book (mirror), about 9 pages declared in MirrorBook.java.  | reads complete; covered by a unit test | 2026-11-16 / 30 / 4 |
| `book_thread` | book/ThreadBook.java | Grimoire book (thread), about 8 pages declared in ThreadBook.java.  | reads complete; covered by a unit test | 2026-11-16 / 30 / 3 |
| `book_plant` | book/PlantBook.java | Grimoire book (plant), about 10 pages declared in PlantBook.java.  | reads complete; covered by a self-test | 2026-11-16 / 30 / 4 |
| `book_sealing` | book/SealingBook.java | Grimoire book (sealing), about 5 pages declared in SealingBook.java.  | reads complete; covered by a unit test | 2026-11-16 / 30 / 4 |
| `book_cotton` | book/CottonBook.java | Grimoire book (cotton), about 0 pages declared in CottonBook.java.  | reads complete; covered by a unit test | 2026-11-16 / 30 / 3 |

## 3. NOT FUNCTIONAL

Only by configuration, not by code: the 30 Dragon Ball ids (section 4). No explicit registration has an empty `use()` body; the checks used were the TODO words (one hit, `book_demon_water` and `book_crystal` each mention a stub word) and a read of `LokiSkill`, `ArcsRaySkill`, `TimePages`, `AntiMagicLordSkill`, `WizardKingSkill`, which all have real bodies.

## 4. Families not enumerated one by one

| Family | Where | Count (READ from files / docs) | Status | Evidence |
|---|---|---|---|---|
| Legacy grimoire skills `grimoire_<type>` | `NUSkills.GRIMOIRES`, `GrimoireMagicSkill` | 105 (one per `MagicType`) | PARTIAL | "What a new grimoire teaches: the Unique book if ported, else the legacy skill. Never both." They exist for types without a book and for old saves; extend Tensura `Skill`, pay flat, UI shows no price (`docs/ultimate_classes.md`) |
| Wiki-attribute books `book_<type>` | `NUSkills.WIKI_ATTRIBUTE_BOOKS`, `book/WikiBooks`, rebuilt families (`EarthFamilyBooks`, `SongRebuild`, `AirBook`, `TreeRebuild`, ...) | 30 | WORKING by reading | rebuilt in 1.93.0; self-tests exist for Song, Tree, Spirit Dive, Shakudo, Light-Bone-Ja |
| Dragon Ball pairs | `DbSkills`, `DbRules` | 15 pairs = 30 ids: Goku, Vegeta, Gohan, Piccolo, Krillin, Goten, Trunks, Future Trunks, Tien, Yamcha, Chiaotzu, Roshi, Bulma, Android 17, Android 18 | **NOT FUNCTIONAL by default** | `dragonBallLegacy = false`: no command, no evolution, no use, taken from players at login; the Dragon Ball rework (`docs/dragonball_skills.md`, `docs/saiyan.md`) replaced them. Ids stay registered |
| Beast Will pairs | `BeastSkills`, `BeastSkillRules` | 27 pairs = 54 ids (`bw_ammit` ... `bw_yamata_no_orochi`) | WORKING by reading | grant on mastery (`beastSkillGrantOnMastery`), evolution, self-test `BeastSkillSelfTests`. The only family with a real survival path |
| Hero weapon abilities | `heroweapon/` (358 files), `docs/heroweapon/` | not counted | not judged | `HeroUseSelfTests`, `HeroSelfTests` exist. Not ManasSkills (`HeroSkill`, `SimpleSkill` are item logic) |
| Djinn | `docs/djinn_roster_36.md` | 36 djinn | not judged | roster document; the djinn code folder is not under `djinn/` (not located) |
| Wave relics | `relics/wave/` (289 files) | not counted | not judged | relic abilities, self-tests per wave |
| Saiyan | `saiyan/` (60 files) | not counted | not judged | rank and form system, `docs/saiyan.md` |
| Greed | `greed/` (18 files) | forms (`GreedForms`), Black Sword | not judged | `GreedSelfTests` exist; its skill `lord_of_gluttonous_greed` is audited above |
| Devil | `devil/` (31 files) | contracts, forms, rituals | not judged | `DevilSelfTests` exist |
| Gate of Babylon, Ki, Nen | `skill/GateOfBabylonSkill` etc. | 3 | audited above | admin grant only |
| Beast Will abilities | `beastwill/BeastWillAbilities` | per will | not judged | `BeastWillSelfTests` |

## 5. Already removed (LegacyIds stubs; registered inert so old worlds load)

These are **not live skills**. Each is an inert, hidden `LegacySkill` stub that the player loses at login (`LegacyMigration`). Source: `skilltree/unique/LegacyIds.java`, `docs/skill_books_removed.md` (2026-11-15), `docs/skilltree/REMOVED.md` (1.96.0).

| List | Count | Removed when / why |
|---|---|---|
| `RETIRED_LINES` (`line_<id>`) | 284 | with the Skill Books, at the owner's request (`docs/skill_books_removed.md`) |
| `RETIRED_TECHNIQUES` (`hk_*`, Heavy Knight) | 12 | same |
| `REMOVED_LINES` (Fairy Tail magics) | 29 | 1.96.0, with the Skill Tree (`docs/skilltree/REMOVED.md`) |
| `REMOVED_TECHNIQUES` (`ft_*`) | 30 | 1.96.0 |
| `REMOVED_SKILLS` | 6: six_eyes, limitless, gold_experience, gold_experience_requiem, vector_manipulation, instant_death | 3.44.0: Six Eyes and its Ultimate Limitless, Gold Experience and Gold Experience Requiem, Vector Manipulation, Instant Death |
| `RetiredMagic` merged books `merged_00` to `merged_63` | 64 | retired merged-magic skills (`core/magic/profile/RetiredMagic`) |
| Total inert ids | 361 + 64 | |

Other removals documented but not stubs: `docs/celestial_requip_removed.md`, `docs/fairy_tail_celestial_books_removed.md`, `docs/elsdocia_legacy.md`.

## 6. Old skills the owner started at one point (grouped by franchise, for a "remove for good" decision)

Everything here already stays out of play: it is either a stub (section 5) or a family that was reworked. The question for the owner is **which stubs can be dropped from the registry for good** (rule 2 keeps them until asked; dropping a stub means an old world that still saved the id may fail to load). Grouping is by name only; no franchise lookup was needed (rule 10 not triggered); `Mixed` is my best-effort bucket and may be wrong for single ids.

### Black Clover (42 ids: 30 line, 12 technique (Heavy Knight))

`abyssal_black_flame`, `anti_magic_blade_dancer`, `black_bulls_vanguard`, `blue_rose_cavalier`, `boar_warrior_might`, `borg_barrier_adept`, `cauldron_witch_alchemist`, `coral_peacock_trickster`, `crimson_lion_blazeguard`, `crozzo_magic_sword`, `dark_triad_hexer`, `devil_union_vessel`, `diamond_crystal_bulwark`, `elven_rite_bearer`, `extreme_magic_adept`, `golden_dawn_prodigy`, `green_mantis_ambusher`, `grimoire_page_scholar`, `heart_kingdom_lifebinder`, `hellflare_flame_lord`, `household_vessel_wielder`, `magic_knight_duelist`, `mana_skin_brawler`, `mana_zone_sentinel`, `metal_vessel_conqueror`, `multi_vessel_warlord`, `purple_orca_tidecaller`, `silver_eagle_marksman`, `spade_beastblood_hunter`, `wizard_king_sovereign`, `hk_provoke`, `hk_shield_wall`, `hk_shield_bash`, `hk_power_strike`, `hk_swift_step`, `hk_crescent_cut`, `hk_gamblers_coin`, `hk_jackpot`, `hk_ember_bite`, `hk_insanity`, `hk_howl_of_ruin`, `hk_fang_of_insanity`

### Mixed / game-system / other series (grouped by id only) (78 ids: 77 line, 1 removed skill (3.44.0))

`aether_bow`, `aether_destruction`, `aether_step`, `aevum_hourglass`, `aggro_manipulation`, `animation_cancel`, `asura_ascension`, `basic_swordsmanship`, `beast_communion`, `black_heavenly_demon_sword`, `bloodlust_pressure`, `bonded_beast`, `boss_pattern_reader`, `braver_command`, `breaking_the_sky_sword`, `cheese_strategy`, `consumable_optimization`, `core_refinement`, `cursed_equipment_mastery`, `damage_rotation`, `debuff_stack_doctrine`, `dryad_world_tree`, `dwarf_guardian`, `dwarven_forge`, `elemental_resist_meta`, `elenoir_wind_weaver`, `endure_ability`, `flame_spirit_ifrit`, `fog_troupe_bandit`, `goliath_colossal_fist`, `greatsword_doctrine`, `grinding_protocol`, `guardian_absolute_defense`, `hidden_class_berserker`, `hoarfrost_art`, `invincibility_frames`, `iron_shadow_guard`, `lady_luck`, `lance_bulwark`, `lance_shadowblade`, `lance_vanguard`, `lee_seolhwa_sword`, `legionary_phalanx_shield`, `little_rookie`, `loot_duplication_glitch`, `magic_tool_artificer`, `mana_beast_hunter`, `mana_rotation`, `min_max_build`, `monster_tamer`, `new_game_plus_memory`, `oath_of_the_heavy_armor`, `overweight_build`, `party_support_rotation`, `perfect_parry_timing`, `prisoner_golden_headband`, `realmheart_physique`, `regis_leywin_devour`, `regressor_hundred_lives`, `save_point_anchor`, `scythe_reaper`, `sequence_break_route`, `seven_seas_swordmaster`, `shield_throw_art`, `shin_yoosung_child`, `siege_calculation`, `spatial_manipulation`, `steel_bulwark`, `stigma_bearer`, `sword_saint_style`, `thunder_art`, `tundra_harpoon_hunter`, `vritra_blood_rite`, `wall_clip_glitch`, `way_of_the_wind`, `werewolf_gale_kick`, `white_tiger_baek`, `instant_death`

### DanMachi (20 ids: 20 line)

`airiel_wind_maiden`, `ais_sword_princess`, `amazon_berserker`, `argonaut_charge`, `bell_firebolt`, `cinder_ella`, `dian_cecht_healer`, `freya_charm`, `goldsmith_radiance`, `haruhime_uchide`, `hephaistos_forge`, `hestia_falna`, `hestia_knife`, `lefiya_viridis_mage`, `liaris_freese`, `rea_laevateinn`, `ryuu_astrea_blade`, `smoldering_fang_of_insanity`, `takemikazuchi_bushi`, `wynn_fimbulvetr`

### Solo Leveling (33 ids: 33 line)

`ashborn_shadow_monarch`, `beast_monarch`, `beru_ant_king`, `cha_hae_in_sword`, `crimson_sovereign`, `double_dungeon_survivor`, `dungeon_conqueror_captain`, `dungeon_supporter`, `frost_monarch`, `go_gunhee_chairman`, `heewon_righteous_knight`, `hunter_ability`, `hunter_healer`, `hunter_mage`, `hunter_ranger`, `hunter_sword_dance`, `igris_blood_red`, `iron_body_monarch`, `jihye_twin_blades`, `kamish_wrath`, `monarchs_domain`, `plague_monarch`, `quicksilver_rush`, `raid_captain_command`, `rulers_authority`, `sangah_war_room`, `shadow_exchange`, `shadow_extraction`, `stealth_skill`, `system_daily_quest`, `temple_delver`, `tyrant_dark_mirror`, `white_flame_sovereign`

### Tensura (25 ids: 25 line)

`baran_demon_monarch`, `beast_will`, `beastkin_lord_carrion`, `demon_king_of_salvation`, `dragon_king_antares`, `dragon_nova_milim`, `dragon_will`, `great_sage_analysis`, `hakurou_blade_way`, `hinata_moonlit_blade`, `kasaka_venom_fang`, `labyrinth_maker_ramiris`, `ogre_lord_might`, `orc_lord_famine`, `predator_gluttony`, `primordial_black_diablo`, `primordial_red_destruction`, `puppet_master_clayman`, `shadow_ninja_souei`, `shuna_holy_weaving`, `slime_body_shapeshift`, `storm_dragon_veldora`, `storm_wolf_ranga`, `vampire_queen_night`, `velzard_frost_dragon`

### Omniscient Reader / system-novel (12 ids: 12 line)

`beta_tester_insight`, `bookmark`, `character_list`, `constellation_patronage`, `dokkaebi_bundle`, `fourth_wall`, `omniscient_viewpoint`, `probability_stories`, `scenario_clearer`, `sea_of_stories`, `secretive_plotter`, `storyteller`

### Magi (19 ids: 19 line)

`dark_rukh_fallen_magi`, `djinn_mirage`, `fanalis_brawler`, `flame_djinn_swordsman`, `flute_magi_prodigy`, `frost_djinn_blade`, `imperial_fan_dancer`, `luminous_wind`, `magic_broom_aerialist`, `magic_carpet_rider`, `magnostadt_staff_wizard`, `magoi_siphoner`, `rukh_caller_priest`, `shadow_djinn_assassin`, `solomon_wisdom_sage`, `spirit_dive_rider`, `stone_djinn_bulwark`, `thunder_djinn_duelist`, `tide_djinn_dancer`

### Fairy Tail (69 ids: 10 line, 29 removed line, 30 removed technique)

`ember_flame_art`, `erza_requip_knight`, `gajeel_iron_dragon`, `gildarts_crash`, `gray_ice_make`, `juvia_water_body`, `laxus_lightning_dragon`, `mirajane_satan_soul`, `natsu_fire_dragon`, `wendy_sky_dragon`, `arc_of_time`, `beast_arm_takeover`, `card_magic`, `celestial_spirit_magic`, `crash_magic`, `dark_ecriture`, `end_demon_awakening`, `exceed_aera`, `fairy_law`, `fire_dragon_slayer`, `gravity_change`, `heavenly_fire_god_slayer`, `ice_make`, `iron_dragon_slayer`, `lightning_dragon_slayer`, `poison_dragon_slayer`, `requip_gunslinger`, `requip_titania`, `satan_soul`, `seith_totems`, `shadow_dragon_slayer`, `sky_dragon_slayer`, `solid_script`, `telekinesis_mind_hand`, `titan_magic`, `water_body`, `white_dragon_slayer`, `wizard_saint_rock_magic`, `wood_make`, `ft_fire_dragons_iron_fist`, `ft_fire_dragons_roar`, `ft_fire_dragons_claw`, `ft_fire_dragons_brilliant_flame`, `ft_fire_dragons_wing_attack`, `ft_fire_dragons_sword_horn`, `ft_crimson_lotus_exploding_flame_blade`, `ft_lightning_dragons_iron_fist`, `ft_lightning_dragons_roar`, `ft_lightning_dragons_breakdown_fist`, `ft_lightning_dragons_heavenward_halberd`, `ft_thunder_palace`, `ft_lightning_dragons_jaw`, `ft_lightning_dragons_flash`, `ft_roaring_thunder`, `ft_sky_dragons_roar`, `ft_sky_dragons_wing_attack`, `ft_sky_dragons_wave_wind`, `ft_sky_dragons_breath`, `ft_sky_troia`, `ft_sky_arms_vernier`, `ft_sky_drill`, `ft_ice_make_lance`, `ft_ice_make_floor`, `ft_ice_make_shield`, `ft_ice_make_cannon`, `ft_ice_make_saucer`, `ft_ice_make_eagle`, `ft_ice_make_hammer`, `ft_ice_make_excalibur`

### God of High School (29 ids: 29 line)

`goh_apollo_charyeok`, `goh_ares_charyeok`, `goh_artemis_charyeok`, `goh_athena_charyeok`, `goh_capoeira`, `goh_charyeok_vessel`, `goh_hades_charyeok`, `goh_haetae_jade`, `goh_hapkido`, `goh_hephaestus_charyeok`, `goh_heracles_labors`, `goh_hermes_charyeok`, `goh_ilpyo_sword`, `goh_judo`, `goh_jumeok_fist`, `goh_kendo`, `goh_ki_cultivation`, `goh_lu_bu_moonlight`, `goh_mighty_style`, `goh_monkey_king`, `goh_muay_thai`, `goh_nyuh_weapon_forms`, `goh_pankration_grappler`, `goh_poseidon`, `goh_taejin_charyeok`, `goh_taekwondo`, `goh_tiger_crane_kungfu`, `goh_tournament_contender`, `goh_zeus_charyeok`

### Tower of God (29 ids: 29 line)

`tog_agnis_ice`, `tog_anak_zahard`, `tog_beast_tamer`, `tog_black_march`, `tog_faerie`, `tog_fisherman`, `tog_floor_guard`, `tog_fug_assassin`, `tog_guide`, `tog_headon_channel`, `tog_inventory`, `tog_irregular`, `tog_jahad_royal`, `tog_light_bearer`, `tog_lighthouse_keeper`, `tog_rak_wraithraiser`, `tog_ranker_authority`, `tog_regular_climber`, `tog_reverse_flow_control`, `tog_rose_shower`, `tog_scout`, `tog_shinsu_reinforcement`, `tog_shinsu_shooting`, `tog_spear_bearer`, `tog_suspendium_arms`, `tog_true_flame`, `tog_wave_controller`, `tog_wolhaiguk_moon`, `tog_zahard_decree`

### Jujutsu Kaisen (2 ids: 2 removed skill (3.44.0))

`six_eyes`, `limitless`

### JoJo's Bizarre Adventure (2 ids: 2 removed skill (3.44.0))

`gold_experience`, `gold_experience_requiem`

### A Certain Magical Index (1 ids: 1 removed skill (3.44.0))

`vector_manipulation`

### Also started, then reworked or switched off (not stubs)

* **Dragon Ball, first version (30 ids, 3.56.0)**: replaced by the saiyan rework, off by default (`dragonBallLegacy`). Ids stay registered.
* **Grimoire legacy skills (105 `grimoire_<type>` + `grimoire_magic`)**: replaced by `BOOK_*` families, kept for types without a book. The guide plans to retire them (P2 #8).
* **Ultimate Magic grimoire pages (`ultimate/`)**: a separate system from the class ranking; kept.
* **Veldanava boss and card screen** (3.41.0): removed; the five skills stay, admin command only.
* **Skill Tree / Skill Points / Arts bar (1.96.0)**, **Skill Books and SP books**, **Black Market shelf** (empty), **Fairy Tail celestial books and requip**: removed (`docs/skilltree/REMOVED.md`, `docs/skill_books_removed.md`, `docs/celestial_requip_removed.md`).
* **Elsdocia** (`docs/elsdocia_legacy.md`) and the arc modules under `arc/`: not skills; not audited.

## 7. Candidate new end-game skills (12)

Each is checked only by a keyword grep over `docs/` and `src/main/java/` (0 hits unless noted), which proves nothing is named that way, not that the idea is absent. Class ties use `UltimateClass` (LORD / KING / GOD / DRAGON) and the Nightmares-derived targets in `docs/ultimate_ranking_nightmares.md` section 8.4 (modes and gates). Canon for the franchise mechanics was **not** looked up in a wiki in this pass (these are proposals; rule 10 applies when one is built).

| # | Name | Source | Mechanic (one line) | Class tie and gate | Overlap check |
|---|---|---|---|---|---|
| 1 | **Hakai (Destruction)** | Dragon Ball Super | Erase one target whose EP ratio is below a threshold (uses the unused `killBelow` 0.60 data of GOD); otherwise heavy damage; long cooldown; never works on bosses with a damage cap. | GOD; 10M+ EP gate; single-owner cap (proposal item 7) | "hakai" 0 hits; `killBelow` exists but no skill reads it |
| 2 | **Fusion Dance / Potara** | Dragon Ball | Two consenting players near each other merge stats for a timed window, then separate; server-checked, both stay separate entities. | KING; both must hold a DB Ultimate; no EP floor | "potara", "fusion dance" 0 hits; multiplayer risk, needs a consent handshake and a hard cap on time |
| 3 | **Eternal Dragon's Wish** | Dragon Ball | Once per real week, choose one of five wishes (revive, Max EP step, skill reroll, a class-bound item, a heal). Nothing is created per tick. | GOD; collects 7 Dragon Balls (we already have Dragon Radar items) | "shenron" 3 hits: check that none is a wish skill first |
| 4 | **Unlimited Blade Works** | Fate/stay night | A bounded domain that fills with copies of weapons you have seen; fires a fixed number per cast. | KING; sibling of Gate of Babylon (same class, shares the weapon pool code) | "unlimited blade" 1 hit (check), "reality marble" 0 |
| 5 | **Jajanken Overdrive (Adult Gon)** | Hunter x Hunter | Spend max health for a short window to multiply Nen output; the spent health returns slowly and never past the cap; cannot be used twice in a row. | LORD; Nen mastered (existing `nen` skill); 500K EP | "adult gon", "jajanken" 0 |
| 6 | **Emperor Time** | Hunter x Hunter | Chain-bound rule: while active you may use all six Nen types at full strength but your life drains per second; lasts until a timer ends. | LORD to KING; Nen mastered | "emperor time" 0 |
| 7 | **Arcanum: Divine Heart** | DanMachi | A once-per-life Falna ability that lets you bank a Level-up as a temporary boost; ties to `DungeonLevel` / `HowlMath` in `arc/danmachi`. | KING; Danmachi Level 7 gate | "arcanum" 1 hit (check) |
| 8 | **Storm Dragon, Lord of Storms** | Tensura (base game) | A bonded True Dragon ally: bond points rise with joint kills; two active modes (storm breath, tempest dive) and one passive; the Nightmares Dragon tier is the shape. | DRAGON (the empty class); gate: bond points, not EP | "storm dragon" 0; "veldora" 3 hits |
| 9 | **Evangelist's Ignition** | Fire Force | Raises Adolla Burst to a sealed state: a larger burst, longer, then a forced rest; a second form of the existing skill. | KING; mastered `adolla_burst` | "evangelist", "adolla link" 0 |
| 10 | **Hundred-Year Archive** | Black Clover (Elf Tribe / Time Magic) | Read the last minute of events around a point and replay one chosen window as a bounded rewind for your party; a cooldown of 20 minutes. | KING; Time grimoire page mastered | rewind exists in `MemoryRewind` / `TimePages`; check overlap before building |
| 11 | **Shadow Monarch: Arise (only if the owner re-opens it)** | Solo Leveling | Raise a bounded number of defeated foes as temporary allies for the length of one fight. | KING; EP 5M | the Solo Leveling lines were retired (section 5); rule 2 means ask first |
| 12 | **Fairy Law (only if the owner re-opens it)** | Fairy Tail | Area spell that only hits foes (friend-or-foe check on the server), one big hit and a long cooldown. | GOD; EP 10M | the line `fairy_law` was removed (section 5); ask first |

Worst-case cost note (rule 11): every candidate is an on-use effect with a cooldown; none needs a per-tick scan. The two multi-player ones (Fusion Dance, Wish) need a server consent check and a global per-world cap.

## 8. What was not checked

* No skill was tested in a game. Statuses are from reading, the lang file, the icon folder, the references and the test sources.
* The lang heuristic looked for `anime_multiverse.skill.<id>`; a skill whose name lives under another key would be marked wrong. `wizard_king`, `spirit_lord`, `anti_magic_spirit_lord` were confirmed absent by a broader grep of the same file.
* The "no way to obtain in survival" flag means no reference to the skill's constant outside `NUSkills` and its own file, plus the docs saying admin grant. A datapack, a quest or an NPC could still grant a skill through an id string I did not search for.
* The icon flag checks `textures/skill/**/<id>.png`; grimoire books use `textures/skill/grimoire/<type>.png`, so a missing flag on a book is not a defect.
* The non-ManasSkill families (section 4) were counted, not judged.
* "Last touched" dates for 47 classes come from one mass edit (2026-11-16).
