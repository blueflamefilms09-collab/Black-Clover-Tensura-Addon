package com.newuniverse.nusmp.star;

import com.newuniverse.nusmp.book.GrimoireBook;
import com.newuniverse.nusmp.prop.MagicPropEntity;
import com.newuniverse.nusmp.prop.MagicProps;
import com.newuniverse.nusmp.prop.PropKind;
import com.newuniverse.nusmp.vfx.VfxShape;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Star Magic nodes: the two prop kinds (docs/vfx_remake/star_ingame.md). Painter: {@code client.prop.StarPropPainter}.
 * <ul>
 *   <li>{@link PropKind#STAR_NODE}: one star hanging in the air. It does nothing but count its life; every 20 ticks it checks that its owner is still online (an owner who left without the logout event
 *       costs at most one second). It is not hittable, not solid, and has no per-tick scan.</li>
 *   <li>{@link PropKind#STAR_PANE}: the barrier of Quartile Scutum. Its box is held in {@link StarGame#PANES}; each tick it asks for the projectiles inside its own box (ONE query of the pane's size, at most
 *       eight panes exist on the server, each lives 5 to 7 seconds) and absorbs the ones that are not its owner's side: the projectile is discarded and the pane's pool shrinks (a shot of a devil-powered owner
 *       counts half). The mod's own bolts are absorbed by {@link #absorbBolt} from {@code SpellRuntime}. The pane breaks when its pool is gone.</li>
 * </ul>
 */
public final class StarProps {
    private StarProps() {}

    /** Raw damage a projectile without its own number takes off a pane. */
    public static final float DEFAULT_SHOT = 4f, BOLT_SHOT = 8f;

    public static void init() {
        MagicProps.register(PropKind.STAR_NODE, new MagicProps.Behavior() {
            @Override public void init(MagicPropEntity e, ServerLevel sl) { e.setNoGravity(true); e.setSize(0.5f, 0.5f); }

            @Override public void tick(MagicPropEntity e, ServerLevel sl) {
                if (e.life() % 20 != 0) return;
                var owner = e.ownerId() == null ? null : sl.getServer().getPlayerList().getPlayer(e.ownerId());
                if (owner == null) e.expire();
            }

            @Override public void end(MagicPropEntity e, ServerLevel sl) { StarGame.bodyGone(e); }
        });
        MagicProps.register(PropKind.STAR_PANE, new MagicProps.Behavior() {
            @Override public void init(MagicPropEntity e, ServerLevel sl) { e.setNoGravity(true); e.setSize(0.5f, 0.5f); }

            @Override public void tick(MagicPropEntity e, ServerLevel sl) { paneTick(e, sl); }

            @Override public void end(MagicPropEntity e, ServerLevel sl) { StarGame.PANES.remove(e.getUUID()); }
        });
    }

    private static void paneTick(MagicPropEntity e, ServerLevel sl) {
        StarPaneBook.Pane pane = StarGame.PANES.get(e.getUUID());
        if (pane == null || sl.getGameTime() >= pane.expiry) { e.expire(); return; }
        double[] b = pane.geom.bounds();
        AABB box = new AABB(b[0], b[1], b[2], b[3], b[4], b[5]).inflate(0.6);
        List<Projectile> shots = sl.getEntitiesOfClass(Projectile.class, box, s -> s.isAlive());
        for (Projectile s : shots) {
            Entity owner = s.getOwner();
            if (isOwnerSide(sl, pane.owner, owner)) continue;
            Vec3 now = s.position(), before = now.subtract(s.getDeltaMovement());
            if (!StarMath.crosses(pane.geom, StarGame.p(before), StarGame.p(now)) && !StarMath.contains(pane.geom, StarGame.p(now))) continue;
            boolean devil = owner instanceof LivingEntity l && GrimoireBook.inDevilState(l);
            float amount = s instanceof AbstractArrow a ? (float) Math.max(2.0, a.getBaseDamage() * s.getDeltaMovement().length()) : DEFAULT_SHOT;
            s.discard();
            if (hit(sl, pane, e, StarRules.paneDamage(amount, devil), now)) return;
        }
    }

    /** Takes damage off the pane, updates its look (param = share left) and breaks it at zero. True when it broke. */
    private static boolean hit(ServerLevel sl, StarPaneBook.Pane pane, MagicPropEntity e, float amount, Vec3 at) {
        boolean broken = StarGame.PANES.damage(e.getUUID(), amount);
        e.setParam(StarRules.packPane(pane.percent(), pane.geom.width(), pane.geom.height()));
        StarFx.send(sl, VfxShape.STAR_PANE, at, StarGame.v(pane.geom.center()), broken ? 1 : 0, 14, 1f);
        if (broken) { e.expire(); return true; }
        return false;
    }

    private static boolean isOwnerSide(ServerLevel sl, java.util.UUID paneOwner, Entity shooter) {
        if (shooter == null) return false;
        if (shooter.getUUID().equals(paneOwner)) return true;
        Entity owner = sl.getEntity(paneOwner);
        return owner != null && shooter.isAlliedTo(owner);
    }

    /**
     * The mod's own bolts ({@code SpellRuntime.Bolt}): a bolt whose step a to b crosses a pane of someone it is not allied with ends there and takes {@link #BOLT_SHOT} off the pane. Cost: nothing while
     * no pane exists, else at most eight slab tests per bolt per tick. Returns true when the bolt was absorbed.
     */
    public static boolean absorbBolt(ServerLevel level, Vec3 a, Vec3 b, ServerPlayer shooter) {
        if (StarGame.PANES.size() == 0) return false;
        StarPaneBook.Pane pane = StarGame.PANES.firstCrossed(StarGame.dimOf(level), StarGame.p(a), StarGame.p(b), owner -> {
            if (shooter.getUUID().equals(owner)) return true;
            Entity o = level.getEntity(owner);
            return o != null && shooter.isAlliedTo(o);
        });
        if (pane == null) return false;
        if (level.getEntity(pane.entity) instanceof MagicPropEntity e) {
            float amount = StarRules.paneDamage(BOLT_SHOT, GrimoireBook.inDevilState(shooter));
            hit(level, pane, e, amount, b);
        }
        return true;
    }
}
