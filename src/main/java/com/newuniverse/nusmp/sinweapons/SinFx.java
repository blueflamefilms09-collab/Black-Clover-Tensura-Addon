package com.newuniverse.nusmp.sinweapons;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Why: the one place the five Berserk weapons send their themed effects (the one shape {@code SIN_WEAPON}, drawn by {@code vfx.client.layer.SinWeaponLayer}, docs/sin_weapons_abilities.md). Each call is ONE
 * effect packet through {@link VfxSpawn} (the per-tick effect budget, players within 96 blocks only). A use sends at most one cast picture and {@link SinRules#HIT_FX_MAX} small hit flashes; a plain hit one
 * flash and at most one proc. Never per tick, never more than the caps of the technique. With the server setting {@code themedVfx} off nothing here is called and the old puff is sent instead.
 */
final class SinFx {
    private SinFx() {}

    /** One picture of {@code style} (see {@link SinVfx}) of weapon {@code k}, from {@code from} to {@code to}. */
    static void send(ServerLevel level, SinRules.Kind k, int style, Vec3 from, Vec3 to) {
        VfxSpawn.send(level, VfxShape.SIN_WEAPON, from, to, 0xFF000000 | k.rgb, SinVfx.duration(style), SinVfx.power(k, style));
    }

    /**
     * The cast picture of a technique: an area one stands on the wielder's feet; a line one leaves the hand and ends {@code reach} blocks along the look line (the layer reads direction and length from the two points).
     */
    static void cast(ServerLevel level, ServerPlayer p, SinRules.Kind k, int style, boolean area, double reach) {
        if (area) {
            Vec3 feet = p.position().add(0.0, 0.05, 0.0);
            send(level, k, style, feet, feet);
        } else {
            send(level, k, style, hand(p), p.getEyePosition().add(p.getLookAngle().scale(reach)));
        }
    }

    /** Where the wielder's hand is: a little below the eyes, half a block forward. */
    static Vec3 hand(ServerPlayer p) { return p.getEyePosition().add(0.0, -0.35, 0.0).add(p.getLookAngle().scale(0.6)); }

    static Vec3 chest(Entity e) { return e.position().add(0.0, e.getBbHeight() * 0.6, 0.0); }
}
