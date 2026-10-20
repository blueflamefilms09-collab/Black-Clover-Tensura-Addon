package com.newuniverse.nusmp.relics.djinn;

import com.newuniverse.nusmp.relics.Fx;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The server's one word to the Tiny Djinn (client/relics/djinn/tiny): "I am about to strike this foe". The Lesser Summon strikes the nearest hostile mob within 12
 * blocks every two seconds ({@code DjinnMagic.lesserBolt}, tick 0 of each 40); {@link DjinnEngine} calls {@link #lesserCue} at tick 24, sixteen ticks earlier, with
 * the same pick, so the little figure can point at the foe (12 ticks), dash (4) and arrive as the bolt lands. Nothing about the Lesser Summon changes: the cue is a
 * packet to the watchers (the {@link DjinnNet.Cue}); a client that lacks it never learns of the cast and the figure simply floats.
 */
public final class TinyDjinnCue {
    private TinyDjinnCue() {}

    /** The foe the Lesser Summon would strike now: the nearest non-player foe within 12 blocks (the pick of {@code DjinnMagic.lesserBolt}), or null. */
    public static LivingEntity pick(ServerPlayer p) {
        LivingEntity best = null;
        double bd = Double.MAX_VALUE;
        for (LivingEntity t : Fx.foesAround(p, 12.0)) {
            if (!DjinnTargets.isMob(t)) continue;
            double dd = t.distanceToSqr(p);
            if (dd < bd) { bd = dd; best = t; }
        }
        return best;
    }

    /** Tells everyone who can see {@code p} (and {@code p}) that the Lesser Summon is about to strike. A stand-in without a connection (a self-test) tells nobody. */
    static void lesserCue(ServerPlayer p) {
        LivingEntity foe = pick(p);
        if (foe == null || p.connection == null) return;
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(p, new DjinnNet.Cue(p.getId(), foe.getId()));
    }
}
