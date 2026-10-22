package com.newuniverse.nusmp.spiritspace;

import com.newuniverse.nusmp.prop.MagicPropEntity;
import com.newuniverse.nusmp.prop.MagicProps;
import com.newuniverse.nusmp.prop.PropKind;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

/**
 * Why: the server half of the Spirit of Space's two prop kinds. SPIRITSPACE_1 is the spirit (variant 0: the orb that circles its owner; variant 1: the powered wisp above and behind) and
 * SPIRITSPACE_2 is one ring of a portal (variants 2 and 3, they only hang where they were put). The props are only bodies: every rule (caps, cooldowns, the portal) lives in {@link SpiritSpace}; the
 * clients draw them from the synced kind, param and owner (the target), placing the orb and the wisp from the owner's own interpolated body each frame, so a spirit needs a position update only
 * every {@link #NUDGE} ticks, just enough to stay inside the owner's tracking range.
 */
public final class SpiritSpaceProps implements MagicProps.Behavior {
    public static final SpiritSpaceProps INSTANCE = new SpiritSpaceProps();
    /** Ticks between two position updates of a following prop. */
    static final int NUDGE = 5;
    private static final double[] AT = new double[3];

    private SpiritSpaceProps() {}

    /** Called once by PropRegistry. */
    public static void init() {
        MagicProps.register(PropKind.SPIRITSPACE_1, INSTANCE);
        MagicProps.register(PropKind.SPIRITSPACE_2, INSTANCE);
    }

    @Override
    public void init(MagicPropEntity e, ServerLevel sl) {
        e.setNoGravity(true);
        int v = SpiritSpaceRules.variant(e.param());
        if (v == SpiritSpaceRules.V_WISP) e.setSize(1.2f, 0.9f);
        else if (v == SpiritSpaceRules.V_ORB) e.setSize(0.6f, 0.6f);
        else e.setSize(1.6f, 2.4f);
    }

    @Override
    public void tick(MagicPropEntity e, ServerLevel sl) {
        int variant = SpiritSpaceRules.variant(e.param());
        if (variant >= SpiritSpaceRules.V_PORTAL_IN || e.life() % NUDGE != 0) return;        // the rings hang where they were put; the others are looked after every few ticks
        LivingEntity o = e.owner();
        if (o == null) { e.expire(); return; }
        if (SpaceMoodRules.moodOf(e.param()) == SpaceMoodRules.Mood.FAREWELL) return;                // saying goodbye: the farewell ends it
        if (!SpiritSpace.isCurrent(o.getUUID(), variant, e.getUUID())) { e.expire(); return; }      // an orphan (the link was dropped): gone
        if (variant == SpiritSpaceRules.V_ORB) SpiritSpaceRules.orbitTo(AT, o.getX(), o.getY(), o.getZ(), o.yBodyRot, e.tickCount);
        else SpiritSpaceRules.wispTo(AT, o.getX(), o.getY(), o.getZ(), o.yBodyRot);
        e.setPos(AT[0], AT[1], AT[2]);
        e.setYRot(o.yBodyRot);
    }

    @Override
    public void end(MagicPropEntity e, ServerLevel sl) {
        SpiritSpace.propEnded(e.ownerId(), SpiritSpaceRules.variant(e.param()), e.getUUID());
    }
}
