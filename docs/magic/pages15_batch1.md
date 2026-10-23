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
| `book_storm` | `gust_jab` | Gust Jab | starter | original | A hard gust punches out in a 5 block cone and shoves up to four foes back. | cone + FX_WIND_GUST | 4 targets, 1-2 packets |
| `book_storm` | `static_snap` | Static Snap | starter | original | A crackling spark flies where you look and snaps on impact, hitting everything within 2.5 blocks and slowing it for 1.5 seconds. | bolt + LIGHTNING_SPEAR | 4 targets, 1-2 packets |
| `book_storm` | `squall_step` | Squall Step | starter | original | You ride a squall 8 blocks forward and knock aside everything on the way. | dash + FX_WIND_GUST | 4 targets on the line, 1 packet |
| `book_storm` | `rain_curtain` | Rain Curtain | mid | original | A curtain of rain falls around you for 10 seconds: it douses fire (fire resistance) and soaks two hearts of damage. | empower + WIND_RING | self only |
| `book_storm` | `thunderhead_strike` | Thunderhead Strike | mid | original | Calls a bolt out of the cloud deck onto the foe under your crosshair (28 blocks), hurting it for a bit more than a normal mid hit and slowing it. | custom skyStrike + STORM_BOLT | 1 target, 2 packets |
| `book_storm` | `hailstone_volley` | Hailstone Volley | mid | original | Five hailstones fan out ahead of you; each one slows its target for 2 seconds. | volley + WIND_SLASH | 5 bolts, 6 targets |
| `book_storm` | `squall_front` | Squall Front | mid | original | A wall of gale rolls 14 blocks ahead of you, 2.5 blocks wide, and drives everything in it back. | line + STORM_FRONT | 6 targets, 1-2 packets |
| `book_storm` | `downburst` | Downburst | mid | original | A column of cold air slams down at the aim point, hitting foes within 5 blocks and pinning them with heavy slowness for 2 seconds. | nova + STORM_FALL | 6 targets, 1-2 packets |
| `book_storm` | `thundercloud` | Thundercloud | zone | original | A small thundercloud follows you for 5 seconds and pelts everything within 6 blocks every second, lifting and pushing it. | field + STORM_CLOUD | 8 targets per pulse, 5 pulses |
| `book_storm` | `tempest_barrage` | Tempest Barrage | zone | original | Calls a bolt down on each of up to eight foes within 12 blocks, one after another four ticks apart. | custom smite + STORM_BOLT | 8 targets, 8 packets, 8 delayed tasks |
| `book_storm` | `storm_maelstrom` | Storm Maelstrom | signature | original | The whole storm collapses into one great whirl at the aim point, 8 blocks wide, that hurls up to ten foes into the air. Needs Senior rank. | nova + STORM_CLOUD | 10 targets, 2 packets |
| `book_thread` | `thread_whip` | Thread Whip | starter | original | Snaps a bundle of threads 8 blocks ahead; everything it lashes is dragged toward you. The name echoes the Black Clover M game list, not the series. | line + THREAD_LINE | 4 targets, 1-2 packets |
| `book_thread` | `tripwire_snare` | Tripwire Snare | starter | original | Hidden trip threads spring up across a 5 block cone and tangle up to four foes, slowing them hard for 3 seconds (Vanessa's Trap Magic style). | cone + THREAD_TRAP_LINE | 4 targets, 1-2 packets |
| `book_thread` | `thread_needle` | Thread Needle | starter | original | A single hard-spun thread flies straight and pins its target: a small hit and weakness for 3 seconds. | bolt + THREAD_LINE | 4 targets, 1-2 packets |
| `book_thread` | `thread_cocoon` | Thread Cocoon | mid | original | You wrap yourself in a cocoon of threads for 8 seconds: resistance, an absorption heart and a sting of thorns for whoever hits you. The name echoes the game lists, not the series. | empower + THREAD_COCOON | self only |
| `book_thread` | `puppet_strings` | Puppet Strings | mid | original | Strings settle on the foe under your crosshair (16 blocks): it is held in place for a few seconds, takes a small hit and is weakened. | bind + THREAD_STRINGS | 1 target (the shared control lock applies) |
| `book_thread` | `silk_swing` | Silk Swing | mid | original | You shoot a thread ahead and swing along it, covering up to 10 blocks at once without hitting anything: a pure move. | dash + THREAD_LINE | self only |
| `book_thread` | `razor_weave` | Razor Weave | mid | original | A wide net of sharpened threads sweeps a 7 block cone, cutting up to six foes and slowing them for 2 seconds. | cone + THREAD_LINE | 6 targets, 1-2 packets |
| `book_thread` | `stitching_threads` | Stitching Threads | mid | original | Fine threads stitch wounds shut: you and every ally within 6 blocks recover 15 percent of their health (the healing decay applies). Grows out of her mending of clothes. | healAllies | allies in 6 blocks |
| `book_thread` | `thousand_threads` | Thousand Threads | zone | original | Five homing threads leave your hands, each seeking a foe and hauling it a little toward you as it cuts. | volley + THREAD_LINE | 5 bolts, 8 targets |
| `book_thread` | `thread_cage` | Thread Cage | zone | original | Threads rise in a ring of 5 blocks at the aim point and close over up to eight foes: damage, heavy slowness and weakness for 3 seconds. | nova + THREAD_COCOON | 8 targets, 1-2 packets |
| `book_thread` | `crimson_loom` | Crimson Loom | signature | original | A great red web is woven at the aim point, 8 blocks wide: up to ten foes are hurt, hauled toward its centre and slowed for 4 seconds. Needs Senior rank. | nova + THREAD_WEB | 10 targets, 2 packets |
| `book_magma` | `ember_splash` | Ember Splash | starter | original | A spray of molten droplets across a 5 block cone burns every foe it touches for 4 seconds. | cone + FLAME_TRAIL | 4 targets, 1-2 packets |
| `book_magma` | `lava_whip` | Lava Whip | starter | original | A lash of liquid rock cracks 9 blocks ahead and sets whatever it strikes alight. | line + FLAME_TRAIL | 4 targets, 1-2 packets |
| `book_magma` | `molten_stride` | Molten Stride | starter | original | You skate 6 blocks on a slick of lava, burning anything you pass through. | dash + FLAME_TRAIL | 4 targets on the line, 1 packet |
| `book_magma` | `obsidian_crust` | Obsidian Crust | mid | original | Your skin cools to black glass for 10 seconds: resistance, fire resistance and a burn for those who strike you. | empower + SPIRIT_AURA | self only |
| `book_magma` | `magma_geyser` | Magma Geyser | mid | original | A geyser of magma bursts at the aim point, lifting up to six foes within 4 blocks and burning them for 4 seconds. | nova + FIRE_PILLAR | 6 targets, 1-2 packets |
| `book_magma` | `slag_shower` | Slag Shower | mid | original | Five clots of slag fly out in a fan; each one that lands burns its target for 3 seconds. | volley + FLAME_TRAIL | 5 bolts, 6 targets |
| `book_magma` | `molten_fist` | Molten Fist | mid | original | Your fist glows white-hot: a narrow, short blow that burns for 5 seconds and drives the foe back hard. | cone + FLAME_EXPLOSION | 6 targets, 1-2 packets |
| `book_magma` | `lava_flow` | Lava Flow | zone | original | The ground splits and a river of lava runs 14 blocks ahead, 2.5 blocks wide, burning and slowing up to eight foes. | line + EARTH_SPIKES | 8 targets, 1-2 packets |
| `book_magma` | `pyroclastic_surge` | Pyroclastic Surge | zone | original | A rolling cloud of hot ash and gas pours across a wide 9 block cone, burning and shoving everything in it. | cone + FIRE_WILD | 8 targets, 1-2 packets |
| `book_magma` | `supervolcano` | Supervolcano | signature | original | The land around you erupts in an 8 block ring: up to ten foes are burned for 6 seconds, thrown up and pushed away. Needs Senior rank. | nova + FIRE_WILD | 10 targets, 2 packets |
| `book_sword` | `blade_flurry` | Blade Flurry | starter | original | A quick fan of summoned blades cuts across a 4 block cone in front of you. | cone + WIND_SLASH | 4 targets, 1-2 packets |
| `book_sword` | `sword_step` | Sword Step | starter | original | You vanish into a lunge of 6 blocks, cutting every foe on the line. | dash + WIND_SLASH | 4 targets on the line, 1 packet |
| `book_sword` | `light_edge` | Light Edge | starter | original | Throws a single blade of pure light that flies straight and cuts the first foe it meets. | bolt + LIGHT_BLADE | 4 targets, 1-2 packets |
| `book_sword` | `crescent_wave` | Crescent Wave | mid | original | Releases a wide crescent of cutting force that bursts in 2.5 blocks where it lands, hurting up to six foes. | bolt + WIND_SLASH | 6 targets, 1-2 packets |
| `book_sword` | `light_thrust` | Light Thrust | mid | original | A needle-thin thrust of light 9 blocks long that runs through everything in a line. | line + LIGHT_BLADE | 6 targets, 1-2 packets |
| `book_sword` | `blade_ward` | Blade Ward | mid | original | Spectral blades circle you for 10 seconds: a little resistance, and anything that strikes you takes a cut in return. | empower + WEAPON_CONSTRUCTS | self only |
| `book_sword` | `rising_cleave` | Rising Cleave | mid | original | An upward cleave that cuts a 4.5 block cone and throws the foes it catches into the air. | cone + WIND_SLASH | 6 targets, 1-2 packets |
| `book_sword` | `sword_storm` | Sword Storm | zone | original | A ring of summoned swords whirls around you for 5 seconds and cuts everything within 5 blocks every second. | field + WEAPON_CONSTRUCTS | 8 targets per pulse, 5 pulses |
| `book_sword` | `skyfall_blades` | Skyfall Blades | zone | original | Swords fall out of a rift above the aim point in five places around it, four ticks apart, each cutting the foes beneath it. | custom strikes + KOTO_SWORDS | 8 targets, 5 packets, 5 delayed tasks |
| `book_sword` | `sovereign_blade` | Sovereign Blade | signature | original | One enormous blade of light cuts a lane 20 blocks long and 2.5 wide through up to ten foes. Needs Senior rank. | line + LIGHT_BLADE | 10 targets, 2 packets |

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

Count: 0 canon pages (most of them weakly sourced, see above), 53 original pages, 53 in all.

## Character grimoires (set entries added)

Every page below is appended to the set file of the character (`exclusive: false`, so the basic book keeps it; the acquisition rule is a mastery threshold in the order of the tiers). Existing generic pages of the book that no set lists
(Calderos, Flame Armour, Phoenix Flare, Shining Flame Cross, Combo Eruption, Thunder Lance, Lightning Surge, Raijin's Thunder Drums, Thunderbird Cavalry, Tempest Ring, Thunder Sphere, Eye of the Storm) are listed where they fit a character's style.
Several new Flame pages are shared by two or more characters, because the Fire book has only 33 pages for four grimoires of 15; that is this batch's own reading, not a statement that each of them uses each page in the series.

* `kaiser` (book_storm): +12 entries: `gust_jab` 0.05, `squall_step` 0.12, `tempest_ring` 0.18, `rain_curtain` 0.25, `thunderhead_strike` 0.32, `squall_front` 0.40, `downburst` 0.46, `thundercloud` 0.55, `thunder_sphere` 0.62, `tempest_barrage` 0.70, `eye_of_the_storm` 0.82, `storm_maelstrom` 0.90.
* `vanessa` (book_thread): +6 entries: `thread_whip` 0.05, `tripwire_snare` 0.10, `stitching_threads` 0.18, `thread_cocoon` 0.34, `puppet_strings` 0.45, `thousand_threads` 0.62.

## Worst case of the batch (rule 11)

* A page is one server cast: at most the tier's targets (4 / 6 / 8 / 10) are hit, nothing runs per tick afterwards. Fields and zones last at most 100 ticks in steps of at least 4 ticks, at most two kit zones per player (kit clamp).
* Custom casts (`delayedBlast`, `strikes`, `smite`, `skyStrike`, `leap`, `barrage`, `skySmash`, `neverNeverLand` in `Batch1`) use only `Pages15Kit.Ctx`: at most 6 delayed tasks of at most 40 ticks, at most 10 effect packets (Ctx budget), damage only through `Ctx.hit` (clamped to 1.5 hits; the series pages are tuned so one target takes about 1.4 to 1.5 hits in all), no entity, no block, no chunk, no static state.
* Self movement (`leap`, `skySmash`) sets the caster's own velocity once or twice and gives Slow Falling for 2 to 3 seconds; it moves no one else. The `pull` rider pushes a target a little toward the caster once per hit (it is not a control lock); controls (`bind`) go through the shared 12 s lock.
* Network: at most about 10 small effect packets to nearby players per cast. Client: the shapes reused are the ones the books already draw (already budgeted by `perf`); no new asset, no new entity type, no new packet.
* The 92 pages add nothing per tick while nobody casts them; the lists are built once per book and cached.

## New and replaced (rule 5)

All 92 pages are NEW; nothing is replaced and nothing is removed. 74 character set entries are NEW (appended; no existing entry changed).
