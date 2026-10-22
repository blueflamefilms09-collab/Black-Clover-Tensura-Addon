package com.newuniverse.nusmp.sylph;

import com.newuniverse.nusmp.prop.MagicPropEntity;
import com.newuniverse.nusmp.spiritcontrol.AbstractSpirit;
import com.newuniverse.nusmp.spiritcontrol.SpiritCatalog;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Why: Yuno's Sylph on the spirit control plane ({@code spiritcontrol.SpiritControl}). It does exactly what the old Sylph key did, called from the grimoire's spirit pages and the
 * {@code /multiverse spirit} command instead: Summon links the small Sylph ({@link SylphCasting#link}), Release unlinks her, and the "powered" ability calls her powered form without waiting for a big page (with
 * its own cooldown and caps). Registered by {@link SylphBoot}; nothing of her looks, sounds, caps or ledgers changes.
 */
final class SylphSpirit extends AbstractSpirit {
    SylphSpirit() { super(SpiritCatalog.SYLPH); }

    @Override
    protected boolean packageAllows(ServerPlayer p) { return SylphCasting.applies(p); }

    private static MagicPropEntity body(ServerPlayer p) {
        SylphCasting.State s = SylphCasting.STATES.get(p.getUUID());
        return s == null ? null : SylphCasting.prop(p.serverLevel(), s.spirit);
    }

    @Override public boolean isOut(ServerPlayer p) { return body(p) != null; }

    @Override
    public boolean summon(ServerPlayer p) {
        if (body(p) != null) return true;
        return SylphCasting.link(p, SylphCasting.stateOf(p));
    }

    @Override
    public void release(ServerPlayer p) {
        SylphCasting.State s = SylphCasting.STATES.get(p.getUUID());
        if (s != null && s.spirit != null) SylphCasting.unlink(p, s, true);
    }

    @Override
    public boolean ability(ServerPlayer p, String abilityId, int arg) {
        if (!"powered".equals(abilityId)) return false;
        return SylphCasting.summonPowered(p, SylphCasting.stateOf(p), p.level().getGameTime(), false);
    }

    @Override
    public Vec3 origin(ServerPlayer p) {
        MagicPropEntity e = body(p);
        return e != null ? at(e) : shoulder(p);
    }
}
