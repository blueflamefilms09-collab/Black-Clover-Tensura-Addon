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

/**
 * Mirror Magic (Gauche Adlai), rebuilt in 0.42 as a front-line attribute (see {@link MirrorWorks}): real ornate mirrors that
 * catch and return spells, tangible Real Doubles from the mirror world, travel between mirrors, mirror blades and a mirror
 * meteor storm, everything scaled by the caster's EP. Page ids from 0.34 are kept; new pages are appended.
 */
public class MirrorBook extends GrimoireBook {
    private final List<BookPage> pages = List.of(
            BookPage.starter("reflect_refrain", "Reflect Refrain", MirrorWorks::reflectRefrain),
            BookPage.signature("real_double", "Real Double", MirrorWorks::realDouble).withCooldown(600),
            // 0.34: wiki spells, appended (0.42: rebuilt)
            BookPage.mid("reflect_ray", "Reflect Ray", MirrorWorks::reflectRay),
            BookPage.zone("large_reflect_ray", "Large Reflect Ray", WikiSpells::largeReflectRay),
            BookPage.signature("full_reflection", "Full Reflection", MirrorWorks::fullReflection),
            // 0.42: appended
            BookPage.mid("mirror_array", "Mirror Array", MirrorWorks::mirrorArray).withCooldown(900),
            BookPage.mid("mirror_step", "Mirror Step", MirrorWorks::mirrorStep).withCooldown(100),
            BookPage.zone("mirrors_slash", "Mirrors Slash", MirrorWorks::mirrorsSlash),
            BookPage.signature("mirrors_meteorite", "Mirrors Meteorite", MirrorWorks::mirrorsMeteorite).withCooldown(900));

    public MirrorBook() { super(MagicType.MIRROR, 0xFFDDEEFF); }
    @Override protected List<BookPage> familyPages() { return pages; }
    @Override public ResourceKey<DamageType> damageType() { return TensuraDamageTypes.MAGIC_GENERIC; }

    /** A blow aimed at you may strike one of your Real Doubles instead (35%): it steps in front of it. */
    @Override
    public boolean onTakenDamage(ManasSkillInstance i, LivingEntity owner, net.minecraft.world.damagesource.DamageSource source,
                                 io.github.manasmods.manascore.network.api.util.Changeable<Float> amount) {
        super.onTakenDamage(i, owner, source, amount);
        if (!(owner instanceof ServerPlayer p) || !(source.getEntity() instanceof LivingEntity) || p.getRandom().nextFloat() >= 0.35f) return true;
        var out = MirrorWorks.doubles(p);
        if (out.isEmpty()) return true;
        var d = out.get(p.getRandom().nextInt(out.size()));
        float a = amount.get();
        amount.set(0f);
        d.hurt(source, a);
        p.displayClientMessage(net.minecraft.network.chat.Component.literal("Your double takes the blow.").withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE), true);
        return true;
    }

    /** Together with your doubles you strike harder (the wiki's double-power attack): +20% per double, up to +60%. */
    @Override
    public boolean onDamageEntity(ManasSkillInstance i, LivingEntity owner, LivingEntity target, net.minecraft.world.damagesource.DamageSource source,
                                  io.github.manasmods.manascore.network.api.util.Changeable<Float> amount) {
        if (owner instanceof ServerPlayer p && source.getDirectEntity() == owner) {
            int n = Math.min(3, MirrorWorks.doubles(p).size());
            if (n > 0) amount.set(amount.get() * (1 + 0.2f * n));
        }
        return true;
    }
}
