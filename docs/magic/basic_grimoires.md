# Basic grimoires: the attribute books of a mage bound to nobody, built on the anime but different from the characters

Owner request: "For the basic magic grimoire they should be based on the anime Black Clover but be different from the Characters' spells." (A basic grimoire is a normal player's grimoire of an attribute: `Canon` empty, not the grimoire of a named character.)

Nothing here was seen in a game. What was checked: the pure rules and the shipped files by JUnit (`charset.BasicGrimoiresTest` and the existing charset / armour policy tests), the sources by scan tests, the type-check against the API stubs. What the CI server smoke test will check in the real game: self-test `magic.basic_grimoires`.

One server switch turns the hiding back: `magic.basicBookHidesSignatures` in `nusmp-character-spells-server.toml` (default `true`; `false` lists everything in the basic books as before, exactly like the character-set pilots; `magic.characterSpellSets=false` turns the whole character-set engine, and with it this, off). The added pages are appended pages: they cannot be switched off without moving mode numbers, so they stay (they only add).

## 1. What was found (the audit) and what was done

Every attribute book was listed from the sources (76 books; the 30 generic wiki attributes of `WikiBooks.attribute` and the rebuilt Earth / Song / Soul families are covered by the exclusivity test only, see 6). Each page of a book was classified:

| Class | Meaning | What happens to it |
|---|---|---|
| (a) generic anime spell | used by ordinary mages or minor named characters who have no character set, or the basic technique of the attribute (the page the set builders left non-exclusive on purpose) | KEPT in the basic book |
| (b) a named character's signature spell that is still visible to the basic book | the owner's own grimoire has it, a basic one must not | HIDDEN from the basic book (the page stays registered; section 3) |
| (c) invented filler | a mod page with no anime spell of that name | KEPT (rule 2: nothing is removed), listed in the tables as "mod's own page" |

Findings, in numbers (see the table of section 5 for every book):

* About 140 pages were already hidden from the basic books by the character sets (`exclusive`). Every one is checked by the test.
* 23 more signature pages were still visible. They are now RESERVED (`charset.BasicSignatures`): Fuegoleon's Crimson Lion King, Noelle's two dragon forms, Jack's Ripper Cut and Rill's Painted Menagerie (both locked to their owner but listed in another attribute's book), William's Yggdrasil and Charlotte's Briar Prison (locked, listed in the Plant book), Finral's Fallen Angel Gate and Door of Fate, Luck's Thunder God's Boots, Gadjah's Pulsaranta, Rill's Souterrain Giant's Strong Arm, Secre's Trinity Seal, Kirsch's Dance of 100 Million Cherry Blossoms, Dante's Body Compression, and the same spells under a second attribute: Gordon's Aufwachen Dachs and Curse-Worker's Neighbor, Zora's Ash Absorbing Formation, Henry's Ultra Giant Bull, Vanica's Decaying World (Curse and Curse-Warding books), Charmy's Glutton's Banquet and Gourmet's Bite (Food and Cotton books), Gauche's Reflect Iris (Eye book).
* 19 generic anime pages were ADDED to six books (Fire 2, Water 3, Wind 6, Dark 1, Ice 4, Beast 3). Every added page is a spell of an ordinary or minor named character who has no character set (section 4).
* The Mermaid Form stays open to every Water mage: the owner's decision in `docs/magic/noelle_forms.md` ("the Mermaid Form and the Valkyrie Dress stay open"); only the two dragon forms (already locked to Noelle) are reserved.

Rule used for single-user attributes (an attribute whose only anime user is a character with a set: Cotton, Thread, Mercury, Mirror, Painting, Dream, Compass, Slash, Song, Sealing, Shadow, World Tree, Recombination, Transmutation ...): the basic book keeps the attribute-level pages the set builders left non-exclusive (they are the wiki's description of the attribute, for example Mirror Magic: Reflect Ray) and loses the owner's named ultimate or signature. Making those books different from their one user would need spells that do not exist in the series; none was invented (section 7).

## 2. How a page is hidden, and who still sees it

| Question | Answer |
|---|---|
| Which pages does the basic grimoire hide? | The pages a character set marks `exclusive` (as before) and the pages in `charset/BasicSignatures`, both only while `basicBookHidesSignatures` is true. Mode 0 of a book is never hidden. |
| Does a hidden page disappear from the game? | No. Its id, name, order, cast and unlock bit stay (rule 2); it is only not shown / not opened in a grimoire bound to nobody. |
| Who still sees a reserved page? | The grimoire of its owner when the owner's set is for that book (`hidden` is false, `listed` is true, `opens` returns null = the old mastery order opens it, the rank gates and free slots still apply). A character without a set sees the whole book as today. A character with a set for that book who is not the owner sees only his own pages (as before). |
| A character whose grimoire is another book (Jack: Slash, Rill: Painting) | The reservation does nothing there (his set is for his own book); the page keeps its old lock line. |
| Pages opening in the basic book | By mastery order as today: page k of n needs k/n of the mastery bar (and its tier's rank). The appended pages are last, so a basic Wind grimoire meets them from about 70 percent mastery; this is the "appended last" rule 2 and the "as today" rule of the brief, and a decision for the owner (section 8). |
| Where it lives | `charset/BasicSignatures` (pure list), three one-line hooks in `CharacterSetRegistry` (`hidden`, `opens`, `listed`). The appended pages: `book/BasicPages` + `book/BasicArts`, hooked by one line in `GrimoireBook.familyAll()` (after every character builder's pages). |

## 3. The reserved pages

| Book | Page | Owner | Why |
|---|---|---|---|
| `book_fire` | `crimson_lion_king` | fuegoleon | Fuegoleon's Crimson Lion King (flame_rebuild.md); no set lists it |
| `book_water` | `sea_dragon_god_form` | noelle | Noelle's Dragon Form (picture 126) |
| `book_water` | `black_dragon_form` | noelle | Noelle's Black Dragon Form (picture 126) |
| `book_sword` | `ripper_cut` | jack | Slash Magic: Ripper Cut is Jack the Ripper's; locked to him, listed in the Sword book |
| `book_creation` | `painted_menagerie` | rill | Painting Magic: Painted Menagerie is Rill's; locked to him, listed in the Creation book |
| `book_plant` | `yggdrasil` | william | World Tree Magic: Yggdrasil is William's; locked to him, listed in the Plant book |
| `book_plant` | `briar_prison` | charlotte | Briar Magic: Briar Prison is Charlotte's; locked to her, listed in the Plant book |
| `book_spatial` | `fallen_angel_gate` | finral | Spatial Magic: Fallen Angel Gate, Finral's portal |
| `book_spatial` | `door_of_fate` | finral | Compound Magic: Door of Fate, Finral with Vanessa |
| `book_lightning` | `thunder_boots` | luck | Lightning Creation Magic: Thunder God's Boots, Luck's |
| `book_lightning` | `pulsaranta` | gadjah | Lightning Magic: Pulsaranta, Gadjah's |
| `book_painting` | `souterrain_giant_arm` | rill | Souterrain Giant's Strong Arm, the arm of Rill's Souterrain Giant |
| `book_sealing` | `trinity_seal` | secre | Trinity Seal Magic, the Seal Magic wiki's spell (Secre's attribute) |
| `book_cherry_blossom` | `dance_of_100_million_cherry_blossoms` | kirsch | Cherry Blossom Magic: Dance of 100 Million Cherry Blossoms, Kirsch's ultimate |
| `book_body` | `body_compression` | dante | Body Magic: Body Compression, Dante's |
| `book_curse` | `aufwachen_dachs` | gordon | Poison Curse Magic: Aufwachen Dachs, Gordon's (also mode 0 of the Poison book) |
| `book_curse` | `curse_workers_neighbor` | gordon | Curse-Worker's Neighbor, Gordon's |
| `book_curse` | `ash_absorbing_formation` | zora | Ash Curse Magic: Ash Absorbing Formation, Zora's |
| `book_curse` | `ultra_giant_bull` | henry | Compound Magic: Ultra Giant Bull, Henry's (The Raging Black Bull) |
| `book_curse_warding` | `decaying_world` | vanica | Curse-Warding Magic: Decaying World, Megicula's and Vanica's |
| `book_food` | `glutton_s_banquet` | charmy | Food Magic: Glutton's Banquet, Charmy's |
| `book_cotton` | `gourmets_bite` | charmy | Gourmet's Bite, Charmy's |
| `book_eye` | `reflect_iris` | gauche | Union Magic: Reflect Iris, Gauche's mirror array |

## 4. The generic pages that were added

All appended at the end of their book, mid tier (12 percent of the mana pool, floor 300, no signature rank), cooldown 10 to 25 seconds, mastery order last of the book, no lock and no owner (any grimoire of that attribute can open them; no character set lists them). The sources are the Black Clover wiki pages found by search (fandom hosts cannot be fetched, section 9) and the repo's wiki snapshot; "origin" says what is known. The effect is this mod's reading of the wiki description; the numbers are this mod's.

| Page | Book | Canon name and user | Origin | What it does | VFX layers | Cooldown |
|---|---|---|---|---|---|---|
| `shining_flame_cross` | Fire | Shining Flame Cross, Theresa Rapual | wiki page; anime/manga not confirmed | a cross of two 6-block lines where you aim: up to 8 foes, 8 damage, burning 3 s | FIRE_BURST + 2 x FLAME_TRAIL | 10 s |
| `combo_eruption` | Fire | Combo Eruption, Dmitri Brint | wiki page; anime/manga not confirmed | 4 fireballs 3 ticks apart, each explodes on contact: up to 8 foes in 2.4 blocks, 6 damage, burning 3 s | FLAME_TRAIL + 4 x FIRE_BURST | 13 s |
| `aqua_curtain` | Water | Aqua Curtain, Rhya | wiki page (Water Magic category) | puts out your fire, 15 s fire resistance + 4 hearts of absorption, turns aside spells within 3.5 blocks for 4 s | WATER_CRADLE (following) + WATER_RING | 20 s |
| `cascade_destroyer` | Water | Cascade Destroyer, Selena Silva | wiki page; Game Rant confirms the water balls | 4 homing water balls 2 ticks apart, 6 damage each | WATER_JAVELIN + 4 x WATER_SPLASH | 13 s |
| `holy_water_assassination_bullet` | Water | Holy Water Assassination Bullet, Solid Silva | wiki page; used on soul corpses in the Magic Wild Dance arc | one piercing bullet, 34 blocks, up to 8 foes, 13 damage (22 to the undead) | WATER_JAVELIN + WATER_BURST | 15 s |
| `slicing_wind_emperor_wintry_wind` | Wind | Slicing Wind Emperor: Wintry Wind, Fanzell Kruger | wiki page | 4 homing wind swords, 6 damage each | 4 x WIND_SLASH | 12 s |
| `slicing_wind_emperor_gale` | Wind | Slicing Wind Emperor: Gale, Fanzell Kruger | wiki page | a 9-block dash through foes, 9 damage | WIND_GALE + WIND_RING | 11 s |
| `slicing_wind_emperor_mountain_retreat` | Wind | Slicing Wind Emperor: Mountain Retreat, Fanzell Kruger | wiki page | 8-block cone, 4 damage, heavy push | WIND_GALE + FX_WIND_GUST + WIND_RING | 13 s |
| `slicing_wind_emperor_whirlwind` | Wind | Slicing Wind Emperor: Whirlwind, Fanzell Kruger | wiki page (Wind Restraining Magic) | holds one foe within 14 blocks in the air; shares the 12 s control lock | WIND_TORNADO + WIND_RING | 20 s |
| `killing_kakara_blade` | Wind | Killing Kakara Blade, Jozou Hanegatsuji | wiki page; anime/manga not confirmed | a 10-block dash through foes, 8 damage | WIND_SLASH + WIND_GALE + WIND_RING | 11 s |
| `piercing_tornado_needle` | Wind | Piercing Tornado Needle, George (Eye of the Midnight Sun) | wiki page | a 20-block piercing line, 10 damage, a small tornado at its end | WIND_GALE + WIND_TORNADO | 12 s |
| `black_star` | Dark | Black Star, Ichika Yami | wiki page (Ichika Yami); whether the manga names it was not confirmed | a black sphere where you aim pulls up to 8 foes in for 1 s, 5.5 damage | DARK_BLACK_HOLE + DARKFX_BURST | 13 s |
| `heavenly_ice_fang` | Ice | Heavenly Ice Fang, Heath Graice | wiki page + repo snapshot | one foe within 24 blocks: 11 damage, frost, lift | ICE_FX1 + ICE_FX3 | 12 s |
| `icicle_bridge` | Ice | Icicle Bridge | repo snapshot (user not confirmed) | a 16-block line of jagged ice, 7 damage, slowness, frost | ICE_FX1 + ICE_FX3 | 13 s |
| `dissolve` | Ice | Dissolve, Heath Graice | wiki page + repo snapshot | melts the frost on you and up to 8 allies within 6 blocks, stops burning, undoes up to 6 spells / projectiles | ICE_FX3 + WATER_RING | 16 s |
| `crystal_ice_shield` | Ice | Crystal Ice Shield, Heath Graice | wiki page (name only); effect is the shield the name says | 10 s of absorption (6 hearts) and a little resistance | ICE_FX2 (following) + ICE_FX3 | 25 s |
| `bear_claw` | Beast | Bear Claw, Vetto | wiki page + repo snapshot | 3.6-block cone, 13 damage, push | BEAST_FX1 + BEAST_FX3 | 10 s |
| `cheetah_charge` | Beast | Cheetah Charge, Vetto | wiki page + repo snapshot | an 11-block dash through foes, 8 damage, 3 s of speed | BEAST_FX2 + BEAST_FX1 | 13 s |
| `rhinoceros_armor` | Beast | Rhinoceros Armor, Vetto | wiki page + repo snapshot | 12 s of absorption (6 hearts) and a little resistance | BEAST_FX3 (following) + BEAST_FX1 | 25 s |

The VFX is deliberately one layer short of the character tier (2 to 3 layers against 3 to 5): a basic page is decent, a character's signature is richer. No new shape, texture, model or sound was needed; the Time Magic VFX and the Time book are not touched.

## 5. Every book, page by page (generated from the sources)

"Shown to a basic grimoire" counts the pages a grimoire bound to nobody lists after this change, mode 0 included. "Basis" says why a kept page is kept.

| Book | Family pages | Shown to a basic grimoire | Pages added |
|---|---|---|---|
| `book_time` | 12 | 10 | - |
| `book_fire` | 24 | 6 | 2 |
| `book_water` | 17 | 10 | 3 |
| `book_wind` | 21 | 9 | 6 |
| `book_earth` | 9 | 9 | - |
| `book_light` | 15 | 8 | - |
| `book_dark` | 15 | 5 | 1 |
| `book_spatial` | 14 | 8 | - |
| `book_lightning` | 16 | 7 | - |
| `book_steel` | 9 | 5 | - |
| `book_mirror` | 11 | 6 | - |
| `book_thread` | 9 | 4 | - |
| `book_plant` | 14 | 8 | - |
| `book_sealing` | 7 | 5 | - |
| `book_gravity` | 5 | 4 | - |
| `book_ice` | 9 | 9 | 4 |
| `book_sword` | 6 | 5 | - |
| `book_explosion` | 4 | 4 | - |
| `book_magma` | 5 | 5 | - |
| `book_mist` | 4 | 4 | - |
| `book_star` | 6 | 6 | - |
| `book_storm` | 6 | 4 | - |
| `book_sand` | 5 | 5 | - |
| `book_shadow` | 13 | 5 | - |
| `book_poison` | 7 | 4 | - |
| `book_reinforcement` | 5 | 5 | - |
| `book_beast` | 8 | 8 | 3 |
| `book_bone` | 5 | 4 | - |
| `book_blood` | 7 | 5 | - |
| `book_creation` | 4 | 3 | - |
| `book_copy` | 4 | 4 | - |
| `book_illusion` | 5 | 5 | - |
| `book_dream` | 5 | 4 | - |
| `book_transmutation` | 1 | 1 | - |
| `book_ash` | 9 | 5 | - |
| `book_cotton` | 7 | 4 | - |
| `book_recombination` | 2 | 1 | - |
| `book_painting` | 17 | 5 | - |
| `book_world_tree` | 7 | 1 | - |
| `book_dice` | 4 | 4 | - |
| `book_slash` | 4 | 3 | - |
| `book_compass` | 4 | 3 | - |
| `book_game` | 6 | 6 | - |
| `book_demon_beast` | 12 | 12 | - |
| `book_body` | 11 | 10 | - |
| `book_eye` | 12 | 11 | - |
| `book_eyeball` | 12 | 12 | - |
| `book_curse` | 12 | 8 | - |
| `book_curse_warding` | 12 | 11 | - |
| `book_demon_fire` | 12 | 12 | - |
| `book_demon_ice` | 12 | 12 | - |
| `book_demon_light` | 12 | 12 | - |
| `book_demon_water` | 10 | 10 | - |
| `book_barrier` | 12 | 12 | - |
| `book_key` | 14 | 14 | - |
| `book_chain` | 12 | 12 | - |
| `book_butoh` | 13 | 9 | - |
| `book_briar` | 13 | 9 | - |
| `book_cherry_blossom` | 12 | 9 | - |
| `book_fungus` | 12 | 12 | - |
| `book_food` | 12 | 11 | - |
| `book_crystal` | 12 | 12 | - |
| `book_corundum` | 13 | 13 | - |
| `book_bronze` | 14 | 14 | - |
| `book_copper` | 12 | 12 | - |
| `book_iron` | 12 | 12 | - |
| `book_black_oil` | 12 | 12 | - |
| `book_gel` | 12 | 12 | - |
| `book_glass` | 12 | 12 | - |
| `book_bubble` | 12 | 12 | - |
| `book_ice_wedge` | 12 | 12 | - |
| `book_legion` | 11 | 11 | - |
| `forbidden_magic` | 8 | 8 | - |

### `book_time` (12 family pages, 10 shown to a basic grimoire)

| Page | Name | Tier | Basis | Status |
|---|---|---|---|---|
| `chrono_stasis` | Chrono Stasis | zone | attribute basic (also in the set of julius, not exclusive) | kept (mode 0, never hidden) |
| `grigora` | Chrono Stasis Grigora | signature | character signature | hidden: exclusive to julius (set) |
| `acceleration` | Time Acceleration | mid | attribute basic (also in the set of julius, not exclusive) | kept |
| `reversal` | Time Reversal | zone | attribute basic (also in the set of julius, not exclusive) | kept |
| `stolen_time` | Stolen Time | starter | attribute basic (also in the set of julius, not exclusive) | kept |
| `chrono_anastasis` | Chrono Anastasis | signature | character signature | hidden: exclusive to julius (set) |
| `hourglass_step` | Hourglass Step | mid | mod's own page, no anime spell of that name (kept) | kept |
| `future_sight` | Borrowed Future | mid | mod's own page, no anime spell of that name (kept) | kept |
| `pendulum_ward` | Pendulum Ward | zone | mod's own page, no anime spell of that name (kept) | kept |
| `age_breaker` | Age Breaker | zone | mod's own page, no anime spell of that name (kept) | kept |
| `moment_reprise` | Moment Reprise | signature | mod's own page, no anime spell of that name (kept) | kept |
| `hourglass_storm` | Hourglass Storm | signature | mod's own page, no anime spell of that name (kept) | kept |

### `book_fire` (24 family pages, 6 shown to a basic grimoire)

| Page | Name | Tier | Basis | Status |
|---|---|---|---|---|
| `exploding_fireball` | Exploding Fireball | starter | attribute basic (also in the set of magna, not exclusive) | kept (mode 0, never hidden) |
| `sol_linea` | Sol Linea | mid | character signature | hidden: exclusive to fuegoleon (set) |
| `calderos` | Calderos | zone | Vermillion-house fire page (flame_rebuild.md: from memory, not confirmed) | kept |
| `leo_rugiens` | Leo Rugiens | signature | character signature | hidden: exclusive to fuegoleon (set) |
| `spiral_flame` | Spiral Flame | mid | character signature | hidden: exclusive to leopold (set) |
| `wild_bursting_flame` | Wild Bursting Flame | zone | character signature | hidden: exclusive to leopold (set) |
| `ignis_columna` | Ignis Columna | signature | character signature | hidden: exclusive to fuegoleon (set) |
| `calidos_brachium` | Calidos Brachium | signature | character signature | hidden: exclusive to mereoleona (set) |
| `crimson_lion_king` | Crimson Lion King | signature | Fuegoleon's Crimson Lion King (flame_rebuild.md); no set lists it | hidden: reserved for fuegoleon |
| `flame_armour` | Flame Armour | mid | mod's approximation (flame_rebuild.md says it is not verified canon) | kept |
| `phoenix_flare` | Phoenix Flare | zone | mod's approximation (flame_rebuild.md says it is not verified canon) | kept |
| `leo_parma` | Leo Parma | mid | character signature | hidden: exclusive to fuegoleon (set) |
| `salamanders_breath` | Salamander's Breath | zone | character signature | hidden: exclusive to fuegoleon (set) |
| `exploding_buckshot` | Exploding Buckshot | mid | character signature | hidden: exclusive to magna (set) |
| `grand_slam` | Grand Slam | mid | character signature | hidden: exclusive to magna (set) |
| `soul_chain_deathmatch` | Soul Chain Tag Deathmatch | signature | character signature | hidden: exclusive to magna (set) |
| `mereoleona_brachium_barrage` | Calidus Brachium Barrage | mid | character signature | hidden: exclusive to mereoleona (set) |
| `mereoleona_purgatory` | Calidus Brachium Purgatory | zone | character signature | hidden: exclusive to mereoleona (set) |
| `mereoleona_purgatory_abyss` | Calidus Brachium Purgatory: Abyss | signature | character signature | hidden: exclusive to mereoleona (set) |
| `mereoleona_hellfire_incarnate` | Hellfire Incarnate | signature | character signature | hidden: exclusive to mereoleona (set) |
| `leopold_deep_spiral_flame` | Deep Spiral Flame | zone | character signature | hidden: exclusive to leopold (set) |
| `leopold_crimson_eruption` | True Flame Magic: Crimson Eruption | signature | character signature | hidden: exclusive to leopold (set) |
| `shining_flame_cross` | Shining Flame Cross | mid | anime: Theresa Rapual | NEW (appended last) |
| `combo_eruption` | Combo Eruption | mid | anime: Dmitri Brint | NEW (appended last) |

### `book_water` (17 family pages, 10 shown to a basic grimoire)

| Page | Name | Tier | Basis | Status |
|---|---|---|---|---|
| `waterball` | Sea Dragon's Waterball | starter | attribute basic (also in the set of noelle, not exclusive) | kept (mode 0, never hidden) |
| `roar` | Sea Dragon's Roar | signature | character signature | hidden: exclusive to noelle (set) |
| `cradle` | Sea Dragon's Cradle | zone | character signature | hidden: exclusive to noelle (set) |
| `aqua_javelin` | Aqua Javelin | mid | wiki-named Water Magic spell (user not confirmed, not a character with a set) | kept |
| `sea_dragons_nest` | Sea Dragon's Nest | zone | character signature | hidden: exclusive to noelle (set) |
| `valkyrie_dress` | Valkyrie Dress | signature | character signature | hidden: exclusive to noelle (set) |
| `sea_dragons_wrath` | Sea Dragon's Wrath | signature | mod's own page in Noelle's naming style; no source gives it to anybody (kept) | kept |
| `tide_lance` | Valkyrie Tide Lance | mid | mod's own page in Noelle's naming style; no source gives it to anybody (kept) | kept |
| `scale_mail` | Sea Dragon's Scale Mail | mid | mod's own page in Noelle's naming style; no source gives it to anybody (kept) | kept |
| `torrent` | Sea Dragon's Torrent | zone | mod's own page in Noelle's naming style; no source gives it to anybody (kept) | kept |
| `mermaid_form` | Mermaid Form | zone | Noelle's Mermaid Form (picture 126); the owner's earlier decision keeps it open to every Water mage | kept |
| `sea_dragon_god_form` | Sea Dragon God Form | signature | Noelle's Dragon Form (picture 126) | hidden: reserved for noelle |
| `black_dragon_form` | Black Dragon Form | signature | Noelle's Black Dragon Form (picture 126) | hidden: reserved for noelle |
| `point_blank_roar` | Point-Blank Sea Dragon's Roar | signature | character signature | hidden: exclusive to noelle (set) |
| `aqua_curtain` | Aqua Curtain | mid | anime: Rhya | NEW (appended last) |
| `cascade_destroyer` | Cascade Destroyer | mid | anime: Selena Silva | NEW (appended last) |
| `holy_water_assassination_bullet` | Holy Water Assassination Bullet | mid | anime: Solid Silva | NEW (appended last) |

### `book_wind` (21 family pages, 9 shown to a basic grimoire)

| Page | Name | Tier | Basis | Status |
|---|---|---|---|---|
| `kamaitachi` | Crescent Kamaitachi | starter | attribute basic (also in the set of yuno, not exclusive) | kept (mode 0, never hidden) |
| `gust_lane` | Gust Lane | mid | mod's own page, no anime spell of that name (kept) | kept |
| `spirit_storm` | Spirit Storm | signature | character signature | hidden: exclusive to yuno (set) |
| `towering_tornado` | Towering Tornado | zone | character signature | hidden: exclusive to yuno (set) |
| `slicing_wind_emperor` | Slicing Wind Emperor | signature | wiki-named: Fanzell Kruger (not a character with a set) | kept |
| `spirit_of_zephyr` | Spirit of Zephyr | signature | character signature | hidden: exclusive to yuno (set) |
| `tornado_fang` | Tornado Fang | mid | character signature | hidden: exclusive to yuno (set) |
| `sylphs_breath` | Sylph's Breath | zone | character signature | hidden: exclusive to yuno (set) |
| `spirits_hushed_dance` | Spirit's Hushed Dance | zone | character signature | hidden: exclusive to yuno (set) |
| `liable_tempest` | Liable Tempest | zone | character signature | hidden: exclusive to yuno (set) |
| `gale_white_bow` | Gale White Bow | mid | character signature | hidden: exclusive to yuno (set) |
| `swift_white_hawk` | Swift White Hawk | mid | character signature | hidden: exclusive to yuno (set) |
| `heavenly_wind_ark` | Heavenly Wind Ark | zone | character signature | hidden: exclusive to yuno (set) |
| `wind_blades_shower` | Wind Blades Shower | mid | character signature | hidden: exclusive to yuno (set) |
| `spirit_of_boreas` | Spirit of Boreas | signature | character signature | hidden: exclusive to yuno (set) |
| `slicing_wind_emperor_wintry_wind` | Slicing Wind Emperor: Wintry Wind | mid | anime: Fanzell Kruger | NEW (appended last) |
| `slicing_wind_emperor_gale` | Slicing Wind Emperor: Gale | mid | anime: Fanzell Kruger | NEW (appended last) |
| `slicing_wind_emperor_mountain_retreat` | Slicing Wind Emperor: Mountain Retreat | mid | anime: Fanzell Kruger | NEW (appended last) |
| `slicing_wind_emperor_whirlwind` | Slicing Wind Emperor: Whirlwind | mid | anime: Fanzell Kruger | NEW (appended last) |
| `killing_kakara_blade` | Killing Kakara Blade | mid | anime: Jozou Hanegatsuji | NEW (appended last) |
| `piercing_tornado_needle` | Piercing Tornado Needle | mid | anime: George | NEW (appended last) |

### `book_earth` (9 family pages, 9 shown to a basic grimoire)

| Page | Name | Tier | Basis | Status |
|---|---|---|---|---|
| `spike_line` | Earth Spikes | starter | mod's own page, no anime spell of that name (kept) | kept (mode 0, never hidden) |
| `wall` | Mud Wall Partition | mid | wiki-named spell of a character without a set | kept |
| `mother_earth_split` | Divided Mother Earth | zone | wiki-named spell of a character without a set | kept |
| `boulder_shot` | Boulder Shot | mid | mod's own page, no anime spell of that name (kept) | kept |
| `witch_hunter_claws` | Witch Hunter Claws | mid | wiki-named spell of a character without a set | kept |
| `rampaging_mother_earth` | Rampaging Mother Earth | signature | wiki-named spell of a character without a set | kept |
| `earth_golem` | Clay Guardian | mid | mod's own page, no anime spell of that name (kept) | kept |
| `earth_armor` | Gnome Plate | mid | mod's own page, no anime spell of that name (kept) | kept |
| `sinkhole` | Swallowing Ground | zone | mod's own page, no anime spell of that name (kept) | kept |

### `book_light` (15 family pages, 8 shown to a basic grimoire)

| Page | Name | Tier | Basis | Status |
|---|---|---|---|---|
| `judgment` | Light Sword of Judgment | starter | wiki-named: Light Sword of Judgment, Patolli and Rhya | kept (mode 0, never hidden) |
| `divine_punishment` | Light Shaft of Divine Punishment | zone | wiki-named: Patolli | kept |
| `healing_ray` | Healing Ray of Light | mid | wiki-named spell of a character without a set | kept |
| `lamp_of_avior` | Lamp of Avior Gloria | signature | character signature | hidden: exclusive to lemiel (set) |
| `light_speed` | Light Speed | mid | attribute basic (also in the set of lemiel, not exclusive) | kept |
| `bright_judgment_whip` | Bright Judgment Whip | mid | wiki-named: Patolli | kept |
| `heaven_splitting_flash` | Heaven-Splitting Flash | zone | wiki-named: Patolli | kept |
| `arrows_of_judgment` | Arrows of Judgment | zone | wiki-named: Patolli | kept |
| `avior_lamp` | Lamp of Avior | mid | character signature | hidden: exclusive to lemiel (set) |
| `absolute_monarch` | Absolute Monarch of Divine Brilliance | signature | wiki-named spell of a character without a set | kept |
| `karna_crescent_cut` | Crescent Cut | mid | character signature | hidden: exclusive to karna (set) |
| `karna_bewitching_moonlight` | Bewitching Moonlight | zone | character signature | hidden: exclusive to karna (set) |
| `karna_full_moon_shell` | Full Moon Shell | mid | character signature | hidden: exclusive to karna (set) |
| `karna_crescent_cut_full_moon` | Crescent Cut Full Moon | zone | character signature | hidden: exclusive to karna (set) |
| `karna_lunatic_crisis` | Lunatic Crisis | signature | character signature | hidden: exclusive to karna (set) |

### `book_dark` (15 family pages, 5 shown to a basic grimoire)

| Page | Name | Tier | Basis | Status |
|---|---|---|---|---|
| `avidya_slash` | Dark Cloaked Avidya Slash | starter | attribute basic (also in the set of yami, not exclusive) | kept (mode 0, never hidden) |
| `black_hole` | Black Hole | zone | character signature | hidden: exclusive to yami (set) |
| `dimension_slash` | Dark Cloaked Dimension Slash | signature | character signature | hidden: exclusive to yami (set) |
| `death_thrust` | Death Thrust | mid | character signature | hidden: exclusive to yami (set) |
| `black_blade` | Dark Cloaked Black Blade | mid | character signature | hidden: exclusive to yami (set) |
| `avidya_wild_slash` | Dark Cloaked Avidya Wild Slash | mid | character signature | hidden: exclusive to yami (set) |
| `black_moon` | Dark Cloaked Black Moon | zone | character signature | hidden: exclusive to yami (set) |
| `iai_slash` | Dark Cloaked Iai Slash | mid | character signature | hidden: exclusive to yami (set) |
| `dimension_slash_equinox` | Dark Cloaked Dimension Slash: Equinox | signature | character signature | hidden: exclusive to yami (set) |
| `dark_cloak` | Dark Cloaked Guard | mid | mod's own page, no anime spell of that name (kept) | kept |
| `shadow_embrace` | Dark Cloaked Shadow Embrace | zone | mod's own page, no anime spell of that name (kept) | kept |
| `black_hole_collapse` | Black Hole Collapse | signature | mod's own page, no anime spell of that name (kept) | kept |
| `deep_black_blade` | Dark Cloaked Deep Black Blade | mid | character signature | hidden: exclusive to yami (set) |
| `black_crescent_moon` | Black Crescent Moon | signature | character signature | hidden: exclusive to yami (set) |
| `black_star` | Black Star | mid | anime: Ichika Yami | NEW (appended last) |

### `book_spatial` (14 family pages, 8 shown to a basic grimoire)

| Page | Name | Tier | Basis | Status |
|---|---|---|---|---|
| `domination` | Sacred Mana Domination | starter | attribute basic (also in the set of langris, not exclusive) | kept (mode 0, never hidden) |
| `fallen_angel_gate` | Fallen Angel Gate | mid | Spatial Magic: Fallen Angel Gate, Finral's portal | hidden: reserved for finral |
| `space_cut` | Spatial Cut | zone | mod's own page, no anime spell of that name (kept) | kept |
| `unopening_red_room` | Unopening Red Room | zone | mod's own page, no anime spell of that name (kept) | kept |
| `myriad_black` | Myriad Black | mid | mod's own page, no anime spell of that name (kept) | kept |
| `door_of_fate` | Door of Fate | signature | Compound Magic: Door of Fate, Finral with Vanessa | hidden: reserved for finral |
| `dimensional_armour` | Dimensional Armour | mid | mod's own page, no anime spell of that name (kept) | kept |
| `gate_strike` | Gate Strike | mid | mod's own page, no anime spell of that name (kept) | kept |
| `gate_swap` | Gate Swap | mid | mod's own page, no anime spell of that name (kept) | kept |
| `dimension_collapse` | Dimension Collapse | signature | mod's own page, no anime spell of that name (kept) | kept |
| `fallen_angel_wingbeat` | Fallen Angel Wingbeat | mid | character signature | hidden: exclusive to finral (set) |
| `langris_archangel_crash` | Archangel's Crash | mid | character signature | hidden: exclusive to langris (set) |
| `langris_archangel_annihilation` | Archangel's Annihilation | signature | character signature | hidden: exclusive to langris (set) |
| `langris_spatial_auto_guard` | Spatial Auto-Guard | mid | character signature | hidden: exclusive to langris (set) |

### `book_lightning` (16 family pages, 7 shown to a basic grimoire)

| Page | Name | Tier | Basis | Status |
|---|---|---|---|---|
| `chain` | Chain Lightning | starter | attribute basic (also in the set of gadjah, not exclusive) | kept (mode 0, never hidden) |
| `thunder_boots` | Thunder God's Boots | mid | Lightning Creation Magic: Thunder God's Boots, Luck's | hidden: reserved for luck |
| `thunder_lance` | Thunder Lance | zone | mod's own page, no anime spell of that name (kept) | kept |
| `thunder_fiend` | Thunder Fiend | mid | character signature | hidden: exclusive to luck (set) |
| `pulsaranta` | Pulsaranta | mid | Lightning Magic: Pulsaranta, Gadjah's | hidden: reserved for gadjah |
| `rising_salim` | God of Lightning Rising Salim | signature | wiki-named: Salim Hapshass | kept |
| `thunderbird_cavalry` | Thunderbird Cavalry: Sky-Splitting Magic Bow Armament | zone | wiki-named spell of a character without a set | kept |
| `black_lightning_battle_fiend` | Black Lightning Battle Fiend | signature | character signature | hidden: exclusive to luck (set) |
| `raging_lightning` | Raging Lightning | mid | attribute basic (also in the set of luck, not exclusive) | kept |
| `thunder_drum` | Raijin's Thunder Drums | zone | mod's own page, no anime spell of that name (kept) | kept |
| `lightning_surge` | Lightning Surge | mid | mod's own page, no anime spell of that name (kept) | kept |
| `thunderclap_crumbling_orb` | Thunderclap Crumbling Orb | mid | character signature | hidden: exclusive to luck (set) |
| `thunder_gods_gloves` | Thunder God's Gloves | mid | character signature | hidden: exclusive to luck (set) |
| `keraunos` | Keraunos | signature | character signature | hidden: exclusive to luck (set) |
| `tenjiolia` | Lightning Magic: Tenjiolia | mid | character signature | hidden: exclusive to gadjah (set) |
| `apocalypra_astrauza` | Lightning Ultimate Magic: Apocalypra Astrauza | signature | character signature | hidden: exclusive to gadjah (set) |

### `book_steel` (9 family pages, 5 shown to a basic grimoire)

| Page | Name | Tier | Basis | Status |
|---|---|---|---|---|
| `steel_shot` | Blazing Steel Shot | starter | attribute basic (also in the set of klaus, not exclusive) | kept (mode 0, never hidden) |
| `returning_lance` | Returning Lance | signature | mod's own page, no anime spell of that name (kept) | kept |
| `obsidian_shot` | Steel Cannon | mid | mod's own page, no anime spell of that name (kept) | kept |
| `steel_spikes` | Steel Spikes | starter | mod's own page, no anime spell of that name (kept) | kept |
| `iron_maiden` | Iron Maiden | mid | mod's own page, no anime spell of that name (kept) | kept |
| `klaus_piercing_lance` | Blazing Piercing Lance | mid | character signature | hidden: exclusive to klaus (set) |
| `klaus_castle_wall` | Steel Castle's Armored Wall | mid | character signature | hidden: exclusive to klaus (set) |
| `klaus_spiral_lance` | Violent Spiral Lance | zone | character signature | hidden: exclusive to klaus (set) |
| `klaus_thunder_arrow` | Thunder Arrow | signature | character signature | hidden: exclusive to klaus (set) |

### `book_mirror` (11 family pages, 6 shown to a basic grimoire)

| Page | Name | Tier | Basis | Status |
|---|---|---|---|---|
| `reflect_refrain` | Reflect Refrain | starter | attribute basic (also in the set of gauche, not exclusive) | kept (mode 0, never hidden) |
| `real_double` | Real Double | signature | character signature | hidden: exclusive to gauche (set) |
| `reflect_ray` | Reflect Ray | mid | attribute basic (also in the set of gauche, not exclusive) | kept |
| `large_reflect_ray` | Large Reflect Ray | zone | attribute basic (also in the set of gauche, not exclusive) | kept |
| `full_reflection` | Full Reflection | signature | character signature | hidden: exclusive to gauche (set) |
| `mirror_array` | Mirror Array | mid | mod's own page, no anime spell of that name (kept) | kept |
| `mirror_step` | Mirror Step | mid | mod's own page, no anime spell of that name (kept) | kept |
| `mirrors_slash` | Mirrors Slash | zone | mod's own page, no anime spell of that name (kept) | kept |
| `mirrors_meteorite` | Mirrors Meteorite | signature | character signature | hidden: exclusive to gauche (set) |
| `double_reflect_refrain` | Double Reflect Refrain | zone | character signature | hidden: exclusive to gauche (set) |
| `mirrors_brigade` | Mirrors Brigade | zone | character signature | hidden: exclusive to gauche (set) |

### `book_thread` (9 family pages, 4 shown to a basic grimoire)

| Page | Name | Tier | Basis | Status |
|---|---|---|---|---|
| `red_thread` | Red Thread | starter | attribute basic (also in the set of vanessa, not exclusive) | kept (mode 0, never hidden) |
| `rouge` | Red Thread of Fate: Rouge | daily | character signature | hidden: exclusive to vanessa (set) |
| `arachnes_web` | Arachne's Web | zone | attribute basic (also in the set of vanessa, not exclusive) | kept |
| `dancing_doll` | Dancing Doll | mid | attribute basic (also in the set of vanessa, not exclusive) | kept |
| `thread_mending` | Thread Magic: Mending | mid | attribute basic (also in the set of vanessa, not exclusive) | kept |
| `arresting_dark_weave` | Arresting Dark Weave | mid | character signature | hidden: exclusive to vanessa (set) |
| `dancing_doll_recall` | Dancing Doll: Recall | mid | character signature | hidden: exclusive to vanessa (set) |
| `thread_trap` | Thread Trap Generation | zone | character signature | hidden: exclusive to vanessa (set) |
| `reinforcement` | Reinforcement Magic and Mana Skin | custom | character signature | hidden: exclusive to vanessa (set) |

### `book_plant` (14 family pages, 8 shown to a basic grimoire)

| Page | Name | Tier | Basis | Status |
|---|---|---|---|---|
| `guidepost` | Magic Flower Guidepost | mid | attribute basic (also in the set of mimosa, not exclusive) | kept (mode 0, never hidden) |
| `hundred_flowers` | Dream World of a Hundred Flowers | signature | attribute basic (also in the set of mimosa, not exclusive) | kept |
| `yggdrasil` | World Tree Magic: Yggdrasil | signature | World Tree Magic: Yggdrasil is William's; locked to him, listed in the Plant book | hidden: reserved for william |
| `briar_prison` | Briar Magic: Briar Prison | zone | Briar Magic: Briar Prison is Charlotte's; locked to her, listed in the Plant book | hidden: reserved for charlotte |
| `seed_bloom` | Plant Magic: Seed Bloom | starter | mod's own page, no anime spell of that name (kept) | kept |
| `healing_bloom` | Plant Magic: Healing Bloom | mid | mod's own page, no anime spell of that name (kept) | kept |
| `flower_sprite` | Plant Magic: Flower Sprite | signature | mod's own page, no anime spell of that name (kept) | kept |
| `ivy_barrier` | Plant Magic: Ivy Barrier | mid | mod's own page, no anime spell of that name (kept) | kept |
| `blossom_dress` | Plant Magic: Blossom Dress | mid | mod's own page, no anime spell of that name (kept) | kept |
| `petal_storm` | Plant Magic: Petal Storm | zone | mod's own page, no anime spell of that name (kept) | kept |
| `mimosa_magic_flower_cannon` | Magic Flower Cannon | mid | character signature | hidden: exclusive to mimosa (set) |
| `mimosa_dream_healing_cradle` | Dream-Healing Flower Cradle | mid | character signature | hidden: exclusive to mimosa (set) |
| `mimosa_princess_healing_robe` | Princess-Healing Flower Robe | mid | character signature | hidden: exclusive to mimosa (set) |
| `mimosa_flower_princess_utopia` | Flower Princess Utopia | signature | character signature | hidden: exclusive to mimosa (set) |

### `book_sealing` (7 family pages, 5 shown to a basic grimoire)

| Page | Name | Tier | Basis | Status |
|---|---|---|---|---|
| `seal` | Seal | zone | attribute basic (also in the set of secre, not exclusive) | kept (mode 0, never hidden) |
| `grand_seal` | Grand Seal | signature | mod's own page, no anime spell of that name (kept) | kept |
| `sealing_chains` | Seal Magic: Sealing Chains | mid | mod's own page, no anime spell of that name (kept) | kept |
| `trinity_seal` | Trinity Seal Magic | signature | Trinity Seal Magic, the Seal Magic wiki's spell (Secre's attribute) | hidden: reserved for secre |
| `seal_barrier` | Seal Magic: Barrier | zone | mod's own page, no anime spell of that name (kept) | kept |
| `random_seal` | Seal Magic: Sigil Combo | mid | mod's own page, no anime spell of that name (kept) | kept |
| `inverse_release` | Seal Magic: Inverse Release | mid | character signature | hidden: exclusive to secre (set) |

### `book_gravity` (5 family pages, 4 shown to a basic grimoire)

| Page | Name | Tier | Basis | Status |
|---|---|---|---|---|
| `heavy_infighting` | Heavy Infighting | starter | attribute basic (also in the set of dante, not exclusive) | kept (mode 0, never hidden) |
| `demon_king` | Presence of the Demon King | signature | character signature | hidden: exclusive to dante (set) |
| `gravity_sphere` | Gravity Sphere | mid | attribute basic (also in the set of dante, not exclusive) | kept |
| `gravity_well` | Gravity Well | zone | mod's own page, no anime spell of that name (kept) | kept |
| `crushing_weight` | Crushing Weight | mid | attribute basic (also in the set of dante, not exclusive) | kept |

### `book_ice` (9 family pages, 9 shown to a basic grimoire)

| Page | Name | Tier | Basis | Status |
|---|---|---|---|---|
| `frost_lance` | Frost Lance | starter | mod's own page, no anime spell of that name (kept) | kept (mode 0, never hidden) |
| `ice_prison` | Ice Prison | signature | mod's own page, no anime spell of that name (kept) | kept |
| `frost_ball` | Frost Ball | starter | mod's own page, no anime spell of that name (kept) | kept |
| `frozen_ground` | Frozen Ground | zone | mod's own page, no anime spell of that name (kept) | kept |
| `blizzard_crown` | Blizzard Crown | mid | mod's own page, no anime spell of that name (kept) | kept |
| `heavenly_ice_fang` | Heavenly Ice Fang | mid | anime: Heath Graice | NEW (appended last) |
| `icicle_bridge` | Icicle Bridge | mid | anime: Ice Magic category | NEW (appended last) |
| `dissolve` | Dissolve | mid | anime: Heath Graice | NEW (appended last) |
| `crystal_ice_shield` | Crystal Ice Shield | mid | anime: Heath Graice | NEW (appended last) |

### `book_sword` (6 family pages, 5 shown to a basic grimoire)

| Page | Name | Tier | Basis | Status |
|---|---|---|---|---|
| `flying_blade` | Origin Flash | starter | mod's own page, no anime spell of that name (kept) | kept (mode 0, never hidden) |
| `thousand_swords` | Origin Flash Barrage | zone | mod's own page, no anime spell of that name (kept) | kept |
| `blade_domain` | Demon-Dweller Sword: Conquering Eon | signature | mod's own page, no anime spell of that name (kept) | kept |
| `summon_demon_dweller` | Sword Magic: Demon-Dweller Sword | mid | mod's own page, no anime spell of that name (kept) | kept |
| `summon_demon_destroyer` | Sword Magic: Demon-Destroyer Sword | mid | mod's own page, no anime spell of that name (kept) | kept |
| `ripper_cut` | Slash Magic: Ripper Cut | signature | Slash Magic: Ripper Cut is Jack the Ripper's; locked to him, listed in the Sword book | hidden: reserved for jack |

### `book_explosion` (4 family pages, 4 shown to a basic grimoire)

| Page | Name | Tier | Basis | Status |
|---|---|---|---|---|
| `burst_shot` | Burst Shot | starter | mod's own page, no anime spell of that name (kept) | kept (mode 0, never hidden) |
| `chain_detonation` | Chain Detonation | zone | mod's own page, no anime spell of that name (kept) | kept |
| `grand_explosion` | Grand Explosion | signature | mod's own page, no anime spell of that name (kept) | kept |
| `heat_sphere` | Heat Sphere | mid | mod's own page, no anime spell of that name (kept) | kept |

### `book_magma` (5 family pages, 5 shown to a basic grimoire)

| Page | Name | Tier | Basis | Status |
|---|---|---|---|---|
| `magma_bullet` | Magma Bullet | starter | mod's own page, no anime spell of that name (kept) | kept (mode 0, never hidden) |
| `magma_pool` | Magma Pool | zone | mod's own page, no anime spell of that name (kept) | kept |
| `eruption` | Eruption | signature | mod's own page, no anime spell of that name (kept) | kept |
| `lava_lance` | Lava Lance | mid | mod's own page, no anime spell of that name (kept) | kept |
| `volcanic_bomb` | Volcanic Bomb | mid | mod's own page, no anime spell of that name (kept) | kept |

### `book_mist` (4 family pages, 4 shown to a basic grimoire)

| Page | Name | Tier | Basis | Status |
|---|---|---|---|---|
| `mist_veil` | Mist Veil | starter | mod's own page, no anime spell of that name (kept) | kept (mode 0, never hidden) |
| `mist_step` | Mist Step | mid | mod's own page, no anime spell of that name (kept) | kept |
| `mist_prison` | Mist Prison | signature | mod's own page, no anime spell of that name (kept) | kept |
| `steam_ball` | Steam Ball | starter | mod's own page, no anime spell of that name (kept) | kept |

### `book_star` (6 family pages, 6 shown to a basic grimoire)

| Page | Name | Tier | Basis | Status |
|---|---|---|---|---|
| `star_shot` | Star Shot | starter | mod's own page, no anime spell of that name (kept) | kept (mode 0, never hidden) |
| `starfall` | Starfall | zone | mod's own page, no anime spell of that name (kept) | kept |
| `constellation` | Constellation | signature | mod's own page, no anime spell of that name (kept) | kept |
| `comet` | Comet | mid | mod's own page, no anime spell of that name (kept) | kept |
| `star_shield` | Star Shield | mid | mod's own page, no anime spell of that name (kept) | kept |
| `supernova` | Supernova | zone | mod's own page, no anime spell of that name (kept) | kept |

### `book_storm` (6 family pages, 4 shown to a basic grimoire)

| Page | Name | Tier | Basis | Status |
|---|---|---|---|---|
| `storm_lance` | Storm Lance | starter | mod's own page, no anime spell of that name (kept) | kept (mode 0, never hidden) |
| `tempest_ring` | Tempest Ring | mid | mod's own page, no anime spell of that name (kept) | kept |
| `eye_of_the_storm` | Eye of the Storm | signature | mod's own page, no anime spell of that name (kept) | kept |
| `thunder_sphere` | Thunder Sphere | zone | mod's own page, no anime spell of that name (kept) | kept |
| `vortex_shield` | Vortex Magic: Vortex Shield | zone | character signature | hidden: exclusive to kaiser (set) |
| `kaiser_area_helix` | Vortex Magic: Area Helix | signature | character signature | hidden: exclusive to kaiser (set) |

### `book_sand` (5 family pages, 5 shown to a basic grimoire)

| Page | Name | Tier | Basis | Status |
|---|---|---|---|---|
| `sand_spear` | Sand Spear | starter | mod's own page, no anime spell of that name (kept) | kept (mode 0, never hidden) |
| `sandstorm` | Sandstorm | zone | mod's own page, no anime spell of that name (kept) | kept |
| `quicksand_tomb` | Quicksand Tomb | signature | mod's own page, no anime spell of that name (kept) | kept |
| `sand_blast` | Sand Blast | starter | mod's own page, no anime spell of that name (kept) | kept |
| `desert_coffin` | Desert Coffin | mid | mod's own page, no anime spell of that name (kept) | kept |

### `book_shadow` (13 family pages, 5 shown to a basic grimoire)

| Page | Name | Tier | Basis | Status |
|---|---|---|---|---|
| `shadow_step` | Dark Garden Invitation | starter | attribute basic (also in the set of nacht, not exclusive) | kept (mode 0, never hidden) |
| `shadow_bind` | Kids' Playground | zone | mod's own page, no anime spell of that name (kept) | kept |
| `shadow_realm` | Shadow Realm | signature | mod's own page, no anime spell of that name (kept) | kept |
| `heavens_shadow_second_sight` | Heaven's Shadow Second Sight | mid | attribute basic (also in the set of nacht, not exclusive) | kept |
| `unite_canis` | Unite Mode: Canis | mid | character signature | hidden: exclusive to nacht (set) |
| `unite_gallus` | Unite Mode: Gallus | mid | character signature | hidden: exclusive to nacht (set) |
| `unite_canis_felis` | Unite Mode: Canis x Felis | signature | character signature | hidden: exclusive to nacht (set) |
| `shadow_travel` | Shadow Travel | mid | attribute basic (also in the set of nacht, not exclusive) | kept |
| `unite_equus` | Unite Mode: Equus | custom | character signature | hidden: exclusive to nacht (set) |
| `unite_felis` | Unite Mode: Felis | mid | character signature | hidden: exclusive to nacht (set) |
| `unite_gallus_call` | Gallus: Walgner's Call | mid | character signature | hidden: exclusive to nacht (set) |
| `dark_hunting_ground` | Dark Hunting Ground | zone | character signature | hidden: exclusive to nacht (set) |
| `atonement_monument` | Atonement Monument | signature | character signature | hidden: exclusive to nacht (set) |

### `book_poison` (7 family pages, 4 shown to a basic grimoire)

| Page | Name | Tier | Basis | Status |
|---|---|---|---|---|
| `venom_bolt` | Aufwachen Dachs | starter | attribute basic (also in the set of gordon, not exclusive) | kept (mode 0, never hidden) |
| `toxic_cloud` | Violett Schirm | zone | attribute basic (also in the set of gordon, not exclusive) | kept |
| `plague_nova` | Basilisk's Breath | signature | mod's own page, no anime spell of that name (kept) | kept |
| `acid_ball` | Acid Ball | mid | mod's own page, no anime spell of that name (kept) | kept |
| `curse_workers_neighbor` | Curse-Worker's Neighbor | mid | character signature | hidden: exclusive to gordon (set) |
| `verbotenes_obst` | Verbotenes Obst | mid | character signature | hidden: exclusive to gordon (set) |
| `poison_clouds_abode` | Poison Cloud's Abode | zone | character signature | hidden: exclusive to gordon (set) |

### `book_reinforcement` (5 family pages, 5 shown to a basic grimoire)

| Page | Name | Tier | Basis | Status |
|---|---|---|---|---|
| `iron_fist` | Iron Fist | starter | mod's own page, no anime spell of that name (kept) | kept (mode 0, never hidden) |
| `mana_reinforcement` | Mana Reinforcement | mid | mod's own page, no anime spell of that name (kept) | kept |
| `full_body` | Full Body Reinforcement | signature | mod's own page, no anime spell of that name (kept) | kept |
| `palm_strike` | Palm Strike | starter | mod's own page, no anime spell of that name (kept) | kept |
| `titan_stomp` | Titan Stomp | zone | mod's own page, no anime spell of that name (kept) | kept |

### `book_beast` (8 family pages, 8 shown to a basic grimoire)

| Page | Name | Tier | Basis | Status |
|---|---|---|---|---|
| `beast_charge` | Beast Charge | starter | mod's own page (the Beast wiki spells are in the added pages) | kept (mode 0, never hidden) |
| `lions_roar` | Lion's Roar | mid | mod's own page, no anime spell of that name (kept) | kept |
| `beast_form` | Beast Form | signature | mod's own page, no anime spell of that name (kept) | kept |
| `feral_claws` | Feral Claws | mid | mod's own page, no anime spell of that name (kept) | kept |
| `pack_hunt` | Pack Hunt | zone | mod's own page, no anime spell of that name (kept) | kept |
| `bear_claw` | Bear Claw | mid | anime: Vetto | NEW (appended last) |
| `cheetah_charge` | Cheetah Charge | mid | anime: Vetto | NEW (appended last) |
| `rhinoceros_armor` | Rhinoceros Armor | mid | anime: Vetto | NEW (appended last) |

### `book_bone` (5 family pages, 4 shown to a basic grimoire)

| Page | Name | Tier | Basis | Status |
|---|---|---|---|---|
| `bone_spear` | Bone Spear | starter | attribute basic (also in the set of zenon, not exclusive) | kept (mode 0, never hidden) |
| `bone_armor` | Bone Armor | mid | attribute basic (also in the set of zenon, not exclusive) | kept |
| `ossuary` | Ossuary | signature | character signature | hidden: exclusive to zenon (set) |
| `rib_cage` | Rib Cage | starter | mod's own page, no anime spell of that name (kept) | kept |
| `bone_rain` | Bone Rain | zone | mod's own page, no anime spell of that name (kept) | kept |

### `book_blood` (7 family pages, 5 shown to a basic grimoire)

| Page | Name | Tier | Basis | Status |
|---|---|---|---|---|
| `blood_bullet` | Blood Bullet | starter | attribute basic (also in the set of vanica, not exclusive) | kept (mode 0, never hidden) |
| `crimson_rain` | Crimson Rain | zone | mod's own page, no anime spell of that name (kept) | kept |
| `blood_pact` | Blood Pact | signature | mod's own page, no anime spell of that name (kept) | kept |
| `blood_whip` | Blood Whip | mid | attribute basic (also in the set of vanica, not exclusive) | kept |
| `hemorrhage` | Hemorrhage | mid | mod's own page, no anime spell of that name (kept) | kept |
| `red_beast` | Blood Magic: Red Beast | mid | character signature | hidden: exclusive to vanica (set) |
| `herd_of_red_beasts` | Blood Magic: Herd of Red Beasts | zone | character signature | hidden: exclusive to vanica (set) |

### `book_creation` (4 family pages, 3 shown to a basic grimoire)

| Page | Name | Tier | Basis | Status |
|---|---|---|---|---|
| `created_blade` | Creation: Blade | starter | mod's own page, no anime spell of that name (kept) | kept (mode 0, never hidden) |
| `created_wall` | Creation: Wall | mid | mod's own page, no anime spell of that name (kept) | kept |
| `arsenal` | Creation: Arsenal | signature | mod's own page, no anime spell of that name (kept) | kept |
| `painted_menagerie` | Painting Magic: Painted Menagerie | signature | Painting Magic: Painted Menagerie is Rill's; locked to him, listed in the Creation book | hidden: reserved for rill |

### `book_copy` (4 family pages, 4 shown to a basic grimoire)

| Page | Name | Tier | Basis | Status |
|---|---|---|---|---|
| `echo_bolt` | Echo Bolt | starter | mod's own page, no anime spell of that name (kept) | kept (mode 0, never hidden) |
| `copy` | Copy | zone | mod's own page, no anime spell of that name (kept) | kept |
| `mimic_volley` | Mimic Volley | mid | mod's own page, no anime spell of that name (kept) | kept |
| `reflection_double` | Reflection Double | mid | mod's own page, no anime spell of that name (kept) | kept |

### `book_illusion` (5 family pages, 5 shown to a basic grimoire)

| Page | Name | Tier | Basis | Status |
|---|---|---|---|---|
| `phantom_strike` | Phantom Strike | starter | mod's own page, no anime spell of that name (kept) | kept (mode 0, never hidden) |
| `decoy` | Decoy | mid | mod's own page, no anime spell of that name (kept) | kept |
| `mass_illusion` | Mass Illusion | signature | mod's own page, no anime spell of that name (kept) | kept |
| `false_step` | False Step | starter | mod's own page, no anime spell of that name (kept) | kept |
| `waking_nightmare` | Waking Nightmare | zone | mod's own page, no anime spell of that name (kept) | kept |

### `book_dream` (5 family pages, 4 shown to a basic grimoire)

| Page | Name | Tier | Basis | Status |
|---|---|---|---|---|
| `dream_haze` | Dream Haze | starter | attribute basic (also in the set of dorothy, not exclusive) | kept (mode 0, never hidden) |
| `sweet_dream` | Sweet Dream | mid | mod's own page, no anime spell of that name (kept) | kept |
| `dream_world` | Dream World | signature | attribute basic (also in the set of dorothy, not exclusive) | kept |
| `glamour_world` | Glamour World | signature | character signature | hidden: exclusive to dorothy (set) |
| `imagination_manifestation` | Imagination Manifestation | mid | attribute basic (also in the set of dorothy, not exclusive) | kept |

### `book_transmutation` (1 family pages, 1 shown to a basic grimoire)

| Page | Name | Tier | Basis | Status |
|---|---|---|---|---|
| `transformation_guise` | Transformation: Borrowed Form | mid | character signature | mode 0: hidden: exclusive to grey (set) (the engine cannot hide mode 0, it stays) |

### `book_ash` (9 family pages, 5 shown to a basic grimoire)

| Page | Name | Tier | Basis | Status |
|---|---|---|---|---|
| `ash_bullets` | Ash Bullets | starter | attribute basic (also in the set of zora, not exclusive) | kept (mode 0, never hidden) |
| `ash_cloud` | Ash Cloud | mid | mod's own page, no anime spell of that name (kept) | kept |
| `ash_absorbing_formation` | Ash Absorbing Formation | zone | character signature | hidden: exclusive to zora (set) |
| `revelation_of_the_cowardly` | Revelation of the Cowardly | signature | character signature | hidden: exclusive to zora (set) |
| `ash_shroud` | Ash Shroud | mid | mod's own page, no anime spell of that name (kept) | kept |
| `ash_soldiers` | Ash Soldiers | zone | mod's own page, no anime spell of that name (kept) | kept |
| `cursed_ash` | Cursed Ash | mid | mod's own page, no anime spell of that name (kept) | kept |
| `ash_snare_trap` | Ash Snare Trap | mid | character signature | hidden: exclusive to zora (set) |
| `counter_trap_magic` | Counter Trap Magic | signature | character signature | hidden: exclusive to zora (set) |

### `book_cotton` (7 family pages, 4 shown to a basic grimoire)

| Page | Name | Tier | Basis | Status |
|---|---|---|---|---|
| `sleeping_sheep_strike` | Sleeping Sheep Strike | starter | attribute basic (also in the set of charmy, not exclusive) | kept (mode 0, never hidden) |
| `sheep_cook` | Sheep Cook | mid | attribute basic (also in the set of charmy, not exclusive) | kept |
| `sheep_bondage` | Sheep Bondage | mid | attribute basic (also in the set of charmy, not exclusive) | kept |
| `cotton_cloud` | Cotton Cloud | zone | attribute basic (also in the set of charmy, not exclusive) | kept |
| `gourmets_bite` | Gourmet's Bite | mid | Gourmet's Bite, Charmy's | hidden: reserved for charmy |
| `gluttons_banquet` | Glutton's Banquet | signature | character signature | hidden: exclusive to charmy (set) |
| `fluffy_sheep_cushion` | Fluffy Sheep Cushion | mid | character signature | hidden: exclusive to charmy (set) |

### `book_recombination` (2 family pages, 1 shown to a basic grimoire)

| Page | Name | Tier | Basis | Status |
|---|---|---|---|---|
| `mana_rocket_punch` | Mana Rocket Punch | mid | character signature | mode 0: hidden: exclusive to henry (set) (the engine cannot hide mode 0, it stays) |
| `mini_bull` | The Mini Bull | mid | character signature | hidden: exclusive to henry (set) |

### `book_painting` (17 family pages, 5 shown to a basic grimoire)

| Page | Name | Tier | Basis | Status |
|---|---|---|---|---|
| `brushstroke` | Brushstroke | starter | attribute basic (also in the set of rill, not exclusive) | kept (mode 0, never hidden) |
| `spring_of_restriction` | Spring of Restriction | mid | attribute basic (also in the set of rill, not exclusive) | kept |
| `camouflage` | Camouflage | mid | mod's own page, no anime spell of that name (kept) | kept |
| `souterrain_giant_arm` | Souterrain Giant's Strong Arm | mid | Souterrain Giant's Strong Arm, the arm of Rill's Souterrain Giant | hidden: reserved for rill |
| `deux_tempetes` | Deux Tempêtes of Fire and Ice | zone | character signature | hidden: exclusive to rill (set) |
| `gods_game` | God's Game | signature | character signature | hidden: exclusive to rill (set) |
| `elemental_quintet` | Elemental Quintet | signature | character signature | hidden: exclusive to rill (set) |
| `master_of_valhalla` | Master of Valhalla | signature | character signature | hidden: exclusive to rill (set) |
| `painted_menagerie` | Painted Menagerie | signature | character signature | hidden: exclusive to rill (set) |
| `living_illustration` | Living Illustration | mid | mod's own page, no anime spell of that name (kept) | kept |
| `counter_palette` | Counter Palette | mid | mod's own page, no anime spell of that name (kept) | kept |
| `souterrain_groundswell` | Painting Magic: Souterrain Groundswell | mid | character signature | hidden: exclusive to rill (set) |
| `sirenes_game` | Painting Magic: Sirene's Game | mid | character signature | hidden: exclusive to rill (set) |
| `four_headed_lindworm` | Painting Magic: Four-Headed Lindworm | zone | character signature | hidden: exclusive to rill (set) |
| `vouivres_shout` | Painting Magic: Phantom Dragon Vouivre's Shout | zone | character signature | hidden: exclusive to rill (set) |
| `souterrain_giant` | Painting Magic: Souterrain Giant | zone | character signature | hidden: exclusive to rill (set) |
| `twilight_of_valhalla` | Painting Magic: Twilight of Valhalla | signature | character signature | hidden: exclusive to rill (set) |

### `book_world_tree` (7 family pages, 1 shown to a basic grimoire)

| Page | Name | Tier | Basis | Status |
|---|---|---|---|---|
| `mistilteinn_seed` | Mistilteinn Seed | starter | character signature | mode 0: hidden: exclusive to william (set) (the engine cannot hide mode 0, it stays) |
| `root_bind` | Root Bind | mid | character signature | hidden: exclusive to william (set) |
| `magic_tree_descent` | Magic Tree Descent | zone | character signature | hidden: exclusive to william (set) |
| `budding_of_yggdrasil` | Budding of Yggdrasil | signature | character signature | hidden: exclusive to william (set) |
| `william_magic_warding_ash` | Magic Warding Ash | mid | character signature | hidden: exclusive to william (set) |
| `william_great_tree_mistilteinn` | Great Tree Mistilteinn | zone | character signature | hidden: exclusive to william (set) |
| `william_mistilteinn_blade` | Mistilteinn Blade | mid | character signature | hidden: exclusive to william (set) |

### `book_dice` (4 family pages, 4 shown to a basic grimoire)

| Page | Name | Tier | Basis | Status |
|---|---|---|---|---|
| `elemental_dice` | Elemental Dice | starter | mod's own page, no anime spell of that name (kept) | kept (mode 0, never hidden) |
| `fate_die` | Fate Die | mid | mod's own page, no anime spell of that name (kept) | kept |
| `gamblers_fallacy` | Gambler's Fallacy | signature | wiki-named spell of a character without a set | kept |
| `loaded_dice` | Loaded Dice | mid | mod's own page, no anime spell of that name (kept) | kept |

### `book_slash` (4 family pages, 3 shown to a basic grimoire)

| Page | Name | Tier | Basis | Status |
|---|---|---|---|---|
| `slash_wave` | Slash Wave | starter | attribute basic (also in the set of jack, not exclusive) | kept (mode 0, never hidden) |
| `forearm_blades` | Forearm Blades | mid | attribute basic (also in the set of jack, not exclusive) | kept |
| `ripper_dash` | Ripper Dash | mid | mod's own page, no anime spell of that name (kept) | kept |
| `death_scythe` | Death Scythe | signature | character signature | hidden: exclusive to jack (set) |

### `book_compass` (4 family pages, 3 shown to a basic grimoire)

| Page | Name | Tier | Basis | Status |
|---|---|---|---|---|
| `useless_north` | Useless North | starter | attribute basic (also in the set of letoile, not exclusive) | kept (mode 0, never hidden) |
| `willful_compass` | Willful Compass | mid | attribute basic (also in the set of letoile, not exclusive) | kept |
| `another_atlas` | Another Atlas | mid | character signature | hidden: exclusive to letoile (set) |
| `compass_rose` | Compass Rose | signature | mod's own page, no anime spell of that name (kept) | kept |

### `book_game` (6 family pages, 6 shown to a basic grimoire)

| Page | Name | Tier | Basis | Status |
|---|---|---|---|---|
| `game_board` | Game Board | starter | mod's own page, no anime spell of that name (kept) | kept (mode 0, never hidden) |
| `monster_toy` | Monster Toy | starter | mod's own page, no anime spell of that name (kept) | kept |
| `temple_shuffle` | Temple Shuffle | mid | mod's own page, no anime spell of that name (kept) | kept |
| `initiative` | Initiative | mid | mod's own page, no anime spell of that name (kept) | kept |
| `dm_verdict` | Dungeon Master's Verdict | signature | mod's own page, no anime spell of that name (kept) | kept |
| `trap_squares` | Trap Squares | mid | mod's own page, no anime spell of that name (kept) | kept |

### `book_demon_beast` (12 family pages, 12 shown to a basic grimoire)

| Page | Name | Tier | Basis | Status |
|---|---|---|---|---|
| `demon_beast_strike` | Underworld Beast Strike | starter | mod's own page, no anime spell of that name (kept) | kept (mode 0, never hidden) |
| `chimera_claw_rend` | Chimera Claw Rend | starter | mod's own page, no anime spell of that name (kept) | kept |
| `demon_porcupine_stingers` | Demon Porcupine's Stingers | starter | wiki-named spell of a character without a set | kept |
| `chimeras_roar` | Chimera's Roar | mid | wiki-named spell of a character without a set | kept |
| `chimera_fang_lunge` | Chimera Fang Lunge | mid | mod's own page, no anime spell of that name (kept) | kept |
| `terror_howl` | Terror Howl | mid | mod's own page, no anime spell of that name (kept) | kept |
| `horned_spirit_rush` | Horned Spirit Beast Rush | mid | mod's own page, no anime spell of that name (kept) | kept |
| `chimera_hunting_ground` | Chimera Hunting Ground | zone | mod's own page, no anime spell of that name (kept) | kept |
| `demon_beast_mantle` | Demon Beast Mantle | mid | mod's own page, no anime spell of that name (kept) | kept |
| `underworld_hide` | Underworld Hide | mid | mod's own page, no anime spell of that name (kept) | kept |
| `hydras_many_heads` | Hydra's Many Heads | signature | mod's own page, no anime spell of that name (kept) | kept |
| `hydras_regrowth` | Hydra's Regrowth | daily | wiki-named spell of a character without a set | kept |

### `book_body` (11 family pages, 10 shown to a basic grimoire)

| Page | Name | Tier | Basis | Status |
|---|---|---|---|---|
| `body_strike` | Titan Strike | starter | mod's own page, no anime spell of that name (kept) | kept (mode 0, never hidden) |
| `bullet_fist` | Bullet Fist | starter | mod's own page, no anime spell of that name (kept) | kept |
| `rapid_barrage` | Rapid Barrage | starter | mod's own page, no anime spell of that name (kept) | kept |
| `body_compression` | Body Compression | mid | Body Magic: Body Compression, Dante's | hidden: reserved for dante |
| `tendon_sever` | Tendon Sever | mid | mod's own page, no anime spell of that name (kept) | kept |
| `hardened_frame` | Hardened Frame | mid | mod's own page, no anime spell of that name (kept) | kept |
| `tissue_mend` | Tissue Mend | mid | mod's own page, no anime spell of that name (kept) | kept |
| `leg_spring` | Leg Spring | mid | mod's own page, no anime spell of that name (kept) | kept |
| `quake_stomp` | Quake Stomp | zone | mod's own page, no anime spell of that name (kept) | kept |
| `titan_form` | Titan Form | signature | mod's own page, no anime spell of that name (kept) | kept |
| `flesh_regrowth` | Flesh Regrowth | daily | mod's own page, no anime spell of that name (kept) | kept |

### `book_eye` (12 family pages, 11 shown to a basic grimoire)

| Page | Name | Tier | Basis | Status |
|---|---|---|---|---|
| `eye_strike` | Eye Strike | starter | mod's own page, no anime spell of that name (kept) | kept (mode 0, never hidden) |
| `piercing_glance` | Piercing Glance | starter | mod's own page, no anime spell of that name (kept) | kept |
| `seeker_eyes` | Seeker Eyes | mid | mod's own page, no anime spell of that name (kept) | kept |
| `mark_of_sight` | Mark of Sight | mid | mod's own page, no anime spell of that name (kept) | kept |
| `blinding_flash` | Blinding Flash | mid | mod's own page, no anime spell of that name (kept) | kept |
| `evil_eye` | Evil Eye | mid | wiki-named spell of a character without a set | kept |
| `keen_sight` | Keen Sight | mid | mod's own page, no anime spell of that name (kept) | kept |
| `iris_ward` | Iris Ward | mid | mod's own page, no anime spell of that name (kept) | kept |
| `sight_step` | Sight Step | mid | mod's own page, no anime spell of that name (kept) | kept |
| `field_of_eyes` | Field of Eyes | zone | mod's own page, no anime spell of that name (kept) | kept |
| `reflect_iris` | Union Magic: Reflect Iris | signature | Union Magic: Reflect Iris, Gauche's mirror array | hidden: reserved for gauche |
| `all_seeing_eye` | All-Seeing Eye | daily | mod's own page, no anime spell of that name (kept) | kept |

### `book_eyeball` (12 family pages, 12 shown to a basic grimoire)

| Page | Name | Tier | Basis | Status |
|---|---|---|---|---|
| `eyeball_strike` | Eyeball Strike | starter | mod's own page, no anime spell of that name (kept) | kept (mode 0, never hidden) |
| `sniper_eye` | Sniper Eye | starter | mod's own page, no anime spell of that name (kept) | kept |
| `roving_eyes` | Roving Eyes | mid | mod's own page, no anime spell of that name (kept) | kept |
| `petrifying_gaze` | Petrifying Gaze | mid | mod's own page, no anime spell of that name (kept) | kept |
| `magicule_scan` | Magicule Scan | mid | mod's own page, no anime spell of that name (kept) | kept |
| `eye_swap` | Eye Swap | mid | mod's own page, no anime spell of that name (kept) | kept |
| `sight_share` | Sight Share | mid | mod's own page, no anime spell of that name (kept) | kept |
| `guardian_eyes` | Guardian Eyes | mid | mod's own page, no anime spell of that name (kept) | kept |
| `scouting_eyes` | Scouting Eyes | mid | mod's own page, no anime spell of that name (kept) | kept |
| `jamming_gaze_field` | Jamming Gaze Field | zone | mod's own page, no anime spell of that name (kept) | kept |
| `catoblepas_s_evil_eye` | Catoblepas's Evil Eye | signature | wiki-named spell of a character without a set | kept |
| `swarm_of_eyes` | Swarm of Eyes | daily | mod's own page, no anime spell of that name (kept) | kept |

### `book_curse` (12 family pages, 8 shown to a basic grimoire)

| Page | Name | Tier | Basis | Status |
|---|---|---|---|---|
| `curse_strike` | Curse Strike | starter | mod's own page, no anime spell of that name (kept) | kept (mode 0, never hidden) |
| `aufwachen_dachs` | Aufwachen Dachs | starter | Poison Curse Magic: Aufwachen Dachs, Gordon's (also mode 0 of the Poison book) | hidden: reserved for gordon |
| `withering_hex` | Withering Hex | starter | mod's own page, no anime spell of that name (kept) | kept |
| `sekke_poison_lizard` | Sekke Poison Lizard | mid | wiki-named: Sekke Bronazza | kept |
| `ash_absorbing_formation` | Ash Absorbing Formation | mid | Ash Curse Magic: Ash Absorbing Formation, Zora's | hidden: reserved for zora |
| `joyful_destructive_ash` | Joyful Destructive Ash | mid | wiki-named: Catherine | kept |
| `curse_workers_neighbor` | Curse-Worker's Neighbor | mid | Curse-Worker's Neighbor, Gordon's | hidden: reserved for gordon |
| `hex_aegis` | Hex Aegis | mid | mod's own page, no anime spell of that name (kept) | kept |
| `cursed_step` | Cursed Step | mid | mod's own page, no anime spell of that name (kept) | kept |
| `dwelling_poison_cloud` | Dwelling of the Poison Cloud | zone | wiki-named spell of a character without a set | kept |
| `ultra_giant_bull` | Ultra Giant Bull | signature | Compound Magic: Ultra Giant Bull, Henry's (The Raging Black Bull) | hidden: reserved for henry |
| `curse_epidemic` | Curse Epidemic | daily | mod's own page, no anime spell of that name (kept) | kept |

### `book_curse_warding` (12 family pages, 11 shown to a basic grimoire)

| Page | Name | Tier | Basis | Status |
|---|---|---|---|---|
| `curse_warding_strike` | Ward Strike | starter | mod's own page, no anime spell of that name (kept) | kept (mode 0, never hidden) |
| `hex_eater` | Hex Eater | starter | mod's own page, no anime spell of that name (kept) | kept |
| `curse_cleanse` | Curse Cleanse | mid | mod's own page, no anime spell of that name (kept) | kept |
| `rune_step` | Rune Step | mid | mod's own page, no anime spell of that name (kept) | kept |
| `malevolent_femcantation` | Malevolent Femcantation | mid | wiki-named spell of a character without a set | kept |
| `curse_transfer` | Curse Transfer | mid | mod's own page, no anime spell of that name (kept) | kept |
| `mirror_ward` | Mirror Ward | mid | mod's own page, no anime spell of that name (kept) | kept |
| `unbound_slave` | Unbound Slave | mid | wiki-named spell of a character without a set | kept |
| `ward_circle` | Ward Circle | zone | mod's own page, no anime spell of that name (kept) | kept |
| `decaying_world` | Decaying World | zone | Curse-Warding Magic: Decaying World, Megicula's and Vanica's | hidden: reserved for vanica |
| `exploding_life` | Exploding Life | signature | wiki-named spell of a character without a set | kept |
| `undying_curse` | Undying Curse | daily | mod's own page, no anime spell of that name (kept) | kept |

### `book_demon_fire` (12 family pages, 12 shown to a basic grimoire)

| Page | Name | Tier | Basis | Status |
|---|---|---|---|---|
| `demon_fire_strike` | Demon Flame Strike | starter | mod's own page, no anime spell of that name (kept) | kept (mode 0, never hidden) |
| `black_flame_bullets` | Black Flame Bullets | starter | mod's own page, no anime spell of that name (kept) | kept |
| `soul_scorch` | Soul Scorch | mid | mod's own page, no anime spell of that name (kept) | kept |
| `ignition_blast` | Ignition Blast | mid | mod's own page, no anime spell of that name (kept) | kept |
| `black_flame_pillar` | Pillar of Black Flame | mid | mod's own page, no anime spell of that name (kept) | kept |
| `hellfire_wave` | Hellfire Wave | mid | mod's own page, no anime spell of that name (kept) | kept |
| `flame_step` | Flame Step | mid | mod's own page, no anime spell of that name (kept) | kept |
| `flame_body` | Flame Body | mid | mod's own page, no anime spell of that name (kept) | kept |
| `black_flame_twister` | Black Flame Twister | zone | mod's own page, no anime spell of that name (kept) | kept |
| `black_flame_veil` | Veil of Black Flame | zone | mod's own page, no anime spell of that name (kept) | kept |
| `demon_fire_apocalypse` | Demon Fire Apocalypse | signature | mod's own page, no anime spell of that name (kept) | kept |
| `cinder_rebirth` | Cinder Rebirth | daily | mod's own page, no anime spell of that name (kept) | kept |

### `book_demon_ice` (12 family pages, 12 shown to a basic grimoire)

| Page | Name | Tier | Basis | Status |
|---|---|---|---|---|
| `demon_ice_strike` | Demon Frost Strike | starter | mod's own page, no anime spell of that name (kept) | kept (mode 0, never hidden) |
| `demon_ice_shards` | Demon Ice: Frost Shards | starter | mod's own page, no anime spell of that name (kept) | kept |
| `demon_ice_lance` | Demon Ice: Piercing Lance | starter | mod's own page, no anime spell of that name (kept) | kept |
| `demon_ice_wave` | Demon Ice: Frostbite Wave | mid | mod's own page, no anime spell of that name (kept) | kept |
| `demon_ice_soul` | Demon Ice: Frozen Soul | mid | mod's own page, no anime spell of that name (kept) | kept |
| `demon_ice_shatter` | Demon Ice: Shatter | mid | mod's own page, no anime spell of that name (kept) | kept |
| `demon_ice_wall` | Demon Ice: Glacier Wall | mid | mod's own page, no anime spell of that name (kept) | kept |
| `demon_ice_slide` | Demon Ice: Glacier Slide | mid | mod's own page, no anime spell of that name (kept) | kept |
| `demon_ice_body` | Demon Ice: Frost Body | mid | mod's own page, no anime spell of that name (kept) | kept |
| `demon_ice_blizzard` | Demon Ice: Blizzard | zone | mod's own page, no anime spell of that name (kept) | kept |
| `demon_ice_zero` | Demon Ice: Absolute Zero | signature | mod's own page, no anime spell of that name (kept) | kept |
| `demon_ice_winter` | Demon Ice: Eternal Winter | daily | mod's own page, no anime spell of that name (kept) | kept |

### `book_demon_light` (12 family pages, 12 shown to a basic grimoire)

| Page | Name | Tier | Basis | Status |
|---|---|---|---|---|
| `demon_light_strike` | Demon Light Strike | starter | mod's own page, no anime spell of that name (kept) | kept (mode 0, never hidden) |
| `light_sword_of_judgment` | Demon Light Magic: Light Sword of Judgment | starter | wiki-named spell of a character without a set | kept |
| `blinding_flash` | Demon Light Magic: Blinding Flash | starter | mod's own page, no anime spell of that name (kept) | kept |
| `bright_judgment_whip` | Demon Light Magic: Bright Judgment Whip | starter | wiki-named spell of a character without a set | kept |
| `prism_breaker` | Demon Light Magic: Prism Breaker | mid | mod's own page, no anime spell of that name (kept) | kept |
| `purging_light` | Demon Light Magic: Purging Light | mid | mod's own page, no anime spell of that name (kept) | kept |
| `light_prison` | Demon Light Magic: Light Prison | mid | mod's own page, no anime spell of that name (kept) | kept |
| `light_step` | Demon Light Magic: Light Step | mid | mod's own page, no anime spell of that name (kept) | kept |
| `veil_of_black_light` | Demon Light Magic: Veil of Black Light | mid | mod's own page, no anime spell of that name (kept) | kept |
| `eclipse_domain` | Demon Light Magic: Eclipse Domain | zone | mod's own page, no anime spell of that name (kept) | kept |
| `light_shaft_of_divine_punishment` | Demon Light Magic: Light Shaft of Divine Punishment | signature | wiki-named spell of a character without a set | kept |
| `demon_dawn` | Demon Light Magic: Dawn of the Demon Light | daily | mod's own page, no anime spell of that name (kept) | kept |

### `book_demon_water` (10 family pages, 10 shown to a basic grimoire)

| Page | Name | Tier | Basis | Status |
|---|---|---|---|---|
| `demon_water_strike` | Demon Tide Strike | starter | mod's own page, no anime spell of that name (kept) | kept (mode 0, never hidden) |
| `tidal_push` | Demon Water Magic: Tidal Push | starter | mod's own page, no anime spell of that name (kept) | kept |
| `dark_tide_wave` | Demon Water Magic: Dark Tide Wave | starter | mod's own page, no anime spell of that name (kept) | kept |
| `drowning_grasp` | Demon Water Magic: Drowning Grasp | mid | mod's own page, no anime spell of that name (kept) | kept |
| `healing_mist` | Demon Water Magic: Healing Mist | mid | mod's own page, no anime spell of that name (kept) | kept |
| `serpent_surge` | Demon Water Magic: Serpent Surge | mid | mod's own page, no anime spell of that name (kept) | kept |
| `abyssal_ward` | Demon Water Magic: Abyssal Ward | mid | mod's own page, no anime spell of that name (kept) | kept |
| `abyssal_whirlpool` | Demon Water Magic: Abyssal Whirlpool | zone | mod's own page, no anime spell of that name (kept) | kept |
| `hydra_of_darkneros` | Demon Water Magic: Hydra of Darkneros | signature | wiki-named spell of a character without a set | kept |
| `tide_rebirth` | Demon Water Magic: Tide Rebirth | daily | mod's own page, no anime spell of that name (kept) | kept |

### `book_barrier` (12 family pages, 12 shown to a basic grimoire)

| Page | Name | Tier | Basis | Status |
|---|---|---|---|---|
| `barrier_strike` | Barrier Strike | starter | mod's own page, no anime spell of that name (kept) | kept (mode 0, never hidden) |
| `hex_shards` | Hex Shards | starter | mod's own page, no anime spell of that name (kept) | kept |
| `barrier_step` | Barrier Step | starter | mod's own page, no anime spell of that name (kept) | kept |
| `voodoo_regalia` | Voodoo Regalia | mid | wiki-named spell of a character without a set | kept |
| `barrier_cannon` | Barrier Cannon | mid | mod's own page, no anime spell of that name (kept) | kept |
| `hexagon_wall` | Hexagon Wall | mid | mod's own page, no anime spell of that name (kept) | kept |
| `deflecting_pane` | Deflecting Pane | mid | mod's own page, no anime spell of that name (kept) | kept |
| `barrier_armour` | Barrier Armour | mid | mod's own page, no anime spell of that name (kept) | kept |
| `spatial_lock` | Spatial Lock | mid | mod's own page, no anime spell of that name (kept) | kept |
| `voodoo_shangrila` | Voodoo Shangrila | zone | wiki-named spell of a character without a set | kept |
| `voodoo_kingdom` | Voodoo Kingdom | signature | wiki-named spell of a character without a set | kept |
| `barrier_reclaim` | Barrier Reclaim | daily | mod's own page, no anime spell of that name (kept) | kept |

### `book_key` (14 family pages, 14 shown to a basic grimoire)

| Page | Name | Tier | Basis | Status |
|---|---|---|---|---|
| `key_strike` | Key Strike | starter | mod's own page, no anime spell of that name (kept) | kept (mode 0, never hidden) |
| `skeleton_keys` | Skeleton Keys | starter | mod's own page, no anime spell of that name (kept) | kept |
| `janus_baptism` | Janus Baptism | mid | wiki-named spell of a character without a set | kept |
| `keyhole_prison` | Keyhole Prison | mid | mod's own page, no anime spell of that name (kept) | kept |
| `door_step` | Door Step | mid | mod's own page, no anime spell of that name (kept) | kept |
| `master_key` | Master Key | mid | mod's own page, no anime spell of that name (kept) | kept |
| `bolted_door` | Bolted Door | mid | mod's own page, no anime spell of that name (kept) | kept |
| `unlock` | Unlock | mid | mod's own page, no anime spell of that name (kept) | kept |
| `locked_domain` | Locked Domain | zone | mod's own page, no anime spell of that name (kept) | kept |
| `key_armory` | Key Armory | zone | mod's own page, no anime spell of that name (kept) | kept |
| `janus_abigail` | Janus Abigail | signature | wiki-named spell of a character without a set | kept |
| `doom_s_gate` | Doom's Gate | daily | wiki-named spell of a character without a set | kept |
| `gate_of_keys` | Gate of Keys | zone | mod's own page, no anime spell of that name (kept) | kept |
| `key_guard` | Key Guard | zone | mod's own page, no anime spell of that name (kept) | kept |

### `book_chain` (12 family pages, 12 shown to a basic grimoire)

| Page | Name | Tier | Basis | Status |
|---|---|---|---|---|
| `chain_strike` | Chain Strike | starter | mod's own page, no anime spell of that name (kept) | kept (mode 0, never hidden) |
| `chain_whip` | Chain Whip | starter | mod's own page, no anime spell of that name (kept) | kept |
| `scorpio_chain` | Scorpio Chain | starter | wiki-named spell of a character without a set | kept |
| `pitless_viper` | Dance of the Pitless Viper | mid | wiki-named spell of a character without a set | kept |
| `kunai_chains` | Kunai Chains | mid | mod's own page, no anime spell of that name (kept) | kept |
| `heavy_chain` | Heavy Chain | mid | wiki-named spell of a character without a set | kept |
| `grapple_chain` | Grapple Chain | mid | mod's own page, no anime spell of that name (kept) | kept |
| `chain_aegis` | Chain Aegis | mid | mod's own page, no anime spell of that name (kept) | kept |
| `chain_armament` | Chain Armament | mid | mod's own page, no anime spell of that name (kept) | kept |
| `chain_cage` | Chain Cage | zone | mod's own page, no anime spell of that name (kept) | kept |
| `iron_chain_formation` | Magic Binding Iron Chain Formation | signature | wiki-named spell of a character without a set | kept |
| `chain_reckoning` | Chain Reckoning | daily | mod's own page, no anime spell of that name (kept) | kept |

### `book_butoh` (13 family pages, 9 shown to a basic grimoire)

| Page | Name | Tier | Basis | Status |
|---|---|---|---|---|
| `butoh_strike` | Dance Strike | starter | attribute basic (also in the set of kiato, not exclusive) | kept (mode 0, never hidden) |
| `butoh_flowing_blade` | Flowing Water Blade | starter | mod's own page, no anime spell of that name (kept) | kept |
| `butoh_rhythm_wave` | Rhythm Wave | mid | mod's own page, no anime spell of that name (kept) | kept |
| `butoh_sea_god_slash` | Union Magic: Sea God Slash | mid | character signature | hidden: exclusive to kiato (set) |
| `butoh_ebb_step` | Ebb Step | mid | mod's own page, no anime spell of that name (kept) | kept |
| `butoh_trance_dance` | Trance Dance | mid | attribute basic (also in the set of kiato, not exclusive) | kept |
| `butoh_tidal_veil` | Tidal Veil | mid | mod's own page, no anime spell of that name (kept) | kept |
| `butoh_undertow` | Undertow | zone | mod's own page, no anime spell of that name (kept) | kept |
| `butoh_metronome_field` | Metronome Field | zone | mod's own page, no anime spell of that name (kept) | kept |
| `butoh_sea_god_descent` | Sea God's Descent | signature | character signature | hidden: exclusive to kiato (set) |
| `butoh_seabed_hymn` | Seabed Hymn | daily | mod's own page, no anime spell of that name (kept) | kept |
| `butoh_dance` | Butoh Dance | custom | character signature | hidden: exclusive to kiato (set) |
| `butoh_dash_deflect` | Deflect Dash | mid | character signature | hidden: exclusive to kiato (set) |

### `book_briar` (13 family pages, 9 shown to a basic grimoire)

| Page | Name | Tier | Basis | Status |
|---|---|---|---|---|
| `briar_strike` | Briar Strike | starter | attribute basic (also in the set of charlotte, not exclusive) | kept (mode 0, never hidden) |
| `thorn_lash` | Thorn Lash | starter | attribute basic (also in the set of charlotte, not exclusive) | kept |
| `creeping_vines` | Creeping Vines | starter | attribute basic (also in the set of charlotte, not exclusive) | kept |
| `bleeding_thorn_wall` | Bleeding Thorn Wall | mid | mod's own page, no anime spell of that name (kept) | kept |
| `rose_scent` | Rose Scent | mid | attribute basic (also in the set of charlotte, not exclusive) | kept |
| `thorn_reflect` | Thorn Reflect | mid | mod's own page, no anime spell of that name (kept) | kept |
| `briar_leap` | Briar Leap | mid | mod's own page, no anime spell of that name (kept) | kept |
| `briar_garden` | Briar Garden | zone | mod's own page, no anime spell of that name (kept) | kept |
| `corpse_hunting_briar_tree` | Briar Creation Magic: Corpse-Hunting Briar Tree | signature | character signature | hidden: exclusive to charlotte (set) |
| `queen_of_briars` | True Briar Magic: Queen of Briars | signature | character signature | hidden: exclusive to charlotte (set) |
| `rose_garden_bloom` | Rose Garden Bloom | daily | mod's own page, no anime spell of that name (kept) | kept |
| `blue_rose_paradise` | Briar Magic: Blue Rose Paradise | zone | character signature | hidden: exclusive to charlotte (set) |
| `scarlet_braided_spear` | Briar Magic: Scarlet-Braided Beautiful Battle Spear | mid | character signature | hidden: exclusive to charlotte (set) |

### `book_cherry_blossom` (12 family pages, 9 shown to a basic grimoire)

| Page | Name | Tier | Basis | Status |
|---|---|---|---|---|
| `cherry_blossom_strike` | Sakura Strike | starter | attribute basic (also in the set of kirsch, not exclusive) | kept (mode 0, never hidden) |
| `razor_petal_stream` | Cherry Blossom: Razor Petal Stream | starter | mod's own page, no anime spell of that name (kept) | kept |
| `petal_fan` | Cherry Blossom: Petal Fan | mid | mod's own page, no anime spell of that name (kept) | kept |
| `blossom_snare` | Cherry Blossom: Blossom Snare | mid | mod's own page, no anime spell of that name (kept) | kept |
| `petal_veil` | Cherry Blossom: Petal Veil | mid | attribute basic (also in the set of kirsch, not exclusive) | kept |
| `petal_step` | Cherry Blossom: Petal Step | mid | attribute basic (also in the set of kirsch, not exclusive) | kept |
| `beautifying_bloom` | Cherry Blossom: Beautifying Bloom | mid | mod's own page, no anime spell of that name (kept) | kept |
| `blossom_blessing` | Cherry Blossom: Blossom Blessing | mid | mod's own page, no anime spell of that name (kept) | kept |
| `clones_of_the_beautiful_me` | Cherry Blossom Magic: Clones of the Beautiful Me | mid | character signature | hidden: exclusive to kirsch (set) |
| `magic_cherry_blossom_blizzard` | Cherry Blossom Magic: Magic Cherry Blossom Blizzard | zone | character signature | hidden: exclusive to kirsch (set) |
| `dance_of_100_million_cherry_blossoms` | Cherry Blossom Magic: Dance of 100 Million Cherry Blossoms | signature | Cherry Blossom Magic: Dance of 100 Million Cherry Blossoms, Kirsch's ultimate | hidden: reserved for kirsch |
| `everlasting_sakura_grove` | Cherry Blossom: Everlasting Sakura Grove | daily | mod's own page, no anime spell of that name (kept) | kept |

### `book_fungus` (12 family pages, 12 shown to a basic grimoire)

| Page | Name | Tier | Basis | Status |
|---|---|---|---|---|
| `fungus_strike` | Spore Strike | starter | mod's own page, no anime spell of that name (kept) | kept (mode 0, never hidden) |
| `spore_burst` | Spore Burst | starter | mod's own page, no anime spell of that name (kept) | kept |
| `mycelium_roots` | Mycelium Roots | starter | mod's own page, no anime spell of that name (kept) | kept |
| `heavy_mr_mushroom` | Fungus Magic: Heavy Mr. Mushroom | mid | wiki-named spell of a character without a set | kept |
| `running_mr_mushroom` | Fungus Magic: Running Mr. Mushroom | mid | wiki-named spell of a character without a set | kept |
| `talking_mr_mushroom` | Fungus Magic: Talking Mr. Mushroom | mid | wiki-named spell of a character without a set | kept |
| `spore_armor` | Spore Armor | mid | mod's own page, no anime spell of that name (kept) | kept |
| `mushroom_wall` | Mushroom Wall | mid | mod's own page, no anime spell of that name (kept) | kept |
| `mycelium_mending` | Mycelium Mending | mid | mod's own page, no anime spell of that name (kept) | kept |
| `spore_cloud` | Spore Cloud | zone | mod's own page, no anime spell of that name (kept) | kept |
| `towering_mr_mushroom` | Fungus Magic: Towering Mr. Mushroom | signature | wiki-named spell of a character without a set | kept |
| `mycelium_bloom` | Mycelium Bloom | daily | mod's own page, no anime spell of that name (kept) | kept |

### `book_food` (12 family pages, 11 shown to a basic grimoire)

| Page | Name | Tier | Basis | Status |
|---|---|---|---|---|
| `food_strike` | Feast Strike | starter | mod's own page, no anime spell of that name (kept) | kept (mode 0, never hidden) |
| `fork_skewer` | Giant Fork | starter | mod's own page, no anime spell of that name (kept) | kept |
| `carving_cleave` | Carving Knife | starter | mod's own page, no anime spell of that name (kept) | kept |
| `pie_barrage` | Pie Barrage | mid | mod's own page, no anime spell of that name (kept) | kept |
| `rotten_feast` | Rotten Feast | mid | mod's own page, no anime spell of that name (kept) | kept |
| `starving_rush` | Starving Rush | mid | mod's own page, no anime spell of that name (kept) | kept |
| `hearty_meal` | Hearty Meal | mid | mod's own page, no anime spell of that name (kept) | kept |
| `full_stomach` | Full Stomach | mid | mod's own page, no anime spell of that name (kept) | kept |
| `silver_platter` | Silver Platter | mid | mod's own page, no anime spell of that name (kept) | kept |
| `banquet_table` | Banquet Table | zone | mod's own page, no anime spell of that name (kept) | kept |
| `glutton_s_banquet` | Glutton's Banquet | signature | Food Magic: Glutton's Banquet, Charmy's | hidden: reserved for charmy |
| `midnight_feast` | Midnight Feast | daily | mod's own page, no anime spell of that name (kept) | kept |

### `book_crystal` (12 family pages, 12 shown to a basic grimoire)

| Page | Name | Tier | Basis | Status |
|---|---|---|---|---|
| `crystal_strike` | Crystal Strike | starter | mod's own page, no anime spell of that name (kept) | kept (mode 0, never hidden) |
| `crystal_shards` | Crystal Shards | starter | mod's own page, no anime spell of that name (kept) | kept |
| `prism_beam` | Prism Beam | mid | mod's own page, no anime spell of that name (kept) | kept |
| `shatter_burst` | Shatter Burst | mid | mod's own page, no anime spell of that name (kept) | kept |
| `crystal_cage` | Crystal Cage | mid | mod's own page, no anime spell of that name (kept) | kept |
| `prism_step` | Prism Step | mid | mod's own page, no anime spell of that name (kept) | kept |
| `crystal_scope` | Crystal Magic: Crystal Scope | mid | wiki-named spell of a character without a set | kept |
| `crystal_armor` | Crystal Armor | mid | mod's own page, no anime spell of that name (kept) | kept |
| `crystal_garden` | Crystal Garden | zone | mod's own page, no anime spell of that name (kept) | kept |
| `prism_dome` | Prism Dome | zone | mod's own page, no anime spell of that name (kept) | kept |
| `grand_prism` | Grand Prism Cannon | signature | mod's own page, no anime spell of that name (kept) | kept |
| `crystal_resonance` | Crystal Resonance | daily | mod's own page, no anime spell of that name (kept) | kept |

### `book_corundum` (13 family pages, 13 shown to a basic grimoire)

| Page | Name | Tier | Basis | Status |
|---|---|---|---|---|
| `corundum_strike` | Corundum Strike | starter | mod's own page, no anime spell of that name (kept) | kept (mode 0, never hidden) |
| `ruby_shards` | Ruby Shards | starter | mod's own page, no anime spell of that name (kept) | kept |
| `sapphire_lance` | Sapphire Lance | starter | mod's own page, no anime spell of that name (kept) | kept |
| `corundum_fracture` | Corundum Fracture | mid | mod's own page, no anime spell of that name (kept) | kept |
| `hard_blast` | Hard Blast | mid | mod's own page, no anime spell of that name (kept) | kept |
| `corundum_rush` | Corundum Rush | mid | mod's own page, no anime spell of that name (kept) | kept |
| `gem_wall` | Gem Wall | mid | mod's own page, no anime spell of that name (kept) | kept |
| `gem_armour` | Gem Armour | mid | mod's own page, no anime spell of that name (kept) | kept |
| `corundum_bastion` | Corundum Bastion | zone | mod's own page, no anime spell of that name (kept) | kept |
| `ideal_closer` | Ideal Closer | signature | wiki-named spell of a character without a set | kept |
| `star_corundum` | Star Corundum | daily | mod's own page, no anime spell of that name (kept) | kept |
| `ideal_closer_fist` | Ideal Closer Fist | mid | wiki-named spell of a character without a set | kept |
| `gem_plate_armour` | Gem Plate Armour | signature | mod's own page, no anime spell of that name (kept) | kept |

### `book_bronze` (14 family pages, 14 shown to a basic grimoire)

| Page | Name | Tier | Basis | Status |
|---|---|---|---|---|
| `bronze_strike` | Bronze Strike | starter | mod's own page, no anime spell of that name (kept) | kept (mode 0, never hidden) |
| `verdigris_spit` | Verdigris Spit | starter | mod's own page, no anime spell of that name (kept) | kept |
| `bronze_shrapnel` | Bronze Shrapnel | starter | mod's own page, no anime spell of that name (kept) | kept |
| `sekke_poison_lizard` | Bronze Curse Magic: Sekke Poison Lizard | mid | wiki-named: Sekke Bronazza | kept |
| `sekke_magnum_cannonball` | Bronze Creation Magic: Sekke Magnum Cannonball | mid | wiki-named: Sekke Bronazza | kept |
| `bronze_bulwark` | Bronze Bulwark | mid | mod's own page, no anime spell of that name (kept) | kept |
| `bronze_statue` | Bronze Statue | mid | mod's own page, no anime spell of that name (kept) | kept |
| `sekke_shooting_star` | Bronze Creation Magic: Sekke Shooting Star | mid | wiki-named: Sekke Bronazza | kept |
| `statue_garrison` | Statue Garrison | zone | mod's own page, no anime spell of that name (kept) | kept |
| `molten_bronze` | Molten Bronze | zone | mod's own page, no anime spell of that name (kept) | kept |
| `super_sekke_magnum_cannonball` | Super Sekke Magnum Cannonball | signature | wiki-named: Sekke Bronazza | kept |
| `bronze_colossus` | Bronze Colossus | daily | mod's own page, no anime spell of that name (kept) | kept |
| `bronze_guardian` | Bronze Guardian | mid | mod's own page, no anime spell of that name (kept) | kept |
| `bronze_armour` | Bronze Armour | signature | mod's own page, no anime spell of that name (kept) | kept |

### `book_copper` (12 family pages, 12 shown to a basic grimoire)

| Page | Name | Tier | Basis | Status |
|---|---|---|---|---|
| `copper_strike` | Copper Strike | starter | mod's own page, no anime spell of that name (kept) | kept (mode 0, never hidden) |
| `conduction_arc` | Conduction Arc | starter | mod's own page, no anime spell of that name (kept) | kept |
| `red_shine_mace` | Red Shine Mace | mid | wiki-named spell of a character without a set | kept |
| `copper_wire_bind` | Copper Wire Bind | mid | mod's own page, no anime spell of that name (kept) | kept |
| `verdigris_blight` | Verdigris Blight | mid | mod's own page, no anime spell of that name (kept) | kept |
| `rail_dash` | Rail Dash | mid | mod's own page, no anime spell of that name (kept) | kept |
| `galvanic_edge` | Galvanic Edge | mid | mod's own page, no anime spell of that name (kept) | kept |
| `copper_plating` | Copper Plating | mid | mod's own page, no anime spell of that name (kept) | kept |
| `copper_bulwark` | Copper Bulwark | mid | mod's own page, no anime spell of that name (kept) | kept |
| `conduction_field` | Conduction Field | zone | mod's own page, no anime spell of that name (kept) | kept |
| `copper_tempest` | Copper Tempest | signature | mod's own page, no anime spell of that name (kept) | kept |
| `plating_restoration` | Plating Restoration | daily | mod's own page, no anime spell of that name (kept) | kept |

### `book_iron` (12 family pages, 12 shown to a basic grimoire)

| Page | Name | Tier | Basis | Status |
|---|---|---|---|---|
| `iron_strike` | Iron Strike | starter | mod's own page, no anime spell of that name (kept) | kept (mode 0, never hidden) |
| `iron_bullet` | Iron Bullet | starter | wiki-named spell of a character without a set | kept |
| `iron_shrapnel` | Iron Shrapnel | starter | mod's own page, no anime spell of that name (kept) | kept |
| `iron_sword_rain` | Iron Sword Rain | mid | mod's own page, no anime spell of that name (kept) | kept |
| `iron_shackles` | Iron Shackles | mid | mod's own page, no anime spell of that name (kept) | kept |
| `iron_charge` | Iron Charge | mid | mod's own page, no anime spell of that name (kept) | kept |
| `iron_fist` | Iron Fist | mid | mod's own page, no anime spell of that name (kept) | kept |
| `iron_wall` | Iron Wall | mid | mod's own page, no anime spell of that name (kept) | kept |
| `immovable_guardian_deity` | Immovable Guardian Deity | mid | wiki-named spell of a character without a set | kept |
| `rust_field` | Rust Field | zone | mod's own page, no anime spell of that name (kept) | kept |
| `iron_god_of_war` | Iron God of War | signature | wiki-named spell of a character without a set | kept |
| `iron_anvil` | Colossus Anvil | daily | mod's own page, no anime spell of that name (kept) | kept |

### `book_black_oil` (12 family pages, 12 shown to a basic grimoire)

| Page | Name | Tier | Basis | Status |
|---|---|---|---|---|
| `black_oil_strike` | Black Oil Strike | starter | mod's own page, no anime spell of that name (kept) | kept (mode 0, never hidden) |
| `sticky_tar` | Sticky Tar | starter | mod's own page, no anime spell of that name (kept) | kept |
| `curse_filled_nails` | Black Oil Creation Magic: Curse-Filled Nails | mid | wiki-named spell of a character without a set | kept |
| `oil_slick` | Oil Slick | mid | mod's own page, no anime spell of that name (kept) | kept |
| `ignite_oil` | Ignite Oil | mid | mod's own page, no anime spell of that name (kept) | kept |
| `oil_slide` | Oil Slide | mid | mod's own page, no anime spell of that name (kept) | kept |
| `oil_soaked_arms` | Oil-Soaked Arms | mid | mod's own page, no anime spell of that name (kept) | kept |
| `viscous_coat` | Viscous Coat | mid | mod's own page, no anime spell of that name (kept) | kept |
| `tar_pit` | Tar Pit | zone | mod's own page, no anime spell of that name (kept) | kept |
| `curse_candle_ritual_disk` | Black Oil Magic: Curse Candle Ritual Disk | zone | wiki-named spell of a character without a set | kept |
| `black_oil_eruption` | Black Oil Eruption | signature | mod's own page, no anime spell of that name (kept) | kept |
| `black_oil_sea` | Black Oil Sea | daily | mod's own page, no anime spell of that name (kept) | kept |

### `book_gel` (12 family pages, 12 shown to a basic grimoire)

| Page | Name | Tier | Basis | Status |
|---|---|---|---|---|
| `gel_strike` | Gel Strike | starter | mod's own page, no anime spell of that name (kept) | kept (mode 0, never hidden) |
| `sticky_gel` | Sticky Gel | starter | mod's own page, no anime spell of that name (kept) | kept |
| `gel_burst` | Gel Burst | mid | mod's own page, no anime spell of that name (kept) | kept |
| `bouncy_gel_pad` | Bouncy Gel Pad | mid | mod's own page, no anime spell of that name (kept) | kept |
| `gel_smother` | Gel Smother | mid | mod's own page, no anime spell of that name (kept) | kept |
| `gel_antidote` | Gel Antidote | mid | mod's own page, no anime spell of that name (kept) | kept |
| `absorbing_gel` | Absorbing Gel | mid | mod's own page, no anime spell of that name (kept) | kept |
| `gel_skin` | Gel Skin | mid | mod's own page, no anime spell of that name (kept) | kept |
| `gel_mire` | Gel Mire | zone | mod's own page, no anime spell of that name (kept) | kept |
| `sticky_salamander` | Gel Magic: Sticky Salamander | zone | wiki-named spell of a character without a set | kept |
| `huge_sticky_salamander` | Gel Magic: Huge Sticky Salamander | signature | wiki-named spell of a character without a set | kept |
| `jelly_cocoon` | Jelly Cocoon | daily | mod's own page, no anime spell of that name (kept) | kept |

### `book_glass` (12 family pages, 12 shown to a basic grimoire)

| Page | Name | Tier | Basis | Status |
|---|---|---|---|---|
| `glass_strike` | Glass Strike | starter | mod's own page, no anime spell of that name (kept) | kept (mode 0, never hidden) |
| `shard_volley` | Shard Volley | starter | mod's own page, no anime spell of that name (kept) | kept |
| `glass_lance` | Glass Lance | starter | mod's own page, no anime spell of that name (kept) | kept |
| `verre_epee` | Glass Magic: Verre Epee | mid | mod's own page, no anime spell of that name (kept) | kept |
| `pane_step` | Pane Step | mid | mod's own page, no anime spell of that name (kept) | kept |
| `verre_detection` | Glass Magic: Verre Detection | mid | mod's own page, no anime spell of that name (kept) | kept |
| `prism_barrier` | Prism Barrier | mid | mod's own page, no anime spell of that name (kept) | kept |
| `glass_shatter` | Glass Shatter | mid | mod's own page, no anime spell of that name (kept) | kept |
| `shard_rain` | Shard Rain | zone | mod's own page, no anime spell of that name (kept) | kept |
| `verre_fleur` | Glass Creation Magic: Verre Fleur | zone | wiki-named spell of a character without a set | kept |
| `le_chateau_de_verre` | Union Magic: Le Chateau de Verre | signature | mod's own page, no anime spell of that name (kept) | kept |
| `crystal_carapace` | Crystal Carapace | daily | mod's own page, no anime spell of that name (kept) | kept |

### `book_bubble` (12 family pages, 12 shown to a basic grimoire)

| Page | Name | Tier | Basis | Status |
|---|---|---|---|---|
| `bubble_strike` | Bubble Strike | starter | mod's own page, no anime spell of that name (kept) | kept (mode 0, never hidden) |
| `bubble_barrage` | Bubble Barrage | starter | mod's own page, no anime spell of that name (kept) | kept |
| `foam_blind` | Foam Blind | mid | mod's own page, no anime spell of that name (kept) | kept |
| `bubble_trap` | Bubble Trap | mid | mod's own page, no anime spell of that name (kept) | kept |
| `bubble_burst` | Bubble Burst | mid | mod's own page, no anime spell of that name (kept) | kept |
| `bubble_ride` | Bubble Ride | mid | mod's own page, no anime spell of that name (kept) | kept |
| `bubble_barrier` | Bubble Barrier | mid | mod's own page, no anime spell of that name (kept) | kept |
| `bubble_skin` | Bubble Skin | mid | mod's own page, no anime spell of that name (kept) | kept |
| `bubble_refresher` | Bubble Healing Magic: Bubble Refresher | zone | wiki-named spell of a character without a set | kept |
| `bubble_mines` | Bubble Mines | zone | mod's own page, no anime spell of that name (kept) | kept |
| `great_bubble` | Great Bubble | signature | mod's own page, no anime spell of that name (kept) | kept |
| `rainbow_cocoon` | Rainbow Cocoon | daily | mod's own page, no anime spell of that name (kept) | kept |

### `book_ice_wedge` (12 family pages, 12 shown to a basic grimoire)

| Page | Name | Tier | Basis | Status |
|---|---|---|---|---|
| `ice_wedge_strike` | Wedge Strike | starter | mod's own page, no anime spell of that name (kept) | kept (mode 0, never hidden) |
| `wedge_volley` | Ice Wedge: Wedge Volley | starter | mod's own page, no anime spell of that name (kept) | kept |
| `wedge_driver` | Ice Wedge: Wedge Driver | starter | mod's own page, no anime spell of that name (kept) | kept |
| `wedge_fan` | Ice Wedge: Wedge Fan | mid | mod's own page, no anime spell of that name (kept) | kept |
| `splitting_wedge` | Ice Wedge: Splitting Wedge | mid | mod's own page, no anime spell of that name (kept) | kept |
| `frost_brand` | Ice Wedge: Frost Brand | mid | mod's own page, no anime spell of that name (kept) | kept |
| `wedge_aegis` | Ice Wedge: Wedge Aegis | mid | mod's own page, no anime spell of that name (kept) | kept |
| `wedge_wall` | Ice Wedge: Wedge Wall | mid | mod's own page, no anime spell of that name (kept) | kept |
| `wedge_step` | Ice Wedge: Wedge Step | mid | mod's own page, no anime spell of that name (kept) | kept |
| `wedge_field` | Ice Wedge: Wedge Field | zone | mod's own page, no anime spell of that name (kept) | kept |
| `world_end_martyr` | Ice Wedge Magic: World End Martyr | signature | wiki-named spell of a character without a set | kept |
| `world_end_church` | Ice Wedge Magic: World End Church | daily | wiki-named spell of a character without a set | kept |

### `book_legion` (11 family pages, 11 shown to a basic grimoire)

| Page | Name | Tier | Basis | Status |
|---|---|---|---|---|
| `legion_strike` | Legion Strike | starter | mod's own page, no anime spell of that name (kept) | kept (mode 0, never hidden) |
| `pawn_march` | Pawn March | starter | mod's own page, no anime spell of that name (kept) | kept |
| `muster_soldiers` | Muster Soldiers | mid | mod's own page, no anime spell of that name (kept) | kept |
| `knight_gambit` | Knight Gambit | mid | mod's own page, no anime spell of that name (kept) | kept |
| `soldier_snare` | Soldier Snare | mid | mod's own page, no anime spell of that name (kept) | kept |
| `commanders_banner` | Commander's Banner | mid | mod's own page, no anime spell of that name (kept) | kept |
| `rook_barricade` | Rook Barricade | mid | mod's own page, no anime spell of that name (kept) | kept |
| `blink_castling` | Blink Castling | mid | wiki-named spell of a character without a set | kept |
| `gehenna_game` | Gehenna Game | zone | wiki-named spell of a character without a set | kept |
| `endless_domination` | Endless Domination | signature | wiki-named spell of a character without a set | kept |
| `end_empress` | End Empress | daily | wiki-named spell of a character without a set | kept |

### `forbidden_magic` (8 family pages, 8 shown to a basic grimoire)

| Page | Name | Tier | Basis | Status |
|---|---|---|---|---|
| `devil_dive` | Devil Dive | signature | mod's own page, no anime spell of that name (kept) | kept (mode 0, never hidden) |
| `five_leaf_whisper` | Five-Leaf Whisper | signature | mod's own page, no anime spell of that name (kept) | kept |
| `life_exchange` | Life Exchange | mid | mod's own page, no anime spell of that name (kept) | kept |
| `reversal_living` | Time Reversal on the Living | signature | mod's own page, no anime spell of that name (kept) | kept |
| `underworld_gate` | Underworld Gate | zone | mod's own page, no anime spell of that name (kept) | kept |
| `mana_devour` | Mana Devour | zone | mod's own page, no anime spell of that name (kept) | kept |
| `curse_misfortune` | Curse of Misfortune | zone | mod's own page, no anime spell of that name (kept) | kept |
| `reincarnation` | Forbidden Reincarnation | signature | mod's own page, no anime spell of that name (kept) | kept |


## 6. Books the table above does not list

The page lists of these books are not in plain `starter(...)` / `mid(...)` calls the audit could read, so no per-page table was made. They are covered as far as the sets go: every page a character set marks exclusive in them is hidden from the basic book (the test checks all of them, through the sources and the string literals), and nothing was added to them.

* Pages named by constants: Mercury (Nozel), Anti-Magic (Asta: all his pages are his, nothing in the book is exclusive), Kotodama (Zagred: creative-only), the base pages of Transmutation (Grey) and Recombination (Henry), the Soul / Soul Corpse / Song / Sound / Memory rebuilds.
* The generic wiki attributes (`WikiBooks.attribute`): Air, Hair, Memory, Mineral, Modification, Mucus, Mud, Nail, Permeation, Poison Plant, Red Ochre, Rock, Sandstone, Scale, Shakudo, Skin, Smoke, Snow, Song, Soul Corpse, Soul, Sound, Spike, Switching, Tongue, Tree, Stone, Vine, Vortex, Wing. Their five generic pages are the engine's, not anime spells; their exclusive pages (Lucius, Rades, Kahono, Mars) are hidden by the sets. They have no anime spells of ordinary mages to add (Soul, Soul Corpse, Song, Mineral are single-user attributes).
* Time (Julius): the Time Magic VFX and `TimeBook` are not touched (rule 3). Its two exclusive pages are hidden by the set; the other ten pages are kept; nothing was added.
* The Tensura-derived and wiki-generated books (Demon Beast, Body, Eye, Eyeball, Curse, Curse-Warding, Demon Fire / Ice / Light / Water, Barrier, Key, Chain, Butoh, Briar, Cherry Blossom, Fungus, Food, Crystal, Corundum, Bronze, Copper, Iron, Black Oil, Gel, Glass, Bubble, Ice Wedge, Legion, Game, Dice, Forbidden) ARE in the tables. Their wiki-named pages belong to minor characters who have no set (Sekke Bronazza's Bronze spells, Catherine's Joyful Destructive Ash ...) and are kept; only the spells of the characters with a set were reserved.

## 7. What was not added, and why (planned / unsourced)

| Book | Candidate | Reason |
|---|---|---|
| Fire | Burst Javelin (Forte Gris; the wiki also lists it for another user) | the Fire book is at the 24-page budget (11 shared + 11 character pages): only the two most anime-accurate pages fit |
| Fire | Exercitus Leonum, Soul Chain Deathmatch variants, Hellfire Incarnate | Vermillion / Magna spells: character pages already |
| Wind | Black Tornado Fang | wiki page of Yuno's Tornado Fang family: Yuno's |
| Earth | Rising Ground, Mud Wall Cliff, Ground Wall, Dirt Clod Seal, March of Holy Fools, Giant King of Wisdom | the Earth book already shows 9 generic pages, none belongs to a character; user and description of these were not fetched; left for a later release |
| Dark | Dark Binding, Black Cocoon, Dusk, Walpurgis Night, Black Heavens, Kids' Playground | Yami's (his set lists them as planned) or union spells with Nacht / Ichika |
| Ice | Ice Funeral, Ice Slicer, Ice Needle, Jotunn's Ice Floe | only the names were found (no description); Endless Ice Fangs and Cage of Infinite Hail are union / compound spells of Heath with Lily / a Mist user |
| Beast | Holy Hunting, Holy Howling, Bogeyman | named on the repo snapshot, no description fetched |
| Water | Sea Dragon style pages | Noelle's: hidden or character pages already |
| Cotton, Thread, Mirror, Painting, Dream, Compass, Slash, Song, Sealing, Shadow, Poison, Steel, World Tree, Recombination, Transmutation, Mercury, Anti-Magic, Time | any further generic page | single-user attributes: the only anime spells are the user's; inventing spells was ruled out |
| Sword, Creation, Explosion, Magma, Mist, Star, Sand, Copy, Illusion, Reinforcement | generic anime pages | no anime source for ordinary mages was found (these books are the mod's own); left as they are (4 to 6 pages each, all KEPT) |
| Dark (5 shown) | more generic pages | only Black Star could be sourced; the Dark book is one page under the "6 generic pages" target |

## 8. Decisions the owner still has to make

1. The added pages open by mastery order and they are LAST, so a basic book meets them late (the Fire pages at 22 of 24 pages need about 90 percent of the bar). Opening the basic book's pages by their place among the SHOWN pages instead of among all pages is a small change in `MasteryPages`; it was not done because the brief says the basic book opens "as today".
2. The Sea Dragon style pages that no source gives to Noelle (Sea Dragon's Wrath, Scale Mail, Torrent, Valkyrie Tide Lance) are kept in the basic Water book as mod pages; renaming them to non-Noelle names is a rename and was not done (rule 2).
3. Whether to also reserve the attribute-level pages that carry a single-user character's named spell (Mirror's Reflect Ray, Poison's Violett Schirm, Shadow's Kids' Playground, Cherry Blossom's other wiki spells ...): they were kept because hiding them would leave a one-user book with two or three pages and nothing anime-based to replace them.
4. Four tests were already red before this work and are not touched by it except the one named below: `AlignmentDataTest` still names `dark_hunting_ground`, `fallen_angel_wingbeat`, `mereoleona_hellfire_incarnate`, `mereoleona_purgatory_abyss` (pages of the character builders), and `NoelleFormAssetsTest` expected exactly three Water pages in `new_pages` (the pilots had added `point_blank_roar`).

## 9. Sources, and what could not be fetched (rule 10)

English wiki first, Japanese as the fallback. Every fandom host (blackclover.fandom.com, bcfanon, namu.wiki, r.jina.ai, manga-legend.com) fails with `getaddrinfo ENOTFOUND` from here, so the only way in was the search tool's summaries (mode extended). Searches run (English): Fire / Wind / Earth / Water / Lightning / Dark / Light / Plant / Ice Magic spell categories; Burst Javelin; Theresa Rapual / Ralph Niaflem / Allen Fiarain; Combo Eruption, Shining Flame Cross; Aqua Curtain; Cascade Destroyer and Holy Water Assassination Bullet; Killing Kakara Blade and Piercing Tornado Needle; Slicing Wind Emperor; Light Sword of Judgment, Bright Judgment Whip, Light Shaft of Divine Punishment; Walpurgis Night, Black Heavens, Black Star, Dark Binding; Heavenly Ice Fang, Dissolve, Crystal Ice Shield, Icicle Bridge; Bear Claw, Cheetah Charge, Rhinoceros Armor; Joyful Destructive Ash; Decaying World; Rising Salim, Pulsaranta, Thunder God's Boots. Japanese (ブラッククローバー 火魔法 魔法一覧) returned only aggregator pages (manga-data.com, renote.net, hissatuwaza) with Fuegoleon / Leopold / Magna spells and no ordinary-mage names, so no entry came from a Japanese source.

Used besides the search summaries: the repo's wiki snapshot (`.github/wiki`, spell descriptions of the Ice, Light, World Tree, Bone ... pages), `docs/attributes/*_rebuild.md`, `docs/magic/character_spells*.md`, `docs/magic/noelle_forms.md` and the 44 character files.

Confirmed by the search summaries (user and effect): Aqua Curtain (Rhya), Cascade Destroyer (Selena), Holy Water Assassination Bullet (Solid Silva), Slicing Wind Emperor and its four variants (Fanzell), Killing Kakara Blade (Jozou), Piercing Tornado Needle (George), Black Star (Ichika Yami), Heavenly Ice Fang and Dissolve and Crystal Ice Shield (Heath), Bear Claw / Cheetah Charge / Rhinoceros Armor (Vetto), Shining Flame Cross (Theresa Rapual), Combo Eruption (Dmitri Brint), Thunder God's Boots (Luck), Pulsaranta (Gadjah), Decaying World (Megicula and Vanica), Joyful Destructive Ash (Catherine), Light Sword of Judgment (Patolli and Rhya).

## 10. Server and performance (rules 9 and 11)

* The server decides everything: the pages are ordinary casts run on the server; the client only asks "is this page hidden" (`CharacterSets.hidden`: two map reads and a tag read, no allocation). No packet, no config key, no client class was added. The switch is the existing server config.
* Per player state: none new. Nothing static per player; nothing to clean on logout, death or dimension change (zones and delayed starts end by themselves and stop when the caster is gone: `gone(p)`).
* Worst case per cast (pinned by `BasicGrimoiresTest.theCastsAreBounded`): at most 6 effect packets, sent to players near the caster only through `VfxSpawn` and inside the per-tick packet budget; at most 4 bolts and 4 delayed starts; at most one zone of 4 seconds at 4 to 5 ticks per step; every hit loop is capped at 8 targets, every discard loop at 6 projectiles; no entity is spawned, no block is placed, no chunk is loaded, nothing is scanned beyond the box of the spell; cooldowns 10 to 25 seconds. With 50 players each casting the heaviest page once per its cooldown: about 6 packets per 10 s per player.
* Client draw cost: only existing shapes, 2 to 3 layers per cast, no new mesh; the far-player culling of `VfxSpawn` applies.
* The registry hooks add two hash lookups to `hidden` / `opens` / `listed`, which the grimoire asks per page; the list is 23 entries.

## 11. Tests, self-test, edits

* New test: `charset.BasicGrimoiresTest` (9 tests): every exclusive page hidden for the basic book; every reserved page real, hidden for the basic book, shown for the owner, hidden for the other characters of the book; every added page new, unique, last, owned by nobody, not a copy of a character's spell, texted, in the armour policy; no book over 24 family pages; the topped-up books show enough pages; the casts bounded; the hooks in place; Time Magic untouched.
* Self-test prefix (add to the CI smoke list): `magic.basic_grimoires` (`charset/BasicGrimoireSelfTests`, registered in `CharacterSetBoot`).
* Tests changed: `AlignmentDataTest` (two names appended to `TOUCHED_BY_NAME`: `black_star`, `holy_water_assassination_bullet`), `NoelleFormAssetsTest` (the Water `new_pages` must contain the three forms instead of being exactly them).
* Edits to existing files (append-only or one line): `book/GrimoireBook.java` (`familyAll` wraps the result in `BasicPages.append`), `charset/CharacterSetRegistry.java` (three lines: `hidden`, `opens`, `listed` ask `BasicSignatures`), `charset/CharacterSetBoot.java` (one line), `data/nusmp/armour_policy.json` (`new_pages` of FLAME, WATER, WIND, DARK, ICE, BEAST).
* New files: `book/BasicPages.java`, `book/BasicArts.java`, `charset/BasicSignatures.java`, `charset/BasicGrimoireSelfTests.java`, `test/.../charset/BasicGrimoiresTest.java`, `tools/lang/basic_spells.json` (19 page texts), this file.

## 12. NEW / REPLACED / KEPT

| Part | Verdict |
|---|---|
| The 19 generic anime pages (Fire 2, Water 3, Wind 6, Dark 1, Ice 4, Beast 3), their casts, texts and policy lines | NEW |
| The reserved-page list and its three registry hooks; the self-test | NEW |
| The basic grimoire showing 23 more signature spells of the characters | REPLACED (they were listed; `basicBookHidesSignatures=false` restores it) |
| Every page id, name, order, cast, cost, cooldown and unlock bit of every book; every character set file; the lock lines of `CharacterSpells`; the Time book | KEPT |
| The mod's own filler pages (Gust Lane, Flame Armour, Phoenix Flare, the Sea Dragon style pages ...) | KEPT in the basic books (class (c) in the tables) |
| The Mermaid Form open to every Water mage | KEPT |

## 13. What was not verified (honest list)

* Nothing was seen or heard in a game: not the pages in the page list, not the casts, not the effects, not how the basic books feel with the hidden pages.
* The anime / manga origin of the added pages: the wiki pages exist; for Combo Eruption, Killing Kakara Blade, Piercing Tornado Needle, Black Star and Icicle Bridge it was not confirmed that the anime shows them.
* The users of Aqua Javelin, Calderos, Icicle Bridge and Crystal Ice Shield were not confirmed; the effect of Crystal Ice Shield and Rhinoceros Armor is the shield the name says (the Rhinoceros Armor description is "stops powerful impacts").
* Whether some wiki-named pages kept in the basic books are in fact also used by characters with a set (the search summaries often gave one user per page, and the wiki lists several for some).
* The "basis" column is a heuristic for the kept pages (a name match against the wiki snapshot and a short list of confirmed users), not a research result per page.
* `MagicEnumAuditTest`, `GrimoireBookPlanTest`, `CanonBookMagicTest`, `MagicAuditTest` and the other tests that need the Minecraft classes cannot run in the shim; the Tensura / ManaScore stubs type-check the new classes except for the known stub artefacts (`MobEffectInstance` is abstract in the stub).
