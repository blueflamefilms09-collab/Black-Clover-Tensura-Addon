package com.newuniverse.nusmp.prop;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * 1.93.0 Flame Magic: the bodies of the spells. FLAME_1 = the Flame Lion (Leo Rugiens: it bounds from the caster to the aim point; its feet follow the line the spell computed),
 * FLAME_2 = the Phoenix (it flies along a line, bobbing, with a trail of fire). Both are {@link WaterPropFlight} flyers: the damage is the spell's own (FireBook); models:
 * tools/gen_flame_summon_geo.py, painter: client.prop.FlamePropPainter.
 */
public final class FlameProps {
    private FlameProps() {}

    /** Called once by PropRegistry. */
    public static void init() {
        MagicProps.register(PropKind.FLAME_1, WaterPropFlight.INSTANCE);
        MagicProps.register(PropKind.FLAME_2, WaterPropFlight.INSTANCE);
    }

    /** The lion running from 'from' (its feet) to 'to' in 'run' ticks, then standing roaring for 'linger' more ticks. */
    public static MagicPropEntity lion(ServerLevel sl, LivingEntity owner, Vec3 from, Vec3 to, int run, int linger, float scale) {
        Vec3 d = to.subtract(from);
        int r = Math.max(4, run);
        return WaterPropFlight.spawn(sl, PropKind.FLAME_1, owner, from, d, d.scale(1.0 / r), r + linger + 8, scale, WaterPropFlight.stopAfter(r));
    }

    /** A phoenix flying along 'dir' at 'speed' blocks per tick. */
    public static MagicPropEntity phoenix(ServerLevel sl, LivingEntity owner, Vec3 from, Vec3 dir, double speed, int ticks, float scale) {
        Vec3 n = dir.normalize();
        return WaterPropFlight.spawn(sl, PropKind.FLAME_2, owner, from, n, n.scale(speed), ticks, scale, WaterPropFlight.BOB);
    }
}
