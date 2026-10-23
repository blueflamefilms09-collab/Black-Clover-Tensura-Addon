# Pages 15, batch 1: Flame, Lightning, Star and Thread

Owner goal: every grimoire has 15 spells. This batch owns these books (and no other): `book_explosion`, `book_storm`, `book_thread`, `book_magma`, `book_sword`, `book_reinforcement`, `book_fire`, `book_star`, `book_lightning`, `book_demon_fire`.

How to build a page: `docs/magic/pages15_recipe.md`. Files this batch edits: `src/main/java/com/newuniverse/nusmp/book/pages15/Batch1.java`, `tools/lang/pages15_batch1.json`,
`src/test/resources/armour_policy/pages15_batch1_new_pages.json`, this file, the character set files of Kaiser, Vanessa, Fuegoleon, Leopold, Magna, Mereoleona, Gadjah and Luck, and the test `Pages15Batch1Test`.
All pages are NEW (appended after every old page of the book, behind the setting `magic.pagesFifteen`); nothing is replaced and nothing is removed. Nothing here was seen in a game.

## Pages

One table row per page, exactly this shape (the test `Pages15SourceScanTest` reads it): book, id, name, tier (starter / mid / zone / signature), origin (`original`, or
`canon: <source>` naming the English wiki page, the Japanese wiki page, or the chapter / episode; say which wiki each came from), then free columns (what it does, the shape and effect it reuses, worst case).

| Book | Id | Name | Tier | Origin | What it does | Reuses | Worst case |
|---|---|---|---|---|---|---|---|
| `book_explosion` | `blast_step` | Blast Step | starter | original | A small blast under your heels throws you up to 7 blocks forward; everything you pass through is hit and shoved. Damage scales with the book's mastery and size. | dash + FLAME_EXPLOSION | 4 targets on the line, 1 packet |
| `book_explosion` | `flash_bang` | Flash Bang | starter | original | A bright burst around you hits up to four foes within 4 blocks and blinds them for 3 seconds. | nova + FIRE_BURST | 4 targets, 1-2 packets |
| `book_explosion` | `powder_burst` | Powder Burst | starter | original | Black powder goes off in front of you: a 6 block cone of small blasts that shoves every foe in it back. | cone + FLAME_EXPLOSION | 4 targets, 1-2 packets |
| `book_explosion` | `delayed_charge` | Delayed Charge | mid | original | Plants a charge where you aim; two seconds later it goes off in 4.5 blocks for about a third more than a normal mid hit, burning and throwing foes back. | custom delayedBlast + MAGIC_CIRCLE telegraph + FLAME_EXPLOSION | 6 targets, 2 packets, 1 delayed task |
| `book_explosion` | `shockwave_ring` | Shockwave Ring | mid | original | A pressure wave rolls out 6 blocks from you, hitting up to six foes and throwing them back and up. | nova + WIND_RING | 6 targets, 1-2 packets |
| `book_explosion` | `cluster_bomb` | Cluster Bomb | mid | original | Four small bombs fan out from your hand; each one that lands blows up on its target and sets it alight for 2 seconds. | volley + FLAME_TRAIL | 4 bolts, 6 targets |
| `book_explosion` | `blast_ward` | Blast Ward | mid | original | Wraps you in reactive plates for 10 seconds: two extra hearts of absorption, and anything that hits you in melee is hurt by the backblast. | empower + SPIRIT_AURA | self only |
| `book_explosion` | `blast_leap` | Blast Leap | mid | original | You detonate the ground under your feet, hurting foes within 3.5 blocks, and ride the blast high and forward, then float down softly for two seconds. | custom leap + FLAME_EXPLOSION | 6 targets, 1 packet |
| `book_explosion` | `carpet_blast` | Carpet Blast | zone | original | Five blasts walk away from you in a straight line, three blocks apart and five ticks between them, shoving whatever stands in their path. | custom strikes + FLAME_EXPLOSION | 8 targets, 5 packets, 5 delayed tasks |
| `book_explosion` | `volatile_ground` | Volatile Ground | zone | original | The ground where you aim turns to powder for 5 seconds: every 1.25 seconds it blows up under everything in 5.5 blocks. | field + FLAME_EXPLOSION | 8 targets per pulse, 4 pulses |
| `book_explosion` | `megaton_crater` | Megaton Crater | signature | original | The book's heaviest charge: one huge blast at the aim point, 7 blocks wide, that burns and hurls up to ten foes. Needs Senior rank. | nova + MAGIC_CIRCLE_EXPLOSION | 10 targets, 2 packets |

## Research sources

Rule 10 (English wiki first, Japanese wiki as the fallback) was followed as far as this sandbox allows: the fandom hosts cannot be fetched (`getaddrinfo ENOTFOUND blackclover.fandom.com`), so everything below was read through
`WebSearch` summaries only, plus the notes the character set files already carry (`sources.en_wiki` / `ja_search`, `planned`). No page text was read in full.

What was found and used:

* **Star Magic** (en-wiki search summary): the Star Magic page lists Quartile Flagellum (a line of starlight joining four stars, swung like a whip), Conjunction (teleport beneath one of your stars) and Never Never Land (Yuno's union spell with Wind Magic that strengthens allies and weakens enemies). The other six Star pages are original.
* **Flame**: Calidos Brachium Purgatory: Flame Burial and Cremation (search summaries of the Mereoleona vs Moris chapters, en-wiki and Sportskeeda), Excelitius Leonum (named in chapter 371 summaries, flagged there as unconfirmed), Flame Rope Binding Formation (炎縄緊縛陣) and Gambler's Base Run (博打走塁) from the Japanese summaries recorded in Magna's set file, Prison Kill Buckshot from a fan list only, Flame-Lightning Explosive Cannon (炎雷爆尽砲, union of Magna and Luck) from the sets' notes. Cinder Jab and Ember Veil are original.
* **Lightning**: Thunder Arrow (神鳴の矢, compound spell, Luck's set notes); the other seven are original (Lightning Stomp echoes a Black Clover M skill name, which is a game, not the series).
* **Reinforcement**: Leg Strength Boost (a supplementary Reinforcement spell named in a search summary of a fan guide). The other nine are original; Arm Strength Boost only borrows its naming.
* **Thread**: nothing new could be confirmed. The earlier note in Vanessa's set already says Thread Whip / Thread Binding / Thread Cocoon exist only in mobile-game databases, so those names are used as plain original pages (marked so in the table).
* **Explosion, Storm, Magma, Sword, Demon Fire**: no spell list could be found for these attributes in the series (Explosion, Storm and Magma are the mod's own attributes; Sword Magic lists only Licht's Origin Flash, Origin Flash Barrage, the Demon-Dweller / Demon-Destroyer swords, which the book already has). Every page is original, in the book's own naming and style.

Could not be fetched at all: every blackclover.fandom.com page (Flame, Lightning, Star, Sword, Reinforcement, Thread, Storm Magic and the character pages), the Japanese fandom wiki, ja.wikipedia (the search tool returned no hits for them). The repo wiki copies in `.github/wiki` have none of the ten books except Demon Fire, whose spells are all covered.

Count: 0 canon pages (most of them weakly sourced, see above), 11 original pages, 11 in all.

## Character grimoires (set entries added)

Every page below is appended to the set file of the character (`exclusive: false`, so the basic book keeps it; the acquisition rule is a mastery threshold in the order of the tiers). Existing generic pages of the book that no set lists
(Calderos, Flame Armour, Phoenix Flare, Shining Flame Cross, Combo Eruption, Thunder Lance, Lightning Surge, Raijin's Thunder Drums, Thunderbird Cavalry, Tempest Ring, Thunder Sphere, Eye of the Storm) are listed where they fit a character's style.
Several new Flame pages are shared by two or more characters, because the Fire book has only 33 pages for four grimoires of 15; that is this batch's own reading, not a statement that each of them uses each page in the series.



## Worst case of the batch (rule 11)

* A page is one server cast: at most the tier's targets (4 / 6 / 8 / 10) are hit, nothing runs per tick afterwards. Fields and zones last at most 100 ticks in steps of at least 4 ticks, at most two kit zones per player (kit clamp).
* Custom casts (`delayedBlast`, `strikes`, `smite`, `skyStrike`, `leap`, `barrage`, `skySmash`, `neverNeverLand` in `Batch1`) use only `Pages15Kit.Ctx`: at most 6 delayed tasks of at most 40 ticks, at most 10 effect packets (Ctx budget), damage only through `Ctx.hit` (clamped to 1.5 hits; the series pages are tuned so one target takes about 1.4 to 1.5 hits in all), no entity, no block, no chunk, no static state.
* Self movement (`leap`, `skySmash`) sets the caster's own velocity once or twice and gives Slow Falling for 2 to 3 seconds; it moves no one else. The `pull` rider pushes a target a little toward the caster once per hit (it is not a control lock); controls (`bind`) go through the shared 12 s lock.
* Network: at most about 10 small effect packets to nearby players per cast. Client: the shapes reused are the ones the books already draw (already budgeted by `perf`); no new asset, no new entity type, no new packet.
* The 92 pages add nothing per tick while nobody casts them; the lists are built once per book and cached.

## New and replaced (rule 5)

All 92 pages are NEW; nothing is replaced and nothing is removed. 74 character set entries are NEW (appended; no existing entry changed).
