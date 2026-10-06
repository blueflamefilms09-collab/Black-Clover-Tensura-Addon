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

/** Thread Magic. */
public class ThreadBook extends GrimoireBook {
    private final List<BookPage> pages = List.of(
            BookPage.starter("red_thread", "Red Thread", ThreadBook::redThread),
            BookPage.daily("rouge", "Rouge", ThreadBook::rouge));

    public ThreadBook() { super(MagicType.THREAD, 0xFFFF3355); }
    @Override protected List<BookPage> familyPages() { return pages; }
    @Override public ResourceKey<DamageType> damageType() { return TensuraDamageTypes.MAGIC_GENERIC; }

    /** Bind one enemy with red thread (hard control, shared lock). */
    static boolean redThread(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity t = target(p, 16);
        if (t == null) { fail(p, "No one to bind."); return false; }
        if (!control(p)) return false;
        int ticks = BalanceLaw.controlTicks(t, 60);
        t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 9));
        t.setDeltaMovement(0, Math.min(0, t.getDeltaMovement().y), 0);
        t.hurtMarked = true;
        b.hurt(i, p, t, mode, 3f);
        b.castCircle(p, 0.6f);
        b.vfx(p, VfxShape.THREAD_LINE, p.getEyePosition(), t.getBoundingBox().getCenter(), ticks, 1f);
        return true;
    }

    /** Summon Rouge for the day: once, she undoes your death (position 4 s ago, half your health). */
    static boolean rouge(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        if (i.getOrCreateTag().getBoolean("RougeArmed")) { fail(p, "Rouge is already watching over you."); return false; }
        i.getOrCreateTag().putBoolean("RougeArmed", true);
        b.castCircle(p, 0.8f);
        b.vfx(p, VfxShape.THREAD_LINE, p.position().add(0, 0.2, 0), p.position().add(0, 2, 0), 30, 0.8f);
        return true;
    }

    /** Remembers where you were each second (for Rouge). */
    @Override
    protected void tickBook(ManasSkillInstance i, ServerPlayer p) {
        var t = i.getOrCreateTag();
        for (int k = 4; k > 0; k--) {
            t.putDouble("PX" + k, t.getDouble("PX" + (k - 1))); t.putDouble("PY" + k, t.getDouble("PY" + (k - 1))); t.putDouble("PZ" + k, t.getDouble("PZ" + (k - 1)));
        }
        t.putDouble("PX0", p.getX()); t.putDouble("PY0", p.getY()); t.putDouble("PZ0", p.getZ());
    }

    @Override
    public boolean onDeath(ManasSkillInstance i, LivingEntity owner, net.minecraft.world.damagesource.DamageSource source) {
        if (!super.onDeath(i, owner, source)) return false;   // five-sided fortune ward first
        var t = i.getOrCreateTag();
        if (!(owner instanceof ServerPlayer p) || !t.getBoolean("RougeArmed")) return true;
        if (source.is(net.minecraft.world.damagesource.DamageTypes.FELL_OUT_OF_WORLD)) return true;
        t.putBoolean("RougeArmed", false);
        i.setCoolDown(1200, 1);   // seconds: one in-game day
        p.setHealth(p.getMaxHealth() * 0.5f);
        if (t.contains("PX4")) p.teleportTo(t.getDouble("PX4"), t.getDouble("PY4"), t.getDouble("PZ4"));
        p.displayClientMessage(net.minecraft.network.chat.Component.literal("Rouge pulls the thread back. That never happened.")
                .withStyle(net.minecraft.ChatFormatting.RED), false);
        vfx(p, VfxShape.THREAD_LINE, p.position().add(0, 0.2, 0), p.position().add(0, 2.2, 0), 30, 1.2f);
        i.markDirty();
        return false;
    }
}
