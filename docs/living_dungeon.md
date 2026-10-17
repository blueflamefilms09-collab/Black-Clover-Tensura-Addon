# The Living Dungeon (Tensura x Black Clover x Delicious in Dungeon)

> **REMOVED in 1.58.0 (owner's request).** The Living Dungeon described below (the cave dimension `nusmp:living_dungeon` with its ecosystem, tiers,
> atmosphere, the Golden City, the Variable Core zones, vaults and collapse, the sentient rooms and immune system, the Maw, the wild food web, the
> Goliath pit and the Charybdis basin, `/multiverse dungeon ...` and `DungeonConfig`) was removed. Kept: the creatures, the two floor bosses, the
> meals and their skills, the Guard, the Treasury relics, the economy's dice and stone tables and the synthesis materials, because other systems
> still use them. What stayed and what went, with the reason for each, is in `docs/labyrinth/mega_v2_prune.md`. The text below is kept as history.

Technical design document, translated to what this mod actually runs on: **NeoForge 21.1 / Minecraft 1.21.1**. The original
design was written against Forge 1.20.1 (capabilities, `AttachCapabilitiesEvent`, `BiomeModifier` for the dimension). Where the
two differ, this document says what is built instead.

Status per stage is at the bottom. Nothing here has been run in game yet; CI compiles it.

---------------------------------------------------------------------------------------------------------------------------

## 1. The closed-loop ecosystem

The Dungeon (`nusmp:living_dungeon`) is an enclosed cave world. Vanilla spawning is bypassed in it: nothing is placed by the
chunk generator or the natural spawner (`FinalizeSpawnEvent` of type `NATURAL`, `CHUNK_GENERATION`, `STRUCTURE`, `PATROL`,
`REINFORCEMENT` is cancelled when the level is the Dungeon). Every creature in it is there because the ecosystem put it there.

### Food web
| Tier | Who | Eats | Is eaten by |
|---|---|---|---|
| 0 | Dungeon Mycelium, moss, magicules (the producers) | the carcass-fed soil | Tier 1 |
| 1 | Tensura-style **slimes** (scavengers) | mycelium, moss, carcasses; absorb magicules and grow | Tier 2 |
| 2 | **Living Armors** (mollusk colonies living in discarded armour) | slimes, moss | Tier 3 |
| 3 | **Basilisks** and **Direwolves** | Tier 2 and Tier 1 | nothing: they starve, or die to adventurers |

### Per-chunk data: `EcosystemData`
| Field | Range | Meaning |
|---|---|---|
| `magicule` (MagiculeDensity) | 0-1000 | free mana in the air and the stone. Slimes feed on it, carcasses give it back, mining and spell-casting spend it. Gates breeding. |
| `biomass` | 0-400 | the weight of life living in the chunk. Gates how many more creatures can be born; falls when they starve or are killed. |
| `oxygen` (OxygenLevel) | 0-100 | breathable air. Fire consumes it; below 35 players are *Suffocating*. Regrows slowly, faster where the Mycelium grows. |

Forge 1.20.1 capability -> **NeoForge data attachment**: `LivingDungeon.ECOSYSTEM` is an `AttachmentType<EcosystemData>`
registered on `NeoForgeRegistries.ATTACHMENT_TYPES` with a codec, read with `chunk.getData(LivingDungeon.ECOSYSTEM)` on a
`LevelChunk` and marked with `chunk.setUnsaved(true)` after a change. There is no `AttachCapabilitiesEvent`, no
`ICapabilityProvider`, no `LazyOptional`.

Every `ECOSYSTEM_TICK` (every 100 ticks, only for chunks within ~4 chunks of a player, where the simulation is loaded):
1. oxygen drifts toward its ceiling (100, or 60 in the Volcanic Vents), faster near Mycelium;
2. magicule regenerates a little (the Yggdrasil roots in the stone) and is eaten by the slimes in the chunk;
3. biomass follows the number of creatures counted in the chunk; if the chunk holds more than it can feed, the hungriest die (starvation);
4. breeding: a tier may add one creature if the tier below it is plentiful enough and the chunk's magicule/biomass allow (stage 2).

### Hunger and carcasses (stage 2)
Every Dungeon creature has a Hunger value (0-100, data attachment on the entity). Hunger falls with time; at 0 the creature takes
starvation damage. A **full** predator ignores players and prey and sleeps (`SleepGoal`); a hungry one hunts
(`HuntForSustenanceGoal`: when hunger is at or below the threshold, the nearest valid prey within 16 blocks, kill -> hunger +50,
magicules +20); territorial animals defend their den (`TerritorialDefendGoal`).

Standard loot is disabled in the Dungeon. A kill leaves a **Carcass** (a block, `CarcassBlock`, no item drops). Scavengers eat it
(restoring hunger); left alone it **decays**, returning its nutrition to the chunk's magicule and feeding the Mycelium, which
regrows the terrain. So a kill is never free: it moves matter around the loop.

### Terrain regrowth
Mined blocks are not gone for good. Each block an adventurer breaks (stone, deepslate, ores, dripstone) is remembered
(`DungeonSave`, saved with the world) and put back after a number of days (`dungeon.regrowDays`, default 2), never while a
player stands close. Meanwhile a `Dungeon Mycelium` veil grows in the hole. Mycelium also appears where carcasses decay and is what
the slimes graze.

---------------------------------------------------------------------------------------------------------------------------

## 2. Dynamic portals: the Dungeon Maw

The Overworld's *ambient magicule density* is tracked per region of 4x4 chunks (`DungeonSave`, saved with the world). Every
big spell adds to it (the cast hook in `GrimoireBook` adds `cost%^2 / 100`), it decays by itself, and when a region crosses
`dungeon.mawThreshold` (default 600) the ground there tears open: a **Dungeon Maw**, a mouth of dense mana wrapped in living roots.

- The Maw stays open for `dungeon.mawMinutes` (default 20). Stepping into it drops you into the Dungeon (at the same x/z, onto a
  safe floor), and a **Root Gate** is placed where you land to take you back to where you stood.
- The roots that wrap it are an obstacle and a weakness: **chop the roots down** (an axe is best). When every root is gone, the Maw
  collapses; when its time runs out, it closes by itself.
- A region that has just opened a Maw drops back to a quarter of the threshold, so a long battle opens several, not a hundred.

### Global broadcasts
Ominous server-wide messages (`dungeon.broadcast`): a Maw opens (rounded to the nearest 100 blocks), a Maw closes, the **Golden City
seal breaks** (the first time any player on the world enters the Golden City tier), and (stage 2) **an ecosystem collapses** (a chunk
loses every tier-1 creature).

---------------------------------------------------------------------------------------------------------------------------

## 3. Atmospheric tiers

The tier is a function of depth and place (`DungeonTier.at`):

| Tier | Where | Rule |
|---|---|---|
| **Upper Catacombs** | y >= 24 | dark, cool, plenty of air. The safe-ish entry floor. |
| **Deep Waterways** | -40 <= y < 24 | the caves flood below sea level. Water conducts: **an electric attack on something standing in water jumps to every swimmer within 6 blocks** at half strength, and stuns them. Water magic is the way through. |
| **Volcanic Vents** | patches of 112 blocks over the upper and middle floors | burn damage unless fire-resistant or wet; the air is thin (oxygen cap 60). **Fire magic and fire damage consume the chunk's oxygen**, and at 35 or less everyone in the chunk is Suffocating. |
| **Golden City** | y < -40, in rare 128-block cells | the core. **Magicule radiation**: *Magicule Poisoning* grows every few seconds and cuts maximum health, unless you have a Mana Zone up. |

Effects (`DungeonEffects`, registered mob effects): **Suffocation** (damage over time, mining and movement slowed), **Magicule
Poisoning** (maximum health -10% per level, 5 levels), **Magicule Sickness** (nausea and weakness: raw monster meat, stage 3).

---------------------------------------------------------------------------------------------------------------------------

## 4. Harvesting and cooking (stage 3)

- **Butcher Cleaver** (a new weapon, with Tensura engravings and a 3D model): used on a Carcass it cuts out the *cuts* the creature
  has by anatomy (a slime: jelly and a core; a Living Armor: flank and shell; a Basilisk: tail, eye, fillet; a Direwolf:
  haunch, hide). A blunt tool (a hammer, a pickaxe, a fist) tears the carcass and ruins the meat (the cuts come out ruined).
- **Raw monster meat is toxic**: eating it gives Magicule Sickness, unless the eater has *Predator* or *Gluttony*, or it was
  purified (Holy Water in the crafting grid).
- **Cooking** (furnace, smoker, campfire) makes safe meals that grant buffs, a temporary skill or a secondary Black Clover magic
  attribute for a time, depending on the creature: the Delicious in Dungeon part.

---------------------------------------------------------------------------------------------------------------------------

## 5. Architecture, translated to 1.21.1

| Design (Forge 1.20.1) | Built (NeoForge 1.21.1) |
|---|---|
| `IEcosystemData` capability on `LevelChunk` | `EcosystemData` data attachment (codec) on `LevelChunk`, `LivingDungeon.ECOSYSTEM` |
| `AttachCapabilitiesEvent<LevelChunk>` | none needed: `chunk.getData(type)` creates the default on first use |
| Entity capability for hunger | entity data attachment `LivingDungeon.HUNGER` (stage 2) |
| `BiomeModifier` JSON for an enclosed dimension | a real dimension: `data/nusmp/dimension/living_dungeon.json` (vanilla `minecraft:caves` noise settings, so it is a stone cave world with a bedrock roof and floor and sea-level water) and `dimension_type/living_dungeon.json`; natural mob spawning is cancelled in code instead of by biome spawner lists |
| `MobSpawnEvent` | `FinalizeSpawnEvent` (cancel by spawn type) |
| `LivingDropsEvent` | same event name in `net.neoforged.neoforge.event.entity.living` |
| `Goal` subclasses | same (`net.minecraft.world.entity.ai.goal.Goal`), registered from `registerGoals()` of the entity |
| Config | `nusmp-dungeon-server.toml` (`[dungeon]` section), `DungeonConfig` |

`HuntForSustenanceGoal` (the design's sample), as it will be written for 1.21.1 in stage 2:

```java
public class HuntForSustenanceGoal extends Goal {
    private final PathfinderMob mob;
    private final double speed;
    private LivingEntity prey;
    public HuntForSustenanceGoal(PathfinderMob mob, double speed) { this.mob = mob; this.speed = speed; setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }
    @Override public boolean canUse() {
        if (Hunger.get(mob) > Hunger.THRESHOLD) return false;                         // full: sleeps, ignores everything
        prey = mob.level().getNearestEntity(LivingEntity.class, TargetingConditions.forCombat().range(16).selector(this::isPrey), mob, mob.getX(), mob.getY(), mob.getZ(), mob.getBoundingBox().inflate(16));
        return prey != null;
    }
    @Override public void tick() {
        mob.getLookControl().setLookAt(prey, 30f, 30f);
        mob.getNavigation().moveTo(prey, speed);
        if (mob.distanceToSqr(prey) < 2.5) {
            mob.doHurtTarget(prey);
            if (!prey.isAlive()) { Hunger.add(mob, 50); Ecosystem.addMagicule(mob, 20); prey = null; }
        }
    }
}
```

---------------------------------------------------------------------------------------------------------------------------

## 6. Stages

| Version | Stage | What |
|---|---|---|
| **1.17.0** | Foundation | this document; the dimension and its type; `EcosystemData` and its tick (oxygen, magicule); tiers and their hazards (heat, suffocation, magicule radiation, conductive water, fire eating oxygen); spawn bypass; mined-terrain regrowth; the Dungeon Maw (ambient density, roots, entry, Root Gate); global broadcasts; the three effects; `/multiverse dungeon ...`; `nusmp-dungeon-server.toml` |
| **1.18.0** | The food web | Hunger; Dungeon Slime, Living Armor, Basilisk, Direwolf; the goals; breeding, starvation, extinction; carcasses; no drops |
| **1.18.0** | Table | Butcher Cleaver; cuts, raw toxicity, Holy Water, cooked meals with buffs and temporary skills/attributes |
| **1.19.0** | Polish | the Dungeon uses the **real mobs of base Tensura** (and Ice and Fire if installed) through an entity-type-tag roster instead of the custom 1.18.0 creatures; every block of the Dungeon is **unbreakable** (events plus a Level mixin); the **Golden City** is built; Magic Sense blunts the radiation; cooked meals lend a skill and a second magic; a reward for cutting a Maw's roots; the Dungeon's own sky, fog and sounds |
| **1.20.0** | Bosses | the four Zogratis bosses (docs/zogratis_bosses.md) |
| **1.21.0** | The Variable Core | the deepest floor builds one of four zones per core cell; hidden vaults and Descent Gates; spatial distortion; the lantern, the compass and organ extraction; five Relics; the Treasury Collapse; Maw portals as towers, ruins and gates (section 8) |
| later | Polish | predator den structures, pack behaviour for the real mobs |

---------------------------------------------------------------------------------------------------------------------------

## 7. What exists now (1.19.0) and what is not done

Done: everything in sections 1 to 5, and the 1.19.0 polish (see the stage table). Never run in game; CI compiles it.

The roster: `data/nusmp/tags/entity_type/dungeon/*.json` (tier1, tier2, tier3, legendary, grazers, aquatic, excluded) lists the entity types that live in the Dungeon; every entry is optional, so a mod that is not installed (Tensura, Ice and Fire) is simply skipped, and a modpack can edit the tags. The custom 1.18.0 creatures stay registered (old worlds may hold them) but nothing spawns them any more.

Unbreakable: `dungeon/DungeonGuard` (config `unbreakable`, default true) refuses every break, explosion, mob grief, piston, tool change and fire/fluid/air replacement of a Dungeon block, and a mixin on `Level`/`LevelChunk` catches mods that call `destroyBlock`, `removeBlock` or `setBlock` directly. The mod's own builders run inside `DungeonGuard.internal(...)`. Carcasses and mycelium are exempt on purpose (the ecosystem eats them).

Not done yet:
- predator den structures and pack behaviour for the real mobs (Tensura's Direwolf already spawns as a pack by itself);
- the Golden City's loot-free rewards beyond the Aurelian Core;
- the Dungeon has no custom sound files (vanilla sounds only).

---------------------------------------------------------------------------------------------------------------------------

## 8. The Variable Core (1.21.0): the Procedural Dimensional Nexus

Source: the owner's design document "The Living Dungeon, the Variable Core, Hidden Vaults and Navigation Mechanics", translated from Forge 1.20.1 to NeoForge 1.21.1 (`dungeon/nexus/**`). Never run in game; CI compiles it.

### 8.1 Cells and zones
The deepest tier (y below -40, in the 128 x 128 block cells that were the Golden City's) is called **The Variable Core** (enum name `GOLDEN_CITY` is kept for saved data). `NexusBuilder` builds a core cell when a player in the Dungeon comes within 56 blocks of it: lazy, resumable after a restart, budgeted per tick, loaded chunks only, written through `DungeonGuard.internal`. A stable hash of the cell chooses one of four zones (`NexusZone`), the same on the server and the clients. A Golden City built by 1.19.0/1.20.0 is adopted as it stands, never built over. State is `nusmp_nexus` (`NexusSave`): PENDING, BUILDING, DONE, COLLAPSING, COLLAPSED.

| Zone | Source | What |
|---|---|---|
| **Monarch's Vault** | Solo Leveling | a hollow obsidian hall piled with gold, artifacts and bones; since 1.22.0 the Blood-Red Commander (4800 health) sleeps on the dais: arts Lunge, Cross and Executioner's Arc besides Domain Slam, Shadow Rain, Soul Rend and the Authority, two phases (50% and 25%), forced summoning of soldiers; its fall collapses the Nexus |
| **Underworld Gate** | Spade Kingdom | a gravity-warped ruin round a vast Gate; gravity zones press down, let go and flip on a visible cycle (telegraphed 4 s ahead, never lethal); a Gatekeeper daemon, lesser daemons and Dark Disciples (10 to 40% of a Devil's power: Lucifero, Megicula, Beelzebub) |
| **Tempest Mirage** | Tensura | an illusion of a monster village (plaza, town hall, 18 houses, fake houses that dissolve into ambushes) held by a hive-mind swarm of mutated Tensura orcs, lizardmen, slimes, goblins and hound dogs (cap 40, waves of 8 while non-creative players are inside); scouts, tanks, flankers, skirmishers; the hive announces every change of state; damage per blow is capped at 10% of max health and 20% per second |
| **Golden City** | Delicious in Dungeon | the 1.19.0 city inside a pristine sealed biosphere of golden and azalea great trees, glow berries, waterfalls and giant mushrooms; apex and legendary predators over herds of grazers; the Biosphere cycle shifts the garden near players |

The Dungeon's own breeding is **switched off** in the Monarch, Underworld and Mirage zones and in the Golden City's garden: only what the zone makes lives there (the hive, `GoldenWild` with caps of 8 apex and 40 prey). Creative and spectating players are never fought and spring no fake house. Every zone opens ways in from the caves (the Dungeon is unbreakable, so the builder cuts them) and retries while players are near. Each zone has a **Treasury** behind a Mana Gate, minor relic pedestals, and exactly one **central** relic. Taking the central relic, or killing the Monarch, collapses the Nexus.

Admin: `/multiverse dungeon nexus where | build | zone <monarch|underworld|mirage|golden> | reset`.

### 8.2 Hidden vaults, Descent Gates, spatial distortion
Illusion Stone and Illusion Deepslate (no collision, block light, water cannot pass) hide Puzzle Vaults, Nutrient Gardens, Guardian Vaults and Descent Shrines. A Descent Gate sends the player to the next floor at the same x/z (Upper y 24 and up, Deep 24 to -40, Core below); Deep to Core only above a built Nexus. Warp Traps throw the player 8 to 24 blocks; gravity pulses lift or slam without damage; blocks placed in a pocket are refused. The Band of the Spatial Anchor ignores all of it; `dungeon.hazards = false` switches it off. Admin: `dungeon vault where | build`, `dungeon gate where`.

### 8.3 Navigation and organs
Ki-Resonance Lantern: off-hand, fuelled (tag `nusmp:lantern_fuel`), amber / blue (illusion wall within 14 blocks) / red (descent gate within 96 blocks). Anti-Bird Compass: rare drop of avian creatures in the Upper Catacombs; points at the nearest cluster of life, spins in distortions. Butcher Cleaver organ cuts by click height: Venom Gland (basilisk, spider), Magic Stone (anything above tier 1).

### 8.4 Relics
Five Relics, worn (Curios, else off-hand) and attuned (sneak 10 s holding it; max 3; death breaks them), each with a curse: Glutton's Chalice (Symbiotic Hunger), Mantle of the Scavenger (Prey's Weakness), Band of the Spatial Anchor (none), Devil-Bound Weapon Sheath (Mana Siphon), Gambler's Grimoire Page (Wild Magic Surge). Commands: `dungeon relic give | attune | release | status`.

### 8.5 Maw portals
A Maw manifests as a stone tower, ruins or an isolated gate (`MawShapes`, `MawFit`, `MawStyle`, `MawManifest`), picked by a hash of the place and the biome; if it does not fit, the other two are tried, then the 1.17.0 plain mouth. `MawFit` checks the whole patch before anything is written (loaded chunks, slope, soft blocks only, no fluid, no creature, write cap); `MawManifest` writes with a ledger and rolls back on failure. At least 8 roots always stand; closing removes only the roots and the Maw, so the stone stays. Wordings: "[Server] A spatial distortion has materialized. A new Dungeon Tower|Ruin|Gate has breached the <Biome> biome near (x, z)..." and "[Server] A localized famine in the <tier> has caused a mass predator die-off." Admin: `dungeon maw open [tower|ruin|gate|hole]`.

### 8.6 The Treasury Collapse
`DungeonCollapseEvent` (central relic taken, the Monarch killed, a Wild Magic Surge) starts a three minute collapse (`CollapseManager`, saved as `nusmp_collapse`): the cell is marked COLLAPSED at once; 3:00 broadcast; cracks; from 2:00 the stone falls bottom-up from the far side towards the exits (the exit corridors stand until the end); a Root Gate at each exit; five waves of mutated guards (cap 10 alive, 3 per exit, 24 in all); at 0:00 the Nexus is wiped and whoever remains is expelled with 75% max health in damage (never lethal) and Magicule Poisoning III. A collapse outside any Nexus is a pocket (no terrain removed). Admin: `dungeon collapse start | stop [all] | status`.

### 8.7 Known limits (1.21.0)
- Never run in game. The Tensura mobs are driven from outside their brains; a brain may undo an order between two passes.
- Zone ways in are cut from nearby caves; a core cell with no cave near it may stay closed until one is brought close.
- `DungeonTier.CORE_TOP` (-26): everything below it inside a core cell is the core tier.
- Release and re-attune of a relic resets its curse clock; the Mantle's physical-blow curse and the Sheath's slash interact (the Mantle cancels first).

## 9. The Sentient Dungeon (1.22.0)

Switch `dungeon.sentient` (server config, default on). Off: the 1.21.0 ecosystem returns unchanged.

- **Rooms and births.** A room is a 16-block cell of the floor; the first time a player enters it, it is rolled (70 percent Catacombs, 60 percent Waterways and Underworld); a populated room cracks its walls (block-break sounds, dust, ash) and births 3 to 8 creatures from the floor's roster, never more than 8 alive in a room; empty and cleared rooms refill after `roomRebirthMinutes`. Born creatures hunt the player who woke them and let go when alone.
- **Floors.** A Upper Catacombs (Goblins, slimes, hound dogs as Kobolds; boss Goliath, 1.23.0), B Deep Waterways (lizardmen, aqua slimes, water spirits; boss Charybdis Spawn, 1.23.0), C Underworld Gate (lesser devils, Dark Disciples; boss Devil Avatar), D Monarch's Vault (boss only: the Blood-Red Commander).
- **Immune system.** Threat per player: refused wall blow 4, refused blast block 1, massive spell (cost c percent) c*c/10; decays 1.5 a second; warnings at 50 and 80 percent; at the limit a Juggernaut is born near the player; the Amulet masks spells; the Wild Magic Surge aggroes it on purpose.
- **Ash and stones.** Death turns the creature to ash; stones by tier (floor bosses and Juggernauts more); a kill by anything but a player leaves a stone one time in four; the 12 materials drop 4 percent.
- **Relics.** Two `dungeon_relic` Curios slots (data in `data/nusmp/curios`); code cap of two; Use Relic key R; attunement needs 10 s of sneaking and standing still (0.25 block drift) and is reset by damage.
- **The Blood-Red Commander.** Lunge (a marked lane, 18-tick wind-up), Cross (20), Executioner's Arc (two cuts); summons 3 then 4 soldiers at 50 and 25 percent and refills; caps 4/6/8.
- **The Devil Avatar.** 900 health, speed 0.34; Ceiling Pull, Floor Crush, Inversion Field, Hex Volley, Dread Brand, Curse Nova, Shadow Step, Call the Disciples; Abyssal Collapse at 60 percent power; Gates form at 25 percent.
- **Collapse v2.** Six waves of a frenzied horde born from the walls (24/6/56 caps); Spatial Surge every 12-20 s per player, never lethal.
- **Known limits.** Nothing was run in game; Juggernauts that unload with their chunk stop counting against the cap of three; old halls need `nexus reset` for the Avatar.

## 10. The floor arenas (1.23.0)

Arena cells (`DungeonTier.arenaOf`, about 8 percent of non-core cells of the Upper Catacombs and the Deep Waterways) are built by the same Nexus builder as the core zones: Goliath's Pit (`dungeon/floor/goliath`) on Floor A, the Charybdis Basin (`dungeon/floor/charybdis`) on Floor B. Each has gates, a mana-gated Treasury with relic pedestals (the Sentient pool), and a boss on `ZogratisBoss` with the framework's Percentage Power System and Gates form. The fight posts a `DungeonCollapseEvent` once. Charybdis steers by velocity (no gravity, not pushed by fluid); its scales are deflected by shield, Magebane parry or a melee strike (`ParryRules`).
