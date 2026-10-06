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

/** Mercury Magic. */
public class MercuryBook extends GrimoireBook {
    private final List<BookPage> pages = List.of(
            BookPage.starter("silver_blade", "Silver Blade", MercuryBook::blade),
            BookPage.signature("mercury_shield", "Mercury Shield", MercuryBook::shield),
            // 0.34: Nozel Silva's character spell
            BookPage.signature("mercury_rain", "Mercury Magic: Mercury Rain", WikiSpells::mercuryRain));

    public MercuryBook() { super(MagicType.MERCURY, 0xFFC9D1DB); }
    @Override protected List<BookPage> familyPages() { return pages; }
    @Override public ResourceKey<DamageType> damageType() { return TensuraDamageTypes.MAGIC_GENERIC; }

    /** A 6-block mercury whip in front of you. */
    static boolean blade(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 eye = p.getEyePosition(), look = p.getViewVector(1f);
        for (LivingEntity t : around(p, p.position(), 6)) {
            if (t.getBoundingBox().getCenter().subtract(eye).normalize().dot(look) < 0.6) continue;
            b.hurt(i, p, t, mode, 9f);
        }
        b.castCircle(p, 0.6f);
        b.vfx(p, VfxShape.WIND_SLASH, eye, eye.add(look.scale(6)), 12, 1.2f);
        return true;
    }

    /** A liquid-metal shield for 8 s: incoming damage is halved. */
    static boolean shield(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        i.getOrCreateTag().putLong("ShieldUntil", p.level().getGameTime() + 160);
        b.castCircle(p, 1.2f);
        b.vfx(p, VfxShape.MIRROR_PANE, p.position().add(0, 1, 0), p.position().add(p.getViewVector(1f)).add(0, 1, 0), 160, 1.2f);
        return true;
    }

    @Override
    public boolean onTakenDamage(ManasSkillInstance i, LivingEntity owner, net.minecraft.world.damagesource.DamageSource source,
                                 io.github.manasmods.manascore.network.api.util.Changeable<Float> amount) {
        super.onTakenDamage(i, owner, source, amount);   // mana skin
        if (owner.level().getGameTime() < i.getOrCreateTag().getLong("ShieldUntil")) amount.set(amount.get() * 0.5f);
        return true;
    }
}
