# Keybind and skill-slot consolidation: audit and proposal

Owner request: "I don't want to have so many keybinds. And that should go for anything I add in my mod. Check to see if you could add it into like a Tensura-like skill to make it work better and not so many keybinds."

Status: **research only.** No Java or resource was changed (this file and `tools/audit/keybind_inventory.py` are the only additions). Nothing was built or run in a game. Written against `HEAD` c7ec12791 (mod 4.14.1). All paths are under `src/main/java/com/newuniverse/nusmp/` unless they start with `docs/` or `tools/`. A second worker is building the "Saiyan Prodigy" hub skill (one intrinsic multi-mode skill for all Saiyan abilities); this audit covers everything else and says what that helper must offer so the other groups can reuse it.

Sources: this repository (grep and reading), and the Tensura Reincarnated 2.0.1.0 jar that the build uses (`build.gradle:70`; class files read with `javap`, no source). The Fandom wikis (rule 10) were not needed: this is a code audit, no canon was looked up.

## 0. The numbers

| Measure | Count |
|---|---|
| Keybindings the mod registers (`new KeyMapping`, all registered through `RegisterKeyMappingsEvent`) | **56** |
| ... bound by default / unbound by default | 50 / 6 |
| ... that trigger an ability or a state change in play | 43 |
| ... that open a screen, toggle a HUD or a visual setting | 13 |
| ... of those 43, Saiyan alone | 23 (41 % of all 56) |
| Default keys that share a key with another mod key or a Tensura default key | **21** of 56 (section 2.2) |
| Keys Tensura itself adds (not ours): slot 1-3, next/previous mode, race ability, dodge, main GUI, name, reload | 10 (`TensuraKeybinds`, verified in the jar, plus a hidden `key.number_0`) |
| Live skill ids in the registry | 347 per `docs/skill_audit.md:11` + 16 Faith-god ids (`skill/NUSkills.java:283`, not in that audit) = about **363** |
| ... of those that are a book (one skill per grimoire family, pages are modes) | 106 (76 explicit + 30 wiki books, `skill/NUSkills.java`) |
| ... that are legacy and off or inert (105 `grimoire_<type>` + `grimoire_magic`, 30 Dragon Ball ids off by default `NUConfig.java:283`) | 136 |
| ... "own slot" skills that are not books and not legacy (explicit classes, Beast pairs, Faith pairs) | about **120** ids (section 4) |
| Inert stubs kept only so old saves load | 361 + 64 (`docs/skill_audit.md:194-206`) |

The six unbound-by-default keys are `saiyan_branch`, `multiverse_status`, `toggle_relic_visuals`, `toggle_show_worn_relics`, `grimoire_slot`, `grimoire_shelf`.

Top five consolidations (details in sections 5 and 7):

1. Saiyan Prodigy hub: **-23 keys** (in progress elsewhere).
2. Hero weapon hub (3 technique keys + summon + change form, plus the Curse key as a sixth mode): **-6 keys**, and it removes five default collisions with Tensura.
3. Beast Will hub (acquire, integrate, ability 1, ability 2, swap): **-5 keys**, and it removes four collisions with Saiyan.
4. UI key merge (GUI Size into GUI Move, System U / I / Floor card / Quest tracker into the System window): **-5 keys**, no skill involved.
5. Relic ring takes Djinn (J / K / H) and the Gambler's page (R): **-4 keys**; plus three small fold-ins (Greed R, Devil N, Song V): **-3 keys**.

All of it together: **56 -> 10 keys** (5 bound: F9, Y, M, G, V; 5 unbound).

## 1. Method and what could not be verified

* Keys: `tools/audit/keybind_inventory.py` (static scan of `static final KeyMapping X = new KeyMapping(...)`, 56 hits, matching `grep -c "new KeyMapping("`); every file with `KeyMapping` was listed (21 files, all under `client/`); every `consumeClick` / `isDown` in `client/` was read. `grep` found **no** `InputEvent`, scroll, `options.key*` or mixin key handler anywhere in `src/main`, and no `ClientTick` handler reading a raw GLFW key, so the 56 are the complete set. The only other key use is `client/manazone/ZoneCastClient.java:53`, which only **reads** Tensura's slot keys.
* Tensura facts come from bytecode of the 2.0.1.0 jar (`TensuraKeybinds`, `AbilityStorage`, `AbilityPreset`, `AbilitySlot`, `ISubAbilityModeHolder`, `PrideSkill`) and ManasCore skill 4.0.0.2 (`ManasSkill` public methods).
* **Not verified, because nothing was run:** how the game behaves when two mappings share one key (vanilla lets only one fire; NeoForge's lookup may fire both: I did not test it); whether Tensura's own ability HUD shows the mode name of a slot; how Tensura's next/previous-mode keys interact with a slot that already stores a mode; the real cost of `instance.setCoolDown` sync; whether a skill granted at login shows up in the slot picker without a preset edit; every "lost" and "risk" below is reasoning from code.
* The skill counts of section 4 come from `skill/NUSkills.java`, `docs/skill_audit.md` and the `getModes` overrides (`grep "int getModes"`). A class without a `getModes` override is read as one mode.

## 2. Keybind inventory (all 56)

Group, field, default key, what it does, where it is declared (file:line), where the press is handled. "Ask" = the client only sends a packet; the server decides.

### 2.1 The table

| # | Group | Field | Default | Does | Declared | Handled |
|---|---|---|---|---|---|---|
| 1 | Saiyan | `BLAST` | `,` | Ki Blast (ask, op 1) | client/saiyan/SaiyanClient.java:56 | :170 `tick` -> `press` :248 |
| 2 | Saiyan | `BARRAGE` | `-` | Rapid Ki Barrage (op 2) | :57 | :170 |
| 3 | Saiyan | `FLIGHT` | G | Ki Flight on/off (op 3) | :59 | :170 |
| 4 | Saiyan | `BURST` | Left Alt | Master burst-dash flight (op 4) | :60 | :170 |
| 5 | Saiyan | `SENSE` | Insert | Saiyan Senses radar (op 5) | :61 | :170 |
| 6 | Saiyan | `SUPPRESS` | Home | Ki Suppression (op 6) | :62 | :170 |
| 7 | Saiyan | `COMBO` | End | Rock Scissors 'N' Paper, one part per press (op 7) | :63 | :170 |
| 8 | Saiyan | `KAME` | Delete | Kamehameha, hold to charge, release to fire (ops 8/9) | :64 | :199 `kamehameha` |
| 9 | Saiyan | `TRANSMIT` | `\` | Instant Transmission (op 13) | :66 | :170 |
| 10 | Saiyan | `BARRIER` | `]` | Ki Barrier (op 14) | :67 | :170 |
| 11 | Saiyan | `SPIRIT` | `[` | Spirit Bomb, hold / release (ops 15/16) | :68 | :213 `spiritBomb` |
| 12 | Saiyan | `METEOR` | `;` | Meteor Combination (op 17) | :69 | :170 |
| 13 | Saiyan | `DRAGON` | Numpad 1 | Dragon Fist (op 18) | :71 | :170 |
| 14 | Saiyan | `BIND` | Numpad 2 | God Bind (op 19) | :72 | :170 |
| 15 | Saiyan | `DEFLECT` | Numpad 3 | Ki Deflection (op 20) | :73 | :170 |
| 16 | Saiyan | `ENERGY` | Numpad 4 | Energy Transfer (op 21) | :74 | :170 |
| 17 | Saiyan | `TECH_NEXT` | Numpad 6 | Cycle the technique cursor over 11 techniques (`SaiyanNet2.CYCLE`, SaiyanNet2.java:19) | :76 | :227 `techKeys` |
| 18 | Saiyan | `TECH_USE` | Numpad 5 | Use the technique the cursor points at | :77 | :227 |
| 19 | Saiyan | `WAVE` | Numpad 7 | Ki Wave, hold / release (ops 29/30) | :78 | :227 |
| 20 | Saiyan | `BRANCH` | unbound | Form branch switch (op 35; the server ignores it unless `branchKey` is on, saiyan/SaiyanTechniques4.java:109) | :79 | :227 |
| 21 | Saiyan | `FORM` | `` ` `` | Enter / leave the selected form (op 10) | :80 | :170 |
| 22 | Saiyan | `FORM_NEXT` | Page Up | Next form (op 11) | :81 | :170 |
| 23 | Saiyan | `FORM_PREV` | Page Down | Previous form (op 12) | :82 | :170 |
| 24 | Beast Will | `ACQUIRE` | `[` | Toggle Acquire form (ask op FORM) | client/beastwill/BeastWillClient.java:45 | :133 `tick` |
| 25 | Beast Will | `INTEGRATE` | `]` | Toggle Integrate form | :46 | :133 |
| 26 | Beast Will | `ABILITY1` | `;` | Ability 1 of the held will | :47 | :133 |
| 27 | Beast Will | `ABILITY2` | `\` | Ability 2 of the held will | :48 | :133 |
| 28 | Beast Will | `SWAP` | `=` | Next will, Shift = previous | :49 | :133 |
| 29 | Hero weapons | `SKILL_1` | Z | Technique 1 of the bound weapon (press / hold heartbeat / release) | client/heroweapon/HeroKeysClient.java:24 | :53 `tick` |
| 30 | Hero weapons | `SKILL_2` | X | Technique 2 | :25 | :53 |
| 31 | Hero weapons | `SKILL_3` | C | Technique 3 | :26 | :53 |
| 32 | Hero weapons | `SUMMON` | B | Summon / put away the weapon | :28 | :78 |
| 33 | Hero weapons | `CYCLE` | N | Change to the next learned form | :30 | :79 |
| 34 | Hero weapons | `EMBRACE` | O | Curse offer: hold Sneak + O for 3 s to accept | client/heroweapon/curse/CurseClient.java:43 | :98 |
| 35 | Hero weapons | `KEY` (Weapon Book) | M | Opens the Weapon Book (forms) screen | client/heroweapon/forms/FormsClient.java:31 | :44 |
| 36 | Black Clover | `ASSIMILATE` (Devil) | N | Switch devil assimilation on / off | client/devil/DevilClient.java:58 | :162 `tick` |
| 37 | Black Clover | `SUMMON` (Grimoire) | G | Float the Grimoire to the hand / hip (Shift = back) | client/grimoire/GrimoireSlotClient.java:28 | :38 |
| 38 | Black Clover | `KEY` (Grimoire slot) | unbound | Open the Grimoire slot screen | :24 | :37 |
| 39 | Black Clover | `KEY` (Grimoire shelf) | unbound | Open / close the shelf the player looks at | client/grimoire/GrimoireShelfClient.java:56 | :110 |
| 40 | Magi | `UP` (Djinn) | J | Raise the Djinn phase (Shift + J dismisses) | client/relics/djinn/DjinnKeys.java:20 | :36 |
| 41 | Magi | `DOWN` (Djinn) | K | Lower the phase | :21 | :40 |
| 42 | Magi | `RUKH` (Djinn) | H | Draw Rukh | :24 | :44 |
| 43 | Relics | `USE` (relic) | R | Use the Gambler's Grimoire Page worn in a relic slot | client/nexus/RelicKeys.java:19 | :26 |
| 44 | Relics | `USE` (worn relic ring) | V | Use the selected ability of the worn relics (Shift = next; hold-type acts charge) | client/relics/RelicActKeys.java:30 | :67 |
| 45 | Relics | `TOGGLE_VISUALS` | unbound | Show / hide worn armour-relic visuals | :34 | :72 |
| 46 | Relics | `TOGGLE_SHOW_WORN` | unbound | Show / hide all worn relics | :38 | :76 |
| 47 | Berserk | `FORM` (Greed) | R | Next form of the Black Sword Greed (Shift = previous) | client/greed/GreedClient.java:34 | :56 |
| 48 | Song | `SING` | V | Hold to sing (stands in for a microphone without Simple Voice Chat) | client/song/SongClient.java:26 | :43 |
| 49 | System / UI | `TOGGLE` | Y | Show / hide the System window | client/mega/system/SystemClient.java:41 | :65 |
| 50 | System / UI | `EDIT` | U | System window edit mode | :43 | :65 |
| 51 | System / UI | `POINTS` | I | Open the stat point screen | :45 | :65 |
| 52 | System / UI | floor card `KEY` | `.` | Show the floor report | client/mega/system/FloorCardClient.java:45 | :91 |
| 53 | System / UI | quest tracker `TOGGLE_TRACKER` | `'` | Show / hide the quest tracker | client/elite/hub/HubClient.java:33 | :58 |
| 54 | System / UI | Multiverse status `KEY` | unbound | Open the full Multiverse status screen | client/multiverse/MultiverseStatusClient.java:30 | :49 |
| 55 | System / UI | GUI Size `KEY` | F8 | Open the GUI Size screen | client/ui/GuiScale.java:44 | :60 |
| 56 | System / UI | GUI Move `KEY` | F9 | Open the HUD edit mode | client/ui/move/GuiMove.java:55 | :77 |

Per group: Saiyan 23, Beast Will 5, Hero weapons 7 (5 shared weapon keys + Weapon Book + Curse), Black Clover 4, Magi (Djinn) 3, Relics 4, Berserk 1, Song 1, System / UI 8 = 56.

Observations that matter for the design:

* The hero weapon keys already are a consolidation: one set of five keys serves all 22 `HeroKind` weapons (heroweapon/HeroKind.java:16-37). Beast Will is the same: one set of five for 27 wills. The relic ring (`V`) already serves every worn relic, Infinity Stone and wave relic through one key (client/relics/RelicActKeys.java:3-8). Only Saiyan has one key per move.
* Saiyan has **two** systems for its moves at once: 12 direct keys plus a 2-key cycle over 11 more techniques (`SaiyanNet2.CYCLE`). A cursor-and-use design is already accepted in the mod.
* `ManasSkill` offers `onPressed`, `onHeld`, `onRelease`, `onScroll`, `canScroll`, `canBeToggled`, per-mode `getCoolDown` (listed from the 4.0.0.2 jar). So hold-to-charge (Kamehameha, Spirit Bomb, Ki Wave, hero technique heartbeats) and toggles (Flight, Senses, Suppression, form) all have a native place in a skill. `skill/ArgonautSkill.java:113-152` already uses `onHeld` / `onRelease`.

### 2.2 Default-key collisions (21 keys of 56)

Pure facts (default values read from our declarations and from `TensuraKeybinds`' static initialiser: B 66, N 78, Z 90, X 88, C 67, Left Alt 342, R 82, V 86).

| Key | Mod keys on it | Also a Tensura default |
|---|---|---|
| `[` | Saiyan Spirit Bomb, Beast Will Acquire | |
| `]` | Saiyan Ki Barrier, Beast Will Integrate | |
| `;` | Saiyan Meteor Combination, Beast Will Ability 1 | |
| `\` | Saiyan Instant Transmission, Beast Will Ability 2 | |
| N | Devil Assimilate, Hero Change Form | Tensura "name" |
| R | Greed Form, Use Relic (Gambler's page) | Tensura "race ability" |
| G | Saiyan Ki Flight, Grimoire Summon | |
| V | Song Sing, Use Worn Relic | Tensura "dodge" |
| Z, X, C | Hero technique 1, 2, 3 | Tensura ability slot 1, 2, 3 |
| B | Hero Summon | Tensura main GUI |
| Left Alt | Saiyan Burst | Tensura "next ability mode" |

`docs/heroweapon/core.md:101` and `client/relics/djinn/DjinnKeys.java:6-9` claim the keys are free, but both only checked the mod's own keys, not Tensura's. The consolidation removes most of these as a side effect (the Saiyan, Beast Will, Hero and Greed / Devil keys all disappear). I did not test whether the game then fires both actions or one.

## 3. How Tensura / ManasCore do modes, and how this repo does them

**Tensura model (verified in bytecode):**

* Three activation keys: `ACTIVATE_SLOT_1..3` (default Z, X, C) plus `NEXT_ABILITY_MODE` (Left Alt) and `PREVIOUS_ABILITY_MODE` (unbound), `RACE_ABILITY` (R), `DODGE` (V).
* A slot stores a **(skill, mode) pair**: `AbilitySlot{ManasSkill skill; int mode}`; an `AbilityPreset` holds exactly 3 slots (`AbilityPreset.<init>` does `getEmptyList(3)`); `AbilityStorage` keeps a list of presets with an `activePreset`. So a player's live loadout is 3 (skill, mode) cells per preset, switchable by preset. A skill with 20 modes costs **no extra key**: the player drops the 3 modes they want into the 3 cells. Tensura ships `SubAbilityModeSelectionScreen` (a scrolling list of one skill's modes to pick into a slot) and `AbilitySelectionScreen`; this repo already themes both (`client/ui/theme/TensuraScreenTheme.java:24`).
* `ManasSkill.onPressed(instance, entity, key, mode)` receives the slot's mode (that is why every skill here has an `int mode` argument). `getModes`, `nextMode`, `getModeId`, `getModeName`, per-mode `instance.getCoolDown(mode)` / `setCoolDown(seconds, mode)`.
* A second, unused-here pattern: `ISubAbilityModeHolder` (implemented by Tensura's `PrideSkill` and `TensuraSkill`): a skill whose modes are **other skills** (sub-skills), with `learnSubSkill`, `removeSubSkill`, `getModeCooldown`, `getModeMastery` per sub-skill. That is "a hub of real skills", with per-sub-skill mastery. This repo does not use it (`grep` finds only the theme comment).

**This repo's pattern (the one to reuse):**

* `skill/NUSkill.java:32` is the base: text, icon, price, mastery, milestones. Multi-mode skills override the five mode methods: `SummonerSkill` (skill/SummonerSkill.java:56-95, modes appended only, `nextMode` skips unlisted modes, locked modes refuse in `onPressed`), `NenSkill` (17 modes, skill/rules/NenRules.java:18), `KiSkill` (10, KiRules.java:13), `GateOfBabylonSkill` (10), `AllFictionSkill` (10 / 16 / 18, FictionRules.java:60-140), `DbSkill` / `BeastSkill` / `FaithGodSkill` (the wheel comes from a data row), `GrimoireBook` (`getModes = pages().size()`, book/GrimoireBook.java:228).
* Two merge precedents that already carry the save-compatibility rules the owner wants: `skill/SummonerMigration.java` (Hawk Eye and Spirit Card become Summoner modes: opens the mode for good in the Summoner tag, copies the toggle and the mastery share, forgets the old skill, the old id stays registered) and `skill/ArgonautMerge.java` (Firebolt folded into Argonaut: SERVER setting, one-off migration at login, a stamp on the instance so switching the setting off gives the old skill back). Both sit behind a setting that restores the old behaviour (`NUConfig.java:159`, `:324`).
* Already hub-like and **keyless**: grimoire books (1 skill per family, pages = modes), Summoner, Nen, Ki, Gate, All Fiction, Shishou (7 modes), Veldanava Ultimates (5 skills, about 10 modes each), Faith (5), Goddess of Faith (7), Wizard King (5), Spirit Lord (3), Lord of Gluttonous Greed (3 modes: `Secret Technique`, `Half-Starvation`, `Offer`, skill/LordOfGluttonousGreedSkill.java:25).

## 4. What needs its own key or its own slot, by franchise

"Key" = a `KeyMapping` of section 2. "Slot" = a skill id that is a separate entry in the player's skill list and fills a Tensura slot on its own. Mode counts are from the `getModes` overrides (a class with no override = 1).

| Franchise | System | Own keys | Own-slot skill ids | Modes today | Note |
|---|---|---|---|---|---|
| Dragon Ball | Saiyan (race flag, forms, techniques) | **23** | 0 (no ManasSkill; race flag + 35 server ops, saiyan/SaiyanNet.java:25, SaiyanNet2.java:11) | n/a | HUD `saiyan_hud` exists |
| Dragon Ball | `ki` (KiSkill, Ultimate) | 0 | 1 | 10: blast, kame, teleport, flare, afterimage, ssj, spirit, kaioken, ui, sense | **Overlaps the Saiyan moves** (blast, kame, transmit, kaioken / ui forms, senses); admin grant only |
| Dragon Ball | 15 character pairs `db_*` | 0 | 30 ids, **off by default** | 3-6 | `dragonBallLegacySkills=false` (NUConfig.java:283); the `db_goku` Unique is also named "Saiyan Prodigy" (`docs/dragonball_skills.md`), a **display-name collision** with the new hub |
| Dragon Ball | Faith of Super Saiyan God | 0 | 2 (pair) | 3-4 | in the Faith pairs below |
| Beast Wills | 27 wills (will item, forms, abilities) | **5** | 54 (27 Unique + 27 Ultimate, `bw_*`) | wheel 2-5 per skill | Two parallel systems per beast: the will's own 2 abilities on keys, and the `bw_*` skill with its own wheel (`docs/beast_skills.md`) |
| Shield Hero | 22 hero weapons (`HeroKind`) | **7** (3 technique, summon, form, curse, book) | 0 | 3 techniques per weapon | keys shared by all 22 weapons |
| Black Clover | 106 grimoire books + 105 legacy `grimoire_<type>` + `grimoire_magic` + 5 time pages | 1 (G summon) + 2 unbound | 106 live + 111 legacy | pages = modes (book/GrimoireBook.java:228) | already consolidated; a player normally holds one family |
| Black Clover | Devil contract | **1** (N) | `forbidden_magic` book | | assimilation toggle only |
| Black Clover | `wizard_king`, `spirit_lord`, `anti_magic_spirit_lord`, Faith of Lucifero | 0 | 4 | 5 / 3 / n / 3 | |
| Tensura (Faith) | `faith`, `goddess_of_faith`, 8 god pairs | 0 | 18 | 5 / 7 / 2-6 | already modes |
| DanMachi | `summoner` / `lord_of_summons` | 0 | 2 | 9 (rework) / 17 (canon) | already merged Hawk Eye + Spirit Card (SummonerMigration) |
| DanMachi | `argonaut` / `heros_argonaut` | 0 | 2 | 4 / 5 | already merged Firebolt (ArgonautMerge); `firebolt` stays registered |
| DanMachi | single-mode skills: `liaris_freese`, `airiel` (2), `hestia`, `freya`, `hephaestus`, `loki` (GodSkill, 1 press each), `hati`, `unbreakable_will`, 5 chanted magics (`wynn_fimbulvetr`, `rea_laevateinn`, `arcs_ray`, `futsunomitama`, `uchide_no_kozuchi`, 1 mode each), `immortal_will` (2) | 0 | **15** | 1-2 | the true "one skill per slot" group; a player holds few of them (god-tied) |
| Fire Force | `hell_walker`, `hell_conqueror`, `adolla_burst`, `devils_footprints` | 0 | 4 | wheel (Shinra rework) / 2 | already wheels |
| Medaka Box | `book_maker`, `all_fiction` | 0 | 2 | 2-18 | already wheels |
| Hunter x Hunter | `nen` | 0 | 1 | 17 | |
| Fate | `gate_of_babylon` | 0 | 1 | 10 | |
| Magi | 36 Djinn (vessels) | **3** | 0 | 4 actions (up, down, off, Rukh: relics/djinn/DjinnNet.java:18) | relic vessels, not skills |
| Magi | Veldanava Ultimates `sandalphon`, `israfil`, `jibril`, `solomon`, `hercules` | 0 | 5 | about 10 each | admin / event grant |
| Berserk | Black Sword Greed | **1** (R) | `lord_of_gluttonous_greed` (1) + Faith of Femto | 3 | form switching is on the key, the 3 modes are on the skill |
| Relics / Infinity Stones / wave relics | relic engine | **2** (R Gambler, V ring) + 2 unbound | 0 | ring of every worn act | Infinity Stone slot abilities run through the relic engine (stones/StoneActs.java:3-7): no key of their own |
| Weapons (Genesis Demon-Slayer, reference weapons) | item `use()` | 0 | 0 | right-click | no key (item/DemonSlayerSwordItem.java:289) |
| Song Magic | `book_song` family + hold-to-sing | **1** (V) | 1 book | pages | the key only replaces a microphone |
| Shishou | `shishou` Ultimate | 0 | 1 | 7 | own HUD `shishou_hud` |
| Misc | `adaptability`, `the_almighty`, `the_visionary` | 0 | 3 | toggle / 3 / 3 | |
| System | System window, floor card, quest tracker, GUI Move / Size | **8** | 0 | | UI only |

Own-slot skill ids that are neither a book, a legacy stub nor a Dragon Ball legacy id: 15 single-mode + 18 Faith + 54 Beast + 5 Veldanava + 4 Black Clover specials + 4 Fire Force + 2 Medaka + 3 Nen / Gate / Ki + 4 DanMachi (Summoner, Argonaut pairs) + 4 Berserk / Shishou / Greed / Faith of Lucifero + 3 misc = about 116-120.

## 5. Proposal per group

Common shape (the "hub"): one **intrinsic** skill (registered once, appended to `NUSkills`, never removed) whose modes are the group's actions; the player puts the modes they use into the 3 Tensura slots (and presets). The server keeps every rule it has today (rate limit `SaiyanNet.MIN_GAP_TICKS`, validation, `SaiyanState` cooldowns); the skill's `onPressed` / `onHeld` / `onRelease` call the same server entry points the packets call now. **No new packet types**, and the old per-key packets become unused (kept registered). The old key classes stay compiled; with the setting "hub keys" on (client config, default on once the hub exists) they are simply not passed to `e.register(...)`, so they leave the Controls list while a saved binding line in `options.txt` stays harmless. Setting off = today's behaviour (rule 12).

What the hub helper must offer so the groups below can reuse it (ask for this from the Saiyan worker):

1. a mode table (id, lang key, kind: PRESS / HOLD_RELEASE / TOGGLE / SCROLL_PAIR, cost, cooldown ticks, visible-if predicate) that drives `getModes`, `nextMode` (skipping hidden modes like `SummonerSkill.nextMode`), `getModeId`, `getModeName` (adds the cooldown, as `DbSkill.getModeName` does);
2. `onHeld` / `onRelease` forwarding for HOLD_RELEASE kinds, with the "release is always delivered" guarantee the Kamehameha has today (client/saiyan/SaiyanClient.java:199-210);
3. `onScroll` for "next / previous" pairs (forms, wills) so those two keys also disappear;
4. one server-side cooldown truth (the group's own state), mirrored to `instance.setCoolDown(seconds, mode)` for Tensura's clock only;
5. auto grant / forget bound to the group's own state (race flag, weapon bond, held will), with the `SummonerMigration` carry-over style for any old skill.

### 5.1 Saiyan (in progress)

| Item | Detail |
|---|---|
| Hub | `saiyan_prodigy`-style intrinsic skill (the other worker owns it) |
| Modes (33 distinct actions, ops 1-35 minus the 2 release ops) | **Combat:** Ki Blast, Rapid Barrage, Rock Scissors Paper, Kamehameha (hold), Spirit Bomb (hold), Ki Wave (hold), Meteor Combination, Dragon Fist. **Movement:** Ki Flight (toggle), Burst Dash, Instant Transmission. **Defence / control:** Ki Barrier, Ki Deflection, God Bind, Ki Suppression (toggle). **Sense / support:** Senses (toggle), Energy Transfer. **Forms:** Form on/off, next / previous (scroll), Branch. **Technique cursor:** the 11 `CYCLE` techniques (Strike, Vanish, Rush, Counter, Breaker, Instinct, Sphere, Reinforce, Avatar, Afterimage, Gravity) as modes or as one "Technique" mode that uses the current one |
| Keys saved | 23 (all) |
| Lost | one-press access to the 23 moves at once (only 3 slots per preset); Blast and Barrage spam needs a slot; the technique cursor key pair becomes plain mode selection |
| Risks | the list is long: group it in the picker (Tensura's mode screen scrolls but does not group); `KiSkill` (`ki`) duplicates 8 of these moves, so the owner must decide: leave `ki` as is (it is admin-grant only and has its own costs) or point it at the same server code; the name "Saiyan Prodigy" already belongs to the `db_goku` Unique (hidden by default); Left Alt / G / bracket collisions vanish |
| Save migration | none: no Saiyan skill exists today, the race flag is the state; grant the hub when the flag turns on, forget it when it turns off |

### 5.2 Hero weapons

| Item | Detail |
|---|---|
| Hub | `hero_weapon` skill, granted when a weapon is bonded, forgotten when the last bond ends (heroweapon/bond/HeroBonds) |
| Modes | Technique 1, Technique 2, Technique 3 (HOLD_RELEASE; the 10-tick heartbeat of `HeroKeysClient` is replaced by `onHeld`), Summon / put away, Change form (or SCROLL_PAIR). Names read the bonded weapon's technique names at runtime, like `HellWalkerSkill.getModeName` (skill/HellWalkerSkill.java:109) |
| Keys saved | 5 (Z, X, C, B, N) ; the Curse key `O` (sneak-hold 3 s) can become a hold on a sixth mode "Embrace" (+1); the Weapon Book `M` stays (it opens a screen) |
| Lost | direct Z / X / C when a weapon is out; no more heartbeat packets |
| Risks | the server dispatcher is `heroweapon/skill/HeroKeys.java` (it already rate limits and rejects unbound players): call it from `onPressed` instead of from the packet; hold state must survive the skill being swapped out mid-hold (the release path of `HeroKeysClient.tick` guards this today, :70); the curse reads the held state of technique 3 (`KeyHold`, `HeroKeys.java:11-13`); it fixes the Tensura slot-key collision that today shadows or doubles Z / X / C |
| Save migration | none: new skill id; bonds are not changed |

### 5.3 Beast Will

| Item | Detail |
|---|---|
| Hub | `beast_will` skill, granted with the first will, forgotten with the last |
| Modes | Ability 1, Ability 2 (names read the active will, like `Summoner` card names), Acquire form (toggle), Integrate form (toggle), Swap will (SCROLL_PAIR) |
| Keys saved | 5 |
| Lost | acquire / integrate / abilities on dedicated keys; swap becomes mode scroll or a mode press |
| Risks | the 54 `bw_*` skills are a second wheel for the same beast: **do not merge them in this step** (their Unique -> Ultimate mastery is per instance, `docs/beast_skills.md`); cooldown clocks are the will's own and counted down locally by the client (BeastWillClient.java:124-130), so the hub mirrors, never owns; `BeastWillClient` panel `beastwill_hud` already shows the abilities and the cooldowns |
| Save migration | none for the hub; the 54 skills stay as they are |

### 5.4 Relic ring takes Djinn and the Gambler's page

| Item | Detail |
|---|---|
| Hub | the existing ring on `V` (client/relics/RelicActKeys.java:3-8), no new skill |
| Modes | Djinn: Raise phase, Lower phase, Dismiss, Draw Rukh as four acts of the bonded vessel; Gambler's page as an act of the worn page |
| Keys saved | 4 (J, K, H, R) |
| Lost | direct phase stepping; "press again to stop" for Rukh becomes a second press of the ring |
| Risks | the Djinn has its own engine and its own packet (`DjinnNet.Act`, relics/djinn/DjinnEngine.java:267); the ring's `Act` object must call it without a second server path; sneak + J (dismiss) maps to Shift in the ring, which today means "next entry" (conflict: needs an explicit dismiss entry); the Djinn HUD `djinn_hud` stays |
| Save migration | none |

### 5.5 Small fold-ins (no new skill)

| Key | Fold into | Why it fits | Saved |
|---|---|---|---|
| Greed `R` (next / previous form) | extra modes or a SCROLL_PAIR on `lord_of_gluttonous_greed` (skill/LordOfGluttonousGreedSkill.java:25; its modes today are the technique, half-starvation, offer) | the skill is the Black Sword's skill | 1 (also removes the R collision) |
| Devil `N` (assimilation on / off) | a toggle mode on `forbidden_magic` (the devil-contract book, `NUSkills.BOOK_FORBIDDEN`) or a mode of the Saiyan-style hub for devils | one on/off server call (`DevilNet.Toggle`) | 1 (removes the N collision) |
| Song `V` (sing) | the song book's page `Sing` (hold) | only used when Simple Voice Chat is absent (client/song/SongClient.java:11-13) | 1 (removes the V collision) |
| Curse `O` | mode 6 of the hero hub (5.2) | | 1 |

### 5.6 UI keys (no skill involved)

| Merge | Result | Saved |
|---|---|---|
| GUI Size (F8) into GUI Move (F9) | one "Layout" key opens edit mode; the size screen is a button there (both screens are in client/ui) | 1 |
| System `U` (edit) and `I` (points) | buttons in the System window opened by `Y` | 2 |
| Floor card `.` | a button / auto show inside the System window | 1 |
| Quest tracker `'` | a toggle in the System window | 1 |
| unbound ones (Multiverse status, Grimoire slot, Grimoire shelf, relic visuals x2, Saiyan branch) | nothing to do for play; they only add rows to the Controls list (6 rows); they can go inside the screens that already have buttons | (6 rows, 0 bound keys) |

### 5.7 Skill-slot consolidation (list size, not keys)

| Group | Proposal | Saved ids | Risk |
|---|---|---|---|
| 5 DanMachi chanted magics (`ChantedMagicSkill`, 1 mode each) | one "Chanted Magic" hub with 5 modes | 5 -> 1 | medium: each has its own mastery and cost; use the `SummonerMigration` carry-over; ids stay registered |
| 4 DanMachi gods (`GodSkill`) | one "Divine Blessing" with 4 modes (a player holds one god) | 4 -> 1 | low value, a player rarely holds more than one |
| `liaris_freese`, `airiel`, `hati`, `unbreakable_will`, `immortal_will` | modes of `argonaut` / `summoner` (as Firebolt and Hawk Eye already were) | up to 5 | each needs the `ArgonautMerge`-style setting |
| 54 `bw_*` Beast skills | later: one "Beast" skill, modes = the active will's moves; needs per-beast mastery (`ISubAbilityModeHolder.getModeMastery`) | 27 live -> 1 | **high**: mastery, evolution and grant-on-mastery are per beast |
| 5 Veldanava Ultimates, Faith pairs, Nen, Gate, Ki | leave: each is already a wheel, owner-granted |  | |

## 6. Cross-cutting risks

* **Save migration.** Skill ids are only ever appended (rule 2); hubs are new ids. The hub is granted from the group's own state at login and on the state change, the way `SummonerMigration.check` runs (a registry look-up per player every 5 s, no tick work). Old per-move skills (Dragon Ball ids, Beast `bw_*`) are not touched in steps 1-3. Players' saved key bindings stay in `options.txt` and do nothing once the key is not registered.
* **Skill ownership.** The groups that are a *state* (Saiyan flag, weapon bond, held will, devil contract) must grant and forget the hub with the state, on death, respawn, dimension change and logout; skills normally survive death (not verified for Tensura 2.0.1.0), so the forget must be explicit. A hub that is forgotten while a hold is active must release first.
* **Cooldown sharing.** ManasCore cooldowns are whole seconds per mode (`NUSkill.cooldown`, skill/NUSkill.java:89); the groups keep ticks in their own state (Saiyan has up to 16, `docs/saiyan.md:64`). Keep the group state as the truth, mirror to `setCoolDown` for the Tensura clock. The server rate limit stays (4 ticks Saiyan, 3 ticks Beast Will, 4 ticks Djinn).
* **Tooltips and names.** Every mode needs `anime_multiverse.skill.mode.<id>` (see `SkillMsg.mode`, `DbSkill.getModeName`); the skill description lists the modes (`DbSkill.describe`). Add the keys through `tools/lang`, then refresh the lang merge.
* **HUD readout.** Today nothing in `client/` reads a skill's mode (`grep getModeName client/` is empty). See section 8.
* **Collisions inside the consolidation.** Two hubs both want Tensura's slot cells: the player has 3 per preset. A Saiyan who is also a hero weapon bearer needs presets; document the preset switch.
* **Rules 9, 11, 12.** The hubs only call server code that exists; the packets drop; no new world scan or per-tick work. Each step needs `SelfTests` prefixes (`hub.modes`, `hub.grant`) in the smoke list and a source-scan test that no hub class imports `net.minecraft.client`. Client config for hiding the legacy keys is a CLIENT config (looks only).
* **Rule 2 vs the owner's request.** The owner asks to reduce keys, which is an explicit replacement; still keep the key classes, the packets and the ids, behind a switch that restores them.

## 7. Ranking and order of work

By keybind savings (descending) with risk:

| Rank | Consolidation | Keys saved | Risk | Needs the helper |
|---|---|---|---|---|
| 1 | Saiyan hub | 23 | medium (33 modes, `ki` overlap, long list) | builds it |
| 2 | Hero weapon hub (+ Curse key) | 5 (+1) | medium (grant / forget on bond, hold state) | yes |
| 3 | Beast Will hub | 5 | medium (per-will names, mirrored cooldowns) | yes |
| 4 | UI key merge | 5 | low (screens only) | no |
| 5 | Relic ring takes Djinn + Gambler | 4 | medium (Djinn engine is separate; Shift is taken) | no |
| 6 | Small fold-ins Greed, Devil, Song | 3 | low | partly |
| 7 | DanMachi single-mode merge | 0 keys, up to 14 ids | medium (mastery carry) | yes |
| 8 | Beast `bw_*` merge | 0 keys, 26 ids | high | yes |

By risk (lowest first): 4 UI, 6 fold-ins, 2, 3, 5, 1, 7, 8.

Recommended order in 4 steps:

1. **Hub helper + Saiyan + mode readout** (-23 keys, 56 -> 33). The Saiyan worker builds the helper; add the readout of section 8 and the client switch that hides the legacy keys. This is the biggest complaint (41 % of all keys) and it proves the helper before anything else uses it. Also state the decision about `ki`.
2. **Hero weapon hub + Beast Will hub** (-11 with the Curse key, 33 -> 22). Both have server dispatchers that already validate; both are shared by many items / wills so one hub serves them all; removes the Tensura Z / X / C / B / N collisions.
3. **The no-new-skill sweep** (-12, 22 -> 10): UI merge (-5), Greed / Devil / Song fold-ins (-3), relic ring takes Djinn and the Gambler's page (-4). Independent items, can be done in parallel; the relic ring one is the riskiest.
4. **Skill-list consolidation and cleanup**: DanMachi chanted magics and gods into hubs with the `SummonerMigration` carry-over, the optional Beast `bw_*` merge, remove stale doc claims about free keys (`docs/heroweapon/core.md:101`, `DjinnKeys.java:6-9`), refresh `docs/saiyan.md` (keys section), and re-run the collision scan (`python tools/audit/keybind_inventory.py`).

Key count after each step: start 56; step 1 -> 33; step 2 -> 22 (hero 5 + Beast Will 5 + Curse 1); step 3 -> 10 (UI -5, Greed / Devil / Song -3, relic ring -4 = -12); step 4 -> 10 (list size only). The ten left: F9 Layout, Y System, M Weapon Book, G Grimoire summon, V relic ring, plus five unbound (Multiverse status, Grimoire slot, Grimoire shelf, relic visuals, show worn relics).

## 8. Client UX: mode HUD or wheel

What exists:

* **No** mode HUD, wheel or radial in this mod for Tensura skills.
* Tensura has its own `SubAbilityModeSelectionScreen`, `AbilitySelectionScreen`, `AbilityCategoriesScreen` and `HudConfig$Abilities` (an ability overlay); the mod only re-themes the screens (client/ui/theme/TensuraScreenTheme.java:24). I could not verify what Tensura's own ability overlay shows for a slot's mode.
* Per-system HUD layers exist and are registered with GUI Move / GUI Size: `saiyan_hud` (client/ui/move/GuiMoveRegistry.java:136), `beastwill_hud` (:134), `hero_hud`, `greed_hud`, `djinn_hud`, `shishou_hud`, devil HUD, `song_meter`, and the closest thing to a mode readout: the relic ring line `relic_keys` (client/relics/RelicActKeys.java:106-129: one string, bottom right, selection + cooldown + "(n/m, shift: next)", a grade-coloured chip, `GuiMove.report("relic_keys", ...)`).

Smallest proposal: **one compact line near the hotbar** (a "mode readout") for the hub skills only.

* New client class `client/hub/HubModeHud.java` (about 80 lines), one render layer `hub_mode` registered like `relic_keys` (an entry in `GuiScaleRegistry` and `GuiMoveRegistry`, anchor bottom-centre above the hotbar), drawn with the existing `UiPanel.panel` chip so it matches the theme.
* Content: for each of the 3 active-preset slots whose skill is a hub (a marker interface on the hub base class): `[Z] Kamehameha  (4s)` in the grade colour, bright when ready, grey with the seconds when not; held-charge modes show `charging 62 %` like `RelicActKeys` does (:117-120). Nothing for slots that hold ordinary skills.
* Data: the client already reads `SkillAPI.getSkillsFrom(player)` (client/ui/codex/SpellSource.java:61, client/ui/CodexPages.java:117) and Tensura's `AbilityStorage` is a ManasCore storage (readable on the client; **not verified that it is synced**). Fallback if it is not: the server sends one tiny sync packet on slot change (3 bytes: skill index + mode per slot), to the owner only.
* Cost (rule 11): 0 packets (or 1 per slot change), at most 3 strings per frame, cached; text rebuilt when the slot or the mode changes and twice a second for the cooldown; no allocation per frame; skipped when `hideGui` or no hub slot is set.
* Also useful for free: the mode name in the existing Tensura mode picker (`getModeName`); a one-line "mode changed" action-bar message when the mode cycles (the Saiyan tech cursor already does this, client/saiyan/SaiyanClient.java:229-233).

A radial wheel is a bigger build (a screen, mouse capture, controller handling) and is **not** needed once 3 slots + presets carry the loadout; do it only if the owner finds 3 slots too few after step 1.

## 9. Could not verify / open decisions for the owner

* Not seen in a running game or built: all of section 5 is a design; hold semantics, the slot picker for a newly granted hub, the Tensura ability overlay, the collision behaviour.
* Whether `AbilityStorage` is synced to the client (needed for the readout without a packet).
* Whether Tensura's NEXT_ABILITY_MODE key cycles the mode **stored in the slot** or the skill's own current mode; the Saiyan hub should be tested for that first.
* Decisions: (a) keep `ki` separate or point it at the Saiyan server code; (b) Beast `bw_*` skills: leave or merge later; (c) the display name "Saiyan Prodigy" is already used by the hidden `db_goku` Unique; (d) whether the six unbound keys should also leave the Controls list; (e) how many Tensura slots the owner considers enough before a radial wheel is built.
