package com.newuniverse.nusmp.spiritdark;

import com.newuniverse.nusmp.prop.MagicPropEntity;
import com.newuniverse.nusmp.spiritcontrol.AbstractSpirit;
import com.newuniverse.nusmp.spiritcontrol.SpiritCatalog;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.Objects;

/**
 * Why: the Spirit of Darkness on the spirit control plane ({@code spiritcontrol.SpiritControl}). It does exactly what the three old Darkness keys did, called from the grimoire's spirit pages and the
 * {@code /multiverse spirit} command instead: Summon links the shadow wisp ({@link SpiritDark#link}), Release unlinks it, and the abilities are Shadow Step and the demon form, each through the spirit's own
 * entry ({@link SpiritDark#onKey}) with its own cooldown, caps and checks, each reporting whether it really happened. Registered by {@link SpiritDarkBoot}; nothing of its looks, sounds, caps or ledgers changes.
 */
final class SpiritDarkControl extends AbstractSpirit {
    SpiritDarkControl() { super(SpiritCatalog.DARK_SPIRIT); }

    @Override
    protected boolean packageAllows(ServerPlayer p) { return SpiritDark.eligible(p); }

    private static MagicPropEntity body(ServerPlayer p) {
        SpiritDark.State s = SpiritDark.STATES.get(p.getUUID());
        if (s == null) return null;
        MagicPropEntity powered = SpiritDark.prop(p.serverLevel(), s.powered);
        return powered != null ? powered : SpiritDark.prop(p.serverLevel(), s.companion);
    }

    @Override public boolean isOut(ServerPlayer p) { return body(p) != null; }

    @Override
    public boolean summon(ServerPlayer p) {
        if (body(p) != null) return true;
        return SpiritDark.link(p, SpiritDark.stateOf(p));
    }

    @Override
    public void release(ServerPlayer p) {
        SpiritDark.State s = SpiritDark.STATES.get(p.getUUID());
        if (s != null && s.companion != null) SpiritDark.unlink(p, s, true);
    }

    @Override
    public boolean ability(ServerPlayer p, String abilityId, int arg) {
        SpiritDark.State s = SpiritDark.STATES.get(p.getUUID());
        if (s == null || s.companion == null) return false;
        switch (abilityId) {
            case "step" -> {
                long before = s.stepReadyAt;
                SpiritDark.onKey(p, SpiritDarkRules.ACT_STEP);
                return s.stepReadyAt != before;
            }
            case "power" -> {
                var before = s.powered;
                SpiritDark.onKey(p, SpiritDarkRules.ACT_POWER);
                return !Objects.equals(before, s.powered);
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
