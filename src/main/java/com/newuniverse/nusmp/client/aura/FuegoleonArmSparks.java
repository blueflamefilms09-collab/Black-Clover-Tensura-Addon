package com.newuniverse.nusmp.client.aura;

import com.newuniverse.nusmp.aura.Aura;
import com.newuniverse.nusmp.book.FuegoleonArmDjinnRules;
import com.newuniverse.nusmp.book.FuegoleonArmDjinnRules.State;
import com.newuniverse.nusmp.book.FuegoleonArmRules;
import com.newuniverse.nusmp.particle.fx.Fx;
import com.newuniverse.nusmp.particle.fx.FxSlot;
import com.newuniverse.nusmp.particle.fx.ThemeKey;
import com.newuniverse.nusmp.perf.PerfProbe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.List;

/**
 * Fuegoleon's flame arm, Djinn look (3.1): the sparks drifting off the arm into the world, the custom particles of the theme {@code fuegoleon_arm.sparks} (data/nusmp/particle_themes.json, written
 * by tools/particles/gen_themes.py), spawned on the CLIENT from the lease the server already sent (no packet of its own). Bounded three times over: only arms within
 * {@link FuegoleonArmDjinnRules#SPARK_RANGE} blocks, at most {@link FuegoleonArmDjinnRules#SPARK_ARMS} arms per tick, one particle every 10 / 5 / 2 ticks per arm (quiet / out / flare, staggered by
 * entity id), and every particle goes through {@code Fx} (the client particle budget, the effect quality and the distance bands). Only for the DJINN look and only while the arm is drawn
 * ({@link FuegoleonArmAura#drawsArm}); the viewer's particle switch {@code spiritDive.particles} applies. No allocation but the particles.
 */
public final class FuegoleonArmSparks {
    private FuegoleonArmSparks() {}

    private static final ThemeKey SPARKS = ThemeKey.of("fuegoleon_arm.sparks");
    private static final PerfProbe.Section SECTION = PerfProbe.section("c.fuegoleon.arm.sparks");    // timed (/multiverse perf)

    /** Game bus, client: once per tick. */
    public static void onClientTick(ClientTickEvent.Post e) {
        if (!SpiritDiveLook.particles() || !FuegoleonArmDjinnRules.drawsLimb(FuegoleonArmLook.get())) return;
        long t0 = PerfProbe.begin();
        try {
            tick();
        } finally {
            PerfProbe.end(SECTION, t0);
        }
    }

    private static void tick() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.player == null || mc.isPaused()) return;
        List<AbstractClientPlayer> players = level.players();
        if (players.isEmpty()) return;
        long now = level.getGameTime();
        double range2 = FuegoleonArmDjinnRules.SPARK_RANGE * FuegoleonArmDjinnRules.SPARK_RANGE;
        int served = 0;
        for (int i = 0, n = players.size(); i < n && served < FuegoleonArmDjinnRules.SPARK_ARMS; i++) {
            AbstractClientPlayer p = players.get(i);
            if (p.distanceToSqr(mc.player) > range2 || !FuegoleonArmAura.drawsArm(p)) continue;
            served++;
            long[] w = PlayerAuraClient.running(p.getId()).get(Aura.FUEGOLEON_ARM);
            State st = FuegoleonArmDjinnRules.state((int) w[2] == FuegoleonArmRules.STYLE_LINKED, DiveCast.casting(p.getId()), PlayerAuraClient.has(p.getId(), Aura.SPIRIT_DIVE));
            if (FuegoleonArmDjinnRules.visible(SpiritDiveLook.armView(), st) <= 0f || !FuegoleonArmDjinnRules.sparkNow(st, now, p.getId())) continue;
            RandomSource r = level.getRandom();
            float yaw = p.yBodyRot * Mth.DEG_TO_RAD;                                                       // the right arm is on the wearer's right: (cos yaw, sin yaw) in the horizontal plane
            double side = 0.42 + r.nextDouble() * 0.2, h = 0.35 + r.nextDouble() * 1.15;
            double x = p.getX() - Mth.cos(yaw) * side, y = p.getY() + h, z = p.getZ() - Mth.sin(yaw) * side;
            Fx.spray(level, SPARKS, r.nextInt(3) == 0 ? FxSlot.ACCENT : FxSlot.MOTE, x, y, z, 1, (r.nextDouble() - 0.5) * 0.02, 0.04 + r.nextDouble() * 0.05, (r.nextDouble() - 0.5) * 0.02, 1.0);
        }
    }

    /** Wires the tick on the game bus (called once from the client block of the mod constructor). */
    public static void init() {
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(FuegoleonArmSparks::onClientTick);
    }
}
