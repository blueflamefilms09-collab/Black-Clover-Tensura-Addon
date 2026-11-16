# Dragon Balls, Super Dragon Balls and Shenron

Status: written, not seen in a game. Nothing in this document has been run in Minecraft; the pure rules are unit tested, the server self-test (`dragonballs.*`) runs in CI.

## 7 or 8 balls (read this first)

Canon is **seven** balls, in both sets (English Dragon Ball wiki: "Earth Dragon Ball", "Super Dragon Ball", "Shenron", "Super Shenron"). The owner asked for eight. The setting `dragonBalls.count` in
`nusmp-dragonballs-server.toml` is 7 or 8 (default **7**, values outside 7..8 are clamped).

To switch to 8: set `dragonBalls.count = 8`, restart (or use the config reload of your server), then run `/multiverse dragonballs scatter earth` and `/multiverse dragonballs scatter super`.
All 16 ball items (1..8 stars, both sets) are always registered, so nothing breaks when the count changes; stars above the count are simply not part of the ritual. The same
setting governs both sets.

## What exists (all NEW, nothing replaced)

| Part | Where |
|---|---|
| 16 ball items: `dragon_ball_earth_1..8`, `dragon_ball_super_1..8`, own 32x32 textures (orange / red stars and violet / gold stars) with a grey stone variant each | `dragonballs/DbItems`, `DragonBallItem`, `textures/item/dragon_ball_*` |
| Dragon Radar (craftable, optional) | `DragonRadarItem`, recipe `dragon_radar.json` |
| Server registry, scatter, stone lock | `DbData` (SavedData `nusmp_dragonballs`), `DbManager`, `DbRules` |
| Spoken ritual (chat) | `DbChat`, `DbRules` |
| Shenron / Super Shenron entity (non-living, not saved) and the session | `ShenronEntity`, `DbSession` |
| Wishes | `WishRules` (pure table), `DbWishes` |
| Dark sky for players nearby (one small optional packet) | `DbNet`, `DbSky`, client `DbClient` |
| Shenron model, renderer | geo `dragonballs_shenron`, `client/dragonballs/ShenronRenderer` |
| Settings | `DbConfig` (SERVER), `DbClient` (CLIENT, `skyDarkening`) |
| Commands | `DbCommands` |
| Server self-test | `DbSelfTests` (prefix `dragonballs`) |

KEPT, untouched: the Wave 101 relic "Seven Dragon Balls of Shenron", the legacy Dragon Ball skills (`skill/DbSkills`), the Saiyan code (only one hook: `DbSaiyanBridge`).

Models: **made with the geo generator (`tools/gen_dragon_balls.py` -> `tools/geo_builder.py`), not Blender** (no Blender exists in the build container; rule 14). Ball icons, the radar and the
Shenron skins are painted by the same script with PIL. A Blender pass for Shenron is the first thing to redo if the owner wants a finer dragon.

## How a player does it

1. Find the balls (Dragon Radar: right-click, shows the nearest ball within `radarRange` and its direction).
2. Bring the whole set together: carry them, or leave them on the ground within `gatherRadius` (6) blocks of you.
3. Say the words in chat (see below). If the balls are all there, the sky darkens near you and the dragon rises.
4. Make the wish by typing it, by clicking one in the menu, or by `/dragonwish <wish>`. Say `cancel` (or `never mind`, `farewell`, `goodbye`) to send the dragon away.
5. After the last wish the dragon leaves and the whole set turns to stone for `lockMinutes` (real time, 60 by default) and scatters again.

## The ritual words

Case and punctuation are ignored; whole words only; the words may be spread over up to 4 chat lines inside `phraseWindowSeconds` (10) per player.

| Set | Phrase (default) | Source |
|---|---|---|
| Earth | `Takkaradoppo Pakkoradoppo Yaikanaan` | The owner's brief called this "the Namekian phrase". It is the Namekian summoning chant as best remembered; **not verified against a wiki** (English wiki pages for Shenron did not carry it in the fetch available here). Treated as config, see below. |
| Earth | `Come forth, Shenron` / `Arise, Shenron` | English wiki, "Shenron" (the Earth Dragon's summoning call, "Arise, Shenron!"). |
| Earth | `Ideyo Shenron` | Japanese wiki (神龍), the Japanese summoning call "出でよ神龍". |
| Earth | `Detekoi Shenron` | From memory, **unverified**; kept as a config value. |
| Super | `Come forth, Dragon of the Gods, and grant my wish, pretty peas!` | English wiki, "Super Shenron" / "Super Dragon Ball". |
| Super | `Come forth, Divine Dragon, and grant my wish, peas and carrots` | The joke variant of the same chant; **uncertain**, config value. |
| Super | short forms `Come forth, Dragon of the Gods` / `Come forth, Divine Dragon` | Shortened so a player is not punished for a typo in the long line. |

Every phrase is a config list (`earthPhrases`, `superPhrases`), so a server can replace any of them without a rebuild. Wiki rule (English first, Japanese as the fallback): the "Source" column
names the page each phrase belongs to, but **the pages were not re-fetched while building this** (no wiki fetch ran in the build session), so every phrase is entered from the named page as remembered,
and none is verified word for word. Not looked up at all: the exact Namekian spelling, and a Japanese canon line for the Super dragon (超神龍 / ポルンガ pages). If a phrase is wrong, change the
config; nothing else depends on the wording.

## Wishes

Shenron grants a curated list, never free text. Wishes are found by id or keyword. Anything containing a forbidden word is refused first (immortality, `live forever`, `kill everyone`, `destroy`, `erase`,
`delete`, `world`, `mind control`, `enslave`, `steal`, ...). A refused or failed wish is **not** used up.

| Wish | Earth Shenron | Super Shenron | Cap / limit |
|---|---|---|---|
| revive | the named player (or yourself) if they died within `reviveMinutes` (60) | everyone who died within `reviveMinutes` and within 96 blocks of you, at most 16 | needs a recorded death; at most 256 deaths are remembered |
| power | +1 level | +2 levels | permanent, **lifetime cap `powerCapLevels` (default 10, hard max 20)**; 1 level = +2 max health and +0.5 attack damage, so the default cap is +20 health, +5 attack |
| coins | 20 gold coins (200,000 bronze) into the Tensura wallet | 100 gold coins | one fixed amount |
| skill | a random skill from a list of 6 | you choose one of a list of 8 | already-known skills are skipped |
| item | a random curated item (golden apples, totems, diamonds, netherite, elytra, experience, a Dragon Radar) | the same table, bigger counts, plus a nether star | every count is 64 or less |
| weapon | not offered | the Ruyi Power Pole relic (already in the mod, God grade), once per player | once per player |
| saiyan | becomes a Saiyan through the Saiyan code hook | same | `saiyanWish` setting; off = refused politely |

Never granted: immortality, mass kill, world edits, destroying others' items, mind control. Number of wishes per summoning: Earth `earthWishes` (default 1, canon), Super `superWishes`
(default **2**, canon is 1; the owner asked for "up to 2"), both limited to 1..3. The weapon reuses an existing relic on purpose: a brand-new weapon would need the engravings, Better Combat file and Blender model
that rules 6, 7, 13 and 14 ask for.

Every wish (set, player, wish, outcome) is written to the server log (`[dragonballs]`).

## The stone lock and the scatter

* After the session ends with at least one wish granted, `DbManager.finish` locks the set for `lockMinutes` (real time, epoch milliseconds, stored in the world: it survives a restart and counts
  while the server is off), turns the set to stone and scatters it again. A stone ball cannot be used in the ritual; when the lock ends the balls on the ground and in bags wake up.
* Lock maths (`DbRules.lockUntil/locked/remainingMs`): `until = now + minutes * 60000` (minutes clamped 0..10080, 0 = no lock), locked while `until > now`.
* Scatter (`DbRules.scatter`): the set's points are drawn in one go inside the ring `scatterMinRadius`..`scatterMaxRadius` (300..3000) round the world spawn, each at least
  `scatterMinSeparation` (500) apart. At most `60 * count` tries; every 20 failed tries the separation is halved, so it always returns exactly `count` points. Points are clamped inside the world border.
  Overworld only.
* A scattered ball is a **pending row**: the real item is only created when the chunk is already loaded by some player (`getChunkNow`, never a chunk load), at most 4 per 5-second pass. So the balls
  appear where players explore, and the Dragon Radar already points at pending places.
* Balls on the ground are invulnerable, never despawn and glow. A ball that falls out of the world (void) or burns is relocated to a fresh place of its own (`relocate`). A scatter bumps a generation
  counter: old balls still in someone's bag turn to dust the next time they are touched ("crumbles"), so a rescatter can never duplicate the set.
* `autoScatter` (default on): on server start, a set with a missing ball is scattered; a ritual that was interrupted by a restart scatters its set again.

## Commands (permission level 2)

* `/multiverse dragonballs status` - lock, ritual and ball places of both sets.
* `/multiverse dragonballs scatter <earth|super>` - scatter the set now.
* `/multiverse dragonballs unlock <earth|super>` - remove the lock.
* `/multiverse dragonballs lock <earth|super> <minutes>` - set the lock.
* `/multiverse dragonballs end <earth|super>` - end a running ritual (with the farewell).
* `/dragonwish <wish> [words]` - the clickable menu runs this; only the current summoner can use it.

## Session rules

One summoning at a time per set (a second attempt is told "busy"). At most two sessions can exist (one per set). A session ends by itself after `sessionSeconds` (300) with a farewell, and immediately if the
summoner dies, logs out or changes dimension. Shenron cannot be hurt and is not saved with the world.

## Settings (`nusmp-dragonballs-server.toml`, section `dragonBalls`)

| Key | Default | Meaning |
|---|---|---|
| `enabled` | true | master switch |
| `count` | 7 | balls per set, 7..8 (canon 7) |
| `earthWishes` / `superWishes` | 1 / 2 | wishes per summoning, 1..3 |
| `lockMinutes` | 60 | stone lock after a wish (real minutes) |
| `sessionSeconds` | 300 | how long the dragon waits |
| `gatherRadius` | 6 | blocks, for balls lying on the ground |
| `phraseWindowSeconds` | 10 | window in which the words may be spread over several lines |
| `autoScatter` | true | scatter on start when needed |
| `scatterMinRadius` / `scatterMaxRadius` / `scatterMinSeparation` | 300 / 3000 / 500 | the scatter ring and spacing |
| `radarRange` | 2000 | Dragon Radar range |
| `powerCapLevels` | 10 | lifetime ceiling of the power wish (hard max 20) |
| `reviveMinutes` | 60 | how long after death a revive is still possible |
| `saiyanWish` | true | allow the Saiyan wish |
| `earthPhrases` / `superPhrases` | see above | the words |

Client (`nusmp-dragonballs-client.toml`): `dragonBalls.skyDarkening` (default true) - turns the dark screen tint and fog off for this player.

## Cost (rule 11), worst case

* Entities: at most 2 Shenron entities (one per set), non-living, not saved, tracking range 16 chunks; at most 16 ball items in the world (8 per set maximum); bolts are visual only (2 per summoning).
* Server work: a sweep every 5 seconds over at most 16 registry rows (at most 4 item spawns, loaded chunks only); a one-second pass over at most 2 sessions; one registry write per carried ball per
  5 seconds (only marked dirty if the ball moved more than 2 blocks); one entity query (a box of `gatherRadius`) and an inventory pass per accepted spoken phrase, and a chat line is dropped past 256 characters.
  Per player a 3-second cooldown after a phrase attempt. Nothing runs per tick per player.
* Chat: per-player rolling window of at most 4 lines of at most 200 characters, removed on logout.
* Network: one 6-byte optional server-to-client packet (`SkyPayload`) per nearby player (within 192 blocks) when a session starts and again every 10 seconds while it lasts. No client-to-server packets
  at all (the menu is chat click commands, validated by the command: only the summoner).
* Client: while a dragon is out, one translucent quad per frame (the dark tint) and one colour multiply per fog computation; the dragon is one geo model draw (70 cubes, about 420 quads, no allocation per frame) per dragon;
  3 END_ROD particles every 6 ticks per dragon. Nothing at all when no dragon is near.
* Persistence: one small SavedData (2 sets x 8 rows). Cleanup: chat windows on logout; sessions on summoner death, logout and dimension change; the in-memory death records (revive wish) are
  capped at 256, used up when a revive happens, and entries older than 24 hours are purged when the map is full (they are not saved: after a restart nobody can be revived).

## Tests

* `DbRulesTest` (count clamp, phrase matching with case/punctuation/windows, lock maths, scatter separation and determinism), `WishRulesTest` (caps, forbidden words, matching, gift tables),
  `DbFilesTest` (every texture/model/lang key exists, server-safety scans of the package).
* Server self-test prefix `dragonballs` (keys `dragonballs.rules`, `dragonballs.scatter`, `dragonballs.ritual`): the CI smoke list must include the prefix.

## Caveats (honest)

* **Not seen in a game.** Everything compiled only against the reference sources and ran only as pure tests; the visuals (dragon, sky, balls) have not been looked at in Minecraft.
* The sky darkening is a screen tint plus a darker fog colour, not a real change of the sky dimension effects.
* Shenron is a geo-generator model, not Blender.
* Earth's Namekian phrase and `detekoi shenron`, and the Super "peas and carrots" variant, are not verified (see the table).
* A ball inside a chest or ender chest is tracked by the last carrier position only; the radar points to that place.
* Balls only exist physically where a chunk is loaded; far scatter points are pending rows until someone gets near.
* No hidden-structure placement for the balls (they lie on the surface), overworld only.
* A ball created in creative mode (no generation tag) is accepted as current.
* The Super weapon is the existing Ruyi Power Pole, not a new weapon.
