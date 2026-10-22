package com.newuniverse.nusmp.gnome;

import com.newuniverse.nusmp.prop.MagicPropEntity;
import com.newuniverse.nusmp.spiritcontrol.AbstractSpirit;
import com.newuniverse.nusmp.spiritcontrol.SpiritCatalog;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Why: the Gnome on the spirit control plane ({@code spiritcontrol.SpiritControl}). It does exactly what the old Gnome key did, called from the grimoire's spirit pages and the
 * {@code /multiverse spirit} command instead: Summon links the stone companion ({@link Gnome#link}), Release unlinks it, and the "giant" ability calls the full form without waiting for a big page (with its own
 * cooldown and caps, the same test the earth pages use). Registered by {@link GnomeBoot}; nothing of its looks, sounds, caps or ledgers changes.
 */
final class GnomeSpirit extends AbstractSpirit {
    GnomeSpirit() { super(SpiritCatalog.GNOME); }

    @Override
    protected boolean packageAllows(ServerPlayer p) { return Gnome.eligible(p); }

    private static MagicPropEntity body(ServerPlayer p) {
        Gnome.State s = Gnome.STATES.get(p.getUUID());
        return s == null ? null : Gnome.prop(p.serverLevel(), s.spirit);
    }

    @Override public boolean isOut(ServerPlayer p) { return body(p) != null; }

    @Override
    public boolean summon(ServerPlayer p) {
        if (body(p) != null) return true;
        return Gnome.link(p, Gnome.state0(p));
    }

    @Override
    public void release(ServerPlayer p) {
        Gnome.State s = Gnome.STATES.get(p.getUUID());
        if (s != null && s.spirit != null) Gnome.unlink(p, s, true);
    }

    @Override
    public boolean ability(ServerPlayer p, String abilityId, int arg) {
        if (!"giant".equals(abilityId)) return false;
        Gnome.State s = Gnome.state0(p);
        long now = p.level().getGameTime();
        if (Gnome.prop(p.serverLevel(), s.full) != null) return false;
        if (!GnomeRules.fullAllowed(now, s.fullReadyAt, Gnome.FULLS.count(p.getUUID()), Gnome.FULLS.total(), GnomeConfig.maxFull())) return false;
        double[] at = GnomeRules.full(p.getX(), p.getY(), p.getZ(), p.yBodyRot);
        return Gnome.summonFull(p, s, now, at);
    }

    @Override
    public Vec3 origin(ServerPlayer p) {
        MagicPropEntity e = body(p);
        return e != null ? at(e) : shoulder(p);
    }
}
