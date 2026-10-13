package com.newuniverse.nusmp.relics.djinn;

import com.newuniverse.nusmp.relics.Fx;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Who a Djinn's spells touch. Foes are the relics' own notion ({@link Fx#foesAround}: hostile mobs, mobs that target the caster, players the
 * caster may harm, never creative or spectator players, never allies or tamed pets, at most 24). Allies are the caster, their team, and the
 * household of their Djinn.
 */
final class DjinnTargets {
    private DjinnTargets() {}

    /** Foes within {@code radius} of {@code centre}, nearest to the caster first (a query of {@code radius + offset} round the caster, then a distance filter). */
    static List<LivingEntity> foesNear(ServerPlayer p, Vec3 centre, double radius) {
        double reach = radius + centre.distanceTo(p.position());
        List<LivingEntity> out = new ArrayList<>();
        double r2 = radius * radius;
        for (LivingEntity t : Fx.foesAround(p, Math.min(32.0, reach))) if (t.position().distanceToSqr(centre) <= r2) out.add(t);
        return out;
    }

    /** The caster, their allies by team, and the household of their Djinn who are within {@code radius}. */
    static List<ServerPlayer> alliesNear(ServerPlayer p, double radius) {
        List<ServerPlayer> out = new ArrayList<>();
        Djinn d = DjinnState.of(p).active();
        List<UUID> house = d == null || p.getServer() == null ? List.of() : DjinnLedger.get(p.getServer()).household(d);
        double r2 = radius * radius;
        for (ServerPlayer o : p.serverLevel().players()) {
            if (!o.isAlive() || o.isSpectator() || o.distanceToSqr(p) > r2) continue;
            if (o == p || o.isAlliedTo(p) || house.contains(o.getUUID())) out.add(o);
        }
        return out;
    }

    /** A non-player foe (the Lesser Summon never opens fire on players). */
    static boolean isMob(LivingEntity t) { return !(t instanceof Player); }
}
