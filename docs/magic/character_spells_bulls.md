# Black Bulls character spell sets: Vanessa, Charmy, Luck, Magna, Gauche, Gordon, Zora, Grey, Henry, Finral, Nacht, Secre

Owner request: "The character's magic spells should be based on what they used and how they cast spells in the anime Black Clover; not all of them had the same spells; fix that (that also includes how they ACQUIRED some spells). Make the character grimoire VFX look 10x cooler than the non-character (basic grimoire) users."

This is phase 2 of `docs/magic/character_spells.md` (the engine and the five pilots: read its schema and acquisition rules first). This file covers the twelve Black Bulls members that were still on the old behaviour (the whole shared book, opening by mastery order). Builder key: `bulls`.

Nothing here was seen in a game. What was checked: the pure rules, the twelve files and the sources by JUnit (`charset.CharacterSetBullsTest`, 17 tests, plus the pilot's `charset.*Test` and `book.ArmourPolicyTest`), the type-check against the API stubs, and one deliberate wrong call as a negative control. What the CI server smoke test will check in the real game: self-test `magic.character_spells_bulls`.

The same server switch as the engine turns everything back: `magic.characterSpellSets` in `nusmp-character-spells-server.toml` (default `true`; `false` = every grimoire shows its whole shared book, pages open by mastery order and the flair is not drawn).

## 1. Characters

| Character | Canon id | Book | Verdict |
|---|---|---|---|
| Vanessa Enoteca | `vanessa` | `book_thread` | done (no new page: every canon spell already had one) |
| Charmy Pappitson | `charmy` | `book_cotton` | done |
| Luck Voltia | `luck` | `book_lightning` | done |
| Magna Swing | `magna` | `book_fire` | done |
| Gauche Adlai | `gauche` | `book_mirror` | done |
| Gordon Agrippa | `gordon` | `book_poison` | done |
| Zora Ideale | `zora` | `book_ash` | done |
| Grey | `grey` | `book_transmutation` | done (thinnest sources of the squad) |
| Henry Legolant | `henry` | `book_recombination` | done |
| Finral Roulacase | `finral` | `book_spatial` | done |
| Nacht Faust | `nacht` | `book_shadow` | done |
| Secre Swallowtail | `secre` | `book_sealing` | done |
| Letoile Becquerel | (`letoile`) | `book_compass` | SKIPPED on purpose: the repo has the CanonBook entry, but she is a Golden Dawn member (Compass Magic), which belongs to the `gold` builder; no file was written so nobody collides |

## 2. What changed for a player

* A grimoire bound to one of the twelve shows only the pages of his set and opens each by his rule (the rank / race gates and the free-slot rule of `MasteryPages` still apply on top). Pages he never used stay in the basic book. Mode 0 of every book is never hidden.
* The basic grimoire (bound to nobody) no longer shows the pages marked `exclusive` (their signature spells). `basicBookHidesSignatures=false` restores that.
* 20 new pages (section 4), each locked to its character.
* The flair (section 5): the grimoire of each of the twelve draws a signature layer in his colour on top of every cast of a mid page or above.
* Acquisition follows the series where the sources said how: Double Reflect Refrain needs the Real Double used first (the wiki says so), Rouge awakens after the plain Red Thread has been used (episode 64), Glutton's Banquet (Food Magic, awakened in the Elf Reincarnation arc) comes after the Sheep Cook, Keraunos is the Heart Kingdom training (Mana Zone gate or 45 percent) after the boots, the Lightning Battle Fiend after Keraunos, Nacht's Union Modes open with his Devil Union casts, Magna's Soul Chain Tag Deathmatch (a hidden spell prepared for months) near the top of his bar. Where no source gave an order the order is this mod's design and is marked `design` in the file.

## 3. How they cast (the owner stressed it)

| Character | In the anime | What this mod does |
|---|---|---|
| Magna | swings a baseball bat of flame at fireballs he tosses | KEPT and reused: `firecast.MagnaSwing` wraps every family page of the Fire book (his three new pages included, because they are appended family pages), so with Magna's Soul Bat in the main hand each cast is a toss and a swing with the contact frame at tick 8; without the bat the cast is refused with the existing message |
| Luck | lightning in the claws and fists, boots that fly | Thunderclap Crumbling Orb is thrown from the claws, Thunder God's Gloves is a 20 s buff with arcs from the fists, Keraunos plays the "arms thrust out" release clip (`withAnim("out")`) and turns him into the bolt |
| Finral | portals and a homing sphere of space | Fallen Angel Wingbeat uses the thrust clip and the existing portal shapes (`SPACE_PORTAL`, `SPACE_RINGGATE`) at his hand, at the foe and at the arrival |
| Secre | open and close: both hands out | Inverse Release uses the existing `seal_crush` clip (arms out, hands open) like the other Sealing pages |
| Zora | ash drawn as a magic circle on the ground | both traps are circles on the ground (`ASH_FORMATION`); the Counter Trap Magic circle follows him |
| Nacht | shadow hands from the ground, devils in his body | the Dark Hunting Ground and the Atonement Monument are shadow pools on the ground; the Union Modes keep the existing Unite code |
| Gauche | a mirror held in the hand | the mirror shapes (`MIRROR_ARRAY`, `MIRROR_RAY`, `MIRROR_FRAME`) are built round the caster and along the lane |
| Vanessa, Charmy, Gordon, Grey, Henry | thread, sheep, poison masses, shape change, the hideout's fist | their effects use the existing shapes of their magic (`THREAD_*`, `COTTON_SHEEP`, `POISON_*`, `ALCHEMY_CIRCLE`, `RECOMBINE_CONSTRUCT`) |

Not done (honest): no new animation clips (the clips are chosen from the existing ones), no cast item requirement other than Magna's existing bat, no keybind, no change of Vanessa's finger gesture, Gordon's two-hands-apart pose or Henry's hideout transformation, no skin change for Grey (her page is a vanishing act, not a disguise).

## 4. The 20 new pages

Appended after the shared pages (ids new, nothing moved), each exclusive to its character, tiers as every page (cost, cooldown, rank gate by tier). The casts are server-decided; the picture is a LAYER of existing `VfxShape`s. Effects marked inferred have a name from the series but no description in the sources: the numbers are this mod's.

| Page | Character | Tier / base cooldown | What it does | Status |
|---|---|---|---|---|
| `fluffy_sheep_cushion` | Charmy | mid / 15 s | you and up to 8 allies within 6 blocks: slow fall, resistance, absorption; foes bounced away, no damage | ja_search, effect inferred |
| `thunderclap_crumbling_orb` | Luck | mid / 8 s | ball of 22 blocks, bursts for 13 in 3 blocks (8 foes) and slows | en_wiki, effect inferred |
| `thunder_gods_gloves` | Luck | mid / 30 s | 20 s of haste II and strength, 3 damage arc a second to the nearest foe within 3 blocks | en_wiki, effect inferred |
| `keraunos` | Luck | signature / 35 s | dash up to 20 blocks as a bolt, 15 damage to 8 foes on the line; needs room | ja_search |
| `exploding_buckshot` | Magna | mid / 10 s | 11-block cone, 9 damage and burning to 8 foes | ja_search |
| `grand_slam` | Magna | mid / 13 s | three fireballs in a fan, 8 damage in 2.6 blocks each, burning | en_wiki |
| `soul_chain_deathmatch` | Magna | signature / 75 s | one foe within 16 blocks: 10 damage, chained 8 s (pulled back past 7 blocks, weakness II on it, strength II on him) | en_wiki; the power split is not modelled |
| `double_reflect_refrain` | Gauche | zone / 20 s | 18-block lane hit twice (10 + 6) for 8 foes | en_wiki (needs Real Double) |
| `mirrors_brigade` | Gauche | zone / 35 s | 8 s wall of mirrors, up to 6 hostile projectiles at a time turned back on their casters | en_wiki, effect inferred |
| `verbotenes_obst` | Gordon | mid / 12 s | poison mass of 27 blocks, bursts for 14 in 3.6 blocks, poison II | en_wiki |
| `poison_clouds_abode` | Gordon | zone / 35 s | 8 s cloud of 6 blocks radius, 3 damage and poison a second to 8 foes | ja_search |
| `ash_snare_trap` | Zora | mid / 15 s | circle waiting 8 s, holds the first foes for 4 s (6 damage) | ja_search |
| `counter_trap_magic` | Zora | signature / 45 s | 10 s circle absorbs up to 3 hostile spells, each returned at the nearest foe for 14 | en_wiki |
| `transformation_guise` | Grey | mid / 40 s | 15 s invisible and quick, hunting mobs lose her | en_wiki, effect inferred (no skin change) |
| `mana_rocket_punch` | Henry | mid / 15 s | piercing bolt of 24 blocks, 14 damage, knock aside | en_wiki |
| `mini_bull` | Henry | mid / 20 s | 12-block charge, 10 damage to 8 foes, thrown forward | en_wiki (name only), effect inferred |
| `fallen_angel_wingbeat` | Finral | mid / 13 s | one foe within 18 blocks: 6 damage and thrown 12 to 20 blocks away | en_wiki |
| `dark_hunting_ground` | Nacht | zone / 30 s | 6 s shadow of 8 blocks: 5 damage a second to 8 foes, blindness, slowness | ja_search |
| `atonement_monument` | Nacht | signature / 90 s | up to 4 foes within 6 blocks: 22 damage, held 4 s in darkness | en_wiki, effect inferred |
| `inverse_release` | Secre | mid / 15 s | you and up to 8 allies within 8 blocks lose binds and a sealed grimoire | en_wiki |

## 5. The flair: "10x cooler than the basic grimoire"

`book.CharacterFlairBulls` (hooked after the cast counter in `GrimoireBook.castPage`, one line) adds, after a successful cast, a signature layer drawn in the character's colour on top of the page's own effect (the page keeps its own picture; the 20 new pages already layer 3 to 5 shapes). Pure palette and limits: `charset.BullsFlairPalette`.

| Tier of the page | Extra layers |
|---|---|
| starter (cost under 10 percent) | none |
| mid (10 to 17 percent) | a ground ring at the feet |
| zone (18 to 29 percent) | + a burst where he aims |
| signature (30 percent and up) | + a following aura |

| Character | Colour | Ring | Burst | Aura |
|---|---|---|---|---|
| Vanessa | 0xFF4060 | `THREAD_WEB` | `THREAD_STRINGS` | `ROUGE_FATE_TAP` |
| Charmy | 0xFFF4FA | `MANA_CHARGE` | `COTTON_SHEEP` | `SPIRIT_AURA` |
| Luck | 0xFFE14A | `LIGHTNINGFX_ZONE` | `LIGHTNINGFX_BURST` | `FX_LIGHTNING_ARC` |
| Magna | 0xFF5A1A | `FLAMEFX_ZONE` | `FLAMEFX_BURST` | `FIRE_PILLAR` |
| Gauche | 0x8E86FF | `MIRROR_FRAME` | `MIRROR_SHATTER` | `MIRROR_DOUBLE` |
| Gordon | 0xB4E040 | `POISON_CURTAIN` | `POISON_BREATH` | `CURSE_FX1` |
| Zora | 0xB0AAA2 | `ASH_FORMATION` | `ASH_FX3` | `ASH_FX2` |
| Grey | 0x7AF0D8 | `ALCHEMY_CIRCLE` | `TRANSMUTATION_FX1` | `SPIRIT_AURA` |
| Henry | 0xFF9A3C | `RECOMBINATION_FX1` | `RECOMBINE_CONSTRUCT` | `SPIRIT_AURA` |
| Finral | 0xB088FF | `SPACE_RINGGATE` | `SPACE_CRACK` | `SPACE_PORTAL` |
| Nacht | 0x6A5AA8 | `SHADOW_POOL` | `SHADOW_UNITE` | `DARKFX_ZONE` |
| Secre | 0x8AE8FF | `SEAL_ORBIT` | `SEAL_BASIC` | `SEAL_BRANCH` |

The flair does not touch the Time Magic VFX, adds no shape, texture, prop or sound (existing `VfxShape`s only) and so needed no preview image; whether it LOOKS 10x better is a plan of layers, not a verified look. It is one hook line: a grimoire without a set, the basic grimoire and the switch off get nothing. No new `VfxShape` or prop painter was added (the brief allowed it for signature spells; the existing shapes were enough and nothing visual can be run here).

## 6. Per-character tables (generated from the files)

### Vanessa (`vanessa`, book `book_thread`, magic THREAD)

| # | Page id | Canon name | Japanese | Origin | Opens when | Page | Exclusive | Source | VFX layers |
|---|---|---|---|---|---|---|---|---|---|
| 1 | `red_thread` | Thread Magic: Red Thread | 糸魔法 | anime | from the start | KEPT | no | en_wiki | unchanged, plus the character flair |
| 2 | `thread_mending` | Thread Magic: Mending | - | anime | from the start | KEPT | no | en_wiki | unchanged, plus the character flair |
| 3 | `arachnes_web` | Arachne's Web | - | anime | mastery 12% | KEPT | no | en_wiki | unchanged, plus the character flair |
| 4 | `thread_trap` | Thread Trap Generation (Trap Magic) | - | anime | mastery 22% | KEPT | yes | en_wiki | unchanged, plus the character flair |
| 5 | `arresting_dark_weave` | Arresting Dark Weave | - | anime | mastery 30% | KEPT | yes | repo | unchanged, plus the character flair |
| 6 | `dancing_doll` | Dancing Doll | - | anime | mastery 38% | KEPT | no | repo | unchanged, plus the character flair |
| 7 | `rouge` | Red Thread of Fate: Rouge | 運命の赤い糸 ルージュ | anime | `red_thread` cast 5 x AND mastery 50% | KEPT | yes | ja_search | unchanged, plus the character flair |
| 8 | `dancing_doll_recall` | Dancing Doll: Recall | - | anime | `dancing_doll` cast 3 x AND mastery 60% | KEPT | yes | repo | unchanged, plus the character flair |
| 9 | `reinforcement` | Reinforcement Magic and Mana Skin | - | anime | mastery 75% | KEPT | yes | repo | unchanged, plus the character flair |

Canon spells with no page (documented, ignored by the engine):

* Door of Fate (Compound Magic with Finral) (anime; en_wiki): Listed on her wiki page as a compound spell; the page lives in the Spatial book and is in Finral's set.
* Thread Whip / Thread Binding Magic / Thread Cocoon (manga; design): Names from the mobile game databases only (not the series): no page.

Notes: No new page was needed: every canon spell she used already had a page in the Thread book. The Black Bulls flair (a thread web ring, a burst of strings, the Rouge fate tap aura) is added on top of her casts by book.CharacterFlairBulls.

### Charmy (`charmy`, book `book_cotton`, magic COTTON)

| # | Page id | Canon name | Japanese | Origin | Opens when | Page | Exclusive | Source | VFX layers |
|---|---|---|---|---|---|---|---|---|---|
| 1 | `sleeping_sheep_strike` | Cotton Creation Magic: Sleeping Sheep Strike | 眠れる羊の一撃 | anime | from the start | KEPT | no | en_wiki | unchanged, plus the character flair |
| 2 | `sheep_cook` | Cotton Creation Magic: Sheep Cook | ヒツジのコックさん | anime | from the start | KEPT | no | en_wiki | unchanged, plus the character flair |
| 3 | `cotton_cloud` | Cotton Magic: Cotton Cloud | 綿雲 | anime | mastery 16% | KEPT | no | ja_search | unchanged, plus the character flair |
| 4 | `sheep_bondage` | Cotton Magic: Sheep Bondage | - | anime | mastery 25% | KEPT | no | ja_search | unchanged, plus the character flair |
| 5 | `fluffy_sheep_cushion` | Cotton Magic: Fluffy Sheep Cushion | 羊のモコモコクッション | anime | mastery 33% | NEW | yes | ja_search | COTTON_SHEEP (following) + SPIRIT_AURA + MAGIC_CIRCLE_EXPLOSION + cast circle |
| 6 | `gluttons_banquet` | Food Magic: Glutton's Banquet | 大食らいの晩餐会 | anime | `sheep_cook` cast 3 x AND mastery 60% | KEPT | yes | repo_wiki | unchanged, plus the character flair |

Canon spells with no page (documented, ignored by the engine):

* Cotton Magic: shrinking herself to palm size / shrinking a foe and wrapping it in cotton (anime; ja_search): Described in one Japanese summary only; no spell name.
* Gourmet's Bite (anime; repo): Already a page of the Cotton book; no source for it could be found this round, so it is not in her set (it stays in the basic book).
* Mana Method cooking of an opponent (Food + Cotton) (anime; en_wiki): Late-series combination; no spell name found.

Notes: Charmy's Food side is Glutton's Banquet only, which is the one Food spell the sources agree on. Her adult dwarf form (chapter 194) is a transformation, not a spell, and is not part of the book.

### Luck (`luck`, book `book_lightning`, magic LIGHTNING)

| # | Page id | Canon name | Japanese | Origin | Opens when | Page | Exclusive | Source | VFX layers |
|---|---|---|---|---|---|---|---|---|---|
| 1 | `thunder_boots` | Lightning Creation Magic: Thunder God's Boots | 雷創成魔法「雷神の長靴」 | anime | from the start | KEPT | no | en_wiki | unchanged, plus the character flair |
| 2 | `thunderclap_crumbling_orb` | Lightning Magic: Thunderclap Crumbling Orb | 迅雷の崩玉 | anime | from the start | NEW | yes | en_wiki | LIGHTNINGFX_CAST + FX_LIGHTNING_ARC + LIGHTNINGFX_BURST at the end + cast circle |
| 3 | `thunder_gods_gloves` | Lightning Creation Magic: Thunder God's Gloves | 雷創成魔法「雷神の手袋」 | anime | `thunder_boots` cast 2 x AND mastery 20% | NEW | yes | en_wiki | LIGHTNINGFX_ZONE (following, 20 s) + FX_LIGHTNING_ARC, one more arc a second to the foe |
| 4 | `keraunos` | True Lightning Magic: Keraunos | 真雷魔法「雷霆(ケラノウス)」 | anime | `thunder_boots` cast 3 x AND (flag `mana_zone` OR mastery 45%) | NEW | yes | ja_search | LIGHTNINGFX_CAST + LIGHTNING_SPEAR along the line + LIGHTNINGFX_BURST at both ends |
| 5 | `thunder_fiend` | Lightning Ultimate Magic: Lightning Battle Fiend (Thunder Fiend) | 雷究極魔法「雷霆刹那戦鬼」 | anime | `keraunos` cast 2 x AND mastery 60% | KEPT | yes | ja_search | unchanged, plus the character flair |
| 6 | `raging_lightning` | Raging Lightning | - | anime | `thunder_fiend` open AND mastery 70% | KEPT | no | owner | unchanged, plus the character flair |
| 7 | `black_lightning_battle_fiend` | Black Lightning Battle Fiend | - | anime | `thunder_fiend` cast 3 x AND mastery 85% | KEPT | yes | en_wiki | unchanged, plus the character flair |

Canon spells with no page (documented, ignored by the engine):

* Thunder Arrow (Compound Magic with Klaus and Pouly) (anime; ja_search): A compound spell made at the Royal Knights selection test; needs two other casters.
* Flame-Lightning Explosive Cannon (Union Magic with Magna) (anime; en_wiki): Union of Flame and Lightning; needs Magna.

Notes: Mode 0 of the Lightning book (Chain Lightning) is the book's default page and cannot be hidden, so it stays although no source names it for Luck. Not in his grimoire (kept in the basic Lightning book, no source found for Luck): Thunder Lance, Pulsaranta, God of Lightning Rising Salim, Thunderbird Cavalry, Raijin's Thunder Drums, Lightning Surge.

### Magna (`magna`, book `book_fire`, magic FLAME)

| # | Page id | Canon name | Japanese | Origin | Opens when | Page | Exclusive | Source | VFX layers |
|---|---|---|---|---|---|---|---|---|---|
| 1 | `exploding_fireball` | Flame Magic: Exploding Fireball | 爆殺轟炎魔球 | anime | from the start | KEPT | no | ja_search | unchanged, plus the character flair |
| 2 | `exploding_buckshot` | Flame Magic: Exploding Buckshot | 爆殺散弾魔球 | anime | mastery 10% | NEW | yes | ja_search | FLAMEFX_CAST + 2 x FLAME_TRAIL (the fan) + FLAMEFX_BURST |
| 3 | `grand_slam` | Flame Creation Magic: Grand Slam | 愚乱怒守羅夢 | anime | `exploding_fireball` cast 4 x AND mastery 20% | NEW | yes | en_wiki | FLAMEFX_CAST + FLAME_TRAIL + FLAMEFX_BURST (+ 3 bolts) |
| 4 | `soul_chain_deathmatch` | Soul Chain Tag Deathmatch | 魂炎鎖死決闘 | anime | `grand_slam` cast 3 x AND mastery 65% | NEW | yes | en_wiki | FLAMEFX_CAST + CHAIN_FX1 (8 s) + FLAMEFX_ZONE on the foe + SPIRIT_AURA on Magna |

Canon spells with no page (documented, ignored by the engine):

* Flame Rope Binding Formation (anime; ja_search): Flame rope that holds the enemy and its magic; one Japanese summary only (a Wikipedia-style entry disagrees), no page.
* Gambling Base Run (anime; ja_search): Flame around the legs for speed; a single source, no page (Fire has no free page slot left for it: the unlock mask is 32 bits).
* Prison Kill Buckshot / Extreme Killing Vanishing Fireball (anime; fan_wiki): Names from a fan list only (a fireball that vanishes and strikes from another angle).
* Flame-Lightning Explosive Cannon (Union Magic with Luck) (anime; en_wiki): Union of Flame and Lightning; needs Luck.

Notes: Everything in the Fire book that is Fuegoleon's, Mereoleona's or Leopold's is hidden from Magna's grimoire (Sol Linea, Calderos, Leo Rugiens, Spiral Flame, Wild Bursting Flame, Ignis Columna, Calidos Brachium, Crimson Lion King, Flame Armour, Phoenix Flare and the Salamander pages). Casting: his pages need Magna's Soul Bat in the main hand (existing firecast hook); this builder did not change it.

### Gauche (`gauche`, book `book_mirror`, magic MIRROR)

| # | Page id | Canon name | Japanese | Origin | Opens when | Page | Exclusive | Source | VFX layers |
|---|---|---|---|---|---|---|---|---|---|
| 1 | `reflect_refrain` | Mirror Magic: Reflect Refrain | リフレクト・リフレイン | anime | from the start | KEPT | no | en_wiki | unchanged, plus the character flair |
| 2 | `reflect_ray` | Mirror Magic: Reflect Ray | リフレクト・レイ | anime | from the start | KEPT | no | en_wiki | unchanged, plus the character flair |
| 3 | `real_double` | Mirror Magic: Real Double | リアル・ダブル | anime | `reflect_ray` cast 3 x AND mastery 15% | KEPT | yes | en_wiki | unchanged, plus the character flair |
| 4 | `large_reflect_ray` | Mirror Magic: Large Reflect Ray | ラージ・リフレクト・レイ | anime | mastery 25% | KEPT | no | ja_search | unchanged, plus the character flair |
| 5 | `double_reflect_refrain` | Mirror Magic: Double Reflect Refrain | ダブル・リフレクト・リフレイン | anime | `real_double` cast 2 x AND mastery 35% | NEW | yes | en_wiki | MIRROR_ARRAY + 2 x MIRROR_RAY (the two bounces) + MIRROR_SHATTER |
| 6 | `full_reflection` | Mirror Magic: Full Reflection | フル・リフレクション | anime | mastery 50% | KEPT | yes | en_wiki | unchanged, plus the character flair |
| 7 | `mirrors_brigade` | Mirror Magic: Mirrors Brigade | ミラーズブリゲイド | anime | `full_reflection` open AND mastery 60% | NEW | yes | en_wiki | MIRROR_FRAME (following, 8 s) + MIRROR_ARRAY ring + MIRROR_SHATTER each second a spell is turned back |
| 8 | `reflect_iris` | Union Magic: Reflect Iris | - | anime | mastery 72% | KEPT | no | repo_wiki | unchanged, plus the character flair |
| 9 | `mirrors_meteorite` | Compound Magic: Mirrors Meteorite | - | anime | `mirrors_brigade` open AND mastery 85% | KEPT | yes | repo_wiki | unchanged, plus the character flair |

Canon spells with no page (documented, ignored by the engine):

* Mirror Magic: communication and mirror travel (anime; en_wiki): Used for two-way talk and moving between mirrors; no spell names found.

Notes: Not in Gauche's grimoire (kept in the basic Mirror book, no source found): Mirror Array, Mirror Step, Mirrors Slash, Glass Garb.

### Gordon (`gordon`, book `book_poison`, magic POISON)

| # | Page id | Canon name | Japanese | Origin | Opens when | Page | Exclusive | Source | VFX layers |
|---|---|---|---|---|---|---|---|---|---|
| 1 | `venom_bolt` | Poison Curse Magic: Aufwachen Dachs | 寝起きの穴熊 | anime | from the start | KEPT | no | repo_wiki | unchanged, plus the character flair |
| 2 | `toxic_cloud` | Poison Magic: Violett Schirm | 紫の傘 | anime | mastery 12% | KEPT | no | en_wiki | unchanged, plus the character flair |
| 3 | `poison_clouds_abode` | Poison Curse Magic: Poison Cloud's Abode | 毒雲の棲家 | anime | mastery 20% | NEW | yes | ja_search | POISON_CURTAIN (8 s) + POISON_BREATH + CURSE_FX1 |
| 4 | `verbotenes_obst` | Poison Magic: Verbotenes Obst | 禁じられた果実 | anime | `toxic_cloud` cast 3 x AND mastery 30% | NEW | yes | en_wiki | POISON_BREATH (flight) + POISON_CURTAIN + POISON_BREATH (burst) |
| 5 | `curse_workers_neighbor` | Curse-Worker's Neighbor | - | anime | `poison_clouds_abode` cast 2 x AND mastery 60% | KEPT | yes | en_wiki | unchanged, plus the character flair |

Canon spells with no page (documented, ignored by the engine):

* Ultra Giant Bull (Compound Magic: Poison, Recombination, Transmutation, Mirror) (anime; repo_wiki): The page lives in the Recombination book and is in Henry's set; Gordon's part is Curse-Worker's Neighbor.

Notes: Not in Gordon's grimoire (kept in the basic Poison book, no source found): Basilisk's Breath (plague nova) and Acid Ball.

### Zora (`zora`, book `book_ash`, magic ASH)

| # | Page id | Canon name | Japanese | Origin | Opens when | Page | Exclusive | Source | VFX layers |
|---|---|---|---|---|---|---|---|---|---|
| 1 | `ash_bullets` | Ash Magic: Ash Bullets | - | anime | from the start | KEPT | no | mobile_wiki | unchanged, plus the character flair |
| 2 | `ash_snare_trap` | Trap Magic: Ash Snare Trap | 括り罠 | anime | from the start | NEW | yes | ja_search | ASH_FORMATION (8 s) + ASH_FX2 + ASH_FX3 when it springs |
| 3 | `ash_absorbing_formation` | Ash Curse Magic: Ash Absorbing Formation | - | anime | mastery 20% | KEPT | yes | repo_wiki | unchanged, plus the character flair |
| 4 | `counter_trap_magic` | Trap Magic: Counter Trap Magic | カウンタートラップ魔法 | anime | `ash_snare_trap` cast 3 x AND mastery 35% | NEW | yes | en_wiki | ASH_FORMATION (following, 10 s) + ASH_FX2 + ASH_FX3 and FX_SPIRIT_NOVA for each spell sent back |
| 5 | `revelation_of_the_cowardly` | Ash Magic: Revelation of the Cowardly | - | anime | `counter_trap_magic` open AND mastery 50% | KEPT | yes | en_wiki | unchanged, plus the character flair |

Canon spells with no page (documented, ignored by the engine):

* Trap Magic: pitfall and mine traps (anime; ja_search): Two more of the three quick traps; no names, the Snare Trap stands for the family.

Notes: Not in Zora's grimoire (kept in the basic Ash book, no source found): Ash Cloud, Ash Shroud, Ash Soldiers, Cursed Ash. Magic Warding Ash is World Tree Magic (William's) and not an Ash spell.

### Grey (`grey`, book `book_transmutation`, magic TRANSMUTATION)

| # | Page id | Canon name | Japanese | Origin | Opens when | Page | Exclusive | Source | VFX layers |
|---|---|---|---|---|---|---|---|---|---|
| 1 | `iron_spikes` | Transmutation: Iron Spikes | - | anime | from the start | KEPT | no | ja_search | unchanged, plus the character flair |
| 2 | `transformation_guise` | Transformation Magic: Borrowed Form | 変身魔法 | anime | from the start | NEW | yes | en_wiki | ALCHEMY_CIRCLE + TRANSMUTATION_FX1 + MAGIC_CIRCLE_EXPLOSION |
| 3 | `magic_convert` | Transmutation Magic: Magic Convert | マジックコンバート | anime | `transformation_guise` cast 2 x AND mastery 30% | KEPT | yes | ja_search | unchanged, plus the character flair |

Canon spells with no page (documented, ignored by the engine):

* Transformation of rocks and other objects (anime; en_wiki): Described only in general terms; no spell name.
* Ultra Giant Bull (Compound Magic: converts a structure into the house's material) (anime; repo_wiki): The page lives in the Recombination book and is in Henry's set.

Notes: The sources for Grey are the thinnest of the squad: her magic is listed as Transformation although the series reveals it to be Transmutation, and no spell names were found. Not in her grimoire (kept in the basic Transmutation book): Quagmire, Grand Transmutation, Alchemic Blades, Flowering, Transmuted Sentinel, Alchemist's Armour.

### Henry (`henry`, book `book_recombination`, magic RECOMBINATION)

| # | Page id | Canon name | Japanese | Origin | Opens when | Page | Exclusive | Source | VFX layers |
|---|---|---|---|---|---|---|---|---|---|
| 1 | `mana_corkscrew` | Recombination Magic: Mana Corkscrew | マナ・コークスクリュー | anime | from the start | KEPT | no | en_wiki | unchanged, plus the character flair |
| 2 | `raging_black_bull` | Recombination Magic: Raging Black Bull | 黒の暴牛号 | anime | `mana_corkscrew` cast 3 x AND mastery 20% | KEPT | yes | en_wiki | unchanged, plus the character flair |
| 3 | `mana_rocket_punch` | Recombination Magic: Mana Rocket Punch | マナロケットパンチ | anime | `raging_black_bull` open AND mastery 30% | NEW | yes | en_wiki | RECOMBINE_CONSTRUCT + RECOMBINATION_FX1 (flight and impact) |
| 4 | `mini_bull` | Recombination Magic: The Mini Bull | - | anime | `raging_black_bull` open AND mastery 40% | NEW | yes | en_wiki | RECOMBINE_CONSTRUCT + RECOMBINATION_FX1 + EARTH_SPIKES |
| 5 | `room_rebuild` | Recombination Magic: Room Rebuild | - | anime | mastery 50% | KEPT | no | ja_search | unchanged, plus the character flair |
| 6 | `room_swap` | Recombination Magic: Room Swap | - | anime | mastery 60% | KEPT | no | en_wiki | unchanged, plus the character flair |
| 7 | `ultra_giant_bull` | Compound Magic: Ultra Giant Bull | - | anime | `raging_black_bull` cast 3 x AND mastery 85% | KEPT | yes | repo_wiki | unchanged, plus the character flair |

Notes: Not in Henry's grimoire (kept in the basic Recombination book, no source found): Bulwark, House Armour, Plank Snare.

### Finral (`finral`, book `book_spatial`, magic SPATIAL)

| # | Page id | Canon name | Japanese | Origin | Opens when | Page | Exclusive | Source | VFX layers |
|---|---|---|---|---|---|---|---|---|---|
| 1 | `fallen_angel_gate` | Spatial Magic: Fallen Angel Gate | 堕天使の抜け穴 | anime | from the start | KEPT | no | en_wiki | unchanged, plus the character flair |
| 2 | `fallen_angel_wingbeat` | Spatial Magic: Fallen Angel Wingbeat | 堕天使の羽ばたき | anime | `fallen_angel_gate` cast 5 x AND mastery 30% | NEW | yes | en_wiki | SPACE_PORTAL + SPACE_RINGGATE (start) + SPACE_CRACK (line) + SPACE_RINGGATE (arrival) |
| 3 | `door_of_fate` | Compound Magic: Door of Fate | - | anime | `fallen_angel_wingbeat` cast 3 x AND mastery 70% | KEPT | no | en_wiki | unchanged, plus the character flair |

Canon spells with no page (documented, ignored by the engine):

* Spatial Support / Spatial Movement Magic Bullet (manga; en_wiki): Skill names of the mobile game only: no page.
* Spatial armour of his late training (anime; design): The mod's Dimensional Armour page exists but no source for it could be found for Finral, so it is not in his set.

Notes: Mode 0 of the Spatial book (Sacred Mana Domination) cannot be hidden and is not Finral's in the series. Not in his grimoire (kept in the basic Spatial book, no source found for Finral): Spatial Cut, Unopening Red Room, Myriad Black, Dimensional Armour, Gate Strike, Gate Swap, Dimension Collapse.

### Nacht (`nacht`, book `book_shadow`, magic SHADOW)

| # | Page id | Canon name | Japanese | Origin | Opens when | Page | Exclusive | Source | VFX layers |
|---|---|---|---|---|---|---|---|---|---|
| 1 | `shadow_step` | Shadow Magic: Invitation to the Dark Garden | 暗い園の誘い | anime | from the start | KEPT | no | ja_search | unchanged, plus the character flair |
| 2 | `shadow_travel` | Shadow Magic: Shadow Corridor | 影の回廊 | anime | mastery 8% | KEPT | no | en_wiki | unchanged, plus the character flair |
| 3 | `unite_canis` | Devil Union Mode: Canis | - | anime | `devil_union` cast 1 x | KEPT | yes | repo | unchanged, plus the character flair |
| 4 | `dark_hunting_ground` | Shadow Magic: Dark Hunting Ground | 暗獄の狩り場 | anime | `unite_canis` open AND mastery 30% | NEW | yes | ja_search | SHADOW_POOL (6 s) + DARKFX_ZONE + SHADOW_UNITE |
| 5 | `unite_gallus` | Devil Union Mode: Gallus | - | anime | `devil_union` cast 2 x | KEPT | yes | en_wiki | unchanged, plus the character flair |
| 6 | `unite_gallus_call` | Gallus: Walgner's Call | - | anime | `unite_gallus` open | KEPT | yes | repo | unchanged, plus the character flair |
| 7 | `unite_felis` | Devil Union Mode: Felis | - | anime | `devil_union` cast 3 x | KEPT | yes | ja_search | unchanged, plus the character flair |
| 8 | `heavens_shadow_second_sight` | Shadow Magic: Heavenly Shadow Thousand-Mile Eye | 天影千里眼 | anime | `unite_felis` open | KEPT | no | ja_search | unchanged, plus the character flair |
| 9 | `unite_equus` | Devil Union Mode: Equus | - | anime | `devil_union` cast 4 x | KEPT | yes | ja_search | unchanged, plus the character flair |
| 10 | `unite_canis_felis` | Devil Union Mode: Canis x Felis | - | anime | `unite_canis` open AND `unite_felis` open AND mastery 80% | KEPT | yes | repo | unchanged, plus the character flair |
| 11 | `atonement_monument` | Shadow Magic: Atonement Monument | 贖罪の碑 | anime | `unite_equus` open AND mastery 85% | NEW | yes | en_wiki | SHADOW_POOL under each of up to 4 foes + DARKFX_BURST + SHADOW_UNITE |

Notes: Not in Nacht's grimoire (kept in the basic Shadow book, no source found): Kids' Playground and Shadow Realm. The order of the four Union Modes is this mod's design (the series names the four devils but not the order they were first united).

### Secre (`secre`, book `book_sealing`, magic SEALING)

| # | Page id | Canon name | Japanese | Origin | Opens when | Page | Exclusive | Source | VFX layers |
|---|---|---|---|---|---|---|---|---|---|
| 1 | `seal` | Seal Magic: Seal | 封緘魔法 | anime | from the start | KEPT | no | ja_search | unchanged, plus the character flair |
| 2 | `wound_sealing` | Seal Magic: Closed Sorrow (Wound Sealing) | - | anime | mastery 10% | KEPT | yes | en_wiki | unchanged, plus the character flair |
| 3 | `inverse_release` | Seal Magic: Inverse Release | - | anime | `wound_sealing` cast 2 x AND mastery 25% | NEW | yes | en_wiki | SEAL_BRANCH + SEAL_BASIC + SEAL_ORBIT on up to 3 freed allies |
| 4 | `branching_array` | Seal Magic: Branching Array (Mana Method) | - | anime | `inverse_release` open AND mastery 40% | KEPT | yes | owner | unchanged, plus the character flair |
| 5 | `orbital_bind` | Seal Magic: Orbital Bind | - | anime | mastery 50% | KEPT | yes | owner | unchanged, plus the character flair |
| 6 | `eternal_prison` | Seal Magic: Eternal Prison | - | anime | `inverse_release` cast 2 x AND mastery 80% | KEPT | yes | en_wiki | unchanged, plus the character flair |

Canon spells with no page (documented, ignored by the engine):

* Forbidden Art against the devil (the black horns) (anime; ja_search): The act that turned her into the bird Nero for 500 years; no spell name.

Notes: Not in Secre's grimoire (kept in the basic Sealing book): Grand Seal, Sealing Chains, Trinity Seal Magic (a compound spell of other mages), Seal Barrier and the Sigil Combo (the basic seal's random combo).


## 7. NEW / REPLACED / KEPT

| Part | Verdict |
|---|---|
| The twelve data files (`data/nusmp/character_spells/<id>.json`), `book.CharacterPagesBulls`, `book.CharacterArtsBulls` (20 pages and casts), `book.CharacterFlairBulls`, `charset.BullsFlairPalette`, the self-test `magic.character_spells_bulls`, the lang fragment `tools/lang/character_spells_bulls.json`, `CharacterSetBullsTest` | NEW |
| A grimoire of one of the twelve showing only his pages and opening them by his rule | REPLACED (the old behaviour: the whole book opening by mastery order; with the switch off it is back) |
| The basic grimoire hiding the signature pages of the twelve (Keraunos, Counter Trap Magic ...) | REPLACED (they were listed but locked or open to anyone); `basicBookHidesSignatures=false` restores it |
| Every page id, name, order, cast, cost and unlock bit of the twelve shared books | KEPT |
| The lock lines of `CharacterSpells` (Vanessa's, Luck's, Gordon's, Henry's, Zora's, Nacht's, Secre's pages), the Soul Bat casting, the Unite code, the rank gates and free slots | KEPT |
| Letoile, and every character of the Golden Dawn / Crimson Lion / Silver Eagle and the other squads | untouched (other builders) |

Pages of a shared book that a character never used are only hidden from him, they stay in the basic book (the list per character is in the `notes` of each file and in the tables). The shared pages that now disappear from the basic book because they are marked `exclusive`: Vanessa's Thread Trap, Arresting Dark Weave, Rouge, Dancing Doll: Recall, Reinforcement; Charmy's Glutton's Banquet; Luck's Thunder Fiend, Black Lightning Battle Fiend, Keraunos and the other two new ones; Magna's three new pages; Gauche's Real Double, Double Reflect Refrain, Full Reflection, Mirrors Brigade, Mirrors Meteorite; Gordon's Curse-Worker's Neighbor and the new two; Zora's Ash Absorbing Formation, Revelation of the Cowardly and the new two; Grey's Borrowed Form and Magic Convert; Henry's Raging Black Bull, Ultra Giant Bull and the new two; Finral's Fallen Angel Wingbeat; Nacht's Unite pages and the new two; Secre's Wound Sealing, Inverse Release, Branching Array, Orbital Bind, Eternal Prison.

## 8. Exact edits to existing files (all additions, one-liners)

| File | Edit |
|---|---|
| `book/ElementBook.java` | `familyPages()` appends `CharacterPagesBulls.forMagic(magic.name(), pages)` (cached); one field + the method line. Covers Cotton, Poison, Ash, Transmutation, Recombination and Shadow, and returns the same list for every other ElementBook |
| `book/LightningBook.java`, `MirrorBook.java`, `SpatialBook.java`, `SealingBook.java` | the same cached wrapper in their own `familyPages()` |
| `book/FireBook.java` | `CharacterPagesBulls.fire(CharacterPages.fire(pages))` (the pilot's pages first, then Magna's) |
| `book/CharacterPages.java` | `isAdded` also answers for the twin class (one line) |
| `book/GrimoireBook.java` | one line after `CharacterSets.onCast`: `CharacterFlairBulls.onCast(player, instance, p)` |
| `charset/CharacterSetRegistry.java` | the twelve ids appended to `SHIPPED` |
| `charset/CharacterSetBoot.java` | `CharacterSetBullsSelfTests.register()` |
| `charset/CharacterSetSelfTests.java` | the pilot's "added pages follow the older pages" check finds the block by its first page instead of assuming it is last (Magna's pages follow the pilot's in the Fire book) |
| `data/nusmp/armour_policy.json` | `new_pages` for COTTON, LIGHTNING, MIRROR, POISON, ASH, TRANSMUTATION, RECOMBINATION, SPATIAL, SHADOW, SEALING (new lines) and FLAME (three ids appended to the pilot's array) |
| `test/.../charset/CharacterSetSourceScanTest.java` | `everyPageAFileNamesExistsInItsBook` skips books outside its four (the squads' books have their own test) and also reads the twin class's pages for the Fire book |

No version bump, no CHANGELOG / README / LIBRARIES / en_us / `.github` edit (the lang text is the fragment). The self-test prefix `magic.character_spells_bulls` has to be added to the CI smoke list by whoever owns `.github`.

## 9. Server and performance (rules 9 and 11)

* The server decides everything: the rules are evaluated by the engine on the server; the client only asks "is this page hidden". No packet type was added.
* Per player state: the grimoire's own use counters (engine) and one long in the player's persistent data (`nusmp_flair_tick`, the last flair); nothing static per player, nothing to clean on logout, death or dimension change. The 20 casts keep no state outside `SpellRuntime` zones and bolts, which end by time.
* Worst case per cast: at most 10 effect packets (Atonement Monument: cast circle, 4 pools, 2 shapes, 3 flair layers), usually 4 to 7; one zone of at most 20 s (the longest is the Gloves, 400 ticks, a step every 20 ticks with one hit and one packet), steps every 5 to 20 ticks, every hit loop capped at 8 live targets (4 for the Monument), up to 3 bolts of 18 ticks (Grand Slam), no entity spawned, no block placed, no chunk loaded (the Wingbeat tests `hasChunkAt` before it teleports a foe), cooldowns 8 to 90 s. Worst case with 50 players each casting the heaviest page once per cooldown: about 10 packets per 10 s per player, about 50 packets a second server wide, inside the existing per-tick budget (`maxEffectPacketsPerTick`, 160) which also guards the flair; the flair itself is 1 to 3 packets per cast and at most one per 6 ticks per player.
* Client draw cost: no new shape, so the existing mesh / quad budgets of each layer apply; the flair adds at most 3 existing short effects per cast, none for far players (the packets go to players within 96 blocks only).
* `CharacterSetBullsTest` pins these limits (no world scan, no entity spawn, zones at most 400 ticks, every hit loop capped, at most 8 packets per cast in the source) the same way the pilot's scan test does. `PerfSourceScanTest` needed no new pattern: the casts use the patterns the pilot's already use (`SpellRuntime.zone`, `bolt`, `VfxSpawn`).

## 10. Research record and what could not be verified (rule 10)

Rule 10: English wiki first, Japanese as the fallback. The fandom hosts (blackclover.fandom.com and every other wiki), ja.wikipedia.org and web.archive.org failed with `getaddrinfo ENOTFOUND` / were blocked from the build machine, so the only way in was the search tool. English searches were run first for every character (the wiki's character page and spell pages, shown in the search summaries); Japanese searches (pixiv encyclopedia, manga-tantei, manga-legend, RENOTE, playnews, the official bclover.jp / TV Tokyo pages and the 必殺技辞典) were the fallback for names, effects and users. Each spell in each file says which source it came from (`en_wiki`, `ja_search`, `repo_wiki` = the fandom spell pages already cached in `.github/wiki`, `owner` = the owner's own specs, `repo` = a name carried over from earlier releases, `mobile_wiki`, `fan_wiki`, `design`).

Could NOT be fetched at all: any fandom page directly (only search summaries), ja.wikipedia, the Japanese fandom wiki, namu.wiki pages (only summaries), chapter and episode numbers of nearly every spell's first use.

NOT verified (honest list):

* The chapter or episode where each spell first appeared. Only these were found: Rouge in episode 64, Shadow Corridor in chapter 274 (a fan wiki), Charmy's dwarf awakening in chapter 194. The ORDER in each file is therefore the series' arcs as far as the sources said and this mod's design otherwise.
* The effects of Mini Bull (name only), Mirrors Brigade, Atonement Monument, Thunderclap Crumbling Orb, Thunder God's Gloves, Fluffy Sheep Cushion, Borrowed Form (inferred from the name or from one line of summary).
* Names the repo already had but the sources did not confirm (Vanessa's Arresting Dark Weave, Dancing Doll and its Recall, Reinforcement; Nacht's union pages; Luck's six pages that are hidden from him: Thunder Lance, Pulsaranta, God of Lightning Rising Salim, Thunderbird Cavalry, Raijin's Thunder Drums, Lightning Surge). They stay in the shared books; if the owner knows one of them is canon for a character, it is one line in his JSON.
* Grey's whole set (the sources only say "Transformation" and mention Magic Convert); Gordon's Basilisk's Breath; Zora's Ash Cloud / Shroud / Soldiers / Cursed Ash; Henry's Bulwark / House Armour / Plank Snare; Finral's other Spatial pages; Gauche's Mirror Array / Step / Slash.
* The Japanese names are as the search summaries wrote them (a few differ between sites: ミラーズブリゲイド / ブリケイト, 炎雷爆尽砲 / 爆陣砲 / 爆発砲).
* The Fire book would have 16 family pages with Magna's three; the unlock mask allows 24. The Crimson Lion builder's pages and any other appended to the Fire book must keep the total at 24 or less (`CharacterSetBullsTest.noBookGoesOverTheTwentyFourFamilyPagesTheUnlockMaskAllows` counts only this squad's and the pilot's; the self-test counts the real book).
* Nothing was seen or heard in a game: the pages opening in order, the "-" for a hidden page in the page list, the casts, the flair, the Soul Bat swing with the new Fire pages, the counts in the Codex and status panel.
* Tests that need Minecraft classes cannot run offline in this shim (`MagicEnumAuditTest`, `GrimoireBookPlanTest`, `CanonBookMagicTest`, `FusionDesignTest`, `GrimoireDesignGeneratorTest`, `MagicAuditTest`, `GrimoireShelfLayoutTest`, `GrimoireCarryTest`, `SpiritLightRulesTest`, `LightMoodRulesTest` and similar).
* The lang audit (every page has text) reads `en_us.json` and will pass after the fragment `tools/lang/character_spells_bulls.json` is merged.
