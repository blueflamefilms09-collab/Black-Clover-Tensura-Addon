package com.newuniverse.nusmp.firecast;

import com.newuniverse.nusmp.book.CharacterSpells;
import com.newuniverse.nusmp.grimoire.CanonBook;
import com.newuniverse.nusmp.prop.MagicPropEntity;
import com.newuniverse.nusmp.spiritcontrol.AbstractSpirit;
import com.newuniverse.nusmp.spiritcontrol.SpiritCatalog;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Why: Fuegoleon's Salamander on the spirit control plane ({@code spiritcontrol.SpiritControl}). It does exactly what the old Salamander key did, called from the grimoire's spirit pages and the
 * {@code /multiverse spirit} command instead: Summon links the small Salamander ({@link Salamander#link}), Release unlinks it, and the "dragon" ability calls the big form without waiting for a big page (with
 * its own cooldown and caps). Registered by {@link FireCastBoot}; nothing of the Salamander's looks, sounds, caps or ledgers changes.
 */
final class SalamanderSpirit extends AbstractSpirit {
    SalamanderSpirit() { super(SpiritCatalog.SALAMANDER); }

    @Override
    protected boolean packageAllows(ServerPlayer p) { return FireCastConfig.enabled() && CharacterSpells.characterOf(p) == CanonBook.FUEGOLEON; }

    private static MagicPropEntity body(ServerPlayer p) {
        FireCasting.State s = FireCasting.STATES.get(p.getUUID());
        return s == null ? null : FireCasting.prop(p.serverLevel(), s.spirit);
    }

    @Override public boolean isOut(ServerPlayer p) { return body(p) != null; }

    @Override
    public boolean summon(ServerPlayer p) {
        FireCasting.State s = FireCasting.stateOf(p);
        if (body(p) != null) return true;
        return Salamander.link(p, s);
    }

    @Override
    public void release(ServerPlayer p) {
        FireCasting.State s = FireCasting.STATES.get(p.getUUID());
        if (s != null && s.spirit != null) Salamander.unlink(p, s, true);
    }

    @Override
    public boolean ability(ServerPlayer p, String abilityId, int arg) {
        if (!"dragon".equals(abilityId)) return false;
        return Salamander.summonDragon(p, FireCasting.stateOf(p), p.level().getGameTime());
    }

    @Override
    public Vec3 origin(ServerPlayer p) {
        MagicPropEntity e = body(p);
        return e != null ? at(e) : shoulder(p);
    }
}
