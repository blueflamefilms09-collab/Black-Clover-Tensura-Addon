# Character spell sets: a grimoire shows the spells its character used, in the order he got them

Owner request: "The character's Magic spell should be based on what they used and how they cast spells in the anime Black Clover; not all of them had the same spells; fix that. That also includes how they acquired some spells: Spirit of Zephyr was a spell used AFTER Spirit Dive was gained, as an example." And: "for the basic magic grimoire they should be based on the anime Black Clover but be different from the Characters' spells."

Nothing here was seen in a game. What was checked: the pure rules, the loader and the shipped files by JUnit (`charset.*Test`), the sources by scan tests, the type-check against the API stubs. What the CI server smoke test will check in the real game: self-test `magic.character_spells`.

One server switch turns everything back: `magic.characterSpellSets` in `nusmp-character-spells-server.toml` (default `true`; `false` = every grimoire shows the whole shared element book and pages open by mastery order, exactly as before).

## 1. How it worked before, and what changed

Before: one grimoire skill per magic (`book_wind` ...). A character's grimoire is the same skill with `Canon` = the character id on the instance (written by `CanonSkillSync` from the grimoire in the Grimoire Slot). Every character saw the whole book; `book.CharacterSpells` only LOCKED a few pages to one character (`spirit_of_zephyr` to Yuno ...). Pages opened in book order by mastery (`multiverse.MasteryPages`: page k of n needs mastery k/n, rank / race gates, free slots 6 to 16).

Now: a character with a data file has his own SET: the pages he used (in the order he got them) and a rule for each. The shared book keeps all its pages (nothing is removed, renamed or moved); the set decides which of them his grimoire SHOWS and WHEN each opens.

| Question | Answer |
|---|---|
| Which pages does a character's grimoire show? | Exactly the pages of his set. Mode 0 of every book (the book's default page) is never hidden: it stays even when the sources do not name it (Fuegoleon's Exploding Fireball). |
| A character WITHOUT a file? | The whole book, as today (unfinished characters keep working). |
| The basic grimoire (bound to nobody)? | The whole book except the pages a character set marks `exclusive` (their signature spells never appear in the basic book). Config `basicBookHidesSignatures` (default true) turns that part off. |
| When does a page open? | The character's rule replaces the mastery order. The rank / race gates and the free-slot rule of `MasteryPages` still apply on top (a signature-tier page still needs its knight rank). |
| What does the old lock do? | It stays. A page that IS in the character's set is his whatever the old lock says; every page this feature ADDED is locked to its character (also with the switch off). |
| Saves? | Page ids, order and unlock bits are untouched. A hidden page keeps its bit; it just is not shown. Appending pages moves the mode numbers of the universal pages after the family list (Spirit Dive, Devil Union, Summon ...), exactly as the 1.93.0 appends did; nothing stores them by number except cooldown slots (a cooldown can sit on another page for its remaining seconds). |

## 2. The data format

One file per character: `src/main/resources/data/nusmp/character_spells/<canon_id>.json` (the id is the `CanonBook` id in lower case). A new file is listed in `CharacterSetRegistry.SHIPPED` (one line; a test fails if a file and the list disagree). A file with ANY problem is skipped as a whole (that character keeps today's grimoire) and the problems are listed in the log and in the self-test result, so a typo can never hide a page by accident.

```json
{
  "schema": 1,
  "canon": "yuno",                    // the CanonBook id, must equal the file name
  "magic": "WIND",                    // the book's MagicType (checked against CanonBook by the self-test)
  "book": "book_wind",                // the book skill's registry path
  "sources": { "en_wiki": "where this came from", "design": "..." },      // every spell names one of these
  "spells": [                         // the spells in the order he got them
    { "page": "kamaitachi",           // a FAMILY page id of the book (existing or added in book.CharacterPages)
      "name": "Wind Magic: Crescent Kamaitachi", "ja": "カマイタチの三日月",
      "origin": "anime",              // anime | manga | anime_only | manga_only
      "exclusive": false,             // true = never in the basic book and never open for another character's grimoire
      "source": "en_wiki",
      "acquire": { "type": "start" }, // see the rules below
      "note": "free text" },
    { "page": "spirit_of_zephyr", "name": "...", "ja": "...", "origin": "anime", "exclusive": true, "source": "en_wiki",
      "acquire": { "type": "all", "of": [ { "type": "page_uses", "page": "spirit_dive", "count": 2 }, { "type": "mastery", "min": 0.4 } ] },
      "note": "Spirit of Zephyr is used AFTER the Spirit Dive was gained" }
  ],
  "planned": [ { "name": "...", "ja": "...", "origin": "anime", "source": "en_wiki", "note": "canon spell with no page yet" } ],
  "notes": "free text"
}
```

### 2.1 Acquisition rules (small, pure, testable: `charset.SpellRule`)

| `type` | Fields | Opens when |
|---|---|---|
| `start` | | always (still subject to the rank gate and a free slot) |
| `mastery` | `min` 0..1 | the grimoire's mastery bar is at least `min` |
| `page_uses` | `page`, `count` 1..1000 | that page has been cast successfully `count` times (any page id of the set, or `spirit_dive`, `spirit_channeling`, `devil_union`); counted on the grimoire's tag, only for the pages some rule names |
| `page_open` | `page` | another page of the set is open |
| `trust` | `min` 0..100 | the bonded spirit's trust (of THIS book's spirit) is at least `min` |
| `flag` | `name` | a stage flag: `spirit_bond` (bonded to this book's spirit), `saint_stage` (trust >= `saintStageTrust` (95) and `saintStageDives` (3) Spirit Dives cast with this grimoire: "near 100% resonance with Sylph"), `mana_zone` (the Mana Zone gate is open), `anti_magic_lord` (the Anti-Magic Lord has awakened) |
| `all` / `any` | `of`: a list of rules (nesting at most 6 deep) | every / at least one of them holds |

Rank and race gates are NOT in the rules: they stay the page gates of `MasteryPages` (config `pageGates`, tier defaults).

### 2.2 What the loader rejects

Invalid JSON, a missing field, a schema other than 1, a bad id, a duplicate page, an unknown origin, a spell naming a source that is not declared, an unknown rule type or flag, a mastery / trust / count out of range, an empty `all` / `any`, a rule that names a page outside the set (except the three universal Spirit pages) or its own page, a dependency cycle between pages (they could never open), a file whose `canon` is not its name, two characters that both claim a page as exclusive. (`charset.CharacterSetLoaderTest`, `CharacterSetRegistryTest`.)

## 3. The code (where each part lives)

| Part | File |
|---|---|
| Rules, facts, data model, loader, registry (all pure: no game class) | `charset/SpellRule`, `RuleFacts`, `CharacterSet`, `CharacterSetLoader`, `CharacterSetRegistry` |
| Game side: hidden?, opens?, count a cast, flags | `charset/CharacterSets` |
| Server config, boot, CI self-test | `charset/CharacterSetConfig`, `CharacterSetBoot`, `CharacterSetSelfTests` |
| Pages added for the pilots (appended after the shared pages) and their casts | `book/CharacterPages`, `book/CharacterArts` |
| Hooks (one line each) | `GrimoireBook.isUnlocked` (hidden), `GrimoireBook.getModeName` (hidden shows "-"), `GrimoireBook.castPage` (count the cast), `MasteryPages.tryUnlock` (the character's rule), `CharacterSpells.owners/block` (added pages belong to their character; a listed page is not blocked), `MultiverseSync` ("Pages" counts only the visible pages), `SpellSource` (hidden pages are not "still sealed"), `NUSMP` (one boot line), `WindBook` / `WaterBook` / `FireBook` / `DarkBook` (the `familyPages()` appends the added pages) |
| Armour policy table | `data/nusmp/armour_policy.json`: the added page ids in `new_pages` of WIND, WATER, FLAME, DARK (the existing guard `ArmourPolicyTest` asks for it) |

## 4. The five pilots

Sources (rule 10: English wiki first, Japanese as the fallback). See section 9 for what could and could not be fetched. "Order" is the order of the table: it is the order this mod opens them in (anime order where known; where the first chapter or episode was not confirmed the order is this mod's design, marked `design` in the file).

Two points of design the owner may want to change (both are data, no code):

* Yuno's Spirit pages (Spirit Storm, Spirit's Hushed Dance, Spirit of Zephyr, Liable Tempest, Spirit of Boreas) all wait for the Spirit Dive to have been cast, and the Spirit Dive itself needs a bonded Sylph. A Yuno grimoire without a Sylph bond therefore never opens them. This is the owner's example taken literally.
* Yami's Black Moon (a Mana Zone spell in the series) opens with the Mana Zone gate OR near the top of the mastery bar (90%), so a player who is not yet at the late-game Mana Zone is not locked out of his signature spell.


## 5. The pages this feature added, and how they look and cost

Appended after the shared pages (ids new, nothing moved), each locked to its character, tiers as every page (cost, cooldown, rank gate by tier). The casts are server-decided; the picture is a LAYER of existing `VfxShape`s (2 to 4 shapes per cast, the Time Magic VFX untouched). Effects marked (inferred) were not described by any source: the name and intent are the series', the numbers are this mod's.

| Page | Character | Tier / base cooldown (the usual cooldown scaling applies) | What it does | VFX layers |
|---|---|---|---|---|
| `tornado_fang` | Yuno | mid / 10 s | 16-block lane, up to 8 foes dragged and lifted, 11 damage | WIND_GALE + 3 x WIND_TORNADO along the lane |
| `sylphs_breath` | Yuno | zone / 25 s | 14-block cone, up to 8 foes thrown back, 12 damage, spells shredded | WIND_GALE + FX_WIND_GUST + WIND_RING |
| `spirits_hushed_dance` | Yuno | zone / 35 s | 10 s (15 s in a Spirit Dive) of speed, light fall, spells within 3.5 blocks turned aside | WIND_ZEPHYR + SPIRIT_AURA following, a WIND_RING every 2 s |
| `liable_tempest` (inferred) | Yuno | zone / 35 s | 5 s tempest of 6 blocks where you aim: pulls in, spins, 5 damage a second, swallows spells | WIND_TORNADO + WIND_RING |
| `gale_white_bow` | Yuno | mid / 12 s | 40-block piercing arrow, 16 damage | WIND_SLASH + WIND_GALE |
| `swift_white_hawk` | Yuno | mid / 13 s | dash up to 16 blocks through foes (9 damage); needs room | WIND_GALE + WIND_SLASH |
| `heavenly_wind_ark` | Yuno | zone / 45 s | you and up to 8 allies within 8 blocks: slow fall, speed II 15 s, a push forward | WIND_TORNADO (following) + WIND_RING + WIND_ZEPHYR |
| `wind_blades_shower` | Yuno | mid / 15 s | 4 volleys over 1.6 s on 5 blocks radius, 5+ damage per volley and foe | WIND_RING + 4 x WIND_SLASH |
| `spirit_of_boreas` | Yuno | signature / 45 s | 6-block freezing cyclone around you: 22 damage, slow, frost, thrown away | FX_SPIRIT_NOVA + WIND_RING + WIND_TORNADO |
| `point_blank_roar` | Noelle | signature / 35 s | water crash on the target within 7 blocks, 3.5 radius, 24 damage, thrown back | WATER_DRAGON + FX_WATER_CRASH + WATER_BURST |
| `leo_parma` | Fuegoleon | mid / 15 s | one foe within 18 blocks: 8 damage, burns, held 3 s, 4 damage a second | FIRE_LION + FIRE_PILLAR + FIRE_BURST |
| `salamanders_breath` | Fuegoleon | zone / 25 s | 14-block cone, 16 damage (20 in a Spirit Dive), burns 5 s | FIRE_SPIRAL + FLAME_TRAIL + FIRE_BURST |
| `deep_black_blade` (inferred) | Yami | mid / 50 s | 20 s of +4 reach and Strength II | DARK_SLASH_BLADE + DARK_SLASH_AVIDYA |
| `black_crescent_moon` (inferred) | Yami | signature / 35 s | 22-block wide cut, 24 damage (36 with Black Moon up) | DARK_SLASH_AVIDYA + DARK_SLASH_DIMENSION + DARK_BLACK_MOON |

Where a spirit is out (Sylph for Yuno, Salamander for Fuegoleon) the existing emitter hooks (`SylphCasting.origin`, `FireCasting.origin`) move the start of `b.vfx(...)` effects to the spirit, so those pages leave the spirit's hands like the older ones do. The big pages (cost tier 20 and up) also call the powered Sylph through the existing `SylphCasting` path.

The Spirit Dive, Valkyrie Dress and Salamander arm are NOT touched: Yuno's Spirit pages count the existing `spirit_dive` page, Noelle's `valkyrie_dress` keeps its `ArmourCast` hook, Fuegoleon's Salamander pages need the existing bond. The Dive / Dress / arm lanes keep their files.

The pilot pages are written to look richer than a basic page (several layers, the spirit as the source); nothing visual could be run here, so "10x cooler" is the layering plan and not a verified look. Nothing here replaces a Time Magic effect, and nothing needed a new model, texture or sound.

## 6. Per-character tables (generated from the files)

### Yuno (`yuno`, book `book_wind`)

| # | Page id | Canon name | Japanese | Origin | Opens when | Page | Exclusive | Source |
|---|---|---|---|---|---|---|---|---|
| 1 | `kamaitachi` | Wind Magic: Crescent Kamaitachi | カマイタチの三日月 | anime | from the start | KEPT | no | en_wiki |
| 2 | `towering_tornado` | Wind Magic: Towering Tornado | 暴嵐の塔 | anime | mastery 12% | KEPT | yes | en_wiki |
| 3 | `tornado_fang` | Wind Magic: Tornado Fang | 暴嵐の牙 | anime | `towering_tornado` open AND mastery 20% | NEW | yes | en_wiki |
| 4 | `wind_blades_shower` | Wind Creation Magic: Wind Blades Shower | 風刃の叢雨 | anime | mastery 28% | NEW | yes | ja_search |
| 5 | `swift_white_hawk` | Wind Creation Magic: Swift White Hawk | 疾風の白鷹 | anime | mastery 35% | NEW | yes | ja_search |
| 6 | `gale_white_bow` | Wind Creation Magic: Gale White Bow | 疾風の白弓 | anime | mastery 42% | NEW | yes | ja_search |
| 7 | `heavenly_wind_ark` | Wind Creation Magic: Heavenly Wind Ark | 天つ風の箱舟 | anime | mastery 50% | NEW | yes | ja_search |
| 8 | `sylphs_breath` | Wind Spirit Magic: Sylph's Breath | シルフの息吹 | anime | flag `spirit_bond` | NEW | yes | en_wiki |
| 9 | `spirit_storm` | Wind Spirit Magic: Spirit Storm | スピリット・ストーム | anime | flag `spirit_bond` AND `spirit_dive` cast 1 x | KEPT | yes | en_wiki |
| 10 | `spirits_hushed_dance` | Wind Spirit Magic: Spirit's Hushed Dance | 精霊の静かなる舞踏 | anime | `spirit_dive` cast 2 x AND mastery 50% | NEW | yes | en_wiki |
| 11 | `spirit_of_zephyr` | Wind Spirit Creation Magic: Spirit of Zephyr | スピリット・オブ・ゼファー | anime | `spirit_dive` cast 2 x AND mastery 40% | KEPT | yes | en_wiki |
| 12 | `liable_tempest` | Liable Tempest | - | anime_only | `spirit_of_zephyr` open AND `spirit_dive` cast 4 x | NEW | yes | en_wiki |
| 13 | `spirit_of_boreas` | Wind Spirit Creation Magic: Spirit of Boreas | - | anime | flag `saint_stage` AND `spirit_of_zephyr` open | NEW | yes | en_wiki |

Canon spells with no page yet (documented for the content builders, ignored by the engine):

* Spirit of Euros (anime; en_wiki): Named in the wiki's spirit list; no description found, no page yet.
* Spirit of Notos (anime; en_wiki): Named in the wiki's spirit list; no description found, no page yet.
* Saint Spirit of Zephyr (anime; en_wiki): The Saint Stage version of Spirit of Zephyr (also seen against Zenon). Should be a stronger form of the same page once Saint Stage is reached.
* Saint Spirit of Boreas (anime; en_wiki): The Saint Stage version of Spirit of Boreas.
* Union Magic: Never-Never Land (anime; en_wiki): Star Magic combined with Sylph's wind: a rotating dome of stars over the whole kingdom. Needs Star Magic, which is a second attribute and not part of the Wind book.
* Star Magic (anime; en_wiki): Yuno's second attribute (a VfxShape STAR_MAGIC exists); a different book.

Notes: Not in Yuno's grimoire (kept in the basic Wind book): Gust Lane and Slicing Wind Emperor were not found in any source for Yuno. Spirit Dive itself is the book's universal Spirit page.

### Noelle (`noelle`, book `book_water`)

| # | Page id | Canon name | Japanese | Origin | Opens when | Page | Exclusive | Source |
|---|---|---|---|---|---|---|---|---|
| 1 | `waterball` | Water Creation Magic: Sea Dragon's Waterball | - | anime | from the start | KEPT | no | other_en |
| 2 | `roar` | Water Creation Magic: Sea Dragon's Roar | 海竜の咆哮 | anime | mastery 10% | KEPT | yes | en_wiki |
| 3 | `sea_dragons_nest` | Water Creation Magic: Sea Dragon's Nest (Lair) | - | anime | mastery 25% | KEPT | yes | other_en |
| 4 | `cradle` | Water Creation Magic: Sea Dragon's Cradle | - | anime | mastery 35% | KEPT | yes | other_en |
| 5 | `point_blank_roar` | Point-Blank Sea Dragon's Roar | - | anime | `roar` cast 3 x AND mastery 50% | NEW | yes | other_en |
| 6 | `valkyrie_dress` | Valkyrie Armor: Sea Goddess's Dress | 海神戦乙女の羽衣 | anime | `cradle` cast 2 x AND mastery 70% | KEPT | yes | en_wiki |

Canon spells with no page yet (documented for the content builders, ignored by the engine):

* Saint Valkyrie Dress (Undine) (anime; en_wiki): Comes from the water spirit Undine and purifies malice. In this mod it is the Water book's Spirit Dive page (the Valkyrie plates); owned by the Valkyrie lane, nothing to add here.
* Valkyrie Armor: Dragon Form (anime; en_wiki): (Japanese read from the owner's picture of the wiki's variations.) Sea God Leviathan's power. The dragon and mermaid forms are built by the Valkyrie lane; add them as pages after that.
* Mermaid Form (anime; en_wiki): Boosts her power underwater. Same lane as the Dragon Form.
* Black Dragon Form (anime; design): Named in the owner's reference picture of her variations; no description found.
* Sea Dragon God's Vortex / Tail (anime; other_en): Listed by one low-quality source only: unconfirmed, no page.

Notes: Not in Noelle's grimoire (kept in the basic Water book): Aqua Javelin, Sea Dragon's Wrath, Valkyrie Tide Lance, Sea Dragon's Scale Mail and Sea Dragon's Torrent were not found in any source for Noelle.

### Fuegoleon (`fuegoleon`, book `book_fire`)

| # | Page id | Canon name | Japanese | Origin | Opens when | Page | Exclusive | Source |
|---|---|---|---|---|---|---|---|---|
| 1 | `sol_linea` | Flame Creation Magic: Sol Linea | 小太陽光芒 (ソル・リーネア) | anime | from the start | KEPT | yes | en_wiki |
| 2 | `ignis_columna` | Flame Magic: Ignis Columna | 螺旋火炎 (イグニス・コルムナ) | anime | mastery 15% | KEPT | yes | en_wiki |
| 3 | `leo_rugiens` | Flame Creation Magic: Leo Rugiens | 大火炎獅子の咆哮 (レオ・ルゼーナス) | anime | mastery 30% | KEPT | yes | en_wiki |
| 4 | `leo_parma` | Flame Creation Magic: Leo Palma (Hand of the Flame Lion) | 大火炎獅子の掌 (レオ・パルマ) | anime | `leo_rugiens` open AND mastery 40% | NEW | yes | ja_search |
| 5 | `salamanders_breath` | Flame Spirit Magic: Salamander's Breath | サラマンダーの吐息 | anime | flag `spirit_bond` AND mastery 50% | NEW | yes | en_wiki |

Canon spells with no page yet (documented for the content builders, ignored by the engine):

* Exercitus Leonum (anime; en_wiki): A wiki page of that name appeared in the Fuegoleon search; the user and effect were not confirmed, so no page.
* Spirit Dive with Salamander (anime; ja_search): The Fire book's Spirit Dive page (universal), with the flaming arm drawn by the Spirit Dive lane; nothing to add here.

Notes: Mode 0 of the Fire book (Exploding Fireball) is the book's default page and cannot be hidden, so it stays in his grimoire although the sources do not name it. Not in his grimoire (kept in the basic Fire book): Calderos, Spiral Flame, Wild Bursting Flame (possibly a Vermillion spell, not confirmed for him), Calidos Brachium (Mereoleona's, still locked to her), Crimson Lion King, Flame Armour and Phoenix Flare.

### Asta (`asta`, book `book_anti_magic`)

| # | Page id | Canon name | Japanese | Origin | Opens when | Page | Exclusive | Source |
|---|---|---|---|---|---|---|---|---|
| 1 | `demon_slayer_slash` | Anti Magic: Demon-Dweller Sword: Black Slash | - | anime | from the start | KEPT | no | en_wiki |
| 2 | `bull_thrust` | Bull Thrust | - | anime | mastery 15% | KEPT | no | other_en |
| 3 | `black_form` | Black Asta | - | anime | mastery 30% | KEPT | no | other_en |
| 4 | `black_meteorite` | Black Meteorite | - | anime | `black_form` cast 1 x | KEPT | no | other_en |
| 5 | `black_hurricane` | Black Hurricane | - | anime | `black_form` cast 1 x | KEPT | no | other_en |
| 6 | `black_divider` | Demon-Slayer Sword: Black Divider | - | anime | `black_form` cast 2 x AND mastery 50% | KEPT | no | other_en |
| 7 | `infinite_slash` | Demon-Slasher Katana: Infinite Slash | - | anime | `black_divider` cast 3 x AND mastery 65% | KEPT | no | other_en |
| 8 | `infinite_slash_equinox` | Demon-Slasher Katana: Infinite Slash Equinox | - | anime | `infinite_slash` cast 3 x AND flag `anti_magic_lord` | KEPT | no | other_en |

Canon spells with no page yet (documented for the content builders, ignored by the engine):

* Demon-Destroyer Sword techniques (anime; en_wiki): The Demon-Destroyer Sword is drawn through the book's sword system; the wiki's Anti Magic page lists it but no technique description was found.

Notes: Every existing Anti-Magic page is Asta's. The set only gives them an anime-order of opening (and the existing Aura costs, weapon requirements and Lord rules are untouched). Black Asta's own Lord rules stay in AntiMagicBook.

### Yami (`yami`, book `book_dark`)

| # | Page id | Canon name | Japanese | Origin | Opens when | Page | Exclusive | Source |
|---|---|---|---|---|---|---|---|---|
| 1 | `avidya_slash` | Dark Cloaked Avidya Slash | - | anime | from the start | KEPT | no | en_wiki |
| 2 | `black_blade` | Dark Cloaked Black Blade | - | anime | mastery 8% | KEPT | yes | en_wiki |
| 3 | `avidya_wild_slash` | Dark Cloaked Avidya Wild Slash | - | anime | mastery 15% | KEPT | yes | en_wiki |
| 4 | `black_hole` | Dark Cloaked Black Hole | - | anime | mastery 22% | KEPT | yes | other_en |
| 5 | `dimension_slash` | Dark Cloaked Dimension Slash | - | anime | mastery 32% | KEPT | yes | en_wiki |
| 6 | `iai_slash` | Dark Cloaked Iai Slash | - | anime | `black_blade` cast 2 x AND mastery 42% | KEPT | yes | en_wiki |
| 7 | `black_moon` | Dark Cloaked Black Moon | - | anime | flag `mana_zone` OR mastery 90% | KEPT | yes | other_en |
| 8 | `death_thrust` | Dark Cloaked Death Thrust | - | anime | `black_moon` cast 1 x | KEPT | yes | other_en |
| 9 | `deep_black_blade` | Dark Cloaked Deep Black Blade | - | anime | `black_blade` cast 3 x AND mastery 60% | NEW | yes | en_wiki |
| 10 | `black_crescent_moon` | Black Crescent Moon | - | anime | `black_moon` cast 3 x AND mastery 75% | NEW | yes | en_wiki |
| 11 | `dimension_slash_equinox` | Dark Cloaked Dimension Slash: Equinox | - | anime | `dimension_slash` cast 4 x AND mastery 85% | KEPT | yes | en_wiki |

Canon spells with no page yet (documented for the content builders, ignored by the engine):

* Dark Cloaked Avidya Blunt Strike (anime; en_wiki): Named on the wiki's offensive list; no description found.
* Black Blade Shadow Strike (anime; en_wiki): Named on the wiki's offensive list; no description found.
* Dark Cloaked Black Cocoon (anime; en_wiki): Named on the wiki's defensive list; no description found.
* Dark Cloaked Dusk (anime; en_wiki): Named on the wiki's defensive list; no description found.

Notes: Not in Yami's grimoire (kept in the basic Dark book): Dark Cloaked Guard, Dark Cloaked Shadow Embrace and Black Hole Collapse are this mod's own pages and were not found in any source for Yami.


## 7. NEW / REPLACED / KEPT

| Part | Verdict |
|---|---|
| The character spell set engine, the five data files, the 14 added pages, the server config, the self-test, the lang fragment | NEW |
| A grimoire of Yuno / Noelle / Fuegoleon / Asta / Yami showing only his pages and opening them by his rule | REPLACED (their old behaviour: the whole book opening by mastery order; with the switch off it is back) |
| The basic grimoire hiding the characters' signature spells | REPLACED (they were listed but locked); `basicBookHidesSignatures` false restores it |
| Every page id, name, order, cast, cost and unlock bit of the four shared books and the Anti-Magic book | KEPT |
| The lock lines of `CharacterSpells`, the Spirit Dive, Valkyrie Dress and Salamander code, the Sylph / Undine / fire casting hooks, rank gates, free slots | KEPT |
| Characters without a file | KEPT as they are (whole book, mastery order) |

Pages of a shared book that a pilot never used are only hidden from that pilot, they stay in the basic book: Yuno lacks Gust Lane and Slicing Wind Emperor; Noelle lacks Aqua Javelin, Sea Dragon's Wrath, Valkyrie Tide Lance, Sea Dragon's Scale Mail and Sea Dragon's Torrent; Fuegoleon lacks Calderos, Spiral Flame, Wild Bursting Flame, Calidos Brachium, Crimson Lion King, Flame Armour and Phoenix Flare; Yami lacks Dark Cloaked Guard, Shadow Embrace and Black Hole Collapse; Asta lacks nothing. The basic books lose (when the setting is on) the pages marked exclusive: the basic Wind book keeps Crescent Kamaitachi and Gust Lane and Slicing Wind Emperor, and so on. The later builder of the basic books re-bases those lists.

## 8. How a content builder adds a character (10 lines)

1. Research (rule 10: English wiki first, Japanese as the fallback) and record the sources in the file.
2. Copy `data/nusmp/character_spells/yuno.json` to `<canon_id>.json`; set `canon`, `magic`, `book` (the character's `CanonBook` entry and its book registry path).
3. List his spells in the order he got them: `page` = an existing family page id of that book (the pages he never used simply are not listed); `acquire` = a rule of section 2.1; `exclusive` = true for his signature spells.
4. A canon spell with no page yet: add a `BookPage` line to the right list in `book/CharacterPages` and its cast to `book/CharacterArts` (or the book's own arts class); a book that is not Wind / Water / Fire / Dark needs the same three-line `withCharacters` wrapper in its `familyPages()` and a `register("book_x", ...)` list.
5. Add one line `"<canon_id>"` to `CharacterSetRegistry.SHIPPED`.
6. Page text: `nusmp.page.<book>.<page_id>` in your lang fragment; added page ids in `new_pages` of the magic in `armour_policy.json`; a new book in the `BOOK_FILES` map of `CharacterSetSourceScanTest`.
7. Run `CharacterSetRegistryTest` and `CharacterSetSourceScanTest` (the file loads, every page resolves, every added page is owned and has text); the self-test `magic.character_spells` does the same in the real game.
8. Put the character in the docs table of this file. Do not touch `CharacterSpells` for the new character's pages: an added page is locked to its owner by the data.

## 9. Research record and what could not be verified (rule 10)

Search was the only way in: every fandom host (blackclover.fandom.com and the other wikis), ja.namu.wiki and w.atwiki.jp failed with `getaddrinfo ENOTFOUND` from the build machine, and web.archive.org is blocked. Searches run (English first, then Japanese): Yuno's spirit techniques, Spirit's Hushed Dance, the first use of Spirit of Zephyr, Yuno's Japanese spell list; Noelle's Water Creation spells and armour forms; Fuegoleon's flame spells in English and Japanese; Asta's Anti-Magic techniques; Yami's Dark Magic list. The owner's pictures 126 (Noelle's variations) and 127 (Yuno's Spirit Magic) were read directly. What each file's `source` ids mean is in the file.

Confirmed by the search summaries or the owner's pictures: Yuno's Spirit Magic list and the Saint Stage rule (127), Spirit's Hushed Dance (Mana Zone, Japanese name, used to evade even Light Magic), Sylph's Breath / Spirit Storm / Spirit of Zephyr Japanese names, Noelle's four variations (126), the Valkyrie Armor awakening in the Elf Reincarnation arc, Sea Dragon's Roar / Waterball / Nest / Cradle, Fuegoleon's Sol Linea / Leo Rugiens / Ignis Columna / Leo Palma / Salamander's Breath and the Salamander arm, Yami's Dark Magic lists (offensive and defensive) and the descriptions of Black Hole, Black Moon, Death Thrust, Black Blade, Dimension Slash.

NOT verified (honest list):

* The chapter or episode where each spell first appeared. Only one datum was found for Spirit of Zephyr (the search summaries disagree: a fan page lists chapter 16 / episode 17, another source says it was used in a Golden Dawn fight in episode 154 and called "new" in chapter 208). The ORDER in the files is therefore the owner's rule (Zephyr after the Dive) plus this mod's design from the arcs, not a measured timeline.
* The effect of Liable Tempest (anime-only; the owner's picture shows lines of wind radiating from one point), Spirit of Boreas (wiki: offensive, close range), Deep Black Blade and Black Crescent Moon (names only): the casts are inferred.
* Japanese names for most Noelle and Yami spells, the Spirit of Boreas, Liable Tempest.
* Whether Slicing Wind Emperor, Calderos, Spiral Flame and Wild Bursting Flame belong to Yuno / Fuegoleon (not found for them, so they were left out of their sets; Wild Bursting Flame showed up in the Fuegoleon search and may be his).
* Where Asta's Bull Thrust, Black Meteorite, Black Hurricane and Infinite Slash came in the story (the order in his file is design).
* Nothing was seen or heard in a game: the effects, the pages opening in order, the "-" for a hidden page in the page list, the Codex and status panel counts.
* `MagicEnumAuditTest` (and the other tests that need the Minecraft classes: `GrimoireBookPlanTest`, `CanonBookMagicTest`, `FusionDesignTest`, `GrimoireDesignGeneratorTest`, `MagicAuditTest`, `GrimoireShelfLayoutTest`, `GrimoireCarryTest`, `SpiritLightRulesTest`, `LightMoodRulesTest`) cannot run offline in this shim; the other 117 test classes of the book, grimoire, multiverse, core, perf, charset, gnome, spiritlight, spiritspace, particle, skilltree and client UI packages, and the Sylph / Undine / fire casting / Spirit of Darkness scan tests, pass. The lang audit (every page has text) reads `en_us.json` and will pass after the fragment `tools/lang/character_spells.json` is merged.

## 10. Server and performance (rules 9 and 11)

* The server decides everything: the rules are evaluated on the server (`MasteryPages` tick, every 2 seconds per player, for the pages of one book); the client only asks "is this page hidden" (`CharacterSets.hidden`: two map reads and a tag read, no allocation, no world access) and reads the same switch (a server config NeoForge syncs). No packet was added; the grimoire's existing tag sync carries the use counts.
* Per player state is the grimoire instance's own tag (`CsUses`: one int per counted page, at most about 5 pages per character, capped at 9999) so it is keyed by the player and goes with the player; nothing static per player. Nothing needs cleaning on logout, death or dimension change.
* The pilot casts: at most 4 effect packets per cast (6 for a blade rain), each through `VfxSpawn` to players within 96 blocks and inside the per-tick packet budget; at most one `SpellRuntime.zone` per cast, 15 s at most, every 5 to 20 ticks; every hit loop is capped at 8 targets; no entity is spawned, no block is placed, no chunk is loaded; cooldowns 10 to 50 s. Worst case with 50 players each casting the heaviest page once per its cooldown: about 4 packets per page per 10 s per player, far inside the existing budget. The scan test `CharacterSetSourceScanTest` pins these limits.

## 11. Characters still on the old behaviour (everyone except the five)

Mereoleona, Leopold, Nozel, Julius, Mimosa, Charmy, Finral, Vanessa, Luck, Gauche, Magna, Zora, Klaus, Kirsch, Karna, William, Langris, Lemiel, Zenon, Vanica, Dante, Lucius, Floga, Gadjah, Lolopechka, Mars, Charlotte, Jack, Rill, Dorothy, Kaiser, Nacht, Grey, Gordon, Henry, Letoile, Zagred, Secre, Rades, Kahono, Kiato. Their grimoire is the whole book as today, with their old lock lines. The steps are in section 8.

Self-test prefix to add to the CI smoke list: `magic.character_spells`.
