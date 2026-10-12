package com.newuniverse.nusmp.dungeon;

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
import net.minecraft.world.entity.LivingEntity;
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
                    if (c != null) ecoTick(c);
                }
            }
        }
    }

    private static void ecoTick(LevelChunk c) {
        EcosystemData e = c.getData(LivingDungeon.ECOSYSTEM);
        ChunkPos cp = c.getPos();
        float ceiling = DungeonTier.at(cp.getMiddleBlockX(), 0, cp.getMiddleBlockZ()).oxygenCeiling;
        if (e.oxygen < ceiling) e.oxygen += (ceiling - e.oxygen) * 0.10f + 0.5f;                  // the air comes back
        else if (e.oxygen > ceiling) e.oxygen = Math.max(ceiling, e.oxygen - 2f);
        e.magicule += 4f - e.biomass * 0.02f;                                                      // the roots feed it, life eats it
        e.clamp();
        c.setUnsaved(true);
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
                DungeonMaw.broadcast(level.getServer(), Component.literal("Something very old has stopped sleeping: the seal of the Golden City is broken. "
                        + p.getName().getString() + " walks its streets.").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
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
                if (!ManaLaw.zoneActive(p)) rad += 1;                                              // a Mana Zone keeps the radiation out
                else rad = Math.max(0, rad - 1);
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
