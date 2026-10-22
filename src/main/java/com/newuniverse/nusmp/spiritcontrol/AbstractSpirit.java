package com.newuniverse.nusmp.spiritcontrol;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Set;

/**
 * Why: the plumbing every {@link SpiritHandle} shares: the pure catalogue row it delegates to (id, name, magics, abilities, spells), the spirit's own switch in {@code nusmp-spirit-control-server.toml}
 * and the fallback start point of a spell (the owner's shoulder). A spirit package extends this and fills in the five real methods.
 */
public abstract class AbstractSpirit implements SpiritHandle {
    private final SpiritCatalog.Entry row;

    protected AbstractSpirit(SpiritCatalog.Entry row) { this.row = row; }

    @Override public String id() { return row.id(); }
    @Override public String display() { return row.display(); }
    @Override public Set<String> magics() { return row.magics(); }
    @Override public List<SpiritAbilityDef> abilities() { return row.abilities(); }
    @Override public List<SpiritSpellDef> spells() { return row.spells(); }

    /** The control plane's own switch for this spirit; the package's rule is {@link #packageAllows}. */
    @Override
    public final boolean eligible(ServerPlayer p) { return SpiritControlConfig.spiritOn(id()) && packageAllows(p); }

    /** The spirit package's own rule (its switch, the canon grimoire or the skill it needs). */
    protected abstract boolean packageAllows(ServerPlayer p);

    /** The start point of a spell: a body the spirit has in the world, else the owner's right shoulder. */
    @Override
    public Vec3 origin(ServerPlayer p) { return shoulder(p); }

    protected static Vec3 at(Entity e) { return e == null ? null : e.position().add(0, e.getBbHeight() * 0.6, 0); }

    protected static Vec3 shoulder(ServerPlayer p) {
        double yaw = Math.toRadians(p.yBodyRot);
        return new Vec3(p.getX() - Math.cos(yaw) * 0.45, p.getY() + p.getEyeHeight() - 0.1, p.getZ() - Math.sin(yaw) * 0.45);
    }
}
