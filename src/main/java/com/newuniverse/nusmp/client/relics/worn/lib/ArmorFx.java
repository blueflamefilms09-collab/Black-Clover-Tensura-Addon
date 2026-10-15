package com.newuniverse.nusmp.client.relics.worn.lib;

import com.newuniverse.nusmp.client.particle.rukh.RukhClientFx;
import com.newuniverse.nusmp.particle.rukh.RukhFx.Alignment;
import com.newuniverse.nusmp.client.relics.fx.FxQuality;
import com.newuniverse.nusmp.client.relics.fx.FxTheme;
import com.newuniverse.nusmp.client.relics.fx.RelicFxOptions;
import com.newuniverse.nusmp.client.relics.fx.WornFxPlan;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import org.joml.Vector3f;

/**
 * The ambient particles of the mod's armour items (the robe sets ...): embers off a Crimson Lion mantle, petals off the Heart uniform, sparks off a Black Bull robe, themed by
 * the catalog ({@code fx} lines) or by the glow colour like the relics', at the rate of the piece's grade tier and the quality option, from the part of the body the piece is
 * on. Called once per wearer by {@code RelicFxClient}, inside the same per-tick budget. Client thread only.
 */
public final class ArmorFx {
    private ArmorFx() {}

    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    /** Spawns this tick's particles of the armour {@code p} wears; returns how many (never more than {@code room}). */
    public static int tick(ClientLevel level, AbstractClientPlayer p, FxQuality q, long tick, int room) {
        if (room <= 0 || !RelicFxOptions.models()) return 0;
        int spawned = 0;
        for (int i = 0; i < SLOTS.length && spawned < room; i++) {
            WornLibrary.Ensemble e = ArmorItems.of(p.getItemBySlot(SLOTS[i]), SLOTS[i]);
            if (e == null || e.theme == FxTheme.NONE) continue;
            WornFxPlan plan = WornFxPlan.of(e.tier, q, 0.0);
            if (plan.particleEvery() <= 0 || (tick + p.getId() * 7L + i * 5L) % plan.particleEvery() != 0) continue;
            for (int k = 0; k < plan.particlesPerEvent() && spawned < room; k++) {
                spawn(level, p, i, e, level.random);
                spawned++;
            }
        }
        return spawned;
    }

    /** Where on the body each slot sheds: height range in blocks above the feet. */
    private static void spawn(ClientLevel level, AbstractClientPlayer p, int slot, WornLibrary.Ensemble e, RandomSource rnd) {
        double y0, y1, spread = 0.32;
        switch (slot) {
            case 0 -> { y0 = 1.65; y1 = 2.05; spread = 0.30; }
            case 1 -> { y0 = 1.0; y1 = 1.5; }
            case 2 -> { y0 = 0.5; y1 = 0.9; }
            default -> { y0 = 0.02; y1 = 0.3; spread = 0.25; }
        }
        double x = p.getX() + (rnd.nextDouble() - 0.5) * 2 * spread, z = p.getZ() + (rnd.nextDouble() - 0.5) * 2 * spread;
        double y = p.getY() + y0 + rnd.nextDouble() * (y1 - y0);
        double vx = (rnd.nextDouble() - 0.5) * 0.01, vz = (rnd.nextDouble() - 0.5) * 0.01;
        int glow = e.colors[WornMesh.GLOW], main = e.colors[WornMesh.MAIN];
        switch (e.theme) {
            case EMBER -> RukhClientFx.spray(level, Alignment.LIGHT, x, y, z, 1, vx, 0.012, vz, 1.0);
            case SNOW -> RukhClientFx.spray(level, Alignment.LIGHT, x, y, z, 1, vx, -0.004, vz, 1.0);
            case SPARK -> RukhClientFx.spray(level, Alignment.LIGHT, x, y, z, 1, vx * 3, 0.0, vz * 3, 1.0);
            case LEAF -> RukhClientFx.spray(level, Alignment.LIGHT, x, y, z, 1, vx, -0.01, vz, 1.0);
            case WISP -> RukhClientFx.spray(level, Alignment.DARK, x, y, z, 1, vx, 0.02, vz, 1.0);
            case STAR -> RukhClientFx.spray(level, Alignment.LIGHT, x, y, z, 1, vx, 0.01, vz, 1.0);
            case BEE -> RukhClientFx.spray(level, Alignment.LIGHT, x, y, z, 1, vx * 4, 0.0, vz * 4, 1.0);
            case PETAL -> RukhClientFx.spray(level, Alignment.LIGHT, x, y, z, 1, vx, -0.01, vz, 1.0);
            default -> { }
        }
    }

}
