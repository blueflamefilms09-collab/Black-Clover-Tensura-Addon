package com.newuniverse.nusmp.spiritspace;

import com.newuniverse.nusmp.prop.MagicPropEntity;
import com.newuniverse.nusmp.spiritcontrol.AbstractSpirit;
import com.newuniverse.nusmp.spiritcontrol.SpiritCatalog;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Why: the Spirit of Space on the spirit control plane ({@code spiritcontrol.SpiritControl}). It does exactly what the three old Space keys did, called from the grimoire's spirit pages and the
 * {@code /multiverse spirit} command instead: Summon links the spirit ({@link SpiritSpace#link}), Release unlinks it, and the abilities are the portal, the gravity pulse, the spatial cut and marking a waypoint
 * ({@link SpiritSpaceRules#A_PORTAL} ...). Each ability is the spirit's own method, which checks and charges its own mana and cooldown and says whether it happened; the waypoint slot (0-based) is validated
 * against the configured number. Waypoints can still be named with the typed {@code /spiritspace} command. Registered by {@link SpiritSpaceBoot}; nothing of its looks, sounds, caps or ledgers changes.
 */
final class SpiritSpaceControl extends AbstractSpirit {
    SpiritSpaceControl() { super(SpiritCatalog.SPACE_SPIRIT); }

    @Override
    protected boolean packageAllows(ServerPlayer p) { return SpiritSpace.allowed(p); }

    private static SpiritSpace.State linked(ServerPlayer p) {
        SpiritSpace.State s = SpiritSpace.STATES.get(p.getUUID());
        return s != null && s.linked ? s : null;
    }

    @Override public boolean isOut(ServerPlayer p) { return linked(p) != null; }

    @Override
    public boolean summon(ServerPlayer p) {
        if (linked(p) != null) return true;
        return SpiritSpace.link(p, SpiritSpace.stateOf(p));
    }

    @Override
    public void release(ServerPlayer p) {
        SpiritSpace.State s = linked(p);
        if (s != null) SpiritSpace.unlink(p, s, true);
    }

    @Override public int slots() { return SpiritSpaceConfig.waypoints(); }

    @Override
    public boolean ability(ServerPlayer p, String abilityId, int arg) {
        SpiritSpace.State s = linked(p);
        if (s == null || !SpiritSpace.allowed(p) || !SpiritSpaceRules.validSlot(arg, SpiritSpaceConfig.waypoints())) return false;
        return switch (abilityId) {
            case "portal" -> SpiritSpace.beginPortal(p, s, arg);
            case "gravity" -> SpiritSpace.gravity(p, s);
            case "cut" -> SpiritSpace.cut(p, s);
            case "set" -> SpiritSpace.setWaypoint(p, s, arg, null);
            default -> false;
        };
    }

    @Override
    public Vec3 origin(ServerPlayer p) {
        SpiritSpace.State s = linked(p);
        MagicPropEntity e = s == null ? null : SpiritSpace.prop(p.serverLevel(), s.spirit);
        return e != null ? at(e) : shoulder(p);
    }
}
