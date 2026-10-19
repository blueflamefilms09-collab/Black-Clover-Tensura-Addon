package com.newuniverse.nusmp.prop;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.book.VortexBook;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 1.93.0 Vortex Magic: the server behaviours of its one prop kind (VORTEX_1; variant in the low byte of {@code param}, see {@link AirPropKit}):
 * <ul>
 *   <li>CORE (0), a maelstrom core: hangs where it was made and pulls everything within 8 blocks towards itself every tick (stronger near the middle, bosses
 *       pulled at a third of the strength), cutting 2 damage x EP power every half second inside 3 blocks; when it ends it implodes: 10 damage in 4.5 blocks and
 *       a last pull.</li>
 *   <li>FUNNEL (1), a cyclone: walks forward along the way it faces at 0.3 blocks a tick, pulling everything within 3.5 blocks into its body and lifting it,
 *       3 damage x EP power every half second; it throws everything it holds away when it ends.</li>
 * </ul>
 * Pulls are small changes of velocity (never teleports), allies and the owner are left alone, creative / spectator players are skipped.
 */
public final class VortexProps {
    private VortexProps() {}

    public static final int CORE = 0, FUNNEL = 1;
    private static final int COLOR = 0xFF9EEBF5;

    /** Called once by PropRegistry. */
    public static void init() {
        MagicProps.register(PropKind.VORTEX_1, new Behavior());
        com.newuniverse.nusmp.book.ext.AttributeEvents.playerTick(VortexBook::mantle);
        VortexSelfTests.register();
    }

    /** Pulls {@code t} towards {@code centre} by {@code k} blocks of speed (a third for a boss), keeping some of its own motion; optionally lifts. */
    public static void pull(LivingEntity t, Vec3 centre, double k, double lift) {
        if (t instanceof net.minecraft.world.entity.player.Player p && (p.isCreative() || p.isSpectator())) return;
        Vec3 d = centre.subtract(t.position().add(0, t.getBbHeight() / 2, 0));
        double len = d.length();
        if (len < 0.3) return;
        double kk = BalanceLaw.isBoss(t) ? k / 3 : k;
        Vec3 v = d.scale(Math.min(kk, len * 0.5) / len);
        t.setDeltaMovement(t.getDeltaMovement().scale(0.85).add(v.x, v.y * 0.6 + lift, v.z));
        t.hurtMarked = true;
        t.fallDistance = 0;
    }

    private static final class Behavior implements MagicProps.Behavior {
        @Override
        public void init(MagicPropEntity e, ServerLevel sl) {
            int v = AirPropKit.variant(e.param());
            float s = e.scale();
            if (v == CORE) e.setSize(1.0f * s, 1.0f * s); else e.setSize(2.0f * s, 3.4f * s);
            e.setParam(AirPropKit.pack(v, 0, sl.getGameTime()));
            VfxSpawn.send(sl, VfxShape.VORTEX_FX3, e.position(), e.position().add(0, 1, 0), COLOR, 20, (v == CORE ? 0.5f : 0.8f) * s);
        }

        @Override
        public void tick(MagicPropEntity e, ServerLevel sl) {
            LivingEntity owner = e.owner();
            if (owner == null) { e.expire(); return; }
            int v = AirPropKit.variant(e.param());
            Vec3 at = e.position().add(0, e.getBbHeight() / 2, 0);
            double reach = v == CORE ? 8.0 : 3.5;
            if (v == FUNNEL) {
                Vec3 dir = Vec3.directionFromRotation(0f, e.getYRot());
                Vec3 next = e.position().add(dir.scale(0.3));
                e.setPos(next);
            }
            for (LivingEntity t : sl.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(reach, reach * 0.7, reach), t -> AirPropKit.fair(owner, t))) {
                double d = t.position().distanceTo(at);
                if (d > reach) continue;
                double k = (v == CORE ? 0.5 : 0.35) * (1.2 - d / (reach * 1.2));
                pull(t, at, k, v == FUNNEL ? 0.05 : 0.0);
                if (e.life() % 10 == 0 && d < (v == CORE ? 3.0 : 2.4))
                    AirPropKit.hit(e, sl, owner, t, (v == CORE ? 2f : 3f) * AirPropKit.powerOf(owner), TensuraDamageTypes.WIND_ELEMENTAL);
            }
        }

        @Override
        public void end(MagicPropEntity e, ServerLevel sl) {
            int v = AirPropKit.variant(e.param());
            Vec3 at = e.position().add(0, e.getBbHeight() / 2, 0);
            LivingEntity owner = e.owner();
            VfxSpawn.send(sl, VfxShape.VORTEX_FX3, at, at.add(0, 1, 0), COLOR, 24, (v == CORE ? 1.0f : 0.9f) * e.scale());
            if (owner == null) return;
            for (LivingEntity t : sl.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(4.5), t -> AirPropKit.fair(owner, t))) {
                if (v == CORE) AirPropKit.hit(e, sl, owner, t, 10f * AirPropKit.powerOf(owner), TensuraDamageTypes.WIND_ELEMENTAL);
                Vec3 out = t.position().subtract(at).multiply(1, 0, 1);
                if (out.lengthSqr() > 1e-4 && v == FUNNEL) {
                    Vec3 o = out.normalize().scale(BalanceLaw.isBoss(t) ? 0.3 : 0.9);
                    t.setDeltaMovement(t.getDeltaMovement().add(o.x, 0.5, o.z));
                    t.hurtMarked = true;
                }
            }
        }
    }
}
