package com.newuniverse.nusmp.spiritdark;

import com.newuniverse.nusmp.prop.MagicPropEntity;
import com.newuniverse.nusmp.prop.MagicProps;
import com.newuniverse.nusmp.prop.PropKind;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

/**
 * Why: the server half of the Spirit of Darkness prop. SPIRITDARK_1 is the shadow-wisp companion (variant 0: above the owner's left shoulder) and the powered demon (variant 1: behind the owner). The
 * prop is only a body: every rule (strikes, caps, Shadow Step) lives in {@link SpiritDark}; the clients draw it from the synced kind, the param word ({@link SpiritDarkRules}: form, pose, face) and the
 * owner (the target), placing it from the owner's own interpolated body, so a prop needs a position update only every {@link #NUDGE} ticks, just enough to stay inside the owner's tracking range.
 * A prop whose link is gone ends itself, except one that is playing its farewell, which finishes it and then ends.
 */
public final class SpiritDarkProps implements MagicProps.Behavior {
    public static final SpiritDarkProps INSTANCE = new SpiritDarkProps();
    /** Ticks between two looks at a prop (the painters place it from the owner every frame, this only keeps the entity near its owner). */
    static final int NUDGE = 5;

    private SpiritDarkProps() {}

    /** Called once by PropRegistry. */
    public static void init() { MagicProps.register(PropKind.SPIRITDARK_1, INSTANCE); }

    @Override
    public void tick(MagicPropEntity e, ServerLevel sl) {
        if (e.life() % NUDGE != 0) return;
        int param = e.param(), variant = SpiritDarkRules.variant(param);
        long now = sl.getGameTime();
        if (SpiritDarkRules.pose(param) == DarkMoodRules.Pose.FAREWELL && SpiritDarkRules.stamp(param) != 0) {          // saying goodbye: stays where it is until the clip has run, then ends
            if (SpiritDarkRules.since(param, now) > DarkMoodRules.Pose.FAREWELL.ticks + 4) e.expire();
            return;
        }
        LivingEntity o = e.owner();
        if (o == null) { e.expire(); return; }
        SpiritDark.State s = SpiritDark.STATES.get(o.getUUID());
        boolean mine = s != null && e.getUUID().equals(variant == SpiritDarkRules.V_POWERED ? s.powered : s.companion);
        if (!mine) { e.expire(); return; }                                                                            // an orphan (the link was dropped): gone
        double[] a = variant == SpiritDarkRules.V_POWERED ? SpiritDarkRules.powered(o.getX(), o.getY(), o.getZ(), o.yBodyRot) : SpiritDarkRules.companion(o.getX(), o.getY(), o.getZ(), o.yBodyRot);
        e.setPos(a[0], a[1], a[2]);
        e.setYRot(o.yBodyRot);
    }

    @Override
    public void end(MagicPropEntity e, ServerLevel sl) {
        SpiritDark.propEnded(e.ownerId(), SpiritDarkRules.variant(e.param()), e.getUUID());
    }
}
