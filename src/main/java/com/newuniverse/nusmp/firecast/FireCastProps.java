package com.newuniverse.nusmp.firecast;

import com.newuniverse.nusmp.prop.MagicPropEntity;
import com.newuniverse.nusmp.prop.MagicProps;
import com.newuniverse.nusmp.prop.PropKind;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Why: the server half of the four props of the fire casting styles. FIRECAST_1 is the tossed ball (variant 0: it just hangs where it was put until the swing) and Leopold's spiral (variant 1: it
 * stays in front of the caster's eye); FIRECAST_2 is the Salamander (variant 2: the small one on the shoulder, variant 3: the big dragon above and behind). The props are only bodies: every
 * rule (damage, caps, the swing test) lives in {@link FireCasting}; the clients draw them from the synced kind, param and the owner (the target), so a prop needs a position update only every
 * {@link #NUDGE} ticks, just enough to stay inside the owner's tracking range.
 */
public final class FireCastProps implements MagicProps.Behavior {
    public static final FireCastProps INSTANCE = new FireCastProps();
    /** Ticks between two position updates of a following prop (the painters place it from the owner every frame, this only keeps the entity near its owner). */
    static final int NUDGE = 5;

    private FireCastProps() {}

    /** Called once by PropRegistry. */
    public static void init() {
        MagicProps.register(PropKind.FIRECAST_1, INSTANCE);
        MagicProps.register(PropKind.FIRECAST_2, INSTANCE);
    }

    @Override
    public void tick(MagicPropEntity e, ServerLevel sl) {
        int variant = FireCastRules.variant(e.param());
        if (variant == FireCastRules.V_BALL) return;                                    // it hangs where it was put
        LivingEntity o = e.owner();
        if (o == null) { e.expire(); return; }
        if (e.life() % NUDGE != 0) return;
        switch (variant) {
            case FireCastRules.V_SPIRAL -> {
                Vec3 at = o.getEyePosition().add(o.getViewVector(1f).scale(1.5)).subtract(0, 0.5, 0);
                e.setPos(at.x, at.y, at.z);
                e.setYRot(o.getYRot());
            }
            case FireCastRules.V_SALAMANDER -> {
                FireCasting.State s = FireCasting.STATES.get(o.getUUID());
                if (s == null || !e.getUUID().equals(s.spirit)) { e.expire(); return; }          // an orphan (the link was dropped): gone
                double[] a = FireCastRules.shoulder(o.getX(), o.getY(), o.getZ(), o.yBodyRot);
                e.setPos(a[0], a[1], a[2]);
                e.setYRot(o.yBodyRot);
                int want = s.dragon != null ? e.param() | FireCastRules.HIDDEN : e.param() & ~FireCastRules.HIDDEN;      // hidden while the big form is out
                if (want != e.param()) e.setParam(want);
                if (e.life() % 100 == 20 && o instanceof ServerPlayer p) FireCasting.aura(p);       // Fire Resistance, as the elemental Salamander gives its master
            }
            case FireCastRules.V_DRAGON -> {
                double[] a = FireCastRules.dragonPosition(o.getX(), o.getY(), o.getZ(), o.yBodyRot);
                e.setPos(a[0], a[1], a[2]);
                e.setYRot(o.yBodyRot);
            }
            default -> { }
        }
    }

    @Override
    public void end(MagicPropEntity e, ServerLevel sl) {
        FireCasting.propEnded(e.ownerId(), FireCastRules.variant(e.param()), e.getUUID());
    }
}
