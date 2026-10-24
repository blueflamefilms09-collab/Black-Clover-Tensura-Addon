# Star Magic in the game: star nodes, the A + B look, the heir, the new pages

Built from the owner's decisions on `docs/vfx_remake/star_magic_design.md` (phase 1) and `docs/vfx_remake/star_new.md` (the three looks). This file says what was built, what is NEW / REPLACED / KEPT (rule 5), the worst case of every part (rule 11), the settings, and what was **not** seen in a game.

**Never run in a game.** Everything was type-checked against the API stubs and covered by pure tests, source scans and a CI self-test; nobody has played it. The pictures are a headless software render of the real layer plus a mirror of the prop painter (see 7). The palette (gold-white and starlight blue) is a **placeholder**: no canon still of Star Magic was available.

## 1. What was built, in plain words

* **Star nodes.** A Star mage hangs small stars in the world with the new page **Star Seed**. A star is a `MagicPropEntity` of the new kind `PropKind.STAR_NODE`: it hangs for 40 seconds, cannot be hit, is tracked to nearby players by vanilla (no custom packet at rest). 6 per mage (8 for the Grinberryall heir), 128 on the whole server (the oldest star of anybody goes first), all server settings.
* **Conjunction (REPLACEMENT of the cast).** Teleports to the ground beneath the star you look at (the star within 20 degrees and 48 blocks nearest the look, no key: the normal aim). It **consumes** that star, brings up to 3 allies standing within 3 blocks (party, team, tamed pets of those: the mod's own `KitAllies` rule; at most 5, setting `star.conjunctionMaxAllies`) and gives the caster 4 ticks of full damage resistance at arrival. No star in view: the old 12-block dash (setting `star.conjunctionFallback`).
* **Quartile Scutum (NEW page).** The four newest stars become a standing pane of starlight (up to 6 by 5 blocks, 1.5 thick, faces the caster) for 5 seconds. It stops projectiles and the mod's own spell bolts of everyone not on the caster's side, until three hits' worth of damage are taken (a devil-powered shot counts half). The stars stay.
* **Quartile Hasta (NEW page, a mix of lance and beam).** The four stars link and charge for 9 ticks, then fire a spear of light (A pointed head, B laser tail) from their middle along the crosshair (32 blocks, up to 6 foes pierced, damage scaling `tierHit * (1 + 0.04 * stars + 0.10 * mastery)`, times 1.15 for the heir, always clamped to 1.5 hits of the tier by `Ctx.hit`). The four stars are **consumed**.
* **Quartile Flagellum (REPLACEMENT of the cast, for the heir).** A whip of starlight from the hand through four stars, 3 lashes over 12 ticks, each foe hit at most twice (0.6 of a tier hit each). It breaks no block. A mage who is not an heir, or has no room for four stars, or `star.nodes = false`, gets the old cone.
* **Black Neverland, Quintile: Atrum Scutum / Hasta / Flagellum (NEW pages, heir).** Black Neverland (canon: manga ch. 381 to 383, Star + Wind + Asta's Anti-Magic): a dome 8 blocks wide for 5 seconds: foes inside are weakened, slowed and lightly hurt, the spell bolts of others end, the caster's allies are quickened and strengthened. The three Quintile pages are the Quartile spells with **five real stars** (no automatic ring), wider / heavier / one more star consumed by Hasta. **Quintile is unverified canon** (one source only): marked so in this doc and in the lang text.
* **The Grinberryall heir (computed, nothing stored).** A Spade-cover Star grimoire at the needed mastery (`star.heirMastery`, default 1.0 = the signature tier) AND the Spade Kingdom Raid cleared in the player's story (Dante, Vanica, Zenon, Morris, Lucifero: `SagaRewards.cleared`). **Required for the whole Quartile family** (Scutum, Hasta, Flagellum's whip), Quintile and Black Neverland: those pages do not open for a non-heir (the page waits for "a Grinberryall heir"; `GrimoireBook.pageBlock`), and every cast re-checks it on the server. The heir also gets 8 stars instead of 6. Non-heirs keep every basic Star page and Conjunction.
* **Dual users.** Server setting `star.dualMode = FUSION | SECOND_GRIMOIRE`. **FUSION (default) is exactly the 1.16.0 behaviour.** SECOND_GRIMOIRE: when the Tower's questionnaire ties Star with another attribute (Star as one of the two, or Star as the blend the pair made), the mage gets TWO real grimoires, the other attribute's and a Star one (the mod's own `GrimoireAcceptance.grantExtra`, kept in the code since 1.6.0 and unused by the tower until now), with the same cover so a Spade mage can become the heir. A black (forbidden) cover never splits.
* **The look.** New client layer `StarMagicLayer` for seven new `VfxShape`s appended at the end of the enum (`STAR_NODE, STAR_LINK, STAR_CONJ, STAR_PANE, STAR_LANCE, STAR_WHIP, STAR_DOME`); `StarPropPainter` draws the placed stars and the pane. **A anime cel** is the base of the star, the lines, the pane and the whip (flat baked colours, alpha blended, two-frame stepped timing); **B volumetric glow** is Conjunction, the dome, the far-distance star sprites and the Hasta beam. No C meshes. Client options in `nusmp-star-v2-client.toml`: `vfx.element.star = V2 | V1` (default V2; V1 draws the new shapes as the old starlight beam and a star as a plain glow) and `vfx.element.starReducedFlashes` (default false).

## 2. NEW, REPLACED, KEPT (rule 5)

| | What |
|---|---|
| **NEW** | the node system (`star/`: rules, registries, config, casts, props, boot, self-test), `PropKind.STAR_NODE` / `STAR_PANE`, seven pages (below), seven `VfxShape`s, `StarMagicLayer`, `StarPropPainter`, `StarV2` + its client config, 24 textures `stv_*`, the heir mark, the dual-mode plan, server config `nusmp-star-server.toml`, tests, self-tests `magic.star_nodes` and `magic.star_nodes_look`, the previews, this doc |
| **REPLACED (behind `star.nodes`; V1 kept as the fallback)** | the cast of `star_conjunction` (the 12-block dash is now the fallback) and of `quartile_flagellum` (the cone is the fallback, and what a non-heir still gets). Ids, names and unlock bits unchanged. |
| **KEPT, untouched** | the other 13 old pages, `VfxShape.STAR_MAGIC` and `ElementIdentityLayer` (about 24 other callers), the Star Shield buff and its aura, Never-Never Land, `MagicType.STAR`, the questionnaire and the covers, the fusion of dual attributes (default), every old texture, the Time Magic VFX. `quartile_flagellum`'s lang text and the 15 older pages' ids, order and mastery are unchanged. |
| **DROPPED by the owner** | Halation (not canon): not built, not in the page plan, tests or previews. |

Shared files were only appended to: `NUSMP` (two lines), `GrimoireBook` (the page-block hook: the heir gate), `Pages15` (one line: the seven pages follow the nine of Batch1, so the `magic.pages15` self-test checks them), `Pages15Rules` (`COOLDOWN_EXEMPT`: Star Seed's 20-tick cooldown), `tools/lang/merge_pages15.py` (also merges `star_magic.json`), `VfxManager` (one registration), `VfxShape` / `PropKind` (entries at the end), `PropRegistry`, `PropPainterRegistry`, `SpellRuntime` (one line: panes stop the mod's bolts), `Batch1` (the two casts wrapped, the old casts inside), `WizardSystem` (the dual plan; the default path is the old code), `armour_policy.json` (`STAR.new_pages`), `preview.py` / `Preview.java` (the star options; and the water classes are now always compiled, which the merged Preview.java already needed: before this fix the preview build failed for every scene that was not a water scene).

## 3. The new pages (rule 10: sources; `docs/magic/pages15_recipe.md` row format)

Appended LAST in `book_star` (after the 15 older pages, by `Pages15`; the universal pages move up, as with every Pages15 book). The list is the same on client and server whatever `star.nodes` says (mode numbers always agree); with `star.nodes = false` the new pages never open and cannot be cast. Star Seed asks a cooldown of 20 ticks (below the starter tier's 80, a deliberate exception: a star is cheap and capped). Wiki access: the English Black Clover Wiki host could not be fetched from the build machine; every canon fact comes from the search summaries recorded in `star_magic_design.md` section 0 (`EN` English wiki pages as summarised by search, `JA` Japanese pages, `FAN` aggregators). **Not fetchable at all:** blackclover.fandom.com page text, any manga panel, any canon still.

| Book | Id | Name | Tier | Origin | What it does | Reuses | Worst case |
|---|---|---|---|---|---|---|---|
| `book_star` | `star_seed` | Star Seed | starter | canon: en-wiki: Star Magic (create and manipulate stars at will; ja-wiki: ブラッククローバー) | Hangs a star where you look; stars last 40 s, 6 per mage (8 heir), 128 on the server. | Pages15Kit.cast, MagicProps | 1 entity, 1 ray, 1 packet |
| `book_star` | `quartile_scutum` | Quartile Scutum | mid | canon: en-wiki: Quartile Scutum (four stars form a barrier; manga ch. 309 and 310; ja-wiki: 四天・盾) | A standing starlight pane from the four newest stars, 5 s; stops shots; stars stay; heir. | Pages15Kit.cast, STAR_PANE | 1 entity, 4 packets, 1 query per tick for 100 ticks |
| `book_star` | `quartile_hasta` | Quartile Hasta | mid | canon: ja-wiki: 四天・槍 (light gathered between four linked stars fired as a laser; en-wiki list only, chapter not found; fan source manga ch. 310) | Charge 9 ticks, then a spear of light with a laser tail along the crosshair; stars consumed; heir. | Pages15Kit.cast, STAR_LANCE | 6 targets, 6 packets |
| `book_star` | `black_neverland` | Black Neverland | zone | canon: fan: manga ch. 381 to 383 (Never-Never Land with Asta's Anti-Magic; ja-wiki title 黒のネバーランド) | A 5 s dome: foes weakened and hurt a little, others' bolts end, allies strengthened; heir. | Pages15Kit.cast, SpellRuntime.dissolveBolts, STAR_DOME | 8 targets, 5 pulses, 1 packet |
| `book_star` | `quintile_atrum_scutum` | Quintile: Atrum Scutum | zone | canon: en-wiki: Never-Never Land page list of Quintile spells (single source, UNVERIFIED canon) | The Scutum with five real stars, up to 8 by 6, 7 s, a bigger pool; heir. | Pages15Kit.cast, STAR_PANE | 1 entity, 5 packets |
| `book_star` | `quintile_atrum_hasta` | Quintile: Atrum Hasta | zone | canon: en-wiki: Never-Never Land page list of Quintile spells (single source, UNVERIFIED canon) | The Hasta with five stars, 1.6 wide, up to 8 foes; five stars consumed; heir. | Pages15Kit.cast, STAR_LANCE | 8 targets, 7 packets |
| `book_star` | `quintile_atrum_flagellum` | Quintile: Atrum Flagellum | zone | canon: en-wiki: Never-Never Land page list of Quintile spells (single source, UNVERIFIED canon) | The whip through five stars, three lashes, each foe hit at most twice; heir. | Pages15Kit.cast, STAR_WHIP | 8 targets, 15 segment queries, 6 packets |

## 4. Server settings (`nusmp-star-server.toml`, section `[star]`; every value is clamped again in `StarRules`)

| Key | Default | Range | |
|---|---|---|---|
| `nodes` | true | | false restores today's Star Magic: no star, Conjunction is the dash, Flagellum the cone, the new pages stay sealed |
| `maxPerPlayer` / `maxPerHeir` | 6 / 8 | 1 to 8 | the oldest star of that mage goes first |
| `maxOnServer` | 128 | 16 to 512 | the oldest star of anybody goes first |
| `lifeTicks` | 800 | 200 to 2400 | |
| `placeRange` | 24 | 8 to 32 | |
| `seedCooldown` | 20 | 10 to 80 | ticks between two placements of one mage |
| `autoQuartet` | true | | fewer than 4 stars: the missing ones are placed on a ring in front of the mage (only for four) |
| `conjunctionFallback` | true | | no star in view: the dash (false: "No star to go to.") |
| `conjunctionMaxAllies` | 3 | 0 to 5 | |
| `heirMastery` | 1.0 | 0.5 to 1.0 | |
| `dualMode` | FUSION | FUSION, SECOND_GRIMOIRE | |

Changing `nodes` takes effect at once for the casts; the page lists are cached per book, so a restart is needed for the "sealed" state to match.

## 5. Worst cases (rules 9 and 11)

**Server (everything on the server thread, keyed by player UUID; cleaned on logout, death, respawn, dimension change and server stop; the client sends nothing new, no packet class was added).**

| Part | Entities | Per cast | Per tick |
|---|---|---|---|
| Stars | at most 128 on the server (`maxOnServer`), each a prop with a life counter | Seed: 1 spawn, 1 ray (ground below), up to 4 collision tests, 1 packet. Reading a mage's stars: at most 8 records and 8 entity-by-UUID lookups | 0 scans; a star checks its owner is online once a second |
| Conjunction | 0 | 1 ray, up to 10 box tests, 1 ally query (radius 3), at most 5 allies moved, 1 packet | none |
| Pane | at most 8 on the server (one per mage) | 1 spawn, 4 or 5 packets | **1 projectile query of the pane's own box per living pane** (at most 8 per tick, each pane lives 100 or 140 ticks, the cooldown is 15 s or more); `SpellRuntime` bolts: at most 8 slab tests per bolt per tick, and nothing at all while no pane exists |
| Hasta | 0 | 4 or 5 link packets + lance + impact flash = at most 7, one segment query after 9 ticks, at most 6 or 8 targets | none |
| Flagellum / Atrum | 0 | 4 or 5 whip legs + 1 flash; 3 lashes x 4 or 5 segment queries = 12 or 15 bounded queries over 12 ticks, each foe hit at most twice | none |
| Black Neverland | 0 | 1 packet; 5 pulses (every 20 ticks) of one foe query, one ally query and one `dissolveBolts` (at most 6) | none (one zone of 100 ticks; at most 2 kit zones per player) |

A cast sends at most 10 effect packets (`StarFx.Budget`), to players within 96 blocks, through the per-tick `PerfBudgets.EFFECT_PACKETS` budget. Damage only through `Ctx.hit` (the balance law, clamped to 1.5 hits of the tier).

**Client (quads per frame; `ctx.seg` thins them with distance and the particle setting; nothing is allocated per frame in the V2 path).**

| Part | Quads | Peak vertex budget |
|---|---|---|
| a placed star | 7 near (halo, star, 2 twinkles, column, ground sigil as 2 quads), 2 far (bloom star + halo), 0 beyond 64 blocks; at most 48 stars drawn in full per frame | n/a (prop, not an activation) |
| the pane | at most 6 | |
| STAR_NODE / LINK / CONJ / PANE / LANCE / WHIP / DOME | 7 / 6 / 12 / 11 / 9 / 5 / 12 worst; caps 12 / 8 / 18 / 12 / 14 / 10 / 18 | 48 / 32 / 72 / 48 / 56 / 40 / 72 vertices (engine cap 400) |
| a crowded view: 20 Star mages x 6 stars = 120 stars + 8 panes | 48 x 7 + 72 x 2 + 8 x 6 = **528** | |

Measured through the real layer (`star_ingame_budget.json`, peak over every tick): node 6, lines (4 activations) 24, Conjunction 12, Scutum links 24, pane hit 3 / break 11, Hasta charge + lance 24, whip (4 legs) 20, dome 12; at the lowest detail 4 / 20 / 11 / 20 / 3 / 7 / 20 / 16 / 8. With `V1` the same effects draw 26 quads each (the old beam) per activation.

## 6. Tests

`StarRulesTest`, `StarNodesTest` (caps, eviction, expiry, 40 players at once, cleanup, pane book), `StarMathTest` (look pick, loop order, pane box and slab test, lance clamp, whip chain), `StarPagesTest` (the seven pages last, tiers, lang, policy, doc rows, Halation absent), `StarSourceScanTest` (server safety, caps, hooks, consume rules, heir gate, delays), `StarV2Test`, `StarV2AssetTest` (24 textures, sizes, corners, no dead asset, old textures kept), `StarV2SourceScanTest` (no per-frame allocation, options, V1, reduced flashes, Time Magic untouched); self-tests `magic.star_nodes` (placement with a stand-in player: caps with real entities, eviction removes the body, a body that is gone is dropped, cleanup leaves nothing, pane book, the book's page order) and `magic.star_nodes_look`. The existing sweep (ArmourPolicy, Pages15, ServerSafety, ConfigFileNames, PerfSourceScan, particle / texture / lang tests) was re-run.

## 7. The previews

`docs/previews/vfx_remake/star/star_ingame.png` is **BEFORE | NEW at three moments**, same camera: BEFORE is the real old layer (`ElementIdentityLayer` STAR_MAGIC, or `AuraLayer` for the Star Shield), NEW is the real `StarMagicLayer`. The placed stars and the pane are props drawn by `StarPropPainter`, which needs a `PoseStack` and cannot run headless: their quads are **mirrored** in `tools/vfx_preview/star_ingame.py` from the same numbers (marked in the script). `star_ingame_extras.png`: the V1 option, reduced flashes, a far star, a crowd of 40. `star_ingame_budget.json`: quads per effect, old and new, full and lowest detail. Rebuild: `python tools/gen_star_v2_textures.py && python tools/vfx_preview/star_ingame.py`.

## 8. What was not verified, honest caveats

* Nothing was seen or played in a game: not the look (software render, no bloom), not the feel, not the numbers (first-pass tier values).
* The pane stops `Projectile` entities (one query of its box per tick) and the mod's `SpellRuntime` bolts. Hitscan magic of other mods, melee and Tensura's own beams are **not** stopped.
* Conjunction: the star's ground comes from one downward ray of 16 blocks; if the neighbour tests all fail the cast is refused ("blocked"). Allies are moved by the same offset and skipped if their box would not fit.
* Dual mode SECOND_GRIMOIRE was only checked as a pure plan and by reading `GrimoireAcceptance.grantExtra`; the second grimoire's page unlocking, slot and item flow were not played. A Star blend of two other attributes (e.g. Fire + Light) gives the FIRST parent plus a Star grimoire.
* Black Neverland needs only the heir mark (the owner's decision); the design doc's extra flags (Sylph bond, Anti-Magic) are not required. Quintile and Black Neverland are as designed in the doc; Quintile is unverified canon.
* Lang: `tools/lang/star_magic.json` is the fragment; `en_us.json` is not touched. **The coordinator must run `python3 -B tools/lang/merge_pages15.py` (it now also merges the Star fragment) BEFORE the CI smoke run: the in-game self-test `magic.pages15` checks that every added page, the seven Star pages included, has its text in `en_us.json`, and fails otherwise.**
* The smoke list in `.github` is not touched; the prefix to add is `magic.star_nodes` (keys `magic.star_nodes` and `magic.star_nodes_look`).

## 9. Decisions the owner still has to make

1. Do stars die to Anti-Magic / a dispel (needs a hook), or stay invulnerable (as built)?
2. Passive seeding by the older pages (`comet`, `starfall`): not built (off in the design).
3. Should Black Neverland also need the Sylph bond and the Anti-Magic flag (the design doc's idea), or stay heir only (as built)?
4. A real canon check of the Quintile spells once the chapters are verified; the three pages can be removed from `StarPages` without touching anything else.
5. The Hasta damage and the pane's pool are first-pass; they follow the tier numbers.
