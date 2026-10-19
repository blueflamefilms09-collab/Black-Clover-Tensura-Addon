package com.newuniverse.nusmp.prop;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * 1.93.0 Lightning Magic: the bodies of the spells. LIGHTNING_1 = the Thunderbird (Thunderbird Cavalry: a flock flies from the caster), LIGHTNING_2 = the Thunder Lance (Thunder Lance, and the
 * lance of the Black Lightning armour). Both are {@link WaterPropFlight} flyers: the damage is the spell's own (LightningBook / WikiSpells); models: tools/gen_lightning_summon_geo.py,
 * painter: client.prop.LightningPropPainter.
 */
public final class LightningProps {
    private LightningProps() {}

    /** Called once by PropRegistry. */
    public static void init() {
        MagicProps.register(PropKind.LIGHTNING_1, WaterPropFlight.INSTANCE);
        MagicProps.register(PropKind.LIGHTNING_2, WaterPropFlight.INSTANCE);
    }

    /** A thunderbird flying along 'dir' at 'speed' blocks per tick. */
    public static MagicPropEntity bird(ServerLevel sl, LivingEntity owner, Vec3 from, Vec3 dir, double speed, int ticks, float scale) {
        Vec3 n = dir.normalize();
        return WaterPropFlight.spawn(sl, PropKind.LIGHTNING_1, owner, from, n, n.scale(speed), ticks, scale, 0);
    }

    /** A lance flying along 'dir' at 'speed' blocks per tick (variant 1 = black lightning). */
    public static MagicPropEntity lance(ServerLevel sl, LivingEntity owner, Vec3 from, Vec3 dir, double speed, int ticks, float scale, int variant) {
        Vec3 n = dir.normalize();
        return WaterPropFlight.spawn(sl, PropKind.LIGHTNING_2, owner, from, n, n.scale(speed), ticks, scale, variant & 15);
    }
}
