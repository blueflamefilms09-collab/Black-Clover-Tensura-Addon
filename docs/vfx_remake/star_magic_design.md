# Star Magic remake: design document (PHASE 1, docs and previews only, nothing is built)

Owner request: "lets try to remake star magic that same way as the elemental magic" + the design prompt `claude_prompt_star_magic_addon.md` (outline: 1 Lore and Origins, 2 Core mechanics, 3 ability list with anime effect and mod logic, 4 player combat loop).
This is the same process as Fire / Water / Wind: research, a BEFORE sheet of the current look, three VFX directions, a recommendation. The pictures and the VFX recommendation are in `docs/vfx_remake/star_new.md`.
Nothing in `src/`, `assets/`, version, changelog, README, lang files or workflows was changed. Time Magic VFX untouched (rule 3).

## 0. Read this first (sources, honest limits)

**How the research was done (rule 10).** The fandom wiki host (`blackclover.fandom.com`) is NOT reachable from this machine (WebFetch: host not found), so every fact below comes from WebSearch result summaries, not from the page text. Each fact carries a tag:

| Tag | Meaning |
|---|---|
| `EN` | English Black Clover Wiki, read through a search summary (page not opened). Pages named by the searches: Star_Magic, House_Grinberryall, Ciel_Grinberryall, Loyce_Grinberryall, Conjunction, Quartile_Scutum, Quartile_Flagellum (page text summarised), Never-Never_Land, Chapter_355 / 356 / 378 |
| `JA` | Japanese sources found by search: ja.wikipedia (Black Clover), hissatuwaza.kill.jp (必殺技辞典), RENOTE spell list, pixiv encyclopedia, tv-tokyo character page |
| `FAN` | fan / news aggregators (epicdope, sportskeeda, ibtimes, animeexplained, otakusnotes, animehunch ...). Low reliability; used only to cross-check chapter numbers, flagged where it is the only source |

**Not fetchable at all:** blackclover.fandom.com (any page), otakusnotes.com (WebFetch), ja.fandom.com Black Clover wiki (no result), any manga page or panel. **No canon image of Star Magic was available** (the owner's earlier art packs contain no Star Magic still; the Wind pack has Yuno stills only), so the VFX looks of phase 1 are built from the TEXT descriptions below plus a general memory of the series' "four-point sparkle star + thin straight line" look; colours (gold-white star, starlight-blue lines and barrier) are a design choice, not a measured canon colour.

**Not found (so not invented):**
* **"Halation" is not a Star Magic spell I could find**, in English or Japanese (two searches each, plus a spell-list search). The Japanese list of Star Magic is: 合天 Conjunction, 四天・盾 Quartile Scutum, 四天・槍 Quartile Hasta, 四天・鞭 Quartile Flagellum, then the union spells. The owner's "Halation" is treated as a MOD-ORIGINAL finisher (section 3.7). The photographic meaning of the word (light bleeding round a bright source) is what direction B of the VFX literally draws.
* The chapter of first appearance of **Quartile Hasta**: no page text was found, only its place in the wiki's Star Magic "Offensive" list (`EN`) and 四天・槍 in the Japanese list (`JA`). Its effect below is the Japanese description (light gathered in the middle of the four stars and fired as a laser, `JA`) plus one FAN article (ch. 310: "combined the light of the four stars into one and focused it toward Zenon", unnamed).
* **Quintile: Atrum Scutum / Hasta / Flagellum**: one search summary of the wiki's Never-Never Land page lists them as "higher-tier" Star Magic (`EN`, single mention, no description, no chapter). Another search for them found nothing. NOT implemented, NOT designed; flagged for verification (section 3.9).
* **Anime episode numbers: none exist yet** for any Star Magic spell. The 2017 to 2021 TV anime (170 episodes) ended at manga chapter 271 (`FAN`); Star Magic first appears in chapter 308 to 310 (`EN`, `FAN`). Season 2 is reported to premiere in October 2026 and to cover the rest of the Spade Kingdom arc "until about chapter 331" (`FAN`, aggregators, premiere date disputed between them). If that range is right, Conjunction, Quartile Scutum and Quartile Flagellum (ch. 309 / 310) fall inside it, but no episode list was found. So in this document **"Anime effect" is the effect as the manga and the wiki describe it**, with the chapter number; it must be rechecked against the animation once those episodes air.

## 1. Lore and Origins: the Spade Kingdom grimoire

### 1.1 The Grinberryall bloodline (canon)

* **House Grinberryall** is the royal family of the Spade Kingdom. Over the generations it passed down three special attributes: **Sun Magic, Moon Magic and Star Magic** (`EN`, House_Grinberryall).
* **Star Magic** lets the user create and manipulate stars at will; it is a "special" attribute inherited in the royal line; the user needs particular spells, stored in the grimoire, to use it fully; **connecting several stars forms a constellation and gives stronger defences and attacks**; the user can **teleport to a point beneath any of their stars** (`EN`, Star_Magic; the same summary for the Japanese side: 星は点を繋げて様々な武器に展開、星を介した瞬間移動, `JA` ja.wikipedia).
* A FAN article describes Star Magic as a hereditary combination of Sun (father **Loyce**, the king) and Moon (mother **Ciel**, the queen); the wiki summaries only say the family held all three attributes, so "Star = Sun + Moon" is `FAN`, one source, and the mod should treat it as flavour, not as a rule.
* **The fall of the house:** the Zogratis siblings (the Dark Triad: Dante, Vanica, Zenon) attacked the royal castle and killed the king and other royals; a royal guard carried the infant **Yuno Grinberryall** to the Clover Kingdom, where he grew up in Hage (`EN` Yuno / Ciel / Loyce pages via search). Yuno's wind grimoire is the four-leaf clover; **his Star Magic grimoire is a second, Spade grimoire**, given to him during the fight with Zenon (ch. 308 to 310, `FAN` epicdope; ch. 309 reveals the "true affinity", `FAN`). Yuno is the only known Star Magic user. (`EN`, `FAN`)
* **Where each spell is first seen (manga):** Conjunction and Quartile Scutum ch. 309 (`EN` wiki cites ch. 309 for both; a `FAN` recap puts Scutum in the Zenon fight at ch. 309/310); Quartile Flagellum ch. 310 (`EN`); Conjunction used again to escape Zenon's subspace ch. 310 (`FAN`); a large circle of stars over the Noble Realm stopping the Paladins' laser fire ch. 355 (`EN`, Chapter_355; single source for "multiple stars form a shield"); **Never-Never Land** ch. 356 (title "Neverland", union with the wind spirit Sylph: allies' magic stronger, enemies' weaker, time and magic controlled inside, `EN`); **Black Neverland** ch. 381 to 383 (Star + Wind + the Anti-Magic that Asta left him, `FAN` ibtimes / animeexplained, Japanese title 黒のネバーランド).

### 1.2 What the mod has today for the bloodline (the page-by-page audit is Appendix A)

| System | What exists | Source file |
|---|---|---|
| The attribute | `MagicType.STAR` ("Star Magic", parent attribute WIND, hit effect PIERCE, theme `magic.STAR`, speed buff). A normal attribute any mage can roll. | `blackclover/MagicType.java`, `data/nusmp/particle_themes.json` |
| How a player gets a grimoire | one roll after the soul type (`GrimoireAcceptance`: 1 percent five-leaf, 2 percent four-leaf, 10 percent three-leaf); the questionnaire gives the attribute (`wizard/MagicFlavors`: Star = GUST + BRIGHT, blend HOT + BRIGHT -> Star); two close attributes FUSE into one grimoire (`dualAttributeChance`), there is no real second grimoire. | `blackclover/GrimoireAcceptance.java`, `wizard/*` |
| The Spade suit | `Kingdom.SPADE` and the covers Spade / Double Spade / Triple Spade exist (the kingdom that accepts a mage decides the suit; Spade: +8 percent attack and spell power, -2 health). Nothing ties Star Magic to the Spade kingdom. | `blackclover/Kingdom.java`, `GrimoireCover.java`, `docs/mana_system.md` |
| The Spade story | `ArcSaga.SPADE_RAID`: bosses Dante, Vanica, Zenon (Dark Triad), then Morris Libardirt and Lucifero (`arc/spade/SpadeRoster`); clearing the saga pays `SagaRewards` and fires the quest event `arc.saga_clear` (`docs/quests.md`). The saga is the killers of the royal house, so it is the natural story gate. | `arc/world/SagaRewards.java` |
| Late game gates | Mana Zone: six requirements incl. rank 14, 15 boss kills, floor 40, a 30-minute attunement (`mana/zone/ManaZoneGate`). Character sets have acquire rules (`start`, `mastery`, `flag`, `page_uses`, `page_open`, `all`, `docs/magic/character_spells.md`); Yuno's Wind set already uses the flag `spirit_bond` (Sylph). | `charset/SpellRule` |
| Yuno | `data/nusmp/character_spells/yuno.json` (magic WIND) lists Never-Never Land and Star Magic only as NOTES ("needs Star Magic, a second attribute and a different book"). There is **no Star character set** and no Star-specific lineage flag. | `character_spells/yuno.json` |

### 1.3 Grimoire acquisition in the mod (recommendation; owner decides, section 7)

Rule 2 and rule 12: nothing existing is removed; the new behaviour sits behind a server setting that restores the old one.

1. **Star stays a rollable attribute (unchanged).** Anyone who rolls Star gets the book as today (15 pages). The old pages keep their ids, unlock bits and mastery order.
2. **The node system opens with the book itself** (no lineage needed): the new page `star_seed` (starter) places stars; the Quartile pages and the upgraded Conjunction work with whatever stars the mage has. So a normal Star mage plays the node game.
3. **The "Grinberryall heir" mark** (a persistent flag like the existing `spirit_bond`, name `grinberryall_heir`) is the lineage, awarded by existing systems only, no new structure:
   * the grimoire's kingdom is **Spade** (`GrimoirePages.coverOf(inst).kingdom == Kingdom.SPADE`; any Spade cover), AND
   * the Star book is mastered to the signature tier (the existing `cast_star` advancement and the mastery bar), AND
   * the **Spade Raid saga is cleared** (`SagaRewards.cleared(d, ArcSaga.SPADE_RAID, roster)`: Dante, Vanica, Zenon, Morris, Lucifero all defeated; the story event `arc.saga_clear` is the trigger): the heir has avenged the house.
   A non-Spade Star mage never gets the mark (old rule kept: they still play the whole base game).
4. **What the mark gives** (all numbers server config): +2 node cap (6 -> 8), quartile spells 15 percent stronger, Conjunction cooldown -20 percent, and it is the gate of the union spells (Never-Never Land needs the mark AND the Sylph `spirit_bond` flag; Black Neverland additionally needs the Anti-Magic flag). It does not hide any page.
5. **Not done, owner decision:** a real second grimoire for Yuno-style dual users (the mod fuses; section 7), a ritual structure in a Spade ruin, a harder gate (mark required for the whole Quartile family).

## 2. Core mechanics: node-based combat

### 2.1 The idea in one paragraph

A Star mage places **stars** (nodes) in the world with `star_seed`. Stars are small invulnerable props that last 40 seconds. Every spell of the Quartile family and Conjunction reads the mage's own stars: Conjunction teleports to beneath one, Scutum / Hasta / Flagellum use the four most recent ones as corners. Lines are drawn between stars at cast time only. A mage with no stars still has every old page (V1 behaviour) and can cast the Quartile spells with an automatic quartet (setting `star.autoQuartet`, 3.4).

### 2.2 Star placement and entities (what it is, how it is coded)

* **Placement.** Actively cast: the page `star_seed` (NEW, starter tier, so Tier.STARTER numbers: cost 8 percent of mana with a floor of 150, cooldown 80 ticks by default; this page asks cooldown 20). The star appears at the point under the crosshair (`GrimoireBook.aim(caster, range)`, range at most 24), lifted 3 blocks above the first solid block below it (a star "hangs in the air above a point", so Conjunction's "beneath the star" is the ground point below). **Passive seeding (setting `star.passiveSeed`, default off):** the landing point of `comet`, `starfall` and `star_shot` also leaves one star, at most one per 2 seconds per player.
* **Entity.** NO new entity type. A star is a `MagicPropEntity` of a new `PropKind.STAR_NODE` (appended at the END of the enum; props are the repo's own "real 3D thing a spell puts in the world", `prop/MagicProps`, behaviours registered by `StarProps.init()`, painters by `client/prop/StarPropPainter`). Flags: no gravity, not solid, not hittable (damage ignored), scale 1, life 800 ticks (config), param = the owner's node index. Because it is a normal tracked entity, **steady state costs 0 custom packets**: vanilla tracking sends it only to players near it.
* **Server state.** One small pure class `star/StarNodes` (no game types, unit-testable) holds `Map<UUID, ArrayDeque<Node>>` where `Node = (entityId, dimension, x, y, z, expiryTick)`. All reads and writes happen on the server thread. `StarNodes` is the only place that knows the caps.
* **Caps (server config `nusmp-star-server.toml`, clamped like `ManaZoneGate`'s floors):**

| Cap | Default | Clamp | Why |
|---|---|---|---|
| nodes per player | 6 (heir: 8) | 1 to 8 | the quartet needs 4, the whip path 4, Halation 5 |
| nodes server-wide | 128 | 16 to 512 | hard bound of entities; the OLDEST node of anyone is evicted when it is exceeded |
| node life | 800 ticks (40 s) | 200 to 2400 | stars are a resource, not a base |
| place range | 24 blocks | 8 to 32 | `GrimoireBook.aim` already caps at 32 |
| seed cooldown | 20 ticks | 10 to 80 | at most 3 placements per second per player |
| Quartile barrier panes (Scutum) | 1 per player, 8 server-wide | fixed | see 2.5 |

* **Validation on the server (rule 9).** The client sends nothing new: the cast goes through the normal grimoire cast packet (the client only asks). On the server a placement needs: alive caster, the page unlocked, mana paid, target point inside a loaded chunk, line of sight (the `aim` ray), the star's block position not inside a solid block, the dimension recorded, the per-player and global caps applied (evict oldest). Conjunction re-checks that the chosen node still exists, is the same dimension, is within range of the caster, and that the destination has `noCollision` before the teleport.
* **Cleanup (no leaks):** on logout, death, dimension change and server stop the player's deque is cleared and the entities discarded (same pattern as `GrimoireSummon` and `ManaZoneManager`, `PlayerLoggedOutEvent`, `LivingDeathEvent`, `PlayerChangedDimensionEvent`); an entity whose owner UUID is gone is discarded on its own life counter, so a missed event costs at most 40 seconds.
* **Per-tick cost:** the entity behaviour is a life counter; `StarNodes` has no per-tick work at all (expiry is read lazily when a spell looks at the list, plus the entity's own `end()`); **no world scan, no chunk load**.

### 2.3 Lines (constellations)

Lines are NOT entities. When a quartile spell casts, the server picks the quartet (2.4) and sends the line segments as ordinary VFX activations (one `STAR_LINK` activation per segment, at most 5 per cast); the client draws them for their duration (30 to 40 ticks) and forgets them. A mage's idle stars show no lines (cheaper, and it keeps the screen readable).

### 2.4 The quartet and the automatic quartet

`StarMath.quartet(nodes, caster)` (pure): the **4 most recently placed live nodes** of the caster in the same dimension within 48 blocks; order them around their centroid (by angle in the plane that best fits them, a 4-point sort) so lines make a quadrilateral. Fewer than 4: with `star.autoQuartet = true` (default true) the cast places the missing stars on a ring of radius 3 in front of the caster (0 extra mana, at most 4 entities, counted in the caps); with `false` the spell fails with the message "Four stars are needed". Complexity: O(8) per cast.

### 2.5 Packets and effects (rule 11)

* Everything visual goes through `VfxSpawn.send` (to players within 96 blocks of the effect, through the per-tick `PerfBudgets.EFFECT_PACKETS` budget, 160 by default) and `Pages15Kit.Ctx.fx`, which already limits a cast to **10 packets**.
* Packets per cast: seed 1; Conjunction 3 (departure, link, arrival); Scutum up to 5 (4 links + the pane entity's own spawn); Hasta up to 6 (4 links, the lance, the impact); Flagellum up to 3 per sweep step x3 = at most 9; Halation up to 8 (5 beams + 3). All inside the 10-packet cast budget; over-budget ones are simply not shown.
* New shapes are APPENDED to `VfxShape` (`STAR_NODE`, `STAR_LINK`, `STAR_CONJ`, `STAR_PANE`, `STAR_LANCE`, `STAR_WHIP`, `STAR_HALO`) drawn by one new layer `StarMagicLayer`; `STAR_MAGIC` and `ElementIdentityLayer` are not touched (about 24 other callers).

### 2.6 Worst-case cost (rule 11), per feature

| Feature | Server | Network | Client draw (quads per frame, see star_new.md for the three looks) |
|---|---|---|---|
| Stars at rest | 128 prop entities server-wide, a life counter each; 0 scans | 0 per tick (vanilla tracking); 1 spawn per placement | one node = 4 quads (A), 5 (B), 18 + 2 cards (C, near only); 20 Star mages in view x 6 stars = 120 stars: A 480 quads, B 600 quads, C near-LOD 16 stars x 20 + 104 far stars as B sprites = about 720 quads |
| `star_seed` | 1 entity + 1 deque op; at most 3 per second per player | 1 | included above |
| Conjunction | 1 ray pick over at most 8 nodes, 1 `noCollision` test, 1 teleport, up to 3 allies teleported (tier cap, party only) | 3 | 2 flashes, 1 line |
| Scutum | 1 pane entity (health pool, see below), AABB at most 6 x 5 x 1.5; **no scan: projectiles hit the pane through vanilla entity collision**; the pane lives 5 s | 5 | pane + 4 lines + 4 stars |
| Hasta | 1 `foesAlong` AABB query (segment at most 32 long, width at most 4), at most 6 targets (MID tier), 1 delayed task (`Ctx.later`, 9 ticks, cancelled if the caster dies) | 6 | 1 lance (A 8 / B 16 / C 132 vertices) + flashes |
| Flagellum | 3 sweep steps x at most 4 `foesAlong` queries = 12 bounded queries per cast, spread over 12 ticks; each target at most 2 hits | 9 | whip of 16 segments (A 64 / B 92 / C 168 vertices) |
| Halation | up to 5 beams, one `Ctx.foes` query (radius 12 clamp), at most 8 targets (ZONE tier), 5 delayed hits | 8 | 5 rays + 1 impact flash |

A player can cast about one spell per 80 ticks at the starter tier, 160 at mid, 300 at zone (`Tier` cooldowns), so per-player server work is a handful of bounded queries every few seconds. Nothing runs per tick per player except what already exists.

### 2.7 Mana and tier numbers used below (from `Pages15Rules.Tier`, which every page of the book already follows)

| Tier | Cost | Cooldown (default) | Raw hit (before the balance law, `Ctx.hit` clamps at 1.5 x) | Pulse | Max targets |
|---|---|---|---|---|---|
| STARTER | 8 percent of mana, floor 150 | 80 ticks | 7 | 2 | 4 |
| MID | 12 percent, floor 300 | 160 | 12 | 3 | 6 |
| ZONE | 20 percent, floor 600 | 300 | 16 | 5 | 8 |
| SIGNATURE | 35 percent, floor 1200 | 600 | 26 | 7 | 10 |

## 3. Comprehensive ability list: canon effect and mod logic

All pages are appended after the 15 existing ones (new pages last, rule 2). Existing pages keep id, name, unlock bit and mastery order. A setting `star.nodes` (server, default true) turns the new behaviour on; `false` restores every cast of today exactly (V1). Damage always goes through `Ctx.hit` (balance law, tier clamp); no new damage engine.

### 3.1 Star Seed (the star itself) , NEW, mod-original mechanic from canon "create stars at will"

* **Anime / manga effect:** Star Magic creates stars and positions them; Yuno's stars hang in the air as small bright four-point stars (`EN`, `JA`; the look is the previews').
* **Mod logic:** page `star_seed`, STARTER, cooldown 20. Pseudo-code:
```
cast(c):
  aim = c.aim(24); if no hit: return c.fail("No point to hang a star over.")
  p = highestFreeAbove(aim, 3); if inSolid(p) or !loaded(p): fail
  StarNodes.add(c.caster.uuid, dim, p, now + life)          // evicts own oldest at the per-player cap, global oldest at the server cap
  MagicProps.spawn(level, STAR_NODE, p, 0, 1f, life, index, caster)
  c.fx(STAR_NODE, hand, p, 12, 1f)                          // 1 packet: a comet from the hand, the star pops
```

### 3.2 Conjunction (Teleportation) , REPLACEMENT of the cast (id `star_conjunction` kept)

* **Anime effect (manga ch. 309 / 310, `EN`, `JA` 合天):** a supplementary spell: with an open grimoire the user holds up two fingers and gestures a circle, then teleports to a spot beneath one of their stars; used to escape Zenon's subspace; can also move allies (`EN` Star Magic summary, `JA` ja.wikipedia: 自分はもちろん味方も).
* **Mod logic:** MID tier, cooldown 160 (existing), cost 12 percent. Target pick: among the caster's live nodes within 48 blocks in the same dimension choose the one with the smallest angle to the look vector, at most 20 degrees; none -> V1 dash (setting `star.conjunctionFallback`, default true; false = "No star to go to"). Destination = the ground point below the star (ray down 16 blocks, stand on top), must pass `noCollision` for the player's box (try the 4 neighbours if blocked). Allies: up to Tier.MID.maxTargets minus 1 allies within 3 blocks of the caster who are in the caster's party / allied (`GrimoireBook.around` already skips non-allies for damage; the ally filter is the existing one), moved by the same offset. The star is NOT consumed (decision, section 7); fall damage reset; 4 ticks of no incoming damage at departure and at arrival as the "evade" (setting, default 4, 0 = off). Server validation as in 2.2.
```
cast(c):
  n = StarMath.pickByLook(nodes(c.caster), c.caster.look, 20deg, 48)
  if n == null: return fallbackDash(c) or c.fail("No star to go to.")
  dest = groundBelow(n.pos); if !fits(c.caster, dest): dest = nearestFree(dest, 1)
  if dest == null: return c.fail("The star's ground is blocked.")
  fx(departure, link, arrival); teleport(c.caster, dest); teleportAllies(..); c.caster.fallDistance = 0; grantIframes(4)
```

### 3.3 Quartile Scutum (Shield) , NEW page `quartile_scutum`

* **Anime effect (ch. 309/310, `EN`, `JA` 四天・盾):** a defensive spell: four stars form a rectangle and beams between them make a barrier that bounces magic and holds against devil-powered magic. (A wiki page for ch. 355 also shows many stars forming a large circle that blocks laser fire, single source.)
* **Mod logic:** MID tier, cooldown 300 (Scutum is a defence, so a longer cooldown than the tier default), cost 12 percent. The quartet gives the four corners; the pane entity (`PropKind.STAR_PANE`, one per player, 8 server-wide) is a `MagicPropEntity` with a box hitbox of the quartet's bounding box (width at most 6, height at most 5, thickness 1.5), flagged hittable and solid for projectiles, health pool = `3 x Tier.MID.maxHit()` raw (so it follows the balance law like every hit), life 100 ticks (5 s). Anything that hits it through vanilla entity collision (arrows, fireballs, most mod projectiles) calls `MagicProps.Behavior.hurt`, which subtracts from the pool (damage tagged "devil" counts 50 percent) and returns false so the pane takes no vanilla damage. When the pool hits 0 the pane breaks (`end()`: shard VFX). The pane is oriented to face the caster. **No per-tick scan.** Things to check when building (not verified in this phase): that `SpellRuntime.bolt` (the mod's own bolts) respects pane entities; if it does not, add one AABB test per bolt against at most 8 panes.
```
cast(c):
  q = quartet(c) or return c.fail("Four stars are needed.")
  plane = fitPlane(q); box = aabb(q).inflate(thickness)
  pane = MagicProps.spawn(level, STAR_PANE, centroid(q), yawToward(caster), 1f, 100, 0, caster)
  pane.setHealthPool(3 * Tier.MID.maxHit()); registerPane(caster.uuid, pane)   // replaces the player's older pane
  c.fx(STAR_LINK x4 around the quad, STAR_PANE)             // 5 packets
```

### 3.4 Quartile Hasta (Spear) , NEW page `quartile_hasta`

* **Anime effect (EN list; JA 四天・槍; FAN ch. 310 "combined the light of the four stars into one"):** an offensive spell: light gathered in the middle of the four linked stars is fired as a high-penetration laser / spear.
* **Mod logic:** MID tier, cooldown 160, cost 12 percent. Charge 9 ticks (`Ctx.later(9, ...)`, cancelled if the caster dies or leaves): the quartet's links draw, the centre glows. Then a lance of light flies from the quartet's centroid along the line to the crosshair point (`c.aim(32)`; or the caster's look direction when nothing is hit), range 32. `Ctx.foesAlong(a, b, 1.2)` finds at most 6 targets; each takes `Ctx.hit(t, raw)` with `raw = Tier.MID.hit * (1 + 0.04 * quartetSize + 0.10 * min(1, mastery))`, clamped by `Ctx.hit` to 1.5 x the tier hit; heir mark x1.15 (still clamped). Piercing: all targets on the line (the cap above). The four stars are consumed (they "overload"); decision in section 7.
```
cast(c):
  q = quartet(c) or fail
  c.fx(links); c.later(9, () -> {
     if (!stillValid(q)) return;
     a = centroid(q); b = c.aim(32) ?: a + look*32
     for t in c.foesAlong(a, b, 1.2): c.hit(t, scale(raw)); knock(0.6).apply(t, caster)
     c.fx(STAR_LANCE, a, b, 8, 1f); c.impact(b, 1f); consume(q) })
```

### 3.5 Quartile Flagellum (Whip) , REPLACEMENT of the cast (id `quartile_flagellum` kept)

* **Anime effect (ch. 310, `EN`, `JA` 四天・鞭):** an offensive spell: starlight links four stars into a line so thin and sharp that it cuts through people and walls; Yuno teleported behind Zenon and swung it from four stars (`FAN`, ibtimes).
* **Mod logic:** STARTER tier (kept: the existing page is a starter), cooldown 90 (existing), cost 8 percent. The whip is a smooth path hand -> star1 -> star2 -> star3 -> star4 (Catmull-Rom, 16 samples); the stars swing in a 110-degree arc in front of the caster over 10 ticks. At the ticks 4, 8 and 12 the server samples the CURRENT path as 4 segments and runs `Ctx.foesAlong(seg, 0.6)` on each; a target is hit at most twice per cast (a small `Set<UUID>` per cast, discarded at the end); each hit is `tier.hit * 0.6` so a full double hit is about 1.2 hits (inside the 1.5 clamp). It does NOT break blocks (the canon "cuts through walls" is a spectacle; block damage is a grief risk on a public server, rule 9; setting `star.whipBreaksBlocks` is not offered). With fewer than 4 stars: auto-quartet (2.4) or V1 cone.
```
cast(c):
  q = quartet(c) or return v1Cone(c)
  for step in {4,8,12}: c.later(step, () -> for seg in path(q, step): for t in c.foesAlong(seg.a, seg.b, .6): if hits[t]++ < 2: c.hit(t, tier.hit*.6))
  c.fx(STAR_WHIP, hand, tip, 14, 1f) per step (3 packets)
```

### 3.6 Never-Never Land (Union Magic with Sylph) , KEPT (partly)

* **Anime effect (ch. 356, `EN`):** with Sylph, Yuno makes a star-and-wind dome over the Noble Realm; inside it time and magic flow can be controlled: allies' magic is strengthened, enemies' weakened; Lucius' time stop does not work on Yuno inside it; Sylph's wings take a star-and-wind pattern.
* **Mod logic:** the existing `never_never_land` page stays as is (solo reading: Speed + Strength 10 s on the caster, Weakness + Slowness on foes within 8 blocks). Optional upgrade behind the heir mark + `spirit_bond`: allies in 10 blocks also get the buff (party only, at most `Tier.MID.maxTargets`). Not designed further; no new engine.

### 3.7 Halation (Beam attack) , NEW page `halation`, mod-original (no canon found)

* **Anime effect:** none (name not found in `EN` or `JA`). Reading: the owner's brief calls it a beam attack in which the stars fire lasers at the crosshair or nearby enemies; as a name it is the photographic bloom of light round a bright source, which is exactly what the volumetric VFX direction draws.
* **Mod logic:** ZONE tier, cooldown 300, cost 20 percent. Uses up to 5 of the caster's stars (the 5 most recent, no quartet needed; fewer than 5 fire fewer beams). Targeting: the living entity under the crosshair within 32 blocks; if none, the foes of `Ctx.foes(aim, 8)` ordered by distance to the aim point. Beam i fires at tick `8 + 4 i` (staggered, `Ctx.later`), each `c.hit(target, Tier.ZONE.pulse * 0.9)`; a target may be hit by several beams; per-target cap 3 hits; at most 8 targets (ZONE cap); every beam is one `STAR_HALO`/ray activation, the impact is one flash. Stars stay (not consumed) but their remaining life is halved (decision, section 7).

### 3.8 Black Neverland (Star + Wind + Anti-Magic) , MISSING, optional

* **Anime effect (ch. 381 to 383, `FAN`; JA 黒のネバーランド):** Yuno's Never-Never Land fused with the Anti-Magic Asta gave him; used against Lucius after "the magic power disappears".
* **Mod logic (only if the owner wants it):** needs the flags heir + `spirit_bond` + an Anti-Magic flag; the zone also nullifies enemy spell damage in the dome. A new SIGNATURE page is not possible in the Pages15 kit (Star already has its one signature, `meteor_swarm`, `Pages15Rules.MAX_SIGNATURE_PER_BOOK = 1`); it would be a character-set page of a new Yuno-Star set (`acquire: all(flag heir, flag spirit_bond, flag anti_magic)`). Not designed in this phase.

### 3.9 Star Dome and Quintile (optional / unverified)

* Star Dome (ch. 355 single source): Scutum cast with 8 stars as a ZONE page, 8-second dome, radius 8 around the caster; optional.
* Quintile: Atrum Scutum / Hasta / Flagellum: unverified (section 0); NOT designed. If they are confirmed they fit as heir-mark pages that need 5 stars.

### 3.10 The 15 old pages

All kept and unchanged. Natural node-aware upgrades (optional, later): `star_chain` ties to the two nearest stars, `orbit_barrage` fires from the stars instead of the caster, `starfall` rains from the stars. Not part of this proposal.

## 4. Player combat loop

Example with a mid-game Star mage (the tier numbers of 2.7; a Spade grimoire holder, not yet heir, so 6 stars and no cooldown bonus). Mana is spent as a share of the mage's maximum.

1. **Open (0 s, mana 100 percent).** The mage walks into a fight with 2 blackened goons and a caster 18 blocks away. `star_seed` x4 around the arena while moving (8 percent each, 20-tick cooldown: 4 stars in about 3 seconds, mana 100 -> 68). Stars hang over the left flank, the middle, the right flank and behind the mage. The goons close in.
2. **Control the ground (4 s).** The enemy caster fires a bolt. The mage casts `quartile_scutum` (12 percent, mana 56): the four stars light up, the pane appears 4 blocks ahead; the bolt dies on it (pool 3 x 12 raw). The pane breaks after 5 s or when its pool is gone.
3. **Evade (6 s).** A goon is on the mage. `star_conjunction` (12 percent, mana 44): the mage aims at the star behind them, vanishes in a flash and appears on the ground under it, with 4 ticks of invulnerability; the star stays for the next jump. Cooldown 8 s.
4. **Strike (8 s).** The mage is now 12 blocks from the enemy caster in a straight line with the middle two stars. `quartile_hasta` (12 percent, mana 32): 9 ticks of charge (the stars' links draw), then the lance goes through the goon in the line and the caster: 2 targets hit, the four stars are used up (the mage has 2 stars left). Mana 32.
5. **Re-seed (10 s).** `star_seed` x2 (16 percent, mana 16) to get back to 4 stars while the cooldown of Hasta runs (160 ticks, 8 s).
6. **Crowd (14 s).** Three more goons arrive. `quartile_flagellum` (8 percent, mana 8 + regen): the whip swings 110 degrees through the group in 10 ticks; each target is hit up to twice.
7. **Finish (20 s).** With 5 stars up and mana regenerated to about 40 percent, `halation` (20 percent): five beams fire from the stars at the enemy caster under the crosshair, 8 + 4 i ticks apart; the stars lose half their remaining life.
8. **Reset.** Stars time out after 40 s, or on death, logout, dimension change. The mage starts again at step 1.

Mana and cooldown pacing: about 5 star placements and 3 quartile spells in 20 seconds consume about 100 percent of mana at the numbers above, so the fight is mana-tight on purpose: the stars are the resource, Conjunction is the escape, a Quartile is the payoff. Numbers are first-pass tier values; none were playtested.

## 5. How it plugs into the mod (no parallel engines)

| Need | Existing system used | New (appended) |
|---|---|---|
| Pages, costs, cooldowns, hit caps | `book/pages15/Pages15Kit` (`cast(Tier, Ctx)`, `Ctx.hit/foes/foesAlong/fx/later/impact`), tiers | page ids `star_seed`, `quartile_scutum`, `quartile_hasta`, `halation` appended to `book_star` |
| Stars and the pane | `prop/MagicProps`, `MagicPropEntity` | `PropKind.STAR_NODE`, `STAR_PANE` appended; `star/StarProps` |
| Rules, caps, quartet | pure classes tested like `ManaZoneGate` | `star/StarNodes`, `star/StarMath`, `star/StarConfig` |
| Lineage gate | `SagaRewards.cleared`, `Kingdom.SPADE`, flags like `spirit_bond`, quest event `arc.saga_clear` | flag `grinberryall_heir` |
| Visuals | `VfxSpawn`, `PerfBudgets.EFFECT_PACKETS`, `PerfProbe` | `VfxShape` entries appended, `StarMagicLayer`, client option `vfx.element.star` (like Fire) |
| Tests | `ServerSafetyTest`, `PerfSourceScanTest`, the CI server smoke test `SelfTests` | self-test prefix `star.nodes.*` (caps, quartet, cleanup, validation, packet count) |
| No keybinds | all casts are grimoire pages (the page list) | none |

## 6. NEW vs REPLACEMENT vs KEPT (rule 5)

* **NEW:** the node (star) system, `star_seed`, `quartile_scutum`, `quartile_hasta`, `halation`, the heir mark, new `VfxShape`s and layer, new props, config, tests; optional Star Dome / Black Neverland.
* **REPLACEMENT (behind `star.nodes`, V1 kept):** the cast of `star_conjunction` (dash -> teleport to a star) and of `quartile_flagellum` (cone -> whip through four stars). Page ids, names and unlock bits unchanged.
* **KEPT untouched:** the other 13 pages, `STAR_MAGIC` and `ElementIdentityLayer` (used by about 24 other files), the Star Shield buff, Never-Never Land, `MagicType.STAR`, the questionnaire, the cover system.

## 7. Open decisions for the owner

1. Heir gate: base Star book for everyone (this proposal) with the Spade-saga heir mark giving bonuses, or the whole Quartile family only for the mark?
2. Conjunction: does it consume the star (more balanced, less fluid) or not (this proposal)? Should it bring allies?
3. Hasta: are the four stars consumed (this proposal) or kept?
4. Halation: keep the name for a mod-original beam finisher (nothing in canon was found), or rename (e.g. the canon-sounding "Quartile Sagitta" is NOT canon either) or drop the page?
5. Do stars die when hit by Anti-Magic / a dispel (needs a hook), or are they always invulnerable (this proposal)?
6. A real second grimoire for dual users (a bigger change than a page set), or keep the fusion.
7. Passive seeding of old pages (default off here) on or off?
8. Quintile and Black Neverland: wait for the chapters to be verified, or skip.
9. Defaults of the caps (6 / 128 / 40 s) and whether `star.nodes` is on by default.

## 8. What was not done or not seen

* No code, no assets, no lang, no version change in this phase. Not built, not compiled, not run in a game; the previews are a headless software render.
* Not verified in source: that `SpellRuntime.bolt` respects pane entities (2.5 / 3.3); the exact tracking range of `MagicPropEntity`; whether `Ctx.later` tasks survive a dimension change (the Ctx already cancels on death or removal).
* Canon uncertainty: chapter of Hasta, Quintile spells, "Halation", Star = Sun + Moon, ch. 355 shield shape, anime episodes (none exist), Season 2 range.

## Appendix A. Audit of the existing Star Magic (what exists today)

### A.1 Where it lives

| Part | File | Fact |
|---|---|---|
| Book | `book/ElementBooks.star()` -> `ElementBook(MagicType.STAR, 0xFFFFF0A0, LIGHT_ELEMENTAL, ...)` | 6 own pages (star_shot, starfall, constellation, comet, star_shield, supernova) |
| Added pages | `book/pages15/Batch1.java` (`STAR = register("book_star", ...)`), spec in `docs/magic/pages15_batch1.md` | 9 pages added: quartile_flagellum, twinkle_dart, star_conjunction, never_never_land, star_beam, star_chain, orbit_barrage, stardust_field, meteor_swarm. Total **15** (`python3 -B tools/page_counts.py --ids book_star`: own 6, char 0, rest 0, basic_added 0). Room left: 20 new per book and 64 family pages (`Pages15Rules`), one signature per book (meteor_swarm uses it). |
| Character set | none for Star (`data/nusmp/character_spells/`: only `yuno.json`, book `book_wind`, mentions Star as a note) | no canon spell is bound to an owner |
| Wiki text | `.github/wiki/spell_*` has `spell_sekke_shooting_star.txt` (bronze creation, not Star Magic) and `spell_silver_star_of_execution.txt` (not Star Magic); **no Star Magic wiki page text exists in the repo** | |
| VFX shape | `VfxShape.STAR_MAGIC` ("starlight projectiles and constellations from 'from' to 'to'") drawn by `vfx/client/layer/ElementIdentityLayer` (it also draws SAND_MAGIC and MIST_MAGIC) | one beam + a spiral of sparks, 12 sparks max (`ctx.seg(12, 5)`); peak **104 vertices** per activation, engine cap 400 |
| Routing | `ElementBook.vfx()`: for `MagicType.STAR` EVERY shape a page asks for is rewritten to `STAR_MAGIC`; `empower()` (Star Shield) bypasses it and sends `SPIRIT_AURA` | so the book has exactly one visual |
| Shared use | `STAR_MAGIC` is also the "light beam" of hero weapons (spear, bow, katana, fan, scythe, instrument, book, sword), celestial / spirit-king props, Merlin's moves, the Gate ritual: **about 24 source files** | **changing the layer would change those too**, so the remake must ADD new shapes and keep `STAR_MAGIC` (rule 2) |
| Other | `MagicFlavors` (GUST + BRIGHT), `MagicGear` (Golden Dawn gear bonus for LIGHT and STAR), advancement `cast_star` | |

### A.2 Every page: name, canon or original, what it does, and the verdict against canon

Verdict legend: **exists** = canon spell present and behaves like the canon; **partly** = canon name or idea present but the mechanic differs; **wrong** = same name, different mechanic; **original** = a mod page with no canon spell of that name (kept, rule 2); **missing** = canon spell without a page.

| # | Page id | Name | Origin | What it does today (cast shape, tier) | Verdict |
|---|---|---|---|---|---|
| 1 | `star_shot` | Star Shot | original | volley of 3 bolts, damage 4, speed 1.8 | original |
| 2 | `starfall` | Starfall | original | field, radius 5, 60 ticks, damage 5 | original |
| 3 | `constellation` | Constellation | original name (canon concept: stars joined into a constellation) | volley of 5 homing bolts + Glowing; **no stars, no lines** | **partly** (the concept is the core of canon Star Magic, the page is a plain volley) |
| 4 | `comet` | Comet | original | bolt with burst, knockback | original |
| 5 | `star_shield` | Star Shield | original | empower: absorption III for 10 s, Absorption + thorns; draws SPIRIT_AURA | **partly** stand-in for Quartile Scutum (no stars, no barrier) |
| 6 | `supernova` | Supernova | original | nova radius 7, 14 damage, blind | original |
| 7 | `quartile_flagellum` | Quartile Flagellum | canon (EN Star Magic page, ch. 310) | starter, cone of 8 blocks, damage by tier, glow 3 s, anim "sweep" | **wrong**: same name, but it is a cone with no four stars and no whip line |
| 8 | `twinkle_dart` | Twinkle Dart | original | bolt that marks with Glowing 4 s | original |
| 9 | `star_conjunction` | Conjunction | canon (ch. 309 / 310) | mid, **dash 12 blocks forward**, no stars involved, no allies | **wrong**: the canon is "teleport to beneath one of your stars" |
| 10 | `never_never_land` | Never Never Land | canon union spell (ch. 356) | mid, solo reading: Speed + Strength 10 s for you, Weakness + Slowness on foes within 8 blocks; `STAR_MAGIC` swirl | **partly** (a union with Sylph in canon; solo reading is documented honestly) |
| 11 | `star_beam` | Star Beam | original | line 16 blocks, damage + Glowing 3 s | original (closest to Hasta / Halation) |
| 12 | `star_chain` | Star Chain | original | bind: one target, hold, Glowing 5 s (`THREAD_LINE`, drawn as `STAR_MAGIC`) | original |
| 13 | `orbit_barrage` | Orbit Barrage | original | zone tier, volley of 5 homing bolts | original |
| 14 | `stardust_field` | Stardust Field | original | zone field radius 5, 100 ticks, Glowing + Slowness | original |
| 15 | `meteor_swarm` | Meteor Swarm | original | signature, 6 strikes of radius 3.5, 2.6 power | original |

### A.3 Canon spell -> mod (the answer table)

| Canon spell (source) | In the mod today | Verdict | Plan (section 3, abilities) |
|---|---|---|---|
| Star creation (the stars themselves) | none: no star persists in the world | **missing** | `star_seed` page + node entity (NEW) |
| Constellation (stars linked by lines) | `constellation` volley | partly | lines drawn between nodes; the page stays |
| **Conjunction** (ch. 309/310, EN/JA 合天) | `star_conjunction`: 12-block dash | **wrong** | REPLACEMENT of the cast behind a setting, id kept, V1 dash is the fallback |
| **Quartile Scutum** (ch. 309/310, EN/JA 四天・盾) | `star_shield` absorption buff | partly | NEW page `quartile_scutum`; `star_shield` stays untouched |
| **Quartile Hasta** (EN list, JA 四天・槍, chapter not found) | `star_beam` line | partly | NEW page `quartile_hasta`; `star_beam` stays |
| **Quartile Flagellum** (ch. 310, EN/JA 四天・鞭) | `quartile_flagellum` cone | **wrong** | REPLACEMENT of the cast behind a setting, id kept, V1 cone is the fallback |
| **Halation** (owner's name, no canon found) | `orbit_barrage` volley is the nearest | missing (mod-original) | NEW page `halation` |
| **Never-Never Land** (ch. 356, union with Sylph) | `never_never_land` solo reading | partly | kept; gate by lineage + `spirit_bond` is an option |
| **Black Neverland** (ch. 381 to 383, `FAN`) | none | missing | optional late page; needs a character-set flag (a signature page is not possible: Star already has its one signature) |
| Star circle over the Noble Realm (ch. 355, `EN` single source) | none | missing | optional "Star Dome" = Scutum with 8 stars, ZONE tier |
| Quintile: Atrum Scutum / Hasta / Flagellum (unverified) | none | unverified | not designed |

## Sources (search-result pages; the page text of most could not be opened)

* English wiki: [Star Magic](https://blackclover.fandom.com/wiki/Star_Magic), [House Grinberryall](https://blackclover.fandom.com/wiki/House_Grinberryall), [Ciel](https://blackclover.fandom.com/wiki/Ciel_Grinberryall), [Loyce](https://blackclover.fandom.com/wiki/Loyce_Grinberryall), [Conjunction](https://blackclover.fandom.com/wiki/Conjunction), [Quartile Scutum](https://blackclover.fandom.com/wiki/Quartile_Scutum), [Quartile Flagellum](https://blackclover.fandom.com/wiki/Quartile_Flagellum), [Never-Never Land](https://blackclover.fandom.com/wiki/Never-Never_Land), [Chapter 355](https://blackclover.fandom.com/wiki/Chapter_355), [Chapter 356](https://blackclover.fandom.com/wiki/Chapter_356), [Yuno](https://blackclover.fandom.com/wiki/Yuno_Grinberryall)
* Japanese: [ja.wikipedia Black Clover](https://ja.wikipedia.org/wiki/%E3%83%96%E3%83%A9%E3%83%83%E3%82%AF%E3%82%AF%E3%83%AD%E3%83%BC%E3%83%90%E3%83%BC), [必殺技辞典](https://hissatuwaza.kill.jp/setume/burakuro0.htm), [RENOTE](https://renote.net/articles/14074), [pixiv encyclopedia Yuno](https://dic.pixiv.net/a/%E3%83%A6%E3%83%8E%E3%83%BB%E3%82%B0%E3%83%AA%E3%83%B3%E3%83%99%E3%83%AA%E3%82%AA%E3%83%BC%E3%83%AB)
* Fan / news (cross-check only): [epicdope ch. 309 Zenon](https://www.epicdope.com/story/black-clover-309-yuno-goes-head-to-head-with-zenon-with-star-magic/), [epicdope Yuno 2 grimoires](https://www.epicdope.com/black-clover-chapter-308-yuno-powers-up-to-2-grimoires/), [ibtimes ch. 310](https://www.ibtimes.com/black-clover-chapter-310-spoilers-last-leg-battle-3320205), [animeexplained ch. 381 to 383](https://www.animeexplained.com/spoilers/black-clover-chapters-381-to-383-full-spoilers/), [sportskeeda ch. 355](https://www.sportskeeda.com/anime/yuno-becomes-youngest-squad-captain-in-black-clover-chapter-355), [epicdope Star Magic](https://www.epicdope.com/how-powerful-is-yunos-second-magic-in-black-clover/), a Season 2 news aggregator ([Jump Festa 2026 report](https://www.bollywoodshaadis.com/articles/black-clover-season-2-premiere-details-jump-festa-2026-72769)).
