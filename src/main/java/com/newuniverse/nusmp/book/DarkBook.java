package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.blackclover.TimeStop;
import com.newuniverse.nusmp.vfx.VfxShape;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.ability.magic.Element;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Dark Magic (Yami). */
public class DarkBook extends GrimoireBook {
    // 0.28: rebuilt after the wiki (Yami Sukehiro); page ids kept so unlocks and mastery carry over, new spells appended
    private final List<BookPage> pages = List.of(
            BookPage.starter("avidya_slash", "Dark Cloaked Avidya Slash", CanonSpells::avidyaSlash),
            BookPage.zone("black_hole", "Black Hole", CanonSpells::blackHole),
            BookPage.signature("dimension_slash", "Dark Cloaked Dimension Slash", CanonSpells::dimensionSlash).withCooldown(300),
            BookPage.mid("death_thrust", "Death Thrust", CanonSpells::deathThrust),
            BookPage.mid("black_blade", "Dark Cloaked Black Blade", CanonSpells::blackBlade).withCooldown(900),
            BookPage.mid("avidya_wild_slash", "Dark Cloaked Avidya Wild Slash", CanonSpells::avidyaWildSlash),
            BookPage.zone("black_moon", "Dark Cloaked Black Moon", CanonSpells::blackMoon).withCooldown(900),
            BookPage.mid("iai_slash", "Dark Cloaked Iai Slash", CanonSpells::iaiSlash),
            BookPage.signature("dimension_slash_equinox", "Dark Cloaked Dimension Slash: Equinox", CanonSpells::dimensionSlashEquinox));

    public DarkBook() { super(MagicType.DARK, 0xFF5B3A8A); }
    @Override protected List<BookPage> familyPages() { return pages; }
    @Override public ResourceKey<DamageType> damageType() { return TensuraDamageTypes.DARKNESS_ELEMENTAL; }

    /** A 4.5-block arc that cuts deeper than armor expects (+25%). */
    static boolean avidya(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 eye = p.getEyePosition(), look = p.getViewVector(1f);
        for (LivingEntity t : around(p, p.position(), 4.5)) {
            if (t.getBoundingBox().getCenter().subtract(eye).normalize().dot(look) < 0.5) continue;
            b.hurt(i, p, t, mode, 12.5f);
        }
        b.castCircle(p, 0.6f);
        b.vfx(p, VfxShape.WIND_SLASH, eye, eye.add(look.scale(4.5)), 12, 1.1f);
        return true;
    }

    /** A pull sphere at the target point for 2 s. */
    static boolean blackHole(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 c = aim(p, 20);
        b.castCircle(p, 1f);
        b.vfx(p, VfxShape.DARK_BLACK_HOLE, c, c.add(0, 1, 0), 40, 4f);
        SpellRuntime.zone(p.serverLevel(), 40, 1, age -> {
            for (LivingEntity t : around(p, c, 4)) {
                Vec3 pull = c.subtract(t.position()).normalize().scale(0.35);
                t.setDeltaMovement(pull);
                t.hurtMarked = true;
                if (age % 10 == 0) b.hurt(i, p, t, mode, 2f);
            }
        });
        return true;
    }

    /** Cuts magic projectiles in its 16-block line first, then everything standing in it. */
    static boolean dimension(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 a = p.getEyePosition(), end = a.add(p.getViewVector(1f).scale(16));
        for (Projectile pr : p.serverLevel().getEntitiesOfClass(Projectile.class, new AABB(a, end).inflate(1.2))) if (pr.getOwner() != p) pr.discard();
        for (LivingEntity t : along(p, a, end, 1.2)) b.hurt(i, p, t, mode, 18f);
        b.castCircle(p, 1.4f);
        b.vfx(p, VfxShape.WIND_SLASH, a, end, 18, 1.6f);
        b.vfx(p, VfxShape.DARK_BLACK_HOLE, end, end.add(0, 1, 0), 20, 1.4f);
        return true;
    }
}
