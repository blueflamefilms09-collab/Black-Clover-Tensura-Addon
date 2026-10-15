package com.newuniverse.nusmp.dungeon.nexus.relic;

import com.newuniverse.nusmp.particle.rukh.RukhFx.Alignment;
import com.newuniverse.nusmp.particle.rukh.RukhFx;
import com.newuniverse.nusmp.antimagic.Nullification;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.book.GrimoireBook;
import com.newuniverse.nusmp.dungeon.DungeonEffects;
import com.newuniverse.nusmp.item.DemonSlayerSwordItem;
import com.newuniverse.nusmp.item.MagicWeaponItem;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.List;

/**
 * The Devil-Bound Weapon Sheath (docs/living_dungeon.md section 7). Worn on the belt and attuned, it drinks the magicules of magic its
 * wearer parries or blocks ({@link SheathMath}: the Magebane Shield's parry window, or any cancelled magical blow) and shows the charge
 * (0 to 100) in its tooltip and on the action bar. Full, the next melee strike also releases a cross-dimensional Anti-Magic slash: a
 * long line of Conceptual Severance (armour, enchantments and resistances are ignored, magic nullification jammed) through everything in
 * front, stopped only by solid walls. Its curse, Mana Siphon: left full for more than a minute while worn, it discharges into the wearer
 * (Magicule Poisoning and a bounded wound), after two warnings, and the charge resets.
 */
final class SheathHandler {
    private SheathHandler() {}

    private static final int TINT = 0xFFFFC0EE;
    /** True while the siphon's own wound is being dealt, so it does not charge the sheath it came from. */
    private static boolean discharging;

    // ---------------------------------------------------------------- the Magebane Shield's parry window (see item.MagebaneShieldArts)
    private static final String K_PARRY = "nusmp_magebane_parry_until", K_PARRY_FREE = "nusmp_magebane_parry_free";

    /** Is the Spellbreak Parry open for this player right now? The same test the shield itself makes. */
    static boolean parryWindow(ServerPlayer p) {
        CompoundTag d = p.getPersistentData();
        if (d.getLong(K_PARRY) <= p.level().getGameTime()) return false;
        if (d.getBoolean(K_PARRY_FREE)) return true;
        return p.isUsingItem() && p.getUseItem().getItem() instanceof MagicWeaponItem w && w.kind == MagicWeaponItem.Kind.MAGEBANE_SHIELD;
    }

    // ---------------------------------------------------------------- charging and striking (last word on a blow)
    /** Registered last and for cancelled events too: by now the parry and every other shield have had their say. */
    static void onDamageLast(LivingIncomingDamageEvent e) {
        DamageSource src = e.getSource();
        if (e.getEntity() instanceof ServerPlayer wearer && !discharging) absorb(e, wearer, src);
        if (src.getEntity() instanceof ServerPlayer striker && !e.isCanceled()) strike(e, striker, src);
    }

    private static void absorb(LivingIncomingDamageEvent e, ServerPlayer p, DamageSource src) {
        if (!RelicRules.magic(src)) return;
        boolean parried = parryWindow(p);
        if (!parried && !e.isCanceled()) return;                                 // an ordinary hit: nothing was blocked
        if (!Worn.has(p, Relic.DEVIL_BOUND_SHEATH)) return;
        ItemStack sheath = Worn.stack(p, Relic.DEVIL_BOUND_SHEATH);
        if (sheath.isEmpty()) return;
        int gain = SheathMath.gain(Math.max(e.getOriginalAmount(), e.getAmount()), parried);
        if (gain <= 0) return;
        Integer have = sheath.get(RelicComponents.CHARGE.get());
        int before = have == null ? 0 : have, after = SheathMath.add(before, gain);
        if (after == before) return;
        sheath.set(RelicComponents.CHARGE.get(), after);
        if (SheathMath.full(after)) sheath.set(RelicComponents.FULL_FOR.get(), 0);
        try {
            EnergyHelper.gainMagicule(p, EnergyHelper.getMaxMagicule(p) * SheathMath.magiculeShare(after - before), EnergyHelper.GainType.NORMAL);
        } catch (RuntimeException | LinkageError noPool) {
            // no pool to feed: the charge still counts
        }
        ServerLevel level = p.serverLevel();
        Vec3 at = p.position().add(0, 1.2, 0).add(p.getViewVector(1f).scale(0.9));
        VfxSpawn.send(level, VfxShape.KOTO_SHATTER, at, p.position().add(0, 1.0, 0), TINT, 14, 0.9f);
        level.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.2f, 0.7f + 0.5f * after / SheathMath.MAX);
        boolean nowFull = SheathMath.full(after);
        p.displayClientMessage(Component.literal(nowFull ? "The Sheath is full! Your next strike cuts across the dimensions."
                : "Sheath charge " + after + " / " + SheathMath.MAX).withStyle(nowFull ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.DARK_AQUA), true);
    }

    private static void strike(LivingIncomingDamageEvent e, ServerPlayer p, DamageSource src) {
        if (!src.is(DamageTypes.PLAYER_ATTACK) || src.getDirectEntity() != p || !Worn.has(p, Relic.DEVIL_BOUND_SHEATH)) return;
        ItemStack sheath = Worn.stack(p, Relic.DEVIL_BOUND_SHEATH);
        if (sheath.isEmpty()) return;
        Integer charge = sheath.get(RelicComponents.CHARGE.get());
        if (charge == null || !SheathMath.full(charge)) return;
        sheath.set(RelicComponents.CHARGE.get(), 0);
        sheath.set(RelicComponents.FULL_FOR.get(), 0);
        slash(p);
    }

    /** The release: a line {@link SheathMath#SLASH_RANGE} long along the wielder's look, cut short by the first solid wall. */
    private static void slash(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        Vec3 eye = p.getEyePosition(), look = p.getViewVector(1f), far = eye.add(look.scale(SheathMath.SLASH_RANGE)), end = far;
        HitResult wall = level.clip(new ClipContext(eye, far, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        if (wall.getType() != HitResult.Type.MISS) end = wall.getLocation();
        Vec3 from = eye.add(0, -0.4, 0).add(look.scale(0.6));
        VfxSpawn.send(level, VfxShape.DARK_SLASH_DIMENSION, from, end, TINT, 16, 2.4f);
        VfxSpawn.send(level, VfxShape.ANTI_MAGIC_SLASH, from, from.add(look.scale(2.5)), TINT, 12, 1.4f);
        VfxSpawn.send(level, VfxShape.SPACE_PORTAL, end, end.subtract(look), TINT, 14, 1.6f);
        level.playSound(null, p.blockPosition(), SoundEvents.TRIDENT_THUNDER.value(), SoundSource.PLAYERS, 1.6f, 1.5f);
        level.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.8f, 0.5f);
        List<LivingEntity> hit = GrimoireBook.along(p, eye, end, SheathMath.SLASH_WIDTH);
        int n = 0;
        for (LivingEntity t : hit) {
            if (n++ >= SheathMath.SLASH_TARGETS) break;
            t.hurt(DemonSlayerSwordItem.severance(p), BalanceLaw.damage(p, t, SheathMath.SLASH_RAW, 0.8));
            try {
                Nullification.interfere(t, 4, 0.5f, 0.25f);
            } catch (RuntimeException | LinkageError noSkills) {
                // nothing to jam
            }
        }
        p.displayClientMessage(Component.literal("Anti-Magic slash!" + (n > 0 ? " " + Math.min(n, SheathMath.SLASH_TARGETS) + " struck." : "")).withStyle(ChatFormatting.LIGHT_PURPLE), true);
    }

    // ---------------------------------------------------------------- Mana Siphon (once a second)
    static void tick(ServerPlayer p, int mask) {
        if ((mask & Relic.DEVIL_BOUND_SHEATH.bit()) == 0) return;
        ItemStack sheath = Worn.stack(p, Relic.DEVIL_BOUND_SHEATH);
        if (sheath.isEmpty()) return;
        Integer charge = sheath.get(RelicComponents.CHARGE.get()), full = sheath.get(RelicComponents.FULL_FOR.get());
        int c = charge == null ? 0 : charge, was = full == null ? 0 : full;
        int now = SheathMath.fullTicks(was, c, 20);
        if (now != was) sheath.set(RelicComponents.FULL_FOR.get(), now);
        long time = p.level().getGameTime();
        switch (SheathMath.siphon(now)) {
            case WARN -> {
                if (RelicState.due(p.getUUID(), "sheath_warn", time, 400)) {
                    p.displayClientMessage(Component.literal("The Sheath hums, straining. Strike, or it will bite you.").withStyle(ChatFormatting.YELLOW), true);
                }
            }
            case CRACKLE -> {
                RukhFx.spray(p.serverLevel(), Alignment.LIGHT, p.getX(), p.getY() + 1.0, p.getZ(), 10, 0.4, 0.6, 0.4, 0.1);
                if (RelicState.due(p.getUUID(), "sheath_crackle", time, 100)) {
                    p.displayClientMessage(Component.literal("The Sheath cracks with magicules! Strike now!").withStyle(ChatFormatting.RED), true);
                }
            }
            case DISCHARGE -> discharge(p, sheath);
            default -> { }
        }
    }

    /** The siphon bites: Magicule Poisoning and a wound that never takes the last point of health. The charge is gone. */
    private static void discharge(ServerPlayer p, ItemStack sheath) {
        sheath.set(RelicComponents.CHARGE.get(), 0);
        sheath.set(RelicComponents.FULL_FOR.get(), 0);
        ServerLevel level = p.serverLevel();
        p.addEffect(new MobEffectInstance(DungeonEffects.MAGICULE_POISONING, 1200, 1));
        float damage = SheathMath.dischargeDamage(p.getHealth(), p.getMaxHealth());
        if (damage > 0f && !p.isCreative()) {
            discharging = true;
            try {
                p.hurt(p.damageSources().magic(), damage);
            } finally {
                discharging = false;
            }
        }
        Vec3 c = p.position().add(0, 1.0, 0);
        VfxSpawn.send(level, VfxShape.MAGIC_CIRCLE_EXPLOSION, c, c, TINT, 14, 1.8f);
        level.playSound(null, p.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.8f, 1.6f);
        p.displayClientMessage(Component.literal("Mana Siphon! The Sheath discharges into you.").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), true);
    }
}
