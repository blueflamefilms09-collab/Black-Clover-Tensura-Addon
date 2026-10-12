# The Living Dungeon (Tensura x Black Clover x Delicious in Dungeon)

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
| later | Polish | models for the creatures, ambience, a Golden City structure, rewards for closing a Maw |

---------------------------------------------------------------------------------------------------------------------------

## 7. What exists now (1.18.0) and what is not done

Done: everything in sections 1 to 5 except the items below. Never run in game; CI compiles it, a headless preview checked the two geo models.

Not done yet:
- the Golden City itself (a structure with its own loot-free rewards): the tier and its radiation exist, nothing is built there;
- Magic Sense (Tensura's skill) as protection against magicule radiation: only a Mana Zone protects;
- temporary skills or secondary Black Clover attributes from cooked meals (only timed spell power and cost changes);
- predators' den structures and pack behaviour; a Maw reward when its roots are cut; the Dungeon's own sky/fog colours and ambient sounds;
- the Living Armor is drawn with the game's humanoid model: a bespoke model would suit it better.
