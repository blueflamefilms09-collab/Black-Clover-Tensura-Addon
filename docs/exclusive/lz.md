# Lucius Zogratis: Paladin Conversion (Soul Magic), 1.43.0

Nothing here was run in the game. See "What was and was not checked" at the end.

## New, replacement, kept

| | |
|---|---|
| **New** | Paladin Conversion (page), Paladin Recall / Dismiss (page), the Paladin marker effect, the Paladin stats and AI, the halo and wings, the follower limit, the config, the data tag `nusmp:paladin_immune`. |
| **Replacement** | None. Nothing the mod already had covers a permanent, player-owned conversion. |
| **Kept untouched** | The 1.42.0 Soul Touch (`PaladinTurns`, the 20 second loan), Lucius's boss Soul Paladins (`SoulPaladins`, `ZogratisEffects.SOUL_PALADIN`), every Lucius boss file. A creature that is one of those is refused by the conversion. |

## How it plays

Both pages are appended to the generic Soul book through `book/ext/SoulExt` (mode numbers 5 and 6 of the family list; no existing page moves).

* **Paladin Conversion** (signature, cost 35% / floor 1200, cooldown 15 s). Aim at a creature within 16 blocks. The page checks the rules, spends the shared control gate (`GrimoireBook.control`, the 12 s hard-control cooldown, as Red Thread does) and starts a three second channel: a white-gold magic circle follows the creature and a helix of light rises off it. The caster's mana is spent when the page is cast, so a broken or failed purification costs the mana. The channel breaks if the caster takes damage, walks more than 20 blocks away, dies, or the target dies or leaves the level. At the end the rules are checked again on the creature as it is now and the chance is rolled once. Success makes a Paladin; failure gives the target Weakness II for 4 s (6 s at 50% mastery or more).
* **Paladin Recall** (mid, cooldown 10 s). Normal cast: every loaded Paladin of the caster is teleported next to them (through a portal if need be). Sneak-cast: dismisses the Paladin under the crosshair, or all of them if none is aimed at.

### Who can be purified (`PaladinRules`)

Never: a player; a boss (BossIdentity, `Tags.EntityTypes.BOSSES`, or 300+ max health); a mob on the `nusmp:paladin_immune` entity tag (ender dragon, wither, warden, elder guardian, creeper, ghast, enderman, silverfish: bosses and the block-griefers); a named Tensura creature with 100+ health; a Zogratis family member; Lucius's boss Soul Paladins; something already your Paladin or another caster's; a tamed or called creature (`OwnableEntity` with an owner, a relic summon, a creature turned by the Soul Touch); something with more than ten times the caster's energy.

Chance (pure, tested): base 35% to 75% by mastery, up to +25% for a weakened target, +10% for a hostile creature, -35% to +15% from the energy gap (log scale), minus up to 50% for magic resistance, clamped to 5%..97%. A hostile creature at or below half health never goes under 90% (unless its resistance is 0.8 or more).

### What a Paladin is

An ordinary mob. Nothing is replaced. Four parts, all undone by a release:

1. A compound `nusmp_paladin` in its persistent data: owner id, conversion game time, the follower cap at the time. This is what saves it.
2. The marker effect `nusmp:paladin` (infinite, ambient, no icon, does nothing by itself). It is what the client reads to dress the creature, and the 10-tick drive puts it back if milk or a totem removes it.
3. Six permanent (saved) attribute modifiers: max health +100%, attack +60% (of the base), armour +8, toughness +4, speed +15%, knockback resistance +0.6, all scaled by `soul.paladinStrength`. A creature without one of those attributes skips it.
4. A place in the pure `PaladinRoster` (the limit).

It also glows (the vanilla glowing tag) and is made persistent (it does not despawn; a released creature stays persistent, there is no API to undo that).

**The one conversion heal.** At conversion the creature is set to its new full health once, as the owner asked. A Paladin is not a boss and has no regeneration; nothing here ever heals it again.

### Limit

`PaladinRoster.limit`: 3 + 1 per 25% mastery, never more than 6 (config can lower it, never raise past 6). When a new conversion would pass the limit, the oldest Paladin (by conversion time) is released, so the order is stable across reloads.

### What a Paladin does

* **Goal-driven mobs** (`PaladinGoals`): its own target goals are taken out (and remembered, weakly) and two goals are added: `DefendOwnerGoal` (the foe that hurt the owner, else the creature the owner last hit, within 8 seconds) and `FollowOwnerGoal` (walk back from 8 blocks when idle, always from 24, teleport from 32). The goal lists of `Mob` are protected and the mod has no access transformer, so they are read by reflection under the official names. If that fails the goals are simply not installed and everything below still works.
* **Every mob, goal-driven or brain-driven (Tensura)**: a drive every ten ticks (`Paladins.drive`, in the style of `RelicSummons.drive` and `PaladinTurns.drive`): marker, leash, target, and a blow when in reach for a creature that has no attack goal of its own (or no attack attribute at all, e.g. a cow, which strikes through a fallback). The `LivingChangeTargetEvent` handler redirects any target that is not fair to the owner's foe, or cancels it.
* **Never harms the owner**, their pets and allies, or the owner's other Paladins: `LivingIncomingDamageEvent` cancels the blow whatever way it arrives (melee, arrow, fireball, blast). The owner's own blows on their Paladin are cancelled too.
* **Safety caps** (`PaladinStats.capHit`): a Paladin's blow on a player is at most 25% of that player's max health; on a boss at most 2% of the boss's max health.
* **No block changes** at all: nothing for the Dungeon guard to veto, and the block-griefers are on the immune tag.

### Released when

* the owner uses Dismiss (all, or the aimed one), or the oldest when a new conversion passes the limit;
* the owner dies: every loaded Paladin is released at once (`PaladinRoster.releaseOwner`), and the death is written to a small world save (`PaladinMemory`, owner id and game time) so a Paladin in an unloaded chunk is released as it loads (`outlived`);
* the Paladin dies (normal drops, its place is freed);
* the owner logging out does nothing: the Paladins stay where they are, saved with the world, and simply stand still until the owner is back.

A released creature is **neutral** by default (its target goals are not given back). `soul.releasedHostile = true` gives them back (the creature is hostile again).

## Render

`client/soul`. One layer, `PaladinLayer`, is added to every `LivingEntityRenderer` of every entity type plus both player skins in one `EntityRenderersEvent.AddLayers`. It draws only when the creature carries the marker. Because Tensura's mobs are drawn by GeckoLib renderers that cannot take a layer, `PaladinOverlay` (a `RenderLevelStageEvent`) also draws any dressed creature the layer did not draw this frame (tracked in a small `IntOpenHashSet`), inside the frustum and within 64 blocks, in the same frame vanilla's living renderer uses. Both call `PaladinDraw`, which:

* reads the scale the renderer already applied from the pose and the creature's size from its bounding box, so the halo and wings fit a spider, a ravager, an enderman and a player: scale = clamp((height + width) / 2.4, 0.35, 4);
* puts the halo above the top of the bounding box and the wings between the shoulder blades (or on top of the back of a low, wide creature);
* draws two shared GeoLite models (`paladin_halo`, `paladin_wings`), each as: skin (full bright), glow map (additive, pulsing), and a faint wider additive bloom. Wings beat faster in the air.

The models are loaded once by `GeoModels` and shared; per frame only pose transforms are made (GeoDraw itself allocates a small corner array per cube, as it does for every model in the mod). The client keeps two tiny sets (the server's mark announcements and the ids drawn this frame), emptied on log-out.

The marker is also announced once a second by `PaladinNet` (the same pattern as `RougeCat`) as a safety net beside the vanilla effect sync, and withdrawn on release.

Assets: `tools/gen_paladin_assets.py` writes the two models, their animations, textures, glow maps and the 18x18 effect icon, plus the preview scenes `tools/render_preview/scenes.d/paladin.json` (`python3 -B tools/gen_paladin_assets.py --preview`, which runs the real `GeoDraw` through `tools/render_preview`; it needs JOML and Gson, see that script). The halo is 20 cubes, the wings 44.

## Classes (and why)

Pure, no Minecraft types, tested: `PaladinStats` (numbers, strength scalar, hit caps), `PaladinRules` (immunity table, chance, channel break), `PaladinRoster` (limit, eviction of the oldest, restore, owner release), `PaladinTargeting` (fair target, leash, grudge).
Server: `Paladins` (state, conversion, release, load, drive, recall), `PaladinChannel` (the three second channel), `PaladinCasts` (the page casts), `PaladinGoals` (goals, reflection), `PaladinEvents` (the game-bus handlers), `PaladinEffects` (the marker effect and its own DeferredRegister), `PaladinMemory` (owner deaths, world save), `PaladinNet` (marker announcement), `PaladinFx` (particles and sounds, existing circle shapes only), `SoulConfig`.
Client: `PaladinClient` (one registration line), `PaladinLayer`, `PaladinOverlay`, `PaladinDraw`.

## Config (`nusmp-soul-server.toml`, SERVER, plain defines)

`soul.paladinEnabled` (true), `soul.paladinMax` (6, ceiling 1..6), `soul.paladinBase` (3), `soul.paladinMasteryStep` (25), `soul.paladinStrength` (1.0, 0..3), `soul.releasedHostile` (false), `soul.paladinOnPlayers` (true; reserved for a player wearing the effect, read by the client with the default if the server file is not synced).

## Commands

None added. To test: give yourself Lucius's grimoire (`/multiverse grimoire canon <player> lucius`), learn Soul Magic, open the pages, summon the grimoire, cast on a weakened zombie. `/effect give <mob> nusmp:paladin infinite` dresses a creature (look only, no owner, no buffs).

## What was and was not checked

Checked: the four pure classes are unit tested (31 tests; mutation-checked: eviction order, limit constants, the reliable rule, the boss immunity, the player hit cap, the pet rule, the owner release each make a test fail). All Java was type-checked against the project stubs: every remaining error is a stub gap (whole missing packages or members the stubs do not carry: `MinecraftServer`, `CustomPacketPayload`, `MemoryModuleType`, `Minecraft`, `ServerPlayer.server`), and a deliberate wrong call is caught. Models were previewed through the real `GeoDraw` (symmetry, sweep and animation looked right).

Not checked (nothing run in game):
* that the vanilla effect sync reaches every tracking client (the announcement covers it if not);
* reflection on `Mob.goalSelector` / `targetSelector` (falls back to the drive);
* the Tensura magic-resistance attribute's scale (read as a fraction, or a percent above 1);
* `getMaxEP` for non-Tensura creatures;
* that `NeoForge`'s access transformer makes `LivingEntityRenderer.addLayer` public (the existing Rouge layer relies on the same);
* how the layer looks on each of the vanilla and Tensura bodies (the size fit is a heuristic from the bounding box);
* the feel of the numbers: chance, cooldowns, the 25% and 2% hit caps.
