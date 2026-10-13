package com.newuniverse.nusmp.dungeon;

import com.newuniverse.nusmp.dungeon.nexus.NexusText;
import com.newuniverse.nusmp.dungeon.nexus.NexusZones;
import com.newuniverse.nusmp.dungeon.sentient.SentientDeath;
import com.newuniverse.nusmp.dungeon.sentient.SentientMode;
import com.newuniverse.nusmp.dungeon.wild.WildRules;
import com.newuniverse.nusmp.dungeon.wild.WildSpawn;
import com.newuniverse.nusmp.dungeon.wild.Wildlife;
import com.newuniverse.nusmp.mana.ManaLaw;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Iterator;
import java.util.Set;

/**
 * The Living Dungeon's rules while it runs (docs/living_dungeon.md): nothing is spawned by the world; each chunk keeps its own ecosystem
 * (oxygen, magicule, biomass) and ticks it near players; the tiers' hazards act on whoever stands in them; mined terrain grows back;
 * the Golden City's seal breaks once.
 */
public final class DungeonEcology {
    private DungeonEcology() {}

    // ---------------------------------------------------------------- the world spawns nothing here
    public static void onFinalizeSpawn(FinalizeSpawnEvent e) {
        if (!LivingDungeon.in(e.getLevel().getLevel())) return;
        MobSpawnType t = e.getSpawnType();
        if (t == MobSpawnType.NATURAL || t == MobSpawnType.CHUNK_GENERATION || t == MobSpawnType.STRUCTURE || t == MobSpawnType.PATROL || t == MobSpawnType.REINFORCEMENT)
            e.setSpawnCancelled(true);
    }

    // ---------------------------------------------------------------- the server tick: ecosystems, repairs, Maws
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        int tick = server.getTickCount();
        if (!DungeonConfig.get(DungeonConfig.ENABLED)) return;
        if (tick % 20 == 11) DungeonMaw.tick(server);
        ServerLevel dungeon = server.getLevel(LivingDungeon.DIMENSION);
        if (dungeon == null) return;
        if (tick % 100 == 17) simulate(dungeon);
        if (tick % 200 == 53) regrow(server, dungeon);
    }

    /** Ticks the ecosystem of every loaded chunk near a player of the Dungeon. */
    private static void simulate(ServerLevel d) {
        int r = DungeonConfig.get(DungeonConfig.SIM_RADIUS);
        Set<Long> done = new HashSet<>();
        for (ServerPlayer p : d.players()) {
            ChunkPos cp = p.chunkPosition();
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    long key = ChunkPos.asLong(cp.x + dx, cp.z + dz);
                    if (!done.add(key)) continue;
                    LevelChunk c = d.getChunkSource().getChunkNow(cp.x + dx, cp.z + dz);
                    if (c != null) ecoTick(d, c);
                }
            }
        }
    }

    private static void ecoTick(ServerLevel d, LevelChunk c) {
        EcosystemData e = c.getData(LivingDungeon.ECOSYSTEM);
        ChunkPos cp = c.getPos();
        float ceiling = DungeonTier.at(cp.getMiddleBlockX(), 0, cp.getMiddleBlockZ()).oxygenCeiling;
        if (e.oxygen < ceiling) e.oxygen += (ceiling - e.oxygen) * 0.10f + 0.5f;                  // the air comes back
        else if (e.oxygen > ceiling) e.oxygen = Math.max(ceiling, e.oxygen - 2f);
        e.magicule += 4f - e.biomass * 0.02f;                                                      // the roots feed it, life eats it
        e.clamp();
        if (DungeonConfig.get(DungeonConfig.LIFE) && !SentientMode.active()) population(d, c, e);           // the Sentient Dungeon breeds nothing: its mobs are born from the walls of the rooms players enter
        e.clamp();
        c.setUnsaved(true);
    }

    // ---------------------------------------------------------------- the food web: births gated by what is below, collapse
    /**
     * 1.19.0: the creatures are the real mobs of Tensura (and Ice and Fire), chosen from the roster tags by {@link WildSpawn}; the tiers are
     * counted with {@link Wildlife#tierOf}, so the surviving 1.18.0 creatures still count. The gates are those of {@link WildRules}.
     */
    private static void population(ServerLevel d, LevelChunk c, EcosystemData e) {
        if (!NexusZones.lifeAllowed(d, c.getPos()) || com.newuniverse.nusmp.dungeon.nexus.collapse.CollapseManager.suppressesLife(d, c.getPos())) return;                                        // 1.21.0: no breeding inside a Nexus that does not allow life
        ChunkPos cp = c.getPos();
        AABB box = new AABB(cp.getMinBlockX(), d.getMinBuildHeight(), cp.getMinBlockZ(), cp.getMaxBlockX() + 1, d.getMaxBuildHeight(), cp.getMaxBlockZ() + 1);
        List<Mob> life = d.getEntitiesOfClass(Mob.class, box, m -> Wildlife.tierOf(m) > 0);
        int n1 = 0, n2 = 0, n3 = 0;
        float mass = 0;
        for (Mob m : life) {
            switch (Wildlife.tierOf(m)) { case 1 -> n1++; case 2 -> n2++; default -> n3++; }
            mass += Wildlife.massOf(m);
        }
        e.biomass = Math.min(EcosystemData.BIOMASS_MAX, mass);
        RandomSource rnd = d.getRandom();
        // slimes are born of dense magicules, as many as the air can feed; each birth costs some
        if (WildRules.slimeBirth(e.magicule, n1, rnd.nextFloat()) && WildSpawn.birth(d, c, 1)) { e.magicule -= 40f; n1++; }
        // armoured things come where slimes are plentiful and the air is fit to breathe
        if (WildRules.midBirth(n1, n2, e.oxygen, rnd.nextFloat()) && WildSpawn.birth(d, c, 2)) n2++;
        // and the predators come where there is something worth hunting
        if (WildRules.apexBirth(n2, n3, e.oxygen, rnd.nextFloat()) && WildSpawn.birth(d, c, 3)) n3++;
        // a dragon-class creature only where the chunk is thick with life, and very rarely
        if (WildRules.legendBirth(e.biomass, n3, e.oxygen, rnd.nextFloat()) && WildSpawn.birthLegend(d, c)) n3++;
        int total = n1 + n2 + n3;
        e.peak = Math.max(e.peak, total);
        if (total == 0 && e.peak >= 6) {                                                           // everything that lived here is gone
            e.peak = 0;
            DungeonMaw.broadcast(d.getServer(), Component.literal(MawText.famine(DungeonTier.at(cp.getMiddleBlockX(), 0, cp.getMiddleBlockZ()).label))      // 1.21.0: the owner's wording, the tier the chunk's ecosystem runs under
                    .withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC));
        }
    }

    /** Brings one creature into a chunk at a floor out of sight of the players. */
    public static boolean spawn(ServerLevel d, LevelChunk c, EntityType<? extends DungeonCreature> type) {
        RandomSource r = d.getRandom();
        for (int t = 0; t < 4; t++) {
            int x = c.getPos().getMinBlockX() + r.nextInt(16), z = c.getPos().getMinBlockZ() + r.nextInt(16);
            BlockPos p = LivingDungeon.column(d, x, z);
            if (p == null || d.getNearestPlayer(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 6, false) != null) continue;
            DungeonCreature m = type.create(d);
            if (m == null) return false;
            m.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, r.nextFloat() * 360f, 0f);
            m.finalizeSpawn(d, d.getCurrentDifficultyAt(p), MobSpawnType.EVENT, null);
            d.addFreshEntity(m);
            return true;
        }
        return false;
    }

    // ---------------------------------------------------------------- nothing drops loot here; what dies leaves a carcass
    public static void onDrops(LivingDropsEvent e) {
        LivingEntity v = e.getEntity();
        if (!(v.level() instanceof ServerLevel sl) || !LivingDungeon.in(sl) || v instanceof Player || !DungeonConfig.get(DungeonConfig.NO_LOOT)) return;
        if (SentientMode.active()) {                                                                  // the Sentient Dungeon: no carcass; what dies turns to ash and leaves Magic Stones
            SentientDeath.onDrops(e, sl, v);
            return;
        }
        e.getDrops().clear();
        if (v instanceof Mob && !(v instanceof DungeonCreature)) CarcassBlock.leave(sl, v.blockPosition(), Wildlife.carcassKind(v), Wildlife.carcassWeight(v));   // 1.19.0: the matching kind (Tensura slimes, Armorsaurus, Basilisk, Direwolf), the generic one for the rest
    }

    // ---------------------------------------------------------------- mined terrain grows back
    public static void onBreak(BlockEvent.BreakEvent e) {
        if (!(e.getLevel() instanceof ServerLevel sl) || !LivingDungeon.in(sl) || !DungeonConfig.get(DungeonConfig.ENABLED)) return;
        BlockState st = e.getState();
        if (!(st.is(BlockTags.BASE_STONE_OVERWORLD) || st.is(Tags.Blocks.ORES) || st.is(Tags.Blocks.STONES) || st.is(Blocks.DRIPSTONE_BLOCK) || st.is(Blocks.POINTED_DRIPSTONE))) return;
        DungeonSave s = DungeonSave.get(sl.getServer());
        long due = sl.getGameTime() + Math.round(DungeonConfig.get(DungeonConfig.REGROW_DAYS) * 24000.0);
        s.repairs.add(new DungeonSave.Repair(e.getPos().asLong(), NbtUtils.writeBlockState(st), due));
        s.setDirty();
        BlockPos pos = e.getPos().immutable();
        sl.getServer().execute(() -> {                                                             // a veil grows in the hole once the block is gone
            if (sl.getBlockState(pos).isAir()) sl.setBlock(pos, DungeonBlocks.DUNGEON_MYCELIUM.get().defaultBlockState(), 3);
        });
    }

    /** Puts back what is due, never while a player is near. */
    private static void regrow(MinecraftServer server, ServerLevel d) {
        DungeonSave s = DungeonSave.get(server);
        if (s.repairs.isEmpty()) return;
        long now = d.getGameTime();
        boolean changed = false;
        int budget = 400;                                                                           // at most this many blocks a pass
        for (Iterator<DungeonSave.Repair> it = s.repairs.iterator(); it.hasNext() && budget > 0; ) {
            DungeonSave.Repair r = it.next();
            if (now < r.due()) continue;
            BlockPos pos = BlockPos.of(r.pos());
            if (!d.hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) continue;
            if (d.getNearestPlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 6, false) != null) continue;
            BlockState back = DungeonSave.readState(d.registryAccess(), r.state());
            BlockState cur = d.getBlockState(pos);
            if (back != null && (cur.isAir() || cur.canBeReplaced() || cur.is(DungeonBlocks.DUNGEON_MYCELIUM.get()))) {
                d.setBlock(pos, back, 3);
                budget--;
            }
            it.remove();
            changed = true;
        }
        if (changed) s.setDirty();
    }

    // ---------------------------------------------------------------- hazards of the tiers
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.tickCount % 20 != 3 || !LivingDungeon.in(p) || p.isSpectator()) return;
        ServerLevel level = p.serverLevel();
        DungeonTier tier = DungeonTier.at(level, p.blockPosition());
        if (tier == DungeonTier.GOLDEN_CITY) {
            DungeonSave s = DungeonSave.get(level.getServer());
            if (!s.cityBreached) {
                s.cityBreached = true;
                s.setDirty();
                DungeonMaw.broadcast(level.getServer(), Component.literal(NexusText.sealBreached(p.getName().getString(), NexusZones.zoneAt(level, p.getBlockX(), p.getBlockZ()).label))
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));         // 1.21.0: the owner's wording; the Nexus names the zone the player walks
            }
        }
        if (!DungeonConfig.get(DungeonConfig.HAZARDS) || p.isCreative()) return;
        CompoundTag d = p.getPersistentData();
        int rad = d.getInt("nusmp_dungeon_rad");
        switch (tier) {
            case VOLCANIC_VENTS -> {
                if (!p.fireImmune() && !p.hasEffect(MobEffects.FIRE_RESISTANCE) && !p.isInWaterOrBubble()) {
                    p.hurt(level.damageSources().hotFloor(), 1.0f);
                    level.sendParticles(ParticleTypes.SMOKE, p.getX(), p.getY() + 0.3, p.getZ(), 4, 0.3, 0.1, 0.3, 0.01);
                }
            }
            case GOLDEN_CITY -> {
                int shift = RadiationShield.step(p);                                               // a Mana Zone or a Golden Ward shuts it out, Magic Sense dulls it
                rad = shift < 0 ? Math.max(0, rad - 1) : rad + shift;
            }
            default -> { }
        }
        if (tier != DungeonTier.GOLDEN_CITY) rad = Math.max(0, rad - 2);
        d.putInt("nusmp_dungeon_rad", rad);
        if (rad >= 10) p.addEffect(new MobEffectInstance(DungeonEffects.MAGICULE_POISONING, 400, Math.min(4, rad / 25), true, true));
        EcosystemData eco = LivingDungeon.eco(level, p.blockPosition());
        if (eco != null && eco.thinAir()) p.addEffect(new MobEffectInstance(DungeonEffects.SUFFOCATION, 80, eco.oxygen <= 15f ? 1 : 0, true, true));
    }

    // ---------------------------------------------------------------- water conducts; fire eats air
    private static boolean chaining;

    private static boolean electric(DamageSource src) {
        if (src.is(DamageTypeTags.IS_LIGHTNING)) return true;
        var key = src.typeHolder().unwrapKey();
        if (key.isEmpty()) return false;
        String path = key.get().location().getPath();
        return path.contains("lightning") || path.contains("thunder") || path.contains("electr") || path.contains("shock");
    }

    public static void onIncomingDamage(LivingIncomingDamageEvent e) {
        LivingEntity v = e.getEntity();
        if (!(v.level() instanceof ServerLevel sl) || !LivingDungeon.in(sl) || e.getAmount() <= 0 || !DungeonConfig.get(DungeonConfig.HAZARDS)) return;
        DamageSource src = e.getSource();
        if (!chaining && v.isInWaterOrBubble() && electric(src)) {                                  // an electric hit on someone in water jumps to every swimmer near
            chaining = true;
            try {
                float jump = Math.min(8f, e.getAmount() * 0.5f);
                for (LivingEntity o : sl.getEntitiesOfClass(LivingEntity.class, v.getBoundingBox().inflate(6), o -> o != v && o.isAlive() && o.isInWaterOrBubble())) {
                    o.hurt(sl.damageSources().lightningBolt(), jump);
                    o.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 2));
                    sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, o.getX(), o.getY() + 1, o.getZ(), 12, 0.4, 0.5, 0.4, 0.2);
                }
            } finally { chaining = false; }
        }
        if (src.is(DamageTypeTags.IS_FIRE) || src.is(DamageTypeTags.IS_EXPLOSION)) burnAir(sl, v.blockPosition(), e.getAmount() * 1.2f);
    }

    /** Fire consumes the air of a chunk. */
    public static void burnAir(ServerLevel level, BlockPos pos, float amount) {
        EcosystemData eco = LivingDungeon.eco(level, pos);
        if (eco == null) return;
        eco.oxygen -= amount;
        eco.clamp();
        LivingDungeon.touch(level, pos);
    }
}
