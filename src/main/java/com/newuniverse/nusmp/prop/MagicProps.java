package com.newuniverse.nusmp.prop;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.EnumMap;

/**
 * 0.53: the registry and spawner for magic props: real 3D things a spell puts in the world (floating eyes, keys, chains, food,
 * bubbles, crystals, soldiers, shards ...). One entity type ({@link MagicPropEntity}), many {@link PropKind}s; each kind has a
 * server {@link Behavior} (what it does each tick) registered by its attribute's {@code prop.<Name>Props.init()} and a client
 * painter (how it looks) registered by {@code client.prop.<Name>PropPainter.register()}.
 *
 * <pre>
 *   // in a spell:
 *   MagicPropEntity eye = MagicProps.spawn(level, PropKind.EYE_1, pos, yaw, 1.0f, 400, 0, player);
 *   // in EyeProps.init():
 *   MagicProps.register(PropKind.EYE_1, (e, sl) -> { ... e.setPos(...); ... });
 * </pre>
 */
public final class MagicProps {
    private MagicProps() {}

    /** What a prop does. Props are not saved: they vanish with a restart, like every spell effect. */
    public interface Behavior {
        /** Once, right after the prop is added to the world. */
        default void init(MagicPropEntity e, ServerLevel sl) {}

        /** Every server tick while it lives. Move it with e.setPos / e.move; hurt things with the usual damage helpers. */
        void tick(MagicPropEntity e, ServerLevel sl);

        /** Once, when its life runs out or its health reaches 0 (burst, drop things, free what it held). */
        default void end(MagicPropEntity e, ServerLevel sl) {}

        /** Someone hits it (only if hittable): return false to ignore the hit. */
        default boolean hurt(MagicPropEntity e, DamageSource source, float amount) { return true; }
    }

    private static final EnumMap<PropKind, Behavior> BEHAVIORS = new EnumMap<>(PropKind.class);

    public static void register(PropKind kind, Behavior b) { BEHAVIORS.put(kind, b); }
    static Behavior behavior(PropKind kind) { return BEHAVIORS.get(kind); }

    /** Registers every attribute's behaviours (the list is generated: prop.PropRegistry). */
    public static void init() { PropRegistry.registerAll(); }

    /**
     * Spawns a prop.
     * @param yaw    degrees; the client rotates the painter's pose by it
     * @param scale  a size factor for the painter (and the default hitbox)
     * @param life   ticks it lives (it then ends and vanishes)
     * @param param  a free number the spell and the painter share (a variant, a count ...); changeable with e.setParam
     * @param owner  the caster (may be null)
     */
    public static MagicPropEntity spawn(ServerLevel sl, PropKind kind, Vec3 at, float yaw, float scale, int life, int param, Entity owner) {
        MagicPropEntity e = com.newuniverse.nusmp.entity.NUEntities.MAGIC_PROP.get().create(sl);
        if (e == null) return null;
        e.setup(kind, scale, life, param, owner);
        e.moveTo(at.x, at.y, at.z, yaw, 0f);
        sl.addFreshEntity(e);
        Behavior b = behavior(kind);
        if (b != null) b.init(e, sl);
        return e;
    }
}
