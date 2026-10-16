package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.blackclover.TimeStop;
import com.newuniverse.nusmp.thread.ThreadConfig;
import com.newuniverse.nusmp.thread.ThreadSpells;
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
            BookPage.daily("rouge", "Red Thread of Fate: Rouge", ThreadBook::rouge),
            // 0.34: wiki spells, appended
            BookPage.zone("arachnes_web", "Arachne's Web", WikiSpells::arachnesWeb),
            BookPage.mid("dancing_doll", "Dancing Doll", WikiSpells::dancingDoll),
            BookPage.mid("thread_mending", "Thread Magic: Mending", WikiSpells::threadMending),
            // 1.43.0: Vanessa's exclusive pages, appended last so no mode number moves (all are new; locked to her grimoire by CharacterSpells)
            BookPage.mid("arresting_dark_weave", "Arresting Dark Weave", ThreadSpells::arrestingDarkWeave),
            BookPage.mid("dancing_doll_recall", "Dancing Doll: Recall", ThreadSpells::dancingDollRecall),
            BookPage.zone("thread_trap", "Thread Trap Generation", ThreadSpells::threadTrapGeneration),
            new BookPage("reinforcement", "Reinforcement Magic and Mana Skin", "Reinforcement Magic and Mana Skin", 0, 0, 20, ThreadSpells::reinforcement));

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
        b.vfx(p, VfxShape.THREAD_STRINGS, p.getEyePosition().add(p.getViewVector(1f).scale(0.6)).add(0, -0.3, 0), t.getBoundingBox().getCenter(), ticks, 1f);
        return true;
    }

    /** Summon Rouge for the day: once, she undoes your death (position 4 s ago, half your health). */
    static boolean rouge(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        if (ThreadConfig.rougeEntity()) return ThreadSpells.callRouge(b, i, p);   // 1.43.0: a real cat of thread; the old behaviour below runs when thread.rougeEntity is false
        if (i.getOrCreateTag().getBoolean("RougeArmed")) { fail(p, "Rouge is already watching over you."); return false; }
        i.getOrCreateTag().putBoolean("RougeArmed", true);
        b.castCircle(p, 0.8f);
        RougeCat.show(p);                                                 // 0.49: Rouge herself, a cat of red thread on your head
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
        ThreadSpells.reinforceTick(i, p);                                  // 1.43.0: the Mana Skin's upkeep and buffs
        if (t.getBoolean("RougeArmed") && !ThreadConfig.rougeEntity()) RougeCat.show(p);                  // 0.49: she stays on your head while she watches over you
    }

    /** Rouge unravels and pulls you out of the way (the wiki: she unravels to dodge): a 12% chance to turn a blow aside. */
    @Override
    public boolean onTakenDamage(ManasSkillInstance i, LivingEntity owner, net.minecraft.world.damagesource.DamageSource source,
                                 io.github.manasmods.manascore.network.api.util.Changeable<Float> amount) {
        super.onTakenDamage(i, owner, source, amount);
        if (owner instanceof ServerPlayer p && !ThreadConfig.rougeEntity() && i.getOrCreateTag().getBoolean("RougeArmed") && source.getEntity() instanceof LivingEntity
                && p.getRandom().nextFloat() < 0.12f) {
            amount.set(0f);
            com.newuniverse.nusmp.vfx.VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.ROUGE_CAT, p, p.position().add(0, 1, 0), 0, 30, 2f);
            p.displayClientMessage(net.minecraft.network.chat.Component.literal("Rouge unravels and pulls you out of the way.").withStyle(net.minecraft.ChatFormatting.RED), true);
        }
        return true;
    }

    @Override
    public boolean onDeath(ManasSkillInstance i, LivingEntity owner, net.minecraft.world.damagesource.DamageSource source) {
        if (!super.onDeath(i, owner, source)) return false;   // five-sided fortune ward first
        var t = i.getOrCreateTag();
        if (!(owner instanceof ServerPlayer p) || ThreadConfig.rougeEntity() || !t.getBoolean("RougeArmed")) return true;   // 1.43.0: the real Rouge replaces the undo
        if (source.is(net.minecraft.world.damagesource.DamageTypes.FELL_OUT_OF_WORLD)) return true;
        t.putBoolean("RougeArmed", false);
        i.setCoolDown(1200, 1);   // seconds: one in-game day
        p.setHealth(p.getMaxHealth() * 0.5f);
        if (t.contains("PX4")) p.teleportTo(t.getDouble("PX4"), t.getDouble("PY4"), t.getDouble("PZ4"));
        p.displayClientMessage(net.minecraft.network.chat.Component.literal("Rouge pulls the thread back. That never happened.")
                .withStyle(net.minecraft.ChatFormatting.RED), false);
        com.newuniverse.nusmp.vfx.VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.ROUGE_CAT, p, p.position().add(0, 1, 0), 0, 40, 2f);   // she unravels
        i.markDirty();
        return false;
    }
}
