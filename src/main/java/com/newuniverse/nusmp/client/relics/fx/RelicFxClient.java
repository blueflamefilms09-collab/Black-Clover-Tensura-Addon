package com.newuniverse.nusmp.client.relics.fx;

import com.newuniverse.nusmp.client.relics.worn.lib.WornLibrary;
import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.RelicSlot;
import com.newuniverse.nusmp.relics.Relics;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Vector3f;

import java.util.List;
import com.newuniverse.nusmp.particle.fx.Fx;
import com.newuniverse.nusmp.particle.fx.FxThemes;
import com.newuniverse.nusmp.particle.fx.FxTone;

/**
 * The ambient particles of worn relics: embers, snow, sparks, leaves, wisps, stars, petals drifting off whoever wears a relic of RARE grade or higher, themed by the relic's glow
 * colour or by its entry in the catalog ({@link FxTheme}), and a thin flowing halo of motes round the head of a GOD or GENESIS wearer. Strictly budgeted, all client side:
 * <ul>
 *   <li>at most {@link FxQuality#particlesPerTick} particles in a client tick over all wearers (the quality option), none if {@code relicfx.particles} is off;</li>
 *   <li>only wearers within 32 blocks, and beyond 4 blocks only those in front of the camera, at most ten wearers looked at per tick;</li>
 *   <li>per wearer and relic only every N ticks (N by grade and quality, staggered by entity id so the work spreads over the ticks);</li>
 *   <li>the wearer's relics come from the 10-tick cache of {@link Relics}.</li>
 * </ul>
 */
public final class RelicFxClient {
    private RelicFxClient() {}

    private static int budget;
    private static boolean inited;

    /** Game bus: registers the client tick. Safe to call twice. */
    public static synchronized void init() {
        if (inited) return;
        inited = true;
        NeoForge.EVENT_BUS.addListener(RelicFxClient::onClientTick);
    }

    static void onClientTick(ClientTickEvent.Post e) {
        budget = 0;
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.player == null || mc.isPaused() || !RelicFxOptions.particles()) return;
        FxQuality q = RelicFxOptions.quality();
        Camera cam = mc.gameRenderer.getMainCamera();
        Vec3 cp = cam.getPosition();
        Vector3f look = cam.getLookVector();
        long tick = level.getGameTime();
        int looked = 0;
        for (AbstractClientPlayer p : level.players()) {
            if (budget >= q.particlesPerTick || looked >= 10) break;
            if (p.isSpectator() || p.isInvisible()) continue;
            double dx = p.getX() - cp.x, dy = p.getEyeY() - cp.y, dz = p.getZ() - cp.z, d2 = dx * dx + dy * dy + dz * dz;
            if (d2 > WornFxPlan.CULL_RANGE * WornFxPlan.CULL_RANGE) continue;
            if (d2 > 16.0 && dx * look.x + dy * look.y + dz * look.z < 0.15 * Math.sqrt(d2)) continue;          // behind the camera or far off to the side
            budget += com.newuniverse.nusmp.client.relics.worn.lib.ArmorFx.tick(level, p, q, tick, q.particlesPerTick - budget);     // the mod's own armour (robe sets ...)
            List<Relics.Worn> worn = Relics.wornList(p);
            if (worn.isEmpty()) continue;
            looked++;
            List<com.newuniverse.nusmp.item.CuriosLink.RenderFlag> flags = null;
            boolean hideArmour = RelicFxOptions.hideArmourRelics();
            for (int i = 0; i < worn.size() && budget < q.particlesPerTick; i++) {
                RelicDef d = worn.get(i).def();
                if (d.slot().isHand() || d.slot() == RelicSlot.CONSUMABLE) continue;
                if (d.slot().isArmor() && hideArmour) continue;                                  // switched off with the option / key
                if (d.slot().isCurio()) {                                                         // the eye icon of the Curios screen: off means no particles either
                    if (flags == null) flags = com.newuniverse.nusmp.item.CuriosLink.flags(p);
                    boolean on = true;
                    for (var f : flags) if (f.stack() == worn.get(i).stack()) { on = f.render(); break; }
                    if (!on) continue;
                }
                WornFxPlan plan = WornFxPlan.of(d.grade().tier(), q, 0.0);
                if (plan.particleEvery() <= 0 || (tick + p.getId() * 7L + i * 3L) % plan.particleEvery() != 0) continue;
                FxTheme theme = themeOf(d);
                for (int k = 0; k < plan.particlesPerEvent() && budget < q.particlesPerTick; k++) spawn(level, p, d, theme, level.random);
                if (d.grade().tier() >= 6 && q != FxQuality.LOW && budget < q.particlesPerTick) halo(level, p, d, tick);
            }
        }
    }

    private static FxTheme themeOf(RelicDef d) {
        var e = WornLibrary.of(d);
        return e != null ? e.theme : FxTheme.fromGlow(d.paletteGlow());
    }

    /** Where on the body a slot's relic sheds its particles: height range in blocks above the feet and how far behind (+) or beside the wearer. */
    private static void spawn(ClientLevel level, AbstractClientPlayer p, RelicDef d, FxTheme theme, RandomSource rnd) {
        if (theme == FxTheme.NONE) return;
        double y0, y1, back = 0.0, side = 0.0, spread = 0.32;
        switch (d.slot()) {
            case HEAD -> { y0 = 1.65; y1 = 2.05; spread = 0.30; }
            case NECK, CHEST -> { y0 = 1.1; y1 = 1.5; }
            case BACK -> { y0 = 0.7; y1 = 1.6; back = 0.28; }
            case BELT, CHARM -> { y0 = 0.7; y1 = 1.1; side = 0.25; }
            case HANDS -> { y0 = 0.75; y1 = 1.05; side = 0.4; }
            case FEET -> { y0 = 0.02; y1 = 0.3; spread = 0.25; }
            default -> { y0 = 0.5; y1 = 1.5; }
        }
        double yaw = Math.toRadians(p.yBodyRot);
        double fx = -Math.sin(yaw), fz = Math.cos(yaw);
        double x = p.getX() - fx * back + fz * side * (rnd.nextBoolean() ? 1 : -1) + (rnd.nextDouble() - 0.5) * 2 * spread;
        double z = p.getZ() - fz * back - fx * side * (rnd.nextBoolean() ? 1 : -1) + (rnd.nextDouble() - 0.5) * 2 * spread;
        double y = p.getY() + y0 + rnd.nextDouble() * (y1 - y0);
        double vx = (rnd.nextDouble() - 0.5) * 0.01, vz = (rnd.nextDouble() - 0.5) * 0.01;
        budget++;
        switch (theme) {
            case EMBER -> Fx.spray(level, FxThemes.RELIC, FxTone.LIGHT, x, y, z, 1, vx, 0.012, vz, 1.0);
            case SNOW -> Fx.spray(level, FxThemes.RELIC, FxTone.LIGHT, x, y, z, 1, vx, -0.004, vz, 1.0);
            case SPARK -> Fx.spray(level, FxThemes.RELIC, FxTone.LIGHT, x, y, z, 1, vx * 3, 0.0, vz * 3, 1.0);
            case LEAF -> Fx.spray(level, FxThemes.RELIC, FxTone.LIGHT, x, y, z, 1, vx, -0.01, vz, 1.0);
            case WISP -> Fx.spray(level, FxThemes.RELIC, FxTone.DARK, x, y, z, 1, vx, 0.02, vz, 1.0);
            case STAR -> Fx.spray(level, FxThemes.RELIC, FxTone.LIGHT, x, y, z, 1, vx, 0.01, vz, 1.0);
            case BEE -> Fx.spray(level, FxThemes.RELIC, FxTone.LIGHT, x, y, z, 1, vx * 4, 0.0, vz * 4, 1.0);
            case PETAL -> Fx.spray(level, FxThemes.RELIC, FxTone.LIGHT, x, y, z, 1, vx, -0.01, vz, 1.0);
            default -> { }
        }
    }

    /** A moving mote of the halo: one particle on a ring over the head, drifting along it. */
    private static void halo(ClientLevel level, AbstractClientPlayer p, RelicDef d, long tick) {
        double a = tick * 0.29 + p.getId();
        double r = 0.42, y = p.getY() + p.getBbHeight() + 0.28;
        double x = p.getX() + Math.cos(a) * r, z = p.getZ() + Math.sin(a) * r;
        budget++;
        double tx = -Math.sin(a) * 0.035, tz = Math.cos(a) * 0.035;
        if (d.grade().tier() >= 7) Fx.spray(level, FxThemes.RELIC, FxTone.LIGHT, x, y, z, 1, tx, 0.0, tz, 1.0);
        else Fx.spray(level, FxThemes.RELIC, FxTone.LIGHT, x, y, z, 1, tx, 0.0, tz, 1.0);
    }

}
