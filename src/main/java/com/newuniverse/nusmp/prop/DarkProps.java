package com.newuniverse.nusmp.prop;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * 1.93.0 Dark Magic: the bodies of the spells. DARK_1 = the Black Blade (variant 0, follows the caster, hanging in front of them: Dark Cloaked Black Blade) and the Crescent (variant 1,
 * flies flat along the ground: Dark Cloaked Avidya Slash / Wild Slash); DARK_2 = the Black Moon (variant 0, hangs where it was cast) and the Black Hole (variant 1). All are
 * {@link WaterPropFlight} bodies: the damage is the spell's own (CanonSpells / DarkBook); models: tools/gen_dark_summon_geo.py, painter: client.prop.DarkPropPainter.
 */
public final class DarkProps {
    private DarkProps() {}

    /** Called once by PropRegistry. */
    public static void init() {
        MagicProps.register(PropKind.DARK_1, WaterPropFlight.INSTANCE);
        MagicProps.register(PropKind.DARK_2, WaterPropFlight.INSTANCE);
    }

    /** The Black Blade in front of the caster for 'ticks' ticks; 'slash' plays the chopping clip instead of the idle one (variant 2). */
    public static MagicPropEntity blade(ServerLevel sl, LivingEntity owner, int ticks, float scale, boolean slash) {
        Vec3 look = owner.getViewVector(1f);
        return WaterPropFlight.spawn(sl, PropKind.DARK_1, owner, owner.getEyePosition().add(look.scale(2.0)), look, Vec3.ZERO, ticks, scale, WaterPropFlight.FOLLOW | (slash ? 2 : 0));
    }

    /** A crescent flying flat along 'dir' (horizontal) at 'speed' blocks per tick. */
    public static MagicPropEntity crescent(ServerLevel sl, LivingEntity owner, Vec3 from, Vec3 dir, double speed, int ticks, float scale) {
        Vec3 flat = new Vec3(dir.x, 0, dir.z);
        Vec3 n = (flat.lengthSqr() < 1e-6 ? new Vec3(0, 0, 1) : flat).normalize();
        return WaterPropFlight.spawn(sl, PropKind.DARK_1, owner, from, n, n.scale(speed), ticks, scale, 1);
    }

    /** The Black Moon hanging at 'at'. */
    public static MagicPropEntity moon(ServerLevel sl, LivingEntity owner, Vec3 at, int ticks, float scale) {
        return WaterPropFlight.spawn(sl, PropKind.DARK_2, owner, at, Vec3.ZERO, Vec3.ZERO, ticks, scale, WaterPropFlight.HOLD);
    }

    /** The Black Hole at 'at'. */
    public static MagicPropEntity hole(ServerLevel sl, LivingEntity owner, Vec3 at, int ticks, float scale) {
        return WaterPropFlight.spawn(sl, PropKind.DARK_2, owner, at, Vec3.ZERO, Vec3.ZERO, ticks, scale, WaterPropFlight.HOLD | 1);
    }
}
