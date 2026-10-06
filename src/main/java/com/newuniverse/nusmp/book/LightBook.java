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

/** Light Magic. */
public class LightBook extends GrimoireBook {
    private final List<BookPage> pages = List.of(
            BookPage.starter("judgment", "Light Sword of Judgment", LightBook::sword),
            BookPage.zone("divine_punishment", "Rays of Divine Punishment", LightBook::rays));

    public LightBook() { super(MagicType.LIGHT, 0xFFFFF2A8); }
    @Override protected List<BookPage> familyPages() { return pages; }
    @Override public ResourceKey<DamageType> damageType() { return TensuraDamageTypes.LIGHT_ELEMENTAL; }

    /** A 12-block beam sword; everything it touches glows. */
    static boolean sword(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 a = p.getEyePosition(), end = a.add(p.getViewVector(1f).scale(12));
        for (LivingEntity t : along(p, a, end, 1.0)) {
            b.hurt(i, p, t, mode, 10f);
            t.addEffect(new MobEffectInstance(MobEffects.GLOWING, 60, 0));
        }
        b.castCircle(p, 0.7f);
        b.vfx(p, VfxShape.LIGHTNING_SPEAR, a, end, 12, 1f);
        b.vfx(p, VfxShape.ELF_CIRCLE, p.position(), p.position().add(0, 1, 0), 20, 0.6f);
        return true;
    }

    /** Mark a target; a second later five light spears fall on it. */
    static boolean rays(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity t = target(p, 24);
        if (t == null) { fail(p, "Mark a target first."); return false; }
        b.castCircle(p, 1f);
        b.vfx(p, VfxShape.ELF_CIRCLE, t.position().add(0, 0.05, 0), t.position().add(0, 1, 0), 40, 0.8f);
        for (int k = 0; k < 5; k++) {
            SpellRuntime.later(p.serverLevel(), 20 + k * 3, () -> {
                if (!t.isAlive()) return;
                Vec3 at = t.position();
                b.vfx(p, VfxShape.LIGHTNING_SPEAR, at.add(0, 12, 0), at, 8, 0.8f);
                for (LivingEntity e : around(p, at, 1.5)) b.hurt(i, p, e, mode, 7f);
            });
        }
        return true;
    }
}
