package com.newuniverse.nusmp.sylph;

import com.newuniverse.nusmp.prop.MagicPropEntity;
import com.newuniverse.nusmp.prop.MagicProps;
import com.newuniverse.nusmp.prop.PropKind;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * Why: the server half of the Sylph prop. SYLPH_1 is the small Sylph beside the caster (variant 0) and the powered form above and behind (variant 1). The prop is only a body: every rule (caps,
 * cooldown, the emitter) lives in {@link SylphCasting}; the clients draw it from the synced kind, param and the owner (the target), so a prop needs a position update only every {@link #NUDGE}
 * ticks, just enough to stay inside the owner's tracking range.
 */
public final class SylphProps implements MagicProps.Behavior {
    public static final SylphProps INSTANCE = new SylphProps();
    /** Ticks between two position updates of a following prop (the painters place it from the owner every frame, this only keeps the entity near its owner). */
    static final int NUDGE = 5;

    private SylphProps() {}

    /** Called once by PropRegistry. */
    public static void init() { MagicProps.register(PropKind.SYLPH_1, INSTANCE); }

    @Override
    public void tick(MagicPropEntity e, ServerLevel sl) {
        if (e.life() % NUDGE != 0) return;
        int variant = SylphRules.variant(e.param());
        LivingEntity o = e.owner();
        if (o == null) { e.expire(); return; }
        SylphCasting.State s = SylphCasting.STATES.get(o.getUUID());
        if (s == null || !e.getUUID().equals(variant == SylphRules.V_POWERED ? s.powered : s.spirit)) { e.expire(); return; }          // an orphan (the link was dropped): gone
        if (variant == SylphRules.V_POWERED) {
            double[] a = SylphRules.powered(o.getX(), o.getY(), o.getZ(), o.yBodyRot);
            e.setPos(a[0], a[1], a[2]);
        } else {
            double[] a = SylphRules.small(o.getX(), o.getY(), o.getZ(), o.yBodyRot);
            e.setPos(a[0], a[1], a[2]);
            int want = s.powered != null ? e.param() | SylphRules.HIDDEN : e.param() & ~SylphRules.HIDDEN;                              // hidden while the big form is out
            if (want != e.param()) e.setParam(want);
            if (e.life() % 100 == 20 && o instanceof ServerPlayer p) SylphCasting.aura(p);
        }
        e.setYRot(o.yBodyRot);
    }

    @Override
    public void end(MagicPropEntity e, ServerLevel sl) {
        SylphCasting.propEnded(e.ownerId(), SylphRules.variant(e.param()), e.getUUID());
    }
}
