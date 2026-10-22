package com.newuniverse.nusmp.gnome;

import com.newuniverse.nusmp.prop.MagicPropEntity;
import com.newuniverse.nusmp.prop.MagicProps;
import com.newuniverse.nusmp.prop.PropKind;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

/**
 * Why: the server half of the Gnome prop. GNOME_1 is the stone companion beside its master (variant 0) and the full-size giant standing ahead of them (variant 1). The props are only bodies: every
 * rule (the link, the caps, the cooldown, the emitter, the sounds) lives in {@link Gnome}; the clients draw them from the synced kind, param and the owner (the target), so a prop needs a position
 * update only every {@link GnomeRules#NUDGE} ticks, just enough to stay inside the owner's tracking range. The giant never moves; it only plays the sink sound when its last second begins.
 */
public final class GnomeProps implements MagicProps.Behavior {
    public static final GnomeProps INSTANCE = new GnomeProps();

    private GnomeProps() {}

    /** Called once by PropRegistry. */
    public static void init() {
        MagicProps.register(PropKind.GNOME_1, INSTANCE);
    }

    @Override
    public void tick(MagicPropEntity e, ServerLevel sl) {
        int variant = GnomeRules.variant(e.param());
        if (variant == GnomeRules.V_FULL) {
            if (e.life() == GnomeRules.FULL_TICKS - GnomeRules.FULL_SINK_TICKS) {                       // the last second begins: it sinks back
                Gnome.playCue(sl, e.ownerId(), "sink", e.getX(), e.getY() + 1.0, e.getZ());
            }
            if (e.life() % GnomeRules.NUDGE == 0 && e.owner() == null) e.expire();                    // its master is gone (logout, dimension): it sinks away at once
            return;
        }
        if (e.life() % GnomeRules.NUDGE != 0) return;
        LivingEntity o = e.owner();
        if (o == null) { e.expire(); return; }
        Gnome.State s = Gnome.stateOf(o.getUUID());
        if (s == null || !e.getUUID().equals(s.spirit)) { e.expire(); return; }                          // an orphan (the link was dropped): gone
        double[] a = GnomeRules.companion(o.getX(), o.getY(), o.getZ(), o.yBodyRot);
        e.setPos(a[0], a[1], a[2]);
        e.setYRot(o.yBodyRot);
        int want = s.full != null ? e.param() | GnomeRules.HIDDEN : e.param() & ~GnomeRules.HIDDEN;      // hidden while the giant stands
        if (want != e.param()) e.setParam(want);
    }

    @Override
    public void end(MagicPropEntity e, ServerLevel sl) {
        Gnome.propEnded(e.ownerId(), GnomeRules.variant(e.param()), e.getUUID());
    }
}
