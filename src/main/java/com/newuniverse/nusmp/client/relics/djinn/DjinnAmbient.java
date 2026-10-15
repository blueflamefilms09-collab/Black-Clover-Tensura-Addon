package com.newuniverse.nusmp.client.relics.djinn;

import com.newuniverse.nusmp.particle.rukh.RukhFx.Alignment;
import com.newuniverse.nusmp.client.particle.rukh.RukhClientFx;
import com.newuniverse.nusmp.client.relics.djinn.DjinnClientState.Snap;
import com.newuniverse.nusmp.client.relics.djinn.DjinnLook.Ambient;
import com.newuniverse.nusmp.client.relics.djinn.DjinnLook.Anchor;
import com.newuniverse.nusmp.relics.djinn.Djinn;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.RandomSource;

import java.util.HashMap;
import java.util.Map;

/**
 * What each Djinn sheds while it is equipped: sparks off Baal's feathers, embers from Amon, bubbles and drops from Vinea, gusts and feathers from
 * Focalor, petals from Zagan, shards from Zepar, notes from Furfur, a trail of embers from Marchosias, motes of light from Sitri, violet sparks from
 * Bifron, white feathers from Paimon, ink and drops from Vassago; a Dark Djinn adds the black rukh streaming off its wings. The recipes are the table in
 * {@link DjinnLook}; this class spawns them as client-side particles, once per game tick per player (the layer is drawn many times a frame), only for a
 * player within 32 blocks, at most six of a kind a tick.
 */
final class DjinnAmbient {
    private DjinnAmbient() {}

    static final double RANGE2 = 32.0 * 32.0;
    private static final Map<Integer, Long> LAST = new HashMap<>();

    /** True the first time it is asked for this player in this game tick (the render layer runs per frame, the particles must not). */
    static boolean due(int entityId, long gameTime) {
        Long last = LAST.get(entityId);
        if (last != null && last == gameTime) return false;
        if (LAST.size() > 200) LAST.clear();
        LAST.put(entityId, gameTime);
        return true;
    }

    static void clear() { LAST.clear(); }

    static void emit(ClientLevel level, AbstractClientPlayer p, Djinn d, Snap s, boolean dark) {
        float share = DjinnLook.phaseShare(s.phase());
        if (share <= 0f) return;
        Ambient a = DjinnLook.ambient(d.ordinal());
        RandomSource rnd = level.getRandom();
        Anchor anchor = DjinnLook.anchorAt(a.anchor(), s.phase());
        spawn(level, rnd, p, a.main(), DjinnLook.spawnCount(a.mainRate(), share, rnd.nextFloat()), anchor, d.glow(), dark);
        if (a.secondRate() > 0) spawn(level, rnd, p, a.second(), DjinnLook.spawnCount(a.secondRate(), share, rnd.nextFloat()), anchor, d.glow(), dark);
        // The Magi Rukh gathering: while the Djinn is worn a flock keeps converging on the player, light for a light Djinn, dark for the dark state.
        if (level.getGameTime() % 10 == 0) RukhClientFx.gather(level, p, dark ? Alignment.DARK : Alignment.LIGHT, phaseGather(s.phase()));
        if (dark) spawn(level, rnd, p, DjinnLook.RUKH_KEY, DjinnLook.spawnCount(DjinnLook.RUKH_RATE, share, rnd.nextFloat()), DjinnLook.anchorAt(Anchor.WINGS, s.phase()), 0x120818, true);
    }

    private static void spawn(ClientLevel level, RandomSource rnd, AbstractClientPlayer p, String key, int count, Anchor anchor, int rgb, boolean dark) {
        boolean rukh = key.equals(DjinnLook.RUKH_KEY);
        if (count <= 0) return;
        double yaw = Math.toRadians(p.yBodyRot);
        double fx = -Math.sin(yaw), fz = Math.cos(yaw), lx = Math.cos(yaw), lz = Math.sin(yaw);
        double h = p.getBbHeight();
        boolean falls = DjinnLook.falls(key);
        for (int i = 0; i < count; i++) {
            double side = rnd.nextBoolean() ? 1.0 : -1.0, x, y, z, ox, oz;
            switch (anchor) {
                case WINGS -> {
                    double reach = 0.45 + rnd.nextDouble() * 1.2, back = 0.32 + rnd.nextDouble() * 0.35;
                    ox = lx * side * reach - fx * back;
                    oz = lz * side * reach - fz * back;
                    y = p.getY() + h * (0.45 + rnd.nextDouble() * 0.5);
                }
                case FEET -> {
                    ox = (rnd.nextDouble() - 0.5) * 0.5 - fx * 0.2;
                    oz = (rnd.nextDouble() - 0.5) * 0.5 - fz * 0.2;
                    y = p.getY() + 0.1 + rnd.nextDouble() * 0.2;
                }
                case ABOVE -> {
                    ox = (rnd.nextDouble() - 0.5) * 0.9;
                    oz = (rnd.nextDouble() - 0.5) * 0.9;
                    y = p.getY() + h + 0.2 + rnd.nextDouble() * 0.5;
                }
                default -> {
                    ox = (rnd.nextDouble() - 0.5) * 1.1;
                    oz = (rnd.nextDouble() - 0.5) * 1.1;
                    y = p.getY() + rnd.nextDouble() * h;
                }
            }
            x = p.getX() + ox;
            z = p.getZ() + oz;
            double vx = (rnd.nextDouble() - 0.5) * 0.03 + ox * 0.03, vz = (rnd.nextDouble() - 0.5) * 0.03 + oz * 0.03;
            double vy = falls ? -0.015 - rnd.nextDouble() * 0.02 : 0.02 + rnd.nextDouble() * 0.05;
            if (key.equals("note")) {                                   // a note's colour is its first velocity component (a hue, 0 to 1)
                RukhClientFx.spray(level, dark ? Alignment.DARK : Alignment.LIGHT, x, y, z, 1, 0.0, 0.02, 0.0, 1.0);
                continue;
            }
            if (key.equals(DjinnLook.RUKH_KEY)) { vx -= fx * 0.05; vz -= fz * 0.05; }        // the black rukh streams off backwards
            RukhClientFx.spray(level, rukh || dark ? Alignment.DARK : Alignment.LIGHT, x, y, z, 1, vx, vy, vz, 1.0);
        }
    }

    /** How many gathering birds per pulse: a few for the Half Equip, more for the Full Equip. */
    static int phaseGather(int phase) { return phase >= 3 ? 3 : 2; }
}
