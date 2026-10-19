package com.newuniverse.nusmp.client.relics.djinn;

import com.newuniverse.nusmp.client.particle.rukh.RukhClientFx;
import com.newuniverse.nusmp.client.relics.djinn.DjinnClientState.Snap;
import com.newuniverse.nusmp.particle.rukh.RukhFx.Alignment;
import com.newuniverse.nusmp.client.relics.djinn.DjinnLook.Anchor;
import com.newuniverse.nusmp.client.relics.djinn.DjinnRoster36.Dust;
import com.newuniverse.nusmp.relics.djinn.Djinn;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.RandomSource;

/**
 * The dust of the 24 Djinn added after Vassago: a coloured particle (the Djinn's glow fading into its body colour; a Dark Djinn's saturated colour fading
 * into black violet) that moves the way its affinity moves (embers rise, water falls, wind swirls, strength sinks heavily, sound spreads in rings, light
 * hangs and twinkles, lightning jitters), born where the Djinn's ambient recipe says (the wings, the body, the feet, above the head, the hand). It is spawned
 * with the ambient particles, so once a game tick for a bearer within 32 blocks, at most six a tick ({@link DjinnLook#spawnCount}); the numbers are the table of
 * {@link DjinnRoster36}.
 */
final class DjinnRoster36Fx {
    private DjinnRoster36Fx() {}

    static void emitPalette(ClientLevel level, AbstractClientPlayer p, Djinn d, Snap s, boolean dark) {
        Dust dust = DjinnRoster36.dustOf(d.name());
        if (dust == null) return;
        float share = DjinnLook.phaseShare(s.phase());
        if (share <= 0f) return;
        RandomSource rnd = level.getRandom();
        int n = DjinnLook.spawnCount(dust.rate(), share, rnd.nextFloat());
        if (n <= 0) return;
        int[] c = DjinnRoster36.colours(d, dark);
        Anchor anchor = DjinnLook.anchorAt(DjinnRoster36.ambientOfName(d.name()).anchor(), s.phase());
        double yaw = Math.toRadians(p.yBodyRot), fx = -Math.sin(yaw), fz = Math.cos(yaw), lx = Math.cos(yaw), lz = Math.sin(yaw), h = p.getBbHeight();
        long gt = level.getGameTime();
        for (int i = 0; i < n; i++) {
            double side = rnd.nextBoolean() ? 1.0 : -1.0, ox, oz, y;
            switch (anchor) {
                case WINGS -> {
                    double reach = 0.45 + rnd.nextDouble() * 1.3, back = 0.3 + rnd.nextDouble() * 0.4;
                    ox = lx * side * reach - fx * back;
                    oz = lz * side * reach - fz * back;
                    y = p.getY() + h * (0.4 + rnd.nextDouble() * 0.55);
                }
                case FEET -> {
                    ox = (rnd.nextDouble() - 0.5) * 0.7;
                    oz = (rnd.nextDouble() - 0.5) * 0.7;
                    y = p.getY() + 0.05 + rnd.nextDouble() * 0.3;
                }
                case ABOVE -> {
                    ox = (rnd.nextDouble() - 0.5) * 1.0;
                    oz = (rnd.nextDouble() - 0.5) * 1.0;
                    y = p.getY() + h + 0.15 + rnd.nextDouble() * 0.6;
                }
                case HAND -> {
                    ox = fx * 0.5 + lx * 0.45 + (rnd.nextDouble() - 0.5) * 0.3;
                    oz = fz * 0.5 + lz * 0.45 + (rnd.nextDouble() - 0.5) * 0.3;
                    y = p.getY() + h * 0.55 + rnd.nextDouble() * 0.3;
                }
                default -> {
                    ox = (rnd.nextDouble() - 0.5) * 1.1;
                    oz = (rnd.nextDouble() - 0.5) * 1.1;
                    y = p.getY() + rnd.nextDouble() * h;
                }
            }
            double vx = 0.0, vy = 0.0, vz = 0.0, jitter = 0.02;
            switch (dust.motion()) {
                case RISE -> { vy = 0.03 + rnd.nextDouble() * 0.05; vx = ox * 0.01; vz = oz * 0.01; }
                case FALL -> { vy = -0.02 - rnd.nextDouble() * 0.03; vx = Math.sin(gt * 0.1 + i) * 0.012; vz = Math.cos(gt * 0.1 + i) * 0.012; }
                case SWIRL -> { vx = -oz * 0.12; vz = ox * 0.12; vy = 0.01 + rnd.nextDouble() * 0.02; }
                case HEAVY -> { vy = -0.035 - rnd.nextDouble() * 0.03; vx = ox * 0.03; vz = oz * 0.03; }
                case RING -> {
                    double len = Math.max(0.2, Math.hypot(ox, oz));
                    vx = ox / len * 0.07;
                    vz = oz / len * 0.07;
                }
                case TWINKLE -> { vy = 0.004; jitter = 0.006; }
                case JITTER -> { vx = (rnd.nextDouble() - 0.5) * 0.12; vy = (rnd.nextDouble() - 0.5) * 0.12; vz = (rnd.nextDouble() - 0.5) * 0.12; jitter = 0.0; }
            }
            vx += (rnd.nextDouble() - 0.5) * jitter;
            vz += (rnd.nextDouble() - 0.5) * jitter;
            RukhClientFx.spray(level, dark ? Alignment.DARK : Alignment.LIGHT, p.getX() + ox, y, p.getZ() + oz, 1, vx, vy, vz, 1.0);   // the Rukh purge: a Rukh, not a vanilla dust
        }
    }

}
