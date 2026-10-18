# Hero weapons: forms, absorption, mastery and the Weapon Book (1.62.0, builder `hforms`)

This is the system the owner described as "weapons absorb raw materials, monster parts or items into a central gem to unlock new specialised forms, stats and skills".
It is the data (`data/nusmp/hero/forms/<itemId>.json`), the validator, the loader, the absorption, the learning of forms, the mastery, the stats that a form puts on the
wielder, the packets and the screen (the Weapon Book), the config, the commands and the self-tests. The contract it plugs into is `docs/heroweapon/design.md` (sections 3,
4, 6, 12 to 15); the rules for writing a forms file are in `docs/heroweapon/forms_authoring.md`.

**New or replacement (the owner's rule 5, compare with what exists):** all NEW. Nothing existing was replaced or removed. The only existing things touched are the five stubs the contract left for this
builder (`HeroForms`, `FormsSystem`, `FormsCommands`, `FormMastery`, `FormsClient`), whose public signatures are kept; methods were only added.

## 1. Canon and what is design

Facts below are from search summaries of the fan wiki (the pages themselves could not be opened from the build machine), from memory, or are the design.

* From a summary of https://shield-hero.fandom.com/wiki/Naofumi_Iwatani/Chronology : the Legendary Shield unlocks new shapes by absorbing things Naofumi finds (the loot of the
  Orange Balloon, a whetstone, a pickaxe and rope from a mine, the Chimera's remains). From a summary of https://shield-hero.fandom.com/wiki/Heroes : for every kind of
  creature, plant and material there is a new form of the weapon and a small increase of its power. This is the "absorb into the gem, a form opens" mechanic.
* From a summary of https://shield-hero.fandom.com/wiki/Legendary_Weapons/Power-up_Methods : the Sword's Mastery rises with use of a form, up to a full level of 100, and
  improves its stats. This is the "mastery" of a form; the series applies it per weapon form and the design asks for a permanent bonus on finishing, which is the
  design's reading.
* Also consulted (summaries only): https://shield-hero.fandom.com/wiki/Legendary_Shield , https://shield-hero.fandom.com/wiki/Chimera .
* From memory (and stated in design.md section 6): the weapon keeps what it absorbed, so the counts only accumulate and unlocking a form never spends them.
* DESIGN (not canon): the numbers (mastery 40 to 400 points, a point per hit, two per technique, one per block), the budgets of the stats, the form tree shape and depth,
  the cap of what the gem takes, the technique lock of two seconds, the Weapon Book.

## 2. The files and why each exists

Server and shared, `com.newuniverse.nusmp.heroweapon.forms`:

| class | job |
|---|---|
| `HeroForm` (lead) | the record of a form; unchanged |
| `HeroForms` | the door: `of`, `get`, `active` (cheap, never throw) and the calls the core makes: `setActive`, `cycle`, `refresh`, `absorbOffhand`; adds `trySetActive` (the refusal as a line to show), `forceActive` (for the curse system when an Overrun ends: the same change without the curse lock and the technique lock) and `forgetSynced` (client) |
| `FormsSchema` | PURE validator of design.md section 6; builds the `HeroForm` records; errors skip a form, warnings are for the file as a whole |
| `FormsJson` | PURE: JSON text to plain maps/lists/numbers (Gson only), and the finder of keys written twice |
| `FormTree` | PURE: requirement progress, availability, the status of a form, the cascade of what a meal opens, the order of the Weapon Book's list |
| `AbsorbRules` | PURE: which items the gem wants, how many it takes, the rate limiter |
| `MasteryRules` | PURE: mastery arithmetic, "finishes exactly once" |
| `FormStats` | PURE: which stat goes on the item, which on the player, the operation, the sum of permanent bonuses |
| `FormLocks` | PURE: the two-second lock after a technique |
| `FormText` | PURE: wording of numbers, stats, slots, ids |
| `FormsWire` | PURE: the byte layout of a form and its checksum (the packet codec uses it) |
| `FormsStore` | the immutable snapshots: loaded from the data pack (server), synced from the server (client); cleared when the server stops |
| `FormsLoader` | the `ResourceManagerReloadListener` for `data/nusmp/hero/forms` |
| `FormLoadout` | `refresh`: makes the player's attribute modifiers and the weapon stack match the bond |
| `FormLearning` | learning a form (all four routes), the catch-up, reset |
| `HeroAbsorb` | the gem's mouth: takes items, tells the wielder, fires the hook, opens forms |
| `FormMastery` | `hit` / `skill` / `block`, finishing a form |
| `FormTags` | item-tag membership from the registry (server and client) |
| `CurseLink` | the one place that knows the curse system's saved key (`series`, `offered`) and its embrace |
| `FormsFeedback`, `FormsLang` | chat/action bar/title/sound/effects that never break the caller; every text is a translatable key with its English as fallback |
| `FormsNet`, `FormsSync` | the four packets; who is watching the book, throttled pushes |
| `FormsCommands`, `FormsConfig`, `FormsSystem`, `FormsSelfTests` | `/multiverse hero form ...`, the config, the entry point, the in-game tests |

Client, `com.newuniverse.nusmp.client.heroweapon.forms`: `FormsClient` (key, the answer to the server), `WeaponBookScreen` (the screen), `BookLayout` (PURE rectangles),
`ClientBook` (the last state).

## 3. The data and how it is read

One file per weapon: `data/nusmp/hero/forms/<itemId>.json` (the schema, with an example, is in `forms_authoring.md`). At each data reload `FormsLoader` reads every file,
runs `FormsSchema` on it (the same code the unit test runs) with the registered weapon logics as the skill index, and swaps the result into `FormsStore` in one step.
A bad FORM is logged and skipped together with every form built on it; a bad FILE (not JSON, wrong weapon, no base form, the form list malformed) leaves the weapon with its
synthetic base form; a file that matches no weapon is reported; a weapon with no file has the synthetic base form (`HeroForm.base`). `/multiverse hero form report` shows
what the loader found.

The client cannot read the server's data pack, so the forms of the player's weapon are sent in a packet (`Defs`) at login, at every data reload, when a weapon is bound and
when the book is opened (only when the client's copy differs by checksum). `HeroForms.of` answers, in order, the loaded data, the synced copy, the synthetic base. If a reload removes the form a bond was in, `refresh` puts the bond back on the base form
(what it learned stays recorded, so the form is usable again if the data returns).

## 4. Absorption

* Accepted: an item some form of the weapon asks for, by id or by tag (config `absorbAnything` lets any other item in, except gear that no form asks for: anything with durability, enchanted
  or renamed, because the off hand usually holds a shield or a totem). A hero weapon is never absorbed.
* How many: only as many as the forms still to be learned can use (config `absorbOnlyWhatIsNeeded`, default true: a stack of 64 for a form that needs 4 loses 4). The
  Weapon Book's shift-click takes the whole stack. Absorbed counts are only added to, never consumed by learning (design.md section 6: the weapon keeps what it ate).
* Routes: the off hand + sneak + use (`HeroForms.absorbOffhand`, called by the core's item; returns true only when something was taken, so a stranger in the off hand does not
  stop the sneak technique), the Weapon Book's Feed slots (one packet, the server looks through the inventory), and `/multiverse hero form absorb <player>`.
* What the player sees and hears: an action-bar line, a sound, a swirl into the hand (`MANA_CHARGE` round the wielder and `KOTO_SHATTER` into the gem), then (if `autoLearn`)
  for each form that opened a chat line ("The Legendary Shield takes in Oak Leaves: it becomes Leaf Shield."), a transmutation circle (`ALCHEMY_CIRCLE`) and a sound.
* Rate limit: at most `absorbPerSecondLimit` absorptions per player per second.

## 5. Learning, the active form and the locks

* A form is AVAILABLE when it is not learned, all its parents are learned, every requirement is met by what was absorbed (a tag counts every absorbed item in it) and, for a
  curse form, its sin is in the curse system's `series`. With `autoLearn` (default) every available form is learned after each meal and when the player logs in or the data
  reloads; otherwise the Learn button (or `/multiverse hero form learn`) does it. A curse form is never learned before its series is unlocked.
* `setActive` refuses: an unknown form, a form not learned, a curse form of a sealed series, a change while `HeroCurse.locked`, a change within `techniqueLockTicks` (40) of
  a technique (`FormMastery.skill` starts the lock). It then keeps the hand in step (`refresh`) and tells the weapon's logic (`HeroLogic.onFormChanged`).
* `cycle` goes to the next learned form in tree order, skipping sealed curse forms (and all curse forms when `cycleCurseForms` is off); a lock stops it and says why on the action bar (sent at the end of the tick, so the core's own "no other form" line, printed when `cycle` returns null, does not hide the reason).
* Binding a weapon teaches the base form and calls `onFormChanged(base, base)`; releasing it removes every modifier.

## 6. What a form does to the wielder (`HeroForms.refresh`)

`refresh` computes what should be there from the bond and changes only what differs, so calling it twice, or every tick, is harmless. It is also called by this system on
login, respawn, dimension change and data reload, and once a second for every bonded player (a weapon put into the hand after the last call, or an attribute reset by something
else, is caught within a second).

| what | where | id | when |
|---|---|---|---|
| permanent `abilityBonus` of finished forms, summed over forms | player attribute, PERMANENT modifier | `nusmp:hero_bonus_<key>` | while the bond exists, summoned or not |
| active form `stats` except attack | player attribute, TRANSIENT modifier | `nusmp:hero_form_<key>` | while the weapon is summoned |
| active form `attack_damage`, `attack_speed` | the weapon stack's `ATTRIBUTE_MODIFIERS`, main hand only, beside whatever the core's item carries | `nusmp:hero_form_<key>` | while summoned |
| the look | the stack's `nusmp:hero_look` component | | set from the active form's `look` |

Keys map to attributes: `attack_damage`, `attack_speed`, `armor`, `armor_toughness`, `max_health`, `movement_speed` (a share: ADD_MULTIPLIED_BASE, 0.05 is plus five
percent), `knockback_resistance`, `reach` (`entity_interaction_range`), `luck`. Every other stat is ADD_VALUE.

## 7. Mastery

`FormMastery.hit/skill/block` add the configured points to the ACTIVE form, capped at the form's `mastery`. The step that reaches the cap finishes the form once: the bond
records the bonus as given, `refresh` applies it, the player gets a title (the form's name, "Mastered"), the quest hook, and a little weapon experience through `HeroXp.gain`
(config `masteryExperience` times the tier). Learning a form also pays `learnExperience` times the tier.

## 8. The Weapon Book

Key `M` (`key.nusmp.hero_book`, category "Multiverse of Anime") or `/multiverse hero form book`. The key asks the server; the server answers with the forms (if the client
lacks them) and the state, and the screen opens on that answer (so a player with no weapon gets the server's message instead of an empty screen). While the book is open the
server compares the state with the one it last showed every `bookRefreshTicks` ticks and pushes it when it differs, so the despair and corruption of the curse system, a summon or
a form set by another system show up without that system calling anything here (`FormsSync.markDirty` only forces a push).

Left: the forms as an indented list (depth-first from the base form, a form under its first parent; the Curse Series after a heading), each with a mark: filled cyan with a dot
(in use), filled green (learned), a hollow square with a breathing dot (available), a hollow dim square (locked), a hollow sin-coloured square with a bar (sealed curse form); a
gold dot at the right for a mastered form. Right, for the chosen form: its name, status and tier, the description, "Learn first: ..." when a parent is missing, a note for a sealed
form, the requirements (an item icon, name, progress bar and count; for a tag the icons of its items turn over), the Feed strip (the inventory items that one of the unmet
requirements would take: click for what is needed, shift-click for the whole stack), the stats, the permanent bonus, the techniques (hover for text, cooldown, cost), the passives,
and the mastery bar. Footer: the buttons Use, Learn, and Embrace (only while the weapon whispers its curse); left, the curse state (the series unlocked, the corruption bar, or the
whisper). Keys inside: Up/Down select, PageUp/PageDown scroll, Enter use, M or Esc close. It is drawn in the mod's GUI theme (anime glass; parchment in the classic look); the
rectangles are `BookLayout` and are tested for ten window sizes from 284 by 160 to 1920 by 1080.

Pictures: `python3 tools/heroweapon/preview_forms_book.py OUT_DIR` draws mock-ups of seven states with PIL (a port of the screen's layout, stand-in item icons, a
monospace stand-in font; the Java is the truth).

## 9. Packets (channel version 1, `optional`)

| id | direction | content |
|---|---|---|
| `nusmp:hero_forms_open` | client to server | the weapon id and checksum of the forms the client holds ("" and 0 for none) |
| `nusmp:hero_forms_act` | client to server | an action (0 use, 1 learn, 2 feed, 3 embrace, 4 close), a text argument (form id or item id), a flag (the whole stack) |
| `nusmp:hero_forms_defs` | server to client | the weapon, the checksum, the forms (`FormsWire`) |
| `nusmp:hero_forms_state` | server to client | open flag, weapon, active form, experience, summoned/locked/curse flags, despair, corruption, the offered sin, learned, mastery, mastered, absorbed, the unlocked series |

The server re-checks every action; a refusal is shown on the action bar with its reason.

## 10. Config: `nusmp-heroweapon-forms-server.toml`, section `[forms]`

| key | default | meaning |
|---|---|---|
| `autoLearn` | true | learn a form as soon as it is available |
| `absorbAnything` | false | the gem takes any item (but not gear no form asks for) |
| `absorbOnlyWhatIsNeeded` | true | take only what the forms still to be learned can use |
| `masteryPerHit` / `masteryPerSkill` / `masteryPerBlock` | 1 / 2 / 1 | mastery points |
| `techniqueLockTicks` | 40 | no change of form this long after a technique |
| `absorbPerSecondLimit` | 10 | absorptions per player per second |
| `learnExperience` / `masteryExperience` | 40 / 80 | weapon experience, times the form's tier (at least 1) |
| `cycleCurseForms` | true | the cycle key passes unlocked curse forms |
| `announce` | true | chat lines and titles |
| `effects` | true | sounds and effects |
| `bookRefreshTicks` | 10 | how often (ticks) an open book is compared with the server and refreshed if it differs; also the least gap between two refreshes |

All plain `define` / `defineInRange`, read through `FormsConfig.get` (the default while the spec is not loaded).

## 11. Commands: `/multiverse hero form ...`

`list [weapon]` and `book` (anyone); `report`, `learn <player> <form|all>`, `set <player> <form>`, `absorb <player>`, `reset <player>`, `mastery <player> <form> <n>`
(permission 2). `learn` ignores parents and requirements (to skip the grind when testing); `set` follows the normal rules; `reset` forgets forms, mastery and meals but keeps
the weapon, its experience and the other systems' state.

## 12. Quest hooks (`NusmpEvents.fire(player, key, arg)`; the door takes ONE argument)

| key | argument | when |
|---|---|---|
| `hero.absorb` | the item's registry id | the gem took items |
| `hero.form` | `<weapon itemId>:<form id>` | a form was learned |
| `hero.mastery` | `<weapon itemId>:<form id>` | a form's mastery was filled |

The door counts the plain key too, so a quest for "any form" uses the key alone and one for a particular form uses `key|arg`.

## 13. Tests

JUnit (pure, run with the shim and mutation-checked): `FormsSchemaTest`, `FormsDataTest` (reads every shipped file; passes on an empty folder; its negative control proves the
validator rejects a bad file), `FormTreeTest`, `AbsorbRulesTest`, `MasteryRulesTest`, `FormStatsTest`, `FormLocksTest`, `FormTextTest`, `FormsWireTest` (200 random rounds),
`BookLayoutTest`. In the game: `hero.forms` (the data on the real registries: every shipped file clean, every item and tag exists and no tag is empty, every skill is registered,
a weapon with skills has a file, the packets round-trip on a real buffer) and `hero.absorb` (a stand-in player bonded to a vassal weapon with four forms planted for the test).

## 14. What is NOT verified (nothing here has run in a game)

* Everything in the game: the loader on a real data pack, the attribute modifiers, the item component edit, the packets, the key, the screen. The code is type-checked against the
  API stubs only (the stubs report six `abstract; cannot be instantiated` artifacts for `new FriendlyByteBuf`, `new KeyMapping` and the title packets, which are concrete in the game).
* The screen's drawing was judged only on the PIL mock-up (`preview_forms_book.py`): the real font is proportional and narrower, item icons are the real ones, and the tooltips
  were not drawn.
* Whether the core's item keeps the attribute component edit (the item class is the core builder's), and that the core calls `FormMastery.hit/skill/block` and `HeroForms.refresh`.
* The shape of the curse system's `bond.extra("curse")`: `series` as a list of texts (a list of compounds with `sin` and a comma-separated text are also read), `offered` as a text or
  a compound with `sin`, and `CurseSystem.embrace(ServerPlayer)` (looked up by reflection, with the command `/multiverse hero curse embrace` as the fallback).
* That `OnDatapackSyncEvent` fires for a joining player (the login event covers it either way).
* That `M` is free in every mod the player has (it is free in vanilla and in this mod; the controls screen shows a clash).
* The balance of the numbers (mastery points, rewards) was not played.
