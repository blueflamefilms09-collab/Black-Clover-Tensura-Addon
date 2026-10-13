package com.newuniverse.nusmp.dungeon.nexus.zone;

import com.newuniverse.nusmp.dungeon.EcosystemData;
import com.newuniverse.nusmp.dungeon.LivingDungeon;
import com.newuniverse.nusmp.dungeon.nexus.ZonePlan;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.List;

/**
 * The mood of the Tempest Mirage: a too cheerful village. Heat haze and drifting petals round whoever is in it, shimmer in the air, and every
 * so often a chime or two a hair out of tune, a celebration that is not quite right. And the richness of its ground: its chunks' ecosystem
 * (biomass and magicule) is pushed up when it is built and kept up while players are in it, which is what the Anti-Bird Compass reads.
 */
final class TempestAmbience {
    private TempestAmbience() {}

    /** About once a second while players are in the village: haze, petals and shimmer round each, now and then a chime. */
    static void tick(ServerLevel level, List<ServerPlayer> inside) {
        RandomSource r = level.getRandom();
        for (ServerPlayer p : inside) {
            level.sendParticles(ParticleTypes.CHERRY_LEAVES, p.getX(), p.getY() + 7.0, p.getZ(), 4, 8.0, 1.5, 8.0, 0.02);
            level.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, p.getX(), p.getY() + 2.0, p.getZ(), 6, 9.0, 2.5, 9.0, 0.0);
            level.sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 1.5, p.getZ(), 2, 8.0, 2.0, 8.0, 0.0);
        }
        if (inside.isEmpty() || r.nextInt(8) != 0) return;
        ServerPlayer who = inside.get(r.nextInt(inside.size()));
        BlockPos at = who.blockPosition().offset(r.nextInt(17) - 8, 2, r.nextInt(17) - 8);
        float base = 0.9f + r.nextFloat() * 0.5f;
        level.playSound(null, at, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.AMBIENT, 0.9f, base);
        level.playSound(null, at, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.AMBIENT, 0.7f, base * 1.06f);              // a hair sharp: it is not quite right
        if (r.nextInt(4) == 0) level.playSound(null, at, SoundEvents.VILLAGER_CELEBRATE, SoundSource.AMBIENT, 0.5f, 0.55f + r.nextFloat() * 0.15f);
    }

    /** Pushes the ecosystem of every chunk the village covers up to the village's levels (never down), so its biomass and magicule stay high. */
    static void pushLife(ServerLevel level, ZonePlan plan) {
        for (int[] ch : plan.footprint.chunks()) {
            LevelChunk chunk = level.getChunkSource().getChunkNow(ch[0], ch[1]);
            if (chunk == null) continue;
            EcosystemData e = chunk.getData(LivingDungeon.ECOSYSTEM);
            boolean changed = false;
            if (e.biomass < TempestRules.BIOMASS) {
                e.biomass = TempestRules.BIOMASS;
                changed = true;
            }
            if (e.magicule < TempestRules.MAGICULE) {
                e.magicule = TempestRules.MAGICULE;
                changed = true;
            }
            if (changed) {
                e.clamp();
                chunk.setUnsaved(true);
            }
        }
    }
}
