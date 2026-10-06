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

/** Mirror Magic. */
public class MirrorBook extends GrimoireBook {
    private final List<BookPage> pages = List.of(
            BookPage.starter("reflect_refrain", "Reflect Refrain", MirrorBook::reflect),
            BookPage.signature("real_double", "Real Double", MirrorBook::realDouble),
            // 0.34: wiki spells, appended
            BookPage.mid("reflect_ray", "Reflect Ray", WikiSpells::reflectRay),
            BookPage.zone("large_reflect_ray", "Large Reflect Ray", WikiSpells::largeReflectRay),
            BookPage.signature("full_reflection", "Full Reflection", WikiSpells::fullReflection));

    public MirrorBook() { super(MagicType.MIRROR, 0xFFDDEEFF); }
    @Override protected List<BookPage> familyPages() { return pages; }
    @Override public ResourceKey<DamageType> damageType() { return TensuraDamageTypes.MAGIC_GENERIC; }

    /** A pane in front of you for 3 s; enemy projectiles that touch it fly back as yours. */
    static boolean reflect(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        b.castCircle(p, 0.6f);
        Vec3 pane = p.getEyePosition().add(p.getViewVector(1f).scale(1.6));
        b.vfx(p, VfxShape.MIRROR_PANE, pane, pane.add(p.getViewVector(1f)), 60, 1f);
        SpellRuntime.zone(p.serverLevel(), 60, 1, age -> {
            for (Projectile pr : p.serverLevel().getEntitiesOfClass(Projectile.class, new AABB(pane, pane).inflate(1.6))) {
                if (pr.getOwner() == p) continue;
                pr.setDeltaMovement(pr.getDeltaMovement().scale(-1));
                pr.setOwner(p);
                pr.hurtMarked = true;
            }
        });
        return true;
    }

    /**
     * Real Double (0.34, with the body-double logic of Tensura's clones): three mirror doubles circle you for 10 s - each blow
     * aimed at you may hit a double instead (it shatters), each of your blows is echoed by the doubles - and your mirror image
     * casts your last page again, once, for free.
     */
    static boolean realDouble(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        i.getOrCreateTag().putLong("DoublesUntil", p.level().getGameTime() + 200);
        i.getOrCreateTag().putInt("Doubles", 3);
        i.markDirty();
        com.newuniverse.nusmp.vfx.VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.MIRROR_DOUBLE, p, p.position().add(0, 1, 0), 0, 200, 3f);
        for (net.minecraft.world.entity.Mob m : p.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.Mob.class, p.getBoundingBox().inflate(16)))
            if (m.getTarget() == p && p.getRandom().nextBoolean()) m.setTarget(null);       // half of them lose you among the doubles
        int last = i.getOrCreateTag().getInt("LastMode");
        if (last == mode || last >= b.familyCount()) last = 0;
        BookPage page = b.page(last);
        b.vfx(p, VfxShape.MIRROR_PANE, p.position().add(0, 1, 0), p.position().add(p.getViewVector(1f)), 30, 1.4f);
        return page.cast().cast(b, i, p, last);
    }

    static int doubles(ManasSkillInstance i, LivingEntity owner) {
        return owner.level().getGameTime() < i.getOrCreateTag().getLong("DoublesUntil") ? i.getOrCreateTag().getInt("Doubles") : 0;
    }

    /** A blow aimed at you may strike a double instead (50%); the double shatters. */
    @Override
    public boolean onTakenDamage(ManasSkillInstance i, LivingEntity owner, net.minecraft.world.damagesource.DamageSource source,
                                 io.github.manasmods.manascore.network.api.util.Changeable<Float> amount) {
        super.onTakenDamage(i, owner, source, amount);
        int n = doubles(i, owner);
        if (n > 0 && source.getEntity() instanceof LivingEntity && owner instanceof ServerPlayer p && p.getRandom().nextBoolean()) {
            amount.set(0f);
            i.getOrCreateTag().putInt("Doubles", n - 1);
            i.markDirty();
            com.newuniverse.nusmp.vfx.VfxSpawn.send(p.serverLevel(), VfxShape.MIRROR_PANE, p.position().add(0, 1, 0), p.position().add(p.getViewVector(1f)).add(0, 1, 0), 0, 12, 1.2f);
            p.displayClientMessage(net.minecraft.network.chat.Component.literal("The blow shatters a double (" + (n - 1) + " left).").withStyle(net.minecraft.ChatFormatting.AQUA), true);
        }
        return true;
    }

    /** Your doubles mirror your blows: +20% damage per double still standing. */
    @Override
    public boolean onDamageEntity(ManasSkillInstance i, LivingEntity owner, LivingEntity target, net.minecraft.world.damagesource.DamageSource source,
                                  io.github.manasmods.manascore.network.api.util.Changeable<Float> amount) {
        int n = doubles(i, owner);
        if (n > 0 && source.getDirectEntity() == owner) amount.set(amount.get() * (1 + 0.2f * n));
        return true;
    }
}
