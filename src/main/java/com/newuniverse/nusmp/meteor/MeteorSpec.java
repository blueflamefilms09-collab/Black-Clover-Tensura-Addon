package com.newuniverse.nusmp.meteor;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * What a caller asks of one meteor ({@link MeteorFall#drop}). Every number is clamped by {@link #clamped()} before the meteor is made, so a caller cannot ask for a monster.
 * <ul>
 *   <li>{@code style}: the look (rock texture, trail colour, default impact picture).</li>
 *   <li>{@code radius}, {@code damage}, {@code damageType}, {@code knockback}: the generic landing, used only when {@code damage} is above 0 and the caster is a player: every foe within
 *       {@code radius} (at most {@link MeteorRules#MAX_TARGETS}) takes {@code damage} times {@link MeteorRules#falloff} of the registry damage type {@code damageType}
 *       ({@code "minecraft:magic"}, {@code "minecraft:in_fire"}, ...; an unknown id reads as magic) and is pushed away. Callers that already have their own damage code leave
 *       {@code damage} at 0 and give {@code onImpact} instead.</li>
 *   <li>{@code size}: the scale of the model (1 = about 1.7 blocks across).</li>
 *   <li>{@code fallTicks}: how long the fall lasts (8 to 60); the landing happens exactly then.</li>
 *   <li>{@code onImpact}: the caller's own landing, the code the ability ran before the meteor existed (its damage, its effects, its picture). It runs on the server at the moment of
 *       landing, once. It is never saved: a meteor in the air when the world closes is gone.</li>
 * </ul>
 * Nothing a meteor does touches a block: no crater, no fire. A caller that always lit fires lights them in its own {@code onImpact}.
 */
public record MeteorSpec(MeteorStyle style, double radius, float damage, String damageType, float size, int fallTicks, double knockback, Impact onImpact) {

    /** The landing of one meteor: {@code at} is the impact point on the ground, {@code caster} the one who dropped it (null when it is gone or was not alive at the landing). */
    @FunctionalInterface
    public interface Impact {
        void land(ServerLevel level, Vec3 at, LivingEntity caster);
    }

    public static final String MAGIC = "minecraft:magic";

    public static MeteorSpec of(MeteorStyle style) { return new MeteorSpec(style == null ? MeteorStyle.FIRE : style, 3.0, 0f, MAGIC, 1.0f, 24, 0.6, null); }

    public MeteorSpec radius(double r) { return new MeteorSpec(style, r, damage, damageType, size, fallTicks, knockback, onImpact); }

    public MeteorSpec damage(float d, String type) { return new MeteorSpec(style, radius, d, type, size, fallTicks, knockback, onImpact); }

    public MeteorSpec size(float s) { return new MeteorSpec(style, radius, damage, damageType, s, fallTicks, knockback, onImpact); }

    public MeteorSpec fall(int ticks) { return new MeteorSpec(style, radius, damage, damageType, size, ticks, knockback, onImpact); }

    public MeteorSpec knockback(double k) { return new MeteorSpec(style, radius, damage, damageType, size, fallTicks, k, onImpact); }

    public MeteorSpec onImpact(Impact i) { return new MeteorSpec(style, radius, damage, damageType, size, fallTicks, knockback, i); }

    /** The same spec with every number inside its limits (see {@link MeteorRules}); the fall is also cut to the server's {@code maxFallTicks}. */
    public MeteorSpec clamped(int serverMaxFall) {
        int fall = Math.min(MeteorRules.clampFall(fallTicks), Math.max(MeteorRules.MIN_FALL, serverMaxFall));
        return new MeteorSpec(style == null ? MeteorStyle.FIRE : style, MeteorRules.clampRadius(radius), MeteorRules.clampDamage(damage),
                damageType == null || damageType.isBlank() ? MAGIC : damageType, MeteorRules.clampSize(size), fall, MeteorRules.clampKnockback(knockback), onImpact);
    }
}
