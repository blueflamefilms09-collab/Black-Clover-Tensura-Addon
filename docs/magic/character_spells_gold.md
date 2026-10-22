# Character spell sets, builder "gold": the Crimson Lion, Golden Dawn, Silver Eagle and Wizard King side

Owner request (phase 2): the character's grimoire shows the spells that character used in the anime, in the order he got them, with a faithful rule for how each was acquired; his signature spells are his alone; and the character grimoire effects look far richer than the basic grimoire's. The engine is `charset` (`docs/magic/character_spells.md`); the five pilots (Yuno, Noelle, Fuegoleon, Asta, Yami) were done before. This document is the part of twelve more characters: Mereoleona, Leopold, Mimosa, William, Klaus, Langris, Lemiel, Julius, Kirsch, Karna, Nozel and Kaiser. Other builders own the Black Bulls ("bulls") and the other captains and kingdoms ("rest"); none of their characters was touched.

Nothing here was seen in a game. What was run: the pure rules and the shipped files by JUnit (`charset.CharacterSetGoldTest` and the engine's own tests), the sources by scan tests, the type check against the API stubs. What the CI server smoke test will check in the real game: the self-test `magic.character_spells_gold`.

The existing server switch `magic.characterSpellSets` (`nusmp-character-spells-server.toml`, default `true`) turns everything here back: `false` gives every grimoire the whole book and the mastery order, as before. The added pages stay locked to their character with the switch off.

## 1. NEW / REPLACED / KEPT

| Part | Verdict |
|---|---|
| 12 character files (`data/nusmp/character_spells/<id>.json`), 12 ids appended to `CharacterSetRegistry.SHIPPED` | NEW |
| 26 pages appended to 7 books (Fire 6, Plant 4, World Tree 3, Steel 4, Spatial 3, Light 5, Storm 1) with their casts, texts and policy lines | NEW |
| Grimoires bound to these twelve characters show their own spells and open them by their rule | REPLACED (the whole book in mastery order; with the switch off it is back) |
| The basic grimoire hides the pages marked exclusive here (see section 6) | REPLACED (they were listed; `basicBookHidesSignatures` false restores it) |
| `CanonBook.KIRSCH` magic `PLANT` to `CHERRY_BLOSSOM` | REPLACED (one word; a decision for the owner, section 9) |
| Every page id, name, order, cast, cost, cooldown and unlock bit of the existing books; the old locks of `CharacterSpells`; the fire casting styles; the pilots' files and pages | KEPT |
| Time Magic VFX, `TimeBook`, `TimeMagicLayer`, the time textures | KEPT (untouched; Julius's set only names existing Time pages) |
| Characters of the other builders | not touched |

Characters skipped: none of this builder's list was skipped. All twelve have a `CanonBook` entry. Two of them are drawn in a book that is not their series' magic because `CanonBook` binds them to it (Karna: Moonlight Magic in the Light book; Kaiser: Vortex Magic in the Storm book; section 9).

## 2. How the sets were made (rule 10: English wiki first, Japanese as the fallback)

Every fandom host was reported as not fetchable from this machine by the phase brief, so this builder used `WebSearch` (mode extended) only and did not retry the hosts. A search returns a summary of the page it found, so every wiki fact below is second hand. Searches run, English first:

* Mereoleona, Leopold, Mimosa, William, Klaus, Langris, Lemiel, Julius, Kirsch, Karna, Nozel, Kaiser: "<name> Black Clover wiki abilities spells ..." (English). Found: Mereoleona (Calidus Brachium, Barrage, Purgatory with Cremation and Abyss variants, Hellfire Incarnate, Mana Zone), Mimosa (Magic Flower Guidepost, Magic Flower Cannon, Dream-Healing Flower Cradle, Princess-Healing Flower Robe, Flower Princess Utopia), William (the World Tree Magic page: Mistilteinn Seed, Dracaena Formation, Magic Tree Descent, Great Tree Mistilteinn, Magic Warding Ash, Budding of Yggdrasil, Mistilteinn Blade, the union Spirit Light Tree), Klaus (Steel Magic page; wall, lances, Thunder Arrow union), Lemiel (Lamp of Avior, Lamp of Avior Gloria, light sword and arrow, Mana Zone, the union Protecting Light), Julius (Chrono Stasis, Time Acceleration, Time Reversal, Chrono Anastasis, Mana Zone, stolen time), Kirsch (Cherry Blossom Magic: Clones of the Beautiful Me, Magic Cherry Blossom Blizzard), Karna (Moonlight Magic page: Crescent Cut, Bewitching Moonlight, Gibbous Ray, Lunatic Crisis, Moon Trap, Full Moon Shell, Crescent Cut Full Moon), Nozel (Silver Rain, Silver Spear, Silver Star of Execution, Silver Shining Regal Spear, Silver Bullet, Silver Blade, Silver Guardian, Silver Eagle), Kaiser (Vortex Magic, no spell names), Langris (Spatial Magic, House Vaude; Archangel's Crash and Destruction from one database).
* Japanese fallback: Klaus (施貫の烈槍, 鋼城の鎧壁, 旋貫の激槍, 神鳴の矢 from manga-tantei), Langris (大天使の撃墜, 大天使の撃滅, the auto-guard after the elf awakening), Kaiser (螺旋結界 エリアヘリックス from ja.wikipedia and namu), Leopold (螺旋焔, 爆乱焔, 深炎螺旋焔, 真炎魔法「紅蓮噴火」 from pixiv and namu), Mereoleona (灼熱腕, 連撃, 煉獄, 煉獄・深淵, 業火の化身 from bclover.jp and manga-tantei).
* Source ids in the files: `en_wiki` (English wiki through search summaries), `ja_search` (Japanese pages through search summaries), `design` (the mod's own placement or mapping, named as such in the entry's note). No entry rests on the builder's memory alone.

What could not be fetched at all: every blackclover.fandom.com page itself (only the search summaries of them), the Japanese fandom wiki, the chapter and episode numbers of nearly every spell (so the ORDER of acquisition is the mod's design, marked in each file), and the Japanese names of most Mimosa, William, Karna, Nozel and Julius spells (left empty rather than guessed). The `origin` column says `anime` where the spell appears on the wiki lists as a series spell and `manga` where a Japanese source named it as a late (Spade Kingdom / Heart Kingdom era) spell; the anime/manga split was NOT verified per spell, except where the English wiki says anime-only.

## 3. How they cast (the owner's "how they cast spells")

| Character | What the repo already did, and what this builder added |
|---|---|
| Mereoleona | The existing `firecast` style `MEREOLEONA` (the key readies the page, a punch combo releases it from the fist) wraps every Fire family page, so her four new pages are cast with the fist with no extra code. The pages use the `thrust`, `slam` and `signature` release clips (a punch for the Barrage, a stamp for the Purgatory family). |
| Leopold | The existing style `LEOPOLD` (hold the key to wind a spiral, release throws it) wraps the two new pages the same way; Deep Spiral Flame sends its first spiral from the emitter and three more from his body. |
| Mimosa | Healing pages target the ally you look at (or you), with a vine of light from the hand to the target; the cannon opens a giant flower in front of her and fires from its mouth. |
| William | The world tree grows over him (canopy, roots, ash tree) and the drain flows from each foe to him. |
| Klaus | Steel forms in front of the hand (`WEAPON_CONSTRUCTS`) and flies along `THREAD_LINE` lanes; the wall rises round him; the Thunder Arrow is a steel arrow with a lightning core and leaves him slowed for 2 s ("hard to control after the shot"). |
| Langris | Strokes of space (`SPACE_CRACK`, `SPACE_TEAR`, `SPACE_SHATTER`) from the eye line; the auto-guard follows him as a ring gate. |
| Karna | Crescents leave the hand, the shell and the moon appear on her, and the Lunatic Crisis is charged for 1.5 s under a red flare before the ray. |
| Kaiser | A vortex turns where he aims. |
| Lemiel, Julius, Kirsch, Nozel | Existing pages only: their casting is the existing page's. |

Release clips (`BookPage.withAnim`, existing clips of `anim.CastAnim`): `thrust`, `up`, `sweep`, `slam`, `out`, `signature`. Not done: a required cast item (a bat for Magna is the bulls builder's), any keybind (the owner has too many keys), and a special posture for Klaus's grimoire-less casting.

## 4. The pages this builder added, how they look and what they do

All 26 are appended after the older pages (ids new, nothing moved), locked to their character (`CharacterSpells.owners` through `CharacterPagesGold.isAdded`), with the usual tier cost and an explicit cooldown (mid 200 to 600 ticks, zone 400 to 1200, signature 900 to 2400). Damage is the usual base damage that goes through the balance law. "Layers" are existing `VfxShape`s in the character's palette (the same colour on every layer of one cast); `b.vfx` is used for the first layer where the casting hooks (fire emitter) must move its start. Effects marked (inferred) are the mod's reading of a name that no source described.

| Page (book) | Character | Tier / cooldown | What it does | VFX layers (packets per cast) |
|---|---|---|---|---|
| `mereoleona_brachium_barrage` (fire) | Mereoleona | mid / 15 s | 4 blasts in 16 ticks from four sides on the target, 5 each within 2.4 blocks, burning | circle, FLAMEFX_ZONE, 4 x FLAME_TRAIL, FIRE_BURST (7) |
| `mereoleona_purgatory` (fire) | Mereoleona | zone / 60 s | 20 s Strength II, fire immunity, ring of 3.5 blocks, 3 a second | circle, FIRE_PILLAR, following FLAMEFX_ZONE + SPIRIT_AURA, FLAMEFX_BURST every 5 s (4 + 4) |
| `mereoleona_purgatory_abyss` (fire) | Mereoleona | signature / 75 s | shock 7 blocks (14, thrown back), then 15 s Strength III, Speed I, ring of 4.5 blocks, 5 a second | circle, FX_FIRE_ERUPTION, the body layers (7) |
| `mereoleona_hellfire_incarnate` (fire) | Mereoleona | signature / 120 s | eruption 9 blocks (20), then 12 s Strength III, Speed II, absorption III, ring of 6 blocks, 6 a second | circle, FX_FIRE_ERUPTION, FIRE_PILLAR, the body layers (8) |
| `leopold_deep_spiral_flame` (fire) | Leopold | zone / 25 s | four 9-block spirals, up to 8 foes, 10 each, burning | circle, 4 x FIRE_SPIRAL, FIRE_BURST (6) |
| `leopold_crimson_eruption` (fire) | Leopold | signature / 50 s | circle of 7 blocks drawn at the aim point, erupts after 24 ticks: 22, burning, thrown up | circle, FLAMEFX_ZONE, then FX_FIRE_ERUPTION + FIRE_PILLAR + FLAMEFX_BURST (5) |
| `mimosa_magic_flower_cannon` (plant) | Mimosa | mid / 13 s | laser 24 blocks, 14, up to 8 foes | circle, PLANT_FX2, LIGHT_FLARE, PLANT_FX1, PLANT_FX3 (5) |
| `mimosa_dream_healing_cradle` (plant) | Mimosa | mid / 20 s | 4 pulses of 12 percent of max health on the ally, poison and wither removed | circle, PLANT_FX1, following PLANT_FX2 + PLANT_FX3 (4) |
| `mimosa_princess_healing_robe` (plant) | Mimosa | mid / 30 s | 20 s Regeneration II, absorption II, Resistance I on the ally | circle, PLANT_FX1, following PLANT_FX3 + PLANT_FX2 (4) |
| `mimosa_flower_princess_utopia` (plant) | Mimosa | signature / 90 s | 14 s garden of 8 blocks: allies +8 percent a second, one 40 percent rescue each, foes slowed | circle, PLANT_FX2, PLANT_FX3, TREE_FX2, LIGHT_FLARE (5) |
| `william_magic_warding_ash` (world tree) | William | mid / 25 s | 8 s: Resistance I, enemy spells within 6 blocks removed (4 a step) | circle, TREE_CANOPY, WORLD_TREE_FX2, TREE_ROOTS (4) |
| `william_great_tree_mistilteinn` (world tree) | William | zone / 45 s | 6 s roots of 9 blocks: up to 8 foes slowed, 3 a second, mana drained to him, rooted at the end | circle, TREE_CANOPY, TREE_ROOTS, WORLD_TREE_FX1, up to 3 TREE_DRAIN (7) |
| `william_mistilteinn_blade` (world tree) | William | mid / 10 s (inferred) | arc 8 blocks, 11, up to 8 foes, a little sap healing | circle, WORLD_TREE_FX1, TREE_FX3, TREE_DRAIN (4) |
| `klaus_piercing_lance` (steel) | Klaus | mid / 10 s | steel lance 29 blocks, pierces, 14 | circle, WEAPON_CONSTRUCTS, THREAD_LINE, end burst (4) |
| `klaus_castle_wall` (steel) | Klaus | mid / 25 s | 7 s Resistance II, absorption II, enemy spells within 4.5 blocks removed | circle, WEAPON_CONSTRUCTS, FX_EARTH_SPIKES, 2 x THREAD_LINE (5) |
| `klaus_spiral_lance` (steel) | Klaus | zone / 20 s | drill 22 blocks, pierces, 16, thrown up | circle, WEAPON_CONSTRUCTS, THREAD_LINE, WIND_GALE, EARTH_SPIKES, burst (6) |
| `klaus_thunder_arrow` (steel) | Klaus | signature / 45 s | 40 blocks at once, 20, stun 1.5 s, cuts spells, caster slowed 2 s | circle, LIGHTNING_SPEAR, THREAD_LINE, FX_LIGHTNING_ARC, LIGHTNINGFX_BURST (5) |
| `langris_archangel_crash` (spatial) | Langris | mid / 12 s | 18 blocks, 16, cuts spells on the line | circle, SPATIAL_FX1, SPACE_CRACK, SPACE_TEAR (4) |
| `langris_archangel_annihilation` (spatial) | Langris | signature / 50 s (inferred) | three strokes over 12 ticks, 12 each | circle, SPACE_RINGGATE, 3 x SPACE_CRACK, SPACE_SHATTER (6) |
| `langris_spatial_auto_guard` (spatial) | Langris | mid / 30 s | 12 s Resistance I, enemy spells within 5 blocks removed | circle, following SPACE_RINGGATE + SPATIAL_FX2, SPACE_CUBE (4) |
| `karna_crescent_cut` (light) | Karna | mid / 10 s | three crescents, 14 blocks, 7 each | circle, LIGHT_FLARE, 3 x LIGHT_BLADE, LIGHT_FX1 (6) |
| `karna_bewitching_moonlight` (light) | Karna | zone / 35 s | the target and up to two beside it held 3 s, weak 6 s | circle, LIGHT_BLADE, up to 3 following LIGHT_FX2, SPIRIT_AURA (6) |
| `karna_full_moon_shell` (light) | Karna | mid / 25 s | 6 s: foes within 3.8 blocks knocked back every 0.5 s, spells stopped, Resistance I | circle, following MAGIC_CIRCLE_HOLD + SPIRIT_AURA, LIGHT_FX3 (4) |
| `karna_crescent_cut_full_moon` (light) | Karna | zone / 35 s | 5 crescents over 40 ticks, 12 blocks, 6 each | circle, MAGIC_CIRCLE_HOLD, LIGHT_FX3, 5 x LIGHT_BLADE (8) |
| `karna_lunatic_crisis` (light) | Karna | signature / 90 s | 1.5 s charge, ray 40 blocks, 26, up to 8 foes, cuts spells | circle, LIGHT_FLARE, then LIGHT_BLADE, LIGHT_FX2, LIGHT_FLARE (5) |
| `kaiser_area_helix` (storm) | Kaiser | signature / 60 s | 8 s vortex of 8 blocks: foes dragged in, slowed, 3 a second, spells swallowed | circle, VORTEX_FX1, VORTEX_FX2, VORTEX_FX3, WIND_RING (5) |

"10x cooler" is the layering plan (three to eight shapes per cast, a palette per character, ground and air accents, a zone that keeps drawing for the long casts) and not a verified look: nothing was seen in a game and no preview tool exists for these shapes beyond the ones the repo already previews. Kept pages (all of Julius, Kirsch, Nozel, Lemiel, and the older pages of the others) draw exactly as before; no cast flair was added to them because that would need a hook in `GrimoireBook` shared with the other builders.

## 5. Server and performance (rules 9 and 11)

* The server decides everything. No packet was added. The client only asks "is this page hidden" (two map reads and a tag read) and draws the shapes it is sent.
* Per cast (worst case): at most 8 effect packets (the Barrage, Crescent Cut Full Moon, Hellfire and the Great Tree), each sent by `VfxSpawn` to players within 96 blocks and inside the per-tick packet budget; at most one `SpellRuntime.zone` per cast (ten pages have one) lasting at most 14 s (the longest, Utopia), stepping every 5 to 20 ticks; at most 8 foes hurt or moved per step (every `for (LivingEntity` loop is capped, checked by `CharacterSetGoldTest`); at most 4 enemy spells removed per step; at most one bolt; no entity is spawned, no block is placed, no chunk is loaded, nothing scans the world outside a zone step.
* A zone step is one box query of at most 9 blocks radius (plus one for projectiles in the ward pages). The heaviest steady cost is a 5-tick ward zone (Warding Ash, Castle Wall, Auto-Guard, Area Helix): 4 box queries a second for at most 14 s per cast. With 50 players each casting the heaviest page once per its cooldown (10 s at the shortest) that is at most about 200 small queries a second server wide, the same order as the pilots' zones.
* Cooldowns: mid 10 to 30 s, zone 20 to 60 s, signature 45 to 120 s. `CharacterSetGoldTest` and the self-test refuse any added page under 8 s (under 30 s for a signature).
* Cleanup: zones and delayed calls check that the caster is still alive and loaded (`gone(p)`) and end by their own time; effects are vanilla potion effects that expire; nothing is stored per player outside the grimoire's own tag (the use counts of the engine, at most a handful of ints per character).
* Rule-9 scan: `CharacterSetGoldTest.theCastsAreBoundedAndServerSafe` fails on any client class, spawn, block placement or unbounded loop in the two new classes.

## 6. Per-character tables (generated from the files)

"Opens when" is the acquisition rule; the rank and race gates and the free-slot rule of `MasteryPages` still apply on top. Page: NEW = added by this builder, KEPT = the page existed.

### Mereoleona Vermillion (`mereoleona`, book `book_fire`, magic `FLAME`)

| # | Page id | Canon name | Japanese | Origin | Opens when | Page | Exclusive | Source |
|---|---|---|---|---|---|---|---|---|
| 1 | `calidos_brachium` | Flame Magic: Calidus Brachium | 灼熱腕(カリドゥス・ブラキウム) | anime | from the start | KEPT | yes | en_wiki |
| 2 | `mereoleona_brachium_barrage` | Mana Zone: Flame Magic: Calidus Brachium Barrage | 灼熱腕 連撃 | manga | `calidos_brachium` cast 3 x AND (flag `mana_zone` OR mastery 40%) | NEW | yes | ja_search |
| 3 | `mereoleona_purgatory` | Flame Magic: Calidus Brachium Purgatory | 灼熱腕 煉獄 | manga | `mereoleona_brachium_barrage` open AND mastery 55% | NEW | yes | ja_search |
| 4 | `mereoleona_purgatory_abyss` | Flame Magic: Calidus Brachium Purgatory: Abyss | 灼熱腕 煉獄・深淵 | manga | `mereoleona_purgatory` cast 2 x AND mastery 70% | NEW | yes | ja_search |
| 5 | `mereoleona_hellfire_incarnate` | Hellfire Incarnate | 業火の化身 | manga | `mereoleona_purgatory_abyss` open AND (flag `mana_zone` OR mastery 88%) | NEW | yes | en_wiki |

Canon spells with no page (documented, ignored by the engine):

* Mana Skin (manga; en_wiki): A defensive technique named by one summary; no page: a passive, not a spell of the book.
* Calidus Brachium Purgatory: Cremation (manga; en_wiki): Named as a variant of Purgatory by the English summary; no description found, so it is folded into the Purgatory page.

Notes: Mode 0 of the Fire book (Exploding Fireball) is the book's default page and cannot be hidden, so it stays in her grimoire although the sources do not name it. Her style of casting (key readies the page, a punch combo releases it from the fist) is the existing firecast MEREOLEONA style and applies to every Fire page, including the four new ones. Not in her grimoire (kept in the basic Fire book): the other Fire pages.

### Leopold Vermillion (`leopold`, book `book_fire`, magic `FLAME`)

| # | Page id | Canon name | Japanese | Origin | Opens when | Page | Exclusive | Source |
|---|---|---|---|---|---|---|---|---|
| 1 | `spiral_flame` | Flame Magic: Spiral Flame | 螺旋焔(らせんほむら) | anime | from the start | KEPT | yes | ja_search |
| 2 | `wild_bursting_flame` | Flame Magic: Wild Bursting Flame | 爆乱焔(ばくらんほむら) | anime | mastery 20% | KEPT | yes | ja_search |
| 3 | `leopold_deep_spiral_flame` | Flame Magic: Deep Spiral Flame | 深炎螺旋焔(しんえんらせんほむら) | manga | `spiral_flame` cast 3 x AND mastery 45% | NEW | yes | ja_search |
| 4 | `leopold_crimson_eruption` | True Flame Magic: Crimson Eruption | 真炎魔法「紅蓮噴火」(ぐれんふんか) | manga | `leopold_deep_spiral_flame` open AND `wild_bursting_flame` cast 2 x AND mastery 70% | NEW | yes | ja_search |

Canon spells with no page (documented, ignored by the engine):

* Leo Rugiens (shared with Fuegoleon) (anime; design): The old lock keeps it open for him (the Vermillion family spell); no source named it for Leopold, so it is not in his list.

Notes: Mode 0 of the Fire book stays. His style of casting (hold the key to wind a spiral up, release throws it) is the existing firecast LEOPOLD style and applies to every Fire page. Spiral Flame and Wild Bursting Flame are marked exclusive to him, so the basic Fire book loses them.

### Mimosa Vermillion (`mimosa`, book `book_plant`, magic `PLANT`)

| # | Page id | Canon name | Japanese | Origin | Opens when | Page | Exclusive | Source |
|---|---|---|---|---|---|---|---|---|
| 1 | `guidepost` | Plant Magic: Magic Flower Guidepost | - | anime | from the start | KEPT | no | en_wiki |
| 2 | `mimosa_magic_flower_cannon` | Plant Magic: Magic Flower Cannon | - | anime | mastery 12% | NEW | yes | en_wiki |
| 3 | `mimosa_dream_healing_cradle` | Plant Magic: Dream-Healing Flower Cradle | - | anime | `guidepost` open AND mastery 25% | NEW | yes | en_wiki |
| 4 | `mimosa_princess_healing_robe` | Plant Magic: Princess-Healing Flower Robe | - | anime | `mimosa_dream_healing_cradle` open AND mastery 40% | NEW | yes | en_wiki |
| 5 | `hundred_flowers` | Dream World of a Hundred Flowers | - | anime | mastery 55% | KEPT | no | design |
| 6 | `mimosa_flower_princess_utopia` | Plant Magic: Flower Princess Utopia | 花姫の夢想郷 | anime | `mimosa_princess_healing_robe` open AND mastery 75% | NEW | yes | en_wiki |

Canon spells with no page (documented, ignored by the engine):

* Princess Healing Flower Garden (anime; en_wiki): One summary says she learned this advanced group heal in the Heart Kingdom; it is probably the same spell as Flower Princess Utopia, so no separate page.
* Magic Flower Bullet (anime_only; en_wiki): A mobile-game skill only; not built.

Notes: The Plant book's other pages (Seed Bloom, Healing Bloom, Flower Sprite, Ivy Barrier, Blossom Dress, Petal Storm, Yggdrasil, Briar Prison) are not in her list and stay in the basic Plant book. Yggdrasil is William's lock, Briar Prison Charlotte's.

### William Vangeance (`william`, book `book_world_tree`, magic `WORLD_TREE`)

| # | Page id | Canon name | Japanese | Origin | Opens when | Page | Exclusive | Source |
|---|---|---|---|---|---|---|---|---|
| 1 | `mistilteinn_seed` | World Tree Magic: Mistilteinn Seed | - | anime | from the start | KEPT | yes | en_wiki |
| 2 | `magic_tree_descent` | World Tree Magic: Magic Tree Descent | - | anime | mastery 10% | KEPT | yes | en_wiki |
| 3 | `root_bind` | World Tree Magic: Dracaena Formation (the page Root Bind) | - | anime | mastery 22% | KEPT | yes | en_wiki |
| 4 | `william_magic_warding_ash` | World Tree Magic: Magic Warding Ash | - | anime | mastery 35% | NEW | yes | en_wiki |
| 5 | `william_great_tree_mistilteinn` | World Tree Magic: Great Tree Mistilteinn | - | anime | `mistilteinn_seed` open AND mastery 50% | NEW | yes | en_wiki |
| 6 | `william_mistilteinn_blade` | World Tree Magic: Mistilteinn Blade | - | anime | `william_great_tree_mistilteinn` open AND mastery 62% | NEW | yes | en_wiki |
| 7 | `budding_of_yggdrasil` | World Tree Magic: Budding of Yggdrasil | - | anime | `magic_tree_descent` open AND mastery 80% | KEPT | yes | en_wiki |

Canon spells with no page (documented, ignored by the engine):

* Demon-Dweller Sword: Spirit Light Tree (anime; en_wiki): A union spell with Licht's sword magic; needs the Anti-Magic sword, not built here.

Notes: Order: the seed is mode 0 (always shown); the rest follow the mod's design, the arc order of first use was not measured. The old Plant-book lock for Yggdrasil stays.

### Klaus Lunettes (`klaus`, book `book_steel`, magic `STEEL`)

| # | Page id | Canon name | Japanese | Origin | Opens when | Page | Exclusive | Source |
|---|---|---|---|---|---|---|---|---|
| 1 | `steel_shot` | Steel Magic: (the book's first page) | - | anime | from the start | KEPT | no | design |
| 2 | `klaus_piercing_lance` | Steel Magic: Blazing Piercing Lance | 施貫の烈槍 | anime | mastery 10% | NEW | yes | ja_search |
| 3 | `klaus_castle_wall` | Steel Magic: Steel Castle's Armored Wall | 鋼城の鎧壁 | anime | mastery 22% | NEW | yes | ja_search |
| 4 | `klaus_spiral_lance` | Steel Magic: Violent Spiral Lance | 旋貫の激槍 | anime | `klaus_piercing_lance` open AND mastery 38% | NEW | yes | ja_search |
| 5 | `klaus_thunder_arrow` | Thunder Arrow (union with Luck Voltia's lightning and Puli's wing magic) | 神鳴の矢 | anime | `klaus_spiral_lance` cast 2 x AND mastery 55% | NEW | yes | ja_search |

Canon spells with no page (documented, ignored by the engine):

* Steel chariot, steel coffin (anime; en_wiki): Named by one summary only; no description, no page.
* Restraining Magic with steel (anime; en_wiki): Named by one summary only; no page.

Notes: The Steel book's Returning Lance, Steel Cannon, Steel Spikes and Iron Maiden were not found for Klaus and stay in the basic Steel book. Mode 0 (steel_shot) cannot be hidden.

### Langris Vaude (`langris`, book `book_spatial`, magic `SPATIAL`)

| # | Page id | Canon name | Japanese | Origin | Opens when | Page | Exclusive | Source |
|---|---|---|---|---|---|---|---|---|
| 1 | `domination` | Spatial Magic: (the book's first page) | - | anime | from the start | KEPT | no | design |
| 2 | `langris_archangel_crash` | Spatial Magic: Archangel's Crash | 大天使の撃墜 | anime | from the start | NEW | yes | ja_search |
| 3 | `langris_archangel_annihilation` | Spatial Magic: Archangel's Annihilation | 大天使の撃滅 | anime | `langris_archangel_crash` cast 3 x AND mastery 50% | NEW | yes | ja_search |
| 4 | `langris_spatial_auto_guard` | Spatial Magic: Auto-Guard | 空間魔法のオートガード | anime | `langris_archangel_annihilation` open AND mastery 65% | NEW | yes | ja_search |

Canon spells with no page (documented, ignored by the engine):

* Fallen Angel's Descent (anime; en_wiki): A search summary of the Baidu wiki calls his ultimate this; it may be a different translation of the Archangel spells, so no page.

Notes: The Spatial book's gate and portal pages belong to Finral and the other Spatial users. Langris's Archangel spells are an attack of removal, not a portal.

### Lemiel Silvamillion Clover (`lemiel`, book `book_light`, magic `LIGHT`)

| # | Page id | Canon name | Japanese | Origin | Opens when | Page | Exclusive | Source |
|---|---|---|---|---|---|---|---|---|
| 1 | `avior_lamp` | Light Magic: Lamp of Avior | アヴィオールの灯 | anime | from the start | KEPT | yes | en_wiki |
| 2 | `light_speed` | Light Magic: (travelling on a beam of light) | - | anime | mastery 25% | KEPT | no | en_wiki |
| 3 | `lamp_of_avior` | Light Magic: Lamp of Avior Gloria | アヴィオールの灯・栄光 | anime | `avior_lamp` open AND (flag `mana_zone` OR mastery 60%) | KEPT | yes | en_wiki |

Canon spells with no page (documented, ignored by the engine):

* Light swords and explosive bursts of light (anime; en_wiki): Described but not named by the wiki; the Light Sword of Judgment page belongs to Patolli, so no page.
* Demon-Dweller Sword: Protecting Light (anime; en_wiki): A union spell with Licht's sword magic; needs the Anti-Magic sword, not built here.

Notes: Mode 0 of the Light book (Light Sword of Judgment) cannot be hidden, so it stays in his grimoire although it is Patolli's spell. He has no new page: all three of his spells already exist.

### Julius Novachrono (`julius`, book `book_time`, magic `TIME`)

| # | Page id | Canon name | Japanese | Origin | Opens when | Page | Exclusive | Source |
|---|---|---|---|---|---|---|---|---|
| 1 | `chrono_stasis` | Time Magic: Chrono Stasis | - | anime | from the start | KEPT | no | en_wiki |
| 2 | `acceleration` | Time Magic: Time Acceleration | - | anime | mastery 18% | KEPT | no | en_wiki |
| 3 | `stolen_time` | Time Magic: Stolen Time | - | anime | mastery 30% | KEPT | no | en_wiki |
| 4 | `reversal` | Time Magic: Time Reversal | - | anime | `acceleration` open AND mastery 42% | KEPT | no | en_wiki |
| 5 | `grigora` | Time Magic: Chrono Stasis Grigora | - | anime | `chrono_stasis` open AND (flag `mana_zone` OR mastery 60%) | KEPT | yes | design |
| 6 | `chrono_anastasis` | Time Magic: Chrono Anastasis | - | anime | `reversal` open AND mastery 85% | KEPT | yes | en_wiki |

Canon spells with no page (documented, ignored by the engine):

* Mana Zone (Time) (anime; en_wiki): A Mana Zone ability, not a page: the book already uses the shared Mana Zone gate.

Notes: Not in his list (they stay in the basic Time book): Hourglass Step, Borrowed Future, Pendulum Ward, Age Breaker, Moment Reprise and Hourglass Storm are the mod's own 'Julius-inspired' pages (version 0.61) and were not found in any source. No new page, and nothing in TimeBook or the Time VFX was touched (rule 3).

### Kirsch Vermillion (`kirsch`, book `book_cherry_blossom`, magic `CHERRY_BLOSSOM`)

| # | Page id | Canon name | Japanese | Origin | Opens when | Page | Exclusive | Source |
|---|---|---|---|---|---|---|---|---|
| 1 | `cherry_blossom_strike` | Cherry Blossom Magic: (blossoms thrown as a strike) | - | anime | from the start | KEPT | no | design |
| 2 | `petal_step` | Cherry Blossom Magic: a passage of blossom wind for allies | - | anime | mastery 15% | KEPT | no | en_wiki |
| 3 | `petal_veil` | Cherry Blossom Magic: blossoms over the battlefield as a defence | - | anime | mastery 30% | KEPT | no | en_wiki |
| 4 | `clones_of_the_beautiful_me` | Cherry Blossom Magic: Clones of the Beautiful Me | - | anime | mastery 50% | KEPT | yes | en_wiki |
| 5 | `magic_cherry_blossom_blizzard` | Cherry Blossom Magic: Magic Cherry Blossom Blizzard | - | anime | `clones_of_the_beautiful_me` open AND mastery 70% | KEPT | yes | en_wiki |

Canon spells with no page (documented, ignored by the engine):

* Beautifying Bloom, Dance of 100 Million Cherry Blossoms, Everlasting Sakura Grove, Blossom Blessing, Blossom Snare, Petal Fan, Razor Petal Stream (anime; design): Pages of the repo's Cherry Blossom book that the searches did not find for Kirsch; they stay in the basic Cherry Blossom book.

Notes: His canon grimoire was bound to the Plant book; the series' magic is Cherry Blossom Magic and the book exists, so CanonBook.KIRSCH now names CHERRY_BLOSSOM (the one edit of an existing line; a Kirsch grimoire made before keeps its Canon tag and shows the whole Plant book). No new page.

### Karna Freese (`karna`, book `book_light`, magic `LIGHT`)

| # | Page id | Canon name | Japanese | Origin | Opens when | Page | Exclusive | Source |
|---|---|---|---|---|---|---|---|---|
| 1 | `karna_crescent_cut` | Moonlight Magic: Crescent Cut | - | anime | from the start | NEW | yes | en_wiki |
| 2 | `karna_bewitching_moonlight` | Moonlight Magic: Bewitching Moonlight | - | anime | mastery 20% | NEW | yes | en_wiki |
| 3 | `karna_full_moon_shell` | Moonlight Magic: Full Moon Shell | - | anime | `karna_crescent_cut` cast 2 x AND mastery 35% | NEW | yes | en_wiki |
| 4 | `karna_crescent_cut_full_moon` | Moonlight Magic: Crescent Cut Full Moon | - | anime | `karna_full_moon_shell` open AND `karna_full_moon_shell` cast 2 x AND mastery 50% | NEW | yes | en_wiki |
| 5 | `karna_lunatic_crisis` | Moonlight Magic: Lunatic Crisis | - | anime | `karna_crescent_cut_full_moon` open AND (flag `mana_zone` OR mastery 75%) | NEW | yes | en_wiki |

Canon spells with no page (documented, ignored by the engine):

* Gibbous Ray (anime; en_wiki): Named on her wiki page; the summary gave no description, so no page.
* Moon Trap (anime; en_wiki): Named on her wiki page; the summary gave no description, so no page.

Notes: Moonlight Magic is not a magic of this mod (CanonBook gives her LIGHT), so her pages are appended to the Light book. Mode 0 of the Light book (Light Sword of Judgment) cannot be hidden and stays in her grimoire. The owner may later give Moonlight its own MagicType and book; her set would then move with it.

### Nozel Silva (`nozel`, book `book_mercury`, magic `MERCURY`)

| # | Page id | Canon name | Japanese | Origin | Opens when | Page | Exclusive | Source |
|---|---|---|---|---|---|---|---|---|
| 1 | `silver_blade` | Mercury Magic: Silver Blade | - | anime | from the start | KEPT | no | en_wiki |
| 2 | `silver_bullet` | Mercury Magic: Silver Bullet | - | anime | mastery 8% | KEPT | no | en_wiki |
| 3 | `silver_spear` | Mercury Magic: Silver Spear | - | anime | mastery 16% | KEPT | no | en_wiki |
| 4 | `mercury_rain` | Mercury Magic: Silver Rain | - | anime | mastery 28% | KEPT | yes | en_wiki |
| 5 | `silver_eagle` | Mercury Magic: Silver Eagle | - | anime | `silver_spear` open AND mastery 40% | KEPT | yes | en_wiki |
| 6 | `mercury_shield` | Mercury Magic: Silver Guardian | - | anime | mastery 52% | KEPT | no | en_wiki |
| 7 | `silver_shining_regal_spear` | Mercury Magic: Silver Shining Regal Spear | - | anime | `silver_spear` cast 3 x AND (flag `mana_zone` OR mastery 68%) | KEPT | yes | en_wiki |
| 8 | `silver_star_of_execution` | Mercury Creation Magic: Silver Star of Execution | - | anime | `silver_shining_regal_spear` open AND mastery 85% | KEPT | yes | en_wiki |

Canon spells with no page (documented, ignored by the engine):

* Valkyrie Armor / Silver Sunset (anime; en_wiki): Valkyrie Armor is another builder's; Silver Sunset was not found in any summary.

Notes: All eight pages of the Mercury book are Nozel's in the sources, so his grimoire shows the whole book; what the set adds is the ORDER he gets them in and the signature marking. The order is the mod's design (no source gave the episodes); the Mana Zone alternatives follow the Spade Kingdom arc where he uses it. No new page: the book already had all of his spells, and the Valkyrie Armour belongs to another builder.

### Kaiser Granvorka (`kaiser`, book `book_storm`, magic `STORM`)

| # | Page id | Canon name | Japanese | Origin | Opens when | Page | Exclusive | Source |
|---|---|---|---|---|---|---|---|---|
| 1 | `vortex_shield` | Vortex Magic: Vortex Shield | - | anime | from the start | KEPT | yes | design |
| 2 | `kaiser_area_helix` | Vortex Magic: Area Helix | 螺旋結界(エリアヘリックス) | anime | mastery 50% | NEW | yes | ja_search |

Canon spells with no page (documented, ignored by the engine):

* Other Vortex Magic spells (anime; ja_search): The searches found no full list for Kaiser; the Japanese summary says only the Area Helix could be confirmed.

Notes: CanonBook gives Kaiser the STORM book (Vortex Magic is a separate MagicType, VORTEX, with its own book), so his page is appended to the Storm book. The Storm book's Storm Lance, Tempest Ring, Eye of the Storm and Thunder Sphere were not found for him and stay in the basic Storm book; mode 0 (Storm Lance) cannot be hidden.

## 7. What the basic books lose, and what the character books hide

Pages marked `exclusive` in a character file are hidden from the basic grimoire (config `basicBookHidesSignatures`, default true) and never open for another character's grimoire. Marked exclusive here: Fire: `calidos_brachium`, `spiral_flame`, `wild_bursting_flame` and the six added pages; Plant: the four added pages; World Tree: `mistilteinn_seed` (mode 0, so it is never actually hidden), `magic_tree_descent`, `root_bind`, `budding_of_yggdrasil` and the three added; Steel and Spatial: the added pages; Light: `avior_lamp`, `lamp_of_avior` and the five added; Time: `grigora`, `chrono_anastasis`; Mercury: `mercury_rain`, `silver_eagle`, `silver_shining_regal_spear`, `silver_star_of_execution`; Cherry Blossom: `clones_of_the_beautiful_me`, `magic_cherry_blossom_blizzard`; Storm: `vortex_shield` and the added page. Not exclusive (they stay in the basic book as before): Mimosa's `guidepost` and `hundred_flowers`, Julius's `chrono_stasis`, `acceleration`, `stolen_time`, `reversal`, Nozel's `silver_blade`, `silver_bullet`, `silver_spear`, `mercury_shield`, Kirsch's `petal_step`, `petal_veil`, `cherry_blossom_strike`, Light's `light_speed`. The later builder of the basic books re-bases these lists.

Pages of a shared book that these characters never used are hidden from them only, and stay in the basic book: Fire (for Mereoleona and Leopold: everything but their own pages and mode 0), Plant (all but Mimosa's), World Tree (none: all four existing pages are William's), Steel (Returning Lance, Steel Cannon, Steel Spikes, Iron Maiden), Spatial (all the portal and gate pages), Light (the Patolli pages for Lemiel and Karna), Time (Julius lacks the six `0.61` pages), Mercury (none), Cherry Blossom (seven pages), Storm (four pages).

Mode 0 of every book is never hidden by the engine, so these stay visible in a grimoire that has no source for them: Exploding Fireball (Mereoleona, Leopold), Light Sword of Judgment (Karna, Lemiel), Storm Lance (Kaiser). Mode 0 is listed in the set only where it is a real spell of the character (Mistilteinn Seed, Chrono Stasis, Silver Blade, Cherry Blossom strike, Steel Shot and Domination as the book's first page, noted as such).

With `magic.lightBoneJa` off (default on), the Light book shows its old five pages and `avior_lamp` is missing; Lemiel's set then has a page that does not exist and the engine hides nothing for him. The self-test runs with the default.

## 8. Edits to existing files (all additions unless marked)

| File | Edit |
|---|---|
| `book/FireBook.java` | `familyPages()` wraps the pilots' list: `CharacterPagesGold.fire(CharacterPages.fire(pages))` (merge hot spot: the other builders' fire pages go through the same line) |
| `book/PlantBook.java`, `WorldTreeBook.java`, `SteelBook.java`, `SpatialBook.java` | a `withGold` field and the same lazy wrapper in `familyPages()` (Spatial is shared with Finral's builder: same hot spot) |
| `book/LightBook.java` | two lazy lists (`goldJa`, `goldOld`) for the two shapes of the book |
| `book/ElementBooks.java` | `storm()`: the list is wrapped in `CharacterPagesGold.storm(...)` |
| `charset/CharacterSetRegistry.java` | twelve ids appended to `SHIPPED` (hot spot) |
| `charset/CharacterSets.java` | `addedPageOwners` also asks `CharacterPagesGold.isAdded` |
| `charset/CharacterSetBoot.java` | one line registering `CharacterSetGoldSelfTests` |
| `grimoire/CanonBook.java` | REPLACED one word: `KIRSCH` magic `PLANT` to `CHERRY_BLOSSOM` |
| `data/nusmp/armour_policy.json` | `new_pages` lines for PLANT, WORLD_TREE, STEEL, SPATIAL, STORM; ids appended to the FLAME and LIGHT `new_pages` arrays (hot spots) |
| `src/test/.../CharacterSetRegistryTest.java` | three lines: the "character without a set" checks used `mereoleona` and `kaiser`, who now have sets; they use `nobody_with_a_set` |
| `src/test/.../CharacterSetSourceScanTest.java` | `everyPageAFileNamesExistsInItsBook` skips books it has no source file for (checked by `CharacterSetGoldTest`) and also accepts the ids of `CharacterPagesGold` (helper `addedGold`) |

New files: `book/CharacterPagesGold.java`, `book/CharacterArtsGold.java`, `charset/CharacterSetGoldSelfTests.java`, twelve `data/nusmp/character_spells/*.json`, `tools/lang/character_spells_gold.json` (26 page texts), `src/test/.../charset/CharacterSetGoldTest.java`, this document. The shared-file hooks for `CharacterSetSourceScanTest.BOOK_FILES` were not needed: the books are checked by the new test instead of by adding seven entries to a `Map.of` that would pass its ten-pair limit.

## 9. Decisions for the owner

1. **Kirsch**: his canon grimoire was bound to the Plant book. The series' magic is Cherry Blossom Magic and the repo has that book (with his two named grimoire spells, Clones of the Beautiful Me and Magic Cherry Blossom Blizzard), so `CanonBook.KIRSCH` now names `CHERRY_BLOSSOM` and his set is in `book_cherry_blossom` with no new page. A Kirsch grimoire made before keeps its tag and shows the whole Plant book. To undo: change the one word back and delete `kirsch.json` and its `SHIPPED` entry.
2. **Karna** (Moonlight Magic) has no magic of her own in the mod, so her five pages sit in the Light book and the engine keeps Light Sword of Judgment visible as mode 0. Giving Moonlight its own `MagicType` and book would fix both; her set would move with it.
3. **Kaiser** (Vortex Magic): `CanonBook` binds him to STORM although a `VORTEX` magic and book exist (with Vortex Shot, Whirlpool, Great Vortex ...). His Area Helix is in the Storm book. Remapping him to VORTEX would let the Vortex book carry it.
4. **Exclusive pages vanish from the basic books** (section 7). If the basic Fire book should keep Spiral Flame and Wild Bursting Flame, set `exclusive` false in `leopold.json`.
5. **Two characters claiming one page** makes the registry skip the whole second file (the engine's rule). Leopold's Spiral Flame and Wild Bursting Flame and Mereoleona's Calidus Brachium are exclusive here; if another builder marks one of them exclusive for Magna or Floga, one of the two files must give it up.
6. **Origins and acquisition order** are the mod's design where the files say so (section 2).

## 10. Tests, self-test and what is not verified

* New JUnit: `charset.CharacterSetGoldTest` (12 tests: files load and match `CanonBook`, every page exists in the real book source, the spells are listed in the order they open, a fresh grimoire opens one or two, every spell opens at full mastery without Mana Zone, no page is exclusive to two characters, Time is untouched, every added page has an owner, a text, a policy line and a cooldown floor, the books append in the right order, the casts are bounded and server safe).
* Changed JUnit: `CharacterSetRegistryTest`, `CharacterSetSourceScanTest` (section 8).
* Ran and passing in the offline shim: the whole `charset` package, `ArmourPolicyTest`, `SpatialPageOrderTest`, `LightBoneJa*Test`, `MercuryRulesTest`, `TreeRulesTest`, `CanonNamingTest`, `PerfSourceScanTest`, `VfxQualityTest`, `ServerSafetyTest`, `ConfigFileNamesTest`, `FireCastRulesTest`; the sweep result is in the report. Tests that need Minecraft classes (`MagicEnumAuditTest`, `GrimoireBookPlanTest`, `CanonBookMagicTest` ...) cannot run offline.
* Type check against the API stubs: 0 real errors in the new and edited files (negative control: two wrong calls were reported).
* Self-test for the CI smoke list: `magic.character_spells_gold` (the twelve sets load, resolve against the real books and `CanonBook`, the added pages follow the older ones in order, are owned, cast and cheap, the late spells wait for the early ones, Mereoleona's and Leopold's grimoires hide what they never used and the basic grimoire hides their signatures).
* Lang: fragment `tools/lang/character_spells_gold.json` (26 keys `nusmp.page.<book>.<id>`) must be merged into `en_us.json` by the release step; the page names themselves are in code.

NOT verified: anything visual or audible; that the fire casting styles (fist, spiral) wrap the new Fire pages the way they wrap the old (they hook every family page, read from source); that Karna's and Mimosa's new pages fit the free-slot and rank-gate rules of `MasteryPages` for the books' new page counts (read, not run); the Japanese names (read from search summaries, a few are back-translations and are marked as translations in the notes); the first appearance of every spell (so every order); the effect of Mistilteinn Blade, Archangel's Annihilation, Bewitching Moonlight and the Purgatory buffs (the sources name them but do not describe them closely); that `PlantBook` / `SteelBook` ... with seven more family pages keep a stable `pagesVersion` for old saves (the pages are appended after the old ones, the unlock bits of old pages do not move).
