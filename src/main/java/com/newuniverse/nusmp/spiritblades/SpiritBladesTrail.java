package com.newuniverse.nusmp.spiritblades;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import com.newuniverse.nusmp.particle.fx.Fx;
import com.newuniverse.nusmp.particle.fx.FxOld;
import com.newuniverse.nusmp.particle.fx.FxThemes;

/**
 * Why: the glowing trail of Spirit of Boreas' arrows, drawn by the server. A trailed arrow is one small record in a list (at most {@value SpiritBladesRules#TRAIL_GLOBAL} over the whole server and
 * {@value SpiritBladesRules#TRAIL_PER_OWNER} per owner: a shot past the cap simply flies without a trail), ticked once per server tick: every {@value SpiritBladesRules#TRAIL_EVERY} ticks it sends
 * ordinary particle packets of {@value SpiritBladesRules#TRAIL_PARTICLES} sparkles (only to players near it, vanilla's own range), at most {@value SpiritBladesRules#TRAIL_PUFFS_PER_TICK} puffs a tick
 * over the server, for at most {@value SpiritBladesRules#TRAIL_MAX_AGE} ticks (the trail ends when the arrow stops, dies or leaves the level). Nothing is spawned, no block or entity is changed. The
 * list is cleared when the server stops; the owner id is only a counter key (the record holds the arrow, which is dropped when it is removed). Server thread only.
 */
public final class SpiritBladesTrail {
    private SpiritBladesTrail() {}

    private static final DustParticleOptions MINT = new DustParticleOptions(new Vector3f(0.62f, 1.0f, 0.84f), 0.8f);

    private static final class Tracked {
        final AbstractArrow arrow;
        final UUID owner;
        int age;

        Tracked(AbstractArrow arrow, UUID owner) {
            this.arrow = arrow;
            this.owner = owner;
        }
    }

    private static final List<Tracked> LIVE = new ArrayList<>(SpiritBladesRules.TRAIL_GLOBAL);

    /** Starts trailing this arrow (a server arrow) unless a cap is reached. */
    public static void track(AbstractArrow arrow) {
        if (!(arrow.level() instanceof ServerLevel)) return;
        Entity o = arrow.getOwner();
        UUID owner = o == null ? new UUID(0L, 0L) : o.getUUID();
        int mine = 0;
        for (Tracked t : LIVE) if (t.owner.equals(owner)) mine++;
        if (!SpiritBladesRules.canTrack(mine, LIVE.size())) return;
        LIVE.add(new Tracked(arrow, owner));
    }

    /** Once per server tick (the post event). */
    public static void tick() {
        if (LIVE.isEmpty()) return;
        int puffs = 0;
        for (int i = LIVE.size() - 1; i >= 0; i--) {
            Tracked t = LIVE.get(i);
            AbstractArrow a = t.arrow;
            if (a.isRemoved() || !(a.level() instanceof ServerLevel level)) {
                LIVE.remove(i);
                continue;
            }
            Vec3 v = a.getDeltaMovement();
            if (SpiritBladesRules.trailOver(t.age, v.lengthSqr())) {
                LIVE.remove(i);
                continue;
            }
            if (SpiritBladesRules.puffDue(t.age) && puffs < SpiritBladesRules.TRAIL_PUFFS_PER_TICK) {
                puffs++;
                level.sendParticles(MINT, a.getX(), a.getY(), a.getZ(), SpiritBladesRules.TRAIL_PARTICLES - 1, 0.04, 0.04, 0.04, 0.0);
                Fx.spray(level, FxThemes.WORLD, FxOld.END_ROD, a.getX(), a.getY(), a.getZ(), 1, 0.03, 0.03, 0.03, 0.0);
            }
            t.age++;
        }
    }

    /** How many arrows are trailed now (for the self-test). */
    public static int live() { return LIVE.size(); }

    /** Ends every trail (server stop). */
    public static void clearAll() { LIVE.clear(); }
}
