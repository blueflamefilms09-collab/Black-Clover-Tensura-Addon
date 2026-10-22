package com.newuniverse.nusmp.undine;

import com.newuniverse.nusmp.prop.MagicPropEntity;
import com.newuniverse.nusmp.prop.MagicProps;
import com.newuniverse.nusmp.prop.PropKind;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * Why: the server half of the Undine's one prop kind. UNDINE_1 is the companion (variant 0: floats beside the caster's right shoulder) and the full form (variant 1: stands behind the caster on a ring of
 * waves). The prop is only a body: every rule (caps, cooldowns, the emitter) lives in {@link UndineCasting}; the clients draw it from the synced kind, param and the owner (the target), so a prop needs a
 * position update only every {@link UndineRules#NUDGE} ticks, just enough to stay inside the owner's tracking range.
 */
public final class UndineProps implements MagicProps.Behavior {
    public static final UndineProps INSTANCE = new UndineProps();

    private UndineProps() {}

    /** Called once by PropRegistry. */
    public static void init() { MagicProps.register(PropKind.UNDINE_1, INSTANCE); }

    @Override
    public void tick(MagicPropEntity e, ServerLevel sl) {
        if (e.life() % UndineRules.NUDGE != 0) return;
        LivingEntity o = e.owner();
        if (o == null) { e.expire(); return; }
        int variant = UndineRules.variant(e.param());
        UndineCasting.State s = UndineCasting.STATES.get(o.getUUID());
        if (variant == UndineRules.V_COMPANION) {
            if (s == null || !e.getUUID().equals(s.spirit)) { e.expire(); return; }          // an orphan (the link was dropped): gone
            double[] a = UndineRules.companion(o.getX(), o.getY(), o.getZ(), o.yBodyRot);
            e.setPos(a[0], a[1], a[2]);
            e.setYRot(o.yBodyRot);
            int want = s.full != null ? e.param() | UndineRules.HIDDEN : e.param() & ~UndineRules.HIDDEN;      // hidden while the full form is out
            if (want != e.param()) e.setParam(want);
            if (e.life() % 100 == 20 && o instanceof ServerPlayer p) UndineCasting.aura(p);
        } else if (variant == UndineRules.V_FULL) {
            if (s == null || !e.getUUID().equals(s.full)) { e.expire(); return; }
            double[] a = UndineRules.full(o.getX(), o.getY(), o.getZ(), o.yBodyRot);
            e.setPos(a[0], a[1], a[2]);
            e.setYRot(o.yBodyRot);
        }
    }

    @Override
    public void end(MagicPropEntity e, ServerLevel sl) {
        UndineCasting.propEnded(e.ownerId(), UndineRules.variant(e.param()), e.getUUID());
    }
}
