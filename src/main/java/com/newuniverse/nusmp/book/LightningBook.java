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

/** Lightning Magic. */
public class LightningBook extends GrimoireBook {
    private final List<BookPage> pages = List.of(
            BookPage.starter("chain", "Chain Lightning", LightningBook::chain),
            BookPage.mid("thunder_boots", "Thunder God's Boots", LightningBook::boots),
            BookPage.zone("thunder_lance", "Thunder Lance", TensuraShots.shot(TensuraShots.Shot.THUNDER_LANCE, 13, 2.4f, 0.6f, 0)));

    public LightningBook() { super(MagicType.LIGHTNING, 0xFFFFE14A); }
    @Override protected List<BookPage> familyPages() { return pages; }
    @Override public ResourceKey<DamageType> damageType() { return TensuraDamageTypes.LIGHTNING_ELEMENTAL; }

    /** Strikes the target, then arcs to two more within 5 blocks. */
    static boolean chain(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity t = target(p, 16);
        if (t == null) { fail(p, "No target for the lightning."); return false; }
        java.util.Set<LivingEntity> hit = new java.util.HashSet<>();
        Vec3 from = p.getEyePosition();
        LivingEntity cur = t;
        for (int jump = 0; jump < 3 && cur != null; jump++) {
            hit.add(cur);
            b.hurt(i, p, cur, mode, jump == 0 ? 9f : 6f);
            Vec3 to = cur.getBoundingBox().getCenter();
            b.vfx(p, VfxShape.FX_LIGHTNING_ARC, from, to, 0, 1f);
            from = to;
            LivingEntity next = null;
            for (LivingEntity e : around(p, to, 5)) if (!hit.contains(e) && (next == null || e.distanceToSqr(cur) < next.distanceToSqr(cur))) next = e;
            cur = next;
        }
        return true;
    }

    /** Dash 6 blocks; your next melee hit within 5 s releases the stored charge. */
    static boolean boots(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 from = p.position(), dir = p.getViewVector(1f).multiply(1, 0, 1).normalize();
        Vec3 dest = null;
        for (double d = 6; d >= 1; d -= 0.5) {
            Vec3 c = from.add(dir.scale(d));
            if (p.level().noCollision(p, p.getBoundingBox().move(c.subtract(from)))) { dest = c; break; }
        }
        if (dest == null) { fail(p, "No room to dash."); return false; }
        b.castCircle(p, 0.6f);
        p.teleportTo(dest.x, dest.y, dest.z);
        b.vfx(p, VfxShape.LIGHTNING_SPEAR, from.add(0, 1, 0), dest.add(0, 1, 0), 10, 1f);
        i.getOrCreateTag().putLong("ChargeUntil", p.level().getGameTime() + 100);
        return true;
    }

    @Override
    public boolean onDamageEntity(ManasSkillInstance i, LivingEntity owner, LivingEntity target,
                                  net.minecraft.world.damagesource.DamageSource source,
                                  io.github.manasmods.manascore.network.api.util.Changeable<Float> amount) {
        if (owner instanceof ServerPlayer p && source.getDirectEntity() == owner
                && owner.level().getGameTime() < i.getOrCreateTag().getLong("ChargeUntil")) {
            i.getOrCreateTag().putLong("ChargeUntil", 0);
            float extra = com.newuniverse.nusmp.balance.BalanceLaw.damage(target, 8f, masteryFrac(i));
            amount.set(amount.get() + extra);
            vfx(p, VfxShape.LIGHTNING_SPEAR, target.position().add(0, 6, 0), target.position(), 8, 0.8f);
        }
        return true;
    }
}
