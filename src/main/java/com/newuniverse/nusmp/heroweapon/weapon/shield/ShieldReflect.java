package com.newuniverse.nusmp.heroweapon.weapon.shield;

import com.newuniverse.nusmp.core.magic.profile.SafeCast;
import com.newuniverse.nusmp.heroweapon.HeroConfig;
import com.newuniverse.nusmp.heroweapon.HeroStyle;
import com.newuniverse.nusmp.heroweapon.HeroWeaponItem;
import com.newuniverse.nusmp.heroweapon.bond.HeroBond;
import com.newuniverse.nusmp.heroweapon.bond.HeroBonds;
import com.newuniverse.nusmp.heroweapon.weapon.vassalkit.Deflect;
import com.newuniverse.nusmp.heroweapon.weapon.vassalkit.Foes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;

/**
 * Why: the owner wants the hero Shield to work like a normal shield, "protecting, reflecting projectiles and all the sorts". The blocking itself is the game's (the item
 * answers UseAnim.BLOCK); this adds the reflecting: a dangerous shot that is about to hit a player who holds a hero Shield up, from the front, is turned back at its
 * shooter instead of landing (the same call the Mirror uses), and becomes the wielder's shot. Server only, one cheap check per projectile impact on a player, no scan, no
 * packet of its own (the entity's movement update is the game's). Behind {@code heroweapon.shieldReflects}.
 */
public final class ShieldReflect {
    private ShieldReflect() {}

    private static boolean booted;

    public static synchronized void init() {
        if (booted) return;
        booted = true;
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, ShieldReflect::onImpact);
    }

    /** Pure: true when a shot at {@code (dx, dz)} from the wielder (horizontal offset of the shot from the wielder) is in front of a wielder looking along {@code (lx, lz)}: within 90 degrees of the look. */
    public static boolean frontal(double lx, double lz, double dx, double dz) {
        double len = Math.hypot(lx, lz), d = Math.hypot(dx, dz);
        if (len < 1.0e-6 || d < 1.0e-6) return true;                       // looking straight up or down, or the shot is on top of the wielder: count it as met
        return (lx * dx + lz * dz) / (len * d) > 0.0;
    }

    private static void onImpact(ProjectileImpactEvent e) {
        if (e.isCanceled() || e.getProjectile().level().isClientSide || !HeroConfig.enabled() || !HeroConfig.shieldReflects()) return;
        HitResult hit = e.getRayTraceResult();
        if (!(hit instanceof EntityHitResult eh) || !(eh.getEntity() instanceof ServerPlayer p) || !p.isBlocking()) return;
        if (!(p.getUseItem().getItem() instanceof HeroWeaponItem w) || w.kind().style() != HeroStyle.BLOCK) return;
        Projectile pr = e.getProjectile();
        SafeCast.tick("hero.shield.reflect", () -> {
            HeroBond b = HeroBonds.of(p);
            if (!b.isBound() || b.kind() != w.kind() || !Deflect.dangerous(pr)) return;
            if (pr.getOwner() == p || pr.getOwner() instanceof LivingEntity o && Foes.isAlly(p, o)) return;
            Vec3 look = p.getViewVector(1f);
            if (!frontal(look.x, look.z, pr.getX() - p.getX(), pr.getZ() - p.getZ())) return;
            e.setCanceled(true);
            Deflect.turnBack(p, pr);
            ShieldFx.sound(p.serverLevel(), p.position(), net.minecraft.sounds.SoundEvents.SHIELD_BLOCK, 1.0f, 1.4f);
        });
    }
}
