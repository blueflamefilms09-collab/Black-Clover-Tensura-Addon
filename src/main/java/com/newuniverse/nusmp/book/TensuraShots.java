package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.item.MagicGear;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.entity.projectile.TensuraFlyingProjectile;
import io.github.manasmods.tensura.entity.projectile.magic.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/**
 * Fires Tensura's own magic projectiles (the same entities, models and impacts Tensura's
 * Fire Bolt / Water Blade / Wind Blade use), owned by the grimoire skill so Tensura resistances,
 * nullification and kill credit apply exactly as for Tensura magic.
 */
public final class TensuraShots {
    private TensuraShots() {}

    public enum Shot {
        FIRE_BALL, FIRE_BOLT, FIRE_LANCE, FLAME_SPHERE, HEAT_SPHERE, MAGMA_SHOT, BLACK_FLAME_BALL, PLASMA_BALL,
        WATER_BALL, WATER_BLADE, FROST_BALL, ICE_LANCE, STEAM_BALL, POISON_BALL, POISON_CUTTER, ACID_BALL,
        WIND_BLADE, WIND_SPHERE, WIND_TORNADO, LIGHTNING_LANCE, LIGHTNING_SPHERE, THUNDER_LANCE, THUNDER_SPHERE,
        GRAVITY_SPHERE, SPACE_CUT, DIMENSION_CUT, SPATIAL_ARROW, MUD_SHOT, OBSIDIAN_SHOT, BOULDER_SHOT, AURA_SLASH
    }

    private static TensuraFlyingProjectile create(Shot s, Level l, LivingEntity o) {
        return switch (s) {
            case FIRE_BALL -> new FireBallProjectile(l, o);
            case FIRE_BOLT -> new FireBoltProjectile(l, o);
            case FIRE_LANCE -> new FireLanceProjectile(l, o);
            case FLAME_SPHERE -> new FlameSphereProjectile(l, o);
            case HEAT_SPHERE -> new HeatSphereProjectile(l, o);
            case MAGMA_SHOT -> new MagmaShotProjectile(l, o);
            case BLACK_FLAME_BALL -> new BlackFlameBallProjectile(l, o);
            case PLASMA_BALL -> new PlasmaBallProjectile(l, o);
            case WATER_BALL -> new WaterBallProjectile(l, o);
            case WATER_BLADE -> new WaterBladeProjectile(l, o);
            case FROST_BALL -> new FrostBallProjectile(l, o);
            case ICE_LANCE -> new IceLanceProjectile(l, o);
            case STEAM_BALL -> new SteamBallProjectile(l, o);
            case POISON_BALL -> new PoisonBallProjectile(l, o);
            case POISON_CUTTER -> new PoisonCutterProjectile(l, o);
            case ACID_BALL -> new AcidBallProjectile(l, o);
            case WIND_BLADE -> new WindBladeProjectile(l, o);
            case WIND_SPHERE -> new WindSphereProjectile(l, o);
            case WIND_TORNADO -> new WindTornadoProjectile(l, o);
            case LIGHTNING_LANCE -> new LightningLanceProjectile(l, o);
            case LIGHTNING_SPHERE -> new LightningSphereProjectile(l, o);
            case THUNDER_LANCE -> new ThunderLanceProjectile(l, o);
            case THUNDER_SPHERE -> new ThunderSphereProjectile(l, o);
            case GRAVITY_SPHERE -> new GravitySphereProjectile(l, o);
            case SPACE_CUT -> new SpaceCutProjectile(l, o);
            case DIMENSION_CUT -> new DimensionCutProjectile(l, o);
            case SPATIAL_ARROW -> new SpatialArrowProjectile(l, o);
            case MUD_SHOT -> new MudShotProjectile(l, o);
            case OBSIDIAN_SHOT -> new ObsidianShotProjectile(l, o);
            case BOULDER_SHOT -> new BoulderShotProjectile(l, o);
            case AURA_SLASH -> new AuraSlashProjectile(l, o);
        };
    }

    /** Fires one Tensura projectile from the caster's eyes along their look, like Tensura's Fire Bolt. */
    public static boolean fire(GrimoireBook book, ManasSkillInstance i, ServerPlayer p, int mode, Shot shot,
                               float damage, float speed, float knock, int burnTicks) {
        TensuraFlyingProjectile pr = create(shot, p.level(), p);
        float dmg = damage * (float) GrimoireBook.cover(i).damage * (float) MagicGear.damageMult(p, book.magic)
                * (GrimoireBook.inUnion(i, p) ? 1.1f : 1f) * GrimoireBook.size(i, p);
        pr.setSkill(p, i, book, mode);
        pr.setDamage(Math.min(dmg, 28f));          // balance law: never above 14 hearts
        if (knock > 0) pr.setKnockForce(knock);
        if (burnTicks > 0) pr.setBurnTicks(burnTicks);
        pr.setSpeed(speed);
        pr.setNoGravity(true);
        pr.setPos(p.getEyePosition().add(p.getLookAngle().scale(0.6)));
        pr.shootFromRot(p.getLookAngle());
        p.level().addFreshEntity(pr);
        book.castCircle(p, 0.8f);
        return true;
    }

    /** A page that fires a Tensura projectile. */
    public static BookPage.Cast shot(Shot shot, float damage, float speed, float knock, int burnTicks) {
        return (b, i, p, mode) -> fire(b, i, p, mode, shot, damage, speed, knock, burnTicks);
    }
}
