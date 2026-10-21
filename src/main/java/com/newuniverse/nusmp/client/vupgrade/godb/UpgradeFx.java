package com.newuniverse.nusmp.client.vupgrade.godb;

import com.newuniverse.nusmp.client.relics.fx.RelicFxOptions;
import com.newuniverse.nusmp.client.upgrade.UpgradeGate;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.List;

/**
 * The ambient particle layers of the upgraded God melee weapons (release 1.102.0, slice god_melee_b): seven layers per weapon while it is held, each one a different sign of the same
 * weapon: idle motes drifting off the blade, a ring of light turning round the head, sparks running along the blade (the energy line), a pool of light at the feet, a flare at the
 * hand, a light shaft rising off the tip, and a rare flare. Which particle, which colour and how dense is the weapon's own table ({@link UpgradeSpec#layers()}). (The eighth layer, the
 * orbiting pieces, and the vein / halo passes are drawn by {@code client/GodMeleeBRender}, not here.)
 * <p>
 * Cost and budget: one pass per client tick over the players, at most {@link #MAX_HOLDERS} holders, none beyond the table's particle distance (32 blocks) from the camera, half the rate
 * beyond 12 blocks, scaled by the effect quality ({@code vfx.vfxQuality}), switched off by {@code visual.particles} and {@code relicfx.particles}, and every spawn asks the client
 * particle budget ({@link PerfBudgets#clientParticles}) first. Nothing is allocated per tick but what the particle engine takes.
 */
public final class UpgradeFx {
    private UpgradeFx() {}

    static final int MAX_HOLDERS = 8;

    public static void onClientTick(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.player == null || mc.isPaused() || !UpgradeConfig.upgrade() || !UpgradeConfig.particles() || !RelicFxOptions.particles()) return;
        Camera cam = mc.gameRenderer.getMainCamera();
        double cx = cam.getPosition().x, cy = cam.getPosition().y, cz = cam.getPosition().z;
        long tick = level.getGameTime();
        if (!UpgradeGate.begin(mc, tick)) return;                                    // 1.110.0: paused, low frame rate, density 0: nothing
        RandomSource rnd = level.random;
        int holders = 0;
        List<AbstractClientPlayer> players = level.players();
        for (int i = 0; i < players.size() && holders < MAX_HOLDERS; i++) {
            AbstractClientPlayer p = players.get(i);
            if (p.isSpectator() || p.isInvisible()) continue;
            ItemStack held = p.getMainHandItem();
            if (held.isEmpty()) continue;
            UpgradeSpecs.Entry entry = UpgradeSpecs.get(BuiltInRegistries.ITEM.getKey(held.getItem()).getPath());
            if (entry == null) continue;
            double dx = p.getX() - cx, dy = p.getY() - cy, dz = p.getZ() - cz;
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            float factor = UpgradeGate.scale(p) * UpgradeMath.distanceFactor(dist, entry.spec.lodNearBlocks(), entry.spec.particleBlocks());   // density x quality x fps x distance x view
            if (factor <= 0f) continue;
            holders++;
            spawn(level, rnd, p, entry, tick, factor, BuiltInRegistries.ITEM.getId(held.getItem()));
        }
    }

    /** One tick of the seven layers of one holder. */
    private static void spawn(ClientLevel level, RandomSource rnd, AbstractClientPlayer p, UpgradeSpecs.Entry entry, long tick, float factor, int itemKey) {
        UpgradeSpec s = entry.spec;
        double yaw = Math.toRadians(p.yBodyRot);
        double fx = -Math.sin(yaw), fz = Math.cos(yaw);                              // forward
        double rx = -Math.cos(yaw), rz = -Math.sin(yaw);                              // the holder's right-hand side
        double crouch = p.isCrouching() ? 0.85 : 1.0;
        double hx = p.getX() + rx * 0.40 + fx * 0.38, hy = p.getY() + 1.05 * crouch, hz = p.getZ() + rz * 0.40 + fz * 0.38;      // the hand
        double len = s.lengthBlocks();
        double dxs = fx * 0.50, dys = 0.866, dzs = fz * 0.50;                        // the weapon axis: forward and up
        // second axis of the ring round the weapon: the hand-side vector, the third: axis x side
        double ax = dxs, ay = dys, az = dzs;
        double vx = ay * rz - az * 0.0, vy = az * rx - ax * rz, vz = ax * 0.0 - ay * rx;
        double vl = Math.max(1e-6, Math.sqrt(vx * vx + vy * vy + vz * vz));
        vx /= vl;
        vy /= vl;
        vz /= vl;
        for (int li = 0; li < s.layers().size(); li++) {
            UpgradeSpec.Layer l = s.layers().get(li);
            int n = UpgradeMath.stochasticCount(l.rate() * factor, rnd.nextFloat());
            if (n <= 0) continue;
            n = UpgradeGate.take(p, itemKey, n);                                    // the item bucket, the player bucket, the live cap, the client allowance
            if (n <= 0) continue;
            for (int k = 0; k < n; k++) {
                double x, y, z, mx = 0, my = 0, mz = 0;
                switch (l.kind()) {
                    case "motes" -> {
                        double f = 0.30 + 0.70 * rnd.nextDouble();
                        x = hx + ax * len * f + (rnd.nextDouble() - 0.5) * 0.30;
                        y = hy + ay * len * f + (rnd.nextDouble() - 0.5) * 0.30;
                        z = hz + az * len * f + (rnd.nextDouble() - 0.5) * 0.30;
                        my = l.speed() * (0.5 + rnd.nextDouble());
                    }
                    case "halo_ring" -> {
                        double a = tick * 0.31 + k * 2.4;
                        double c = Math.cos(a) * 0.38, sn = Math.sin(a) * 0.38;
                        x = hx + ax * len * 0.86 + rx * c + vx * sn;
                        y = hy + ay * len * 0.86 + vy * sn;
                        z = hz + az * len * 0.86 + rz * c + vz * sn;
                    }
                    case "edge_line" -> {
                        double f = ((tick * 0.045) + k * 0.5) % 1.0;
                        x = hx + ax * len * f + (rnd.nextDouble() - 0.5) * 0.06;
                        y = hy + ay * len * f + (rnd.nextDouble() - 0.5) * 0.06;
                        z = hz + az * len * f + (rnd.nextDouble() - 0.5) * 0.06;
                        mx = (rnd.nextDouble() - 0.5) * l.speed();
                        mz = (rnd.nextDouble() - 0.5) * l.speed();
                    }
                    case "ground_cast" -> {
                        double a = tick * 0.23 + k * 3.1;
                        double r = s.groundRadius() * (0.7 + 0.3 * rnd.nextDouble());
                        x = p.getX() + Math.cos(a) * r;
                        y = p.getY() + 0.06;
                        z = p.getZ() + Math.sin(a) * r;
                        my = 0.004;
                    }
                    case "wrist_flare" -> {
                        x = hx + (rnd.nextDouble() - 0.5) * 0.14;
                        y = hy + (rnd.nextDouble() - 0.5) * 0.14;
                        z = hz + (rnd.nextDouble() - 0.5) * 0.14;
                        my = l.speed() * rnd.nextDouble();
                    }
                    case "light_shaft" -> {
                        x = hx + ax * len + (rnd.nextDouble() - 0.5) * 0.12;
                        y = hy + ay * len + rnd.nextDouble() * s.shaftHeight() * 0.25;
                        z = hz + az * len + (rnd.nextDouble() - 0.5) * 0.12;
                        my = l.speed() * (0.6 + 0.8 * rnd.nextDouble());
                    }
                    default -> {                                                     // accent_flare: a rare burst at the tip
                        x = hx + ax * len + (rnd.nextDouble() - 0.5) * 0.3;
                        y = hy + ay * len + (rnd.nextDouble() - 0.5) * 0.3;
                        z = hz + az * len + (rnd.nextDouble() - 0.5) * 0.3;
                        mx = (rnd.nextDouble() - 0.5) * l.speed() * 2.0;
                        my = (rnd.nextDouble() - 0.2) * l.speed() * 2.0;
                        mz = (rnd.nextDouble() - 0.5) * l.speed() * 2.0;
                    }
                }
                UpgradeGate.add(level, entry.particles[li], x, y, z, mx, my, mz);
            }
        }
    }
}
