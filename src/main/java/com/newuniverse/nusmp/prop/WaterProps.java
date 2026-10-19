package com.newuniverse.nusmp.prop;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * 1.93.0 Water Magic: the bodies of the spells. WATER_1 = the Sea Dragon (Sea Dragon's Roar: it flies from the caster to the target), WATER_2 = the Aqua Javelin (variant 0, a lance of
 * pressurised water) and the Sea Dragon's Tide Lance of the Valkyrie armour (variant 1, the same body drawn bigger and bluer). Both are {@link WaterPropFlight} flyers: the damage is the
 * spell's own (WaterBook); models: tools/gen_water_summon_geo.py, painter: client.prop.WaterPropPainter.
 */
public final class WaterProps {
    private WaterProps() {}

    /** Called once by PropRegistry. */
    public static void init() {
        MagicProps.register(PropKind.WATER_1, WaterPropFlight.INSTANCE);
        MagicProps.register(PropKind.WATER_2, WaterPropFlight.INSTANCE);
        WaterRebuildSelfTests.register();                 // the CI server smoke test runs magic.water_rebuild / dark_rebuild / lightning_rebuild / flame_rebuild
    }

    /** The Sea Dragon flying from 'from' to 'to' in 'flight' ticks (it lives a little longer so it can fade). */
    public static MagicPropEntity dragon(ServerLevel sl, LivingEntity owner, Vec3 from, Vec3 to, int flight, float scale) {
        Vec3 d = to.subtract(from);
        int f = Math.max(6, flight);
        return WaterPropFlight.spawn(sl, PropKind.WATER_1, owner, from, d, d.scale(1.0 / f), f + 10, scale, 0);
    }

    /** A javelin / tide lance flying along 'dir' at 'speed' blocks per tick. */
    public static MagicPropEntity javelin(ServerLevel sl, LivingEntity owner, Vec3 from, Vec3 dir, double speed, int ticks, float scale, int variant) {
        Vec3 n = dir.normalize();
        return WaterPropFlight.spawn(sl, PropKind.WATER_2, owner, from, n, n.scale(speed), ticks, scale, variant & 15);
    }
}
