package com.newuniverse.nusmp.star;

import com.newuniverse.nusmp.vfx.VfxShape;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Star Magic nodes, phase 2: stars sometimes die to Anti-Magic (docs/vfx_remake/star_ingame.md). The one server hook is {@code Nullification.shatterBarriers}, the mod's dispel that every Anti-Magic slash,
 * the Nihility zone and the other dispel-style arts already call at the moment of the cast; it calls {@link #onDispel} once with the sphere it dispels. There is no tick listener and no new event.
 *
 * <p>What happens: every star and Quartile Scutum pane of another mage that lies inside the sphere is rolled ({@link StarDispelRules#chance}); a winner breaks (record and body removed at once) with a small
 * shatter through {@link StarFx} (the pane's own break shards, scaled down for a star). Stars and panes of the caster and the caster's allies are never hit. The server switch {@code star.nodes} off, or
 * {@code star.dispelChance} 0, makes this a no-op.
 *
 * <p>Cost (rules 9 and 11): a pass over the in-memory star registry (at most 512 stars) and pane book (at most 8), no entity query, no chunk access; at most {@link StarDispelRules#MAX_CANDIDATES} stars
 * considered and {@link StarDispelRules#MAX_BREAKS} broken per call, each break one effect packet through the per-tick effect budget to players within 96 blocks. Zero work while nothing exists.
 */
public final class StarDispel {
    private StarDispel() {}

    /** The flags the shatter rides in the effect's seed (pane layer: 0 is a hit, anything else is the break shards). */
    public static final int FLAG_STAR = 2, FLAG_PANE = 3;
    public static final float STAR_POWER = 0.45f, PANE_POWER = 1.0f;

    /** The hook: dispel the sphere (c, r) as {@code by} (the caster, may be null) with the server's chance. Returns how many stars and panes broke. */
    public static int onDispel(ServerLevel level, Vec3 c, double r, LivingEntity by) {
        if (StarGame.NODES.total() == 0 && StarGame.PANES.size() == 0) return 0;
        if (!StarConfig.nodes()) return 0;
        return onDispel(level, c, r, by, StarConfig.dispelChance(), level.getRandom());
    }

    /** The same with the base chance and the random source given (the self-test and the tests use it). */
    public static int onDispel(ServerLevel level, Vec3 c, double r, LivingEntity by, double base, RandomSource rnd) {
        if (base <= 0.0 || r <= 0.0) return 0;
        if (StarGame.NODES.total() == 0 && StarGame.PANES.size() == 0) return 0;
        long now = level.getGameTime();
        String dim = StarGame.dimOf(level);
        Map<UUID, Boolean> heirs = new HashMap<>(4);
        StarFx.Budget budget = new StarFx.Budget();
        int broke = 0;
        for (StarNodes.Owned o : StarGame.NODES.near(dim, c.x, c.y, c.z, r, now, StarDispelRules.MAX_CANDIDATES)) {
            if (broke >= StarDispelRules.MAX_BREAKS) break;
            StarNodes.Node n = o.node();
            if (friendly(level, o.owner(), by) || !StarGame.GRACE.ready(n.entity(), now)) continue;
            boolean pane = StarGame.PANES.isEndStar(o.owner(), dim, n.x(), n.y(), n.z(), StarDispelRules.END_STAR_TOLERANCE);
            double chance = StarDispelRules.chance(base, pane ? StarDispelRules.Role.PANE : StarDispelRules.Role.NODE, heir(level, o.owner(), heirs));
            if (chance <= 0.0) continue;
            if (!StarDispelRules.breaks(chance, rnd.nextDouble())) { StarGame.GRACE.survived(n.entity(), now); continue; }
            StarGame.breakStar(level.getServer(), o.owner(), n);
            Vec3 at = new Vec3(n.x(), n.y(), n.z());
            StarFx.send(budget, level, VfxShape.STAR_PANE, at, at, FLAG_STAR, 12, STAR_POWER);
            broke++;
        }
        for (StarPaneBook.Pane p : StarGame.PANES.within(dim, c.x, c.y, c.z, r)) {
            if (broke >= StarDispelRules.MAX_BREAKS) break;
            if (friendly(level, p.owner, by) || !StarGame.GRACE.ready(p.entity, now)) continue;
            double chance = StarDispelRules.chance(base, StarDispelRules.Role.PANE, heir(level, p.owner, heirs));
            if (chance <= 0.0) continue;
            if (!StarDispelRules.breaks(chance, rnd.nextDouble())) { StarGame.GRACE.survived(p.entity, now); continue; }
            StarGame.breakPane(level.getServer(), p);
            Vec3 mid = StarGame.v(p.geom.center());
            StarFx.send(budget, level, VfxShape.STAR_PANE, mid, mid, FLAG_PANE, 14, PANE_POWER);
            broke++;
        }
        return broke;
    }

    /** The caster's own, or an ally's. A missing caster (a zone whose owner left) spares nothing. */
    static boolean friendly(ServerLevel level, UUID owner, LivingEntity by) {
        if (by == null) return false;
        if (by.getUUID().equals(owner)) return true;
        ServerPlayer o = level.getServer().getPlayerList().getPlayer(owner);
        return o != null && (by.isAlliedTo(o) || o.isAlliedTo(by));
    }

    /** Is this owner a Grinberryall heir (read once per owner per call; an owner who is offline is not). */
    private static boolean heir(ServerLevel level, UUID owner, Map<UUID, Boolean> cache) {
        Boolean known = cache.get(owner);
        if (known != null) return known;
        ServerPlayer o = level.getServer().getPlayerList().getPlayer(owner);
        boolean heir = o != null && StarGame.isHeir(o);
        cache.put(owner, heir);
        return heir;
    }
}
