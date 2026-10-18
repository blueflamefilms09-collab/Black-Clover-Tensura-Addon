# Vassal Book (`vassal_book`): the living spellbook

Class VASSAL, style CAST, canon holder Ethnobalt (the Book Hero; the owner names no holder for the Book). Logic `heroweapon/weapon/book/BookLogic`, forms
`data/nusmp/hero/forms/vassal_book.json`.
This page is written in sections; the LOOK section comes first because the art builder (`hart3`) reads it first.

## 1. Look (for the art builder)

Source of the look: the owner's text ("Book"), the wiki's skill names (pages that wrap, cage and explode, feathers, a white library room) and the design below. The
wiki's appearance sections for the Book are mostly empty; colours and shape are DESIGN.

* **Silhouette.** A heavy hardback spellbook held CLOSED like a grimoire: about 0.55 block wide, 0.75 tall and 0.16 thick, the spine toward the wielder's palm and the
  front cover facing out and forward. Its clasp strap is UNDONE and hangs open, a few loose pages stand out of the top edge slightly fanned (the book is alive and
  restless), a red ribbon hangs from the bottom. Class `handitem` of `weapon_classes.json` (largest dimension 0.5 to 0.9 block).
* **Named parts (9 to 14).** `cover_front`, `cover_back`, `spine` (rounded, four raised bands), `page_block` (ivory, inset 0.02 block from the covers, visible on three
  sides, with fine page lines in the texture), `corners` (four gilded corner caps), `frame` (a gilded border line on the front cover), `emblem` (a raised quill-and-feather
  motif under the gem), `gem_mount` (a gold eight-point star around the gem), `gem`, `clasp_strap` (a leather strap with a gold buckle hanging open from the front edge),
  `ribbon` (red silk bookmark), `loose_pages` (three thin sheets standing out of the top, slightly fanned and curled).
* **Palette.** deep teal-blue leather `#1F3B52`, gold `#C9A24A`, pages ivory `#EFE6CF`, ribbon red `#A6232E`, strap dark brown `#3A2A20`, gem white-gold `#FFE9A0`.
* **The gem.** A round cabochon (radius about 0.06 block) in the centre of the front cover, in the gold star mount; white-gold with a warm glow.
* **Glow skin** (additive): the gem (strong), the star mount, the gilded frame line (faint), the loose pages (soft cyan-white, as if lines of text glowed on them), the
  page block's edge (very faint warm).
* **In hand.** The mesh origin at the spine's middle where the hand grips; the book's height along the mesh y axis (the "tip" is the top edge, up-forward on the icon's
  diagonal), the front cover facing -x (forward and out). Sprite (16 x 16, kept as the inventory icon): a small teal book with a gold corner and a pale dot.
* **Mood.** Scholarly and warm; the opposite of the grimoires of the Black Clover books, which this must not be confused with (no clover, no mana-crest).

## 2. Canon facts and where they come from

Only search summaries of the fandom wiki could be read (the pages themselves cannot be opened from the build machine). "Basis" says which part of a claim is a
SUMMARY (read), MEMORY (what I remember of the series, not confirmed) or DESIGN (mine).

| fact | page | basis |
|---|---|---|
| The Vassal Book is one of the eight Vassal Weapons of Glass' World; its current hero is Ethnobalt, who took it after his Vassal Boat was stolen (Book 16 to 17); it suited his nature as a library rabbit better. | https://shield-hero.fandom.com/wiki/Vassal_Book , https://shield-hero.fandom.com/wiki/Ethnobalt | summary |
| Power-up method: 'Revised Edition': scan the rarity tag of a rarer version of a weapon form you already have and upgrade the form to its stats (the rarer, the stronger). | https://shield-hero.fandom.com/wiki/Legendary_Weapons/Power-up_Methods | summary |
| Skills: Paper Splits Rock: Explode (the paper lights up and explodes, pushing back whoever is near), Life Force Style Magic Explosion (pages wrap the target and trap its life energy, so the explosion does extra damage), Libraria (the area becomes a white room from which many magic attacks, birds of lightning and fire, beams and boulders, strike from every angle), Composition Form One: Bird of Flame (the pages gather into a flaming bird), Composition Four: Constriction (the pages form a cage), Continuation: False Ceiling (needles fall from the cage's ceiling). | https://shield-hero.fandom.com/wiki/Vassal_Book , https://shield-hero.fandom.com/wiki/Kyo_Ethnina/Powers_and_Abilities | summary |
| The Book Hero's forms are books: the Pipe Book (tentatively Constriction) and the Animal Needle Book (tentatively False Ceiling). | the Vassal Book page | summary (marked '?' in the wiki) |
| Forbidden Composition: Revelation (feathers that merge into a black beam, some of which heal) is a CURSE-series skill. | the Vassal Book page | summary; the idea of the Feather Volley page is taken from it, WITHOUT the curse |
| The base name 'Beginner's Book', the two chapters, the page deck, the completion and Libraria as its reward. | - | DESIGN |
| NOT built: Forbidden Composition: Revelation and every other curse-series skill. | - | DESIGN (the brief gives the Book no series) |

## 3. How it plays

The Book is read, not swung, and its loop is the **volume**. It holds two chapters, and each chapter is a short deck of pages in a fixed order. **Read Composition** (right click) reads
the next page of the offence chapter: a **Bird of Flame** (a flame down a line), then a **Needle Rain** (a circle of paper on the ground with four volleys), then, with the Feather page, a
**Feather Volley** (a beam). **Read Verse** (sneak and right click) reads the next page of the support chapter: a **Paper Wall** (absorption for you and your friends), a **Healing Verse**
(three pulses of healing) and, with the Binding page, the **Constriction**. The action bar always says what comes next in each chapter and how much of the volume is read, **Turn the
Page** skips a page that does not suit the moment, and when every page of both chapters has been read the volume is COMPLETE: **Libraria**, the white room, may be cast, uses the volume up
and the reading starts again. A page that finds no target is not read and costs nothing. The key technique of each form is its own burst: **Paper Splits Rock** (the base form and the Revised
Edition), the **Constriction** (the Pipe Book) or the **Life Force Burst** (the Animal Needle Book and the Library Tome).

## 4. The forms

| form | tier | parents | requires (absorbed) | mastery | stats while active | skills (slot: technique) | passives |
|---|---|---|---|---|---|---|---|
| **Beginner's Book** (`base`) | 0 | - | nothing | - | attack_damage 2, attack_speed -2.4, luck 0.5 | use: Read Composition; sneak: Read Verse; key1: Turn the Page; key2: Paper Splits Rock; key3: Libraria | `chapters` |
| **Pipe Book** (`pipe_book`) | 1 | `base` | 32 x paper; 16 x leather; 16 x iron_ingot | 100 | attack_damage 2, attack_speed -2.4, armor 1, knockback_resistance 0.1 | use: Read Composition; sneak: Read Verse; key1: Turn the Page; key2: Constriction; key3: Libraria | `chapters`, `deck_cage` |
| **Animal Needle Book** (`animal_needle_book`) | 1 | `base` | 32 x paper; 24 x feather; 32 x iron_nugget | 100 | attack_damage 3, attack_speed -2, movement_speed 0.02 | use: Read Composition; sneak: Read Verse; key1: Turn the Page; key2: Life Force Burst; key3: Libraria | `chapters`, `deck_feathers` |
| **Revised Edition** (`revised_edition`) | 2 | `pipe_book`, `animal_needle_book` | 32 x book; 32 x ink_sac; 16 x glow_ink_sac; 32 x lapis_lazuli | 200 | attack_damage 3, attack_speed -2.2, armor 2, max_health 4, luck 1 | use: Read Composition; sneak: Read Verse; key1: Turn the Page; key2: Paper Splits Rock; key3: Libraria | `chapters`, `deck_cage`, `deck_feathers`, `revised_edition` |
| **Library Tome** (`library_tome`) | 3 | `revised_edition` | 16 x bookshelf; 8 x enchanted_book; 4 x lectern; 8 x tensura:medium_quality_magic_crystal | 320 | attack_damage 4, attack_speed -2.2, armor 3, armor_toughness 1, max_health 8, luck 2 | use: Read Composition; sneak: Read Verse; key1: Turn the Page; key2: Life Force Burst; key3: Libraria | `chapters`, `deck_cage`, `deck_feathers`, `revised_edition`, `grand_library` |

The tree is a diamond with a top: the Pipe Book (the Binding page) and the Animal Needle Book (the Feather page) both lead to the Revised Edition, which holds all six pages and makes them 25
percent stronger, and the Library Tome (tier 3) adds the Grand Library (Libraria in ten volleys; the volume needs all but one page). Tier 1 asks for common things (paper, leather, iron;
paper, feathers, iron nuggets), tier 2 for rarer (books, ink, glow ink, lapis), the top for a real task (sixteen bookshelves, eight enchanted books, lecterns and Tensura magic crystals). The
gem takes only items some form of the Book asks for; absorbed items accumulate and are never consumed by learning a form. Every stat is inside the budget of design.md section 6 and every
ability bonus inside a quarter of it (`BookFormsTest` checks both). Changing to a form with the same pages keeps the volume's progress; changing to one with other pages starts it unread.

## 5. The techniques

| technique (`id`) | kind | rest | cost | raw damage | foes | works for |
|---|---|---|---|---|---|---|
| **Read Composition** (`book.read_composition`) | ordinary | 3 s (60 ticks) | 1.5 % | 11.25 | 8 | 1.5 s |
| **Read Verse** (`book.read_verse`) | utility | 5 s (100 ticks) | 2 % | 7.5 | 1 | 4.5 s |
| **Turn the Page** (`book.turn_page`) | utility | 5 s (100 ticks) | 1 % | none | - | instant |
| **Paper Splits Rock** (`book.paper_burst`) | ordinary | 4 s (80 ticks) | 1.5 % | 11.25 | 8 | 0.4 s |
| **Life Force Burst** (`book.life_force_burst`) | heavy | 7 s (140 ticks) | 2.5 % | 17.5 | 1 | 1.2 s |
| **Constriction** (`book.constriction`) | utility | 8 s (160 ticks) | 2 % | 7.5 | 1 | 1.5 s |
| **Libraria** (`book.libraria`) | ultimate | 40 s (800 ticks) | 7 % | 3.2 x 10 = 32 | 12 | 7 s |

Rests are before the level scaling (the dispatcher shortens them by up to a quarter); damage is raw, before the level factor (1.0 to 2.2) and the balance law, and the table shows the most a
revised page does. A page that finds no target is not read and costs nothing.

The pages themselves (not skills of their own, so they have no lang keys; the action bar names them):

| page | chapter | effect | numbers |
|---|---|---|---|
| Bird of Flame (`bird`) | offence | a flame down a line of 14 blocks | 6 raw to up to 4 foes (7.5 revised) |
| Needle Rain (`needles`) | offence | a circle of 3 blocks at the aimed point; 4 volleys, 5 ticks apart, after a 10 tick tell | 2.5 raw a volley, 10 in all (3 a volley, 12 in all revised) |
| Feather Volley (`feathers`) | offence (with `deck_feathers`) | feathers gather for 6 ticks, then a beam of 12 blocks; a foe hit mends the wielder 2 percent of their health | 9 raw to up to 5 foes (11.25 revised) |
| Paper Wall (`wall`) | support | absorption (2 hearts) for 10 seconds (12.5 revised) on the wielder and friends within 5 blocks | no damage |
| Healing Verse (`verse`) | support | 3 pulses 30 ticks apart on the wielder and friends within 6 blocks, only those hurt | 1.6 plus 2 percent of their health a pulse (25 percent more revised), three pulses never over a quarter of their health |
| Constriction (`cage`) | support (with `deck_cage`) | the cage (see the technique) | 6 raw from the False Ceiling (7.5 revised) |

### Read Composition (`book.read_composition`)

Reads the next page of the offence chapter, in order: a Bird of Flame (6 damage along a line of 14 blocks), a Needle Rain (4 volleys of 2.5 on a circle of 3 blocks) and, with the Feather page, a beam of 9 damage. A page that finds no target is not read.

* **Numbers:** ordinary technique, rests 3 s, costs 1.5 % of the maximum magicules, raw damage 11.25 per foe at most.
* **Tell:** Each page shows its own: the bird's flame lane, the needle circle on the ground (10 ticks before the first volley), the feathers gathering (6 ticks); the action bar names what comes next.
* **Answer:** Step out of the bird's line or the needle circle; the feather beam is a line of 12 blocks, 1 block to each side.
* **Effects:** Bird of Flame: `WIND_GALE` tinted flame and a `FIRE_BURST`; Needle Rain: a dust ring, `SEAL_BASIC` and falling `THREAD_LINE` needles; Feather Volley: a dust gathering, `STAR_MAGIC`, then a `LIGHT_BLADE` beam.
* **Why:** The offence chapter. The pages come in the order written (Bird of Flame, Needle Rain, with the Feather page third); a page that finds no target is NOT read (the deck does not advance, nothing is spent). Revised Edition makes them 25 percent stronger within the ordinary limit.

### Read Verse (`book.read_verse`)

Reads the next page of the support chapter, in order: a Paper Wall (absorption for you and friends within 5 blocks), a Healing Verse (3 pulses, together at most 25 percent of a friend's health) and, with the Binding page, a Constriction.

* **Numbers:** utility technique, rests 5 s, costs 2 % of the maximum magicules, raw damage 7.5 per foe at most.
* **Tell:** A gold ring of paper under the wielder (the wall), a gold rune circle (the verse), a cube of paper round the foe (the cage).
* **Answer:** -
* **Effects:** Paper Wall: `SEAL_BASIC`, `MIRROR_PANE`; Healing Verse: `ELF_CIRCLE` and `SEAL_WOUND` streams to friends; Constriction: `SEAL_CUBE` round the foe.
* **Why:** The support chapter: Paper Wall (absorption for 10 seconds on you and friends within 5 blocks), Healing Verse (three pulses through `HeroCtx.heal`, together never more than a quarter of a friend's health) and, with the Binding page, the Constriction (hard control).

### Turn the Page (`book.turn_page`)

Turns past the next page of the chapter you read last without reading it, so another page comes up. A skipped page is still unread, so the volume is not complete until it is read.

* **Numbers:** utility technique, rests 5 s, costs 1 % of the maximum magicules, no damage.
* **Tell:** The page-turn sound and the action bar line.
* **Answer:** -
* **Effects:** a page-turn sound.
* **Why:** The player's agency in the deck: skip the next page of the chapter read last. A skipped page stays unread, so it must come round again before the volume is complete.

### Paper Splits Rock (`book.paper_burst`)

The page you aim at lights up (a ring shows where) and 8 ticks later explodes: foes within 3.5 blocks take 9 damage and are pushed away. Counter: leave the ring.

* **Numbers:** ordinary technique, rests 4 s, costs 1.5 % of the maximum magicules, raw damage 11.25 per foe at most.
* **Tell:** A ring on the ground at the aimed page and its circle charging for 8 ticks (0.4 s) before the burst.
* **Answer:** Leave the ring (3.5 blocks) during the charge.
* **Effects:** `MAGIC_CIRCLE_EXPLOSION` (the circle charges and bursts), a dust ring, an explosion sound.
* **Why:** Canon Paper Splits Rock: Explode (the page lights up and explodes, pushing foes back). It needs a foe at the aimed point, so it cannot be wasted on empty ground.

### Life Force Burst (`book.life_force_burst`)

Pages wrap the foe on your crosshair; if it stays within 6 blocks of where it was wrapped for 1.2 seconds, the trapped life force bursts for 14 damage. Counter: run before the burst.

* **Numbers:** heavy technique, rests 7 s, costs 2.5 % of the maximum magicules, raw damage 17.5 per foe at most.
* **Tell:** Pages wrap the foe (`THREAD_COCOON`) and a ring marks the spot for 24 ticks (1.2 s).
* **Answer:** Run: if the foe is more than 6 blocks from where it was wrapped when the pages tighten, they unravel and nothing happens.
* **Effects:** `THREAD_COCOON` (the wrapping), then `MAGIC_CIRCLE_EXPLOSION` on the foe, an explosion sound.
* **Why:** Canon Life Force Style Magic Explosion (pages wrap the target and trap its life energy, so the explosion hits harder): the Book's heavy single-target hit, 14 raw (17.5 with Revised Edition, under the heavy limit).

### Constriction (`book.constriction`)

Pages close into a cage round the foe on your crosshair (within 14 blocks): it is rooted for up to 2 seconds, then needles fall through the False Ceiling for 6 damage. It is hard control and shares the 12 second control rest.

* **Numbers:** utility technique, rests 8 s, costs 2 % of the maximum magicules, raw damage 7.5 per foe at most.
* **Tell:** The cube of paper closing round the foe.
* **Answer:** After the root ends (at most 2 seconds, 1 for a boss, 3 at most on a player) the needles fall at 1.5 s; nothing else to dodge.
* **Effects:** `SEAL_CUBE` round the foe, page sounds, falling `THREAD_LINE` needles.
* **Why:** Canon Composition Four: Constriction (a cage of pages) and Continuation: False Ceiling (needles from the cage's ceiling). Hard control: it asks `HeroCtx.control()` (the 12 second lock) only once it knows it will work, and the root length goes through `BalanceLaw.controlTicks`.

### Libraria (`book.libraria`)

Ultimate. Needs a complete volume (every page of both chapters read). The room turns white and in 8 volleys birds of fire, lightning, boulders and beams fall on every foe within 9 blocks for 3.5 damage each (28 in all). Counter: leave the white room.

* **Numbers:** ultimate technique, rests 40 s, costs 7 % of the maximum magicules, raw damage 32 per foe at most.
* **Tell:** The room turns white round the wielder (`SEAL_CUBE`, 18 blocks across) with a dust ring of 9 blocks and a beacon sound for 20 ticks (1 s) before the first volley.
* **Answer:** Leave the white room (9 blocks) during the first second; the volleys come every 12 ticks.
* **Effects:** `SEAL_CUBE` (the white room), then per volley a different show on up to three foes: `WIND_GALE` fire birds, `LIGHTNING_SPEAR`, `STONE_SPIKES`, `LIGHT_BLADE` beams.
* **Why:** Canon Libraria (the area becomes a white room from which birds of fire and lightning, beams and boulders strike from every angle). It needs a COMPLETE volume and uses it up; 8 volleys of 3.5 (28), or 10 of 3.2 (32) with the Grand Library.

## 6. Passives

| passive | what it does | forms |
|---|---|---|
| **Two Chapters** (`chapters`) | The book holds two chapters read in order: use reads the offence chapter, sneak and use the support chapter. Reading every page of both completes the volume and opens Libraria. | all five |
| **Feather Page** (`deck_feathers`) | Adds the Feather Volley to the offence chapter: a beam of 12 blocks for 9 damage that mends you a little. | Animal Needle Book, Revised Edition, Library Tome |
| **Binding Page** (`deck_cage`) | Adds the Constriction to the support chapter. | Pipe Book, Revised Edition, Library Tome |
| **Revised Edition** (`revised_edition`) | Every page and burst is 25 percent stronger, within the limits of its kind. | Revised Edition, Library Tome |
| **Grand Library** (`grand_library`) | Libraria falls in 10 volleys, and the volume needs all but one page. | Library Tome |

## 7. Curse series

None. The brief gives the Book no series (the wiki's Forbidden Composition: Revelation belongs to the curse series and is not built), and the seven sins are taken: the Holy Weapons have the canon
ones, the Mirror (Envy) and the Chain (Sloth) the other two (design.md section 2). The forms file therefore has no `curse` form and only the base look.

## 8. Engravings (for `item/WeaponEngravings`, lead integration)

```java
Map.entry("vassal_book", List.of(e("magic_weapon", 1), e("elemental_boost", 2), e("restoration", 1), e("magic_protection", 1))),   // Ethnobalt's book
```

* `magic_weapon` 1: a spellbook is a magic tool.
* `elemental_boost` 2: the bird of flame, the lightning and the boulders of Libraria.
* `restoration` 1: the Healing Verse.
* `magic_protection` 1: the Paper Wall.

## 9. Numbers and the budget

`BookNumbers` holds every number once. `PageDeck` (the order of the pages), `BookDecks` (which pages a form holds) and `Volume` (two chapters and the completion) are the pure loop. The table is checked
by `BookNumbersTest` against `Budget` (design.md section 5). The edges the tests pin: no offence page, revised or not, passes the ordinary limit of 12 on one foe (the revised needles are held to 3
a volley), the bird even revised stays at 7.5 so it needs no tell, the Life Force Burst revised is 17.5 (heavy limit 20), Libraria is 28 (32 with the Grand Library, exactly the ultimate limit), and
the Healing Verse never passes a quarter of a friend's health at any health. The deck tests show the order, the wrap, that a skipped page stays unread, that an empty deck never throws and that a
volume rebuilds only when the pages change. The texts (`BookText`) are built from the same constants.

## 10. Code map (the WHY of each file)

| file | why |
|---|---|
| `weapon/book/BookLogic` | the part the shared machinery calls: lists the skills, rebuilds the volume on a change of form, the self-test `hero.weapon.book` (which reads a whole volume and casts Libraria), forgetting a wielder |
| `weapon/book/BookSkills` | the seven techniques, one small method each |
| `weapon/book/BookPages` | the six pages, written once for the chapters and the key techniques |
| `weapon/book/BookNumbers` | every number, the budget rows, the pure page rules |
| `weapon/book/PageDeck`, `BookDecks`, `Volume` | the pure loop: one deck, the pages a form holds, the two-chapter volume |
| `weapon/book/BookText` | every name and description (and the six page names), built from the numbers; writes the lang entries |
| `weapon/book/BookState` | the per-wielder volume, bounded, cleared on logout and server stop (kept when the book is put away) |
| `data/nusmp/hero/forms/vassal_book.json` | the five forms |
| `weapon/glassworld/*` | the kit shared with the Fan, the Scythe and the Katana (see `docs/heroweapon/weapon_glassworld.md`) |

Tests: `PageDeckTest`, `BookDecksTest`, `VolumeTest`, `BookNumbersTest`, `BookTextTest`, `BookFormsTest` (and the kit's tests). Self-test in the game: `hero.weapon.book` (with a library round that reads a whole volume and casts Libraria), run with `/multiverse mega selftest hero.weapon.book` (permission 2).
Config keys: none. Commands: none of its own. Language: `hero.skill.<id>.name|desc` for the seven skills and `hero.passive.book.<id>` (+ `.desc`) for the five passives, in `frag/hvc.lang.json`.

## 11. What is unverified

* **Nothing here was run in a game.** The type checker (javac against the API stubs) and the pure unit tests are all that ran; every statement about how a technique
  feels (distances of a dash, the look of an effect, the timing of a tell) is a calculation or a reading of the effect layers' comments, not something seen.
* **The dispatcher and the hooks are the core builder's.** The skills assume what the contract says: a refused cast (returns false) costs nothing and starts no rest,
  `onHit` follows a melee blow of the wielder, `onDamaged` runs before the wielder takes the damage and its return value replaces the amount, `onTick` runs about every
  five ticks while the weapon is summoned. The kit's guard `GlassKit.striking()` keeps a weapon from answering its own techniques' damage if the core's `onHit` also fires
  for them.
* **Effects.** Shapes, colours, durations and sizes were chosen from the javadoc of the effect layers (`vfx/client/layer/*`); the layers were not run. A shape whose
  `power` means something else in play than its comment says would look off, not break.
* **Forms.** Every item id was checked against the vanilla item list and the Tensura id reference, and the three Tensura ids used are the ones other code of the repo
  already uses; the forms system's own self-test `hero.forms` resolves them against the real registries in the game. The counts are a first balance, not tuned.
* **Stats.** `attack_damage` and `attack_speed` are read as the forms builder reads them (modifiers on the item); if it reads them as absolute values the weapons
  would hit far too softly or fast, and the numbers here would need one pass.
* **Damage immunity.** Every technique strike first cuts the foe's vanilla damage immunity (`invulnerableTime`) to ten ticks (`GlassKit.strike`, rule `Immunity`, tested), so a blow lands in full instead of being swallowed by the hit before it, as the older weapons of this mod do. The hits of a quick series (the Needle Rain: four volleys five ticks apart) are marked `rapid`: on a PLAYER they keep the vanilla immunity, so a flurry cannot stack past the balance law's 40 percent cap on players, and such a series lands only some of its hits there (as a vanilla sword swung that fast would). Whether another mod changes the immunity of its entities was not checked.
* **The volume is kept in memory only**: a server restart or a logout starts the book unread (a deliberate small cost; saving it would need the bond's open area, which the Weapon Book system
  also uses under another key).
* **A page that needs a target** (the bird, the needles, the feathers, the cage) is not read when it finds none, which can hold the deck on that page; Turn the Page is the answer, and the
  Healing Verse and the Paper Wall never refuse.
* **Healing** goes through `HeroCtx.heal`, so the balance law decays a second heal on the same target within 20 seconds to 40 percent: the second and third pulse of a Healing Verse do less
  than the first (on a 20 health friend: 2.0, then 0.8 and 0.8, so 3.6 in all).
* **Canon:** the colours and shape of the weapon, and which of the series' skills each form owns, are from summaries and memory only; the wiki marks the books of the skills with '?'.
