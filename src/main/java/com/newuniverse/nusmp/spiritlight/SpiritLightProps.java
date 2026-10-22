package com.newuniverse.nusmp.spiritlight;

import com.newuniverse.nusmp.prop.MagicPropEntity;
import com.newuniverse.nusmp.prop.MagicProps;
import com.newuniverse.nusmp.prop.PropKind;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

/**
 * Why: the server half of the two props of the Spirit of Light. SPIRIT_LIGHT_1 is the lion cub (variant 0, beside the owner's shoulder) and the seraph (variant 1, behind the owner); SPIRIT_LIGHT_2 is
 * a beam (variant 2) and a pulse ring (variant 3), which only live a few ticks and need nothing from the server but to end. The props are only bodies: every rule (damage, caps, cooldowns) lives in
 * {@link SpiritLight}; the clients draw them from the synced kind, param and the owner (the target), placing a form from its owner's own interpolated body each frame, so a form needs a position update
 * only every {@link #NUDGE} ticks, just enough to stay inside the owner's tracking range.
 */
public final class SpiritLightProps implements MagicProps.Behavior {
    public static final SpiritLightProps INSTANCE = new SpiritLightProps();
    /** Ticks between two position updates of a following form. */
    static final int NUDGE = 5;

    private SpiritLightProps() {}

    /** Called once by PropRegistry. */
    public static void init() {
        MagicProps.register(PropKind.SPIRIT_LIGHT_1, INSTANCE);
        MagicProps.register(PropKind.SPIRIT_LIGHT_2, INSTANCE);
    }

    @Override
    public void tick(MagicPropEntity e, ServerLevel sl) {
        int variant = SpiritLightRules.variant(e.param());
        if ((variant != SpiritLightRules.V_CUB && variant != SpiritLightRules.V_SERAPH) || e.life() % NUDGE != 0) return;        // beams and rings only live out their few ticks
        LivingEntity o = e.owner();
        if (o == null) { e.expire(); return; }
        SpiritLight.State s = SpiritLight.STATES.get(o.getUUID());
        boolean mine = s != null && e.getUUID().equals(variant == SpiritLightRules.V_CUB ? s.cub : s.seraph);
        if (!mine) { e.expire(); return; }                                                                   // an orphan (the link was dropped): gone
        double[] a = variant == SpiritLightRules.V_CUB ? SpiritLightRules.companion(o.getX(), o.getY(), o.getZ(), o.yBodyRot) : SpiritLightRules.seraph(o.getX(), o.getY(), o.getZ(), o.yBodyRot);
        e.setPos(a[0], a[1], a[2]);
        e.setYRot(o.yBodyRot);
    }

    @Override
    public void end(MagicPropEntity e, ServerLevel sl) {
        SpiritLight.propEnded(e.ownerId(), SpiritLightRules.variant(e.param()), e.getUUID());
    }
}
