package com.newuniverse.nusmp.spiritlight;

import com.newuniverse.nusmp.prop.MagicPropEntity;
import com.newuniverse.nusmp.spiritcontrol.AbstractSpirit;
import com.newuniverse.nusmp.spiritcontrol.SpiritCatalog;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.Objects;

/**
 * Why: the Spirit of Light on the spirit control plane ({@code spiritcontrol.SpiritControl}). It does exactly what the two old Light keys did, called from the grimoire's spirit pages and the
 * {@code /multiverse spirit} command instead: Summon links the lion cub ({@link SpiritLight#link}), Release unlinks it, and the abilities are the light beam ({@link SpiritLight#fireBeam}), the Glowing pulse
 * ({@link SpiritLight#pulse}) and the seraph form ({@link SpiritLight#togglePower}), each with its own cooldown and caps, each reporting whether it really happened. Registered by
 * {@link SpiritLightBoot}; nothing of its looks, sounds, caps or ledgers changes.
 */
final class SpiritLightControl extends AbstractSpirit {
    SpiritLightControl() { super(SpiritCatalog.LIGHT_SPIRIT); }

    @Override
    protected boolean packageAllows(ServerPlayer p) { return SpiritLight.eligible(p); }

    private static MagicPropEntity body(ServerPlayer p) {
        SpiritLight.State s = SpiritLight.STATES.get(p.getUUID());
        if (s == null) return null;
        MagicPropEntity seraph = SpiritLight.prop(p.serverLevel(), s.seraph);
        return seraph != null ? seraph : SpiritLight.prop(p.serverLevel(), s.cub);
    }

    @Override public boolean isOut(ServerPlayer p) { return body(p) != null; }

    @Override
    public boolean summon(ServerPlayer p) {
        if (body(p) != null) return true;
        return SpiritLight.link(p, SpiritLight.stateOf(p), true);
    }

    @Override
    public void release(ServerPlayer p) {
        SpiritLight.State s = SpiritLight.STATES.get(p.getUUID());
        if (s != null && s.cub != null) SpiritLight.unlink(p, s, true);
    }

    @Override
    public boolean ability(ServerPlayer p, String abilityId, int arg) {
        SpiritLight.State s = SpiritLight.STATES.get(p.getUUID());
        if (s == null || s.cub == null) return false;
        switch (abilityId) {
            case "beam" -> {
                long before = s.nextBeam;
                SpiritLight.fireBeam(p, s);
                return s.nextBeam != before;
            }
            case "pulse" -> {
                long before = s.nextPulse;
                SpiritLight.pulse(p, s);
                return s.nextPulse != before;
            }
            case "power" -> {
                var before = s.seraph;
                SpiritLight.togglePower(p, s);
                return !Objects.equals(before, s.seraph);
            }
            default -> { return false; }
        }
    }

    @Override
    public Vec3 origin(ServerPlayer p) {
        MagicPropEntity e = body(p);
        return e != null ? at(e) : shoulder(p);
    }
}
