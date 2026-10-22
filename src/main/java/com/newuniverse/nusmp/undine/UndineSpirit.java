package com.newuniverse.nusmp.undine;

import com.newuniverse.nusmp.book.CharacterSpells;
import com.newuniverse.nusmp.grimoire.CanonBook;
import com.newuniverse.nusmp.prop.MagicPropEntity;
import com.newuniverse.nusmp.spiritcontrol.AbstractSpirit;
import com.newuniverse.nusmp.spiritcontrol.SpiritCatalog;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Why: Noelle's Undine on the spirit control plane ({@code spiritcontrol.SpiritControl}). It does exactly what the old Undine key did, called from the grimoire's spirit pages and the
 * {@code /multiverse spirit} command instead: Summon links the companion ({@link UndineCasting#link}), Release unlinks her, and the "full" ability calls her full form without waiting for a big page (with
 * its own cooldown and caps). Registered by {@link UndineBoot}; nothing of her looks, sounds, caps or ledgers changes.
 */
final class UndineSpirit extends AbstractSpirit {
    UndineSpirit() { super(SpiritCatalog.UNDINE); }

    @Override
    protected boolean packageAllows(ServerPlayer p) { return UndineConfig.enabled() && CharacterSpells.characterOf(p) == CanonBook.NOELLE; }

    private static MagicPropEntity body(ServerPlayer p) {
        UndineCasting.State s = UndineCasting.STATES.get(p.getUUID());
        return s == null ? null : UndineCasting.active(p.serverLevel(), s);
    }

    @Override public boolean isOut(ServerPlayer p) { return body(p) != null; }

    @Override
    public boolean summon(ServerPlayer p) {
        UndineCasting.State s = UndineCasting.stateOf(p);
        if (UndineCasting.prop(p.serverLevel(), s.spirit) != null) return true;
        return UndineCasting.link(p, s);
    }

    @Override
    public void release(ServerPlayer p) {
        UndineCasting.State s = UndineCasting.STATES.get(p.getUUID());
        if (s != null && s.spirit != null) UndineCasting.unlink(p, s, true);
    }

    @Override
    public boolean ability(ServerPlayer p, String abilityId, int arg) {
        if (!"full".equals(abilityId)) return false;
        return UndineCasting.summonFull(p, UndineCasting.stateOf(p), p.level().getGameTime());
    }

    @Override
    public Vec3 origin(ServerPlayer p) {
        MagicPropEntity e = body(p);
        return e != null ? at(e) : shoulder(p);
    }
}
