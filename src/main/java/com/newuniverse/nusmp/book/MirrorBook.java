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
            BookPage.signature("real_double", "Real Double", MirrorBook::realDouble));

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

    /** Your mirror image casts your last page again, once, for free. */
    static boolean realDouble(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        int last = i.getOrCreateTag().getInt("LastMode");
        if (last == mode || last >= b.familyCount()) last = 0;
        BookPage page = b.page(last);
        b.vfx(p, VfxShape.MIRROR_PANE, p.position().add(0, 1, 0), p.position().add(p.getViewVector(1f)), 30, 1.4f);
        return page.cast().cast(b, i, p, last);
    }
}
